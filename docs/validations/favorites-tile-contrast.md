# Validación independiente — `favorites-tile-contrast`

- **Feature**: `favorites-tile-contrast` (issue #130, `area: ui`), segunda de la revisión de diseño del
  2026-10-14.
- **Spec**: `docs/specs/favorites-tile-contrast.md`.
- **Rama / worktree**: `feature/favorites-tile-contrast` (`../Default-Project-worktrees/favorites-tile-contrast`),
  desde `origin/main` (`804f009`, merge del PR #133).
- **Veredicto**: **`accept`** (validator independiente, agente con contexto limpio, sesión 117).
- **Estado tras persistir**: `accepted` en `feature_list.json`.

## Qué cambia

En `ui/favorites/FavoriteRecipeCard.kt`, `RecipeMonogramTile` (miniatura 96×96, esquina 24, de la
**lista** de Favoritos) pasa de fondo `scheme.surfaceVariant` + filete terracota de 5 px a **fondo
`scheme.primary`** (`#D9552B`) con el **monograma en `scheme.onPrimary`** (`#FFFFFF`). Se **retira**
`TILE_ACCENT_RULE` (5 px) y su `Box` (sobre `primary` medía 1,00:1, invisible) y se ajusta el import
(`fillMaxHeight` fuera; `width`/`fillMaxWidth` se conservan). Geometría, tipografía (`MONOGRAM_STYLE`
30sp/34sp) y el resto de la fila (título, meta, corazón, swipe, `Surface`) **intactos**; sin tokens
nuevos. Test JVM nuevo `ui/theme/FavoriteTileContrastTest` (4 tests). Docs durables: `DESIGN.md`,
`docs/technical-discovery.md` y `docs/adr/0006` §6.

## Verificación re-ejecutada por el validador

- `bash init.sh` → **exit 0**, pero con `test`/`lint` `UP-TO-DATE`; el validador **no** lo tomó como
  evidencia y forzó:
  - `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL 4m14s, 34/34 tareas**.
  - `bash scripts/gradle.sh lint --rerun-tasks` → **BUILD SUCCESSFUL 4m42s, 30/30 tareas**.
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **98 suites / 737 tests /
  0 fallos / 0 errores / 0 saltados**; `FavoriteTileContrastTest` = 4/0/0/0. Baseline re-derivada en
  `HEAD` (`804f009`): 97 suites / 733 anotaciones `@Test` ⇒ delta **+1 suite / +4 tests**.
- Parseo propio del SARIF `app/build/reports/lint-results-debug.sarif`: **13 resultados, 0 errores, 0 bajo
  `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2,
  `OldTargetApi` 1; todos preexistentes).
- **Ratios WCAG recalculados de forma independiente**: miniatura vs superficie de tarjeta **3,9738**
  claro (`#D9552B` / `#FFFFFF`) y **3,3056** oscuro (`#D9552B` / `#3A2E26`), ≥3,0 (WCAG 1.4.11);
  monograma `onPrimary`/`primary` **3,9738** (≥3,0, texto grande); negativos del fondo anterior
  `surfaceVariant`/claro **1,2088** y `darkSurfaceVariant`/oscuro **1,0445** (<3,0); regla retirada
  `#D9552B`/`#D9552B` **1,0000**. Roles: `Light`/`DarkColorScheme.primary == SazonPrimary`,
  `onPrimary == SazonOnPrimary`, `SazonPrimary != SazonSurfaceVariant`.
- **Alcance/pureza** (greps del validador): `TILE_ACCENT_RULE` en `app/src` → **0**; 0 `surfaceVariant`
  dentro de `RecipeMonogramTile`; 0 hex en `ui/favorites`; `git diff --name-only` acotado a los ficheros
  esperados + `feature_list.json` + la spec; sin diff en `ui/theme/**`, `RecipeMonogram.kt`,
  `FavoritesScreen.kt`, la rejilla, `data/**`, `domain/**`, `assets/**`, build/lockfile, `init.sh`,
  `scripts/**` ni `docs/design/concepts/**`; sin dependencias nuevas.
- **Escenarios 1-7** de la spec confirmados (S3 por inspección, como manda el Verification Plan: el
  `?.let` no pinta `Text` sin monograma).
- **Capturas**: `docs/design/concepts/favorites-tile-contrast/` **no existe** y
  `docs/design/concepts/**` queda limpio (exención autorizada, issue #130, registrada en `DESIGN.md`).
- El implementer **no** hizo commit, push ni PR (`HEAD` en `804f009`, sin rama remota ni PR).

## Hallazgos

Sin hallazgos **Critical/High/Medium** ni de seguridad (el diff es un cambio de dos roles de color, la
retirada de un trazo invisible, un test JVM puro y 3 docs; mejora de accesibilidad: 1,21:1 / 1,04:1 →
3,97:1 / 3,31:1).

- **Low/Info**: `PROGRESS.md` no tenía aún el bloque de sesión (lo escribe el orquestador al persistir
  el `accept`). No bloquea.
- **Low/Info**: `verification[2]` de `feature_list.json` sin reconciliar literalmente; el criterio está
  satisfecho en su forma reconciliada por la exención de #130 y se anotó al persistir el `accept`. No
  bloquea.
- **Low/Info**: Scenario 3 (sin monograma) verificado por inspección, no por test; la spec fija
  exactamente 4 tests y asigna S3 a inspección. No bloquea.
- **Info**: flake transitorio del daemon de Gradle en `lint` (SIGSEGV en G1 GC), sin relación con el
  código; las tiradas forzadas del validador salieron verdes y no quedan restos en el repo.

## Pendiente de la persona

Revisión visual en el emulador tras el merge (no automatizable desde WSL; `AGENTS.md` no automatiza UI):
Favoritos en **lista**, claro y oscuro — la miniatura terracota resalta, el monograma se lee, no hay
filete de 5 px, título/meta/corazón no se han movido y una receta sin iniciales muestra el cuadrado
sólido sin texto. Un defecto visual se corrige con una feature correctiva.
