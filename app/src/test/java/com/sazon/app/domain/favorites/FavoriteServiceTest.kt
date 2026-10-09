package com.sazon.app.domain.favorites

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1, 2 y 10 de la spec (la parte de `toggle`): marcar guarda **una copia** con
 * `savedAt = now` y devuelve `true`; desmarcar la quita y devuelve `false`; `isFavorite` refleja el
 * estado; la copia normaliza `matchScore` a `null`; dos recetas coexisten. Logica pura (repositorio en
 * memoria), sin Android ni Robolectric.
 */
class FavoriteServiceTest {

    private val now = 1_700_000_000_000L

    private fun servicio(repository: FavoriteRepository) = FavoriteService(repository) { now }

    @Test
    fun marcar_una_receta_no_guardada_guarda_una_copia_con_saved_at_y_devuelve_true() = runTest {
        val repository = InMemoryFavoriteRepository()
        val recipe = receta("fake-ai-arroz", "Arroz frito", matchScore = 88)

        val marcado = servicio(repository).toggle(recipe)

        assertTrue("Marcar devuelve true", marcado)
        val favoritos = repository.get()
        assertEquals("Debe haber exactamente una fila", 1, favoritos.size)
        assertEquals(now, favoritos.single().savedAt)
        assertEquals(recipe.id, favoritos.single().recipeId)
        assertEquals(recipe.copy(matchScore = null), favoritos.single().recipe)
    }

    @Test
    fun la_copia_normaliza_match_score_a_null() = runTest {
        val repository = InMemoryFavoriteRepository()

        servicio(repository).toggle(receta("ai-1", "Receta con score", matchScore = 92))

        assertNull(
            "matchScore es derivado y no se persiste",
            repository.get().single().recipe.matchScore,
        )
    }

    @Test
    fun volver_a_alternar_la_quita_y_devuelve_false() = runTest {
        val repository = InMemoryFavoriteRepository()
        val recipe = receta("ai-2", "Lentejas")
        val service = servicio(repository)

        assertTrue(service.toggle(recipe))
        val desmarcado = service.toggle(recipe)

        assertFalse("Desmarcar devuelve false", desmarcado)
        assertTrue("La lista queda vacia (caso negativo)", repository.get().isEmpty())
        assertFalse(service.isFavorite(recipe.id))
    }

    @Test
    fun is_favorite_refleja_el_estado() = runTest {
        val repository = InMemoryFavoriteRepository()
        val recipe = receta("ai-3", "Tortilla")
        val service = servicio(repository)

        assertFalse("Sin guardar no es favorita", service.isFavorite(recipe.id))
        service.toggle(recipe)
        assertTrue("Tras marcar es favorita", service.isFavorite(recipe.id))
        service.toggle(recipe)
        assertFalse("Tras desmarcar deja de serlo", service.isFavorite(recipe.id))
    }

    @Test
    fun dos_recetas_distintas_coexisten() = runTest {
        val repository = InMemoryFavoriteRepository()
        val service = servicio(repository)
        val primera = receta("ai-4", "Primera")
        val segunda = receta("ai-5", "Segunda")

        service.toggle(primera)
        service.toggle(segunda)

        assertEquals("Dos favoritos coexisten", 2, repository.get().size)
        assertTrue(service.isFavorite(primera.id))
        assertTrue(service.isFavorite(segunda.id))
    }

    @Test
    fun remarcar_la_misma_receta_no_duplica() = runTest {
        val repository = InMemoryFavoriteRepository()
        val recipe = receta("ai-6", "Unica")
        val service = servicio(repository)

        service.toggle(recipe) // guarda
        service.toggle(recipe) // quita
        service.toggle(recipe) // vuelve a guardar

        assertEquals("Reescribir el mismo recipeId no duplica", 1, repository.get().size)
    }
}

/** Repositorio de Favoritos en memoria para los tests de [FavoriteService]. */
private class InMemoryFavoriteRepository : FavoriteRepository {

    private val state = MutableStateFlow<List<Favorite>>(emptyList())

    override suspend fun get(): List<Favorite> = state.value.sortedByDescending { it.savedAt }

    override fun observe(): Flow<List<Favorite>> = state

    override suspend fun isFavorite(recipeId: String): Boolean =
        state.value.any { it.recipeId == recipeId }

    override suspend fun save(favorite: Favorite) {
        state.value = state.value.filterNot { it.recipeId == favorite.recipeId } + favorite
    }

    override suspend fun remove(recipeId: String) {
        state.value = state.value.filterNot { it.recipeId == recipeId }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

private fun receta(
    id: String,
    title: String,
    matchScore: Int? = null,
): Recipe = Recipe(
    id = id,
    source = RecipeSource.AI,
    title = title,
    description = "Receta de prueba.",
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
    nutrition = Nutrition(kcal = 300, protein = 10, carbs = 40, fat = 8),
    matchScore = matchScore,
)
