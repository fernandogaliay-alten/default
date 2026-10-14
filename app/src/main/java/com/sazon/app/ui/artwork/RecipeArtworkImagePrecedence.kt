package com.sazon.app.ui.artwork

import com.sazon.app.data.image.DishImageOrigin

/**
 * Capa **efectiva** que resulta de resolver la imagen de plato de una receta: `GENERATED` si llego la
 * imagen generada y `TYPOGRAPHIC` si no (la ficha tipografica, que es la **capa base**: color de la
 * superficie + marca de agua del glifo). Ya **no** existe la capa `LOCAL`: la cadena de fotos local deja
 * de ser parte de la imagen de receta (la borra `retire-recipe-photos`), de modo que la base sin imagen
 * es siempre la tipografia.
 */
enum class RecipeArtworkImageLayer {
    TYPOGRAPHIC,
    GENERATED,
}

/**
 * Precedencia de la capa de fondo de [RecipeArtwork] y objetivo del crossfade, **puros** y
 * deterministas (JVM, sin Compose): la misma entrada produce siempre la misma capa y el mismo alfa.
 *
 * Es **generico** (`<T : Any>`) a proposito: la app usa `ImageBitmap`, pero la logica se prueba con
 * `String` y sin Android ni Compose.
 */
object RecipeArtworkImagePrecedence {

    /**
     * Capa efectiva: `GENERATED` si hay imagen generada y `TYPOGRAPHIC` si no (la ficha tipografica del
     * llamante queda visible).
     */
    fun <T : Any> layerOf(generated: T?): RecipeArtworkImageLayer =
        if (generated != null) RecipeArtworkImageLayer.GENERATED else RecipeArtworkImageLayer.TYPOGRAPHIC

    /**
     * Objetivo del alfa de la capa de fondo: `1f` cuando hay imagen generada y `0f` cuando no. La base
     * tipografica se pinta siempre, de modo que la transicion `0f -> 1f` es un crossfade sobre la
     * tipografia sin dejar nunca un frame en blanco ni mover el layout.
     */
    fun <T : Any> crossfadeTarget(generated: T?): Float = if (generated != null) 1f else 0f

    /**
     * **Origen resuelto** de la imagen que se pinta, para el diagnostico (solo debug). Puro y generico
     * (`<T : Any>`, igual que [layerOf]) para poder probarlo en JVM sin Android ni Compose:
     *
     * - con imagen generada, `SEED` si [resolvedOrigin] es [DishImageOrigin.SEED] (los bytes venian del
     *   set semilla) y `GENERATED` en cualquier otro caso;
     * - sin imagen, `GLYPH`: la base tipografica (marca de agua del glifo).
     *
     * `DishImageOrigin.CATEGORY` (la foto local) queda **sin productor** en la tarjeta/hero desde esta
     * feature; lo limpia `retire-recipe-photos`. No cambia ninguna de las capas: solo las clasifica a
     * posteriori.
     */
    fun <T : Any> originOf(
        generated: T?,
        resolvedOrigin: DishImageOrigin?,
    ): DishImageOrigin = when {
        generated != null ->
            if (resolvedOrigin == DishImageOrigin.SEED) DishImageOrigin.SEED else DishImageOrigin.GENERATED
        else -> DishImageOrigin.GLYPH
    }
}
