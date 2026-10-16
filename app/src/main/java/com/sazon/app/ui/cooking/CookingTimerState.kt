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
 * **Reloj unico**: cuando la cuenta atras esta En marcha el temporizador lleva el **instante de fin
 * absoluto** [endAtElapsedMillis] (misma base `ELAPSED_REALTIME` que la alarma y el servicio) y el
 * restante se **deriva** de el ([at]); nunca se acumulan deltas fijos, asi que los latidos tardios o
 * desiguales de la capa Compose **no** acumulan deriva y el valor mostrado y el de la notificacion del
 * sistema dicen siempre lo mismo.
 *
 * Es **determinista**: no lee el reloj; el instante entra siempre **inyectado** (`nowMillis`). Las
 * transiciones no validas son **no-op** (devuelven el mismo estado) y ninguna operacion muta el
 * receptor.
 *
 * Invariante de [endAtElapsedMillis]: **no nulo solo En marcha con tiempo que contar** (`remaining > 0`),
 * igual que `CookingSession`; en Inactivo, Pausado y Terminado no hay nada que proyectar. Por eso la
 * persistencia ([CookingSession]) no necesita mas que ese `Long` para recalcular lo que queda.
 *
 * [hasDuration] distingue un paso con duracion (`minutes > 0`) de uno sin ella: la UI oculta el
 * bloque completo del temporizador cuando es `false` (escenario 6 de la spec).
 */
data class CookingTimerState(
    val status: CookingTimerStatus,
    val totalMillis: Long,
    val remainingMillis: Long,
    val endAtElapsedMillis: Long? = null,
) {

    /** `true` si el paso tiene una duracion real (`minutes > 0`); si no, la UI oculta el bloque. */
    val hasDuration: Boolean get() = totalMillis > 0

    /** `true` mientras la cuenta atras esta activa (el reloj de la UI solo late en este estado). */
    val isRunning: Boolean get() = status == CookingTimerStatus.RUNNING

    /** `true` cuando la cuenta atras llego a cero y dejo de contar. */
    val isFinished: Boolean get() = status == CookingTimerStatus.FINISHED

    /**
     * `IDLE -> RUNNING` **anclando** el fin en `nowMillis + remainingMillis` (solo si queda tiempo que
     * contar); no-op desde cualquier otro estado.
     */
    fun start(nowMillis: Long): CookingTimerState =
        if (status == CookingTimerStatus.IDLE) anchoredAt(nowMillis) else this

    /**
     * `RUNNING -> PAUSED` **congelando** el tiempo en lo que queda en [nowMillis] (`at(now).remaining`)
     * y **anulando el ancla**; no-op desde cualquier otro estado.
     */
    fun pause(nowMillis: Long): CookingTimerState =
        if (status == CookingTimerStatus.RUNNING) {
            copy(
                status = CookingTimerStatus.PAUSED,
                remainingMillis = at(nowMillis).remainingMillis,
                endAtElapsedMillis = null,
            )
        } else {
            this
        }

    /**
     * `PAUSED -> RUNNING` **reanclando** el fin en `nowMillis + remainingMillis`; no-op desde cualquier
     * otro estado.
     */
    fun resume(nowMillis: Long): CookingTimerState =
        if (status == CookingTimerStatus.PAUSED) anchoredAt(nowMillis) else this

    /**
     * Devuelve el temporizador a [CookingTimerStatus.IDLE] con `remaining = total` y **sin ancla**,
     * desde cualquier estado.
     */
    fun reset(): CookingTimerState =
        copy(status = CookingTimerStatus.IDLE, remainingMillis = totalMillis, endAtElapsedMillis = null)

    /**
     * Proyeccion **exacta** del temporizador en el instante [nowMillis]: si no esta En marcha devuelve
     * el mismo estado; si lo esta, `remaining = (endAt - now).coerceAtLeast(0)` (jamas negativo) y, al
     * alcanzar o pasar el fin, pasa a [CookingTimerStatus.FINISHED] con `remaining = 0` y **sin ancla**.
     *
     * Es la **unica** forma de hacer latir la cuenta atras: el llamante lee el reloj y lo inyecta; una
     * lectura tardia no acumula error porque el valor no depende del delta. Defensivo con una fila
     * corrupta sin ancla: degrada al restante guardado (no lanza).
     */
    fun at(nowMillis: Long): CookingTimerState {
        if (status != CookingTimerStatus.RUNNING) return this
        val endAt = endAtElapsedMillis ?: return this
        val remaining = (endAt - nowMillis).coerceAtLeast(0L)
        return if (remaining == 0L) {
            copy(status = CookingTimerStatus.FINISHED, remainingMillis = 0L, endAtElapsedMillis = null)
        } else {
            copy(remainingMillis = remaining)
        }
    }

    /**
     * Instante de fin (`ELAPSED_REALTIME`) que le corresponde al temporizador con el instante
     * [nowMillis] inyectado: el **ancla** si la tiene y, si no, `nowMillis + remainingMillis`. `null`
     * fuera de Marcha o sin tiempo que contar (nada que programar).
     */
    fun projectedEndMillis(nowMillis: Long): Long? =
        if (isRunning && remainingMillis > 0L) {
            endAtElapsedMillis ?: (nowMillis + remainingMillis)
        } else {
            null
        }

    /** Copia En marcha con el ancla puesta en `nowMillis + remainingMillis` (o sin ancla si es cero). */
    private fun anchoredAt(nowMillis: Long): CookingTimerState = copy(
        status = CookingTimerStatus.RUNNING,
        endAtElapsedMillis = if (remainingMillis > 0L) nowMillis + remainingMillis else null,
    )

    companion object {
        /**
         * Temporizador **Inactivo** de un paso de [minutes] minutos: `total = max(0, minutes) * 60_000`
         * y `remaining = total`, **sin ancla**. Un paso sin duracion (`minutes <= 0`) da
         * `hasDuration = false`.
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
