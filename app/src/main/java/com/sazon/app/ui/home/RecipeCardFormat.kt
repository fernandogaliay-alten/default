package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Formatos puros de la tarjeta de receta ([RecipeCard]): la linea de metadatos y la pildora de
 * coincidencia. Testeables en JVM, sin Compose ni Android.
 *
 * `matchScore` es derivado y lo fija `results-screen`; en esta feature vale `null` y la pildora no
 * se pinta.
 */
object RecipeCardFormat {

    fun metaLine(taxonomy: Taxonomy, recipe: Recipe): String {
        val difficulty = taxonomy.difficulty(recipe.difficultyId)?.label ?: recipe.difficultyId
        return "${recipe.totalMinutes} min · $difficulty · ${recipe.servings} pers."
    }

    fun matchBadge(score: Int?): String? =
        score?.let { "$it% coincidencia" }
}
