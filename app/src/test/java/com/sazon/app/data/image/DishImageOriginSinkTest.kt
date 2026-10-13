package com.sazon.app.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenario 9 de `docs/specs/dish-image-prompt-v2.md`: el sumidero de orígenes es de **sesión**,
 * **puro** (JDK, sin Android ni disco) y tolerante con entradas basura del borde.
 */
class DishImageOriginSinkTest {

    @Test
    fun escenario_9_el_ultimo_origen_registrado_gana_por_recipe_id() {
        val sink = DishImageOriginSink()

        sink.record("ai-1234", DishImageOrigin.SEED)
        sink.record("ai-1234", DishImageOrigin.GENERATED)
        sink.record("seed-arroz-pollo", DishImageOrigin.CATEGORY)

        assertEquals(DishImageOrigin.GENERATED, sink.originFor("ai-1234"))
        assertEquals(DishImageOrigin.CATEGORY, sink.originFor("seed-arroz-pollo"))
    }

    @Test
    fun escenario_9b_los_ids_nulos_o_en_blanco_se_ignoran_y_no_crean_entradas() {
        val sink = DishImageOriginSink()

        sink.record(null, DishImageOrigin.GENERATED)
        sink.record("", DishImageOrigin.GENERATED)
        sink.record("   ", DishImageOrigin.GENERATED)

        assertNull(sink.originFor(null))
        assertNull(sink.originFor(""))
        assertNull(sink.originFor("   "))
        assertNull("Ningun id desconocido tiene origen", sink.originFor("ai-inexistente"))
    }

    @Test
    fun escenario_9c_el_origen_registrado_por_un_id_utilizable_se_consulta_exactamente() {
        val sink = DishImageOriginSink()

        DishImageOrigin.entries.forEach { origin -> sink.record("id-$origin", origin) }

        DishImageOrigin.entries.forEach { origin ->
            assertEquals(origin, sink.originFor("id-$origin"))
        }
        assertNull("Un id solo coincide consigo mismo", sink.originFor("id-OTRO"))
    }

    @Test
    fun escenario_9d_dos_instancias_no_comparten_estado() {
        val uno = DishImageOriginSink()
        val dos = DishImageOriginSink()

        uno.record("ai-1234", DishImageOrigin.SEED)

        assertEquals(DishImageOrigin.SEED, uno.originFor("ai-1234"))
        assertNull("El sumidero no es global", dos.originFor("ai-1234"))
    }
}
