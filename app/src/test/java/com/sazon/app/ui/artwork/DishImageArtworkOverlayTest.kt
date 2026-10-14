package com.sazon.app.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 6 de `docs/specs/dish-artwork-as-card-background.md` y de
 * `docs/specs/recipe-card-l2-unified.md`: la geometria del hueco L2 (ratio 16:9), las decisiones
 * tipograficas derivadas de la capa y el reparto de la banda solida son **puros** y deterministas (JVM,
 * sin Android ni Compose). No hay caso vacio: las dos capas, los dos valores de alfa y los cuatro
 * vectores de la banda se afirman.
 */
class DishImageArtworkOverlayTest {

    @Test
    fun el_hueco_l2_es_dieciseis_nueve() {
        assertEquals(16f / 9f, DishImageArtworkOverlay.HOLE_RATIO, 0f)
        assertTrue("Es mas ancho que alto", DishImageArtworkOverlay.HOLE_RATIO > 1f)
    }

    @Test
    fun la_banda_es_una_linea_salvo_en_la_tarjeta_ancha() {
        // Estrecha (RecipeCard): una linea.
        assertEquals(
            FichaBandLayout.ONE_LINE,
            DishImageArtworkOverlay.bandLayout(wide = false, compact = false),
        )
        // Ancha (ResultRecipeCard): tres filas.
        assertEquals(
            FichaBandLayout.THREE_ROWS,
            DishImageArtworkOverlay.bandLayout(wide = true, compact = false),
        )
    }

    @Test
    fun la_banda_compacta_es_siempre_una_linea_y_manda_sobre_wide() {
        assertEquals(
            FichaBandLayout.ONE_LINE,
            DishImageArtworkOverlay.bandLayout(wide = false, compact = true),
        )
        // Caso imposible `wide && compact`: manda `compact`, determinista y sin excepcion.
        assertEquals(
            FichaBandLayout.ONE_LINE,
            DishImageArtworkOverlay.bandLayout(wide = true, compact = true),
        )
    }

    @Test
    fun la_tipografia_va_sobre_imagen_solo_con_imagen() {
        assertTrue(DishImageArtworkOverlay.headingOnImage(RecipeArtworkImageLayer.GENERATED))
        assertFalse(DishImageArtworkOverlay.headingOnImage(RecipeArtworkImageLayer.TYPOGRAPHIC))
    }

    @Test
    fun la_marca_de_agua_va_solo_sin_imagen() {
        assertTrue(DishImageArtworkOverlay.watermarkVisible(RecipeArtworkImageLayer.TYPOGRAPHIC))
        assertFalse(DishImageArtworkOverlay.watermarkVisible(RecipeArtworkImageLayer.GENERATED))
    }

    @Test
    fun el_alfa_de_la_capa_sale_de_la_precedencia() {
        assertEquals(1f, DishImageArtworkOverlay.crossfadeAlpha(RecipeArtworkImageLayer.GENERATED), 0f)
        assertEquals(0f, DishImageArtworkOverlay.crossfadeAlpha(RecipeArtworkImageLayer.TYPOGRAPHIC), 0f)
    }

    @Test
    fun las_decisiones_son_deterministas_y_coherentes() {
        RecipeArtworkImageLayer.entries.forEach { layer ->
            val onImage = DishImageArtworkOverlay.headingOnImage(layer)
            val watermark = DishImageArtworkOverlay.watermarkVisible(layer)
            assertEquals(
                "Sobre imagen y marca de agua son excluyentes",
                onImage,
                !watermark,
            )
            assertEquals(
                "Misma entrada, mismo alfa",
                DishImageArtworkOverlay.crossfadeAlpha(layer),
                DishImageArtworkOverlay.crossfadeAlpha(layer),
                0f,
            )
        }
    }
}
