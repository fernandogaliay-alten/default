package com.sazon.app.ui.search

/**
 * Reglas **puras** de la consulta de la Busqueda con IA (sin Compose ni Android, testeable en JVM).
 *
 * La normalizacion (recorte) es lo que se muestra y se envia; [canSubmit] concentra la decision de
 * habilitar el envio, que depende de la conexion y de que haya texto, no de adivinar en la capa
 * Compose.
 */
object AiSearchQuery {

    /** Consulta lista para mostrar/enviar: recortada por los extremos. */
    fun normalize(query: String): String = query.trim()

    /** Solo se puede enviar con conexion y con texto no en blanco. */
    fun canSubmit(query: String, isOnline: Boolean): Boolean = isOnline && query.isNotBlank()
}
