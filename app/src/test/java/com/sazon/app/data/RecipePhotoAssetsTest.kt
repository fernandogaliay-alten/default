package com.sazon.app.data

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.SeedRecipeJson
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verificacion exigida de `recipe-photos`: la **cadena de respaldo** (foto propia de la receta →
 * foto de su categoria → foto por defecto → `null`) y la **existencia real** de los 31 assets
 * empaquetados en `app/src/main/assets/img/recipes/`.
 *
 * El caso de exhaustividad **no pasa en vacio**: itera la Taxonomia **real**
 * (`docs/domain/taxonomy.json` via `taxonomy.asset.path`) y el catalogo semilla **real**
 * (`app/src/main/assets/seed-recipes.json` via `seed.recipes.asset.path`) —mismo patron que
 * `TaxonomyIntegrityTest`/`SeedRecipeCatalogTest`— y el directorio de fotos se publica con
 * `recipe.photos.assets.dir` desde `app/build.gradle.kts`.
 */
class RecipePhotoAssetsTest {

    private val assetsDir = File(assetsDirPath())
    private val realExists: (String) -> Boolean = { path -> photoFile(path).isFile }
    private val taxonomy = TaxonomyJson.parse(File(taxonomyJsonPath()).readText())
    private val seedRecipes = SeedRecipeJson.parse(File(seedRecipesJsonPath()).readText()).recipes

    // --- Escenario 1: foto propia de la receta semilla -------------------------------------------

    @Test
    fun cada_receta_semilla_resuelve_su_foto_propia() {
        assertEquals("Se esperaban las 12 recetas semilla", 12, seedRecipes.size)
        seedRecipes.forEach { recipe ->
            val resolved = RecipePhotoAssets.resolve(recipe.id, recipe.categoryId, realExists)

            assertNotNull("La receta semilla '${recipe.id}' deberia resolver su foto propia", resolved)
            assertEquals(
                "La receta semilla '${recipe.id}' deberia resolver su foto propia",
                "${RecipePhotoAssets.ROOT}/seed/${recipe.id}",
                resolved!!.substringBeforeLast('.'),
            )
            assertTrue(
                "'$resolved' deberia existir y no estar vacio",
                photoFile(resolved!!).length() > 1024,
            )
        }
    }

    // --- Escenario 2: respaldo a la foto de la categoria (recetas de IA) --------------------------

    @Test
    fun una_receta_ai_sin_foto_propia_cae_a_la_foto_de_su_categoria() {
        val receta = receta(id = "ai-0123456789abcdef", categoryId = "rice")

        val resolved = RecipePhotoAssets.resolve(receta.id, receta.categoryId, realExists)

        assertEquals("${RecipePhotoAssets.ROOT}/category/cat-rice.jpg", resolved)
        assertTrue(photoFile(resolved!!).length() > 1024)
    }

    // --- Escenario 3: respaldo parcial semilla → categoria ---------------------------------------

    @Test
    fun si_falta_la_foto_propia_de_una_semilla_la_cadena_sigue_en_la_categoria() {
        val sinFotoPropia: (String) -> Boolean = { path ->
            if (path.startsWith("${RecipePhotoAssets.ROOT}/seed/")) {
                false
            } else {
                photoFile(path).isFile
            }
        }

        val resolved = RecipePhotoAssets.resolve("seed-tortilla-patatas", "egg_breakfast", sinFotoPropia)

        assertEquals("${RecipePhotoAssets.ROOT}/category/cat-egg_breakfast.jpg", resolved)
    }

    // --- Escenario 4: por defecto y, si no hay nada, `null` --------------------------------------

    @Test
    fun una_categoria_nula_vacia_o_fuera_de_las_18_cae_a_la_foto_por_defecto() {
        assertEquals(18, RecipePhotoAssets.CATEGORY_IDS.size)
        listOf(null, "", "  ", "categoria-inventada").forEach { categoria ->
            val resolved = RecipePhotoAssets.resolve("ai-sin-foto", categoria, realExists)
            assertEquals(
                "categoryId=$categoria deberia caer a la foto por defecto",
                "${RecipePhotoAssets.ROOT}/default.jpg",
                resolved,
            )
        }
        // `other` SI es una de las 18 categorias reales: su foto existe y no se confunde con `default`.
        assertEquals(
            "${RecipePhotoAssets.ROOT}/category/cat-other.jpg",
            RecipePhotoAssets.resolve("ai-otro", "other", realExists),
        )
    }

    @Test
    fun sin_ningun_asset_utilizable_la_resolucion_devuelve_null_sin_lanzar() {
        val nada: (String) -> Boolean = { false }

        assertNull(RecipePhotoAssets.resolve("seed-tortilla-patatas", "egg_breakfast", nada))
        assertNull(RecipePhotoAssets.resolve("ai-sin-foto", "rice", nada))
        assertNull(RecipePhotoAssets.resolve("", null, nada))
    }

    // --- Escenario 5 (verificacion exigida): exhaustividad de assets -----------------------------

    @Test
    fun cada_receta_semilla_cada_categoria_y_default_resuelven_un_asset_existente_y_no_vacio() {
        // Las 12 recetas semilla, del catalogo REAL.
        assertEquals("Se esperaban 12 recetas semilla en el catalogo real", 12, seedRecipes.size)
        val idsSemilla = seedRecipes.map { it.id }
        assertEquals("Los ids semilla deben ser unicos", idsSemilla.size, idsSemilla.toSet().size)
        idsSemilla.forEach { id ->
            val resolved = RecipePhotoAssets.resolve(id, null, realExists)
            assertNotNull("La receta semilla '$id' no resuelve ningun asset", resolved)
            assertTrue(
                "'$resolved' ($id) deberia existir y no estar vacio",
                photoFile(resolved!!).length() > 1024,
            )
        }

        // Las 18 categorias REALES de la Taxonomia.
        assertEquals(18, taxonomy.recipeCategories.size)
        taxonomy.recipeCategories.forEach { category ->
            val resolved = RecipePhotoAssets.resolve("ai-sin-foto", category.id, realExists)
            assertNotNull("La categoria '${category.id}' no resuelve ningun asset", resolved)
            assertEquals(
                "${RecipePhotoAssets.ROOT}/category/cat-${category.id}",
                resolved!!.substringBeforeLast('.'),
            )
            assertTrue(
                "'$resolved' (${category.id}) deberia existir y no estar vacio",
                photoFile(resolved!!).length() > 1024,
            )
        }

        // La foto por defecto.
        val porDefecto = RecipePhotoAssets.resolve("ai-sin-foto", null, realExists)
        assertNotNull("El caso por defecto no resuelve ningun asset", porDefecto)
        assertEquals("${RecipePhotoAssets.ROOT}/default.jpg", porDefecto)
        assertTrue(photoFile(porDefecto!!).length() > 1024)
    }

    // --- Escenario 8: inventario y presupuesto --------------------------------------------------

    @Test
    fun el_inventario_empaquetado_tiene_los_31_ficheros_dentro_del_presupuesto() {
        val semillas = File(assetsDir, "seed").listFiles()?.filter { it.isFile }.orEmpty()
        val categorias = File(assetsDir, "category").listFiles()?.filter { it.isFile }.orEmpty()
        val porDefecto = assetsDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("${RecipePhotoAssets.DEFAULT_KEY}.") }
            .orEmpty()

        assertEquals("Se esperaban 12 fotos seed/", 12, semillas.size)
        assertEquals("Se esperaban 18 fotos category/", 18, categorias.size)
        assertEquals("Se esperaba 1 foto default", 1, porDefecto.size)

        val bytes = (semillas + categorias + porDefecto).sumOf { it.length() }
        assertTrue(
            "Las fotos suman $bytes bytes, por encima del presupuesto de 8 MB",
            bytes <= 8 * 1024 * 1024,
        )
    }

    // --- Determinismo y candidatos ---------------------------------------------------------------

    @Test
    fun la_resolucion_es_determinista() {
        val primera = RecipePhotoAssets.resolve("seed-pasta-tomate", "pasta", realExists)
        val segunda = RecipePhotoAssets.resolve("seed-pasta-tomate", "pasta", realExists)

        assertEquals(primera, segunda)
        assertNotNull(primera)
    }

    @Test
    fun los_candidatos_van_de_la_foto_propia_a_la_por_defecto() {
        assertEquals(
            listOf(
                "${RecipePhotoAssets.ROOT}/seed/seed-tortilla-patatas",
                "${RecipePhotoAssets.ROOT}/category/cat-egg_breakfast",
                "${RecipePhotoAssets.ROOT}/default",
            ),
            RecipePhotoAssets.candidates("seed-tortilla-patatas", "egg_breakfast"),
        )
    }

    @Test
    fun un_id_y_una_categoria_inventados_no_caen_a_una_categoria_real() {
        val candidatos = RecipePhotoAssets.candidates("id-inventado", "cat-no-existe")

        assertFalse(
            "Un categoryId desconocido no debe producir candidato de categoria: $candidatos",
            candidatos.any { it.startsWith("${RecipePhotoAssets.ROOT}/category/") },
        )
        assertEquals(
            listOf("${RecipePhotoAssets.ROOT}/seed/id-inventado", "${RecipePhotoAssets.ROOT}/default"),
            candidatos,
        )
        // Con un predicado que no encuentra nada, la resolucion es `null` (el glifo es el ultimo
        // recurso) y nunca se inventa la ruta de una categoria real.
        assertNull(RecipePhotoAssets.resolve("id-inventado", "cat-no-existe") { false })
    }

    @Test
    fun el_predicado_de_existencia_falso_no_deja_pasar_ninguna_ruta() {
        val vistas = mutableListOf<String>()

        val resolved = RecipePhotoAssets.resolve("seed-pasta-tomate", "pasta") { path ->
            vistas += path
            false
        }

        assertNull(resolved)
        assertEquals(
            "Se debe probar cada candidato y cada extension en orden",
            listOf(
                "img/recipes/seed/seed-pasta-tomate.webp",
                "img/recipes/seed/seed-pasta-tomate.jpg",
                "img/recipes/seed/seed-pasta-tomate.png",
                "img/recipes/category/cat-pasta.webp",
                "img/recipes/category/cat-pasta.jpg",
                "img/recipes/category/cat-pasta.png",
                "img/recipes/default.webp",
                "img/recipes/default.jpg",
                "img/recipes/default.png",
            ),
            vistas,
        )
    }
}

private fun receta(
    id: String,
    categoryId: String,
) = Recipe(
    id = id,
    source = RecipeSource.AI,
    title = "Receta de prueba",
    description = "",
    categoryId = categoryId,
    mealTypeId = "lunch",
    cuisineId = "italian",
    totalMinutes = 20,
    prepMinutes = 5,
    cookMinutes = 15,
    difficultyId = "easy",
    servings = 4,
    ingredients = emptyList(),
    steps = emptyList(),
)

/**
 * Fichero de un candidato dentro de `app/src/main/assets/img/recipes/` (la propiedad de sistema
 * apunta a esa carpeta, mientras que los candidatos incluyen la raiz [RecipePhotoAssets.ROOT]).
 */
private fun photoFile(candidatePath: String): File =
    File(assetsDirPath(), candidatePath.removePrefix("${RecipePhotoAssets.ROOT}/"))

private fun assetsDirPath(): String = checkNotNull(System.getProperty("recipe.photos.assets.dir")) {
    "Falta la propiedad de sistema recipe.photos.assets.dir"
}

private fun taxonomyJsonPath(): String = checkNotNull(System.getProperty("taxonomy.asset.path")) {
    "Falta la propiedad de sistema taxonomy.asset.path"
}

private fun seedRecipesJsonPath(): String = checkNotNull(System.getProperty("seed.recipes.asset.path")) {
    "Falta la propiedad de sistema seed.recipes.asset.path"
}
