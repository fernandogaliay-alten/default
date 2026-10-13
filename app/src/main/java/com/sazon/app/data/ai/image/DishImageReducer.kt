package com.sazon.app.data.ai.image

/**
 * Costura de reduccion/codificacion: convierte los bytes crudos que devuelve el proveedor en la imagen
 * que se cacheara (WebP reducido). Es una `fun interface` para que el transporte se pruebe en **JVM
 * puro** con un reductor falso y `android.graphics` quede aislado en un unico fichero.
 */
fun interface DishImageReducer {

    /** Reduce los bytes crudos del proveedor a la imagen que se cacheara; `null` si no se puede. */
    fun reduceToWebp(rawBytes: ByteArray): ByteArray?
}
