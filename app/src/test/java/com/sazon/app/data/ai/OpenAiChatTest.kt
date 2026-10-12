package com.sazon.app.data.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenario 3 de `docs/specs/ai-response-format.md`: serializacion del DTO con el **mismo** `Json` que
 * usa el cliente (`ignoreUnknownKeys = true`, `encodeDefaults = true`).
 *
 * Prueba negativa clave: sin formato la clave `response_format` **nunca** aparece (ni como `null`);
 * con [ResponseFormat.JSON_OBJECT] aparece como `{"type":"json_object"}`.
 */
class OpenAiChatTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun sin_formato_no_se_serializa_la_clave_ni_un_null() {
        val request = ChatCompletionRequest(
            model = AiProviderPolicy.MODEL_ALIAS,
            messages = listOf(ChatMessage(role = "user", content = "hola")),
        )

        val text = json.encodeToString(ChatCompletionRequest.serializer(), request)

        assertFalse("Nunca la clave response_format", text.contains("response_format"))
        assertFalse("Nunca un response_format nulo", text.contains("null"))
        assertTrue("El resto del contrato no cambia", text.contains("\"model\":\"chat\""))
        assertNull(responseFormatType(text))
    }

    @Test
    fun con_el_formato_activo_se_emite_json_object() {
        val request = ChatCompletionRequest(
            model = AiProviderPolicy.MODEL_ALIAS,
            messages = listOf(ChatMessage(role = "user", content = "hola")),
            responseFormat = ResponseFormat.JSON_OBJECT,
        )

        val text = json.encodeToString(ChatCompletionRequest.serializer(), request)

        assertEquals("json_object", responseFormatType(text))
        assertEquals(
            "El type sale de la politica, en un unico sitio",
            AiProviderPolicy.RESPONSE_FORMAT_JSON_OBJECT,
            ResponseFormat.JSON_OBJECT.type,
        )
    }

    private fun responseFormatType(text: String): String? =
        Json.parseToJsonElement(text)
            .jsonObject["response_format"]
            ?.jsonObject
            ?.get("type")
            ?.jsonPrimitive
            ?.content
}
