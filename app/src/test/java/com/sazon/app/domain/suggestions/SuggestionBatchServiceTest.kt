package com.sazon.app.domain.suggestions

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.PreferenceSignature
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1–9 de la spec con un generador falso, un repositorio en memoria y la Taxonomia real
 * leida por propiedad de sistema. Logica pura: sin Android ni Robolectric.
 */
class SuggestionBatchServiceTest {

    private val taxonomy = taxonomy()
    private val now = 1_700_000_000_000L

    private val seedArroz = seedRecipe("seed-arroz", "Arroz con verduras")
    private val seedLentejas = seedRecipe("seed-lentejas", "Lentejas estofadas")
    private val aiSegura = aiRecipe("fake-ai-segura", "Arroz frito seguro")
    private val aiInsegura = aiRecipe(
        id = "fake-ai-insegura",
        title = "Bocadillo de tomate",
        cuisineId = "spanish",
        ingredients = listOf(RecipeIngredient("Pan", 1.0, unit = null, ingredientId = null)),
    )

    private fun servicio(
        generator: RecipeGenerator,
        repository: SuggestionBatchRepository,
        seedRecipes: List<Recipe> = listOf(seedArroz, seedLentejas),
    ) = SuggestionBatchService(generator, repository, taxonomy, seedRecipes) { now }

    // Escenario 1: mismo perfil y lote fresco no generan llamada.
    @Test
    fun un_lote_fresco_con_la_misma_firma_no_llama_al_generador_ni_lo_reescribe() = runTest {
        val profile = perfil()
        val cached = SuggestionBatch(PreferenceSignature.of(profile), now - 1_000L, listOf(seedArroz))
        val repository = InMemorySuggestionBatchRepository(cached)
        val generator = CountingGenerator { listOf(aiSegura) }

        val result = servicio(generator, repository).ensureBatch(profile)

        assertEquals(cached, result)
        assertEquals("No debe llamarse al generador", 0, generator.calls)
        assertEquals("No debe reescribirse el lote", 0, repository.saveCount)
        assertEquals(cached, repository.get())
    }

    // Escenario 2: firma distinta marca obsoleto y regenera.
    @Test
    fun una_firma_distinta_regenera_y_persiste_con_la_firma_nueva() = runTest {
        val profile = perfil()
        val repository = InMemorySuggestionBatchRepository(
            SuggestionBatch("firma-antigua", now - 1_000L, listOf(seedArroz)),
        )
        val generator = CountingGenerator { listOf(aiSegura) }

        val result = servicio(generator, repository).ensureBatch(profile)

        assertEquals("Debe invocarse al generador", 1, generator.calls)
        assertEquals(PreferenceSignature.of(profile), result.preferenceSignature)
        assertEquals(PreferenceSignature.of(profile), repository.get()?.preferenceSignature)
        assertEquals(now, result.generatedAt)
    }

    // Escenario 3: caducidad marca obsoleto.
    @Test
    fun un_lote_caducado_regenera() = runTest {
        val profile = perfil()
        val repository = InMemorySuggestionBatchRepository(
            SuggestionBatch(
                PreferenceSignature.of(profile),
                now - SuggestionBatchPolicy.BATCH_TTL_MILLIS,
                listOf(seedArroz),
            ),
        )
        val generator = CountingGenerator { listOf(aiSegura) }

        servicio(generator, repository).ensureBatch(profile)

        assertEquals("Debe invocarse al generador", 1, generator.calls)
    }

    // Escenario 4: frontera del TTL resuelta en el servicio.
    @Test
    fun la_frontera_del_ttl_decide_entre_usar_la_cache_y_regenerar() = runTest {
        val profile = perfil()
        val signature = PreferenceSignature.of(profile)

        val justoAntes = InMemorySuggestionBatchRepository(
            SuggestionBatch(signature, now - SuggestionBatchPolicy.BATCH_TTL_MILLIS + 1, listOf(seedArroz)),
        )
        val generatorAntes = CountingGenerator { listOf(aiSegura) }
        servicio(generatorAntes, justoAntes).ensureBatch(profile)
        assertEquals("Un milisegundo antes del TTL sirve la cache", 0, generatorAntes.calls)

        val justoEnElLimite = InMemorySuggestionBatchRepository(
            SuggestionBatch(signature, now - SuggestionBatchPolicy.BATCH_TTL_MILLIS, listOf(seedArroz)),
        )
        val generatorLimite = CountingGenerator { listOf(aiSegura) }
        servicio(generatorLimite, justoEnElLimite).ensureBatch(profile)
        assertEquals("En el TTL exacto hay que regenerar", 1, generatorLimite.calls)
    }

    // Escenario 5 (verificacion exigida): sin IA disponible cae a solo semilla y queda persistido.
    @Test
    fun sin_ia_disponible_el_lote_cae_a_solo_semilla_y_queda_persistido() = runTest {
        val profile = perfil()
        val repository = InMemorySuggestionBatchRepository(null)
        val generator = CountingGenerator { throw IllegalStateException("sin red o sin clave") }

        val result = servicio(generator, repository).ensureBatch(profile)

        assertEquals(1, generator.calls)
        assertTrue("El lote no debe quedar vacio", result.recipes.isNotEmpty())
        assertTrue(
            "Solo debe haber recetas seed",
            result.recipes.all { it.source == RecipeSource.SEED },
        )
        assertEquals("Debe quedar persistido y utilizable offline", result, repository.get())
    }

    // Escenario 6: peticion explicita refresca aunque este fresco.
    @Test
    fun force_refresca_un_lote_fresco() = runTest {
        val profile = perfil()
        val repository = InMemorySuggestionBatchRepository(
            SuggestionBatch(PreferenceSignature.of(profile), now - 1_000L, listOf(seedArroz)),
        )
        val generator = CountingGenerator { listOf(aiSegura) }

        val result = servicio(generator, repository).ensureBatch(profile, force = true)

        assertEquals(1, generator.calls)
        assertTrue(result.recipes.any { it.source == RecipeSource.AI })
    }

    // Escenario 7: lote inexistente se genera y se persiste con seed y ai.
    @Test
    fun un_lote_inexistente_se_genera_y_se_persiste_con_seed_y_ai() = runTest {
        val profile = perfil()
        val repository = InMemorySuggestionBatchRepository(null)
        val generator = CountingGenerator { listOf(aiSegura) }

        val result = servicio(generator, repository).ensureBatch(profile)

        assertEquals(1, generator.calls)
        assertTrue("Debe contener semillas", result.recipes.any { it.source == RecipeSource.SEED })
        assertTrue("Debe contener la ai valida", result.recipes.any { it.source == RecipeSource.AI })
        assertEquals(now, result.generatedAt)
        assertEquals(result, repository.get())
    }

    // Escenario 8 (caso negativo de seguridad): la ai insegura se descarta, la segura entra.
    @Test
    fun una_receta_ai_insegura_se_descarta_y_la_segura_entra() = runTest {
        val profile = perfil(hardRestrictions = listOf("gluten"))
        val repository = InMemorySuggestionBatchRepository(null)
        val generator = CountingGenerator { listOf(aiSegura, aiInsegura) }

        val result = servicio(generator, repository).ensureBatch(profile)

        assertTrue("La ai segura debe entrar", result.recipes.any { it.id == "fake-ai-segura" })
        assertFalse("La ai insegura debe descartarse", result.recipes.any { it.id == "fake-ai-insegura" })
        assertTrue("Todas las semillas entran", result.recipes.any { it.id == "seed-arroz" })
        assertTrue(result.recipes.any { it.id == "seed-lentejas" })
    }

    // Escenario 9: duplicados seed/ai por titulo normalizado, gana la seed.
    @Test
    fun los_duplicados_por_titulo_se_deduplican_y_gana_la_seed() = runTest {
        val profile = perfil()
        val repository = InMemorySuggestionBatchRepository(null)
        val generador = CountingGenerator {
            listOf(aiRecipe("fake-ai-arroz", "arroz con VERDURAS"))
        }
        val semilla = seedRecipe("seed-arroz", "Arroz con verduras")

        val result = servicio(generador, repository, seedRecipes = listOf(semilla)).ensureBatch(profile)

        assertEquals("Debe quedar una sola receta para el titulo", 1, result.recipes.size)
        assertEquals("Gana la seed", "seed-arroz", result.recipes.single().id)
        assertEquals(RecipeSource.SEED, result.recipes.single().source)
    }
}

private class InMemorySuggestionBatchRepository(initial: SuggestionBatch?) : SuggestionBatchRepository {

    private val state = MutableStateFlow(initial)

    var saveCount = 0
        private set

    override suspend fun get(): SuggestionBatch? = state.value

    override fun observe(): Flow<SuggestionBatch?> = state

    override suspend fun save(batch: SuggestionBatch) {
        saveCount++
        state.value = batch
    }

    override suspend fun clear() {
        state.value = null
    }
}

private class CountingGenerator(
    private val result: () -> List<Recipe>,
) : RecipeGenerator {

    var calls = 0
        private set

    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> {
        calls++
        return result()
    }

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> =
        error("searchByText no se usa en el Lote de sugerencias")

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = error("generateFromIngredients no se usa en el Lote de sugerencias")
}

private fun perfil(
    hardRestrictions: List<String> = emptyList(),
    dietPattern: String = "omnivore",
) = PreferenceProfile(
    dietPatternId = dietPattern,
    hardRestrictionIds = hardRestrictions,
    softPreferenceIds = emptyList(),
    cuisineIds = emptyList(),
    preferredIngredientIds = emptyList(),
    dislikedIngredientIds = emptyList(),
    difficultyId = "easy",
    timeBucketId = "lte_15",
    defaultServings = 2,
    updatedAt = 1_600_000_000_000L,
)

private fun seedRecipe(id: String, title: String): Recipe =
    Recipe(
        id = id,
        source = RecipeSource.SEED,
        title = title,
        description = "Receta semilla de prueba.",
        categoryId = "rice",
        mealTypeId = "lunch",
        cuisineId = "asian",
        totalMinutes = 20,
        prepMinutes = 5,
        cookMinutes = 15,
        difficultyId = "easy",
        servings = 2,
        ingredients = listOf(RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz")),
        steps = listOf(RecipeStep(1, "Cocinar", "Cocina el arroz.", 15)),
        declaredAllergens = emptyList(),
    )

private fun aiRecipe(
    id: String,
    title: String,
    cuisineId: String = "asian",
    ingredients: List<RecipeIngredient> =
        listOf(RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz")),
): Recipe =
    Recipe(
        id = id,
        source = RecipeSource.AI,
        title = title,
        description = "Receta ai de prueba.",
        categoryId = "rice",
        mealTypeId = "lunch",
        cuisineId = cuisineId,
        totalMinutes = 20,
        prepMinutes = 5,
        cookMinutes = 15,
        difficultyId = "easy",
        servings = 2,
        ingredients = ingredients,
        steps = listOf(RecipeStep(1, "Cocinar", "Cocina los ingredientes.", 15)),
        tips = emptyList(),
        nutrition = Nutrition(kcal = 300, protein = 10, carbs = 40, fat = 8),
        declaredAllergens = emptyList(),
    )

private fun taxonomy(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
