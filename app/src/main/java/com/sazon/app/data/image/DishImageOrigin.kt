package com.sazon.app.data.image

/**
 * Origen **resuelto** de la imagen de plato que se pinta, para diagnostico (solo debug): responde a
 * "de donde salio esta imagen" cuando una misma foto se repite o cuando la generacion no esta
 * disponible.
 *
 * Vocabulario cerrado de tres valores; cada uno identifica **una** etapa de la cadena:
 *
 * - [GENERATED]: bytes del camino de generacion (proveedor de imagen), o una copia de esos bytes en
 *   memoria/disco. Es el caso sano cuando el interruptor esta encendido y queda tope por sesion.
 * - [SEED]: asset del **set semilla** (`img/dishes/seed/<clave canonica>`), que viaja en el APK y no
 *   gasta cuota ni red.
 * - [GLYPH]: ultimo recurso: sin imagen, de modo que [com.sazon.app.ui.artwork.RecipeArtwork] deja
 *   visible la **ficha tipografica** del llamante (color de la superficie o degradado de la cocina +
 *   marca de agua del glifo). La capa `CATEGORY` (foto local de `recipe-photos`) desaparecio con
 *   `retire-recipe-photos` (ADR 0006): ya no hay foto local que clasificar.
 *
 * Es un valor **puro** de diagnostico: no interviene en la resolucion de bytes, ni en la cache, ni en
 * el pintado, ni en el gasto de cuota.
 */
enum class DishImageOrigin {
    GENERATED,
    SEED,
    GLYPH,
}
