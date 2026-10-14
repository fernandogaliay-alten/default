package com.sazon.app.ui.artwork

/**
 * Decisiones **puras** de la variante **L2** (la imagen generada como capa de fondo del hueco y del
 * hero, ADR 0006): la relacion del hueco y el tratamiento tipografico segun la capa resuelta. Sin
 * Compose y sin Android, de modo que toda la decision que hoy vive en `@Composable` se prueba en JVM.
 *
 * La imagen en si la resuelve [RecipeArtwork]; esto solo responde a "hay imagen o no" a partir de la
 * capa efectiva ([RecipeArtworkImagePrecedence.layerOf]).
 */
object DishImageArtworkOverlay {

    /**
     * Relacion de aspecto del hueco con la capa de fondo (**16:9**), fija con imagen o sin ella para que
     * la aparicion del fondo no cambie el layout (escenario 6). El hueco 16:10 sigue siendo la ficha
     * sin el ajuste encendido.
     */
    const val HOLE_RATIO: Float = 16f / 9f

    /**
     * Categoria y titulo se pintan con el tratamiento **sobre imagen** (blanco, con `scrim` debajo)
     * cuando la capa es [RecipeArtworkImageLayer.GENERATED], y con el tratamiento **sobre superficie**
     * (`onSurface`/`onSurfaceVariant`) cuando es tipografica.
     */
    fun headingOnImage(layer: RecipeArtworkImageLayer): Boolean =
        layer == RecipeArtworkImageLayer.GENERATED

    /**
     * La **marca de agua** del glifo (identidad de categoria) se pinta **solo** sin imagen, para que dos
     * recetas de la misma categoria y cocina no se vean identicas; con imagen, la imagen es la textura.
     */
    fun watermarkVisible(layer: RecipeArtworkImageLayer): Boolean =
        layer == RecipeArtworkImageLayer.TYPOGRAPHIC

    /**
     * Alfa objetivo de la capa de fondo para [layer], derivado de la **unica** regla de precedencia
     * ([RecipeArtworkImagePrecedence.crossfadeTarget]): `1f` con imagen y `0f` sin ella. La duracion del
     * fundido vive en un unico sitio ([DishImageArtworkPolicy.CROSSFADE_MILLIS]).
     */
    fun crossfadeAlpha(layer: RecipeArtworkImageLayer): Float =
        RecipeArtworkImagePrecedence.crossfadeTarget(
            generated = layer.takeIf { it == RecipeArtworkImageLayer.GENERATED },
        )
}
