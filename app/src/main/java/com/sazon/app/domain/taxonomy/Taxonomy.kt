package com.sazon.app.domain.taxonomy

import kotlinx.serialization.Serializable

@Serializable
data class DietPattern(
    val id: String,
    val label: String,
    val description: String,
    val excludedTags: List<String>,
    val promptHint: String? = null,
)

@Serializable
data class HardRestriction(
    val id: String,
    val label: String,
    val icon: String,
    val keywords: List<String>,
)

@Serializable
data class SoftPreference(
    val id: String,
    val label: String,
    val icon: String,
    val description: String,
    val promptHint: String? = null,
)

@Serializable
data class MealType(
    val id: String,
    val label: String,
    val icon: String,
)

@Serializable
data class Cuisine(
    val id: String,
    val label: String,
    val icon: String,
)

@Serializable
data class TimeBucket(
    val id: String,
    val label: String,
    val maxMinutes: Int? = null,
)

@Serializable
data class Difficulty(
    val id: String,
    val label: String,
    val level: Int,
)

@Serializable
data class Servings(
    val min: Int,
    val max: Int,
    val default: Int,
)

@Serializable
data class AvatarColor(
    val id: String,
    val label: String,
    val hex: String,
)

@Serializable
data class Ingredient(
    val id: String,
    val label: String,
    val category: String,
    val icon: String,
    val allergens: List<String>,
    val tags: List<String>,
)

@Serializable
data class RecipeCategory(
    val id: String,
    val label: String,
)

class Taxonomy(
    val dietPatterns: List<DietPattern>,
    val hardRestrictions: List<HardRestriction>,
    val softPreferences: List<SoftPreference>,
    val mealTypes: List<MealType>,
    val cuisines: List<Cuisine>,
    val timeBuckets: List<TimeBucket>,
    val difficulties: List<Difficulty>,
    val servings: Servings,
    val avatarColors: List<AvatarColor>,
    val ingredients: List<Ingredient>,
    val recipeCategories: List<RecipeCategory>,
    val categoryFallbackIcon: Map<String, String>,
) {
    private val dietPatternsById = dietPatterns.associateBy { it.id }
    private val hardRestrictionsById = hardRestrictions.associateBy { it.id }
    private val softPreferencesById = softPreferences.associateBy { it.id }
    private val mealTypesById = mealTypes.associateBy { it.id }
    private val cuisinesById = cuisines.associateBy { it.id }
    private val timeBucketsById = timeBuckets.associateBy { it.id }
    private val difficultiesById = difficulties.associateBy { it.id }
    private val avatarColorsById = avatarColors.associateBy { it.id }
    private val ingredientsById = ingredients.associateBy { it.id }
    private val recipeCategoriesById = recipeCategories.associateBy { it.id }

    fun dietPattern(id: String): DietPattern? = dietPatternsById[id]

    fun restriction(id: String): HardRestriction? = hardRestrictionsById[id]

    fun softPreference(id: String): SoftPreference? = softPreferencesById[id]

    fun mealType(id: String): MealType? = mealTypesById[id]

    fun cuisine(id: String): Cuisine? = cuisinesById[id]

    fun timeBucket(id: String): TimeBucket? = timeBucketsById[id]

    fun difficulty(id: String): Difficulty? = difficultiesById[id]

    fun avatarColor(id: String): AvatarColor? = avatarColorsById[id]

    fun ingredient(id: String): Ingredient? = ingredientsById[id]

    fun recipeCategory(id: String): RecipeCategory? = recipeCategoriesById[id]
}
