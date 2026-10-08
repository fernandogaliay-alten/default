package com.sazon.app.ui.onboarding

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-8 de `docs/specs/onboarding-diet-style-cuisines.md`: logica pura del borrador de los
 * pasos 2-4 (dieta single-select, estilo multi-select con "Ninguno" y cocinas multi-select), con
 * casos negativos que fallarian si una operacion no hiciera nada o tocara otro campo. La retencion
 * entre pasos es de `rememberSaveable` (capa Compose) y el acabado visual es manual (AGENTS.md).
 */
class OnboardingPreferencesDraftTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun escenario_1_la_dieta_es_seleccion_unica_y_reemplaza() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        val conVegano = draft.selectDietPattern("vegan")
        assertEquals("vegan", conVegano.dietPatternId)
        assertNotNull("El id elegido procede de la Taxonomia", taxonomy.dietPattern(conVegano.dietPatternId))

        val conVegetariano = conVegano.selectDietPattern("vegetarian")
        assertEquals("vegetarian", conVegetariano.dietPatternId)
        assertNotEquals("vegetarian", conVegano.dietPatternId)
    }

    @Test
    fun escenario_2_el_estilo_es_opcional_y_admite_ninguno() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
        assertTrue("Sin preferencias, 'Ninguno' esta marcado", draft.isStyleNone)

        val conPreferencia = draft.toggleSoftPreference("keto")
        assertFalse("Al marcar una preferencia 'Ninguno' deja de estarlo", conPreferencia.isStyleNone)

        val vaciado = conPreferencia.clearSoftPreferences()
        assertTrue("Pulsar 'Ninguno' vacia el estilo", vaciado.softPreferenceIds.isEmpty())
        assertTrue(vaciado.isStyleNone)
    }

    @Test
    fun escenario_3_alternar_preferencias_blandas_sin_duplicados() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        val unaVez = draft.toggleSoftPreference("keto")
        assertEquals(listOf("keto"), unaVez.softPreferenceIds)

        val dosVeces = unaVez.toggleSoftPreference("keto")
        assertTrue("Volver a pulsarla la quita", dosVeces.softPreferenceIds.isEmpty())

        val varias = draft
            .toggleSoftPreference("keto")
            .toggleSoftPreference("real_fooding")
        assertEquals(2, varias.softPreferenceIds.size)
        assertTrue(varias.softPreferenceIds.contains("keto"))
        assertTrue(varias.softPreferenceIds.contains("real_fooding"))
        assertEquals(
            "La alternancia usa semantica de conjunto (sin duplicados)",
            varias.softPreferenceIds.toSet().size,
            varias.softPreferenceIds.size,
        )
    }

    @Test
    fun escenario_4_alternar_cocinas_favoritas_desde_la_taxonomia() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        val una = draft.toggleCuisine("spanish")
        assertEquals(listOf("spanish"), una.cuisineIds)
        assertNotNull(taxonomy.cuisine("spanish"))

        val quitada = una.toggleCuisine("spanish")
        assertTrue(quitada.cuisineIds.isEmpty())

        val dos = una.toggleCuisine("italian")
        assertTrue(dos.cuisineIds.containsAll(listOf("spanish", "italian")))
        assertEquals(2, dos.cuisineIds.toSet().size)
    }

    @Test
    fun escenario_5_el_borrador_es_inmutable_y_conserva_las_elecciones() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
            .selectDietPattern("vegan")
            .toggleSoftPreference("keto")
            .toggleCuisine("spanish")

        val siguiente = draft.toggleSoftPreference("high_protein")

        assertNotSame(draft, siguiente)
        assertEquals("vegan", draft.dietPatternId)
        assertEquals(listOf("keto"), draft.softPreferenceIds)
        assertEquals(listOf("spanish"), draft.cuisineIds)
        assertEquals(2, siguiente.softPreferenceIds.size)
    }

    @Test
    fun escenario_6_el_borrador_arranca_con_valores_de_la_taxonomia() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)

        assertEquals(taxonomy.dietPatterns.first().id, draft.dietPatternId)
        assertEquals("omnivore", draft.dietPatternId)
        assertTrue("El estilo arranca vacio", draft.softPreferenceIds.isEmpty())
        assertTrue("Las cocinas arrancan vacias", draft.cuisineIds.isEmpty())
        assertTrue(taxonomy.dietPatterns.isNotEmpty())
        assertTrue(taxonomy.softPreferences.isNotEmpty())
        assertTrue(taxonomy.cuisines.isNotEmpty())
    }

    @Test
    fun escenario_7_caso_negativo_cada_campo_es_independiente() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
            .selectDietPattern("vegan")
            .toggleSoftPreference("keto")
            .toggleCuisine("spanish")

        val dietaCambiada = draft.selectDietPattern("vegetarian")
        assertEquals("vegetarian", dietaCambiada.dietPatternId)
        assertEquals("Cambiar la dieta no toca el estilo", listOf("keto"), dietaCambiada.softPreferenceIds)
        assertEquals("Cambiar la dieta no toca las cocinas", listOf("spanish"), dietaCambiada.cuisineIds)

        val cocinaAlternada = draft.toggleCuisine("italian")
        assertEquals("Alternar una cocina no toca el estilo", listOf("keto"), cocinaAlternada.softPreferenceIds)
        assertEquals("Alternar una cocina no toca la dieta", "vegan", cocinaAlternada.dietPatternId)
    }

    @Test
    fun escenario_8_caso_negativo_ninguno_no_deja_residuo() {
        val draft = OnboardingPreferencesDraft.initial(taxonomy)
            .toggleSoftPreference("keto")
            .toggleSoftPreference("real_fooding")
        assertEquals(2, draft.softPreferenceIds.size)

        val ninguno = draft.clearSoftPreferences()
        assertTrue(ninguno.softPreferenceIds.isEmpty())
        assertTrue(ninguno.isStyleNone)

        val reabierto = ninguno.toggleSoftPreference("low_calorie")
        assertEquals("Solo queda la preferencia nueva", listOf("low_calorie"), reabierto.softPreferenceIds)
        assertFalse(reabierto.isStyleNone)
    }
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
