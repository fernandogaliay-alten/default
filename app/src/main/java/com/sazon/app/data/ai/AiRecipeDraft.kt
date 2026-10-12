package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import kotlinx.serialization.Serializable

/**
 * Cantidad normalizada del ingrediente "al gusto" del wire (`quantity` nula o ausente).
 *
 * El par `(0.0, unit = null)` es la representacion aceptada de "Al gusto": la UI
 * ([com.sazon.app.ui.detail.IngredientFormat.quantityLabel]) pinta `"Al gusto"` siempre que
 * `unit == null` (ignora la cantidad) y [com.sazon.app.ui.detail.ServingScaler] **no escala** cuando
 * `unit == null`. Una sola fuente para ese valor.
 */
internal const val AL_GUSTO_QUANTITY: Double = 0.0

/**
 * DTO del **wire** de un ingrediente del modelo: a diferencia de [RecipeIngredient] de dominio, la
 * `quantity` es **anulable**.
 *
 * El modelo emite legitimamente ingredientes "al gusto" con `"quantity": null` (o con el campo
 * ausente): el dominio declara `quantity` como `Double` no anulable y, si el adaptador decodificara
 * directamente con el tipo de dominio, un solo ingrediente "al gusto" tumbaria la **receta entera**
 * como `SCHEMA`. Este DTO mantiene el wire explicito y deja la normalizacion a
 * [toRecipeIngredient].
 *
 * El resto de campos sigue la disciplina tolerante del adaptador: defaults para los opcionales
 * (`unit`, `isPantry`, `ingredientId`); solo `name` es obligatorio.
 */
@Serializable
internal data class AiIngredientDraft(
    val name: String,
    val quantity: Double? = null,
    val unit: MeasureUnit? = null,
    val isPantry: Boolean = false,
    val ingredientId: String? = null,
)

/**
 * Normaliza el ingrediente del wire a [RecipeIngredient] de dominio, **sin debilitar el esquema**:
 *
 * - `quantity == null` (nula **o** ausente) -> "Al gusto": `quantity = [AL_GUSTO_QUANTITY]` y
 *   `unit = null`. La `unit` del wire se **descarta** aunque venga: sin cantidad la unidad no es
 *   interpretable y forzarla garantiza el render correcto (evita un `"0 g"` enganoso).
 * - `quantity != null` -> mapeo **1:1**: la cantidad y la unidad del wire se conservan intactas.
 */
internal fun AiIngredientDraft.toRecipeIngredient(): RecipeIngredient =
    if (quantity == null) {
        RecipeIngredient(
            name = name,
            quantity = AL_GUSTO_QUANTITY,
            unit = null,
            isPantry = isPantry,
            ingredientId = ingredientId,
        )
    } else {
        RecipeIngredient(
            name = name,
            quantity = quantity,
            unit = unit,
            isPantry = isPantry,
            ingredientId = ingredientId,
        )
    }

/**
 * DTO del **wire** del proveedor: la forma que el modelo devuelve de verdad, calcada del contrato de
 * `docs/domain/taxonomy.md` §4 **menos los campos que posee la app** (`id`, `source` y `matchScore`).
 *
 * Es la capa anti-corrupcion del adaptador: el wire del modelo no lleva `id` ni `source` (los asigna
 * la app) y `matchScore` es derivado y no persistido. Al no declarar esos campos y decodificar con
 * `ignoreUnknownKeys = true`, si el modelo los enviara igualmente se **ignoran**: la app siempre
 * posee esos tres campos.
 *
 * Los ingredientes usan [AiIngredientDraft] (wire con `quantity` anulable) y se **normalizan** en
 * [toRecipe] (`toRecipeIngredient`): el dominio exige `quantity: Double` no anulable y el wire
 * legitimamente trae "al gusto". [RecipeStep] y [Nutrition] se reutilizan de `domain.recipe`: son
 * esquema del contrato sin campos de app, por lo que no hace falta duplicarlos.
 */
@Serializable
internal data class AiRecipeDraft(
    val title: String,
    val description: String,
    val categoryId: String,
    val mealTypeId: String,
    val cuisineId: String,
    val totalMinutes: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficultyId: String,
    val servings: Int,
    val ingredients: List<AiIngredientDraft>,
    val steps: List<RecipeStep>,
    val tips: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    val declaredAllergens: List<String> = emptyList(),
) {

    /**
     * Mapea el wire a la Receta de dominio **completando los campos que posee la app**:
     * `id` (recibido, ver [AiRecipeIds]), `source = AI` y `matchScore = null` (derivado). Los
     * ingredientes se normalizan con [toRecipeIngredient] (una `quantity` nula o ausente pasa a
     * "al gusto": `0.0` + `unit = null`).
     */
    fun toRecipe(id: String): Recipe = Recipe(
        id = id,
        source = RecipeSource.AI,
        title = title,
        description = description,
        categoryId = categoryId,
        mealTypeId = mealTypeId,
        cuisineId = cuisineId,
        totalMinutes = totalMinutes,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        difficultyId = difficultyId,
        servings = servings,
        ingredients = ingredients.map { it.toRecipeIngredient() },
        steps = steps,
        tips = tips,
        nutrition = nutrition,
        declaredAllergens = declaredAllergens,
        matchScore = null,
    )
}
