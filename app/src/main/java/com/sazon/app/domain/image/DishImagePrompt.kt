package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Constructor del prompt de imagen de un plato: una sola plantilla, siempre la misma, traducida de la
 * [Recipe] y la Taxonomia (patron de `PromptBuilder`, `docs/ai-dish-images-proposal.md` §4.2).
 *
 * Plantilla resultante:
 *
 * ```
 * <PROMPT_PREFIX> <plato>[, <cocina>], <STYLE_SUFFIX>
 * ```
 *
 * - `<plato>` = [DishVisualDescriptor.imagePrompt] del descriptor (fase 5) si existe y no esta en
 *   blanco; si no, el titulo de la receta con espacios colapsados; si queda vacio, la etiqueta de su
 *   `categoryId` real en la Taxonomia; si tampoco hay, [DEFAULT_DISH].
 * - `<cocina>` = etiqueta de la cocina de la Taxonomia **solo** si existe y no esta en blanco; en
 *   cualquier otro caso la clausula se **omite** (nunca un marcador de "desconocida").
 *
 * El titulo conserva mayusculas y diacriticos: el prompt es texto para un modelo de imagen (la forma
 * natural ayuda) y la forma normalizada sin acentos es exclusiva de [DishImageCacheKey].
 *
 * Logica **pura**: sin Android, sin red, sin estado, sin `Random` ni reloj. Misma entrada -> misma
 * cadena; el resultado **nunca** esta en blanco y contiene [STYLE_SUFFIX] exactamente una vez. No
 * lanza por datos de entrada (titulo/categoria/cocina vacios, `id` desconocidos o Taxonomia vacia).
 */
object DishImagePrompt {

    /** Arranque fijo del prompt. */
    const val PROMPT_PREFIX = "Fotografía gastronómica de"

    /** Plato de ultimo recurso cuando la receta no tiene ni titulo ni categoria real. */
    const val DEFAULT_DISH = "plato casero"

    /** Coletilla de estilo fija de la casa (un unico lugar, pura y testeada). */
    const val STYLE_SUFFIX =
        "cerámica artesanal, luz natural suave, estilo editorial gastronómico, sin texto ni manos"

    private val WHITESPACE = Regex("\\s+")

    /** Prompt de imagen de [recipe]; determinista y nunca en blanco. */
    fun build(taxonomy: Taxonomy, recipe: Recipe, descriptor: DishVisualDescriptor? = null): String {
        val dish = dishName(taxonomy, recipe, descriptor)
        val cuisine = taxonomy.cuisine(recipe.cuisineId)
            ?.label
            ?.let(::singleLine)
            ?.takeIf { it.isNotEmpty() }
        return if (cuisine == null) {
            "$PROMPT_PREFIX $dish, $STYLE_SUFFIX"
        } else {
            "$PROMPT_PREFIX $dish, $cuisine, $STYLE_SUFFIX"
        }
    }

    /**
     * Degradacion `imagePrompt del descriptor -> titulo -> etiqueta de categoria real -> DEFAULT_DISH`.
     * El descriptor (fase 5) **solo** cambia la fuente del plato: prefijo, cocina y [STYLE_SUFFIX]
     * siguen intactos.
     */
    private fun dishName(
        taxonomy: Taxonomy,
        recipe: Recipe,
        descriptor: DishVisualDescriptor?,
    ): String {
        descriptor?.imagePrompt
            ?.let(::singleLine)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        singleLine(recipe.title).takeIf { it.isNotEmpty() }?.let { return it }
        val category = taxonomy.recipeCategory(recipe.categoryId)?.label?.let(::singleLine)
        if (!category.isNullOrEmpty()) return category
        return DEFAULT_DISH
    }

    /** Colapsa cualquier espacio en blanco para no ensuciar la plantilla con espacios dobles. */
    private fun singleLine(value: String): String = WHITESPACE.replace(value, " ").trim()
}
