package com.sazon.app.data.image

import java.io.File

/**
 * Cache de disco de imagenes de plato: ficheros WebP en una raiz **inyectada** (`java.io.File`, sin
 * Android) cuyo nombre es la clave canonica (`DishImageCachePolicy.fileNameFor`).
 *
 * - La raiz se recibe por constructor: en produccion `AndroidDishImageCache.root(context)`
 *   (`cacheDir/dish-images`) y en tests un directorio temporal, por eso `data.image` **no** necesita
 *   `Context`.
 * - [read] devuelve `null` si el fichero no existe o no se puede leer; al acertar **toca la recencia**
 *   (`lastModified = now()`), de modo que la eviccion es una LRU real y no un FIFO. Si el sistema de
 *   ficheros no soporta `lastModified`, la LRU degrada a FIFO (aceptado).
 * - [write] hace una escritura **atomica** (fichero temporal en la misma raiz + `renameTo`) para no
 *   dejar nunca un fichero a medio escribir servible, y despues una **pasada de eviccion** que solo
 *   considera `<clave>.webp` dentro de [root].
 * - **Nunca lanza** por I/O: [read] devuelve `null` y [write] devuelve `false` (limpiando el
 *   temporal). La persistencia es *best-effort*.
 */
class FileDishImageCache(
    private val root: File,
    private val maxBytes: Long = DishImageCachePolicy.MAX_DISK_BYTES,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Bytes cacheados de [cacheKey], o `null` si no hay copia legible; al acertar toca la recencia. */
    fun read(cacheKey: String): ByteArray? = try {
        val file = fileFor(cacheKey)
        if (!file.isFile) {
            null
        } else {
            val bytes = file.readBytes()
            file.setLastModified(now())
            bytes
        }
    } catch (exception: Exception) {
        null
    }

    /**
     * Persiste [bytes] con [cacheKey] (escritura atomica) y ejecuta la eviccion. Devuelve `false` si
     * no se pudo crear la raiz, escribir o renombrar; el temporal se limpia en cualquier caso.
     */
    fun write(cacheKey: String, bytes: ByteArray): Boolean {
        val target = fileFor(cacheKey)
        return try {
            if (!root.isDirectory && !root.mkdirs()) return false

            val temporary = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, root)
            try {
                temporary.writeBytes(bytes)
                if (!temporary.renameTo(target)) return false
            } finally {
                temporary.delete()
            }

            evict(protectedFileName = target.name)
            true
        } catch (exception: Exception) {
            false
        }
    }

    /** Suma de tamanos de los ficheros `<clave>.webp` de la raiz; `0` si no hay raiz util. */
    fun totalBytes(): Long = entries().sumOf { it.sizeBytes }

    /** Ficheros regulares `<clave>.webp` **dentro** de [root]; nunca lanza ni sale del directorio. */
    fun entries(): List<DishImageCacheEntry> {
        val files = try {
            root.listFiles()
        } catch (exception: Exception) {
            null
        } ?: return emptyList()

        return files
            .filter { it.isFile && it.name.endsWith(".${DishImageCachePolicy.EXTENSION}") }
            .map { DishImageCacheEntry(it.name, it.length(), it.lastModified()) }
    }

    /**
     * Borra las victimas mas antiguas hasta que los candidatos quepan en el presupuesto restante
     * (`maxBytes - tamano del recien escrito`). El fichero recien escrito ([protectedFileName]) nunca
     * es candidato, asi que una imagen nueva no se borra en su propia pasada.
     */
    private fun evict(protectedFileName: String) {
        val allEntries = entries()
        val candidates = allEntries.filter { it.fileName != protectedFileName }
        val justWritten = allEntries.firstOrNull { it.fileName == protectedFileName }?.sizeBytes ?: 0L

        for (victim in DishImageCachePolicy.evictionVictims(candidates, maxBytes - justWritten)) {
            try {
                // El nombre viene de `entries()`: solo `<clave>.webp` dentro de la raiz.
                File(root, victim).delete()
            } catch (exception: Exception) {
                // La eviccion es best-effort: un fichero que no se puede borrar no debe romper nada.
            }
        }
    }

    private fun fileFor(cacheKey: String): File = File(root, DishImageCachePolicy.fileNameFor(cacheKey))

    private companion object {
        const val TEMP_PREFIX = "dish-image-"
        const val TEMP_SUFFIX = ".tmp"
    }
}
