package com.sazon.app.data.ai.image

import com.sazon.app.data.ai.AiProviderPolicy
import kotlin.math.roundToInt

/**
 * Politica de la **imagen de plato** en un unico sitio: endpoint, alias de modelo, tamano generado,
 * geometria de la reduccion local y timeouts propios. Son constantes y funciones **puras** (sin HTTP y
 * sin Android), de modo que se testean en JVM, igual que [AiProviderPolicy].
 *
 * Se separa de [AiProviderPolicy] a proposito (fase 2 de `docs/ai-dish-images-proposal.md` y
 * `docs/adr/0005-ai-dish-images.md`): la imagen **no** reintenta ni comparte la cola de razonamiento
 * del chat, asi que ni sus timeouts ni su politica de intentos son los de chat.
 */
object DishImagePolicy {

    /** Ruta del endpoint de generacion de imagenes (misma base que el chat, ver [AiProviderPolicy]). */
    const val IMAGES_GENERATIONS_PATH = "/images/generations"

    /** Ruta del endpoint de ajuste con referencia de estilo (fase 6, variante C de la propuesta §4.2). */
    const val IMAGES_EDITS_PATH = "/images/edits"

    /**
     * Alias del modelo de imagen. El alias aislado, no el nombre real, puede cambiar sin romper nada.
     * El proveedor lo expone como `image` (la propuesta §3 lo probo contra `/images/generations`).
     */
    const val MODEL_ALIAS = "image"

    /**
     * Alias del modelo de **ajuste** con referencia. El alias aislado, no el nombre real: la propuesta
     * §4.2 lo nombra como `image-edit` en la variante C (nunca el nombre real del modelo).
     */
    const val EDIT_MODEL_ALIAS = "image-edit"

    /**
     * Interruptor del anclaje de estilo (fase 6). `true` (por defecto) pide el ajuste con la referencia;
     * `false` va directo a texto->imagen con **cero** peticiones al ajuste: es la palanca de rollback si
     * el proveedor no soporta `/images/edits`. Un unico sitio, patron
     * [AiProviderPolicy.RESPONSE_FORMAT_ENABLED]; la produccion lo lee y el adaptador lo recibe por
     * constructor.
     */
    const val STYLE_REFERENCE_ENABLED = true

    /**
     * Estados de **rechazo definitivo** del ajuste que disparan el respaldo por texto
     * (`400/404/405/413/415/422`): el ajuste no se acepta (peticion mal formada, endpoint o modelo
     * desconocido, payload/medio no soportado), asi que el camino de texto si puede funcionar. Un
     * `401/403/429/5xx` **no** entra aqui: la clave, la cuota o el servidor fallarian igual y el
     * respaldo solo duplicaria el gasto.
     */
    val EDIT_FALLBACK_STATUSES = setOf(400, 404, 405, 413, 415, 422)

    /**
     * Tamano pedido al proveedor: **el unico soportado hoy**. `512x512` responde `400` en la prueba
     * real de la propuesta §3; la reduccion a [TARGET_WIDTH_PX] se hace **despues**, en local.
     */
    const val IMAGE_SIZE = "1024x1024"

    /** Ancho objetivo del WebP guardado (reduccion local, nunca se agranda). */
    const val TARGET_WIDTH_PX = 960

    /** Calidad de `Bitmap.CompressFormat.WEBP_LOSSY` al re-codificar (0-100). */
    const val WEBP_QUALITY = 80

    /**
     * Timeouts propios de la imagen, **mas cortos** que los de chat (connect 10 s / socket 30 s /
     * request 40 s frente a 10 / 120 / 150 s): la imagen mide ~4,2 s y no comparte la cola de
     * razonamiento del chat. Se aplican **por peticion** sobre el `HttpClient` compartido.
     */
    val TIMEOUTS = DishImageTimeouts(
        connectMillis = 10_000L,
        socketMillis = 30_000L,
        requestMillis = 40_000L,
    )

    /** Un solo intento: un reintento duplicaria la cuota (~2 % por imagen) sin cambiar la experiencia. */
    const val MAX_ATTEMPTS = 1

    /** URL completa del endpoint (`BASE_URL` + [IMAGES_GENERATIONS_PATH]). */
    fun endpointUrl(): String = AiProviderPolicy.BASE_URL + IMAGES_GENERATIONS_PATH

    /** URL completa del endpoint de ajuste (`BASE_URL` + [IMAGES_EDITS_PATH]). */
    fun editsUrl(): String = AiProviderPolicy.BASE_URL + IMAGES_EDITS_PATH

    /**
     * Un rechazo definitivo del ajuste ([EDIT_FALLBACK_STATUSES]) dispara el respaldo por texto; ningun
     * otro estado (timeout de transporte, `401/403/429`, `5xx` o un `2xx`) lo hace. Pura y testeable en
     * JVM: decide en un unico sitio "esto no es un problema de red, es que el ajuste no se acepta".
     */
    fun editFailureRequiresTextFallback(statusCode: Int): Boolean =
        statusCode in EDIT_FALLBACK_STATUSES

    /**
     * Geometria de la reduccion local: si [width] supera [TARGET_WIDTH_PX] devuelve
     * `[TARGET_WIDTH_PX, alto proporcional]` (minimo 1 px); si no, devuelve el tamano original sin
     * agrandar. Pura (sin Android) y testeable en JVM.
     */
    fun scaledSize(width: Int, height: Int): IntArray {
        if (width <= TARGET_WIDTH_PX) return intArrayOf(width, height)
        val scaledHeight = (height.toDouble() * TARGET_WIDTH_PX / width).roundToInt().coerceAtLeast(1)
        return intArrayOf(TARGET_WIDTH_PX, scaledHeight)
    }
}

/**
 * Timeouts de la peticion de imagen. Es un `data class` para poder **inyectar valores diminutos** en
 * el test de timeout; en produccion se usa [DishImagePolicy.TIMEOUTS].
 */
data class DishImageTimeouts(
    val connectMillis: Long,
    val socketMillis: Long,
    val requestMillis: Long,
)
