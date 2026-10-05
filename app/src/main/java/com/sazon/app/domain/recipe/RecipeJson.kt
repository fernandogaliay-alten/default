package com.sazon.app.domain.recipe

import kotlinx.serialization.json.Json

object RecipeJson {

    private val format = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(recipe: Recipe): String =
        format.encodeToString(Recipe.serializer(), recipe)

    fun parse(source: String): Recipe =
        format.decodeFromString(Recipe.serializer(), source)
}
