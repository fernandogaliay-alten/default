package com.sazon.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas de `DESIGN.md`: inputs a 16dp, tarjetas e imágenes a 24dp y chips/botones en píldora.
 * `SazonPillShape` se usa explícitamente en botones y chips (`rounded.full` = 999px).
 */
val SazonShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Píldora completa para botones y chips. */
val SazonPillShape = RoundedCornerShape(percent = 50)
