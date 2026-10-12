package com.sazon.app.data.ai

/**
 * Formateo **puro** de las lineas del modo de diagnostico verboso (`verboseDiagnostics`), para que
 * `MainActivity` sea una capa delgada (solo itera y llama a `Log.d`) y esto se pruebe en JVM.
 *
 * Dos formatos, uno por linea de Logcat:
 *
 * - `discard[i/n] REASON[/CAUSA] title="…"` (o `title=(sin titulo)`): una por descarte.
 * - `raw[i/n] <trozo>`: `n` trozos de como mucho [MAX_CHUNK_CHARS] caracteres, de modo que
 *   concatenarlos reconstruye el `content` original.
 *
 * Nunca emite la clave ni la cabecera `Authorization`: el `content` del modelo y los titulos no las
 * contienen (se registran solo con el interruptor activo).
 */
object AiVerboseDiagnostics {

    /** Logcat trunca ~4000 char; 3_000 deja margen a la etiqueta y al prefijo `raw[i/n] `. */
    const val MAX_CHUNK_CHARS = 3_000

    /** Placeholder del titulo cuando el elemento no llego a decodificarse. */
    const val MISSING_TITLE = "(sin titulo)"

    /**
     * `raw[i/n] <trozo>` por cada trozo del `content`, cada uno de como mucho [maxChars] caracteres
     * (mas el prefijo). No corta un par suplente (no deja un `high surrogate` huerfano). `null` o
     * vacio -> lista vacia.
     */
    fun rawLines(rawContent: String?, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
        if (rawContent.isNullOrEmpty()) return emptyList()
        require(maxChars > 0) { "maxChars debe ser positivo" }
        val chunks = chunks(rawContent, maxChars)
        return chunks.mapIndexed { index, chunk -> "raw[${index + 1}/${chunks.size}] $chunk" }
    }

    /**
     * `discard[i/n] REASON[/CAUSA] title="…"` por cada descarte, con el titulo saneado (newlines a
     * espacio) y `title=(sin titulo)` cuando falta.
     */
    fun discardLines(discards: List<AiDiscardDetail>): List<String> =
        discards.mapIndexed { index, detail ->
            val cause = detail.cause?.let { "/${it.name}" }.orEmpty()
            val title = detail.title?.let { "title=\"${sanitize(it)}\"" } ?: "title=$MISSING_TITLE"
            "discard[${index + 1}/${discards.size}] ${detail.reason}$cause $title"
        }

    /** Trocea sin partir un par suplente UTF-16 (el par entero pasa al trozo siguiente). */
    private fun chunks(text: String, maxChars: Int): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + maxChars, text.length)
            // Si el corte cae entre un high y un low surrogate, se retrocede uno para no partirlo.
            if (end < text.length && end > start + 1 &&
                Character.isHighSurrogate(text[end - 1]) && Character.isLowSurrogate(text[end])
            ) {
                end -= 1
            }
            result += text.substring(start, end)
            start = end
        }
        return result
    }

    /** Sanea el titulo: cualquier salto de linea se convierte en un espacio. */
    private fun sanitize(title: String): String =
        title.replace(Regex("[\\r\\n]+"), " ").trim()
}
