package com.sazon.app.ui.onboarding

/**
 * Maquina de pasos del Onboarding, pura y testeable en JVM.
 *
 * [stepIndex] es base cero y se recorta al rango valido: [next] no avanza mas alla del ultimo paso y
 * [back] no retrocede antes del primero. El progreso se muestra como "Paso N de 7".
 */
data class OnboardingProgress(
    val totalSteps: Int = OnboardingStep.TOTAL_STEPS,
    val stepIndex: Int = 0,
) {
    init {
        require(totalSteps >= 1) { "El onboarding debe tener al menos un paso" }
    }

    private val safeIndex: Int = stepIndex.coerceIn(0, totalSteps - 1)

    val stepNumber: Int get() = safeIndex + 1

    val step: OnboardingStep get() = OnboardingStep.entries[safeIndex]

    val canGoBack: Boolean get() = safeIndex > 0

    val isLastStep: Boolean get() = safeIndex == totalSteps - 1

    val progressLabel: String get() = "Paso $stepNumber de $totalSteps"

    fun next(): OnboardingProgress =
        copy(stepIndex = (safeIndex + 1).coerceAtMost(totalSteps - 1))

    fun back(): OnboardingProgress =
        copy(stepIndex = (safeIndex - 1).coerceAtLeast(0))
}
