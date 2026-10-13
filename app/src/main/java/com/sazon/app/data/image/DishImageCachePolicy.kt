package com.sazon.app.data.image

/**
 * Politica **pura** de la cache de imagenes de plato en un unico sitio (fase 3 de
 * `docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`): subdirectorio, extension,
 * tope de disco, tope de memoria, nombre de fichero y seleccion de victimas de la eviccion. Sin
 * Android, sin I/O y sin estado: la misma entrada produce siempre la misma salida (mismo patron que
 * `data.ai.image.DishImagePolicy` y que la logica pura de las fotos de plato).
 *
 * La clave canonica de la fase 1 **es** el nombre de fichero: su alfabeto
 * (`[a-z0-9-]`, versionado `dish-image-v1-...`) ya es apto, de modo que dos titulos equivalentes
 * comparten fichero por construccion y no hace falta rehash ni indice.
 */
object DishImageCachePolicy {

    /** Subdirectorio de la cache **bajo `cacheDir`**, nunca el almacenamiento de Room. */
    const val SUBDIRECTORY = "dish-images"

    /** Extension del fichero cacheado: el formato que fija el adaptador de la fase 2 (WebP). */
    const val EXTENSION = "webp"

    /** Tope de disco en MiB (`80 MB`), el presupuesto acordado para la cache de imagenes. */
    const val MAX_DISK_BYTES = 80L * 1024 * 1024

    /** Tope de la LRU en memoria; mismo valor que el precedente aceptado de LRU en memoria. */
    const val MAX_MEMORY_ENTRIES = 16

    /** Nombre de fichero de una clave canonica: `<cacheKey>.webp` (sin rehash ni reindice). */
    fun fileNameFor(cacheKey: String): String = "$cacheKey.$EXTENSION"

    /**
     * Victimas de una pasada de eviccion, **puras y deterministas**: ordena [candidates] por
     * `lastModifiedMillis` ascendente con desempate por nombre ascendente (la LRU con toque de
     * recencia, `read`) y devuelve los nombres a borrar, **los mas antiguos primero**, hasta que
     * `total - borrados <= maxBytes`.
     *
     * [maxBytes] es el **presupuesto que pueden ocupar los candidatos**; el llamante
     * (`FileDishImageCache.write`) le resta el tamano del fichero recien escrito, que **no** es
     * candidato: asi una imagen nueva nunca se borra en su propia pasada y, si por si sola superara
     * el tope, la cache queda con ese unico fichero. Devuelve lista vacia si no hay candidatos o si
     * ya caben en el presupuesto.
     */
    fun evictionVictims(candidates: List<DishImageCacheEntry>, maxBytes: Long): List<String> {
        var remaining = candidates.sumOf { it.sizeBytes }
        if (remaining <= maxBytes) return emptyList()

        val victims = mutableListOf<String>()
        val oldestFirst = candidates.sortedWith(
            compareBy({ it.lastModifiedMillis }, { it.fileName }),
        )
        for (candidate in oldestFirst) {
            if (remaining <= maxBytes) break
            victims += candidate.fileName
            remaining -= candidate.sizeBytes
        }
        return victims
    }
}

/**
 * Fichero de la cache tal como lo ve la politica: nombre, tamano y marca de recencia
 * (`lastModified`). Datos puros para que la seleccion de victimas se pruebe en JVM sin tocar disco.
 */
data class DishImageCacheEntry(
    val fileName: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
)
