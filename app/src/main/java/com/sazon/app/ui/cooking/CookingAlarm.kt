package com.sazon.app.ui.cooking

/**
 * Decision del **aviso de sistema** de una transicion del temporizador del modo cocina
 * (`docs/specs/cooking-timer-alarm.md`): lo **unico** que la capa Android necesita saber para dejar el
 * aviso en el estado correcto.
 *
 * Es una decision **pura**, no una orden de plataforma: quien programa y cancela es el adaptador
 * Android que vive detras de la costura [CookingAlarm].
 */
sealed interface CookingAlarmCommand {

    /**
     * Hay que programar (o **reemplazar**) el aviso para el instante [triggerAtMillis] (el fin de la
     * cuenta atras): `now + remaining` del temporizador que acaba de arrancar.
     */
    data class Schedule(val triggerAtMillis: Long) : CookingAlarmCommand

    /** No debe quedar ningun aviso vivo (Pausar, Reiniciar, cambio de paso, llegar a cero o salir). */
    data object Cancel : CookingAlarmCommand

    /** La alarma ya esta donde debe: no se toca (ticks, no-ops y casos residuales). */
    data object None : CookingAlarmCommand
}

/**
 * Politica **pura** del aviso de fin del temporizador del modo cocina (sin Compose ni Android;
 * testeable en JVM, patron [CookingTimerState]).
 *
 * Es **determinista** y **nunca lee el reloj**: el instante entra por parametro (`nowMillis`), igual
 * que el tiempo de [CookingTimerState.tick] entra por `deltaMillis`.
 *
 * Reglas de [onTimerChange], evaluadas en este orden:
 * 1. **Iniciar/Reanudar** (`next.isRunning && !previous.isRunning` y `next.remainingMillis > 0`) ->
 *    `Schedule(nowMillis + next.remainingMillis)`: el instante de fin.
 * 2. Cualquier otro caso con `next` En marcha (los **ticks**, el doble "Iniciar" no-op y el caso
 *    defensivo `remainingMillis <= 0`) -> `None`: **no** se reprograma en cada segundo (seria el bug
 *    clasico de "una alarma por tick").
 * 3. `next` fuera de Marcha con **transicion real** (`previous != next`: Pausar, Reiniciar, cambio de
 *    paso o llegar a cero) -> `Cancel`.
 * 4. `next` fuera de Marcha y **no-op** del temporizador (`previous == next`) -> `None`.
 *
 * Un cambio de paso no necesita una regla propia: [CookingModeState.next]/[CookingModeState.previous]
 * **reinician** el temporizador a Inactivo, asi que la regla 3 ya cancela el aviso del paso anterior.
 */
object CookingAlarmPolicy {

    /** Decide el comando de una transicion del temporizador; `nowMillis` se inyecta (nunca el reloj). */
    fun onTimerChange(
        previous: CookingTimerState,
        next: CookingTimerState,
        nowMillis: Long,
    ): CookingAlarmCommand = when {
        next.isRunning && !previous.isRunning && next.remainingMillis > 0 ->
            CookingAlarmCommand.Schedule(nowMillis + next.remainingMillis)

        next.isRunning -> CookingAlarmCommand.None

        previous != next -> CookingAlarmCommand.Cancel

        else -> CookingAlarmCommand.None
    }

    /**
     * Salir del modo cocina ("Atras"/"Terminar"/`onClose` y descarte de la composicion): no debe quedar
     * ningun aviso. La cancelacion es **idempotente**: da igual si habia algo programado.
     */
    fun onExit(): CookingAlarmCommand = CookingAlarmCommand.Cancel
}

/**
 * Costura del **aviso de sistema** del temporizador del modo cocina: aplica el [CookingAlarmCommand]
 * que decide [CookingAlarmPolicy].
 *
 * Predominan dos reglas: es **pura** en `ui.cooking` (sin `android.*` ni `androidx.*`: los
 * adaptadores de plataforma viven en `data.cooking`) y su implementacion por defecto es **no-op**, de
 * modo que previews, tests y composiciones sin cableado compilan y no hacen nada.
 *
 * Contrato: **degradacion silenciosa** (ADR 0005). Ninguna implementacion lanza; un fallo o un permiso
 * denegado no pueden romper la UI ni bloquear el temporizador.
 */
interface CookingAlarm {

    /** Deja el aviso en el estado que pide [command] (programar, cancelar o no tocar nada). */
    fun apply(command: CookingAlarmCommand)
}

/**
 * Default **sin efecto**: previews, tests y componentes aislados no necesitan Android ni permisos y el
 * modo cocina funciona exactamente igual (el aviso en pantalla "¡Listo!" es de `cooking-mode`).
 */
object NoOpCookingAlarm : CookingAlarm {
    override fun apply(command: CookingAlarmCommand) = Unit
}
