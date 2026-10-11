package com.sazon.app.ui.generator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sazon.app.ui.theme.SazonSpacing
import kotlinx.coroutines.delay

private const val SUBLINE_ROTATION_MILLIS = 2200L
private val PLATE_SIZE = 96.dp

/**
 * Estado de generacion a pantalla completa (`docs/design/stitch-prompt.md` C), capa Compose delgada:
 * una animacion calida terracota dibujada con Compose (sin iconos ni imagenes), el titular en Manrope,
 * las sublineas rotatorias con `LaunchedEffect` + `delay` y un "Cancelar" **secundario** (`TextButton`,
 * no la accion principal). El microcopy lo compone [GeneratorProgressMessages] (puro); esta pantalla
 * solo lo pinta y delega la cancelacion en [onCancel].
 */
@Composable
fun GeneratorGeneratingScreen(
    headline: String,
    subLines: List<String>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "Cancelar",
) {
    var index by remember(subLines) { mutableIntStateOf(0) }
    LaunchedEffect(subLines) {
        if (subLines.size <= 1) return@LaunchedEffect
        while (true) {
            delay(SUBLINE_ROTATION_MILLIS)
            index = (index + 1) % subLines.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SazonSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GeneratingAnimation()

        Spacer(modifier = Modifier.height(SazonSpacing.lg))
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        val subLine = subLines.getOrNull(index).orEmpty()
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        Text(
            text = subLine,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(SazonSpacing.lg))
        TextButton(onClick = onCancel) {
            Text(text = cancelLabel)
        }
    }
}

/**
 * Animacion calida: un plato/reloj terracota que gira sobre una pista ambar, dibujado con `Canvas`
 * (sin iconos ni imagenes ni dependencias nuevas).
 */
@Composable
private fun GeneratingAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "generating")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    val track = MaterialTheme.colorScheme.tertiaryContainer
    val arc = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(PLATE_SIZE)) {
        val strokeWidth = 8.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(width = size.width - strokeWidth, height = size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        drawArc(
            color = arc,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
