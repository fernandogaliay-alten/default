package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Formatos puros de la tarjeta de receta (`docs/specs/recipe-card-typographic.md`): la dificultad sale
 * de la Taxonomia real (con respaldo al id), la linea de categoria combina categoria y cocina, los
 * **tres ingredientes principales** excluyen la despensa y se completan con ella, y la coincidencia
 * solo existe con `matchScore` (compacta en la tarjeta estrecha).
 */
class RecipeCardFormatTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun la_linea_de_metadatos_usa_la_etiqueta_real_de_dificultad() {
        val linea = RecipeCardFormat.metaLine(taxonomy, receta(difficultyId = "easy", totalMinutes = 25, servings = 2))

        assertEquals("25 min · Fácil · 2 pers.", linea)
    }

    @Test
    fun una_dificultad_desconocida_cae_al_propio_id() {
        val linea = RecipeCardFormat.metaLine(taxonomy, receta(difficultyId = "experto"))

        assertEquals("20 min · experto · 4 pers.", linea)
    }

    @Test
    fun sin_puntuacion_no_hay_pildora_de_coincidencia() {
        assertNull(RecipeCardFormat.matchBadge(null))
    }

    @Test
    fun con_puntuacion_la_pildora_muestra_el_porcentaje() {
        assertEquals("92% coincidencia", RecipeCardFormat.matchBadge(92))
    }

    @Test
    fun la_coincidencia_compacta_no_repite_la_palabra() {
        assertEquals("92%", RecipeCardFormat.matchBadge(92, compact = true))
        assertNull(RecipeCardFormat.matchBadge(null, compact = true))
    }

    @Test
    fun la_linea_de_categoria_combina_categoria_y_cocina_de_la_taxonomia() {
        val linea = RecipeCardFormat.categoryLine(
            taxonomy,
            receta(difficultyId = "easy", categoryId = "rice", cuisineId = "latin"),
        )

        assertEquals("Arroz · Latina", linea)
    }

    @Test
    fun una_categoria_o_cocina_desconocida_cae_al_propio_id() {
        val linea = RecipeCardFormat.categoryLine(
            taxonomy,
            receta(difficultyId = "easy", categoryId = "futurista", cuisineId = "marte"),
        )

        assertEquals("futurista · marte", linea)
    }

    @Test
    fun los_ingredientes_principales_excluyen_la_despensa_y_respetan_el_orden() {
        // Vector real de la semilla (Tortilla de patatas): la sal y el aceite son despensa.
        val principales = RecipeCardFormat.mainIngredients(tortilla())

        assertEquals(
            listOf("Patata", "Huevo", "Cebolla"),
            principales.map { it.name },
        )
        assertEquals(
            listOf("600 g", "6 ud", "1 ud"),
            principales.map { it.quantityLabel },
        )
    }

    @Test
    fun con_menos_de_tres_ingredientes_sin_despensa_se_completa_con_la_despensa_en_su_orden() {
        val recipe = receta(
            difficultyId = "easy",
            ingredients = listOf(
                ingredient("Tomate", 2.0, MeasureUnit.UNIT),
                ingredient("Cebolla", 1.0, MeasureUnit.UNIT),
                ingredient("Sal", 1.0, unit = null, isPantry = true),
                ingredient("Aceite", 30.0, MeasureUnit.MILLILITER, isPantry = true),
                ingredient("Vinagre", 15.0, MeasureUnit.MILLILITER, isPantry = true),
            ),
        )

        assertEquals(
            listOf("Tomate", "Cebolla", "Sal"),
            RecipeCardFormat.mainIngredients(recipe).map { it.name },
        )
    }

    @Test
    fun una_receta_con_menos_de_tres_ingredientes_devuelve_los_que_haya() {
        val recipe = receta(
            difficultyId = "easy",
            ingredients = listOf(
                ingredient("Pasta", 200.0, MeasureUnit.GRAM),
                ingredient("Sal", 1.0, unit = null, isPantry = true),
            ),
        )

        assertEquals(
            listOf("Pasta", "Sal"),
            RecipeCardFormat.mainIngredients(recipe).map { it.name },
        )
    }

    @Test
    fun la_seleccion_de_ingredientes_es_determinista() {
        val recipe = tortilla()

        assertEquals(
            RecipeCardFormat.mainIngredients(recipe),
            RecipeCardFormat.mainIngredients(recipe),
        )
    }

    @Test
    fun una_receta_sin_ingredientes_no_tiene_ingredientes_principales() {
        val recipe = receta(difficultyId = "easy", ingredients = emptyList())

        assertEquals(emptyList<MainIngredient>(), RecipeCardFormat.mainIngredients(recipe))
        assertEquals("", RecipeCardFormat.ingredientsLine(recipe))
    }

    @Test
    fun la_linea_de_ingredientes_une_los_nombres_con_punto_medio() {
        assertEquals("Patata · Huevo · Cebolla", RecipeCardFormat.ingredientsLine(tortilla()))
    }

    private fun tortilla() = receta(
        difficultyId = "easy",
        title = "Tortilla de patatas",
        ingredients = listOf(
            ingredient("Patata", 600.0, MeasureUnit.GRAM),
            ingredient("Huevo", 6.0, MeasureUnit.UNIT),
            ingredient("Cebolla", 1.0, MeasureUnit.UNIT),
            ingredient("Aceite de oliva", 150.0, MeasureUnit.MILLILITER, isPantry = true),
            ingredient("Sal", 1.0, unit = null, isPantry = true),
        ),
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

private fun receta(
    difficultyId: String,
    totalMinutes: Int = 20,
    servings: Int = 4,
    title: String = "Receta de prueba",
    categoryId: String = "other",
    cuisineId: String = "spanish",
    ingredients: List<RecipeIngredient> = emptyList(),
) = Recipe(
    id = "receta-1",
    source = RecipeSource.SEED,
    title = title,
    description = "",
    categoryId = categoryId,
    mealTypeId = "lunch",
    cuisineId = cuisineId,
    totalMinutes = totalMinutes,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = difficultyId,
    servings = servings,
    ingredients = ingredients,
    steps = emptyList(),
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
