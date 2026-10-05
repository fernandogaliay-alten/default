package com.sazon.app.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceSignatureTest {

    @Test
    fun dos_perfiles_identicos_dan_la_misma_firma_y_es_estable() {
        val firmaA = PreferenceSignature.of(perfilDeReferencia())
        val firmaB = PreferenceSignature.of(perfilDeReferencia())

        assertEquals(firmaA, firmaB)
        // Recalcular sobre la misma instancia no cambia el resultado.
        assertEquals(firmaA, PreferenceSignature.of(perfilDeReferencia()))
    }

    @Test
    fun cada_campo_de_preferencias_altera_la_firma() {
        val referencia = perfilDeReferencia()
        val firmaReferencia = PreferenceSignature.of(referencia)

        val variantes = mapOf(
            "dietPatternId" to referencia.copy(dietPatternId = "vegetarian"),
            "hardRestrictionIds" to referencia.copy(
                hardRestrictionIds = referencia.hardRestrictionIds + "nuts",
            ),
            "softPreferenceIds" to referencia.copy(
                softPreferenceIds = referencia.softPreferenceIds + "sweet",
            ),
            "cuisineIds" to referencia.copy(cuisineIds = referencia.cuisineIds + "thai"),
            "preferredIngredientIds" to referencia.copy(
                preferredIngredientIds = referencia.preferredIngredientIds + "beef",
            ),
            "dislikedIngredientIds" to referencia.copy(
                dislikedIngredientIds = referencia.dislikedIngredientIds + "olives",
            ),
            "difficultyId" to referencia.copy(difficultyId = "hard"),
            "timeBucketId" to referencia.copy(timeBucketId = "under_60"),
            "defaultServings" to referencia.copy(defaultServings = 4),
        )

        for ((campo, variante) in variantes) {
            assertNotEquals(
                "El campo '$campo' deberia alterar la firma",
                firmaReferencia,
                PreferenceSignature.of(variante),
            )
        }
    }

    @Test
    fun anadir_o_quitar_un_elemento_de_una_lista_altera_la_firma() {
        val referencia = perfilDeReferencia()
        val firmaReferencia = PreferenceSignature.of(referencia)

        val conElementoDeMas = referencia.copy(
            hardRestrictionIds = referencia.hardRestrictionIds + "nuts",
        )
        val conElementoDeMenos = referencia.copy(
            hardRestrictionIds = listOf("gluten"),
        )

        assertNotEquals(firmaReferencia, PreferenceSignature.of(conElementoDeMas))
        assertNotEquals(firmaReferencia, PreferenceSignature.of(conElementoDeMenos))
    }

    @Test
    fun el_nombre_no_altera_la_firma() {
        val perfil = UserProfile(
            displayName = "Ana",
            avatarColorId = "coral",
            createdAt = 1_700_000_000_000L,
            preferences = perfilDeReferencia(),
        )
        val firmaBase = PreferenceSignature.of(perfil.preferences)

        val renombrado = perfil.copy(displayName = "Beatriz")

        assertEquals(firmaBase, PreferenceSignature.of(renombrado.preferences))
    }

    @Test
    fun el_color_de_avatar_no_altera_la_firma() {
        val perfil = UserProfile(
            displayName = "Ana",
            avatarColorId = "coral",
            createdAt = 1_700_000_000_000L,
            preferences = perfilDeReferencia(),
        )
        val firmaBase = PreferenceSignature.of(perfil.preferences)

        val conOtroAvatar = perfil.copy(avatarColorId = "teal")

        assertEquals(firmaBase, PreferenceSignature.of(conOtroAvatar.preferences))
    }

    @Test
    fun el_orden_y_los_duplicados_de_las_listas_no_alteran_la_firma() {
        val firmaReferencia = PreferenceSignature.of(perfilDeReferencia())

        val desordenado = perfilDeReferencia().copy(
            hardRestrictionIds = listOf("lactose", "gluten"),
            softPreferenceIds = listOf("smoky", "spicy"),
            cuisineIds = listOf("mexican", "italian"),
            preferredIngredientIds = listOf("tomato", "chicken"),
        )
        val conDuplicados = perfilDeReferencia().copy(
            hardRestrictionIds = listOf("gluten", "lactose", "gluten"),
            softPreferenceIds = listOf("spicy", "spicy", "smoky"),
            cuisineIds = listOf("italian", "italian", "mexican"),
            preferredIngredientIds = listOf("chicken", "tomato", "chicken"),
            dislikedIngredientIds = listOf("cilantro", "cilantro"),
        )

        assertEquals(firmaReferencia, PreferenceSignature.of(desordenado))
        assertEquals(firmaReferencia, PreferenceSignature.of(conDuplicados))
    }

    @Test
    fun los_sellos_de_tiempo_no_alteran_la_firma() {
        val perfil = UserProfile(
            displayName = "Ana",
            avatarColorId = "coral",
            createdAt = 1_700_000_000_000L,
            preferences = perfilDeReferencia(),
        )
        val firmaBase = PreferenceSignature.of(perfil.preferences)

        val otroUpdatedAt = perfil.copy(
            preferences = perfil.preferences.copy(updatedAt = 1_999_999_999_999L),
        )
        val otroCreatedAt = perfil.copy(createdAt = 1_999_999_999_999L)

        assertEquals(firmaBase, PreferenceSignature.of(otroUpdatedAt.preferences))
        assertEquals(firmaBase, PreferenceSignature.of(otroCreatedAt.preferences))
    }

    @Test
    fun la_firma_coincide_con_el_vector_dorado() {
        val firma = PreferenceSignature.of(perfilDeReferencia())

        assertEquals(
            "256cbcb3fae4784e9f89ab408d690a8ad184f36435fc276ca25e9b9b42197a8c",
            firma,
        )
        assertTrue("La firma debe ser SHA-256 en hex minuscula", firma.matches(Regex("[0-9a-f]{64}")))
    }
}

private fun perfilDeReferencia() = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = listOf("gluten", "lactose"),
    softPreferenceIds = listOf("spicy", "smoky"),
    cuisineIds = listOf("italian", "mexican"),
    preferredIngredientIds = listOf("chicken", "tomato"),
    dislikedIngredientIds = listOf("cilantro"),
    difficultyId = "easy",
    timeBucketId = "under_30",
    defaultServings = 2,
    updatedAt = 1_700_000_000_000L,
)
