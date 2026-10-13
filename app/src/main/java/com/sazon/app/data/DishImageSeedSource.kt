package com.sazon.app.data

/**
 * Seam de los **bytes empaquetados** del set semilla de imagenes de plato (fase 7 de
 * `docs/ai-dish-images-proposal.md`): dada la **clave canonica** de un plato
 * (`DishImageCacheKey.of(titulo)`, p. ej. `dish-image-v1-arroz-con-pollo-criollo`), devuelve la imagen
 * preparada que viaja en el APK.
 *
 * Contrato de fallo identico al puerto de generacion (ADR 0005): `null` = no hay asset disponible,
 * **nunca** excepcion; la **cancelacion** de la corrutina si se propaga. Quien lo implementa en la app
 * es `AssetDishImageSeedSource` (costura Android); el resolutor lo recibe como parametro **opcional**
 * (`DishImageResolver.seed`), de modo que sin fuente semilla el comportamiento es el de la fase 3.
 *
 * La interfaz es **funcional** (`fun interface`) para que los tests construyan una fuente falsa con un
 * lambda y cuenten lecturas sin tocar assets ni red.
 */
fun interface DishImageSeedSource {

    /** Bytes del asset semilla de [cacheKey]; `null` si no hay ninguno legible o esta vacio. */
    suspend fun bytesFor(cacheKey: String): ByteArray?
}
