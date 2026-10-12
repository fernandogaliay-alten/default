package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.RecipeValidator
import com.sazon.app.domain.validation.ValidationOutcome

/**
 * Decode **puro** (sin HTTP) del `content` que devuelve el modelo dentro del envelope OpenAI.
 *
 * Pasos:
 * 1. `content` `null` o en blanco -> lista vacia (no es un error del proveedor).
 * 2. Normalizacion minima: recorte y, si envuelve, se quita **una** valla markdown envolvente
 *    (```` ```json ... ``` ```` o ```` ``` ... ``` ````).
 * 3. `RecipeValidator.validateResponse` con las restricciones **neutras** de [AiProviderPolicy]
 *    (el proveedor **no** pre-filtra por dieta ni restricciones duras).
 * 4. **Nunca lanza** en estos casos ni reintenta: `Malformed` -> lista vacia; si no, las aceptadas
 *    (posiblemente vacias). Una receta ilegible dentro de un lote legible se descarta sin tumbar el
 *    resto (parseo elemento a elemento del validador).
 */
object AiRecipePayload {

    fun decode(content: String?, taxonomy: Taxonomy): List<Recipe> {
        val trimmed = content?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyList()

        val normalized = stripMarkdownFence(trimmed)
        return when (
            val outcome = RecipeValidator.validateResponse(
                rawResponse = normalized,
                taxonomy = taxonomy,
                constraints = AiProviderPolicy.NEUTRAL_CONSTRAINTS,
            )
        ) {
            is ValidationOutcome.ValidationCompleted -> outcome.accepted
            is ValidationOutcome.Malformed -> emptyList()
        }
    }

    /**
     * Quita una valla markdown envolvente (```json / ``` ... ```). Si el texto no empieza por una
     * valla o no la cierra, se devuelve tal cual (recortado).
     */
    private fun stripMarkdownFence(text: String): String {
        if (!text.startsWith("```")) return text

        val withoutOpening = text.removePrefix("```")
            .let { body ->
                if (body.startsWith("json", ignoreCase = true)) body.substring(4) else body
            }
        val closingIndex = withoutOpening.lastIndexOf("```")
        if (closingIndex < 0) return text

        return withoutOpening.substring(0, closingIndex).trim()
    }
}
