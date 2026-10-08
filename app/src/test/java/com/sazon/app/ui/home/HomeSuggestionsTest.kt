package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 1 de `docs/specs/home-offline-blocks.md`: la Home se muestra poblada de inmediato con las
 * recetas semilla mientras el Lote no se ha resuelto. Logica pura, sin emulador.
 */
class HomeSuggestionsTest {

    private val seeds = listOf(receta("seed-1"), receta("seed-2"))
    private val lote = listOf(receta("ai-1"), receta("seed-1"))

    @Test
    fun sin_lote_resuelto_se_muestran_las_semillas() {
        assertEquals(seeds, HomeSuggestions.visible(batchRecipes = null, seedRecipes = seeds))
    }

    @Test
    fun con_lote_resuelto_se_muestran_las_recetas_del_lote() {
        assertEquals(lote, HomeSuggestions.visible(batchRecipes = lote, seedRecipes = seeds))
    }

    @Test
    fun un_lote_vacio_no_cae_a_semilla() {
        val visibles = HomeSuggestions.visible(batchRecipes = emptyList(), seedRecipes = seeds)

        assertTrue("Un lote resuelto vacio es un resultado real, no una ausencia", visibles.isEmpty())
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
