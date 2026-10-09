package com.sazon.app.domain.favorites

import com.sazon.app.domain.recipe.Recipe

/**
 * Servicio de dominio que **marca o desmarca** una receta como Favorito.
 *
 * Es la unica operacion de marcado: la usan el corazon/"Guardar" del Detalle y el corazon de la
 * tarjeta de la lista de Favoritos, evitando duplicar la logica. Puro (sin Android ni Room): depende
 * del puerto [FavoriteRepository] y de un reloj inyectable [now] (determinista en test).
 *
 * Al guardar persiste **una copia** de la [Recipe] con `matchScore` normalizado a `null`: es un valor
 * **derivado y no persistido** (`docs/domain-model.md`). Marcar/desmarcar **no** toca el Lote de
 * sugerencias.
 */
class FavoriteService(
    private val repository: FavoriteRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /**
     * Alterna el estado de Favorito de [recipe]: si ya es favorita la elimina y devuelve `false`; si
     * no lo era, la guarda (copia con `savedAt = now()`) y devuelve `true`.
     */
    suspend fun toggle(recipe: Recipe): Boolean {
        return if (repository.isFavorite(recipe.id)) {
            repository.remove(recipe.id)
            false
        } else {
            repository.save(
                Favorite(
                    recipe = recipe.copy(matchScore = null),
                    savedAt = now(),
                ),
            )
            true
        }
    }

    suspend fun isFavorite(recipeId: String): Boolean = repository.isFavorite(recipeId)
}
