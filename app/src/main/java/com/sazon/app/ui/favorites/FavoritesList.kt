package com.sazon.app.ui.favorites

import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.taxonomy.Taxonomy
import java.text.Normalizer

/**
 * Logica pura de la lista de Favoritos: filtrado por titulo y orden. Testeable en JVM, sin Compose ni
 * Android.
 *
 * El filtro es una busqueda por **titulo** normalizado (minusculas + sin acentos, recortado); una
 * consulta en blanco devuelve todo. El orden depende de [FavoritesSort]: recientes (`savedAt` desc),
 * tiempo (`totalMinutes` asc) o dificultad (indice en `taxonomy.difficulties`). Es estable, de modo
 * que los empates conservan el orden de entrada.
 */
object FavoritesList {

    private val DIACRITICS = "\\p{Mn}+".toRegex()

    fun visible(
        favorites: List<Favorite>,
        query: String,
        sort: FavoritesSort,
        taxonomy: Taxonomy,
    ): List<Favorite> {
        val normalizedQuery = normalize(query)
        val filtered = favorites.filter { favorite ->
            normalizedQuery.isEmpty() || normalize(favorite.recipe.title).contains(normalizedQuery)
        }
        return when (sort) {
            FavoritesSort.RECENT -> filtered.sortedByDescending { it.savedAt }
            FavoritesSort.TIME -> filtered.sortedBy { it.recipe.totalMinutes }
            FavoritesSort.DIFFICULTY -> filtered.sortedBy { difficultyIndex(taxonomy, it.recipe.difficultyId) }
        }
    }

    /** Conteo legible de la cabecera: "1 receta" en singular; "$count recetas" en el resto. */
    fun countLabel(count: Int): String = if (count == 1) "1 receta" else "$count recetas"

    /**
     * Indice de la dificultad en `taxonomy.difficulties`. Una dificultad desconocida degrada a
     * `Int.MAX_VALUE` (queda al final) para que el orden siga siendo estable sin lanzar.
     */
    private fun difficultyIndex(taxonomy: Taxonomy, difficultyId: String): Int {
        val index = taxonomy.difficulties.indexOfFirst { it.id == difficultyId }
        return if (index >= 0) index else Int.MAX_VALUE
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
            .trim()
}
