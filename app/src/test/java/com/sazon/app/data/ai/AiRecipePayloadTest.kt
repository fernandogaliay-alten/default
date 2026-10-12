package com.sazon.app.data.ai

import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import com.sazon.app.domain.validation.DiscardReason
import com.sazon.app.domain.validation.RecipeConstraints
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decode tolerante de `AiRecipePayload.decode` con la **forma real** del contrato de la IA
 * (`docs/domain/taxonomy.md` §4): JSON **literal sin `id` ni `source`** (los posee la app).
 *
 * Es el punto ciego que ocultaba el bug de #70: los fixtures se fabricaban con
 * `RecipeJson.encode(recipe)`, que **si** incluye `id` y `source` y por eso no reproducia lo que el
 * modelo envia. Aqui los cuerpos son literales del contrato (nunca `RecipeJson.encode`).
 *
 * Ademas del resultado (`accepted`), se prueba el **diagnostico** que la correctiva
 * `ai-call-diagnostics` (issue #72) anade al decode: `received`/`accepted`/`discardedByReason` (los
 * cuatro `DiscardReason`) y `contentState`/longitudes del contenido.
 *
 * Taxonomia real. El proveedor **no** pre-filtra por dieta/restricciones (restricciones neutras).
 */
class AiRecipePayloadTest {

    private val taxonomy = taxonomy()

    private fun decode(
        content: String?,
        constraints: RecipeConstraints = AiProviderPolicy.NEUTRAL_CONSTRAINTS,
    ): AiDecodeResult = AiRecipePayload.decode(content, taxonomy, constraints)

    // Escenario 1: forma real del contrato -> 1 receta valida, con source/id completados.
    @Test
    fun un_contrato_valido_devuelve_las_recetas() {
        val result = decode(envelope(arrozDraft()))

        assertEquals(1, result.accepted.size)
        val recipe = result.accepted.single()
        assertEquals("Arroz salteado con verduras", recipe.title)
        assertEquals("Un arroz rapido con verduras.", recipe.description)
        assertEquals("rice", recipe.categoryId)
        assertEquals("lunch", recipe.mealTypeId)
        assertEquals("asian", recipe.cuisineId)
        assertEquals(25, recipe.totalMinutes)
        assertEquals(2, recipe.servings)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(2, recipe.steps.size)
        assertEquals(RecipeSource.AI, recipe.source)
        assertTrue("El id lo completa la app", recipe.id.startsWith(AiRecipeIds.PREFIX))

        assertEquals(AiContentState.RECIPES, result.contentState)
        assertEquals(1, result.received)
        assertTrue("Sin descartes en un lote valido", result.discardedByReason.isEmpty())
        assertTrue("El contenido leido tiene longitud real", result.contentChars > 0)
        assertTrue("El contenido leido tiene bytes reales", result.contentBytes > 0)
    }

    @Test
    fun una_receta_rota_dentro_de_un_lote_legible_no_tumba_el_resto() {
        val result = decode(envelope(arrozDraft(), """{"title":"Rota"}"""))

        assertEquals(1, result.accepted.size)
        assertEquals("Arroz salteado con verduras", result.accepted.single().title)
        assertEquals("El elemento roto tambien cuenta como recibido", 2, result.received)
        assertEquals(mapOf(DiscardReason.SCHEMA to 1), result.discardedByReason)
    }

    @Test
    fun una_valla_markdown_envolvente_se_quita() {
        val fenced = "```json\n${envelope(arrozDraft())}\n```"

        val result = decode(fenced)

        assertEquals(1, result.accepted.size)
        assertEquals("Arroz salteado con verduras", result.accepted.single().title)
        assertEquals(AiContentState.RECIPES, result.contentState)
    }

    // Escenario 5: tolerancia preservada (nunca lanza ni reintenta).
    @Test
    fun las_respuestas_ilegibles_o_vacias_devuelven_lista_vacia_sin_lanzar() {
        val cases = listOf(
            "lo siento, no puedo ayudarte",
            "",
            "   ",
            "{\"foo\":1}",
            "[1,2]",
            "{\"recipes\":3}",
            "no es json",
        )

        cases.forEach { content ->
            val result = decode(content)
            assertTrue("El contenido '$content' debe dar lista vacia", result.accepted.isEmpty())
            assertEquals("El contenido '$content' no tiene recetas", 0, result.received)
            assertTrue("El contenido '$content' no descarta recetas", result.discardedByReason.isEmpty())
            assertTrue(
                "El contenido '$content' es blanco o ilegible",
                result.contentState == AiContentState.BLANK || result.contentState == AiContentState.ILLEGIBLE,
            )
        }
        val nullResult = decode(null)
        assertTrue("null debe dar lista vacia", nullResult.accepted.isEmpty())
        assertEquals(AiContentState.BLANK, nullResult.contentState)
    }

    // Escenario 2: EMPTY por contenido en blanco o ilegible, con longitudes coherentes (0/0 vs > 0).
    @Test
    fun el_estado_del_contenido_y_las_longitudes_distinguen_blanco_ilegible_y_recetas() {
        listOf(null, "", "   ", "\n").forEach { blank ->
            val result = decode(blank)
            assertEquals(AiContentState.BLANK, result.contentState)
            assertEquals(0, result.contentChars)
            assertEquals(0, result.contentBytes)
            assertEquals(0, result.received)
        }

        val illegible = decode("lo siento, no puedo ayudarte")
        assertEquals(AiContentState.ILLEGIBLE, illegible.contentState)
        assertEquals(28, illegible.contentChars)
        assertEquals(28, illegible.contentBytes)

        // `contentChars` son unidades UTF-16 y `contentBytes` UTF-8: el emoji lo distingue.
        val unicode = decode("sí, 🤖")
        assertEquals(AiContentState.ILLEGIBLE, unicode.contentState)
        assertEquals(6, unicode.contentChars)
        assertEquals(9, unicode.contentBytes)

        val recipes = decode(envelope(arrozDraft()))
        assertEquals(AiContentState.RECIPES, recipes.contentState)
        assertTrue(recipes.contentChars > 0 && recipes.contentBytes > 0)
    }

    @Test
    fun el_proveedor_no_prefiltra_por_dieta_ni_restricciones_duras() {
        // Con restricciones neutras, el pollo (tag poultry) y el "Pan" libre (gluten) llegan tal cual:
        // el filtrado por Perfil es del llamador (asi el estado Bloqueado de ai-search sigue vivo).
        val result = decode(envelope(arrozDraft(), polloDraft(), bocadilloDraft()))

        assertEquals(3, result.accepted.size)
        assertTrue(result.accepted.any { it.title == "Pollo al ajillo" })
        assertTrue(result.accepted.any { it.title == "Bocadillo de tomate" })
        assertTrue("Sin filtrado no hay descartes", result.discardedByReason.isEmpty())
    }

    // Escenario 2: id unico en la llamada y source = ai, incluso con una receta repetida literal.
    @Test
    fun cada_receta_recibe_un_id_unico_y_source_ai_con_un_duplicado_literal() {
        val result = decode(envelope(arrozDraft(), polloDraft(), arrozDraft()))

        assertEquals(3, result.accepted.size)
        assertTrue("Todas son ai", result.accepted.all { it.source == RecipeSource.AI })
        assertTrue(
            "Todas con id no vacio y prefijo ai-",
            result.accepted.all { it.id.startsWith(AiRecipeIds.PREFIX) },
        )
        assertEquals("No puede haber dos ids iguales", 3, result.accepted.map { it.id }.toSet().size)
        assertTrue(
            "El duplicado literal se desambigua",
            result.accepted.map { it.id }.any { it.endsWith("-2") },
        )
    }

    // Escenario 4: las reglas del Validador siguen aplicandose y el elemento roto no tumba el resto.
    @Test
    fun las_reglas_del_validador_siguen_aplicandose_y_la_valida_sobrevive() {
        val result = decode(
            envelope(
                draftJson(title = "Comida inexistente", mealTypeId = "second_breakfast"),
                draftJson(title = "Cocina inexistente", cuisineId = "martian"),
                draftJson(title = "Dificultad inexistente", difficultyId = "impossible"),
                // Los pasos del contrato suman 25 min: totalMinutes 60 deja una deriva > 5 min.
                draftJson(title = "Tiempos incoherentes", totalMinutes = 60, prepMinutes = 20, cookMinutes = 40),
                draftJson(title = "Receta valida"),
            ),
        )

        assertEquals(1, result.accepted.size)
        assertEquals("Receta valida", result.accepted.single().title)
        assertEquals(5, result.received)
        assertEquals(
            mapOf(DiscardReason.SCHEMA to 3, DiscardReason.INCOHERENT to 1),
            result.discardedByReason,
        )
    }

    // Escenario 6: matchScore lo sigue derivando la app (el del modelo se ignora).
    @Test
    fun el_matchscore_del_contrato_se_ignora_y_la_receta_sale_sin_score() {
        val result = decode(envelope(draftJson(matchScore = 92)))

        assertEquals(1, result.accepted.size)
        assertNull("El score es derivado y no persistido", result.accepted.single().matchScore)
    }

    // Escenario 3 (diagnostico): el resumen del decode cuenta recibidas, aceptadas y motivos, en el
    // orden de `DiscardReason`.
    @Test
    fun el_resumen_del_decode_cuenta_recibidas_aceptadas_y_motivos_del_descarte() {
        val result = decode(
            envelope(
                draftJson(title = "Comida inexistente", mealTypeId = "second_breakfast"),
                draftJson(title = "Tiempos incoherentes", totalMinutes = 60, prepMinutes = 20, cookMinutes = 40),
                arrozDraft(),
            ),
        )

        assertEquals(3, result.received)
        assertEquals(1, result.accepted.size)
        assertEquals(
            mapOf(DiscardReason.SCHEMA to 1, DiscardReason.INCOHERENT to 1),
            result.discardedByReason,
        )
        assertEquals(
            "Los motivos van en el orden del enum",
            listOf(DiscardReason.SCHEMA, DiscardReason.INCOHERENT),
            result.discardedByReason.keys.toList(),
        )
    }

    // Escenario 3: con restricciones **no neutras** el resumen expresa tambien DIET_PATTERN y
    // HARD_RESTRICTION: los **cuatro** DiscardReason son representables.
    @Test
    fun las_restricciones_no_neutras_permiten_representar_los_cuatro_motivos_de_descarte() {
        val veganYGluten = RecipeConstraints(dietPatternId = "vegan", hardRestrictionIds = setOf("gluten"))

        val result = decode(
            envelope(
                """{"title":"Rota"}""",
                polloDraft(),
                bocadilloDraft(),
                draftJson(title = "Tiempos incoherentes", totalMinutes = 60, prepMinutes = 20, cookMinutes = 40),
                arrozDraft(),
            ),
            constraints = veganYGluten,
        )

        assertEquals(5, result.received)
        assertEquals(1, result.accepted.size)
        assertEquals("Arroz salteado con verduras", result.accepted.single().title)
        assertEquals(
            "Los cuatro motivos, en el orden del enum",
            listOf(
                DiscardReason.SCHEMA,
                DiscardReason.DIET_PATTERN,
                DiscardReason.HARD_RESTRICTION,
                DiscardReason.INCOHERENT,
            ),
            result.discardedByReason.keys.toList(),
        )
        assertEquals(1, result.discardedByReason[DiscardReason.SCHEMA])
        assertEquals(1, result.discardedByReason[DiscardReason.DIET_PATTERN])
        assertEquals(1, result.discardedByReason[DiscardReason.HARD_RESTRICTION])
        assertEquals(1, result.discardedByReason[DiscardReason.INCOHERENT])
    }

    private fun envelope(vararg draftJson: String): String =
        """{"recipes":[${draftJson.joinToString(",")}]}"""

    private fun taxonomy(): Taxonomy {
        val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
            "Falta la propiedad de sistema taxonomy.asset.path"
        }
        return TaxonomyJson.parse(File(path).readText())
    }
}

private const val ARROZ_INGREDIENTS =
    """[{"name":"Arroz","quantity":200,"unit":"g","isPantry":false,"ingredientId":"arroz"},{"name":"Zanahoria","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"zanahoria"}]"""
private const val ARROZ_STEPS =
    """[{"order":1,"title":"Cocer el arroz","description":"Cuece el arroz.","minutes":12},{"order":2,"title":"Saltear","description":"Saltea las verduras.","minutes":13}]"""
private const val POLLO_INGREDIENTS =
    """[{"name":"Pollo","quantity":500,"unit":"g","isPantry":false,"ingredientId":"pollo"},{"name":"Ajo","quantity":4,"unit":"ud","isPantry":false,"ingredientId":"ajo"}]"""
private const val POLLO_STEPS =
    """[{"order":1,"title":"Dorar el pollo","description":"Dora el pollo por todos lados.","minutes":15},{"order":2,"title":"Anadir el ajo","description":"Incorpora el ajo laminado.","minutes":20}]"""
private const val BOCADILLO_INGREDIENTS =
    """[{"name":"Pan","quantity":1,"unit":null,"isPantry":false},{"name":"Tomate","quantity":1,"unit":"ud","isPantry":false,"ingredientId":"tomate"}]"""
private const val BOCADILLO_STEPS =
    """[{"order":1,"title":"Montar el bocadillo","description":"Unta el tomate y aliña.","minutes":5}]"""

private fun arrozDraft(): String = draftJson()

private fun polloDraft(): String = draftJson(
    title = "Pollo al ajillo",
    description = "Pollo dorado con ajo y perejil.",
    categoryId = "poultry",
    mealTypeId = "dinner",
    cuisineId = "spanish",
    totalMinutes = 35,
    prepMinutes = 10,
    cookMinutes = 25,
    difficultyId = "medium",
    servings = 2,
    ingredients = POLLO_INGREDIENTS,
    steps = POLLO_STEPS,
)

private fun bocadilloDraft(): String = draftJson(
    title = "Bocadillo de tomate",
    description = "Un bocadillo sencillo de tomate y aceite.",
    categoryId = "sandwich_wrap",
    mealTypeId = "breakfast",
    cuisineId = "spanish",
    totalMinutes = 5,
    prepMinutes = 5,
    cookMinutes = 0,
    difficultyId = "easy",
    servings = 1,
    ingredients = BOCADILLO_INGREDIENTS,
    steps = BOCADILLO_STEPS,
    declaredAllergens = """["gluten"]""",
)

/**
 * Receta del contrato §4 como **JSON literal**: sin `id` ni `source`, con `matchScore` opcional
 * (campo del contrato que la app ignora). Nunca se construye desde `Recipe`/`RecipeJson.encode`.
 */
private fun draftJson(
    title: String = "Arroz salteado con verduras",
    description: String = "Un arroz rapido con verduras.",
    categoryId: String = "rice",
    mealTypeId: String = "lunch",
    cuisineId: String = "asian",
    totalMinutes: Int = 25,
    prepMinutes: Int = 10,
    cookMinutes: Int = 15,
    difficultyId: String = "easy",
    servings: Int = 2,
    ingredients: String = ARROZ_INGREDIENTS,
    steps: String = ARROZ_STEPS,
    tips: String = "[]",
    nutrition: String = "null",
    declaredAllergens: String = "[]",
    matchScore: Int? = null,
): String {
    val matchScoreField = matchScore?.let { """, "matchScore": $it""" }.orEmpty()
    return """
        {
          "title": "$title",
          "description": "$description",
          "categoryId": "$categoryId",
          "mealTypeId": "$mealTypeId",
          "cuisineId": "$cuisineId",
          "totalMinutes": $totalMinutes,
          "prepMinutes": $prepMinutes,
          "cookMinutes": $cookMinutes,
          "difficultyId": "$difficultyId",
          "servings": $servings,
          "ingredients": $ingredients,
          "steps": $steps,
          "tips": $tips,
          "nutrition": $nutrition,
          "declaredAllergens": $declaredAllergens$matchScoreField
        }
    """.trimIndent()
}
