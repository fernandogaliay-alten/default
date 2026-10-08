package com.sazon.app.ui

import com.sazon.app.domain.profile.UserProfile

/** Destino de nivel superior de la app. */
enum class AppDestination { ONBOARDING, MAIN }

/**
 * Puerta de primer arranque, pura y testeable en JVM.
 *
 * La senal es la **presencia de Perfil** en Room, no un flag en DataStore: el Perfil es el artefacto
 * que produce el onboarding y `ProfileRepository.observe()` ya es la fuente aceptada. Sin Perfil se
 * muestra el onboarding (obligatorio y no saltable); con Perfil, el destino principal.
 */
object AppGate {

    fun decide(profile: UserProfile?): AppDestination =
        if (profile == null) AppDestination.ONBOARDING else AppDestination.MAIN
}
