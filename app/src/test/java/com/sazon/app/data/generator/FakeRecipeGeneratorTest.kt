package com.sazon.app.data.generator

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeJson
import com.sazon.app.domain.recipe.RecipeSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FakeRecipeGeneratorTest {

    private val generador = FakeRecipeGenerator()

    @Test
    fun sugerir_devuelve_datos_fijos_y_deterministas() = runTest {
        val primera = generador.suggestByPreferences(perfilDeReferencia())
        val segunda = generador.suggestByPreferences(perfilDeReferencia())

        assertTrue("La lista de sugerencias no debe estar vacia", primera.isNotEmpty())
        assertEquals("Dos llamadas deben devolver la misma lista", primera, segunda)
        assertTrue(
            "Todas las recetas deben ser de origen AI",
            primera.all { it.source == RecipeSource.AI },
        )
    }

    @Test
    fun buscar_devuelve_datos_fijos_y_deterministas_e_ignora_el_texto() = runTest {
        val primera = generador.searchByText("pasta carbonara", perfilDeReferencia())
        val repetida = generador.searchByText("pasta carbonara", perfilDeReferencia())
        val otraConsulta = generador.searchByText("sushi vegano", perfilDeReferencia())

        assertTrue("La lista de busqueda no debe estar vacia", primera.isNotEmpty())
        assertEquals("La misma consulta debe dar la misma lista", primera, repetida)
        assertEquals(
            "El falso debe ignorar el texto y devolver su conjunto fijo",
            primera,
            otraConsulta,
        )
    }

    @Test
    fun generar_devuelve_datos_fijos_y_deterministas_e_ignora_la_entrada() = runTest {
        val primera = generador.generateFromIngredients(
            ingredients = listOf(RecipeIngredient("Tomate", 1.0, ingredientId = "tomate")),
            constraints = constraintsDeReferencia(),
        )
        val segunda = generador.generateFromIngredients(
            ingredients = listOf(RecipeIngredient("Arroz", 2.0, ingredientId = "arroz")),
            constraints = constraintsDeReferencia(servings = 6, notes = "sin picante"),
        )

        assertTrue("La lista generada no debe estar vacia", primera.isNotEmpty())
        assertEquals("El falso debe ignorar ingredientes y ajustes", primera, segunda)
    }

    @Test
    fun las_recetas_fijas_pasan_la_ida_y_vuelta_de_recipe_json() = runTest {
        val recetas = recetasFijasDeLasTresOperaciones()

        for (receta in recetas) {
            val recuperada = RecipeJson.parse(RecipeJson.encode(receta))

            assertEquals("La receta debe sobrevivir a encode/parse", receta, recuperada)
            assertEquals(RecipeSource.AI, recuperada.source)
            assertNull(recuperada.matchScore)
        }
    }

    @Test
    fun el_conjunto_fijo_incluye_disparadores_de_descarte() = runTest {
        val ingredientes = recetasFijasDeLasTresOperaciones().flatMap { it.ingredients }

        val ingredienteLibrePan = ingredientes.any { it.ingredientId == null && it.name == "Pan" }
        val ingredienteDeCarneOAve = ingredientes.any { it.ingredientId in IDS_DE_CARNE_O_AVE }

        assertTrue(
            "Debe haber un ingrediente libre 'Pan' (ingredientId = null) para el caso de gluten",
            ingredienteLibrePan,
        )
        assertTrue(
            "Debe haber un ingrediente de carne o ave con id real de la Taxonomia",
            ingredienteDeCarneOAve,
        )
    }

    @Test
    fun el_puerto_y_el_falso_no_importan_android_ni_hacen_red() {
        val raiz = raizDelRepositorio()
        val fuentes = listOf(
            "app/src/main/java/com/sazon/app/domain/generator/RecipeGenerator.kt",
            "app/src/main/java/com/sazon/app/domain/generator/GenerationConstraints.kt",
            "app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt",
        )

        for (ruta in fuentes) {
            val fichero = File(raiz, ruta)
            assertTrue("Falta el fichero fuente $ruta", fichero.isFile)
            val contenido = fichero.readText()

            assertFalse("$ruta no debe importar android.*", contenido.contains("import android."))
            assertFalse("$ruta no debe importar androidx.*", contenido.contains("import androidx."))
            assertFalse("$ruta no debe usar red (java.net)", contenido.contains("java.net"))
            assertFalse("$ruta no debe usar HTTP", contenido.lowercase().contains("http"))
        }
    }

    private suspend fun recetasFijasDeLasTresOperaciones(): List<Recipe> {
        val sugeridas = generador.suggestByPreferences(perfilDeReferencia())
        val buscadas = generador.searchByText("consulta", perfilDeReferencia())
        val generadas = generador.generateFromIngredients(
            ingredients = listOf(RecipeIngredient("Tomate", 1.0, ingredientId = "tomate")),
            constraints = constraintsDeReferencia(),
        )
        return (sugeridas + buscadas + generadas).distinct()
    }
}

private val IDS_DE_CARNE_O_AVE = setOf(
    "pollo",
    "pavo",
    "ternera",
    "cerdo",
    "cordero",
    "jamon",
    "chorizo",
    "bacon",
)

private fun raizDelRepositorio(): File {
    val inicio = System.getProperty("user.dir") ?: error("No hay user.dir en el test")
    var dir: File? = File(inicio).absoluteFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").isFile) return dir
        dir = dir.parentFile
    }
    error("No se encontro settings.gradle.kts desde $inicio")
}

private fun perfilDeReferencia() = PreferenceProfile(
    dietPatternId = "omnivore",
    hardRestrictionIds = listOf("gluten"),
    softPreferenceIds = listOf("economical"),
    cuisineIds = listOf("spanish"),
    preferredIngredientIds = listOf("tomate"),
    dislikedIngredientIds = emptyList(),
    difficultyId = "easy",
    timeBucketId = "lte_30",
    defaultServings = 2,
    updatedAt = 1_700_000_000_000L,
)

private fun constraintsDeReferencia(
    servings: Int = 2,
    notes: String? = null,
) = GenerationConstraints(
    profile = perfilDeReferencia(),
    servings = servings,
    mealTypeId = "dinner",
    timeBucketId = "lte_30",
    difficultyId = "easy",
    notes = notes,
)
