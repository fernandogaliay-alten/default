package com.sazon.app.ui.generator

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.taxonomy.Servings
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios 1-5 y 7 de `docs/specs/generator-settings-step.md`: logica pura del paso 2 del Generador
 * (precarga del Perfil, single-select que reemplaza, recorte del stepper, alergias con "Ninguna" y
 * notas). El escenario 6 (navegacion a Perfil) vive en `MainShell` y el 8 (conservacion del borrador)
 * en `rememberSaveable`; ambos son de la capa Compose/inspeccion. Sin Compose ni Android; se testea en
 * JVM. Incluye casos negativos que fallarian si la logica no hiciera nada o tocara otros campos.
 */
class GeneratorSettingsStateTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())
    private val limits: Servings = taxonomy.servings

    @Test
    fun escenario_1_initial_precarga_comensales_alergias_franja_y_dificultad_del_perfil() {
        val state = GeneratorSettingsState.initial(
            preferences = profile(
                defaultServings = 4,
                hardRestrictionIds = listOf("gluten"),
                difficultyId = "medium",
                timeBucketId = "lte_30",
            ),
            limits = limits,
        )

        assertEquals("El stepper muestra los comensales habituales", 4, state.servings)
        assertEquals(listOf("gluten"), state.hardRestrictionIds)
        assertEquals("medium", state.difficultyId)
        assertEquals("lte_30", state.timeBucketId)
        assertNull("El tipo de comida arranca sin preferencia", state.mealTypeId)
        assertEquals("", state.notes)
    }

    @Test
    fun escenario_1_un_default_servings_fuera_de_rango_se_recorta_al_de_la_taxonomia() {
        val alto = GeneratorSettingsState.initial(profile(defaultServings = 99), limits)
        val bajo = GeneratorSettingsState.initial(profile(defaultServings = 0), limits)

        assertEquals("No supera taxonomy.servings.max", limits.max, alto.servings)
        assertEquals("No baja de taxonomy.servings.min", limits.min, bajo.servings)
    }

    @Test
    fun escenario_2_los_cinco_tipos_de_comida_de_la_taxonomia_incluyen_postre() {
        assertEquals(5, taxonomy.mealTypes.size)
        assertTrue(
            "El tipo de comida sale siempre de la Taxonomia",
            taxonomy.mealTypes.all { taxonomy.mealType(it.id) != null },
        )
        assertEquals("Postre", taxonomy.mealType("dessert")?.label)
        assertEquals(
            listOf("breakfast", "lunch", "dinner", "snack", "dessert"),
            taxonomy.mealTypes.map { it.id },
        )
    }

    @Test
    fun escenario_2_tipo_de_comida_single_select_opcional() {
        val state = GeneratorSettingsState.initial(profile(), limits)

        val conPostre = state.selectMealType("dessert")
        assertEquals("dessert", conPostre.mealTypeId)

        val conComida = conPostre.selectMealType("lunch")
        assertEquals("La eleccion reemplaza a la anterior", "lunch", conComida.mealTypeId)

        val sinPreferencia = conComida.selectMealType("lunch")
        assertNull("Volver a pulsar el seleccionado lo deselecciona", sinPreferencia.mealTypeId)
    }

    @Test
    fun escenario_3_tiempo_y_dificultad_son_single_select_que_reemplazan() {
        val state = GeneratorSettingsState.initial(
            preferences = profile(difficultyId = "easy", timeBucketId = "lte_30"),
            limits = limits,
        )

        val conTiempo = state.selectTimeBucket("lte_45")
        assertEquals("lte_45", conTiempo.timeBucketId)
        assertEquals("La dificultad no se toca", "easy", conTiempo.difficultyId)

        val conDificultad = conTiempo.selectDifficulty("hard")
        assertEquals("hard", conDificultad.difficultyId)
        assertEquals("El tiempo elegido se conserva", "lte_45", conDificultad.timeBucketId)
        assertEquals(
            "Solo hay una franja marcada: la nueva reemplaza a la anterior",
            "lte_30",
            state.timeBucketId,
        )
    }

    @Test
    fun escenario_4_comensales_se_recortan_al_rango_de_la_taxonomia() {
        val state = GeneratorSettingsState.initial(profile(defaultServings = 2), limits)

        val hastaMax = (0..20).fold(state) { acc, _ -> acc.setServings(acc.servings + 1, limits) }
        assertEquals("El stepper nunca supera el maximo", limits.max, hastaMax.servings)

        val hastaMin = (0..20).fold(hastaMax) { acc, _ -> acc.setServings(acc.servings - 1, limits) }
        assertEquals("El stepper nunca baja del minimo", limits.min, hastaMin.servings)

        assertEquals("Dentro del rango se respeta", 6, state.setServings(6, limits).servings)
    }

    @Test
    fun escenario_5_alergias_multiselect_sin_duplicados_y_ninguna_deja_la_lista_vacia() {
        val state = GeneratorSettingsState.initial(
            preferences = profile(hardRestrictionIds = listOf("gluten")),
            limits = limits,
        )
        assertFalse("Con gluten marcado 'Ninguna' no esta activa", state.hasNoRestrictions)

        val conLactosa = state.toggleHardRestriction("lactose")
        assertEquals(listOf("gluten", "lactose"), conLactosa.hardRestrictionIds)

        val dosVeces = conLactosa.toggleHardRestriction("lactose")
        assertEquals("Alternar dos veces no duplica", listOf("gluten"), dosVeces.hardRestrictionIds)

        val ninguna = conLactosa.clearHardRestrictions()
        assertTrue("'Ninguna' deja la lista vacia", ninguna.hardRestrictionIds.isEmpty())
        assertTrue(ninguna.hasNoRestrictions)
    }

    @Test
    fun escenario_5_un_toggle_repetido_no_duplica_la_lista() {
        val state = GeneratorSettingsState.initial(profile(), limits)
            .toggleHardRestriction("gluten")
            .toggleHardRestriction("lactose")
            .toggleHardRestriction("gluten")
            .toggleHardRestriction("soy")

        assertEquals(listOf("lactose", "soy"), state.hardRestrictionIds)
        assertEquals(
            "Semantica de conjunto: sin ids repetidos",
            state.hardRestrictionIds.toSet().size,
            state.hardRestrictionIds.size,
        )
    }

    @Test
    fun escenario_7_las_notas_conservan_el_texto_tal_cual() {
        val multilinea = "Sin horno\nAprovechar las sobras…"
        val state = GeneratorSettingsState.initial(profile(), limits).typeNotes(multilinea)

        assertEquals(multilinea, state.notes)
        assertTrue("Se conservan los saltos de linea", state.notes.contains("\n"))

        val enBlanco = state.typeNotes("")
        assertEquals("Si queda en blanco guarda cadena vacia", "", enBlanco.notes)
    }

    @Test
    fun caso_negativo_cambiar_un_campo_no_toca_los_demas() {
        val state = GeneratorSettingsState.initial(
            preferences = profile(
                defaultServings = 3,
                hardRestrictionIds = listOf("gluten"),
                difficultyId = "medium",
                timeBucketId = "lte_30",
            ),
            limits = limits,
        ).selectMealType("dinner")

        val tocado = state
            .setServings(7, limits)
            .selectTimeBucket("gt_60")
            .selectDifficulty("hard")
            .toggleHardRestriction("lactose")
            .typeNotes("algo")

        assertEquals(7, tocado.servings)
        assertEquals("gt_60", tocado.timeBucketId)
        assertEquals("hard", tocado.difficultyId)
        assertEquals(listOf("gluten", "lactose"), tocado.hardRestrictionIds)
        assertEquals("algo", tocado.notes)
        assertEquals("El tipo de comida no se toca", "dinner", tocado.mealTypeId)
    }

    @Test
    fun caso_negativo_las_operaciones_son_inmutables() {
        val original = GeneratorSettingsState.initial(
            preferences = profile(defaultServings = 2, hardRestrictionIds = listOf("gluten")),
            limits = limits,
        )

        val mutado = original
            .setServings(8, limits)
            .selectMealType("dessert")
            .selectTimeBucket("lte_15")
            .selectDifficulty("hard")
            .toggleHardRestriction("soy")
            .typeNotes("nota")

        assertEquals("El estado original no cambia", 2, original.servings)
        assertNull(original.mealTypeId)
        assertEquals("lte_30", original.timeBucketId)
        assertEquals("medium", original.difficultyId)
        assertEquals(listOf("gluten"), original.hardRestrictionIds)
        assertEquals("", original.notes)

        assertEquals("El estado mutado conserva sus valores", "nota", mutado.notes)
        assertEquals(listOf("gluten", "soy"), mutado.hardRestrictionIds)
        assertTrue(mutado.clearHardRestrictions().hardRestrictionIds.isEmpty())
        assertEquals("clear() sobre el mutado no altera el original", listOf("gluten"), original.hardRestrictionIds)
    }
}

private fun profile(
    defaultServings: Int = 2,
    hardRestrictionIds: List<String> = emptyList(),
    difficultyId: String = "medium",
    timeBucketId: String = "lte_30",
) = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = hardRestrictionIds,
    softPreferenceIds = emptyList(),
    cuisineIds = emptyList(),
    preferredIngredientIds = emptyList(),
    dislikedIngredientIds = emptyList(),
    difficultyId = difficultyId,
    timeBucketId = timeBucketId,
    defaultServings = defaultServings,
    updatedAt = 0L,
)

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
