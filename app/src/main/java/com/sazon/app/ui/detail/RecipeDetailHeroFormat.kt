package com.sazon.app.ui.detail

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.ui.home.MainIngredient
import com.sazon.app.ui.home.RecipeCardFormat

/**
 * Contenido y geometria del **hero tipografico** del Detalle (ADR 0006, direccion C3): logica
 * **pura**, sin Compose ni Android, testeable en JVM.
 *
 * El hero **no** pinta ninguna foto: publica su **geometria** como constantes ([HERO_HEIGHT_DP],
 * [FILLET_HEIGHT_DP], [WATERMARK_DP], [WATERMARK_ALPHA]) y su **contenido** en [HeroContent]. El
 * contenido **reutiliza** lo ya aceptado de la ficha tipografica ([RecipeCardFormat.categoryLine] y
 * [RecipeCardFormat.mainIngredients]: despensa fuera, orden de declaracion, tope de 3, respaldo al
 * `id` cuando la Taxonomia no trae la etiqueta), de modo que la app tiene **una sola** regla de
 * "categoria · cocina" y de "ingredientes principales".
 *
 * La capa Compose ([RecipeDetailScreen]) convierte las constantes `_DP` a `dp`; este fichero no
 * importa `androidx.compose.*`.
 */
object RecipeDetailHeroFormat {

    /** Alto del hero (`docs/design/concepts/recipe-detail-typographic-header/detalle-receta`). */
    const val HERO_HEIGHT_DP = 240

    /** Grosor del filete de la cocina en el borde inferior (`DESIGN.md`). */
    const val FILLET_HEIGHT_DP = 6

    /** Lado de la marca de agua del glifo de la categoria en el hero. */
    const val WATERMARK_DP = 190

    /** Opacidad de la marca de agua (`DESIGN.md`: al 12 %, como textura). */
    const val WATERMARK_ALPHA = 0.12f

    /** Ingredientes principales que pinta el hero; la misma regla que la ficha ([RecipeCardFormat]). */
    const val MAIN_INGREDIENTS = RecipeCardFormat.MAIN_INGREDIENTS

    /**
     * Contenido del hero: linea de categoria (`"Arroz · Latina"`), titulo y hasta
     * [MAIN_INGREDIENTS] ingredientes con su cantidad ya formateada.
     */
    data class HeroContent(
        val category: String,
        val title: String,
        val ingredients: List<MainIngredient>,
    )

    /**
     * Contenido del hero de [recipe] contra la [taxonomy] real, sin `Random` ni reloj: dos llamadas
     * iguales devuelven el mismo [HeroContent]. Una receta sin datos no rompe: la lista de
     * ingredientes queda vacia y la linea de categoria cae al propio `id`.
     */
    fun of(taxonomy: Taxonomy, recipe: Recipe): HeroContent = HeroContent(
        category = RecipeCardFormat.categoryLine(taxonomy, recipe),
        title = recipe.title,
        ingredients = RecipeCardFormat.mainIngredients(recipe),
    )
}
