package com.sazon.app.domain.generator

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient

/**
 * Puerto de dominio del Generador de recetas (la IA).
 *
 * El dominio depende de esta abstraccion, no de un SDK concreto: la implementacion real es lo ultimo
 * que se conecta y hasta entonces el flujo completo se construye sobre `FakeRecipeGenerator`.
 *
 * Las tres operaciones son `suspend` porque la implementacion real hara E/S. Devuelven `List<Recipe>`
 * directamente: el modelo de errores/reintentos de la IA (`ai_unavailable`) sigue abierto y se
 * resolvera al integrar el proveedor. Las recetas devueltas **no** estan validadas; pasarlas por el
 * Validador es responsabilidad del llamante.
 */
interface RecipeGenerator {

    /** Recetas de sugerencia a partir del Perfil, sin ingredientes declarados; alimenta el Lote. */
    suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe>

    /** Recetas para la Busqueda con IA escrita en la Home. */
    suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe>

    /** Recetas para el flujo principal a partir de ingredientes y ajustes de generacion. */
    suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe>
}
