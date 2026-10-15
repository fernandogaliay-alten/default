package com.sazon.app.data.cooking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sazon.app.ui.cooking.NotificationPermissionPrompt
import com.sazon.app.ui.cooking.NotificationPermissionState

/**
 * Adaptador Android de la costura [NotificationPermissionPrompt] (feature
 * `cooking-timer-alarm-permission-context`, escenarios 1-8 de su spec): lee el permiso
 * `POST_NOTIFICATIONS` del sistema y lanza el dialogo con el `ActivityResultLauncher` de la
 * `MainActivity`; guarda en **memoria de sesion** (una instancia por proceso, creada en `onCreate`) el
 * instante del ultimo intento.
 *
 * - [state] es la **inyeccion**: entrega el permiso del sistema tal cual y el instante del ultimo
 *   intento para que la politica pura [com.sazon.app.ui.cooking.NotificationPermissionPolicy] decida.
 * - [request] **delega** el lanzador que le inyectan (el de `MainActivity`, registrado antes de
 *   `onCreate`); el resultado del dialogo sigue siendo **no-op**: el estado se **relee** en el
 *   siguiente [state]. Nunca lanza.
 * - La memoria de sesion ya **no** vive en un flag estatico de `MainActivity` (sin estado estatico):
 *   una instancia por proceso equivale a "una vez por sesion" y, al reentrar en el Modo cocina dentro
 *   del mismo proceso, no se vuelve a pedir.
 * - **Degradacion silenciosa** (ADR 0005): no lanzar y, si el usuario deniega o descarta el dialogo, el
 *   Modo cocina, la cuenta atras y el "¡Listo!" siguen intactos.
 */
class AndroidNotificationPermissionPrompt(
    context: Context,
    private val launch: (String) -> Unit,
) : NotificationPermissionPrompt {

    private val appContext: Context = context.applicationContext

    /** Memoria de sesion (una instancia por proceso): `null` = esta sesion no ha pedido aun. */
    private var askedAtMillis: Long? = null

    override fun state(): NotificationPermissionState {
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return NotificationPermissionState(granted = granted, askedAtMillis = askedAtMillis)
    }

    /** Lanza el dialogo del sistema. **Best-effort**: absorbe cualquier fallo del lanzador. */
    override fun request() {
        runCatching { launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    override fun markAsked(atMillis: Long) {
        askedAtMillis = atMillis
    }
}
