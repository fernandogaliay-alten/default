package com.sazon.app.data.cooking

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Canal y contenido de las notificaciones del temporizador con Android real (Robolectric, sin
 * emulador; `docs/specs/cooking-timer-alarm.md`, escenarios 2, 3 y 6; y
 * `docs/specs/cooking-timer-foreground-service.md`, escenarios 5 y 9): el canal `cooking_timer` se crea
 * con importancia **alta**, sonido y vibracion; las dos notificaciones (la del aviso de fin y la
 * **persistente** del servicio en primer plano) se **construyen** y se **publican** sin excepcion con su
 * id **fijo**; su contenido es **generico** (titulo/texto exactos, sin la receta) y **sin extras
 * propios**; el `Intent` de contenido lleva **solo** el flag booleano de reanudacion (ni receta, ni
 * pasos, ni claves); y con `POST_NOTIFICATIONS` denegado no se publica nada y no hay excepcion.
 *
 * El comportamiento con el movil bloqueado, en Doze o con la pantalla apagada no es automatizable
 * (AGENTS.md): lo revisa la persona en el emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CookingNotificationsTest {

    private val application: Application get() = ApplicationProvider.getApplicationContext()

    private val context: Context get() = application

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val shadowNotifications get() = shadowOf(notificationManager)

    private fun grantPostNotifications() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowNotifications.setNotificationsEnabled(true)
    }

    /** Sello de fin fresco (hora de pared) para las llamadas de publicacion. */
    private fun selloFresco(offsetMillis: Long = 0L): Long = System.currentTimeMillis() + offsetMillis

    @Test
    fun el_canal_se_crea_con_importancia_alta_sonido_vibracion_y_visibilidad_publica() {
        CookingNotifications.ensureChannel(context)

        val canal = notificationManager.getNotificationChannel(CookingNotifications.CHANNEL_ID)

        assertNotNull("El canal del aviso existe tras asegurarlo", canal)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, canal.importance)
        assertEquals(CookingNotifications.CHANNEL_NAME, canal.name)
        assertEquals(CookingNotifications.CHANNEL_DESCRIPTION, canal.description)
        assertTrue("El canal vibra", canal.shouldVibrate())
        assertNotNull("El canal tiene patron de vibracion", canal.vibrationPattern)
        assertNotNull("El canal tiene sonido configurado", canal.sound)
        assertEquals(Notification.VISIBILITY_PUBLIC, canal.lockscreenVisibility)
    }

    @Test
    fun asegurar_el_canal_dos_veces_es_idempotente() {
        CookingNotifications.ensureChannel(context)
        val primero = notificationManager.getNotificationChannel(CookingNotifications.CHANNEL_ID)

        CookingNotifications.ensureChannel(context)

        val segundo = notificationManager.getNotificationChannel(CookingNotifications.CHANNEL_ID)
        assertEquals(primero, segundo)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, segundo.importance)
        assertEquals(
            "Crear el canal dos veces no crea un canal nuevo",
            1,
            shadowNotifications.getNotificationChannels().count {
                it.id == CookingNotifications.CHANNEL_ID
            },
        )
    }

    @Test
    fun la_notificacion_se_construye_sin_excepcion_con_el_copy_generico() {
        val notificacion = CookingNotifications.notification(context, contentIntent = null, whenMillis = 1_600_000L)

        assertEquals(CookingNotifications.CHANNEL_ID, notificacion.channelId)
        assertEquals(
            "¡Listo! El paso ha terminado",
            notificacion.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals(
            "Toca para volver al modo cocina.",
            notificacion.extras.getString(Notification.EXTRA_TEXT),
        )
        assertEquals(CookingNotifications.TITLE, notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(CookingNotifications.TEXT, notificacion.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(
            "El `when` es el sello de fin (idempotencia entre los dos publicadores)",
            1_600_000L,
            notificacion.`when`,
        )
    }

    @Test
    fun la_notificacion_lleva_icono_monocromo_autocancel_y_categoria_de_alarma() {
        val notificacion = CookingNotifications.notification(context, contentIntent = null, whenMillis = 1_600_000L)

        assertNotNull("El small icon no es nulo", notificacion.smallIcon)
        assertTrue(
            "El small icon es el vector monocromo del aviso",
            notificacion.smallIcon.resId == com.sazon.app.R.drawable.ic_notification_timer,
        )
        assertTrue(
            "autoCancel: la notificacion se descarta al tocarla",
            notificacion.flags and Notification.FLAG_AUTO_CANCEL != 0,
        )
        assertEquals(Notification.CATEGORY_ALARM, notificacion.category)
    }

    @Test
    fun la_notificacion_de_fin_es_descartable_y_no_es_ongoing() {
        // El aviso duradero (feature `cooking-timer-finish-notification`, P2) **no** se sostiene con un
        // servicio vivo: sigue siendo descartable (autoCancel y **no** `FLAG_ONGOING_EVENT`).
        val notificacion = CookingNotifications.notification(context, contentIntent = null, whenMillis = 1_600_000L)

        assertTrue(
            "Descartable: el usuario puede quitarla de la bandeja",
            notificacion.flags and Notification.FLAG_AUTO_CANCEL != 0,
        )
        assertEquals(
            "El aviso de fin no es una notificacion persistente",
            0,
            notificacion.flags and Notification.FLAG_ONGOING_EVENT,
        )
    }

    @Test
    fun el_aviso_de_fin_y_la_persistente_no_comparten_el_modo_ongoing() {
        val avisoDeFin = CookingNotifications.notification(context, contentIntent = null, whenMillis = 1_600_000L)
        val persistente = CookingNotifications.ongoing(context, 1_600_000L, contentIntent = null)

        assertEquals(
            "La persistente si es ongoing",
            Notification.FLAG_ONGOING_EVENT,
            persistente.flags and Notification.FLAG_ONGOING_EVENT,
        )
        assertEquals(
            "La del fin no, aunque compartan canal",
            0,
            avisoDeFin.flags and Notification.FLAG_ONGOING_EVENT,
        )
    }

    @Test
    fun la_notificacion_no_tiene_extras_propios_y_su_contenido_es_solo_el_copy_generico() {
        val notificacion = CookingNotifications.notification(context, contentIntent = null, whenMillis = 1_600_000L)
        val claves = notificacion.extras.keySet()
        val contenido = notificacion.extras.toString()

        assertFalse(
            "Sin claves propias de la app (todo lo que hay lo pone el sistema)",
            claves.any { it.startsWith("com.sazon") },
        )
        assertFalse(
            "Sin clave de extras del aviso",
            claves.any { it.contains("cooking_alarm") },
        )
        assertEquals(CookingNotifications.TITLE, notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(CookingNotifications.TEXT, notificacion.extras.getString(Notification.EXTRA_TEXT))
        assertFalse("Sin rastro de recetas ni ingredientes", contenido.contains(TITULO_RECETA))
        assertFalse("Sin rastro de recetas ni ingredientes", contenido.contains(NOMBRE_INGREDIENTE))
    }

    @Test
    fun la_funcion_no_puede_filtrar_la_receta_porque_no_la_recibe() {
        // Garantia **estructural** (no una convencion): la construccion y la publicacion solo aceptan
        // `Context`, el `PendingIntent` de contenido y el escalar del sello de fin; no hay ningun
        // parametro de receta ni de paso.
        val firma = CookingNotifications::class.java.methods
            .filter { it.name == "notification" || it.name == "notifyFinish" }
            .flatMap { it.parameterTypes.toList() }
            .toSet()

        assertTrue(
            "Solo `Context`, `PendingIntent` y el escalar del sello cruzan a la notificacion",
            firma.all {
                it == Context::class.java ||
                    it == android.app.PendingIntent::class.java ||
                    it == java.lang.Long.TYPE ||
                    it == java.lang.Long::class.java
            },
        )
    }

    @Test
    fun publicar_reemplaza_el_aviso_y_nunca_lo_duplica() {
        grantPostNotifications()

        CookingNotifications.notifyFinish(context, selloFresco(), contentIntent = null)

        assertEquals("Un unico aviso publicado", 1, shadowNotifications.size())
        assertNotNull(
            "Con el id fijo",
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
        )

        CookingNotifications.notifyFinish(context, selloFresco(1_000L), contentIntent = null)

        assertEquals("El id fijo reemplaza la notificacion anterior", 1, shadowNotifications.size())
    }

    @Test
    fun el_mismo_sello_no_vuelve_a_publicar_ni_a_alertar() {
        grantPostNotifications()
        val sello = selloFresco()

        CookingNotifications.notifyFinish(context, sello, contentIntent = null)
        val primera = shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID)

        // Segunda publicacion del MISMO fin (la alarma de respaldo coincidiendo con el servicio): el
        // aviso no se republica, asi que el objeto mostrado sigue siendo el mismo (una sola alerta).
        CookingNotifications.notifyFinish(context, sello, contentIntent = null)

        assertSame(
            "El fin ya anunciado no se vuelve a publicar (una sola notificacion y una sola alerta)",
            primera,
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
        )
    }

    @Test
    fun un_fin_fuera_de_la_ventana_de_frescura_no_se_publica() {
        grantPostNotifications()

        CookingNotifications.notifyFinish(
            context,
            finishWallClockMillis = selloFresco(-120_000L),
            contentIntent = null,
        )

        assertEquals(
            "El aviso de un paso anterior se descarta en vez de sonar fuera de contexto",
            0,
            shadowNotifications.size(),
        )
    }

    @Test
    fun un_fin_nuevo_vuelve_a_alertar_aunque_el_anterior_siga_en_la_bandeja() {
        grantPostNotifications()

        CookingNotifications.notifyFinish(context, selloFresco(), contentIntent = null)
        val anterior = shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID)

        // Fin del paso siguiente: sello distinto y fresco -> se publica (no se silencia con
        // `setOnlyAlertOnce`, que apagaria el aviso legitimo siguiente).
        CookingNotifications.notifyFinish(context, selloFresco(1_000L), contentIntent = null)

        assertNotSame(
            anterior,
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
        )
    }

    @Test
    fun con_el_permiso_denegado_no_publica_y_no_lanza() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        CookingNotifications.notifyFinish(context, selloFresco(), contentIntent = null)

        assertEquals("Sin permiso no se publica nada", 0, shadowNotifications.size())
    }

    @Test
    fun con_las_notificaciones_apagadas_no_publica_y_no_lanza() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowNotifications.setNotificationsEnabled(false)

        CookingNotifications.notifyFinish(context, selloFresco(), contentIntent = null)

        assertEquals("Con las notificaciones apagadas no se publica nada", 0, shadowNotifications.size())
    }

    @Test
    fun el_receptor_publica_el_aviso_generico_con_el_id_fijo() {
        grantPostNotifications()
        val sello = selloFresco()

        CookingAlarmReceiver().onReceive(
            context,
            Intent(context, CookingAlarmReceiver::class.java)
                .putExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, sello),
        )

        val publicada = shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID)
        assertNotNull("El receptor publica el aviso", publicada)
        assertEquals(CookingNotifications.TITLE, publicada.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(CookingNotifications.TEXT, publicada.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("Con el sello del fin que le llego", sello, publicada.`when`)
        assertNotNull("La notificacion tiene intent de contenido", publicada.contentIntent)
    }

    @Test
    fun el_receptor_de_respaldo_no_duplica_un_fin_ya_anunciado_por_el_servicio() {
        grantPostNotifications()
        val sello = selloFresco()
        // El servicio publico primero (productor primario) con ese sello.
        CookingNotifications.notifyFinish(context, sello, contentIntent = null)
        val delServicio = shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID)

        // La alarma de respaldo dispara despues con el MISMO sello.
        CookingAlarmReceiver().onReceive(
            context,
            Intent(context, CookingAlarmReceiver::class.java)
                .putExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, sello),
        )

        assertSame(
            "Mismo fin -> una sola notificacion y una sola alerta",
            delServicio,
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
        )
    }

    @Test
    fun el_receptor_sin_sello_degrada_al_instante_actual_sin_lanzar() {
        grantPostNotifications()

        CookingAlarmReceiver().onReceive(context, Intent(context, CookingAlarmReceiver::class.java))

        assertNotNull(
            "Sin el escalar del sello el aviso sigue publicandose (degradacion silenciosa)",
            shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID),
        )
    }

    @Test
    fun el_toque_de_la_notificacion_vuelve_a_la_actividad_de_sazon() {
        grantPostNotifications()

        CookingAlarmReceiver().onReceive(
            context,
            Intent(context, CookingAlarmReceiver::class.java)
                .putExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, selloFresco()),
        )

        val publicada = shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID)
        val shadowIntent = shadowOf(publicada.contentIntent)
        assertTrue("El PendingIntent de contenido abre una Activity", shadowIntent.isActivity)
        val intent = shadowIntent.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(
            "NEW_TASK: el aviso funciona con la app en segundo plano",
            intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0,
        )
        assertTrue(
            "SINGLE_TOP: reutiliza la actividad viva en lugar de recrearla",
            intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0,
        )
    }

    // --- Notificacion **persistente** y flag de reanudacion (feature
    // `cooking-timer-foreground-service`, escenarios 9 y 5) ---

    @Test
    fun la_notificacion_persistente_se_construye_sin_excepcion_con_la_cuenta_atras_del_sistema() {
        val notificacion = CookingNotifications.ongoing(context, 1_600_000L, contentIntent = null)

        assertEquals("Cocina en marcha", notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(CookingNotifications.ONGOING_TITLE, notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(CookingNotifications.CHANNEL_ID, notificacion.channelId)
        assertEquals(1_600_000L, notificacion.`when`)
        assertTrue(
            "El sistema pinta la cuenta atras",
            notificacion.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER),
        )
        assertTrue(
            "Y la pinta hacia abajo",
            notificacion.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN),
        )
        assertTrue(
            "Persistente: no se descarta con un gesto",
            notificacion.flags and Notification.FLAG_ONGOING_EVENT != 0,
        )
        assertNull("Sin cuerpo de texto", notificacion.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(com.sazon.app.R.drawable.ic_notification_timer, notificacion.smallIcon.resId)
    }

    @Test
    fun la_notificacion_persistente_se_publica_con_su_id_fijo_y_sin_extras_de_receta() {
        val notificacion = CookingNotifications.ongoing(context, 1_600_000L, contentIntent = null)
        val claves = notificacion.extras.keySet()
        val contenido = notificacion.extras.toString()

        assertFalse(
            "Sin claves propias de la app",
            claves.any { it.startsWith("com.sazon") },
        )
        assertFalse("Sin rastro de recetas ni ingredientes", contenido.contains(TITULO_RECETA))
        assertFalse("Sin rastro de recetas ni ingredientes", contenido.contains(NOMBRE_INGREDIENTE))
        assertEquals(
            "La persistente usa un id distinto del aviso de fin",
            1000,
            CookingNotifications.ONGOING_NOTIFICATION_ID,
        )
        assertNotEquals(
            "Los dos ids no pueden colisionar",
            CookingNotifications.NOTIFICATION_ID,
            CookingNotifications.ONGOING_NOTIFICATION_ID,
        )
    }

    @Test
    fun el_intent_de_contenido_lleva_solo_el_flag_de_reanudacion() {
        val pendingIntent = CookingNotifications.contentIntent(context, CookingAlarmReceiver.CONTENT_REQUEST_CODE)

        val shadowIntent = shadowOf(checkNotNull(pendingIntent))
        assertTrue("Es un PendingIntent de Activity", shadowIntent.isActivity)
        val intent = shadowIntent.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(
            "El flag de reanudacion es un booleano",
            intent.getBooleanExtra(CookingNotifications.EXTRA_RESUME_COOKING, false),
        )
        assertEquals(
            "Nada mas viaja al sistema (sin receta, sin pasos, sin claves)",
            setOf(CookingNotifications.EXTRA_RESUME_COOKING),
            intent.extras?.keySet().orEmpty(),
        )
    }

    @Test
    fun el_receptor_publica_el_aviso_con_el_flag_de_reanudacion_y_sin_datos_de_la_receta() {
        grantPostNotifications()

        CookingAlarmReceiver().onReceive(context, Intent(context, CookingAlarmReceiver::class.java))

        val publicada = shadowNotifications.getNotification(CookingNotifications.NOTIFICATION_ID)
        val intent = shadowOf(publicada.contentIntent).savedIntent
        assertTrue(intent.getBooleanExtra(CookingNotifications.EXTRA_RESUME_COOKING, false))
        assertEquals(
            setOf(CookingNotifications.EXTRA_RESUME_COOKING),
            intent.extras?.keySet().orEmpty(),
        )
    }

    private companion object {
        const val TITULO_RECETA = "Tortilla de patatas"
        const val NOMBRE_INGREDIENTE = "Patata agria"
    }
}
