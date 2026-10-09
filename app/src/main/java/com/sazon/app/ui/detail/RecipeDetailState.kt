package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Servings

/** Pestanas del Detalle de receta, en orden, con su microcopy. */
enum class RecipeDetailTab(val label: String) {
    INGREDIENTS("Ingredientes"),
    STEPS("Pasos"),
    NOTES("Notas"),
}

/**
 * Estado del Detalle de receta: comensales + pestana activa. Logica **pura**, sin Compose ni Android.
 *
 * No guarda el `Recipe` (para poder serializarse con un `Saver` y reutilizarse): el escalado se
 * computa con `ServingScaler.scaledIngredients(recipe, state.servings)`. Las operaciones son inmutables
 * y `setServings` acota siempre a los limites de la Taxonomia ([Servings]).
 */
data class RecipeDetailState(
    val servings: Int,
    val selectedTab: RecipeDetailTab = RecipeDetailTab.INGREDIENTS,
) {
    /** Devuelve una copia con [value] recortado a `limits` (`1..12` en la Taxonomia real). */
    fun setServings(value: Int, limits: Servings): RecipeDetailState =
        copy(servings = value.coerceIn(limits.min, limits.max))

    /** Devuelve una copia con [tab] como pestana activa. */
    fun selectTab(tab: RecipeDetailTab): RecipeDetailState = copy(selectedTab = tab)

    companion object {
        /**
         * Estado inicial: raciones de la receta recortadas a `limits` y pestana `INGREDIENTS`.
         */
        fun initial(recipe: Recipe, limits: Servings): RecipeDetailState =
            RecipeDetailState(servings = recipe.servings.coerceIn(limits.min, limits.max))
    }
}
