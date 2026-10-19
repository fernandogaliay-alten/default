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
import com.sazon.app.ui.cooking.CookingStepLabel
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
 * (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1, 4, 7 y 9; y
 * `docs/specs/cooking-timer-notification-reliability.md`, escenarios 3 y 5): se arranca con la
 * notificacion **persistente** (id `1000`, cronometro descendente, sin datos de la receta), espera al
 * instante de fin con **unico** despertar y entonces **publica el aviso de fin** (`1001`, productor
 * primario del "¡Listo!"), **retira** la persistente y se detiene; con el instante de fin **ya pasado**
 * no pinta nada (no hay cronometro caducado ni cuenta atras en negativo); sin el extra no arranca nada;
 * con `POST_NOTIFICATIONS` denegado arranca igual, sin lanzar.
 *
 * El comportamiento real (pantalla bloqueada, Doze, bateria agresiva) no es automatizable (AGENTS.md):
 * lo revisa la persona en el emulador.
 *
 * Desde `cooking-timer-notification-step-label` el `Intent` del servicio puede llevar ademas la
 * **etiqueta del paso** (numero + titulo, la frontera de privacidad) y la persistente la pinta como
 * titulo y cuerpo; sin etiqueta todo queda como antes (titulo generico, sin cuerpo).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CookingTimerServiceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val shadowNotifications get() = shadowOf(notificationManager)

    private fun grantPostNotifications() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowNotifications.setNotificationsEnabled(true)
    }

    private fun lanzar(intent: Intent): ServiceController<CookingTimerService> =
        Robolectric.buildService(CookingTimerService::class.java, intent).create().startCommand(0, 1)

    /** Arranca el servicio con una cuenta atras de [restanteMillis], su sello de fin y la etiqueta. */
    private fun conCuentaAtras(
        restanteMillis: Long,
        stepLabel: CookingStepLabel? = null,
    ): ServiceController<CookingTimerService> =
        lanzar(
            CookingTimerService.intent(
                context = context,
                endAtElapsedMillis = SystemClock.elapsedRealtime() + restanteMillis,
                finishWallClockMillis = System.currentTimeMillis() + restanteMillis,
                stepLabel = stepLabel,
            ),
        )

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
            "Al arrancar (fin futuro) todavia no hay aviso de fin",
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
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
    fun la_persistente_muestra_el_numero_y_el_titulo_del_paso_con_la_cuenta_atras_intacta() {
        val controller = conCuentaAtras(600_000L, CookingStepLabel("Paso 2 de 5", "Dorar el pollo"))

        val notificacion = checkNotNull(controller.shadow.lastForegroundNotification)

        assertEquals("Paso 2 de 5", notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Dorar el pollo", notificacion.extras.getString(Notification.EXTRA_TEXT))
        assertTrue(
            "El sistema sigue pintando la cuenta atras",
            notificacion.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER),
        )
        assertTrue(notificacion.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertTrue(
            "Sigue siendo persistente y alertando una sola vez",
            notificacion.flags and Notification.FLAG_ONGOING_EVENT != 0 &&
                notificacion.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
        )
        assertFalse(
            "Ni un dato mas de la receta en la notificacion",
            notificacion.extras.keySet().any { it.startsWith("com.sazon") },
        )
        assertFalse(
            "Sin rastro de la descripcion del paso",
            notificacion.extras.toString().contains(DESCRIPCION_PASO),
        )

        controller.destroy()
    }

    @Test
    fun al_armar_el_paso_nuevo_la_persistente_lleva_su_etiqueta_y_no_la_heredada() {
        val controller = Robolectric.buildService(
            CookingTimerService::class.java,
            CookingTimerService.intent(
                context,
                SystemClock.elapsedRealtime() + 600_000L,
                System.currentTimeMillis() + 600_000L,
                CookingStepLabel("Paso 1 de 5", "Picar la cebolla"),
            ),
        ).create()
        controller.startCommand(0, 1)
        assertEquals(
            "Paso 1 de 5",
            checkNotNull(controller.shadow.lastForegroundNotification)
                .extras.getString(Notification.EXTRA_TITLE),
        )

        // Cambio de paso: la politica cancela el aviso (para el servicio) y el armado siguiente compone la
        // etiqueta del paso **nuevo**; la persistente nunca arrastra la del anterior.
        controller.get().onStartCommand(
            CookingTimerService.intent(
                context,
                SystemClock.elapsedRealtime() + 300_000L,
                System.currentTimeMillis() + 300_000L,
                CookingStepLabel("Paso 2 de 5", "Dorar el pollo"),
            ),
            0,
            2,
        )

        val notificacion = checkNotNull(controller.shadow.lastForegroundNotification)
        assertEquals("Paso 2 de 5", notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Dorar el pollo", notificacion.extras.getString(Notification.EXTRA_TEXT))

        controller.destroy()
    }

    @Test
    fun los_dos_escalares_del_temporizador_son_lo_unico_que_viaja_al_servicio_sin_etiqueta() {
        val intent = CookingTimerService.intent(context, 1_600_000L, 1_700_000L, stepLabel = null)

        assertEquals(CookingTimerService::class.java.name, intent.component?.className)
        assertEquals(1_600_000L, intent.getLongExtra(CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS, 0L))
        assertEquals(
            "El sello de fin compartido con la alarma",
            1_700_000L,
            intent.getLongExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, 0L),
        )
        assertEquals(
            "Nada mas cruza al sistema",
            setOf(
                CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS,
                CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS,
            ),
            intent.extras?.keySet().orEmpty(),
        )
    }

    @Test
    fun con_etiqueta_el_intent_del_servicio_lleva_el_numero_y_el_titulo_del_paso_y_nada_mas() {
        val intent = CookingTimerService.intent(
            context,
            1_600_000L,
            1_700_000L,
            CookingStepLabel("Paso 2 de 5", "Dorar el pollo"),
        )

        assertEquals("Paso 2 de 5", intent.getStringExtra(CookingTimerService.EXTRA_STEP_PROGRESS))
        assertEquals("Dorar el pollo", intent.getStringExtra(CookingTimerService.EXTRA_STEP_TITLE))
        assertEquals(
            "Fin + sello + numero + titulo: ni un dato mas de la receta cruza al sistema",
            setOf(
                CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS,
                CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS,
                CookingTimerService.EXTRA_STEP_PROGRESS,
                CookingTimerService.EXTRA_STEP_TITLE,
            ),
            intent.extras?.keySet().orEmpty(),
        )
    }

    @Test
    fun al_vencer_la_cuenta_atras_publica_el_aviso_retira_la_persistente_y_se_detiene() {
        grantPostNotifications()
        val endAt = SystemClock.elapsedRealtime() + 600_000L
        val sello = System.currentTimeMillis() + 600_000L
        val controller = lanzar(CookingTimerService.intent(context, endAt, sello, stepLabel = null))
        val shadow = controller.shadow
        assertFalse("Antes de vencer sigue vivo", shadow.isStoppedBySelf())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(10))

        assertTrue("Al vencer se detiene", shadow.isStoppedBySelf())
        assertTrue("Y retira la notificacion persistente", shadow.isForegroundStopped)
        assertTrue(shadow.notificationShouldRemoved)
        val aviso = checkNotNull(
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
        )
        assertEquals(
            "Publica el aviso de fin (productor primario)",
            CookingNotifications.TITLE,
            aviso.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals(CookingNotifications.TEXT, aviso.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(
            "El aviso lleva el sello de fin compartido como `when`",
            sello,
            aviso.`when`,
        )

        controller.destroy()
    }

    @Test
    fun el_aviso_de_fin_del_servicio_no_lleva_datos_de_la_receta() {
        grantPostNotifications()
        val controller = conCuentaAtras(600_000L)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(10))

        val aviso = checkNotNull(shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID))
        assertFalse(
            "Sin claves propias de la app",
            aviso.extras.keySet().any { it.startsWith("com.sazon") },
        )
        assertFalse(
            "Sin rastro de la receta",
            aviso.extras.toString().contains("Tortilla de patatas"),
        )

        controller.destroy()
    }

    @Test
    fun con_el_instante_de_fin_ya_pasado_no_pinta_la_cuenta_atras() {
        val controller = conCuentaAtras(-1_000L)

        val shadow = controller.shadow

        assertTrue(shadow.isStoppedBySelf())
        assertNull(
            "No hay cronometro caducado: no se pinta nunca en negativo",
            shadow.lastForegroundNotification,
        )

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
            controller.get().onStartCommand(
                CookingTimerService.intent(context, 1L, 1L, stepLabel = null),
                0,
                2,
            ),
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
            CookingTimerService.intent(
                context,
                SystemClock.elapsedRealtime() + 600_000L,
                System.currentTimeMillis() + 600_000L,
                stepLabel = null,
            ),
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

    private companion object {

        /** Descripcion de un paso que **nunca** puede aparecer en la notificacion persistente. */
        const val DESCRIPCION_PASO = "Sella la piel a fuego fuerte y dora por los dos lados."
    }
}
