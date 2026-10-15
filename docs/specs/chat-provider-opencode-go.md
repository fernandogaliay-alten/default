# Feature Implementation Spec: Chat por OpenCode Go (`deepseek-v4.1-flash`); las imágenes siguen en DevExpert

## Source Feature

- `id`: `chat-provider-opencode-go`
- `area`: `integration` (no hay UI: es una correctiva de `real-ai-provider`)
- `depends_on`: `["real-ai-provider"]` (satisfecha: `accepted`)
- `status`: `not_started` en el momento de planificar
- `source`: `feature_list.json` (entrada de `chat-provider-opencode-go`; issue #141)
- Correctiva pedida por la persona (2026-10-14) por **agotamiento de la cuota semanal de DevExpert**:
  **solo el chat** cambia de proveedor; la **generación de imágenes de plato sigue en DevExpert**
  (`docs/adr/0005-ai-dish-images.md`).
- Verificación exigida (7 puntos de la feature, detallados en los escenarios 1-5): endpoint/cabeceras/
  modelo del chat en Go; imágenes en DevExpert con base **propia** y su clave; dos claves desde
  `local.properties` (nunca versionadas); degradación a modo semilla sin clave de chat; cobertura JVM
  con `MockEngine`; `bash init.sh` verde.

## Goal

Que las **tres operaciones de chat** que hoy sirve `DevExpertRecipeGenerator` (sugerir por
preferencias, buscar por texto y generar desde ingredientes) se sirvan contra **OpenCode Go**
(`https://opencode.ai/zen/go/v1`, modelo `deepseek-v4.1-flash`) con su **propia clave**
(`opencode.apiKey` → `BuildConfig.OPENCODE_API_KEY`) y las **dos cabeceras que el proveedor exige**
(`x-opencode-session` obligatoria y un `User-Agent` propio), manteniendo **intactos** el contrato
OpenAI del wire (`OpenAiChat`), la política de timeouts/reintentos/`response_format`/fallback, el
diagnóstico local y la degradación a modo semilla.

En paralelo, la **imagen de plato deja de colgar de la base del chat**: `DishImagePolicy` fija su
**propia** base de DevExpert (`https://inference.devexpert.io/v1`, alias `image`/`image-edit`) y su
propia clave (`devexpert.apiKey` → `BuildConfig.DEVEXPERT_API_KEY`), de modo que el cambio de proveedor
del chat **no puede** arrastrar las imágenes. El dominio, `RecipeValidator`, `Recipe`, `PromptBuilder`
y la Taxonomía **no se tocan**.

## Non-Goals

- **No se toca el dominio**: `domain/**`, `Recipe.kt`, `RecipeValidator.kt` (puerta dura), `RecipeIds`,
  `PromptBuilder`, `DishImageRequest`/`DishImagePrompt`/`DishImageCacheKey` y `docs/domain/taxonomy.json`
  quedan intactos. El wire del chat ya es compatible OpenAI (`OpenAiChat`): cambia **base + modelo +
  cabeceras + clave**, no el contrato.
- **No cambia la cadena de imagen**: `data.ai.image` (`DevExpertDishImageGenerator`, multipart
  `/images/edits`, respaldo por texto), `data.image` (caché, tope, interruptor), assets semilla,
  `ui.artwork` y `MainActivity` en su parte de imagen siguen **exactamente** igual salvo recibir la
  clave desde el mismo `BuildConfig.DEVEXPERT_API_KEY` de siempre.
- **No se recalibra la política**: timeouts (10/120/150/240 s), `MAX_ATTEMPTS = 2`,
  `RETRY_BACKOFF_MILLIS`, `RETRY_AFTER_MAX_MILLIS`, `isRetryableStatus`, `requiresResponseFormatFallback`
  y `RESPONSE_FORMAT_ENABLED` no cambian de valor. Solo se matiza el KDoc de la calibración (medida
  contra el proveedor anterior; se conserva como techo conservador).
- **No se renombran ficheros ni clases**: `DevExpertRecipeGenerator`/`DevExpertRecipeGeneratorTest`
  conservan su nombre (es deuda de nomenclatura conocida y acotada: se documenta, no se arregla aquí);
  tampoco se renombra `DevExpertDishImageGenerator`.
- **No se añade dependencia, cliente HTTP ni SDK**: se reutiliza el `aiClient` compartido
  (`AiHttpClientFactory`) y el engine OkHttp ya presentes; no se toca `gradle/libs.versions.toml`.
- **No se usa el endpoint Console** (`https://opencode.ai/inference/...`): responde
  `402 Insufficient account funds` (verificado a mano el 2026-10-14). Objetivo = Go.
- **No hay ADR nuevo**: se **enmiendan** `docs/adr/0003-ai-provider.md` y `docs/adr/0005-ai-dish-images.md`.
- **Sin UI, sin capturas, sin `docs/design/concepts/<feature-id>/`** (no hay cambio visual).

## Job Story

Cuando la cuota semanal de DevExpert se agota y el chat deja de responder,
quiero que el chat pase a OpenCode Go con su propia clave y sus cabeceras obligatorias,
para que generar, buscar y sugerir recetas sigan funcionando sin arrastrar la generación de imágenes,
que debe seguir en DevExpert.

## Users And Permissions

- Persona usuaria de la app (única, local-first): sin permisos nuevos; `INTERNET` ya está declarado en
  el manifiesto. Sin cuentas, login ni roles.
- Build de desarrollo/CI: lee `local.properties` (no versionado) en tiempo de configuración; **nunca**
  imprime el valor ni la ruta de ninguna clave.

## Acceptance Scenarios

### Scenario 1: el chat va a OpenCode Go con cabeceras y modelo (verificación exigida)

Given una petición de cualquiera de las tres operaciones con `opencode.apiKey` no vacía,
When el generador envía el intento HTTP,
Then la petición es `POST https://opencode.ai/zen/go/v1/chat/completions`, lleva
`Authorization: Bearer <clave>`, `x-opencode-session: <constante no vacía>` y un `User-Agent` no vacío,
y el cuerpo lleva `"model":"deepseek-v4.1-flash"` con el mismo `messages`/`response_format` de hoy.

### Scenario 2: la clave de chat ausente degrada sin red (verificación exigida)

Given `opencode.apiKey` vacía o ausente (`BuildConfig.OPENCODE_API_KEY == ""`),
When se pide una generación,
Then se lanza `AiUnavailableException(MISSING_KEY)` **sin ninguna petición HTTP** y el mensaje cita la
clave de chat (`opencode.apiKey`), no la de DevExpert.

### Scenario 3: las imágenes siguen en DevExpert y desacopladas del chat (verificación exigida)

Given el cambio de base del chat,
When se inspecciona/ejecuta la política y el adaptador de imagen,
Then `DishImagePolicy.endpointUrl()` == `https://inference.devexpert.io/v1/images/generations` y
`editsUrl()` == `https://inference.devexpert.io/v1/images/edits` (literales, con la base **propia** de
`DishImagePolicy`, que **no** es `AiProviderPolicy.BASE_URL`), y una petición de imagen **no** lleva la
cabecera `x-opencode-session` (que sería un fallo de acoplamiento del cliente compartido).

### Scenario 4: dos claves, una pantalla de inyección cada una (verificación exigida)

Given `local.properties` con `opencode.apiKey` y `devexpert.apiKey`,
When se compila,
Then `BuildConfig.OPENCODE_API_KEY` alimenta **solo** al generador de chat y
`BuildConfig.DEVEXPERT_API_KEY` **solo** al adaptador de imagen; si falta una propiedad, el valor es
`""` y su consumidor degrada en silencio (chat → semilla; imagen → `null` → ficha tipográfica).

### Scenario 5: el resto del comportamiento no cambia (inspección + tests)

Given el comportamiento aceptado de `real-ai-provider`/`ai-response-format`/`ai-verbose-diagnostics`,
When se ejecuta la suite,
Then timeouts, reintento de transporte, `Retry-After`, fallback one-shot de `400/422`, clasificación de
fallos, diagnóstico (`AiCallEvent`, `onDescriptors`, `verboseDiagnostics`) y modo semilla siguen
verdes sin cambios en sus tests.

## Repository Research

### Files Inspected

- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — base (`BASE_URL` =
  DevExpert), `MODEL_ALIAS = "chat"`, `CHAT_COMPLETIONS_PATH`, timeouts, reintentos y
  `RESPONSE_FORMAT_ENABLED`; **aquí vive el cambio de chat**.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — `sendAttempt` (único punto
  que monta la petición: `client.post(AiProviderPolicy.endpointUrl())`, `Authorization`, `timeout`,
  `ChatCompletionRequest(model = model, …)`), el mensaje de `MISSING_KEY` (cita `devexpert.apiKey`) y
  el default `model = AiProviderPolicy.MODEL_ALIAS`; **aquí viven cabeceras y clave de chat**.
- `app/src/main/java/com/sazon/app/data/ai/OpenAiChat.kt` — DTOs del envelope OpenAI (`model`,
  `messages`, `response_format`); **no cambia** (el wire ya encaja).
- `app/src/main/java/com/sazon/app/data/ai/AiHttpClientFactory.kt` — cliente Ktor/OkHttp con
  `HttpTimeout` e `install(ContentNegotiation)`; `expectSuccess = false`; **no cambia** (los headers
  van por petición, ver Technical Approach).
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — **hoy** deriva su base de
  `AiProviderPolicy.BASE_URL` (`endpointUrl()`/`editsUrl()`) y lo documenta ("misma base que el chat");
  alias `image`/`image-edit`; **aquí vive el desacople**.
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — `POST` con
  `Authorization: Bearer <clave de DevExpert>`; **no cambia** (recibe la clave por constructor).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — cableado manual: `aiClient` **compartido** por el
  generador de chat (línea 79-111, clave `DEVEXPERT_API_KEY` en la 80) y por la cadena de imagen
  (línea 159-175, clave `DEVEXPERT_API_KEY` en la 164); comentario de la línea 70 ("Proveedor de IA
  real (DevExpert Inference…)").
- `app/build.gradle.kts` — lee `devexpert.apiKey` de `local.properties` y publica
  `BuildConfig.DEVEXPERT_API_KEY` con escapado; convención a replicar para `opencode.apiKey`.
- Tests: `AiProviderPolicyTest.kt` (asserta endpoint DevExpert y `"chat"`), `OpenAiChatTest.kt`
  (`MODEL_ALIAS`), `DevExpertRecipeGeneratorTest.kt` (asserta URL DevExpert, `model == "chat"`,
  `Bearer $KEY`, `MISSING_KEY`), `image/DishImagePolicyTest.kt` (asserta las URLs de imagen **como**
  `AiProviderPolicy.BASE_URL + …`), `image/DevExpertDishImageGeneratorTest.kt` (asserta las URLs de
  DevExpert y captura las peticiones con `MockEngine`).
- Docs durables: `docs/technical-discovery.md` (tabla de claves línea ~43; párrafo del adaptador de
  chat ~444-498; bullet `Integrations` ~587-590; bullet del adaptador de imagen ~649-662; `Constraints`
  de coste/cuota ~1045), `docs/domain-model.md` (~93), `docs/risks-and-open-questions.md` (~86),
  `docs/adr/0003-ai-provider.md`, `docs/adr/0005-ai-dish-images.md`.
- `local.properties` (no versionado) — ya contiene `devexpert.apiKey` **y** `opencode.apiKey`.

### Existing Patterns To Follow

- **Política en un único sitio** (`AiProviderPolicy`, `DishImagePolicy`): constantes y funciones puras,
  testables en JVM; el productor las lee y el adaptador las recibe por constructor.
- **Clave por `BuildConfig`** desde `local.properties`, con escapado (`replace("\\", "\\\\")` +
  `replace("\"", "\\\"")`) y valor `""` si falta la propiedad; nunca se imprime.
- **Cabeceras por petición** en el builder de Ktor (`header(HttpHeaders.Authorization, …)`), no en el
  cliente, porque el cliente es **compartido** con la cadena de imagen.
- **Degradación a modo semilla** vía `AiUnavailableException` tipada; `MISSING_KEY` **sin** petición.
- **Tests de adaptador con `MockEngine`**: se capturan URL, método, cabeceras y cuerpo de la petición
  real (`MockAi`/`lastRequest`), sin red ni clave real (`test-key`).
- **Desviación de capturas no aplica**: no es UI (misma clase que `real-ai-provider`).

### Current Gaps

- `DishImagePolicy` **hoy** acopla su base a `AiProviderPolicy.BASE_URL`: si solo se cambia la base del
  chat, las imágenes irían a OpenCode Go y fallarían **en silencio** (`null` → ficha tipográfica). Es el
  riesgo principal de la feature.
- El `aiClient` es **único** para chat e imagen: unas cabeceras por defecto en el cliente filtrarían
  `x-opencode-session` a DevExpert.
- `app/build.gradle.kts` **no** lee todavía `opencode.apiKey`; `MainActivity` usa
  `DEVEXPERT_API_KEY` también en el chat, y el mensaje de `MISSING_KEY` cita `devexpert.apiKey`.
- Varios tests y docs fijan literalmente el endpoint/modelo/clave de chat de DevExpert.
- **Unknown (no bloquea)**: de `x-opencode-session` está **verificado** que el proveedor exige su
  **presencia** (sin ella `400 MissingSessionID`; con ella el chat responde `200`), pero **no** está
  verificado que exija un valor concreto. La spec fija una constante estable y no sensible; si el
  proveedor rechazara el valor, es un cambio de **una línea**.

## Technical Approach

### 1. Política del chat — `data/ai/AiProviderPolicy.kt` (modificar)

- `BASE_URL` → `"https://opencode.ai/zen/go/v1"` (KDoc: proveedor del **chat**; la imagen tiene su
  propia base en `DishImagePolicy`). `CHAT_COMPLETIONS_PATH` y `endpointUrl()` no cambian de forma: el
  endpoint resultante es `https://opencode.ai/zen/go/v1/chat/completions`.
- `MODEL_ALIAS` → **renombrado** a `MODEL_ID` con valor `"deepseek-v4.1-flash"`: OpenCode Go **no
  expone alias**, el id es literal y queda fijado (renombrar evita un nombre que miente). El contraste
  con `DishImagePolicy.MODEL_ALIAS`/`EDIT_MODEL_ALIAS` (DevExpert **sí** usa alias) es deliberado y se
  documenta. Sitios: `AiProviderPolicy`, el default `model` de `DevExpertRecipeGenerator`,
  `AiProviderPolicyTest` y `OpenAiChatTest`.
- Nuevas constantes (un único sitio, puras):
  - `const val SESSION_HEADER = "x-opencode-session"` — nombre de la cabecera obligatoria.
  - `const val SESSION_ID = "sazon-android"` — valor **estable y no sensible**; **nunca** la clave.
  - `const val USER_AGENT = "Sazon-Android/1.0"` — `User-Agent` propio recomendado por el proveedor.
- **No cambian** timeouts, `MAX_ATTEMPTS`, backoff, `Retry-After`, `isRetryableStatus`,
  `requiresResponseFormatFallback`, `RESPONSE_FORMAT_ENABLED`, `NEUTRAL_CONSTRAINTS` ni
  `transportFailureReason`. El KDoc de `SOCKET_TIMEOUT_MILLIS` se matiza: la calibración (115,8 s de
  `mimo-v2.6-pro`, ~46,6 s del alias `chat`) se midió contra el proveedor **anterior** y se conserva
  como techo **conservador** hasta tener mediciones de Go (no se recalibra en esta feature).

### 2. Petición del chat — `data/ai/DevExpertRecipeGenerator.kt` (modificar)

- En `sendAttempt`, junto al `Authorization` existente, añadir **por petición**:
  `header(AiProviderPolicy.SESSION_HEADER, AiProviderPolicy.SESSION_ID)` y
  `header(HttpHeaders.UserAgent, AiProviderPolicy.USER_AGENT)`.
- **No** usar `defaultRequest`/defaults de `AiHttpClientFactory`: el `aiClient` es **compartido** con
  `DevExpertDishImageGenerator`, así que unos defaults filtrarían `x-opencode-session` a DevExpert.
- Default del constructor: `model: String = AiProviderPolicy.MODEL_ID`.
- Mensaje de `MISSING_KEY`: pasa a citar la clave de chat (`opencode.apiKey`).
- KDoc de clase: dice que el chat se sirve contra **OpenCode Go** (base/modelo/cabeceras), que el
  **nombre del fichero/clase es histórico** (nació sobre DevExpert) y que las **imágenes** son
  `DevExpertDishImageGenerator`; degradación a semilla y política intactas.

### 3. Desacople de la imagen — `data/ai/image/DishImagePolicy.kt` (modificar)

- Nueva constante **propia**: `const val BASE_URL = "https://inference.devexpert.io/v1"`.
- `endpointUrl()`/`editsUrl()` pasan a usarla; se **elimina** el uso (y el `import`) de
  `AiProviderPolicy`, reescribiendo las menciones del KDoc a texto/enlace cualificado ("base propia del
  proveedor de imagen (DevExpert)"; "mismo patrón que `AiProviderPolicy.RESPONSE_FORMAT_ENABLED`").
- Alias `image`/`image-edit`, rutas, `IMAGE_SIZE`, `TARGET_WIDTH_PX`, `WEBP_QUALITY`, `TIMEOUTS`,
  `MAX_ATTEMPTS = 1`, `EDIT_FALLBACK_STATUSES` y `scaledSize` **no cambian**.

### 4. Claves — `app/build.gradle.kts` y `MainActivity.kt` (modificar)

- `build.gradle.kts`: leer `opencode.apiKey` de `local.properties` (mismo bloque `Properties`, mismo
  escapado) y publicar `buildConfigField("String", "OPENCODE_API_KEY", …)`. **Se conserva**
  `DEVEXPERT_API_KEY`.
- `MainActivity`: el generador de chat usa `BuildConfig.OPENCODE_API_KEY` (línea ~80); el adaptador de
  imagen **mantiene** `BuildConfig.DEVEXPERT_API_KEY` (línea ~164). Se actualizan los comentarios de
  cableado (chat = OpenCode Go; imagen = DevExpert) sin cambiar la composición compartida del
  `aiClient`.

### 5. Tests (modificar; sin red y sin clave real)

- `AiProviderPolicyTest`: endpoint literal de Go, `MODEL_ID == "deepseek-v4.1-flash"`,
  `SESSION_HEADER`/`SESSION_ID`/`USER_AGENT` no vacíos y `SESSION_ID != clave`; el resto de tests (que
  no fijan base/modelo) intactos.
- `OpenAiChatTest`: usa `MODEL_ID`; sin cambios de comportamiento.
- `DevExpertRecipeGeneratorTest`: en el camino feliz, URL == la de Go, `Bearer $KEY`,
  `x-opencode-session == AiProviderPolicy.SESSION_ID`, `User-Agent == AiProviderPolicy.USER_AGENT`,
  `model == "deepseek-v4.1-flash"`; los escenarios de reintento/fallback/deadline/diagnóstico no
  cambian de aserciones (solo de URL/modelo si los fijaban).
- `image/DishImagePolicyTest`: URLs literales de DevExpert; **nueva aserción negativa de desacople**
  (`assertNotEquals(AiProviderPolicy.BASE_URL, DishImagePolicy.BASE_URL)`); la comparación de timeouts
  imagen < chat se conserva.
- `image/DevExpertDishImageGeneratorTest`: las URLs de DevExpert no cambian; **nueva aserción negativa**
  de que la petición de imagen **no** lleva `AiProviderPolicy.SESSION_HEADER`.
- No se crean suites nuevas si basta con editar las existentes (evita ruido en el conteo del gate).

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — modificar; base Go, `MODEL_ID`,
  cabeceras (`SESSION_HEADER`/`SESSION_ID`/`USER_AGENT`) y KDoc.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — modificar; dos cabeceras por
  petición, default `MODEL_ID`, mensaje de `MISSING_KEY`, KDoc.
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — modificar; `BASE_URL` propia de
  DevExpert y fin del uso de la base del chat.
- `app/build.gradle.kts` — modificar; `opencode.apiKey` → `BuildConfig.OPENCODE_API_KEY` (conserva
  `DEVEXPERT_API_KEY`).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar; clave de chat y comentarios.
- `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt`,
  `…/data/ai/OpenAiChatTest.kt`, `…/data/ai/DevExpertRecipeGeneratorTest.kt`,
  `…/data/ai/image/DishImagePolicyTest.kt`, `…/data/ai/image/DevExpertDishImageGeneratorTest.kt` —
  modificar; literales del chat + aserciones de desacople.
- `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md`,
  `docs/adr/0003-ai-provider.md`, `docs/adr/0005-ai-dish-images.md` — modificar (ver abajo).
- `feature_list.json` y `PROGRESS.md` — **solo el implementer/orquestador**, con el estado y la
  evidencia (no los toca el planner).

Ficheros que **no** deben aparecer en el diff: `domain/**`, `Recipe.kt`, `RecipeValidator.kt`,
`PromptBuilder.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `AiRecipePayload.kt`, `AiRecipeIds.kt`,
`AiDiagnostics.kt`, `data/ai/image/DevExpertDishImageGenerator.kt`, `data/image/**`, `ui/**`,
`docs/domain/taxonomy.json`, `app/src/main/assets/**`, `gradle/libs.versions.toml`, `init.sh`,
`scripts/**`, `local.properties` (nunca se versiona) y `docs/adr/0006*`.

## Visual Design Impact

- UI involved: no. No cambia ninguna pantalla, estado, microcopy, geometría ni token.
- Design source: not applicable. **No** se crea `docs/design/concepts/chat-provider-opencode-go/` y no
  se abre revisión visual. `DESIGN.md` **no** requiere cambios (solo menciona
  `DevExpertDishImageGenerator`, que no cambia).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — la integración de IA deja de ser "una sola" de DevExpert:
  chat = OpenCode Go (base, `POST /chat/completions`, `Bearer OPENCODE_API_KEY`, modelo
  `deepseek-v4.1-flash`, cabeceras `x-opencode-session` + `User-Agent`), imagen = DevExpert con base
  **propia** (`DishImagePolicy.BASE_URL`); tabla de claves con las **dos**; párrafo del adaptador de
  chat y `Constraints` de cuota (DevExpert semanal + Go compartida con el harness).
- `docs/domain-model.md`: **update** — la "Implementación real" del puerto `RecipeGenerator`
  (`DevExpertRecipeGenerator`) pasa a citar OpenCode Go.
- `docs/adr/0003-ai-provider.md`: **update** — la decisión de proveedor se parte en **chat (OpenCode
  Go)** e **imagen (DevExpert)**, con una nota fechada (2026-10-14, issue #141) que registra el motivo
  (cuota agotada), el endpoint Console descartado (`402`) y que la predicción "cambiar de proveedor es
  un cambio en una sola capa" se cumplió (cero cambios en el dominio). Se mantiene: HTTP directo sin
  SDK, `response_format`/fallback, sin `json_schema`, clave por `BuildConfig`, modo semilla.
- `docs/adr/0005-ai-dish-images.md`: **update** — nota de que la base de imagen es **propia**
  (`DishImagePolicy.BASE_URL`, DevExpert) y **no** se deriva de la base del chat; nada más del ADR cambia.
- `docs/risks-and-open-questions.md`: **update** — el bullet "Proveedor/modelo de IA" pasa a describir
  chat = OpenCode Go + imagen = DevExpert; **riesgo nuevo** registrado: Go está pensado para agentes de
  programación y **monitoriza el abuso**, y su cuota (5 h/semana/mes) es **compartida con el harness**
  de este repo, además del `402` del endpoint Console (sin saldo) y de que el id del modelo está
  **fijado** (si Go lo renombra, el chat degrada a semilla hasta cambiar una constante).
- `AGENTS.md`: **not needed** — no cambian el flujo de agentes, el camino de arranque ni las reglas
  operativas (la verificación sigue siendo `bash init.sh`).
- `CONTEXT.md`, `DESIGN.md`, `docs/build-brief.md`, `docs/domain/taxonomy.md`,
  `docs/ai-dish-images-proposal.md` y `docs/backend-proposal.md` (no aceptado): **not needed**.

## Implementation Plan

1. `AiProviderPolicy`: base Go, `MODEL_ID`, `SESSION_HEADER`/`SESSION_ID`/`USER_AGENT` y KDoc.
2. `DevExpertRecipeGenerator`: cabeceras por petición, default `MODEL_ID`, mensaje de `MISSING_KEY`,
   KDoc (nombre histórico).
3. `DishImagePolicy`: `BASE_URL` propia de DevExpert y fin del acoplamiento con la base del chat.
4. `build.gradle.kts` + `MainActivity`: `OPENCODE_API_KEY` (chat) y `DEVEXPERT_API_KEY` (imagen).
5. Tests de `data.ai` y `data.ai.image` (incluidas las dos aserciones negativas).
6. Docs durables (technical-discovery, domain-model, risks, ADR 0003 y 0005).
7. `bash init.sh` verde + tirada focal; registrar evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `AiProviderPolicy`: `BASE_URL` → OpenCode Go, `MODEL_ALIAS` → `MODEL_ID =
      "deepseek-v4.1-flash"`, constantes de cabecera/UA y KDoc (incluida la matización de la calibración).
- [ ] `DevExpertRecipeGenerator.sendAttempt`: añadir `x-opencode-session` y `User-Agent` **por
      petición**; default `MODEL_ID`; mensaje de `MISSING_KEY` con `opencode.apiKey`; KDoc.
- [ ] `DishImagePolicy`: `BASE_URL` propia + `endpointUrl()`/`editsUrl()`; retirar el uso del import de
      `AiProviderPolicy` y reescribir las menciones del KDoc.
- [ ] `app/build.gradle.kts`: leer `opencode.apiKey` y publicar `BuildConfig.OPENCODE_API_KEY`
      (conservar `DEVEXPERT_API_KEY`).
- [ ] `MainActivity`: chat con `OPENCODE_API_KEY`, imagen con `DEVEXPERT_API_KEY`, comentarios.
- [ ] Actualizar los 5 ficheros de test (`AiProviderPolicyTest`, `OpenAiChatTest`,
      `DevExpertRecipeGeneratorTest`, `DishImagePolicyTest`, `DevExpertDishImageGeneratorTest`) con los
      literales nuevos y las dos aserciones negativas.
- [ ] Actualizar las 5 docs durables enumeradas arriba.
- [ ] `bash init.sh` (test + lint) en verde; `git diff --name-only` sin ficheros fuera de la lista.
- [ ] Registrar evidencia (gate, greps, diff) y el estado en `feature_list.json`/`PROGRESS.md`.

## Verification Plan

- **Gate estándar**: `bash init.sh` (ejecuta `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`) → exit 0. **No** debe arrancar procesos largos y **no** se invoca `./gradlew`
  directamente ni `--stop`. Si sale `UP-TO-DATE`, el implementer puede forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y `bash scripts/gradle.sh lint --rerun-tasks`
  y parsear los XML/SARIF a mano.
- **Tirada focal**: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
  BUILD SUCCESSFUL (cubre `data.ai` y `data.ai.image`).
- **Comprobaciones literales** (en los tests, no solo por grep): endpoint de chat ==
  `https://opencode.ai/zen/go/v1/chat/completions`; `Authorization: Bearer <clave>`;
  `x-opencode-session` presente e igual a la constante; `User-Agent` presente;
  `model == "deepseek-v4.1-flash"`; endpoint/edits de imagen == literales de DevExpert;
  `DishImagePolicy.BASE_URL != AiProviderPolicy.BASE_URL`; petición de imagen **sin**
  `x-opencode-session`.
- **Baseline y delta**: el implementer re-deriva la baseline en `HEAD` (último gate registrado: 98
  suites / 738 tests / 0 fallos / 0 errores / 0 saltados) y reporta el delta real (se esperan **0
  suites nuevas** y un delta pequeño de tests). Lint: 0 errores y 13 warnings preexistentes (0 bajo
  `app/src`).
- **Greps de alcance**: `grep -rn "inference.devexpert.io" app/src/main/java/com/sazon/app/data/ai/`
  → solo debe aparecer en `image/DishImagePolicy.kt`; `grep -rn "devexpert.apiKey" app/src` → 0;
  `grep -rn "MODEL_ALIAS" app/src/main/java/com/sazon/app/data/ai` → 0 (queda solo en `image/`).
- **E2E**: **no aplica**. Este repo no tiene comando E2E persistente; no hay cambio de
  comportamiento de UI (no hay pantalla afectada) y la verificación canónica es JVM.
- **Manual (persona, tras el merge; no automatizable desde WSL)**: con la clave válida, generar/buscar
  y comprobar HTTP 200 en logcat (`SazonAi`); con `opencode.apiKey` vaciada, modo semilla sin fallar;
  con el interruptor de imágenes encendido, que una imagen se sigue generando/ajustando en DevExpert.

## Evidence To Capture

- `bash init.sh` (exit 0) y, si aplica, las tiradas `--rerun-tasks`; parseo propio de
  `app/build/test-results/testDebugUnitTest/TEST-*.xml` (suites/tests/fallos/errores/saltados + delta
  frente a la baseline re-derivada) y resumen del SARIF de lint (0 errores; 13 warnings preexistentes;
  0 bajo `app/src`).
- `git diff --name-only` con solo los ficheros previstos y confirmación de que las dos claves no están
  en el diff ni en ningún fichero versionado; greps de alcance (endpoint de chat en Go; DevExpert solo
  en la política de imagen; 0 `MODEL_ALIAS` en `data.ai`; 0 `x-opencode-session` en las peticiones de
  imagen).
- Datos completos en `feature_list.json` (`evidence`) y resumen en `PROGRESS.md`, incluidos el gate, el
  delta y la verificación manual pendiente de la persona.

## Decisions And Unknowns

### Decisiones cerradas (el implementer no las reabre)

- Solo el **chat** cambia de proveedor; la **imagen** sigue en DevExpert con base y clave propias.
- Las cabeceras de Go van **por petición**, nunca como defaults del cliente compartido.
- `x-opencode-session` lleva una constante estable y **no sensible** (`"sazon-android"`), nunca la clave.
- `MODEL_ALIAS` (chat) se **renombra** a `MODEL_ID` porque Go no expone alias; la imagen **conserva**
  sus alias (`image`/`image-edit`).
- No se renombran clases/ficheros (`DevExpertRecipeGenerator` queda como nombre histórico documentado).
- No se recalibran timeouts/reintentos: la política medida con DevExpert se conserva como techo.

### Unknowns y riesgos (no bloquean)

- **Valor exacto de `x-opencode-session`**: verificado que el proveedor exige su presencia; no
  verificado que imponga un formato. Mitigación: constante en un único sitio (cambio de una línea) y
  prueba manual post-merge.
- **`response_format: json_object` en Go**: no verificado. Si lo rechazara con `400/422`, el fallback
  one-shot ya aceptado lo cubre (sin código nuevo); un `400` por otro motivo gastaría el reintento de
  formato (comportamiento preexistente, no lo empeora).
- **Cuota compartida de Go**: el chat de la app consume la misma cuota que el harness de agentes
  (5 h/semana/mes) y Go **monitoriza el abuso**; queda registrado en `docs/risks-and-open-questions.md`.
- **Id de modelo fijado**: si Go renombra `deepseek-v4.1-flash`, el chat degrada a modo semilla hasta
  cambiar la constante (sin fallo de la app).
- **Deuda de nomenclatura** (`DevExpertRecipeGenerator` sirviendo OpenCode Go): documentada en el KDoc;
  un renombrado provider-neutral sería una correctiva aparte.

## Validator Checklist

- [ ] El alcance es el de la feature: solo chat (política + petición), desacople de la imagen, dos
      claves y docs; **cero** cambios en dominio, `RecipeValidator`, wire, caché/imagen, UI o assets.
- [ ] Los 5 escenarios de aceptación pasan (endpoint/cabeceras/modelo de Go; `MISSING_KEY` sin red con
      la clave de chat; base de imagen propia de DevExpert y sin `x-opencode-session`; dos claves con
      consumidores separados; comportamiento aceptado intacto).
- [ ] Se ve la evidencia: `bash init.sh` verde, XML/SARIF parseados, greps de alcance, diff acotado.
- [ ] **Ninguna** clave aparece en el diff, en un fichero versionado ni en logs; `local.properties`
      sigue sin versionarse.
- [ ] Las cabeceras de Go **no** viajan en las peticiones de imagen (aserción negativa verde).
- [ ] Timeouts, reintentos, fallback de `response_format`, diagnóstico y modo semilla no cambian de
      comportamiento ni de tests (salvo literales de proveedor/modelo).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente por el implementer (no por el
      planner) con el estado y la evidencia; verificación manual pendiente anotada.
- [ ] No se ha colado trabajo de otras features (ni renombrados ni dependencias nuevas).
- [ ] UI: no aplica (no hay cambio visual; no se crea `docs/design/concepts/<feature-id>/`).
