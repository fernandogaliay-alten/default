package com.sazon.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 4 de `docs/specs/onboarding-shell-and-identity.md`: iniciales del avatar (maximo 2) y
 * `null` cuando no hay nombre. Logica pura, sin emulador.
 */
class AvatarInitialsTest {

    @Test
    fun un_nombre_vacio_o_solo_espacios_no_tiene_iniciales() {
        assertNull(AvatarInitials.of(""))
        assertNull(AvatarInitials.of("   "))
    }

    @Test
    fun una_sola_palabra_da_su_primera_letra_en_mayuscula() {
        assertEquals("L", AvatarInitials.of("Lucía"))
    }

    @Test
    fun dos_palabras_dan_las_dos_iniciales_en_mayuscula() {
        assertEquals("LG", AvatarInitials.of("Lucía Gómez"))
    }

    @Test
    fun los_espacios_sobrantes_no_alteran_las_iniciales() {
        assertEquals("LG", AvatarInitials.of("  Lucía   Gómez  "))
    }

    @Test
    fun nunca_devuelve_mas_de_dos_iniciales() {
        val iniciales = AvatarInitials.of("Ana María López")

        assertEquals("AM", iniciales)
        assertTrue("Debe tener como maximo 2 caracteres", (iniciales?.length ?: 0) <= 2)
    }
}
