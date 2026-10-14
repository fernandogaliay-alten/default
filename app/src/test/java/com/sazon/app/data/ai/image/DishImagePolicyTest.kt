package com.sazon.app.data.ai.image

import com.sazon.app.data.ai.AiProviderPolicy
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 3 de `docs/specs/dish-image-generator.md`: la geometria de la reduccion (`scaledSize`) es
 * **pura** y nunca agranda. Ademas fija la coherencia del endpoint, el alias, el tamano, la calidad y
 * los timeouts propios (mas cortos que los del chat). Todo JVM puro.
 */
class DishImagePolicyTest {

    // Escenario 3 (correctiva `chat-provider-opencode-go`): la imagen tiene su **propia** base de
    // DevExpert y **no** la deriva de la base del chat (que ahora es OpenCode Go).
    @Test
    fun el_endpoint_usa_la_base_propia_de_devexpert_y_no_la_del_chat() {
        assertEquals("https://inference.devexpert.io/v1", DishImagePolicy.BASE_URL)
        assertEquals(
            "https://inference.devexpert.io/v1/images/generations",
            DishImagePolicy.endpointUrl(),
        )
        assertEquals(DishImagePolicy.BASE_URL + "/images/generations", DishImagePolicy.endpointUrl())
        assertNotEquals(
            "La base de la imagen no puede ser la del chat",
            AiProviderPolicy.BASE_URL,
            DishImagePolicy.BASE_URL,
        )
    }

    // El alias, el tamano generado, el ancho objetivo, la calidad y los intentos son los decididos.
    @Test
    fun las_constantes_del_contrato_son_las_decididas() {
        assertEquals("image", DishImagePolicy.MODEL_ALIAS)
        assertEquals("1024x1024", DishImagePolicy.IMAGE_SIZE)
        assertEquals(960, DishImagePolicy.TARGET_WIDTH_PX)
        assertEquals(80, DishImagePolicy.WEBP_QUALITY)
        assertEquals("Sin reintentos: no se gasta cuota dos veces", 1, DishImagePolicy.MAX_ATTEMPTS)
    }

    // La politica de imagen es propia y mas corta que la de chat (no comparte su cola de razonamiento).
    @Test
    fun los_timeouts_de_imagen_son_mas_cortos_que_los_de_chat() {
        val timeouts = DishImagePolicy.TIMEOUTS
        assertTrue(timeouts.connectMillis > 0)
        assertTrue(timeouts.socketMillis > 0)
        assertTrue(timeouts.requestMillis > 0)
        assertTrue(
            "El socket de imagen debe ser mas corto que el de chat",
            timeouts.socketMillis < AiProviderPolicy.SOCKET_TIMEOUT_MILLIS,
        )
        assertTrue(
            "La peticion de imagen debe ser mas corta que la de chat",
            timeouts.requestMillis < AiProviderPolicy.REQUEST_TIMEOUT_MILLIS,
        )
        assertTrue(
            "El techo de peticion cubre el de socket",
            timeouts.requestMillis > timeouts.socketMillis,
        )
    }

    // Escenario 2: una fuente 1024x1024 se reduce a 960x960 (alto proporcional).
    @Test
    fun un_origen_cuadrado_1024_se_reduce_a_960_cuadrado() {
        assertArrayEquals(intArrayOf(960, 960), DishImagePolicy.scaledSize(1024, 1024))
    }

    // Escenario 3: un origen mas estrecho que 960 NO se agranda: conserva sus dimensiones.
    @Test
    fun un_origen_menor_que_960_no_se_agranda() {
        assertArrayEquals(intArrayOf(400, 300), DishImagePolicy.scaledSize(400, 300))
        assertArrayEquals(intArrayOf(960, 540), DishImagePolicy.scaledSize(960, 540))
        assertArrayEquals(intArrayOf(1, 1), DishImagePolicy.scaledSize(1, 1))
    }

    // El alto se mantiene proporcional al reducir (y se redondea al entero mas cercano).
    @Test
    fun el_alto_reducido_es_proporcional() {
        assertArrayEquals(intArrayOf(960, 540), DishImagePolicy.scaledSize(1920, 1080))
        assertArrayEquals(intArrayOf(960, 600), DishImagePolicy.scaledSize(1280, 800))
        assertArrayEquals(intArrayOf(960, 100), DishImagePolicy.scaledSize(961, 100))
    }

    // Un alto proporcional que redondearia a 0 se acota al minimo de 1 px (nunca un bitmap vacio).
    @Test
    fun el_alto_reducido_se_acota_al_minimo_de_un_pixel() {
        assertArrayEquals(intArrayOf(960, 1), DishImagePolicy.scaledSize(2_000, 1))
    }

    // Escenario 11: el ajuste vive en este unico sitio y usa la base propia de DevExpert.
    @Test
    fun el_endpoint_de_ajuste_usa_la_base_propia_y_la_ruta_de_edits() {
        assertEquals(
            "https://inference.devexpert.io/v1/images/edits",
            DishImagePolicy.editsUrl(),
        )
        assertEquals(DishImagePolicy.BASE_URL + "/images/edits", DishImagePolicy.editsUrl())
        assertEquals("image-edit", DishImagePolicy.EDIT_MODEL_ALIAS)
        assertTrue("El anclaje esta activo por defecto", DishImagePolicy.STYLE_REFERENCE_ENABLED)
    }

    // Escenario 11: solo un rechazo definitivo dispara el respaldo por texto; ningun otro estado.
    @Test
    fun el_respaldo_por_texto_solo_lo_dispara_un_rechazo_definitivo() {
        val fallback = setOf(400, 404, 405, 413, 415, 422)
        assertEquals(fallback, DishImagePolicy.EDIT_FALLBACK_STATUSES)
        fallback.forEach { status ->
            assertTrue(
                "El estado $status debe disparar respaldo",
                DishImagePolicy.editFailureRequiresTextFallback(status),
            )
        }
        listOf(401, 403, 408, 429, 500, 502, 503, 200, 201, 204, 100, 999).forEach { status ->
            assertFalse(
                "El estado $status NO debe disparar respaldo",
                DishImagePolicy.editFailureRequiresTextFallback(status),
            )
        }
    }
}
