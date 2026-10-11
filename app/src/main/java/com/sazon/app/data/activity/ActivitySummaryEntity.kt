package com.sazon.app.data.activity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla de una sola fila que guarda el contador persistido de recetas generadas.
 *
 * La fila ausente representa "nunca se genero nada" (0). Es analoga a `user_profile`/`suggestion_batch`
 * en sus bases propias.
 */
@Entity(tableName = "activity_summary")
data class ActivitySummaryEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val recipesGeneratedCount: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
