package com.sazon.app.domain.search

import com.sazon.app.domain.generation.GenerationResult

/**
 * Memo **de sesion** de la Busqueda con IA: `consulta normalizada -> resultado`.
 *
 * Logica pura (sin Android, sin red, sin persistencia) y testeable en JVM. Materializa la politica de
 * `docs/domain-model.md`: "no se persiste un cache consulta->resultado; solo se memoiza en memoria
 * durante la sesion". El mapa vive en memoria y **no** sobrevive al proceso; [AiSearchService] decide
 * que solo se guardan los resultados **exitosos** (un fallo no se memoiza, para que "Reintentar" vuelva
 * a llamar al generador).
 */
class SearchMemo {

    private val entries = mutableMapOf<String, GenerationResult>()

    /** Devuelve el resultado memoizado para [key], o `null` si no hay entrada. */
    fun get(key: String): GenerationResult? = entries[key]

    /** Guarda [result] bajo [key] (la ultima escritura gana). */
    fun put(key: String, result: GenerationResult) {
        entries[key] = result
    }

    companion object {
        /** Clave canonica de una consulta: recortada y en minusculas. */
        fun key(query: String): String = query.trim().lowercase()
    }
}
