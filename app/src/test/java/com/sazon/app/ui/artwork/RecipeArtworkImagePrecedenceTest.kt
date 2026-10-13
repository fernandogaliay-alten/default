package com.sazon.app.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Escenario 2 de `docs/specs/dish-image-progressive-ui.md`: la precedencia y el objetivo del crossfade
 * son **puros** y deterministas (JVM, sin Android ni Compose). Se prueban con `String` para no depender
 * de tipos de UI.
 */
class RecipeArtworkImagePrecedenceTest {

    @Test
    fun la_generada_manda_la_local_respalda_y_sin_ninguna_es_fallback() {
        assertEquals(
            "Con generada y local, la capa efectiva es la generada",
            RecipeArtworkImageLayer.GENERATED,
            RecipeArtworkImagePrecedence.layerOf("local", "generada"),
        )
        assertEquals(
            "Sin generada pero con local, la capa efectiva es la local",
            RecipeArtworkImageLayer.LOCAL,
            RecipeArtworkImagePrecedence.layerOf("local", null),
        )
        assertEquals(
            "Sin local pero con generada, sigue mandando la generada",
            RecipeArtworkImageLayer.GENERATED,
            RecipeArtworkImagePrecedence.layerOf(null, "generada"),
        )
        assertEquals(
            "Sin ninguna de las dos, el ultimo recurso",
            RecipeArtworkImageLayer.FALLBACK,
            RecipeArtworkImagePrecedence.layerOf<String>(null, null),
        )
    }

    @Test
    fun el_objetivo_del_crossfade_es_uno_solo_con_imagen_generada() {
        assertEquals(
            "Hay generada: la capa superior va a opaca",
            1f,
            RecipeArtworkImagePrecedence.crossfadeTarget("generada"),
            0f,
        )
        assertEquals(
            "Sin generada: la capa superior queda transparente",
            0f,
            RecipeArtworkImagePrecedence.crossfadeTarget<String>(null),
            0f,
        )
    }
}
