# Feature Implementation Spec: Diagnóstico de las llamadas a la IA y timeouts para la latencia real

## Source Feature

- `id`: `ai-call-diagnostics` · `area`: `integration` · `depends_on`: `["ai-recipe-decode"]`
  (`accepted`, sesión 071) · `status`: `not_started` · `source`: `feature_list.json`
- **No es feature de UI**: `area: integration`; sin pantallas, estados, microcopy ni capturas. No usa
  `docs/design/concepts/` y **no** se bloquea por Stitch.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) `./gradlew test` cubre la política de timeouts **recalibrada**
  contra la latencia real medida y el **evento de diagnóstico** con/sin recetas descartadas (motivo
  incluido), **sin exponer la clave ni los cuerpos completos**; (2) prueba **manual** en emulador: con
  clave válida, logcat (`SazonAi`) detalla cada llamada y las recetas reales llegan; con un lote sin
  recetas aceptadas, el log indica el motivo.

**Contexto (correctiva de `real-ai-provider`, issue #72).** El proveedor real ya está conectado y
decodifica la forma real del contrato (§4), pero (a) la política de timeouts se calibró contra **una**
medición (28,0 s) mientras la latencia real medida con la misma clave y el mismo prompt de 6 recetas
varía entre modelos/modos —`chat` 28,0 / 33,1 / 42,1 / 46,6 s, `chat`+`json_object` 85,3 s,
`chat-pro`+`json_schema` 97,0 s, `deepseek-v4-flash` 52,6 s y `mimo-v2.6-pro` **115,8 s** (peor caso)—
y (b) cuando una llamada termina `EMPTY` el logcat no explica por qué: no hay longitudes del contenido
del modelo, ni recuento de recetas recibidas vs. aceptadas, ni el **motivo** del descarte
(`SCHEMA`/`DIETA`/`RESTRICCION`/`INCOHERENTE`), que hoy se pierde porque
`AiRecipePayload.decode` devuelve solo `List<Recipe>`.

## Goal

Dos cambios acotados, ambos en `com.sazon.app.data.ai` y sus tests:

1. **Recalibrar** `AiProviderPolicy` a la latencia real con evidencia de **varios** modelos
   (`SOCKET 90 s → 120 s`, `REQUEST 120 s → 150 s`, `TOTAL_DEADLINE 180 s → 240 s`; `CONNECT`,
   `MAX_ATTEMPTS`, backoff, `Retry-After` y la semántica de reintentos **sin cambios**), y recalibrar
   los tests de holgura como **prueba de regresión** (deben fallar con los valores viejos).
2. **Enriquecer el diagnóstico local**: `AiRecipePayload.decode` deja de perder el motivo del descarte
   (devuelve un **resultado de decode** con contadores y resumen de motivos) y `AiCallEvent` + el
   `Log.d("SazonAi", …)` de `MainActivity` pasan a explicar un `EMPTY`/descarte con **bytes y
   caracteres** del contenido crudo del modelo, **recetas recibidas vs. aceptadas** y el **resumen de
   motivos** de descarte. El evento por la costura aditiva `onEvent` **nunca** contiene la clave, la
   cabecera `Authorization` ni los cuerpos: solo longitudes y motivos/categorías.

No cambia el puerto `RecipeGenerator`, ni el dominio aceptado (`Recipe`, `RecipeValidator`), ni la UI,
ni el comportamiento observable salvo que un `EMPTY` pasa a ser diagnosticable.

## Non-Goals

- **No** se añade `response_format` / structured outputs / `json_schema` / `json_object`: decisión
  pendiente de la persona, fuera de alcance. (**Resuelto** por `ai-response-format`, issue #74:
  `json_object` se envía con fallback one-shot; `json_schema` sigue fuera.)
- **No** se cambia de proveedor ni de modelo (`chat` sigue siendo el alias diario).
- **No** se toca el dominio aceptado: `RecipeValidator`, `Recipe`, el puerto `RecipeGenerator`, ni la
  UI, ni las bases Room, ni la Taxonomía, ni `seed-recipes.json`, ni el build/dependencias, ni
  `init.sh`/`scripts/**`.
- **No** se cambia la semántica de reintentos (`MAX_ATTEMPTS = 2`, backoff 500 ms, `Retry-After` ≤ 5 s,
  reintentables 408/429/5xx, nunca 4xx): esta feature ajusta **valores de timeout** y **diagnóstico**.
- **No** se registra ni se muestra la clave, la cabecera `Authorization`, la petición ni la respuesta
  completa; tampoco se persiste ni se envía telemetría (el evento sigue siendo local y de solo lectura
  en Logcat).
- **No** se registran títulos ni `detail` de recetas descartadas: solo motivos/categorías y recuentos.
- **No** se reescriben las specs históricas `real-ai-provider.md`/`ai-provider-timeouts.md` (solo se
  anotan sus *Unknown* para que no queden valores viejos).

## Job Story

Como desarrollador que depura con clave real en Android Studio,
quiero que logcat diga, para cada llamada a la IA, cuánto contenido devolvió el modelo, cuántas recetas
llegaron y cuántas se aceptaron, y por qué motivo se descartaron,
para distinguir si el problema es el prompt, el modelo o el modo/Perfil, sin exponer la clave ni los
cuerpos y sin esperar a reproducir a ciegas.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia nada de lo que ve; sigue
  pudiendo cancelar la carga. Con latencia peor, la operación espera hasta el deadline antes de
  degradar, igual que antes pero con un techo mayor.
- **Desarrollador / agente**: verifica en **JVM** con `MockEngine` y reloj inyectado: valores e
  invariantes de la política, frontera del recorte, y el evento con `EMPTY`/descarte y sin fugas.
  Nunca usa red real ni clave real (clave ficticia `test-key`).
- **Sistema**: conserva `AiUnavailableException` tipada → modo semilla y el deadline total acotando la
  operación completa; el puerto `RecipeGenerator` no cambia.

## Acceptance Scenarios

### Scenario 1: la política admite la latencia real de varios modelos (JVM, puro)

Given el peor caso medido `WORST_LATENCY_MILLIS = 115_800L` (`mimo-v2.6-pro`) y el peor del alias
diario `CHAT_WORST_LATENCY_MILLIS = 46_600L`,
When se comprueban los valores, el recorte por intento y las invariantes,
Then `SOCKET_TIMEOUT_MILLIS (120 s) > WORST_LATENCY`, `SOCKET_TIMEOUT_MILLIS ≥ 2 × CHAT_WORST`,
`REQUEST_TIMEOUT_MILLIS (150 s) ≥ SOCKET_TIMEOUT_MILLIS`, `attemptRequestTimeoutMillis(TOTAL_DEADLINE) ==
REQUEST_TIMEOUT_MILLIS`, `TOTAL_DEADLINE_MILLIS (240 s) > REQUEST_TIMEOUT_MILLIS + RETRY_BACKOFF_MILLIS`,
`TOTAL_DEADLINE − WORST_LATENCY − RETRY_BACKOFF ≥ WORST_LATENCY` (cabe otro intento igualmente lento),
`canRetry(1, TOTAL_DEADLINE − WORST_LATENCY − RETRY_BACKOFF)` cierto, `CONNECT_TIMEOUT_MILLIS ==
10_000L` y `REQUEST ≥ CONNECT`. **Con los valores viejos (90/120/180)** fallan al menos
`SOCKET > WORST`, `SOCKET ≥ 2 × CHAT_WORST` y el margen de reintento: es prueba de regresión.
Frontera del recorte: `(TOTAL_DEADLINE) == REQUEST`, `(REQUEST + 1) == REQUEST`,
`(REQUEST − 1) == REQUEST − 1`, `(0) == 0`, `(negativo) == 0`.

### Scenario 2: `EMPTY` por contenido vacío o ilegible explicado por el evento

Given un `MockEngine` que responde `200` con `choices[0].message.content` vacío (`""` / en blanco) o con
texto no-JSON (`"lo siento, no puedo ayudarte"`),
When se invoca una operación del proveedor con `onEvent` capturado,
Then el último `AiCallEvent` es `outcome = EMPTY` con `received = 0`, `accepted = 0`,
`discardedByReason` vacío, y con `contentChars`/`contentBytes` coherentes con el contenido
(`0/0` si está en blanco; `> 0` si es texto ilegible). Es **una sola** llamada, sin reintento.

### Scenario 3: lote con N recibidas, algunas descartadas, y resumen de motivos en el evento

Given un `MockEngine` que responde `200` con un lote de recetas del contrato §4 donde algunas caen por
`SCHEMA` (campos obligatorios ausentes / ids de Taxonomía inexistentes), otras por `INCOHERENT`
(deriva de tiempos > 5 min o `declaredAllergens` incompletos) y una es válida,
When se invoca la operación,
Then el evento es `outcome = SUCCESS` con `received = N`, `accepted = 1` y
`discardedByReason = {SCHEMA: a, INCOHERENT: b}` (solo motivos presentes). Además, un test del
resultado de decode con restricciones **no neutras** (`RecipeConstraints(dietPatternId = "vegan",
hardRestrictionIds = setOf("gluten"))`) prueba que el resumen también expresa `DIET_PATTERN` y
`HARD_RESTRICTION`: los **cuatro** `DiscardReason` son representables.

### Scenario 4: todas descartadas ⇒ `EMPTY` con motivos

Given un `MockEngine` que responde `200` con un lote en el que **ninguna** receta sobrevive al
Validador,
When se invoca la operación,
Then el evento es `outcome = EMPTY` con `received = N > 0`, `accepted = 0` y `discardedByReason` no
vacío (el motivo explica el `EMPTY`), sin lanzar.

### Scenario 5: el evento nunca expone la clave ni los cuerpos

Given cualquier evento emitido (clave ficticia `test-key` y cuerpos con texto reconocible),
When se serializan,
Then `toString()` de **ningún** evento contiene `test-key`, `Bearer`/`Authorization`, el texto del
`content` del modelo ni el título de las recetas: solo operación, desenlace, intento, estado HTTP,
duración, longitudes y motivos/categorías.

### Scenario 6: nada más de la política cambia (regresión)

Given los 43 tests actuales de `data.ai` (`AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6,
`AiRecipePayloadTest` 8, `DevExpertRecipeGeneratorTest` 19),
When corre `com.sazon.app.data.ai.*`,
Then siguen verdes (429 + `Retry-After` ≤ 5 s, 4xx sin reintento, 429 agotado → `RATE_LIMITED`,
5xx/`IOException`/socket timeout → reintento y `TIMEOUT`, deadline agotado → `TIMEOUT` con 1 intento,
cancelación relanzada, `MISSING_KEY` sin petición, tolerancia del decode, ids deterministas, reglas
del Validador y `matchScore == null`), salvo los cambios mecánicos de esta feature (el decode devuelve
un resultado y la holgura se recalibra).

### Scenario 7: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida en `local.properties` y el emulador de Android Studio en **Windows**,
When se genera/busca con IA,
Then logcat (`SazonAi`) muestra por llamada estado, intento, duración, `content=…c/…b`,
`received=… accepted=…` y `discards={…}`; con recetas reales llegan a Resultados, y si un lote no
acepta ninguna, el log indica el motivo. Con la clave ausente o vaciada sigue el **modo semilla** sin
fallar.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `ai-call-diagnostics` (issue #72; alcance ya acordado por la persona).
- `docs/specs/ai-provider-timeouts.md` y `docs/specs/ai-recipe-decode.md` — plantilla y convenciones de
  correctiva; política de timeouts vigente (90/120/180) y contrato del decode actual.
- `docs/specs/real-ai-provider.md` — contrato de `AiProviderPolicy`, `execute`, `AiCallEvent` y el
  *Unknown* "Latencia real de 6 recetas" (nota `> Resuelto por ai-provider-timeouts` con 90/120/180) y
  el *Unknown* de `AiCallEvent` (línea ~705).
- `docs/technical-discovery.md` — párrafo de `data.ai` (líneas ~336–364), "Integrations" (~450–463),
  "Testing and Verification" (~493–508) y "Observability" (~520–530): citan valores 90/120/180 y la
  costura `AiCallEvent`.
- `docs/risks-and-open-questions.md` — bullet "Errores y reintentos de la IA" (~28–39) con 90/120/180.
- `docs/adr/0003-ai-provider.md` — sin `response_format`/structured outputs; degradación a modo semilla.
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — `CONNECT 10_000`, `SOCKET 90_000`,
  `REQUEST 120_000`, `TOTAL_DEADLINE 180_000`, `MAX_ATTEMPTS 2`, backoff 500, `Retry-After` 5_000,
  `NEUTRAL_CONSTRAINTS`, `attemptRequestTimeoutMillis`, `canRetry`, `transportFailureReason`.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — `decode(content, taxonomy): List<Recipe>`
  (pierde `reason`/`detail` de `RecipeValidation.Discarded`); usa `AiRecipeDraft` y
  `RecipeValidator.validateRecipe(..., NEUTRAL_CONSTRAINTS)`.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiCallOutcome`
  (`SUCCESS`/`EMPTY`/`RETRY`/`UNAVAILABLE`), `AiOperation`, `AiCallEvent(operation, outcome, attempt,
  httpStatus, durationMillis, detail)`.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — `execute` (rama
  `AttemptOutcome.Success` → `decode` → `EMPTY`/`SUCCESS` → `report`) y `report(...)`; `sendAttempt`
  devuelve `Success(status, content)`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (~50–63) — cableado `onEvent` → `Log.d("SazonAi", …)`.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — `DiscardReason`
  (`SCHEMA`/`DIET_PATTERN`/`HARD_RESTRICTION`/`INCOHERENT`), `RecipeValidation.Discarded(reason, detail)`,
  `validateRecipe` (público, es el motor que el decode ya reutiliza).
- `app/src/test/java/com/sazon/app/data/ai/{AiProviderPolicyTest,AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt`
  — convenciones: `runTest`, `MockAi` con contador, clave `test-key`, reloj inyectado, `MockEngine`,
  nombres en español con `_`, casos negativos; `REAL_LATENCY_MILLIS = 28_000L` (holgura); el decode
  devuelve `List<Recipe>`.
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` y `domain/search/AiSearchService.kt`
  — **los llamadores** ya construyen su `discarded` con el Perfil efectivo; no se tocan (el evento es
  del proveedor).

### Existing Patterns To Follow

- **Política en un único sitio**, pura y testeada en JVM; funciones puras + tests de frontera y de
  invariantes con caso negativo (no pasar en vacío).
- **Resultado tipado en lugar de perder detalle**: precedente `RecipeValidation`/`ValidationOutcome` del
  dominio; aquí un resultado de decode en `data.ai`.
- **Costuras aditivas**: `onEvent`/`now` con default; los eventos se capturan con un `mutableListOf` en
  el test; los campos nuevos de `AiCallEvent` van con default para no romper construcciones/tests.
- **Vocabulario de dominio**: el motivo es `DiscardReason` (enum aceptado), no una taxonomía nueva.
- **Longitudes, no cuerpos**: ningún campo del evento lleva texto libre del modelo.

### Current Gaps

- Los tests de holgura solo conocen **una** medición (28,0 s); el peor caso real (115,8 s) no cabe con
  margen en `SOCKET 90 s`.
- `AiRecipePayload.decode` descarta el `reason`: un `EMPTY` no es diagnosticable.
- `AiCallEvent` no lleva longitudes del contenido, ni recibidas vs. aceptadas, ni resumen de motivos.
- `technical-discovery.md`, `risks-and-open-questions.md` y las notas de `real-ai-provider.md` citan
  90/120/180 s (valores viejos).
- El planner **no** ejecutó `bash init.sh` (no hay cambios de código en esta sesión); el implementer
  parte del verde de la sesión 071 (386 tests, 43 en `data.ai`) y corre el gate él mismo.

## Technical Approach

### 1. Valores nuevos en `AiProviderPolicy` (sin tocar reintentos)

| Constante | Antes | Después | Razón |
|---|---|---|---|
| `CONNECT_TIMEOUT_MILLIS` | 10 s | **10 s** (sin cambios) | La conexión funcionó en toda la evidencia |
| `SOCKET_TIMEOUT_MILLIS` | 90 s | **120 s** | `> 115,8 s` (peor modelo medido) y `≥ 2 × 46,6 s` (peor del alias `chat`): la lectura no corta la respuesta |
| `REQUEST_TIMEOUT_MILLIS` | 120 s | **150 s** | Techo por intento `> SOCKET`, así el guardián de **lectura** salta primero y el de petición acota una respuesta que gotea |
| `TOTAL_DEADLINE_MILLIS` | 180 s | **240 s** | Cabe el peor intento medido + backoff + otro igual de lento (`240 − 115,8 − 0,5 = 123,7 ≥ 115,8`) |

Invariantes (los tests las fijan y deben fallar con los valores viejos): `CONNECT (10) ≤ REQUEST (150)`;
`REQUEST (150) ≥ SOCKET (120) > WORST (115,8)`; `SOCKET ≥ 2 × CHAT_WORST (46,6)` (los 4 tiempos del
alias diario: 28,0/33,1/42,1/46,6 s); `attemptRequestTimeoutMillis(TOTAL_DEADLINE) == REQUEST`;
`TOTAL_DEADLINE > REQUEST + BACKOFF`; y, tras el peor intento medido más el backoff, el deadline
restante aún cubre otro intento igualmente lento y `canRetry` es cierto. La lógica de
`attemptRequestTimeoutMillis` no cambia, solo la escala. KDoc de las tres constantes con la evidencia
multi-modelo (issue #72).

### 2. Resultado de decode con motivos — `com.sazon.app.data.ai.AiDecodeResult`

Nuevo fichero `AiDecodeResult.kt`:

```kotlin
internal enum class AiContentState { BLANK, ILLEGIBLE, RECIPES }

internal data class AiDecodeResult(
    val accepted: List<Recipe>,
    val received: Int,                        // nº de elementos del array "recipes"
    val discardedByReason: Map<DiscardReason, Int>, // solo motivos presentes, orden de DiscardReason
    val contentChars: Int,                    // content.length (unidades UTF-16); 0 si null/blanco
    val contentBytes: Int,                    // content.toByteArray(UTF_8).size; 0 si null/blanco
    val contentState: AiContentState,
)
```

`AiRecipePayload.decode` pasa a **devolver `AiDecodeResult`** (misma firma más un parámetro
`constraints: RecipeConstraints = AiProviderPolicy.NEUTRAL_CONSTRAINTS`):
`null`/blanco → `BLANK` (listas/mapa vacíos, `received = 0`, longitudes reales); root o `recipes` no
legibles → `ILLEGIBLE`; array leído → `RECIPES` con `received` y, por cada elemento, `RecipeValidation`:
`Accepted` suma a `accepted`; `Discarded` incrementa `discardedByReason[reason]` (se pierde `detail` a
propósito: solo se cuenta el **motivo**). El contrato sigue siendo **nunca lanza**, elemento a
elemento. Los tests actuales de `AiRecipePayloadTest` cambian solo a `decode(...).accepted` (mismo
comportamiento con el default neutro). El proveedor llama al default (neutro) y **no** pre-filtra: la
decisión de `ai-recipe-decode` (estado Bloqueado de `ai-search`) se conserva; el parámetro existe para
que un test pueda ejercer cada `DiscardReason`.

### 3. Evento enriquecido — `AiCallEvent` (`AiDiagnostics.kt`)

Campos nuevos (con default para no romper nada, aditivos):

```kotlin
data class AiCallEvent(
    /* … los 6 actuales … */
    val contentBytes: Int? = null,                 // bytes UTF-8 del content del modelo
    val contentChars: Int? = null,                 // caracteres del content del modelo
    val received: Int? = null,                     // recetas recibidas
    val accepted: Int? = null,                     // recetas aceptadas por el decode
    val discardedByReason: Map<DiscardReason, Int> = emptyMap(),
)
```

`null`/`emptyMap` cuando no hubo decode (clave ausente, `RETRY`, `UNAVAILABLE`, deadline). La KDoc
mantiene la garantía: **nunca** clave, `Authorization` ni cuerpos; solo longitudes y motivos.

### 4. Emisión y log

En `DevExpertRecipeGenerator.execute`, la rama `AttemptOutcome.Success` usa el resultado:
`callOutcome = if (result.accepted.isEmpty()) EMPTY else SUCCESS` y `report(..., decodeResult = result)`;
`report` rellena los campos nuevos (o `null`/vacío si no hay resultado). `MainActivity` actualiza el
formato:

```kotlin
Log.d("SazonAi",
    "${event.operation} ${event.outcome} attempt=${event.attempt} status=${event.httpStatus} " +
    "${event.durationMillis}ms content=${event.contentChars}c/${event.contentBytes}b " +
    "received=${event.received} accepted=${event.accepted} discards=${event.discardedByReason} ${event.detail}")
```

`discardedByReason` se materializa en orden de `DiscardReason` (determinista para el test y el log).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` — `AiDecodeResult` + `AiContentState`.

**Modificar (código)**

- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — 120/150/240 + KDoc con la evidencia.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — `decode` devuelve `AiDecodeResult`
  (con `constraints` default neutro) y cuenta motivos/longitudes; KDoc actualizado.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — 5 campos nuevos en `AiCallEvent` + KDoc.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — consumir el resultado y
  emitir el evento enriquecido (`report(..., decodeResult)`); sin tocar `sendAttempt` ni los reintentos.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — formato del `Log.d("SazonAi", …)`.

**Modificar (tests)**

- `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` — `REAL_LATENCY_MILLIS` → peor caso
  (`115_800L`) + `CHAT_WORST_LATENCY_MILLIS` (`46_600L`), invariantes/márgenes nuevos y frontera.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` — adaptar a `.accepted`; tests del
  resumen (`received`/`accepted`/`discardedByReason` con los cuatro motivos) y de `contentState`
  (BLANK/ILLEGIBLE/RECIPES) con longitudes.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — eventos: `EMPTY` por
  vacío/ilegible, lote parcial descartado, todas descartadas y no-fuga.

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`,
`OpenAiChat.kt`, `AiHttpClientFactory.kt` (lee las constantes), `domain/**`, `data/generator/**`,
`ui/**`, las bases Room, `app/src/main/assets/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`,
`AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md`, `docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). No hay pantalla, estado, microcopy, token ni acabado
  nuevos; el único cambio fuera de `data.ai` es el formato del **log** de `MainActivity`.
- Design source: not applicable. Screens or states affected: ninguno.
- New design artifact required: **no**. `docs/design/concepts/ai-call-diagnostics/` **no** es
  necesario y la feature **no** se bloquea por capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — párrafo de `data.ai` (líneas ~336–364) y "Integrations"
  (~450–463): valores **120/150/240** y recorte `min(150 s, restante)`; "Observability" (~520–530):
  `AiCallEvent` ahora lleva `contentChars`/`contentBytes`, `received`/`accepted` y
  `discardedByReason` (el motivo del Validador en el proveedor ya es real, no una promesa);
  "Testing and Verification" (~493–508): holgura contra el peor caso medido y tests del evento.
- `docs/risks-and-open-questions.md`: **update** — el bullet "Errores y reintentos de la IA" (~28–39)
  pasa de 90/120/180 s a **120/150/240 s** y cita la evidencia multi-modelo (peor caso 115,8 s) y el
  diagnóstico enriquecido. Sin renumerar ni tocar tablas.
- `docs/specs/real-ai-provider.md`: **update mínimo** — la nota `> Resuelto por ai-provider-timeouts`
  del *Unknown* de latencia pasa a los valores nuevos y apunta a esta correctiva; el *Unknown* de
  `AiCallEvent` (~705) se anota con que el proveedor ya reporta longitudes y motivos. Sus escenarios y
  valores históricos no se reescriben.
- `docs/specs/ai-provider-timeouts.md`: **update mínimo** — nota en su *Unknown* de latencia
  ("recalibrada por `ai-call-diagnostics` (issue #72): 120/150/240 s con peor caso medido 115,8 s").
- `docs/domain-model.md`, `docs/domain/taxonomy.md`, `AGENTS.md`, `DESIGN.md`, `docs/design/**`: **not
  needed** — no cambian entidades, contrato del prompt, flujo, arranque ni dirección visual.
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. `AiProviderPolicy`: valores 120/150/240 s con KDoc de la evidencia; `CONNECT`, `MAX_ATTEMPTS`,
   backoff y `Retry-After` intactos.
2. Crear `AiDecodeResult` y hacer que `AiRecipePayload.decode` devuelva el resultado (contadores,
   motivos, longitudes y estado), con `constraints` default neutro.
3. Añadir los campos a `AiCallEvent` y propagarlos en `execute`/`report`; actualizar el log de
   `MainActivity`.
4. Recalibrar `AiProviderPolicyTest` (peor caso + invariantes + frontera) y adaptar/añadir los tests de
   `AiRecipePayloadTest` y `DevExpertRecipeGeneratorTest`.
5. `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
   verde; actualizar las cuatro docs; el implementer registra estado/evidencia en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

> Estado (sesión 072, implementer): **todas completadas**; verificado con `bash init.sh` y la tirada
> focal `com.sazon.app.data.ai.*`. Las casillas de `Validator Checklist` quedan para el validador.

- [x] `AiProviderPolicy`: `SOCKET = 120_000L`, `REQUEST = 150_000L`, `TOTAL_DEADLINE = 240_000L`;
      `CONNECT`, `MAX_ATTEMPTS`, backoff y `Retry-After` sin cambios; KDoc con la tabla multi-modelo.
- [x] `AiDecodeResult` (`AiContentState` + data class) en `data.ai`.
- [x] `AiRecipePayload.decode` devuelve `AiDecodeResult`; cuenta `received`/`accepted`/
      `discardedByReason`, calcula `contentChars`/`contentBytes` y `contentState` (BLANK/ILLEGIBLE/
      RECIPES); `constraints` default `NEUTRAL_CONSTRAINTS`; sigue sin lanzar; KDoc actualizado.
- [x] `AiCallEvent`: `contentBytes`/`contentChars`/`received`/`accepted`/`discardedByReason` con default;
      `report(...)` los rellena desde el resultado del decode; sin campos de texto del modelo.
- [x] `MainActivity`: `Log.d("SazonAi", …)` muestra `content=…c/…b`, `received=…`, `accepted=…`,
      `discards={…}`.
- [x] `AiProviderPolicyTest`: `REAL_LATENCY_MILLIS = 115_800L` (+ `CHAT_WORST_LATENCY_MILLIS =
      46_600L`), invariantes y márgenes de Scenario 1 (fallan con 90/120/180) y frontera del recorte.
- [x] `AiRecipePayloadTest`: adaptar a `.accepted`; resumen con los cuatro `DiscardReason` (incluido un
      caso con restricciones no neutras) y `contentState` con longitudes; conservar tolerancia,
      unicidad de ids, reglas del Validador y `matchScore`.
- [x] `DevExpertRecipeGeneratorTest`: `EMPTY` por contenido vacío e ilegible (Scenario 2), lote parcial
      con resumen en el evento (Scenario 3), todas descartadas → `EMPTY` con motivos (Scenario 4) y
      no-fuga de clave/cuerpos (Scenario 5).
- [x] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
      verde (0 errores de lint y sin warnings nuevos en ficheros de la feature).
- [x] Inspección de alcance: `git diff --stat` solo con lo previsto; `Recipe.kt`, `RecipeValidator.kt`,
      `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`,
      `ui/**`, build y `init.sh` intactos; sin dependencias nuevas.
- [x] Docs durables (`technical-discovery.md`, `risks-and-open-questions.md`, notas en
      `real-ai-provider.md` y `ai-provider-timeouts.md`).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).
- `AiProviderPolicyTest` — Scenario 1: `REAL_LATENCY_MILLIS = 115_800L` documentado;
  `SOCKET > WORST`; `SOCKET ≥ 2 × CHAT_WORST`; `REQUEST ≥ SOCKET > WORST`; `REQUEST ≥ CONNECT == 10_000`;
  `attemptRequestTimeoutMillis(TOTAL_DEADLINE) == REQUEST`; `TOTAL_DEADLINE > REQUEST + BACKOFF`;
  `TOTAL_DEADLINE − WORST − BACKOFF ≥ WORST` y `canRetry(1, …)` cierto. **Con 90/120/180 fallan
  `SOCKET > WORST`, `SOCKET ≥ 2 × CHAT_WORST` y el margen de reintento** (regresión real). Frontera del
  recorte: `(REQUEST + 1) == REQUEST`, `(REQUEST − 1) == REQUEST − 1`, `(TOTAL_DEADLINE) == REQUEST`,
  `(0) == 0`, `(negativo) == 0`.
- `AiRecipePayloadTest` — resumen del decode: `received`/`accepted`/`discardedByReason` con lote
  mixto; caso con `constraints` no neutras que produce `DIET_PATTERN` y `HARD_RESTRICTION` (los cuatro
  motivos representables); `BLANK`/`ILLEGIBLE`/`RECIPES` con `contentChars`/`contentBytes`; regresión de
  tolerancia, unicidad de ids, reglas del Validador y `matchScore == null`.
- `DevExpertRecipeGeneratorTest` — `MockEngine` 200 con contenido vacío e ilegible → evento `EMPTY`
  (`received = 0`, `accepted = 0`, longitudes); lote parcial → evento `SUCCESS` con `received = N`,
  `accepted = 1` y `discardedByReason = {SCHEMA: a, INCOHERENT: b}`; todas descartadas → `EMPTY` con
  motivos; ningún evento contiene `test-key`, `Bearer`, el texto del `content` ni títulos.
- Regresión: los 43 tests de `data.ai` siguen contando y en verde (los del decode, adaptados a
  `.accepted`); `RecipeValidatorTest` intacto.
- **Lint:** `bash init.sh` → 0 errores y sin warnings nuevos (los 13 preexistentes se mantienen).
- **Inspección:** `git diff --stat` limitado a lo previsto; `AiHttpClientFactory.kt` sin cambios (prueba
  de que no hay literales duplicados); `MAX_ATTEMPTS`/`RETRY_BACKOFF_MILLIS`/`RETRY_AFTER_MAX_MILLIS`
  sin cambios; `grep` de `90_000`/`120_000`/`180_000` en `data.ai` sin resultados.
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):** con
  `devexpert.apiKey` válida, `bash scripts/gradle.sh assembleDebug` y ejecutar; buscar/generar y ver en
  logcat la línea `SazonAi` con `content=…c/…b received=… accepted=… discards={…}` y las recetas
  reales; forzar un lote sin recetas aceptadas y comprobar que el motivo aparece; clave ausente o
  vaciada → modo semilla sin fallar.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; `MockEngine` cubre el transporte y el
  wiring en JVM, y lo visual/manual lo revisa la persona). `init.sh` **no cambia**.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.*` con recuento de tests y 0 fallos
  (esperado: 43 preexistentes en `data.ai` / 386 en total, sin eliminar ninguno; los del decode
  adaptados).
- Nombres y resultado de los tests nuevos: holgura con peor caso 115,8 s e invariantes (fallarían con
  90/120/180), `EMPTY` por vacío/ilegible, resumen `{SCHEMA…, INCOHERENT…}` en el evento, todas
  descartadas → `EMPTY`, los cuatro motivos en el resumen del decode, y no-fuga de clave/cuerpos.
- `git diff --stat` de lo tocado y confirmación de que `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`,
  `ui/**`, build, Taxonomía, `seed-recipes.json`, `init.sh` y `DESIGN.md` no cambian.
- Resultado **manual** del emulador (logcat `SazonAi` con las métricas y el motivo; recetas reales;
  clave ausente → modo semilla).
- Docs actualizadas (sin valores 90/120/180) y anotación de estado/evidencia en `feature_list.json` y
  `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **Valores**: connect 10 s (sin cambios), socket **120 s**, request **150 s**, deadline **240 s**; el
  peor caso medido (115,8 s) cabe en la lectura con ~4,2 s de margen y, tras él más el backoff, aún
  queda deadline para otro intento igualmente lento. `MAX_ATTEMPTS`, backoff y `Retry-After` no se
  tocan: el problema era la calibración del tiempo, no la política de reintentos aceptada.
- **Resultado tipado del decode** (`AiDecodeResult`) en `data.ai` en lugar de exponer `detail` o
  cambiar el dominio: el motivo se expresa con el **enum aceptado** `DiscardReason` y solo se cuenta
  (no se propaga el texto del descarte ni títulos).
- **`constraints` con default neutro en `decode`**: la producción (`DevExpertRecipeGenerator`) sigue
  llamando con `NEUTRAL_CONSTRAINTS` (no pre-filtra; el estado Bloqueado de `ai-search` se conserva) y
  el parámetro permite que un test ejercite **los cuatro** motivos. Es aditivo: los tests existentes
  pasan el default y solo cambian a `.accepted`.
- **`discardedByReason` como `Map<DiscardReason, Int>`** en el evento: es el "resumen" pedido, sin
  cuerpos ni títulos, y en orden de `DiscardReason` para un log determinista.
- **Longitudes, no cuerpos**: `contentChars` = `content.length` (unidades UTF-16) y `contentBytes` =
  UTF-8; el evento se serializa inmune a la clave y a los cuerpos (test de no-fuga).
- **Log en `MainActivity`**: único cambio fuera de `data.ai`; mantiene `Log.d` local (sin telemetría).

### Hallazgos de la sesión de implementación

- **Visibilidad de `AiDecodeResult`/`AiContentState` (§2): el `internal` propuesto no compila tal cual.**
  El bloque de la spec declara el tipo de resultado `internal`, pero `AiRecipePayload.decode` era
  `public`: un `public` no puede exponer un tipo de retorno `internal`
  (`'public' function exposes its 'internal' return type`). Resuelto con el criterio que la propia spec
  pide (mantener la visibilidad correcta): los dos tipos nuevos siguen **`internal`** y `decode` pasa a
  **`internal fun`** (el objeto `AiRecipePayload` sigue público, como `AiRecipeIds` es `internal`). Los
  tests JVM del mismo módulo lo usan por los *friend paths* (igual que ya hacían con
  `AiRecipeIds`/`AiRecipeDraft`). Sin cambio de comportamiento ni de firma visible para la app.
- **Prueba de regresión de los timeouts**: al restaurar temporalmente los valores viejos (90/120/180),
  `AiProviderPolicyTest.la_politica_admite_con_holgura_la_latencia_real_de_varios_modelos` **falla en su
  primera aserción** (`90_000 > 115_800` es falso: "El socket timeout debe superar el peor caso
  medido"); el test se detiene ahí, así que no evalúa las otras dos aserciones que también fallarían
  (`socket >= 2x 46,6 s` y el margen de reintento). Igual que en `ai-provider-timeouts`, el
  discriminador viejo-vs-nuevo es real; el orden de las aserciones hace que el fallo se vea en la
  primera.
- **`content == null` colapsa dos casos** (ya estaba en los *Unknowns*): un envelope ilegible y un
  `content` vacío dan ambos `BLANK` (`0 c/0 b`). Medir la longitud del cuerpo HTTP crudo queda fuera de
  alcance.

### Unknowns

- **`DIET_PATTERN`/`HARD_RESTRICTION` no son alcanzables en el decode de producción** porque usa
  restricciones neutras (decisión de `ai-recipe-decode`): en producción el resumen solo mostrará
  `SCHEMA`/`INCOHERENT`. El tipo soporta los cuatro y el test los ejerce con restricciones no neutras.
  Decidir lo contrario (pre-filtrar en el proveedor) rompería el Bloqueado de `ai-search`: **no** se
  hace.
- **`content == null` colapsa** "envelope del proveedor ilegible" y "contenido vacío": el evento
  mostrará `0 c/0 b` y `0 recibidas`. Distinguirlos exigiría reportar la longitud del **cuerpo HTTP**
  crudo (hoy fuera de alcance).
- **Margen estrecho con `mimo-v2.6-pro`** (115,8 s vs. socket 120 s, ~4,2 s): otra medición por encima
  de 120 s se degradará honestamente como `TIMEOUT` (socket) y es revisable en el mismo sitio.
- **UX de la espera**: 240 s de deadline es mucha pantalla de carga; Generador y Búsqueda tienen
  "Cancelar". Un rediseño de la espera sería de UI, no de esta feature.
- **Recorte por intento y caché de clientes de Ktor**: `timeout { … }` cambia la capability por
  intento; ya ocurría con los valores viejos y no altera el resultado (anotado para no confundirlo con
  una regresión). `socketTimeoutMillis` aplica a lectura y escritura; el mensaje enviado es pequeño.

## Validator Checklist

- [ ] `AiProviderPolicy` sigue siendo el **único** sitio de la política: socket 120 s, request 150 s,
      deadline 240 s; connect 10 s, `MAX_ATTEMPTS`, backoff y `Retry-After` **sin cambios**.
- [ ] Los tests demuestran la holgura frente al **peor caso medido (115,8 s)** y **fallan** con los
      valores viejos (90/120/180); la frontera del recorte se prueba.
- [ ] `AiRecipePayload.decode` devuelve un **resultado** con `accepted`, `received`,
      `discardedByReason` y longitudes/estado; sigue sin lanzar y con restricciones **neutras** por
      defecto (no pre-filtra).
- [ ] El `AiCallEvent` explica un `EMPTY` (contenido vacío/ilegible → longitudes y `0` recibidas), un
      lote parcial (`received`/`accepted`/resumen de motivos) y el caso de todas descartadas, y **nunca**
      contiene clave, `Authorization`, cuerpos ni títulos.
- [ ] Los **cuatro** `DiscardReason` son representables en el resumen (test con restricciones no
      neutras) y el resumen del evento queda en el orden del enum.
- [ ] Alcance: solo `data.ai` (código + sus tests), `MainActivity` (log) y las cuatro docs;
      `Recipe.kt`, `RecipeValidator.kt`, `domain/**`, `ui/**`, Room, Taxonomía, `seed-recipes.json`,
      build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos) y regresión de los 43
      tests de `data.ai` y de `RecipeValidatorTest`.
- [ ] Docs sin valores 90/120/180 (`technical-discovery.md`, `risks-and-open-questions.md`) y notas de
      *Unknown* trazadas en `real-ai-provider.md` y `ai-provider-timeouts.md`.
- [ ] `Visual Design Impact` sin capturas (no hay UI) y la verificación **manual** en emulador anotada
      como tal (logcat con métricas y motivo; clave ausente → modo semilla), no automatizable desde WSL.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por implementer/validador/orquestador.
