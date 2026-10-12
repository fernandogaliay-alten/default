package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeStep

/**
 * Estado del **modo cocina**: paso actual, pasos marcados como hechos y temporizador del paso
 * (logica **pura**, sin Compose ni Android; testeable en JVM).
 *
 * No guarda la `Recipe` (para poder serializarse con un `Saver` en la capa Compose): la navegacion
 * recibe la `List<RecipeStep>` y solo el indice viaja en el estado. Todas las operaciones son
 * inmutables.
 *
 * El **temporizador esta ligado al paso actual**: [next] y [previous] lo reemplazan por el del paso
 * nuevo (Inactivo, con su duracion). El **marcado** ([toggleCompleted]) es **independiente** del
 * indice y del temporizador y persiste entre pasos.
 */
data class CookingModeState(
    val stepIndex: Int,
    val totalSteps: Int,
    val completedOrders: Set<Int> = emptySet(),
    val timer: CookingTimerState,
) {

    /** `true` en el primer paso (o sin pasos): "Anterior" queda deshabilitado. */
    val isFirstStep: Boolean get() = stepIndex <= 0

    /** `true` en el ultimo paso (o sin pasos): el boton primario pasa a leer "Terminar". */
    val isLastStep: Boolean get() = stepIndex >= totalSteps - 1

    /** `true` si la receta no trae pasos: guarda defensiva, el dominio garantiza `1..n`. */
    val isEmpty: Boolean get() = totalSteps <= 0

    /**
     * Avanza al paso siguiente acotando a `0..totalSteps-1` y **reinicia** el temporizador al del
     * paso nuevo. En el ultimo paso (o sin pasos) es **no-op** y devuelve el mismo estado.
     */
    fun next(steps: List<RecipeStep>): CookingModeState {
        if (isEmpty) return this
        val nextIndex = (stepIndex + 1).coerceAtMost(totalSteps - 1)
        if (nextIndex == stepIndex) return this
        return copy(stepIndex = nextIndex, timer = timerFor(steps, nextIndex))
    }

    /**
     * Retrocede al paso anterior acotando a `0..totalSteps-1` y **reinicia** el temporizador al del
     * paso nuevo. En el primer paso (o sin pasos) es **no-op** y devuelve el mismo estado.
     */
    fun previous(steps: List<RecipeStep>): CookingModeState {
        if (isEmpty) return this
        val previousIndex = (stepIndex - 1).coerceAtLeast(0)
        if (previousIndex == stepIndex) return this
        return copy(stepIndex = previousIndex, timer = timerFor(steps, previousIndex))
    }

    /**
     * Anade/quita el `RecipeStep.order` de [completedOrders]; **no** toca el indice ni el
     * temporizador. Llamarlo dos veces con el mismo `order` deja el conjunto como estaba.
     */
    fun toggleCompleted(order: Int): CookingModeState =
        if (order in completedOrders) {
            copy(completedOrders = completedOrders - order)
        } else {
            copy(completedOrders = completedOrders + order)
        }

    /** Devuelve una copia con [timer] como temporizador del paso actual (inmutable). */
    fun withTimer(timer: CookingTimerState): CookingModeState = copy(timer = timer)

    companion object {
        /**
         * Estado inicial del modo: paso 1 (`stepIndex = 0`), tantos pasos como `recipe.steps` y el
         * temporizador Inactivo del primer paso (o `of(0)` si la receta no trae pasos, sin lanzar).
         */
        fun initial(recipe: Recipe): CookingModeState = CookingModeState(
            stepIndex = 0,
            totalSteps = recipe.steps.size,
            completedOrders = emptySet(),
            timer = timerFor(recipe.steps, 0),
        )

        /** Temporizador Inactivo del paso [index], con `getOrNull` (no lanza si la lista es mas corta). */
        private fun timerFor(steps: List<RecipeStep>, index: Int): CookingTimerState =
            CookingTimerState.of(steps.getOrNull(index)?.minutes ?: 0)
    }
}
