package com.sazon.app.data.ai.image

import android.content.Context
import java.io.IOException

/**
 * Costura Android de la **referencia de estilo** (fase 6): lee del APK el asset neutro ya empaquetado
 * y versionado que resuelve la logica **pura** [DishImageStyleReferencePolicy] y devuelve la
 * [DishImageStyleReference] lista para el multipart de `/images/edits`.
 *
 * Es el **segundo** fichero de `data.ai.image` que importa `android.*` (el otro es
 * [AndroidDishImageReducer]); **no** importa `android.graphics` (no decodifica ni re-codifica: la
 * referencia viaja byte a byte). La lectura es *best-effort* (patron de las costuras de assets):
 * ausencia, `IOException` o bytes vacios devuelven `null` **sin lanzar**, y el adaptador degrada al
 * camino de texto->imagen de la fase 2.
 *
 * **No** hay subida del usuario, ni permisos, ni galeria, ni camara, ni persistencia: la referencia es
 * una constante de la app. [assetBase] es inyectable para poder probar el camino "no existe" sin
 * depender del contenido real.
 */
class AssetDishImageStyleReference(
    private val context: Context,
    private val assetBase: String = DishImageStyleReferencePolicy.ASSET_BASE,
) {

    /** Referencia de estilo cargada del asset versionado, o `null` si no hay ninguna legible. */
    fun load(): DishImageStyleReference? {
        val path = DishImageStyleReferencePolicy.resolve(assetBase, ::assetExists) ?: return null
        val bytes = read(path) ?: return null
        return DishImageStyleReference(
            bytes = bytes,
            mediaType = DishImageStyleReferencePolicy.mediaTypeFor(path.substringAfterLast('.')),
            fileName = DishImageStyleReferencePolicy.fileNameFor(path),
        )
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
