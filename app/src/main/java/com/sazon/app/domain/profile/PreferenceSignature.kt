package com.sazon.app.domain.profile

import java.security.MessageDigest

/**
 * Firma determinista del Perfil de preferencias.
 *
 * Se guardara junto a cada Lote de sugerencias para decidir, sin adivinar, si el lote ha quedado
 * obsoleto. La firma **excluye la identidad** por construccion: recibe un [PreferenceProfile], no un
 * [UserProfile], de modo que el compilador garantiza que `displayName` y `avatarColorId` no pueden
 * entrar en el calculo. Tambien excluye `updatedAt`: es un sello temporal, no una preferencia, y si
 * entrara, editar solo el nombre invalidaria el lote.
 *
 * Logica pura de dominio: sin Android, sin Room, sin corrutinas y sin dependencias nuevas
 * (`java.security.MessageDigest` es JDK estandar).
 */
object PreferenceSignature {

    /**
     * Version del algoritmo de canonicalizacion/hash. Si cambia, las firmas guardadas dejan de
     * coincidir y el lote se considerara obsoleto una sola vez (comportamiento seguro).
     */
    private const val ALGORITHM_VERSION = "v1"

    /**
     * Separador de campos y de elementos de lista.
     *
     * Se elige U+001F (unit separator) porque los `id` de la Taxonomia son `snake_case`
     * (`[a-z0-9_]`, sin acentos) y nunca lo contienen, asi que la concatenacion canonica no puede
     * producir colisiones entre campos distintos.
     */
    private const val SEPARATOR = "\u001F"

    /**
     * Devuelve la firma SHA-256 en hexadecimal minuscula (64 caracteres) del Perfil de preferencias.
     *
     * Las listas se tratan como **conjuntos**: se eliminan duplicados y se ordenan, porque el orden
     * de seleccion no es semantico y no debe provocar refrescos innecesarios del Lote.
     */
    fun of(preferences: PreferenceProfile): String {
        val canonical = buildString {
            append(ALGORITHM_VERSION)
            append(SEPARATOR)
            append(preferences.dietPatternId)
            append(SEPARATOR)
            appendSet(preferences.hardRestrictionIds)
            append(SEPARATOR)
            appendSet(preferences.softPreferenceIds)
            append(SEPARATOR)
            appendSet(preferences.cuisineIds)
            append(SEPARATOR)
            appendSet(preferences.preferredIngredientIds)
            append(SEPARATOR)
            appendSet(preferences.dislikedIngredientIds)
            append(SEPARATOR)
            append(preferences.difficultyId)
            append(SEPARATOR)
            append(preferences.timeBucketId)
            append(SEPARATOR)
            append(preferences.defaultServings)
        }
        return sha256Hex(canonical)
    }

    private fun StringBuilder.appendSet(ids: List<String>) {
        append(ids.distinct().sorted().joinToString(SEPARATOR))
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (byte in digest) {
                append("%02x".format(byte.toInt() and 0xFF))
            }
        }
    }
}
