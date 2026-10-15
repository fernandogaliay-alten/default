package com.sazon.app.data.cooking

import com.sazon.app.ui.cooking.CookingSession
import com.sazon.app.ui.cooking.CookingSessionStore

/**
 * Adaptador Room de la costura [CookingSessionStore]: una fila unica en `cooking_session.db`.
 *
 * **Degradacion silenciosa** (ADR 0005): las tres operaciones van envueltas en `runCatching`, de modo
 * que un fallo de disco, una fila ilegible o un `Recipe` que ya no decodifica se comportan como "sin
 * sesion" y **nunca** rompen el Modo cocina ni la app.
 */
class RoomCookingSessionStore(private val dao: CookingSessionDao) : CookingSessionStore {

    override suspend fun load(): CookingSession? = runCatching { dao.get()?.toDomain() }.getOrNull()

    override suspend fun save(session: CookingSession) {
        runCatching { dao.upsert(session.toEntity()) }
    }

    override suspend fun clear() {
        runCatching { dao.deleteAll() }
    }
}
