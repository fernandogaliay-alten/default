package com.sazon.app.data.cooking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sazon.app.MainActivity
import com.sazon.app.R

/**
 * Canal y notificaciones del **temporizador** del modo cocina (capa Android;
 * `docs/specs/cooking-timer-alarm.md` y `docs/specs/cooking-timer-foreground-service.md`).
 *
 * Son dos notificaciones con id **fijo** y distinto:
 *
 * - la **persistente** ([ONGOING_NOTIFICATION_ID], `ongoing`) que publica el servicio en primer plano
 *   ([CookingTimerService]) con la cuenta atras decreciente mientras corre el temporizador;
 * - la del **aviso de fin** ([NOTIFICATION_ID], `docs/specs/cooking-timer-alarm.md`, escenarios 2, 3 y
 *   6) que publica el receptor de la alarma exacta.
 *
 * El contenido es **generico** por construccion: ni [ongoing] ni [notification] reciben la receta, asi
 * que no pueden exponer la descripcion del paso, los ingredientes ni las notas. Son notificaciones de
 * sistema que no forman parte de ninguna pantalla de la app.
 *
 * **Degradacion silenciosa** (ADR 0005): si `POST_NOTIFICATIONS` esta denegado (o las notificaciones
 * estan apagadas) el aviso de fin **no se publica** y **no** hay excepcion, y la persistente la publica
 * el servicio con `startForeground` (que no exige el permiso: el sistema simplemente no la muestra y el
 * servicio sigue corriendo); el modo cocina y su "¡Listo!" en pantalla siguen intactos.
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

    /**
     * Id **fijo** de la notificacion **persistente** del servicio en primer plano (la cuenta atras).
     * Es distinto del [NOTIFICATION_ID] aceptado del aviso de fin, de modo que las dos pueden coexistir
     * sin reemplazarse.
     */
    const val ONGOING_NOTIFICATION_ID = 1000

    /**
     * Titulo **fijo** de la notificacion persistente: solo dice que hay una cocina en marcha. La cuenta
     * atras la pinta el **sistema** (cronometro descendente), asi que aqui no hay ningun dato de la
     * receta.
     */
    const val ONGOING_TITLE = "Cocina en marcha"

    /**
     * Extra **booleano** de los `Intent` de contenido de las dos notificaciones: "vengo del
     * temporizador, reanuda el paso". Es lo **unico** que distingue un arranque normal de una vuelta al
     * Modo cocina (ni receta, ni paso, ni claves cruzan al sistema).
     */
    const val EXTRA_RESUME_COOKING = "com.sazon.app.extra.cooking.RESUME_COOKING"

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
     * Construye la notificacion **persistente** del servicio en primer plano: titulo fijo
     * ([ONGOING_TITLE]) y una **cuenta atras que pinta el sistema** (`setUsesChronometer(true)` +
     * `setChronometerCountDown(true)` + `setWhen(endWallClockMillis)`), sin `contentText` ni extras
     * propios: no expone la receta, los ingredientes, los pasos ni la clave de IA.
     *
     * `ongoing` + `onlyAlertOnce` + `silent`: no se puede descartar con un gesto y **no** vuelve a
     * alertar (ni suena ni vibra) al refrescarse; se publica en el **mismo canal** aceptado
     * `cooking_timer` (una notificacion no puede elegir un canal distinto sin cambiarlo para todas). Es
     * una funcion **pura de Android**: entra el instante de fin ya calculado (`endWallClockMillis`, hora
     * de pared, que es la base del cronometro), de modo que un test puede fijarlo y comprobarlo.
     *
     * Si [contentIntent] es `null` la notificacion se construye igual (sin toque) en vez de fallar.
     */
    fun ongoing(
        context: Context,
        endWallClockMillis: Long,
        contentIntent: PendingIntent?,
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(ONGOING_TITLE)
            .setWhen(endWallClockMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        return builder.build()
    }

    /**
     * `PendingIntent` de **contenido** de las dos notificaciones del temporizador (compartido por el
     * receptor del aviso de fin y el servicio en primer plano): abre `MainActivity` con
     * `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP` —de modo que **reutiliza** la actividad viva
     * en lugar de recrearla— y le pasa **solo** [EXTRA_RESUME_COOKING] (`true`): ninguna receta, paso ni
     * clave cruza al sistema. Devuelve `null` si el sistema no puede construirlo (nunca lanza).
     */
    fun contentIntent(context: Context, requestCode: Int): PendingIntent? = runCatching {
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_RESUME_COOKING, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

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
