# Feature Implementation Spec: Set semilla de imágenes de platos frecuentes

## Source Feature

- `id`: `dish-image-seed`
- `area`: `data`
- `depends_on`: `["dish-image-domain"]` (satisfecha: `accepted`, sesión 086)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1116-1130) + `docs/ai-dish-images-proposal.md` §5 y §10
  (fase **7**) + `docs/adr/0005-ai-dish-images.md` (fase 7 pendiente)
- Verificación exigida (textual de la feature):
  1. Los assets semilla están versionados y se resuelven por la clave canónica (`dish-image-v1-…`); un
     test comprueba el mapeo clave→asset.
  2. `./gradlew test` sigue en verde y sin dependencias nuevas; el script de preparación es idempotente
     y no corre en runtime.
  3. Sin red, un plato del set semilla muestra su imagen propia y no la genérica de categoría.

## Goal

Añadir la **capa de assets semilla** de la propuesta §5: los **platos frecuentes del demo** viajan
**preparados una vez** y **versionados** como fotos WebP en el APK, resueltas por la **clave canónica**
del plato (`DishImageCacheKey`, p. ej. `dish-image-v1-arroz-con-pollo-criollo`), e insertadas como un
nivel nuevo en la cadena ya existente de imagen generada:

```
memoria → disco → ASSETS SEMILLA (por clave canónica) → generador → (foto propia → categoría → default → glifo)
```

Con eso, un plato del set muestra su **imagen propia** (no la genérica de su categoría) **sin gastar
cuota y sin esperar red**: el nivel semilla gana al generador, así que la **primera** imagen de esos
platos tampoco se paga. La preparación (generar las fotos **una vez**) la hace un **script de una sola
vez** que **no** entra en `init.sh` ni en runtime, siguiendo el patrón de
`scripts/fetch-recipe-photos.sh` (validación exacta de claves, conversión a WebP q80 ≤960 px,
idempotencia, presupuesto de tamaño, exit ≠ 0 ante cualquier empaquetado parcial).

Es la **última fase** de `docs/ai-dish-images-proposal.md`: no cambia el puerto, el prompt, la clave,
la caché de disco, el ajuste de sesión ni la UI; solo **añade un nivel de resolución** delante del
generador y el contenido que lo alimenta.

## Non-Goals

- **No** se toca el dominio: `domain/image/**` (`DishImageGenerator`, `DishImageRequest`,
  `DishImagePrompt`, `DishImageCacheKey`, `DishVisualDescriptor`) queda **intacto** y
  `DishImageCacheKey.VERSION` **no** sube (la normalización y la plantilla del prompt no cambian).
- **No** se toca `data.ai.image/**` (adaptador, política, wire, reducción): el script de preparación no
  es código de la app y no cambia el transporte.
- **No** se toca la **caché** de la fase 3 más allá del nivel nuevo del resolutor: `DishImageCachePolicy`,
  `FileDishImageCache`, `MemoryDishImageCache` y `AndroidDishImageCache` quedan como están; el nivel
  semilla **no** persiste nada en disco.
- **No** se toca la **UI ni el cableado visual**: `ui/artwork/**` (`RecipeArtwork`,
  `RecipePhotoSource`, `GeneratedRecipeArtworkSource`, `RecipeImageDecoder`, política), `MainShell` y los
  5 puntos de uso no cambian; el nivel semilla entra por el resolutor que ya está detrás del seam de la
  fase 4.
- **No** se cambia el **gate del interruptor** ni el **tope por sesión** de `dish-image-settings-toggle`:
  con el interruptor apagado se conserva la semántica aceptada ("solo la foto local", **tampoco** las
  imágenes semilla).
- **No** se retira ni se reordena la **cadena local** de `recipe-photos`
  (`data.RecipePhotoAssets`/`AssetRecipePhotoSource`): sigue siendo el respaldo y el último recurso.
- **No** se empaquetan assets para las **12 recetas semilla** (ya tienen su foto propia por `id`).
- **No** se persiste el set semilla en `cacheDir`/Room ni se copia al almacenamiento de la app.
- **No** se implementa `dish-visual-descriptor-wiring` (el `dishKey`/`imagePrompt` de la fase 5 siguen
  sin entrega en runtime).
- **No** se añaden dependencias, ni se toca `gradle/libs.versions.toml`, `init.sh`, la Taxonomía
  (`docs/domain/taxonomy.json`), `seed-recipes.json`, `AndroidManifest.xml` ni `.gitattributes` (ya
  cubre `*.webp`/`*.jpg`/`*.jpeg`/`*.png` como binarios).
- **No hay capturas de UI**: `area: data`, sin pantalla ni estado visual nuevo. **No** se crea
  `docs/design/concepts/dish-image-seed/`.

## Job Story

Cuando enseñe la demo con los platos más buscados,
quiero que sus fotos ya estén dentro de la app,
para que se vean al instante, sin red y sin gastar cuota ni esperar la generación (~4,2 s) de cada una.

## Users And Permissions

- **Usuario local**: **no interactúa** de forma nueva; ve en las mismas tarjetas y en el Detalle la
  imagen del plato (ahora la del set semilla para esos platos) sin red y sin permisos nuevos.
- **Desarrollador / agente**: puede probar la resolución semilla en **JVM** con un `exists`/fuente
  falso (sin red, sin clave y sin cuota) y comprobar la lectura real de los assets con Robolectric; la
  **preparación** de los assets exige una acción puntual (script) con clave o con imágenes de origen.
- **Persona**: decide/confirma la **lista** de platos frecuentes y puede aportar las imágenes de origen
  (vía `--source`) si prefiere no gastar cuota en la preparación; la revisión visual final es suya tras
  el merge.
- **Sistema**: la resolución en runtime es local, determinista y de solo lectura; **sin** red.

## Acceptance Scenarios

### Scenario 1: el asset semilla se resuelve por la clave canónica

Given un plato del set semilla (p. ej. `"Arroz con pollo criollo"`) y su clave
`DishImageCacheKey.of(title) == "dish-image-v1-arroz-con-pollo-criollo"`,
When se resuelve el mapa clave→asset con el predicado de existencia real,
Then devuelve `img/dishes/seed/dish-image-v1-arroz-con-pollo-criollo.<ext>`, probando las extensiones
`webp → jpg → png` en ese orden, y el fichero existe y **no** está vacío.

### Scenario 2: el nivel semilla gana al generador (sin cuota y sin red)

Given un `DishImageResolver` con una fuente semilla que devuelve bytes para la clave del plato y un
generador falso que cuenta llamadas y lanza si se invoca,
When se resuelve la imagen del plato (memoria fría y sin copia en disco),
Then devuelve los bytes del asset semilla y el generador se invoca **0** veces; en la raíz de la caché
**no** queda ningún fichero (el asset no se duplica en disco).

### Scenario 3: la segunda resolución no vuelve a leer el asset

Given el mismo resolutor del Scenario 2 y una fuente semilla que cuenta lecturas,
When se resuelve dos veces el mismo plato,
Then la fuente semilla se lee **una sola vez** (el acierto posterior sale de la memoria del resolutor)
y el generador sigue en **0** llamadas.

### Scenario 4: la precedencia es memoria → disco → semilla

Given un plato cuya clave **sí** tiene copia en disco (generada antes) **y** asset semilla, y otro
plato con asset semilla pero sin copia,
When se resuelven ambos,
Then el primero devuelve los **bytes del disco** (sin tocar la fuente semilla ni el generador) y el
segundo los del **asset semilla** (sin generar y sin escribir en disco).

### Scenario 5: un plato fuera del set no cambia nada (caso negativo)

Given un `DishImageResolver` con fuente semilla real y un plato cuya clave **no** tiene asset
(p. ej. `DishImageCacheKey.of("Plato que no existe")`),
When se resuelve,
Then el nivel semilla devuelve `null` y la cadena sigue **exactamente** como hoy: disco → generador →
`null`; el set semilla **no** es un comodín y no resuelve una imagen para cualquier plato.

### Scenario 6: el fallo de la fuente semilla degrada en silencio

Given una fuente semilla que devuelve `null`, un array vacío o lanza `IllegalStateException`,
When se resuelve una clave,
Then no se interrumpe la cadena (se consulta al generador como hoy / se devuelve `null`), sin
excepción; con `CancellationException`, el resolutor la **relanza**.

### Scenario 7: sin fuente semilla el resolutor es el de la fase 3

Given un `DishImageResolver` construido **sin** fuente semilla (parámetro por defecto),
When se resuelven los escenarios de `dish-image-cache`,
Then el comportamiento es **idéntico** al aceptado hoy (memoria → disco → generador → persistir) y sus
tests siguen verdes sin cambios.

### Scenario 8: la clave se sanea antes de tocar el sistema de ficheros de assets

Given una clave vacía, en blanco, con mayúsculas, con `..` o con `/` (p. ej. `"../../etc/passwd"`),
When se calcula la ruta del asset,
Then devuelve `null` y **nunca** una ruta fuera de `img/dishes/seed/` (el alfabeto canónico `[a-z0-9-]`
es el único admitido; mismo espíritu que el hallazgo Low de `dish-image-cache`).

### Scenario 9: la lectura Android de los assets degrada en silencio

Given `AssetDishImageSeedSource(context)` con un `Context` de test y el asset real de un plato del set,
When se leen sus bytes,
Then devuelve bytes **no vacíos** con la cabecera real de la imagen (WebP `RIFF…WEBP`, JPEG o PNG); con
una clave **no incluida** en el set devuelve `null`, y con una `root` inexistente devuelve `null`,
**sin lanzar** en ningún caso.

### Scenario 10: el set está versionado, es determinista y se empaqueta

Given el árbol de trabajo,
When se inspeccionan `app/src/main/assets/img/dishes/seed/` y el APK de debug,
Then hay **un fichero por plato listado** (`<cacheKey>.<ext>`, extensión `webp`/`jpg`/`png`), el total
no supera **2 MiB**, los ficheros **no** están ignorados por git y viajan en el APK.

### Scenario 11: el script de preparación es de una sola vez e idempotente

Given `scripts/prepare-dish-image-seed.sh`,
When se ejecuta dos veces sin `--force` (con los assets ya empaquetados),
Then la segunda pasada **no** vuelve a generar ni descargar nada (omite lo existente y lo registra),
`init.sh` **no** lo invoca, ningún código de runtime lo referencia, y el script sale con código ≠ 0 ante
una clave faltante, un asset vacío, una extensión desconocida o un total por encima del presupuesto
(**nunca** un empaquetado parcial silencioso).

### Scenario 12: alcance y pureza (inspección)

Given la implementación terminada,
When se inspecciona el diff,
Then `domain/image/**`, `data/ai/image/**`, `data/image/{DishImageCachePolicy,FileDishImageCache,
MemoryDishImageCache,AndroidDishImageCache}.kt`, `ui/**`, `data/RecipePhotoAssets.kt`,
`data/AssetRecipePhotoSource.kt`, `MainActivity` (más allá de una línea de construcción), la Taxonomía,
`seed-recipes.json`, `init.sh`, `gradle/libs.versions.toml` y `AndroidManifest.xml` **no** cambian; sin
dependencias nuevas; `docs/design/concepts/dish-image-seed/` **no** existe; y el **único** fichero nuevo
con `android.*` es `data/AssetDishImageSeedSource.kt`.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-image-seed`, líneas 1116-1130) — metadata, verificación literal y la
  nota "resolución por clave canónica delante de la cadena de categoría".
- `docs/ai-dish-images-proposal.md` — §2 (cadena objetivo), §5 (las **tres capas** de caché: memoria,
  disco propio y **assets semilla** "preparados una vez y versionados", coste que eliminan: "incluso la
  **primera** generación"; clave = `dishKey` canónico; misma referencia a `scripts/fetch-recipe-photos.sh`),
  §6 (no hacer esperar, fallo silencioso), §8 (cuota), §10 (fase 7) y §11.4 (pregunta abierta del set
  semilla, resuelta por esta feature).
- `docs/adr/0005-ai-dish-images.md` — decisiones 1-4 (request, fallo `null`, prompt, clave versionada),
  "Pendiente: fase 7" y la regla durable de invalidación (`VERSION`).
- `docs/adr/0004-recipe-photos-as-assets.md` — convención de assets versionados, cadena de respaldo,
  presupuesto de tamaño y "el script es una herramienta de una sola vez fuera de `init.sh`".
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — el punto exacto de inserción
  (`memoria → disco → generador → persistir`, `Mutex` por clave, `CancellationException` relanzada);
  se le **añade** un parámetro nuevo al final con default `null`.
- `app/src/main/java/com/sazon/app/data/image/{FileDishImageCache,DishImageCachePolicy}.kt` — "el nombre
  de fichero **es** la clave canónica" (`<cacheKey>.webp`) y la escritura atómica; el nivel semilla
  **no** las usa (los assets ya están en el APK).
- `app/src/main/java/com/sazon/app/data/RecipePhotoAssets.kt` — patrón directo: **lógica pura** de
  imagen en `data` (sin Android ni I/O), raíz + grupos, `EXTENSIONS = [webp, jpg, png]` y `resolve(…,
  exists)` con predicado inyectado.
- `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` — patrón de **costura Android**
  (`context.assets.open(path).use { … }` en `Dispatchers.IO`, `null` sin lanzar, LRU de bitmaps).
- `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` — la clave canónica versionada
  (`dish-image-v1-<slug>`, alfabeto `[a-z0-9-]`), que es **el nombre del asset**.
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` — `generatedIfEnabled` (gate del
  interruptor en el punto de inyección) y `GatedDishImageGenerator` (tope en el borde de generación): la
  interacción con el nivel semilla queda acotada por el gate (decisión 6).
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 135-157) — composición de `DishImageResolver`
  y `DishImageArtworkLoader`; es donde se pasa la fuente semilla real.
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkLoader.kt` — el consumidor del resolutor
  (LRU de decodificadas por clave + `Semaphore` de 2): el nivel semilla no lo cambia ni añade
  concurrencia nueva.
- `app/src/test/java/com/sazon/app/data/image/DishImageResolverTest.kt` — patrón de test con directorio
  temporal y generador falso (claves `dish-image-v1-arroz-con-pollo-criollo` y
  `dish-image-v1-paella-valenciana` ya son sus vectores).
- `app/src/test/java/com/sazon/app/data/image/AndroidDishImageCacheTest.kt` — patrón Robolectric
  `@Config(sdk = [33])` + `ApplicationProvider` para la costura Android.
- `app/build.gradle.kts` (líneas 13-15 y 124-130) — `systemProperty("recipe.photos.assets.dir", …)` ya
  publica un directorio de assets a los tests: mismo mecanismo para el set semilla.
- `app/src/main/java/com/sazon/app/data/ai/image/Wire/Prompt de la fase 2/6`:
  `DishImagePolicy` (`IMAGE_SIZE = 1024x1024`, `TARGET_WIDTH_PX = 960`, `WEBP_QUALITY = 80`,
  `MODEL_ALIAS = image`, `EDIT_MODEL_ALIAS = image-edit`, `EDIT_FALLBACK_STATUSES`) y
  `DishImagePrompt` (`PROMPT_PREFIX`, `DEFAULT_DISH`, `STYLE_SUFFIX`) — contrato que el script **espeja**.
- `app/src/main/assets/img/recipes/default.jpg` — la referencia de estilo neutra que el script usa en el
  camino `/images/edits` (fase 6).
- `scripts/fetch-recipe-photos.sh` — **patrón del script**: `set -euo pipefail`, `--force`/`--help`,
  claves esperadas **en orden** y validadas, extensión real por *magic bytes*, conversión a WebP q80
  ≤960 px si hay herramienta, inventario final, presupuesto de tamaño, exit ≠ 0 ante cualquier desvío y
  **fuera** de `init.sh`.
- `docs/domain/taxonomy.json` — `cuisines` (9) con sus `label` en español: el script resuelve la
  etiqueta de cocina del prompt desde **esta** fuente, como la app.
- `docs/technical-discovery.md` (tabla **Data and Storage**, líneas 29-42, y bullets de las fases 1-6) —
  dónde se documenta la frontera durable de la imagen de plato; el punto 3 de §8 y §10 del ADR.
- `docs/risks-and-open-questions.md` (línea 144) — el riesgo de cuota con "pendiente solo el set semilla
  de platos frecuentes (fase 7)".
- `docs/domain/taxonomy.md` §6 — la estrategia visual vigente (cadena generada delante de la cadena de
  assets), que esta fase amplía con un nivel.
- `docs/specs/dish-image-cache.md`, `docs/specs/dish-style-reference.md` y
  `docs/specs/dish-image-domain.md` — nivel de detalle, decisiones y estilo de las fases previas.

### Existing Patterns To Follow

- **Lógica pura en `data` + costura Android aislada**: `RecipePhotoAssets` (puro, `exists` inyectado) y
  `AssetRecipePhotoSource` (Android, `null` sin lanzar). El set semilla replica exactamente esa pareja.
- **Resolución por clave canónica como nombre**: la clave ya es apta como nombre de fichero/asset
  (`[a-z0-9-]`, versionada) — no hay rehash, índice ni campo nuevo de dominio.
- **Degradación silenciosa**: `null` = no disponible, excepción nunca; `CancellationException`
  relanzada.
- **Política/constantes en un único sitio**, puras y testeables en JVM.
- **Tests JUnit 4** en `app/src/test/**`, nombres en español con `_`, casos negativos que **no** pasan en
  vacío, Robolectric `@Config(sdk = [33])` solo para la costura Android, y assets leídos por
  `System.getProperty(...)` publicado desde `app/build.gradle.kts`.
- **Script de una sola vez** (patrón `fetch-recipe-photos.sh`): idempotente, validado, con inventario y
  presupuesto, **fuera** de `init.sh`.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo.

### Current Gaps

- **No existe ningún nivel "semilla"**: hoy la primera imagen de un plato `ai-*` siempre cuesta red,
  cuota y ~4,2 s (`data.ai.image` vía el generador); los assets de `recipe-photos` solo resuelven por
  `id`/`categoryId`, nunca por la clave canónica del plato.
- **No hay assets de plato concretos**: `app/src/main/assets/img/` solo tiene `recipes/` (12 `seed/`,
  18 `category/`, `default`); no hay `img/dishes/seed/`.
- **No hay script de preparación del set**: el único script de assets es `fetch-recipe-photos.sh`
  (láminas de Stitch de `recipe-photos`).
- **La lista de "platos más buscados del demo" no está documentada en ningún sitio del repo**; esta spec
  propone una lista inicial de 6 (ver "Decisions And Unknowns") y fija el **mecanismo** con independencia
  de su cardinalidad.

## Technical Approach

### 1. Dónde vive

- Paquete `com.sazon.app.data` (junto a `RecipePhotoAssets`/`AssetRecipePhotoSource`):
  `DishImageSeedAssets.kt` (**puro**), `DishImageSeedSource.kt` (seam `fun interface`,
  Kotlin/JDK) y `AssetDishImageSeedSource.kt` (**Android**, único fichero nuevo con `android.*`).
- `com.sazon.app.data.image.DishImageResolver` — **modificado** (un parámetro nuevo y un nivel).
- `com.sazon.app.MainActivity` — **modificado** (una línea: pasar la fuente semilla real).
- `scripts/prepare-dish-image-seed.sh` — **nuevo** (herramienta de una sola vez).
- `app/src/main/assets/img/dishes/seed/` — **nuevo** (los assets versionados).

### 2. Convención de assets (nueva)

| Grupo | Ruta | Clave | Nº |
|---|---|---|---|
| Foto preparada de un plato frecuente | `img/dishes/seed/<cacheKey>.<ext>` | `DishImageCacheKey.of(título)` | 1 por plato de la lista |

- `<ext>` ∈ `webp`, `jpg`, `png`, probados **en ese orden** (`RecipePhotoAssets.EXTENSIONS`); la
  extensión real la decide el script por *magic bytes*.
- Los ficheros son **fuente versionada** (como `seed-recipes.json` y `img/recipes/**`), **no** se añaden
  a `.gitignore`, y `.gitattributes` ya los cubre como binarios.
- **Nunca** se empaqueta un asset para un plato que ya tenga foto propia (las **12 recetas semilla**: su
  foto vive en `img/recipes/seed/<recipeId>.<ext>`), para no pisar una foto propia con otra distinta.

### 3. Mapa puro: `DishImageSeedAssets.kt`

```kotlin
object DishImageSeedAssets {
    const val ROOT = "img/dishes/seed"                 // raíz dentro de assets/
    val EXTENSIONS = RecipePhotoAssets.EXTENSIONS      // [webp, jpg, png]
    val SAFE_KEY = Regex("[a-z0-9-]+")                 // alfabeto canónico (sin `..`, `/`, mayúsculas)

    fun isSafeKey(cacheKey: String): Boolean
    fun path(cacheKey: String, extension: String): String?          // null si !isSafeKey
    fun resolve(cacheKey: String, exists: (String) -> Boolean): String?  // primera extensión existente
}
```

- **Puro**: sin Android, sin Compose, sin I/O; `exists` **inyectado** (test JVM: fichero en disco; app:
  asset). Determinista y sin estado.
- `resolve` recorre `EXTENSIONS` en orden y devuelve la primera ruta completa existente; `null` si
  ninguna existe o la clave no es segura.
- `SAFE_KEY` es **defensa en profundidad** (no la fuente de verdad de la canonicalización, que es
  `DishImageCacheKey`): una clave fabricada a mano con `..`/`/` devuelve `null` en vez de una ruta fuera
  de `ROOT` (recoge el candidato Low de `dish-image-cache`).

### 4. Seam y costura Android

```kotlin
/** Bytes empaquetados del plato por su clave canónica; `null` = no hay asset. Nunca lanza. */
fun interface DishImageSeedSource {
    suspend fun bytesFor(cacheKey: String): ByteArray?
}
```

```kotlin
class AssetDishImageSeedSource(
    private val context: Context,
    private val root: String = DishImageSeedAssets.ROOT,   // inyectable para el test "no existe"
) : DishImageSeedSource {
    override suspend fun bytesFor(cacheKey: String): ByteArray?   // Dispatchers.IO, best-effort
}
```

- `bytesFor` resuelve la ruta con `DishImageSeedAssets.resolve(cacheKey, ::exists)` y la lee con
  `context.assets.open(path).use { it.readBytes() }` en `Dispatchers.IO`.
- `null` si no hay asset, la lectura falla o los bytes están vacíos. **Nunca** lanza.
- Sin LRU propia: el acierto lo cachea el resolutor (memoria) y el loader (decodificadas), así que no
  hay que duplicar caché.

### 5. Nivel nuevo en `DishImageResolver` (modificado)

Constructor (**aditivo**, el parámetro nuevo va **al final** con default para no tocar ningún llamante):

```kotlin
class DishImageResolver(
    private val disk: FileDishImageCache,
    private val memory: MemoryDishImageCache,
    private val generator: DishImageGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val seed: DishImageSeedSource? = null,   // fase 7; null = comportamiento de la fase 3
)
```

`imageFor(request)` (solo cambia el interior del cerrojo):

1. **Memoria** (`memory.get(cacheKey)`) — igual que hoy.
2. `Mutex` por clave y, dentro: re-consulta memoria → `disk.read(cacheKey)` en
   `withContext(ioDispatcher)` → si hay copia, a memoria y se devuelve (**el disco gana a la semilla**).
3. **Semilla** (`seedOrNull(cacheKey)`): si devuelve bytes no vacíos, se guardan en **memoria** y se
   devuelven **sin** escribir en disco y **sin** invocar al generador.
4. **Generador** → persistir (best-effort) → memoria → devolver — **exactamente** como hoy.

`seedOrNull` replica la política de fallo del generador: `CancellationException` se **relanza**,
cualquier otra `Exception` se degrada a `null` (la cadena sigue).

**Por qué en el resolutor y no en la cadena de `recipe-photos`**: (a) la identidad de un asset semilla es
la **clave canónica del plato**, que ya viaja en el `DishImageRequest` que consume el resolutor
(`RecipePhotoSource` se keya por `Recipe`, no por clave); (b) el nivel semilla es la **tercera capa de la
misma caché** (propuesta §5) y se inserta donde estaba el generador, dejando la cadena local como
respaldo intacta; (c) no hay decodificación ni Compose en `data`.

**Por qué no se persiste en `cacheDir`**: los bytes ya viajan en el APK; copiarlos consumiría el
presupuesto de 80 MiB y podrían ser evictados por la LRU o borrados con `cacheDir` por el sistema,
perdiendo el beneficio. Leer del asset es determinista y siempre disponible.

**Interacción con el ajuste de sesión**: la fuente semilla queda **detrás** del mismo gate de la fase 4
(el proveedor del seam usa `generatedIfEnabled`), así que con "Imágenes del plato" **apagado** la app
muestra exactamente la foto local (tampoco las semilla). La semilla **no** consume tope (solo lo hace el
borde de generación) y no toca la red.

### 6. Cableado: `MainActivity`

Una línea más, con la misma disciplina *best-effort* del resto:

```kotlin
val dishImageResolver = DishImageResolver(
    disk = FileDishImageCache(AndroidDishImageCache.root(applicationContext)),
    memory = MemoryDishImageCache(),
    generator = GatedDishImageGenerator(/* … igual que hoy … */),
    seed = AssetDishImageSeedSource(applicationContext),   // fase 7: assets versionados por clave
)
```

### 7. Script de preparación (`scripts/prepare-dish-image-seed.sh`)

**Herramienta de una sola vez**, con el patrón de `fetch-recipe-photos.sh`: `set -euo pipefail`,
`--force`/`--help`, validación **exacta** de la lista de claves, *magic bytes*, conversión a WebP q80
≤960 px si hay `cwebp`/`magick`/`convert`, inventario final, presupuesto y exit ≠ 0 ante cualquier
desvío. **No** entra en `init.sh` y **ningún** código de runtime lo invoca.

- **Lista autoritativa** en el script: `EXPECTED_SEED_KEYS` (ordenada, claves canónicas) y, por clave, el
  **plato** y la **cocina** (`cuisineId`); el prompt se compone **espejando** `DishImagePrompt`
  (`<PROMPT_PREFIX> <plato>[, <etiqueta de cocina>], <STYLE_SUFFIX>`), con la etiqueta de cocina leída de
  `docs/domain/taxonomy.json` (misma fuente que la app).
- **Modo por defecto (proveedor)**: lee `devexpert.apiKey` de `local.properties` (nunca la imprime;
  falla con mensaje claro si falta) y, por plato, **genera la imagen una vez**:
  - `POST /images/edits` (multipart: `model=image-edit`, `prompt`, `size=1024x1024`, `image=@` la foto
    neutra `img/recipes/default.<ext>`) con `Authorization: Bearer`, espejando la fase 6;
  - **respaldo** a `POST /images/generations` (`model=image`) solo ante rechazo definitivo
    (`400/404/405/413/415/422`) o `2xx` inutilizable; transporte/timeout/`401/403/429/5xx` → fallo del
    plato (**sin** tercera llamada);
  - decodifica `data[0].b64_json` (`jq` + `base64 -d`), detecta la extensión por *magic bytes*,
    convierte a WebP q80 ≤960 px (si no hay herramienta, conserva el original y lo registra) y escribe
    `app/src/main/assets/img/dishes/seed/<cacheKey>.<ext>`.
- **Modo alternativo (sin cuota) `--source <dir>`**: importa imágenes ya obtenidas, una por plato
  nombrada `<cacheKey>.<ext>`, y ejecuta la **misma** validación+empaquetado sin llamar al proveedor.
  Vía documentada para obtenerlas: ejecutar una vez los platos de la demo con la app y copiar
  `cacheDir/dish-images/` (cuyos nombres **ya son** las claves canónicas).
- **Idempotencia**: si el asset ya existe y no se pasa `--force`, se omite y se registra.
- **Validación final**: exactamente `EXPECTED_SEED_KEYS` (conteo + nombres + `[a-z0-9-]`), extensión ∈
  `{webp,jpg,png}`, cada fichero no vacío, total ≤ **2 MiB**; cualquier desvío → exit ≠ 0.
- **Nunca inventa assets**: si no hay clave, red o el proveedor rechaza, el script falla; la sesión se
  detiene y lo reporta **bloqueado** (mismo criterio que `recipe-photos`).

### 8. Frontera con lo ya aceptado (no tocar)

`DishImageCacheKey.VERSION` sigue `dish-image-v1` porque **ni la normalización ni la plantilla del
prompt** cambian (el nivel semilla solo **añade** un origen de bytes). Subir `VERSION` (p. ej. por un
cambio de `STYLE_SUFFIX`) cambia **todas** las claves y, con ellas, los **nombres de los assets semilla**:
es el disparador documentado para **volver a preparar el set**.

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/DishImageSeedAssets.kt` — crear; mapa puro clave→asset
  (`ROOT`, `EXTENSIONS`, `SAFE_KEY`, `isSafeKey`, `path`, `resolve`).
- `app/src/main/java/com/sazon/app/data/DishImageSeedSource.kt` — crear; seam `fun interface`
  (bytes por clave, `null`, sin lanzar).
- `app/src/main/java/com/sazon/app/data/AssetDishImageSeedSource.kt` — crear; lectura Android de los
  assets en `Dispatchers.IO` (único fichero nuevo con `android.*`).
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — modificar; parámetro `seed` al
  final (default `null`) y el nivel semilla entre el disco y el generador.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar; pasar
  `seed = AssetDishImageSeedSource(applicationContext)`.
- `scripts/prepare-dish-image-seed.sh` — crear; preparación de una sola vez (proveedor o `--source`),
  validada, idempotente y **fuera** de `init.sh`.
- `app/src/main/assets/img/dishes/seed/<cacheKey>.<ext>` — crear (uno por plato de la lista);
  **assets versionados**.
- `app/build.gradle.kts` — modificar **solo** para publicar
  `systemProperty("dish.image.seed.assets.dir", …)` a los tests JVM (mismo patrón que
  `recipe.photos.assets.dir`).
- `app/src/test/java/com/sazon/app/data/DishImageSeedAssetsTest.kt` — crear; escenarios 1, 5, 8 y 10.
- `app/src/test/java/com/sazon/app/data/AssetDishImageSeedSourceTest.kt` — crear; escenario 9
  (Robolectric `@Config(sdk = [33])`).
- `app/src/test/java/com/sazon/app/data/image/DishImageResolverTest.kt` — modificar; escenarios 2-7 con
  una fuente semilla falsa (contador de lecturas) y el generador falso ya existente.
- `docs/adr/0005-ai-dish-images.md` — modificar; estado "Aceptado parcialmente (fases 1-7)" y alcance de
  la fase 7 (nivel semilla por clave canónica, assets versionados, script de una sola vez).
- `docs/technical-discovery.md` — modificar; fila nueva en **Data and Storage**, el nivel semilla del
  resolutor, la ubicación/tests y que la preparación es de una sola vez.
- `docs/domain/taxonomy.md` §6 — modificar; la cadena generada gana el nivel de assets semilla.
- `docs/risks-and-open-questions.md` — modificar; el set semilla deja de estar pendiente y se registra su
  coste **de preparación** (una vez) frente al ahorro en demo.
- `PROGRESS.md` y `feature_list.json` — modificar al implementar/validar (estado, evidencia); **no** los
  toca el planificador.

**No se toca**: `domain/**`, `data/ai/image/**`, `data/image/**` salvo `DishImageResolver.kt`,
`ui/**`, `data/RecipePhotoAssets.kt`, `data/AssetRecipePhotoSource.kt`, `Recipe.kt`,
`RecipeValidator.kt`, `gradle/libs.versions.toml`, `init.sh`, `scripts/gradle.sh`, `.gitattributes`,
`AndroidManifest.xml`, `app/src/main/assets/img/recipes/**`, `seed-recipes.json`,
`docs/domain/taxonomy.json`, `DESIGN.md`, `docs/design/**` ni las specs ya aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: data`; no hay pantalla, componente Compose, microcopy, icono ni estado
  visual nuevos; lo único visual es una **foto de contenido** que se pinta por el seam ya aceptado).
- Design source: no aplica. Las fotos semilla siguen la dirección visual **ya aceptada** en `DESIGN.md`
  y `recipe-photos` (la foto del plato como protagonista); esta feature **no** cambia esa dirección.
- Screens or states affected: ninguna (misma geometría, mismo crossfade, mismo fallback: solo cambia de
  dónde salen los bytes para ciertos platos).
- New design artifact required: no.
- **El gate de capturas de UI no aplica**: **no** se crea `docs/design/concepts/dish-image-seed/` ni
  ninguna otra carpeta en `docs/design/concepts/`. El set semilla es **contenido de datos** (assets
  versionados con nombre = clave canónica), no un concepto de pantalla; su calidad visual se revisa
  contra las fotos ya aceptadas de `recipe-photos` y `DESIGN.md`.

## Durable Documentation Impact

- `docs/adr/0005-ai-dish-images.md`: **update** — pasa a "Aceptado parcialmente (**fases 1-7**)": se
  registra la decisión de la fase 7 (nivel de assets semilla por clave canónica delante del generador,
  assets versionados en `assets/img/dishes/seed/`, preparación por script de una sola vez, sin persistir
  en `cacheDir`, sin subir `VERSION`) y que la lista de fases de la propuesta queda **completa**.
- `docs/technical-discovery.md`: **update** — frontera durable nueva: la **tabla Data and Storage** gana
  la fila de las fotos semilla de plato (`img/dishes/seed/<cacheKey>.<ext>`, resueltas por
  `data.DishImageSeedAssets` y leídas por `data.AssetDishImageSeedSource`, sin red y sin persistencia) y
  el bullet de la fase 7 describe el nivel del resolutor, el script de una sola vez y los tests JVM.
- `docs/domain/taxonomy.md` §6: **update** — la resolución de la imagen de plato gana el nivel de
  **assets semilla** delante del proveedor (memoria → disco → semilla → generador → cadena local).
- `docs/risks-and-open-questions.md`: **update** — la fila de cuota/latencia deja de tener "pendiente el
  set semilla": se registra que su **preparación** cuesta ~2 % de cuota por plato **una sola vez** (fuera
  del gate, y evitable con `--source`) y que esos platos ya **no** gastan cuota ni esperan red en runtime.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; la foto sigue
  resolviéndose por convención (no es campo de `Recipe`) y el set no se persiste como entidad.
- `docs/ai-dish-images-proposal.md`: **not needed** — es la propuesta (documento no autoritativo); §11.4
  queda resuelto y registrado en el ADR 0005, que es la fuente de verdad del estado de las fases.
- `docs/design/**`: **not needed** — el gate de capturas no aplica (no es UI).
- `DESIGN.md`: **not needed** — sin UI, sin microcopy y sin cambio de dirección visual.
- `docs/build-brief.md`: **not needed** — no cambia el alcance del MVP (la ampliación de imágenes ya está
  registrada por `recipe-photos`).
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo (el script nuevo queda
  fuera de `init.sh`, como `fetch-recipe-photos.sh`).
- `.gitattributes`: **not needed** — `*.webp`/`*.jpg`/`*.jpeg`/`*.png binary` ya están.
- `ARCHITECTURE.md` / `CONSTRAINTS.md`: **not needed** — no existen en este repositorio (misma decisión
  del harness que registraron las specs previas).

## Implementation Plan

1. Crear `DishImageSeedAssets` (puro) y hacer pasar `DishImageSeedAssetsTest` en la parte de mapa
   (clave→ruta, orden de extensiones, clave insegura → `null`, determinismo, clave fuera del set → `null`).
2. Crear `DishImageSeedSource` + `AssetDishImageSeedSource` y su test Robolectric (asset real, clave
   desconocida, `root` inexistente, sin lanzar).
3. Escribir `scripts/prepare-dish-image-seed.sh` (lista, prompt espejo, modo proveedor con respaldo,
   `--source`, idempotencia, validación, inventario y presupuesto).
4. Ejecutarlo **una vez** (o con `--source`) y empaquetar los assets; comprobar el inventario y ≤2 MiB;
   completar el test de assets reales con `dish.image.seed.assets.dir`.
5. Añadir el nivel semilla a `DishImageResolver` y ampliar `DishImageResolverTest` (semilla gana al
   generador, no escribe en disco, acierto de memoria, precedencia con el disco, casos negativos y
   cancelación).
6. Cablear `MainActivity` (`seed = AssetDishImageSeedSource(applicationContext)`) y publicar la
   `systemProperty` en `app/build.gradle.kts`.
7. Inspeccionar alcance y pureza (greps del Verification Plan), `assembleDebug` + `unzip -l` para
   comprobar que los assets viajan en el APK.
8. Actualizar el ADR 0005, `docs/technical-discovery.md`, `docs/domain/taxonomy.md` §6 y
   `docs/risks-and-open-questions.md`.
9. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `DishImageSeedAssets` con `ROOT = "img/dishes/seed"`, `EXTENSIONS` (de `RecipePhotoAssets`),
      `SAFE_KEY = [a-z0-9-]+`, `isSafeKey`, `path` y `resolve(cacheKey, exists)`.
- [ ] Crear `DishImageSeedSource` (`fun interface`, `suspend bytesFor(cacheKey): ByteArray?`) y
      `AssetDishImageSeedSource(context, root)` (assets en `Dispatchers.IO`, `null` sin lanzar).
- [ ] Modificar `DishImageResolver`: parámetro `seed: DishImageSeedSource? = null` **al final** y el
      nivel `memoria → disco → semilla → generador` (la semilla a memoria y a la respuesta, **sin**
      `disk.write`), con `CancellationException` relanzada y `Exception` → `null`.
- [ ] Cablear `MainActivity` con `seed = AssetDishImageSeedSource(applicationContext)` (una línea, sin
      tocar el gate del interruptor ni el tope).
- [ ] Publicar `systemProperty("dish.image.seed.assets.dir", …)` en `app/build.gradle.kts`.
- [ ] Escribir `scripts/prepare-dish-image-seed.sh` (claves esperadas ordenadas + plato/cocina, prompt
      espejo de `DishImagePrompt` con la etiqueta de `docs/domain/taxonomy.json`, modo proveedor
      `/images/edits` con respaldo acotado por estado, modo `--source <dir>`, `--force`/`--help`,
      *magic bytes*, WebP q80 ≤960 px, inventario, ≤2 MiB, exit ≠ 0 ante desvío).
- [ ] Ejecutar el script **una vez** y empaquetar `app/src/main/assets/img/dishes/seed/` (uno por plato);
      registrar el inventario, las extensiones y el total.
- [ ] Escribir `DishImageSeedAssetsTest`: mapa clave→ruta (orden `webp→jpg→png`), clave insegura/`null` →
      `null`, determinismo, clave ajena al set → `null`, y **para cada plato de la lista** que
      `DishImageCacheKey.of(título)` resuelve un fichero **existente y no vacío** bajo
      `dish.image.seed.assets.dir` (verificación exigida #1).
- [ ] Escribir `AssetDishImageSeedSourceTest` (Robolectric `@Config(sdk = [33])`): bytes reales no
      vacíos de un plato del set, clave desconocida → `null`, `root` inexistente → `null`.
- [ ] Ampliar `DishImageResolverTest`: la semilla gana al generador (`0` llamadas), **no** escribe en la
      caché, la segunda resolución no relee el asset, el disco gana a la semilla, `seed = null` mantiene
      el comportamiento de la fase 3, `null`/vacío/excepción de la fuente degradan sin romper y
      `CancellationException` se propaga.
- [ ] Verificar por inspección que `domain/image/**`, `data/ai/image/**`, la caché, `ui/**`, la cadena de
      `recipe-photos` y `seed-recipes.json` **no** cambian; sin dependencias nuevas; sin
      `docs/design/concepts/dish-image-seed/`.
- [ ] Actualizar el ADR 0005, `docs/technical-discovery.md`, `docs/domain/taxonomy.md` §6 y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde y registrar la evidencia.
- [ ] Actualizar `feature_list.json` (a `passing` con evidencia) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red, **no** gasta cuota y
  **no** modifica `init.sh`.
- Focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.DishImageSeedAssetsTest"
  --tests "com.sazon.app.data.AssetDishImageSeedSourceTest" --tests "com.sazon.app.data.image.*"` →
  `BUILD SUCCESSFUL`.
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. El implementer **re-deriva** la baseline en su HEAD (la última baseline aceptada
  conocida es **88 suites / 643 tests**, sesión 095) y confirma que el delta es exactamente el de las
  suites nuevas/ampliadas.
- `bash scripts/gradle.sh lint` → **0 errores** (los 13 warnings de `GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion` y `OldTargetApi` son preexistentes, todos en el
  build/version catalog, **ninguno** bajo `app/src`).
- Inspección de alcance y pureza:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/data` → `AssetDishImageSeedSource.kt`
    (y los ya existentes `AssetRecipePhotoSource`/`AssetSeedRecipeSource`/`AssetTaxonomySource`); en
    `app/src/main/java/com/sazon/app/data/image` sigue **solo** `AndroidDishImageCache.kt`.
  - `grep -rn "prepare-dish-image-seed" init.sh` → **0**; `grep -rn "prepare-dish-image-seed" app/src`
    → **0** (el script no corre en runtime).
  - `git diff --name-only` solo con los ficheros previstos; `test -d docs/design/concepts/dish-image-seed`
    → **no existe**.
  - `git check-ignore app/src/main/assets/img/dishes/seed/<clave>.webp` → **no** ignorado.
- Script (idempotencia y validación): segunda ejecución sin `--force` → omite lo existente y registra
  `= …`; inventario final con **una clave por plato**, extensión ∈ `{webp,jpg,png}` y total ≤ 2 MiB.
- `bash scripts/gradle.sh assembleDebug` + `unzip -l app/build/outputs/apk/debug/app-debug.apk` →
  los assets del set presentes bajo `assets/img/dishes/seed/`.
- **Sin red** (verificación exigida #3): se cubre en **JVM** con el generador falso
  (`DishImageResolverTest`): un plato del set devuelve los bytes del asset con **0** llamadas al
  generador; con `seed = null` (o clave fuera del set) la cadena es la de la fase 3. Sin emulador.
- **Prueba manual de la persona** (tras el merge, no bloqueante): abrir en el emulador un plato del set
  en Resultados/Detalle y comprobar que muestra su foto (no la genérica de su categoría) **sin red** y
  que, con "Imágenes del plato" apagado, se ve la foto local como hasta ahora.
- Cobertura E2E: **no aplica**. No hay suite E2E persistente y `AGENTS.md` no automatiza UI de emulador;
  el contrato verificable (mapa clave→asset, nivel semilla del resolutor, lectura de assets y
  degradación) se cubre en JVM (+Robolectric para la costura).
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de las tiradas focales.
- Conteo parseado del XML (suites/tests, 0 fallos/errores/saltados) con las suites nombradas
  (`DishImageSeedAssetsTest`, `AssetDishImageSeedSourceTest`, `DishImageResolverTest`) y la baseline
  re-derivada.
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Inventario del set: una clave por plato, extensión usada por fichero, bytes totales (≤2 MiB) y salida
  del script (incluida la segunda pasada idempotente).
- Vectores concretos: `DishImageCacheKey.of("Arroz con pollo criollo") ==
  "dish-image-v1-arroz-con-pollo-criollo"` resuelve
  `img/dishes/seed/dish-image-v1-arroz-con-pollo-criollo.<ext>` existente y no vacío;
  `"../../etc/passwd"` y `""` → `null`; `resolve` con `exists` falso en `webp` y verdadero en `jpg` →
  la ruta `.jpg`; semilla → **0** llamadas al generador y **0** ficheros en la raíz de la caché; disco
  gana a semilla; `seed = null` → comportamiento de la fase 3.
- Resultado de los greps/diff de inspección, `test -d docs/design/concepts/dish-image-seed` y
  `git check-ignore`.
- `unzip -l` del APK de debug mostrando `assets/img/dishes/seed/**`.
- Anotación en `feature_list.json` (evidencia + `passing`) y `PROGRESS.md`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **El nivel semilla vive en `DishImageResolver`, entre el disco y el generador.** Es la **tercera capa
   de la caché** de la propuesta §5 ("assets semilla: platos frecuentes preparados una vez"), su
   identidad es la **clave canónica** que ya viaja en el `DishImageRequest`, y así la cadena local
   (`recipe-photos`) queda **intacta** como respaldo. *Alternativa descartada*: insertar un nivel en
   `ui.artwork.RecipePhotoSource` (keyed por `Recipe`, no por clave) o en `RecipePhotoAssets` (keyed por
   `recipeId`/`categoryId`): obligaría a duplicar el cálculo de la clave en la capa de assets y a tocar
   un seam Compose aceptado.
2. **El disco gana a la semilla** (`memoria → disco → semilla → generador`). En la práctica un plato del
   set **nunca** tiene copia en disco (no se llega a generar), así que el orden solo decide el caso
   degenerado (una copia generada antes de existir el asset); preferimos no cambiar la semántica de
   acierto de la fase 3 y no releer assets cuando ya hay copia. *Alternativa*: semilla antes del disco
   (uniformidad garantizada) — descartada por el motivo anterior; el caso se cubre por test para que la
   precedencia quede fijada.
3. **La semilla no se persiste en `cacheDir`.** Escribir en la caché duplicaría bytes que ya viajan en el
   APK, consumiría el tope de 80 MiB y podría perderlos por evicción LRU o por un borrado del sistema
   (`cacheDir` es descartable); leer del asset es determinista y siempre disponible. Sí se cachean los
   bytes en la **memoria** del resolutor (y las decodificadas en el loader).
4. **`seed` es un parámetro nuevo al final con default `null`.** Mantiene el comportamiento de la fase 3
   sin fuente semilla (tests y llamantes intactos) y convierte la fase 7 en **aditiva**. *Alternativa
   descartada*: un decorador de `DishImageGenerator` (la semilla no es generación: no debe consumir
   tope ni pasar por el borde de cuota, y devolver bytes del decorador haría que el resolutor los
   **persistiera**, justo lo que evita la decisión 3).
5. **Convención `img/dishes/seed/<cacheKey>.<ext>` y extensión probada en orden `webp → jpg → png`.** La
   clave canónica ya es un nombre de fichero válido; no hace falta índice ni campo de dominio. Se
   reutiliza `RecipePhotoAssets.EXTENSIONS` para no fijar el formato (el script puede haber convertido a
   WebP o no, según la herramienta disponible).
6. **Saneado de la clave en el mapa puro** (`[a-z0-9-]+`): una clave fabricada a mano no puede producir
   una ruta fuera de `ROOT` (recoge el hallazgo Low de `dish-image-cache` en la capa que sí toca el
   sistema de ficheros de assets).
7. **El set semilla se prepara con un script de una sola vez, fuera de `init.sh` y de runtime.** Patrón
   `fetch-recipe-photos.sh`: lista exacta validada, `--force`, *magic bytes*, WebP q80 ≤960 px, inventario,
   presupuesto ≤ 2 MiB y exit ≠ 0 ante cualquier desvío. El script **espeja** el contrato del proveedor
   (`/images/edits` con `img/recipes/default.<ext>` como referencia y respaldo acotado por estado a
   `/images/generations`) y el prompt (`DishImagePrompt`) para que el asset empaquetado sea **la misma
   imagen** que la app generaría. *Alternativa*: no llamar al proveedor y recibir siempre las imágenes
   por `--source` (menos código en bash, pero sin vía autónoma y reproducible dentro del repo); se
   ofrece como **modo** del mismo script.
8. **El set son platos frecuentes que NO tienen foto propia.** Las 12 recetas semilla ya resuelven su
   foto por `id` (`img/recipes/seed/<recipeId>.<ext>`); un asset semilla para su título **pisaría** esa
   foto propia con otra distinta (la semilla gana al generador, y el generado gana a la cadena local), así
   que se excluyen por regla.
9. **`DishImageCacheKey.VERSION` no sube.** Ni la normalización ni la plantilla del prompt cambian; el
   asset semilla solo añade un origen de bytes. Un cambio de `STYLE_SUFFIX` o de la normalización sigue
   siendo el disparador durable de invalidación (ADR 0005) y arrastra los **nombres** de los assets
   semilla: la consecuencia documentada es **volver a preparar el set**.
10. **`MainActivity` solo gana una línea.** El gate del interruptor (`generatedIfEnabled`) y el tope
    (`GatedDishImageGenerator`) no se tocan: la semilla queda detrás del mismo gate (apagado = solo la
    foto local) y **no** consume tope porque nunca llega al borde de generación.
11. **Sin capturas y sin `docs/design/concepts/dish-image-seed/`**: `area: data`; el set es contenido de
    datos, no dirección visual. Las fotos siguen la dirección ya aceptada por `recipe-photos`/`DESIGN.md`.
12. **Lista inicial de 6 platos frecuentes** (clave canónica ← título → cocina), **confirmable** por la
    persona y **ampliable** de forma aditiva (una línea en el script + un título en el test):

    | Clave canónica | Título del plato | `cuisineId` |
    |---|---|---|
    | `dish-image-v1-arroz-con-pollo-criollo` | `Arroz con pollo criollo` | `spanish` |
    | `dish-image-v1-paella-valenciana` | `Paella valenciana` | `spanish` |
    | `dish-image-v1-pizza-margarita` | `Pizza margarita` | `italian` |
    | `dish-image-v1-hamburguesa-casera` | `Hamburguesa casera` | `american` |
    | `dish-image-v1-tarta-de-chocolate` | `Tarta de chocolate` | `italian` |
    | `dish-image-v1-ensalada-cesar` | `Ensalada césar` | `american` |

    El **mecanismo** (mapa, nivel del resolutor, script y tests) **no** depende de la cardinalidad ni de
    los nombres concretos: el test deriva cada clave con `DishImageCacheKey.of(título)` (la
    canonicalización **real** de la fase 1), de modo que un desajuste entre el nombre del asset y la clave
    canónica hace **fallar** el test.

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **La lista del demo no está documentada** en el repo: la de la decisión 12 es una propuesta razonable
  (incluye el ejemplo de la propuesta, *arroz con pollo criollo*, y el vector ya usado en
  `DishImageResolverTest`, *paella valenciana*). Si la persona quiere otros platos, es cambiar la tabla
  del script y del test y volver a preparar; el resto no cambia.
- **Coste de preparación**: el modo proveedor gasta ~2 % de cuota **por plato** (una vez; para 6 platos,
  ~12 % de la cuota semanal) y no es puerta del gate; el modo `--source` evita ese gasto. A cambio, esos
  platos **no** vuelven a gastar cuota nunca (ni la primera vez).
- **Espejo del prompt y del wire en bash**: el script duplica la plantilla de `DishImagePrompt` y el
  contrato de `/images/edits`; una deriva es **cosmética** (la foto sigue siendo del plato) y queda
  acotada por la regla de `VERSION` (cambiar la plantilla obliga a subir versión y a regenerar el set).
  Si la validación considera el espejo demasiado, la salida es generar el set **por la app** una vez y
  usar `--source`.
- **El proveedor puede devolver una imagen poco apetitosa o equivocada** para un plato: se revisa en la
  prueba manual y se corrige con `--force` (o con una feature correctiva), sin bloquear el flujo.
- **Dependencia de `jq`/`base64`/`cwebp`-`magick`** en el script: si falta `jq`/`base64`, el script falla
  con mensaje claro; si no hay herramienta de conversión, conserva el original (jpg/png) y lo registra
  (el código prueba extensiones, así que no se bloquea nada).
- **Los assets semilla no entran en la LRU de disco**: si un plato del set se generara igualmente (p. ej.
  un `dishKey` distinto del descriptor, fase 5 sin entrega en runtime hoy), pagaría cuota y su copia de
  disco **tendría prioridad** sobre el asset (decisión 2). Aceptado y cubierto por test.
- **`VERSION` sube → el set queda huérfano**: los assets `dish-image-v1-*` dejan de resolverse; hay que
  volver a prepararlos (misma regla durable que la caché de disco). No se implementa limpieza.
- **Con el interruptor "Imágenes del plato" apagado no se ven las semilla** (decisión 10): es la
  semántica aceptada de la fase 4 ("solo la foto local"). Si la persona prefiere que las semilla se vean
  igualmente (son gratis y offline), es una feature de ajuste propia, no de esta fase.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: **sin** cambios en `domain/image/**`,
      `data/ai/image/**`, la caché de la fase 3 (`DishImageCachePolicy`, `FileDishImageCache`,
      `MemoryDishImageCache`, `AndroidDishImageCache`), `ui/**`, la cadena de `recipe-photos`,
      `seed-recipes.json`, la Taxonomía, `init.sh`, el lockfile ni `AndroidManifest.xml`; sin
      dependencias nuevas.
- [ ] Los 12 escenarios pasan, incluidos los negativos: clave insegura/ajena al set → `null` (nunca una
      ruta fuera de `ROOT`), la semilla gana al generador con **0** llamadas y **sin** escribir en la
      caché, el disco gana a la semilla, `seed = null` mantiene el comportamiento de la fase 3, y la
      fuente semilla que devuelve `null`/vacío/lanza no rompe la cadena (`CancellationException`
      relanzada).
- [ ] El mapa clave→asset es **puro** (sin Android, sin I/O, `exists` inyectado), determinista, prueba
      las extensiones `webp → jpg → png` en orden y sanea la clave con el alfabeto canónico `[a-z0-9-]`.
- [ ] Un test JVM (verificación exigida #1) deriva `DishImageCacheKey.of(título)` para **cada** plato de
      la lista y afirma que resuelve un asset **existente y no vacío** bajo
      `app/src/main/assets/img/dishes/seed/` (propiedad `dish.image.seed.assets.dir`), y que una clave
      fuera del set **no** resuelve nada.
- [ ] Los assets están **versionados** (uno por plato, extensión `webp`/`jpg`/`png`, no ignorados por
      git), el total ≤ 2 MiB, ninguno vacío y viajan en el APK de debug (`unzip -l`).
- [ ] El **script de preparación** es de una sola vez, idempotente (segunda pasada omite lo existente),
      valida la lista de claves y el presupuesto, sale ≠ 0 ante cualquier desvío, **no** está en
      `init.sh` y **ningún** código de runtime lo invoca.
- [ ] La semilla **no** consume el tope por sesión ni escribe en `cacheDir`, y el gate del interruptor y
      el tope de `dish-image-settings-toggle` **no** cambian.
- [ ] `DishImageCacheKey.VERSION` sigue `dish-image-v1` y el prompt/clave de las fases 1/5 son idénticos.
- [ ] El ADR 0005 (fases 1-7), `docs/technical-discovery.md` (tabla Data and Storage incluida),
      `docs/domain/taxonomy.md` §6 y `docs/risks-and-open-questions.md` están actualizados y coherentes.
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica; `area: data`).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR; si se ejecutó el modo proveedor, su coste de cuota queda documentado.
