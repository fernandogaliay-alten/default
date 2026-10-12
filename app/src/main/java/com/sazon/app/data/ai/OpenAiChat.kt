@file:OptIn(ExperimentalSerializationApi::class)

package com.sazon.app.data.ai

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs del envelope OpenAI de `/chat/completions`.
 *
 * Son tolerantes (`ignoreUnknownKeys` en el cliente): solo interesa `choices[0].message.content`, el
 * texto JSON con el contrato de la Taxonomia. Se envia `response_format` con el unico `type` de hoy
 * (`{"type":"json_object"}`, `docs/adr/0003-ai-provider.md` y `ai-response-format`); **no** se envia
 * `json_schema`, `stream`, `temperature` ni `max_tokens` (ADR 0003 y no-goals de `real-ai-provider`).
 */
@Serializable
data class ChatMessage(
    val role: String? = null,
    val content: String? = null,
)

/**
 * Formato de respuesta pedido al proveedor. Tolerante a valores inesperados porque solo se
 * **serializa**; el unico valor usado hoy es [JSON_OBJECT].
 */
@Serializable
data class ResponseFormat(val type: String) {
    companion object {
        /** `{"type":"json_object"}`: pide JSON parseable (forma, no dominio). */
        val JSON_OBJECT = ResponseFormat(AiProviderPolicy.RESPONSE_FORMAT_JSON_OBJECT)
    }
}

/**
 * Peticion: `{"model":"chat","messages":[{"role":"system",...},{"role":"user",...}]}`, mas
 * `"response_format":{"type":"json_object"}` cuando se activa.
 *
 * `@EncodeDefault(NEVER)` es deliberado: el cliente serializa con `encodeDefaults = true`, asi que sin
 * la anotacion la clave se emitiria como `"response_format":null` cuando no se usa. Con ella, la clave
 * se **omite** y solo aparece cuando [responseFormat] vale [ResponseFormat.JSON_OBJECT].
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val responseFormat: ResponseFormat? = null,
)

@Serializable
data class ChatChoice(
    val message: ChatMessage = ChatMessage(),
)

/** Respuesta: `choices` puede faltar o venir vacio; el proveedor degrada a lista vacia. */
@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)
