package com.sazon.app.domain.suggestions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Escenarios 1–6 de la spec: frescura, firma, caducidad, frontera del TTL y `force`.
 *
 * Logica pura, sin Android ni Robolectric. [SuggestionBatchPolicy.decide] no lee reloj ni disco:
 * recibe `now`, `force` y la cache, por lo que es determinista.
 */
class SuggestionBatchPolicyTest {

    private val now = 1_700_000_000_000L
    private val signature = "firma-actual"

    private fun lote(signature: String, generatedAt: Long) =
        SuggestionBatch(preferenceSignature = signature, generatedAt = generatedAt, recipes = emptyList())

    @Test
    fun el_ttl_es_de_siete_dias() {
        assertEquals(7L * 24 * 60 * 60 * 1_000, SuggestionBatchPolicy.BATCH_TTL_MILLIS)
    }

    @Test
    fun sin_lote_en_disco_la_decision_es_generar_por_missing() {
        val decision = SuggestionBatchPolicy.decide(cached = null, currentSignature = signature, now = now)
        assertEquals(BatchDecision.Generate(BatchRefreshReason.MISSING), decision)
    }

    @Test
    fun un_lote_ausente_manda_sobre_force() {
        val decision =
            SuggestionBatchPolicy.decide(cached = null, currentSignature = signature, now = now, force = true)
        assertEquals(BatchDecision.Generate(BatchRefreshReason.MISSING), decision)
    }

    @Test
    fun firma_igual_y_dentro_del_ttl_usa_la_cache() {
        val cached = lote(signature, generatedAt = now - 1_000L)
        val decision = SuggestionBatchPolicy.decide(cached, currentSignature = signature, now = now)
        assertEquals(BatchDecision.UseCached, decision)
    }

    @Test
    fun una_firma_distinta_marca_el_lote_como_obsoleto() {
        val cached = lote("firma-antigua", generatedAt = now - 1_000L)
        val decision = SuggestionBatchPolicy.decide(cached, currentSignature = signature, now = now)
        assertEquals(BatchDecision.Generate(BatchRefreshReason.SIGNATURE_CHANGED), decision)
    }

    @Test
    fun un_lote_caducado_marca_expired() {
        val cached = lote(signature, generatedAt = now - SuggestionBatchPolicy.BATCH_TTL_MILLIS)
        val decision = SuggestionBatchPolicy.decide(cached, currentSignature = signature, now = now)
        assertEquals(BatchDecision.Generate(BatchRefreshReason.EXPIRED), decision)
    }

    @Test
    fun en_la_frontera_del_ttl_un_milisegundo_menos_todavia_sirve_la_cache() {
        val justoAntes = lote(signature, generatedAt = now - SuggestionBatchPolicy.BATCH_TTL_MILLIS + 1)
        assertEquals(
            BatchDecision.UseCached,
            SuggestionBatchPolicy.decide(justoAntes, currentSignature = signature, now = now),
        )

        val justoEnElLimite = lote(signature, generatedAt = now - SuggestionBatchPolicy.BATCH_TTL_MILLIS)
        assertEquals(
            BatchDecision.Generate(BatchRefreshReason.EXPIRED),
            SuggestionBatchPolicy.decide(justoEnElLimite, currentSignature = signature, now = now),
        )
    }

    @Test
    fun force_refresca_un_lote_fresco_con_la_misma_firma() {
        val cached = lote(signature, generatedAt = now - 1_000L)
        val decision =
            SuggestionBatchPolicy.decide(cached, currentSignature = signature, now = now, force = true)
        assertEquals(BatchDecision.Generate(BatchRefreshReason.FORCED), decision)
    }
}
