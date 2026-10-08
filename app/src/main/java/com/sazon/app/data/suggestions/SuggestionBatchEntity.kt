package com.sazon.app.data.suggestions

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla de una sola fila que espeja el Lote de sugerencias.
 *
 * Las recetas se guardan serializadas como JSON en `recipesJson`; la fila ausente representa el
 * lote inexistente.
 */
@Entity(tableName = "suggestion_batch")
data class SuggestionBatchEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val preferenceSignature: String,
    val generatedAt: Long,
    val recipesJson: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
