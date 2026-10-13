package com.sazon.app.ui.artwork

import com.sazon.app.domain.image.DishImageCacheKey
import com.sazon.app.domain.image.DishImageRequest
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 3-7 de `docs/specs/dish-image-progressive-ui.md`: el loader se ejercita con un resolutor
 * y un decodificador **falsos** (`T = String`), sin red, sin clave, sin cuota y sin Android. La
 * composicion de la peticion usa la clave canonica **real** (`DishImageCacheKey`), de modo que la
 * comparticion por titulo equivalente no pasa en vacio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DishImageArtworkLoaderTest {

    // Escenario 3: `null` o array vacio degradan a `null` SIN cachear nada.
    @Test
    fun un_resolutor_sin_imagen_devuelve_null_sin_cachear() = runTest {
        val nullResolver = FakeGeneratedResolver { null }
        val nullLoader = loader(nullResolver, decode = { it.decodeToString() })

        assertNull(nullLoader.imageFor(receta("Arroz con pollo criollo")))
        assertNull(nullLoader.imageFor(receta("Arroz con pollo criollo")))
        assertEquals("Un fallo no se cachea: se vuelve a resolver", 2, nullResolver.calls)
        assertEquals(0, nullLoader.decodedCount)

        val emptyResolver = FakeGeneratedResolver { ByteArray(0) }
        val emptyLoader = loader(emptyResolver, decode = { it.decodeToString() })

        assertNull(emptyLoader.imageFor(receta("Tortilla de patatas")))
        assertEquals(1, emptyResolver.calls)
        assertEquals(0, emptyLoader.decodedCount)
    }

    // Escenario 3 (codec roto): `decode` devuelve null -> null sin cachear.
    @Test
    fun un_decodificador_que_falla_devuelve_null_sin_cachear() = runTest {
        val resolver = FakeGeneratedResolver { "imagen".encodeToByteArray() }
        val loader = loader(resolver, decode = { null })

        assertNull(loader.imageFor(receta("Paella valenciana")))
        assertNull(loader.imageFor(receta("Paella valenciana")))

        assertEquals("El codec roto no se cachea", 2, resolver.calls)
        assertEquals(0, loader.decodedCount)
    }

    // Escenario 4: la misma clave canonica se resuelve una vez; despues sale del LRU sin gasto.
    @Test
    fun la_misma_clave_canonica_se_resuelve_una_vez() = runTest {
        val resolver = FakeGeneratedResolver { "imagen".encodeToByteArray() }
        var decodeCalls = 0
        val loader = loader(
            resolver,
            decode = {
                decodeCalls += 1
                it.decodeToString()
            },
        )

        val first = loader.imageFor(receta("Arroz con Pollo Criollo"))
        val second = loader.imageFor(receta("  arroz  con   pollo criollo "))

        assertNotNull(first)
        assertEquals("Dos titulos equivalentes comparten imagen", first, second)
        assertEquals("El resolutor se invoca una sola vez", 1, resolver.calls)
        assertEquals("Se decodifica una sola vez", 1, decodeCalls)
        assertEquals(1, loader.decodedCount)
    }

    // Escenario 5: la cancelacion se propaga (no se convierte en null ni se traga).
    @Test
    fun la_cancelacion_del_resolutor_se_propaga() = runTest {
        val resolver = FakeGeneratedResolver { throw CancellationException("cancelado") }
        val loader = loader(resolver, decode = { it.decodeToString() })
        var propagated: CancellationException? = null

        try {
            loader.imageFor(receta("Arroz con pollo criollo"))
        } catch (cancellation: CancellationException) {
            propagated = cancellation
        }

        assertNotNull("El loader debe relanzar la cancelacion", propagated)
        assertEquals(1, resolver.calls)
        assertEquals(0, loader.decodedCount)
    }

    // Escenario 6: con tres peticiones a la vez, nunca hay mas de 2 resoluciones en vuelo.
    @Test
    fun nunca_hay_mas_de_dos_resoluciones_en_vuelo() = runTest {
        val release = CompletableDeferred<Unit>()
        val resolver = FakeGeneratedResolver {
            release.await()
            it.cacheKey.encodeToByteArray()
        }
        val loader = loader(resolver, decode = { it.decodeToString() })

        val first = async { loader.imageFor(receta("Receta A")) }
        val second = async { loader.imageFor(receta("Receta B")) }
        val third = async { loader.imageFor(receta("Receta C")) }
        advanceUntilIdle()

        assertEquals("Dos en vuelo; la tercera espera al permiso", 2, resolver.maxInFlight)
        assertEquals("Solo dos han empezado a resolver", 2, resolver.calls)

        release.complete(Unit)
        advanceUntilIdle()

        assertNotNull(first.await())
        assertNotNull(second.await())
        assertNotNull(third.await())
        assertEquals("Las tres terminan", 3, resolver.calls)
        assertEquals(3, loader.decodedCount)
    }

    // Escenario 7: el LRU se acota y descarta la menos usada; comparte por clave canonica.
    @Test
    fun el_lru_se_acota_y_descarta_la_menos_usada() = runTest {
        val resolver = FakeGeneratedResolver { it.cacheKey.encodeToByteArray() }
        val loader = loader(resolver, decode = { it.decodeToString() }, maxDecoded = 2)

        loader.imageFor(receta("Receta A"))
        loader.imageFor(receta("Receta B"))
        assertEquals("El tope es 2", 2, loader.decodedCount)

        loader.imageFor(receta("Receta A"))
        assertEquals("El acierto no vuelve a resolver", 2, resolver.calls)

        loader.imageFor(receta("Receta C"))
        assertEquals(3, resolver.calls)
        assertEquals("Tras descartar una, el tope sigue en 2", 2, loader.decodedCount)

        loader.imageFor(receta("Receta A"))
        assertEquals("A sobrevive por haber sido usada", 3, resolver.calls)

        loader.imageFor(receta("Receta B"))
        assertEquals("B fue la descartada: se resuelve de nuevo", 4, resolver.calls)
        assertEquals(2, loader.decodedCount)
        assertTrue("El tope nunca se supera", loader.decodedCount <= 2)
    }

    private fun loader(
        resolver: FakeGeneratedResolver,
        decode: (ByteArray) -> String?,
        maxDecoded: Int = DishImageArtworkPolicy.MAX_DECODED_IMAGES,
        maxConcurrent: Int = DishImageArtworkPolicy.MAX_CONCURRENT_GENERATIONS,
    ) = DishImageArtworkLoader(
        requestFor = { recipe ->
            DishImageRequest(cacheKey = DishImageCacheKey.of(recipe), prompt = recipe.title)
        },
        resolve = resolver::resolve,
        decode = decode,
        maxConcurrent = maxConcurrent,
        maxDecoded = maxDecoded,
    )
}

/** Resolutor falso: cuenta invocaciones y el maximo de resoluciones simultaneas; sin red ni cuota. */
private class FakeGeneratedResolver(
    private val block: suspend (DishImageRequest) -> ByteArray?,
) {

    var calls = 0
        private set

    var maxInFlight = 0
        private set

    private var inFlight = 0

    suspend fun resolve(request: DishImageRequest): ByteArray? {
        calls += 1
        inFlight += 1
        maxInFlight = maxOf(maxInFlight, inFlight)
        return try {
            block(request)
        } finally {
            inFlight -= 1
        }
    }
}

private fun receta(title: String) = Recipe(
    id = title,
    source = RecipeSource.SEED,
    title = title,
    description = "",
    categoryId = "rice",
    mealTypeId = "lunch",
    cuisineId = "spanish",
    totalMinutes = 20,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 4,
    ingredients = emptyList(),
    steps = emptyList(),
)
