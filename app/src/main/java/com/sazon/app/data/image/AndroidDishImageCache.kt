package com.sazon.app.data.image

import android.content.Context
import java.io.File

/**
 * Costura Android de la cache de imagenes de plato: **el unico** fichero de `com.sazon.app.data.image`
 * que importa `android.*`.
 *
 * La raiz vive bajo el directorio de cache de la app (`cacheDir/dish-images`) y **nunca** bajo el
 * almacenamiento de ficheros: la cache es descartable por el sistema en cualquier momento y no debe
 * mezclarse con el almacenamiento de Room (Perfil, Favoritos, Lote). El resolutor que la usa se
 * construye en la fase 4; aqui **no** se cablea.
 */
object AndroidDishImageCache {

    /** Raiz de la cache: `cacheDir/dish-images`, creada de forma perezosa por `FileDishImageCache`. */
    fun root(context: Context): File = File(context.cacheDir, DishImageCachePolicy.SUBDIRECTORY)
}
