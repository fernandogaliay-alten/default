package com.sazon.app.ui.favorites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.home.RecipeCardFormat
import com.sazon.app.ui.theme.SazonPrimaryContainer
import com.sazon.app.ui.theme.SazonSecondaryContainer
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/** Cifras tabulares para la meta y el conteo ("tnum" en `DESIGN.md`). */
private const val TABULAR_FIGURES = "tnum"

/**
 * Tarjeta horizontal de Favorito (`RecipeListCard` de `DESIGN.md`): miniatura degradada 96x96
 * redondeada con glifo de plato, titulo (2 lineas), meta en cifras tabulares y el **corazon** a la
 * derecha (relleno; quita el Favorito). Tap en la tarjeta abre el Detalle.
 *
 * La quita secundaria es un swipe a la derecha con la accion visible "Eliminar" sobre el
 * [SwipeToDismissBox] de Material 3 (sin dependencia de iconos); tanto el corazon como el swipe llaman
 * a [onRemove], la operacion pura `remove(recipeId)`.
 */
@Composable
fun FavoriteRecipeCard(
    favorite: Favorite,
    taxonomy: Taxonomy,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { onRemove() },
        backgroundContent = { DismissBackground() },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = SazonShapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SazonSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecipeThumbnail()
                Spacer(modifier = Modifier.width(SazonSpacing.md))
                FavoriteText(
                    taxonomy = taxonomy,
                    recipe = favorite.recipe,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(SazonSpacing.sm))
                HeartButton(onClick = onRemove)
            }
        }
    }
}

/**
 * Version compacta en rejilla del Favorito (dos columnas): miniatura degradada arriba con el corazon
 * superpuesto, titulo y meta debajo. Comparte la logica de [FavoriteRecipeCard].
 */
@Composable
fun FavoriteRecipeTile(
    favorite: Favorite,
    taxonomy: Taxonomy,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = SazonShapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.linearGradient(listOf(SazonPrimaryContainer, SazonSecondaryContainer)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PlateGlyph(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(SazonSpacing.sm),
                ) {
                    HeartButton(onClick = onRemove)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SazonSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = favorite.recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = RecipeCardFormat.metaLine(taxonomy, favorite.recipe),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = TABULAR_FIGURES,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Miniatura 96x96 redondeada con el degradado melocoton->oliva y el glifo de plato. */
@Composable
private fun RecipeThumbnail() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(SazonShapes.small)
            .background(Brush.linearGradient(listOf(SazonPrimaryContainer, SazonSecondaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        PlateGlyph(
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
    }
}

/** Titulo (2 lineas) y meta de la tarjeta horizontal. */
@Composable
private fun FavoriteText(
    taxonomy: Taxonomy,
    recipe: Recipe,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = RecipeCardFormat.metaLine(taxonomy, recipe),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Corazon de la tarjeta: relleno (la receta ya es favorita) y quita al pulsar. */
@Composable
private fun HeartButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            HeartGlyph(
                filled = true,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Fondo del swipe: placa terracota con la accion visible "Eliminar" alineada al final. */
@Composable
private fun DismissBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(SazonShapes.large)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = "Eliminar",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(horizontal = SazonSpacing.lg),
        )
    }
}

/** Glifo de plato (dos circulos concentricos) dibujado sin dependencias de iconos. */
@Composable
private fun PlateGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx())
        drawCircle(
            color = tint,
            radius = size.minDimension / 2f - stroke.width,
            style = stroke,
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.28f,
            style = stroke,
        )
    }
}

/** Corazon dibujado con Compose (sin iconos externos): relleno o contorno. */
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
