package com.sazon.app.ui.artwork

import com.sazon.app.domain.recipe.Recipe

/**
 * Glifo de línea que identifica la **categoría** de una receta (`categoryId` de la Taxonomía).
 *
 * Hay **uno por cada una de las 18 `recipeCategories` reales**; [PLATE] es a la vez el glifo de la
 * categoría `other` y el **fallback** para un `categoryId` nulo, vacío o desconocido (dos círculos
 * concéntricos, el placeholder aceptado antes de esta feature).
 *
 * Este enum no importa Compose ni Android: la categoría se decide aquí (JVM) y se pinta en
 * `RecipeArtwork`.
 */
enum class RecipeGlyph {
    PASTA,
    RICE,
    SOUP_CREAM,
    SALAD,
    MEAT,
    POULTRY,
    FISH_SEAFOOD,
    VEGETARIAN,
    VEGAN,
    LEGUME_STEW,
    PIZZA_BREAD,
    EGG_BREAKFAST,
    SANDWICH_WRAP,
    TAPAS_APPETIZER,
    DESSERT,
    SMOOTHIE_DRINK,
    CASSEROLE_OVEN,
    PLATE,
}

/**
 * Acento del degradado del bloque, elegido por la **cocina** de la receta (`cuisineId` de la
 * Taxonomía): **uno por cada una de las 10 `cuisines` reales** más [DEFAULT].
 *
 * El acento solo nombra el **par de roles del tema** (`start` arriba → `end` abajo); la resolución
 * rol → `Color` vive en la capa Compose de `RecipeArtwork`, de modo que este fichero no importa
 * Compose y el modo claro/oscuro sale gratis. [DEFAULT] reproduce exactamente el degradado
 * `primaryContainer` → `secondaryContainer` que la app usaba antes.
 */
enum class RecipeAccent {
    EMBER,
    GROVE,
    TOMATO,
    GINGER,
    SESAME,
    CURRY,
    CHILI,
    HONEY,
    CLAY,
    ROSE,
    DEFAULT,
}

/** Estilo visual de la imagen de una receta: [glyph] por categoría y [accent] por cocina. */
data class RecipeStyle(
    val glyph: RecipeGlyph,
    val accent: RecipeAccent,
)

/**
 * Mapeo **puro**, **exhaustivo** y **determinista** de los ids de la Taxonomía al estilo del
 * placeholder de receta: glifo por `categoryId` (18 categorías reales) y acento por `cuisineId`
 * (10 cocinas reales), con **fallback total** (`null`/vacío/desconocido → [RecipeGlyph.PLATE] +
 * [RecipeAccent.DEFAULT]).
 *
 * Sin Compose, sin Android, sin red, sin reloj y sin `Random`: dos llamadas iguales devuelven el
 * mismo [RecipeStyle]. El test `RecipeArtworkStyleTest` itera la Taxonomía real y comprueba que
 * cada id tiene estilo propio y que no hay duplicados.
 */
object RecipeArtworkStyle {

    /** Glifo de la categoría; `null`, vacío o desconocido caen al plato ([RecipeGlyph.PLATE]). */
    fun glyphFor(categoryId: String?): RecipeGlyph = when (categoryId) {
        "pasta" -> RecipeGlyph.PASTA
        "rice" -> RecipeGlyph.RICE
        "soup_cream" -> RecipeGlyph.SOUP_CREAM
        "salad" -> RecipeGlyph.SALAD
        "meat" -> RecipeGlyph.MEAT
        "poultry" -> RecipeGlyph.POULTRY
        "fish_seafood" -> RecipeGlyph.FISH_SEAFOOD
        "vegetarian" -> RecipeGlyph.VEGETARIAN
        "vegan" -> RecipeGlyph.VEGAN
        "legume_stew" -> RecipeGlyph.LEGUME_STEW
        "pizza_bread" -> RecipeGlyph.PIZZA_BREAD
        "egg_breakfast" -> RecipeGlyph.EGG_BREAKFAST
        "sandwich_wrap" -> RecipeGlyph.SANDWICH_WRAP
        "tapas_appetizer" -> RecipeGlyph.TAPAS_APPETIZER
        "dessert" -> RecipeGlyph.DESSERT
        "smoothie_drink" -> RecipeGlyph.SMOOTHIE_DRINK
        "casserole_oven" -> RecipeGlyph.CASSEROLE_OVEN
        "other" -> RecipeGlyph.PLATE
        else -> RecipeGlyph.PLATE
    }

    /** Acento de la cocina; `null`, vacío o desconocido caen al degradado por defecto. */
    fun accentFor(cuisineId: String?): RecipeAccent = when (cuisineId) {
        "spanish" -> RecipeAccent.EMBER
        "mediterranean" -> RecipeAccent.GROVE
        "italian" -> RecipeAccent.TOMATO
        "asian" -> RecipeAccent.GINGER
        "japanese" -> RecipeAccent.SESAME
        "indian" -> RecipeAccent.CURRY
        "mexican" -> RecipeAccent.CHILI
        "american" -> RecipeAccent.HONEY
        "middle_eastern" -> RecipeAccent.CLAY
        "latin" -> RecipeAccent.ROSE
        else -> RecipeAccent.DEFAULT
    }

    /** Estilo a partir de los ids sueltos (los mismos `null` de una receta sin clasificar). */
    fun of(categoryId: String?, cuisineId: String?): RecipeStyle = RecipeStyle(
        glyph = glyphFor(categoryId),
        accent = accentFor(cuisineId),
    )

    /** Estilo de una [Recipe]; delega en [of] con su `categoryId` y su `cuisineId`. */
    fun of(recipe: Recipe): RecipeStyle = of(recipe.categoryId, recipe.cuisineId)
}
