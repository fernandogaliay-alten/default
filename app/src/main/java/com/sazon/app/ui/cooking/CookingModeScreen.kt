package com.sazon.app.ui.cooking

import android.os.SystemClock
import androidx.activity.compose.BackHandler
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
 * El reloj real vive **fuera** de la logica pura: [CookingTimerState] lleva el instante de fin
 * **absoluto** mientras la cuenta atras corre, y un [LaunchedEffect] recalcula `at(elapsedRealtime())`
 * con un latido **alineado al segundo** de la cuenta atras (nunca acumula deriva). La receta es un
 * parametro (no se guarda en el `Saver`).
 *
 * **Trabajo del sistema por gesto** (feature `cooking-timer-notification-reliability`): cada gesto del
 * temporizador o de navegacion ("Iniciar"/"Reanudar"/"Pausar"/"Reiniciar"/"Anterior"/"Siguiente") pasa
 * por un **unico punto de entrada** que hace **una sola** lectura del reloj y con ella aplica el estado
 * nuevo y las **dos** decisiones puras ([CookingAlarmPolicy.onTimerChange], que programa el respaldo en
 * el **ancla** del temporizador + arranca el servicio, y [NotificationPermissionPolicy.onTimerChange],
 * que muestra el rationale la primera vez que arranca sin permiso). El comando `Schedule` transporta
 * ademas la **etiqueta del paso** ([CookingStepLabel]: "Paso N de M" + el titulo, feature
 * `cooking-timer-notification-step-label`), que es lo que la notificacion persistente pinta; se compone
 * con el estado **nuevo** (paso actual) y la receta, y tambien en el rearme al reanudar. Ya **no** hay
 * un efecto con
 * `state.timer.status` como clave ni estado `previousTimer`: la alarma y el servicio se arman/cancelan
 * en el mismo punto que el gesto, no en la composicion.
 *
 * Un [DisposableEffect] cancela el aviso al desmontarse (Atras/Terminar/`onClose`); la pantalla **no**
 * conoce Android (default [NoOpCookingAlarm]).
 *
 * **Permiso de notificaciones en contexto** (feature `cooking-timer-alarm-permission-context`): la
 * decision [NotificationPermissionPolicy] (pura) se consulta en ese **mismo** punto por gesto; si pide
 * el rationale se registra el intento (`markAsked`, una vez por sesion) y se muestra
 * [NotificationPermissionRationale], que solo al pulsar "Continuar" lanza el dialogo del sistema por la
 * costura [NotificationPermissionPrompt] (default [NoOpNotificationPermissionPrompt]). El rationale
 * **no** gatea nada: el aviso ya esta programado y el reloj de la UI es un efecto aparte.
 *
 * **Sesion persistida** (feature `cooking-timer-foreground-service`): la pantalla guarda su estado por
 * **cambio significativo** en la costura [CookingSessionStore] (default [NoOpCookingSessionStore]) y lo
 * **borra** al salir ("Atras"/"Terminar"), momento en el que tambien **retira** el aviso de fin que
 * pudiera quedar en la bandeja ([CookingAlarm.dismissFinishNotification], feature
 * `cooking-timer-finish-notification`). Si llega [resume] (el usuario toco una notificacion y el
 * proceso habia muerto), el estado inicial es el **paso exacto** con la cuenta atras **recalculada**
 * desde el instante de fin (el ancla) y el trabajo del sistema (alarma + servicio) se **rearma** una
 * vez. El desmontaje de la composicion **no** borra la sesion ni limpia el aviso: una muerte de
 * actividad o una rotacion no debe perderla.
 *
 * **Confirmacion al salir** (feature `cooking-mode-exit-confirmation`): las **tres** vias de salida
 * ("Atras" de la cabecera, "Terminar" de la barra inferior —o "Atras" con una receta sin pasos— y el
 * **gesto/tecla atras del sistema**, interceptado con [BackHandler] mientras el modo esta compuesto)
 * pasan por la decision **pura** [CookingExitPolicy]. Si hay algo que perder (temporizador En marcha o
 * Pausado, o algun paso marcado) se muestra [CookingExitDialog]; solo "Salir" activa el camino de salida
 * aceptado (`exiting` -> `session.clear()` -> `onClose()` -> `onDispose`). Abrir/cerrar el dialogo no toca
 * la sesion, la alarma/servicio ni el reloj: la cuenta atras sigue corriendo detras y "Seguir cocinando" no
 * cambia nada. Sin nada que perder, la salida es directa.
 *
 * **Confirmacion al cambiar de paso** (feature `cooking-mode-step-change-confirmation`): "Siguiente" y
 * "Anterior" de la barra inferior pasan por la decision **pura** [CookingStepChangePolicy], que pide
 * confirmacion **solo** si el temporizador del paso actual esta **En marcha o Pausado** (lo unico que se
 * pierde: `next`/`previous` lo reemplazan por el del paso nuevo) y **solo** si la direccion cambia de
 * paso de verdad (el marcado persiste, asi que `completedOrders` **no** cuenta aqui). Con confirmacion se
 * muestra [CookingStepChangeDialog]; "Cambiar de paso" ejecuta la navegacion aceptada
 * (`state.next`/`state.previous`) y "Seguir en este paso" (o el descarte) solo cierra el dialogo: el paso,
 * el tiempo, el marcado, la sesion y la alarma quedan intactos, con la cuenta atras viva detras. Sin nada
 * que perder el cambio es directo. El **gesto/tecla atras del sistema** con este dialogo abierto lo
 * descarta (rama nueva de [BackHandler], antes de la de salida), sin salir del modo.
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
    // Rationale del permiso: la primera vez que arranca el temporizador sin permiso concedido.
    var showRationale by rememberSaveable { mutableStateOf(false) }
    // Salida explicita ("Atras"/"Terminar"): primero se **borra** la sesion y solo despues se cierra el
    // modo, para que el borrado no se quede a medias si la composicion se desmonta antes.
    var exiting by remember { mutableStateOf(false) }
    // Confirmacion de salida visible (feature `cooking-mode-exit-confirmation`): se abre solo cuando la
    // decision **pura** [CookingExitPolicy] dice que hay algo que perder. Abrir/cerrar el dialogo **no**
    // toca `exiting`, la sesion, la alarma ni el reloj: solo "Salir" activa el camino de salida de abajo.
    var confirmExit by remember { mutableStateOf(false) }
    // Confirmacion de cambio de paso visible (feature `cooking-mode-step-change-confirmation`): guarda la
    // **direccion** pendiente (enum, estado estable para Compose) y se abre solo cuando la decision
    // **pura** [CookingStepChangePolicy] dice que el temporizador En marcha/Pausado se perderia. Es
    // `remember` (no `rememberSaveable`), como `confirmExit`: al rotar se pierde, limitacion aceptada.
    var pendingStepChange by remember { mutableStateOf<CookingStepChangePoint?>(null) }

    // Etiqueta del paso ([CookingStepLabel], frontera de privacidad): el numero en la microcopy del
    // encabezado ("Paso N de M") y el titulo del paso, derivados del estado y la receta en el momento de
    // armar el trabajo del sistema. Como es **derivada** no se persiste ni puede quedar obsoleta: cada
    // armado compone la del paso que arranca.
    fun stepLabelOf(modeState: CookingModeState): CookingStepLabel? = CookingStepLabel.of(
        stepIndex = modeState.stepIndex,
        totalSteps = modeState.totalSteps,
        step = recipe.steps.getOrNull(modeState.stepIndex),
    )

    // **Punto unico por gesto** (feature `cooking-timer-notification-reliability`): una sola lectura del
    // reloj alimenta el estado nuevo y las **dos** decisiones puras. Asi la alarma (respaldo) y el
    // servicio se arman/cancelan en el propio gesto, no en un efecto dependiente de la composicion. La
    // etiqueta del paso nuevo viaja con el comando `Schedule` (feature
    // `cooking-timer-notification-step-label`): es lo que la notificacion persistente pinta.
    // Un no-op del temporizador (doble "Iniciar", "Pausar" en Inactivo, "Reiniciar" ya Inactivo, un paso
    // sin duracion) no toca nada.
    fun onUserAction(transform: (CookingModeState, Long) -> CookingModeState) {
        val previous = state.timer
        val now = SystemClock.elapsedRealtime()
        val next = transform(state, now)
        if (next == state) return
        state = next
        alarm.apply(
            CookingAlarmPolicy.onTimerChange(
                previous = previous,
                next = next.timer,
                nowMillis = now,
                stepLabel = stepLabelOf(next),
            ),
        )
        val permissionDecision = NotificationPermissionPolicy.onTimerChange(
            previous = previous,
            next = next.timer,
            state = notificationPermission.state(),
        )
        if (permissionDecision == NotificationPermissionCommand.AskWithRationale) {
            // Se registra al MOSTRAR el rationale (no al responder): si el usuario lo deja sin
            // contestar, la sesion tampoco vuelve a molestar.
            notificationPermission.markAsked(now)
            showRationale = true
        }
    }

    // Reanudacion de una sesion En marcha (el proceso murio a mitad): la cuenta atras ya se recalculo
    // desde el ancla, pero la alarma y el servicio murieron con el proceso, asi que se **rearma** el
    // trabajo del sistema **una vez**, tratando la reanudacion como `IDLE -> RUNNING`. La etiqueta sale
    // del estado **reanudado** (el paso persistido), no del paso 1.
    LaunchedEffect(Unit) {
        val resumed = resume ?: return@LaunchedEffect
        if (resumed.timer.status != CookingTimerStatus.RUNNING) return@LaunchedEffect
        alarm.apply(
            CookingAlarmPolicy.onTimerChange(
                previous = resumed.timer.copy(status = CookingTimerStatus.IDLE),
                next = state.timer,
                nowMillis = SystemClock.elapsedRealtime(),
                stepLabel = stepLabelOf(state),
            ),
        )
    }

    // Persistencia por **cambio significativo** (arrancar, pausar, reanudar, reiniciar, cambiar de paso,
    // marcar, llegar a cero): **nunca** por latido, porque lo que queda de cuenta atras se recalcula al
    // reanudar desde el instante de fin (el ancla).
    LaunchedEffect(state.stepIndex, state.timer.status, state.completedOrders) {
        session.save(CookingSession.of(state, recipe, SystemClock.elapsedRealtime()))
    }

    // Salida explicita: se retira el aviso de fin que quede en la bandeja, se borra la sesion y despues
    // se cierra el modo (la notificacion ya no reanuda nada). El `onDispose` de abajo **no** borra nada
    // ni limpia la notificacion: una rotacion no debe perder la sesion ni borrar el aviso.
    LaunchedEffect(exiting) {
        if (exiting) {
            alarm.dismissFinishNotification()
            session.clear()
            onClose()
        }
    }
    // Las tres vias de salida pasan por la misma decision pura: "Atras" (cabecera y gesto/tecla atras del
    // sistema) por [CookingExitPoint.BACK_BUTTON] y "Terminar" por [CookingExitPoint.FINISH]. Con algo que
    // perder se abre el dialogo; sin nada que perder se sale directamente, como antes de esta feature.
    val requestBack: () -> Unit = {
        if (CookingExitPolicy.requiresConfirmation(state, CookingExitPoint.BACK_BUTTON)) {
            confirmExit = true
        } else {
            exiting = true
        }
    }
    val requestFinish: () -> Unit = {
        if (CookingExitPolicy.requiresConfirmation(state, CookingExitPoint.FINISH)) {
            confirmExit = true
        } else {
            exiting = true
        }
    }

    // Cambio de paso ("Siguiente"/"Anterior"): la decision **pura** [CookingStepChangePolicy] pide
    // confirmacion solo con el temporizador del paso actual En marcha/Pausado y solo si la direccion
    // cambia de paso de verdad; sin nada que perder se ejecuta la navegacion aceptada sin friccion.
    // Confirmar tambien acaba en `state.next`/`state.previous` (abajo), asi que la regla aceptada de
    // reinicio del temporizador al paso nuevo queda **intacta**.
    val requestPrevious: () -> Unit = {
        if (CookingStepChangePolicy.requiresConfirmation(state, CookingStepChangePoint.PREVIOUS)) {
            pendingStepChange = CookingStepChangePoint.PREVIOUS
        } else {
            onUserAction { s, _ -> s.previous(recipe.steps) }
        }
    }
    val requestNext: () -> Unit = {
        if (CookingStepChangePolicy.requiresConfirmation(state, CookingStepChangePoint.NEXT)) {
            pendingStepChange = CookingStepChangePoint.NEXT
        } else {
            onUserAction { s, _ -> s.next(recipe.steps) }
        }
    }

    // Gesto/tecla atras del sistema: se intercepta **solo mientras el Modo cocina esta compuesto** (fuera
    // del modo, la app no gana pila de navegacion y el atras sigue como hoy). Con el dialogo de cambio de
    // paso abierto, el atras lo **descarta** (equivale a "Seguir en este paso": no cambia de paso, no sale
    // del modo y no reabre). Con el dialogo de salida abierto, lo descarta igual. Sin ningun dialogo,
    // enruta por el mismo gate que "Atras".
    BackHandler {
        when {
            pendingStepChange != null -> pendingStepChange = null
            confirmExit -> confirmExit = false
            else -> requestBack()
        }
    }

    // Salir del modo (Atras, Terminar, `onClose` o cualquier descarte de la composicion) no debe dejar
    // ningun aviso vivo ni la cuenta atras pintandose: la cancelacion es idempotente, haya o no algo
    // programado. **No** borra la sesion persistida (una rotacion o una muerte de actividad no deben
    // perderla; solo la salida explicita la borra).
    DisposableEffect(Unit) {
        onDispose { alarm.apply(CookingAlarmPolicy.onExit()) }
    }

    // Latido del reloj **derivado del instante absoluto** (no arma el aviso: eso vive en el gesto de
    // arriba): en cada vuelta se recalculcula `at(elapsedRealtime())` (proyeccion exacta, sin acumular
    // deriva) y se espera hasta el proximo cambio de segundo de la cuenta atras (`remaining % 1000`),
    // de modo que el numero de la pantalla voltea con el cronometro del sistema. La clave es "esta En
    // marcha": el bucle arranca al Iniciar/Reanudar y sale al Pausar/Reiniciar/llegar a cero.
    LaunchedEffect(state.timer.isRunning) {
        while (state.timer.isRunning) {
            state = state.withTimer(state.timer.at(SystemClock.elapsedRealtime()))
            if (!state.timer.isRunning || state.timer.remainingMillis <= 0L) break
            val untilNextSecond = state.timer.remainingMillis % 1_000L
            delay(if (untilNextSecond == 0L) 1_000L else untilNextSecond)
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
                onPrevious = requestPrevious,
                onNext = requestNext,
                onClose = requestFinish,
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
                onBack = requestBack,
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
                        onStart = { onUserAction { s, now -> s.withTimer(s.timer.start(now)) } },
                        onPause = { onUserAction { s, now -> s.withTimer(s.timer.pause(now)) } },
                        onResume = { onUserAction { s, now -> s.withTimer(s.timer.resume(now)) } },
                        onReset = { onUserAction { s, _ -> s.withTimer(s.timer.reset()) } },
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

    // Confirmacion de salida: "Salir" activa la salida explicita de arriba (borra la sesion y cierra el
    // modo; el desmontaje cancela alarma/servicio); "Seguir cocinando" (o el descarte) solo cierra el
    // dialogo, sin tocar nada mas. El reloj sigue corriendo detras del dialogo.
    if (confirmExit) {
        CookingExitDialog(
            onConfirm = {
                confirmExit = false
                exiting = true
            },
            onDismiss = { confirmExit = false },
        )
    }

    // Confirmacion de cambio de paso: "Cambiar de paso" ejecuta la navegacion **aceptada**
    // (`state.next`/`state.previous`: avanza/retrocede y reinicia el temporizador al del paso nuevo) y
    // "Seguir en este paso" (o el descarte) solo cierra el dialogo, sin tocar el paso, el tiempo, el
    // marcado, la sesion ni la alarma. Abrir/cerrar el dialogo no fija `exiting` ni pausa el reloj: la
    // cuenta atras sigue corriendo detras.
    pendingStepChange?.let { point ->
        CookingStepChangeDialog(
            onConfirm = {
                pendingStepChange = null
                onUserAction { s, _ ->
                    when (point) {
                        CookingStepChangePoint.NEXT -> s.next(recipe.steps)
                        CookingStepChangePoint.PREVIOUS -> s.previous(recipe.steps)
                    }
                }
            },
            onDismiss = { pendingStepChange = null },
        )
    }
}

/**
 * Dialogo de confirmacion de **cambio de paso** del Modo cocina: `AlertDialog` de Material 3 con el
 * microcopy de [CookingStepChangeCopy] y los **roles** de `SazonTheme` (sin tokens, formas ni colores
 * nuevos; `DESIGN.md` no cubre dialogos). Una sola accion principal ("Cambiar de paso", en `primary`) y
 * el descarte "Seguir en este paso"; `colorScheme.error` **no** se usa (no hay estilo destructivo).
 */
@Composable
private fun CookingStepChangeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = CookingStepChangeCopy.TITLE) },
        text = { Text(text = CookingStepChangeCopy.MESSAGE) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(text = CookingStepChangeCopy.CONFIRM)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = CookingStepChangeCopy.DISMISS)
            }
        },
    )
}

/**
 * Dialogo de confirmacion de salida del Modo cocina: `AlertDialog` de Material 3 con el microcopy de
 * [CookingExitCopy] y los **roles** de `SazonTheme` (sin tokens, formas ni colores nuevos; `DESIGN.md` no
 * cubre dialogos). Una sola accion principal ("Salir", en `primary`) y el descarte "Seguir cocinando".
 */
@Composable
private fun CookingExitDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = CookingExitCopy.TITLE) },
        text = { Text(text = CookingExitCopy.MESSAGE) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(text = CookingExitCopy.CONFIRM)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = CookingExitCopy.DISMISS)
            }
        },
    )
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
 * los `order` marcados, el estado/millis del temporizador y su **ancla** ([CookingTimerState.endAtElapsedMillis],
 * sin la cual una rotacion volveria a derivar de un `remainingMillis` obsoleto). La `Recipe` **no** se
 * guarda (es un parametro), de modo que la logica pura queda libre de Compose.
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
            state.timer.endAtElapsedMillis?.toString().orEmpty(),
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
                endAtElapsedMillis = saved.getOrNull(6)
                    ?.let { it as String }
                    ?.takeIf { it.isNotEmpty() }
                    ?.toLongOrNull(),
            ),
        )
    },
)
