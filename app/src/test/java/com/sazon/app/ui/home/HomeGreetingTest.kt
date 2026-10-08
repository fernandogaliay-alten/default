package com.sazon.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Escenarios 2 y 3 de `docs/specs/home-offline-blocks.md`: saludo por franja horaria y degradacion sin
 * nombre. Logica pura, sin emulador ni Android.
 */
class HomeGreetingTest {

    @Test
    fun por_la_manana_saluda_con_buenos_dias() {
        assertEquals("Buenos días, Lucía", HomeGreeting.text("Lucía", 9))
    }

    @Test
    fun por_la_tarde_saluda_con_buenas_tardes() {
        assertEquals("Buenas tardes, Lucía", HomeGreeting.text("Lucía", 14))
    }

    @Test
    fun por_la_noche_saluda_con_buenas_noches() {
        assertEquals("Buenas noches, Lucía", HomeGreeting.text("Lucía", 22))
    }

    @Test
    fun sin_nombre_cae_al_literal_buenas_tardes() {
        assertEquals("Buenas tardes", HomeGreeting.text(null, 9))
        assertEquals("Buenas tardes", HomeGreeting.text("", 14))
        assertEquals("Buenas tardes", HomeGreeting.text("   ", 22))
    }

    @Test
    fun el_nombre_se_recorta_antes_de_saludar() {
        assertEquals("Buenas tardes, Lucía", HomeGreeting.text("  Lucía  ", 14))
    }

    @Test
    fun las_fronteras_de_las_bandas_son_5_11_12_20_y_21() {
        assertEquals("Buenas noches, Ana", HomeGreeting.text("Ana", 4))
        assertEquals("Buenos días, Ana", HomeGreeting.text("Ana", 5))
        assertEquals("Buenos días, Ana", HomeGreeting.text("Ana", 11))
        assertEquals("Buenas tardes, Ana", HomeGreeting.text("Ana", 12))
        assertEquals("Buenas tardes, Ana", HomeGreeting.text("Ana", 20))
        assertEquals("Buenas noches, Ana", HomeGreeting.text("Ana", 21))
    }

    @Test
    fun sin_nombre_no_devuelve_coma_suelta_ni_espacios_de_mas() {
        val sinNombre = HomeGreeting.text("   ", 14)

        assertFalse("No debe quedar una coma suelta", sinNombre.contains(","))
        assertEquals(sinNombre, sinNombre.trim())
    }
}
