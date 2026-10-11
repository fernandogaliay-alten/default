package com.sazon.app.data.activity

import com.sazon.app.domain.activity.ActivitySummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementacion de [ActivitySummaryRepository] sobre Room.
 *
 * `observeRecipesGeneratedCount`/`recipesGeneratedCount` mapean la fila ausente a `0`;
 * `incrementRecipesGenerated` ignora un delta no positivo (la regla "solo suma" tambien se defiende
 * aqui, ademas de en el servicio de dominio).
 */
class RoomActivitySummaryRepository(
    private val dao: ActivitySummaryDao,
) : ActivitySummaryRepository {

    override fun observeRecipesGeneratedCount(): Flow<Int> = dao.observeCount().map { it ?: 0 }

    override suspend fun recipesGeneratedCount(): Int = dao.getCount() ?: 0

    override suspend fun incrementRecipesGenerated(delta: Int) {
        if (delta > 0) dao.increment(delta)
    }
}
