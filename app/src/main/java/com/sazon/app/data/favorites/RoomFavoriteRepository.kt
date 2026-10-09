package com.sazon.app.data.favorites

import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.favorites.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementacion de [FavoriteRepository] sobre Room.
 *
 * `observe` delega en el DAO (que ya ordena por `savedAt` desc) y `isFavorite` pregunta por una fila
 * concreta; no mantiene estado propio.
 */
class RoomFavoriteRepository(
    private val dao: FavoriteDao,
) : FavoriteRepository {

    override suspend fun get(): List<Favorite> = dao.getAll().map { it.toDomain() }

    override fun observe(): Flow<List<Favorite>> =
        dao.observe().map { entities -> entities.map { it.toDomain() } }

    override suspend fun isFavorite(recipeId: String): Boolean = dao.getById(recipeId) != null

    override suspend fun save(favorite: Favorite) = dao.upsert(favorite.toEntity())

    override suspend fun remove(recipeId: String) = dao.delete(recipeId)

    override suspend fun clear() = dao.deleteAll()
}
