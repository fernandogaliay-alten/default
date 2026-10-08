package com.sazon.app.data.suggestions

import com.sazon.app.domain.recipe.RecipeJson
import com.sazon.app.domain.suggestions.SuggestionBatch

fun SuggestionBatchEntity.toDomain(): SuggestionBatch =
    SuggestionBatch(
        preferenceSignature = preferenceSignature,
        generatedAt = generatedAt,
        recipes = RecipeJson.parseList(recipesJson),
    )

fun SuggestionBatch.toEntity(): SuggestionBatchEntity =
    SuggestionBatchEntity(
        id = SuggestionBatchEntity.SINGLETON_ID,
        preferenceSignature = preferenceSignature,
        generatedAt = generatedAt,
        recipesJson = RecipeJson.encodeList(recipes),
    )
