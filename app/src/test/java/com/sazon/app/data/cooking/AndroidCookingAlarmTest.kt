package com.sazon.app.data.cooking

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.ui.cooking.CookingAlarmCommand
import org.junit.Assert.assertEquals
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
            "Unico dato que viaja al servicio",
            setOf(CookingTimerService.EXTRA_END_AT_ELAPSED_MILLIS),
            arrancado.extras?.keySet().orEmpty(),
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
}
