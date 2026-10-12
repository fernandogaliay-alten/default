# Feature Implementation Spec: Formato JSON de la respuesta de la IA (`json_object` con fallback)

## Source Feature

- `id`: `ai-response-format` · `area`: `integration` · `depends_on`: `["ai-call-diagnostics"]`
  (`accepted`, sesión 073) · `status`: `not_started` · `source`: `feature_list.json` (issue #74).
- **No es feature de UI**: `area: integration`; sin pantallas, estados, microcopy ni capturas. No usa
  `docs/design/concepts/` y **no** se bloquea por Stitch.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) `./gradlew test` cubre que la petición incluye
  `response_format` cuando está activo y lo omite cuando no, que el fallback reintenta **una sola vez**
  ante 400/422 sin `response_format` y que **nunca** se serializa un `response_format` nulo, sin
  exponer la clave; (2) prueba **manual** en emulador: con clave válida siguen llegando recetas reales
  y el logcat (`SazonAi`) indica el modo usado; con el interruptor apagado el comportamiento es
  idéntico al actual.

**Contexto (correctiva de `real-ai-provider`, issue #74).** El proveedor real ya está conectado,
decodifica la forma real del contrato §4 y su diagnóstico explica cada llamada (`AiCallEvent` +
`Log.d("SazonAi", …)`). Hoy la petición **no** envía `response_format` (decisión de ADR 0003). La
persona decidió (issue #74), con evidencia real, que **sí** se envíe `response_format
{"type":"json_object"}` en el **alias `chat` actual** —verificado HTTP 200 con `chat`+`json_object`—
para reducir respuestas en prosa/ilegibles, **sin** usar `json_schema` ni pinnear `chat-pro`. Si el
proveedor lo rechazara (400/422), la app reintenta **una sola vez** sin formato, de modo que la
degradación sea invisible para el usuario (que ya ve solo recetas validadas). El **Validador no
cambia**: el formato garantiza *forma* (JSON parseable), no *dominio* (dieta/alérgenos/coherencia).

## Goal

Un cambio acotado en `com.sazon.app.data.ai` (más el formato del log de `MainActivity`) con cinco
piezas:

1. **Enviar** `response_format: {"type":"json_object"}` en `/chat/completions` con el alias `chat`
   actual, en las **tres** operaciones (comparten `execute`/`sendAttempt`). **No** `json_schema`,
   **no** `chat-pro`.
2. **Fallback degradable**: si el proveedor responde **400 o 422** con el formato activo, reintentar
   **una única vez** sin `response_format`; el resto de estados no cambian.
3. **Interruptor** en `AiProviderPolicy` (`RESPONSE_FORMAT_ENABLED`) para poder apagar la feature desde
   un único sitio, más una costura de constructor para poder probarlo.
4. **Observabilidad**: el modo usado (`json_object` vs. sin formato) y si saltó el fallback, sobre el
   diagnóstico ya aceptado (`AiCallEvent` / `Log.d("SazonAi")`).
5. **Nunca** serializar `"response_format": null`: `AiHttpClientFactory` usa `encodeDefaults = true`, así
   que la clave se **omite** cuando no se usa (no se emite `null`).

No cambia el puerto `RecipeGenerator`, ni el dominio aceptado (`Recipe`, `RecipeValidator`), ni la UI,
ni la persistencia, ni los timeouts/reintentos de transporte, ni el decode. El comportamiento
observable para el usuario es el mismo (solo recetas válidas) salvo que el logcat indica el modo.

## Non-Goals

- **No** se añade `json_schema`/structured outputs, ni se fuerza un esquema estricto: la compatibilidad
  del proveedor no lo garantiza (ADR 0003) y queda fuera de alcance por decisión de la persona.
- **No** se pinnea `chat-pro` ni cambia el alias `chat`; tampoco se cambia de proveedor
  (OpenCode Zen/Go sería feature + ADR aparte).
- **No** se toca el dominio aceptado: `Recipe.kt`, `RecipeValidator.kt`, `RecipeConstraints`,
  `DiscardReason`, el puerto `RecipeGenerator`, ni las bases Room, la Taxonomía, `seed-recipes.json`.
  El formato solo garantiza **forma**; el Validador sigue siendo la **puerta dura** de dominio.
- **No** se cambia la semántica de reintentos de transporte (`MAX_ATTEMPTS = 2`, backoff 500 ms,
  `Retry-After` ≤ 5 s, reintentables 408/429/5xx, nunca 4xx) ni los timeouts (connect 10 s, socket
  120 s, request 150 s, deadline 240 s de `ai-call-diagnostics`). El fallback es un camino **aparte**,
  no una relajación de `isRetryableStatus`.
- **No** se toca `AiRecipePayload`/`AiDecodeResult`/`AiRecipeDraft`/`AiRecipeIds` ni el `PromptBuilder`
  (el prompt ya exige JSON y eso satisface la convención de `json_object`).
- **No** se añaden dependencias, ni se toca el build, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
  `DESIGN.md` ni `docs/design/**`.
- **No** se registra ni se muestra la clave, la cabecera `Authorization`, la petición ni los cuerpos:
  el evento sigue siendo local y solo añade el **modo** (un enum), no texto del modelo.

## Job Story

Como desarrollador del producto,
quiero que las llamadas pidan JSON al proveedor y que, si el proveedor no lo acepta, se reintente una
vez sin ese formato y quede registrado en logcat qué modo se usó,
para reducir respuestas en prosa/ilegibles sin arriesgar la disponibilidad y sin depender de una
capacidad (`json_schema`) que el proveedor no garantiza.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia nada de lo que ve; sigue
  viendo solo recetas válidas y puede cancelar la carga.
- **Desarrollador / agente**: verifica en **JVM** con `MockEngine` y clave ficticia (`test-key`): el
  cuerpo de la petición con/sin `response_format`, el fallback one-shot ante 400/422, la ausencia de
  `null`, y los eventos con el modo/fallback. Nunca usa red real ni clave real.
- **Sistema**: conserva `AiUnavailableException` tipada → modo semilla y el deadline total acotando la
  operación completa; el puerto `RecipeGenerator` no cambia.

## Acceptance Scenarios

### Scenario 1: formato activo ⇒ la petición lleva `response_format` (JVM)

Given el interruptor `AiProviderPolicy.RESPONSE_FORMAT_ENABLED == true` (por defecto) y un `MockEngine`
que responde `200` con recetas válidas,
When se invoca cualquiera de las tres operaciones,
Then el cuerpo de la petición contiene `"response_format":{"type":"json_object"}`, hace **una sola**
llamada, llegan las recetas y el evento final es `SUCCESS` con `responseFormat = JSON_OBJECT`. El
resto del contrato de la petición (`model`, `messages`, `Authorization`) no cambia.

### Scenario 2: interruptor apagado ⇒ omite la clave y no hay `null` (JVM)

Given `responseFormatEnabled = false` (costura del constructor) y un `MockEngine`,
When se invoca,
Then el cuerpo **no** contiene la clave `response_format` (ni `"response_format":null`), hace una sola
llamada y un `400`/`422` degrada a `HTTP_ERROR` con **una** llamada, exactamente como hoy. El evento
lleva `responseFormat = NONE`.

### Scenario 3: serialización directa: nunca un `response_format` nulo (JVM)

Given un `ChatCompletionRequest(model, messages)` **sin** formato, serializado con el **mismo**
`Json { ignoreUnknownKeys = true; encodeDefaults = true }` que usa el cliente,
When se serializa a texto,
Then la cadena **no** contiene `response_format` y, en particular, **no** contiene
`"response_format":null`. La clave solo aparece cuando el valor es `ResponseFormat.JSON_OBJECT`.

### Scenario 4: fallback: 400/422 con formato activo reintenta una vez sin formato (JVM)

Given formato activo y un `MockEngine` que responde `400` (y el caso análogo `422`) en la **primera**
llamada y `200` con recetas válidas en la segunda,
When se invoca la operación,
Then hay exactamente **2** llamadas: la primera con `response_format`, la segunda **sin**; las recetas
llegan; el evento intermedio es `FALLBACK` (`httpStatus = 400`, `responseFormat = JSON_OBJECT`) y el
final `SUCCESS` con `responseFormat = NONE`. No hay backoff artificial entre ambos intentos.

### Scenario 5: el fallback es una sola vez (JVM)

Given formato activo y un `MockEngine` que responde **siempre** `400` (y el caso `422`),
When se invoca,
Then hay exactamente **2** llamadas (una con formato, una sin), **no** hay una tercera, y se lanza
`AiUnavailableException` con `reason = HTTP_ERROR`; el evento final es `UNAVAILABLE` con
`responseFormat = NONE`. Si el segundo intento devuelve `200` vacío/ilegible, no se reintenta el
formato (no se vuelve a activar): un único fallback por operación.

### Scenario 6: el fallback solo lo disparan 400/422 (JVM, regresión)

Given formato activo y un `MockEngine` que responde:
- `401`/`403`/`404` → **una** llamada, `HTTP_ERROR`, **sin** fallback (aunque el cuerpo llevaba
  `response_format`), igual que hoy.
- `429` (con o sin `Retry-After` corto) y `5xx` → se mantiene el reintento de **transporte** (2
  llamadas, **ambas** con formato) y `RATE_LIMITED`/`HTTP_ERROR`; `isRetryableStatus` no cambia.

Then ningún otro estado provoca el fallback de formato.

### Scenario 7: observabilidad del modo y del fallback, sin fugas (JVM)

Given cualquier secuencia de la feature (éxito con formato, fallback, interruptor apagado),
When se inspeccionan los `AiCallEvent`,
Then se distingue el modo usado (`responseFormat = JSON_OBJECT`/`NONE`) y si saltó el fallback
(`outcome = FALLBACK`), y `toString()` de **ningún** evento contiene `test-key`, `Bearer`/
`Authorization` ni el cuerpo del modelo (se extiende la garantía de `ai-call-diagnostics`).

### Scenario 8: regresión de la política y del decode (JVM)

Given los **50** tests actuales de `data.ai` (`AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6,
`AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 23),
When corre `com.sazon.app.data.ai.*`,
Then siguen verdes salvo los cambios mecánicos de esta feature (nuevos casos de formato/fallback y el
campo `responseFormat` con default): timeouts 120/150/240, `MAX_ATTEMPTS`/backoff/`Retry-After`,
clasificación de transporte, decode con la forma real del contrato, ids deterministas, reglas del
Validador y `matchScore == null` intactos.

### Scenario 9: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida en `local.properties` y el emulador de Android Studio en **Windows**,
When se genera/busca con IA,
Then siguen llegando recetas reales y logcat (`SazonAi`) muestra por llamada el **modo** usado
(`format=JSON_OBJECT`/`NONE`) y, si saltó, la línea `FALLBACK`; con `RESPONSE_FORMAT_ENABLED = false`
el comportamiento es idéntico al actual (sin `response_format` en la petición). Clave ausente o
vaciada → modo semilla sin fallar.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `ai-response-format` (issue #74; alcance ya acordado por la persona).
- `docs/specs/ai-call-diagnostics.md` — plantilla y convenciones de correctiva; estado vigente del
  diagnóstico (`AiCallEvent`, `AiDecodeResult`) y confirmación de que `response_format` quedó fuera de
  alcance entonces.
- `docs/specs/real-ai-provider.md` — contrato de `execute`/`sendAttempt`, `AiCallEvent` y las notas
  «`response_format` no se envía» (líneas ~357, ~667, ~740).
- `docs/adr/0003-ai-provider.md` — decisión vigente: **no** depender de salida estructurada forzada
  (`json_schema`); degradación a modo semilla. Es la decisión que esta feature **matiza** (`json_object`
  sí; `json_schema` no) y que hay que anotar en docs.
- `docs/technical-discovery.md` — párrafo de `data.ai` (~336–369), «Integrations» (~442–468, incluye
  «sin `response_format`, `stream`, `temperature` ni `max_tokens`»), «Testing and Verification»
  (~491–531) y «Observability» (~533–549).
- `docs/risks-and-open-questions.md` — bullets «Errores y reintentos de la IA» (~28–44) y
  «Proveedor/modelo de IA» (~76–79, «no depende de structured outputs»).
- `app/src/main/java/com/sazon/app/data/ai/OpenAiChat.kt` — DTOs del envelope; `ChatCompletionRequest`
  solo tiene `model` y `messages` (KDoc dice que **no** se envía `response_format`).
- `app/src/main/java/com/sazon/app/data/ai/AiHttpClientFactory.kt` — `ContentNegotiation` con
  `Json { ignoreUnknownKeys = true; encodeDefaults = true }` (la razón del riesgo del `null`) y
  `HttpTimeout` con la política de `AiProviderPolicy`; `expectSuccess = false`.
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — política en un único sitio;
  `MODEL_ALIAS`, `isRetryableStatus`, `unavailableReason`, timeouts 120/150/240 y `NEUTRAL_CONSTRAINTS`.
  Un `400/422` **no** es reintentable (`isRetryableStatus`).
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — `execute` (bucle con
  `attempt`, deadline y ramas `Success`/`HttpFailure`/`TransportFailure`), `sendAttempt` (construye
  `ChatCompletionRequest`), `report(...)` y la costura `onEvent`/`now`.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiCallOutcome`
  (`SUCCESS`/`EMPTY`/`RETRY`/`UNAVAILABLE`), `AiOperation`, `AiCallEvent(…, detail, contentChars, …)`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (~50–67) — cableado `onEvent` → `Log.d("SazonAi", …)`
  (el único cambio fuera de `data.ai`).
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — convenciones: `runTest`,
  `MockAi` (contador + `lastRequest` + `bodyText()`), clave `test-key`, `MockEngine`, reloj inyectado,
  nombres en español con `_` y casos negativos; `provider(...)` construye el generador.
- `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` — estilo de tests puros de política
  (positivos y negativos).
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` y
  `domain/search/AiSearchService.kt` — los llamadores ya degradan a modo semilla; **no** se tocan (el
  fallback es interno al proveedor).

### Existing Patterns To Follow

- **Política en un único sitio**, pura y testeada en JVM: interruptor y estados del fallback viven en
  `AiProviderPolicy`.
- **Costuras aditivas con default**: `onEvent`/`now` (ya existentes) y, para el interruptor, un
  parámetro de constructor con default `= AiProviderPolicy.RESPONSE_FORMAT_ENABLED` (mismo patrón que
  `model: String = AiProviderPolicy.MODEL_ALIAS`), para que la producción lea el interruptor y los tests
  inyecten el valor apagado/encendido.
- **Campos aditivos con default** en `AiCallEvent` y un valor nuevo de `AiCallOutcome` para no romper
  construcciones ni tests.
- **Vocabulario del dominio**: el modo es un enum propio de `data.ai` (`AiResponseFormat`); no se
  introduce un tipo de dominio.
- **Nunca claves ni cuerpos**: el evento solo añade un enum; la garantía de no-fuga se mantiene y se
  re-prueba.
- **Reintentos a mano y explícitos**: el fallback es una rama más del `when` de `execute`, testeable en
  JVM con `MockEngine`.

### Current Gaps

- No existe `response_format` en la petición (`ChatCompletionRequest` solo tiene `model`/`messages`).
- No hay un interruptor ni una rama de fallback; un `400/422` hoy degrada a `HTTP_ERROR` en **una**
  llamada.
- `AiCallEvent` no distingue el modo ni el fallback.
- El riesgo del `null` no está cubierto por ningún test: el cliente serializa con `encodeDefaults =
  true`.
- `technical-discovery.md`, `risks-and-open-questions.md`, `real-ai-provider.md` y `ai-call-diagnostics.md`
  afirman «no se envía `response_format`» y hay que anotarlo.
- El planner **no** ejecutó `bash init.sh` (no hay cambios de código en esta sesión); el implementer
  parte del verde de la sesión 073 (393 tests totales, 50 en `data.ai`) y corre el gate él mismo.

## Technical Approach

### 1. Interruptor y estados del fallback — `AiProviderPolicy`

```kotlin
/**
 * Interruptor del `response_format`. `true` (por defecto) pide `{"type":"json_object"}`;
 * `false` desactiva la feature por completo (comportamiento idéntico al actual, sin la clave).
 * Un único sitio; la producción lo lee y el generador lo recibe por constructor.
 */
const val RESPONSE_FORMAT_ENABLED = true

/** El único `type` de `response_format` usado hoy. */
const val RESPONSE_FORMAT_JSON_OBJECT = "json_object"

/** Un 400/422 con el formato activo dispara el fallback (una sola vez); ningún otro estado. */
fun requiresResponseFormatFallback(statusCode: Int): Boolean =
    statusCode == 400 || statusCode == 422
```

`isRetryableStatus`, `unavailableReason`, timeouts, `MAX_ATTEMPTS`, backoff y `Retry-After` **no**
cambian.

### 2. DTO de la petición con clave opcional bien serializada — `OpenAiChat.kt`

```kotlin
@Serializable
data class ResponseFormat(val type: String) {
    companion object {
        val JSON_OBJECT = ResponseFormat(AiProviderPolicy.RESPONSE_FORMAT_JSON_OBJECT)
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val responseFormat: ResponseFormat? = null,
)
```

**Por qué `@EncodeDefault(EncodeDefault.Mode.NEVER)`** (y no `explicitNulls = false` global ni un DTO
aparte): el cliente usa `encodeDefaults = true`; la anotación hace que, por propiedad, el valor por
defecto (`null`) **nunca** se emita, así que la clave se **omite** cuando no se usa y solo aparece como
`"response_format":{"type":"json_object"}` cuando se activa. `explicitNulls = false` en el `Json` del
cliente cambiaría el comportamiento global de serialización (más superficie de riesgo) y un segundo DTO
duplicaría la petición. La anotación exige `@OptIn(ExperimentalSerializationApi::class)`. La KDoc de
`OpenAiChat` deja de afirmar «no se envía `response_format`».

### 3. `sendAttempt` con formato y rama de fallback — `DevExpertRecipeGenerator.kt`

- Constructor: nuevo parámetro `responseFormatEnabled: Boolean = AiProviderPolicy.RESPONSE_FORMAT_ENABLED`.
- Estado local en `execute`: `var usedFallback = false` (una vez por operación).
- Cada iteración: `val includeFormat = responseFormatEnabled && !usedFallback`;
  `sendAttempt(prompt, remaining, includeFormat)` construye
  `ChatCompletionRequest(…, responseFormat = if (includeFormat) ResponseFormat.JSON_OBJECT else null)`.
- `HttpFailure`: **primero** el fallback de formato; después, la lógica actual sin cambios:

```kotlin
val fallback = includeFormat &&
    AiProviderPolicy.requiresResponseFormatFallback(outcome.status) &&
    !usedFallback
if (fallback) {
    usedFallback = true
    report(operation, AiCallOutcome.FALLBACK, attempt, outcome.status, startedAt,
        "HTTP ${outcome.status}", responseFormat = AiResponseFormat.JSON_OBJECT)
    attempt += 1            // el fallback consume el segundo intento (MAX_ATTEMPTS = 2)
} else if (retryable && hasRetryMargin(...)) {
    // rama actual, con responseFormat = formatOf(includeFormat)
} else if (retryable && attempt < AiProviderPolicy.MAX_ATTEMPTS) {
    // rama actual (deadline)
} else {
    // rama actual -> AiUnavailableException; responseFormat = formatOf(includeFormat)
}
```

Con `responseFormatEnabled = false` la condición `includeFormat` es siempre falsa y el flujo es
**idéntico** al actual. Con formato activo y sin 400/422, el único cambio es la clave del cuerpo y el
campo del evento. El fallback **no** usa `isRetryableStatus` (400/422 siguen sin ser reintentables de
transporte) y no introduce backoff artificial; el deadline total (`TOTAL_DEADLINE_MILLIS`) sigue
acotando la operación. Como máximo hay **una** petición de fallback por operación.

### 4. Observabilidad — `AiDiagnostics.kt` y `MainActivity.kt`

```kotlin
enum class AiResponseFormat { JSON_OBJECT, NONE }

enum class AiCallOutcome { SUCCESS, EMPTY, RETRY, FALLBACK, UNAVAILABLE }

data class AiCallEvent(
    /* … los 11 actuales … */
    val responseFormat: AiResponseFormat = AiResponseFormat.NONE,
)
```

`report(...)` recibe `responseFormat` (default `NONE`) y lo propaga. La línea de `MainActivity` añade
`format=${event.responseFormat}` (el `outcome` ya imprime `FALLBACK`). El evento sigue sin clave,
`Authorization`, cuerpos ni títulos: solo añade un enum.

## Expected File Changes

**Modificar (código)**

- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — `RESPONSE_FORMAT_ENABLED`,
  `RESPONSE_FORMAT_JSON_OBJECT` y `requiresResponseFormatFallback(status)`; KDoc.
- `app/src/main/java/com/sazon/app/data/ai/OpenAiChat.kt` — `ResponseFormat` + campo opcional
  `responseFormat` con `@SerialName`/`@EncodeDefault(NEVER)`; KDoc corregida.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — costura
  `responseFormatEnabled`; `sendAttempt(..., includeFormat)`; rama de fallback en `execute`;
  `report(..., responseFormat)`.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiResponseFormat`,
  `AiCallOutcome.FALLBACK` y campo `responseFormat` en `AiCallEvent`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — formato del `Log.d("SazonAi", …)` con el modo.

**Crear / modificar (tests)**

- `app/src/test/java/com/sazon/app/data/ai/OpenAiChatTest.kt` — **nuevo**: serialización del DTO con
  `encodeDefaults = true` (Scenario 3): sin formato no aparece `response_format` (ni `null`); con
  `ResponseFormat.JSON_OBJECT` aparece `{"type":"json_object"}`.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — Scenarios 1, 2, 4, 5, 6
  y 7: cuerpo con/sin `response_format` (capturado del `MockEngine` real), fallback one-shot 400/422,
  no-fallback en 401/403/404/429/5xx, eventos y no-fuga.
- `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` — `requiresResponseFormatFallback`
  (400/422 `true`; 401/403/404/429/500/599 `false`) y `RESPONSE_FORMAT_ENABLED == true` por defecto.

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiRecipePayload.kt`,
`AiDecodeResult.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt` (sigue leyendo la
política, sin literales nuevos), `domain/**`, `data/generator/**`, `ui/**`, las bases Room,
`app/src/main/assets/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`,
`init.sh`, `scripts/**`, `DESIGN.md`, `docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). No hay pantalla, estado, microcopy, token ni acabado
  nuevos; el único cambio fuera de `data.ai` es el formato del **log** de `MainActivity`.
- Design source: not applicable. Screens or states affected: ninguno.
- New design artifact required: **no**. `docs/design/concepts/ai-response-format/` **no** es necesario y
  la feature **no** se bloquea por capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — párrafo de `data.ai` (matiza que ahora se envía
  `response_format {"type":"json_object"}` con fallback y kill switch), «Integrations» (sustituye «sin
  `response_format`, …» por el envío de `json_object` y la aclaración de que `json_schema` sigue
  fuera), «Testing and Verification» (tests del cuerpo con/sin formato y del fallback) y
  «Observability» (`AiCallEvent` ahora lleva `responseFormat` y el desenlace `FALLBACK`).
- `docs/risks-and-open-questions.md`: **update** — bullets «Errores y reintentos de la IA» y
  «Proveedor/modelo de IA»: `json_object` (forma) se usa con fallback; `json_schema`/structured outputs
  siguen descartados. Sin renumerar ni tocar tablas.
- `docs/adr/0003-ai-provider.md`: **update mínimo (nota)** — precisar que la app envía
  `response_format: json_object` (solo forma, con fallback) y **mantiene** el rechazo de
  `json_schema`/structured outputs forzados; la decisión de fondo no cambia.
- `docs/specs/real-ai-provider.md`: **update mínimo** — anotar en sus *Unknowns*/notas de la petición
  (líneas ~357, ~667, ~740) que `response_format: json_object` ya se envía y apunta a esta correctiva;
  sus escenarios y valores históricos no se reescriben.
- `docs/specs/ai-call-diagnostics.md`: **update mínimo** — anotar en su Non-Goal («**No** se añade
  `response_format`…») que la decisión quedó **resuelta** por `ai-response-format` (issue #74).
- `docs/domain-model.md`, `docs/domain/taxonomy.md`, `AGENTS.md`, `DESIGN.md`, `docs/design/**`: **not
  needed** — no cambian entidades, contrato del prompt, flujo, arranque ni dirección visual.
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. `AiProviderPolicy`: añadir `RESPONSE_FORMAT_ENABLED`, `RESPONSE_FORMAT_JSON_OBJECT` y
   `requiresResponseFormatFallback(status)` (sin tocar timeouts ni reintentos).
2. `OpenAiChat`: `ResponseFormat` y el campo opcional `responseFormat` con `@SerialName` +
   `@EncodeDefault(NEVER)`; corregir la KDoc.
3. `AiDiagnostics`: `AiResponseFormat`, `AiCallOutcome.FALLBACK` y campo `responseFormat` con default.
4. `DevExpertRecipeGenerator`: costura `responseFormatEnabled`; `sendAttempt(..., includeFormat)`; rama
   de fallback en `execute`; propagar `responseFormat` en `report`.
5. `MainActivity`: añadir `format=${event.responseFormat}` al `Log.d("SazonAi", …)`.
6. Tests: `OpenAiChatTest` (nuevo, Scenario 3), casos de formato/fallback en
   `DevExpertRecipeGeneratorTest` (Scenarios 1, 2, 4, 5, 6, 7) y `requiresResponseFormatFallback` en
   `AiProviderPolicyTest`.
7. `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
   verde; actualizar las cinco docs; el implementer registra estado/evidencia en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `AiProviderPolicy`: `RESPONSE_FORMAT_ENABLED = true`, `RESPONSE_FORMAT_JSON_OBJECT =
      "json_object"` y `requiresResponseFormatFallback(status) = status == 400 || status == 422`;
      timeouts/`isRetryableStatus`/backoff/`Retry-After` intactos; KDoc.
- [ ] `OpenAiChat`: `ResponseFormat` (+ companion `JSON_OBJECT`) y `ChatCompletionRequest` con
      `@SerialName("response_format") @EncodeDefault(EncodeDefault.Mode.NEVER) val responseFormat:
      ResponseFormat? = null`; KDoc actualizada.
- [ ] `AiDiagnostics`: `enum AiResponseFormat { JSON_OBJECT, NONE }`, `AiCallOutcome.FALLBACK` y
      `AiCallEvent.responseFormat: AiResponseFormat = NONE`; `report` lo propaga.
- [ ] `DevExpertRecipeGenerator`: `responseFormatEnabled` por constructor (default de la política);
      `sendAttempt(prompt, remaining, includeFormat)` monta `ResponseFormat.JSON_OBJECT` solo si
      `includeFormat`; rama de fallback (400/422, una vez, sin backoff, `attempt += 1`) antes de la
      lógica actual; `formatOf(includeFormat)` en todas las llamadas a `report`.
- [ ] `MainActivity`: `format=${event.responseFormat}` en el `Log.d("SazonAi", …)`.
- [ ] `OpenAiChatTest` (nuevo): serializar `ChatCompletionRequest` con `Json { ignoreUnknownKeys = true;
      encodeDefaults = true }`; sin formato **no** aparece `response_format` ni `"response_format":null`;
      con `JSON_OBJECT` aparece `{"type":"json_object"}`.
- [ ] `DevExpertRecipeGeneratorTest`: cuerpo con formato (Scenario 1) y sin formato con el interruptor
      apagado (Scenario 2, incluido `400/422` → 1 llamada); fallback 400 y 422 → 2 llamadas con/sin
      formato y eventos `FALLBACK`+`SUCCESS` (Scenario 4); 400/422 persistente → 2 llamadas y
      `HTTP_ERROR` (Scenario 5); 401/403/404 sin fallback y 429/5xx con reintento de transporte
      (Scenario 6); `responseFormat`/`FALLBACK` en los eventos y no-fuga (Scenario 7).
- [ ] `AiProviderPolicyTest`: `requiresResponseFormatFallback` (400/422 sí; resto no) y
      `RESPONSE_FORMAT_ENABLED` por defecto `true`.
- [ ] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
      verde (0 errores de lint y sin warnings nuevos en ficheros de la feature).
- [ ] Inspección de alcance: `git diff --stat` solo con lo previsto; `Recipe.kt`, `RecipeValidator.kt`,
      `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, build,
      Taxonomía, `seed-recipes.json` e `init.sh` intactos; sin dependencias nuevas.
- [ ] Docs durables (`technical-discovery.md`, `risks-and-open-questions.md`, nota en
      `adr/0003-ai-provider.md`, notas en `real-ai-provider.md` y `ai-call-diagnostics.md`).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).
- `OpenAiChatTest` — Scenario 3: **prueba negativa clave**: con `encodeDefaults = true`, un
  `ChatCompletionRequest` sin formato **no** emite `response_format` (ni `null`); con
  `ResponseFormat.JSON_OBJECT` emite `{"type":"json_object"}`.
- `DevExpertRecipeGeneratorTest` — Scenario 1: el cuerpo capturado del `MockEngine` contiene
  `"response_format":{"type":"json_object"}`; Scenario 2: con `responseFormatEnabled = false` el cuerpo
  no tiene la clave, una sola llamada y `400/422` → `HTTP_ERROR` en una llamada; Scenario 4: `400` y
  `422` → 2 llamadas (primera con formato, segunda sin) + evento `FALLBACK` (`httpStatus` correcto,
  `responseFormat = JSON_OBJECT`) y `SUCCESS` (`responseFormat = NONE`); Scenario 5: `400`/`422`
  persistente → exactamente 2 llamadas y `HTTP_ERROR` (sin tercera); Scenario 6: `401/403/404` → 1
  llamada sin fallback y `429`/`5xx` → reintento de transporte con formato; Scenario 7: eventos con el
  modo/`FALLBACK` y `toString()` sin `test-key`, `Bearer`/`Authorization` ni cuerpos.
- `AiProviderPolicyTest` — `requiresResponseFormatFallback(400)`/`(422)` ciertos y
  `(401)`/`(403)`/`(404)`/`(429)`/`(500)`/`(599)` falsos (caso negativo que no pasa en vacío);
  `RESPONSE_FORMAT_ENABLED` por defecto `true`.
- Regresión: los **50** tests de `data.ai` siguen contando y en verde (los nuevos suman); `RecipeValidatorTest`
  intacto; timeouts 120/150/240 y `MAX_ATTEMPTS`/backoff/`Retry-After` sin cambios.
- **Lint:** `bash init.sh` → 0 errores y sin warnings nuevos (los preexistentes del build se mantienen);
  `@OptIn(ExperimentalSerializationApi::class)` sin warnings.
- **Inspección:** `git diff --stat` limitado a lo previsto; `grep` de `response_format` fuera de
  `OpenAiChat.kt`/`DevExpertRecipeGenerator.kt`/tests sin resultados inesperados; `Recipe.kt`,
  `RecipeValidator.kt`, `AiRecipePayload.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, build,
  Taxonomía, `seed-recipes.json` e `init.sh` sin cambios.
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):** con
  `devexpert.apiKey` válida, `bash scripts/gradle.sh assembleDebug` y ejecutar; generar/buscar y ver en
  logcat la línea `SazonAi` con `format=JSON_OBJECT` (o `FALLBACK`) y las recetas reales; con
  `RESPONSE_FORMAT_ENABLED = false` confirmar que no cambia nada; clave ausente o vaciada → modo semilla.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; `MockEngine` cubre el transporte y el
  fallback en JVM, y lo visual/manual lo revisa la persona). `init.sh` **no** cambia.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.*` con recuento de tests y 0 fallos
  (esperado: 50 preexistentes en `data.ai` más los nuevos, sin eliminar ninguno).
- Nombres y resultado de los tests nuevos: cuerpo con/sin `response_format`, la prueba negativa de que
  nunca se serializa `null`, el fallback one-shot con `400` **y** `422`, el no-fallback en
  `401/403/404/429/5xx` y los eventos con `responseFormat`/`FALLBACK` sin fugas.
- `git diff --stat` de lo tocado y confirmación de que `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, build,
  Taxonomía, `seed-recipes.json`, `init.sh` y `DESIGN.md` no cambian.
- Resultado **manual** del emulador (logcat `SazonAi` con `format=…`/`FALLBACK`; recetas reales;
  interruptor apagado → comportamiento idéntico; clave ausente → modo semilla).
- Docs actualizadas y anotación de estado/evidencia en `feature_list.json` y `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **`json_object`, no `json_schema`**: la persona verificó HTTP 200 con `chat`+`json_object`; `chat`+
  `json_schema` da 400 y `chat-pro`+`json_schema` tarda 97 s. Se envía solo la forma (JSON parseable);
  el **dominio** sigue en manos de `RecipeValidator` (puerta dura). Decisión de la persona, no se
  reabre.
- **Fallback one-shot ante 400/422**: reintentar **una vez** sin `response_format`. Es un camino
  aparte del reintento de transporte: `400/422` siguen sin ser `isRetryableStatus` y **ningún** otro
  estado dispara el fallback. El fallback consume el segundo intento (`MAX_ATTEMPTS = 2`) y el deadline
  total sigue acotando la operación; como máximo, una petición de fallback por operación.
- **Interruptor en `AiProviderPolicy` (`RESPONSE_FORMAT_ENABLED`)** más costura de constructor
  (`responseFormatEnabled`): la producción lee la constante y los tests inyectan el valor apagado. Un
  flag **de runtime/remoto** (que se apague sin recompilar) exigiría infraestructura nueva (no hay
  config remota ni ajustes persistentes para esto) y queda **fuera de alcance**; el fallback ya hace
  segura la degradación por llamada.
- **`@EncodeDefault(EncodeDefault.Mode.NEVER)`** en el campo nullable en lugar de `explicitNulls =
  false` global o un DTO aparte: anula `encodeDefaults = true` solo para esa propiedad, omite la clave
  cuando no se usa y no cambia el `Json` del cliente ni duplica la petición.
- **Modo del evento como enum** (`AiResponseFormat`) y desenlace nuevo `AiCallOutcome.FALLBACK`:
  mínimo, aditivo (con default), sin texto del modelo, y suficiente para que el logcat diga el modo y
  si saltó el fallback.

### Unknowns

- **Latencia con `json_object`**: medido `chat`+`json_object` 85,3 s frente a `chat` 28,0–46,6 s. Cabe
  en socket 120 s y en el deadline 240 s, pero reduce el margen; una medición por encima de 120 s se
  degradará honestamente como `TIMEOUT` (socket) y es revisable en el mismo sitio.
- **Un 400/422 no relacionado con el formato** (p. ej. prompt/parámetro inválido) también dispararía
  el fallback: cuesta **una** petición extra y el evento `FALLBACK` deja el rastro. Se acepta como
  coste; el fallback no empeora el resultado.
- **`FALLBACK` no se reintenta si el segundo intento también devuelve 400/422** (una sola vez por
  operación): es el comportamiento pedido; reintentar el formato en bucle no aporta.
- **`response_format` es específico del proveedor**: si DevExpert dejara de aceptarlo, el fallback
  (o el interruptor) lo neutraliza; cambiar de proveedor sigue siendo feature + ADR aparte.
- **Log de `MainActivity`**: único cambio fuera de `data.ai`; mantiene `Log.d` local (sin telemetría).

## Validator Checklist

- [ ] La petición incluye `"response_format":{"type":"json_object"}` cuando el interruptor está activo
      y lo **omite** (sin `null`) cuando está apagado; el interruptor es `AiProviderPolicy.RESPONSE_FORMAT_ENABLED`
      y la producción lo lee por el default del constructor.
- [ ] La prueba negativa demuestra que **nunca** se serializa `response_format: null` con
      `encodeDefaults = true` (DTO y cuerpo real del `MockEngine`).
- [ ] El fallback se dispara **solo** ante `400`/`422`, **una sola vez**, sin `response_format` en la
      segunda petición; ningún otro estado lo dispara y `isRetryableStatus`/los timeouts no cambian.
- [ ] Con el interruptor apagado el comportamiento es **idéntico** al actual (una llamada por
      operación, `400/422` → `HTTP_ERROR`, `429`/`5xx` → reintento de transporte).
- [ ] `AiCallEvent` distingue el modo (`responseFormat = JSON_OBJECT`/`NONE`) y el fallback
      (`outcome = FALLBACK`) y **nunca** contiene clave, `Authorization`, cuerpos ni títulos.
- [ ] Alcance: solo `data.ai` (código + sus tests), `MainActivity` (log) y las cinco docs;
      `Recipe.kt`, `RecipeValidator.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`,
      `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, Room, Taxonomía, `seed-recipes.json`, build,
      `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos) y regresión de los 50
      tests de `data.ai` y de `RecipeValidatorTest`.
- [ ] Docs actualizadas (`technical-discovery.md`, `risks-and-open-questions.md`, nota en
      `adr/0003-ai-provider.md` y notas en `real-ai-provider.md`/`ai-call-diagnostics.md`).
- [ ] `Visual Design Impact` sin capturas (no hay UI) y la verificación **manual** en emulador anotada
      como tal (logcat con `format=…`/`FALLBACK`; interruptor apagado; clave ausente → modo semilla),
      no automatizable desde WSL.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por implementer/validador/orquestador.
