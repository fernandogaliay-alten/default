package com.sazon.app.ui.cooking

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Formatos puros del modo cocina (`docs/specs/cooking-mode.md`): etiqueta de progreso, fraccion de la
 * barra, duracion del paso y cuenta atras `mm:ss`, con los casos limite (`total = 0`, tiempo negativo).
 * Logica pura sin Compose ni Android.
 */
class CookingModeFormatTest {

    @Test
    fun la_etiqueta_de_progreso_numera_el_paso_desde_uno() {
        assertEquals("Paso 2 de 5", CookingModeFormat.progressLabel(stepIndex = 1, totalSteps = 5))
        assertEquals("Paso 1 de 1", CookingModeFormat.progressLabel(stepIndex = 0, totalSteps = 1))
        assertEquals("Paso 5 de 5", CookingModeFormat.progressLabel(stepIndex = 4, totalSteps = 5))
    }

    @Test
    fun la_fraccion_de_progreso_va_de_cero_a_uno() {
        assertEquals(0.4f, CookingModeFormat.progressFraction(stepIndex = 1, totalSteps = 5), 0.0001f)
        assertEquals(1f, CookingModeFormat.progressFraction(stepIndex = 0, totalSteps = 1), 0.0001f)
        assertEquals(1f, CookingModeFormat.progressFraction(stepIndex = 4, totalSteps = 5), 0.0001f)
    }

    @Test
    fun la_fraccion_de_progreso_acota_los_extremos() {
        assertEquals(0f, CookingModeFormat.progressFraction(stepIndex = -1, totalSteps = 5), 0.0001f)
        assertEquals(1f, CookingModeFormat.progressFraction(stepIndex = 10, totalSteps = 5), 0.0001f)
    }

    @Test
    fun sin_pasos_la_fraccion_es_cero_y_no_divide_por_cero() {
        assertEquals(0f, CookingModeFormat.progressFraction(stepIndex = 0, totalSteps = 0), 0.0001f)
        assertEquals(0f, CookingModeFormat.progressFraction(stepIndex = 3, totalSteps = -2), 0.0001f)
    }

    @Test
    fun la_duracion_reproduce_la_pildora_del_detalle() {
        assertEquals("10 min", CookingModeFormat.durationLabel(10))
        assertEquals("1 min", CookingModeFormat.durationLabel(1))
    }

    @Test
    fun la_cuenta_atras_es_mm_ss_con_dos_digitos() {
        assertEquals("10:00", CookingModeFormat.countdown(600_000L))
        assertEquals("09:42", CookingModeFormat.countdown(582_000L))
        assertEquals("00:01", CookingModeFormat.countdown(1_000L))
        assertEquals("00:00", CookingModeFormat.countdown(0L))
    }

    @Test
    fun la_cuenta_atras_trunca_a_segundos_y_no_baja_de_cero() {
        assertEquals("09:59", CookingModeFormat.countdown(599_999L))
        assertEquals("00:00", CookingModeFormat.countdown(999L))
        assertEquals("00:00", CookingModeFormat.countdown(-5_000L))
    }

    @Test
    fun el_aviso_de_exito_es_el_de_la_captura() {
        assertEquals("¡Listo!", CookingModeFormat.FINISHED_NOTICE)
    }
}
