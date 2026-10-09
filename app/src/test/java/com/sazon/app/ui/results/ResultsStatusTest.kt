package com.sazon.app.ui.results

import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.domain.validation.DiscardReason
import com.sazon.app.domain.validation.DiscardedRecipe
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seleccion de estado de la pantalla de Resultados (`docs/specs/results-screen.md`, escenarios 1, 3, 4
 * y 5): `Content` puntuado y ordenado por `matchScore` desc (sin `discarded`), `Empty` y `Unavailable`
 * con y sin semilla. Logica pura sin Compose ni Android.
 */
class ResultsStatusTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    private val perfilBase: PreferenceProfile = perfil()

    @Test
    fun content_lista_solo_aceptadas_puntuadas_y_ordenadas_por_relevancia() {
        val result = GenerationResult(
            accepted = listOf(recetaBaja(), recetaAlta()),
            discarded = listOf(
                DiscardedRecipe(
                    reason = DiscardReason.HARD_RESTRICTION,
                    detail = "Pan libre con gluten",
                    title = "Bocadillo de tomate",
                ),
            ),
            aiUnavailable = false,
        )

        val status = ResultsStatus.from(result, perfilBase, taxonomy, emptyList(), seedRecipes = emptyList())

        assertTrue("Esperaba Content, fue $status", status is ResultsStatus.Content)
        val content = status as ResultsStatus.Content
        assertEquals(listOf("alta", "baja"), content.recipes.map { it.id })
        assertTrue("Toda receta del Content lleva matchScore", content.recipes.all { it.matchScore != null })
        assertTrue(
            "Ninguna receta descartada se renderiza",
            content.recipes.none { it.title == "Bocadillo de tomate" },
        )
    }

    @Test
    fun content_recorta_el_score_a_un_valor_no_nulo() {
        val result = GenerationResult(accepted = listOf(recetaAlta()), discarded = emptyList())

        val content = ResultsStatus.from(
            result,
            perfilBase,
            taxonomy,
            emptyList(),
            seedRecipes = emptyList(),
        ) as ResultsStatus.Content

        assertNotNull(content.recipes.single().matchScore)
        assertEquals(86, content.recipes.single().matchScore)
    }

    @Test
    fun accepted_tiene_prioridad_sobre_ai_unavailable() {
        val result = GenerationResult(
            accepted = listOf(recetaAlta()),
            discarded = emptyList(),
            aiUnavailable = true,
        )

        val status = ResultsStatus.from(result, perfilBase, taxonomy, emptyList(), seedRecipes = emptyList())

        assertTrue("Con aceptadas, gana Content aunque aiUnavailable sea true", status is ResultsStatus.Content)
    }

    @Test
    fun empty_cuando_no_hay_aceptadas_ni_fallo_de_ia() {
        val result = GenerationResult(accepted = emptyList(), discarded = emptyList(), aiUnavailable = false)

        val status = ResultsStatus.from(result, perfilBase, taxonomy, emptyList(), seedRecipes = emptyList())

        assertEquals(ResultsStatus.Empty, status)
    }

    @Test
    fun unavailable_con_semillas_las_puntua_y_ordena() {
        val result = GenerationResult(accepted = emptyList(), discarded = emptyList(), aiUnavailable = true)
        val semillas = listOf(recetaBaja(id = "seed-baja"), recetaAlta(id = "seed-alta"))

        val status = ResultsStatus.from(result, perfilBase, taxonomy, emptyList(), seedRecipes = semillas)

        assertTrue("Esperaba Unavailable, fue $status", status is ResultsStatus.Unavailable)
        val unavailable = status as ResultsStatus.Unavailable
        assertEquals(listOf("seed-alta", "seed-baja"), unavailable.seedRecipes.map { it.id })
        assertTrue(unavailable.seedRecipes.all { it.matchScore != null })
    }

    @Test
    fun unavailable_sin_semillas_es_un_disponible_vacio() {
        val result = GenerationResult(accepted = emptyList(), discarded = emptyList(), aiUnavailable = true)

        val status = ResultsStatus.from(result, perfilBase, taxonomy, emptyList(), seedRecipes = emptyList())

        assertEquals(ResultsStatus.Unavailable(emptyList()), status)
    }
}

private fun recetaAlta(id: String = "alta"): Recipe = receta(
    id = id,
    cuisineId = "italian",
    totalMinutes = 25,
    difficultyId = "easy",
    ingredients = listOf(
        ingrediente("Tomate", "tomate"),
        ingrediente("Cebolla", "cebolla"),
        ingrediente("Arroz", "arroz"),
    ),
)

private fun recetaBaja(id: String = "baja"): Recipe = receta(
    id = id,
    cuisineId = "thai",
    totalMinutes = 90,
    difficultyId = "hard",
    ingredients = listOf(ingrediente("Arroz", "arroz")),
)

private fun ingrediente(name: String, ingredientId: String?): RecipeIngredient = RecipeIngredient(
    name = name,
    quantity = 0.0,
    unit = null,
    isPantry = false,
    ingredientId = ingredientId,
)

private fun receta(
    id: String,
    cuisineId: String,
    totalMinutes: Int,
    difficultyId: String,
    ingredients: List<RecipeIngredient>,
): Recipe = Recipe(
    id = id,
    source = RecipeSource.SEED,
    title = "Receta $id",
    description = "Descripción $id",
    categoryId = "other",
    mealTypeId = "lunch",
    cuisineId = cuisineId,
    totalMinutes = totalMinutes,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = difficultyId,
    servings = 2,
    ingredients = ingredients,
    steps = emptyList(),
)

private fun perfil(): PreferenceProfile = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = emptyList(),
    softPreferenceIds = emptyList(),
    cuisineIds = listOf("italian"),
    preferredIngredientIds = listOf("tomate", "cebolla", "ajo"),
    dislikedIngredientIds = emptyList(),
    difficultyId = "easy",
    timeBucketId = "lte_30",
    defaultServings = 2,
    updatedAt = 1_600_000_000_000L,
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
