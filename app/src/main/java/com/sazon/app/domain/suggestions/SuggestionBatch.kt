package com.sazon.app.domain.suggestions

import com.sazon.app.domain.recipe.Recipe

/**
 * Lote de sugerencias cacheado.
 *
 * Contiene sus recetas (`seed` + `ai`) para que la lectura sea autocontenida offline, mas la firma
 * de preferencias con la que se genero (su clave de cache) y el instante de generacion.
 *
 * La frescura NO se persiste: `fresh`/`stale` de `docs/domain-model.md` es el resultado derivado de
 * [SuggestionBatchPolicy.decide], no una columna mas.
 */
data class SuggestionBatch(
    val preferenceSignature: String,
    val generatedAt: Long,
    val recipes: List<Recipe>,
) {
    val recipeIds: List<String> get() = recipes.map { it.id }
}

/** Motivo por el que hay que (re)generar el Lote de sugerencias en vez de usar la cache. */
enum class BatchRefreshReason { MISSING, SIGNATURE_CHANGED, EXPIRED, FORCED }

/**
 * Decision de frescura del Lote: usar la cache existente o generar uno nuevo con su motivo.
 *
 * Es el resultado de [SuggestionBatchPolicy.decide] y no se persiste.
 */
sealed interface BatchDecision {
    data object UseCached : BatchDecision
    data class Generate(val reason: BatchRefreshReason) : BatchDecision
}
