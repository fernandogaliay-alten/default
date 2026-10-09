package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.MeasureUnit

/**
 * Formateo de la etiqueta de cantidad de un ingrediente del Detalle: logica **pura**, sin Compose ni
 * Android.
 *
 * - `unit == null` ("al gusto") -> `"Al gusto"`, nunca una cantidad numerica.
 * - Fracciones en medios -> `"½"`, `"1½"`, `"2½"` (nunca `"0.5"`); el resto, enteros.
 * - Sufijo de unidad: `g`, `ml`, `ud`, `cda`, `cdta` (`"300 g"`, `"4½ ud"`, `"½ cda"`).
 */
object IngredientFormat {

    fun quantityLabel(quantity: Double, unit: MeasureUnit?): String {
        if (unit == null) return "Al gusto"
        return "${numberLabel(quantity)} ${suffix(unit)}"
    }

    /** Numero con medios como `½`, `1½`… y el resto como enteros, redondeando a la mitad mas cercana. */
    private fun numberLabel(value: Double): String {
        val halves = Math.round(value * 2)
        val whole = halves / 2L
        val hasHalf = halves % 2L != 0L
        return when {
            whole == 0L && hasHalf -> "½"
            hasHalf -> "$whole½"
            else -> whole.toString()
        }
    }

    private fun suffix(unit: MeasureUnit): String = when (unit) {
        MeasureUnit.GRAM -> "g"
        MeasureUnit.MILLILITER -> "ml"
        MeasureUnit.UNIT -> "ud"
        MeasureUnit.TABLESPOON -> "cda"
        MeasureUnit.TEASPOON -> "cdta"
    }
}
