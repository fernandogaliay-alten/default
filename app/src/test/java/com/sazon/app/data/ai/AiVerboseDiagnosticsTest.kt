package com.sazon.app.data.ai

import com.sazon.app.domain.validation.DiscardReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Formateo puro del modo de diagnostico verboso (`docs/specs/ai-verbose-diagnostics.md`, escenarios 4
 * y 5): troceado del `content` reconstruible y por debajo del limite de Logcat, lineas por descarte
 * con motivo/causa/titulo saneado y **sin** fuga de la clave ni de la cabecera `Authorization`.
 *
 * No usa Android ni Logcat: es JVM puro, igual que el objeto que prueba.
 */
class AiVerboseDiagnosticsTest {

    // Escenario 4: concatenar los trozos reconstruye el contenido original.
    @Test
    fun los_trozos_del_crudo_se_reconstruyen_concatenando() {
        val content = recipesJson(200)

        val lines = AiVerboseDiagnostics.rawLines(content)

        assertTrue("Un contenido de varios KB debe trocearse", lines.size > 1)
        val bodies = lines.map { it.substringAfter("] ") }
        assertEquals("Los trozos deben reconstruir el contenido", content, bodies.joinToString(""))
    }

    // Escenario 4: cada linea queda por debajo del limite de Logcat (~4000 char).
    @Test
    fun cada_trozo_respeta_el_limite_de_logcat() {
        val content = recipesJson(200)

        val lines = AiVerboseDiagnostics.rawLines(content)

        lines.forEach { line ->
            assertTrue("Cada trozo cabe en una linea de Logcat", line.length <= AiVerboseDiagnostics.MAX_CHUNK_CHARS + 16)
        }
        assertEquals("3000 caracteres por trozo", 3_000, AiVerboseDiagnostics.MAX_CHUNK_CHARS)
    }

    // Escenario 4: los indices `[i/n]` numeran los trozos de forma estable.
    @Test
    fun los_indices_numeran_los_trozos() {
        val lines = AiVerboseDiagnostics.rawLines("x".repeat(6_500))

        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("raw[1/3] "))
        assertTrue(lines[1].startsWith("raw[2/3] "))
        assertTrue(lines[2].startsWith("raw[3/3] "))
        assertEquals(3_000, lines[0].substringAfter("] ").length)
        assertEquals(500, lines[2].substringAfter("] ").length)
    }

    // Escenario 4: el troceado no parte un par suplente (nunca deja un surrogate huerfano).
    @Test
    fun el_troceado_no_parte_pares_suplentes() {
        val content = "abc\uD83E\uDD16defg" // "abc" + emoji fuera del BMP + "defg"

        val lines = AiVerboseDiagnostics.rawLines(content, maxChars = 4)

        assertEquals(content, lines.map { it.substringAfter("] ") }.joinToString(""))
        lines.forEach { line ->
            val body = line.substringAfter("] ")
            assertFalse("No puede empezar por el low surrogate suelto", Character.isLowSurrogate(body.first()))
            assertFalse("No puede acabar en el high surrogate suelto", Character.isHighSurrogate(body.last()))
        }
    }

    @Test
    fun un_contenido_nulo_o_vacio_no_emite_lineas() {
        assertTrue(AiVerboseDiagnostics.rawLines(null).isEmpty())
        assertTrue(AiVerboseDiagnostics.rawLines("").isEmpty())
    }

    // Escenario 4: una linea por descarte con motivo, causa (solo si es INCOHERENT) y titulo saneado.
    @Test
    fun cada_descarte_tiene_su_linea_con_motivo_causa_y_titulo() {
        val discards = listOf(
            AiDiscardDetail(DiscardReason.INCOHERENT, AiIncoherentCause.TIME_DRIFT, "Tortilla de patatas"),
            AiDiscardDetail(DiscardReason.INCOHERENT, AiIncoherentCause.ALLERGEN_COVERAGE, "Leche caliente"),
            AiDiscardDetail(DiscardReason.SCHEMA, null, "Rota\ncon salto"),
            AiDiscardDetail(DiscardReason.HARD_RESTRICTION, null, null),
        )

        val lines = AiVerboseDiagnostics.discardLines(discards)

        assertEquals(
            listOf(
                """discard[1/4] INCOHERENT/TIME_DRIFT title="Tortilla de patatas"""",
                """discard[2/4] INCOHERENT/ALLERGEN_COVERAGE title="Leche caliente"""",
                """discard[3/4] SCHEMA title="Rota con salto"""",
                "discard[4/4] HARD_RESTRICTION title=(sin titulo)",
            ),
            lines,
        )
    }

    // Escenario 5: ninguna linea del helper (ni el crudo ni los titulos) lleva la clave ni la cabecera.
    @Test
    fun ninguna_linea_expone_la_clave_ni_authorization() {
        val content = recipesJson(10)
        val discards = listOf(AiDiscardDetail(DiscardReason.SCHEMA, null, "Tortilla de patatas"))

        val lines = AiVerboseDiagnostics.rawLines(content) + AiVerboseDiagnostics.discardLines(discards)

        assertTrue(lines.isNotEmpty())
        lines.forEach { line ->
            assertFalse("Nunca la clave", line.contains("test-key"))
            assertFalse("Nunca Bearer", line.contains("Bearer"))
            assertFalse("Nunca Authorization", line.contains("Authorization"))
        }
    }

    private fun recipesJson(entries: Int): String =
        (0 until entries).joinToString(separator = ",", prefix = """{"recipes":[""", postfix = "]}") { index ->
            """{"title":"Receta $index","description":"Descripcion larga de la receta $index para superar el limite."}"""
        }
}
