package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sesion del Modo cocina: proyeccion del **instante de fin** y reanudacion del paso
 * (`docs/specs/cooking-timer-foreground-service.md`, escenarios 1, 3, 4 y 6; y
 * `docs/specs/cooking-timer-finish-notification.md`, escenarios 4 y 5). Es la verificacion
 * **exigida** por la entrada de `feature_list.json` de la feature del servicio y, en parte, de la del
 * aviso duradero.
 *
 * Logica **pura** sin Compose ni Android y **sin leer el reloj**: el instante entra siempre por
 * parametro (`nowMillis`), nunca del sistema. Mismo patron que `CookingAlarmPolicyTest`.
 */
class CookingSessionTest {

    @Test
    fun al_iniciar_la_sesion_proyecta_el_instante_de_fin() {
        val modo = CookingModeState.initial(receta()).withTimer(CookingTimerState.of(10).start())

        val sesion = CookingSession.of(modo, receta(), nowMillis = 1_000_000L)

        assertEquals("now + remaining (10 min)", 1_600_000L, sesion.endAtElapsedMillis)
        assertEquals(CookingTimerStatus.RUNNING, sesion.timer.status)
        assertTrue(sesion.isRunning)
        assertTrue(sesion.isResumable)
    }

    @Test
    fun al_reanudar_la_sesion_proyecta_el_nuevo_instante_de_fin() {
        val enMarcha = CookingTimerState.of(10).start().tick(300_000L)
        val pausado = enMarcha.pause()
        val reanudado = pausado.resume()

        val alPausar = CookingSession.of(modoCon(pausado), receta(), nowMillis = 1_500_000L)
        val alReanudar = CookingSession.of(modoCon(reanudado), receta(), nowMillis = 2_000_000L)

        assertNull("Pausado no proyecta fin (nada que rearrancar)", alPausar.endAtElapsedMillis)
        assertTrue("Pausado es reanudable", alPausar.isResumable)
        assertEquals("Reanudar proyecta lo que queda desde el instante nuevo", 2_300_000L, alReanudar.endAtElapsedMillis)
    }

    @Test
    fun inactivo_pausado_y_terminado_no_proyectan_instante_de_fin() {
        val inactivo = CookingTimerState.of(10)
        val pausado = inactivo.start().pause()
        val terminado = inactivo.start().tick(600_000L)

        assertNull(CookingSession.of(modoCon(inactivo), receta(), 1_000L).endAtElapsedMillis)
        assertNull(CookingSession.of(modoCon(pausado), receta(), 1_000L).endAtElapsedMillis)
        assertNull(CookingSession.of(modoCon(terminado), receta(), 1_000L).endAtElapsedMillis)
        assertFalse(CookingSession.of(modoCon(inactivo), receta(), 1_000L).isResumable)
        assertTrue(CookingSession.of(modoCon(pausado), receta(), 1_000L).isResumable)
        assertFalse("Un Terminado no es resumible", CookingSession.of(modoCon(terminado), receta(), 1_000L).isResumable)
    }

    @Test
    fun un_temporizador_en_marcha_sin_tiempo_que_contar_no_proyecta_fin() {
        val enMarcha = CookingTimerState.of(0).copy(status = CookingTimerStatus.RUNNING)

        val sesion = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 1_000L)

        assertNull("Sin tiempo que contar no hay nada que rearrancar", sesion.endAtElapsedMillis)
        assertNull(sesion.projectedEndMillis(1_000L))
    }

    @Test
    fun reanudar_recalcula_lo_que_queda_desde_el_instante_de_fin() {
        val enMarcha = CookingTimerState.of(10).start()
        val sesion = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 1_000_000L)

        val reanudada = sesion.resumedMode(nowMillis = 1_240_000L)

        assertEquals(CookingTimerStatus.RUNNING, reanudada.timer.status)
        assertEquals("Han pasado 240 s: quedan 360 s", 360_000L, reanudada.timer.remainingMillis)
        assertEquals(sesion.stepIndex, reanudada.stepIndex)
        assertEquals(sesion.completedOrders, reanudada.completedOrders)
    }

    @Test
    fun si_el_instante_de_fin_ya_paso_la_sesion_se_reanuda_terminada() {
        val enMarcha = CookingTimerState.of(10).start()
        val sesion = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 1_000_000L)

        val reanudada = sesion.resumedMode(nowMillis = 1_600_000L)
        val reanudadaTarde = sesion.resumedMode(nowMillis = 9_999_999_999L)

        assertEquals("El instante exacto ya es cero", CookingTimerStatus.FINISHED, reanudada.timer.status)
        assertEquals(0L, reanudada.timer.remainingMillis)
        assertEquals("Mucho despues sigue a cero", CookingTimerStatus.FINISHED, reanudadaTarde.timer.status)
        assertEquals(0L, reanudadaTarde.timer.remainingMillis)
    }

    @Test
    fun la_muerte_del_proceso_a_mitad_reanuda_en_marcha_con_el_tiempo_restante() {
        // La sesion persistida es la de "En marcha": el proceso muere antes de que la app llegue a cero.
        val enMarcha = CookingTimerState.of(10).start().tick(120_000L)
        val persistida = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 5_000L)

        val reanudada = persistida.resumedMode(nowMillis = 125_000L)

        assertEquals(CookingTimerStatus.RUNNING, reanudada.timer.status)
        assertEquals(360_000L, reanudada.timer.remainingMillis)
    }

    @Test
    fun reanudar_en_pausa_o_inactivo_reconstruye_lo_persistido_tal_cual() {
        val pausado = CookingTimerState.of(10).start().tick(120_000L).pause()
        val inactivo = CookingTimerState.of(10)

        assertEquals(pausado, CookingSession.of(modoCon(pausado), receta(), 0L).resumedMode(999_999L).timer)
        assertEquals(inactivo, CookingSession.of(modoCon(inactivo), receta(), 0L).resumedMode(999_999L).timer)
    }

    @Test
    fun una_sesion_en_marcha_sin_instante_de_fin_degrada_al_tiempo_persistido_sin_lanzar() {
        val enMarcha = CookingTimerState.of(10).start().tick(120_000L)
        val corrupta = CookingSession(
            recipe = receta(),
            stepIndex = 1,
            completedOrders = setOf(1),
            timer = enMarcha,
            endAtElapsedMillis = null,
        )

        val reanudada = corrupta.resumedMode(nowMillis = 9_999_999_999L)

        assertEquals(CookingTimerStatus.RUNNING, reanudada.timer.status)
        assertEquals(
            "Sin instante de fin se conserva el restante persistido",
            480_000L,
            reanudada.timer.remainingMillis,
        )
    }

    @Test
    fun el_paso_el_marcado_y_la_receta_viajan_intactos() {
        val receta = receta()
        val modo = CookingModeState(
            stepIndex = 1,
            totalSteps = receta.steps.size,
            completedOrders = setOf(1),
            timer = CookingTimerState.of(0),
        )

        val sesion = CookingSession.of(modo, receta, nowMillis = 42L)

        assertEquals(receta, sesion.recipe)
        assertEquals(1, sesion.stepIndex)
        assertEquals(setOf(1), sesion.completedOrders)
        assertEquals(modo, sesion.mode())
        assertEquals("El modo persistido no toca la cuenta atras", modo.timer, sesion.mode().timer)
    }

    @Test
    fun la_proyeccion_del_fin_y_la_reanudacion_son_deterministas_con_el_instante_inyectado() {
        val enMarcha = CookingTimerState.of(10).start()

        val primera = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 7_777L)
        val segunda = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 7_777L)

        assertEquals(primera, segunda)
        assertEquals(607_777L, primera.endAtElapsedMillis)
        assertEquals(primera.resumedMode(8_000L), segunda.resumedMode(8_000L))
        assertEquals(1_147_777L, primera.projectedEndMillis(547_777L))
    }

    @Test
    fun terminado_no_proyecta_fin_y_la_politica_deja_la_alarma_viva_sin_reprogramar() {
        // "Aviso duradero" (Scenario 1 y 3): al llegar a cero la sesion deja de proyectar fin, la
        // politica **no** cancela (el aviso de fin lo publica la alarma, que se deja viva) y los ticks
        // posteriores devuelven `None`.
        val enMarcha = CookingTimerState.of(10).start()
        val terminado = enMarcha.tick(600_000L)
        val sesionTerminada = CookingSession.of(modoCon(terminado), receta(), nowMillis = 1_600_000L)

        assertNull(sesionTerminada.endAtElapsedMillis)
        assertNull(sesionTerminada.projectedEndMillis(1_600_000L))
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(enMarcha, terminado, nowMillis = 1_600_000L),
        )
        assertEquals(
            "Los ticks de un Terminado no reprograman nada",
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(terminado, terminado.tick(1_000L), nowMillis = 1_601_000L),
        )
    }

    @Test
    fun reabre_desde_la_notificacion_en_marcha_pausado_y_terminado_pero_no_inactivo() {
        val inactivo = CookingTimerState.of(10)
        val enMarcha = inactivo.start()
        val pausado = inactivo.start().pause()
        val terminado = inactivo.start().tick(600_000L)

        assertTrue(CookingSession.of(modoCon(enMarcha), receta(), 1_000L).reopensFromNotification)
        assertTrue(CookingSession.of(modoCon(pausado), receta(), 1_000L).reopensFromNotification)
        assertTrue(
            "Terminado tambien reabre el paso desde el aviso de fin",
            CookingSession.of(modoCon(terminado), receta(), 1_000L).reopensFromNotification,
        )
        assertFalse(
            "Inactivo no tiene nada que reabrir",
            CookingSession.of(modoCon(inactivo), receta(), 1_000L).reopensFromNotification,
        )
    }

    @Test
    fun una_sesion_terminada_reabre_en_el_paso_con_cero_y_sin_rearmar() {
        val terminado = CookingTimerState.of(10).start().tick(600_000L)
        val sesion = CookingSession.of(modoCon(terminado), receta(), nowMillis = 1_600_000L)

        val reabierta = sesion.resumedMode(nowMillis = 1_640_000L)

        assertTrue(sesion.reopensFromNotification)
        assertEquals(CookingTimerStatus.FINISHED, reabierta.timer.status)
        assertEquals(0L, reabierta.timer.remainingMillis)
        assertEquals("Sin instante de fin proyectado no hay nada que rearrancar", null, sesion.endAtElapsedMillis)
        // La reanudacion de una sesion Terminada no rearma alarma ni servicio: `IDLE -> FINISHED` no
        // programa nada.
        assertEquals(
            CookingAlarmCommand.None,
            CookingAlarmPolicy.onTimerChange(
                previous = sesion.timer.copy(status = CookingTimerStatus.IDLE),
                next = reabierta.timer,
                nowMillis = 1_640_000L,
            ),
        )
    }

    @Test
    fun una_sesion_terminada_reanudada_como_paso_sigue_terminada_y_no_rearma() {
        val terminado = CookingTimerState.of(10).start().tick(600_000L)
        val sesion = CookingSession.of(modoCon(terminado), receta(), nowMillis = 1_600_000L)

        val reanudada = sesion.resumedMode(nowMillis = 1_600_000L)

        assertEquals(CookingTimerStatus.FINISHED, reanudada.timer.status)
        assertFalse("Terminado no es `isResumable` por si solo (semantica aceptada)", sesion.isResumable)
        assertTrue("Pero si reabre desde la notificacion", sesion.reopensFromNotification)
    }

    @Test
    fun el_aviso_se_proyecta_y_se_rearma_con_la_misma_decision_pura_al_reanudar() {
        // Reanudar una sesion En marcha se trata como `IDLE -> RUNNING` (el servicio y la alarma
        // murieron con el proceso): la politica da el mismo `Schedule(now + remaining)`.
        val enMarcha = CookingTimerState.of(10).start().tick(120_000L)
        val sesion = CookingSession.of(modoCon(enMarcha), receta(), nowMillis = 10_000L)

        val comando = CookingAlarmPolicy.onTimerChange(
            previous = sesion.timer.copy(status = CookingTimerStatus.IDLE),
            next = sesion.resumedMode(nowMillis = 20_000L).timer,
            nowMillis = 20_000L,
        )

        assertEquals(CookingAlarmCommand.Schedule(490_000L), comando)
    }
}

private fun modoCon(timer: CookingTimerState) = CookingModeState(
    stepIndex = 0,
    totalSteps = 2,
    completedOrders = emptySet(),
    timer = timer,
)

private fun receta() = Recipe(
    id = "seed-pasta",
    source = RecipeSource.SEED,
    title = "Pasta con tomate",
    description = "Receta semilla de prueba.",
    categoryId = "pasta",
    mealTypeId = "lunch",
    cuisineId = "italian",
    totalMinutes = 25,
    prepMinutes = 5,
    cookMinutes = 20,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = listOf(
        RecipeStep(1, "Cocer", "Cuece la pasta.", 10),
        RecipeStep(2, "Servir", "Sirve con la salsa.", 0),
    ),
    declaredAllergens = emptyList(),
)
