package com.sazon.app.ui.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Tarjeta de receta del carrusel "Sugeridas para ti" y del bloque "Tus recetas" de la Home.
 *
 * Es la **ficha tipografica** de [RecipeFicha] en su variante **estrecha** (ADR 0006, estructura **L2
 * unificada**): categoria en versalitas, titulo en Manrope a 2 lineas, los **tres ingredientes
 * principales en una linea**, la fila de metadatos con el porcentaje de coincidencia en terracota, el
 * **filete de 6px** con el degradado de la cocina y la **marca de agua** del glifo de categoria; todo
 * dentro del hueco **16:9** y la banda solida. La geometria la aporta [RecipeFicha]. Con el ajuste de
 * imagenes apagado (default) la tarjeta es esa misma estructura **sin** capa de fondo; con
 * [imageBackground] `true` la imagen generada es la capa de fondo del hueco (misma geometria). El ancho
 * del carrusel (220dp) lo fija este llamante. El click opcional abre el Detalle (`recipe-detail-screen`).
 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    taxonomy: Taxonomy,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    imageBackground: Boolean = false,
) {
    RecipeFicha(
        recipe = recipe,
        taxonomy = taxonomy,
        modifier = modifier.width(CARD_WIDTH),
        wide = false,
        onClick = onClick,
        imageBackground = imageBackground,
    )
}

/** Ancho de la tarjeta del carrusel de la Home (16:9 => alto ~123,75dp). */
private val CARD_WIDTH = 220.dp
