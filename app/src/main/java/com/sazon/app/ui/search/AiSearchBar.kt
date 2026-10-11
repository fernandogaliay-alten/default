package com.sazon.app.ui.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bloque 2 de la Home: la barra de **Busqueda con IA** (capa Compose delgada).
 *
 * Pildora (`SazonPillShape`) con el glifo de destello a la izquierda, el envio circular terracota a la
 * derecha y el helper debajo. Sin conexion, el campo queda deshabilitado y atenuado y el helper pasa a
 * [AiSearchCopy.OFFLINE_NOTE]. El envio solo se habilita cuando [AiSearchQuery.canSubmit] lo permite
 * (conexion + texto no en blanco); la tecla de teclado es `ImeAction.Search`. Esta capa no decide
 * nada: solo pinta y delega en [onQueryChange]/[onSubmit].
 */
@Composable
fun AiSearchBar(
    query: String,
    isOnline: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSubmit = AiSearchQuery.canSubmit(query, isOnline)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = isOnline,
            singleLine = true,
            shape = SazonPillShape,
            placeholder = {
                Text(
                    text = AiSearchCopy.PLACEHOLDER,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = { SparkGlyph(tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { SubmitButton(enabled = canSubmit, onSubmit = onSubmit) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (canSubmit) onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = if (isOnline) AiSearchCopy.HELPER else AiSearchCopy.OFFLINE_NOTE,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SazonSpacing.md),
        )
    }
}

/** Envio circular terracota (deshabilitado si no se puede enviar); la "flecha" se dibuja con `Canvas`. */
@Composable
private fun SubmitButton(
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(enabled = enabled, onClick = onSubmit),
        contentAlignment = Alignment.Center,
    ) {
        SendGlyph(
            tint = if (enabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Glifo de destello (estrella de cuatro puntas), dibujado sin iconos ni imagenes. */
@Composable
private fun SparkGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outer = size.minDimension / 2f
        val inner = outer * 0.34f
        val path = Path()
        for (vertex in 0 until 8) {
            val radius = if (vertex % 2 == 0) outer else inner
            val angle = Math.toRadians((vertex * 45 - 90).toDouble())
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            if (vertex == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = tint)
    }
}

/** Flecha de envio (triangulo) dibujada con `Canvas`. */
@Composable
private fun SendGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val path = Path()
        path.moveTo(size.width * 0.24f, 0f)
        path.lineTo(size.width * 0.80f, size.height / 2f)
        path.lineTo(size.width * 0.24f, size.height)
        path.close()
        drawPath(path = path, color = tint)
    }
}
