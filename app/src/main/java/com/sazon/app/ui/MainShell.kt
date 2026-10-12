package com.sazon.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sazon.app.data.NetworkStatusSource
import com.sazon.app.domain.activity.ActivitySummaryRepository
import com.sazon.app.domain.favorites.FavoriteRepository
import com.sazon.app.domain.favorites.FavoriteService
import com.sazon.app.domain.generation.GenerationService
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.search.AiSearchService
import com.sazon.app.domain.suggestions.SuggestionBatchRepository
import com.sazon.app.domain.suggestions.SuggestionBatchService
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.cooking.CookingModeScreen
import com.sazon.app.ui.detail.RecipeDetailScreen
import com.sazon.app.ui.favorites.FavoritesScreen
import com.sazon.app.ui.generator.GeneratorFlow
import com.sazon.app.ui.home.HomeRecipes
import com.sazon.app.ui.home.HomeScreen
import com.sazon.app.ui.home.HomeSuggestions
import com.sazon.app.ui.navigation.MainNavigationState
import com.sazon.app.ui.navigation.MainTab
import com.sazon.app.ui.navigation.PushedScreen
import com.sazon.app.ui.profile.ProfileScreen
import com.sazon.app.ui.search.AiSearchFlow
import com.sazon.app.ui.theme.SazonComponents
import kotlinx.coroutines.launch

/**
 * Armazon principal de la app tras el onboarding: barra de navegacion inferior de tres destinos
 * (Inicio · Favoritos · Perfil), FAB "＋ Crear receta" y enrutado del contenido.
 *
 * Aqui tambien se **compone** el Lote de sugerencias en la app: observa
 * [SuggestionBatchRepository.observe] y asegura el Lote una vez al entrar con
 * [SuggestionBatchService.ensureBatch] (politica cache-first ya aceptada). La seccion "Sugerencias"
 * del Perfil reusa esa costura: su boton fuerza un refresco con `ensureBatch(..., force = true)` y
 * `MainShell` aporta el estado de UI "en vuelo" que deshabilita el boton (sin `ProfileRepository`:
 * el refresco no cambia ninguna preferencia). Mientras el Lote no se
 * resuelve, el carrusel pinta las recetas semilla empaquetadas ([HomeSuggestions]); no hay pantalla de
 * carga. La Home es offline: el unico generador conectado es `FakeRecipeGenerator` (sin red).
 *
 * El estado de navegacion vive en un `Saver` (patron de `OnboardingFlow`) y no en una libreria. La
 * pestana Perfil monta la pantalla real ([ProfileScreen], que escribe el Perfil con [onSaveProfile]);
 * la pestana Favoritos monta [FavoritesScreen] sobre los favoritos persistidos (observa
 * [FavoriteRepository.observe]) y su corazon cablea [FavoriteService.toggle]; esa misma lista alimenta
 * el bloque "Guardadas" de la Home y el corazon/"Guardada" del Detalle. El Generador es real
 * ([GeneratorFlow], pasos de
 * ingredientes y de ajustes) y dispara la generacion validada a traves de [GenerationService].
 *
 * El **bloque 2 de la Home** (Busqueda con IA) es el unico que necesita red: [HomeScreen] monta
 * `AiSearchBar` con la conectividad observada de [NetworkStatusSource] y, al enviar, empuja
 * [PushedScreen.SEARCH], que monta [AiSearchFlow] sobre [AiSearchService]. La consulta vive en
 * `rememberSaveable` (sobrevive a empujar/sacar la pagina y a la rotacion).
 *
 * Desde el **Detalle**, "Empezar a cocinar" abre el **Modo cocina** ([CookingModeScreen]) como segundo
 * overlay: recorre los pasos con progreso, marcado y temporizador, y "Atras"/"Terminar" vuelven al
 * Detalle intacto.
 */
@Composable
fun MainShell(
    profile: UserProfile,
    taxonomy: Taxonomy,
    suggestionBatchRepository: SuggestionBatchRepository,
    suggestionBatchService: SuggestionBatchService,
    generationService: GenerationService,
    favoriteRepository: FavoriteRepository,
    favoriteService: FavoriteService,
    activitySummaryRepository: ActivitySummaryRepository,
    aiSearchService: AiSearchService,
    networkStatusSource: NetworkStatusSource,
    seedRecipes: List<Recipe>,
    onSaveProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var navigation by rememberSaveable(stateSaver = MainNavigationStateSaver) {
        mutableStateOf(MainNavigationState())
    }

    val batch by suggestionBatchRepository.observe().collectAsState(initial = null)
    val favorites by favoriteRepository.observe().collectAsState(initial = emptyList())
    val recipesGeneratedCount by activitySummaryRepository.observeRecipesGeneratedCount()
        .collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    // Refresco manual "en vuelo": es estado de UI (no persistido) para deshabilitar el boton y evitar
    // dobles disparos; vive aqui porque `MainShell` posee el servicio y el scope.
    var refreshingSuggestions by remember { mutableStateOf(false) }
    // Detalle hospedado como overlay: `Recipe` no es serializable, asi que vive en `remember` (rotar
    // cierra el detalle, igual que el `result` de Resultados; Unknown de la spec).
    var detailRecipe by remember { mutableStateOf<Recipe?>(null) }
    // Modo cocina hospedado como segundo overlay, **encima** del Detalle: al cerrarlo (o "Terminar")
    // se vuelve al Detalle intacto. `Recipe` no es serializable, asi que vive en `remember` (rotar
    // cierra el modo, igual que el Detalle).
    var cookingRecipe by remember { mutableStateOf<Recipe?>(null) }
    // Consulta de la Busqueda con IA: sobrevive a empujar/sacar la pagina (la Home se desmonta al
    // empujar) y a la rotacion.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // Conectividad observada (unica dependencia externa de la Home); inicial optimista (true).
    val isOnline by networkStatusSource.observe().collectAsState(initial = true)
    LaunchedEffect(profile.preferences) {
        suggestionBatchService.ensureBatch(profile.preferences)
    }
    val suggestions = HomeSuggestions.visible(
        batchRecipes = batch?.recipes,
        seedRecipes = seedRecipes,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (navigation.pushed) {
            null -> Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    MainBottomBar(
                        selected = navigation.tab,
                        onSelect = { tab -> navigation = navigation.selectTab(tab) },
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { navigation = navigation.push(PushedScreen.GENERATOR) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(text = "＋ Crear receta")
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (navigation.tab) {
                        MainTab.HOME -> HomeScreen(
                            profile = profile,
                            taxonomy = taxonomy,
                            suggestions = suggestions,
                            recipes = HomeRecipes(
                                saved = favorites.map { it.recipe },
                                recentlyOpened = emptyList(),
                            ),
                            isOnline = isOnline,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearch = { navigation = navigation.push(PushedScreen.SEARCH) },
                            onOpenGenerator = {
                                navigation = navigation.push(PushedScreen.GENERATOR)
                            },
                            onOpenRecipe = { detailRecipe = it },
                        )

                        MainTab.FAVORITES -> FavoritesScreen(
                            favorites = favorites,
                            taxonomy = taxonomy,
                            onOpenRecipe = { detailRecipe = it },
                            onToggleFavorite = { recipe ->
                                scope.launch { favoriteService.toggle(recipe) }
                            },
                            onExplore = { navigation = navigation.selectTab(MainTab.HOME) },
                        )

                        MainTab.PROFILE -> ProfileScreen(
                            profile = profile,
                            taxonomy = taxonomy,
                            onSaveProfile = onSaveProfile,
                            suggestionsGeneratedAt = batch?.generatedAt,
                            isRefreshingSuggestions = refreshingSuggestions,
                            onRefreshSuggestions = {
                                if (!refreshingSuggestions) {
                                    refreshingSuggestions = true
                                    scope.launch {
                                        try {
                                            suggestionBatchService.ensureBatch(
                                                profile.preferences,
                                                force = true,
                                            )
                                        } finally {
                                            refreshingSuggestions = false
                                        }
                                    }
                                }
                            },
                            recipesGeneratedCount = recipesGeneratedCount,
                            favoritesCount = favorites.size,
                        )
                    }
                }
            }

            PushedScreen.GENERATOR -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                GeneratorFlow(
                    taxonomy = taxonomy,
                    preferences = profile.preferences,
                    generationService = generationService,
                    seedRecipes = seedRecipes,
                    onClose = { navigation = navigation.pop() },
                    onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) },
                    onOpenRecipe = { detailRecipe = it },
                )
            }

            PushedScreen.SEARCH -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                AiSearchFlow(
                    query = searchQuery,
                    preferences = profile.preferences,
                    taxonomy = taxonomy,
                    searchService = aiSearchService,
                    onBack = { navigation = navigation.pop() },
                    onOpenRecipe = { detailRecipe = it },
                    onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) },
                    onCreateRecipe = { navigation = navigation.push(PushedScreen.GENERATOR) },
                )
            }
        }

        // El Detalle se pinta **encima** de la Home o del Generador sin desmontarlos: al cerrarlo se
        // vuelve al estado intacto (resultado y borrador del Generador incluidos).
        detailRecipe?.let { recipe ->
            RecipeDetailScreen(
                recipe = recipe,
                taxonomy = taxonomy,
                isSaved = favorites.any { it.recipe.id == recipe.id },
                onToggleSave = { scope.launch { favoriteService.toggle(recipe) } },
                onStartCooking = { cookingRecipe = recipe },
                onBack = { detailRecipe = null },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Modo cocina: se pinta encima del Detalle sin desmontarlo; "Atras"/"Terminar" vuelven al
        // Detalle con su estado intacto.
        cookingRecipe?.let { recipe ->
            CookingModeScreen(
                recipe = recipe,
                onClose = { cookingRecipe = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MainBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.height(SazonComponents.bottomNavHeight),
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    TabGlyph(
                        tab = tab,
                        filled = isSelected,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            SazonComponents.bottomNavInactive
                        },
                    )
                },
                // El activo combina glifo relleno + etiqueta; el inactivo es solo glifo gris
                // (DESIGN.md: no depender solo del color).
                label = if (isSelected) {
                    { Text(text = tab.label) }
                } else {
                    null
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = SazonComponents.bottomNavInactive,
                    unselectedTextColor = SazonComponents.bottomNavInactive,
                ),
            )
        }
    }
}

/** Glifo de destino dibujado con Compose (sin dependencia de iconos): casa, marca o persona. */
@Composable
private fun TabGlyph(
    tab: MainTab,
    filled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val width = size.width
        val height = size.height
        val path = Path()
        when (tab) {
            MainTab.HOME -> {
                path.moveTo(0.5f * width, 0.10f * height)
                path.lineTo(0.92f * width, 0.46f * height)
                path.lineTo(0.92f * width, 0.90f * height)
                path.lineTo(0.08f * width, 0.90f * height)
                path.lineTo(0.08f * width, 0.46f * height)
                path.close()
            }

            MainTab.FAVORITES -> {
                path.moveTo(0.26f * width, 0.10f * height)
                path.lineTo(0.74f * width, 0.10f * height)
                path.lineTo(0.74f * width, 0.90f * height)
                path.lineTo(0.50f * width, 0.68f * height)
                path.lineTo(0.26f * width, 0.90f * height)
                path.close()
            }

            MainTab.PROFILE -> {
                path.addOval(Rect(center = Offset(0.5f * width, 0.32f * height), radius = 0.20f * width))
                path.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = 0.20f * width,
                            top = 0.58f * height,
                            right = 0.80f * width,
                            bottom = 0.90f * height,
                        ),
                        topLeft = CornerRadius(0.30f * width),
                        topRight = CornerRadius(0.30f * width),
                        bottomLeft = CornerRadius(0.30f * width),
                        bottomRight = CornerRadius(0.30f * width),
                    ),
                )
            }
        }
        if (filled) {
            drawPath(path = path, color = tint)
        } else {
            drawPath(path = path, color = tint, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/**
 * Saver de [MainNavigationState] para `rememberSaveable`, en la capa Compose: mantiene la logica pura
 * libre de Compose. Guarda el nombre de la pestana y el de la pagina empujada (vacio si no hay).
 */
private val MainNavigationStateSaver: Saver<MainNavigationState, Any> = listSaver(
    save = { state -> listOf(state.tab.name, state.pushed?.name.orEmpty()) },
    restore = { saved ->
        MainNavigationState(
            tab = MainTab.valueOf(saved[0]),
            pushed = saved[1].takeIf { it.isNotEmpty() }?.let(PushedScreen::valueOf),
        )
    },
)
