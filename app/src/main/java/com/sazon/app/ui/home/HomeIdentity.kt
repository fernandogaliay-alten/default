package com.sazon.app.ui.home

import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.onboarding.AvatarInitials

/**
 * Identidad de la cabecera de la Home (color del avatar + iniciales), pura y testeable en JVM.
 *
 * El `hex` sale de la Taxonomia por `avatarColorId` y, si el id no resuelve (perfil corrupto o
 * migrado), del **primer** `avatarColors`. Las iniciales las calcula [AvatarInitials] (maximo 2,
 * `null` sin nombre), que la UI traduce a un icono generico.
 */
data class HomeIdentity(
    val hex: String,
    val initials: String?,
) {
    companion object {
        fun from(profile: UserProfile, taxonomy: Taxonomy): HomeIdentity {
            val color = taxonomy.avatarColor(profile.avatarColorId) ?: taxonomy.avatarColors.first()
            return HomeIdentity(
                hex = color.hex,
                initials = AvatarInitials.of(profile.displayName),
            )
        }
    }
}
