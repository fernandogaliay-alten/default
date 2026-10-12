# Validación independiente — `ai-verbose-diagnostics`

- Fecha: 2026-10-12
- Feature: `ai-verbose-diagnostics` (correctiva de `ai-call-diagnostics`) · `area: integration` (no UI)
- Spec: `docs/specs/ai-verbose-diagnostics.md`
- Rol: `validator` (agente con contexto limpio, skill `feature-validator`), **independiente** del implementer
- Rama / worktree: `feature/ai-verbose-diagnostics`
- Estado validado: `passing` (sesión 077, implementer) — sin commit
- Dependencia: `ai-call-diagnostics` (`accepted`, sesión 073) — satisfecha
- **Veredicto: `accept`**

## Alcance validado (leído en el diff, no del resumen del implementer)

1. **Desglose de `INCOHERENT` (siempre activo, A+B)**: `AiIncoherentCause { TIME_DRIFT,
   ALLERGEN_COVERAGE }` (vocabulario de diagnóstico en `data.ai`, **no** un valor nuevo de
   `DiscardReason`) y `AiCallEvent.incoherentByCause`. `AiRecipePayload.decode` clasifica cada descarte
   `INCOHERENT` recomputando **solo** la regla 4a con `RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES`
   (si no es 4a es 4b: el Validador devuelve el primer fallo y evalúa 4a antes que 4b).
2. **Modo depuración (D, ocultable)**: `DevExpertRecipeGenerator` gana
   `verboseDiagnostics: Boolean = false`; `decode(verbose)` rellena `AiDecodeResult.discards`
   (`AiDiscardDetail`: motivo, causa y título del draft o del primitivo `title` del JSON bruto) solo en
   verbose; el generador añade `rawContent = outcome.content` solo en verbose. `MainActivity` usa
   `verboseDiagnostics = BuildConfig.DEBUG` (sin tocar `app/build.gradle.kts`) y añade `incoherent={…}`
   (solo si hay causas) más las líneas verbosas.
3. **Helper puro** `AiVerboseDiagnostics`: `MAX_CHUNK_CHARS = 3_000`, `rawLines` (trocea el `content` sin
   cortar pares suplentes; `null`/vacío → vacío) y `discardLines` (motivo/causa/título con newlines
   saneados; `(sin titulo)` si falta).
4. **No-fuga**: en ningún modo se registra la clave ni `Authorization`; con el interruptor apagado no hay
   títulos ni cuerpo crudo.

## Verificación re-ejecutada por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`), `BUILD SUCCESSFUL` |
| Tests focal (forzados) | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*" --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 2m31s`, **34/34 tareas** (no `UP-TO-DATE`) |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 2m37s`, 30/30 tareas |
| XML (parseo propio) | `app/build/test-results/**/*.xml` (60 del gate; 6 del rerun focal) | Gate: **60 suites / 419 tests / 0 fallos / 0 errores / 0 saltados**. Rerun focal fresco: **76/76** en `data.ai`, 0 fallos |
| Lint XML | `app/build/reports/lint-results-debug.xml` | **0 errores**, 13 warnings (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1) — preexistentes, **0 bajo `app/src`** |
| Warnings de compilación | grep en el log de build | **ninguno** en ficheros de la feature |
| Alcance | `git diff --stat`, `git status --porcelain`, `git diff --quiet -- <ruta>` | solo lo previsto; ficheros prohibidos **UNCHANGED** |

## Comprobaciones independientes de los Scenarios 1-7

- **S1** (desglose 4a/4b): `AiRecipePayload.decode` cuenta `incoherentByCause` por causa;
  `AiCallEvent.incoherentByCause` la propaga y `MainActivity` añade `incoherent={…}` solo si hay causas.
  `el_resumen_distingue_las_dos_causas_de_incoherent` y
  `el_evento_distingue_las_dos_causas_de_incoherent_sin_depuracion` pasan en rerun fresco. **OK**.
- **S2** (coherencia con el Validador): el clasificador recomputa **solo** la 4a; como el Validador evalúa
  4a antes que 4b y devuelve el primer fallo, «no 4a ⇒ 4b» es correcto.
  `la_clasificacion_de_las_causas_de_incoherent_es_coherente_con_el_validador` contrasta (solo 4a /
  solo 4b / ambas / ninguna) contra `RecipeValidator.validateRecipe`. Confirmé que `leche` tiene
  `allergens:["lactose"]` en `taxonomy.json` (la 4b es real, no vacua) y que la deriva de la 4a del test
  es 35 > 5. **OK**.
- **S3** (apagado ⇒ sin detalle ni crudo): `verboseDiagnostics=false` por defecto; `discards` vacío y
  `rawContent` null; `toString()` sin títulos ni cuerpo. Sin `INCOHERENT`, `causes=""` y la línea
  `SazonAi` es la concatenación byte a byte actual (`discards={…} ` + `detail`) y los dos `Log.d` extra
  no emiten nada (listas vacías). Verificado por inspección del formato (orden de campos conservado).
  **OK**.
- **S4** (activado ⇒ detalle + crudo troceado): `rawContent == outcome.content` (el
  `choices[0].message.content`, no el envelope); `AiVerboseDiagnostics.rawLines/discardLines` puros;
  `joinToString("")` reconstruye; cada línea ≤ `MAX_CHUNK_CHARS` + prefijo; no parte pares suplentes.
  Tests pasan. **OK**.
- **S5** (no-fuga): `ningun_evento_ni_linea_expone_la_clave_ni_authorization_en_ningun_modo` recorre
  ambos modos. Grep confirma que `apiKey` solo se usa en el check de vacío y en
  `header(Authorization, "Bearer $apiKey")`; nunca se loguea. **OK**.
- **S6** (regresión): los 62 tests previos de `data.ai` siguen (ninguno eliminado; 76 totales = 62 + 14).
  Cambios aditivos con default; los tipos cambiados (`AiCallEvent`, `AiDecodeResult`) **no** se
  referencian fuera de `data.ai` (grep: 0 referencias externas). **OK**.
- **S7** (manual en emulador): no automatizable desde WSL; queda registrado como PENDIENTE en
  `feature_list.json` y `PROGRESS.md`. **Pendiente por diseño**.

## Hallazgos

**Critical / High / Medium: ninguno.**

**Low / informativos (no bloquean la aceptación):**

1. *La igualdad byte a byte de la línea base `SazonAi` y el prefijo `incoherent=` no están cubiertos por
   test automatizado* (viven en `MainActivity`, acoplada a Android). `grep -rn "incoherent="` solo
   aparece en `MainActivity.kt`. Mitigación: formato de concatenación trivial, el helper puro sí está
   probado y la spec difiere esta parte a la verificación manual. **No requiere cambio.**
2. *`AiVerboseDiagnostics.chunks` con `maxChars == 1`* podría partir un par suplente (la guarda
   `end > start + 1` impide el retroceso). Solo alcanzable con un caller que pase `maxChars = 1`;
   producción usa 3000 y los tests cubren `maxChars = 4`. **No requiere cambio.**
3. *`docs/validations/ai-verbose-diagnostics.md` no existía* al validar (convención del repo). Lo ha
   persistido el orquestador junto con el cambio de estado. **Resuelto.**

## Disciplina de alcance (verificada)

`git status --porcelain` = 9 modificados + 2 nuevos de código/test + spec, **todos** del alcance:
`app/src/main/java/com/sazon/app/data/ai/{AiDiagnostics,AiDecodeResult,AiRecipePayload,DevExpertRecipeGenerator}.kt`,
`app/src/main/java/com/sazon/app/data/ai/AiVerboseDiagnostics.kt` (nuevo),
`app/src/main/java/com/sazon/app/MainActivity.kt` (solo el log/wiring),
`app/src/test/java/com/sazon/app/data/ai/{AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt`,
`app/src/test/java/com/sazon/app/data/ai/AiVerboseDiagnosticsTest.kt` (nuevo),
`docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `docs/adr/0003-ai-provider.md`,
`feature_list.json`, `PROGRESS.md` y `docs/specs/ai-verbose-diagnostics.md` (spec del planner).
`git diff --quiet` confirma **intactos**: `Recipe.kt`, `RecipeValidator.kt`
(`DiscardReason`/`RecipeValidation`/`DiscardedRecipe`), `AiRecipeDraft.kt`, `AiRecipeIds.kt`,
`AiProviderPolicy.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`, `ui/**`,
`data/generator/**`, Room, `app/src/main/assets/**` (Taxonomía, `seed-recipes.json`),
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
`DESIGN.md` y `docs/design/**`. **Sin dependencias nuevas** y **sin capturas** (no es UI).

## Seguridad (checklist acotada a la feature)

- **Sin secretos**: el diff no añade claves/tokens (solo la clave ficticia `test-key` en tests y el
  nombre `devexpert.apiKey`, no su valor); `local.properties` sigue fuera de control de versiones; no
  hay `buildConfigField` nuevo.
- **Clave / `Authorization`**: verificado por código (única aparición de `apiKey` es el header y el check
  de vacío) y por test en los dos modos; ninguna línea ni `toString()` contiene
  `test-key`/`Bearer`/`Authorization`.
- **Exposición de datos**: en modo verbose sí se registran el `content` del modelo y los títulos —
  **exactamente lo que pide la spec**, **apagado por defecto**, activo **solo en debug** vía
  `BuildConfig.DEBUG` (release-safe, sin persistencia) y trazado como **deuda temporal** con criterio de
  retirada en `docs/risks-and-open-questions.md` y nota en `docs/adr/0003-ai-provider.md`.
- **Servicios externos / dependencias / boundary**: sin cambios (no se tocan `AiProviderPolicy`,
  `AiHttpClientFactory`, `OpenAiChat`, política de reintentos ni `response_format`); sin dependencias
  nuevas.
- **Sin hallazgos de seguridad Critical/High.** El riesgo residual (volumen de log / contenido en debug)
  es intencional, acotado y documentado.

## Documentación y harness

- Docs durables actualizadas según la spec: `docs/technical-discovery.md` (párrafo `data.ai` + Testing +
  Observability con `incoherent={…}` y el modo verboso debug-only), `docs/risks-and-open-questions.md`
  (desglose 4a/4b y fila de deuda temporal con criterio de retirada y coste) y nota en
  `docs/adr/0003-ai-provider.md`. No se tocó `AGENTS.md`, `DESIGN.md`, `docs/design/**`,
  `docs/domain-model.md` ni `docs/domain/taxonomy.md` (correcto, sin impacto).
- No es feature de UI (`area: integration`): sin capturas de `docs/design/concepts/**`, correcto.
- `init.sh` **sin cambios** y sigue siendo el gate real (test + lint vía `scripts/gradle.sh`, exit 0; sin
  procesos largos). `feature_list.json` coherente (`passing` + evidencia) y `PROGRESS.md` con la sesión
  077, reflejo del estado real.

## Veredicto y acción siguiente

**`accept`.** Los Scenarios 1-6 están implementados, cubiertos por test y verificados de forma
independiente; el gate está en verde (419 tests / 0 fallos, lint 0 errores) y el alcance está
disciplinado. Scenario 7 (manual en emulador) queda pendiente por diseño.

Acción del **orquestador** (esta sesión **no** la ejecuta): persistir `accepted` en `feature_list.json`
(hecho), crear el commit local de `feature/ai-verbose-diagnostics` y dejar la publicación (push + PR, sin
merge) para `github-flow`. Queda la verificación **manual** de emulador (Scenario 7) a cargo de la
persona, no bloqueante para el flujo autónomo. Los 2 Low informativos quedan como pulido opcional.
