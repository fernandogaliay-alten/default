package com.sazon.app.domain.prompt

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Ingrediente disponible para una peticion.
 *
 * [ingredientId] apunta a la Taxonomia; `null` significa ingrediente libre, que solo se conoce por
 * [name].
 */
data class PromptIngredient(
    val name: String,
    val ingredientId: String? = null,
)

/**
 * Peticion para la que se compone el prompt: el Perfil de preferencias mas el contexto opcional
 * (ajustes de la generacion, ingredientes disponibles y textos libres del usuario).
 */
data class PromptRequest(
    val preferences: PreferenceProfile,
    val servings: Int = preferences.defaultServings,
    val ingredients: List<PromptIngredient> = emptyList(),
    val mealTypeId: String? = null,
    val timeBucketId: String? = null,
    val difficultyId: String? = null,
    val query: String? = null,
    val notes: String? = null,
)

/**
 * Prompt ya compuesto y separado en el bloque estable de sistema y el bloque por peticion de usuario.
 *
 * [system] depende solo de la Taxonomia (rol, contrato de salida, unidades y enumeracion de ids
 * validos); [user] traduce el Perfil y el contexto concreto de la peticion.
 */
data class BuiltPrompt(
    val system: String,
    val user: String,
)

/**
 * Constructor del prompt que la app envia a la IA a partir del Perfil de preferencias.
 *
 * Traduce los `*Id` seleccionados a las etiquetas y `promptHint` de la Taxonomia y exige una salida
 * estructurada con `id` de la Taxonomia para todo lo clasificable (contrato de
 * `docs/domain/taxonomy.md` seccion 4). Garantiza que el patron de dieta y las restricciones duras
 * aparecen **siempre** en el prompt.
 *
 * Es logica pura de dominio: sin Android, sin red, sin persistencia, sin coroutines y sin estado
 * mutable, de modo que la misma entrada produce siempre el mismo texto. Nunca lanza por datos de
 * entrada: los ids desconocidos se omiten y las listas vacias se degradan a un prompt valido.
 */
object PromptBuilder {

    /** Numero de recetas pedidas por generacion (decision de `docs/risks-and-open-questions.md`). */
    const val RECIPES_PER_GENERATION = 6

    /** Esqueleto del contrato de salida exigido al modelo (campos de `taxonomy.md` seccion 4). */
    private val OUTPUT_CONTRACT = """
        {
          "recipes": [
            {
              "title": "string",
              "description": "string (una línea)",
              "categoryId": "recipeCategories.id",
              "mealTypeId": "mealTypes.id",
              "cuisineId": "cuisines.id",
              "totalMinutes": 25,
              "prepMinutes": 10,
              "cookMinutes": 15,
              "difficultyId": "difficulties.id",
              "servings": 2,
              "ingredients": [
                { "name": "string", "quantity": 200, "unit": "g", "isPantry": false }
              ],
              "steps": [
                { "order": 1, "title": "string", "description": "string", "minutes": 5 }
              ],
              "tips": ["string"],
              "nutrition": { "kcal": 420, "protein": 25, "carbs": 30, "fat": 18 },
              "declaredAllergens": ["hardRestrictions.id"],
              "dishKey": "string (opcional)",
              "imagePrompt": "string (opcional)"
            }
          ]
        }
    """.trimIndent()

    fun build(taxonomy: Taxonomy, request: PromptRequest): BuiltPrompt = BuiltPrompt(
        system = buildSystem(taxonomy),
        user = buildUser(taxonomy, request),
    )

    /** Bloque estable: rol, regla de salida, contrato, unidades, despensa e ids validos. */
    private fun buildSystem(taxonomy: Taxonomy): String = buildString {
        appendLine("Eres un asistente de cocina experto que propone recetas para una app en español.")
        appendLine()
        appendLine("Responde ÚNICAMENTE con el JSON pedido, sin markdown, sin comentarios y sin texto adicional.")
        appendLine("El JSON debe tener esta forma (un objeto con un array \"recipes\"):")
        appendLine(OUTPUT_CONTRACT)
        appendLine()
        appendLine("Genera exactamente $RECIPES_PER_GENERATION recetas.")
        appendLine()
        appendLine("En \"unit\" usa solo uno de estos valores: g, ml, ud, cda, cdta, o null para \"al gusto\".")
        appendLine("La despensa básica se asume disponible y no hace falta declararla: sal, aceite, pimienta y agua.")
        appendLine()
        appendLine("Campos opcionales de cada receta: \"dishKey\" (nombre breve y específico del plato terminado; nunca un ingrediente ni la categoría) e \"imagePrompt\" (descripción visual breve del plato terminado). Si no los envías, la app usará el título y la taxonomía.")
        appendLine()
        appendLine("Respeta SIEMPRE el patrón de dieta y las restricciones duras que se indican más abajo;")
        appendLine("si una receta incumple una restricción dura, descártala y propón otra.")
        appendLine()
        appendLine("En los campos clasificables (\"categoryId\", \"mealTypeId\", \"cuisineId\", \"difficultyId\" y \"declaredAllergens\")")
        appendLine("usa exclusivamente los siguientes id; nunca escribas texto libre en esos campos:")
        appendLine()
        appendIdList("recipeCategories (categoryId)", taxonomy.recipeCategories.map { it.id to it.label })
        appendIdList("mealTypes (mealTypeId)", taxonomy.mealTypes.map { it.id to it.label })
        appendIdList("cuisines (cuisineId)", taxonomy.cuisines.map { it.id to it.label })
        appendIdList("difficulties (difficultyId)", taxonomy.difficulties.map { it.id to it.label })
        appendIdList("hardRestrictions (declaredAllergens)", taxonomy.hardRestrictions.map { it.id to it.label })
    }

    /** Bloque por peticion: Perfil traducido y contexto opcional, con secciones fijas de seguridad. */
    private fun buildUser(taxonomy: Taxonomy, request: PromptRequest): String = buildString {
        val preferences = request.preferences

        // 1. Patrón de dieta: siempre presente.
        appendLine("Patrón de dieta:")
        val dietPattern = taxonomy.dietPattern(preferences.dietPatternId)
        if (dietPattern == null) {
            appendLine("- (id no reconocido; omitido)")
        } else {
            appendLine("- ${dietPattern.id} — ${dietPattern.label}")
            dietPattern.promptHint?.takeIf { it.isNotBlank() }?.let { appendLine("  $it") }
        }
        appendLine()

        // 2. Restricciones duras: sección de seguridad, nunca se omite.
        appendLine("Restricciones duras:")
        val restrictions = preferences.hardRestrictionIds.mapNotNull { taxonomy.restriction(it) }
        if (restrictions.isEmpty()) {
            appendLine("- Ninguna")
        } else {
            restrictions.forEach { appendLine("- ${it.id} — ${it.label}") }
        }
        appendLine()

        // 3. Preferencias blandas: solo ranking.
        appendLine("Preferencias blandas (solo ordenan, nunca descartan):")
        val softPreferences = preferences.softPreferenceIds.mapNotNull { taxonomy.softPreference(it) }
        if (softPreferences.isEmpty()) {
            appendLine("- Ninguna")
        } else {
            softPreferences.forEach { preference ->
                appendLine("- ${preference.id} — ${preference.label}")
                preference.promptHint?.takeIf { it.isNotBlank() }?.let { appendLine("  $it") }
            }
        }
        appendLine()

        // 4. Cocinas favoritas.
        appendLine("Cocinas favoritas:")
        val cuisines = preferences.cuisineIds.mapNotNull { taxonomy.cuisine(it) }
        if (cuisines.isEmpty()) {
            appendLine("- Ninguna")
        } else {
            cuisines.forEach { appendLine("- ${it.label}") }
        }
        appendLine()

        // 5. Ingredientes preferidos y evitados.
        appendLine("Ingredientes que me gustan:")
        appendIngredientLabels(this, taxonomy, preferences.preferredIngredientIds)
        appendLine("Ingredientes que evito:")
        appendIngredientLabels(this, taxonomy, preferences.dislikedIngredientIds)
        appendLine()

        // 6. Ajustes de la generación.
        appendLine("Ajustes:")
        request.mealTypeId?.let { id ->
            taxonomy.mealType(id)?.let { appendLine("- Comida: ${it.label}") }
        }
        request.difficultyId?.let { id ->
            taxonomy.difficulty(id)?.let { appendLine("- Dificultad: ${it.label}") }
        }
        request.timeBucketId?.let { id ->
            taxonomy.timeBucket(id)?.let { bucket ->
                val limit = bucket.maxMinutes
                    ?.let { max -> "hasta $max minutos" }
                    ?: "sin límite máximo"
                appendLine("- Tiempo: ${bucket.label} ($limit)")
            }
        }
        appendLine("- Comensales: ${request.servings}")
        appendLine()

        // 7. Ingredientes disponibles (de Taxonomía o libres).
        if (request.ingredients.isNotEmpty()) {
            appendLine("Ingredientes disponibles:")
            request.ingredients.forEach { ingredient ->
                val label = ingredient.ingredientId
                    ?.let { id -> taxonomy.ingredient(id)?.label }
                    ?: ingredient.name
                appendLine("- $label")
            }
            appendLine()
        }

        // 8. Textos libres del usuario.
        if (request.query != null || request.notes != null) {
            request.query?.let { appendLine("Búsqueda: $it") }
            request.notes?.let { appendLine("Notas: $it") }
            appendLine()
        }

        // 9. Petición explícita de recetas.
        appendLine(
            "Genera exactamente $RECIPES_PER_GENERATION recetas que cumplan " +
                "el patrón de dieta y las restricciones duras indicadas.",
        )
    }
}

private fun StringBuilder.appendIdList(title: String, entries: List<Pair<String, String>>) {
    appendLine("$title:")
    entries.forEach { (id, label) -> appendLine("- $id — $label") }
}

private fun appendIngredientLabels(
    builder: StringBuilder,
    taxonomy: Taxonomy,
    ids: List<String>,
) {
    val labels = ids.mapNotNull { taxonomy.ingredient(it)?.label }
    if (labels.isEmpty()) {
        builder.appendLine("- Ninguno")
    } else {
        labels.forEach { builder.appendLine("- $it") }
    }
}
