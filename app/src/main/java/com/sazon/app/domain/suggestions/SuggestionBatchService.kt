package com.sazon.app.domain.suggestions

import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.PreferenceSignature
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.RecipeConstraints
import com.sazon.app.domain.validation.RecipeValidation
import com.sazon.app.domain.validation.RecipeValidator
import java.text.Normalizer
import kotlinx.coroutines.CancellationException

/**
 * Servicio de dominio que asegura el Lote de sugerencias con politica **cache-first**.
 *
 * Pureza: sin Android ni Room; solo depende del puerto [RecipeGenerator], del puerto
 * [SuggestionBatchRepository], de la [Taxonomy], del catalogo semilla y de un reloj inyectable.
 *
 * Algoritmo de [ensureBatch]:
 * 1. Consume la firma con [PreferenceSignature.of] (no la reimplementa).
 * 2. Lee la cache y decide con [SuggestionBatchPolicy].
 * 3. Si la cache sirve, la devuelve sin reescribir ni llamar al generador.
 * 4. Si hay que generar, lo intenta en modo best-effort: filtra las recetas `ai` con
 *    [RecipeValidator], deduplica por titulo normalizado (gana la `seed`), persiste y devuelve.
 *    Si el generador falla, degrada a solo semilla sin propagar el error.
 */
class SuggestionBatchService(
    private val generator: RecipeGenerator,
    private val repository: SuggestionBatchRepository,
    private val taxonomy: Taxonomy,
    private val seedRecipes: List<Recipe>,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun ensureBatch(
        profile: PreferenceProfile,
        force: Boolean = false,
    ): SuggestionBatch {
        val signature = PreferenceSignature.of(profile)
        val cached = repository.get()
        if (SuggestionBatchPolicy.decide(cached, signature, now(), force) is BatchDecision.UseCached) {
            return checkNotNull(cached)
        }
        return generate(profile, signature)
    }

    private suspend fun generate(profile: PreferenceProfile, signature: String): SuggestionBatch {
        val constraints = RecipeConstraints(
            dietPatternId = profile.dietPatternId,
            hardRestrictionIds = profile.hardRestrictionIds.toSet(),
        )
        val timestamp = now()

        val generated = try {
            generator.suggestByPreferences(profile)
        } catch (cancellation: CancellationException) {
            // No tragarse la cancelacion: romperia la cancelacion estructurada.
            throw cancellation
        } catch (_: Exception) {
            // Modo esperado, no error: IA no disponible -> solo semilla.
            null
        }

        val recipes = if (generated == null) {
            deduplicateByTitle(seedRecipes)
        } else {
            val safeAi = generated.mapNotNull { recipe ->
                if (recipe.source != RecipeSource.AI) {
                    recipe
                } else {
                    when (val result = RecipeValidator.validateRecipe(recipe, taxonomy, constraints)) {
                        is RecipeValidation.Accepted -> result.recipe
                        is RecipeValidation.Discarded -> null
                    }
                }
            }
            deduplicateByTitle(seedRecipes + safeAi)
        }

        val batch = SuggestionBatch(signature, timestamp, recipes)
        repository.save(batch)
        return batch
    }

    private companion object {
        private val DIACRITICS = "\\p{Mn}+".toRegex()

        /** Conserva la primera aparicion por titulo normalizado (seed antes que ai). */
        private fun deduplicateByTitle(recipes: List<Recipe>): List<Recipe> {
            val seen = mutableSetOf<String>()
            return recipes.filter { seen.add(normalizeTitle(it.title)) }
        }

        private fun normalizeTitle(title: String): String =
            Normalizer.normalize(title, Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase()
                .trim()
    }
}
