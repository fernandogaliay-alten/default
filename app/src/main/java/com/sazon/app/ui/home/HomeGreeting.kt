package com.sazon.app.ui.home

/**
 * Saludo por franja horaria de la cabecera de la Home, puro y testeable en JVM.
 *
 * Regla de `docs/design/stitch-prompt.md` seccion B y `docs/domain-model.md`: con nombre se usa la
 * banda horaria; sin nombre (nulo o solo espacios) cae al literal **"Buenas tardes"**, un caso de
 * perfil corrupto/migrado. Los cortes 5..11 / 12..20 / 21..4 son una eleccion documentada, ajustable
 * como detalle visual sin cambiar este contrato.
 */
object HomeGreeting {

    /** Literal de respaldo cuando no hay nombre que saludar. */
    const val FALLBACK = "Buenas tardes"

    fun text(displayName: String?, hour: Int): String {
        val name = displayName?.trim().orEmpty()
        if (name.isEmpty()) return FALLBACK
        return "${band(hour)}, $name"
    }

    private fun band(hour: Int): String = when (hour) {
        in 5..11 -> "Buenos días"
        in 12..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
