package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politica **pura** del aviso de sistema del temporizador (`docs/specs/cooking-timer-alarm.md`,
 * escenarios 1, 4, 5, 7 y 8, y `docs/specs/cooking-timer-finish-notification.md`, escenarios 1 y 3):
 * programar al Iniciar/Reanudar para el **instante de fin** (`now + remaining`), cancelar al
 * Pausar/Reiniciar/cambiar de paso/salir, **dejar viva la alarma al llegar a cero** (el `tick` que cruza
 * el cero ya **no** cancela, para no adelantarse al disparo de `AlarmManager`) y **no** reprogramar en
 * los ticks.
 *
 * Logica pura sin Compose ni Android: el instante entra siempre por parametro (`nowMillis`), nunca se
 * lee el reloj. Mismo patron que `CookingTimerStateTest`.
 */
class CookingAlarmPolicyTest {

    @Test
    fun al_iniciar_se_programa_el_aviso_en_el_instante_de_fin() {
        val inactivo = CookingTimerState.of(10)

        val comando = CookingAlarmPolicy.onTimerChange(
            previous = inactivo,
            next = inactivo.start(),
            nowMillis = 1_000_000L,
        )

        assertEquals(CookingAlarmCommand.Schedule(1_600_000L), comando)
    }

    @Test
    fun el_instante_de_fin_sale_del_parametro_y_nunca_del_reloj() {
        val inactivo = CookingTimerState.of(10)
        val enMarcha = inactivo.start()

        // Mismo estado de entrada, distinto `nowMillis`: el instante de fin se mueve con el parametro.
        assertEquals(
            CookingAlarmCommand.Schedule(600_000L),
            CookingAlarmPolicy.onTimerChange(inactivo, enMarcha, nowMillis = 0L),
        )
        assertEquals(
            CookingAlarmCommand.Schedule(9_400_600_000L),
            CookingAlarmPolicy.onTimerChange(inactivo, enMarcha, nowMillis = 9_400_000_000L),
        )
    }

    @Test
    fun la_decision_es_determinista_para_el_mismo_now_inyectado() {
        val inactivo = CookingTimerState.of(10)
        val enMarcha = inactivo.start()

        val primera = CookingAlarmPolicy.onTimerChange(inactivo, enMarcha, nowMillis = 7_777L)
        val segunda = CookingAlarmPolicy.onTimerChange(inactivo, enMarcha, nowMillis = 7_777L)

        assertEquals(primera, segunda)
        assertEquals(CookingAlarmCommand.Schedule(607_777L), primera)
    }

    @Test
    fun al_reanudar_se_reprograma_desde_el_tiempo_congelado() {
        val enMarcha = CookingTimerState.of(10).start().tick(300_000L)
        val pausado = enMarcha.pause()

        val alPausar = CookingAlarmPolicy.onTimerChange(enMarcha, pausado, nowMillis = 1_500_000L)
        val alReanudar = CookingAlarmPolicy.onTimerChange(pausado, pausado.resume(), nowMillis = 2_000_000L)

        assertEquals(CookingAlarmCommand.Cancel, alPausar)
        assertEquals(CookingAlarmCommand.Schedule(2_300_000L), alReanudar)
    }

    @Test
    fun al_pausar_se_cancela_el_aviso() {
        val enMarcha = CookingTimerState.of(10).start().tick(60_000L)

        val comando = CookingAlarmPolicy.onTimerChange(enMarcha, enMarcha.pause(), nowMillis = 1_100_000L)

        assertEquals(CookingAlarmCommand.Cancel, comando)
    }

    @Test
    fun al_reiniciar_desde_en_marcha_pausado_o_terminado_se_cancela_el_aviso() {
        val enMarcha = CookingTimerState.of(10).start().tick(120_000L)
        val pausado = CookingTimerState.of(10).start().tick(120_000L).pause()
        val terminado = CookingTimerState.of(1).start().tick(60_000L)

        assertEquals(
            CookingAlarmCommand.Cancel,
            CookingAlarmPolicy.onTimerChange(enMarcha, enMarcha.reset(), nowMillis = 1_200_000L),
        )
        assertEquals(
            CookingAlarmCommand.Cancel,
            CookingAlarmPolicy.onTimerChange(pausado, pausado.reset(), nowMillis = 1_200_000L),
        )
        assertEquals(
            CookingAlarmCommand.Cancel,
            CookingAlarmPolicy.onTimerChange(terminado, terminado.reset(), nowMillis = 1_200_000L),
        )
    }

    @Test
    fun cambiar_de_paso_cancela_el_aviso_del_paso_anterior() {
        val steps = listOf(
            RecipeStep(order = 1, title = "Primero", description = "", minutes = 10),
            RecipeStep(order = 2, title = "Segundo", description = "", minutes = 8),
        )
        val enPasoUno = CookingModeState.initial(recetaCon(steps))
            .withTimer(CookingTimerState.of(10).start().tick(120_000L))

        // "Siguiente" reinicia el temporizador al del paso nuevo (Inactivo): RUNNING -> IDLE.
        val enPasoDos = enPasoUno.next(steps)

        assertEquals(
            CookingAlarmCommand.Cancel,
            CookingAlarmPolicy.onTimerChange(enPasoUno.timer, enPasoDos.timer, nowMillis = 1_300_000L),
        )
    }

    @Test
    fun llegar_a_cero_no_cancela_la_alarma_y_no_reprograma() {
        val enMarcha = CookingTimerState.of(1).start()
        val medio = enMarcha.tick(30_000L)
        val terminado = medio.tick(30_000L)

        assertEquals(CookingTimerStatus.FINISHED, terminado.status)
        assertEquals(
            "El tick que cruza el cero deja la alarma viva (es la unica productora del aviso)",
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(medio, terminado, nowMillis = 2_000_000L),
        )
        // Los ticks posteriores al fin siguen siendo no-op del temporizador: tampoco tocan el aviso.
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(terminado, terminado.tick(1_000L), nowMillis = 2_001_000L),
        )
    }

    @Test
    fun reiniciar_desde_terminado_si_cancela_la_alarma() {
        // No-regresion de P1: la arista `FINISHED -> IDLE` ("Reiniciar" desde Terminado) es una
        // transicion real y sigue cancelando la alarma pendiente.
        val terminado = CookingTimerState.of(10).start().tick(600_000L)

        assertEquals(
            CookingAlarmCommand.Cancel,
            CookingAlarmPolicy.onTimerChange(terminado, terminado.reset(), nowMillis = 1_600_000L),
        )
    }

    @Test
    fun los_ticks_intermedios_no_reprograman_el_aviso() {
        var timer = CookingTimerState.of(10).start()
        val comandos = (1..599).map { segundo ->
            val siguiente = timer.tick(1_000L)
            val comando = CookingAlarmPolicy.onTimerChange(timer, siguiente, 1_000_000L + segundo)
            timer = siguiente
            comando
        }

        assertEquals(CookingTimerStatus.RUNNING, timer.status)
        assertTrue(
            "Ningun tick intermedio programa ni cancela",
            comandos.all { it == CookingAlarmCommand.None },
        )
    }

    @Test
    fun los_no_ops_del_temporizador_fuera_de_marcha_no_tocan_el_aviso() {
        val inactivo = CookingTimerState.of(10)
        val pausado = inactivo.start().pause()
        val terminado = CookingTimerState.of(1).start().tick(60_000L)

        // `pause()`/`resume()` desde Inactivo y `pause()`/`start()` desde Pausado son no-op del
        // temporizador: devuelven el **mismo** estado, no hay transicion real y no se cancela nada.
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(inactivo, inactivo.pause(), nowMillis = 5L),
        )
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(inactivo, inactivo.resume(), nowMillis = 5L),
        )
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(pausado, pausado.pause(), nowMillis = 5L),
        )
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(pausado, pausado.start(), nowMillis = 5L),
        )
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(terminado, terminado.tick(1_000L), nowMillis = 5L),
        )
    }

    @Test
    fun el_doble_iniciar_no_op_no_reprograma_nada() {
        val enMarcha = CookingTimerState.of(10).start()

        // Segundo "Iniciar" con el temporizador En marcha: `start()` es no-op y devuelve el mismo estado.
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(enMarcha, enMarcha.start(), nowMillis = 1_500_000L),
        )
    }

    @Test
    fun un_paso_sin_duracion_nunca_programa_alarma() {
        val sinDuracion = CookingTimerState.of(0)

        assertTrue(
            "Sin `Iniciar` alcanzable, un paso sin duracion no llega ni a RUNNING",
            !sinDuracion.hasDuration,
        )
        // Defensivo: aunque se arrancara, no hay instante de fin que programar.
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(sinDuracion, sinDuracion.start(), nowMillis = 1_000L),
        )
    }

    @Test
    fun salir_del_modo_cocina_cancela_el_aviso() {
        assertEquals(CookingAlarmCommand.Cancel, CookingAlarmPolicy.onExit())
    }

    @Test
    fun la_corrida_completa_emite_un_solo_schedule_y_ningun_cancel() {
        var timer = CookingTimerState.of(10)
        val comandos = mutableListOf<CookingAlarmCommand>()

        // Iniciar: el unico Schedule de toda la corrida.
        val iniciado = timer.start()
        comandos += CookingAlarmPolicy.onTimerChange(timer, iniciado, nowMillis = 1_000_000L)
        timer = iniciado

        // 600 ticks hasta cero (FINISHED) y 2 ticks no-op posteriores.
        repeat(602) { indice ->
            val siguiente = timer.tick(1_000L)
            comandos += CookingAlarmPolicy.onTimerChange(timer, siguiente, 1_000_000L + (indice + 1) * 1_000L)
            timer = siguiente
        }

        assertEquals(CookingTimerStatus.FINISHED, timer.status)
        assertEquals(0L, timer.remainingMillis)
        assertEquals("Un unico aviso programado", 1, comandos.count { it is CookingAlarmCommand.Schedule })
        assertEquals(CookingAlarmCommand.Schedule(1_600_000L), comandos.first())
        assertEquals(
            "El cruce del cero ya no cancela la alarma pendiente (P1)",
            0,
            comandos.count { it == CookingAlarmCommand.Cancel },
        )
        assertTrue(
            "Ningun comando cancela en toda la corrida",
            comandos.none { it is CookingAlarmCommand.Cancel },
        )
        assertEquals(
            "Ninguna reprogramacion tras FINISHED",
            0,
            comandos.drop(601).count { it is CookingAlarmCommand.Schedule },
        )
    }

    @Test
    fun la_costura_por_defecto_no_hace_nada() {
        // El default no-op deja la pantalla sin Android y sin efectos; da igual el comando.
        NoOpCookingAlarm.apply(CookingAlarmCommand.Schedule(1L))
        NoOpCookingAlarm.apply(CookingAlarmCommand.Cancel)
        NoOpCookingAlarm.apply(CookingAlarmCommand.None)
        // El comando de limpieza del aviso de fin tampoco tiene efecto (y no lanza).
        NoOpCookingAlarm.dismissFinishNotification()
    }

    private fun recetaCon(steps: List<RecipeStep>): Recipe = Recipe(
        id = "receta-1",
        source = RecipeSource.SEED,
        title = "Receta de prueba",
        description = "",
        categoryId = "other",
        mealTypeId = "lunch",
        cuisineId = "spanish",
        totalMinutes = 30,
        prepMinutes = 10,
        cookMinutes = 20,
        difficultyId = "easy",
        servings = 4,
        ingredients = emptyList(),
        steps = steps,
    )
}
