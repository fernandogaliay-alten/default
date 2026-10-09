package com.sazon.app.ui.results

import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Origen de la pantalla de Resultados: el **Generador** (con el recuento de ingredientes, la franja y
 * los comensales del borrador) o la **Busqueda con IA** (con la consulta escrita).
 *
 * La Busqueda con IA (`ai-search`) es una feature futura; aqui solo se deja lista la costura de su
 * subtitulo, sin conectarla (no-goal de `docs/specs/results-screen.md`).
 */
sealed interface ResultsOrigin {

    data class Generator(
        val ingredientCount: Int,
        val timeBucketId: String?,
        val servings: Int,
    ) : ResultsOrigin

    data class Search(val query: String) : ResultsOrigin
}

/** Titulo y subtitulo ya formateados de la cabecera de Resultados. */
data class ResultsHeader(val title: String, val subtitle: String)

/**
 * Formatea la cabecera de Resultados (logica **pura**, testeable en JVM, sin Compose ni Android):
 *
 * - [ResultsOrigin.Generator]: titulo "Estas son tus recetas" y subtitulo
 *   "A partir de N ingredientes · X · S comensales" (singular/plural; `X` = `maxMinutes` de la franja
 *   como "30 min", o su `label` si es `null`; si el `timeBucketId` no resuelve se omite).
 * - [ResultsOrigin.Search]: titulo "Resultados de tu busqueda" y subtitulo = consulta recortada entre
 *   comillas ("…").
 */
object ResultsHeaders {

    const val GENERATOR_TITLE = "Estas son tus recetas"
    const val SEARCH_TITLE = "Resultados de tu búsqueda"

    fun of(origin: ResultsOrigin, taxonomy: Taxonomy): ResultsHeader = when (origin) {
        is ResultsOrigin.Generator -> ResultsHeader(
            title = GENERATOR_TITLE,
            subtitle = generatorSubtitle(origin, taxonomy),
        )

        is ResultsOrigin.Search -> ResultsHeader(
            title = SEARCH_TITLE,
            subtitle = "“${origin.query.trim()}”",
        )
    }

    private fun generatorSubtitle(origin: ResultsOrigin.Generator, taxonomy: Taxonomy): String {
        val parts = buildList {
            add(ingredientCountText(origin.ingredientCount))
            timeText(origin.timeBucketId, taxonomy)?.let { add(it) }
            add(servingsText(origin.servings))
        }
        return "A partir de " + parts.joinToString(" · ")
    }

    private fun ingredientCountText(count: Int): String =
        if (count == 1) "1 ingrediente" else "$count ingredientes"

    private fun servingsText(servings: Int): String =
        if (servings == 1) "1 comensal" else "$servings comensales"

    private fun timeText(timeBucketId: String?, taxonomy: Taxonomy): String? {
        val bucket = timeBucketId?.let { taxonomy.timeBucket(it) } ?: return null
        val maxMinutes = bucket.maxMinutes
        return if (maxMinutes != null) "$maxMinutes min" else bucket.label
    }
}
