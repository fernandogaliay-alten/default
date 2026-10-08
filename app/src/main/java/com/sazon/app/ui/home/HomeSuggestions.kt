package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe

/**
 * Respaldo inmediato del carrusel "Sugeridas para ti", puro y testeable en JVM.
 *
 * El Lote de sugerencias puede resolverse de forma asincrona, asi que la Home pinta las recetas
 * semilla empaquetadas mientras no hay lote (`null`) y las sustituye cuando el Lote emite. Un lote ya
 * resuelto **vacio** no cae a semilla: es un resultado real de la politica cache-first (el generador
 * puede haber devuelto cero recetas), no una ausencia de dato.
 */
object HomeSuggestions {

    fun visible(batchRecipes: List<Recipe>?, seedRecipes: List<Recipe>): List<Recipe> =
        batchRecipes ?: seedRecipes
}
