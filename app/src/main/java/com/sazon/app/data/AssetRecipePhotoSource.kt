package com.sazon.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sazon.app.domain.recipe.Recipe
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Costura Android de las fotos de plato: resuelve la ruta con la logica **pura** [RecipePhotoAssets]
 * (existe un fichero en `assets/`) y devuelve el `android.graphics.Bitmap` decodificado.
 *
 * - Devuelve `android.graphics.Bitmap` (**no** un tipo de Compose): `com.sazon.app.data` sigue **sin**
 *   depender de `ui`, igual que [AssetSeedRecipeSource] y [AssetTaxonomySource].
 * - La decodificacion corre en `Dispatchers.IO` para no bloquear el hilo de composicion al hacer
 *   scroll; hay una **cache LRU** en memoria de [MAX_CACHED_PHOTOS] bitmaps decodificados (los assets
 *   estan acotados a 960 px de ancho por el script de empaquetado).
 * - Devuelve `null` (**nunca** lanza) si no hay ningun asset utilizable o si la decodificacion falla.
 */
class AssetRecipePhotoSource(private val context: Context) {

    // LinkedHashMap en modo acceso (el mas reciente al final) con eviccion del mas antiguo: la cache
    // vive en el hilo de IO, pero se sincroniza por si el composable la consultara desde otro hilo.
    private val cache = object : LinkedHashMap<String, Bitmap>(MAX_CACHED_PHOTOS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > MAX_CACHED_PHOTOS
    }

    /** Foto de [recipe] (propia → categoria → por defecto) o `null` si no hay ninguna disponible. */
    suspend fun bitmapFor(recipe: Recipe): Bitmap? = withContext(Dispatchers.IO) {
        val path = RecipePhotoAssets.resolve(recipe.id, recipe.categoryId, ::exists)
            ?: return@withContext null

        cached(path)?.let { return@withContext it }

        val bitmap = decode(path) ?: return@withContext null
        synchronized(cache) { cache[path] = bitmap }
        bitmap
    }

    /** Existe el asset; cualquier fallo de lectura se trata como "no existe". */
    private fun exists(path: String): Boolean = try {
        context.assets.open(path).close()
        true
    } catch (exception: IOException) {
        false
    }

    /** Decodifica el asset en ARGB_8888 (la config por defecto de `BitmapFactory`); `null` si falla. */
    private fun decode(path: String): Bitmap? = try {
        context.assets.open(path).use { stream -> BitmapFactory.decodeStream(stream) }
    } catch (exception: IOException) {
        null
    }

    private fun cached(path: String): Bitmap? = synchronized(cache) { cache[path] }

    private companion object {
        /** Tope de bitmaps decodificados en memoria; suficiente para una pantalla con scroll. */
        const val MAX_CACHED_PHOTOS = 16
    }
}
