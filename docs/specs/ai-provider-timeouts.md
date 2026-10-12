# Feature Implementation Spec: Timeouts del proveedor de IA ajustados a la latencia real

## Source Feature

- `id`: `ai-provider-timeouts` · `area`: `integration` · `depends_on`: `["real-ai-provider"]`
  (`accepted`, sesión 067) · `status`: `not_started` · `source`: `feature_list.json`
- No es feature de UI: sin pantallas, estados, microcopy ni capturas.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) `./gradlew test` comprueba que la política **admite la latencia
  real** (socket, request y deadline holgados) y que un **timeout de socket se clasifica como `TIMEOUT`**,
  con casos negativos en la frontera; (2) prueba **manual** en emulador: con clave válida el Generador
  devuelve recetas reales del proveedor (no modo semilla) y con clave ausente sigue degradando a modo
  semilla sin fallar.

**Contexto (correctiva de `real-ai-provider`, issue #68).** El proveedor real ya está conectado y
aceptado, pero degrada a modo semilla en la práctica: el alias `chat` (modelo con *reasoning*) tarda
~28 s o más en devolver 6 recetas, por encima de `SOCKET_TIMEOUT_MILLIS = 30 s` y de
`TOTAL_DEADLINE_MILLIS = 60 s`; la operación agota el socket timeout (etiquetado `NETWORK`), reintenta y
agota el deadline (`TIMEOUT`). Evidencia: logcat (`RETRY`/`NETWORK` ~30 s, `UNAVAILABLE`/`TIMEOUT` ~60 s)
y una petición equivalente al endpoint desde WSL con **HTTP 200 en 28,0 s** (6.194 tokens). No es la
clave ni el endpoint. Candidato ya previsto en los *Unknowns* de `docs/specs/real-ai-provider.md`
("Latencia real de 6 recetas … revisable con evidencia real (correctiva, sin tocar el dominio)").

## Goal

Que una operación normal del Generador (6 recetas, `chat`) quepa dentro de la política: subir
`AiProviderPolicy` a la latencia real (socket, timeout por intento y deadline total holgados) y
clasificar bien el fallo de transporte, de modo que un timeout de socket se reporte como `TIMEOUT` y no
como `NETWORK`. Todo el cambio vive en `com.sazon.app.data.ai` y sus tests: la política sigue en un único
sitio y los reintentos, el backoff, la degradación a modo semilla y el contrato del puerto
`RecipeGenerator` **no cambian**. Resultado: con clave válida, Generador, Lote de la Home y Búsqueda con
IA devuelven recetas reales; si el proveedor se atasca de verdad, la degradación queda registrada con el
motivo correcto (`TIMEOUT`).

## Non-Goals

- **No** se cambia el puerto `RecipeGenerator`, ni el dominio, ni la UI, ni la Taxonomía, ni las bases
  Room, ni `FakeRecipeGenerator`.
- **No** se cambia la política de reintentos: `MAX_ATTEMPTS = 2`, backoff fijo 500 ms, `Retry-After`
  solo si ≤ 5 s, reintentables 408/429/5xx y nunca 400/401/403/404/422. Esta feature ajusta **valores de
  timeout** y **una clasificación**, no el número ni las condiciones de los intentos.
- **No** se añade *streaming*, `response_format`, `max_tokens`, `temperature`, `chat-pro`, selección de
  modelo ni multi-proveedor.
- **No** se añaden dependencias ni cambios de build (`app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `AndroidManifest.xml`, `init.sh`, `scripts/**` no se tocan). Si apareciera una necesidad de build, se
  para y se documenta como decisión/Unknown (como en `real-ai-provider`).
- **No** se añade timeout propio en la UI ni se cambia el estado de carga/cancelación ya aceptado, ni la
  telemetría (sigue el `AiCallEvent` local).
- **No** se cambia el manejo de clave ausente ni la degradación a modo semilla: la verificación 2 es una
  **regresión a preservar**.
- **No** se reescribe la spec histórica `docs/specs/real-ai-provider.md` (solo se anota su Unknown).

## Job Story

Como usuario local con clave válida, quiero que generar recetas con IA espere lo que el modelo tarda de
verdad (media minuto largo) en vez de rendirse por un timeout mal calibrado, para recibir recetas reales
en lugar del estado degradado con semillas.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia nada de lo que ve salvo que las
  recetas pasan a ser reales; sigue pudiendo **cancelar** la carga en Generador y Búsqueda con IA.
- **Desarrollador / agente**: verifica en **JVM** holgura, frontera del recorte y clasificación de
  timeout de socket vs. fallo de red, con `MockEngine` y reloj inyectado: sin red real ni clave real.
- **Sistema**: conserva el contrato de degradación aceptado (`AiUnavailableException` tipada →
  `aiUnavailable` / lote solo semilla) y el deadline total sigue acotando la operación completa.

## Acceptance Scenarios

### Scenario 1: la política admite la latencia real medida (JVM, puro)

Given la latencia medida como referencia documentada en el test (`REAL_LATENCY_MILLIS = 28_000L`,
HTTP 200 con 6.194 tokens),
When se comprueban los valores y el recorte por intento,
Then `SOCKET_TIMEOUT_MILLIS ≥ 2 × REAL_LATENCY`, `REQUEST_TIMEOUT_MILLIS ≥ SOCKET_TIMEOUT_MILLIS`,
`attemptRequestTimeoutMillis(TOTAL_DEADLINE_MILLIS) ≥ 3 × REAL_LATENCY`,
`TOTAL_DEADLINE_MILLIS - REQUEST_TIMEOUT_MILLIS ≥ 2 × REAL_LATENCY` (margen para un reintento lento),
`CONNECT_TIMEOUT_MILLIS = 10_000L` y `REQUEST_TIMEOUT_MILLIS ≥ CONNECT_TIMEOUT_MILLIS`.
Con los valores viejos (30/40/60 s) las tres primeras aserciones **fallan**: es prueba de regresión.

### Scenario 2: timeout de socket ⇒ `TIMEOUT`, no `NETWORK` (JVM, puro y end-to-end)

Given un `MockEngine` que lanza `java.net.SocketTimeoutException` en todos los intentos,
When se invoca `suggestByPreferences(perfil)`,
Then hay exactamente **2** intentos y se lanza `AiUnavailableException(TIMEOUT)` (hoy `NETWORK`), y el
último `AiCallEvent` lleva `outcome = UNAVAILABLE` y `detail = "TIMEOUT"`.

### Scenario 3: timeout de socket en el primer intento y éxito en el segundo

Given un `MockEngine` que lanza `SocketTimeoutException` en la primera llamada y responde 200 con el
contrato en la segunda,
When se invoca la operación,
Then hay 2 intentos, se devuelven las recetas, el evento `RETRY` del primer intento lleva
`detail = "TIMEOUT"` y el backoff es el fijo de 500 ms.

### Scenario 4: frontera — lo que no es timeout sigue siendo `NETWORK`

Given causas de transporte construidas en el test,
When se clasifican con `AiProviderPolicy.transportFailureReason(cause)`,
Then → `TIMEOUT`: `java.net.SocketTimeoutException` (el tipo que expone el engine OkHttp de Ktor en JVM),
`io.ktor.client.plugins.HttpRequestTimeoutException` y `java.io.InterruptedIOException`. → `NETWORK`:
`io.ktor.client.network.sockets.ConnectTimeoutException` (es un `java.net.ConnectException`: nunca se
habló con el proveedor), `java.io.IOException` genérica y `java.net.SocketException`. → `UNEXPECTED`:
`IllegalStateException` y `null`.

### Scenario 5: nada más de la política cambia (regresión)

Given los tests aceptados de `AiProviderPolicyTest` y `DevExpertRecipeGeneratorTest`,
When corre `com.sazon.app.data.ai.*` con los valores nuevos,
Then siguen verdes los escenarios de `real-ai-provider` (429 + `Retry-After` ≤ 5 s y backoff fijo, 4xx
sin reintento, 429 agotado → `RATE_LIMITED`, 5xx/`IOException` → reintento, dos fallos → 2 intentos,
deadline agotado → `TIMEOUT` con 1 intento, cancelación relanzada, clave ausente → `MISSING_KEY` sin
petición, 200 ilegible/vacío → lista vacía, prompts y degradación de los llamadores), salvo la **única**
aserción que codificaba el valor viejo (`attemptRequestTimeoutMillis(60_000L)`), que se actualiza.

### Scenario 6: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida en `local.properties` y el emulador de Android Studio en **Windows**,
When se genera desde el Generador con ingredientes reales (y se usan la Home y la Búsqueda con IA),
Then llegan recetas del proveedor que pasan el Validador y se muestran en Resultados sin degradar a modo
semilla; y con la clave vaciada o ausente, la app entra en **modo semilla** sin fallar.

## Repository Research

### Files Inspected

- `feature_list.json` — entradas `ai-provider-timeouts` (evidencia vacía, dos verificaciones) y
  `real-ai-provider` (evidencia del validador: 57 suites / 371 tests, 28 de ellos de `data.ai`).
- `docs/specs/real-ai-provider.md` — contrato de `AiProviderPolicy` y de `execute` (§2–§5), escenarios
  1–11 y el *Unknown* de latencia que esta feature resuelve.
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — **único** sitio de la política
  (`CONNECT 10_000`, `SOCKET 30_000`, `REQUEST 40_000`, `TOTAL_DEADLINE 60_000`, `MAX_ATTEMPTS 2`,
  `RETRY_BACKOFF_MILLIS 500`, `RETRY_AFTER_MAX_MILLIS 5_000`) y helpers puros (`isRetryableStatus`,
  `retryDelayMillis`, `attemptRequestTimeoutMillis`, `canRetry`, `unavailableReason`).
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — `execute` (deadline fijado una
  vez, recorte por intento, `hasRetryMargin`, `throwDeadlineExhausted`) y `sendAttempt`, con los **dos**
  sitios a corregir: `catch (timeout: HttpRequestTimeoutException)` → `TIMEOUT` y
  `catch (io: IOException)` → `NETWORK`, tanto en la petición como en `bodyAsText()`.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiUnavailableReason` (KDoc de `TIMEOUT` y
  `NETWORK`), `AiCallEvent` (el `detail` de un fallo de transporte es `reason.name`: el motivo cambia a
  `TIMEOUT` de forma visible en logcat).
- `app/src/main/java/com/sazon/app/data/ai/AiHttpClientFactory.kt` — instala `HttpTimeout` **leyendo las
  constantes**; no tiene literales propios, así que **no hace falta tocarlo**. `AiRecipePayload.kt` y
  `OpenAiChat.kt` no participan en timeouts.
- `app/src/test/java/com/sazon/app/data/ai/{AiProviderPolicyTest,DevExpertRecipeGeneratorTest}.kt` —
  convenciones: `runTest`, nombres en español con `_`, `MockAi` con contador de llamadas, clave ficticia
  `test-key`, reloj inyectable, `MockEngine`, casos negativos.
- `docs/technical-discovery.md` (§ proveedor de IA, "Integrations", "Testing and Verification") y
  `docs/risks-and-open-questions.md` (bullet "Errores y reintentos de la IA" en *Resueltas*) — citan
  **valores concretos** (10/30/40/60 s) y quedan desalineados si el código cambia.
- Búsqueda en `app/src/main/java` (`withTimeout|Timeout|MILLIS|deadline`): **no** hay ningún `withTimeout`
  ni deadline de dominio/UI que envuelva al proveedor. Los demás `*_MILLIS` son ajenos al transporte y no
  lo acotan (`BATCH_TTL_MILLIS`, `MIN_VISIBLE_MILLIS`, `SUBLINE_ROTATION_MILLIS`, `DAY_MILLIS`), así que
  subir los timeouts del proveedor es suficiente y no queda un techo oculto.

### Ktor 3.6.0: jerarquía real de excepciones

No se puede inspeccionar el jar de la caché de Gradle desde esta sesión; se inspeccionaron los **fuentes
del tag 3.6.0** (GitHub):

- `ktor-client-core/common/.../HttpTimeout.kt`: `HttpRequestTimeoutException(...) : IOException` (de
  `kotlinx.io`) es lo que lanza el plugin al agotarse el timeout de la petición.
- `ktor-client-core/jvm/.../network/sockets/TimeoutExceptions.kt`:
  `actual typealias SocketTimeoutException = java.net.SocketTimeoutException` y
  `ConnectTimeoutException : java.net.ConnectException`. Es decir, en JVM el "socket timeout" de Ktor es
  literalmente `java.net.SocketTimeoutException`, un `InterruptedIOException` (→ `IOException`); por eso
  hoy cae en el catch genérico y se etiqueta `NETWORK`.
- `ktor-client-okhttp/jvm/.../{OkHttpEngine,OkUtils}.kt`: `socketTimeoutMillis` se aplica como
  `readTimeout`/`writeTimeout` de OkHttp y `mapOkHttpException` convierte el `SocketTimeoutException` del
  engine en el typealias de arriba (o en `ConnectTimeoutException` si era un timeout de conexión).

Conclusión: capturar `java.net.SocketTimeoutException` (o, más general, `java.io.InterruptedIOException`)
**antes** del catch de `IOException` basta; `HttpRequestTimeoutException` se queda como está.

### Existing Patterns To Follow

- Política en un único sitio, pura y testeada en JVM (funciones puras + tests de frontera).
- Costuras aditivas con default (`now`, `onEvent`) y `MockEngine` + reloj inyectado para no esperar tiempo
  real; ningún test debe pasar en vacío (tablas de casos "sí"/"no").
- Alcance estricto: solo `data.ai` y sus tests.

### Current Gaps

- Timeouts por debajo de la latencia real (30/40/60 s vs. ~28 s medidos y "o más").
- El timeout de socket se etiqueta `NETWORK`: la evidencia del logcat y el motivo de la degradación son
  incorrectos.
- Las dos docs durables citan los valores viejos.
- El planner **no ejecutó** `bash init.sh` (no hay cambios de código en esta sesión); el implementer parte
  del verde de la sesión 067 y corre el gate él mismo.

## Technical Approach

### 1. Valores nuevos (sin tocar `MAX_ATTEMPTS`, backoff ni `Retry-After`)

| Constante | Antes | Después | Razón |
|---|---|---|---|
| `CONNECT_TIMEOUT_MILLIS` | 10 s | **10 s** (sin cambios) | La conexión funcionó en la evidencia; el cuello es la respuesta del modelo |
| `SOCKET_TIMEOUT_MILLIS` | 30 s | **90 s** | ≈3× los 28,0 s medidos: una lectura lenta no corta la respuesta |
| `REQUEST_TIMEOUT_MILLIS` | 40 s | **120 s** | Techo por intento; ≥ socket, así el guardián de lectura salta primero y el de petición acota una respuesta que gotea |
| `TOTAL_DEADLINE_MILLIS` | 60 s | **180 s** | Cabe el intento lento + backoff + un segundo intento recortado (~59 s), que sigue siendo > 2× la latencia medida |

Invariantes (los tests las fijan): `0 < CONNECT ≤ REQUEST`, `REQUEST ≥ SOCKET`,
`TOTAL_DEADLINE > REQUEST + RETRY_BACKOFF`. Con estos valores, tras un primer intento que consuma los
120 s quedan 60 s: `canRetry(1, 59_500)` es cierto y el segundo intento recibe
`min(120 s, 59,5 s) = 59,5 s`. La lógica de `attemptRequestTimeoutMillis` no cambia, solo la escala.

### 2. Clasificación del fallo de transporte en un solo sitio

Añadir a `AiProviderPolicy` una función pura:

```kotlin
/** Motivo de degradacion de un fallo de transporte (sin HTTP). */
fun transportFailureReason(cause: Throwable?): AiUnavailableReason = when {
    cause is HttpRequestTimeoutException -> TIMEOUT       // timeout por intento
    cause is SocketTimeoutException -> TIMEOUT            // java.net: timeout de socket (lectura/escritura)
    cause is InterruptedIOException -> TIMEOUT            // call timeout de OkHttp
    cause is IOException -> NETWORK                       // DNS, conexion rechazada, reset...
    else -> UNEXPECTED                                    // incluye null
}
```

`java.net.ConnectException` (el `ConnectTimeoutException` de Ktor) **no** es `InterruptedIOException`: se
decide a propósito que "no llegamos a hablar con el proveedor" es `NETWORK`, no timeout del modelo.

En `DevExpertRecipeGenerator.sendAttempt`, los **dos** bloques de captura delegan en ella:

```kotlin
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: HttpRequestTimeoutException) {
    return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
} catch (failure: SocketTimeoutException) {   // java.net, ANTES del IOException generico
    return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
} catch (failure: IOException) {
    return AttemptOutcome.TransportFailure(AiProviderPolicy.transportFailureReason(failure), failure)
} catch (unexpected: Exception) {            // igual que hoy
    throw AiUnavailableException(AiUnavailableReason.UNEXPECTED, "...", unexpected)
}
```

Igual en el bloque que envuelve `response.bodyAsText()`. El catch explícito de
`HttpRequestTimeoutException` **se conserva** (robusto aunque `kotlinx.io.IOException` no sea alias de
`java.io.IOException` en JVM); si el implementer comprueba que sí lo es, los tres catches pueden
condensarse en uno con los mismos tests y comportamiento. El resto de `execute` (deadline,
`hasRetryMargin`, `throwDeadlineExhausted`, `CancellationException`, `MISSING_KEY`, 200 ilegible →
`EMPTY`) **no se toca**.

### 3. Coherencia en el código

KDoc de `AiUnavailableReason.TIMEOUT` ("plazo total, timeout por intento **o timeout de socket**") y de
`NETWORK` ("fallo de transporte que **no** es un timeout"), y comentario de las constantes con la
referencia a la latencia medida (issue #68).

## Expected File Changes

**Modificar** (solo `data.ai`, sus tests y docs):

- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — valores nuevos + `transportFailureReason`.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — los dos sitios de clasificación
  de `sendAttempt` pasan por `AiProviderPolicy.transportFailureReason`.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — KDoc de `TIMEOUT` y `NETWORK`.
- `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` — corregir la aserción del valor viejo
  y añadir holgura (Scenario 1), frontera del recorte y clasificación (Scenario 4).
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — Scenarios 2, 3 y el intento
  lento.
- `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `docs/specs/real-ai-provider.md` —
  ver *Durable Documentation Impact*.

**No se toca**: `AiHttpClientFactory.kt` (lee las constantes), `AiRecipePayload.kt`, `OpenAiChat.kt`,
`domain/**`, `ui/**`, `data/generator/**`, Room, `app/src/main/assets/**`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md`,
`docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). Screens or states affected: ninguno (la carga ya existe, es
  cancelable y no cambia; sin microcopy, tokens ni acabado nuevos).
- New design artifact required: **no**; `docs/design/concepts/ai-provider-timeouts/` **no** es necesario y
  la feature **no** se bloquea por capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — valores nuevos en el párrafo de `data.ai` (socket 90 s,
  request 120 s, deadline 180 s, recorte `min(120 s, restante)`) y la clasificación (timeout de
  socket/petición → `TIMEOUT`; fallo de conexión/red → `NETWORK`); coherencia en "Integrations" y
  "Testing and Verification".
- `docs/risks-and-open-questions.md`: **update** — el bullet "Errores y reintentos de la IA" de
  *Resueltas* cita valores concretos: pasa a los nuevos y menciona la clasificación. Sin renumerar ni
  tocar tablas.
- `docs/specs/real-ai-provider.md`: **update mínimo** — anotar en el *Unknown* "Latencia real de 6
  recetas" que esta feature la resolvió (28,0 s / 6.194 tokens, valores nuevos), al estilo de la nota
  "Hallazgo del implementer" que ya existe allí. Sus decisiones históricas no se reescriben.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian. `DESIGN.md` y `docs/design/**`:
  **not needed**. `CONSTRAINTS.md` / `ARCHITECTURE.md`: no aplican (el repo los omite).
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. Fijar los valores nuevos en `AiProviderPolicy` (KDoc con la latencia medida).
2. Añadir `transportFailureReason` y hacer que los dos bloques de captura de `sendAttempt` pasen por él.
3. Ajustar el KDoc de `AiUnavailableReason.TIMEOUT`/`NETWORK`.
4. Actualizar `AiProviderPolicyTest` (aserción del valor viejo + tests de holgura, frontera y
   clasificación).
5. Añadir a `DevExpertRecipeGeneratorTest` los casos de socket timeout y el de intento lento.
6. Tirada focal + `bash init.sh` en verde; actualizar las tres docs; el implementer registra estado y
   evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `AiProviderPolicy`: `SOCKET = 90_000L`, `REQUEST = 120_000L`, `TOTAL_DEADLINE = 180_000L`;
      `CONNECT`, `MAX_ATTEMPTS`, backoff y `Retry-After` intactos; KDoc con la latencia medida.
- [ ] `AiProviderPolicy.transportFailureReason(cause)` pura con la tabla del *Technical Approach* y
      `null → UNEXPECTED`.
- [ ] `sendAttempt`: capturar `java.net.SocketTimeoutException` **antes** del `IOException` genérico (en la
      petición y en `bodyAsText()`), delegando el motivo en el clasificador; `CancellationException` sigue
      relanzándose primero.
- [ ] KDoc de `TIMEOUT` (incluye timeout de socket) y `NETWORK` (fallo no-timeout).
- [ ] `AiProviderPolicyTest`: `attemptRequestTimeoutMillis(60_000L)` (hoy comparado con
      `REQUEST_TIMEOUT_MILLIS`, fallaría con 120 s) pasa a usar un valor por encima de la constante;
      fronteras del recorte (`REQUEST ± 1`, `TOTAL_DEADLINE_MILLIS`, `0`, negativo); `REAL_LATENCY_MILLIS =
      28_000L` y aserciones de holgura; clasificación completa (positivos y negativos).
- [ ] `DevExpertRecipeGeneratorTest`: dos `SocketTimeoutException` → 2 intentos + `TIMEOUT` + evento
      `UNAVAILABLE`/`detail = "TIMEOUT"`; `SocketTimeoutException` + 200 → 2 intentos, recetas y evento
      `RETRY`/`detail = "TIMEOUT"`; intento lento (~35 s de reloj inyectado) → 1 llamada y recetas.
- [ ] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
      verde (0 errores de lint y sin warnings nuevos en ficheros de la feature).
- [ ] Inspección de alcance: `git diff --stat` solo con lo previsto; `git grep` de `30_000`/`40_000`/
      `60_000` en `data.ai` sin resultados.
- [ ] Docs durables (`technical-discovery.md`, `risks-and-open-questions.md`, nota en
      `real-ai-provider.md`).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).
- `AiProviderPolicyTest` — **holgura** (`REAL_LATENCY_MILLIS = 28_000L` documentado en el test):
  `SOCKET_TIMEOUT_MILLIS ≥ 2 × REAL_LATENCY`; `REQUEST_TIMEOUT_MILLIS ≥ SOCKET_TIMEOUT_MILLIS`;
  `attemptRequestTimeoutMillis(TOTAL_DEADLINE_MILLIS) ≥ 3 × REAL_LATENCY`;
  `TOTAL_DEADLINE_MILLIS - REQUEST_TIMEOUT_MILLIS ≥ 2 × REAL_LATENCY` y
  `canRetry(1, TOTAL_DEADLINE_MILLIS - REQUEST_TIMEOUT_MILLIS - RETRY_BACKOFF_MILLIS)` cierto;
  `CONNECT_TIMEOUT_MILLIS == 10_000L` y `≤ REQUEST_TIMEOUT_MILLIS`. **Las tres primeras fallan con los
  valores viejos** (prueba de regresión real).
- `AiProviderPolicyTest` — **frontera del recorte**: `attemptRequestTimeoutMillis(TOTAL_DEADLINE_MILLIS) ==
  REQUEST_TIMEOUT_MILLIS`; `(REQUEST + 1) == REQUEST`; `(REQUEST - 1) == REQUEST - 1`; `(0) == 0`;
  `(negativo) == 0`.
- `AiProviderPolicyTest` — **clasificación**: `java.net.SocketTimeoutException("timeout")`,
  `HttpRequestTimeoutException("https://…", 120_000)` e `InterruptedIOException("timeout")` → `TIMEOUT`;
  `io.ktor.client.network.sockets.ConnectTimeoutException("connect", null)`, `IOException("sin red")` y
  `java.net.SocketException("broken pipe")` → `NETWORK`; `IllegalStateException("boom")` y `null` →
  `UNEXPECTED`. (Para el timeout de socket basta el tipo, no la función fábrica homónima de
  `io.ktor.client.plugins`: en JVM son el mismo `java.net.SocketTimeoutException`.)
- `DevExpertRecipeGeneratorTest` (`MockEngine`, reloj inyectado, clave ficticia `test-key`): socket timeout
  en los dos intentos → **2** llamadas, `AiUnavailableException(TIMEOUT)` y evento final `UNAVAILABLE` con
  `detail = "TIMEOUT"`; socket timeout y luego 200 → 2 llamadas, recetas y `RETRY` con
  `detail = "TIMEOUT"`; intento lento (~35 s de reloj) → **1** llamada y recetas.
- Regresión: los 28 tests actuales de `data.ai` siguen verdes (salvo la aserción corregida), incluidos
  429 + `Retry-After`, 4xx sin reintento, deadline agotado → `TIMEOUT` con 1 intento, cancelación,
  `MISSING_KEY` sin petición, 200 ilegible/vacío y la degradación de `GenerationService`/`AiSearchService`.
- **Lint:** `bash init.sh` → 0 errores y sin warnings nuevos (los 13 preexistentes se mantienen).
- **Inspección:** `git diff --stat` limitado a lo previsto; `AiHttpClientFactory.kt` sin cambios (prueba de
  que no hay literales duplicados); `MAX_ATTEMPTS`, `RETRY_BACKOFF_MILLIS` y `RETRY_AFTER_MAX_MILLIS` sin
  cambios.
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):**
  - Clave válida: `bash scripts/gradle.sh assembleDebug` y ejecutar; generar desde el Generador y
    comprobar que **llegan recetas reales** que pasan el Validador (verificación 2, parte 1); repetir en
    Home (Lote) y Búsqueda con IA, cronometrando (~28 s medidos; carga cancelable).
  - Logcat: aparece `SUCCESS` (o `EMPTY` si el modelo no devuelve JSON legible) en vez de
    `RETRY`/`UNAVAILABLE`; si aun así hay timeout, el `detail` es `TIMEOUT`, nunca `NETWORK` para un
    timeout de lectura.
  - Clave vaciada o ausente: sigue entrando en **modo semilla** sin fallar (verificación 2, parte 2);
    cambiar `local.properties` exige reconstruir.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; el `MockEngine` cubre el transporte en JVM y
  lo visual/manual lo revisa la persona). `init.sh` **no cambia**.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.*` con recuento de tests y 0 fallos
  (esperado: 371 preexistentes + los nuevos, sin eliminar ninguno).
- Nombres y resultado de los tests nuevos: holgura, clasificación pura, socket timeout → `TIMEOUT`, socket
  timeout + éxito e intento lento; nota de que el escenario 2 daría `NETWORK` contra `main` (regresión).
- `git diff --stat` de lo tocado y confirmación de que `AiHttpClientFactory.kt`, `domain/**`, `ui/**`,
  build, Taxonomía, `seed-recipes.json`, `init.sh` y `DESIGN.md` no cambian.
- Resultado **manual** del emulador (clave válida → recetas reales y tiempo observado; clave ausente →
  modo semilla) y la línea de logcat con el `detail`.
- Docs actualizadas y anotación de estado/evidencia en `feature_list.json` y `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **Valores**: connect 10 s (sin cambios), socket **90 s**, request **120 s**, deadline **180 s**; la
  razón de cada uno está en la tabla. Regla: un intento lento real (28 s medidos, con holgura) cabe en el
  primer intento y, tras uno agotado, quedan ≥ 2× la latencia medida para el reintento.
- **`MAX_ATTEMPTS`, backoff y `Retry-After` no se tocan**: el problema era la calibración del tiempo y la
  etiqueta del motivo, no la política de reintentos ya aceptada.
- **Timeout de socket ⇒ `TIMEOUT`, no `NETWORK`**: en JVM el `SocketTimeoutException` de Ktor es
  `java.net.SocketTimeoutException` (typealias verificado en 3.6.0), un `InterruptedIOException`;
  significa "el modelo tardó en responder", no "no hay red", y el `AiCallEvent` debe decir la verdad.
- **La conexión sigue siendo `NETWORK`**: `ConnectTimeoutException` de Ktor es un `java.net.ConnectException`
  (nunca se habló con el proveedor); se prueba como caso negativo.
- **Clasificación en `AiProviderPolicy`**: junto al resto de la política y testeable de forma pura,
  además de end-to-end con `MockEngine`. Importa tipos de Ktor, pero no hace HTTP.
- **`AiHttpClientFactory` no se toca** (lee las constantes) y **no hay cambios de build ni de UI**: no
  hacen falta dependencias y el usuario ya puede cancelar.

### Unknowns

- **La latencia real puede ser mayor y variable**: 28,0 s es **una** medición. Si aparecen operaciones
  > 90 s, la degradación seguirá siendo honesta (`TIMEOUT`) y la calibración es revisable en el mismo
  sitio (nueva correctiva), incluidos timeouts distintos por alias (`chat` vs. `chat-pro`), hoy fuera de
  alcance.
- **UX de la espera**: 180 s de deadline es mucha pantalla de carga; Generador y Búsqueda tienen
  "Cancelar". Si molesta, el arreglo sería de UI (progreso/estimación), no de esta feature.
- **Recorte por intento y caché de clientes de Ktor**: `timeout { requestTimeoutMillis = … }` cambia la
  capability por intento y el engine OkHttp cachea un cliente por configuración de capability, así que un
  reintento puede crear cliente/pool nuevos. Ya ocurría con los valores viejos, no altera el resultado y
  queda fuera de alcance; se anota para no confundirlo con una regresión.
- **`socketTimeoutMillis` de Ktor aplica a lectura y escritura**; el mensaje enviado es pequeño, así que
  subirlo no tiene efectos adversos esperables.
- **`HttpRequestTimeoutException` y `java.io.IOException`**: no se pudo confirmar desde esta sesión que
  `kotlinx.io.IOException` sea alias de `java.io.IOException` en JVM; por eso el contrato mantiene catches
  explícitos y el clasificador decide el motivo (mismo comportamiento en ambos casos).
- **El motivo de descarte del Validador sigue sin registrarse en el proveedor** (limitación aceptada de
  `real-ai-provider`); no cambia aquí.

## Validator Checklist

- [ ] `AiProviderPolicy` sigue siendo el **único** sitio de la política: socket 90 s, request 120 s,
      deadline 180 s; connect 10 s, `MAX_ATTEMPTS`, backoff y `Retry-After` **sin cambios**.
- [ ] Los tests demuestran la **holgura frente a la latencia medida** y **fallan** con los valores viejos.
- [ ] Un **timeout de socket** se clasifica como `TIMEOUT` (puro y con `MockEngine`, incluido el `detail`
      del evento); los negativos (`ConnectTimeoutException`, `IOException`, `SocketException`,
      `IllegalStateException`, `null`) quedan como `NETWORK`/`UNEXPECTED`.
- [ ] El recorte por intento se prueba en la frontera (`REQUEST ± 1`, `TOTAL_DEADLINE`, 0, negativo).
- [ ] El resto de la política sigue en verde (429 + `Retry-After`, 4xx sin reintento, 429 agotado,
      5xx/`IOException`, deadline agotado → `TIMEOUT` con 1 intento, cancelación relanzada, `MISSING_KEY`
      sin petición, 200 ilegible/vacío, degradación de los llamadores).
- [ ] Alcance: solo `data.ai`, sus tests y las tres docs; `AiHttpClientFactory.kt`, `domain/**`, `ui/**`,
      build, Taxonomía, `seed-recipes.json`, `init.sh` y `DESIGN.md` intactos; sin dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos).
- [ ] Docs sin valores viejos (10/30/40/60) y con la nota de Unknown trazada en `real-ai-provider.md`.
- [ ] La verificación **manual** en emulador queda anotada como tal (clave válida → recetas reales; clave
      ausente → modo semilla), no automatizable desde WSL.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por implementer/validador/orquestador.
