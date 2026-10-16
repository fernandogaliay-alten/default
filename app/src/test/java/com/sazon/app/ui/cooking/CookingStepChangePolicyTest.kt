package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision **pura** de la confirmacion al cambiar de paso en el Modo cocina
 * (`docs/specs/cooking-mode-step-change-confirmation.md`, escenarios 1, 2, 5 y 6: **la verificacion
 * exigida**).
 *
 * Logica pura sin Compose ni Android y sin leer el reloj: la matriz de estados de [CookingModeState] se
 * recorre **por las dos direcciones** (`NEXT` y `PREVIOUS`) con asercion explicita por direccion, sobre
 * un paso **intermedio** (para que ambas sean posibles) y ademas sobre el primero y el ultimo (donde una
 * de las dos direcciones es un no-op). Los casos negativos fallarian si la politica aprobara siempre o si
 * copiara la condicion de [CookingExitPolicy] (`completedOrders`). Mismo patron que `CookingExitPolicyTest`.
 */
class CookingStepChangePolicyTest {

    @Test
    fun en_marcha_en_un_paso_intermedio_pide_confirmacion_en_las_dos_direcciones() {
        val enMarcha = enPasoIntermedioCon(CookingTimerState.of(10).start(0L))

        assertEquals(CookingTimerStatus.RUNNING, enMarcha.timer.status)
        assertAmbasDirecciones(enMarcha, esperado = true)
    }

    @Test
    fun pausado_en_un_paso_intermedio_pide_confirmacion_en_las_dos_direcciones() {
        val pausado = enPasoIntermedioCon(CookingTimerState.of(10).start(0L).at(120_000L).pause(120_000L))

        assertEquals(CookingTimerStatus.PAUSED, pausado.timer.status)
        assertTrue(pausado.completedOrders.isEmpty())
        assertAmbasDirecciones(pausado, esperado = true)
    }

    @Test
    fun inactivo_sin_marcados_no_pide_confirmacion_en_ninguna_direccion() {
        val inactivo = enPasoIntermedioCon(CookingTimerState.of(10))

        assertEquals(CookingTimerStatus.IDLE, inactivo.timer.status)
        assertAmbasDirecciones(inactivo, esperado = false)
    }

    @Test
    fun terminado_en_un_paso_intermedio_no_pide_confirmacion_en_ninguna_direccion() {
        val terminado = enPasoIntermedioCon(CookingTimerState.of(1).start(0L).at(60_000L))

        assertEquals(CookingTimerStatus.FINISHED, terminado.timer.status)
        assertAmbasDirecciones(terminado, esperado = false)
    }

    /**
     * Caso negativo clave: el **marcado** (`completedOrders`) **no** cuenta, porque persiste entre pasos
     * (nada que perder por el). Este test fallaria si la politica copiara la condicion de
     * [CookingExitPolicy].
     */
    @Test
    fun inactivo_con_marcados_no_pide_confirmacion_en_ninguna_direccion() {
        val inactivoConMarcados = enPasoIntermedioCon(CookingTimerState.of(10))
            .toggleCompleted(1)
            .toggleCompleted(2)

        assertEquals(CookingTimerStatus.IDLE, inactivoConMarcados.timer.status)
        assertTrue(inactivoConMarcados.completedOrders.isNotEmpty())
        assertAmbasDirecciones(inactivoConMarcados, esperado = false)
    }

    @Test
    fun terminado_con_marcados_no_pide_confirmacion_en_ninguna_direccion() {
        val terminadoConMarcados = enPasoIntermedioCon(CookingTimerState.of(1).start(0L).at(60_000L))
            .toggleCompleted(1)

        assertEquals(CookingTimerStatus.FINISHED, terminadoConMarcados.timer.status)
        assertAmbasDirecciones(terminadoConMarcados, esperado = false)
    }

    @Test
    fun un_paso_sin_duracion_no_pide_confirmacion() {
        val sinDuracion = enPasoIntermedioCon(CookingTimerState.of(0))

        assertFalse(sinDuracion.timer.hasDuration)
        assertAmbasDirecciones(sinDuracion, esperado = false)
    }

    @Test
    fun el_primer_paso_no_pide_confirmacion_al_retroceder_pero_si_al_avanzar() {
        val primerPasoEnMarcha = CookingModeState.initial(recetaDeTresPasos)
            .withTimer(CookingTimerState.of(10).start(0L))

        assertTrue(primerPasoEnMarcha.isFirstStep)
        assertFalse(
            "PREVIOUS (no-op en el primer paso)",
            CookingStepChangePolicy.requiresConfirmation(
                primerPasoEnMarcha,
                CookingStepChangePoint.PREVIOUS,
            ),
        )
        assertTrue(
            "NEXT (si cambia de paso)",
            CookingStepChangePolicy.requiresConfirmation(
                primerPasoEnMarcha,
                CookingStepChangePoint.NEXT,
            ),
        )
    }

    @Test
    fun el_ultimo_paso_no_pide_confirmacion_al_avanzar_pero_si_al_retroceder() {
        val ultimoPasoEnMarcha = CookingModeState.initial(recetaDeTresPasos)
            .next(recetaDeTresPasos.steps)
            .next(recetaDeTresPasos.steps)
            .withTimer(CookingTimerState.of(5).start(0L))

        assertTrue(ultimoPasoEnMarcha.isLastStep)
        assertFalse(
            "NEXT (no-op en el ultimo paso: la primaria lee 'Terminar' y va por la salida)",
            CookingStepChangePolicy.requiresConfirmation(
                ultimoPasoEnMarcha,
                CookingStepChangePoint.NEXT,
            ),
        )
        assertTrue(
            "PREVIOUS (si cambia de paso)",
            CookingStepChangePolicy.requiresConfirmation(
                ultimoPasoEnMarcha,
                CookingStepChangePoint.PREVIOUS,
            ),
        )
    }

    @Test
    fun una_receta_de_un_solo_paso_no_pide_confirmacion_en_ninguna_direccion() {
        val receta = recetaCon(
            listOf(RecipeStep(order = 1, title = "Unico", description = "", minutes = 10)),
        )
        // Aunque el temporizador corra, ninguna direccion cambia de paso (es primero y ultimo a la vez).
        val unicoPaso = CookingModeState.initial(receta).withTimer(CookingTimerState.of(10).start(0L))

        assertTrue(unicoPaso.isFirstStep)
        assertTrue(unicoPaso.isLastStep)
        assertAmbasDirecciones(unicoPaso, esperado = false)
    }

    @Test
    fun una_receta_sin_pasos_no_pide_confirmacion_en_ninguna_direccion() {
        // Caso defensivo: el dominio garantiza `1..n`, pero con `isEmpty` ninguna direccion navega.
        val sinPasos = CookingModeState.initial(recetaCon(emptyList()))
            .withTimer(CookingTimerState.of(5).start(0L))

        assertTrue(sinPasos.isEmpty)
        assertAmbasDirecciones(sinPasos, esperado = false)
    }

    @Test
    fun desmarcar_no_cambia_la_decision_con_el_temporizador_en_marcha() {
        val enMarcha = enPasoIntermedioCon(CookingTimerState.of(10).start(0L))
        val marcado = enMarcha.toggleCompleted(1)
        val desmarcado = marcado.toggleCompleted(1)

        assertEquals(enMarcha, desmarcado)
        assertAmbasDirecciones(marcado, esperado = true)
        assertAmbasDirecciones(desmarcado, esperado = true)
    }

    @Test
    fun los_ticks_no_cambian_la_decision_mientras_corre_el_temporizador() {
        var timer = CookingTimerState.of(10).start(0L)
        val decisiones = (1..599).map { segundo ->
            timer = timer.at(segundo * 1_000L)
            val decision = ambasDirecciones(enPasoIntermedioCon(timer))
            assertEquals("Segundo $segundo", CookingTimerStatus.RUNNING, timer.status)
            decision
        }

        assertTrue("Todos los ticks intermedios piden confirmacion", decisiones.all { it })
    }

    @Test
    fun la_decision_es_determinista_y_no_muta_el_estado() {
        val estado = enPasoIntermedioCon(CookingTimerState.of(10).start(0L)).toggleCompleted(1)

        val primera = CookingStepChangePolicy.requiresConfirmation(estado, CookingStepChangePoint.NEXT)
        val segunda = CookingStepChangePolicy.requiresConfirmation(estado, CookingStepChangePoint.NEXT)

        assertEquals(primera, segunda)
        assertEquals(estado, enPasoIntermedioCon(CookingTimerState.of(10).start(0L)).toggleCompleted(1))
    }

    /** El microcopy vive en una constante pura del mismo paquete (sin recursos ni `strings.xml`). */
    @Test
    fun el_microcopy_del_dialogo_es_no_vacio_y_estable() {
        assertEquals("¿Cambiar de paso?", CookingStepChangeCopy.TITLE)
        assertEquals(
            "El temporizador de este paso se reiniciará con el del paso nuevo.",
            CookingStepChangeCopy.MESSAGE,
        )
        assertEquals("Cambiar de paso", CookingStepChangeCopy.CONFIRM)
        assertEquals("Seguir en este paso", CookingStepChangeCopy.DISMISS)
    }

    /**
     * Aserción explicita de la matriz **por las dos direcciones**: los casos negativos (`esperado =
     * false`) fallarian si la politica aprobara sin mirar el estado.
     */
    private fun assertAmbasDirecciones(state: CookingModeState, esperado: Boolean) {
        assertEquals(
            "NEXT con $state",
            esperado,
            CookingStepChangePolicy.requiresConfirmation(state, CookingStepChangePoint.NEXT),
        )
        assertEquals(
            "PREVIOUS con $state",
            esperado,
            CookingStepChangePolicy.requiresConfirmation(state, CookingStepChangePoint.PREVIOUS),
        )
    }

    private fun ambasDirecciones(state: CookingModeState): Boolean =
        CookingStepChangePolicy.requiresConfirmation(state, CookingStepChangePoint.NEXT) &&
            CookingStepChangePolicy.requiresConfirmation(state, CookingStepChangePoint.PREVIOUS)

    /** Estado del modo en el **paso intermedio** (indice 1) con el [timer] inyectado. */
    private fun enPasoIntermedioCon(timer: CookingTimerState): CookingModeState =
        CookingModeState.initial(recetaDeTresPasos)
            .next(recetaDeTresPasos.steps)
            .withTimer(timer)

    private val recetaDeTresPasos = recetaCon(
        listOf(
            RecipeStep(order = 1, title = "Primero", description = "", minutes = 10),
            RecipeStep(order = 2, title = "Segundo", description = "", minutes = 8),
            RecipeStep(order = 3, title = "Tercero", description = "", minutes = 5),
        ),
    )

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
