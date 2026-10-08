package com.sazon.app.ui.preferences

import com.sazon.app.domain.taxonomy.Ingredient
import java.text.Normalizer

/**
 * Filtro **puro** del buscador del editor de ingredientes (paso 5). No importa Compose ni Android, de
 * modo que se testea en JVM.
 *
 * La coincidencia es por subcadena sobre el `label`, insensible a mayusculas y a acentos; una consulta
 * en blanco devuelve todas las opciones.
 */
object IngredientsFilter {

    fun matching(ingredients: List<Ingredient>, query: String): List<Ingredient> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return ingredients
        return ingredients.filter { normalize(it.label).contains(normalizedQuery) }
    }

    private fun normalize(raw: String): String {
        val withoutAccents = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
        return withoutAccents.lowercase()
    }

    private val DIACRITICS = Regex("\\p{Mn}+")
}
