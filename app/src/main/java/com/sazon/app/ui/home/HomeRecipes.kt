package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe

/**
 * Costura de datos del bloque "Tus recetas" (bloque 5 de la Home): las recetas guardadas.
 *
 * El bloque solo pinta grupos con contenido; hoy el unico grupo es "Guardadas", alimentado por los
 * Favoritos (`MainShell`). El grupo de recetas abiertas hace poco no existe porque ninguna feature
 * registra `lastOpenedAt`; cuando exista, volvera aqui con su propia lista.
 *
 * [isEmpty] decide el estado vacio de usuario nuevo ("Cuando guardes una receta aparecera aqui.").
 *
 * [latest] acota el grupo `saved` a las [MAX_VISIBLE] ultimas: es la operacion pura que consume "Tus
 * recetas" (el bloque ya no filtra en local). El orden de entrada ya es "mas reciente primero" (lo
 * aporta `FavoriteRepository`, `savedAt` desc, el mismo de la pestana Favoritos), asi que el tope
 * **no** reordena; sin Compose ni red.
 */
data class HomeRecipes(
    val saved: List<Recipe> = emptyList(),
) {
    val isEmpty: Boolean get() = saved.isEmpty()

    /**
     * Devuelve una copia con el grupo `saved` acotado a las [MAX_VISIBLE] primeras recetas (mismo
     * orden de entrada, sin reordenar) sin mutar el bloque original.
     */
    fun latest(): HomeRecipes = copy(saved = saved.take(MAX_VISIBLE))

    companion object {
        /** Tope de tarjetas del grupo "Guardadas" del bloque "Tus recetas". */
        const val MAX_VISIBLE = 6
    }
}
