package com.sazon.app.data.suggestions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionBatchDao {

    @Query("SELECT * FROM suggestion_batch WHERE id = 1")
    suspend fun get(): SuggestionBatchEntity?

    @Query("SELECT * FROM suggestion_batch WHERE id = 1")
    fun observe(): Flow<SuggestionBatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SuggestionBatchEntity)

    @Query("DELETE FROM suggestion_batch")
    suspend fun deleteAll()
}
