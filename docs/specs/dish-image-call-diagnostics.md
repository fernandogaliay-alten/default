# Feature Implementation Spec: Diagnóstico de la llamada de imagen (cuota y estado HTTP visibles en logcat)

## Source Feature

- `id`: `dish-image-call-diagnostics` · `area`: `integration` · `depends_on`:
  `["dish-image-generator"]` (`accepted`, sesión 090) · `status`: `not_started` · `source`:
  `feature_list.json`
- **No es feature de UI**: `area: integration`; sin pantallas, estados, microcopy ni capturas. No usa
  `docs/design/concepts/` y **no** se bloquea por Stitch.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) **Test JVM**: la llamada de imagen emite un evento de
  diagnóstico con el **estado HTTP** y el **motivo clasificado** (`CUOTA` en 402/429, `AUTH` en
  401/403, `SERVER` en 5xx, `TRANSPORT` en fallo de red/timeout, `INVALID` en cuerpo/codificación
  ilegible, `OK` en 2xx con bytes); (2) **Test JVM**: el generador sigue devolviendo `null` **sin
  lanzar** ante cualquier fallo y el evento **no** expone la clave ni la cabecera `Authorization`;
  (3) `bash init.sh` sigue en verde y **sin dependencias nuevas**.
- `user_visible_behavior` (textual): no cambia la UI; en logcat cada llamada de generación/ajuste de
  imagen deja una línea con el estado HTTP y el motivo clasificado, de modo que un fallo de cuota de
  DevExpert deja de ser invisible.
- `notes` (textual): correctiva pedida por la persona (2026-10-15) al comprobar que la llamada de
  imagen falla en silencio; alcance = observar la llamada (estado HTTP + motivo) con el patrón
  `AiCallEvent` del chat, consumida por un `Log.d` (tag `SazonDishImage`) en `MainActivity`; **no**
  cambia la cadena de resolución, la caché, el gate del interruptor ni el tope, y **no** convierte un
  fallo en excepción ni en UI de error.

## Goal

Un único cambio acotado, en `com.sazon.app.data.ai.image` (código + tests) y **una** línea de log en
`MainActivity`:

`DevExpertDishImageGenerator` gana una costura **aditiva** `onEvent: (DishImageCallEvent) -> Unit = {}`
(más `now: () -> Long` para la duración) y emite **un evento por cada llamada HTTP** real —la petición
de texto (`/images/generations`) o la de ajuste (`/images/edits`)— con el **estado HTTP**, el
**motivo clasificado** (`OK`/`CUOTA`/`AUTH`/`SERVER`/`TRANSPORT`/`INVALID`/`HTTP_ERROR`), la
**duración** y el tamaño de los **bytes entregados**. La clasificación es una función **pura** en
`DishImagePolicy` (único sitio de la política), testeable en JVM. `MainActivity` cablea el evento a
`Log.d("SazonDishImage", …)`.

El contrato de fallo **no** cambia: `null` **sin excepción**, la cancelación se propaga y sigue
habiendo **una sola** petición por camino (`MAX_ATTEMPTS = 1`). El evento **nunca** contiene la clave,
la cabecera `Authorization`, el prompt ni el cuerpo. La cadena de resolución (memoria → disco →
semilla → generador), la caché, el gate del interruptor (`GatedDishImageGenerator`) y el tope por
sesión quedan **intactos**: solo se **observa** la llamada que ya ocurría.

## Non-Goals

- **No** se toca la cadena de resolución (`DishImageResolver`), la caché (`FileDishImageCache`,
  `MemoryDishImageCache`), el set semilla, el gate del interruptor (`GatedDishImageGenerator`), el
  tope por sesión (`DishImageGenerationBudget`) ni el ajuste (fase 4). Un acierto de caché, un acierto
  de semilla o un gate cerrado **no** producen evento (no hay llamada).
- **No** se cambia la política de imagen: endpoint, base, alias de modelo, `IMAGE_SIZE`,
  `TARGET_WIDTH_PX`, `WEBP_QUALITY`, `TIMEOUTS` (10/30/40 s), `MAX_ATTEMPTS = 1`,
  `EDIT_FALLBACK_STATUSES` ni `editFailureRequiresTextFallback`.
- **No** se convierte ningún fallo en excepción, en UI de error, en reintento ni en respaldo nuevos.
  El evento es **observación pasiva**: si se emite o no, el resultado de `generate` es idéntico.
- **No** se cambia el puerto `DishImageGenerator` ni el dominio (`domain/image/**`).
- **No** se toca el diagnóstico del **chat** (`AiCallEvent`, `AiDiagnostics.kt`,
  `DevExpertRecipeGenerator`, `Log.d("SazonAi", …)`): el tipo nuevo es propio de la imagen.
- **No** se registra la clave, la cabecera `Authorization`, el prompt ni el cuerpo de la respuesta;
  tampoco se persiste ni se envía telemetría (evento local, de solo lectura en Logcat).
- **No** se añaden dependencias, ni se toca `build.gradle.kts`, `gradle/libs.versions.toml`,
  `AndroidManifest.xml`, `init.sh` ni `scripts/**`.
- **No** hay UI, ni `DESIGN.md`, ni capturas, ni `docs/design/**`.

## Job Story

Como desarrollador que depura con clave real en Android Studio,
quiero que logcat diga, para cada llamada HTTP de imagen, el estado devuelto y un motivo clasificado
(`CUOTA` en un 402/429, `AUTH` en 401/403, `SERVER` en 5xx, `TRANSPORT` en fallo de red/timeout,
`INVALID` en cuerpo ilegible),
para distinguir al instante una cuota agotada de DevExpert de un problema de clave, de red o de
codificación, sin exponer la clave ni el cuerpo.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia **nada** de lo que ve: sigue la
  degradación silenciosa a la ficha tipográfica (ADR 0005/0006) y no aparece ningún error.
- **Desarrollador / agente**: verifica en **JVM** con `MockEngine` y un `DishImageReducer` falso; sin
  red real ni clave real (clave ficticia `test-key`). Nunca usa Android (el adaptador es transporte +
  códec puros).
- **Sistema**: conserva el contrato `null`/cancelación del puerto y el único intento por camino; el
  `Log.d` es local y solo de diagnóstico.

## Acceptance Scenarios

### Scenario 1: 2xx con bytes ⇒ evento `OK` (camino de texto)

Given un `MockEngine` que responde `200` con `{"data":[{"b64_json":"…"}]}` y un reductor que devuelve
esos bytes,
When se invoca `generate(request)` con `onEvent` capturado,
Then se captura **exactamente un** evento `call = GENERATE`, `reason = OK`, `httpStatus = 200`,
`bytes > 0`, `durationMillis >= 0`; la llamada devuelve los bytes y hay **una** petición.

### Scenario 2: clasificación de estados no-2xx y de transporte

Given peticiones basadas en el estado,
When se invoca `generate`,
Then el evento es: `402` → `CUOTA`; `429` → `CUOTA`; `401` → `AUTH`; `403` → `AUTH`; `500`/`503` →
`SERVER`; `400`/`404`/`408` → `HTTP_ERROR`; y un fallo de transporte
(`IOException`/`SocketTimeoutException`/`HttpRequestTimeoutException`) → `TRANSPORT` con
`httpStatus = null`. En todos: `bytes = null`, la llamada devuelve `null` y hay **una** petición.

### Scenario 3: 2xx ilegible ⇒ `INVALID`

Given un `MockEngine` que responde `200` con un cuerpo ilegible o inutilizable (`"no soy json"`,
`"{}"`, `"{\"data\":[]}"`, `b64_json` vacío, base64 inválido) o con bytes que el reductor rechaza,
When se invoca `generate`,
Then el evento es `reason = INVALID`, `httpStatus = 200`, `bytes = null`; la llamada devuelve `null`
y hay **una** petición.

### Scenario 4: camino de ajuste (con referencia) ⇒ `EDIT`

Given una `DishImageStyleReference` utilizable,
When se invoca `generate`,
Then el ajuste emite `call = EDIT` (Scenario 1 con `200` → `OK`; transport → `TRANSPORT` con
`httpStatus = null`); un rechazo definitivo (`400/404/405/413/415/422`) o una carga `2xx`
inutilizable del ajuste emiten `EDIT` (`HTTP_ERROR`/`INVALID` respectivamente) y **después** el
respaldo por texto emite su propio evento `GENERATE`, de modo que un rechazo con respaldo deja **dos**
eventos (y **dos** peticiones, sin una tercera).

### Scenario 5: el evento nunca expone la clave ni la cabecera `Authorization`

Given cualquier evento emitido (clave ficticia `test-key` y prompt reconocible),
When se serializan con `toString()`,
Then **ninguno** contiene `test-key`, `Bearer`/`Authorization` ni el texto del `prompt`: solo la
llamada, el motivo, el estado HTTP, la duración, `detail` y el recuento de bytes.

### Scenario 6: nada más cambia (regresión)

Given la clave en blanco (0 peticiones, `null` **sin** evento), la cancelación (se **relanza**, no se
convierte en `null`), los `4xx/5xx`/transporte (un solo intento, sin reintento) y los 22 tests
actuales de `com.sazon.app.data.ai.image.*`,
When corre la tirada,
Then siguen verdes **sin cambiar aserciones** (el evento es aditivo con default no-op) y el
comportamiento observable es el mismo.

### Scenario 7: `DishImagePolicy.reasonFor` es pura y total (JVM, puro)

Given la función `reasonFor(httpStatus, deliveredBytes)`,
When se le pasan los estados de los Scenarios 1-2 y `null`,
Then devuelve la clasificación requerida y, para un `2xx`, `OK` si `deliveredBytes` y `INVALID` si no;
el caso negativo no pasa en vacío (`402 != AUTH`, `500 != CUOTA`, etc.).

### Scenario 8: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida en `local.properties`, `BuildConfig.DEBUG` y el emulador de Android
Studio en **Windows** (el `adb` de WSL no lo ve),
When se pinta una imagen de plato no cacheada con el interruptor encendido,
Then logcat (tag `SazonDishImage`) muestra una línea por llamada con llamada, motivo, estado HTTP,
duración y bytes; con la cuota agotada aparece `CUOTA status=402|429` (o el estado real del
proveedor) en lugar del fallo mudo.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `dish-image-call-diagnostics` (alcance y verificación ya acordados).
- `docs/specs/ai-call-diagnostics.md` — patrón de correctiva de diagnóstico: costura `onEvent`
  aditiva, evento local sin clave/cabeceras, test de no-fuga y log en `MainActivity`.
- `docs/specs/dish-image-generator.md` / `docs/specs/dish-style-reference.md` — contrato del
  adaptador (una petición por camino, respaldo acotado por estado) y hallazgo L2 "sin diagnóstico,
  degrada en silencio" que esta correctiva atiende.
- `docs/adr/0005-ai-dish-images.md` (líneas ~121-134, ~244-266, ~310-316) — contrato `null` sin
  excepción, `MAX_ATTEMPTS = 1`, respaldo por estado; describe el adaptador como "mudo (sin `Log`)".
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — `generate`,
  `editImage`, `generateFromText`, `EditOutcome` (`Edited`/`FallbackToText`/`Unavailable`); **no**
  emite ningún evento hoy.
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — `TIMEOUTS`, `MAX_ATTEMPTS`,
  `EDIT_FALLBACK_STATUSES`, `editFailureRequiresTextFallback` (precedente de función pura de política).
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePayload.kt` — `decode` tolerante que
  **nunca lanza** y devuelve `null` (base para clasificar `INVALID`).
- `app/src/main/java/com/sazon/app/data/ai/image/DishImageReducer.kt` / `AndroidDishImageReducer.kt` —
  `reduceToWebp` es `fun interface` inyectable (fake en JVM).
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — patrón `AiCallEvent` (data class con
  campos de diagnóstico, sin clave/cabeceras) a imitar con un tipo propio de la imagen.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — costura `onEvent` con
  default no-op, `now` inyectable, `report(...)` y `detail = "HTTP ${status}"`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas ~115-147 `SazonAi`; ~200-242 cadena de
  imagen y `Log.d("SazonDishImage", …)` del origen) — punto de cableado del evento y tag de log.
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` (`GatedDishImageGenerator`,
  ~146-153) y `DishImageResolver.kt` — capas **por encima** del adaptador; quedan intactas.
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — contrato del puerto (`null`,
  cancelación); **no** se toca.
- `app/src/test/java/com/sazon/app/data/ai/image/DevExpertDishImageGeneratorTest.kt` —
  `MockImages` (+ `calls`), `passThrough()`, clave `test-key`, escenarios 1-10 de las fases 2 y 6.
- `app/src/test/java/com/sazon/app/data/ai/image/DishImagePolicyTest.kt` — convenciones de la
  política pura (mapeo de estados, frontera, caso negativo).
- `docs/technical-discovery.md` — "Observability" (~1221-1254) y cadena de imagen y diagnóstico de
  origen (~915-932).

### Existing Patterns To Follow

- **Costura aditiva con default**: `(T) -> Unit = {}` y `now: () -> Long = System::currentTimeMillis`
  al **final** de la lista de parámetros (los tests/constructores actuales no cambian).
- **Política pura en un único sitio**, testeada en JVM con caso negativo (`editFailureRequiresTextFallback`).
- **Evento "solo longitudes y categorías"**: nunca clave, `Authorization`, prompt ni cuerpo.
- **`detail` corto y determinista** (`"HTTP ${status}"` o `"transport"`), como en el chat.
- **Tests JVM con `MockEngine`** y reductor falso; clave ficticia `test-key`; sin Robolectric.
- **Un evento por llamada HTTP** con `durationMillis`, sin telemetría ni persistencia.

### Current Gaps

- `DevExpertDishImageGenerator` no emite **nada**: una cuota agotada (402/429) o un 401 son
  invisibles en logcat (hallazgo L2 de `dish-image-generator`).
- `DishImagePolicy` no tiene una clasificación pura de estado HTTP → motivo.
- No existe un tipo de evento de llamada de imagen (el `AiCallEvent` del chat es de otro dominio y no
  se reutiliza, para no acoplar chat e imagen).
- `docs/adr/0005-ai-dish-images.md` y el KDoc del adaptador afirman "sin `Log` / mudo": quedan
  obsoletos y hay que anotarlos.
- El planner **no** ejecutó `bash init.sh` (no hay cambios de código en esta sesión); el implementer
  parte del verde del `HEAD` actual (115 suites / 911 tests) y corre el gate él mismo.

## Technical Approach

### 1. Tipo de evento — nuevo `com.sazon.app.data.ai.image.DishImageDiagnostics`

Fichero nuevo `DishImageDiagnostics.kt` (tipos **públicos**: el evento viaja en un parámetro de
constructor de la clase pública del adaptador):

```kotlin
/** Llamada HTTP de imagen observada. */
enum class DishImageCall { GENERATE, EDIT }

/** Motivo clasificado de una llamada de imagen. */
enum class DishImageCallReason { OK, CUOTA, AUTH, SERVER, TRANSPORT, INVALID, HTTP_ERROR }

/** Evento de diagnóstico LOCAL de una llamada HTTP de imagen (sin telemetría). Nunca clave/Auth. */
data class DishImageCallEvent(
    val call: DishImageCall,
    val reason: DishImageCallReason,
    val httpStatus: Int?,          // null si no hubo respuesta (transporte)
    val durationMillis: Long,
    val detail: String,            // "HTTP <status>" o "transport"; sin prompt ni cuerpo
    val bytes: Int? = null,        // bytes entregados; solo no nulo en OK
)
```

### 2. Clasificación pura — `DishImagePolicy.reasonFor` (único sitio)

```kotlin
fun reasonFor(httpStatus: Int?, deliveredBytes: Boolean): DishImageCallReason = when {
    httpStatus == null -> DishImageCallReason.TRANSPORT
    httpStatus == 402 || httpStatus == 429 -> DishImageCallReason.CUOTA
    httpStatus == 401 || httpStatus == 403 -> DishImageCallReason.AUTH
    httpStatus in 500..599 -> DishImageCallReason.SERVER
    httpStatus in 200..299 -> if (deliveredBytes) DishImageCallReason.OK else DishImageCallReason.INVALID
    else -> DishImageCallReason.HTTP_ERROR   // residual: 400/404/408/… y cualquier otro
}
```

Cubre **todos** los mapeos exigidos y deja el residual en `HTTP_ERROR` (documentado). `INVALID`
incluye "2xx sin bytes entregables" (cuerpo ilegible, base64 inválido o bytes que el reductor no
convierte). Sin estado en blanco: no hay evento (ver abajo).

### 3. Emisión en el adaptador — `DevExpertDishImageGenerator`

Parámetros **nuevos al final con default** (aditivos):

```kotlin
private val onEvent: (DishImageCallEvent) -> Unit = {},
private val now: () -> Long = System::currentTimeMillis,
```

Reglas de emisión (una por **llamada HTTP**; el resultado de `generate` no depende de ellas):

| Punto | `call` | Cuándo | `httpStatus` | `reason` | `bytes` |
|---|---|---|---|---|---|
| Clave en blanco | — | `apiKey.isBlank()` | **sin evento** (no hay llamada) | | |
| `/images/edits` | `EDIT` | transporte antes de respuesta | `null` | `TRANSPORT` | `null` |
| `/images/edits` | `EDIT` | no-2xx | estado | `reasonFor(status, false)` | `null` |
| `/images/edits` | `EDIT` | 2xx, fallo al leer cuerpo | estado | `TRANSPORT` | `null` |
| `/images/edits` | `EDIT` | 2xx, decode/reduce nulo o vacío | `200` | `INVALID` | `null` |
| `/images/edits` | `EDIT` | 2xx con bytes reducidos | `200` | `OK` | tamaño |
| `/images/generations` | `GENERATE` | transporte antes de respuesta | `null` | `TRANSPORT` | `null` |
| `/images/generations` | `GENERATE` | no-2xx | estado | `reasonFor(status, false)` | `null` |
| `/images/generations` | `GENERATE` | 2xx, fallo al leer cuerpo | estado | `TRANSPORT` | `null` |
| `/images/generations` | `GENERATE` | 2xx, decode/reduce nulo | `200` | `INVALID` | `null` |
| `/images/generations` | `GENERATE` | 2xx con bytes | `200` | `OK` | tamaño |

- `detail = if (httpStatus != null) "HTTP $httpStatus" else "transport"`.
- `durationMillis = (now() - startedAt).coerceAtLeast(0L)`, con `startedAt = now()` al entrar en cada
  fase (`editImage` / `generateFromText`); es por llamada, no por `generate` completo.
- **Un rechazo con respaldo** emite `EDIT` y, después, `GENERATE`: dos eventos y dos peticiones (el
  respaldo sigue acotado por estado; no se añade una tercera llamada).
- La **cancelación** (`CancellationException`) se relanza **antes** de emitir: no hay evento de una
  llamada cancelada (el gate y la caché no se tocan).
- La emisión se hace con un helper privado `report(call, httpStatus, startedAt, bytes)` que llama a
  `onEvent`. El adaptador sigue siendo transporte + códec: sin `Log`, sin dominio, sin `cacheKey`.

### 4. Cableado en `MainActivity`

En la construcción de `DevExpertDishImageGenerator(...)` (líneas ~204-209) se añade:

```kotlin
onEvent = { event ->
    Log.d(
        "SazonDishImage",
        "${event.call} ${event.reason} status=${event.httpStatus} " +
            "${event.durationMillis}ms bytes=${event.bytes} ${event.detail}",
    )
},
```

Un solo cambio fuera de `data.ai.image`. Es local, sin clave ni cabeceras; se cablea **sin** gate de
debug, igual que el `onEvent` del chat `SazonAi` (ver *Decisions*).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/data/ai/image/DishImageDiagnostics.kt` — `DishImageCall`,
  `DishImageCallReason` y `DishImageCallEvent`.

**Modificar (código)**

- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — `reasonFor(...)` pura + KDoc.
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — `onEvent`/`now`,
  helper `report(...)`, emisión por llamada en `editImage`/`generateFromText`; KDoc actualizado (ya no
  es "mudo").
- `app/src/main/java/com/sazon/app/MainActivity.kt` — `onEvent` → `Log.d("SazonDishImage", …)`.

**Modificar (tests)**

- `app/src/test/java/com/sazon/app/data/ai/image/DishImagePolicyTest.kt` — mapeo de `reasonFor` con
  los seis motivos exigidos, residual `HTTP_ERROR`, `2xx` → `OK`/`INVALID` y caso negativo.
- `app/src/test/java/com/sazon/app/data/ai/image/DevExpertDishImageGeneratorTest.kt` — eventos por
  escenario (OK, CUOTA/AUTH/SERVER/HTTP_ERROR, TRANSPORT, INVALID, EDIT y respaldo) y test de no-fuga.

**No se toca**: `DishImagePayload.kt`, `DishImageReducer.kt`, `AndroidDishImageReducer.kt`,
`DishImageStyleReference*.kt`, `data/image/**` (caché, resolver, gate, tope, ajuste, descriptor),
`domain/**`, `ui/**`, `data/ai/AiDiagnostics.kt` y el resto de `data.ai` (chat), `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `AndroidManifest.xml`, `app/src/main/assets/**`, `init.sh`, `scripts/**`,
`DESIGN.md`, `docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). Sin pantalla, estado, microcopy, token ni acabado nuevos;
  el único cambio fuera de `data.ai.image` es una línea de `Log.d` en `MainActivity`.
- Design source: not applicable. Screens or states affected: ninguno.
- New design artifact required: **no**. `docs/design/concepts/dish-image-call-diagnostics/` **no** es
  necesario y la feature **no** se bloquea por capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — "Observability" (~1221-1254): añadir la costura
  `DishImageCallEvent` (`data.ai.image`: llamada, motivo clasificado, estado HTTP, duración, bytes),
  el `Log.d("SazonDishImage", …)` y el mapeo `CUOTA`/`AUTH`/`SERVER`/`TRANSPORT`/`INVALID`/`OK`
  (residual `HTTP_ERROR`), y que nunca lleva clave/`Authorization`/prompt/cuerpo. Si el párrafo de la
  cadena de imagen (~915-932) describe el adaptador como mudo, anotarlo.
- `docs/adr/0005-ai-dish-images.md`: **update mínimo** — las menciones "no registra nada (sin `Log`)"
  (~132) y "transporte puro y mudo (sin `Log`)" (~257) pasan a "transporte + códec puros con
  diagnóstico local aditivo (`onEvent`, correctiva `dish-image-call-diagnostics`)"; el contrato `null`
  sin excepción, `MAX_ATTEMPTS = 1` y el respaldo por estado **no** cambian.
- `docs/specs/dish-image-generator.md`: **update mínimo** — nota en su hallazgo L2 ("decodificación
  estricta + sin diagnóstico") de que esta correctiva lo atiende con el evento + log; sus escenarios y
  valores históricos no se reescriben.
- `docs/domain-model.md`, `docs/domain/taxonomy.md`, `AGENTS.md`, `DESIGN.md`, `docs/design/**`:
  **not needed** — no cambian entidades, prompts, flujo, arranque ni dirección visual.
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. Crear `DishImageDiagnostics.kt` con `DishImageCall`, `DishImageCallReason` y `DishImageCallEvent`.
2. Añadir `DishImagePolicy.reasonFor(httpStatus, deliveredBytes)` pura, con KDoc del mapeo y del
   residual.
3. Añadir `onEvent`/`now` a `DevExpertDishImageGenerator` y emitir **un evento por llamada** en
   `editImage` y `generateFromText` (tabla §3), sin cambiar el flujo, la decisión de respaldo, los
   timeouts ni el contrato de `null`/cancelación; actualizar su KDoc.
4. Cablear `onEvent` → `Log.d("SazonDishImage", …)` en `MainActivity`.
5. Añadir los tests de la política y del adaptador (incluida la no-fuga) y correr
   `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*"` y `bash init.sh`;
   actualizar las tres docs; implementer registra estado/evidencia en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `DishImageDiagnostics.kt`: enums `DishImageCall`/`DishImageCallReason` + `DishImageCallEvent`
      (públicos; sin campos de clave/`Authorization`/prompt/cuerpo).
- [ ] `DishImagePolicy.reasonFor` pura y total con el mapeo exigido (`CUOTA` 402/429, `AUTH`
      401/403, `SERVER` 5xx, `INVALID` 2xx sin bytes, `OK` 2xx con bytes, `TRANSPORT` sin estado,
      residual `HTTP_ERROR`); KDoc.
- [ ] `DevExpertDishImageGenerator`: `onEvent`/`now` al final con default; helper `report`;
      emisión por llamada en las dos fases según la tabla §3; `detail` determinista; KDoc actualizado
      (nunca clave/cabeceras; ya no "mudo").
- [ ] Sin cambios en el flujo: una petición por camino, respaldo acotado por estado, `null` sin
      excepción, cancelación relanzada; clave en blanco → 0 peticiones y **0** eventos.
- [ ] `MainActivity`: `onEvent` → `Log.d("SazonDishImage", …)` en la construcción del adaptador.
- [ ] `DishImagePolicyTest`: `reasonFor` con los seis motivos exigidos, `2xx`→`OK`/`INVALID`, residual
      `HTTP_ERROR` y caso negativo (los estados no se confunden entre sí).
- [ ] `DevExpertDishImageGeneratorTest`: Scenario 1 (`OK` con bytes), Scenario 2 (402/429/401/403/5xx/
      otros 4xx/transporte), Scenario 3 (INVALID), Scenario 4 (`EDIT` OK, rechazo con respaldo = 2
      eventos, transporte del ajuste = 1 evento), Scenario 5 (no-fuga) y no-regresión del contrato.
- [ ] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*"` y
      `bash init.sh` en verde; 0 errores de lint y sin warnings nuevos.
- [ ] Inspección de alcance: `git diff --stat` solo con lo previsto; `DishImagePolicy.TIMEOUTS`,
      `MAX_ATTEMPTS`, `EDIT_FALLBACK_STATUSES`, `data/image/**`, `domain/**`, `ui/**`, build y
      `init.sh` intactos; sin dependencias nuevas.
- [ ] Docs durables (`technical-discovery.md`, ADR 0005, nota en `dish-image-generator.md`) y
      evidencia/estado en `feature_list.json`/`PROGRESS.md` (implementer/validador).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*"`. **Nunca**
  `./gradlew` directo ni `--stop` (AGENTS.md).
- `DishImagePolicyTest` — Scenario 7: `reasonFor(null, *)` = `TRANSPORT`; `402`/`429` = `CUOTA`;
  `401`/`403` = `AUTH`; `500`/`599` = `SERVER`; `200` con bytes = `OK`; `200` sin bytes = `INVALID`;
  `400`/`404`/`408` = `HTTP_ERROR`. Caso negativo (`402 != AUTH`, `500 != CUOTA`, `200` con/sin bytes
  difieren).
- `DevExpertDishImageGeneratorTest` — Scenario 1: 200 con bytes → `call=GENERATE`, `reason=OK`,
  `status=200`, `bytes>0`. Scenario 2: los estados de cada clasificación y transporte
  (`httpStatus=null`, `bytes=null`), `null` sin lanzar y **una** petición. Scenario 3: 200 ilegible →
  `INVALID`, `bytes=null`. Scenario 4: con referencia, `call=EDIT`; rechazo definitivo con respaldo →
  `EDIT` + `GENERATE` (2 eventos, 2 peticiones, sin tercera); transporte del ajuste → 1 evento
  `EDIT TRANSPORT` sin respaldo. Scenario 5: **ningún** `toString()` de evento contiene `test-key`,
  `Bearer`/`Authorization` ni el `prompt`.
- Regresión: los 22 tests actuales de `com.sazon.app.data.ai.image.*` siguen verdes **sin cambiar
  aserciones** (el `onEvent`/`now` nuevos son defaults); clave en blanco → 0 peticiones y 0 eventos;
  cancelación relanzada; sin reintentos.
- **Lint:** `bash init.sh` → 0 errores y sin warnings nuevos (los 13 preexistentes se mantienen).
- **Inspección:** `git diff --stat` limitado a lo previsto; `DishImagePolicy` sin cambios en
  `TIMEOUTS`/`MAX_ATTEMPTS`/`EDIT_FALLBACK_STATUSES`; `data/image/**`, `domain/**`, `ui/**`,
  `data/ai/AiDiagnostics.kt`, build, `init.sh` y `scripts/**` sin diff; sin dependencias nuevas.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; `MockEngine` cubre el transporte en JVM
  y lo visual/manual lo revisa la persona). `init.sh` **no cambia**.
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):** con
  `devexpert.apiKey` válida, `bash scripts/gradle.sh assembleDebug` y ejecutar; con el interruptor
  encendido, ver en logcat una línea `SazonDishImage` por llamada con llamada, motivo, estado HTTP,
  duración y bytes; con la cuota agotada, `CUOTA status=402|429` (o el estado real del proveedor) en
  lugar del fallo mudo; con caché/semilla no aparece ninguna línea.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.image.*` con recuento de tests y 0
  fallos (baseline del `HEAD` actual: 115 suites / 911 tests; 4 suites / 22 tests en
  `data.ai.image`; sin eliminar ninguno).
- Nombres y resultado de los tests nuevos: mapeo completo de `reasonFor` (seis motivos + residual +
  `2xx`), evento `OK`/`CUOTA`/`AUTH`/`SERVER`/`TRANSPORT`/`INVALID`, `EDIT` + respaldo (2 eventos) y
  no-fuga de clave/`Authorization`/prompt.
- `git diff --stat` de lo tocado y confirmación de que `data/image/**`, `domain/**`, `ui/**`,
  `data/ai/AiDiagnostics.kt`, build, `init.sh` y `scripts/**` no cambian.
- Resultado **manual** del emulador (línea `SazonDishImage` con motivo `CUOTA` y estado HTTP ante
  cuota agotada; una línea por llamada; ninguna línea en aciertos de caché/semilla).
- Docs actualizadas (technical-discovery, ADR 0005 y nota en `dish-image-generator.md`) y anotación
  de estado/evidencia en `feature_list.json` y `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **Tipo de evento propio de la imagen** (`DishImageCallEvent` en `data.ai.image`) en lugar de
  reutilizar `AiCallEvent` del chat: no acopla chat e imagen (bases y ciclos distintos) y mantiene el
  diagnóstico local, de solo lectura y sin telemetría. Campos: llamada, motivo, estado HTTP, duración,
  `detail` y bytes.
- **Una clasificación única con `OK`** (`DishImageCallReason`) porque la conducta observable habla de
  "el motivo clasificado (… OK en 2xx con bytes)". El residual de estados no listados
  (`400/404/408/…`, y cualquier otro) va a `HTTP_ERROR`, para que la función sea **total** y ningún
  estado quede sin motivo.
- **Una función pura `reasonFor(httpStatus, deliveredBytes)` en `DishImagePolicy`**: un único sitio
  de política, testeable en JVM sin red (precedente `editFailureRequiresTextFallback`). El adaptador
  solo compone (transporte → `TRANSPORT`; 2xx → `OK`/`INVALID` según bytes entregados).
- **Un evento por llamada HTTP**, no por `generate`: un rechazo del ajuste con respaldo deja dos
  líneas (`EDIT` y `GENERATE`) y sigue habiendo **dos** peticiones (nunca tres). Encaja con "cada
  llamada de generación/ajuste deja una línea".
- **Clave en blanco ⇒ 0 peticiones y 0 eventos**: no hay llamada que observar. (El chat sí emite
  `MISSING_KEY` porque su operación es una llamada lógica; aquí la feature clasifica **llamadas
  HTTP** y la clave ausente ya degrada en silencio por diseño.)
- **`INVALID` cubre "2xx sin bytes entregables"** (cuerpo ilegible, base64 inválido o bytes que el
  reductor no convierte a imagen): es el motivo correcto para el fallo de decodificación, coherente
  con "cuerpo/codificación ilegible".
- **`TRANSPORT` con `httpStatus` conservado cuando el fallo es al leer el cuerpo de un 2xx**: la
  respuesta se recibió, así que se registra el estado; si el fallo ocurre antes de la respuesta,
  `httpStatus = null`.
- **`detail` determinista** (`"HTTP <status>"` / `"transport"`): mismo estilo que el `SazonAi` del
  chat y comprobable en test; nunca prompt ni cuerpo.
- **Log cableado sin gate de debug**, igual que el `onEvent` del chat (`SazonAi`): no lleva clave,
  cabeceras, prompt ni cuerpo y `Log.d` no se compila fuera de debug en la práctica; el gate de
  `BuildConfig.DEBUG` se reserva para el diagnóstico verboso del chat y el origen de la imagen
  (`dish-image-prompt-v2`), que sí incluyen texto.

### Unknowns

- **Estado real de la cuota agotada de DevExpert**: la feature asume 402/429 → `CUOTA`; si el
  proveedor usara otro estado (p. ej. 403) se leería `AUTH`. Se verá en la prueba manual y es
  revisable en el mismo sitio (`reasonFor`).
- **Detalle de la cuota en el cuerpo** (p. ej. `insufficient_quota`): **no** se parsea (el evento
  nunca lleva cuerpo). Extraerlo sería otra correctiva.
- **3xx/redirects**: con `HTTP_ERROR` de residual; si Ktor sigue redirecciones por defecto, un 3xx
  podría no llegar nunca al adaptador. No verificado en esta sesión.
- **`durationMillis`** es reloj de pared en ms: los tests solo comprueban `>= 0`, no un valor exacto.
- **Granularidad con caché/semilla**: un acierto de caché o de semilla **no** produce línea (no hay
  HTTP); es lo esperado, pero la prueba manual debe confirmarlo para no confundir "sin línea" con
  "diagnóstico roto".

## Validator Checklist

- [ ] La costura `onEvent` es **aditiva** (default no-op) y `now` inyectable; los constructores y los
      tests existentes **no** cambian su firma ni sus aserciones.
- [ ] Cada llamada HTTP (`EDIT` y `GENERATE`) emite **un** evento con estado HTTP y motivo clasificado;
      un rechazo con respaldo deja `EDIT` + `GENERATE` y sigue habiendo **dos** peticiones como máximo.
- [ ] `DishImagePolicy.reasonFor` es **pura y total** y cubre `CUOTA` (402/429), `AUTH` (401/403),
      `SERVER` (5xx), `TRANSPORT` (sin estado), `INVALID` (2xx sin bytes), `OK` (2xx con bytes) y el
      residual `HTTP_ERROR`, con caso negativo.
- [ ] El generador sigue devolviendo **`null` sin lanzar** ante cualquier fallo, la cancelación se
      **relanza**, la clave en blanco hace **0** peticiones y no hay reintentos (`MAX_ATTEMPTS = 1`).
- [ ] Ningún `toString()` de evento contiene la clave, `Authorization`/`Bearer`, el `prompt` ni el
      cuerpo; el evento no se persiste ni se envía a telemetría.
- [ ] Alcance: solo `data.ai.image` (código + sus tests), la línea de log de `MainActivity` y las tres
      docs; `data/image/**`, `domain/**`, `ui/**`, `data/ai/AiDiagnostics.kt`, Room, Taxonomía,
      assets, build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin
      dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos) y regresión de los 22
      tests de `data.ai.image`.
- [ ] `Visual Design Impact` sin capturas (no hay UI) y la verificación **manual** en emulador anotada
      como tal (línea `SazonDishImage` con `CUOTA`/estado ante cuota agotada), no automatizable desde
      WSL.
- [ ] Docs durables actualizadas (technical-discovery, ADR 0005, nota en `dish-image-generator.md`) y
      `feature_list.json`/`PROGRESS.md` actualizados por implementer/validador/orquestador.
