package com.sazon.app.data.favorites

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos propia de Favoritos (`favorites.db`, version 1).
 *
 * Es una base separada de `ProfileDatabase` porque anadir la tabla obligaria a subir la version y a
 * una migracion **no ejercitable en tests JVM**; mismo criterio que `SuggestionBatchDatabase` en
 * `suggestion-batch-cache`. La consolidacion en una unica base de la app queda como deuda futura.
 */
@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FavoritesDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        private const val DATABASE_NAME = "favorites.db"

        fun build(context: Context): FavoritesDatabase =
            Room.databaseBuilder(context, FavoritesDatabase::class.java, DATABASE_NAME).build()
    }
}
