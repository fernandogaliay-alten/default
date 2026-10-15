package com.sazon.app.data.cooking

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sazon.app.ui.cooking.CookingAlarm
import com.sazon.app.ui.cooking.CookingAlarmCommand

/**
 * Adaptador Android de la costura [CookingAlarm]: programa en `AlarmManager` la alarma de **un solo
 * disparo** del aviso y la cancela cuando deja de tener sentido
 * (`docs/specs/cooking-timer-alarm.md`, escenarios 1, 4 y 5).
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
 * - `Cancel` cancela **solo** la alarma pendiente con el **mismo** `PendingIntent` (mismo `requestCode`
 *   y mismo `Intent`): **no** retira una notificacion ya mostrada.
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

    /** Programa la alarma exacta para [at] (o la inexacta de respaldo); nunca lanza. */
    private fun schedule(at: Long) {
        runCatching {
            val manager = alarmManager ?: return
            val pendingIntent = broadcastPendingIntent() ?: return
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
    }

    /** Cancela la alarma pendiente (idempotente); nunca lanza y **no** toca la notificacion. */
    private fun cancel() {
        runCatching {
            val manager = alarmManager ?: return
            val pendingIntent = broadcastPendingIntent() ?: return
            manager.cancel(pendingIntent)
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
