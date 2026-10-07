package com.sazon.app.domain.prompt

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val taxonomy = taxonomyReal()

    @Test
    fun escenario_1_dieta_y_restricciones_duras_aparecen_siempre() {
        val perfil = perfilDeReferencia(
            dietPatternId = "vegetarian",
            hardRestrictionIds = listOf("gluten", "lactose"),
        )

        val prompt = PromptBuilder.build(taxonomy, PromptRequest(perfil))
        // Se comprueba sobre `user`: el bloque `system` enumera los ids de la Taxonomia y enmascararia
        // una omision real en el perfil.
        val user = prompt.user

        val dieta = requireNotNull(taxonomy.dietPattern("vegetarian")) { "Falta 'vegetarian'" }
        assertTrue("Debe aparecer el id del patrón de dieta", user.contains(dieta.id))
        assertTrue("Debe aparecer la label del patrón de dieta", user.contains(dieta.label))

        listOf("gluten", "lactose").forEach { id ->
            val restriccion = requireNotNull(taxonomy.restriction(id)) { "Falta '$id'" }
            assertTrue("Debe aparecer el id de la restricción '$id'", user.contains(restriccion.id))
            assertTrue("Debe aparecer la label de la restricción '$id'", user.contains(restriccion.label))
        }
    }

    @Test
    fun escenario_2_sin_restricciones_duras_la_seccion_sigue_presente() {
        val perfil = perfilDeReferencia(hardRestrictionIds = emptyList())

        val prompt = PromptBuilder.build(taxonomy, PromptRequest(perfil))

        assertTrue(
            "La sección de seguridad debe seguir presente",
            prompt.user.contains("Restricciones duras:"),
        )
        assertTrue(
            "Sin restricciones debe indicarse explícitamente que no hay ninguna",
            prompt.user.contains("Ninguna"),
        )
    }

    @Test
    fun escenario_3_pide_seis_recetas_y_el_contrato_de_salida() {
        val prompt = PromptBuilder.build(taxonomy, PromptRequest(perfilDeReferencia()))
        val texto = prompt.system + "\n" + prompt.user

        assertEquals(6, PromptBuilder.RECIPES_PER_GENERATION)
        assertTrue(
            "Debe pedir 6 recetas",
            texto.contains("${PromptBuilder.RECIPES_PER_GENERATION} recetas"),
        )

        CLAVES_DEL_CONTRATO.forEach { clave ->
            assertTrue("Falta la clave '$clave' del contrato de salida", texto.contains(clave))
        }
    }

    @Test
    fun escenario_4_enumera_los_id_validos_y_prohibe_texto_libre() {
        val prompt = PromptBuilder.build(taxonomy, PromptRequest(perfilDeReferencia()))
        val system = prompt.system

        val idsClasificables = taxonomy.recipeCategories.map { it.id } +
            taxonomy.mealTypes.map { it.id } +
            taxonomy.cuisines.map { it.id } +
            taxonomy.difficulties.map { it.id } +
            taxonomy.hardRestrictions.map { it.id }

        idsClasificables.forEach { id ->
            assertTrue("El id '$id' debe enumerarse en el prompt", system.contains(id))
        }

        assertTrue(
            "Debe instruir a usar solo esos id en los campos clasificables",
            system.contains("usa exclusivamente"),
        )
        assertTrue(
            "Debe prohibir el texto libre en los campos clasificables",
            system.contains("nunca escribas texto libre"),
        )
    }

    @Test
    fun escenario_5_los_prompt_hint_de_dieta_y_preferencias_blandas_se_inyectan() {
        val perfil = perfilDeReferencia(
            dietPatternId = "flexitarian",
            softPreferenceIds = listOf("real_fooding", "economical"),
        )

        val prompt = PromptBuilder.build(taxonomy, PromptRequest(perfil))

        val hintDieta = requireNotNull(taxonomy.dietPattern("flexitarian")?.promptHint)
        assertTrue("Debe incluirse el promptHint del patrón de dieta", prompt.user.contains(hintDieta))

        listOf("real_fooding", "economical").forEach { id ->
            val hint = requireNotNull(taxonomy.softPreference(id)?.promptHint) { "Falta '$id'" }
            assertTrue("Debe incluirse el promptHint de '$id'", prompt.user.contains(hint))
        }
    }

    @Test
    fun escenario_6_el_contexto_opcional_se_refleja_en_el_prompt() {
        val request = PromptRequest(
            preferences = perfilDeReferencia(),
            mealTypeId = "dinner",
            timeBucketId = "lte_30",
            difficultyId = "easy",
            notes = "sin picante",
            ingredients = listOf(
                PromptIngredient(name = "Tomate", ingredientId = "tomate"),
                PromptIngredient(name = "Pan", ingredientId = null),
            ),
        )

        val prompt = PromptBuilder.build(taxonomy, request)
        val user = prompt.user

        val comida = requireNotNull(taxonomy.mealType("dinner"))
        val dificultad = requireNotNull(taxonomy.difficulty("easy"))
        val franja = requireNotNull(taxonomy.timeBucket("lte_30"))
        val tomate = requireNotNull(taxonomy.ingredient("tomate"))

        assertTrue("Debe aparecer la label de la comida", user.contains(comida.label))
        assertTrue(
            "Debe aparecer el límite de minutos de la franja",
            user.contains("${franja.maxMinutes} minutos"),
        )
        assertTrue("Debe aparecer la label de la dificultad", user.contains(dificultad.label))
        assertTrue("Debe aparecer el texto de notes", user.contains("sin picante"))
        assertTrue("Debe aparecer la label del ingrediente resuelto", user.contains(tomate.label))
        assertTrue("Debe aparecer el nombre del ingrediente libre", user.contains("Pan"))
    }

    @Test
    fun escenario_6b_una_franja_sin_limite_lo_dice_explicitamente() {
        val request = PromptRequest(
            preferences = perfilDeReferencia(),
            timeBucketId = "gt_60",
        )

        val prompt = PromptBuilder.build(taxonomy, request)

        assertTrue(
            "Una franja con maxMinutes = null debe indicar que no hay límite",
            prompt.user.contains("sin límite máximo"),
        )
    }

    @Test
    fun escenario_7_los_id_desconocidos_no_rompen_ni_se_inventan() {
        val perfil = perfilDeReferencia(
            dietPatternId = "no-existe",
            hardRestrictionIds = listOf("no-existe"),
        )
        val request = PromptRequest(preferences = perfil, mealTypeId = "no-existe")

        val prompt = PromptBuilder.build(taxonomy, request)
        val texto = prompt.system + "\n" + prompt.user

        assertFalse("Los ids desconocidos no deben aparecer en el prompt", texto.contains("no-existe"))
        assertTrue("El patrón de dieta sigue presente", prompt.user.contains("Patrón de dieta:"))
        assertTrue("Las restricciones duras siguen presentes", prompt.user.contains("Restricciones duras:"))
        assertTrue(
            "Sin restricciones resueltas debe indicarse que no hay ninguna",
            prompt.user.contains("Ninguna"),
        )
    }

    @Test
    fun escenario_8_determinismo() {
        val request = PromptRequest(
            preferences = perfilDeReferencia(),
            mealTypeId = "lunch",
            ingredients = listOf(PromptIngredient(name = "Pan")),
            query = "algo rápido",
        )

        val primera = PromptBuilder.build(taxonomy, request)
        val segunda = PromptBuilder.build(taxonomy, request)

        assertEquals("El bloque system debe ser idéntico", primera.system, segunda.system)
        assertEquals("El bloque user debe ser idéntico", primera.user, segunda.user)
    }

    @Test
    fun el_paquete_domain_prompt_no_importa_android_ni_hace_red() {
        val fichero = File(
            raizDelRepositorio(),
            "app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt",
        )
        assertTrue("Falta el fichero fuente de PromptBuilder", fichero.isFile)

        val contenido = fichero.readText()
        assertFalse("No debe importar android.*", contenido.contains("import android."))
        assertFalse("No debe importar androidx.*", contenido.contains("import androidx."))
        assertFalse("No debe usar red (java.net)", contenido.contains("java.net"))
    }
}

private val CLAVES_DEL_CONTRATO = listOf(
    "recipes",
    "title",
    "description",
    "categoryId",
    "mealTypeId",
    "cuisineId",
    "totalMinutes",
    "prepMinutes",
    "cookMinutes",
    "difficultyId",
    "servings",
    "ingredients",
    "steps",
    "tips",
    "nutrition",
    "declaredAllergens",
)

private fun taxonomyReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}

private fun raizDelRepositorio(): File {
    val inicio = System.getProperty("user.dir") ?: error("No hay user.dir en el test")
    var dir: File? = File(inicio).absoluteFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").isFile) return dir
        dir = dir.parentFile
    }
    error("No se encontro settings.gradle.kts desde $inicio")
}

private fun perfilDeReferencia(
    dietPatternId: String = "omnivore",
    hardRestrictionIds: List<String> = listOf("gluten"),
    softPreferenceIds: List<String> = listOf("economical"),
    cuisineIds: List<String> = listOf("spanish"),
    preferredIngredientIds: List<String> = listOf("tomate"),
    dislikedIngredientIds: List<String> = emptyList(),
    difficultyId: String = "easy",
    timeBucketId: String = "lte_30",
    defaultServings: Int = 2,
) = PreferenceProfile(
    dietPatternId = dietPatternId,
    hardRestrictionIds = hardRestrictionIds,
    softPreferenceIds = softPreferenceIds,
    cuisineIds = cuisineIds,
    preferredIngredientIds = preferredIngredientIds,
    dislikedIngredientIds = dislikedIngredientIds,
    difficultyId = difficultyId,
    timeBucketId = timeBucketId,
    defaultServings = defaultServings,
    updatedAt = 1_700_000_000_000L,
)
