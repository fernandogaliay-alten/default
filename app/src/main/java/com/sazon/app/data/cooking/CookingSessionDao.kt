package com.sazon.app.data.cooking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Acceso a la fila unica de la sesion del Modo cocina. El `REPLACE` sobre el mismo `id` garantiza que
 * persistir dos veces **reemplaza** y nunca duplica (`CookingSessionEntity.SINGLETON_ID`).
 */
@Dao
interface CookingSessionDao {

    @Query("SELECT * FROM cooking_session WHERE id = 1")
    suspend fun get(): CookingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CookingSessionEntity)

    @Query("DELETE FROM cooking_session")
    suspend fun deleteAll()
}
