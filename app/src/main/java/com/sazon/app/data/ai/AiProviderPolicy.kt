package com.sazon.app.data.ai

import com.sazon.app.domain.validation.RecipeConstraints
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

/**
 * Politica del **chat** de la IA en un **unico sitio**: endpoint, modelo, cabeceras del proveedor,
 * timeouts, reintentos y degradacion. Son constantes y decisiones **puras** (sin HTTP), de modo que se
 * testean en JVM.
 *
 * El chat se sirve contra **OpenCode Go** (`docs/adr/0003-ai-provider.md`, correctiva
 * `chat-provider-opencode-go`); la **imagen de plato** tiene su **propia** base y su propia politica en
 * `com.sazon.app.data.ai.image.DishImagePolicy` (DevExpert), de modo que cambiar de proveedor el chat
 * no puede arrastrar las imagenes. Cierra la pregunta abierta 1 de
 * `docs/risks-and-open-questions.md` (ver `docs/specs/real-ai-provider.md`).
 */
object AiProviderPolicy {

    /** Base de la API compatible con OpenAI de **OpenCode Go** (proveedor del **chat**). */
    const val BASE_URL = "https://opencode.ai/zen/go/v1"

    /** Ruta del endpoint de chat completions. */
    const val CHAT_COMPLETIONS_PATH = "/chat/completions"

    /**
     * Id del modelo del chat. A diferencia de DevExpert (que expone **alias**), OpenCode Go **no**
     * expone alias: el id es literal y queda fijado aqui (un unico sitio). Si el proveedor lo
     * renombra, el chat degrada a modo semilla hasta cambiar esta constante.
     */
    const val MODEL_ID = "deepseek-v4.1-flash"

    /**
     * Nombre de la cabecera **obligatoria** de OpenCode Go. Sin ella el proveedor responde
     * `400 MissingSessionID` (verificado a mano el 2026-10-14). Viaja **por peticion** (nunca como
     * default del cliente compartido: filtraria la cabecera a las peticiones de imagen de DevExpert).
     */
    const val SESSION_HEADER = "x-opencode-session"

    /**
     * Valor de [SESSION_HEADER]: una constante **estable y no sensible** que identifica a la app.
     * **Nunca** la clave ni nada derivado de ella.
     */
    const val SESSION_ID = "sazon-android"

    /** `User-Agent` propio recomendado por el proveedor; tambien viaja por peticion. */
    const val USER_AGENT = "Sazon-Android/1.0"

    /**
     * Interruptor del `response_format`. `true` (por defecto) pide `{"type":"json_object"}`;
     * `false` desactiva la feature por completo (comportamiento identico al anterior, sin la clave).
     * Un unico sitio; la produccion lo lee y el generador lo recibe por constructor.
     */
    const val RESPONSE_FORMAT_ENABLED = true

    /** El unico `type` de `response_format` usado hoy (ver `docs/adr/0003-ai-provider.md`). */
    const val RESPONSE_FORMAT_JSON_OBJECT = "json_object"

    /** Timeout de conexion (connect). */
    const val CONNECT_TIMEOUT_MILLIS = 10_000L

    /**
     * Timeout de socket (lectura/escritura). Cubre el **peor caso medido** de la misma clave y el
     * mismo prompt de 6 recetas (issue #72): `mimo-v2.6-pro` **115,8 s**, con ~4,2 s de margen, y
     * sigue siendo `>= 2x` el peor del alias diario `chat` (46,6 s). Con 90 s la lectura cortaba la
     * respuesta antes de que el modelo terminara de razonar.
     *
     * Evidencia multi-modelo (6 recetas, HTTP 200): `chat` 28,0 / 33,1 / 42,1 / 46,6 s,
     * `chat`+`json_object` 85,3 s, `chat-pro`+`json_schema` 97,0 s, `deepseek-v4-flash` 52,6 s,
     * `mimo-v2.6-pro` 115,8 s.
     *
     * **Matiz (correctiva `chat-provider-opencode-go`)**: esa calibracion se midio contra el proveedor
     * **anterior** (DevExpert). Al pasar el chat a OpenCode Go (`deepseek-v4.1-flash`) **no** se
     * recalibra: se conserva como **techo conservador** hasta tener mediciones del proveedor nuevo.
     */
    const val SOCKET_TIMEOUT_MILLIS = 120_000L

    /**
     * Timeout por intento (request). Techo por encima del de socket (el guardia de lectura salta
     * primero) para acotar una respuesta que gotea.
     */
    const val REQUEST_TIMEOUT_MILLIS = 150_000L

    /**
     * Deadline total por operacion, sumando todos los intentos. Cabe el peor intento medido
     * (115,8 s), el backoff y **otro** intento igualmente lento
     * (`240 - 115,8 - 0,5 = 123,7 s >= 115,8 s`), asi que una latencia alta no degrada a modo semilla
     * antes de tiempo (issue #72).
     */
    const val TOTAL_DEADLINE_MILLIS = 240_000L

    /** Maximo de intentos: el primero mas **un** reintento. */
    const val MAX_ATTEMPTS = 2

    /** Backoff fijo entre intentos. */
    const val RETRY_BACKOFF_MILLIS = 500L

    /** `Retry-After` solo se respeta si no supera este umbral; si no, se usa el backoff fijo. */
    const val RETRY_AFTER_MAX_MILLIS = 5_000L

    /**
     * Restricciones "neutras": el proveedor **no** pre-filtra por dieta ni por restricciones duras.
     *
     * `dietPatternId = ""` es a proposito un id que no existe en la Taxonomia (`dietPattern("") == null`),
     * asi que la regla 2 del validador es un no-op, y `hardRestrictionIds` vacio deja la regla 3 sin
     * candidatos. Los tres llamadores ya validan con el Perfil efectivo; si el proveedor pre-filtrara,
     * el estado **Bloqueado** de `ai-search` (decidido por el motivo del descarte) se degradaria a
     * "sin resultados".
     */
    val NEUTRAL_CONSTRAINTS = RecipeConstraints(dietPatternId = "", hardRestrictionIds = emptySet())

    /** URL completa del endpoint (`BASE_URL` + `CHAT_COMPLETIONS_PATH`). */
    fun endpointUrl(): String = BASE_URL + CHAT_COMPLETIONS_PATH

    /**
     * Un estado HTTP es reintentable solo si es 408 (timeout), 429 (cuota) o 5xx. Un 400/401/403/404/422
     * y demas 4xx **nunca** se reintentan.
     */
    fun isRetryableStatus(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 429 || statusCode in 500..599

    /**
     * Un `400`/`422` con el `response_format` activo dispara el **fallback** (reintentar una sola vez
     * sin la clave); ningun otro estado. Es un camino **aparte** del reintento de transporte: estos
     * estados siguen **sin** ser [isRetryableStatus] y no usan backoff ni `Retry-After`.
     */
    fun requiresResponseFormatFallback(statusCode: Int): Boolean =
        statusCode == 400 || statusCode == 422

    /**
     * Backoff a esperar antes del reintento segun la cabecera `Retry-After`.
     *
     * Solo se interpreta como **segundos** y solo se respeta en el rango `0..5` s; cualquier otro
     * valor (30, ausente, no numerico, formato fecha HTTP) cae al backoff fijo
     * [RETRY_BACKOFF_MILLIS].
     */
    fun retryDelayMillis(retryAfterHeaderValue: String?): Long {
        val seconds = retryAfterHeaderValue?.trim()?.toLongOrNull() ?: return RETRY_BACKOFF_MILLIS
        val millis = seconds * 1_000L
        return if (seconds >= 0 && millis <= RETRY_AFTER_MAX_MILLIS) millis else RETRY_BACKOFF_MILLIS
    }

    /**
     * Timeout de un intento: se recorta al tiempo restante del deadline total
     * (`min(REQUEST_TIMEOUT_MILLIS, remainingMillis)`). Un `remaining` no positivo se trata como 0.
     */
    fun attemptRequestTimeoutMillis(remainingMillis: Long): Long =
        minOf(REQUEST_TIMEOUT_MILLIS, remainingMillis.coerceAtLeast(0L))

    /**
     * Hay margen para un nuevo intento: queda menos del maximo y el backoff fijo no agota el deadline
     * restante.
     */
    fun canRetry(attempt: Int, remainingMillis: Long): Boolean =
        attempt < MAX_ATTEMPTS && remainingMillis - RETRY_BACKOFF_MILLIS > 0

    /**
     * Motivo de la degradacion para un estado HTTP de fallo: 429 -> [AiUnavailableReason.RATE_LIMITED];
     * cualquier otro 4xx/5xx -> [AiUnavailableReason.HTTP_ERROR].
     */
    fun unavailableReason(statusCode: Int): AiUnavailableReason =
        if (statusCode == 429) AiUnavailableReason.RATE_LIMITED else AiUnavailableReason.HTTP_ERROR

    /**
     * Motivo de degradacion de un fallo de **transporte** (sin estado HTTP). Es pura y decide el
     * diagnostico local en un unico sitio.
     *
     * - `HttpRequestTimeoutException` (timeout por intento de Ktor) y `SocketTimeoutException`
     *   (`java.net`, el timeout de socket real que expone el engine OkHttp en JVM: "el modelo tardo en
     *   responder") e `InterruptedIOException` (call timeout de OkHttp) -> [AiUnavailableReason.TIMEOUT].
     * - `IOException` generico (DNS, conexion rechazada, reset...) -> [AiUnavailableReason.NETWORK].
     *   `java.net.ConnectException` (el `ConnectTimeoutException` de Ktor) **no** es un timeout del
     *   modelo: nunca se llego a hablar con el proveedor, asi que es red.
     * - Cualquier otra causa (incluido `null`) -> [AiUnavailableReason.UNEXPECTED].
     */
    fun transportFailureReason(cause: Throwable?): AiUnavailableReason = when {
        cause is HttpRequestTimeoutException -> AiUnavailableReason.TIMEOUT
        cause is SocketTimeoutException -> AiUnavailableReason.TIMEOUT
        cause is InterruptedIOException -> AiUnavailableReason.TIMEOUT
        cause is IOException -> AiUnavailableReason.NETWORK
        else -> AiUnavailableReason.UNEXPECTED
    }
}
