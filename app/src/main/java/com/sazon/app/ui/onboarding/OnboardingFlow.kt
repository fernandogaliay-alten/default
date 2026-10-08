package com.sazon.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Huesped de los pasos del Onboarding.
 *
 * Mantiene el borrador **en memoria** (`rememberSaveable`): el paso 1 guarda nombre y color, los
 * pasos 2-7 son marcadores hasta `onboarding-diet-style-cuisines` y
 * `onboarding-ingredients-safety-kitchen`, y el guardado/cierre del Perfil (costura [onFinished])
 * llega en la ultima feature del onboarding. No se persiste un Perfil inventado.
 */
@Composable
fun OnboardingFlow(
    taxonomy: Taxonomy,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatarColorId by rememberSaveable {
        mutableStateOf(taxonomy.avatarColors.firstOrNull()?.id.orEmpty())
    }

    val progress = OnboardingProgress(OnboardingStep.TOTAL_STEPS, stepIndex)
    val identity = OnboardingIdentityState(name = name, avatarColorId = avatarColorId)
    val primaryEnabled = progress.step != OnboardingStep.IDENTITY || identity.canProceed

    OnboardingScaffold(
        progress = progress,
        canGoBack = progress.canGoBack,
        onBack = { stepIndex = progress.back().stepIndex },
        primaryLabel = progress.step.nextLabel,
        primaryEnabled = primaryEnabled,
        onPrimary = {
            if (progress.isLastStep) {
                onFinished()
            } else {
                stepIndex = progress.next().stepIndex
            }
        },
        content = {
            when (progress.step) {
                OnboardingStep.IDENTITY -> OnboardingIdentityScreen(
                    identity = identity,
                    taxonomy = taxonomy,
                    onNameChange = { name = it },
                    onColorChange = { avatarColorId = it },
                    modifier = modifier,
                )

                else -> OnboardingPlaceholderStep(step = progress.step)
            }
        },
    )
}

/**
 * Marcador de posicion de los pasos 2-7. Existe para ejercitar el armazon mientras su contenido real
 * llega; su primario avanza el paso igual que cualquier otro.
 */
@Composable
private fun OnboardingPlaceholderStep(step: OnboardingStep) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(SazonSpacing.lg))
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        Text(
            text = "Este paso llega en una próxima versión.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
