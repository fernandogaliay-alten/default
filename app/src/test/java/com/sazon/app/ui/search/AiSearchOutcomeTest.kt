package com.sazon.app.ui.search

import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.domain.validation.DiscardReason
import com.sazon.app.domain.validation.DiscardedRecipe
import com.sazon.app.ui.results.ResultsStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1, 2, 5 y 6 de la spec (decision del estado): `Results` con aceptadas, `NoResults` con
 * vacio sin descartes de perfil, `Blocked` con `DIET_PATTERN`/`HARD_RESTRICTION` y `Failure` con
 * `aiUnavailable`; y que un descarte `INCOHERENT`/`SCHEMA` **no** produce `Blocked`.
 */
class AiSearchOutcomeTest {

    private val taxonomy = taxonomy()
    private val preferences = perfil()

    @Test
    fun las_aceptadas_producen_resultados_con_el_subtitulo_de_busqueda() {
        val result = GenerationResult(accepted = listOf(arroz()), discarded = emptyList())

        val stage = AiSearchOutcome.stageOf(result, "  pollo y arroz  ", preferences, taxonomy)

        assertTrue(stage is AiSearchStage.Results)
        stage as AiSearchStage.Results
        assertEquals("Resultados de tu búsqueda", stage.header.title)
        assertEquals("“pollo y arroz”", stage.header.subtitle)
        assertTrue("El estado debe ser Content", stage.status is ResultsStatus.Content)
        assertEquals(
            listOf("Arroz salteado con verduras"),
            (stage.status as ResultsStatus.Content).recipes.map { it.title },
        )
    }

    @Test
    fun un_vacio_sin_descartes_de_perfil_es_sin_resultados() {
        val stage = AiSearchOutcome.stageOf(
            GenerationResult(accepted = emptyList(), discarded = emptyList()),
            "algo raro",
            preferences,
            taxonomy,
        )

        assertEquals(AiSearchStage.NoResults("algo raro"), stage)
    }

    @Test
    fun un_descarte_por_dieta_es_bloqueo() {
        val result = GenerationResult(
            accepted = emptyList(),
            discarded = listOf(discard(DiscardReason.DIET_PATTERN)),
        )

        assertEquals(AiSearchStage.Blocked("carne"), AiSearchOutcome.stageOf(result, "carne", preferences, taxonomy))
    }

    @Test
    fun un_descarte_por_restriccion_dura_es_bloqueo() {
        val result = GenerationResult(
            accepted = emptyList(),
            discarded = listOf(discard(DiscardReason.HARD_RESTRICTION)),
        )

        assertEquals(AiSearchStage.Blocked("bocadillo"), AiSearchOutcome.stageOf(result, "bocadillo", preferences, taxonomy))
    }

    @Test
    fun un_descarte_incoherente_o_de_esquema_no_es_bloqueo() {
        val incoherent = GenerationResult(
            accepted = emptyList(),
            discarded = listOf(discard(DiscardReason.INCOHERENT), discard(DiscardReason.SCHEMA)),
        )

        assertEquals(
            AiSearchStage.NoResults("cualquiera"),
            AiSearchOutcome.stageOf(incoherent, "cualquiera", preferences, taxonomy),
        )
    }

    @Test
    fun ai_unavailable_es_fallo() {
        val result = GenerationResult(accepted = emptyList(), discarded = emptyList(), aiUnavailable = true)

        assertEquals(AiSearchStage.Failure("pollo"), AiSearchOutcome.stageOf(result, "pollo", preferences, taxonomy))
    }

    private fun discard(reason: DiscardReason) =
        DiscardedRecipe(reason = reason, detail = "detalle", title = "Receta")
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

private fun perfil() = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = emptyList(),
    softPreferenceIds = emptyList(),
    cuisineIds = emptyList(),
    preferredIngredientIds = emptyList(),
    dislikedIngredientIds = emptyList(),
    difficultyId = "easy",
    timeBucketId = "lte_15",
    defaultServings = 2,
    updatedAt = 1_600_000_000_000L,
)

private fun taxonomy(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
