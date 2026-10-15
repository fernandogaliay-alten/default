package com.sazon.app.data.cooking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receptor de la **alarma** del temporizador del modo cocina: el sistema lo despierta en el instante de
 * fin (aunque la app este en segundo plano, el proceso muerto o la pantalla bloqueada) y publica la
 * notificacion del aviso (`docs/specs/cooking-timer-alarm.md`, escenarios 2 y 3).
 *
 * No tiene estado, ni `goAsync`, ni `WakeLock`, ni servicio: el sistema mantiene la CPU durante
 * `onReceive` y publicar una notificacion es instantaneo. Es el **unico** origen del aviso, con id
 * fijo ([CookingNotifications.NOTIFICATION_ID]), de modo que "no se duplica" se cumple por
 * construccion.
 *
 * El contenido es **generico** ([CookingNotifications.TITLE]/[CookingNotifications.TEXT]): el receptor
 * no lee la receta ni ningun extra, asi que no puede filtrar datos. El `PendingIntent` de contenido lo
 * construye [CookingNotifications.contentIntent] —**compartido** con la notificacion persistente del
 * servicio en primer plano—: abre `MainActivity` con `FLAG_ACTIVITY_NEW_TASK or
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
        runCatching {
            CookingNotifications.notify(
                context,
                CookingNotifications.contentIntent(context, CONTENT_REQUEST_CODE),
            )
        }
    }

    companion object {

        /** Request code del `PendingIntent` de contenido (no colisiona con el de la alarma). */
        internal const val CONTENT_REQUEST_CODE = 2
    }
}
