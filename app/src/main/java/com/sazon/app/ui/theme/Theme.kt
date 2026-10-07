package com.sazon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema global de Sazón. Cablea los tokens de `DESIGN.md` (color, tipografía y formas) y elige la
 * variante clara u oscura según el sistema. **No** hay color dinámico de Material You: ninguna ruta
 * lee el fondo de pantalla, de modo que los colores de la app no cambian con él.
 */
@Composable
fun SazonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = SazonTypography,
        shapes = SazonShapes,
        content = content,
    )
}
