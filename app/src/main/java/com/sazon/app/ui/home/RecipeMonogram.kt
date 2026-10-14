package com.sazon.app.ui.home

/**
 * Monograma de iniciales de una receta: la logica **pura** que sostiene la miniatura 96x96 de la lista
 * de Favoritos (`RecipeListCard` de `DESIGN.md`, ADR 0006 decision 6).
 *
 * Regla (Escenario 5 de `docs/specs/recipe-card-typographic.md`):
 * - se toman las **palabras significativas** del titulo (todas menos los conectores `de`, `del`, `la`,
 *   `el`, `los`, `las`, `con`, `al`, `y`, `a`, `en`, `para`, `un`, `una`, `o`, `e`) en el orden en que
 *   aparecen;
 * - el monograma son **hasta 2 iniciales en mayuscula**: la de la **primera** palabra significativa y
 *   la de la **ultima**; con una sola palabra significativa hay **1** inicial;
 * - un titulo vacio, en blanco o sin ninguna palabra significativa devuelve `null` (la UI no pinta
 *   monograma).
 *
 * La inicial **ultima** (y no la segunda) es lo que reproduce los vectores de las capturas aceptadas,
 * p. ej. `Arroz con pollo criollo` -> `AC` (A de Arroz + C de criollo): con las dos primeras palabras
 * significativas saldria `AP`, que no es el monograma de la captura. Ver el hallazgo del implementer en
 * la spec (`docs/specs/recipe-card-typographic.md`).
 *
 * Sin Compose, sin Android, sin red, sin reloj y sin `Random`: dos llamadas iguales devuelven el mismo
 * resultado. Vectores de las capturas de referencia: `Tortilla de patatas` -> `TP`,
 * `Arroz con pollo criollo` -> `AC`, `Gazpacho andaluz` -> `GA`, `Curry de garbanzos` -> `CG`,
 * `Pasta al tomate` -> `PT`.
 */
object RecipeMonogram {

    /** Palabras funcionales que no aportan inicial (en minusculas, sin signos). */
    private val CONNECTORS = setOf(
        "de", "del", "la", "el", "los", "las", "con", "al", "y", "a", "en", "para", "un", "una", "o", "e",
    )

    /** Separadores de palabra: cualquier espacio en blanco (incluido el duro `\u00A0`). */
    private val SEPARATORS =
        charArrayOf(' ', '\t', '\n', '\r', '\u000B', '\u000C', '\u00A0')

    /**
     * Iniciales del [title] (mayusculas, 1 o 2) o `null` si no hay ninguna palabra significativa. Los
     * signos de puntuacion no cuentan: se usa la primera **letra** de cada palabra.
     */
    fun of(title: String): String? {
        val initials = title
            .split(*SEPARATORS)
            .asSequence()
            .map { word -> word.filter { it.isLetter() } }
            .filter { it.isNotEmpty() && it.lowercase() !in CONNECTORS }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .toList()
        return when (initials.size) {
            0 -> null
            1 -> initials[0].toString()
            else -> "${initials.first()}${initials.last()}"
        }
    }
}
