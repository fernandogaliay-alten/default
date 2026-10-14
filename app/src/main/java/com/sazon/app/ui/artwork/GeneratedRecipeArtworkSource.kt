package com.sazon.app.ui.artwork

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.sazon.app.domain.recipe.Recipe

/**
 * Fuente de la **imagen de plato generada** de una receta, consumida por [RecipeArtwork] como su
 * unica capa de fondo: desde `retire-recipe-photos` (ADR 0006) ya **no** existe el seam de las fotos
 * locales. Devuelve `null` cuando no hay imagen generada disponible (sin red, sin clave, fallo,
 * timeout o codec roto); en ese caso [RecipeArtwork] deja visible la **ficha tipografica** del
 * llamante (degradacion silenciosa: nunca un error de UI ni un hueco).
 *
 * La **cancelacion** de la corrutina que resuelve la imagen se propaga: salir de composicion o
 * reciclar la tarjeta al hacer scroll cancela la peticion.
 */
fun interface GeneratedRecipeArtworkSource {
    /** Imagen generada del plato, o `null` (degrada a la ficha tipografica). */
    suspend fun generatedFor(recipe: Recipe): ImageBitmap?
}

/**
 * Default: **no produce** imagen generada (`null`). Cualquier host que no provea el seam (previews,
 * tests, componentes aislados) deja visible exactamente la **ficha tipografica**, que es la razon de
 * elegir un `CompositionLocal` con default seguro.
 */
object NoGeneratedRecipeArtworkSource : GeneratedRecipeArtworkSource {
    override suspend fun generatedFor(recipe: Recipe): ImageBitmap? = null
}

/**
 * Proveedor de la imagen generada para el arbol de composicion. La app real lo provee en
 * `MainActivity` con un `DishImageArtworkLoader` que resuelve via `data.image.DishImageResolver`
 * (memoria -> disco -> semilla -> generador, fases 3 y 7) y decodifica a `ImageBitmap`.
 */
val LocalGeneratedRecipeArtworkSource = staticCompositionLocalOf<GeneratedRecipeArtworkSource> {
    NoGeneratedRecipeArtworkSource
}
