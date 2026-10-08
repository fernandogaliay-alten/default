package com.sazon.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.preferences.CuisinesEditor
import com.sazon.app.ui.preferences.DietPatternEditor
import com.sazon.app.ui.preferences.SoftPreferencesEditor
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Huesped de los pasos del Onboarding.
 *
 * Mantiene el borrador **en memoria** (`rememberSaveable`): el paso 1 guarda nombre y color, los
 * pasos 2-4 (dieta, estilo y cocinas) editan el borrador [OnboardingPreferencesDraft], los pasos 5-7
 * siguen como marcadores hasta `onboarding-ingredients-safety-kitchen`, y el guardado/cierre del
 * Perfil (costura [onFinished]) llega en la ultima feature del onboarding. No se persiste un Perfil
 * inventado.
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
    var draft by rememberSaveable(stateSaver = OnboardingPreferencesDraftSaver) {
        mutableStateOf(OnboardingPreferencesDraft.initial(taxonomy))
    }

    val progress = OnboardingProgress(OnboardingStep.TOTAL_STEPS, stepIndex)
    val identity = OnboardingIdentityState(name = name, avatarColorId = avatarColorId)
    // Solo el paso 1 puede bloquear el avance: la dieta tiene valor por defecto y estilo/cocinas son
    // opcionales, asi que los pasos 2-4 siempre permiten continuar.
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

                OnboardingStep.DIET -> PreferenceStep(step = progress.step) {
                    DietPatternEditor(
                        patterns = taxonomy.dietPatterns,
                        selectedId = draft.dietPatternId,
                        onSelect = { draft = draft.selectDietPattern(it) },
                        modifier = modifier,
                    )
                }

                OnboardingStep.STYLE -> PreferenceStep(step = progress.step) {
                    SoftPreferencesEditor(
                        options = taxonomy.softPreferences,
                        selectedIds = draft.softPreferenceIds,
                        onToggle = { draft = draft.toggleSoftPreference(it) },
                        onClear = { draft = draft.clearSoftPreferences() },
                        modifier = modifier,
                    )
                }

                OnboardingStep.CUISINES -> PreferenceStep(step = progress.step) {
                    CuisinesEditor(
                        cuisines = taxonomy.cuisines,
                        selectedIds = draft.cuisineIds,
                        onToggle = { draft = draft.toggleCuisine(it) },
                        modifier = modifier,
                    )
                }

                else -> OnboardingPlaceholderStep(step = progress.step)
            }
        },
    )
}

/**
 * Envoltorio del enunciado de un paso de preferencias: la pregunta vive aqui (no en los editores)
 * para que los editores sigan siendo reutilizables por el Perfil sin conocer el onboarding.
 */
@Composable
private fun PreferenceStep(
    step: OnboardingStep,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SazonSpacing.lg),
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.lg))
        content()
    }
}

/**
 * Marcador de posicion de los pasos 5-7. Existe para ejercitar el armazon mientras su contenido real
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

/**
 * Saver de [OnboardingPreferencesDraft] para `rememberSaveable`, en la capa Compose: mantiene la
 * logica pura libre de Compose. Serializa el borrador como `[dietPatternId, softCount, ...soft, ...cuisines]`.
 */
private val OnboardingPreferencesDraftSaver: Saver<OnboardingPreferencesDraft, Any> = listSaver(
    save = { draft ->
        listOf(draft.dietPatternId, draft.softPreferenceIds.size.toString()) +
            draft.softPreferenceIds +
            draft.cuisineIds
    },
    restore = { saved ->
        val dietPatternId = saved[0]
        val softCount = saved[1].toInt()
        OnboardingPreferencesDraft(
            dietPatternId = dietPatternId,
            softPreferenceIds = saved.subList(2, 2 + softCount).toList(),
            cuisineIds = saved.drop(2 + softCount),
        )
    },
)
