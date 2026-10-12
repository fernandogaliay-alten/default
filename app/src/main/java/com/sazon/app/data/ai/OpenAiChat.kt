package com.sazon.app.data.ai

import kotlinx.serialization.Serializable

/**
 * DTOs del envelope OpenAI de `/chat/completions`.
 *
 * Son tolerantes (`ignoreUnknownKeys` en el cliente): solo interesa `choices[0].message.content`, el
 * texto JSON con el contrato de la Taxonomia. No se envia `response_format`/`json_schema`, ni
 * `stream`, ni `temperature`, ni `max_tokens` (ADR 0003 y no-goals de `real-ai-provider`).
 */
@Serializable
data class ChatMessage(
    val role: String? = null,
    val content: String? = null,
)

/** Peticion: `{"model":"chat","messages":[{"role":"system",...},{"role":"user",...}]}`. */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
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
