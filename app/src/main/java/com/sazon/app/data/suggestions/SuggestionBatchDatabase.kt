package com.sazon.app.data.suggestions

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos propia del Lote de sugerencias (`suggestion_batch.db`, version 1).
 *
 * Es una base separada de `ProfileDatabase` para no obligar a una migracion de la persistencia del
 * Perfil ya aceptada (ver la spec: base de datos propia). La consolidacion en una unica base de la
 * app queda como deuda futura.
 */
@Database(
    entities = [SuggestionBatchEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SuggestionBatchDatabase : RoomDatabase() {

    abstract fun suggestionBatchDao(): SuggestionBatchDao

    companion object {
        private const val DATABASE_NAME = "suggestion_batch.db"

        fun build(context: Context): SuggestionBatchDatabase =
            Room.databaseBuilder(context, SuggestionBatchDatabase::class.java, DATABASE_NAME).build()
    }
}
