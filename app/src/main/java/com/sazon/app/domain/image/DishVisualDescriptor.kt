package com.sazon.app.domain.image

/**
 * Descriptor visual del plato que el propio modelo puede emitir **en la misma llamada** que genera
 * la receta (variante B de `docs/ai-dish-images-proposal.md` §4.1, fase 5).
 *
 * Es informacion **cosmetica del modelo**, no un campo de [com.sazon.app.domain.recipe.Recipe] ni una
 * regla del Validador: **no** se persiste y **no** se valida. Su unico efecto es alimentar la
 * politica pura de imagen ([DishImageCacheKey], [DishImagePrompt]) cuando esta disponible; su
 * ausencia degrada **exactamente** al comportamiento de la fase 1 (clave y prompt por titulo/id).
 *
 * - [dishKey] es el **plato canonico** (pensado como clave de cache compartida): permite que dos
 *   variantes del mismo plato (`"arroz con pollo"` / `"arroz con pollo fácil"`) compartan imagen.
 *   Se conserva **tal cual** (mayusculas y diacriticos incluidos); quien lo canonicaliza es
 *   [DishImageCacheKey.normalize].
 * - [imagePrompt] es la **descripcion visual del plato terminado**; sustituye solo la clausula del
 *   plato en [DishImagePrompt], conservando prefijo, cocina y coletilla de estilo.
 *
 * [of] es puro (sin Android, sin red, sin estado ni reloj): colapsa espacios, recorta y descarta a
 * `null` los campos inutiles. Nunca lanza por datos de entrada arbitrarios (el wire los puede traer
 * de cualquier tipo).
 */
data class DishVisualDescriptor(
    /** Plato canonico tal y como lo emite el modelo, o `null` si no aporta nada util. */
    val dishKey: String? = null,
    /** Descripcion visual del plato terminado, o `null` si esta vacia. */
    val imagePrompt: String? = null,
) {

    /** `true` cuando ningun campo es utilizable (el draft lo convierte en `null`). */
    val isEmpty: Boolean get() = dishKey == null && imagePrompt == null

    /** `false` cuando al menos un campo es utilizable. */
    val isNotEmpty: Boolean get() = !isEmpty

    companion object {

        /** Descriptor vacio: equivale a `null` a efectos de clave y prompt. */
        val NONE = DishVisualDescriptor()

        private val WHITESPACE = Regex("\\s+")

        /**
         * Limpia y normaliza un descriptor crudo (p. ej. del wire de la IA):
         *
         * - cada campo colapsa espacios en blanco y se recorta ([singleLine]);
         * - `dishKey` se conserva **tal cual** (solo se descarta si tras [DishImageCacheKey.normalize]
         *   no queda ningun caracter `[a-z0-9]`: `"🍲"`, `"..."`, `"  "`);
         * - `imagePrompt` se descarta si queda vacio.
         *
         * El resultado puede tener los dos campos `null` ([isEmpty]).
         */
        fun of(dishKey: String?, imagePrompt: String?): DishVisualDescriptor =
            DishVisualDescriptor(
                dishKey = dishKey
                    ?.let(::singleLine)
                    ?.takeIf { it.isNotEmpty() && DishImageCacheKey.normalize(it).isNotEmpty() },
                imagePrompt = imagePrompt
                    ?.let(::singleLine)
                    ?.takeIf { it.isNotEmpty() },
            )

        /** Colapsa cualquier espacio en blanco para no ensuciar la plantilla con espacios dobles. */
        private fun singleLine(value: String): String = WHITESPACE.replace(value, " ").trim()
    }
}
