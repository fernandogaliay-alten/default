package com.sazon.app.data.cooking

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.R
import java.time.Duration
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowService

/**
 * Servicio en primer plano de la cuenta atras
 * (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1, 4, 7 y 9): se arranca con la
 * notificacion **persistente** (id `1000`, cronometro descendente, sin datos de la receta), espera al
 * instante de fin con **un unico** despertar y entonces retira la notificacion y se detiene; sin el
 * extra no arranca nada; con `POST_NOTIFICATIONS` denegado arranca igual, sin lanzar. El servicio
 * **no** publica el aviso de fin (id `1001`), que sigue siendo de la alarma aceptada.
 *
 * El comportamiento real (pantalla bloqueada, Doze, bateria agresiva) no es automatizable (AGENTS.md):
 * lo revisa la persona en el emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CookingTimerServiceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun lanzar(intent: Intent): ServiceController<CookingTimerService> =
        Robolectric.buildService(CookingTimerService::class.java, intent).create().startCommand(0, 1)

    private fun conCuentaAtras(restanteMillis: Long): ServiceController<CookingTimerService> =
        lanzar(CookingTimerService.intent(context, SystemClock.elapsedRealtime() + restanteMillis))

    private val ServiceController<CookingTimerService>.shadow: ShadowService
        get() = shadowOf(get())

    @Test
    fun arranca_en_primer_plano_con_la_notificacion_persistente() {
        val controller = conCuentaAtras(600_000L)

        val shadow = controller.shadow

        assertEquals(
            "La persistente es la 1000, no la del aviso de fin",
            CookingNotifications.ONGOING_NOTIFICATION_ID,
            shadow.lastForegroundNotificationId,
        )
        assertEquals(1000, CookingNotifications.ONGOING_NOTIFICATION_ID)
        assertTrue("El servicio queda en primer plano", shadow.isLastForegroundNotificationAttached())
        assertNull(
            "El servicio no publica la notificacion del aviso de fin",
            shadowOf(notificationManager).getNotification(CookingNotifications.NOTIFICATION_ID),
        )

        controller.destroy()
    }

    @Test
    fun la_notificacion_persistente_pinta_la_cuenta_atras_con_el_instante_de_fin() {
        val controller = conCuentaAtras(600_000L)
        val finEsperado = System.currentTimeMillis() + 600_000L

        val notificacion = checkNotNull(controller.shadow.lastForegroundNotification)

        assertEquals(CookingNotifications.CHANNEL_ID, notificacion.channelId)
        assertEquals(
            CookingNotifications.ONGOING_TITLE,
            notificacion.extras.getString(Notification.EXTRA_TITLE),
        )
        assertTrue(
            "Es un cronometro",
            notificacion.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER),
        )
        assertTrue(
            "El cronometro va hacia abajo (cuenta atras)",
            notificacion.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN),
        )
        assertTrue(
            "El instante del cronometro es el fin de la cuenta atras (60 s de margen por el arranque)",
            abs(notificacion.`when` - finEsperado) < 60_000L,
        )
        assertEquals(R.drawable.ic_notification_timer, notificacion.smallIcon.resId)
        assertTrue(
            "Persistente: no se puede descartar con un gesto",
            notificacion.flags and Notification.FLAG_ONGOING_EVENT != 0,
        )
        assertTrue(
            "Solo alerta una vez",
            notificacion.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
        )
        assertEquals(
            "Silenciosa: no se alerta sola (sin grupo, deja de ser GROUP_ALERT_ALL)",
            NotificationCompat.GROUP_ALERT_SUMMARY,
            NotificationCompat.getGroupAlertBehavior(notificacion),
        )
        assertEquals(
            "Sin categoria de alarma: es un cronometro",
            Notification.CATEGORY_STOPWATCH,
            notificacion.category,
        )

        controller.destroy()
    }

    @Test
    fun la_notificacion_persistente_no_expone_la_receta() {
        val controller = conCuentaAtras(600_000L)

        val notificacion = checkNotNull(controller.shadow.lastForegroundNotification)

        assertNull(
            "Sin cuerpo de texto: no hay copy de la receta",
            notificacion.extras.getString(Notification.EXTRA_TEXT),
        )
        assertFalse(
            "Sin claves propias de la app",
            notificacion.extras.keySet().any { it.startsWith("com.sazon") },
        )
        assertNotNull("Pero si lleva el intent de contenido", notificacion.contentIntent)

        controller.destroy()
    }

    @Test
    fun el_unico_dato_que_viaja_al_servicio_es_el_instante_de_fin() {
        val intent = CookingTimerService.intent(context, 1_600_000L)

        assertEquals(CookingTimerService::class.java.name, intent.component?.className)
        assertEquals(1_600_000L, intent.getLongExtra(CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS, 0L))
        assertEquals(
            "Nada mas cruza al sistema",
            setOf(CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS),
            intent.extras?.keySet().orEmpty(),
        )
    }

    @Test
    fun al_vencer_la_cuenta_atras_retira_la_notificacion_y_se_detiene() {
        val controller = conCuentaAtras(600_000L)
        val shadow = controller.shadow
        assertFalse("Antes de vencer sigue vivo", shadow.isStoppedBySelf())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(10))

        assertTrue("Al vencer se detiene", shadow.isStoppedBySelf())
        assertTrue("Y retira la notificacion persistente", shadow.isForegroundStopped)
        assertTrue(shadow.notificationShouldRemoved)

        controller.destroy()
    }

    @Test
    fun con_el_instante_de_fin_ya_pasado_no_se_queda_pintando() {
        val controller = conCuentaAtras(-1_000L)

        val shadow = controller.shadow

        assertTrue(shadow.isStoppedBySelf())
        assertTrue(shadow.isForegroundStopped)

        controller.destroy()
    }

    @Test
    fun sin_el_instante_de_fin_se_apaga_sin_arrancar_el_primer_plano_y_sin_lanzar() {
        val controller = lanzar(Intent(context, CookingTimerService::class.java))

        val shadow = controller.shadow

        assertNull("Sin instante de fin no hay notificacion", shadow.lastForegroundNotification)
        assertTrue(shadow.isStoppedBySelf())

        controller.destroy()
    }

    @Test
    fun el_servicio_no_se_recrea_tras_morir_el_proceso() {
        val controller = conCuentaAtras(600_000L)

        assertEquals(
            "Sin intent no se recrea: el toque vuelve al paso por la sesion persistida",
            android.app.Service.START_NOT_STICKY,
            controller.get().onStartCommand(CookingTimerService.intent(context, 1L), 0, 2),
        )

        controller.destroy()
    }

    @Test
    fun con_el_permiso_denegado_el_servicio_arranca_igual_y_sin_lanzar() {
        // Scenario 7: denegar `POST_NOTIFICATIONS` no puede romper nada. El servicio sigue corriendo y
        // en primer plano (la notificacion simplemente no se ve; Robolectric no modela esa visibilidad).
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val controller = conCuentaAtras(600_000L)

        val shadow = controller.shadow
        assertEquals(CookingNotifications.ONGOING_NOTIFICATION_ID, shadow.lastForegroundNotificationId)
        assertFalse("Sin permiso no se detiene ni falla", shadow.isStoppedBySelf())

        controller.destroy()
    }

    @Test
    fun arrancar_dos_veces_es_idempotente_y_no_duplica_la_notificacion() {
        val controller = Robolectric.buildService(
            CookingTimerService::class.java,
            CookingTimerService.intent(context, SystemClock.elapsedRealtime() + 600_000L),
        ).create()
        controller.startCommand(0, 1)

        controller.startCommand(0, 2)

        val shadow = shadowOf(controller.get())
        assertEquals(
            "El id fijo reemplaza: sigue habiendo una sola notificacion",
            CookingNotifications.ONGOING_NOTIFICATION_ID,
            shadow.lastForegroundNotificationId,
        )
        assertTrue(shadow.isLastForegroundNotificationAttached())
        assertFalse("Sigue en marcha", shadow.isStoppedBySelf())

        controller.destroy()
    }
}
