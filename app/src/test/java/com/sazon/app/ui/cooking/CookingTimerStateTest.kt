package com.sazon.app.ui.cooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Maquina de estados del temporizador del modo cocina con **reloj unico**
 * (`docs/specs/cooking-timer-notification-reliability.md`, escenarios 1 y 2; enmienda la verificacion
 * de `docs/specs/cooking-mode.md`).
 *
 * La cuenta atras se deriva de un **instante de fin absoluto** (`endAtElapsedMillis`, inyectado): el
 * restante es la proyeccion exacta `endAt - now`, nunca la suma de deltas, asi que latidos tardios o
 * desiguales **no** acumulan deriva y el valor jamas es negativo. Logica pura sin Compose ni Android.
 */
class CookingTimerStateTest {

    private val inicio = 1_000_000L

    @Test
    fun of_diez_minutos_arranca_inactivo_con_la_duracion_total() {
        val timer = CookingTimerState.of(10)

        assertEquals(CookingTimerStatus.IDLE, timer.status)
        assertEquals(600_000L, timer.totalMillis)
        assertEquals(600_000L, timer.remainingMillis)
        assertNull("Inactivo no tiene ancla", timer.endAtElapsedMillis)
        assertTrue(timer.hasDuration)
        assertFalse(timer.isRunning)
        assertFalse(timer.isFinished)
    }

    @Test
    fun of_sin_duracion_no_tiene_temporizador() {
        val sinMinutos = CookingTimerState.of(0)
        val negativo = CookingTimerState.of(-5)

        assertFalse(sinMinutos.hasDuration)
        assertEquals(0L, sinMinutos.totalMillis)
        assertEquals(0L, sinMinutos.remainingMillis)
        assertFalse(negativo.hasDuration)
        assertEquals(0L, negativo.totalMillis)
    }

    @Test
    fun arrancar_ancla_el_instante_de_fin() {
        val timer = CookingTimerState.of(10).start(inicio)

        assertEquals(CookingTimerStatus.RUNNING, timer.status)
        assertTrue(timer.isRunning)
        assertEquals(600_000L, timer.remainingMillis)
        assertEquals("El ancla es now + remaining", inicio + 600_000L, timer.endAtElapsedMillis)
    }

    @Test
    fun arrancar_un_paso_sin_duracion_no_ancla_nada() {
        val timer = CookingTimerState.of(0).start(inicio)

        assertNull("Sin tiempo que contar no hay ancla", timer.endAtElapsedMillis)
    }

    @Test
    fun el_restante_es_la_proyeccion_exacta_ante_latidos_tardios_y_desiguales() {
        val timer = CookingTimerState.of(10).start(inicio)

        // Lecturas inyectadas a 1,5 s, 3 s y 61 s del arranque: siempre `endAt - now`.
        assertEquals(598_500L, timer.at(inicio + 1_500L).remainingMillis)
        assertEquals(597_000L, timer.at(inicio + 3_000L).remainingMillis)
        assertEquals(539_000L, timer.at(inicio + 61_000L).remainingMillis)
        assertEquals(CookingTimerStatus.RUNNING, timer.at(inicio + 61_000L).status)
    }

    @Test
    fun la_deriva_acumulada_es_cero() {
        val timer = CookingTimerState.of(10).start(inicio)

        // Un latido de 12 s no descuenta 12 s del valor anterior: la proyeccion ignora el delta.
        val trasLatidoTardio = timer.at(inicio + 12_000L)

        assertEquals(588_000L, trasLatidoTardio.remainingMillis)
        assertEquals(
            "La proyeccion no depende de cuantas lecturas se hayan hecho",
            trasLatidoTardio.remainingMillis,
            timer.at(inicio + 1_000L).at(inicio + 12_000L).remainingMillis,
        )
    }

    @Test
    fun al_alcanzar_o_pasar_el_fin_queda_terminado_y_nunca_en_negativo() {
        val timer = CookingTimerState.of(10).start(inicio)

        val exacto = timer.at(inicio + 600_000L)
        assertEquals(CookingTimerStatus.FINISHED, exacto.status)
        assertTrue(exacto.isFinished)
        assertEquals(0L, exacto.remainingMillis)
        assertNull("Terminado suelta el ancla", exacto.endAtElapsedMillis)

        val tarde = timer.at(inicio + 9_999_999L)
        assertEquals(CookingTimerStatus.FINISHED, tarde.status)
        assertEquals(0L, tarde.remainingMillis)
    }

    @Test
    fun las_lecturas_posteriores_al_fin_son_no_op() {
        val terminado = CookingTimerState.of(1).start(inicio).at(inicio + 90_000L)

        assertEquals(CookingTimerStatus.FINISHED, terminado.status)
        assertEquals(terminado, terminado.at(inicio + 90_001L))
        assertEquals(0L, terminado.at(inicio + 90_001L).remainingMillis)
    }

    @Test
    fun pausar_congela_el_tiempo_en_el_instante_y_anula_el_ancla() {
        val enMarcha = CookingTimerState.of(10).start(inicio)

        val pausado = enMarcha.pause(inicio + 120_000L)

        assertEquals(CookingTimerStatus.PAUSED, pausado.status)
        assertFalse(pausado.isRunning)
        assertEquals("Congela lo que quedaba en ese instante", 480_000L, pausado.remainingMillis)
        assertNull("Pausado no proyecta fin", pausado.endAtElapsedMillis)
        assertEquals("Y las lecturas posteriores no lo mueven", pausado, pausado.at(inicio + 900_000L))
    }

    @Test
    fun reanudar_reancla_el_fin_desde_lo_congelado() {
        val pausado = CookingTimerState.of(10).start(inicio).pause(inicio + 300_000L)

        val reanudado = pausado.resume(inicio + 500_000L)

        assertEquals(CookingTimerStatus.RUNNING, reanudado.status)
        assertEquals(300_000L, reanudado.remainingMillis)
        assertEquals("Reanuda anclando en el instante nuevo", inicio + 800_000L, reanudado.endAtElapsedMillis)
        assertEquals(298_000L, reanudado.at(inicio + 502_000L).remainingMillis)
    }

    @Test
    fun reset_vuelve_a_inactivo_con_el_total_y_sin_ancla_desde_cualquier_estado() {
        val enMarcha = CookingTimerState.of(10).start(inicio)
        val terminado = enMarcha.at(inicio + 600_000L)

        val desdeMarcha = enMarcha.reset()
        val desdeTerminado = terminado.reset()

        assertEquals(CookingTimerStatus.IDLE, desdeMarcha.status)
        assertEquals(600_000L, desdeMarcha.remainingMillis)
        assertNull(desdeMarcha.endAtElapsedMillis)
        assertEquals(CookingTimerStatus.IDLE, desdeTerminado.status)
        assertEquals(600_000L, desdeTerminado.remainingMillis)
        assertNull(desdeTerminado.endAtElapsedMillis)
    }

    @Test
    fun projected_end_prefiere_el_ancla_y_cae_al_restante_sin_ella() {
        val enMarcha = CookingTimerState.of(10).start(inicio)

        assertEquals(inicio + 600_000L, enMarcha.projectedEndMillis(inicio + 999_999L))
        assertEquals(
            "Sin ancla (fila defensiva) proyecta now + remaining",
            (inicio + 999_999L) + 600_000L,
            enMarcha.copy(endAtElapsedMillis = null).projectedEndMillis(inicio + 999_999L),
        )
        assertNull("Inactivo no proyecta fin", CookingTimerState.of(10).projectedEndMillis(inicio))
        assertNull(
            "Pausado tampoco",
            enMarcha.pause(inicio + 1_000L).projectedEndMillis(inicio + 1_000L),
        )
        assertNull(
            "Un paso sin tiempo que contar no proyecta fin",
            CookingTimerState.of(0).copy(status = CookingTimerStatus.RUNNING).projectedEndMillis(inicio),
        )
    }

    @Test
    fun las_transiciones_invalidas_son_no_op() {
        val inactivo = CookingTimerState.of(10)
        val enMarcha = inactivo.start(inicio)
        val pausado = enMarcha.pause(inicio + 1_000L)
        val terminado = CookingTimerState.of(1).start(inicio).at(inicio + 60_000L)

        // start solo vale desde IDLE.
        assertEquals(CookingTimerStatus.RUNNING, inactivo.start(inicio).status)
        assertEquals(enMarcha, enMarcha.start(inicio + 5_000L))
        assertEquals(pausado, pausado.start(inicio + 5_000L))
        assertEquals(terminado, terminado.start(inicio + 5_000L))
        // pause solo vale desde RUNNING.
        assertEquals(inactivo, inactivo.pause(inicio))
        assertEquals(pausado, pausado.pause(inicio + 5_000L))
        assertEquals(terminado, terminado.pause(inicio + 5_000L))
        // resume solo vale desde PAUSED.
        assertEquals(inactivo, inactivo.resume(inicio))
        assertEquals(enMarcha, enMarcha.resume(inicio + 5_000L))
        assertEquals(terminado, terminado.resume(inicio + 5_000L))
    }

    @Test
    fun at_fuera_de_en_marcha_o_sin_ancla_es_no_op() {
        val inactivo = CookingTimerState.of(10)
        val pausado = inactivo.start(inicio).pause(inicio + 1_000L)
        val terminado = CookingTimerState.of(1).start(inicio).at(inicio + 60_000L)
        val sinAncla = CookingTimerState.of(10).start(inicio).copy(endAtElapsedMillis = null)

        assertEquals(inactivo, inactivo.at(inicio + 5_000L))
        assertEquals(pausado, pausado.at(inicio + 5_000L))
        assertEquals(terminado, terminado.at(inicio + 5_000L))
        assertEquals(
            "Defensivo: sin ancla se conserva el restante guardado (no lanza)",
            sinAncla,
            sinAncla.at(inicio + 5_000L),
        )
        assertEquals(600_000L, sinAncla.at(inicio + 5_000L).remainingMillis)
    }

    @Test
    fun las_operaciones_no_mutan_el_estado_original() {
        val original = CookingTimerState.of(10)

        val cambiado = original.start(inicio).at(inicio + 1_000L)

        assertEquals(CookingTimerStatus.IDLE, original.status)
        assertEquals(600_000L, original.remainingMillis)
        assertNull(original.endAtElapsedMillis)
        assertNotEquals(original, cambiado)
    }
}
