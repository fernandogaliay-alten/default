package com.sazon.app.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.search.AiSearchService
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.generator.GeneratorGeneratingScreen
import com.sazon.app.ui.results.ResultsScreen
import kotlinx.coroutines.delay

/** Duracion minima visible del estado de carga para que "Cancelar busqueda" sea observable (UX). */
private const val MIN_VISIBLE_MILLIS = 900L

/**
 * Huesped de la pagina empujada [com.sazon.app.ui.navigation.PushedScreen.SEARCH]: la Busqueda con IA.
 *
 * Arranca la busqueda al montarse con `LaunchedEffect(query, searchId)` y respeta la duracion minima
 * visible (~900 ms, paridad con el Generador). El pintado va por *stage* (puro, decidido por
 * [AiSearchOutcome]):
 *
 * - **carga** -> reutiliza la animacion del Generador ([GeneratorGeneratingScreen]) con el microcopy de
 *   la busqueda y "Cancelar busqueda", que vuelve a la Home con la consulta preservada;
 * - **resultados** -> reutiliza la pantalla de Resultados ([ResultsScreen]) con el subtitulo de la
 *   consulta ("Resultados de tu busqueda" + la consulta entre comillas);
 * - **bloqueado / sin resultados / fallo** -> los estados delgados de [AiSearchStates].
 *
 * "Reintentar" invalida el resultado e incrementa `searchId` para relanzar la busqueda con la misma
 * consulta; el memo del servicio no guarda los fallos, asi que vuelve a llamar al generador. El
 * resultado vive en `remember` (no `rememberSaveable`): `Recipe` no es serializable trivialmente y
 * rotar vuelve al estado de carga (mismo criterio que el Generador).
 */
@Composable
fun AiSearchFlow(
    query: String,
    preferences: PreferenceProfile,
    taxonomy: Taxonomy,
    searchService: AiSearchService,
    onBack: () -> Unit,
    onOpenRecipe: (Recipe) -> Unit,
    onEditProfile: () -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    imageBackground: Boolean = false,
) {
    var result by remember { mutableStateOf<GenerationResult?>(null) }
    var searchId by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, searchId) {
        val startedAt = System.currentTimeMillis()
        val searched = searchService.search(query, preferences)
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - elapsed)
        result = searched
    }

    val retry = {
        result = null
        searchId += 1
    }

    val current = result
    when {
        current != null -> when (val stage = AiSearchOutcome.stageOf(current, query, preferences, taxonomy)) {
            is AiSearchStage.Results -> ResultsScreen(
                status = stage.status,
                header = stage.header,
                taxonomy = taxonomy,
                onBack = onBack,
                onChangeIngredients = onCreateRecipe,
                onRetry = retry,
                onOpenRecipe = onOpenRecipe,
                modifier = modifier,
                imageBackground = imageBackground,
            )

            is AiSearchStage.Blocked -> BlockedState(
                query = stage.query,
                onBack = onBack,
                onReviewProfile = onEditProfile,
                modifier = modifier,
            )

            is AiSearchStage.NoResults -> NoResultsState(
                query = stage.query,
                onBack = onBack,
                onCreateRecipe = onCreateRecipe,
                modifier = modifier,
            )

            is AiSearchStage.Failure -> FailureState(
                query = stage.query,
                onBack = onBack,
                onRetry = retry,
                modifier = modifier,
            )
        }

        else -> GeneratorGeneratingScreen(
            headline = AiSearchProgress.HEADLINE,
            subLines = AiSearchProgress.subLines(query),
            onCancel = onBack,
            modifier = modifier,
            cancelLabel = AiSearchCopy.CANCEL_LABEL,
        )
    }
}
