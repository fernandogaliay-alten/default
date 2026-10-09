package com.sazon.app.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.RecipeCategory
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Fila de chips de seleccion **unica** para el bloque "Tus recetas" (bloque B.5 de
 * `docs/design/stitch-prompt.md`): "Todas" primero, una chip por categoria **presente**, y al final
 * "Rapido" y "Vegetariano".
 *
 * Los filtros **derivados** se distinguen de las categorias por dos vias: van agrupados aparte (tras un
 * separador) y, cuando estan seleccionados, usan el acento **oliva** (`secondary`/
 * `chip-selected-secondary`) en vez del terracota (`primary`/`chip-selected`). Tokens del tema, sin
 * hex sueltos. Solo pinta y delega la seleccion en [onSelect]; la logica pura vive en
 * [LocalResultFilters].
 */
@Composable
fun ResultFilterRow(
    taxonomy: Taxonomy,
    categories: List<RecipeCategory>,
    selected: LocalResultFilter,
    onSelect: (LocalResultFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryFilters = buildList {
        add(LocalResultFilter.All)
        addAll(categories.map { LocalResultFilter.Category(it.id) })
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(categoryFilters) { filter ->
            ResultFilterChip(
                label = LocalResultFilters.label(taxonomy, filter),
                selected = filter == selected,
                derived = false,
                onClick = { onSelect(filter) },
            )
        }
        item { Spacer(modifier = Modifier.width(SazonSpacing.sm)) }
        items(DERIVED_FILTERS) { filter ->
            ResultFilterChip(
                label = LocalResultFilters.label(taxonomy, filter),
                selected = filter == selected,
                derived = true,
                onClick = { onSelect(filter) },
            )
        }
    }
}

private val DERIVED_FILTERS = listOf(LocalResultFilter.Fast, LocalResultFilter.Vegetarian)

@Composable
private fun ResultFilterChip(
    label: String,
    selected: Boolean,
    derived: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        shape = SazonPillShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (derived) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            },
            selectedLabelColor = if (derived) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
        ),
    )
}
