package com.sazon.app.ui.generator

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mapeo puro del borrador del Generador a los tipos de dominio (escenario 4 y casos negativos).
 * Logica pura sin Compose ni Android.
 */
class GeneratorRequestFactoryTest {

    @Test
    fun ingredients_conserva_nombre_e_id_y_usa_cantidad_cero() {
        val state = GeneratorIngredientsState(
            ingredients = listOf(
                GeneratorIngredient(name = "Tomate", ingredientId = "tomate"),
                GeneratorIngredient(name = "Cebolla", ingredientId = null),
            ),
        )

        val mapped = GeneratorRequestFactory.ingredients(state)

        assertEquals(
            listOf(
                RecipeIngredient(name = "Tomate", quantity = 0.0, unit = null, isPantry = false, ingredientId = "tomate"),
                RecipeIngredient(name = "Cebolla", quantity = 0.0, unit = null, isPantry = false, ingredientId = null),
            ),
            mapped,
        )
        assertTrue("El input no lleva cantidad/unidad", mapped.all { it.quantity == 0.0 && it.unit == null })
        assertTrue("El input no es despensa", mapped.all { !it.isPantry })
    }

    @Test
    fun constraints_sustituye_las_restricciones_del_perfil_por_las_del_paso_de_ajustes() {
        val profile = perfil(hardRestrictions = listOf("lactosa"))
        val settings = GeneratorSettingsState(
            servings = 4,
            mealTypeId = "dinner",
            timeBucketId = "lte_30",
            difficultyId = "medium",
            hardRestrictionIds = listOf("gluten", "frutos_secos"),
            notes = "sin horno",
        )

        val constraints = GeneratorRequestFactory.constraints(settings, profile)

        assertEquals(listOf("gluten", "frutos_secos"), constraints.profile.hardRestrictionIds)
        assertEquals(4, constraints.servings)
        assertEquals("dinner", constraints.mealTypeId)
        assertEquals("lte_30", constraints.timeBucketId)
        assertEquals("medium", constraints.difficultyId)
        assertEquals("sin horno", constraints.notes)
        // El resto del Perfil se conserva (solo cambian las restricciones duras).
        assertEquals(profile.dietPatternId, constraints.profile.dietPatternId)
        assertEquals(profile.defaultServings, constraints.profile.defaultServings)
        assertEquals(profile.updatedAt, constraints.profile.updatedAt)
    }

    @Test
    fun notas_en_blanco_pasan_a_null() {
        val settings = GeneratorSettingsState(
            servings = 2,
            timeBucketId = "lte_15",
            difficultyId = "easy",
            notes = "   ",
        )

        assertNull(GeneratorRequestFactory.constraints(settings, perfil()).notes)
    }

    @Test
    fun caso_negativo_cambiar_las_restricciones_cambia_el_resultado() {
        val base = GeneratorSettingsState(
            servings = 2,
            timeBucketId = "lte_15",
            difficultyId = "easy",
        )

        val conGluten = GeneratorRequestFactory.constraints(
            base.copy(hardRestrictionIds = listOf("gluten")),
            perfil(),
        ).profile.hardRestrictionIds
        val sinGluten = GeneratorRequestFactory.constraints(
            base.copy(hardRestrictionIds = emptyList()),
            perfil(),
        ).profile.hardRestrictionIds

        assertEquals(listOf("gluten"), conGluten)
        assertTrue("Quitar la restriccion en los ajustes debe vaciar las constraints", sinGluten.isEmpty())
        assertNotEquals("El caso no pasa en vacio: el mapeo reacciona a los ajustes", conGluten, sinGluten)
    }
}

private fun perfil(hardRestrictions: List<String> = emptyList()) = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = hardRestrictions,
    softPreferenceIds = emptyList(),
    cuisineIds = emptyList(),
    preferredIngredientIds = emptyList(),
    dislikedIngredientIds = emptyList(),
    difficultyId = "easy",
    timeBucketId = "lte_15",
    defaultServings = 2,
    updatedAt = 1_600_000_000_000L,
)
