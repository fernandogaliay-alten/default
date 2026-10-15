package com.sazon.app.data.cooking

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos **propia** de la sesion del Modo cocina (`cooking_session.db`, version 1).
 *
 * Es una base separada de las aceptadas (`ProfileDatabase`, `FavoritesDatabase`,
 * `SuggestionBatchDatabase`, `ActivitySummaryDatabase`) para no obligar a migrar ninguna de ellas
 * (patron de `SuggestionBatchDatabase`). La consolidacion en una unica base queda como deuda futura.
 */
@Database(
    entities = [CookingSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CookingSessionDatabase : RoomDatabase() {

    abstract fun cookingSessionDao(): CookingSessionDao

    companion object {
        private const val DATABASE_NAME = "cooking_session.db"

        fun build(context: Context): CookingSessionDatabase =
            Room.databaseBuilder(context, CookingSessionDatabase::class.java, DATABASE_NAME).build()
    }
}
