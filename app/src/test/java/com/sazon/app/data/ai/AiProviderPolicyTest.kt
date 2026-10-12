package com.sazon.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politica del proveedor de IA (`AiProviderPolicy`): reintentabilidad, `Retry-After`, recorte del
 * timeout por intento, margen de reintento y motivo de degradacion. Pura, sin HTTP ni Android.
 *
 * Casos negativos en cada decision (estados no reintentables, `Retry-After` fuera de rango o
 * ilegible, `canRetry` sin margen) para que los tests no pasen en vacio.
 */
class AiProviderPolicyTest {

    @Test
    fun los_estados_reintentables_son_408_429_y_5xx() {
        listOf(408, 429, 500, 502, 503, 599).forEach { status ->
            assertTrue("El estado $status debe ser reintentable", AiProviderPolicy.isRetryableStatus(status))
        }
    }

    @Test
    fun los_estados_no_reintentables_incluyen_4xx_y_3xx() {
        listOf(200, 201, 302, 400, 401, 403, 404, 422, 499).forEach { status ->
            assertFalse("El estado $status NO debe ser reintentable", AiProviderPolicy.isRetryableStatus(status))
        }
    }

    @Test
    fun el_endpoint_es_la_url_de_chat_completions() {
        assertEquals("https://inference.devexpert.io/v1/chat/completions", AiProviderPolicy.endpointUrl())
        assertEquals("chat", AiProviderPolicy.MODEL_ALIAS)
    }

    @Test
    fun retry_after_corto_se_respeta_y_el_largo_o_ilegible_cae_al_backoff_fijo() {
        assertEquals(2_000L, AiProviderPolicy.retryDelayMillis("2"))
        assertEquals(5_000L, AiProviderPolicy.retryDelayMillis("5"))
        assertEquals(0L, AiProviderPolicy.retryDelayMillis("0"))
        assertEquals(500L, AiProviderPolicy.retryDelayMillis("30"))
        assertEquals(500L, AiProviderPolicy.retryDelayMillis(null))
        assertEquals(500L, AiProviderPolicy.retryDelayMillis("abc"))
        assertEquals(500L, AiProviderPolicy.retryDelayMillis("-1"))
        assertEquals(500L, AiProviderPolicy.retryDelayMillis("Wed, 21 Oct 2015 07:28:00 GMT"))
    }

    @Test
    fun el_timeout_por_intento_se_recorta_a_lo_que_queda_de_deadline() {
        assertEquals(AiProviderPolicy.REQUEST_TIMEOUT_MILLIS, AiProviderPolicy.attemptRequestTimeoutMillis(60_000L))
        assertEquals(5_000L, AiProviderPolicy.attemptRequestTimeoutMillis(5_000L))
        assertEquals(0L, AiProviderPolicy.attemptRequestTimeoutMillis(0L))
        assertEquals(0L, AiProviderPolicy.attemptRequestTimeoutMillis(-1_000L))
    }

    @Test
    fun can_retry_exige_margen_sobre_el_backoff_y_un_intento_disponible() {
        assertTrue(AiProviderPolicy.canRetry(attempt = 1, remainingMillis = 60_000L))
        assertTrue(AiProviderPolicy.canRetry(attempt = 1, remainingMillis = 501L))
        assertFalse(AiProviderPolicy.canRetry(attempt = 1, remainingMillis = 500L))
        assertFalse(AiProviderPolicy.canRetry(attempt = 1, remainingMillis = 0L))
        assertFalse(AiProviderPolicy.canRetry(attempt = 2, remainingMillis = 60_000L))
    }

    @Test
    fun el_motivo_distingue_429_del_resto_de_errores_http() {
        assertEquals(AiUnavailableReason.RATE_LIMITED, AiProviderPolicy.unavailableReason(429))
        assertEquals(AiUnavailableReason.HTTP_ERROR, AiProviderPolicy.unavailableReason(401))
        assertEquals(AiUnavailableReason.HTTP_ERROR, AiProviderPolicy.unavailableReason(500))
    }

    @Test
    fun las_restricciones_del_proveedor_son_neutras() {
        assertEquals("", AiProviderPolicy.NEUTRAL_CONSTRAINTS.dietPatternId)
        assertTrue(AiProviderPolicy.NEUTRAL_CONSTRAINTS.hardRestrictionIds.isEmpty())
    }
}
