package com.sazon.app.ui.cooking

import com.sazon.app.domain.recipe.RecipeStep
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Etiqueta **pura** del paso (feature `cooking-timer-notification-step-label`, escenarios 2 y 5): el
 * numero reutiliza la microcopy ya aceptada del encabezado ([CookingModeFormat.progressLabel]) y el
 * titulo es el del paso; sin paso o sin pasos devuelve `null`, y entonces la notificacion persistente
 * cae a su titulo generico.
 *
 * La etiqueta es la **frontera de privacidad** del temporizador del sistema: solo lleva esas dos cadenas
 * (ni descripcion, ni ingredientes, ni notas, ni clave de IA). Logica pura sin Compose ni Android y sin
 * leer el reloj, como el resto del modelo (`CookingTimerStateTest`, `CookingAlarmPolicyTest`).
 */
class CookingStepLabelTest {

    private val paso = RecipeStep(
        order = 2,
        title = "Dorar el pollo",
        description = "Sella la piel a fuego fuerte y dora por los dos lados.",
        minutes = 8,
    )

    @Test
    fun la_etiqueta_lleva_el_numero_del_paso_y_su_titulo() {
        val etiqueta = checkNotNull(CookingStepLabel.of(stepIndex = 1, totalSteps = 5, step = paso))

        assertEquals("Paso 2 de 5", etiqueta.progress)
        assertEquals("Dorar el pollo", etiqueta.title)
        assertEquals(CookingStepLabel("Paso 2 de 5", "Dorar el pollo"), etiqueta)
    }

    @Test
    fun el_numero_reutiliza_la_microcopy_del_encabezado_del_modo_cocina() {
        val etiqueta = checkNotNull(CookingStepLabel.of(1, 5, paso))

        assertEquals(
            "Misma cadena que el encabezado: sin microcopy paralela",
            CookingModeFormat.progressLabel(1, 5),
            etiqueta.progress,
        )
    }

    @Test
    fun el_primer_y_el_ultimo_paso_se_numeran_con_la_misma_regla() {
        val primero = checkNotNull(
            CookingStepLabel.of(0, 3, paso.copy(order = 1, title = "Picar la cebolla")),
        )
        val ultimo = checkNotNull(
            CookingStepLabel.of(2, 3, paso.copy(order = 3, title = "Servir con perejil")),
        )

        assertEquals("Paso 1 de 3", primero.progress)
        assertEquals("Picar la cebolla", primero.title)
        assertEquals("Paso 3 de 3", ultimo.progress)
        assertEquals("Servir con perejil", ultimo.title)
    }

    @Test
    fun sin_paso_no_hay_etiqueta() {
        assertNull(
            "Sin paso no se inventa etiqueta: la notificacion cae al titulo generico",
            CookingStepLabel.of(stepIndex = 1, totalSteps = 5, step = null),
        )
    }

    @Test
    fun sin_pasos_no_hay_etiqueta() {
        assertNull(CookingStepLabel.of(stepIndex = 0, totalSteps = 0, step = paso))
        assertNull(CookingStepLabel.of(stepIndex = 0, totalSteps = -1, step = paso))
    }

    @Test
    fun la_etiqueta_no_arrastra_la_descripcion_ni_los_minutos_del_paso() {
        val etiqueta = checkNotNull(CookingStepLabel.of(1, 5, paso))
        val contenido = etiqueta.toString()

        assertFalse("Sin la descripcion del paso", contenido.contains(paso.description))
        assertFalse("Sin la duracion del paso", contenido.contains("8 min"))
        assertNotEquals("El titulo no es la descripcion", etiqueta.title, paso.description)
    }

    @Test
    fun la_etiqueta_es_determinista_y_no_muta_nada() {
        val primera = CookingStepLabel.of(1, 5, paso)
        val segunda = CookingStepLabel.of(1, 5, paso)

        assertEquals(primera, segunda)
        assertEquals(
            "El paso de la receta no se toca",
            RecipeStep(2, "Dorar el pollo", "Sella la piel a fuego fuerte y dora por los dos lados.", 8),
            paso,
        )
    }

    @Test
    fun la_etiqueta_solo_tiene_el_numero_y_el_titulo() {
        // Frontera de privacidad **estructural**: el tipo no tiene ningun campo mas que esos dos.
        val campos = CookingStepLabel::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(setOf("progress", "title"), campos)
    }
}
