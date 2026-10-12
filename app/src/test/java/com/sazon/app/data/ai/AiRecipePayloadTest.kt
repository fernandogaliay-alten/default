package com.sazon.app.data.ai

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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decode tolerante de `AiRecipePayload.decode`: contrato valido, elemento roto dentro de un lote
 * legible, valla markdown y respuestas ilegibles/vacias que **no** lanzan. Ademas comprueba que el
 * proveedor **no** pre-filtra por dieta/restricciones (restricciones neutras). Taxonomia real.
 */
class AiRecipePayloadTest {

    private val taxonomy = taxonomy()

    @Test
    fun un_contrato_valido_devuelve_las_recetas() {
        val recipes = AiRecipePayload.decode(envelope(recipeJson(arroz())), taxonomy)

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
    }

    @Test
    fun una_receta_rota_dentro_de_un_lote_legible_no_tumba_el_resto() {
        val recipes = AiRecipePayload.decode(
            envelope(recipeJson(arroz()), """{"title":"Rota"}"""),
            taxonomy,
        )

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
    }

    @Test
    fun una_valla_markdown_envolvente_se_quita() {
        val fenced = "```json\n${envelope(recipeJson(arroz()))}\n```"

        val recipes = AiRecipePayload.decode(fenced, taxonomy)

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
    }

    @Test
    fun las_respuestas_ilegibles_o_vacias_devuelven_lista_vacia_sin_lanzar() {
        val cases = listOf(
            "lo siento, no puedo ayudarte",
            "",
            "   ",
            "{\"foo\":1}",
            "[1,2]",
            "no es json",
        )

        cases.forEach { content ->
            val recipes = AiRecipePayload.decode(content, taxonomy)
            assertTrue("El contenido '$content' debe dar lista vacia", recipes.isEmpty())
        }
        assertTrue("null debe dar lista vacia", AiRecipePayload.decode(null, taxonomy).isEmpty())
    }

    @Test
    fun el_proveedor_no_prefiltra_por_dieta_ni_restricciones_duras() {
        // Con restricciones neutras, el pollo (tag poultry) y el "Pan" libre (gluten) llegan tal cual:
        // el filtrado por Perfil es del llamador (asi el estado Bloqueado de ai-search sigue vivo).
        val recipes = AiRecipePayload.decode(
            envelope(recipeJson(arroz()), recipeJson(polloAlAjillo()), recipeJson(bocadilloTomate())),
            taxonomy,
        )

        assertEquals(3, recipes.size)
        assertTrue(recipes.any { it.title == "Pollo al ajillo" })
        assertTrue(recipes.any { it.title == "Bocadillo de tomate" })
    }

    private fun envelope(vararg recipeJson: String): String =
        """{"recipes":[${recipeJson.joinToString(",")}]}"""

    private fun recipeJson(recipe: Recipe): String = RecipeJson.encode(recipe)
}

private fun arroz(): Recipe = Recipe(
    id = "test-arroz",
    source = RecipeSource.AI,
    title = "Arroz salteado con verduras",
    description = "Un arroz rapido con verduras.",
    categoryId = "rice",
    mealTypeId = "lunch",
    cuisineId = "asian",
    totalMinutes = 25,
    prepMinutes = 10,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz"),
        RecipeIngredient("Zanahoria", 1.0, MeasureUnit.UNIT, ingredientId = "zanahoria"),
    ),
    steps = listOf(
        RecipeStep(1, "Cocer el arroz", "Cuece el arroz.", 12),
        RecipeStep(2, "Saltear", "Saltea las verduras.", 13),
    ),
    declaredAllergens = emptyList(),
    matchScore = null,
)

private fun polloAlAjillo(): Recipe = Recipe(
    id = "test-pollo-ajillo",
    source = RecipeSource.AI,
    title = "Pollo al ajillo",
    description = "Pollo dorado con ajo y perejil.",
    categoryId = "poultry",
    mealTypeId = "dinner",
    cuisineId = "spanish",
    totalMinutes = 35,
    prepMinutes = 10,
    cookMinutes = 25,
    difficultyId = "medium",
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Pollo", 500.0, MeasureUnit.GRAM, ingredientId = "pollo"),
        RecipeIngredient("Ajo", 4.0, MeasureUnit.UNIT, ingredientId = "ajo"),
    ),
    steps = listOf(
        RecipeStep(1, "Dorar el pollo", "Dora el pollo por todos lados.", 15),
        RecipeStep(2, "Anadir el ajo", "Incorpora el ajo laminado.", 20),
    ),
    declaredAllergens = emptyList(),
    matchScore = null,
)

private fun bocadilloTomate(): Recipe = Recipe(
    id = "test-bocadillo",
    source = RecipeSource.AI,
    title = "Bocadillo de tomate",
    description = "Un bocadillo sencillo de tomate y aceite.",
    categoryId = "sandwich_wrap",
    mealTypeId = "breakfast",
    cuisineId = "spanish",
    totalMinutes = 5,
    prepMinutes = 5,
    cookMinutes = 0,
    difficultyId = "easy",
    servings = 1,
    ingredients = listOf(
        // Ingrediente libre "Pan": activa gluten por keywords si el llamador tiene esa restriccion.
        RecipeIngredient("Pan", 1.0, unit = null, ingredientId = null),
        RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"),
    ),
    steps = listOf(RecipeStep(1, "Montar el bocadillo", "Unta el tomate y aliña.", 5)),
    declaredAllergens = listOf("gluten"),
    matchScore = null,
)

private fun taxonomy(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
