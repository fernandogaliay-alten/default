package com.sazon.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Tokens del **texto y del `scrim` sobre la imagen** de la variante **L2** (la imagen generada como capa
 * de fondo del hueco y del hero, ADR 0006). Es el **unico sitio** con estos valores: `ui/artwork` los
 * consume **por roles** (0 hex sueltos fuera de `ui/theme`), igual que [SazonCardTokens] con el cromo de
 * la tarjeta.
 *
 * El `scrim` es un degradado vertical que oscurece la imagen lo justo para que el texto blanco se lea
 * (accesibilidad de `DESIGN.md`: "degradado sutil bajo el texto blanco"); los valores salen del
 * prototipo L2 (`docs/design/concepts/typographic-cards/prototipo-c3-imagenes.html`) y de las capturas
 * `dish-artwork-as-card-background/{tarjeta-con-imagen,detalle-con-imagen}`. La variante oscura va algo
 * mas cerrada porque la imagen se apoya en `darkBackground`.
 */
object CardImageOverlay {

    /** Categoria y titulo **sobre la imagen**: blanco puro (`#FFFFFF`). */
    val onImageText: Color = Color(0xFFFFFFFF)

    /** Versalitas de categoria **sobre la imagen**: blanco al 86 % (`rgba(255,255,255,.86)`). */
    val onImageVariant: Color = Color(0xDBFFFFFF)

    /** Hairline de las filas de ingredientes **sobre la imagen**: blanco al 32 %. */
    val onImageHairline: Color = Color(0x52FFFFFF)

    // Degradado `to top` del prototipo, expresado de arriba (stop 0) a abajo (stop 1): el color mas
    // claro/asentado en el centro-alto (52 % desde abajo) y el mas cerrado al pie de la imagen.
    private val scrimLightStops: Array<out Pair<Float, Color>> = arrayOf(
        0f to Color(0x6B140C08), // rgba(20,12,8,.42)
        0.48f to Color(0x2E140C08), // rgba(20,12,8,.18)
        1f to Color(0xC7140C08), // rgba(20,12,8,.78)
    )

    private val scrimDarkStops: Array<out Pair<Float, Color>> = arrayOf(
        0f to Color(0x800A0604), // rgba(10,6,4,.5)
        0.48f to Color(0x380A0604), // rgba(10,6,4,.22)
        1f to Color(0xD10A0604), // rgba(10,6,4,.82)
    )

    /**
     * Paradas del `scrim` para la variante [dark] (oscuras mas cerradas); util para inspeccion y tests.
     */
    fun scrimStops(dark: Boolean): List<Pair<Float, Color>> =
        (if (dark) scrimDarkStops else scrimLightStops).toList()

    /**
     * `scrim` de contraste que cubre la imagen: degradado vertical con las paradas del prototipo
     * (arriba oscuro suave, centro mas claro, pie cerrado). Se pinta **encima** de la imagen y **debajo**
     * de la tipografia.
     */
    fun scrimBrush(dark: Boolean): Brush {
        val stops = if (dark) scrimDarkStops else scrimLightStops
        return Brush.verticalGradient(stops[0], stops[1], stops[2])
    }
}
