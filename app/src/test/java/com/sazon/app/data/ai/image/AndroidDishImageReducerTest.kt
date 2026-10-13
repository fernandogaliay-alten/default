package com.sazon.app.data.ai.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.sazon.app.data.ai.AiHttpClientFactory
import com.sazon.app.domain.image.DishImageRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Escenarios 2, 3 y 8 de `docs/specs/dish-image-generator.md`: el codec Android real se ejercita con
 * Robolectric en **modo grafico nativo** (`@GraphicsMode(NATIVE)`, soportado en Linux/WSL), que es la
 * unica forma de usar `BitmapFactory`/`compress(WEBP_LOSSY)` de verdad sin emulador ni dependencias
 * nuevas. Las imagenes de prueba se generan **dentro** del test, asi que no se versiona ningun binario.
 *
 * El ultimo test recorre el camino extremo a extremo `b64_json -> WebP 960` con `MockEngine` y el
 * reductor real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidDishImageReducerTest {

    // Escenario 2: una fuente 1024x1024 sale como WebP de 960x960.
    @Test
    fun una_fuente_1024x1024_sale_como_webp_de_960() {
        val webp = AndroidDishImageReducer.reduceToWebp(pngBytes(1024, 1024))

        assertNotNull(webp)
        assertTrue("La salida debe ser WebP", webp!!.isWebp())
        val bitmap = decode(webp)
        assertEquals("Ancho objetivo", 960, bitmap.width)
        assertEquals("Alto proporcional", 960, bitmap.height)
    }

    // Escenario 3: un origen mas estrecho que 960 no se agranda y sigue saliendo WebP.
    @Test
    fun un_origen_menor_que_960_no_se_agranda_y_sigue_siendo_webp() {
        val webp = AndroidDishImageReducer.reduceToWebp(pngBytes(400, 300))

        assertNotNull(webp)
        assertTrue("La salida debe ser WebP", webp!!.isWebp())
        val bitmap = decode(webp)
        assertEquals(400, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    // Se re-codifica SIEMPRE a WebP, aunque el origen ya mida <= 960.
    @Test
    fun un_origen_que_ya_cabe_se_re_codifica_a_webp() {
        val webp = AndroidDishImageReducer.reduceToWebp(pngBytes(800, 600))

        assertNotNull(webp)
        assertTrue("Debe fijar el formato aunque no reduzca", webp!!.isWebp())
        assertEquals(800, decode(webp).width)
    }

    // Escenario 8: bytes que no son una imagen devuelven null sin lanzar.
    @Test
    fun unos_bytes_que_no_son_una_imagen_devuelven_null() {
        assertNull(AndroidDishImageReducer.reduceToWebp(byteArrayOf(0x01, 0x02, 0x03, 0x04)))
        assertNull(AndroidDishImageReducer.reduceToWebp(ByteArray(0)))
        assertNull(AndroidDishImageReducer.reduceToWebp("no soy una imagen".encodeToByteArray()))
    }

    // Camino extremo a extremo: `data[0].b64_json` (1024x1024) -> WebP de 960 con el reductor real.
    @Test
    fun el_camino_extremo_a_extremo_produce_webp_de_960() = runTest {
        val body = Json.encodeToString(
            ImageGenerationResponse.serializer(),
            ImageGenerationResponse(
                listOf(
                    ImageGenerationDatum(
                        b64Json = Base64.getEncoder().encodeToString(pngBytes(1024, 1024)),
                    ),
                ),
            ),
        )
        val engine = MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val generator = DevExpertDishImageGenerator(
            apiKey = "test-key",
            client = AiHttpClientFactory.create(engine),
        )

        val webp = generator.generate(
            DishImageRequest(cacheKey = "dish-image-v1-plato", prompt = "Fotografía gastronómica de un plato"),
        )

        assertNotNull("El adaptador real debe devolver la imagen reducida", webp)
        assertTrue("La salida debe ser WebP", webp!!.isWebp())
        assertEquals(960, decode(webp).width)
    }

    /** PNG solido generado en el test (nunca versionado): fuente real que `BitmapFactory` decodifica. */
    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val output = ByteArrayOutputStream()
        val encoded = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        check(encoded) { "No se pudo generar la imagen de prueba" }
        return output.toByteArray()
    }

    private fun decode(bytes: ByteArray): Bitmap =
        checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "La salida debe ser una imagen decodificable"
        }
}

/** Cabecera WebP (`RIFF`...`WEBP`). */
private fun ByteArray.isWebp(): Boolean =
    size >= 12 &&
        copyOfRange(0, 4).decodeToString() == "RIFF" &&
        copyOfRange(8, 12).decodeToString() == "WEBP"
