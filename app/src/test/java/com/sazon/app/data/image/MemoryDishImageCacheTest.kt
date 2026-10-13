package com.sazon.app.data.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenario 8 de `docs/specs/dish-image-cache.md` para la LRU en memoria: tope de entradas, descarte
 * de la **menos usada recientemente** (un `get` cuenta como uso) y `clear`. La parte de "una clave
 * descartada se sirve del disco sin llamar al generador" se cubre en `DishImageResolverTest`, que es
 * quien orquesta memoria + disco.
 */
class MemoryDishImageCacheTest {

    @Test
    fun con_tope_de_dos_la_menos_usada_recientemente_se_descarta() {
        val memory = MemoryDishImageCache(maxEntries = 2)
        memory.put("a", byteArrayOf(1))
        memory.put("b", byteArrayOf(2))
        assertEquals(2, memory.size)

        // Tocar 'a' lo rejuvenece: 'b' pasa a ser la menos usada.
        assertArrayEquals(byteArrayOf(1), memory.get("a"))

        memory.put("c", byteArrayOf(3))

        assertEquals("El tamano se mantiene en el tope", 2, memory.size)
        assertNotNull("La recien tocada 'a' sobrevive", memory.get("a"))
        assertNotNull("La recien insertada 'c' sobrevive", memory.get("c"))
        assertNull("La menos usada 'b' se descarta", memory.get("b"))
    }

    @Test
    fun get_de_una_clave_ausente_devuelve_null() {
        assertNull(MemoryDishImageCache().get("no-existe"))
    }

    @Test
    fun put_reemplaza_los_bytes_de_la_misma_clave_sin_crecer() {
        val memory = MemoryDishImageCache(maxEntries = 2)
        memory.put("a", byteArrayOf(1))
        memory.put("a", byteArrayOf(9))

        assertEquals(1, memory.size)
        assertArrayEquals(byteArrayOf(9), memory.get("a"))
    }

    @Test
    fun clear_vacia_la_cache() {
        val memory = MemoryDishImageCache()
        memory.put("a", byteArrayOf(1))
        memory.put("b", byteArrayOf(2))

        memory.clear()

        assertEquals(0, memory.size)
        assertNull(memory.get("a"))
        assertNull(memory.get("b"))
    }

    @Test
    fun por_defecto_usa_el_tope_de_la_politica() {
        val memory = MemoryDishImageCache()
        repeat(DishImageCachePolicy.MAX_MEMORY_ENTRIES + 5) { index ->
            memory.put("clave-$index", byteArrayOf(index.toByte()))
        }

        assertEquals(DishImageCachePolicy.MAX_MEMORY_ENTRIES, memory.size)
        assertNull("La primera clave se descarto", memory.get("clave-0"))
        assertNotNull(memory.get("clave-${DishImageCachePolicy.MAX_MEMORY_ENTRIES + 4}"))
    }
}
