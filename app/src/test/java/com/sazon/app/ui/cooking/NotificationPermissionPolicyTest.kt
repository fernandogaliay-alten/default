package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politica **pura** de la peticion del permiso de notificaciones
 * (`docs/specs/cooking-timer-alarm-permission-context.md`, escenarios 1-8): la peticion se dispara
 * **solo** en la arista de "arrancar el temporizador" (`!previous.isRunning && next.isRunning`), sin
 * permiso concedido y sin intento previo en la sesion.
 *
 * Logica pura sin Compose ni Android: el permiso y el instante entran **inyectados**, nunca se lee el
 * reloj. Mismo patron que `CookingAlarmPolicyTest`.
 */
class NotificationPermissionPolicyTest {

    private val sinPedir = NotificationPermissionState(granted = false)
    private val concedido = NotificationPermissionState(granted = true)
    private val yaPedido = NotificationPermissionState(granted = false, askedAtMillis = 1_000L)

    @Test
    fun al_arrancar_el_temporizador_sin_permiso_ni_intento_se_pide_con_rationale() {
        val inactivo = CookingTimerState.of(10)

        val decision = NotificationPermissionPolicy.onTimerChange(
            previous = inactivo,
            next = inactivo.start(),
            state = sinPedir,
        )

        assertEquals(NotificationPermissionCommand.AskWithRationale, decision)
    }

    @Test
    fun la_primera_composicion_de_la_pantalla_no_pide_nada() {
        // No es una transicion real: `previous == next == IDLE` (sin "Iniciar" pulsado).
        val inactivo = CookingTimerState.of(10)

        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(inactivo, inactivo, sinPedir),
        )
    }

    @Test
    fun abrir_el_modo_cocina_o_un_paso_sin_duracion_no_pide_nada() {
        val sinDuracion = CookingTimerState.of(0)

        assertTrue("Sin `Iniciar` alcanzable, un paso sin duracion no lleva a RUNNING", !sinDuracion.hasDuration)
        assertEquals(CookingTimerStatus.IDLE, sinDuracion.status)
        assertEquals(
            "El bloque del temporizador no se pinta, asi que no hay arista de arranque que atender",
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(sinDuracion, sinDuracion, sinPedir),
        )
    }

    @Test
    fun con_el_permiso_concedido_no_se_pide_ni_se_muestra_el_rationale() {
        val inactivo = CookingTimerState.of(10)

        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(inactivo, inactivo.start(), concedido),
        )
    }

    @Test
    fun reanudar_tras_un_intento_previo_no_vuelve_a_pedir_en_la_sesion() {
        val enMarcha = CookingTimerState.of(10).start().tick(60_000L)
        val pausado = enMarcha.pause()

        assertEquals(
            NotificationPermissionCommand.AskWithRationale,
            NotificationPermissionPolicy.onTimerChange(inactivo(), enMarcha, sinPedir),
        )
        assertEquals(
            "Pausar -> Reanudar despues de haberse pedido no vuelve a molestar",
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(pausado, pausado.resume(), yaPedido),
        )
    }

    @Test
    fun un_segundo_arranque_en_la_misma_sesion_ya_no_pide() {
        val inactivo = CookingTimerState.of(10)

        // Tras el primer "Iniciar" la capa Compose registra el intento (`markAsked`): el segundo
        // arranque del mismo temporizador (o de otro paso) no pide nada.
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(inactivo, inactivo.start(), yaPedido),
        )
    }

    @Test
    fun pausar_reiniciar_cambiar_de_paso_y_llegar_a_cero_no_piden_nada() {
        val enMarcha = CookingTimerState.of(10).start().tick(120_000L)
        val pausado = enMarcha.pause()
        val terminado = CookingTimerState.of(1).start().tick(60_000L)

        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(enMarcha, pausado, sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(enMarcha, enMarcha.reset(), sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(pausado, pausado.reset(), sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(enMarcha, terminado, sinPedir),
        )
    }

    @Test
    fun cambiar_de_paso_no_pide_y_el_arranque_del_paso_nuevo_tampoco_tras_el_intento() {
        val steps = listOf(
            RecipeStep(order = 1, title = "Primero", description = "", minutes = 10),
            RecipeStep(order = 2, title = "Segundo", description = "", minutes = 8),
        )
        val enPasoUno = CookingModeState.initial(recetaCon(steps))
            .withTimer(CookingTimerState.of(10).start().tick(120_000L))

        // "Siguiente" reinicia el temporizador al del paso nuevo (Inactivo): RUNNING -> IDLE.
        val enPasoDos = enPasoUno.next(steps)
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(enPasoUno.timer, enPasoDos.timer, sinPedir),
        )
        // Y el arranque del paso nuevo, en la misma sesion, ya no pide (hubo intento antes).
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(
                enPasoDos.timer,
                enPasoDos.timer.start(),
                yaPedido,
            ),
        )
    }

    @Test
    fun los_ticks_y_los_no_ops_del_temporizador_no_piden_nada() {
        val inactivo = CookingTimerState.of(10)
        val pausado = inactivo.start().pause()
        val terminado = CookingTimerState.of(1).start().tick(60_000L)

        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(inactivo, inactivo.pause(), sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(inactivo, inactivo.resume(), sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(pausado, pausado.pause(), sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(pausado, pausado.start(), sinPedir),
        )
        // El doble "Iniciar" con el temporizador ya En marcha es no-op: no hay arista real.
        val enMarcha = inactivo.start()
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(enMarcha, enMarcha.start(), sinPedir),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(terminado, terminado.tick(1_000L), sinPedir),
        )
    }

    @Test
    fun los_ticks_intermedios_no_piden_permiso() {
        var timer = CookingTimerState.of(10).start()
        val decisiones = (1..599).map { _ ->
            val siguiente = timer.tick(1_000L)
            val decision = NotificationPermissionPolicy.onTimerChange(timer, siguiente, sinPedir)
            timer = siguiente
            decision
        }

        assertEquals(CookingTimerStatus.RUNNING, timer.status)
        assertTrue(
            "Ningun tick intermedio pide el permiso",
            decisiones.all { it == NotificationPermissionCommand.None },
        )
    }

    @Test
    fun la_decision_es_determinista_para_el_mismo_estado_inyectado() {
        val inactivo = CookingTimerState.of(10)
        val enMarcha = inactivo.start()

        val primera = NotificationPermissionPolicy.onTimerChange(inactivo, enMarcha, sinPedir)
        val segunda = NotificationPermissionPolicy.onTimerChange(inactivo, enMarcha, sinPedir)

        assertEquals(primera, segunda)
        assertEquals(NotificationPermissionCommand.AskWithRationale, primera)
    }

    @Test
    fun el_instante_del_intento_entra_por_parametro_y_nunca_se_lee_del_reloj() {
        // Dos instantes inyectados distintos dan estados distintos, sin que la politica lea el reloj:
        // el resultado (misma arista, mismo permiso sin conceder) es identico, pero el estado resultante
        // conserva el instante que le entrego quien la llama.
        val primero = sinPedir.asked(1L)
        val segundo = sinPedir.asked(9L)

        assertNotEquals(primero, segundo)
        assertEquals(1L, primero.askedAtMillis)
        assertEquals(9L, segundo.askedAtMillis)
        assertEquals(
            "El instante no cambia la decision: ambos ya tienen intento",
            NotificationPermissionPolicy.onTimerChange(
                CookingTimerState.of(10),
                CookingTimerState.of(10).start(),
                primero,
            ),
            NotificationPermissionPolicy.onTimerChange(
                CookingTimerState.of(10),
                CookingTimerState.of(10).start(),
                segundo,
            ),
        )
        assertEquals(
            NotificationPermissionCommand.None,
            NotificationPermissionPolicy.onTimerChange(
                CookingTimerState.of(10),
                CookingTimerState.of(10).start(),
                primero,
            ),
        )
    }

    @Test
    fun el_estado_asked_devuelve_una_copia_y_no_muta_el_receptor() {
        val original = NotificationPermissionState(granted = false)

        val conIntento = original.asked(4_200L)

        assertEquals("asked() no muta el receptor", null, original.askedAtMillis)
        assertEquals(4_200L, conIntento.askedAtMillis)
        assertEquals(false, conIntento.granted)
    }

    @Test
    fun la_costura_por_defecto_no_hace_nada_y_da_el_permiso_por_concedido() {
        val estado = NoOpNotificationPermissionPrompt.state()

        assertTrue("El default no-op declara el permiso concedido", estado.granted)
        assertEquals(null, estado.askedAtMillis)
        NoOpNotificationPermissionPrompt.request()
        NoOpNotificationPermissionPrompt.markAsked(1L)
        assertEquals("markAsked del no-op no cambia nada", estado, NoOpNotificationPermissionPrompt.state())
    }

    private fun inactivo(): CookingTimerState = CookingTimerState.of(10)

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
