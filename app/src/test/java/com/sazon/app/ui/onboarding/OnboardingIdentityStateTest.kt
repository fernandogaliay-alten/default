package com.sazon.app.ui.onboarding

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 2 y 6 de `docs/specs/onboarding-shell-and-identity.md`: estado del paso 1. La validez
 * del nombre sale de `ProfileName` y el color por defecto, del primer `avatarColors` de la
 * Taxonomia real.
 */
class OnboardingIdentityStateTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun el_estado_inicial_usa_el_primer_color_de_la_taxonomia() {
        val inicial = OnboardingIdentityState.initial(taxonomy)

        assertEquals("terracotta", inicial.avatarColorId)
        assertEquals("", inicial.name)
        assertFalse("Sin nombre no se puede continuar", inicial.canProceed)
        assertNull("Sin nombre no hay iniciales", inicial.initials)
    }

    @Test
    fun un_nombre_vacio_o_solo_espacios_no_permite_continuar() {
        assertFalse(OnboardingIdentityState("", "terracotta").canProceed)
        assertFalse(OnboardingIdentityState("   ", "terracotta").canProceed)
        assertFalse(OnboardingIdentityState("", "terracotta").isNameValid)
    }

    @Test
    fun un_nombre_valido_permite_continuar() {
        val estado = OnboardingIdentityState("Lucía", "terracotta")

        assertTrue(estado.canProceed)
        assertTrue(estado.isNameValid)
        assertEquals("Lucía", estado.normalizedName)
        assertEquals("L", estado.initials)
    }

    @Test
    fun un_nombre_de_40_caracteres_es_valido_y_uno_de_41_no() {
        assertTrue(OnboardingIdentityState("a".repeat(40), "terracotta").canProceed)
        assertFalse(OnboardingIdentityState("a".repeat(41), "terracotta").canProceed)
    }

    @Test
    fun el_nombre_se_normaliza_al_recortar_espacios() {
        val estado = OnboardingIdentityState("  Ana  ", "terracotta")

        assertEquals("Ana", estado.normalizedName)
        assertTrue(estado.canProceed)
    }

    @Test
    fun cambiar_el_color_se_refleja_en_el_estado() {
        val estado = OnboardingIdentityState("Ana", "olive")

        assertEquals("olive", estado.avatarColorId)
        assertEquals("#5F7A3D", taxonomy.avatarColor(estado.avatarColorId)?.hex)
    }
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
