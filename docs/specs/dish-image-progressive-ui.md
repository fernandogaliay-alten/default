# Feature Implementation Spec: Carga progresiva de la imagen de plato

## Source Feature

- `id`: `dish-image-progressive-ui`
- `area`: `integration` (reclasificada por la persona el 2026-10-13; el efecto es un cambio de
  **fuente** de imagen, no de diseño)
- `depends_on`: `["dish-image-cache"]` (satisfecha: `accepted`, sesión 091)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Insertar la **imagen de plato generada** (fase 4 de `docs/ai-dish-images-proposal.md` y de
`docs/adr/0005-ai-dish-images.md`) como un **nivel por delante** de la cadena aceptada de
`recipe-photos`, sin cambiar ni la pantalla ni la geometría: `ui.artwork.RecipeArtwork` pide la
imagen al resolutor ya existente (`DishImageResolver`: memoria → disco → generador real, fase 3) para
las recetas **visibles**, pinta **al instante** la foto local de siempre y, cuando llega la generada,
la **sustituye con un crossfade estándar**. Si la generación falla, tarda, se cancela o no hay red, se
**conserva la foto local** (y, si tampoco la hay, el degradado + glifo de último recurso): nunca un
error de UI, nunca un hueco de imagen.

Es la **mitad de integración** del split de la fase 4 acordado por la persona (2026-10-13): la costura
de resolución, la progresión visible, la cancelación, el límite de concurrencia **y el mecanismo del
tope de generaciones por sesión** (que esta feature debe respetar según su propio `verification`). El
**interruptor de usuario y la gestión visible del tope** (mostrarlo/ajustarlo en Ajustes y persistirlo)
son la otra mitad (`dish-image-settings-toggle`, `area: ui`), ver "Scope Reconciliation".

## Non-Goals

- **No hay UI nueva**: mismo composable, **misma firma y geometría**, los **5 puntos de uso sin
  cambios** (`RecipeCard` 16:10, `ResultRecipeCard` 16:10, `FavoriteRecipeCard` tarjeta 96 de alto y
  miniatura 96x96, `DetailHero` 240). Sin layout, sin microcopy, sin tokens, sin estados nuevos.
- **No se implementa el interruptor de usuario ni la cara visible del tope por sesión** (pantalla de
  Ajustes, mostrar/ajustar el tope, persistencia de esa preferencia): son de
  `dish-image-settings-toggle`. Lo que **sí** va aquí es el **mecanismo** del tope (constante +
  contador + imposición en el borde de generación), porque el `verification` de esta feature exige
  "tope de generaciones por sesión respetado" (ver "Scope Reconciliation").
- **No hay indicador/shimmer** de "generando imagen" ni ningún estado visual nuevo.
- **No se modifica la cadena local de `recipe-photos`**: `data.RecipePhotoAssets` y
  `data.AssetRecipePhotoSource` quedan **intactos** y siguen siendo el respaldo y la imagen instantánea.
- **No se modifica ningún fichero aceptado de `data.image`** (`DishImageResolver`,
  `DishImageCachePolicy`, `FileDishImageCache`, `MemoryDishImageCache`, `AndroidDishImageCache`); solo
  se **añade** `DishImageGenerationBudget.kt` (presupuesto + decorador del generador). Tampoco se tocan
  `data.ai.image` ni `domain.image`, ni `Recipe`/`RecipeValidator`/la Taxonomía/`seed-recipes.json`.
- **No hay set semilla** (fase 7), ni `/images/edits` (fase 6), ni `dishKey`/`imagePrompt` en el wire
  (fase 5), ni autochequeo con visión (propuesta §7.4).
- **No hay timeout nuevo en la UI**: el **timeout propio** de esta feature es el de la política de
  imagen ya aceptada (`DishImagePolicy.TIMEOUTS`, 10/30/40 s, un solo intento, fase 2), ver Decisión 8.
- **No hay dependencias nuevas** ni cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/**`, `AndroidManifest.xml`. Sin telemetría ni `Log` (ni la clave ni las claves
  canónicas en logs).
- La generación de imagen **no** cuenta en el Resumen de actividad ni en Favoritos y no persiste
  recetas.
- No se toca `docs/design/concepts/` (ver "Visual Design Impact").

## Scope Reconciliation (split de la fase 4, 2026-10-13)

El `verification` de esta feature (`feature_list.json`) incluye la frase **"tope de generaciones por
sesión respetado"**, y las notas del split atribuyen el interruptor **y el tope** a
`dish-image-settings-toggle`. La persona zanjó la tensión (2026-10-13): la costura de esta feature
**debe respetar el tope** y hay que aclarar qué parte corresponde a cada feature **sin contradecir la
verificación**. Reparto (decisión cerrada, no reabrir):

- **Aquí (`dish-image-progressive-ui`) — el mecanismo del tope**: la constante
  `DishImageGenerationBudget.MAX_GENERATIONS_PER_SESSION` en un único sitio (patrón
  `AiProviderPolicy`), el contador de generaciones **por sesión de proceso** y su **imposición** en el
  borde de generación (`BudgetedDishImageGenerator`, decorador del puerto `DishImageGenerator`), con
  tests JVM. Así la frase del `verification` es literalmente **verdadera y testeable en esta feature**
  y **no** hace falta tocar `feature_list.json` (el planner no lo edita y la verificación se respeta tal
  cual está escrita).
- **En `dish-image-settings-toggle` — la gestión del tope y el interruptor**: el interruptor de usuario
  (activar/desactivar la generación), la **exposición** del tope en Ajustes ("ver el tope por sesión
  desde los ajustes", su `user_visible_behavior`), cualquier ajuste o reset y la persistencia de esa
  preferencia. Esa feature **reutiliza** el mecanismo (lee `used`/`maxGenerations` y puede reiniciar o
  sustituir el presupuesto en el cableado), sin re-implementarlo; su verificación "el tope por sesión se
  respeta (patrón `AiProviderPolicy`)" se cubre comprobando este mismo mecanismo.
- **Definición de "sesión"**: la **vida del proceso** de la app. Un `DishImageGenerationBudget` por
  proceso (creado en `MainActivity`), sin persistencia: al morir el proceso, el contador empieza en
  cero. El `reset()` queda expuesto para el hermano (p. ej. al cambiar el interruptor).
- **Semántica de consumo**: una unidad por **invocación del generador** (intento = petición al
  proveedor, que es donde se puede gastar cuota), con reserva estricta (`tryAcquire` antes de invocar;
  si el tope está agotado el decorador devuelve `null` **sin** invocar al delegado). Los aciertos de
  memoria/disco de la fase 3 **no** pasan por el decorador y **no** consumen presupuesto (una imagen se
  paga una vez).

## Job Story

Cuando abro una pantalla con recetas (Home, Resultados, Favoritos, Detalle),
quiero seguir viendo la foto del plato de inmediato y que, si la app tiene (o consigue) una imagen
más fiel a ese plato concreto, se sustituya sola con una transición suave,
para que la imagen represente el plato sin hacerme esperar y sin errores ni huecos cuando no hay red.

## Users And Permissions

- **Usuario local**: ve siempre una imagen (local al instante; generada al llegar). No interactúa con
  nada nuevo y nunca ve un error por imagen. Sin permisos nuevos.
- **Desarrollador / agente**: ejercita la costura en **JVM** con fuentes y decodificadores falsos, sin
  red, sin clave, sin cuota y sin emulador (Robolectric solo para el decodificador Android real).
- **Persona**: revisa el acabado en el emulador tras el merge (AGENTS.md no automatiza UI).

## Acceptance Scenarios

### Scenario 1: progresión con crossfade (camino feliz)

Given una receta visible con fuente local que devuelve un bitmap y fuente generada que devuelve otro,
When el componente compone y la generación llega después de la local,
Then la capa visible es primero `LOCAL` y después `GENERATED` con un crossfade estándar, sin frame en
blanco en ningún momento.

### Scenario 2: la generada manda cuando existe; la local es el respaldo (precedencia pura)

Given (`local`, `generated`) = (b1, b2), (b1, null) y (null, null),
Then la capa resuelta es `GENERATED`, `LOCAL` y `FALLBACK` respectivamente, y el objetivo del crossfade
es `1f` solo cuando hay generada.

### Scenario 3: degradación silenciosa (fallo, lentitud o sin red)

Given un resolutor que devuelve `null` (sin copia en caché y sin red/clave), está vacío o el
decodificador falla,
When se resuelve la imagen generada de una receta con foto local,
Then la imagen visible sigue siendo la **foto local**, no se pinta ningún error ni se deja hueco, y no
se propaga ninguna excepción (contrato `null` del ADR 0005). Si el proveedor **tarda** más de lo que
su timeout propio permite (`DishImagePolicy.TIMEOUTS`, máximo 40 s de request), el resolutor devuelve
`null` y la foto local se conserva igualmente.

### Scenario 4: acierto de caché (memoria/disco) sin gasto de cuota

Given la clave canónica del plato ya resuelta (memoria o disco de la fase 3),
When la tarjeta se vuelve a componer (scroll de ida y vuelta),
Then la imagen generada se muestra sin invocar al generador (una imagen se paga una vez) y la
progresión no espera ~4 s.

### Scenario 5: cancelación al salir de pantalla o hacer scroll

Given una petición de imagen generada en curso (`awaitCancellation`),
When el composable sale de la composición / la tarjeta se recicla,
Then la corrutina se cancela y `CancellationException` **se propaga** (no se convierte en `null` ni se
traga), sin escribir estado y sin fallar.

### Scenario 6: concurrencia limitada a 2

Given un resolutor falso que suspende hasta que se le libera y tres tarjetas visibles pidiendo a la vez,
When se lanzan las tres resoluciones,
Then nunca hay más de **2** resoluciones en vuelo simultáneas y las tres terminan.

### Scenario 7: LRU de imágenes decodificadas (acotada y por clave canónica)

Given un `DishImageArtworkLoader` con tope de 2 imágenes decodificadas,
When se resuelven tres claves distintas y se vuelve a la primera,
Then el tamaño se mantiene en 2, se descarta la menos usada recientemente y la primera se resuelve del
resolutor sin volver a decodificar; dos recetas con la **misma** clave canónica comparten imagen.

### Scenario 8: alcance y pureza (inspección)

Given la implementación terminada,
When se inspecciona el diff,
Then `RecipeArtwork` conserva firma (`recipe`, `modifier`, `glyphSize`) y geometría, **ningún** punto de
uso (5) cambia, `RecipePhotoSource`/`LocalRecipePhotoSource` siguen intactos, `data.ai.image`/
`domain.image` no se modifican, `data.image` solo **gana** el fichero del presupuesto (ningún fichero
aceptado cambia), solo `ui.artwork` lee el seam nuevo, el único fichero de `ui.artwork` con
`android.graphics` es el decodificador, no hay dependencias nuevas y **no** existe
`docs/design/concepts/dish-image-progressive-ui/`.

### Scenario 9: tope de generaciones por sesión respetado (mecanismo)

Given un `DishImageGenerationBudget` con tope inyectado pequeño (p. ej. 2) envolviendo un generador
falso que cuenta invocaciones y devuelve bytes,
When se piden imágenes generadas hasta agotarlo y una más,
Then las primeras `N` invocan al generador (una unidad por petición al proveedor), la `N+1` devuelve
`null` **sin invocar** al delegado (la UI conserva la foto local) y `used`/`remaining`/`exhausted`
reflejan el consumo; un acierto de caché (memoria/disco) **no** consume presupuesto y `reset()`
restaura el contador. La cancelación del delegado se propaga sin consumir de más.

## Repository Research

### Files Inspected

- `feature_list.json` (entradas `dish-image-progressive-ui` y `dish-image-settings-toggle`, líneas
  992-1021) — metadata, verificación exigida (incluye "tope de generaciones por sesión respetado") y la
  nota del split.
- `docs/ai-dish-images-proposal.md` — §4 (la costura es `ui.artwork`, "solo gana un nivel por
  delante"), §5 (las tres capas y la cadena objetivo), §6 (no hacer esperar, **solo lo visible** con
  cancelación al scroll, **concurrencia limitada (2)**, fallo silencioso, **política de timeouts propia
  para imagen**, transición suave; guardarraíles = tope por sesión + interruptor), §8 (riesgos), §10
  (fase 4).
- `docs/adr/0005-ai-dish-images.md` — estado "fases 1-3"; contrato `null`/cancelación propagada;
  decisión 7: la decodificación y **su LRU de bitmaps** son de la fase 4; la cadena local pasa a
  respaldo.
- `docs/specs/dish-image-cache.md` — §7 "Frontera con la fase 4" (el nivel se inserta en `ui.artwork`
  con decodificación + LRU de bitmaps) y la decisión de que la LRU de la fase 3 guarda **bytes**.
- `docs/specs/dish-image-domain.md`, `docs/specs/dish-image-generator.md`, `docs/specs/recipe-photos.md`
  — contratos de las fases 1-3 y de la cadena local aceptada.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — **ya modificado** en este worktree:
  dos `produceState` (local + generada), `animateFloatAsState` sobre
  `RecipeArtworkImagePrecedence.crossfadeTarget` y render por capas (`Box` + `matchParentSize`).
- `app/src/main/java/com/sazon/app/ui/artwork/{GeneratedRecipeArtworkSource,RecipeArtworkImagePrecedence,DishImageArtworkPolicy,DishImageArtworkLoader,RecipeImageDecoder,AndroidRecipeImageDecoder}.kt`
  — **ya creados** en este worktree (seam `fun interface` + default no-op + CompositionLocal;
  precedencia pura genérica; política pura; loader genérico con LRU por clave canónica + `Semaphore`;
  costura `fun interface` + decodificador Android).
- `app/src/main/java/com/sazon/app/ui/artwork/RecipePhotoSource.kt` — seam de la foto local,
  **intacto** (el seam nuevo es paralelo, no lo generaliza).
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — `imageFor(request): ByteArray?`,
  `Mutex` por clave, `null`/vacío = no disponible, `CancellationException` relanzada; **no se modifica**.
- `app/src/main/java/com/sazon/app/data/image/{DishImageCachePolicy,FileDishImageCache,MemoryDishImageCache,AndroidDishImageCache}.kt`
  — piezas que se **cablean** (no se modifican).
- `app/src/main/java/com/sazon/app/data/ai/image/{DevExpertDishImageGenerator,DishImagePolicy,DishImageReducer,AndroidDishImageReducer}.kt`
  — adaptador real (ya instanciado en `MainActivity` en este worktree) y el patrón de costura que
  replica el decodificador. `DishImagePolicy.TIMEOUTS` (10/30/40 s) + `MAX_ATTEMPTS = 1` son el
  **timeout propio** de esta feature (Decisión 8).
- `app/src/main/java/com/sazon/app/domain/image/{DishImageGenerator,DishImageRequest,DishImageCacheKey}.kt`
  — puerto (`suspend fun generate(request): ByteArray?`), request compuesto por
  `DishImageRequest.of(taxonomy, recipe)` y clave canónica; **no se modifican** (el decorador del
  presupuesto implementa el puerto).
- `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` — patrón de **LRU de bitmaps**
  (`LinkedHashMap` en modo acceso, 16) y `null` en vez de excepción; **no se modifica**.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — **ya cableado** en este worktree (generador +
  resolutor + loader + decodificador + `LocalGeneratedRecipeArtworkSource`); aquí entra el presupuesto.
- Los 5 puntos de uso: `ui/home/RecipeCard.kt`, `ui/results/ResultRecipeCard.kt`,
  `ui/favorites/FavoriteRecipeCard.kt` (2 usos), `ui/detail/RecipeDetailScreen.kt` (`DetailHero`).
  **Ninguno cambia.**
- `app/build.gradle.kts` — `testImplementation` tiene `junit`, `kotlinx-coroutines-test`,
  `ktor-client-mock`, `robolectric` y `androidx.test:core`; **no** hay deps de test de Compose
  (`ui-test-junit4` solo en `androidTestImplementation`). Por eso la lógica testeable se **extrae** de
  `@Composable`.
- `DESIGN.md` — "Generated Concept Images" (excepciones de capturas #77/#85/#86/#91: el precedente
  exacto de esta desviación) y "Components → Imagen de receta".
- `docs/technical-discovery.md` (líneas 531-579), `docs/domain/taxonomy.md` §6,
  `docs/risks-and-open-questions.md` (línea 144) — estado durable a actualizar.

### Existing Patterns To Follow

- **Seam Compose**: `fun interface` + `staticCompositionLocalOf` con default **seguro** (no-op), para
  previews, tests y componentes aislados.
- **Lógica pura en JVM, costura Android aislada en un único fichero** (`AndroidDishImageReducer`).
- **Política en un único sitio**, pura y determinista, con topes **inyectables** (`AiProviderPolicy`,
  `DishImagePolicy`, `DishImageCachePolicy`): es exactamente el patrón del presupuesto por sesión.
- **Decorador sobre el puerto del dominio** para gobernar el generador sin modificar ni el puerto ni el
  adaptador real (misma filosofía que `BudgetedDishImageGenerator`).
- **LRU en memoria** con `LinkedHashMap(accessOrder = true)` (patrón `AssetRecipePhotoSource`).
- **Degradación silenciosa**: `null` = no disponible; nunca excepción por datos/red/códec;
  `CancellationException` relanzada.
- **Test JUnit 4** en `app/src/test/java/com/sazon/app/...`, nombres en español con `_`; Robolectric
  `@Config(sdk = [33])`/`@GraphicsMode(NATIVE)` solo para lo Android gráfico.
- **Verificación canónica** `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- **El mecanismo del tope por sesión no existe**: no hay ningún contador de generaciones ni guardarraíl
  de presupuesto en el repo (`grep` de "budget/presupuesto/tope por sesión" en `app/src` → 0). Es el
  delta principal de esta spec.
- **Nota de estado del worktree (inspección 2026-10-13)**: la rama `feature/dish-image-progressive-ui`
  ya contiene la implementación de `ui/artwork` (seam, precedencia, política, loader, decodificadores),
  el cambio de `RecipeArtwork.kt`, el cableado de `MainActivity` y las suites
  `RecipeArtworkImagePrecedenceTest`, `GeneratedRecipeArtworkSourceTest`, `DishImageArtworkLoaderTest` y
  `AndroidRecipeImageDecoderTest`, alineados con este contrato **salvo** el tope por sesión. El plan de
  implementación es **idempotente**: los pasos 1-6 verifican/ajustan lo existente; el paso nuevo es el
  presupuesto (más sus tests y los docs). Si se ejecuta desde un árbol limpio, los mismos pasos crean
  todo desde cero.
- La decodificación de los bytes WebP y su LRU de bitmaps **ya existen** en `ui.artwork`
  (`AndroidRecipeImageDecoder` + `DishImageArtworkLoader`); la fase 3 lo dejó a esta fase y está
  cubierto.

## Technical Approach

### 1. Un segundo seam, sin tocar el existente

`LocalRecipePhotoSource` (foto local, instantánea) se queda como está. El seam paralelo de la imagen
**generada** vive en `ui.artwork` (`GeneratedRecipeArtworkSource` + `NoGeneratedRecipeArtworkSource` +
`LocalGeneratedRecipeArtworkSource`, default no-op). `RecipeArtwork` lee los dos locals y lanza **dos**
`produceState` independientes (claves `recipe.id`/`recipe.categoryId`): la local se pinta sin esperar a
la generada y la generada, cuando llega, la sustituye. Salir de composición (o reciclar la tarjeta al
hacer scroll) cancela ambas corrutinas y `DishImageResolver` propaga la cancelación.

### 2. Precedencia y crossfade, puros y testables en JVM

`RecipeArtworkImagePrecedence` (puro, genérico para testear con `String`):
`enum RecipeArtworkImageLayer { FALLBACK, LOCAL, GENERATED }`,
`fun <T : Any> layerOf(local: T?, generated: T?): RecipeArtworkImageLayer` (la generada manda; la local
es el respaldo; sin ninguna, `FALLBACK`) y `fun <T : Any> crossfadeTarget(generated: T?): Float`
(`1f` solo cuando hay generada).

`DishImageArtworkPolicy` (puro, un único sitio): `MAX_CONCURRENT_GENERATIONS = 2`,
`CROSSFADE_MILLIS = 250`, `MAX_DECODED_IMAGES = 16`.

### 3. Carga y decodificación (una pieza genérica y JVM-testable)

`DishImageArtworkLoader<T : Any>(requestFor, resolve, decode, maxConcurrent, maxDecoded)` con
`suspend fun imageFor(recipe: Recipe): T?`. Contrato vinculante:

1. `request = requestFor(recipe)` (`DishImageRequest.of(taxonomy, recipe)` en la app); si el LRU de
   imágenes decodificadas tiene `request.cacheKey`, se devuelve **sin** llamar al resolutor ni
   decodificar.
2. `Semaphore(maxConcurrent).withPermit { resolve(request) }`: nunca más de **2** resoluciones en vuelo
   (la `Mutex` por clave de la fase 3 evita pagar dos veces el mismo plato).
3. `null` o array vacío → `null` **sin cachear** (se conserva la foto local).
4. `decode(bytes)` → `null` (códec roto) → `null` **sin cachear**.
5. Éxito → se cachea por **clave canónica** y se devuelve; `CancellationException` **se propaga**.

### 4. Decodificador Android aislado

`RecipeImageDecoder` (`fun interface`) + `AndroidRecipeImageDecoder` (único fichero de `ui.artwork` con
`android.graphics`: `BitmapFactory.decodeByteArray` → `asImageBitmap()`), `null` en vez de excepción.

### 5. `RecipeArtwork`: capas y crossfade (misma firma, misma geometría)

Dos `produceState`, `animateFloatAsState(crossfadeTarget(generated), tween(CROSSFADE_MILLIS))` y render
en `Box(modifier)` con capa base **siempre pintada** (foto local `ContentScale.Crop` o degradado +
glifo, `matchParentSize()`) y capa generada encima con alfa animado. Reglas no negociables: la capa
base **nunca** queda vacía (sin huecos ni frames en blanco), ambos `Image` con
`contentDescription = null`, y la firma/aspecto de los 5 llamantes intactos.

### 6. Cableado en `MainActivity` (composición explícita, sin DI)

Con el `aiClient`, la `taxonomy` y `applicationContext` ya existentes: generador real →
**`BudgetedDishImageGenerator`** (nuevo) → `DishImageResolver` (fase 3) → `DishImageArtworkLoader` →
`LocalGeneratedRecipeArtworkSource`. Se **reutiliza** el mismo `aiClient` (los timeouts de imagen se
aplican por petición, ADR 0005); sin red ni I/O en el arranque.

```kotlin
val dishImageGenerationBudget = DishImageGenerationBudget()   // uno por proceso = por sesión
val dishImageResolver = DishImageResolver(
    disk = FileDishImageCache(AndroidDishImageCache.root(applicationContext)),
    memory = MemoryDishImageCache(),
    generator = BudgetedDishImageGenerator(
        delegate = DevExpertDishImageGenerator(BuildConfig.DEVEXPERT_API_KEY, aiClient),
        budget = dishImageGenerationBudget,
    ),
)
val dishImageLoader = DishImageArtworkLoader(
    requestFor = { recipe -> DishImageRequest.of(taxonomy, recipe) },
    resolve = dishImageResolver::imageFor,
    decode = AndroidRecipeImageDecoder::decode,
)
```

### 7. Tope de generaciones por sesión (mecanismo; patrón `AiProviderPolicy`)

Nuevo en `com.sazon.app.data.image` (**un solo fichero**, puro, sin Android ni Compose). No es un
consumidor de la imagen: **no** llama al resolutor ni pinta; gobierna al generador desde el cableado.

```kotlin
// data/image/DishImageGenerationBudget.kt (puro)
class DishImageGenerationBudget(
    private val maxGenerations: Int = MAX_GENERATIONS_PER_SESSION,
) {
    companion object {
        /** Tope de generaciones por sesión; en un único sitio (patrón AiProviderPolicy). */
        const val MAX_GENERATIONS_PER_SESSION = 12
    }
    val used: Int
    val remaining: Int
    val exhausted: Boolean
    /** Reserva una unidad; `false` si el tope está agotado (el llamante NO invoca al generador). */
    fun tryAcquire(): Boolean
    /** Reinicia el contador (la "sesión" es la vida del proceso; lo expuesto para el hermano). */
    fun reset()
}

/** Decorador del puerto: `null` sin invocar al delegado cuando el presupuesto está agotado. */
class BudgetedDishImageGenerator(
    private val delegate: DishImageGenerator,
    private val budget: DishImageGenerationBudget,
) : DishImageGenerator {
    override suspend fun generate(request: DishImageRequest): ByteArray? =
        if (budget.tryAcquire()) delegate.generate(request) else null
}
```

Comportamiento (vinculante): una **unidad por invocación del generador** (intento = petición al
proveedor); reserva estricta con `tryAcquire` (thread-safe, p. ej. `AtomicInteger` con CAS o
`@Synchronized`) para que con concurrencia 2 no se rebase el tope; el decorador **no** captura
excepciones (la `CancellationException` se propaga); los aciertos de memoria/disco no pasan por aquí y
no consumen presupuesto. El valor por defecto (12) es ajustable: cubre dos pantallas de Resultados
(6 + 6) y deja el techo de sesión en ~24 % de la cuota semanal; el hermano puede exponerlo o
sustituirlo.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/artwork/GeneratedRecipeArtworkSource.kt` — crear (ya existe);
  seam (`fun interface` + default no-op + `LocalGeneratedRecipeArtworkSource`).
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtworkImagePrecedence.kt` — crear (ya existe);
  precedencia y objetivo de crossfade, puros y genéricos.
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkPolicy.kt` — crear (ya existe);
  constantes (concurrencia 2, crossfade 250 ms, LRU 16).
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkLoader.kt` — crear (ya existe); LRU por
  clave canónica + `Semaphore(2)` + degradación `null` + cancelación propagada.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeImageDecoder.kt` +
  `AndroidRecipeImageDecoder.kt` — crear (ya existen); costura + decodificador Android.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — modificar (ya modificado); segundo
  `produceState` y render por capas con crossfade. **Misma firma y geometría.**
- `app/src/main/java/com/sazon/app/data/image/DishImageGenerationBudget.kt` — **crear (NUEVO, pendiente)**;
  presupuesto por sesión (`tryAcquire`/`reset`/`used`/`remaining`/`exhausted`, tope inyectable) y el
  decorador `BudgetedDishImageGenerator` del puerto `DishImageGenerator`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar (ajustar); envolver el generador real
  con `BudgetedDishImageGenerator` (un `DishImageGenerationBudget` por proceso).
- `app/src/test/java/com/sazon/app/ui/artwork/RecipeArtworkImagePrecedenceTest.kt` — crear (ya existe);
  Scenario 2.
- `app/src/test/java/com/sazon/app/ui/artwork/DishImageArtworkLoaderTest.kt` — crear (ya existe);
  Scenarios 3-7 (fuentes falsas, `T = String`, sin red ni cuota).
- `app/src/test/java/com/sazon/app/ui/artwork/GeneratedRecipeArtworkSourceTest.kt` — crear (ya existe);
  el default no-op devuelve `null`.
- `app/src/test/java/com/sazon/app/ui/artwork/AndroidRecipeImageDecoderTest.kt` — crear (ya existe);
  Robolectric: WebP reales → `ImageBitmap`, basura → `null`.
- `app/src/test/java/com/sazon/app/data/image/DishImageGenerationBudgetTest.kt` — **crear (NUEVO,
  pendiente)**; Scenario 9 (tope respetado sin invocar al delegado, aciertos de caché sin consumo,
  `reset()`, cancelación propagada).
- `docs/technical-discovery.md` — modificar; la cadena de resolución en runtime gana el nivel generado,
  `DishImageResolver`/adaptador de imagen pasan a tener consumidor, `ui.artwork` decodifica + LRU y el
  presupuesto por sesión gobierna el generador.
- `docs/adr/0005-ai-dish-images.md` — modificar; estado "Aceptado parcialmente (fases 1-4)" con la nota
  del split (integración + mecanismo del tope aquí; interruptor/gestión visible en el hermano).
- `DESIGN.md` — modificar; **excepción de capturas** (ver abajo) y cadena de imagen en runtime +
  crossfade estándar.
- `docs/domain/taxonomy.md` §6 — modificar; la resolución visual vigente gana el nivel generado delante
  de la cadena local (lo anunciaba la fase 3).
- `docs/risks-and-open-questions.md` — modificar; el gasto de cuota **se materializa** (hay consumidor):
  mitigación implementada (caché + concurrencia 2 + **mecanismo** del tope por sesión) y pendiente la
  gestión visible del tope + interruptor (hermano) y el set semilla (fase 7).
- `PROGRESS.md` y `feature_list.json` — al implementar/validar (estado y evidencia); **no** los toca el
  planner.

**No se toca**: `data/**` salvo el fichero nuevo `data/image/DishImageGenerationBudget.kt`,
`domain/**`, `ui/**` salvo `ui/artwork/**`, los 5 puntos de uso y sus contenedores,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `app/src/androidTest/**`,
`AndroidManifest.xml`, la Taxonomía, `seed-recipes.json`, `assets/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **sí, comportamiento visual** (se cambia de dónde sale la imagen y cómo se sustituye),
  pero **`area: integration`** y **sin diseño nuevo**.
- Design source: `DESIGN.md` (mandan sus tokens y reglas); la transición viene de la propuesta §6.7
  ("transición suave … para que el reemplazo no 'salte'").
- Screens or states affected: las **ya aceptadas** `results-screen` (`resultados`),
  `recipe-detail-screen` (`detalle-receta`), `favorites` (`favoritos`), y la Home (`home-offline-blocks`,
  backfill pendiente). Ninguna pantalla nueva; ningún estado nuevo (la base nunca queda vacía).
- **Capturas: DESVIACIÓN AUTORIZADA — no se crea `docs/design/concepts/dish-image-progressive-ui/` ni
  ninguna subcarpeta.** Motivo (misma clase que `recipe-artwork` #77, `favorites-sort-controls` #85,
  `profile-stats-heading` #86 y `home-recently-opened-group` #91): este slice **no introduce layout,
  geometría, microcopy ni estado visual nuevos**; reutiliza pantallas ya aceptadas y se limita a
  **cambiar la fuente** de la imagen (local → generada) con un fade estándar. La dirección visual
  aplicable son las capturas ya aceptadas (`results-screen/resultados`,
  `recipe-detail-screen/detalle-receta`, `favorites/favoritos`) más la sección **Components → Imagen de
  receta** de `DESIGN.md`, contra las que la persona revisa el acabado en el emulador tras el merge.
- **Obligatorio**: el implementer registra la excepción en `DESIGN.md` ("Generated Concept Images",
  mismo formato que las excepciones de #77/#85/#86/#91, citando esta feature como desviación
  documentada) y anota la cadena en runtime y el crossfade estándar (250 ms, un único sitio) en
  "Components → Imagen de receta".
- Nota de gate: la desviación de capturas está **autorizada por la persona** (2026-10-13) para esta
  feature, por lo que el gate de capturas queda cubierto por el registro en `DESIGN.md`; el `planner`
  no crea carpetas fuera de `docs/specs/`.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — nuevo nivel de resolución de imagen en runtime
  (`ui.artwork` → `data.image` → `data.ai.image`), consumidor real del puerto y de la caché,
  decodificación + LRU de `ImageBitmap` en UI y el presupuesto por sesión (`data.image`).
- `docs/adr/0005-ai-dish-images.md`: **update** — "Aceptado parcialmente (fases 1-4)"; la fase 4 se
  entrega partida (integración + mecanismo del tope aquí; interruptor y gestión visible en
  `dish-image-settings-toggle`) y la cadena local pasa formalmente a respaldo.
- `DESIGN.md`: **update** — excepción de capturas (desviación documentada) + cadena de imagen y
  crossfade.
- `docs/domain/taxonomy.md` §6: **update** — la resolución visual vigente incluye el nivel generado
  delante de la cadena de assets (lo difería la fase 3).
- `docs/risks-and-open-questions.md`: **update** — el riesgo de cuota/latencia se materializa; la
  mitigación del tope por sesión pasa a **implementada (mecanismo)** y la pendiente es su gestión
  visible + el interruptor (hermano) y el set semilla (fase 7).
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados nuevos (la imagen no es
  un campo de `Recipe`, ADR 0004).
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `docs/ai-dish-images-proposal.md`: **not needed** — documento de propuesta; `feature_list.json` y el
  ADR son la fuente de verdad del estado.
- `docs/design/concepts/`: **not needed** — desviación autorizada (arriba).

## Implementation Plan

1. Verificar/crear el seam (`GeneratedRecipeArtworkSource` + default no-op) y la precedencia/crossfade
   puros con `RecipeArtworkImagePrecedenceTest`.
2. Verificar/crear `DishImageArtworkPolicy` (concurrencia 2, crossfade 250, LRU 16).
3. Verificar/crear `DishImageArtworkLoader` (LRU por clave canónica, `Semaphore(2)`, `null` sin cachear,
   cancelación propagada) y `DishImageArtworkLoaderTest`.
4. Verificar/crear `RecipeImageDecoder` + `AndroidRecipeImageDecoder` y su test Robolectric.
5. Verificar/modificar `RecipeArtwork` (segundo `produceState` + render por capas con crossfade),
   **sin** cambiar firma, geometría ni los 5 puntos de uso.
6. **Crear `DishImageGenerationBudget` + `BudgetedDishImageGenerator`** (puros, thread-safe, tope
   inyectable) y hacer pasar `DishImageGenerationBudgetTest` (Scenario 9).
7. Cablear/ajustar `MainActivity` envolviendo el generador real con el decorador del presupuesto (un
   presupuesto por proceso) y proveer `LocalGeneratedRecipeArtworkSource`.
8. Inspeccionar pureza/alcance (greps del Verification Plan).
9. Actualizar `DESIGN.md` (excepción de capturas), `docs/technical-discovery.md`, el ADR 0005,
   `docs/domain/taxonomy.md` §6 y `docs/risks-and-open-questions.md`.
10. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
    independiente es de otro rol).

## Implementation Tasks

- [ ] Verificar `GeneratedRecipeArtworkSource` (`fun interface`), `NoGeneratedRecipeArtworkSource` y
      `LocalGeneratedRecipeArtworkSource` (default no-op).
- [ ] Verificar `RecipeArtworkImagePrecedence` (genérico, puro: `layerOf`, `crossfadeTarget`).
- [ ] Verificar `DishImageArtworkPolicy` (`MAX_CONCURRENT_GENERATIONS = 2`, `CROSSFADE_MILLIS = 250`,
      `MAX_DECODED_IMAGES = 16`).
- [ ] Verificar `DishImageArtworkLoader` (LRU por clave canónica, `Semaphore(2)`, degradación `null`,
      `CancellationException` relanzada, sin `Log`).
- [ ] Verificar `RecipeImageDecoder` + `AndroidRecipeImageDecoder` (único `android.graphics` de
      `ui/artwork`).
- [ ] Verificar `RecipeArtwork`: segundo `produceState` + capa base (nunca vacía) + capa generada con
      alfa animado; firma y geometría intactas.
- [ ] **Crear `DishImageGenerationBudget`** (`MAX_GENERATIONS_PER_SESSION = 12` en un único sitio,
      `tryAcquire`/`reset`/`used`/`remaining`/`exhausted`, tope inyectable, thread-safe) y
      **`BudgetedDishImageGenerator`** (decorador del puerto: `null` sin invocar al delegado cuando el
      presupuesto está agotado; cancelación propagada).
- [ ] Ajustar `MainActivity`: `BudgetedDishImageGenerator(DevExpertDishImageGenerator(...),
      DishImageGenerationBudget())` reutilizando `aiClient`, `taxonomy` y
      `AndroidDishImageCache.root(...)`.
- [ ] Escribir `DishImageGenerationBudgetTest` (Scenario 9) y verificar las suites existentes
      (`RecipeArtworkImagePrecedenceTest`, `GeneratedRecipeArtworkSourceTest`,
      `DishImageArtworkLoaderTest`, `AndroidRecipeImageDecoderTest`) con casos negativos que no pasan
      en vacío.
- [ ] Verificar por inspección firma/geometría de los 5 puntos de uso, pureza de `ui/artwork`, que el
      presupuesto no es un consumidor del pipeline y que `docs/design/concepts/` no cambia.
- [ ] Actualizar `DESIGN.md`, `docs/technical-discovery.md`, el ADR 0005, `docs/domain/taxonomy.md` §6 y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde y registrar la evidencia.
- [ ] Actualizar `feature_list.json` (a `passing` con evidencia, **sin tocar su `verification`**) y
      `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew lint`
  vía `scripts/gradle.sh`), **no** arranca procesos largos y **no** modifica `init.sh`.
- Tiradas focales:
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` → `BUILD SUCCESSFUL`.
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"` → `BUILD SUCCESSFUL`
    (incluye `DishImageGenerationBudgetTest`; regresión de las 34 pruebas de la fase 3).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline de referencia: **77 suites / 551 tests** (validación de
  `dish-image-cache`, sesión 091); el implementer **re-deriva** el número exacto en su HEAD y confirma
  el delta (5 suites nuevas: `RecipeArtworkImagePrecedenceTest`, `GeneratedRecipeArtworkSourceTest`,
  `DishImageArtworkLoaderTest`, `AndroidRecipeImageDecoderTest`, `DishImageGenerationBudgetTest`).
- `bash scripts/gradle.sh lint` → **0 errores** (los warnings de `GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion` y `OldTargetApi` son preexistentes, **ninguno**
  bajo `app/src`; su recuento exacto varía con la metadata de versiones del momento).
- Cobertura exigida por el `verification` de la feature (mapeo explícito):
  - "RecipeArtwork resuelve primero la imagen generada (resolver) y cae a la foto local si devuelve
    null" → `RecipeArtworkImagePrecedenceTest` (Scenario 2) + `DishImageArtworkLoaderTest`
    (Scenarios 3-4).
  - "cancelación al salir de pantalla o hacer scroll" → `DishImageArtworkLoaderTest` Scenario 5
    (`CancellationException` propagada) + semántica de `produceState` (cancelación al salir de
    composición, verificado por inspección).
  - "tope de generaciones por sesión respetado" → `DishImageGenerationBudgetTest` (Scenario 9).
  - "sin consumidores nuevos fuera de ui/artwork" → greps de abajo. **Interpretación**: el presupuesto
    (`data.image`) no es un consumidor (no llama al resolutor ni pinta; es un guardarraíl del
    generador); el único consumidor del pipeline de imagen generada es `ui.artwork` (+ `MainActivity`
    como raíz de composición, igual que hace hoy con `LocalRecipePhotoSource`).
- Inspección de pureza y alcance:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/ui/artwork` → solo
    `AndroidRecipeImageDecoder.kt`.
  - `grep -rn "Log\.\|filesDir\|getDatabasePath" app/src/main/java/com/sazon/app/ui/artwork` → 0.
  - `grep -rn "LocalGeneratedRecipeArtworkSource" app/src/main` → solo `ui/artwork/**` y
    `MainActivity.kt`; ningún punto de uso ni pantalla.
  - `grep -rn "DishImageResolver\|GeneratedRecipeArtworkSource" app/src/main/java/com/sazon/app/ui` →
    solo `ui/artwork/**` (ninguna pantalla consume el resolutor).
  - `git diff --name-only` solo con: `ui/artwork/**`, `MainActivity.kt`, el fichero nuevo
    `data/image/DishImageGenerationBudget.kt`, los tests nuevos/existentes de estas suites y los 5 docs
    previstos (+ `feature_list.json`/`PROGRESS.md`); 5 puntos de uso, `RecipePhotoSource.kt`, los
    ficheros aceptados de `data/**`, `domain/**`, `build.gradle.kts`, `gradle/libs.versions.toml`,
    `init.sh`, `scripts/**`, la Taxonomía, `seed-recipes.json`, `assets/**` intactos; **nada** en
    `docs/design/concepts/`.
  - Inspección visual del código de `RecipeArtwork.kt`: firma `(recipe, modifier, glyphSize)` y la
    geometría de los 5 llamantes intactas.
- Cobertura **E2E**: **no aplica**. El repo no tiene suite E2E persistente y AGENTS.md no automatiza UI
  de emulador; la cobertura canónica es el test JVM (más Robolectric para el decodificador) y la
  revisión visual/manual de la persona.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: con clave válida en el
  emulador, abrir Home/Resultados/Favoritos/Detalle y comprobar en claro y oscuro que (a) la foto local
  aparece al instante, (b) al llegar la generada se sustituye con un crossfade **sin saltos ni huecos**,
  (c) al hacer scroll rápido y salir de pantalla no hay crash ni parpadeo blanco, y (d) sin red (avión)
  se conserva la foto local sin ningún error.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de las tiradas focales (`ui.artwork.*` y `data.image.*`).
- Conteo parseado del XML del gate (suites/tests, 0 fallos/errores/saltados) con las 5 suites nuevas
  nombradas y la baseline re-derivada.
- Informe de lint (0 errores, ninguno bajo `app/src`).
- Resultado de los greps de inspección (Android solo en `AndroidRecipeImageDecoder.kt`; seam solo en
  `ui/artwork/**` + `MainActivity.kt`; sin `Log`/`filesDir`; 5 puntos de uso y `RecipePhotoSource.kt`
  intactos; `data.image` solo con el fichero nuevo; `docs/design/concepts/` sin cambios).
- Vectores concretos de los tests: `(b1, b2) → GENERATED`, `(b1, null) → LOCAL`, `(null, null) →
  FALLBACK`; `crossfadeTarget(null) == 0f` y `crossfadeTarget(b) == 1f`; resolutor llamado **una vez**
  para la misma clave canónica y **0** veces con LRU caliente; `resolve` → `null` produce `null` **sin**
  cachear; `decode` → `null` produce `null` sin cachear; máximo **2** resoluciones en vuelo con 3
  peticiones; `CancellationException` propagada al cancelar; decodificación real de WebP → `ImageBitmap`
  no nulo y bytes basura → `null` (Robolectric). **Tope**: con `MAX_GENERATIONS_PER_SESSION` inyectado
  `N`, las primeras `N` peticiones invocan al generador y la `N+1` devuelve `null` **sin invocarlo**;
  `used`/`remaining`/`exhausted` correctos; un acierto de caché no consume presupuesto; `reset()`
  restaura.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **El nivel generado va por delante de la cadena local, para toda receta visible** (cadena literal de
   la propuesta §2 y nota de la feature): se pide la imagen generada de cada receta visible y, si llega,
   sustituye a la foto local. **No** se filtra por origen (`seed`/`ai`) en este slice.
2. **La foto local se pinta sin esperar** a la generada: dos `produceState` independientes, no una
   secuencia que retrasaría hasta ~4 s.
3. **Crossfade por capas**: capa base siempre pintada (local o degradado+glifo) + capa generada con alfa
   animado. Se descarta `Crossfade` de Compose sobre un único estado porque la base puede quedar vacía
   entre transiciones; la base continua garantiza "sin huecos".
4. **Seam nuevo, no se generaliza `RecipePhotoSource`**: cambiar su contrato obligaría a reescribir los 5
   llamantes; un segundo `CompositionLocal` con default no-op mantiene previews/tests seguros.
5. **La decodificación y su LRU de bitmaps viven en `ui.artwork`** (decisión 7 del ADR 0005 / fase 3):
   `data.image` sigue sin `android.graphics`.
6. **Concurrencia limitada a 2** con un `Semaphore` en el loader; la `Mutex` por clave de la fase 3
   evita pagar dos veces el mismo plato.
7. **Fallo/`null` = degradación a la cadena local**, nunca excepción ni error de UI; la **cancelación se
   propaga** (los `produceState` cancelan al salir de composición o reciclar la tarjeta).
8. **Timeout propio = la política de imagen de la fase 2** (`DishImagePolicy.TIMEOUTS`: connect 10 s /
   socket 30 s / request 40 s, `MAX_ATTEMPTS = 1`), que es "más corta que la de chat" según la propuesta
   §6.6 y ya está aceptada. La costura **no** añade un segundo timeout en la UI: si el proveedor tarda,
   el propio timeout del transporte devuelve `null` (máx. 40 s) y se conserva la foto local ("si falla,
   **tarda** o no hay red"). Se descarta un abandono anticipado en la UI (p. ej. 10 s): cancelar una
   generación en curso ya pagada gasta cuota sin aportar nada (la foto local ya está pintada) e
   impediría cachear la imagen en disco para la próxima composición.
9. **Tope de generaciones por sesión: mecanismo aquí, gestión visible en el hermano** (ver "Scope
   Reconciliation"). Semántica: **una unidad por invocación del generador** (intento = petición al
   proveedor), reserva estricta (`tryAcquire`), `null` sin invocar al delegado cuando está agotado,
   aciertos de caché sin consumo, **sesión = vida del proceso** (sin persistencia; `reset()` expuesto
   para `dish-image-settings-toggle`). Valor por defecto `MAX_GENERATIONS_PER_SESSION = 12` en un único
   sitio (patrón `AiProviderPolicy`), inyectable en tests.
10. **Sin dependencias nuevas**: la lógica testeable en JVM se extrae de `@Composable` (no hay test de
    Compose en `testImplementation`).

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **Coste de cuota en recetas semilla**: la cadena literal genera también para las 12 recetas semilla,
  que ya tienen foto propia curada. El tope por sesión (12) y la caché limitan el gasto, pero si la
  persona lo prefiere, el refinamiento es barato (un predicado que omita la generación cuando
  `RecipePhotoAssets` resuelve foto propia); **no** se aplica por defecto para no desviarse del plan.
- **Un fallo del proveedor consume una unidad del tope**: se contabiliza el **intento** (es donde se
  puede gastar cuota y es la lectura conservadora del presupuesto). Consecuencia: una sesión sin red
  puede agotar el tope con fallos rápidos y, al volver la red, no se generará nada más hasta que
  empiece un proceso nuevo. Aceptado como guardarraíl de presupuesto; si molestara, el hermano puede
  refinar la semántica (reembolsar intentos fallidos) sin cambiar la costura.
- **Cancelación al hacer scroll puede "perder" una generación ya enviada**: si se cancela a mitad de la
  petición HTTP, la cuota ya está gastada y el disco no guarda nada; un scroll de ida y vuelta puede
  repetirla (y consumir otra unidad del tope). Aceptado: la cancelación es requisito de la fase.
- **La clave canónica ignora el origen de la receta**: dos recetas (semilla y `ai`) con el mismo título
  comparten imagen (decisión del ADR 0005, frontera tipográfica).
- **Compositar dos imágenes por tarjeta** durante el crossfade añade coste de dibujo en rejillas con
  scroll; el crossfade dura 250 ms y la capa base no se decodifica dos veces (LRU).
- **Los dos LRU de bitmaps** (`AssetRecipePhotoSource` para la local, `DishImageArtworkLoader` para la
  generada) no se unifican (non-goal); 16 entradas cada uno.
- **`cacheDir` puede vaciarlo el sistema**: un acierto perdido regenera (si queda presupuesto) y vuelve
  a gastar cuota (comportamiento aceptado por la fase 3).
- **El valor del tope (12) es una primera estimación**: ajustable por la persona; vive en un único sitio
  precisamente para cambiarlo sin tocar la costura.

## Validator Checklist

- [ ] Alcance: solo `ui/artwork/**`, `MainActivity.kt`, el fichero nuevo
      `data/image/DishImageGenerationBudget.kt` (+ tests y docs); **sin** interruptor, **sin** gestión
      visible del tope (Ajustes/persistencia), **sin** UI/pantalla/microcopy nueva, **sin** modificar
      ficheros aceptados de `data/**`, `domain/**`, `RecipePhotoSource.kt`, los 5 puntos de uso, build,
      lockfile, `init.sh` ni dependencias.
- [ ] `RecipeArtwork` conserva la firma `(recipe, modifier, glyphSize)` y la geometría aceptada de los 5
      puntos de uso (16:10, 96x96/alto 96, hero 240); los llamantes no cambian.
- [ ] La capa base **nunca** queda vacía: con generada = `null` se ve la foto local o, si tampoco hay, el
      degradado + glifo de siempre; ningún error de UI.
- [ ] La generada se resuelve **solo** vía `DishImageResolver` (memoria → disco → generador) y se
      **degrada a `null`** sin cachear ante fallo/`null`/códec roto; `CancellationException` se propaga.
- [ ] Con la misma clave canónica el resolutor se invoca **una vez** (LRU + `Mutex` por clave) y nunca
      hay más de **2** resoluciones en vuelo.
- [ ] **El tope de generaciones por sesión está implementado y respetado** (Scenario 9): tope en un
      único sitio (patrón `AiProviderPolicy`), contador por sesión de proceso, imposición estricta en el
      borde de generación (`BudgetedDishImageGenerator` devuelve `null` **sin invocar** al delegado
      agotado), aciertos de caché sin consumo y `reset()` disponible para el hermano.
- [ ] El presupuesto **no** es un consumidor del pipeline (no llama al resolutor ni pinta): el único
      consumidor nuevo sigue siendo `ui.artwork` (+ `MainActivity` como raíz de composición).
- [ ] `docs/design/concepts/` **no** cambia y `DESIGN.md` registra la excepción de capturas con el mismo
      formato que #77/#85/#86/#91.
- [ ] `docs/technical-discovery.md`, el ADR 0005, `docs/domain/taxonomy.md` §6 y
      `docs/risks-and-open-questions.md` están actualizados y coherentes (fase 4 partida: integración +
      mecanismo del tope aquí; interruptor y gestión visible en el hermano; cadena local como respaldo).
- [ ] Escenarios 1-9 cubiertos por tests JVM/Robolectric (incluidos los negativos: `null`, códec roto,
      cancelación, concurrencia, tope agotado) o justificados por inspección/prueba manual según
      AGENTS.md.
- [ ] `feature_list.json` y `PROGRESS.md` reflejan estado y evidencia; el `verification` de la feature
      queda **intacto** (su frase del tope se cumple de verdad) y el implementer no hizo commit, push ni
      PR.
