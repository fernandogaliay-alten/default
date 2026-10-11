package com.sazon.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sazon.app.ui.theme.SazonShapes
import com.sazon.app.ui.theme.SazonSpacing

/** Cifras tabulares para el numero grande ("tnum" en `DESIGN.md`). */
private const val TABULAR_FIGURES = "tnum"

/**
 * Seccion **"Resumen"** del Perfil: dos tarjetas de estadistica compactas y de **igual ancho** ("recetas
 * generadas" y "favoritas").
 *
 * Capa Compose delgada y puramente presentacional: los dos numeros llegan ya calculados (`recipesGeneratedCount`
 * es el contador persistido; `favoritesCount` se deriva de la lista observada de Favoritos). Se pinta
 * entre la cabecera de identidad y la seccion "Sugerencias". Sin iconos ni imagenes (el repositorio no
 * tiene `material-icons` y `DESIGN.md` los marca como opcionales); solo tarjetas `surface`, cifras
 * tabulares en el numero grande y la etiqueta en texto secundario, con tokens del tema.
 */
@Composable
fun ActivitySummarySection(
    recipesGeneratedCount: Int,
    favoritesCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = SazonSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
    ) {
        Text(
            text = "Resumen",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SazonSpacing.sm)) {
            ActivityStatCard(
                value = recipesGeneratedCount,
                label = "recetas generadas",
                modifier = Modifier.weight(1f),
            )
            ActivityStatCard(
                value = favoritesCount,
                label = "favoritas",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Tarjeta de estadistica: numero grande en cifras tabulares (peso Medium) y etiqueta secundaria. */
@Composable
private fun ActivityStatCard(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = SazonShapes.large,
    ) {
        Column(
            modifier = Modifier.padding(SazonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SazonSpacing.sm),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = TABULAR_FIGURES,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
