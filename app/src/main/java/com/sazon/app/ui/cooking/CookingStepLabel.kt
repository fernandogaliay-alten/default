package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.RecipeStep

/**
 * Etiqueta **pura** del paso cuyo temporizador corre: el numero en la microcopy ya aceptada del
 * encabezado del Modo cocina ("Paso N de M", [CookingModeFormat.progressLabel]) y el **titulo** del
 * paso.
 *
 * Es la **frontera de privacidad** del temporizador del sistema (feature
 * `cooking-timer-notification-step-label`, ADR 0007/0008): es lo **unico** derivado de la receta que
 * puede cruzar a la notificacion persistente, por decision explicita de la persona. Ni la descripcion
 * del paso, ni los ingredientes, ni las notas, ni los tips, ni la nutricion ni la clave de IA cruzan;
 * la garantia sigue siendo **estructural** (la notificacion no recibe la `Recipe`).
 *
 * Sin Compose ni Android (testeable en JVM) y sin leer el reloj: se **deriva** del paso actual + la
 * receta en el momento de armar el trabajo del sistema (el gesto del temporizador o el rearme al
 * reanudar), asi que **no** se persiste ni puede quedar obsoleta.
 */
data class CookingStepLabel(val progress: String, val title: String) {

    companion object {

        /**
         * Etiqueta del paso `stepIndex` (0-based) de `totalSteps` con el titulo de [step]: el numero
         * reutiliza [CookingModeFormat.progressLabel] (misma cadena que el encabezado, sin microcopy
         * paralela) y el titulo es el del paso.
         *
         * Degradacion defensiva (patron `steps.getOrNull`): devuelve `null` si no hay paso o si la
         * receta no trae pasos, y entonces la notificacion persistente cae a su titulo generico.
         */
        fun of(stepIndex: Int, totalSteps: Int, step: RecipeStep?): CookingStepLabel? {
            if (step == null || totalSteps <= 0) return null
            return CookingStepLabel(
                progress = CookingModeFormat.progressLabel(stepIndex, totalSteps),
                title = step.title,
            )
        }
    }
}
