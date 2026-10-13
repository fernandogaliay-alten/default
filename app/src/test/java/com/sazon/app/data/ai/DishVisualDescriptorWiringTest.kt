package com.sazon.app.data.ai

import com.sazon.app.data.image.DishVisualDescriptorStore
import com.sazon.app.domain.image.DishImageCacheKey
import com.sazon.app.domain.image.DishImagePrompt
import com.sazon.app.domain.image.DishImageRequest
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 2-4 de `docs/specs/dish-visual-descriptor-wiring.md`: el descriptor que produce el
 * **decode** ([AiDecodeResult.descriptors]) llega al **unico punto de composicion**
 * (`DishImageRequest.of`) a traves del almacen de sesion ([DishVisualDescriptorStore]), que es
 * exactamente lo que cablea `MainActivity` (`descriptorFor(recipe.id)`).
 *
 * El test vive en `data.ai` porque consume `AiRecipePayload.decode`/`AiDecodeResult` (`internal`, via
 * *friend paths*); el almacen y la composicion son publicos. Corre en JVM con la Taxonomia real, sin
 * red, sin cuota y sin dependencias nuevas.
 */
class DishVisualDescriptorWiringTest {

    private val taxonomy = taxonomy()

    // Escenario 2: propagacion decode -> almacen -> DishImageRequest.of (clave y prompt del descriptor).
    @Test
    fun escenario_2_el_descriptor_del_decode_llega_a_dish_image_request() {
        val result = AiRecipePayload.decode(
            envelope(
                draft(
                    title = "Arroz con pollo criollo",
                    dishKey = "arroz-con-pollo-criollo",
                    imagePrompt = "arroz amarillo con pollo desmenuzado y aceitunas",
                ),
            ),
            taxonomy,
        )
        val store = DishVisualDescriptorStore()
        store.record(result.descriptors)

        val recipe = result.accepted.single()
        assertEquals("El descriptor va indexado por el id generado", recipe.id, result.descriptors.keys.single())

        val descriptor = store.descriptorFor(recipe.id)
        assertTrue("El descriptor del decode queda registrado", descriptor != null)

        // Exactamente la composicion que hace `MainActivity.requestFor`.
        val request = DishImageRequest.of(taxonomy, recipe, store.descriptorFor(recipe.id))

        assertEquals(DishImageCacheKey.of(recipe, descriptor), request.cacheKey)
        assertEquals("dish-image-v1-arroz-con-pollo-criollo", request.cacheKey)
        assertEquals(DishImagePrompt.build(taxonomy, recipe, descriptor), request.prompt)
        assertTrue(
            "El prompt usa el imagePrompt del modelo como clausula del plato",
            request.prompt.contains("arroz amarillo con pollo desmenuzado y aceitunas"),
        )
        assertEquals("El recipeId no cambia", recipe.id, request.recipeId)
    }

    // Escenario 3: dos variantes del mismo dishKey comparten la clave canonica (una sola imagen).
    @Test
    fun escenario_3_dos_variantes_del_mismo_dish_key_comparten_la_clave_canonica() {
        val result = AiRecipePayload.decode(
            envelope(
                draft(title = "Arroz con pollo", dishKey = "arroz-con-pollo"),
                draft(title = "Arroz con pollo fácil", dishKey = "arroz-con-pollo"),
            ),
            taxonomy,
        )
        val store = DishVisualDescriptorStore()
        store.record(result.descriptors)

        val uno = result.accepted[0]
        val dos = result.accepted[1]
        val claveUno = DishImageRequest.of(taxonomy, uno, store.descriptorFor(uno.id)).cacheKey
        val claveDos = DishImageRequest.of(taxonomy, dos, store.descriptorFor(dos.id)).cacheKey

        assertEquals("dish-image-v1-arroz-con-pollo", claveUno)
        assertEquals("Dos variantes del mismo plato comparten una sola clave", claveUno, claveDos)

        // La segunda variante pagaria una imagen distinta sin descriptor; por eso el cableado importa.
        assertEquals("dish-image-v1-arroz-con-pollo-facil", DishImageCacheKey.of(dos))
        assertNotEquals(DishImageCacheKey.of(dos), claveDos)
    }

    @Test
    fun escenario_3b_con_titulos_que_no_coinciden_con_el_slug_ambas_claves_por_titulo_difieren() {
        val result = AiRecipePayload.decode(
            envelope(
                draft(title = "Arroz con pollo de la abuela", dishKey = "arroz-con-pollo"),
                draft(title = "Arroz con pollo fácil", dishKey = "arroz-con-pollo"),
            ),
            taxonomy,
        )
        val store = DishVisualDescriptorStore()
        store.record(result.descriptors)

        val uno = result.accepted[0]
        val dos = result.accepted[1]
        val claveCompartida = DishImageRequest.of(taxonomy, uno, store.descriptorFor(uno.id)).cacheKey

        assertEquals("dish-image-v1-arroz-con-pollo", claveCompartida)
        assertEquals(
            "La clave canonica no sale del titulo",
            claveCompartida,
            DishImageRequest.of(taxonomy, dos, store.descriptorFor(dos.id)).cacheKey,
        )
        assertNotEquals(
            "La clave compartida no sale del título de la primera variante",
            claveCompartida,
            DishImageCacheKey.of(uno),
        )
        assertNotEquals(
            "La clave compartida no sale del título de la segunda variante",
            claveCompartida,
            DishImageCacheKey.of(dos),
        )
    }

    // Escenario 4 (negativo): sin descriptor la composicion es EXACTAMENTE la de la fase 1.
    @Test
    fun escenario_4_sin_descriptor_la_composicion_es_exactamente_la_de_la_fase_1() {
        val store = DishVisualDescriptorStore()

        // Receta semilla del catalogo, receta restaurada del Lote o Favorito, e id en blanco/nulo.
        val recetas = listOf(
            receta(id = "seed-arroz-pollo", source = RecipeSource.SEED, title = "Arroz con pollo"),
            receta(id = "ai-restaurada-del-lote", source = RecipeSource.AI, title = "Tortilla de patatas"),
            receta(id = "", source = RecipeSource.AI, title = "Plato sin id"),
            receta(id = "   ", source = RecipeSource.AI, title = "Plato con id en blanco"),
        )

        recetas.forEach { recipe ->
            assertNull("Una receta sin registrar no tiene descriptor", store.descriptorFor(recipe.id))
            assertEquals(
                "Sin descriptor la composicion es la de la fase 1",
                DishImageRequest.of(taxonomy, recipe),
                DishImageRequest.of(taxonomy, recipe, store.descriptorFor(recipe.id)),
            )
        }
    }

    @Test
    fun escenario_4b_un_decode_sin_descriptor_deja_el_almacen_vacio_y_la_clave_por_titulo() {
        val result = AiRecipePayload.decode(
            envelope(draft(title = "Arroz con pollo criollo", dishKey = null, imagePrompt = null)),
            taxonomy,
        )
        val store = DishVisualDescriptorStore()
        store.record(result.descriptors)

        val recipe = result.accepted.single()
        assertTrue("Sin descriptor el decode no aporta entradas", result.descriptors.isEmpty())
        assertNull(store.descriptorFor(recipe.id))

        val request = DishImageRequest.of(taxonomy, recipe, store.descriptorFor(recipe.id))
        assertEquals("La clave sigue saliendo del titulo", DishImageCacheKey.of(recipe), request.cacheKey)
        assertEquals("dish-image-v1-arroz-con-pollo-criollo", request.cacheKey)
        assertEquals("El prompt sigue saliendo del titulo", DishImagePrompt.build(taxonomy, recipe), request.prompt)
    }

    private fun taxonomy(): Taxonomy {
        val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
            "Falta la propiedad de sistema taxonomy.asset.path"
        }
        return TaxonomyJson.parse(File(path).readText())
    }
}

private fun receta(
    id: String,
    source: RecipeSource,
    title: String,
) = Recipe(
    id = id,
    source = source,
    title = title,
    description = "Receta de prueba",
    categoryId = "rice",
    mealTypeId = "lunch",
    cuisineId = "spanish",
    totalMinutes = 30,
    prepMinutes = 10,
    cookMinutes = 20,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = emptyList(),
)

private fun envelope(vararg draftJson: String): String =
    """{"recipes":[${draftJson.joinToString(",")}]}"""

/** Receta valida del contrato §4 (sin `id` ni `source`) con el descriptor opcional del modelo. */
private fun draft(
    title: String = "Arroz salteado con verduras",
    dishKey: String? = null,
    imagePrompt: String? = null,
): String {
    val descriptor = buildString {
        if (dishKey != null) append(""", "dishKey": "$dishKey"""")
        if (imagePrompt != null) append(""", "imagePrompt": "$imagePrompt"""")
    }
    return """
        {
          "title": "$title",
          "description": "Un arroz rapido con verduras.",
          "categoryId": "rice",
          "mealTypeId": "lunch",
          "cuisineId": "asian",
          "totalMinutes": 25,
          "prepMinutes": 10,
          "cookMinutes": 15,
          "difficultyId": "easy",
          "servings": 2,
          "ingredients": [{"name":"Arroz","quantity":200,"unit":"g","isPantry":false,"ingredientId":"arroz"},{"name":"Zanahoria","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"zanahoria"}],
          "steps": [{"order":1,"title":"Cocer el arroz","description":"Cuece el arroz.","minutes":12},{"order":2,"title":"Saltear","description":"Saltea las verduras.","minutes":13}],
          "tips": [],
          "nutrition": null,
          "declaredAllergens": []$descriptor
        }
    """.trimIndent()
}
