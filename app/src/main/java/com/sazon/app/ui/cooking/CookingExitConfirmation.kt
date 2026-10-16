package com.sazon.app.ui.cooking

/**
 * Punto de salida del **Modo cocina** que puede pedir confirmacion (feature
 * `cooking-mode-exit-confirmation`): el "Atras" de la cabecera (o el **gesto/tecla atras del sistema**,
 * que la capa Compose enruta por el mismo sitio con `BackHandler`) y el "Terminar" de la barra inferior.
 *
 * Los dos puntos existen para que "Atras y Terminar piden lo mismo" sea un **invariante testeado** y para
 * tener un unico sitio donde una divergencia futura fuese **deliberada**; hoy comparten la misma puerta
 * ([CookingExitPolicy.requiresConfirmation]).
 */
enum class CookingExitPoint {
    /** "Atras" de la cabecera y gesto/tecla atras del sistema. */
    BACK_BUTTON,

    /** "Terminar" (accion primaria del ultimo paso, o "Atras" con una receta sin pasos). */
    FINISH,
}

/**
 * Microcopy del **dialogo de confirmacion de salida** (constantes puras: sin recursos ni
 * `strings.xml`, mismo criterio que `NotificationPermissionCopy` y que el contenido del aviso aceptado
 * en `CookingNotifications`).
 */
object CookingExitCopy {

    /** Titulo del dialogo. */
    const val TITLE = "¿Salir del modo cocina?"

    /** Cuerpo del dialogo: explica que se pierde si se sale. */
    const val MESSAGE = "Si sales ahora perderás el progreso de tu receta."

    /** Accion principal: sale y descarta la sesion, como la salida explicita aceptada. */
    const val CONFIRM = "Salir"

    /** Descarte: se queda en el modo **sin cambios**. */
    const val DISMISS = "Seguir cocinando"
}

/**
 * Decision **pura** de la confirmacion de salida del Modo cocina (sin Compose ni Android, sin leer el
 * reloj y sin la `Recipe`: solo depende de [CookingModeState]; testeable en JVM, patron
 * [CookingAlarmPolicy]/[NotificationPermissionPolicy]).
 *
 * Regla unica: hay que confirmar si hay **algo que perder** — el temporizador del paso actual
 * **En marcha o Pausado** (incluido el tiempo congelado de una pausa) o algun paso marcado como hecho
 * (`completedOrders` no vacio) —. Un temporizador **Terminado** *solo* **no** cuenta: su tiempo ya se
 * consumio; con pasos marcados si. Sin nada que perder la salida es **directa**, como antes de esta
 * feature. El caso defensivo de una receta sin pasos (Inactivo, sin marcados) tambien sale directo sin
 * lanzar.
 *
 * La decision **no** ejecuta nada: quien la usa (la capa Compose de `CookingModeScreen`) decide si abre
 * el dialogo o si activa la salida explicita aceptada (`exiting` -> `session.clear()` -> `onClose()` ->
 * `onDispose` con `CookingAlarmPolicy.onExit()`).
 */
object CookingExitPolicy {

    /**
     * `true` si [point] debe pedir confirmacion antes de salir con el estado [state]; `false` si la
     * salida es directa. Los dos puntos de salida comparten la **misma** condicion: el `when` hace
     * explicito que ninguno queda sin cubrir (una divergencia futura tendria que ser deliberada y
     * romperia el test JVM de la decision).
     */
    fun requiresConfirmation(state: CookingModeState, point: CookingExitPoint): Boolean = when (point) {
        CookingExitPoint.BACK_BUTTON -> hasSomethingToLose(state)
        CookingExitPoint.FINISH -> hasSomethingToLose(state)
    }

    /**
     * "Algo que perder": temporizador En marcha o Pausado, o algun paso marcado. Terminado/Inactivo
     * cuentan solo por los pasos marcados.
     */
    private fun hasSomethingToLose(state: CookingModeState): Boolean = when (state.timer.status) {
        CookingTimerStatus.RUNNING,
        CookingTimerStatus.PAUSED,
        -> true

        CookingTimerStatus.IDLE,
        CookingTimerStatus.FINISHED,
        -> state.completedOrders.isNotEmpty()
    }
}
