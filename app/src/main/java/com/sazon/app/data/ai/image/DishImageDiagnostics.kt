package com.sazon.app.data.ai.image

/**
 * Vocabulario **local** de diagnostico de una llamada HTTP de imagen de plato (correctiva
 * `dish-image-call-diagnostics`).
 *
 * Es propio de la imagen y **no** reutiliza el `AiCallEvent` del chat (`com.sazon.app.data.ai`): la
 * imagen vive en DevExpert con su propia base y su propio ciclo, y acoplar los dos diagnosticos
 * obligaria a compartir tipos sin necesidad. El evento lo emite la costura aditiva `onEvent` de
 * [DevExpertDishImageGenerator] (por defecto no-op) y `MainActivity` lo cablea a `android.util.Log`
 * con la etiqueta `SazonDishImage`. **Nunca** contiene la clave, la cabecera `Authorization`, el
 * `prompt` ni el cuerpo de la respuesta; no se persiste ni se envia a telemetria.
 */

/** Camino HTTP observado: la peticion de texto (`/images/generations`) o la de ajuste (`/images/edits`). */
enum class DishImageCall { GENERATE, EDIT }

/**
 * Motivo clasificado de una llamada de imagen ([DishImagePolicy.reasonFor], un unico sitio de la
 * politica). Es un vocabulario **total**: ningun estado queda sin motivo (el residual es [HTTP_ERROR]).
 */
enum class DishImageCallReason {
    /** `2xx` con bytes entregados (imagen reducida lista). */
    OK,

    /** Cuota agotada o limite de peticiones: `402`/`429`. */
    CUOTA,

    /** Problema de credenciales: `401`/`403`. */
    AUTH,

    /** Fallo del servidor: `5xx`. */
    SERVER,

    /** Fallo de red/timeout antes de tener respuesta, o al leer el cuerpo de un `2xx`. */
    TRANSPORT,

    /** `2xx` sin bytes entregables (cuerpo ilegible, base64 invalido o bytes que el reductor no convierte). */
    INVALID,

    /** Residual: cualquier otro estado no clasificado (`400`/`404`/`408`/`3xx`, ...). */
    HTTP_ERROR,
}

/**
 * Evento de diagnostico **local** de una llamada HTTP de imagen (sin telemetria ni persistencia).
 *
 * Solo lleva categoria ([call], [reason]), estado HTTP, duracion, [detail] y **longitud** de los bytes
 * entregados: nunca la clave, la cabecera `Authorization`, el `prompt` ni el cuerpo. Un evento por
 * llamada HTTP real: un acierto de cache o de semilla, o un gate cerrado, **no** emiten evento (no hay
 * llamada); un rechazo del ajuste con respaldo por texto emite `EDIT` y despues `GENERATE`.
 */
data class DishImageCallEvent(
    /** Camino observado. */
    val call: DishImageCall,
    /** Motivo clasificado ([DishImagePolicy.reasonFor]). */
    val reason: DishImageCallReason,
    /** Estado HTTP devuelto; `null` si no hubo respuesta (fallo de transporte). */
    val httpStatus: Int?,
    /** Duracion de la llamada en milisegundos (reloj de pared; siempre `>= 0`). */
    val durationMillis: Long,
    /** Detalle corto y determinista: `"HTTP <status>"` o `"transport"`; sin prompt ni cuerpo. */
    val detail: String,
    /** Bytes entregados; solo no nulo en [DishImageCallReason.OK]. */
    val bytes: Int? = null,
)
