package com.sazon.app.domain.taxonomy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class Items<T>(val items: List<T>)

@Serializable
private data class IngredientsNode(
    val categoryFallbackIcon: Map<String, String> = emptyMap(),
    val items: List<Ingredient>,
)

@Serializable
private data class TaxonomyFile(
    val dietPatterns: Items<DietPattern>,
    val hardRestrictions: Items<HardRestriction>,
    val softPreferences: Items<SoftPreference>,
    val mealTypes: Items<MealType>,
    val cuisines: Items<Cuisine>,
    val timeBuckets: Items<TimeBucket>,
    val difficulties: Items<Difficulty>,
    val servings: Servings,
    val avatarColors: Items<AvatarColor>,
    val ingredients: IngredientsNode,
    val recipeCategories: Items<RecipeCategory>,
)

object TaxonomyJson {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(source: String): Taxonomy {
        val file = json.decodeFromString(TaxonomyFile.serializer(), source)
        return Taxonomy(
            dietPatterns = file.dietPatterns.items,
            hardRestrictions = file.hardRestrictions.items,
            softPreferences = file.softPreferences.items,
            mealTypes = file.mealTypes.items,
            cuisines = file.cuisines.items,
            timeBuckets = file.timeBuckets.items,
            difficulties = file.difficulties.items,
            servings = file.servings,
            avatarColors = file.avatarColors.items,
            ingredients = file.ingredients.items,
            recipeCategories = file.recipeCategories.items,
            categoryFallbackIcon = file.ingredients.categoryFallbackIcon,
        )
    }
}
