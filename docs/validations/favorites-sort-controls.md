# Validation Record: favorites-sort-controls

- Feature: `favorites-sort-controls`
- Spec: `docs/specs/favorites-sort-controls.md`
- Date: 2026-10-13
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `bash init.sh` (WSL, vía `scripts/gradle.sh`) → **exit 0**. Aviso: en esa pasada Gradle reportó
  `testDebugUnitTest`/`lintAnalyzeDebug` **UP-TO-DATE** (no re-ejecutó nada), así que **no** se tomó como
  evidencia suficiente y se forzó una pasada real (abajo).
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**, 34/34 tareas
  **executed** (exit 0).
- `bash scripts/gradle.sh lint --rerun-tasks` → **BUILD SUCCESSFUL**, 30/30 tareas **executed** (exit 0).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml` → **64 suites, 456 tests, 0
  fallos / 0 errores / 0 saltados**. Suites de Favoritos: `FavoritesListTest` 9, `FavoriteServiceTest` 6,
  `RoomFavoriteRepositoryTest` 9, todas verdes. Coincide exactamente con el número del implementer
  (reproducido, no aceptado de su resumen).
- Parseo propio de `app/build/reports/lint-results-debug.xml` → **0 errores, 13 warnings** preexistentes
  (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1),
  **todos** en `app/build.gradle.kts`, `gradle/libs.versions.toml` y `gradle-wrapper.properties`;
  **0 issues bajo `app/src/`** → 0 en `app/src/main/java/com/sazon/app/ui/favorites`.

## Code conformance (inspección del diff, no del resumen)

- `FavoritesSort.kt`: único cambio `RECENT("Añadidas recientemente", "recent")` →
  `RECENT("Recientes", "recent")`. `key`, orden de entries, `TIME`/`DIFFICULTY` y `fromKey` (desconocido →
  `RECENT`) intactos; la semántica de orden (`FavoritesList.visible`, `savedAt` desc) no se toca.
- `FavoritesScreen.kt`: `ViewModeButton` (dos `FilterChip` de texto) eliminado →
  `ViewModeToggle(Surface(shape = SazonPillShape, color = colorScheme.surface, shadowElevation = 2.dp)`
  con `Row(selectableGroup().padding(2.dp))`) + `ViewModeIconButton`
  (`Modifier.size(28.dp).clip(CircleShape).background(fill).selectable(role = Role.RadioButton)` con
  `semantics { contentDescription }`). Activo `primary`/`onPrimary`; inactivo `Color.Transparent` +
  `onSurfaceVariant`. `FavoritesSortRow` conserva el `LazyRow(Modifier.weight(1f))` con los tres
  `FilterChip` de orden. `ListGlyph`/`GridGlyph` privados con `Canvas` (patrón `CloudOffGlyph`/
  `BookmarkGlyph`), 16dp dentro de círculos de 28dp.
- Greps propios: **0** referencias a `ViewModeButton` en `app/src`; **0** literales de texto
  "Lista"/"Rejilla" salvo las **2** `contentDescription` exigidas por el Scenario 6; **0** hex sueltos
  (`#RRGGBB` o `Color(0x…)`) en `app/src/main/java/com/sazon/app/ui/favorites`; sin imports huérfanos
  (todos los nuevos — `selectable`, `selectableGroup`, `CircleShape`, `Surface`, `clip`, `CornerRadius`,
  `Size`, `Role`, `contentDescription`, `semantics` — están en uso).
- Comparado con la captura aceptada `docs/design/concepts/favorites/favoritos/code.html` (líneas 74-98
  claro y 260-286 oscuro): píldora `bg-white`/`bg-[#241C16]` con `p-0.5`, dos botones `w-7 h-7` (28px)
  redondos, activo `#D9552B`/blanco con `view_agenda`, inactivo `#8C7E74` con `grid_view`, icono 16px. La
  implementación reproduce tamaño, geometría, relleno interior de 2dp y mapeo de roles.

## Scope discipline

- `git status --porcelain=v1 --untracked-files=all` → solo `M DESIGN.md`, `M PROGRESS.md`,
  `M FavoritesScreen.kt`, `M FavoritesSort.kt`, `M feature_list.json` y `?? docs/specs/favorites-sort-controls.md`
  (spec del planner, se commitea en el accept como en features previas).
- Sin tocar: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `domain/**`, `data/**`,
  `FavoriteRecipeCard.kt`, `FavoritesList.kt`, `FavoritesViewMode.kt`, `MainShell.kt`, `SazonApp.kt`,
  `MainActivity.kt`, `ui.theme/**`, `docs/design/concepts/favorites/**`, `docs/design/stitch*/**`.
  Sin dependencias nuevas. No se creó `docs/design/concepts/favorites-sort-controls/` (exención #85).
- `git diff --stat` solo con los ficheros previstos; sin trabajo de otras correctivas.

## Acceptance Scenarios

- Scenario 1 (chip por defecto "Recientes", verificación exigida): **por inspección**, `RECENT.label ==
  "Recientes"` y `fromKey` degrada a `RECENT`; sigue siendo el orden por defecto (`savedAt` desc). El
  **recorte** exacto es acabado visual de emulador (persona).
- Scenario 2 (toggle icónico): **por inspección**, no hay ningún `Text` en el toggle; píldora `surface`,
  dos círculos de 28dp, activo `primary`/`onPrimary`, inactivo sin relleno + `onSurfaceVariant`.
- Scenario 3 (cambia el modo conservando el comportamiento): **por inspección**, el toggle reutiliza
  `onSelectViewMode` y el estado `viewMode`+`FavoritesViewModeSaver` sin cambios (no hay estado nuevo).
- Scenario 4 (resto de la pantalla sin cambios): **por inspección**, el diff de `FavoritesScreen.kt` solo
  toca la zona del toggle y añade glifos; buscador, chips "Tiempo"/"Dificultad", nota offline, tarjetas,
  rejilla y estado vacío intactos.
- Scenario 5 (roles del tema, sin hex): **por inspección**, solo `colorScheme.surface/primary/onPrimary/
  onSurfaceVariant` y `Color.Transparent`; grep de hex sueltos = 0.
- Scenario 6 (accesibilidad): **por inspección**, `selectableGroup()` + `selectable(selected, onClick,
  role = Role.RadioButton)` + `contentDescription` "Lista"/"Rejilla"; el estado seleccionado se anuncia y
  cada botón tiene descripción, así que el estado no depende solo del color.
- Scenario 7 (sin lógica nueva ni dependencias): **re-ejecutado**, mismas 64 suites / 456 tests en verde,
  lint sin errores ni warnings en ficheros de la feature, `app/build.gradle.kts` y
  `gradle/libs.versions.toml` intactos.

## Documentation / harness

- `DESIGN.md` (verificado en el diff): registra el microcopy **"Recientes"** (Core Screen 6 + bullet
  **FilterChip**), el **toggle lista/rejilla icónico** (bullet propio: 28dp, activo terracota) y la
  **excepción de capturas** de `favorites-sort-controls` (issue #85), en la misma línea que la de
  `recipe-artwork` (#77). Coherente con la captura aceptada y sin contradicción con la tabla `Colors`
  ("Primary = acción principal, **estado activo**, coincidencia").
- Spec: el `Visual Design Impact` registra la exención de capturas (issue #85) y exige el registro en
  `DESIGN.md`; la entry de `feature_list.json` también recoge la desviación autorizada en `notes`.
  Confirmado el texto de la issue **#85** (abierta) como brief autoritativo.
- `PROGRESS.md` sesión 081 y `feature_list.json` (status `passing`, 10 evidencias) consistentes con lo
  reproducido. `init.sh` sigue ejecutando `test` + `lint` de verdad y falla con exit ≠ 0.
- Sin tocar `AGENTS.md`, `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/design/stitch*/`
  ni `docs/specs/favorites.md` (históricos), según `Durable Documentation Impact`.

## No commit / no push / no PR

- `HEAD` = `9fb7dbc` ("chore: registra las features correctivas…"), commit de provisioning contenido en
  `origin/main`; la rama `feature/favorites-sort-controls` **no tiene upstream**, no existe rama remota
  (`git ls-remote --heads origin feature/favorites-sort-controls` → vacío) y `gh pr list --head
  feature/favorites-sort-controls` → `[]`. Todo el trabajo está **sin commitear** en el worktree.

## Findings

- Sin hallazgos **Critical/High/Medium**.
- Low / informativos (no bloqueantes):
  1. **Objetivo táctil de 28dp vs "48dp mínimo"** de `DESIGN.md` (Accessibility Baseline): se sigue la
     captura aceptada y el toggle queda por debajo del mínimo declarado (igual que los `FilterChip` de la
     fila). Es un `Unknown` **explícito** de la spec y `DESIGN.md` registra los 28dp, así que no es una
     desviación oculta; la persona lo revisa en el emulador y, si lo considera un defecto de ergonomía, se
     corrige con una feature correctiva (regla de AGENTS.md).
  2. **Acabado puramente visual** (que el chip "Recientes" quepa sin recorte, fidelidad fina de
     `ListGlyph`/`GridGlyph`, claro/oscuro) queda para la revisión manual de la persona: `AGENTS.md` no
     automatiza UI de emulador y la spec lo asigna explícitamente a la persona.
  3. La rama está **3 commits por detrás** de `origin/main`; toca sincronizar antes de publicar
     (lo hace el orquestador de `github-flow` con `scripts/sync-worktree.sh`). No es un defecto de la
     feature.
- Seguridad: **sin hallazgos**. Diff solo de UI Compose local (sin red, sin persistencia, sin input de
  usuario, sin secretos, sin datos personales, sin dependencias nuevas, sin `WebView`/intents); las cadenas
  nuevas son literales estáticos mostrados como descripción accesible.

## Result

`accept`. Sin reparaciones requeridas. El orquestador persiste `accepted` en `feature_list.json`, crea el
commit local de la feature (incluyendo esta spec y este registro) y `github-flow` publica (push + PR, sin
merge). Queda para la persona la revisión manual de emulador (chip "Recientes" entero y toggle icónico en
claro y oscuro) contra la captura aceptada `favorites/favoritos` y el registro de `DESIGN.md`.
