package com.sazon.app.data.ai.image

import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * Implementacion real del puerto [DishImageGenerator] sobre **DevExpert Inference**: la fase 2 pide la
 * imagen por texto (`POST /images/generations`, modelo `image`, `size 1024x1024`) y la fase 6
 * (`docs/ai-dish-images-proposal.md` §4.2, variante C) la pide **ajustada contra una referencia de
 * estilo** (`POST /images/edits`, modelo `image-edit`, multipart con la parte `image`) cuando el
 * adaptador recibe una [DishImageStyleReference]; si el ajuste se **rechaza**, degrada al camino de
 * texto de la fase 2.
 *
 * Es **transporte + codec puros**: no conoce el modelo de recetas ni la Taxonomia (recibe un
 * [DishImageRequest] ya resuelto y usa `request.prompt` tal cual) y es la **unica** capa nueva que usa
 * Ktor. La clave llega por constructor y solo viaja en la cabecera `Authorization`: no aparece en el
 * cuerpo, ni en logs (el adaptador **no registra nada**) ni en el arbol versionado.
 *
 * Contrato de fallo (ADR 0005): **fallo o latencia = `null`, nunca excepcion** (la app conserva su
 * foto local y no muestra error por imagen); la **cancelacion** de la corrutina si se propaga en las
 * dos fases. Se hace **una sola** peticion por camino ([DishImagePolicy.MAX_ATTEMPTS] = 1). Con
 * referencia, el respaldo por texto solo se dispara ante un **rechazo definitivo** del ajuste
 * ([DishImagePolicy.editFailureRequiresTextFallback]) o una carga `2xx` inutilizable; un fallo de
 * transporte/timeout o un `401/403/429/5xx` devuelven `null` **sin** segunda llamada (el respaldo
 * fallaria igual y duplicaria el gasto). En ningun caso hay mas de **dos** intentos.
 *
 * La salida es la imagen ya **reducida y codificada** ([DishImageReducer]); el tamano objetivo y el
 * formato viven en este adaptador (fase 1 los dejo al adaptador) y la fase 3 los reutiliza. El
 * [DishImageRequest.cacheKey] **no** interviene en la peticion de red: es la identidad de cache de la
 * fase 3 y este adaptador no la reinterpreta.
 */
class DevExpertDishImageGenerator(
    private val apiKey: String,
    private val client: HttpClient,
    private val reducer: DishImageReducer = AndroidDishImageReducer,
    private val model: String = DishImagePolicy.MODEL_ALIAS,
    private val timeouts: DishImageTimeouts = DishImagePolicy.TIMEOUTS,
    /** Referencia de estilo de la casa (fase 6); `null` = comportamiento de la fase 2. */
    private val styleReference: DishImageStyleReference? = null,
    /** Interruptor del anclaje ([DishImagePolicy.STYLE_REFERENCE_ENABLED]); apagado = solo texto. */
    private val styleReferenceEnabled: Boolean = DishImagePolicy.STYLE_REFERENCE_ENABLED,
) : DishImageGenerator {

    override suspend fun generate(request: DishImageRequest): ByteArray? {
        // 1. Clave en blanco: se degrada a `null` SIN ninguna peticion HTTP (tampoco al ajuste).
        if (apiKey.isBlank()) return null

        // 2. La referencia solo cuenta con el ajuste habilitado y bytes no vacios; si no, es la fase 2.
        val reference = styleReference
            ?.takeIf { styleReferenceEnabled && it.bytes.isNotEmpty() }

        // 3. Camino de ajuste: una sola peticion; el respaldo por texto lo decide el resultado.
        if (reference != null) {
            when (val outcome = editImage(reference, request.prompt)) {
                is EditOutcome.Edited -> return outcome.bytes
                EditOutcome.FallbackToText -> Unit
                EditOutcome.Unavailable -> return null
            }
        }

        // 4. Respaldo / camino sin referencia: el de la fase 2, una sola peticion, sin reintentos.
        return generateFromText(request.prompt)
    }

    /**
     * Un intento contra `/images/edits` con el multipart de [editsFormData]. `CancellationException` se
     * relanza; transporte/timeout y `401/403/429/5xx` son [EditOutcome.Unavailable]; un rechazo
     * definitivo ([DishImagePolicy.editFailureRequiresTextFallback]) o una carga `2xx` inutilizable son
     * [EditOutcome.FallbackToText].
     */
    private suspend fun editImage(
        reference: DishImageStyleReference,
        prompt: String,
    ): EditOutcome {
        val response = try {
            client.post(DishImagePolicy.editsUrl()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                timeout {
                    connectTimeoutMillis = timeouts.connectMillis
                    socketTimeoutMillis = timeouts.socketMillis
                    requestTimeoutMillis = timeouts.requestMillis
                }
                setBody(
                    MultiPartFormDataContent(
                        editsFormData(reference, DishImagePolicy.EDIT_MODEL_ALIAS, prompt, DishImagePolicy.IMAGE_SIZE),
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return EditOutcome.Unavailable
        }

        // Fuera de 2xx: solo un rechazo definitivo dispara el respaldo (400/404/405/413/415/422).
        val status = response.status.value
        if (status !in 200..299) {
            return if (DishImagePolicy.editFailureRequiresTextFallback(status)) {
                EditOutcome.FallbackToText
            } else {
                EditOutcome.Unavailable
            }
        }

        // El fallo de transporte al leer el cuerpo del 2xx tambien degrada a `null`, sin respaldo.
        val rawBody = try {
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return EditOutcome.Unavailable
        }

        // Carga inutilizable (decode o reduce nulos/vacios) dispara el respaldo por texto.
        val rawBytes = DishImagePayload.decode(rawBody) ?: return EditOutcome.FallbackToText
        val reduced = reducer.reduceToWebp(rawBytes)
        return if (reduced != null && reduced.isNotEmpty()) {
            EditOutcome.Edited(reduced)
        } else {
            EditOutcome.FallbackToText
        }
    }

    /**
     * Camino de texto->imagen de la fase 2 (`POST /images/generations`), reutilizado tal cual por el
     * respaldo. Una sola peticion; no-2xx, transporte/timeout o carga ilegible devuelven `null`.
     */
    private suspend fun generateFromText(prompt: String): ByteArray? {
        val response = try {
            client.post(DishImagePolicy.endpointUrl()) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                timeout {
                    connectTimeoutMillis = timeouts.connectMillis
                    socketTimeoutMillis = timeouts.socketMillis
                    requestTimeoutMillis = timeouts.requestMillis
                }
                setBody(ImageGenerationRequest(model, prompt, DishImagePolicy.IMAGE_SIZE))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return null
        }

        if (response.status.value !in 200..299) return null

        val rawBody = try {
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return null
        }

        val rawBytes = DishImagePayload.decode(rawBody) ?: return null
        return reducer.reduceToWebp(rawBytes)
    }

    /** Resultado de un intento de ajuste: entrega bytes, cae al texto o degrada a `null` sin respaldo. */
    private sealed interface EditOutcome {
        class Edited(val bytes: ByteArray) : EditOutcome
        data object FallbackToText : EditOutcome
        data object Unavailable : EditOutcome
    }
}
