package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.Recipe

/**
 * Foto **pura** de la **sesion del Modo cocina**: la receta, el paso actual, el marcado y el
 * temporizador, mas el **instante de fin** proyectado (`ELAPSED_REALTIME`), que es lo unico que hace
 * falta para reanudar la cuenta atras tras morir el proceso (`docs/specs/cooking-timer-foreground-service.md`,
 * escenarios 1, 3 y 6).
 *
 * Es **determinista** y **no lee el reloj**: el instante entra siempre **inyectado** (`nowMillis`), igual
 * que el resto del modelo del temporizador ([CookingTimerState]). Es serializable por construccion
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

    /**
     * `true` si **la via de la notificacion** debe reabrir el paso: [isResumable] **o** una sesion en
     * Terminado (el aviso "¡Listo!" sigue en la bandeja y tocar vuelve al paso, ya a cero). `Inactivo`
     * queda fuera: no hay nada que reabrir y no puede coexistir con un aviso de fin.
     *
     * Es un predicado **compuesto** a proposito: [isResumable] conserva su semantica aceptada ("hay
     * cuenta atras que reanudar") y sigue siendo codigo vivo. Lo consume `MainShell` en el arranque por
     * `EXTRA_RESUME_COOKING`; un arranque **normal** no lo consulta.
     */
    val reopensFromNotification: Boolean
        get() = isResumable || timer.status == CookingTimerStatus.FINISHED

    /** Estado del modo cocina tal cual se persistio (sin recalcular la cuenta atras). */
    fun mode(): CookingModeState = CookingModeState(
        stepIndex = stepIndex,
        totalSteps = recipe.steps.size,
        completedOrders = completedOrders,
        timer = timer,
    )

    /**
     * Estado del modo cocina **recalculado** con el instante [nowMillis]: En marcha, lo que queda de
     * cuenta atras sale de [CookingTimerState.at] sobre el **ancla** de la fila persistida
     * (`endAtElapsedMillis`), de modo que la pantalla reabre con el **mismo** reloj que la sesion; si el
     * instante ya paso, el temporizador queda **Terminado** con `remaining = 0` (Scenario 6: muerte del
     * proceso a mitad o despues del fin). Los demas estados se reconstruyen tal cual.
     *
     * Defensivo y **sin lanzar**: si `endAtElapsedMillis` fuera `null` con el temporizador En marcha
     * (fila corrupta), degrada al `remainingMillis` persistido.
     */
    fun resumedMode(nowMillis: Long): CookingModeState {
        val persisted = mode()
        if (!isRunning) return persisted
        val endAt = endAtElapsedMillis ?: return persisted
        return persisted.withTimer(timer.copy(endAtElapsedMillis = endAt).at(nowMillis))
    }

    /**
     * Instante de fin (`ELAPSED_REALTIME`) que le corresponde a la sesion En marcha con el instante
     * [nowMillis] inyectado: el **ancla** del temporizador ([CookingTimerState.projectedEndMillis]).
     * `null` fuera de Marcha o sin tiempo que contar (nada que rearrancar).
     */
    fun projectedEndMillis(nowMillis: Long): Long? = timer.projectedEndMillis(nowMillis)

    companion object {

        /**
         * Proyecta la sesion del modo [mode] sobre [recipe] con el instante [nowMillis] inyectado:
         * `endAtElapsedMillis` **reutiliza el ancla** del temporizador (`projectedEndMillis`), de modo
         * que UI, sesion persistida, alarma y servicio comparten el **mismo** `Long` (no hay hasta un
         * latido de retraso). Es `null` en Inactivo, Pausado y Terminado (pausar/reiniciar/cambiar de
         * paso/llegar a cero lo paran, Scenario 3 y 4).
         */
        fun of(mode: CookingModeState, recipe: Recipe, nowMillis: Long): CookingSession {
            val timer = mode.timer
            return CookingSession(
                recipe = recipe,
                stepIndex = mode.stepIndex,
                completedOrders = mode.completedOrders,
                timer = timer,
                endAtElapsedMillis = if (
                    timer.status == CookingTimerStatus.RUNNING && timer.remainingMillis > 0L
                ) {
                    timer.projectedEndMillis(nowMillis)
                } else {
                    null
                },
            )
        }
    }
}
