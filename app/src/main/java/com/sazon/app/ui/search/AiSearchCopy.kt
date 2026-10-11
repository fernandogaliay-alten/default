package com.sazon.app.ui.search

/**
 * Microcopy de la Busqueda con IA en un **unico** sitio (sin Compose ni Android, testeable en JVM).
 *
 * Los textos autoritativos salen de `docs/design/stitch-prompt.md` seccion B y de
 * `docs/design/stitch/11-home-busqueda-ia.md`; el resto se toma de la captura aceptada
 * `docs/design/concepts/ai-search/busqueda-ia/` y es revisable por la persona.
 */
object AiSearchCopy {

    // Barra (bloque 2 de la Home).
    const val PLACEHOLDER = "¿Qué te apetece hoy?"
    const val HELPER = "Pídeselo a la IA: «algo con pollo y arroz»"
    const val OFFLINE_NOTE = "Sin conexión · la búsqueda no está disponible"

    // Carga.
    const val CANCEL_LABEL = "Cancelar búsqueda"

    // Fallo de IA / red.
    const val FAILURE_TITLE = "No hemos podido buscar. Revisa tu conexión."
    const val RETRY_LABEL = "Reintentar"

    // Bloqueo por Perfil (tarjeta ambar, nunca roja).
    const val BLOCKED_TITLE = "Tu perfil no permite esta receta"
    const val BLOCKED_BODY = "Tu alimentación o tus alergias impiden esta búsqueda."
    const val REVIEW_PROFILE_LABEL = "Revisar mi perfil"

    // Sin resultados (no bloqueo por Perfil).
    const val NO_RESULTS_TITLE = "No hemos encontrado recetas para esa búsqueda"
    const val NO_RESULTS_BODY = "Prueba a pedirlo de otra forma o crea una receta con lo que tengas."
    const val CREATE_RECIPE_LABEL = "Crear receta"
}
