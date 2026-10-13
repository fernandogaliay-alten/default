package com.sazon.app.ui.artwork

/**
 * Capa **efectiva** que resulta de dibujar [RecipeArtwork] (capa base + capa superior):
 * `GENERATED` si llego la imagen generada, `LOCAL` si solo hay foto local y `FALLBACK` si no hay
 * ninguna de las dos (degradado + glifo).
 */
enum class RecipeArtworkImageLayer {
    FALLBACK,
    LOCAL,
    GENERATED,
}

/**
 * Precedencia de las capas de [RecipeArtwork] y objetivo del crossfade, **puros** y deterministas
 * (JVM, sin Compose): la misma entrada produce siempre la misma capa y el mismo alfa.
 *
 * Es **generico** (`<T : Any>`) a proposito: la app usa `ImageBitmap`, pero la logica se prueba con
 * `String` y sin Android ni Compose.
 */
object RecipeArtworkImagePrecedence {

    /**
     * Capa efectiva: `GENERATED` si hay imagen generada; `LOCAL` si hay foto local; si no,
     * `FALLBACK`. La generada **manda** cuando existe; la local es el respaldo.
     */
    fun <T : Any> layerOf(local: T?, generated: T?): RecipeArtworkImageLayer = when {
        generated != null -> RecipeArtworkImageLayer.GENERATED
        local != null -> RecipeArtworkImageLayer.LOCAL
        else -> RecipeArtworkImageLayer.FALLBACK
    }

    /**
     * Objetivo del alfa de la capa generada: `1f` cuando hay imagen generada, `0f` cuando no. La capa
     * base se pinta siempre, de modo que la transicion `0f -> 1f` es un crossfade sobre la local (o
     * sobre el degradado) sin dejar nunca un frame en blanco.
     */
    fun <T : Any> crossfadeTarget(generated: T?): Float = if (generated != null) 1f else 0f
}
