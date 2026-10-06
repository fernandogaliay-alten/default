package com.sazon.app.domain.validation

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeJson
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeValidatorTest {

    private val taxonomy: Taxonomy = taxonomiaReal()
    private val sinRestricciones = RecipeConstraints(
        dietPatternId = "omnivore",
        hardRestrictionIds = emptySet(),
    )

    @Test
    fun escenario_1_respuesta_segura_y_coherente_se_acepta() {
        val outcome = RecipeValidator.validateResponse(
            respuesta(recetaValida()),
            taxonomy,
            sinRestricciones,
        )

        assertTrue("Se esperaba ValidationCompleted: $outcome", outcome is ValidationOutcome.ValidationCompleted)
        val completado = outcome as ValidationOutcome.ValidationCompleted
        assertEquals(1, completado.accepted.size)
        assertTrue("No deberia haber descartes: ${completado.discarded}", completado.discarded.isEmpty())
        assertEquals(recetaValida(), completado.accepted.first())
    }

    @Test
    fun escenario_2_respuesta_malformada_no_lanza() {
        val noJson = RecipeValidator.validateResponse("esto no es json", taxonomy, sinRestricciones)
        assertTrue(noJson is ValidationOutcome.Malformed)

        val sinRecipes = RecipeValidator.validateResponse("{}", taxonomy, sinRestricciones)
        assertTrue(sinRecipes is ValidationOutcome.Malformed)

        val recipesNoArray = RecipeValidator.validateResponse("""{"recipes": 3}""", taxonomy, sinRestricciones)
        assertTrue(recipesNoArray is ValidationOutcome.Malformed)
    }

    @Test
    fun escenario_3_id_de_clasificacion_desconocido_descarta() {
        val resultado = RecipeValidator.validateRecipe(
            recetaValida().copy(difficultyId = "imposible"),
            taxonomy,
            sinRestricciones,
        )

        val descarte = requiereDescarte(resultado)
        assertEquals(DiscardReason.SCHEMA, descarte.reason)
        assertTrue(descarte.detail.contains("difficultyId"))
        assertTrue(descarte.detail.contains("imposible"))
    }

    @Test
    fun escenario_4_category_id_desconocido_usa_el_fallback() {
        val resultado = RecipeValidator.validateRecipe(
            recetaValida().copy(categoryId = "no-existe"),
            taxonomy,
            sinRestricciones,
        )

        val receta = requiereAceptada(resultado)
        assertEquals("other", receta.categoryId)
    }

    @Test
    fun escenario_5_dieta_incumplida_se_descarta() {
        val constraints = RecipeConstraints("vegetarian", emptySet())
        val receta = recetaValida().copy(
            ingredients = listOf(RecipeIngredient("Pollo", 200.0, MeasureUnit.GRAM, ingredientId = "pollo")),
        )

        val descarte = requiereDescarte(RecipeValidator.validateRecipe(receta, taxonomy, constraints))

        assertEquals(DiscardReason.DIET_PATTERN, descarte.reason)
        assertTrue(descarte.detail.contains("Pollo"))
        assertTrue(descarte.detail.contains("poultry"))
    }

    @Test
    fun escenario_6_alergeno_por_keywords_en_ingrediente_libre() {
        val constraints = RecipeConstraints("omnivore", setOf("gluten"))
        val receta = recetaValida().copy(
            ingredients = listOf(RecipeIngredient("pan", 2.0, MeasureUnit.UNIT, ingredientId = null)),
        )

        val descarte = requiereDescarte(RecipeValidator.validateRecipe(receta, taxonomy, constraints))

        assertEquals(DiscardReason.HARD_RESTRICTION, descarte.reason)
        assertTrue(descarte.detail.contains("gluten"))
        assertTrue(descarte.detail.contains("pan"))
    }

    @Test
    fun escenario_7_alergeno_por_ingrediente_de_la_taxonomia() {
        val constraints = RecipeConstraints("omnivore", setOf("gluten"))
        val receta = recetaValida().copy(
            ingredients = listOf(RecipeIngredient("Espaguetis", 200.0, MeasureUnit.GRAM, ingredientId = "pasta")),
        )

        val descarte = requiereDescarte(RecipeValidator.validateRecipe(receta, taxonomy, constraints))

        assertEquals(DiscardReason.HARD_RESTRICTION, descarte.reason)
        assertTrue(descarte.detail.contains("gluten"))
        assertTrue(descarte.detail.contains("Espaguetis"))
    }

    @Test
    fun escenario_8_normalizacion_de_acentos_y_mayusculas() {
        val constraints = RecipeConstraints("omnivore", setOf("sesame"))
        val receta = recetaValida().copy(
            ingredients = listOf(RecipeIngredient("SÉSAMO tostado", 5.0, MeasureUnit.GRAM, ingredientId = null)),
        )

        val descarte = requiereDescarte(RecipeValidator.validateRecipe(receta, taxonomy, constraints))

        assertEquals(DiscardReason.HARD_RESTRICTION, descarte.reason)
        assertTrue(descarte.detail.contains("sesame"))
    }

    @Test
    fun escenario_9_incoherencia_de_tiempos() {
        val receta = recetaValida().copy(
            totalMinutes = 25,
            steps = listOf(RecipeStep(1, "Mezclar", "Mezcla todo.", 10)),
        )

        val descarte = requiereDescarte(RecipeValidator.validateRecipe(receta, taxonomy, sinRestricciones))

        assertEquals(DiscardReason.INCOHERENT, descarte.reason)
        assertTrue(descarte.detail.contains("10"))
        assertTrue(descarte.detail.contains("25"))
    }

    @Test
    fun escenario_10_alergeno_omitido_en_declared_allergens() {
        val receta = recetaValida().copy(
            ingredients = listOf(RecipeIngredient("Leche", 100.0, MeasureUnit.MILLILITER, ingredientId = "leche")),
            declaredAllergens = emptyList(),
        )

        val descarte = requiereDescarte(RecipeValidator.validateRecipe(receta, taxonomy, sinRestricciones))

        assertEquals(DiscardReason.INCOHERENT, descarte.reason)
        assertTrue(descarte.detail.contains("lactose"))
    }

    @Test
    fun escenario_11_una_receta_invalida_no_tumba_el_lote() {
        val rota = """{"id":"rota","source":"ai"}"""
        val raw = """{"recipes":[${RecipeJson.encode(recetaValida())},$rota]}"""

        val outcome = RecipeValidator.validateResponse(raw, taxonomy, sinRestricciones)

        assertTrue(outcome is ValidationOutcome.ValidationCompleted)
        val completado = outcome as ValidationOutcome.ValidationCompleted
        assertEquals(1, completado.accepted.size)
        assertEquals(1, completado.discarded.size)
        assertEquals(DiscardReason.SCHEMA, completado.discarded.first().reason)
    }

    @Test
    fun un_ingrediente_con_id_desconocido_se_degrada_a_libre() {
        val receta = recetaValida().copy(
            ingredients = listOf(
                RecipeIngredient("Zanahoria", 1.0, MeasureUnit.UNIT, ingredientId = "no-existe"),
            ),
        )

        val aceptada = requiereAceptada(RecipeValidator.validateRecipe(receta, taxonomy, sinRestricciones))

        assertNull(aceptada.ingredients.first().ingredientId)
    }

    private fun requiereDescarte(resultado: RecipeValidation): RecipeValidation.Discarded {
        assertTrue("Se esperaba un descarte: $resultado", resultado is RecipeValidation.Discarded)
        return resultado as RecipeValidation.Discarded
    }

    private fun requiereAceptada(resultado: RecipeValidation): Recipe {
        assertTrue("Se esperaba aceptacion: $resultado", resultado is RecipeValidation.Accepted)
        return (resultado as RecipeValidation.Accepted).recipe
    }
}

private fun respuesta(vararg recetas: Recipe): String =
    """{"recipes":[${recetas.joinToString(",") { RecipeJson.encode(it) }}]}"""

private fun recetaValida() = Recipe(
    id = "receta-001",
    source = RecipeSource.AI,
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
    ),
    steps = listOf(
        RecipeStep(1, "Cocinar la pasta", "Cuece la pasta en agua con sal.", 12),
        RecipeStep(2, "Preparar la salsa", "Sofrie el tomate y mezcla.", 13),
    ),
    tips = listOf("Reserva agua de coccion para ligar la salsa."),
    declaredAllergens = emptyList(),
)

private fun taxonomiaReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
