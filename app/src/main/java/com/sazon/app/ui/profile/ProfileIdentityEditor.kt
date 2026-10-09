package com.sazon.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sazon.app.domain.taxonomy.AvatarColor
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.onboarding.AvatarInitials
import com.sazon.app.ui.onboarding.AvatarPreview
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Cabecera de identidad del Perfil y su modo edicion.
 *
 * Reutiliza [AvatarPreview] (iniciales sobre el `hex` de la Taxonomia) y el microcopy de
 * `docs/design/stitch-prompt.md` seccion G ("Tu perfil", "¿Cómo te llamamos?", "Así te saludaremos en
 * Inicio."). En modo edicion muestra el campo, las ocho muestras de `avatarColors` con la seleccionada
 * anillada en terracota y "Guardar" (deshabilitado si el nombre es invalido). No crea una paleta
 * propia: las muestras salen de la Taxonomia.
 */
@Composable
fun ProfileIdentityEditor(
    state: ProfileEditState,
    taxonomy: Taxonomy,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedHex = taxonomy.avatarColor(state.avatarColorId)?.hex
        ?: taxonomy.avatarColors.firstOrNull()?.hex.orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarPreview(
                colorHex = selectedHex,
                initials = AvatarInitials.of(state.displayName),
                size = 64.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Tu perfil",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isEditing) {
                TextButton(onClick = onEdit) {
                    Text(text = "Editar")
                }
            }
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(SazonSpacing.md))
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onNameChange,
                label = { Text("¿Cómo te llamamos?") },
                supportingText = { Text("Así te saludaremos en Inicio.") },
                singleLine = true,
                isError = state.displayName.isNotEmpty() && !state.isNameValid,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(SazonSpacing.md))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            ) {
                taxonomy.avatarColors.forEach { color ->
                    AvatarColorSwatch(
                        color = color,
                        selected = color.id == state.avatarColorId,
                        onClick = { onColorChange(color.id) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(SazonSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text(text = "Cancelar")
                }
                Button(
                    onClick = onSave,
                    enabled = state.isNameValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Guardar")
                }
            }
        }
    }
}

@Composable
private fun AvatarColorSwatch(
    color: AvatarColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatch = Color(color.hex.toColorInt())
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(swatch)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = color.label },
    )
}
