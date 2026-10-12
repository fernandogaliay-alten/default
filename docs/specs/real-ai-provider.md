# Feature Implementation Spec: Proveedor de IA real tras la interfaz

## Source Feature

- `id`: `real-ai-provider`
- `area`: `integration` (no es feature de UI: no hay pantallas ni comportamiento visual nuevo)
- `depends_on`: `["prompt-builder", "generation-flow-with-validation"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Verificación de la feature (solo manual, ver *Verification Plan*): generar recetas end-to-end en el
  emulador con clave válida, y entrar en modo semilla sin fallar con clave ausente o inválida.
- Nota de la feature: proveedor decidido en `docs/adr/0003-ai-provider.md` (DevExpert Inference,
  `https://inference.devexpert.io/v1`, `POST /chat/completions`, alias `chat`). Es lo último que se
  conecta; la clave va en `local.properties` y **no** se versiona (ADR 0002).

## Goal

Conectar el **proveedor de IA real** detrás del puerto de dominio `RecipeGenerator`: la app habla
HTTP con **DevExpert Inference** (API compatible con OpenAI) usando **Ktor Client (engine OkHttp) +
`kotlinx.serialization`**, compone el prompt con el `PromptBuilder` ya aceptado, mapea el envelope
OpenAI (`choices[0].message.content` → texto JSON) y devuelve las recetas **decodificables** (posiblemente
vacías) sin lanzar. La política de **timeouts, reintentos y degradación a modo semilla** queda fijada en
un único sitio (`AiProviderPolicy`) y verificada en JVM con un engine inyectable (`MockEngine`).

La feature cierra la **pregunta abierta 1** de `docs/risks-and-open-questions.md` (errores y reintentos
de la IA) y la mueve a "Resueltas" con la política fija, citando esta spec. Además añade al build las
dependencias de Ktor, inyecta la clave en `BuildConfig.DEVEXPERT_API_KEY` desde `local.properties` y
añade el permiso `INTERNET` al manifiesto. **El resto del código no conoce Ktor**: solo
`com.sazon.app.data.ai` (y `MainActivity`, que solo lo compone).

## Non-Goals

- **No** se cambia la firma del puerto `RecipeGenerator` (3 operaciones `suspend` que devuelven
  `List<Recipe>`): sigue **sin** tipo de resultado propio. El modelo de error se materializa con
  `AiUnavailableException` (ver *Technical Approach* §5) y los llamadores aceptados no se modifican.
- **No** se toca lógica de dominio ya aceptada: `domain.generator`, `domain.validation`,
  `domain.prompt`, `domain.recipe`, `domain.profile`, `domain.suggestions`, `domain.search`,
  `domain.generation`, `domain.favorites`, `domain.activity`. `GenerationConstraints` y
  `FakeRecipeGenerator` quedan intactos.
- **No** hay UI nueva ni cambios de comportamiento de pantalla: ni pantallas, ni microcopy, ni estados,
  ni tokens, ni capturas de Stitch. Lo único que se toca de UI es el **cableado de composición** en
  `MainActivity` (sustituir la construcción del generador).
- **No** se implementan `chat-pro` (alias avanzado), selección de modelo en Ajustes, streaming,
  `response_format`/structured outputs, `max_tokens`, ni multi-proveedor.
- **No** se añade persistencia nueva ni caché nueva (la política cache-first del Lote ya protege el
  coste) y **no** se implementa el log como sistema (solo un evento local en memoria, sin telemetría).
- **No** se cambia `init.sh` ni las reglas de verificación del harness.
- **No** se añaden dependencias fuera de Ktor (nada de Retrofit, OkHttp directo, DI, MockK, Turbine).
- **No** se versiona la clave ni se imprime su valor: ni en logs, ni en mensajes de error, ni en tests,
  ni en esta spec.

## Job Story

Como usuario local,
quiero que las recetas de verdad las genere una IA real a partir de mis ingredientes y mis preferencias,
para tener variedad más allá de las 12 recetas semilla, y que si el proveedor falla o no hay clave la
app siga funcionando con lo que ya tengo en el dispositivo.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002); no hay cuentas ni permisos nuevos (solo el
  permiso de red del manifiesto). No cambia nada de lo que ve hoy salvo que las recetas pasan a ser
  reales.
- **Desarrollador / agente**: cubre en JVM la política (timeouts/reintentos/deadline), el mapeo del
  envelope, el parseo tolerante, la degradación y los tres prompts, con un `MockEngine` inyectable:
  **sin red real y sin clave real**. La prueba end-to-end contra el proveedor la hace la persona.
- **Sistema**: devuelve `List<Recipe>` desordenada y sin validar (contrato del puerto); los llamadores
  (`SuggestionBatchService`, `GenerationService`, `AiSearchService`) siguen aplicando el
  `RecipeValidator` con el Perfil efectivo. Si la IA no está disponible, el sistema degrada a modo
  semilla, que es un modo esperado y no un error de la app (`docs/domain-model.md`, "Disponibilidad de
  IA").

## Acceptance Scenarios

### Scenario 1: camino feliz (HTTP 200 con el contrato del prompt)

Given un `MockEngine` que responde `200` con `choices[0].message.content` = `{ "recipes": [ <receta
válida> ] }` y una clave de test,
When se invoca `generateFromIngredients(ingredientes, constraints)`,
Then devuelve las recetas decodificables (idénticas al contrato), con **una sola** llamada HTTP, sin
reintento; la petición va a `https://inference.devexpert.io/v1/chat/completions`, con cabecera
`Authorization: Bearer <clave>` y cuerpo `{"model":"chat","messages":[{role:system},{role:user}]}`.

### Scenario 2: clave ausente degrada a modo semilla (verificación manual del harness, comprobada en JVM)

Given `apiKey = ""` (cadena vacía o `BuildConfig` ausente),
When se invoca cualquiera de las 3 operaciones,
Then lanza `AiUnavailableException(MISSING_KEY)` **sin ninguna llamada HTTP** y, con el
`GenerationService` real ya aceptado, el resultado es `aiUnavailable = true` con listas vacías → la UI
muestra el estado degradado con semillas (**modo semilla**), sin microcopy específico de cuota/clave y
sin fallar.

### Scenario 3: 200 ilegible o vacío no lanza (estado vacío, no excepción)

Given un `MockEngine` que responde `200` con `content = "lo siento, no puedo ayudarte"`, `content =
null`, `content = ""` o `choices = []`,
When se invoca la operación,
Then devuelve una lista **vacía** (sin excepción y sin reintento: 1 llamada) y, con el
`GenerationService` real, `aiUnavailable = false` y `accepted` vacío → la pantalla de Resultados muestra
su **estado vacío con "Reintentar"**.

### Scenario 4: reintento único ante 429 respetando `Retry-After` corto

Given un `MockEngine` que responde `429` (con `Retry-After: 2`) y después `200`,
When se invoca la operación,
Then hay **2** intentos en total, el resultado es el de la segunda respuesta y el backoff es el de
`Retry-After` (2 s). Con `Retry-After: 30` (> 5 s) o ausente/ilegible, el backoff es el fijo de 500 ms.

### Scenario 5: nunca se reintenta un 4xx no reintentable

Given un `MockEngine` que responde `400`, `401`, `403`, `404` o `422`,
When se invoca la operación,
Then hay **una sola** llamada y se lanza `AiUnavailableException(HTTP_ERROR)` (con `RATE_LIMITED` en el
caso del 429 agotado) → los llamadores degradan a modo semilla.

### Scenario 6: reintento ante 5xx, `IOException` y timeout; máximo 2 intentos

Given un `MockEngine` que responde `503` (o lanza `IOException`, o agota el timeout) y después `200`,
When se invoca la operación,
Then se reintenta una vez y se devuelve el resultado; si los **dos** intentos fallan de forma
reintentable, se lanza `AiUnavailableException` con el motivo del último fallo y **nunca** hay un tercer
intento.

### Scenario 7: deadline total de 60 s

Given un reloj inyectado con el que el primer intento consume el deadline (60 s),
When se invoca la operación,
Then **no** se inicia un segundo intento (1 llamada) y, antes de cada intento, el timeout de la petición
se recorta a lo que queda del deadline (`min(40 s, restante)`).

### Scenario 8: la cancelación se propaga

Given una llamada en vuelo,
When se cancela el job de la operación,
Then se relanza `CancellationException` (no se convierte en `AiUnavailableException`, no se reintenta y
no se traga).

### Scenario 9: el proveedor **no** pre-filtra por dieta ni restricciones duras

Given un `MockEngine` que responde `200` con una receta que lleva `pollo` (tag `poultry`) y otra con
"Pan" libre (gluten),
When el `AiSearchService` real busca con un Perfil vegano, y con un Perfil con gluten,
Then el proveedor devuelve **ambas** recetas al llamador tal cual y es el llamador quien produce
`DiscardReason.DIET_PATTERN` / `DiscardReason.HARD_RESTRICTION` → el estado **Bloqueado** de `ai-search`
sigue siendo alcanzable con el proveedor real (no se degrada a "sin resultados").

### Scenario 10: las tres operaciones componen su prompt

Given la Taxonomía real y un Perfil de ejemplo,
When se invocan `suggestByPreferences`, `searchByText(query)` y `generateFromIngredients(...)`,
Then las tres envían un mensaje `system` con el contrato de `PromptBuilder` y un mensaje `user` con
"Patrón de dieta:" y "Restricciones duras:"; `searchByText` incluye `Búsqueda: <query>`;
`generateFromIngredients` incluye "Ingredientes disponibles:" y los ajustes (comida, tiempo,
dificultad, comensales y notas); `suggestByPreferences` no lleva ni ingredientes ni consulta.

### Scenario 11: end-to-end contra el proveedor real (verificación **manual**, no automatizable)

Given `local.properties` con `devexpert.apiKey` válida y el emulador de Android Studio en **Windows**,
When se genera desde el Generador con ingredientes reales,
Then llegan recetas del proveedor, pasan el Validador y se muestran en Resultados; y con la clave
borrada o inválida la app entra en **modo semilla** sin fallar.
Motivo de manual: el `adb` de WSL no ve el emulador (AGENTS.md, "Notas de entorno"); no hay E2E
automatizable de UI.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `real-ai-provider` (`area: integration`, verificación solo manual).
- `PROGRESS.md` (sesiones 060–065 y estado actual) — `ai-search` `accepted`; camino estándar
  `bash init.sh`; se mantiene la **pregunta 1** abierta hasta `real-ai-provider`.
- `AGENTS.md` — una feature por sesión; nunca `./gradlew` directo (usar `init.sh` o
  `scripts/gradle.sh`); clave no versionada; el trabajo lo cierra la validación independiente.
- `docs/adr/0003-ai-provider.md` — proveedor, base URL, endpoint, `Bearer`, alias `chat`/`chat-pro`, sin
  SDK, sin structured outputs, degradación a modo semilla. `docs/adr/0002-local-first-no-backend.md` —
  clave en `BuildConfig` desde `local.properties`, APK solo demo.
- `docs/risks-and-open-questions.md` — la tabla "Implementation-Time Questions" queda **solo con la
  pregunta 1** (errores/reintentos de la IA) más la nota de renumeración y la sección "Resueltas".
- `docs/technical-discovery.md` — "Candidate Stack" ya prevé Ktor/Retrofit + `kotlinx.serialization`;
  fila `Clave de IA | BuildConfig, desde local.properties`; secciones "Integrations", "Observability"
  (log local de eventos de IA con proveedor, código, duración y motivo de descarte) y "Testing and
  Verification" (dominio testeable en JVM).
- `docs/domain/taxonomy.md` §4 (contrato JSON de la respuesta) y §5 (reglas del validador).
- `docs/domain-model.md` — `RecipeGenerator` ("Implementación real: **pendiente**"), estado
  `ai_available | ai_unavailable (sin clave, sin red, error del proveedor o cuota)`.
- `app/src/main/java/com/sazon/app/domain/generator/{RecipeGenerator,GenerationConstraints}.kt` — puerto
  de 3 operaciones `suspend` que devuelven `List<Recipe>` "no validadas"; `constraints` lleva el
  `profile` **efectivo** (la UI sustituye sus `hardRestrictionIds` por los del paso de ajustes).
- `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt` — única implementación hoy;
  sigue siendo la de test/desarrollo (no se toca).
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — `build(taxonomy, PromptRequest)`
  → `BuiltPrompt(system, user)`; `PromptRequest(preferences, servings, ingredients, mealTypeId,
  timeBucketId, difficultyId, query, notes)` y `PromptIngredient(name, ingredientId)`.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` —
  `validateResponse(rawResponse, taxonomy, constraints)` (parseo **elemento a elemento**, `Malformed`
  si el JSON no es un objeto con array `recipes`), `validateRecipe`, `RecipeConstraints`,
  `DiscardReason`.
- Los tres llamadores, para fijar el contrato de degradación:
  `domain/suggestions/SuggestionBatchService.kt` (catch `Exception` → lote **solo semilla**),
  `domain/generation/GenerationService.kt` (catch `Exception` → `aiUnavailable = true`),
  `domain/search/AiSearchService.kt` (catch `Exception` → `aiUnavailable`, y **Bloqueado** según el
  motivo del descarte `DIET_PATTERN`/`HARD_RESTRICTION`).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — composición manual (sin DI): hoy
  `val generator = FakeRecipeGenerator()` compartido por los tres servicios.
- `app/src/main/AndroidManifest.xml` — **solo** `ACCESS_NETWORK_STATE`: falta `INTERNET`.
- `app/build.gradle.kts` — `buildFeatures { compose = true }` (sin `buildConfig`), dependencias con
  version catalog, `kotlinx.serialization` ya aplicado, propiedades de sistema de test.
- `gradle/libs.versions.toml` — versiones fijadas (Kotlin 2.2.10, coroutines 1.11.0,
  serialization 1.9.0); **no** hay Ktor.
- `local.properties` (no versionado, en `.gitignore`) — contiene `sdk.dir` y la propiedad
  `devexpert.apiKey` (se inspeccionó **solo el nombre de la propiedad**; su valor no se copia a ningún
  artefacto).
- `app/src/test/java/com/sazon/app/domain/search/AiSearchServiceTest.kt` — convenciones de test JVM
  (`runTest`, generadores de prueba, Taxonomía real por `System.getProperty("taxonomy.asset.path")`,
  nombres en español con `_`, casos negativos).
- `init.sh` y `scripts/gradle.sh` — `bash init.sh` = `./gradlew test` + `./gradlew lint` con la salida
  redirigida; no arranca procesos largos ni necesita modos nuevos.
- Verificación **externa** del planner (Maven Central): `ktor-client-core`, `ktor-client-okhttp`,
  `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json` y `ktor-client-mock` existen en
  la versión **3.6.0** (última estable publicada).

### Existing Patterns To Follow

- **Puerto de dominio + adaptador en `data`**: `AssetTaxonomySource`, `AssetSeedRecipeSource`,
  `NetworkStatusSource` y `FakeRecipeGenerator` viven en `com.sazon.app.data`; el dominio no conoce
  Android ni red.
- **Composición manual sin DI** en `MainActivity` y paso por parámetro (`SazonApp` → `MainShell`).
- **Reloj inyectable** (`SuggestionBatchService(now: () -> Long)`) y **costuras aditivas con default**
  (`onRecipesGenerated`) — mismo estilo para el reloj del deadline y el logger local.
- **Tests JUnit 4 en JVM**, Taxonomía real por propiedad de sistema, casos negativos que no pasan en
  vacío; sin emulador.
- **Nada de hex/estilo**: esta feature no pinta.

### Current Gaps

- No existe ninguna implementación real del puerto: todo el flujo corre con `FakeRecipeGenerator`.
- No hay cliente HTTP, ni `kotlinx.serialization` de red (los DTOs de Taxonomía/Receta no cubren el
  envelope OpenAI), ni política de timeouts/reintentos, ni inyección de la clave.
- No hay permiso `INTERNET` en el manifiesto.
- `docs/risks-and-open-questions.md` mantiene abierta la pregunta 1.

## Technical Approach

Estrategia: **dependencias de red + política fija en un objeto + adaptador `RecipeGenerator` en `data`
+ mapa del envelope tolerante**, sin tocar dominio aceptado ni UI. Todo lo verificable se prueba en JVM
inyectando el engine (`MockEngine`).

### 1. Build: dependencias, `BuildConfig` y manifiesto

`gradle/libs.versions.toml` (añadir):

```toml
[versions]
ktor = "3.6.0"

[libraries]
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { group = "io.ktor", name = "ktor-client-mock", version.ref = "ktor" }
```

`app/build.gradle.kts`:
- `implementation(libs.ktor.client.core)`, `implementation(libs.ktor.client.okhttp)`,
  `implementation(libs.ktor.client.content.negotiation)`,
  `implementation(libs.ktor.serialization.kotlinx.json)`; `testImplementation(libs.ktor.client.mock)`.
- `buildFeatures { compose = true; buildConfig = true }`.
- Leer `local.properties` **en tiempo de configuración** y publicar el campo (valor vacío si falta el
  fichero o la propiedad; **jamás** imprimir el valor ni el fichero):

```kotlin
import java.util.Properties

val localProperties = Properties().apply {
    rootProject.layout.projectDirectory.file("local.properties")
        .asFile.takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}
val devexpertApiKey: String = localProperties.getProperty("devexpert.apiKey", "")

android {
    defaultConfig {
        buildConfigField(
            "String",
            "DEVEXPERT_API_KEY",
            "\"${devexpertApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
    }
}
```

`app/src/main/AndroidManifest.xml`: añadir
`<uses-permission android:name="android.permission.INTERNET" />` (se conserva
`ACCESS_NETWORK_STATE`).

### 2. Política en un único sitio — `com.sazon.app.data.ai.AiProviderPolicy`

Todas las constantes y decisiones de la política de IA, **sin** HTTP (puro y testeable):

```kotlin
object AiProviderPolicy {
    const val BASE_URL = "https://inference.devexpert.io/v1"
    const val CHAT_COMPLETIONS_PATH = "/chat/completions"
    const val MODEL_ALIAS = "chat"

    const val CONNECT_TIMEOUT_MILLIS = 10_000L   // connect
    const val SOCKET_TIMEOUT_MILLIS = 30_000L    // socket
    const val REQUEST_TIMEOUT_MILLIS = 40_000L   // por intento
    const val TOTAL_DEADLINE_MILLIS = 60_000L    // por operacion (todos los intentos)
    const val MAX_ATTEMPTS = 2                   // 1 reintento como maximo
    const val RETRY_BACKOFF_MILLIS = 500L        // backoff fijo
    const val RETRY_AFTER_MAX_MILLIS = 5_000L    // Retry-After solo si <= 5 s

    /** Restricciones "neutras": el proveedor NO aplica dieta ni restricciones duras (ver Decisiones). */
    val NEUTRAL_CONSTRAINTS = RecipeConstraints(dietPatternId = "", hardRestrictionIds = emptySet())

    fun endpointUrl(): String = BASE_URL + CHAT_COMPLETIONS_PATH
    fun isRetryableStatus(statusCode: Int): Boolean   // 408, 429 y 500..599 -> true; resto -> false
    fun retryDelayMillis(retryAfterHeaderValue: String?): Long   // segundos 0..5 -> ese; si no -> 500
    fun attemptRequestTimeoutMillis(remainingMillis: Long): Long = minOf(REQUEST_TIMEOUT_MILLIS, remainingMillis)
    fun canRetry(attempt: Int, remainingMillis: Long): Boolean   // attempt < MAX_ATTEMPTS && remaining - backoff > 0
    fun unavailableReason(statusCode: Int): AiUnavailableReason    // 429 -> RATE_LIMITED; otro 4xx/5xx -> HTTP_ERROR
}
```

`AiUnavailableReason` (enum, `data.ai`): `MISSING_KEY`, `TIMEOUT`, `NETWORK`, `RATE_LIMITED`,
`HTTP_ERROR`, `UNEXPECTED`. `AiUnavailableException(reason, cause?)` extiende `Exception` (nunca
`Error`).

### 3. Cliente HTTP y envelope OpenAI

`com.sazon.app.data.ai.AiHttpClientFactory`:

```kotlin
object AiHttpClientFactory {
    fun create(engine: HttpClientEngine? = null): HttpClient   // null -> OkHttp; en tests -> MockEngine
}
```

- Instala `ContentNegotiation` con `json(Json { ignoreUnknownKeys = true; encodeDefaults = true })`
  (usado para **serializar la petición**) e `HttpTimeout` con
  `connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS`, `socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS`,
  `requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS`.
- `expectSuccess` se queda en `false`: el **estado HTTP lo inspecciona el proveedor** (no queremos que
  Ktor lance por un 4xx/5xx y perdamos el código).
- Los reintentos se implementan **a mano** en el proveedor (no con `HttpRequestRetry`), para que la
  política sea explícita, verificable en JVM y respete el deadline total.

`com.sazon.app.data.ai.OpenAiChat.kt` — DTOs `@Serializable` (con `ignoreUnknownKeys`):
- Petición: `ChatCompletionRequest(model: String, messages: List<ChatMessage>)` y
  `ChatMessage(role: String? = null, content: String? = null)`.
- Respuesta: `ChatCompletionResponse(choices: List<ChatChoice> = emptyList())`,
  `ChatChoice(message: ChatMessage = ChatMessage())`.
- Cuerpo enviado: `{"model":"chat","messages":[{"role":"system",...},{"role":"user",...}]}`. **No** se
  envía `response_format`/`json_schema` (ADR 0003), ni `stream`, ni `temperature`, ni `max_tokens`.
- Cabeceras: `Authorization: Bearer <clave>` y `Content-Type: application/json`.

`com.sazon.app.data.ai.AiRecipePayload` — decode **puro** del contenido (sin HTTP, directamente
testeable):

```kotlin
object AiRecipePayload {
    fun decode(content: String?, taxonomy: Taxonomy): List<Recipe>
}
```

1. `content` nulo o en blanco → `emptyList()` (no es un error del proveedor).
2. Normalización mínima: recortar y **quitar una valla markdown envolvente** (` ```json ... ``` ` o
   ` ``` ... ``` `) si está presente.
3. `RecipeValidator.validateResponse(texto, taxonomy, AiProviderPolicy.NEUTRAL_CONSTRAINTS)`:
   `ValidationCompleted(accepted, _)` → `accepted`; `Malformed` → `emptyList()`.
4. **Nunca lanza** en estos casos, ni reintenta: devuelve la lista (posiblemente vacía).

### 4. Adaptador — `com.sazon.app.data.ai.DevExpertRecipeGenerator`

```kotlin
class DevExpertRecipeGenerator(
    private val apiKey: String,
    private val taxonomy: Taxonomy,
    private val client: HttpClient,
    private val model: String = AiProviderPolicy.MODEL_ALIAS,
    private val onEvent: (AiCallEvent) -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) : RecipeGenerator
```

**Las tres operaciones** componen el prompt con `PromptBuilder.build(taxonomy, request)` y llaman al
mismo `execute(operation, prompt)`:

| Operación | `PromptRequest` |
|---|---|
| `suggestByPreferences(profile)` | `preferences = profile`, `servings = profile.defaultServings` (sin ingredientes ni consulta) |
| `searchByText(query, profile)` | `preferences = profile`, `servings = profile.defaultServings`, `query = query` |
| `generateFromIngredients(ingredients, constraints)` | `preferences = constraints.profile` (**Perfil efectivo**), `servings = constraints.servings`, `ingredients = ingredients.map { PromptIngredient(it.name, it.ingredientId) }`, `mealTypeId`, `timeBucketId`, `difficultyId`, `notes` de `constraints` |

**`execute(operation, builtPrompt)`** (deadline y reintentos, en este orden exacto):
1. Si `apiKey.isBlank()` → emite el evento y lanza `AiUnavailableException(MISSING_KEY)` **sin** hacer
   ninguna petición.
2. `deadline = now() + TOTAL_DEADLINE_MILLIS` — se fija **una vez**, antes del primer intento; el
   deadline total se aplica en el bucle (comprobación de margen antes de cada intento y recorte del
   timeout de la petición).
3. Bucle: `remaining = deadline - now()`; si `remaining <= 0` → `AiUnavailableException(TIMEOUT)`;
   `POST endpointUrl()` con
   `timeout { requestTimeoutMillis = attemptRequestTimeoutMillis(remaining) }`.
   - `2xx` → `AiRecipePayload.decode(content, taxonomy)` y devolver.
   - Fallo **reintentable** (`isRetryableStatus(status)` = 408/429/5xx, `IOException`,
     `HttpRequestTimeoutException`/`SocketTimeoutException`) con `canRetry(...)` → `delay(retryDelay)`
     (cancellable) y segundo intento; si no, `AiUnavailableException(unavailableReason(status))`.
     > Hallazgo del implementer: cuando lo que impide el reintento es el **deadline** (y no el número
     > de intentos), el motivo es `TIMEOUT` —es el plazo lo que agota la operación—; `HTTP_ERROR`/
     > `RATE_LIMITED` (`unavailableReason(status)`) queda para el fallo reintentable que agota
     > `MAX_ATTEMPTS`. Así "deadline agotado ⇒ `TIMEOUT`" vale tanto en el corte antes de un intento
     > como al decidir no reintentar.
   - Fallo **no reintentable** (400/401/403/404/422 y demás 4xx/3xx) → `AiUnavailableException` en el
     primer intento (1 sola llamada).
   - `CancellationException` se comprueba/relanza **siempre** (antes de cualquier `catch`), y nunca se
     convierte en `AiUnavailableException`.
4. Cualquier otra excepción inesperada → `AiUnavailableException(UNEXPECTED, cause)`.

**Diagnóstico local** (`AiCallEvent(operation, outcome, attempt, httpStatus, durationMillis, detail)`
con `outcome ∈ { SUCCESS, EMPTY, RETRY, UNAVAILABLE }`): costura con default no-op, cableada en
`MainActivity` a `android.util.Log`. El evento **nunca** incluye la clave, la cabecera `Authorization`
ni los cuerpos; `detail` es el motivo/categoría (p. ej. `MISSING_KEY`, `HTTP 429`, `deadline`).

### 5. Modo degradado y composición

- `MainActivity` sustituye `FakeRecipeGenerator()` por
  `DevExpertRecipeGenerator(apiKey = BuildConfig.DEVEXPERT_API_KEY, taxonomy = taxonomy,
  client = AiHttpClientFactory.create(), onEvent = { ... })`, creado **después** de cargar la Taxonomía
  y compartido por los tres servicios como hoy. `FakeRecipeGenerator` sigue existiendo para tests y no
  se toca.
- **Modo semilla**: el puerto no tiene tipo de resultado, así que la única señal que los llamadores ya
  aceptados entienden como "IA no disponible" es una excepción (`catch (_: Exception)` →
  `aiUnavailable` / lote seed-only / estado `Unavailable` con semillas). Por eso el proveedor lanza
  `AiUnavailableException` **tipada** (nunca `Error`, nunca sin capturar) en clave ausente, errores de
  transporte y HTTP no recuperable, y **no** lanza en un 200 ilegible o vacío (devuelve lista vacía).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — constantes y decisiones puras.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiUnavailableReason`,
  `AiUnavailableException`, `AiCallOutcome` y `AiCallEvent`.
- `app/src/main/java/com/sazon/app/data/ai/OpenAiChat.kt` — DTOs de petición/respuesta.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — decode tolerante del `content`.
- `app/src/main/java/com/sazon/app/data/ai/AiHttpClientFactory.kt` — cliente Ktor (OkHttp/Mock).
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — adaptador del puerto.
- `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` — política (reintentabilidad,
  `Retry-After`, recorte del timeout, `canRetry`).
- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` — decode tolerante (contrato, valla
  markdown, vacío/null/no-JSON/`choices` vacío) con la Taxonomía real.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — `MockEngine`: escenarios
  1–10 (incluye la degradación con `GenerationService`/`AiSearchService` reales).

**Modificar**

- `gradle/libs.versions.toml` — `ktor = "3.6.0"` + las 5 librerías de §1.
- `app/build.gradle.kts` — dependencias de Ktor, `buildConfig = true` y `DEVEXPERT_API_KEY`.
- `app/src/main/AndroidManifest.xml` — permiso `INTERNET`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer el proveedor real y el logger local.
- `docs/technical-discovery.md` — capa `data.ai` con Ktor, `BuildConfig`, política, permiso `INTERNET`,
  verificación con `MockEngine` y log local.
- `docs/risks-and-open-questions.md` — **cerrar la pregunta 1** (ver *Durable Documentation Impact*).
- `docs/domain-model.md` — `RecipeGenerator`: "Implementación real: `DevExpertRecipeGenerator`
  (`data.ai`, Ktor + DevExpert Inference); implementación de test: `FakeRecipeGenerator`".
- `feature_list.json` y `PROGRESS.md` — estado y evidencia, **al implementar** (no en esta sesión).

**No se toca**: `init.sh`, `scripts/**`, `domain/**` (incluidos `RecipeGenerator`,
`GenerationConstraints`, `RecipeValidator`, `PromptBuilder`), `data/generator/FakeRecipeGenerator.kt`,
`ui/**` (salvo el cableado de `MainActivity`), las bases Room, la Taxonomía, `seed-recipes.json`,
`DESIGN.md` y `docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`).
- `docs/design/concepts/`: **not needed** — no hay pantalla, estado, microcopy ni acabado nuevos: el
  comportamiento visible es el ya aceptado (Generador, Resultados, Home, Búsqueda con IA) con otra
  implementación detrás del puerto. No se piden capturas de Stitch y no se bloquea la feature por
  capturas.
- Superficie tocada: solo el **cableado de composición** en `MainActivity` (construcción del
  generador), que no pinta nada.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — "Candidate Stack" (cliente HTTP = **Ktor Client + engine
  OkHttp**), "Data and Storage" (fila de la clave: `BuildConfig.DEVEXPERT_API_KEY` desde
  `local.properties`), "Integrations" (el adaptador real ya existe: `data.ai`, prompts de
  `PromptBuilder`, envelope OpenAI, parseo tolerante, política), "Observability" (log local de eventos
  de IA materializado como costura `AiCallEvent`) y "Testing and Verification" (tests JVM con
  `MockEngine`, sin red ni clave).
- `docs/domain-model.md`: **update** — la línea "Implementación real: pendiente" de `RecipeGenerator`
  pasa a nombrar `DevExpertRecipeGenerator`; el estado `ai_unavailable` (sin clave, sin red, error o
  cuota) no cambia.
- `docs/risks-and-open-questions.md`: **update** — **la pregunta 1 (errores y reintentos de la IA)
  queda resuelta por esta feature**:
  - Añadir a **"Resueltas"** un bullet con la política fija (connect 10 s, socket 30 s, timeout por
    intento 40 s, deadline total 60 s; máximo 1 reintento solo ante timeout/`IOException`/408/429/5xx
    con backoff fijo de 500 ms y `Retry-After` solo si ≤ 5 s; nunca 400/401/403/404/422; un 200 ilegible
    o vacío devuelve las recetas decodificables, posiblemente vacías, para el estado vacío con
    "Reintentar"; cuota agotada y clave ausente degradan a modo semilla sin microcopy de cuota),
    indicando que las constantes viven en `com.sazon.app.data.ai.AiProviderPolicy` y **citando
    `docs/specs/real-ai-provider.md`**.
  - Dejar coherente la sección **"Implementation-Time Questions"**: la tabla queda **sin filas**
    (encabezado + nota explícita de que **no quedan preguntas de implementación abiertas**, que la
    última —la 1— la resolvió `real-ai-provider`) y actualizar la nota de renumeración (los números eran
    posicionales y las specs históricas conservan sus citas). No renumerar nada más.
- `DESIGN.md` y `docs/design/concepts/**`: **not needed** — sin cambio visual ni capturas.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión.

## Implementation Plan

1. Añadir Ktor al version catalog y al build, `buildConfig = true` con `DEVEXPERT_API_KEY`, y el
   permiso `INTERNET`.
2. Crear `AiProviderPolicy`, `AiDiagnostics` y `OpenAiChat` (constantes, tipos de error y DTOs).
3. Crear `AiRecipePayload` y `AiHttpClientFactory`.
4. Crear `DevExpertRecipeGenerator` (3 operaciones, prompt, deadline, reintentos, degradación, log).
5. Escribir los tres tests JVM con `MockEngine` (política, payload, proveedor + degradación).
6. Cablear el proveedor real en `MainActivity`.
7. Ejecutar `bash init.sh` en verde y actualizar `docs/technical-discovery.md`,
   `docs/domain-model.md` y `docs/risks-and-open-questions.md` (cierre de la pregunta 1); el
   implementer registra evidencia y estado en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `gradle/libs.versions.toml`: `ktor = "3.6.0"` + `ktor-client-core`, `ktor-client-okhttp`,
      `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-mock`.
- [ ] `app/build.gradle.kts`: dependencias (`implementation` ×4, `testImplementation` ×1),
      `buildConfig = true` y `buildConfigField("String", "DEVEXPERT_API_KEY", ...)` leyendo
      `local.properties` con valor vacío si falta.
- [ ] `AndroidManifest.xml`: permiso `INTERNET`.
- [ ] `AiProviderPolicy` con las constantes fijadas y los helpers puros (`isRetryableStatus`,
      `retryDelayMillis`, `attemptRequestTimeoutMillis`, `canRetry`, `unavailableReason`,
      `NEUTRAL_CONSTRAINTS`).
- [ ] `AiDiagnostics`: `AiUnavailableReason`, `AiUnavailableException`, `AiCallOutcome`, `AiCallEvent`.
- [ ] `OpenAiChat`: DTOs de petición y respuesta `@Serializable`.
- [ ] `AiRecipePayload.decode(content, taxonomy)`: vacío/null → `emptyList()`; quitar valla markdown;
      `validateResponse` con `NEUTRAL_CONSTRAINTS`; `Malformed` → `emptyList()`; nunca lanza.
- [ ] `AiHttpClientFactory.create(engine = null)`: OkHttp por defecto, `ContentNegotiation` y
      `HttpTimeout` con la política, `expectSuccess = false`.
- [ ] `DevExpertRecipeGenerator`: clave ausente → `MISSING_KEY` sin petición; las 3 operaciones
      componen `PromptRequest`; `execute` con deadline total, timeout recortado por intento, 1 reintento
      con backoff/`Retry-After`, nunca reintentar 4xx no reintentables, relanzar `CancellationException`
      y emitir `AiCallEvent`.
- [ ] `AiProviderPolicyTest`: tabla de estados reintentables, `Retry-After` (2 s, 30 s, ausente,
      ilegible), recorte del timeout por intento y `canRetry` en la frontera del deadline.
- [ ] `AiRecipePayloadTest`: contrato válido; una receta rota dentro de un lote legible; valla markdown;
      `"lo siento"`/`""`/`null`/`[1,2]`/`choices` vacío → vacío sin lanzar; `NEUTRAL_CONSTRAINTS` no
      aplica dieta ni restricciones (el `pollo` y el "Pan" siguen llegando).
- [ ] `DevExpertRecipeGeneratorTest`: escenarios 1–10 con `MockEngine` y reloj inyectado, incluidos la
      URL/cabecera/cuerpo de la petición, los 3 prompts, 429→200 con `Retry-After`, 401 sin reintento,
      5xx/`IOException`→200, dos fallos → 2 llamadas, deadline agotado → 1 llamada, cancelación, y la
      degradación real con `GenerationService` (clave ausente → `aiUnavailable`) y con `AiSearchService`
      (Blocked por `DIET_PATTERN`/`HARD_RESTRICTION`).
- [ ] `MainActivity`: componer `AiHttpClientFactory.create()` +
      `DevExpertRecipeGenerator(BuildConfig.DEVEXPERT_API_KEY, taxonomy, client, onEvent = Log)` y
      usarlo en los tres servicios.
- [ ] `bash init.sh` en verde; actualizar `docs/technical-discovery.md`, `docs/domain-model.md` y
      `docs/risks-and-open-questions.md` (pregunta 1 → "Resueltas" y tabla vacía con nota).

## Verification Plan

- **Automática (JVM, la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo (AGENTS.md) y **nunca** `--stop` (es global: mataría builds de otros worktrees).
  - `AiProviderPolicyTest`: 408/429/500/502/503 → reintentables; 200/201/400/401/403/404/422/302 → no;
    `Retry-After: 2` → 2000 ms, `: 30`/ausente/`abc` → 500 ms; `attemptRequestTimeoutMillis(60_000)` =
    40 000 y `(5_000)` = 5000; `canRetry(1, remaining)` verdadero solo con margen sobre el backoff,
    `canRetry(2, …)` falso.
  - `AiRecipePayloadTest`: 200 con el contrato → recetas; receta ilegible dentro de un lote legible → se
    descarta sin tumbar el resto; valla markdown → decodifica; `"lo siento"`, `""`, `null`, JSON sin
    `recipes`, `choices` vacío → lista vacía **sin excepción**; el `pollo` (poultry) y el "Pan" libre
    **sí** llegan (el proveedor no pre-filtra).
  - `DevExpertRecipeGeneratorTest` (`MockEngine`, reloj inyectado, clave de test ficticia
    `"test-key"`): una sola llamada en el camino feliz, URL
    `https://inference.devexpert.io/v1/chat/completions`, cabecera `Authorization: Bearer test-key`,
    `model = "chat"` y `messages[0].role = system`; los tres prompts (Scenario 10); 429+`Retry-After` →
    2 intentos; 401 → 1 intento + `AiUnavailableException(HTTP_ERROR)`; 503/`IOException` → 2 intentos
    y éxito; dos 500 → 2 intentos y excepción; deadline consumido → 1 intento; cancelación → relanza
    `CancellationException`; `apiKey = ""` → 0 llamadas y `MISSING_KEY`; con `GenerationService` real
    (clave ausente) → `aiUnavailable = true`; con `AiSearchService` real y perfil vegano/con gluten →
    `DIET_PATTERN`/`HARD_RESTRICTION` y estado Bloqueado alcanzable.
- **Lint:** `bash init.sh` → 0 errores y **sin warnings nuevos** en ficheros de la feature (los 13
  preexistentes se mantienen; si aparece un `GradleDependency`/`NewerVersionAvailable` por Ktor, es la
  categoría ya presente y no un error).
- **Inspección:** `git diff --stat` limitado a los ficheros de *Expected File Changes*; `domain/**`,
  `data/generator/FakeRecipeGenerator.kt`, `ui/**` (salvo `MainActivity.kt`), las bases Room, la
  Taxonomía, `seed-recipes.json`, `DESIGN.md` e `init.sh` intactos; `git grep` del **valor** de la clave
  no encuentra nada (y `local.properties` sigue ignorado por git); ninguna traza de la clave ni de la
  cabecera `Authorization` en logs, mensajes de error o tests.
- **Manual (la persona, en el emulador de Android Studio en Windows; no automatizable):**
  - Con `devexpert.apiKey` válida en `local.properties`: `bash scripts/gradle.sh assembleDebug` y
    ejecutar en el emulador; desde el Generador, generar con ingredientes reales y comprobar que llegan
    recetas del proveedor que pasan el Validador y se muestran en Resultados (**verificación exigida 1**);
    comprobar también la Home (Lote `suggestByPreferences`) y la Búsqueda con IA.
  - Con la clave vaciada (o ausente) en `local.properties`: la app entra en **modo semilla** y no falla
    (**verificación exigida 2**); el Generador muestra el estado degradado con semillas y la Home sigue
    poblada con las 12 semillas.
  - Nota: cambiar `local.properties` exige reconstruir; el `adb` de WSL no ve el emulador (AGENTS.md).
- Cobertura E2E: **no aplica**. No hay suite E2E persistente y AGENTS.md no automatiza UI de emulador;
  el `MockEngine` cubre el transporte en JVM y lo visual/manual queda para la persona.
- `init.sh`: **no cambia**; sigue ejecutando `test` + `lint` no bloqueantes, sin procesos largos.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal `com.sazon.app.data.ai.*`: nº de tests y 0
  fallos (esperado: 343 preexistentes + los nuevos), y el lint con 0 errores y sin warnings nuevos.
- Detalle de cobertura de los tres suites nuevos (política, decode tolerante, proveedor) con los
  escenarios 1–10, incluidos los negativos (401 sin reintento, 429 agotado, deadline, cancelación,
  clave ausente, 200 ilegible).
- `git diff --stat` de los ficheros tocados y confirmación de que `domain/**`, `FakeRecipeGenerator`,
  `ui/**` (salvo `MainActivity.kt`), `init.sh`, las bases Room, la Taxonomía y `seed-recipes.json` no
  cambian.
- Comprobación de que el valor de la clave no aparece en el repositorio versionado ni en logs/tests.
- Descripción del resultado **manual** en el emulador (clave válida → recetas reales; clave
  ausente/inválida → modo semilla); captura opcional.
- `docs/risks-and-open-questions.md` con la pregunta 1 en "Resueltas" y la tabla sin filas con nota.
- Anotación de estado/evidencia en `feature_list.json` y `PROGRESS.md` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Ktor Client + OkHttp + `kotlinx.serialization`, aislado en `data.ai`**: el dominio sigue conociendo
  solo el puerto `RecipeGenerator`; ningún fichero fuera de `data.ai` (y el cableado de `MainActivity`)
  importa Ktor. Sin SDK del proveedor (ADR 0003). Versión **3.6.0** fijada y comprobada en Maven Central
  por el planner.
- **Dependencias nuevas sí, devDependency de test incluida**: `ktor-client-mock` es
  `testImplementation`; no entra en el APK.
- **Clave en `BuildConfig.DEVEXPERT_API_KEY`** desde `local.properties` (ADR 0002/0003). El valor **no**
  se imprime ni se versiona; la spec solo nombra la propiedad. Si falta o está vacía, el campo es `""` y
  el build no falla.
- **`INTERNET` en el manifiesto**: sin él la primera petición real falla; se conserva
  `ACCESS_NETWORK_STATE` (Búsqueda con IA).
- **`validateResponse` con restricciones "neutras"**: el proveedor decodifica con
  `RecipeValidator.validateResponse(...)` (parseo elemento a elemento, `Malformed` → vacío) pero **sin**
  aplicar dieta ni restricciones duras, porque los tres llamadores ya validan con el Perfil efectivo y
  `ai-search` decide su estado **Bloqueado** por el motivo del descarte: si el proveedor pre-filtrara con
  el Perfil, esas recetas no llegarían al llamador y "Bloqueado" se degradaría a "sin resultados" (regresión
  de una feature aceptada). `dietPatternId = ""` es a propósito un id que no existe en la Taxonomía
  (`taxonomy.dietPattern("") == null`), así que la regla 2 es un no-op y `hardRestrictionIds` vacío deja
  la regla 3 sin candidatos.
- **Consecuencia asumida de lo anterior**: una receta que el proveedor descarta por esquema (no
  decodificable/`*Id` desconocido) o por incoherencia **no** puede aparecer luego en el `discarded` del
  llamador; el conjunto de `accepted` es idéntico (el llamador la descartaría igual), pero se pierde ese
  motivo concreto de diagnóstico. Se documenta como limitación conocida; mejorar la observabilidad del
  motivo en el proveedor sería una feature correctiva de la sección "Observability" de
  `docs/technical-discovery.md`.
- **"Modo semilla" = `AiUnavailableException` (tipada)**: el puerto `RecipeGenerator` **no** tiene tipo
  de resultado y no se cambia (dominio aceptado). Los tres llamadores ya interpretan `Exception` como
  `aiUnavailable`/lote solo semilla, así que la degradación se materializa lanzando una excepción
  **controlada y tipada** (nunca `Error` ni una excepción sin capturar). Un 200 ilegible o vacío **no**
  lanza: devuelve lista vacía (escenario 3).
- **Reintentos a mano, no con el plugin de Ktor**: hace explícita y testeable la política (backoff fijo,
  `Retry-After` acotado, un solo reintento, nunca 4xx no reintentables) y permite aplicar el **deadline
  total** recortando el timeout de cada intento (`min(40 s, restante)`), con el margen comprobado antes
  de reintentar.
- **Deadline total en `execute`**, fijado una vez por operación antes del primer intento; el reloj es
  inyectable (`now: () -> Long`) para poder probarlo en JVM sin esperar 60 s reales (convención de
  `SuggestionBatchService`).
- **`temperature`/`max_tokens`/`response_format` no se envían**: ADR 0003 descarta structured outputs y
  no hay decisión de producto sobre creatividad/longitud; quedan como *Unknown* revisable.
- **Log local con costura, no sistema**: `AiCallEvent` con default no-op (convención de las costuras
  aditivas del repo) cableado a `android.util.Log` en `MainActivity`; nunca contiene la clave ni las
  cabeceras. Sin telemetría (Observability).
- **Sin cambios de UI**: no hay estados ni microcopy nuevos; el estado vacío con "Reintentar" y el
  estado degradado con semillas ya existen (Resultados) y el estado "sin resultados"/"fallo" de la
  Búsqueda con IA también.

### Unknowns

- **Compatibilidad de versiones**: Ktor 3.6.0 con Kotlin 2.2.10, coroutines 1.11.0 y serialization 1.9.0
  no se ha compilado todavía en este repo. Si la resolución de dependencias exige subir alguna versión
  del catálogo, se sube **solo** la mínima necesaria y se registra en la evidencia (no se cambian
  versiones por gusto).
  > Hallazgo del implementer (al implementar): Ktor 3.6.0 compila con el toolchain actual, pero arrastra
  > OkHttp **5.5.0**, cuyo artefacto Android (`okhttp-android`) declara `minCompileSdk=37` y rompe
  > `checkDebugAarMetadata` con el `compileSdk` del repo (API 36.1). El build **fuerza OkHttp 5.4.0**
  > (la última cuyo artefacto Android acepta `compileSdk 36`), sin cambiar el engine ni `compileSdk`: la
  > versión vive en `gradle/libs.versions.toml` (`okhttp`) y la regla en `app/build.gradle.kts`.
  > Documentado en `docs/technical-discovery.md` ("Constraints").
- **Fidelidad del modelo al contrato**: sin structured outputs, el modelo puede responder en prosa o con
  vallas markdown. Se mitiga con la normalización de vallas, `ignoreUnknownKeys` y el decode elemento a
  elemento; JSON embebido en texto (p. ej. "Aquí tienes: {...}") **no** se repara (→ lista vacía /
  estado vacío). Candidato a correctiva si aparece en la práctica.
- **Latencia real de 6 recetas**: si el proveedor tarda más que el deadline total, la operación degrada
  a modo semilla. El valor del deadline (y de `MAX_ATTEMPTS`) es revisable con evidencia real
  (correctiva, sin tocar el dominio).
  > Resuelto por `ai-provider-timeouts` (correctiva, issue #68): con evidencia real de **HTTP 200 en
  > 28,0 s** (6.194 tokens) para 6 recetas con el alias `chat`, los timeouts de `AiProviderPolicy`
  > subieron a socket **90 s**, timeout por intento **120 s** y deadline total **180 s** (connect 10 s
  > sin cambios), y el timeout de socket pasó a clasificarse como `TIMEOUT` (antes `NETWORK`). El
  > `MAX_ATTEMPTS` y el backoff no cambian. Ver `docs/specs/ai-provider-timeouts.md`.
  > Recalibrado después por `ai-call-diagnostics` (correctiva, issue #72): con la misma clave y el
  > mismo prompt de 6 recetas la latencia varía entre modelos/modos —`chat` 28,0 / 33,1 / 42,1 /
  > 46,6 s, `chat`+`json_object` 85,3 s, `chat-pro`+`json_schema` 97,0 s, `deepseek-v4-flash` 52,6 s
  > y `mimo-v2.6-pro` **115,8 s** (peor caso)—, así que los timeouts pasan a socket **120 s**, intento
  > **150 s** y deadline **240 s** (connect, `MAX_ATTEMPTS`, backoff y `Retry-After` sin cambios). Ver
  > `docs/specs/ai-call-diagnostics.md`.
- **Cuota semanal**: un 429 sostenido deja la app en modo semilla hasta el reset; **no** hay microcopy
  específico de cuota en el MVP (decisión de producto); el aviso genérico de modo degradado de la UI ya
  aceptada es suficiente.
- **`Retry-After` en formato fecha HTTP**: solo se interpreta como segundos; otros formatos caen al
  backoff fijo de 500 ms (comportamiento documentado y testeado).
- **`AiCallEvent` no se persiste ni se muestra**: es diagnóstico en `Logcat`; el motivo de descarte del
  Validador sigue registrándose donde ya se registra (llamadores), no en el proveedor.
  > Anotado por `ai-call-diagnostics` (correctiva, issue #72): el proveedor **ya** reporta el motivo en
  > el evento —`AiCallEvent` gana `contentChars`/`contentBytes`, `received`/`accepted` y
  > `discardedByReason` (recuento por `DiscardReason`), y `DevExpertRecipeGenerator` los rellena desde el
  > resultado de `AiRecipePayload.decode`—. Sigue sin persistirse ni mostrarse en la UI; el `Log.d` de
  > `MainActivity` es el único sitio que lo imprime. Ver `docs/specs/ai-call-diagnostics.md`.
- **Clave en el APK**: riesgo ya asumido y documentado (ADR 0002, tabla de riesgos); esta feature no lo
  cambia.

## Validator Checklist

- [ ] El puerto `RecipeGenerator` y el dominio aceptado no cambian; solo `data.ai` (y el cableado de
      `MainActivity`) conoce Ktor.
- [ ] `AiProviderPolicy` es el **único** sitio con las constantes (10/30/40/60 s, 2 intentos, 500 ms,
      `Retry-After` ≤ 5 s) y los helpers puros están cubiertos con casos negativos.
- [ ] Se reintenta **solo** ante timeout/`IOException`/408/429/5xx, con **1** reintento máximo y backoff
      fijo (o `Retry-After` ≤ 5 s); 400/401/403/404/422 **nunca** se reintentan (1 sola llamada).
- [ ] El deadline total (60 s) se aplica en `execute`, antes de cada intento, con el timeout recortado
      (`min(40 s, restante)`); un deadline agotado no inicia un nuevo intento.
- [ ] Un 200 ilegible o vacío **no** lanza y devuelve las recetas decodificables (posiblemente vacías),
      decodificando elemento a elemento; el estado resultante es el vacío con "Reintentar" (Generador).
- [ ] Un 200 con `choices[0].message.content` válido se mapea a `List<Recipe>` y las recetas **no** se
      pre-filtran por dieta/restricciones (el `discarded` del llamador conserva los motivos; `ai-search`
      mantiene su estado Bloqueado).
- [ ] Clave ausente → `MISSING_KEY` **sin** ninguna petición, y los llamadores aceptados degradan a modo
      semilla (`aiUnavailable`/lote solo semilla/estado con semillas) sin fallar.
- [ ] `CancellationException` se relanza siempre (nunca se convierte en `AiUnavailableException`).
- [ ] La petición lleva `Authorization: Bearer <clave>`, `model = "chat"`, `messages` con el `system` del
      `PromptBuilder` y el `user` de la operación; **no** lleva `response_format` ni `stream`.
- [ ] El valor de la clave no está en el repositorio, ni en logs, ni en mensajes de error, ni en tests
      (los tests usan una clave ficticia); `local.properties` sigue ignorado.
- [ ] `INTERNET` está en el manifiesto junto a `ACCESS_NETWORK_STATE`.
- [ ] Los tests de `data.ai` corren en JVM con `MockEngine` (sin red real ni clave real) y con casos
      negativos que no pasan en vacío.
- [ ] `docs/technical-discovery.md`, `docs/domain-model.md` y `docs/risks-and-open-questions.md`
      (pregunta 1 → "Resueltas", tabla sin filas y con nota) quedan coherentes; `init.sh` sin cambios y
      `bash init.sh` en verde.
- [ ] `Visual Design Impact` sin capturas (no hay UI nueva) y evidencia de que no se toca `ui/**` salvo
      el cableado de `MainActivity`.
- [ ] Las dos verificaciones manuales del harness quedan anotadas como **manuales** (emulador en
      Windows), no automatizables desde WSL.
