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

    /**
     * No debe quedar ningun aviso vivo (Pausar, Reiniciar, cambio de paso o salir). **No** se emite al
     * llegar a cero ([CookingTimerStatus.FINISHED]): la alarma pendiente es la que publica el aviso de
     * fin y cancelarla ahi abriria una carrera con el disparo de `AlarmManager`.
     */
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
 * 3. `next` **entra en Terminado** con **transicion real** (`previous != next`: el `tick` que cruza el
 *    cero) -> `None`: la alarma pendiente se deja **viva**, porque es la **unica** productora del aviso
 *    de fin. Si aqui se cancelara, un `tick` de la composicion que llegue a cero antes de que dispare
 *    `AlarmManager` borraria el aviso antes de publicarse (carrera no determinista). El servicio de la
 *    cuenta atras se para **solo**, con su propio `delay`.
 * 4. `next` fuera de Marcha y de Terminado con **transicion real** (`previous != next`: Pausar,
 *    Reiniciar, cambio de paso o salir) -> `Cancel`.
 * 5. `next` **no-op** del temporizador (`previous == next`) -> `None`.
 *
 * Un cambio de paso no necesita una regla propia: [CookingModeState.next]/[CookingModeState.previous]
 * **reinician** el temporizador a Inactivo, asi que la regla 4 ya cancela el aviso del paso anterior.
 * "Reiniciar" desde Terminado (`FINISHED -> IDLE`) tambien cae en la regla 4 y cancela.
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

        next.isFinished && previous != next -> CookingAlarmCommand.None

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

    /**
     * **Retira** de la bandeja el aviso de fin ya mostrado (id fijo `1001`), si lo hubiera. Es un
     * comando **aparte** de [CookingAlarmCommand.Cancel] —que no retira notificaciones mostradas
     * (decision 9 del ADR 0007)— y lo llama **solo** la salida explicita del Modo cocina
     * ("Atras"/"Terminar"), **no** el desmontaje de la composicion: una rotacion no debe borrar el aviso.
     *
     * Contrato: **no lanza** (degradacion silenciosa, ADR 0005); sin permiso o sin notificacion
     * mostrada no hace nada.
     */
    fun dismissFinishNotification()
}

/**
 * Default **sin efecto**: previews, tests y componentes aislados no necesitan Android ni permisos y el
 * modo cocina funciona exactamente igual (el aviso en pantalla "¡Listo!" es de `cooking-mode`).
 */
object NoOpCookingAlarm : CookingAlarm {
    override fun apply(command: CookingAlarmCommand) = Unit

    override fun dismissFinishNotification() = Unit
}
