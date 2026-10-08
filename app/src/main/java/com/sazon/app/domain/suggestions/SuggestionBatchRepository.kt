package com.sazon.app.domain.suggestions

import kotlinx.coroutines.flow.Flow

/**
 * Puerto de persistencia del Lote de sugerencias.
 *
 * El dominio depende de esta abstraccion; la implementacion sobre Room vive en
 * `com.sazon.app.data.suggestions`.
 */
interface SuggestionBatchRepository {

    suspend fun get(): SuggestionBatch?

    fun observe(): Flow<SuggestionBatch?>

    suspend fun save(batch: SuggestionBatch)

    suspend fun clear()
}
