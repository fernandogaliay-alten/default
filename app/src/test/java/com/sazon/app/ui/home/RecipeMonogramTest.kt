package com.sazon.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Monograma de la miniatura 96x96 de la lista de Favoritos (`docs/specs/recipe-card-typographic.md`,
 * Escenario 5): hasta dos iniciales de las palabras significativas, saltando los conectores, y `null`
 * cuando el titulo no aporta ninguna.
 */
class RecipeMonogramTest {

    @Test
    fun los_vectores_de_las_capturas_dan_dos_iniciales() {
        assertEquals("TP", RecipeMonogram.of("Tortilla de patatas"))
        assertEquals("AC", RecipeMonogram.of("Arroz con pollo criollo"))
        assertEquals("GA", RecipeMonogram.of("Gazpacho andaluz"))
        assertEquals("CG", RecipeMonogram.of("Curry de garbanzos"))
        assertEquals("PT", RecipeMonogram.of("Pasta al tomate"))
    }

    @Test
    fun una_sola_palabra_significativa_da_una_inicial() {
        assertEquals("T", RecipeMonogram.of("Tortilla"))
    }

    @Test
    fun con_tres_o_mas_palabras_significativas_el_monograma_es_la_primera_y_la_ultima() {
        assertEquals("AC", RecipeMonogram.of("Arroz con pollo criollo"))
        assertEquals("TV", RecipeMonogram.of("Tofu salteado con verduras"))
        assertEquals("TP", RecipeMonogram.of("Tortitas de avena y plátano"))
    }

    @Test
    fun un_titulo_vacio_o_en_blanco_no_tiene_monograma() {
        assertNull(RecipeMonogram.of(""))
        assertNull(RecipeMonogram.of("   "))
        assertNull(RecipeMonogram.of("\t\n"))
    }

    @Test
    fun un_titulo_de_solo_conectores_no_tiene_monograma() {
        assertNull(RecipeMonogram.of("de la con al"))
    }

    @Test
    fun las_iniciales_van_en_mayuscula_y_se_toman_del_orden_declarado() {
        assertEquals("TP", RecipeMonogram.of("tortilla de patatas"))
        assertEquals("GA", RecipeMonogram.of("gazpacho andaluz"))
    }

    @Test
    fun los_signos_no_cuentan_y_el_monograma_es_determinista() {
        assertEquals("TP", RecipeMonogram.of("Tortilla, de patatas."))
        assertEquals(
            RecipeMonogram.of("Arroz con pollo criollo"),
            RecipeMonogram.of("Arroz con pollo criollo"),
        )
    }
}
