package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Meta del hero (`docs/specs/recipe-detail-screen.md`, `RecipeDetailFormat`): cuatro pares con la
 * dificultad real (y respaldo al `id`) y `Calorias = "—"` sin nutricion, sin inventar cifras. Logica
 * pura sin Compose ni Android sobre la Taxonomia real.
 */
class RecipeDetailFormatTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun los_cuatro_pares_usan_la_etiqueta_real_de_dificultad() {
        val recipe = receta(difficultyId = "easy", nutrition = Nutrition(320, 14, 24, 19))

        val meta = RecipeDetailFormat.metaEntries(taxonomy, recipe)

        assertEquals(listOf("Tiempo", "Dificultad", "Comensales", "Calorías"), meta.map { it.label })
        assertEquals(
            listOf("40 min", "Fácil", "4", "320 kcal"),
            meta.map { it.value },
        )
    }

    @Test
    fun una_dificultad_desconocida_cae_al_propio_id() {
        val meta = RecipeDetailFormat.metaEntries(taxonomy, receta(difficultyId = "experto"))

        assertEquals("experto", meta.first { it.label == "Dificultad" }.value)
    }

    @Test
    fun sin_nutricion_las_calorias_son_un_guion_sin_inventar_cifras() {
        val meta = RecipeDetailFormat.metaEntries(taxonomy, receta(nutrition = null))

        assertEquals(RecipeDetailFormat.NO_NUTRITION, meta.first { it.label == "Calorías" }.value)
        assertEquals("—", meta.first { it.label == "Calorías" }.value)
    }

    @Test
    fun la_caption_del_ajustador_es_estable() {
        assertEquals("Cantidades ajustadas automáticamente", RecipeDetailFormat.ADJUSTED_CAPTION)
    }

    private fun receta(
        difficultyId: String = "easy",
        nutrition: Nutrition? = null,
    ): Recipe = Recipe(
        id = "receta-1",
        source = RecipeSource.SEED,
        title = "Receta de prueba",
        description = "",
        categoryId = "other",
        mealTypeId = "lunch",
        cuisineId = "spanish",
        totalMinutes = 40,
        prepMinutes = 15,
        cookMinutes = 25,
        difficultyId = difficultyId,
        servings = 4,
        ingredients = emptyList(),
        steps = emptyList(),
        nutrition = nutrition,
    )
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
