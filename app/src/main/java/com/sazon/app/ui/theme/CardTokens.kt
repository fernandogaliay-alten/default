package com.sazon.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cromo de la **tarjeta** (ficha de receta, ADR 0006) resuelto por variante clara/oscura.
 *
 * Es un contrato **de tarjeta**, no un rol de `MaterialTheme.colorScheme`: en oscuro la tarjeta
 * necesita una superficie y un **límite** propios que no alteran `darkSurface` ni el navegador, las
 * hojas o el resto de pantallas. `boundaryWidth == 0.dp` equivale a "sin borde" (y [boundary] es
 * transparente); en oscuro el borde **sustituye a la sombra** como elevación, porque la sombra no se
 * ve sobre un fondo oscuro.
 */
data class SazonCardColors(
    val surface: Color,
    val boundary: Color,
    val boundaryWidth: Dp,
)

/**
 * Tokens de la tarjeta declarados por el frontmatter de `DESIGN.md` y el ADR 0006
 * (`components.recipe-card.backgroundDark`/`borderDark`), más la **política pura** del tinte cálido
 * de la cocina.
 *
 * En oscuro, superficie `darkCardSurface` `#3A2E26` + borde `darkCardOutline` `#7D6A5C` dan
 * **3,26:1** con `darkBackground` `#241C16` (≥ 3:1 de WCAG 1.4.11; el `darkSurface` anterior daba
 * 1,15:1 y el relleno nuevo solo 1,28:1, por eso el límite es el **borde**). `LocalSazonCardColors`
 * lo publica a las pantallas a traves de `SazonTheme`. Sus consumidores son la ficha tipografica de
 * `recipe-card-typographic` (`ui/home/RecipeFicha.kt`, en Inicio, Resultados y la rejilla de Favoritos)
 * y la miniatura de la lista de Favoritos; antes de esa feature nacieron sin consumidores, igual que
 * `domain.image` nació antes de su UI.
 */
object SazonCardTokens {

    /**
     * Alfa del **tinte cálido de la cocina** sobre la superficie de la tarjeta oscura. La banda
     * documentada es **12–16 %** (prototipo C3: el tinte "no cambia la luminancia"); este valor debe
     * mantener el relleno por debajo del umbral de contraste y el borde como límite (ver
     * `CardTokensTest`).
     */
    const val TINT_ALPHA: Float = 0.14f

    /** Claro: la tarjeta es la `surface` blanca del tema y **no** lleva borde. */
    val light: SazonCardColors = SazonCardColors(
        surface = SazonSurface,
        boundary = Color.Transparent,
        boundaryWidth = 0.dp,
    )

    /** Oscuro: superficie y borde propios para no perderse en `darkBackground`. */
    val dark: SazonCardColors = SazonCardColors(
        surface = SazonDarkCardSurface,
        boundary = SazonDarkCardOutline,
        boundaryWidth = 1.dp,
    )

    /** Variante segun el tema: `true` devuelve [dark], `false` [light]. */
    fun of(dark: Boolean): SazonCardColors = if (dark) this.dark else light

    /**
     * Relleno teñido por la **cocina**: composición **pura** y determinista del [accent] sobre
     * [surface] con el `lerp` de Compose (interpolación en **Oklab**, no por componente en sRGB), sin
     * Android, sin reloj y sin `Random`. Dos llamadas iguales devuelven el mismo color.
     */
    fun tintedSurface(surface: Color, accent: Color, alpha: Float = TINT_ALPHA): Color =
        lerp(surface, accent, alpha)
}

/**
 * Contrato de tarjeta vigente para el arbol de composicion. Lo provee `SazonTheme` segun `darkTheme`;
 * su valor por defecto es [SazonCardTokens.light] (blanco y sin borde), de modo que previews, tests y
 * componentes aislados quedan igual que hoy sin cablear nada.
 */
val LocalSazonCardColors = staticCompositionLocalOf { SazonCardTokens.light }
