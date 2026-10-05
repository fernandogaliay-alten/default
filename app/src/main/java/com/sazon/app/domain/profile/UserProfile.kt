package com.sazon.app.domain.profile

/**
 * Identidad del Usuario local mas su Perfil de preferencias.
 *
 * Todos los campos son obligatorios: el onboarding es completo y la ausencia de perfil se
 * representa con la fila entera a `null`, nunca con un perfil por defecto.
 */
data class UserProfile(
    val displayName: String,
    val avatarColorId: String,
    val createdAt: Long,
    val preferences: PreferenceProfile,
)

/**
 * Preferencias declaradas por el usuario en el onboarding y editables en el Perfil.
 *
 * Los `*Id` referencian la Taxonomia; esta capa los guarda tal cual, sin validarlos.
 */
data class PreferenceProfile(
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
)
