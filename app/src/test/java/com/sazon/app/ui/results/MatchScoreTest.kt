package com.sazon.app.ui.results

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Formula derivada de `matchScore` (`docs/specs/results-screen.md`, escenario 2 y verificacion
 * exigida por el harness): ejemplo trabajado 86/76, rango `0..100`, cobertura por `ingredientId` y por
 * nombre normalizado, sin ingredientes resueltos (no divide por cero) y determinismo. Logica pura sin
 * Compose ni Android sobre la Taxonomia real.
 */
class MatchScoreTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    private val perfilBase: PreferenceProfile = perfil(
        preferredIngredientIds = listOf("tomate", "cebolla", "ajo"),
        cuisineIds = listOf("italian"),
        timeBucketId = "lte_30",
        difficultyId = "easy",
    )

    @Test
    fun el_ejemplo_trabajado_da_86() {
        assertEquals(86, MatchScore.of(recetaItaliana(), perfilBase, taxonomy, emptyList()))
    }

    @Test
    fun un_ingrediente_evitado_presente_resta_la_penalizacion_y_da_76() {
        val perfil = perfilBase.copy(dislikedIngredientIds = listOf("cebolla"))

        assertEquals(76, MatchScore.of(recetaItaliana(), perfil, taxonomy, emptyList()))
    }

    @Test
    fun es_determinista() {
        val primera = MatchScore.of(recetaItaliana(), perfilBase, taxonomy, emptyList())
        val segunda = MatchScore.of(recetaItaliana(), perfilBase, taxonomy, emptyList())

        assertEquals(primera, segunda)
    }

    @Test
    fun la_cobertura_casa_por_id_y_por_nombre_normalizado() {
        val usuario = listOf(
            ingredienteUsuario(name = "Tomate", ingredientId = "tomate"),
            ingredienteUsuario(name = "CEBOLLA", ingredientId = null),
        )

        assertEquals(90, MatchScore.of(recetaItaliana(), perfilBase, taxonomy, usuario))
    }

    @Test
    fun un_ingrediente_del_usuario_sin_coincidencia_baja_la_puntuacion() {
        val sinUsuario = MatchScore.of(recetaItaliana(), perfilBase, taxonomy, emptyList())
        val conDesconocido = MatchScore.of(
            recetaItaliana(),
            perfilBase,
            taxonomy,
            listOf(ingredienteUsuario(name = "Pimiento", ingredientId = null)),
        )

        assertTrue("La cobertura 0 debe penalizar frente a no calcularla", conDesconocido < sinUsuario)
        assertTrue(conDesconocido in 0..100)
    }

    @Test
    fun una_receta_sin_ingredientes_resueltos_no_divide_por_cero() {
        val sinResueltos = receta(
            cuisineId = "thai",
            totalMinutes = 90,
            difficultyId = "hard",
            ingredients = listOf(ingredienteUsuario(name = "Algo libre", ingredientId = null)),
        )

        val score = MatchScore.of(sinResueltos, perfilBase, taxonomy, emptyList())

        assertTrue(score in 0..100)
        assertEquals("Sin señales a favor, la base es 0", 0, score)
    }

    @Test
    fun una_receta_sin_ingredientes_no_divide_por_cero() {
        val vacia = receta(ingredients = emptyList())

        assertTrue(MatchScore.of(vacia, perfilBase, taxonomy, emptyList()) in 0..100)
    }

    @Test
    fun la_penalizacion_se_recorta_a_cero_y_nunca_baja_de_cero() {
        val ids = (1..10).map { "ing-$it" }
        val perfil = perfilBase.copy(
            preferredIngredientIds = ids,
            dislikedIngredientIds = ids,
        )
        val receta = receta(
            ingredients = ids.map { ingredienteUsuario(name = "Ingrediente $it", ingredientId = it) },
        )

        assertEquals(0, MatchScore.of(receta, perfil, taxonomy, emptyList()))
    }
}

private fun recetaItaliana(): Recipe = receta(
    cuisineId = "italian",
    totalMinutes = 25,
    difficultyId = "easy",
    ingredients = listOf(
        ingredienteUsuario(name = "Tomate", ingredientId = "tomate"),
        ingredienteUsuario(name = "Cebolla", ingredientId = "cebolla"),
        ingredienteUsuario(name = "Arroz", ingredientId = "arroz"),
    ),
)

private fun ingredienteUsuario(
    name: String,
    ingredientId: String?,
): RecipeIngredient = RecipeIngredient(
    name = name,
    quantity = 0.0,
    unit = null,
    isPantry = false,
    ingredientId = ingredientId,
)

private fun receta(
    cuisineId: String = "italian",
    totalMinutes: Int = 20,
    difficultyId: String = "easy",
    ingredients: List<RecipeIngredient> = emptyList(),
): Recipe = Recipe(
    id = "receta-1",
    source = RecipeSource.SEED,
    title = "Receta de prueba",
    description = "Una descripción",
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

private fun perfil(
    preferredIngredientIds: List<String> = emptyList(),
    cuisineIds: List<String> = emptyList(),
    timeBucketId: String = "lte_30",
    difficultyId: String = "easy",
): PreferenceProfile = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = emptyList(),
    softPreferenceIds = emptyList(),
    cuisineIds = cuisineIds,
    preferredIngredientIds = preferredIngredientIds,
    dislikedIngredientIds = emptyList(),
    difficultyId = difficultyId,
    timeBucketId = timeBucketId,
    defaultServings = 2,
    updatedAt = 1_600_000_000_000L,
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
