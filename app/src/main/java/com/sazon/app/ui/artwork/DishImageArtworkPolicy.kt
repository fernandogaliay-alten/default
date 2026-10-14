package com.sazon.app.ui.artwork

/**
 * Politica de la **imagen de plato en la UI** en un unico sitio (fase 4 de
 * `docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`): limite de generaciones
 * concurrentes, duracion del crossfade y tope del LRU de imagenes decodificadas. Constantes puras,
 * sin Compose y sin Android (mismo patron que `data.image.DishImageCachePolicy`).
 *
 * Separado de la politica de datos a proposito: aqui viven los parametros de **pintado** (cuantas
 * generaciones caben a la vez, cuanto dura el fade y cuantos bitmaps se retienen), no la politica de
 * cache de bytes ni la del proveedor.
 */
object DishImageArtworkPolicy {

    /**
     * Maximo de resoluciones de imagen generada **en vuelo** a la vez (propuesta §6.4): acota el
     * gasto de cuota mientras se hace scroll en una rejilla.
     */
    const val MAX_CONCURRENT_GENERATIONS = 2

    /** Duracion del crossfade estandar cuando la imagen generada aparece sobre la ficha tipografica. */
    const val CROSSFADE_MILLIS = 250

    /**
     * Tope del LRU de **imagenes decodificadas** generadas (mismo valor que
     * `DishImageCachePolicy.MAX_MEMORY_ENTRIES`); es el **unico** LRU de imagenes de receta desde que
     * `retire-recipe-photos` retiro la cadena local.
     */
    const val MAX_DECODED_IMAGES = 16
}
