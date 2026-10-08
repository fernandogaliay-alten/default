package com.sazon.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta anclada a `DESIGN.md` (frontmatter YAML). Es la única fuente de verdad del color de la app:
// ninguna pantalla debe declarar hex propios. Los roles `*Container`, `inverse*`, `surfaceVariant`,
// `outlineVariant` y `scrim` que DESIGN.md no fija se derivan aquí y quedan pendientes de un repaso
// visual de la persona (ver `docs/specs/design-tokens-and-theme.md`).

// --- Paleta de DESIGN.md ---
val SazonPrimary = Color(0xFFD9552B)
val SazonOnPrimary = Color(0xFFFFFFFF)
val SazonPrimaryContainer = Color(0xFFFFDCCF)
val SazonSecondary = Color(0xFF5F7A3D)
val SazonOnSecondary = Color(0xFFFFFFFF)
val SazonAccent = Color(0xFFF2A93B) // `accent` -> rol `tertiary`
val SazonOnAccent = Color(0xFF2B2118)
val SazonBackground = Color(0xFFFFF8F0)
val SazonSurface = Color(0xFFFFFFFF)
val SazonText = Color(0xFF2B2118) // `text` -> `onBackground` y `onSurface`
val SazonOutline = Color(0xFFE8DDD3)
val SazonSuccess = Color(0xFF4C7A34)
val SazonError = Color(0xFFB3261E)
val SazonOnError = Color(0xFFFFFFFF)
val SazonDarkBackground = Color(0xFF241C16)
val SazonDarkSurface = Color(0xFF312720)
val SazonBottomNavInactive = Color(0xFF9A8C7F)

// --- Roles derivados del modo claro (no fijados por DESIGN.md) ---
val SazonOnPrimaryContainer = SazonText
val SazonSecondaryContainer = Color(0xFFDCE8CC)
val SazonOnSecondaryContainer = SazonText
val SazonAccentContainer = Color(0xFFFCE3B8)
val SazonOnAccentContainer = SazonText
val SazonInversePrimary = Color(0xFFA63B1A)
val SazonInverseSurface = SazonText
val SazonInverseOnSurface = SazonBackground
val SazonSurfaceVariant = Color(0xFFF2E8DE)
val SazonOnSurfaceVariant = Color(0xFF5A4B40)
val SazonOutlineVariant = SazonOutline
val SazonErrorContainer = Color(0xFFFFDAD6)
val SazonOnErrorContainer = Color(0xFF410E0B)
val SazonScrim = Color(0xFF000000)

// --- Roles derivados del modo oscuro (anclados a `darkBackground`/`darkSurface` y acentos) ---
val SazonDarkOnBackground = SazonBackground
val SazonDarkPrimaryContainer = Color(0xFF6E2C15)
val SazonDarkOnPrimaryContainer = Color(0xFFFFDCCF)
val SazonDarkSecondaryContainer = Color(0xFF2F3D1E)
val SazonDarkOnSecondaryContainer = Color(0xFFD7E8BF)
val SazonDarkAccentContainer = Color(0xFF4A3413)
val SazonDarkOnAccentContainer = Color(0xFFFFE7BE)
val SazonDarkOnSurface = SazonBackground
val SazonDarkOutline = Color(0xFF5A4B40)
val SazonDarkSurfaceVariant = Color(0xFF3D3129)
val SazonDarkOnSurfaceVariant = SazonOutline
val SazonDarkOutlineVariant = Color(0xFF3D3129)
val SazonDarkInversePrimary = Color(0xFFFFB59A)
val SazonDarkInverseSurface = SazonBackground
val SazonDarkInverseOnSurface = SazonText
val SazonDarkErrorContainer = Color(0xFF8C0009)
val SazonDarkOnErrorContainer = Color(0xFFFFDAD6)

/**
 * Esquema claro de Material 3 con **todos** los roles sobrescritos: ningún valor por defecto
 * purple/pink de la plantilla puede mostrarse. Es `internal` porque las pantallas consumen el tema a
 * través de `MaterialTheme.colorScheme`; el test de tokens lo inspecciona en el mismo paquete.
 */
internal val LightColorScheme = lightColorScheme(
    primary = SazonPrimary,
    onPrimary = SazonOnPrimary,
    primaryContainer = SazonPrimaryContainer,
    onPrimaryContainer = SazonOnPrimaryContainer,
    inversePrimary = SazonInversePrimary,
    secondary = SazonSecondary,
    onSecondary = SazonOnSecondary,
    secondaryContainer = SazonSecondaryContainer,
    onSecondaryContainer = SazonOnSecondaryContainer,
    tertiary = SazonAccent,
    onTertiary = SazonOnAccent,
    tertiaryContainer = SazonAccentContainer,
    onTertiaryContainer = SazonOnAccentContainer,
    background = SazonBackground,
    onBackground = SazonText,
    surface = SazonSurface,
    onSurface = SazonText,
    surfaceVariant = SazonSurfaceVariant,
    onSurfaceVariant = SazonOnSurfaceVariant,
    surfaceTint = SazonPrimary,
    inverseSurface = SazonInverseSurface,
    inverseOnSurface = SazonInverseOnSurface,
    error = SazonError,
    onError = SazonOnError,
    errorContainer = SazonErrorContainer,
    onErrorContainer = SazonOnErrorContainer,
    outline = SazonOutline,
    outlineVariant = SazonOutlineVariant,
    scrim = SazonScrim,
    surfaceBright = SazonSurface,
    surfaceDim = SazonBackground,
    surfaceContainer = SazonSurface,
    surfaceContainerHigh = SazonSurface,
    surfaceContainerHighest = SazonSurface,
    surfaceContainerLow = SazonSurface,
    surfaceContainerLowest = SazonSurface,
)

/**
 * Esquema oscuro derivado de `darkBackground`/`darkSurface` conservando los acentos terracota y
 * oliva de `DESIGN.md`, con texto crema. Ver tabla de mapeo en la spec.
 */
internal val DarkColorScheme = darkColorScheme(
    primary = SazonPrimary,
    onPrimary = SazonOnPrimary,
    primaryContainer = SazonDarkPrimaryContainer,
    onPrimaryContainer = SazonDarkOnPrimaryContainer,
    inversePrimary = SazonDarkInversePrimary,
    secondary = SazonSecondary,
    onSecondary = SazonOnSecondary,
    secondaryContainer = SazonDarkSecondaryContainer,
    onSecondaryContainer = SazonDarkOnSecondaryContainer,
    tertiary = SazonAccent,
    onTertiary = SazonOnAccent,
    tertiaryContainer = SazonDarkAccentContainer,
    onTertiaryContainer = SazonDarkOnAccentContainer,
    background = SazonDarkBackground,
    onBackground = SazonDarkOnBackground,
    surface = SazonDarkSurface,
    onSurface = SazonDarkOnSurface,
    surfaceVariant = SazonDarkSurfaceVariant,
    onSurfaceVariant = SazonDarkOnSurfaceVariant,
    surfaceTint = SazonPrimary,
    inverseSurface = SazonDarkInverseSurface,
    inverseOnSurface = SazonDarkInverseOnSurface,
    error = SazonError,
    onError = SazonOnError,
    errorContainer = SazonDarkErrorContainer,
    onErrorContainer = SazonDarkOnErrorContainer,
    outline = SazonDarkOutline,
    outlineVariant = SazonDarkOutlineVariant,
    scrim = SazonScrim,
    surfaceBright = SazonDarkSurface,
    surfaceDim = SazonDarkBackground,
    surfaceContainer = SazonDarkSurface,
    surfaceContainerHigh = SazonDarkSurface,
    surfaceContainerHighest = SazonDarkSurface,
    surfaceContainerLow = SazonDarkSurface,
    surfaceContainerLowest = SazonDarkSurface,
)
