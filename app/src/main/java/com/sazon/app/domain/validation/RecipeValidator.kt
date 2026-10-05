package com.sazon.app.domain.validation

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Restricciones de validacion de una generacion. Desacoplado a proposito de `PreferenceProfile`:
 * la capa que use el validador mapea el Perfil y los ajustes por generacion a estas dos listas.
 */
data class RecipeConstraints(
    val dietPatternId: String,
    val hardRestrictionIds: Set<String>,
)

/** Motivo de descarte de una receta. Coincide con los estados de `docs/domain-model.md`. */
enum class DiscardReason { SCHEMA, DIET_PATTERN, HARD_RESTRICTION, INCOHERENT }

/** Receta descartada con su motivo legible. [title] es `null` si no llego a decodificarse. */
data class DiscardedRecipe(
    val reason: DiscardReason,
    val detail: String,
    val title: String? = null,
)

/** Resultado de validar una receta individual. */
sealed interface RecipeValidation {
    data class Accepted(val recipe: Recipe) : RecipeValidation
    data class Discarded(val reason: DiscardReason, val detail: String) : RecipeValidation
}

/** Resultado de validar una respuesta completa de la IA. */
sealed interface ValidationOutcome {
    data class Malformed(val detail: String) : ValidationOutcome
    data class ValidationCompleted(
        val accepted: List<Recipe>,
        val discarded: List<DiscardedRecipe>,
    ) : ValidationOutcome
}

/**
 * Capa obligatoria de validacion post-IA (reglas 1-4 de `docs/domain/taxonomy.md` seccion 5).
 *
 * Es logica pura de dominio: sin Android, sin UI, sin red y sin persistencia. Falla rapido ante una
 * Taxonomia corrupta, pero **nunca lanza** ante una respuesta de IA mala: devuelve el motivo.
 */
object RecipeValidator {

    /** Deriva maxima admitida entre la suma de `steps.minutes` y `totalMinutes`. */
    const val MAX_STEP_TOTAL_DRIFT_MINUTES = 5

    /** Unico id con fallback declarado en la Taxonomia (`recipeCategories.fallback`). */
    private const val FALLBACK_CATEGORY_ID = "other"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parsea `{ "recipes": [...] }` elemento a elemento y valida cada receta. Una respuesta ilegible
     * devuelve [ValidationOutcome.Malformed]; una receta ilegible dentro de un lote legible se
     * descarta con [DiscardReason.SCHEMA] sin tumbar el resto.
     */
    fun validateResponse(
        rawResponse: String,
        taxonomy: Taxonomy,
        constraints: RecipeConstraints,
    ): ValidationOutcome {
        val root = runCatching { json.parseToJsonElement(rawResponse) }.getOrNull() as? JsonObject
            ?: return ValidationOutcome.Malformed(
                "La respuesta no es un objeto JSON con la forma { \"recipes\": [...] }.",
            )

        val elements = root["recipes"] as? JsonArray
            ?: return ValidationOutcome.Malformed("La respuesta no contiene un array 'recipes'.")

        val accepted = mutableListOf<Recipe>()
        val discarded = mutableListOf<DiscardedRecipe>()

        elements.forEach { element ->
            val recipe = runCatching {
                json.decodeFromJsonElement(Recipe.serializer(), element)
            }.getOrNull()

            if (recipe == null) {
                discarded += DiscardedRecipe(
                    reason = DiscardReason.SCHEMA,
                    detail = "La receta no se pudo decodificar con el esquema de Receta.",
                    title = titleOf(element),
                )
                return@forEach
            }

            when (val result = validateRecipe(recipe, taxonomy, constraints)) {
                is RecipeValidation.Accepted -> accepted += result.recipe
                is RecipeValidation.Discarded -> discarded += DiscardedRecipe(
                    reason = result.reason,
                    detail = result.detail,
                    title = recipe.title,
                )
            }
        }

        return ValidationOutcome.ValidationCompleted(accepted, discarded)
    }

    /** Aplica las reglas 1-4 en orden y devuelve el primer incumplimiento. */
    fun validateRecipe(
        recipe: Recipe,
        taxonomy: Taxonomy,
        constraints: RecipeConstraints,
    ): RecipeValidation {
        // Regla 1: esquema / ids de clasificacion.
        taxonomy.mealType(recipe.mealTypeId)
            ?: return discard(DiscardReason.SCHEMA, "Campo 'mealTypeId' con id desconocido '${recipe.mealTypeId}'.")
        taxonomy.cuisine(recipe.cuisineId)
            ?: return discard(DiscardReason.SCHEMA, "Campo 'cuisineId' con id desconocido '${recipe.cuisineId}'.")
        taxonomy.difficulty(recipe.difficultyId)
            ?: return discard(DiscardReason.SCHEMA, "Campo 'difficultyId' con id desconocido '${recipe.difficultyId}'.")

        val normalizedCategoryId = if (taxonomy.recipeCategory(recipe.categoryId) != null) {
            recipe.categoryId
        } else {
            FALLBACK_CATEGORY_ID
        }

        val normalizedIngredients = recipe.ingredients.map { ingredient ->
            val ingredientId = ingredient.ingredientId
            if (ingredientId != null && taxonomy.ingredient(ingredientId) == null) {
                ingredient.copy(ingredientId = null)
            } else {
                ingredient
            }
        }

        // Regla 2: patron de dieta (excludedTags sobre ingredientes resueltos por id).
        val dietPattern = taxonomy.dietPattern(constraints.dietPatternId)
        if (dietPattern != null) {
            val excludedTags = dietPattern.excludedTags.toSet()
            normalizedIngredients.forEach { ingredient ->
                val resolved = ingredient.ingredientId?.let { taxonomy.ingredient(it) } ?: return@forEach
                val offending = resolved.tags.firstOrNull { it in excludedTags } ?: return@forEach
                return discard(
                    DiscardReason.DIET_PATTERN,
                    "El ingrediente '${ingredient.name}' (${resolved.id}) tiene el tag '$offending' " +
                        "excluido por el patron de dieta '${dietPattern.id}'.",
                )
            }
        }

        // Regla 3: restricciones duras (allergens curados + keywords sobre el nombre).
        val activeRestrictions = constraints.hardRestrictionIds.mapNotNull { taxonomy.restriction(it) }
        normalizedIngredients.forEach { ingredient ->
            val candidates = mutableSetOf<String>()
            ingredient.ingredientId?.let { taxonomy.ingredient(it) }?.allergens?.forEach { candidates += it }
            activeRestrictions.forEach { restriction ->
                if (IngredientText.matchesAny(ingredient.name, restriction.keywords)) {
                    candidates += restriction.id
                }
            }
            val hit = candidates.firstOrNull { it in constraints.hardRestrictionIds }
            if (hit != null) {
                return discard(
                    DiscardReason.HARD_RESTRICTION,
                    "El ingrediente '${ingredient.name}' activa la restriccion dura '$hit'.",
                )
            }
        }

        // Regla 4a: coherencia de tiempos.
        val stepMinutes = recipe.steps.sumOf { it.minutes }
        if (abs(stepMinutes - recipe.totalMinutes) > MAX_STEP_TOTAL_DRIFT_MINUTES) {
            return discard(
                DiscardReason.INCOHERENT,
                "Los pasos suman $stepMinutes min pero totalMinutes es ${recipe.totalMinutes} min " +
                    "(deriva mayor que $MAX_STEP_TOTAL_DRIFT_MINUTES min).",
            )
        }

        // Regla 4b: declaredAllergens debe cubrir los allergens curados de los ingredientes resueltos.
        val declared = recipe.declaredAllergens.toSet()
        val required = normalizedIngredients
            .mapNotNull { it.ingredientId?.let { id -> taxonomy.ingredient(id) } }
            .flatMap { it.allergens }
            .toSet()
        val missing = (required - declared).sorted()
        if (missing.isNotEmpty()) {
            return discard(
                DiscardReason.INCOHERENT,
                "La receta usa alergenos no declarados en declaredAllergens: ${missing.joinToString(", ")}.",
            )
        }

        return RecipeValidation.Accepted(
            recipe.copy(categoryId = normalizedCategoryId, ingredients = normalizedIngredients),
        )
    }

    private fun discard(reason: DiscardReason, detail: String): RecipeValidation.Discarded =
        RecipeValidation.Discarded(reason, detail)

    private fun titleOf(element: JsonElement): String? =
        ((element as? JsonObject)?.get("title") as? JsonPrimitive)?.contentOrNull
}
