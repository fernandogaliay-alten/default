package com.sazon.app.data.ai.image

import com.sazon.app.data.RecipePhotoAssets
import java.util.Locale

/**
 * Referencia de estilo **ya cargada**, lista para ser la parte de fichero (`image`) del multipart de
 * `/images/edits` (fase 6, `docs/adr/0005-ai-dish-images.md`).
 *
 * No es un dato por plato ni un campo de la peticion de dominio ([DishImageRequest][com.sazon.app.domain.image.DishImageRequest]):
 * era la ampliacion "foto de referencia" que el ADR 0005 dejo **prevista** y que la fase 6 resuelve
 * como una **constante de la app** (un unico asset versionado), no como I/O del dominio. La clase solo
 * transporta bytes + media type + nombre de fichero: no sabe de donde salen ni los persiste.
 */
class DishImageStyleReference(
    /** Bytes del asset de referencia, tal cual (byte a byte los del APK: nunca se re-codifican). */
    val bytes: ByteArray,
    /** `Content-Type` de la parte (`image/jpeg` | `image/webp` | `image/png`). */
    val mediaType: String,
    /** Nombre de fichero de la parte (`filename` del `Content-Disposition`). */
    val fileName: String,
)

/**
 * Convencion de la **referencia de estilo** de la casa: el asset neutro **ya empaquetado y versionado**
 * de `recipe-photos` (`img/recipes/default.<ext>`), jamas una imagen subida por el usuario.
 *
 * Logica **pura** (sin Android, sin I/O): la existencia de un fichero se inyecta con un predicado
 * (`exists`), igual que [RecipePhotoAssets.resolve], de modo que la misma funcion sirve al test JVM y
 * al cargador Android ([AssetDishImageStyleReference]) sin duplicar reglas. Se resuelve **por
 * extensiones** porque el script de empaquetado (`scripts/fetch-recipe-photos.sh`) puede haber
 * convertido el asset a WebP: **no** se asume formato ni se inventa una ruta.
 */
object DishImageStyleReferencePolicy {

    /** Base del asset de referencia: la foto generica/neutra de `recipe-photos` (`img/recipes/default`). */
    val ASSET_BASE = "${RecipePhotoAssets.ROOT}/${RecipePhotoAssets.DEFAULT_KEY}"

    /** Extensiones que se prueban **en orden**, las mismas que acepta el empaquetado de fotos. */
    val EXTENSIONS = RecipePhotoAssets.EXTENSIONS

    /** Nombre base del fichero de la parte `image` (la extension la aporta el asset resuelto). */
    const val FILE_NAME_BASE = "dish-style-reference"

    /**
     * Primera ruta completa existente (`<base>.<ext>`) probando [EXTENSIONS] en orden, o `null` si no
     * existe ninguna. `null` = la app degrada al camino de texto->imagen (nunca una ruta inventada).
     */
    fun resolve(exists: (String) -> Boolean): String? = resolve(ASSET_BASE, exists)

    /**
     * Variante con [base] inyectable (misma logica que [resolve]): la usa el cargador Android para su
     * costura de test (una base inexistente debe resolver `null`); en produccion vale [ASSET_BASE].
     */
    fun resolve(base: String, exists: (String) -> Boolean): String? {
        for (extension in EXTENSIONS) {
            val path = "$base.$extension"
            if (exists(path)) return path
        }
        return null
    }

    /**
     * `Content-Type` de la referencia segun su extension. Se admite `jpeg` como alias de `jpg`; una
     * extension desconocida degrada a `application/octet-stream` (nunca lanza). Las extensiones reales
     * salen de [EXTENSIONS], asi que en produccion siempre es uno de los tres tipos de imagen.
     */
    fun mediaTypeFor(extension: String): String = when (extension.lowercase(Locale.ROOT)) {
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "png" -> "image/png"
        else -> "application/octet-stream"
    }

    /**
     * Nombre de fichero de la parte `image` derivado de la extension de [path]
     * (`"img/recipes/default.jpg"` -> `"dish-style-reference.jpg"`). Si [path] no trae extension, usa
     * [FILE_NAME_BASE] a secas. Determinista y sin I/O.
     */
    fun fileNameFor(path: String): String {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
        return if (extension.isEmpty()) FILE_NAME_BASE else "$FILE_NAME_BASE.$extension"
    }
}
