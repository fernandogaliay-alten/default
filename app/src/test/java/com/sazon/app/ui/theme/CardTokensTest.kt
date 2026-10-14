package com.sazon.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG y tinte cálido de la **tarjeta** (escenarios 2-5 de
 * `docs/specs/card-typographic-tokens-and-design.md`). Es un test JVM puro: no necesita emulador ni
 * Android framework (solo `Color` y `dp` de Compose).
 *
 * El vector clave del ADR 0006: en oscuro el **límite** de la tarjeta es el **borde**
 * (`darkCardOutline` `#7D6A5C` vs `darkBackground` `#241C16` = 3,26:1); el relleno (`#3A2E26`, 1,28:1)
 * y el antiguo `darkSurface` (`#312720`, 1,15:1) están por debajo del 3:1, y esas dos aserciones
 * documentan el defecto para que la métrica no pueda pasar en vacío.
 */
class CardTokensTest {

    @Test
    fun el_borde_de_la_tarjeta_cumple_el_3_a_1_de_wcag_1_4_11() {
        val ratio = contrastRatio(SazonCardTokens.dark.boundary.toHex(), SazonDarkBackground.toHex())

        assertTrue(
            "El límite de la tarjeta en oscuro da ${ratio.texto()} contra darkBackground; " +
                "WCAG 1.4.11 pide >= 3,0",
            ratio >= 3.0,
        )
        assertEquals("El ADR 0006 documenta 3,26:1 para el borde", 3.26, ratio, 0.05)
    }

    @Test
    fun la_metrica_detecta_el_defecto_anterior_y_no_pasa_en_vacio() {
        val background = SazonDarkBackground.toHex()
        val anterior = contrastRatio(SazonDarkSurface.toHex(), background)
        val relleno = contrastRatio(SazonCardTokens.dark.surface.toHex(), background)

        assertEquals("darkSurface #312720 daba 1,15:1", 1.15, anterior, 0.05)
        assertTrue("darkSurface ya estaba por debajo de 3,0 (${anterior.texto()})", anterior < 3.0)
        assertEquals("El relleno nuevo #3A2E26 da 1,28:1", 1.28, relleno, 0.05)
        assertTrue(
            "El relleno tampoco llega a 3,0 (${relleno.texto()}): el límite de la tarjeta es el borde",
            relleno < 3.0,
        )
        assertTrue(
            "El límite debe ser el borde, no el relleno",
            contrastRatio(SazonCardTokens.dark.boundary.toHex(), background) > relleno,
        )
    }

    @Test
    fun el_tinte_calido_de_la_cocina_es_sutil_y_no_se_come_el_borde() {
        val surface = SazonCardTokens.dark.surface
        val boundary = SazonCardTokens.dark.boundary.toHex()
        val background = SazonDarkBackground.toHex()
        val acentos = listOf(
            "primaryContainer" to SazonDarkPrimaryContainer,
            "secondaryContainer" to SazonDarkSecondaryContainer,
            "tertiaryContainer" to SazonDarkAccentContainer,
            "surfaceVariant" to SazonDarkSurfaceVariant,
        )

        assertTrue(
            "TINT_ALPHA debe caer en la banda documentada 12-16 %: ${SazonCardTokens.TINT_ALPHA}",
            SazonCardTokens.TINT_ALPHA in 0.12f..0.16f,
        )

        acentos.forEach { (rol, acento) ->
            val tenido = SazonCardTokens.tintedSurface(surface, acento).toHex()
            val contraFondo = contrastRatio(tenido, background)
            val contraBorde = contrastRatio(tenido, boundary)

            assertTrue(
                "$rol: el relleno teñido ($tenido) sigue por debajo del umbral " +
                    "(contra fondo = ${contraFondo.texto()})",
                contraFondo < 2.0,
            )
            assertTrue(
                "$rol: el borde debe seguir siendo el límite ($tenido contra borde = " +
                    "${contraBorde.texto()})",
                contraBorde >= 2.0,
            )
            assertEquals(
                "$rol: el tinte debe ser determinista (dos llamadas iguales, mismo color)",
                SazonCardTokens.tintedSurface(surface, acento),
                SazonCardTokens.tintedSurface(surface, acento),
            )
        }
    }

    @Test
    fun el_tinte_reproduce_los_vectores_del_prototipo_c3() {
        val surface = SazonCardTokens.dark.surface

        assertHexCerca(
            "primaryContainer al 12 %",
            "#402E24",
            SazonCardTokens.tintedSurface(surface, SazonDarkPrimaryContainer, 0.12f).toHex(),
        )
        assertHexCerca(
            "secondaryContainer al 16 %",
            "#383025",
            SazonCardTokens.tintedSurface(surface, SazonDarkSecondaryContainer, 0.16f).toHex(),
        )
    }

    @Test
    fun el_modo_claro_no_gana_borde_ni_cambia_su_superficie() {
        val claro = SazonCardTokens.of(dark = false)

        assertEquals("La tarjeta clara es la surface del tema", "#FFFFFF", claro.surface.toHex())
        assertEquals(SazonSurface.toHex(), claro.surface.toHex())
        assertEquals("Sin borde en claro", Color.Transparent, claro.boundary)
        assertEquals("Sin grosor de borde en claro", 0.dp, claro.boundaryWidth)
        assertEquals(
            "En oscuro la tarjeta sí lleva límite",
            1.dp,
            SazonCardTokens.dark.boundaryWidth,
        )
        assertEquals(
            "of(dark = true) devuelve la variante oscura",
            SazonCardTokens.dark,
            SazonCardTokens.of(dark = true),
        )
        assertEquals(
            "of(dark = false) devuelve la variante clara",
            SazonCardTokens.light,
            SazonCardTokens.of(dark = false),
        )
    }
}

/** Color a `#RRGGBB` en mayúsculas, el mismo formato que usa el frontmatter de `DESIGN.md`. */
private fun Color.toHex(): String = String.format(Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)

/** Luminancia relativa sRGB de la fórmula WCAG 2.x. */
private fun relativeLuminance(hex: String): Double {
    val rgb = hex.removePrefix("#").toInt(16)

    fun canal(componente: Int): Double {
        val c = componente / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    return 0.2126 * canal((rgb shr 16) and 0xFF) +
        0.7152 * canal((rgb shr 8) and 0xFF) +
        0.0722 * canal(rgb and 0xFF)
}

/** Ratio de contraste WCAG entre dos colores `#RRGGBB`: `(L_claro + 0,05) / (L_oscuro + 0,05)`. */
private fun contrastRatio(a: String, b: String): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val claro = max(la, lb)
    val oscuro = min(la, lb)
    return (claro + 0.05) / (oscuro + 0.05)
}

private fun Double.texto(): String = String.format(Locale.ROOT, "%.2f", this)

/** Compara dos `#RRGGBB` con tolerancia de ±1 por componente (redondeo float de la interpolación). */
private fun assertHexCerca(que: String, esperado: String, real: String) {
    val e = esperado.removePrefix("#").toInt(16)
    val r = real.removePrefix("#").toInt(16)
    for (i in 0..2) {
        val shift = 16 - 8 * i
        val canalEsperado = (e shr shift) and 0xFF
        val canalReal = (r shr shift) and 0xFF
        assertTrue(
            "$que: esperado $esperado, real $real (canal $i: $canalEsperado vs $canalReal)",
            max(canalEsperado, canalReal) - min(canalEsperado, canalReal) <= 1,
        )
    }
}
