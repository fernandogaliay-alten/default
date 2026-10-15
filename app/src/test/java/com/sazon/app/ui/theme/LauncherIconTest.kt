package com.sazon.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato verificable del **icono de lanzador** de Sazon: icono adaptativo de tres capas apuntando a
 * vectores propios, paleta tomada de `DESIGN.md` y sin rastro del icono de plantilla de Android Studio.
 *
 * Test JVM puro: lee el manifiesto (`android.manifest.path`) y el arbol `res/` (`android.res.dir`) con
 * `AppResourceFiles` y el frontmatter de `DESIGN.md` con `designMarkdown()`/`frontmatterSectionMap()`.
 * Cubre los escenarios 1, 2, 3 y 6 (el negativo obligatorio) de `docs/specs/app-launcher-icon.md`; el
 * acabado visual en el emulador lo revisa la persona (AGENTS.md no automatiza UI de emulador).
 */
class LauncherIconTest {

    private val markdown = designMarkdown()
    private val designColors = frontmatterSectionMap(markdown, "colors")
    private val lightColors = AppResourceFiles.colorResourceMap("values/colors.xml")
    private val nightColors = AppResourceFiles.colorResourceMap("values-night/colors.xml")

    private fun vectors(): Map<String, String> = ICON_VECTORS.associateWith { AppResourceFiles.text(it) }

    @Test
    fun el_icono_adaptativo_declara_las_tres_capas_y_apunta_a_vectores_propios() {
        val capas = linkedMapOf(
            "background" to "@drawable/ic_launcher_background",
            "foreground" to "@drawable/ic_launcher_foreground",
            "monochrome" to "@drawable/ic_launcher_monochrome",
        )
        listOf("mipmap-anydpi/ic_launcher.xml", "mipmap-anydpi/ic_launcher_round.xml").forEach { path ->
            assertTrue("Falta $path", AppResourceFiles.exists(path))
            val root = AppResourceFiles.parse(path).documentElement
            assertEquals("$path no es un <adaptive-icon>", "adaptive-icon", root.tagName)

            capas.forEach { (capa, esperado) ->
                val element = root.children(capa).singleOrNull()
                assertNotNull("$path no declara una unica capa $capa", element)
                assertEquals("capa $capa de $path", esperado, element!!.androidAttr("drawable"))
            }

            capas.values.forEach { referencia ->
                val vectorPath = "drawable/${referencia.removePrefix("@drawable/")}.xml"
                assertTrue("$path apunta a $referencia, que no existe", AppResourceFiles.exists(vectorPath))
                val vector = AppResourceFiles.parse(vectorPath)
                assertEquals("$vectorPath no es un <vector>", "vector", vector.documentElement.tagName)
                assertTrue("$vectorPath no tiene ningun <path>", vector.elements("path").isNotEmpty())
            }
        }
    }

    @Test
    fun el_manifiesto_sigue_declarando_el_icono_y_el_icono_redondo() {
        val application = AppResourceFiles.parse(AppResourceFiles.manifestFile).elements("application").single()
        assertEquals("@mipmap/ic_launcher", application.androidAttr("icon"))
        assertEquals("@mipmap/ic_launcher_round", application.androidAttr("roundIcon"))
    }

    @Test
    fun la_paleta_del_icono_sale_de_design_md() {
        val problems = launcherColorProblems(designColors, lightColors, nightColors, vectors())
        assertTrue("Derivas de color del icono: $problems", problems.isEmpty())
    }

    @Test
    fun el_foreground_usa_los_dos_roles_del_motivo() {
        val problems = launcherRoleProblems(vectors())
        assertTrue("Derivas de rol del foreground: $problems", problems.isEmpty())
    }

    @Test
    fun no_queda_rastro_del_icono_de_plantilla() {
        val binarios = listOf("webp", "png", "jpg", "jpeg").flatMap { AppResourceFiles.files(it) }
        assertTrue(
            "Quedan assets binarios de icono: ${binarios.map { AppResourceFiles.relativePath(it) }}",
            binarios.isEmpty(),
        )

        // Marcadores del icono de plantilla de Android Studio: verde, rejilla, degradado y robot.
        val marcadores = listOf(
            "#3DDC84" to true,
            "33FFFFFF" to true,
            "aapt:attr" to false,
            "M65.3,45.828" to false,
            "strokeWidth=\"0.8\"" to false,
        )
        (AppResourceFiles.xmlFiles() + AppResourceFiles.manifestFile).forEach { file ->
            val content = file.readText()
            marcadores.forEach { (marcador, ignoreCase) ->
                assertFalse(
                    "${file.name} conserva '$marcador' (icono de plantilla)",
                    content.contains(marcador, ignoreCase = ignoreCase),
                )
            }
        }
    }

    @Test
    fun una_paleta_mutada_es_detectada() {
        val mutado = markdown.replace("primary: \"#D9552B\"", "primary: \"#123456\"")
        assertNotEquals("El reemplazo no altero el documento: cambio el formato del frontmatter", markdown, mutado)
        val problemasMarkdown = launcherColorProblems(
            frontmatterSectionMap(mutado, "colors"),
            lightColors,
            nightColors,
            vectors(),
        )
        assertTrue(
            "Se esperaba detectar la deriva de primary y no se detecto ninguna",
            problemasMarkdown.isNotEmpty(),
        )

        val nocheMutada = nightColors + ("sazon_launcher_background" to "#000000")
        assertNotEquals(nightColors, nocheMutada)
        val problemasNoche = launcherColorProblems(designColors, lightColors, nocheMutada, vectors())
        assertTrue(
            "Se esperaba detectar la deriva de values-night y no se detecto ninguna",
            problemasNoche.isNotEmpty(),
        )
    }
}
