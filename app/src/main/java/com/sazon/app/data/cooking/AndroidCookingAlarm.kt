package com.sazon.app.data.cooking

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sazon.app.ui.cooking.CookingAlarm
import com.sazon.app.ui.cooking.CookingAlarmCommand

/**
 * Adaptador Android de la costura [CookingAlarm]: programa en `AlarmManager` la alarma de **un solo
 * disparo** del aviso y la cancela cuando deja de tener sentido
 * (`docs/specs/cooking-timer-alarm.md`, escenarios 1, 4 y 5). Ademas arranca y para el **servicio en
 * primer plano** que pinta la cuenta atras
 * (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1 y 4), con la **misma**
 * idempotencia: el comando ya transporta exactamente las dos cosas.
 *
 * - La base de tiempo es **`ELAPSED_REALTIME_WAKEUP`**: el instante que llega en
 *   [CookingAlarmCommand.Schedule] es `SystemClock.elapsedRealtime()` + lo que quedaba de cuenta
 *   atras (el reloj lo lee la capa Compose, nunca la politica pura).
 * - El `requestCode` fijo implica que **solo hay una alarma viva**: cada programacion **reemplaza** la
 *   anterior (`FLAG_UPDATE_CURRENT`), de modo que no se acumulan avisos de pasos distintos.
 * - Si `canScheduleExactAlarms()` es `true` (framework, API 31+; `USE_EXACT_ALARM` es un permiso
 *   **normal** concedido al instalar) usa `setExactAndAllowWhileIdle` — exacta incluso en Doze —; si
 *   no, **degrada** a `setAndAllowWhileIdle` (inexacta: el aviso puede llegar tarde, pero llega).
 *   Nunca `setRepeating`/`setInexactRepeating`.
 * - `Schedule` arranca tambien el servicio en primer plano (`startForegroundService` con
 *   [CookingTimerService.intent], que solo lleva el instante de fin) y `Cancel` lo para
 *   (`stopService`), de modo que la notificacion persistente existe **solo** En marcha. Las dos patas
 *   son **independientes**: si una falla, la otra se intenta igual (cada una con su `runCatching`).
 *   Arrancar el servicio siempre ocurre desde una interaccion del usuario con la app en primer plano
 *   ("Iniciar"/"Reanudar"), asi que no aplica la restriccion de arranque en segundo plano.
 * - `Cancel` cancela **solo** la alarma pendiente con el **mismo** `PendingIntent` (mismo `requestCode`
 *   y mismo `Intent`): **no** retira una notificacion ya mostrada (decision 9 aceptada). Detener el
 *   servicio si retira su notificacion persistente, que es justo lo que se quiere al Pausar/Reiniciar/
 *   cambiar de paso/salir.
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
            is CookingAlarmCommand.Schedule -> schedule(command.triggerAtMillis)
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
     * Programa la alarma exacta para [at] (o la inexacta de respaldo) **y** arranca el servicio en
     * primer plano con el mismo instante de fin; nunca lanza.
     */
    private fun schedule(at: Long) {
        runCatching {
            val manager = alarmManager ?: return@runCatching
            val pendingIntent = broadcastPendingIntent() ?: return@runCatching
            if (manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    at,
                    pendingIntent,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    at,
                    pendingIntent,
                )
            }
        }
        // Pata del servicio: independiente de la alarma (si esta fallara, la cuenta atras sigue).
        runCatching {
            ContextCompat.startForegroundService(appContext, CookingTimerService.intent(appContext, at))
        }
    }

    /**
     * Cancela la alarma pendiente (idempotente) y **para** el servicio en primer plano; nunca lanza y
     * **no** toca la notificacion del aviso de fin ya mostrada.
     */
    private fun cancel() {
        runCatching {
            val manager = alarmManager ?: return@runCatching
            val pendingIntent = broadcastPendingIntent() ?: return@runCatching
            manager.cancel(pendingIntent)
        }
        runCatching {
            appContext.stopService(Intent(appContext, CookingTimerService::class.java))
        }
    }

    /**
     * `PendingIntent` **explicito** de broadcast hacia [CookingAlarmReceiver]. No lleva extras: el
     * contenido del aviso es **generico** y solo lo decide [CookingNotifications], de modo que la
     * receta no cruza al servicio de alarmas del sistema.
     */
    private fun broadcastPendingIntent(): PendingIntent? = runCatching {
        PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            Intent(appContext, CookingAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    companion object {

        /** Request code unico de la alarma: una sola viva; cada programacion reemplaza la anterior. */
        private const val ALARM_REQUEST_CODE = 1
    }
}
