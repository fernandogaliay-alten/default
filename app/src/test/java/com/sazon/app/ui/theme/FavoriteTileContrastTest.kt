package com.sazon.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste de la **miniatura de la lista de Favoritos** (escenarios 4-5 de
 * `docs/specs/favorites-tile-contrast.md`, issue #130). Es un test JVM puro: no necesita emulador ni
 * Android framework (solo `Color` de Compose).
 *
 * El contrato (`DESIGN.md` → `components.recipe-tile-favorites`): la miniatura 96x96 se pinta sobre
 * `primary` (`#D9552B`) con el monograma en `onPrimary` (`#FFFFFF`), así que resalta del fondo de la
 * tarjeta —`SazonCardTokens.light.surface` `#FFFFFF` = 3,97:1 en claro y `SazonCardTokens.dark.surface`
 * `#3A2E26` = 3,31:1 en oscuro— por encima del 3:1 de WCAG 1.4.11. Los dos vectores negativos
 * (`surfaceVariant` `#F2E8DE` = 1,21:1 y `darkSurfaceVariant` `#3D3129` = 1,04:1) documentan el defecto
 * anterior para que la métrica **no** pueda pasar en vacío.
 */
class FavoriteTileContrastTest {

    @Test
    fun la_miniatura_cumple_el_3_a_1_de_wcag_1_4_11_contra_la_tarjeta() {
        val claro = contrastRatio(SazonPrimary.toHex(), SazonCardTokens.light.surface.toHex())
        val oscuro = contrastRatio(SazonPrimary.toHex(), SazonCardTokens.dark.surface.toHex())

        assertEquals("#FFFFFF (superficie de la tarjeta clara)", "#FFFFFF", SazonCardTokens.light.surface.toHex())
        assertEquals("#3A2E26 (superficie de la tarjeta oscura)", "#3A2E26", SazonCardTokens.dark.surface.toHex())
        assertTrue(
            "La miniatura primary #D9552B en claro da ${claro.texto()}; WCAG 1.4.11 pide >= 3,0",
            claro >= 3.0,
        )
        assertEquals("El contrato documenta 3,97:1 en claro", 3.97, claro, 0.05)
        assertTrue(
            "La miniatura primary #D9552B en oscuro da ${oscuro.texto()}; WCAG 1.4.11 pide >= 3,0",
            oscuro >= 3.0,
        )
        assertEquals("El contrato documenta 3,31:1 en oscuro", 3.31, oscuro, 0.05)
    }

    @Test
    fun el_monograma_se_lee_sobre_el_fondo_primary() {
        val ratio = contrastRatio(SazonOnPrimary.toHex(), SazonPrimary.toHex())

        assertEquals("El monograma se pinta en onPrimary #FFFFFF", "#FFFFFF", SazonOnPrimary.toHex())
        assertTrue(
            "El monograma onPrimary sobre primary da ${ratio.texto()}; es texto grande (30sp Manrope " +
                "Bold), umbral >= 3,0",
            ratio >= 3.0,
        )
        assertEquals("El mismo par documenta 3,97:1", 3.97, ratio, 0.05)
    }

    @Test
    fun el_fondo_anterior_no_llegaba_al_umbral() {
        val anteriorClaro = contrastRatio(SazonSurfaceVariant.toHex(), SazonCardTokens.light.surface.toHex())
        val anteriorOscuro =
            contrastRatio(SazonDarkSurfaceVariant.toHex(), SazonCardTokens.dark.surface.toHex())

        assertEquals("surfaceVariant #F2E8DE sobre la tarjeta clara daba 1,21:1", 1.21, anteriorClaro, 0.05)
        assertTrue(
            "El fondo anterior en claro (${anteriorClaro.texto()}) ya estaba por debajo de 3,0",
            anteriorClaro < 3.0,
        )
        assertEquals(
            "darkSurfaceVariant #3D3129 sobre la tarjeta oscura daba 1,04:1",
            1.04,
            anteriorOscuro,
            0.05,
        )
        assertTrue(
            "El fondo anterior en oscuro (${anteriorOscuro.texto()}) ya estaba por debajo de 3,0",
            anteriorOscuro < 3.0,
        )
        assertTrue(
            "primary debe mejorar el contraste anterior en claro",
            contrastRatio(SazonPrimary.toHex(), SazonCardTokens.light.surface.toHex()) > anteriorClaro,
        )
        assertTrue(
            "primary debe mejorar el contraste anterior en oscuro",
            contrastRatio(SazonPrimary.toHex(), SazonCardTokens.dark.surface.toHex()) > anteriorOscuro,
        )
    }

    @Test
    fun los_roles_del_tema_de_la_miniatura_son_primary_y_onPrimary() {
        assertEquals("primary #D9552B en el esquema claro", SazonPrimary, LightColorScheme.primary)
        assertEquals("onPrimary #FFFFFF en el esquema claro", SazonOnPrimary, LightColorScheme.onPrimary)
        assertEquals("primary #D9552B en el esquema oscuro", SazonPrimary, DarkColorScheme.primary)
        assertEquals("onPrimary #FFFFFF en el esquema oscuro", SazonOnPrimary, DarkColorScheme.onPrimary)
        assertEquals("primary es #D9552B", "#D9552B", SazonPrimary.toHex())
        assertEquals("onPrimary es #FFFFFF", "#FFFFFF", SazonOnPrimary.toHex())
        assertNotEquals(
            "La miniatura no puede volver a pintarse sobre surfaceVariant",
            SazonPrimary,
            SazonSurfaceVariant,
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
