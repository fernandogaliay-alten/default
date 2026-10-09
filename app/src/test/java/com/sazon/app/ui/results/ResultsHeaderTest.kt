package com.sazon.app.ui.results

import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Formateo del encabezado de Resultados (`docs/specs/results-screen.md`, escenario 6 y singular/plural):
 * origen Generador (franja con y sin `maxMinutes`, franja desconocida omitida) y origen Busqueda
 * (consulta entre comillas). Logica pura sin Compose ni Android.
 */
class ResultsHeaderTest {

    private val taxonomy: Taxonomy = TaxonomyJson.parse(taxonomyJson())

    @Test
    fun el_generador_usa_titulo_y_subtitulo_con_la_franja() {
        val header = ResultsHeaders.of(
            ResultsOrigin.Generator(ingredientCount = 6, timeBucketId = "lte_30", servings = 2),
            taxonomy,
        )

        assertEquals("Estas son tus recetas", header.title)
        assertEquals("A partir de 6 ingredientes · 30 min · 2 comensales", header.subtitle)
    }

    @Test
    fun el_generador_usa_singular_para_un_ingrediente_y_un_comensal() {
        val header = ResultsHeaders.of(
            ResultsOrigin.Generator(ingredientCount = 1, timeBucketId = "lte_15", servings = 1),
            taxonomy,
        )

        assertEquals("A partir de 1 ingrediente · 15 min · 1 comensal", header.subtitle)
    }

    @Test
    fun una_franja_sin_max_minutos_usa_su_etiqueta() {
        val header = ResultsHeaders.of(
            ResultsOrigin.Generator(ingredientCount = 2, timeBucketId = "gt_60", servings = 2),
            taxonomy,
        )

        assertEquals("A partir de 2 ingredientes · Más de 60 min · 2 comensales", header.subtitle)
    }

    @Test
    fun una_franja_desconocida_se_omite() {
        val header = ResultsHeaders.of(
            ResultsOrigin.Generator(ingredientCount = 2, timeBucketId = "inexistente", servings = 3),
            taxonomy,
        )

        assertEquals("A partir de 2 ingredientes · 3 comensales", header.subtitle)
    }

    @Test
    fun una_franja_ausente_se_omite() {
        val header = ResultsHeaders.of(
            ResultsOrigin.Generator(ingredientCount = 3, timeBucketId = null, servings = 4),
            taxonomy,
        )

        assertEquals("A partir de 3 ingredientes · 4 comensales", header.subtitle)
    }

    @Test
    fun la_busqueda_usa_el_titulo_y_la_consulta_entre_comillas() {
        val header = ResultsHeaders.of(ResultsOrigin.Search("algo con pollo y arroz"), taxonomy)

        assertEquals("Resultados de tu búsqueda", header.title)
        assertEquals("“algo con pollo y arroz”", header.subtitle)
    }

    @Test
    fun la_busqueda_recorta_la_consulta() {
        val header = ResultsHeaders.of(ResultsOrigin.Search("  algo con pollo y arroz  "), taxonomy)

        assertEquals("“algo con pollo y arroz”", header.subtitle)
    }
}

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}
