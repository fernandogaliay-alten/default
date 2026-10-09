package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Formateo de cantidades (`docs/specs/recipe-detail-screen.md`, `IngredientFormat`): mitades como `½`,
 * enteros, sufijos de unidad y "Al gusto" para la unidad `null`. Logica pura sin Compose ni Android.
 */
class IngredientFormatTest {

    @Test
    fun la_unidad_nula_se_muestra_como_al_gusto() {
        assertEquals("Al gusto", IngredientFormat.quantityLabel(1.0, unit = null))
    }

    @Test
    fun los_gramos_y_mililitros_son_enteros_con_sufijo() {
        assertEquals("300 g", IngredientFormat.quantityLabel(300.0, MeasureUnit.GRAM))
        assertEquals("115 ml", IngredientFormat.quantityLabel(115.0, MeasureUnit.MILLILITER))
    }

    @Test
    fun las_unidades_con_media_muestran_el_simbolo() {
        assertEquals("4½ ud", IngredientFormat.quantityLabel(4.5, MeasureUnit.UNIT))
        assertEquals("½ ud", IngredientFormat.quantityLabel(0.5, MeasureUnit.UNIT))
        assertEquals("½ cda", IngredientFormat.quantityLabel(0.5, MeasureUnit.TABLESPOON))
        assertEquals("1½ cdta", IngredientFormat.quantityLabel(1.5, MeasureUnit.TEASPOON))
        assertEquals("2½ cda", IngredientFormat.quantityLabel(2.5, MeasureUnit.TABLESPOON))
    }

    @Test
    fun nunca_aparece_el_formato_decimal_punto_cinco() {
        val mitades = listOf(0.5, 1.5, 2.5).map { IngredientFormat.quantityLabel(it, MeasureUnit.UNIT) }

        mitades.forEach { label ->
            assertFalse("No debe contener '.5': $label", label.contains(".5"))
        }
        assertFalse(IngredientFormat.quantityLabel(0.5, MeasureUnit.UNIT).contains("0.5"))
    }

    @Test
    fun los_enteros_no_llevan_decimales_ni_simbolo_de_mitad() {
        assertEquals("3 ud", IngredientFormat.quantityLabel(3.0, MeasureUnit.UNIT))
        assertEquals("1 ud", IngredientFormat.quantityLabel(1.0, MeasureUnit.UNIT))
    }
}
