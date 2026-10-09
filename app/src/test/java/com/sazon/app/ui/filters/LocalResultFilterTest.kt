package com.sazon.app.ui.filters

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Ingredient
import com.sazon.app.domain.taxonomy.Servings
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-5 y claves de `docs/specs/local-result-filters.md`: filtrado local puro (categoria,
 * "Rapido" y "Vegetariano") sobre recetas ya cargadas. Logica pura, sin Compose ni Android; la
 * Taxonomia real se lee por la propiedad de sistema `taxonomy.asset.path`.
 */
class LocalResultFilterTest {

    private val taxonomy: Taxonomy = taxonomiaReal()

    @Test
    fun escenario_1_all_no_filtra_y_preserva_el_orden() {
        val recetas = listOf(
            receta("paste", categoryId = "pasta", totalMinutes = 20),
            receta("arroz", categoryId = "rice", totalMinutes = 40),
            receta("ensalada", categoryId = "salad", totalMinutes = 10),
        )

        val resultado = LocalResultFilters.visible(recetas, LocalResultFilter.All, taxonomy)

        assertSame("All debe devolver la misma lista", recetas, resultado)
        assertEquals(listOf("paste", "arroz", "ensalada"), resultado.map { it.id })
    }

    @Test
    fun escenario_2_categoria_filtra_por_category_id_y_la_desconocida_vacia() {
        val recetas = listOf(
            receta("paste", categoryId = "pasta"),
            receta("arroz", categoryId = "rice"),
            receta("otra-pasta", categoryId = "pasta"),
        )

        val soloPasta = LocalResultFilters.visible(
            recetas,
            LocalResultFilter.Category("pasta"),
            taxonomy,
        )
        assertEquals(listOf("paste", "otra-pasta"), soloPasta.map { it.id })

        val desconocida = LocalResultFilters.visible(
            recetas,
            LocalResultFilter.Category("no-existe"),
            taxonomy,
        )
        assertTrue("Una categoria inexistente devuelve vacio, no todas", desconocida.isEmpty())
    }

    @Test
    fun escenario_3_rapido_incluye_la_frontera_30_y_excluye_31() {
        val recetas = listOf(
            receta("r20", totalMinutes = 20),
            receta("r30", totalMinutes = 30),
            receta("r31", totalMinutes = 31),
        )

        val rapido = LocalResultFilters.visible(recetas, LocalResultFilter.Fast, taxonomy)

        assertEquals("La frontera <= 30 se incluye", listOf("r20", "r30"), rapido.map { it.id })
        assertEquals(30, LocalResultFilters.FAST_MAX_MINUTES)
    }

    @Test
    fun escenario_4_vegetariano_excluye_tags_animales_e_incluye_la_vegetal() {
        val animales = listOf(
            receta("pollo", ingredientIds = listOf("pollo")), // poultry
            receta("salmon", ingredientIds = listOf("salmon")), // fish
            receta("gamba", ingredientIds = listOf("gamba")), // seafood
            receta("ternera", ingredientIds = listOf("ternera")), // meat
        )
        val vegetal = receta("vegetal", ingredientIds = listOf("tofu", "brocoli"))
        val recetas = animales + vegetal

        val resultado = LocalResultFilters.visible(recetas, LocalResultFilter.Vegetarian, taxonomy)
        val ids = resultado.map { it.id }

        assertEquals("Solo pasa la vegetal", listOf("vegetal"), ids)
        val idsAnimales = animales.map { it.id }
        assertTrue(
            "Ninguna receta animal debe colarse (caso negativo)",
            ids.none { it in idsAnimales },
        )
    }

    @Test
    fun escenario_5_ingrediente_libre_no_se_excluye_por_tags() {
        // "Pollo" como texto libre (ingredientId = null) no resuelve en la Taxonomia: no se excluye.
        val librer = receta("libre", ingredientIds = listOf(null))
        // El mismo caso con id real si se excluye, para que el test no pase en vacio.
        val conId = receta("con-id", ingredientIds = listOf("pollo"))

        val resultado = LocalResultFilters.visible(
            listOf(librer, conId),
            LocalResultFilter.Vegetarian,
            taxonomy,
        )

        assertEquals("El ingrediente libre no se detecta; el resuelto si", listOf("libre"), resultado.map { it.id })
    }

    @Test
    fun escenario_6_claves_hacen_round_trip_y_los_derivados_se_marcan() {
        assertEquals(LocalResultFilter.All, LocalResultFilters.fromKey(LocalResultFilter.All.key))
        assertEquals(
            LocalResultFilter.Category("rice"),
            LocalResultFilters.fromKey(LocalResultFilter.Category("rice").key),
        )
        assertEquals(LocalResultFilter.Fast, LocalResultFilters.fromKey(LocalResultFilter.Fast.key))
        assertEquals(
            LocalResultFilter.Vegetarian,
            LocalResultFilters.fromKey(LocalResultFilter.Vegetarian.key),
        )
        assertEquals(
            "Una clave desconocida degrada a Todas",
            LocalResultFilter.All,
            LocalResultFilters.fromKey("desconocido"),
        )

        assertFalse(LocalResultFilters.isDerived(LocalResultFilter.All))
        assertFalse(LocalResultFilters.isDerived(LocalResultFilter.Category("pasta")))
        assertTrue(LocalResultFilters.isDerived(LocalResultFilter.Fast))
        assertTrue(LocalResultFilters.isDerived(LocalResultFilter.Vegetarian))

        assertEquals("Todas", LocalResultFilters.label(taxonomy, LocalResultFilter.All))
        assertEquals("Pasta", LocalResultFilters.label(taxonomy, LocalResultFilter.Category("pasta")))
        assertEquals(
            "Etiqueta de respaldo al id",
            "no-existe",
            LocalResultFilters.label(taxonomy, LocalResultFilter.Category("no-existe")),
        )
        assertEquals("Rápido", LocalResultFilters.label(taxonomy, LocalResultFilter.Fast))
        assertEquals("Vegetariano", LocalResultFilters.label(taxonomy, LocalResultFilter.Vegetarian))
    }

    @Test
    fun categories_in_devuelve_presentes_sin_duplicados_en_orden_de_taxonomia() {
        val recetas = listOf(
            receta("a", categoryId = "rice"),
            receta("b", categoryId = "pasta"),
            receta("c", categoryId = "rice"),
            receta("d", categoryId = "no-existe"),
            receta("e", categoryId = "vegetarian"),
        )

        val categorias = LocalResultFilters.categoriesIn(recetas, taxonomy).map { it.id }

        assertEquals(listOf("pasta", "rice", "vegetarian"), categorias)
        assertEquals("Sin duplicados", categorias.distinct(), categorias)
    }

    @Test
    fun vegetarian_tags_usa_el_patron_real_y_tiene_respaldo_defensivo() {
        assertEquals(
            "La fuente unica de verdad son los excludedTags del patron vegetariano",
            setOf("meat", "poultry", "fish", "seafood"),
            LocalResultFilters.vegetarianTags(taxonomy),
        )

        val sinPatron = taxonomiaSinPatronVegetariano()
        assertEquals(
            "Sin patron vegetariano se usa el respaldo constante",
            LocalResultFilters.FALLBACK_VEGETARIAN_TAGS,
            LocalResultFilters.vegetarianTags(sinPatron),
        )
        // Y el filtro sigue excluyendo por tags con el respaldo.
        assertEquals(
            emptyList<String>(),
            LocalResultFilters.visible(
                listOf(receta("pollo", ingredientIds = listOf("pollo"))),
                LocalResultFilter.Vegetarian,
                sinPatron,
            ).map { it.id },
        )
    }
}

private fun receta(
    id: String,
    categoryId: String = "other",
    totalMinutes: Int = 20,
    ingredientIds: List<String?> = emptyList(),
) = Recipe(
    id = id,
    source = RecipeSource.SEED,
    title = "Receta $id",
    description = "",
    categoryId = categoryId,
    mealTypeId = "lunch",
    cuisineId = "spanish",
    totalMinutes = totalMinutes,
    prepMinutes = 5,
    cookMinutes = (totalMinutes - 5).coerceAtLeast(0),
    difficultyId = "easy",
    servings = 2,
    ingredients = ingredientIds.mapIndexed { index, ingredientId ->
        RecipeIngredient(
            name = ingredientId ?: "Libre $index",
            quantity = 100.0,
            unit = MeasureUnit.GRAM,
            ingredientId = ingredientId,
        )
    },
    steps = emptyList(),
)

private fun taxonomiaReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}

private fun taxonomiaSinPatronVegetariano(): Taxonomy = Taxonomy(
    dietPatterns = emptyList(),
    hardRestrictions = emptyList(),
    softPreferences = emptyList(),
    mealTypes = emptyList(),
    cuisines = emptyList(),
    timeBuckets = emptyList(),
    difficulties = emptyList(),
    servings = Servings(min = 1, max = 12, default = 2),
    avatarColors = emptyList(),
    ingredients = listOf(
        Ingredient(
            id = "pollo",
            label = "Pollo",
            category = "poultry",
            icon = "chicken",
            allergens = emptyList(),
            tags = listOf("poultry"),
        ),
    ),
    recipeCategories = emptyList(),
    categoryFallbackIcon = emptyMap(),
)
