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
import com.sazon.app.domain.taxonomy.TimeBucket
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable de la franja de tiempo disponible (single-select), sin estado.
 *
 * Recibe las franjas de la Taxonomia y el id seleccionado, y avisa por callback. No conoce el
 * onboarding ni el Perfil y no renderiza iconos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeBucketEditor(
    timeBuckets: List<TimeBucket>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        timeBuckets.forEach { timeBucket ->
            TimeBucketChip(
                label = timeBucket.label,
                selected = timeBucket.id == selectedId,
                onClick = { onSelect(timeBucket.id) },
            )
        }
    }
}

@Composable
private fun TimeBucketChip(
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
