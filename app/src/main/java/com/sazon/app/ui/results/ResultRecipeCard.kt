package com.sazon.app.ui.results

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.home.RecipeFicha

/**
 * Tarjeta de una receta en la pantalla de Resultados (a ancho completo).
 *
 * Es la **ficha tipografica** de [RecipeFicha] en su variante **ancha** (ADR 0006): los **tres
 * ingredientes principales** se muestran en **tres filas** (nombre a la izquierda, **cantidad a la
 * derecha en cifras tabulares** y hairline entre filas), titulo a 26sp, la fila de metadatos y el
 * **porcentaje de coincidencia** en terracota. Debajo del hueco 16:10 se conserva la **descripcion**
 * (1 linea) dentro de la misma tarjeta. **Sin ninguna imagen**.
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
    RecipeFicha(
        recipe = recipe,
        taxonomy = taxonomy,
        modifier = modifier.fillMaxWidth(),
        wide = true,
        onClick = onClick,
        description = recipe.description,
    )
}
