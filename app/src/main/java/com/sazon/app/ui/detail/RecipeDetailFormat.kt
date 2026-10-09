package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Formatos puros del Detalle de receta: la meta del hero (cuatro pares etiqueta/valor) y las captions.
 * Sin Compose ni Android; testeable en JVM.
 */
object RecipeDetailFormat {

    /** Caption que acompana al ajustador de comensales (microcopy de `stitch/16-detalle-receta.md`). */
    const val ADJUSTED_CAPTION = "Cantidades ajustadas automáticamente"

    /** Etiqueta si una receta no trae nutricion: nunca inventa cifras. */
    const val NO_NUTRITION = "—"

    data class Meta(val label: String, val value: String)

    /**
     * Los cuatro pares del hero: `Tiempo`, `Dificultad` (etiqueta real, con respaldo al `id`),
     * `Comensales` y `Calorias` (`"—"` si `nutrition == null`; sin `matchScore`).
     */
    fun metaEntries(taxonomy: Taxonomy, recipe: Recipe): List<Meta> = listOf(
        Meta(label = "Tiempo", value = "${recipe.totalMinutes} min"),
        Meta(
            label = "Dificultad",
            value = taxonomy.difficulty(recipe.difficultyId)?.label ?: recipe.difficultyId,
        ),
        Meta(label = "Comensales", value = recipe.servings.toString()),
        Meta(
            label = "Calorías",
            value = recipe.nutrition?.let { "${it.kcal} kcal" } ?: NO_NUTRITION,
        ),
    )
}
