package com.sazon.app.ui.profile

import com.sazon.app.data.image.DishImageSettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 7 de `docs/specs/dish-image-settings-toggle.md`: el microcopy del tope por sesion es
 * **puro y determinista** (sin Compose ni Android): la misma entrada produce siempre la misma cadena,
 * con singular/plural correcto, tope alcanzado y apagado.
 */
class DishImageSettingsCopyTest {

    private fun state(enabled: Boolean, used: Int, max: Int = 12) =
        DishImageSettingsState(enabled = enabled, used = used, max = max)

    @Test
    fun con_el_interruptor_activado_y_el_tope_intacto_indica_12_de_12() {
        assertEquals(
            "Quedan 12 de 12 imágenes en esta sesión.",
            DishImageSettingsCopy.summary(state(enabled = true, used = 0)),
        )
    }

    @Test
    fun con_una_sola_imagen_restante_usa_el_singular() {
        assertEquals(
            "Queda 1 de 12 imágenes en esta sesión.",
            DishImageSettingsCopy.summary(state(enabled = true, used = 11)),
        )
    }

    @Test
    fun con_dos_o_mas_restantes_usa_el_plural() {
        assertEquals(
            "Quedan 5 de 12 imágenes en esta sesión.",
            DishImageSettingsCopy.summary(state(enabled = true, used = 7)),
        )
    }

    @Test
    fun con_el_tope_agotado_anuncia_alcanzado_y_reanudado() {
        assertEquals(
            "Tope de esta sesión alcanzado (12 de 12). Se reanudará al reiniciar la app.",
            DishImageSettingsCopy.summary(state(enabled = true, used = 12)),
        )
    }

    @Test
    fun con_el_interruptor_apagado_indica_que_se_usa_solo_la_foto_local() {
        assertEquals(
            "Desactivado: se usa solo la foto local.",
            DishImageSettingsCopy.summary(state(enabled = false, used = 0)),
        )
    }

    @Test
    fun apagado_manda_sobre_el_tope_agotado() {
        val apagadoConTopeAlcanzado = DishImageSettingsCopy.summary(state(enabled = false, used = 12))

        assertEquals(DishImageSettingsCopy.DISABLED_SUMMARY, apagadoConTopeAlcanzado)
        assertNotEquals(
            "Apagado no debe anunciar el tope",
            DishImageSettingsCopy.summary(state(enabled = true, used = 12)),
            apagadoConTopeAlcanzado,
        )
    }

    @Test
    fun el_resumen_es_determinista() {
        val entry = state(enabled = true, used = 3)

        assertEquals(
            DishImageSettingsCopy.summary(entry),
            DishImageSettingsCopy.summary(entry),
        )
    }

    @Test
    fun el_microcopy_de_la_seccion_es_estable() {
        assertEquals("Imágenes del plato", DishImageSettingsCopy.SECTION_TITLE)
        assertEquals("Generar imágenes del plato", DishImageSettingsCopy.TOGGLE_LABEL)
        assertEquals(
            "Las imágenes nuevas se generan con IA y consumen cuota; las que ya existen se reutilizan.",
            DishImageSettingsCopy.HELPER_TEXT,
        )
        assertTrue(
            "El resumen del tope cita el maximo inyectado, no un literal oculto",
            DishImageSettingsCopy.summary(state(enabled = true, used = 0, max = 3))
                .contains("de 3 imágenes"),
        )
    }
}
