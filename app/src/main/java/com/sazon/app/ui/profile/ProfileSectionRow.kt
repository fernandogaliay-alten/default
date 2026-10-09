package com.sazon.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Fila de resumen de una seccion del Perfil: etiqueta del grupo, resumen de los valores actuales y un
 * chevron. Toda la fila es pulsable (objetivo tactil >=48dp) para abrir la hoja de edicion
 * correspondiente. El resumen se calcula en el huesped con labels de la Taxonomia (con respaldo al
 * `id`); aqui solo se pinta.
 */
@Composable
fun ProfileSectionRow(
    label: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SazonSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Chevron(tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Chevron dibujado con Compose (sin dependencia de iconos): dos trazos en ">". */
@Composable
private fun Chevron(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val midY = size.height / 2f
        drawLine(
            color = tint,
            start = Offset(x = size.width * 0.35f, y = midY - size.height * 0.25f),
            end = Offset(x = size.width * 0.65f, y = midY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(x = size.width * 0.65f, y = midY),
            end = Offset(x = size.width * 0.35f, y = midY + size.height * 0.25f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
