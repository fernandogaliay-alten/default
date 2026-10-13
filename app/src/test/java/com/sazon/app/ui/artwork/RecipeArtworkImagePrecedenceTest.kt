package com.sazon.app.ui.artwork

import com.sazon.app.data.image.DishImageOrigin
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

    // --- Escenario 8 de `docs/specs/dish-image-prompt-v2.md` (diagnostico del origen) ---

    @Test
    fun escenario_8_el_origen_resuelto_cubre_las_cuatro_capas() {
        assertEquals(
            "Con generada y sin origen semilla, es generada",
            DishImageOrigin.GENERATED,
            RecipeArtworkImagePrecedence.originOf<String>(null, "generada", null),
        )
        assertEquals(
            "Con generada y origen SEED, la imagen salio del set semilla",
            DishImageOrigin.SEED,
            RecipeArtworkImagePrecedence.originOf("local", "generada", DishImageOrigin.SEED),
        )
        assertEquals(
            "Con generada y cualquier otro origen, la generada manda",
            DishImageOrigin.GENERATED,
            RecipeArtworkImagePrecedence.originOf("local", "generada", DishImageOrigin.GENERATED),
        )
        assertEquals(
            "Con generada y origen CATEGORY (dato incoherente) la generada manda",
            DishImageOrigin.GENERATED,
            RecipeArtworkImagePrecedence.originOf(null, "generada", DishImageOrigin.CATEGORY),
        )
        assertEquals(
            "Sin generada pero con foto local, es la cadena local",
            DishImageOrigin.CATEGORY,
            RecipeArtworkImagePrecedence.originOf("local", null, null),
        )
        assertEquals(
            "Sin generada ni foto local, es el glifo",
            DishImageOrigin.GLYPH,
            RecipeArtworkImagePrecedence.originOf<String>(null, null, null),
        )
    }

    @Test
    fun escenario_8b_la_clasificacion_del_origen_es_pura_y_determinista() {
        val primera = RecipeArtworkImagePrecedence.originOf("local", null, DishImageOrigin.SEED)
        val segunda = RecipeArtworkImagePrecedence.originOf("local", null, DishImageOrigin.SEED)

        assertEquals("Misma entrada, mismo origen", primera, segunda)
        assertEquals(DishImageOrigin.CATEGORY, primera)
    }
}
