package com.sazon.app.data.favorites

import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.recipe.RecipeJson

fun FavoriteEntity.toDomain(): Favorite =
    Favorite(
        recipe = RecipeJson.parse(recipeJson),
        savedAt = savedAt,
    )

fun Favorite.toEntity(): FavoriteEntity =
    FavoriteEntity(
        recipeId = recipeId,
        savedAt = savedAt,
        recipeJson = RecipeJson.encode(recipe),
    )
