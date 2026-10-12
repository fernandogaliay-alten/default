package com.sazon.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.artwork.RecipeArtwork
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Tarjeta de receta del carrusel "Sugeridas para ti".
 *
 * La imagen la pinta el componente unico `ui.artwork.RecipeArtwork` (glifo de linea por categoria,
 * acento del degradado por cocina, fallback total): el repositorio no tiene assets de receta ni
 * dependencia de iconos. La geometria aceptada (16:10, esquinas 24 del `Surface`) la aporta este
 * llamante. El click opcional abre el Detalle (`recipe-detail-screen`).
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
            RecipeArtwork(
                recipe = recipe,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SazonComponents.recipeCardImageRatio),
            )
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
