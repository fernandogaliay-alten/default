package com.sazon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Destino minimo reutilizable del armazon principal: Favoritos, Perfil y Generador (empujado con
 * boton atras). No implementa sus pantallas reales; esas features (`favorites`, `profile-screen`,
 * `generator-ingredients-step`) sustituiran solo el contenido, igual que este archivo sustituye a
 * `HomePlaceholder`.
 */
@Composable
fun SectionPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (onBack != null) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(SazonSpacing.md),
                ) {
                    Text(text = "Atrás")
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
