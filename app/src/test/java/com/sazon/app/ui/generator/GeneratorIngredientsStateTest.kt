package com.sazon.app.ui.generator

import com.sazon.app.domain.taxonomy.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-6 de `docs/specs/generator-ingredients-step.md`: logica pura del paso 1 del Generador
 * (anadir/quitar, dedupe, contador y habilitacion de "Continuar"). Sin Compose ni Android; se testea en
 * JVM. Incluye casos negativos que fallarian si la logica no hiciera nada (texto en blanco, duplicado
 * por nombre normalizado y quitar hasta cero).
 */
class GeneratorIngredientsStateTest {

    private val tomate = ingredient(id = "tomate", label = "Tomate")
    private val cebolla = ingredient(id = "cebolla", label = "Cebolla")

    @Test
    fun escenario_4_arranca_en_cero_y_no_permite_continuar() {
        val state = GeneratorIngredientsState()

        assertEquals(0, state.count)
        assertTrue(state.isEmpty)
        assertFalse("Con cero ingredientes no se puede continuar", state.canContinue)
        assertEquals("0 ingredientes añadidos", state.countLabel)
    }

    @Test
    fun escenario_1_toggle_taxonomy_anade_un_chip_y_habilita_continuar() {
        val state = GeneratorIngredientsState().toggleTaxonomy(tomate)

        assertEquals(1, state.count)
        assertEquals(listOf(GeneratorIngredient(name = "Tomate", ingredientId = "tomate")), state.ingredients)
        assertEquals("1 ingrediente añadido", state.countLabel)
        assertTrue("Con un ingrediente ya se puede continuar", state.canContinue)
    }

    @Test
    fun escenario_5_toggle_taxonomy_dos_veces_quita_el_mismo_ingrediente() {
        val conTomate = GeneratorIngredientsState().toggleTaxonomy(tomate)
        val sinTomate = conTomate.toggleTaxonomy(tomate)

        assertTrue("El segundo toque quita", sinTomate.isEmpty)
        assertFalse(sinTomate.canContinue)
        // No se guardan duplicados por ingredientId.
        assertTrue(conTomate.ingredients.none { it.ingredientId == null })
    }

    @Test
    fun escenario_2_add_free_anade_libre_con_id_nulo_y_vacia_la_query() {
        val state = GeneratorIngredientsState(query = "cebolla").addFree()

        assertEquals(1, state.count)
        assertEquals(
            listOf(GeneratorIngredient(name = "cebolla", ingredientId = null)),
            state.ingredients,
        )
        assertNull("El libre no tiene ingredientId", state.ingredients.single().ingredientId)
        assertEquals("El input se vacia", "", state.query)
    }

    @Test
    fun escenario_2_add_free_en_blanco_es_no_op() {
        val vacio = GeneratorIngredientsState(query = "   ")
        val trasBlanco = vacio.addFree()

        assertEquals("Un texto en blanco no anade nada", 0, trasBlanco.count)
        assertEquals("   ", trasBlanco.query)
        assertFalse(trasBlanco.canContinue)

        val vacioSinEspacios = GeneratorIngredientsState(query = "").addFree()
        assertEquals(0, vacioSinEspacios.count)
    }

    @Test
    fun escenario_5_un_libre_duplicado_por_nombre_normalizado_no_crea_chip() {
        val state = GeneratorIngredientsState()
            .let { it.copy(query = " cebolla ") }.addFree()
            .let { it.copy(query = "CEBOLLA") }.addFree()

        assertEquals("No se crean dos chips cebolla/CEBOLLA", 1, state.count)
        assertEquals("No se vacia la query cuando el duplicado se rechaza", "CEBOLLA", state.query)
    }

    @Test
    fun escenario_5_un_libre_duplicado_de_la_taxonomia_no_crea_chip() {
        val state = GeneratorIngredientsState()
            .toggleTaxonomy(tomate)
            .let { it.copy(query = "tomate") }.addFree()
            .let { it.copy(query = "TOMÁTE") }.addFree()

        assertEquals("tomate/TOMÁTE ya existe como chip de Taxonomia", 1, state.count)
        assertEquals("tomate", state.ingredients.single().ingredientId)
    }

    @Test
    fun escenario_3_remove_baja_el_contador() {
        val state = GeneratorIngredientsState()
            .toggleTaxonomy(tomate)
            .toggleTaxonomy(cebolla)

        assertEquals(2, state.count)

        val trasQuitar = state.remove(state.ingredients.first())

        assertEquals(1, trasQuitar.count)
        assertEquals("1 ingrediente añadido", trasQuitar.countLabel)
        assertEquals(listOf("cebolla"), trasQuitar.ingredients.map { it.ingredientId })
    }

    @Test
    fun escenario_3_4_quitar_el_ultimo_deja_sin_continuar() {
        val state = GeneratorIngredientsState().toggleTaxonomy(tomate)
        val sinNinguno = state.remove(state.ingredients.first())

        assertEquals(0, sinNinguno.count)
        assertEquals("0 ingredientes añadidos", sinNinguno.countLabel)
        assertFalse("Quitar todos deshabilita Continuar", sinNinguno.canContinue)
    }

    @Test
    fun escenario_6_count_label_singular_y_plural() {
        assertEquals("0 ingredientes añadidos", GeneratorIngredientsState().countLabel)
        assertEquals(
            "1 ingrediente añadido",
            GeneratorIngredientsState().toggleTaxonomy(tomate).countLabel,
        )
        assertEquals(
            "3 ingredientes añadidos",
            GeneratorIngredientsState()
                .toggleTaxonomy(tomate)
                .toggleTaxonomy(cebolla)
                .let { it.copy(query = "pimiento") }.addFree()
                .countLabel,
        )
    }

    @Test
    fun type_query_actualiza_la_consulta_sin_tocar_los_ingredientes() {
        val state = GeneratorIngredientsState()
            .toggleTaxonomy(tomate)
            .typeQuery("ceb")

        assertEquals("ceb", state.query)
        assertEquals(listOf("tomate"), state.ingredients.map { it.ingredientId })
    }

    @Test
    fun invariante_nunca_hay_dos_ingredientes_con_la_misma_identidad_normalizada() {
        val state = GeneratorIngredientsState()
            .toggleTaxonomy(tomate)
            .let { it.copy(query = "tomate") }.addFree()
            .let { it.copy(query = "  TOMATE ") }.addFree()
            .let { it.copy(query = "Tómate") }.addFree()
            .toggleTaxonomy(tomate) // lo quita
            .toggleTaxonomy(tomate) // lo vuelve a anadir
            .let { it.copy(query = "cebolla") }.addFree()

        val nombres = state.ingredients.map { normalize(it.name) }
        assertEquals("Sin nombres normalizados repetidos", nombres.distinct(), nombres)

        val ids = state.ingredients.mapNotNull { it.ingredientId }
        assertEquals("Sin ingredientId repetidos", ids.distinct(), ids)

        assertEquals(2, state.count)
        assertTrue(state.canContinue)
    }
}

private fun ingredient(id: String, label: String) = Ingredient(
    id = id,
    label = label,
    category = "vegetable",
    icon = id,
    allergens = emptyList(),
    tags = emptyList(),
)

private fun normalize(raw: String): String =
    raw.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
