package com.sazon.app.ui.artwork

import com.sazon.app.domain.image.DishImageRequest
import com.sazon.app.domain.recipe.Recipe
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Carga la imagen generada de una receta y la **decodifica** a un tipo de imagen del que la UI pueda
 * pintar (`ImageBitmap` en la app, `T` = `String` en los tests).
 *
 * Es una pieza **generica y JVM-testable**: no importa Compose ni Android (la decodificacion entra
 * por [decode]) y toda la politica (concurrencia, tope del LRU) viene de
 * [DishImageArtworkPolicy]. La resolucion real la aporta `data.image.DishImageResolver`
 * (memoria -> disco -> generador, fase 3) por [resolve]; el loader **no** conoce la red ni el
 * proveedor.
 *
 * Contrato (vinculante):
 *
 * 1. La peticion se compone con [requestFor] (`DishImageRequest.of(taxonomy, recipe)` en la app); si
 *    el LRU de imagenes decodificadas ya tiene `request.cacheKey`, se devuelve **sin** llamar a
 *    [resolve] ni a [decode] (una imagen generada se paga una vez; el scroll de ida y vuelta no
 *    espera ni gasta cuota).
 * 2. La resolucion corre bajo un [Semaphore] de [maxConcurrent] permisos: nunca hay mas de ese numero
 *    de resoluciones en vuelo (la `Mutex` por clave de la fase 3 sigue evitando pagar dos veces el
 *    mismo plato).
 * 3. `null` o array vacio de [resolve] -> `null` **sin cachear** nada (la UI conserva la foto local).
 * 4. `decode` -> `null` (codec roto) -> `null` **sin cachear**; un fichero corrupto degrada a la
 *    cadena local.
 * 5. Exito -> se guarda por **clave canonica** (`request.cacheKey`, de modo que dos titulos
 *    equivalentes comparten imagen) y se devuelve; `CancellationException` **se propaga** (no se
 *    convierte en `null`).
 */
class DishImageArtworkLoader<T : Any>(
    private val requestFor: (Recipe) -> DishImageRequest,
    private val resolve: suspend (DishImageRequest) -> ByteArray?,
    private val decode: (ByteArray) -> T?,
    private val maxConcurrent: Int = DishImageArtworkPolicy.MAX_CONCURRENT_GENERATIONS,
    private val maxDecoded: Int = DishImageArtworkPolicy.MAX_DECODED_IMAGES,
) {

    private val semaphore = Semaphore(maxConcurrent.coerceAtLeast(1))

    // LinkedHashMap en modo acceso (el mas reciente al final) con eviccion del mas antiguo: el
    // precedente aceptado de `AssetRecipePhotoSource`, aqui para las imagenes decodificadas
    // generadas. El acceso va bajo `synchronized` porque varias corrutinas pueden resolver a la vez.
    private val decoded = object : LinkedHashMap<String, T>(maxDecoded.coerceAtLeast(1), 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>): Boolean =
            size > maxDecoded
    }

    /** Numero de imagenes decodificadas retenidas; nunca supera [maxDecoded] (seam de test). */
    val decodedCount: Int get() = synchronized(decoded) { decoded.size }

    /** Imagen generada de [recipe], o `null` si no esta disponible (degrada a la cadena local). */
    suspend fun imageFor(recipe: Recipe): T? {
        val request = requestFor(recipe)
        cached(request.cacheKey)?.let { return it }

        val bytes = semaphore.withPermit { resolve(request) }
        if (bytes == null || bytes.isEmpty()) return null

        val image = decode(bytes) ?: return null
        synchronized(decoded) { decoded[request.cacheKey] = image }
        return image
    }

    private fun cached(cacheKey: String): T? = synchronized(decoded) { decoded[cacheKey] }
}
