package com.sazon.app.ui.cooking

import android.os.SystemClock
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing
import kotlinx.coroutines.delay

/** Cifras tabulares para el progreso, la cuenta atras y el numero de paso ("tnum" en `DESIGN.md`). */
private const val TABULAR_FIGURES = "tnum"

/** Opacidad de un paso marcado como hecho (`DESIGN.md`: "dims the row opacity to 0.45"). */
private const val COMPLETED_ALPHA = 0.45f

/**
 * Pantalla de **Modo cocina**: capa Compose **delgada** sobre la logica pura de `ui.cooking`
 * ([CookingModeState], [CookingTimerState], [CookingModeFormat]). Direccion visual de
 * `docs/design/concepts/cooking-mode/modo-cocina/` (Stitch no genero una variante por estado del
 * temporizador; los cuatro estados se implementan a partir de esa misma captura, manda `DESIGN.md`).
 *
 * Estructura:
 * - **Cabecera**: boton circular "Atras" + "Progreso"/"Paso X de N" en cifras tabulares y barra fina
 *   terracota ([CookingModeFormat.progressFraction]).
 * - **Titulo** de la receta (una linea, elipsis) y **tarjeta del paso** (circulo numerado terracota,
 *   titulo y cuerpo; tachados y atenuados si el paso esta en `completedOrders`).
 * - **Bloque del temporizador** (solo si `timer.hasDuration`), con acento miel: pildora de duracion,
 *   cuenta atras en cifras tabulares y el control de cada uno de los cuatro estados; al terminar, el
 *   aviso **"¡Listo!"** en miel/oliva (nunca `error`).
 * - **Fila de marcado** "Marcar paso como hecho" y **barra inferior** fija "Anterior" + primario
 *   "Siguiente" (o "Terminar" en el ultimo paso).
 *
 * El reloj real vive **fuera** de la logica pura: un [LaunchedEffect] alimenta `tick(1000)` mientras
 * el temporizador esta En marcha. La receta es un parametro (no se guarda en el `Saver`).
 *
 * **Aviso de sistema** (feature `cooking-timer-alarm`): un segundo [LaunchedEffect] (con
 * `state.timer.status` como clave, de modo que los `tick` no lo reinician) pregunta a la politica
 * **pura** [CookingAlarmPolicy] que hacer con el aviso en cada transicion y lo aplica por la costura
 * [CookingAlarm] (default [NoOpCookingAlarm]: la pantalla no conoce Android). El instante de fin se
 * mide con `SystemClock.elapsedRealtime()`, que entra **inyectado** en la politica (que nunca lee el
 * reloj). Un [DisposableEffect] cancela el aviso al desmontarse (Atras/Terminar/`onClose`).
 *
 * **Permiso de notificaciones en contexto** (feature `cooking-timer-alarm-permission-context`): en ese
 * **mismo** efecto, [NotificationPermissionPolicy] (pura) decide si hay que mostrar el rationale la
 * **primera** vez que arranca el temporizador sin permiso; si es asi se registra el intento
 * (`markAsked`, una vez por sesion) y se muestra [NotificationPermissionRationale], que solo al pulsar
 * "Continuar" lanza el dialogo del sistema por la costura [NotificationPermissionPrompt] (default
 * [NoOpNotificationPermissionPrompt]). El rationale **no** gatea nada: el aviso ya esta programado y el
 * reloj de la UI es un efecto aparte.
 *
 * **Sesion persistida** (feature `cooking-timer-foreground-service`): la pantalla guarda su estado por
 * **cambio significativo** en la costura [CookingSessionStore] (default [NoOpCookingSessionStore]) y lo
 * **borra** al salir ("Atras"/"Terminar"). Si llega [resume] (el usuario toco una notificacion y el
 * proceso habia muerto), el estado inicial es el **paso exacto** con la cuenta atras **recalculada**
 * desde el instante de fin y el trabajo del sistema (alarma + servicio) se **rearma** una vez. El
 * desmontaje de la composicion **no** borra la sesion: una muerte de actividad o una rotacion no debe
 * perderla.
 */
@Composable
fun CookingModeScreen(
    recipe: Recipe,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    alarm: CookingAlarm = NoOpCookingAlarm,
    notificationPermission: NotificationPermissionPrompt = NoOpNotificationPermissionPrompt,
    session: CookingSessionStore = NoOpCookingSessionStore,
    resume: CookingSession? = null,
) {
    var state by rememberSaveable(stateSaver = CookingModeStateSaver) {
        mutableStateOf(
            resume?.resumedMode(SystemClock.elapsedRealtime()) ?: CookingModeState.initial(recipe),
        )
    }
    // Ultimo temporizador al que se le aplico la politica: el efecto solo corre al **cambiar de
    // estado**, asi que basta con guardar el primero de cada estado (solo se consulta por
    // `isRunning`/igualdad, nunca por `remainingMillis`). Arranca ya igual al estado (incluida la
    // reanudacion), de modo que el efecto de transiciones no rearma lo que rearma la reanudacion.
    var previousTimer by remember { mutableStateOf(state.timer) }
    // Rationale del permiso: la primera vez que arranca el temporizador sin permiso concedido.
    var showRationale by rememberSaveable { mutableStateOf(false) }
    // Salida explicita ("Atras"/"Terminar"): primero se **borra** la sesion y solo despues se cierra el
    // modo, para que el borrado no se quede a medias si la composicion se desmonta antes.
    var exiting by remember { mutableStateOf(false) }

    // Reanudacion de una sesion En marcha (el proceso murio a mitad): la cuenta atras ya se recalculo
    // desde el instante de fin, pero la alarma y el servicio murieron con el proceso, asi que se
    // **rearma** el trabajo del sistema **una vez**, tratando la reanudacion como `IDLE -> RUNNING`.
    // `previousTimer` arranca ya igual al temporizador del estado para que el efecto de transiciones no
    // lo repita.
    LaunchedEffect(Unit) {
        val resumed = resume ?: return@LaunchedEffect
        if (resumed.timer.status != CookingTimerStatus.RUNNING) return@LaunchedEffect
        alarm.apply(
            CookingAlarmPolicy.onTimerChange(
                previous = resumed.timer.copy(status = CookingTimerStatus.IDLE),
                next = state.timer,
                nowMillis = SystemClock.elapsedRealtime(),
            ),
        )
    }

    // Persistencia por **cambio significativo** (arrancar, pausar, reanudar, reiniciar, cambiar de paso,
    // marcar, llegar a cero): **nunca** por `tick`, porque lo que queda de cuenta atras se recalcula al
    // reanudar desde el instante de fin.
    LaunchedEffect(state.stepIndex, state.timer.status, state.completedOrders) {
        session.save(CookingSession.of(state, recipe, SystemClock.elapsedRealtime()))
    }

    // Salida explicita: se borra la sesion y despues se cierra el modo (la notificacion que quede en la
    // bandeja ya no reanuda nada). El `onDispose` de abajo **no** borra nada.
    LaunchedEffect(exiting) {
        if (exiting) {
            session.clear()
            onClose()
        }
    }
    val requestExit: () -> Unit = { exiting = true }

    // Transiciones del temporizador -> comando del aviso (programar al Iniciar/Reanudar, cancelar al
    // Pausar/Reiniciar/cambiar de paso/llegar a cero) y -> decision del permiso (rationale la primera
    // vez que se arranca sin permiso). Los `tick` devuelven `None` en ambos casos: no se reprograma ni
    // se pide permiso cada segundo.
    LaunchedEffect(state.timer.status) {
        val now = SystemClock.elapsedRealtime()
        alarm.apply(
            CookingAlarmPolicy.onTimerChange(
                previous = previousTimer,
                next = state.timer,
                nowMillis = now,
            ),
        )
        val permissionDecision = NotificationPermissionPolicy.onTimerChange(
            previous = previousTimer,
            next = state.timer,
            state = notificationPermission.state(),
        )
        if (permissionDecision == NotificationPermissionCommand.AskWithRationale) {
            // Se registra al MOSTRAR el rationale (no al responder): si el usuario lo deja sin
            // contestar, la sesion tampoco vuelve a molestar.
            notificationPermission.markAsked(now)
            showRationale = true
        }
        previousTimer = state.timer
    }

    // Salir del modo (Atras, Terminar, `onClose` o cualquier descarte de la composicion) no debe dejar
    // ningun aviso vivo ni la cuenta atras pintandose: la cancelacion es idempotente, haya o no algo
    // programado. **No** borra la sesion persistida (una rotacion o una muerte de actividad no deben
    // perderla; solo la salida explicita la borra).
    DisposableEffect(Unit) {
        onDispose { alarm.apply(CookingAlarmPolicy.onExit()) }
    }

    LaunchedEffect(state.timer.status) {
        while (state.timer.isRunning) {
            delay(1_000L)
            state = state.withTimer(state.timer.tick(1_000L))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            CookingBottomBar(
                isEmpty = state.isEmpty,
                isFirstStep = state.isFirstStep,
                isLastStep = state.isLastStep,
                onPrevious = { state = state.previous(recipe.steps) },
                onNext = { state = state.next(recipe.steps) },
                onClose = requestExit,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            CookingModeHeader(
                stepIndex = state.stepIndex,
                totalSteps = state.totalSteps,
                onBack = requestExit,
            )
            val step = recipe.steps.getOrNull(state.stepIndex)
            if (state.isEmpty || step == null) {
                EmptyStepsNotice()
            } else {
                RecipeHeading(title = recipe.title)
                StepCard(
                    step = step,
                    completed = step.order in state.completedOrders,
                )
                if (state.timer.hasDuration) {
                    TimerBlock(
                        minutes = step.minutes,
                        timer = state.timer,
                        onStart = { state = state.withTimer(state.timer.start()) },
                        onPause = { state = state.withTimer(state.timer.pause()) },
                        onResume = { state = state.withTimer(state.timer.resume()) },
                        onReset = { state = state.withTimer(state.timer.reset()) },
                    )
                }
                MarkStepRow(
                    completed = step.order in state.completedOrders,
                    onToggle = { state = state.toggleCompleted(step.order) },
                )
            }
            Spacer(modifier = Modifier.height(SazonSpacing.lg))
        }
    }

    // Rationale previo al dialogo del sistema: se muestra la primera vez que arranca el temporizador
    // sin permiso. "Continuar" lanza el dialogo; "Ahora no" (o descartarlo) no pide nada y no vuelve a
    // molestar en la sesion. La costura no conoce Android (default no-op).
    if (showRationale) {
        NotificationPermissionRationale(
            onConfirm = {
                showRationale = false
                notificationPermission.request()
            },
            onDismiss = { showRationale = false },
        )
    }
}

/**
 * Rationale del permiso de notificaciones: `AlertDialog` de Material 3 con el microcopy de
 * [NotificationPermissionCopy] y los roles de `SazonTheme` (sin tokens, formas ni colores nuevos;
 * `DESIGN.md` no cubre dialogos). Una sola accion principal ("Continuar") y el descarte "Ahora no".
 */
@Composable
private fun NotificationPermissionRationale(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = NotificationPermissionCopy.TITLE) },
        text = { Text(text = NotificationPermissionCopy.MESSAGE) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(text = NotificationPermissionCopy.CONFIRM)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = NotificationPermissionCopy.DISMISS)
            }
        },
    )
}

/** Cabecera: "Atras" circular flotante, etiqueta de progreso y barra fina terracota. */
@Composable
private fun CookingModeHeader(
    stepIndex: Int,
    totalSteps: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg, vertical = SazonSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ChevronGlyph(
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (totalSteps > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Progreso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CookingModeFormat.progressLabel(stepIndex, totalSteps),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = TABULAR_FIGURES,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        ProgressBar(fraction = CookingModeFormat.progressFraction(stepIndex, totalSteps))
    }
}

/** Barra de progreso fina: pista `outlineVariant` y relleno terracota (una sola accion primaria). */
@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(SazonPillShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(SazonPillShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** Rotulo "Cocinando ahora" + titulo de la receta (Manrope, una linea, elipsis). */
@Composable
private fun RecipeHeading(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = "Cocinando ahora",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tarjeta del paso: circulo numerado terracota, titulo y cuerpo (tachados si esta hecho). */
@Composable
private fun StepCard(
    step: RecipeStep,
    completed: Boolean,
) {
    val contentAlpha = if (completed) COMPLETED_ALPHA else 1f
    val decoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg, vertical = SazonSpacing.md),
        shape = SazonShapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SazonSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step.order.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFeatureSettings = TABULAR_FIGURES,
                            ),
                        )
                    }
                }
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = decoration,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(contentAlpha),
                )
            }
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = decoration,
                modifier = Modifier.alpha(contentAlpha),
            )
        }
    }
}

/**
 * Bloque del temporizador por paso, con acento miel (`tertiaryContainer`): pildora de duracion,
 * cuenta atras en cifras tabulares y el control del estado actual. Solo se pinta cuando el paso
 * tiene duracion (`hasDuration`).
 */
@Composable
private fun TimerBlock(
    minutes: Int,
    timer: CookingTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timerPulseAlpha",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg),
        shape = SazonShapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SazonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClockGlyph(
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Temporizador de paso",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                DurationPill(minutes = minutes)
            }
            Text(
                text = timerCaption(timer.status),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            when (timer.status) {
                CookingTimerStatus.IDLE -> {
                    CountdownRow(
                        remainingMillis = timer.totalMillis,
                        totalMillis = null,
                    )
                    TimerActionButton(label = "Iniciar", onClick = onStart)
                }

                CookingTimerStatus.RUNNING -> {
                    CountdownRow(
                        remainingMillis = timer.remainingMillis,
                        totalMillis = timer.totalMillis,
                        alpha = pulseAlpha,
                    )
                    TimerActionButton(label = "Pausar", onClick = onPause)
                }

                CookingTimerStatus.PAUSED -> {
                    CountdownRow(
                        remainingMillis = timer.remainingMillis,
                        totalMillis = null,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                    ) {
                        FilledTonalButton(
                            onClick = onReset,
                            shape = SazonPillShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(SazonComponents.buttonHeight),
                        ) {
                            Text(text = "Reiniciar")
                        }
                        TimerActionButton(
                            label = "Reanudar",
                            onClick = onResume,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }

                CookingTimerStatus.FINISHED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = CookingModeFormat.countdown(timer.remainingMillis),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFeatureSettings = TABULAR_FIGURES,
                            ),
                        )
                        FinishedNotice()
                    }
                    TimerActionButton(label = "Reiniciar", onClick = onReset)
                }
            }
        }
    }
}

/** Pildora miel con la duracion total del paso (`"10 min"`). */
@Composable
private fun DurationPill(minutes: Int) {
    Surface(
        shape = SazonPillShape,
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
    ) {
        Text(
            text = CookingModeFormat.durationLabel(minutes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            modifier = Modifier.padding(horizontal = SazonSpacing.sm, vertical = 4.dp),
        )
    }
}

/** Cuenta atras en cifras tabulares, con el total como referencia cuando aplica. */
@Composable
private fun CountdownRow(
    remainingMillis: Long,
    totalMillis: Long?,
    alpha: Float = 1f,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = CookingModeFormat.countdown(remainingMillis),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            modifier = Modifier.alpha(alpha),
        )
        if (totalMillis != null) {
            Text(
                text = "/ ${CookingModeFormat.countdown(totalMillis)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFeatureSettings = TABULAR_FIGURES,
                ),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

/** Aviso de exito al llegar a cero: miel/oliva con visto, **nunca** `error`. */
@Composable
private fun FinishedNotice() {
    Surface(
        shape = SazonPillShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SazonSpacing.sm, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckGlyph(
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = CookingModeFormat.FINISHED_NOTICE,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Accion del temporizador en miel (`tertiary`), para no robar el terracota a la accion principal. */
@Composable
private fun TimerActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = SazonPillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(SazonComponents.buttonHeight),
    ) {
        Text(text = label)
    }
}

/** Fila de marcado del paso; independiente de la navegacion y del temporizador. */
@Composable
private fun MarkStepRow(
    completed: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg, vertical = SazonSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = completed, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(SazonSpacing.sm))
        Text(
            text = "Marcar paso como hecho",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Guarda defensiva: el dominio garantiza `1..n` pasos, asi que no deberia alcanzarse. */
@Composable
private fun EmptyStepsNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SazonSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = "Cocinando ahora",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Esta receta no tiene pasos que seguir.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Barra inferior fija: "Anterior" (outline) y el primario terracota "Siguiente"/"Terminar". */
@Composable
private fun CookingBottomBar(
    isEmpty: Boolean,
    isFirstStep: Boolean,
    isLastStep: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val primaryIsFinish = isEmpty || isLastStep
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SazonSpacing.lg,
                    end = SazonSpacing.lg,
                    top = SazonSpacing.sm,
                    bottom = SazonSpacing.md,
                ),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            OutlinedButton(
                onClick = { if (isEmpty) onClose() else onPrevious() },
                enabled = isEmpty || !isFirstStep,
                shape = SazonPillShape,
                modifier = Modifier
                    .weight(1f)
                    .height(SazonComponents.buttonHeight),
            ) {
                Text(text = if (isEmpty) "Atrás" else "Anterior")
            }
            Button(
                onClick = { if (primaryIsFinish) onClose() else onNext() },
                shape = SazonPillShape,
                modifier = Modifier
                    .weight(1.6f)
                    .height(SazonComponents.buttonHeight),
            ) {
                Text(text = if (primaryIsFinish) "Terminar" else "Siguiente")
            }
        }
    }
}

/** Microcopy de la fila de estado del temporizador (captura `modo-cocina`). */
private fun timerCaption(status: CookingTimerStatus): String = when (status) {
    CookingTimerStatus.IDLE -> "Duración paso"
    CookingTimerStatus.RUNNING -> "Cuenta atrás"
    CookingTimerStatus.PAUSED -> "Tiempo congelado"
    CookingTimerStatus.FINISHED -> "Tiempo cumplido"
}

/** Glifo de reloj (circulo + agujas) dibujado con Compose, sin iconos externos. */
@Composable
private fun ClockGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = tint, radius = size.minDimension / 2f - stroke.width, style = stroke)
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - size.minDimension * 0.26f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + size.minDimension * 0.22f, center.y),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

/** Glifo de visto (aviso "¡Listo!"). */
@Composable
private fun CheckGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0.10f * size.width, 0.55f * size.height)
            lineTo(0.38f * size.width, 0.82f * size.height)
            lineTo(0.90f * size.width, 0.20f * size.height)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** Glifo de chevron del boton "Atras" (apunta a la izquierda), sin iconos externos. */
@Composable
private fun ChevronGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0.65f * size.width, 0.18f * size.height)
            lineTo(0.35f * size.width, 0.50f * size.height)
            lineTo(0.65f * size.width, 0.82f * size.height)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * Saver de [CookingModeState] para `rememberSaveable`, en la capa Compose: guarda el indice, el total,
 * los `order` marcados y el estado/millis del temporizador. La `Recipe` **no** se guarda (es un
 * parametro), de modo que la logica pura queda libre de Compose.
 */
private val CookingModeStateSaver: Saver<CookingModeState, Any> = listSaver(
    save = { state ->
        listOf(
            state.stepIndex.toString(),
            state.totalSteps.toString(),
            state.completedOrders.joinToString(separator = ","),
            state.timer.status.name,
            state.timer.totalMillis.toString(),
            state.timer.remainingMillis.toString(),
        )
    },
    restore = { saved ->
        CookingModeState(
            stepIndex = (saved[0] as String).toInt(),
            totalSteps = (saved[1] as String).toInt(),
            completedOrders = (saved[2] as String)
                .takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                .orEmpty(),
            timer = CookingTimerState(
                status = CookingTimerStatus.valueOf(saved[3] as String),
                totalMillis = (saved[4] as String).toLong(),
                remainingMillis = (saved[5] as String).toLong(),
            ),
        )
    },
)
