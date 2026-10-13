package com.sazon.app.data.image

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
 * Escenarios 1-5, 8-11 de `docs/specs/dish-image-cache.md`: el resolutor orquesta memoria -> disco ->
 * generador -> persistir con un **generador falso** (cuenta llamadas, sin red, sin clave y sin cuota)
 * y un directorio temporal. Todo en JVM puro; el almacen es real (ficheros en disco), de modo que las
 * aserciones de persistencia no pasan en vacio.
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

    private fun cacheRoot() = File(temporaryFolder.root, "dish-images")

    private fun resolver(
        root: File,
        generator: DishImageGenerator,
        memory: MemoryDishImageCache = MemoryDishImageCache(),
        maxBytes: Long = DishImageCachePolicy.MAX_DISK_BYTES,
    ) = DishImageResolver(
        disk = FileDishImageCache(root, maxBytes = maxBytes),
        memory = memory,
        generator = generator,
        ioDispatcher = Dispatchers.Unconfined,
    )
}

private const val KEY = "dish-image-v1-arroz-con-pollo-criollo"
private const val KEY_A = "dish-image-v1-arroz-con-pollo-criollo"
private const val KEY_B = "dish-image-v1-tortilla-de-patatas"
private const val KEY_C = "dish-image-v1-paella-valenciana"
private const val OTHER_KEY = "dish-image-v1-lentejas-estofadas"

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
