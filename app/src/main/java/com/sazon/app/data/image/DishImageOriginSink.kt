package com.sazon.app.data.image

import java.util.concurrent.ConcurrentHashMap

/**
 * Almacen en memoria, por sesion, del **origen resuelto** de la imagen de cada receta: mapea
 * `recipeId` -> [DishImageOrigin] para que el diagnostico de `ui.artwork` pueda cruzar "de donde
 * salieron los bytes" (`DishImageResolver`) con "que capa gano al pintar" (`RecipeArtwork`), que son
 * los dos unicos puntos que lo saben.
 *
 * Vive en `data.image`, junto al estado de sesion ya existente ([DishVisualDescriptorStore],
 * [DishImageSettings], [MemoryDishImageCache]): `domain.image` esta documentado como **puro y sin
 * estado**. Solo usa el JDK (**sin Android, sin Compose, sin red, sin reloj**), no persiste nada y
 * **nunca** escribe en disco: es **efimero por sesion** y no toca ninguna base ni `Recipe`.
 *
 * Es tolerante con entradas basura del borde: un `recipeId` `null`/en blanco se **ignora** (no crea
 * entrada) y una consulta con `null`/en blanco/desconocido devuelve `null`. Con el mismo `id`
 * registrado dos veces **gana el ultimo**.
 */
class DishImageOriginSink {

    private val origins = ConcurrentHashMap<String, DishImageOrigin>()

    /**
     * Registra el [origin] resuelto para [recipeId]. Un `recipeId` `null` o en blanco se **ignora**;
     * con el mismo id dos veces gana el ultimo.
     */
    fun record(recipeId: String?, origin: DishImageOrigin) {
        if (recipeId.isNullOrBlank()) return
        origins[recipeId] = origin
    }

    /**
     * Ultimo origen registrado para [recipeId], o `null` si el id es `null`/en blanco o no tiene
     * entrada (receta nunca resuelta en esta sesion).
     */
    fun originFor(recipeId: String?): DishImageOrigin? =
        recipeId?.takeIf { it.isNotBlank() }?.let { origins[it] }
}
