package com.sazon.app.ui.cooking

/**
 * Costura del **almacen de la sesion del Modo cocina** (feature `cooking-timer-foreground-service`):
 * guarda la [CookingSession] para que el toque en la notificacion devuelva al paso exacto incluso si el
 * proceso habia muerto.
 *
 * Predominan las mismas dos reglas que en [CookingAlarm]: es **pura** en `ui.cooking` (sin `android.*`
 * ni `androidx.*`; el adaptador de plataforma —Room— vive en `data.cooking`) y su implementacion por
 * defecto es **no-op**, de modo que previews, tests y composiciones sin cableado compilan y no guardan
 * ni reanudan nada.
 *
 * Contrato: **degradacion silenciosa** (ADR 0005). Ninguna implementacion lanza; una fila ilegible o un
 * almacen no disponible se comportan como "sin sesion" y nunca rompen el Modo cocina.
 */
interface CookingSessionStore {

    /** Sesion persistida o `null` si no hay (o no se puede leer). Nunca lanza. */
    suspend fun load(): CookingSession?

    /** Guarda (o **reemplaza**) la sesion unica. Nunca lanza. */
    suspend fun save(session: CookingSession)

    /** Borra la sesion (salida explicita del modo). Nunca lanza. */
    suspend fun clear()
}

/**
 * Default **sin efecto** (patron [NoOpCookingAlarm]): nada se guarda y nada se reanuda. Previews, tests
 * y composiciones aisladas no necesitan Android ni persistencia, y el Modo cocina funciona exactamente
 * igual.
 */
object NoOpCookingSessionStore : CookingSessionStore {

    override suspend fun load(): CookingSession? = null

    override suspend fun save(session: CookingSession) = Unit

    override suspend fun clear() = Unit
}
