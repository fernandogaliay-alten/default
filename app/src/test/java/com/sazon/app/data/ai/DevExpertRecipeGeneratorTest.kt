package com.sazon.app.data.ai

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeJson
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.search.AiSearchService
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.domain.validation.DiscardReason
import com.sazon.app.ui.search.AiSearchOutcome
import com.sazon.app.ui.search.AiSearchStage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-10 de `docs/specs/real-ai-provider.md` con `MockEngine` y reloj inyectado: contrato de
 * la peticion, reintentos/`Retry-After`, no-reintento de 4xx, deadline, cancelacion, clave ausente
 * (sin peticion), degradacion a modo semilla de los llamadores reales y los tres prompts.
 *
 * Sin red real ni clave real: la clave es ficticia (`test-key`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevExpertRecipeGeneratorTest {

    private val taxonomy = taxonomy()

    // Escenario 1: camino feliz con una sola llamada y contrato exacto de la peticion.
    @Test
    fun el_camino_feliz_hace_una_sola_llamada_con_url_cabecera_y_cuerpo() = runTest {
        val mock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }
        val recipes = provider(client = AiHttpClientFactory.create(mock.engine))
            .generateFromIngredients(listOf(RecipeIngredient("Tomate", 1.0)), constraints())

        assertEquals(1, mock.calls)
        assertEquals("Arroz salteado con verduras", recipes.single().title)

        val request = mock.lastRequest
        assertEquals("https://inference.devexpert.io/v1/chat/completions", request.url.toString())
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("Bearer $KEY", request.headers[HttpHeaders.Authorization])

        val body = Json.parseToJsonElement(request.bodyText()).jsonObject
        assertEquals("chat", body["model"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray
        assertEquals("system", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("user", messages[1].jsonObject["role"]!!.jsonPrimitive.content)
        assertTrue(
            messages[0].jsonObject["content"]!!.jsonPrimitive.content.contains("Restricciones duras:") ||
                messages[1].jsonObject["content"]!!.jsonPrimitive.content.contains("Restricciones duras:"),
        )
    }

    // Escenario 2: clave ausente -> MISSING_KEY sin ninguna peticion HTTP.
    @Test
    fun la_clave_ausente_lanza_missing_key_sin_ninguna_peticion() = runTest {
        val mock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }
        val generator = provider(apiKey = "", client = AiHttpClientFactory.create(mock.engine))

        val thrown = captureUnavailable { generator.generateFromIngredients(emptyList(), constraints()) }

        assertNotNull(thrown)
        assertEquals(AiUnavailableReason.MISSING_KEY, thrown!!.reason)
        assertEquals("No debe hacer ninguna peticion", 0, mock.calls)
    }

    // Escenario 2 (degradacion real): con el GenerationService aceptado, clave ausente -> aiUnavailable.
    @Test
    fun la_clave_ausente_degrada_el_generation_service_a_modo_semilla() = runTest {
        val mock = MockAi { _, _ -> error("no deberia llamarse") }
        val generator = provider(apiKey = "", client = AiHttpClientFactory.create(mock.engine))
        val service = com.sazon.app.domain.generation.GenerationService(generator = generator, taxonomy = taxonomy)

        val result = service.generate(listOf(RecipeIngredient("Tomate", 1.0)), constraints())

        assertTrue("Debe marcar aiUnavailable", result.aiUnavailable)
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.discarded.isEmpty())
        assertEquals(0, mock.calls)
    }

    // Escenario 3: un 200 ilegible o vacio NO lanza y devuelve lista vacia (1 sola llamada).
    @Test
    fun un_200_ilegible_o_vacio_no_lanza_y_devuelve_lista_vacia() = runTest {
        val bodies = listOf(
            openAiBody("lo siento, no puedo ayudarte"),
            openAiBody(null),
            openAiBody(""),
            """{"choices":[]}""",
            "<html>vaya</html>",
        )

        bodies.forEach { body ->
            val mock = MockAi { _, _ -> respond(content = body, status = HttpStatusCode.OK, headers = jsonHeaders()) }
            val recipes = provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil())

            assertTrue("El cuerpo '$body' debe dar lista vacia", recipes.isEmpty())
            assertEquals("El cuerpo '$body' no debe reintentar", 1, mock.calls)
        }
    }

    // Escenario 4: reintento unico ante 429 respetando un Retry-After corto (2 s).
    @Test
    fun reintenta_una_vez_ante_429_respetando_retry_after_corto() = runTest {
        val mock = MockAi { _, call ->
            if (call == 1) {
                respond(content = "", status = HttpStatusCode.TooManyRequests, headers = headersOf(HttpHeaders.RetryAfter, "2"))
            } else {
                okBody(recipeEnvelope(arroz()))
            }
        }
        val startedAt = testScheduler.currentTime

        val recipes = provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil())

        assertEquals(2, mock.calls)
        assertEquals("El backoff debe ser el de Retry-After", 2_000L, testScheduler.currentTime - startedAt)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
    }

    // Escenario 4: Retry-After largo (>5 s) o ausente cae al backoff fijo de 500 ms.
    @Test
    fun un_retry_after_largo_o_ausente_usa_el_backoff_fijo() = runTest {
        val headers = listOf(
            headersOf(HttpHeaders.RetryAfter, "30"),
            headersOf(HttpHeaders.RetryAfter, "abc"),
            headersOf(),
        )

        headers.forEach { retryHeaders ->
            val mock = MockAi { _, call ->
                if (call == 1) {
                    respond(content = "", status = HttpStatusCode.TooManyRequests, headers = retryHeaders)
                } else {
                    okBody(recipeEnvelope(arroz()))
                }
            }
            val startedAt = testScheduler.currentTime

            provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil())

            assertEquals(2, mock.calls)
            assertEquals("Debe usar el backoff fijo", 500L, testScheduler.currentTime - startedAt)
        }
    }

    // Escenario 5: nunca se reintenta un 4xx no reintentable (1 sola llamada). 400/422 se excluyen a
    // proposito: con el formato activo disparan el fallback one-shot (ver los tests de la feature).
    @Test
    fun nunca_reintenta_un_4xx_no_reintentable() = runTest {
        listOf(401, 403, 404).forEach { status ->
            val mock = MockAi { _, _ -> respond(content = "", status = HttpStatusCode.fromValue(status)) }

            val thrown = captureUnavailable { provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil()) }

            assertNotNull("El estado $status debe degradar", thrown)
            assertEquals("El estado $status", AiUnavailableReason.HTTP_ERROR, thrown!!.reason)
            assertEquals("El estado $status solo hace una llamada", 1, mock.calls)
        }
    }

    // Escenario 5: un 429 agotado (dos intentos) lanza RATE_LIMITED.
    @Test
    fun un_429_agotado_lanza_rate_limited_tras_dos_intentos() = runTest {
        val mock = MockAi { _, _ -> respond(content = "", status = HttpStatusCode.TooManyRequests) }

        val thrown = captureUnavailable { provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil()) }

        assertNotNull(thrown)
        assertEquals(AiUnavailableReason.RATE_LIMITED, thrown!!.reason)
        assertEquals(2, mock.calls)
    }

    // Escenario 6: se reintenta ante 5xx, IOException y timeout; luego exito.
    @Test
    fun reintenta_ante_5xx_ioexception_y_timeout() = runTest {
        val fiveHundred = MockAi { _, call ->
            if (call == 1) respond(content = "", status = HttpStatusCode.ServiceUnavailable) else okBody(recipeEnvelope(arroz()))
        }
        val ioError = MockAi { _, call ->
            if (call == 1) throw IOException("sin red") else okBody(recipeEnvelope(arroz()))
        }
        val timeout = MockAi { _, call ->
            if (call == 1) throw SocketTimeoutException("timeout") else okBody(recipeEnvelope(arroz()))
        }

        listOf(fiveHundred, ioError, timeout).forEach { mock ->
            val recipes = provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil())
            assertEquals("Debe reintentar una vez", 2, mock.calls)
            assertEquals(1, recipes.size)
        }
    }

    // Escenario 6: dos fallos reintentables -> 2 llamadas y excepcion, nunca un tercer intento.
    @Test
    fun dos_fallos_reintentables_lanzan_tras_dos_intentos_sin_tercero() = runTest {
        val mock = MockAi { _, _ -> respond(content = "", status = HttpStatusCode.InternalServerError) }

        val thrown = captureUnavailable { provider(client = AiHttpClientFactory.create(mock.engine)).suggestByPreferences(perfil()) }

        assertNotNull(thrown)
        assertEquals(AiUnavailableReason.HTTP_ERROR, thrown!!.reason)
        assertEquals(2, mock.calls)
    }

    // Escenario 2 de `docs/specs/ai-provider-timeouts.md`: un timeout de socket agotado se reporta como
    // TIMEOUT (hoy NETWORK) tras exactamente 2 intentos, y el evento final lo dice en su `detail`.
    @Test
    fun un_timeout_de_socket_agotado_lanza_timeout_tras_dos_intentos() = runTest {
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, _ -> throw SocketTimeoutException("timeout de lectura") }

        val thrown = captureUnavailable {
            provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                .suggestByPreferences(perfil())
        }

        assertNotNull(thrown)
        assertEquals("Un timeout de socket no es un fallo de red", AiUnavailableReason.TIMEOUT, thrown!!.reason)
        assertEquals(2, mock.calls)
        assertEquals(AiCallOutcome.UNAVAILABLE, events.last().outcome)
        assertEquals("TIMEOUT", events.last().detail)
    }

    // Escenario 3: timeout de socket en el primer intento y exito en el segundo -> 2 llamadas, recetas,
    // evento RETRY con `detail = "TIMEOUT"` y backoff fijo de 500 ms.
    @Test
    fun un_timeout_de_socket_seguido_de_exito_reintenta_con_motivo_timeout() = runTest {
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, call ->
            if (call == 1) throw SocketTimeoutException("timeout") else okBody(recipeEnvelope(arroz()))
        }
        val startedAt = testScheduler.currentTime

        val recipes = provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
            .suggestByPreferences(perfil())

        assertEquals(2, mock.calls)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
        assertEquals("TIMEOUT", events.first { it.outcome == AiCallOutcome.RETRY }.detail)
        assertEquals("El backoff del reintento de transporte es el fijo", 500L, testScheduler.currentTime - startedAt)
    }

    // Escenario 1 de `docs/specs/ai-provider-timeouts.md`: un intento lento (~35 s, por encima del
    // socket timeout viejo de 30 s) cabe en la politica nueva -> 1 sola llamada y recetas.
    @Test
    fun un_intento_lento_dentro_de_la_politica_no_degrada() = runTest {
        val events = mutableListOf<AiCallEvent>()
        var fakeNow = 0L
        val mock = MockAi { _, _ ->
            fakeNow += 35_000L
            okBody(recipeEnvelope(arroz()))
        }

        val recipes = provider(
            client = AiHttpClientFactory.create(mock.engine),
            onEvent = { events += it },
            now = { fakeNow },
        ).suggestByPreferences(perfil())

        assertEquals("Un intento lento que acaba bien no reintenta", 1, mock.calls)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
        assertEquals(AiCallOutcome.SUCCESS, events.last().outcome)
        assertEquals(35_000L, events.last().durationMillis)
    }

    // Escenario 7: con el deadline agotado no se inicia un segundo intento (1 llamada).
    @Test
    fun el_deadline_agotado_no_inicia_un_segundo_intento() = runTest {
        var fakeNow = 0L
        val mock = MockAi { _, _ ->
            fakeNow += AiProviderPolicy.TOTAL_DEADLINE_MILLIS
            respond(content = "", status = HttpStatusCode.ServiceUnavailable)
        }

        val thrown = captureUnavailable {
            provider(client = AiHttpClientFactory.create(mock.engine), now = { fakeNow }).suggestByPreferences(perfil())
        }

        assertNotNull(thrown)
        assertEquals(AiUnavailableReason.TIMEOUT, thrown!!.reason)
        assertEquals("No debe iniciar el segundo intento", 1, mock.calls)
    }

    // Escenario 8: la cancelacion se relanza siempre, no se convierte ni se traga.
    @Test
    fun la_cancelacion_se_propaga() = runTest {
        val started = CompletableDeferred<Unit>()
        val mock = MockAi { _, _ ->
            started.complete(Unit)
            awaitCancellation()
        }
        val generator = provider(client = AiHttpClientFactory.create(mock.engine))

        var propagated: CancellationException? = null
        val job = launch {
            try {
                generator.suggestByPreferences(perfil())
            } catch (cancellation: CancellationException) {
                propagated = cancellation
                throw cancellation
            }
        }

        started.await()
        assertEquals("La peticion debe estar en vuelo", 1, mock.calls)
        job.cancelAndJoin()

        assertTrue("El job debe quedar cancelado", job.isCancelled)
        assertNotNull("Debe relanzarse CancellationException", propagated)
    }

    // Escenario 9: el proveedor no pre-filtra; el llamador conserva el estado Bloqueado de ai-search.
    @Test
    fun el_proveedor_no_prefiltra_y_ai_search_mantiene_el_estado_bloqueado() = runTest {
        val mock = MockAi { _, _ -> okBody(recipeEnvelope(polloAlAjillo(), bocadilloTomate())) }
        val generator = provider(client = AiHttpClientFactory.create(mock.engine))
        val service = AiSearchService(generator = generator, taxonomy = taxonomy)
        val profile = perfil(dietPattern = "vegan", hardRestrictions = listOf("gluten"))

        val result = service.search("pollo", profile)

        assertTrue("Ninguna receta sobrevive al Perfil", result.accepted.isEmpty())
        assertFalse(result.aiUnavailable)
        assertTrue(
            "El pollo cae por dieta",
            result.discarded.any { it.title == "Pollo al ajillo" && it.reason == DiscardReason.DIET_PATTERN },
        )
        assertTrue(
            "El bocadillo cae por restriccion dura",
            result.discarded.any { it.title == "Bocadillo de tomate" && it.reason == DiscardReason.HARD_RESTRICTION },
        )
        assertTrue(AiSearchOutcome.stageOf(result, "pollo", profile, taxonomy) is AiSearchStage.Blocked)
    }

    // Escenario 10: las tres operaciones componen su prompt.
    @Test
    fun las_tres_operaciones_componen_su_prompt() = runTest {
        val bodies = mutableListOf<String>()
        val mock = MockAi { request, _ ->
            bodies += request.bodyText()
            okBody(recipeEnvelope(arroz()))
        }
        val generator = provider(client = AiHttpClientFactory.create(mock.engine))

        generator.suggestByPreferences(perfil())
        generator.searchByText("pollo", perfil())
        generator.generateFromIngredients(
            listOf(RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate")),
            GenerationConstraints(
                profile = perfil(),
                servings = 4,
                mealTypeId = "dinner",
                timeBucketId = "lte_30",
                difficultyId = "easy",
                notes = "sin picante",
            ),
        )

        assertEquals(3, bodies.size)
        val users = bodies.map { userContent(it) }

        users.forEach { user ->
            assertTrue(user.contains("Patrón de dieta:"))
            assertTrue(user.contains("Restricciones duras:"))
        }
        assertFalse("suggestByPreferences no lleva ingredientes", users[0].contains("Ingredientes disponibles:"))
        assertFalse("suggestByPreferences no lleva consulta", users[0].contains("Búsqueda:"))
        assertTrue(users[1].contains("Búsqueda: pollo"))
        assertFalse("searchByText no lleva ingredientes", users[1].contains("Ingredientes disponibles:"))
        assertTrue(users[2].contains("Ingredientes disponibles:"))
        assertTrue(users[2].contains("Tomate"))
        assertTrue(users[2].contains("Comida:"))
        assertTrue(users[2].contains("Comensales: 4"))
        assertTrue(users[2].contains("Notas: sin picante"))
    }

    // Diagnostico local: se emiten eventos y ninguno incluye la clave.
    @Test
    fun los_eventos_de_diagnostico_no_incluyen_la_clave() = runTest {
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }

        provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
            .suggestByPreferences(perfil())

        assertTrue(events.isNotEmpty())
        assertEquals(AiCallOutcome.SUCCESS, events.last().outcome)
        assertTrue(events.none { it.toString().contains(KEY) })
    }

    // Escenario 2 de `docs/specs/ai-call-diagnostics.md`: un 200 con contenido vacio o ilegible deja
    // el evento en EMPTY explicando las longitudes del contenido y 0 recibidas/aceptadas, en una sola
    // llamada (sin reintento).
    @Test
    fun un_contenido_vacio_o_ilegible_se_explica_en_el_evento_como_empty() = runTest {
        val cases = listOf("" to 0, "   " to 0, "lo siento, no puedo ayudarte" to 28)

        cases.forEach { (content, expectedChars) ->
            val events = mutableListOf<AiCallEvent>()
            val mock = MockAi { _, _ -> okBody(content) }

            val recipes = provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                .suggestByPreferences(perfil())

            assertTrue("El contenido '$content' debe dar lista vacia", recipes.isEmpty())
            assertEquals("El contenido '$content' no debe reintentar", 1, mock.calls)

            val event = events.last()
            assertEquals(AiCallOutcome.EMPTY, event.outcome)
            assertEquals("HTTP 200", event.detail)
            assertEquals(0, event.received ?: -1)
            assertEquals(0, event.accepted ?: -1)
            assertTrue("Un EMPTY sin descartes no tiene motivos", event.discardedByReason.isEmpty())
            assertEquals("Longitudes del contenido de '$content'", expectedChars, event.contentChars ?: -1)
            if (expectedChars == 0) {
                assertEquals("El contenido en blanco no tiene bytes", 0, event.contentBytes ?: -1)
            } else {
                assertTrue("El texto ilegible tiene bytes", (event.contentBytes ?: 0) > 0)
            }
        }
    }

    // Escenario 3: un lote parcial deja el evento en SUCCESS con recibidas, aceptadas y el resumen de
    // motivos de descarte.
    @Test
    fun un_lote_parcial_se_explica_en_el_evento_con_recibidas_aceptadas_y_motivos() = runTest {
        val content = """{"recipes":[
            {"title":"Rota"},
            ${contractRecipe(title = "Comida inexistente", mealTypeId = "second_breakfast")},
            ${contractRecipe(title = "Tiempos incoherentes", totalMinutes = 60)},
            ${contractRecipe()}
        ]}"""
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, _ -> okBody(content) }

        val recipes = provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
            .generateFromIngredients(emptyList(), constraints())

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
        assertEquals(1, mock.calls)

        val event = events.last()
        assertEquals(AiCallOutcome.SUCCESS, event.outcome)
        assertEquals(4, event.received ?: -1)
        assertEquals(1, event.accepted ?: -1)
        assertEquals(
            mapOf(DiscardReason.SCHEMA to 2, DiscardReason.INCOHERENT to 1),
            event.discardedByReason,
        )
    }

    // Escenario 4: un lote en el que ninguna receta sobrevive queda EMPTY (no lanza), con N recibidas
    // y el motivo del descarte explicando el vacio.
    @Test
    fun un_lote_sin_recetas_aceptadas_queda_empty_con_el_motivo() = runTest {
        val content = """{"recipes":[{"title":"Rota"},${contractRecipe(totalMinutes = 60)}]}"""
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, _ -> okBody(content) }

        val recipes = provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
            .generateFromIngredients(emptyList(), constraints())

        assertTrue("No debe lanzar: el lote queda vacio", recipes.isEmpty())
        assertEquals(1, mock.calls)

        val event = events.last()
        assertEquals(AiCallOutcome.EMPTY, event.outcome)
        assertEquals(2, event.received ?: -1)
        assertEquals(0, event.accepted ?: -1)
        assertTrue("El motivo debe explicar el EMPTY", event.discardedByReason.isNotEmpty())
        assertEquals(
            mapOf(DiscardReason.SCHEMA to 1, DiscardReason.INCOHERENT to 1),
            event.discardedByReason,
        )
    }

    // Escenario 5: ningun evento expone la clave, la cabecera Authorization, el cuerpo del modelo ni
    // los titulos de las recetas; solo longitudes, recuentos y motivos.
    @Test
    fun ningun_evento_expone_la_clave_ni_los_cuerpos() = runTest {
        val events = mutableListOf<AiCallEvent>()
        val bodies = listOf(
            openAiBody(recipeEnvelope(arroz())),
            openAiBody("lo siento, no puedo ayudarte"),
        )

        bodies.forEach { body ->
            val mock = MockAi { _, _ -> respond(content = body, status = HttpStatusCode.OK, headers = jsonHeaders()) }
            provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                .suggestByPreferences(perfil())
        }

        assertTrue(events.isNotEmpty())
        events.forEach { event ->
            val text = event.toString()
            assertFalse("Nunca la clave", text.contains(KEY))
            assertFalse("Nunca la cabecera Authorization", text.contains("Bearer"))
            assertFalse("Nunca la cabecera Authorization", text.contains("Authorization"))
            assertFalse("Nunca el cuerpo del modelo", text.contains("lo siento, no puedo ayudarte"))
            assertFalse("Nunca el titulo de una receta", text.contains("Arroz salteado con verduras"))
        }

        // El evento si explica: longitudes y desenlace, sin cuerpos.
        assertTrue(events.any { it.outcome == AiCallOutcome.SUCCESS && (it.contentChars ?: 0) > 0 })
        assertTrue(events.any { it.outcome == AiCallOutcome.EMPTY && (it.contentChars ?: 0) > 0 })
    }

    // Escenario 7 de `docs/specs/ai-recipe-decode.md` (regresion real del bug #70): un 200 con la
    // **forma real** del contrato §4 (JSON literal, sin `id` ni `source`) se resuelve en recetas y el
    // `GenerationService` real las acepta con `aiUnavailable == false`. Con el codigo de `main`
    // (`RecipeValidator.validateResponse`, que decodifica con `Recipe.serializer()` y exige
    // `id`/`source`) este mismo test daria lista y `accepted` vacios.
    @Test
    fun la_forma_real_del_contrato_llega_al_generation_service_como_recetas_aceptadas() = runTest {
        val mock = MockAi { _, _ -> okBody(REAL_CONTRACT_CONTENT) }
        val generator = provider(client = AiHttpClientFactory.create(mock.engine))
        val ingredients = listOf(RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"))

        val recipes = generator.generateFromIngredients(ingredients, constraints())

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
        assertEquals(RecipeSource.AI, recipes.single().source)
        assertTrue("El id lo completa la app", recipes.single().id.startsWith("ai-"))

        val service = com.sazon.app.domain.generation.GenerationService(generator = generator, taxonomy = taxonomy)
        val result = service.generate(ingredients, constraints())

        assertEquals("Una llamada por operacion", 2, mock.calls)
        assertFalse("Con el contrato real no se degrada", result.aiUnavailable)
        assertEquals(1, result.accepted.size)
        assertEquals("Arroz salteado con verduras", result.accepted.single().title)
        assertTrue("La receta valida no se descarta", result.discarded.isEmpty())
    }

    // --- Escenarios de `docs/specs/ai-response-format.md` (correctiva issue #74) ---

    // Escenario 1: con el interruptor activo (por defecto) la peticion lleva
    // `"response_format":{"type":"json_object"}`, una sola llamada y el evento SUCCESS con JSON_OBJECT.
    @Test
    fun el_formato_activo_envia_response_format_y_una_sola_llamada() = runTest {
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }

        val recipes = provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
            .generateFromIngredients(listOf(RecipeIngredient("Tomate", 1.0)), constraints())

        assertEquals(1, mock.calls)
        assertEquals(1, recipes.size)
        assertEquals(
            "json_object",
            responseFormatType(mock.lastRequest.bodyText()),
        )
        assertEquals(AiCallOutcome.SUCCESS, events.last().outcome)
        assertEquals(AiResponseFormat.JSON_OBJECT, events.last().responseFormat)
    }

    // Escenario 2: con el interruptor apagado el cuerpo NO contiene la clave (ni `null`), una sola
    // llamada y el evento lleva NONE.
    @Test
    fun el_interruptor_apagado_omite_la_clave_y_no_emite_null() = runTest {
        val events = mutableListOf<AiCallEvent>()
        val mock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }

        val recipes = provider(
            client = AiHttpClientFactory.create(mock.engine),
            responseFormatEnabled = false,
            onEvent = { events += it },
        ).suggestByPreferences(perfil())

        assertEquals(1, mock.calls)
        assertEquals(1, recipes.size)
        val body = mock.lastRequest.bodyText()
        assertFalse("Nunca la clave response_format", body.contains("response_format"))
        assertNull("Nunca un response_format nulo", responseFormatType(body))
        assertEquals(AiResponseFormat.NONE, events.last().responseFormat)
        assertEquals(AiCallOutcome.SUCCESS, events.last().outcome)
    }

    // Escenario 2 (degradacion): con el interruptor apagado un 400/422 degrada en UNA sola llamada,
    // exactamente como antes de la feature.
    @Test
    fun con_el_interruptor_apagado_un_400_o_422_degrada_en_una_sola_llamada() = runTest {
        listOf(400, 422).forEach { status ->
            val mock = MockAi { _, _ -> respond(content = "", status = HttpStatusCode.fromValue(status)) }

            val thrown = captureUnavailable {
                provider(client = AiHttpClientFactory.create(mock.engine), responseFormatEnabled = false)
                    .suggestByPreferences(perfil())
            }

            assertNotNull("El estado $status debe degradar", thrown)
            assertEquals("El estado $status", AiUnavailableReason.HTTP_ERROR, thrown!!.reason)
            assertEquals("El estado $status solo hace una llamada con el interruptor apagado", 1, mock.calls)
        }
    }

    // Escenario 4: 400/422 con formato activo -> exactamente 2 llamadas (primera con formato, segunda
    // sin), sin backoff artificial, evento FALLBACK (con el estado y JSON_OBJECT) y SUCCESS con NONE.
    @Test
    fun el_fallback_ante_400_o_422_reintenta_una_vez_sin_formato() = runTest {
        listOf(400, 422).forEach { status ->
            val events = mutableListOf<AiCallEvent>()
            val bodies = mutableListOf<String>()
            val startedAt = testScheduler.currentTime
            val mock = MockAi { request, call ->
                bodies += request.bodyText()
                if (call == 1) {
                    respond(content = "", status = HttpStatusCode.fromValue(status))
                } else {
                    okBody(recipeEnvelope(arroz()))
                }
            }

            val recipes = provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                .suggestByPreferences(perfil())

            assertEquals("El estado $status hace exactamente 2 llamadas", 2, mock.calls)
            assertEquals(1, recipes.size)
            assertEquals("json_object", responseFormatType(bodies[0]))
            assertEquals("La segunda peticion va sin formato", null, responseFormatType(bodies[1]))
            assertEquals("El fallback no espera backoff", 0L, testScheduler.currentTime - startedAt)

            val fallback = events.single { it.outcome == AiCallOutcome.FALLBACK }
            assertEquals(status, fallback.httpStatus)
            assertEquals(AiResponseFormat.JSON_OBJECT, fallback.responseFormat)
            assertEquals(AiCallOutcome.SUCCESS, events.last().outcome)
            assertEquals(AiResponseFormat.NONE, events.last().responseFormat)
        }
    }

    // Escenario 5: 400/422 persistente -> exactamente 2 llamadas (una con formato, una sin), sin
    // tercera, y HTTP_ERROR con el evento final UNAVAILABLE y NONE (un unico fallback por operacion).
    @Test
    fun el_fallback_es_una_sola_vez_ante_400_o_422_persistente() = runTest {
        listOf(400, 422).forEach { status ->
            val events = mutableListOf<AiCallEvent>()
            val mock = MockAi { _, _ -> respond(content = "", status = HttpStatusCode.fromValue(status)) }

            val thrown = captureUnavailable {
                provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                    .suggestByPreferences(perfil())
            }

            assertNotNull("El estado $status debe degradar", thrown)
            assertEquals(AiUnavailableReason.HTTP_ERROR, thrown!!.reason)
            assertEquals("El estado $status hace exactamente 2 llamadas, nunca una tercera", 2, mock.calls)
            assertEquals(AiCallOutcome.UNAVAILABLE, events.last().outcome)
            assertEquals(AiResponseFormat.NONE, events.last().responseFormat)
        }
    }

    // Regresion (Finding 1 del validador): el fallback respeta `MAX_ATTEMPTS = 2`. Si el primer intento
    // consume el reintento de transporte (503 o IOException) y el segundo devuelve 400/422, NO se anade
    // una tercera peticion: el 400/422 sin presupuesto cae en la rama actual -> UNAVAILABLE/HTTP_ERROR
    // con el formato del intento y sin `FALLBACK`.
    @Test
    fun el_fallback_no_anade_un_tercer_intento() = runTest {
        // 503 en la 1. llamada (consume el reintento de transporte) y 400 en la 2.
        val retryThen400 = MockAi { _, call ->
            if (call == 1) {
                respond(content = "", status = HttpStatusCode.ServiceUnavailable)
            } else {
                respond(content = "", status = HttpStatusCode.BadRequest)
            }
        }
        // IOException en la 1. y 422 en la 2. (misma familia: transporte + fallback sin presupuesto).
        val ioThen422 = MockAi { _, call ->
            if (call == 1) {
                throw IOException("sin red")
            } else {
                respond(content = "", status = HttpStatusCode.UnprocessableEntity)
            }
        }

        listOf(retryThen400 to 400, ioThen422 to 422).forEach { (mock, lastStatus) ->
            val events = mutableListOf<AiCallEvent>()

            val thrown = captureUnavailable {
                provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                    .suggestByPreferences(perfil())
            }

            assertNotNull("El estado final $lastStatus debe degradar", thrown)
            assertEquals("El estado final $lastStatus", AiUnavailableReason.HTTP_ERROR, thrown!!.reason)
            assertEquals("El estado final $lastStatus nunca hace una tercera peticion", 2, mock.calls)
            assertTrue(
                "El estado final $lastStatus no debe emitir FALLBACK",
                events.none { it.outcome == AiCallOutcome.FALLBACK },
            )
            assertTrue(
                "El primer intento reintenta el transporte",
                events.any { it.outcome == AiCallOutcome.RETRY },
            )
            assertEquals(AiCallOutcome.UNAVAILABLE, events.last().outcome)
            assertEquals(
                "El intento final conserva el formato (JSON_OBJECT)",
                AiResponseFormat.JSON_OBJECT,
                events.last().responseFormat,
            )
        }
    }

    // Escenario 6: 401/403/404 no disparan fallback (1 llamada, HTTP_ERROR, con formato en el evento);
    // 429/5xx mantienen el reintento de transporte (2 llamadas, ambas con formato, sin FALLBACK).
    @Test
    fun el_fallback_solo_lo_disparan_400_y_422() = runTest {
        listOf(401, 403, 404).forEach { status ->
            val events = mutableListOf<AiCallEvent>()
            val mock = MockAi { _, _ -> respond(content = "", status = HttpStatusCode.fromValue(status)) }

            val thrown = captureUnavailable {
                provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                    .suggestByPreferences(perfil())
            }

            assertEquals(AiUnavailableReason.HTTP_ERROR, thrown!!.reason)
            assertEquals("El estado $status no dispara el fallback", 1, mock.calls)
            assertEquals(AiCallOutcome.UNAVAILABLE, events.last().outcome)
            assertEquals(AiResponseFormat.JSON_OBJECT, events.last().responseFormat)
        }

        listOf(429, 503).forEach { status ->
            val bodies = mutableListOf<String>()
            val events = mutableListOf<AiCallEvent>()
            val mock = MockAi { request, _ ->
                bodies += request.bodyText()
                respond(content = "", status = HttpStatusCode.fromValue(status))
            }

            val thrown = captureUnavailable {
                provider(client = AiHttpClientFactory.create(mock.engine), onEvent = { events += it })
                    .suggestByPreferences(perfil())
            }

            assertNotNull(thrown)
            assertEquals("El estado $status reintenta el transporte", 2, mock.calls)
            assertEquals("Ambos intentos llevan formato", listOf("json_object", "json_object"), bodies.map { responseFormatType(it) })
            assertTrue("El reintento de transporte no es FALLBACK", events.none { it.outcome == AiCallOutcome.FALLBACK })
        }
    }

    // Escenario 7: los eventos distinguen el modo y el fallback y ninguno filtra la clave, la cabecera
    // Authorization ni el texto del modelo.
    @Test
    fun los_eventos_distinguen_el_modo_y_el_fallback_sin_fugas() = runTest {
        val allEvents = mutableListOf<AiCallEvent>()

        // Exito con formato.
        val okMock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }
        provider(client = AiHttpClientFactory.create(okMock.engine), onEvent = { allEvents += it })
            .suggestByPreferences(perfil())
        assertEquals(AiResponseFormat.JSON_OBJECT, allEvents.last().responseFormat)

        // Fallback visible en la secuencia.
        val fallbackMock = MockAi { _, call ->
            if (call == 1) respond(content = "", status = HttpStatusCode.BadRequest) else okBody(recipeEnvelope(arroz()))
        }
        provider(client = AiHttpClientFactory.create(fallbackMock.engine), onEvent = { allEvents += it })
            .suggestByPreferences(perfil())
        assertTrue(allEvents.any { it.outcome == AiCallOutcome.FALLBACK })

        // Interruptor apagado -> NONE y sin FALLBACK.
        val offEvents = mutableListOf<AiCallEvent>()
        val offMock = MockAi { _, _ -> okBody(recipeEnvelope(arroz())) }
        provider(
            client = AiHttpClientFactory.create(offMock.engine),
            responseFormatEnabled = false,
            onEvent = { offEvents += it },
        ).suggestByPreferences(perfil())
        assertEquals(AiResponseFormat.NONE, offEvents.last().responseFormat)
        assertTrue(offEvents.none { it.outcome == AiCallOutcome.FALLBACK })
        allEvents += offEvents

        allEvents.forEach { event ->
            val text = event.toString()
            assertFalse("Nunca la clave", text.contains(KEY))
            assertFalse("Nunca la cabecera Authorization", text.contains("Bearer"))
            assertFalse("Nunca la cabecera Authorization", text.contains("Authorization"))
            assertFalse("Nunca el titulo de una receta", text.contains("Arroz salteado con verduras"))
        }
    }

    private fun provider(
        apiKey: String = KEY,
        client: HttpClient,
        responseFormatEnabled: Boolean = AiProviderPolicy.RESPONSE_FORMAT_ENABLED,
        onEvent: (AiCallEvent) -> Unit = {},
        now: () -> Long = System::currentTimeMillis,
    ) = DevExpertRecipeGenerator(
        apiKey = apiKey,
        taxonomy = taxonomy,
        client = client,
        responseFormatEnabled = responseFormatEnabled,
        onEvent = onEvent,
        now = now,
    )

    private suspend fun captureUnavailable(block: suspend () -> Unit): AiUnavailableException? {
        var thrown: AiUnavailableException? = null
        try {
            block()
        } catch (unavailable: AiUnavailableException) {
            thrown = unavailable
        }
        return thrown
    }

    private fun MockRequestHandleScope.okBody(content: String) = respond(
        content = openAiBody(content),
        status = HttpStatusCode.OK,
        headers = jsonHeaders(),
    )

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")
}

private const val KEY = "test-key"

/**
 * Forma **real** del contrato §4 de `docs/domain/taxonomy.md`: JSON literal **sin** `id` ni
 * `source` (los posee la app). Cierra el punto ciego del bug #70: los fixtures con
 * `RecipeJson.encode` si los incluian y por eso el fallo no se detectaba.
 */
private val REAL_CONTRACT_CONTENT: String = """
    {"recipes":[{
      "title":"Arroz salteado con verduras",
      "description":"Un arroz rapido con verduras.",
      "categoryId":"rice",
      "mealTypeId":"lunch",
      "cuisineId":"asian",
      "totalMinutes":25,
      "prepMinutes":10,
      "cookMinutes":15,
      "difficultyId":"easy",
      "servings":2,
      "ingredients":[
        {"name":"Arroz","quantity":200,"unit":"g","isPantry":false,"ingredientId":"arroz"},
        {"name":"Zanahoria","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"zanahoria"}
      ],
      "steps":[
        {"order":1,"title":"Cocer el arroz","description":"Cuece el arroz.","minutes":12},
        {"order":2,"title":"Saltear","description":"Saltea las verduras.","minutes":13}
      ],
      "tips":[],
      "nutrition":null,
      "declaredAllergens":[]
    }]}
""".trimIndent()


/** Mock con contador de llamadas y captura de peticiones para inspeccionarlas en los tests. */
private class MockAi(
    private val handler: suspend MockRequestHandleScope.(HttpRequestData, Int) -> HttpResponseData,
) {
    private val requests = mutableListOf<HttpRequestData>()

    var calls = 0
        private set

    val engine: MockEngine = MockEngine { request ->
        requests += request
        calls += 1
        handler(request, calls)
    }

    val lastRequest: HttpRequestData get() = requests.last()
}

private fun HttpRequestData.bodyText(): String =
    (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty()

/** `response_format.type` de la peticion, o `null` si el cuerpo no lleva la clave. */
private fun responseFormatType(body: String): String? =
    Json.parseToJsonElement(body)
        .jsonObject["response_format"]
        ?.jsonObject
        ?.get("type")
        ?.jsonPrimitive
        ?.content

private fun openAiBody(content: String?): String =
    Json.encodeToString(
        ChatCompletionResponse.serializer(),
        ChatCompletionResponse(
            choices = listOf(ChatChoice(message = ChatMessage(role = "assistant", content = content))),
        ),
    )

private fun userContent(requestJson: String): String =
    Json.parseToJsonElement(requestJson)
        .jsonObject["messages"]!!
        .jsonArray[1]
        .jsonObject["content"]!!
        .jsonPrimitive
        .content

private fun recipeEnvelope(vararg recipes: Recipe): String =
    """{"recipes":[${recipes.joinToString(",") { RecipeJson.encode(it) }}]}"""

/**
 * Receta del contrato §4 como **JSON literal** (sin `id` ni `source`, los posee la app): la forma que
 * el modelo envia de verdad. Se usa para probar el diagnostico del decode (escenarios 3 y 4 de
 * `docs/specs/ai-call-diagnostics.md`).
 *
 * Por defecto es **coherente** (el unico paso suma `stepMinutes` = 25 = `totalMinutes`, ingredientes
 * sin alergenos declarados). Para forzar un descarte por tiempos, sube `totalMinutes` sin tocar
 * `stepMinutes`.
 */
private fun contractRecipe(
    title: String = "Arroz salteado con verduras",
    mealTypeId: String = "lunch",
    cuisineId: String = "asian",
    difficultyId: String = "easy",
    totalMinutes: Int = 25,
    stepMinutes: Int = 25,
): String = """
    {
      "title": "$title",
      "description": "Un arroz rapido con verduras.",
      "categoryId": "rice",
      "mealTypeId": "$mealTypeId",
      "cuisineId": "$cuisineId",
      "totalMinutes": $totalMinutes,
      "prepMinutes": 10,
      "cookMinutes": 15,
      "difficultyId": "$difficultyId",
      "servings": 2,
      "ingredients":[
        {"name":"Arroz","quantity":200,"unit":"g","isPantry":false,"ingredientId":"arroz"},
        {"name":"Zanahoria","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"zanahoria"}
      ],
      "steps":[
        {"order":1,"title":"Cocer el arroz","description":"Cuece el arroz.","minutes":$stepMinutes}
      ],
      "tips":[],
      "nutrition":null,
      "declaredAllergens":[]
    }
""".trimIndent()

private fun perfil(
    dietPattern: String = "omnivore",
    hardRestrictions: List<String> = emptyList(),
) = PreferenceProfile(
    dietPatternId = dietPattern,
    hardRestrictionIds = hardRestrictions,
    softPreferenceIds = emptyList(),
    cuisineIds = emptyList(),
    preferredIngredientIds = emptyList(),
    dislikedIngredientIds = emptyList(),
    difficultyId = "easy",
    timeBucketId = "lte_15",
    defaultServings = 2,
    updatedAt = 1_600_000_000_000L,
)

private fun constraints() = GenerationConstraints(
    profile = perfil(),
    servings = 2,
)

private fun arroz(): Recipe = Recipe(
    id = "test-arroz",
    source = RecipeSource.AI,
    title = "Arroz salteado con verduras",
    description = "Un arroz rapido con verduras.",
    categoryId = "rice",
    mealTypeId = "lunch",
    cuisineId = "asian",
    totalMinutes = 25,
    prepMinutes = 10,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz"),
        RecipeIngredient("Zanahoria", 1.0, MeasureUnit.UNIT, ingredientId = "zanahoria"),
    ),
    steps = listOf(
        RecipeStep(1, "Cocer el arroz", "Cuece el arroz.", 12),
        RecipeStep(2, "Saltear", "Saltea las verduras.", 13),
    ),
    declaredAllergens = emptyList(),
    matchScore = null,
)

private fun polloAlAjillo(): Recipe = Recipe(
    id = "test-pollo-ajillo",
    source = RecipeSource.AI,
    title = "Pollo al ajillo",
    description = "Pollo dorado con ajo y perejil.",
    categoryId = "poultry",
    mealTypeId = "dinner",
    cuisineId = "spanish",
    totalMinutes = 35,
    prepMinutes = 10,
    cookMinutes = 25,
    difficultyId = "medium",
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Pollo", 500.0, MeasureUnit.GRAM, ingredientId = "pollo"),
        RecipeIngredient("Ajo", 4.0, MeasureUnit.UNIT, ingredientId = "ajo"),
    ),
    steps = listOf(
        RecipeStep(1, "Dorar el pollo", "Dora el pollo por todos lados.", 15),
        RecipeStep(2, "Anadir el ajo", "Incorpora el ajo laminado.", 20),
    ),
    declaredAllergens = emptyList(),
    matchScore = null,
)

private fun bocadilloTomate(): Recipe = Recipe(
    id = "test-bocadillo",
    source = RecipeSource.AI,
    title = "Bocadillo de tomate",
    description = "Un bocadillo sencillo de tomate y aceite.",
    categoryId = "sandwich_wrap",
    mealTypeId = "breakfast",
    cuisineId = "spanish",
    totalMinutes = 5,
    prepMinutes = 5,
    cookMinutes = 0,
    difficultyId = "easy",
    servings = 1,
    ingredients = listOf(
        RecipeIngredient("Pan", 1.0, unit = null, ingredientId = null),
        RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"),
    ),
    steps = listOf(RecipeStep(1, "Montar el bocadillo", "Unta el tomate y aliña.", 5)),
    declaredAllergens = listOf("gluten"),
    matchScore = null,
)

private fun taxonomy(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
