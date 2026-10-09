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
 * Escenario 5 de `docs/specs/home-offline-blocks.md` (estado vacio del bloque "Tus recetas") y
 * `HomeRecipes.filteredBy` de `docs/specs/local-result-filters.md`: el filtro se aplica a cada grupo
 * de forma independiente. Logica pura.
 */
class HomeRecipesTest {

    private val taxonomy: Taxonomy = taxonomiaReal()

    @Test
    fun sin_guardadas_ni_abiertas_el_bloque_esta_vacio() {
        assertTrue(HomeRecipes().isEmpty)
        assertTrue(HomeRecipes(saved = emptyList(), recentlyOpened = emptyList()).isEmpty)
    }

    @Test
    fun con_una_receta_guardada_el_bloque_no_esta_vacio() {
        val bloque = HomeRecipes(saved = listOf(receta("seed-1")), recentlyOpened = emptyList())

        assertFalse(bloque.isEmpty)
    }

    @Test
    fun con_una_receta_abierta_hace_poco_el_bloque_no_esta_vacio() {
        val bloque = HomeRecipes(saved = emptyList(), recentlyOpened = listOf(receta("ai-1")))

        assertFalse(bloque.isEmpty)
    }

    @Test
    fun filtered_by_categoria_filtra_cada_grupo_de_forma_independiente() {
        val bloque = HomeRecipes(
            saved = listOf(
                receta("s-pasta", categoryId = "pasta"),
                receta("s-arroz", categoryId = "rice"),
            ),
            recentlyOpened = listOf(receta("r-pasta", categoryId = "pasta")),
        )

        val filtrado = bloque.filteredBy(LocalResultFilter.Category("pasta"), taxonomy)

        assertEquals(listOf("s-pasta"), filtrado.saved.map { it.id })
        assertEquals(listOf("r-pasta"), filtrado.recentlyOpened.map { it.id })
    }

    @Test
    fun filtered_by_rapido_respeta_la_frontera_30_en_cada_grupo() {
        val bloque = HomeRecipes(
            saved = listOf(
                receta("s30", totalMinutes = 30),
                receta("s31", totalMinutes = 31),
            ),
            recentlyOpened = listOf(
                receta("r10", totalMinutes = 10),
                receta("r45", totalMinutes = 45),
            ),
        )

        val filtrado = bloque.filteredBy(LocalResultFilter.Fast, taxonomy)

        assertEquals(listOf("s30"), filtrado.saved.map { it.id })
        assertEquals(listOf("r10"), filtrado.recentlyOpened.map { it.id })
    }

    @Test
    fun filtered_by_all_conserva_los_grupos_sin_tocarlos() {
        val bloque = HomeRecipes(
            saved = listOf(receta("s1"), receta("s2")),
            recentlyOpened = listOf(receta("r1")),
        )

        val filtrado = bloque.filteredBy(LocalResultFilter.All, taxonomy)

        assertEquals(bloque.saved, filtrado.saved)
        assertEquals(bloque.recentlyOpened, filtrado.recentlyOpened)
    }

    @Test
    fun filtered_by_sin_coincidencias_deja_los_dos_grupos_vacios() {
        val bloque = HomeRecipes(
            saved = listOf(receta("s-pasta", categoryId = "pasta")),
            recentlyOpened = listOf(receta("r-pasta", categoryId = "pasta")),
        )

        val filtrado = bloque.filteredBy(LocalResultFilter.Category("rice"), taxonomy)

        assertTrue(filtrado.isEmpty)
    }

    @Test
    fun filtered_by_no_altera_el_bloque_original() {
        val bloque = HomeRecipes(
            saved = listOf(receta("s-pasta", categoryId = "pasta")),
            recentlyOpened = emptyList(),
        )

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
