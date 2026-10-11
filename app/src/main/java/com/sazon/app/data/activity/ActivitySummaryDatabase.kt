package com.sazon.app.data.activity

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos propia del Resumen de actividad (`activity_summary.db`, version 1).
 *
 * Es una base separada de `ProfileDatabase`/`FavoritesDatabase`/`SuggestionBatchDatabase` porque anadir
 * la tabla a una base aceptada obligaria a subir la version y a una migracion no ejercitable en tests
 * JVM; mismo criterio que `favorites.db` y `suggestion_batch.db`. La consolidacion en una unica base
 * queda como deuda futura.
 */
@Database(
    entities = [ActivitySummaryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ActivitySummaryDatabase : RoomDatabase() {

    abstract fun activitySummaryDao(): ActivitySummaryDao

    companion object {
        private const val DATABASE_NAME = "activity_summary.db"

        fun build(context: Context): ActivitySummaryDatabase =
            Room.databaseBuilder(context, ActivitySummaryDatabase::class.java, DATABASE_NAME).build()
    }
}
