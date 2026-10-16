package com.sazon.app.ui.cooking

/**
 * Direccion del **cambio de paso** que puede pedir confirmacion (feature
 * `cooking-mode-step-change-confirmation`): "Siguiente" y "Anterior" de la barra inferior.
 *
 * Las dos direcciones existen para que "Siguiente y Anterior piden lo mismo **cuando la direccion es
 * posible**" sea un **invariante testeado** y para tener un unico sitio donde una divergencia futura
 * fuese **deliberada** (evita el fallo tipico de gatear solo uno de los dos botones); hoy comparten la
 * misma puerta ([CookingStepChangePolicy.requiresConfirmation]), con la unica diferencia de que la
 * direccion tiene que **cambiar de paso** de verdad (en el primer paso "Anterior" esta deshabilitado y
 * en el ultimo la primaria lee "Terminar" y va por la confirmacion de salida).
 */
enum class CookingStepChangePoint {
    /** "Siguiente" (accion primaria de un paso intermedio). */
    NEXT,

    /** "Anterior" (accion secundaria, deshabilitada en el primer paso). */
    PREVIOUS,
}

/**
 * Microcopy del **dialogo de confirmacion de cambio de paso** (constantes puras: sin recursos ni
 * `strings.xml`, mismo criterio que `CookingExitCopy` y `NotificationPermissionCopy`).
 *
 * Es **neutro respecto a la direccion**: vale igual para "Siguiente" y para "Anterior", y **no**
 * interpola el tiempo restante, que es un valor movil mientras la cuenta atras corre detrás del
 * dialogo.
 */
object CookingStepChangeCopy {

    /** Titulo del dialogo. */
    const val TITLE = "¿Cambiar de paso?"

    /** Cuerpo del dialogo: explica que el temporizador del paso actual no se conserva. */
    const val MESSAGE = "El temporizador de este paso se reiniciará con el del paso nuevo."

    /** Accion principal: cambia de paso y reinicia el temporizador al del paso nuevo, como hoy. */
    const val CONFIRM = "Cambiar de paso"

    /** Descarte: se queda en el paso actual **sin cambios**, con su temporizador intacto. */
    const val DISMISS = "Seguir en este paso"
}

/**
 * Decision **pura** de la confirmacion al cambiar de paso en el Modo cocina (sin Compose ni Android,
 * sin leer el reloj y sin la `Recipe`: solo depende de [CookingModeState]; testeable en JVM, patron
 * [CookingExitPolicy]/[CookingAlarmPolicy]).
 *
 * Regla unica: hay que confirmar **solo** si el temporizador del paso actual esta **En marcha o
 * Pausado** (lo unico que se pierde al cambiar de paso: `next`/`previous` lo reemplazan por el del paso
 * nuevo, Inactivo) **y** esa [point] cambia realmente de paso. El **marcado** (`completedOrders`) **no**
 * cuenta —a diferencia de [CookingExitPolicy]— porque persiste entre pasos: esa es la diferencia clave
 * entre las dos politicas. Un temporizador **Terminado** tampoco cuenta (su tiempo ya se consumio,
 * coherente con la politica de salida) y un paso **sin duracion** tampoco (ni se pinta el bloque del
 * temporizador). Sin nada que perder, el cambio de paso es **directo**, como antes de esta feature.
 *
 * La **guarda de direccion** ([CookingStepChangePoint.NEXT] exige `!isLastStep` y
 * [CookingStepChangePoint.PREVIOUS] exige `!isFirstStep`, mas el caso defensivo `!isEmpty`) no cambia el
 * comportamiento observable —la barra inferior ya garantiza que "Anterior" no se pulsa en el primer paso
 * y que en el ultimo la primaria es "Terminar"—, pero convierte "un no-op nunca pide confirmacion" en
 * una **invariante probada** en JVM.
 *
 * La decision **no** ejecuta nada: quien la usa (la capa Compose de `CookingModeScreen`) decide si abre
 * el dialogo o si ejecuta la navegacion aceptada (`state.next(recipe.steps)` / `state.previous(...)`).
 */
object CookingStepChangePolicy {

    /**
     * `true` si [point] debe pedir confirmacion antes de cambiar de paso con el estado [state]; `false`
     * si el cambio es directo. El `when` sobre la direccion hace explicito que ninguna queda sin cubrir.
     */
    fun requiresConfirmation(state: CookingModeState, point: CookingStepChangePoint): Boolean =
        hasTimerInProgress(state) && changesStep(state, point)

    /**
     * "Hay algo que perder": el temporizador del paso actual En marcha o Pausado. Inactivo y Terminado
     * (con o sin pasos marcados) no cuentan.
     */
    private fun hasTimerInProgress(state: CookingModeState): Boolean = when (state.timer.status) {
        CookingTimerStatus.RUNNING,
        CookingTimerStatus.PAUSED,
        -> true

        CookingTimerStatus.IDLE,
        CookingTimerStatus.FINISHED,
        -> false
    }

    /** `true` si [point] cambia de paso de verdad (no-op acotado y receta sin pasos nunca confirman). */
    private fun changesStep(state: CookingModeState, point: CookingStepChangePoint): Boolean = when (point) {
        CookingStepChangePoint.NEXT -> !state.isEmpty && !state.isLastStep
        CookingStepChangePoint.PREVIOUS -> !state.isEmpty && !state.isFirstStep
    }
}
