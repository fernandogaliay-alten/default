package com.sazon.app.domain.activity

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import kotlinx.coroutines.flow.Flow

/**
 * Servicio de dominio del Resumen de actividad: la **unica** regla "solo `ai`, solo suma".
 *
 * Es logica pura (sin Android, Room ni Compose): delega la persistencia en [ActivitySummaryRepository]
 * y decide cuantas recetas de un evento de generacion cuentan. Se cuenta **cada receta `ai` aceptada**
 * por el `RecipeValidator`, no recetas distintas; por eso el contador nunca baja al refrescar el Lote
 * (que reemplaza sus recetas `ai`). El numero de Favoritas no pasa por aqui: se deriva de la lista
 * observada de Favoritos.
 */
class ActivitySummaryService(
    private val repository: ActivitySummaryRepository,
) {

    /** Emite el contador persistido de recetas generadas (0 si nunca se genero nada). */
    fun observeRecipesGeneratedCount(): Flow<Int> = repository.observeRecipesGeneratedCount()

    /** Lee el contador persistido de recetas generadas (0 si nunca se genero nada). */
    suspend fun recipesGeneratedCount(): Int = repository.recipesGeneratedCount()

    /**
     * Registra las recetas aceptadas de un evento de generacion y devuelve el contador persistido.
     *
     * Solo cuentan las recetas `source == AI`; si el delta es `0` no escribe (una generacion sin `ai`
     * aceptadas no altera el contador) y un delta no positivo nunca lo disminuye.
     */
    suspend fun recordAcceptedRecipes(recipes: List<Recipe>): Int {
        val delta = recipes.count { it.source == RecipeSource.AI }
        if (delta > 0) {
            repository.incrementRecipesGenerated(delta)
        }
        return repository.recipesGeneratedCount()
    }
}
