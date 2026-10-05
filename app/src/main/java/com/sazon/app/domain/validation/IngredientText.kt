package com.sazon.app.domain.validation

import java.text.Normalizer

/**
 * Normalizacion de texto y matching por keywords para el validador.
 *
 * El matching es deliberadamente **conservador**: se busca contencion (substring) de cada keyword
 * sobre el nombre normalizado. Las keywords de la Taxonomia son raices ("pan", "trigo", "nata"), de
 * modo que se prefiere un falso positivo (descartar y registrar) a un falso negativo (servir gluten).
 * Es deuda aceptable del MVP; la decision fina vive en `docs/domain/taxonomy.json` cambiando keywords.
 *
 * Logica pura (JDK), sin dependencias de Android.
 */
internal object IngredientText {

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val WHITESPACE = Regex("\\s+")

    /** Minusculas, sin acentos, espacios colapsados y recortados. */
    fun normalize(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed
            .replace(DIACRITICS, "")
            .replace(WHITESPACE, " ")
            .trim()
    }

    /** `true` si [text] contiene (substring) alguna keyword, comparando ambos normalizados. */
    fun matchesAny(text: String, keywords: List<String>): Boolean {
        val normalizedText = normalize(text)
        if (normalizedText.isEmpty()) return false
        return keywords.any { keyword ->
            val normalizedKeyword = normalize(keyword)
            normalizedKeyword.isNotEmpty() && normalizedText.contains(normalizedKeyword)
        }
    }
}
