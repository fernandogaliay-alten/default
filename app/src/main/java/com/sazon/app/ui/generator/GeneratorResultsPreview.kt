package com.sazon.app.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.home.RecipeCardFormat
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Preview **minimo** de resultados (seam), no la pantalla de Resultados real (`results-screen`, que es
 * duena del destino, las imagenes, el orden, el `matchScore` y los estados vacio/error completos).
 *
 * Capa Compose delgada sobre el [GenerationResult] puro: pinta el titular "Estas son tus recetas" y
 * **solo** las recetas de `accepted` (titulo + `RecipeCardFormat.metaLine`); si `aiUnavailable`, una
 * linea de aviso en ambar discreto (`tertiaryContainer`, nunca rojo); si `accepted` esta vacio, una
 * linea de estado vacio. Acciones: "Cambiar ingredientes" (vuelve a los ajustes, [onChangeIngredients])
 * y "Atras" ([onBack], cierra el Generador). Sin imagenes, sin `matchScore` y sin orden.
 */
@Composable
fun GeneratorResultsPreview(
    result: GenerationResult,
    taxonomy: Taxonomy,
    onBack: () -> Unit,
    onChangeIngredients: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SazonSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
    ) {
        Text(
            text = "Estas son tus recetas",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        when {
            result.aiUnavailable -> AiUnavailableNotice()

            result.accepted.isEmpty() -> Text(
                text = "No hemos encontrado recetas con esos ingredientes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> result.accepted.forEach { recipe ->
                AcceptedRecipeRow(recipe = recipe, taxonomy = taxonomy)
            }
        }

        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        TextButton(onClick = onChangeIngredients) {
            Text(text = "Cambiar ingredientes")
        }
        TextButton(onClick = onBack) {
            Text(text = "Atrás")
        }
    }
}

/** Fila de una receta aceptada: titulo y linea de metadatos (`RecipeCardFormat.metaLine`). */
@Composable
private fun AcceptedRecipeRow(
    recipe: Recipe,
    taxonomy: Taxonomy,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = SazonShapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SazonSpacing.md)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(SazonSpacing.sm))
            Text(
                text = RecipeCardFormat.metaLine(taxonomy, recipe),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Aviso discreto de IA no disponible en ambar (`tertiaryContainer`), nunca en rojo. */
@Composable
private fun AiUnavailableNotice() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = SazonShapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Ahora mismo no podemos generar recetas nuevas. Vuelve a intentarlo más tarde.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(SazonSpacing.md),
        )
    }
}
