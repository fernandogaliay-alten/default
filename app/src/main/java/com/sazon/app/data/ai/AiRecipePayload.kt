package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
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
 * Pasos:
 * 1. `content` `null` o en blanco -> lista vacia (no es un error del proveedor).
 * 2. Normalizacion minima: recorte y, si envuelve, se quita **una** valla markdown envolvente
 *    (```` ```json ... ``` ```` o ```` ``` ... ``` ````).
 * 3. Parseo tolerante del root `{"recipes":[...]}`; si no encaja -> lista vacia.
 * 4. Elemento a elemento: el que no encaja con el wire del contrato se **salta** (equivale al
 *    descarte `SCHEMA`); una receta rota no tumba el resto.
 * 5. `draft.toRecipe(id = AiRecipeIds.idFor(draft, usedIds))`: la app completa `id` y `source = ai`
 *    **antes** de validar.
 * 6. `RecipeValidator.validateRecipe(...)`: `Accepted` suma a la lista; `Discarded` se omite (el
 *    decode solo devuelve aceptadas).
 *
 * **Nunca lanza** en estos casos ni reintenta: una respuesta ilegible o un elemento roto dan lista
 * vacia / se saltan.
 */
object AiRecipePayload {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(content: String?, taxonomy: Taxonomy): List<Recipe> {
        val trimmed = content?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyList()

        val normalized = stripMarkdownFence(trimmed)
        val root = runCatching { json.parseToJsonElement(normalized) }.getOrNull() as? JsonObject
            ?: return emptyList()
        val elements = root["recipes"] as? JsonArray ?: return emptyList()

        val usedIds = mutableSetOf<String>()
        val accepted = mutableListOf<Recipe>()
        elements.forEach { element ->
            val draft = runCatching {
                json.decodeFromJsonElement(AiRecipeDraft.serializer(), element)
            }.getOrNull() ?: return@forEach

            val recipe = draft.toRecipe(id = AiRecipeIds.idFor(draft, usedIds))
            when (val result = RecipeValidator.validateRecipe(recipe, taxonomy, AiProviderPolicy.NEUTRAL_CONSTRAINTS)) {
                is RecipeValidation.Accepted -> accepted += result.recipe
                is RecipeValidation.Discarded -> Unit
            }
        }
        return accepted
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
