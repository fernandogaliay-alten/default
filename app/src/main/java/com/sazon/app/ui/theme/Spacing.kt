package com.sazon.app.ui.theme

import androidx.compose.ui.unit.dp

/** Ritmo de espaciado de `DESIGN.md` (`spacing`). */
object SazonSpacing {
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
}

/** Métricas de componentes tomadas de `DESIGN.md` (`components`). */
object SazonComponents {
    val buttonHeight = 52.dp
    val bottomNavHeight = 80.dp

    /** Relación de aspecto de la imagen de `RecipeCard` (`16:10`). */
    const val recipeCardImageRatio = 16f / 10f

    /** Color inactivo de la barra inferior (`bottom-nav.inactiveColor`). */
    val bottomNavInactive = SazonBottomNavInactive
}
