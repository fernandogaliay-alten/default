package com.sazon.app.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.preferences.DifficultyEditor
import com.sazon.app.ui.preferences.HardRestrictionsEditor
import com.sazon.app.ui.preferences.ServingsStepper
import com.sazon.app.ui.preferences.TimeBucketEditor
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Paso 2 del Generador ("Ajusta tu receta"), capa Compose **delgada** sobre [GeneratorSettingsState]:
 * seis tarjetas con tiempo disponible, tipo de comida, comensales, dificultad, alergias a evitar y
 * notas libres. No guarda estado propio: recibe el estado y delega toda la edicion en callbacks; la
 * logica pura vive en [GeneratorSettingsState].
 *
 * Los grupos salen **siempre** de la Taxonomia (la UI nunca diverge de ella) y se **reutilizan** los
 * editores ya aceptados de `ui.preferences` ([TimeBucketEditor], [DifficultyEditor], [ServingsStepper] y
 * [HardRestrictionsEditor], este ultimo pinta las alergias en **ambar** y nunca en rojo). El tipo de
 * comida no tiene editor previo: es una fila local de `FilterChip` single-select opcional (el toque
 * sobre el seleccionado lo deselecciona). "Editar en mi perfil" es un `TextButton` de texto (sin iconos)
 * que navega a la pestana Perfil; el primario anclado "Generar recetas" sigue siendo del huesped.
 */
@Composable
fun GeneratorSettingsScreen(
    state: GeneratorSettingsState,
    taxonomy: Taxonomy,
    onServingsChange: (Int) -> Unit,
    onMealTypeSelect: (String) -> Unit,
    onTimeBucketSelect: (String) -> Unit,
    onDifficultySelect: (String) -> Unit,
    onToggleRestriction: (String) -> Unit,
    onClearRestrictions: () -> Unit,
    onNotesChange: (String) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
    ) {
        SettingsCard(title = "Tiempo disponible") {
            TimeBucketEditor(
                timeBuckets = taxonomy.timeBuckets,
                selectedId = state.timeBucketId,
                onSelect = onTimeBucketSelect,
            )
        }

        SettingsCard(title = "Tipo de comida") {
            MealTypeEditor(
                taxonomy = taxonomy,
                selectedId = state.mealTypeId,
                onSelect = onMealTypeSelect,
            )
        }

        SettingsCard(title = "Comensales") {
            ServingsStepper(
                servings = taxonomy.servings,
                value = state.servings,
                onChange = onServingsChange,
            )
        }

        SettingsCard(title = "Dificultad") {
            DifficultyEditor(
                difficulties = taxonomy.difficulties,
                selectedId = state.difficultyId,
                onSelect = onDifficultySelect,
            )
        }

        SettingsCard(title = "Alergias a evitar") {
            HardRestrictionsEditor(
                restrictions = taxonomy.hardRestrictions,
                selectedIds = state.hardRestrictionIds,
                onToggle = onToggleRestriction,
                onClear = onClearRestrictions,
            )
            if (state.hasNoRestrictions) {
                Spacer(modifier = Modifier.height(SazonSpacing.sm))
                Text(
                    text = "Ninguna declarada",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onEditProfile) {
                Text(text = "Editar en mi perfil")
            }
        }

        SettingsCard(title = "¿Algo más que deba saber?") {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                minLines = 3,
                singleLine = false,
                placeholder = {
                    Text(text = "Ej.: sin horno, aprovechar las sobras, en 10 minutos…")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Tarjeta blanca del paso: titulo `titleMedium` y contenido debajo, con esquina grande. */
@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = SazonShapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SazonSpacing.sm)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(SazonSpacing.sm))
            content()
        }
    }
}

/**
 * Fila local de chips del tipo de comida (no hay editor previo): un `FilterChip` por `taxonomy.mealTypes`
 * con el mismo tratamiento visual que el resto (`SazonPillShape`, seleccionado `primary`/`onPrimary`).
 * Single-select **opcional**: el toque sobre el seleccionado delega en el callback, que lo deja en `null`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealTypeEditor(
    taxonomy: Taxonomy,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        taxonomy.mealTypes.forEach { mealType ->
            FilterChip(
                selected = mealType.id == selectedId,
                onClick = { onSelect(mealType.id) },
                label = { Text(text = mealType.label) },
                shape = SazonPillShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}
