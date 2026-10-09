package com.sazon.app.data.favorites

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla **multi-fila** de Favoritos: una fila por receta guardada.
 *
 * La receta se guarda serializada como JSON en `recipeJson` (copia completa); `recipeId` es la clave
 * primaria (coincide con `recipe.id`) y `savedAt` alimenta el orden por defecto.
 */
@Entity(tableName = "favorite_recipe")
data class FavoriteEntity(
    @PrimaryKey val recipeId: String,
    val savedAt: Long,
    val recipeJson: String,
)
