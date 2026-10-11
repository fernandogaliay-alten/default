package com.sazon.app.domain.search

import com.sazon.app.data.generator.FakeRecipeGenerator
import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.domain.validation.DiscardReason
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 2, 3, 6, 8 y 9 de la spec: orquestacion `searchByText -> validador`, degradacion a
 * `aiUnavailable`, memo de sesion y reporte de aceptadas. Logica pura sin Android ni Robolectric.
 *
 * Hallazgo (ver `docs/specs/ai-search.md`): con el `FakeRecipeGenerator` real, un Perfil **vegano**
 * solo descarta "Pollo al ajillo"; las otras tres fixtures son vegetales y se aceptan, asi que
 * `accepted` **no** queda vacio. Para satisfacer la verificacion exigida ("una consulta que
 * contradice el patron de dieta devuelve **cero** recetas") el caso de cero recetas usa un generador
 * cuya salida es **toda** de dieta contraria (solo el pollo).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiSearchServiceTest {

    private val taxonomy = taxonomy()

    private fun service(
        generator: RecipeGenerator,
        onRecipesGenerated: suspend (List<Recipe>) -> Unit = {},
    ) = AiSearchService(
        generator = generator,
        taxonomy = taxonomy,
        onRecipesGenerated = onRecipesGenerated,
    )

    // Escenario 2 (verificacion exigida): una consulta que contradice el patron de dieta devuelve
    // cero recetas. Generador cuya salida es toda de dieta contraria -> accepted queda vacio.
    @Test
    fun consulta_que_contradice_el_patron_de_dieta_devuelve_cero_recetas() = runTest {
        val result = service(FixedGenerator(listOf(polloAlAjillo())))
            .search("carne", perfil(dietPattern = "vegan"))

        assertTrue("Con perfil vegano no puede haber aceptadas", result.accepted.isEmpty())
        assertFalse(result.aiUnavailable)
        val pollo = result.discarded.single { it.title == "Pollo al ajillo" }
        assertEquals(DiscardReason.DIET_PATTERN, pollo.reason)
    }

    // El generador falso real: con perfil vegano el pollo cae por dieta aunque el resto se acepte.
    @Test
    fun con_el_generador_falso_el_pollo_se_descarta_por_dieta() = runTest {
        val result = service(FakeRecipeGenerator()).search("carne", perfil(dietPattern = "vegan"))

        assertFalse(
            "El pollo no puede estar aceptado con dieta vegana",
            result.accepted.any { it.title == "Pollo al ajillo" },
        )
        val pollo = result.discarded.single { it.title == "Pollo al ajillo" }
        assertEquals(DiscardReason.DIET_PATTERN, pollo.reason)
    }

    // Escenario 3: una restriccion dura incumplida no llega a resultados.
    @Test
    fun la_restriccion_dura_descarta_el_bocadillo() = runTest {
        val result = service(FakeRecipeGenerator())
            .search("bocadillo", perfil(hardRestrictions = listOf("gluten")))

        assertFalse(
            "El bocadillo (Pan libre -> gluten) no puede estar aceptado",
            result.accepted.any { it.title == "Bocadillo de tomate" },
        )
        val bocadillo = result.discarded.single { it.title == "Bocadillo de tomate" }
        assertEquals(DiscardReason.HARD_RESTRICTION, bocadillo.reason)
    }

    // Escenario 6: un fallo del generador degrada a aiUnavailable sin propagar.
    @Test
    fun un_generador_que_lanza_degrada_a_ai_unavailable() = runTest {
        val result = service(ThrowingGenerator()).search("pollo", perfil())

        assertTrue("Debe marcar aiUnavailable", result.aiUnavailable)
        assertTrue("Sin recetas aceptadas", result.accepted.isEmpty())
        assertTrue("Sin descartes (no hubo respuesta)", result.discarded.isEmpty())
    }

    // Escenario 7: la cancelacion se re-lanza (no se traga).
    @Test
    fun la_cancelacion_se_propaga() = runTest {
        val searchService = service(SuspendingGenerator())
        var propagated: CancellationException? = null
        val job = launch {
            try {
                searchService.search("pollo", perfil())
            } catch (cancellation: CancellationException) {
                propagated = cancellation
                throw cancellation
            }
        }

        runCurrent()
        job.cancelAndJoin()

        assertTrue("La corrutina debe quedar cancelada", job.isCancelled)
        assertNotNull("AiSearchService debe re-lanzar CancellationException", propagated)
    }

    // La consulta en blanco no llama al generador (la UI impide enviar en blanco).
    @Test
    fun la_consulta_en_blanco_no_llama_al_generador() = runTest {
        val generator = CountingGenerator()
        val result = service(generator).search("   ", perfil())

        assertEquals(0, generator.calls)
        assertTrue(result.accepted.isEmpty())
        assertTrue(result.discarded.isEmpty())
        assertFalse(result.aiUnavailable)
    }

    // Escenario 8: el memo sirve la misma consulta sin repetir llamada; una distinta si llama.
    @Test
    fun el_memo_no_repite_llamada_para_la_misma_consulta() = runTest {
        val generator = CountingGenerator(listOf(arroz()))
        val searchService = service(generator)

        searchService.search("  Pollo  ", perfil())
        searchService.search("pollo", perfil())
        assertEquals("La misma consulta normalizada no repite llamada", 1, generator.calls)
        assertEquals("El generador recibe la clave normalizada", "pollo", generator.lastQuery)

        searchService.search("arroz", perfil())
        assertEquals("Una consulta distinta si vuelve a llamar", 2, generator.calls)
    }

    // Escenario 8: un fallo no se memoiza, de modo que "Reintentar" vuelve a llamar.
    @Test
    fun un_fallo_no_se_memoiza_y_reintentar_vuelve_a_llamar() = runTest {
        val generator = ThrowingGenerator()
        val searchService = service(generator)

        val first = searchService.search("pollo", perfil())
        val second = searchService.search("pollo", perfil())

        assertTrue(first.aiUnavailable)
        assertTrue(second.aiUnavailable)
        assertEquals("El fallo no se memoiza: se vuelve a llamar", 2, generator.calls)
    }

    // Escenario 9: la costura recibe exactamente las aceptadas.
    @Test
    fun on_recipes_generated_recibe_solo_las_aceptadas() = runTest {
        val captured = mutableListOf<List<Recipe>>()
        val result = service(
            generator = FakeRecipeGenerator(),
            onRecipesGenerated = { recipes -> captured += recipes },
        ).search("bocadillo", perfil(hardRestrictions = listOf("gluten")))

        assertEquals(1, captured.size)
        assertEquals(result.accepted.map { it.title }.sorted(), captured.single().map { it.title }.sorted())
        assertFalse(captured.single().any { it.title == "Bocadillo de tomate" })
    }
}

private class FixedGenerator(private val recipes: List<Recipe>) : RecipeGenerator {
    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> =
        error("suggestByPreferences no se usa en la busqueda")

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> = recipes

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = error("generateFromIngredients no se usa en la busqueda")
}

private class CountingGenerator(private val recipes: List<Recipe> = emptyList()) : RecipeGenerator {
    var calls = 0
    var lastQuery: String? = null

    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> =
        error("suggestByPreferences no se usa en la busqueda")

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> {
        calls += 1
        lastQuery = query
        return recipes
    }

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = error("generateFromIngredients no se usa en la busqueda")
}

private class ThrowingGenerator : RecipeGenerator {
    var calls = 0

    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> =
        error("suggestByPreferences no se usa en la busqueda")

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> {
        calls += 1
        throw IllegalStateException("sin red o sin clave")
    }

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = error("generateFromIngredients no se usa en la busqueda")
}

private class SuspendingGenerator : RecipeGenerator {
    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> =
        error("suggestByPreferences no se usa en la busqueda")

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> =
        awaitCancellation()

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = error("generateFromIngredients no se usa en la busqueda")
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

private fun perfil(
    hardRestrictions: List<String> = emptyList(),
    dietPattern: String = "omnivore",
) = PreferenceProfile(
    dietPatternId = dietPattern,
    hardRestrictionIds = hardRestrictions,
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
