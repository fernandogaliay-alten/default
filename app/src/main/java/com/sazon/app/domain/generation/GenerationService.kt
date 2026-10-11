package com.sazon.app.domain.generation

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.DiscardedRecipe
import com.sazon.app.domain.validation.RecipeConstraints
import com.sazon.app.domain.validation.RecipeValidation
import com.sazon.app.domain.validation.RecipeValidator
import kotlinx.coroutines.CancellationException

/**
 * Orquestacion pura del corte vertical principal: **generador -> validador**.
 *
 * Pureza: sin Android, sin red, sin persistencia y sin Compose; solo depende del puerto
 * [RecipeGenerator] y de la [Taxonomy], de modo que se testea en JVM.
 *
 * [generate] construye las [RecipeConstraints] con el `profile` **efectivo** que viaja en las
 * [GenerationConstraints] (la UI sustituye sus restricciones por las del paso de ajustes), llama a
 * `generator.generateFromIngredients(...)` y valida **receta a receta** con [RecipeValidator] (el
 * puerto ya devuelve `List<Recipe>`, asi que no se usa `validateResponse`; mismo patron que
 * `SuggestionBatchService`).
 *
 * Modo degradado: la [CancellationException] se **re-lanza** (no se traga, romperia la cancelacion
 * estructurada); cualquier otra [Exception] del generador devuelve un [GenerationResult] con
 * `aiUnavailable = true` y listas vacias, sin propagar. Una receta mala nunca lanza: se descarta con
 * su motivo.
 */
class GenerationService(
    private val generator: RecipeGenerator,
    private val taxonomy: Taxonomy,
    /**
     * Costura aditiva para reportar las recetas **aceptadas** de una generacion (Resumen de
     * actividad). Por defecto es un no-op: no rompe constructores ni tests existentes. El filtro
     * "solo `ai`" lo aplica el servicio de dominio consumidor, no este servicio.
     */
    private val onRecipesGenerated: suspend (List<Recipe>) -> Unit = {},
) {

    suspend fun generate(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): GenerationResult {
        val recipeConstraints = RecipeConstraints(
            dietPatternId = constraints.profile.dietPatternId,
            hardRestrictionIds = constraints.profile.hardRestrictionIds.toSet(),
        )

        val recipes = try {
            generator.generateFromIngredients(ingredients, constraints)
        } catch (cancellation: CancellationException) {
            // No tragarse la cancelacion: romperia la cancelacion estructurada del huesped.
            throw cancellation
        } catch (_: Exception) {
            // Modo esperado, no error: IA no disponible -> sin recetas aceptadas, sin propagar.
            return GenerationResult(accepted = emptyList(), discarded = emptyList(), aiUnavailable = true)
        }

        val accepted = mutableListOf<Recipe>()
        val discarded = mutableListOf<DiscardedRecipe>()
        recipes.forEach { recipe ->
            when (val result = RecipeValidator.validateRecipe(recipe, taxonomy, recipeConstraints)) {
                is RecipeValidation.Accepted -> accepted += result.recipe
                is RecipeValidation.Discarded -> discarded += DiscardedRecipe(
                    reason = result.reason,
                    detail = result.detail,
                    title = recipe.title,
                )
            }
        }

        onRecipesGenerated(accepted)
        return GenerationResult(accepted = accepted, discarded = discarded)
    }
}
