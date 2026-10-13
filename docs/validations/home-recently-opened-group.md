# Validation Record: home-recently-opened-group

- Feature: `home-recently-opened-group`
- Spec: `docs/specs/home-recently-opened-group.md`
- Issue: #91
- Date: 2026-10-13
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `bash init.sh` → **exit 0**, pero `testDebugUnitTest`/`lint` salieron UP-TO-DATE ⇒ se forzó pasada real.
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**, 34/34 executed (exit 0).
- `bash scripts/gradle.sh lint --rerun-tasks` → **BUILD SUCCESSFUL**, 30/30 executed (exit 0).
- Parseo propio de `TEST-*.xml` → **65 suites / 475 tests / 0 fallos / 0 errores / 0 saltados**;
  `HomeRecipesTest` = 7 tests verdes, ninguno vacío. Reproduce el número del implementer.
- Parseo propio de `lint-results-debug.xml` → **0 errores, 13 warnings** preexistentes
  (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1),
  **0 bajo `app/src/`**.

## Code conformance

- `HomeRecipesGroup` arranca con `if (recipes.isEmpty()) return` (nunca pinta un grupo sin contenido); KDoc.
- `RecipesSection`: `allRecipes = recipes.saved`, sin la llamada a "Abiertas hace poco"; "Guardadas" se
  invoca sin condicionar su no-vacuidad en el call site (el guard vive en el componente). Estado vacío
  global y línea de filtro intactos.
- `HomeRecipes`: único grupo `saved`; `isEmpty`/`filteredBy` sobre `saved`; sin `recentlyOpened`.
- `MainShell`: `HomeRecipes(saved = favorites.map { it.recipe })`.
- Greps: 0 `recentlyOpened` y 0 "Abiertas hace poco" en `app/src`; 1 solo call site de `HomeRecipesGroup`.
- `test -d docs/design/concepts/home-recently-opened-group` → no existe (exención autorizada, #91).

## Scope discipline

- `git status -uall`: solo los ficheros previstos (`HomeRecipes.kt`, `HomeScreen.kt`, `MainShell.kt`,
  `HomeRecipesTest.kt`, `DESIGN.md`, `docs/technical-discovery.md`, `feature_list.json`, `PROGRESS.md`)
  más la spec del planner.
- Sin diff en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `ui.theme`,
  `ui.filters`, `domain/**`, `data/**`, specs aceptadas ni `docs/design/**`. Sin dependencias nuevas.
  Ficheros en UTF-8 sin BOM.

## Documentation / harness

- `DESIGN.md`: Core Screen 2 "solo grupos con contenido" + párrafo de excepción de capturas (#91, tras el
  de #86). `docs/technical-discovery.md`: frase de coherencia. `feature_list.json` `passing` (6
  evidencias); `PROGRESS.md` sesión 085. `init.sh` sin cambios y funcional.

## No commit / no push / no PR

- HEAD `6cbd5b1` == `origin/main` (0/0); rama sin upstream, sin rama remota, `gh pr list` → `[]`.
  Todo el trabajo estaba sin commitear en el worktree al validar.

## Findings

- Sin hallazgos Critical/High/Medium. Low/informativos: el guard Compose no es testeable en JVM (declarado
  así en spec y evidencia); quedan referencias históricas a "Abiertas hace poco" en `CONTEXT.md`,
  `docs/domain-model.md`, `docs/design/stitch*/**`, capturas pendientes de la Home, `docs/backend-proposal.md`
  y specs aceptadas (vocabulario/intención histórica, non-goal declarado; manda `DESIGN.md`); la
  verificación manual de emulador queda pendiente de la persona. Seguridad: **sin hallazgos** (diff solo
  UI Compose local: sin red, sin persistencia, sin input, sin secretos).

## Result

`accept`. El orquestador persiste `accepted` en `feature_list.json`, crea el commit local (incluyendo la
spec y este registro) y `github-flow` publica (push + PR con `Closes #91`, sin merge).
