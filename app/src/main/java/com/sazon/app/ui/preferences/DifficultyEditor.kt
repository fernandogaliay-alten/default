package com.sazon.app.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Difficulty
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable del nivel de cocina (single-select), sin estado.
 *
 * Recibe las dificultades de la Taxonomia (se ordenan por `level` para una lectura ascendente) y el id
 * seleccionado, y avisa por callback. No conoce el onboarding ni el Perfil y no renderiza iconos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DifficultyEditor(
    difficulties: List<Difficulty>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        difficulties.sortedBy { it.level }.forEach { difficulty ->
            SingleChoiceChip(
                label = difficulty.label,
                selected = difficulty.id == selectedId,
                onClick = { onSelect(difficulty.id) },
            )
        }
    }
}

@Composable
private fun SingleChoiceChip(
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
