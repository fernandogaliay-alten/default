package com.sazon.app.ui.profile

/**
 * Formateador **puro** de la antiguedad del Lote de sugerencias y microcopy de la seccion
 * "Sugerencias" del Perfil. Sin Compose ni Android: se testea en JVM.
 *
 * La seccion no persiste ninguna fecha "de refresco": deriva la antiguedad de
 * `SuggestionBatch.generatedAt`, que ya es el sello del Lote observado. La escala es de **dias**
 * ("hoy"/"hace N dias"), como pide `docs/design/stitch-prompt.md` seccion G; el `now` lo inyecta la
 * capa Compose, capturado una vez, de modo que la funcion es determinista.
 */
object SuggestionsRefreshFormat {

    /** Milisegundos de un dia; base de la escala de dias de la antiguedad. */
    const val DAY_MILLIS: Long = 24L * 60 * 60 * 1_000

    /** Texto del boton de refresco cuando no hay una peticion en vuelo. */
    const val ACTION_LABEL = "Actualizar sugerencias"

    /** Texto del boton mientras un refresco esta en vuelo. */
    const val REFRESHING_LABEL = "Actualizando…"

    /** Helper bajo la fecha que explica cuando se renueva el Lote. */
    const val HELPER_TEXT = "Se renuevan cuando cambian tus preferencias."

    /** Respaldo cuando `observe()` aun no ha emitido ningun Lote (`generatedAt == null`). */
    const val NO_BATCH_LABEL = "Aún no hay sugerencias."

    /**
     * Etiqueta de la antiguedad relativa del Lote.
     *
     * `generatedAt == null` cae al respaldo [NO_BATCH_LABEL]. En otro caso cuenta dias completos
     * transcurridos desde `generatedAt` hasta `now`: `0` -> "hoy", `1` -> "hace 1 dia" y `>= 2` ->
     * "hace N dias". Un `now` anterior a `generatedAt` (cambio de hora del dispositivo) se degrada a
     * "hoy" sin producir dias negativos.
     */
    fun lastUpdatedLabel(generatedAt: Long?, now: Long): String {
        if (generatedAt == null) return NO_BATCH_LABEL
        val elapsedDays = (now - generatedAt).coerceAtLeast(0L) / DAY_MILLIS
        return when (elapsedDays) {
            0L -> "Última actualización: hoy"
            1L -> "Última actualización: hace 1 día"
            else -> "Última actualización: hace $elapsedDays días"
        }
    }
}
