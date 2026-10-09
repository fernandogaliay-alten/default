package com.sazon.app.ui.generator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Ingredient
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.preferences.IngredientsFilter
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

private const val HELPER = "Añade los ingredientes que tengas y nosotros hacemos el resto."

/**
 * Paso 1 del Generador ("¿Qué tienes?"), capa Compose **delgada** sobre [GeneratorIngredientsState]:
 * input libre con "Añadir", chips de los anadidos con "×", contador y nube colapsable de
 * "Ingredientes sugeridos". No guarda estado propio salvo el plegado de la nube (local) y delega todo
 * en los callbacks; la logica pura vive en [GeneratorIngredientsState].
 *
 * La nube y el buscador salen **siempre** de `taxonomy.ingredients` (la UI nunca diverge de la
 * Taxonomia) y el filtro reutiliza [IngredientsFilter]. Sin iconos: los chips muestran su `label` y la
 * "×" es texto; los colores/formas salen de los tokens del tema.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorIngredientsScreen(
    state: GeneratorIngredientsState,
    taxonomy: Taxonomy,
    onQueryChange: (String) -> Unit,
    onAddFree: () -> Unit,
    onToggleTaxonomy: (Ingredient) -> Unit,
    onRemove: (GeneratorIngredient) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = HELPER,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.md))

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = SazonPillShape,
            placeholder = { Text(text = "Escribe un ingrediente") },
            trailingIcon = {
                TextButton(
                    onClick = onAddFree,
                    enabled = state.query.isNotBlank(),
                ) {
                    Text(text = "Añadir")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.ingredients.isNotEmpty()) {
            Spacer(modifier = Modifier.height(SazonSpacing.md))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
            ) {
                state.ingredients.forEach { ingredient ->
                    AddedIngredientChip(
                        ingredient = ingredient,
                        onRemove = { onRemove(ingredient) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SazonSpacing.md))
        Text(
            text = state.countLabel,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(SazonSpacing.lg))

        SuggestedIngredientsCloud(
            taxonomy = taxonomy,
            query = state.query,
            selectedIds = state.ingredients.mapNotNull { it.ingredientId }.toSet(),
            onToggleTaxonomy = onToggleTaxonomy,
        )
    }
}

/** Chip de un ingrediente anadido: su `label` y una "×" que lo quita (sin iconos). */
@Composable
private fun AddedIngredientChip(
    ingredient: GeneratorIngredient,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = onRemove,
        shape = SazonPillShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SazonSpacing.md, vertical = SazonSpacing.sm),
        ) {
            Text(text = ingredient.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(SazonSpacing.sm))
            Text(text = "×", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Nube **colapsable** "Ingredientes sugeridos": chips de `taxonomy.ingredients` filtrados por la
 * consulta con [IngredientsFilter]. Un chip aparece **seleccionado** si su `id` esta entre los
 * anadidos; al pulsarlo, [onToggleTaxonomy] alterna. El plegado es estado local de UI (`rememberSaveable`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestedIngredientsCloud(
    taxonomy: Taxonomy,
    query: String,
    selectedIds: Set<String>,
    onToggleTaxonomy: (Ingredient) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val filtered = IngredientsFilter.matching(taxonomy.ingredients, query)

    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(text = "Ingredientes sugeridos")
            Spacer(modifier = Modifier.width(SazonSpacing.sm))
            Text(
                text = if (expanded) "−" else "+",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (expanded) {
            if (filtered.isEmpty()) {
                Text(
                    text = "Sin coincidencias.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
                ) {
                    filtered.forEach { ingredient ->
                        SuggestedIngredientChip(
                            label = ingredient.label,
                            selected = ingredient.id in selectedIds,
                            onClick = { onToggleTaxonomy(ingredient) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedIngredientChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        shape = SazonPillShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
