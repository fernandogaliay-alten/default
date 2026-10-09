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
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Huesped del asistente del Generador dentro de la pagina empujada [PushedScreen.GENERATOR]
 * (`com.sazon.app.ui.navigation`).
 *
 * Mantiene el **paso** (`rememberSaveable`) y el borrador de ingredientes
 * ([GeneratorIngredientsState] con su `Saver`, en la capa Compose) para que el paso 2 lo conserve. Pinta
 * una barra superior con "Atras" y el titulo del paso y el primario anclado (`step.primaryLabel`),
 * deshabilitado en [GeneratorStep.INGREDIENTS] mientras `!ingredients.canContinue`.
 *
 * "Atras" es `step.previous() ?: onClose()` (desde el paso 1 cierra el Generador, desde el 2 vuelve al 1
 * conservando los ingredientes). El primario es `step.next()`: en [GeneratorStep.SETTINGS] no tiene
 * efecto (la generacion es de `generation-flow-with-validation`).
 *
 * El paso 1 ([GeneratorIngredientsScreen]) es de esta feature; el paso 2 es un **marcador** que rellenara
 * `generator-settings-step`.
 */
@Composable
fun GeneratorFlow(
    taxonomy: Taxonomy,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable(stateSaver = GeneratorStepSaver) {
        mutableStateOf(GeneratorStep.INGREDIENTS)
    }
    var ingredients by rememberSaveable(stateSaver = GeneratorIngredientsStateSaver) {
        mutableStateOf(GeneratorIngredientsState())
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

                GeneratorStep.SETTINGS -> GeneratorSettingsPlaceholder()
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

/** Marcador del paso 2 ("Ajusta tu receta"); su contenido es de `generator-settings-step`. */
@Composable
private fun GeneratorSettingsPlaceholder() {
    Text(
        text = "El paso de ajustes llega en 'generator-settings-step'.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
