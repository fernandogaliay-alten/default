package com.sazon.app.ui.onboarding

import com.sazon.app.domain.profile.ProfileName
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Estado en memoria del paso 1 (identidad). No persiste nada: el borrador del nombre y el color vive
 * en `rememberSaveable` y el guardado del Perfil llega en la ultima feature del onboarding.
 *
 * La validez del nombre y su recorte salen de [ProfileName] (unica fuente de verdad); las iniciales,
 * de [AvatarInitials].
 */
data class OnboardingIdentityState(
    val name: String,
    val avatarColorId: String,
) {
    val normalizedName: String get() = ProfileName.normalize(name)

    val isNameValid: Boolean get() = ProfileName.isValid(name)

    val canProceed: Boolean get() = isNameValid

    val initials: String? get() = AvatarInitials.of(name)

    companion object {
        /**
         * Estado inicial del paso 1: nombre vacio y el **primer** color de `avatarColors` de la
         * Taxonomia (`terracotta`), que actua tambien como respaldo si el color elegido
         * desaparece. La integridad de la Taxonomia garantiza al menos un color.
         */
        fun initial(taxonomy: Taxonomy): OnboardingIdentityState =
            OnboardingIdentityState(
                name = "",
                avatarColorId = taxonomy.avatarColors.first().id,
            )
    }
}
