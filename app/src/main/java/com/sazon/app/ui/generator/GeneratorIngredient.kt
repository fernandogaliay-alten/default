package com.sazon.app.ui.generator

/**
 * Ingrediente **anadido** en el paso 1 del Generador ("¿Qué tienes?").
 *
 * Si `ingredientId != null` es un ingrediente de la **Taxonomia** (con su `label`); si es `null` es un
 * ingrediente **libre** escrito por el usuario (solo `name`). Replica el modelo de
 * `PromptIngredient`/`RecipeIngredient` y es la costura hacia ellos que consumira
 * `generation-flow-with-validation`: aqui solo se captura, el estado no inventa ids.
 *
 * Logica pura: sin imports de Compose ni Android, testeable en JVM.
 */
data class GeneratorIngredient(
    val name: String,
    val ingredientId: String? = null,
)
