package com.sazon.app.domain.recipe

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object RecipeJson {

    private val format = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val listSerializer = ListSerializer(Recipe.serializer())

    fun encode(recipe: Recipe): String =
        format.encodeToString(Recipe.serializer(), recipe)

    fun parse(source: String): Recipe =
        format.decodeFromString(Recipe.serializer(), source)

    /** Ida y vuelta de una lista de recetas (columna JSON del Lote); reusa la config de [format]. */
    fun encodeList(recipes: List<Recipe>): String =
        format.encodeToString(listSerializer, recipes)

    fun parseList(source: String): List<Recipe> =
        format.decodeFromString(listSerializer, source)
}
