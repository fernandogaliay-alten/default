package com.sazon.app.ui.favorites

/**
 * Criterios de orden de la lista de Favoritos (chips de la pestana).
 *
 * Logica pura: sin Compose ni Android. [fromKey] permite persistir/restaurar el criterio con un
 * `Saver`; una clave desconocida degrada a [RECENT].
 */
enum class FavoritesSort(val label: String, val key: String) {
    RECENT("Añadidas recientemente", "recent"),
    TIME("Tiempo", "time"),
    DIFFICULTY("Dificultad", "difficulty");

    companion object {
        fun fromKey(key: String): FavoritesSort =
            entries.firstOrNull { it.key == key } ?: RECENT
    }
}
