package com.sazon.app.data.ai

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

    /** Fallo final: se lanza [AiUnavailableException]. */
    UNAVAILABLE,
}

/** Operacion del puerto que origino la llamada (para el diagnostico). */
enum class AiOperation {
    SUGGEST_BY_PREFERENCES,
    SEARCH_BY_TEXT,
    GENERATE_FROM_INGREDIENTS,
}

/**
 * Evento de diagnostico **local** de una llamada a la IA (sin telemetria).
 *
 * Se emite por la costura aditiva `onEvent` del proveedor (por defecto no-op) y `MainActivity` lo
 * cablea a `android.util.Log`. **Nunca** contiene la clave, la cabecera `Authorization` ni los
 * cuerpos: [detail] es la categoria/motivo (p. ej. `MISSING_KEY`, `HTTP 429`, `deadline`).
 */
data class AiCallEvent(
    val operation: AiOperation,
    val outcome: AiCallOutcome,
    val attempt: Int,
    val httpStatus: Int?,
    val durationMillis: Long,
    val detail: String,
)
