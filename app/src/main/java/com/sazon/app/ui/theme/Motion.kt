package com.sazon.app.ui.theme

/**
 * Tokens de **movimiento** de `DESIGN.md` (seccion `motion` de su frontmatter).
 *
 * Lenguaje unico de movimiento de la app: un fundido con un deslizamiento corto, **una sola duracion**
 * y **una sola distancia** para los cuatro frentes (navegacion, overlays, pasos y aparicion de
 * contenido), coherente con el caracter "tranquilo" del documento de diseno. La capa Compose los
 * mapea en `com.sazon.app.ui.motion.MotionContent` (`EASING` `standard` -> `FastOutSlowInEasing`).
 *
 * Objeto **puro** (solo `Int`/`String`/`Boolean`: sin imports de Compose ni Android), para que su test
 * sea JVM puro, como `MotionPolicy`. `MotionTokensTest` lo compara exactamente con `DESIGN.md`.
 */
object SazonMotion {
    /** Duracion de **toda** transicion, en milisegundos. */
    const val DURATION_MILLIS = 240

    /** Distancia del deslizamiento que acompana al fundido, en dp. */
    const val SLIDE_OFFSET_DP = 24

    /** Curva de aceleracion declarada en `DESIGN.md`; Compose la mapea a `FastOutSlowInEasing`. */
    const val EASING = "standard"

    /** Orden de capas: la capa **entrante** se pinta por encima de la saliente. */
    const val ENTER_ON_TOP = true
}
