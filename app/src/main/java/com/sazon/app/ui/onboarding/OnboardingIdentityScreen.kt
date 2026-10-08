package com.sazon.app.ui.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Paso 1 del Onboarding: identidad (nombre obligatorio + color de avatar) con vista previa en vivo.
 *
 * El microcopy sale de `docs/design/stitch-prompt.md` (seccion A, paso 1). El nombre se valida con
 * [OnboardingIdentityState] (via `ProfileName`); el color se elige entre los `avatarColors` de la
 * Taxonomia, nunca de una paleta propia.
 */
@Composable
fun OnboardingIdentityScreen(
    identity: OnboardingIdentityState,
    taxonomy: Taxonomy,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SazonSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cocina con lo que ya tienes",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dinos qué te gusta y la IA hará el resto.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.lg))

        val selectedHex = taxonomy.avatarColor(identity.avatarColorId)?.hex
            ?: taxonomy.avatarColors.first().hex
        AvatarPreview(colorHex = selectedHex, initials = identity.initials)

        Spacer(modifier = Modifier.height(SazonSpacing.lg))

        OutlinedTextField(
            value = identity.name,
            onValueChange = onNameChange,
            label = { Text("¿Cómo te llamamos?") },
            supportingText = { Text("Así te saludaremos en Inicio.") },
            singleLine = true,
            isError = identity.name.isNotEmpty() && !identity.isNameValid,
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
                    selected = color.id == identity.avatarColorId,
                    onClick = { onColorChange(color.id) },
                )
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
