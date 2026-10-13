package com.sazon.app.data.image

import com.sazon.app.data.DishImageSeedSource
import com.sazon.app.domain.image.DishImageCacheKey
import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Escenarios 1-5, 8-11 de `docs/specs/dish-image-cache.md` (memoria -> disco -> generador ->
 * persistir) y escenarios 2-7 de `docs/specs/dish-image-seed.md` (el nivel semilla entre el disco y el
 * generador: **memoria -> disco -> semilla -> generador**). El generador y la fuente semilla son
 * **falsos** (cuentan llamadas y lecturas: sin red, sin clave y sin cuota) y el almacen es un
 * directorio temporal real, de modo que las aserciones de persistencia no pasan en vacio. Todo en JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DishImageResolverTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    // Escenario 1: la segunda peticion con la misma clave sale de memoria y no llama al generador.
    @Test
    fun la_segunda_peticion_con_la_misma_clave_no_llama_al_generador() = runTest {
        val root = cacheRoot()
        val generator = FakeDishImageGenerator { webpBytes(32) }
        val resolver = resolver(root, generator)

        val first = resolver.imageFor(request(KEY))
        val second = resolver.imageFor(request(KEY))

        assertNotNull(first)
        assertNotNull(second)
        assertArrayEquals("Las dos respuestas son iguales", first, second)
        assertEquals("El generador se invoca una sola vez", 1, generator.calls)
        assertEquals(
            "En disco queda un unico fichero con esa clave",
            listOf("$KEY.webp"),
            root.listFiles()!!.map { it.name },
        )
    }

    // Escenario 2: con la memoria fria (resolutor nuevo) se sirve del disco sin llamar al generador.
    @Test
    fun con_la_memoria_fria_se_sirve_del_disco_sin_llamar_al_generador() = runTest {
        val root = cacheRoot()
        val onDisk = resolver(root, FakeDishImageGenerator { webpBytes(32) }).imageFor(request(KEY))!!
        val coldGenerator = FakeDishImageGenerator { webpBytes(999) }

        val fromDisk = resolver(root, coldGenerator).imageFor(request(KEY))

        assertArrayEquals("Devuelve exactamente los bytes del fichero", onDisk, fromDisk)
        assertEquals("El generador no se invoca", 0, coldGenerator.calls)
    }

    // Escenario 3: sin red (generador null) se sirve del disco; sin copia, null sin crear fichero.
    @Test
    fun sin_red_se_sirve_del_disco_y_sin_copia_devuelve_null_sin_escribir() = runTest {
        val root = cacheRoot()
        val cached = resolver(root, FakeDishImageGenerator { webpBytes(32) }).imageFor(request(KEY))!!

        val offlineGenerator = FakeDishImageGenerator { null }
        val offline = resolver(root, offlineGenerator)

        val fromDisk = offline.imageFor(request(KEY))
        assertArrayEquals(cached, fromDisk)
        assertEquals("No invoca al generador si hay copia", 0, offlineGenerator.calls)

        val missing = offline.imageFor(request(OTHER_KEY))
        assertNull("Sin copia y sin generador, null", missing)
        assertEquals("Si no hay copia, si consulta al generador", 1, offlineGenerator.calls)
        assertFalse(File(root, "$OTHER_KEY.webp").exists())
        assertEquals("No se crea ningun fichero nuevo", 1, root.listFiles()!!.size)
    }

    // Escenario 4: dos titulos equivalentes comparten fichero (clave canonica por construccion).
    @Test
    fun dos_titulos_equivalentes_comparten_un_solo_fichero() = runTest {
        val keyFromPlainTitle = DishImageCacheKey.of("Arroz con Pollo Criollo")
        val keyFromMessyTitle = DishImageCacheKey.of("  arroz  con   pollo criollo ")
        val otherKey = DishImageCacheKey.of("Tortilla de patatas")
        assertEquals("Las dos claves equivalentes coinciden", keyFromPlainTitle, keyFromMessyTitle)

        val root = cacheRoot()
        val generator = FakeDishImageGenerator { webpBytes(32) }
        val resolver = resolver(root, generator)

        resolver.imageFor(request(keyFromPlainTitle))
        resolver.imageFor(request(keyFromMessyTitle))
        resolver.imageFor(request(otherKey))

        assertEquals("Una generacion por clave distinta", 2, generator.calls)
        assertEquals(
            "Un solo fichero para los dos equivalentes y otro para el tercer plato",
            listOf("$keyFromPlainTitle.webp", "$otherKey.webp").sorted(),
            root.listFiles()!!.map { it.name }.sorted(),
        )
    }

    // Escenario 5: bajo la raiz inyectada, nombre = clave + .webp y contenido identico al generador.
    @Test
    fun el_fichero_es_la_clave_mas_webp_con_los_bytes_del_generador() = runTest {
        val root = cacheRoot()
        val bytes = webpBytes(64)
        val generator = FakeDishImageGenerator { bytes }

        val result = resolver(root, generator).imageFor(request(KEY))

        assertNotNull(result)
        assertArrayEquals(bytes, result)
        assertTrue("Los bytes empiezan por RIFF...WEBP", result!!.isWebp())

        val file = File(root, "$KEY.webp")
        assertTrue(file.isFile)
        assertArrayEquals("Identicos a los del generador", bytes, file.readBytes())
    }

    // Escenario 8: una clave descartada de la memoria se sirve del disco sin volver a generar.
    @Test
    fun una_clave_descartada_de_la_memoria_se_sirve_del_disco_sin_generar() = runTest {
        val root = cacheRoot()
        val generator = FakeDishImageGenerator { webpBytes(32) }
        val resolver = resolver(root, generator, memory = MemoryDishImageCache(maxEntries = 2))

        val firstBytes = resolver.imageFor(request(KEY_A))!!
        resolver.imageFor(request(KEY_B))
        resolver.imageFor(request(KEY_C))
        assertEquals(3, generator.calls)

        val again = resolver.imageFor(request(KEY_A))

        assertArrayEquals("Se sirve del disco y coincide", firstBytes, again)
        assertEquals("No se vuelve a pagar la generacion", 3, generator.calls)
    }

    // Escenario 9: null, array vacio o excepcion del generador -> null sin persistir nada.
    @Test
    fun un_generador_que_falla_no_persiste_nada() = runTest {
        val root = cacheRoot()
        val failures = listOf<suspend (DishImageRequest) -> ByteArray?>(
            { null },
            { ByteArray(0) },
            { throw IllegalStateException("fallo del proveedor") },
        )

        failures.forEachIndexed { index, failure ->
            val generator = FakeDishImageGenerator(failure)
            val memory = MemoryDishImageCache()

            val result = resolver(root, generator, memory = memory).imageFor(request(KEY))

            assertNull("El fallo #$index debe degradar a null", result)
            assertEquals("El fallo #$index no deja entrada de memoria", 0, memory.size)
            assertFalse("El fallo #$index no crea fichero", File(root, "$KEY.webp").exists())
            assertEquals("El fallo #$index no deja temporales", 0, root.listFiles()?.size ?: 0)
        }
    }

    // Escenario 9 (cancelacion): CancellationException se relanza, no se convierte en null.
    @Test
    fun la_cancelacion_del_generador_se_propaga() = runTest {
        val generator = FakeDishImageGenerator { throw CancellationException("cancelado") }
        var propagated: CancellationException? = null

        try {
            resolver(cacheRoot(), generator).imageFor(request(KEY))
        } catch (cancellation: CancellationException) {
            propagated = cancellation
        }

        assertNotNull("El resolutor debe relanzar la cancelacion", propagated)
        assertEquals(1, generator.calls)
    }

    // Escenario 10: dos peticiones concurrentes de la misma clave pagan una sola generacion.
    @Test
    fun dos_peticiones_concurrentes_de_la_misma_clave_pagan_una_sola_generacion() = runTest {
        val root = cacheRoot()
        val release = CompletableDeferred<Unit>()
        val generator = FakeDishImageGenerator {
            release.await()
            webpBytes(32)
        }
        val resolver = resolver(root, generator)

        val first = async { resolver.imageFor(request(KEY)) }
        val second = async { resolver.imageFor(request(KEY)) }
        advanceUntilIdle()

        assertEquals("La segunda peticion espera al Mutex, no genera", 1, generator.calls)

        release.complete(Unit)
        advanceUntilIdle()

        val firstBytes = first.await()
        val secondBytes = second.await()
        assertNotNull(firstBytes)
        assertNotNull(secondBytes)
        assertArrayEquals(firstBytes, secondBytes)
        assertEquals("Sigue siendo una sola invocacion", 1, generator.calls)
    }

    // Escenario 10 (variante): con claves distintas, una generacion por clave.
    @Test
    fun con_claves_distintas_el_generador_se_invoca_una_vez_por_clave() = runTest {
        val root = cacheRoot()
        val generator = FakeDishImageGenerator { webpBytes(32) }
        val resolver = resolver(root, generator)

        val first = async { resolver.imageFor(request(KEY_A)) }
        val second = async { resolver.imageFor(request(KEY_B)) }
        advanceUntilIdle()

        assertNotNull(first.await())
        assertNotNull(second.await())
        assertEquals(2, generator.calls)
        assertEquals(2, root.listFiles()!!.size)
    }

    // Escenario 11: si no se puede persistir, se devuelven igualmente los bytes generados.
    @Test
    fun si_no_se_puede_persistir_devuelve_igualmente_los_bytes() = runTest {
        val unusableRoot = File(temporaryFolder.root, "raiz-ocupada")
        unusableRoot.writeText("un fichero regular bloquea la raiz")
        assertTrue(unusableRoot.isFile)
        val bytes = webpBytes(16)

        val result = resolver(unusableRoot, FakeDishImageGenerator { bytes }).imageFor(request(KEY))

        assertArrayEquals("La persistencia es best-effort", bytes, result)
        assertTrue("La raiz sigue siendo un fichero intacto", unusableRoot.isFile)
    }

    // La segunda peticion tambien se sirve sin generador si la primera se cancelo y no persistio.
    @Test
    fun tras_una_cancelacion_no_queda_nada_persistido_ni_en_memoria() = runTest {
        val root = cacheRoot()
        val memory = MemoryDishImageCache()
        val generator = FakeDishImageGenerator { throw CancellationException("cancelado") }

        launch {
            runCatching { resolver(root, generator, memory = memory).imageFor(request(KEY)) }
        }
        advanceUntilIdle()

        assertEquals(0, memory.size)
        assertEquals(0, root.listFiles()?.size ?: 0)
    }

    // --- Fase 7 (`dish-image-seed`): el nivel semilla entre el disco y el generador ------------------

    // Escenario 2: la semilla gana al generador: 0 llamadas, 0 ficheros en la cache, 0 red.
    @Test
    fun la_semilla_gana_al_generador_sin_generar_y_sin_escribir_en_disco() = runTest {
        val root = cacheRoot()
        val seedBytes = webpBytes(48)
        val seed = FakeDishImageSeedSource { if (it == SEED_KEY) seedBytes else null }
        val generator = FakeDishImageGenerator { throw IllegalStateException("el generador no debe invocarse") }

        val result = resolver(root, generator, seed = seed).imageFor(request(SEED_KEY))

        assertArrayEquals("Sirve los bytes del asset semilla", seedBytes, result)
        assertEquals("El generador se invoca 0 veces", 0, generator.calls)
        assertEquals("El asset no se duplica en la cache", 0, root.listFiles()?.size ?: 0)
    }

    // Escenario 3: la segunda resolucion sale de la memoria del resolutor y no relee el asset.
    @Test
    fun la_segunda_resolucion_no_vuelve_a_leer_el_asset() = runTest {
        val seed = FakeDishImageSeedSource { webpBytes(32) }
        val generator = FakeDishImageGenerator { webpBytes(999) }
        val resolver = resolver(cacheRoot(), generator, seed = seed)

        val first = resolver.imageFor(request(SEED_KEY))
        val second = resolver.imageFor(request(SEED_KEY))

        assertNotNull(first)
        assertArrayEquals("Los dos aciertos son identicos", first, second)
        assertEquals("El asset se lee una sola vez", 1, seed.reads)
        assertEquals("El generador sigue en 0 llamadas", 0, generator.calls)
    }

    // Escenario 4: precedencia memoria -> disco -> semilla (el disco gana a la semilla).
    @Test
    fun el_disco_gana_a_la_semilla_y_la_semilla_no_se_escribe() = runTest {
        val root = cacheRoot()
        val onDisk = resolver(root, FakeDishImageGenerator { webpBytes(32) }).imageFor(request(SEED_KEY))!!
        val seedBytes = webpBytes(48)
        val seed = FakeDishImageSeedSource { seedBytes }
        val generator = FakeDishImageGenerator { webpBytes(999) }

        val fromDisk = resolver(root, generator, seed = seed).imageFor(request(SEED_KEY))
        assertArrayEquals("El plato con copia se sirve del disco", onDisk, fromDisk)
        assertEquals("No toca la fuente semilla", 0, seed.reads)
        assertEquals("No genera", 0, generator.calls)

        val fromSeed = resolver(root, generator, seed = seed).imageFor(request(OTHER_KEY))
        assertArrayEquals("El plato sin copia se sirve del asset semilla", seedBytes, fromSeed)
        assertEquals("Consulta la semilla una vez", 1, seed.reads)
        assertEquals("No genera", 0, generator.calls)
        assertEquals(
            "El asset semilla no se escribe en la cache",
            listOf("$SEED_KEY.webp"),
            root.listFiles()!!.map { it.name },
        )
    }

    // Escenario 5: una clave fuera del set no cambia nada (el set semilla no es un comodin).
    @Test
    fun una_clave_fuera_del_set_mantiene_la_cadena_de_la_fase_3() = runTest {
        val root = cacheRoot()
        val seed = FakeDishImageSeedSource { if (it == SEED_KEY) webpBytes(48) else null }
        val generated = webpBytes(24)

        val fromGenerator = resolver(root, FakeDishImageGenerator { generated }, seed = seed)
            .imageFor(request(OTHER_KEY))
        assertArrayEquals("Sin asset se genera como en la fase 3", generated, fromGenerator)
        assertTrue("El generado si se persiste", File(root, "$OTHER_KEY.webp").isFile)

        val offline = FakeDishImageGenerator { null }
        val missing = resolver(root, offline, seed = seed)
            .imageFor(request(DishImageCacheKey.of("Plato que no existe")))

        assertNull("Sin copia, sin asset y sin generador, null", missing)
        assertEquals("El generador se consulta (la semilla no resuelve esa clave)", 1, offline.calls)
    }

    // Escenario 6: null, array vacio o excepcion de la fuente semilla degradan sin romper la cadena.
    @Test
    fun el_fallo_de_la_fuente_semilla_degrada_en_silencio() = runTest {
        val failures = listOf<suspend (String) -> ByteArray?>(
            { null },
            { ByteArray(0) },
            { throw IllegalStateException("fallo del asset") },
        )

        failures.forEachIndexed { index, failure ->
            val root = File(cacheRoot(), "intento-$index")
            val seed = FakeDishImageSeedSource(failure)
            val generator = FakeDishImageGenerator { webpBytes(32) }
            val memory = MemoryDishImageCache()

            val result = resolver(root, generator, memory = memory, seed = seed).imageFor(request(SEED_KEY))

            assertArrayEquals("El fallo #$index no rompe la cadena", webpBytes(32), result)
            assertEquals("El fallo #$index consulta la semilla", 1, seed.reads)
            assertEquals("El fallo #$index cae al generador", 1, generator.calls)
            assertTrue("El fallo #$index genera y persiste", File(root, "$SEED_KEY.webp").isFile)
        }
    }

    // Escenario 6 (cancelacion): CancellationException de la fuente semilla se relanza.
    @Test
    fun la_cancelacion_de_la_fuente_semilla_se_propaga() = runTest {
        val seed = FakeDishImageSeedSource { throw CancellationException("cancelado") }
        val generator = FakeDishImageGenerator { webpBytes(32) }
        var propagated: CancellationException? = null

        try {
            resolver(cacheRoot(), generator, seed = seed).imageFor(request(SEED_KEY))
        } catch (cancellation: CancellationException) {
            propagated = cancellation
        }

        assertNotNull("El resolutor debe relanzar la cancelacion", propagated)
        assertEquals(1, seed.reads)
        assertEquals("La cadena no llega al generador", 0, generator.calls)
    }

    // Escenario 7: sin fuente semilla (parametro por defecto) el resolutor es el de la fase 3.
    @Test
    fun sin_fuente_semilla_el_comportamiento_es_el_de_la_fase_3() = runTest {
        val root = cacheRoot()
        val generator = FakeDishImageGenerator { webpBytes(32) }

        val result = resolver(root, generator).imageFor(request(SEED_KEY))

        assertArrayEquals(webpBytes(32), result)
        assertEquals("Sin semilla se genera (y se paga) como en la fase 3", 1, generator.calls)
        assertEquals(
            "Sin semilla se persiste como en la fase 3",
            listOf("$SEED_KEY.webp"),
            root.listFiles()!!.map { it.name },
        )
    }

    // --- Escenario 10 de `dish-image-prompt-v2`: diagnostico del origen resuelto --------------------

    @Test
    fun el_origen_resuelto_reporta_seed_para_el_set_semilla_y_generado_para_el_generador() = runTest {
        val root = cacheRoot()
        val seed = FakeDishImageSeedSource { if (it == SEED_KEY) webpBytes(48) else null }
        val reportados = mutableListOf<Pair<String, DishImageOrigin>>()
        val resolver = resolver(
            root,
            FakeDishImageGenerator { webpBytes(24) },
            seed = seed,
            onResolved = { request, origin -> reportados += request.cacheKey to origin },
        )

        resolver.imageFor(request(SEED_KEY))
        resolver.imageFor(request(OTHER_KEY))

        assertEquals(
            listOf(SEED_KEY to DishImageOrigin.SEED, OTHER_KEY to DishImageOrigin.GENERATED),
            reportados,
        )
    }

    @Test
    fun un_acierto_de_memoria_sobre_bytes_semilla_sigue_reportando_seed() = runTest {
        val seed = FakeDishImageSeedSource { webpBytes(48) }
        val reportados = mutableListOf<DishImageOrigin>()
        val resolver = resolver(
            cacheRoot(),
            FakeDishImageGenerator { webpBytes(24) },
            seed = seed,
            onResolved = { _, origin -> reportados += origin },
        )

        resolver.imageFor(request(SEED_KEY))
        resolver.imageFor(request(SEED_KEY))

        assertEquals(
            "El acierto de memoria no degrada el origen a GENERATED",
            listOf(DishImageOrigin.SEED, DishImageOrigin.SEED),
            reportados,
        )
        assertEquals("El asset se lee una sola vez", 1, seed.reads)
    }

    @Test
    fun un_acierto_de_disco_reporta_generado_sin_tocar_semilla_ni_generador() = runTest {
        val root = cacheRoot()
        resolver(root, FakeDishImageGenerator { webpBytes(32) }).imageFor(request(KEY))!!
        val seed = FakeDishImageSeedSource { webpBytes(48) }
        val offline = FakeDishImageGenerator { throw IllegalStateException("no debe generar") }
        val reportados = mutableListOf<DishImageOrigin>()

        resolver(root, offline, seed = seed, onResolved = { _, origin -> reportados += origin })
            .imageFor(request(KEY))

        assertEquals(listOf(DishImageOrigin.GENERATED), reportados)
        assertEquals(0, seed.reads)
    }

    @Test
    fun un_fallo_no_reporta_origen_y_la_cancelacion_se_propaga_sin_reportar() = runTest {
        val fallos = listOf<suspend (DishImageRequest) -> ByteArray?>({ null }, { ByteArray(0) })
        fallos.forEachIndexed { index, fallo ->
            val reportados = mutableListOf<DishImageOrigin>()
            val result = resolver(
                File(cacheRoot(), "fallo-$index"),
                FakeDishImageGenerator(fallo),
                onResolved = { _, origin -> reportados += origin },
            ).imageFor(request(KEY))

            assertNull("El fallo #$index degrada a null", result)
            assertTrue("El fallo #$index no reporta origen", reportados.isEmpty())
        }

        val reportadosEnCancelacion = mutableListOf<DishImageOrigin>()
        var propagada: CancellationException? = null
        try {
            resolver(
                File(cacheRoot(), "cancelado"),
                FakeDishImageGenerator { throw CancellationException("cancelado") },
                onResolved = { _, origin -> reportadosEnCancelacion += origin },
            ).imageFor(request(KEY))
        } catch (cancellation: CancellationException) {
            propagada = cancellation
        }

        assertNotNull("La cancelacion se sigue propagando", propagada)
        assertTrue("La cancelacion no reporta origen", reportadosEnCancelacion.isEmpty())
    }

    @Test
    fun el_callback_por_defecto_es_no_op_y_no_cambia_bytes_ni_orden_de_llamadas() = runTest {
        val rootPorDefecto = File(cacheRoot(), "por-defecto")
        val rootExplicito = File(cacheRoot(), "explicito")
        val generadorPorDefecto = FakeDishImageGenerator { webpBytes(32) }
        val generadorExplicito = FakeDishImageGenerator { webpBytes(32) }
        val semillaPorDefecto = FakeDishImageSeedSource { null }
        val semillaExplicita = FakeDishImageSeedSource { null }

        val porDefecto = resolver(rootPorDefecto, generadorPorDefecto, seed = semillaPorDefecto)
            .imageFor(request(OTHER_KEY))
        val explicito = resolver(
            rootExplicito,
            generadorExplicito,
            seed = semillaExplicita,
            onResolved = { _, _ -> },
        ).imageFor(request(OTHER_KEY))

        assertArrayEquals("Mismos bytes con y sin callback", porDefecto, explicito)
        assertEquals("Mismo numero de generaciones", generadorPorDefecto.calls, generadorExplicito.calls)
        assertEquals("Mismo numero de lecturas de semilla", semillaPorDefecto.reads, semillaExplicita.reads)
        assertEquals(
            "Mismo resultado persistido",
            rootPorDefecto.listFiles()!!.map { it.name },
            rootExplicito.listFiles()!!.map { it.name },
        )
    }

    private fun cacheRoot() = File(temporaryFolder.root, "dish-images")

    private fun resolver(
        root: File,
        generator: DishImageGenerator,
        memory: MemoryDishImageCache = MemoryDishImageCache(),
        maxBytes: Long = DishImageCachePolicy.MAX_DISK_BYTES,
        seed: DishImageSeedSource? = null,
        onResolved: (DishImageRequest, DishImageOrigin) -> Unit = { _, _ -> },
    ) = DishImageResolver(
        disk = FileDishImageCache(root, maxBytes = maxBytes),
        memory = memory,
        generator = generator,
        ioDispatcher = Dispatchers.Unconfined,
        seed = seed,
        onResolved = onResolved,
    )
}

private const val KEY = "dish-image-v2-arroz-con-pollo-criollo"
// Escenarios de la fase 7 (`dish-image-seed`): la clave del plato del set semilla es el mismo vector
// que ya usaba la fase 3.
private const val SEED_KEY = "dish-image-v2-arroz-con-pollo-criollo"
private const val KEY_A = "dish-image-v2-arroz-con-pollo-criollo"
private const val KEY_B = "dish-image-v2-tortilla-de-patatas"
private const val KEY_C = "dish-image-v2-paella-valenciana"
private const val OTHER_KEY = "dish-image-v2-lentejas-estofadas"

private fun request(cacheKey: String) = DishImageRequest(
    cacheKey = cacheKey,
    prompt = "Fotografía gastronómica de $cacheKey",
)

/** Generador falso: cuenta llamadas, no hace red y delega en [block] (que puede suspender o fallar). */
private class FakeDishImageGenerator(
    private val block: suspend (DishImageRequest) -> ByteArray?,
) : DishImageGenerator {

    var calls = 0
        private set

    override suspend fun generate(request: DishImageRequest): ByteArray? {
        calls += 1
        return block(request)
    }
}

/**
 * Fuente semilla falsa (fase 7): cuenta **lecturas** y delega en [block] (que puede suspender o
 * fallar), de modo que los escenarios comprueban que el asset se lee una sola vez y que la semilla
 * gana al generador sin red.
 */
private class FakeDishImageSeedSource(
    private val block: suspend (String) -> ByteArray?,
) : DishImageSeedSource {

    var reads = 0
        private set

    override suspend fun bytesFor(cacheKey: String): ByteArray? {
        reads += 1
        return block(cacheKey)
    }
}

/** Bytes con cabecera WebP (`RIFF`...`WEBP`), el formato que fija la fase 2. */
private fun webpBytes(size: Int = 12): ByteArray {
    val bytes = ByteArray(size.coerceAtLeast(12))
    "RIFF".toByteArray().copyInto(bytes, 0)
    "WEBP".toByteArray().copyInto(bytes, 8)
    return bytes
}

private fun ByteArray.isWebp(): Boolean =
    size >= 12 &&
        copyOfRange(0, 4).decodeToString() == "RIFF" &&
        copyOfRange(8, 12).decodeToString() == "WEBP"
