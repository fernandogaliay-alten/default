package com.sazon.app.data.favorites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_recipe ORDER BY savedAt DESC")
    fun observe(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorite_recipe ORDER BY savedAt DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT * FROM favorite_recipe WHERE recipeId = :recipeId")
    suspend fun getById(recipeId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorite_recipe WHERE recipeId = :recipeId")
    suspend fun delete(recipeId: String)

    @Query("DELETE FROM favorite_recipe")
    suspend fun deleteAll()
}
