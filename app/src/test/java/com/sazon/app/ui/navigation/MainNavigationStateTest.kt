package com.sazon.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenarios 6 y 7 de `docs/specs/home-offline-blocks.md`: barra inferior de tres destinos y pagina
 * empujada del Generador. Logica pura, sin Compose ni Android.
 */
class MainNavigationStateTest {

    @Test
    fun la_barra_tiene_tres_destinos_en_orden_inicio_favoritos_perfil() {
        assertEquals(
            listOf(MainTab.HOME, MainTab.FAVORITES, MainTab.PROFILE),
            MainTab.entries.toList(),
        )
        assertEquals("Inicio", MainTab.HOME.label)
        assertEquals("Favoritos", MainTab.FAVORITES.label)
        assertEquals("Perfil", MainTab.PROFILE.label)
    }

    @Test
    fun por_defecto_arranca_en_inicio_y_sin_pagina_empujada() {
        val estado = MainNavigationState()

        assertEquals(MainTab.HOME, estado.tab)
        assertNull(estado.pushed)
    }

    @Test
    fun select_tab_cambia_la_pestana_activa() {
        val estado = MainNavigationState().selectTab(MainTab.PROFILE)

        assertEquals(MainTab.PROFILE, estado.tab)
    }

    @Test
    fun push_conserva_la_pestana_y_marca_la_pagina_empujada() {
        val estado = MainNavigationState(tab = MainTab.FAVORITES).push(PushedScreen.GENERATOR)

        assertEquals(MainTab.FAVORITES, estado.tab)
        assertEquals(PushedScreen.GENERATOR, estado.pushed)
    }

    @Test
    fun pop_vuelve_a_la_home_sin_perder_la_pestana() {
        val estado = MainNavigationState(tab = MainTab.FAVORITES)
            .push(PushedScreen.GENERATOR)
            .pop()

        assertEquals(MainTab.FAVORITES, estado.tab)
        assertNull(estado.pushed)
    }

    @Test
    fun pop_sin_pagina_empujada_no_cambia_nada() {
        val estado = MainNavigationState(tab = MainTab.PROFILE)

        assertEquals(estado, estado.pop())
    }
}
