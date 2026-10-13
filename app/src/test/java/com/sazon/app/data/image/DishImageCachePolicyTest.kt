package com.sazon.app.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 5 y 6 de `docs/specs/dish-image-cache.md` en la parte **pura**: nombre de fichero,
 * constantes del tope y seleccion determinista de victimas de la eviccion (orden por recencia,
 * desempate por nombre, ya por debajo del tope, lista vacia y candidato mayor que el tope).
 */
class DishImageCachePolicyTest {

    // Vector concreto exigido: la clave canonica es el nombre de fichero, mas la extension WebP.
    @Test
    fun el_nombre_de_fichero_es_la_clave_canonica_mas_webp() {
        assertEquals(
            "dish-image-v1-arroz-con-pollo-criollo.webp",
            DishImageCachePolicy.fileNameFor("dish-image-v1-arroz-con-pollo-criollo"),
        )
    }

    @Test
    fun las_constantes_del_tope_y_la_extension_son_las_declaradas() {
        assertEquals("dish-images", DishImageCachePolicy.SUBDIRECTORY)
        assertEquals("webp", DishImageCachePolicy.EXTENSION)
        assertEquals(80L * 1024 * 1024, DishImageCachePolicy.MAX_DISK_BYTES)
        assertEquals(83_886_080L, DishImageCachePolicy.MAX_DISK_BYTES)
        assertEquals(16, DishImageCachePolicy.MAX_MEMORY_ENTRIES)
    }

    @Test
    fun sin_candidatos_no_hay_victimas() {
        assertTrue(DishImageCachePolicy.evictionVictims(emptyList(), 100L).isEmpty())
    }

    @Test
    fun por_debajo_del_tope_no_hay_victimas() {
        val candidates = listOf(entry("a.webp", 40L, 10L), entry("b.webp", 60L, 20L))

        assertTrue("Total 100 <= tope 100", DishImageCachePolicy.evictionVictims(candidates, 100L).isEmpty())
        assertTrue("Total 100 <= tope 200", DishImageCachePolicy.evictionVictims(candidates, 200L).isEmpty())
    }

    @Test
    fun borra_los_mas_antiguos_hasta_caber_en_el_tope() {
        val candidates = listOf(
            entry("a.webp", 100L, 10L),
            entry("b.webp", 100L, 20L),
            entry("c.webp", 100L, 30L),
        )

        // Total 300: con presupuesto 200 basta borrar el mas antiguo; con 100, dos; con 0, todos.
        assertEquals(listOf("a.webp"), DishImageCachePolicy.evictionVictims(candidates, 200L))
        assertEquals(listOf("a.webp", "b.webp"), DishImageCachePolicy.evictionVictims(candidates, 100L))
        assertEquals(
            listOf("a.webp", "b.webp", "c.webp"),
            DishImageCachePolicy.evictionVictims(candidates, 0L),
        )
    }

    // Caso negativo que no pasa en vacio: con el mismo `lastModified` manda el nombre ascendente. Si
    // el desempate fuera descendente, la victima seria "b.webp" y la asercion fallaria.
    @Test
    fun con_el_mismo_last_modified_desempata_por_nombre_ascendente() {
        val candidates = listOf(entry("b.webp", 10L, 5L), entry("a.webp", 10L, 5L))

        assertEquals(listOf("a.webp"), DishImageCachePolicy.evictionVictims(candidates, 10L))
    }

    @Test
    fun un_candidato_mayor_que_el_tope_se_borra() {
        val candidates = listOf(entry("grande.webp", 500L, 1L))

        assertEquals(listOf("grande.webp"), DishImageCachePolicy.evictionVictims(candidates, 100L))
    }
}

private fun entry(fileName: String, sizeBytes: Long, lastModifiedMillis: Long) =
    DishImageCacheEntry(fileName, sizeBytes, lastModifiedMillis)
