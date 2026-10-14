package com.sazon.app.ui.artwork

import com.sazon.app.data.image.DishImageSettings
import com.sazon.app.data.image.DishImageSettingsPolicy
import com.sazon.app.data.image.GatedDishImageGenerator
import com.sazon.app.domain.image.DishImageCacheKey
import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-5 de `docs/specs/dish-artwork-as-card-background.md`: el **gate del interruptor** y la
 * **degradacion** de la capa de fondo, ejercitados tal como los cablea `MainActivity`
 * (`settings.generatedIfEnabled { loader.imageFor(recipe) }`) con un resolutor y un decodificador
 * **falsos** (`T = String`), sin red, sin clave, sin cuota y sin Android. La afirmacion del resultado
 * usa la capa pura ([RecipeArtworkImagePrecedence.layerOf]) para cerrar el circulo con la UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeBackgroundGateTest {

    // Escenario 1: apagado -> null SIN invocar al resolutor (0 cache, 0 semilla, 0 generador).
    @Test
    fun apagado_no_fondo_y_cero_invocaciones_del_resolutor() = runTest {
        val resolver = CountingResolver { "imagen".encodeToByteArray() }
        val loader = loader(resolver)
        val settings = DishImageSettings(enabled = false)

        val result = settings.generatedIfEnabled { loader.imageFor(receta("Arroz con pollo criollo")) }

        assertNull("Apagado no produce imagen", result)
        assertEquals("Apagado no toca el resolutor (cache/semilla/generador)", 0, resolver.calls)
        assertEquals(0, loader.decodedCount)
        assertEquals(RecipeArtworkImageLayer.TYPOGRAPHIC, RecipeArtworkImagePrecedence.layerOf(result))
        assertEquals(0f, RecipeArtworkImagePrecedence.crossfadeTarget(result), 0f)
    }

    // Escenario 2: encendido + imagen disponible -> GENERATED con alfa objetivo 1f.
    @Test
    fun encendido_con_imagen_es_fondo_con_alfa_uno() = runTest {
        val resolver = CountingResolver { "imagen".encodeToByteArray() }
        val loader = loader(resolver)
        val settings = DishImageSettings(enabled = true)

        val result = settings.generatedIfEnabled { loader.imageFor(receta("Arroz con pollo criollo")) }

        assertNotNull(result)
        assertEquals("Se resuelve una sola vez", 1, resolver.calls)
        assertEquals(RecipeArtworkImageLayer.GENERATED, RecipeArtworkImagePrecedence.layerOf(result))
        assertEquals(1f, RecipeArtworkImagePrecedence.crossfadeTarget(result), 0f)
        assertEquals(
            "El alfa de la capa sale de la precedencia",
            1f,
            DishImageArtworkOverlay.crossfadeAlpha(RecipeArtworkImagePrecedence.layerOf(result)),
            0f,
        )
        assertTrue(DishImageArtworkOverlay.headingOnImage(RecipeArtworkImageLayer.GENERATED))
        assertFalse(DishImageArtworkOverlay.watermarkVisible(RecipeArtworkImageLayer.GENERATED))
    }

    // Escenario 3: encendido + null (sin red/clave) o codec roto -> TYPOGRAPHIC, sin cachear.
    @Test
    fun encendido_sin_imagen_o_con_codec_roto_cae_a_tipografica() = runTest {
        val settings = DishImageSettings(enabled = true)

        val nullResolver = CountingResolver { null }
        val nullLoader = loader(nullResolver)
        val nullResult = settings.generatedIfEnabled { nullLoader.imageFor(receta("Tortilla de patatas")) }

        assertNull(nullResult)
        assertEquals(1, nullResolver.calls)
        assertEquals(RecipeArtworkImageLayer.TYPOGRAPHIC, RecipeArtworkImagePrecedence.layerOf(nullResult))
        assertEquals(0f, RecipeArtworkImagePrecedence.crossfadeTarget(nullResult), 0f)
        assertTrue(DishImageArtworkOverlay.watermarkVisible(RecipeArtworkImageLayer.TYPOGRAPHIC))

        val brokenLoader = DishImageArtworkLoader(
            requestFor = ::requestOf,
            resolve = { "bytes".encodeToByteArray() },
            decode = { null },
        )
        val brokenResult = settings.generatedIfEnabled { brokenLoader.imageFor(receta("Paella valenciana")) }

        assertNull("Un codec roto degrada a null", brokenResult)
        assertEquals(RecipeArtworkImageLayer.TYPOGRAPHIC, RecipeArtworkImagePrecedence.layerOf(brokenResult))
        assertEquals(0, brokenLoader.decodedCount)
    }

    // Escenario 4a: "tarda" (el timeout de la politica se degrada aguas arriba a null) -> TYPOGRAPHIC.
    @Test
    fun una_resolucion_lenta_que_cae_a_null_es_tipografica() = runTest {
        val resolver = CountingResolver {
            delay(1_000)
            null
        }
        val settings = DishImageSettings(enabled = true)

        val result = settings.generatedIfEnabled { loader(resolver).imageFor(receta("Arroz con pollo criollo")) }

        assertNull(result)
        assertEquals(RecipeArtworkImageLayer.TYPOGRAPHIC, RecipeArtworkImagePrecedence.layerOf(result))
        assertEquals(1, resolver.calls)
    }

    // Escenario 4b: la cancelacion se propaga (no se convierte en null ni se traga).
    @Test
    fun la_cancelacion_se_propaga() = runTest {
        val resolver = CountingResolver { throw CancellationException("cancelado") }
        val settings = DishImageSettings(enabled = true)
        var propagated: CancellationException? = null

        try {
            settings.generatedIfEnabled { loader(resolver).imageFor(receta("Arroz con pollo criollo")) }
        } catch (cancellation: CancellationException) {
            propagated = cancellation
        }

        assertNotNull("El seam debe relanzar la cancelacion", propagated)
        assertEquals(1, resolver.calls)
    }

    // Escenario 5: tope por sesion agotado -> null SIN invocar al delegado (0 cuota).
    @Test
    fun con_el_tope_agotado_el_decorador_no_invoca_al_generador() = runTest {
        val settings = DishImageSettings(enabled = true)
        val delegate = CountingGenerator()
        val gated = GatedDishImageGenerator(delegate = delegate, settings = settings)

        repeat(DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION) {
            assertTrue("Reserva dentro del tope", settings.tryAcquireGeneration())
        }
        assertTrue("El tope queda agotado", settings.state.value.exhausted)

        val result = gated.generate(requestOf(receta("Arroz con pollo criollo")))

        assertNull(result)
        assertEquals("Agotado no invoca al delegado (0 cuota)", 0, delegate.calls)
        assertEquals(RecipeArtworkImageLayer.TYPOGRAPHIC, RecipeArtworkImagePrecedence.layerOf(result))
    }

    private fun loader(resolver: CountingResolver) = DishImageArtworkLoader(
        requestFor = ::requestOf,
        resolve = resolver::resolve,
        decode = { it.decodeToString() },
    )
}

/** Resolutor falso: cuenta invocaciones; sin red ni cuota. */
private class CountingResolver(
    private val block: suspend (DishImageRequest) -> ByteArray?,
) {
    var calls = 0
        private set

    suspend fun resolve(request: DishImageRequest): ByteArray? {
        calls += 1
        return block(request)
    }
}

/** Generador falso: cuenta invocaciones del borde de generacion. */
private class CountingGenerator : DishImageGenerator {
    var calls = 0
        private set

    override suspend fun generate(request: DishImageRequest): ByteArray? {
        calls += 1
        return "imagen".encodeToByteArray()
    }
}

private fun requestOf(recipe: Recipe) = DishImageRequest(
    cacheKey = DishImageCacheKey.of(recipe),
    prompt = recipe.title,
)

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
