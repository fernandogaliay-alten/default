package com.sazon.app.ui.home

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.recipe.SeedRecipeCatalog
import com.sazon.app.domain.recipe.SeedRecipeJson
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.ui.results.MatchScore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios del carrusel "Sugeridas para ti" (`docs/specs/home-suggestions-personalized.md`): la
 * fuente (Lote resuelto o respaldo de semilla) pasa **siempre** por el mismo camino — filtro de
 * seguridad (`RecipeValidator`) → orden por relevancia (`MatchScore`) → tope de
 * [HomeSuggestions.MAX_VISIBLE]. Logica pura, sin emulador.
 *
 * Los casos negativos (dieta y restriccion dura) no pasan en vacio: el mismo input con un Perfil
 * `omnivore` sin restricciones **si** muestra las recetas descartadas.
 */
class HomeSuggestionsTest {

    private val taxonomy = taxonomiaReal()
    private val catalogoSemilla = catalogoSemillaReal()

    private val seeds = listOf(receta("seed-1"), receta("seed-2"))
    private val lote = listOf(receta("ai-1", source = RecipeSource.AI), receta("seed-2"))

    @Test
    fun sin_lote_resuelto_se_muestran_las_semillas() {
        assertEquals(seeds, HomeSuggestions.visible(null, seeds, perfil(), taxonomy))
    }

    @Test
    fun con_lote_resuelto_se_muestran_las_recetas_del_lote() {
        assertEquals(lote, HomeSuggestions.visible(lote, seeds, perfil(), taxonomy))
    }

    @Test
    fun un_lote_vacio_no_cae_a_semilla() {
        val visibles = HomeSuggestions.visible(emptyList(), seeds, perfil(), taxonomy)

        assertTrue("Un lote resuelto vacio es un resultado real, no una ausencia", visibles.isEmpty())
    }

    // --- Escenario 1: tope de 6 tarjetas ---

    @Test
    fun el_carrusel_se_acota_a_seis_tarjetas() {
        val ocho = (1..8).map { receta("seed-$it") }

        val visibles = HomeSuggestions.visible(ocho, emptyList(), perfil(), taxonomy)

        assertEquals(HomeSuggestions.MAX_VISIBLE, visibles.size)
        assertEquals("Todas puntuan igual: el tope conserva el orden de origen", ocho.take(6), visibles)
    }

    @Test
    fun con_menos_de_seis_seguras_no_se_recorta() {
        val cuatro = (1..4).map { receta("seed-$it") }

        val visibles = HomeSuggestions.visible(cuatro, emptyList(), perfil(), taxonomy)

        assertEquals(4, visibles.size)
        assertEquals(cuatro, visibles)
    }

    // --- Escenario 2: orden por relevancia descendente, estable en empates ---

    @Test
    fun el_orden_es_por_relevancia_descendente() {
        val afin = receta("seed-italiana", cuisineId = "italian", totalMinutes = 25, difficultyId = "easy")
        val ajena = receta("seed-otra", cuisineId = "spanish", totalMinutes = 60, difficultyId = "hard")
        val preferences = perfil(
            cuisineIds = listOf("italian"),
            timeBucketId = "lte_30",
            difficultyId = "easy",
        )

        assertTrue(
            "El perfil debe puntuar mas la receta afin",
            MatchScore.of(afin, preferences, taxonomy) > MatchScore.of(ajena, preferences, taxonomy),
        )

        // El input llega en orden inverso al esperado: solo un ranking real lo reordena.
        val visibles = HomeSuggestions.visible(listOf(ajena, afin), emptyList(), preferences, taxonomy)

        assertEquals(listOf("seed-italiana", "seed-otra"), visibles.map { it.id })
    }

    @Test
    fun los_empates_conservan_el_orden_de_origen() {
        val a = receta("seed-a")
        val b = receta("seed-b")
        val c = receta("seed-c")

        val visibles = HomeSuggestions.visible(listOf(c, a, b), emptyList(), perfil(), taxonomy)

        assertEquals("Los empates no se reordenan por azar", listOf("seed-c", "seed-a", "seed-b"), visibles.map { it.id })
        assertEquals(
            "Las tres deben puntuar igual para que el caso no pase en vacio",
            1,
            visibles.map { MatchScore.of(it, perfil(), taxonomy) }.toSet().size,
        )
    }

    // --- Escenario 3: exclusion por patron de dieta y restriccion dura (semilla y ai) ---

    @Test
    fun la_dieta_descarta_semilla_y_ai_con_pollo_mientras_las_seguras_siguen() {
        val semillaPollo = receta(
            id = "seed-pollo",
            ingredients = listOf(ingrediente("Pollo", "pollo")),
        )
        val aiPollo = receta(
            id = "ai-pollo",
            source = RecipeSource.AI,
            ingredients = listOf(ingrediente("Pollo", "pollo")),
        )
        val segura = receta(id = "seed-tomate", ingredients = listOf(ingrediente("Tomate", "tomate")))
        val entrada = listOf(semillaPollo, aiPollo, segura)

        val vegetarianas = HomeSuggestions.visible(
            entrada,
            emptyList(),
            perfil(dietPatternId = "vegetarian"),
            taxonomy,
        )

        assertEquals(listOf("seed-tomate"), vegetarianas.map { it.id })

        // Control positivo: el caso negativo no pasa en vacio (sin dieta las tres entran).
        val omnivoras = HomeSuggestions.visible(entrada, emptyList(), perfil(), taxonomy)
        assertEquals(3, omnivoras.size)
        assertEquals(setOf("seed-pollo", "ai-pollo", "seed-tomate"), omnivoras.map { it.id }.toSet())
    }

    @Test
    fun la_restriccion_dura_descarta_el_ingrediente_libre_pan_mientras_las_seguras_siguen() {
        val panLibre = receta(
            id = "seed-bocadillo",
            ingredients = listOf(RecipeIngredient("Pan", 2.0, MeasureUnit.UNIT, ingredientId = null)),
        )
        val segura = receta(id = "seed-tomate", ingredients = listOf(ingrediente("Tomate", "tomate")))
        val entrada = listOf(panLibre, segura)

        val conGluten = HomeSuggestions.visible(
            entrada,
            emptyList(),
            perfil(hardRestrictionIds = listOf("gluten")),
            taxonomy,
        )

        assertEquals(listOf("seed-tomate"), conGluten.map { it.id })

        // Control positivo: sin la restriccion dura el "Pan" libre si aparece.
        val sinRestricciones = HomeSuggestions.visible(entrada, emptyList(), perfil(), taxonomy)
        assertEquals(2, sinRestricciones.size)
        assertEquals(setOf("seed-bocadillo", "seed-tomate"), sinRestricciones.map { it.id }.toSet())
    }

    // --- Escenario 5: el respaldo de semilla offline se filtra, se ordena y se acota igual ---

    @Test
    fun el_respaldo_de_semilla_se_acota_con_el_mismo_camino() {
        val visibles = HomeSuggestions.visible(null, catalogoSemilla.recipes, perfil(), taxonomy)

        assertEquals(HomeSuggestions.MAX_VISIBLE, visibles.size)
    }

    @Test
    fun con_gluten_el_respaldo_de_semilla_excluye_las_recetas_inseguras() {
        val visibles = HomeSuggestions.visible(
            null,
            catalogoSemilla.recipes,
            perfil(hardRestrictionIds = listOf("gluten")),
            taxonomy,
        )

        assertTrue("Debe quedar respaldo suficiente para el carrusel", visibles.isNotEmpty())
        assertTrue(visibles.size <= HomeSuggestions.MAX_VISIBLE)
        assertTrue(
            "Ninguna semilla que active gluten puede sobrevivir al filtro: ${visibles.map { it.id }}",
            visibles.none { it.id in SEMILLAS_CON_GLUTEN },
        )
        assertTrue(
            "Caso explicito de la spec (pasta y declaredAllergens activan gluten)",
            visibles.none { it.id == "seed-pasta-tomate" },
        )
    }

    // --- Escenario 6: las preferencias blandas penalizan, nunca descartan ---

    @Test
    fun las_preferencias_blandas_penalizan_pero_no_descartan() {
        val preferences = perfil(
            cuisineIds = listOf("italian"),
            timeBucketId = "lte_30",
            difficultyId = "easy",
            dislikedIngredientIds = listOf("cebolla"),
        )
        val conEvitado = receta(
            id = "seed-con-cebolla",
            cuisineId = "italian",
            totalMinutes = 25,
            ingredients = listOf(ingrediente("Cebolla", "cebolla")),
        )
        val limpia = receta(
            id = "seed-sin-cebolla",
            cuisineId = "italian",
            totalMinutes = 25,
            ingredients = listOf(ingrediente("Tomate", "tomate")),
        )

        assertTrue(
            "El ingrediente evitado debe penalizar el score",
            MatchScore.of(limpia, preferences, taxonomy) > MatchScore.of(conEvitado, preferences, taxonomy),
        )

        // El input llega con la penalizada primero: si no se descarta pero si se reordena, ambos extremos
        // del comportamiento quedan cubiertos.
        val visibles = HomeSuggestions.visible(listOf(conEvitado, limpia), emptyList(), preferences, taxonomy)

        assertEquals(setOf("seed-con-cebolla", "seed-sin-cebolla"), visibles.map { it.id }.toSet())
        assertEquals(listOf("seed-sin-cebolla", "seed-con-cebolla"), visibles.map { it.id })
    }

    // --- Escenario 7: sin cambio visual (score no materializado) ---

    @Test
    fun las_recetas_devueltas_conservan_su_match_score_original() {
        val segura = receta(id = "seed-tomate", ingredients = listOf(ingrediente("Tomate", "tomate")))

        val visibles = HomeSuggestions.visible(listOf(segura), emptyList(), perfil(), taxonomy)

        assertEquals(1, visibles.size)
        assertSame("La Home pinta la receta original, no una copia", segura, visibles.first())
        assertNull(
            "Sin materializar el score: la pildora de coincidencia de RecipeCard no se pinta",
            visibles.first().matchScore,
        )
    }
}

private val SEMILLAS_CON_GLUTEN = setOf(
    "seed-gazpacho-andaluz",
    "seed-pasta-tomate",
    "seed-tortitas-de-avena",
    "seed-tarta-de-manzana",
)

/**
 * Fixture **coherente** (pasos que suman `totalMinutes` y alérgenos declarados coherentes) para que el
 * filtro de seguridad no descarte por la regla 4 lo que el test quiere conservar.
 */
private fun receta(
    id: String,
    source: RecipeSource = RecipeSource.SEED,
    cuisineId: String = "spanish",
    totalMinutes: Int = 20,
    difficultyId: String = "easy",
    ingredients: List<RecipeIngredient> = emptyList(),
): Recipe = Recipe(
    id = id,
    source = source,
    title = "Receta $id",
    description = "",
    categoryId = "other",
    mealTypeId = "lunch",
    cuisineId = cuisineId,
    totalMinutes = totalMinutes,
    prepMinutes = 5,
    cookMinutes = totalMinutes - 5,
    difficultyId = difficultyId,
    servings = 2,
    ingredients = ingredients,
    steps = listOf(
        RecipeStep(1, "Preparar", "Prepara los ingredientes.", totalMinutes / 2),
        RecipeStep(2, "Cocinar", "Cocina y sirve.", totalMinutes - totalMinutes / 2),
    ),
)

private fun ingrediente(name: String, ingredientId: String) =
    RecipeIngredient(name, 100.0, MeasureUnit.GRAM, ingredientId = ingredientId)

private fun perfil(
    dietPatternId: String = "omnivore",
    hardRestrictionIds: List<String> = emptyList(),
    cuisineIds: List<String> = emptyList(),
    preferredIngredientIds: List<String> = emptyList(),
    dislikedIngredientIds: List<String> = emptyList(),
    difficultyId: String = "easy",
    timeBucketId: String = "lte_45",
): PreferenceProfile = PreferenceProfile(
    dietPatternId = dietPatternId,
    hardRestrictionIds = hardRestrictionIds,
    softPreferenceIds = emptyList(),
    cuisineIds = cuisineIds,
    preferredIngredientIds = preferredIngredientIds,
    dislikedIngredientIds = dislikedIngredientIds,
    difficultyId = difficultyId,
    timeBucketId = timeBucketId,
    defaultServings = 2,
    updatedAt = 0L,
)

private fun taxonomiaReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}

private fun catalogoSemillaReal(): SeedRecipeCatalog {
    val path = checkNotNull(System.getProperty("seed.recipes.asset.path")) {
        "Falta la propiedad de sistema seed.recipes.asset.path"
    }
    return SeedRecipeJson.parse(File(path).readText())
}
