package com.sazon.app.data.image

/**
 * Origen **resuelto** de la imagen de plato que se pinta, para diagnostico (solo debug): responde a
 * "de donde salio esta imagen" cuando una misma foto se repite o cuando la generacion no esta
 * disponible.
 *
 * Vocabulario cerrado de cuatro valores; cada uno identifica **una** etapa de la cadena:
 *
 * - [GENERATED]: bytes del camino de generacion (proveedor de imagen), o una copia de esos bytes en
 *   memoria/disco. Es el caso sano cuando el interruptor esta encendido y queda tope por sesion.
 * - [SEED]: asset del **set semilla** (`img/dishes/seed/<clave canonica>`), que viaja en el APK y no
 *   gasta cuota ni red.
 * - [CATEGORY]: **foto local** empaquetada de la cadena de `recipe-photos` (foto propia de la receta
 *   semilla, foto de su categoria o la foto por defecto). La etiqueta **agrupa** las tres: el
 *   vocabulario pedido tiene cuatro valores y `RecipePhotoSource` no expone cual de ellas gano; el
 *   fallback a esta capa es lo que hoy resulta indistinguible de un fallo de generacion en logcat.
 * - [GLYPH]: ultimo recurso: ni imagen generada ni semilla ni foto local, de modo que
 *   [com.sazon.app.ui.artwork.RecipeArtwork] pinta el degradado con el glifo.
 *
 * Es un valor **puro** de diagnostico: no interviene en la resolucion de bytes, ni en la cache, ni en
 * el pintado, ni en el gasto de cuota.
 */
enum class DishImageOrigin {
    GENERATED,
    SEED,
    CATEGORY,
    GLYPH,
}
