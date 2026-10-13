# Feature Implementation Spec: Caché de imágenes de plato (disco + memoria)

## Source Feature

- `id`: `dish-image-cache`
- `area`: `data`
- `depends_on`: `["dish-image-generator"]` (satisfecha: `accepted`, sesión 090)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Implementar la **caché de la imagen de plato** (fase 3 de `docs/ai-dish-images-proposal.md` y de
`docs/adr/0005-ai-dish-images.md`): ficheros **WebP en `cacheDir`** cuyo **nombre de fichero es la clave
canónica** del plato (`DishImageCacheKey`, p. ej. `dish-image-v1-arroz-con-pollo-criollo.webp`), más una
**caché LRU en memoria**, y un **resolutor** que orquesta `memoria → disco → generador real → persistir`.
Con la misma clave canónica, una imagen generada una vez **no se vuelve a pedir al proveedor** (ni se
gasta cuota) aunque no haya red; el uso de disco queda acotado por un **tope de 80 MiB con evicción LRU**
de los ficheros más antiguos.

Es el **nivel que la fase 4 (`dish-image-progressive-ui`) insertará delante de la cadena de
`recipe-photos`**, que pasa a ser el respaldo: `imageFor(request)` devuelve bytes cuando hay imagen
generada y `null` cuando no la hay (sin red, sin copia, fallo), y el llamante conserva la cadena local
sin cambios. En **esta** fase la caché queda **sin consumidores ni cableado** (igual que el adaptador en
la fase 2): lo que se entrega es la capa de datos con su contrato, verificado en **JVM** con un generador
falso y directorios temporales, **sin gastar cuota y sin emulador**.

## Non-Goals

- **No hay UI ni cableado**: no se toca `ui.artwork.RecipeArtwork`, `ui.artwork.RecipePhotoSource`,
  `LocalRecipePhotoSource`, `MainActivity`/`SazonApp`/`MainShell` ni los puntos de uso. La **inserción
  real** del nivel generado delante de la cadena de assets, la carga progresiva, el shimmer/transición y
  los guardarraíles (interruptor de usuario, tope de generaciones por sesión, concurrencia limitada,
  cancelación al hacer scroll) son la **fase 4** (`area: ui`, con capturas obligatorias).
- **No se modifica la cadena local de `recipe-photos`**: `data.RecipePhotoAssets`,
  `data.AssetRecipePhotoSource` y `ui.artwork.RecipeArtwork*` quedan intactos; siguen siendo la **única**
  resolución de imagen en runtime hasta que la fase 4 monte el nivel nuevo delante.
- **No hay Room**: la decisión de la persona (2026-10-13) es **ficheros en `cacheDir`**, no índice en
  Room (resuelve la pregunta abierta §11.2 de la propuesta). No se crea ninguna base, tabla ni migración.
- **No se toca `domain.image`** (`DishImageGenerator`, `DishImageRequest`, `DishImagePrompt`,
  `DishImageCacheKey`), ni `data.ai.image` (`DevExpertDishImageGenerator`, `DishImagePolicy`,
  `DishImageReducer`), ni `Recipe`/`RecipeValidator`/la Taxonomía/`seed-recipes.json`.
- **No hay códec ni decodificación en esta capa**: la caché almacena **tal cual** los bytes WebP que ya
  produce la fase 2; no recodifica, no reduce, no genera miniaturas y **no** importa `android.graphics`.
  La LRU de **bitmaps decodificados** es de la fase 4 (UI), donde vive el pintado.
- **No se empaqueta un set semilla** de platos frecuentes (fase 7), **no** se usa `/images/edits`
  (fase 6) ni se añaden `dishKey`/`imagePrompt` al wire (fase 5).
- **No se añaden dependencias** ni se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/**` ni `AndroidManifest.xml`. Sin telemetría ni `Log`.

## Job Story

Cuando la app pida la imagen generada de un plato (una o varias veces, con o sin red),
quiero que la caché la recuerde por su clave canónica y la sirva desde memoria o disco,
para no volver a gastar cuota ni esperar ~4 s por la misma imagen, y para que el almacenamiento no
crezca sin límite.

## Users And Permissions

- **Desarrollador / agente**: puede ejercitar la caché con un `DishImageGenerator` falso y un directorio
  temporal; sin red, sin clave real, sin cuota y sin emulador (JVM).
- **Usuario local**: **no interactúa** todavía — no hay consumidor del resolutor, así que la app sigue
  pintando exactamente la cadena de assets de hoy y **no** se gasta cuota en runtime (se materializa en
  la fase 4).
- **Permisos**: ninguno nuevo (sin red, sin `INTERNET`, sin almacenamiento externo).

## Acceptance Scenarios

### Scenario 1: segunda petición con la misma clave → no llama al generador (memoria)

Given un `DishImageResolver` con memoria vacía, un `FileDishImageCache` sobre un directorio **temporal
vacío** y un generador falso que cuenta llamadas y devuelve bytes WebP no nulos,
When se llama dos veces a `imageFor(request)` con la misma `cacheKey`,
Then el generador se invoca **una sola vez**, las dos respuestas son **iguales** y no nulas, y en el disco
queda **un único** fichero con esa clave.

### Scenario 2: memoria fría → se sirve del disco y no llama al generador

Given un resolutor **nuevo** (memoria vacía) sobre el directorio donde el Scenario 1 ya escribió el
fichero,
When `imageFor(igual request)`,
Then el generador **no** se invoca y devuelve exactamente los bytes del fichero.

### Scenario 3: sin red (generador `null`) se sirve del disco; sin copia, `null` (respaldo)

Given disco con la imagen de una clave y un generador que devuelve `null` (equivalente a "sin red"), y
otra clave **sin** copia en disco,
When se resuelven ambas,
Then la primera devuelve los bytes del disco **sin invocar** al generador y la segunda devuelve `null`
sin lanzar y **sin** crear ningún fichero (el llamante conserva la cadena local de `recipe-photos`).

### Scenario 4: dos títulos equivalentes comparten fichero (clave canónica)

Given dos `DishImageRequest` cuyas `cacheKey` coinciden por construcción
(`DishImageCacheKey.of` de `"Arroz con Pollo Criollo"` y de `"  arroz  con   pollo criollo "`), y un
tercer request de otro plato,
When se resuelven los tres,
Then hay **un solo** fichero para los dos equivalentes, el generador se invoca **una vez** por clave
manualmente distinta y el tercer plato produce **otro** fichero.

### Scenario 5: fichero bajo la raíz inyectada, nombre = clave + `.webp`, contenido = bytes del generador

Given un directorio temporal como raíz,
When se resuelve una clave,
Then existe `File(root, "<cacheKey>.webp")`, sus bytes son **idénticos** a los que devolvió el generador
y empiezan por los *magic bytes* de WebP (`RIFF`…`WEBP`); la caché **no** recodifica (no hay
`android.graphics` en `com.sazon.app.data.image`).

### Scenario 6: la evicción LRU respeta el tope borrando los más antiguos

Given un `FileDishImageCache` con un tope **inyectado** pequeño (p. ej. 300 bytes) y tres ficheros con
`lastModified` crecientes,
When se escribe uno nuevo que hace superar el tope,
Then se borran los más antiguos (los menos recientemente usados) hasta que el total del directorio sea
**≤ tope**, el fichero recién escrito **sobrevive** y los más recientes también.

### Scenario 7: la evicción no sale de su subdirectorio ni toca nada ajeno

Given un fichero ajeno **fuera** de la raíz de la caché (p. ej. un `.db` de Room o un `default.webp` en
el directorio padre) y un fichero con otra extensión **dentro**,
When se fuerza una pasada de evicción,
Then el fichero ajeno y el de extensión distinta quedan **intactos**: la caché solo considera y solo
borra ficheros `<clave>.webp` dentro de su raíz. `filesDir` y las bases Room no se tocan (verificado por
inspección y por el test Android del Scenario 13).

### Scenario 8: LRU de memoria por acceso

Given un `MemoryDishImageCache` con tope de **2 entradas**,
When se insertan tres claves y se vuelve a pedir la primera,
Then el tamaño se mantiene en 2, la **menos usada recientemente** es la que se descarta y, si se vuelve a
resolver una clave descartada, se sirve **del disco** sin llamar al generador.

### Scenario 9: fallo del generador → `null` sin persistir; la cancelación se propaga

Given un generador que devuelve `null`, que devuelve un array **vacío** o que lanza
`IllegalStateException`,
When se resuelve una clave sin copia,
Then `imageFor` devuelve `null`, **no** lanza, **no** crea fichero ni entrada de memoria y no deja
temporales residuales. Con un generador que lanza `CancellationException`, el resolutor la **relanza**
(no la convierte en `null`).

### Scenario 10: serialización por clave (no se paga dos veces la misma imagen)

Given dos llamadas **concurrentes** a `imageFor` con la **misma** clave y un generador lento que suspende
hasta que ambas están lanzadas,
When ambas terminan,
Then el generador se ha invocado **una sola vez** y las dos respuestas son la misma imagen no nula. Con
dos claves distintas, el generador se invoca una vez **por clave**.

### Scenario 11: la caché nunca lanza por fallos de I/O

Given una raíz **no utilizable** (p. ej. un fichero regular ocupando la ruta de la raíz o un fichero
ilegible),
When se escribe y se lee,
Then `write` no persiste (devuelve `false`/no-op) y `read` devuelve `null`, **sin** excepción; y si el
generador sí devuelve bytes, `imageFor` los devuelve igualmente aunque no se hayan podido persistir
(la persistencia es *best-effort*).

### Scenario 12: alcance y pureza (inspección)

Given la implementación terminada,
When se inspecciona el paquete y el diff,
Then `com.sazon.app.data.image` **no** importa Compose ni `ui.*`, **no** conoce `Recipe`, `Taxonomy`,
`DishImagePrompt` ni `DishImageCacheKey` (consume `request.cacheKey`/`request.prompt`), **no** usa `Log`
ni `filesDir`/`getDatabasePath`, y el **único** import de `android.*` está en `AndroidDishImageCache.kt`;
no cambian `domain.image`, `data.ai.image`, la cadena de `recipe-photos`, la UI, la Taxonomía,
`seed-recipes.json`, el build, el lockfile ni `init.sh`; no hay entradas nuevas en `docs/design/concepts/`.

### Scenario 13: la raíz es `cacheDir/dish-images` (costura Android, Robolectric)

Given un `Context` de test (`@Config(sdk = [33])`),
Then `AndroidDishImageCache.root(context) == File(context.cacheDir, "dish-images")` y una resolución con
generador falso escribe el fichero **dentro** de `context.cacheDir` y **no** en `context.filesDir`.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-image-cache`, líneas 963-976) — metadata, verificación exigida
  (segunda petición sin generador; títulos equivalentes comparten fichero; sin red se sirve del disco;
  evicción LRU con tope; WebP; `cacheDir`; sin dependencias) y la nota con la decisión de la persona.
- `docs/ai-dish-images-proposal.md` — §4 (tabla de piezas: caché de disco "base Room propia o ficheros
  en `cacheDir`"), §5 (las tres capas: memoria = bitmaps LRU, disco propio por clave, set semilla;
  clave = `dishKey`; **tope de disco 50-100 MB** con evicción LRU), §6 (no hacer esperar, fallo
  silencioso, timeouts propios), §8 (riesgos de cuota y de crecimiento de almacenamiento), §10 (fase 3 =
  "caché en disco + memoria, evicción LRU, clave canónica"; fase 4 = UI progresiva) y §11.2 (Room vs
  ficheros → decidido: ficheros).
- `docs/adr/0005-ai-dish-images.md` — contrato de fallo (`null`, cancelación propagada), clave versionada
  como identidad de caché, formato WebP decidido en la fase 2 y "la caché (fase 3)" como su reutilizadora
  tal cual, y la cadena local como **respaldo** (la inserta la fase 4). Estado actual "fases 1-2".
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — `DishImageRequest(cacheKey,
  prompt, recipeId)` con `of(taxonomy, recipe)` como **único punto de composición** y el puerto
  `suspend fun generate(request): ByteArray?` (formato decidido por la fase 2 y la caché de la fase 3).
  **No se modifica.**
- `app/src/main/java/com/sazon/app/data/ai/image/DishImagePolicy.kt` y `DishImageReducer.kt` — patrón de
  "política pura en un único sitio" + "costura `fun interface` para aislar `android.graphics` en un solo
  fichero", que esta feature replica sin Android.
- `app/src/main/java/com/sazon/app/data/RecipePhotoAssets.kt` — patrón de lógica **pura** de imagen en
  `data` (sin Android ni I/O, contrato `null`, `EXTENSIONS`), determinista y sin estado.
- `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` — precedente de **LRU en memoria**
  (`LinkedHashMap` en modo acceso con evicción, `MAX_CACHED_PHOTOS = 16`), I/O en `Dispatchers.IO`,
  `null` en vez de excepción y `synchronized` sobre el mapa. **No se modifica.**
- `app/src/main/java/com/sazon/app/ui/artwork/RecipePhotoSource.kt` y `RecipeArtwork.kt` — el seam
  (`fun interface` + `LocalRecipePhotoSource` con `null` por defecto) y los 5 puntos de uso con su
  geometría; **no** se tocan en esta fase (fase 4).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — composición manual de dependencias y el
  `CompositionLocalProvider(LocalRecipePhotoSource provides …)`; **no** se toca aquí.
- `app/build.gradle.kts` (líneas 89-130) — `junit` 4.13.2, `kotlinx-coroutines-test`, `robolectric` y
  `androidx.test:core` ya están en `testImplementation`; las tareas `Test` reciben las system properties
  de assets. **No hace falta tocar el build.**
- `docs/technical-discovery.md` — tabla **Data and Storage** (líneas 29-40) y los bullets de las fases 1-2
  (líneas 530-562): `data.ai.image` existe pero "sigue **sin caché, sin UI y sin consumidores**".
- `docs/risks-and-open-questions.md` (línea 144) — riesgo de cuota (~2 %/imagen), latencia (~4,2 s) y
  crecimiento de almacenamiento, con la mitigación ya prevista (clave compartida + caché persistente con
  evicción LRU y tope + tope por sesión + interruptor + set semilla).
- `docs/domain/taxonomy.md` §6 — estrategia visual vigente y la cadena de respaldo de `RecipePhotoAssets`;
  la resolución en runtime **no** cambia en esta fase.
- `docs/specs/dish-image-domain.md` y `docs/specs/dish-image-generator.md` — estructura, nivel de detalle
  y decisiones de las dos fases previas; `docs/specs/recipe-photos.md` — cadena local aceptada.

### Existing Patterns To Follow

- Paquete de datos `com.sazon.app.data.<área>`, con la **lógica pura** separada de la **costura Android**
  (`RecipePhotoAssets` vs `AssetRecipePhotoSource`); `data` **nunca** depende de `ui`/Compose.
- Costura `fun interface` + implementación Android aislada en un único fichero (`DishImageReducer` /
  `AndroidDishImageReducer`).
- LRU en memoria con `LinkedHashMap(accessOrder = true)` y evicción del más antiguo (`AssetRecipePhotoSource`).
- Degradación silenciosa: `null` = no disponible; nunca excepción por datos, red o I/O; `CancellationException`
  relanzada.
- Política y constantes puras en un **único sitio**, testeables en JVM, con topes **inyectables** para
  poder probarlos con valores diminutos (`DishImageTimeouts` es el precedente).
- Tests JUnit 4 en `app/src/test/java/com/sazon/app/...`, nombres de test en español con `_`, y Robolectric
  `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [33])` solo para lo que necesita Android.
- Verificación canónica `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo.

### Current Gaps

- **No existe `com.sazon.app.data.image`**: hoy no hay ninguna noción de caché de imagen en el repo (ni
  memoria ni disco). La única caché en memoria de imágenes es privada de `AssetRecipePhotoSource`.
- **No existe nada que escriba en `cacheDir`**: `grep -rn "cacheDir" app/src/main` → 0; el repo solo usa
  Room (`filesDir`) para persistir.
- **El resolutor no existe**: hoy el adaptador (`data.ai.image`) devuelve bytes y **nadie** los guarda;
  cada llamada futura pagaría la cuota otra vez.
- **Nadie consume el puerto todavía**: sigue siendo cierto después de esta feature (lo cambia la fase 4).

## Technical Approach

### 1. Dónde vive

Nuevo paquete `app/src/main/java/com/sazon/app/data/image/` (`com.sazon.app.data.image`), con cinco
ficheros: la política pura, el almacén de ficheros, la LRU en memoria, el resolutor y la **única** costura
Android. Es la misma frontera que `data.ai.image` (política pura + costura Android aislada), pero **esta
feature no necesita códec**: el formato ya lo fija la fase 2. Firmas orientativas (el contrato es lo
vinculante):

```kotlin
// 1) DishImageCachePolicy.kt — puro: constantes y política de nombres/evicción
object DishImageCachePolicy {
    const val SUBDIRECTORY = "dish-images"                 // bajo cacheDir (nunca filesDir)
    const val EXTENSION = "webp"                           // formato fijado en la fase 2
    const val MAX_DISK_BYTES = 80L * 1024 * 1024           // 80 MiB = 83_886_080 bytes
    const val MAX_MEMORY_ENTRIES = 16                      // patrón AssetRecipePhotoSource
    fun fileNameFor(cacheKey: String): String              // "<cacheKey>.webp"
    fun evictionVictims(candidates: List<DishImageCacheEntry>, maxBytes: Long): List<String>
}
data class DishImageCacheEntry(val fileName: String, val sizeBytes: Long, val lastModifiedMillis: Long)

// 2) FileDishImageCache.kt — java.io.File, sin Android
class FileDishImageCache(
    private val root: File,
    private val maxBytes: Long = DishImageCachePolicy.MAX_DISK_BYTES,
    private val now: () -> Long = System::currentTimeMillis,   // para tests deterministas
) {
    fun read(cacheKey: String): ByteArray?                 // null si no existe/ilegible; al acertar toca la recencia
    fun write(cacheKey: String, bytes: ByteArray): Boolean // escritura atómica + evicción; false si no se pudo
    fun totalBytes(): Long
    fun entries(): List<DishImageCacheEntry>               // solo "<clave>.webp" dentro de root
}

// 3) MemoryDishImageCache.kt — puro Kotlin/JDK
class MemoryDishImageCache(private val maxEntries: Int = DishImageCachePolicy.MAX_MEMORY_ENTRIES) {
    fun get(cacheKey: String): ByteArray?
    fun put(cacheKey: String, bytes: ByteArray)
    fun clear()
    val size: Int
}

// 4) DishImageResolver.kt — orquestación (memoria -> disco -> generador -> persistir)
class DishImageResolver(
    private val disk: FileDishImageCache,
    private val memory: MemoryDishImageCache,
    private val generator: DishImageGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun imageFor(request: DishImageRequest): ByteArray?
}

// 5) AndroidDishImageCache.kt — ÚNICO fichero con android.*
object AndroidDishImageCache {
    fun root(context: Context): File = File(context.cacheDir, DishImageCachePolicy.SUBDIRECTORY)
}
```

### 2. Política pura (`DishImageCachePolicy`)

- `fileNameFor(cacheKey) = "$cacheKey.$EXTENSION"`: la clave de la fase 1 ya es apta como nombre de
  fichero (alfabeto `[a-z0-9-]`, versionada `dish-image-v1-…`), así que **no** se rehashea ni se
  reindexa: "dos títulos equivalentes comparten fichero" sale por construcción.
- `MAX_DISK_BYTES = 80L * 1024 * 1024` (los 80 MB de `feature_list.json`, en MiB, un único sitio).
- `evictionVictims(candidates, maxBytes)` es **pura y determinista**: ordena los candidatos por
  `lastModifiedMillis` ascendente con **desempate por nombre** ascendente y devuelve los nombres a borrar
  (los más antiguos primero) hasta que `total - borrados ≤ maxBytes`; con `total ≤ maxBytes` o lista vacía
  devuelve lista vacía. El **recién escrito no es candidato** (lo excluye el llamante), así que una imagen
  nueva nunca se borra en su propia pasada (si por sí sola superara el tope, la caché queda con ese único
  fichero; caso teórico con imágenes de ~30-60 KB).

### 3. Almacén de ficheros (`FileDishImageCache`)

- Recibe la **raíz por constructor** (en producción `AndroidDishImageCache.root(context)`, en tests un
  directorio temporal): por eso `data.image` no necesita `Context`.
- `read`: devuelve `null` si el fichero no existe o no se puede leer; al acertar **actualiza
  `lastModified` con `now()`** (recencia = LRU real; si el sistema de ficheros no lo soporta, la LRU
  degrada a FIFO, aceptado). **Nunca lanza.**
- `write`: `root.mkdirs()`, escrita **atómica** (fichero temporal en la misma raíz + `renameTo`) para no
  dejar un fichero a medio escribir servible, y después la **pasada de evicción**. Devuelve `false` si no
  se pudo escribir o renombrar; limpia el temporal. **Nunca lanza.**
- `entries()`/`totalBytes()` lista únicamente ficheros **regulares** `<clave>.webp` dentro de `root`; la
  evicción **nunca** borra nada fuera de `root` ni de esa extensión (Scenario 7).

### 4. LRU en memoria (`MemoryDishImageCache`)

`LinkedHashMap<String, ByteArray>(initialCapacity, 0.75f, /* accessOrder = */ true)` con
`removeEldestEntry { size > maxEntries }`, igual que el precedente aceptado de `AssetRecipePhotoSource`
(tope 16), y acceso `synchronized` porque el resolutor puede llamarse desde varias corrutinas. Guarda
**bytes codificados** (WebP), no bitmaps: ver "Decisions And Unknowns" (decisión 5).

### 5. Resolutor (`DishImageResolver`)

`suspend fun imageFor(request: DishImageRequest): ByteArray?`, en este orden:

1. **Memoria**: `memory.get(request.cacheKey)` → si hay, se devuelve.
2. **Sección crítica por clave**: una `Mutex` por `cacheKey` (mapa concurrente) **serializa** las
   peticiones de la misma clave, de modo que dos peticiones simultáneas no pagan dos veces la imagen
   (Scenario 10). Dentro del cerrojo: **re-consulta memoria** y, si no, `disk.read(cacheKey)` en
   `withContext(ioDispatcher)`. Si hay copia → a memoria y se devuelve.
3. **Generador**: `generator.generate(request)`, con `CancellationException` **relanzada** y cualquier otra
   `Exception` degradada a `null` (la persistencia y el pintado son *best-effort*). `null` o array vacío =
   "no disponible" → se devuelve `null` **sin** escribir nada.
4. **Persistir y devolver**: `disk.write(cacheKey, bytes)` ignorando el resultado booleano (si falla, se
   devuelven igualmente los bytes), `memory.put(cacheKey, bytes)` y se devuelven los bytes.

El resolutor **no** conoce `Recipe`/`Taxonomy`: recibe el `DishImageRequest` ya compuesto por
`DishImageRequest.of(taxonomy, recipe)` (fase 1) y usa `request.cacheKey`/`request.prompt` tal cual.

### 6. Costura Android (`AndroidDishImageCache`)

`object` de un solo método: `root(context) = File(context.cacheDir, SUBDIRECTORY)`. Es el único fichero de
`data.image` que importa `android.*` y el punto que la fase 4 usará para construir el resolutor (que
también recibirá el `DishImageGenerator` real ya cableado en `MainActivity`). **No** se cablea aquí.

### 7. Frontera con la fase 4 (no implementar aquí)

La cadena objetivo es `imagen generada (memoria → disco) → assets propios → assets de categoría →
default → placeholder`; esta fase entrega el **primer nivel** y su contrato (`bytes` para una clave), y la
fase 4 lo inserta en `ui.artwork` (proveedor de `LocalRecipePhotoSource` que intenta el resolutor y cae a
`AssetRecipePhotoSource` cuando devuelve `null`), añade la decodificación a `ImageBitmap` y su LRU de
bitmaps, la carga progresiva y los guardarraíles. La cadena local de `recipe-photos` **no** se toca.

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/image/DishImageCachePolicy.kt` — crear; constantes
  (`SUBDIRECTORY`, `EXTENSION`, `MAX_DISK_BYTES`, `MAX_MEMORY_ENTRIES`), `fileNameFor` y la política pura
  de evicción (`DishImageCacheEntry` + `evictionVictims`).
- `app/src/main/java/com/sazon/app/data/image/FileDishImageCache.kt` — crear; `read`/`write` atómicos,
  `entries`/`totalBytes` y evicción LRU sobre una raíz inyectada, sin lanzar.
- `app/src/main/java/com/sazon/app/data/image/MemoryDishImageCache.kt` — crear; LRU de bytes por clave.
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — crear; orquestación
  memoria → disco → generador → persistir con `Mutex` por clave.
- `app/src/main/java/com/sazon/app/data/image/AndroidDishImageCache.kt` — crear; `root(context)` bajo
  `cacheDir` (único import de `android.*`).
- `app/src/test/java/com/sazon/app/data/image/DishImageCachePolicyTest.kt` — crear; nombres, tope y
  selección de víctimas (orden, empates por nombre, total ya por debajo del tope, lista vacía, fichero
  mayor que el tope).
- `app/src/test/java/com/sazon/app/data/image/FileDishImageCacheTest.kt` — crear; temporales
  (`TemporaryFolder`); ida y vuelta, recencia al leer, evicción con tope inyectado, nada fuera de la raíz,
  errores de I/O sin excepción (Scenarios 5-7 y 11).
- `app/src/test/java/com/sazon/app/data/image/MemoryDishImageCacheTest.kt` — crear; Scenario 8.
- `app/src/test/java/com/sazon/app/data/image/DishImageResolverTest.kt` — crear; Scenarios 1-4 y 9-11 con
  un generador falso y un directorio temporal (JVM puro).
- `app/src/test/java/com/sazon/app/data/image/AndroidDishImageCacheTest.kt` — crear; Scenario 13 con
  Robolectric `@Config(sdk = [33])` y `ApplicationProvider.getApplicationContext<Context>()`.
- `docs/technical-discovery.md` — modificar; registrar la caché `data.image` (ficheros en `cacheDir`
  indexados por la clave canónica, tope 80 MiB con evicción LRU, LRU de bytes en memoria, resolutor) y
  que sigue **sin consumidores**, con la cadena de `recipe-photos` como única resolución en runtime.
- `docs/adr/0005-ai-dish-images.md` — modificar; estado "Aceptado parcialmente (fases 1-3)" y nota de la
  fase 3 (ficheros en `cacheDir` y no Room, clave = nombre de fichero, tope de disco, LRU de memoria),
  dejando las fases 4-7 pendientes.
- `docs/risks-and-open-questions.md` — modificar; el riesgo de crecimiento de almacenamiento pasa a tener
  mitigación implementada (tope + evicción) y la cuota sigue sin gastarse hasta la fase 4.
- `PROGRESS.md` y `feature_list.json` — modificar al implementar/validar (estado, evidencia); **no** los
  toca el planificador.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`app/src/androidTest/**`, `AndroidManifest.xml`, `domain/**` (en particular `domain/image/**`),
`data/ai/image/**`, `data/RecipePhotoAssets.kt`, `data/AssetRecipePhotoSource.kt`, `ui/**` (en particular
`ui/artwork/**` y `MainActivity.kt`), la Taxonomía, `seed-recipes.json`, `assets/**`, `DESIGN.md`,
`docs/domain/taxonomy.md`, `docs/design/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: data`; no hay pantalla, ni componente Compose, ni estado visual, ni
  microcopy).
- Design source: no aplica.
- Screens or states affected: ninguna (la app sigue pintando la cadena de assets de hoy; el cambio visual
  llega en la fase 4).
- New design artifact required: no.
- **El gate de capturas de UI no aplica**: **no** se crea `docs/design/concepts/dish-image-cache/` ni
  ninguna otra carpeta en `docs/design/concepts/`. Las capturas de referencia son obligatorias en la
  fase 4 (`dish-image-progressive-ui`, `area: ui`).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — la frontera de imagen de plato gana la **caché**
  (`data.image`: ficheros en `cacheDir` indexados por la clave canónica, tope 80 MiB con evicción LRU,
  LRU de bytes en memoria, resolutor memoria → disco → generador) y la tabla **Data and Storage** debe
  reflejarla; se mantiene explícito que **nadie la consume** todavía y que la cadena de `recipe-photos`
  sigue siendo la única resolución en runtime.
- `docs/adr/0005-ai-dish-images.md`: **update** — estado "Aceptado parcialmente (fases 1-3)": se registra
  la decisión de la fase 3 (**ficheros en `cacheDir`, no Room**, clave = nombre de fichero, tope de disco
  con evicción, LRU de memoria, sin códec en la caché) y que las fases 4-7 siguen pendientes. La regla
  durable de invalidación (subir `DishImageCacheKey.VERSION`) no cambia.
- `docs/risks-and-open-questions.md`: **update** — la mitigación de crecimiento de almacenamiento deja de
  ser hipotética (tope + evicción implementados); el gasto de cuota se materializa igualmente en la
  fase 4.
- `docs/domain-model.md`: **not needed** — no hay entidades, relaciones ni estados nuevos: la caché es un
  artefacto de almacenamiento, no una entidad de dominio, y la imagen no es un campo de `Recipe`
  (descartado en el ADR 0004).
- `docs/domain/taxonomy.md` §6: **not needed** en esta fase — la resolución de imagen en runtime **no**
  cambia (nadie consume la caché); se actualizará en la fase 4, cuando la cadena gane su primer nivel.
- `AGENTS.md`: **not needed** — no cambian el flujo, el arranque ni las reglas de trabajo.
- `DESIGN.md`: **not needed** — sin UI ni microcopy nuevos.
- `docs/ai-dish-images-proposal.md`: **not needed** — documento de propuesta; `feature_list.json` y el ADR
  son la fuente de verdad del estado.

## Implementation Plan

1. Crear `DishImageCachePolicy` (constantes, `fileNameFor`, `DishImageCacheEntry` y `evictionVictims`) y
   hacer pasar `DishImageCachePolicyTest` (JVM).
2. Crear `FileDishImageCache` (lectura/escritura atómica, recencia, evicción, tolerancia a I/O) y hacer
   pasar `FileDishImageCacheTest` con `TemporaryFolder` (JVM).
3. Crear `MemoryDishImageCache` y hacer pasar `MemoryDishImageCacheTest` (JVM).
4. Crear `DishImageResolver` (memoria → disco → generador → persistir; `Mutex` por clave; cancelación
   propagada) y hacer pasar `DishImageResolverTest` con un generador falso (JVM).
5. Crear `AndroidDishImageCache.root(context)` y `AndroidDishImageCacheTest` (Robolectric), incluida la
   escritura real bajo `cacheDir` y la no-escritura bajo `filesDir`.
6. Inspeccionar pureza y alcance (greps del Verification Plan) y que no hay ficheros fuera del alcance.
7. Actualizar `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y el ADR 0005.
8. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `DishImageCachePolicy` con `SUBDIRECTORY = "dish-images"`, `EXTENSION = "webp"`,
      `MAX_DISK_BYTES = 80L * 1024 * 1024`, `MAX_MEMORY_ENTRIES = 16`, `fileNameFor` y
      `evictionVictims` (puros, deterministas, con desempate por nombre).
- [ ] Crear `FileDishImageCache` con raíz y tope inyectables, `now: () -> Long` inyectable, `read`
      (con toque de recencia), `write` (temporal + `renameTo`, evicción después, limpieza del temporal),
      `entries`/`totalBytes` limitados a `<clave>.webp` dentro de la raíz y **sin lanzar**.
- [ ] Crear `MemoryDishImageCache` (LRU de bytes, `accessOrder = true`, tope de entradas, `synchronized`).
- [ ] Crear `DishImageResolver.imageFor(request)` con el orden memoria → disco → generador → persistir,
      `Mutex` por clave y `CancellationException` relanzada.
- [ ] Crear `AndroidDishImageCache.root(context) = File(context.cacheDir, SUBDIRECTORY)` (único import de
      `android.*`).
- [ ] Escribir `DishImageCachePolicyTest`, `FileDishImageCacheTest` (temporales), `MemoryDishImageCacheTest`
      y `DishImageResolverTest` (generador falso, sin red), cubriendo los Scenarios 1-11 con casos
      negativos que no pasan en vacío.
- [ ] Escribir `AndroidDishImageCacheTest` con Robolectric `@Config(sdk = [33])` (Scenario 13).
- [ ] Verificar por inspección la pureza del paquete (solo `AndroidDishImageCache.kt` con `android.*`; sin
      Compose, sin `Recipe`/`Taxonomy`/`DishImageCacheKey`, sin `Log`, sin `filesDir`) y que no cambia
      ningún fichero fuera del alcance.
- [ ] Actualizar `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y el ADR 0005.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde y registrar la evidencia.
- [ ] Actualizar `feature_list.json` (a `passing` con evidencia) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew lint`
  vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red, **no** gasta cuota y **no**
  modifica `init.sh`.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"` →
  `BUILD SUCCESSFUL`.
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0 errores,
  0 saltados**. Baseline de referencia: **72 suites / 517 tests** (última validación independiente,
  sesión 090); el implementer **re-deriva** el número exacto en su HEAD y confirma el delta (las 5 suites
  nuevas: `DishImageCachePolicyTest`, `FileDishImageCacheTest`, `MemoryDishImageCacheTest`,
  `DishImageResolverTest` y `AndroidDishImageCacheTest`).
- `bash scripts/gradle.sh lint` → **0 errores** (los 13 warnings de `GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion` y `OldTargetApi` son preexistentes, todos en el
  build/version catalog, **ninguno** bajo `app/src`).
- Inspección de pureza y alcance:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/data/image` → solo `AndroidDishImageCache.kt`.
  - `grep -rn "androidx\.\|Recipe\|Taxonomy\|DishImagePrompt\|DishImageCacheKey\|Log\.\|filesDir\|getDatabasePath"`
    bajo `app/src/main/java/com/sazon/app/data/image` → 0.
  - `git diff --name-only` solo con los 5 ficheros de `main`, los 5 de test y los 3 docs previstos
    (+ `feature_list.json`/`PROGRESS.md`); `app/build.gradle.kts`, `gradle/libs.versions.toml`,
    `init.sh`, `scripts/**`, `domain/**`, `data/ai/image/**`, `data/RecipePhotoAssets.kt`,
    `data/AssetRecipePhotoSource.kt`, `ui/**`, `MainActivity.kt`, la Taxonomía, `seed-recipes.json`,
    `assets/**`, `DESIGN.md` y `docs/design/**` intactos; `docs/design/concepts/` sin entradas nuevas.
- Cobertura E2E: **no aplica**. El repo no tiene suite E2E persistente; la feature no cambia el
  comportamiento observable (no tiene consumidor: sin UI, sin red, sin cableado) y su verificación
  canónica es el test JVM (más Robolectric para la costura de `cacheDir`), según `AGENTS.md`.
- Nota operativa para quien implemente: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca**
  `./gradlew` directo ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de la tirada focal `--tests "com.sazon.app.data.image.*"`.
- Conteo parseado del XML del gate (suites/tests, 0 fallos/errores/saltados) con los tests nuevos
  nombrados y la baseline re-derivada.
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Resultado de los greps de inspección (Android solo en `AndroidDishImageCache.kt`; sin Compose/dominio/
  `Log`/`filesDir`; ficheros del alcance).
- Vectores concretos comprobados en los tests: `fileNameFor("dish-image-v1-arroz-con-pollo-criollo") ==
  "dish-image-v1-arroz-con-pollo-criollo.webp"`; fichero con bytes idénticos a los del generador y *magic
  bytes* `RIFF`…`WEBP`; **una sola** invocación del generador en la segunda petición y en dos
  concurrentes de la misma clave; resolución de disco con generador `null`; evicción con un tope inyectado
  (borra los `lastModified` más antiguos y no sale de la raíz); `null` sin persistir para `null`/vacío/
  excepción del generador; `CancellationException` relanzada; `root(context) == File(context.cacheDir,
  "dish-images")` y escritura bajo `cacheDir` (no `filesDir`).
- Anotación en `feature_list.json` (evidencia + `passing`) y en `PROGRESS.md`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **Ficheros en `cacheDir/dish-images`, no Room.** Decisión de la persona (2026-10-13) que resuelve la
   pregunta abierta §11.2 de la propuesta: la imagen ya está codificada en un fichero y el índice no
   necesita consultas, así que una base Room solo añadiría esquema, migraciones y una segunda fuente de
   verdad. La raíz se **inyecta** (`File`) para que los tests corran en JVM con un directorio temporal, y
   la costura Android (`AndroidDishImageCache.root(context)`) es el único punto que conoce `cacheDir`.
2. **La clave canónica es el nombre de fichero** (`<cacheKey>.webp`), sin rehash ni índice: la clave de la
   fase 1 ya es un identificador apto (`[a-z0-9-]`, versionada), y así "dos títulos equivalentes comparten
   imagen" sale por construcción.
3. **La caché no recodifica**: almacena **tal cual** los bytes que devuelve el generador (WebP q80 de
   960 px, fijado en la fase 2). La extensión `.webp` es una convención del nombre, no una conversión; por
   eso `data.image` **no** importa `android.graphics`.
4. **Tope de disco = 80 MiB (`80L * 1024 * 1024`), evicción LRU** por `lastModified` (con toque de
   recencia al leer), desempate por nombre y exclusión del recién escrito; la evicción solo borra
   `<clave>.webp` dentro de su raíz.
5. **La LRU de memoria guarda bytes, no bitmaps decodificados** (desviación razonada de la propuesta §5,
   que sitúa ahí los bitmaps): (a) mantiene `data.image` casi puro y **todo** su test en JVM sin Robolectric
   salvo la costura; (b) la decodificación y *su* LRU de bitmaps pertenecen a la **fase 4** (UI), que ya
   tiene el patrón aceptado de `AssetRecipePhotoSource` (LRU 16) y es donde se pinta con scroll; (c) el
   valor de la memoria aquí es no repetir disco y no repetir proveedor. Tope: 16 entradas (precedente del
   repo).
6. **Un `DishImageResolver` orquesta memoria → disco → generador → persistir**, y es el nivel que la fase 4
   inserta delante de `recipe-photos`. En esta fase queda **sin consumidores ni cableado**: la cadena local
   sigue intacta y el contrato de inserción es "`imageFor != null` → se pinta lo generado; `null` → se
   conserva la cadena actual".
7. **Fallo = `null`, nunca excepción; la cancelación se propaga.** Coherente con el ADR 0005 y con
   `DishImageGenerator`; la persistencia es *best-effort* (si falla el disco, se devuelven los bytes
   generados). No se introduce un tipo de error nuevo.
8. **Deduplicación por clave dentro del proceso** (`Mutex` por `cacheKey`): dos peticiones simultáneas del
   mismo plato (p. ej. tarjeta y hero) no pagan dos veces ~2 % de cuota; la segunda espera y se resuelve de
   memoria/disco. No hay coordinación entre procesos (la app es un único proceso).
9. **Escritura atómica** (temporal en la misma raíz + `renameTo`) y limpieza del temporal si falla: un
   fichero a medio escribir nunca queda servible.
10. **Sin dependencias nuevas y sin tocar el build**: `junit`, `kotlinx-coroutines-test`, `robolectric` y
    `androidx.test:core` ya están; las tareas `Test` ya reciben sus system properties. Sin telemetría ni
    `Log` (la clave y los bytes no deben aparecer en logs).

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **Un cambio de `DishImageCacheKey.VERSION` deja huérfanos** los ficheros `dish-image-v1-*.webp` (nunca
  más se leen, pero siguen ocupando disco hasta que la LRU los evicte). Está **acotado por el tope** y se
  acepta; si molesta, una feature posterior puede borrar por prefijo de versión.
- **`lastModified` con resolución gruesa o no soportado** (algunos sistemas de ficheros) degrada la LRU a
  FIFO y puede producir empates: el desempate por nombre mantiene el resultado **determinista**, y los
  tests fijan `lastModified` explícitamente.
- **Un fichero corrupto o truncado se sirve tal cual**: la caché no decodifica, así que no puede
  detectarlo; la fase 4 lo degradará a la cadena local cuando falle la decodificación.
- **`cacheDir` puede vaciarlo el sistema** en cualquier momento: la caché es *best-effort*, un fallo de
  acierto solo regenera (y vuelve a gastar cuota); comportamiento esperado, no un defecto.
- **La primera imagen de cada plato sigue costando** red, cuota y ~4 s: esta fase solo elimina los usos
  repetidos; el set semilla (fase 7) ataca la primera.
- **La costura `AndroidDishImageCache.root` solo la usan los tests** hasta que la fase 4 cablee el
  resolutor en `MainActivity`; es deliberado (nadie consume el puerto todavía, igual que en la fase 2).
- **El tope de 80 MiB es del subdirectorio de la caché** (suma de longitudes de fichero), no de toda la
  app; el resto del almacenamiento (Room, assets) no se mide ni se toca.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: **sin** UI/cableado, sin Room, sin códec ni
      `android.graphics`, sin consumidores, sin set semilla, sin `/images/edits`, sin campos nuevos en el
      wire, sin dependencias nuevas ni cambios en build/lockfile/`init.sh`/`domain.image`/`data.ai.image`/
      la cadena de `recipe-photos`.
- [ ] Los 13 escenarios pasan, incluidos los negativos: `null`/vacío/excepción del generador (sin
      persistir), I/O no utilizable (sin lanzar), evicción que no sale de su raíz, y cancelación propagada.
- [ ] La segunda petición con la misma clave **no** invoca al generador (memoria y, con memoria fría,
      disco); dos títulos equivalentes comparten **un solo** fichero; con generador `null` la imagen se
      sirve del disco y, sin copia, se devuelve `null` sin escribir nada.
- [ ] El fichero vive bajo la raíz inyectada, se llama `<cacheKey>.webp`, su contenido es **idéntico** al
      del generador (`RIFF`…`WEBP`) y la caché **no** recodifica.
- [ ] La evicción respeta el tope (probada con un tope inyectado), borra los más antiguos (LRU, desempate
      por nombre), nunca borra el recién escrito y **nunca** toca ficheros fuera de su raíz ni de la
      extensión de la caché.
- [ ] `AndroidDishImageCache.root(context)` es `File(context.cacheDir, "dish-images")` y el fichero se
      escribe bajo `cacheDir`, no bajo `filesDir` (test Robolectric `@Config(sdk = [33])`).
- [ ] `com.sazon.app.data.image` **no** conoce `Recipe`/`Taxonomy`/`DishImagePrompt`/`DishImageCacheKey`,
      no importa Compose ni `ui.*`, no usa `Log`, y el único import de `android.*` está en
      `AndroidDishImageCache.kt`.
- [ ] `docs/technical-discovery.md` (tabla Data and Storage incluida), `docs/risks-and-open-questions.md`
      y el ADR 0005 están actualizados y coherentes (fase 3 entregada, fases 4-7 pendientes, caché sin
      consumidores).
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica a esta feature; es de la
      fase 4).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo commit,
      push ni PR.
