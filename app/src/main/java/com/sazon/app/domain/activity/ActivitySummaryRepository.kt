package com.sazon.app.domain.activity

import kotlinx.coroutines.flow.Flow

/**
 * Puerto de persistencia del Resumen de actividad.
 *
 * Solo cubre el contador **persistido** de recetas generadas; el numero de Favoritas es derivado de la
 * lista observada de Favoritos y no se guarda aqui (ver `docs/domain-model.md`). El dominio depende de
 * esta abstraccion; la implementacion sobre Room vive en `com.sazon.app.data.activity`.
 */
interface ActivitySummaryRepository {

    /** Emite el contador persistido (0 si nunca se ha escrito la fila). */
    fun observeRecipesGeneratedCount(): Flow<Int>

    /** Lee el contador persistido (0 si nunca se ha escrito la fila). */
    suspend fun recipesGeneratedCount(): Int

    /** Suma [delta] al contador persistido; un [delta] no positivo es un no-op. */
    suspend fun incrementRecipesGenerated(delta: Int)
}
