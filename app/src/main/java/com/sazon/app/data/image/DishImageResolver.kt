package com.sazon.app.data.image

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
 * Resolutor de la imagen de plato: orquesta **memoria -> disco -> generador real -> persistir** por la
 * clave canonica del peticion. Es el nivel que la fase 4 (`dish-image-progressive-ui`) insertara
 * delante de la cadena de `recipe-photos`; aqui queda **sin consumidores ni cableado**.
 *
 * Orden y garantias:
 *
 * 1. **Memoria**: si hay bytes, se devuelven sin tocar disco ni proveedor.
 * 2. **Seccion critica por clave**: una [Mutex] por `cacheKey` **serializa** las peticiones
 *    simultaneas del mismo plato, de modo que dos tarjetas del mismo plato no pagan dos veces ~2 % de
 *    cuota. Dentro del cerrojo se re-consulta la memoria y, si no, el disco en [ioDispatcher].
 * 3. **Generador**: `null` o array vacio = "no disponible" -> se devuelve `null` **sin** persistir
 *    nada. Cualquier `Exception` se degrada a `null`; la **cancelacion** se relanza (la fase 4 cancela
 *    al salir de pantalla o al hacer scroll).
 * 4. **Persistir y devolver**: la persistencia es *best-effort* (`disk.write` puede fallar y se
 *    devuelven igualmente los bytes generados), despues se guardan en memoria.
 *
 * No conoce el modelo de recetas ni la Taxonomia: consume el [DishImageRequest] ya compuesto por
 * `DishImageRequest.of(taxonomy, recipe)` (fase 1) y usa `request.cacheKey`/`request.prompt` tal cual.
 */
class DishImageResolver(
    private val disk: FileDishImageCache,
    private val memory: MemoryDishImageCache,
    private val generator: DishImageGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val locks = ConcurrentHashMap<String, Mutex>()

    /** Imagen de [request]: memoria, disco, generacion; `null` si no hay ninguna disponible. */
    suspend fun imageFor(request: DishImageRequest): ByteArray? {
        val cacheKey = request.cacheKey
        memory.get(cacheKey)?.let { return it }

        val mutex = locks.computeIfAbsent(cacheKey) { Mutex() }
        return mutex.withLock {
            memory.get(cacheKey)?.let { return@withLock it }

            val fromDisk = withContext(ioDispatcher) { disk.read(cacheKey) }
            if (fromDisk != null) {
                memory.put(cacheKey, fromDisk)
                return@withLock fromDisk
            }

            val generated = generateOrNull(request)
            if (generated == null || generated.isEmpty()) return@withLock null

            withContext(ioDispatcher) { disk.write(cacheKey, generated) }
            memory.put(cacheKey, generated)
            generated
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
