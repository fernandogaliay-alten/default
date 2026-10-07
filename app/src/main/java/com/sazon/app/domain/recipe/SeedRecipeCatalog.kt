package com.sazon.app.domain.recipe

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Catálogo local de recetas semilla, tal y como viaja en el asset `seed-recipes.json`.
 *
 * El envelope `{ "recipes": [...] }` se decodifica directamente en esta clase; no hay una clase
 * intermedia. El parser vive en el dominio (sin Android) para poder testarse en JVM, igual que
 * [com.sazon.app.domain.taxonomy.TaxonomyJson].
 */
@Serializable
data class SeedRecipeCatalog(val recipes: List<Recipe>)

/**
 * Parser puro del catálogo de recetas semilla.
 *
 * Los campos desconocidos se ignoran para que el contenido pueda crecer sin romper el parser. Si el
 * asset falta o el JSON está malformado, [parse] lanza [kotlinx.serialization.SerializationException]:
 * es un fallo de build que cubre el test de integridad, no una degradación en ejecución.
 */
object SeedRecipeJson {

    private val format = Json { ignoreUnknownKeys = true }

    fun parse(source: String): SeedRecipeCatalog =
        format.decodeFromString(SeedRecipeCatalog.serializer(), source)
}
