package com.sazon.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.sazon.app.data.image.DishImageSettingsState
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Seccion **"Imagenes del plato"** del Perfil: interruptor de generacion + tope por sesion visible.
 *
 * Capa Compose **delgada**: todo el microcopy sale de [DishImageSettingsCopy] (puro, testeado en JVM)
 * y solo se pintan tokens/ritmo ya existentes del Perfil (titulo `titleMedium`, label `bodyLarge`,
 * resumen `bodyMedium`/`onSurfaceVariant`, helper `labelSmall`, `SazonSpacing.md`). La fila entera es
 * el control (`toggleable` con `role = Role.Switch`) y el [Switch] solo refleja el estado
 * (`onCheckedChange = null`), para no depender solo del color (Accessibility Baseline) y mantener el
 * objetivo tactil >= 48dp. Sin iconos ni dependencias nuevas.
 */
@Composable
fun DishImageSettingsSection(
    state: DishImageSettingsState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = SazonSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = DishImageSettingsCopy.SECTION_TITLE,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = state.enabled,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(vertical = SazonSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DishImageSettingsCopy.TOGGLE_LABEL,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = DishImageSettingsCopy.summary(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = DishImageSettingsCopy.HELPER_TEXT,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = null,
            )
        }
    }
}
