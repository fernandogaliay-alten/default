package com.sazon.app.data.cooking

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep
import com.sazon.app.ui.cooking.CookingSession
import com.sazon.app.ui.cooking.CookingSessionStore
import com.sazon.app.ui.cooking.CookingTimerState
import com.sazon.app.ui.cooking.CookingTimerStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Persistencia de la sesion del Modo cocina (`docs/specs/cooking-timer-foreground-service.md`,
 * escenarios 6 y 8): ida y vuelta exacta (con los campos opcionales de la receta), **una sola fila** al
 * sobrescribir, `clear()` -> `null`, degradacion de una fila ilegible a "sin sesion" y contrato
 * **"no lanza"** de la costura.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomCookingSessionStoreTest {

    private lateinit var database: CookingSessionDatabase
    private lateinit var store: CookingSessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CookingSessionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomCookingSessionStore(database.cookingSessionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sin_fila_la_lectura_es_null() = runTest {
        assertNull(store.load())
    }

    @Test
    fun guarda_y_relee_la_sesion_completa_con_los_campos_opcionales() = runTest {
        val original = sesionEnMarcha()

        store.save(original)

        assertEquals(original, store.load())
    }

    @Test
    fun sobrescribe_la_fila_unica() = runTest {
        val primero = sesionEnMarcha()
        val segundo = CookingSession(
            recipe = receta(titulo = "Arroz con verduras"),
            stepIndex = 1,
            completedOrders = setOf(1),
            timer = CookingTimerState.of(0).copy(status = CookingTimerStatus.PAUSED, remainingMillis = 300_000L),
            endAtElapsedMillis = null,
        )

        store.save(primero)
        store.save(segundo)

        assertEquals(segundo, store.load())
        assertEquals("Debe seguir habiendo una sola fila", 1, contarFilas())
    }

    @Test
    fun clear_deja_la_lectura_en_null() = runTest {
        store.save(sesionEnMarcha())

        store.clear()

        assertNull(store.load())
        assertEquals(0, contarFilas())
    }

    @Test
    fun una_fila_ilegible_se_comporta_como_sin_sesion_y_no_lanza() = runTest {
        database.cookingSessionDao().upsert(
            CookingSessionEntity(
                recipeJson = "{ esto no es una receta }",
                stepIndex = 0,
                completedOrders = "",
                timerStatus = "NO_EXISTE",
                totalMillis = 1L,
                remainingMillis = 1L,
                endAtElapsedMillis = null,
            ),
        )

        assertNull("Una fila corrupta no puede romper la app", store.load())
    }

    @Test
    fun la_costura_no_lanza_aunque_el_almacen_falle() = runTest {
        val storeQueFalla = RoomCookingSessionStore(daoQueLanza())

        assertNull(storeQueFalla.load())
        storeQueFalla.save(sesionEnMarcha())
        storeQueFalla.clear()
    }

    @Test
    fun el_temporizador_conserva_su_estado_y_su_instante_de_fin() = runTest {
        val pausado = CookingSession(
            recipe = receta(),
            stepIndex = 1,
            completedOrders = setOf(1, 2),
            timer = CookingTimerState.of(10).start().tick(120_000L).pause(),
            endAtElapsedMillis = null,
        )
        val terminado = pausado.copy(
            timer = pausado.timer.copy(status = CookingTimerStatus.FINISHED, remainingMillis = 0L),
        )

        store.save(pausado)
        assertEquals(pausado, store.load())

        store.save(terminado)
        assertEquals(terminado, store.load())

        val enMarcha = sesionEnMarcha()
        store.save(enMarcha)
        assertEquals(
            "El instante de fin persistido sobrevive a la ida y vuelta",
            enMarcha.endAtElapsedMillis,
            store.load()?.endAtElapsedMillis,
        )
    }

    private fun daoQueLanza(): CookingSessionDao = object : CookingSessionDao {
        override suspend fun get(): CookingSessionEntity? = error("disco lleno")

        override suspend fun upsert(entity: CookingSessionEntity) = error("disco lleno")

        override suspend fun deleteAll() = error("disco lleno")
    }

    private fun contarFilas(): Int =
        database.query("SELECT COUNT(*) FROM cooking_session", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}

/** Sesion En marcha con instante de fin proyectado (el caso que hay que poder reanudar). */
private fun sesionEnMarcha() = CookingSession(
    recipe = receta(),
    stepIndex = 1,
    completedOrders = setOf(1),
    timer = CookingTimerState.of(10).start().tick(300_000L),
    endAtElapsedMillis = 1_500_000L,
)

private fun receta(titulo: String = "Pasta con tomate") = Recipe(
    id = "seed-pasta",
    source = RecipeSource.SEED,
    title = titulo,
    description = "Receta semilla de prueba.",
    categoryId = "pasta",
    mealTypeId = "lunch",
    cuisineId = "italian",
    totalMinutes = 25,
    prepMinutes = 5,
    cookMinutes = 20,
    difficultyId = "easy",
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Pasta", 200.0, MeasureUnit.GRAM, ingredientId = "pasta"),
        RecipeIngredient("Albahaca", 1.0, unit = null, ingredientId = null),
    ),
    steps = listOf(
        RecipeStep(1, "Cocer", "Cuece la pasta.", 10),
        RecipeStep(2, "Servir", "Sirve con la salsa.", 0),
    ),
    nutrition = Nutrition(kcal = 420, protein = 14, carbs = 70, fat = 9),
    declaredAllergens = listOf("gluten"),
)
