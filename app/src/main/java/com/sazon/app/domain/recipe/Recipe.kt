package com.sazon.app.domain.recipe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RecipeSource {
    @SerialName("seed")
    SEED,

    @SerialName("ai")
    AI,
}

@Serializable
enum class MeasureUnit {
    @SerialName("g")
    GRAM,

    @SerialName("ml")
    MILLILITER,

    @SerialName("ud")
    UNIT,

    @SerialName("cda")
    TABLESPOON,

    @SerialName("cdta")
    TEASPOON,
}

@Serializable
data class Recipe(
    val id: String,
    val source: RecipeSource,
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
    val matchScore: Int? = null,
)

@Serializable
data class RecipeIngredient(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit? = null,
    val isPantry: Boolean = false,
    val ingredientId: String? = null,
)

@Serializable
data class RecipeStep(
    val order: Int,
    val title: String,
    val description: String,
    val minutes: Int,
)

@Serializable
data class Nutrition(
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)
