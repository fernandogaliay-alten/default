package com.sazon.app.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sazon.app.domain.taxonomy.Ingredient
import com.sazon.app.ui.theme.SazonPillShape
import com.sazon.app.ui.theme.SazonSpacing

/**
 * Editor reutilizable de ingredientes del paso 5 (paso 5: ingredientes que me gustan y que evito),
 * sin estado.
 *
 * Ofrece un buscador que filtra el listado por `label` ([IngredientsFilter]) y dos grupos sobre el
 * mismo listado filtrado: "Me gusta" (tratamiento terracota) y "Evito" (tratamiento oliva + helper).
 * Recibe los ids marcados y avisa por callback: la exclusion mutua (un ingrediente nunca en los dos
 * grupos) la garantiza el borrador, no el editor. No conoce el onboarding ni el Perfil y no renderiza
 * iconos (el repositorio no tiene assets ni dependencia de iconos).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientsEditor(
    ingredients: List<Ingredient>,
    preferredIds: List<String>,
    dislikedIds: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onTogglePreferred: (String) -> Unit,
    onToggleDisliked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = IngredientsFilter.matching(ingredients, query)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(text = "+ Añadir otro") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
        Text(
            text = "Un ingrediente se comparte entre los dos grupos: elegirlo en uno lo quita del otro.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SazonSpacing.md))

        IngredientGroup(
            title = "Me gusta",
            helper = null,
            filtered = filtered,
            selectedIds = preferredIds,
            treatment = IngredientChipTreatment.Preferred,
            onToggle = onTogglePreferred,
        )

        Spacer(modifier = Modifier.height(SazonSpacing.lg))

        IngredientGroup(
            title = "Evito",
            helper = "No son alergias, solo cosas que prefieres no ver.",
            filtered = filtered,
            selectedIds = dislikedIds,
            treatment = IngredientChipTreatment.Disliked,
            onToggle = onToggleDisliked,
        )
    }
}

/** Tratamiento visual de un grupo de ingredientes: terracota ("Me gusta") u oliva ("Evito"). */
private enum class IngredientChipTreatment { Preferred, Disliked }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientGroup(
    title: String,
    helper: String?,
    filtered: List<Ingredient>,
    selectedIds: List<String>,
    treatment: IngredientChipTreatment,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (helper != null) {
            Spacer(modifier = Modifier.height(SazonSpacing.sm))
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(SazonSpacing.sm))
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
                    IngredientChip(
                        label = ingredient.label,
                        selected = selectedIds.contains(ingredient.id),
                        treatment = treatment,
                        onClick = { onToggle(ingredient.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientChip(
    label: String,
    selected: Boolean,
    treatment: IngredientChipTreatment,
    onClick: () -> Unit,
) {
    val container = when (treatment) {
        IngredientChipTreatment.Preferred -> MaterialTheme.colorScheme.primaryContainer
        IngredientChipTreatment.Disliked -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = when (treatment) {
        IngredientChipTreatment.Preferred -> MaterialTheme.colorScheme.onPrimaryContainer
        IngredientChipTreatment.Disliked -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val accent = when (treatment) {
        IngredientChipTreatment.Preferred -> MaterialTheme.colorScheme.primary
        IngredientChipTreatment.Disliked -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        onClick = onClick,
        shape = SazonPillShape,
        color = if (selected) container else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) onContainer else MaterialTheme.colorScheme.onSurface,
        border = if (selected) {
            BorderStroke(2.dp, accent)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(
                horizontal = SazonSpacing.md,
                vertical = SazonSpacing.sm,
            ),
        )
    }
}
