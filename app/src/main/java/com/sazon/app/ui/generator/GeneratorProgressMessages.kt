package com.sazon.app.ui.generator

import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Microcopy **puro** del estado de carga del Generador (`docs/design/stitch-prompt.md` seccion C).
 *
 * Sin Compose ni Android (solo `domain.taxonomy`), testeable en JVM. Compone la cabecera fija y la
 * lista de sublineas rotatorias a partir de los ajustes elegidos:
 * - "Combinando tus ingredientes" siempre.
 * - "Ajustando a {maxMinutes} minutos" si la franja elegida tiene `maxMinutes`; "Ajustando el tiempo"
 *   si no lo tiene o si el `id` no resuelve en la Taxonomia.
 * - "Evitando tus alergias…" solo si hay restricciones duras marcadas.
 */
object GeneratorProgressMessages {

    /** Cabecera fija del estado de generacion. */
    const val HEADLINE = "Buscando recetas para ti…"

    fun subLines(settings: GeneratorSettingsState, taxonomy: Taxonomy): List<String> {
        val maxMinutes = taxonomy.timeBucket(settings.timeBucketId)?.maxMinutes
        return buildList {
            add("Combinando tus ingredientes")
            add(
                if (maxMinutes != null) {
                    "Ajustando a $maxMinutes minutos"
                } else {
                    "Ajustando el tiempo"
                },
            )
            if (settings.hardRestrictionIds.isNotEmpty()) {
                add("Evitando tus alergias…")
            }
        }
    }
}
