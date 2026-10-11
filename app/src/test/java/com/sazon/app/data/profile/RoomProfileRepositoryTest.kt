package com.sazon.app.data.profile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.ProfileRepository
import com.sazon.app.domain.profile.UserProfile
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomProfileRepositoryTest {

    private lateinit var database: ProfileDatabase
    private lateinit var repository: ProfileRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ProfileDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomProfileRepository(database.profileDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun guarda_relee_y_compara_un_perfil_completo() = runTest {
        val original = perfilCompleto()

        repository.save(original)
        val recuperado = repository.get()

        assertEquals(original, recuperado)
    }

    @Test
    fun normaliza_el_nombre_al_guardar() = runTest {
        val original = perfilCompleto().copy(displayName = "  Ana  ")

        repository.save(original)
        val recuperado = repository.get()

        assertEquals("Ana", recuperado?.displayName)
        assertEquals(original.copy(displayName = "Ana"), recuperado)
    }

    @Test
    fun rechaza_un_nombre_vacio_o_solo_espacios_sin_escribir() = runTest {
        val invalido = perfilCompleto().copy(displayName = "   ")

        val error = guardandoYCapturandoError(invalido)

        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
        assertNull("No debe quedar ninguna fila escrita", repository.get())
    }

    @Test
    fun rechaza_un_nombre_de_mas_de_40_caracteres_sin_escribir() = runTest {
        val invalido = perfilCompleto().copy(displayName = "a".repeat(41))

        val error = guardandoYCapturandoError(invalido)

        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
        assertNull("No debe quedar ninguna fila escrita", repository.get())
    }

    @Test
    fun acepta_un_nombre_de_exactamente_40_caracteres() = runTest {
        val valido = perfilCompleto().copy(displayName = "a".repeat(40))

        repository.save(valido)

        assertEquals("a".repeat(40), repository.get()?.displayName)
    }

    @Test
    fun un_perfil_ausente_se_devuelve_como_null() = runTest {
        assertNull(repository.get())
    }

    @Test
    fun sobrescribe_la_fila_unica() = runTest {
        val primero = perfilCompleto().copy(displayName = "Ana")
        val segundo = perfilCompleto().copy(displayName = "Luis")

        repository.save(primero)
        repository.save(segundo)

        assertEquals(segundo, repository.get())
        assertEquals("Debe seguir habiendo una sola fila", 1, contarFilas())
    }

    @Test
    fun clear_elimina_el_perfil() = runTest {
        repository.save(perfilCompleto())

        repository.clear()

        assertNull(repository.get())
        assertEquals(0, contarFilas())
    }

    @Test
    fun los_cambios_se_observan_como_flow() = runTest {
        val emisiones = Collections.synchronizedList(mutableListOf<UserProfile?>())
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe().collect { emisiones.add(it) }
        }

        esperarHastaQue { emisiones.isNotEmpty() }
        assertNull("La primera emision debe ser null", emisiones.first())

        val perfil = perfilCompleto()
        repository.save(perfil)

        esperarHastaQue { emisiones.size >= 2 }
        assertEquals(perfil, emisiones[1])
        assertFalse(emisiones.size > 2)

        job.cancel()
    }

    private suspend fun guardandoYCapturandoError(profile: UserProfile): Throwable? =
        try {
            repository.save(profile)
            null
        } catch (error: IllegalArgumentException) {
            error
        }

    private fun contarFilas(): Int =
        database.query("SELECT COUNT(*) FROM user_profile", null).use { cursor ->
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

private fun perfilCompleto() = UserProfile(
    displayName = "Ana",
    avatarColorId = "terracotta",
    createdAt = 1_700_000_000_000L,
    preferences = PreferenceProfile(
        dietPatternId = "omnivore",
        hardRestrictionIds = listOf("gluten", "lactosa"),
        softPreferenceIds = listOf("spicy", "high_protein"),
        cuisineIds = listOf("italian", "japanese", "mexican"),
        preferredIngredientIds = listOf("tomate", "pollo", "arroz"),
        dislikedIngredientIds = listOf("cilantro", "aceituna"),
        difficultyId = "easy",
        timeBucketId = "under_30",
        defaultServings = 2,
        updatedAt = 1_700_000_100_000L,
    ),
)
