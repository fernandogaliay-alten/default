package com.sazon.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Microcopy puro del estado de carga: cabecera, tres sublineas y linea de prompt con la consulta
 * recortada.
 */
class AiSearchProgressTest {

    @Test
    fun la_cabecera_es_la_del_generador() {
        assertEquals("Buscando recetas para ti…", AiSearchProgress.HEADLINE)
    }

    @Test
    fun hay_tres_sublineas_estables() {
        val subLines = AiSearchProgress.subLines("pollo")

        assertEquals(3, subLines.size)
        assertEquals(
            listOf(
                "Interpretando antojos y texto",
                "Filtrando por alérgenos y dieta",
                "Calculando tiempos de cocción",
            ),
            subLines,
        )
    }

    @Test
    fun la_linea_de_prompt_normaliza_la_consulta() {
        assertEquals("Prompt: “pollo y arroz”", AiSearchProgress.promptLine("   pollo y arroz   "))
        assertTrue(AiSearchProgress.promptLine("pollo").startsWith("Prompt:"))
    }
}
