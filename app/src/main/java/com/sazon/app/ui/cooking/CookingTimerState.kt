package com.sazon.app.ui.cooking

/**
 * Estados del **temporizador por paso** del modo cocina (captura
 * `docs/design/concepts/cooking-mode/modo-cocina/`, matriz de 4 estados).
 */
enum class CookingTimerStatus {
    /** Inactivo: no ha arrancado (o se reinicio); muestra la duracion total. */
    IDLE,

    /** En marcha: cuenta atras activa. */
    RUNNING,

    /** Pausado: el tiempo queda congelado. */
    PAUSED,

    /** Terminado: llego a cero y dejo de contar; aviso en pantalla "¡Listo!". */
    FINISHED,
}

/**
 * Maquina de estados **pura** del temporizador por paso (sin Compose ni Android; testeable en JVM).
 *
 * Es **determinista**: no lee el reloj; el tiempo entra siempre por [tick] con el `delta` explicito
 * que le pasa la capa Compose (`LaunchedEffect` + `delay(1000)`). Las transiciones no validas son
 * **no-op** (devuelven el mismo estado) y ninguna operacion muta el receptor.
 *
 * [hasDuration] distingue un paso con duracion (`minutes > 0`) de uno sin ella: la UI oculta el
 * bloque completo del temporizador cuando es `false` (escenario 6 de la spec).
 */
data class CookingTimerState(
    val status: CookingTimerStatus,
    val totalMillis: Long,
    val remainingMillis: Long,
) {

    /** `true` si el paso tiene una duracion real (`minutes > 0`); si no, la UI oculta el bloque. */
    val hasDuration: Boolean get() = totalMillis > 0

    /** `true` mientras la cuenta atras esta activa (el reloj de la UI solo late en este estado). */
    val isRunning: Boolean get() = status == CookingTimerStatus.RUNNING

    /** `true` cuando la cuenta atras llego a cero y dejo de contar. */
    val isFinished: Boolean get() = status == CookingTimerStatus.FINISHED

    /** `IDLE -> RUNNING`; no-op desde cualquier otro estado. */
    fun start(): CookingTimerState =
        if (status == CookingTimerStatus.IDLE) copy(status = CookingTimerStatus.RUNNING) else this

    /** `RUNNING -> PAUSED` (congela el tiempo); no-op desde cualquier otro estado. */
    fun pause(): CookingTimerState =
        if (status == CookingTimerStatus.RUNNING) copy(status = CookingTimerStatus.PAUSED) else this

    /** `PAUSED -> RUNNING` (sigue desde el valor congelado); no-op desde cualquier otro estado. */
    fun resume(): CookingTimerState =
        if (status == CookingTimerStatus.PAUSED) copy(status = CookingTimerStatus.RUNNING) else this

    /** Devuelve el temporizador a [CookingTimerStatus.IDLE] con `remaining = total`, desde cualquier estado. */
    fun reset(): CookingTimerState =
        copy(status = CookingTimerStatus.IDLE, remainingMillis = totalMillis)

    /**
     * Descuenta [deltaMillis] de la cuenta atras **solo** en [CookingTimerStatus.RUNNING].
     *
     * `deltaMillis <= 0` es no-op. Al llegar a cero el estado pasa a [CookingTimerStatus.FINISHED] y
     * deja de contar: los [tick] posteriores no lo reducen mas (se acota a `0`).
     */
    fun tick(deltaMillis: Long): CookingTimerState {
        if (status != CookingTimerStatus.RUNNING || deltaMillis <= 0L) return this
        val remaining = (remainingMillis - deltaMillis).coerceAtLeast(0L)
        return if (remaining == 0L) {
            copy(status = CookingTimerStatus.FINISHED, remainingMillis = 0L)
        } else {
            copy(remainingMillis = remaining)
        }
    }

    companion object {
        /**
         * Temporizador **Inactivo** de un paso de [minutes] minutos: `total = max(0, minutes) * 60_000`
         * y `remaining = total`. Un paso sin duracion (`minutes <= 0`) da `hasDuration = false`.
         */
        fun of(minutes: Int): CookingTimerState {
            val total = minutes.coerceAtLeast(0).toLong() * 60_000L
            return CookingTimerState(
                status = CookingTimerStatus.IDLE,
                totalMillis = total,
                remainingMillis = total,
            )
        }
    }
}
