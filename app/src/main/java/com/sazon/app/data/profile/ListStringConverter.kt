package com.sazon.app.data.profile

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Convierte cada lista de `String` en una columna JSON con `kotlinx.serialization`.
 *
 * Se usa JSON en lugar de un separador artesanal para no romper con comas dentro de un id.
 * No se ordena: se preserva el orden tal cual para que el ida y vuelta sea exacto.
 */
class ListStringConverter {

    private val format = Json

    @TypeConverter
    fun fromList(values: List<String>): String =
        format.encodeToString(ListSerializer(String.serializer()), values)

    @TypeConverter
    fun toList(value: String): List<String> =
        format.decodeFromString(ListSerializer(String.serializer()), value)
}
