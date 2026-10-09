package com.sazon.app.ui.generator

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.RecipeIngredient

/**
 * Mapeo **puro** del borrador del Generador (ingredientes + ajustes) a los tipos de dominio.
 *
 * Sin Compose ni Android (solo `domain.*`), testeable en JVM. Es la costura que consume
 * `GenerationService` en el corte `generation-flow-with-validation`.
 *
 * Reglas:
 * - [ingredients] conserva el `name` y el `ingredientId` (Taxonomia si no es `null`, libre si lo es).
 *   `quantity = 0.0`, `unit = null` y `isPantry = false` porque el input del usuario **no** lleva
 *   cantidad ni unidad: las decide el modelo.
 * - [constraints] construye las [GenerationConstraints] con el `profile` **efectivo**: copia el
 *   [PreferenceProfile] sustituyendo `hardRestrictionIds` por las alergias del paso de ajustes, de modo
 *   que tanto el validador como el prompt (proveedor real) ven la seleccion por generacion. El resto de
 *   campos de ajuste se mapean tal cual; unas notas en blanco pasan a `null`.
 */
object GeneratorRequestFactory {

    fun ingredients(state: GeneratorIngredientsState): List<RecipeIngredient> =
        state.ingredients.map { ingredient ->
            RecipeIngredient(
                name = ingredient.name,
                quantity = 0.0,
                unit = null,
                isPantry = false,
                ingredientId = ingredient.ingredientId,
            )
        }

    fun constraints(
        settings: GeneratorSettingsState,
        profile: PreferenceProfile,
    ): GenerationConstraints =
        GenerationConstraints(
            profile = profile.copy(hardRestrictionIds = settings.hardRestrictionIds),
            servings = settings.servings,
            mealTypeId = settings.mealTypeId,
            timeBucketId = settings.timeBucketId,
            difficultyId = settings.difficultyId,
            notes = settings.notes.ifBlank { null },
        )
}
