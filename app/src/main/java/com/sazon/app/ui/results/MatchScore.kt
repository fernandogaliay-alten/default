package com.sazon.app.ui.results

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.taxonomy.Taxonomy
import java.text.Normalizer

/**
 * Formula **derivada** de la coincidencia de una receta con el Perfil y con los ingredientes que el
 * usuario ha introducido en el Generador.
 *
 * Logica **pura** (sin Compose ni Android, solo `domain.*`), determinista (sin reloj ni `Random`) y
 * **no persistida**: se recalcula al mostrar resultados y se materializa en memoria con
 * `recipe.copy(matchScore = ...)` (ver `ResultsStatus.from`). Fija la pregunta abierta de `matchScore`
 * (`docs/risks-and-open-questions.md`) segun `docs/specs/results-screen.md`.
 *
 * Es un **heurisitico de presentacion**, no una regla de seguridad: la seguridad la garantiza
 * [com.sazon.app.domain.validation.RecipeValidator] (una receta insegura nunca llega aqui).
 *
 * Señales, cada una una fraccion `0..1`, con pesos como constantes en este unico sitio:
 * - `coverage` (`W_COVERAGE`): ingredientes del usuario presentes en la receta / nº de ingredientes del
 *   usuario; aplica solo con `userIngredients` no vacio.
 * - `liked` (`W_LIKED`): ingredientes **resueltos** de la receta que son preferidos del usuario / nº de
 *   ingredientes resueltos; aplica si la receta tiene al menos un ingrediente resuelto.
 * - `cuisine` (`W_CUISINE`): `1` si la cocina de la receta esta entre las cocinas favoritas.
 * - `time` (`W_TIME`): `1` si `totalMinutes <= maxMinutes` de la franja del Perfil; aplica solo si la
 *   franja tiene `maxMinutes != null`.
 * - `difficulty` (`W_DIFFICULTY`): `1` si la dificultad coincide con la del Perfil.
 *
 * Un ingrediente del usuario casa con uno de la receta si ambos tienen el mismo `ingredientId`, o por
 * **nombre normalizado** (`trim` + minusculas + sin acentos), de modo que un ingrediente libre tambien
 * cuenta. "Ingrediente resuelto" es `ingredientId != null`.
 *
 * Penalizacion: [DISLIKE_PENALTY] puntos por cada ingrediente resuelto de la receta que este en
 * `dislikedIngredientIds` (preferencia blanda: **penaliza, nunca descarta**).
 *
 * `score = clamp(round(100 * Σ(w·f) / Σ(w aplicables)) − penalizacion, 0, 100)`.
 */
object MatchScore {

    /** Peso de la cobertura de los ingredientes del usuario. */
    const val W_COVERAGE = 3

    /** Peso de los ingredientes preferidos del usuario presentes en la receta. */
    const val W_LIKED = 3

    /** Peso de la cocina favorita. */
    const val W_CUISINE = 2

    /** Peso de la franja de tiempo disponible. */
    const val W_TIME = 1

    /** Peso de la dificultad preferida. */
    const val W_DIFFICULTY = 1

    /** Puntos que resta cada ingrediente resuelto que el usuario evita (nunca descarta). */
    const val DISLIKE_PENALTY = 10

    /**
     * Devuelve la coincidencia `0..100` de [recipe] con [preferences] y [userIngredients].
     *
     * @param userIngredients ingredientes introducidos en el Generador (vacio si no aplica);
     *   `quantity`/`unit`/`isPantry` se ignoran, solo cuenta `name`/`ingredientId`.
     */
    fun of(
        recipe: Recipe,
        preferences: PreferenceProfile,
        taxonomy: Taxonomy,
        userIngredients: List<RecipeIngredient> = emptyList(),
    ): Int {
        var weighted = 0.0
        var totalWeight = 0

        val resolved = recipe.ingredients.filter { it.ingredientId != null }

        if (userIngredients.isNotEmpty()) {
            val matched = userIngredients.count { user ->
                recipe.ingredients.any { it.matches(user) }
            }
            weighted += W_COVERAGE * (matched.toDouble() / userIngredients.size)
            totalWeight += W_COVERAGE
        }

        if (resolved.isNotEmpty()) {
            val liked = resolved.count { it.ingredientId in preferences.preferredIngredientIds }
            weighted += W_LIKED * (liked.toDouble() / resolved.size)
            totalWeight += W_LIKED
        }

        weighted += if (recipe.cuisineId in preferences.cuisineIds) W_CUISINE else 0
        totalWeight += W_CUISINE

        val maxMinutes = taxonomy.timeBucket(preferences.timeBucketId)?.maxMinutes
        if (maxMinutes != null) {
            weighted += if (recipe.totalMinutes <= maxMinutes) W_TIME else 0
            totalWeight += W_TIME
        }

        weighted += if (recipe.difficultyId == preferences.difficultyId) W_DIFFICULTY else 0
        totalWeight += W_DIFFICULTY

        val ratio = if (totalWeight == 0) 0.0 else weighted / totalWeight
        val base = Math.round(100.0 * ratio).toInt()
        val penalty = resolved.count { it.ingredientId in preferences.dislikedIngredientIds } * DISLIKE_PENALTY
        return (base - penalty).coerceIn(0, 100)
    }

    private fun RecipeIngredient.matches(other: RecipeIngredient): Boolean =
        (ingredientId != null && ingredientId == other.ingredientId) ||
            normalize(name) == normalize(other.name)

    private val DIACRITICS = Regex("\\p{Mn}+")

    private fun normalize(raw: String): String =
        Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
}
