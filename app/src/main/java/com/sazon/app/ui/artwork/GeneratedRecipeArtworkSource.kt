package com.sazon.app.ui.artwork

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.sazon.app.domain.recipe.Recipe

/**
 * Fuente de la **imagen de plato generada** de una receta, consumida por [RecipeArtwork] como un
 * **nivel por delante** de la cadena local ([RecipePhotoSource]).
 *
 * Es un seam **paralelo** al de la foto local, no una generalizacion de este: [RecipePhotoSource]
 * mantiene su contrato (foto instantanea y respaldo) y los 5 puntos de uso siguen sin conocer ni el
 * asset ni Android. Devuelve `null` cuando no hay imagen generada disponible (sin red, sin clave,
 * fallo, timeout o codec roto); en ese caso [RecipeArtwork] conserva la foto local o, si tampoco la
 * hay, el degradado + glifo de siempre (nunca un error de UI ni un hueco).
 *
 * La **cancelacion** de la corrutina que resuelve la imagen se propaga: salir de composicion o
 * reciclar la tarjeta al hacer scroll cancela la peticion.
 */
fun interface GeneratedRecipeArtworkSource {
    /** Imagen generada del plato, o `null` (degrada a la cadena local). */
    suspend fun generatedFor(recipe: Recipe): ImageBitmap?
}

/**
 * Default: **no produce** imagen generada (`null`). Cualquier host que no provea el seam (previews,
 * tests, componentes aislados) sigue pintando exactamente lo de hoy (foto local o degradado + glifo),
 * que es la razon de elegir un `CompositionLocal` con default seguro.
 */
object NoGeneratedRecipeArtworkSource : GeneratedRecipeArtworkSource {
    override suspend fun generatedFor(recipe: Recipe): ImageBitmap? = null
}

/**
 * Proveedor de la imagen generada para el arbol de composicion. La app real lo provee en
 * `MainActivity` con un `DishImageArtworkLoader` que resuelve via `data.image.DishImageResolver`
 * (memoria -> disco -> generador, fase 3) y decodifica a `ImageBitmap`.
 */
val LocalGeneratedRecipeArtworkSource = staticCompositionLocalOf<GeneratedRecipeArtworkSource> {
    NoGeneratedRecipeArtworkSource
}
