package com.sazon.app.domain.recipe

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la integridad del catálogo semilla autorado en `app/src/main/assets/seed-recipes.json`.
 *
 * Cubre los escenarios 1 y 3–6 de la spec (el escenario 2, la carga real del asset, vive en
 * `com.sazon.app.data.AssetSeedRecipeSourceTest`). El caso negativo no es un test que se espera rojo:
 * es una aserción sobre `violations(...)` con un JSON mutado, igual que en `TaxonomyIntegrityTest`.
 */
class SeedRecipeCatalogTest {

    private val originalJson = seedRecipesJson()
    private val catalog = SeedRecipeJson.parse(originalJson)
    private val taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun el_catalogo_se_carga_con_al_menos_doce_recetas_semilla() {
        assertTrue("Se esperaban al menos 12 recetas, hay ${catalog.recipes.size}", catalog.recipes.size >= 12)
        assertEquals("Los ids deben ser unicos", catalog.recipes.size, catalog.recipes.map { it.id }.toSet().size)
        catalog.recipes.forEach { recipe ->
            assertEquals("La receta ${recipe.id} debe ser seed", RecipeSource.SEED, recipe.source)
            assertNull("matchScore debe quedar null en las semillas", recipe.matchScore)
            assertTrue("El id debe empezar por seed-: ${recipe.id}", recipe.id.startsWith("seed-"))
        }
    }

    @Test
    fun cada_receta_sobrevive_a_la_ida_y_vuelta_del_esquema() {
        catalog.recipes.forEach { recipe ->
            val recuperada = RecipeJson.parse(RecipeJson.encode(recipe))
            assertEquals("La receta ${recipe.id} no sobrevive a la ida y vuelta", recipe, recuperada)
        }
    }

    @Test
    fun la_integridad_referencial_y_de_contenido_se_cumple() {
        val problems = violations(catalog, taxonomy)
        assertTrue("Violaciones encontradas: $problems", problems.isEmpty())
    }

    @Test
    fun el_catalogo_tiene_diversidad_minima_offline() {
        assertTrue(
            "Falta una receta rapida (totalMinutes <= 30)",
            catalog.recipes.any { it.totalMinutes <= 30 },
        )
        assertTrue(
            "Falta una receta con ingredientes sin etiquetas animales (apta vegana)",
            catalog.recipes.any { it.esAptaVegana(taxonomy) },
        )
    }

    @Test
    fun un_catalogo_con_una_categoria_inexistente_es_rechazado() {
        val roto = originalJson.replace(
            "\"categoryId\": \"pasta\"",
            "\"categoryId\": \"categoria-inexistente\"",
        )
        assertNotEquals("El reemplazo no altero el JSON: el formato cambio", originalJson, roto)
        val problems = violations(SeedRecipeJson.parse(roto), taxonomy)
        assertTrue("Se esperaba al menos una violacion, y no hubo ninguna", problems.isNotEmpty())
    }

    @Test
    fun un_catalogo_con_un_alergeno_omitido_es_rechazado() {
        val roto = originalJson.replace(
            "\"declaredAllergens\": [\"gluten\"]",
            "\"declaredAllergens\": []",
        )
        assertNotEquals("El reemplazo no altero el JSON: el formato cambio", originalJson, roto)
        val problems = violations(SeedRecipeJson.parse(roto), taxonomy)
        assertTrue("Se esperaba al menos una violacion, y no hubo ninguna", problems.isNotEmpty())
    }
}

private val ANIMAL_TAGS = setOf("meat", "poultry", "fish", "seafood", "dairy", "egg", "honey", "gelatin")

private fun seedRecipesJson(): String {
    val path = checkNotNull(System.getProperty("seed.recipes.asset.path")) {
        "Falta la propiedad de sistema seed.recipes.asset.path"
    }
    return File(path).readText()
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}

private fun Recipe.esAptaVegana(taxonomy: Taxonomy): Boolean =
    ingredients
        .mapNotNull { it.ingredientId }
        .mapNotNull { taxonomy.ingredient(it) }
        .none { ingredient -> ingredient.tags.any { it in ANIMAL_TAGS } }

private fun violations(catalog: SeedRecipeCatalog, taxonomy: Taxonomy): List<String> {
    val problems = mutableListOf<String>()

    val restrictionIds = taxonomy.hardRestrictions.map { it.id }.toSet()
    val categoryIds = taxonomy.recipeCategories.map { it.id }.toSet()
    val mealTypeIds = taxonomy.mealTypes.map { it.id }.toSet()
    val cuisineIds = taxonomy.cuisines.map { it.id }.toSet()
    val difficultyIds = taxonomy.difficulties.map { it.id }.toSet()
    val servingsRange = taxonomy.servings.min..taxonomy.servings.max

    catalog.recipes.map { it.id }.groupingBy { it }.eachCount()
        .filter { it.value > 1 }
        .keys
        .forEach { problems += "id duplicado '$it'" }

    catalog.recipes.forEach { recipe ->
        if (!recipe.id.startsWith("seed-")) problems += "${recipe.id}: debe empezar por seed-"
        if (recipe.source != RecipeSource.SEED) problems += "${recipe.id}: source debe ser seed"
        if (recipe.matchScore != null) problems += "${recipe.id}: matchScore debe ser null"

        if (recipe.categoryId !in categoryIds) problems += "${recipe.id}: categoryId '${recipe.categoryId}' no existe"
        if (recipe.mealTypeId !in mealTypeIds) problems += "${recipe.id}: mealTypeId '${recipe.mealTypeId}' no existe"
        if (recipe.cuisineId !in cuisineIds) problems += "${recipe.id}: cuisineId '${recipe.cuisineId}' no existe"
        if (recipe.difficultyId !in difficultyIds) {
            problems += "${recipe.id}: difficultyId '${recipe.difficultyId}' no existe"
        }

        if (recipe.servings !in servingsRange) {
            problems += "${recipe.id}: servings ${recipe.servings} fuera de $servingsRange"
        }

        if (recipe.ingredients.isEmpty()) problems += "${recipe.id}: sin ingredientes"
        if (recipe.steps.isEmpty()) problems += "${recipe.id}: sin pasos"

        recipe.steps.forEachIndexed { index, step ->
            if (step.order != index + 1) problems += "${recipe.id}: orden de paso ${step.order} != ${index + 1}"
            if (step.minutes < 0) problems += "${recipe.id}: paso ${step.order} con minutos negativos"
        }
        val stepTotal = recipe.steps.sumOf { it.minutes }
        if (abs(stepTotal - recipe.totalMinutes) > 5) {
            problems += "${recipe.id}: suma de pasos $stepTotal no cuadra con total ${recipe.totalMinutes}"
        }

        val expectedAllergens = mutableSetOf<String>()
        recipe.ingredients.forEach { ingredient ->
            if (ingredient.quantity <= 0) {
                problems += "${recipe.id}: ingrediente '${ingredient.name}' sin cantidad"
            }
            val ingredientId = ingredient.ingredientId ?: return@forEach
            val resolved = taxonomy.ingredient(ingredientId)
            if (resolved == null) {
                problems += "${recipe.id}: ingredientId '$ingredientId' no existe"
            } else {
                expectedAllergens += resolved.allergens
            }
        }

        recipe.declaredAllergens.forEach { allergen ->
            if (allergen !in restrictionIds) {
                problems += "${recipe.id}: declaredAllergen '$allergen' no es una restriccion dura"
            }
        }
        val missing = expectedAllergens - recipe.declaredAllergens.toSet()
        if (missing.isNotEmpty()) {
            problems += "${recipe.id}: declaredAllergens omite $missing"
        }
    }

    return problems
}
