package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estado del modo cocina (`docs/specs/cooking-mode.md`, escenarios 2, 5 y 7): arranque en el paso 1,
 * navegacion "Siguiente"/"Anterior" con reinicio del temporizador, marcado por `RecipeStep.order`
 * independiente, receta sin pasos sin crash e inmutabilidad. Logica pura sin Compose ni Android.
 */
class CookingModeStateTest {

    @Test
    fun arranca_en_el_paso_uno_con_el_temporizador_de_ese_paso() {
        val state = CookingModeState.initial(recetaConCincoPasos())

        assertEquals(0, state.stepIndex)
        assertEquals(5, state.totalSteps)
        assertTrue(state.completedOrders.isEmpty())
        assertEquals(CookingTimerStatus.IDLE, state.timer.status)
        assertEquals(600_000L, state.timer.totalMillis)
        assertTrue(state.isFirstStep)
        assertFalse(state.isLastStep)
    }

    @Test
    fun siguiente_avanza_y_reinicia_el_temporizador_al_paso_nuevo() {
        val steps = recetaConCincoPasos().steps
        val enMarcha = CookingModeState.initial(recetaConCincoPasos())
            .withTimer(CookingTimerState.of(10).start().tick(120_000L))

        val siguiente = enMarcha.next(steps)

        assertEquals(1, siguiente.stepIndex)
        assertFalse(siguiente.isFirstStep)
        // El temporizador se reinicia a Inactivo con la duracion del paso 2 (8 min).
        assertEquals(CookingTimerStatus.IDLE, siguiente.timer.status)
        assertEquals(480_000L, siguiente.timer.totalMillis)
        assertEquals(480_000L, siguiente.timer.remainingMillis)
    }

    @Test
    fun anterior_retrocede_y_reinicia_el_temporizador_al_paso_nuevo() {
        val steps = recetaConCincoPasos().steps
        val enPaso3 = CookingModeState.initial(recetaConCincoPasos()).next(steps).next(steps)

        assertEquals(2, enPaso3.stepIndex)

        val anterior = enPaso3.previous(steps)

        assertEquals(1, anterior.stepIndex)
        assertEquals(CookingTimerStatus.IDLE, anterior.timer.status)
        assertEquals(480_000L, anterior.timer.totalMillis)
    }

    @Test
    fun en_los_extremos_la_navegacion_es_no_op() {
        val steps = recetaConCincoPasos().steps
        val primero = CookingModeState.initial(recetaConCincoPasos())
        val ultimo = primero.next(steps).next(steps).next(steps).next(steps)

        assertTrue(ultimo.isLastStep)
        assertEquals(4, ultimo.stepIndex)
        // "Anterior" en el primero y "Siguiente" en el ultimo no salen de rango.
        assertEquals(primero, primero.previous(steps))
        assertEquals(ultimo, ultimo.next(steps))
    }

    @Test
    fun el_ultimo_paso_se_reconoce_con_total_steps() {
        val steps = recetaConCincoPasos().steps
        var state = CookingModeState.initial(recetaConCincoPasos())

        assertFalse(state.isLastStep)
        repeat(4) { state = state.next(steps) }

        assertTrue(state.isLastStep)
        assertEquals(4, state.stepIndex)
    }

    @Test
    fun marcar_y_desmarcar_un_paso_no_toca_el_indice_ni_el_temporizador() {
        val state = CookingModeState.initial(recetaConCincoPasos())
            .withTimer(CookingTimerState.of(10).start().tick(60_000L))

        val marcado = state.toggleCompleted(2)

        assertTrue(2 in marcado.completedOrders)
        assertEquals(state.stepIndex, marcado.stepIndex)
        assertEquals(state.timer, marcado.timer)

        val desmarcado = marcado.toggleCompleted(2)

        assertFalse(2 in desmarcado.completedOrders)
    }

    @Test
    fun el_marcado_persiste_al_cambiar_de_paso() {
        val steps = recetaConCincoPasos().steps

        val marcado = CookingModeState.initial(recetaConCincoPasos()).toggleCompleted(1)
        val avanzado = marcado.next(steps)

        assertTrue(1 in avanzado.completedOrders)
        assertEquals(1, avanzado.stepIndex)
    }

    @Test
    fun una_receta_sin_pasos_no_hace_crash_y_no_sale_de_rango() {
        val state = CookingModeState.initial(recetaSinPasos())

        assertEquals(0, state.stepIndex)
        assertEquals(0, state.totalSteps)
        assertTrue(state.isEmpty)
        assertTrue(state.isFirstStep)
        assertTrue(state.isLastStep)
        assertFalse(state.timer.hasDuration)

        // Guarda defensiva: navegar no lanza ni cambia el indice.
        assertEquals(state, state.next(emptyList()))
        assertEquals(state, state.previous(emptyList()))
    }

    @Test
    fun una_lista_de_pasos_mas_corta_que_el_total_no_lanza() {
        val state = CookingModeState(
            stepIndex = 0,
            totalSteps = 3,
            timer = CookingTimerState.of(10),
        )

        val siguiente = state.next(listOf(RecipeStep(order = 1, title = "Único", description = "", minutes = 5)))

        assertEquals(1, siguiente.stepIndex)
        assertFalse(siguiente.timer.hasDuration)
    }

    @Test
    fun las_operaciones_no_mutan_el_estado_original() {
        val original = CookingModeState.initial(recetaConCincoPasos())

        val cambiado = original.next(recetaConCincoPasos().steps).toggleCompleted(1)

        assertEquals(0, original.stepIndex)
        assertTrue(original.completedOrders.isEmpty())
        assertNotEquals(original, cambiado)
    }

    private fun recetaConCincoPasos(): Recipe = receta(
        steps = listOf(
            RecipeStep(order = 1, title = "Pelar y cortar", description = "Pela las patatas.", minutes = 10),
            RecipeStep(order = 2, title = "Freír", description = "Fríe a fuego medio.", minutes = 8),
            RecipeStep(order = 3, title = "Cuajar", description = "Cuaja la tortilla.", minutes = 5),
            RecipeStep(order = 4, title = "Emplatar", description = "Sirve caliente.", minutes = 2),
            RecipeStep(order = 5, title = "Reposar", description = "Deja reposar.", minutes = 3),
        ),
    )

    private fun recetaSinPasos(): Recipe = receta(steps = emptyList())

    private fun receta(steps: List<RecipeStep>): Recipe = Recipe(
        id = "receta-1",
        source = RecipeSource.SEED,
        title = "Receta de prueba",
        description = "",
        categoryId = "other",
        mealTypeId = "lunch",
        cuisineId = "spanish",
        totalMinutes = 30,
        prepMinutes = 10,
        cookMinutes = 20,
        difficultyId = "easy",
        servings = 4,
        ingredients = emptyList(),
        steps = steps,
    )
}
