package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeSource
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DishImageCacheKeyTest {

    @Test
    fun escenario_5_el_mismo_plato_con_mayusculas_espacios_y_puntuacion_da_la_misma_clave() {
        val nombres = listOf(
            "Arroz con Pollo Criollo",
            "  arroz  con   pollo criollo ",
            "ARROZ CON POLLO CRIOLLO",
            "Arroz con pollo criollo!",
        )

        val claves = nombres.map { DishImageCacheKey.of(it) }

        assertEquals("Las cuatro variantes deben compartir clave", 1, claves.toSet().size)
        assertEquals("dish-image-v1-arroz-con-pollo-criollo", claves.first())
    }

    @Test
    fun escenario_5b_la_clave_es_un_slug_sin_diacriticos_ni_guiones_sobrantes() {
        assertEquals(
            "dish-image-v1-salmon-a-la-plancha",
            DishImageCacheKey.of("Salmón a la plancha"),
        )
        assertEquals("salmon-a-la-plancha", DishImageCacheKey.normalize("Salmón a la plancha"))

        // Alfabeto de fichero: solo [a-z0-9-], sin guiones en los extremos ni repetidos.
        val claves = listOf(
            DishImageCacheKey.of("Arroz con pollo criollo"),
            DishImageCacheKey.of("Salmón a la plancha"),
            DishImageCacheKey.of("¡Croquetas  de  jamón!"),
            DishImageCacheKey.of("Tarta de queso (al horno)"),
        )
        claves.forEach { clave ->
            assertTrue(
                "La clave '$clave' debe ser version + slug [a-z0-9-]",
                clave.matches(Regex("^dish-image-v1-[a-z0-9]+(-[a-z0-9]+)*$")),
            )
        }
    }

    @Test
    fun escenario_6_la_clave_es_estable_y_discriminante() {
        val primera = receta(id = "seed-arroz-pollo", title = "Arroz con pollo")
        val segundaEnOtraLlamada = receta(
            id = "ai-1234",
            source = RecipeSource.AI,
            title = "Arroz con pollo",
            categoryId = "poultry",
            cuisineId = "asian",
        )
        val otra = receta(id = "seed-tortilla-patatas", title = "Tortilla de patatas")

        assertEquals(
            "Dos recetas del mismo plato comparten clave aunque cambien id/source/categoria/cocina",
            DishImageCacheKey.of(primera),
            DishImageCacheKey.of(segundaEnOtraLlamada),
        )
        assertEquals(
            "La clave por receta coincide con la clave por nombre de plato",
            DishImageCacheKey.of("Arroz con pollo"),
            DishImageCacheKey.of(primera),
        )
        assertNotEquals(DishImageCacheKey.of(primera), DishImageCacheKey.of(otra))
        assertEquals(
            "La clave es estable entre llamadas",
            DishImageCacheKey.of(primera),
            DishImageCacheKey.of(primera),
        )
    }

    @Test
    fun escenario_7_sin_nombre_de_plato_la_clave_no_colisiona() {
        val sinTituloConId = receta(id = "seed-tortilla-patatas", title = "")
        val sinTituloYsinId = receta(id = "", title = "")

        val conId = DishImageCacheKey.of(sinTituloConId)
        val porDefecto = DishImageCacheKey.of(sinTituloYsinId)

        assertEquals("dish-image-v1-plato-seed-tortilla-patatas", conId)
        assertEquals("dish-image-v1-plato", porDefecto)
        assertNotEquals(
            "Dos recetas sin nombre no deben compartir imagen",
            conId,
            porDefecto,
        )
        assertFalse("Ninguna clave puede quedar vacia", conId.isBlank())
        assertFalse("Ninguna clave puede quedar vacia", porDefecto.isBlank())
    }

    @Test
    fun escenario_8_la_normalizacion_es_tipografica_no_semantica() {
        assertNotEquals(
            "La normalizacion no fusiona variantes semanticas",
            DishImageCacheKey.of("Arroz con pollo"),
            DishImageCacheKey.of("Arroz con pollo fácil"),
        )
        assertNotEquals(
            "La normalizacion no descarta palabras funcionales",
            DishImageCacheKey.of("Arroz con pollo"),
            DishImageCacheKey.of("Receta de arroz con pollo"),
        )
    }

    @Test
    fun el_normalizador_degrada_a_vacio_y_solo_emite_el_alfabeto_permitido() {
        assertEquals("", DishImageCacheKey.normalize(""))
        assertEquals("", DishImageCacheKey.normalize("   "))
        assertEquals("", DishImageCacheKey.normalize("--"))
        assertEquals("", DishImageCacheKey.normalize("🍲"))
        assertEquals("", DishImageCacheKey.normalize("寿司"))
        assertEquals("arroz-con-pollo", DishImageCacheKey.normalize("¡Arroz, con pollo!"))
        assertEquals("arroz-con-pollo", DishImageCacheKey.normalize("--Arroz -- con -- pollo--"))
        assertEquals("croquetas-de-jamon", DishImageCacheKey.normalize("  Croquetas  de  jamón  "))
        assertEquals("plato", DishImageCacheKey.normalize(DishImageCacheKey.FALLBACK_KEY))

        // Un nombre sin caracteres utiles cae al respaldo por defecto, nunca a cadena vacia.
        assertEquals("dish-image-v1-plato", DishImageCacheKey.of("🍲"))
        assertEquals("dish-image-v1-plato", DishImageCacheKey.of(""))
    }

    @Test
    fun la_clave_no_depende_del_idioma_del_dispositivo() {
        val anterior = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            // Con minusculas dependientes del idioma, "I" seria "ı" y la clave cambiaria de dispositivo
            // a dispositivo; `Locale.ROOT` lo impide.
            assertEquals("i", DishImageCacheKey.normalize("I"))
            assertEquals("istanbul", DishImageCacheKey.normalize("İSTANBUL"))
            assertEquals("dish-image-v1-istanbul", DishImageCacheKey.of("Istanbul"))
        } finally {
            Locale.setDefault(anterior)
        }
    }

    @Test
    fun la_version_es_el_primer_segmento_y_la_clave_por_defecto_la_incluye() {
        assertEquals("dish-image-v1", DishImageCacheKey.VERSION)
        assertEquals("plato", DishImageCacheKey.FALLBACK_KEY)
        assertTrue(DishImageCacheKey.of("Arroz con pollo").startsWith("${DishImageCacheKey.VERSION}-"))
        assertEquals("${DishImageCacheKey.VERSION}-${DishImageCacheKey.FALLBACK_KEY}", DishImageCacheKey.of(""))
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
