package com.sazon.app.ui.generator

import com.sazon.app.domain.taxonomy.Ingredient
import java.text.Normalizer

/**
 * Estado **puro** del paso 1 del Generador ("¿Qué tienes?"): la lista de ingredientes anadidos (de la
 * Taxonomia o libres), la consulta del input libre y las operaciones para anadir/quitar/alternar.
 *
 * No importa Compose ni Android (solo `domain.taxonomy` y `java.text.Normalizer`), de modo que se
 * testea en JVM. Las operaciones son inmutables (`copy`): cada accion devuelve un estado nuevo.
 *
 * Reglas fijadas por la spec `docs/specs/generator-ingredients-step.md`:
 * - dedupe de Taxonomia por `ingredientId` ([toggleTaxonomy]: alterna),
 * - dedupe de libres por **nombre normalizado** (`trim` + minusculas + sin acentos) tambien contra los
 *   ya anadidos (de Taxonomia o libres),
 * - un texto en blanco es un **no-op** ([addFree]),
 * - [canContinue] es verdadero en cuanto hay **al menos un** ingrediente.
 */
data class GeneratorIngredientsState(
    val ingredients: List<GeneratorIngredient> = emptyList(),
    val query: String = "",
) {
    /** Numero de ingredientes anadidos. */
    val count: Int get() = ingredients.size

    val isEmpty: Boolean get() = ingredients.isEmpty()

    /** El paso 1 solo deja avanzar con al menos un ingrediente. */
    val canContinue: Boolean get() = ingredients.isNotEmpty()

    /** Contador con concordancia singular/plural: "0 ingredientes anadidos", "1 ingrediente anadido", ... */
    val countLabel: String
        get() = when (count) {
            0 -> "0 ingredientes añadidos"
            1 -> "1 ingrediente añadido"
            else -> "$count ingredientes añadidos"
        }

    /** Actualiza la consulta del input libre. */
    fun typeQuery(text: String): GeneratorIngredientsState = copy(query = text)

    /**
     * Anade o quita un ingrediente de la Taxonomia: si su `id` ya esta entre los anadidos lo quita
     * (segundo toque); si no, lo anade con su `label` e `id`.
     */
    fun toggleTaxonomy(ingredient: Ingredient): GeneratorIngredientsState {
        val existing = ingredients.firstOrNull { it.ingredientId == ingredient.id }
        return if (existing != null) {
            copy(ingredients = ingredients - existing)
        } else {
            copy(
                ingredients = ingredients + GeneratorIngredient(
                    name = ingredient.label,
                    ingredientId = ingredient.id,
                ),
            )
        }
    }

    /**
     * Anade un ingrediente **libre** desde la consulta: recorta el texto; si queda vacio **no** anade;
     * si su nombre normalizado coincide con el de un ingrediente ya anadido **no** anade; en otro caso
     * anade un libre (`ingredientId = null`) y **vacia la consulta**.
     */
    fun addFree(): GeneratorIngredientsState {
        val name = query.trim()
        if (name.isEmpty()) return this
        if (ingredients.any { normalize(it.name) == normalize(name) }) return this
        return copy(
            ingredients = ingredients + GeneratorIngredient(name = name, ingredientId = null),
            query = "",
        )
    }

    /** Quita el chip concreto (el primer ingrediente igual, por valor). */
    fun remove(ingredient: GeneratorIngredient): GeneratorIngredientsState =
        copy(ingredients = ingredients - ingredient)

    private companion object {
        private val DIACRITICS = Regex("\\p{Mn}+")

        /** Normaliza para el dedupe: `trim` + minusculas + sin acentos. */
        fun normalize(raw: String): String =
            Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase()
    }
}
