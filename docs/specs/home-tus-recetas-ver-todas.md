# Feature Implementation Spec: "Tus recetas" — las últimas guardadas y botón "Ver todas"

## Source Feature

- `id`: `home-tus-recetas-ver-todas`
- `area`: `ui`
- `depends_on`: `["home-offline-blocks", "favorites", "local-result-filters"]` (las tres `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner **no**
  modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (issue **#131**)
- Referencias: `DESIGN.md` (`Core Screens` 2 y 6; bloque *Excepción de capturas* issues #129–#132;
  `Components` → `button-secondary`), `PROGRESS.md` (sesión 113) y las specs aceptadas
  `home-offline-blocks`, `favorites` y `local-result-filters`.
- **Exención de capturas autorizada** (2026-10-14, issue #131; misma clase que #77/#85/#86/#91/#98/#101/
  #110, registrada en `DESIGN.md` líneas ~241-262): es una **correctiva** del bloque "Tus recetas"
  contra `DESIGN.md` y las capturas **ya aceptadas**, así que **no** genera capturas nuevas y **no** se
  crea `docs/design/concepts/home-tus-recetas-ver-todas/`. La feature **no** queda bloqueada por capturas.

## Goal

La persona dejó de ver sentido a los chips que filtraban localmente las recetas guardadas dentro del
bloque **"Tus recetas"** de la Home (un bloque de resumen no es una pantalla de exploración). El bloque
pasa a ser un **resumen**: el **título "Tus recetas"** con una acción **"Ver todas"** que abre la pestaña
**Favoritos**, y como mucho las **6 últimas** recetas guardadas (las más recientes primero, el mismo orden
que la pestaña Favoritos), pintadas con el grupo "Guardadas" que ya existe. El estado vacío de usuario
nuevo ("Cuando guardes una receta aparecerá aquí.") y la regla "solo se pintan grupos con contenido" se
conservan.

El **filtro local** (`ui.filters.LocalResultFilter`/`LocalResultFilters`/`ResultFilterRow` y su test) **no
se retira** en esta feature: se conserva tal cual, como pedía la entrada de `feature_list.json`, y la Home
deja de ser su consumidor. La Home deja también de mantener **estado** de filtro (`rememberSaveable`).

## Non-Goals

- **No** se toca `com.sazon.app.ui.filters` (`LocalResultFilter.kt`, `ResultFilterRow.kt`,
  `LocalResultFilterTest.kt`): ni se borra ni se reescribe. Retirar ese módulo (o cablearlo en otra
  pantalla) es una decisión/feature aparte, no esta correctiva.
- **No** se cablea el filtro local en la pantalla **Resultados** del Generador (`ui.results.ResultsScreen`)
  ni en **Favoritos** (que tiene su propia búsqueda por título y su orden). Ver "Decisions And Unknowns"
  (reconciliación de la segunda viñeta del `verification`).
- **No** cambian las tarjetas (`RecipeFicha`/`RecipeCard`), ni el Detalle, ni el Generador, ni la Búsqueda
  con IA, ni la pestaña Favoritos, ni el carrusel "Sugeridas para ti".
- **No** hay microcopy nuevo salvo la etiqueta **"Ver todas"** (elegida por la persona; **no**
  "Todas tus recetas"); **no** hay pantalla, ruta, estado visual, token, tipografía, asset ni dependencia
  nuevos; **sin** cambios en `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `assets/**`,
  `AndroidManifest.xml`, `domain/**`, `data/**`.
- **No** se crean ni modifican capturas bajo `docs/design/concepts/**` (exención #131).
- **No** se cambia ninguna regla de verificación ni el gate (`init.sh`).

## Job Story

Cuando abro la Home y quiero revisar lo que he guardado,
quiero ver de un vistazo las últimas recetas guardadas (sin chips de filtro en medio) y un botón claro
que me lleve a la pestaña Favoritos,
para seguir explorando la colección completa en la pantalla que sí está hecha para buscar y ordenar.

## Users And Permissions

- **Usuario local**: ve el bloque "Tus recetas" con hasta 6 guardadas y pulsa "Ver todas" para llegar a
  Favoritos. Todo **offline**; no hay permisos, cuentas ni red implicados.
- **Desarrollador / agente**: cambia la Home (`ui.home`), cablea la costura en `MainShell` y verifica en
  **JVM** (sin emulador, sin Robolectric, sin Compose UI tests; el repo no los tiene).
- **Persona**: revisa el acabado en el emulador tras el merge (AGENTS.md no automatiza UI), contra
  `DESIGN.md` y las capturas aceptadas; un defecto visual se corrige con una feature correctiva.

## Acceptance Scenarios

### Scenario 1: bloque poblado con más de 6 guardadas

Given el usuario tiene **8** recetas en Favoritos (ordenadas por `savedAt` desc: la más reciente
primero),
When se pinta el bloque "Tus recetas" de la Home,
Then se ven el título "Tus recetas" y la acción "Ver todas"; el grupo "Guardadas" pinta **exactamente 6**
tarjetas, las **6 más recientes** y en el mismo orden que la pestaña Favoritos (más reciente primero);
**no** se pinta ninguna fila de chips ni la línea "No hay recetas con este filtro.".

### Scenario 2: bloque poblado con 1 a 6 guardadas

Given el usuario tiene **3** recetas guardadas,
When se pinta el bloque,
Then se ven las 3 (mismo orden que Favoritos), el título y la acción "Ver todas" — sin recorte y sin
chips. Con **6** exactas se ven las 6 (el tope no recorta de más).

### Scenario 3: usuario nuevo (0 guardadas)

Given no hay recetas guardadas,
When se pinta el bloque,
Then solo se ven el título "Tus recetas" y la línea "Cuando guardes una receta aparecerá aquí."
(`HomeRecipes.isEmpty == true`); **no** hay chips, ni grupo "Guardadas", ni acción "Ver todas".

### Scenario 4: "Ver todas" abre la pestaña Favoritos

Given la Home activa (`MainNavigationState(tab = HOME, pushed = null)`) con recetas guardadas,
When se pulsa "Ver todas",
Then `HomeScreen` invoca `onOpenFavorites` y `MainShell` aplica
`navigation.selectTab(MainTab.FAVORITES)`, de modo que se monta `FavoritesScreen` con "Favoritos" como
pestaña activa en la barra inferior (y la Home deja de estar compuesta). El orden por defecto de Favoritos
es `FavoritesSort.RECENT` (`savedAt` desc), el mismo que el del bloque.

### Scenario 5: regla pura del tope (JVM)

Given la implementación terminada,
When se consulta la regla pura del bloque,
Then `HomeRecipes.MAX_VISIBLE == 6`; `HomeRecipes(saved = 8 recetas).latest()` devuelve las **6 primeras**
en el **mismo orden** de entrada (no reordena); con ≤ 6 recetas `latest()` devuelve la lista intacta; la
llamada **no** muta el original (`saved` sigue con 8) y `isEmpty` sigue siendo `saved.isEmpty()` antes y
después del tope. Sin Android ni Compose.

### Scenario 6: alcance, pureza y capturas

Given la implementación terminada,
When se inspecciona el diff,
Then `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` **no** importa ni usa
`ui.filters.LocalResultFilter*`/`ResultFilterRow` ni `rememberSaveable`/`Saver` para el filtro;
`HomeRecipes.filteredBy` ya **no** existe (`grep` → 0 en `app/src`); `ui/filters/**` y
`LocalResultFilterTest` están **intactos**; `domain/**`, `data/**`, `assets/**`, build/lockfile,
`init.sh`, `scripts/**` y `DESIGN.md` sin diff; **no** existe
`docs/design/concepts/home-tus-recetas-ver-todas/` y las capturas aceptadas de referencia no se han
modificado.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `home-tus-recetas-ver-todas` (metadata, `verification` de 3 viñetas y
  notas con la exención de capturas de la issue #131) y las entradas `accepted` de sus tres dependencias.
- `DESIGN.md` — `Core Screens` 2 (líneas ~485-495: el bloque muestra las **6 últimas** con el botón
  "Ver todas", **sin** chips, y se retira la línea "No hay recetas con este filtro."), `Core Screens` 6,
  bloque *Excepción de capturas* (~241-262) y tokens (`button-secondary`, `rounded.full`, `spacing`).
- `PROGRESS.md` líneas ~688-734 y ~4820-4879 — decisión de la persona (sesión 113): 6 últimas + botón
  "Ver todas" (etiqueta elegida por ella), worktree/issue #131 y exención de capturas.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — **el** fichero a corregir: mantiene el estado
  `filter` (`rememberSaveable` + `LocalResultFilterSaver`, líneas 78-80), pinta `ResultFilterRow`
  (líneas 251-256) y la línea "No hay recetas con este filtro." (líneas 264-270) dentro de
  `RecipesSection`; `HomeScreen` recibe `recipes: HomeRecipes` (línea 66) y una costura `onOpenGenerator`/
  `onOpenRecipe` (71-72) donde encaja la nueva.
- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — modelo del bloque; hoy expone `isEmpty` y
  `filteredBy(filter, taxonomy)` (su KDoc dice que es "la operacion pura que consume 'Tus recetas'").
- `app/src/main/java/com/sazon/app/ui/home/HomeSuggestions.kt` — precedente del tope (`MAX_VISIBLE = 6`) y
  del patrón "logica pura + capa Compose delgada".
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — construye `HomeRecipes(saved = favorites.map { it.recipe })`
  (187-189) y posee `navigation` (`MainNavigationState` + `MainNavigationStateSaver`); la pestaña
  Favoritos ya se monta desde ahí (201-210).
- `app/src/main/java/com/sazon/app/ui/navigation/MainNavigationState.kt` — `MainTab.HOME/FAVORITES/PROFILE`
  y `fun selectTab(tab)` (conserva `pushed`, que en Home es `null`).
- `app/src/main/java/com/sazon/app/data/favorites/FavoriteDao.kt` — `ORDER BY savedAt DESC` en
  `observe()`/`getAll()`: la lista que llega a `HomeRecipes.saved` ya viene **más reciente primero**.
- `app/src/main/java/com/sazon/app/ui/favorites/{FavoritesSort,FavoritesList,FavoritesScreen}.kt` — el
  orden por defecto de Favoritos es `RECENT` = `savedAt` desc; mismo orden que el bloque.
- `app/src/main/java/com/sazon/app/ui/filters/{LocalResultFilter,ResultFilterRow}.kt` — módulo del filtro
  local (a **conservar**, no a tocar).
- `app/src/main/java/com/sazon/app/ui/results/ResultsScreen.kt` — **verificado**: la pantalla Resultados
  del Generador **no** pinta `ResultFilterRow` ni usa `LocalResultFilters` (solo hay chips de orden en
  Favoritos). Relevante para la reconciliación de la viñeta 2 del `verification`.
- `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` — hoy cubre el estado vacío y **5 casos** de
  `filteredBy` (quedan obsoletos).
- `app/src/test/java/com/sazon/app/ui/navigation/MainNavigationStateTest.kt` — `selectTab` ya está cubierto
  (`select_tab_cambia_la_pestana_activa`): no hace falta test nuevo de navegación pura.
- `docs/technical-discovery.md` líneas ~104-164 — describe la Home y el filtrado local aplicado al bloque
  "Tus recetas" (queda obsoleto).
- `CONTEXT.md` líneas ~106-115 — "Bloque 'Tus recetas'" y "Filtros de resultados" (dicen que el filtro
  aplica al bloque de la Home).
- `docs/design/stitch-prompt.md` líneas ~154-161 — sección B.5: describe la fila de chips dentro de "Tus
  recetas" (queda obsoleta).
- `docs/specs/{home-offline-blocks,local-result-filters,favorites}.md` — contratos aceptados que esta
  feature **no** reabre salvo el bloque "Tus recetas"; `local-result-filters` (línea ~40) ya dice que
  "Tus recetas es el único consumidor actual" del filtro.
- `docs/design/concepts/{home-offline-blocks,local-result-filters,favorites}/` — solo `README.md` (los
  backfills de Home y filtros **no** tienen `.png` todavía); `favorites/favoritos/screen.png` sí existe.

### Existing Patterns To Follow

- **Lógica pura testeable en JVM + capa Compose delgada** (`HomeGreeting`, `HomeIdentity`,
  `HomeSuggestions`, `HomeRecipes`); ninguna regla nueva dentro de un `@Composable`.
- **Tope como constante con KDoc** (`HomeSuggestions.MAX_VISIBLE = 6`); aquí `HomeRecipes.MAX_VISIBLE = 6`
  es **coherente** con el carrusel, sin acoplarse a él (no se importa `HomeSuggestions`).
- **Navegación sin librería**: `MainNavigationState.selectTab` + `Saver` de `rememberSaveable` en
  `MainShell`; la costura de pantalla es un callback (`onOpenGenerator`, `onOpenRecipe`, `onExplore`).
- **Acción ligera con `TextButton`** para navegación/secundarios de texto (Resultados: "Atrás"; Perfil:
  "Editar"; Generador: "Cancelar"): es el tratamiento elegido para "Ver todas" (ver decisiones). El token
  `button-secondary` (`DESIGN.md`) queda para barras/botones secundarios grandes ("Cambiar ingredientes").
- **Orden más reciente primero** provisto por la fuente (Room `savedAt DESC`), no re-ordenado en la UI.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- El bloque "Tus recetas" mantiene **estado de UI de filtro** y pinta **chips** que la persona ha
  descartado; además pinta la línea "No hay recetas con este filtro." (se retira).
- No existe **regla pura** del tope "6 últimas": hoy sería un `take(6)` inline en Compose, no testeable.
- `HomeRecipes.filteredBy` queda **sin lectores** al retirar los chips (su único consumidor es
  `HomeScreen`), y su KDoc afirmaría algo falso.
- El `verification` de la feature afirma que `ResultFilterRow`/`LocalResultFilters` "siguen en uso" **en
  Resultados**; en el repo actual **no** hay ningún consumidor en `app/src/main` aparte de la Home
  (verificado por grep). Ver "Decisions And Unknowns" (reconciliación).

## Technical Approach

1. **Regla pura del bloque** (`ui/home/HomeRecipes.kt`): añadir `const val MAX_VISIBLE = 6` (en un
   `companion object`) y `fun latest(): HomeRecipes = copy(saved = saved.take(MAX_VISIBLE))`, con KDoc que
   documente que el orden de entrada ya es "más reciente primero" (lo aporta `FavoriteRepository`) y que
   el tope **no** reordena. Retirar `filteredBy` y los imports de `ui.filters`/`Taxonomy` que solo él usaba.
   *Nombres provisionales*: si se mueven, mantener el contrato y citarlo en la evidencia.
2. **Home sin estado de filtro** (`ui/home/HomeScreen.kt`): eliminar `filter`, `LocalResultFilterSaver`,
   los imports de `ui.filters`, `rememberSaveable`/`Saver`/`listSaver`/`mutableStateOf`/`getValue`/
   `setValue` si quedan sin uso, la llamada a `ResultFilterRow`, el cálculo de `categoriesIn` y la línea
   "No hay recetas con este filtro.".
3. **Cabecera del bloque con la acción** (`RecipesSection`): fila `fillMaxWidth` con
   `Arrangement.SpaceBetween` y alineación centrada que pinta `Text("Tus recetas", titleMedium)` y, **solo
   si `!recipes.isEmpty`**, un `TextButton(onClick = onOpenFavorites) { Text("Ver todas") }`. Después:
   estado vacío (línea actual) **o** `HomeRecipesGroup("Guardadas", recipes.latest().saved, ...)`. Nada
   más cambia en `HomeRecipesGroup` (sigue sin pintar grupos vacíos).
4. **Costura de navegación**: `HomeScreen` gana el parámetro `onOpenFavorites: () -> Unit` (antes de
   `modifier`) y `MainShell` lo cablea a `navigation = navigation.selectTab(MainTab.FAVORITES)` (mismo
   patrón que el `onExplore` de Favoritos, que hace `selectTab(MainTab.HOME)`).
5. **Tests JVM** (`HomeRecipesTest`): conservar los dos casos de estado vacío/no vacío; **quitar** los 5 de
   `filteredBy` (su sujeto desaparece) y añadir casos de `latest()`: 8 → 6 primeras en orden, ≤ 6 sin
   recorte, 6 exactas sin recorte, `MAX_VISIBLE == 6`, y que `latest()` no muta el original.
6. **Docs durables**: `docs/technical-discovery.md` (Home y filtrado local), `CONTEXT.md` (entradas del
   bloque y de los filtros) y `docs/design/stitch-prompt.md` (sección B.5). `DESIGN.md` **no** se toca
   (ya es la fuente de verdad del cambio, escrito por la sesión 113).

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — modificar; `MAX_VISIBLE = 6` + `latest()`
  (puro) y retirada de `filteredBy`.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — modificar; nuevo `onOpenFavorites`, título del
  bloque con `TextButton("Ver todas")`, tope por `latest()`, y retirada del estado de filtro, de
  `ResultFilterRow` y de la línea "No hay recetas con este filtro."; KDoc del bloque al día.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — modificar; `onOpenFavorites = { navigation =
  navigation.selectTab(MainTab.FAVORITES) }` (+ KDoc de la Home).
- `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` — modificar; casos de `latest()`/tope en
  lugar de los de `filteredBy`.
- `docs/technical-discovery.md` — modificar; el bloque "Tus recetas" ya no filtra en local (6 últimas +
  "Ver todas" → Favoritos) y la Home no mantiene estado de filtro.
- `CONTEXT.md` — modificar (mínimo); entradas "Bloque 'Tus recetas'" y "Filtros de resultados".
- `docs/design/stitch-prompt.md` — modificar (mínimo); sección B.5 sin la fila de chips y con la acción
  "Ver todas", para que el prompt no contradiga `DESIGN.md`.
- `docs/design/concepts/**` — **not needed** (exención #131; solo lectura, no se crea carpeta).
- `DESIGN.md`, `docs/build-brief.md`, `docs/domain-model.md`, `docs/adr/**`, `AGENTS.md` — **not needed**
  (ver abajo).
- `feature_list.json` y `PROGRESS.md` — los actualiza el flujo: el implementer deja la entrada en
  `passing` con evidencia; `accepted` lo persiste el orquestador.

## Visual Design Impact

- UI involved: **sí** (`area: ui`; cambia el contenido del bloque 5 de la Home).
- Design source: **`DESIGN.md` manda** (`Core Screens` 2, líneas ~485-495, ya describe las 6 últimas + el
  botón "Ver todas" y la retirada de los chips y de la línea de "sin filtro") más las capturas **ya
  aceptadas** de referencia: `docs/design/concepts/favorites/favoritos/screen.png` (+ `code.html`,
  `DESIGN.md`) y las capturas aceptadas de la Home / `local-result-filters` en lo que **no** cambia
  (cabecera, héroe, carrusel, tarjetas y barra inferior).
- Screens or states affected: **Home → "Tus recetas"**, en **claro y oscuro**, con los estados
  **vacío** (0 guardadas), **poblado ≤ 6** y **poblado > 6** (tope).
- New design artifact required: **no**.
- **UI captures (NOT required — exención autorizada, issue #131)**: `DESIGN.md` registra que esta feature
  **no** genera capturas nuevas y que **no** se crea
  `docs/design/concepts/home-tus-recetas-ver-todas/`. El implementer **no** crea ni modifica nada bajo
  `docs/design/concepts/**`. Nota: el backfill de capturas de la Home (`home-offline-blocks/home/`) sigue
  **pendiente** (hoy solo hay `README.md`), así que no se exige ni se cita ningún `.png` de Home que no
  exista.
- Feature-specific visual states: **ninguno nuevo**. Se conservan el título del bloque (`titleMedium`),
  el título de grupo "Guardadas" (`titleSmall`), el carrusel (`LazyRow` con `SazonSpacing.md`) y el estado
  vacío (`bodyLarge` + `onSurfaceVariant`). La única pieza nueva es la acción de texto **"Ver todas"**
  (`TextButton`, chrome de Material 3, sin tokens nuevos ni hex sueltos), alineada al final de la fila del
  título — es el punto que la persona revisa en el emulador.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — el párrafo de la Home (bloque "Tus recetas" = 6 últimas +
  "Ver todas" → pestaña Favoritos) y el del filtrado local (el módulo `ui.filters` se conserva, pero la
  Home ya **no** lo aplica ni mantiene estado de filtro).
- `CONTEXT.md`: **update (mínimo)** — "Bloque 'Tus recetas'" (muestra las 6 últimas guardadas, más
  recientes primero, y un botón "Ver todas" a Favoritos) y "Filtros de resultados" (deja de aplicarse al
  bloque de la Home).
- `docs/design/stitch-prompt.md`: **update (mínimo)** — sección B.5.
- `DESIGN.md`: **not needed** — ya declara el comportamiento nuevo (lo escribió la sesión 113) y sigue
  siendo la fuente de verdad; esta feature lo **implementa**.
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados nuevos.
- `docs/build-brief.md`, `docs/adr/**`: **not needed** — alcance histórico/decisiones aceptadas; el
  comportamiento vigente vive en `CONTEXT.md`, `DESIGN.md` y `docs/technical-discovery.md`.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/design/concepts/**`: **not needed** — exención de capturas; nada que versionar aquí.

## Implementation Plan

1. `HomeRecipes`: añadir `MAX_VISIBLE = 6` y `latest()`; retirar `filteredBy` e imports; KDoc al día.
2. `HomeRecipesTest`: quitar los casos de `filteredBy` y añadir los de `latest()`/tope (Escenario 5).
3. `HomeScreen`: retirar estado de filtro + chips + línea "sin filtro"; pintar la fila del título con
   `TextButton("Ver todas")` y las recetas por `recipes.latest().saved`; añadir `onOpenFavorites`; KDoc.
4. `MainShell`: cablear `onOpenFavorites` a `selectTab(MainTab.FAVORITES)`; KDoc de la Home.
5. Actualizar `docs/technical-discovery.md`, `CONTEXT.md` y `docs/design/stitch-prompt.md`.
6. Ejecutar el gate, las inspecciones de alcance (`grep`, `git diff --name-only`) y registrar la evidencia
   (la entrada queda `passing`; la validación independiente es de otro rol).

## Implementation Tasks

- [ ] `HomeRecipes`: `MAX_VISIBLE = 6` y `latest()` (puro, estable, sin mutar) con el contrato del
      Escenario 5; retirar `filteredBy` y los imports de `ui.filters`/`Taxonomy`.
- [ ] `HomeRecipesTest`: fuera los 5 casos de `filteredBy`; dentro 8 → 6 en orden, ≤ 6 sin recorte, 6
      exactas, `MAX_VISIBLE == 6` y no-mutación; se conservan los dos casos de estado vacío/no vacío.
- [ ] `HomeScreen`: fuera `filter`, `LocalResultFilterSaver`, `ResultFilterRow`, `categoriesIn`, la línea
      "No hay recetas con este filtro." y los imports que queden sin uso.
- [ ] `HomeScreen`: fila del título "Tus recetas" + `TextButton("Ver todas")` (solo si hay guardadas) y
      grupo "Guardadas" con `recipes.latest().saved`; nuevo parámetro `onOpenFavorites`.
- [ ] `MainShell`: `onOpenFavorites = { navigation = navigation.selectTab(MainTab.FAVORITES) }` y KDoc.
- [ ] Verificar por inspección: 0 referencias a `filteredBy` en `app/src`; `ui/home` sin imports de
      `ui.filters`; `ui/filters/**` y `LocalResultFilterTest` **intactos**; sin hex nuevos fuera de
      `ui/theme`; `domain/**`, `data/**`, `assets/**`, build/lockfile, `init.sh`, `scripts/**` y
      `DESIGN.md` sin diff; `docs/design/concepts/**` sin cambios y **sin** carpeta nueva.
- [ ] Actualizar `docs/technical-discovery.md`, `CONTEXT.md` y `docs/design/stitch-prompt.md` (B.5).
- [ ] `bash init.sh` (test + lint) en verde, registrar evidencia y avisar al orquestador; **sin** commit,
      push ni PR por parte del implementer.

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante estándar: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); `init.sh` **no** se modifica y **no** arranca procesos largos.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.home.*"
  --rerun-tasks` → `BUILD SUCCESSFUL` (sin regresión de `HomeGreetingTest`, `HomeIdentityTest`,
  `HomeSuggestionsTest`, `RecipeCardFormatTest`, `RecipeMonogramTest`).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **0 fallos/errores/saltados**;
  re-derivar la baseline en `HEAD` (`git grep -c '@Test'`) y registrar el delta (esperado: **−5** casos de
  `filteredBy`, **+5** de `latest()`/tope ⇒ delta 0 o ±0, sin suites nuevas).
- Lint: `app/build/reports/lint-results-debug.sarif` → **0 errores** y ningún hallazgo nuevo bajo
  `app/src` (los `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion`/`OldTargetApi`
  son preexistentes).
- Mapeo explícito del `verification` de la feature:
  - *"no pinta la fila de filtros locales; como máximo 6 guardadas, las más recientes (orden de
    Favoritos), y un botón 'Ver todas' que navega a la pestaña Favoritos (MainTab.FAVORITES)"* →
    escenarios 1, 2, 4 y 5 + inspección de `HomeScreen` (0 `ResultFilterRow`, 0 `LocalResultFilterSaver`)
    y del `onOpenFavorites` cableado a `selectTab(MainTab.FAVORITES)`.
  - *"El filtro local se conserva en Resultados (ResultFilterRow/LocalResultFilters siguen en uso) y la
    Home deja de mantener el estado de filtro"* → **reconciliado**: esta feature **no retira** nada de
    `ui/filters/**` (ni su test) y la Home deja de mantener el estado de filtro; pero **hecho verificado
    en este repo**: `ResultFilterRow`/`LocalResultFilters` **no** tienen consumidor en `app/src/main`
    aparte de la Home (la pantalla Resultados del Generador no filtra en local). Se acepta como
    "conservado y sin recortes"; **no** se cablean en Resultados (sería otra feature). Ver "Decisions And
    Unknowns".
  - *"'bash init.sh' verde (test + lint) y las capturas de referencia de
    docs/design/concepts/home-tus-recetas-ver-todas/ respetadas"* → gate + **exención de la issue #131**
    (registrada en `DESIGN.md`): esa carpeta **no** se crea ni se exige; las capturas aceptadas de
    referencia se comprueban **sin modificar** (`git status docs/design/concepts` limpio y
    `test ! -e docs/design/concepts/home-tus-recetas-ver-todas`).
- Inspección de alcance y pureza:
  - `grep -rn "filteredBy" app/src` → **0**.
  - `grep -rn "ResultFilterRow\|LocalResultFilter" app/src/main/java/com/sazon/app/ui/home` → **0**.
  - `grep -rn "import android\|import androidx" app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` →
    **0** (sigue siendo lógica pura).
  - `git diff --name-only` = Expected File Changes (+ esta spec); **sin** diff en `ui/filters/**`,
    `domain/**`, `data/**`, `assets/**`, `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`,
    `DESIGN.md` ni `docs/design/concepts/**`.
- Cobertura **E2E**: **no aplica** — el repo no tiene suite E2E persistente y `AGENTS.md` no automatiza UI
  de emulador (el único test instrumentado es `ExampleInstrumentedTest`). La cobertura canónica es la
  lógica pura en JVM (`latest()`/`MAX_VISIBLE`) más la revisión visual de la persona; el cableado Compose
  de "Ver todas" se verifica por inspección del diff.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: en el emulador, con 0, 3 y 8
  favoritos, abrir Inicio en claro y oscuro y comprobar que "Tus recetas" no tiene chips, muestra como
  mucho las 6 últimas en el mismo orden que Favoritos, y que "Ver todas" abre la pestaña Favoritos (con la
  barra inferior marcando "Favoritos" y el orden "Recientes").

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal; conteos de suites/tests con delta vs `HEAD`.
- Resultado de `latest()`: 8 recetas → ids 1..6 en el mismo orden; 6 → 6; 3 → 3; el original conserva 8;
  `MAX_VISIBLE == 6`.
- Confirmación por inspección: `HomeScreen` sin estado de filtro ni chips, `TextButton("Ver todas")` solo
  con guardadas, `onOpenFavorites` → `selectTab(MainTab.FAVORITES)` en `MainShell`.
- `git diff --name-only` acotado a los ficheros esperados, `ui/filters/**` y `LocalResultFilterTest`
  intactos, `docs/design/concepts/**` limpio y **sin** carpeta `home-tus-recetas-ver-todas/`.
- Registro en `docs/technical-discovery.md`, `CONTEXT.md` y `docs/design/stitch-prompt.md`; confirmación
  de que el implementer **no** hizo commit, push ni PR.

## Decisions And Unknowns

Decisiones cerradas (el implementer no las reabre):

1. **Tope 6** (`HomeRecipes.MAX_VISIBLE = 6`), aplicado por la regla pura `latest()` sobre la lista que ya
   llega **más reciente primero** desde `FavoriteRepository` (`savedAt` desc, idéntico al orden "Recientes"
   de Favoritos). La Home **no** reordena ni consulta `savedAt`.
2. **"Ver todas" siempre que el bloque tenga guardadas** (no se condiciona a "más de 6"): el bloque deja
   de ser un sitio donde explorar, así que el acceso a Favoritos es útil también con 3 recetas. Con 0
   guardadas solo se pinta el estado vacío (sin acción).
3. **Microcopy exacto**: `"Ver todas"` (etiqueta elegida por la persona). Se conservan `"Tus recetas"`,
   `"Guardadas"` y `"Cuando guardes una receta aparecerá aquí."`. Se **retira** exactamente la línea
   `"No hay recetas con este filtro."`.
4. **Tratamiento del botón**: `TextButton` con la etiqueta "Ver todas" al final de la fila del título del
   bloque. Se descarta el `OutlinedButton` con el token `button-secondary` (ese token es de barras/botones
   secundarios grandes, como "Cambiar ingredientes") porque añadiría cromo a una sección de resumen
   ("tranquilo, pocos colores, sin adornos innecesarios", `DESIGN.md` → Product Feel). Si a la persona no
   le encaja en el emulador, es una **feature correctiva**, no una improvisación del implementer.
5. **`HomeRecipes.filteredBy` se retira**: era el único punto de filtro del bloque (su KDoc lo dice) y
   queda sin lectores; dejar un método muerto con un KDoc falso es la clase de deriva que esta correctiva
   elimina (precedente: `recipe-card-l2-unified` retiró la constante muerta `recipeCardImageRatio`).
6. **`ui/filters/**` no se toca** (ni `LocalResultFilter`, ni `ResultFilterRow`, ni su test): literal de la
   entrada de `feature_list.json`. En cambio, el estado de filtro **de la Home** desaparece.
7. **Sin capturas nuevas**: exención autorizada (#131) registrada en `DESIGN.md`; nada que crear bajo
   `docs/design/concepts/**`.

Incógnitas y riesgos (no bloquean; quedan anotados y los decide la persona):

- **`ResultFilterRow`/`LocalResultFilters` quedan sin consumidor en la app**: al retirar los chips de la
  Home, ese módulo (y `LocalResultFilters.visible`/`categoriesIn`) solo lo usan sus tests; la pantalla
  **Resultados** del Generador no pinta filtros locales. `DESIGN.md` (línea ~493) dice "el filtro local
  sigue en Resultados", lo que **no** es cierto en el código actual. Esta feature **conserva** el módulo
  (no lo retira y no lo cablea): retirarlo es una limpieza aparte y cablearlo en Resultados es una feature
  nueva. **Recomendación para el orquestador/persona**: decidir después de esta feature si se da de alta
  una feature de seguimiento (p. ej. filtros locales en Resultados) o si se retira el módulo; hasta
  entonces queda documentado en `docs/technical-discovery.md`.
- **KW de la segunda viñeta del `verification`**: se lee como "el módulo de filtros locales no se retira y
  la Home deja de mantener estado de filtro", no como "existe un consumidor en Resultados" (falso hoy).
  Si el validador exige lo segundo, el veredicto debe ser `revise`/`block` **sobre esa viñeta**, no sobre
  el comportamiento pedido por la persona (que sí queda implementado).
- **Etiqueta y sitio de "Ver todas"**: es la única pieza visual nueva y no hay captura (exención); se
  sigue `DESIGN.md` y el patrón `TextButton` del repo, y lo revisa la persona en el emulador.
- **Bloque con 6 tarjetas**: en la Home el carrusel "Guardadas" pasa a ser más corto cuando hay más de 6
  guardadas (antes se pintaban todas). Es el comportamiento pedido, no una regresión.
- **Supersede parcialmente** los escenarios del bloque "Tus recetas" de `home-offline-blocks` (chips y
  etiquetas de grupo en el estado vacío) y de `local-result-filters` (chips en la Home) más el escenario 9
  de `favorites` (el bloque mostraba **todas** las guardadas). Esas specs aceptadas **no** se editan
  (son históricas); el comportamiento vigente queda en `DESIGN.md`, `CONTEXT.md` y
  `docs/technical-discovery.md`.

## Validator Checklist

- [ ] Alcance: solo `ui/home/HomeRecipes.kt`, `ui/home/HomeScreen.kt`, `ui/MainShell.kt`,
      `ui/home/HomeRecipesTest.kt` y las 3 docs durables; **sin** tocar `ui/filters/**`,
      `domain/**`, `data/**`, `assets/**`, build/lockfile, `init.sh`, `scripts/**`, `DESIGN.md` ni
      `docs/design/concepts/**`, y **sin** dependencias nuevas.
- [ ] Escenarios 1-6: hasta 6 guardadas (más recientes primero, mismo orden que Favoritos), sin chips ni
      la línea "No hay recetas con este filtro.", estado vacío intacto y "Ver todas" → pestaña Favoritos.
- [ ] La regla pura `latest()`/`MAX_VISIBLE` es determinista, no reordena, no muta y está cubierta por sus
      casos JVM (incluida la frontera 6/7).
- [ ] `HomeScreen` no importa `ui.filters` ni mantiene estado de filtro (`rememberSaveable`/`Saver`);
      `HomeRecipes.filteredBy` ya no existe; `ui/filters/**` y `LocalResultFilterTest` intactos.
- [ ] `bash init.sh` verde con evidencia (comandos, conteos, vectores, greps); lint sin hallazgos nuevos
      bajo `app/src`.
- [ ] Capturas: **no** se exige ni existe `docs/design/concepts/home-tus-recetas-ver-todas/` (exención
      #131 registrada en `DESIGN.md`) y las capturas aceptadas de referencia **no** se han modificado.
- [ ] `docs/technical-discovery.md`, `CONTEXT.md` y `docs/design/stitch-prompt.md` (B.5) actualizados;
      `feature_list.json` (solo esta entrada, `passing` + evidencia) y `PROGRESS.md` coherentes; sin
      commit/push/PR del implementer.
- [ ] No se añade comportamiento ajeno: ni filtros en Resultados/Favoritos, ni cambios de tarjetas,
      Detalle, Generador, Búsqueda con IA, carrusel "Sugeridas para ti" ni tokens/microcopy/estados
      nuevos más allá de "Ver todas".
