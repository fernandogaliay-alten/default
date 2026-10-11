package com.sazon.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.sazon.app.ui.theme.SazonComponents
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Estados **delgados** de la pagina de Busqueda con IA que no son la lista de resultados: bloqueo por
 * Perfil, sin resultados y fallo. Capa Compose, sin logica: el estado lo decide [AiSearchOutcome].
 *
 * Cada uno conserva la consulta visible y ofrece la accion que toca; el bloqueo por Perfil usa una
 * tarjeta **ambar** (`tertiaryContainer`, nunca roja) con su accion secundaria "Revisar mi perfil".
 */

/** Bloqueo por Perfil: la consulta choca con la dieta o una restriccion dura. */
@Composable
fun BlockedState(
    query: String,
    onBack: () -> Unit,
    onReviewProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AiSearchStateScaffold(onBack = onBack, modifier = modifier) {
        QueryPill(query = query)
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = SazonShapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(SazonSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            ) {
                Text(
                    text = AiSearchCopy.BLOCKED_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = AiSearchCopy.BLOCKED_BODY,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        OutlinedButton(
            onClick = onReviewProfile,
            shape = SazonPillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(SazonComponents.buttonHeight),
        ) {
            Text(text = AiSearchCopy.REVIEW_PROFILE_LABEL)
        }
    }
}

/** Sin resultados: la consulta no choca con el Perfil, simplemente no hay recetas. */
@Composable
fun NoResultsState(
    query: String,
    onBack: () -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AiSearchStateScaffold(onBack = onBack, modifier = modifier) {
        Text(
            text = AiSearchCopy.NO_RESULTS_TITLE,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        QueryPill(query = query)
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        Text(
            text = AiSearchCopy.NO_RESULTS_BODY,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        Button(
            onClick = onCreateRecipe,
            shape = SazonPillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(SazonComponents.buttonHeight),
        ) {
            Text(text = AiSearchCopy.CREATE_RECIPE_LABEL)
        }
    }
}

/** Fallo de IA/red: se conserva la consulta y se ofrece "Reintentar" (nunca cae a semilla). */
@Composable
fun FailureState(
    query: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AiSearchStateScaffold(onBack = onBack, modifier = modifier) {
        QueryPill(query = query)
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        Text(
            text = AiSearchCopy.FAILURE_TITLE,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.md))
        Button(
            onClick = onRetry,
            shape = SazonPillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(SazonComponents.buttonHeight),
        ) {
            Text(text = AiSearchCopy.RETRY_LABEL)
        }
    }
}

/** Armazon comun: cabecera "Atras" (texto, sin iconos) y cuerpo centrado. */
@Composable
private fun AiSearchStateScaffold(
    onBack: () -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SazonSpacing.sm, top = SazonSpacing.sm),
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "Atrás")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(SazonSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/** Pastilla discreta con la consulta entre comillas (normalizada). */
@Composable
private fun QueryPill(query: String) {
    Surface(
        shape = SazonPillShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Text(
            text = "“${AiSearchQuery.normalize(query)}”",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SazonSpacing.md, vertical = SazonSpacing.sm),
        )
    }
}
