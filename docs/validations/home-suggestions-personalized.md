# Validación independiente — `home-suggestions-personalized`

- Feature id: `home-suggestions-personalized`
- Spec: `docs/specs/home-suggestions-personalized.md`
- Rama/worktree: `feature/home-suggestions-personalized`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/home-suggestions-personalized`)
- Rol: `validator` (contexto limpio; no implementa ni acepta en ficheros)
- Fecha: 2026-10-13
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 098)
- Veredicto: **`accept`**

## Alcance validado

Correctiva de `area: ui` (issue #110) pedida por la persona: el carrusel "Sugeridas para ti" de la Home
mostraba el Lote tal cual (12 semillas fijas + hasta 6 `ai`), aparentando que el Perfil no se aplicaba.
Tras la feature, `com.sazon.app.ui.home.HomeSuggestions.visible(batchRecipes, seedRecipes, preferences,
taxonomy)` es un pipeline **puro** con `MAX_VISIBLE = 6`: `batchRecipes ?: seedRecipes` → filtro de
seguridad `RecipeValidator.validateRecipe(...) is RecipeValidation.Accepted` → ranking decorado por
`MatchScore.of(...)` (`sortedByDescending`, estable en empates) → `take(6)`, **sin** materializar
`matchScore` (la píldora de `RecipeCard` no se pinta: sin cambio visual). El respaldo de semilla offline
(`null`) pasa por el mismo camino y un Lote resuelto **vacío** (`emptyList()`) sigue vacío.

Exención de capturas autorizada por la persona (misma clase que #77/#85/#86/#91/#98): no se crea
`docs/design/concepts/home-suggestions-personalized/`; el diseño no cambia.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 1 en la 1.ª pasada** (flake de entorno, ver abajo) → **exit 0** al re-ejecutar |
| Tests (forzado) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (5m 28s; 34/34 tareas ejecutadas) |
| Lint (forzado) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (5m 02s; 30/30 tareas ejecutadas); SARIF regenerado |
| Parseo XML propio | `app/build/test-results/testDebugUnitTest/TEST-*.xml` (frescos) | **92 suites / 683 tests / 0 fallos / 0 errores / 0 saltados**; `HomeSuggestionsTest` **13** |
| Parseo SARIF propio | `app/build/reports/lint-results-debug.sarif` | **0 errores**; 13 warnings preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`** |

**Flake de entorno (no bloqueante, informativo):** la primera `bash init.sh` falló con
`java.nio.file.NoSuchFileException: .../test-results/testDebugUnitTest/binary/in-progress-results-generic.bin`
(directorio binario de resultados de Gradle sobre DrvFs `/mnt/c`), **no** por un test. El rerun forzado y
una segunda `init.sh` pasaron. Solo relevante porque `init.sh` es el gate estándar y flaqueó una vez bajo
presión de daemons concurrentes.

### Baseline y delta re-derivados (independientes)

- `git grep -l "@Test" HEAD -- 'app/src/test/**'` → **92 ficheros**.
- `git grep -h "@Test" HEAD -- 'app/src/test/**' | wc -l` → **673 anotaciones**.
- Árbol de trabajo: **683 tests** y `HomeSuggestionsTest` **3 → 13**.
- Delta real: **+10 tests / +0 suites**. La baseline "90 suites / 660 tests" citada en la spec es
  **obsoleta**: es la anterior a `dish-visual-descriptor-wiring` (cuyo propio registro de PROGRESS ya da
  92/673). El implementer registró la baseline correcta.

## Escenarios de la spec (verificación independiente)

1. **Tope ≤ 6** — `MAX_VISIBLE = 6` + `take(MAX_VISIBLE)`. Tests: 8 seguras → exactamente 6 conservando
   el orden de origen; 4 → 4 (sin recorte); catálogo real + `omnivore` → 6. Fixtures **coherentes**
   (`steps` suman `totalMinutes`), así que el filtro deja pasar las 8: **no vacío**. ✔
2. **Orden por `MatchScore` descendente, empates estables** — sort decorado
   (`source.map { it to MatchScore.of(it, preferences, taxonomy) }.sortedByDescending { (_, s) -> s }`,
   score calculado **una vez**; `sortedByDescending` es estable). Tests: el input invertido se reordena;
   el caso de empate asegura que las tres puntúan igual (tamaño de set 1) **y** conserva el orden de
   origen. ✔
3. **Exclusión por dieta / restricción dura (semilla e `ai`), negativo obligatorio** — `vegetarian`
   descarta la semilla con `pollo` **y** la `ai` con `pollo` y conserva la segura, con **control positivo**
   `omnivore` donde las tres aparecen; `gluten` descarta el "Pan" libre (`ingredientId = null`) con su
   control positivo. **No vacío.** ✔
4. **Lote resuelto vacío sigue vacío** — `batchRecipes ?: seedRecipes` con `emptyList()` (≠ `null`) →
   vacío; test explícito distinto del `null → semillas`. ✔
5. **Respaldo de semilla offline filtrado/ordenado/acotado** — catálogo real: `omnivore` → 6; `gluten`
   → no vacío, ≤ 6, ninguna de `SEMILLAS_CON_GLUTEN` y, explícitamente, **no** `seed-pasta-tomate`. El
   validador **re-derivó** el set real de semillas con gluten de `app/src/main/assets/seed-recipes.json`
   (solo `seed-gazpacho-andaluz`, `seed-pasta-tomate`, `seed-tortitas-de-avena`, `seed-tarta-de-manzana`;
   no falta ninguna quinta). ✔
6. **Preferencias blandas penalizan, nunca descartan** — `dislikedIngredientIds` solo afecta a
   `MatchScore` (penalización); ambas aparecen, la penalizada después, y `score(limpia) > score(penalizada)`. ✔
7. **Sin cambio visual / score no materializado** — sin `copy(matchScore = …)`; el test usa `assertSame`
   de la instancia original y comprueba `matchScore == null`. ✔

## Inspección / grep (evidencia)

- **Pureza**: `HomeSuggestions.kt` importa solo `domain.profile/recipe/taxonomy/validation` +
  `ui.results.MatchScore`; **0** imports `android`/`androidx`/Compose. ✔
- **Reutilización**: usa `RecipeValidator.validateRecipe(...)` y `MatchScore.of(...)` (firma
  `(recipe, preferences, taxonomy, userIngredients = emptyList())`; `RecipeValidation.Accepted` existe). ✔
- **Cableado**: `MainShell.kt` con `remember(batch?.recipes, seedRecipes, profile.preferences, taxonomy)`
  envolviendo la llamada; `HomeScreen`/`RecipeCard` intactos. ✔
- **Alcance** (`git diff --name-only`): exactamente los 8 ficheros previstos (`DESIGN.md`, `PROGRESS.md`,
  `MainShell.kt`, `HomeSuggestions.kt`, `HomeSuggestionsTest.kt`, `docs/risks-and-open-questions.md`,
  `docs/technical-discovery.md`, `feature_list.json`) + la spec nueva sin seguimiento. **Sin diff** en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `data/**`,
  `ui/results/**`, `ui/home/HomeScreen.kt`, `RecipeCard.kt`, la Taxonomía, `seed-recipes.json` ni
  `docs/design/**`. ✔
- **Capturas**: `docs/design/concepts/home-suggestions-personalized/` **no** existe; `DESIGN.md` registra
  la excepción (issue #110) y el comportamiento vigente del carrusel (≤ 6 seguras y ordenadas, sin
  píldora), con nota en "Core Screens". ✔
- **Deuda de seguridad de Resultados**: registrada como fila nueva en `docs/risks-and-open-questions.md`
  (con la correctiva recomendada `results-seed-safety-filter`); `ResultsStatus.kt` **no** está en el diff
  (no se "arregló" fuera de alcance). ✔
- **Harness**: `feature_list.json` coherente; `PROGRESS.md` con **una** sesión nueva (098), sin sesiones
  ni evidencias duplicadas. ✔

## Hallazgos

**Critical / High / Medium:** ninguno.

**Low (no bloqueantes):**

- **L1 — resumen "Estado verificado actual" de `PROGRESS.md` desactualizado**: el titular aún terminaba
  con el bloque de `dish-visual-descriptor-wiring` (`WIP = 0`) sin mencionar esta feature (el registro de
  la sesión 098 ya era correcto). `feature_list.json` (fuente de verdad) era coherente. **Corregido por
  el orquestador en la sesión 099** (bullet nuevo con `passing`/`accepted`, 92/683 y la prueba manual
  pendiente).
- **L2 (informativo) — el negativo del escenario 3 usa una fixture semilla sintética** (`seed-pollo`,
  `ingredientId = "pollo"`) en lugar de `seed-pollo-al-ajillo` real; es lo previsto por la spec ("fixtures
  propias") y el catálogo real se ejercita en el escenario 5. Sin acción.
- **L3 (informativo) — flake de `init.sh` en DrvFs** (ver arriba): entorno, no la feature.

## Evaluación de seguridad

Feature de **seguridad**: no añade red, secretos, auth, persistencia ni entrada de usuario; aplica el
`RecipeValidator` ya aceptado (patrón de dieta + restricciones duras) como filtro duro antes de pintar,
**reforzando** el camino de la Home. Sin secretos en el diff, sin dependencias ni llamadas externas
nuevas. La brecha conocida y **deliberadamente diferida** (`ResultsStatus.Unavailable`, modo degradado de
Resultados, sigue sin filtrar) queda registrada como deuda con correctiva recomendada y **fuera** del
alcance de esta feature. Sin hallazgos bloqueantes.

## Estado recomendado

- Veredicto: **`accept`**.
- Orquestador: persistir `home-suggestions-personalized` → `accepted`, crear el commit local
  (`feature-flow`) y publicar vía `github-flow`. La **revisión manual en emulador** (≤ 6 tarjetas acordes
  al Perfil con Lote y con respaldo de semilla, aspecto intacto) queda para la persona tras el merge
  (AGENTS.md no automatiza UI).
- No requiere re-validación.
