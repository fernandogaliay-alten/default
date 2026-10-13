package com.sazon.app.data.ai

import com.sazon.app.domain.image.DishVisualDescriptor
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.domain.validation.DiscardReason
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 2-4 y 10 de `docs/specs/dish-visual-descriptor.md`: parseo **tolerante** del descriptor
 * visual opcional (`dishKey` + `imagePrompt`) en la frontera del wire.
 *
 * Los cuerpos son **JSON literales** del contrato §4 (sin `id` ni `source`), como
 * [AiRecipePayloadTest]. El descriptor **nunca** puede descartar la receta ni alterar
 * `accepted`/`discardedByReason`, y **no** entra en el `id` ([AiRecipeIds]).
 */
class DishVisualDescriptorWireTest {

    private val taxonomy = taxonomy()

    private fun decode(content: String): AiDecodeResult = AiRecipePayload.decode(content, taxonomy)

    // Escenario 2: el descriptor presente se extrae y la receta sale exactamente como sin el.
    @Test
    fun escenario_2_el_descriptor_presente_se_extrae_y_no_altera_la_receta() {
        val sinDescriptor = decode(envelope(draft()))
        val conDescriptor = decode(
            envelope(
                draft(
                    """, "dishKey": "arroz-con-pollo-criollo", "imagePrompt": "arroz amarillo con pollo desmenuzado, guisantes, zanahoria y aceitunas"""",
                ),
            ),
        )

        assertEquals(
            "La receta aceptada es exactamente la misma con y sin descriptor",
            sinDescriptor.accepted,
            conDescriptor.accepted,
        )
        assertEquals(1, conDescriptor.received)
        assertEquals(sinDescriptor.discardedByReason, conDescriptor.discardedByReason)
        assertTrue("Sin descriptor no hay descriptores", sinDescriptor.descriptors.isEmpty())

        val recipe = conDescriptor.accepted.single()
        assertEquals("El descriptor va indexado por el id generado", recipe.id, conDescriptor.descriptors.keys.single())
        assertTrue("El id lo completa la app", recipe.id.startsWith(AiRecipeIds.PREFIX))
        val descriptor = conDescriptor.descriptors.getValue(recipe.id)
        assertEquals("arroz-con-pollo-criollo", descriptor.dishKey)
        assertEquals(
            "arroz amarillo con pollo desmenuzado, guisantes, zanahoria y aceitunas",
            descriptor.imagePrompt,
        )
        assertFalse(descriptor.isEmpty)
    }

    // Escenario 3: sin descriptor (ausente, nulo o en blanco) hay degradacion exacta.
    @Test
    fun escenario_3_sin_descriptor_el_resultado_es_identico_al_de_una_respuesta_sin_los_campos() {
        val baseline = decode(envelope(draft()))

        val sinDescriptor = listOf(
            draft(),
            draft(""", "dishKey": null, "imagePrompt": null"""),
            draft(""", "dishKey": "", "imagePrompt": "" """),
            draft(""", "dishKey": "   ", "imagePrompt": "   " """),
        )

        sinDescriptor.forEach { draftJson ->
            val result = decode(envelope(draftJson))
            assertEquals("La receta se acepta igual", baseline.accepted, result.accepted)
            assertEquals(1, result.received)
            assertEquals(baseline.discardedByReason, result.discardedByReason)
            assertTrue("Sin campos utiles no hay descriptores", result.descriptors.isEmpty())
            assertEquals(baseline.contentState, result.contentState)
        }
    }

    // Escenario 4: formato invalido -> se ignora, nunca descarta la receta (caso negativo).
    @Test
    fun escenario_4_un_tipo_json_equivocado_no_descarta_la_receta_ni_aporta_descriptor() {
        val casosInvalidos = listOf(
            """, "dishKey": 123, "imagePrompt": 456""",
            """, "dishKey": 3.5, "imagePrompt": true""",
            """, "dishKey": {"canonico": "arroz"}, "imagePrompt": ["arroz"]""",
            """, "dishKey": [1, 2], "imagePrompt": {"texto": "arroz"}""",
        )

        casosInvalidos.forEach { campos ->
            val result = decode(envelope(draft(campos)))
            assertEquals("La receta sobrevive a '$campos'", 1, result.accepted.size)
            assertFalse(
                "Un campo cosmetico nunca puede contar como SCHEMA ('$campos')",
                result.discardedByReason.containsKey(DiscardReason.SCHEMA),
            )
            assertTrue("Sin campos textuales utiles no hay descriptor ('$campos')", result.descriptors.isEmpty())
        }
    }

    @Test
    fun escenario_4b_un_dish_key_no_canonicalizable_se_ignora_pero_no_tumba_la_receta() {
        listOf("🍲", "...", "寿司", "---").forEach { dishKey ->
            val result = decode(envelope(draft(""", "dishKey": "$dishKey", "imagePrompt": "" """)))

            assertEquals("La receta sigue aceptada con dishKey '$dishKey'", 1, result.accepted.size)
            assertFalse(result.discardedByReason.containsKey(DiscardReason.SCHEMA))
            assertTrue("Un dishKey sin caracteres utiles no aporta descriptor", result.descriptors.isEmpty())
        }
    }

    @Test
    fun escenario_4c_un_descriptor_parcial_conserva_solo_el_campo_utilizable() {
        val soloImagePrompt = decode(envelope(draft(""", "dishKey": "🍲", "imagePrompt": "plato terminado"""",)))
        val descriptorPrompt = soloImagePrompt.descriptors.values.single()
        assertNull("El dishKey inutil se descarta", descriptorPrompt.dishKey)
        assertEquals("El imagePrompt util se conserva", "plato terminado", descriptorPrompt.imagePrompt)

        val soloDishKey = decode(envelope(draft(""", "dishKey": "arroz-con-pollo", "imagePrompt": "   """",)))
        val descriptorKey = soloDishKey.descriptors.values.single()
        assertEquals("arroz-con-pollo", descriptorKey.dishKey)
        assertNull("El imagePrompt en blanco se descarta", descriptorKey.imagePrompt)
    }

    @Test
    fun un_descarte_no_aporta_descriptor() {
        // `mealTypeId` inexistente: la receta se descarta por SCHEMA aunque traiga descriptor.
        val result = decode(
            envelope(draft(""", "dishKey": "arroz-con-pollo", "imagePrompt": "plato"""", mealTypeId = "second_breakfast")),
        )

        assertEquals(0, result.accepted.size)
        assertEquals(mapOf(DiscardReason.SCHEMA to 1), result.discardedByReason)
        assertTrue("Un descarte no aporta descriptor", result.descriptors.isEmpty())
    }

    // Escenario 10: los ids de las recetas no cambian al anadir el descriptor.
    @Test
    fun escenario_10_el_descriptor_queda_fuera_de_la_cadena_canonica_del_id() {
        val sinDescriptor = decode(envelope(draft()))
        val conDescriptor = decode(
            envelope(draft(""", "dishKey": "arroz-con-pollo", "imagePrompt": "plato terminado"""",)),
        )

        assertEquals(
            "Dos respuestas que solo difieren en el descriptor comparten id",
            sinDescriptor.accepted.single().id,
            conDescriptor.accepted.single().id,
        )
        assertTrue(conDescriptor.accepted.single().id.startsWith(AiRecipeIds.PREFIX))
        assertTrue(sinDescriptor.descriptors.isEmpty())
        assertEquals(1, conDescriptor.descriptors.size)
    }

    private fun taxonomy(): Taxonomy {
        val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
            "Falta la propiedad de sistema taxonomy.asset.path"
        }
        return TaxonomyJson.parse(File(path).readText())
    }
}

private fun envelope(vararg draftJson: String): String =
    """{"recipes":[${draftJson.joinToString(",")}]}"""

/**
 * Receta del contrato §4 como **JSON literal** (sin `id` ni `source`), parametrizable con campos
 * extra del descriptor. [descriptorFields] debe empezar por coma (o ser vacio).
 */
private fun draft(
    descriptorFields: String = "",
    mealTypeId: String = "lunch",
): String = """
    {
      "title": "Arroz salteado con verduras",
      "description": "Un arroz rapido con verduras.",
      "categoryId": "rice",
      "mealTypeId": "$mealTypeId",
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
      "declaredAllergens": []$descriptorFields
    }
""".trimIndent()
