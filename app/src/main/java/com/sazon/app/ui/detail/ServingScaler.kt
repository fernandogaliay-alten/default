package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient

/**
 * Escalado de raciones del Detalle de receta: logica **pura** y determinista, sin Compose ni Android.
 *
 * El escalado **no es lineal**: cada unidad se redondea a su propio paso para que las cantidades
 * mostradas sean cocinables (gramos/mililitros a multiplos de 5; `ud`/`cda`/`cdta` a medios) y la
 * unidad `null` ("al gusto", p. ej. sal o especias) **no se escala** (ver `docs/domain-model.md`, edge
 * case "escalado de raciones").
 *
 * Reglas:
 * - `factor = targetServings / recipe.servings.coerceAtLeast(1)` (nunca divide por cero).
 * - `factor == 1.0` (raciones originales): devuelve las cantidades **intactas**, sin redondear.
 * - `unit == null`: devuelve la cantidad original, sea cual sea el factor.
 * - `quantity == 0.0` se conserva `0.0`; si `quantity > 0` el resultado nunca baja del paso de su
 *   unidad (minimo `5` en `g`/`ml`, `0.5` en el resto).
 * - Redondeo **half-up** (`Math.round`), sin reloj ni `Random`; no persiste nada.
 *
 * Los textos ("½", "Al gusto") los resuelve [IngredientFormat]; el escalador solo devuelve numeros.
 */
object ServingScaler {

    /** Paso de redondeo de `g` y `ml`. */
    const val GRAM_STEP = 5.0

    /** Paso de redondeo de `ud`, `cda` y `cdta`. */
    const val UNIT_STEP = 0.5

    /**
     * Devuelve los ingredientes de [recipe] con sus cantidades escaladas a [targetServings], con el
     * redondeo por unidad descrito arriba y conservando el resto de campos de cada ingrediente.
     */
    fun scaledIngredients(recipe: Recipe, targetServings: Int): List<RecipeIngredient> {
        val factor = targetServings.toDouble() / recipe.servings.coerceAtLeast(1)
        return recipe.ingredients.map { ingredient ->
            ingredient.copy(
                quantity = scaleQuantity(ingredient.quantity, ingredient.unit, factor),
            )
        }
    }

    /**
     * Escala una cantidad concreta por [factor] aplicando el paso de su [unit]. Devuelve la cantidad
     * original si [unit] es `null` o si [factor] es exactamente `1.0` (raciones originales intactas).
     */
    fun scaleQuantity(quantity: Double, unit: MeasureUnit?, factor: Double): Double {
        if (unit == null) return quantity
        if (factor == 1.0) return quantity
        val step = stepFor(unit)
        val rounded = roundToStep(quantity * factor, step)
        return if (quantity > 0.0 && rounded < step) step else rounded
    }

    /** Redondeo half-up de [value] al multiplo de [step] mas cercano. */
    private fun roundToStep(value: Double, step: Double): Double =
        (Math.round(value / step) * step)

    private fun stepFor(unit: MeasureUnit): Double = when (unit) {
        MeasureUnit.GRAM, MeasureUnit.MILLILITER -> GRAM_STEP
        MeasureUnit.UNIT, MeasureUnit.TABLESPOON, MeasureUnit.TEASPOON -> UNIT_STEP
    }
}
