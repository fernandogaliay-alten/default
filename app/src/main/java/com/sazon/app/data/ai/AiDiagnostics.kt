package com.sazon.app.data.ai

import com.sazon.app.domain.validation.DiscardReason

/**
 * Motivo por el que la IA quedo no disponible para una operacion.
 *
 * Es la senal que el proveedor arroja al llamador (envolviendola en [AiUnavailableException]) y que
 * los servicios de dominio aceptados ya interpretan como **modo semilla** (`aiUnavailable` / lote
 * solo semilla). No es un tipo del puerto `RecipeGenerator`: el puerto no tiene tipo de resultado y
 * no se cambia.
 */
enum class AiUnavailableReason {
    /** Falta la clave del proveedor (blank); no se hace ninguna peticion. */
    MISSING_KEY,

    /** Se agoto el plazo total de la operacion, el timeout por intento **o** el timeout de socket. */
    TIMEOUT,

    /** Fallo de transporte que **no** es un timeout: DNS, conexion rechazada/reset, etc. */
    NETWORK,

    /** HTTP 429: cuota agotada o limite de peticiones. */
    RATE_LIMITED,

    /** HTTP 4xx/5xx no recuperable (400/401/403/404/422, ...). */
    HTTP_ERROR,

    /** Cualquier otro fallo inesperado (incluida una serializacion rota). */
    UNEXPECTED,
}

/**
 * Excepcion tipada y controlada que materializa el **modo semilla**.
 *
 * Nunca es un `Error` ni una excepcion sin capturar: los tres llamadores aceptados
 * (`SuggestionBatchService`, `GenerationService`, `AiSearchService`) la capturan como `Exception` y
 * degradan a modo semilla. **No** se lanza en un 200 ilegible o vacio (eso devuelve lista vacia).
 */
class AiUnavailableException(
    val reason: AiUnavailableReason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Desenlace de un intento de llamada a la IA, para el diagnostico local. */
enum class AiCallOutcome {
    /** 2xx con recetas decodificables. */
    SUCCESS,

    /** 2xx sin recetas decodificables (vacio o ilegible): no es un error pero no hay nada. */
    EMPTY,

    /** Fallo reintentable: se hara un segundo intento. */
    RETRY,

    /**
     * `400`/`422` con el `response_format` activo: se hara **una** peticion de fallback sin la clave
     * (`AiProviderPolicy.requiresResponseFormatFallback`). Camino aparte del reintento de transporte.
     */
    FALLBACK,

    /** Fallo final: se lanza [AiUnavailableException]. */
    UNAVAILABLE,
}

/** Modo de `response_format` usado en un intento, para el diagnostico local. */
enum class AiResponseFormat {
    /** El intento llevo `"response_format":{"type":"json_object"}`. */
    JSON_OBJECT,

    /** El intento no llevo `response_format` (interruptor apagado, fallback o sin activar). */
    NONE,
}

/** Operacion del puerto que origino la llamada (para el diagnostico). */
enum class AiOperation {
    SUGGEST_BY_PREFERENCES,
    SEARCH_BY_TEXT,
    GENERATE_FROM_INGREDIENTS,
}

/**
 * Causa de un descarte [DiscardReason.INCOHERENT]: distingue las **dos** partes de la regla 4 del
 * Validador (`RecipeValidator`), que hoy colapsa en un unico motivo.
 *
 * Es un vocabulario **de diagnostico** en `data.ai`, no un valor nuevo de [DiscardReason]: la decision
 * de aceptar/descartar sigue siendo del Validador (puerta dura).
 */
enum class AiIncoherentCause {
    /** Regla 4a: `abs(steps.sumOf { it.minutes } - totalMinutes) > MAX_STEP_TOTAL_DRIFT_MINUTES`. */
    TIME_DRIFT,

    /** Regla 4b: `declaredAllergens` no cubre los alergenos curados de los ingredientes resueltos. */
    ALLERGEN_COVERAGE,
}

/**
 * Detalle de un descarte, **solo** en modo depuracion (ver `DevExpertRecipeGenerator`).
 *
 * En el camino por defecto no se retiene: [title] es texto del modelo y el evento nunca lo expone.
 * La clave y la cabecera `Authorization` no forman parte de este detalle.
 */
data class AiDiscardDetail(
    val reason: DiscardReason,
    /** Causa del descarte; solo no nula cuando [reason] es [DiscardReason.INCOHERENT]. */
    val cause: AiIncoherentCause? = null,
    /** Titulo de la receta, o `null` si el elemento no llego a decodificarse. */
    val title: String? = null,
)

/**
 * Evento de diagnostico **local** de una llamada a la IA (sin telemetria).
 *
 * Se emite por la costura aditiva `onEvent` del proveedor (por defecto no-op) y `MainActivity` lo
 * cablea a `android.util.Log`. **Nunca** contiene la clave ni la cabecera `Authorization`. En el
 * camino por defecto ([discards] vacio y [rawContent] `null`) tampoco contiene el texto del modelo ni
 * titulos de recetas: [detail] es la categoria/motivo (p. ej. `MISSING_KEY`, `HTTP 429`, `deadline`),
 * los campos del decode son **longitudes** ([contentChars]/[contentBytes]), recuentos
 * ([received]/[accepted]) y **resumenes** ([discardedByReason]/[incoherentByCause]).
 *
 * **En modo depuracion** (`verboseDiagnostics`, ver `DevExpertRecipeGenerator`; activo solo en debug)
 * el evento lleva ademas el detalle de cada descarte ([discards], con titulo) y el `content` crudo del
 * modelo ([rawContent]); aun asi **nunca** la clave ni la cabecera `Authorization`.
 */
data class AiCallEvent(
    val operation: AiOperation,
    val outcome: AiCallOutcome,
    val attempt: Int,
    val httpStatus: Int?,
    val durationMillis: Long,
    val detail: String,
    /** Bytes UTF-8 del `content` del modelo; `null` si no hubo decode (clave ausente, retry, deadline). */
    val contentBytes: Int? = null,
    /** Caracteres (unidades UTF-16) del `content` del modelo; `null` si no hubo decode. */
    val contentChars: Int? = null,
    /** Recetas recibidas en el array `recipes`; `null` si no hubo decode. */
    val received: Int? = null,
    /** Recetas que el decode acepto; `null` si no hubo decode. */
    val accepted: Int? = null,
    /** Resumen de descartes por motivo, en orden de [DiscardReason]; vacio si no hubo descartes. */
    val discardedByReason: Map<DiscardReason, Int> = emptyMap(),
    /** Modo de `response_format` usado en el intento (nunca el texto del modelo). */
    val responseFormat: AiResponseFormat = AiResponseFormat.NONE,
    /** Desglose de los descartes [DiscardReason.INCOHERENT] por causa; vacio si no hubo. */
    val incoherentByCause: Map<AiIncoherentCause, Int> = emptyMap(),
    /** Detalle de cada descarte; **solo** en modo depuracion (vacio por defecto). */
    val discards: List<AiDiscardDetail> = emptyList(),
    /** `content` crudo del modelo; **solo** en modo depuracion (`null` por defecto). */
    val rawContent: String? = null,
)
