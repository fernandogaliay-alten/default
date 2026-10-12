package com.sazon.app.data.ai

import java.security.MessageDigest

/**
 * Politica de `id` de las recetas `ai`, **determinista y sin `Random`, reloj ni `UUID`**.
 *
 * El `id` de una receta `ai` es la clave primaria del Favorito (`favorite_recipe.recipeId`) y la
 * identidad de `isSaved`, asi que no puede depender del orden ni del indice de la respuesta: dos
 * llamadas distintas que devolvieran recetas distintas colisionarian y un Favorito apuntaria a otra
 * receta. El hash del **contenido** es unico entre llamadas (salvo duplicado exacto, que se
 * desambigua dentro de la llamada) y, ademas, regenerar el mismo contenido conserva el `id`.
 *
 * Misma disciplina que [com.sazon.app.domain.profile.PreferenceSignature]: canonicalizacion con
 * separadores de control y SHA-256 en hex minuscula (`java.security.MessageDigest` es JDK estandar).
 */
internal object AiRecipeIds {

    /** Prefijo de todos los ids de recetas `ai` (distinto del `fake-*` del generador falso). */
    const val PREFIX = "ai-"

    /**
     * Version del algoritmo de canonicalizacion/hash. Si cambia, los ids de las recetas `ai` cambian
     * (los Favoritos ya guardados conservan su copia independiente).
     */
    private const val ALGORITHM_VERSION = "ai-recipe-v1"

    /** Separador de campos y de elementos de lista (como `PreferenceSignature`, U+001F / U+001E). */
    private const val FIELD_SEPARATOR = "\u001F"
    private const val LIST_SEPARATOR = "\u001E"

    /** Valor de la nutricion ausente: distingue "sin nutricion" de una nutricion con valores 0. */
    private const val NUTRITION_NONE = "none"

    /**
     * Cadena canonica del contenido del draft: prefijo de version y todos los campos del contrato,
     * con separador [FIELD_SEPARATOR] entre campos y [LIST_SEPARATOR] entre elementos de lista.
     *
     * Ingredientes, pasos y tips respetan su **orden** (es semantico en el prompt y en la receta);
     * `declaredAllergens` se **ordena** (es un conjunto, como en el Perfil).
     */
    fun canonical(draft: AiRecipeDraft): String = buildString {
        append(ALGORITHM_VERSION)
        appendField(draft.title)
        appendField(draft.description)
        appendField(draft.categoryId)
        appendField(draft.mealTypeId)
        appendField(draft.cuisineId)
        appendField(draft.totalMinutes.toString())
        appendField(draft.prepMinutes.toString())
        appendField(draft.cookMinutes.toString())
        appendField(draft.difficultyId)
        appendField(draft.servings.toString())
        appendField(
            draft.ingredients.joinToString(LIST_SEPARATOR) { ingredient ->
                listOf(
                    ingredient.name,
                    ingredient.quantity.toString(),
                    ingredient.unit?.name.orEmpty(),
                    ingredient.isPantry.toString(),
                    ingredient.ingredientId.orEmpty(),
                ).joinToString(FIELD_SEPARATOR)
            },
        )
        appendField(
            draft.steps.joinToString(LIST_SEPARATOR) { step ->
                listOf(step.order.toString(), step.title, step.minutes.toString())
                    .joinToString(FIELD_SEPARATOR)
            },
        )
        // `tips` se incluye (aunque la lista de campos de la spec lo omite): el propio Unknown de la
        // spec dice que "un cambio de contenido (p. ej. tips) cambia el id". Excluirlo generaria ids
        // compartidos entre llamadas para recetas que solo difieren en los trucos.
        appendField(draft.tips.joinToString(LIST_SEPARATOR))
        appendField(draft.declaredAllergens.sorted().joinToString(LIST_SEPARATOR))
        appendField(
            draft.nutrition?.let { nutrition ->
                listOf(nutrition.kcal, nutrition.protein, nutrition.carbs, nutrition.fat)
                    .joinToString(FIELD_SEPARATOR)
            } ?: NUTRITION_NONE,
        )
    }

    /**
     * `id` **unico dentro de la llamada**: [PREFIX] + SHA-256 hex minuscula de [canonical]; ante
     * contenido duplicado exacto se prueba `-2`, `-3`, ... (determinista segun el orden de la
     * respuesta) y el `id` elegido se anade a [used].
     */
    fun idFor(draft: AiRecipeDraft, used: MutableSet<String>): String {
        val base = PREFIX + sha256Hex(canonical(draft))
        var candidate = base
        var suffix = 2
        while (!used.add(candidate)) {
            candidate = "$base-$suffix"
            suffix += 1
        }
        return candidate
    }

    private fun StringBuilder.appendField(value: String) {
        append(FIELD_SEPARATOR)
        append(value)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (byte in digest) {
                append("%02x".format(byte.toInt() and 0xFF))
            }
        }
    }
}
