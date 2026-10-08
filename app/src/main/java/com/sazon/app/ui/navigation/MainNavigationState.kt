package com.sazon.app.ui.navigation

/**
 * Destinos de nivel superior del armazon principal (barra inferior), en orden. Las etiquetas son el
 * microcopy de `docs/design/stitch-prompt.md` seccion 3.
 *
 * Logica pura: no importa Compose ni Android; el `Saver` de `rememberSaveable` vive en la capa
 * Compose (`MainShell`).
 */
enum class MainTab(val label: String) {
    HOME("Inicio"),
    FAVORITES("Favoritos"),
    PROFILE("Perfil"),
}

/** Paginas empujadas por encima de las pestanas (crecera con `RESULTS`/`DETAIL` en sus features). */
enum class PushedScreen { GENERATOR }

/**
 * Estado de navegacion del armazon principal: pestana activa y pagina empujada opcional.
 *
 * Navegacion sin libreria (mismo criterio que `AppDestination`): las operaciones son inmutables
 * (`copy`) y la pestana se conserva al empujar y sacar una pagina. `navigation-compose` puede
 * adoptarse despues sin cambiar este contrato.
 */
data class MainNavigationState(
    val tab: MainTab = MainTab.HOME,
    val pushed: PushedScreen? = null,
) {
    fun selectTab(tab: MainTab): MainNavigationState = copy(tab = tab)

    fun push(screen: PushedScreen): MainNavigationState = copy(pushed = screen)

    fun pop(): MainNavigationState = copy(pushed = null)
}
