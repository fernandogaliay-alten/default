package com.sazon.app.ui.home

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.ui.filters.LocalResultFilter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estado vacio del bloque "Tus recetas" (bloque 5 de la Home) y `HomeRecipes.filteredBy`: el filtro
 * local se aplica al unico grupo (`saved`, alimentado por los Favoritos). Logica pura.
 */
class HomeRecipesTest {

    private val taxonomy: Taxonomy = taxonomiaReal()

    @Test
    fun sin_guardadas_el_bloque_esta_vacio() {
        assertTrue(HomeRecipes().isEmpty)
        assertTrue(HomeRecipes(saved = emptyList()).isEmpty)
    }

    @Test
    fun con_una_receta_guardada_el_bloque_no_esta_vacio() {
        val bloque = HomeRecipes(saved = listOf(receta("seed-1")))

        assertFalse(bloque.isEmpty)
    }

    @Test
    fun filtered_by_categoria_filtra_las_guardadas() {
        val bloque = HomeRecipes(
            saved = listOf(
                receta("s-pasta", categoryId = "pasta"),
                receta("s-arroz", categoryId = "rice"),
            ),
        )

        val filtrado = bloque.filteredBy(LocalResultFilter.Category("pasta"), taxonomy)

        assertEquals(listOf("s-pasta"), filtrado.saved.map { it.id })
    }

    @Test
    fun filtered_by_rapido_respeta_la_frontera_30_en_las_guardadas() {
        val bloque = HomeRecipes(
            saved = listOf(
                receta("s30", totalMinutes = 30),
                receta("s31", totalMinutes = 31),
            ),
        )

        val filtrado = bloque.filteredBy(LocalResultFilter.Fast, taxonomy)

        assertEquals(listOf("s30"), filtrado.saved.map { it.id })
    }

    @Test
    fun filtered_by_all_conserva_las_guardadas_sin_tocarlas() {
        val bloque = HomeRecipes(saved = listOf(receta("s1"), receta("s2")))

        val filtrado = bloque.filteredBy(LocalResultFilter.All, taxonomy)

        assertEquals(bloque.saved, filtrado.saved)
    }

    @Test
    fun filtered_by_sin_coincidencias_deja_el_grupo_vacio() {
        val bloque = HomeRecipes(saved = listOf(receta("s-pasta", categoryId = "pasta")))

        val filtrado = bloque.filteredBy(LocalResultFilter.Category("rice"), taxonomy)

        assertTrue(filtrado.isEmpty)
    }

    @Test
    fun filtered_by_no_altera_el_bloque_original() {
        val bloque = HomeRecipes(saved = listOf(receta("s-pasta", categoryId = "pasta")))

        bloque.filteredBy(LocalResultFilter.Category("rice"), taxonomy)

        assertEquals("filteredBy no muta el original", listOf("s-pasta"), bloque.saved.map { it.id })
    }
}

private fun receta(
    id: String,
    categoryId: String = "other",
    totalMinutes: Int = 20,
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
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = emptyList(),
)

private fun taxonomiaReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
