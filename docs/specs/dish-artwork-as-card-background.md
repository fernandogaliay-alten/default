# Feature Implementation Spec: La imagen generada pasa a capa de fondo de la tarjeta y del hero (L2)

## Source Feature

- `id`: `dish-artwork-as-card-background`
- `area`: `integration`
- `depends_on`: `["recipe-card-typographic", "recipe-detail-typographic-header"]` (ambas `accepted`)
- `status`: `not_started` en el momento de planificar (cuarta de las cinco features del ADR 0006)
- `source`: `feature_list.json` + `docs/adr/0006-typographic-recipe-cards.md` (variante **L2**)
- Verificación exigida (textual): tests JVM de la precedencia y del gate (apagado = sin fondo y sin
  tocar caché/set semilla/generador; encendido = fondo si el resolver devuelve imagen, ficha
  tipográfica si falla, tarda, se cancela o se agota el tope por sesión); la geometría **no cambia** al
  aparecer la imagen y el fundido sigue en `DishImageArtworkPolicy.CROSSFADE_MILLIS`; `RecipeArtwork`
  conserva firma y geometría y los cinco puntos de uso siguen compilando con el mismo contrato;
  capturas de referencia obligatorias (existen, ver "Visual Design Impact"); `bash init.sh` verde y sin
  dependencias nuevas.
- **Capturas de referencia: ya aportadas** (2026-10-14). El planner **no** las pide ni las crea.

## Goal

Convertir la imagen de plato generada en la **capa de fondo opcional** del hueco de la tarjeta y del
hero del Detalle (variante **L2**), con la **ficha tipográfica como capa base** (es lo que garantiza
que no haya hueco, frame en blanco ni salto de layout): la categoría y el título se quedan **encima**
de la imagen, los ingredientes con cantidad y los metadatos pasan a una **banda sólida** y la imagen
entra con el **crossfade ya existente** (250 ms). `ui.artwork.RecipeArtwork` deja de ser "la imagen de
la receta" y pasa a ser el **proveedor de la capa de fondo**; conserva firma y geometría.

Con el ajuste **"Generar imágenes del plato" apagado no se consulta nada** (ni caché, ni set semilla,
ni generador): la tarjeta sigue siendo la ficha tipográfica aceptada. El mecanismo del interruptor y el
tope por sesión **ya existen** (`dish-image-settings-toggle`) y se respetan **tal cual**.

## Non-Goals

- **No** se implementa nada del interruptor ni del tope (ya existen): no se tocan
  `data/image/DishImageSettings.kt`, `DishImageGenerationBudget.kt`, la sección de Perfil ni el
  microcopy del tope.
- **No** se retira la cadena local de fotos (`data.RecipePhotoAssets`, `data.AssetRecipePhotoSource`,
  `ui.artwork.RecipePhotoSource`, los 31 assets): eso es `retire-recipe-photos` (depende de esta
  feature). Aquí la cadena simplemente **deja de ser leída** por la tarjeta/hero.
- **No** se toca la miniatura 96x96 de la lista de Favoritos (`RecipeMonogramTile`): ADR 0006,
  decisión 6 (monograma + regla terracota de 5px). Sin cambios.
- **No** hay pantalla, ruta, pestaña, microcopy ni estado de error nuevos; no hay shimmer, ni
  indicador de "generando", ni aviso de tope agotado en la tarjeta/hero (degradación **silenciosa**).
- **No** se cambia `RecipeDetailHeroFormat` (geometría publicada: 240/6/190/0,12f/3) ni el escalado de
  raciones, pestañas ni barra inferior del Detalle.
- **No** se modifican `domain/**`, `data/image/**` (aceptado), `data/ai/image/**`,
  `data.RecipePhotoAssets.kt`, `data/AssetRecipePhotoSource.kt`, la Taxonomía, `seed-recipes.json`,
  `assets/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
  `AndroidManifest.xml` ni `docs/design/concepts/**` (las capturas son de solo lectura).
- **No** hay dependencias nuevas. Sin telemetría nueva ni `Log` nuevo.

## Job Story

Cuando abro el Inicio, los Resultados, Favoritos o el Detalle con el ajuste de imágenes encendido,
quiero ver la imagen real de mi plato **detrás** de la tipografía (y seguir leyendo la ficha entera con
nitidez), y que si la imagen no llega —sin red, fallo, espera, cancelación o tope agotado— la ficha
tipográfica se quede tal cual, sin errores ni saltos,
para que la imagen aporte apetito sin restarme legibilidad ni fiabilidad.

## Users And Permissions

- **Usuario local**: enciende o apaga el ajuste en Perfil (ya existe) y ve el resultado en tarjetas y
  hero. No interactúa con nada nuevo. Sin permisos nuevos.
- **Desarrollador / agente**: ejercita la precedencia, el gate y la degradación en **JVM** con
  resolutores/decodificadores falsos, sin red, sin clave, sin cuota y sin emulador.
- **Persona**: revisa el acabado en el emulador tras el merge (AGENTS.md no automatiza UI), contra las
  capturas obligatorias.

## Acceptance Scenarios

### Scenario 1: apagado = sin fondo y sin tocar caché, set semilla ni generador

Given un `DishImageSettings` con el interruptor **apagado**, un `DishImageArtworkLoader` con un
`resolve` contador (y memorias/generador falsos contadores detrás),
When se compone la costura tal como la cablea `MainActivity`
(`settings.generatedIfEnabled { loader.imageFor(recipe) }`),
Then devuelve `null` con **0 invocaciones** del resolutor (0 lecturas de caché, 0 del set semilla, 0
del generador) y la capa resultante es `TYPOGRAPHIC` (ficha tipográfica, sin fondo).

### Scenario 2: encendido + imagen disponible = fondo con scrim y fundido

Given el interruptor **encendido** y un resolutor que devuelve bytes WebP válidos,
When se resuelve y decodifica la imagen de una tarjeta y de un hero visibles,
Then la capa es `GENERATED`, el alfa objetivo del fundido es `1f` (con
`DishImageArtworkPolicy.CROSSFADE_MILLIS` = 250 ms), la imagen llena el hueco con
`ContentScale.Crop` bajo el `scrim` de contraste, la categoría y el título usan el tratamiento
**sobre imagen** y la marca de agua **no** se pinta; en la tarjeta los ingredientes con cantidad y los
metadatos quedan en la **banda sólida** y en el hero la tira de metadatos no cambia.

### Scenario 3: encendido + fallo o `null` = ficha tipográfica en silencio

Given el interruptor encendido y un resolutor que devuelve `null` (sin copia en caché, sin red, sin
clave) o un decodificador que devuelve `null` (códec roto),
When se compone la tarjeta/hero,
Then la capa es `TYPOGRAPHIC`: la marca de agua se pinta, la tipografía usa el tratamiento **sobre
superficie**, no hay microcopy ni estado de error, no se cachea nada y no se pinta ningún hueco.

### Scenario 4: encendido + timeout o cancelación

Given el interruptor encendido y una resolución que tarda más que el timeout propio de la política de
imagen (`DishImagePolicy.TIMEOUTS`, máx. 40 s) o cuya corrutina se cancela al salir de composición,
Then el timeout se degrada a `null` → `TYPOGRAPHIC` (igual que el escenario 3), y la
`CancellationException` **se propaga** (no se convierte en `null` ni se traga).

### Scenario 5: encendido + tope por sesión agotado

Given el interruptor encendido y el tope por sesión agotado (`GatedDishImageGenerator`),
When llega una petición nueva,
Then el decorador devuelve `null` **sin invocar** al delegado (0 cuota) y la tarjeta/hero se queda
`TYPOGRAPHIC`, sin consumir más tope.

### Scenario 6: geometría invariante y fundido en un único sitio

Given la tarjeta con el ajuste encendido y la capa pasando de `TYPOGRAPHIC` a `GENERATED`,
Then el hueco conserva su relación (`DishImageArtworkOverlay.HOLE_RATIO` = 16:9), el hero conserva
`RecipeDetailHeroFormat.HERO_HEIGHT_DP` = 240 dp, la banda sólida y la tira no se mueven (solo cambia
el alfa de la capa de fondo, de `0f` a `1f`) y la duración del fundido sale de
`DishImageArtworkPolicy.CROSSFADE_MILLIS` (250 ms) sin constante duplicada.

### Scenario 7: contrato de `RecipeArtwork` y consumidores

Given la implementación terminada,
When se inspecciona el diff,
Then `RecipeArtwork` conserva los parámetros `(recipe, modifier, glyphSize)` con sus **valores por
defecto** (los cinco puntos de uso históricos siguen compilando sin cambios) y no introduce geometría
propia (no fija ratio ni tamaño); sus consumidores pasan a ser **2** (el hueco de `RecipeFicha` y el
hero del Detalle); **deja de leer** `LocalRecipePhotoSource` y no pinta degradado+glifo de respaldo;
y no hay dependencias nuevas.

### Scenario 8: alcance, pureza y capturas

Given la implementación terminada,
When se inspecciona el diff,
Then `data/**` aceptado, `domain/**`, `ui/theme` fuera de los tokens nuevos, la Taxonomía,
`seed-recipes.json`, `assets/**`, build/lockfile, `init.sh` y `scripts/**` están intactos; las capturas
obligatorias **existen y no se han modificado**; la lógica nueva testeable en JVM no importa Android ni
Compose; y no hay hex sueltos fuera de `ui/theme`.

## Repository Research

### Files Inspected

- `feature_list.json` (líneas 1240-1334) — entradas de `card-typographic-tokens-and-design`,
  `recipe-card-typographic`, `recipe-detail-typographic-header`, **esta** y `retire-recipe-photos`:
  metadata, `verification`, notas y las capturas ya aportadas.
- `docs/adr/0006-typographic-recipe-cards.md` — decisión **L2** (§1-3), el cambio de rol de
  `RecipeArtwork` (§7) y la retirada de la cadena local (§4).
- `docs/design/concepts/dish-artwork-as-card-background/README.md` + `tarjeta-con-imagen/{screen.png,
  screen-dark.png,code.html,DESIGN.md}` + `detalle-con-imagen/{screen.png,screen-dark.png,code.html,
  DESIGN.md}` — **capturas obligatorias**, ya presentes; y los prototipos versionados
  `docs/design/concepts/typographic-cards/{prototipo-c3.html,prototipo-c3-imagenes.html,design.md}`
  (tabla OFF/ON y scrim: `linear-gradient(to top, rgba(20,12,8,.78), rgba(20,12,8,.18) 52%,
  rgba(20,12,8,.42))`; oscuro `rgba(10,6,4,.82)/.22/.5`; hueco L2 `16/9`; banda
  `padding:11px 13px 13px`).
- `DESIGN.md` — frontmatter `components.recipe-card-image` (ratio 16:10 / 16:9, `scrim`) y
  `components.recipe-card`; **Overview** (la imagen es tipográfica y la foto es capa de fondo);
  **Components → RecipeCard / RecipeListCard / Imagen de receta** (base tipográfica, variante L2,
  "sin imagen queda la ficha tipográfica con la marca de agua al 12 %", crossfade 250 ms, degradación
  silenciosa); **Shapes** (hero + tira); OQ6 (16:10 vs 3:2 en el carrusel).
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — hoy: `produceState` de la foto local
  y de la generada, `animateFloatAsState` sobre `RecipeArtworkImagePrecedence.crossfadeTarget` y capa
  base (foto local o degradado+glifo) + capa generada. **0 puntos de uso** hoy (grep: solo la
  definición).
- `app/src/main/java/com/sazon/app/ui/artwork/{GeneratedRecipeArtworkSource,RecipeArtworkImagePrecedence,
  DishImageArtworkPolicy,RecipeArtworkPrimitives,RecipeArtworkStyle,DishImageOriginDiagnostics}.kt` —
  seam `fun interface` + default no-op + `LocalGeneratedRecipeArtworkSource`; precedencia pura genérica;
  política pura (concurrencia 2, crossfade 250, LRU 16); primitivas compartidas (`RecipeGlyphArtwork`,
  `recipeAccentColors`, `recipeAccentBrush`).
- `app/src/main/java/com/sazon/app/ui/home/RecipeFicha.kt` — la ficha compartida por la tarjeta
  estrecha (Inicio), la ancha (Resultados) y la celda `compact` de la rejilla de Favoritos: hueco
  `SazonComponents.recipeCardImageRatio` (16:10), marca de agua 150 dp @12 %, `FichaHeading`,
  `FichaIngredients` (una línea o tres filas), `FichaFoot`, filete de 6 px y cromo desde
  `LocalSazonCardColors`.
- `ui/home/RecipeCard.kt`, `ui/results/ResultRecipeCard.kt`, `ui/favorites/FavoriteRecipeCard.kt`
  (`FavoriteRecipeTile`, `RecipeMonogramTile`) — los 4 llamantes de `RecipeFicha`; `HomeScreen.kt`,
  `ResultsScreen.kt`, `FavoritesScreen.kt` (los instancian); `ui/MainShell.kt` (monta las pantallas y
  el Detalle como overlay y **ya** observa `dishImageSettings.state`).
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — `DetailHero` (240 dp, base
  `recipeAccentBrush`, marca de agua 190 dp @12 %, categoría/título/tres filas, filete de 6 dp) y
  `MetaStrip`; hoy **no** consume `RecipeArtwork` (lo dejó así `recipe-detail-typographic-header`).
- `app/src/main/java/com/sazon/app/data/image/{DishImageResolver,DishImageSettings,DishImageGenerationBudget}.kt`
  y `ui/artwork/DishImageArtworkLoader.kt` — la cadena memoria → disco → semilla → generador con
  `Mutex` por clave, el gate del interruptor (`generatedIfEnabled`) y el tope por sesión
  (`GatedDishImageGenerator`). **No se modifican.**
- `app/src/main/java/com/sazon/app/MainActivity.kt` — el punto de inyección (generador envuelto por el
  decorador, `DishImageResolver`, `DishImageArtworkLoader`, `CompositionLocalProvider` con
  `LocalRecipePhotoSource`, `LocalGeneratedRecipeArtworkSource` gateado y el diagnóstico de origen).
- `app/src/main/java/com/sazon/app/ui/theme/{CardTokens,Spacing,Color}.kt` — contrato de tarjeta
  (`SazonCardTokens`/`LocalSazonCardColors`, tinte cálido), `SazonComponents.recipeCardImageRatio`
  (16:10) y la paleta (único sitio con hex).
- `docs/specs/{dish-image-progressive-ui,dish-image-settings-toggle,recipe-card-typographic,
  recipe-detail-typographic-header}.md` — contratos aceptados que esta feature **no reabre**.
- `docs/technical-discovery.md` (líneas 40, 230-268, 727-760, 829-838, 1016-1021),
  `docs/domain/taxonomy.md` §6 (líneas 172-215) y `docs/risks-and-open-questions.md` — estado durable a
  ajustar.

### Existing Patterns To Follow

- **Capa base tipográfica + capa de fondo con fundido** (`RecipeArtwork` + `animateFloatAsState` +
  `tween(CROSSFADE_MILLIS)`): mismo mecanismo, nueva fuente (solo la generada).
- **Política en un único sitio**, pura y determinista (`DishImageArtworkPolicy`, `DishImageSettingsPolicy`,
  `AiProviderPolicy`): el ratio del hueco y las decisiones tipográficas van a un objeto puro.
- **Lógica pura en JVM + capa Compose delgada**: la decisión de "sobre imagen / sobre superficie" se
  extrae de `@Composable` y se prueba con `String`/enums.
- **Degradación silenciosa**: `null` = no disponible; nunca excepción ni error de UI;
  `CancellationException` relanzada.
- **Composición explícita (sin DI)**: `MainActivity` construye y `SazonApp`/`MainShell` reparten por
  parámetro; los `CompositionLocal` se reservan a los seams de imagen.
- **Test JUnit 4** en `app/src/test/java/com/sazon/app/...`, nombres en español con `_`.
- **Verificación canónica** `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- **`RecipeArtwork` no tiene consumidores**: tras `recipe-card-typographic` y
  `recipe-detail-typographic-header` las tarjetas, la celda de Favoritos y el hero dejaron de usarlo
  (grep de `RecipeArtwork(` en `app/src` → solo la definición). Esta feature **lo reincorpora** con el
  rol nuevo (capa de fondo) en dos superficies.
- **No existe la estructura L2** en la tarjeta (hueco + banda sólida) ni el `scrim` de contraste en
  ningún sitio (`grep` de `scrim` en `app/src` → 0); tampoco existe el tratamiento "sobre imagen" ni
  una relación 16:9 propia.
- **No hay forma de saber el modo (tarjeta con/sin capa de fondo) en `RecipeFicha`**: hoy no recibe el
  ajuste; `MainShell` ya lo tiene (`dishImageSettings.state`).

## Technical Approach

### 1. Reconciliación del `verification` (frasa heredada de fase 4) — **decisión cerrada, no reabrir**

El `verification` cita "los **cinco puntos de uso** siguen compilando con el mismo contrato". Ese
contrato es el de `recipe-artwork`/`dish-image-progressive-ui` (Inicio, Resultados, tarjeta de
Favoritos, celda de Favoritos y hero). Tras `recipe-card-typographic` y
`recipe-detail-typographic-header` esos cinco llamantes **ya no existen** (0 llamadas). Se interpreta
como **compatibilidad de contrato**, no como "reintroducir 5 llamadas":

- `RecipeArtwork` conserva los parámetros `(recipe, modifier, glyphSize)` **con sus valores por
  defecto** y no fija ratio ni tamaño (la geometría la aporta el `modifier` del llamante), así que los
  cinco puntos de uso históricos seguirían compilando sin cambios.
- La única adición es un **slot de contenido opcional** (default no-op) que recibe la capa resuelta,
  **aditivo y source-compatible** (mismo patrón que la costura `onDescriptors` de
  `DevExpertRecipeGenerator`, aceptada): lo necesitan la tarjeta y el hero para elegir el tratamiento
  tipográfico sobre imagen.
- Sus consumidores pasan a ser **2**: el hueco de `RecipeFicha` y el hero del Detalle.

### 2. Modo de la capa de fondo (explícito, por parámetro)

`RecipeFicha` (y su hero) reciben `imageBackground: Boolean = false`; `MainShell` —que ya observa
`dishImageSettings.state`— lo reparte a `HomeScreen`, `ResultsScreen`, `FavoritesScreen` y al Detalle,
y esos lo reenvían a `RecipeCard`, `ResultRecipeCard`, `FavoriteRecipeTile` y `RecipeDetailScreen`.
Con `false` (default) el resultado es **exactamente** la ficha/hero aceptados: previews y tests no
cambian. Se descarta un `CompositionLocal` nuevo (el repo reserva los locales a los seams de imagen y
`dish-image-settings-toggle` ya rechazó un local para el ajuste).

### 3. Estructura L2 cuando `imageBackground == true`

**Hueco** (`Box` de ancho completo, `aspectRatio(DishImageArtworkOverlay.HOLE_RATIO)` = **16:9**),
pintado por capas:

1. **Base tipográfica** (nunca vacía): el color de la `Surface` de la tarjeta (o la
   `recipeAccentBrush` del hero) + la **marca de agua** del glifo (150 dp en la tarjeta, 190 dp en el
   hero, `onSurfaceVariant` al 12 %).
2. **`RecipeArtwork`** como capa de **fondo**: la imagen generada (`ContentScale.Crop`, alfa animado) y
   el **`scrim`** de contraste encima de la imagen con el **mismo alfa**.
3. **Contenido** (por el slot): la marca de agua **solo sin imagen**, categoría + título en
   tratamiento **sobre imagen** (`#FFFFFF` / blanco al 86 %) cuando hay imagen y **sobre superficie**
   (`onSurface`/`onSurfaceVariant`) cuando no, filas de ingredientes del hero y **filete de 6 px** al
   pie del hueco.

**Banda sólida** (solo en la tarjeta, debajo del hueco): `surface` de la tarjeta (en oscuro
`darkCardSurface` + tinte de cocina + borde, como hoy), con las **tres filas de ingrediente con
cantidad** (nombre `onSurfaceVariant`, cantidad `onSurface` tabular, hairline `outline` entre filas) y
la fila de metadatos con la coincidencia en terracota; la celda `compact` de Favoritos conserva su
contenido aceptado (ingredientes en una línea, **sin** fila de metadatos: OQ6 sigue abierta). El
`description` de Resultados sigue debajo, sin cambios.

**Con `imageBackground == false`** el componente renderiza la ficha/hero aceptados **sin ningún
cambio** (16:10, todo dentro del hueco, con la marca de agua y el filete de siempre).

**Invariante de geometría**: la estructura (hueco + banda / hero 240 dp) es **la misma** antes y
después de que llegue la imagen; lo único que cambia es el alfa de la capa de fondo (`0f` → `1f`), así
que no hay salto de layout. Cambiar el ajuste **no** fuerza re-resolución de las tarjetas ya pintadas
(decisión 5 de `dish-image-settings-toggle`): se aplica a las peticiones nuevas.

### 4. `RecipeArtwork` como proveedor de la capa de fondo

```kotlin
@Composable
fun RecipeArtwork(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    glyphSize: Dp = RecipeArtworkDefaults.GlyphSize,   // reservado (compatibilidad de contrato)
    content: @Composable (RecipeArtworkImageLayer) -> Unit = {},
)
```

- Resuelve la imagen **solo** por el seam ya existente (`LocalGeneratedRecipeArtworkSource` →
  `DishImageArtworkLoader` → `DishImageResolver`); el gate del interruptor y el tope siguen en
  `MainActivity`/`GatedDishImageGenerator`, **sin tocar**.
- Pinta la imagen con `alpha = RecipeArtworkImagePrecedence.crossfadeTarget(generated)` y
  `tween(DishImageArtworkPolicy.CROSSFADE_MILLIS)` y, encima, el `scrim` con el mismo alfa; sin imagen
  no pinta nada (la base tipográfica del llamante queda visible).
- **Deja de** leer `LocalRecipePhotoSource` y **deja de** pintar el degradado + glifo de respaldo (la
  capa base es ahora del llamante, que ya la pinta). `glyphSize` se **retiene documentado como
  reservado** (compatibilidad de contrato, ver §1).
- Notifica el origen resuelto al diagnóstico como hoy (llamada lateral; solo debug).

### 5. Piezas puras nuevas (JVM)

- `ui/artwork/DishImageArtworkOverlay.kt` (**nuevo, puro**): `HOLE_RATIO = 16f / 9f`,
  `headingOnImage(layer)`, `watermarkVisible(layer)` y `crossfadeAlpha`/decisiones derivadas de
  `RecipeArtworkImagePrecedence`; sin Compose ni Android.
- `ui/theme/CardImageOverlay.kt` (**nuevo**, único sitio con los valores del `scrim`): los dos
  degradados (claro/oscuro) del prototipo y los colores "sobre imagen" y su hairline, declarados en
  `DESIGN.md`; `ui/artwork` los consume por roles (0 hex sueltos fuera de `ui/theme`).
- `RecipeArtworkImagePrecedence` (**reducida a dos capas**): `RecipeArtworkImageLayer { TYPOGRAPHIC,
  GENERATED }`, `layerOf(generated)`, `crossfadeTarget(generated)` (sin cambios de semántica) y
  `originOf(generated, resolvedOrigin)`; se retira la capa `LOCAL` (la cadena local deja de ser parte
  de la imagen de receta). `data.image.DishImageOrigin.CATEGORY` queda sin productor (no se toca
  `data/**`: lo limpia `retire-recipe-photos`).

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — modificar; rol = capa de fondo
  (imagen + `scrim` con fundido), slot de contenido aditivo, sin foto local ni degradado de respaldo;
  firma y geometría conservadas.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtworkImagePrecedence.kt` — modificar; dos capas
  (`TYPOGRAPHIC`/`GENERATED`) y `originOf` sin `LOCAL`.
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkOverlay.kt` — **crear**; ratio 16:9 y
  decisiones tipográficas puras.
- `app/src/main/java/com/sazon/app/ui/theme/CardImageOverlay.kt` — **crear**; tokens del `scrim`
  (claro/oscuro) y colores sobre imagen + hairline.
- `app/src/main/java/com/sazon/app/ui/home/RecipeFicha.kt` — modificar; `imageBackground: Boolean =
  false`, estructura L2 (hueco 16:9 + `RecipeArtwork` + banda sólida) y uso de la capa del slot.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt`,
  `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` (`FavoriteRecipeTile`) —
  modificar; reenvían `imageBackground`.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt`,
  `app/src/main/java/com/sazon/app/ui/results/ResultsScreen.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — modificar; reciben y reenvían el
  flag.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt`,
  `app/src/main/java/com/sazon/app/ui/search/AiSearchFlow.kt` — modificar; **hallazgo del implementer**:
  `ResultsScreen` no lo monta `MainShell` directamente, sino `GeneratorFlow` (pestaña Generador) y
  `AiSearchFlow` (búsqueda con IA), así que el flag tiene que atravesarlos para llegar a Resultados de
  verdad (si no, Resultados se quedaría siempre con el default `false`). Cambio **aditivo** (parámetro
  con default `false`), sin alterar su contrato.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — modificar; `DetailHero` pinta
  `RecipeArtwork` como capa de fondo y usa el tratamiento sobre imagen; **geometría 240 intacta**.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — modificar; reparte el flag (ya observa el
  estado del ajuste) a las 3 pantallas y al Detalle.
- `app/src/test/java/com/sazon/app/ui/artwork/{DishImageArtworkOverlayTest.kt (nuevo),
  RecipeArtworkImagePrecedenceTest.kt (modificar), RecipeBackgroundGateTest.kt (nuevo)}` — escenarios
  1-6.
- `DESIGN.md` — modificar; declarar los tokens del `scrim` y del tratamiento sobre imagen, el ratio
  16:9 del hueco L2, registrar las capturas aceptadas `tarjeta-con-imagen` y `detalle-con-imagen`, y
  reconciliar las frases que dejaban la capa base como foto local (Components → Imagen de receta).
- `docs/technical-discovery.md` — modificar; `RecipeArtwork` = capa de fondo, la tarjeta/hero dejan de
  leer la cadena local, `ui.artwork` gana el ratio/decisiones L2 y los tests nuevos.
- `docs/adr/0006-typographic-recipe-cards.md` — modificar; estado: cuarta feature implementada (queda
  `retire-recipe-photos`).
- `docs/domain/taxonomy.md` §6 — modificar; la resolución visual vigente (el glifo/acento siguen vivos;
  el degradado+glifo deja de ser "último recurso" de la tarjeta/hero) y el origen `CATEGORY` sin
  productor.
- `docs/risks-and-open-questions.md` — modificar; la cadena local queda **sin lectores** (la borra
  `retire-recipe-photos`) y el acabado de las variantes estrecha/compacta queda para revisión visual.
- `PROGRESS.md` y `feature_list.json` — al implementar/validar; **no** los toca el planner.

**No se toca**: `domain/**`, `data/**` (ni la cadena local ni el ajuste), `ui/theme` fuera del fichero
nuevo, la Taxonomía, `seed-recipes.json`, `assets/**`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `app/src/androidTest/**`, `AndroidManifest.xml`,
`docs/design/concepts/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **sí** (comportamiento visual de la tarjeta y del hero), `area: integration`.
- Design source: `DESIGN.md` (mandan sus tokens y reglas) + **capturas de referencia obligatorias**
  (ya aportadas, de solo lectura):
  - `docs/design/concepts/dish-artwork-as-card-background/tarjeta-con-imagen/screen.png`
  - `docs/design/concepts/dish-artwork-as-card-background/tarjeta-con-imagen/screen-dark.png`
  - `docs/design/concepts/dish-artwork-as-card-background/tarjeta-con-imagen/code.html`
  - `docs/design/concepts/dish-artwork-as-card-background/tarjeta-con-imagen/DESIGN.md`
  - `docs/design/concepts/dish-artwork-as-card-background/detalle-con-imagen/screen.png`
  - `docs/design/concepts/dish-artwork-as-card-background/detalle-con-imagen/screen-dark.png`
  - `docs/design/concepts/dish-artwork-as-card-background/detalle-con-imagen/code.html`
  - `docs/design/concepts/dish-artwork-as-card-background/detalle-con-imagen/DESIGN.md`
- Screens or states affected: tarjeta de **Inicio** (estrecha), **Resultados** (ancha), **rejilla de
  Favoritos** (compacta) y **hero del Detalle**; estados: **apagado** (ficha/hero tipográficos, sin
  cambios), **encendido con imagen** (L2) y **encendido sin imagen** (tipográfico, sin error), en claro
  y oscuro.
- New design artifact required: **no**; las capturas existen. El implementer **no** crea ni modifica
  nada bajo `docs/design/concepts/**`.
- El implementer registra en `DESIGN.md`: los tokens del `scrim` y del texto sobre imagen, el ratio
  16:9 del hueco L2, la lista de capturas aceptadas de esta feature y la reconciliación de las frases
  que aún describían la foto local como capa base.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — la cadena en runtime (tarjeta/hero) pasa a ser
  `RecipeArtwork` (capa de fondo) → `data.image` → `data.ai.image`; la cadena local queda sin lectores;
  `RecipeArtworkImagePrecedence` con dos capas; tests JVM nuevos.
- `docs/domain/taxonomy.md` §6: **update** — el glifo (marca de agua / identidad) y el acento (filete)
  siguen vigentes; el degradado+glifo deja de ser el último recurso de tarjeta/hero y `CATEGORY` queda
  sin productor.
- `docs/adr/0006-typographic-recipe-cards.md`: **update** — estado con la cuarta feature implementada.
- `docs/risks-and-open-questions.md`: **update** — cadena local sin lectores (pendiente de borrado) y
  acabado de las variantes no capturadas para revisión visual.
- `DESIGN.md`: **update** — tokens del `scrim`/texto sobre imagen, ratio L2 y capturas registradas.
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados nuevos.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `docs/design/concepts/**`: **not needed** — capturas ya aportadas; el implementer no las toca.
- `docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`: **not needed** — la fase 4 ya
  estaba cerrada; esta feature es del ADR 0006 (si el implementer toca el ADR 0005, es solo para
  apuntar la capa de fondo, sin cambiar contratos).

## Implementation Plan

1. Reducir `RecipeArtworkImagePrecedence` a dos capas (`TYPOGRAPHIC`/`GENERATED`) y crear
   `DishImageArtworkOverlay` (ratio 16:9 + decisiones) con su test.
2. Crear `ui/theme/CardImageOverlay.kt` (tokens del `scrim` y del texto sobre imagen) y declararlos en
   `DESIGN.md`.
3. Reconvertir `RecipeArtwork` en proveedor de la capa de fondo (imagen + `scrim` con fundido, slot de
   contenido aditivo, sin foto local ni degradado de respaldo) manteniendo firma y geometría.
4. Añadir `imageBackground` a `RecipeFicha` con la estructura L2 (hueco 16:9 + banda sólida) y el uso
   de la capa del slot en la tipografía y la marca de agua; dejar `false` como comportamiento aceptado.
5. Convertir `DetailHero` en hero con capa de fondo (misma geometría de 240) usando `RecipeArtwork` y
   el tratamiento sobre imagen.
6. Repartir el flag desde `MainShell` (ya observa el ajuste) por las 3 pantallas, los 4 llamantes de
   `RecipeFicha` y el Detalle.
7. Escribir los tests JVM (gate apagado sin tocar nada; degradación por `null`/timeout/cancelación/tope
   agotado; precedencia y fundido; ratio/decisiones) y actualizar el test existente.
8. Inspeccionar pureza y alcance (greps del Verification Plan) y que las capturas no se han tocado.
9. Actualizar `DESIGN.md`, `docs/technical-discovery.md`, el ADR 0006, `docs/domain/taxonomy.md` §6 y
   `docs/risks-and-open-questions.md`.
10. Ejecutar `bash init.sh` en verde, registrar la evidencia y dejar la feature `passing` (la validación
    independiente es de otro rol).

## Implementation Tasks

- [ ] Reducir `RecipeArtworkImagePrecedence` a `RecipeArtworkImageLayer { TYPOGRAPHIC, GENERATED }`
      (`layerOf`, `crossfadeTarget`, `originOf`) y actualizar su test.
- [ ] Crear `ui/artwork/DishImageArtworkOverlay.kt` (puro): `HOLE_RATIO = 16f/9f`,
      `headingOnImage`, `watermarkVisible`; `DishImageArtworkOverlayTest` con vectores y determinismo.
- [ ] Crear `ui/theme/CardImageOverlay.kt` con los dos `scrim` (claro/oscuro) y los colores sobre
      imagen + hairline; **único sitio con los valores** (0 hex en `ui/artwork`).
- [ ] Reconducir `RecipeArtwork`: imagen + `scrim` con `crossfadeTarget`/`CROSSFADE_MILLIS`; slot de
      contenido opcional con la capa resuelta; sin `LocalRecipePhotoSource` ni degradado de respaldo;
      firma `(recipe, modifier, glyphSize)` y geometría conservadas.
- [ ] `RecipeFicha`: `imageBackground: Boolean = false`; con `true`, hueco 16:9 (base tipográfica +
      `RecipeArtwork` + contenido del slot) y **banda sólida** con las tres filas con cantidad y los
      metadatos (la celda `compact` mantiene una línea y sin metadatos); con `false`, lo aceptado.
- [ ] `RecipeCard`, `ResultRecipeCard` y `FavoriteRecipeTile`: reenviar `imageBackground`.
- [ ] `HomeScreen`, `ResultsScreen`, `FavoritesScreen`: recibir y reenviar el flag.
- [ ] `DetailHero`: capa de fondo con `RecipeArtwork` (base `recipeAccentBrush` + marca de agua cuando
      no hay imagen) y tratamiento sobre imagen; `HERO_HEIGHT_DP` = 240 intacta.
- [ ] `MainShell`: repartir `dishImageSettings.state.value.enabled` (ya observado) a las pantallas y al
      Detalle.
- [ ] `RecipeBackgroundGateTest` (JVM): apagado → `null` con **0** invocaciones del resolutor (caché,
      semilla y generador incluidos); encendido sin imagen → `TYPOGRAPHIC`; encendido con imagen →
      `GENERATED` con `crossfadeTarget == 1f`; tope agotado → `TYPOGRAPHIC` sin invocar al delegado;
      cancelación propagada.
- [ ] Verificar por inspección: firma/geometría de `RecipeArtwork`, 2 consumidores, `ui/artwork` sin
      hex, `data/**`/`domain/**`/build/lockfile/`init.sh`/`scripts/**`/`assets/**` intactos y
      `docs/design/concepts/**` sin cambios.
- [ ] Actualizar `DESIGN.md`, `docs/technical-discovery.md`, el ADR 0006, `docs/domain/taxonomy.md` §6
      y `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde, registrar la evidencia y actualizar
      `feature_list.json` (a `passing`, **sin tocar su `verification`**) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**: ejecuta el gate no bloqueante estándar (`./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`), **no** arranca procesos largos (ni emulador) y **no** se
  modifica `init.sh`.
- Tiradas focales: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` y
  `--tests "com.sazon.app.ui.home.*"` → `BUILD SUCCESSFUL` (sin regresión de `RecipeCardFormatTest`,
  `RecipeMonogramTest` ni del decodificador).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline de referencia: **96 suites / 730 tests** (aceptación de
  `recipe-detail-typographic-header`, sesión 106); el implementer **re-deriva** el número exacto en su
  HEAD (`git grep -c '@Test'`) y confirma el delta.
- `bash scripts/gradle.sh lint` → **0 errores** y sin warnings nuevos bajo `app/src` (los
  `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion`/`OldTargetApi` son
  preexistentes).
- Mapeo explícito del `verification`:
  - "apagado = sin fondo y sin tocar caché, set semilla ni generador" → `RecipeBackgroundGateTest`
    (escenario 1: 0 invocaciones del resolutor).
  - "encendido = fondo cuando el resolver devuelve imagen y ficha tipográfica cuando falla, tarda, se
    cancela o se agota el tope" → `RecipeBackgroundGateTest` + `DishImageArtworkOverlayTest` +
    `RecipeArtworkImagePrecedenceTest` (escenarios 2-5).
  - "la geometría no cambia al aparecer la imagen y el fundido se mantiene en
    `CROSSFADE_MILLIS`" → escenario 6 (ratio del hueco, `HERO_HEIGHT_DP`, alfa `0f`/`1f` y la única
    constante 250 ms) + inspección de la estructura (la capa solo cambia alfa).
  - "`RecipeArtwork` conserva firma y geometría" → escenario 7 (inspección de la firma, 0 geometría
    propia, 2 consumidores).
  - "capturas obligatorias antes de implementar" → escenario 8 (existen y no se modifican).
  - "'bash init.sh' verde; sin dependencias nuevas" → gate + `git diff` sin build/lockfile.
- Inspección de pureza y alcance:
  - `grep -rn "import android\|import androidx" app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkOverlay.kt`
    → **0**.
  - `grep -rn "0xFF\|0x[0-9A-Fa-f]\{8\}" app/src/main/java/com/sazon/app/ui/artwork` → **0** (los
    valores viven en `ui/theme/CardImageOverlay.kt`).
  - `grep -rn "RecipeArtwork(" app/src/main/java/com/sazon/app/ui` → **2** consumidores (hueco y hero).
  - `grep -rn "LocalRecipePhotoSource" app/src/main/java/com/sazon/app/ui` → solo `MainActivity.kt`
    (provider conservado; la cadena la borra `retire-recipe-photos`) y `RecipePhotoSource.kt`.
  - `git diff --name-only` solo con los ficheros previstos; `git status docs/design/concepts` **sin
    cambios**; sin dependencias nuevas.
- Cobertura **E2E**: **no aplica** (el repo no tiene suite E2E persistente y AGENTS.md no automatiza UI
  de emulador); la cobertura canónica es el test JVM/Robolectric y la revisión visual de la persona.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: con clave válida en el
  emulador, abrir Inicio, Resultados, la rejilla de Favoritos y el Detalle, en **claro y oscuro**, y
  comprobar que (a) encendido y con imagen el fondo entra con fundido, sin mover nada y con la
  tipografía legible sobre la imagen; (b) sin imagen (avión o tope agotado) se ve la ficha tipográfica
  sin error ni hueco; (c) apagado no hay fondo y no se genera nada; (d) la tarjeta **estrecha** del
  carrusel y la **compacta** de la rejilla se ven bien con la banda (acento de revisión visual
  declarado).
- Nota operativa: `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo ni
  `./gradlew --stop` (AGENTS.md, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de las tiradas focales (`ui.artwork.*`, `ui.home.*`).
- Conteo parseado del XML (suites/tests, 0 fallos/errores/saltados) con las suites nuevas nombradas y
  la baseline re-derivada en HEAD.
- Informe de lint (0 errores, ninguno nuevo bajo `app/src`).
- Vectores concretos: apagado → `null` con 0 invocaciones del resolutor; `layerOf(null) == TYPOGRAPHIC`
  y `layerOf(bytes) == GENERATED`; `crossfadeTarget(null) == 0f` / `crossfadeTarget(bytes) == 1f`;
  `HOLE_RATIO == 16f/9f`; `headingOnImage(GENERATED) == true` / `(TYPOGRAPHIC) == false`;
  `watermarkVisible(TYPOGRAPHIC) == true` / `(GENERATED) == false`; tope agotado sin invocar al
  delegado; `CancellationException` propagada; `HERO_HEIGHT_DP == 240` (test existente verde).
- Resultado de los greps de inspección y de `git diff --stat` (2 consumidores, 0 hex en `ui/artwork`,
  `data/**`/`domain/**`/build/`init.sh` sin diff, capturas intactas).
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas (el implementer no las reabre):

1. **Modo explícito por parámetro** (`imageBackground`), no por `CompositionLocal`: coherencia con
   "composición explícita, sin DI" y con el rechazo del local en `dish-image-settings-toggle`; el
   default `false` mantiene previews, tests y la ficha aceptada.
2. **La capa base es la ficha/hero tipográficos** (superficie + marca de agua / degradado de cocina):
   `RecipeArtwork` no pinta base propia, así que la tarjeta **nunca** queda vacía y **nunca** cae en la
   foto de categoría (ADR 0006, problema 2).
3. **La tarjeta con el ajuste encendido usa la estructura L2 fija** (hueco 16:9 + banda sólida), con
   imagen o sin ella: es lo que garantiza "sin salto de layout". La diferencia de estructura respecto
   a la ficha apagada (16:10) solo se percibe al **cambiar el ajuste**, que no re-resuelve tarjetas ya
   pintadas (decisión 5 de `dish-image-settings-toggle`).
4. **El tratamiento tipográfico cambia con la capa** (sobre imagen / sobre superficie) y por eso el
   slot de contenido recibe la capa resuelta: es la forma de que la categoría y el título se lean en
   ambos estados sin mover nada.
5. **La cadena local deja de ser leída, no se borra**: la borran los 31 assets, `RecipePhotoAssets`,
   `AssetRecipePhotoSource` y el seam en `retire-recipe-photos`; aquí solo se retira la capa `LOCAL` de
   `RecipeArtwork`/la precedencia.
6. **`glyphSize` se retiene como reservado** (compatibilidad de firma/contrato): la marca de agua la
   sigue pintando el llamante dentro del slot (la tarjeta 150 dp, el hero 190 dp), porque sus offsets
   y tamaños difieren y son geometría aceptada.
7. **Sin dependencias nuevas y `init.sh` sin cambios**: la lógica testeable se extrae de `@Composable`
   (no hay test de Compose en `testImplementation`).

Incógnitas y riesgos (no bloquean, quedan anotados):

- **Variantes no capturadas**: las capturas solo cubren la tarjeta ancha (Resultados) y el hero; el
  hueco 16:9 + banda en la tarjeta **estrecha** del carrusel y en la celda **compacta** de la rejilla es
  una extrapolación (la compacta conserva su contenido aceptado y OQ6 sigue abierta). Revisión visual
  de la persona; un defecto se corrige con una feature correctiva.
- **Contraste del título sobre la imagen**: depende del `scrim`, que es un degradado sobre una foto
  arbitraria; no es medible en JVM. Se sigue el valor del prototipo y lo revisa la persona.
- **El ajuste no "vacía" las tarjetas ya pintadas**: al apagarlo, las que ya mostraban fondo lo
  mantienen hasta recomponerse (decisión 5 heredada); coherente con la prueba manual.
- **Duplicar el fondo en rejillas con scroll**: el fundido dura 250 ms y la LRU de bitmaps (16) evita
  re-decodificar; mismo coste que en la fase 4 aceptada.
- **`data.image.DishImageOrigin.CATEGORY`** queda sin productor: se deja como está (no se toca
  `data/**`) y lo limpia `retire-recipe-photos`; el diagnóstico de debug pierde esa etiqueta en la
  práctica (no es contrato de usuario).
- **Dimensionar**: siguen dos superficies (tarjeta + hero) con un único mecanismo, sin UI nueva; el
  alcance cabe en una spec (la spec es larga por el detalle de L2, no por abarcar dos flujos).

## Validator Checklist

- [ ] Alcance: solo `ui/artwork/**` (fichero nuevo + 2 modificados), `ui/theme/CardImageOverlay.kt`
      (nuevo), `ui/home/{RecipeFicha,RecipeCard,HomeScreen}.kt`, `ui/results/{ResultRecipeCard,ResultsScreen}.kt`,
      `ui/favorites/{FavoriteRecipeCard,FavoritesScreen}.kt`, `ui/detail/RecipeDetailScreen.kt`,
      `ui/MainShell.kt`, los tests previstos y las 5 docs; **sin** tocar `data/**`, `domain/**`, la
      cadena local, el ajuste/tope, build/lockfile, `init.sh`, `scripts/**`, `assets/**` ni
      `docs/design/concepts/**`, y **sin** dependencias nuevas.
- [ ] Apagado: **0** lecturas de caché/semilla y **0** invocaciones del generador, sin fondo y sin
      cambio en la ficha aceptada (escenario 1).
- [ ] Encendido: fondo con `scrim` cuando el resolver devuelve imagen; ficha tipográfica (sin error ni
      hueco) cuando falla, tarda, se cancela o el tope está agotado; `CancellationException` propagada
      (escenarios 2-5).
- [ ] Geometría invariante: hueco 16:9 fijo, hero 240 dp, banda/tira sin mover; el fundido usa
      `DishImageArtworkPolicy.CROSSFADE_MILLIS` sin constante duplicada (escenario 6).
- [ ] `RecipeArtwork` conserva `(recipe, modifier, glyphSize)` con sus defaults, no impone geometría,
      tiene 2 consumidores y ya no lee la foto local ni pinta el degradado de respaldo (escenario 7).
- [ ] La lógica nueva testeable es pura (sin Android/Compose), determinista y con casos negativos que
      no pasan en vacío; los tests existentes (`RecipeCardFormatTest`, `RecipeMonogramTest`,
      `RecipeDetailHeroFormatTest`, `DishImageArtworkLoaderTest`) siguen verdes.
- [ ] Las capturas obligatorias existen, se han seguido y **no** se han modificado; `DESIGN.md`
      declara los tokens nuevos, el ratio L2 y las capturas aceptadas.
- [ ] `docs/technical-discovery.md`, `docs/domain/taxonomy.md` §6, el ADR 0006 y
      `docs/risks-and-open-questions.md` están actualizados y coherentes.
- [ ] `feature_list.json` y `PROGRESS.md` reflejan estado y evidencia; el `verification` de la feature
      queda **intacto** y el implementer no hizo commit, push ni PR.
