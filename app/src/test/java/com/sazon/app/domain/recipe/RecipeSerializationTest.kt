package com.sazon.app.domain.recipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSerializationTest {

    @Test
    fun una_receta_completa_va_y_vuelve_sin_perdida() {
        val original = recetaCompleta()

        val json = RecipeJson.encode(original)
        val recuperada = RecipeJson.parse(json)

        assertEquals(original, recuperada)
        assertEquals(2, recuperada.ingredients.count { it.ingredientId == null })
        assertEquals(1, recuperada.ingredients.count { it.isPantry })
    }

    @Test
    fun una_receta_sin_nutricion_deja_los_opcionales_en_null() {
        val original = recetaSinNutricion()

        val json = RecipeJson.encode(original)
        val recuperada = RecipeJson.parse(json)

        assertEquals(original, recuperada)
        assertNull(recuperada.nutrition)
        assertNull(recuperada.matchScore)
    }

    @Test
    fun el_origen_se_conserva_distinguido_en_el_json() {
        val seed = recetaCompleta().copy(source = RecipeSource.SEED)
        val ai = recetaCompleta().copy(source = RecipeSource.AI)

        val jsonSeed = RecipeJson.encode(seed)
        val jsonAi = RecipeJson.encode(ai)

        assertTrue("JSON sin 'seed': $jsonSeed", jsonSeed.contains("\"source\":\"seed\""))
        assertTrue("JSON sin 'ai': $jsonAi", jsonAi.contains("\"source\":\"ai\""))
        assertEquals(RecipeSource.SEED, RecipeJson.parse(jsonSeed).source)
        assertEquals(RecipeSource.AI, RecipeJson.parse(jsonAi).source)
    }

    @Test
    fun la_unidad_es_un_valor_cerrado_y_admite_ausencia() {
        val conUnidad = recetaCompleta().copy(
            ingredients = listOf(
                RecipeIngredient("Tomate", 200.0, MeasureUnit.GRAM, ingredientId = "tomate"),
                RecipeIngredient("Albahaca fresca", 1.0, unit = null, ingredientId = null),
            ),
        )

        val json = RecipeJson.encode(conUnidad)
        val recuperada = RecipeJson.parse(json)

        assertTrue(json.contains("\"unit\":\"g\""))
        assertEquals(MeasureUnit.GRAM, recuperada.ingredients[0].unit)
        assertNull(recuperada.ingredients[1].unit)
        assertEquals(conUnidad, recuperada)
    }
}

private fun recetaCompleta() = Recipe(
    id = "receta-001",
    source = RecipeSource.SEED,
    title = "Pasta al tomate",
    description = "Una receta sencilla de pasta con tomate.",
    categoryId = "pasta",
    mealTypeId = "lunch",
    cuisineId = "italian",
    totalMinutes = 25,
    prepMinutes = 10,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Tomate", 200.0, MeasureUnit.GRAM, ingredientId = "tomate"),
        RecipeIngredient("Albahaca fresca", 1.0, MeasureUnit.UNIT, ingredientId = null),
        RecipeIngredient("Sal", 1.0, MeasureUnit.TEASPOON, isPantry = true, ingredientId = null),
    ),
    steps = listOf(
        RecipeStep(1, "Cocinar la pasta", "Cuece la pasta en agua con sal.", 12),
        RecipeStep(2, "Preparar la salsa", "Sofríe el tomate y mezcla.", 13),
    ),
    tips = listOf("Reserva agua de cocción para ligar la salsa."),
    nutrition = Nutrition(kcal = 420, protein = 25, carbs = 30, fat = 18),
    declaredAllergens = listOf("gluten"),
    matchScore = 92,
)

private fun recetaSinNutricion() = Recipe(
    id = "receta-002",
    source = RecipeSource.AI,
    title = "Ensalada rápida",
    description = "Una ensalada de lo que tengas.",
    categoryId = "salads",
    mealTypeId = "dinner",
    cuisineId = "mediterranean",
    totalMinutes = 10,
    prepMinutes = 10,
    cookMinutes = 0,
    difficultyId = "easy",
    servings = 1,
    ingredients = listOf(
        RecipeIngredient("Lechuga", 100.0, MeasureUnit.GRAM, ingredientId = null),
    ),
    steps = listOf(
        RecipeStep(1, "Montar", "Lava y mezcla los ingredientes.", 10),
    ),
)
