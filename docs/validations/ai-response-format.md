# Validación independiente — `ai-response-format`

- Fecha: 2026-10-12
- Feature: `ai-response-format` (cuarta correctiva de `real-ai-provider`, issue #74) · `area: integration` (no UI)
- Spec: `docs/specs/ai-response-format.md`
- Rol: `validator` (agente con contexto limpio, skill `feature-validator`)
- Rama / worktree: `feature/ai-response-format`
- Veredicto: **`revise`** (1 hallazgo Medium: el fallback puede producir un **tercer intento HTTP**;
  todo lo demás, conforme)
- **Veredicto final tras reparación (2026-10-12): `accept`** — ver «Re-validación independiente
  (tras reparación)» al final de este documento; la sección original se conserva como historial del
  `revise`.

## Alcance validado (leído en el diff, no del resumen del implementer)

1. **`response_format` en la petición**: `ResponseFormat(type)` + `ChatCompletionRequest.responseFormat`
   con `@SerialName("response_format")` + `@EncodeDefault(EncodeDefault.Mode.NEVER)` y default `null`;
   `sendAttempt(prompt, remaining, includeFormat)` monta `ResponseFormat.JSON_OBJECT` solo si
   `includeFormat = responseFormatEnabled && !usedFallback`.
2. **Interruptor**: `AiProviderPolicy.RESPONSE_FORMAT_ENABLED = true`, `RESPONSE_FORMAT_JSON_OBJECT =
   "json_object"`, `requiresResponseFormatFallback(status) = status == 400 || status == 422`, más la
   costura de constructor `responseFormatEnabled` (default = la constante). `isRetryableStatus`,
   120/150/240, `MAX_ATTEMPTS = 2`, backoff 500 ms y `Retry-After ≤ 5 s` **sin tocar**.
3. **Fallback one-shot**: rama `HttpFailure` → `AiCallOutcome.FALLBACK`, `attempt += 1`, sin backoff,
   `usedFallback = true` permanente (no se reactiva el formato).
4. **Observabilidad**: `AiResponseFormat { JSON_OBJECT, NONE }`, `AiCallOutcome.FALLBACK`,
   `AiCallEvent.responseFormat = NONE` (aditivo con default) y `format=…` en el `Log.d("SazonAi", …)`.
5. **No-`null`**: la prueba negativa usa el **mismo** `Json { ignoreUnknownKeys = true; encodeDefaults =
   true }` que `AiHttpClientFactory` (líneas 37–38), y hay prueba en el cuerpo real del `MockEngine`.

## Verificación re-ejecutada por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); primera pasada **UP-TO-DATE** → forzado |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL**, 34/34 tareas ejecutadas (2m31s) |
| XML (parseo propio) | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **59 suites / 404 tests / 0 fallos / 0 errores / 0 saltados** |
| `data.ai` | mismo XML | **61 tests** (AiProviderPolicyTest 12, AiRecipeIdsTest 6, AiRecipePayloadTest 11, DevExpertRecipeGeneratorTest 30, OpenAiChatTest 2) = 50 previos + 11 nuevos, ninguno eliminado |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL**, 30/30 tareas ejecutadas |
| Lint XML | `app/build/reports/lint-results-debug.xml` | **0 errores**, 13 warnings (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **todos** en `app/build.gradle.kts`/`gradle/libs.versions.toml`/`gradle-wrapper.properties`, **0 bajo `app/src`** |
| Sonda de control del fallback | copia scratch en `/tmp/opencode/rsf-probe1` (repositorio **no** modificado), `testDebugUnitTest --tests …probe_*` | ver Hallazgo 1 (3 llamadas confirmadas) |
| Alcance | `git status --porcelain`, `git diff --stat`, `git diff --quiet HEAD -- <ruta>` | solo lo previsto (14 modificados + 2 nuevos, todos del alcance); sin dependencias nuevas |

## Comprobaciones independientes de los Scenarios 1-8

- **S1** (formato activo ⇒ clave y 1 llamada): `el_formato_activo_envia_response_format_y_una_sola_llamada`
  + sonda propia que parsea el cuerpo del `MockEngine` → `response_format.type = "json_object"`, 1 llamada,
  `SUCCESS`/`JSON_OBJECT`. **OK**.
- **S2** (interruptor apagado): `el_interruptor_apagado_omite_la_clave_y_no_emite_null` (cuerpo sin la
  clave, parseo `null`, `NONE`) y `con_el_interruptor_apagado_un_400_o_422_degrada_en_una_sola_llamada`
  (400 y 422 → 1 llamada, `HTTP_ERROR`). Con `includeFormat = false` la rama de fallback es inalcanzable
  ⇒ comportamiento idéntico al anterior. **OK**.
- **S3** (nunca `"response_format":null` con `encodeDefaults = true`): `OpenAiChatTest` (2 tests) con el
  `Json` idéntico al del cliente; la aserción de `!contains("null")` es gruesa pero la de parseo
  (`responseFormatType(text) == null`) es precisa y cubre el caso. **OK**.
- **S4** (400 **y** 422 → 2 llamadas con/sin formato, `FALLBACK` con estado y `JSON_OBJECT`, `SUCCESS`
  con `NONE`, sin backoff): `el_fallback_ante_400_o_422_reintenta_una_vez_sin_formato` (aserta
  `testScheduler.currentTime` sin avanzar). **OK**.
- **S5** (una sola vez): `el_fallback_es_una_sola_vez_ante_400_o_422_persistente` (400 y 422 persistentes
  → exactamente 2 llamadas, `HTTP_ERROR`). **OK en el caso aislado**; ver Hallazgo 1 para el caso mixto.
- **S6** (solo 400/422 disparan el fallback): `el_fallback_solo_lo_disparan_400_y_422`
  (401/403/404 → 1 llamada sin fallback y con formato en el evento; 429/503 → 2 llamadas **ambas** con
  formato y sin `FALLBACK`) + `AiProviderPolicyTest.el_fallback_de_formato_solo_lo_disparan_400_y_422`
  (200/201/302/401/403/404/408/429/500/502/503/599 → `false`). **OK**.
- **S7** (observabilidad y no-fuga): `los_eventos_distinguen_el_modo_y_el_fallback_sin_fugas` (modo y
  `FALLBACK` visibles; `toString()` sin `test-key`, `Bearer`, `Authorization` ni título). Verificado
  además en el código: `AiCallEvent` solo lleva enums/recuentos/longitudes y el log de `MainActivity`
  añade `format=${event.responseFormat}` sin tocar la clave. **OK**.
- **S8** (regresión): 404/404 en verde con los 50 tests previos de `data.ai` presentes; timeouts 120/150/240,
  `MAX_ATTEMPTS`, backoff y `Retry-After` intactos en el diff. **OK**.
- **S9** (manual en emulador): no automatizable desde WSL; queda como verificación de la persona
  (declarado así en spec, `PROGRESS.md` y `feature_list.json`). **Pendiente por diseño**.

## Hallazgos

### Finding 1 — El fallback puede añadir un **tercer** intento HTTP (contradice `MAX_ATTEMPTS = 2`)

- **Severity: Medium**
- **Evidence**: `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` (rama `HttpFailure`,
  líneas ~144–156): la condición del fallback (`includeFormat && requiresResponseFormatFallback(status) &&
  !usedFallback`) **no** consulta `AiProviderPolicy.canRetry`/`MAX_ATTEMPTS`, y el `attempt += 1` del
  fallback no está acotado. Si el **primer** intento consume el reintento de transporte
  (408/429/5xx o fallo de transporte) y el **segundo** devuelve 400/422, el fallback se toma igualmente
  → tercera petición HTTP.
  Sonda ejecutada de verdad contra el código de la feature (copia scratch `/tmp/opencode/rsf-probe1`, el
  worktree **no** se tocó; `testDebugUnitTest --tests "…probe_*"`, 2 de 3 probes fallan a propósito):
  - `503 → 400`: `calls=3 formats=[json_object, json_object, null]`
    `events=[1:RETRY/JSON_OBJECT, 2:FALLBACK/JSON_OBJECT, 3:SUCCESS/NONE] recipes=1`
  - `IOException → 422`: `calls=3 formats=[json_object, json_object, null] reason=HTTP_ERROR`
  - `400 → 429` (control): `calls=2 formats=[json_object, null] reason=RATE_LIMITED` (correcto, sin tercera)
  Contradice la spec §Technical Approach 3 («el fallback consume el segundo intento (`MAX_ATTEMPTS = 2`)»),
  los Non-Goals («No se cambia la semántica de reintentos de transporte (`MAX_ATTEMPTS = 2`…)»), el KDoc
  de `DevExpertRecipeGenerator` («nunca mas de una por operacion» — es cierto — pero el comentario
  «el fallback consume el segundo intento» es falso en este camino) y las docs durables ya actualizadas
  (`docs/technical-discovery.md` y `docs/risks-and-open-questions.md`: «consumiendo el 2.º intento»).
  `AiProviderPolicy.MAX_ATTEMPTS` documenta «el primero mas **un** reintento»: aquí la operación hace 3.
- **Why it matters**: rompe una invariante de política documentada y una cota de red que el repo
  consideraba dura (coste/latencia extra y rastro ambiguo en logcat: `RETRY` → `FALLBACK` → `SUCCESS`
  con 3 peticiones); el usuario no lo ve (la sonda llegó a devolver recetas), pero una futura sesión no
  puede fiarse de `MAX_ATTEMPTS` ni de las docs tal como están. La secuencia no está cubierta por ningún
  test.
- **Required change**: que el fallback respete el presupuesto de intentos de la operación (o, si la
  persona prefiere mantener la resiliencia, **ampliar la spec explícitamente** y corregir KDoc/docs; se
  deja como alternativa, no como defecto silencioso).
- **Suggested implementation** (opción recomendada, mínima, dentro del alcance):
  1. En la rama de fallback, añadir la guarda del presupuesto, p. ej.
     `val canFallback = includeFormat && AiProviderPolicy.requiresResponseFormatFallback(outcome.status) &&
      AiProviderPolicy.canRetry(attempt, remaining)` (o `attempt < AiProviderPolicy.MAX_ATTEMPTS` si se
     prefiere no exigir margen de backoff, ya que el fallback no espera).
  2. Con la guarda, un 400/422 en el último intento cae en la rama actual → `UNAVAILABLE` con el formato
     del intento (`JSON_OBJECT`) y `HTTP_ERROR`, sin tercera petición.
  3. Añadir el test de regresión que hoy falta en `DevExpertRecipeGeneratorTest`:
     `el_fallback_no_anade_un_tercer_intento` con `MockEngine` que responde `503` (o lanza `IOException`)
     en la 1.ª llamada y `400`/`422` en la 2.ª, asertando `mock.calls == 2`, que no hay `FALLBACK` tras
     el `RETRY` y que el motivo es `HTTP_ERROR`.
  4. Ajustar la KDoc de `DevExpertRecipeGenerator` si el comentario «consume el segundo intento» queda
     impreciso.
  5. Si en su lugar se decide permitir 3 peticiones, actualizar spec + KDoc + `technical-discovery.md` +
     `risks-and-open-questions.md` para declarar que el fallback puede superar `MAX_ATTEMPTS` (acotado por
     el deadline) y cubrirlo con test.
- **Verification after fix**:
  - `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y `bash init.sh` en verde (404 + 1 tests).
  - Nuevo test: `503/IOException → 400/422` ⇒ **exactamente 2** llamadas, sin `FALLBACK` posterior al
    `RETRY`; `400/422` en el primer intento ⇒ sigue habiendo fallback (2 llamadas, la 2.ª sin la clave).

### Finding 2 — Condición redundante en la rama de fallback

- **Severity: Low**
- **Evidence**: `DevExpertRecipeGenerator.kt` ~145–147: `includeFormat` ya implica `!usedFallback`
  (`includeFormat = responseFormatEnabled && !usedFallback`), así que `&& !usedFallback` es código muerto.
- **Why it matters**: sugiere que `usedFallback` protege algo que no protege; con Finding 1 arreglado
  conviene dejar la guarda del presupuesto como única condición adicional.
- **Required change**: simplificar la condición al corregir Finding 1 (sin cambio de comportamiento).

### Finding 3 — Aserción gruesa en `OpenAiChatTest`

- **Severity: Low** (informativo)
- **Evidence**: `OpenAiChatTest.kt:36` `assertFalse(text.contains("null"))` es sensible a cualquier
  `null` del JSON (el implementer ya documentó que con el **prompt** real la aserción ingenua fallaba).
  La aserción precisa (`responseFormatType(text) == null`, línea 38) sí cubre la intención.
- **Required change**: ninguna obligatoria; opcional, acotar la aserción al campo
  (`!text.contains("\"response_format\":null")`).

### Finding 4 — `nunca_reintenta_un_4xx_no_reintentable` acotado a 401/403/404

- **Severity: Low** (informativo)
- **Evidence**: `DevExpertRecipeGeneratorTest.kt:~180` (antes incluía 400/422). No hay pérdida real de
  cobertura: el caso 400/422 con el interruptor **apagado** (1 llamada, `HTTP_ERROR`) sigue cubierto por
  `con_el_interruptor_apagado_un_400_o_422_degrada_en_una_sola_llamada`, y con el interruptor activo por
  los tests del fallback.

## Disciplina de alcance (verificada)

`git status --porcelain` = 14 modificados + 2 nuevos, **todos** del alcance declarado: `AiProviderPolicy.kt`,
`OpenAiChat.kt`, `AiDiagnostics.kt`, `DevExpertRecipeGenerator.kt`, `MainActivity.kt` (solo el log),
`AiProviderPolicyTest.kt`, `DevExpertRecipeGeneratorTest.kt`, `OpenAiChatTest.kt` (nuevo),
`docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `docs/adr/0003-ai-provider.md`,
`docs/specs/real-ai-provider.md`, `docs/specs/ai-call-diagnostics.md`, `PROGRESS.md`, `feature_list.json`
y `docs/specs/ai-response-format.md` (spec del planner, sin trackear). `git diff --quiet HEAD --` confirma
**intactos**: `Recipe.kt`, `RecipeValidator.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`,
`AiHttpClientFactory.kt`, `domain/**`, `ui/**`, `app/src/main/assets/**` (Taxonomía, `seed-recipes.json`),
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
`DESIGN.md` y `docs/design/**`. **Sin dependencias nuevas** y **sin capturas** (no es UI). `grep` de
`response_format` restringido a `data.ai` (código y sus tests) más su KDoc.

## Seguridad (checklist portable, alcance de la feature)

- **Sin secretos**: el diff no añade claves/tokens (solo la clave ficticia `test-key` en tests y el nombre
  `devexpert.apiKey`, no su valor); `local.properties` sigue fuera de control de versiones.
- **Sin fuga de datos**: `AiCallEvent` solo gana un enum de modo; no expone clave, `Authorization`, cuerpos
  ni títulos, y el `Log.d("SazonAi", …)` sigue siendo local (sin telemetría). Probado (S7).
- **Servicio externo**: URL constante de la política (no construida con entrada del usuario), credencial
  fuera de la UI, fallos degradados a `AiUnavailableException` tipada. Única observación: la cota de red
  por operación (Finding 1) — sin impacto de seguridad, sí de coste.
- **Dependencias**: sin cambios en el version catalog ni en el build.
- **No hay hallazgos de seguridad Critical/High.** 

## Documentación y harness

- Docs durables al día y coherentes con el comportamiento **salvo** la frase «consumiendo el 2.º
  intento» (Finding 1) en `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`; la nota de
  ADR 0003, las notas de `real-ai-provider.md`/`ai-call-diagnostics.md` y el párrafo de Observability
  (`responseFormat`/`FALLBACK`, `format=…`) son correctos y no contradictorios.
- `init.sh` **sin cambios** y sigue ejecutando el gate real (test + lint vía `scripts/gradle.sh`, exit 0);
  no arranca procesos largos. `feature_list.json` (`passing` + evidencia) y `PROGRESS.md` (sesión 074)
  reflejan el estado real y el siguiente paso.
- Scenario 9 (manual en emulador, clave real, logcat) queda pendiente de la persona y está declarado como
  no automatizable desde WSL, conforme a la política del repo.

## Implementation Repair Brief

Goal: que el fallback de formato **nunca** añada un tercer intento HTTP (respetar `MAX_ATTEMPTS = 2`),
manteniendo intactos el resto de escenarios y el comportamiento con el interruptor apagado.

Do:
1. En `DevExpertRecipeGenerator.execute`, rama `AttemptOutcome.HttpFailure`: añadir la guarda del
   presupuesto a la condición del fallback
   (`AiProviderPolicy.canRetry(attempt, remaining)` — o `attempt < AiProviderPolicy.MAX_ATTEMPTS`), dejando
   `includeFormat && requiresResponseFormatFallback(status)` como condiciones previas y eliminando el
   `&& !usedFallback` redundante.
2. Si el 400/422 llega sin presupuesto, dejar que caiga en la rama actual (`UNAVAILABLE` + `HTTP_ERROR`,
   `responseFormat` del intento que falló): sin tercera petición.
3. Añadir el test de regresión `el_fallback_no_anade_un_tercer_intento` en `DevExpertRecipeGeneratorTest`
   (503 o `IOException` en la 1.ª llamada y 400/422 en la 2.ª ⇒ `mock.calls == 2`, sin `FALLBACK`, con
   `HTTP_ERROR`), y conservar los tests existentes del fallback aislado (400/422 con 2 llamadas).
4. Si aparecería la opción de mantener 3 peticiones, no hacerlo en silencio: requiere actualizar la spec y
   las dos docs durables.

Do not:
- tocar `Recipe.kt`, `RecipeValidator.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`,
  `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, Room, Taxonomía, `seed-recipes.json`, build,
  dependencias, `init.sh`, `scripts/**`, `DESIGN.md` ni `docs/design/**`;
- cambiar `isRetryableStatus`, los timeouts 120/150/240, `MAX_ATTEMPTS`, el backoff ni `Retry-After`;
- reintroducir `response_format` en la segunda petición del fallback ni serializar `null`.

Run:
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
- `bash init.sh` (exit 0; 405 tests esperados con el nuevo caso)

Update:
- `PROGRESS.md` (nueva evidencia del implementer) y `feature_list.json` (evidencia; el estado lo mueve el
  orquestador).

Verification after fix:
- Los 404 tests previos + el nuevo en verde; `400/422` en el primer intento sigue dando 2 llamadas
  (una con formato, una sin); `503/IOException → 400/422` da exactamente 2 y ningún `FALLBACK`.

## Acción siguiente

Devolver el veredicto **`revise`** al orquestador: **no** persistir `accepted` todavía. El implementer
aplica el Repair Brief; después, re-validación independiente (puede ser focal: el cambio es de una sola
condición + un test). El **orquestador** es quien persiste `accepted` y crea el commit local; esta sesión
**no** ha modificado código, no ha commiteado ni ha hecho push/PR.

---

# Re-validación independiente (tras reparación) — 2026-10-12

- Feature: `ai-response-format` (issue #74) · Spec: `docs/specs/ai-response-format.md`
- Rol: `validator` (contexto limpio, skill `feature-validator`), **independiente** del implementer
- Estado validado: `passing` (sesión 075, reparación del Finding 1) en `feature/ai-response-format`
- **Veredicto: `accept`**

## Qué se reparó (verificado en el diff, no del resumen)

- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` (rama `AttemptOutcome.HttpFailure`):
  la condición del fallback es ahora
  `includeFormat && AiProviderPolicy.requiresResponseFormatFallback(outcome.status) && attempt < AiProviderPolicy.MAX_ATTEMPTS`;
  el `&& !usedFallback` redundante (Finding 2) **se eliminó** (lo implica `includeFormat = responseFormatEnabled && !usedFallback`).
  KDoc del generador precisada.
- Test de regresión nuevo `DevExpertRecipeGeneratorTest.el_fallback_no_anade_un_tercer_intento`
  (`503 → 400` y `IOException → 422`).
- `docs/risks-and-open-questions.md` y `docs/technical-discovery.md` matizadas (el fallback solo consume
  el 2.º intento **si queda presupuesto**; el `400/422` del 2.º intento degrada a `HTTP_ERROR`).
- `ai-call-diagnostics`/`real-ai-provider`/`adr 0003`/`OpenAiChat`/`AiDiagnostics`/`AiProviderPolicy`/
  `MainActivity` **sin cambios nuevos** respecto al `revise` (diff idéntico salvo lo anterior); no se
  tocó ni `isRetryableStatus` ni los timeouts 120/150/240, `MAX_ATTEMPTS`, backoff ni `Retry-After`.

## Gate y verificaciones re-ejecutadas por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); 1.ª pasada **UP-TO-DATE** → forzado |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL**, 34/34 tareas ejecutadas (2m37s) |
| XML (parseo propio) | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **59 suites / 405 tests / 0 fallos / 0 errores / 0 saltados** |
| `data.ai` | mismo XML | **62 tests** (AiProviderPolicyTest 12, AiRecipeIdsTest 6, AiRecipePayloadTest 11, DevExpertRecipeGeneratorTest 31, OpenAiChatTest 2) = 61 previos + 1 nuevo, ninguno eliminado |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL**, 30/30 tareas ejecutadas (2m38s) |
| Lint XML | `app/build/reports/lint-results-debug.xml` | **0 errores**, 13 warnings (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 con localización bajo `app/src`** |
| **Sonda propia del camino mixto** | copia scratch `/tmp/opencode/rsf-probe2` (worktree **no** modificado), `testDebugUnitTest --tests com.sazon.app.data.ai.ValidatorProbeTest` | **7/7 en verde**; salida verbatim abajo |
| Alcance | `git status --porcelain`, `git diff --quiet origin/main -- <ruta>`, `grep -rl response_format` | solo lo previsto; intactos los ficheros no-alcance; sin dependencias nuevas |

### Sonda independiente del camino mixto (hallazgo central)

Test propio (`ValidatorProbeTest`, copia scratch; el worktree no se tocó). Salida literal del `system-out`
del XML:

```
PROBE mixto_503_luego_400        calls=2 formats=[json_object, json_object] events=[1:RETRY/JSON_OBJECT, 2:UNAVAILABLE/JSON_OBJECT] reason=HTTP_ERROR
PROBE mixto_io_luego_422         calls=2 formats=[json_object, json_object] events=[1:RETRY/JSON_OBJECT, 2:UNAVAILABLE/JSON_OBJECT] reason=HTTP_ERROR
PROBE aislado_400_luego_200      calls=2 formats=[json_object, null]          events=[1:FALLBACK/JSON_OBJECT, 2:SUCCESS/NONE] recipes=1
PROBE aislado_422_luego_200      calls=2 formats=[json_object, null]          events=[1:FALLBACK/JSON_OBJECT, 2:SUCCESS/NONE] recipes=1
PROBE persistente_400            calls=2 formats=[json_object, null]          events=[1:FALLBACK/JSON_OBJECT, 2:UNAVAILABLE/NONE]
PROBE fallback_luego_429         calls=2 formats=[json_object, null]          events=[1:FALLBACK/JSON_OBJECT, 2:UNAVAILABLE/NONE] reason=RATE_LIMITED
PROBE transporte_429_persistente calls=2 formats=[json_object, json_object]   events=[1:RETRY/JSON_OBJECT, 2:UNAVAILABLE/JSON_OBJECT] reason=RATE_LIMITED
```

Interpretación (independiente del test del implementer):

- **Camino mixto** `503 → 400` y `IOException → 422`: **exactamente 2** llamadas HTTP, **sin** evento
  `FALLBACK`, evento `RETRY` de transporte presente y degradación final `UNAVAILABLE`/`HTTP_ERROR` con
  `responseFormat = JSON_OBJECT` (el formato del intento que falló). El bug del `revise`
  (`calls=3, formats=[json_object, json_object, null]`) **ya no se reproduce**.
- **Fallback aislado** `400`/`422` en el 1.er intento: **2** llamadas, la 1.ª con `json_object` y la 2.ª
  **sin** la clave, `FALLBACK` + `SUCCESS`, `responseFormat = NONE`. Sin tercera petición.
- **Persistente** `400`: 2 llamadas, `HTTP_ERROR`. **`400 → 429`**: 2 llamadas (el 429 del 2.º intento
  no tiene presupuesto y no dispara nada), `RATE_LIMITED`.
- **Transporte intacto** `429` persistente: 2 llamadas, **ambas** con formato, sin `FALLBACK`,
  `RATE_LIMITED` → `isRetryableStatus`, `MAX_ATTEMPTS`, backoff y `Retry-After` **sin cambios**.

Cota verificada: la operación sigue limitada por `MAX_ATTEMPTS = 2` (`attempt < MAX_ATTEMPTS` es la
guarda que lo garantiza) y por el deadline total de 240 s.

## Estado de los hallazgos previos

- **Finding 1 (Medium) — RESUELTO**: el fallback ya no añade un tercer intento HTTP en el camino mixto
  (sonda propia arriba). La spec («el fallback consume el 2.º intento»), los Non-Goals
  (`MAX_ATTEMPTS = 2`) y las docs durables ya **no** se contradicen.
- **Finding 2 (Low) — RESUELTO**: `&& !usedFallback` eliminado de la condición del fallback.
- **Finding 3 (Low, informativo) — abierto, no bloqueante**: `OpenAiChatTest` conserva
  `assertFalse(text.contains("null"))` (gruesa); la aserción precisa `responseFormatType(text) == null`
  cubre la intención. Opcional acotar a `"\"response_format\":null"`.
- **Finding 4 (Low, informativo) — abierto, no bloqueante**: `nunca_reintenta_un_4xx_no_reintentable`
  sigue acotado a 401/403/404; 400/422 con el interruptor apagado están cubiertos por
  `con_el_interruptor_apagado_un_400_o_422_degrada_en_una_sola_llamada`.

## Regresión de Scenarios 1-8 y reglas aceptadas

- **S1-S7**: presentes y en verde en el XML (nombres verificados uno a uno:
  `el_formato_activo_envia_response_format_y_una_sola_llamada`, `el_interruptor_apagado_omite_la_clave_y_no_emite_null`,
  `con_el_interruptor_apagado_un_400_o_422_degrada_en_una_sola_llamada`,
  `el_fallback_ante_400_o_422_reintenta_una_vez_sin_formato`, `el_fallback_es_una_sola_vez_ante_400_o_422_persistente`,
  `el_fallback_no_anade_un_tercer_intento`, `el_fallback_solo_lo_disparan_400_y_422`,
  `los_eventos_distinguen_el_modo_y_el_fallback_sin_fugas`; `dos_fallos_reintentables_lanzan_tras_dos_intentos_sin_tercero`
  también sigue verde).
- **S8**: 405/405 tests en verde; timeouts 120/150/240, `MAX_ATTEMPTS`, backoff y `Retry-After` sin
  cambios en el diff de `AiProviderPolicy.kt` (solo añade `RESPONSE_FORMAT_ENABLED`,
  `RESPONSE_FORMAT_JSON_OBJECT` y `requiresResponseFormatFallback`).
- **S9** (manual en emulador): sigue pendiente por diseño (no automatizable desde WSL); declarada así en
  spec, `PROGRESS.md` y `feature_list.json`.

## Disciplina de alcance (verificada)

`git status --porcelain` = 14 modificados + 3 nuevos (`OpenAiChatTest.kt`, la spec del planner y este
documento), **todos** del alcance. `git diff --quiet origin/main --` confirma **intactos**:
`Recipe.kt`, `RecipeValidator.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`,
`domain/**`, `ui/**`, `app/src/main/assets/**` (Taxonomía, `seed-recipes.json`), `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md` y
`docs/design/**` (0 rutas cambiadas en esos árboles). `grep -rl response_format` en `app/src` solo
aparece en `data.ai` (código y sus tests); sin dependencias nuevas y sin capturas (no es UI).

## Seguridad (checklist portable, alcance de la feature)

- **Sin secretos**: el diff no añade claves/tokens (solo la clave ficticia `test-key` en tests y el
  nombre `devexpert.apiKey`, no su valor); `local.properties` fuera de control de versiones.
- **Sin fuga de datos**: `AiCallEvent` solo gana un enum de modo; no expone clave, `Authorization`,
  cuerpos ni títulos; el `Log.d("SazonAi", …)` añade `format=…` y sigue siendo local (sin telemetría).
  Probado por `los_eventos_distinguen_el_modo_y_el_fallback_sin_fugas`.
- **Servicio externo**: URL constante de la política, credencial fuera de la UI, fallos degradados a
  `AiUnavailableException` tipada; la cota de red por operación (Finding 1) ahora **respeta
  `MAX_ATTEMPTS`** → menos coste, sin cambio de seguridad.
- **Dependencias**: sin cambios en el version catalog ni en el build.
- **Sin hallazgos Critical/High.** Los Findings 3/4 son informativos, sin impacto de seguridad.

## Documentación y harness

- Docs durables coherentes con el comportamiento tras la matización (el fallback solo consume el 2.º
  intento si queda presupuesto). Nota de ADR 0003, notas de `real-ai-provider.md`/`ai-call-diagnostics.md`
  y Observability (`responseFormat`/`FALLBACK`, `format=…`) correctas.
- `init.sh` **sin cambios** y ejecuta el gate real (test + lint, exit 0), sin procesos largos.
- `feature_list.json` sigue `passing` con evidencia de la reparación (incluida la nueva entrada
  `REPARACION …`), coherente con el estado real. `PROGRESS.md` registra la sesión 075 y deja como
  siguiente paso la re-validación (esta sesión).

## Veredicto y acción siguiente

**`accept`.** El único hallazgo que bloqueaba (`Finding 1`, Medium) está resuelto y **reproducido de
forma independiente**: el camino mixto hace exactamente 2 llamadas sin `FALLBACK`, el fallback aislado
`400/422` sigue haciendo 2 llamadas con `FALLBACK`+`SUCCESS`, y no hay bucle ni tercera petición
(acotado por `canRetry`/`MAX_ATTEMPTS`). Gate verde (405 tests / 0 fallos, lint 0 errores), alcance
disciplinado y docs/harness coherentes.

Acción del **orquestador** (esta sesión **no** la ejecuta): persistir `accepted` en `feature_list.json`,
crear el commit local de `feature/<id>` y publicar por `github-flow`. Queda la verificación **manual**
de emulador (Scenario 9) a cargo de la persona y, como de costumbre, no es bloqueante para el flujo
autónomo. Los Findings 3/4 quedan como pulido informativo, sin bloquear.
