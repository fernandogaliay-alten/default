package com.sazon.app.ui.home

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenarios 2 y 3 de `docs/specs/home-offline-blocks.md`: avatar de la cabecera. El color resuelve
 * en la Taxonomia real (leida por la propiedad de sistema `taxonomy.asset.path`) y degrada al primer
 * `avatarColors` si el id no existe; las iniciales salen de `AvatarInitials`.
 */
class HomeIdentityTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun un_color_valido_usa_su_hex_de_la_taxonomia() {
        val identidad = HomeIdentity.from(perfil("Lucía", "terracotta"), taxonomy)

        assertEquals("#D9552B", identidad.hex)
        assertEquals("L", identidad.initials)
    }

    @Test
    fun un_color_inexistente_cae_al_primer_avatar_color() {
        val primero = taxonomy.avatarColors.first().hex
        val identidad = HomeIdentity.from(perfil("Lucía", "color-inexistente"), taxonomy)

        assertEquals(primero, identidad.hex)
    }

    @Test
    fun un_nombre_compuesto_toma_dos_iniciales() {
        val identidad = HomeIdentity.from(perfil("Lucía Gómez", "olive"), taxonomy)

        assertEquals("LG", identidad.initials)
        assertEquals("#5F7A3D", identidad.hex)
    }

    @Test
    fun un_nombre_vacio_no_tiene_iniciales() {
        val identidad = HomeIdentity.from(perfil("   ", "terracotta"), taxonomy)

        assertNull("Sin nombre no hay iniciales", identidad.initials)
    }
}

private fun perfil(displayName: String, avatarColorId: String) = UserProfile(
    displayName = displayName,
    avatarColorId = avatarColorId,
    createdAt = 1_700_000_000_000L,
    preferences = PreferenceProfile(
        dietPatternId = "omnivore",
        hardRestrictionIds = emptyList(),
        softPreferenceIds = emptyList(),
        cuisineIds = emptyList(),
        preferredIngredientIds = emptyList(),
        dislikedIngredientIds = emptyList(),
        difficultyId = "easy",
        timeBucketId = "lte_15",
        defaultServings = 2,
        updatedAt = 1_700_000_100_000L,
    ),
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
