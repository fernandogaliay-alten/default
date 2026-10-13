# Feature Implementation Spec: Estilo homogéneo con referencia (image-edit)

## Source Feature

- `id`: `dish-style-reference`
- `area`: `integration`
- `depends_on`: `["dish-image-generator"]` (satisfecha: `accepted`, sesión 090)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1091-1104) + `docs/ai-dish-images-proposal.md` §4.2
  (variante **C**) + `docs/adr/0005-ai-dish-images.md` (fase 6 pendiente)
- Verificación exigida (textual de la feature):
  1. `./gradlew test` cubre el adaptador `/images/edits` con motor falso: envía la imagen de
     referencia, decodifica la respuesta y degrada a `/images/generations` si el ajuste falla.
  2. La referencia de estilo es un asset versionado, no una imagen subida por el usuario.
  3. Prueba real opcional y guardada (~2 % de cuota) comparando el resultado con la variante A.

## Goal

Añadir la **variante C** de la propuesta sobre el adaptador ya aceptado de la fase 2
(`data.ai.image.DevExpertDishImageGenerator`): cuando haya una **referencia de estilo**, la imagen se
pide con `POST /images/edits` (multipart, modelo `image-edit`) usando esa referencia como ancla visual,
y si el ajuste **es rechazado** el adaptador **degrada a `POST /images/generations`** (el camino
texto→imagen de la fase 2) sin romper nada. La referencia es un **asset ya empaquetado y versionado**
(`img/recipes/default.<ext>`, la foto neutra aceptada de `recipe-photos`), nunca una imagen subida por
el usuario: la app no pide permisos, no abre galería ni cámara y no persiste nada nuevo.

El contrato del puerto, el prompt, la clave canónica, la caché, el ajuste de sesión y la UI **no
cambian**: la fase 6 es una capa de **transporte** (qué endpoint y con qué cuerpo) dentro del mismo
adaptador, con la misma degradación silenciosa (`null` = no disponible, la cancelación se propaga) y
la misma política de un solo intento por petición al proveedor.

## Non-Goals

- **No** se cambia el prompt ni la clave: `domain.image` (`DishImagePrompt`, `DishImageCacheKey`,
  `DishImageRequest`, `DishImageGenerator`, `DishVisualDescriptor`) queda **intacto** y
  `DishImageCacheKey.VERSION` **no** sube (ver Decisión 3).
- **No** se añade ningún campo a `DishImageRequest` (la referencia **no** viaja por la petición de
  dominio: es un ancla de la casa, no un dato por plato; ver Decisión 1).
- **No** hay UI, microcopy, pantalla, estado visual ni capturas: `area: integration`, sin
  comportamiento de pantalla nuevo. **No** se crea `docs/design/concepts/dish-style-reference/`.
- **No** se toca `data.image` (resolutor, cachés, `DishImageSettings`, `DishImageGenerationBudget`,
  `GatedDishImageGenerator`) ni el tope por sesión ni el interruptor de usuario: la fase 6 vive
  **por debajo** de ellos (el presupuesto sigue contando una unidad por petición; ver Decisión 6).
- **No** se toca `ui/**`, `RecipeArtwork`, `MainShell`, `SazonApp` ni ningún punto de uso: el crossfade
  y la precedencia de la fase 4 no cambian.
- **No** se añade, modifica ni convierte **ningún asset**: la referencia es el asset ya existente y
  versionado `img/recipes/default.<ext>`; **no** hay subida de usuario, ni `mask`, ni `n`, ni
  `response_format`, ni `stream`, ni visión/autochequeo (propuesta §7.4).
- **No** se implementan las fases 7 (`dish-image-seed`) ni `dish-visual-descriptor-wiring` (el
  descriptor sigue sin entrega en runtime).
- **No** hay dependencias nuevas: el multipart vive en `ktor-client-core` (ya presente).
  **No** se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
  `AndroidManifest.xml`, la Taxonomía ni `seed-recipes.json`.
- **No** se añade telemetría, `onEvent` ni `Log`: el adaptador sigue **sin registrar nada**.

## Job Story

Cuando la app genera la imagen de un plato,
quiero que la foto se ajuste contra una **referencia de estilo de la casa** (el asset neutro aceptado)
en lugar de depender solo de la coletilla de texto del prompt,
para que todas las imágenes generadas compartan dirección visual sin subir nada mío — y si el ajuste
no está disponible o es rechazado, que se genere por texto como hasta ahora.

## Users And Permissions

- **Desarrollador / agente**: puede construir y probar el camino `/images/edits` con un motor HTTP
  falso, sin red, sin clave real y sin gastar cuota.
- **Usuario local**: **no interactúa** (ninguna pantalla ni microcopy cambia) y **no aporta ni sube
  ninguna imagen**: la referencia viaja en el APK. Su único efecto es que las imágenes nuevas se pidan
  con el ajuste y, si el proveedor lo rechaza, se pidan por texto (indistinguible en UI).

## Acceptance Scenarios

### Scenario 1: sin referencia, el contrato de la fase 2 no cambia

Given un adaptador sin `styleReference`, o con la referencia deshabilitada
(`DishImagePolicy.STYLE_REFERENCE_ENABLED = false`) o con bytes vacíos,
When se llama a `generate(request)`,
Then se hace **exactamente una** petición `POST https://inference.devexpert.io/v1/images/generations`
con el cuerpo de la fase 2 (`model=image`, `prompt=request.prompt`, `size=1024x1024`) y el resultado es
el de la fase 2 (bytes reducidos, o `null` con el mismo contrato de fallo).

### Scenario 2: con referencia, el ajuste es una sola petición multipart

Given un adaptador con una referencia de estilo (bytes conocidos, `mediaType` y `fileName`) y un motor
falso que responde `200 {"data":[{"b64_json":"<bytes>"}]}`,
When se llama a `generate(request)`,
Then se hace **exactamente una** petición `POST https://inference.devexpert.io/v1/images/edits` con
`Authorization: Bearer <clave>`, `Content-Type: multipart/form-data` (con boundary) y un cuerpo que
contiene **solo** las partes `model` = `image-edit`, `prompt` = `request.prompt` (idéntico, sin
reinterpretar), `size` = `1024x1024` y `image` (la parte de fichero, con el `filename` y el
`Content-Type` de la referencia y sus **bytes byte a byte**); la respuesta se decodifica con
`DishImagePayload` y se reduce con el `DishImageReducer` (WebP 960 q80 en producción) y `generate`
devuelve esos bytes. Ni `cacheKey` ni `recipeId` viajan en la petición.

### Scenario 3: un rechazo definitivo del ajuste degrada a texto→imagen

Given una referencia y un motor que responde `400`, `404`, `405`, `413`, `415` o `422` a
`/images/edits` y `200` a `/images/generations`,
When se llama a `generate`,
Then se hacen **exactamente dos** peticiones (primero edits, después generations con el cuerpo de la
fase 2 y el mismo `prompt`) y se devuelven los bytes de la segunda.

### Scenario 4: un `200` del ajuste con carga inutilizable también degrada

Given una referencia y un motor que responde `200` a `/images/edits` con cuerpo no JSON, `{}`,
`{"data":[]}`, `{"data":[{"b64_json":""}]}` o base64 inválida — o cuyo `b64_json` decodifica a bytes
que el reductor no puede decodificar (`null`), pero responde `200` con una imagen válida a
`/images/generations`,
When se llama a `generate`,
Then hay dos peticiones (edits + generations) y se devuelven los bytes del respaldo; si el respaldo
también falla, `null` sin lanzar.

### Scenario 5: un fallo de transporte o timeout del ajuste devuelve `null` sin respaldo

Given una referencia y un motor cuya petición a `/images/edits` falla con `SocketTimeoutException`,
`io.ktor.client.plugins.HttpRequestTimeoutException`, `IOException` genérico (o excede el timeout de
petición inyectado),
When se llama a `generate`,
Then devuelve `null` sin lanzar y se ha hecho **una sola** petición: el transporte está roto, así que
no se gasta una segunda llamada (misma disciplina que `MAX_ATTEMPTS = 1`).

### Scenario 6: los estados que no son rechazo del ajuste devuelven `null` sin respaldo

Given una referencia y un motor que responde `401`, `403`, `429` o `500` a `/images/edits`,
When se llama a `generate`,
Then devuelve `null` con **una sola** petición y sin respaldo (clave, cuota o servidor: el respaldo
fallaría igual y costaría otra llamada).

### Scenario 7: el respaldo nunca encadena ni reintenta

Given una referencia, `404` en `/images/edits` y `500` en `/images/generations`,
When se llama a `generate`,
Then hay exactamente **dos** peticiones, devuelve `null` y **no** hay una tercera petición ni un
segundo intento contra `edits` (un solo intento por petición al proveedor, `MAX_ATTEMPTS = 1`).

### Scenario 8: sin clave no hay ninguna petición (tampoco al ajuste)

Given un adaptador con `apiKey` en blanco **y** una referencia de estilo,
When se llama a `generate`,
Then devuelve `null` y el motor falso registra **0** peticiones.

### Scenario 9: la cancelación se propaga en ambas fases

Given una petición en curso contra `/images/edits` (o contra el respaldo `/images/generations`),
When se cancela la corrutina que llama a `generate`,
Then `generate` relanza `CancellationException` (nunca la convierte en `null` ni intenta el respaldo).

### Scenario 10: la referencia es un asset versionado, nunca una subida del usuario

Given la resolución pura de la referencia (`DishImageStyleReferencePolicy`) con un predicado de
existencia falso/verdadero y el cargador Android real (`AssetDishImageStyleReference`),
When se carga la referencia,
Then se resuelve el asset **empaquetado y versionado** `img/recipes/default.<ext>` probando las
extensiones de `RecipePhotoAssets.EXTENSIONS` en orden (nunca una ruta inventada), devuelve los bytes
del asset con su `mediaType` (`image/jpeg` / `image/webp` / `image/png`) y un `fileName` derivado de la
extensión; si ningún asset existe, la lectura falla o los bytes son vacíos, devuelve **`null`** sin
lanzar (y el adaptador degrada al camino de texto). No hay permiso nuevo, ni galería, ni cámara, ni
`ActivityResult`, ni ruta fuera de `assets/`, ni persistencia de la referencia.

### Scenario 11: la política del ajuste vive en un único sitio y es pura

Given `DishImagePolicy`,
When se inspecciona y se testea en JVM,
Then `editsUrl()` es `https://inference.devexpert.io/v1/images/edits`, `EDIT_MODEL_ALIAS` es
`image-edit`, `STYLE_REFERENCE_ENABLED` es el único interruptor del anclaje,
`editFailureRequiresTextFallback(status)` es `true` **solo** para `400/404/405/413/415/422` (y `false`
para `401/403/408/429`, `5xx`, `2xx` y códigos arbitrarios) y **no** hay ninguna constante nueva de
endpoint, modelo, tamaño o fallback fuera de `DishImagePolicy` (sin HTTP y sin Android).

### Scenario 12: el prompt, la clave y la política de imagen de la fase 1/5 no cambian

Given la composición aceptada de la fase 1 y de la fase 5,
When se construyen `DishImagePrompt.build(...)`, `DishImageCacheKey.of(...)` y
`DishImageRequest.of(...)`,
Then los resultados son **idénticos** a los de hoy, `DishImageCacheKey.VERSION` sigue siendo
`dish-image-v1`, el prompt que viaja a `/images/edits` es exactamente `request.prompt` (con su
`STYLE_SUFFIX`) y el formato guardado sigue siendo el WebP ≤960 px del `DishImageReducer` compartido.

### Scenario 13: el cableado degrada en silencio

Given `MainActivity` (que construye el adaptador y ahora le pasa la referencia cargada del asset),
When se inspecciona,
Then el adaptador recibe `styleReference` desde `AssetDishImageStyleReference`; con `null` (asset
ausente o ilegible) la app queda **exactamente** en el comportamiento de la fase 2 (una sola petición
a `/images/generations` por imagen) y no hay ningún estado de error, ni UI, ni cambio en el ajuste de
sesión (`DishImageSettings`) ni en el tope.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-style-reference`, líneas 1091-1104) — metadata, verificación y nota
  de fase 6/variante C.
- `docs/ai-dish-images-proposal.md` §2 (pieza C), §3 (prueba real: `512` no soportado, `b64_json`, ~2 %
  de cuota), §4 (adaptador en `data.ai.image`), §4.2 (la variante C puede sustituir parte del anclaje
  de texto por `/images/edits` con una foto aceptada, "p. ej. `default.jpg`"), §6 (fallo silencioso,
  timeouts propios, sin reintento agresivo) y §10 (fase 6).
- `docs/adr/0005-ai-dish-images.md` — decisiones 1 (request aditivo con "foto de referencia" como
  ampliación **prevista**), 2 (fallo `null`), 3 (estilo en un único sitio), 4 (clave versionada) y
  "Pendiente: fases 6-7".
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — adaptador actual
  (clave y `HttpClient` por constructor, `timeout { }` por petición, `CancellationException` relanzada
  primero, `expectSuccess = false`, sin `Log`); es el fichero que gana el camino de edits + respaldo.
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — endpoint, alias, tamaño,
  `TIMEOUTS`, `MAX_ATTEMPTS`, `scaledSize`: donde entran las constantes del ajuste.
- `app/src/main/java/com/sazon/app/data/ai/image/OpenAiImages.kt` — DTOs del wire de generación (la
  respuesta `data[].b64_json` se reutiliza tal cual para edits; la petición de generación no cambia).
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePayload.kt` — decodificación tolerante
  compartida (sin cambios).
- `app/src/main/java/com/sazon/app/data/ai/image/DishImageReducer.kt` y `AndroidDishImageReducer.kt` —
  costura de reducción y único fichero con `android.graphics` (sin cambios).
- `app/src/main/java/com/sazon/app/data/RecipePhotoAssets.kt` — convención **pura** de los assets de
  receta (`ROOT = "img/recipes"`, `DEFAULT_KEY = "default"`, `EXTENSIONS = [webp, jpg, png]`): es el
  ancla versionada que se reutiliza y el patrón de resolución por extensiones a imitar.
- `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` — patrón de lectura de assets con
  `context.assets`, contrato `null` sin lanzar y cache LRU; aquí solo se toma el patrón de lectura.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 126-150) — punto de cableado del
  adaptador y de la caché (`GatedDishImageGenerator` envuelve a `DevExpertDishImageGenerator`).
- `app/src/main/java/com/sazon/app/data/image/{DishImageResolver,DishImageSettings,GatedDishImageGenerator}.kt`
  — contrato del borde (una unidad de tope por petición, `null` degradado, cancelación propagada): no
  se tocan y acotan la consecuencia de la Decisión 6.
- `app/src/main/java/com/sazon/app/domain/image/{DishImageGenerator,DishImagePrompt,DishImageCacheKey,DishVisualDescriptor}.kt`
  — la política pura aceptada (fases 1 y 5): se consume tal cual y **no** se modifica.
- `app/src/test/java/com/sazon/app/data/ai/image/DevExpertDishImageGeneratorTest.kt` — patrón de test
  con `MockEngine` + `AiHttpClientFactory.create(mock.engine)`, `runTest`, clave ficticia `test-key`,
  reductor falso y contador de peticiones: es el fichero que gana los escenarios 1-9.
- `app/src/test/java/com/sazon/app/data/ai/image/DishImagePolicyTest.kt` — patrón del test de política
  pura (escenario 11).
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — patrón **directo** del que se copia
  la forma: `RESPONSE_FORMAT_ENABLED` (interruptor de una feature en un único sitio) y
  `requiresResponseFormatFallback(400/422)` / `isRetryableStatus` (fallback **acotado por estado**, no
  por transporte).
- `gradle/libs.versions.toml` — Ktor `3.6.0` y `ktor-client-mock` presentes: el multipart
  (`io.ktor.client.request.forms`) vive en `ktor-client-core` y **no** hace falta tocar el build.
- `scripts/fetch-recipe-photos.sh` — los assets de `recipe-photos` son una lámina aceptada; `default.*`
  es la foto **genérica/neutra** y el script convierte a WebP q80 ≤960 px **si hay herramienta** (por
  eso la extensión real puede variar y la referencia debe resolverse por extensiones).
- `docs/technical-discovery.md` (bullets de las fases 1-5, líneas 531-614) — el adaptador real, la
  caché, la UI y el descriptor; incluye la frase "una unidad por petición al proveedor" que la
  Decisión 6 matiza.
- `docs/risks-and-open-questions.md` (fila de cuota/latencia, línea 144, y lista de ADRs, líneas
  169-176) — el riesgo de cuota y el estado de las fases.
- `docs/design/concepts/README.md` — el gate de capturas aplica solo a features de UI (esta no lo es).

### Existing Patterns To Follow

- **Ktor aislado en `data.ai`**: el multipart no introduce red en ninguna otra capa; el adaptador sigue
  sin conocer `Recipe`/`Taxonomía` (usa `request.prompt` tal cual).
- **Fallback acotado por estado, ya existente**: `AiProviderPolicy.requiresResponseFormatFallback`
  (400/422) y `isRetryableStatus` son el precedente exacto de "un fallback concreto, decidido por
  estado, sin retry de transporte".
- **Interruptor de feature en un único sitio**: `AiProviderPolicy.RESPONSE_FORMAT_ENABLED` es el patrón
  de `DishImagePolicy.STYLE_REFERENCE_ENABLED`.
- **Resolución de assets por extensión con `exists` inyectado**: `RecipePhotoAssets.resolve(...)`
  (puro, JVM-testeable) y su costura Android `AssetRecipePhotoSource` (lectura real, `null` sin lanzar).
- **Degradación silenciosa**: `null` = no disponible, nunca excepción; la `CancellationException` se
  relanza primero (ADR 0005).
- **Un solo intento por petición al proveedor** (`MAX_ATTEMPTS = 1`): no se duplica cuota.
- **Tests JVM con `MockEngine`** para el transporte y **Robolectric** solo para la costura Android.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo.

### Current Gaps

- **No existe ninguna noción de referencia de estilo**: hoy el estilo vive solo en el `STYLE_SUFFIX` del
  prompt; no hay endpoint de edits, ni multipart, ni asset identificado como ancla.
- **`DishImagePolicy` no declara `/images/edits`** ni el alias `image-edit` (solo generación).
- **No hay precedente de multipart en el repo**: `data.ai` solo envía JSON (chat e imágenes); el test
  del wire tampoco tiene precedente de inspección de un cuerpo multipart.
- **El proveedor no está verificado en código para edits**: el contrato (alias `image-edit`, multipart,
  tamaño 1024, `b64_json`) viene de la documentación del proveedor citada en la propuesta §13 y de que
  el gateway es compatible con OpenAI; la prueba real es **opcional** (verificación #3) y la degradación
  está diseñada para que un desacuerdo degrade sin romper nada.

## Technical Approach

### 1. Dónde vive

Todo en `com.sazon.app.data.ai.image` (sin paquete nuevo): la política pura en `DishImagePolicy`, el
contrato del multipart en `OpenAiImages.kt`, la referencia (valor + política pura) en
`DishImageStyleReference.kt`, su cargador Android en `AssetDishImageStyleReference.kt` y el flujo
edits→respaldo en `DevExpertDishImageGenerator`. El dominio, `data.image`, `ui` y `data/ai` (chat) no
se tocan.

### 2. Política (pura): `DishImagePolicy`

```kotlin
object DishImagePolicy {
    // ... lo existente (IMAGES_GENERATIONS_PATH, MODEL_ALIAS, IMAGE_SIZE, TARGET_WIDTH_PX,
    //     WEBP_QUALITY, TIMEOUTS, MAX_ATTEMPTS, endpointUrl, scaledSize) no cambia ...

    const val IMAGES_EDITS_PATH = "/images/edits"
    const val EDIT_MODEL_ALIAS = "image-edit"     // alias del proveedor, no el nombre real
    const val STYLE_REFERENCE_ENABLED = true      // patrón AiProviderPolicy.RESPONSE_FORMAT_ENABLED

    /** Estados de "rechazo definitivo" del ajuste que disparan el respaldo por texto. */
    val EDIT_FALLBACK_STATUSES = setOf(400, 404, 405, 413, 415, 422)

    fun editsUrl(): String = AiProviderPolicy.BASE_URL + IMAGES_EDITS_PATH
    fun editFailureRequiresTextFallback(statusCode: Int): Boolean = statusCode in EDIT_FALLBACK_STATUSES
}
```

- `IMAGE_SIZE` (`1024x1024`) y `TIMEOUTS` se **reutilizan** para el ajuste: mismo tamaño pedido y misma
  política de tiempos (una subida de referencia de pocos cientos de KB no pide otra).
- `STYLE_REFERENCE_ENABLED` es el **único** interruptor del anclaje: apagado = el adaptador va directo
  a texto→imagen (una línea de rollback, sin nueva feature).
- `editFailureRequiresTextFallback` es pura y testeable: el conjunto de estados es el único sitio donde
  se decide "esto no es un problema de red, es que el ajuste no se acepta".

### 3. Referencia de estilo: valor + política pura + cargador Android

`DishImageStyleReference.kt` (puro salvo el tipo de bytes):

```kotlin
/** Referencia de estilo ya cargada: la parte de fichero (`image`) del multipart. */
class DishImageStyleReference(
    val bytes: ByteArray,
    val mediaType: String,   // "image/jpeg" | "image/webp" | "image/png"
    val fileName: String,    // p. ej. "dish-style-reference.jpg"
)

/** Convención de la referencia: el asset neutro aceptado de recipe-photos. Puro, sin I/O. */
object DishImageStyleReferencePolicy {
    val ASSET_BASE = "${RecipePhotoAssets.ROOT}/${RecipePhotoAssets.DEFAULT_KEY}" // "img/recipes/default"
    val EXTENSIONS = RecipePhotoAssets.EXTENSIONS                                // [webp, jpg, png]

    fun resolve(exists: (String) -> Boolean): String?          // primera extensión existente, o null
    fun mediaTypeFor(extension: String): String                // jpg/jpeg -> image/jpeg, webp, png
    fun fileNameFor(path: String): String                      // "dish-style-reference.<ext>"
}
```

- La referencia es **una foto aceptada que ya viaja en el APK** (`img/recipes/default.<ext>`, la foto
  genérica/neutra de `recipe-photos`, propuesta §4.2). **No** se crea ni modifica ningún asset.
- Se resuelve por extensiones (el script de empaquetado puede haber convertido a WebP) y `null` si no
  hay ninguna: mismo patrón que `RecipePhotoAssets.resolve` (predicado inyectado → JVM puro).

`AssetDishImageStyleReference.kt` (Android, `android.content.Context` + `java.io`):

```kotlin
class AssetDishImageStyleReference(
    private val context: Context,
    private val assetBase: String = DishImageStyleReferencePolicy.ASSET_BASE,
) {
    /** Bytes de la referencia; `null` si no hay asset legible (degrada al camino de texto). */
    fun load(): DishImageStyleReference?
}
```

- Lee `context.assets.open(path)` en `use { }`, best-effort (patrón `AssetRecipePhotoSource.exists`):
  ausencia, `IOException` o bytes vacíos → `null`, **nunca** lanza.
- Es el **segundo** fichero Android de `data.ai.image` (el otro es `AndroidDishImageReducer`); el único
  con `android.graphics` sigue siendo el reductor.
- `assetBase` inyectable para poder testear el camino "no existe" sin depender del contenido real.

### 4. Wire: multipart de `/images/edits` en `OpenAiImages.kt`

Se **reutiliza** `ImageGenerationResponse`/`ImageGenerationDatum` (la respuesta se asume con la misma
forma `data[].b64_json`; `url`/`revised_prompt` se ignoran). Se añade el constructor del cuerpo:

```kotlin
const val EDIT_REFERENCE_PART = "image"

/** Partes del multipart de `/images/edits`: model, prompt, size y la referencia como fichero. */
fun editsFormData(reference: DishImageStyleReference, model: String, prompt: String, size: String): List<PartData>
```

Contrato exacto del cuerpo (OpenAI-compatible):

| Parte | Valor |
|---|---|
| `model` | `image-edit` |
| `prompt` | `request.prompt` (idéntico; incluye el `STYLE_SUFFIX`) |
| `size` | `1024x1024` |
| `image` | los bytes de la referencia, con `Content-Disposition: filename="<fileName>"` y `Content-Type: <mediaType>` |

Sin `n`, sin `mask`, sin `response_format`, sin `stream` y sin ningún campo de dominio. El envío usa
`MultiPartFormDataContent` (o `submitFormWithBinaryData`) de `io.ktor.client.request.forms`
(`ktor-client-core`): **sin dependencias nuevas**.

### 5. Adaptador: `DevExpertDishImageGenerator` (edits con respaldo por texto)

Constructor (aditivo; los llamantes existentes no cambian de comportamiento):

```kotlin
class DevExpertDishImageGenerator(
    private val apiKey: String,
    private val client: HttpClient,
    private val reducer: DishImageReducer = AndroidDishImageReducer,
    private val model: String = DishImagePolicy.MODEL_ALIAS,
    private val timeouts: DishImageTimeouts = DishImagePolicy.TIMEOUTS,
    private val styleReference: DishImageStyleReference? = null,          // hace falta la fase 6
    private val styleReferenceEnabled: Boolean = DishImagePolicy.STYLE_REFERENCE_ENABLED,
) : DishImageGenerator
```

Flujo de `generate(request)`:

1. `apiKey.isBlank()` → `null`, **sin** ninguna petición.
2. `val reference = styleReference?.takeIf { styleReferenceEnabled && it.bytes.isNotEmpty() }`.
3. Si `reference != null`: **un** intento contra `editsUrl()` con el multipart del punto 4, el mismo
   `timeout { }` por petición y `Authorization: Bearer <clave>`.
   - `CancellationException` → **relanzar** (nunca respaldo).
   - Cualquier otra `Exception` (transporte/timeout) → `return null` (escenario 5; sin respaldo).
   - Estado fuera de `200..299` → si `DishImagePolicy.editFailureRequiresTextFallback(status)` se pasa
     al paso 4; si no → `return null` (escenario 6).
   - Estado `2xx`: leer el cuerpo (`bodyAsText`; el fallo de transporte en la lectura también degrada a
     `null`, sin respaldo) → `DishImagePayload.decode` → `reducer.reduceToWebp`. Si el resultado no es
     `null` y no está vacío → **devolverlo**. Si es inutilizable → paso 4 (escenario 4).
4. **Respaldo / camino sin referencia**: el camino de la fase 2 **tal cual** (`endpointUrl()`, cuerpo
   JSON `model`/`prompt`/`size`, un intento, no-2xx → `null`, transporte/timeout → `null`, decode +
   reduce). Se extrae a un privado (`generateFromText(prompt)`) y se reutiliza para no duplicar la
   política de fallo. **Nunca** se vuelve a intentar `edits` ni se encadena un tercer intento.

El adaptador sigue **sin `Log`**, sin telemetría, sin estado y sin conocer el dominio.

### 6. Cableado: `MainActivity`

Una línea más en la composición ya existente, con la misma disciplina best-effort que
`AssetTaxonomySource`/`AssetSeedRecipeSource`:

```kotlin
val styleReference = AssetDishImageStyleReference(applicationContext).load()   // null = fase 2
val dishImageResolver = DishImageResolver(
    ...,
    generator = GatedDishImageGenerator(
        delegate = DevExpertDishImageGenerator(
            apiKey = BuildConfig.DEVEXPERT_API_KEY,
            client = aiClient,
            styleReference = styleReference,
        ),
        settings = dishImageSettings,
    ),
)
```

Nada más cambia en la cadena (`DishImageResolver`, caché, loader, seams, `RecipeArtwork`).

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` — modificar; path/alias de edits,
  `editsUrl()`, `EDIT_FALLBACK_STATUSES`, `editFailureRequiresTextFallback`, `STYLE_REFERENCE_ENABLED`.
- `app/src/main/java/com/sazon/app/data/ai/image/DishImageStyleReference.kt` — crear; valor de la
  referencia + `DishImageStyleReferencePolicy` (pura, reutiliza `RecipePhotoAssets`).
- `app/src/main/java/com/sazon/app/data/ai/image/AssetDishImageStyleReference.kt` — crear; cargador
  Android best-effort del asset versionado (`null` sin lanzar).
- `app/src/main/java/com/sazon/app/data/ai/image/OpenAiImages.kt` — modificar; `EDIT_REFERENCE_PART` y
  `editsFormData(...)` (los DTOs de respuesta se reutilizan; la petición de generación no cambia).
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — modificar; parámetros
  aditivos, camino de edits y respaldo acotado por estado (extrayendo el camino de texto de la fase 2).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar; cargar la referencia del asset y
  pasarla al adaptador.
- `app/src/test/java/com/sazon/app/data/ai/image/DishImagePolicyTest.kt` — modificar; escenario 11
  (edits URL/alias, conjunto exacto de estados de respaldo, interruptor).
- `app/src/test/java/com/sazon/app/data/ai/image/DevExpertDishImageGeneratorTest.kt` — modificar;
  escenarios 1-9 con `MockEngine` (edits multipart, respaldo, sin respaldo, sin clave, cancelación).
- `app/src/test/java/com/sazon/app/data/ai/image/DishImageStyleReferenceTest.kt` — crear; escenario 10
  (parte pura: `resolve` con `exists` falso/verdadero y orden de extensiones, `mediaTypeFor`,
  `fileNameFor`).
- `app/src/test/java/com/sazon/app/data/ai/image/AssetDishImageStyleReferenceTest.kt` — crear;
  Robolectric `@Config(sdk = [33])`: el asset real versionado se lee (bytes no vacíos + `mediaType` +
  `fileName`) y un `assetBase` inexistente devuelve `null` sin lanzar.
- `docs/adr/0005-ai-dish-images.md` — modificar; estado "Aceptado parcialmente (fases 1-6)", alcance de
  la fase 6 (referencia versionada, respaldo acotado por estado, sin subida de usuario) y consecuencias
  (sin subir `VERSION`; ≤2 llamadas por petición si el ajuste se rechaza).
- `docs/technical-discovery.md` — modificar; el adaptador gana `/images/edits` (multipart, alias
  `image-edit`) con respaldo por texto, la referencia es el asset neutro versionado, el cableado en
  `MainActivity` y el matiz del presupuesto (una unidad por petición, ≤2 llamadas).
- `docs/risks-and-open-questions.md` — modificar; la fila de cuota/latencia anota que un rechazo del
  ajuste cuesta una llamada extra (con el interruptor como rollback) y la lista de ADRs refleja las
  fases 1-6.
- `PROGRESS.md` y `feature_list.json` — modificar al implementar/validar (estado, evidencia); **no** los
  toca el planificador.

**No se toca**: `domain/image/**`, `domain/**`, `data/image/**`, `data/ai/**` (chat) salvo lo listado,
`ui/**`, `data/RecipePhotoAssets.kt`, `data/AssetRecipePhotoSource.kt`, `Recipe.kt`,
`RecipeValidator.kt`, `AiRecipeIds.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`,
`init.sh`, `scripts/**`, `app/src/main/assets/**` (ni un byte), `AndroidManifest.xml`,
`docs/domain/taxonomy.json`, `seed-recipes.json`, `DESIGN.md`, `docs/design/**`,
`app/src/androidTest/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: integration`; no hay pantalla, componente Compose, microcopy ni estado
  visual; la dirección visual de la app no cambia).
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.
- **El gate de capturas no aplica**: **no** se crea `docs/design/concepts/dish-style-reference/` ni
  ninguna otra carpeta bajo `docs/design/concepts/` (las capturas obligatorias son solo de features
  `area: ui`). La referencia de estilo es un **asset de contenido** (`img/recipes/default.<ext>`), no un
  concepto de diseño de pantalla.

## Durable Documentation Impact

- `docs/adr/0005-ai-dish-images.md`: **update** — pasa a "Aceptado parcialmente (fases 1-6)"; documenta
  la decisión de la fase 6 (ajuste con referencia versionada, respaldo **solo** ante rechazo definitivo,
  sin subida de usuario, sin subir `VERSION`, ≤2 llamadas por petición) y deja la fase 7 pendiente.
- `docs/technical-discovery.md`: **update** — nueva frontera durable del adaptador `data.ai.image`
  (`/images/edits` multipart con alias `image-edit` + respaldo a `/images/generations`), la referencia
  como asset versionado, el cableado en `MainActivity` y el matiz de "una unidad por petición, hasta
  dos llamadas al proveedor si el ajuste se rechaza".
- `docs/risks-and-open-questions.md`: **update** — la fila de cuota/latencia recoge el coste adicional
  (una llamada desechada por rechazo del ajuste, acotada y reversible con
  `DishImagePolicy.STYLE_REFERENCE_ENABLED`) y la lista de ADRs refleja las fases 1-6.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; la referencia no
  se persiste y no es un campo de `Recipe`.
- `docs/domain/taxonomy.md` §4 y §6: **not needed** — el contrato con la IA y la cadena de resolución en
  runtime no cambian (mismo nivel generado; solo cambia el endpoint que lo produce).
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `DESIGN.md`: **not needed** — sin UI, sin microcopy y sin cambio de dirección visual; el ancla es
  contenido generado, no diseño de pantalla.
- `docs/ai-dish-images-proposal.md`: **not needed** — sigue siendo la propuesta; §4.2 ya describe la
  variante C.
- `docs/design/**`: **not needed** — el gate de capturas no aplica.

## Implementation Plan

1. Extender `DishImagePolicy` (path/alias de edits, `editsUrl`, conjunto de estados de respaldo,
   `editFailureRequiresTextFallback`, `STYLE_REFERENCE_ENABLED`) y ampliar `DishImagePolicyTest`.
2. Crear `DishImageStyleReference` + `DishImageStyleReferencePolicy` (puro) con su test JVM (resolución
   por extensiones, media types y nombre de fichero).
3. Crear `AssetDishImageStyleReference` (lectura del asset, `null` sin lanzar) y su test Robolectric
   (asset real + `assetBase` inexistente).
4. Añadir `editsFormData(...)` a `OpenAiImages.kt` (multipart: `model`, `prompt`, `size`, `image`).
5. Extender `DevExpertDishImageGenerator` (parámetros aditivos, camino de edits, respaldo acotado por
   estado, extracción del camino de texto) y ampliar `DevExpertDishImageGeneratorTest` con los
   escenarios 1-9 (JVM, `MockEngine`, reductor falso, clave `test-key`).
6. Cablear en `MainActivity` (`AssetDishImageStyleReference(...).load()` → `styleReference`).
7. Inspeccionar alcance y pureza: `domain.image`, `data.image`, `ui/**` y los assets sin diff;
   `android.*` solo en `AndroidDishImageReducer.kt` y `AssetDishImageStyleReference.kt`; sin `Log`; sin
   dependencias nuevas; `docs/design/concepts/dish-style-reference` inexistente.
8. Actualizar el ADR 0005, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`.
9. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Añadir a `DishImagePolicy` `IMAGES_EDITS_PATH`, `EDIT_MODEL_ALIAS`, `STYLE_REFERENCE_ENABLED`,
      `EDIT_FALLBACK_STATUSES`, `editsUrl()` y `editFailureRequiresTextFallback(status)`.
- [ ] Crear `DishImageStyleReference` (`bytes`, `mediaType`, `fileName`) y
      `DishImageStyleReferencePolicy` (`ASSET_BASE` = `img/recipes/default`, `EXTENSIONS` de
      `RecipePhotoAssets`, `resolve(exists)`, `mediaTypeFor`, `fileNameFor`).
- [ ] Crear `AssetDishImageStyleReference(context, assetBase)` con lectura best-effort del asset
      (`null` si no existe/ilegible/vacío, sin lanzar).
- [ ] Añadir `EDIT_REFERENCE_PART = "image"` y `editsFormData(reference, model, prompt, size)` a
      `OpenAiImages.kt` (sin cambiar los DTOs ni la petición de generación).
- [ ] Extender `DevExpertDishImageGenerator` con `styleReference` y `styleReferenceEnabled`: un intento a
      `/images/edits` (multipart, mismo `timeout { }` y `Bearer`), respaldo a texto **solo** ante
      `editFailureRequiresTextFallback(status)` o carga inutilizable, `null` ante transporte/timeout y
      `401/403/429/5xx`, y `CancellationException` relanzada en ambas fases.
- [ ] Extraer el camino de texto de la fase 2 a un privado reutilizado por el respaldo (sin duplicar la
      política de fallo ni el decode/reduce).
- [ ] Pasar `styleReference = AssetDishImageStyleReference(applicationContext).load()` al adaptador en
      `MainActivity` (best-effort; `null` = comportamiento de la fase 2).
- [ ] Ampliar `DishImagePolicyTest` (edits URL/alias, interruptor y el conjunto exacto de estados que
      disparan respaldo: `400/404/405/413/415/422` sí; `401/403/408/429`, `5xx`, `2xx` no).
- [ ] Ampliar `DevExpertDishImageGeneratorTest`: multipart exacto (una petición, campos y bytes de
      referencia, sin `cacheKey`/`recipeId`), respaldo ante `400/404/405/413/415/422` y ante `200`
      inutilizable, `null` sin respaldo ante transporte/timeout y `401/403/429/500`, sin cadena de un
      tercer intento, sin clave = 0 peticiones y cancelación propagada.
- [ ] Crear `DishImageStyleReferenceTest` (JVM) y `AssetDishImageStyleReferenceTest` (Robolectric
      `@Config(sdk = [33])`).
- [ ] Verificar por inspección que `domain.image`, `data.image`, `ui/**`, los assets y
      `RecipePhotoAssets.kt` no cambian; `android.*` solo en los dos ficheros Android de la lista; sin
      `Log`; `VERSION` sigue `dish-image-v1`.
- [ ] Actualizar el ADR 0005, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde y registrar la evidencia.
- [ ] Actualizar `feature_list.json` (a `passing` con evidencia) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red, **no**
  gasta cuota y **no** modifica `init.sh`.
- Focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*"
  --rerun-tasks` → `BUILD SUCCESSFUL` (las 4 suites de la fase 2 + las 2 nuevas).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. El implementer **re-deriva** la baseline en su HEAD (la última baseline
  aceptada conocida en este worktree es **86 suites / 619 tests**, sesión 094) y confirma que el delta
  es exactamente el de los tests nuevos/ampliados de `data.ai.image`.
- `bash scripts/gradle.sh lint` → **0 errores** (los 13 warnings de `GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion` y `OldTargetApi` son preexistentes, todos en el
  build/version catalog, **ninguno** bajo `app/src`).
- Inspección de alcance y pureza:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/data/ai/image` → solo
    `AndroidDishImageReducer.kt` y `AssetDishImageStyleReference.kt`.
  - `grep -rn "Log\.\|filesDir\|getDatabasePath\|android.graphics" app/src/main/java/com/sazon/app/data/ai/image`
    → `android.graphics` solo en `AndroidDishImageReducer.kt`; 0 `Log`; 0 almacenamiento interno.
  - `grep -rn "Recipe\|Taxonomy\|DishImagePrompt\|DishImageCacheKey" app/src/main/java/com/sazon/app/data/ai/image`
    → 0 (el adaptador consume `request.prompt` tal cual).
  - `grep -rn "VERSION" app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` →
    `dish-image-v1` sin cambios; `git diff --name-only` sin `domain/image/**`, `data/image/**`,
    `ui/**`, `app/src/main/assets/**`, build, `init.sh`, `scripts/**`, `AndroidManifest.xml`.
  - `test -d docs/design/concepts/dish-style-reference` → **no existe** (el gate de UI no aplica).
  - Multipart: el test afirma URL, método, `Authorization`, `Content-Type` multipart y las partes
    (nombres, valores y bytes de la referencia). La forma concreta de leer el cuerpo en `MockEngine`
    (`MultiPartFormDataContent.parts` o los bytes del `OutgoingContent`) es un detalle de
    implementación; lo que **no** es negociable es el contrato de las cuatro partes.
- Cobertura E2E: **no aplica**. No hay suite E2E persistente; la feature no introduce pantalla ni flujo
  de UI nuevo y su verificación canónica es el test JVM con `MockEngine` (más Robolectric para el
  asset), conforme a `AGENTS.md`.
- **Prueba real opcional** (verificación exigida #3, **no** puerta de aceptación): **una** llamada a
  `/images/edits` con la referencia de `local.properties` y el mismo prompt de la variante A, para
  comparar ambas imágenes. **Gasta ~2 % de la cuota semanal**; se hace una sola vez, nunca desde un test
  y nunca con la clave en el árbol versionado (artefactos fuera del repo o en
  `docs/validations/dish-style-reference.md`). Sirve además para decidir si
  `DishImagePolicy.STYLE_REFERENCE_ENABLED` debe quedarse en `true`: si el proveedor rechazara siempre
  el ajuste, el coste sería una llamada desechada por imagen y el interruptor es el rollback.
- **Revisión manual de la persona** (tras el merge, no bloqueante): en el emulador, generar una imagen y
  comprobar que el resultado sigue siendo una foto de plato plausible (el cableado solo se verifica por
  inspección en JVM).
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de la tirada focal
  `--tests "com.sazon.app.data.ai.image.*" --rerun-tasks`.
- Conteo parseado del XML (suites/tests, 0 fallos/errores/saltados) con los tests nuevos/ampliados
  nombrados (`DishImagePolicyTest`, `DevExpertDishImageGeneratorTest`, `DishImageStyleReferenceTest`,
  `AssetDishImageStyleReferenceTest`) y la baseline re-derivada.
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Vectores concretos: partes del multipart (`model=image-edit`, `prompt` idéntico, `size=1024x1024`,
  `image` con los bytes de la referencia) y **una sola** petición; respaldo con exactamente 2 peticiones
  ante `400/404/405/413/415/422` y ante `200` inutilizable; `null` con 1 petición ante
  `SocketTimeoutException`/`HttpRequestTimeoutException`/`IOException`, `401/403/429/500`; sin cadena de
  un tercer intento; `apiKey` en blanco → 0 peticiones; `CancellationException` relanzada; el asset real
  resuelto como `img/recipes/default.<ext>` con su `mediaType`.
- Resultado de los greps/diff de inspección y de `test -d docs/design/concepts/dish-style-reference`.
- Si se ejecuta la prueba real opcional: la imagen resultante, la comparación con la variante A, la
  latencia observada y la decisión sobre `STYLE_REFERENCE_ENABLED` (con el recordatorio del ~2 % de
  cuota).
- Anotación en `feature_list.json` (evidencia + `passing`) y en `PROGRESS.md`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **La referencia es un ajuste de la casa, no un dato de la petición: `DishImageRequest` no gana
   campos.** El ADR 0005 (decisión 1) dejó abierta la ampliación del request con una "foto de
   referencia"; se **descartó** para esta fase: añadir `ByteArray` a un `data class` de dominio rompería
   la igualdad estructural que ya se testea (`DishImageRequestTest`) y llevaría I/O de assets al
   dominio, cuando el ancla es una **constante de la app** (un único asset versionado). La alternativa
   elegida (parámetro de constructor + cargador en `data.ai.image`) mantiene intactos el puerto, la
   política pura y sus tests, y deja el punto de composición de la fase 1/5 exactamente igual.
2. **El respaldo es un fallback por estado, no un reintento de transporte.** Solo se cae a
   `/images/generations` cuando el ajuste es **rechazado** (`400/404/405/413/415/422`) o cuando un `2xx`
   trae carga inutilizable. Transporte/timeout (`SocketTimeoutException`,
   `HttpRequestTimeoutException`, `IOException`) y `401/403/429/5xx` devuelven `null` **sin** segunda
   llamada: el respaldo fallaría igual y duplicaría el gasto (misma lógica que `MAX_ATTEMPTS = 1` y que
   `AiProviderPolicy.requiresResponseFormatFallback`). En ningún caso hay más de **dos** intentos.
3. **El prompt y la clave no cambian; `VERSION` no sube.** La referencia **añade** un ancla visual; no
   sustituye el `STYLE_SUFFIX` (que sigue viviendo en un único sitio, `DishImagePrompt`), así que la
   plantilla y la normalización —los dos disparadores durables de invalidación— no cambian y
   `DishImageCacheKey.VERSION` sigue `dish-image-v1`. *Consecuencia aceptada*: las imágenes ya
   cacheadas conservan el aspecto de la fase 2-5 hasta que la LRU las evicte (estilo mixto transitorio);
   subir `VERSION` a `dish-image-v2` haría uniforme el estilo de golpe pero obligaría a **repagar cuota
   por cada plato ya cacheado**, así que se deja como decisión de seguimiento si la persona prefiere
   uniformidad a gasto (una línea, con su propia evidencia).
4. **La referencia es el asset aceptado y versionado `img/recipes/default.<ext>`.** Es una foto de la
   lámina aceptada de `recipe-photos` (propuesta §4.2 la nombra expresamente), viaja en el APK y se
   resuelve por extensiones porque el script de empaquetado puede haberla convertido a WebP. **No** se
   crea ni convierte ningún asset nuevo (el planificador y el implementer no producen binarios) y **no**
   hay subida del usuario, galería, cámara ni permisos. *Alternativa*: un ancla propia
   (`img/style/...`), que sería otro asset y su propia feature.
5. **`STYLE_REFERENCE_ENABLED` es el único interruptor del anclaje** (patrón
   `AiProviderPolicy.RESPONSE_FORMAT_ENABLED`): apagado, el adaptador va directo a texto→imagen con cero
   peticiones al ajuste. Es la palanca de rollback si la prueba real (o el uso) muestra que `image-edit`
   no está soportado por el proveedor.
6. **El tope por sesión sigue contando una unidad por petición de imagen, no por llamada al proveedor.**
   No se toca `DishImageSettings`/`GatedDishImageGenerator`: un rechazo del ajuste implica **hasta dos**
   llamadas (una desechada) para una unidad de presupuesto. Es acotado (≤2), solo ocurre ante rechazo y
   reversible con el interruptor; se documenta en `technical-discovery.md` y en la fila de riesgo. Si
   importara, la salida natural es contar llamadas **dentro** del adaptador o mover el respaldo a un
   decorador, y sería su propia feature.
7. **El adaptador sigue siendo transporte puro y mudo.** No conoce `Recipe`/`Taxonomía`/descriptor, usa
   `request.prompt` tal cual, no mete `cacheKey`/`recipeId` en la petición, no registra nada (`Log`) y la
   clave solo viaja en `Authorization`. El contrato de fallo no cambia: `null` = no disponible y la
   `CancellationException` se propaga en las dos fases.
8. **Sin capturas de UI**: `area: integration`, sin comportamiento de pantalla (mismo criterio que las
   fases 1-5 ya aceptadas); el gate de `docs/design/concepts/<feature-id>/` no aplica.

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **Soporte real de `/images/edits` por el proveedor** (alias `image-edit`, multipart, `size`
   `1024x1024`, respuesta `b64_json`): se asume por la documentación del proveedor y la compatibilidad
   OpenAI (propuesta §13), **no** verificado en código. Mitigación: el respaldo por texto, el
   interruptor y la prueba real opcional; el aislamiento en `data.ai.image` permite corregirlo con una
   feature correctiva.
- **Formato exigido de la imagen de referencia**: OpenAI-compatible suele aceptar PNG/JPEG/WebP, pero
   algunas variantes de `/images/edits` exigen PNG con alfa; si `default.jpg` fuera rechazado (`415`),
   el respaldo cubre el caso (y la prueba real lo revelaría, sugiriendo una conversión local en una
   feature posterior).
- **Inspección del cuerpo multipart en `MockEngine`**: es el primer multipart del repo. La vía cómoda
   es leer `MultiPartFormDataContent.parts` (o los bytes del `OutgoingContent` con el helper de
   `ktor-client-mock`); si costara, el contrato mínimo verificable sigue siendo `Content-Type` multipart
   + partes + bytes de la referencia. Reportar como hallazgo si se debilita cualquier aserción.
- **Lectura de assets en Robolectric**: el test del cargador usa los assets reales del módulo; si el
  `AssetManager` de Robolectric no los sirviera en WSL, hay que **reportarlo** (la parte pura ya queda
  cubierta en JVM y la costura Android se puede aislar/inspeccionar), no debilitar la verificación.
- **Estilo mixto transitorio en la caché** (Decisión 3): imágenes anteriores mantienen el aspecto de la
  fase 2-5 hasta que la LRU las evicte.
- **El ancla cambia si cambia `default.jpg`**: la referencia sigue al asset neutro de `recipe-photos`;
  un cambio de esa foto cambia el estilo de las imágenes nuevas (degradación silenciosa, sin error).
- **Payload del multipart**: se reenvían los bytes del asset en cada generación (cientos de KB como
  mucho, dado que los assets están acotados a 960 px de ancho); impacto despreciable frente a los ~4,2 s
  y el ~2 % de cuota por imagen.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: **sin** cambios en `domain.image` (puerto, request,
      prompt, clave, descriptor), `data.image`, `ui/**`, los assets, `RecipePhotoAssets.kt`, build,
      `init.sh`, `AndroidManifest.xml`, la Taxonomía ni `seed-recipes.json`; sin dependencias nuevas.
- [ ] Los 13 escenarios pasan, incluidos los negativos obligatorios: sin referencia = contrato de la
      fase 2 (una petición), respaldo **solo** ante `400/404/405/413/415/422` y `200` inutilizable,
      `null` sin respaldo ante transporte/timeout y `401/403/429/5xx`, sin cadena de un tercer intento,
      sin clave = 0 peticiones y `CancellationException` relanzada en ambas fases.
- [ ] La petición de ajuste es exactamente `POST https://inference.devexpert.io/v1/images/edits`
      multipart con `model=image-edit`, `prompt=request.prompt`, `size=1024x1024` y la parte `image` con
      los bytes de la referencia (filename + media type), sin `cacheKey`/`recipeId` ni campos extra, y
      **una sola** vez por `generate` (≤2 llamadas en total, nunca más).
- [ ] La referencia es un **asset versionado** (`img/recipes/default.<ext>` vía
      `RecipePhotoAssets.EXTENSIONS`): no hay subida de usuario, permisos nuevos, galería, cámara ni
      persistencia; ausencia/fallo de lectura → `null` sin lanzar y degradación al camino de texto.
- [ ] `DishImageCacheKey.VERSION` sigue siendo `dish-image-v1`, el prompt y la clave producidos por la
      fase 1/5 son idénticos, y el formato guardado sigue siendo el WebP ≤960 px del reductor
      compartido.
- [ ] `data.ai.image` sigue siendo transporte puro y mudo: sin `Recipe`/`Taxonomía`/`DishImagePrompt`/
      `DishImageCacheKey`, sin `Log`, `android.graphics` solo en `AndroidDishImageReducer.kt` y `android.*`
      solo en los dos ficheros Android de la lista.
- [ ] El cableado (`MainActivity`) pasa la referencia al adaptador y con `null` deja el comportamiento
      de la fase 2 intacto; el tope por sesión y el interruptor de usuario no cambian.
- [ ] El ADR 0005 (fases 1-6) y los otros dos docs durables están actualizados y coherentes, incluido el
      matiz del presupuesto (una unidad por petición, ≤2 llamadas) y el coste de un rechazo.
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR; la prueba real (si se hizo) queda documentada con su gasto de cuota.
