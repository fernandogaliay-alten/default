package com.sazon.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sazon.app.ui.theme.SazonOnPrimary
import com.sazon.app.ui.theme.SazonPrimary

/**
 * Avatar de iniciales reutilizable (paso 1, Home y Perfil). Pinta el circulo con el color de la
 * Taxonomia y muestra las iniciales; si no hay nombre, un icono generico de persona, nunca iniciales
 * vacias. El color de las iniciales es blanco porque `avatarColors[].hex` garantiza contraste.
 *
 * El icono de persona se dibuja con primitivas de Compose en vez de `Icons.Default.Person`: el
 * catalogo `compose-bom` no arrastra `material-icons-core` y la spec de esta feature prohibe anadir
 * dependencias (ver hallazgo en `docs/specs/onboarding-shell-and-identity.md`).
 */
@Composable
fun AvatarPreview(
    colorHex: String,
    initials: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val color = remember(colorHex) { avatarColorOrFallback(colorHex) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (initials == null) {
            PersonGlyph(
                tint = SazonOnPrimary,
                modifier = Modifier.size(size / 2),
            )
        } else {
            Text(
                text = initials,
                color = SazonOnPrimary,
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}

/** Silueta generica de persona (cabeza + hombros), sin dependencias de iconos. */
@Composable
private fun PersonGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = this.size.width
        val height = this.size.height
        val headRadius = width * 0.22f
        drawCircle(
            color = tint,
            radius = headRadius,
            center = Offset(x = width / 2f, y = headRadius),
        )
        val bodyTop = headRadius * 2f + height * 0.06f
        drawRoundRect(
            color = tint,
            topLeft = Offset(x = width * 0.16f, y = bodyTop),
            size = Size(width = width * 0.68f, height = height - bodyTop),
            cornerRadius = CornerRadius(x = width * 0.34f, y = width * 0.34f),
        )
    }
}

/**
 * Convierte el `hex` de la Taxonomia a color; si viniera corrupto, cae al primario terracota en vez
 * de lanzar. La seleccion real siempre sale de `avatarColors`, por lo que el respaldo es defensivo.
 */
private fun avatarColorOrFallback(hex: String): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(SazonPrimary)
