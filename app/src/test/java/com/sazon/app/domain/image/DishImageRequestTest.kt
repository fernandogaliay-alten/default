package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DishImageRequestTest {

    private val taxonomy = taxonomyReal()

    @Test
    fun escenario_9_la_peticion_compone_el_prompt_y_la_clave_canonicos() {
        val recipe = receta(title = "Arroz con pollo criollo", cuisineId = "spanish")

        val request = DishImageRequest.of(taxonomy, recipe)

        assertEquals(DishImagePrompt.build(taxonomy, recipe), request.prompt)
        assertEquals(DishImageCacheKey.of(recipe), request.cacheKey)
        assertEquals("seed-arroz-pollo", request.recipeId)
    }

    @Test
    fun escenario_9b_dos_construcciones_sucesivas_son_iguales() {
        val recipe = receta()
        val primera = DishImageRequest.of(taxonomy, recipe)

        assertEquals("data class con igualdad estructural", primera, DishImageRequest.of(taxonomy, recipe))
        assertEquals(
            "El valor coincide con su construccion campo a campo",
            DishImageRequest(
                cacheKey = DishImageCacheKey.of(recipe),
                prompt = DishImagePrompt.build(taxonomy, recipe),
                recipeId = recipe.id,
            ),
            primera,
        )
        assertNotEquals(
            "Otra receta produce otra peticion",
            primera,
            DishImageRequest.of(taxonomy, receta(id = "seed-tortilla", title = "Tortilla de patatas")),
        )
    }

    @Test
    fun escenario_9c_un_id_vacio_degrada_a_recipe_id_nulo() {
        val sinId = DishImageRequest.of(taxonomy, receta(id = ""))
        val idEnBlanco = DishImageRequest.of(taxonomy, receta(id = "   "))

        assertNull(sinId.recipeId)
        assertNull(idEnBlanco.recipeId)
        assertFalse("La clave sigue existiendo aunque no haya id", sinId.cacheKey.isBlank())
        assertFalse("El prompt sigue existiendo aunque no haya id", sinId.prompt.isBlank())
    }

    @Test
    fun escenario_9d_el_recipe_id_es_opcional_y_por_defecto_nulo() {
        assertEquals(DishImageRequest("clave", "prompt"), DishImageRequest("clave", "prompt", recipeId = null))
        assertNull(DishImageRequest("clave", "prompt").recipeId)
    }

    @Test
    fun escenario_9e_la_clave_no_depende_de_la_clasificacion_ni_del_origen() {
        val semilla = DishImageRequest.of(taxonomy, receta(id = "seed-arroz-pollo"))
        val ia = DishImageRequest.of(
            taxonomy,
            receta(
                id = "ai-1234",
                source = RecipeSource.AI,
                title = "Arroz con pollo",
                categoryId = "poultry",
                cuisineId = "asian",
            ),
        )

        assertEquals(
            "La identidad de cache es el nombre del plato, no su clasificacion ni su origen",
            semilla.cacheKey,
            ia.cacheKey,
        )
        assertEquals("seed-arroz-pollo", semilla.recipeId)
        assertEquals("ai-1234", ia.recipeId)
    }

    // --- Escenario 9 de `docs/specs/dish-visual-descriptor.md` (fase 5) ---

    @Test
    fun descriptor_escenario_9_la_peticion_compone_ambas_piezas_con_el_descriptor() {
        val recipe = receta(title = "Arroz con pollo fácil", cuisineId = "spanish")
        val descriptor = DishVisualDescriptor(
            dishKey = "arroz-con-pollo",
            imagePrompt = "arroz amarillo con pollo desmenuzado",
        )

        val request = DishImageRequest.of(taxonomy, recipe, descriptor)

        assertEquals(DishImageCacheKey.of(recipe, descriptor), request.cacheKey)
        assertEquals(DishImagePrompt.build(taxonomy, recipe, descriptor), request.prompt)
        assertEquals("El recipeId no cambia con el descriptor", "seed-arroz-pollo", request.recipeId)
        assertEquals("dish-image-v2-arroz-con-pollo", request.cacheKey)
        val camposDelValor = DishImageRequest::class.java.declaredFields
            .map { it.name }
            // El companion es estatico y el compilador de Compose sintetiza `$stable`: ninguno es un
            // campo del valor.
            .filterNot { it == "Companion" || it == "\$stable" }
            .toSet()
        assertEquals(
            "El descriptor no anade campos al transporte puro",
            setOf("cacheKey", "prompt", "recipeId"),
            camposDelValor,
        )
    }

    @Test
    fun descriptor_escenario_9b_sin_descriptor_el_resultado_es_el_de_la_fase_1() {
        val recipe = receta(title = "Arroz con pollo criollo", cuisineId = "spanish")

        assertEquals(
            "Omitir el descriptor equivale a la fase 1",
            DishImageRequest.of(taxonomy, recipe),
            DishImageRequest.of(taxonomy, recipe, descriptor = null),
        )
        assertEquals(
            DishImageRequest.of(taxonomy, recipe),
            DishImageRequest.of(taxonomy, recipe, descriptor = DishVisualDescriptor.NONE),
        )
    }
}

private fun receta(
    id: String = "seed-arroz-pollo",
    source: RecipeSource = RecipeSource.SEED,
    title: String = "Arroz con pollo",
    categoryId: String = "rice",
    cuisineId: String = "spanish",
) = Recipe(
    id = id,
    source = source,
    title = title,
    description = "Receta de prueba",
    categoryId = categoryId,
    mealTypeId = "lunch",
    cuisineId = cuisineId,
    totalMinutes = 30,
    prepMinutes = 10,
    cookMinutes = 20,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = emptyList(),
)

private fun taxonomyReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
