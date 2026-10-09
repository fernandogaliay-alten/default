package com.sazon.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 3-5 de `docs/specs/manual-suggestions-refresh.md`: la antiguedad relativa del Lote es
 * logica pura, sin emulador ni Android. Cubre la tabla de dias, los limites exactos, el respaldo sin
 * Lote y casos negativos que no pasan en vacio.
 */
class SuggestionsRefreshFormatTest {

    private val now = 1_700_000_000_000L
    private val day = SuggestionsRefreshFormat.DAY_MILLIS

    // --- Escenario 4: tabla de antiguedad relativa ---

    @Test
    fun menos_de_un_dia_muestra_hoy() {
        assertEquals("Última actualización: hoy", SuggestionsRefreshFormat.lastUpdatedLabel(now, now))
        assertEquals(
            "Última actualización: hoy",
            SuggestionsRefreshFormat.lastUpdatedLabel(now - day / 2, now),
        )
    }

    @Test
    fun justo_un_dia_muestra_hace_1_dia() {
        assertEquals(
            "Última actualización: hace 1 día",
            SuggestionsRefreshFormat.lastUpdatedLabel(now - day, now),
        )
    }

    @Test
    fun tres_dias_muestra_hace_3_dias() {
        assertEquals(
            "Última actualización: hace 3 días",
            SuggestionsRefreshFormat.lastUpdatedLabel(now - 3 * day, now),
        )
    }

    @Test
    fun siete_dias_muestra_hace_7_dias() {
        assertEquals(
            "Última actualización: hace 7 días",
            SuggestionsRefreshFormat.lastUpdatedLabel(now - 7 * day, now),
        )
    }

    @Test
    fun justo_un_dia_menos_un_ms_muestra_hoy() {
        assertEquals(
            "Última actualización: hoy",
            SuggestionsRefreshFormat.lastUpdatedLabel(now - day + 1, now),
        )
    }

    @Test
    fun dos_dias_muestra_hace_2_dias() {
        assertEquals(
            "Última actualización: hace 2 días",
            SuggestionsRefreshFormat.lastUpdatedLabel(now - 2 * day, now),
        )
    }

    // --- Escenario 4 (caso negativo): un reloj retrasado no produce dias negativos ---

    @Test
    fun reloj_retrasado_se_degrada_a_hoy_sin_negativos() {
        val label = SuggestionsRefreshFormat.lastUpdatedLabel(now + 5 * day, now)

        assertEquals("Última actualización: hoy", label)
        assertTrue("La etiqueta nunca debe contener un signo negativo", !label.contains("-"))
        assertTrue("La etiqueta nunca debe citar dias negativos", !label.contains("hace -"))
    }

    // --- Escenario 3: la etiqueta refleja el generatedAt mas reciente ---

    @Test
    fun un_generated_at_mas_reciente_produce_una_etiqueta_mas_corta() {
        val antiguo = SuggestionsRefreshFormat.lastUpdatedLabel(now - 3 * day, now)
        val reciente = SuggestionsRefreshFormat.lastUpdatedLabel(now, now)

        assertEquals("Última actualización: hace 3 días", antiguo)
        assertEquals("Última actualización: hoy", reciente)
        assertNotEquals(antiguo, reciente)
    }

    // --- Escenario 5: sin Lote se muestra un respaldo ---

    @Test
    fun sin_lote_muestra_el_respaldo() {
        assertEquals(
            "Aún no hay sugerencias.",
            SuggestionsRefreshFormat.lastUpdatedLabel(null, now),
        )
    }

    @Test
    fun el_respaldo_no_confunde_el_caso_sin_lote_con_uno_fresco() {
        val sinLote = SuggestionsRefreshFormat.lastUpdatedLabel(null, now)
        val fresco = SuggestionsRefreshFormat.lastUpdatedLabel(now, now)

        assertNotEquals(sinLote, fresco)
        assertEquals(SuggestionsRefreshFormat.NO_BATCH_LABEL, sinLote)
    }

    // --- Microcopy fijado en un solo sitio ---

    @Test
    fun el_microcopy_de_la_seccion_es_estable() {
        assertEquals("Actualizar sugerencias", SuggestionsRefreshFormat.ACTION_LABEL)
        assertEquals("Actualizando…", SuggestionsRefreshFormat.REFRESHING_LABEL)
        assertEquals(
            "Se renuevan cuando cambian tus preferencias.",
            SuggestionsRefreshFormat.HELPER_TEXT,
        )
        assertEquals(86_400_000L, SuggestionsRefreshFormat.DAY_MILLIS)
    }
}
