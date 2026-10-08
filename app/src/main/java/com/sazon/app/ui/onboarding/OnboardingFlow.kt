package com.sazon.app.ui.onboarding

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.preferences.CuisinesEditor
import com.sazon.app.ui.preferences.DietPatternEditor
import com.sazon.app.ui.preferences.DifficultyEditor
import com.sazon.app.ui.preferences.HardRestrictionsEditor
import com.sazon.app.ui.preferences.IngredientsEditor
import com.sazon.app.ui.preferences.ServingsStepper
import com.sazon.app.ui.preferences.SoftPreferencesEditor
import com.sazon.app.ui.preferences.TimeBucketEditor
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Huesped de los pasos del Onboarding.
 *
 * Mantiene el borrador **en memoria** (`rememberSaveable`): el paso 1 guarda nombre y color, los
 * pasos 2-7 editan el borrador [OnboardingPreferencesDraft] (dieta, estilo, cocinas, ingredientes,
 * restricciones duras y cocina) y, al pulsar el primario del ultimo paso, [OnboardingProfileBuilder]
 * construye el [UserProfile] y se lo entrega a [onFinished]. El flujo **no** conoce el repositorio ni
 * las coroutines: `SazonApp` persiste el Perfil y la puerta `AppGate` pasa a `MAIN` por la observacion.
 */
@Composable
fun OnboardingFlow(
    taxonomy: Taxonomy,
    onFinished: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatarColorId by rememberSaveable {
        mutableStateOf(taxonomy.avatarColors.firstOrNull()?.id.orEmpty())
    }
    var ingredientQuery by rememberSaveable { mutableStateOf("") }
    var draft by rememberSaveable(stateSaver = OnboardingPreferencesDraftSaver) {
        mutableStateOf(OnboardingPreferencesDraft.initial(taxonomy))
    }

    val progress = OnboardingProgress(OnboardingStep.TOTAL_STEPS, stepIndex)
    val identity = OnboardingIdentityState(name = name, avatarColorId = avatarColorId)
    // El unico requisito de avance es el nombre del paso 1: los pasos 2-7 tienen valores por defecto
    // (o son opcionales) y se mantiene la regla de identidad para todo el flujo, de modo que un nombre
    // invalido deja el primario deshabilitado tambien en el ultimo paso.
    val primaryEnabled = identity.canProceed

    OnboardingScaffold(
        progress = progress,
        canGoBack = progress.canGoBack,
        onBack = { stepIndex = progress.back().stepIndex },
        primaryLabel = progress.step.nextLabel,
        primaryEnabled = primaryEnabled,
        onPrimary = {
            if (progress.isLastStep) {
                onFinished(
                    OnboardingProfileBuilder.build(
                        displayName = name,
                        avatarColorId = avatarColorId,
                        draft = draft,
                        now = System.currentTimeMillis(),
                    ),
                )
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

                OnboardingStep.INGREDIENTS -> PreferenceStep(step = progress.step) {
                    IngredientsEditor(
                        ingredients = taxonomy.ingredients,
                        preferredIds = draft.preferredIngredientIds,
                        dislikedIds = draft.dislikedIngredientIds,
                        query = ingredientQuery,
                        onQueryChange = { ingredientQuery = it },
                        onTogglePreferred = { draft = draft.togglePreferredIngredient(it) },
                        onToggleDisliked = { draft = draft.toggleDislikedIngredient(it) },
                        modifier = modifier,
                    )
                }

                OnboardingStep.RESTRICTIONS -> PreferenceStep(step = progress.step) {
                    HardRestrictionsEditor(
                        restrictions = taxonomy.hardRestrictions,
                        selectedIds = draft.hardRestrictionIds,
                        onToggle = { draft = draft.toggleHardRestriction(it) },
                        onClear = { draft = draft.clearHardRestrictions() },
                        modifier = modifier,
                    )
                }

                OnboardingStep.KITCHEN -> PreferenceStep(step = progress.step) {
                    LabeledGroup(title = "Nivel de cocina") {
                        DifficultyEditor(
                            difficulties = taxonomy.difficulties,
                            selectedId = draft.difficultyId,
                            onSelect = { draft = draft.selectDifficulty(it) },
                            modifier = modifier,
                        )
                    }
                    Spacer(modifier = Modifier.height(SazonSpacing.lg))
                    LabeledGroup(title = "¿Cuánto tiempo sueles tener?") {
                        TimeBucketEditor(
                            timeBuckets = taxonomy.timeBuckets,
                            selectedId = draft.timeBucketId,
                            onSelect = { draft = draft.selectTimeBucket(it) },
                            modifier = modifier,
                        )
                    }
                    Spacer(modifier = Modifier.height(SazonSpacing.lg))
                    LabeledGroup(title = "¿Para cuántos cocinas normalmente?") {
                        ServingsStepper(
                            servings = taxonomy.servings,
                            value = draft.defaultServings,
                            onChange = { draft = draft.setServings(it, taxonomy.servings) },
                            modifier = modifier,
                        )
                    }
                }
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

/** Subtitulo de un grupo dentro de un paso (p. ej. las tres colecciones del paso 7). */
@Composable
private fun LabeledGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        content()
    }
}

/**
 * Saver de [OnboardingPreferencesDraft] para `rememberSaveable`, en la capa Compose: mantiene la
 * logica pura libre de Compose. Cada lista se serializa como `[conteo, ...elementos]` y los tres
 * campos de cocina cierran la secuencia.
 */
private val OnboardingPreferencesDraftSaver: Saver<OnboardingPreferencesDraft, Any> = listSaver(
    save = { draft ->
        buildList {
            add(draft.dietPatternId)
            addAll(draft.softPreferenceIds.withSizePrefix())
            addAll(draft.cuisineIds.withSizePrefix())
            addAll(draft.preferredIngredientIds.withSizePrefix())
            addAll(draft.dislikedIngredientIds.withSizePrefix())
            addAll(draft.hardRestrictionIds.withSizePrefix())
            add(draft.difficultyId)
            add(draft.timeBucketId)
            add(draft.defaultServings.toString())
        }
    },
    restore = { saved ->
        var index = 0
        fun nextString(): String = saved[index++]
        fun nextList(): List<String> {
            val count = nextString().toInt()
            val list = saved.subList(index, index + count).map { it as String }
            index += count
            return list
        }
        OnboardingPreferencesDraft(
            dietPatternId = nextString(),
            softPreferenceIds = nextList(),
            cuisineIds = nextList(),
            preferredIngredientIds = nextList(),
            dislikedIngredientIds = nextList(),
            hardRestrictionIds = nextList(),
            difficultyId = nextString(),
            timeBucketId = nextString(),
            defaultServings = nextString().toInt(),
        )
    },
)

private fun List<String>.withSizePrefix(): List<String> = listOf(size.toString()) + this
