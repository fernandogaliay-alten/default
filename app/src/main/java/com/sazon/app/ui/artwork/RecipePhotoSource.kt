package com.sazon.app.ui.artwork

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.sazon.app.domain.recipe.Recipe

/**
 * Fuente de la **foto de plato** de una receta, consumida por [RecipeArtwork]. Devuelve `null` cuando
 * no hay ninguna foto disponible; en ese caso [RecipeArtwork] pinta el degradado + glifo de siempre
 * (el **ultimo recurso**, nunca un bloque vacio).
 */
fun interface RecipePhotoSource {
    suspend fun photoFor(recipe: Recipe): ImageBitmap?
}

/**
 * Proveedor de la foto de plato para el arbol de composicion.
 *
 * El valor por defecto **no** produce foto (`null`): cualquier host que no provea la fuente (previews,
 * tests, componentes aislados) sigue pintando el degradado + glifo de hoy. Esa degradacion segura es
 * la razon de elegir un `CompositionLocal` en vez de `LocalContext` (que obligaria a `RecipeArtwork` a
 * conocer Android) o de anadir un parametro a los 5 puntos de llamada y a sus 6 contenedores.
 *
 * La app real lo provee en `MainActivity` con `AssetRecipePhotoSource` (assets offline).
 */
val LocalRecipePhotoSource = staticCompositionLocalOf<RecipePhotoSource> {
    RecipePhotoSource { null }
}
