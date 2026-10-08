package com.sazon.app.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.sazon.app.domain.taxonomy.DietPattern
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable del patron de dieta (single-select), sin estado.
 *
 * Materializa la regla "un editor, dos entradas": recibe las opciones de la Taxonomia y el id
 * seleccionado, y avisa por callback. No conoce el onboarding ni el Perfil, y no renderiza iconos
 * (el repositorio no tiene assets ni dependencia de iconos).
 */
@Composable
fun DietPatternEditor(
    patterns: List<DietPattern>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        patterns.forEach { pattern ->
            DietPatternCard(
                pattern = pattern,
                selected = pattern.id == selectedId,
                onClick = { onSelect(pattern.id) },
            )
        }
    }
}

@Composable
private fun DietPatternCard(
    pattern: DietPattern,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SazonShapes.large,
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
        Column(modifier = Modifier.padding(SazonSpacing.md)) {
            Text(
                text = pattern.label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(SazonSpacing.sm))
            Text(
                text = pattern.description,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
