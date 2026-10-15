package com.sazon.app.data.image

/**
 * Costura de persistencia del **estado del interruptor** de imagenes del plato. Un solo bit: la
 * implementacion puede ser `SharedPreferences` (app, `AndroidDishImageSettingsStore`) o una falsa en
 * memoria (tests).
 *
 * Es **sincrona** y **pura** (sin `android.*`, sin coroutines) a proposito: el ajuste se restaura en
 * el punto de composicion **antes** del primer frame, asi que una API suspendente obligaria a
 * `runBlocking` en el hilo principal o a una correccion asincrona posterior (parpadeo del interruptor
 * y una primera decision del gate potencialmente equivocada). Ver
 * `docs/specs/dish-image-settings-persistence.md`.
 *
 * Contrato: **no** lanza (un fallo se degrada a `null` / no-op, patron de degradacion silenciosa del
 * ADR 0005) y **no** expone nada del tope por sesion, de modo que el tope no puede persistirse por
 * construccion.
 */
interface DishImageSettingsStore {

    /** Estado guardado del interruptor, o `null` si nunca se guardo o el valor no es legible. */
    fun loadEnabled(): Boolean?

    /** Guarda el estado del interruptor (mejor esfuerzo; un fallo no interrumpe la UI). */
    fun saveEnabled(enabled: Boolean)
}

/**
 * Default seguro: sin estado guardado y sin efecto. Se usa en tests, previews y composiciones sin
 * ajuste (mismo patron que `NoDishImageOriginDiagnostics` / `LocalGeneratedRecipeArtworkSource`), de
 * forma que el parametro `store` de [DishImageSettings] tiene un default no-op y ninguna composicion
 * real "sin persistencia" puede olvidarse por accidente.
 */
object NoOpDishImageSettingsStore : DishImageSettingsStore {

    override fun loadEnabled(): Boolean? = null

    override fun saveEnabled(enabled: Boolean) = Unit
}
