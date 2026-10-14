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
 * Es la **ficha tipografica** de [RecipeFicha] en su variante **estrecha** (ADR 0006): categoria en
 * versalitas, titulo en Manrope a 2 lineas, los **tres ingredientes principales en una linea**, la fila
 * de metadatos con el porcentaje de coincidencia en terracota, el **filete de 6px** con el degradado de
 * la cocina y la **marca de agua** del glifo de categoria. **Sin ninguna imagen**: la geometria aceptada
 * la aporta el hueco 16:10 de la ficha. El ancho del carrusel (220dp) lo fija este llamante. El click
 * opcional abre el Detalle (`recipe-detail-screen`).
 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    taxonomy: Taxonomy,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    RecipeFicha(
        recipe = recipe,
        taxonomy = taxonomy,
        modifier = modifier.width(CARD_WIDTH),
        wide = false,
        onClick = onClick,
    )
}

/** Ancho de la tarjeta del carrusel de la Home (16:10 => alto ~137,5dp). */
private val CARD_WIDTH = 220.dp
