package com.sazon.app.data.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 3 y 4 de `docs/specs/dish-image-settings-toggle.md`: el contador atomico del **tope por
 * sesion** con un tope **inyectado** (casos negativos que no pasan en vacio). Todo en JVM puro, sin
 * Android, sin red y sin emulador.
 */
class DishImageGenerationBudgetTest {

    @Test
    fun el_tope_por_defecto_viene_de_la_politica_en_un_unico_sitio() {
        val budget = DishImageGenerationBudget()

        assertEquals(12, budget.max)
        assertEquals(DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION, budget.max)
    }

    @Test
    fun con_el_tope_intacto_used_es_cero_y_remaining_es_el_tope() {
        val budget = DishImageGenerationBudget(maxGenerations = 5)

        assertEquals(0, budget.used)
        assertEquals(5, budget.remaining)
        assertFalse(budget.exhausted)
    }

    @Test
    fun las_primeras_n_reservas_pasan_y_la_siguiente_no() {
        val budget = DishImageGenerationBudget(maxGenerations = 2)

        assertTrue("La primera reserva pasa", budget.tryAcquire())
        assertTrue("La segunda reserva pasa", budget.tryAcquire())
        assertFalse("La tercera (N+1) no pasa", budget.tryAcquire())

        assertEquals(2, budget.used)
        assertEquals(0, budget.remaining)
        assertTrue(budget.exhausted)
    }

    @Test
    fun con_tope_cero_ninguna_reserva_pasa() {
        val budget = DishImageGenerationBudget(maxGenerations = 0)

        assertFalse(budget.tryAcquire())
        assertFalse(budget.tryAcquire())
        assertEquals(0, budget.used)
        assertEquals(0, budget.remaining)
        assertTrue(budget.exhausted)
    }

    @Test
    fun remaining_nunca_es_negativo_aunque_se_insista_tras_agotar() {
        val budget = DishImageGenerationBudget(maxGenerations = 1)
        budget.tryAcquire()

        assertFalse(budget.tryAcquire())
        assertFalse(budget.tryAcquire())
        assertEquals(1, budget.used)
        assertEquals(0, budget.remaining)
    }

    @Test
    fun reservas_concurrentes_nunca_rebasan_el_tope() = runBlocking {
        val budget = DishImageGenerationBudget(maxGenerations = 3)

        val results = (1..12)
            .map { async(Dispatchers.Default) { budget.tryAcquire() } }
            .awaitAll()

        assertEquals("Exactamente k exitos con k < N", 3, results.count { it })
        assertEquals(3, budget.used)
        assertEquals(0, budget.remaining)
        assertTrue(budget.exhausted)
    }

    @Test
    fun cada_reserva_consume_una_unidad_distinta() {
        val budget = DishImageGenerationBudget(maxGenerations = 12)

        budget.tryAcquire()
        val first = budget.used

        budget.tryAcquire()

        assertEquals(1, first)
        assertEquals(2, budget.used)
        assertNotEquals(budget.used, budget.remaining)
    }
}
