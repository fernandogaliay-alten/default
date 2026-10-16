package com.sazon.app.data.cooking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Receptor de la **alarma** del temporizador del modo cocina: el sistema lo despierta en el instante de
 * fin (aunque la app este en segundo plano, el proceso muerto o la pantalla bloqueada) y es el
 * **respaldo** del aviso (`docs/specs/cooking-timer-alarm.md`, escenarios 2 y 3). El productor
 * **primario** es el servicio en primer plano, que publica el aviso en el instante exacto; este receptor
 * solo entra cuando el proceso estaba **muerto o congelado** y el servicio no llego a publicar.
 *
 * Al disparar hace tres cosas, en este orden y cada una con su `runCatching` (nunca lanza):
 *
 * 1. **Para el servicio** (`stopService`) y **retira la notificacion persistente** (`cancel(1000)`), de
 *    modo que la retirada de la cuenta atras tambien es exacta cuando el FGS esta congelado o muerto y
 *    el cronometro del sistema no puede seguir contando hacia arriba.
 * 2. Publica el aviso de fin por [CookingNotifications.notifyFinish] con el **sello de fin** que viaja
 *    en el `Intent` (o, si faltara, con el instante actual): la decision pura descarta el fin ya
 *    anunciado por el servicio (mismo sello) y los sellos fuera de la ventana de frescura, asi que
 *    aunque los dos publicadores coincidan hay **una sola** notificacion y **una sola** alerta.
 *
 * No tiene estado, ni `goAsync`, ni `WakeLock`: el sistema mantiene la CPU durante `onReceive` y
 * publicar una notificacion es instantaneo. El id es **fijo**
 * ([CookingNotifications.NOTIFICATION_ID]), de modo que "no se duplica" se cumple por construccion.
 *
 * El contenido es **generico** ([CookingNotifications.TITLE]/[CookingNotifications.TEXT]): el receptor
 * no lee la receta, solo el escalar del sello, asi que no puede filtrar datos. El `PendingIntent` de
 * contenido lo construye [CookingNotifications.contentIntent] —**compartido** con la notificacion
 * persistente del servicio—: abre `MainActivity` con `FLAG_ACTIVITY_NEW_TASK or
 * FLAG_ACTIVITY_SINGLE_TOP` y le pasa **solo** [CookingNotifications.EXTRA_RESUME_COOKING] (`true`),
 * para que el toque vuelva al paso exacto (`docs/specs/cooking-timer-foreground-service.md`, escenarios
 * 5 y 6).
 *
 * Se declara en el manifiesto con `android:exported="false"`: el `PendingIntent` que lo lanza es
 * explicito y de la misma app. **Nunca lanza**: si el permiso `POST_NOTIFICATIONS` esta denegado o
 * falla cualquier paso, no publica nada.
 */
class CookingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val finishWallClockMillis = intent
            .getLongExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, 0L)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        runCatching { context.stopService(Intent(context, CookingTimerService::class.java)) }
        runCatching {
            NotificationManagerCompat.from(context)
                .cancel(CookingNotifications.ONGOING_NOTIFICATION_ID)
        }
        runCatching {
            CookingNotifications.notifyFinish(
                context,
                finishWallClockMillis = finishWallClockMillis,
                contentIntent = CookingNotifications.contentIntent(context, CONTENT_REQUEST_CODE),
            )
        }
    }

    companion object {

        /** Request code del `PendingIntent` de contenido (no colisiona con el de la alarma). */
        internal const val CONTENT_REQUEST_CODE = 2
    }
}
