package com.sazon.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Armazon reutilizable del Onboarding (paso 1 y pasos 2-7; Perfil reusara su contenido):
 * etiqueta de progreso "Paso N de 7" con puntos, "Atras" arriba a la izquierda (solo si se puede
 * retroceder), contenido con scroll y un unico boton primario terracota anclado abajo.
 *
 * No conoce el contenido ni los datos: recibe progreso, accion de atras y etiqueta/habilitacion del
 * primario. Asi los pasos siguientes no duplican pantalla.
 */
@Composable
fun OnboardingScaffold(
    progress: OnboardingProgress,
    canGoBack: Boolean,
    onBack: () -> Unit,
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OnboardingHeader(
                progress = progress,
                canGoBack = canGoBack,
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SazonSpacing.lg),
            ) {
                content()
            }

            Button(
                onClick = onPrimary,
                enabled = primaryEnabled,
                shape = SazonPillShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SazonSpacing.lg,
                        end = SazonSpacing.lg,
                        top = SazonSpacing.sm,
                        bottom = SazonSpacing.md,
                    )
                    .height(SazonComponents.buttonHeight),
            ) {
                Text(text = primaryLabel)
            }
        }
    }
}

@Composable
private fun OnboardingHeader(
    progress: OnboardingProgress,
    canGoBack: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SazonSpacing.sm,
                end = SazonSpacing.md,
                top = SazonSpacing.sm,
                bottom = SazonSpacing.sm,
            ),
    ) {
        if (canGoBack) {
            TextButton(onClick = onBack) {
                Text(text = "Atrás")
            }
        } else {
            // Reserva la altura del boton para que el progreso no salte al aparecer "Atras".
            Spacer(modifier = Modifier.height(SazonComponents.buttonHeight))
        }
        Text(
            text = progress.progressLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SazonSpacing.sm),
        )
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        Row(
            modifier = Modifier.padding(horizontal = SazonSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            repeat(progress.totalSteps) { index ->
                ProgressDot(active = index <= progress.stepIndex)
            }
        }
    }
}

@Composable
private fun ProgressDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(if (active) 10.dp else 8.dp)
            .clip(CircleShape)
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ),
    )
}
