# Validación independiente — `home-tus-recetas-ver-todas`

- Feature id: `home-tus-recetas-ver-todas`
- Spec: `docs/specs/home-tus-recetas-ver-todas.md`
- Rama/worktree: `feature/home-tus-recetas-ver-todas`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/home-tus-recetas-ver-todas`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 117;
  sin commit/push/PR, trabajo en disco — esperado)
- Veredicto: **accept**

## Alcance validado

Feature de UI (`area: ui`, issue #131, depende de `home-offline-blocks`, `favorites` y
`local-result-filters`, las tres `accepted`). El bloque **"Tus recetas"** de la Home deja de pintar los
chips de filtro local y la línea "No hay recetas con este filtro."; muestra como máximo las **6 últimas**
guardadas (mismo orden que la pestaña Favoritos, más reciente primero) y un botón **"Ver todas"** que abre
la pestaña Favoritos (`MainTab.FAVORITES`).

1. `ui/home/HomeRecipes.kt` (puro): `companion object { const val MAX_VISIBLE = 6 }` +
   `fun latest(): HomeRecipes = copy(saved = saved.take(MAX_VISIBLE))` (no reordena, no muta, sin
   Compose/Android); se **retira** `filteredBy` y los imports de `ui.filters`/`domain.taxonomy`.
2. `ui/home/HomeScreen.kt`: fuera el estado de filtro (`rememberSaveable` + `LocalResultFilterSaver`),
   `ResultFilterRow`, `categoriesIn` y la línea "No hay recetas con este filtro."; `RecipesSection` pinta
   la fila del título con `TextButton("Ver todas")` **solo si hay guardadas** y el grupo "Guardadas" con
   `recipes.latest().saved`; nuevo parámetro `onOpenFavorites: () -> Unit`.
3. `ui/MainShell.kt`: `onOpenFavorites = { navigation = navigation.selectTab(MainTab.FAVORITES) }`.
4. `ui/home/HomeRecipesTest.kt`: fuera los 5 casos de `filteredBy`; dentro 5 de `latest()`/tope.

El módulo `com.sazon.app.ui.filters` (`LocalResultFilter.kt`, `ResultFilterRow.kt`,
`LocalResultFilterTest.kt`) se **conserva intacto** (no se retira ni se cablea en Resultados).
Exención de capturas autorizada (issue #131, registrada en `DESIGN.md`): **no** debe existir
`docs/design/concepts/home-tus-recetas-ver-todas/`.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); salió `UP-TO-DATE`, se forzó rerun |
| Test JVM forzado | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL, 5m40s, 34/34 tareas |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | BUILD SUCCESSFUL, 4m35s, 30/30 tareas |
| Parseo propio XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **97 suites / 733 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline en `HEAD` | `git grep -c '@Test' HEAD` | **97 ficheros / 733 anotaciones** → delta **0 tests / 0 suites** (`HomeRecipesTest` 7) |
| Lint SARIF | `app/build/reports/lint-results-debug.sarif` | **13 resultados / 0 errores / 0 bajo `app/src`** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1 = preexistentes) |

`HomeRecipesTest` = 7 tests verdes: `max_visible_es_6`,
`latest_con_mas_de_6_guardadas_devuelve_las_6_primeras_en_orden`, `latest_con_menos_de_6_guardadas_no_recorta`,
`latest_con_6_exactas_no_recorta`, `latest_no_altera_el_bloque_original`, más los dos casos de
estado vacío/no vacío.

## Escenarios

- **E1** (>6 guardadas): `RecipesSection` pinta el título + "Ver todas" y el grupo "Guardadas" con
  `recipes.latest().saved`; `take(6)` deja las 6 primeras sin reordenar. ✔
- **E2** (1..6): sin recorte; 6 exactas no se recortan. ✔
- **E3** (0 guardadas): solo título + "Cuando guardes una receta aparecerá aquí."; sin chips, sin grupo,
  sin acción (`HomeRecipes.isEmpty`). ✔
- **E4** (navegación): `onOpenFavorites` → `navigation.selectTab(MainTab.FAVORITES)` con el mismo patrón
  que el `onExplore` de Favoritos; `selectTab` conserva `pushed` (null en Home). ✔
- **E5** (regla pura): `MAX_VISIBLE == 6`; `latest()` no reordena, no muta y `isEmpty` no cambia. ✔
- **E6** (alcance/pureza/capturas): verificado por diff, greps y `git status`. ✔

Orden confirmado de forma independiente: `FavoriteDao.observe/getAll` → `ORDER BY savedAt DESC`;
`RoomFavoriteRepository.observe()` lo preserva; `FavoritesSort` por defecto = `RECENT`; `MainShell`
alimenta `HomeRecipes.saved` con `favorites.map { it.recipe }`, así que el bloque y la pestaña comparten
orden.

## Punto sensible: viñeta 2 del `verification`

El `verification` literal dice "El filtro local se conserva en Resultados (ResultFilterRow/LocalResultFilters
siguen en uso)". Comprobado por el validador: `ResultFilterRow`/`LocalResultFilters` **no** tienen ningún
consumidor en `app/src/main` aparte de la Home (`ResultsScreen.kt` no filtra en local; Favoritos no usa el
módulo). La discrepancia es **preexistente** (la spec aceptada `local-result-filters` ya dice que "Tus
recetas es el único consumidor actual") y esta feature **no** la introduce: **conserva** `ui/filters/**` y
su test intactos. Se **acepta la reconciliación** de la spec ("módulo conservado sin recortes, sin cablear
en Resultados"): exigir un consumidor vivo en Resultados equivaldría a una feature fuera de alcance que la
persona no pidió. No es un `revise`.

## Disciplina de alcance y seguridad

- `git diff --name-only` = los 4 ficheros de código/test + las 3 docs durables (+ la spec sin trackear);
  **sin** diff en `ui/filters/**` (`git diff -- ui/filters` vacío, byte a byte igual a `HEAD`),
  `domain/**`, `data/**`, `assets/**`, `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`,
  `DESIGN.md` ni `docs/design/concepts/**`.
- Greps rehechos por el validador: `filteredBy` en `app/src` → **0**;
  `ResultFilterRow|LocalResultFilter` en `ui/home` → **0**;
  `rememberSaveable|Saver|mutableStateOf|"No hay recetas con este filtro"` en `HomeScreen.kt` → **0**;
  0 imports `android`/`androidx` en `HomeRecipes.kt`.
- Capturas: `test ! -e docs/design/concepts/home-tus-recetas-ver-todas` OK y
  `git status docs/design/concepts` limpio.
- Seguridad: sin secretos, sin red, sin auth, sin entrada de usuario, sin SQL, sin dependencias nuevas;
  el diff no introduce superficie de ataque.

## Hallazgos

**Critical / High / Medium: ninguno.**

Low/Info informativos no bloqueantes:

- **L1** — la viñeta 2 del `verification` de `feature_list.json` conserva una redacción que sugiere un
  consumidor vivo en Resultados (falso hoy); reconciliada al persistir `accepted`. Decisión opcional de la
  persona: dar de alta una feature de seguimiento (filtros locales en Resultados) o una limpieza que retire
  el módulo huérfano.
- **L2** — `CONTEXT.md` "Filtros de resultados … Aplica a la pantalla Favoritos" (Favoritos usa su propia
  búsqueda por título/orden); redacción heredada e imprecisa, coherente con la definición del concepto.
- **L3** — inconsistencia interna histórica de `DESIGN.md` (texto de la exención #91 ~línea 197 frente a
  `Core Screens` ~línea 490); `DESIGN.md` está fuera de alcance y `Core Screens` es la autoridad.

## Cierre

Veredicto **`accept`**; el orquestador de `feature-flow` persistió `accepted` en `feature_list.json`
(sesión 118), registró el cierre en `PROGRESS.md` y crea el commit local de la rama. La publicación
(push + PR) queda para el orquestador de `github-flow`. Queda para la persona la **revisión manual** en el
emulador (no automatizable desde WSL): Inicio en claro y oscuro con 0, 3 y 8 favoritos — "Tus recetas" sin
chips, como mucho las 6 últimas en el orden de Favoritos, y "Ver todas" abriendo la pestaña Favoritos con
la barra marcando "Favoritos"; un defecto visual se corrige con una feature correctiva.
