package com.sazon.app.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Fabrica del cliente HTTP del proveedor de IA (Ktor + engine OkHttp).
 *
 * - `null` -> engine **OkHttp** (produccion); en tests se inyecta un `MockEngine`.
 * - `ContentNegotiation` con `json` (usado para **serializar la peticion**) e `HttpTimeout` con la
 *   politica de [AiProviderPolicy].
 * - `expectSuccess = false`: el estado HTTP lo inspecciona el proveedor (no queremos que Ktor lance
 *   por un 4xx/5xx y se pierda el codigo).
 * - Los **reintentos son a mano** en `DevExpertRecipeGenerator` (no `HttpRequestRetry`): asi la
 *   politica es explicita, testeable en JVM y respeta el deadline total.
 */
object AiHttpClientFactory {

    fun create(engine: HttpClientEngine? = null): HttpClient =
        if (engine == null) {
            HttpClient(OkHttp) { configureAiClient() }
        } else {
            HttpClient(engine) { configureAiClient() }
        }

    private fun HttpClientConfig<*>.configureAiClient() {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = AiProviderPolicy.CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = AiProviderPolicy.SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = AiProviderPolicy.REQUEST_TIMEOUT_MILLIS
        }
    }
}
