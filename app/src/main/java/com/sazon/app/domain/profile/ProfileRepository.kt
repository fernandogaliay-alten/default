package com.sazon.app.domain.profile

import kotlinx.coroutines.flow.Flow

/**
 * Puerto de persistencia del Perfil de usuario.
 *
 * El onboarding y la pantalla Perfil dependen de esta abstraccion, no de Room. La ausencia de
 * perfil se representa con `null`; no se inventa un perfil por defecto.
 */
interface ProfileRepository {

    suspend fun get(): UserProfile?

    fun observe(): Flow<UserProfile?>

    suspend fun save(profile: UserProfile)

    suspend fun clear()
}
