package com.sazon.app.data.image

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Escenario 13 de `docs/specs/dish-image-cache.md`: la **costura Android** de la cache, con Robolectric
 * `@Config(sdk = [33])` (sin emulador). Comprueba que la raiz es `cacheDir/dish-images` y que una
 * resolucion real escribe el fichero **dentro** de `cacheDir` y **no** en `filesDir` (que es de Room).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidDishImageCacheTest {

    @Test
    fun la_raiz_es_el_subdirectorio_dish_images_de_cache_dir() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals(File(context.cacheDir, "dish-images"), AndroidDishImageCache.root(context))
        assertEquals("dish-images", AndroidDishImageCache.root(context).name)
    }

    @Test
    fun la_resolucion_escribe_bajo_cache_dir_y_no_bajo_files_dir() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheKey = "dish-image-v2-arroz-con-pollo-criollo"
        val bytes = webpBytes(16)
        val resolver = DishImageResolver(
            disk = FileDishImageCache(AndroidDishImageCache.root(context)),
            memory = MemoryDishImageCache(),
            generator = object : DishImageGenerator {
                override suspend fun generate(request: DishImageRequest): ByteArray = bytes
            },
            ioDispatcher = Dispatchers.Unconfined,
        )

        val result = resolver.imageFor(DishImageRequest(cacheKey = cacheKey, prompt = "plato de prueba"))

        assertNotNull(result)
        val written = File(context.cacheDir, "dish-images/$cacheKey.webp")
        assertTrue("El fichero debe existir bajo cacheDir", written.isFile)
        assertTrue(
            "Ruta real: ${written.absolutePath}",
            written.absolutePath.startsWith(context.cacheDir.absolutePath + File.separator),
        )
        assertFalse(
            "No debe escribirse bajo filesDir",
            written.absolutePath.startsWith(context.filesDir.absolutePath + File.separator),
        )
        assertFalse(
            "filesDir no debe ganar un subdirectorio dish-images",
            File(context.filesDir, "dish-images").exists(),
        )
    }
}

/** Bytes con cabecera WebP (`RIFF`...`WEBP`), el formato que fija la fase 2. */
private fun webpBytes(size: Int = 12): ByteArray {
    val bytes = ByteArray(size.coerceAtLeast(12))
    "RIFF".toByteArray().copyInto(bytes, 0)
    "WEBP".toByteArray().copyInto(bytes, 8)
    return bytes
}
