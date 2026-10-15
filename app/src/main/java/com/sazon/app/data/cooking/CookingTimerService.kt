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
 * la app (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1, 2 y 4).
 *
 * Es un **pintor**: no cambia ningun estado, no lee la sesion persistida y **no** publica el "¡Listo!"
 * (eso sigue siendo de la alarma exacta de `cooking-timer-alarm`, unica duena del aviso de fin). Su
 * unico trabajo es mantener viva la notificacion **persistente** (`ONGOING_NOTIFICATION_ID`) mientras
 * corre la cuenta atras; la cuenta atras que ve el usuario la **renderiza el sistema** con el cronometro
 * descendente de la notificacion, asi que no hay ni una publicacion por segundo ni un bucle de `tick`.
 *
 * - `onStartCommand` lee **solo** `EXTRA_END_AT_ELAPSED_MILLIS` (instante de fin en `ELAPSED_REALTIME`,
 *   la misma base que la alarma aceptada), llama a `startForeground(...)` **inmediato** (dentro de los
 *   5 s que exige el framework) y programa el **unico** despertar: un `delay` hasta ese instante tras el
 *   cual retira la notificacion (`STOP_FOREGROUND_REMOVE`) y se detiene.
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
        scheduleStop(endAtElapsedMillis)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Un **unico** despertar: espera lo que queda hasta el instante de fin (base **monotona**, la que
     * decide la extincion real) y entonces retira la notificacion persistente y se detiene.
     */
    private fun scheduleStop(endAtElapsedMillis: Long) {
        stopJob?.cancel()
        stopJob = scope.launch {
            delay((endAtElapsedMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
        }
    }

    /**
     * Instante de fin en **hora de pared** para el cronometro de la notificacion (que se expresa asi):
     * `now` de pared + lo que queda segun el reloj monotono. El fin real lo decide siempre
     * `endAtElapsedMillis`; esto es solo lo que pinta el sistema (ver Unknowns de la spec: un cambio de
     * reloj mientras corre puede desviar la cuenta atras mostrada).
     */
    private fun wallClockEnd(endAtElapsedMillis: Long): Long =
        System.currentTimeMillis() + (endAtElapsedMillis - SystemClock.elapsedRealtime())

    companion object {

        /** Unico dato que viaja al servicio: el instante de fin en `ELAPSED_REALTIME`. */
        const val EXTRA_END_AT_ELAPSED_MILLIS = "com.sazon.app.extra.cooking.END_AT_ELAPSED_MILLIS"

        /**
         * `Intent` **explicito** de arranque del servicio: lleva **solo** el instante de fin (ni la
         * receta, ni el paso, ni la clave de IA cruzan al sistema).
         */
        fun intent(context: Context, endAtElapsedMillis: Long): Intent =
            Intent(context, CookingTimerService::class.java)
                .putExtra(EXTRA_END_AT_ELAPSED_MILLIS, endAtElapsedMillis)
    }
}
