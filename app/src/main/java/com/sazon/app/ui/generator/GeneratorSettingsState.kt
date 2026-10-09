package com.sazon.app.ui.generator

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.taxonomy.Servings

/**
 * Estado **puro** del paso 2 del Generador ("Ajusta tu receta"): los ajustes por generación que el
 * usuario edita (comensales, tipo de comida, franja, dificultad, alergias a evitar y notas).
 *
 * No importa Compose ni Android (solo `domain.profile` y `domain.taxonomy`), de modo que se testea en
 * JVM. Todas las operaciones son inmutables (`copy`): cada acción devuelve un estado nuevo.
 *
 * Reglas fijadas por la spec `docs/specs/generator-settings-step.md`:
 * - [initial] precarga comensales, franja, dificultad y alergias del [PreferenceProfile]; el tipo de
 *   comida arranca en `null` (el perfil no lo tiene) y las notas en blanco.
 * - [setServings] recorta con `coerceIn` a los límites de la Taxonomía; el stepper muestra el valor.
 * - [selectTimeBucket] y [selectDifficulty] son **single-select obligatorio** (reemplazan).
 * - [selectMealType] es **single-select opcional**: pulsar el ya seleccionado lo deja en `null`.
 * - [toggleHardRestriction] tiene semántica de **conjunto** (sin duplicados, alterna); [clearHardRestrictions]
 *   deja la lista vacía y [hasNoRestrictions] la refleja.
 * - [typeNotes] conserva el texto tal cual (incluidos saltos de línea).
 *
 * Esta feature solo **captura** los ajustes: no los convierte a `GenerationConstraints` ni llama al
 * generador (eso es de `generation-flow-with-validation`).
 */
data class GeneratorSettingsState(
    val servings: Int,
    val mealTypeId: String? = null,
    val timeBucketId: String,
    val difficultyId: String,
    val hardRestrictionIds: List<String> = emptyList(),
    val notes: String = "",
) {
    /** Verdadero cuando no hay ninguna alergia marcada ("Ninguna declarada"). */
    val hasNoRestrictions: Boolean get() = hardRestrictionIds.isEmpty()

    /** Recorta los comensales a un valor válido de la Taxonomía y devuelve el estado nuevo. */
    fun setServings(value: Int, limits: Servings): GeneratorSettingsState =
        copy(servings = value.coerceIn(limits.min, limits.max))

    /**
     * Selecciona (o deselecciona) el tipo de comida: si `id` ya está seleccionado lo deja en `null`
     * (sin preferencia); si no, lo reemplaza.
     */
    fun selectMealType(id: String): GeneratorSettingsState =
        copy(mealTypeId = if (mealTypeId == id) null else id)

    /** Selecciona la franja de tiempo disponible (single-select que reemplaza). */
    fun selectTimeBucket(id: String): GeneratorSettingsState = copy(timeBucketId = id)

    /** Selecciona la dificultad (single-select que reemplaza). */
    fun selectDifficulty(id: String): GeneratorSettingsState = copy(difficultyId = id)

    /** Añade o quita una alergia (semántica de conjunto, sin duplicados). */
    fun toggleHardRestriction(id: String): GeneratorSettingsState =
        if (id in hardRestrictionIds) {
            copy(hardRestrictionIds = hardRestrictionIds - id)
        } else {
            copy(hardRestrictionIds = hardRestrictionIds + id)
        }

    /** Deja la lista de alergias vacía (equivale a "Ninguna"). */
    fun clearHardRestrictions(): GeneratorSettingsState = copy(hardRestrictionIds = emptyList())

    /** Actualiza las notas libres conservando el texto tal cual. */
    fun typeNotes(text: String): GeneratorSettingsState = copy(notes = text)

    companion object {
        /**
         * Estado inicial del paso 2 a partir del Perfil: copia `defaultServings` (recortado a los
         * límites), `timeBucketId`, `difficultyId` y `hardRestrictionIds`; `mealTypeId = null` y
         * `notes = ""`.
         */
        fun initial(preferences: PreferenceProfile, limits: Servings): GeneratorSettingsState =
            GeneratorSettingsState(
                servings = preferences.defaultServings.coerceIn(limits.min, limits.max),
                mealTypeId = null,
                timeBucketId = preferences.timeBucketId,
                difficultyId = preferences.difficultyId,
                hardRestrictionIds = preferences.hardRestrictionIds,
                notes = "",
            )
    }
}
