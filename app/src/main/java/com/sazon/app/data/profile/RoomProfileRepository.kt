package com.sazon.app.data.profile

import com.sazon.app.domain.profile.ProfileName
import com.sazon.app.domain.profile.ProfileRepository
import com.sazon.app.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementacion de [ProfileRepository] sobre Room.
 *
 * `save` normaliza el nombre con [ProfileName] y rechaza un nombre invalido con
 * `IllegalArgumentException` **antes** de tocar la base de datos.
 */
class RoomProfileRepository(
    private val dao: ProfileDao,
) : ProfileRepository {

    override suspend fun get(): UserProfile? = dao.get()?.toDomain()

    override fun observe(): Flow<UserProfile?> = dao.observe().map { it?.toDomain() }

    override suspend fun save(profile: UserProfile) {
        val normalized = ProfileName.normalize(profile.displayName)
        require(ProfileName.isValid(normalized)) {
            "El nombre del perfil debe tener entre ${ProfileName.MIN_LENGTH} y " +
                "${ProfileName.MAX_LENGTH} caracteres tras recortar"
        }
        dao.upsert(profile.copy(displayName = normalized).toEntity())
    }

    override suspend fun clear() = dao.deleteAll()
}
