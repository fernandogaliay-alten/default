package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.filters.LocalResultFilter
import com.sazon.app.ui.filters.LocalResultFilters

/**
 * Costura de datos del bloque "Tus recetas" (bloque 5 de la Home): las recetas guardadas y las
 * abiertas hace poco.
 *
 * En esta feature ambas listas llegan vacias desde `MainShell` (Favoritos y "Abiertas hace poco" no
 * existen todavia); `favorites`/`recipe-detail-screen` las alimentaran. [isEmpty] decide el estado
 * vacio de usuario nuevo ("Cuando guardes una receta aparecera aqui.").
 *
 * [filteredBy] aplica el filtro local ([LocalResultFilters.visible]) a cada grupo de forma
 * independiente, sin Compose ni red; es la operacion pura que consume "Tus recetas".
 */
data class HomeRecipes(
    val saved: List<Recipe> = emptyList(),
    val recentlyOpened: List<Recipe> = emptyList(),
) {
    val isEmpty: Boolean get() = saved.isEmpty() && recentlyOpened.isEmpty()

    /** Devuelve una copia con cada grupo acotado por [filter] (mismo orden interno). */
    fun filteredBy(filter: LocalResultFilter, taxonomy: Taxonomy): HomeRecipes = copy(
        saved = LocalResultFilters.visible(saved, filter, taxonomy),
        recentlyOpened = LocalResultFilters.visible(recentlyOpened, filter, taxonomy),
    )
}
