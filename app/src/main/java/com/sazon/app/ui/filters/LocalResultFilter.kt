package com.sazon.app.ui.filters

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.RecipeCategory
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Seleccion **unica** de los filtros locales de resultados (definicion de "Filtros de resultados" de
 * `CONTEXT.md`): categoria por `categoryId` y dos filtros **derivados**, "Rapido" (`totalMinutes <= 30`)
 * y "Vegetariano" (sin ingredientes con tags animales).
 *
 * Logica pura: no importa Compose ni Android; la fila de chips ([ResultFilterRow]) y el `Saver` de
 * `rememberSaveable` viven en la capa Compose. La seleccion sobrevive a cambios de configuracion via
 * [key] / [LocalResultFilters.fromKey].
 */
sealed interface LocalResultFilter {

    /** Clave estable y serializable de la seleccion (para `rememberSaveable`). */
    val key: String

    /** "Todas": no filtra. */
    data object All : LocalResultFilter {
        override val key: String = "all"
    }

    /** Filtro por categoria de receta (`categoryId`). */
    data class Category(val categoryId: String) : LocalResultFilter {
        override val key: String = "$CATEGORY_PREFIX$categoryId"
    }

    /** Filtro derivado "Rapido": `totalMinutes <= [LocalResultFilters.FAST_MAX_MINUTES]`. */
    data object Fast : LocalResultFilter {
        override val key: String = "fast"
    }

    /** Filtro derivado "Vegetariano": sin ingredientes con tags animales. */
    data object Vegetarian : LocalResultFilter {
        override val key: String = "vegetarian"
    }

    companion object {
        const val CATEGORY_PREFIX = "category:"
    }
}

/**
 * Operaciones puras (sin Compose/Android) del filtrado local sobre recetas **ya cargadas en memoria**:
 * predicados, etiquetas, categorias presentes y serializacion de la seleccion. Testeable en JVM.
 *
 * "Rapido" y "Vegetariano" son **derivados**, no categorias: "Vegetariano" no es
 * `categoryId == "vegetarian"` (que existe como categoria de receta), se calcula por los tags de los
 * ingredientes resueltos en la Taxonomia. La deteccion es por **tags, no por keywords**: un ingrediente
 * libre (`ingredientId == null`) no se excluye (limitacion aceptada de `CONTEXT.md`/`recipe-validator`).
 */
object LocalResultFilters {

    /** Frontera de "Rapido" (minutos): `totalMinutes <= FAST_MAX_MINUTES`. */
    const val FAST_MAX_MINUTES = 30

    /** Respaldo defensivo si la Taxonomia no declara el patron vegetariano (no ocurre hoy). */
    val FALLBACK_VEGETARIAN_TAGS = setOf("meat", "poultry", "fish", "seafood")

    /**
     * Aplica [filter] a las [recipes], preservando el orden de entrada. `All` devuelve la lista tal
     * cual; `Category` compara `categoryId`; `Fast` compara `totalMinutes <=` [FAST_MAX_MINUTES]; y
     * `Vegetarian` excluye recetas con algun ingrediente resuelto cuyos tags esten en
     * [vegetarianTags].
     */
    fun visible(
        recipes: List<Recipe>,
        filter: LocalResultFilter,
        taxonomy: Taxonomy,
    ): List<Recipe> = when (filter) {
        LocalResultFilter.All -> recipes

        is LocalResultFilter.Category -> recipes.filter { it.categoryId == filter.categoryId }

        LocalResultFilter.Fast -> recipes.filter { it.totalMinutes <= FAST_MAX_MINUTES }

        LocalResultFilter.Vegetarian -> {
            val animalTags = vegetarianTags(taxonomy)
            recipes.filter { recipe ->
                recipe.ingredients.none { ingredient ->
                    ingredient.ingredientId
                        ?.let { taxonomy.ingredient(it) }
                        ?.tags
                        ?.any { it in animalTags } == true
                }
            }
        }
    }

    /**
     * Tags animales que excluye "Vegetariano", derivados de la fuente unica de verdad
     * `dietPatterns[vegetarian].excludedTags` (`meat`/`poultry`/`fish`/`seafood`), con respaldo
     * constante defensivo.
     */
    fun vegetarianTags(taxonomy: Taxonomy): Set<String> =
        taxonomy.dietPattern("vegetarian")?.excludedTags?.toSet() ?: FALLBACK_VEGETARIAN_TAGS

    /**
     * Categorias **presentes** en [recipes], sin duplicados y en el orden de `taxonomy.recipeCategories`.
     * Los `categoryId` desconocidos se ignoran (no tienen chip).
     */
    fun categoriesIn(recipes: List<Recipe>, taxonomy: Taxonomy): List<RecipeCategory> {
        val present = recipes.map { it.categoryId }.toSet()
        return taxonomy.recipeCategories.filter { it.id in present }
    }

    /** Etiqueta del chip: "Todas", label de la categoria (respaldo al id), "Rapido" o "Vegetariano". */
    fun label(taxonomy: Taxonomy, filter: LocalResultFilter): String = when (filter) {
        LocalResultFilter.All -> "Todas"
        is LocalResultFilter.Category -> taxonomy.recipeCategory(filter.categoryId)?.label
            ?: filter.categoryId
        LocalResultFilter.Fast -> "Rápido"
        LocalResultFilter.Vegetarian -> "Vegetariano"
    }

    /** Verdadero para los filtros derivados ("Rapido" y "Vegetariano"), que se tratan en oliva. */
    fun isDerived(filter: LocalResultFilter): Boolean =
        filter == LocalResultFilter.Fast || filter == LocalResultFilter.Vegetarian

    /**
     * Inversa de [LocalResultFilter.key]. Una clave desconocida degrada a [LocalResultFilter.All], de
     * modo que un estado restaurado corrupto nunca rompe la Home.
     */
    fun fromKey(key: String): LocalResultFilter = when {
        key == LocalResultFilter.All.key -> LocalResultFilter.All
        key == LocalResultFilter.Fast.key -> LocalResultFilter.Fast
        key == LocalResultFilter.Vegetarian.key -> LocalResultFilter.Vegetarian
        key.startsWith(LocalResultFilter.CATEGORY_PREFIX) -> key
            .removePrefix(LocalResultFilter.CATEGORY_PREFIX)
            .takeIf { it.isNotEmpty() }
            ?.let(LocalResultFilter::Category)
            ?: LocalResultFilter.All
        else -> LocalResultFilter.All
    }
}
