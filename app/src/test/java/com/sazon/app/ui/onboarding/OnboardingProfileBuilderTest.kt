package com.sazon.app.ui.onboarding

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.ui.AppDestination
import com.sazon.app.ui.AppGate
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Escenarios 10 y 12 de `docs/specs/onboarding-ingredients-safety-kitchen.md`: el cierre construye un
 * [com.sazon.app.domain.profile.UserProfile] completo desde la identidad y el borrador, la puerta pasa
 * a `MAIN`, y un nombre invalido se rechaza. Logica pura, sin emulador.
 */
class OnboardingProfileBuilderTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun escenario_10_el_cierre_construye_el_perfil_completo_y_la_puerta_pasa_a_main() {
        val draft = borradorCompleto()
        val now = 1_700_000_000_000L

        val profile = OnboardingProfileBuilder.build(
            displayName = "  Lucía Gómez  ",
            avatarColorId = "olive",
            draft = draft,
            now = now,
        )

        assertEquals("El nombre se normaliza (recorte)", "Lucía Gómez", profile.displayName)
        assertEquals("olive", profile.avatarColorId)
        assertEquals("createdAt es el instante del cierre", now, profile.createdAt)
        assertEquals("updatedAt es el instante del cierre", now, profile.preferences.updatedAt)

        assertEquals("vegan", profile.preferences.dietPatternId)
        assertEquals(listOf("gluten"), profile.preferences.hardRestrictionIds)
        assertEquals(listOf("keto"), profile.preferences.softPreferenceIds)
        assertEquals(listOf("spanish"), profile.preferences.cuisineIds)
        assertEquals(listOf("tomate"), profile.preferences.preferredIngredientIds)
        assertEquals(listOf("cebolla"), profile.preferences.dislikedIngredientIds)
        assertEquals("hard", profile.preferences.difficultyId)
        assertEquals("gt_60", profile.preferences.timeBucketId)
        assertEquals(4, profile.preferences.defaultServings)

        assertEquals("Con el Perfil construido la puerta es MAIN", AppDestination.MAIN, AppGate.decide(profile))
    }

    @Test
    fun escenario_10_los_valores_por_defecto_del_borrador_tambien_se_copian() {
        val now = 42L
        val profile = OnboardingProfileBuilder.build(
            displayName = "Ana",
            avatarColorId = "terracotta",
            draft = OnboardingPreferencesDraft.initial(taxonomy),
            now = now,
        )

        assertEquals("omnivore", profile.preferences.dietPatternId)
        assertEquals("easy", profile.preferences.difficultyId)
        assertEquals("lte_15", profile.preferences.timeBucketId)
        assertEquals(2, profile.preferences.defaultServings)
        assertEquals(now, profile.createdAt)
        assertEquals(now, profile.preferences.updatedAt)
    }

    @Test
    fun escenario_12_caso_negativo_un_nombre_invalido_no_construye_el_perfil() {
        val draft = borradorCompleto()
        val now = 1_700_000_000_000L

        assertThrows(IllegalArgumentException::class.java) {
            OnboardingProfileBuilder.build("", "terracotta", draft, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingProfileBuilder.build("   ", "terracotta", draft, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingProfileBuilder.build("a".repeat(41), "terracotta", draft, now)
        }
    }

    @Test
    fun escenario_12_un_nombre_de_40_caracteres_tras_recortar_es_valido() {
        val profile = OnboardingProfileBuilder.build(
            displayName = "  " + "a".repeat(40) + "  ",
            avatarColorId = "terracotta",
            draft = OnboardingPreferencesDraft.initial(taxonomy),
            now = 7L,
        )

        assertEquals(40, profile.displayName.length)
    }

    private fun borradorCompleto(): OnboardingPreferencesDraft =
        OnboardingPreferencesDraft.initial(taxonomy)
            .selectDietPattern("vegan")
            .toggleSoftPreference("keto")
            .toggleCuisine("spanish")
            .togglePreferredIngredient("tomate")
            .toggleDislikedIngredient("cebolla")
            .toggleHardRestriction("gluten")
            .selectDifficulty("hard")
            .selectTimeBucket("gt_60")
            .setServings(4, taxonomy.servings)
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
