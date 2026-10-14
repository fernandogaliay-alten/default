# Feature Implementation Spec: Fotos de plato por receta semilla y categoría

> **Superada (2026-10-14) por `docs/specs/retire-recipe-photos.md`.** La feature `retire-recipe-photos`
> (ADR 0006) **revierte** esta spec: los 30 assets de `seed/` y `category/` y toda la cadena local
> (`data.RecipePhotoAssets.resolve/candidates`, `data.AssetRecipePhotoSource`,
> `ui.artwork.RecipePhotoSource`) fueron retirados del APK y del runtime; se **conserva** solo
> `img/recipes/default.jpg` como ancla de estilo de `dish-style-reference`. El fuente de verdad vigente
> es `docs/adr/0006-typographic-recipe-cards.md`. Esta spec se conserva como **evidencia histórica** (no
> se reescriben sus escenarios).

## Source Feature

- `id`: `recipe-photos`
- `area`: `ui` (con trabajo de assets + un script de empaquetado)
- `title`: "Fotos de plato por receta semilla y categoría"
- `depends_on`: `["recipe-artwork"]` — `accepted` (validación independiente, sesión 078). Es la **única**
  dependencia y está satisfecha.
- `status`: `in_progress` en `feature_list.json` (el planner **no** modifica `feature_list.json` ni
  `PROGRESS.md`; el estado lo llevan implementer/validador/orquestador).
- `source`: `feature_list.json` + issue **#83**
  (`[recipe-photos] Fotos de plato por receta semilla y categoría`), que es el brief autoritativo.
- Verificación exigida (de la feature, literal): capturas obligatorias (ya entregadas);
  `./gradlew test` cubre en JVM la cadena de respaldo (semilla → categoría → por defecto) y que **cada
  receta semilla, cada una de las 18 categorías y el caso por defecto resuelven un asset existente**;
  las 4 copias del placeholder usan el componente único `RecipeArtwork` **con la foto**, con el glifo
  dibujado **solo como último recurso** si faltara el asset; y prueba manual de emulador (claro/oscuro,
  ninguna receta sin foto).
- **Gate de capturas de UI: satisfecho.** Existen y son válidas (v2, 2026-10-13):
  `docs/design/concepts/recipe-photos/README.md`, `.../stitch-prompt.md`,
  `.../fotos-semillas/{screen.png,code.html,DESIGN.md}` y
  `.../fotos-categorias/{screen.png,code.html,DESIGN.md}`. Ver `Visual Design Impact`.

## Goal

Que las tarjetas y el Detalle muestren una **fotografía del plato** —empaquetada como **asset offline**,
sin red y sin dependencias nuevas— en lugar del bloque plano con glifo, con una **cadena de respaldo
determinista**: **foto propia de la receta semilla → foto de su categoría → foto genérica por defecto**.
El `RecipeArtwork` actual (degradado por cocina + glifo por categoría, `recipe-artwork`) se conserva
**intacto** y pasa a ser el **último recurso** cuando no exista ningún asset utilizable. La feature
incluye la **descarga y el empaquetado** de las 31 imágenes (12 recetas semilla + 18 categorías + 1 por
defecto) a partir de los `src` expuestos por las capturas.

## Non-Goals

- **No** se añaden dependencias: ni Coil (ni ninguna librería de imágenes/iconos). Se usa
  `android.content.res.AssetManager` + `android.graphics.BitmapFactory` + `bitmap.asImageBitmap()`.
  `app/build.gradle.kts` cambia **solo** para publicar la ruta de los assets en los tests JVM;
  `gradle/libs.versions.toml` queda intacto.
- **No** se toca el dominio: `Recipe` **no** gana campo de imagen (`docs/domain-model.md` intacto);
  tampoco `RecipeArtworkStyle`/`RecipeGlyph`/`RecipeAccent` (siguen siendo el fallback).
- **No** se cambia ningún comportamiento no-visual de las 4 tarjetas/pantallas (título, meta, píldora de
  coincidencia, corazón, click, pestañas, barra inferior) ni su **geometría** (16:10 de las tarjetas,
  96x96 y alto 96 de Favoritos, hero 240 del Detalle).
- **No** se añaden pantallas, estados ni microcopy. **No** se toca `init.sh` (el script de descarga es
  una herramienta de una sola vez, **no** forma parte del gate).
- **No** se descargan imágenes en tiempo de ejecución ni se cachean en disco: las fotos van en el APK.
- **No** se implementa el modo oscuro "de nuevo": las fotos son las mismas y los roles del tema siguen
  gobernando el fallback.

## Job Story

Cuando hojeo mis recetas, quiero **ver el plato** en lugar de un bloque de color con un icono, para
reconocer de un vistazo qué voy a cocinar y para que la app se sienta apetitosa (DESIGN.md: "la foto del
plato siempre presente", "deja que la foto del plato sea la protagonista") sin perder el
funcionamiento offline ni la disciplina de alcance del MVP.

## Users And Permissions

- **Usuario local**: dueño del dispositivo (ADR 0002). Ve Home, Resultados, Favoritos (lista y rejilla)
  y el Detalle. Sin cuentas, sin roles, sin red.
- **Desarrollador / agente**: cubre en JVM la **cadena de respaldo** y la **existencia real de los
  assets**; el acabado (encuadre, color, legibilidad) lo revisa la persona en el emulador.
- **Sistema**: la resolución es local, determinista y de solo lectura; sin `Random`, sin reloj, sin red.

## Acceptance Scenarios

### Scenario 1: foto propia de la receta semilla

Given una receta semilla del catálogo (`source = seed`, `id = seed-*`) cuya foto está empaquetada,
When `RecipeArtwork` pinta su imagen,
Then se muestra la **foto de esa receta** (p. ej. `img/recipes/seed/seed-tortilla-patatas.<ext>`) y
**no** el bloque con glifo.

### Scenario 2: respaldo a la foto de la categoría (recetas generadas por IA)

Given una receta `source = ai` (id `ai-<hash>`, sin foto propia) con `categoryId = "rice"`,
When `RecipeArtwork` pinta su imagen,
Then se muestra la **foto de su categoría** (`img/recipes/category/cat-rice.<ext>`).

### Scenario 3: respaldo parcial semilla → categoría

Given una receta semilla cuya foto propia **no** está empaquetada (caso negativo simulado con un
`exists` falso) pero cuya categoría sí tiene foto,
When se resuelve la cadena,
Then se devuelve la **foto de la categoría** (la cadena no se corta en el primer fallo).

### Scenario 4: respaldo final a la foto por defecto y, si no hay nada, al glifo

Given una receta con `categoryId` nulo, vacío o **fuera de las 18 categorías reales**,
When se resuelve la cadena,
Then se devuelve `img/recipes/default.<ext>`; y si **ningún** candidato existe, la resolución devuelve
`null` y `RecipeArtwork` pinta el **degradado + glifo actuales** (`RecipeArtworkStyle`, sin cambios):
nunca una excepción ni un bloque vacío.

### Scenario 5: exhaustividad de assets (verificación exigida)

Given `docs/domain/taxonomy.json` y `app/src/main/assets/seed-recipes.json` (fuentes versionadas,
leídas por test con propiedades de sistema, mismo patrón que `TaxonomyIntegrityTest`/`SeedRecipeCatalogTest`),
When corre `./gradlew test`,
Then un test JVM afirma que **las 12 recetas semilla** (`seed-*`), **las 18 `recipeCategories`** y el
caso **`default`** resuelven cada uno a un fichero de asset **existente y no vacío** dentro de
`app/src/main/assets/img/recipes/`.

### Scenario 6: un único componente, 4 puntos de uso, sin copias nuevas

Given el código actual (`RecipeArtwork` ya sustituyó las 4 copias del placeholder),
When se implementa esta feature,
Then los mismos **5 puntos de llamada en 4 ficheros** (`RecipeCard`, `ResultRecipeCard`,
`FavoriteRecipeCard` ×2 y `RecipeDetailScreen`) siguen siendo la **única** vía de pintar la imagen y no
aparece ningún `PlateGlyph`, ningún `BitmapFactory` ni ninguna carga de assets fuera de
`data.AssetRecipePhotoSource`.

### Scenario 7: geometría y encuadre preservados

Given las capturas y el layout ya aceptados,
When se sustituye el bloque por la foto,
Then se conservan el **16:10** de las tarjetas, el **96x96** y el **alto 96** de Favoritos y el **hero de
240** del Detalle, con recorte tipo *cover* (`ContentScale.Crop`) y **sin** cambiar esquinas ni
tamaños. (Deviación documentada: `DESIGN.md` y la lámina piden 16:11 para "media"; se mantiene el 16:10
aceptado y `Crop` absorbe la diferencia; ver `Decisions And Unknowns`.)

### Scenario 8: offline, sin dependencias y con presupuesto de tamaño

Given el APK y el dispositivo sin red,
When se abren Home, Resultados, Favoritos y Detalle,
Then las fotos se ven **sin conexión**, `app/build.gradle.kts`/`gradle/libs.versions.toml` **no** ganan
dependencias, y el total de `app/src/main/assets/img/recipes/` no supera **8 MB** (objetivo ~2-4 MB con
WebP; `docs/technical-discovery.md` ya lo pide).

### Scenario 9: claro/oscuro y accesibilidad de la imagen

Given `SazonTheme` en claro y en oscuro,
When se pinta una tarjeta o el hero,
Then la foto se ve igual en ambos modos (no depende de roles de color) y el **texto sobre la foto**
mantiene su contraste (la barra/borde inferior del hero ya lo resolvía); el glifo del fallback sigue
saliendo de roles del tema.

### Scenario 10: documentación durable alineada

Given que esta feature **cambia una decisión del MVP** (la imagen pasó de placeholder dibujado a fotos
empaquetadas),
When se implementa,
Then existe un **ADR nuevo** (`docs/adr/0004-*.md`) que lo justifica y las docs `docs/build-brief.md`,
`docs/technical-discovery.md`, `DESIGN.md`, `docs/risks-and-open-questions.md` y
`docs/domain/taxonomy.md` (§6) quedan **coherentes** con el nuevo comportamiento.

## Repository Research

### Files Inspected

- `feature_list.json` (entry `recipe-photos`: verificación literal, dependencia `recipe-artwork`,
  notas del brief) y `PROGRESS.md` (estado verificado, `recipe-artwork` aceptada en la sesión 078,
  política de capturas y `Backfill pendiente`).
- `AGENTS.md` — UI verificada por lógica/estado en JVM + capturas obligatorias; nunca `./gradlew`
  directo (usar `bash init.sh` o `bash scripts/gradle.sh`); una feature por sesión; disciplina de
  alcance; actualizar artefactos del repo.
- `docs/design/concepts/recipe-photos/README.md` — **v2 válida**: los dos lotes exposan imágenes
  **separadas** a 1376x768 nativas, descargar cada `src` con sufijo `=s0`, mapeo **por posición** de los
  12 `seed-*` (el `alt` **no** es la clave) y por `alt` de los 19 `cat-*`/`default`.
- `docs/design/concepts/recipe-photos/stitch-prompt.md` — el prompt que originó las fotos: lista
  ordenada de los 12 `seed-*` y de los 19 `cat-*`/`default` con la descripción de cada plato.
- `docs/design/concepts/recipe-photos/fotos-semillas/{code.html,DESIGN.md,screen.png}` — **12 `<img>`**
  con `src` de `lh3.googleusercontent.com/aida-public/...` y comentarios `<!-- NN. <clave> -->`
  (la clave de cada fila está en el comentario, en orden).
- `docs/design/concepts/recipe-photos/fotos-categorias/{code.html,DESIGN.md,screen.png}` — **19 `<img>`**
  con `src` de `lh3.googleusercontent.com/aida/...` y `alt` **exacto** de cada clave.
- `docs/specs/recipe-artwork.md` (aceptada) — componente único `ui.artwork.RecipeArtwork`,
  `RecipeArtworkStyle` puro (glifo por `categoryId`, acento por `cuisineId`, fallback total), geometría
  y tamaños conservados, y el KDoc que la declara "único punto a cambiar" para imágenes futuras.
- `app/src/main/java/com/sazon/app/ui/artwork/{RecipeArtwork.kt,RecipeArtworkStyle.kt}` — el componente
  (Box con `Brush.linearGradient` + `Canvas` del glifo) y el mapeo puro que **no se tocan**.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt`, `.../results/ResultRecipeCard.kt`,
  `.../favorites/FavoriteRecipeCard.kt` (×2) y `.../detail/RecipeDetailScreen.kt` — los 5 puntos de
  llamada (16:10 / 56 / 96x96 y alto 96 / hero 240).
- `app/src/main/java/com/sazon/app/data/AssetSeedRecipeSource.kt` y `AssetTaxonomySource.kt` — patrón de
  costura de assets (`context.assets.open(...)`), paquete `data` sin dependencias de `ui`/`domain` sucios.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — composición explícita **sin DI** de repositorios,
  `Taxonomy`, `seedRecipes` y servicios, y `setContent { SazonTheme { SazonApp(...) } }`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — 11 parámetros explícitos; raíz de la app.
- `app/build.gradle.kts` — `systemProperty("taxonomy.asset.path"/"seed.recipes.asset.path"/
  "design.tokens.path", ...)` es el patrón para que los tests JVM lean ficheros versionados.
- `app/src/test/java/com/sazon/app/ui/artwork/RecipeArtworkStyleTest.kt`,
  `.../domain/taxonomy/TaxonomyIntegrityTest.kt`, `.../domain/recipe/SeedRecipeCatalogTest.kt` — patrón
  de test JVM que itera la Taxonomía real y el catálogo semilla real.
- `app/src/main/assets/{seed-recipes.json,taxonomy.json}`, `.gitignore` (solo `taxonomy.json` está
  ignorado; `seed-recipes.json` es fuente versionada) y `.gitattributes` (ya hay `*.webp binary`).
- `docs/domain/taxonomy.md` §6 — dice `assets/img/recipes/{categoryId}.webp` (18 imágenes): la
  convención real la fija esta feature.
- `docs/technical-discovery.md` (tabla "Iconos e imágenes de receta", párrafo de `RecipeArtwork`,
  Constraint "Tamaño del APK … WebP y resolución moderada"), `DESIGN.md` (Product Feel, `imageRatio`,
  Components → "Imagen de receta", Open Design Question 4 resuelta el 2026-10-12 por `recipe-artwork`),
  `docs/build-brief.md` (Non-Goals y el precedente de ampliación `cooking-mode`),
  `docs/risks-and-open-questions.md` (Resueltas → "Licencias de iconos y fotos de receta: **no
  bloqueante**"), `docs/adr/0001-taxonomy-in-code.md` (formato de ADR).
- `scripts/import-stitch-captures.sh` — precedente de script del repo; `scripts/gradle.sh` — wrapper
  obligatorio para ejecutar Gradle desde una sesión de agente.

### Existing Patterns To Follow

- **Lógica pura + capa Android/Compose delgada**: la cadena de respaldo vive en Kotlin sin Android ni
  Compose y se testea en JVM; la lectura de assets es una costura en `com.sazon.app.data`.
- **Costura de assets por convención**: `AssetXxxSource(context)` en `com.sazon.app.data`, sin DI.
- **Tests JUnit 4** en `app/src/test/**`, nombres en español con `_`, con **caso negativo que no pasa en
  vacío**, leyendo ficheros versionados vía `System.getProperty(...)` publicado por `app/build.gradle.kts`.
- **Tokens del tema, nunca hex sueltos** (aplica al fallback, que no cambia).
- **`init.sh` es el gate no bloqueante** (`./gradlew test` + `./gradlew lint`) y **no** cambia.

### Current Gaps

- No existe ningún asset de imagen de receta (`app/src/main/assets/` solo tiene el catálogo y la
  Taxonomía generada).
- No existe cadena de respaldo ni convención de nombres para fotos de receta.
- `RecipeArtwork` no puede leer assets: no hay loader ni seam para inyectarlo.
- `docs/domain/taxonomy.md` §6 describe una convención de imágenes que **nunca** existió.

## Technical Approach

Cinco piezas: **assets + script de empaquetado**, **resolución pura**, **costura Android**, **seam
Compose** y **`RecipeArtwork` con foto + fallback al glifo**, más las docs durables y el ADR.

### 1. Assets y script de empaquetado (alcance de la feature)

Convención de nombres en `app/src/main/assets/img/recipes/` (fuente **versionada**, como
`seed-recipes.json`; **no** se añade a `.gitignore`):

| Grupo | Fichero | Clave | Nº |
|---|---|---|---|
| Foto propia | `seed/<seedRecipeId>.<ext>` | `id` de la receta semilla | 12 |
| Foto de categoría | `category/cat-<categoryId>.<ext>` | `categoryId` real | 18 |
| Foto por defecto | `default.<ext>` | `default` (genérica/neutra) | 1 |

- `<ext>`: una de `webp`, `jpg`, `png` (la que exista; el código la **prueba** en ese orden y no asume
  formato).
- **Script nuevo** `scripts/fetch-recipe-photos.sh` (bash, WSL, idempotente, con `--force`):
  1. extrae los `src` de `fotos-semillas/code.html` (12) emparejando con la clave del comentario
     `<!-- NN. <clave> -->` y **valida** que son exactamente los 12 `seed-*` esperados y **en orden**;
  2. extrae los `src` de `fotos-categorias/code.html` (19) por `alt` y **valida** que son exactamente
     los 19 esperados;
  3. descarga cada `src` añadiendo el sufijo **`=s0`** (sin sufijo el host devuelve un thumbnail de
     512 px) y elige la extensión real por *magic bytes*;
  4. si existen `cwebp` o `magick`/`convert`, convierte a **WebP q80** y redimensiona a **ancho máximo
     960 px** (Constraint "Tamaño del APK" de `docs/technical-discovery.md`); si no existe ninguna
     herramienta, conserva el original y **lo registra** en la salida;
  5. escribe en `app/src/main/assets/img/recipes/{seed,category}/` y `default.<ext>`; **falla con código
     distinto de 0** si falta un `src`, si el conteo no es 12/19 o si una descarga falla (nunca un
     empaquetado parcial silencioso);
  6. imprime un inventario final (nº de ficheros, bytes totales, extensión usada por grupo).
- El script **no** se ejecuta desde `init.sh` (necesita red): es una herramienta de una sola vez que
  corre el implementer. Si la red a `lh3.googleusercontent.com` no está disponible, **se detiene y se
  reporta bloqueado**; no se inventan assets.
- Presupuesto: total ≤ **8 MB** (objetivo ~2-4 MB con WebP).
- `.gitattributes`: añadir `*.jpg binary`, `*.jpeg binary` y `*.png binary` (ya existe `*.webp binary`).

### 2. Resolución pura — `app/src/main/java/com/sazon/app/data/RecipePhotoAssets.kt` (nuevo)

Sin Android, sin Compose, sin I/O (testeable en JVM):

```kotlin
object RecipePhotoAssets {
    const val ROOT = "img/recipes"                       // raíz dentro de assets/
    const val DEFAULT_KEY = "default"
    val EXTENSIONS = listOf("webp", "jpg", "png")        // orden de prueba
    val CATEGORY_IDS = setOf(/* las 18 recipeCategories reales */)

    fun candidates(recipeId: String, categoryId: String?): List<String>   // rutas SIN extensión, en orden
    fun resolve(recipeId: String, categoryId: String?, exists: (String) -> Boolean): String?
}
```

- `candidates("seed-tortilla-patatas", "egg_breakfast")` →
  `["img/recipes/seed/seed-tortilla-patatas", "img/recipes/category/cat-egg_breakfast", "img/recipes/default"]`.
- Un `categoryId` **fuera** de las 18 reales (`null`, `""`, `"no-existe"`) **no** produce candidato de
  categoría (`cat-other` solo con el id real `other`); la cadena salta directo a `default`.
- `resolve` recorre los candidatos **en orden** y, para cada uno, prueba `EXTENSIONS` en orden, devolviendo
  la primera ruta **completa** cuyo `exists` sea verdadero; si no hay ninguna, devuelve `null`.
- **Determinista** y sin estado: mismos argumentos → mismo resultado.
- El **test** usa `exists = { File(assetsDir, it).isFile }` y, en la app, `exists` es la lectura real de
  assets; la misma función sirve a los dos.

### 3. Costura Android — `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` (nuevo)

- `class AssetRecipePhotoSource(context: Context)` con:
  - `private fun exists(path: String): Boolean` (`context.assets.open(path).close()` en `try/catch`
    `IOException`/`FileNotFoundException`);
  - `suspend fun bitmapFor(recipe: Recipe): android.graphics.Bitmap?` = `withContext(Dispatchers.IO)`
    → `RecipePhotoAssets.resolve(recipe.id, recipe.categoryId, ::exists)` → `BitmapFactory.decodeStream`
    con `inPreferredConfig = RGB_565`… **decisión**: `ARGB_8888` por simplicidad y fidelidad (los
    assets están acotados a ≤960 px de ancho);
  - caché **LRU propia** de bitmaps decodificados (`LinkedHashMap` con `removeEldestEntry`,
    `MAX_CACHED_PHOTOS = 16`) para no re-decodificar al hacer scroll;
  - devuelve `null` (nunca lanza) si no hay ruta o la decodificación falla.
- Devuelve `android.graphics.Bitmap` (**no** un tipo de Compose): `com.sazon.app.data` sigue **sin**
  depender de `ui`, igual que `AssetSeedRecipeSource`/`AssetTaxonomySource`.

### 4. Seam Compose — `app/src/main/java/com/sazon/app/ui/artwork/RecipePhotoSource.kt` (nuevo)

- `fun interface RecipePhotoSource { suspend fun photoFor(recipe: Recipe): ImageBitmap? }`.
- `val LocalRecipePhotoSource = staticCompositionLocalOf<RecipePhotoSource> { RecipePhotoSource { null } }`
  — el valor por defecto **no** produce foto, así que cualquier host sin proveedor (previews, tests)
  sigue pintando el degradado + glifo de hoy: comportamiento seguro por defecto.
- Se elige un `CompositionLocal` (y **no** `LocalContext`) porque: (a) mantiene `RecipeArtwork` sin
  `Context`, (b) permite un valor por defecto degradado, (c) evita teñir de Android la capa Compose
  compartida y (d) evita reescribir las firmas de `SazonApp`, `MainShell`, `HomeScreen`, `ResultsScreen`
  `GeneratorFlow`, `FavoritesScreen` y `RecipeDetailScreen`, que hoy solo reciben la `Recipe`.
  Alternativa descartada: pasar el loader como parámetro por 5 puntos de llamada y 6 contenedores.

### 5. `RecipeArtwork` con foto y glifo como último recurso (modificado)

`suspend fun RecipeArtwork(recipe, modifier, glyphSize)` conserva **exactamente** su firma pública y su
geometría; internamente:

```kotlin
val source = LocalRecipePhotoSource.current
val photo by produceState<ImageBitmap?>(null, recipe.id, recipe.categoryId) { value = source.photoFor(recipe) }
if (photo != null) {
    Image(bitmap = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
} else {
    /* bloque actual: Box + Brush.linearGradient(accentColors(style.accent)) + RecipeGlyphCanvas */
}
```

- `contentDescription = null`: la imagen es **decorativa** (el título de la receta ya se anuncia);
  mantenerlo igual que el placeholder actual no cambia la semántica de accesibilidad.
- El glifo y el degradado **no** cambian de código ni de tokens: `RecipeArtworkStyle` intacto.
- `modifier` sigue aportando ratio/tamaño/recorte (16:10, 96x96, alto 96, hero 240) y las esquinas las
  sigue redondeando el `Surface` padre o el `clip` del llamante.

### 6. Cableado — `MainActivity.kt` (modificado)

Envolver `SazonApp(...)` con el proveedor del seam, con el source Android ya compuesto (sin DI, como el
resto): `CompositionLocalProvider(LocalRecipePhotoSource provides RecipePhotoSource { recipe ->
assetRecipePhotoSource.bitmapFor(recipe)?.asImageBitmap() })`.

### 7. Docs durables y ADR

- **ADR nuevo `docs/adr/0004-recipe-photos-as-assets.md`** (formato de `0001`, con
  Estado/Contexto/Decisión/Alternativas consideradas/Consecuencias): las fotos de plato viajan
  **empaquetadas como assets offline**; ajusta la decisión "sin imágenes" y fija la convención de
  nombres, la cadena de respaldo, el descarte de Coil/red y las consecuencias (tamaño del APK,
  "añadir una receta semilla = añadir su foto").
- `DESIGN.md`: reescribir el bullet **Imagen de receta** de `Components` (la foto es la imagen
  principal; el glifo/degradado es el último recurso), registrar la dirección visual de las dos láminas
  y enmendar la Open Design Question 4 (resuelta por `recipe-photos`, issue #83).
- `docs/technical-discovery.md`: actualizar la fila "Iconos e imágenes de receta" y el párrafo de
  `RecipeArtwork` (assets + cadena semilla→categoría→default + glifo como último recurso), y la
  Constraint de tamaño del APK (ahora hay fotos: ≤8 MB, WebP).
- `docs/build-brief.md`: registrar la **ampliación del MVP** en `Non-Goals` (mismo estilo que el bullet
  de `cooking-mode`): las fotos de plato entran en el MVP como assets offline. *Nota de precisión*: hoy
  `build-brief.md` **no** contiene un bullet literal "sin imágenes ni assets"; la restricción vive en
  `DESIGN.md` (OQ4) y en `docs/technical-discovery.md`, que **también** se ajustan.
- `docs/risks-and-open-questions.md`: precisar el bullet de licencias (fotos generadas por nosotros con
  el prompt de Stitch; app personal no distribuida) y añadir el riesgo de tamaño/decodificación.
- `docs/domain/taxonomy.md` §6: sustituir la convención inventada
  (`assets/img/recipes/{categoryId}.webp`) por la real (`img/recipes/{seed,category}/…` + `default`).
- `PROGRESS.md`/`feature_list.json`: evidencia y estado, **al implementar** (no en esta sesión).

## Expected File Changes

**Crear**

- `scripts/fetch-recipe-photos.sh` — descarga + valida + empaqueta las 31 fotos.
- `app/src/main/assets/img/recipes/seed/seed-<…>.<ext>` (12 ficheros) y
  `app/src/main/assets/img/recipes/category/cat-<…>.<ext>` (18) y `default.<ext>` (1) — **assets
  versionados** (no ignorados).
- `app/src/main/java/com/sazon/app/data/RecipePhotoAssets.kt` — cadena de respaldo **pura**.
- `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` — lectura/decodificación de assets.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipePhotoSource.kt` — seam Compose + `LocalRecipePhotoSource`.
- `app/src/test/java/com/sazon/app/data/RecipePhotoAssetsTest.kt` — escenarios 1-5, 8 y casos negativos.
- `docs/adr/0004-recipe-photos-as-assets.md` — ADR.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — foto primero; degradado+glifo como
  último recurso (firma y geometría intactas).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer `AssetRecipePhotoSource` y proveer el seam.
- `app/build.gradle.kts` — **solo** `systemProperty("recipe.photos.assets.dir", <…>/src/main/assets/img/recipes)`.
- `.gitattributes` — `*.jpg`/`*.jpeg`/`*.png` binary.
- `DESIGN.md`, `docs/technical-discovery.md`, `docs/build-brief.md`,
  `docs/risks-and-open-questions.md`, `docs/domain/taxonomy.md`.
- `feature_list.json` y `PROGRESS.md` — **al implementar**.

**No se toca**: `gradle/libs.versions.toml`, `domain/**` (`Recipe`/`RecipeArtworkStyle`), la Taxonomía
(`docs/domain/taxonomy.json`, `Taxonomy`/`TaxonomyJson`), `seed-recipes.json`, `data/**` de
Room/Perfil/Lote/IA, `ui.theme/**`, `ui/detail/**` (salvo que compile por el cambio de `RecipeArtwork`,
que no le afecta), `init.sh` y `scripts/gradle.sh`.

## Visual Design Impact

- UI involved: **sí** — imagen de receta en `RecipeCard` (Home), `ResultRecipeCard` (Resultados),
  tarjeta y miniatura de Favoritos y hero del Detalle; claro y oscuro; y el fallback.
- **Capturas obligatorias (gate de UI satisfecho, v2 2026-10-13)** — son **láminas de fotos**, no
  pantallas de la app, y son la **fuente de las imágenes** y la dirección visual del acabado:
  - `docs/design/concepts/recipe-photos/README.md` — mapeo, claves y procedimiento de descarga.
  - `docs/design/concepts/recipe-photos/stitch-prompt.md` — lista ordenada de platos y claves.
  - `docs/design/concepts/recipe-photos/fotos-semillas/screen.png` (+ `code.html`, `DESIGN.md`) —
    lote A: 12 fotos, una por receta semilla, en el **orden** de la tabla del README.
  - `docs/design/concepts/recipe-photos/fotos-categorias/screen.png` (+ `code.html`, `DESIGN.md`) —
    lote B: 19 fotos, una por categoría (`cat-*`) **más** `default`.
- Design source: `DESIGN.md` (Product Feel "la foto del plato siempre presente"; `recipe-card.imageRatio:
  16:10`; `rounded.md: 24px`; Do's "deja que la foto del plato sea la protagonista") + las dos láminas.
  Ante contradicción, manda `DESIGN.md`.
- New design artifact required: **no** (las capturas ya están versionadas; el estado queda registrado en
  `DESIGN.md`).
- **Deviación documentada**: la lámina pide 16:11 para "media" y el layout aceptado es 16:10 (tarjetas),
  96x96 (miniatura), alto 96 (rejilla) y hero 240; **se mantiene** la geometría aceptada y la foto se
  recorta con `ContentScale.Crop`. No se cambian esquinas ni tamaños.
- Sin hex sueltos: el fallback sigue con roles de `MaterialTheme.colorScheme`.
- Un defecto visual (encuadre, foto poco apetitosa, foto equivocada) se corrige con una **feature
  correctiva**, no bloquea el flujo (regla de `AGENTS.md`).

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio no lo usa (decisión del harness).
- `CONSTRAINTS.md`: **not needed** — no existe; no nace una regla MUST de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian (el script nuevo no entra
  en `init.sh`).
- `docs/adr/`: **create** — `0004-recipe-photos-as-assets.md` (cambia la decisión "sin imágenes").
- `docs/build-brief.md`: **update** — registrar la ampliación del MVP (fotos como assets offline).
- `docs/technical-discovery.md`: **update** — imagen de receta, ubicación de los assets y del loader,
  tamaño del APK, tests nuevos.
- `docs/domain/taxonomy.md`: **update** — §6 pasa de convención inventada a la real.
- `docs/domain-model.md`: **not needed** — `Recipe` no gana campo; la imagen se resuelve por convención.
- `DESIGN.md`: **update** — Components (Imagen de receta), dirección visual de las láminas y OQ4.
- `docs/risks-and-open-questions.md`: **update** — licencias de fotos (precisión) y riesgo de tamaño.
- `docs/design/concepts/recipe-photos/`: **no change** — las capturas ya están entregadas y válidas.
- `PROGRESS.md` y `feature_list.json`: **update al implementar**.

## Implementation Plan

1. `scripts/fetch-recipe-photos.sh` + descarga y empaquetado de los 31 assets (validar conteos, formato
   y presupuesto ≤8 MB) y `.gitattributes`.
2. `data/RecipePhotoAssets.kt` (cadena pura) y `RecipePhotoAssetsTest` (incluida la verificación
   exigida de assets existentes vía `recipe.photos.assets.dir`); publicar la propiedad en
   `app/build.gradle.kts`.
3. `data/AssetRecipePhotoSource.kt` (lectura/decodificación + LRU, devuelve `null` sin lanzar).
4. `ui/artwork/RecipePhotoSource.kt` (seam + `LocalRecipePhotoSource` con default nulo).
5. `ui/artwork/RecipeArtwork.kt`: foto → `Image(Crop)`; si no hay, el bloque actual.
6. `MainActivity.kt`: componer el source y proveer el seam.
7. Docs: ADR 0004 + `DESIGN.md`, `docs/technical-discovery.md`, `docs/build-brief.md`,
   `docs/risks-and-open-questions.md`, `docs/domain/taxonomy.md`.
8. `bash init.sh` en verde, tirada focal del test y `assembleDebug` para comprobar el empaquetado;
   registrar evidencia.

## Implementation Tasks

- [ ] Escribir `scripts/fetch-recipe-photos.sh` (extracción, validación de claves/orden, `=s0`,
      WebP/960 px si hay herramienta, inventario, `--force`, exit ≠ 0 ante fallo).
- [ ] Ejecutarlo y empaquetar los **31** assets en `app/src/main/assets/img/recipes/{seed,category}/` +
      `default`; comprobar ≤8 MB; añadir `*.jpg`/`*.jpeg`/`*.png binary` a `.gitattributes`.
- [ ] Crear `data/RecipePhotoAssets.kt` (`ROOT`, `DEFAULT_KEY`, `EXTENSIONS`, `CATEGORY_IDS` con las 18
      categorías reales, `candidates`, `resolve`) **sin** Android ni Compose.
- [ ] Publicar `systemProperty("recipe.photos.assets.dir", …)` en `app/build.gradle.kts`.
- [ ] Escribir `RecipePhotoAssetsTest`: cadena semilla→categoría→default; fallo del primer candidato;
      categoría desconocida → solo `default`; nada existe → `null`; determinismo; **las 12 semillas, las
      18 categorías y `default` resuelven a un fichero real no vacío**; casos negativos que no pasan en vacío.
- [ ] Crear `data/AssetRecipePhotoSource.kt` (existe/decodifica en `Dispatchers.IO`, LRU 16, `null` sin
      excepción, devuelve `android.graphics.Bitmap`).
- [ ] Crear `ui/artwork/RecipePhotoSource.kt` (`fun interface` + `LocalRecipePhotoSource` con default nulo).
- [ ] Modificar `RecipeArtwork`: `produceState` + `Image(ContentScale.Crop)` y, si `null`, el bloque
      actual; firma, geometría y `RecipeArtworkStyle` intactos.
- [ ] Cablear `MainActivity` (`AssetRecipePhotoSource` + `CompositionLocalProvider`).
- [ ] Escribir el ADR 0004 y actualizar `DESIGN.md`, `docs/technical-discovery.md`,
      `docs/build-brief.md`, `docs/risks-and-open-questions.md`, `docs/domain/taxonomy.md`.
- [ ] `bash init.sh` en verde + tirada focal + `assembleDebug`; evidencia en `feature_list.json`/`PROGRESS.md`.

## Verification Plan

- `bash init.sh` (vía `scripts/gradle.sh`) → **esperado**: exit 0 (`./gradlew test` + `./gradlew lint`),
  sin procesos largos. Es el gate no bloqueante y **no cambia** (no se añaden modos; el script de
  descarga queda **fuera** del gate).
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.RecipePhotoAssetsTest"`
  → **esperado**: verde.
- `./gradlew test` → **esperado**: `RecipePhotoAssetsTest` en verde sin romper las 60 suites existentes
  (incluida `RecipeArtworkStyleTest`, que debe seguir pasando sin cambios):
  - **Scenario 5 (verificación exigida)**: itera `SeedRecipeJson.parse` del catálogo real
    (`seed.recipes.asset.path`) y `TaxonomyJson.parse` de la Taxonomía real (`taxonomy.asset.path`) y,
    con `exists = { File(System.getProperty("recipe.photos.assets.dir"), it).isFile }`, afirma que cada
    una de las 12 recetas semilla, cada una de las 18 `recipeCategories` y `default` resuelven a un
    fichero existente de tamaño > 1 KB.
  - Cadena: semilla con foto propia → ruta `seed/…`; semilla sin foto propia (predicado falso) →
    `category/cat-…`; receta `ai-…` → `category/cat-…`; categoría desconocida o `null` → `default`.
  - Fallback total: ningún candidato existe → `null` (el glifo es el último recurso) y sin excepción.
  - Determinismo: dos llamadas iguales devuelven la misma ruta.
  - **Casos negativos**: `cat-no-existe` **nunca** aparece en `candidates`; un id inventado con una
    categoría inventada devuelve `null` (no cae a una categoría real); el predicado de existencia falso
    no deja pasar ninguna ruta.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `bash scripts/gradle.sh assembleDebug` + inspección del APK (`unzip -l`) → **esperado**: los 31 assets
  presentes en `assets/img/recipes/` y tamaño total ≤ 8 MB.
- **Grep/inspección**: `RecipeArtwork` sigue siendo el único punto de imagen (los 4 ficheros, 5 usos);
  **0** apariciones de `PlateGlyph`; ningún `BitmapFactory`/`AssetManager` fuera de
  `data/AssetRecipePhotoSource.kt`; `RecipeArtworkStyle.kt` sin cambios; `gradle/libs.versions.toml`,
  `domain/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`, `ui.theme/**` e `init.sh` intactos;
  `git diff --stat` solo con lo previsto.
- **Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI)**: abrir Home, Resultados,
  Favoritos (lista y rejilla) y Detalle y comprobar que **todas** muestran foto (claro y oscuro), que una
  receta generada por IA muestra la foto de su categoría, que dos recetas de la misma categoría con
  fotos propias no se confunden, que el encuadre no recorta el plato de forma molesta y que si se
  eliminara un asset (prueba destructiva opcional) aparece el glifo en vez de un hueco.
- Cobertura E2E: **no aplica** — no existe suite E2E persistente y `AGENTS.md` prohíbe automatizar UI de
  emulador. El contrato verificable (cadena de respaldo, exhaustividad y existencia de assets,
  determinismo, fallback) se cubre en JVM.

## Evidence To Capture

- Inventario de assets: nº de ficheros por grupo (12/18/1), extensión usada, bytes totales (≤8 MB) y
  salida del script de descarga.
- Salida de `./gradlew test` con `RecipePhotoAssetsTest` (nº de tests, 0 fallos) y el total de la suite;
  evidencia de que el test **itera la Taxonomía y el catálogo semilla reales** (12 + 18 + 1).
- `lint` con 0 errores y sin warnings nuevos en ficheros de la feature; `init.sh` con código 0.
- `unzip -l` del APK de debug mostrando `assets/img/recipes/**`.
- Grep: `RecipeArtwork` en los 4 ficheros, 0 `PlateGlyph`, 0 `BitmapFactory` fuera de
  `data/AssetRecipePhotoSource.kt`.
- Inspección: `RecipePhotoAssets` puro (sin Android/Compose), `RecipeArtworkStyle.kt` y el resto del
  dominio sin cambios, sin dependencias nuevas.
- `DESIGN.md`, `docs/technical-discovery.md`, `docs/build-brief.md`,
  `docs/risks-and-open-questions.md`, `docs/domain/taxonomy.md` y `docs/adr/0004-*.md` coherentes.
- Resultado manual en emulador (descripción) de las 4 pantallas en claro y oscuro.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Validator Checklist

- [ ] Existen **31** assets reales (12 `seed/`, 18 `category/cat-`, 1 `default`) versionados en
      `app/src/main/assets/img/recipes/`, no ignorados por git, y el total ≤ 8 MB (Scenario 8).
- [ ] `RecipePhotoAssets` es **puro** (sin Android/Compose/I/O), **determinista** y con la cadena
      documentada: semilla → categoría → `default` → `null`.
- [ ] Un `categoryId` desconocido/`null` **no** produce candidato de categoría y cae a `default`
      (Scenario 4) y **nunca** se pinta un bloque vacío ni se lanza excepción.
- [ ] El test JVM itera la **Taxonomía real** y el **catálogo semilla real** y afirma que las 12
      semillas, las 18 categorías y `default` resuelven un asset existente y no vacío (**verificación
      exigida**, Scenario 5).
- [ ] `RecipeArtwork` conserva firma pública y geometría (16:10, 96x96, alto 96, hero 240), usa
      `ContentScale.Crop`, y sigue siendo el **único** componente de imagen en los 4 ficheros / 5 usos;
      **0** `PlateGlyph` y **0** cargas de assets fuera de `data/AssetRecipePhotoSource.kt` (Scenarios 6-7).
- [ ] Si falta el asset, se pinta el **degradado + glifo** actuales: `RecipeArtworkStyle` sin cambios y
      `RecipeArtworkStyleTest` sigue verde (Scenario 4).
- [ ] **Sin dependencias nuevas**: `gradle/libs.versions.toml` intacto y `app/build.gradle.kts` solo con
      la nueva `systemProperty`; sin Coil, sin red en tiempo de ejecución (Scenario 8).
- [ ] **Disciplina de alcance**: no cambian el dominio, la Taxonomía (`docs/domain/taxonomy.json`),
      `seed-recipes.json`, `domain.suggestions`, Room, `ui.theme/**`, `init.sh` ni `scripts/gradle.sh`;
      `git diff --stat` solo con lo previsto.
- [ ] Para UI: `Visual Design Impact` nombra las capturas existentes
      (`docs/design/concepts/recipe-photos/fotos-semillas/…` y `.../fotos-categorias/…`) y el acabado se
      revisa contra ellas y contra `DESIGN.md`.
- [ ] Un **ADR nuevo** (`docs/adr/0004-*.md`) documenta el cambio de decisión (fotos como assets) y las
      docs durables (`build-brief`, `technical-discovery`, `DESIGN.md`, `risks-and-open-questions`,
      `domain/taxonomy.md`) quedan coherentes (Scenario 10).
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Las fotos van en `assets/` y se versionan** (como `seed-recipes.json`): es lo único compatible con
  "offline, sin backend y sin red" (ADR 0001/0002) y con el gate de tests JVM.
- **Convención `seed/<recipeId>`, `category/cat-<categoryId>` y `default`**: replica las claves de las
  láminas (`README.md` v2) y permite que el respaldo sea mecánico; la raíz `img/recipes/` coincide con la
  intuición de `docs/domain/taxonomy.md` §6, que se actualiza a la convención real.
- **La cadena se resuelve con un predicado `exists` inyectado**: la misma función pura sirve al test
  (fichero en disco) y a la app (asset), sin duplicar reglas y sin I/O en la lógica.
- **El fallback al glifo no se toca**: `RecipeArtworkStyle` y el bloque de degradado quedan intactos, así
  que la feature es **aditiva** respecto de `recipe-artwork` y su test sigue siendo válido.
- **Seam por `CompositionLocal` con default nulo** (y no parámetro por 5 puntos de llamada, ni
  `LocalContext`): ver §4 de `Technical Approach`; es la opción de menor diff y la única que degrada
  "solo" si nadie provee el loader.
- **Prueba de extensiones en orden `webp, jpg, png`** en vez de fijar el formato: hace el código
  independiente de si la máquina del implementer tiene `cwebp`/ImageMagick, y permite migrar a WebP sin
  tocar Kotlin.
- **`ContentScale.Crop` con la geometría aceptada**: mantiene las capturas de layout de las features ya
  aceptadas; adoptar el 16:11 de la lámina obligaría a rehacer cuatro layouts aceptados (fuera de alcance).
- **Sin caché en disco y sin red en runtime**: las fotos son contenido empaquetado; la única caché es la
  LRU en memoria de 16 bitmaps decodificados.
- **El script de descarga no entra en `init.sh`**: el gate debe seguir siendo offline y no bloqueante.

### Unknowns

- **Tamaño y formato reales de las imágenes**: el README v2 dice 1376x768 nativas; las notas de
  `feature_list.json` (v1) decían 1200x896. La v2 manda. La extensión real (`webp`/`jpg`/`png`) solo se
  sabrá al descargar; el código la prueba, así que no bloquea. Si el total superara 8 MB sin
  herramienta de conversión, el implementer debe documentar cómo lo redujo.
- **Jank/memoria al hacer scroll**: decodificar en `Dispatchers.IO` con LRU mitiga el coste, pero un
  `LazyColumn` con muchas tarjetas puede notar la carga. El acabado y la fluidez los revisa la persona;
  si molesta, la salida es una feature correctiva (p. ej. cachear o añadir Coil entonces, con su ADR).
- **Fidelidad de las fotos a la receta**: la foto de la categoría es genérica por diseño (dos recetas IA
  de arroz pueden repetir la misma foto). Es una decisión aceptada (el `categoryId` es lo único que la
  IA devuelve con garantías), pero la persona puede considerarlo flojo visualmente → feature correctiva.
- **`docs/domain/taxonomy.md` §6** hablaba de `assets/icons/ingredients/{icon}.svg` (nunca empaquetados):
  esta feature solo corrige la línea de fotos; los iconos siguen sin assets (fuera de alcance).
- **Procedencia/licencia de las fotos**: son generadas con el prompt propio del repo
  (`stitch-prompt.md`) y la app es personal y no se distribuye (ya resuelto como no bloqueante en
  `docs/risks-and-open-questions.md`); queda registrado en el ADR 0004 por si cambia.
