package com.sazon.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.filters.LocalResultFilter
import com.sazon.app.ui.filters.LocalResultFilters
import com.sazon.app.ui.filters.ResultFilterRow
import com.sazon.app.ui.onboarding.AvatarPreview
import com.sazon.app.ui.theme.SazonInversePrimary
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonPrimary
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing
import java.util.Calendar

/**
 * Home real de Sazon: los cuatro bloques **locales** de `DESIGN.md` (el bloque 2, Búsqueda con IA, es
 * de `ai-search`).
 *
 * 1. Cabecera con saludo por franja horaria y avatar de iniciales.
 * 3. Tarjeta-heroe que abre el Generador.
 * 4. Carrusel "Sugeridas para ti" sobre las recetas resueltas (Lote o respaldo a semilla).
 * 5. Bloque "Tus recetas" con su estado vacio de usuario nuevo.
 *
 * Se muestra poblada de inmediato, sin pantalla de carga. La hora se captura una sola vez con
 * `Calendar` y la identidad se recuerda. Sin Compose en la logica pura: esta capa solo pinta.
 */
@Composable
fun HomeScreen(
    profile: UserProfile,
    taxonomy: Taxonomy,
    suggestions: List<Recipe>,
    recipes: HomeRecipes,
    onOpenGenerator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val identity = remember(profile, taxonomy) { HomeIdentity.from(profile, taxonomy) }
    var filter by rememberSaveable(stateSaver = LocalResultFilterSaver) {
        mutableStateOf<LocalResultFilter>(LocalResultFilter.All)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SazonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.lg),
    ) {
        item {
            HomeHeader(
                greeting = HomeGreeting.text(profile.displayName, hour),
                identity = identity,
            )
        }
        item { HeroCard(onOpenGenerator = onOpenGenerator) }
        item { SuggestionsSection(suggestions = suggestions, taxonomy = taxonomy) }
        item {
            RecipesSection(
                recipes = recipes,
                taxonomy = taxonomy,
                filter = filter,
                onSelect = { filter = it },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    identity: HomeIdentity,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        AvatarPreview(
            colorHex = identity.hex,
            initials = identity.initials,
            size = 40.dp,
        )
    }
}

@Composable
private fun HeroCard(onOpenGenerator: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SazonShapes.medium)
            .background(Brush.linearGradient(listOf(SazonPrimary, SazonInversePrimary))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SazonSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            Text(
                text = "¿Qué tienes en la nevera?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "Añade tus ingredientes y te proponemos recetas al instante.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Button(
                onClick = onOpenGenerator,
                shape = SazonPillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = SazonPrimary,
                ),
            ) {
                Text(text = "Crear receta")
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: List<Recipe>,
    taxonomy: Taxonomy,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = "Sugeridas para ti",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "basado en tus gustos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(SazonSpacing.md),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(suggestions) { recipe ->
                RecipeCard(recipe = recipe, taxonomy = taxonomy)
            }
        }
    }
}

@Composable
private fun RecipesSection(
    recipes: HomeRecipes,
    taxonomy: Taxonomy,
    filter: LocalResultFilter,
    onSelect: (LocalResultFilter) -> Unit,
) {
    val allRecipes = recipes.saved + recipes.recentlyOpened
    val categories = LocalResultFilters.categoriesIn(allRecipes, taxonomy)
    val filtered = recipes.filteredBy(filter, taxonomy)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = "Tus recetas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (recipes.isEmpty) {
            Text(
                text = "Cuando guardes una receta aparecerá aquí.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ResultFilterRow(
                taxonomy = taxonomy,
                categories = categories,
                selected = filter,
                onSelect = onSelect,
            )
            if (filtered.isEmpty) {
                Text(
                    text = "No hay recetas con este filtro.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HomeRecipesGroup(
                    title = "Guardadas",
                    recipes = filtered.saved,
                    taxonomy = taxonomy,
                )
                HomeRecipesGroup(
                    title = "Abiertas hace poco",
                    recipes = filtered.recentlyOpened,
                    taxonomy = taxonomy,
                )
            }
        }
    }
}

/**
 * Grupo del bloque "Tus recetas": muestra su titulo siempre y, si trae recetas, su carrusel
 * horizontal. El estado vacio global lo cubre la linea del bloque ([RecipesSection]).
 */
@Composable
private fun HomeRecipesGroup(
    title: String,
    recipes: List<Recipe>,
    taxonomy: Taxonomy,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (recipes.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.md),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(recipes) { recipe ->
                    RecipeCard(recipe = recipe, taxonomy = taxonomy)
                }
            }
        }
    }
}

/**
 * Saver de [LocalResultFilter] para `rememberSaveable`, en la capa Compose: mantiene la logica pura
 * libre de Compose. Guarda la [LocalResultFilter.key] y la restaura con [LocalResultFilters.fromKey]
 * (una clave desconocida degrada a "Todas").
 */
private val LocalResultFilterSaver: Saver<LocalResultFilter, Any> = listSaver(
    save = { filter -> listOf(filter.key) },
    restore = { saved -> LocalResultFilters.fromKey(saved[0] as String) },
)
