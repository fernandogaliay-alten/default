package com.sazon.app.data.profile

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ProfileEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(ListStringConverter::class)
abstract class ProfileDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    companion object {
        private const val DATABASE_NAME = "sazon.db"

        fun build(context: Context): ProfileDatabase =
            Room.databaseBuilder(context, ProfileDatabase::class.java, DATABASE_NAME).build()
    }
}
