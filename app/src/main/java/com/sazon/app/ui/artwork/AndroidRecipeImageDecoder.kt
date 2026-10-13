package com.sazon.app.ui.artwork

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Costura Android del decodificador de imagenes generadas: **el unico** fichero de
 * `com.sazon.app.ui.artwork` que importa `android.graphics`.
 *
 * `BitmapFactory.decodeByteArray` es justo el `(ByteArray) -> ImageBitmap?` que espera
 * [DishImageArtworkLoader] con `T = ImageBitmap`. La decodificacion **no lanza** (mismo contrato que
 * la cadena local): bytes vacios, corruptos o que no son una imagen devuelven `null` y la UI conserva
 * la foto local.
 */
object AndroidRecipeImageDecoder : RecipeImageDecoder {

    override fun decode(bytes: ByteArray): ImageBitmap? {
        if (bytes.isEmpty()) return null
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (exception: Exception) {
            null
        }
    }
}
