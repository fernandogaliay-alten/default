package com.sazon.app.data.ai.image

import com.sazon.app.data.ai.AiHttpClientFactory
import com.sazon.app.domain.image.DishImageRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
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
 * Escenarios 1, 4, 5, 6, 9 y 10 de `docs/specs/dish-image-generator.md` con `MockEngine` y un
 * [DishImageReducer] falso: el transporte se prueba en **JVM puro** (sin Robolectric y sin Android).
 * Sin red real ni clave real: la clave es ficticia (`test-key`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevExpertDishImageGeneratorTest {

    // Escenario 1: una unica peticion POST con URL, cabeceras y cuerpo exactos; devuelve bytes no nulos.
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

    // Escenario 4: cualquier error HTTP devuelve null sin lanzar y en una sola peticion (sin reintentos).
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

    // Escenario 5: un timeout o un fallo de transporte devuelve null sin lanzar (una sola peticion).
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

    // Escenario 5 (variante): una respuesta que excede el timeout de peticion inyectado devuelve null.
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

    // Escenario 6: sin clave no hay peticion.
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

    // Escenario 9: la cancelacion se relanza, no se convierte en null.
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

    // Escenario 10: transporte puro; la cacheKey no se reinterpreta ni viaja en la peticion de red.
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

    private fun generator(
        apiKey: String = KEY,
        client: io.ktor.client.HttpClient,
        reducer: DishImageReducer = passThrough(),
    ) = DevExpertDishImageGenerator(apiKey = apiKey, client = client, reducer = reducer)

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

/** Respuesta `{"data":[{"b64_json":"<bytes>"}]}` con el base64 pedido. */
private fun encodedResponse(bytes: ByteArray): String = Json.encodeToString(
    ImageGenerationResponse.serializer(),
    ImageGenerationResponse(
        listOf(ImageGenerationDatum(b64Json = Base64.getEncoder().encodeToString(bytes))),
    ),
)

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
}

private fun HttpRequestData.bodyText(): String =
    (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty()
