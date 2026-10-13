package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.filters.LocalResultFilter
import com.sazon.app.ui.filters.LocalResultFilters

/**
 * Costura de datos del bloque "Tus recetas" (bloque 5 de la Home): las recetas guardadas.
 *
 * El bloque solo pinta grupos con contenido; hoy el unico grupo es "Guardadas", alimentado por los
 * Favoritos (`MainShell`). El grupo de recetas abiertas hace poco no existe porque ninguna feature
 * registra `lastOpenedAt`; cuando exista, volvera aqui con su propia lista.
 *
 * [isEmpty] decide el estado vacio de usuario nuevo ("Cuando guardes una receta aparecera aqui.").
 *
 * [filteredBy] aplica el filtro local ([LocalResultFilters.visible]) al grupo `saved`, sin Compose ni
 * red; es la operacion pura que consume "Tus recetas".
 */
data class HomeRecipes(
    val saved: List<Recipe> = emptyList(),
) {
    val isEmpty: Boolean get() = saved.isEmpty()

    /** Devuelve una copia con el grupo acotado por [filter] (mismo orden interno). */
    fun filteredBy(filter: LocalResultFilter, taxonomy: Taxonomy): HomeRecipes = copy(
        saved = LocalResultFilters.visible(saved, filter, taxonomy),
    )
}
