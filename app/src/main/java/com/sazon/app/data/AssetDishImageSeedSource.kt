package com.sazon.app.data

import android.content.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Costura Android del set semilla: resuelve la ruta con la logica **pura** [DishImageSeedAssets]
 * (existe un fichero en `assets/`) y devuelve sus **bytes** (la imagen ya viene codificada: no hay
 * decodificacion ni `android.graphics`).
 *
 * - Es el **unico** fichero nuevo de la fase 7 que importa `android.*`, y vive en `data` junto a las
 *   demas costuras de assets ([AssetSeedRecipeSource], [AssetTaxonomySource]).
 * - La lectura corre en [Dispatchers.IO]; el acierto lo cachea el resolutor (memoria) y el loader de
 *   `ui.artwork` (decodificadas), asi que **no** hay LRU propia que duplique cache.
 * - Devuelve `null` (**nunca** lanza) si el asset no existe, la lectura falla o los bytes estan vacios;
 *   [root] es inyectable para poder probar el camino "no existe" sin depender del contenido real.
 */
class AssetDishImageSeedSource(
    private val context: Context,
    private val root: String = DishImageSeedAssets.ROOT,
) : DishImageSeedSource {

    override suspend fun bytesFor(cacheKey: String): ByteArray? = withContext(Dispatchers.IO) {
        val path = DishImageSeedAssets.resolve(cacheKey, ::assetExists, root) ?: return@withContext null
        read(path)
    }

    /** Existe el asset; cualquier fallo de lectura se trata como "no existe". */
    private fun assetExists(path: String): Boolean = try {
        context.assets.open(path).close()
        true
    } catch (exception: IOException) {
        false
    }

    /** Lee los bytes del asset; `null` si la lectura falla o el fichero esta vacio. Nunca lanza. */
    private fun read(path: String): ByteArray? = try {
        context.assets.open(path).use { stream -> stream.readBytes() }.takeIf { it.isNotEmpty() }
    } catch (exception: IOException) {
        null
    }
}
