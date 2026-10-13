package com.sazon.app.data.ai.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs del wire de `/images/generations` (mismo estilo tolerante que `OpenAiChat`: el cliente
 * serializa con `ignoreUnknownKeys`).
 *
 * La peticion lleva **solo** `model`, `prompt` y `size`: ni `n`, ni `response_format`, ni `stream`
 * (no-goals de `dish-image-generator`). La respuesta interesa por `data[i].b64_json`; `url`,
 * `revised_prompt` y cualquier clave desconocida se ignoran.
 */
@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    val size: String,
)

@Serializable
data class ImageGenerationDatum(
    @SerialName("b64_json")
    val b64Json: String? = null,
)

/** Respuesta: `data` puede faltar o venir vacia; se degrada a lista vacia. */
@Serializable
data class ImageGenerationResponse(
    val data: List<ImageGenerationDatum> = emptyList(),
)
