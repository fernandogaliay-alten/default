package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.DiscardReason
import com.sazon.app.domain.validation.RecipeConstraints
import com.sazon.app.domain.validation.RecipeValidation
import com.sazon.app.domain.validation.RecipeValidator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Decode **puro** (sin HTTP) del `content` que devuelve el modelo dentro del envelope OpenAI.
 *
 * Es la capa anti-corrupcion del adaptador: el wire del proveedor sigue el contrato de
 * `docs/domain/taxonomy.md` §4, que **no** incluye `id` ni `source` (los posee la app), mientras que
 * `Recipe` los exige. El decode decodifica el wire con [AiRecipeDraft], **completa** `id`
 * ([AiRecipeIds]) y `source = ai` y solo **despues** aplica las reglas 1-4 del Validador con
 * [RecipeValidator.validateRecipe] y las restricciones **neutras** de [AiProviderPolicy] (el
 * proveedor **no** pre-filtra por dieta ni restricciones duras).
 *
 * Devuelve un [AiDecodeResult] **tipado** en lugar de perder el detalle (motivo del descarte y
 * longitudes del contenido): asi un `EMPTY` es diagnosticable sin exponer el cuerpo.
 *
 * Pasos:
 * 1. `content` `null` o en blanco -> [AiContentState.BLANK] (listas y mapa vacios; longitudes 0).
 * 2. Normalizacion minima: recorte y, si envuelve, se quita **una** valla markdown envolvente
 *    (```` ```json ... ``` ```` o ```` ``` ... ``` ````).
 * 3. Parseo tolerante del root `{"recipes":[...]}`; si no encaja -> [AiContentState.ILLEGIBLE].
 * 4. Elemento a elemento: el que no encaja con el wire del contrato se **salta** y suma un descarte
 *    `SCHEMA`; una receta rota no tumba el resto.
 * 5. `draft.toRecipe(id = AiRecipeIds.idFor(draft, usedIds))`: la app completa `id` y `source = ai`
 *    **antes** de validar.
 * 6. `RecipeValidator.validateRecipe(..., constraints)`: `Accepted` suma a [AiDecodeResult.accepted];
 *    `Discarded` incrementa su motivo en [AiDecodeResult.discardedByReason] (el `detail` se pierde a
 *    proposito: solo se cuenta el **motivo**, nunca texto libre ni titulos).
 *
 * **Nunca lanza** en estos casos ni reintenta: una respuesta ilegible o un elemento roto dan estado
 * ilegible / se saltan.
 */
object AiRecipePayload {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decode del `content` del modelo. [constraints] por defecto son las **neutras** de
     * [AiProviderPolicy]: produccion **no** pre-filtra (el estado Bloqueado de `ai-search` se
     * conserva); el parametro existe para que un test pueda ejercer cada [DiscardReason].
     *
     * Es `internal` porque devuelve un tipo `internal` ([AiDecodeResult]); no forma parte de la API
     * publica del modulo.
     */
    internal fun decode(
        content: String?,
        taxonomy: Taxonomy,
        constraints: RecipeConstraints = AiProviderPolicy.NEUTRAL_CONSTRAINTS,
    ): AiDecodeResult {
        val raw = content ?: ""
        // Solo longitudes (nunca el cuerpo): 0/0 cuando el contenido esta en blanco.
        val contentChars = if (raw.isBlank()) 0 else raw.length
        val contentBytes = if (raw.isBlank()) 0 else raw.toByteArray(Charsets.UTF_8).size

        if (raw.isBlank()) {
            return AiDecodeResult(emptyList(), 0, emptyMap(), 0, 0, AiContentState.BLANK)
        }

        val normalized = stripMarkdownFence(raw.trim())
        val root = runCatching { json.parseToJsonElement(normalized) }.getOrNull() as? JsonObject
        val elements = root?.get("recipes") as? JsonArray
            ?: return AiDecodeResult(emptyList(), 0, emptyMap(), contentChars, contentBytes, AiContentState.ILLEGIBLE)

        val usedIds = mutableSetOf<String>()
        val accepted = mutableListOf<Recipe>()
        val discardedCounts = mutableMapOf<DiscardReason, Int>()

        elements.forEach { element ->
            val draft = runCatching {
                json.decodeFromJsonElement(AiRecipeDraft.serializer(), element)
            }.getOrNull()

            if (draft == null) {
                discardedCounts.increment(DiscardReason.SCHEMA)
                return@forEach
            }

            val recipe = draft.toRecipe(id = AiRecipeIds.idFor(draft, usedIds))
            when (val result = RecipeValidator.validateRecipe(recipe, taxonomy, constraints)) {
                is RecipeValidation.Accepted -> accepted += result.recipe
                is RecipeValidation.Discarded -> discardedCounts.increment(result.reason)
            }
        }

        return AiDecodeResult(
            accepted = accepted,
            received = elements.size,
            discardedByReason = discardedCounts.ordered(),
            contentChars = contentChars,
            contentBytes = contentBytes,
            contentState = AiContentState.RECIPES,
        )
    }

    private fun MutableMap<DiscardReason, Int>.increment(reason: DiscardReason) {
        this[reason] = (this[reason] ?: 0) + 1
    }

    /** Resumen determinista: solo motivos presentes, en el orden de [DiscardReason]. */
    private fun Map<DiscardReason, Int>.ordered(): Map<DiscardReason, Int> =
        DiscardReason.entries.mapNotNull { reason -> this[reason]?.let { reason to it } }.toMap()

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
