package com.sazon.app.ui.generator

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Microcopy puro del estado de carga: cabecera fija y sublineas derivadas de la Taxonomia y los
 * ajustes. Logica pura sin Compose ni Android.
 */
class GeneratorProgressMessagesTest {

    private val taxonomy = taxonomy()

    @Test
    fun la_cabecera_es_fija() {
        assertEquals("Buscando recetas para ti…", GeneratorProgressMessages.HEADLINE)
    }

    @Test
    fun la_sublinea_de_tiempo_usa_el_max_minutos_de_la_franja() {
        val lines = GeneratorProgressMessages.subLines(settings(timeBucketId = "lte_30"), taxonomy)

        assertTrue(lines.contains("Combinando tus ingredientes"))
        assertTrue("La franja de 30 min aporta su maximo", lines.contains("Ajustando a 30 minutos"))
    }

    @Test
    fun una_franja_sin_max_minutos_usa_la_sublinea_generica() {
        val lines = GeneratorProgressMessages.subLines(settings(timeBucketId = "gt_60"), taxonomy)

        assertTrue(lines.contains("Ajustando el tiempo"))
        assertFalse("No debe inventar un maximo", lines.any { it.startsWith("Ajustando a ") })
    }

    @Test
    fun la_sublinea_de_alergias_aparece_solo_con_restricciones() {
        val conRestricciones = GeneratorProgressMessages.subLines(
            settings(hardRestrictionIds = listOf("gluten")),
            taxonomy,
        )
        assertTrue(conRestricciones.contains("Evitando tus alergias…"))

        val sinRestricciones = GeneratorProgressMessages.subLines(settings(), taxonomy)
        assertFalse(sinRestricciones.contains("Evitando tus alergias…"))
    }
}

private fun settings(
    timeBucketId: String = "lte_15",
    hardRestrictionIds: List<String> = emptyList(),
) = GeneratorSettingsState(
    servings = 2,
    timeBucketId = timeBucketId,
    difficultyId = "easy",
    hardRestrictionIds = hardRestrictionIds,
)

private fun taxonomy(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}
