package com.sazon.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.onboarding.OnboardingPreferencesDraft
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
 * Grupo de preferencias editable desde el Perfil. Cada valor abre su hoja inferior con el **mismo**
 * composable sin estado que usa el onboarding (`ui.preferences`), materializando la regla "un editor,
 * dos entradas". El [title] se usa como cabecera de la hoja.
 */
enum class ProfileSection(val title: String) {
    DIET("Alimentación"),
    STYLE("Estilo"),
    CUISINES("Cocinas favoritas"),
    INGREDIENTS("Ingredientes"),
    RESTRICTIONS("Alergias e intolerancias"),
    DIFFICULTY("Nivel de cocina"),
    TIME_BUCKET("Tiempo disponible"),
    SERVINGS("Comensales habituales"),
}

/**
 * Hoja inferior que envuelve el editor compartido de una [ProfileSection] sobre el Perfil.
 *
 * No crea editores nuevos: reutiliza los de `ui.preferences` con los valores del borrador actual y
 * avisa por [onDraftChange]. La persistencia la decide el huesped ([ProfileScreen]) al cerrar la hoja
 * ("Listo" o descarte): aqui solo se cierra con [onDismiss]. La `query` del buscador de ingredientes
 * se conserva con `rememberSaveable`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorSheet(
    section: ProfileSection,
    state: ProfileEditState,
    taxonomy: Taxonomy,
    onDraftChange: (OnboardingPreferencesDraft) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.preferencesDraft
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SazonSpacing.lg)
                .padding(bottom = SazonSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            when (section) {
                ProfileSection.DIET -> DietPatternEditor(
                    patterns = taxonomy.dietPatterns,
                    selectedId = draft.dietPatternId,
                    onSelect = { onDraftChange(draft.selectDietPattern(it)) },
                )

                ProfileSection.STYLE -> SoftPreferencesEditor(
                    options = taxonomy.softPreferences,
                    selectedIds = draft.softPreferenceIds,
                    onToggle = { onDraftChange(draft.toggleSoftPreference(it)) },
                    onClear = { onDraftChange(draft.clearSoftPreferences()) },
                )

                ProfileSection.CUISINES -> CuisinesEditor(
                    cuisines = taxonomy.cuisines,
                    selectedIds = draft.cuisineIds,
                    onToggle = { onDraftChange(draft.toggleCuisine(it)) },
                )

                ProfileSection.INGREDIENTS -> {
                    var query by rememberSaveable { mutableStateOf("") }
                    IngredientsEditor(
                        ingredients = taxonomy.ingredients,
                        preferredIds = draft.preferredIngredientIds,
                        dislikedIds = draft.dislikedIngredientIds,
                        query = query,
                        onQueryChange = { query = it },
                        onTogglePreferred = { onDraftChange(draft.togglePreferredIngredient(it)) },
                        onToggleDisliked = { onDraftChange(draft.toggleDislikedIngredient(it)) },
                    )
                }

                ProfileSection.RESTRICTIONS -> HardRestrictionsEditor(
                    restrictions = taxonomy.hardRestrictions,
                    selectedIds = draft.hardRestrictionIds,
                    onToggle = { onDraftChange(draft.toggleHardRestriction(it)) },
                    onClear = { onDraftChange(draft.clearHardRestrictions()) },
                )

                ProfileSection.DIFFICULTY -> DifficultyEditor(
                    difficulties = taxonomy.difficulties,
                    selectedId = draft.difficultyId,
                    onSelect = { onDraftChange(draft.selectDifficulty(it)) },
                )

                ProfileSection.TIME_BUCKET -> TimeBucketEditor(
                    timeBuckets = taxonomy.timeBuckets,
                    selectedId = draft.timeBucketId,
                    onSelect = { onDraftChange(draft.selectTimeBucket(it)) },
                )

                ProfileSection.SERVINGS -> ServingsStepper(
                    servings = taxonomy.servings,
                    value = draft.defaultServings,
                    onChange = { onDraftChange(draft.setServings(it, taxonomy.servings)) },
                )
            }

            Spacer(modifier = Modifier.height(SazonSpacing.sm))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Listo")
            }
        }
    }
}
