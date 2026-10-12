package com.sazon.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.artwork.RecipeArtwork
import com.sazon.app.ui.preferences.ServingsStepper
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonPrimaryContainer
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/** Cifras tabulares para cantidades, comensales y nutricion ("tnum" en `DESIGN.md`). */
private const val TABULAR_FIGURES = "tnum"

/**
 * Pantalla de Detalle de receta: capa Compose **delgada** sobre la logica pura de `ui.detail`
 * ([ServingScaler], [IngredientFormat], [RecipeDetailFormat], [RecipeDetailState]).
 *
 * Estructura (direccion de `docs/design/concepts/recipe-detail-screen/detalle-receta/`):
 * - **Hero**: imagen de receta de `ui.artwork.RecipeArtwork` (240 dp, glifo 72 dp) con "Atras" y el
 *   corazon/guardar flotante.
 * - **Tarjeta** superpuesta con titulo, descripcion y los cuatro pares de [RecipeDetailFormat].
 * - **Pestanas** Ingredientes · Pasos · Notas (activa terracota).
 * - Cuerpo por pestana: ajustador de comensales ([ServingsStepper] reutilizado **sin cambios**) con la
 *   caption "Cantidades ajustadas automaticamente" + filas nombre/cantidad; timeline de pasos con
 *   pildora ambar `"N min"`; tips y rejilla de nutricion opcional.
 * - **Barra inferior** fija con "Guardar" (outline) y el primario terracota "Empezar a cocinar".
 *
 * `isSaved`/`onToggleSave` y `onStartCooking` son **costuras no-op** de `favorites` y del modo cocina:
 * se pintan segun la captura, pero esta feature no persiste ni cocina. La receta es un parametro (no se
 * guarda en el `Saver`); el estado memorizado es comensales + pestana.
 */
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    taxonomy: Taxonomy,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onStartCooking: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by rememberSaveable(stateSaver = RecipeDetailStateSaver) {
        mutableStateOf(RecipeDetailState.initial(recipe, taxonomy.servings))
    }
    val ingredients = ServingScaler.scaledIngredients(recipe, state.servings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DetailBottomBar(
                isSaved = isSaved,
                onToggleSave = onToggleSave,
                onStartCooking = onStartCooking,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            DetailHero(
                recipe = recipe,
                onBack = onBack,
                isSaved = isSaved,
                onToggleSave = onToggleSave,
            )
            // La tarjeta sube sobre el hero (superficie superpuesta de `DESIGN.md`).
            Column(modifier = Modifier.offset(y = (-SazonSpacing.lg))) {
                DetailCard(taxonomy = taxonomy, recipe = recipe)
                DetailTabs(
                    selected = state.selectedTab,
                    onSelect = { state = state.selectTab(it) },
                )
                when (state.selectedTab) {
                    RecipeDetailTab.INGREDIENTS -> IngredientsTab(
                        taxonomy = taxonomy,
                        servings = state.servings,
                        onServingsChange = { state = state.setServings(it, taxonomy.servings) },
                        ingredients = ingredients,
                    )

                    RecipeDetailTab.STEPS -> StepsTab(steps = recipe.steps)

                    RecipeDetailTab.NOTES -> NotesTab(tips = recipe.tips, nutrition = recipe.nutrition)
                }
                Spacer(modifier = Modifier.height(SazonSpacing.lg))
            }
        }
    }
}

/** Hero: imagen de receta ([RecipeArtwork], alto 240) con "Atras" y el corazon/guardar flotante. */
@Composable
private fun DetailHero(
    recipe: Recipe,
    onBack: () -> Unit,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        RecipeArtwork(
            recipe = recipe,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            glyphSize = 72.dp,
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(SazonSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = SazonPillShape, color = MaterialTheme.colorScheme.surface) {
                TextButton(onClick = onBack) {
                    Text(text = "Atrás")
                }
            }
            Surface(
                onClick = onToggleSave,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    HeartGlyph(
                        filled = isSaved,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** Tarjeta superpuesta: titulo (Manrope), descripcion y la meta de cuatro pares. */
@Composable
private fun DetailCard(
    taxonomy: Taxonomy,
    recipe: Recipe,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SazonShapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SazonSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MetaStrip(taxonomy = taxonomy, recipe = recipe)
        }
    }
}

/** Meta del hero: cuatro pares (Tiempo, Dificultad, Comensales, Calorias) en cifras tabulares. */
@Composable
private fun MetaStrip(
    taxonomy: Taxonomy,
    recipe: Recipe,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SazonShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SazonSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            RecipeDetailFormat.metaEntries(taxonomy, recipe).forEach { meta ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = meta.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = meta.value,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = TABULAR_FIGURES,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Fila de pestanas fija; la activa va rellena de terracota. */
@Composable
private fun DetailTabs(
    selected: RecipeDetailTab,
    onSelect: (RecipeDetailTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg, vertical = SazonSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        RecipeDetailTab.entries.forEach { tab ->
            val active = tab == selected
            Surface(
                onClick = { onSelect(tab) },
                shape = SazonPillShape,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = SazonSpacing.sm),
                )
            }
        }
    }
}

/** Pestana Ingredientes: ajustador de comensales + caption + filas nombre/cantidad. */
@Composable
private fun IngredientsTab(
    taxonomy: Taxonomy,
    servings: Int,
    onServingsChange: (Int) -> Unit,
    ingredients: List<RecipeIngredient>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SazonShapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SazonSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Comensales",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = RecipeDetailFormat.ADJUSTED_CAPTION,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ServingsStepper(
                    servings = taxonomy.servings,
                    value = servings,
                    onChange = onServingsChange,
                )
            }
        }
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        ingredients.forEachIndexed { index, ingredient ->
            IngredientRow(ingredient = ingredient)
            if (index != ingredients.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

/** Fila de ingrediente: checkbox local (visual), nombre y cantidad a la derecha en cifras tabulares. */
@Composable
private fun IngredientRow(ingredient: RecipeIngredient) {
    var checked by remember(ingredient.name) { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Spacer(modifier = Modifier.width(SazonSpacing.sm))
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(SazonSpacing.sm))
        Text(
            text = IngredientFormat.quantityLabel(ingredient.quantity, ingredient.unit),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Pestana Pasos: timeline vertical numerado con pildora ambar de duracion (`tertiaryContainer`). */
@Composable
private fun StepsTab(steps: List<RecipeStep>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg),
    ) {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = SazonSpacing.sm),
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = step.order.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFeatureSettings = TABULAR_FIGURES,
                                ),
                            )
                        }
                    }
                    if (index != steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(SazonPrimaryContainer),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(SazonSpacing.sm))
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = SazonShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SazonSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(SazonSpacing.sm))
                            DurationPill(minutes = step.minutes)
                        }
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Pildora ambar con la duracion del paso; nunca roja (`DESIGN.md`). */
@Composable
private fun DurationPill(minutes: Int) {
    Surface(
        shape = SazonPillShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = "$minutes min",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            modifier = Modifier.padding(horizontal = SazonSpacing.sm, vertical = 4.dp),
        )
    }
}

/** Pestana Notas: tips y, si existe, rejilla compacta de nutricion; sin datos no inventa cifras. */
@Composable
private fun NotesTab(
    tips: List<String>,
    nutrition: Nutrition?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
    ) {
        if (tips.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SazonShapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SazonSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                ) {
                    Text(
                        text = "Consejos del chef",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    tips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (nutrition != null) {
            NutritionCard(nutrition = nutrition)
        } else if (tips.isEmpty()) {
            Text(
                text = "Sin información nutricional.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Rejilla compacta `Calorias · Proteinas · Carbohidratos · Grasas`. */
@Composable
private fun NutritionCard(nutrition: Nutrition) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SazonShapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SazonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            Text(
                text = "Información nutricional",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            ) {
                NutritionCell(label = "Calorías", value = "${nutrition.kcal} kcal", modifier = Modifier.weight(1f))
                NutritionCell(label = "Proteínas", value = "${nutrition.protein} g", modifier = Modifier.weight(1f))
                NutritionCell(label = "Carbohidratos", value = "${nutrition.carbs} g", modifier = Modifier.weight(1f))
                NutritionCell(label = "Grasas", value = "${nutrition.fat} g", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NutritionCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/** Barra inferior fija: "Guardar" (outline, costura de `favorites`) + primario "Empezar a cocinar". */
@Composable
private fun DetailBottomBar(
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onStartCooking: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SazonSpacing.lg,
                    end = SazonSpacing.lg,
                    top = SazonSpacing.sm,
                    bottom = SazonSpacing.md,
                ),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            OutlinedButton(
                onClick = onToggleSave,
                shape = SazonPillShape,
                modifier = Modifier
                    .weight(1f)
                    .height(SazonComponents.buttonHeight),
            ) {
                Text(text = if (isSaved) "Guardada" else "Guardar")
            }
            Button(
                onClick = onStartCooking,
                shape = SazonPillShape,
                modifier = Modifier
                    .weight(1.6f)
                    .height(SazonComponents.buttonHeight),
            ) {
                Text(text = "Empezar a cocinar")
            }
        }
    }
}

/** Corazon dibujado con Compose (sin iconos externos): relleno si esta guardada, contorno si no. */
@Composable
private fun HeartGlyph(
    filled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(0.5f * width, 0.88f * height)
            cubicTo(0.08f * width, 0.55f * height, 0.16f * width, 0.12f * height, 0.5f * width, 0.36f * height)
            cubicTo(0.84f * width, 0.12f * height, 0.92f * width, 0.55f * height, 0.5f * width, 0.88f * height)
            close()
        }
        if (filled) {
            drawPath(path = path, color = tint)
        } else {
            drawPath(path = path, color = tint, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/**
 * Saver de [RecipeDetailState] para `rememberSaveable`, en la capa Compose: guarda
 * `[servings, nombre de la pestana]`. La `Recipe` **no** se guarda (es un parametro), de modo que la
 * logica pura queda libre de Compose.
 */
private val RecipeDetailStateSaver: Saver<RecipeDetailState, Any> = listSaver(
    save = { state -> listOf(state.servings.toString(), state.selectedTab.name) },
    restore = { saved ->
        RecipeDetailState(
            servings = (saved[0] as String).toInt(),
            selectedTab = RecipeDetailTab.valueOf(saved[1] as String),
        )
    },
)
