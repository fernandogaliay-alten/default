package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decode tolerante de `AiRecipePayload.decode` con la **forma real** del contrato de la IA
 * (`docs/domain/taxonomy.md` §4): JSON **literal sin `id` ni `source`** (los posee la app).
 *
 * Es el punto ciego que ocultaba el bug de #70: los fixtures se fabricaban con
 * `RecipeJson.encode(recipe)`, que **si** incluye `id` y `source` y por eso no reproducia lo que el
 * modelo envia. Aqui los cuerpos son literales del contrato (nunca `RecipeJson.encode`).
 *
 * Taxonomia real. El proveedor **no** pre-filtra por dieta/restricciones (restricciones neutras).
 */
class AiRecipePayloadTest {

    private val taxonomy = taxonomy()

    // Escenario 1: forma real del contrato -> 1 receta valida, con source/id completados.
    @Test
    fun un_contrato_valido_devuelve_las_recetas() {
        val recipes = AiRecipePayload.decode(envelope(arrozDraft()), taxonomy)

        assertEquals(1, recipes.size)
        val recipe = recipes.single()
        assertEquals("Arroz salteado con verduras", recipe.title)
        assertEquals("Un arroz rapido con verduras.", recipe.description)
        assertEquals("rice", recipe.categoryId)
        assertEquals("lunch", recipe.mealTypeId)
        assertEquals("asian", recipe.cuisineId)
        assertEquals(25, recipe.totalMinutes)
        assertEquals(2, recipe.servings)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(2, recipe.steps.size)
        assertEquals(RecipeSource.AI, recipe.source)
        assertTrue("El id lo completa la app", recipe.id.startsWith(AiRecipeIds.PREFIX))
    }

    @Test
    fun una_receta_rota_dentro_de_un_lote_legible_no_tumba_el_resto() {
        val recipes = AiRecipePayload.decode(
            envelope(arrozDraft(), """{"title":"Rota"}"""),
            taxonomy,
        )

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
    }

    @Test
    fun una_valla_markdown_envolvente_se_quita() {
        val fenced = "```json\n${envelope(arrozDraft())}\n```"

        val recipes = AiRecipePayload.decode(fenced, taxonomy)

        assertEquals(1, recipes.size)
        assertEquals("Arroz salteado con verduras", recipes.single().title)
    }

    // Escenario 5: tolerancia preservada (nunca lanza ni reintenta).
    @Test
    fun las_respuestas_ilegibles_o_vacias_devuelven_lista_vacia_sin_lanzar() {
        val cases = listOf(
            "lo siento, no puedo ayudarte",
            "",
            "   ",
            "{\"foo\":1}",
            "[1,2]",
            "{\"recipes\":3}",
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
            envelope(arrozDraft(), polloDraft(), bocadilloDraft()),
            taxonomy,
        )

        assertEquals(3, recipes.size)
        assertTrue(recipes.any { it.title == "Pollo al ajillo" })
        assertTrue(recipes.any { it.title == "Bocadillo de tomate" })
    }

    // Escenario 2: id unico en la llamada y source = ai, incluso con una receta repetida literal.
    @Test
    fun cada_receta_recibe_un_id_unico_y_source_ai_con_un_duplicado_literal() {
        val recipes = AiRecipePayload.decode(
            envelope(arrozDraft(), polloDraft(), arrozDraft()),
            taxonomy,
        )

        assertEquals(3, recipes.size)
        assertTrue("Todas son ai", recipes.all { it.source == RecipeSource.AI })
        assertTrue("Todas con id no vacio y prefijo ai-", recipes.all { it.id.startsWith(AiRecipeIds.PREFIX) })
        assertEquals("No puede haber dos ids iguales", 3, recipes.map { it.id }.toSet().size)
        assertTrue(
            "El duplicado literal se desambigua",
            recipes.map { it.id }.any { it.endsWith("-2") },
        )
    }

    // Escenario 4: las reglas del Validador siguen aplicandose y el elemento roto no tumba el resto.
    @Test
    fun las_reglas_del_validador_siguen_aplicandose_y_la_valida_sobrevive() {
        val recipes = AiRecipePayload.decode(
            envelope(
                draftJson(title = "Comida inexistente", mealTypeId = "second_breakfast"),
                draftJson(title = "Cocina inexistente", cuisineId = "martian"),
                draftJson(title = "Dificultad inexistente", difficultyId = "impossible"),
                // Los pasos del contrato suman 25 min: totalMinutes 60 deja una deriva > 5 min.
                draftJson(title = "Tiempos incoherentes", totalMinutes = 60, prepMinutes = 20, cookMinutes = 40),
                draftJson(title = "Receta valida"),
            ),
            taxonomy,
        )

        assertEquals(1, recipes.size)
        assertEquals("Receta valida", recipes.single().title)
    }

    // Escenario 6: matchScore lo sigue derivando la app (el del modelo se ignora).
    @Test
    fun el_matchscore_del_contrato_se_ignora_y_la_receta_sale_sin_score() {
        val recipes = AiRecipePayload.decode(envelope(draftJson(matchScore = 92)), taxonomy)

        assertEquals(1, recipes.size)
        assertNull("El score es derivado y no persistido", recipes.single().matchScore)
    }

    private fun envelope(vararg draftJson: String): String =
        """{"recipes":[${draftJson.joinToString(",")}]}"""

    private fun taxonomy(): Taxonomy {
        val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
            "Falta la propiedad de sistema taxonomy.asset.path"
        }
        return TaxonomyJson.parse(File(path).readText())
    }
}

private const val ARROZ_INGREDIENTS =
    """[{"name":"Arroz","quantity":200,"unit":"g","isPantry":false,"ingredientId":"arroz"},{"name":"Zanahoria","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"zanahoria"}]"""
private const val ARROZ_STEPS =
    """[{"order":1,"title":"Cocer el arroz","description":"Cuece el arroz.","minutes":12},{"order":2,"title":"Saltear","description":"Saltea las verduras.","minutes":13}]"""
private const val POLLO_INGREDIENTS =
    """[{"name":"Pollo","quantity":500,"unit":"g","isPantry":false,"ingredientId":"pollo"},{"name":"Ajo","quantity":4,"unit":"ud","isPantry":false,"ingredientId":"ajo"}]"""
private const val POLLO_STEPS =
    """[{"order":1,"title":"Dorar el pollo","description":"Dora el pollo por todos lados.","minutes":15},{"order":2,"title":"Anadir el ajo","description":"Incorpora el ajo laminado.","minutes":20}]"""
private const val BOCADILLO_INGREDIENTS =
    """[{"name":"Pan","quantity":1,"unit":null,"isPantry":false},{"name":"Tomate","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"tomate"}]"""
private const val BOCADILLO_STEPS =
    """[{"order":1,"title":"Montar el bocadillo","description":"Unta el tomate y aliña.","minutes":5}]"""

private fun arrozDraft(): String = draftJson()

private fun polloDraft(): String = draftJson(
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
    ingredients = POLLO_INGREDIENTS,
    steps = POLLO_STEPS,
)

private fun bocadilloDraft(): String = draftJson(
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
    ingredients = BOCADILLO_INGREDIENTS,
    steps = BOCADILLO_STEPS,
    declaredAllergens = """["gluten"]""",
)

/**
 * Receta del contrato §4 como **JSON literal**: sin `id` ni `source`, con `matchScore` opcional
 * (campo del contrato que la app ignora). Nunca se construye desde `Recipe`/`RecipeJson.encode`.
 */
private fun draftJson(
    title: String = "Arroz salteado con verduras",
    description: String = "Un arroz rapido con verduras.",
    categoryId: String = "rice",
    mealTypeId: String = "lunch",
    cuisineId: String = "asian",
    totalMinutes: Int = 25,
    prepMinutes: Int = 10,
    cookMinutes: Int = 15,
    difficultyId: String = "easy",
    servings: Int = 2,
    ingredients: String = ARROZ_INGREDIENTS,
    steps: String = ARROZ_STEPS,
    tips: String = "[]",
    nutrition: String = "null",
    declaredAllergens: String = "[]",
    matchScore: Int? = null,
): String {
    val matchScoreField = matchScore?.let { """, "matchScore": $it""" }.orEmpty()
    return """
        {
          "title": "$title",
          "description": "$description",
          "categoryId": "$categoryId",
          "mealTypeId": "$mealTypeId",
          "cuisineId": "$cuisineId",
          "totalMinutes": $totalMinutes,
          "prepMinutes": $prepMinutes,
          "cookMinutes": $cookMinutes,
          "difficultyId": "$difficultyId",
          "servings": $servings,
          "ingredients": $ingredients,
          "steps": $steps,
          "tips": $tips,
          "nutrition": $nutrition,
          "declaredAllergens": $declaredAllergens$matchScoreField
        }
    """.trimIndent()
}
