package com.sazon.app.data.image

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-7 de `docs/specs/dish-image-settings-persistence.md`: el **interruptor** de imagenes
 * del plato se guarda y se restaura al arrancar (encendido/apagado), sin estado guardado manda el
 * default de la politica (**apagado**), el **tope por sesion no se persiste** (arranca en 12), solo
 * se escribe al mover el interruptor y el almacenamiento degrada en silencio.
 *
 * Todo en JVM con una costura **falsa** en memoria que cuenta escrituras: sin red, sin clave, sin
 * cuota, sin Android y sin emulador. Las construcciones existentes del ajuste
 * (`DishImageSettings()` / `DishImageSettings(enabled = …, budget = …)`) no cambian de sentido: el
 * parametro `store` tiene default no-op.
 */
class DishImageSettingsPersistenceTest {

    // --- Escenario 4: guardar y releer el estado (verificacion exigida 1) ---

    @Test
    fun guardar_encendido_y_releerlo_devuelve_encendido() {
        val store = RecordingDishImageSettingsStore()
        val first = DishImageSettings.restored(store)

        first.setEnabled(true)

        val reopened = DishImageSettings.restored(store)
        assertTrue("El nuevo arranque lee el ultimo estado movido", reopened.state.value.enabled)
        assertEquals(1, store.writes)
        assertEquals(listOf(true), store.written)
    }

    @Test
    fun guardar_apagado_y_releerlo_devuelve_apagado() {
        val store = RecordingDishImageSettingsStore(stored = true)
        val first = DishImageSettings.restored(store)

        first.setEnabled(false)

        val reopened = DishImageSettings.restored(store)
        assertFalse("Apagar sobrevive al reinicio", reopened.state.value.enabled)
        assertEquals(1, store.writes)
        assertEquals(listOf(false), store.written)
    }

    @Test
    fun el_valor_releido_es_siempre_el_ultimo_movido_por_el_usuario() {
        val store = RecordingDishImageSettingsStore()
        val settings = DishImageSettings.restored(store)

        settings.setEnabled(true)
        assertTrue(DishImageSettings.restored(store).state.value.enabled)

        settings.setEnabled(false)
        assertFalse(DishImageSettings.restored(store).state.value.enabled)
        assertEquals("Una escritura por movimiento, en orden", listOf(true, false), store.written)
    }

    // --- Escenario 3: sin estado guardado manda el default de la politica ---

    @Test
    fun sin_estado_guardado_se_usa_el_default_de_la_politica_y_no_se_escribe() {
        val store = RecordingDishImageSettingsStore()

        val settings = DishImageSettings.restored(store)

        assertEquals(
            DishImageSettingsState(enabled = false, used = 0, max = 12),
            settings.state.value,
        )
        assertFalse(DishImageSettingsPolicy.DEFAULT_ENABLED)
        assertEquals("Arrancar no guarda nada", 0, store.writes)
    }

    // --- Escenarios 1 y 2: arranque restaurado y gate del interruptor (verificacion exigida 2) ---

    @Test
    fun el_arranque_con_estado_persistido_encendido_pide_imagenes() = runTest {
        val store = RecordingDishImageSettingsStore(stored = true)

        val settings = DishImageSettings.restored(store)
        var producerCalls = 0
        val result = settings.generatedIfEnabled {
            producerCalls += 1
            "imagen"
        }

        assertEquals(
            DishImageSettingsState(enabled = true, used = 0, max = 12),
            settings.state.value,
        )
        assertEquals("Encendido delega en la cadena normal", "imagen", result)
        assertEquals("El productor se invoca una vez", 1, producerCalls)
    }

    @Test
    fun el_arranque_con_estado_persistido_apagado_no_pide_imagenes() = runTest {
        val store = RecordingDishImageSettingsStore(stored = false)

        val settings = DishImageSettings.restored(store)
        var producerCalls = 0
        val result = settings.generatedIfEnabled {
            producerCalls += 1
            "imagen"
        }

        assertNull("Apagado corta sin producir", result)
        assertEquals("No se lee cache, ni semilla, ni API: 0 invocaciones", 0, producerCalls)
        assertEquals(
            DishImageSettingsState(enabled = false, used = 0, max = 12),
            settings.state.value,
        )
        assertFalse("Apagado tampoco reserva tope", settings.tryAcquireGeneration())
        assertEquals(0, settings.state.value.used)
    }

    @Test
    fun el_arranque_sin_estado_guardado_no_pide_imagenes() = runTest {
        val settings = DishImageSettings.restored(RecordingDishImageSettingsStore())
        var producerCalls = 0

        val result = settings.generatedIfEnabled {
            producerCalls += 1
            "imagen"
        }

        assertNull(result)
        assertEquals(0, producerCalls)
    }

    // --- Escenarios 5 y 6: el tope por sesion no se persiste (verificacion exigida 3) ---

    @Test
    fun el_tope_no_se_persiste_y_arranca_en_12() {
        val store = RecordingDishImageSettingsStore(stored = true)

        // Sesion anterior (proceso muerto): el usuario encendio el interruptor y gasto 2 unidades.
        val previousSession = DishImageSettings.restored(store)
        assertTrue(previousSession.state.value.enabled)
        assertTrue(previousSession.tryAcquireGeneration())
        assertTrue(previousSession.tryAcquireGeneration())
        assertEquals(2, previousSession.state.value.used)

        // Sesion nueva: el interruptor sigue encendido, el tope vuelve a 12.
        val session = DishImageSettings.restored(store)

        assertTrue("La eleccion sobrevive", session.state.value.enabled)
        assertEquals(0, session.state.value.used)
        assertEquals(12, session.state.value.max)
        assertEquals(12, session.state.value.remaining)
        assertFalse(session.state.value.exhausted)
    }

    @Test
    fun solo_el_interruptor_se_escribe_y_solo_al_moverlo() {
        val store = RecordingDishImageSettingsStore()
        val settings = DishImageSettings.restored(store)

        settings.setEnabled(true)
        assertTrue(settings.tryAcquireGeneration())
        assertTrue(settings.tryAcquireGeneration())

        assertEquals("La reserva de tope no escribe", 1, store.writes)
        assertEquals(listOf(true), store.written)

        settings.setEnabled(false)

        assertEquals("Solo el interruptor escribe", 2, store.writes)
        assertEquals(listOf(true, false), store.written)
    }

    @Test
    fun la_sesion_nueva_no_hereda_el_consumo_ni_siquiera_con_el_estado_encendido() {
        val store = RecordingDishImageSettingsStore(stored = true)
        val settings = DishImageSettings.restored(store)

        // Se agota el tope de la sesion; el estado guardado no cambia por ello.
        repeat(DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION) {
            assertTrue(settings.tryAcquireGeneration())
        }
        assertTrue(settings.state.value.exhausted)
        assertEquals("El tope agotado no escribe nada", 0, store.writes)

        val next = DishImageSettings.restored(store)

        assertTrue(next.state.value.enabled)
        assertEquals(DishImageSettingsState(enabled = true, used = 0, max = 12), next.state.value)
    }

    // --- Escenario 7: degradacion silenciosa del almacenamiento ---

    @Test
    fun un_store_que_lanza_al_leer_degrada_al_default_apagado() = runTest {
        val settings = DishImageSettings.restored(FailingDishImageSettingsStore(failOnLoad = true))
        var producerCalls = 0

        val result = settings.generatedIfEnabled { producerCalls += 1; "imagen" }

        assertFalse("La lectura rota cae al default de la politica", settings.state.value.enabled)
        assertNull(result)
        assertEquals(0, producerCalls)
        assertEquals(DishImageSettingsState(enabled = false, used = 0, max = 12), settings.state.value)
    }

    @Test
    fun un_store_que_lanza_al_guardar_deja_la_sesion_funcionando() = runTest {
        val settings = DishImageSettings.restored(FailingDishImageSettingsStore(failOnSave = true))

        settings.setEnabled(true)

        assertTrue("El estado en memoria cambia igual", settings.state.value.enabled)
        assertEquals("Encendido sigue pidiendo imagenes", "imagen", settings.generatedIfEnabled { "imagen" })

        settings.setEnabled(false)

        assertFalse("Apagar tambien funciona sin almacenamiento", settings.state.value.enabled)
    }

    @Test
    fun el_default_del_parametro_store_es_no_op() {
        val settings = DishImageSettings()

        settings.setEnabled(true)

        assertTrue(settings.state.value.enabled)
        assertEquals(12, settings.state.value.max)
        assertNull("La costura por defecto no guarda nada", NoOpDishImageSettingsStore.loadEnabled())
        NoOpDishImageSettingsStore.saveEnabled(true)
    }
}

/** Costura falsa en memoria: guarda el ultimo valor y **cuenta** cada escritura. */
private class RecordingDishImageSettingsStore(
    private var stored: Boolean? = null,
) : DishImageSettingsStore {

    var writes = 0
        private set

    val written = mutableListOf<Boolean>()

    override fun loadEnabled(): Boolean? = stored

    override fun saveEnabled(enabled: Boolean) {
        writes += 1
        written += enabled
        stored = enabled
    }
}

/** Costura que revienta en la operacion indicada: comprueba la degradacion silenciosa. */
private class FailingDishImageSettingsStore(
    private val failOnLoad: Boolean = false,
    private val failOnSave: Boolean = false,
) : DishImageSettingsStore {

    override fun loadEnabled(): Boolean? {
        if (failOnLoad) throw IllegalStateException("preferencias ilegibles")
        return null
    }

    override fun saveEnabled(enabled: Boolean) {
        if (failOnSave) throw IllegalStateException("no se puede escribir")
    }
}
