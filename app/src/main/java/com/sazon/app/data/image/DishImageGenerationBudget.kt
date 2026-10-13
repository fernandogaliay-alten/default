package com.sazon.app.data.image

import java.util.concurrent.atomic.AtomicInteger

/**
 * Tope de **generaciones por sesion** de la imagen de plato: contador atomico con reserva
 * **estricta** (una unidad por invocacion del generador real, es decir por peticion al proveedor).
 *
 * Es **puro** (sin Android, sin Compose, sin I/O ni reloj) y **thread-safe**: la concurrencia del
 * loader de la fase 4 es 2, asi que dos generaciones simultaneas no pueden rebasar el tope
 * ([AtomicInteger] con CAS). El tope viene **inyectado** (por defecto la constante de
 * [DishImageSettingsPolicy], en un unico sitio, patron `AiProviderPolicy`) para poder probarlo con un
 * valor pequeno sin depender de la constante de produccion.
 *
 * "Sesion" = vida del proceso: el contador se crea una vez en `MainActivity` y vuelve a cero al morir
 * el proceso. **No** se persiste (Decision 2 de `docs/specs/dish-image-settings-toggle.md`).
 */
class DishImageGenerationBudget(
    private val maxGenerations: Int = DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION,
) {

    private val counter = AtomicInteger(0)

    /** Tope de la sesion (unidades reservables). */
    val max: Int get() = maxGenerations

    /** Unidades ya reservadas (invocaciones del generador permitidas). */
    val used: Int get() = counter.get()

    /** Unidades que quedan; nunca negativo. */
    val remaining: Int get() = (maxGenerations - counter.get()).coerceAtLeast(0)

    /** El tope esta agotado (`used >= max`). */
    val exhausted: Boolean get() = counter.get() >= maxGenerations

    /**
     * Reserva una unidad; `false` si el tope esta agotado (el llamante **no** debe invocar al
     * generador). Nunca rebasa el tope, ni con dos reservas simultaneas.
     */
    fun tryAcquire(): Boolean {
        while (true) {
            val current = counter.get()
            if (current >= maxGenerations) return false
            if (counter.compareAndSet(current, current + 1)) return true
        }
    }
}
