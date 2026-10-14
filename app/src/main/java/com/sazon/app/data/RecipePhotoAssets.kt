package com.sazon.app.data

/**
 * Constantes **compartidas** de los assets de imagen de plato *(antes: cadena de respaldo de las
 * fotos locales de `recipe-photos`, retirada por ADR 0006 con `retire-recipe-photos`)*.
 *
 * Ya **no** es la cadena de fotos de la receta: la capa base sin imagen es la **ficha tipografica**
 * (ADR 0006) y la unica imagen de una receta es la **generada por IA** como capa de fondo. Lo que
 * queda aqui son las dos piezas que las cadenas de assets **vivas** siguen reutilizando:
 *
 * 1. El **ancla de estilo** de la casa: la foto neutra y versionada `img/recipes/default.<ext>` que
 *    `DishImageStyleReferencePolicy` (fase 6, ADR 0005) lee para el multipart de `/images/edits`. No
 *    se muestra en ninguna pantalla; es solo la referencia de estilo que viaja en el APK.
 * 2. El **orden de extensiones** que se prueban por candidato (`webp` -> `jpg` -> `png`), compartido
 *    por esa referencia y por el **set semilla** `DishImageSeedAssets` (fase 7), para no fijar el
 *    formato en dos sitios.
 *
 * Logica **pura**: sin Android, sin Compose y **sin I/O**. Determinista y sin estado.
 */
object RecipePhotoAssets {

    /** Raiz de los assets bajo `assets/` que contiene el ancla de estilo (`img/recipes/default.<ext>`). */
    const val ROOT = "img/recipes"

    /** Clave del asset generico/neutro: hoy, el ancla de estilo de la casa. */
    const val DEFAULT_KEY = "default"

    /**
     * Extensiones que se prueban **en orden** para cada candidato. No se asume formato (el ancla
     * esta versionada como `default.jpg`) y el orden se comparte con [DishImageSeedAssets], de modo
     * que la app funciona con `webp`, `jpg` o `png` sin cambios.
     */
    val EXTENSIONS = listOf("webp", "jpg", "png")
}
