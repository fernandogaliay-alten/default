package com.sazon.app.ui.search

/**
 * Microcopy **puro** del estado de carga de la Busqueda con IA (sin Compose ni Android, testeable en
 * JVM). Misma animacion que el Generador; aqui solo cambia el texto.
 *
 * Las tres sublineas rotatorias salen de la captura aceptada
 * `docs/design/concepts/ai-search/busqueda-ia/`; la cabecera y la linea de prompt son las fijadas por
 * la spec.
 */
object AiSearchProgress {

    /** Cabecera fija del estado de carga (paridad con el Generador). */
    const val HEADLINE = "Buscando recetas para ti…"

    /** Tres sublineas rotatorias del ciclo de busqueda. */
    fun subLines(query: String): List<String> = listOf(
        "Interpretando antojos y texto",
        "Filtrando por alérgenos y dieta",
        "Calculando tiempos de cocción",
    )

    /** Linea de recuerdo de la consulta ya normalizada. */
    fun promptLine(query: String): String = "Prompt: “" + AiSearchQuery.normalize(query) + "”"
}
