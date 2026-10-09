package com.sazon.app.domain.favorites

import com.sazon.app.domain.recipe.Recipe

/**
 * Favorito del Usuario local.
 *
 * Guarda **una copia completa de la [Recipe]** (no solo su id) porque el Lote de sugerencias puede
 * refrescarse y perder la receta `ai` original (`docs/domain-model.md`). El [recipeId] es la clave
 * (coincide con `recipe.id`) y el Favorito es **independiente** del Lote.
 *
 * [savedAt] es el instante en que se guardo (reloj inyectable en [FavoriteService]); alimenta el
 * orden por defecto "Anadidas recientemente" y el conteo.
 */
data class Favorite(
    val recipe: Recipe,
    val savedAt: Long,
) {
    val recipeId: String get() = recipe.id
}
