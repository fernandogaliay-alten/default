package com.sazon.app.ui.motion

import com.sazon.app.ui.navigation.MainTab
import com.sazon.app.ui.navigation.PushedScreen
import com.sazon.app.ui.theme.SazonMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Selector de direccion ([MotionPolicy]) y sus desplazamientos, JVM puro (sin Compose ni Android).
 *
 * Cubre el escenario 8 de `docs/specs/screen-transitions.md`, con casos negativos: `FORWARD` y
 * `BACKWARD` nunca coinciden y `NONE` no desplaza.
 */
class MotionPolicyTest {

    @Test
    fun la_pestana_avanza_adelante_y_retrocede_atras() {
        assertEquals(MotionDirection.FORWARD, MotionPolicy.tabChange(MainTab.HOME, MainTab.FAVORITES))
        assertEquals(MotionDirection.FORWARD, MotionPolicy.tabChange(MainTab.FAVORITES, MainTab.PROFILE))
        assertEquals(MotionDirection.BACKWARD, MotionPolicy.tabChange(MainTab.PROFILE, MainTab.FAVORITES))
        assertEquals(MotionDirection.BACKWARD, MotionPolicy.tabChange(MainTab.FAVORITES, MainTab.HOME))
        assertEquals(MotionDirection.NONE, MotionPolicy.tabChange(MainTab.HOME, MainTab.HOME))
    }

    @Test
    fun el_indice_de_los_pasos_define_la_direccion() {
        assertEquals(MotionDirection.FORWARD, MotionPolicy.directionOf(0, 1))
        assertEquals(MotionDirection.FORWARD, MotionPolicy.directionOf(2, 3))
        assertEquals(MotionDirection.BACKWARD, MotionPolicy.directionOf(3, 2))
        assertEquals(MotionDirection.NONE, MotionPolicy.directionOf(3, 3))
        assertEquals(MotionDirection.NONE, MotionPolicy.directionOf(0, 0))
    }

    @Test
    fun empujar_una_pagina_es_adelante_y_sacarla_atras() {
        assertEquals(MotionDirection.FORWARD, MotionPolicy.pageChange(null, PushedScreen.GENERATOR))
        assertEquals(MotionDirection.FORWARD, MotionPolicy.pageChange(PushedScreen.SEARCH, PushedScreen.GENERATOR))
        assertEquals(MotionDirection.BACKWARD, MotionPolicy.pageChange(PushedScreen.GENERATOR, null))
        assertEquals(MotionDirection.NONE, MotionPolicy.pageChange(null, null))
        assertEquals(MotionDirection.NONE, MotionPolicy.pageChange(PushedScreen.SEARCH, PushedScreen.SEARCH))
    }

    @Test
    fun los_desplazamientos_acompanan_a_la_direccion() {
        assertEquals(SazonMotion.SLIDE_OFFSET_DP, MotionPolicy.enterOffsetDp(MotionDirection.FORWARD))
        assertEquals(-SazonMotion.SLIDE_OFFSET_DP, MotionPolicy.enterOffsetDp(MotionDirection.BACKWARD))
        assertEquals(0, MotionPolicy.enterOffsetDp(MotionDirection.NONE))
        assertEquals(-SazonMotion.SLIDE_OFFSET_DP, MotionPolicy.exitOffsetDp(MotionDirection.FORWARD))
        assertEquals(SazonMotion.SLIDE_OFFSET_DP, MotionPolicy.exitOffsetDp(MotionDirection.BACKWARD))
        assertEquals(0, MotionPolicy.exitOffsetDp(MotionDirection.NONE))
    }

    @Test
    fun adelante_y_atras_son_opuestos_y_sin_movimiento_no_desplaza() {
        assertNotEquals(
            MotionPolicy.enterOffsetDp(MotionDirection.FORWARD),
            MotionPolicy.enterOffsetDp(MotionDirection.BACKWARD),
        )
        assertNotEquals(
            MotionPolicy.enterOffsetDp(MotionDirection.FORWARD),
            MotionPolicy.exitOffsetDp(MotionDirection.FORWARD),
        )
        assertEquals(0, MotionPolicy.enterOffsetDp(MotionDirection.NONE))
        assertEquals(0, MotionPolicy.exitOffsetDp(MotionDirection.NONE))
    }
}
