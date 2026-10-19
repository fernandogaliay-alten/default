package com.sazon.app.data.cooking

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sazon.app.ui.cooking.CookingAlarm
import com.sazon.app.ui.cooking.CookingAlarmCommand
import com.sazon.app.ui.cooking.CookingStepLabel

/**
 * Adaptador Android de la costura [CookingAlarm]: programa en `AlarmManager` la alarma de **un solo
 * disparo** que sirve de **respaldo** del aviso de fin y la cancela cuando deja de tener sentido
 * (`docs/specs/cooking-timer-alarm.md`, escenarios 1, 4 y 5). Ademas arranca y para el **servicio en
 * primer plano** que pinta la cuenta atras y que, desde `cooking-timer-notification-reliability`, es el
 * **productor primario** del "¡Listo!" (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1
 * y 4), con la **misma** idempotencia: el comando ya transporta exactamente las dos cosas.
 *
 * - La base de tiempo de la alarma es **`ELAPSED_REALTIME_WAKEUP`**: el instante que llega en
 *   [CookingAlarmCommand.Schedule] es el **ancla** del temporizador (o, si no la tiene, `elapsedRealtime
 *   + remaining`; el reloj lo lee la capa Compose, nunca la politica pura).
 * - El **sello de fin** en hora de pared se calcula **una vez** aqui (`currentTimeMillis() + (at -
 *   elapsedRealtime())`) y viaja en **los dos** `Intent`: el del `PendingIntent` de la alarma y el de
 *   arranque del servicio. Es lo que hace idempotente el aviso con dos publicadores (mismo fin -> una
 *   sola notificacion y una sola alerta).
 * - El `requestCode` fijo implica que **solo hay una alarma viva**: cada programacion **reemplaza** la
 *   anterior (`FLAG_UPDATE_CURRENT`, que tambien actualiza el sello), de modo que no se acumulan avisos
 *   de pasos distintos.
 * - Si `canScheduleExactAlarms()` es `true` (framework, API 31+; `USE_EXACT_ALARM` es un permiso
 *   **normal** concedido al instalar) usa `setExactAndAllowWhileIdle` — exacta incluso en Doze —; si
 *   no, **degrada** a `setAndAllowWhileIdle` (inexacta: el aviso puede llegar tarde, pero llega).
 *   Nunca `setRepeating`/`setInexactRepeating`.
 * - `Schedule` arranca tambien el servicio en primer plano (`startForegroundService` con
 *   [CookingTimerService.intent], que lleva el instante de fin, el sello y la **etiqueta del paso**) y
 *   `Cancel` lo para (`stopService`), de modo que la notificacion persistente existe **solo** En marcha. Las dos patas son
 *   **independientes**: si una falla, la otra se intenta igual (cada una con su `runCatching`).
 *   Arrancar el servicio siempre ocurre desde una interaccion del usuario con la app en primer plano
 *   ("Iniciar"/"Reanudar"/"Siguiente"/"Anterior"), asi que no aplica la restriccion de arranque en
 *   segundo plano.
 * - `Cancel` cancela **solo** la alarma pendiente con el **mismo** `PendingIntent` (mismo `requestCode`):
 *   **no** retira una notificacion ya mostrada (decision 9 aceptada). Detener el servicio si retira su
 *   notificacion persistente, que es justo lo que se quiere al Pausar/Reiniciar/cambiar de paso/salir.
 * - [dismissFinishNotification] es el comando **aparte** que si retira el aviso de fin ya mostrado
 *   (`Cancel(1001)`); lo llama **solo** la salida explicita del Modo cocina, nunca el desmontaje de la
 *   composicion (una rotacion no debe borrar el aviso).
 * - **Degradacion silenciosa** (ADR 0005): todo va envuelto en `runCatching`; un `SecurityException`,
 *   un `AlarmManager` ausente o un paquete sin launch intent no pueden tumbar el modo cocina.
 */
class AndroidCookingAlarm(context: Context) : CookingAlarm {

    private val appContext: Context = context.applicationContext

    private val alarmManager: AlarmManager? =
        appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun apply(command: CookingAlarmCommand) {
        when (command) {
            is CookingAlarmCommand.Schedule -> schedule(command.triggerAtMillis, command.stepLabel)
            CookingAlarmCommand.Cancel -> cancel()
            CookingAlarmCommand.None -> Unit
        }
    }

    /**
     * Retira de la bandeja el aviso de fin ya mostrado (id fijo
     * [CookingNotifications.NOTIFICATION_ID]) y **no** toca la alarma pendiente ni el servicio. Es
     * **independiente** de [cancel] (decision 9 del ADR 0007: `Cancel` no retira notificaciones
     * mostradas) y lo llama **solo** la salida explicita del Modo cocina; **nunca lanza**.
     */
    override fun dismissFinishNotification() {
        runCatching {
            NotificationManagerCompat.from(appContext)
                .cancel(CookingNotifications.NOTIFICATION_ID)
        }
    }

    /**
     * Calcula **una vez** el sello de fin compartido, programa la alarma exacta para [at] (o la
     * inexacta de respaldo) con ese sello y arranca el servicio en primer plano con el instante de fin,
     * el sello **y** la etiqueta del paso ([stepLabel], opcional); nunca lanza.
     *
     * La etiqueta viaja **solo** al `Intent` del servicio: el `PendingIntent` de la alarma **no** la
     * lleva (no la necesita: el aviso de fin `1001` es generico y no cambia), de modo que el camino del
     * respaldo con el proceso muerto sigue sin exponer nada nuevo.
     */
    private fun schedule(at: Long, stepLabel: CookingStepLabel?) {
        val finishWallClockMillis = System.currentTimeMillis() + (at - SystemClock.elapsedRealtime())
        val pendingIntent = broadcastPendingIntent(finishWallClockMillis)
        runCatching {
            val manager = alarmManager ?: return@runCatching
            val intent = pendingIntent ?: return@runCatching
            if (manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    at,
                    intent,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    at,
                    intent,
                )
            }
        }
        // Pata del servicio (productor primario del aviso): independiente de la alarma (si esta
        // fallara, la cuenta atras y el aviso puntual siguen).
        runCatching {
            ContextCompat.startForegroundService(
                appContext,
                CookingTimerService.intent(appContext, at, finishWallClockMillis, stepLabel),
            )
        }
    }

    /**
     * Cancela la alarma pendiente (idempotente) y **para** el servicio en primer plano; nunca lanza y
     * **no** toca la notificacion del aviso de fin ya mostrada.
     */
    private fun cancel() {
        runCatching {
            val manager = alarmManager ?: return@runCatching
            val intent = cancelPendingBroadcast() ?: return@runCatching
            manager.cancel(intent)
        }
        runCatching {
            appContext.stopService(Intent(appContext, CookingTimerService::class.java))
        }
    }

    /**
     * `PendingIntent` **ya existente** de la alarma (o `null` si no hay ninguno): `Cancel` no debe
     * **reescribir** el sello de un `PendingIntent` vivo (el receiver lo lee si la alarma llegara a
     * dispararse en la carrera de la cancelacion), asi que se busca **sin** crear ni actualizar nada
     * (`FLAG_NO_CREATE`). Si el sistema no lo devolviera (registro ya liberado), degrada al camino
     * aceptado —crear/actualizar por identidad, sin extras— para que la cancelacion siga siendo
     * efectiva: `AlarmManager.cancel` casa por identidad (`requestCode` + `Intent.filterEquals`), no por
     * los extras.
     */
    private fun cancelPendingBroadcast(): PendingIntent? =
        existingBroadcastPendingIntent() ?: broadcastPendingIntent(finishWallClockMillis = null)

    /**
     * `PendingIntent` **explicito** de broadcast hacia [CookingAlarmReceiver] con el sello del fin
     * ([CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS]): un **escalar**, nunca datos de la receta,
     * de modo que el contenido del aviso sigue siendo generico y solo lo decide [CookingNotifications].
     * `FLAG_UPDATE_CURRENT` reescribe el sello al reprogramar. Con [finishWallClockMillis] `null` no se
     * anade extra (camino de cancelacion).
     */
    private fun broadcastPendingIntent(finishWallClockMillis: Long?): PendingIntent? = runCatching {
        val intent = Intent(appContext, CookingAlarmReceiver::class.java)
        if (finishWallClockMillis != null) {
            intent.putExtra(
                CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS,
                finishWallClockMillis,
            )
        }
        PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    /**
     * `PendingIntent` **ya existente** de la alarma (o `null` si no hay ninguna programada): se busca
     * con `FLAG_NO_CREATE`, que no crea ni actualiza nada.
     */
    private fun existingBroadcastPendingIntent(): PendingIntent? = runCatching {
        PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            Intent(appContext, CookingAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    companion object {

        /** Request code unico de la alarma: una sola viva; cada programacion reemplaza la anterior. */
        private const val ALARM_REQUEST_CODE = 1
    }
}
