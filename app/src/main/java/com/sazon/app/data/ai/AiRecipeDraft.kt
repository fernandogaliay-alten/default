package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import kotlinx.serialization.Serializable

/**
 * DTO del **wire** del proveedor: la forma que el modelo devuelve de verdad, calcada del contrato de
 * `docs/domain/taxonomy.md` §4 **menos los campos que posee la app** (`id`, `source` y `matchScore`).
 *
 * Es la capa anti-corrupcion del adaptador: el wire del modelo no lleva `id` ni `source` (los asigna
 * la app) y `matchScore` es derivado y no persistido. Al no declarar esos campos y decodificar con
 * `ignoreUnknownKeys = true`, si el modelo los enviara igualmente se **ignoran**: la app siempre
 * posee esos tres campos.
 *
 * Reutiliza [RecipeIngredient], [RecipeStep] y [Nutrition] de `domain.recipe`: son esquema del
 * contrato sin campos de app, por lo que no hace falta duplicarlos.
 */
@Serializable
internal data class AiRecipeDraft(
    val title: String,
    val description: String,
    val categoryId: String,
    val mealTypeId: String,
    val cuisineId: String,
    val totalMinutes: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficultyId: String,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
    val tips: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    val declaredAllergens: List<String> = emptyList(),
) {

    /**
     * Mapea el wire a la Receta de dominio **completando los campos que posee la app**:
     * `id` (recibido, ver [AiRecipeIds]), `source = AI` y `matchScore = null` (derivado).
     */
    fun toRecipe(id: String): Recipe = Recipe(
        id = id,
        source = RecipeSource.AI,
        title = title,
        description = description,
        categoryId = categoryId,
        mealTypeId = mealTypeId,
        cuisineId = cuisineId,
        totalMinutes = totalMinutes,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        difficultyId = difficultyId,
        servings = servings,
        ingredients = ingredients,
        steps = steps,
        tips = tips,
        nutrition = nutrition,
        declaredAllergens = declaredAllergens,
        matchScore = null,
    )
}
