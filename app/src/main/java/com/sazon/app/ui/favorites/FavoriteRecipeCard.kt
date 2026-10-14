package com.sazon.app.ui.favorites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.home.RecipeCardFormat
import com.sazon.app.ui.home.RecipeFicha
import com.sazon.app.ui.home.RecipeMonogram
import com.sazon.app.ui.theme.LocalSazonCardColors
import com.sazon.app.ui.theme.SazonFontFamilies
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/** Cifras tabulares para la meta y el conteo ("tnum" en `DESIGN.md`). */
private const val TABULAR_FIGURES = "tnum"

/** Lado de la miniatura de la lista (`DESIGN.md` → `recipe-tile-favorites.size`). */
private val TILE_SIZE = 96.dp

/** Regla terracota de la miniatura (`DESIGN.md` → `accentRule`): 5px a la izquierda. */
private val TILE_ACCENT_RULE = 5.dp

/** Monograma del plato: Manrope Bold 30sp sobre `surfaceVariant` (prototipo C3, `.mono`). */
private val MONOGRAM_STYLE = TextStyle(
    fontFamily = SazonFontFamilies.Display,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.04f).em,
)

/**
 * Tarjeta horizontal de Favorito (`RecipeListCard` de `DESIGN.md`): miniatura 96x96 con el
 * **monograma** de iniciales del plato, titulo (2 lineas), meta en cifras tabulares y el **corazon** a
 * la derecha (relleno; quita el Favorito). Tap en la tarjeta abre el Detalle.
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
    val card = LocalSazonCardColors.current
    // Escenario 6: en oscuro **cualquier** tarjeta usa la superficie y el borde del contrato de tarjeta
    // (el `darkSurface` #312720 daba 1,15:1 con el fondo y es justo el defecto que corrige el ADR 0006);
    // en claro se queda como estaba (la `surface` blanca y sin borde).
    val darkCard = card.boundaryWidth > 0.dp

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
            color = card.surface,
            border = if (darkCard) BorderStroke(card.boundaryWidth, card.boundary) else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SazonSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecipeMonogramTile(recipe = favorite.recipe)
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
 * Version compacta en rejilla del Favorito (dos columnas): la **ficha tipografica compacta** de
 * `RecipeFicha` (categoria, titulo a 2 lineas, ingredientes en una linea, filete por cocina y marca de
 * agua), con el **corazon** superpuesto en la esquina superior derecha, dentro de la estructura **L2
 * unificada** (hueco **16:9** + banda solida). La celda omite la fila de metadatos: a ~172dp de ancho es
 * lo unico que cabe sin subir la tarjeta a 3:2, y es exactamente lo que muestra la captura
 * `docs/design/concepts/recipe-card-typographic/favoritos-rejilla/`. Comparte la logica de
 * [FavoriteRecipeCard]; con el ajuste apagado (default) **sin** capa de fondo; con [imageBackground]
 * `true` la imagen generada es la capa de fondo del hueco (misma geometria).
 */
@Composable
fun FavoriteRecipeTile(
    favorite: Favorite,
    taxonomy: Taxonomy,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    imageBackground: Boolean = false,
) {
    RecipeFicha(
        recipe = favorite.recipe,
        taxonomy = taxonomy,
        modifier = modifier,
        wide = false,
        compact = true,
        onClick = onClick,
        overlay = {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(SazonSpacing.sm),
            ) {
                HeartButton(onClick = onRemove)
            }
        },
        imageBackground = imageBackground,
    )
}

/**
 * Miniatura 96x96 de la lista de Favoritos (`DESIGN.md` → `recipe-tile-favorites`): superficie
 * `surfaceVariant`, esquina 24 y la **regla terracota de 5px a la izquierda** (`primary`, **no** por
 * cocina: a 5px los pares suaves de `RecipeAccent` no se perciben). El contenido es el **monograma**
 * de iniciales del plato ([RecipeMonogram]); una receta sin palabras significativas no pinta monograma.
 */
@Composable
private fun RecipeMonogramTile(recipe: Recipe) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(TILE_SIZE)
            .clip(SazonShapes.medium)
            .background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(TILE_ACCENT_RULE)
                .fillMaxHeight()
                .background(scheme.primary),
        )
        RecipeMonogram.of(recipe.title)?.let { initials ->
            Text(
                text = initials,
                style = MONOGRAM_STYLE,
                color = scheme.onSurfaceVariant,
            )
        }
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
