package com.sazon.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sazon.app.data.image.DishImageSettingsState
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.onboarding.OnboardingPreferencesDraft
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Pestana Perfil real: el usuario **consulta y modifica** su identidad y sus preferencias.
 *
 * Es una capa Compose delgada sobre [ProfileEditState] (logica pura, testeada en JVM). Cada grupo de
 * preferencias abre una **hoja inferior** con el **mismo** editor sin estado que el onboarding
 * (`ui.preferences`): la regla "un editor, dos entradas". Al guardar se entrega el [UserProfile]
 * editado a [onSaveProfile]; como `SazonApp` observa el Perfil, el armazon se recompone y cambiar
 * `preferences` (y no solo la identidad) es lo unico que invalida el Lote.
 *
 * Tras la cabecera de identidad y antes de "Mis preferencias" se pintan la seccion **"Estadísticas"**
 * ([ActivitySummarySection], que recibe el contador persistido de recetas generadas y el numero de
 * Favoritas derivado) y despues la seccion **"Sugerencias"** ([ProfileSuggestionsSection]), que muestra
 * la antiguedad del Lote observado y ofrece un refresco manual; el async y el estado "en vuelo" los
 * aporta `MainShell`, no esta pantalla. Al **final** de la lista se anade el ajuste de app
 * **"Imágenes del plato"** ([DishImageSettingsSection]): interruptor + tope por sesion visible. Los
 * demas ajustes (Notificaciones, Idioma, "Sobre la app") y "Borrar mis datos" quedan fuera de alcance
 * (features propias); el estado de color de avatar corrupto se degrada al primer `avatarColors` en
 * [ProfileEditState.from].
 */
@Composable
fun ProfileScreen(
    profile: UserProfile,
    taxonomy: Taxonomy,
    onSaveProfile: (UserProfile) -> Unit,
    suggestionsGeneratedAt: Long?,
    isRefreshingSuggestions: Boolean,
    onRefreshSuggestions: () -> Unit,
    recipesGeneratedCount: Int,
    favoritesCount: Int,
    imageGeneration: DishImageSettingsState,
    onImageGenerationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by rememberSaveable(stateSaver = ProfileEditStateSaver) {
        mutableStateOf(ProfileEditState.from(profile, taxonomy))
    }
    var editingIdentity by rememberSaveable { mutableStateOf(false) }
    var activeSectionName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSection = activeSectionName?.let(ProfileSection::valueOf)
    // El `now` de la antiguedad se captura una vez, como `HomeGreeting` captura la hora.
    val now = remember { System.currentTimeMillis() }

    // Resincroniza el estado editable cuando el Perfil persistido cambia (p. ej. tras un guardado).
    LaunchedEffect(profile) {
        state = ProfileEditState.from(profile, taxonomy)
    }

    fun persist(updated: UserProfile) {
        if (updated != profile) onSaveProfile(updated)
    }

    // Identidad y hojas son modos mutuamente excluyentes: abrir una cierra la otra.
    fun openSection(section: ProfileSection) {
        editingIdentity = false
        activeSectionName = section.name
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SazonSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        item {
            ProfileIdentityEditor(
                state = state,
                taxonomy = taxonomy,
                isEditing = editingIdentity,
                onNameChange = { state = state.withDisplayName(it) },
                onColorChange = { state = state.withAvatarColorId(it) },
                onEdit = {
                    editingIdentity = true
                    activeSectionName = null
                },
                onCancel = {
                    val persisted = ProfileEditState.from(profile, taxonomy)
                    state = state
                        .withDisplayName(persisted.displayName)
                        .withAvatarColorId(persisted.avatarColorId)
                    editingIdentity = false
                },
                onSave = {
                    persist(state.toProfile(profile, System.currentTimeMillis()))
                    editingIdentity = false
                },
            )
        }

        item {
            ActivitySummarySection(
                recipesGeneratedCount = recipesGeneratedCount,
                favoritesCount = favoritesCount,
            )
        }

        item {
            ProfileSuggestionsSection(
                generatedAt = suggestionsGeneratedAt,
                isRefreshing = isRefreshingSuggestions,
                onRefresh = onRefreshSuggestions,
                now = now,
            )
        }

        item { SectionHeading(text = "Mis preferencias") }
        item {
            ProfileSectionRow(
                label = "Alimentación",
                summary = dietSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.DIET) },
            )
        }
        item {
            ProfileSectionRow(
                label = "Estilo",
                summary = softSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.STYLE) },
            )
        }
        item {
            ProfileSectionRow(
                label = "Cocinas favoritas",
                summary = cuisineSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.CUISINES) },
            )
        }
        item {
            ProfileSectionRow(
                label = "Ingredientes que me gustan",
                summary = preferredIngredientsSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.INGREDIENTS) },
            )
        }
        item {
            ProfileSectionRow(
                label = "Ingredientes que evito",
                summary = dislikedIngredientsSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.INGREDIENTS) },
            )
        }

        item { SectionHeading(text = "Alergias e intolerancias") }
        item {
            ProfileSectionRow(
                label = "Restricciones alimentarias",
                summary = restrictionsSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.RESTRICTIONS) },
            )
        }

        item { SectionHeading(text = "Mi cocina") }
        item {
            ProfileSectionRow(
                label = "Nivel de cocina",
                summary = difficultySummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.DIFFICULTY) },
            )
        }
        item {
            ProfileSectionRow(
                label = "Tiempo disponible",
                summary = timeBucketSummary(state.preferencesDraft, taxonomy),
                onClick = { openSection(ProfileSection.TIME_BUCKET) },
            )
        }
        item {
            ProfileSectionRow(
                label = "Comensales habituales",
                summary = servingsSummary(state.preferencesDraft),
                onClick = { openSection(ProfileSection.SERVINGS) },
            )
        }

        // Ajuste de app (gasto de cuota), al final a proposito: no es una preferencia culinaria y asi
        // no se reordena ninguna seccion aceptada del Perfil.
        item {
            DishImageSettingsSection(
                state = imageGeneration,
                onToggle = onImageGenerationChange,
            )
        }
    }

    if (activeSection != null) {
        ProfileEditorSheet(
            section = activeSection,
            state = state,
            taxonomy = taxonomy,
            onDraftChange = { updated -> state = state.withPreferencesDraft(updated) },
            onDismiss = {
                // La hoja persiste al cerrarse ("Listo" o descarte) solo si hubo cambio: si el
                // borrador quedo igual, `toProfile` reutiliza `base.preferences` y `updated == profile`.
                persist(state.toProfile(profile, System.currentTimeMillis()))
                activeSectionName = null
            },
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Column(modifier = Modifier.padding(top = SazonSpacing.sm)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun dietSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    taxonomy.dietPattern(draft.dietPatternId)?.label ?: draft.dietPatternId

private fun softSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    joinLabels(draft.softPreferenceIds, { taxonomy.softPreference(it)?.label }, "Ninguno")

private fun cuisineSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    joinLabels(draft.cuisineIds, { taxonomy.cuisine(it)?.label }, "Ninguna")

private fun preferredIngredientsSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    joinLabels(draft.preferredIngredientIds, { taxonomy.ingredient(it)?.label }, "Ninguno")

private fun dislikedIngredientsSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    joinLabels(draft.dislikedIngredientIds, { taxonomy.ingredient(it)?.label }, "Ninguno")

private fun restrictionsSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    joinLabels(draft.hardRestrictionIds, { taxonomy.restriction(it)?.label }, "Ninguna")

private fun difficultySummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    taxonomy.difficulty(draft.difficultyId)?.label ?: draft.difficultyId

private fun timeBucketSummary(draft: OnboardingPreferencesDraft, taxonomy: Taxonomy): String =
    taxonomy.timeBucket(draft.timeBucketId)?.label ?: draft.timeBucketId

private fun servingsSummary(draft: OnboardingPreferencesDraft): String =
    if (draft.defaultServings == 1) "1 comensal" else "${draft.defaultServings} comensales"

/** Resuelve labels con respaldo al `id` y un texto cuando la lista esta vacia. */
private fun joinLabels(
    ids: List<String>,
    labelOf: (String) -> String?,
    emptyText: String,
): String =
    if (ids.isEmpty()) emptyText else ids.joinToString(", ") { labelOf(it) ?: it }

/**
 * Saver de [ProfileEditState] para `rememberSaveable`, en la capa Compose: mantiene la logica pura
 * libre de Compose. Guarda los dos campos escalares de identidad y el borrador con el mismo formato de
 * listas que `OnboardingFlow` (`[conteo, ...elementos]`).
 */
private val ProfileEditStateSaver: Saver<ProfileEditState, Any> = listSaver(
    save = { state ->
        buildList {
            add(state.displayName)
            add(state.avatarColorId)
            add(state.preferencesDraft.dietPatternId)
            addAll(state.preferencesDraft.softPreferenceIds.withSizePrefix())
            addAll(state.preferencesDraft.cuisineIds.withSizePrefix())
            addAll(state.preferencesDraft.preferredIngredientIds.withSizePrefix())
            addAll(state.preferencesDraft.dislikedIngredientIds.withSizePrefix())
            addAll(state.preferencesDraft.hardRestrictionIds.withSizePrefix())
            add(state.preferencesDraft.difficultyId)
            add(state.preferencesDraft.timeBucketId)
            add(state.preferencesDraft.defaultServings.toString())
        }
    },
    restore = { saved ->
        var index = 2
        fun nextString(): String = saved[index++]
        fun nextList(): List<String> {
            val count = nextString().toInt()
            val list = saved.subList(index, index + count)
            index += count
            return list
        }
        ProfileEditState(
            displayName = saved[0],
            avatarColorId = saved[1],
            preferencesDraft = OnboardingPreferencesDraft(
                dietPatternId = nextString(),
                softPreferenceIds = nextList(),
                cuisineIds = nextList(),
                preferredIngredientIds = nextList(),
                dislikedIngredientIds = nextList(),
                hardRestrictionIds = nextList(),
                difficultyId = nextString(),
                timeBucketId = nextString(),
                defaultServings = nextString().toInt(),
            ),
        )
    },
)

private fun List<String>.withSizePrefix(): List<String> = listOf(size.toString()) + this
