package com.sazon.app.data.ai

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.prompt.BuiltPrompt
import com.sazon.app.domain.prompt.PromptBuilder
import com.sazon.app.domain.prompt.PromptIngredient
import com.sazon.app.domain.prompt.PromptRequest
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.taxonomy.Taxonomy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Implementacion real del puerto [RecipeGenerator] sobre **DevExpert Inference** (API compatible con
 * OpenAI). Vive en `data.ai`: es el unico sitio (junto al cableado de `MainActivity`) que conoce Ktor.
 *
 * Las tres operaciones componen su prompt con [PromptBuilder] y llaman al mismo [execute], que aplica
 * la politica de [AiProviderPolicy] (deadline total, timeout por intento recortado, un reintento con
 * backoff/`Retry-After`, nunca 4xx no reintentables) y traduce los fallos a [AiUnavailableException],
 * que los llamadores aceptados ya entienden como **modo semilla**.
 *
 * Con el interruptor [responseFormatEnabled] activo (por defecto) cada peticion pide
 * `response_format: {"type":"json_object"}`. Si el proveedor responde **400/422** con presupuesto de
 * intentos (es decir, en el primer intento), se hace **una** peticion de fallback **sin** la clave
 * (consume el segundo intento; nunca mas de una por operacion, sin backoff). Un 400/422 sin
 * presupuesto (el segundo intento) degrada como el resto; el resto de estados no cambian la politica
 * de reintentos de transporte.
 *
 * Un 200 ilegible o vacio **no** lanza: devuelve las recetas decodificables (posiblemente vacias).
 * La [CancellationException] se **relanza** siempre. El diagnostico local se emite por [onEvent]
 * (default no-op) y nunca incluye la clave ni las cabeceras.
 */
class DevExpertRecipeGenerator(
    private val apiKey: String,
    private val taxonomy: Taxonomy,
    private val client: HttpClient,
    private val model: String = AiProviderPolicy.MODEL_ALIAS,
    private val responseFormatEnabled: Boolean = AiProviderPolicy.RESPONSE_FORMAT_ENABLED,
    private val onEvent: (AiCallEvent) -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) : RecipeGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> = execute(
        operation = AiOperation.SUGGEST_BY_PREFERENCES,
        prompt = PromptBuilder.build(
            taxonomy,
            PromptRequest(preferences = profile, servings = profile.defaultServings),
        ),
    )

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> = execute(
        operation = AiOperation.SEARCH_BY_TEXT,
        prompt = PromptBuilder.build(
            taxonomy,
            PromptRequest(
                preferences = profile,
                servings = profile.defaultServings,
                query = query,
            ),
        ),
    )

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = execute(
        operation = AiOperation.GENERATE_FROM_INGREDIENTS,
        prompt = PromptBuilder.build(
            taxonomy,
            PromptRequest(
                preferences = constraints.profile,
                servings = constraints.servings,
                ingredients = ingredients.map { PromptIngredient(it.name, it.ingredientId) },
                mealTypeId = constraints.mealTypeId,
                timeBucketId = constraints.timeBucketId,
                difficultyId = constraints.difficultyId,
                notes = constraints.notes,
            ),
        ),
    )

    private suspend fun execute(operation: AiOperation, prompt: BuiltPrompt): List<Recipe> {
        val startedAt = now()

        // 1. Clave ausente: se degrada a modo semilla SIN ninguna peticion HTTP.
        if (apiKey.isBlank()) {
            report(
                operation = operation,
                outcome = AiCallOutcome.UNAVAILABLE,
                attempt = 0,
                httpStatus = null,
                startedAt = startedAt,
                detail = AiUnavailableReason.MISSING_KEY.name,
            )
            throw AiUnavailableException(
                AiUnavailableReason.MISSING_KEY,
                "Falta la clave del proveedor de IA (devexpert.apiKey); se degrada a modo semilla.",
            )
        }

        // 2. Deadline total, fijado UNA vez antes del primer intento.
        val deadline = now() + AiProviderPolicy.TOTAL_DEADLINE_MILLIS
        var attempt = 1
        // El fallback de formato se hace como maximo UNA vez por operacion (consume el 2. intento).
        var usedFallback = false

        while (true) {
            val includeFormat = responseFormatEnabled && !usedFallback
            val remaining = deadline - now()
            if (remaining <= 0) {
                throwDeadlineExhausted(operation, attempt, startedAt, formatOf(includeFormat))
            }

            when (val outcome = sendAttempt(prompt, remaining, includeFormat)) {
                is AttemptOutcome.Success -> {
                    val result = AiRecipePayload.decode(outcome.content, taxonomy)
                    val callOutcome = if (result.accepted.isEmpty()) AiCallOutcome.EMPTY else AiCallOutcome.SUCCESS
                    report(
                        operation, callOutcome, attempt, outcome.status, startedAt, "HTTP ${outcome.status}", result,
                        responseFormat = formatOf(includeFormat),
                    )
                    return result.accepted
                }

                is AttemptOutcome.HttpFailure -> {
                    val retryDelay = AiProviderPolicy.retryDelayMillis(outcome.retryAfter)
                    val retryable = AiProviderPolicy.isRetryableStatus(outcome.status)
                    // 400/422 con el formato activo: fallback one-shot antes de la logica de transporte.
                    // La guarda de presupuesto (`attempt < MAX_ATTEMPTS`) impide una tercera peticion
                    // cuando el reintento de transporte ya consumio el segundo intento: en ese caso el
                    // 400/422 cae en la rama actual (UNAVAILABLE/HTTP_ERROR). `includeFormat` ya implica
                    // `!usedFallback`, asi que no se repite esa condicion.
                    val fallback = includeFormat &&
                        AiProviderPolicy.requiresResponseFormatFallback(outcome.status) &&
                        attempt < AiProviderPolicy.MAX_ATTEMPTS
                    if (fallback) {
                        usedFallback = true
                        report(
                            operation, AiCallOutcome.FALLBACK, attempt, outcome.status, startedAt,
                            "HTTP ${outcome.status}", responseFormat = AiResponseFormat.JSON_OBJECT,
                        )
                        // Sin backoff: el fallback consume el segundo intento (MAX_ATTEMPTS = 2).
                        attempt += 1
                    } else if (retryable && hasRetryMargin(deadline, attempt, retryDelay)) {
                        report(
                            operation, AiCallOutcome.RETRY, attempt, outcome.status, startedAt,
                            "HTTP ${outcome.status}", responseFormat = formatOf(includeFormat),
                        )
                        delay(retryDelay)
                        attempt += 1
                    } else if (retryable && attempt < AiProviderPolicy.MAX_ATTEMPTS) {
                        // Fallo reintentable, pero el plazo total impide otro intento: el motivo es el
                        // deadline; un estado reintentable agotado por numero de intentos conserva su motivo.
                        throwDeadlineExhausted(operation, attempt, startedAt, formatOf(includeFormat))
                    } else {
                        report(
                            operation, AiCallOutcome.UNAVAILABLE, attempt, outcome.status, startedAt,
                            "HTTP ${outcome.status}", responseFormat = formatOf(includeFormat),
                        )
                        throw AiUnavailableException(
                            AiProviderPolicy.unavailableReason(outcome.status),
                            "El proveedor de IA respondio ${outcome.status}.",
                        )
                    }
                }

                is AttemptOutcome.TransportFailure -> {
                    if (hasRetryMargin(deadline, attempt, AiProviderPolicy.RETRY_BACKOFF_MILLIS)) {
                        report(
                            operation, AiCallOutcome.RETRY, attempt, null, startedAt, outcome.reason.name,
                            responseFormat = formatOf(includeFormat),
                        )
                        delay(AiProviderPolicy.RETRY_BACKOFF_MILLIS)
                        attempt += 1
                    } else if (attempt < AiProviderPolicy.MAX_ATTEMPTS) {
                        throwDeadlineExhausted(operation, attempt, startedAt, formatOf(includeFormat))
                    } else {
                        report(
                            operation, AiCallOutcome.UNAVAILABLE, attempt, null, startedAt, outcome.reason.name,
                            responseFormat = formatOf(includeFormat),
                        )
                        throw AiUnavailableException(
                            outcome.reason,
                            "Fallo de transporte al llamar a la IA.",
                            outcome.cause,
                        )
                    }
                }
            }
        }
    }

    /** Un intento HTTP. Los fallos se devuelven como datos; la cancelacion se relanza siempre. */
    private suspend fun sendAttempt(prompt: BuiltPrompt, remainingMillis: Long, includeFormat: Boolean): AttemptOutcome {
        val response = try {
            client.post(AiProviderPolicy.endpointUrl()) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                timeout { requestTimeoutMillis = AiProviderPolicy.attemptRequestTimeoutMillis(remainingMillis) }
                setBody(
                    ChatCompletionRequest(
                        model = model,
                        messages = listOf(
                            ChatMessage(role = SYSTEM_ROLE, content = prompt.system),
                            ChatMessage(role = USER_ROLE, content = prompt.user),
                        ),
                        responseFormat = if (includeFormat) ResponseFormat.JSON_OBJECT else null,
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: HttpRequestTimeoutException) {
            return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
        } catch (failure: SocketTimeoutException) {
            // java.net, ANTES del IOException generico: es un timeout del modelo, no un fallo de red.
            return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
        } catch (failure: IOException) {
            return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
        } catch (unexpected: Exception) {
            throw AiUnavailableException(
                AiUnavailableReason.UNEXPECTED,
                "Fallo inesperado al llamar a la IA.",
                unexpected,
            )
        }

        val status = response.status.value
        if (status !in 200..299) {
            return AttemptOutcome.HttpFailure(status, response.headers[HttpHeaders.RetryAfter])
        }

        val rawBody = try {
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: HttpRequestTimeoutException) {
            return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
        } catch (failure: SocketTimeoutException) {
            // java.net, ANTES del IOException generico: es un timeout del modelo, no un fallo de red.
            return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
        } catch (failure: IOException) {
            return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
        } catch (_: Exception) {
            // 200 con cuerpo ilegible: no es un error, simplemente no hay recetas.
            return AttemptOutcome.Success(status, content = null)
        }

        return AttemptOutcome.Success(status, parseContent(rawBody))
    }

    /** Decode tolerante del envelope: `choices[0].message.content`, o `null` si no encaja. */
    private fun parseContent(rawBody: String): String? = runCatching {
        json.decodeFromString(ChatCompletionResponse.serializer(), rawBody)
    }.getOrNull()
        ?.choices
        ?.firstOrNull()
        ?.message
        ?.content

    /** Hay margen para otro intento si no se agoto el deadline ni el backoff lo agotaria. */
    private fun hasRetryMargin(deadline: Long, attempt: Int, delayMillis: Long): Boolean {
        val remaining = deadline - now()
        return AiProviderPolicy.canRetry(attempt, remaining) && remaining - delayMillis > 0
    }

    /**
     * El plazo total ([AiProviderPolicy.TOTAL_DEADLINE_MILLIS]) es lo que impide otro intento: el motivo
     * de degradacion es `TIMEOUT`.
     */
    private fun throwDeadlineExhausted(
        operation: AiOperation,
        attempt: Int,
        startedAt: Long,
        responseFormat: AiResponseFormat,
    ): Nothing {
        report(
            operation, AiCallOutcome.UNAVAILABLE, attempt, null, startedAt, DEADLINE_DETAIL,
            responseFormat = responseFormat,
        )
        throw AiUnavailableException(
            AiUnavailableReason.TIMEOUT,
            "Se agoto el plazo total de la peticion a la IA.",
        )
    }

    private fun report(
        operation: AiOperation,
        outcome: AiCallOutcome,
        attempt: Int,
        httpStatus: Int?,
        startedAt: Long,
        detail: String,
        decodeResult: AiDecodeResult? = null,
        responseFormat: AiResponseFormat = AiResponseFormat.NONE,
    ) {
        onEvent(
            AiCallEvent(
                operation = operation,
                outcome = outcome,
                attempt = attempt,
                httpStatus = httpStatus,
                durationMillis = (now() - startedAt).coerceAtLeast(0L),
                detail = detail,
                // Solo longitudes y motivos del decode; null/vacio cuando no hubo decode.
                contentBytes = decodeResult?.contentBytes,
                contentChars = decodeResult?.contentChars,
                received = decodeResult?.received,
                accepted = decodeResult?.accepted?.size,
                discardedByReason = decodeResult?.discardedByReason ?: emptyMap(),
                responseFormat = responseFormat,
            ),
        )
    }

    /** Modo del evento segun si el intento llevo `response_format`. */
    private fun formatOf(includeFormat: Boolean): AiResponseFormat =
        if (includeFormat) AiResponseFormat.JSON_OBJECT else AiResponseFormat.NONE
}

private const val SYSTEM_ROLE = "system"
private const val USER_ROLE = "user"
private const val DEADLINE_DETAIL = "deadline"

/** Resultado interno de un intento HTTP. */
private sealed interface AttemptOutcome {
    data class Success(val status: Int, val content: String?) : AttemptOutcome
    data class HttpFailure(val status: Int, val retryAfter: String?) : AttemptOutcome
    data class TransportFailure(val reason: AiUnavailableReason, val cause: Throwable?) : AttemptOutcome
}
