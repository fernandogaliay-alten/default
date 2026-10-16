package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision **pura** de la confirmacion al salir del Modo cocina
 * (`docs/specs/cooking-mode-exit-confirmation.md`, escenarios 1, 2, 6 y 7: **la verificacion exigida**).
 *
 * Logica pura sin Compose ni Android y sin leer el reloj: la matriz de estados de [CookingModeState] se
 * recorre **por los dos puntos de salida** (`BACK_BUTTON` y `FINISH`) con asercion explicita, y los casos
 * negativos fallarian si la politica aprobara siempre. Mismo patron que `CookingAlarmPolicyTest`.
 */
class CookingExitPolicyTest {

    @Test
    fun en_marcha_pide_confirmacion_por_los_dos_puntos_de_salida() {
        val enMarcha = enPasoCon(CookingTimerState.of(10).start())

        assertAmbosPuntos(enMarcha, esperado = true)
    }

    @Test
    fun pausado_sin_pasos_marcados_pide_confirmacion_por_los_dos_puntos_de_salida() {
        val pausado = enPasoCon(CookingTimerState.of(10).start().tick(120_000L).pause())

        assertEquals(CookingTimerStatus.PAUSED, pausado.timer.status)
        assertTrue(pausado.completedOrders.isEmpty())
        assertAmbosPuntos(pausado, esperado = true)
    }

    @Test
    fun inactivo_con_pasos_marcados_pide_confirmacion_por_los_dos_puntos_de_salida() {
        val inactivoConMarcado = enPasoCon(CookingTimerState.of(10)).toggleCompleted(1)

        assertEquals(CookingTimerStatus.IDLE, inactivoConMarcado.timer.status)
        assertAmbosPuntos(inactivoConMarcado, esperado = true)
    }

    @Test
    fun terminado_con_pasos_marcados_pide_confirmacion_por_los_dos_puntos_de_salida() {
        val terminadoConMarcado = enPasoCon(CookingTimerState.of(1).start().tick(60_000L))
            .toggleCompleted(1)

        assertEquals(CookingTimerStatus.FINISHED, terminadoConMarcado.timer.status)
        assertAmbosPuntos(terminadoConMarcado, esperado = true)
    }

    @Test
    fun un_ultimo_paso_inactivo_con_marcados_es_el_escenario_de_terminar() {
        val receta = recetaCon(
            listOf(
                RecipeStep(order = 1, title = "Primero", description = "", minutes = 10),
                RecipeStep(order = 2, title = "Segundo", description = "", minutes = 0),
            ),
        )
        // Ultimo paso (sin duracion: bloque del temporizador ni se pinta), Inactivo y con un paso marcado.
        val ultimoPaso = CookingModeState.initial(receta)
            .next(receta.steps)
            .toggleCompleted(1)

        assertTrue(ultimoPaso.isLastStep)
        assertEquals(CookingTimerStatus.IDLE, ultimoPaso.timer.status)
        assertAmbosPuntos(ultimoPaso, esperado = true)
    }

    @Test
    fun inactivo_sin_nada_que_perder_no_pide_confirmacion() {
        val inactivoVacio = enPasoCon(CookingTimerState.of(10))

        assertTrue(inactivoVacio.completedOrders.isEmpty())
        assertAmbosPuntos(inactivoVacio, esperado = false)
    }

    @Test
    fun un_paso_sin_duracion_sin_marcados_no_pide_confirmacion() {
        val sinDuracion = enPasoCon(CookingTimerState.of(0))

        assertFalse(sinDuracion.timer.hasDuration)
        assertAmbosPuntos(sinDuracion, esperado = false)
    }

    @Test
    fun terminado_solo_no_pide_confirmacion() {
        val terminadoSinMarcados = enPasoCon(CookingTimerState.of(1).start().tick(60_000L))

        assertEquals(CookingTimerStatus.FINISHED, terminadoSinMarcados.timer.status)
        assertTrue(terminadoSinMarcados.completedOrders.isEmpty())
        // El tiempo ya se consumio: un temporizador Terminado **solo** no es "algo que perder".
        assertAmbosPuntos(terminadoSinMarcados, esperado = false)
    }

    @Test
    fun una_receta_sin_pasos_no_pide_confirmacion() {
        val sinPasos = CookingModeState.initial(recetaCon(emptyList()))

        assertTrue(sinPasos.isEmpty)
        assertEquals(CookingTimerStatus.IDLE, sinPasos.timer.status)
        assertTrue(sinPasos.completedOrders.isEmpty())
        assertAmbosPuntos(sinPasos, esperado = false)
    }

    @Test
    fun desmarcar_el_ultimo_paso_vuelve_a_permitir_la_salida_directa() {
        val inactivo = enPasoCon(CookingTimerState.of(10))
        val marcado = inactivo.toggleCompleted(1)
        val desmarcado = marcado.toggleCompleted(1)

        assertAmbosPuntos(marcado, esperado = true)
        assertEquals(inactivo, desmarcado)
        assertAmbosPuntos(desmarcado, esperado = false)
    }

    @Test
    fun los_ticks_no_cambian_la_decision_mientras_corre_el_temporizador() {
        var timer = CookingTimerState.of(10).start()
        val decisiones = (1..599).map { segundo ->
            timer = timer.tick(1_000L)
            val decision = ambosPuntos(enPasoCon(timer))
            assertEquals("Segundo $segundo", CookingTimerStatus.RUNNING, timer.status)
            decision
        }

        assertTrue("Todos los ticks intermedios piden confirmacion", decisiones.all { it })
    }

    @Test
    fun los_dos_puntos_de_salida_piden_siempre_lo_mismo() {
        val casos = listOf(
            enPasoCon(CookingTimerState.of(10).start()),
            enPasoCon(CookingTimerState.of(10).start().pause()),
            enPasoCon(CookingTimerState.of(10)).toggleCompleted(1),
            enPasoCon(CookingTimerState.of(1).start().tick(60_000L)),
            enPasoCon(CookingTimerState.of(10)),
            enPasoCon(CookingTimerState.of(0)),
            CookingModeState.initial(recetaCon(emptyList())),
        )

        casos.forEach { estado ->
            assertEquals(
                "Mismo veredicto en los dos puntos para $estado",
                CookingExitPolicy.requiresConfirmation(estado, CookingExitPoint.BACK_BUTTON),
                CookingExitPolicy.requiresConfirmation(estado, CookingExitPoint.FINISH),
            )
        }
    }

    @Test
    fun la_decision_es_determinista_y_no_muta_el_estado() {
        val estado = enPasoCon(CookingTimerState.of(10)).toggleCompleted(2)

        val primera = CookingExitPolicy.requiresConfirmation(estado, CookingExitPoint.BACK_BUTTON)
        val segunda = CookingExitPolicy.requiresConfirmation(estado, CookingExitPoint.BACK_BUTTON)

        assertEquals(primera, segunda)
        assertEquals(estado, enPasoCon(CookingTimerState.of(10)).toggleCompleted(2))
    }

    /** El microcopy vive en una constante pura del mismo paquete (sin recursos ni `strings.xml`). */
    @Test
    fun el_microcopy_del_dialogo_es_no_vacio_y_estable() {
        assertEquals("¿Salir del modo cocina?", CookingExitCopy.TITLE)
        assertEquals("Si sales ahora perderás el progreso de tu receta.", CookingExitCopy.MESSAGE)
        assertEquals("Salir", CookingExitCopy.CONFIRM)
        assertEquals("Seguir cocinando", CookingExitCopy.DISMISS)
    }

    /**
     * Aserción explicita de la matriz **por los dos puntos de salida**: los casos negativos
     * (`esperado = false`) fallarian si la politica aprobara sin mirar el estado.
     */
    private fun assertAmbosPuntos(state: CookingModeState, esperado: Boolean) {
        assertEquals(
            "BACK_BUTTON con $state",
            esperado,
            CookingExitPolicy.requiresConfirmation(state, CookingExitPoint.BACK_BUTTON),
        )
        assertEquals(
            "FINISH con $state",
            esperado,
            CookingExitPolicy.requiresConfirmation(state, CookingExitPoint.FINISH),
        )
    }

    private fun ambosPuntos(state: CookingModeState): Boolean =
        CookingExitPolicy.requiresConfirmation(state, CookingExitPoint.BACK_BUTTON) &&
            CookingExitPolicy.requiresConfirmation(state, CookingExitPoint.FINISH)

    /** Estado del modo en el primer paso de una receta con duracion y el [timer] inyectado. */
    private fun enPasoCon(timer: CookingTimerState): CookingModeState =
        CookingModeState.initial(recetaCon(stepsDePrueba)).withTimer(timer)

    private val stepsDePrueba = listOf(
        RecipeStep(order = 1, title = "Primero", description = "", minutes = 10),
        RecipeStep(order = 2, title = "Segundo", description = "", minutes = 8),
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
