package com.sazon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema global de Sazón. Cablea los tokens de `DESIGN.md` (color, tipografía y formas) y elige la
 * variante clara u oscura según el sistema. **No** hay color dinámico de Material You: ninguna ruta
 * lee el fondo de pantalla, de modo que los colores de la app no cambian con él.
 *
 * Además provee el contrato **de tarjeta** ([LocalSazonCardColors]: superficie y límite, ver
 * `CardTokens.kt`), que `MaterialTheme.colorScheme` no cubre porque en oscuro la tarjeta no usa
 * `darkSurface`.
 */
@Composable
fun SazonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSazonCardColors provides SazonCardTokens.of(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = SazonTypography,
            shapes = SazonShapes,
            content = content,
        )
    }
}
