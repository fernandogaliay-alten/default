package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.taxonomy.Servings
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DishImagePromptTest {

    private val taxonomy = taxonomyReal()

    @Test
    fun escenario_1_el_prompt_es_determinista_con_titulo_cocina_y_estilo() {
        val recipe = receta(title = "Arroz con pollo criollo", categoryId = "rice", cuisineId = "spanish")

        val primera = DishImagePrompt.build(taxonomy, recipe)
        val segunda = DishImagePrompt.build(taxonomy, recipe)
        val conOtraTaxonomia = DishImagePrompt.build(taxonomyReal(), recipe)

        assertEquals("Misma entrada, misma cadena", primera, segunda)
        assertEquals("Dos instancias de Taxonomy parseadas aparte dan lo mismo", primera, conOtraTaxonomia)

        assertTrue("Debe incluir el titulo", primera.contains("Arroz con pollo criollo"))
        assertTrue("Debe incluir la etiqueta real de la cocina", primera.contains("Española"))
        assertTrue("Debe incluir la coletilla de estilo", primera.contains(DishImagePrompt.STYLE_SUFFIX))
        assertEquals(
            "La coletilla de estilo aparece exactamente una vez",
            1,
            primera.split(DishImagePrompt.STYLE_SUFFIX).size - 1,
        )
        assertFalse("No puede haber espacios dobles", primera.contains("  "))
        assertFalse("No puede haber comas duplicadas", primera.contains(",,"))
        assertTrue("El prompt no puede quedar en blanco", primera.isNotBlank())
    }

    @Test
    fun escenario_1b_los_espacios_sobrantes_del_titulo_se_colapsan() {
        val recipe = receta(title = "  Arroz   con  pollo criollo  ")

        val prompt = DishImagePrompt.build(taxonomy, recipe)

        assertTrue(prompt.contains("Arroz con pollo criollo"))
        assertFalse("No puede haber espacios dobles", prompt.contains("  "))
    }

    @Test
    fun escenario_2_sin_titulo_el_plato_se_describe_por_su_categoria() {
        val recipe = receta(title = "", categoryId = "dessert", cuisineId = "spanish")
        val categoria = requireNotNull(taxonomy.recipeCategory("dessert")) { "Falta 'dessert'" }

        val prompt = DishImagePrompt.build(taxonomy, recipe)

        assertEquals("Postres", categoria.label)
        assertTrue("Debe describir el plato con la etiqueta real de la categoria", prompt.contains("Postres"))
        assertFalse("No debe caer al plato por defecto", prompt.contains(DishImagePrompt.DEFAULT_DISH))
        assertTrue("El prompt no puede quedar en blanco", prompt.isNotBlank())
    }

    @Test
    fun escenario_3_una_receta_incompleta_degrada_al_prompt_por_defecto_sin_lanzar() {
        val vacia = receta(id = "", title = "", categoryId = "", cuisineId = "")
        val categoriaDesconocida = receta(id = "", title = "", categoryId = "no-existe", cuisineId = "")

        val porDefecto = DishImagePrompt.build(taxonomy, vacia)
        val conCategoriaDesconocida = DishImagePrompt.build(taxonomy, categoriaDesconocida)
        val conTaxonomiaVacia = DishImagePrompt.build(taxonomyVacia(), vacia)

        listOf(porDefecto, conCategoriaDesconocida, conTaxonomiaVacia).forEach { prompt ->
            assertTrue("El prompt no puede quedar en blanco", prompt.isNotBlank())
            assertTrue("Debe contener el plato por defecto", prompt.contains(DishImagePrompt.DEFAULT_DISH))
            assertTrue("Debe contener la coletilla de estilo", prompt.contains(DishImagePrompt.STYLE_SUFFIX))
        }
        assertEquals("Una categoria desconocida degrada al plato por defecto", porDefecto, conCategoriaDesconocida)
        assertEquals("Una Taxonomia vacia degrada al plato por defecto", porDefecto, conTaxonomiaVacia)
    }

    @Test
    fun escenario_4_una_cocina_desconocida_o_vacia_no_ensucia_el_prompt() {
        val cocinaVacia = DishImagePrompt.build(taxonomy, receta(cuisineId = ""))
        val cocinaDesconocida = DishImagePrompt.build(taxonomy, receta(cuisineId = "no-existe"))
        val sinCocinas = DishImagePrompt.build(taxonomySinCocinas(), receta(cuisineId = "spanish"))

        assertEquals("Una cocina vacia no anade clausula", cocinaVacia, cocinaDesconocida)
        assertEquals("Una cocina desconocida no anade clausula", cocinaVacia, sinCocinas)
        assertFalse("No debe aparecer la etiqueta de la cocina", cocinaVacia.contains("Española"))
        assertFalse(
            "No debe aparecer ningun marcador de cocina desconocida",
            cocinaVacia.lowercase().contains("desconocid"),
        )
        assertTrue("El prompt sigue siendo valido", cocinaVacia.isNotBlank())
        assertTrue("El prompt sigue conteniendo el titulo", cocinaVacia.contains("Arroz con pollo"))
        assertEquals("El prompt sigue siendo determinista", cocinaVacia, DishImagePrompt.build(taxonomy, receta(cuisineId = "")))
    }

    @Test
    fun la_plantilla_fija_el_orden_prefix_plato_cocina_y_estilo() {
        val prompt = DishImagePrompt.build(
            taxonomy,
            receta(title = "Arroz con pollo criollo", cuisineId = "spanish"),
        )

        assertEquals(
            "Fotografía gastronómica de Arroz con pollo criollo, Española, ${DishImagePrompt.STYLE_SUFFIX}",
            prompt,
        )
        assertEquals("Fotografía gastronómica de", DishImagePrompt.PROMPT_PREFIX)
        assertEquals("plato casero", DishImagePrompt.DEFAULT_DISH)
    }

    // --- Escenarios 7-8 de `docs/specs/dish-visual-descriptor.md` (fase 5) ---

    @Test
    fun descriptor_escenario_7_el_image_prompt_sustituye_la_clausula_del_plato() {
        val recipe = receta(title = "Arroz con pollo fácil", cuisineId = "spanish")
        val descriptor = DishVisualDescriptor(
            dishKey = "arroz-con-pollo",
            imagePrompt = "arroz amarillo con pollo desmenuzado, guisantes y zanahoria",
        )

        val prompt = DishImagePrompt.build(taxonomy, recipe, descriptor)

        assertTrue(
            "Debe usar el imagePrompt como descripcion del plato",
            prompt.contains("arroz amarillo con pollo desmenuzado, guisantes y zanahoria"),
        )
        assertFalse("El titulo de la receta ya no describe el plato", prompt.contains("Arroz con pollo fácil"))
        assertTrue("Conserva el prefijo", prompt.startsWith(DishImagePrompt.PROMPT_PREFIX))
        assertTrue("Conserva la clausula de cocina de la receta", prompt.contains("Española"))
        assertTrue("Conserva la coletilla de estilo", prompt.contains(DishImagePrompt.STYLE_SUFFIX))
        assertEquals(
            "La coletilla de estilo aparece exactamente una vez",
            1,
            prompt.split(DishImagePrompt.STYLE_SUFFIX).size - 1,
        )
        assertTrue("El prompt no puede quedar en blanco", prompt.isNotBlank())
    }

    @Test
    fun descriptor_escenario_7b_los_espacios_del_image_prompt_se_colapsan() {
        val recipe = receta(title = "Arroz con pollo")
        val descriptor = DishVisualDescriptor(
            imagePrompt = "  arroz   amarillo  con   pollo,  guisantes ",
        )

        val prompt = DishImagePrompt.build(taxonomy, recipe, descriptor)

        assertTrue(prompt.contains("arroz amarillo con pollo, guisantes"))
        assertFalse("No puede haber espacios dobles", prompt.contains("  "))
    }

    @Test
    fun descriptor_escenario_7c_un_image_prompt_no_canonicalizable_sigue_sirviendo_de_texto() {
        // `imagePrompt` es texto para un modelo de imagen: no se le exige el alfabeto de la clave.
        val prompt = DishImagePrompt.build(
            taxonomy,
            receta(title = "Arroz con pollo"),
            DishVisualDescriptor(imagePrompt = "🍲"),
        )

        assertTrue("El emoji sigue siendo una descripcion valida", prompt.contains("🍲"))
        assertFalse("No cae al titulo si hay imagePrompt", prompt.contains("Arroz con pollo"))
    }

    @Test
    fun descriptor_escenario_8_sin_image_prompt_el_prompt_es_el_de_la_fase_1() {
        val conTitulo = receta(title = "Arroz con pollo criollo", cuisineId = "spanish")
        val sinTituloConCategoria = receta(title = "", categoryId = "dessert", cuisineId = "spanish")
        val sinTituloNiCategoria = receta(title = "", categoryId = "", cuisineId = "")

        val descriptoresInutiles = listOf(
            null,
            DishVisualDescriptor.NONE,
            DishVisualDescriptor(dishKey = "arroz-con-pollo"),
            DishVisualDescriptor(dishKey = "arroz-con-pollo", imagePrompt = ""),
            DishVisualDescriptor(dishKey = "arroz-con-pollo", imagePrompt = "   "),
        )

        descriptoresInutiles.forEach { descriptor ->
            assertEquals(
                "Con descriptor sin imagePrompt ($descriptor) el prompt es identico al de la fase 1",
                DishImagePrompt.build(taxonomy, conTitulo),
                DishImagePrompt.build(taxonomy, conTitulo, descriptor),
            )
            assertEquals(
                DishImagePrompt.build(taxonomy, sinTituloConCategoria),
                DishImagePrompt.build(taxonomy, sinTituloConCategoria, descriptor),
            )
            assertEquals(
                DishImagePrompt.build(taxonomy, sinTituloNiCategoria),
                DishImagePrompt.build(taxonomy, sinTituloNiCategoria, descriptor),
            )
        }

        assertTrue(DishImagePrompt.build(taxonomy, sinTituloConCategoria).contains("Postres"))
        assertTrue(DishImagePrompt.build(taxonomy, sinTituloNiCategoria).contains(DishImagePrompt.DEFAULT_DISH))
    }

    @Test
    fun el_paquete_domain_image_no_importa_android_ni_hace_red() {
        val ficheros = listOf(
            "app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt",
            "app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt",
            "app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt",
        )

        ficheros.forEach { ruta ->
            val fichero = File(raizDelRepositorio(), ruta)
            assertTrue("Falta el fichero fuente $ruta", fichero.isFile)

            val contenido = fichero.readText()
            val importaciones = Regex("^import .*$", RegexOption.MULTILINE).findAll(contenido)
                .map { it.value }

            importaciones.forEach { importacion ->
                assertFalse("$ruta no debe importar android.*: $importacion", importacion.startsWith("import android."))
                assertFalse("$ruta no debe importar androidx.*: $importacion", importacion.startsWith("import androidx."))
                assertFalse("$ruta no debe importar Ktor: $importacion", importacion.startsWith("import io.ktor"))
                assertFalse("$ruta no debe hacer red (java.net): $importacion", importacion.startsWith("import java.net"))
                assertFalse("$ruta no debe importar aleatoriedad: $importacion", importacion.startsWith("import kotlin.random"))
                assertFalse("$ruta no debe importar aleatoriedad: $importacion", importacion.startsWith("import java.util.Random"))
                assertFalse("$ruta no debe importar UUID: $importacion", importacion.startsWith("import java.util.UUID"))
                assertFalse("$ruta no debe lanzar coroutines: $importacion", importacion.startsWith("import kotlinx.coroutines.launch"))
            }
            assertFalse("$ruta no debe usar reloj", contenido.contains("currentTimeMillis"))
            assertFalse("$ruta no debe usar aleatoriedad", contenido.contains("Math.random"))
        }
    }
}

private fun receta(
    id: String = "seed-arroz-pollo",
    source: RecipeSource = RecipeSource.SEED,
    title: String = "Arroz con pollo",
    categoryId: String = "rice",
    cuisineId: String = "spanish",
) = Recipe(
    id = id,
    source = source,
    title = title,
    description = "Receta de prueba",
    categoryId = categoryId,
    mealTypeId = "lunch",
    cuisineId = cuisineId,
    totalMinutes = 30,
    prepMinutes = 10,
    cookMinutes = 20,
    difficultyId = "easy",
    servings = 2,
    ingredients = emptyList(),
    steps = emptyList(),
)

private fun taxonomyReal(): Taxonomy {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return TaxonomyJson.parse(File(path).readText())
}

private fun taxonomyVacia() = Taxonomy(
    dietPatterns = emptyList(),
    hardRestrictions = emptyList(),
    softPreferences = emptyList(),
    mealTypes = emptyList(),
    cuisines = emptyList(),
    timeBuckets = emptyList(),
    difficulties = emptyList(),
    servings = Servings(min = 1, max = 8, default = 2),
    avatarColors = emptyList(),
    ingredients = emptyList(),
    recipeCategories = emptyList(),
    categoryFallbackIcon = emptyMap(),
)

private fun taxonomySinCocinas(): Taxonomy {
    val real = taxonomyReal()
    return Taxonomy(
        dietPatterns = real.dietPatterns,
        hardRestrictions = real.hardRestrictions,
        softPreferences = real.softPreferences,
        mealTypes = real.mealTypes,
        cuisines = emptyList(),
        timeBuckets = real.timeBuckets,
        difficulties = real.difficulties,
        servings = real.servings,
        avatarColors = real.avatarColors,
        ingredients = real.ingredients,
        recipeCategories = real.recipeCategories,
        categoryFallbackIcon = real.categoryFallbackIcon,
    )
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
