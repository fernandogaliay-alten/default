package com.sazon.app.data

/**
 * Convencion de nombres y **resolucion** del set semilla de imagenes de plato: platos frecuentes del
 * demo preparados **una vez** y empaquetados como assets versionados (`app/src/main/assets/`, misma
 * disciplina que `seed-recipes.json` y los assets de `img/recipes/`).
 *
 * Es la **tercera capa** de la cache de imagen de plato de `docs/ai-dish-images-proposal.md` §5:
 * `DishImageResolver` la consulta entre el disco y el generador, de modo que esos platos muestran su
 * foto propia **sin red y sin gastar cuota** (ni siquiera la primera vez). Ver
 * `docs/adr/0005-ai-dish-images.md` (fase 7).
 *
 * Logica **pura**: sin Android, sin Compose y **sin I/O**. La existencia de un fichero se inyecta con
 * un predicado (`exists`), de modo que la misma funcion sirve al test JVM (fichero en disco) y a la app
 * (`AssetDishImageSeedSource`, lectura real de assets) sin duplicar reglas. Determinista y sin estado:
 * los mismos argumentos devuelven el mismo resultado.
 *
 * | Grupo | Ruta | Clave | Nº |
 * |---|---|---|---|
 * | Foto preparada de un plato frecuente | `img/dishes/seed/<cacheKey>.<ext>` | `DishImageCacheKey.of(titulo)` | 1 por plato |
 *
 * **Nunca** se empaqueta un asset para un plato que ya tenga foto propia (las 12 recetas semilla
 * resuelven su foto por `id` en `img/recipes/seed/<recipeId>.<ext>`): un asset semilla para su titulo
 * pisaria una foto propia con otra distinta (la semilla gana al generador y lo generado gana a la
 * cadena local).
 */
object DishImageSeedAssets {

    /** Raiz del set semilla dentro de `assets/`. */
    const val ROOT = "img/dishes/seed"

    /**
     * Extensiones que se prueban **en orden** para cada clave. No se asume formato: el script de
     * preparacion (`scripts/prepare-dish-image-seed.sh`) usa la extension real (*magic bytes*) y
     * convierte a WebP q80 cuando hay herramienta, de modo que la app funciona con `webp`, `jpg` o
     * `png` sin cambios. Se reutiliza la lista de `recipe-photos` para no fijar el formato en dos
     * sitios.
     */
    val EXTENSIONS = RecipePhotoAssets.EXTENSIONS

    /**
     * Alfabeto canonico de la clave: `[a-z0-9-]+`. Es **defensa en profundidad**, no la fuente de
     * verdad de la canonicalizacion (esa es `DishImageCacheKey`): una clave fabricada a mano con `..`,
     * `/`, espacios o mayusculas devuelve `null` en vez de una ruta fuera de [ROOT].
     */
    val SAFE_KEY = Regex("[a-z0-9-]+")

    /** La clave es un nombre de asset admisible (alfabeto canonico, no vacia). */
    fun isSafeKey(cacheKey: String): Boolean = SAFE_KEY.matches(cacheKey)

    /**
     * Ruta completa `"<root>/<cacheKey>.<extension>"`, o `null` si [cacheKey] no es segura: **nunca**
     * una ruta fuera de [root]. [root] es inyectable para poder probar el caso "raiz inexistente" sin
     * depender del contenido real.
     */
    fun path(cacheKey: String, extension: String, root: String = ROOT): String? =
        if (isSafeKey(cacheKey)) "$root/$cacheKey.$extension" else null

    /**
     * Primera ruta completa cuyo fichero exista, recorriendo [EXTENSIONS] **en orden**
     * (`webp` -> `jpg` -> `png`). Devuelve `null` si la clave no es segura o si no existe ninguna
     * extension: nunca lanza ni devuelve una ruta vacia.
     *
     * `resolve("dish-image-v2-arroz-con-pollo-criollo", exists)` devuelve
     * `"img/dishes/seed/dish-image-v2-arroz-con-pollo-criollo.<ext>"` con la primera extension
     * existente. [root] por defecto es [ROOT] (misma raiz que usa la app en runtime).
     */
    fun resolve(
        cacheKey: String,
        exists: (String) -> Boolean,
        root: String = ROOT,
    ): String? {
        if (!isSafeKey(cacheKey)) return null
        for (extension in EXTENSIONS) {
            val candidate = path(cacheKey, extension, root) ?: continue
            if (exists(candidate)) return candidate
        }
        return null
    }
}
