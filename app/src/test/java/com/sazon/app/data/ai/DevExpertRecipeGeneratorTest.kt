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

    // Escenario 5: nunca se reintenta un 4xx no reintentable (1 sola llamada).
    @Test
    fun nunca_reintenta_un_4xx_no_reintentable() = runTest {
        listOf(400, 401, 403, 404, 422).forEach { status ->
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

    private fun provider(
        apiKey: String = KEY,
        client: HttpClient,
        onEvent: (AiCallEvent) -> Unit = {},
        now: () -> Long = System::currentTimeMillis,
    ) = DevExpertRecipeGenerator(
        apiKey = apiKey,
        taxonomy = taxonomy,
        client = client,
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
