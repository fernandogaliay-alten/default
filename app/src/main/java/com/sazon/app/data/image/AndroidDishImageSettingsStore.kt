package com.sazon.app.data.image

import android.content.Context
import androidx.core.content.edit

/**
 * Adaptador Android de [DishImageSettingsStore] sobre **`SharedPreferences`**: el **unico** fichero de
 * persistencia del ajuste y el unico de esta feature que importa `android.*`.
 *
 * Por que `SharedPreferences` y no Room ni DataStore (Decision 1 de
 * `docs/specs/dish-image-settings-persistence.md`): es **API del framework** (sin dependencias nuevas;
 * `androidx.datastore` seguiria diferido), un **solo bit** no justifica una base Room con
 * entidad/DAO/KSP y el arranque necesita el valor de forma **sincrona** antes del primer frame (Room
 * obligaria a `runBlocking` o a una correccion asincrona posterior).
 *
 * `MODE_PRIVATE` (dato no sensible, sin permisos nuevos) y escritura con la extension KTX
 * `androidx.core.content.edit` (**apply**, nunca la escritura bloqueante `commit`: no hay I/O
 * bloqueante en el hilo principal, se evita el warning `ApplySharedPref` de lint y se usa el idioma
 * recomendado por lint frente a `prefs.edit().apply()`, que dispararia `UseKtx`). El fichero y la clave son **detalle de
 * implementacion** (no vocabulario de dominio) y **sin migracion**: la ausencia de valor significa
 * "sin estado guardado" (default de la politica). Ambos metodos estan blindados: un valor de tipo
 * distinto (el framework lanzaria `ClassCastException`) o un fichero ilegible se degradan a `null` /
 * no-op en vez de romper la UI.
 */
class AndroidDishImageSettingsStore(context: Context) : DishImageSettingsStore {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** `null` si nunca se guardo, si el valor guardado no es un `Boolean` o si la lectura falla. */
    override fun loadEnabled(): Boolean? =
        runCatching {
            if (prefs.contains(KEY_ENABLED)) prefs.getBoolean(KEY_ENABLED, false) else null
        }.getOrNull()

    /** Mejor esfuerzo: un fallo de escritura deja la sesion con el valor elegido y sin error de UI. */
    override fun saveEnabled(enabled: Boolean) {
        runCatching { prefs.edit { putBoolean(KEY_ENABLED, enabled) } }
    }

    private companion object {
        const val PREFS_NAME = "dish_image_settings"
        const val KEY_ENABLED = "enabled"
    }
}
