package com.sazon.app.ui.onboarding

import com.sazon.app.domain.taxonomy.Servings
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Borrador en memoria de las preferencias de los pasos 2-7 del onboarding: patron de dieta
 * (single-select), preferencias de estilo (multi-select opcional con "Ninguno"), cocinas favoritas
 * (multi-select), ingredientes que me gustan/evito (multi-select con exclusion mutua), restricciones
 * duras (multi-select con "Ninguna") y cocina (dificultad y franja single-select + comensales).
 *
 * Es **logica pura**: no importa Compose ni Android, de modo que la seleccion y la alternancia se
 * testean en JVM. El flujo Compose lo guarda con `rememberSaveable` (Saver en la capa Compose); la
 * construccion y persistencia del Perfil viven en [OnboardingProfileBuilder] y `SazonApp`.
 *
 * No valida ids contra la Taxonomia: el editor solo ofrece ids validos y la validacion del Perfil no
 * pertenece aqui.
 */
data class OnboardingPreferencesDraft(
    val dietPatternId: String,
    val softPreferenceIds: List<String>,
    val cuisineIds: List<String>,
    val preferredIngredientIds: List<String>,
    val dislikedIngredientIds: List<String>,
    val hardRestrictionIds: List<String>,
    val difficultyId: String,
    val timeBucketId: String,
    val defaultServings: Int,
) {
    /** Selecciona un unico patron de dieta: reemplaza al anterior. */
    fun selectDietPattern(id: String): OnboardingPreferencesDraft =
        copy(dietPatternId = id)

    /** Alterna una preferencia blanda con semantica de conjunto (sin duplicados). */
    fun toggleSoftPreference(id: String): OnboardingPreferencesDraft =
        copy(softPreferenceIds = softPreferenceIds.toggle(id))

    /** Alterna una cocina favorita con semantica de conjunto (sin duplicados). */
    fun toggleCuisine(id: String): OnboardingPreferencesDraft =
        copy(cuisineIds = cuisineIds.toggle(id))

    /** Accion "Ninguno": deja las preferencias de estilo vacias. */
    fun clearSoftPreferences(): OnboardingPreferencesDraft =
        copy(softPreferenceIds = emptyList())

    /** "Ninguno" esta activo cuando no hay ninguna preferencia blanda marcada. */
    val isStyleNone: Boolean
        get() = softPreferenceIds.isEmpty()

    /**
     * Alterna un ingrediente en "Me gusta" con semantica de conjunto. Si lo **anade**, lo quita de
     * "Evito" (exclusion mutua: un ingrediente nunca esta en los dos grupos a la vez); si lo **quita**,
     * no toca "Evito".
     */
    fun togglePreferredIngredient(id: String): OnboardingPreferencesDraft =
        copy(
            preferredIngredientIds = preferredIngredientIds.toggle(id),
            dislikedIngredientIds = if (preferredIngredientIds.contains(id)) {
                dislikedIngredientIds
            } else {
                dislikedIngredientIds.filterNot { it == id }
            },
        )

    /** Simetrico de [togglePreferredIngredient] para el grupo "Evito". */
    fun toggleDislikedIngredient(id: String): OnboardingPreferencesDraft =
        copy(
            dislikedIngredientIds = dislikedIngredientIds.toggle(id),
            preferredIngredientIds = if (dislikedIngredientIds.contains(id)) {
                preferredIngredientIds
            } else {
                preferredIngredientIds.filterNot { it == id }
            },
        )

    /** Alterna una restriccion dura con semantica de conjunto (sin duplicados). */
    fun toggleHardRestriction(id: String): OnboardingPreferencesDraft =
        copy(hardRestrictionIds = hardRestrictionIds.toggle(id))

    /** Accion "Ninguna": deja las restricciones duras vacias. */
    fun clearHardRestrictions(): OnboardingPreferencesDraft =
        copy(hardRestrictionIds = emptyList())

    /** "Ninguna" esta activo cuando no hay ninguna restriccion dura marcada. */
    val hasNoRestrictions: Boolean
        get() = hardRestrictionIds.isEmpty()

    /** Selecciona un unico nivel de cocina: reemplaza al anterior. */
    fun selectDifficulty(id: String): OnboardingPreferencesDraft =
        copy(difficultyId = id)

    /** Selecciona una unica franja de tiempo: reemplaza a la anterior. */
    fun selectTimeBucket(id: String): OnboardingPreferencesDraft =
        copy(timeBucketId = id)

    /** Ajusta los comensales recortando al rango de la Taxonomia (`limits`). */
    fun setServings(servings: Int, limits: Servings): OnboardingPreferencesDraft =
        copy(defaultServings = servings.coerceIn(limits.min, limits.max))

    companion object {
        /**
         * Borrador inicial valido y derivado de la Taxonomia: primer `dietPatterns` por defecto
         * (`omnivore`), listas vacias, primer `difficulties` (`easy`), primer `timeBuckets`
         * (`lte_15`) y `servings.default` (`2`); la integridad de la Taxonomia garantiza que hay al
         * menos una opcion de cada lista.
         */
        fun initial(taxonomy: Taxonomy): OnboardingPreferencesDraft =
            OnboardingPreferencesDraft(
                dietPatternId = taxonomy.dietPatterns.first().id,
                softPreferenceIds = emptyList(),
                cuisineIds = emptyList(),
                preferredIngredientIds = emptyList(),
                dislikedIngredientIds = emptyList(),
                hardRestrictionIds = emptyList(),
                difficultyId = taxonomy.difficulties.first().id,
                timeBucketId = taxonomy.timeBuckets.first().id,
                defaultServings = taxonomy.servings.default,
            )
    }
}

private fun List<String>.toggle(id: String): List<String> =
    if (contains(id)) filterNot { it == id } else this + id
