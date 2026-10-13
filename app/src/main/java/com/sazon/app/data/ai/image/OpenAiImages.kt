package com.sazon.app.data.ai.image

import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs del wire de `/images/generations` (mismo estilo tolerante que `OpenAiChat`: el cliente
 * serializa con `ignoreUnknownKeys`).
 *
 * La peticion lleva **solo** `model`, `prompt` y `size`: ni `n`, ni `response_format`, ni `stream`
 * (no-goals de `dish-image-generator`). La respuesta interesa por `data[i].b64_json`; `url`,
 * `revised_prompt` y cualquier clave desconocida se ignoran.
 *
 * La fase 6 (`/images/edits`) **reutiliza** [ImageGenerationResponse]/[ImageGenerationDatum] (la
 * respuesta se asume con la misma forma `data[].b64_json`) y anade [editsFormData], el constructor del
 * cuerpo **multipart**; la peticion JSON de generacion no cambia.
 */
@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    val size: String,
)

/** Nombre de la parte de fichero (la referencia de estilo) en el multipart de `/images/edits`. */
const val EDIT_REFERENCE_PART = "image"

/**
 * Cuerpo **multipart** de `POST /images/edits` (fase 6). Contiene **solo** las cuatro partes del
 * contrato OpenAI-compatible: `model` = [DishImagePolicy.EDIT_MODEL_ALIAS] (`image-edit`), `prompt` =
 * el prompt de la peticion (identico, sin reinterpretar), `size` = [DishImagePolicy.IMAGE_SIZE] y
 * [EDIT_REFERENCE_PART] con los **bytes** de la referencia, su `Content-Type` ([DishImageStyleReference.mediaType])
 * y su `filename` ([DishImageStyleReference.fileName]). Sin `n`, `mask`, `response_format`, `stream`
 * ni ningun campo de dominio. Puro: no hace I/O ni conoce la red.
 */
fun editsFormData(
    reference: DishImageStyleReference,
    model: String,
    prompt: String,
    size: String,
): List<PartData> = formData {
    append("model", model)
    append("prompt", prompt)
    append("size", size)
    append(
        EDIT_REFERENCE_PART,
        reference.bytes,
        Headers.build {
            append(HttpHeaders.ContentType, reference.mediaType)
            append(HttpHeaders.ContentDisposition, "filename=\"${reference.fileName}\"")
        },
    )
}

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
