package com.sazon.app.data.favorites

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.favorites.Favorite
import com.sazon.app.domain.favorites.FavoriteRepository
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Escenario 10 de la spec: persistencia Room de Favoritos en JVM (Robolectric, base en memoria):
 * ida y vuelta exacta de una `Recipe` completa con `nutrition`/`ingredientId` opcionales, idempotencia
 * multi-fila, `remove` selectivo, `clear`, `observe` y orden `savedAt` desc.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomFavoriteRepositoryTest {

    private lateinit var database: FavoritesDatabase
    private lateinit var repository: FavoriteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FavoritesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFavoriteRepository(database.favoriteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun una_lista_vacia_se_devuelve_sin_filas() = runTest {
        assertTrue(repository.get().isEmpty())
    }

    @Test
    fun guarda_relee_y_compara_un_favorito_completo_con_campos_opcionales() = runTest {
        val original = Favorite(
            recipe = recetaCompleta("ai-libre", "Bocadillo de tomate", conOpcionales = true),
            savedAt = 1_700_000_000_000L,
        )

        repository.save(original)

        assertEquals(original, repository.get().single())
    }

    @Test
    fun is_favorite_refleja_el_estado() = runTest {
        val recipe = recetaCompleta("ai-x", "Receta X")

        assertFalse(repository.isFavorite(recipe.id))

        repository.save(Favorite(recipe = recipe, savedAt = 10L))

        assertTrue(repository.isFavorite(recipe.id))
        assertFalse(repository.isFavorite("no-existe"))
    }

    @Test
    fun dos_favoritos_coexisten() = runTest {
        repository.save(Favorite(recetaCompleta("ai-1", "Primera"), savedAt = 100L))
        repository.save(Favorite(recetaCompleta("ai-2", "Segunda"), savedAt = 200L))

        assertEquals("Deben coexistir dos filas", 2, repository.get().size)
        assertEquals(2, contarFilas())
    }

    @Test
    fun reescribir_el_mismo_recipe_id_no_duplica() = runTest {
        val recipe = recetaCompleta("ai-1", "Version uno")
        repository.save(Favorite(recipe = recipe, savedAt = 100L))
        repository.save(Favorite(recipe = recipe.copy(title = "Version dos"), savedAt = 300L))

        assertEquals("Sigue habiendo una sola fila", 1, contarFilas())
        assertEquals("Version dos", repository.get().single().recipe.title)
        assertEquals(300L, repository.get().single().savedAt)
    }

    @Test
    fun remove_borra_solo_esa_receta() = runTest {
        repository.save(Favorite(recetaCompleta("ai-1", "Primera"), savedAt = 100L))
        repository.save(Favorite(recetaCompleta("ai-2", "Segunda"), savedAt = 200L))

        repository.remove("ai-1")

        assertEquals(1, repository.get().size)
        assertNull(repository.get().singleOrNull { it.recipeId == "ai-1" })
        assertEquals("ai-2", repository.get().single().recipeId)
    }

    @Test
    fun clear_deja_la_lectura_vacia() = runTest {
        repository.save(Favorite(recetaCompleta("ai-1", "Primera"), savedAt = 100L))

        repository.clear()

        assertTrue(repository.get().isEmpty())
        assertEquals(0, contarFilas())
    }

    @Test
    fun el_orden_devuelto_es_saved_at_desc() = runTest {
        repository.save(Favorite(recetaCompleta("ai-viejo", "Viejo"), savedAt = 100L))
        repository.save(Favorite(recetaCompleta("ai-nuevo", "Nuevo"), savedAt = 300L))
        repository.save(Favorite(recetaCompleta("ai-medio", "Medio"), savedAt = 200L))

        assertEquals(
            listOf("ai-nuevo", "ai-medio", "ai-viejo"),
            repository.get().map { it.recipeId },
        )
    }

    @Test
    fun los_cambios_se_observan_como_flow() = runTest {
        val emisiones = Collections.synchronizedList(mutableListOf<List<Favorite>>())
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe().collect { emisiones.add(it) }
        }

        esperarHastaQue { emisiones.isNotEmpty() }
        assertTrue("La primera emision debe ser la lista vacia", emisiones.first().isEmpty())

        val favorito = Favorite(recetaCompleta("ai-1", "Observada"), savedAt = 100L)
        repository.save(favorito)

        esperarHastaQue { emisiones.size >= 2 }
        assertEquals(listOf(favorito), emisiones[1])
        assertFalse(emisiones.size > 2)

        job.cancel()
    }

    private fun contarFilas(): Int =
        database.query("SELECT COUNT(*) FROM favorite_recipe", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private suspend fun esperarHastaQue(condicion: () -> Boolean) {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                while (!condicion()) {
                    delay(20)
                }
            }
        }
    }
}

private fun recetaCompleta(
    id: String,
    title: String,
    conOpcionales: Boolean = false,
): Recipe = Recipe(
    id = id,
    source = RecipeSource.AI,
    title = title,
    description = "Receta de prueba.",
    categoryId = "sandwich_wrap",
    mealTypeId = "breakfast",
    cuisineId = "spanish",
    totalMinutes = 5,
    prepMinutes = 5,
    cookMinutes = 0,
    difficultyId = "easy",
    servings = 1,
    ingredients = if (conOpcionales) {
        listOf(
            RecipeIngredient("Pan", 1.0, unit = null, ingredientId = null),
            RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"),
        )
    } else {
        listOf(RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz"))
    },
    steps = listOf(RecipeStep(1, "Montar", "Monta la receta.", 5)),
    tips = emptyList(),
    nutrition = if (conOpcionales) Nutrition(kcal = 250, protein = 8, carbs = 30, fat = 9) else null,
    declaredAllergens = if (conOpcionales) listOf("gluten") else emptyList(),
    matchScore = null,
)
