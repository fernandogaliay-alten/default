package com.sazon.app.ui.search

import com.sazon.app.domain.generation.GenerationResult
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.validation.DiscardReason
import com.sazon.app.ui.results.ResultsHeader
import com.sazon.app.ui.results.ResultsHeaders
import com.sazon.app.ui.results.ResultsOrigin
import com.sazon.app.ui.results.ResultsStatus

/**
 * Estado **puro** de la pagina de Busqueda con IA. Logica sin Compose ni Android, testeable en JVM.
 *
 * La regla de negocio clave: el **bloqueo por Perfil** se distingue de "sin resultados" por el **motivo
 * del descarte** que da el `RecipeValidator`, no adivinando por la consulta.
 */
sealed interface AiSearchStage {

    /** Hay recetas aceptadas: se reutiliza la pantalla de Resultados con el subtitulo de busqueda. */
    data class Results(val status: ResultsStatus, val header: ResultsHeader) : AiSearchStage

    /** `accepted` vacio con descartes por dieta o restriccion dura: la consulta choca con el Perfil. */
    data class Blocked(val query: String) : AiSearchStage

    /** `accepted` vacio sin descartes por dieta/restriccion dura: simplemente no hay recetas. */
    data class NoResults(val query: String) : AiSearchStage

    /** El generador fallo (`aiUnavailable`): se conserva la consulta y se ofrece reintentar. */
    data class Failure(val query: String) : AiSearchStage
}

object AiSearchOutcome {

    /**
     * Decide el estado a partir del resultado de la busqueda, la consulta escrita, el Perfil y la
     * Taxonomia:
     *
     * 1. `aiUnavailable` -> [AiSearchStage.Failure];
     * 2. `accepted` no vacio -> [AiSearchStage.Results] (puntuado y ordenado por relevancia; sin
     *    ingredientes de usuario ni semillas, la busqueda fallida nunca cae a semilla);
     * 3. algun descarte con `DIET_PATTERN` o `HARD_RESTRICTION` -> [AiSearchStage.Blocked];
     * 4. en otro caso -> [AiSearchStage.NoResults].
     */
    fun stageOf(
        result: GenerationResult,
        query: String,
        preferences: PreferenceProfile,
        taxonomy: Taxonomy,
    ): AiSearchStage = when {
        result.aiUnavailable -> AiSearchStage.Failure(query)

        result.accepted.isNotEmpty() -> AiSearchStage.Results(
            status = ResultsStatus.from(
                result = result,
                preferences = preferences,
                taxonomy = taxonomy,
                userIngredients = emptyList(),
                seedRecipes = emptyList(),
            ),
            header = ResultsHeaders.of(origin = ResultsOrigin.Search(query), taxonomy = taxonomy),
        )

        result.discarded.any {
            it.reason == DiscardReason.DIET_PATTERN || it.reason == DiscardReason.HARD_RESTRICTION
        } -> AiSearchStage.Blocked(query)

        else -> AiSearchStage.NoResults(query)
    }
}
