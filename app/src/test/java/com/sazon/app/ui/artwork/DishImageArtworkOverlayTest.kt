package com.sazon.app.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 6 de `docs/specs/dish-artwork-as-card-background.md`: la geometria del hueco L2 (ratio
 * 16:9) y las decisiones tipograficas derivadas de la capa son **puras** y deterministas (JVM, sin
 * Android ni Compose). No hay caso vacio: las dos capas y los dos valores de alfa se afirman.
 */
class DishImageArtworkOverlayTest {

    @Test
    fun el_hueco_l2_es_dieciseis_nueve() {
        assertEquals(16f / 9f, DishImageArtworkOverlay.HOLE_RATIO, 0f)
        assertTrue("Es mas ancho que el 16:10 de la ficha apagada", DishImageArtworkOverlay.HOLE_RATIO > 16f / 10f)
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
