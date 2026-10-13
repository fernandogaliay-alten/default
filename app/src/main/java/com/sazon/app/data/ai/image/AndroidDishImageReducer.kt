package com.sazon.app.data.ai.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

/**
 * Reductor Android real: decodifica los bytes crudos del proveedor, los reduce a
 * [DishImagePolicy.TARGET_WIDTH_PX] de ancho (sin agrandar) y los **re-codifica siempre** a WebP q
 * [DishImagePolicy.WEBP_QUALITY] para fijar formato y tamano que la fase 3 guardara tal cual.
 *
 * Es el **unico** fichero de `data.ai.image` que importa `android.graphics` (misma disciplina que el
 * resto de la capa de datos, que aisla el codec Android de la logica pura). **Nunca lanza**: cualquier
 * fallo de decodificacion o compresion devuelve `null`.
 */
object AndroidDishImageReducer : DishImageReducer {

    override fun reduceToWebp(rawBytes: ByteArray): ByteArray? {
        val decoded = try {
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        } catch (failure: Exception) {
            null
        } ?: return null

        return try {
            val (targetWidth, targetHeight) = DishImagePolicy.scaledSize(decoded.width, decoded.height)
            val scaled = if (targetWidth != decoded.width || targetHeight != decoded.height) {
                decoded.scale(targetWidth, targetHeight, /* filter = */ true)
            } else {
                decoded
            }

            try {
                val output = ByteArrayOutputStream()
                val encoded = scaled.compress(
                    Bitmap.CompressFormat.WEBP_LOSSY,
                    DishImagePolicy.WEBP_QUALITY,
                    output,
                )
                if (encoded) output.toByteArray().takeIf { it.isNotEmpty() } else null
            } finally {
                // `Bitmap.scale` (createScaledBitmap) puede devolver el mismo bitmap si la geometria no cambia.
                if (scaled !== decoded) scaled.recycle()
            }
        } catch (failure: Exception) {
            null
        } finally {
            decoded.recycle()
        }
    }
}
