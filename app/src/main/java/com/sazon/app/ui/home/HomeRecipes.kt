package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe

/**
 * Costura de datos del bloque "Tus recetas" (bloque 5 de la Home): las recetas guardadas y las
 * abiertas hace poco.
 *
 * En esta feature ambas listas llegan vacias desde `MainShell` (Favoritos y "Abiertas hace poco" no
 * existen todavia); `favorites`/`recipe-detail-screen` las alimentaran. [isEmpty] decide el estado
 * vacio de usuario nuevo ("Cuando guardes una receta aparecera aqui.").
 */
data class HomeRecipes(
    val saved: List<Recipe> = emptyList(),
    val recentlyOpened: List<Recipe> = emptyList(),
) {
    val isEmpty: Boolean get() = saved.isEmpty() && recentlyOpened.isEmpty()
}
