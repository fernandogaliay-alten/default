package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Servings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Estado del Detalle de receta (`docs/specs/recipe-detail-screen.md`, escenarios 5 y 6): arranque en
 * las raciones de la receta acotadas a `1..12`, recorte del stepper, cambio de pestana unico y
 * operaciones inmutables. Logica pura sin Compose ni Android.
 */
class RecipeDetailStateTest {

    private val limites = Servings(min = 1, max = 12, default = 2)

    @Test
    fun arranca_en_las_raciones_de_la_receta_y_en_ingredientes() {
        val state = RecipeDetailState.initial(receta(servings = 4), limites)

        assertEquals(4, state.servings)
        assertEquals(RecipeDetailTab.INGREDIENTS, state.selectedTab)
    }

    @Test
    fun arranca_recortando_las_raciones_fuera_de_rango() {
        assertEquals(12, RecipeDetailState.initial(receta(servings = 99), limites).servings)
        assertEquals(1, RecipeDetailState.initial(receta(servings = 0), limites).servings)
    }

    @Test
    fun set_servings_recorta_al_rango_de_la_taxonomia() {
        val state = RecipeDetailState.initial(receta(servings = 4), limites)

        assertEquals(12, state.setServings(50, limites).servings)
        assertEquals(1, state.setServings(-3, limites).servings)
        assertEquals(7, state.setServings(7, limites).servings)
    }

    @Test
    fun select_tab_reemplaza_la_pestana_activa_y_es_unica() {
        val state = RecipeDetailState.initial(receta(servings = 4), limites)

        val pasos = state.selectTab(RecipeDetailTab.STEPS)

        assertEquals(RecipeDetailTab.STEPS, pasos.selectedTab)
        val notas = pasos.selectTab(RecipeDetailTab.NOTES)
        assertEquals(RecipeDetailTab.NOTES, notas.selectedTab)
        assertNotEquals(RecipeDetailTab.STEPS, notas.selectedTab)
    }

    @Test
    fun las_operaciones_no_mutan_el_estado_original() {
        val state = RecipeDetailState.initial(receta(servings = 4), limites)

        val cambiado = state.setServings(2, limites).selectTab(RecipeDetailTab.NOTES)

        assertEquals(4, state.servings)
        assertEquals(RecipeDetailTab.INGREDIENTS, state.selectedTab)
        assertNotEquals(state, cambiado)
    }

    private fun receta(servings: Int): Recipe = Recipe(
        id = "receta-1",
        source = RecipeSource.SEED,
        title = "Receta de prueba",
        description = "",
        categoryId = "other",
        mealTypeId = "lunch",
        cuisineId = "spanish",
        totalMinutes = 20,
        prepMinutes = 5,
        cookMinutes = 15,
        difficultyId = "easy",
        servings = servings,
        ingredients = emptyList(),
        steps = emptyList(),
    )
}
