package com.sazon.app.data.image

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Round-trip real del adaptador [AndroidDishImageSettingsStore] sobre `SharedPreferences` en JVM
 * (Robolectric, sin emulador): guardar y releer con una **segunda** instancia del store sobre el
 * mismo `Context`, store virgen -> `null` y valor guardado de tipo distinto -> `null` (degradacion
 * silenciosa, nunca excepcion). Scenario 7 y verificacion exigida 1 de
 * `docs/specs/dish-image-settings-persistence.md`.
 *
 * La relectura usa el **mismo** `Context` a proposito: `SharedPreferences` esta cacheado por nombre,
 * de modo que `apply()` (asincrono en disco, nunca `commit()`) ya es visible. Si el harness mostrara
 * asincronia, la salida correcta **no** es `commit()` (lint `ApplySharedPref` bajo `app/src`), sino
 * ajustar el test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidDishImageSettingsStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun guarda_y_relee_el_estado_con_una_segunda_instancia() {
        AndroidDishImageSettingsStore(context).saveEnabled(true)

        val reopened = AndroidDishImageSettingsStore(context)
        assertEquals(true, reopened.loadEnabled())

        reopened.saveEnabled(false)

        val afterOff = AndroidDishImageSettingsStore(context)
        assertEquals("El valor releido es el ultimo guardado", false, afterOff.loadEnabled())
    }

    @Test
    fun un_store_virgen_devuelve_null() {
        val store = AndroidDishImageSettingsStore(context)

        assertNull("Sin estado guardado no hay valor que restaurar", store.loadEnabled())
    }

    @Test
    fun un_valor_de_tipo_distinto_se_degrada_a_null() {
        // El framework lanzaria ClassCastException al leer este valor como Boolean; el adaptador lo
        // trata como "sin estado guardado" (default de la politica).
        context.getSharedPreferences("dish_image_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("enabled", "si")
            .apply()

        val store = AndroidDishImageSettingsStore(context)

        assertNull(store.loadEnabled())
    }

    @Test
    fun el_ajuste_restaurado_sobre_el_adaptador_recupera_la_eleccion() {
        AndroidDishImageSettingsStore(context).saveEnabled(true)

        val settings = DishImageSettings.restored(AndroidDishImageSettingsStore(context))

        assertTrue(settings.state.value.enabled)
        assertEquals(DishImageSettingsState(enabled = true, used = 0, max = 12), settings.state.value)

        settings.setEnabled(false)

        val next = DishImageSettings.restored(AndroidDishImageSettingsStore(context))
        assertFalse(next.state.value.enabled)
    }
}
