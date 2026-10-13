package com.sazon.app.ui.artwork

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El default del seam ([NoGeneratedRecipeArtworkSource]) **no** produce imagen generada. Es la
 * degradacion segura de previews, tests y hosts sin proveedor: con este default [RecipeArtwork] pinta
 * exactamente lo de hoy (foto local o degradado + glifo), sin tocar red ni cache.
 */
class GeneratedRecipeArtworkSourceTest {

    @Test
    fun el_default_no_produce_imagen_generada() = runBlocking {
        assertNull(NoGeneratedRecipeArtworkSource.generatedFor(receta("Arroz con pollo criollo")))
        assertNull(NoGeneratedRecipeArtworkSource.generatedFor(receta("")))
    }
}

private fun receta(title: String) = Recipe(
    id = "receta-de-prueba",
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
