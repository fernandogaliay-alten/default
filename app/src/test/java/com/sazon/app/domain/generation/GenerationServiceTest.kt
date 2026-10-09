package com.sazon.app.domain.generation

import com.sazon.app.data.generator.FakeRecipeGenerator
import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
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
 * Escenarios 1-4, 6 y 7 de la spec. Usa el `FakeRecipeGenerator` real y la Taxonomia real leida por
 * propiedad de sistema; logica pura sin Android ni Robolectric.
 *
 * El caso negativo obligatorio (escenario 2) es el nucleo de la feature: una receta insegura devuelta
 * por el generador (el "Bocadillo de tomate" con "Pan" libre) no puede llegar a `accepted`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GenerationServiceTest {

    private val taxonomy = taxonomy()

    private fun service(generator: RecipeGenerator) = GenerationService(generator, taxonomy)

    // Escenario 1: camino feliz. Las tres seguras entran; "Pollo al ajillo" cae por INCOHERENT.
    @Test
    fun generacion_segura_camino_feliz() = runTest {
        val result = service(FakeRecipeGenerator()).generate(ingredients(), constraints())

        assertFalse(result.aiUnavailable)
        val titles = result.accepted.map { it.title }
        assertTrue("El arroz seguro debe entrar", titles.contains("Arroz salteado con verduras"))
        assertTrue("Las lentejas seguras deben entrar", titles.contains("Lentejas estofadas"))
        assertTrue("El bocadillo sin restricciones debe entrar", titles.contains("Bocadillo de tomate"))
        assertFalse("El pollo incoherente no debe entrar", titles.contains("Pollo al ajillo"))

        val pollo = result.discarded.single { it.title == "Pollo al ajillo" }
        assertEquals(DiscardReason.INCOHERENT, pollo.reason)
    }

    // Escenario 2 (verificacion exigida): una receta insegura no llega a la lista aceptada.
    @Test
    fun una_receta_insegura_no_llega_a_la_lista_aceptada() = runTest {
        val result = service(FakeRecipeGenerator())
            .generate(ingredients(), constraints(perfil(hardRestrictions = listOf("gluten"))))

        assertFalse(
            "El bocadillo (Pan libre -> gluten) no puede estar aceptado",
            result.accepted.any { it.title == "Bocadillo de tomate" },
        )
        val bocadillo = result.discarded.single { it.title == "Bocadillo de tomate" }
        assertEquals(DiscardReason.HARD_RESTRICTION, bocadillo.reason)
        assertTrue(
            "El resto de recetas seguras si entran",
            result.accepted.any { it.title == "Arroz salteado con verduras" },
        )
        assertTrue(result.accepted.any { it.title == "Lentejas estofadas" })
    }

    // Escenario 3: el patron de dieta tambien descarta.
    @Test
    fun el_patron_de_dieta_tambien_descarta() = runTest {
        val result = service(FakeRecipeGenerator())
            .generate(ingredients(), constraints(perfil(dietPattern = "vegetarian")))

        assertFalse(
            "El pollo no puede estar aceptado con dieta vegetariana",
            result.accepted.any { it.title == "Pollo al ajillo" },
        )
        val pollo = result.discarded.single { it.title == "Pollo al ajillo" }
        assertEquals(DiscardReason.DIET_PATTERN, pollo.reason)
    }

    // Escenario 4: se honran las alergias por generacion (las del Perfil efectivo en las constraints).
    @Test
    fun se_honran_las_alergias_por_generacion() = runTest {
        val generatorService = service(FakeRecipeGenerator())

        // Perfil sin gluten, pero el paso de ajustes marca gluten -> se descarta el bocadillo.
        val conGluten = generatorService.generate(
            ingredients(),
            constraints(perfil(hardRestrictions = emptyList()).copy(hardRestrictionIds = listOf("gluten"))),
        )
        assertFalse(
            "Con gluten efectivo el bocadillo debe descartarse",
            conGluten.accepted.any { it.title == "Bocadillo de tomate" },
        )
        assertEquals(
            DiscardReason.HARD_RESTRICTION,
            conGluten.discarded.single { it.title == "Bocadillo de tomate" }.reason,
        )

        // Perfil con gluten, pero el paso de ajustes lo quita -> el bocadillo vuelve a entrar.
        val sinGluten = generatorService.generate(
            ingredients(),
            constraints(perfil(hardRestrictions = listOf("gluten")).copy(hardRestrictionIds = emptyList())),
        )
        assertTrue(
            "Al quitar el gluten en los ajustes el bocadillo debe entrar",
            sinGluten.accepted.any { it.title == "Bocadillo de tomate" },
        )
    }

    // Escenario 6: un generador que suspende indefinidamente y la cancelacion se re-lanza.
    @Test
    fun la_cancelacion_se_propaga() = runTest {
        val generatorService = service(SuspendingGenerator())
        var propagated: CancellationException? = null
        val job = launch {
            try {
                generatorService.generate(emptyList(), constraints())
            } catch (cancellation: CancellationException) {
                propagated = cancellation
                throw cancellation
            }
        }

        runCurrent()
        job.cancelAndJoin()

        assertTrue("La corrutina debe quedar cancelada", job.isCancelled)
        assertNotNull("GenerationService debe re-lanzar CancellationException", propagated)
    }

    // Escenario 7: IA no disponible degrada sin propagar la excepcion.
    @Test
    fun ia_no_disponible_degrada_sin_lanzar() = runTest {
        val result = service(FailingGenerator()).generate(ingredients(), constraints())

        assertTrue("Debe marcar aiUnavailable", result.aiUnavailable)
        assertTrue("Sin recetas aceptadas", result.accepted.isEmpty())
        assertTrue("Sin descartes (no hubo respuesta)", result.discarded.isEmpty())
    }
}

private class SuspendingGenerator : RecipeGenerator {
    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> =
        error("suggestByPreferences no se usa en la generacion principal")

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> =
        error("searchByText no se usa en la generacion principal")

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = awaitCancellation()
}

private class FailingGenerator : RecipeGenerator {
    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> =
        error("suggestByPreferences no se usa en la generacion principal")

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> =
        error("searchByText no se usa en la generacion principal")

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = throw IllegalStateException("sin red o sin clave")
}

private fun ingredients(): List<RecipeIngredient> =
    listOf(RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"))

private fun constraints(profile: PreferenceProfile = perfil()): GenerationConstraints =
    GenerationConstraints(profile = profile, servings = 2)

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
