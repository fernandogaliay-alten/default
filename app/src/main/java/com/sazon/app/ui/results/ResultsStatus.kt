package com.sazon.app.ui.results

import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Estado **puro** de la pantalla de Resultados, derivado del [GenerationResult] y del Perfil.
 *
 * Logica pura (sin Compose ni Android) y testeable en JVM. [from] decide el estado, **puntua** cada
 * receta con [MatchScore] y la ordena por relevancia (`matchScore` descendente, empates estables al
 * orden original) materializando el score en memoria con `copy` (nunca persistido):
 *
 * - `accepted` no vacio -> [Content] (solo aceptadas; las `discarded` **nunca** se pintan),
 * - si no, `aiUnavailable` -> [Unavailable] con las recetas semilla puntuadas/ordenadas (puede ser
 *   vacio),
 * - si no -> [Empty].
 *
 * La presentacion de [Unavailable] depende de `seedRecipes.isEmpty()` (franja ambar + lista vs titular
 * de error), pero **ambos** ofrecen "Reintentar" (lo resuelve la capa Compose).
 */
sealed interface ResultsStatus {

    /** Resultados poblados: recetas aceptadas, ya con `matchScore`, ordenadas por relevancia. */
    data class Content(val recipes: List<Recipe>) : ResultsStatus

    /** Sin recetas aceptadas ni fallo de IA. */
    data object Empty : ResultsStatus

    /** IA no disponible: semillas locales (vacio -> titular de error) con accion de reintento. */
    data class Unavailable(val seedRecipes: List<Recipe>) : ResultsStatus

    companion object {

        /**
         * Construye el estado (puntuado y ordenado) a partir del resultado de la generacion.
         *
         * @param userIngredients ingredientes del borrador del Generador (para la cobertura); vacio si
         *   no aplica.
         * @param seedRecipes recetas semilla locales que se muestran en modo degradado.
         */
        fun from(
            result: GenerationResult,
            preferences: PreferenceProfile,
            taxonomy: Taxonomy,
            userIngredients: List<RecipeIngredient>,
            seedRecipes: List<Recipe>,
        ): ResultsStatus = when {
            result.accepted.isNotEmpty() -> Content(
                scoreAndRank(result.accepted, preferences, taxonomy, userIngredients),
            )

            result.aiUnavailable -> Unavailable(
                scoreAndRank(seedRecipes, preferences, taxonomy, userIngredients),
            )

            else -> Empty
        }

        private fun scoreAndRank(
            recipes: List<Recipe>,
            preferences: PreferenceProfile,
            taxonomy: Taxonomy,
            userIngredients: List<RecipeIngredient>,
        ): List<Recipe> = recipes
            .map { recipe ->
                recipe.copy(
                    matchScore = MatchScore.of(recipe, preferences, taxonomy, userIngredients),
                )
            }
            .sortedByDescending { it.matchScore }
    }
}
