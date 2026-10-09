package com.sazon.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Seccion **"Sugerencias"** del Perfil: expone la politica cache-first mostrando cuando se actualizo
 * el Lote por ultima vez y ofreciendo un refresco manual.
 *
 * Capa Compose delgada: toda la fecha y el microcopy salen de [SuggestionsRefreshFormat], puro y
 * testeado en JVM. No hay iconos ni imagenes (el repositorio no tiene `material-icons`). El `now` lo
 * captura el huesped una sola vez. El boton es la unica accion **secundaria** de la pantalla (tono
 * oliva), para no competir con la accion principal terracota del Perfil.
 */
@Composable
fun ProfileSuggestionsSection(
    generatedAt: Long?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    now: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = SazonSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = "Sugerencias",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = SuggestionsRefreshFormat.lastUpdatedLabel(generatedAt, now),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = SuggestionsRefreshFormat.HELPER_TEXT,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onRefresh,
            enabled = !isRefreshing,
            shape = SazonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier.height(SazonComponents.buttonHeight),
        ) {
            Text(
                text = if (isRefreshing) {
                    SuggestionsRefreshFormat.REFRESHING_LABEL
                } else {
                    SuggestionsRefreshFormat.ACTION_LABEL
                },
            )
        }
    }
}
