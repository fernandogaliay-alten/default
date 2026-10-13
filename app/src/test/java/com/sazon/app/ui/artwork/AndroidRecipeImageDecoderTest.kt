package com.sazon.app.ui.artwork

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * La costura Android del decodificador ([AndroidRecipeImageDecoder]) se ejercita con Robolectric en
 * **modo grafico nativo** (`@GraphicsMode(NATIVE)`, soportado en Linux/WSL), la unica forma de usar
 * `BitmapFactory` de verdad sin emulador ni dependencias nuevas. Los bytes WebP se generan **dentro**
 * del test (`Bitmap.compress(WEBP_LOSSY)`), asi que no se versiona ningun binario.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidRecipeImageDecoderTest {

    @Test
    fun unos_bytes_webp_reales_se_decodifican_a_image_bitmap() {
        val image = AndroidRecipeImageDecoder.decode(webpBytes(width = 64, height = 48))

        assertNotNull("Uns WebP validos deben decodificarse", image)
        assertEquals(64, image!!.width)
        assertEquals(48, image.height)
    }

    @Test
    fun unos_bytes_que_no_son_una_imagen_devuelven_null_sin_lanzar() {
        assertNull(AndroidRecipeImageDecoder.decode(ByteArray(0)))
        assertNull(AndroidRecipeImageDecoder.decode(byteArrayOf(0x01, 0x02, 0x03, 0x04)))
        assertNull(AndroidRecipeImageDecoder.decode("no soy una imagen".encodeToByteArray()))
    }
}

/** WebP real generado en el test (nunca versionado), decodificable por `BitmapFactory`. */
private fun webpBytes(width: Int, height: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.GREEN)
    val output = ByteArrayOutputStream()
    val encoded = bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, output)
    bitmap.recycle()
    check(encoded) { "No se pudo generar la imagen WebP de prueba" }
    return output.toByteArray()
}
