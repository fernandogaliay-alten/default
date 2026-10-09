package com.sazon.app.ui.favorites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Pestana **Favoritos** (`MainTab.FAVORITES`): la lista local de recetas guardadas, consultable sin
 * conexion. Capa Compose **delgada** sobre la logica pura [FavoritesList]/[FavoritesSort]/
 * [FavoritesViewMode].
 *
 * Estructura (direccion de `docs/design/concepts/favorites/favoritos/`):
 * - cabecera "Mis recetas guardadas" + linea de conteo ([FavoritesList.countLabel]);
 * - buscador ([OutlinedTextField]) y fila de chips de orden ([FavoritesSort.entries]) con el toggle
 *   lista/rejilla;
 * - nota discreta "Disponibles sin conexion";
 * - cuerpo poblado: lista de [FavoriteRecipeCard] o rejilla de dos columnas ([FavoriteRecipeTile]);
 * - **estado vacio** cuando no hay favoritos: glifo de marcador, titular, cuerpo y boton primario
 *   "Explorar recetas" ([onExplore]).
 *
 * El estado `query`/`sort`/`viewMode` vive en `rememberSaveable` con `Saver` (patron de `HomeScreen`);
 * la lista llega observada por `MainShell`, no se recuerda aqui. Sin red y sin pantalla de carga.
 */
@Composable
fun FavoritesScreen(
    favorites: List<Favorite>,
    taxonomy: Taxonomy,
    onOpenRecipe: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable(stateSaver = FavoritesSortSaver) {
        mutableStateOf(FavoritesSort.RECENT)
    }
    var viewMode by rememberSaveable(stateSaver = FavoritesViewModeSaver) {
        mutableStateOf(FavoritesViewMode.LIST)
    }

    val visible = FavoritesList.visible(favorites, query, sort, taxonomy)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SazonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
    ) {
        item { FavoritesHeader(count = favorites.size) }

        if (favorites.isEmpty()) {
            item { FavoritesEmptyState(onExplore = onExplore) }
        } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(text = "Buscar por título") },
                    singleLine = true,
                    shape = SazonShapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                FavoritesSortRow(
                    selected = sort,
                    onSelect = { sort = it },
                    viewMode = viewMode,
                    onSelectViewMode = { viewMode = it },
                )
            }
            item { OfflineNote() }

            if (visible.isEmpty()) {
                item {
                    Text(
                        text = "No hay favoritas con esta búsqueda.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (viewMode == FavoritesViewMode.LIST) {
                items(visible, key = { it.recipeId }) { favorite ->
                    FavoriteRecipeCard(
                        favorite = favorite,
                        taxonomy = taxonomy,
                        onClick = { onOpenRecipe(favorite.recipe) },
                        onRemove = { onToggleFavorite(favorite.recipe) },
                    )
                }
            } else {
                items(visible.chunked(2), key = { row -> row.first().recipeId }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.md),
                    ) {
                        row.forEach { favorite ->
                            FavoriteRecipeTile(
                                favorite = favorite,
                                taxonomy = taxonomy,
                                onClick = { onOpenRecipe(favorite.recipe) },
                                onRemove = { onToggleFavorite(favorite.recipe) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** Cabecera "Mis recetas guardadas" con la linea de conteo en cifras tabulares. */
@Composable
private fun FavoritesHeader(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Mis recetas guardadas",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = FavoritesList.countLabel(count),
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Fila de chips de orden (terracota) + toggle lista/rejilla. */
@Composable
private fun FavoritesSortRow(
    selected: FavoritesSort,
    onSelect: (FavoritesSort) -> Unit,
    viewMode: FavoritesViewMode,
    onSelectViewMode: (FavoritesViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(FavoritesSort.entries) { entry ->
                FilterChip(
                    selected = entry == selected,
                    onClick = { onSelect(entry) },
                    label = { Text(text = entry.label) },
                    shape = SazonPillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.size(SazonSpacing.sm))
        ViewModeButton(
            label = "Lista",
            active = viewMode == FavoritesViewMode.LIST,
            onClick = { onSelectViewMode(FavoritesViewMode.LIST) },
        )
        Spacer(modifier = Modifier.size(4.dp))
        ViewModeButton(
            label = "Rejilla",
            active = viewMode == FavoritesViewMode.GRID,
            onClick = { onSelectViewMode(FavoritesViewMode.GRID) },
        )
    }
}

@Composable
private fun ViewModeButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(text = label) },
        shape = SazonPillShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
    )
}

/** Nota discreta "Disponibles sin conexion" con un glifo de nube tachada dibujado con Compose. */
@Composable
private fun OfflineNote() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CloudOffGlyph(
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Disponibles sin conexión",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Estado vacio: glifo de marcador, titular, cuerpo y boton primario "Explorar recetas". */
@Composable
private fun FavoritesEmptyState(onExplore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SazonSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = SazonShapes.medium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BookmarkGlyph(
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = "Aún no has guardado ninguna receta",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Pulsa el corazón de cualquier receta para tenerla siempre a mano.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onExplore,
            shape = SazonPillShape,
            modifier = Modifier.height(SazonComponents.buttonHeight),
        ) {
            Text(text = "Explorar recetas")
        }
    }
}

/** Glifo de nube tachada (nube + diagonal) dibujado sin dependencias de iconos. */
@Composable
private fun CloudOffGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        val path = Path().apply {
            moveTo(0.18f * size.width, 0.72f * size.height)
            cubicTo(
                -0.05f * size.width, 0.72f * size.height,
                0.02f * size.width, 0.34f * size.height,
                0.30f * size.width, 0.40f * size.height,
            )
            cubicTo(
                0.38f * size.width, 0.10f * size.height,
                0.86f * size.width, 0.16f * size.height,
                0.82f * size.width, 0.52f * size.height,
            )
            cubicTo(
                1.02f * size.width, 0.56f * size.height,
                0.98f * size.width, 0.72f * size.height,
                0.82f * size.width, 0.72f * size.height,
            )
        }
        drawPath(path = path, color = tint, style = stroke)
        drawLine(
            color = tint,
            start = Offset(0.12f * size.width, 0.18f * size.height),
            end = Offset(0.88f * size.width, 0.86f * size.height),
            strokeWidth = stroke.width,
        )
    }
}

/** Glifo de marcador (bookmark) del estado vacio, dibujado con Compose. */
@Composable
private fun BookmarkGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0.26f * size.width, 0.10f * size.height)
            lineTo(0.74f * size.width, 0.10f * size.height)
            lineTo(0.74f * size.width, 0.90f * size.height)
            lineTo(0.50f * size.width, 0.68f * size.height)
            lineTo(0.26f * size.width, 0.90f * size.height)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

/** Saver de [FavoritesSort] para `rememberSaveable`: guarda y restaura por [FavoritesSort.key]. */
private val FavoritesSortSaver: Saver<FavoritesSort, String> = Saver(
    save = { it.key },
    restore = { FavoritesSort.fromKey(it) },
)

/** Saver de [FavoritesViewMode] para `rememberSaveable`: por [FavoritesViewMode.key]. */
private val FavoritesViewModeSaver: Saver<FavoritesViewMode, String> = Saver(
    save = { it.key },
    restore = { FavoritesViewMode.fromKey(it) },
)
