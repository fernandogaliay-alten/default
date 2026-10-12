# Feature Implementation Spec: Placeholder de receta por categoría y cocina (componente RecipeArtwork)

## Source Feature

- `id`: `recipe-artwork`
- `area`: `ui`
- `depends_on`: `["results-screen", "recipe-detail-screen", "favorites"]` (las tres `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json` + issue **#77** (`[recipe-artwork] Placeholder de receta por categoría y
  cocina (componente RecipeArtwork)`), que es el brief autoritativo.
- Verificación exigida (de la feature): un test JVM que **itera la Taxonomía real** y afirma que cada
  uno de los **18 `recipeCategories`** (y **10 `cuisines`**) tiene estilo, más el caso de fallback; las
  **4 copias** del placeholder usan el componente único; `DESIGN.md` registra la extensión.
- **Desviación de capturas autorizada por la persona** (issue #77): esta feature **no** genera capturas
  nuevas. Ver `Visual Design Impact`.

## Goal

Sustituir las **4 copias** del placeholder de receta (degradado melocotón→oliva + `PlateGlyph`
duplicado a mano) por **un único componente** `RecipeArtwork` con un **mapeo puro, exhaustivo,
determinista y offline**: un **glifo** de línea dibujado con Compose según `categoryId` (18 categorías
reales) y un **acento** (degradado del bloque) según `cuisineId` (10 cocinas reales), de forma que dos
recetas de la misma categoría se vean distintas, con **fallback total** (id nulo/desconocido → `other`
→ plato + degradado por defecto; nunca vacío) y **sin dependencias nuevas**.

`Recipe` no tiene campo de imagen y el MVP no empaqueta fotos; el placeholder seguía copiado y plano en
`RecipeCard`, `ResultRecipeCard`, `FavoriteRecipeCard` (lista + miniatura) y `RecipeDetailScreen`
(hero). `results-screen` ya dejó anotado que "extraer un placeholder interno compartido es opcional pero
preferible"; esta feature lo hace y añade variedad por categoría/cocina. `RecipeArtwork` queda como
**único punto de cambio** si el día de mañana se añaden imágenes generadas por IA (seam futuro).

## Non-Goals

- **No** se añaden imágenes reales ni assets: ni `assets/img/recipes/**`, ni Coil, ni ninguna librería de
  imágenes o de iconos (`material-icons-*`). Los glifos se dibujan con primitivas de Compose (`Canvas`).
- **No** se añaden dependencias: `app/build.gradle.kts` y `gradle/libs.versions.toml` **intactos**.
- **No** se tocan el dominio (`domain.recipe`), la Taxonomía (`docs/domain/taxonomy.json`,
  `Taxonomy`/`TaxonomyJson`), `seed-recipes.json`, Room, el Lote, el Perfil ni `init.sh`.
- **No** se cambia el comportamiento no-visual de las 4 tarjetas/pantallas (título, meta, píldora de
  coincidencia, corazón, click, pestañas, barra inferior): solo cambia **cómo se pinta la imagen**.
- **No** se crean pantallas, estados ni microcopy nuevos; **no** se crea
  `docs/design/concepts/recipe-artwork/` (capturas exentas).
- **No** se implementa el modo oscuro "de nuevo": se consumen los tokens existentes (ya soportados).

## Job Story

Cuando miro una lista de recetas sin fotos (porque el MVP no tiene imágenes),
quiero reconocer de un vistazo la **categoría** del plato y diferenciar unas recetas de otras,
para que el catálogo se sienta intencionado y no como un mosaico de bloques idénticos.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); ve las tarjetas y el detalle. No hay
  cuentas ni roles.
- **Desarrollador / agente**: cubre en JVM el **mapeo puro y exhaustivo** (glyph por `categoryId`,
  acento por `cuisineId`, fallback, determinismo) y el reemplazo de las 4 copias. El acabado fino de los
  18 glifos lo revisa la persona en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: todo es local y determinista; **sin red**, sin reloj, sin `Random`.

## Acceptance Scenarios

### Scenario 1: glifo por categoría (18 reales) y determinismo

Given una receta con un `categoryId` real de la Taxonomía (p. ej. `pasta`),
When `RecipeArtwork` pinta su imagen,
Then aparece el **glifo de esa categoría** (dibujado con Compose, trazo de línea) y, al repetir la
llamada, el resultado es **idéntico** (determinista, sin `Random`/reloj).

### Scenario 2: acento por cocina (10 reales) — dos recetas de la misma categoría difieren

Given dos recetas con el **mismo** `categoryId` pero **distinta** `cuisineId` (p. ej. `pasta`+`italian`
y `pasta`+`asian`),
Then el **degradado del bloque** es distinto (acento de la cocina), de modo que no se ven iguales,
manteniendo el glifo de la categoría.

### Scenario 3: fallback total (id desconocido o nulo)

Given una receta con `categoryId` nulo, vacío o inexistente, o con `cuisineId` nulo/vacío/inexistente,
When se pinta la imagen,
Then se usa el estilo **por defecto**: glifo de **plato** (dos círculos concéntricos, el `PlateGlyph`
actual, equivalente a la categoría `other`) y degradado **melocotón→oliva** actual; **nunca** se pinta
un bloque vacío ni se lanza excepción.

### Scenario 4: un único componente sustituye las 4 copias

Given el código actual (4 copias de `PlateGlyph` + degradado en `RecipeCard`, `ResultRecipeCard`,
`FavoriteRecipeCard` [lista y miniatura] y `RecipeDetailScreen` [hero]),
When se implementa la feature,
Then **no queda ninguna** definición/uso de `PlateGlyph` en `app/src` y el degradado+glifo solo existe
dentro de `RecipeArtwork`; los 4 puntos llaman a `RecipeArtwork`.

### Scenario 5: geometría y tamaños de glifo preservados

Given las capturas y el layout aceptados,
When se sustituyen las 4 copias,
Then se conservan sin cambios: tarjeta de Home y de Resultados **16:10** (`SazonComponents.
recipeCardImageRatio`) con esquinas 24; miniatura de Favoritos **96x96** con `SazonShapes.small`;
bloque superior de la rejilla de Favoritos **alto 96**; hero del Detalle **alto 240**; y los tamaños de
glifo **48 / 56 / 40 (x2) / 72 dp** respectivamente.

### Scenario 6: claro y oscuro sin hex sueltos

Given `SazonTheme` en modo claro y en oscuro,
When `RecipeArtwork` resuelve el acento,
Then los colores salen de **roles del tema** (`MaterialTheme.colorScheme.*`) —no de hex propios—, de
modo que el modo oscuro usa los `*Container` oscuros con el glifo legible.

### Scenario 7: exhaustividad garantizada por test (verificación exigida)

Given `docs/domain/taxonomy.json` (fuente versionada, leída por test con la propiedad de sistema
`taxonomy.asset.path`, mismo patrón que `TaxonomyIntegrityTest`),
When corre `./gradlew test`,
Then un test JVM itera los **18 `recipeCategories`** y las **10 `cuisines`** reales y afirma que cada id
tiene estilo propio (glifo/acento esperado, sin duplicados), más el **caso de fallback** con un id
desconocido que **no** devuelve un estilo de categoría específico ni falla.

### Scenario 8: DESIGN.md registra la extensión y la desviación

Given que esta feature es la única documentación visual aplicable,
When se implementa,
Then `DESIGN.md` documenta el placeholder extendido (glifo por categoría, acento por cocina, fallback)
y registra explícitamente la **desviación** (sin capturas nuevas) de `recipe-artwork`.

## Repository Research

### Files Inspected

- `feature_list.json` (entry `recipe-artwork`) y `PROGRESS.md` — verificación exigida, nota de la
  desviación de capturas, camino `init.sh` y política de capturas por pantalla.
- `AGENTS.md` — UI verificada por lógica/estado en JVM; capturas obligatorias salvo desviación
  autorizada; una feature por sesión; nunca `./gradlew` directo.
- Issue **#77** (`gh issue view 77`) — brief autoritativo (objetivo, exhaustividad por test, desviación).
- `DESIGN.md` — `recipe-card.imageRatio: "16:10"`, `rounded.md: 24px`, `RecipeCard`/`RecipeListCard`,
  "no gradientes llamativos", "pocos colores", `Generated Concept Images`, Open Design Question 4.
- `docs/build-brief.md` / `docs/risks-and-open-questions.md` — las fotos de receta son **no
  bloqueantes**: placeholders de degradado o fotos propias; la app es personal y no se publica.
- `docs/domain-model.md` — `Recipe` ya tiene `categoryId` y `cuisineId` (no cambia el modelo).
- `docs/domain/taxonomy.md` — §6 "Fotos de receta: `assets/img/recipes/{categoryId}.webp`" y §75
  "si falta, se usa `other`" (drift preexistente: el MVP no empaqueta imágenes; ver Unknowns).
- `docs/technical-discovery.md` — tabla "Iconos e imágenes de receta | Assets locales …", `ui.home`/
  `ui.results`/`ui.favorites`/`ui.detail` y los párrafos que describen "degradado + glifo".
- `docs/specs/results-screen.md` — "Imagen placeholder: se reutiliza el degradado melocotón→oliva +
  glifo de plato" y "extraer un placeholder interno compartido es opcional pero preferible".
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` — `PlateGlyph` (48dp, 2dp stroke, dos
  círculos) + `Brush.linearGradient(SazonPrimaryContainer, SazonSecondaryContainer)` sobre
  `.fillMaxWidth().aspectRatio(SazonComponents.recipeCardImageRatio)`.
- `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt` — misma copia con glifo 56dp.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — `PlateGlyph` con
  `.size(40.dp)` en la miniatura 96x96 (`SazonShapes.small`) y en el bloque alto-96 de la rejilla (aquí
  el `PlateGlyph` no fija tamaño propio: `Modifier.size(40.dp)` en el call site).
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — hero `height(240.dp)` con un
  `Box(size(72.dp))` envolviendo `PlateGlyph(fillMaxSize)`; "Atrás" y corazón son hermanos del bloque.
- `app/src/main/java/com/sazon/app/ui/theme/{Color,Spacing,Shape}.kt` — roles `*Container` (claro y
  oscuro), `SazonComponents.recipeCardImageRatio`, `SazonShapes`/`SazonPillShape`.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `categoryId`, `cuisineId`.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — `RecipeCategory`, `Cuisine`,
  `recipeCategories`, `cuisines`.
- `docs/domain/taxonomy.json` — 18 `recipeCategories` (`pasta, rice, soup_cream, salad, meat, poultry,
  fish_seafood, vegetarian, vegan, legume_stew, pizza_bread, egg_breakfast, sandwich_wrap,
  tapas_appetizer, dessert, smoothie_drink, casserole_oven, other`) y 10 `cuisines` (`spanish,
  mediterranean, italian, asian, japanese, indian, mexican, american, middle_eastern, latin`).
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` y
  `.../ui/onboarding/OnboardingPreferencesDraftTest.kt` — patrón `TaxonomyJson.parse(taxonomyJson())`
  leyendo `System.getProperty("taxonomy.asset.path")` (lo publica `app/build.gradle.kts`).
- `app/src/test/java/com/sazon/app/ui/home/RecipeCardFormatTest.kt` — precedente de test JVM de lógica
  pura de `ui.home`.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: el mapeo vive en Kotlin sin imports de Compose/Android y se
  testea en JVM; los `@Composable` solo pintan (patrón de `ui.home`/`ui.results`/`ui.favorites`).
- **Paquete compartido de UI**: como `ui.theme`, `ui.preferences` y `ui.filters`, un paquete propio
  para lo reutilizable: `ui.artwork`.
- **Glifos dibujados con Compose, sin librería de iconos**: `PlateGlyph`/`HeartGlyph` ya lo hacen con
  `Canvas`/`Path`/`Stroke` (2.dp). Trazo normalizado al tamaño del canvas para escalar con el `size`.
- **Tokens del tema, nunca hex sueltos** (`ui.theme`); el degradado actual usa
  `SazonPrimaryContainer`→`SazonSecondaryContainer`.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, con **caso negativo** que no
  pasa en vacío.
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`), sin procesos largos.

### Current Gaps

- No existe ningún componente común de imagen de receta; hay **4 copias** de `PlateGlyph` + degradado.
- No existe mapeo de `categoryId`→glifo ni de `cuisineId`→acento.
- No existe paquete `ui.artwork`.
- `DESIGN.md` no describe el placeholder por categoría/cocina ni la desviación de capturas.

## Technical Approach

**Mapeo puro (`ui.artwork`) + un componente Compose `RecipeArtwork` + reemplazo de las 4 copias**, sin
dependencias ni cambios de dominio. El color se resuelve por **roles del tema** (claro/oscuro gratis) y
los glifos se dibujan con primitivas de Compose.

### 1. Mapeo puro — `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtworkStyle.kt`

Sin imports de Compose/Android (testeable en JVM). Contiene:

- `enum class RecipeGlyph { PASTA, RICE, SOUP_CREAM, SALAD, MEAT, POULTRY, FISH_SEAFOOD, VEGETARIAN,
  VEGAN, LEGUME_STEW, PIZZA_BREAD, EGG_BREAKFAST, SANDWICH_WRAP, TAPAS_APPETIZER, DESSERT,
  SMOOTHIE_DRINK, CASSEROLE_OVEN, PLATE }` — 18 miembros, uno por categoría real; `PLATE` es el glifo
  de `other` **y** el fallback (el `PlateGlyph` actual, dos círculos concéntricos).
- `enum class RecipeAccent { EMBER, GROVE, TOMATO, GINGER, SESAME, CURRY, CHILI, HONEY, CLAY, ROSE,
  DEFAULT }` — 10 acentos (uno por cocina real) + `DEFAULT` (fallback). El acento fija el **degradado
  del bloque** (color superior → inferior) por **roles del tema**; la resolución rol→`Color` vive en la
  capa Compose (así `RecipeArtworkStyle` no importa Compose y el modo oscuro es correcto).
- `data class RecipeStyle(val glyph: RecipeGlyph, val accent: RecipeAccent)`.
- `object RecipeArtworkStyle`:
  - `fun glyphFor(categoryId: String?): RecipeGlyph`
  - `fun accentFor(cuisineId: String?): RecipeAccent`
  - `fun of(categoryId: String?, cuisineId: String?): RecipeStyle`
  - `fun of(recipe: Recipe): RecipeStyle` (delega en las anteriores)

**Tabla glifo ↔ `categoryId` (1:1, exhaustiva):**

| `categoryId` | `RecipeGlyph` | Glifo (línea, 2dp) |
|---|---|---|
| `pasta` | `PASTA` | tenedor con hebras/espiral |
| `rice` | `RICE` | cuenco con granos |
| `soup_cream` | `SOUP_CREAM` | cuenco con cuchara y vapor |
| `salad` | `SALAD` | cuenco con hojas |
| `meat` | `MEAT` | chuleta (óvalo con hueso) |
| `poultry` | `POULTRY` | muslo de ave |
| `fish_seafood` | `FISH_SEAFOOD` | pez (o gamba) |
| `vegetarian` | `VEGETARIAN` | hoja |
| `vegan` | `VEGAN` | brote (dos hojas y tallo) |
| `legume_stew` | `LEGUME_STEW` | olla con tapa |
| `pizza_bread` | `PIZZA_BREAD` | porción de pizza / hogaza |
| `egg_breakfast` | `EGG_BREAKFAST` | huevo (óvalo con yema) |
| `sandwich_wrap` | `SANDWICH_WRAP` | sándwich triangular |
| `tapas_appetizer` | `TAPAS_APPETIZER` | platito con pincho |
| `dessert` | `DESSERT` | copa/cupcake |
| `smoothie_drink` | `SMOOTHIE_DRINK` | vaso con pajita |
| `casserole_oven` | `CASSEROLE_OVEN` | fuente de horno |
| `other` | `PLATE` | plato (dos círculos concéntricos, **actual**) |
| nulo/vacío/desconocido | `PLATE` | **fallback** (igual que `other`) |

Se permite reutilizar un primitivo base (cuenco/plato/vaso) entre categorías **solo** si el resultado
es distinguible y el enum sigue siendo 1:1 con las 18 categorías.

**Tabla acento ↔ `cuisineId` (rol del tema; el degradado va de `start` arriba a `end` abajo):**

| `cuisineId` | `RecipeAccent` | start | end |
|---|---|---|---|
| `spanish` | `EMBER` | `surfaceVariant` | `primaryContainer` |
| `mediterranean` | `GROVE` | `secondaryContainer` | `primaryContainer` |
| `italian` | `TOMATO` | `primaryContainer` | `tertiaryContainer` |
| `asian` | `GINGER` | `tertiaryContainer` | `primaryContainer` |
| `japanese` | `SESAME` | `surfaceVariant` | `secondaryContainer` |
| `indian` | `CURRY` | `tertiaryContainer` | `secondaryContainer` |
| `mexican` | `CHILI` | `primaryContainer` | `surfaceVariant` |
| `american` | `HONEY` | `tertiaryContainer` | `surfaceVariant` |
| `middle_eastern` | `CLAY` | `secondaryContainer` | `surfaceVariant` |
| `latin` | `ROSE` | `secondaryContainer` | `tertiaryContainer` |
| nulo/vacío/desconocido | `DEFAULT` | `primaryContainer` | `secondaryContainer` (**degradado actual**) |

Los 11 pares son **distintos**; `DEFAULT` reproduce exactamente el degradado de hoy. Todos los roles
existen en claro y oscuro (`Color.kt`), así que no hace falta ningún hex nuevo. El **tinte del glifo**
se mantiene en `onSurfaceVariant` (el actual), legible sobre los 11 degradados en ambos modos.

### 2. Componente — `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt`

```kotlin
@Composable
fun RecipeArtwork(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    glyphSize: Dp = RecipeArtworkDefaults.GlyphSize, // 48.dp
)
```

- Interno: `val style = RecipeArtworkStyle.of(recipe)`; `Box(modifier.background(Brush.linearGradient(
  accentColors(style.accent))))` con `contentAlignment = Alignment.Center` y, dentro,
  `RecipeGlyphCanvas(glyph = style.glyph, tint = MaterialTheme.colorScheme.onSurfaceVariant,
  modifier = Modifier.size(glyphSize))`.
- `accentColors(accent): List<Color>` es un `when (accent)` que devuelve los dos roles de la tabla desde
  `MaterialTheme.colorScheme` (nada de hex).
- `RecipeGlyphCanvas` es `private`: un `Canvas` con el trazo de `2.dp` (patrón del `PlateGlyph` actual)
  y un `when (glyph)` que dibuja cada glifo con `Path`/`drawCircle`/`drawLine`/`drawArc`/`drawOval`
  normalizado al tamaño del canvas (usa `size.width/height` como el `HeartGlyph`). `PLATE` es,
  literalmente, los dos círculos concéntricos de hoy → el fallback conserva el aspecto actual.
- El `modifier` lo aporta el llamante con tamaño/ratio/clip; el componente **no** fija forma (así las
  esquinas las sigue redondeando el `Surface` padre y la miniatura su `clip(SazonShapes.small)`).
- KDoc: deja explícito que es el **único punto a cambiar** para imágenes IA futuras.

### 3. Reemplazo de las 4 copias (sin cambiar el resto del layout)

- `ui/home/RecipeCard.kt`: eliminar `PlateGlyph`; llamar `RecipeArtwork(recipe, Modifier.fillMaxWidth()
  .aspectRatio(SazonComponents.recipeCardImageRatio))` con `glyphSize = 48.dp`; quitar imports de
  `Canvas`/`Brush`/`Color`/`Stroke` que queden sin uso.
- `ui/results/ResultRecipeCard.kt`: igual, `glyphSize = 56.dp`.
- `ui/favorites/FavoriteRecipeCard.kt`: en la miniatura `RecipeArtwork(favorite.recipe,
  Modifier.size(96.dp).clip(SazonShapes.small), glyphSize = 40.dp)`; en el bloque de la rejilla
  `RecipeArtwork(favorite.recipe, Modifier.fillMaxWidth().height(96.dp), glyphSize = 40.dp)`. Eliminar
  `PlateGlyph`; **conservar** `HeartGlyph`, `HeartButton`, `DismissBackground` y el corazón
  superpuesto del tile.
- `ui/detail/RecipeDetailScreen.kt`: en `DetailHero`, sustituir el bloque + `PlateGlyph` por
  `RecipeArtwork(recipe, Modifier.fillMaxWidth().height(240.dp), glyphSize = 72.dp)` (cambia la
  firma de `DetailHero` para recibir `recipe`); "Atrás" y el corazón siguen siendo hermanos.

### 4. Documentación

- `DESIGN.md`: registrar la extensión (bullet de componentes `RecipeArtwork` + tabla/resumen de
  glifo por categoría y acento por cocina + fallback) y la **desviación** de capturas;
  marcar resuelta la Open Design Question 4 (MVP: placeholder dibujado, sin fotos).
- `docs/technical-discovery.md`: anotar que la imagen de receta se resuelve con el placeholder
  `ui.artwork.RecipeArtwork` (determinista, offline, por categoría/cocina) y que
  `assets/img/recipes/**` **no** se empaqueta en el MVP.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtworkStyle.kt` — enums `RecipeGlyph`/
  `RecipeAccent`, `RecipeStyle` y el mapeo puro (sin Compose).
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — componente + `RecipeGlyphCanvas`
  privado + resolución de acentos por roles del tema.
- `app/src/test/java/com/sazon/app/ui/artwork/RecipeArtworkStyleTest.kt` — escenarios 1, 2, 3 y 7.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` — quitar `PlateGlyph`, usar `RecipeArtwork`.
- `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt` — idem.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — idem en miniatura y tile.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — `DetailHero` con `RecipeArtwork`.
- `DESIGN.md` — registrar la extensión del placeholder y la desviación de capturas.
- `docs/technical-discovery.md` — anotar `ui.artwork.RecipeArtwork` y la ausencia de assets de imagen.
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar** (estado y evidencia), no ahora.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain.recipe`,
`domain.taxonomy`, `Taxonomy`/`TaxonomyJson`, `docs/domain/taxonomy.json`, `seed-recipes.json`,
`data/**`, Room, el Lote, el Perfil, `ui.theme/**`, `ui/MainShell.kt`, `ui/SazonApp.kt`,
`MainActivity.kt`, `init.sh`.

## Visual Design Impact

- UI involved: **sí** (imagen de receta en 4 puntos de la app).
- Design source: **`DESIGN.md`** (`recipe-card.imageRatio: 16:10`, `rounded.md: 24px`, `RecipeCard`/
  `RecipeListCard`, "pocos colores", "no gradientes llamativos") + el degradado/glifo ya aceptado en
  `results-screen`/`recipe-detail-screen`/`favorites`.
- Screens or states affected: `RecipeCard` (Home), `ResultRecipeCard` (Resultados), tarjeta y
  miniatura de Favoritos, hero del Detalle; claro y oscuro; fallback (id desconocido/nulo).
- New design artifact required: **no**.
- **Capturas de UI (exención autorizada por la persona — issue #77 y nota de la entry
  `recipe-artwork` en `feature_list.json`)**: esta feature **no** genera capturas y **no** se crea
  `docs/design/concepts/recipe-artwork/` ni su `README.md`. Motivo: el placeholder ya está definido en
  `DESIGN.md`/el prompt de Stitch como el **fallback** del diseño ("soft peach-to-olive gradient block
  with a thin line-art plate icon") y esta feature solo lo **extiende** por categoría y cocina.
  `DESIGN.md` sigue siendo la fuente de verdad ante contradicciones.
- **La única documentación visual aplicable es el `DESIGN.md` actualizado** (ver
  "Expected File Changes" y Scenario 8): la extensión del placeholder debe quedar **registrada allí**
  como parte del alcance, y es contra ese registro contra lo que se revisa el acabado.
- Sin hex sueltos: los acentos se resuelven por roles de `MaterialTheme.colorScheme`; el tinte del
  glifo sigue siendo `onSurfaceVariant` (el actual).
- Un defecto visual se corrige con una **feature correctiva** (regla de AGENTS.md), no bloquea el flujo.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — la imagen de receta se resuelve con
  `ui.artwork.RecipeArtwork` (por categoría/cocina, offline, determinista) y `assets/img/recipes/**`
  no se empaqueta en el MVP.
- `docs/domain-model.md`: **not needed** — `Recipe` ya tiene `categoryId`/`cuisineId`; no cambian
  entidades, relaciones ni estados.
- `docs/domain/taxonomy.md`: **not needed** — el drift de §6 (`assets/img/recipes/{categoryId}.webp`) es
  preexistente y se anota como Unknown; esta feature no edita la doc de la Taxonomía.
- `DESIGN.md`: **update** — registrar el placeholder extendido (glifo por categoría, acento por cocina,
  fallback) y la desviación explícita de capturas; resolver la Open Design Question 4.
- `docs/design/concepts/`: **no change** — capturas exentas por decisión de la persona (issue #77).
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear `ui/artwork/RecipeArtworkStyle.kt` (enums + mapeo puro + fallback).
2. Escribir `RecipeArtworkStyleTest` (exhaustividad sobre la Taxonomía real, determinismo, fallback).
3. Crear `ui/artwork/RecipeArtwork.kt` (degradado por acento, `RecipeGlyphCanvas` con los 18 glifos,
   `PLATE` idéntico al actual).
4. Sustituir las 4 copias (`RecipeCard`, `ResultRecipeCard`, `FavoriteRecipeCard` x2,
   `RecipeDetailScreen`) y eliminar los `PlateGlyph` privados; conservar geometría y tamaños.
5. Actualizar `DESIGN.md` (extensión + desviación + OQ4) y `docs/technical-discovery.md`.
6. Ejecutar `bash init.sh` en verde y registrar la evidencia.

## Implementation Tasks

- [ ] Crear `RecipeGlyph` (18 miembros) y `RecipeAccent` (11 miembros) con `RecipeArtworkStyle`.
- [ ] Implementar `glyphFor`/`accentFor`/`of(recipe)` con fallback total (nulo/desconocido → PLATE/DEFAULT).
- [ ] Escribir `RecipeArtworkStyleTest`: los 18 `categoryId` reales → su glifo (sin duplicados); las 10
      `cuisines` reales → su acento (sin duplicados); determinismo; fallback; caso negativo.
- [ ] Crear `RecipeArtwork` con degradado por acento (roles del tema) y `RecipeGlyphCanvas` con los 18
      glifos de línea (`PLATE` = dos círculos del `PlateGlyph` actual).
- [ ] Sustituir en `RecipeCard` (48dp), `ResultRecipeCard` (56dp), `FavoriteRecipeCard` (96x96 y tile,
      40dp) y `DetailHero` (240dp, 72dp); eliminar los `PlateGlyph` privados y los imports huérfanos.
- [ ] Verificar por grep que no queda `PlateGlyph` y que el degradado+glifo solo vive en `RecipeArtwork`.
- [ ] Actualizar `DESIGN.md` (bullet `RecipeArtwork`, tabla por categoría/cocina, desviación de
      capturas, OQ4 resuelta) y `docs/technical-discovery.md`.
- [ ] Ejecutar `bash init.sh` en verde y anotar la evidencia en `feature_list.json`/`PROGRESS.md`.

## Verification Plan

- `bash init.sh` (vía `scripts/gradle.sh`) → **esperado**: exit 0 (`./gradlew test` + `./gradlew lint`),
  sin procesos largos. Es el gate no bloqueante; **no cambia** y no necesita modos nuevos.
- `./gradlew test` (vía `init.sh`) → **esperado**: `RecipeArtworkStyleTest` en verde sin romper las
  suites existentes:
  - **Escenario 7 (verificación exigida)**: itera `TaxonomyJson.parse(taxonomyJson())` (leído de
    `System.getProperty("taxonomy.asset.path")`, el `docs/domain/taxonomy.json` versionado) y afirma que
    **cada** uno de los 18 `recipeCategories` y **cada** una de las 10 `cuisines` mapea a un estilo
    esperado, con **sin duplicados** entre categorías (18 glifos distintos) y entre cocinas (10 acentos
    distintos).
  - Determinismo: dos llamadas iguales devuelven el mismo `RecipeStyle`.
  - Fallback: `glyphFor(null)`/`glyphFor("")`/`glyphFor("no-existe")` → `PLATE`;
    `accentFor(null)`/`accentFor("")`/`accentFor("no-existe")` → `DEFAULT`; `of(recipe)` con ids nulos
    no lanza.
  - **Caso negativo**: un id desconocido **no** devuelve el estilo de ninguna categoría real/cocina real
    (p. ej. `glyphFor("no-existe") != glyphFor("pasta")` y `accentFor("no-existe") != accentFor("italian")`).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- **Inspección**: `ui.artwork/RecipeArtworkStyle.kt` sin imports de Compose/Android; sin hex en el
  código de la feature; sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio,
  Taxonomía, `seed-recipes.json`, Room, `ui.theme/**`, `MainShell.kt`, `SazonApp.kt`, `MainActivity.kt`
  ni `init.sh`; `git diff --stat` solo con lo previsto.
- **Grep**: `PlateGlyph` → 0 apariciones en `app/src`; `Brush.linearGradient` referido solo desde
  `RecipeArtwork.kt`.
  - **Finding del implementer (2026-10-12)**: `Brush.linearGradient` tiene un uso **ajeno y
    preexistente** (`ui/home/HomeScreen.kt`, la tarjeta-héroe "¿Qué tienes en la nevera?" con
    `SazonPrimary` → `SazonInversePrimary`) que esta feature **no** toca. El chequeo se cumple en su
    intención real: el degradado **del placeholder** (`primaryContainer` → `secondaryContainer`) ya solo
    vive en `recipe-artwork.kt`; el grep correcto es
    `grep -rn "Brush.linearGradient" app/src | grep -v HomeScreen`.
- **Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI)**: abrir Home, Resultados,
  Favoritos (lista y rejilla) y el Detalle y comprobar que el glifo corresponde a la categoría, que dos
  recetas de la misma categoría y distinta cocina se ven diferentes, que el fallback sigue siendo el
  degradado+plato actual, y que en modo oscuro el bloque y el glifo se leen bien.
- Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
  emulador; el comportamiento verificable (mapeo puro exhaustivo, determinismo, fallback) se cubre en
  JVM y el acabado visual lo revisa la persona contra `DESIGN.md`.

## Evidence To Capture

- Salida de `./gradlew test` con `RecipeArtworkStyleTest` (nº de tests, 0 fallos) y el total de la suite;
  evidencia de que el test **itera la Taxonomía real** (18 categorías y 10 cocinas).
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature; `init.sh` con código 0.
- Grep sin `PlateGlyph` en `app/src` y con el degradado solo en `RecipeArtwork.kt`.
- Inspección: `RecipeArtworkStyle.kt` puro (sin Compose/Android), sin hex sueltos, sin cambios en
  build/lockfile/dominio/Taxonomía/`seed-recipes.json`/`init.sh`.
- `DESIGN.md` con la extensión del placeholder registrada y la desviación de capturas anotada.
- Resultado manual en emulador (descripción) del glifo por categoría, el acento por cocina y el fallback.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Validator Checklist

- [ ] La implementación se queda en `ui.artwork` + los 4 puntos de uso + `DESIGN.md`/
      `docs/technical-discovery.md`; sin pantallas, estados ni microcopy nuevos.
- [ ] El mapeo es **puro** (`RecipeArtworkStyle` sin imports de Compose/Android), **exhaustivo**
      (18 `RecipeGlyph` ↔ 18 `recipeCategories`, 10 `RecipeAccent` ↔ 10 `cuisines`) y **determinista**.
- [ ] **Fallback total**: `null`/vacío/desconocido → `PLATE` + `DEFAULT` (el degradado+plato actual),
      nunca vacío ni excepción (Scenario 3).
- [ ] El test JVM **itera la Taxonomía real** (`taxonomy.asset.path`, 18 categorías y 10 cocinas) y
      afirma que cada id tiene estilo, más el caso de fallback (**verificación exigida**, Scenario 7).
- [ ] Las **4 copias** usan `RecipeArtwork` y **no queda ningún `PlateGlyph`** en `app/src`; el
      degradado+glifo solo vive en `RecipeArtwork.kt` (Scenario 4).
- [ ] Geometría y tamaños de glifo preservados (16:10, 96x96, alto 96, alto 240; 48/56/40/40/72 dp) y
      sin cambios en el comportamiento no-visual de las tarjetas/pantallas (Scenario 5).
- [ ] Sin hex sueltos: los acentos salen de roles de `MaterialTheme.colorScheme` y funcionan en claro y
      oscuro (Scenario 6).
- [ ] Sin dependencias nuevas; `app/build.gradle.kts` y `gradle/libs.versions.toml` intactos; sin tocar
      dominio, Taxonomía, `seed-recipes.json`, Room, `ui.theme/**`, `MainShell.kt`, `SazonApp.kt`,
      `MainActivity.kt` ni `init.sh`.
- [ ] Para UI: `Visual Design Impact` registra la **desviación** de capturas (exención de la persona,
      issue #77), **no** crea `docs/design/concepts/recipe-artwork/` y exige que la extensión quede
      registrada en el `DESIGN.md` actualizado (Scenario 8).
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `DESIGN.md` y
      `docs/technical-discovery.md` coherentes.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Un componente + un mapeo puro**: `RecipeArtwork` pinta; `RecipeArtworkStyle` decide. Permite testear
  en JVM el 100% del contrato (exhaustividad, determinismo, fallback) sin Compose, como el resto de la UI.
- **El acento es el degradado del bloque** (no solo el tinte del glifo): hace visible la diferencia entre
  dos recetas de la misma categoría (objetivo del issue) sin alterar el glifo, que identifica la categoría.
- **Acento por roles del tema, no por hex**: resuelve claro/oscuro gratis y respeta la regla "sin hex
  sueltos"; el conjunto se limita a `{primaryContainer, secondaryContainer, tertiaryContainer,
  surfaceVariant}` para no introducir "gradientes llamativos" (DESIGN.md).
- **El fallback reproduce exactamente el aspecto actual** (degradado `primaryContainer`→
  `secondaryContainer` + plato de dos círculos): el caso sin categoría/cocina no es un caso raro, así que
  hereda el look ya aceptado.
- **`RecipeArtwork` recibe la `Recipe`** (no `categoryId`/`cuisineId` sueltos): los 4 llamantes ya
  tienen una `Recipe` y se evita que cada llamante repita el mapeo; el mapeo por id sigue expuesto
  (`glyphFor`/`accentFor`) para el test que itera la Taxonomía.
- **El `modifier` del llamante conserva tamaño/ratio/clip**: no cambia ninguna geometría (16:10, 96x96,
  alto 96, alto 240) ni las esquinas (las redondea el `Surface` padre o el `clip`), así que las capturas
  aceptadas no se desmienten.
- **`ui.artwork` como paquete compartido**: coherente con `ui.theme`/`ui.preferences`/`ui.filters` y
  reutilizable por cualquier pantalla futura.
- **Sin dependencias nuevas, sin imágenes y `init.sh` sin cambios**.

### Unknowns

- **Fidelidad fina de los 18 glifos**: son line-art dibujado a mano con primitivas de Compose; el test
  garantiza la **exhaustividad y el determinismo**, no la calidad del trazo. El acabado de cada glifo lo
  revisa la persona; un glifo poco legible se corrige con feature correctiva.
- **Contraste del glifo sobre los 11 degradados**: el tinte se mantiene en `onSurfaceVariant` (el
  actual) y los degradados son claros/palidos en claro y oscuros en oscuro; si en el repaso visual algún
  par queda flojo, se ajusta el rol del acento con feature correctiva.
- **`docs/domain/taxonomy.md` §6** sigue diciendo `assets/img/recipes/{categoryId}.webp` (18 imágenes):
  drift **preexistente** (el MVP no empaqueta imágenes; `seed-recipe-catalog` ya lo declaró). Esta
  feature no edita la doc de la Taxonomía; se deja anotado para no confundir al validador.
- **Imágenes IA futuras**: cuando existan, `RecipeArtwork` es el único punto a cambiar (seam documentado
  en su KDoc); hoy no hay decisión sobre el formato/origen.
