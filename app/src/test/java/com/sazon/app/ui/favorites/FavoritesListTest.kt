package com.sazon.app.ui.favorites

import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 5 y 6 de la spec: conteo singular/plural, los tres ordenes, busqueda por titulo
 * normalizada (mayusculas y acentos), consulta en blanco, sin coincidencias y `fromKey` desconocido.
 * Logica pura, sin Compose ni Android; la Taxonomia real se lee por `taxonomy.asset.path`.
 */
class FavoritesListTest {

    private val taxonomy: Taxonomy = taxonomiaReal()

    @Test
    fun count_label_es_singular_con_uno_y_plural_con_cero_o_mas() {
        assertEquals("1 receta", FavoritesList.countLabel(1))
        assertEquals("0 recetas", FavoritesList.countLabel(0))
        assertEquals("3 recetas", FavoritesList.countLabel(3))
    }

    @Test
    fun recent_ordena_por_saved_at_descendente() {
        val favoritos = listOf(
            favorito("a", savedAt = 100),
            favorito("b", savedAt = 300),
            favorito("c", savedAt = 200),
        )

        val visibles = FavoritesList.visible(favoritos, "", FavoritesSort.RECENT, taxonomy)

        assertEquals(listOf("b", "c", "a"), visibles.map { it.recipeId })
    }

    @Test
    fun time_ordena_por_total_minutes_ascendente() {
        val favoritos = listOf(
            favorito("a", totalMinutes = 20),
            favorito("b", totalMinutes = 40),
            favorito("c", totalMinutes = 10),
        )

        val visibles = FavoritesList.visible(favoritos, "", FavoritesSort.TIME, taxonomy)

        assertEquals(listOf("c", "a", "b"), visibles.map { it.recipeId })
    }

    @Test
    fun difficulty_ordena_por_el_indice_de_la_taxonomia() {
        val ordenTaxonomia = taxonomy.difficulties.map { it.id }
        assertTrue("La Taxonomia de prueba necesita al menos dos dificultades", ordenTaxonomia.size >= 2)
        // Se insertan al reves del orden de la Taxonomia.
        val favoritos = ordenTaxonomia.reversed().mapIndexed { index, difficultyId ->
            favorito("dif-$index", difficultyId = difficultyId)
        }

        val visibles = FavoritesList.visible(favoritos, "", FavoritesSort.DIFFICULTY, taxonomy)

        assertEquals(ordenTaxonomia, visibles.map { it.recipe.difficultyId })
    }

    @Test
    fun una_consulta_en_blanco_devuelve_todo() {
        val favoritos = listOf(
            favorito("a", title = "Arroz"),
            favorito("b", title = "Lentejas"),
        )

        val visibles = FavoritesList.visible(favoritos, "   ", FavoritesSort.RECENT, taxonomy)

        assertEquals(2, visibles.size)
        assertEquals(setOf("a", "b"), visibles.map { it.recipeId }.toSet())
    }

    @Test
    fun la_busqueda_filtra_por_titulo_ignorando_mayusculas_y_acentos() {
        val favoritos = listOf(
            favorito("salmon", title = "Salmón a la plancha"),
            favorito("pina", title = "Tarta de Piña"),
            favorito("arroz", title = "Arroz con verduras"),
        )

        val sinAcentoMinuscula = FavoritesList.visible(favoritos, "salmon", FavoritesSort.RECENT, taxonomy)
        assertEquals(listOf("salmon"), sinAcentoMinuscula.map { it.recipeId })

        val conMayusculas = FavoritesList.visible(favoritos, "PINA", FavoritesSort.RECENT, taxonomy)
        assertEquals(listOf("pina"), conMayusculas.map { it.recipeId })
    }

    @Test
    fun una_receta_que_solo_coincide_por_descripcion_no_entra() {
        val favoritos = listOf(
            favorito("solo-descripcion", title = "Arroz blanco", description = "con salmón ahumado"),
        )

        val visibles = FavoritesList.visible(favoritos, "salmon", FavoritesSort.RECENT, taxonomy)

        assertTrue("La busqueda es solo por titulo (caso negativo)", visibles.isEmpty())
    }

    @Test
    fun sin_coincidencias_devuelve_vacio() {
        val favoritos = listOf(favorito("a", title = "Arroz"))

        val visibles = FavoritesList.visible(favoritos, "zzz", FavoritesSort.RECENT, taxonomy)

        assertTrue(visibles.isEmpty())
    }

    @Test
    fun from_key_desconocido_degrada_a_recent() {
        FavoritesSort.entries.forEach { entry ->
            assertEquals(entry, FavoritesSort.fromKey(entry.key))
        }
        assertEquals(FavoritesSort.RECENT, FavoritesSort.fromKey("desconocido"))

        FavoritesViewMode.entries.forEach { mode ->
            assertEquals(mode, FavoritesViewMode.fromKey(mode.key))
        }
        assertEquals(FavoritesViewMode.LIST, FavoritesViewMode.fromKey("desconocido"))
    }
}

private fun favorito(
    id: String,
    title: String = "Receta $id",
    description: String = "Descripcion de $id.",
    savedAt: Long = 0L,
    totalMinutes: Int = 20,
    difficultyId: String = "easy",
): Favorite = Favorite(
    recipe = Recipe(
        id = id,
        source = RecipeSource.SEED,
        title = title,
        description = description,
        categoryId = "rice",
        mealTypeId = "lunch",
        cuisineId = "spanish",
        totalMinutes = totalMinutes,
        prepMinutes = 5,
        cookMinutes = (totalMinutes - 5).coerceAtLeast(0),
        difficultyId = difficultyId,
        servings = 2,
        ingredients = emptyList(),
        steps = emptyList(),
    ),
    savedAt = savedAt,
)

private fun taxonomiaReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
