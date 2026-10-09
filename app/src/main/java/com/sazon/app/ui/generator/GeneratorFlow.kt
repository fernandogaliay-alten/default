package com.sazon.app.ui.generator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Huesped del asistente del Generador dentro de la pagina empujada [PushedScreen.GENERATOR]
 * (`com.sazon.app.ui.navigation`).
 *
 * Mantiene el **paso** (`rememberSaveable`) y los borradores de ingredientes ([GeneratorIngredientsState]
 * con su `Saver`) y de ajustes ([GeneratorSettingsState], precargado del Perfil con su `Saver`, en la capa
 * Compose) para que se conserven al ir y volver entre pasos. Pinta una barra superior con "Atras" y el
 * titulo del paso y el primario anclado (`step.primaryLabel`), deshabilitado en [GeneratorStep.INGREDIENTS]
 * mientras `!ingredients.canContinue`.
 *
 * "Atras" es `step.previous() ?: onClose()` (desde el paso 1 cierra el Generador, desde el 2 vuelve al 1
 * conservando los ingredientes). El primario es `step.next()`: en [GeneratorStep.SETTINGS] no tiene
 * efecto (la generacion es de `generation-flow-with-validation`).
 *
 * El paso 1 ([GeneratorIngredientsScreen]) y el paso 2 ([GeneratorSettingsScreen]) son de esta feature;
 * `onEditProfile` navega a la pestana Perfil (el Perfil real es de `profile-screen`).
 */
@Composable
fun GeneratorFlow(
    taxonomy: Taxonomy,
    preferences: PreferenceProfile,
    onClose: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable(stateSaver = GeneratorStepSaver) {
        mutableStateOf(GeneratorStep.INGREDIENTS)
    }
    var ingredients by rememberSaveable(stateSaver = GeneratorIngredientsStateSaver) {
        mutableStateOf(GeneratorIngredientsState())
    }
    var settings by rememberSaveable(stateSaver = GeneratorSettingsStateSaver) {
        mutableStateOf(GeneratorSettingsState.initial(preferences, taxonomy.servings))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GeneratorHeader(
                title = step.title,
                onBack = {
                    val previous = step.previous()
                    if (previous != null) step = previous else onClose()
                },
            )
        },
        bottomBar = {
            GeneratorPrimaryBar(
                label = step.primaryLabel,
                enabled = step != GeneratorStep.INGREDIENTS || ingredients.canContinue,
                onClick = { step.next()?.let { step = it } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SazonSpacing.lg),
        ) {
            when (step) {
                GeneratorStep.INGREDIENTS -> GeneratorIngredientsScreen(
                    state = ingredients,
                    taxonomy = taxonomy,
                    onQueryChange = { ingredients = ingredients.typeQuery(it) },
                    onAddFree = { ingredients = ingredients.addFree() },
                    onToggleTaxonomy = { ingredients = ingredients.toggleTaxonomy(it) },
                    onRemove = { ingredients = ingredients.remove(it) },
                )

                GeneratorStep.SETTINGS -> GeneratorSettingsScreen(
                    state = settings,
                    taxonomy = taxonomy,
                    onServingsChange = { settings = settings.setServings(it, taxonomy.servings) },
                    onMealTypeSelect = { settings = settings.selectMealType(it) },
                    onTimeBucketSelect = { settings = settings.selectTimeBucket(it) },
                    onDifficultySelect = { settings = settings.selectDifficulty(it) },
                    onToggleRestriction = { settings = settings.toggleHardRestriction(it) },
                    onClearRestrictions = { settings = settings.clearHardRestrictions() },
                    onNotesChange = { settings = settings.typeNotes(it) },
                    onEditProfile = onEditProfile,
                )
            }
        }
    }
}

/** Barra superior del asistente: "Atras" (texto, sin iconos) y el titulo del paso. */
@Composable
private fun GeneratorHeader(
    title: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SazonSpacing.sm,
                end = SazonSpacing.md,
                top = SazonSpacing.sm,
                bottom = SazonSpacing.sm,
            ),
    ) {
        TextButton(onClick = onBack) {
            Text(text = "Atrás")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = SazonSpacing.sm),
        )
    }
}

/** Barra inferior anclada con la unica accion principal terracota del paso. */
@Composable
private fun GeneratorPrimaryBar(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SazonSpacing.lg,
                end = SazonSpacing.lg,
                top = SazonSpacing.sm,
                bottom = SazonSpacing.md,
            ),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = SazonPillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(SazonComponents.buttonHeight),
        ) {
            Text(text = label)
        }
    }
}

/**
 * Saver de [GeneratorStep] para `rememberSaveable`: guarda el nombre del enum (la logica pura no conoce
 * Compose). Mantiene la logica libre de Compose.
 */
private val GeneratorStepSaver: Saver<GeneratorStep, String> = Saver(
    save = { it.name },
    restore = { GeneratorStep.valueOf(it) },
)

/**
 * Saver de [GeneratorIngredientsState] para `rememberSaveable`, en la capa Compose: guarda
 * `[query, count, name, id]...` (la `id` vacia se restaura como `null`, ingrediente libre).
 */
private val GeneratorIngredientsStateSaver: Saver<GeneratorIngredientsState, Any> = listSaver(
    save = { state ->
        buildList {
            add(state.query)
            add(state.ingredients.size.toString())
            state.ingredients.forEach { ingredient ->
                add(ingredient.name)
                add(ingredient.ingredientId.orEmpty())
            }
        }
    },
    restore = { saved ->
        var index = 0
        fun nextString(): String = saved[index++]
        val query = nextString()
        val count = nextString().toInt()
        val ingredients = buildList {
            repeat(count) {
                val name = nextString()
                val id = nextString()
                add(GeneratorIngredient(name = name, ingredientId = id.ifEmpty { null }))
            }
        }
        GeneratorIngredientsState(ingredients = ingredients, query = query)
    },
)

/**
 * Saver de [GeneratorSettingsState] para `rememberSaveable`, en la capa Compose: guarda
 * `[servings, mealTypeId.orEmpty(), timeBucketId, difficultyId, count, …restrictionIds, notes]` (el
 * `mealTypeId` vacio se restaura como `null`, sin preferencia). Mantiene la logica libre de Compose y
 * conserva los ajustes al ir y volver del paso 1.
 */
private val GeneratorSettingsStateSaver: Saver<GeneratorSettingsState, Any> = listSaver(
    save = { state ->
        buildList {
            add(state.servings.toString())
            add(state.mealTypeId.orEmpty())
            add(state.timeBucketId)
            add(state.difficultyId)
            add(state.hardRestrictionIds.size.toString())
            state.hardRestrictionIds.forEach { add(it) }
            add(state.notes)
        }
    },
    restore = { saved ->
        var index = 0
        fun nextString(): String = saved[index++]
        val servings = nextString().toInt()
        val mealTypeId = nextString().ifEmpty { null }
        val timeBucketId = nextString()
        val difficultyId = nextString()
        val count = nextString().toInt()
        val restrictions = buildList { repeat(count) { add(nextString()) } }
        val notes = nextString()
        GeneratorSettingsState(
            servings = servings,
            mealTypeId = mealTypeId,
            timeBucketId = timeBucketId,
            difficultyId = difficultyId,
            hardRestrictionIds = restrictions,
            notes = notes,
        )
    },
)
