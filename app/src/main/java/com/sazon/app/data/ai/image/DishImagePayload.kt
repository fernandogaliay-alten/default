package com.sazon.app.data.ai.image

import java.util.Base64
import kotlinx.serialization.json.Json

/**
 * Decodificacion **tolerante** de `data[0].b64_json` a bytes crudos. Puro Kotlin (`java.util.Base64`
 * esta disponible con `minSdk 33`), sin Android y sin HTTP.
 *
 * Es el equivalente del decodificador del chat para la imagen: cualquier cosa que no encaje devuelve
 * `null` y **nunca lanza** (cuerpo nulo/blank, JSON invalido, `data` ausente/vacia/no-lista, `b64_json`
 * nulo o en blanco y base64 invalida).
 */
object DishImagePayload {

    private val json = Json { ignoreUnknownKeys = true }

    /** Primer `data[i].b64_json` no vacio decodificado; `null` si no hay nada utilizable. Nunca lanza. */
    fun decode(rawBody: String?): ByteArray? {
        val body = rawBody?.takeIf { it.isNotBlank() } ?: return null

        val response = runCatching {
            json.decodeFromString(ImageGenerationResponse.serializer(), body)
        }.getOrNull() ?: return null

        val encoded = response.data
            .firstNotNullOfOrNull { it.b64Json?.takeIf { value -> value.isNotBlank() } }
            ?: return null

        val bytes = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() ?: return null
        return bytes.takeIf { it.isNotEmpty() }
    }
}
