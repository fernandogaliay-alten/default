package com.sazon.app.domain.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politica pura del descriptor visual del plato (fase 5): limpieza, `isEmpty` y degradacion.
 *
 * Los escenarios 5-8 (clave/prompt) viven en [DishImageCacheKeyTest], [DishImagePromptTest] y
 * [DishImageRequestTest]; los del wire (2-4 y 10), en
 * `com.sazon.app.data.ai.DishVisualDescriptorWireTest`.
 */
class DishVisualDescriptorTest {

    @Test
    fun un_descriptor_completo_conserva_el_dish_key_tal_cual_y_limpia_el_image_prompt() {
        val descriptor = DishVisualDescriptor.of(
            dishKey = "  Arroz con Pollo Criollo  ",
            imagePrompt = "  arroz   amarillo con pollo,  guisantes y zanahoria  ",
        )

        assertEquals(
            "El dishKey conserva mayusculas y solo se colapsa el espacio",
            "Arroz con Pollo Criollo",
            descriptor.dishKey,
        )
        assertEquals(
            "El imagePrompt se colapsa a una sola linea",
            "arroz amarillo con pollo, guisantes y zanahoria",
            descriptor.imagePrompt,
        )
        assertFalse("Con ambos campos utiles no esta vacio", descriptor.isEmpty)
        assertTrue(descriptor.isNotEmpty)
    }

    @Test
    fun of_conserva_mayusculas_y_diacriticos_del_dish_key_porque_quien_canonicaliza_es_la_clave() {
        val descriptor = DishVisualDescriptor.of(dishKey = "Salmón a la Plancha", imagePrompt = null)

        assertEquals("Salmón a la Plancha", descriptor.dishKey)
        assertEquals(
            "Normalizar no es trabajo del descriptor",
            DishImageCacheKey.normalize("Salmón a la Plancha"),
            DishImageCacheKey.normalize(requireNotNull(descriptor.dishKey)),
        )
    }

    @Test
    fun un_dish_key_sin_ningun_caracter_utilizable_se_descarta() {
        listOf("🍲", "...", "  ", "寿司", "--").forEach { bruto ->
            assertNull(
                "El dishKey '$bruto' no deja caracter [a-z0-9] y debe descartarse",
                DishVisualDescriptor.of(dishKey = bruto, imagePrompt = null).dishKey,
            )
        }
    }

    @Test
    fun un_image_prompt_vacio_o_en_blanco_se_descarta() {
        listOf(null, "", "   ", "\n").forEach { bruto ->
            assertNull(
                "El imagePrompt '$bruto' debe descartarse",
                DishVisualDescriptor.of(dishKey = null, imagePrompt = bruto).imagePrompt,
            )
        }
    }

    @Test
    fun un_descriptor_sin_campos_utiles_es_vacio_y_equivale_a_none() {
        val vacio = DishVisualDescriptor.of(dishKey = "🍲", imagePrompt = "   ")

        assertTrue("Sin campos utiles el descriptor esta vacio", vacio.isEmpty)
        assertFalse(vacio.isNotEmpty)
        assertEquals(vacio, DishVisualDescriptor.NONE)
        assertTrue(DishVisualDescriptor.NONE.isEmpty)
        assertEquals(null, DishVisualDescriptor.NONE.dishKey)
        assertEquals(null, DishVisualDescriptor.NONE.imagePrompt)
    }

    @Test
    fun un_solo_campo_utilizable_basta_para_no_estar_vacio() {
        val soloDishKey = DishVisualDescriptor.of(dishKey = "arroz-con-pollo", imagePrompt = "")
        val soloPrompt = DishVisualDescriptor.of(dishKey = "", imagePrompt = "arroz con pollo")

        assertFalse(soloDishKey.isEmpty)
        assertNull("Sin imagePrompt util, queda a null", soloDishKey.imagePrompt)
        assertEquals("arroz-con-pollo", soloDishKey.dishKey)

        assertFalse(soloPrompt.isEmpty)
        assertNull("Sin dishKey util, queda a null", soloPrompt.dishKey)
        assertEquals("arroz con pollo", soloPrompt.imagePrompt)
    }

    @Test
    fun of_es_puro_y_no_lanza_con_entradas_arbitrarias() {
        val casos = listOf(
            null to null,
            "" to "",
            "  " to "  ",
            "🍲" to "🤖",
            "Arroz  con   pollo" to "Arroz   con  pollo",
        )

        casos.forEach { (dishKey, imagePrompt) ->
            val primera = DishVisualDescriptor.of(dishKey, imagePrompt)
            val segunda = DishVisualDescriptor.of(dishKey, imagePrompt)
            assertEquals("Misma entrada, mismo descriptor", primera, segunda)
        }
    }
}
