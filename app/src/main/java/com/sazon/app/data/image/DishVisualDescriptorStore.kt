package com.sazon.app.data.image

import com.sazon.app.domain.image.DishVisualDescriptor
import java.util.concurrent.ConcurrentHashMap

/**
 * Almacen en memoria, por sesion, del descriptor visual del plato que el modelo emite en la misma
 * llamada que la receta (fase 5): mapea `recipeId` -> [DishVisualDescriptor] para que el punto de
 * composicion ([com.sazon.app.domain.image.DishImageRequest.of]) pueda consultarlo al pedir la imagen.
 *
 * Vive en `data.image`, junto al estado de sesion ya existente ([DishImageSettings],
 * [MemoryDishImageCache]); `domain.image` esta documentado como **puro y sin estado** y debe seguir
 * siendolo. El almacen solo usa el JDK (**sin Android, sin Compose, sin red, sin reloj**) y no
 * persiste nada: al recrear la `Activity` arranca vacio y una receta restaurada se compone por titulo
 * (fase 1) hasta la siguiente llamada de IA. Es **efimero por sesion**, de modo que no cambia ni
 * `SuggestionBatchDatabase`, ni `FavoritesDatabase`, ni `Recipe`.
 *
 * La clave es el `id` **generado** de la receta (el mismo que usan Favoritos/`isSaved`), asi que la
 * consulta es exacta (`descriptorFor(recipe.id)`); una operacion que no registre descriptores (fallo,
 * degradacion o cancelacion) deja el mapa como estaba y la app degrada a la fase 1.
 */
class DishVisualDescriptorStore {

    private val descriptors = ConcurrentHashMap<String, DishVisualDescriptor>()

    /**
     * Registra los [descriptors] de un decode por `id` de receta aceptada. Entradas inutiles se
     * **ignoran**: una clave en blanco (nunca deberia llegar, pero el wire es dato no confiable) y un
     * descriptor vacio ([DishVisualDescriptor.isEmpty], incluido [DishVisualDescriptor.NONE]) no crean
     * entrada. Con el mismo `id` registrado dos veces **gana el ultimo** (informacion cosmetica).
     */
    fun record(descriptors: Map<String, DishVisualDescriptor>) {
        descriptors.forEach { (recipeId, descriptor) ->
            if (recipeId.isBlank() || descriptor.isEmpty) return@forEach
            this.descriptors[recipeId] = descriptor
        }
    }

    /**
     * Descriptor registrado para [recipeId], o `null` si el id es `null`/en blanco o no tiene entrada
     * (receta semilla, receta restaurada del Lote o un Favorito, o `id` desconocido). Un `null` hace
     * que la composicion sea **exactamente** la de la fase 1.
     */
    fun descriptorFor(recipeId: String?): DishVisualDescriptor? =
        recipeId?.takeIf { it.isNotBlank() }?.let { descriptors[it] }
}
