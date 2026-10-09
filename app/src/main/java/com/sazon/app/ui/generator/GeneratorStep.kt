package com.sazon.app.ui.generator

/**
 * Pasos del asistente de dos pasos del Generador (microcopy de `docs/design/stitch-prompt.md` C).
 *
 * [INGREDIENTS] es el paso 1 real ("¿Qué tienes?") que implementa esta feature; [SETTINGS] es el paso 2
 * ("Ajusta tu receta"), un marcador que rellenara `generator-settings-step`.
 *
 * Logica pura: sin imports de Compose ni Android, testeable en JVM. [next]/[previous] no se salen de
 * rango (devuelven `null` en los extremos).
 */
enum class GeneratorStep(val title: String, val primaryLabel: String) {
    INGREDIENTS("¿Qué tienes?", "Continuar"),
    SETTINGS("Ajusta tu receta", "Generar recetas"),
    ;

    val isLast: Boolean get() = this == SETTINGS

    fun next(): GeneratorStep? = when (this) {
        INGREDIENTS -> SETTINGS
        SETTINGS -> null
    }

    fun previous(): GeneratorStep? = when (this) {
        INGREDIENTS -> null
        SETTINGS -> INGREDIENTS
    }
}
