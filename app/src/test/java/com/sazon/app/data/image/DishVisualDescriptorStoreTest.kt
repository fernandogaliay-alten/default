package com.sazon.app.data.image

import com.sazon.app.domain.image.DishVisualDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Escenario 5 de `docs/specs/dish-visual-descriptor-wiring.md`: el almacen de sesion del descriptor
 * visual es **tolerante, por sesion y efimero**.
 *
 * Es un valor puro de JVM (solo JDK): sin Android, sin red, sin reloj y sin disco.
 */
class DishVisualDescriptorStoreTest {

    private val descriptor = DishVisualDescriptor(dishKey = "arroz-con-pollo", imagePrompt = "plato")

    @Test
    fun escenario_5_un_id_en_blanco_o_un_descriptor_vacio_no_crean_entrada() {
        val store = DishVisualDescriptorStore()

        store.record(
            mapOf(
                "" to descriptor,
                "   " to descriptor,
                "ai-1234" to DishVisualDescriptor.NONE,
                "ai-5678" to DishVisualDescriptor(),
            ),
        )

        assertNull("Un id en blanco no crea entrada", store.descriptorFor(""))
        assertNull("Un id en blanco no crea entrada", store.descriptorFor("   "))
        assertNull("Un descriptor vacio no crea entrada", store.descriptorFor("ai-1234"))
        assertNull("Un descriptor vacio no crea entrada", store.descriptorFor("ai-5678"))
    }

    @Test
    fun escenario_5_con_el_mismo_id_registrado_dos_veces_gana_el_ultimo() {
        val store = DishVisualDescriptorStore()
        val primero = DishVisualDescriptor(dishKey = "arroz-con-pollo")
        val segundo = DishVisualDescriptor(dishKey = "arroz-con-pollo-facil", imagePrompt = "plato facil")

        store.record(mapOf("ai-1234" to primero))
        assertEquals(primero, store.descriptorFor("ai-1234"))

        store.record(mapOf("ai-1234" to segundo))
        assertEquals("El ultimo registro del mismo id gana", segundo, store.descriptorFor("ai-1234"))
    }

    @Test
    fun escenario_5_descriptor_for_devuelve_null_para_id_nulo_en_blanco_o_desconocido() {
        val store = DishVisualDescriptorStore()
        store.record(mapOf("ai-1234" to descriptor))

        assertNull(store.descriptorFor(null))
        assertNull(store.descriptorFor(""))
        assertNull(store.descriptorFor("   "))
        assertNull("Un id desconocido degrada a fase 1", store.descriptorFor("ai-9999"))
        assertEquals(descriptor, store.descriptorFor("ai-1234"))
    }

    @Test
    fun escenario_5_el_almacen_es_por_sesion_y_no_comparte_estado() {
        val uno = DishVisualDescriptorStore()
        val otro = DishVisualDescriptorStore()

        uno.record(mapOf("ai-1234" to descriptor))

        assertEquals(descriptor, uno.descriptorFor("ai-1234"))
        assertNull("Dos almacenes distintos no comparten estado", otro.descriptorFor("ai-1234"))
    }

    @Test
    fun escenario_5_el_descriptor_guardado_es_el_mismo_valor_puro() {
        val store = DishVisualDescriptorStore()
        val descriptor = DishVisualDescriptor(dishKey = "arroz-con-pollo", imagePrompt = "plato")

        store.record(mapOf("ai-1234" to descriptor))

        val guardado = store.descriptorFor("ai-1234")
        assertEquals(descriptor, guardado)
        // El valor es inmutable: no hay copia defensiva que perder ni mutacion posible.
        assertSame(descriptor, guardado)
    }
}
