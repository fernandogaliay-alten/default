package com.sazon.app.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Cuisine
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable de cocinas favoritas (multi-select), sin estado.
 *
 * Rejilla de dos columnas de tiles seleccionables; recibe las cocinas de la Taxonomia y los ids
 * marcados, y avisa por callback. No conoce el onboarding ni el Perfil. Los tiles no muestran icono
 * (sin assets ni dependencia de iconos): solo su `label`.
 */
@Composable
fun CuisinesEditor(
    cuisines: List<Cuisine>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        cuisines.chunked(2).forEach { rowCuisines ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            ) {
                rowCuisines.forEach { cuisine ->
                    CuisineTile(
                        cuisine = cuisine,
                        selected = selectedIds.contains(cuisine.id),
                        onClick = { onToggle(cuisine.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowCuisines.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CuisineTile(
    cuisine: Cuisine,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(SazonSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cuisine.label,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
