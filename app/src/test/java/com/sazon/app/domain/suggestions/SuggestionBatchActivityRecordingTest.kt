package com.sazon.app.domain.suggestions

import com.sazon.app.domain.activity.ActivitySummaryRepository
import com.sazon.app.domain.activity.ActivitySummaryService
import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 3, 4 y 6 de la spec: el refresco del Lote reporta sus recetas `ai` aceptadas al
 * `ActivitySummaryService` (contador persistido) y la caché no vuelve a contar. Logica pura con
 * generador falso y puertos en memoria; sin Android ni Robolectric.
 */
class SuggestionBatchActivityRecordingTest {

    private val taxonomy = recordingTaxonomy()
    private val now = 1_700_000_000_000L

    private val seedArroz = recordingSeedRecipe("seed-arroz", "Arroz con verduras")

    private fun aiRecipe(id: String, title: String): Recipe =
        Recipe(
            id = id,
            source = RecipeSource.AI,
            title = title,
            description = "Receta ai de prueba.",
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

    private fun servicio(
        generator: RecipeGenerator,
        batchRepository: SuggestionBatchRepository,
        activityRepository: ActivitySummaryRepository,
    ): SuggestionBatchService {
        val activityService = ActivitySummaryService(activityRepository)
        return SuggestionBatchService(
            generator = generator,
            repository = batchRepository,
            taxonomy = taxonomy,
            seedRecipes = listOf(seedArroz),
            now = { now },
            onRecipesGenerated = { recipes -> activityService.recordAcceptedRecipes(recipes) },
        )
    }

    private fun perfil() = PreferenceProfile(
        dietPatternId = "omnivore",
        hardRestrictionIds = emptyList(),
        softPreferenceIds = emptyList(),
        cuisineIds = emptyList(),
        preferredIngredientIds = emptyList(),
        dislikedIngredientIds = emptyList(),
        difficultyId = "easy",
        timeBucketId = "lte_15",
        defaultServings = 2,
        updatedAt = 1_600_000_000_000L,
    )

    // Escenario 3: un refresco registra cada ai aceptada.
    @Test
    fun un_refresco_del_lote_registra_las_ai_aceptadas() = runTest {
        val activity = RecordingActivityRepository()
        val generator = RecordingGenerator {
            listOf(aiRecipe("ai-1", "Arroz frito uno"), aiRecipe("ai-2", "Arroz frito dos"))
        }

        servicio(generator, RecordingBatchRepository(null), activity)
            .ensureBatch(perfil())

        assertEquals(2, activity.recipesGeneratedCount())
    }

    // Escenario 6: la caché no vuelve a contar.
    @Test
    fun la_segunda_llamada_servida_por_cache_no_registra() = runTest {
        val activity = RecordingActivityRepository()
        val batchRepository = RecordingBatchRepository(null)
        val generator = RecordingGenerator { listOf(aiRecipe("ai-1", "Arroz frito")) }
        val service = servicio(generator, batchRepository, activity)

        service.ensureBatch(perfil())
        assertEquals(1, activity.recipesGeneratedCount())

        // El Lote es fresco y la firma no cambio: la politica sirve la cache sin llamar al generador.
        service.ensureBatch(perfil())

        assertEquals("La cache no debe llamar al generador", 1, generator.calls)
        assertEquals("La cache no debe volver a contar", 1, activity.recipesGeneratedCount())
    }

    // Escenario 4 (verificacion exigida): un segundo refresco nunca baja el contador.
    @Test
    fun un_segundo_refresco_no_baja_el_contador() = runTest {
        val activity = RecordingActivityRepository()
        val batchRepository = RecordingBatchRepository(null)
        var generated: List<Recipe> = listOf(
            aiRecipe("ai-1", "Arroz uno"),
            aiRecipe("ai-2", "Arroz dos"),
            aiRecipe("ai-3", "Arroz tres"),
        )
        val generator = RecordingGenerator { generated }
        val service = servicio(generator, batchRepository, activity)

        service.ensureBatch(perfil(), force = true)
        val trasPrimero = activity.recipesGeneratedCount()
        assertEquals(3, trasPrimero)

        // El Lote se refresca con menos recetas ai: el contador no puede bajar.
        generated = listOf(aiRecipe("ai-4", "Arroz cuatro"))
        service.ensureBatch(perfil(), force = true)

        val trasSegundo = activity.recipesGeneratedCount()
        assertTrue("El contador nunca decrece", trasSegundo >= trasPrimero)
        assertEquals(4, trasSegundo)
    }

    // Escenario 4: un fallo del generador (solo semilla) tampoco baja el contador.
    @Test
    fun el_fallo_del_generador_no_altera_el_contador() = runTest {
        val activity = RecordingActivityRepository()
        val batchRepository = RecordingBatchRepository(null)
        var falla = false
        val generator = RecordingGenerator {
            if (falla) throw IllegalStateException("sin red o sin clave")
            listOf(aiRecipe("ai-1", "Arroz frito"))
        }
        val service = servicio(generator, batchRepository, activity)

        service.ensureBatch(perfil(), force = true)
        assertEquals(1, activity.recipesGeneratedCount())

        falla = true
        service.ensureBatch(perfil(), force = true)

        assertEquals("El fallo del generador no debe tocar el contador", 1, activity.recipesGeneratedCount())
        assertTrue(batchRepository.get()!!.recipes.all { it.source == RecipeSource.SEED })
    }
}

private class RecordingBatchRepository(initial: SuggestionBatch?) : SuggestionBatchRepository {

    private val state = MutableStateFlow(initial)

    override suspend fun get(): SuggestionBatch? = state.value

    override fun observe(): Flow<SuggestionBatch?> = state

    override suspend fun save(batch: SuggestionBatch) {
        state.value = batch
    }

    override suspend fun clear() {
        state.value = null
    }
}

private class RecordingActivityRepository : ActivitySummaryRepository {

    private val state = MutableStateFlow(0)

    override fun observeRecipesGeneratedCount(): Flow<Int> = state

    override suspend fun recipesGeneratedCount(): Int = state.value

    override suspend fun incrementRecipesGenerated(delta: Int) {
        if (delta > 0) state.value += delta
    }
}

private class RecordingGenerator(
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

private fun recordingSeedRecipe(id: String, title: String): Recipe =
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

private fun recordingTaxonomy(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}

