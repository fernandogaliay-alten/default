package com.sazon.app.ui.cooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Maquina de estados del temporizador del modo cocina (`docs/specs/cooking-mode.md`, escenarios 3 y 4
 * y 6): cuenta atras determinista por `tick(delta)`, pausa/reanudacion, terminado al llegar a cero
 * (sin seguir contando), `reset` y no-ops. Logica pura sin Compose ni Android.
 */
class CookingTimerStateTest {

    @Test
    fun of_diez_minutos_arranca_inactivo_con_la_duracion_total() {
        val timer = CookingTimerState.of(10)

        assertEquals(CookingTimerStatus.IDLE, timer.status)
        assertEquals(600_000L, timer.totalMillis)
        assertEquals(600_000L, timer.remainingMillis)
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
    fun arrancar_pasa_de_inactivo_a_en_marcha() {
        val timer = CookingTimerState.of(10).start()

        assertEquals(CookingTimerStatus.RUNNING, timer.status)
        assertTrue(timer.isRunning)
        assertEquals(600_000L, timer.remainingMillis)
    }

    @Test
    fun diez_ticks_de_un_segundo_descuentan_diez_segundos() {
        var timer = CookingTimerState.of(10).start()

        repeat(10) { timer = timer.tick(1_000L) }

        assertEquals(CookingTimerStatus.RUNNING, timer.status)
        assertEquals(590_000L, timer.remainingMillis)
        assertEquals(600_000L, timer.totalMillis)
    }

    @Test
    fun al_llegar_a_cero_pasa_a_terminado_y_deja_de_contar() {
        var timer = CookingTimerState.of(10).start()

        repeat(600) { timer = timer.tick(1_000L) }

        assertEquals(CookingTimerStatus.FINISHED, timer.status)
        assertTrue(timer.isFinished)
        assertEquals(0L, timer.remainingMillis)

        // Los ticks posteriores son no-op: no vuelve a contar ni a valores negativos.
        val despues = timer.tick(1_000L).tick(5_000L)
        assertEquals(CookingTimerStatus.FINISHED, despues.status)
        assertEquals(0L, despues.remainingMillis)
    }

    @Test
    fun un_tick_mayor_que_lo_restante_no_baja_de_cero() {
        val timer = CookingTimerState.of(1).start().tick(90_000L)

        assertEquals(CookingTimerStatus.FINISHED, timer.status)
        assertEquals(0L, timer.remainingMillis)
    }

    @Test
    fun pausar_congela_el_tiempo_y_reanudar_sigue_desde_ahi() {
        val enMarcha = CookingTimerState.of(10).start().tick(1_000L)

        val pausado = enMarcha.pause()

        assertEquals(CookingTimerStatus.PAUSED, pausado.status)
        assertFalse(pausado.isRunning)
        assertEquals(599_000L, pausado.remainingMillis)
        // Congelado: el tick no reduce nada mientras esta pausado.
        assertEquals(599_000L, pausado.tick(30_000L).remainingMillis)
        assertEquals(CookingTimerStatus.PAUSED, pausado.tick(30_000L).status)

        val reanudado = pausado.resume()

        assertEquals(CookingTimerStatus.RUNNING, reanudado.status)
        assertEquals(599_000L, reanudado.remainingMillis)
        assertEquals(598_000L, reanudado.tick(1_000L).remainingMillis)
    }

    @Test
    fun reset_vuelve_a_inactivo_con_el_total_desde_cualquier_estado() {
        val enMarcha = CookingTimerState.of(10).start().tick(1_000L)
        val terminado = CookingTimerState.of(10).start().tick(600_000L)

        val desdeMarcha = enMarcha.reset()
        val desdeTerminado = terminado.reset()

        assertEquals(CookingTimerStatus.IDLE, desdeMarcha.status)
        assertEquals(600_000L, desdeMarcha.remainingMillis)
        assertEquals(CookingTimerStatus.IDLE, desdeTerminado.status)
        assertEquals(600_000L, desdeTerminado.remainingMillis)
    }

    @Test
    fun las_transiciones_invalidas_son_no_op() {
        val inactivo = CookingTimerState.of(10)
        val enMarcha = inactivo.start()
        val pausado = enMarcha.pause()
        val terminado = CookingTimerState.of(1).start().tick(60_000L)

        // start solo vale desde IDLE.
        assertEquals(CookingTimerStatus.RUNNING, inactivo.start().status)
        assertEquals(enMarcha, enMarcha.start())
        assertEquals(pausado, pausado.start())
        assertEquals(terminado, terminado.start())
        // pause solo vale desde RUNNING.
        assertEquals(inactivo, inactivo.pause())
        assertEquals(pausado, pausado.pause())
        assertEquals(terminado, terminado.pause())
        // resume solo vale desde PAUSED.
        assertEquals(inactivo, inactivo.resume())
        assertEquals(enMarcha, enMarcha.resume())
        assertEquals(terminado, terminado.resume())
    }

    @Test
    fun tick_fuera_de_en_marcha_o_con_delta_no_positivo_es_no_op() {
        val inactivo = CookingTimerState.of(10)
        val pausado = inactivo.start().pause()
        val enMarcha = inactivo.start().tick(1_000L)

        assertEquals(inactivo, inactivo.tick(1_000L))
        assertEquals(pausado, pausado.tick(1_000L))
        assertEquals(enMarcha, enMarcha.tick(0L))
        assertEquals(enMarcha, enMarcha.tick(-5_000L))
    }

    @Test
    fun las_operaciones_no_mutan_el_estado_original() {
        val original = CookingTimerState.of(10)

        val cambiado = original.start().tick(1_000L)

        assertEquals(CookingTimerStatus.IDLE, original.status)
        assertEquals(600_000L, original.remainingMillis)
        assertNotEquals(original, cambiado)
    }
}
