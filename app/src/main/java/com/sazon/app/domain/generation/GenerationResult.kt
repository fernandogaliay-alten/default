package com.sazon.app.domain.generation

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.validation.DiscardedRecipe

/**
 * Resultado **puro** de una generacion del corte principal del Generador.
 *
 * [accepted] son las recetas que pasaron el [com.sazon.app.domain.validation.RecipeValidator];
 * [discarded] lleva cada descarte con su motivo; [aiUnavailable] es verdadero cuando el generador fallo
 * (sin clave/red/error del proveedor) y se degrado a modo sin recetas, sin propagar la excepcion.
 *
 * No importa Compose ni Android: es el contrato entre la orquestacion pura y la capa Compose.
 */
data class GenerationResult(
    val accepted: List<Recipe>,
    val discarded: List<DiscardedRecipe>,
    val aiUnavailable: Boolean = false,
)
