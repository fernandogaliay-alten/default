package com.sazon.app.domain.activity

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Escenarios 3, 4 y 7 de la spec con un repositorio en memoria. Logica pura: sin Android ni
 * Robolectric. La regla "solo `ai`, solo suma" vive en `ActivitySummaryService`.
 */
class ActivitySummaryServiceTest {

    @Test
    fun una_lista_vacia_no_escribe_nada() = runTest {
        val repository = InMemoryActivitySummaryRepository(initial = 7)
        val service = ActivitySummaryService(repository)

        val result = service.recordAcceptedRecipes(emptyList())

        assertEquals("Devuelve el valor persistido", 7, result)
        assertEquals("No debe escribir con delta 0", 0, repository.incrementCalls)
    }

    @Test
    fun solo_las_recetas_seed_no_cuentan() = runTest {
        val repository = InMemoryActivitySummaryRepository(initial = 5)
        val service = ActivitySummaryService(repository)

        val result = service.recordAcceptedRecipes(
            listOf(recipe("seed-1", RecipeSource.SEED), recipe("seed-2", RecipeSource.SEED)),
        )

        assertEquals(5, result)
        assertEquals("Una generacion sin ai aceptadas no altera el contador", 0, repository.incrementCalls)
    }

    @Test
    fun cada_receta_ai_aceptada_suma_uno() = runTest {
        val repository = InMemoryActivitySummaryRepository(initial = 24)
        val service = ActivitySummaryService(repository)

        val result = service.recordAcceptedRecipes(
            listOf(recipe("ai-1", RecipeSource.AI), recipe("ai-2", RecipeSource.AI)),
        )

        assertEquals("C + N", 26, result)
        assertEquals(26, repository.recipesGeneratedCount())
    }

    @Test
    fun en_una_lista_mixta_solo_cuentan_las_ai() = runTest {
        val repository = InMemoryActivitySummaryRepository(initial = 0)
        val service = ActivitySummaryService(repository)

        val result = service.recordAcceptedRecipes(
            listOf(
                recipe("seed-1", RecipeSource.SEED),
                recipe("ai-1", RecipeSource.AI),
                recipe("ai-2", RecipeSource.AI),
                recipe("seed-2", RecipeSource.SEED),
            ),
        )

        assertEquals(2, result)
    }

    @Test
    fun el_contador_nunca_decrece_al_encadenar_eventos() = runTest {
        val repository = InMemoryActivitySummaryRepository(initial = 0)
        val service = ActivitySummaryService(repository)

        val primero = service.recordAcceptedRecipes(
            listOf(recipe("ai-1", RecipeSource.AI), recipe("ai-2", RecipeSource.AI)),
        )
        val soloSeed = service.recordAcceptedRecipes(listOf(recipe("seed-1", RecipeSource.SEED)))
        val segundo = service.recordAcceptedRecipes(listOf(recipe("ai-3", RecipeSource.AI)))
        val vacio = service.recordAcceptedRecipes(emptyList())
        val tercero = service.recordAcceptedRecipes(
            listOf(recipe("ai-4", RecipeSource.AI), recipe("ai-5", RecipeSource.AI)),
        )

        assertEquals(2, primero)
        assertEquals("Una generacion sin ai no baja el contador", 2, soloSeed)
        assertEquals(3, segundo)
        assertEquals("Una lista vacia no baja el contador", 3, vacio)
        assertEquals(5, tercero)
    }

    @Test
    fun observar_no_altera_el_contador() = runTest {
        val repository = InMemoryActivitySummaryRepository(initial = 11)
        val service = ActivitySummaryService(repository)

        // Escenario 7: solo aceptar recetas ai mueve el contador; consultarlo no lo cambia.
        assertEquals(11, service.recipesGeneratedCount())
        assertEquals(0, repository.incrementCalls)
        assertEquals(11, service.recipesGeneratedCount())
    }
}

private class InMemoryActivitySummaryRepository(initial: Int = 0) : ActivitySummaryRepository {

    private val state = MutableStateFlow(initial)

    var incrementCalls = 0
        private set

    override fun observeRecipesGeneratedCount(): Flow<Int> = state

    override suspend fun recipesGeneratedCount(): Int = state.value

    override suspend fun incrementRecipesGenerated(delta: Int) {
        incrementCalls++
        if (delta > 0) state.value += delta
    }
}

private fun recipe(id: String, source: RecipeSource): Recipe =
    Recipe(
        id = id,
        source = source,
        title = "Receta $id",
        description = "Receta de prueba.",
        categoryId = "rice",
        mealTypeId = "lunch",
        cuisineId = "asian",
        totalMinutes = 20,
        prepMinutes = 5,
        cookMinutes = 15,
        difficultyId = "easy",
        servings = 2,
        ingredients = listOf(RecipeIngredient("Arroz", 200.0, ingredientId = "arroz")),
        steps = listOf(RecipeStep(1, "Cocinar", "Cocina el arroz.", 15)),
        declaredAllergens = emptyList(),
    )
