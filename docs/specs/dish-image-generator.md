# Feature Implementation Spec: Generador real de imagen de plato (proveedor de IA)

## Source Feature

- `id`: `dish-image-generator`
- `area`: `integration`
- `depends_on`: `["dish-image-domain"]` (satisfecha: `accepted`, sesión 086)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Implementar el **adaptador real** del puerto ya aceptado `com.sazon.app.domain.image.DishImageGenerator`:
`com.sazon.app.data.ai.image.DevExpertDishImageGenerator`. Es la **fase 2** de
`docs/ai-dish-images-proposal.md` y de `docs/adr/0005-ai-dish-images.md` (fase 1, `dish-image-domain`,
ya `accepted`): transporte real contra el gateway de DevExpert Inference (`POST
/images/generations`, modelo `image`, `size 1024x1024`), decodificación de `data[0].b64_json` y
**reducción local** a WebP q80 de **960 px de ancho**. El adaptador es **transporte + códec puros**:
no conoce `Recipe` ni la Taxonomía (recibe `DishImageRequest` ya resuelto) y **nadie lo consume
todavía** (lo cablean las fases 3-4).

El contrato de fallo es el del ADR 0005: **fallo o latencia = `null`, nunca excepción** (la app
conserva su foto local y no muestra error por imagen); la **cancelación** de la corrutina sí se
propaga. `prompt` y `cacheKey` los sigue decidiendo el dominio (`DishImagePrompt`,
`DishImageCacheKey`): el adaptador es la **única capa nueva que usa Ktor** y no toca la clave de API
más que para enviarla en la cabecera.

## Non-Goals

- **No** hay caché (ni memoria/LRU, ni ficheros en `cacheDir`, ni Room, ni evicción, ni tope de
  disco) ni inserción en la cadena de `recipe-photos`: fase 3 (`dish-image-cache`).
- **No** hay UI, ni cambios en `ui.artwork.RecipeArtwork` (carga progresiva, shimmer, transición),
  ni guardarraíles de sesión (interruptor, tope de generaciones): fase 4
  (`dish-image-progressive-ui`, `area: ui`, con capturas obligatorias).
- **No** se cablea en `MainActivity` ni en ningún consumidor: el adaptador queda **sin consumidores**
  (fase 3 lo monta delante de la caché). No se toca `BuildConfig` ni `local.properties`.
- **No** se usa `/images/edits` ni la referencia de estilo (fase 6); **no** se añade `dishKey` /
  `imagePrompt` al contrato de la IA (fase 5); **no** se empaqueta un set semilla (fase 7).
- **No** se toca `com.sazon.app.domain.image` (prompt, clave, puerto ni `DishImageRequest`), ni
  `Recipe`/`RecipeValidator`/`PromptBuilder`, ni la Taxonomía ni `seed-recipes.json`.
- **No** se añaden dependencias: Ktor Client + OkHttp, `kotlinx.serialization` y Robolectric ya están
  en el build. **No** se toca `app/build.gradle.kts`, `gradle/libs.versions.toml` ni `init.sh`.
- **No** se añade telemetría, `onEvent` ni `Log`: el adaptador **no registra nada** (la clave nunca
  aparece en logs porque no hay logs).
- **No** se persiste ni se modela la imagen en el dominio (`ByteArray?` es todo el contrato).

## Job Story

Cuando la app pida la imagen generada de un plato,
quiero que el adaptador real la pida al proveedor y la devuelva **ya reducida y codificada**,
para que las fases 3 (caché) y 4 (pintado progresivo) solo tengan que almacenar y pintar bytes, sin
volver a decidir transporte, tamaño, formato ni política de fallo.

## Users And Permissions

- **Desarrollador / agente**: puede construir el adaptador y ejercitarlo con un motor HTTP falso, sin
  red, sin clave real y sin gastar cuota.
- **Usuario local**: **no interactúa** — nadie llama al puerto todavía, así que la app no hace ninguna
  petición de imagen ni cambia visualmente. La cuota se empieza a gastar en la fase 4.

## Acceptance Scenarios

### Scenario 1: contrato exacto de la petición (camino feliz)

Given un `DishImageRequest` con `prompt` no vacío y una clave de API ficticia,
When se llama a `generate(request)` y el motor HTTP falso responde `200` con
`{"data":[{"b64_json":"<bytes>"}]}`,
Then se hace **exactamente una** petición `POST https://inference.devexpert.io/v1/images/generations`
con `Authorization: Bearer <clave>`, `Content-Type: application/json` y cuerpo
`{"model":"image","prompt":<request.prompt>,"size":"1024x1024"}`, y `generate` devuelve bytes no nulos.

### Scenario 2: decodificación de `b64_json` y reducción a WebP q80 de 960 px

Given una respuesta `200` cuyo `data[0].b64_json` es la base64 de una imagen real de 1024×1024,
When se llama a `generate` con el reductor Android real,
Then los bytes devueltos son un **WebP válido** (`RIFF`…`WEBP`) que decodifica a **960 px de ancho**
(alto proporcional; 960×960 para una fuente cuadrada) y la calidad de codificación es
`DishImagePolicy.WEBP_QUALITY` (80).

### Scenario 3: nunca se agranda

Given una imagen de origen más estrecha que 960 px (p. ej. 400×300),
When se reduce,
Then la salida sigue siendo WebP con **400 px** de ancho (no se escala hacia arriba) y el alto es
proporcional.

### Scenario 4: un error HTTP devuelve `null` sin lanzar y sin reintentar

Given un motor HTTP falso que responde `400`, `401`, `429` o `500`,
When se llama a `generate`,
Then devuelve `null`, **no** lanza ninguna excepción y se ha hecho **una sola** petición (sin
reintentos: no se gasta cuota dos veces).

### Scenario 5: un timeout o un fallo de transporte devuelve `null` sin lanzar

Given un motor HTTP falso que falla con `java.net.SocketTimeoutException`,
`io.ktor.client.plugins.HttpRequestTimeoutException` o un `IOException` genérico (o una respuesta que
excede el timeout de petición inyectado),
When se llama a `generate`,
Then devuelve `null` sin lanzar y sin propagar el fallo.

### Scenario 6: sin clave no hay petición

Given un adaptador con `apiKey` en blanco,
When se llama a `generate`,
Then devuelve `null` y el motor HTTP falso registra **0** peticiones.

### Scenario 7: un `200` malformado devuelve `null` sin lanzar (caso negativo obligatorio)

Given respuestas `200` con cuerpo no JSON, `{}`, `{"data":[]}`, `{"data":[{}]}`,
`{"data":[{"b64_json":""}]}`, `b64_json` con base64 inválida o un `data` que no es lista,
When se llama a `generate`,
Then **cada** caso devuelve `null` sin lanzar y sin intentar reducir nada.

### Scenario 8: bytes de imagen indecodificables → `null`

Given un `b64_json` que decodifica a bytes que **no** son una imagen,
When se llama a `generate` con el reductor Android real,
Then el reductor devuelve `null`, `generate` devuelve `null` y no lanza.

### Scenario 9: la cancelación se propaga

Given una petición en curso,
When se cancela la corrutina que llama a `generate`,
Then `generate` relanza `CancellationException` (no la convierte en `null`; mismo contrato que
`DevExpertRecipeGenerator`).

### Scenario 10: transporte puro, sin dominio y sin clave en claro

Given cualquiera de los escenarios anteriores,
When se inspecciona la implementación,
Then `com.sazon.app.data.ai.image` **no** importa `Recipe`, `Taxonomy`, `DishImagePrompt` ni
`DishImageCacheKey` (usa `request.prompt` / `request.cacheKey` tal cual), el único import de
`android.graphics` está en `AndroidDishImageReducer.kt`, nadie usa `Log`, y ni el cuerpo de la
petición ni el código contienen la clave fuera de la cabecera `Authorization`.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-image-generator`, líneas 934-948) — metadata, verificación
  exigida y no-goals.
- `docs/ai-dish-images-proposal.md` — §2 (piezas A/C/B), §3 (prueba real: `512` no soportado,
  `1024x1024` OK, ~4,2 s, `b64_json`, ~2 % de cuota/imagen, sin URL), §4 (adaptador en
  `data.ai.image`), §5 (clave), §6 (no hacer esperar, fallo silencioso, política de timeouts propia).
- `docs/adr/0005-ai-dish-images.md` — decisión de fase 1 (puerto keyed por `DishImageRequest`, fallo
  `null`, cancelación propagada, formato decidido por el adaptador, clave versionada) y las fases 2-7
  pendientes (la fase 2 es esta).
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — puerto
  `suspend fun generate(request: DishImageRequest): ByteArray?`; `DishImageRequest(cacheKey, prompt,
  recipeId)` + `of(taxonomy, recipe)`. **No se modifica.**
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — patrón del adaptador real:
  clave en constructor, `HttpClient` inyectado, `Json { ignoreUnknownKeys = true }`, captura de
  `CancellationException` **antes** que `IOException`/`HttpRequestTimeoutException`/`SocketTimeoutException`,
  `expectSuccess = false` y código HTTP inspeccionado a mano.
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — única fuente de `BASE_URL`
  (`https://inference.devexpert.io/v1`) y comparación de timeouts (chat: socket 120 s / request 150 s /
  deadline 240 s).
- `app/src/main/java/com/sazon/app/data/ai/AiHttpClientFactory.kt` — cliente Ktor con
  `ContentNegotiation` + `HttpTimeout` (política de chat) y `expectSuccess = false`; el timeout por
  petición se puede sobrescribir con `timeout { }` (como ya hace `DevExpertRecipeGenerator`).
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — vocabulario de diagnóstico de **chat**
  (`AiCallEvent`, `AiUnavailableReason`): **no** se reutiliza en esta fase (el puerto de imagen es
  `ByteArray?` y no se emiten eventos ni excepciones tipadas).
- `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` — único uso actual de
  `android.graphics.BitmapFactory` (chain local): decodifica con `decodeStream`, devuelve `null` si
  falla y no lanza. Patrón a imitar.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — patrón de test con
  `MockEngine` + `AiHttpClientFactory.create(mock.engine)` + `runTest`, clave ficticia `test-key`,
  inspección del `HttpRequestData` (URL, método, `Authorization`, cuerpo JSON).
- `app/src/test/java/com/sazon/app/data/profile/RoomProfileRepositoryTest.kt` — patrón Robolectric
  (`@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [33])`).
- `app/build.gradle.kts` — Ktor client/okhttp/content-negotiation, `kotlinx-serialization-json`,
  `ktor-client-mock` y `robolectric` ya presentes; las tareas `Test` ya reciben
  `taxonomy.asset.path`/`seed.recipes.asset.path`. **No hace falta tocar el build.**
- `docs/technical-discovery.md` §Integrations (líneas 514-568) — dónde se documenta el puerto
  `domain.image` (línea 530) y que "nadie consume el puerto todavía".
- `docs/risks-and-open-questions.md` línea 144 — riesgo de cuota (~2 %/imagen) y latencia (~4,2 s),
  hoy "Aún no se genera nada: en la fase 1… se materializa en las fases 2-3".
- `docs/domain/taxonomy.md` §6 (líneas 149-166) — estrategia visual vigente; la resolución en runtime
  **no** cambia en esta fase.
- `docs/specs/dish-image-domain.md` — spec aceptada de la fase 1 (estructura y decisiones previas).

### Existing Patterns To Follow

- Adaptador real en `data.ai.<área>`, Ktor aislado en esa capa (regla del ADR 0003), clave y
  `HttpClient` por constructor, sin estado y sin `Log`.
- Política en un **único sitio** (`AiProviderPolicy` → aquí `DishImagePolicy`), con las constantes
  puras y testeables en JVM.
- Degradación silenciosa: `null` como "no disponible", nunca excepción; `CancellationException`
  relanzada primero.
- Separar lógica pura de la capa Android y aislar `android.graphics` en un único fichero
  (`AssetRecipePhotoSource`).
- Tests JUnit 4 en `app/src/test/...` con `MockEngine` (JVM puro) y Robolectric solo cuando hace falta
  Android.
- Verificación canónica `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo.

### Current Gaps

- **No existe `com.sazon.app.data.ai.image`**: hoy no hay adaptador de imagen ni ninguna noción de
  reducción a WebP en el repo.
- **No existe nada que decodifique `b64_json`**: `data.ai` solo sabe de `choices[0].message.content`
  (chat) y `data.RecipePhotoAssets` solo de assets locales.
- **El endpoint de imágenes no está en el código**: `AiProviderPolicy` solo declara
  `/chat/completions`; la comprobación real de `/images/generations` está en la propuesta §3 (prueba
  manual), no en tests.
- **No hay precedente de test de códec en JVM**: el único uso de `BitmapFactory` (assets locales) no
  tiene test unitario (deuda ya anotada en `docs/validations/recipe-photos.md`).
- **Nadie consume el puerto**: sigue siendo cierto después de esta feature (lo cambian las fases 3-4).

## Technical Approach

### 1. Dónde vive

Nuevo paquete `app/src/main/java/com/sazon/app/data/ai/image/` (`com.sazon.app.data.ai.image`), con
cinco ficheros. Es la misma frontera que `data.ai` (única capa que conoce Ktor, ADR 0003) y la
separación "política pura + transporte + códec Android aislado" del resto del repo.

### 2. Política: `DishImagePolicy.kt`

`object` puro (sin HTTP, sin Android), mismo patrón que `AiProviderPolicy`:

```kotlin
object DishImagePolicy {
    const val IMAGES_GENERATIONS_PATH = "/images/generations"
    const val MODEL_ALIAS = "image"            // el alias, no el nombre real del modelo
    const val IMAGE_SIZE = "1024x1024"         // 512 responde 400 en el proveedor (propuesta §3)
    const val TARGET_WIDTH_PX = 960            // ancho de guardado (reducción local)
    const val WEBP_QUALITY = 80

    val TIMEOUTS = DishImageTimeouts(          // política propia, MÁS CORTA que la de chat
        connectMillis = 10_000L,
        socketMillis = 30_000L,
        requestMillis = 40_000L,
    )
    val MAX_ATTEMPTS = 1                       // sin reintentos: no se gasta cuota dos veces

    fun endpointUrl(): String                  // AiProviderPolicy.BASE_URL + IMAGES_GENERATIONS_PATH
    fun scaledSize(width: Int, height: Int): IntArray   // solo reduce, proporcional, mínimo 1 px
}

data class DishImageTimeouts(
    val connectMillis: Long,
    val socketMillis: Long,
    val requestMillis: Long,
)
```

- `endpointUrl()` reutiliza `AiProviderPolicy.BASE_URL` (una sola base para el proveedor) y añade la
  ruta de imágenes.
- `scaledSize` es **pura** y decide la geometría de la reducción: si `width > TARGET_WIDTH_PX`,
  escala a `960` de ancho redondeando el alto proporcionalmente (`min 1 px`); si no, devuelve el
  tamaño original (**nunca** agranda). Testeable sin Android.
- `TIMEOUTS` es un `data class` para poder **inyectar valores diminutos** en el test de timeout, igual
  que `now: () -> Long` en `DevExpertRecipeGenerator`; en producción se usa `DishImagePolicy.TIMEOUTS`.

### 3. DTOs del wire: `OpenAiImages.kt`

`@Serializable`, mismo estilo que `OpenAiChat.kt` (tolerancia con `ignoreUnknownKeys`):

```kotlin
@Serializable data class ImageGenerationRequest(val model: String, val prompt: String, val size: String)
@Serializable data class ImageGenerationDatum(@SerialName("b64_json") val b64Json: String? = null)
@Serializable data class ImageGenerationResponse(val data: List<ImageGenerationDatum> = emptyList())
```

Solo `model`, `prompt` y `size` en la petición (ni `n`, ni `response_format`, ni `stream`).

### 4. Decodificación tolerante: `DishImagePayload.kt`

`object` puro (Kotlin + `java.util.Base64`, disponible en Android con `minSdk 33`):

```kotlin
object DishImagePayload {
    /** Primer `data[i].b64_json` no vacío decodificado; `null` si no hay nada utilizable. Nunca lanza. */
    fun decode(rawBody: String?): ByteArray?
}
```

- `rawBody` nulo/blank, JSON inválido, `data` ausente/vacía/no-lista, `b64_json` nulo o en blanco y
  base64 inválida → `null`, sin excepción.
- Ignora `url`, `revised_prompt` y cualquier clave desconocida.

### 5. Códec: `DishImageReducer.kt` + `AndroidDishImageReducer.kt`

Costura inyectable (para que el transporte se pruebe en **JVM puro**, sin Robolectric) y su
implementación Android aislada en un único fichero:

```kotlin
/** Reduce los bytes crudos del proveedor a la imagen que se cacheará; `null` si no se puede. */
fun interface DishImageReducer {
    fun reduceToWebp(rawBytes: ByteArray): ByteArray?
}

/** Único fichero de `data.ai.image` que importa `android.graphics`. */
object AndroidDishImageReducer : DishImageReducer {
    override fun reduceToWebp(rawBytes: ByteArray): ByteArray? {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null
        val (w, h) = DishImagePolicy.scaledSize(decoded.width, decoded.height)
        val scaled = if (w != decoded.width || h != decoded.height)
            Bitmap.createScaledBitmap(decoded, w, h, /* filter = */ true) else decoded
        // compress(WEBP_LOSSY, WEBP_QUALITY) a un ByteArrayOutputStream; reciclar bitmaps; null si falla
    }
}
```

- `Bitmap.CompressFormat.WEBP_LOSSY` existe desde API 30 y `minSdk` es 33.
- Se re-codifica **siempre** a WebP (aunque el origen ya mida ≤ 960), para fijar formato y tamaño.
- No lanza: cualquier fallo de decodificación/compresión devuelve `null`.

### 6. Adaptador: `DevExpertDishImageGenerator.kt`

```kotlin
class DevExpertDishImageGenerator(
    private val apiKey: String,
    private val client: HttpClient,
    private val reducer: DishImageReducer = AndroidDishImageReducer,
    private val model: String = DishImagePolicy.MODEL_ALIAS,
    private val timeouts: DishImageTimeouts = DishImagePolicy.TIMEOUTS,
) : DishImageGenerator {

    override suspend fun generate(request: DishImageRequest): ByteArray? { ... }
}
```

Flujo de `generate`:

1. `apiKey.isBlank()` → `return null` (**sin** petición HTTP).
2. `POST DishImagePolicy.endpointUrl()` con `ContentType.Application.Json`,
   `Authorization: Bearer $apiKey`, `setBody(ImageGenerationRequest(model, request.prompt,
   DishImagePolicy.IMAGE_SIZE))` y `timeout { connectTimeoutMillis / socketTimeoutMillis /
   requestTimeoutMillis = timeouts }` (sobrescribe la política de chat del cliente compartido).
3. Capturas en este orden: `CancellationException` → **relanzar**; `HttpRequestTimeoutException`,
   `SocketTimeoutException`, `IOException` y cualquier otro `Exception` → `return null`.
4. Estado HTTP fuera de `200..299` → `return null` (no se lee el cuerpo, no hay reintento).
5. `response.bodyAsText()` (mismo tratamiento de excepciones) → `DishImagePayload.decode(raw)`; si es
   `null` → `null`.
6. `reducer.reduceToWebp(rawBytes)` y se devuelve tal cual (puede ser `null`).
7. `request.cacheKey` **no** se usa en la petición de red: es la identidad de caché de la fase 3 y el
   adaptador no la reinterpreta (el KDoc lo deja explícito).

Sin `Log`, sin telemetría y sin excepciones tipadas: el contrato del puerto es `ByteArray?`.

### 7. Frontera con las fases posteriores (no implementar aquí)

La fase 3 envuelve este adaptador con la caché (clave = `DishImageCacheKey`, formato ya WebP) y la
fase 4 lo inserta delante de la cadena de `recipe-photos` (que pasa a respaldo). El tamaño objetivo
(960) y el formato WebP se fijan **aquí** (ADR 0005: los decide el adaptador) y la fase 3 los
reutiliza tal cual.

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — crear; endpoint, alias `image`,
  `1024x1024`, `TARGET_WIDTH_PX`, `WEBP_QUALITY`, `TIMEOUTS`, `MAX_ATTEMPTS` y `scaledSize` pura.
- `app/src/main/java/com/sazon/app/data/ai/image/OpenAiImages.kt` — crear; DTOs `@Serializable` de la
  petición y la respuesta de `/images/generations`.
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePayload.kt` — crear; decodificación
  tolerante de `data[0].b64_json` a bytes.
- `app/src/main/java/com/sazon/app/data/ai/image/DishImageReducer.kt` — crear; la costura
  `fun interface DishImageReducer`.
- `app/src/main/java/com/sazon/app/data/ai/image/AndroidDishImageReducer.kt` — crear; decodificación,
  reducción a 960 y compresión WebP q80 (único fichero con `android.graphics`).
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — crear; adaptador
  Ktor del puerto `DishImageGenerator` (única capa nueva que usa Ktor).
- `app/src/test/java/com/sazon/app/data/ai/image/DishImagePolicyTest.kt` — crear; escenario 3
  (`scaledSize`), `endpointUrl`, alias/tamaño/calidad y timeouts más cortos que los de chat.
- `app/src/test/java/com/sazon/app/data/ai/image/DishImagePayloadTest.kt` — crear; escenario 7.
- `app/src/test/java/com/sazon/app/data/ai/image/DevExpertDishImageGeneratorTest.kt` — crear;
  escenarios 1, 4, 5, 6, 9 y 10 con `MockEngine` y un `DishImageReducer` falso (JVM puro).
- `app/src/test/java/com/sazon/app/data/ai/image/AndroidDishImageReducerTest.kt` — crear; escenarios 2,
  3 y 8 (Robolectric `@GraphicsMode(NATIVE)`, `@Config(sdk = [33])`), más el camino
  `b64_json → WebP 960` extremo a extremo con `MockEngine` y el reductor real.
- `docs/technical-discovery.md` — modificar; registrar el adaptador `data.ai.image` (política propia,
  un intento, reducción local WebP 960 q80, `null` como fallo) y que sigue **sin caché, sin UI y sin
  consumidores**, con la cadena de `recipe-photos` como única resolución en runtime.
- `docs/risks-and-open-questions.md` — modificar; la fila de cuota/latencia pasa de "aún no se genera
  nada" a "el transporte existe desde la fase 2; se materializa al consumirse en las fases 3-4".
- `docs/adr/0005-ai-dish-images.md` — modificar; estado a "Aceptado parcialmente (fases 1-2)" y nota
  del adaptador (fuera de `domain.image`; `null` como fallo; sin reintento; WebP 960 q80).
- `PROGRESS.md` y `feature_list.json` — modificar al implementar/validar (estado, evidencia); **no**
  los toca el planificador.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`app/src/androidTest/**`, `app/src/main/assets/**`, `domain/image/**`, el resto de `domain/**`,
`data/**` (salvo los ficheros nuevos), `ui/**`, `MainActivity`/`SazonApp`/`MainShell`, la Taxonomía,
`seed-recipes.json`, `DESIGN.md`, `docs/design/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: integration`; no hay pantalla, ni componente Compose, ni estado visual).
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.
- **El gate de capturas de UI no aplica**: **no** se crea
  `docs/design/concepts/dish-image-generator/` ni ninguna otra carpeta en `docs/design/concepts/`
  (las capturas son obligatorias solo en la fase 4, `dish-image-progressive-ui`).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — nueva frontera durable: el puerto `DishImageGenerator`
  pasa a tener adaptador real en `data.ai.image` (Ktor sigue aislado; política de imagen propia, un
  solo intento, reducción local a WebP 960 q80) y se deja explícito que sigue **sin caché, sin UI y
  sin consumidores**, con `recipe-photos` como única resolución en runtime.
- `docs/risks-and-open-questions.md`: **update** — el riesgo de cuota (~2 %/imagen) y latencia (~4,2 s)
  deja de ser hipotético para el transporte y su mitigación se consuma en las fases 3-4.
- `docs/adr/0005-ai-dish-images.md`: **update** — estado "Aceptado parcialmente (fases 1-2)": se
  registra que la fase 2 es transporte + códec (adaptador fuera del dominio, `null` como fallo, sin
  reintento, WebP 960 q80) y que las fases 3-7 siguen pendientes.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; la imagen no
  es un campo de `Recipe` (descartado en ADR 0004) y nada se persiste todavía.
- `docs/domain/taxonomy.md` §6: **not needed** en esta fase — la resolución de imagen en runtime **no**
  cambia (nadie consume el puerto); se actualizará en la fase 4, cuando la cadena gane su primer nivel.
- `AGENTS.md`: **not needed** — no cambian el flujo, el arranque ni las reglas de trabajo.
- `DESIGN.md`: **not needed** — sin UI, sin microcopy nuevo; `STYLE_SUFFIX` es texto de prompt (fase 1).
- `docs/ai-dish-images-proposal.md`: **not needed** — documento de propuesta; `feature_list.json` y el
  ADR son la fuente de verdad del estado.

## Implementation Plan

1. Crear `DishImagePolicy` con endpoint, constantes, `TIMEOUTS` y `scaledSize`; hacer pasar
   `DishImagePolicyTest` (geometría de reducción y coherencia de timeouts, JVM).
2. Crear los DTOs (`OpenAiImages`) y `DishImagePayload` con `DishImagePayloadTest` (tolerancia al
   `200` malformado, JVM).
3. Crear la costura `DishImageReducer` y el adaptador `DevExpertDishImageGenerator` (transporte,
   política de fallo, cancelación) y `DevExpertDishImageGeneratorTest` con `MockEngine` y un reductor
   falso (JVM puro).
4. Crear `AndroidDishImageReducer` (BitmapFactory → `createScaledBitmap` → WebP q80) y
   `AndroidDishImageReducerTest` con Robolectric `@GraphicsMode(NATIVE)`, incluido el camino extremo a
   extremo `b64_json → WebP 960` con `MockEngine`.
5. Inspeccionar pureza y alcance: sin `Recipe`/`Taxonomy` en `data.ai.image`, `android.graphics` solo
   en `AndroidDishImageReducer.kt`, sin `Log`, sin cambios en build/lockfile/dominio/UI/`init.sh`.
6. Actualizar `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y el ADR 0005.
7. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `DishImagePolicy` (`IMAGES_GENERATIONS_PATH`, `MODEL_ALIAS = "image"`,
      `IMAGE_SIZE = "1024x1024"`, `TARGET_WIDTH_PX = 960`, `WEBP_QUALITY = 80`, `TIMEOUTS`,
      `MAX_ATTEMPTS = 1`, `endpointUrl()` reutilizando `AiProviderPolicy.BASE_URL`, `scaledSize`).
- [ ] Crear `OpenAiImages.kt` con `ImageGenerationRequest` (`model`, `prompt`, `size`),
      `ImageGenerationDatum` (`@SerialName("b64_json")`) e `ImageGenerationResponse` (`data`).
- [ ] Crear `DishImagePayload.decode(rawBody: String?): ByteArray?` tolerante y sin lanzar
      (`java.util.Base64`).
- [ ] Crear `fun interface DishImageReducer` y `AndroidDishImageReducer` (decodificar con
      `BitmapFactory.decodeByteArray`, reducir con `scaledSize` sin agrandar, `compress(WEBP_LOSSY,
      WEBP_QUALITY)`, reciclar bitmaps, `null` si falla).
- [ ] Crear `DevExpertDishImageGenerator` con la clave y el `HttpClient` por constructor, el timeout
      por petición desde `TIMEOUTS` y la política: clave en blanco → `null` sin petición; no-2xx →
      `null`; transporte/timeout → `null`; `CancellationException` relanzada; sin reintentos; sin `Log`.
- [ ] Escribir `DishImagePolicyTest`, `DishImagePayloadTest` y `DevExpertDishImageGeneratorTest`
      (JVM puro, `MockEngine`, clave ficticia `test-key`).
- [ ] Escribir `AndroidDishImageReducerTest` con `@RunWith(RobolectricTestRunner::class)`,
      `@Config(sdk = [33])` y `@GraphicsMode(GraphicsMode.Mode.NATIVE)`: WebP de 960 px, no agrandar,
      bytes inválidos → `null` y el camino extremo a extremo con `MockEngine`.
- [ ] Verificar por inspección que `data.ai.image` no importa dominio (`Recipe`/`Taxonomy`) ni usa
      `Log`, y que `android.graphics` solo aparece en `AndroidDishImageReducer.kt`.
- [ ] Actualizar `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y el ADR 0005.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde y registrar la evidencia.
- [ ] Actualizar `feature_list.json` (a `passing` con evidencia) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red y **no**
  modifica `init.sh`.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*"` →
  `BUILD SUCCESSFUL`.
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline en HEAD: **68 suites / 496 tests** (última validación independiente,
  sesión 086); el implementer **re-deriva** el número exacto en su HEAD y confirma el delta (los
  tests nuevos de `data.ai.image`).
- `bash scripts/gradle.sh lint` → **0 errores** (los 13 warnings de `GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion` y `OldTargetApi` son preexistentes, todos en el
  build/version catalog, **ninguno** bajo `app/src`).
- Inspección de pureza y alcance:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/data/ai/image` → solo
    `AndroidDishImageReducer.kt`.
  - `grep -rn "Recipe\|Taxonomy\|DishImagePrompt\|DishImageCacheKey" app/src/main/java/com/sazon/app/data/ai/image`
    → 0 (el adaptador consume `request.prompt`/`request.cacheKey`, no los deriva).
  - `grep -rn "Log\." app/src/main/java/com/sazon/app/data/ai/image` → 0.
  - `git diff --name-only` solo con los 6 ficheros de `main`, los 4 de test y los 3 docs previstos
    (+ `feature_list.json`/`PROGRESS.md`); build/lockfile/`init.sh`/`scripts/**`/`domain/**`/`ui/**`/
    assets/`DESIGN.md`/`docs/design/**` intactos y `docs/design/concepts/` sin entradas nuevas.
- Cobertura E2E: **no aplica**. No hay suite E2E persistente, la feature no cambia comportamiento
  observable (nadie llama al puerto; sin UI ni persistencia) y la verificación canónica del repo para
  esta capa es el test JVM con `MockEngine` (más Robolectric para el códec), según `AGENTS.md`.
- **Prueba real opcional** (verificación exigida #3, **no** es puerta de aceptación): una generación
  `1024x1024` contra el proveedor con la clave de `local.properties` demuestra que decodifica y se
  reduce de verdad. **Gasta ~2 % de la cuota semanal**; se hace **una sola vez**, fuera del gate, nunca
  desde un test y nunca con la clave en el árbol versionado; si se hace, se adjunta la imagen
  resultante (p. ej. en `docs/validations/dish-image-generator.md`, o con el script de la propuesta §3)
  y se anota la latencia observada. Si no se hace, la feature **puede** aceptarse: la verificación #1 y
  #2 sí son obligatorias.
- Nota operativa para quien implemente: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`;
  **nunca** `./gradlew` directo ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de la tirada focal `--tests "com.sazon.app.data.ai.image.*"`.
- Conteo parseado del XML del gate (suites/tests, 0 fallos/errores/saltados) con los tests nuevos
  nombrados (`DishImagePolicyTest`, `DishImagePayloadTest`, `DevExpertDishImageGeneratorTest`,
  `AndroidDishImageReducerTest`) y la baseline re-derivada.
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Resultado de los greps de inspección (dominio, `android.graphics`, `Log`, ficheros del alcance).
- Vectores concretos comprobados en los tests: URL/método/cabecera/cuerpo exactos, `data[0].b64_json`
  decodificado, WebP (`RIFF`…`WEBP`) de 960 px de ancho, 400 px sin agrandar, `null` para 400/401/429/
  500, para `SocketTimeoutException`/`HttpRequestTimeoutException` y para clave en blanco (0
  peticiones), y `CancellationException` relanzada.
- Anotación en `feature_list.json` (evidencia + `passing`) y en `PROGRESS.md`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementador no las reabre):

1. **Fallos → `null`, sin excepción y sin tipo de error nuevo.** Es el contrato del ADR 0005 y del
   puerto (`ByteArray?`); ningún consumidor necesita distinguir "falló" de "no se intentó" (la fase 4
   degrada en silencio). La cancelación **sí** se propaga.
2. **Un solo intento (`MAX_ATTEMPTS = 1`), sin backoff ni `Retry-After`.** La propuesta §6.6 pide una
   política propia para imagen "sin reintento agresivo"; con ~2 % de cuota por imagen, reintentar
   duplica el gasto sin cambiar la experiencia (la foto local ya se pinta). Se separa de
   `AiProviderPolicy` (que sí reintenta) en vez de reutilizarla.
3. **Timeouts propios más cortos que los de chat** (connect 10 s / socket 30 s / request 40 s, frente a
   10/120/150 s): la imagen mide ~4,2 s y no comparte la cola de razonamiento del chat. Se fijan con
   `timeout { }` **por petición** sobre el `HttpClient` compartido (el de la fábrica está calibrado
   para chat), y son inyectables (`DishImageTimeouts`) para poder testear el timeout con valores
   diminutos.
4. **La reducción vive en el adaptador y se aísla tras `DishImageReducer`.** El ADR 0005 deja el
   formato al adaptador; la costura permite probar el transporte en JVM puro (`MockEngine` + reductor
   falso) y aislar `android.graphics` en un único fichero, sin repetir la deuda de
   `AssetRecipePhotoSource` (sin test unitario).
5. **Se re-codifica siempre a WebP q80 a 960 px de ancho, sin agrandar.** Generar a 1024 es una
   restricción del proveedor (512 responde 400); reducir en local baja ~870 KB a decenas de KB y fija
   el formato que la fase 3 guardará tal cual.
6. **El test del códec usa Robolectric en modo gráfico nativo** (`@GraphicsMode(NATIVE)` +
   `@Config(sdk = [33])`, Linux/WSL soportado desde Robolectric 4.12): es la única forma de ejercitar
   `BitmapFactory`/`compress(WEBP_LOSSY)` de verdad sin añadir dependencias ni emulador. Las imágenes
   de prueba se **generan dentro del test** (bitmap + `compress`), así que no se versionan binarios ni
   assets nuevos. Si el runtime nativo no estuviera disponible en el entorno, es un **bloqueo a
   reportar**, no una excusa para debilitar la verificación.
7. **Sin diagnostics ni `Log` en esta fase.** El puerto no tiene tipo de evento, no hay consumidor y la
   clave no debe aparecer en logs: la opción más simple y segura es no registrar nada (la fase 4, si lo
   necesita, añade su propia costura).
8. **Sin consumidor ni cableado.** Nadie llama al puerto (lo hacen las fases 3-4) y no se toca
   `MainActivity`/`BuildConfig`; la clave llega por constructor, como en `DevExpertRecipeGenerator`, y
   su origen (`BuildConfig.DEVEXPERT_API_KEY` ← `local.properties`) ya existe y no se modifica.

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **Runtime gráfico nativo de Robolectric**: requiere los artefactos nativos (se resuelven como
  dependencias transitivas de Robolectric ya presente); la primera ejecución puede necesitar descarga.
  Si el modo `NATIVE` no arranca en WSL, hay que reportarlo como bloqueo (decisión 6).
- **`Bitmap.createScaledBitmap` con `filter = true` altera los píxeles** (reescalado con interpolación):
  el test comprueba **geometría y formato**, no coincidencia pixel a pixel (igual que la propuesta §8
  asume para "imagen distinta para el mismo prompt").
- **`WEBP_LOSSY` y `quality`**: la calidad (80) no es verificable de forma fiable por el test; se
  comprueba el valor de la constante y que la salida es WebP del ancho esperado.
- **El proveedor podría cambiar el tamaño mínimo o el formato de respuesta** (`b64_json` vs `url`): la
  degradación es `null` (se conserva la foto local) y el aislamiento en `data.ai.image` permite
  corregirlo con una feature correctiva.
- **La clave se lee solo por parámetro**: si el alias `image` cambiara de nombre, es una constante de
  `DishImagePolicy` (un único sitio).
- **El test de timeout real con valores diminutos depende del soporte de timeout por petición** en el
  cliente compartido; el caso determinista (el motor lanza `SocketTimeoutException`) siempre está
  cubierto.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: **sin** caché, persistencia, UI, consumidores,
      set semilla, `/images/edits`, campos nuevos en el wire, dependencias nuevas ni cambios en
      `Recipe`/`RecipeValidator`/`PromptBuilder`/`domain.image`/build/`init.sh`.
- [ ] Los 10 escenarios pasan, incluidos los negativos obligatorios: clave en blanco (0 peticiones),
      `200` malformado (escenario 7), bytes no imagen (escenario 8) y cancelación propagada.
- [ ] El adaptador devuelve `null` y **no lanza** ante error HTTP, timeout, fallo de transporte, cuerpo
      ilegible, `b64_json` ausente/inválido o imagen indecodificable; **cancela** relanzando
      `CancellationException`.
- [ ] Se hace **una sola** petición por `generate` (sin reintentos) y la petición es exactamente
      `POST https://inference.devexpert.io/v1/images/generations` con `Bearer`, `model = "image"`,
      `prompt` = `request.prompt` y `size = "1024x1024"`.
- [ ] La salida es **WebP** y como máximo de **960 px de ancho**, sin agrandar orígenes menores; la
      geometría se decide en `DishImagePolicy.scaledSize` (pura y testeada).
- [ ] `data.ai.image` **no** conoce `Recipe`/`Taxonomy`/`DishImagePrompt`/`DishImageCacheKey`; el único
      import de `android.graphics` es `AndroidDishImageReducer.kt`; **no** hay `Log` ni telemetría.
- [ ] La clave de API solo entra por constructor y viaja en la cabecera `Authorization`; **no** aparece
      en el cuerpo, en mensajes de error, en logs ni en el árbol versionado (tests con `test-key`).
- [ ] `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y el ADR 0005 están
      actualizados y coherentes (fase 2 entregada, fases 3-7 pendientes).
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica a esta feature).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR.
