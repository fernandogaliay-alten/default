package com.sazon.app.data.activity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO de la base propia del Resumen de actividad.
 *
 * Una sola fila (`id = 1`). [increment] garantiza de forma **atomica** que la fila existe (se inserta
 * con contador `0` si falta) y suma [delta], de modo que el primer incremento no se pierde. Es una
 * `abstract class` (no una interface) para poder escribir el cuerpo en Kotlin sin depender del modo
 * `jvm-default`; Room genera la envoltura `@Transaction`.
 */
@Dao
abstract class ActivitySummaryDao {

    @Query("SELECT recipesGeneratedCount FROM activity_summary WHERE id = 1")
    abstract fun observeCount(): Flow<Int?>

    @Query("SELECT recipesGeneratedCount FROM activity_summary WHERE id = 1")
    abstract suspend fun getCount(): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(entity: ActivitySummaryEntity)

    @Query("UPDATE activity_summary SET recipesGeneratedCount = recipesGeneratedCount + :delta WHERE id = 1")
    abstract suspend fun add(delta: Int)

    @Transaction
    open suspend fun increment(delta: Int) {
        insertIfAbsent(ActivitySummaryEntity())
        add(delta)
    }
}
