package com.sazon.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas puras de la consulta: [AiSearchQuery.canSubmit] y [AiSearchQuery.normalize].
 */
class AiSearchQueryTest {

    @Test
    fun sin_conexion_no_se_puede_enviar_aunque_haya_texto() {
        assertFalse(AiSearchQuery.canSubmit("pollo y arroz", isOnline = false))
    }

    @Test
    fun en_blanco_no_se_puede_enviar_aunque_haya_conexion() {
        assertFalse(AiSearchQuery.canSubmit("", isOnline = true))
        assertFalse(AiSearchQuery.canSubmit("   ", isOnline = true))
    }

    @Test
    fun con_conexion_y_texto_se_puede_enviar() {
        assertTrue(AiSearchQuery.canSubmit("pollo", isOnline = true))
    }

    @Test
    fun normalize_recorta_los_extremos() {
        assertEquals("pollo y arroz", AiSearchQuery.normalize("   pollo y arroz   "))
        assertEquals("", AiSearchQuery.normalize("   "))
    }
}
