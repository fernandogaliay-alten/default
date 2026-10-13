package com.sazon.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.image.DishImageCacheKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Escenario 9 de `docs/specs/dish-image-seed.md`: la **costura Android** del set semilla lee el asset
 * real empaquetado en el APK con Robolectric `@Config(sdk = [33])` (sin emulador, sin red y sin cuota),
 * y degrada en silencio (`null`, **sin lanzar**) cuando la clave no esta en el set o la raiz no existe.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AssetDishImageSeedSourceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun el_asset_real_de_un_plato_del_set_devuelve_bytes_no_vacios_con_cabecera_real() = runBlocking {
        val bytes = AssetDishImageSeedSource(context)
            .bytesFor(DishImageCacheKey.of("Arroz con pollo criollo"))

        assertNotNull("El asset real del plato debe leerse", bytes)
        assertTrue("Los bytes no deben estar vacios", bytes!!.isNotEmpty())
        assertTrue(
            "La cabecera debe ser de una imagen real (WebP, JPEG o PNG)",
            bytes.hasImageHeader(),
        )
    }

    @Test
    fun una_clave_fuera_del_set_devuelve_null_sin_lanzar() = runBlocking {
        val bytes = AssetDishImageSeedSource(context)
            .bytesFor(DishImageCacheKey.of("Plato que no existe"))

        assertNull(bytes)
    }

    @Test
    fun una_clave_insegura_devuelve_null_sin_lanzar() = runBlocking {
        val source = AssetDishImageSeedSource(context)

        assertNull(source.bytesFor("../../etc/passwd"))
        assertNull(source.bytesFor(""))
    }

    @Test
    fun una_raiz_inexistente_devuelve_null_sin_lanzar() = runBlocking {
        val bytes = AssetDishImageSeedSource(context, root = "img/dishes/seed-inexistente")
            .bytesFor(DishImageCacheKey.of("Arroz con pollo criollo"))

        assertNull(bytes)
    }
}

/** Cabecera de un WebP (`RIFF`...`WEBP`), un JPEG (`ff d8 ff`) o un PNG (`89 PNG`). */
private fun ByteArray.hasImageHeader(): Boolean = when {
    size >= 12 &&
        copyOfRange(0, 4).decodeToString() == "RIFF" &&
        copyOfRange(8, 12).decodeToString() == "WEBP" -> true

    size >= 3 &&
        this[0] == 0xFF.toByte() &&
        this[1] == 0xD8.toByte() &&
        this[2] == 0xFF.toByte() -> true

    size >= 8 &&
        this[0] == 0x89.toByte() &&
        copyOfRange(1, 4).decodeToString() == "PNG" -> true

    else -> false
}
