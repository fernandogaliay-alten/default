package com.sazon.app.ui.profile

import com.sazon.app.data.image.DishImageSettingsState

/**
 * Microcopy de la seccion **"Imagenes del plato"** del Perfil, **puro y determinista** (sin Compose ni
 * Android): la capa Compose solo pinta lo que sale de aqui (misma disciplina que
 * `SuggestionsRefreshFormat`), de modo que el texto visible se prueba en JVM.
 *
 * Los textos quedan registrados en `DESIGN.md` (seccion "Imagenes del plato"); la persona los revisa
 * en el emulador tras el merge.
 */
object DishImageSettingsCopy {

    /** Titulo de la seccion, con el estilo de seccion ya existente del Perfil. */
    const val SECTION_TITLE = "Imágenes del plato"

    /** Etiqueta del interruptor. */
    const val TOGGLE_LABEL = "Generar imágenes del plato"

    /** Helper que explica el gasto de cuota y la reutilizacion. */
    const val HELPER_TEXT =
        "Las imágenes nuevas se generan con IA y consumen cuota; las que ya existen se reutilizan."

    /** Resumen con el interruptor apagado. */
    const val DISABLED_SUMMARY = "Desactivado: las recetas se muestran sin imagen."

    /**
     * Resumen visible del tope por sesion, derivado **solo** del estado (determinista):
     *
     * - apagado -> [DISABLED_SUMMARY] (manda el interruptor, aunque quede o falte tope);
     * - `remaining == 0` -> tope de la sesion alcanzado, con el aviso de que se reanuda al reiniciar;
     * - `remaining == 1` -> singular ("Queda 1 de N…");
     * - resto -> plural ("Quedan N de N…").
     */
    fun summary(state: DishImageSettingsState): String = when {
        !state.enabled -> DISABLED_SUMMARY
        state.remaining == 0 -> "Tope de esta sesión alcanzado (${state.max} de ${state.max}). " +
            "Se reanudará al reiniciar la app."
        state.remaining == 1 -> "Queda 1 de ${state.max} imágenes en esta sesión."
        else -> "Quedan ${state.remaining} de ${state.max} imágenes en esta sesión."
    }
}
