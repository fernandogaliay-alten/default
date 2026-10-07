package com.sazon.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sazon.app.R

/**
 * Familias tipográficas de `DESIGN.md`, empaquetadas como fuentes OFL en `res/font/`:
 * Manrope (títulos) e Inter (cuerpo). Se cargan por recurso, sin fuentes descargables por red.
 */
object SazonFontFamilies {
    val Display = FontFamily(
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_bold, FontWeight.Bold),
    )

    val Body = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
    )
}

/**
 * Escala tipográfica de `DESIGN.md`: `display` 32sp/700, `h1` 28sp/600, `body` 16sp/400 y
 * `caption` 12sp/500, mapeados a los estilos de Material 3 que consumen las pantallas.
 */
val SazonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = SazonFontFamilies.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SazonFontFamilies.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
