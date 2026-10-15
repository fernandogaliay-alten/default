package com.sazon.app.data.cooking

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla de **una sola fila** que espeja la sesion del Modo cocina (`cooking_session.db`, version 1;
 * feature `cooking-timer-foreground-service`).
 *
 * La receta viaja serializada como JSON en `recipeJson` (misma convencion que el Lote de sugerencias,
 * `RecipeJson`) y el marcado de pasos como CSV de `RecipeStep.order` (mismo criterio que el `Saver`
 * aceptado de `CookingModeState`). La fila ausente representa "sin sesion".
 */
@Entity(tableName = "cooking_session")
data class CookingSessionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val recipeJson: String,
    val stepIndex: Int,
    val completedOrders: String,
    val timerStatus: String,
    val totalMillis: Long,
    val remainingMillis: Long,
    val endAtElapsedMillis: Long?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
