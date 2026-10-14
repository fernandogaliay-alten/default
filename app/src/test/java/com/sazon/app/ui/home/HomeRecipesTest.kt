package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estado vacio del bloque "Tus recetas" (bloque 5 de la Home) y regla pura del tope
 * [HomeRecipes.latest]/[HomeRecipes.MAX_VISIBLE]: el bloque resume como mucho las 6 ultimas guardadas,
 * sin reordenar (la lista ya llega mas reciente primero desde los Favoritos) y sin mutar el original.
 * Logica pura, sin Android ni Compose.
 */
class HomeRecipesTest {

    @Test
    fun sin_guardadas_el_bloque_esta_vacio() {
        assertTrue(HomeRecipes().isEmpty)
        assertTrue(HomeRecipes(saved = emptyList()).isEmpty)
        assertTrue("sin guardadas el tope se queda vacio", HomeRecipes().latest().isEmpty)
    }

    @Test
    fun con_una_receta_guardada_el_bloque_no_esta_vacio() {
        val bloque = HomeRecipes(saved = listOf(receta("seed-1")))

        assertFalse(bloque.isEmpty)
    }

    @Test
    fun max_visible_es_6() {
        assertEquals(6, HomeRecipes.MAX_VISIBLE)
    }

    @Test
    fun latest_con_mas_de_6_guardadas_devuelve_las_6_primeras_en_orden() {
        val bloque = HomeRecipes(saved = (1..8).map { receta("s$it") })

        val ultimas = bloque.latest()

        assertEquals(
            "el tope no reordena: conserva las 6 primeras del orden de entrada",
            listOf("s1", "s2", "s3", "s4", "s5", "s6"),
            ultimas.saved.map { it.id },
        )
    }

    @Test
    fun latest_con_menos_de_6_guardadas_no_recorta() {
        val bloque = HomeRecipes(saved = listOf(receta("s1"), receta("s2"), receta("s3")))

        assertEquals(listOf("s1", "s2", "s3"), bloque.latest().saved.map { it.id })
    }

    @Test
    fun latest_con_6_exactas_no_recorta() {
        val bloque = HomeRecipes(saved = (1..6).map { receta("s$it") })

        assertEquals(6, bloque.latest().saved.size)
        assertFalse(bloque.latest().isEmpty)
    }

    @Test
    fun latest_no_altera_el_bloque_original() {
        val bloque = HomeRecipes(saved = (1..8).map { receta("s$it") })

        val ultimas = bloque.latest()

        assertEquals("latest() no muta el original", 8, bloque.saved.size)
        assertEquals(
            "isEmpty sigue siendo saved.isEmpty() despues del tope",
            bloque.isEmpty,
            bloque.saved.isEmpty(),
        )
        assertFalse(ultimas.isEmpty)
    }
}

private fun receta(
    id: String,
    categoryId: String = "other",
    totalMinutes: Int = 20,
) = Recipe(
    id = id,
    source = RecipeSource.SEED,
    title = "Receta $id",
    description = "",
    categoryId = categoryId,
    mealTypeId = "lunch",
    cuisineId = "spanish",
    totalMinutes = totalMinutes,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = emptyList(),
)
