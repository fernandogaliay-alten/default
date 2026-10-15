package com.sazon.app.data.cooking

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.ui.cooking.NotificationPermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Adaptador Android de la peticion del permiso (Robolectric, sin emulador;
 * `docs/specs/cooking-timer-alarm-permission-context.md`, escenarios 1-5): `state()` entrega el permiso
 * **inyectado** del sistema y la memoria de sesion; `markAsked` la fija; `request()` **delega** el
 * lanzador una vez con `POST_NOTIFICATIONS` y **nunca lanza**, ni siquiera si el lanzador falla.
 *
 * El dialogo real del sistema (rationale -> dialogo, concesion, denegacion y "no volver a pedir") no es
 * automatizable (AGENTS.md): lo revisa la persona en el emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidNotificationPermissionPromptTest {

    private val application: Application get() = ApplicationProvider.getApplicationContext()

    private val context: Context get() = application

    /** Lanzador falso: cuenta invocaciones y recuerda el ultimo permiso pedido. */
    private class FakeLauncher(private val throwOnLaunch: Boolean = false) {
        val requestedPermissions = mutableListOf<String>()

        fun launch(permission: String) {
            requestedPermissions += permission
            if (throwOnLaunch) error("El lanzador del sistema no esta disponible")
        }
    }

    @Test
    fun sin_permiso_concedido_y_sin_intento_el_estado_lo_refleja_tal_cual() {
        val prompt = AndroidNotificationPermissionPrompt(context, FakeLauncher()::launch)

        val estado = prompt.state()

        assertFalse("El permiso no esta concedido en esta instalacion", estado.granted)
        assertEquals("Todavia no se ha pedido en esta sesion", null, estado.askedAtMillis)
    }

    @Test
    fun el_permiso_concedido_del_sistema_llega_inyectado_en_el_estado() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val prompt = AndroidNotificationPermissionPrompt(context, FakeLauncher()::launch)

        assertTrue("El permiso concedido se lee del sistema", prompt.state().granted)
    }

    @Test
    fun el_estado_se_relee_en_cada_llamada_igual_que_la_politica_espera() {
        val prompt = AndroidNotificationPermissionPrompt(context, FakeLauncher()::launch)
        assertFalse(prompt.state().granted)

        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue("state() es la inyeccion viva del permiso, no una copia al construir", prompt.state().granted)
    }

    @Test
    fun mark_asked_fija_la_memoria_de_sesion_con_el_instante_inyectado() {
        val prompt = AndroidNotificationPermissionPrompt(context, FakeLauncher()::launch)

        prompt.markAsked(1_234L)

        assertEquals(1_234L, prompt.state().askedAtMillis)
        assertEquals(
            "El intento no inventa el permiso",
            false,
            prompt.state().granted,
        )
    }

    @Test
    fun la_memoria_de_sesion_es_por_instancia_y_sobrevive_a_varias_lecturas() {
        val prompt = AndroidNotificationPermissionPrompt(context, FakeLauncher()::launch)
        prompt.markAsked(1L)

        prompt.markAsked(9L)

        assertEquals("El ultimo intento manda", 9L, prompt.state().askedAtMillis)
        assertEquals(
            NotificationPermissionState(granted = false, askedAtMillis = 9L),
            prompt.state(),
        )
    }

    @Test
    fun request_delega_el_lanzador_una_sola_vez_con_post_notifications() {
        val launcher = FakeLauncher()
        val prompt = AndroidNotificationPermissionPrompt(context, launcher::launch)

        prompt.request()

        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), launcher.requestedPermissions)
    }

    @Test
    fun request_no_lanza_aunque_el_lanzador_falle() {
        val launcher = FakeLauncher(throwOnLaunch = true)
        val prompt = AndroidNotificationPermissionPrompt(context, launcher::launch)

        prompt.request()

        assertEquals("El lanzador se invoco igualmente", 1, launcher.requestedPermissions.size)
    }
}
