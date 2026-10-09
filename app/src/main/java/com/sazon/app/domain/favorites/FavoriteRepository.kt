package com.sazon.app.domain.favorites

import kotlinx.coroutines.flow.Flow

/**
 * Puerto de persistencia de Favoritos.
 *
 * El dominio depende de esta abstraccion; la implementacion sobre Room vive en
 * `com.sazon.app.data.favorites`. Es una tabla **multi-fila** (una por Favorito), no un singleton.
 */
interface FavoriteRepository {

    suspend fun get(): List<Favorite>

    fun observe(): Flow<List<Favorite>>

    suspend fun isFavorite(recipeId: String): Boolean

    suspend fun save(favorite: Favorite)

    suspend fun remove(recipeId: String)

    suspend fun clear()
}
