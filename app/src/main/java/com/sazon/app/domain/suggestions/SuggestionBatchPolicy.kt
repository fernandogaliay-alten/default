package com.sazon.app.domain.suggestions

/**
 * Politica de frescura del Lote de sugerencias.
 *
 * Es logica pura y determinista: recibe la cache, la firma actual, `now` y `force`; no lee reloj ni
 * disco. La caducidad vive en un unico sitio, [BATCH_TTL_MILLIS]
 * (`docs/risks-and-open-questions.md`).
 */
object SuggestionBatchPolicy {

    /** Caducidad del Lote: 7 dias, en un unico sitio configurable. */
    const val BATCH_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1_000

    /**
     * Orden de decision: ausente -> `MISSING`, peticion explicita -> `FORCED`, firma distinta ->
     * `SIGNATURE_CHANGED`, caducado -> `EXPIRED`, si no usar cache.
     *
     * A los `BATCH_TTL_MILLIS` exactos el lote ya esta obsoleto (`elapsed >= TTL`).
     */
    fun decide(
        cached: SuggestionBatch?,
        currentSignature: String,
        now: Long,
        force: Boolean = false,
    ): BatchDecision {
        if (cached == null) return BatchDecision.Generate(BatchRefreshReason.MISSING)
        if (force) return BatchDecision.Generate(BatchRefreshReason.FORCED)
        if (cached.preferenceSignature != currentSignature) {
            return BatchDecision.Generate(BatchRefreshReason.SIGNATURE_CHANGED)
        }
        val elapsed = now - cached.generatedAt
        if (elapsed >= BATCH_TTL_MILLIS) return BatchDecision.Generate(BatchRefreshReason.EXPIRED)
        return BatchDecision.UseCached
    }
}
