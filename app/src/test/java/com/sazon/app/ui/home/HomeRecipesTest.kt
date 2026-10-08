package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 5 de `docs/specs/home-offline-blocks.md`: el bloque "Tus recetas" esta vacio con un usuario
 * nuevo y deja de estarlo si cualquiera de los dos grupos trae contenido. Logica pura.
 */
class HomeRecipesTest {

    @Test
    fun sin_guardadas_ni_abiertas_el_bloque_esta_vacio() {
        assertTrue(HomeRecipes().isEmpty)
        assertTrue(HomeRecipes(saved = emptyList(), recentlyOpened = emptyList()).isEmpty)
    }

    @Test
    fun con_una_receta_guardada_el_bloque_no_esta_vacio() {
        val bloque = HomeRecipes(saved = listOf(receta("seed-1")), recentlyOpened = emptyList())

        assertFalse(bloque.isEmpty)
    }

    @Test
    fun con_una_receta_abierta_hace_poco_el_bloque_no_esta_vacio() {
        val bloque = HomeRecipes(saved = emptyList(), recentlyOpened = listOf(receta("ai-1")))

        assertFalse(bloque.isEmpty)
    }
}

private fun receta(id: String) = Recipe(
    id = id,
    source = RecipeSource.SEED,
    title = "Receta $id",
    description = "",
    categoryId = "other",
    mealTypeId = "lunch",
    cuisineId = "spanish",
    totalMinutes = 20,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = emptyList(),
)
