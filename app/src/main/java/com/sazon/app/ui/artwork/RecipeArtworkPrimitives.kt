package com.sazon.app.ui.artwork

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Primitivas de dibujo **compartidas** de la imagen de receta, extraidas de `RecipeArtwork` para que
 * la ficha tipografica (`recipe-card-typographic`, ADR 0006) pueda reutilizarlas sin duplicar el
 * glifo ni la tabla de acentos:
 *
 * - [RecipeGlyphArtwork] pinta el glifo de linea de [RecipeGlyph] (marca de agua de la ficha y ultimo
 *   recurso de la capa de fondo de `RecipeArtwork`).
 * - [recipeAccentColors] / [recipeAccentBrush] resuelven el par de **roles del tema** de cada
 *   [RecipeAccent]; el brush es el degradado del bloque y su par de colores alimenta tambien el
 *   **filete de 6px** y el **tinte** de la tarjeta oscura (con un gradiente horizontal propio).
 *
 * `ui.artwork` sigue siendo la **unica** fuente de dibujo del glifo y del degradado de acento: la
 * geometria de los glifos es identica a la aceptada en `recipe-artwork`, sin assets y sin
 * dependencias nuevas.
 */

/** Grosor del trazo de linea de todos los glifos (`DESIGN.md`: line-art fino). */
private val GLYPH_STROKE = 2.dp

/**
 * Dibuja el glifo de linea de [glyph] con trazo de [GLYPH_STROKE] normalizado al tamano del canvas
 * (mismas coordenadas relativas que el glifo de plato que reemplaza). [RecipeGlyph.PLATE] es,
 * literalmente, los dos circulos concentricos del placeholder anterior.
 */
@Composable
fun RecipeGlyphArtwork(
    glyph: RecipeGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = GLYPH_STROKE.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val minSide = size.minDimension
        val center = Offset(w / 2f, h / 2f)

        when (glyph) {
            // Plato: los dos círculos concéntricos del placeholder aceptado (y el fallback).
            RecipeGlyph.PLATE -> {
                drawCircle(tint, radius = minSide / 2f - strokeWidth, center = center, style = stroke)
                drawCircle(tint, radius = minSide * 0.28f, center = center, style = stroke)
            }

            // Tenedor con hebras.
            RecipeGlyph.PASTA -> {
                listOf(0.34f, 0.44f, 0.56f, 0.66f).forEach { x ->
                    drawLine(
                        tint,
                        Offset(x * w, 0.16f * h),
                        Offset(x * w, 0.44f * h),
                        strokeWidth,
                        stroke.cap,
                    )
                }
                drawLine(tint, Offset(0.30f * w, 0.44f * h), Offset(0.70f * w, 0.44f * h), strokeWidth, stroke.cap)
                drawLine(tint, Offset(0.50f * w, 0.44f * h), Offset(0.50f * w, 0.86f * h), strokeWidth, stroke.cap)
                drawPath(
                    path = Path().apply {
                        moveTo(0.26f * w, 0.70f * h)
                        cubicTo(0.38f * w, 0.58f * h, 0.62f * w, 0.84f * h, 0.76f * w, 0.70f * h)
                    },
                    color = tint,
                    style = stroke,
                )
            }

            // Cuenco con granos.
            RecipeGlyph.RICE -> {
                drawLine(tint, Offset(0.20f * w, 0.48f * h), Offset(0.80f * w, 0.48f * h), strokeWidth, stroke.cap)
                drawArc(
                    tint,
                    0f,
                    180f,
                    false,
                    topLeft = Offset(0.20f * w, 0.28f * h),
                    size = Size(0.60f * w, 0.40f * h),
                    style = stroke,
                )
                drawOval(tint, Offset(0.34f * w, 0.30f * h), Size(0.11f * w, 0.08f * h), style = stroke)
                drawOval(tint, Offset(0.55f * w, 0.32f * h), Size(0.11f * w, 0.08f * h), style = stroke)
                drawOval(tint, Offset(0.45f * w, 0.19f * h), Size(0.11f * w, 0.08f * h), style = stroke)
            }

            // Cuenco con cuchara y vapor.
            RecipeGlyph.SOUP_CREAM -> {
                drawLine(tint, Offset(0.18f * w, 0.54f * h), Offset(0.82f * w, 0.54f * h), strokeWidth, stroke.cap)
                drawArc(
                    tint,
                    0f,
                    180f,
                    false,
                    topLeft = Offset(0.18f * w, 0.34f * h),
                    size = Size(0.64f * w, 0.40f * h),
                    style = stroke,
                )
                drawLine(tint, Offset(0.62f * w, 0.52f * h), Offset(0.84f * w, 0.22f * h), strokeWidth, stroke.cap)
                drawCircle(tint, radius = 0.07f * w, center = Offset(0.86f * w, 0.19f * h), style = stroke)
                drawPath(
                    path = Path().apply {
                        moveTo(0.34f * w, 0.46f * h)
                        cubicTo(0.26f * w, 0.36f * h, 0.42f * w, 0.30f * h, 0.34f * w, 0.18f * h)
                    },
                    color = tint,
                    style = stroke,
                )
                drawPath(
                    path = Path().apply {
                        moveTo(0.48f * w, 0.46f * h)
                        cubicTo(0.40f * w, 0.36f * h, 0.56f * w, 0.30f * h, 0.48f * w, 0.18f * h)
                    },
                    color = tint,
                    style = stroke,
                )
            }

            // Cuenco con hojas.
            RecipeGlyph.SALAD -> {
                drawLine(tint, Offset(0.18f * w, 0.56f * h), Offset(0.82f * w, 0.56f * h), strokeWidth, stroke.cap)
                drawArc(
                    tint,
                    0f,
                    180f,
                    false,
                    topLeft = Offset(0.18f * w, 0.36f * h),
                    size = Size(0.64f * w, 0.42f * h),
                    style = stroke,
                )
                drawLeaf(tint, Offset(0.50f * w, 0.56f * h), Offset(0.26f * w, 0.26f * h), 0.09f * w, stroke)
                drawLeaf(tint, Offset(0.50f * w, 0.56f * h), Offset(0.74f * w, 0.24f * h), 0.09f * w, stroke)
                drawLeaf(tint, Offset(0.50f * w, 0.56f * h), Offset(0.52f * w, 0.16f * h), 0.07f * w, stroke)
            }

            // Chuleta (óvalo con hueso).
            RecipeGlyph.MEAT -> {
                drawOval(tint, Offset(0.16f * w, 0.28f * h), Size(0.52f * w, 0.44f * h), style = stroke)
                drawLine(tint, Offset(0.60f * w, 0.62f * h), Offset(0.82f * w, 0.42f * h), strokeWidth, stroke.cap)
                drawCircle(tint, radius = 0.07f * w, center = Offset(0.84f * w, 0.38f * h), style = stroke)
            }

            // Muslo de ave.
            RecipeGlyph.POULTRY -> {
                drawCircle(tint, radius = 0.24f * w, center = Offset(0.40f * w, 0.38f * h), style = stroke)
                drawLine(tint, Offset(0.54f * w, 0.54f * h), Offset(0.76f * w, 0.76f * h), strokeWidth, stroke.cap)
                drawCircle(tint, radius = 0.06f * w, center = Offset(0.79f * w, 0.79f * h), style = stroke)
            }

            // Pez (con ojo y cola).
            RecipeGlyph.FISH_SEAFOOD -> {
                drawOval(tint, Offset(0.12f * w, 0.36f * h), Size(0.54f * w, 0.30f * h), style = stroke)
                drawPath(
                    path = Path().apply {
                        moveTo(0.64f * w, 0.51f * h)
                        lineTo(0.88f * w, 0.30f * h)
                        lineTo(0.88f * w, 0.72f * h)
                        close()
                    },
                    color = tint,
                    style = stroke,
                )
                drawCircle(tint, radius = 0.04f * w, center = Offset(0.25f * w, 0.47f * h))
            }

            // Hoja.
            RecipeGlyph.VEGETARIAN -> {
                drawLeaf(tint, Offset(0.50f * w, 0.86f * h), Offset(0.50f * w, 0.14f * h), 0.22f * w, stroke)
                drawLine(tint, Offset(0.50f * w, 0.84f * h), Offset(0.50f * w, 0.22f * h), strokeWidth, stroke.cap)
            }

            // Brote (dos hojas y tallo).
            RecipeGlyph.VEGAN -> {
                drawLine(tint, Offset(0.50f * w, 0.88f * h), Offset(0.50f * w, 0.34f * h), strokeWidth, stroke.cap)
                drawLeaf(tint, Offset(0.50f * w, 0.58f * h), Offset(0.22f * w, 0.36f * h), 0.08f * w, stroke)
                drawLeaf(tint, Offset(0.50f * w, 0.48f * h), Offset(0.78f * w, 0.26f * h), 0.08f * w, stroke)
            }

            // Olla con tapa.
            RecipeGlyph.LEGUME_STEW -> {
                drawLine(tint, Offset(0.24f * w, 0.52f * h), Offset(0.76f * w, 0.52f * h), strokeWidth, stroke.cap)
                drawArc(
                    tint,
                    0f,
                    180f,
                    false,
                    topLeft = Offset(0.24f * w, 0.30f * h),
                    size = Size(0.52f * w, 0.44f * h),
                    style = stroke,
                )
                drawLine(tint, Offset(0.14f * w, 0.54f * h), Offset(0.24f * w, 0.54f * h), strokeWidth, stroke.cap)
                drawLine(tint, Offset(0.76f * w, 0.54f * h), Offset(0.86f * w, 0.54f * h), strokeWidth, stroke.cap)
                drawOval(tint, Offset(0.26f * w, 0.45f * h), Size(0.48f * w, 0.14f * h), style = stroke)
                drawCircle(tint, radius = 0.05f * w, center = Offset(0.50f * w, 0.43f * h), style = stroke)
            }

            // Porción de pizza (cuña con corteza y dos ingredientes).
            RecipeGlyph.PIZZA_BREAD -> {
                drawPath(
                    path = Path().apply {
                        moveTo(0.50f * w, 0.14f * h)
                        lineTo(0.18f * w, 0.74f * h)
                        cubicTo(0.34f * w, 0.86f * h, 0.66f * w, 0.86f * h, 0.82f * w, 0.74f * h)
                        close()
                    },
                    color = tint,
                    style = stroke,
                )
                drawCircle(tint, radius = 0.055f * w, center = Offset(0.42f * w, 0.56f * h), style = stroke)
                drawCircle(tint, radius = 0.055f * w, center = Offset(0.60f * w, 0.64f * h), style = stroke)
            }

            // Huevo (óvalo con yema).
            RecipeGlyph.EGG_BREAKFAST -> {
                drawOval(tint, Offset(0.22f * w, 0.14f * h), Size(0.56f * w, 0.72f * h), style = stroke)
                drawCircle(tint, radius = 0.13f * w, center = Offset(0.50f * w, 0.46f * h), style = stroke)
            }

            // Sándwich triangular con relleno y palillo.
            RecipeGlyph.SANDWICH_WRAP -> {
                drawPath(
                    path = Path().apply {
                        moveTo(0.50f * w, 0.18f * h)
                        lineTo(0.18f * w, 0.80f * h)
                        lineTo(0.82f * w, 0.80f * h)
                        close()
                    },
                    color = tint,
                    style = stroke,
                )
                drawLine(tint, Offset(0.30f * w, 0.64f * h), Offset(0.70f * w, 0.64f * h), strokeWidth, stroke.cap)
                drawLine(tint, Offset(0.50f * w, 0.06f * h), Offset(0.50f * w, 0.20f * h), strokeWidth, stroke.cap)
            }

            // Platito con pincho y aceitunas.
            RecipeGlyph.TAPAS_APPETIZER -> {
                drawOval(tint, Offset(0.12f * w, 0.62f * h), Size(0.68f * w, 0.16f * h), style = stroke)
                drawLine(tint, Offset(0.46f * w, 0.62f * h), Offset(0.62f * w, 0.20f * h), strokeWidth, stroke.cap)
                drawCircle(tint, radius = 0.09f * w, center = Offset(0.63f * w, 0.18f * h), style = stroke)
                drawCircle(tint, radius = 0.07f * w, center = Offset(0.55f * w, 0.40f * h), style = stroke)
            }

            // Cupcake (molde, crema y cereza).
            RecipeGlyph.DESSERT -> {
                drawPath(
                    path = Path().apply {
                        moveTo(0.28f * w, 0.54f * h)
                        lineTo(0.72f * w, 0.54f * h)
                        lineTo(0.64f * w, 0.86f * h)
                        lineTo(0.36f * w, 0.86f * h)
                        close()
                    },
                    color = tint,
                    style = stroke,
                )
                drawPath(
                    path = Path().apply {
                        moveTo(0.28f * w, 0.54f * h)
                        cubicTo(0.26f * w, 0.34f * h, 0.44f * w, 0.30f * h, 0.50f * w, 0.44f * h)
                        cubicTo(0.56f * w, 0.30f * h, 0.74f * w, 0.34f * h, 0.72f * w, 0.54f * h)
                    },
                    color = tint,
                    style = stroke,
                )
                drawCircle(tint, radius = 0.06f * w, center = Offset(0.50f * w, 0.24f * h), style = stroke)
            }

            // Vaso con pajita.
            RecipeGlyph.SMOOTHIE_DRINK -> {
                drawPath(
                    path = Path().apply {
                        moveTo(0.30f * w, 0.26f * h)
                        lineTo(0.70f * w, 0.26f * h)
                        lineTo(0.62f * w, 0.86f * h)
                        lineTo(0.38f * w, 0.86f * h)
                        close()
                    },
                    color = tint,
                    style = stroke,
                )
                drawLine(tint, Offset(0.34f * w, 0.44f * h), Offset(0.66f * w, 0.44f * h), strokeWidth, stroke.cap)
                drawLine(tint, Offset(0.56f * w, 0.32f * h), Offset(0.68f * w, 0.10f * h), strokeWidth, stroke.cap)
            }

            // Fuente de horno con asas.
            RecipeGlyph.CASSEROLE_OVEN -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.20f * w, 0.34f * h),
                    size = Size(0.60f * w, 0.42f * h),
                    cornerRadius = CornerRadius(0.08f * w, 0.08f * h),
                    style = stroke,
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.04f * w, 0.46f * h),
                    size = Size(0.14f * w, 0.18f * h),
                    cornerRadius = CornerRadius(0.05f * w, 0.05f * h),
                    style = stroke,
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.82f * w, 0.46f * h),
                    size = Size(0.14f * w, 0.18f * h),
                    cornerRadius = CornerRadius(0.05f * w, 0.05f * h),
                    style = stroke,
                )
            }
        }
    }
}

/**
 * Par de colores (`start` arriba → `end` abajo) del degradado del bloque, resuelto con **roles del
 * tema** para cada [RecipeAccent]. [RecipeAccent.DEFAULT] reproduce el degradado
 * `primaryContainer` → `secondaryContainer` que la app usaba antes de esta feature.
 *
 * Es la **unica** tabla acento → color: [recipeAccentBrush] (degradado del bloque) y la ficha
 * tipografica (filete de 6px y tinte de la tarjeta oscura) la consumen.
 */
@Composable
fun recipeAccentColors(accent: RecipeAccent): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return when (accent) {
        RecipeAccent.EMBER -> listOf(scheme.surfaceVariant, scheme.primaryContainer)
        RecipeAccent.GROVE -> listOf(scheme.secondaryContainer, scheme.primaryContainer)
        RecipeAccent.TOMATO -> listOf(scheme.primaryContainer, scheme.tertiaryContainer)
        RecipeAccent.GINGER -> listOf(scheme.tertiaryContainer, scheme.primaryContainer)
        RecipeAccent.SESAME -> listOf(scheme.surfaceVariant, scheme.secondaryContainer)
        RecipeAccent.CURRY -> listOf(scheme.tertiaryContainer, scheme.secondaryContainer)
        RecipeAccent.CHILI -> listOf(scheme.primaryContainer, scheme.surfaceVariant)
        RecipeAccent.HONEY -> listOf(scheme.tertiaryContainer, scheme.surfaceVariant)
        RecipeAccent.CLAY -> listOf(scheme.secondaryContainer, scheme.surfaceVariant)
        RecipeAccent.ROSE -> listOf(scheme.secondaryContainer, scheme.tertiaryContainer)
        RecipeAccent.DEFAULT -> listOf(scheme.primaryContainer, scheme.secondaryContainer)
    }
}

/**
 * Degradado del bloque de la imagen de receta para [accent]: el **mismo** brush que pintaba
 * `RecipeArtwork` antes de extraerse (mismo orden `start` → `end`), de modo que su comportamiento no
 * cambia.
 */
@Composable
fun recipeAccentBrush(accent: RecipeAccent): Brush = Brush.linearGradient(recipeAccentColors(accent))

/**
 * Hoja de dos arcos simétricos de `base` a `tip` con un abultamiento `bulge` a cada lado. Se usa en
 * los glifos de hoja/brote/ensalada, que Compose no trae como primitiva.
 */
private fun DrawScope.drawLeaf(
    tint: Color,
    base: Offset,
    tip: Offset,
    bulge: Float,
    stroke: Stroke,
) {
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
    val nx = dy / length
    val ny = -dx / length
    val path = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(
            base.x + dx * 0.25f + nx * bulge,
            base.y + dy * 0.25f + ny * bulge,
            base.x + dx * 0.75f + nx * bulge,
            base.y + dy * 0.75f + ny * bulge,
            tip.x,
            tip.y,
        )
        cubicTo(
            base.x + dx * 0.75f - nx * bulge,
            base.y + dy * 0.75f - ny * bulge,
            base.x + dx * 0.25f - nx * bulge,
            base.y + dy * 0.25f - ny * bulge,
            base.x,
            base.y,
        )
        close()
    }
    drawPath(path = path, color = tint, style = stroke)
}
