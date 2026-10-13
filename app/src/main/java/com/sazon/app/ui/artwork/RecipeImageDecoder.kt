package com.sazon.app.ui.artwork

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodifica los bytes de una imagen generada (WebP de la fase 2) a una imagen pintable por Compose.
 *
 * Es la costura que aisla el codec: [DishImageArtworkLoader] recibe un `(ByteArray) -> T?` y en la app
 * es [AndroidRecipeImageDecoder], el **unico** fichero de `ui.artwork` que importa `android.graphics`.
 * En los tests JVM se sustituye por un decodificador falso.
 *
 * Contrato de fallo: **nunca lanza**; devuelve `null` si los bytes no son una imagen decodificable.
 */
fun interface RecipeImageDecoder {
    /** Imagen decodificada de [bytes], o `null` si el codec falla. */
    fun decode(bytes: ByteArray): ImageBitmap?
}
