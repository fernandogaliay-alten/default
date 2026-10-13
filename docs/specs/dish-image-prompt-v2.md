# Feature Implementation Spec: Prompt de imagen v2 y diagnóstico del origen de la imagen

## Source Feature

- `id`: `dish-image-prompt-v2`
- `area`: `integration`
- `depends_on`: `["dish-visual-descriptor-wiring", "dish-image-seed"]` (ambas satisfechas: `accepted`,
  sesión 097 y 096)
- `status`: `not_started` (en el momento de planificar; issue #111)
- `source`: `feature_list.json` (entrada `dish-image-prompt-v2`, líneas 1187-1199) +
  `docs/ai-dish-images-proposal.md` (§4.1/§4.2/§6 y §8) + `docs/adr/0005-ai-dish-images.md`
  (regla durable de invalidación por `VERSION`)
- Verificación exigida (textual de la feature):
  1. `./gradlew test` cubre el nuevo template del prompt (título + descripción/ingredientes +
     composición + negativos), determinista y nunca en blanco, y el bump de
     `DishImageCacheKey.VERSION` a `dish-image-v2` (clave nueva que invalida lo cacheado).
  2. El diagnóstico del origen resuelto (GENERATED / SEED / CATEGORY / GLYPH) está disponible solo en
     depuración y no cambia el comportamiento.
  3. `bash init.sh` verde; sin dependencias nuevas; sin UI nueva (sin capturas).

## Goal

Corregir la **calidad del prompt de imagen** y **hacer observable** de dónde sale la imagen que se
pinta, tras la prueba real de la persona (2026-10-13): algunas imágenes generadas **no cuadran con el
plato** y una misma foto se repite tres veces sin que hoy se pueda saber por qué.

Dos cambios acotados:

1. **`DishImagePrompt` v2.** La plantilla deja de describir el plato con **una sola** cláusula y pasa a
   llevar **siempre** el **título del plato como identidad**, la **descripción visual del descriptor**
   como detalle (fase 5, ya entregada en runtime por `dish-visual-descriptor-wiring`), los
   **ingredientes clave** de la receta, una cláusula de **composición** (ración completa, primer plano,
   fondo neutro) y **negativos reforzados**. Además se refuerza la instrucción del contrato de
   `PromptBuilder` para que el modelo emita un `imagePrompt` **más rico** (ingredientes y aspecto del
   plato terminado, no una categoría).
2. **Invalidación coherente.** Cambiar la plantilla **obliga** (ADR 0005, decisión 4) a subir
   `DishImageCacheKey.VERSION` a `dish-image-v2`: toda clave cacheada cambia, la caché de disco anterior
   queda huérfana (`LRU` la evicta) y los **assets semilla** se renombran a la clave v2 (los bytes ya
   empaquetados siguen siendo válidos: **no** se vuelve a pagar cuota).

Y, como **diagnóstico de debug** (sin cambio de comportamiento), el **origen resuelto** de la imagen
pintada (`GENERATED` / `SEED` / `CATEGORY` / `GLYPH`) queda en logcat **solo en debug**, para poder
distinguir "generada", "asset semilla", "foto local (categoría)" y "glifo" cuando una imagen se repite.

No se toca el puerto, el transporte, la caché, la cadena local de `recipe-photos`, el almacén del
descriptor ni la UI más allá de emitir el diagnóstico.

## Non-Goals

- **No** se cambia la **arquitectura** de imagen: puerto `DishImageGenerator`, `DishImageRequest` (sin
  campos nuevos), `data.ai.image` (adaptador, política, wire, reducción, `/images/edits`),
  `data.image` (`FileDishImageCache`, `MemoryDishImageCache`, `DishImageCachePolicy`,
  `AndroidDishImageCache`, `DishImageSettings`, `DishImageSettingsPolicy`,
  `DishImageGenerationBudget`, `GatedDishImageGenerator`) y `DishVisualDescriptorStore` quedan intactos
  salvo `DishImageResolver` (un hook de diagnóstico).
- **No** se toca la **cadena local** de `recipe-photos` (`data.RecipePhotoAssets`,
  `data.AssetRecipePhotoSource`) ni el **set semilla** más allá del **renombrado** de sus 6 ficheros y
  del espejo del script: `data.DishImageSeedAssets` y `data.AssetDishImageSeedSource` **no** cambian
  (salvo comentarios KDoc con ejemplos `dish-image-v1-`).
- **No** se **vuelve a preparar** el set con el proveedor: el renombrado es **sin red y sin cuota**.
  Regenerarlo (`--force`) queda como decisión futura de la persona, no de esta feature.
- **No** se toca el **dominio de recetas** ni la seguridad: `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipeIds.kt`, `AiRecipeDraft.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`, `RecipeGenerator` y
  `data/ai/**` (chat) no cambian.
- **No** hay **UI nueva**, ni pantalla, ni microcopy, ni gesto, ni estado visual nuevos: el diagnóstico
  es **una línea de log** (no altera el pintado) y el cambio de prompt no es visual de pantalla. **No**
  se crea `docs/design/concepts/dish-image-prompt-v2/` (**el gate de capturas no aplica**).
- **No** se cambia el **proveedor**, el modelo, el endpoint, el tamaño, los timeouts ni el número de
  intentos de imagen.
- **No** se implementan otras correcciones: el **tope de 12 por sesión** y el **manejo de `429`**
  (posible causa del "fallback a categoría") son una **feature correctiva separada**, pendiente de
  decisión (nota de la entrada de `feature_list.json`).
- **No** se implementa limpieza de las claves/ficheros v1 huérfanos (caché de disco ni assets).
- **No** hay dependencias nuevas ni cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/gradle.sh`, assets que no sean el renombrado, `docs/domain/taxonomy.json`,
  `seed-recipes.json`, `AndroidManifest.xml`, `DESIGN.md` ni `docs/design/**`.
- **No** se reescriben las **specs ni las validaciones ya aceptadas** (son registro histórico).

## Job Story

Cuando abro una receta y la foto que veo no representa el plato (o se repite la misma en varias
tarjetas),
quiero que el prompt pida siempre el plato con su título, sus ingredientes y una composición clara, y
poder ver en logcat de dónde salió esa imagen (generada, semilla, categoría o glifo),
para entender qué está pasando y que las imágenes nuevas sí cuadren con el plato — sin gastar cuota
extra ni romper la caché ni el modo offline.

## Users And Permissions

- **Persona / desarrollador**: es el único beneficiario del diagnóstico; con `BuildConfig.DEBUG` activo
  (build de debug) ve `SazonDishImage` en logcat. En release **no** hay diagnóstico.
- **Usuario local**: **no** interactúa de forma nueva; el interruptor "Imágenes del plato" sigue
  gobernando la generación y las imágenes anteriores se regeneran solas al cambiar la clave (la caché
  v1 queda huérfana y se evicta por LRU).
- **Sistema**: la composición del prompt y la clasificación del origen son **puras y deterministas**;
  ninguna de las dos toca red, disco, cuota ni el pintado.

## Acceptance Scenarios

### Scenario 1: el prompt v2 lleva identidad, ingredientes, composición y negativos

Given la Taxonomía real (por `taxonomy.asset.path`) y una receta **sin** descriptor con título
`"Arroz con pollo criollo"`, cocina `spanish` y tres ingredientes no-despensa,
When se construye `DishImagePrompt.build(taxonomy, recipe)`,
Then el prompt empieza por `PROMPT_PREFIX` + el **título** como identidad, incluye la etiqueta real de
la cocina, los **ingredientes clave**, [`COMPOSITION`], [`STYLE_SUFFIX`] y [`NEGATIVES`]; es
**determinista** (misma entrada → misma cadena, también con dos instancias de `Taxonomy`), **nunca**
queda en blanco, no contiene `,,` ni espacios dobles y [`STYLE_SUFFIX`] aparece **exactamente una vez**.

### Scenario 2: con descriptor, el título sigue siendo la identidad y el `imagePrompt` es el detalle

Given la misma receta con un descriptor `imagePrompt = "arroz amarillo con pollo desmenuzado, guisantes
y zanahoria"`,
When se construye el prompt,
Then **contiene el título de la receta** (identidad, siempre) **y** el `imagePrompt` colapsado como
detalle, además de ingredientes, composición, estilo y negativos; el `imagePrompt` **ya no sustituye**
al título (cambio deliberado respecto a la fase 5).

### Scenario 3: ingredientes clave (tope, orden, sin despensa) — caso negativo incluido

Given una receta con más de [`MAX_KEY_INGREDIENTS`] ingredientes no-despensa, uno de ellos repetido y
varios `isPantry = true` (y otra receta **sin** ingredientes),
When se construye el prompt,
Then se añaden como mucho [`MAX_KEY_INGREDIENTS`] nombres distintos **en el orden de la receta**, los de
despensa quedan fuera y, si no hay ninguno, la cláusula de ingredientes **se omite** sin dejar comas ni
espacios sobrantes.

### Scenario 4: degradación por título/cocina/categoría se mantiene (sin regresión)

Given recetas con título vacío (con categoría real, con categoría desconocida) y con cocina vacía o
desconocida, y una Taxonomía vacía,
When se construye el prompt,
Then la identidad degrada `título → etiqueta de categoría real → DEFAULT_DISH`, la cláusula de cocina
se **omite** cuando el id no existe o está vacío (nunca un marcador de "desconocida"), no aparece
`DEFAULT_DISH` cuando hay categoría real, el resultado nunca queda en blanco y sigue conteniendo
composición, estilo y negativos.

### Scenario 5: `VERSION` sube a v2 y las claves nuevas invalidan las v1

Given `DishImageCacheKey`,
When se inspecciona `VERSION` y se derivan claves de un mismo plato,
Then `VERSION == "dish-image-v2"`, las claves tienen la forma `dish-image-v2-<slug>` con el mismo
alfabeto `[a-z0-9-]` y la misma independencia de `Locale`, y la clave v2 de un plato **difiere** de su
clave v1 (dos entradas de caché distintas: lo cacheado queda invalidado).

### Scenario 6: los assets semilla se resuelven por la clave v2 (renombrado, sin cuota)

Given los 6 platos del set de `dish-image-seed` y el directorio
`app/src/main/assets/img/dishes/seed/`,
When se deriva la clave canónica de cada título con `DishImageCacheKey.of(título)` (ahora v2) y se
resuelve el asset,
Then cada plato resuelve un asset **existente y no vacío** con el nombre `dish-image-v2-<slug>.<ext>`,
hay **un** asset por plato y el total sigue ≤ 2 MiB; los ficheros son **byte-idénticos** a los v1
(renombrado, sin regeneración) y `EXPECTED_SEED_KEYS` del script de preparación ya está en v2.

### Scenario 7: el contrato de `PromptBuilder` pide un `imagePrompt` más rico

Given el bloque de sistema de `PromptBuilder.build(taxonomy, request)`,
When se inspecciona el texto,
Then el `OUTPUT_CONTRACT` sigue conteniendo `dishKey` e `imagePrompt` (opcionales) y la instrucción
describe `imagePrompt` como una **descripción visual del plato terminado** (ingredientes principales y
aspecto/color/textura, sin texto ni manos), breve y de una línea; el resto del contrato y la
enumeración de ids no cambian y el prompt sigue siendo determinista.

### Scenario 8: clasificación del origen (pura, las 4 capas)

Given la pareja de capas (foto local presente/ausente, imagen generada presente/ausente) y el origen
que registró el resolutor,
When se clasifica con `RecipeArtworkImagePrecedence.originOf(local, generated, resolvedOrigin)`,
Then `generated != null` → `SEED` si el resolutor registró `SEED` y `GENERATED` en cualquier otro caso
(la generada **manda**); sin generada y con foto local → `CATEGORY`; sin ninguna de las dos → `GLYPH`;
el clasificador es **puro** y determinista (JVM, con `String` como `T`).

### Scenario 9: el sumidero de orígenes es de sesión y tolerante

Given `DishImageOriginSink`,
When se registran orígenes por `recipeId` (incluidos `null`/en blanco y valores no soportados por el
resolutor) y se consulta `originFor`,
Then `originFor` devuelve el último origen registrado para un id utilizable, `null` para id
`null`/en blanco/desconocido, los ids en blanco se **ignoran** (no crean entradas), con el mismo id
**gana el último**, dos instancias no comparten estado y **nada** se escribe en disco.

### Scenario 10: el resolutor reporta GENERATED / SEED sin cambiar el contrato

Given un `DishImageResolver` con un callback `onResolved` de grabación, un generador falso y una fuente
semilla falsa,
When se resuelve una clave que está en el set semilla y otra que solo tiene generador (o copia en
disco),
Then el callback reporta `SEED` para la primera (también en un **acierto de memoria** posterior sobre
los mismos bytes semilla) y `GENERATED` para la segunda; con el callback por **defecto** (no-op) el
resolutor devuelve **exactamente** los mismos bytes y el mismo orden de llamadas que hoy (sin
regresión); un fallo **no** reporta nada y la `CancellationException` sigue propagándose.

### Scenario 11: el diagnóstico es solo de debug y no cambia el pintado

Given `MainActivity` con `BuildConfig.DEBUG` en `false` (release) y en `true` (debug),
When se inspecciona el árbol de composición y el código,
Then el diagnóstico de origen se provee **solo** con `BuildConfig.DEBUG = true` (en release el default
seguro no produce ninguna línea) y ninguna rama del pintado de `RecipeArtwork` depende de él: mismo
fallback, mismo crossfade, misma geometría y mismo número y orden de peticiones de imagen.

### Scenario 12: alcance y pureza (inspección)

Given el diff de la feature,
Then `domain/image/**` no importa Android ni red (solo añade constantes y texto), `data.ai.image/**`,
la caché, `DishImageSettings`/gate/tope, `data.DishImageSeedAssets` (salvo KDoc),
`data.AssetDishImageSeedSource`, `data.RecipePhotoAssets`, `data.AssetRecipePhotoSource`,
`DishVisualDescriptorStore`, `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `data/ai/**` (chat),
build, `gradle/libs.versions.toml`, `init.sh`, `scripts/**` (salvo el espejo del set),
`docs/domain/taxonomy.json`, `seed-recipes.json` y `DESIGN.md` **no** cambian; sin dependencias nuevas;
**no** existe `docs/design/concepts/dish-image-prompt-v2/`; los 6 assets del set están **renombrados**
(no regenerados) y las specs aceptadas/`docs/validations/**` no se reescriben.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-image-prompt-v2`, líneas 1187-1199) — alcance literal, notificación
  de la causa probable del síntoma y el pendiente separado del tope/`429`.
- `docs/ai-dish-images-proposal.md` §4.1 (descriptor del modelo), §4.2 (prompt y estilo, «coletilla de
  estilo fija en un único lugar»), §6 (no hacer esperar, degradación silenciosa), §8 (cuota, imagen
  distinta para el mismo prompt) y §11 — el diseño de origen y la razón de subir `VERSION`.
- `docs/adr/0005-ai-dish-images.md` — decisión 4 (`VERSION` es el **único** mecanismo de invalidación)
  y las consecuencias ya escritas: subir la versión deja huérfanos los `dish-image-v1-*` y **cambia los
  nombres de los assets semilla** (obliga a «volver a preparar el set»).
- `app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt` — plantilla actual
  `<PROMPT_PREFIX> <plato>[, <cocina>], <STYLE_SUFFIX>`, degradación del `<plato>`
  (`imagePrompt → título → categoría → DEFAULT_DISH`) y `singleLine`.
- `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` — `VERSION = "dish-image-v1"`,
  `normalize`/`of(dishName)`/`of(recipe, descriptor)`; la clave es el nombre del asset semilla.
- `app/src/main/java/com/sazon/app/domain/image/DishVisualDescriptor.kt` — `dishKey`/`imagePrompt`
  (limpieza con `of`); su KDoc dice que `imagePrompt` **sustituye** la cláusula del plato: deja de ser
  exacto con v2.
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — `DishImageRequest.of` (único
  punto de composición) y el puerto (contrato de fallo `null`); **no** se toca.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `title`, `categoryId`, `cuisineId` e
  `ingredients: List<RecipeIngredient>` (`name`, `isPantry`) son las fuentes puras del prompt v2.
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — `OUTPUT_CONTRACT` y la línea de
  instrucción de los campos opcionales (línea 109).
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — la cadena
  `memoria → disco → semilla → generador → persistir`, con la `Mutex` por clave y el contrato de fallo;
  único punto donde se sabe si los bytes salieron de la **semilla** o del camino de **generación**.
- `app/src/main/java/com/sazon/app/data/image/{File,Memory}DishImageCache.kt` y
  `DishImageCachePolicy.kt` — el nombre de fichero **es** la clave (`<cacheKey>.webp`) y la LRU/tope de
  80 MiB: la invalidación por `VERSION` deja huérfanos los `dish-image-v1-*` hasta la evicción.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — las **dos capas** (base local +
  generada con crossfade) y los dos seams; único sitio donde se sabe **qué capa ganó**.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtworkImagePrecedence.kt` — lógica **pura** de
  `layerOf`/`crossfadeTarget` (JVM, genérica `T : Any`): hogar natural del clasificador de origen.
- `app/src/main/java/com/sazon/app/ui/artwork/{RecipePhotoSource,GeneratedRecipeArtworkSource}.kt` —
  los dos seams con default seguro (`null`) y sus CompositionLocals.
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkLoader.kt` — LRU de decodificadas +
  `Semaphore` de 2; **no** cambia.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 76-208) — el **composition root**: crea el
  `DishVisualDescriptorStore`, compone `requestFor`, el resolutor con `seed`, el proveedor del seam con
  `generatedIfEnabled` y el patrón de log ya existente (`Log.d("SazonAi", …)`, `verboseDiagnostics =
  BuildConfig.DEBUG`).
- `app/src/main/java/com/sazon/app/data/image/DishVisualDescriptorStore.kt` — precedente exacto de
  **estado de sesión puro JDK en memoria** (`record`/`descriptorFor`, ignora claves en blanco).
- `app/src/main/java/com/sazon/app/data/DishImageSeedAssets.kt` +
  `AssetDishImageSeedSource.kt` — convención `img/dishes/seed/<cacheKey>.<ext>` y la resolución por
  clave canónica (el renombrado a v2 las deja funcionando sin cambios de código).
- `scripts/prepare-dish-image-seed.sh` (líneas 28-33 y 89-132) — `EXPECTED_SEED_KEYS` **hardcodeadas en
  v1**, el espejo de `PROMPT_PREFIX`/`STYLE_SUFFIX` y la validación final (una clave por plato,
  `[a-z0-9-]`, extensión admitida, ≤ 2 MiB): debe pasar a v2 o el script falla con "asset ajeno a la
  lista".
- Tests afectados por el bump: `domain/image/DishImageCacheKeyTest.kt` (línea 149 `VERSION` + vectores),
  `DishImagePromptTest.kt` (plantilla exacta y los escenarios 7/7c/8, que asumen que `imagePrompt`
  sustituye al título), `domain/image/DishImageRequestTest.kt` (línea 106),
  `data/DishImageSeedAssetsTest.kt` (línea 31 y el caso real),
  `data/ai/DishVisualDescriptorWiringTest.kt` (líneas 58/85/89/109/165),
  `ui/artwork/RecipeArtworkImagePrecedenceTest.kt` y `domain/prompt/PromptBuilderTest.kt`.
- `docs/domain/taxonomy.md` §4 (líneas 130-145, descriptor) y §6 (líneas 182-195, estrategia visual),
  `docs/technical-discovery.md` (líneas 532-557 y 618, 757), `docs/risks-and-open-questions.md`
  (línea 145) — docs durables que citan la plantilla, `VERSION` o los nombres v1.
- `docs/specs/dish-visual-descriptor.md`, `docs/specs/dish-visual-descriptor-wiring.md` y
  `docs/specs/dish-image-seed.md` — estilo, nivel de detalle y vectores v1 (registro histórico que
  **no** se reescribe).

### Existing Patterns To Follow

- **Lógica pura y determinista** en `domain.image` (JDK, sin Android, sin red, sin estado, sin reloj):
  `DishImagePrompt`/`DishImageCacheKey`; las constantes de plantilla viven en un **único sitio**.
- **Clasificación pura genérica** en `ui.artwork` (`RecipeArtworkImagePrecedence` con `T : Any`),
  testeada en JVM con `String` y sin Compose.
- **Estado de sesión en memoria, puro JDK, en `data.image`** (`DishVisualDescriptorStore`,
  `DishImageSettings`): modelo del sumidero de orígenes (no en `domain.image`, documentado **sin
  estado**).
- **Costura aditiva con default** para no romper constructores ni tests (`onEvent`, `onDescriptors`,
  `seed`): patrón del hook `onResolved` del resolutor.
- **Diagnóstico de debug gobernado por `BuildConfig.DEBUG`** y emitido desde `MainActivity`
  (`Log.d("SazonAi", …)`): mismo patrón para el log `SazonDishImage`.
- **Assets versionados como fuente de verdad** con la clave canónica como **nombre** (fase 7): el
  renombrado no toca código, solo ficheros y la lista del script.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo. Tests JUnit 4 en JVM, nombres en español con `_`.

### Current Gaps

- La plantilla v1 describe el plato con **una** cláusula: si el modelo no emite `imagePrompt`, el prompt
  no aporta ingredientes, composición ni negativos reforzados (probable causa de imágenes que no
  cuadran).
- `DishImageCacheKey.VERSION` sigue `dish-image-v1`: un prompt nuevo **no** se aplicaría a los platos ya
  cacheados (la caché se indexa por clave, no por prompt).
- El **origen** de la imagen pintada no es observable: `DishImageResolver` sabe si sirvió la semilla o
  el generador, `RecipeArtwork` sabe si ganó la generada o la foto local, y **nada** une ambos datos;
  hoy un `null` del generador (timeout, `429`, tope de 12 por sesión) es indistinguible de un acierto de
  categoría en logcat.
- Ningún test cubre que la clave v2 resuelve los assets semilla renombrados (lo cubrirá
  `DishImageSeedAssetsTest`, que hoy afirma el vector v1).

## Technical Approach

### 1. `DishImagePrompt` v2 (solo cambia la plantilla; la firma no)

`domain.image.DishImagePrompt` mantiene `build(taxonomy, recipe, descriptor: DishVisualDescriptor? = null)`
y gana constantes nuevas (mismo `object`, un único sitio):

| Constante | Valor propuesto |
|---|---|
| `PROMPT_PREFIX` | `"Fotografía gastronómica de"` (sin cambios) |
| `DEFAULT_DISH` | `"plato casero"` (sin cambios) |
| `COMPOSITION` | `"una ración completa del plato en primer plano, sobre fondo neutro"` |
| `STYLE_SUFFIX` | `"cerámica artesanal, luz natural suave, estilo editorial gastronómico"` (los negativos salen de aquí) |
| `NEGATIVES` | `"sin texto, sin manos, sin personas, sin cubiertos ni marcas de agua, sin collage ni varios platos, sin fondo recargado"` |
| `MAX_KEY_INGREDIENTS` | `5` |

Plantilla v2 (identidad **siempre**, después los detalles):

```
<PROMPT_PREFIX> <identidad>[, <cocina>]: <detalle>, <ingredientes clave>, <COMPOSITION>, <STYLE_SUFFIX>, <NEGATIVES>
```

- `<identidad>` = **título** de la receta; si está vacío, la etiqueta real de su `categoryId`; y si
  tampoco, `DEFAULT_DISH` (la degradación de la fase 1 se conserva).
- `<cocina>` = etiqueta real de la cocina; la cláusula se **omite** si el id no existe o está vacío.
- `<detalle>` = `descriptor.imagePrompt` (colapsado con `singleLine`); si no hay, no aporta segmento.
  **Ya no sustituye** al título: es el detalle visual del plato terminado.
- `<ingredientes clave>` = hasta `MAX_KEY_INGREDIENTS` nombres de ingredientes **no despensa**
  (`!isPantry`), **distintos** y **en el orden de la receta**, unidos por `, `; se omite si no hay.
- El ensamblado une los segmentos **no vacíos** con `", "` y normaliza las comas repetidas
  (`Regex("(\\s*,\\s*)+") -> ", "`) para garantizar que no aparece `,,` ni un espacio doble; el
  resultado **nunca** queda en blanco y contiene `STYLE_SUFFIX` **una sola vez**.

### 2. Invalidación: `VERSION = "dish-image-v2"` y assets semilla renombrados

- `DishImageCacheKey.VERSION` pasa a `"dish-image-v2"` (KDoc actualizado); `normalize`, `of(dishName)`,
  `of(recipe, descriptor)` y `FALLBACK_KEY` **no** cambian.
- Consecuencias (documentadas, no implementadas): los `dish-image-v1-*.webp` de `cacheDir/dish-images`
  quedan huérfanos hasta que la LRU los evicta (acotado por el tope de 80 MiB).
- Los **6 assets** `app/src/main/assets/img/dishes/seed/dish-image-v1-*.webp` se **renombran** a
  `dish-image-v2-*.webp` con `git mv` (**bytes idénticos**, `md5` sin cambios): el renombrado **no**
  cuesta cuota ni red, y el asset ya era (y sigue siendo) la foto correcta de ese plato. Alternativa
  **descartada en esta feature**: re-preparar con `scripts/prepare-dish-image-seed.sh --force`
  (~2 % de cuota por plato, ~12 % en total) — queda como decisión de la persona.
- `scripts/prepare-dish-image-seed.sh`: `EXPECTED_SEED_KEYS` pasa a v2 (**obligatorio**: la validación
  final compara el directorio con esa lista y fallaría con "asset ajeno a la lista") y su **espejo** del
  prompt se pone al día (título como identidad + `COMPOSITION` + `NEGATIVES`; las cláusulas de
  `imagePrompt` y de ingredientes **no** aplican al script, que solo compone título+cocina: se anota en
  un comentario como deriva conocida y acotada).

### 3. Contrato de `PromptBuilder` (instrucción más rica)

En `OUTPUT_CONTRACT`/instrucción (línea 109) se pide un `imagePrompt` **más rico**: descripción visual
del **plato terminado**, con sus **ingredientes principales** y su **aspecto** (color, textura,
guarnición), breve y en una línea; se mantiene **opcional** y se aclara que la app usará el título y la
taxonomía si falta. `dishKey` no cambia. El bloque `system` sigue dependiendo solo de la Taxonomía y el
prompt sigue siendo determinista.

### 4. Diagnóstico del origen (solo debug, sin cambio de comportamiento)

Cuatro valores pedidos: `GENERATED` (camino de generación, o copia en memoria/disco de bytes
generados), `SEED` (asset del set semilla), `CATEGORY` (foto local empaquetada de `recipe-photos`; la
etiqueta cubre propia/categoría/por defecto, porque el vocabulario pedido no las separa y el seam local
no lo expone) y `GLYPH` (degradado + glifo).

- **`data.image.DishImageOrigin`** (enum, puro): los 4 valores con su KDoc.
- **`data.image.DishImageOriginSink`** (puro, `ConcurrentHashMap`, sin Android ni persistencia; patrón
  `DishVisualDescriptorStore`): `record(recipeId: String?, origin)` (ignora ids en blanco; gana el
  último) y `originFor(recipeId: String?): DishImageOrigin?`.
- **`DishImageResolver`**: parámetro nuevo **al final con default** `onResolved: (DishImageRequest,
  DishImageOrigin) -> Unit = { _, _ -> }`, invocado con `SEED` cuando sirve la fuente semilla y con
  `GENERATED` en memoria/disco/generador; para que un **acierto de memoria** sobre bytes semilla siga
  reportando `SEED`, el resolutor recuerda el origen por clave en un mapa pequeño al poblar la memoria
  (default `GENERATED`). El contrato de devolución y el orden de la cadena **no** cambian.
- **`RecipeArtworkImagePrecedence.originOf(local, generated, resolvedOrigin)`** (puro, genérico):
  `generated != null` → `SEED` si `resolvedOrigin == SEED`, `GENERATED` en otro caso; si no,
  `local != null` → `CATEGORY`; si no, `GLYPH`.
- **`ui.artwork.DishImageOriginDiagnostics`** (interface) + `NoDishImageOriginDiagnostics` (no-op) +
  `LocalDishImageOriginDiagnostics` (default seguro): `resolvedOriginFor(recipe)` y
  `onPainted(recipe, origin)`. `RecipeArtwork` clasifica y notifica (una vez por par `local`/`generated`)
  **sin** que ninguna rama de pintado dependa del diagnóstico.
- **`MainActivity`**: crea el sumidero, le pasa `onResolved = { request, origin ->
  dishImageOrigins.record(request.recipeId, origin) }` al resolutor y provee el diagnóstico **solo** si
  `BuildConfig.DEBUG`, emitiendo `Log.d("SazonDishImage", "origin=$origin id=${recipe.id}
  title=${recipe.title}")`; en release el default no-op no emite nada. No se registra nada más (sin
  claves, sin URLs, sin bytes).

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt` — **modificar**; plantilla v2,
  constantes `COMPOSITION`/`NEGATIVES`/`MAX_KEY_INGREDIENTS`, ingredientes clave y normalización de
  comas.
- `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` — **modificar**; `VERSION =
  "dish-image-v2"` y KDoc.
- `app/src/main/java/com/sazon/app/domain/image/DishVisualDescriptor.kt` — **KDoc**; `imagePrompt` es
  el **detalle**, no un sustituto del título (comportamiento y firma intactos).
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — **modificar**; instrucción del
  `imagePrompt` más rica (contrato y ids intactos).
- `app/src/main/java/com/sazon/app/data/image/DishImageOrigin.kt` — **crear**; enum puro.
- `app/src/main/java/com/sazon/app/data/image/DishImageOriginSink.kt` — **crear**; sumidero de sesión
  puro.
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — **modificar**; `onResolved` +
  origen recordado por clave (sin cambio de contrato).
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtworkImagePrecedence.kt` — **modificar**;
  `originOf`.
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageOriginDiagnostics.kt` — **crear**; interface,
  no-op y CompositionLocal.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — **modificar**; emite el diagnóstico
  (sin cambio visual).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — **modificar**; sumidero, `onResolved` y
  diagnóstico debug (`BuildConfig.DEBUG`).
- `app/src/main/assets/img/dishes/seed/dish-image-v2-*.webp` — **renombrar** (6 ficheros, mismos bytes).
- `scripts/prepare-dish-image-seed.sh` — **modificar**; `EXPECTED_SEED_KEYS` v2 y espejo del prompt.
- `app/src/test/java/com/sazon/app/domain/image/DishImagePromptTest.kt` — **modificar**; escenarios 1-4 y
  la plantilla exacta; reescribir los escenarios 7/7c/8 de la fase 5 (el título ya no desaparece).
- `app/src/test/java/com/sazon/app/domain/image/DishImageCacheKeyTest.kt` — **modificar**; `VERSION` y
  vectores a v2.
- `app/src/test/java/com/sazon/app/domain/image/DishImageRequestTest.kt` — **modificar**; vector de
  clave.
- `app/src/test/java/com/sazon/app/data/DishImageSeedAssetsTest.kt` — **modificar**; vector v2 y el caso
  real de assets renombrados.
- `app/src/test/java/com/sazon/app/data/image/DishImageOriginSinkTest.kt` — **crear**; escenario 9.
- `app/src/test/java/com/sazon/app/data/image/DishImageResolverTest.kt` — **modificar**; escenario 10
  (semilla → `SEED`, generado/disco → `GENERATED`, default no-op sin regresión).
- `app/src/test/java/com/sazon/app/ui/artwork/RecipeArtworkImagePrecedenceTest.kt` — **modificar**;
  escenario 8.
- `app/src/test/java/com/sazon/app/data/ai/DishVisualDescriptorWiringTest.kt` — **modificar**; vectores
  v2.
- `app/src/test/java/com/sazon/app/domain/prompt/PromptBuilderTest.kt` — **modificar**; aserción de la
  instrucción del `imagePrompt`.
- Todos los literales `dish-image-v1-` que queden en `app/src/test/**` (claves opacas en
  `DishImageResolverTest`, `DishImageArtworkLoaderTest`, `FileDishImageCacheTest`,
  `DishImageSettingsTest`, `DishImageCachePolicyTest`, `AndroidDishImageCacheTest`,
  `AndroidDishImageReducerTest`, `DevExpertDishImageGeneratorTest`) — **actualizar a v2** por coherencia
  (no son puerta).
- KDoc con ejemplos v1: `data/DishImageSeedAssets.kt`, `data/DishImageSeedSource.kt`,
  `data/image/DishImageCachePolicy.kt` — **actualizar** (solo comentario).
- `docs/adr/0005-ai-dish-images.md`, `docs/technical-discovery.md`, `docs/domain/taxonomy.md` §4/§6 y
  `docs/risks-and-open-questions.md` — **modificar** (ver "Durable Documentation Impact").
- `PROGRESS.md` y `feature_list.json` — **modificar** al implementar/validar; los toca el implementer,
  **no** el planner.

**No se toca**: `domain/image/DishImageGenerator.kt`, `data/ai/image/**`, `data/ai/**` (chat),
`data/image/{FileDishImageCache,MemoryDishImageCache,DishImageCachePolicy,AndroidDishImageCache,
DishImageSettings,DishImageGenerationBudget,GatedDishImageGenerator,DishVisualDescriptorStore}.kt`,
`data/DishImageSeedAssets.kt` (salvo KDoc), `data/AssetDishImageSeedSource.kt`,
`data/RecipePhotoAssets.kt`,
`data/AssetRecipePhotoSource.kt`, `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`,
`ui/artwork/{DishImageArtworkLoader,RecipePhotoSource,GeneratedRecipeArtworkSource,RecipeArtworkStyle}.kt`,
el resto de `ui/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**` (salvo
el espejo del set), `docs/domain/taxonomy.json`, `seed-recipes.json`, `AndroidManifest.xml`,
`DESIGN.md`, `docs/design/**`, las specs aceptadas y `docs/validations/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). No hay pantalla, componente, microcopy, icono ni estado
  visual nuevos; el diagnóstico es **una línea de logcat** y el cambio de prompt no altera el layout ni
  la imagen ya pintada (solo afecta a las generaciones futuras).
- Design source: no aplica (`DESIGN.md` no cambia).
- Screens or states affected: ninguna; mismo fallback, mismo crossfade de 250 ms y misma geometría de
  los 5 puntos de uso.
- New design artifact required: **no**.
- **El gate de capturas no aplica**: **no** se crea `docs/design/concepts/dish-image-prompt-v2/` ni
  ninguna otra carpeta bajo `docs/design/concepts/` (mismo criterio que `dish-image-domain`,
  `dish-image-cache`, `dish-visual-descriptor`, `dish-style-reference` y `dish-image-seed`).
- `RecipeArtwork.kt` **sí** cambia (emite el diagnóstico), pero **ninguna** de sus ramas de pintado
  depende de él: es una llamada lateral con default no-op.

## Durable Documentation Impact

- `docs/adr/0005-ai-dish-images.md`: **update** — la decisión 4 pasa a `dish-image-v2` y se registra el
  alcance de esta feature (prompt v2 con identidad+detalle+ingredientes+composición+negativos,
  invalidación de la caché, assets semilla **renombrados** sin cuota) y el diagnóstico de debug del
  origen. La regla durable (subir `VERSION` al cambiar la plantilla) se mantiene y se cumple por primera
  vez.
- `docs/technical-discovery.md`: **update** — la plantilla del bullet de `domain.image` (líneas 539-542)
  pasa a v2; `VERSION` pasa a `dish-image-v2` (líneas 543-546); los ejemplos v1 (líneas 618 y 757) y el
  bullet de la fase 7 se actualizan; se añade el diagnóstico de origen (resolutor → sumidero → artwork →
  log `SazonDishImage`, solo debug).
- `docs/domain/taxonomy.md` §4: **update** — `imagePrompt` es la **descripción visual del plato
  terminado** que se usa como **detalle** (el título de la receta sigue siendo la identidad del prompt);
  se pide más rico (ingredientes y aspecto).
- `docs/domain/taxonomy.md` §6: **update** — la cadena de imagen cita la plantilla v2 y que la clave es
  `dish-image-v2-…`; los assets semilla llevan el nombre de la clave v2.
- `docs/risks-and-open-questions.md`: **update** — los ejemplos `dish-image-v1-*` pasan a v2 y se anota
  que el cambio de plantilla (v1→v2) **invalida** la caché de disco y **renombra** los assets semilla
  (coste cero en cuota) y que el fallback a la foto de categoría ya es observable en debug.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados (la imagen sigue
  resolviéndose por convención y no es campo de `Recipe`).
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `DESIGN.md` / `docs/design/**`: **not needed** — sin UI ni dirección visual nuevas.
- `docs/ai-dish-images-proposal.md`: **not needed** — sigue siendo la propuesta no autoritativa.
- Specs aceptadas y `docs/validations/**`: **not needed** (registro histórico de las fases 1-7);
  `feature_list.json`/`PROGRESS.md`: **update** al implementar/validar (implementer).

## Implementation Plan

1. `DishImagePrompt` v2 + sus constantes; reescribir `DishImagePromptTest` (plantilla exacta, identidad
   siempre presente, ingredientes, negativos, degradación, casos negativos y los escenarios 7/7c/8
   actualizados) y poner en verde la tirada focal.
2. `DishImageCacheKey.VERSION = "dish-image-v2"`; actualizar `DishImageCacheKeyTest`,
   `DishImageRequestTest`, `DishVisualDescriptorWiringTest` y los literales v1 de los tests de imagen.
3. Renombrar con `git mv` los 6 assets a `dish-image-v2-<slug>.webp`, actualizar
   `EXPECTED_SEED_KEYS` y el espejo del prompt en `scripts/prepare-dish-image-seed.sh` y actualizar
   `DishImageSeedAssetsTest` (vector v2 + assets reales + ≤ 2 MiB).
4. Instrucción del `imagePrompt` en `PromptBuilder` + aserción en `PromptBuilderTest`; KDoc de
   `DishVisualDescriptor`.
5. `DishImageOrigin` + `DishImageOriginSink` + su test (escenario 9) y `originOf` en
   `RecipeArtworkImagePrecedence` + su test (escenario 8).
6. `onResolved` en `DishImageResolver` (con el origen recordado por clave) + ampliar
   `DishImageResolverTest` (escenario 10, incluido el default no-op sin regresión).
7. `DishImageOriginDiagnostics` + `LocalDishImageOriginDiagnostics`, la notificación en `RecipeArtwork`
   y el cableado en `MainActivity` (sumidero + `onResolved` + proveedor solo con `BuildConfig.DEBUG`).
8. Inspección de alcance y pureza (greps del Verification Plan), `bash init.sh` en verde y evidencia.
9. Actualizar los cuatro docs durables y dejar la feature `passing` (la validación independiente es de
   otro rol).

## Implementation Tasks

- [ ] Reescribir `DishImagePrompt` con la plantilla v2 (`COMPOSITION`, `NEGATIVES`, `STYLE_SUFFIX` sin
      negativos, `MAX_KEY_INGREDIENTS`), identidad **siempre** (título → categoría → `DEFAULT_DISH`),
      detalle del descriptor, ingredientes clave no-despensa y normalización de comas/espacios.
- [ ] Ampliar/reescribir `DishImagePromptTest`: determinismo, nunca en blanco, sin `,,` ni dobles
      espacios, `STYLE_SUFFIX` una vez, cocina real/omitida, categoría/`DEFAULT_DISH`, ingredientes
      (tope/orden/distintos/sin despensa/omisión) y los escenarios 7/7c/8 con el **título presente**.
- [ ] Subir `DishImageCacheKey.VERSION` a `dish-image-v2` y actualizar su KDoc.
- [ ] Actualizar los vectores v1→v2 en `DishImageCacheKeyTest`, `DishImageRequestTest`,
      `DishVisualDescriptorWiringTest` y los literales opacos de los tests de imagen.
- [ ] `git mv` los 6 assets `img/dishes/seed/dish-image-v1-*.webp` → `dish-image-v2-*.webp` (bytes
      idénticos; verificar `md5`) y actualizar `EXPECTED_SEED_KEYS` + el espejo del prompt en
      `scripts/prepare-dish-image-seed.sh`.
- [ ] Actualizar `DishImageSeedAssetsTest` (vector `dish-image-v2-arroz-con-pollo-criollo`, assets
      reales resueltos por la clave v2, ≤ 2 MiB) y los KDoc que citan `dish-image-v1-`.
- [ ] Enriquecer la instrucción de `imagePrompt` en `PromptBuilder` y añadir su aserción en
      `PromptBuilderTest`; ajustar el KDoc de `DishVisualDescriptor` (detalle, no sustituto).
- [ ] Crear `data/image/DishImageOrigin.kt` y `data/image/DishImageOriginSink.kt` (puros) y su test.
- [ ] Añadir `originOf` a `RecipeArtworkImagePrecedence` con su test (las 4 capas + precedencia).
- [ ] Añadir `onResolved: (DishImageRequest, DishImageOrigin) -> Unit = { _, _ -> }` a
      `DishImageResolver`, con el origen por clave para los aciertos de memoria, y cubrirlo en
      `DishImageResolverTest` (SEED/GENERATED, default no-op sin regresión, fallo sin reporte,
      cancelación propagada).
- [ ] Crear `ui/artwork/DishImageOriginDiagnostics.kt` (interface + no-op + CompositionLocal), notificar
      desde `RecipeArtwork` sin alterar el pintado y cablear en `MainActivity` el sumidero, `onResolved`
      y el proveedor **solo** con `BuildConfig.DEBUG`.
- [ ] Verificar por inspección que el resto de `domain/image/**`, `data/ai/**`, `data/ai/image/**`, la
      caché, `DishImageSettings`, `ui/**` (salvo `RecipeArtwork` y la precedencia), `Recipe.kt`,
      `RecipeValidator.kt`, `AiRecipeIds.kt`, build, `init.sh` y `seed-recipes.json` no cambian; sin
      dependencias nuevas; sin `docs/design/concepts/dish-image-prompt-v2/`.
- [ ] Actualizar el ADR 0005, `docs/technical-discovery.md`, `docs/domain/taxonomy.md` §4/§6 y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` en verde, registrar la evidencia y actualizar `feature_list.json` /
      `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red, **no** gasta cuota y
  **no** modifica `init.sh`.
- Focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.domain.image.*"
  --tests "com.sazon.app.domain.prompt.PromptBuilderTest" --tests "com.sazon.app.data.image.*"
  --tests "com.sazon.app.data.DishImageSeedAssetsTest"
  --tests "com.sazon.app.data.ai.DishVisualDescriptorWiringTest"
  --tests "com.sazon.app.ui.artwork.*" --rerun-tasks` → `BUILD SUCCESSFUL`.
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. El implementer **re-deriva** la baseline en su HEAD (última baseline aceptada
  conocida: **92 suites / 673 tests**, sesión 097) y confirma que el delta es el de las suites nuevas
  (`DishImageOriginSinkTest`) más las ampliadas.
- `bash scripts/gradle.sh lint` → **0 errores** y los **13 warnings preexistentes**
  (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`, `OldTargetApi`),
  **ninguno** bajo `app/src`.
- Asset del set (sin red, sin cuota): `md5sum` de los 6 ficheros antes y después del `git mv` →
  **idénticos**; `ls app/src/main/assets/img/dishes/seed/` → 6 ficheros `dish-image-v2-*.webp`;
  `grep -rn "dish-image-v1" app/src/main/assets` → **0**; `DishImageSeedAssetsTest` resuelve cada plato
  por `DishImageCacheKey.of(título)` y el total sigue ≤ 2 MiB.
- Inspección de alcance y pureza:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/domain/image` → **0**; tampoco
    `io.ktor`/`java.net` en `domain.image`; `DishImagePrompt.kt` sin `Log` ni reloj.
  - `grep -rn "import android" app/src/main/java/com/sazon/app/data/image` → **solo**
    `AndroidDishImageCache.kt` (el sumidero y el enum son JDK puro; el resolutor no gana imports
    Android).
  - `git diff --name-only` = la lista de "Expected File Changes" (sin `data/ai/image/**`, caché,
    `DishImageSettings`, `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, build, `init.sh`).
  - `grep -rn "dish-image-v1" app/src/main app/src/test docs/adr docs/technical-discovery.md
    docs/domain/taxonomy.md docs/risks-and-open-questions.md scripts` → **0** (los v1 solo sobreviven en
    specs/validaciones históricas y `docs/validations/**`).
  - `test -d docs/design/concepts/dish-image-prompt-v2` → **no existe**.
- Diagnóstico (solo debug, sin comportamiento): inspección de `MainActivity` (`BuildConfig.DEBUG`
  gobierna el proveedor del seam; en release queda el no-op) y de `RecipeArtwork` (ninguna rama de
  pintado lee el diagnóstico); escenarios 8-10 cubiertos en JVM (clasificador, sumidero y hook del
  resolutor) sin Android ni red.
- Cobertura E2E: **no aplica**. No hay suite E2E persistente y `AGENTS.md` no automatiza UI de
  emulador; el contrato verificable (plantilla, clave/versión, assets por clave v2 y clasificación de
  origen) se cubre en JVM, y lo puramente visual lo revisa la persona tras el merge.
- **Prueba manual de la persona** (tras el merge, no bloqueante y no automatizable desde WSL): con
  clave válida, generar/buscar algo con `imagePrompt` y comprobar en logcat `SazonDishImage` que el
  origen es `GENERATED` (o `SEED` para los 6 platos del demo) y `CATEGORY` cuando el proveedor falla o
  el tope de 12 se agota; comprobar que las imágenes nuevas se parecen más al plato.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de la tirada focal.
- Conteo parseado de los XML (suites/tests, 0 fallos/errores/saltados) con las suites nombradas
  (`DishImagePromptTest`, `DishImageCacheKeyTest`, `DishImageOriginSinkTest`,
  `RecipeArtworkImagePrecedenceTest`, `DishImageResolverTest`, `DishImageSeedAssetsTest`) y la baseline
  re-derivada.
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Vectores concretos del prompt: la cadena exacta del escenario 1; el escenario 2 con el título **y** el
  `imagePrompt`; un caso con ingredientes de despensa excluidos y con más de 5 ingredientes; ausencia de
  `,,`/dobles espacios; `STYLE_SUFFIX` una vez.
- Vector de versión: `DishImageCacheKey.VERSION == "dish-image-v2"` y
  `DishImageCacheKey.of("Arroz con pollo criollo") == "dish-image-v2-arroz-con-pollo-criollo"` (≠ su
  clave v1); assets resueltos por esa clave.
- `md5sum` de los 6 assets antes/después del renombrado (idénticos) e inventario (6 ficheros, ≤ 2 MiB).
- Vectores del diagnóstico: `originOf(null, img, null) == GENERATED`;
  `originOf(null, img, SEED) == SEED`; `originOf(img, null, null) == CATEGORY`;
  `originOf(null, null, null) == GLYPH`; sumidero (ignora ids en blanco, gana el último, `null` para
  desconocido).
- Resultado de los greps/diff de inspección, `test -d docs/design/concepts/dish-image-prompt-v2` y
  confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).
- Anotación en `feature_list.json` (evidencia + `passing`) y `PROGRESS.md`.

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementador no las reabre):

1. **El título es siempre la identidad y el `imagePrompt` pasa a ser el detalle.** Es lo que pide la
   persona ("incluir SIEMPRE el título como identidad + la descripción del descriptor como detalle") y
   corrige la causa probable de imágenes que no cuadran. *Coste asumido*: los escenarios 7/7c/8 de
   `docs/specs/dish-visual-descriptor.md` (que afirmaban lo contrario) se **reescriben**; es un cambio
   de contrato deliberado, no una regresión.
2. **La plantilla gana composición y negativos reforzados como constantes propias** (`COMPOSITION`,
   `NEGATIVES`) y `STYLE_SUFFIX` pierde la parte de negativos. Un único sitio para el estilo y el
   vocabulario visual (ADR 0005, decisión 3); el resultado sigue conteniendo `STYLE_SUFFIX` una vez.
3. **Ingredientes clave = hasta 5 nombres no-despensa, distintos y en el orden de la receta.** Puro,
   determinista y acotado; la despensa (sal/aceite/pimienta/agua) no aporta identidad y alargaría el
   prompt. El tope vive en una constante para poder ajustarlo en un único sitio.
4. **`VERSION` sube a `dish-image-v2` (obligatorio).** La caché se indexa por clave, no por prompt: sin
   el bump, ningún plato ya cacheado recibiría la plantilla nueva (regla durable del ADR 0005).
5. **Los assets semilla se renombran, no se regeneran.** Su identidad es la clave canónica y su
   contenido ya es la foto correcta del plato; re-prepararlos costaría ~12 % de cuota sin mejorar el
   resultado (la plantilla nueva solo afecta a generaciones futuras). El test del set (que deriva la
   clave con `of(título)`) **fuerza** el renombrado, así que un asset v1 huérfano no puede pasar
   desapercibido.
6. **El diagnóstico vive en `data.image` (enum + sumidero de sesión) y en `ui.artwork` (clasificador
   puro + seam).** El resolutor es el único que sabe `SEED` vs `GENERATED`; el artwork es el único que
   sabe qué capa ganó; el sumidero los une por `recipe.id` sin cambiar ninguna firma pública ni el
   contrato del resolutor. *Alternativas descartadas*: cambiar el retorno de
   `DishImageGenerator`/`DishImageResolver` (rompe el transporte puro y sus tests) o llevar el origen
   en los seams de Compose (cambia una API aceptada por un diagnóstico).
7. **`CATEGORY` agrupa toda la cadena local** (foto propia, de categoría o por defecto), porque el
   vocabulario pedido tiene 4 valores y `RecipePhotoSource` no expone la ruta; partir la cadena local
   exigiría abrir un seam aceptado. Se documenta en el KDoc del enum.
8. **El diagnóstico es de solo debug y no cambia comportamiento.** Se provee únicamente con
   `BuildConfig.DEBUG` (patrón `verboseDiagnostics` del chat), el default es no-op y ninguna rama de
   pintado, caché, cuota o red depende de él. El `Log.d` vive en `MainActivity` (root) con la etiqueta
   `SazonDishImage`; `domain.image`/`data.image` siguen **sin** `Log`.
9. **`DishImageResolver` mantiene su contrato**: `onResolved` es un parámetro nuevo **al final con
   default** no-op; los bytes devueltos, el orden de la cadena, la `Mutex` por clave y la propagación de
   `CancellationException` no cambian.
10. **No se limpian** los `dish-image-v1-*` de `cacheDir` ni los assets v1: la LRU del tope de 80 MiB
    los evicta (consecuencia ya documentada en el ADR 0005). La limpieza por prefijo es otra feature.
11. **Sin capturas**: `area: integration`; el cambio de prompt no es visual de pantalla y el
    diagnóstico es un log. **No** se crea `docs/design/concepts/dish-image-prompt-v2/`.
12. **El tope de 12 por sesión y el manejo del `429` quedan fuera**: son la otra correctiva probable
    (nota de `feature_list.json`) y necesitan su propia entrada y evidencia. Esta feature solo los hace
    **observables** vía el diagnóstico.

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **La longitud del prompt crece** (identidad + detalle + hasta 5 ingredientes + composición + estilo +
  negativos): es texto corto para un modelo de imagen y no se factura por longitud aquí, pero si el
  proveedor lo recortara o degradara la calidad, el ajuste es cambiar `NEGATIVES`/`MAX_KEY_INGREDIENTS`
  (una línea) y **volver a subir `VERSION`** (regla durable).
- **El proveedor puede seguir devolviendo imágenes poco apetitosas o equivocadas**: el prompt mejora la
  probabilidad, no la garantiza; el descarte de la imagen (autochequeo con visión, §7.4 de la
  propuesta) sigue fuera de alcance.
- **Coste de la invalidación**: cada plato ya cacheado se **regenera** una vez (~2 % de cuota) al pedirse
  por la clave v2; es el precio explícito de aplicar la plantilla nueva y queda acotado por la LRU y el
  tope por sesión.
- **El espejo del script** (`prepare-dish-image-seed.sh`) sigue siendo bash duplicado y **no puede**
  reproducir las cláusulas de `imagePrompt`/ingredientes (solo tiene título y cocina): la deriva es
  cosmética, está acotada por la regla de `VERSION` y se anota en el propio script.
- **`CATEGORY` no distingue propia/categoría/`default`**: si en la prueba manual hace falta ese detalle,
  la salida es abrir `RecipePhotoSource` (feature propia), no forzarlo aquí.
- **`RecipeArtwork` gana una llamada lateral**: si el `LaunchedEffect` se dispara de más, el
  diagnóstico repite líneas (molestia de logcat, no defecto); el clasificador y el sumidero son puros y
  baratos.
- **El renombrado de assets en git** debe conservar los bytes (usar `git mv`, no reescribir): si algún
  fichero se corrompe, el test del set falla (no pasa en vacío).
- **Viñeta de la fase 5**: el registro histórico (`docs/specs/dish-visual-descriptor*.md`,
  `docs/validations/**`) seguirá diciendo "`VERSION` sigue `dish-image-v1`": es correcto **para aquella
  feature**; la fuente de verdad del cambio es el ADR 0005 y `docs/technical-discovery.md`.

## Validator Checklist

- [ ] El alcance se respeta: **sin** dependencias nuevas, sin UI/capturas, sin tocar `Recipe.kt`,
      `RecipeValidator.kt`, `AiRecipeIds.kt`, `data/ai/**`, `data/ai/image/**`, la caché,
      `DishImageSettings`/gate/tope, `DishImageSeedAssets`/`AssetDishImageSeedSource`, la cadena de
      `recipe-photos`, `DishVisualDescriptorStore`, build, `init.sh` ni `seed-recipes.json`.
- [ ] Los 12 escenarios pasan, incluidos los negativos (sin título, categoría desconocida, cocina
      vacía, Taxonomía vacía, sin ingredientes no-despensa, `originFor` de id desconocido/en blanco,
      default no-op del resolutor).
- [ ] `DishImagePrompt.build` incluye **siempre** el título como identidad y, con descriptor, el
      `imagePrompt` como detalle; el resultado es determinista, nunca en blanco, sin `,,` ni dobles
      espacios y con `STYLE_SUFFIX` exactamente una vez (verificación exigida #1, primera mitad).
- [ ] `DishImageCacheKey.VERSION == "dish-image-v2"` y las claves v2 difieren de las v1 (invalidación);
      los 6 assets del set están renombrados a `dish-image-v2-*.webp` **byte-idénticos**, se resuelven
      por la clave canónica y el total sigue ≤ 2 MiB; `EXPECTED_SEED_KEYS` del script está en v2
      (verificación exigida #1, segunda mitad).
- [ ] El diagnóstico de origen (GENERATED / SEED / CATEGORY / GLYPH) es **puro y determinista**, se
      provee **solo** con `BuildConfig.DEBUG`, su default es no-op y **ninguna** rama de pintado,
      caché, cuota o red depende de él (verificación exigida #2).
- [ ] `onResolved` es aditivo con default y **no** cambia los bytes devueltos, el orden de la cadena ni
      la propagación de `CancellationException`; un fallo no reporta; un acierto de memoria sobre bytes
      semilla sigue reportando `SEED`.
- [ ] `com.sazon.app.domain.image` y `com.sazon.app.data.image` (salvo `AndroidDishImageCache`) no
      importan Android ni red y no añaden `Log`; los tests corren en JVM sin dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint) con la baseline re-derivada y la evidencia registrada
      (verificación exigida #3).
- [ ] Los cuatro docs durables (ADR 0005, `docs/technical-discovery.md`, §4/§6 de
      `docs/domain/taxonomy.md`, `docs/risks-and-open-questions.md`) están actualizados y coherentes con
      `dish-image-v2` y con el diagnóstico de debug.
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica) y no se reescriben specs
      ni validaciones aceptadas.
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR.
