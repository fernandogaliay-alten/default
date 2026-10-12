package com.sazon.app.ui.cooking

import java.util.Locale

/**
 * Microcopy y formatos **puros** del modo cocina (sin Compose ni Android; testeable en JVM):
 * el progreso ("Paso 2 de 5" + fraccion de barra), la duracion del paso ("10 min") y la cuenta
 * atras ("09:42"), siempre con cifras tabulares en la capa Compose.
 */
object CookingModeFormat {

    /** Aviso de exito **en pantalla** al llegar a cero; el acento es miel/oliva, nunca rojo. */
    const val FINISHED_NOTICE = "¡Listo!"

    /** Etiqueta de progreso del modo: `(stepIndex + 1)` y el total de pasos (microcopy de la captura). */
    fun progressLabel(stepIndex: Int, totalSteps: Int): String = "Paso ${stepIndex + 1} de $totalSteps"

    /**
     * Fraccion de progreso de la barra fina: `(stepIndex + 1) / totalSteps` acotada a `0f..1f`.
     * Con `totalSteps <= 0` devuelve `0f` (sin division por cero).
     */
    fun progressFraction(stepIndex: Int, totalSteps: Int): Float {
        if (totalSteps <= 0) return 0f
        return ((stepIndex + 1).toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
    }

    /** Duracion del paso en la pildora miel, igual que la pildora ambar del Detalle: `"N min"`. */
    fun durationLabel(minutes: Int): String = "$minutes min"

    /**
     * Cuenta atras `mm:ss` en [Locale.ROOT] (cifras tabulares en la capa Compose). [remainingMillis]
     * se acota a `>= 0` y se trunca a **segundos enteros** (`floor`): `600000 -> "10:00"`,
     * `582000 -> "09:42"`, `0 -> "00:00"`.
     */
    fun countdown(remainingMillis: Long): String {
        val totalSeconds = remainingMillis.coerceAtLeast(0L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}
