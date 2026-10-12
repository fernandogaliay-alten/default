package com.sazon.app.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.sazon.app.ui.home.RecipeCardFormat
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Tarjeta de una receta en la pantalla de Resultados (a ancho completo).
 *
 * La imagen la pinta el componente unico `ui.artwork.RecipeArtwork` (glifo por categoria, acento por
 * cocina): se conserva el 16:10 con esquinas 24 del `Surface`. Debajo, titulo (2 lineas), descripcion
 * (1 linea), la linea de metadatos `RecipeCardFormat.metaLine` (cifras tabulares) y la pildora de
 * coincidencia `RecipeCardFormat.matchBadge` en terracota.
 *
 * **Sin corazon** (favoritos es de `favorites`); el click opcional abre el Detalle
 * (`recipe-detail-screen`).
 */
@Composable
fun ResultRecipeCard(
    recipe: Recipe,
    taxonomy: Taxonomy,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = SazonShapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column {
            RecipeArtwork(
                recipe = recipe,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SazonComponents.recipeCardImageRatio),
                glyphSize = 56.dp,
            )
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
