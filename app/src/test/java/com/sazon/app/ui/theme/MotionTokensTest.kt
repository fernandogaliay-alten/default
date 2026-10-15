package com.sazon.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprueba que los tokens de movimiento ([SazonMotion]) coinciden **exactamente** con el bloque
 * `motion` del frontmatter de `DESIGN.md` (fuente de verdad visual). Test JVM puro: no necesita
 * emulador ni Android framework.
 *
 * Cubre el escenario 7 de `docs/specs/screen-transitions.md`. El caso negativo no se espera en rojo:
 * muta una copia del documento en memoria y exige que la comparacion detecte la deriva, de modo que el
 * test no puede pasar en vacio (patron de `DesignTokensTest`/`TaxonomyIntegrityTest`).
 */
class MotionTokensTest {

    private val markdown = designMarkdown()

    @Test
    fun los_tokens_de_movimiento_coinciden_con_design_md() {
        assertTrue(
            "DESIGN.md y SazonMotion derivan: ${motionMismatches(markdown)}",
            motionMismatches(markdown).isEmpty(),
        )
    }

    @Test
    fun una_copia_con_la_duracion_alterada_es_detectada() {
        val mutado = markdown.replace("duration: 240ms", "duration: 999ms")
        assertNotEquals(
            "El reemplazo no altero el documento: el formato del frontmatter cambio",
            markdown,
            mutado,
        )
        assertTrue(
            "Se esperaba detectar la deriva de `duration` y no se detecto ninguna",
            motionMismatches(mutado).isNotEmpty(),
        )
    }

    @Test
    fun los_tokens_de_movimiento_tienen_sentido() {
        assertTrue("La duracion debe ser mayor que cero", SazonMotion.DURATION_MILLIS > 0)
        assertTrue("El desplazamiento debe ser mayor que cero", SazonMotion.SLIDE_OFFSET_DP > 0)
        assertEquals("standard", SazonMotion.EASING)
        assertTrue(SazonMotion.ENTER_ON_TOP)
    }
}

/**
 * Derivas entre el bloque `motion` de `DESIGN.md` y [SazonMotion]. Devolver una lista permite usarla
 * como asercion positiva (documento real) y negativa (copia mutada).
 */
private fun motionMismatches(markdown: String): List<String> {
    val motion = frontmatterSectionMap(markdown, "motion")
    val problems = mutableListOf<String>()

    fun check(name: String, designValue: String?, actual: String) {
        if (designValue == null) {
            problems += "DESIGN.md no declara motion.$name"
        } else if (designValue != actual) {
            problems += "motion.$name: DESIGN.md=$designValue SazonMotion=$actual"
        }
    }

    check("duration", motion["duration"], "${SazonMotion.DURATION_MILLIS}ms")
    check("slide", motion["slide"], "${SazonMotion.SLIDE_OFFSET_DP}px")
    check("easing", motion["easing"], SazonMotion.EASING)
    check("enterOnTop", motion["enterOnTop"], SazonMotion.ENTER_ON_TOP.toString())
    return problems
}
