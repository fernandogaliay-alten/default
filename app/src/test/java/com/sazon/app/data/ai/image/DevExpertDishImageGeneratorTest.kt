package com.sazon.app.data.ai.image

import com.sazon.app.data.ai.AiHttpClientFactory
import com.sazon.app.domain.image.DishImageRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-9 de `docs/specs/dish-style-reference.md` (mas los escenarios 1, 4, 5, 6, 9 y 10 de la
 * fase 2, `docs/specs/dish-image-generator.md`) con `MockEngine` y un [DishImageReducer] falso: el
 * transporte se prueba en **JVM puro** (sin Robolectric y sin Android). Sin red real ni clave real: la
 * clave es ficticia (`test-key`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevExpertDishImageGeneratorTest {

    // Escenario 1 (fase 2): una unica peticion POST con URL, cabeceras y cuerpo exactos; devuelve bytes.
    @Test
    fun el_camino_feliz_hace_una_sola_peticion_con_el_contrato_exacto() = runTest {
        val rawBytes = byteArrayOf(0x10, 0x20, 0x30)
        val mock = MockImages { _, _ -> okBody(encodedResponse(rawBytes)) }
        val generator = generator(client = AiHttpClientFactory.create(mock.engine), reducer = passThrough())

        val result = generator.generate(request(prompt = "Fotografía gastronómica de paella valenciana"))

        assertNotNull("El camino feliz devuelve bytes", result)
        assertEquals(rawBytes.toList(), result!!.toList())
        assertEquals("Exactamente una peticion", 1, mock.calls)

        val req = mock.lastRequest
        assertEquals("https://inference.devexpert.io/v1/images/generations", req.url.toString())
        assertEquals(HttpMethod.Post, req.method)
        assertEquals("Bearer $KEY", req.headers[HttpHeaders.Authorization])
        val outgoingContentType = (req.body as OutgoingContent).contentType
        assertNotNull("La peticion debe declarar un Content-Type", outgoingContentType)
        assertTrue(
            "Content-Type fue '$outgoingContentType'",
            outgoingContentType!!.match(ContentType.Application.Json),
        )

        val body = Json.parseToJsonElement(req.bodyText()).jsonObject
        assertEquals("image", body["model"]!!.jsonPrimitive.content)
        assertEquals(
            "Fotografía gastronómica de paella valenciana",
            body["prompt"]!!.jsonPrimitive.content,
        )
        assertEquals("1024x1024", body["size"]!!.jsonPrimitive.content)
        assertEquals(
            "Solo model, prompt y size",
            setOf("model", "prompt", "size"),
            body.keys,
        )
    }

    // Escenario 4 (fase 2): cualquier error HTTP devuelve null sin lanzar y en una sola peticion.
    @Test
    fun un_error_http_devuelve_null_sin_lanzar_y_sin_reintentar() = runTest {
        listOf(400, 401, 429, 500).forEach { status ->
            val mock = MockImages { _, _ ->
                respond(content = "", status = HttpStatusCode.fromValue(status))
            }

            val result = generator(client = AiHttpClientFactory.create(mock.engine)).generate(request())

            assertNull("El estado $status debe devolver null", result)
            assertEquals("El estado $status no debe reintentar", 1, mock.calls)
        }
    }

    // Escenario 5 (fase 2): un timeout o un fallo de transporte devuelve null sin lanzar.
    @Test
    fun un_timeout_o_fallo_de_transporte_devuelve_null_sin_lanzar() = runTest {
        val failures = listOf<Throwable>(
            SocketTimeoutException("timeout de socket"),
            HttpRequestTimeoutException("https://inference.devexpert.io/v1/images/generations", 40_000L),
            IOException("sin red"),
        )

        failures.forEach { failure ->
            val mock = MockImages { _, _ -> throw failure }

            val result = generator(client = AiHttpClientFactory.create(mock.engine)).generate(request())

            assertNull("El fallo ${failure::class.simpleName} debe devolver null", result)
            assertEquals("El fallo ${failure::class.simpleName} no debe reintentar", 1, mock.calls)
        }
    }

    // Escenario 5 (fase 2, variante): una respuesta que excede el timeout de peticion inyectado es null.
    @Test
    fun una_respuesta_lenta_que_excede_el_timeout_inyectado_devuelve_null() = runBlocking {
        val mock = MockImages { _, _ ->
            withContext(Dispatchers.IO) { Thread.sleep(1_000L) }
            okBody(encodedResponse(byteArrayOf(1)))
        }
        val generator = DevExpertDishImageGenerator(
            apiKey = KEY,
            client = AiHttpClientFactory.create(mock.engine),
            reducer = passThrough(),
            timeouts = DishImageTimeouts(connectMillis = 50L, socketMillis = 50L, requestMillis = 100L),
        )

        val result = generator.generate(request())

        assertNull("El timeout inyectado debe degradar a null", result)
        assertEquals(1, mock.calls)
    }

    // Escenario 6 (fase 2): sin clave no hay peticion.
    @Test
    fun sin_clave_no_hay_peticion() = runTest {
        listOf("", "   ").forEach { blank ->
            val mock = MockImages { _, _ -> okBody(encodedResponse(byteArrayOf(1))) }

            val result = generator(apiKey = blank, client = AiHttpClientFactory.create(mock.engine))
                .generate(request())

            assertNull("La clave '$blank' debe degradar a null", result)
            assertEquals("La clave '$blank' no debe hacer ninguna peticion", 0, mock.calls)
        }
    }

    // Escenario 9 (fase 2): la cancelacion se relanza, no se convierte en null.
    @Test
    fun la_cancelacion_se_propaga() = runTest {
        val started = CompletableDeferred<Unit>()
        val mock = MockImages { _, _ ->
            started.complete(Unit)
            awaitCancellation()
        }
        val generator = generator(client = AiHttpClientFactory.create(mock.engine))

        var propagated: CancellationException? = null
        val job = launch {
            try {
                generator.generate(request())
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

    // Escenario 10 (fase 2): transporte puro; la cacheKey no se reinterpreta ni viaja en la peticion.
    @Test
    fun el_adaptador_es_transporte_puro_y_no_usa_la_cache_key() = runTest {
        val bodies = mutableListOf<String>()
        val mock = MockImages { req, _ ->
            bodies += req.bodyText()
            okBody(encodedResponse(byteArrayOf(1)))
        }

        generator(client = AiHttpClientFactory.create(mock.engine)).generate(
            DishImageRequest(
                cacheKey = "dish-image-v1-paella-valenciana",
                prompt = "Fotografía gastronómica de paella valenciana",
                recipeId = "test-paella",
            ),
        )

        val body = bodies.single()
        assertEquals(
            "El prompt viaja tal cual (no se deriva del dominio)",
            "Fotografía gastronómica de paella valenciana",
            Json.parseToJsonElement(body).jsonObject["prompt"]!!.jsonPrimitive.content,
        )
        assertFalse("La cacheKey no viaja en el cuerpo", body.contains("dish-image-v1"))
        assertFalse("El recipeId no viaja en el cuerpo", body.contains("test-paella"))
    }

    // Escenario 2: con referencia, UNA sola peticion multipart a /images/edits con las cuatro partes.
    @Test
    fun con_referencia_hace_un_unico_post_multipart_a_edits_con_las_cuatro_partes() = runTest {
        val reference = styleReference()
        val responseBytes = byteArrayOf(0x0A, 0x0B, 0x0C)
        val mock = MockImages { _, _ -> okBody(encodedResponse(responseBytes)) }
        val generator = generator(reference = reference, client = AiHttpClientFactory.create(mock.engine))

        val result = generator.generate(request(prompt = "Fotografía gastronómica de paella valenciana"))

        assertEquals("El ajuste devuelve los bytes reducidos", responseBytes.toList(), result!!.toList())
        assertEquals("Exactamente una peticion (al ajuste)", 1, mock.calls)

        val req = mock.lastRequest
        assertEquals("https://inference.devexpert.io/v1/images/edits", req.url.toString())
        assertEquals(HttpMethod.Post, req.method)
        assertEquals("Bearer $KEY", req.headers[HttpHeaders.Authorization])

        val outgoingContentType = (req.body as OutgoingContent).contentType
        assertNotNull("La peticion debe declarar un Content-Type", outgoingContentType)
        assertTrue(
            "Content-Type fue '$outgoingContentType'",
            outgoingContentType!!.match(ContentType.MultiPart.FormData),
        )
        assertTrue(
            "El Content-Type debe traer boundary",
            outgoingContentType.toString().contains("boundary="),
        )

        val wire = req.body.toByteArray()
        val text = wire.decodeToString()
        assertTrue(
            "Falta la parte model",
            text.contains("Content-Disposition: form-data; name=\"model\""),
        )
        assertTrue("El modelo del ajuste es image-edit", text.contains("image-edit"))
        assertTrue(
            "Falta la parte prompt",
            text.contains("Content-Disposition: form-data; name=\"prompt\""),
        )
        assertTrue(
            "El prompt viaja identico",
            text.contains("Fotografía gastronómica de paella valenciana"),
        )
        assertTrue(
            "Falta la parte size",
            text.contains("Content-Disposition: form-data; name=\"size\""),
        )
        assertTrue("El tamano es 1024x1024", text.contains("1024x1024"))
        assertTrue(
            "La parte image debe traer name y filename",
            text.contains(
                "Content-Disposition: form-data; name=\"image\"; filename=\"dish-style-reference.jpg\"",
            ),
        )
        assertTrue("La referencia viaja como image/jpeg", text.contains("Content-Type: image/jpeg"))
        assertTrue(
            "Los bytes de la referencia viajan byte a byte",
            wire.containsBytes(reference.bytes),
        )
        assertEquals(
            "Exactamente las cuatro partes del contrato",
            4,
            Regex("Content-Disposition: form-data; name=").findAll(text).count(),
        )
        assertFalse("La cacheKey no viaja en el multipart", text.contains("dish-image-v1"))
        assertFalse("El recipeId no viaja en el multipart", text.contains("test-paella"))
    }

    // Escenario 1: sin referencia (o deshabilitada o vacia) el contrato de la fase 2 no cambia.
    @Test
    fun sin_referencia_utilizable_el_contrato_de_la_fase_2_no_cambia() = runTest {
        val variants = listOf<DishImageStyleReference?>(
            null,
            styleReference(bytes = byteArrayOf()),
        )
        variants.forEach { reference ->
            val mock = MockImages { _, _ -> okBody(encodedResponse(byteArrayOf(7))) }
            val generator = generator(reference = reference, client = AiHttpClientFactory.create(mock.engine))

            val result = generator.generate(request(prompt = "prompt sin ajuste"))

            assertNotNull("Sin referencia sigue devolviendo bytes", result)
            assertEquals("Sin referencia solo hay una peticion", 1, mock.calls)
            assertEquals(
                "Sin referencia se va a generaciones",
                "https://inference.devexpert.io/v1/images/generations",
                mock.lastRequest.url.toString(),
            )
            assertEquals(
                "prompt sin ajuste",
                Json.parseToJsonElement(mock.lastRequest.bodyText()).jsonObject["prompt"]!!.jsonPrimitive.content,
            )
        }

        // Interruptor del anclaje apagado: tambien va directo a texto->imagen.
        val mock = MockImages { _, _ -> okBody(encodedResponse(byteArrayOf(7))) }
        val disabled = generator(
            reference = styleReference(),
            referenceEnabled = false,
            client = AiHttpClientFactory.create(mock.engine),
        )

        assertNotNull(disabled.generate(request()))
        assertEquals(1, mock.calls)
        assertEquals(
            "https://inference.devexpert.io/v1/images/generations",
            mock.lastRequest.url.toString(),
        )
    }

    // Escenario 3: un rechazo definitivo del ajuste (400/404/405/413/415/422) degrada al texto.
    @Test
    fun un_rechazo_definitivo_del_ajuste_degrada_a_generations() = runTest {
        listOf(400, 404, 405, 413, 415, 422).forEach { status ->
            val fallbackBytes = byteArrayOf((status % 256).toByte(), 1)
            val mock = MockImages { req, _ ->
                if (req.isEdits()) {
                    respond(content = "", status = HttpStatusCode.fromValue(status))
                } else {
                    okBody(encodedResponse(fallbackBytes))
                }
            }
            val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

            val result = generator.generate(request(prompt = "prompt con respaldo"))

            assertEquals("El estado $status debe devolver el respaldo", fallbackBytes.toList(), result!!.toList())
            assertEquals("El estado $status exige edits + generations", 2, mock.calls)
            assertTrue("La primera peticion es al ajuste", mock.allRequests.first().isEdits())
            assertTrue("La segunda es a generaciones", mock.allRequests.last().isGenerations())
            assertEquals(
                "El respaldo reutiliza el mismo prompt",
                "prompt con respaldo",
                Json.parseToJsonElement(mock.allRequests.last().bodyText()).jsonObject["prompt"]!!.jsonPrimitive.content,
            )
        }
    }

    // Escenario 4: un 200 del ajuste con carga inutilizable tambien degrada al texto.
    @Test
    fun un_200_inutilizable_del_ajuste_degrada_a_generations() = runTest {
        val unusableBodies = listOf(
            "no soy json",
            "{}",
            "{\"data\":[]}",
            "{\"data\":[{\"b64_json\":\"\"}]}",
            "{\"data\":[{\"b64_json\":\"@@@no-base64\"}]}",
        )
        unusableBodies.forEach { body ->
            val fallbackBytes = byteArrayOf(0x42, 0x43)
            val mock = MockImages { req, _ ->
                if (req.isEdits()) okBody(body) else okBody(encodedResponse(fallbackBytes))
            }
            val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

            val result = generator.generate(request())

            assertEquals("La carga '$body' debe degradar al respaldo", fallbackBytes.toList(), result!!.toList())
            assertEquals("La carga '$body' exige edits + generations", 2, mock.calls)
        }
    }

    // Escenario 4 (variante): un 200 decodificable pero que el reductor no puede reducir tambien degrada.
    @Test
    fun un_200_que_el_reductor_no_puede_reducir_degrada_a_generations() = runTest {
        val fallbackBytes = byteArrayOf(0x51, 0x52)
        val mock = MockImages { req, _ ->
            if (req.isEdits()) {
                // Bytes validos en base64, pero el reductor "real" los rechazara (primera invocacion).
                okBody(encodedResponse(byteArrayOf(0x00, 0x01)))
            } else {
                okBody(encodedResponse(fallbackBytes))
            }
        }
        // Reductor que devuelve null SOLO en el ajuste y deja pasar el respaldo (misma disciplina que
        // un WebP indecodificable en produccion).
        val reducer = DishImageReducer { raw ->
            if (raw.contentEquals(byteArrayOf(0x00, 0x01))) null else raw
        }
        val generator = generator(
            reference = styleReference(),
            reducer = reducer,
            client = AiHttpClientFactory.create(mock.engine),
        )

        val result = generator.generate(request())

        assertEquals("El respaldo entrega los bytes", fallbackBytes.toList(), result!!.toList())
        assertEquals("El respaldo exige edits + generations", 2, mock.calls)
    }

    // Escenario 4 (variante): si el ajuste es inutilizable y el respaldo tambien falla, null sin lanzar.
    @Test
    fun si_el_ajuste_y_el_respaldo_fallan_devuelve_null_sin_lanzar() = runTest {
        val mock = MockImages { req, _ ->
            if (req.isEdits()) {
                okBody("{}")
            } else {
                respond(content = "", status = HttpStatusCode.InternalServerError)
            }
        }
        val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

        assertNull(generator.generate(request()))
        assertEquals("edits + generations, sin tercer intento", 2, mock.calls)
    }

    // Escenario 5: un fallo de transporte/timeout del ajuste devuelve null SIN respaldo (una peticion).
    @Test
    fun un_fallo_de_transporte_o_timeout_del_ajuste_devuelve_null_sin_respaldo() = runTest {
        val failures = listOf<Throwable>(
            SocketTimeoutException("timeout de socket"),
            HttpRequestTimeoutException("https://inference.devexpert.io/v1/images/edits", 40_000L),
            IOException("sin red"),
        )

        failures.forEach { failure ->
            val mock = MockImages { _, _ -> throw failure }
            val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

            val result = generator.generate(request())

            assertNull("El fallo ${failure::class.simpleName} debe devolver null", result)
            assertEquals(
                "El fallo ${failure::class.simpleName} no debe respaldar",
                1,
                mock.calls,
            )
        }
    }

    // Escenario 5 (variante): una respuesta del ajuste que excede el timeout inyectado es null sin respaldo.
    @Test
    fun una_respuesta_lenta_del_ajuste_excede_el_timeout_y_no_respalda() = runBlocking {
        val mock = MockImages { _, _ ->
            withContext(Dispatchers.IO) { Thread.sleep(1_000L) }
            okBody(encodedResponse(byteArrayOf(1)))
        }
        val generator = generator(
            reference = styleReference(),
            timeouts = DishImageTimeouts(connectMillis = 50L, socketMillis = 50L, requestMillis = 100L),
            client = AiHttpClientFactory.create(mock.engine),
        )

        val result = generator.generate(request())

        assertNull("El timeout del ajuste debe degradar a null", result)
        assertEquals("Sin respaldo ante timeout", 1, mock.calls)
    }

    // Escenario 6: los estados que no son rechazo del ajuste devuelven null sin respaldo (una peticion).
    @Test
    fun los_estados_que_no_son_rechazo_devuelven_null_sin_respaldo() = runTest {
        listOf(401, 403, 408, 429, 500).forEach { status ->
            val mock = MockImages { _, _ ->
                respond(content = "", status = HttpStatusCode.fromValue(status))
            }
            val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

            val result = generator.generate(request())

            assertNull("El estado $status debe devolver null", result)
            assertEquals("El estado $status no debe respaldar", 1, mock.calls)
        }
    }

    // Escenario 7: el respaldo nunca encadena ni reintenta (delete + error de respaldo = dos peticiones).
    @Test
    fun el_respaldo_nunca_encadena_ni_reintenta() = runTest {
        val mock = MockImages { req, _ ->
            if (req.isEdits()) {
                respond(content = "", status = HttpStatusCode.NotFound)
            } else {
                respond(content = "", status = HttpStatusCode.InternalServerError)
            }
        }
        val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

        val result = generator.generate(request())

        assertNull("El respaldo fallido devuelve null", result)
        assertEquals("Exactamente dos peticiones, sin tercer intento", 2, mock.calls)
        assertEquals("Un solo intento contra el ajuste", 1, mock.allRequests.count { it.isEdits() })
    }

    // Escenario 8: sin clave no hay ninguna peticion, tampoco al ajuste.
    @Test
    fun sin_clave_con_referencia_no_hay_ninguna_peticion() = runTest {
        listOf("", "   ").forEach { blank ->
            val mock = MockImages { _, _ -> okBody(encodedResponse(byteArrayOf(1))) }
            val generator = generator(
                apiKey = blank,
                reference = styleReference(),
                client = AiHttpClientFactory.create(mock.engine),
            )

            assertNull("La clave '$blank' debe degradar a null", generator.generate(request()))
            assertEquals("La clave '$blank' no debe hacer ninguna peticion", 0, mock.calls)
        }
    }

    // Escenario 9: la cancelacion se propaga en la fase de ajuste (no se convierte en null ni respalda).
    @Test
    fun la_cancelacion_se_propaga_en_la_fase_de_ajuste() = runTest {
        val started = CompletableDeferred<Unit>()
        val mock = MockImages { _, _ ->
            started.complete(Unit)
            awaitCancellation()
        }
        val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

        var propagated: CancellationException? = null
        val job = launch {
            try {
                generator.generate(request())
            } catch (cancellation: CancellationException) {
                propagated = cancellation
                throw cancellation
            }
        }

        started.await()
        assertEquals("La peticion al ajuste debe estar en vuelo", 1, mock.calls)
        job.cancelAndJoin()

        assertTrue("El job debe quedar cancelado", job.isCancelled)
        assertNotNull("Debe relanzarse CancellationException", propagated)
        assertEquals("La cancelacion no debe encadenar respaldo", 1, mock.calls)
    }

    // Escenario 9: la cancelacion tambien se propaga durante el respaldo por texto.
    @Test
    fun la_cancelacion_se_propaga_en_el_respaldo() = runTest {
        val started = CompletableDeferred<Unit>()
        val mock = MockImages { req, _ ->
            if (req.isEdits()) {
                respond(content = "", status = HttpStatusCode.NotFound)
            } else {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        val generator = generator(reference = styleReference(), client = AiHttpClientFactory.create(mock.engine))

        var propagated: CancellationException? = null
        val job = launch {
            try {
                generator.generate(request())
            } catch (cancellation: CancellationException) {
                propagated = cancellation
                throw cancellation
            }
        }

        started.await()
        assertEquals("Se debe haber alcanzado el respaldo", 2, mock.calls)
        job.cancelAndJoin()

        assertTrue("El job debe quedar cancelado", job.isCancelled)
        assertNotNull("Debe relanzarse CancellationException", propagated)
    }

    private fun generator(
        apiKey: String = KEY,
        client: io.ktor.client.HttpClient,
        reducer: DishImageReducer = passThrough(),
        reference: DishImageStyleReference? = null,
        referenceEnabled: Boolean = DishImagePolicy.STYLE_REFERENCE_ENABLED,
        timeouts: DishImageTimeouts = DishImagePolicy.TIMEOUTS,
    ) = DevExpertDishImageGenerator(
        apiKey = apiKey,
        client = client,
        reducer = reducer,
        timeouts = timeouts,
        styleReference = reference,
        styleReferenceEnabled = referenceEnabled,
    )

    private fun MockRequestHandleScope.okBody(content: String) = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}

private const val KEY = "test-key"

/** Reductor falso para el transporte en JVM puro: devuelve los bytes tal cual. */
private fun passThrough(): DishImageReducer = DishImageReducer { it }

private fun request(
    prompt: String = "Fotografía gastronómica de un plato casero",
) = DishImageRequest(cacheKey = "dish-image-v1-plato", prompt = prompt)

/** Referencia de estilo de prueba: bytes, media type y filename conocidos. */
private fun styleReference(
    bytes: ByteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04),
    mediaType: String = "image/jpeg",
    fileName: String = "dish-style-reference.jpg",
) = DishImageStyleReference(bytes = bytes, mediaType = mediaType, fileName = fileName)

/** Respuesta `{"data":[{"b64_json":"<bytes>"}]}` con el base64 pedido. */
private fun encodedResponse(bytes: ByteArray): String = Json.encodeToString(
    ImageGenerationResponse.serializer(),
    ImageGenerationResponse(
        listOf(ImageGenerationDatum(b64Json = Base64.getEncoder().encodeToString(bytes))),
    ),
)

private fun HttpRequestData.isEdits(): Boolean = url.encodedPath.endsWith("/images/edits")

private fun HttpRequestData.isGenerations(): Boolean = url.encodedPath.endsWith("/images/generations")

/** Contiene [needle] como subsecuencia de bytes (verifica los bytes crudos de la referencia). */
private fun ByteArray.containsBytes(needle: ByteArray): Boolean {
    if (needle.isEmpty()) return true
    outer@ for (start in 0..size - needle.size) {
        for (offset in needle.indices) {
            if (this[start + offset] != needle[offset]) continue@outer
        }
        return true
    }
    return false
}

/** Mock con contador de llamadas y captura de peticiones para inspeccionarlas en los tests. */
private class MockImages(
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

    val allRequests: List<HttpRequestData> get() = requests.toList()
}

private fun HttpRequestData.bodyText(): String =
    (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty()
