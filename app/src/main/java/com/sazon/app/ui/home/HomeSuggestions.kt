package com.sazon.app.ui.home

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.RecipeConstraints
import com.sazon.app.domain.validation.RecipeValidation
import com.sazon.app.domain.validation.RecipeValidator
import com.sazon.app.ui.results.MatchScore

/**
 * Contenido del carrusel "Sugeridas para ti", puro y testeable en JVM.
 *
 * Fuente: el Lote de sugerencias cuando ya se ha resuelto, o las recetas semilla empaquetadas mientras
 * no hay lote (`null`). Un lote ya resuelto **vacio** no cae a semilla: es un resultado real de la
 * politica cache-first (el generador puede haber devuelto cero recetas), no una ausencia de dato.
 *
 * La fuente (semilla o `ai`) pasa **siempre** por el mismo camino, en este orden:
 * 1. **Filtro de seguridad**: se descartan las recetas que incumplen una Restriccion dura o el Patron
 *    de dieta, reutilizando [RecipeValidator] (la capa aceptada del dominio; no se reimplementa). Se
 *    conserva la receta **original**, no la normalizada que devuelve `Accepted`, porque la Home pinta
 *    la receta tal cual.
 * 2. **Orden por relevancia**: [MatchScore] descendente (ingredientes preferidos, cocinas favoritas,
 *    franja de tiempo y dificultad, penalizando los ingredientes que el usuario evita). El score se
 *    calcula **una sola vez** por receta (decorar → ordenar → desordenar) y `sortedByDescending` es
 *    **estable**: los empates conservan el orden de origen. `userIngredients` se deja por defecto
 *    (vacio): la Home no declara ingredientes, asi que la señal de cobertura no aplica.
 * 3. **Tope**: como maximo [MAX_VISIBLE] tarjetas.
 *
 * El score **no** se materializa (`copy(matchScore = ...)`): las `Recipe` devueltas conservan su
 * `matchScore` original (hoy `null` para semilla y `ai` persistida), de modo que la pildora de
 * coincidencia de `RecipeCard` no se pinta y el aspecto del carrusel no cambia. Las preferencias
 * **blandas** (p. ej. "ingredientes que evito") solo penalizan el ranking; **nunca** descartan.
 *
 * Logica pura: sin Compose ni Android, sin dependencias nuevas.
 */
object HomeSuggestions {

    /** Tope de tarjetas del carrusel "Sugeridas para ti". */
    const val MAX_VISIBLE = 6

    fun visible(
        batchRecipes: List<Recipe>?,
        seedRecipes: List<Recipe>,
        preferences: PreferenceProfile,
        taxonomy: Taxonomy,
    ): List<Recipe> {
        val source = batchRecipes ?: seedRecipes

        // Mismo mapeo inline Perfil -> RecipeConstraints que `SuggestionBatchService`, `GenerationService`
        // y `AiSearchService`; no se crea un mapper compartido ni se toca el dominio.
        val constraints = RecipeConstraints(
            dietPatternId = preferences.dietPatternId,
            hardRestrictionIds = preferences.hardRestrictionIds.toSet(),
        )

        return source
            .filter { recipe ->
                RecipeValidator.validateRecipe(recipe, taxonomy, constraints) is RecipeValidation.Accepted
            }
            .map { recipe -> recipe to MatchScore.of(recipe, preferences, taxonomy) }
            .sortedByDescending { (_, score) -> score }
            .take(MAX_VISIBLE)
            .map { (recipe, _) -> recipe }
    }
}
