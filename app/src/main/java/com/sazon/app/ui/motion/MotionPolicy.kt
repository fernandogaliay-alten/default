package com.sazon.app.ui.motion

import com.sazon.app.ui.navigation.MainTab
import com.sazon.app.ui.navigation.PushedScreen
import com.sazon.app.ui.theme.SazonMotion

/**
 * Direccion de una transicion de movimiento.
 *
 * `NONE` es **sin desplazamiento** (solo fundido): con el mismo destino, `AnimatedContent` no ejecuta
 * ninguna transicion.
 */
enum class MotionDirection { FORWARD, BACKWARD, NONE }

/**
 * Selector **puro** de la direccion del movimiento (sin Compose ni Android, solo depende de
 * `ui.navigation`, tambien puro): a partir del destino anterior y el siguiente decide `FORWARD`
 * (adelante), `BACKWARD` (atras) o `NONE` (sin cambio), y traduce esa direccion a los desplazamientos
 * de `SazonMotion`. Testeable en JVM (`MotionPolicyTest`).
 *
 * Reglas fijadas (contrato):
 *
 * | Cambio | `FORWARD` | `BACKWARD` | `NONE` |
 * |---|---|---|---|
 * | Pestana ([tabChange]) | ordinal creciente: Inicio -> Favoritos -> Perfil | ordinal decreciente | misma pestana |
 * | Pagina ([pageChange]) | `null -> X` (empujar) y `X -> Y` (cambio de pagina) | `X -> null` (sacar) | mismo destino |
 * | Pasos ([directionOf]) | indice creciente | indice decreciente | mismo indice |
 */
object MotionPolicy {

    /** Direccion de un cambio de indice (pasos del onboarding y del Generador). */
    fun directionOf(fromIndex: Int, toIndex: Int): MotionDirection = when {
        toIndex > fromIndex -> MotionDirection.FORWARD
        toIndex < fromIndex -> MotionDirection.BACKWARD
        else -> MotionDirection.NONE
    }

    /** Direccion de un cambio de pestana por el ordinal de [MainTab]. */
    fun tabChange(from: MainTab, to: MainTab): MotionDirection =
        directionOf(from.ordinal, to.ordinal)

    /** Direccion de un cambio de pagina empujada: empujar/cambiar = adelante, sacar = atras. */
    fun pageChange(from: PushedScreen?, to: PushedScreen?): MotionDirection = when {
        from == to -> MotionDirection.NONE
        from == null -> MotionDirection.FORWARD
        to == null -> MotionDirection.BACKWARD
        else -> MotionDirection.FORWARD
    }

    /** Desplazamiento de entrada en dp: `+24` adelante, `-24` atras, `0` sin movimiento. */
    fun enterOffsetDp(direction: MotionDirection): Int = when (direction) {
        MotionDirection.FORWARD -> SazonMotion.SLIDE_OFFSET_DP
        MotionDirection.BACKWARD -> -SazonMotion.SLIDE_OFFSET_DP
        MotionDirection.NONE -> 0
    }

    /** Desplazamiento de salida en dp: el opuesto al de entrada. */
    fun exitOffsetDp(direction: MotionDirection): Int = -enterOffsetDp(direction)
}
