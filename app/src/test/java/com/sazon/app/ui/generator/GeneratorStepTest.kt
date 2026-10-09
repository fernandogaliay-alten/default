package com.sazon.app.ui.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 7 (navegacion pura del asistente) y microcopy de `docs/specs/generator-ingredients-step.md`:
 * `GeneratorStep` materializa el asistente de dos pasos, sin salirse de rango. Sin Compose ni Android.
 */
class GeneratorStepTest {

    @Test
    fun ingredients_avanza_a_settings() {
        assertEquals(GeneratorStep.SETTINGS, GeneratorStep.INGREDIENTS.next())
        assertFalse(GeneratorStep.INGREDIENTS.isLast)
    }

    @Test
    fun settings_es_el_ultimo_y_no_avanza() {
        assertTrue(GeneratorStep.SETTINGS.isLast)
        assertNull("El ultimo paso no tiene siguiente", GeneratorStep.SETTINGS.next())
    }

    @Test
    fun ingredients_no_retrocede() {
        assertNull("El primer paso (paso 1) no tiene anterior", GeneratorStep.INGREDIENTS.previous())
    }

    @Test
    fun settings_retrocede_a_ingredients() {
        assertEquals(GeneratorStep.INGREDIENTS, GeneratorStep.SETTINGS.previous())
    }

    @Test
    fun microcopy_de_los_pasos() {
        assertEquals("¿Qué tienes?", GeneratorStep.INGREDIENTS.title)
        assertEquals("Continuar", GeneratorStep.INGREDIENTS.primaryLabel)
        assertEquals("Ajusta tu receta", GeneratorStep.SETTINGS.title)
        assertEquals("Generar recetas", GeneratorStep.SETTINGS.primaryLabel)
    }
}
