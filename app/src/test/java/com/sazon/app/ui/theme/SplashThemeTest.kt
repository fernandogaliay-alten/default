package com.sazon.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato verificable del **splash de marca** de Android 12+ y de la no-regresion del tema base.
 *
 * Test JVM puro sobre los temas reales del repositorio (`AppResourceFiles`) y el frontmatter de
 * `DESIGN.md`. Cubre los escenarios 4, 5 y 6 (el negativo obligatorio) de
 * `docs/specs/app-launcher-icon.md`. `values-night-v31/` es imprescindible porque en la precedencia de
 * qualifiers *night* va antes que *platform version*: sin esa variante, en un API 31+ en modo oscuro el
 * sistema elegiria `values-night/` y el splash perderia los atributos de marca.
 *
 * Ademas comprueba la **geometria** de `ic_splash_mark` dentro del circulo interior de 2/3 que el sistema
 * hace visible (la marca del primer frame quedo recortada en la primera implementacion): el contrato de
 * color no lo ve, porque un color de la paleta puede estar fuera de ese circulo.
 */
class SplashThemeTest {

    private val markdown = designMarkdown()
    private val designColors = frontmatterSectionMap(markdown, "colors")
    private val lightColors = AppResourceFiles.colorResourceMap("values/colors.xml")
    private val nightColors = AppResourceFiles.colorResourceMap("values-night/colors.xml")

    private val baseLight = AppResourceFiles.style("values/themes.xml", "Theme.Sazon")
    private val baseNight = AppResourceFiles.style("values-night/themes.xml", "Theme.Sazon")
    private val v31Light = AppResourceFiles.style("values-v31/themes.xml", "Theme.Sazon")
    private val v31Night = AppResourceFiles.style("values-night-v31/themes.xml", "Theme.Sazon")

    @Test
    fun el_tema_base_sigue_fijando_fondo_y_modo_oscuro_y_no_declara_splash() {
        val problems = fondoYModoOscuroProblems("values" to baseLight) +
            fondoYModoOscuroProblems("values-night" to baseNight) +
            sinSplashProblems("values" to baseLight) +
            sinSplashProblems("values-night" to baseNight)
        assertTrue("Derivas del tema base: $problems", problems.isEmpty())
    }

    @Test
    fun las_variantes_v31_repiten_el_tema_base_y_anaden_el_splash() {
        val problems = fondoYModoOscuroProblems("values-v31" to v31Light) +
            fondoYModoOscuroProblems("values-night-v31" to v31Night) +
            repetirParentProblems() +
            splashProblems(v31Light, v31Night)
        assertTrue("Derivas del splash: $problems", problems.isEmpty())
    }

    @Test
    fun el_splash_usa_el_fondo_calido_y_la_marca_de_la_paleta() {
        assertEquals(
            "values: el fondo del splash es el fondo calido de DESIGN.md",
            designColors["background"],
            lightColors["sazon_window_background"],
        )
        assertEquals(
            "values-night: el fondo del splash es el oscuro de DESIGN.md",
            designColors["darkBackground"],
            nightColors["sazon_window_background"],
        )

        val splashMark = "drawable/ic_splash_mark.xml"
        assertTrue("Falta $splashMark", AppResourceFiles.exists(splashMark))
        val vector = AppResourceFiles.parse(splashMark)
        assertEquals("$splashMark no es un <vector>", "vector", vector.documentElement.tagName)
        assertTrue("$splashMark no tiene ningun <path>", vector.elements("path").isNotEmpty())

        val problems = launcherColorProblems(
            designColors,
            lightColors,
            nightColors,
            mapOf(splashMark to AppResourceFiles.text(splashMark)),
        )
        assertTrue("Derivas de color del splash: $problems", problems.isEmpty())
    }

    @Test
    fun la_marca_del_splash_cabe_en_el_circulo_visible_de_la_plataforma() {
        val problemas = splashMarkGeometryProblems(AppResourceFiles.text(SPLASH_MARK_VECTOR))
        assertTrue(
            "La marca del splash sale del circulo de 2/3 que el sistema hace visible: $problemas",
            problemas.isEmpty(),
        )
    }

    @Test
    fun una_marca_de_splash_desbordada_es_detectada() {
        // Geometria de la primera implementacion (sin el <group> de escala): el vientre del cuenco llegaba
        // a radio 44 de los 108 y la mascara de plataforma lo recortaba. La comprobacion debe verlo.
        val desbordada = """
            |<vector xmlns:android="http://schemas.android.com/apk/res/android"
            |    android:viewportWidth="108"
            |    android:viewportHeight="108">
            |    <path android:pathData="M20,60h68" android:strokeWidth="8" />
            |    <path android:pathData="M22,62a32,32 0 0,0 64,0" android:strokeWidth="8" />
            |</vector>
        """.trimMargin()
        assertTrue(
            "Se esperaba detectar la geometria desbordada (radio 44 > 36) y no se detecto",
            splashMarkGeometryProblems(desbordada).isNotEmpty(),
        )

        // El semiancho de trazo tambien cuenta: un punto a radio 34 cabe con un trazo fino y no con uno
        // grueso. Sin esta cota la comprobacion podria ignorar el trazo y pasar de forma incompleta.
        val puntoAlLimite = """
            |<vector xmlns:android="http://schemas.android.com/apk/res/android"
            |    android:viewportWidth="108"
            |    android:viewportHeight="108">
            |    <path android:pathData="M54,20" android:strokeWidth="%s" />
            |</vector>
        """.trimMargin()
        assertTrue(
            "Se esperaba detectar el trazo grueso que sobresale del circulo visible y no se detecto",
            splashMarkGeometryProblems(puntoAlLimite.format("8")).isNotEmpty(),
        )
        assertTrue(
            "Un punto a radio 34 con trazo fino (35 <= 36) no deberia dar problemas",
            splashMarkGeometryProblems(puntoAlLimite.format("2")).isEmpty(),
        )
    }

    @Test
    fun una_variante_v31_mutada_es_detectada() {
        val sinIcono = v31Night.copy(items = v31Night.items - "android:windowSplashScreenAnimatedIcon")
        assertNotEquals(v31Night, sinIcono)
        assertTrue(
            "Se esperaba detectar la falta del icono de splash y no se detecto",
            splashProblems(v31Light, sinIcono).isNotEmpty(),
        )

        val fondoAjeno = v31Light.copy(
            items = v31Light.items + ("android:windowSplashScreenBackground" to "@color/sazon_launcher_accent"),
        )
        assertTrue(
            "Se esperaba detectar un fondo de splash ajeno al token y no se detecto",
            splashProblems(fondoAjeno, v31Night).isNotEmpty(),
        )

        val placa = v31Light.copy(
            items = v31Light.items + (
                "android:windowSplashScreenIconBackgroundColor" to "@color/sazon_launcher_background"
                ),
        )
        assertTrue(
            "Se esperaba detectar windowSplashScreenIconBackgroundColor y no se detecto",
            splashProblems(placa, v31Night).isNotEmpty(),
        )
    }

    /** Fondo y modo oscuro: los dos items que el tema base ya fijaba y que v31 debe repetir. */
    private fun fondoYModoOscuroProblems(tema: Pair<String, AppResourceFiles.AndroidStyle>): List<String> {
        val (nombre, style) = tema
        val problems = mutableListOf<String>()
        if (style.items["android:windowBackground"] != "@color/sazon_window_background") {
            problems += "$nombre: windowBackground=${style.items["android:windowBackground"]}"
        }
        if (style.items["android:forceDarkAllowed"] != "false") {
            problems += "$nombre: forceDarkAllowed=${style.items["android:forceDarkAllowed"]}"
        }
        return problems
    }

    /** Los temas base no llevan atributos de splash: el splash vive solo en las variantes v31. */
    private fun sinSplashProblems(tema: Pair<String, AppResourceFiles.AndroidStyle>): List<String> {
        val (nombre, style) = tema
        return style.items.keys.filter { it.startsWith("android:windowSplashScreen") }.map { item ->
            "$nombre: declara $item (los temas base no llevan splash)"
        }
    }

    /** Una carpeta cualificada no hereda items: el `parent` debe repetirse tal cual. */
    private fun repetirParentProblems(): List<String> {
        val problems = mutableListOf<String>()
        if (v31Light.parent != baseLight.parent) {
            problems += "values-v31: parent=${v31Light.parent} (base=${baseLight.parent})"
        }
        if (v31Night.parent != baseNight.parent) {
            problems += "values-night-v31: parent=${v31Night.parent} (base=${baseNight.parent})"
        }
        return problems
    }

    private fun splashProblems(
        light: AppResourceFiles.AndroidStyle,
        night: AppResourceFiles.AndroidStyle,
    ): List<String> {
        val problems = mutableListOf<String>()
        listOf("values-v31" to light, "values-night-v31" to night).forEach { (nombre, style) ->
            val fondo = style.items["android:windowSplashScreenBackground"]
            if (fondo != "@color/sazon_window_background") {
                problems += "$nombre: windowSplashScreenBackground=$fondo"
            }
            val icono = style.items["android:windowSplashScreenAnimatedIcon"]
            if (icono != "@drawable/ic_splash_mark") {
                problems += "$nombre: windowSplashScreenAnimatedIcon=$icono"
            }
            if (style.items.containsKey("android:windowSplashScreenIconBackgroundColor")) {
                problems += "$nombre: declara windowSplashScreenIconBackgroundColor (decision: sin placa)"
            }
        }
        return problems
    }
}
