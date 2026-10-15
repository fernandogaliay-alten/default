package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe

/**
 * Foto **pura** de la **sesion del Modo cocina**: la receta, el paso actual, el marcado y el
 * temporizador, mas el **instante de fin** proyectado (`ELAPSED_REALTIME`), que es lo unico que hace
 * falta para reanudar la cuenta atras tras morir el proceso (`docs/specs/cooking-timer-foreground-service.md`,
 * escenarios 1, 3 y 6).
 *
 * Es **determinista** y **no lee el reloj**: el instante entra siempre **inyectado** (`nowMillis`), igual
 * que el tiempo de [CookingTimerState.tick] entra por `deltaMillis`. Es serializable por construccion
 * (la `Recipe` ya lo es) y no importa `android.*` ni `androidx.*`, de modo que se prueba en JVM.
 *
 * [endAtElapsedMillis] es **no nulo solo En marcha**: en Inactivo, Pausado y Terminado no hay nada que
 * rearrancar (Scenario 3), asi que la persistencia de esos estados no proyecta fin.
 */
data class CookingSession(
    val recipe: Recipe,
    val stepIndex: Int,
    val completedOrders: Set<Int>,
    val timer: CookingTimerState,
    val endAtElapsedMillis: Long?,
) {

    /** `true` con la cuenta atras activa (es el unico estado con instante de fin proyectado). */
    val isRunning: Boolean get() = timer.status == CookingTimerStatus.RUNNING

    /**
     * `true` si el toque en la notificacion debe devolver al paso: **En marcha o Pausado** (un
     * Terminado solo se reanuda si el usuario viene de la notificacion, Scenario 8).
     */
    val isResumable: Boolean get() = isRunning || timer.status == CookingTimerStatus.PAUSED

    /** Estado del modo cocina tal cual se persistio (sin recalcular la cuenta atras). */
    fun mode(): CookingModeState = CookingModeState(
        stepIndex = stepIndex,
        totalSteps = recipe.steps.size,
        completedOrders = completedOrders,
        timer = timer,
    )

    /**
     * Estado del modo cocina **recalculado** con el instante [nowMillis]: En marcha, lo que queda de
     * cuenta atras es `endAtElapsedMillis - nowMillis` y, si el instante ya paso, el temporizador queda
     * **Terminado** con `remaining = 0` (Scenario 6: muerte del proceso a mitad o despues del fin). Los
     * demas estados se reconstruyen tal cual.
     *
     * Defensivo y **sin lanzar**: si `endAtElapsedMillis` fuera `null` con el temporizador En marcha
     * (fila corrupta), degrada al `remainingMillis` persistido.
     */
    fun resumedMode(nowMillis: Long): CookingModeState {
        val persisted = mode()
        if (!isRunning) return persisted
        val endAt = endAtElapsedMillis ?: return persisted
        val remaining = (endAt - nowMillis).coerceAtLeast(0L)
        return persisted.withTimer(
            timer.copy(
                status = if (remaining == 0L) CookingTimerStatus.FINISHED else CookingTimerStatus.RUNNING,
                remainingMillis = remaining,
            ),
        )
    }

    /**
     * Instante de fin (`ELAPSED_REALTIME`) que le corresponde a la sesion En marcha con el instante
     * [nowMillis] inyectado: `nowMillis + remainingMillis`. `null` fuera de Marcha o sin tiempo que
     * contar (nada que rearrancar).
     */
    fun projectedEndMillis(nowMillis: Long): Long? =
        if (isRunning && timer.remainingMillis > 0L) nowMillis + timer.remainingMillis else null

    companion object {

        /**
         * Proyecta la sesion del modo [mode] sobre [recipe] con el instante [nowMillis] inyectado:
         * `endAtElapsedMillis = nowMillis + timer.remainingMillis` **solo** En marcha con tiempo que
         * contar; `null` en Inactivo, Pausado y Terminado (pausar/reiniciar/cambiar de paso/llegar a
         * cero lo paran, Scenario 3 y 4).
         */
        fun of(mode: CookingModeState, recipe: Recipe, nowMillis: Long): CookingSession = CookingSession(
            recipe = recipe,
            stepIndex = mode.stepIndex,
            completedOrders = mode.completedOrders,
            timer = mode.timer,
            endAtElapsedMillis = if (
                mode.timer.status == CookingTimerStatus.RUNNING && mode.timer.remainingMillis > 0L
            ) {
                nowMillis + mode.timer.remainingMillis
            } else {
                null
            },
        )
    }
}
