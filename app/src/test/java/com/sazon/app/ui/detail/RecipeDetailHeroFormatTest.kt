package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.ui.home.MainIngredient
import com.sazon.app.ui.home.RecipeCardFormat
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contenido y geometria del hero tipografico del Detalle
 * (`docs/specs/recipe-detail-typographic-header.md`): la linea de categoria sale de la Taxonomia
 * real (con respaldo al `id`), el titulo es el de la receta, los **tres ingredientes principales**
 * excluyen la despensa y respetan el orden de declaracion, y la geometria publicada (240/6/190/12 %)
 * es contrato. Logica pura sin Compose ni Android; casos negativos no vacios.
 */
class RecipeDetailHeroFormatTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun el_hero_resume_categoria_titulo_e_ingredientes_con_cantidad() {
        val hero = RecipeDetailHeroFormat.of(taxonomy, arrozConPollo())

        assertEquals("Arroz · Latina", hero.category)
        assertEquals("Arroz con pollo criollo", hero.title)
        assertEquals(
            listOf("Pollo", "Arroz", "Zanahoria"),
            hero.ingredients.map { it.name },
        )
        assertEquals(
            listOf("400 g", "300 g", "1 ud"),
            hero.ingredients.map { it.quantityLabel },
        )
    }

    @Test
    fun los_ingredientes_excluyen_la_despensa_y_respetan_el_orden_de_declaracion() {
        // Vector real de la semilla (Tortilla de patatas): el aceite y la sal son despensa.
        val hero = RecipeDetailHeroFormat.of(taxonomy, tortilla())

        assertEquals("Huevos y desayunos · Española", hero.category)
        assertEquals(listOf("Patata", "Huevo", "Cebolla"), hero.ingredients.map { it.name })
        assertEquals(listOf("600 g", "6 ud", "1 ud"), hero.ingredients.map { it.quantityLabel })
    }

    @Test
    fun una_cantidad_sin_unidad_es_al_gusto() {
        val hero = RecipeDetailHeroFormat.of(
            taxonomy,
            receta(
                ingredients = listOf(
                    ingredient("Pimienta", 1.0, unit = null),
                    ingredient("Perejil", 1.0, unit = null),
                ),
            ),
        )

        assertEquals(listOf("Al gusto", "Al gusto"), hero.ingredients.map { it.quantityLabel })
    }

    @Test
    fun el_contenido_del_hero_es_determinista() {
        val recipe = tortilla()

        assertEquals(
            RecipeDetailHeroFormat.of(taxonomy, recipe),
            RecipeDetailHeroFormat.of(taxonomy, recipe),
        )
    }

    @Test
    fun una_receta_vacia_no_rompe_y_no_tiene_ingredientes() {
        val hero = RecipeDetailHeroFormat.of(taxonomy, receta(ingredients = emptyList()))

        assertEquals(emptyList<MainIngredient>(), hero.ingredients)
        assertEquals("Otras recetas · Española", hero.category)
    }

    @Test
    fun con_dos_ingredientes_el_hero_pinta_los_dos() {
        val hero = RecipeDetailHeroFormat.of(
            taxonomy,
            receta(
                ingredients = listOf(
                    ingredient("Pasta", 200.0, MeasureUnit.GRAM),
                    ingredient("Tomate", 2.0, MeasureUnit.UNIT),
                ),
            ),
        )

        assertEquals(listOf("Pasta", "Tomate"), hero.ingredients.map { it.name })
        assertEquals(2, hero.ingredients.size)
    }

    @Test
    fun con_solo_despensa_el_hero_se_completa_hasta_tres() {
        val hero = RecipeDetailHeroFormat.of(
            taxonomy,
            receta(
                ingredients = listOf(
                    ingredient("Sal", 1.0, unit = null, isPantry = true),
                    ingredient("Aceite", 30.0, MeasureUnit.MILLILITER, isPantry = true),
                    ingredient("Vinagre", 15.0, MeasureUnit.MILLILITER, isPantry = true),
                ),
            ),
        )

        assertEquals(listOf("Sal", "Aceite", "Vinagre"), hero.ingredients.map { it.name })
        assertEquals(RecipeDetailHeroFormat.MAIN_INGREDIENTS, hero.ingredients.size)
    }

    @Test
    fun con_mas_de_tres_ingredientes_el_hero_no_pasa_de_tres() {
        val hero = RecipeDetailHeroFormat.of(
            taxonomy,
            receta(
                ingredients = listOf(
                    ingredient("Uno", 1.0, MeasureUnit.UNIT),
                    ingredient("Dos", 2.0, MeasureUnit.UNIT),
                    ingredient("Tres", 3.0, MeasureUnit.UNIT),
                    ingredient("Cuatro", 4.0, MeasureUnit.UNIT),
                ),
            ),
        )

        assertEquals(listOf("Uno", "Dos", "Tres"), hero.ingredients.map { it.name })
    }

    @Test
    fun una_categoria_y_cocina_desconocidas_caen_al_propio_id() {
        val hero = RecipeDetailHeroFormat.of(
            taxonomy,
            receta(categoryId = "futurista", cuisineId = "marte", ingredients = emptyList()),
        )

        assertEquals("futurista · marte", hero.category)
    }

    @Test
    fun la_geometria_del_hero_es_contrato() {
        assertEquals(240, RecipeDetailHeroFormat.HERO_HEIGHT_DP)
        assertEquals(6, RecipeDetailHeroFormat.FILLET_HEIGHT_DP)
        assertEquals(190, RecipeDetailHeroFormat.WATERMARK_DP)
        assertEquals(0.12f, RecipeDetailHeroFormat.WATERMARK_ALPHA, 0.0f)
        assertEquals(3, RecipeDetailHeroFormat.MAIN_INGREDIENTS)
        assertEquals(RecipeCardFormat.MAIN_INGREDIENTS, RecipeDetailHeroFormat.MAIN_INGREDIENTS)
        assertTrue(RecipeDetailHeroFormat.WATERMARK_ALPHA in 0f..1f)
    }

    private fun tortilla() = receta(
        title = "Tortilla de patatas",
        categoryId = "egg_breakfast",
        ingredients = listOf(
            ingredient("Patata", 600.0, MeasureUnit.GRAM),
            ingredient("Huevo", 6.0, MeasureUnit.UNIT),
            ingredient("Cebolla", 1.0, MeasureUnit.UNIT),
            ingredient("Aceite de oliva", 150.0, MeasureUnit.MILLILITER, isPantry = true),
            ingredient("Sal", 1.0, unit = null, isPantry = true),
        ),
    )

    private fun arrozConPollo() = receta(
        title = "Arroz con pollo criollo",
        categoryId = "rice",
        cuisineId = "latin",
        ingredients = listOf(
            ingredient("Pollo", 400.0, MeasureUnit.GRAM),
            ingredient("Arroz", 300.0, MeasureUnit.GRAM),
            ingredient("Zanahoria", 1.0, MeasureUnit.UNIT),
        ),
    )

    private fun receta(
        title: String = "Receta de prueba",
        categoryId: String = "other",
        cuisineId: String = "spanish",
        ingredients: List<RecipeIngredient> = emptyList(),
    ) = Recipe(
        id = "receta-1",
        source = RecipeSource.SEED,
        title = title,
        description = "Descripcion que el hero ya no pinta",
        categoryId = categoryId,
        mealTypeId = "lunch",
        cuisineId = cuisineId,
        totalMinutes = 40,
        prepMinutes = 15,
        cookMinutes = 25,
        difficultyId = "easy",
        servings = 4,
        ingredients = ingredients,
        steps = emptyList(),
    )
}

private fun ingredient(
    name: String,
    quantity: Double,
    unit: MeasureUnit?,
    isPantry: Boolean = false,
) = RecipeIngredient(
    name = name,
    quantity = quantity,
    unit = unit,
    isPantry = isPantry,
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
