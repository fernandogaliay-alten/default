package com.sazon.app.data.ai

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Politica del proveedor de IA (`AiProviderPolicy`): reintentabilidad, `Retry-After`, recorte del
 * timeout por intento y su frontera, margen de reintento, holgura frente a la latencia real medida,
 * clasificacion del fallo de transporte y motivo de degradacion. Pura, sin HTTP ni Android.
 *
 * Casos negativos en cada decision (estados no reintentables, `Retry-After` fuera de rango o
 * ilegible, `canRetry` sin margen, conexion/`IOException` que no son timeout, causa nula) para que
 * los tests no pasen en vacio.
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
        val request = AiProviderPolicy.REQUEST_TIMEOUT_MILLIS
        assertEquals(
            "Con deadline holgado manda la constante por intento",
            request,
            AiProviderPolicy.attemptRequestTimeoutMillis(request + 1),
        )
        assertEquals(
            "En la frontera exacta devuelve la constante",
            request,
            AiProviderPolicy.attemptRequestTimeoutMillis(request),
        )
        assertEquals(
            "Por debajo del techo se recorta al tiempo restante",
            request - 1,
            AiProviderPolicy.attemptRequestTimeoutMillis(request - 1),
        )
        assertEquals(request, AiProviderPolicy.attemptRequestTimeoutMillis(AiProviderPolicy.TOTAL_DEADLINE_MILLIS))
        assertEquals(5_000L, AiProviderPolicy.attemptRequestTimeoutMillis(5_000L))
        assertEquals(0L, AiProviderPolicy.attemptRequestTimeoutMillis(0L))
        assertEquals(0L, AiProviderPolicy.attemptRequestTimeoutMillis(-1_000L))
    }

    // Escenario 1 (de `docs/specs/ai-call-diagnostics.md`): la politica admite la latencia real
    // medida de **varios** modelos/modos (issue #72), con el peor caso `mimo-v2.6-pro` a 115,8 s y el
    // peor del alias diario `chat` a 46,6 s. Con los valores viejos (socket 90 s / request 120 s /
    // deadline 180 s) fallan `SOCKET > WORST`, `SOCKET >= 2x CHAT_WORST` y el margen de reintento: es
    // la prueba de regresion de la correctiva.
    @Test
    fun la_politica_admite_con_holgura_la_latencia_real_de_varios_modelos() {
        assertTrue(
            "El socket timeout debe superar el peor caso medido (mimo-v2.6-pro, 115,8 s)",
            AiProviderPolicy.SOCKET_TIMEOUT_MILLIS > REAL_LATENCY_MILLIS,
        )
        assertTrue(
            "El socket timeout debe cubrir al menos 2x el peor caso del alias diario 'chat' (46,6 s)",
            AiProviderPolicy.SOCKET_TIMEOUT_MILLIS >= 2 * CHAT_WORST_LATENCY_MILLIS,
        )
        assertTrue(
            "El timeout por intento no debe ser menor que el de socket",
            AiProviderPolicy.REQUEST_TIMEOUT_MILLIS >= AiProviderPolicy.SOCKET_TIMEOUT_MILLIS,
        )
        assertEquals(
            "Con deadline holgado el intento usa el techo por intento",
            AiProviderPolicy.REQUEST_TIMEOUT_MILLIS,
            AiProviderPolicy.attemptRequestTimeoutMillis(AiProviderPolicy.TOTAL_DEADLINE_MILLIS),
        )
        assertTrue(
            "El deadline total debe caber el intento completo mas el backoff",
            AiProviderPolicy.TOTAL_DEADLINE_MILLIS >
                AiProviderPolicy.REQUEST_TIMEOUT_MILLIS + AiProviderPolicy.RETRY_BACKOFF_MILLIS,
        )
        assertTrue(
            "Tras el peor intento medido mas el backoff debe quedar deadline para otro intento igual de lento",
            AiProviderPolicy.TOTAL_DEADLINE_MILLIS - REAL_LATENCY_MILLIS - AiProviderPolicy.RETRY_BACKOFF_MILLIS >=
                REAL_LATENCY_MILLIS,
        )
        assertTrue(
            "Con lo que sobra del deadline debe haber margen para un reintento igual de lento",
            AiProviderPolicy.canRetry(
                attempt = 1,
                remainingMillis = AiProviderPolicy.TOTAL_DEADLINE_MILLIS -
                    REAL_LATENCY_MILLIS -
                    AiProviderPolicy.RETRY_BACKOFF_MILLIS,
            ),
        )
        assertEquals("El timeout de conexion no cambia", 10_000L, AiProviderPolicy.CONNECT_TIMEOUT_MILLIS)
        assertTrue(AiProviderPolicy.REQUEST_TIMEOUT_MILLIS >= AiProviderPolicy.CONNECT_TIMEOUT_MILLIS)
    }

    // Escenario 4: clasificacion del fallo de transporte, con positivos y negativos.
    @Test
    fun un_fallo_de_transporte_se_clasifica_por_su_causa() {
        // Positivos: timeouts del modelo/peticion (el socket de Ktor en JVM es java.net).
        assertEquals(
            AiUnavailableReason.TIMEOUT,
            AiProviderPolicy.transportFailureReason(HttpRequestTimeoutException("https://inference.devexpert.io/v1", 120_000)),
        )
        assertEquals(
            AiUnavailableReason.TIMEOUT,
            AiProviderPolicy.transportFailureReason(SocketTimeoutException("timeout de lectura")),
        )
        assertEquals(
            AiUnavailableReason.TIMEOUT,
            AiProviderPolicy.transportFailureReason(InterruptedIOException("call timeout")),
        )
        // Negativos: fallos que no son timeout del modelo (la conexion nunca llego a hablar: red).
        assertEquals(
            AiUnavailableReason.NETWORK,
            AiProviderPolicy.transportFailureReason(ConnectTimeoutException("connect", null)),
        )
        assertEquals(AiUnavailableReason.NETWORK, AiProviderPolicy.transportFailureReason(IOException("sin red")))
        assertEquals(AiUnavailableReason.NETWORK, AiProviderPolicy.transportFailureReason(SocketException("broken pipe")))
        // Cualquier otra causa (incluido null): inesperado.
        assertEquals(
            AiUnavailableReason.UNEXPECTED,
            AiProviderPolicy.transportFailureReason(IllegalStateException("boom")),
        )
        assertEquals(AiUnavailableReason.UNEXPECTED, AiProviderPolicy.transportFailureReason(null))
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

/**
 * Peor latencia real medida (issue #72): una operacion de 6 recetas con `mimo-v2.6-pro` devuelve
 * HTTP 200 en **115,8 s**. La politica de timeouts debe admitirla con holgura (socket 120 s).
 */
private const val REAL_LATENCY_MILLIS = 115_800L

/**
 * Peor latencia medida del alias diario `chat` (issue #72): 46,6 s (las otras mediciones fueron
 * 28,0 / 33,1 / 42,1 s). El socket timeout debe cubrir al menos el doble.
 */
private const val CHAT_WORST_LATENCY_MILLIS = 46_600L
