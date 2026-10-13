package com.sazon.app.data.ai.image

import java.util.Base64
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenario 7 de `docs/specs/dish-image-generator.md`: un `200` malformado devuelve `null` sin lanzar
 * y sin intentar reducir nada. JVM puro, sin Android ni red.
 */
class DishImagePayloadTest {

    private val payload = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x01, 0x02)

    @Test
    fun un_b64_json_valido_se_decodifica_a_bytes() {
        val body = Json.encodeToString(
            ImageGenerationResponse.serializer(),
            ImageGenerationResponse(
                listOf(ImageGenerationDatum(b64Json = Base64.getEncoder().encodeToString(payload))),
            ),
        )

        assertArrayEquals(payload, DishImagePayload.decode(body))
    }

    // Elige el primer `b64_json` no vacio aunque venga precedido de entradas incompletas.
    @Test
    fun elige_el_primer_b64_json_utilizable() {
        val body = Json.encodeToString(
            ImageGenerationResponse.serializer(),
            ImageGenerationResponse(
                listOf(
                    ImageGenerationDatum(b64Json = null),
                    ImageGenerationDatum(b64Json = "   "),
                    ImageGenerationDatum(b64Json = Base64.getEncoder().encodeToString(payload)),
                ),
            ),
        )

        assertArrayEquals(payload, DishImagePayload.decode(body))
    }

    // Un cuerpo 200 malformado devuelve null sin lanzar en TODOS los casos.
    @Test
    fun un_200_malformado_devuelve_null_sin_lanzar() {
        val cases = listOf(
            null,
            "",
            "   ",
            "<html>vaya</html>",
            "{}",
            """{"data":[]}""",
            """{"data":[{}]}""",
            """{"data":[{"b64_json":""}]}""",
            """{"data":[{"b64_json":"   "}]}""",
            """{"data":[{"b64_json":"no-es-base64!!"}]}""",
            """{"data":"no-es-lista"}""",
            """{"data":{"b64_json":"abc"}}""",
        )

        cases.forEach { body ->
            assertNull("El cuerpo '$body' debe degradar a null", DishImagePayload.decode(body))
        }
    }
}
