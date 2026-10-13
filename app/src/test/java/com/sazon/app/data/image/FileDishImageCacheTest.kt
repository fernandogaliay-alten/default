package com.sazon.app.data.image

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Escenarios 5, 6, 7 y 11 de `docs/specs/dish-image-cache.md` para el almacen de ficheros: ida y
 * vuelta con la clave canonica como nombre, recencia al leer, escritura atomica, eviccion LRU con un
 * tope **inyectado** que no sale de su raiz, y fallos de I/O sin excepcion. Todo en JVM puro con
 * directorios temporales (`TemporaryFolder`), sin Robolectric ni Android.
 */
class FileDishImageCacheTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    // Escenario 5: el fichero vive bajo la raiz inyectada, se llama `<clave>.webp` y el contenido es
    // identico al escrito.
    @Test
    fun ida_y_vuelta_con_la_clave_canonica_como_nombre_de_fichero() {
        val key = "dish-image-v2-arroz-con-pollo-criollo"
        val cache = FileDishImageCache(cacheRoot())
        val bytes = webpBytes(64)

        assertTrue(cache.write(key, bytes))

        val file = File(cacheRoot(), "$key.webp")
        assertTrue("El fichero debe vivir bajo la raiz inyectada", file.isFile)
        assertArrayEquals(bytes, file.readBytes())
        assertArrayEquals(bytes, cache.read(key))
        assertEquals(listOf("$key.webp"), cache.entries().map { it.fileName })
        assertEquals(bytes.size.toLong(), cache.totalBytes())
    }

    @Test
    fun read_devuelve_null_sin_crear_nada_cuando_no_hay_fichero() {
        val cache = FileDishImageCache(cacheRoot())

        assertNull(cache.read("dish-image-v2-no-existe"))
        assertEquals(0L, cache.totalBytes())
        assertTrue(cache.entries().isEmpty())
    }

    // La LRU se apoya en la recencia: al acertar se actualiza `lastModified` con el reloj inyectado.
    @Test
    fun al_acertar_se_actualiza_la_recencia() {
        val cache = FileDishImageCache(cacheRoot(), now = { 5_000L })
        assertTrue(cache.write("clave", webpBytes()))
        File(cacheRoot(), "clave.webp").setLastModified(1_000L)

        assertArrayEquals(webpBytes(), cache.read("clave"))

        assertEquals(5_000L, File(cacheRoot(), "clave.webp").lastModified())
    }

    @Test
    fun la_escritura_es_atomica_y_no_deja_temporales() {
        val cache = FileDishImageCache(cacheRoot())
        assertTrue(cache.write("clave", webpBytes()))

        assertEquals(
            "Solo el fichero definitivo, ningun temporal",
            listOf("clave.webp"),
            cacheRoot().listFiles()!!.map { it.name }.sorted(),
        )
    }

    // Escenario 6: con un tope inyectado de 300 bytes y tres ficheros de 100 con `lastModified`
    // crecientes, escribir un cuarto borra el mas antiguo y el total del directorio queda <= tope.
    @Test
    fun la_eviccion_respeta_el_tope_y_borra_los_mas_antiguos() {
        val cache = FileDishImageCache(cacheRoot(), maxBytes = 300L, now = { 0L })
        assertTrue(cache.write("a", ByteArray(100)))
        assertTrue(cache.write("b", ByteArray(100)))
        assertTrue(cache.write("c", ByteArray(100)))
        File(cacheRoot(), "a.webp").setLastModified(1_000L)
        File(cacheRoot(), "b.webp").setLastModified(2_000L)
        File(cacheRoot(), "c.webp").setLastModified(3_000L)

        assertTrue(cache.write("d", ByteArray(100)))

        assertFalse("El mas antiguo debe evictarse", File(cacheRoot(), "a.webp").exists())
        assertTrue("El recien escrito sobrevive", File(cacheRoot(), "d.webp").exists())
        assertTrue("Los mas recientes sobreviven", File(cacheRoot(), "b.webp").exists())
        assertTrue("Los mas recientes sobreviven", File(cacheRoot(), "c.webp").exists())
        assertTrue("El total del directorio debe caber en el tope", cache.totalBytes() <= 300L)
    }

    // Escenario 7: la eviccion solo considera y solo borra `<clave>.webp` dentro de su raiz.
    @Test
    fun la_eviccion_no_sale_de_su_raiz_ni_toca_otra_extension() {
        val foreignWebp = File(temporaryFolder.root, "otro.webp")
        foreignWebp.writeBytes(ByteArray(50))
        val foreignDatabase = File(temporaryFolder.root, "sazon.db")
        foreignDatabase.writeBytes(ByteArray(50))

        val cache = FileDishImageCache(cacheRoot(), maxBytes = 10L, now = { 0L })
        assertTrue(cache.write("a", ByteArray(100)))
        File(cacheRoot(), "notas.txt").writeBytes(ByteArray(100))

        // Fuerza una pasada de eviccion con una escritura que supera el tope inyectado.
        assertTrue(cache.write("b", ByteArray(100)))

        assertFalse("La victima es el webp mas antiguo de la raiz", File(cacheRoot(), "a.webp").exists())
        assertTrue("El webp de fuera de la raiz queda intacto", foreignWebp.exists())
        assertTrue("La base de datos de fuera de la raiz queda intacta", foreignDatabase.exists())
        assertTrue(
            "Un fichero de otra extension dentro de la raiz queda intacto",
            File(cacheRoot(), "notas.txt").exists(),
        )
        assertTrue(File(cacheRoot(), "b.webp").exists())
    }

    // Escenario 11: una raiz no utilizable no lanza; write falla y read devuelve null.
    @Test
    fun con_una_raiz_no_utilizable_no_lanza_y_no_persiste() {
        val blocker = File(temporaryFolder.root, "raiz-ocupada")
        blocker.writeText("un fichero regular ocupa la ruta de la raiz")
        assertTrue(blocker.isFile)
        val cache = FileDishImageCache(blocker)

        assertFalse("write debe degradar a false", cache.write("clave", webpBytes()))
        assertNull("read debe degradar a null", cache.read("clave"))
        assertEquals(0L, cache.totalBytes())
        assertTrue(cache.entries().isEmpty())
        assertTrue("La raiz sigue siendo un fichero intacto", blocker.isFile)
    }

    // Escenario 11 (variante): un directorio ocupando el nombre del fichero destino tampoco lanza.
    @Test
    fun no_lanza_si_el_destino_es_un_directorio() {
        val cache = FileDishImageCache(cacheRoot())
        assertTrue(File(cacheRoot(), "clave.webp").mkdirs())

        assertFalse(cache.write("clave", webpBytes()))
        assertNull(cache.read("clave"))
    }

    private fun cacheRoot() = File(temporaryFolder.root, "dish-images")
}

/** Bytes con cabecera WebP (`RIFF`...`WEBP`), el formato que fija la fase 2. */
private fun webpBytes(size: Int = 12): ByteArray {
    val bytes = ByteArray(size.coerceAtLeast(12))
    "RIFF".toByteArray().copyInto(bytes, 0)
    "WEBP".toByteArray().copyInto(bytes, 8)
    return bytes
}
