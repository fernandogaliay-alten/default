package com.sazon.app.ui.preferences

import com.sazon.app.domain.taxonomy.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 1 (buscador) de `docs/specs/onboarding-ingredients-safety-kitchen.md`: filtro puro del
 * buscador del editor de ingredientes. Logica pura, sin Compose ni Android.
 */
class IngredientsFilterTest {

    private val ingredientes = listOf(
        ingrediente("tomate", "Tomate"),
        ingrediente("cebolla", "Cebolla"),
        ingrediente("pina", "Piña"),
        ingrediente("aguacate", "Aguacate"),
    )

    @Test
    fun consulta_vacia_devuelve_todas_las_opciones() {
        assertEquals(ingredientes, IngredientsFilter.matching(ingredientes, ""))
        assertEquals(ingredientes, IngredientsFilter.matching(ingredientes, "   "))
    }

    @Test
    fun coincide_por_subcadena_sin_importar_mayusculas() {
        assertEquals(
            listOf("tomate"),
            IngredientsFilter.matching(ingredientes, "TOM").map { it.id },
        )
        assertEquals(
            listOf("cebolla"),
            IngredientsFilter.matching(ingredientes, "ceb").map { it.id },
        )
    }

    @Test
    fun coincide_sin_importar_acentos_en_ambos_sentidos() {
        assertEquals(
            "Sin acento encuentra la etiqueta con acento",
            listOf("pina"),
            IngredientsFilter.matching(ingredientes, "pina").map { it.id },
        )
        assertEquals(
            "Con acento encuentra la etiqueta sin acento",
            listOf("pina"),
            IngredientsFilter.matching(ingredientes, "PÍÑA").map { it.id },
        )
    }

    @Test
    fun consulta_sin_coincidencias_devuelve_lista_vacia() {
        assertTrue(IngredientsFilter.matching(ingredientes, "zzz").isEmpty())
    }

    private fun ingrediente(id: String, label: String) = Ingredient(
        id = id,
        label = label,
        category = "verdura",
        icon = "x",
        allergens = emptyList(),
        tags = emptyList(),
    )
}
