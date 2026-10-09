package com.sazon.app.ui.profile

import com.sazon.app.domain.profile.ProfileName
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.onboarding.OnboardingPreferencesDraft

/**
 * Estado de edicion **puro** del Perfil: identidad (nombre + color de avatar) mas el borrador de
 * preferencias ya aceptado del onboarding ([OnboardingPreferencesDraft]).
 *
 * Es logica pura: no importa Compose ni Android, de modo que la traduccion a [UserProfile] se testea
 * en JVM. La capa Compose ([ProfileScreen]) solo pinta y guarda.
 *
 * La traduccion a Perfil ([toProfile]) es la garantia estructural de la regla clave de la feature:
 * editar **solo** la identidad reutiliza `base.preferences` tal cual (mismo `updatedAt`), de modo que
 * la Firma de preferencias no cambia y el Lote de sugerencias no se invalida; editar una preferencia
 * reconstruye `preferences` con `updatedAt = now`, lo que si cambia la firma.
 */
data class ProfileEditState(
    val displayName: String,
    val avatarColorId: String,
    val preferencesDraft: OnboardingPreferencesDraft,
) {

    fun withDisplayName(name: String): ProfileEditState = copy(displayName = name)

    fun withAvatarColorId(id: String): ProfileEditState = copy(avatarColorId = id)

    fun withPreferencesDraft(draft: OnboardingPreferencesDraft): ProfileEditState =
        copy(preferencesDraft = draft)

    /** El nombre es obligatorio: misma regla que `ProfileName` y `ProfileRepository.save`. */
    val isNameValid: Boolean
        get() = ProfileName.isValid(displayName)

    /**
     * Reconstruye el [UserProfile] editado sobre [base].
     *
     * - Exige [isNameValid]: un nombre invalido lanza [IllegalArgumentException] (no se escribe).
     * - Normaliza el nombre (`trim`).
     * - Conserva `createdAt = base.createdAt` (es una edicion, no una reconstruccion).
     * - Conserva `base.preferences` **tal cual** (mismo `updatedAt`) si el borrador no cambio, para
     *   que editar solo la identidad no invalide el Lote; si alguna preferencia cambio, reconstruye
     *   `preferences` con `updatedAt = now`.
     *
     * No valida ids contra la Taxonomia: las hojas solo ofrecen ids validos (coherente con
     * `profile-persistence`). La igualdad de data class del borrador decide si hubo cambio.
     */
    fun toProfile(base: UserProfile, now: Long): UserProfile {
        require(isNameValid) { "El nombre del Perfil no es valido: '$displayName'" }
        val preferences = if (preferencesDraft == OnboardingPreferencesDraft.from(base.preferences)) {
            base.preferences
        } else {
            preferencesDraft.toPreferences(now)
        }
        return UserProfile(
            displayName = ProfileName.normalize(displayName),
            avatarColorId = avatarColorId,
            createdAt = base.createdAt,
            preferences = preferences,
        )
    }

    companion object {
        /**
         * Deriva el estado editable desde un [profile] existente y la Taxonomia: normaliza el nombre,
         * resuelve el color de avatar al `id` de `avatarColors` con **respaldo al primero** si el id
         * viniera corrupto (perfil migrado), y copia las preferencias al borrador.
         */
        fun from(profile: UserProfile, taxonomy: Taxonomy): ProfileEditState =
            ProfileEditState(
                displayName = ProfileName.normalize(profile.displayName),
                avatarColorId = taxonomy.avatarColor(profile.avatarColorId)?.id
                    ?: taxonomy.avatarColors.first().id,
                preferencesDraft = OnboardingPreferencesDraft.from(profile.preferences),
            )
    }
}
