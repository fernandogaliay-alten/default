package com.sazon.app.ui.cooking

/**
 * Estado **inyectado** de la peticion de `POST_NOTIFICATIONS` en el Modo cocina (feature
 * `cooking-timer-alarm-permission-context`, escenarios 1-8 de su spec): el permiso actual y el instante
 * del ultimo intento de esta sesion.
 *
 * Es un dato **puro**: no lee Android ni el reloj. Quien lo construye (el adaptador de plataforma) le
 * entrega el permiso ya resuelto y, si hubo intento, el instante medido fuera.
 *
 * `askedAtMillis == null` significa "esta sesion no ha pedido todavia"; no-nulo, "ya hubo un intento".
 * Hoy solo distingue esos dos casos, pero guardar el **instante** deja la puerta abierta a un cooldown
 * futuro sin cambiar el contrato.
 */
data class NotificationPermissionState(
    val granted: Boolean,
    val askedAtMillis: Long? = null,
) {

    /**
     * Registra el intento con el instante **inyectado** (nunca lee el reloj): devuelve una copia, no
     * muta el receptor.
     */
    fun asked(atMillis: Long): NotificationPermissionState = copy(askedAtMillis = atMillis)
}

/** Que debe hacer la capa Compose con la peticion del permiso en una transicion del temporizador. */
sealed interface NotificationPermissionCommand {

    /** Mostrar el **rationale** y, solo si el usuario acepta, lanzar el dialogo del sistema. */
    data object AskWithRationale : NotificationPermissionCommand

    /** Nada que hacer: no se muestra ni el rationale ni el dialogo del sistema. */
    data object None : NotificationPermissionCommand
}

/**
 * Microcopy del **rationale** del permiso (constantes puras: sin recursos ni `strings.xml`, igual que
 * el contenido del aviso aceptado en `CookingNotifications`).
 */
object NotificationPermissionCopy {

    /** Titulo del rationale. */
    const val TITLE = "Avisos del temporizador"

    /** Cuerpo del rationale: explica para que sirve el permiso antes del dialogo del sistema. */
    const val MESSAGE =
        "Para avisarte cuando termine el paso, aunque no estes en la app, Sazon necesita permiso " +
            "para enviarte notificaciones."

    /** Accion principal del rationale: lanza el dialogo del sistema. */
    const val CONFIRM = "Continuar"

    /** Descarte del rationale: no se pide el permiso y no se vuelve a molestar en la sesion. */
    const val DISMISS = "Ahora no"
}

/**
 * Politica **pura** de la peticion del permiso de notificaciones (sin Compose ni Android; testeable en
 * JVM, patron [CookingAlarmPolicy]).
 *
 * Es **determinista** y **nunca lee el reloj**: el permiso, la memoria de sesion y (en el adaptador) el
 * instante entran inyectados. La **unica** arista que dispara la peticion es la de "arrancar el
 * temporizador" (`!previous.isRunning && next.isRunning`), la misma que ya dispara el aviso de fin: los
 * ticks, los no-ops del temporizador, Pausar, Reiniciar, cambiar de paso, llegar a cero y salir
 * devuelven [NotificationPermissionCommand.None], y tambien la **primera** composicion (donde
 * `previous == next == IDLE`, que no es una transicion real).
 */
object NotificationPermissionPolicy {

    /**
     * Decide si hay que mostrar el rationale en la transicion `previous -> next` con el estado
     * [state] inyectado. Orden de reglas: no es "arrancar" -> [NotificationPermissionCommand.None];
     * permiso ya concedido -> `None`; ya pedido en la sesion -> `None`; en cualquier otro caso ->
     * [NotificationPermissionCommand.AskWithRationale].
     */
    fun onTimerChange(
        previous: CookingTimerState,
        next: CookingTimerState,
        state: NotificationPermissionState,
    ): NotificationPermissionCommand = when {
        !(next.isRunning && !previous.isRunning) -> NotificationPermissionCommand.None

        state.granted -> NotificationPermissionCommand.None

        state.askedAtMillis != null -> NotificationPermissionCommand.None

        else -> NotificationPermissionCommand.AskWithRationale
    }
}
