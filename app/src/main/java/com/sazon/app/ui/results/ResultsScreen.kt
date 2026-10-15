package com.sazon.app.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.motion.MotionAxis
import com.sazon.app.ui.motion.MotionContent
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Pantalla de Resultados real (sustituye al preview `GeneratorResultsPreview`).
 *
 * Capa Compose **delgada** sobre el estado puro [ResultsStatus] y la cabecera [ResultsHeader]: no
 * calcula ni ordena nada. `topBar` con "Atras" (texto, sin iconos) + titulo + subtitulo; `bottomBar`
 * fijo con el secundario "Cambiar ingredientes"; cuerpo por estado:
 *
 * - [ResultsStatus.Content]: lista vertical de [ResultRecipeCard] (solo recetas aceptadas).
 * - [ResultsStatus.Empty]: estado vacio ("No hemos encontrado recetas con esos ingredientes").
 * - [ResultsStatus.Unavailable] con semillas: franja ambar (`tertiaryContainer`, **nunca** rojo) con el
 *   texto de modo degradado, "Reintentar" y la lista de semillas.
 * - [ResultsStatus.Unavailable] sin semillas: titular de error con "Reintentar".
 *
 * "Cambiar ingredientes" (vuelve al paso de ingredientes del Generador) vive en la barra inferior; el
 * estado de error/modo degradado ofrece ademas "Reintentar" (relanza la generacion con el mismo
 * borrador). La pantalla es **reutilizable** por `ai-search` (`ResultsOrigin.Search`).
 */
@Composable
fun ResultsScreen(
    status: ResultsStatus,
    header: ResultsHeader,
    taxonomy: Taxonomy,
    onBack: () -> Unit,
    onChangeIngredients: () -> Unit,
    onRetry: () -> Unit,
    onOpenRecipe: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
    imageBackground: Boolean = false,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ResultsHeaderBar(header = header, onBack = onBack) },
        bottomBar = { ResultsBottomBar(onChangeIngredients = onChangeIngredients) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // El cuerpo por estado se revela con fundido y subida de 24 dp (clave local por estado).
            // La clave es el propio `ResultsStatus` para que la capa saliente conserve sus datos.
            MotionContent(
                targetState = status,
                axis = MotionAxis.VERTICAL,
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
                    is ResultsStatus.Content -> ResultsList(
                        recipes = current.recipes,
                        taxonomy = taxonomy,
                        onOpenRecipe = onOpenRecipe,
                        imageBackground = imageBackground,
                    )

                    ResultsStatus.Empty -> EmptyState()

                    is ResultsStatus.Unavailable -> UnavailableState(
                        seedRecipes = current.seedRecipes,
                        taxonomy = taxonomy,
                        onRetry = onRetry,
                        onOpenRecipe = onOpenRecipe,
                        imageBackground = imageBackground,
                    )
                }
            }
        }
    }
}

/** Cabecera: "Atras" (texto) + titulo (`headlineLarge`) + subtitulo (`bodyLarge`). */
@Composable
private fun ResultsHeaderBar(
    header: ResultsHeader,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SazonSpacing.sm,
                end = SazonSpacing.md,
                top = SazonSpacing.sm,
                bottom = SazonSpacing.sm,
            ),
    ) {
        TextButton(onClick = onBack) {
            Text(text = "Atrás")
        }
        Text(
            text = header.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = SazonSpacing.sm),
        )
        Text(
            text = header.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SazonSpacing.sm),
        )
    }
}

/** Barra inferior anclada con la accion secundaria "Cambiar ingredientes". */
@Composable
private fun ResultsBottomBar(onChangeIngredients: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SazonSpacing.lg,
                end = SazonSpacing.lg,
                top = SazonSpacing.sm,
                bottom = SazonSpacing.md,
            ),
    ) {
        OutlinedButton(
            onClick = onChangeIngredients,
            shape = SazonPillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(SazonComponents.buttonHeight),
        ) {
            Text(text = "Cambiar ingredientes")
        }
    }
}

/** Lista vertical de tarjetas (resultados poblados o semillas en modo degradado). */
@Composable
private fun ResultsList(
    recipes: List<Recipe>,
    taxonomy: Taxonomy,
    onOpenRecipe: (Recipe) -> Unit,
    imageBackground: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SazonSpacing.lg, vertical = SazonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
    ) {
        items(recipes) { recipe ->
            ResultRecipeCard(
                recipe = recipe,
                taxonomy = taxonomy,
                onClick = { onOpenRecipe(recipe) },
                imageBackground = imageBackground,
            )
        }
    }
}

/** Estado vacio: sin recetas aceptadas ni fallo. La accion "Cambiar ingredientes" es la barra inferior. */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SazonSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No hemos encontrado recetas con esos ingredientes",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        Text(
            text = "Prueba a cambiar los ingredientes y vuelve a generar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Estado de error/modo degradado: franja ambar + semillas, o titular de error; ambos con "Reintentar". */
@Composable
private fun UnavailableState(
    seedRecipes: List<Recipe>,
    taxonomy: Taxonomy,
    onRetry: () -> Unit,
    onOpenRecipe: (Recipe) -> Unit,
    imageBackground: Boolean,
) {
    if (seedRecipes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SazonSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No hemos podido generar las recetas. Revisa tu conexión.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(SazonSpacing.md))
            RetryButton(onRetry = onRetry)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = SazonSpacing.lg, vertical = SazonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.md),
        ) {
            item { DegradedBanner() }
            item { RetryButton(onRetry = onRetry) }
            items(seedRecipes) { recipe ->
                ResultRecipeCard(
                    recipe = recipe,
                    taxonomy = taxonomy,
                    onClick = { onOpenRecipe(recipe) },
                    imageBackground = imageBackground,
                )
            }
        }
    }
}

/** Franja ambar discreta de modo degradado (`tertiaryContainer`), nunca en rojo. */
@Composable
private fun DegradedBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = SazonShapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Ahora mismo no podemos generar recetas nuevas. Aquí tienes nuestras sugerencias.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(SazonSpacing.md),
        )
    }
}

/** Accion primaria "Reintentar" (relanza la generacion con el mismo borrador). */
@Composable
private fun RetryButton(onRetry: () -> Unit) {
    Button(
        onClick = onRetry,
        shape = SazonPillShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(SazonComponents.buttonHeight),
    ) {
        Text(text = "Reintentar")
    }
}
