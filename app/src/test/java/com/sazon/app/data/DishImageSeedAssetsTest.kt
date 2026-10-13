package com.sazon.app.data

import com.sazon.app.domain.image.DishImageCacheKey
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verificacion exigida #1 de `dish-image-seed`: el **mapa clave canonica -> asset** del set semilla
 * (escenarios 1, 2, 5, 8 y 10 de `docs/specs/dish-image-seed.md`).
 *
 * El caso de existencias **no pasa en vacio**: deriva cada clave con `DishImageCacheKey.of(titulo)` (la
 * canonicalizacion **real** de la fase 1) y comprueba contra los assets empaquetados en
 * `app/src/main/assets/img/dishes/seed/`, cuyo directorio publica `dish.image.seed.assets.dir` desde
 * `app/build.gradle.kts` (mismo patron que `RecipePhotoAssetsTest`). Todo en JVM: sin Android, sin red
 * y sin cuota.
 *
 * Desde `dish-image-prompt-v2` las claves son `dish-image-v2-<slug>`: el renombrado de los 6 assets
 * (bytes identicos, sin cuota) es lo que hace que este test siga resolviendolos por la clave canonica.
 */
class DishImageSeedAssetsTest {

    private val realExists: (String) -> Boolean = { path -> seedFile(path).isFile }

    // Escenario 1: la clave canonica del plato resuelve su asset, existente y no vacio.
    @Test
    fun cada_plato_del_set_resuelve_su_asset_por_la_clave_canonica() {
        assertEquals(
            "El vector de la spec no cambia",
            "dish-image-v2-arroz-con-pollo-criollo",
            DishImageCacheKey.of("Arroz con pollo criollo"),
        )

        setDishTitles().forEach { title ->
            val cacheKey = DishImageCacheKey.of(title)
            val resolved = DishImageSeedAssets.resolve(cacheKey, realExists)

            assertNotNull("El plato '$title' deberia resolver su asset semilla", resolved)
            assertTrue(
                "El asset de '$title' lleva el nombre de la clave v2",
                resolved!!.substringAfterLast('/').startsWith("dish-image-v2-"),
            )
            assertEquals(
                "El asset de '$title' debe ser la clave canonica bajo la raiz del set",
                "${DishImageSeedAssets.ROOT}/$cacheKey",
                resolved.substringBeforeLast('.'),
            )
            val file = seedFile(resolved)
            assertTrue("'$resolved' deberia existir", file.isFile)
            assertTrue("'$resolved' no deberia estar vacio", file.length() > 0)
        }
    }

    // Escenario 2 (mapa): se prueba webp -> jpg -> png en ese orden y null si no existe ninguna.
    @Test
    fun las_extensiones_se_prueban_en_orden_webp_jpg_png() {
        val key = "dish-image-v2-plato-de-prueba"

        assertEquals(
            "Con solo webp y jpg gana webp",
            "${DishImageSeedAssets.ROOT}/$key.webp",
            DishImageSeedAssets.resolve(key, { it.endsWith("$key.webp") || it.endsWith("$key.jpg") }),
        )
        assertEquals(
            "Sin webp, gana jpg",
            "${DishImageSeedAssets.ROOT}/$key.jpg",
            DishImageSeedAssets.resolve(key, { it == "${DishImageSeedAssets.ROOT}/$key.jpg" }),
        )
        assertEquals(
            "Sin webp ni jpg, gana png",
            "${DishImageSeedAssets.ROOT}/$key.png",
            DishImageSeedAssets.resolve(key, { it == "${DishImageSeedAssets.ROOT}/$key.png" }),
        )
        assertNull(
            "Sin ninguna extension no hay ruta",
            DishImageSeedAssets.resolve(key, { false }),
        )
        assertEquals(listOf("webp", "jpg", "png"), DishImageSeedAssets.EXTENSIONS)
    }

    // Escenario 8 (determinismo del mapa): misma entrada -> misma salida, sin estado.
    @Test
    fun el_mapa_es_determinista() {
        val key = "dish-image-v2-paella-valenciana"
        val exists: (String) -> Boolean = { it == "${DishImageSeedAssets.ROOT}/$key.webp" }

        val first = DishImageSeedAssets.resolve(key, exists)
        val second = DishImageSeedAssets.resolve(key, exists)

        assertEquals(first, second)
        assertEquals("${DishImageSeedAssets.ROOT}/$key.webp", first)
        assertNull(
            "Otra clave con el mismo predicado no resuelve nada",
            DishImageSeedAssets.resolve("dish-image-v2-otro-plato", exists),
        )

        val altRoot = "img/dishes/seed-alt"
        val existsAlt: (String) -> Boolean = { it == "$altRoot/$key.webp" }
        assertEquals(
            "La ruta se compone con la raiz inyectada",
            "$altRoot/$key.webp",
            DishImageSeedAssets.resolve(key, existsAlt, root = altRoot),
        )
    }

    // Escenario 8 (saneado): una clave no canonica nunca produce una ruta fuera de ROOT.
    @Test
    fun una_clave_insegura_no_produce_ninguna_ruta() {
        val unsafeKeys = listOf(
            "",
            "   ",
            "Dish-Image-V1",
            "../etc/passwd",
            "../../etc/passwd",
            "dish/images",
            "dish image",
            "dish_image",
            "plato.",
            "plato\u0000",
        )

        unsafeKeys.forEach { cacheKey ->
            assertFalse("'$cacheKey' no es una clave segura", DishImageSeedAssets.isSafeKey(cacheKey))
            assertNull("path() de '$cacheKey' debe ser null", DishImageSeedAssets.path(cacheKey, "webp"))
            assertNull(
                "resolve() de '$cacheKey' debe ser null aunque todo exista",
                DishImageSeedAssets.resolve(cacheKey, { true }),
            )
        }

        assertTrue(DishImageSeedAssets.isSafeKey("dish-image-v2-arroz-con-pollo-criollo"))
    }

    // Escenario 5: una clave ajena al set no resuelve nada (el set no es un comodin).
    @Test
    fun una_clave_fuera_del_set_no_resuelve_nada() {
        val cacheKey = DishImageCacheKey.of("Plato que no existe")

        assertNull(DishImageSeedAssets.resolve(cacheKey, realExists))
    }

    // Escenario 10: el set versionado tiene un fichero por plato, extension conocida y <= 2 MiB.
    @Test
    fun el_set_esta_versionado_con_un_asset_por_plato_y_dentro_del_presupuesto() {
        val expectedKeys = setDishTitles().map { DishImageCacheKey.of(it) }
        val files = File(assetsDirPath()).listFiles().orEmpty().filter { it.isFile }

        assertEquals("Un asset por plato listado", expectedKeys.size, files.size)
        assertEquals(
            "Las claves de los assets son las claves canonicas esperadas",
            expectedKeys.sorted(),
            files.map { it.name.substringBeforeLast('.') }.sorted(),
        )
        assertTrue(
            "Todos los assets del set estan renombrados a la clave v2",
            files.all { it.name.startsWith("dish-image-v2-") },
        )

        var total = 0L
        files.forEach { file ->
            val extension = file.name.substringAfterLast('.')
            assertTrue(
                "'${file.name}' usa una extension admitida",
                extension in DishImageSeedAssets.EXTENSIONS,
            )
            assertTrue("'${file.name}' no debe estar vacio", file.length() > 0)
            assertTrue(
                "'${file.name}' debe resolverse por su clave canonica",
                expectedKeys.contains(file.name.substringBeforeLast('.')),
            )
            total += file.length()
        }
        assertTrue(
            "El set ocupa $total B y debe caber en 2 MiB",
            total <= 2L * 1024 * 1024,
        )
    }
}

/** Los 6 platos frecuentes del demo (decision 12 de la spec); la clave se deriva con `of(titulo)`. */
private fun setDishTitles(): List<String> = listOf(
    "Arroz con pollo criollo",
    "Paella valenciana",
    "Pizza margarita",
    "Hamburguesa casera",
    "Tarta de chocolate",
    "Ensalada césar",
)

/**
 * Fichero de un candidato dentro del directorio del set (la propiedad de sistema apunta a esa carpeta,
 * mientras que las rutas del mapa incluyen la raiz [DishImageSeedAssets.ROOT]).
 */
private fun seedFile(candidatePath: String): File =
    File(assetsDirPath(), candidatePath.removePrefix("${DishImageSeedAssets.ROOT}/"))

private fun assetsDirPath(): String = checkNotNull(System.getProperty("dish.image.seed.assets.dir")) {
    "Falta la propiedad de sistema dish.image.seed.assets.dir"
}
