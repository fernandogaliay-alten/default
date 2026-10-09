package com.sazon.app.ui.favorites

/**
 * Modo de presentacion de la lista de Favoritos: lista vertical o rejilla de dos columnas.
 *
 * Logica pura: sin Compose ni Android. [fromKey] permite persistirlo con un `Saver`; una clave
 * desconocida degrada a [LIST].
 */
enum class FavoritesViewMode(val key: String) {
    LIST("list"),
    GRID("grid");

    companion object {
        fun fromKey(key: String): FavoritesViewMode =
            entries.firstOrNull { it.key == key } ?: LIST
    }
}
