package com.sazon.app.data.image

/**
 * LRU en memoria de la imagen de plato: guarda los **bytes ya codificados** (WebP) por clave canonica,
 * no bitmaps decodificados (la decodificacion y su LRU de bitmaps son de la fase 4, donde se pinta).
 *
 * Mismo patron que el precedente aceptado de LRU en memoria (`data`): `LinkedHashMap` en modo
 * **acceso** (`accessOrder = true`, el mas reciente al final) con eviccion del mas antiguo al superar
 * [maxEntries]. El acceso va bajo `synchronized` porque el resolutor puede llamarse desde varias
 * corrutinas a la vez.
 */
class MemoryDishImageCache(
    private val maxEntries: Int = DishImageCachePolicy.MAX_MEMORY_ENTRIES,
) {

    private val entries = object : LinkedHashMap<String, ByteArray>(
        maxEntries.coerceAtLeast(1),
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>): Boolean =
            size > maxEntries
    }

    /** Numero de entradas vivas; nunca supera [maxEntries]. */
    val size: Int get() = synchronized(entries) { entries.size }

    /** Bytes de [cacheKey], o `null`; un acceso cuenta como uso (LRU). */
    fun get(cacheKey: String): ByteArray? = synchronized(entries) { entries[cacheKey] }

    /** Inserta o reemplaza los bytes de [cacheKey], descartando la entrada menos usada si procede. */
    fun put(cacheKey: String, bytes: ByteArray) {
        synchronized(entries) { entries[cacheKey] = bytes }
    }

    /** Vacia la cache (no toca el disco). */
    fun clear() {
        synchronized(entries) { entries.clear() }
    }
}
