package com.sazon.app.ui.artwork

import com.sazon.app.data.image.DishImageOrigin
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Escenarios 2 y 6 de `docs/specs/dish-artwork-as-card-background.md`: la precedencia de la **capa de
 * fondo** y el objetivo del crossfade son **puros** y deterministas (JVM, sin Android ni Compose). Se
 * prueban con `String` para no depender de tipos de UI. La capa paso de tres valores
 * (`FALLBACK`/`LOCAL`/`GENERATED`) a **dos** (`TYPOGRAPHIC`/`GENERATED`) con
 * `dish-artwork-as-card-background`, y la cadena local de fotos se retiro por completo con
 * `retire-recipe-photos` (ADR 0006), de modo que la base sin imagen es siempre la tipografia.
 */
class RecipeArtworkImagePrecedenceTest {

    @Test
    fun con_imagen_es_generada_y_sin_imagen_es_tipografica() {
        assertEquals(
            "Con imagen generada, la capa efectiva es la generada",
            RecipeArtworkImageLayer.GENERATED,
            RecipeArtworkImagePrecedence.layerOf("generada"),
        )
        assertEquals(
            "Sin imagen, la capa efectiva es la ficha tipografica",
            RecipeArtworkImageLayer.TYPOGRAPHIC,
            RecipeArtworkImagePrecedence.layerOf<String>(null),
        )
    }

    @Test
    fun el_objetivo_del_crossfade_es_uno_solo_con_imagen_generada() {
        assertEquals(
            "Hay imagen: la capa de fondo va a opaca",
            1f,
            RecipeArtworkImagePrecedence.crossfadeTarget("generada"),
            0f,
        )
        assertEquals(
            "Sin imagen: la capa de fondo queda transparente",
            0f,
            RecipeArtworkImagePrecedence.crossfadeTarget<String>(null),
            0f,
        )
    }

    @Test
    fun la_precedencia_es_pura_y_determinista() {
        assertEquals(
            "Misma entrada, misma capa",
            RecipeArtworkImagePrecedence.layerOf("generada"),
            RecipeArtworkImagePrecedence.layerOf("generada"),
        )
        assertEquals(
            "Misma entrada, mismo alfa",
            RecipeArtworkImagePrecedence.crossfadeTarget("generada"),
            RecipeArtworkImagePrecedence.crossfadeTarget("generada"),
            0f,
        )
    }

    // --- Diagnostico del origen (dos capas) ---

    @Test
    fun el_origen_resuelto_cubre_las_dos_capas() {
        assertEquals(
            "Con imagen y sin origen semilla, es generada",
            DishImageOrigin.GENERATED,
            RecipeArtworkImagePrecedence.originOf<String>("generada", null),
        )
        assertEquals(
            "Con imagen y origen SEED, la imagen salio del set semilla",
            DishImageOrigin.SEED,
            RecipeArtworkImagePrecedence.originOf("generada", DishImageOrigin.SEED),
        )
        assertEquals(
            "Con imagen y cualquier otro origen, la generada manda",
            DishImageOrigin.GENERATED,
            RecipeArtworkImagePrecedence.originOf("generada", DishImageOrigin.GENERATED),
        )
        assertEquals(
            "Sin imagen, la base tipografica es el glifo",
            DishImageOrigin.GLYPH,
            RecipeArtworkImagePrecedence.originOf<String>(null, null),
        )
        assertEquals(
            "Sin imagen y con un origen residual, sigue siendo el glifo",
            DishImageOrigin.GLYPH,
            RecipeArtworkImagePrecedence.originOf<String>(null, DishImageOrigin.SEED),
        )
    }

    @Test
    fun la_clasificacion_del_origen_es_pura_y_determinista() {
        val primera = RecipeArtworkImagePrecedence.originOf<String>(null, DishImageOrigin.GENERATED)
        val segunda = RecipeArtworkImagePrecedence.originOf<String>(null, DishImageOrigin.GENERATED)

        assertEquals("Misma entrada, mismo origen", primera, segunda)
        assertEquals(DishImageOrigin.GLYPH, primera)
    }
}
