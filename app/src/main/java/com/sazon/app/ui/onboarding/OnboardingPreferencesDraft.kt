package com.sazon.app.ui.onboarding

import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Borrador en memoria de las preferencias de los pasos 2-4 del onboarding: patron de dieta
 * (single-select), preferencias de estilo (multi-select opcional con "Ninguno") y cocinas favoritas
 * (multi-select).
 *
 * Es **logica pura**: no importa Compose ni Android, de modo que la seleccion y la alternancia se
 * testean en JVM. El flujo Compose lo guarda con `rememberSaveable` (Saver en la capa Compose); no se
 * persiste un Perfil.
 *
 * No valida ids contra la Taxonomia: el editor solo ofrece ids validos y la validacion del Perfil no
 * pertenece aqui.
 */
data class OnboardingPreferencesDraft(
    val dietPatternId: String,
    val softPreferenceIds: List<String>,
    val cuisineIds: List<String>,
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

    companion object {
        /**
         * Borrador inicial valido y derivado de la Taxonomia: primer `dietPatterns` por defecto
         * (`omnivore`, la integridad de la Taxonomia garantiza al menos uno) y listas vacias.
         */
        fun initial(taxonomy: Taxonomy): OnboardingPreferencesDraft =
            OnboardingPreferencesDraft(
                dietPatternId = taxonomy.dietPatterns.first().id,
                softPreferenceIds = emptyList(),
                cuisineIds = emptyList(),
            )
    }
}

private fun List<String>.toggle(id: String): List<String> =
    if (contains(id)) filterNot { it == id } else this + id
