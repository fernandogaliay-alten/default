package com.sazon.app.data.image

import com.sazon.app.data.DishImageSeedSource
import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Resolutor de la imagen de plato: orquesta **memoria -> disco -> semilla -> generador real ->
 * persistir** por la clave canonica del peticion. Es el nivel que la fase 4
 * (`dish-image-progressive-ui`) inserta delante de la cadena de `recipe-photos`.
 *
 * Orden y garantias:
 *
 * 1. **Memoria**: si hay bytes, se devuelven sin tocar disco ni proveedor.
 * 2. **Seccion critica por clave**: una [Mutex] por `cacheKey` **serializa** las peticiones
 *    simultaneas del mismo plato, de modo que dos tarjetas del mismo plato no pagan dos veces ~2 % de
 *    cuota. Dentro del cerrojo se re-consulta la memoria y, si no, el disco en [ioDispatcher].
 * 3. **Semilla** (fase 7): si el disco no tiene copia y [seed] devuelve bytes no vacios, se guardan en
 *    **memoria** y se devuelven **sin** escribir en disco y **sin** invocar al generador. El asset ya
 *    viaja en el APK: duplicarlo consumiria el presupuesto de la cache y podria perderse por eviccion.
 *    El **disco gana a la semilla** (un plato con copia generada antes de existir el asset se sirve de
 *    su copia, como en la fase 3). `null`/array vacio/excepcion de la fuente degradan a `null` (la
 *    cadena sigue); la cancelacion se relanza.
 * 4. **Generador**: `null` o array vacio = "no disponible" -> se devuelve `null` **sin** persistir
 *    nada. Cualquier `Exception` se degrada a `null`; la **cancelacion** se relanza (la fase 4 cancela
 *    al salir de pantalla o al hacer scroll).
 * 5. **Persistir y devolver**: la persistencia es *best-effort* (`disk.write` puede fallar y se
 *    devuelven igualmente los bytes generados), despues se guardan en memoria.
 *
 * No conoce el modelo de recetas ni la Taxonomia: consume el [DishImageRequest] ya compuesto por
 * `DishImageRequest.of(taxonomy, recipe)` (fase 1) y usa `request.cacheKey`/`request.prompt` tal cual.
 * Con `seed = null` (parametro nuevo al final, por defecto) el comportamiento es **exactamente** el de
 * la fase 3.
 *
 * [onResolved] es una costura **aditiva** de diagnostico (parametro nuevo al final con default no-op):
 * se invoca con el [DishImageOrigin] resuelto ([DishImageOrigin.SEED] cuando sirven los bytes del set
 * semilla y [DishImageOrigin.GENERATED] en memoria/disco/generador). **No** cambia los bytes devueltos,
 * el orden de la cadena, la `Mutex` por clave ni la propagacion de la cancelacion; un fallo **no** la
 * invoca. Para que un acierto de **memoria** sobre bytes semilla siga reportando `SEED`, el resolutor
 * recuerda por clave el origen con el que poblo esa memoria (default `GENERATED`).
 */
class DishImageResolver(
    private val disk: FileDishImageCache,
    private val memory: MemoryDishImageCache,
    private val generator: DishImageGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val seed: DishImageSeedSource? = null,
    private val onResolved: (DishImageRequest, DishImageOrigin) -> Unit = { _, _ -> },
) {

    private val locks = ConcurrentHashMap<String, Mutex>()

    /** Origen con el que se poblo la memoria por clave; `null` = desconocido (se asume generado). */
    private val memoryOrigins = ConcurrentHashMap<String, DishImageOrigin>()

    /** Imagen de [request]: memoria, disco, semilla, generacion; `null` si no hay ninguna disponible. */
    suspend fun imageFor(request: DishImageRequest): ByteArray? {
        val cacheKey = request.cacheKey
        memory.get(cacheKey)?.let { bytes ->
            onResolved(request, originOf(cacheKey))
            return bytes
        }

        val mutex = locks.computeIfAbsent(cacheKey) { Mutex() }
        return mutex.withLock {
            memory.get(cacheKey)?.let { bytes ->
                onResolved(request, originOf(cacheKey))
                return@withLock bytes
            }

            val fromDisk = withContext(ioDispatcher) { disk.read(cacheKey) }
            if (fromDisk != null) {
                remember(cacheKey, DishImageOrigin.GENERATED)
                memory.put(cacheKey, fromDisk)
                onResolved(request, DishImageOrigin.GENERATED)
                return@withLock fromDisk
            }

            val seeded = seedOrNull(cacheKey)
            if (seeded != null) {
                remember(cacheKey, DishImageOrigin.SEED)
                memory.put(cacheKey, seeded)
                onResolved(request, DishImageOrigin.SEED)
                return@withLock seeded
            }

            val generated = generateOrNull(request)
            if (generated == null || generated.isEmpty()) return@withLock null

            withContext(ioDispatcher) { disk.write(cacheKey, generated) }
            remember(cacheKey, DishImageOrigin.GENERATED)
            memory.put(cacheKey, generated)
            onResolved(request, DishImageOrigin.GENERATED)
            generated
        }
    }

    /** Origen recordado para [cacheKey] al poblar la memoria; `GENERATED` si no consta. */
    private fun originOf(cacheKey: String): DishImageOrigin =
        memoryOrigins[cacheKey] ?: DishImageOrigin.GENERATED

    private fun remember(cacheKey: String, origin: DishImageOrigin) {
        memoryOrigins[cacheKey] = origin
    }

    /**
     * Bytes del asset semilla de [cacheKey] (fase 7), o `null` si no hay fuente, no hay asset, esta
     * vacio o falla. Degrada cualquier fallo a `null`, pero **relanza** la cancelacion.
     */
    private suspend fun seedOrNull(cacheKey: String): ByteArray? {
        val source = seed ?: return null
        return try {
            source.bytesFor(cacheKey)?.takeIf { it.isNotEmpty() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            null
        }
    }

    /** Genera degradando cualquier fallo a `null`, pero **relanzando** la cancelacion. */
    private suspend fun generateOrNull(request: DishImageRequest): ByteArray? = try {
        generator.generate(request)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        null
    }
}
