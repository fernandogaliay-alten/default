package com.sazon.app.ui.onboarding

/**
 * Los 7 pasos del Onboarding, en orden. Es la fuente del indice de paso y de las etiquetas del
 * armazon compartido; los pasos 2-7 son marcadores en esta feature (`onboarding-shell-and-identity`).
 *
 * Logica pura: no importa Compose ni Android, de modo que la maquina de pasos se testea en JVM.
 */
enum class OnboardingStep(
    val title: String,
    val nextLabel: String,
) {
    IDENTITY(title = "Bienvenida", nextLabel = "Empezar"),
    DIET(title = "¿Cómo te alimentas?", nextLabel = "Siguiente"),
    STYLE(title = "¿Algún estilo que te importe?", nextLabel = "Siguiente"),
    CUISINES(title = "Tus cocinas favoritas", nextLabel = "Siguiente"),
    INGREDIENTS(title = "¿Qué te gusta cocinar?", nextLabel = "Siguiente"),
    RESTRICTIONS(title = "Alergias e intolerancias", nextLabel = "Siguiente"),
    KITCHEN(title = "Tu cocina", nextLabel = "Empezar a cocinar"),
    ;

    companion object {
        /** Numero total de pasos del onboarding. */
        val TOTAL_STEPS: Int = entries.size
    }
}
