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
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.suggestions.SuggestionBatchRepository
import com.sazon.app.domain.suggestions.SuggestionBatchService
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.generator.GeneratorFlow
import com.sazon.app.ui.home.HomeRecipes
import com.sazon.app.ui.home.HomeScreen
import com.sazon.app.ui.home.HomeSuggestions
import com.sazon.app.ui.navigation.MainNavigationState
import com.sazon.app.ui.navigation.MainTab
import com.sazon.app.ui.navigation.PushedScreen
import com.sazon.app.ui.profile.ProfileScreen
import com.sazon.app.ui.theme.SazonComponents

/**
 * Armazon principal de la app tras el onboarding: barra de navegacion inferior de tres destinos
 * (Inicio · Favoritos · Perfil), FAB "＋ Crear receta" y enrutado del contenido.
 *
 * Aqui tambien se **compone** el Lote de sugerencias en la app: observa
 * [SuggestionBatchRepository.observe] y asegura el Lote una vez al entrar con
 * [SuggestionBatchService.ensureBatch] (politica cache-first ya aceptada). Mientras el Lote no se
 * resuelve, el carrusel pinta las recetas semilla empaquetadas ([HomeSuggestions]); no hay pantalla de
 * carga. La Home es offline: el unico generador conectado es `FakeRecipeGenerator` (sin red).
 *
 * El estado de navegacion vive en un `Saver` (patron de `OnboardingFlow`) y no en una libreria. La
 * pestana Perfil monta la pantalla real ([ProfileScreen], que escribe el Perfil con [onSaveProfile]);
 * Favoritos sigue siendo [SectionPlaceholder].
 */
@Composable
fun MainShell(
    profile: UserProfile,
    taxonomy: Taxonomy,
    suggestionBatchRepository: SuggestionBatchRepository,
    suggestionBatchService: SuggestionBatchService,
    seedRecipes: List<Recipe>,
    savedRecipes: List<Recipe>,
    recentlyOpenedRecipes: List<Recipe>,
    onSaveProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var navigation by rememberSaveable(stateSaver = MainNavigationStateSaver) {
        mutableStateOf(MainNavigationState())
    }

    val batch by suggestionBatchRepository.observe().collectAsState(initial = null)
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
                                saved = savedRecipes,
                                recentlyOpened = recentlyOpenedRecipes,
                            ),
                            onOpenGenerator = {
                                navigation = navigation.push(PushedScreen.GENERATOR)
                            },
                        )

                        MainTab.FAVORITES -> SectionPlaceholder(title = "Favoritos")

                        MainTab.PROFILE -> ProfileScreen(
                            profile = profile,
                            taxonomy = taxonomy,
                            onSaveProfile = onSaveProfile,
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
                    onClose = { navigation = navigation.pop() },
                )
            }
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
