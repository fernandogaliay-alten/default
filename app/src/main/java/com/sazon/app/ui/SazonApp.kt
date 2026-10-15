package com.sazon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.sazon.app.data.NetworkStatusSource
import com.sazon.app.data.image.DishImageSettings
import com.sazon.app.domain.activity.ActivitySummaryRepository
import com.sazon.app.domain.favorites.FavoriteRepository
import com.sazon.app.domain.favorites.FavoriteService
import com.sazon.app.domain.generation.GenerationService
import com.sazon.app.domain.profile.ProfileRepository
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.search.AiSearchService
import com.sazon.app.domain.suggestions.SuggestionBatchRepository
import com.sazon.app.domain.suggestions.SuggestionBatchService
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.cooking.CookingAlarm
import com.sazon.app.ui.cooking.NoOpCookingAlarm
import com.sazon.app.ui.onboarding.OnboardingFlow
import kotlinx.coroutines.launch

/**
 * Raiz de la app y puerta de primer arranque.
 *
 * Observa el **Perfil completo** ([ProfileRepository.observe]) y distingue la carga de la ausencia de
 * Perfil: mientras no ha emitido ([ProfileObservation.Loading]) pinta solo el fondo del tema; tras
 * emitir decide con [AppGate]. Sin Perfil monta el Onboarding obligatorio; con Perfil, el armazon
 * principal ([MainShell]) ya con la Taxonomia y las dependencias del Lote compuestas en `MainActivity`
 * (sin DI).
 *
 * La costura `onFinished` del onboarding persiste el Perfil con [ProfileRepository.save]: como se
 * observa `observe()`, tras el `save` llega el Perfil y `AppGate.decide` pasa a `MAIN`.
 */
@Composable
fun SazonApp(
    profileRepository: ProfileRepository,
    taxonomy: Taxonomy,
    suggestionBatchRepository: SuggestionBatchRepository,
    suggestionBatchService: SuggestionBatchService,
    generationService: GenerationService,
    favoriteRepository: FavoriteRepository,
    favoriteService: FavoriteService,
    activitySummaryRepository: ActivitySummaryRepository,
    aiSearchService: AiSearchService,
    networkStatusSource: NetworkStatusSource,
    dishImageSettings: DishImageSettings,
    seedRecipes: List<Recipe>,
    modifier: Modifier = Modifier,
    cookingAlarm: CookingAlarm = NoOpCookingAlarm,
) {
    val state by produceState<ProfileObservation>(
        initialValue = ProfileObservation.Loading,
        profileRepository,
    ) {
        profileRepository.observe().collect { profile ->
            value = ProfileObservation.Loaded(profile)
        }
    }

    when (val current = state) {
        ProfileObservation.Loading -> SazonBackground(modifier = modifier)

        is ProfileObservation.Loaded -> when (AppGate.decide(current.profile)) {
            AppDestination.ONBOARDING -> {
                val scope = rememberCoroutineScope()
                OnboardingFlow(
                    taxonomy = taxonomy,
                    onFinished = { profile -> scope.launch { profileRepository.save(profile) } },
                    modifier = modifier,
                )
            }

            AppDestination.MAIN -> {
                val scope = rememberCoroutineScope()
                MainShell(
                    profile = checkNotNull(current.profile) { "MAIN exige un Perfil" },
                    taxonomy = taxonomy,
                    suggestionBatchRepository = suggestionBatchRepository,
                    suggestionBatchService = suggestionBatchService,
                    generationService = generationService,
                    favoriteRepository = favoriteRepository,
                    favoriteService = favoriteService,
                    activitySummaryRepository = activitySummaryRepository,
                    aiSearchService = aiSearchService,
                    networkStatusSource = networkStatusSource,
                    dishImageSettings = dishImageSettings,
                    seedRecipes = seedRecipes,
                    onSaveProfile = { updated -> scope.launch { profileRepository.save(updated) } },
                    modifier = modifier,
                    cookingAlarm = cookingAlarm,
                )
            }
        }
    }
}

/** Estado de la observacion del Perfil: distingue "aun cargando" de "sin Perfil". */
private sealed interface ProfileObservation {
    data object Loading : ProfileObservation
    data class Loaded(val profile: UserProfile?) : ProfileObservation
}

@Composable
private fun SazonBackground(modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}
