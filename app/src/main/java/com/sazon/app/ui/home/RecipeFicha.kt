package com.sazon.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.artwork.DishImageArtworkOverlay
import com.sazon.app.ui.artwork.FichaBandLayout
import com.sazon.app.ui.artwork.RecipeAccent
import com.sazon.app.ui.artwork.RecipeArtwork
import com.sazon.app.ui.artwork.RecipeArtworkImageLayer
import com.sazon.app.ui.artwork.RecipeArtworkStyle
import com.sazon.app.ui.artwork.RecipeGlyphArtwork
import com.sazon.app.ui.artwork.RecipeStyle
import com.sazon.app.ui.artwork.recipeAccentColors
import com.sazon.app.ui.theme.CardImageOverlay
import com.sazon.app.ui.theme.LocalSazonCardColors
import com.sazon.app.ui.theme.SazonCardTokens
import com.sazon.app.ui.theme.SazonFontFamilies
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/** Padding interior del hueco en la tarjeta estrecha (prototipo C3: `13px`). */
private val NARROW_PADDING = 13.dp

/** Padding interior del hueco en la tarjeta ancha (prototipo C3: `16px`). */
private val WIDE_PADDING = SazonSpacing.md

/** Padding superior de la banda sólida de la variante L2 (prototipo: `11px 13px 13px`). */
private val BAND_PADDING_TOP = 11.dp

/** Filete del borde inferior con el degradado de la cocina (`DESIGN.md`: 6px). */
private val FILLET_HEIGHT = 6.dp

/** Hairline entre filas de ingredientes y sobre la fila de metadatos (`DESIGN.md`). */
private val HAIRLINE = 1.dp

/** Separacion de la fila de metadatos respecto a su hairline (prototipo C3: `7px`). */
private val FOOT_PADDING_TOP = 7.dp

/** Separacion entre las filas de ingredientes de la tarjeta ancha (prototipo C3: `3px`). */
private val INGREDIENT_ROW_GAP = 3.dp

/** Marca de agua del glifo de categoria: tamano y desbordamiento del prototipo C3 (`150px`). */
private val WATERMARK_SIZE = 150.dp
private val WATERMARK_OFFSET_X = 16.dp
private val WATERMARK_OFFSET_Y = 18.dp

/** Opacidad de la marca de agua (`DESIGN.md`: al 12 %, como textura). */
private const val WATERMARK_ALPHA = 0.12f

/** Cifras tabulares (`DESIGN.md`: tiempos, cantidades y comensales se alinean). */
private const val TABULAR_FIGURES = "tnum"

/**
 * Ficha tipografica de una receta (direccion **C3 "carta de restaurante"**, ADR 0006): el contenido
 * **compartido** que consumen `RecipeCard` (Inicio, estrecha), `ResultRecipeCard` (Resultados, ancha) y
 * la rejilla de Favoritos (compacta).
 *
 * Es la estructura **L2 unificada** (`recipe-card-l2-unified`): **siempre** el hueco **16:9**
 * ([DishImageArtworkOverlay.HOLE_RATIO]) con la **categoria en versalitas** y el **titulo**, la **marca
 * de agua** del glifo al 12 % y el **filete de 6px** con el degradado de la cocina al pie; **debajo**,
 * la **banda solida** con los **tres ingredientes principales** con cantidad (una linea o tres filas
 * segun [DishImageArtworkOverlay.bandLayout]) y la fila `tiempo · dificultad · comensales` con la
 * coincidencia en terracota (salvo en [compact]). El cromo sale de [LocalSazonCardColors].
 *
 * Con [imageBackground] `false` (por defecto) **no** se compone [RecipeArtwork]: el contenido del hueco
 * se pinta directamente sobre la superficie de la tarjeta con la capa sintetica
 * [RecipeArtworkImageLayer.TYPOGRAPHIC] (0 lecturas de cache/semilla/generador). Con `true`,
 * [RecipeArtwork] aporta la **capa de fondo** (imagen + `scrim` + crossfade) y la **unica** diferencia es
 * esa capa (la categoria y el titulo pasan al tratamiento sobre imagen y la marca de agua desaparece).
 * La geometria es **la misma** con imagen o sin ella: no hay salto de layout.
 *
 * @param wide `true` para la tarjeta ancha de Resultados (banda a tres filas, titulo a 26sp y
 *   descripcion debajo de la banda).
 * @param compact `true` para la celda de la rejilla de Favoritos (dos columnas): titulo a 16sp,
 *   ingredientes a 10,5sp y **sin** fila de metadatos (contenido aceptado de la celda compacta). La
 *   banda es siempre de **una linea**; manda sobre [wide].
 * @param description texto opcional que se pinta **debajo de la banda** dentro de la misma tarjeta (la
 *   descripcion de una linea de Resultados); `null` en la tarjeta estrecha.
 * @param overlay contenido superpuesto a la tarjeta (el corazon de la rejilla de Favoritos).
 * @param imageBackground `true` con el ajuste "Generar imagenes del plato" encendido: la imagen generada
 *   es la **capa de fondo** del hueco. Apagado (default) la estructura es identica, sin capa de fondo.
 */
@Composable
fun RecipeFicha(
    recipe: Recipe,
    taxonomy: Taxonomy,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    imageBackground: Boolean = false,
) {
    val card = LocalSazonCardColors.current
    // La variante oscura es la que aporta limite propio (boundaryWidth > 0); en claro la tarjeta es la
    // `surface` blanca sin borde, asi que no se tine (prototipo C3: `--card-tint: none`).
    val darkCard = card.boundaryWidth > 0.dp
    val style = RecipeArtworkStyle.of(recipe)
    val surface = if (darkCard) {
        SazonCardTokens.tintedSurface(card.surface, recipeAccentColors(style.accent).first())
    } else {
        card.surface
    }

    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = SazonShapes.medium,
        color = surface,
        border = if (darkCard) BorderStroke(card.boundaryWidth, card.boundary) else null,
        shadowElevation = if (darkCard) 0.dp else 4.dp,
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth()) {
                Hole(
                    recipe = recipe,
                    taxonomy = taxonomy,
                    style = style,
                    wide = wide,
                    compact = compact,
                    imageBackground = imageBackground,
                )
                SolidBand(
                    recipe = recipe,
                    taxonomy = taxonomy,
                    wide = wide,
                    compact = compact,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = FichaTypography.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = NARROW_PADDING,
                                end = NARROW_PADDING,
                                top = 10.dp,
                                bottom = NARROW_PADDING,
                            ),
                    )
                }
            }
            overlay?.invoke(this)
        }
    }
}

/**
 * Hueco **16:9** de la estructura **L2 unificada**: la **ficha tipografica** (categoria + titulo, y la
 * marca de agua del glifo) es la capa base y el **filete** va encima, al pie. Con [imageBackground]
 * `true` [RecipeArtwork] aporta la **capa de fondo** (imagen + `scrim` + crossfade); con `false` el
 * contenido del hueco se pinta directamente con la capa sintetica
 * [RecipeArtworkImageLayer.TYPOGRAPHIC], **sin** componer [RecipeArtwork].
 */
@Composable
private fun Hole(
    recipe: Recipe,
    taxonomy: Taxonomy,
    style: RecipeStyle,
    wide: Boolean,
    compact: Boolean,
    imageBackground: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(DishImageArtworkOverlay.HOLE_RATIO),
    ) {
        if (imageBackground) {
            RecipeArtwork(
                recipe = recipe,
                modifier = Modifier.matchParentSize(),
            ) { layer ->
                HoleHeading(
                    recipe = recipe,
                    taxonomy = taxonomy,
                    style = style,
                    wide = wide,
                    compact = compact,
                    layer = layer,
                )
            }
        } else {
            HoleHeading(
                recipe = recipe,
                taxonomy = taxonomy,
                style = style,
                wide = wide,
                compact = compact,
                layer = RecipeArtworkImageLayer.TYPOGRAPHIC,
            )
        }
        AccentFillet(accent = style.accent, modifier = Modifier.align(Alignment.BottomStart))
    }
}

/**
 * Contenido del hueco para la **capa resuelta** [layer]: la **marca de agua** del glifo solo sin imagen
 * ([DishImageArtworkOverlay.watermarkVisible]) y la categoria y el titulo con el tratamiento sobre imagen
 * o sobre superficie ([DishImageArtworkOverlay.headingOnImage]).
 */
@Composable
private fun HoleHeading(
    recipe: Recipe,
    taxonomy: Taxonomy,
    style: RecipeStyle,
    wide: Boolean,
    compact: Boolean,
    layer: RecipeArtworkImageLayer,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
        if (DishImageArtworkOverlay.watermarkVisible(layer)) {
            RecipeGlyphArtwork(
                glyph = style.glyph,
                tint = scheme.onSurfaceVariant.copy(alpha = WATERMARK_ALPHA),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = WATERMARK_OFFSET_X, y = -WATERMARK_OFFSET_Y)
                    .size(WATERMARK_SIZE),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (wide) WIDE_PADDING else NARROW_PADDING),
        ) {
            FichaHeading(
                category = RecipeCardFormat.categoryLine(taxonomy, recipe),
                title = recipe.title,
                wide = wide,
                compact = compact,
                onImage = DishImageArtworkOverlay.headingOnImage(layer),
            )
        }
    }
}

/**
 * **Banda solida** de la estructura L2, **siempre** debajo del hueco: la superficie de la tarjeta con
 * los ingredientes con cantidad, repartidos por [DishImageArtworkOverlay.bandLayout] (una linea si
 * [FichaBandLayout.ONE_LINE]; tres filas con cifras tabulares si [FichaBandLayout.THREE_ROWS]), y la
 * fila de metadatos con la coincidencia (la celda [compact] no la pinta).
 */
@Composable
private fun SolidBand(
    recipe: Recipe,
    taxonomy: Taxonomy,
    wide: Boolean,
    compact: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = NARROW_PADDING,
                end = NARROW_PADDING,
                top = BAND_PADDING_TOP,
                bottom = NARROW_PADDING,
            ),
    ) {
        when (DishImageArtworkOverlay.bandLayout(wide, compact)) {
            FichaBandLayout.THREE_ROWS ->
                FichaIngredients(recipe = recipe, wide = true, compact = false)

            FichaBandLayout.ONE_LINE ->
                FichaIngredients(recipe = recipe, wide = false, compact = compact)
        }
        if (!compact) {
            FichaFoot(taxonomy = taxonomy, recipe = recipe, compactMatch = !wide)
        }
    }
}

/** Filete de 6px con el degradado de la cocina, sobre el borde inferior del hueco. */
@Composable
private fun AccentFillet(accent: RecipeAccent, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FILLET_HEIGHT)
            .background(Brush.horizontalGradient(recipeAccentColors(accent))),
    )
}

/** Categoria en versalitas + titulo de la ficha. */
@Composable
private fun FichaHeading(
    category: String,
    title: String,
    wide: Boolean,
    compact: Boolean,
    onImage: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val categoryColor = if (onImage) CardImageOverlay.onImageVariant else scheme.onSurfaceVariant
    val titleColor = if (onImage) CardImageOverlay.onImageText else scheme.onSurface
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = category.uppercase(),
            style = FichaTypography.category,
            color = categoryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            style = when {
                wide -> FichaTypography.titleWide
                compact -> FichaTypography.titleCompact
                else -> FichaTypography.titleNarrow
            },
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Los tres ingredientes principales: una linea (`·`) o tres filas con cantidad tabular. */
@Composable
private fun FichaIngredients(recipe: Recipe, wide: Boolean, compact: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val main = RecipeCardFormat.mainIngredients(recipe)
    if (main.isEmpty()) return

    if (!wide) {
        Text(
            text = RecipeCardFormat.ingredientsLine(recipe),
            style = if (compact) {
                FichaTypography.ingredientsLineCompact
            } else {
                FichaTypography.ingredientsLine
            },
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(INGREDIENT_ROW_GAP),
    ) {
        main.forEach { ingredient ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = ingredient.name,
                    style = FichaTypography.ingredientName,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(SazonSpacing.sm))
                Text(
                    text = ingredient.quantityLabel,
                    style = FichaTypography.ingredientQuantity,
                    color = scheme.onSurface,
                )
            }
            Hairline()
        }
    }
}

/** Fila de metadatos (cifras tabulares) y coincidencia en terracota. */
@Composable
private fun FichaFoot(
    taxonomy: Taxonomy,
    recipe: Recipe,
    compactMatch: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Hairline()
        Spacer(modifier = Modifier.height(FOOT_PADDING_TOP))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = RecipeCardFormat.metaLine(taxonomy, recipe),
                style = FichaTypography.meta,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            RecipeCardFormat.matchBadge(recipe.matchScore, compact = compactMatch)?.let { badge ->
                Spacer(modifier = Modifier.width(SazonSpacing.sm))
                Text(
                    text = badge,
                    style = FichaTypography.match,
                    color = scheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Separador de 1 dp con el `outline` del tema (las hairlines de `DESIGN.md`). */
@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HAIRLINE)
            .background(MaterialTheme.colorScheme.outline),
    )
}

/**
 * Estilos de la ficha, construidos **localmente** desde [SazonFontFamilies] y los valores del
 * prototipo C3 / `DESIGN.md`. **No** se anaden roles a `SazonTypography`: la escala compartida del
 * tema no cambia (la ficha no puede caer a la tipografia por defecto de Material).
 */
private object FichaTypography {

    val category = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.14.em,
    )

    val titleNarrow = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02f).em,
    )

    val titleWide = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.02f).em,
    )

    /** Titulo de la celda de la rejilla de Favoritos (prototipo C3: `16px`). */
    val titleCompact = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.02f).em,
    )

    val ingredientsLine = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
    )

    /** Linea de ingredientes de la celda de la rejilla de Favoritos (prototipo C3: `10,5px`). */
    val ingredientsLineCompact = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
    )

    val ingredientName = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )

    val ingredientQuantity = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )

    val meta = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )

    val match = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    )

    val description = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )
}
