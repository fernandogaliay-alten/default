package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Constructor del prompt de imagen de un plato: una sola plantilla, siempre la misma, traducida de la
 * [Recipe] y la Taxonomia (patron de `PromptBuilder`, `docs/ai-dish-images-proposal.md` §4.2).
 *
 * Plantilla v2 (identidad **siempre**, despues los detalles; los segmentos vacios se omiten):
 *
 * ```
 * <PROMPT_PREFIX> <identidad>[, <cocina>][, <detalle>][, <ingredientes clave>], <COMPOSITION>, <STYLE_SUFFIX>, <NEGATIVES>
 * ```
 *
 * - `<identidad>` = **titulo** de la receta con espacios colapsados; si queda vacio, la etiqueta real
 *   de su `categoryId` en la Taxonomia; si tampoco hay, [DEFAULT_DISH]. Es **siempre** la identidad del
 *   plato: el `imagePrompt` del descriptor ya **no** la sustituye, es el detalle.
 * - `<cocina>` = etiqueta de la cocina de la Taxonomia **solo** si existe y no esta en blanco; en
 *   cualquier otro caso la clausula se **omite** (nunca un marcador de "desconocida").
 * - `<detalle>` = [DishVisualDescriptor.imagePrompt] (fase 5) colapsado: la descripcion visual del
 *   plato terminado. Si no hay, no aporta segmento.
 * - `<ingredientes clave>` = hasta [MAX_KEY_INGREDIENTS] nombres de ingredientes **no despensa**
 *   (`!isPantry`), distintos y **en el orden de la receta**, unidos por `", "`. Se omite si no hay
 *   ninguno.
 * - [COMPOSITION], [STYLE_SUFFIX] y [NEGATIVES] cierran siempre la plantilla.
 *
 * El ensamblado une los segmentos **no vacios** con `", "` y colapsa las rachas de comas para
 * garantizar que no aparece `,,` ni un espacio doble.
 *
 * El titulo conserva mayusculas y diacriticos: el prompt es texto para un modelo de imagen (la forma
 * natural ayuda) y la forma normalizada sin acentos es exclusiva de [DishImageCacheKey].
 *
 * Logica **pura**: sin Android, sin red, sin estado, sin `Random` ni reloj. Misma entrada -> misma
 * cadena; el resultado **nunca** esta en blanco y contiene [STYLE_SUFFIX] exactamente una vez. No
 * lanza por datos de entrada (titulo/categoria/cocina vacios, ingredientes vacios, `id` desconocidos o
 * Taxonomia vacia).
 */
object DishImagePrompt {

    /** Arranque fijo del prompt. */
    const val PROMPT_PREFIX = "Fotografía gastronómica de"

    /** Plato de ultimo recurso cuando la receta no tiene ni titulo ni categoria real. */
    const val DEFAULT_DISH = "plato casero"

    /**
     * Clausula de composicion: el encuadre del plato terminado. Deliberadamente **no** lleva negativos
     * (esos viven en [NEGATIVES]) para que cada pieza del vocabulario visual tenga un unico lugar.
     */
    const val COMPOSITION = "una ración completa del plato en primer plano, sobre fondo neutro"

    /** Coletilla de estilo fija de la casa (un unico lugar, pura y testeada). */
    const val STYLE_SUFFIX = "cerámica artesanal, luz natural suave, estilo editorial gastronómico"

    /**
     * Negativos reforzados, al final del prompt: lo que el modelo **no** debe pintar (en v1 la parte
     * "sin texto ni manos" viajaba dentro de [STYLE_SUFFIX]).
     */
    const val NEGATIVES =
        "sin texto, sin manos, sin personas, sin cubiertos ni marcas de agua, " +
            "sin collage ni varios platos, sin fondo recargado"

    /**
     * Tope de ingredientes clave que se anaden al prompt. Acotado a proposito: la despensa (sal,
     * aceite, pimienta, agua) no aporta identidad y una lista larga alargaria el prompt de imagen.
     */
    const val MAX_KEY_INGREDIENTS = 5

    private val WHITESPACE = Regex("\\s+")

    /** Colapsa cualquier racha de comas (con sus espacios) para no dejar `,,` en la plantilla. */
    private val COMMA_RUNS = Regex("(\\s*,\\s*)+")

    /** Prompt de imagen de [recipe]; determinista y nunca en blanco. */
    fun build(taxonomy: Taxonomy, recipe: Recipe, descriptor: DishVisualDescriptor? = null): String {
        val identity = identityClause(taxonomy, recipe)
        val cuisine = taxonomy.cuisine(recipe.cuisineId)
            ?.label
            ?.let(::singleLine)
            ?.takeIf { it.isNotEmpty() }

        val segments = listOfNotNull(
            if (cuisine == null) identity else "$identity, $cuisine",
            descriptor?.imagePrompt?.let(::singleLine)?.takeIf { it.isNotEmpty() },
            keyIngredients(recipe),
            COMPOSITION,
            STYLE_SUFFIX,
            NEGATIVES,
        )

        val body = COMMA_RUNS.replace(segments.joinToString(", "), ", ").trim()
        return "$PROMPT_PREFIX $body"
    }

    /**
     * Identidad del plato (v2): **titulo** de la receta; si queda vacio, la etiqueta real de su
     * categoria; si tampoco, [DEFAULT_DISH]. El descriptor **no** participa: su `imagePrompt` es el
     * detalle visual, no la identidad.
     */
    private fun identityClause(taxonomy: Taxonomy, recipe: Recipe): String {
        singleLine(recipe.title).takeIf { it.isNotEmpty() }?.let { return it }
        val category = taxonomy.recipeCategory(recipe.categoryId)?.label?.let(::singleLine)
        if (!category.isNullOrEmpty()) return category
        return DEFAULT_DISH
    }

    /**
     * Ingredientes clave: hasta [MAX_KEY_INGREDIENTS] nombres **distintos** de ingredientes no
     * despensa, **en el orden de la receta**, unidos por `", "`. `null` si no hay ninguno (la clausula
     * se omite).
     */
    private fun keyIngredients(recipe: Recipe): String? {
        val names = recipe.ingredients
            .asSequence()
            .filterNot { it.isPantry }
            .map { singleLine(it.name) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_KEY_INGREDIENTS)
            .toList()
        return names.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    /** Colapsa cualquier espacio en blanco para no ensuciar la plantilla con espacios dobles. */
    private fun singleLine(value: String): String = WHITESPACE.replace(value, " ").trim()
}
