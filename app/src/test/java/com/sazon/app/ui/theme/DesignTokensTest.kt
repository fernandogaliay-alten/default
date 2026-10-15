package com.sazon.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprueba que los tokens expuestos por el tema coinciden exactamente con el frontmatter YAML de
 * `DESIGN.md` (fuente única de verdad). Es un test JVM: no necesita emulador ni Android framework.
 *
 * Cubre los escenarios 1, 2 y 4 de `docs/specs/design-tokens-and-theme.md`. El caso negativo no es un
 * test que se espera en rojo: muta una copia del documento en memoria y exige que `mismatches(...)`
 * detecte la deriva, de modo que el test no puede pasar en vacío (patrón de `TaxonomyIntegrityTest`).
 */
class DesignTokensTest {

    private val markdown = designMarkdown()

    @Test
    fun la_variante_clara_expone_los_tokens_de_design_md() {
        assertTrue("DESIGN.md no declara derivas: ${mismatches(markdown)}", mismatches(markdown).isEmpty())
    }

    @Test
    fun la_variante_oscura_usa_los_anclajes_de_design_md() {
        val colors = frontmatterSectionMap(markdown, "colors")
        val scheme = DarkColorScheme

        assertEquals("darkBackground", colors["darkBackground"], scheme.background.toHex())
        assertEquals("darkSurface", colors["darkSurface"], scheme.surface.toHex())
        assertEquals("primary", colors["primary"], scheme.primary.toHex())
        assertEquals("secondary", colors["secondary"], scheme.secondary.toHex())
        assertEquals("accent", colors["accent"], scheme.tertiary.toHex())
        assertEquals("texto crema", "#FFF8F0", scheme.onBackground.toHex())
        assertEquals("texto crema", "#FFF8F0", scheme.onSurface.toHex())
    }

    @Test
    fun los_tokens_de_tarjeta_del_modo_oscuro_coinciden_con_design_md() {
        val colors = frontmatterSectionMap(markdown, "colors")

        assertEquals("darkCardSurface", colors["darkCardSurface"], SazonDarkCardSurface.toHex())
        assertEquals("darkCardOutline", colors["darkCardOutline"], SazonDarkCardOutline.toHex())
        assertEquals("darkCardSurface", colors["darkCardSurface"], SazonCardTokens.dark.surface.toHex())
        assertEquals("darkCardOutline", colors["darkCardOutline"], SazonCardTokens.dark.boundary.toHex())
    }

    @Test
    fun dark_surface_no_cambia_y_los_tokens_de_tarjeta_son_otros() {
        val colors = frontmatterSectionMap(markdown, "colors")

        assertEquals("darkSurface en el frontmatter", "#312720", colors["darkSurface"])
        assertEquals("darkSurface en el esquema oscuro", colors["darkSurface"], DarkColorScheme.surface.toHex())
        assertEquals("SazonDarkSurface", "#312720", SazonDarkSurface.toHex())
        assertNotEquals(
            "La superficie de la tarjeta no puede ser `darkSurface`",
            SazonDarkSurface.toHex(),
            SazonCardTokens.dark.surface.toHex(),
        )
        assertNotEquals(
            "El borde de la tarjeta no puede ser `darkSurface`",
            SazonDarkSurface.toHex(),
            SazonCardTokens.dark.boundary.toHex(),
        )
        assertEquals(
            "En claro la tarjeta sigue siendo la `surface` del tema",
            LightColorScheme.surface.toHex(),
            SazonCardTokens.light.surface.toHex(),
        )
    }

    @Test
    fun la_escala_tipografica_coincide_con_design_md() {
        val typography = frontmatterTypographyMap(markdown)
        assertTypography("display", SazonTypography.displayLarge, typography)
        assertTypography("h1", SazonTypography.headlineLarge, typography)
        assertTypography("body", SazonTypography.bodyLarge, typography)
        assertTypography("caption", SazonTypography.labelSmall, typography)
    }

    @Test
    fun el_espaciado_y_las_formas_coinciden_con_design_md() {
        val spacing = frontmatterSectionMap(markdown, "spacing")
        assertDp("spacing.sm", spacing["sm"], SazonSpacing.sm.value)
        assertDp("spacing.md", spacing["md"], SazonSpacing.md.value)
        assertDp("spacing.lg", spacing["lg"], SazonSpacing.lg.value)

        val rounded = frontmatterSectionMap(markdown, "rounded")
        assertShape("rounded.sm", rounded["sm"], SazonShapes.extraSmall)
        assertShape("rounded.sm", rounded["sm"], SazonShapes.small)
        assertShape("rounded.md", rounded["md"], SazonShapes.medium)
        assertShape("rounded.md", rounded["md"], SazonShapes.large)
    }

    @Test
    fun una_copia_de_design_md_con_primary_alterado_es_detectada() {
        val mutado = markdown.replace("primary: \"#D9552B\"", "primary: \"#123456\"")
        assertNotEquals("El reemplazo no altero el documento: el formato del frontmatter cambio", markdown, mutado)
        assertTrue(
            "Se esperaba detectar la deriva de `primary` y no se detecto ninguna",
            mismatches(mutado).isNotEmpty(),
        )
    }

    @Test
    fun ningun_rol_usa_la_paleta_de_plantilla() {
        val plantilla = setOf(
            "#D0BCFF", "#CCC2DC", "#EFB8C8", // Purple80/PurpleGrey80/Pink80
            "#6650A4", "#625B71", "#7D5260", // Purple40/PurpleGrey40/Pink40
            "#BB86FC", "#6200EE", "#3700B3", "#03DAC5", "#018786", // colores de plantilla de res/values
        )
        rolesDeAmbosEsquemas().forEach { (nombre, color) ->
            assertTrue(
                "El rol $nombre usa un color de la paleta de plantilla (${color.toHex()})",
                color.toHex() !in plantilla,
            )
        }
    }
}

private fun Color.toHex(): String = String.format(Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)

private fun rolesDeAmbosEsquemas(): List<Pair<String, Color>> = listOf(
    "light.primary" to LightColorScheme.primary,
    "light.onPrimary" to LightColorScheme.onPrimary,
    "light.primaryContainer" to LightColorScheme.primaryContainer,
    "light.onPrimaryContainer" to LightColorScheme.onPrimaryContainer,
    "light.secondary" to LightColorScheme.secondary,
    "light.secondaryContainer" to LightColorScheme.secondaryContainer,
    "light.tertiary" to LightColorScheme.tertiary,
    "light.tertiaryContainer" to LightColorScheme.tertiaryContainer,
    "light.background" to LightColorScheme.background,
    "light.surface" to LightColorScheme.surface,
    "light.onSurface" to LightColorScheme.onSurface,
    "light.outline" to LightColorScheme.outline,
    "light.error" to LightColorScheme.error,
    "light.surfaceTint" to LightColorScheme.surfaceTint,
    "light.inverseSurface" to LightColorScheme.inverseSurface,
    "light.surfaceVariant" to LightColorScheme.surfaceVariant,
    "dark.primary" to DarkColorScheme.primary,
    "dark.onPrimary" to DarkColorScheme.onPrimary,
    "dark.primaryContainer" to DarkColorScheme.primaryContainer,
    "dark.onPrimaryContainer" to DarkColorScheme.onPrimaryContainer,
    "dark.secondary" to DarkColorScheme.secondary,
    "dark.secondaryContainer" to DarkColorScheme.secondaryContainer,
    "dark.tertiary" to DarkColorScheme.tertiary,
    "dark.tertiaryContainer" to DarkColorScheme.tertiaryContainer,
    "dark.background" to DarkColorScheme.background,
    "dark.surface" to DarkColorScheme.surface,
    "dark.onSurface" to DarkColorScheme.onSurface,
    "dark.outline" to DarkColorScheme.outline,
    "dark.error" to DarkColorScheme.error,
    "dark.surfaceTint" to DarkColorScheme.surfaceTint,
    "dark.inverseSurface" to DarkColorScheme.inverseSurface,
    "dark.surfaceVariant" to DarkColorScheme.surfaceVariant,
)

/**
 * Derivas entre un frontmatter de `DESIGN.md` y los tokens reales de la variante clara y la tipografía.
 * Devolver una lista permite usarla como aserción positiva (documento real) y negativa (copia mutada).
 */
private fun mismatches(markdown: String): List<String> {
    val colors = frontmatterSectionMap(markdown, "colors")
    val problems = mutableListOf<String>()

    fun check(name: String, role: String, actual: Color) {
        val expected = colors[name]
        if (expected == null) {
            problems += "DESIGN.md no declara colors.$name"
        } else if (expected != actual.toHex()) {
            problems += "$role: DESIGN.md=$expected tema=${actual.toHex()}"
        }
    }

    check("primary", "primary", LightColorScheme.primary)
    check("onPrimary", "onPrimary", LightColorScheme.onPrimary)
    check("primaryContainer", "primaryContainer", LightColorScheme.primaryContainer)
    check("secondary", "secondary", LightColorScheme.secondary)
    check("accent", "tertiary", LightColorScheme.tertiary)
    check("background", "background", LightColorScheme.background)
    check("surface", "surface", LightColorScheme.surface)
    check("text", "onBackground", LightColorScheme.onBackground)
    check("text", "onSurface", LightColorScheme.onSurface)
    check("outline", "outline", LightColorScheme.outline)
    check("error", "error", LightColorScheme.error)

    val display = frontmatterTypographyMap(markdown)["display"]
    if (display == null) {
        problems += "DESIGN.md no declara typography.display"
    } else {
        val size = display["fontSize"]?.removeSuffix("sp")?.toFloatOrNull()
        val weight = display["fontWeight"]?.toIntOrNull()
        if (size != SazonTypography.displayLarge.fontSize.value) {
            problems += "display.fontSize: DESIGN.md=$size tema=${SazonTypography.displayLarge.fontSize.value}"
        }
        val actualWeight = SazonTypography.displayLarge.fontWeight?.weight
        if (weight != actualWeight) {
            problems += "display.fontWeight: DESIGN.md=$weight tema=$actualWeight"
        }
    }
    return problems
}

private fun assertTypography(
    designName: String,
    style: TextStyle,
    typography: Map<String, Map<String, String>>,
) {
    val expected = checkNotNull(typography[designName]) { "DESIGN.md no define typography.$designName" }
    val fontSize = checkNotNull(expected["fontSize"]) { "typography.$designName sin fontSize" }
        .removeSuffix("sp").toFloat()
    val fontWeight = checkNotNull(expected["fontWeight"]) { "typography.$designName sin fontWeight" }.toInt()
    assertEquals("fontSize de $designName", fontSize, style.fontSize.value, 0.01f)
    assertEquals("fontWeight de $designName", fontWeight, style.fontWeight?.weight)
}

private fun assertDp(token: String, designValue: String?, actual: Float) {
    val expected = checkNotNull(designValue) { "DESIGN.md no declara $token" }
        .removeSuffix("px").toFloat()
    assertEquals("$token", expected, actual, 0.01f)
}

private fun assertShape(token: String, designValue: String?, actual: Shape) {
    val expected = checkNotNull(designValue) { "DESIGN.md no declara $token" }
        .removeSuffix("px").toFloat()
    assertEquals("$token", RoundedCornerShape(expected.dp), actual)
}

/** Extrae `typography` (dos niveles: estilo -> propiedades -> valor) del frontmatter. */
private fun frontmatterTypographyMap(markdown: String): Map<String, Map<String, String>> {
    val lines = frontmatter(markdown)
    val start = lines.indexOfFirst { it.trimEnd() == "typography:" }
    check(start >= 0) { "DESIGN.md no tiene la seccion 'typography' en el frontmatter" }
    val result = linkedMapOf<String, MutableMap<String, String>>()
    var current: String? = null
    for (i in start + 1 until lines.size) {
        val line = lines[i]
        if (line.isBlank()) continue
        if (!line.startsWith("  ")) break
        val trimmed = line.trim()
        if (line.startsWith("    ")) {
            val separator = trimmed.indexOf(':')
            if (separator <= 0) continue
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim().trim('"')
            current?.let { result.getOrPut(it) { linkedMapOf() }[key] = value }
        } else {
            current = trimmed.removeSuffix(":").trim()
            result.getOrPut(current!!) { linkedMapOf() }
        }
    }
    return result
}
