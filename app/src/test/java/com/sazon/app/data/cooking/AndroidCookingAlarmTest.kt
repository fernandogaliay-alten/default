package com.sazon.app.data.cooking

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.ui.cooking.CookingAlarmCommand
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Adaptador Android del aviso con `AlarmManager` real (Robolectric + `ShadowAlarmManager`, sin
 * emulador; `docs/specs/cooking-timer-alarm.md`, escenarios 1, 4 y 5): `Schedule` deja **una** alarma
 * para el instante pedido en la base `ELAPSED_REALTIME_WAKEUP`, `Cancel` no deja ninguna y `None` no
 * toca el `AlarmManager`.
 *
 * El ShadowAlarmManager de Robolectric 4.17 **si** implementa `setExactAndAllowWhileIdle` y
 * `setAndAllowWhileIdle` (y `setCanScheduleExactAlarms`), asi que la ruta **exacta** y la degradacion
 * **inexacta** se comprueban de verdad; no hay limitacion del shadow que documentar. El disparo real
 * (Doze, pantalla bloqueada) no es automatizable: lo revisa la persona en el emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidCookingAlarmTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val application: Application get() = ApplicationProvider.getApplicationContext()

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val shadowAlarms: ShadowAlarmManager get() = shadowOf(alarmManager)

    @Test
    fun programar_deja_una_sola_alarma_en_el_instante_indicado() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))

        val programadas = shadowAlarms.scheduledAlarms
        assertEquals("Una unica alarma programada", 1, programadas.size)
        assertEquals(1_600_000L, programadas.first().triggerAtMs)
        assertEquals(
            "La base es el reloj monotono que no depende de la hora del sistema",
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            programadas.first().getType(),
        )
        assertTrue("La alarma despierta el dispositivo", programadas.first().isAllowWhileIdle)
    }

    @Test
    fun reprogramar_reemplaza_la_alarma_anterior() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))
        alarm.apply(CookingAlarmCommand.Schedule(2_300_000L))

        val programadas = shadowAlarms.scheduledAlarms
        assertEquals("El requestCode fijo reemplaza la alarma anterior", 1, programadas.size)
        assertEquals(2_300_000L, programadas.first().triggerAtMs)
    }

    @Test
    fun sin_alarma_exacta_concedida_degrada_a_una_alarma_inexacta_que_despierta_igual() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))

        val programadas = shadowAlarms.scheduledAlarms
        assertEquals("Degrada, pero programa igual", 1, programadas.size)
        assertEquals(1_600_000L, programadas.first().triggerAtMs)
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, programadas.first().getType())
        assertTrue(programadas.first().isAllowWhileIdle)
    }

    @Test
    fun cancelar_no_deja_ninguna_alarma_pendiente() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)
        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))
        assertEquals(1, shadowAlarms.scheduledAlarms.size)

        alarm.apply(CookingAlarmCommand.Cancel)

        assertEquals("Pausar/Reiniciar/salir dejan cero alarmas", 0, shadowAlarms.scheduledAlarms.size)
    }

    @Test
    fun cancelar_sin_nada_programado_es_idempotente_y_no_lanza() {
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.Cancel)
        alarm.apply(CookingAlarmCommand.Cancel)

        assertEquals(0, shadowAlarms.scheduledAlarms.size)
    }

    @Test
    fun none_no_toca_el_alarmmanager() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.None)

        assertEquals("Los ticks no programan ni cancelan nada", 0, shadowAlarms.scheduledAlarms.size)
    }

    @Test
    fun cancelar_tras_programar_y_volver_a_programar_es_un_ciclo_limpio() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))
        alarm.apply(CookingAlarmCommand.Cancel)
        alarm.apply(CookingAlarmCommand.Schedule(2_300_000L))

        assertEquals(1, shadowAlarms.scheduledAlarms.size)
        assertEquals(2_300_000L, shadowAlarms.scheduledAlarms.first().triggerAtMs)
    }

    // --- Pata del **servicio en primer plano** de la cuenta atras (feature
    // `cooking-timer-foreground-service`, escenarios 1 y 4): el mismo comando que deja la alarma en su
    // sitio arranca y para el servicio. Casos **anadidos**: los del aviso de fin no se alteran. ---

    @Test
    fun programar_arranca_tambien_el_servicio_de_la_cuenta_atras_con_el_instante_de_fin() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)
        shadowOf(application).clearStartedServices()

        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))

        val arrancado = shadowOf(application).nextStartedService
        assertEquals(CookingTimerService::class.java.name, arrancado.component?.className)
        assertEquals(
            "El servicio recibe el mismo instante de fin que la alarma",
            1_600_000L,
            arrancado.getLongExtra(CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS, 0L),
        )
        assertEquals(
            "Los dos escalares del temporizador (fin monotono + sello en hora de pared)",
            setOf(
                CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS,
                CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS,
            ),
            arrancado.extras?.keySet().orEmpty(),
        )
    }

    @Test
    fun el_sello_de_fin_compartido_viaja_en_los_dos_intent() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val alarm = AndroidCookingAlarm(context)
        shadowOf(application).clearStartedServices()
        val selloEsperado = System.currentTimeMillis() + 600_000L

        alarm.apply(CookingAlarmCommand.Schedule(SystemClock.elapsedRealtime() + 600_000L))

        val alarma = shadowAlarms.scheduledAlarms.single()
        val intentDeLaAlarma = shadowOf(alarma.operation).savedIntent
        val selloDeLaAlarma = intentDeLaAlarma
            .getLongExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, 0L)
        val intentDelServicio = checkNotNull(shadowOf(application).nextStartedService)
        val selloDelServicio = intentDelServicio
            .getLongExtra(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS, 0L)

        assertTrue("El sello es un instante positivo", selloDeLaAlarma > 0L)
        assertEquals(
            "El sello se calcula una vez y va en los dos Intent",
            selloDeLaAlarma,
            selloDelServicio,
        )
        assertTrue(
            "El sello es el fin en hora de pared (60 s de margen por el arranque)",
            abs(selloDeLaAlarma - selloEsperado) < 60_000L,
        )
        assertEquals(
            "El PendingIntent de la alarma solo lleva el escalar del sello (nunca la receta)",
            setOf(CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS),
            intentDeLaAlarma.extras?.keySet().orEmpty(),
        )
    }

    @Test
    fun cancelar_para_el_servicio_de_la_cuenta_atras() {
        val alarm = AndroidCookingAlarm(context)
        alarm.apply(CookingAlarmCommand.Schedule(1_600_000L))

        alarm.apply(CookingAlarmCommand.Cancel)

        val parado = shadowOf(application).nextStoppedService
        assertEquals(CookingTimerService::class.java.name, parado.component?.className)
    }

    @Test
    fun none_no_arranca_ni_para_el_servicio() {
        val alarm = AndroidCookingAlarm(context)
        shadowOf(application).clearStartedServices()

        alarm.apply(CookingAlarmCommand.None)

        assertNull("Los ticks no tocan el servicio", shadowOf(application).peekNextStartedService())
    }

    // --- Limpieza del aviso de fin al salir del modo (feature `cooking-timer-finish-notification`,
    // escenarios 5 y 6): `dismissFinishNotification` retira la `1001` ya mostrada, es idempotente y sin
    // permiso no lanza; `Cancel` **no** la retira (guarda de la decision 9 del ADR 0007). ---

    @Test
    fun dismiss_finish_notification_retira_la_1001_ya_mostrada() {
        postFinishNotification()
        assertNotNull(
            "El aviso de fin esta en la bandeja antes de limpiarlo",
            notifications().getNotification(CookingNotifications.NOTIFICATION_ID),
        )

        AndroidCookingAlarm(context).dismissFinishNotification()

        assertNull(
            "La limpieza al salir (Atras/Terminar) retira el aviso de fin",
            notifications().getNotification(CookingNotifications.NOTIFICATION_ID),
        )
    }

    @Test
    fun dismiss_finish_notification_es_idempotente_y_sin_permiso_no_lanza() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val alarm = AndroidCookingAlarm(context)

        // Sin aviso mostrado y sin permiso: no lanza ni deja nada.
        alarm.dismissFinishNotification()
        alarm.dismissFinishNotification()

        assertNull(notifications().getNotification(CookingNotifications.NOTIFICATION_ID))
    }

    @Test
    fun cancelar_no_retira_la_1001_ya_mostrada_guarda_decision_9() {
        postFinishNotification()
        val alarm = AndroidCookingAlarm(context)

        alarm.apply(CookingAlarmCommand.Cancel)

        assertNotNull(
            "Decision 9 del ADR 0007: `Cancel` no retira una notificacion ya mostrada; solo la limpia " +
                "`dismissFinishNotification` en la salida explicita",
            notifications().getNotification(CookingNotifications.NOTIFICATION_ID),
        )
    }

    /** Publica el aviso de fin (id `1001`) con el permiso concedido, como haria el receptor. */
    private fun postFinishNotification() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifications().setNotificationsEnabled(true)
        CookingNotifications.notifyFinish(
            context,
            finishWallClockMillis = System.currentTimeMillis(),
            contentIntent = null,
        )
    }

    private fun notifications(): org.robolectric.shadows.ShadowNotificationManager =
        shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
}
