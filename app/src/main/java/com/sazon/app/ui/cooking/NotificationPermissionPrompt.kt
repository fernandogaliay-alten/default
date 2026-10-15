package com.sazon.app.ui.cooking

/**
 * Costura de la peticion del permiso de notificaciones del Modo cocina (feature
 * `cooking-timer-alarm-permission-context`): entrega el estado **inyectado** de esta sesion y lanza el
 * dialogo del sistema.
 *
 * Predominan las mismas dos reglas que en [CookingAlarm]: es **pura** en `ui.cooking` (sin `android.*`
 * ni `androidx.*`; el adaptador de plataforma vive en `data.cooking`) y su implementacion por defecto
 * es **no-op**, de modo que previews, tests y composiciones sin cableado compilan y no piden nada.
 *
 * Contrato: **degradacion silenciosa** (ADR 0005). Ninguna implementacion lanza; denegar el permiso no
 * puede romper el Modo cocina ni gatear el temporizador.
 */
interface NotificationPermissionPrompt {

    /** Estado de esta sesion: permiso actual + instante del ultimo intento (`null` = nunca). */
    fun state(): NotificationPermissionState

    /** Lanza el dialogo del sistema. **Best-effort**: nunca lanza. */
    fun request()

    /** Registra el intento con el instante **inyectado** (memoria de sesion). */
    fun markAsked(atMillis: Long)
}

/**
 * Default **sin efecto** (patron [NoOpCookingAlarm]): declara el permiso como concedido para que nada se
 * pida. Previews, tests y composiciones aisladas no necesitan Android ni permisos, y el Modo cocina
 * funciona exactamente igual.
 */
object NoOpNotificationPermissionPrompt : NotificationPermissionPrompt {

    override fun state(): NotificationPermissionState = NotificationPermissionState(granted = true)

    override fun request() = Unit

    override fun markAsked(atMillis: Long) = Unit
}
