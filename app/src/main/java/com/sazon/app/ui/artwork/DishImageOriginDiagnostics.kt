package com.sazon.app.ui.artwork

import androidx.compose.runtime.staticCompositionLocalOf
import com.sazon.app.data.image.DishImageOrigin
import com.sazon.app.domain.recipe.Recipe

/**
 * Costura de **diagnostico** del origen de la imagen de plato: [RecipeArtwork] clasifica que capa gano
 * ([RecipeArtworkImagePrecedence.originOf]) y la **notifica** aqui, sin que ninguna rama de pintado
 * dependa de ello. Es una llamada lateral con default no-op: el comportamiento visual es identico con
 * o sin diagnostico.
 *
 * [resolvedOriginFor] devuelve el origen que **registro el resolutor** para la receta
 * (`data.image.DishImageOriginSink`, alimentado por `DishImageResolver`) para poder distinguir un
 * acierto del **set semilla** de una generacion real; `null` cuando no hay dato (receta nunca
 * resuelta en esta sesion o diagnostico no provisto).
 *
 * La implementacion real vive en `MainActivity` y **solo** se provee en depuracion
 * (`BuildConfig.DEBUG`): en release queda [NoDishImageOriginDiagnostics], que no emite nada.
 */
interface DishImageOriginDiagnostics {

    /** Origen registrado por el resolutor para [recipe], o `null` si no hay dato. */
    fun resolvedOriginFor(recipe: Recipe): DishImageOrigin?

    /** Notifica el [origin] resuelto de la imagen pintada de [recipe]. */
    fun onPainted(recipe: Recipe, origin: DishImageOrigin)
}

/**
 * Default seguro: no produce ni emite nada. Cualquier host que no provea el seam (previews, tests,
 * componentes aislados) y la build de **release** siguen pintando exactamente lo de hoy.
 */
object NoDishImageOriginDiagnostics : DishImageOriginDiagnostics {

    override fun resolvedOriginFor(recipe: Recipe): DishImageOrigin? = null

    override fun onPainted(recipe: Recipe, origin: DishImageOrigin) = Unit
}

/**
 * Proveedor del diagnostico de origen para el arbol de composicion. La app real lo provee en
 * `MainActivity` **solo** con `BuildConfig.DEBUG`; en release se queda el default no-op.
 */
val LocalDishImageOriginDiagnostics = staticCompositionLocalOf<DishImageOriginDiagnostics> {
    NoDishImageOriginDiagnostics
}
