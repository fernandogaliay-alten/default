package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 3 de `docs/specs/ai-recipe-decode.md`: la politica de ids de `AiRecipeIds` es
 * **determinista** (mismo contenido -> mismo id), **estable** e **independiente del orden** de la
 * respuesta, con un id unico dentro de la llamada aun con contenido duplicado. Sin `Random`, reloj ni
 * `UUID`; test JVM puro (no necesita Taxonomia).
 */
class AiRecipeIdsTest {

    @Test
    fun el_mismo_contenido_produce_el_mismo_id() {
        val first = AiRecipeIds.idFor(draft(), mutableSetOf())
        val second = AiRecipeIds.idFor(draft(), mutableSetOf())

        assertEquals(first, second)
    }

    @Test
    fun contenido_distinto_produce_id_distinto() {
        val base = AiRecipeIds.idFor(draft(), mutableSetOf())
        val changedByTitle = AiRecipeIds.idFor(draft(title = "Otra receta"), mutableSetOf())
        val changedByStepMinutes = AiRecipeIds.idFor(
            draft(steps = listOf(RecipeStep(1, "Cocer el arroz", "Cuece el arroz.", 13))),
            mutableSetOf(),
        )
        val changedByTips = AiRecipeIds.idFor(draft(tips = listOf("Un truco extra.")), mutableSetOf())
        val changedByNutrition = AiRecipeIds.idFor(
            draft(nutrition = Nutrition(kcal = 400, protein = 8, carbs = 70, fat = 7)),
            mutableSetOf(),
        )

        listOf(changedByTitle, changedByStepMinutes, changedByTips, changedByNutrition).forEach { id ->
            assertNotEquals(base, id)
        }
    }

    @Test
    fun el_id_tiene_prefijo_ai_y_un_hash_hex_de_64_caracteres() {
        val id = AiRecipeIds.idFor(draft(), mutableSetOf())

        assertTrue("Prefijo ai-", id.startsWith("ai-"))
        val hash = id.removePrefix("ai-")
        assertEquals(64, hash.length)
        assertTrue("Hex minuscula", hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun dos_drafts_identicos_con_used_compartido_se_desambiguan_con_sufijo() {
        val used = mutableSetOf<String>()

        val first = AiRecipeIds.idFor(draft(), used)
        val second = AiRecipeIds.idFor(draft(), used)
        val third = AiRecipeIds.idFor(draft(), used)

        assertTrue("El primero es el hash base", first.startsWith("ai-"))
        assertEquals("$first-2", second)
        assertEquals("$first-3", third)
        assertEquals("Tres ids distintos", 3, used.size)
    }

    @Test
    fun el_id_de_un_draft_no_depende_del_orden_de_la_respuesta() {
        val ownId = AiRecipeIds.idFor(draft(), mutableSetOf())
        val anotherFirst = mutableSetOf(AiRecipeIds.idFor(draft(title = "Otra receta"), mutableSetOf()))

        assertEquals(ownId, AiRecipeIds.idFor(draft(), anotherFirst))
    }

    @Test
    fun el_orden_de_declared_allergens_no_cambia_el_id() {
        val sorted = AiRecipeIds.idFor(draft(declaredAllergens = listOf("gluten", "lactosa")), mutableSetOf())
        val reversed = AiRecipeIds.idFor(draft(declaredAllergens = listOf("lactosa", "gluten")), mutableSetOf())

        assertEquals(sorted, reversed)
    }
}

private fun draft(
    title: String = "Arroz salteado con verduras",
    ingredients: List<RecipeIngredient> = listOf(
        RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz"),
        RecipeIngredient("Zanahoria", 1.0, MeasureUnit.UNIT, ingredientId = "zanahoria"),
    ),
    steps: List<RecipeStep> = listOf(
        RecipeStep(1, "Cocer el arroz", "Cuece el arroz.", 12),
        RecipeStep(2, "Saltear", "Saltea las verduras.", 13),
    ),
    tips: List<String> = emptyList(),
    nutrition: Nutrition? = null,
    declaredAllergens: List<String> = emptyList(),
): AiRecipeDraft = AiRecipeDraft(
    title = title,
    description = "Un arroz rapido con verduras.",
    categoryId = "rice",
    mealTypeId = "lunch",
    cuisineId = "asian",
    totalMinutes = 25,
    prepMinutes = 10,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = ingredients,
    steps = steps,
    tips = tips,
    nutrition = nutrition,
    declaredAllergens = declaredAllergens,
)
