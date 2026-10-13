package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.taxonomy.Taxonomy

/**
 * Peticion de imagen de un plato, ya resuelta: el adaptador (fase 2) es transporte puro y **no**
 * conoce ni la [Recipe] ni la [Taxonomy].
 *
 * [prompt] y [cacheKey] son el punto de entrada estable del puerto: la variante B (fase 5) cambiara
 * **quien** los rellena, no la firma. [recipeId] es solo trazabilidad (nunca la identidad de cache).
 */
data class DishImageRequest(
    /** Clave canonica de cache (`DishImageCacheKey`), apta como nombre de fichero. */
    val cacheKey: String,
    /** Prompt de imagen ya compuesto (`DishImagePrompt`). */
    val prompt: String,
    /** `id` de la receta de origen, o `null` si no tiene; no interviene en la clave. */
    val recipeId: String? = null,
) {
    companion object {

        /**
         * **Unico punto de composicion** de la peticion: prompt y clave se derivan aqui de la
         * Taxonomia y la receta, de modo que ningun llamante redecide la politica.
         */
        fun of(taxonomy: Taxonomy, recipe: Recipe): DishImageRequest = DishImageRequest(
            cacheKey = DishImageCacheKey.of(recipe),
            prompt = DishImagePrompt.build(taxonomy, recipe),
            recipeId = recipe.id.takeIf { it.isNotBlank() },
        )
    }
}

/**
 * Puerto de dominio del generador de imagenes de plato.
 *
 * El dominio depende de esta abstraccion, no de un SDK: la implementacion real (DevExpert Inference,
 * Ktor y decodificacion de `b64_json`) llega en la fase 2 (`dish-image-generator`) y hasta entonces
 * **nadie consume el puerto**.
 *
 * Contrato de fallo: `null` = imagen no disponible, **no** excepcion (misma disciplina que
 * `data.RecipePhotoAssets.resolve` y `RecipeGenerator`): la app conserva la cadena local de
 * `recipe-photos` y nunca muestra un error por imagen. La **cancelacion** de la corrutina si se
 * propaga, porque la fase 4 cancela al salir de pantalla o al hacer scroll.
 *
 * El **formato** de los bytes (WebP reducido y su extension) lo deciden el adaptador de la fase 2 y la
 * cache de la fase 3: el dominio no modela codecs.
 */
interface DishImageGenerator {

    /** Imagen ya codificada lista para cachear; `null` = no disponible (degradacion silenciosa). */
    suspend fun generate(request: DishImageRequest): ByteArray?
}
