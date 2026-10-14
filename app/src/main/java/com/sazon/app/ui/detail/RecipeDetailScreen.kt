package com.sazon.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.artwork.DishImageArtworkOverlay
import com.sazon.app.ui.artwork.RecipeArtwork
import com.sazon.app.ui.artwork.RecipeArtworkStyle
import com.sazon.app.ui.artwork.RecipeGlyphArtwork
import com.sazon.app.ui.artwork.RecipeStyle
import com.sazon.app.ui.artwork.recipeAccentBrush
import com.sazon.app.ui.artwork.recipeAccentColors
import com.sazon.app.ui.home.MainIngredient
import com.sazon.app.ui.preferences.ServingsStepper
import com.sazon.app.ui.theme.CardImageOverlay
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonFontFamilies
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonPrimaryContainer
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/** Cifras tabulares para cantidades, comensales y nutricion ("tnum" en `DESIGN.md`). */
private const val TABULAR_FIGURES = "tnum"

/** Esquinas **inferiores** del hero y de la tira (`SazonShapes.medium` = 24 dp); las superiores, rectas. */
private val HeroBottomShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

/** Padding interior del hero (prototipo C3 del Detalle: `18px`). */
private val HERO_PADDING = 18.dp

/** Desbordamiento de la marca de agua por la esquina inferior derecha (prototipo: `-24px`/`-6px`). */
private val WATERMARK_OFFSET_X = 24.dp
private val WATERMARK_OFFSET_Y = 6.dp

/** Aire entre el titulo y los ingredientes del hero (prototipo: `12px`). */
private val HERO_INGREDIENTS_TOP = SazonSpacing.sm

/** Separacion entre las filas de ingredientes del hero (prototipo: `3px`). */
private val INGREDIENT_ROW_GAP = 3.dp

/** Aire entre la fila de ingrediente y su hairline (prototipo: `2px`). */
private val INGREDIENT_ROW_PADDING_BOTTOM = 2.dp

/** Hairline de las filas de ingredientes y de la tira (`DESIGN.md`). */
private val HAIRLINE = 1.dp

/**
 * Pantalla de Detalle de receta: capa Compose **delgada** sobre la logica pura de `ui.detail`
 * ([ServingScaler], [IngredientFormat], [RecipeDetailFormat], [RecipeDetailState]).
 *
 * Estructura (direccion de `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/`):
 * - **Hero** ([RecipeDetailHeroFormat]): 240 dp, categoría en versalitas, título en Manrope,
 *   los **tres ingredientes principales con cantidad** (cifras tabulares), marca de agua del glifo al
 *   12 % y filete de 6 px con el degradado de la cocina. Con el ajuste de imágenes apagado **sin
 *   ninguna foto** (ADR 0006); con `imageBackground` la imagen generada es la **capa de fondo** del hero
 *   y la tipografía pasa al tratamiento sobre imagen (sin mover la geometría). "Atras" y el
 *   corazon/guardar siguen flotando encima.
 * - **Tira de metadatos**: bloque propio justo debajo del hero (los cuatro pares de [RecipeDetailFormat]),
 *   sin tarjeta superpuesta ni desplazamiento.
 * - **Pestanas** Ingredientes · Pasos · Notas (activa terracota).
 * - Cuerpo por pestana: ajustador de comensales ([ServingsStepper] reutilizado **sin cambios**) con la
 *   caption "Cantidades ajustadas automaticamente" + filas nombre/cantidad; timeline de pasos con
 *   pildora ambar `"N min"`; tips y rejilla de nutricion opcional.
 * - **Barra inferior** fija con "Guardar" (outline) y el primario terracota "Empezar a cocinar".
 *
 * La **descripcion** de la receta ya **no** se pinta: vivia en la tarjeta superpuesta que esta feature
 * retira (desviacion declarada en la spec). `isSaved`/`onToggleSave` y `onStartCooking` son **costuras
 * no-op** de `favorites` y del modo cocina: se pintan segun la captura, pero esta feature no persiste
 * ni cocina. La receta es un parametro (no se guarda en el `Saver`); el estado memorizado es
 * comensales + pestana.
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
    imageBackground: Boolean = false,
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
                taxonomy = taxonomy,
                recipe = recipe,
                onBack = onBack,
                isSaved = isSaved,
                onToggleSave = onToggleSave,
                imageBackground = imageBackground,
            )
            // La tira de metadatos ocupa el lugar de la tarjeta superpuesta (sin solape ni `offset`).
            MetaStrip(taxonomy = taxonomy, recipe = recipe)
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

/**
 * Hero del Detalle, **240 dp** ([RecipeDetailHeroFormat.HERO_HEIGHT_DP], geometria intacta): degradado
 * de la cocina, marca de agua del glifo al 12 %, categoria en versalitas, titulo en Manrope y los tres
 * ingredientes con cantidad en cifras tabulares; el filete de 6 px al pie. Con [imageBackground] `true`
 * la **imagen generada** es la **capa de fondo** ([RecipeArtwork]) y la tipografia pasa al tratamiento
 * **sobre imagen**; la marca de agua se pinta **solo** sin imagen. "Atras" (pildora) y el
 * corazon/guardar (48 dp) siguen flotando encima, sin cambios de comportamiento.
 */
@Composable
private fun DetailHero(
    taxonomy: Taxonomy,
    recipe: Recipe,
    onBack: () -> Unit,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    imageBackground: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val style = RecipeArtworkStyle.of(recipe)
    val content = RecipeDetailHeroFormat.of(taxonomy, recipe)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SazonSpacing.md)
                .clip(HeroBottomShape)
                .background(recipeAccentBrush(style.accent))
                .height(RecipeDetailHeroFormat.HERO_HEIGHT_DP.dp),
        ) {
            HeroLayer(
                recipe = recipe,
                style = style,
                content = content,
                imageBackground = imageBackground,
            )
            // Filete de 6 dp con el degradado de la cocina, sobre el borde inferior del hero.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(RecipeDetailHeroFormat.FILLET_HEIGHT_DP.dp)
                    .background(Brush.horizontalGradient(recipeAccentColors(style.accent))),
            )
        }
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

/**
 * Capa de la estructura del hero: la base tipografica (degradado de la cocina + marca de agua) y, con
 * [imageBackground], la imagen generada como **capa de fondo** ([RecipeArtwork]). La tipografia se
 * reubica en el slot para leer la **capa resuelta**: marca de agua solo sin imagen, categoria, titulo y
 * filas de ingredientes con el tratamiento sobre imagen o sobre superficie.
 */
@Composable
private fun BoxScope.HeroLayer(
    recipe: Recipe,
    style: RecipeStyle,
    content: RecipeDetailHeroFormat.HeroContent,
    imageBackground: Boolean,
) {
    if (imageBackground) {
        RecipeArtwork(recipe = recipe, modifier = Modifier.matchParentSize()) { layer ->
            HeroForeground(
                style = style,
                content = content,
                onImage = DishImageArtworkOverlay.headingOnImage(layer),
                watermarkVisible = DishImageArtworkOverlay.watermarkVisible(layer),
            )
        }
    } else {
        HeroForeground(
            style = style,
            content = content,
            onImage = false,
            watermarkVisible = true,
        )
    }
}

/** Contenido del hero (marca de agua + tipografia) con el tratamiento de la capa resuelta. */
@Composable
private fun HeroForeground(
    style: RecipeStyle,
    content: RecipeDetailHeroFormat.HeroContent,
    onImage: Boolean,
    watermarkVisible: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
        // Marca de agua: desborda por la esquina inferior derecha y el `clip` del hero la recorta.
        if (watermarkVisible) {
            RecipeGlyphArtwork(
                glyph = style.glyph,
                tint = scheme.onSurfaceVariant.copy(alpha = RecipeDetailHeroFormat.WATERMARK_ALPHA),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = WATERMARK_OFFSET_X, y = WATERMARK_OFFSET_Y)
                    .size(RecipeDetailHeroFormat.WATERMARK_DP.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(HERO_PADDING),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = content.category.uppercase(),
                style = HeroTypography.category,
                color = if (onImage) CardImageOverlay.onImageVariant else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content.title,
                style = HeroTypography.title,
                color = if (onImage) CardImageOverlay.onImageText else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            HeroIngredients(ingredients = content.ingredients, onImage = onImage)
        }
    }
}

/** Los tres ingredientes del hero: nombre a la izquierda y cantidad tabular a la derecha, con hairline. */
@Composable
private fun HeroIngredients(ingredients: List<MainIngredient>, onImage: Boolean) {
    if (ingredients.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HERO_INGREDIENTS_TOP),
        verticalArrangement = Arrangement.spacedBy(INGREDIENT_ROW_GAP),
    ) {
        ingredients.forEach { ingredient ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = INGREDIENT_ROW_PADDING_BOTTOM),
                ) {
                    Text(
                        text = ingredient.name,
                        style = HeroTypography.ingredientName,
                        color = if (onImage) CardImageOverlay.onImageVariant else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(SazonSpacing.sm))
                    Text(
                        text = ingredient.quantityLabel,
                        style = HeroTypography.ingredientQuantity,
                        color = if (onImage) CardImageOverlay.onImageText else scheme.onSurface,
                    )
                }
                Hairline(if (onImage) CardImageOverlay.onImageHairline else scheme.outline)
            }
        }
    }
}

/**
 * Tira de metadatos: bloque propio (`surface`, esquinas inferiores 24 dp, mismo ancho que el hero) con
 * los **cuatro pares** de [RecipeDetailFormat.metaEntries] en columnas iguales separadas por un hairline
 * vertical (no tras la ultima), etiqueta en versalitas y valor en Manrope tabular.
 */
@Composable
private fun MetaStrip(
    taxonomy: Taxonomy,
    recipe: Recipe,
) {
    val scheme = MaterialTheme.colorScheme
    val entries = RecipeDetailFormat.metaEntries(taxonomy, recipe)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SazonSpacing.md),
        shape = HeroBottomShape,
        color = scheme.surface,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = SazonSpacing.sm, horizontal = 4.dp),
        ) {
            entries.forEachIndexed { index, meta ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = meta.label.uppercase(),
                        style = HeroTypography.metaLabel,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = meta.value,
                        style = HeroTypography.metaValue,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                if (index != entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(HAIRLINE)
                            .background(scheme.outline),
                    )
                }
            }
        }
    }
}

/** Separador de 1 dp con el `outline` del tema (las hairlines de `DESIGN.md`). */
@Composable
private fun Hairline(color: Color = MaterialTheme.colorScheme.outline) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HAIRLINE)
            .background(color),
    )
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

/**
 * Estilos del hero y de la tira, construidos **localmente** desde [SazonFontFamilies] y los valores del
 * prototipo C3 / `DESIGN.md` (29/12,5/9,5 sp…). **No** se anaden roles a `SazonTypography`: el tema solo
 * overridea 4 estilos, asi que `titleSmall`/`labelSmall`/`headlineLarge` no usan Manrope donde se espera.
 */
private object HeroTypography {

    /** Categoria en versalitas del hero (prototipo: `11px`, tracking `.14em`). */
    val category = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.14.em,
    )

    /** Titulo del hero en Manrope (prototipo: `29px` / `1.08` / `-.025em`). */
    val title = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 29.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.025f).em,
    )

    val ingredientName = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    )

    val ingredientQuantity = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )

    /** Etiqueta de la tira en versalitas (prototipo: `9,5px`, tracking `.1em`). */
    val metaLabel = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.1.em,
    )

    /** Valor de la tira en Manrope tabular (prototipo: `14px`). */
    val metaValue = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )
}
