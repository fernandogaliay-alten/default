package com.sazon.app.domain.generator

import com.sazon.app.domain.profile.PreferenceProfile

/**
 * Ajustes de una generacion del paso de ingredientes y ajustes del generador.
 *
 * Es una costura deliberadamente minima: la UI que la rellena (`generator-settings-step`) es otra
 * feature y podra extenderla. Se empaqueta el [profile] para respetar la firma documentada
 * `generateFromIngredients(ingredients, constraints)` de `docs/domain-model.md`.
 *
 * Los `*Id` referencian la Taxonomia y se guardan tal cual, sin validarlos aqui.
 */
data class GenerationConstraints(
    val profile: PreferenceProfile,
    val servings: Int,
    val mealTypeId: String? = null,
    val timeBucketId: String? = null,
    val difficultyId: String? = null,
    val notes: String? = null,
)
