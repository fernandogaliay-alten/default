package com.sazon.app.domain.search

import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.DiscardedRecipe
import com.sazon.app.domain.validation.RecipeConstraints
import com.sazon.app.domain.validation.RecipeValidation
import com.sazon.app.domain.validation.RecipeValidator
import kotlinx.coroutines.CancellationException

/**
 * Orquestacion pura de la **Busqueda con IA** de la Home: `searchByText -> validador`.
 *
 * Mismo patron que [com.sazon.app.domain.generation.GenerationService], pero para la otra operacion
 * del mismo puerto. Pureza: sin Android, sin red, sin persistencia y sin Compose; solo depende del
 * puerto [RecipeGenerator] y de la [Taxonomy], de modo que se testea en JVM.
 *
 * [search] normaliza la consulta con [SearchMemo.key] (recorte + minusculas); si queda vacia devuelve
 * un [GenerationResult] vacio **sin** llamar al generador (la UI impide enviar en blanco). Si la clave
 * ya esta en el [memo] devuelve el memo **sin** llamar al generador (misma sesion). Si no, llama a
 * `generator.searchByText(query, profile)` y valida **receta a receta** con [RecipeValidator] (no
 * `validateResponse`: el puerto ya devuelve `List<Recipe>`), construyendo las [RecipeConstraints] con
 * el perfil.
 *
 * Modo degradado: la [CancellationException] se **re-lanza** (romperia la cancelacion estructurada);
 * cualquier otra [Exception] devuelve un [GenerationResult] con `aiUnavailable = true` y **no** se
 * memoiza (asi "Reintentar" vuelve a llamar). Cuando la busqueda completa, reporta las aceptadas por
 * la costura [onRecipesGenerated] y memoiza el resultado.
 */
class AiSearchService(
    private val generator: RecipeGenerator,
    private val taxonomy: Taxonomy,
    /**
     * Costura aditiva para reportar las recetas **aceptadas** de una busqueda (Resumen de actividad).
     * Por defecto es un no-op: no rompe constructores ni tests existentes.
     */
    private val onRecipesGenerated: suspend (List<Recipe>) -> Unit = {},
    private val memo: SearchMemo = SearchMemo(),
) {

    suspend fun search(query: String, profile: PreferenceProfile): GenerationResult {
        val key = SearchMemo.key(query)
        if (key.isEmpty()) {
            return GenerationResult(accepted = emptyList(), discarded = emptyList())
        }

        memo.get(key)?.let { return it }

        val recipeConstraints = RecipeConstraints(
            dietPatternId = profile.dietPatternId,
            hardRestrictionIds = profile.hardRestrictionIds.toSet(),
        )

        val recipes = try {
            generator.searchByText(key, profile)
        } catch (cancellation: CancellationException) {
            // No tragarse la cancelacion: romperia la cancelacion estructurada del huesped.
            throw cancellation
        } catch (_: Exception) {
            // Modo esperado, no error: IA no disponible -> sin recetas y sin memoizar el fallo.
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

        val result = GenerationResult(accepted = accepted, discarded = discarded)
        onRecipesGenerated(accepted)
        memo.put(key, result)
        return result
    }
}
