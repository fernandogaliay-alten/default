package com.sazon.app.data.image

import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Escenarios 1-6 y 8 de `docs/specs/dish-image-settings-toggle.md`: interruptor (apagado no pide
 * ninguna imagen), tope por sesion impuesto por el decorador, estado observable y aciertos de cache
 * sin consumo. Todo en JVM: sin red, sin clave, sin cuota y sin emulador; los generadores son
 * **falsos** que cuentan invocaciones y la cache de disco es real sobre un directorio temporal, de
 * modo que las aserciones no pasan en vacio.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DishImageSettingsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    // --- Escenario 1: interruptor apagado -> 0 peticiones y sin consumo ---

    @Test
    fun interruptor_apagado_no_pide_ninguna_imagen_ni_consume_tope() = runTest {
        val settings = DishImageSettings(enabled = false)
        var producerCalls = 0

        val result = settings.generatedIfEnabled {
            producerCalls += 1
            "imagen"
        }

        assertNull("Apagado devuelve null sin producir", result)
        assertEquals("El productor no se invoca ni una vez", 0, producerCalls)
        assertFalse(settings.state.value.enabled)
        assertEquals("Apagado no reserva tope", 0, settings.state.value.used)
    }

    @Test
    fun interruptor_encendido_permite_producir_una_vez() = runTest {
        val settings = DishImageSettings(enabled = true)
        var producerCalls = 0

        val result = settings.generatedIfEnabled {
            producerCalls += 1
            "imagen"
        }

        assertEquals("Encendido delega en el productor", "imagen", result)
        assertEquals(1, producerCalls)
    }

    @Test
    fun apagar_el_interruptor_tras_producir_corta_la_siguiente_peticion() = runTest {
        val settings = DishImageSettings()
        var producerCalls = 0
        settings.generatedIfEnabled { producerCalls += 1; "a" }

        settings.setEnabled(false)
        val afterOff = settings.generatedIfEnabled { producerCalls += 1; "b" }

        assertNull(afterOff)
        assertEquals("Solo se produjo antes de apagar", 1, producerCalls)
    }

    // --- Escenario 4: apagado no consume tope; apagar/encender no es un reset ---

    @Test
    fun el_interruptor_apagado_no_consume_tope_ni_reserva() {
        val settings = DishImageSettings(
            enabled = false,
            budget = DishImageGenerationBudget(maxGenerations = 2),
        )

        assertFalse(settings.tryAcquireGeneration())
        assertFalse(settings.tryAcquireGeneration())

        assertEquals(0, settings.state.value.used)
        assertEquals(2, settings.state.value.remaining)
        assertFalse(settings.state.value.exhausted)
    }

    @Test
    fun apagar_y_encender_no_reinicia_el_tope_agotado() {
        val settings = DishImageSettings(
            budget = DishImageGenerationBudget(maxGenerations = 1),
        )
        assertTrue(settings.tryAcquireGeneration())
        assertTrue(settings.state.value.exhausted)

        settings.setEnabled(false)
        settings.setEnabled(true)

        assertTrue("Apagar/encender no es un reset", settings.state.value.exhausted)
        assertEquals(1, settings.state.value.used)
        assertFalse("El tope agotado sigue cortando", settings.tryAcquireGeneration())
    }

    // --- Escenario 5: estado observable (determinista) ---

    @Test
    fun el_estado_inicial_es_activado_sin_consumo_y_con_el_tope_de_la_politica() {
        val settings = DishImageSettings()

        assertEquals(DishImageSettingsState(enabled = true, used = 0, max = 12), settings.state.value)
        assertEquals(12, settings.state.value.remaining)
        assertFalse(settings.state.value.exhausted)
    }

    @Test
    fun una_reserva_re_emite_el_estado_con_el_consumo_actualizado() {
        val settings = DishImageSettings()

        assertTrue(settings.tryAcquireGeneration())

        assertEquals(DishImageSettingsState(enabled = true, used = 1, max = 12), settings.state.value)
        assertEquals(11, settings.state.value.remaining)
        assertFalse(settings.state.value.exhausted)
    }

    @Test
    fun el_estado_es_un_state_flow_observable() = runTest {
        val settings = DishImageSettings()
        val seen = mutableListOf<DishImageSettingsState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            settings.state.collect { seen += it }
        }

        settings.tryAcquireGeneration()
        settings.setEnabled(false)
        job.cancel()

        assertEquals(DishImageSettingsState(enabled = true, used = 0, max = 12), seen.first())
        assertTrue("Emite el consumo", seen.any { it.used == 1 && it.enabled })
        assertTrue("Emite el interruptor apagado", seen.any { !it.enabled })
    }

    // --- Escenario 3: el decorador impone el tope sin invocar al delegado agotado ---

    @Test
    fun el_decorador_agotado_no_invoca_al_delegado() = runTest {
        val settings = DishImageSettings(
            budget = DishImageGenerationBudget(maxGenerations = 2),
        )
        val delegate = CountingDishImageGenerator()
        val gated = GatedDishImageGenerator(delegate, settings)

        assertNotNull(gated.generate(request("a")))
        assertNotNull(gated.generate(request("b")))
        val third = gated.generate(request("c"))

        assertNull("Agotado devuelve null sin invocar", third)
        assertEquals("Una invocacion por reserva", 2, delegate.calls)
        assertEquals(2, settings.state.value.used)
        assertEquals(0, settings.state.value.remaining)
        assertTrue(settings.state.value.exhausted)
    }

    @Test
    fun el_decorador_apagado_no_invoca_al_delegado() = runTest {
        val settings = DishImageSettings(enabled = false)
        val delegate = CountingDishImageGenerator()
        val gated = GatedDishImageGenerator(delegate, settings)

        assertNull(gated.generate(request("a")))
        assertEquals(0, delegate.calls)
        assertEquals(0, settings.state.value.used)
    }

    @Test
    fun la_constante_del_tope_vive_en_un_unico_sitio() {
        assertEquals(12, DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION)
        assertTrue(DishImageSettingsPolicy.DEFAULT_ENABLED)
    }

    // --- Escenario 6: un acierto de cache no consume tope ---

    @Test
    fun un_acierto_de_cache_no_consume_tope() = runTest {
        val settings = DishImageSettings(
            budget = DishImageGenerationBudget(maxGenerations = 5),
        )
        val delegate = CountingDishImageGenerator()
        val resolver = resolver(settings, delegate)

        resolver.imageFor(request(KEY))
        resolver.imageFor(request(KEY))

        assertEquals("La segunda resolucion es un acierto de memoria", 1, delegate.calls)
        assertEquals(1, settings.state.value.used)

        resolver.imageFor(request(OTHER_KEY))

        assertEquals("Una clave nueva si paga una generacion", 2, delegate.calls)
        assertEquals(2, settings.state.value.used)
    }

    @Test
    fun un_acierto_de_disco_tampoco_consume_tope() = runTest {
        val root = File(temporaryFolder.root, "dish-images")
        val settings = DishImageSettings(
            budget = DishImageGenerationBudget(maxGenerations = 5),
        )
        resolver(settings, CountingDishImageGenerator(), root = root).imageFor(request(KEY))

        val coldDelegate = CountingDishImageGenerator { webpBytes(99) }
        val fromDisk = resolver(settings, coldDelegate, root = root).imageFor(request(KEY))

        assertNotNull(fromDisk)
        assertEquals("La memoria fria se sirve del disco", 0, coldDelegate.calls)
        assertEquals(1, settings.state.value.used)
    }

    // --- Escenario 8: fallo y cancelacion (degradacion silenciosa, reserva consumida) ---

    @Test
    fun un_fallo_del_proveedor_degrada_a_null_y_consume_la_unidad() = runTest {
        val settings = DishImageSettings(
            budget = DishImageGenerationBudget(maxGenerations = 5),
        )
        val delegate = CountingDishImageGenerator {
            throw IllegalStateException("sin red")
        }

        val result = resolver(settings, delegate).imageFor(request(KEY))

        assertNull("El fallo degrada a null sin romper la UI", result)
        assertEquals("El intento se paga (es donde se gasta)", 1, settings.state.value.used)
    }

    @Test
    fun la_cancelacion_del_generador_se_propaga_y_consume_la_unidad() = runTest {
        val settings = DishImageSettings(
            budget = DishImageGenerationBudget(maxGenerations = 5),
        )
        val delegate = CountingDishImageGenerator {
            throw CancellationException("cancelado")
        }
        var propagated: CancellationException? = null

        try {
            resolver(settings, delegate).imageFor(request(KEY))
        } catch (cancellation: CancellationException) {
            propagated = cancellation
        }

        assertNotNull("La cancelacion se propaga por el decorador y el resolutor", propagated)
        assertEquals("La reserva previa a la cancelacion queda consumida", 1, settings.state.value.used)
    }

    @Test
    fun el_decorador_no_convierte_la_cancelacion_en_null() = runTest {
        val settings = DishImageSettings()
        val gated = GatedDishImageGenerator(
            CountingDishImageGenerator { throw CancellationException("cancelado") },
            settings,
        )
        var propagated = false

        try {
            gated.generate(request(KEY))
        } catch (cancellation: CancellationException) {
            propagated = true
        }

        assertTrue("El decorador no captura excepciones", propagated)
        assertEquals(1, settings.state.value.used)
    }

    // --- Helpers ---

    private fun resolver(
        settings: DishImageSettings,
        delegate: DishImageGenerator,
        root: File = File(temporaryFolder.root, "dish-images"),
    ) = DishImageResolver(
        disk = FileDishImageCache(root),
        memory = MemoryDishImageCache(),
        generator = GatedDishImageGenerator(delegate, settings),
        ioDispatcher = Dispatchers.Unconfined,
    )
}

private const val KEY = "dish-image-v1-arroz-con-pollo-criollo"
private const val OTHER_KEY = "dish-image-v1-tortilla-de-patatas"

private fun request(cacheKey: String) = DishImageRequest(
    cacheKey = cacheKey,
    prompt = "Fotografía gastronómica de $cacheKey",
)

/** Generador falso: cuenta invocaciones, no hace red y delega en [block] (que puede suspender). */
private class CountingDishImageGenerator(
    private val block: suspend (DishImageRequest) -> ByteArray? = { webpBytes() },
) : DishImageGenerator {

    var calls = 0
        private set

    override suspend fun generate(request: DishImageRequest): ByteArray? {
        calls += 1
        return block(request)
    }
}

/** Bytes con cabecera WebP (`RIFF`...`WEBP`), el formato que fija la fase 2. */
private fun webpBytes(size: Int = 12): ByteArray {
    val bytes = ByteArray(size.coerceAtLeast(12))
    "RIFF".toByteArray().copyInto(bytes, 0)
    "WEBP".toByteArray().copyInto(bytes, 8)
    return bytes
}
