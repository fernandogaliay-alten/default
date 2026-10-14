package com.sazon.app.data.ai.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenario 10 (parte **pura**) de `docs/specs/dish-style-reference.md`: la referencia de estilo se
 * resuelve por extensiones sobre el asset neutro y versionado (el ancla de estilo de `recipe-photos`,
 * conservada por `retire-recipe-photos`), con extension -> media type y nombre de fichero
 * deterministas. JVM puro, sin Android y sin I/O (`exists` inyectado).
 */
class DishImageStyleReferenceTest {

    // La base y las extensiones son las del ancla conservada: `img/recipes/default` y [webp, jpg, png].
    @Test
    fun la_referencia_apunta_al_asset_por_defecto_de_recetas() {
        assertEquals("img/recipes/default", DishImageStyleReferencePolicy.ASSET_BASE)
        assertEquals(listOf("webp", "jpg", "png"), DishImageStyleReferencePolicy.EXTENSIONS)
    }

    // Sin ningun asset presente no hay referencia (nunca una ruta inventada): degrada al texto.
    @Test
    fun sin_asset_no_hay_referencia() {
        assertNull(DishImageStyleReferencePolicy.resolve { false })
    }

    // Se prueba en orden de extensiones: con webp y png presentes gana webp.
    @Test
    fun se_prueba_en_orden_de_extensiones() {
        val resolved = DishImageStyleReferencePolicy.resolve { it in setOf("img/recipes/default.webp", "img/recipes/default.png") }
        assertEquals("img/recipes/default.webp", resolved)
    }

    // Si faltan las anteriores, cae a jpg; si solo hay png, a png (nunca una ruta de otra extension).
    @Test
    fun cae_a_la_siguiente_extension_disponible() {
        assertEquals(
            "img/recipes/default.jpg",
            DishImageStyleReferencePolicy.resolve { it in setOf("img/recipes/default.jpg", "img/recipes/default.png") },
        )
        assertEquals(
            "img/recipes/default.png",
            DishImageStyleReferencePolicy.resolve { it == "img/recipes/default.png" },
        )
    }

    // La base es inyectable (costura del cargador Android): una base inexistente resuelve `null`.
    @Test
    fun una_base_inexistente_resuelve_null() {
        assertNull(
            DishImageStyleReferencePolicy.resolve(
                base = "img/nope/ausente",
                exists = { path -> path == "img/recipes/default.jpg" },
            ),
        )
    }

    // El media type se deriva de la extension; jpeg es alias de jpg y lo desconocido no lanza.
    @Test
    fun el_media_type_se_deriva_de_la_extension() {
        assertEquals("image/jpeg", DishImageStyleReferencePolicy.mediaTypeFor("jpg"))
        assertEquals("image/jpeg", DishImageStyleReferencePolicy.mediaTypeFor("jpeg"))
        assertEquals("image/webp", DishImageStyleReferencePolicy.mediaTypeFor("webp"))
        assertEquals("image/png", DishImageStyleReferencePolicy.mediaTypeFor("png"))
        assertEquals("application/octet-stream", DishImageStyleReferencePolicy.mediaTypeFor("gif"))
    }

    // El nombre de fichero de la parte `image` conserva la extension del asset resuelto.
    @Test
    fun el_nombre_de_fichero_conserva_la_extension() {
        assertEquals(
            "dish-style-reference.jpg",
            DishImageStyleReferencePolicy.fileNameFor("img/recipes/default.jpg"),
        )
        assertEquals(
            "dish-style-reference.webp",
            DishImageStyleReferencePolicy.fileNameFor("img/recipes/default.webp"),
        )
        assertEquals(
            "dish-style-reference",
            DishImageStyleReferencePolicy.fileNameFor("img/recipes/default"),
        )
    }
}
