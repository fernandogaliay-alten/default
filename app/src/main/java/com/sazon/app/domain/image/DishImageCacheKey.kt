package com.sazon.app.domain.image

import com.sazon.app.domain.recipe.Recipe
import java.text.Normalizer
import java.util.Locale

/**
 * Clave canonica de la imagen de un plato: identidad de cache de la fase 3, derivada **solo** del
 * nombre del plato.
 *
 * Politica (ver `docs/adr/0005-ai-dish-images.md` y `docs/ai-dish-images-proposal.md` §5):
 *
 * - La clave es el **nombre del plato**, no el `id` ni la clasificacion de la receta: dos recetas del
 *   mismo plato (busquedas distintas, recetas `ai` regeneradas) comparten imagen y no repiten cuota.
 * - `recipe.id` entra **solo como respaldo** cuando no hay titulo util, para que dos recetas sin
 *   nombre no compartan imagen (una no debe heredar la foto de la otra).
 * - [VERSION] es el **unico** mecanismo de invalidacion: cambiar la normalizacion o la plantilla del
 *   prompt (`DishImagePrompt`) obliga a subirla, porque la cache se indexa por clave y no por prompt.
 *
 * Logica **pura** (JDK), sin Android, sin red, sin estado y sin reloj/aleatoriedad: la misma entrada
 * produce siempre la misma clave. Nunca lanza por datos de entrada y nunca devuelve una cadena vacia
 * (lo peor que hace es degradar al respaldo por defecto).
 */
object DishImageCacheKey {

    /** Version del algoritmo de normalizacion/clave; subirla invalida la cache de imagenes. */
    const val VERSION = "dish-image-v1"

    /** Respaldo final cuando no hay ni titulo ni `id` utilizables (`dish-image-v1-plato`). */
    const val FALLBACK_KEY = "plato"

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val WHITESPACE = Regex("\\s+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]")
    private val HYPHENS = Regex("-+")

    /**
     * Slug puro (sin version) de un nombre de plato: minusculas, sin diacriticos, sin puntuacion y
     * con guiones colapsados. Cadena vacia si no queda ningun caracter `[a-z0-9]` (titulo en CJK,
     * emoji, solo simbolos).
     *
     * Pasos, en este orden: recorte y colapso de espacios, descomposicion `NFD` y eliminacion de
     * `\p{Mn}+`, minusculas con [Locale.ROOT] (**explicito**: el idioma del dispositivo no puede
     * cambiar la clave), sustitucion de todo caracter fuera de `[a-z0-9]` por `-`, colapso de guiones
     * repetidos y recorte de los extremos.
     *
     * La normalizacion es **tipografica, no semantica**: `"Arroz con pollo"` y `"Arroz con pollo
     * facil"` producen claves distintas a proposito (una imagen redundante es preferible a una imagen
     * equivocada); la fusion conceptual llega con el descriptor del modelo (fase 5).
     */
    fun normalize(dishName: String): String {
        val collapsed = WHITESPACE.replace(dishName.trim(), " ")
        val withoutDiacritics = DIACRITICS.replace(
            Normalizer.normalize(collapsed, Normalizer.Form.NFD),
            "",
        )
        val lowercased = withoutDiacritics.lowercase(Locale.ROOT)
        return HYPHENS.replace(NON_ALPHANUMERIC.replace(lowercased, "-"), "-").trim('-')
    }

    /**
     * Clave de un **nombre de plato**: [VERSION] + slug, o [FALLBACK_KEY] si el nombre no aporta
     * ningun caracter util. Alfabeto `[a-z0-9-]`, apta como nombre de fichero y como columna de Room.
     */
    fun of(dishName: String): String {
        val slug = normalize(dishName)
        return if (slug.isEmpty()) "$VERSION-$FALLBACK_KEY" else "$VERSION-$slug"
    }

    /**
     * Clave de una [recipe]: titulo -> respaldo por `id` (`dish-image-v1-plato-<slug-id>`) ->
     * respaldo por defecto (`dish-image-v1-plato`).
     *
     * `categoryId`, `cuisineId`, `source` y `id` **no** influyen cuando hay titulo util.
     */
    fun of(recipe: Recipe): String {
        val byTitle = normalize(recipe.title)
        if (byTitle.isNotEmpty()) return "$VERSION-$byTitle"
        val byId = normalize(recipe.id)
        if (byId.isNotEmpty()) return "$VERSION-$FALLBACK_KEY-$byId"
        return "$VERSION-$FALLBACK_KEY"
    }
}
