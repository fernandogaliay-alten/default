package com.sazon.app.ui.profile

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.PreferenceSignature
import com.sazon.app.domain.profile.UserProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.ui.onboarding.OnboardingPreferencesDraft
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 2-4, 8, 9 y 10 de `docs/specs/profile-screen.md`: logica pura del estado de edicion del
 * Perfil. Cubre que editar la identidad **no** cambia `preferences` (ni la firma) y que editar una
 * preferencia reconstruye el `PreferenceProfile` con `updatedAt = now` y firma distinta, con el mismo
 * resultado que produciria `OnboardingPreferencesDraft`, ademas de nombre invalido y color corrupto.
 * La capa Compose (hojas, muestras, acabado visual) es manual (AGENTS.md).
 */
class ProfileEditStateTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun from_conserva_el_nombre_normalizado_y_los_nueve_campos_del_borrador() {
        val profile = baseProfile()
        val state = ProfileEditState.from(profile, taxonomy)

        assertEquals("Lucía", state.displayName)
        assertEquals("terracotta", state.avatarColorId)
        assertEquals(profile.preferences.dietPatternId, state.preferencesDraft.dietPatternId)
        assertEquals(profile.preferences.hardRestrictionIds, state.preferencesDraft.hardRestrictionIds)
        assertEquals(profile.preferences.softPreferenceIds, state.preferencesDraft.softPreferenceIds)
        assertEquals(profile.preferences.cuisineIds, state.preferencesDraft.cuisineIds)
        assertEquals(profile.preferences.preferredIngredientIds, state.preferencesDraft.preferredIngredientIds)
        assertEquals(profile.preferences.dislikedIngredientIds, state.preferencesDraft.dislikedIngredientIds)
        assertEquals(profile.preferences.difficultyId, state.preferencesDraft.difficultyId)
        assertEquals(profile.preferences.timeBucketId, state.preferencesDraft.timeBucketId)
        assertEquals(profile.preferences.defaultServings, state.preferencesDraft.defaultServings)
        // El borrador no arrastra updatedAt (no es una preferencia).
        assertEquals(
            "Sin cambios, toProfile reproduce el PreferenceProfile original",
            profile.preferences,
            state.toProfile(profile, now = 9_999L).preferences,
        )
    }

    @Test
    fun escenario_2_editar_el_nombre_lo_normaliza_y_exige_nombre_valido() {
        val base = baseProfile()
        val state = ProfileEditState.from(base, taxonomy).withDisplayName("  Ana  ")

        val updated = state.toProfile(base, now = 2_000L)
        assertEquals("Ana", updated.displayName)
        assertEquals("terracotta", updated.avatarColorId)
        assertEquals(base.createdAt, updated.createdAt)
        assertEquals("Mismo PreferenceProfile (incluido updatedAt)", base.preferences, updated.preferences)

        listOf("", "   ", "a".repeat(41)).forEach { invalid ->
            val invalidState = ProfileEditState.from(base, taxonomy).withDisplayName(invalid)
            assertFalse("'$invalid' no es un nombre valido", invalidState.isNameValid)
            assertThrows(IllegalArgumentException::class.java) {
                invalidState.toProfile(base, now = 2_000L)
            }
        }

        assertTrue(ProfileEditState.from(base, taxonomy).withDisplayName("a".repeat(40)).isNameValid)
    }

    @Test
    fun escenario_3_editar_el_color_de_avatar_no_toca_las_preferencias() {
        val base = baseProfile()
        val state = ProfileEditState.from(base, taxonomy).withAvatarColorId("olive")

        val updated = state.toProfile(base, now = 2_000L)
        assertEquals("olive", updated.avatarColorId)
        assertEquals("Lucía", updated.displayName)
        assertEquals("El PreferenceProfile se conserva entero, incluso updatedAt", base.preferences, updated.preferences)
    }

    @Test
    fun escenario_4_cambiar_la_identidad_no_invalida_el_lote_y_una_preferencia_si() {
        val base = baseProfile()
        val baseSignature = PreferenceSignature.of(base.preferences)

        val soloNombre = ProfileEditState.from(base, taxonomy)
            .withDisplayName("Otra")
            .toProfile(base, now = 2_000L)
        assertEquals(baseSignature, PreferenceSignature.of(soloNombre.preferences))

        val soloAvatar = ProfileEditState.from(base, taxonomy)
            .withAvatarColorId("olive")
            .toProfile(base, now = 2_000L)
        assertEquals(baseSignature, PreferenceSignature.of(soloAvatar.preferences))

        // Caso negativo: un cambio de preferencia SI cambia la firma.
        val conPreferencia = ProfileEditState.from(base, taxonomy)
            .withPreferencesDraft(OnboardingPreferencesDraft.from(base.preferences).toggleCuisine("italian"))
            .toProfile(base, now = 2_000L)
        assertNotEquals(baseSignature, PreferenceSignature.of(conPreferencia.preferences))
    }

    @Test
    fun escenario_6_editar_una_cocina_se_refleja_igual_que_en_el_onboarding() {
        val base = baseProfile()
        val draft = OnboardingPreferencesDraft.from(base.preferences).toggleCuisine("italian")

        val updated = ProfileEditState.from(base, taxonomy)
            .withPreferencesDraft(draft)
            .toProfile(base, now = 3_000L)

        assertEquals(listOf("mediterranean", "italian"), updated.preferences.cuisineIds)
        assertEquals(3_000L, updated.preferences.updatedAt)
        assertEquals(
            "El resultado coincide con el motor compartido del onboarding",
            draft.toPreferences(3_000L),
            updated.preferences,
        )
    }

    @Test
    fun escenario_7_cada_campo_de_preferencias_actualiza_updatedAt_y_cambia_la_firma() {
        val base = baseProfile()
        val baseSignature = PreferenceSignature.of(base.preferences)
        val now = 7_777L

        val drafts = listOf(
            "dietPatternId" to OnboardingPreferencesDraft.from(base.preferences).selectDietPattern("vegan"),
            "hardRestrictionIds" to OnboardingPreferencesDraft.from(base.preferences)
                .toggleHardRestriction("lactose"),
            "softPreferenceIds" to OnboardingPreferencesDraft.from(base.preferences)
                .toggleSoftPreference("high_protein"),
            "cuisineIds" to OnboardingPreferencesDraft.from(base.preferences).toggleCuisine("italian"),
            "preferredIngredientIds" to OnboardingPreferencesDraft.from(base.preferences)
                .togglePreferredIngredient("arroz"),
            "dislikedIngredientIds" to OnboardingPreferencesDraft.from(base.preferences)
                .toggleDislikedIngredient("pimiento"),
            "difficultyId" to OnboardingPreferencesDraft.from(base.preferences).selectDifficulty("medium"),
            "timeBucketId" to OnboardingPreferencesDraft.from(base.preferences).selectTimeBucket("lte_30"),
            "defaultServings" to OnboardingPreferencesDraft.from(base.preferences)
                .setServings(4, taxonomy.servings),
        )

        drafts.forEach { (campo, draft) ->
            val updated = ProfileEditState.from(base, taxonomy)
                .withPreferencesDraft(draft)
                .toProfile(base, now)

            assertEquals("$campo: updatedAt avanza", now, updated.preferences.updatedAt)
            assertNotEquals("$campo: la firma cambia", baseSignature, PreferenceSignature.of(updated.preferences))
            assertEquals(
                "$campo: mismo resultado que OnboardingPreferencesDraft",
                draft.toPreferences(now),
                updated.preferences,
            )
        }
    }

    @Test
    fun escenario_8_se_mantienen_las_reglas_de_seleccion_del_onboarding() {
        val base = baseProfile()

        val sinEstilo = OnboardingPreferencesDraft.from(base.preferences)
            .toggleSoftPreference("high_protein")
            .clearSoftPreferences()
        assertTrue(sinEstilo.softPreferenceIds.isEmpty())
        assertTrue(sinEstilo.isStyleNone)

        val sinRestricciones = OnboardingPreferencesDraft.from(base.preferences).clearHardRestrictions()
        assertTrue(sinRestricciones.hardRestrictionIds.isEmpty())
        assertTrue(sinRestricciones.hasNoRestrictions)

        // Exclusion mutua: "cebolla" esta en "Evito"; elegirla en "Me gusta" la mueve.
        val movido = OnboardingPreferencesDraft.from(base.preferences).togglePreferredIngredient("cebolla")
        assertTrue(movido.preferredIngredientIds.contains("cebolla"))
        assertFalse(movido.dislikedIngredientIds.contains("cebolla"))

        val vuelta = movido.toggleDislikedIngredient("cebolla")
        assertTrue(vuelta.dislikedIngredientIds.contains("cebolla"))
        assertFalse(vuelta.preferredIngredientIds.contains("cebolla"))

        // Single-select reemplaza.
        assertEquals("vegan", OnboardingPreferencesDraft.from(base.preferences).selectDietPattern("vegan").dietPatternId)

        // Comensales recortados al rango de la Taxonomia.
        assertEquals(
            taxonomy.servings.max,
            OnboardingPreferencesDraft.from(base.preferences)
                .setServings(999, taxonomy.servings)
                .defaultServings,
        )
        assertEquals(
            taxonomy.servings.min,
            OnboardingPreferencesDraft.from(base.preferences)
                .setServings(0, taxonomy.servings)
                .defaultServings,
        )
    }

    @Test
    fun escenario_9_caso_negativo_identidad_y_preferencias_son_independientes() {
        val base = baseProfile()

        // Cambiar solo la preferencia no toca la identidad.
        val preferencia = ProfileEditState.from(base, taxonomy)
            .withPreferencesDraft(OnboardingPreferencesDraft.from(base.preferences).selectDietPattern("vegan"))
            .toProfile(base, now = 4_000L)
        assertEquals("Lucía", preferencia.displayName)
        assertEquals("terracotta", preferencia.avatarColorId)
        assertEquals(base.createdAt, preferencia.createdAt)
        assertEquals("vegan", preferencia.preferences.dietPatternId)

        // Cambiar solo la identidad no toca ninguna preferencia.
        val identidad = ProfileEditState.from(base, taxonomy)
            .withDisplayName("Otra")
            .withAvatarColorId("olive")
            .toProfile(base, now = 4_000L)
        assertEquals("Otra", identidad.displayName)
        assertEquals("olive", identidad.avatarColorId)
        assertEquals(base.preferences, identidad.preferences)
    }

    @Test
    fun escenario_10_caso_negativo_color_de_avatar_corrupto_se_degrada() {
        val corrupto = baseProfile().copy(avatarColorId = "color-inexistente")

        val state = ProfileEditState.from(corrupto, taxonomy)

        assertEquals("Resuelve el primer avatarColors de la Taxonomia", taxonomy.avatarColors.first().id, state.avatarColorId)
        assertNotEquals(corrupto.avatarColorId, state.avatarColorId)

        // Editar solo el nombre desde un color corrupto sigue sin tocar las preferencias.
        val updated = state.withDisplayName("Ana").toProfile(corrupto, now = 2_000L)
        assertEquals(taxonomy.avatarColors.first().id, updated.avatarColorId)
        assertEquals(corrupto.preferences, updated.preferences)
    }

    private fun baseProfile(updatedAt: Long = 5_000L): UserProfile =
        UserProfile(
            displayName = "Lucía",
            avatarColorId = "terracotta",
            createdAt = 1_000L,
            preferences = PreferenceProfile(
                dietPatternId = "omnivore",
                hardRestrictionIds = listOf("gluten"),
                softPreferenceIds = listOf("keto"),
                cuisineIds = listOf("mediterranean"),
                preferredIngredientIds = listOf("tomate"),
                dislikedIngredientIds = listOf("cebolla"),
                difficultyId = "easy",
                timeBucketId = "lte_15",
                defaultServings = 2,
                updatedAt = updatedAt,
            ),
        )
}

private fun assertThrows(expected: Class<out Throwable>, block: () -> Unit) {
    try {
        block()
    } catch (error: Throwable) {
        if (expected.isInstance(error)) return
        throw AssertionError("Se esperaba ${expected.simpleName} pero se lanzo ${error::class.java.simpleName}", error)
    }
    throw AssertionError("Se esperaba ${expected.simpleName} pero no se lanzo ninguna excepcion")
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
