package com.sazon.app.data.cooking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servicio en **primer plano** que pinta la **cuenta atras** del temporizador del Modo cocina fuera de
 * la app (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1, 2 y 4) y que, desde la
 * correctiva `cooking-timer-notification-reliability`, es el **productor primario** del aviso de fin
 * "¡Listo!" (la alarma exacta queda como **respaldo** para el caso de proceso muerto).
 *
 * Mantiene viva la notificacion **persistente** (`ONGOING_NOTIFICATION_ID`) mientras corre la cuenta
 * atras; la cuenta atras que ve el usuario la **renderiza el sistema** con el cronometro descendente de
 * la notificacion, asi que no hay ni una publicacion por segundo ni un bucle de latidos.
 *
 * - `onStartCommand` lee **solo** dos extras: `EXTRA_END_AT_ELAPSED_MILLIS` (instante de fin en
 *   `ELAPSED_REALTIME`, la misma base que la alarma) y [CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS]
 *   (el **sello de fin** compartido, que viaja tambien en el `PendingIntent` de la alarma). Si el
 *   instante de fin **ya paso**, **no** pinta nada (no hay cronometro caducado ni cuenta atras en
 *   negativo) y se apaga; si no, llama a `startForeground(...)` **inmediato** (dentro de los 5 s que
 *   exige el framework) con el `when` **recalculado en esa misma lectura** y programa el **unico**
 *   despertar.
 * - Al vencer, **publica** la `1001` con el sello (el aviso puntual, sin depender de que el fabricante
 *   difiera la alarma), **retira** su notificacion persistente (`STOP_FOREGROUND_REMOVE`) y se detiene.
 *   Republicar no duplica ni vuelve a alertar: lo garantiza [CookingFinishPublication] sobre el sello.
 * - Devuelve **`START_NOT_STICKY`**: sin intent no se recrea. Si falta el extra (o el proceso se recrea
 *   sin el), se apaga sin mas.
 * - **Degradacion silenciosa** (ADR 0005): si `startForeground` no se puede completar (p. ej. el sistema
 *   no permite arrancar en primer plano o `POST_NOTIFICATIONS` esta denegado), el servicio se detiene
 *   sin lanzar; el Modo cocina, la cuenta atras en pantalla y el "¡Listo!" siguen intactos.
 *
 * No usa `ServiceCompat.startForeground` (no existe en `androidx.core` 1.10.1): el `startForeground(id,
 * notification)` del framework aplica el tipo `specialUse` declarado en el manifiesto, que es lo que se
 * quiere.
 */
class CookingTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var stopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        // Defensivo: el canal ya lo crea `MainActivity` al arrancar; asegurarlo aqui cubre el caso de un
        // proceso recreado solo para el servicio. Es idempotente y no lanza.
        runCatching { CookingNotifications.ensureChannel(this) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || !intent.hasExtra(EXTRA_END_AT_ELAPSED_MILLIS)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val endAtElapsedMillis = intent.getLongExtra(EXTRA_END_AT_ELAPSED_MILLIS, 0L)
        val finishWallClockMillis = intent
            .getLongExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, 0L)
            .takeIf { it > 0L }
            ?: wallClockEnd(endAtElapsedMillis)
        if (endAtElapsedMillis <= SystemClock.elapsedRealtime()) {
            // El fin ya paso: no se pinta un cronometro caducado (nunca en negativo) y se apaga.
            stopSelf()
            return START_NOT_STICKY
        }
        val foreground = runCatching {
            startForeground(
                CookingNotifications.ONGOING_NOTIFICATION_ID,
                CookingNotifications.ongoing(
                    context = this,
                    endWallClockMillis = wallClockEnd(endAtElapsedMillis),
                    contentIntent = CookingNotifications.contentIntent(
                        this,
                        CookingAlarmReceiver.CONTENT_REQUEST_CODE,
                    ),
                ),
            )
        }.isSuccess
        if (!foreground) {
            // Sin notificacion persistente el servicio no tiene sentido: se apaga en silencio.
            stopSelf()
            return START_NOT_STICKY
        }
        scheduleFinish(endAtElapsedMillis, finishWallClockMillis)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Un **unico** despertar: espera lo que queda hasta el instante de fin (base **monotona**, la que
     * decide la extincion real) y entonces, en este orden, **publica el aviso** `1001` con el sello
     * compartido (el productor primario, puntual), retira la notificacion persistente
     * (`STOP_FOREGROUND_REMOVE`) y se detiene.
     */
    private fun scheduleFinish(endAtElapsedMillis: Long, finishWallClockMillis: Long) {
        stopJob?.cancel()
        stopJob = scope.launch {
            delay((endAtElapsedMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            runCatching {
                CookingNotifications.notifyFinish(
                    context = this@CookingTimerService,
                    finishWallClockMillis = finishWallClockMillis,
                    contentIntent = CookingNotifications.contentIntent(
                        this@CookingTimerService,
                        CookingAlarmReceiver.CONTENT_REQUEST_CODE,
                    ),
                )
            }
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
        }
    }

    /**
     * Instante de fin en **hora de pared** para el cronometro de la notificacion (que se expresa asi):
     * `now` de pared + lo que queda segun el reloj monotono. Se **recalcula en la publicacion**, de modo
     * que la cuenta atras no hereda un `when` obsoleto. El fin real lo decide siempre
     * `endAtElapsedMillis`; esto es solo lo que pinta el sistema (ver Unknowns de la spec: un cambio de
     * reloj mientras corre puede desviar la cuenta atras mostrada).
     */
    private fun wallClockEnd(endAtElapsedMillis: Long): Long =
        System.currentTimeMillis() + (endAtElapsedMillis - SystemClock.elapsedRealtime())

    companion object {

        /** Instante de fin del temporizador en `ELAPSED_REALTIME` (base monotona). */
        const val EXTRA_END_AT_ELAPSED_MILLIS = "com.sazon.app.extra.cooking.END_AT_ELAPSED_MILLIS"

        /**
         * `Intent` **explicito** de arranque del servicio: lleva **solo** los dos escalares del
         * temporizador (el instante de fin monotono y el sello de fin en hora de pared); ni la receta,
         * ni el paso, ni la clave de IA cruzan al sistema.
         */
        fun intent(
            context: Context,
            endAtElapsedMillis: Long,
            finishWallClockMillis: Long,
        ): Intent = Intent(context, CookingTimerService::class.java)
            .putExtra(EXTRA_END_AT_ELAPSED_MILLIS, endAtElapsedMillis)
            .putExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, finishWallClockMillis)
    }
}
