package com.sazon.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonPrimaryContainer
import com.sazon.app.ui.theme.SazonSecondaryContainer
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Tarjeta de receta del carrusel "Sugeridas para ti".
 *
 * El repositorio no tiene assets de receta ni dependencia de iconos, asi que la imagen es un **bloque
 * degradado** melocoton->oliva con un glifo de plato dibujado con Compose (mismo criterio documentado
 * en `AvatarPreview`). El click opcional abre el Detalle (`recipe-detail-screen`).
 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    taxonomy: Taxonomy,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .width(220.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = SazonShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SazonComponents.recipeCardImageRatio)
                    .background(
                        Brush.linearGradient(
                            listOf(SazonPrimaryContainer, SazonSecondaryContainer),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PlateGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SazonSpacing.sm),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RecipeCardFormat.matchBadge(recipe.matchScore)?.let { badge ->
                    Surface(
                        shape = SazonPillShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(
                                horizontal = SazonSpacing.sm,
                                vertical = 4.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Glifo de plato (dos circulos concentricos) dibujado sin dependencias de iconos. */
@Composable
private fun PlateGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(48.dp)) {
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
