package com.sazon.app.data.cooking

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sazon.app.MainActivity

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
 * no lee la receta ni ningun extra, asi que no puede filtrar datos. El toque en la notificacion abre
 * [MainActivity] con `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP`, de modo que **reutiliza** la
 * actividad viva (que conserva el modo cocina del paso terminado) en lugar de recrearla.
 *
 * Se declara en el manifiesto con `android:exported="false"`: el `PendingIntent` que lo lanza es
 * explicito y de la misma app. **Nunca lanza**: si el permiso `POST_NOTIFICATIONS` esta denegado o
 * falla cualquier paso, no publica nada.
 */
class CookingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            val contentIntent = PendingIntent.getActivity(
                context,
                CONTENT_REQUEST_CODE,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            CookingNotifications.notify(context, contentIntent)
        }
    }

    companion object {

        /** Request code del `PendingIntent` de contenido (no colisiona con el de la alarma). */
        internal const val CONTENT_REQUEST_CODE = 2
    }
}
