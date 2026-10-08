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
import com.sazon.app.domain.taxonomy.HardRestriction
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable de restricciones duras del paso 6 (pantalla propia de seguridad), sin estado.
 *
 * Ofrece un banner de tinte **ambar** con el aviso de seguridad y chips multi-select + "Ninguna"
 * (seleccionado cuando la lista esta vacia). Los chips seleccionados usan el rol **tertiary**
 * (`tertiaryContainer`/`tertiary`), nunca el rol de error/rojo: los alergenos son informacion de
 * seguridad, no una alarma. Recibe las restricciones de la Taxonomia y avisa por callback; no conoce
 * el onboarding ni el Perfil.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HardRestrictionsEditor(
    restrictions: List<HardRestriction>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = SazonShapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Lo usamos para que ninguna receta ponga en riesgo tu salud.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(SazonSpacing.md),
            )
        }
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            RestrictionChip(
                label = "Ninguna",
                selected = selectedIds.isEmpty(),
                onClick = onClear,
            )
            restrictions.forEach { restriction ->
                RestrictionChip(
                    label = restriction.label,
                    selected = selectedIds.contains(restriction.id),
                    onClick = { onToggle(restriction.id) },
                )
            }
        }
    }
}

@Composable
private fun RestrictionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SazonPillShape,
        color = if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
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
