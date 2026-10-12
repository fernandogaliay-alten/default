package com.sazon.app.ui.artwork

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verificacion exigida de `recipe-artwork`: el mapeo puro es **exhaustivo** contra la Taxonomia
 * **real** (`docs/domain/taxonomy.json`, leida por la propiedad de sistema `taxonomy.asset.path`),
 * **determinista** y con **fallback total**. No es un test de vacio: recorre las 18 categorias y las
 * 10 cocinas reales y exige que cada una tenga su estilo propio, sin duplicados.
 */
class RecipeArtworkStyleTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun cada_categoria_real_de_la_taxonomia_tiene_su_glifo_sin_duplicados() {
        assertEquals(18, taxonomy.recipeCategories.size)
        assertTrue(
            "La tabla del test no cubre las categorias reales: " +
                "${taxonomy.recipeCategories.map { it.id }}",
            GLYPH_BY_CATEGORY.keys.containsAll(taxonomy.recipeCategories.map { it.id }),
        )

        taxonomy.recipeCategories.forEach { category ->
            assertEquals(
                "categoria '${category.id}'",
                GLYPH_BY_CATEGORY.getValue(category.id),
                RecipeArtworkStyle.glyphFor(category.id),
            )
        }

        val glyphs = taxonomy.recipeCategories.map { RecipeArtworkStyle.glyphFor(it.id) }
        assertEquals("Hay glifos repetidos entre categorias: $glyphs", 18, glyphs.toSet().size)
        assertEquals(
            "El enum RecipeGlyph no es 1:1 con las 18 categorias",
            RecipeGlyph.entries.toSet(),
            glyphs.toSet(),
        )
    }

    @Test
    fun cada_cocina_real_de_la_taxonomia_tiene_su_acento_sin_duplicados() {
        assertEquals(10, taxonomy.cuisines.size)
        assertTrue(
            "La tabla del test no cubre las cocinas reales: ${taxonomy.cuisines.map { it.id }}",
            ACCENT_BY_CUISINE.keys.containsAll(taxonomy.cuisines.map { it.id }),
        )

        taxonomy.cuisines.forEach { cuisine ->
            assertEquals(
                "cocina '${cuisine.id}'",
                ACCENT_BY_CUISINE.getValue(cuisine.id),
                RecipeArtworkStyle.accentFor(cuisine.id),
            )
        }

        val accents = taxonomy.cuisines.map { RecipeArtworkStyle.accentFor(it.id) }
        assertEquals("Hay acentos repetidos entre cocinas: $accents", 10, accents.toSet().size)
        assertEquals(
            "El enum RecipeAccent no es 1:1 con las 10 cocinas + DEFAULT",
            RecipeAccent.entries.toSet(),
            accents.toSet() + RecipeAccent.DEFAULT,
        )
    }

    @Test
    fun el_mapeo_es_determinista() {
        taxonomy.recipeCategories.forEach { category ->
            assertEquals(
                RecipeArtworkStyle.glyphFor(category.id),
                RecipeArtworkStyle.glyphFor(category.id),
            )
        }
        taxonomy.cuisines.forEach { cuisine ->
            assertEquals(
                RecipeArtworkStyle.accentFor(cuisine.id),
                RecipeArtworkStyle.accentFor(cuisine.id),
            )
        }

        val receta = receta(categoryId = "pasta", cuisineId = "asian")
        assertEquals(RecipeArtworkStyle.of(receta), RecipeArtworkStyle.of(receta))
        assertEquals(RecipeArtworkStyle.of(receta), RecipeArtworkStyle.of("pasta", "asian"))
    }

    @Test
    fun un_id_nulo_vacio_o_desconocido_cae_al_fallback_de_plato_y_degradado_por_defecto() {
        listOf(null, "", "  ", "no-existe", "PASTA", "Pasta").forEach { desconocido ->
            assertEquals(
                "glyphFor($desconocido) deberia ser PLATE",
                RecipeGlyph.PLATE,
                RecipeArtworkStyle.glyphFor(desconocido),
            )
            assertEquals(
                "accentFor($desconocido) deberia ser DEFAULT",
                RecipeAccent.DEFAULT,
                RecipeArtworkStyle.accentFor(desconocido),
            )
        }

        assertEquals(
            RecipeStyle(glyph = RecipeGlyph.PLATE, accent = RecipeAccent.DEFAULT),
            RecipeArtworkStyle.of(categoryId = null, cuisineId = null),
        )
        assertEquals(
            RecipeStyle(glyph = RecipeGlyph.PLATE, accent = RecipeAccent.DEFAULT),
            RecipeArtworkStyle.of(categoryId = "", cuisineId = "no-existe"),
        )
    }

    @Test
    fun el_fallback_reproduce_el_estilo_de_la_categoria_other() {
        assertEquals(RecipeGlyph.PLATE, RecipeArtworkStyle.glyphFor("other"))
        assertEquals(RecipeArtworkStyle.glyphFor("other"), RecipeArtworkStyle.glyphFor("no-existe"))
    }

    @Test
    fun un_id_desconocido_no_devuelve_el_estilo_de_ninguna_categoria_o_cocina_concreta() {
        // `other` -> PLATE es el propio fallback; ninguna OTRA categoria real puede coincidir.
        val categoriasConcretas = taxonomy.recipeCategories
            .filter { it.id != "other" }
            .map { RecipeArtworkStyle.glyphFor(it.id) }
        assertFalse(
            "Un id desconocido comparte glifo con una categoria concreta",
            categoriasConcretas.contains(RecipeArtworkStyle.glyphFor("no-existe")),
        )
        assertNotEquals(RecipeArtworkStyle.glyphFor("no-existe"), RecipeArtworkStyle.glyphFor("pasta"))

        val acentosReales = taxonomy.cuisines.map { RecipeArtworkStyle.accentFor(it.id) }
        assertFalse(
            "Un id desconocido comparte acento con una cocina concreta",
            acentosReales.contains(RecipeArtworkStyle.accentFor("no-existe")),
        )
        assertNotEquals(RecipeArtworkStyle.accentFor("no-existe"), RecipeArtworkStyle.accentFor("italian"))
    }

    @Test
    fun el_estilo_de_una_receta_sale_de_su_categoria_y_su_cocina() {
        val receta = receta(categoryId = "dessert", cuisineId = "french")
        assertEquals(
            RecipeStyle(glyph = RecipeGlyph.DESSERT, accent = RecipeAccent.DEFAULT),
            RecipeArtworkStyle.of(receta),
        )
    }

    @Test
    fun una_receta_sin_clasificar_no_lanza_y_usa_el_fallback() {
        // `Recipe.categoryId`/`cuisineId` no son anulables: el caso "sin clasificar" real es un id
        // vacio o desconocido, y debe caer igualmente al plato + degradado por defecto.
        val receta = receta(categoryId = null, cuisineId = null)
        assertEquals("", receta.categoryId)
        assertEquals(
            RecipeStyle(glyph = RecipeGlyph.PLATE, accent = RecipeAccent.DEFAULT),
            RecipeArtworkStyle.of(receta),
        )

        val clasificadaRara = receta(categoryId = "categoria-futura", cuisineId = "cocina-futura")
        assertEquals(
            RecipeStyle(glyph = RecipeGlyph.PLATE, accent = RecipeAccent.DEFAULT),
            RecipeArtworkStyle.of(clasificadaRara),
        )
    }
}

/** Mapa esperado `categoryId` -> glifo (las 18 categorias reales de la Taxonomia). */
private val GLYPH_BY_CATEGORY = mapOf(
    "pasta" to RecipeGlyph.PASTA,
    "rice" to RecipeGlyph.RICE,
    "soup_cream" to RecipeGlyph.SOUP_CREAM,
    "salad" to RecipeGlyph.SALAD,
    "meat" to RecipeGlyph.MEAT,
    "poultry" to RecipeGlyph.POULTRY,
    "fish_seafood" to RecipeGlyph.FISH_SEAFOOD,
    "vegetarian" to RecipeGlyph.VEGETARIAN,
    "vegan" to RecipeGlyph.VEGAN,
    "legume_stew" to RecipeGlyph.LEGUME_STEW,
    "pizza_bread" to RecipeGlyph.PIZZA_BREAD,
    "egg_breakfast" to RecipeGlyph.EGG_BREAKFAST,
    "sandwich_wrap" to RecipeGlyph.SANDWICH_WRAP,
    "tapas_appetizer" to RecipeGlyph.TAPAS_APPETIZER,
    "dessert" to RecipeGlyph.DESSERT,
    "smoothie_drink" to RecipeGlyph.SMOOTHIE_DRINK,
    "casserole_oven" to RecipeGlyph.CASSEROLE_OVEN,
    "other" to RecipeGlyph.PLATE,
)

/** Mapa esperado `cuisineId` -> acento (las 10 cocinas reales de la Taxonomia). */
private val ACCENT_BY_CUISINE = mapOf(
    "spanish" to RecipeAccent.EMBER,
    "mediterranean" to RecipeAccent.GROVE,
    "italian" to RecipeAccent.TOMATO,
    "asian" to RecipeAccent.GINGER,
    "japanese" to RecipeAccent.SESAME,
    "indian" to RecipeAccent.CURRY,
    "mexican" to RecipeAccent.CHILI,
    "american" to RecipeAccent.HONEY,
    "middle_eastern" to RecipeAccent.CLAY,
    "latin" to RecipeAccent.ROSE,
)

private fun receta(
    categoryId: String?,
    cuisineId: String?,
) = Recipe(
    id = "receta-1",
    source = RecipeSource.SEED,
    title = "Receta de prueba",
    description = "",
    categoryId = categoryId ?: "",
    mealTypeId = "lunch",
    cuisineId = cuisineId ?: "",
    totalMinutes = 20,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 4,
    ingredients = emptyList(),
    steps = emptyList(),
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
