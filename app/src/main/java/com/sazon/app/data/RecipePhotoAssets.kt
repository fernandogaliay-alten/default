package com.sazon.app.data

/**
 * Convencion de nombres y **cadena de respaldo** de las fotos de plato empaquetadas como assets
 * offline (`app/src/main/assets/`, fuente versionada, como `seed-recipes.json`).
 *
 * Logica **pura**: sin Android, sin Compose y **sin I/O**. La existencia de un fichero se inyecta con
 * un predicado (`exists`), de modo que la misma funcion sirve al test JVM (fichero en disco) y a la
 * app (`AssetRecipePhotoSource`, lectura real de assets) sin duplicar reglas. Determinista y sin
 * estado: los mismos argumentos devuelven el mismo resultado.
 *
 * Raiz y grupos (ver `docs/design/concepts/recipe-photos/README.md` y `docs/domain/taxonomy.md` §6):
 *
 * | Grupo | Ruta | Clave |
 * |---|---|---|
 * | Foto propia de la receta semilla | `img/recipes/seed/<recipeId>.<ext>` | `id` de la receta |
 * | Foto de la categoria | `img/recipes/category/cat-<categoryId>.<ext>` | `categoryId` real |
 * | Foto por defecto | `img/recipes/default.<ext>` | generica/neutra |
 */
object RecipePhotoAssets {

    /** Raiz de las fotos dentro de `assets/`. */
    const val ROOT = "img/recipes"

    /** Clave de la foto generica/neutra de ultimo recurso. */
    const val DEFAULT_KEY = "default"

    /**
     * Extensiones que se prueban **en orden** para cada candidato. No se asume formato: el script de
     * empaquetado (`scripts/fetch-recipe-photos.sh`) usa la extension real (magic bytes) y convierte a
     * WebP cuando hay herramienta, de modo que la app funciona con `webp`, `jpg` o `png` sin cambios.
     */
    val EXTENSIONS = listOf("webp", "jpg", "png")

    /**
     * Las **18 `recipeCategories` reales** de la Taxonomia. Un `categoryId` fuera de este conjunto
     * (`null`, vacio, `"no-existe"`) **no** produce candidato de categoria: la cadena salta directo a
     * la foto por defecto (nunca se inventa `cat-<id>`).
     */
    val CATEGORY_IDS = setOf(
        "pasta",
        "rice",
        "soup_cream",
        "salad",
        "meat",
        "poultry",
        "fish_seafood",
        "vegetarian",
        "vegan",
        "legume_stew",
        "pizza_bread",
        "egg_breakfast",
        "sandwich_wrap",
        "tapas_appetizer",
        "dessert",
        "smoothie_drink",
        "casserole_oven",
        "other",
    )

    /**
     * Candidatos **sin extension**, en orden de respaldo: foto propia de la receta (si su `id` no esta
     * vacio) → foto de su categoria (solo si `categoryId` es una de las 18 reales) → foto por defecto.
     *
     * `candidates("seed-tortilla-patatas", "egg_breakfast")` devuelve
     * `["img/recipes/seed/seed-tortilla-patatas", "img/recipes/category/cat-egg_breakfast",
     * "img/recipes/default"]`.
     */
    fun candidates(recipeId: String, categoryId: String?): List<String> = buildList {
        if (recipeId.isNotBlank()) add("$ROOT/seed/$recipeId")
        if (categoryId != null && categoryId in CATEGORY_IDS) {
            add("$ROOT/category/cat-$categoryId")
        }
        add("$ROOT/$DEFAULT_KEY")
    }

    /**
     * Resuelve la **primera** ruta completa cuyo fichero exista, recorriendo [candidates] en orden y,
     * para cada candidato, [EXTENSIONS] en orden. Devuelve `null` si no existe ninguno (el llamante
     * pinta entonces el degradado + glifo de `RecipeArtwork`): nunca lanza ni devuelve una ruta vacia.
     */
    fun resolve(
        recipeId: String,
        categoryId: String?,
        exists: (String) -> Boolean,
    ): String? {
        for (base in candidates(recipeId, categoryId)) {
            for (extension in EXTENSIONS) {
                val path = "$base.$extension"
                if (exists(path)) return path
            }
        }
        return null
    }
}
