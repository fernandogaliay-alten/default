package com.sazon.app.data.suggestions

import com.sazon.app.domain.suggestions.SuggestionBatch
import com.sazon.app.domain.suggestions.SuggestionBatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementacion de [SuggestionBatchRepository] sobre Room. */
class RoomSuggestionBatchRepository(
    private val dao: SuggestionBatchDao,
) : SuggestionBatchRepository {

    override suspend fun get(): SuggestionBatch? = dao.get()?.toDomain()

    override fun observe(): Flow<SuggestionBatch?> = dao.observe().map { it?.toDomain() }

    override suspend fun save(batch: SuggestionBatch) = dao.upsert(batch.toEntity())

    override suspend fun clear() = dao.deleteAll()
}
