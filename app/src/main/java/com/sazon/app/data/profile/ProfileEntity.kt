package com.sazon.app.data.profile

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla de una sola fila que espeja la identidad y las preferencias del Perfil.
 *
 * El perfil ausente es la tabla vacia; nunca se escribe una fila con campos a `null`.
 */
@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val displayName: String,
    val avatarColorId: String,
    val createdAt: Long,
    val dietPatternId: String,
    val hardRestrictionIds: List<String>,
    val softPreferenceIds: List<String>,
    val cuisineIds: List<String>,
    val preferredIngredientIds: List<String>,
    val dislikedIngredientIds: List<String>,
    val difficultyId: String,
    val timeBucketId: String,
    val defaultServings: Int,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
