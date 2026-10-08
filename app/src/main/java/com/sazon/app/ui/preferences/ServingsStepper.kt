package com.sazon.app.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Servings

/**
 * Editor reutilizable de comensales (stepper), sin estado.
 *
 * Muestra `− value +` con cifras tabulares; `−` se deshabilita en `servings.min` y `+` en
 * `servings.max` (el recorte definitivo lo hace el borrador). Recibe los limites de la Taxonomia, el
 * valor y un callback; no conoce el onboarding ni el Perfil.
 */
@Composable
fun ServingsStepper(
    servings: Servings,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepperButton(
            label = "\u2212",
            enabled = value > servings.min,
            onClick = { onChange(value - 1) },
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp),
        )
        StepperButton(
            label = "+",
            enabled = value < servings.max,
            onClick = { onChange(value + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(48.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall)
    }
}

private const val TABULAR_FIGURES = "tnum"
