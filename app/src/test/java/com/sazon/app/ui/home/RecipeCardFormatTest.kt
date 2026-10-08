package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Formato de la tarjeta de receta de `docs/specs/home-offline-blocks.md`: la dificultad sale de la
 * Taxonomia real (con respaldo al id) y la pildora de coincidencia solo existe con `matchScore`.
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
}

private fun receta(
    difficultyId: String,
    totalMinutes: Int = 20,
    servings: Int = 4,
) = Recipe(
    id = "receta-1",
    source = RecipeSource.SEED,
    title = "Receta de prueba",
    description = "",
    categoryId = "other",
    mealTypeId = "lunch",
    cuisineId = "spanish",
    totalMinutes = totalMinutes,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = difficultyId,
    servings = servings,
    ingredients = emptyList(),
    steps = emptyList(),
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
