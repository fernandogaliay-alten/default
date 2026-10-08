package com.sazon.app.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.SoftPreference
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable de preferencias de estilo (multi-select opcional), sin estado.
 *
 * Ofrece un chip "Ninguno" seleccionado cuando no hay ninguna preferencia marcada; pulsarlo invoca
 * [onClear]. "Ninguno" y una preferencia concreta nunca estan seleccionados a la vez porque "Ninguno"
 * se deriva de la lista vacia. No conoce el onboarding ni el Perfil.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoftPreferencesEditor(
    options: List<SoftPreference>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Opcional. Puedes cambiarlo cuando quieras.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            PreferenceChip(
                label = "Ninguno",
                selected = selectedIds.isEmpty(),
                onClick = onClear,
            )
            options.forEach { option ->
                PreferenceChip(
                    label = option.label,
                    selected = selectedIds.contains(option.id),
                    onClick = { onToggle(option.id) },
                )
            }
        }
    }
}

@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SazonPillShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(
                horizontal = SazonSpacing.md,
                vertical = SazonSpacing.sm,
            ),
        )
    }
}
