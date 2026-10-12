# Validación independiente — `ai-call-diagnostics`

- Fecha: 2026-10-12
- Feature: `ai-call-diagnostics` (correctiva de `real-ai-provider`, issue #72) · `area: integration` (no UI)
- Spec: `docs/specs/ai-call-diagnostics.md`
- Rol: `validator` (agente con contexto limpio, skill `feature-validator`)
- Rama / worktree: `feature/ai-call-diagnostics`
- Veredicto: **`accept`**

## Alcance validado

1. **Recalibración de `AiProviderPolicy`** (único sitio de la política): `SOCKET_TIMEOUT_MILLIS`
   90 s → **120 s**, `REQUEST_TIMEOUT_MILLIS` 120 s → **150 s**, `TOTAL_DEADLINE_MILLIS` 180 s → **240 s**;
   `CONNECT_TIMEOUT_MILLIS` (10 s), `MAX_ATTEMPTS` (2), `RETRY_BACKOFF_MILLIS` (500 ms) y
   `RETRY_AFTER_MAX_MILLIS` (5 s) **sin cambios**, igual que la lógica de
   `attemptRequestTimeoutMillis` (`min(150 s, restante)`) y `canRetry`.
2. **Diagnóstico de `EMPTY`**: `AiDecodeResult`/`AiContentState` nuevos; `AiRecipePayload.decode`
   devuelve el resultado con `accepted`, `received`, `discardedByReason` (motivos `DiscardReason`, en
   orden del enum, solo los presentes) y longitudes `contentChars`/`contentBytes`; `AiCallEvent` gana 5
   campos aditivos con default y `report(...)` los rellena; `MainActivity` loguea
   `content=…c/…b received=… accepted=… discards={…}`. **Nunca** clave, `Authorization`, cuerpos ni
   títulos.

Fuera de alcance confirmado fuera: **no** se añade `response_format`/structured outputs.

## Verificación re-ejecutada por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | exit 0 (test + lint vía `scripts/gradle.sh`; primera pasada `UP-TO-DATE`) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL, 34/34 tareas |
| Tirada focal | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` | BUILD SUCCESSFUL, **50 tests / 0 fallos** |
| XML (parseo propio) | `app/build/test-results/testDebugUnitTest/*.xml` | **58 suites / 393 tests / 0 fallos / 0 errores / 0 saltados** (386 previos + 7 nuevos, ninguno eliminado) |
| `data.ai` | mismo XML | 50 tests: `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 23 |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | BUILD SUCCESSFUL, 30/30 tareas |
| Lint XML | `app/build/reports/lint-results-debug.xml` | **0 errores**, 10 warnings, **todos** en `app/build.gradle.kts`/`gradle/libs.versions.toml`, **0 bajo `app/src`** |

## Comprobaciones adicionales

- **Regresión real reproducida de forma independiente** (aritmética, sin dejar el repo modificado): con
  los valores viejos, `90_000 > 115_800` = falso, `90_000 ≥ 2 × 46_600` = falso y
  `180_000 − 115_800 − 500 ≥ 115_800` = falso; con 120/150/240 las tres son ciertas. Discriminador
  viejo-vs-nuevo genuino.
- **Escenarios 1-6** de la spec cubiertos (holgura con peor caso 115,8 s y frontera del recorte; `EMPTY`
  por vacío/ilegible con `received=0`/`accepted=0` y longitudes `0/0` vs `>0`; lote parcial
  `received=4 accepted=1 {SCHEMA:2, INCOHERENT:1}`; los cuatro `DiscardReason` representables con
  restricciones no neutras; todas descartadas → `EMPTY` con motivos sin lanzar; no-fuga de
  clave/cabecera/cuerpos/títulos). **Scenario 7** (emulador) queda como verificación manual de la
  persona, no automatizable desde WSL.
- **Disciplina de alcance**: solo `data.ai` (código + tests), el `Log.d` de `MainActivity`, las 4 docs y
  el harness; `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`,
  `AiHttpClientFactory.kt` (lee las constantes → sin literales duplicados), `OpenAiChat.kt`, `domain/**`,
  `ui/**`, Room, Taxonomía, `seed-recipes.json`/assets, build/version catalog, `AndroidManifest.xml`,
  `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` **intactos**; sin dependencias nuevas; sin
  capturas (no es UI).
- **Seguridad**: sin secretos en el diff (solo la clave ficticia `test-key`; `local.properties`
  ignorado); el `AiCallEvent` no expone clave/`Authorization`/cuerpos/títulos; entrada no confiable
  (salida del modelo) tratada tolerante sin lanzar.

## Hallazgos

- **Critical / High / Medium: ninguno.**
- **Low 1** — `contentState` (BLANK vs ILLEGIBLE) no viaja en el evento; la distinción se apoya en las
  longitudes (`0/0` vs `>0`). Cumple lo pedido (la spec solo exige longitudes). No bloquea.
- **Low 2** — el número de warnings de lint oscila con su caché de red (`GradleDependency`/
  `NewerVersionAvailable`); **0 bajo `app/src`** y sin warnings nuevos en ficheros de la feature.
- **Low 3 (informativo)** — `AiRecipePayload.decode` pasó de `public` a `internal` (junto con
  `AiDecodeResult`/`AiContentState`) para no exponer un tipo `internal` desde una API pública; sin
  llamadores fuera del módulo. Documentado en la spec.

## Acción siguiente

El **orquestador** persiste el `accept` (estado `accepted` en `feature_list.json`, evidencia y registro
en `PROGRESS.md`) y crea el **commit local** de `feature/ai-call-diagnostics`. La **publicación**
(push + PR) la hace `github-flow`; esta sesión **no** hace push ni PR.
