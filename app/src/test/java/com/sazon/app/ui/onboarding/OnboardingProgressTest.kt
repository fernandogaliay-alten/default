package com.sazon.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1 y 3 de `docs/specs/onboarding-shell-and-identity.md`: la maquina de pasos del
 * armazon. Logica pura, sin emulador.
 */
class OnboardingProgressTest {

    @Test
    fun el_primer_paso_no_puede_retroceder_y_se_etiqueta_como_1_de_7() {
        val progress = OnboardingProgress()

        assertEquals(7, OnboardingStep.TOTAL_STEPS)
        assertEquals(7, progress.totalSteps)
        assertEquals(1, progress.stepNumber)
        assertEquals(OnboardingStep.IDENTITY, progress.step)
        assertFalse("En el paso 1 no debe poder volverse atras", progress.canGoBack)
        assertEquals("Paso 1 de 7", progress.progressLabel)
        assertFalse("El paso 1 no es el ultimo", progress.isLastStep)
    }

    @Test
    fun avanzar_habilita_atras_y_actualiza_la_etiqueta() {
        val segundo = OnboardingProgress().next()

        assertEquals(2, segundo.stepNumber)
        assertEquals(OnboardingStep.DIET, segundo.step)
        assertTrue(segundo.canGoBack)
        assertEquals("Paso 2 de 7", segundo.progressLabel)
    }

    @Test
    fun atras_desde_el_paso_2_vuelve_al_paso_1() {
        val vuelta = OnboardingProgress().next().back()

        assertEquals(1, vuelta.stepNumber)
        assertFalse(vuelta.canGoBack)
        assertEquals("Paso 1 de 7", vuelta.progressLabel)
    }

    @Test
    fun en_el_ultimo_paso_no_se_avanza_mas_alla() {
        val ultimo = OnboardingProgress(stepIndex = 6)

        assertTrue(ultimo.isLastStep)
        assertEquals(7, ultimo.stepNumber)
        assertEquals(OnboardingStep.KITCHEN, ultimo.step)
        assertEquals("Empezar a cocinar", ultimo.step.nextLabel)
        assertEquals(7, ultimo.next().stepNumber)
        assertEquals(6, ultimo.next().stepIndex)
        assertTrue("Sigue en el ultimo paso", ultimo.next().isLastStep)
    }

    @Test
    fun atras_en_el_primer_paso_no_se_sale_de_rango() {
        val primero = OnboardingProgress(stepIndex = 0)

        assertEquals(0, primero.back().stepIndex)
        assertEquals(1, primero.back().stepNumber)
        assertFalse(primero.back().canGoBack)
    }
}
