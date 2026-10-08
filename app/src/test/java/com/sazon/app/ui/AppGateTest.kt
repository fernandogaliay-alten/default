package com.sazon.app.ui

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Escenarios 1 y 5 de `docs/specs/onboarding-shell-and-identity.md`: la puerta de primer arranque es
 * la presencia de Perfil. Logica pura, sin emulador.
 */
class AppGateTest {

    @Test
    fun sin_perfil_el_destino_es_el_onboarding() {
        assertEquals(AppDestination.ONBOARDING, AppGate.decide(null))
    }

    @Test
    fun con_perfil_el_destino_es_la_app_principal() {
        assertEquals(AppDestination.MAIN, AppGate.decide(perfil()))
    }
}

private fun perfil() = UserProfile(
    displayName = "Ana",
    avatarColorId = "terracotta",
    createdAt = 1_700_000_000_000L,
    preferences = PreferenceProfile(
        dietPatternId = "omnivore",
        hardRestrictionIds = emptyList(),
        softPreferenceIds = emptyList(),
        cuisineIds = emptyList(),
        preferredIngredientIds = emptyList(),
        dislikedIngredientIds = emptyList(),
        difficultyId = "easy",
        timeBucketId = "under_30",
        defaultServings = 2,
        updatedAt = 1_700_000_100_000L,
    ),
)
