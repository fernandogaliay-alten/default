package com.sazon.app.ui.onboarding

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-9 de `docs/specs/onboarding-ingredients-safety-kitchen.md`: logica pura del borrador de
 * los pasos 5-7 (ingredientes con exclusion mutua, restricciones duras con "Ninguna" y cocina con
 * recorte de comensales), con casos negativos que fallarian si una operacion no hiciera nada o tocara
 * otro campo. La retencion entre pasos es de `rememberSaveable` (capa Compose) y el acabado visual es
 * manual (AGENTS.md).
 */
class OnboardingIngredientsDraftTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun escenario_1_el_paso_5_arranca_vacio_con_opciones_de_la_taxonomia() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        assertTrue("Me gusta arranca vacio", draft.preferredIngredientIds.isEmpty())
        assertTrue("Evito arranca vacio", draft.dislikedIngredientIds.isEmpty())
        assertTrue("Hay ingredientes en la Taxonomia", taxonomy.ingredients.isNotEmpty())
        assertTrue(
            "Los ids de ingrediente son los de la Taxonomia",
            taxonomy.ingredients.all { taxonomy.ingredient(it.id) != null },
        )
    }

    @Test
    fun escenario_2_elegir_en_me_gusta_lo_quita_de_evito_y_viceversa() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        val enGusta = draft.togglePreferredIngredient("tomate")
        assertEquals(listOf("tomate"), enGusta.preferredIngredientIds)
        assertTrue("Elegirlo en 'Me gusta' no lo deja en 'Evito'", enGusta.dislikedIngredientIds.isEmpty())

        val enEvito = enGusta.toggleDislikedIngredient("tomate")
        assertTrue("Elegirlo en 'Evito' lo quita de 'Me gusta'", enEvito.preferredIngredientIds.isEmpty())
        assertEquals(listOf("tomate"), enEvito.dislikedIngredientIds)

        val otraVez = enEvito.togglePreferredIngredient("tomate")
        assertEquals(listOf("tomate"), otraVez.preferredIngredientIds)
        assertTrue("Vuelve a quitarlo de 'Evito'", otraVez.dislikedIngredientIds.isEmpty())
    }

    @Test
    fun escenario_3_alternar_dos_veces_deja_la_lista_como_estaba_sin_duplicados() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        val unaVez = draft.togglePreferredIngredient("tomate")
        val dosVeces = unaVez.togglePreferredIngredient("tomate")
        assertTrue("Alternar dos veces quita el ingrediente", dosVeces.preferredIngredientIds.isEmpty())

        val sinDuplicados = draft
            .togglePreferredIngredient("tomate")
            .togglePreferredIngredient("cebolla")
            .togglePreferredIngredient("tomate")
        assertEquals(listOf("cebolla"), sinDuplicados.preferredIngredientIds)
        assertEquals(
            "La alternancia usa semantica de conjunto",
            sinDuplicados.preferredIngredientIds.toSet().size,
            sinDuplicados.preferredIngredientIds.size,
        )
    }

    @Test
    fun escenario_4_caso_negativo_me_gusta_y_evito_no_se_solapan_nunca() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
            .togglePreferredIngredient("tomate")
            .toggleDislikedIngredient("cebolla")
            .toggleDislikedIngredient("tomate")
            .togglePreferredIngredient("ajo")
            .togglePreferredIngredient("cebolla")

        assertTrue(
            "La interseccion de ambos grupos es vacia",
            draft.preferredIngredientIds.intersect(draft.dislikedIngredientIds.toSet()).isEmpty(),
        )
        assertEquals(
            "Sin duplicados en 'Me gusta'",
            draft.preferredIngredientIds.toSet().size,
            draft.preferredIngredientIds.size,
        )
        assertEquals(
            "Sin duplicados en 'Evito'",
            draft.dislikedIngredientIds.toSet().size,
            draft.dislikedIngredientIds.size,
        )
    }

    @Test
    fun escenario_5_el_paso_6_ofrece_exactamente_las_restricciones_de_la_taxonomia() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        assertEquals(11, taxonomy.hardRestrictions.size)
        assertTrue("Sin restricciones, 'Ninguna' esta activa", draft.hasNoRestrictions)

        val marcada = draft.toggleHardRestriction("gluten")
        assertEquals(listOf("gluten"), marcada.hardRestrictionIds)
        assertFalse("Con una restriccion 'Ninguna' deja de estarlo", marcada.hasNoRestrictions)
        assertTrue("El id procede de la Taxonomia", taxonomy.restriction("gluten") != null)
    }

    @Test
    fun escenario_6_alternancia_de_restricciones_y_ninguna_sin_residuo() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
            .toggleHardRestriction("gluten")
            .toggleHardRestriction("lactose")
        assertEquals(2, draft.hardRestrictionIds.size)

        val ninguna = draft.clearHardRestrictions()
        assertTrue(ninguna.hardRestrictionIds.isEmpty())
        assertTrue(ninguna.hasNoRestrictions)

        val reabierta = ninguna.toggleHardRestriction("soy")
        assertEquals("Solo queda la restriccion nueva", listOf("soy"), reabierta.hardRestrictionIds)
        assertFalse(reabierta.hasNoRestrictions)

        val alternadaDosVeces = draft.toggleHardRestriction("gluten")
        assertEquals(listOf("lactose"), alternadaDosVeces.hardRestrictionIds)
    }

    @Test
    fun escenario_7_el_paso_7_arranca_con_los_valores_por_defecto_de_la_taxonomia() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        assertEquals(taxonomy.difficulties.first().id, draft.difficultyId)
        assertEquals("easy", draft.difficultyId)
        assertEquals(taxonomy.timeBuckets.first().id, draft.timeBucketId)
        assertEquals("lte_15", draft.timeBucketId)
        assertEquals(taxonomy.servings.default, draft.defaultServings)
        assertEquals(2, draft.defaultServings)
    }

    @Test
    fun escenario_8_paso_7_single_select_y_recorte_de_comensales() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        val otraDificultad = draft.selectDifficulty("hard")
        assertEquals("hard", otraDificultad.difficultyId)
        assertNotEquals("La eleccion reemplaza a la anterior", "easy", otraDificultad.difficultyId)

        val otraFranja = otraDificultad.selectTimeBucket("lte_30")
        assertEquals("lte_30", otraFranja.timeBucketId)
        assertNotEquals("La eleccion reemplaza a la anterior", "lte_15", otraFranja.timeBucketId)

        assertEquals("No baja de min", 1, draft.setServings(0, taxonomy.servings).defaultServings)
        assertEquals("No sube de max", 12, draft.setServings(99, taxonomy.servings).defaultServings)
        assertEquals("Dentro del rango se respeta", 6, draft.setServings(6, taxonomy.servings).defaultServings)
    }

    @Test
    fun escenario_9_caso_negativo_cada_paso_escribe_solo_sus_campos() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
            .selectDietPattern("vegan")
            .toggleSoftPreference("keto")
            .toggleCuisine("spanish")

        val conIngredientes = draft
            .togglePreferredIngredient("tomate")
            .toggleDislikedIngredient("cebolla")
        assertEquals("vegan", conIngredientes.dietPatternId)
        assertEquals(listOf("keto"), conIngredientes.softPreferenceIds)
        assertEquals(listOf("spanish"), conIngredientes.cuisineIds)
        assertEquals(listOf("tomate"), conIngredientes.preferredIngredientIds)
        assertEquals(listOf("cebolla"), conIngredientes.dislikedIngredientIds)

        val conRestriccion = conIngredientes.toggleHardRestriction("gluten")
        assertEquals("Cambiar restricciones no toca 'Me gusta'", listOf("tomate"), conRestriccion.preferredIngredientIds)
        assertEquals("Cambiar restricciones no toca 'Evito'", listOf("cebolla"), conRestriccion.dislikedIngredientIds)
        assertEquals("Cambiar restricciones no toca la dieta", "vegan", conRestriccion.dietPatternId)

        val conCocina = conRestriccion.selectDifficulty("hard").selectTimeBucket("gt_60")
        assertEquals(listOf("gluten"), conCocina.hardRestrictionIds)
        assertEquals(listOf("tomate"), conCocina.preferredIngredientIds)
        assertEquals(listOf("cebolla"), conCocina.dislikedIngredientIds)
        assertEquals("vegan", conCocina.dietPatternId)
        assertEquals(listOf("keto"), conCocina.softPreferenceIds)
        assertEquals(listOf("spanish"), conCocina.cuisineIds)
    }
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
