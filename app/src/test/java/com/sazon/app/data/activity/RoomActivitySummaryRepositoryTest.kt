package com.sazon.app.data.activity

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.activity.ActivitySummaryRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Persistencia Room del contador del Resumen en JVM (Robolectric, base en memoria): fila ausente -> 0,
 * `increment` crea la fila y acumula, `observe` emite el valor nuevo y un delta no positivo no escribe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomActivitySummaryRepositoryTest {

    private lateinit var database: ActivitySummaryDatabase
    private lateinit var repository: ActivitySummaryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ActivitySummaryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomActivitySummaryRepository(database.activitySummaryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sin_fila_el_contador_es_cero() = runTest {
        assertEquals(0, repository.recipesGeneratedCount())
    }

    @Test
    fun increment_crea_la_fila_y_acumula() = runTest {
        repository.incrementRecipesGenerated(3)

        assertEquals("El primer incremento no debe perderse", 3, repository.recipesGeneratedCount())

        repository.incrementRecipesGenerated(4)

        assertEquals(7, repository.recipesGeneratedCount())
        assertEquals("Sigue habiendo una sola fila", 1, contarFilas())
    }

    @Test
    fun observe_emite_el_valor_nuevo() = runTest {
        val emisiones = Collections.synchronizedList(mutableListOf<Int>())
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeRecipesGeneratedCount().collect { emisiones.add(it) }
        }

        esperarHastaQue { emisiones.isNotEmpty() }
        assertEquals("Sin fila, observe emite 0", 0, emisiones.first())

        repository.incrementRecipesGenerated(2)

        esperarHastaQue { emisiones.contains(2) }
        assertEquals(2, emisiones.last())

        job.cancel()
    }

    @Test
    fun un_delta_no_positivo_no_escribe() = runTest {
        repository.incrementRecipesGenerated(5)
        repository.incrementRecipesGenerated(0)
        repository.incrementRecipesGenerated(-3)

        assertEquals("No debe bajar ni cambiar", 5, repository.recipesGeneratedCount())
    }

    @Test
    fun un_delta_no_positivo_sobre_base_vacia_no_crea_fila() = runTest {
        repository.incrementRecipesGenerated(0)
        repository.incrementRecipesGenerated(-1)

        assertEquals(0, repository.recipesGeneratedCount())
        assertEquals("No debe crear fila", 0, contarFilas())
    }

    private fun contarFilas(): Int =
        database.query("SELECT COUNT(*) FROM activity_summary", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private suspend fun esperarHastaQue(condicion: () -> Boolean) {
        withContext(Dispatchers.Default) {
            // Margen amplio a proposito: las emisiones del Flow de Room corren en su propio executor y
            // pueden retrasarse cuando hay builds de Gradle en paralelo; el flake historico fue un timeout de 5 s.
            withTimeout(30_000) {
                while (!condicion()) {
                    delay(10)
                }
            }
        }
    }
}
