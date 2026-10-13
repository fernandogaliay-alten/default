package com.sazon.app.data.ai.image

import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * Implementacion real del puerto [DishImageGenerator] sobre **DevExpert Inference** (`POST
 * /images/generations`, modelo `image`, `size 1024x1024`): fase 2 de
 * `docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`.
 *
 * Es **transporte + codec puros**: no conoce el modelo de recetas ni la Taxonomia (recibe un
 * [DishImageRequest] ya resuelto y usa `request.prompt` tal cual) y es la **unica** capa nueva que usa
 * Ktor. La clave llega por constructor y solo viaja en la cabecera `Authorization`: no aparece en el
 * cuerpo, ni en logs (el adaptador **no registra nada**) ni en el arbol versionado.
 *
 * Contrato de fallo (ADR 0005): **fallo o latencia = `null`, nunca excepcion** (la app conserva su
 * foto local y no muestra error por imagen); la **cancelacion** de la corrutina si se propaga (misma
 * disciplina que el adaptador de chat). Se hace **una sola** peticion por `generate`
 * ([DishImagePolicy.MAX_ATTEMPTS] = 1): reintentar duplicaria la cuota sin cambiar la experiencia.
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
) : DishImageGenerator {

    override suspend fun generate(request: DishImageRequest): ByteArray? {
        // 1. Clave en blanco: se degrada a `null` SIN ninguna peticion HTTP.
        if (apiKey.isBlank()) return null

        // 2. Una sola peticion; el timeout se sobrescribe por peticion (el cliente compartido esta
        // calibrado para chat). Cualquier fallo de transporte o timeout degrada a `null`.
        val response = try {
            client.post(DishImagePolicy.endpointUrl()) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                timeout {
                    connectTimeoutMillis = timeouts.connectMillis
                    socketTimeoutMillis = timeouts.socketMillis
                    requestTimeoutMillis = timeouts.requestMillis
                }
                setBody(ImageGenerationRequest(model, request.prompt, DishImagePolicy.IMAGE_SIZE))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return null
        }

        // 3. Fuera de 2xx no se lee el cuerpo y no hay reintento.
        if (response.status.value !in 200..299) return null

        val rawBody = try {
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return null
        }

        // 4. Decodificacion tolerante de `data[0].b64_json` y reduccion local a WebP.
        val rawBytes = DishImagePayload.decode(rawBody) ?: return null
        return reducer.reduceToWebp(rawBytes)
    }
}
