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
import com.sazon.app.ui.artwork.RecipeAccent
import com.sazon.app.ui.artwork.RecipeArtwork
import com.sazon.app.ui.artwork.RecipeArtworkStyle
import com.sazon.app.ui.artwork.RecipeGlyphArtwork
import com.sazon.app.ui.artwork.RecipeStyle
import com.sazon.app.ui.artwork.recipeAccentColors
import com.sazon.app.ui.theme.CardImageOverlay
import com.sazon.app.ui.theme.LocalSazonCardColors
import com.sazon.app.ui.theme.SazonCardTokens
import com.sazon.app.ui.theme.SazonComponents
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
 * Con [imageBackground] `false` (por defecto) la ficha es **exactamente** la aceptada: dentro del hueco
 * **16:10** (`SazonComponents.recipeCardImageRatio`), sin ninguna imagen, la **categoria en versalitas**
 * y el **titulo** en Manrope Bold, los **tres ingredientes principales** (una linea con `·` si
 * `wide == false`, o tres filas con la cantidad a la derecha en cifras tabulares si `wide == true`), la
 * fila `tiempo · dificultad · comensales` con la coincidencia en terracota (salvo en [compact]) y el
 * **filete de 6px** con el degradado de la cocina. El cromo sale de [LocalSazonCardColors].
 *
 * Con [imageBackground] `true` la tarjeta usa la estructura **L2** del ADR 0006 (imagen generada como
 * capa de fondo): el hueco pasa a **16:9** ([DishImageArtworkOverlay.HOLE_RATIO]) con la **capa de
 * fondo** ([RecipeArtwork]) y, debajo, una **banda solida** con los ingredientes con cantidad y los
 * metadatos. La estructura es **la misma** con imagen o sin ella (solo cambia el alfa de la capa de
 * fondo), asi que no hay salto de layout. La marca de agua y el tratamiento tipografico (sobre imagen o
 * sobre superficie) los decide la **capa resuelta**.
 *
 * @param wide `true` para la tarjeta ancha de Resultados (tres filas, titulo a 26sp y descripcion
 *   debajo del hueco).
 * @param compact `true` para la celda de la rejilla de Favoritos (dos columnas): titulo a 16sp,
 *   ingredientes a 10,5sp y **sin** fila de metadatos, la unica combinacion que cabe en una celda de
 *   ~172dp sin subir la tarjeta a 3:2 (OQ6, abierta). Nunca junto con [wide].
 * @param description texto opcional que se pinta **debajo del hueco** dentro de la misma tarjeta (la
 *   descripcion de una linea de Resultados); `null` en la tarjeta estrecha.
 * @param overlay contenido superpuesto a la tarjeta (el corazon de la rejilla de Favoritos).
 * @param imageBackground `true` con el ajuste "Generar imagenes del plato" encendido: la imagen
 *   generada es la capa de fondo del hueco y los ingredientes pasan a la banda solida (L2).
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
                if (imageBackground) {
                    ImageBackgroundHole(
                        recipe = recipe,
                        taxonomy = taxonomy,
                        style = style,
                        wide = wide,
                        compact = compact,
                    )
                    SolidBand(
                        recipe = recipe,
                        taxonomy = taxonomy,
                        wide = wide,
                        compact = compact,
                    )
                } else {
                    TypographicHole(
                        recipe = recipe,
                        taxonomy = taxonomy,
                        style = style,
                        wide = wide,
                        compact = compact,
                    )
                }
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
 * Hueco **16:10** de la ficha sin el ajuste de imagenes (comportamiento aceptado): marca de agua del
 * glifo al 12 %, contenido completo dentro del hueco y filete al pie. **Sin** `RecipeArtwork`.
 */
@Composable
private fun TypographicHole(
    recipe: Recipe,
    taxonomy: Taxonomy,
    style: RecipeStyle,
    wide: Boolean,
    compact: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SazonComponents.recipeCardImageRatio),
    ) {
        RecipeGlyphArtwork(
            glyph = style.glyph,
            tint = scheme.onSurfaceVariant.copy(alpha = WATERMARK_ALPHA),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = WATERMARK_OFFSET_X, y = -WATERMARK_OFFSET_Y)
                .size(WATERMARK_SIZE),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (wide) WIDE_PADDING else NARROW_PADDING),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            FichaHeading(
                category = RecipeCardFormat.categoryLine(taxonomy, recipe),
                title = recipe.title,
                wide = wide,
                compact = compact,
                onImage = false,
            )
            FichaIngredients(recipe = recipe, wide = wide, compact = compact)
            if (!compact) {
                FichaFoot(
                    taxonomy = taxonomy,
                    recipe = recipe,
                    compactMatch = !wide,
                )
            }
        }
        AccentFillet(accent = style.accent, modifier = Modifier.align(Alignment.BottomStart))
    }
}

/**
 * Hueco **16:9** de la variante **L2** (imagen como capa de fondo): la **ficha tipografica** es la capa
 * base y [RecipeArtwork] aporta la imagen con su `scrim` y el fundido; el contenido del slot pinta la
 * marca de agua (solo sin imagen) y la categoria y el titulo con el tratamiento de la capa resuelta. El
 * filete va encima, al pie del hueco.
 */
@Composable
private fun ImageBackgroundHole(
    recipe: Recipe,
    taxonomy: Taxonomy,
    style: RecipeStyle,
    wide: Boolean,
    compact: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(DishImageArtworkOverlay.HOLE_RATIO),
    ) {
        RecipeArtwork(
            recipe = recipe,
            modifier = Modifier.matchParentSize(),
        ) { layer ->
            val onImage = DishImageArtworkOverlay.headingOnImage(layer)
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
                        onImage = onImage,
                    )
                }
            }
        }
        AccentFillet(accent = style.accent, modifier = Modifier.align(Alignment.BottomStart))
    }
}

/**
 * **Banda solida** de la variante L2, debajo del hueco: la superficie de la tarjeta con los
 * ingredientes con cantidad (tres filas con cifras tabulares; una linea en la celda [compact]) y la fila
 * de metadatos con la coincidencia (la celda [compact] no la pinta).
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
        if (compact) {
            FichaIngredients(recipe = recipe, wide = false, compact = true)
        } else {
            FichaIngredients(recipe = recipe, wide = true, compact = false)
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
