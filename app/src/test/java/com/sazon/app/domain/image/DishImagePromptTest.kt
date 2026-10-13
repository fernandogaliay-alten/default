package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
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
    fun escenario_1_el_prompt_v2_es_determinista_con_identidad_ingredientes_composicion_y_negativos() {
        val recipe = receta(
            title = "Arroz con pollo criollo",
            categoryId = "rice",
            cuisineId = "spanish",
            ingredients = listOf(
                ingrediente("Pollo"),
                ingrediente("Arroz"),
                ingrediente("Zanahoria"),
            ),
        )

        val primera = DishImagePrompt.build(taxonomy, recipe)
        val segunda = DishImagePrompt.build(taxonomy, recipe)
        val conOtraTaxonomia = DishImagePrompt.build(taxonomyReal(), recipe)

        assertEquals("Misma entrada, misma cadena", primera, segunda)
        assertEquals("Dos instancias de Taxonomy parseadas aparte dan lo mismo", primera, conOtraTaxonomia)

        assertEquals(
            "La plantilla v2 une identidad + ingredientes + composicion + estilo + negativos",
            "Fotografía gastronómica de Arroz con pollo criollo, Española, Pollo, Arroz, Zanahoria, " +
                "${DishImagePrompt.COMPOSITION}, ${DishImagePrompt.STYLE_SUFFIX}, ${DishImagePrompt.NEGATIVES}",
            primera,
        )
        assertTrue("Empieza por el prefijo", primera.startsWith(DishImagePrompt.PROMPT_PREFIX))
        assertTrue("Debe incluir el titulo como identidad", primera.contains("Arroz con pollo criollo"))
        assertTrue("Debe incluir la etiqueta real de la cocina", primera.contains("Española"))
        assertTrue(
            "Debe incluir los ingredientes clave",
            primera.contains("Pollo, Arroz, Zanahoria"),
        )
        assertTrue("Debe incluir la composicion", primera.contains(DishImagePrompt.COMPOSITION))
        assertTrue("Debe incluir la coletilla de estilo", primera.contains(DishImagePrompt.STYLE_SUFFIX))
        assertTrue("Debe incluir los negativos reforzados", primera.contains(DishImagePrompt.NEGATIVES))
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
    fun escenario_1b_los_espacios_sobrantes_del_titulo_y_de_los_ingredientes_se_colapsan() {
        val recipe = receta(
            title = "  Arroz   con  pollo criollo  ",
            ingredients = listOf(ingrediente("  Pollo   desmenuzado ")),
        )

        val prompt = DishImagePrompt.build(taxonomy, recipe)

        assertTrue(prompt.contains("Arroz con pollo criollo"))
        assertTrue(prompt.contains("Pollo desmenuzado"))
        assertFalse("No puede haber espacios dobles", prompt.contains("  "))
    }

    @Test
    fun escenario_2_sin_titulo_el_plato_se_identifica_por_su_categoria() {
        val recipe = receta(title = "", categoryId = "dessert", cuisineId = "spanish")
        val categoria = requireNotNull(taxonomy.recipeCategory("dessert")) { "Falta 'dessert'" }

        val prompt = DishImagePrompt.build(taxonomy, recipe)

        assertEquals("Postres", categoria.label)
        assertTrue("Debe describir el plato con la etiqueta real de la categoria", prompt.contains("Postres"))
        assertFalse("No debe caer al plato por defecto", prompt.contains(DishImagePrompt.DEFAULT_DISH))
        assertTrue("El prompt no puede quedar en blanco", prompt.isNotBlank())
    }

    @Test
    fun escenario_3_con_descriptor_el_titulo_sigue_siendo_la_identidad_y_el_image_prompt_es_el_detalle() {
        val recipe = receta(
            title = "Arroz con pollo criollo",
            cuisineId = "spanish",
            ingredients = listOf(ingrediente("Pollo"), ingrediente("Arroz")),
        )
        val descriptor = DishVisualDescriptor(
            dishKey = "arroz-con-pollo",
            imagePrompt = "arroz amarillo con pollo desmenuzado, guisantes y zanahoria",
        )

        val prompt = DishImagePrompt.build(taxonomy, recipe, descriptor)

        assertTrue(
            "El titulo sigue siendo la identidad (ya no lo sustituye el imagePrompt)",
            prompt.contains("Arroz con pollo criollo"),
        )
        assertTrue(
            "El imagePrompt entra como detalle visual del plato terminado",
            prompt.contains("arroz amarillo con pollo desmenuzado, guisantes y zanahoria"),
        )
        assertTrue("Conserva el prefijo", prompt.startsWith(DishImagePrompt.PROMPT_PREFIX))
        assertTrue("Conserva la clausula de cocina de la receta", prompt.contains("Española"))
        assertTrue("Conserva los ingredientes clave", prompt.contains("Pollo, Arroz"))
        assertTrue("Conserva la composicion", prompt.contains(DishImagePrompt.COMPOSITION))
        assertTrue("Conserva la coletilla de estilo", prompt.contains(DishImagePrompt.STYLE_SUFFIX))
        assertTrue("Conserva los negativos", prompt.contains(DishImagePrompt.NEGATIVES))
        assertEquals(
            "La coletilla de estilo aparece exactamente una vez",
            1,
            prompt.split(DishImagePrompt.STYLE_SUFFIX).size - 1,
        )
        assertFalse("No puede haber espacios dobles", prompt.contains("  "))
        assertFalse("No puede haber comas duplicadas", prompt.contains(",,"))
        assertTrue("El prompt no puede quedar en blanco", prompt.isNotBlank())
        val identidad = prompt.indexOf("Arroz con pollo criollo")
        val detalle = prompt.indexOf("arroz amarillo con pollo desmenuzado")
        assertTrue("La identidad va antes que el detalle", identidad in 0 until detalle)
    }

    @Test
    fun escenario_3b_los_espacios_del_image_prompt_se_colapsan() {
        val recipe = receta(title = "Arroz con pollo")
        val descriptor = DishVisualDescriptor(
            imagePrompt = "  arroz   amarillo  con   pollo,  guisantes ",
        )

        val prompt = DishImagePrompt.build(taxonomy, recipe, descriptor)

        assertTrue(prompt.contains("arroz amarillo con pollo, guisantes"))
        assertFalse("No puede haber espacios dobles", prompt.contains("  "))
        assertFalse("No puede haber comas duplicadas", prompt.contains(",,"))
    }

    @Test
    fun escenario_3c_un_image_prompt_no_canonicalizable_sigue_sirviendo_de_detalle() {
        // `imagePrompt` es texto para un modelo de imagen: no se le exige el alfabeto de la clave.
        val prompt = DishImagePrompt.build(
            taxonomy,
            receta(title = "Arroz con pollo"),
            DishVisualDescriptor(imagePrompt = "🍲"),
        )

        assertTrue("El emoji sigue siendo una descripcion valida", prompt.contains("🍲"))
        assertTrue("El titulo sigue presente aunque haya imagePrompt", prompt.contains("Arroz con pollo"))
    }

    @Test
    fun escenario_4_los_ingredientes_clave_respetan_tope_orden_distintos_y_despensa() {
        val recipe = receta(
            title = "Paella valenciana",
            ingredients = listOf(
                ingrediente("Arroz"),
                ingrediente("Sal", isPantry = true),
                ingrediente("Azafrán"),
                ingrediente("Arroz"),
                ingrediente("Pollo"),
                ingrediente("Judías"),
                ingrediente("Aceite", isPantry = true),
                ingrediente("Pimiento"),
                ingrediente("Guisantes"),
            ),
        )

        val prompt = DishImagePrompt.build(taxonomy, recipe)

        assertEquals("El tope de ingredientes clave es 5", 5, DishImagePrompt.MAX_KEY_INGREDIENTS)
        assertTrue(
            "Se anaden los 5 primeros distintos no-despensa en el orden de la receta",
            prompt.contains("Arroz, Azafrán, Pollo, Judías, Pimiento"),
        )
        assertFalse("El ingrediente sobrante queda fuera", prompt.contains("Guisantes"))
        assertFalse("Los de despensa quedan fuera", prompt.contains("Sal"))
        assertFalse("Los de despensa quedan fuera", prompt.contains("Aceite"))
    }

    @Test
    fun escenario_4b_sin_ingredientes_utiles_la_clausula_se_omite_sin_dejar_comas() {
        val sinIngredientes = receta(title = "Arroz con pollo", cuisineId = "spanish")
        val soloDespensa = receta(
            title = "Arroz con pollo",
            cuisineId = "spanish",
            ingredients = listOf(ingrediente("Sal", isPantry = true), ingrediente("Aceite", isPantry = true)),
        )
        val nombresVacios = receta(
            title = "Arroz con pollo",
            cuisineId = "spanish",
            ingredients = listOf(ingrediente("   "), ingrediente("")),
        )

        val esperado = "Fotografía gastronómica de Arroz con pollo, Española, " +
            "${DishImagePrompt.COMPOSITION}, ${DishImagePrompt.STYLE_SUFFIX}, ${DishImagePrompt.NEGATIVES}"

        assertEquals("Sin ingredientes la clausula se omite", esperado, DishImagePrompt.build(taxonomy, sinIngredientes))
        assertEquals("Solo despensa no anade clausula", esperado, DishImagePrompt.build(taxonomy, soloDespensa))
        assertEquals("Nombres vacios no anaden clausula", esperado, DishImagePrompt.build(taxonomy, nombresVacios))
        assertFalse("No quedan comas duplicadas", esperado.contains(",,"))
        assertFalse("No quedan espacios dobles", esperado.contains("  "))
    }

    @Test
    fun escenario_5_una_receta_incompleta_degrada_al_prompt_por_defecto_sin_lanzar() {
        val vacia = receta(id = "", title = "", categoryId = "", cuisineId = "")
        val categoriaDesconocida = receta(id = "", title = "", categoryId = "no-existe", cuisineId = "")

        val porDefecto = DishImagePrompt.build(taxonomy, vacia)
        val conCategoriaDesconocida = DishImagePrompt.build(taxonomy, categoriaDesconocida)
        val conTaxonomiaVacia = DishImagePrompt.build(taxonomyVacia(), vacia)

        listOf(porDefecto, conCategoriaDesconocida, conTaxonomiaVacia).forEach { prompt ->
            assertTrue("El prompt no puede quedar en blanco", prompt.isNotBlank())
            assertTrue("Debe contener el plato por defecto", prompt.contains(DishImagePrompt.DEFAULT_DISH))
            assertTrue("Debe contener la composicion", prompt.contains(DishImagePrompt.COMPOSITION))
            assertTrue("Debe contener la coletilla de estilo", prompt.contains(DishImagePrompt.STYLE_SUFFIX))
            assertTrue("Debe contener los negativos", prompt.contains(DishImagePrompt.NEGATIVES))
        }
        assertEquals("Una categoria desconocida degrada al plato por defecto", porDefecto, conCategoriaDesconocida)
        assertEquals("Una Taxonomia vacia degrada al plato por defecto", porDefecto, conTaxonomiaVacia)
    }

    @Test
    fun escenario_5b_una_cocina_desconocida_o_vacia_no_ensucia_el_prompt() {
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
        assertEquals(
            "El prompt sigue siendo determinista",
            cocinaVacia,
            DishImagePrompt.build(taxonomy, receta(cuisineId = "")),
        )
    }

    @Test
    fun la_plantilla_fija_el_orden_prefijo_identidad_composicion_estilo_y_negativos() {
        val prompt = DishImagePrompt.build(
            taxonomy,
            receta(title = "Arroz con pollo criollo", cuisineId = "spanish"),
        )

        assertEquals(
            "Fotografía gastronómica de Arroz con pollo criollo, Española, ${DishImagePrompt.COMPOSITION}, " +
                "${DishImagePrompt.STYLE_SUFFIX}, ${DishImagePrompt.NEGATIVES}",
            prompt,
        )
        assertEquals("Fotografía gastronómica de", DishImagePrompt.PROMPT_PREFIX)
        assertEquals("plato casero", DishImagePrompt.DEFAULT_DISH)
        assertEquals(
            "una ración completa del plato en primer plano, sobre fondo neutro",
            DishImagePrompt.COMPOSITION,
        )
        assertEquals(
            "cerámica artesanal, luz natural suave, estilo editorial gastronómico",
            DishImagePrompt.STYLE_SUFFIX,
        )
        assertEquals(
            "sin texto, sin manos, sin personas, sin cubiertos ni marcas de agua, " +
                "sin collage ni varios platos, sin fondo recargado",
            DishImagePrompt.NEGATIVES,
        )
    }

    // --- Escenarios 2 y 8 de `docs/specs/dish-visual-descriptor.md` (fase 5), reescritos en v2 ---

    @Test
    fun descriptor_sin_image_prompt_el_prompt_presupone_el_resto_de_la_plantilla_v2() {
        val conTitulo = receta(title = "Arroz con pollo criollo", cuisineId = "spanish")
        val sinTituloConCategoria = receta(title = "", categoryId = "dessert", cuisineId = "spanish")
        val sinTituloNiCategoria = receta(title = "", categoryId = "", cuisineId = "")

        val descriptoresSinDetalle = listOf(
            null,
            DishVisualDescriptor.NONE,
            DishVisualDescriptor(dishKey = "arroz-con-pollo"),
            DishVisualDescriptor(dishKey = "arroz-con-pollo", imagePrompt = ""),
            DishVisualDescriptor(dishKey = "arroz-con-pollo", imagePrompt = "   "),
        )

        descriptoresSinDetalle.forEach { descriptor ->
            assertEquals(
                "Con descriptor sin imagePrompt ($descriptor) el prompt no gana segmento de detalle",
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

private fun ingrediente(name: String, isPantry: Boolean = false) = RecipeIngredient(
    name = name,
    quantity = 1.0,
    isPantry = isPantry,
)

private fun receta(
    id: String = "seed-arroz-pollo",
    source: RecipeSource = RecipeSource.SEED,
    title: String = "Arroz con pollo",
    categoryId: String = "rice",
    cuisineId: String = "spanish",
    ingredients: List<RecipeIngredient> = emptyList(),
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
    ingredients = ingredients,
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
