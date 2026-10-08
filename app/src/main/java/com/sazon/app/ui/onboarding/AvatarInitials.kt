package com.sazon.app.ui.onboarding

/**
 * Iniciales del Avatar, puras y testeables en JVM.
 *
 * Regla de `docs/domain-model.md`: como maximo **2 iniciales** en mayusculas. Con un nombre vacio o
 * solo espacios devuelve `null`, que la UI traduce a un icono generico de persona (nunca iniciales
 * vacias).
 */
object AvatarInitials {

    fun of(displayName: String): String? {
        val words = displayName.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        return words.take(MAX_INITIALS)
            .map { it.first().uppercaseChar() }
            .joinToString(separator = "")
            .take(MAX_INITIALS)
    }

    private const val MAX_INITIALS = 2

    private val WHITESPACE = Regex("\\s+")
}
