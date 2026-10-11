# Validation Record: ai-search

- Feature: `ai-search`
- Spec: `docs/specs/ai-search.md`
- Date: 2026-10-12
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `bash init.sh` (WSL, vía `scripts/gradle.sh`) → exit 0 (`./gradlew test` + `./gradlew lint`, sin
  procesos largos).
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL, 32/32 tareas
  ejecutadas.
- Parseo propio de `app/build/test-results/testDebugUnitTest/*.xml` → **54 suites, 343 tests, 0
  fallos/errores/saltados**; 27 nuevos (`AiSearchServiceTest` 9, `SearchMemoTest` 5,
  `AiSearchOutcomeTest` 6, `AiSearchQueryTest` 4, `AiSearchProgressTest` 3) + 316 preexistentes en
  verde. Los números coinciden exactamente con los del implementer (reproducidos, no aceptados de su
  resumen).
- Parseo propio de `lint-results-debug.xml` → **0 errores, 13 warnings** preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1);
  **0 en ficheros de la feature**.
- Pureza: `com.sazon.app.domain.search` y la lógica pura de `ui.search`
  (`AiSearchQuery`/`AiSearchCopy`/`AiSearchProgress`/`AiSearchOutcome`) sin imports
  `android.*`/`androidx.*`; Compose solo en `AiSearchBar`/`AiSearchFlow`/`AiSearchStates`; sin hex
  sueltos ni iconos.
- Alcance: `git diff --stat` → `ui.results/**` intacto; `domain/**` intacto; `data/**` solo el nuevo
  `NetworkStatusSource.kt`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, la
  Taxonomía y `seed-recipes.json` sin cambios; sin dependencias nuevas.

## Conformance

- La Búsqueda con IA vive en `domain.search` + `ui.search` con lógica pura testeable en JVM y capa
  Compose delgada; `ui.results` **intacto** (solo reutilizado).
- Una consulta que contradice el patrón de dieta devuelve **cero recetas** (verificación exigida):
  cubierta por `consulta_que_contradice_el_patron_de_dieta_devuelve_cero_recetas` (test negativo
  genuino: si la orquestación no validara, fallaría). Una Restricción dura incumplida tampoco llega a
  resultados (`Bocadillo de tomate` → `HARD_RESTRICTION`).
- Estado Bloqueado decidido por el **motivo del descarte** (`DIET_PATTERN`/`HARD_RESTRICTION`) y
  distinguido de "sin resultados" en `AiSearchOutcomeTest`; `SCHEMA`/`INCOHERENT` no produce Bloqueado.
- Fallo: conserva la consulta, ofrece "Reintentar" y no cae a semilla; el memo no memoiza fallos
  (2 llamadas). "Cancelar búsqueda" vuelve con la consulta conservada.
- Memo de sesión (`SearchMemo`, en memoria, clave `trim().lowercase()`): no repite llamada al
  generador para la misma consulta normalizada; una distinta sí llama.
- `onRecipesGenerated` recibe exactamente las recetas `ai` aceptadas.
- Capturas obligatorias presentes en `docs/design/concepts/ai-search/busqueda-ia/`
  (`screen.png`, `code.html`, `DESIGN.md`), nombradas por la spec; `init.sh` sin cambios y ejecuta el
  gate real (repo reiniciable).

## Findings

- Sin hallazgos Critical/High/Medium. Sin acciones de seguridad requeridas (diff acotado, sin
  secretos ni tráfico de red, sin nuevos datos personales persistidos; `ACCESS_NETWORK_STATE` es el
  permiso correcto).
- Low / informativos (no bloqueantes):
  1. **Scenario 2 vs fixture real**: con el `FakeRecipeGenerator` real, un Perfil vegano y sin gluten
     solo descarta "Pollo al ajillo" (las otras tres fixtures son vegetales), así que `accepted` **no**
     queda vacío y el estado real sería Resultados, no Bloqueado. El `Given` literal del Scenario 2 es
     imposible con esa fixture. Se acepta como limitación de la fixture de desarrollo, no de la lógica:
     la verificación exigida se satisface con un test negativo genuino (generador cuya salida es toda de
     dieta contraria), el bloqueo por `DIET_PATTERN`/`HARD_RESTRICTION` se cubre por separado, y la
     inconsistencia está trazada en la spec (*Unknowns*/*Findings*), `PROGRESS.md` y la evidencia. El
     estado Bloqueado no es demostrable en smoke manual con el fake actual; corregirlo exigiría tocar la
     fixture aceptada (fuera de alcance) o una feature correctiva.
  2. `AiSearchProgress.subLines(query)` no usa el parámetro (microcopy estable por diseño; sin
     impacto).
  3. El memo se indexa solo por consulta normalizada (no por Perfil): coincide literalmente con la spec
     §1/§2; no es un defecto.
  4. `AiSearchFlow.LaunchedEffect(query, searchId)` con `result` en `remember` (rotar vuelve a carga):
     mismo criterio que el Generador y declarado como *Unknown* en la spec.

## Result

`accept`. Sin reparaciones requeridas. El orquestador persiste `accepted` en `feature_list.json` y crea
el commit local de la feature (la publicación queda para el orquestador de `github-flow`).
