package com.sazon.app.ui.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.sazon.app.ui.home.RecipeCardFormat
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonPrimaryContainer
import com.sazon.app.ui.theme.SazonSecondaryContainer
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Tarjeta de una receta en la pantalla de Resultados (a ancho completo).
 *
 * Imagen placeholder de **degradado** melocoton->oliva (16:10, esquinas 24px) con glifo de plato, titulo
 * (2 lineas), descripcion (1 linea), la linea de metadatos `RecipeCardFormat.metaLine` (cifras
 * tabulares) y la pildora de coincidencia `RecipeCardFormat.matchBadge` en terracota.
 *
 * Reutiliza el mismo degradado/glifo que `com.sazon.app.ui.home.RecipeCard` (el repositorio no tiene
 * assets de receta ni iconos). **Sin corazon** y **sin click**: favoritos y detalle son features
 * propias (`favorites`, `recipe-detail-screen`), fuera de alcance.
 */
@Composable
fun ResultRecipeCard(
    recipe: Recipe,
    taxonomy: Taxonomy,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SazonShapes.large,
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
                    .padding(SazonSpacing.md),
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
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = RecipeCardFormat.metaLine(taxonomy, recipe),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
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
                                horizontal = SazonSpacing.md,
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
    Canvas(modifier = modifier.size(56.dp)) {
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
