package com.sazon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sazon.app.data.AssetTaxonomySource
import com.sazon.app.domain.profile.ProfileRepository
import com.sazon.app.ui.onboarding.OnboardingFlow
import kotlinx.coroutines.flow.map

/**
 * Raiz de la app y puerta de primer arranque.
 *
 * Observa el Perfil ([ProfileRepository.observe]) y decide con [AppGate]: sin Perfil muestra el
 * Onboarding obligatorio; con Perfil, el destino principal. No se introduce una libreria de
 * navegacion ni inyeccion de dependencias: el cambio de destino es este estado minimo. Mientras el
 * Perfil aun no se ha resuelto (`null`) se pinta solo el fondo del tema.
 *
 * La costura `onFinished` no tiene efecto en esta feature: el guardado y cierre del onboarding
 * llegan en `onboarding-ingredients-safety-kitchen`, de modo que la puerta sigue mostrando el
 * onboarding hasta entonces.
 */
@Composable
fun SazonApp(
    profileRepository: ProfileRepository,
    taxonomySource: AssetTaxonomySource,
    modifier: Modifier = Modifier,
) {
    val destination by produceState<AppDestination?>(initialValue = null, profileRepository) {
        profileRepository.observe()
            .map { AppGate.decide(it) }
            .collect { value = it }
    }

    when (destination) {
        null -> Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        AppDestination.ONBOARDING -> {
            val taxonomy = remember { taxonomySource.load() }
            OnboardingFlow(
                taxonomy = taxonomy,
                onFinished = { /* Sin efecto: el cierre del onboarding es de una feature posterior. */ },
                modifier = modifier,
            )
        }

        AppDestination.MAIN -> HomePlaceholder(modifier = modifier)
    }
}
