package com.sazon.app.ui.artwork

/**
 * Reparto de la **banda solida** de la ficha segun el ancho de la tarjeta (OQ6 resuelta): una linea en
 * la tarjeta estrecha/compacta y tres filas en la ancha.
 */
enum class FichaBandLayout { ONE_LINE, THREE_ROWS }

/**
 * Decisiones **puras** de la variante **L2** (la imagen generada como capa de fondo del hueco y del
 * hero, ADR 0006): la relacion del hueco, el tratamiento tipografico segun la capa resuelta y el reparto
 * de la banda solida. Sin Compose y sin Android, de modo que toda la decision que hoy vive en
 * `@Composable` se prueba en JVM.
 *
 * La imagen en si la resuelve [RecipeArtwork]; esto solo responde a "hay imagen o no" a partir de la
 * capa efectiva ([RecipeArtworkImagePrecedence.layerOf]).
 */
object DishImageArtworkOverlay {

    /**
     * Relacion de aspecto del hueco (**16:9**), **unica** desde la estructura L2 unificada
     * (`recipe-card-l2-unified`): con la capa de fondo o sin ella, para que la aparicion del fondo no
     * cambie el layout (escenario 6). Ya no hay hueco 16:10.
     */
    const val HOLE_RATIO: Float = 16f / 9f

    /**
     * Reparto de la **banda solida** (ingredientes con cantidad) segun el ancho, no el ajuste de
     * imagenes: **una linea** cuando la tarjeta es estrecha o compacta y **tres filas** con la cantidad a
     * la derecha en cifras tabulares cuando es ancha (Resultados). En el caso imposible `wide && compact`
     * manda `compact` (determinista, sin excepcion).
     */
    fun bandLayout(wide: Boolean, compact: Boolean): FichaBandLayout = when {
        compact -> FichaBandLayout.ONE_LINE
        wide -> FichaBandLayout.THREE_ROWS
        else -> FichaBandLayout.ONE_LINE
    }

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
