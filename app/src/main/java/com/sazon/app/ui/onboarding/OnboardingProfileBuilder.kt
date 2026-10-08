package com.sazon.app.ui.onboarding

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.ProfileName
import com.sazon.app.domain.profile.UserProfile

/**
 * Constructor **puro** del [UserProfile] a partir de la identidad (paso 1) y el borrador (pasos 2-7).
 *
 * Es la unica traduccion borrador -> Perfil: normaliza el nombre (mismo contrato que
 * `RoomProfileRepository.save`), inyecta el instante del cierre (`now`, sin reloj oculto) y copia
 * **todos** los campos del borrador a [PreferenceProfile]. No importa Compose ni Android, asi que el
 * cierre se testea en JVM.
 *
 * Rechaza un nombre invalido con `IllegalArgumentException` (via `require`), de modo que no se
 * construye ni se persiste un Perfil invalido.
 */
object OnboardingProfileBuilder {

    fun build(
        displayName: String,
        avatarColorId: String,
        draft: OnboardingPreferencesDraft,
        now: Long,
    ): UserProfile {
        require(ProfileName.isValid(displayName)) {
            "El nombre del perfil debe tener entre ${ProfileName.MIN_LENGTH} y " +
                "${ProfileName.MAX_LENGTH} caracteres tras recortar"
        }
        return UserProfile(
            displayName = ProfileName.normalize(displayName),
            avatarColorId = avatarColorId,
            createdAt = now,
            preferences = PreferenceProfile(
                dietPatternId = draft.dietPatternId,
                hardRestrictionIds = draft.hardRestrictionIds,
                softPreferenceIds = draft.softPreferenceIds,
                cuisineIds = draft.cuisineIds,
                preferredIngredientIds = draft.preferredIngredientIds,
                dislikedIngredientIds = draft.dislikedIngredientIds,
                difficultyId = draft.difficultyId,
                timeBucketId = draft.timeBucketId,
                defaultServings = draft.defaultServings,
                updatedAt = now,
            ),
        )
    }
}
