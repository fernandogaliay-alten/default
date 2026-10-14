package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.detail.IngredientFormat

/**
 * Ingrediente principal de la ficha tipografica: el **nombre** y su **cantidad ya formateada**
 * (`IngredientFormat.quantityLabel`, reutilizada del Detalle). Es una proyeccion pura, sin Compose ni
 * Android: la UI solo decide como la pinta (una linea o tres filas con cifras tabulares).
 */
data class MainIngredient(
    val name: String,
    val quantityLabel: String,
)

/**
 * Formatos puros de la tarjeta de receta ([RecipeCard], `ResultRecipeCard` y la rejilla de Favoritos):
 * la linea de categoria, la linea de metadatos, los **tres ingredientes principales** y la
 * coincidencia. Testeables en JVM, sin Compose ni Android.
 *
 * `matchScore` es derivado y lo fija `results-screen`; en el carrusel de la Home y en Favoritos vale
 * `null` y la coincidencia no se pinta.
 */
object RecipeCardFormat {

    /** Ingredientes principales que pinta la ficha (Escenario 4: hasta [limit] = 3). */
    const val MAIN_INGREDIENTS = 3

    /**
     * Linea de categoria en versalitas (`"Arroz · Latina"`): **categoria de la receta · cocina**, con
     * respaldo al propio id cuando la Taxonomia no trae la etiqueta. La UI la pinta en mayusculas.
     */
    fun categoryLine(taxonomy: Taxonomy, recipe: Recipe): String {
        val category = taxonomy.recipeCategory(recipe.categoryId)?.label ?: recipe.categoryId
        val cuisine = taxonomy.cuisine(recipe.cuisineId)?.label ?: recipe.cuisineId
        return "$category · $cuisine"
    }

    fun metaLine(taxonomy: Taxonomy, recipe: Recipe): String {
        val difficulty = taxonomy.difficulty(recipe.difficultyId)?.label ?: recipe.difficultyId
        return "${recipe.totalMinutes} min · $difficulty · ${recipe.servings} pers."
    }

    /**
     * Hasta [limit] **ingredientes principales**, deterministas (Escenario 4):
     * - se **excluyen** los `isPantry == true` (sal, aceite, vinagre);
     * - se conserva el **orden de declaracion** de la receta;
     * - si tras excluirlos quedan menos de [limit], se completan con los `isPantry` en su orden;
     * - una receta con menos de [limit] ingredientes devuelve los que haya (nunca menos, nunca mas).
     *
     * Sin `Random`, sin reloj, sin Android y sin red: dos llamadas iguales devuelven la misma lista.
     */
    fun mainIngredients(recipe: Recipe, limit: Int = MAIN_INGREDIENTS): List<MainIngredient> {
        val nonPantry = recipe.ingredients.filterNot { it.isPantry }
        val pantry = recipe.ingredients.filter { it.isPantry }
        return (nonPantry + pantry).take(limit).map { it.toMainIngredient() }
    }

    /**
     * Texto de la coincidencia: `"92% coincidencia"` por defecto (contrato vigente intacto) y `"92%"`
     * en [compact] (la tarjeta estrecha, donde no cabe la palabra). `null` cuando no hay puntuacion.
     */
    fun matchBadge(score: Int?, compact: Boolean = false): String? =
        score?.let { if (compact) "$it%" else "$it% coincidencia" }

    /** Linea de ingredientes de la tarjeta estrecha: nombres unidos por `·` (sin cantidad). */
    fun ingredientsLine(recipe: Recipe): String =
        mainIngredients(recipe).joinToString(separator = " · ") { it.name }

    private fun RecipeIngredient.toMainIngredient(): MainIngredient = MainIngredient(
        name = name,
        quantityLabel = IngredientFormat.quantityLabel(quantity, unit),
    )
}
