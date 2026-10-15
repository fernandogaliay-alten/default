package com.sazon.app.ui.theme

import java.io.File

/**
 * Parser **compartido** del frontmatter de `DESIGN.md` para los tests JVM de tokens
 * (`DesignTokensTest`, `MotionTokensTest`). Extraido tal cual de `DesignTokensTest` (mismo codigo,
 * visibilidad `internal`) para no duplicar la logica.
 */

/** Lee `DESIGN.md` desde la ruta publicada por la propiedad de sistema `design.tokens.path`. */
internal fun designMarkdown(): String {
    val path = checkNotNull(System.getProperty("design.tokens.path")) {
        "Falta la propiedad de sistema design.tokens.path"
    }
    return File(path).readText()
}

/** Devuelve las lineas internas del bloque frontmatter YAML delimitado por `---`. */
internal fun frontmatter(markdown: String): List<String> {
    val lines = markdown.lines()
    val start = lines.indexOfFirst { it.trim() == "---" }
    check(start >= 0) { "DESIGN.md no tiene frontmatter YAML" }
    val end = (start + 1 until lines.size).firstOrNull { lines[it].trim() == "---" }
        ?: error("El frontmatter de DESIGN.md no se cierra con ---")
    return lines.subList(start + 1, end)
}

/** Extrae las entradas `clave: valor` de una seccion plana de dos espacios del frontmatter. */
internal fun frontmatterSectionMap(markdown: String, section: String): Map<String, String> {
    val lines = frontmatter(markdown)
    val start = lines.indexOfFirst { it.trimEnd() == "$section:" }
    check(start >= 0) { "DESIGN.md no tiene la seccion '$section' en el frontmatter" }
    val result = linkedMapOf<String, String>()
    for (i in start + 1 until lines.size) {
        val line = lines[i]
        if (line.isBlank()) continue
        if (!line.startsWith("  ")) break
        if (line.startsWith("    ")) continue
        val trimmed = line.trim()
        val separator = trimmed.indexOf(':')
        if (separator <= 0) continue
        val key = trimmed.substring(0, separator).trim()
        val value = trimmed.substring(separator + 1).trim().trim('"')
        result[key] = value
    }
    return result
}
