package com.sazon.app.data.image

import com.sazon.app.domain.image.DishImageGenerator
import com.sazon.app.domain.image.DishImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Politica del **ajuste de imagen de plato** en un unico sitio (fase 4 de
 * `docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`): estado inicial del
 * interruptor y tope de generaciones por sesion. Constantes puras, sin Compose y sin Android (mismo
 * patron que `AiProviderPolicy` / `DishImageCachePolicy`).
 */
object DishImageSettingsPolicy {

    /**
     * Estado inicial del interruptor: la generacion viene **apagada** (opt-in). Hasta que el usuario
     * lo encienda no se lee cache, no se genera ni se consume tope (ADR 0006: sin red ni cuota por
     * defecto); encenderlo restaura el comportamiento de `dish-image-settings-toggle`.
     */
    const val DEFAULT_ENABLED = false

    /**
     * Tope de **generaciones por sesion**. Cubre dos pantallas de Resultados (6 + 6) sin exponer la
     * cuota semanal. Es una primera estimacion y se cambia en este unico punto.
     */
    const val MAX_GENERATIONS_PER_SESSION = 12
}

/**
 * Estado observable del ajuste, lo que pinta la seccion "Imagenes del plato" del Perfil.
 *
 * [remaining] y [exhausted] son **derivados** (misma disciplina determinista que
 * `DishImageCachePolicy.evictionVictims`): la misma entrada produce siempre la misma salida.
 */
data class DishImageSettingsState(
    /** Interruptor visible: `true` permite pedir imagenes, `false` deja la ficha tipografica. */
    val enabled: Boolean,
    /** Unidades de tope ya consumidas en la sesion. */
    val used: Int,
    /** Tope de la sesion. */
    val max: Int,
) {
    /** Unidades restantes; nunca negativo. */
    val remaining: Int get() = (max - used).coerceAtLeast(0)

    /** El tope de la sesion esta agotado (`used >= max`). */
    val exhausted: Boolean get() = used >= max
}

/**
 * Ajuste de sesion de la imagen de plato: **interruptor** + **tope por sesion**, en memoria.
 *
 * Uno por **proceso** = una sesion (se crea una vez en `MainActivity`); al morir el proceso el
 * contador vuelve a cero y el interruptor vuelve a [DishImageSettingsPolicy.DEFAULT_ENABLED]
 * (**apagado**: la generacion es opt-in y el arranque no gasta cuota ni reserva tope). **No** se
 * persiste (Decision 2 del spec): el ajuste no es una preferencia culinaria y no entra en
 * `UserProfile`.
 *
 * Se gobierna en dos puntos (Decision 1 del spec):
 *
 * 1. **Punto de inyeccion** (proveedor del seam en `MainActivity`): [generatedIfEnabled] corta en
 *    seco con el interruptor apagado, de modo que no se lee cache ni se genera nada (queda la ficha
 *    tipografica). Apagado **no** reserva tope.
 * 2. **Borde de generacion** ([GatedDishImageGenerator]): la reserva y el tope, donde se gasta cuota;
 *    los aciertos de memoria/disco **no** pasan por ahi.
 *
 * `state` re-emite al cambiar el interruptor y al reservar una unidad, de forma que la UI observa la
 * **misma** fuente que aplica el tope (no hay dos contadores que puedan divergir, Decision 6).
 */
class DishImageSettings(
    enabled: Boolean = DishImageSettingsPolicy.DEFAULT_ENABLED,
    private val budget: DishImageGenerationBudget = DishImageGenerationBudget(),
) {

    private val _state = MutableStateFlow(
        DishImageSettingsState(enabled = enabled, used = budget.used, max = budget.max),
    )

    /** Estado observable del ajuste; lo consume la seccion de Perfil con `collectAsState`. */
    val state: StateFlow<DishImageSettingsState> = _state.asStateFlow()

    /** El interruptor. Apagar/encender **no** reinicia el tope (Decision 7). */
    fun setEnabled(value: Boolean) {
        _state.update { it.copy(enabled = value) }
    }

    /**
     * Reserva una generacion: `false` si el interruptor esta **apagado** o el tope **agotado**. El
     * llamante ([GatedDishImageGenerator]) no invoca al delegado cuando devuelve `false`. Apagado
     * **no** reserva (cortocircuito: no consume tope).
     */
    fun tryAcquireGeneration(): Boolean {
        if (!_state.value.enabled) return false
        val acquired = budget.tryAcquire()
        if (acquired) {
            _state.update { it.copy(used = budget.used) }
        }
        return acquired
    }

    /**
     * Gate del **interruptor** en el punto de inyeccion: con el ajuste apagado devuelve `null`
     * **sin** invocar [produce] (no se lee cache, no se genera nada); encendido delega. Generico
     * (`<T : Any>`) para poder probarse en JVM con `String`, sin Android ni Compose.
     */
    suspend fun <T : Any> generatedIfEnabled(produce: suspend () -> T?): T? =
        if (_state.value.enabled) produce() else null
}

/**
 * **Decorador** del puerto [DishImageGenerator]: reserva una unidad **antes** de invocar al delegado y
 * devuelve `null` **sin invocarlo** si el interruptor esta apagado o el tope agotado (degradacion
 * silenciosa: la UI muestra la ficha tipografica).
 *
 * Recibe el [DishImageSettings] completo (no un contador suelto) para que el estado observable que
 * pinta Ajustes y el tope aplicado sean la misma fuente (Decision 6). **No** captura excepciones: un
 * fallo del proveedor se degrada en [DishImageResolver] (que ya relanza la `CancellationException`),
 * y una generacion cancelada deja consumida la reserva previa (semantica conservadora, Decision 4).
 */
class GatedDishImageGenerator(
    private val delegate: DishImageGenerator,
    private val settings: DishImageSettings,
) : DishImageGenerator {

    override suspend fun generate(request: DishImageRequest): ByteArray? =
        if (settings.tryAcquireGeneration()) delegate.generate(request) else null
}
