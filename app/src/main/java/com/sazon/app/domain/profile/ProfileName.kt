package com.sazon.app.domain.profile

/**
 * Regla de negocio del nombre del Perfil, en un unico sitio.
 *
 * El nombre es obligatorio: entre [MIN_LENGTH] y [MAX_LENGTH] caracteres **tras recortar**.
 */
object ProfileName {

    const val MIN_LENGTH = 1
    const val MAX_LENGTH = 40

    fun normalize(raw: String): String = raw.trim()

    fun isValid(raw: String): Boolean =
        normalize(raw).length in MIN_LENGTH..MAX_LENGTH
}
