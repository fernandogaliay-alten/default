package com.sazon.app.data.cooking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sazon.app.R

/**
 * Canal y notificacion del **aviso de fin del temporizador** del modo cocina (capa Android;
 * `docs/specs/cooking-timer-alarm.md`, escenarios 2, 3 y 6).
 *
 * El contenido es **generico** por construccion: [notification] **no recibe** la receta, asi que no
 * puede exponer la descripcion del paso, los ingredientes ni las notas. Es una notificacion de
 * sistema que no forma parte de ninguna pantalla de la app.
 *
 * **Degradacion silenciosa** (ADR 0005): si `POST_NOTIFICATIONS` esta denegado (o las notificaciones
 * estan apagadas) no se publica nada y **no** hay excepcion; el modo cocina y su "¡Listo!" en pantalla
 * siguen intactos.
 */
object CookingNotifications {

    /** Id del canal propio del aviso. Sus atributos son **inmutables** una vez creado. */
    const val CHANNEL_ID = "cooking_timer"

    /** Nombre visible del canal en los ajustes del sistema. */
    const val CHANNEL_NAME = "Temporizador de cocina"

    /** Descripcion visible del canal en los ajustes del sistema. */
    const val CHANNEL_DESCRIPTION = "Aviso cuando termina el temporizador de un paso"

    /** Id **fijo** de la notificacion: publicarla otra vez la **reemplaza**, nunca la duplica. */
    const val NOTIFICATION_ID = 1001

    /** Titulo generico del aviso: no nombra la receta ni el paso. */
    const val TITLE = "¡Listo! El paso ha terminado"

    /** Texto generico del aviso: no nombra la receta ni el paso. */
    const val TEXT = "Toca para volver al modo cocina."

    /** Patron de vibracion corto del canal: dos pulsos de 250 ms con 150 ms de pausa. */
    private val VIBRATION_PATTERN = longArrayOf(0L, 250L, 150L, 250L)

    /**
     * Crea —**idempotente**— el canal [CHANNEL_ID]: importancia **alta** (heads-up), vibracion activada
     * con patron corto, sonido de notificacion del sistema y visibilidad publica en la pantalla de
     * bloqueo. Se llama una vez en `MainActivity.onCreate` (el canal existe desde el arranque y aparece
     * en los ajustes del sistema) y de nuevo antes de publicar (defensivo). Crear dos veces el mismo id
     * no cambia nada.
     */
    fun ensureChannel(context: Context) {
        val manager = notificationManager(context) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build(),
            )
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Construye la notificacion del aviso: icono **monocromo** (el sistema lo tine), el copy generico y
     * `autoCancel`. No anade **ningun extra propio**. Si [contentIntent] es `null` la notificacion se
     * construye igual (sin toque) en vez de fallar.
     */
    fun notification(context: Context, contentIntent: PendingIntent?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(TITLE)
            .setContentText(TEXT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        return builder.build()
    }

    /**
     * Publica el aviso con el id **fijo** [NOTIFICATION_ID] (republicarlo **reemplaza** la notificacion
     * anterior, de modo que nunca se duplica, tampoco con la app en primer plano).
     *
     * Con `POST_NOTIFICATIONS` denegado o las notificaciones apagadas **no publica** (se comprueba
     * antes para no depender del sistema) y si el permiso se retirara entre la comprobacion y la
     * publicacion, el `SecurityException` se absorbe **explicitamente**: el Modo cocina y su "¡Listo!"
     * en pantalla nunca se rompen por el aviso (degradacion silenciosa, ADR 0005).
     */
    fun notify(context: Context, contentIntent: PendingIntent?) {
        runCatching { ensureChannel(context) }
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, notification(context, contentIntent))
        } catch (permissionDenied: SecurityException) {
            // Permiso retirado entre la comprobacion y la publicacion: el aviso se pierde en silencio.
        }
    }

    /** `true` si el aviso se puede publicar: permiso concedido **y** notificaciones encendidas. */
    private fun canPost(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun notificationManager(context: Context): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
}
