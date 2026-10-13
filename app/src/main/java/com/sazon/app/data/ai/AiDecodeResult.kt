package com.sazon.app.data.ai

import com.sazon.app.domain.image.DishVisualDescriptor
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.validation.DiscardReason

/**
 * Que se encontro en el `content` que devolvio el modelo. Distingue el vacio del ilegible, que hoy
 * se confunden en un `EMPTY` sin explicacion.
 */
internal enum class AiContentState {
    /** `content` nulo o en blanco ([contentChars]/[AiDecodeResult.contentBytes] son 0). */
    BLANK,

    /** Hay texto pero el root o el array `recipes` no son legibles (no hay recetas que contar). */
    ILLEGIBLE,

    /** Se leyo el array `recipes` (puede venir con 0 aceptadas). */
    RECIPES,
}

/**
 * Resultado tipado del decode del `content` del modelo: no pierde el detalle que el `List<Recipe>`
 * de antes tiraba.
 *
 * El proveedor solo usa [accepted] para decidir `SUCCESS`/`EMPTY`; lo demas alimenta el diagnostico
 * local ([AiCallEvent]) sin exponer nunca el cuerpo completo:
 *
 * - [received] es el numero de elementos del array `recipes` (incluidos los que no encajan con el
 *   wire del contrato).
 * - [discardedByReason] cuenta los descartes **por motivo** ([DiscardReason]), en el orden del enum
 *   y solo con los motivos presentes: es el "resumen" que explica un `EMPTY` o un lote parcial. El
 *   `detail` del descarte **no** se propaga (ni titulos ni texto libre).
 * - [incoherentByCause] desglosa los descartes [DiscardReason.INCOHERENT] por causa del Validador
 *   ([AiIncoherentCause.TIME_DRIFT] = regla 4a, [AiIncoherentCause.ALLERGEN_COVERAGE] = regla 4b), en
 *   el orden de [AiIncoherentCause] y solo con las causas presentes: es un **diagnostico**, la
 *   decision de aceptar/descartar sigue siendo del Validador.
 * - [discards] es el detalle por descarte (motivo, causa y titulo) y **solo** se rellena en modo
 *   depuracion (`verbose = true`); por defecto es `emptyList()` y **no se retiene ningun titulo**.
 * - [contentChars] (unidades UTF-16 de `content`) y [contentBytes] (UTF-8) son **longitudes**, nunca
 *   el contenido.
 * - [descriptors] es el **transportador** del descriptor visual opcional del modelo (fase 5) por
 *   `id` **generado** de cada receta **aceptada**: solo aparece con descriptor utilizable y hoy
 *   **nadie** lo consume en runtime (el punto de composicion de `DishImageRequest` sigue pasando
 *   `descriptor = null`).
 *
 * Es `internal` como el resto del decode ([AiRecipePayload]): no forma parte de la API publica del
 * modulo. Los tests JVM del propio modulo lo usan por los *friend paths*.
 */
internal data class AiDecodeResult(
    val accepted: List<Recipe>,
    val received: Int,
    val discardedByReason: Map<DiscardReason, Int>,
    val incoherentByCause: Map<AiIncoherentCause, Int>,
    val discards: List<AiDiscardDetail>,
    val contentChars: Int,
    val contentBytes: Int,
    val contentState: AiContentState,
    val descriptors: Map<String, DishVisualDescriptor> = emptyMap(),
)
