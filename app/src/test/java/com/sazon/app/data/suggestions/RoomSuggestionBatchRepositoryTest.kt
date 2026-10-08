package com.sazon.app.data.suggestions

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.suggestions.SuggestionBatch
import com.sazon.app.domain.suggestions.SuggestionBatchRepository
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Escenario 10 de la spec: ida y vuelta exacta de persistencia del Lote, fila unica al sobrescribir,
 * `clear()` -> `null` y emision por `observe()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomSuggestionBatchRepositoryTest {

    private lateinit var database: SuggestionBatchDatabase
    private lateinit var repository: SuggestionBatchRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SuggestionBatchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomSuggestionBatchRepository(database.suggestionBatchDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun un_lote_ausente_se_devuelve_como_null() = runTest {
        assertNull(repository.get())
    }

    @Test
    fun guarda_relee_y_compara_un_lote_completo_con_campos_opcionales() = runTest {
        val original = loteCompleto()

        repository.save(original)

        assertEquals(original, repository.get())
    }

    @Test
    fun sobrescribe_la_fila_unica() = runTest {
        val primero = loteCompleto()
        val segundo = loteCompleto().copy(preferenceSignature = "firma-nueva", generatedAt = 42L)

        repository.save(primero)
        repository.save(segundo)

        assertEquals(segundo, repository.get())
        assertEquals("Debe seguir habiendo una sola fila", 1, contarFilas())
    }

    @Test
    fun clear_deja_la_lectura_en_null() = runTest {
        repository.save(loteCompleto())

        repository.clear()

        assertNull(repository.get())
        assertEquals(0, contarFilas())
    }

    @Test
    fun los_cambios_se_observan_como_flow() = runTest {
        val emisiones = Collections.synchronizedList(mutableListOf<SuggestionBatch?>())
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe().collect { emisiones.add(it) }
        }

        esperarHastaQue { emisiones.isNotEmpty() }
        assertNull("La primera emision debe ser null", emisiones.first())

        val lote = loteCompleto()
        repository.save(lote)

        esperarHastaQue { emisiones.size >= 2 }
        assertEquals(lote, emisiones[1])
        assertFalse(emisiones.size > 2)

        job.cancel()
    }

    private fun contarFilas(): Int =
        database.query("SELECT COUNT(*) FROM suggestion_batch", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private suspend fun esperarHastaQue(condicion: () -> Boolean) {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                while (!condicion()) {
                    delay(20)
                }
            }
        }
    }
}

private fun loteCompleto() = SuggestionBatch(
    preferenceSignature = "firma-de-prueba",
    generatedAt = 1_700_000_000_000L,
    recipes = listOf(
        Recipe(
            id = "seed-arroz",
            source = RecipeSource.SEED,
            title = "Arroz con verduras",
            description = "Receta semilla de prueba.",
            categoryId = "rice",
            mealTypeId = "lunch",
            cuisineId = "asian",
            totalMinutes = 20,
            prepMinutes = 5,
            cookMinutes = 15,
            difficultyId = "easy",
            servings = 2,
            ingredients = listOf(
                RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz"),
            ),
            steps = listOf(RecipeStep(1, "Cocinar", "Cocina el arroz.", 15)),
            declaredAllergens = emptyList(),
        ),
        Recipe(
            id = "fake-ai-libre",
            source = RecipeSource.AI,
            title = "Bocadillo de tomate",
            description = "Receta ai de prueba con campos opcionales.",
            categoryId = "sandwich_wrap",
            mealTypeId = "breakfast",
            cuisineId = "spanish",
            totalMinutes = 5,
            prepMinutes = 5,
            cookMinutes = 0,
            difficultyId = "easy",
            servings = 1,
            ingredients = listOf(
                RecipeIngredient("Pan", 1.0, unit = null, ingredientId = null),
                RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"),
            ),
            steps = listOf(RecipeStep(1, "Montar", "Monta el bocadillo.", 5)),
            tips = emptyList(),
            nutrition = Nutrition(kcal = 250, protein = 8, carbs = 30, fat = 9),
            declaredAllergens = listOf("gluten"),
            matchScore = null,
        ),
    ),
)
