package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.recipe.Nutrition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escalado de raciones (`docs/specs/recipe-detail-screen.md`, escenarios 1-4: verificacion exigida).
 * Logica pura sin Compose ni Android sobre la receta semilla `seed-tortilla-patatas`.
 */
class ServingScalerTest {

    private val tortilla: Recipe = Recipe(
        id = "seed-tortilla-patatas",
        source = RecipeSource.SEED,
        title = "Tortilla de patatas",
        description = "Tortilla clásica de patata y huevo, jugosa por dentro.",
        categoryId = "egg_breakfast",
        mealTypeId = "dinner",
        cuisineId = "spanish",
        totalMinutes = 40,
        prepMinutes = 15,
        cookMinutes = 25,
        difficultyId = "easy",
        servings = 4,
        ingredients = listOf(
            ingrediente("Patata", 600.0, MeasureUnit.GRAM),
            ingrediente("Huevo", 6.0, MeasureUnit.UNIT),
            ingrediente("Cebolla", 1.0, MeasureUnit.UNIT),
            ingrediente("Aceite de oliva", 150.0, MeasureUnit.MILLILITER),
            ingrediente("Sal", 1.0, null),
        ),
        steps = listOf(
            RecipeStep(1, "Pelar y cortar", "Pela las patatas.", 15),
            RecipeStep(2, "Freír", "Fríe la patata.", 15),
            RecipeStep(3, "Cuajar", "Cuaja la tortilla.", 10),
        ),
        nutrition = Nutrition(kcal = 320, protein = 14, carbs = 24, fat = 19),
    )

    @Test
    fun a_dos_comensales_escala_y_redondea_por_unidad() {
        val scaled = ServingScaler.scaledIngredients(tortilla, targetServings = 2)

        assertQuantity(scaled, "Patata", 300.0)
        assertQuantity(scaled, "Huevo", 3.0)
        assertQuantity(scaled, "Cebolla", 0.5)
        assertQuantity(scaled, "Aceite de oliva", 75.0)
    }

    @Test
    fun a_tres_comensales_aplica_el_redondeo_no_lineal() {
        val scaled = ServingScaler.scaledIngredients(tortilla, targetServings = 3)

        assertQuantity(scaled, "Patata", 450.0)
        assertQuantity(scaled, "Huevo", 4.5)
        assertQuantity(scaled, "Cebolla", 1.0)
        assertQuantity(scaled, "Aceite de oliva", 115.0)
    }

    @Test
    fun a_las_raciones_originales_las_cantidades_quedan_intactas() {
        val receta = tortilla.copy(
            ingredients = listOf(
                ingrediente("Patata", 600.0, MeasureUnit.GRAM),
                ingrediente("Cebolla", 0.5, MeasureUnit.UNIT),
            ),
        )

        val scaled = ServingScaler.scaledIngredients(receta, targetServings = receta.servings)

        assertEquals(receta.ingredients, scaled)
        assertQuantity(scaled, "Cebolla", 0.5)
    }

    @Test
    fun la_unidad_al_gusto_no_se_escala() {
        val scaled = ServingScaler.scaledIngredients(tortilla, targetServings = 1)

        assertQuantity(scaled, "Sal", 1.0)
        assertNull(scaled.first { it.name == "Sal" }.unit)
    }

    @Test
    fun una_receta_sin_raciones_no_divide_por_cero() {
        val sinRaciones = tortilla.copy(servings = 0)

        val scaled = ServingScaler.scaledIngredients(sinRaciones, targetServings = 2)

        // recipe.servings = 0 -> factor = 2 / coerceAtLeast(1) = 2 (no divide por cero).
        assertQuantity(scaled, "Patata", 1200.0)
        assertQuantity(scaled, "Sal", 1.0)
        assertQuantity(scaled, "Huevo", 12.0)
    }

    @Test
    fun una_cantidad_cero_se_conserva_cero() {
        assertEquals(
            0.0,
            ServingScaler.scaleQuantity(0.0, MeasureUnit.GRAM, factor = 0.5),
            1e-9,
        )
    }

    @Test
    fun el_resultado_es_determinista() {
        val primera = ServingScaler.scaledIngredients(tortilla, targetServings = 3)
        val segunda = ServingScaler.scaledIngredients(tortilla, targetServings = 3)

        assertEquals(primera, segunda)
    }

    @Test
    fun una_cantidad_positiva_nunca_baja_del_paso_de_su_unidad() {
        // 3 g * 0.5 = 1.5 -> round a 0 con paso 5; se eleva al minimo (5 g).
        assertEquals(5.0, ServingScaler.scaleQuantity(3.0, MeasureUnit.GRAM, factor = 0.5), 1e-9)
        // 1 ud * 0.1 = 0.1 -> round a 0 con paso 0.5; se eleva al minimo (0.5 ud).
        assertEquals(0.5, ServingScaler.scaleQuantity(1.0, MeasureUnit.UNIT, factor = 0.1), 1e-9)
    }

    private fun ingrediente(name: String, quantity: Double, unit: MeasureUnit?) =
        RecipeIngredient(name = name, quantity = quantity, unit = unit)

    private fun assertQuantity(
        scaled: List<RecipeIngredient>,
        name: String,
        expected: Double,
    ) {
        val actual = scaled.first { it.name == name }.quantity
        assertEquals("Cantidad de $name", expected, actual, 1e-9)
    }
}
