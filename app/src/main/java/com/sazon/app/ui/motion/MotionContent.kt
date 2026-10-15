package com.sazon.app.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sazon.app.ui.theme.SazonMotion

/** Eje del deslizamiento: horizontal (navegacion y pasos) o vertical (overlays y contenidos). */
enum class MotionAxis { HORIZONTAL, VERTICAL }

/**
 * **Unica** primitiva de movimiento de la app (los cuatro frentes la reutilizan; no se usan
 * `Crossfade` ni `AnimatedVisibility` para no fragmentar el lenguaje).
 *
 * Envuelve [AnimatedContent] con una `transitionSpec` construida desde los tokens [SazonMotion]:
 * fundido + deslizamiento de [SazonMotion.SLIDE_OFFSET_DP] con `FastOutSlowInEasing` y
 * `SazonMotion.DURATION_MILLIS`. En [MotionAxis.HORIZONTAL] la direccion ([MotionDirection]) decide el
 * signo: `FORWARD` entra desde la derecha y la saliente se va a la izquierda, `BACKWARD` al reves,
 * `NONE` no desplaza. En [MotionAxis.VERTICAL] entra **desde abajo** y sale hacia abajo (la
 * `direction` se **ignora**, documentado). Solo pinta: los efectos secundarios y los `Saver` de los
 * llamantes quedan **fuera**.
 *
 * `sizeTransform = null` evita recorte y saltos de layout (las superficies son de tamano fijo o viven
 * en un contenedor con scroll) y `targetContentZIndex` fija el orden de capas de `SazonMotion`.
 *
 * @param targetState estado cuya identidad dispara la transicion.
 * @param direction direccion del cambio (por defecto `NONE` = solo fundido).
 */
@Composable
fun <T> MotionContent(
    targetState: T,
    modifier: Modifier = Modifier,
    axis: MotionAxis = MotionAxis.HORIZONTAL,
    direction: (initial: T, target: T) -> MotionDirection = { _, _ -> MotionDirection.NONE },
    content: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val resolved = direction(initialState, targetState)
            val slideSpec = tween<IntOffset>(
                durationMillis = SazonMotion.DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            )
            val fadeInSpec = fadeIn(
                animationSpec = tween(
                    durationMillis = SazonMotion.DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            val fadeOutSpec = fadeOut(
                animationSpec = tween(
                    durationMillis = SazonMotion.DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            val enter: EnterTransition
            val exit: ExitTransition
            when (axis) {
                MotionAxis.HORIZONTAL -> {
                    val enterDp = MotionPolicy.enterOffsetDp(resolved)
                    val exitDp = MotionPolicy.exitOffsetDp(resolved)
                    enter = fadeInSpec + slideInHorizontally(slideSpec) {
                        with(density) { enterDp.dp.roundToPx() }
                    }
                    exit = fadeOutSpec + slideOutHorizontally(slideSpec) {
                        with(density) { exitDp.dp.roundToPx() }
                    }
                }

                MotionAxis.VERTICAL -> {
                    val offsetDp = MotionPolicy.enterOffsetDp(MotionDirection.FORWARD)
                    enter = fadeInSpec + slideInVertically(slideSpec) {
                        with(density) { offsetDp.dp.roundToPx() }
                    }
                    exit = fadeOutSpec + slideOutVertically(slideSpec) {
                        with(density) { offsetDp.dp.roundToPx() }
                    }
                }
            }
            ContentTransform(
                targetContentEnter = enter,
                initialContentExit = exit,
                targetContentZIndex = if (SazonMotion.ENTER_ON_TOP) 1f else -1f,
                sizeTransform = null,
            )
        },
        label = "sazon-motion",
    ) { state ->
        content(state)
    }
}
