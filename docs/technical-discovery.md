# Technical Discovery

## Product Surface

App **Android nativa**, orientación retrato, un único usuario local. Sin web, sin escritorio, sin
backend propio. El diseño de referencia está en `DESIGN.md`.

## Candidate Stack

- Kotlin + Jetpack Compose + Material 3.
- Android: **`minSdk` 33** (Android 13), **`targetSdk` 36** y **`compileSdk` 36**. Decisión de curso:
  no se soportan versiones anteriores a Android 13.
- Toolchain del bootstrap: Gradle 9.5.0 (wrapper), AGP 9.3.3, Kotlin 2.2.10, Compose BOM 2026.02.01.
- `applicationId` / paquete base: **`com.sazon.app`**. Barato de cambiar ahora; caro tras publicar.
- Room para persistencia local; DataStore para flags de aplicación (p. ej. "onboarding completado").
- KSP para procesar las anotaciones de Room. Con el Kotlin integrado de AGP 9, KSP necesita la
  excepción `android.disallowKotlinSourceSets=false` en `gradle.properties` para registrar sus fuentes
  generadas; sin ella, la configuración falla.
- Coroutines + Flow para asincronía.
- Cliente HTTP: **Ktor Client** (engine **OkHttp**) + `kotlinx.serialization` contra la API compatible
  con OpenAI de **DevExpert Inference** (proveedor decidido; ver `docs/adr/0003-ai-provider.md`). El
  version catalog fija **Ktor 3.6.0**; OkHttp queda **forzado a 5.4.0** en `app/build.gradle.kts`
  porque Ktor 3.6.0 arrastra 5.5.0, cuyo artefacto Android exige `compileSdk 37` (el repo compila
  contra API 36.1) y rompe `checkDebugAarMetadata`. Ver "Constraints".
- Inyección de dependencias ligera. En un proyecto de curso, manual o Koin es suficiente; Hilt añade
  ceremonia.
- Tests: JUnit en JVM. El dominio no debe depender de Android para poder testarse sin emulador.

## Data and Storage

| Qué | Dónde | Por qué |
|---|---|---|
| Taxonomía | `app/src/main/assets/taxonomy.json`, generado por la tarea `syncTaxonomy` desde `docs/domain/taxonomy.json`, y parseado a objetos tipados al arrancar | Fuente única editable y artefacto empaquetado, sin base de datos |
| Recetas semilla | **JSON fuente versionado** en `app/src/main/assets/seed-recipes.json`, cargado por la app y validado por test (decisión: riesgos, antes pregunta 7) | Contenido empaquetado, siempre disponible offline, editable sin recompilar |
| Perfil de usuario (identidad) | Room (1 fila, compartida con preferencias) | Nombre y color de avatar |
| Perfil de preferencias | Room (1 fila); el flag de onboarding en DataStore queda **diferido** (la puerta usa la presencia de Perfil, ver más abajo) | Estado mutable del usuario; fuente de la firma |
| Resumen de actividad | Contador `recipesGeneratedCount` en Room + nº de Favoritos observados | El contador se persiste; el conteo de favoritos se deriva |
| Favoritos | Room | Disponibilidad offline garantizada |
| Lote de sugerencias y recetas `ai` | Room, base propia `suggestion_batch.db` (`data.suggestions`); política y servicio puros en `domain.suggestions` | Evita llamadas repetidas a la IA y da soporte offline |
| Iconos e imágenes de receta | **Fotos de plato empaquetadas** como assets versionados en `app/src/main/assets/img/recipes/` (12 `seed/`, 18 `category/cat-`, 1 `default`), resueltas por `data.RecipePhotoAssets` y decodificadas por `data.AssetRecipePhotoSource`; el placeholder dibujado `ui.artwork.RecipeArtwork` (glifo por categoría, acento por cocina) queda como **último recurso** | Determinista, offline, sin red en runtime, sin dependencias nuevas |
| Imagen de plato **generada** (caché) | Ficheros **WebP en `cacheDir/dish-images`** cuyo nombre es la clave canónica (`<cacheKey>.webp`), más una **LRU de bytes en memoria** (16), resueltos por `data.image` (`DishImageResolver`); tope de disco de **80 MiB con evicción LRU** | Reutiliza sin volver a gastar cuota la imagen ya generada; el sistema puede descartar `cacheDir` en cualquier momento (nunca se usa `filesDir`) |
| Fotos semilla de **platos frecuentes** | **Assets versionados** en `app/src/main/assets/img/dishes/seed/<cacheKey>.<ext>` (6 fotos WebP de 960 px, ~439 KiB), resueltas por `data.DishImageSeedAssets` y leídas por `data.AssetDishImageSeedSource` | Es la **tercera capa** de la caché de imagen de plato (fase 7): esos platos muestran su foto propia **sin red y sin gastar cuota** (ni la primera vez), sin duplicar bytes en `cacheDir`; se preparan una sola vez con `scripts/prepare-dish-image-seed.sh` (fuera de `init.sh` y de runtime) |
| Clave de IA | `BuildConfig.DEVEXPERT_API_KEY`, desde `local.properties` (que no se versiona) | Decisión de demo; ver riesgos |

La Taxonomía **no** se persiste en base de datos: se empaqueta con la app y es idéntica entre
versiones. Los `id` nunca se renombran.

`docs/domain/taxonomy.json` es la única fuente editable. La tarea de Gradle `syncTaxonomy` la copia a
`app/src/main/assets/`, que está ignorado en git. El parseo vive en `com.sazon.app.domain.taxonomy`
(`TaxonomyJson.parse`, sin dependencias de Android) y la lectura del asset en `com.sazon.app.data`
(`AssetTaxonomySource`), de modo que los tests corren en JVM sin emulador. Ver
`docs/specs/taxonomy-in-code.md`.

El esquema de una receta vive en `com.sazon.app.domain.recipe` (`Recipe`, `RecipeIngredient`,
`RecipeStep`, `Nutrition`, `RecipeSource` y `MeasureUnit`), un único modelo serializable con
`kotlinx.serialization` para recetas `seed` y `ai`, también sin dependencias de Android. Ver
`docs/specs/recipe-domain-model.md`.

El Catálogo de recetas semilla vive en `app/src/main/assets/seed-recipes.json` (12 recetas, envelope
`{ "recipes": [...] }`). A diferencia de la Taxonomía, este JSON es **fuente versionada**, no generado:
no tiene gemelo en `docs/` ni tarea `sync`, y `.gitignore` no lo ignora. El parser puro vive en
`com.sazon.app.domain.recipe` (`SeedRecipeCatalog` + `SeedRecipeJson.parse`, sin Android) y la lectura
del asset en `com.sazon.app.data` (`AssetSeedRecipeSource`), mismo patrón que la Taxonomía. La
integridad y coherencia del contenido se verifican en JVM con `SeedRecipeCatalogTest` (referencias a la
Taxonomía, alérgenos, tiempos y dos casos negativos con JSON mutado) y `AssetSeedRecipeSourceTest`
(Robolectric) confirma que el asset se empaqueta y se carga por nombre. Ver
`docs/specs/seed-recipe-catalog.md`.

El Validador de recetas vive en `com.sazon.app.domain.validation` (`RecipeValidator`,
`RecipeConstraints`, `DiscardReason`, `DiscardedRecipe`, `RecipeValidation`, `ValidationOutcome` y el
helper interno `IngredientText`). Es lógica pura sin Android: consume el modelo de Receta y la
Taxonomía, parsea el envelope `{ "recipes": [...] }` elemento a elemento (sin lanzar ante una
respuesta mala) y aplica las reglas 1–4 de `docs/domain/taxonomy.md` §5 en orden, devolviendo las
recetas aceptadas y el motivo de cada descarte (no registra log ni persiste: eso es del llamador).
Testeado en JVM (`RecipeValidatorTest`). Ver `docs/specs/recipe-validator.md`.

La persistencia del Perfil de usuario (identidad + preferencias) vive en
`com.sazon.app.data.profile` sobre Room 2.8.5: una sola fila (`user_profile`) con `INSERT OR REPLACE`
y una columna JSON por lista (conversor con `kotlinx.serialization`). El dominio puro
`com.sazon.app.domain.profile` define `UserProfile`, `PreferenceProfile`, `ProfileName` y el puerto
`ProfileRepository` (`get`/`observe`/`save`/`clear`). El test corre en JVM con Robolectric y base de
datos en memoria, sin emulador. Ver `docs/specs/profile-persistence.md`.

La Firma de preferencias vive en `com.sazon.app.domain.profile.PreferenceSignature.of(preferences)`:
un hash SHA-256 determinista sobre una cadena canonica (listas tratadas como conjuntos) que **excluye**
la identidad (nombre y color de avatar) y `updatedAt`. Logica pura sin Android, testeada en JVM. El
Lote de sugerencias la **consume** (`SuggestionBatchService`) y la guarda como metadato de la cache
(su clave), sin almacen separado de firmas. Ver `docs/specs/preference-signature.md`.

El Lote de sugerencias vive en `com.sazon.app.domain.suggestions` como logica pura
(`SuggestionBatch`, `BatchRefreshReason`, `BatchDecision`, `SuggestionBatchPolicy` con
`BATCH_TTL_MILLIS` de 7 dias en un unico sitio, el puerto `SuggestionBatchRepository` y
`SuggestionBatchService.ensureBatch`). El servicio es **cache-first**: consume
`PreferenceSignature.of(profile)` y solo llama al Generador cuando la politica decide
`MISSING`/`SIGNATURE_CHANGED`/`EXPIRED`/`FORCED`; filtra las recetas `ai` con `RecipeValidator`,
deduplica por titulo normalizado (gana la `seed`) y, si el Generador falla, degrada a un lote
seed-only persistido (modo esperado, sin tragarse `CancellationException`). La persistencia esta en
`com.sazon.app.data.suggestions` sobre Room, en una base **propia** (`suggestion_batch.db`, version 1,
una fila con las recetas como columna JSON via `RecipeJson.encodeList`/`parseList`) para no migrar la
base aceptada del Perfil. Testeado en JVM (`SuggestionBatchPolicyTest`, `SuggestionBatchServiceTest` y
`RoomSuggestionBatchRepositoryTest`). La composicion **si** ocurre ya en la app: la realiza
`home-offline-blocks`. Ver `docs/specs/suggestion-batch-cache.md`.

La **Home real y el armazon principal** viven en `com.sazon.app.ui`: `MainShell` es un `Scaffold` con
la **barra de navegacion inferior** de tres destinos (Inicio · Favoritos · Perfil, `MainTab`) y el
**FAB** "＋ Crear receta", y enruta el contenido por pestana (`ui.home.HomeScreen` para Inicio,
`ui.favorites.FavoritesScreen` para Favoritos, `ui.profile.ProfileScreen` para Perfil y
`ui.generator.GeneratorFlow` en el Generador empujado). `HomeScreen` pinta los cuatro bloques **locales**
de `DESIGN.md` (el bloque 2, Búsqueda con IA, es de `ai-search`): cabecera con saludo por franja
horaria y avatar, tarjeta-heroe al Generador, carrusel "Sugeridas para ti" y bloque "Tus recetas" con su
estado vacio. La logica/estado de la Home es **pura** y testeable en JVM en `com.sazon.app.ui.home`
(`HomeGreeting`, `HomeIdentity`, `HomeSuggestions`, `HomeRecipes`, `RecipeCardFormat`) y en
`com.sazon.app.ui.navigation` (`MainTab`, `PushedScreen`, `MainNavigationState`, navegacion sin
libreria: el `Saver` de `rememberSaveable` vive en la capa Compose). `MainActivity` compone a mano
(sin DI) `SuggestionBatchDatabase` + `RoomSuggestionBatchRepository` + `SuggestionBatchService`
(`FakeRecipeGenerator`, sin red) + la Taxonomia y el catalogo semilla; `SazonApp` observa el
`UserProfile` completo y monta `MainShell`. `MainShell` **consume el Lote tal cual**:
`SuggestionBatchRepository.observe()` alimenta el carrusel y `SuggestionBatchService.ensureBatch` se
llama una vez al entrar (politica cache-first ya aceptada); el usuario tambien puede **forzarlo** desde
la seccion "Sugerencias" del Perfil (`ensureBatch(..., force = true)`), que reusa el mismo Lote
observado. Mientras el Lote no se resuelve, la Home pinta las recetas semilla empaquetadas
(`HomeSuggestions.visible`) y nunca muestra una pantalla de carga. La Home es **offline**: solo la futura Búsqueda con IA necesita red. Ver
`docs/specs/home-offline-blocks.md`.

Los **Favoritos** viven en `com.sazon.app.domain.favorites` (logica pura: `Favorite`, que persiste una
**copia** de la `Recipe` mas su `savedAt`, el puerto `FavoriteRepository` y `FavoriteService.toggle`,
que marca/desmarca con reloj inyectable y normaliza `matchScore` a `null` por ser derivado y no
persistido), en `com.sazon.app.data.favorites` su persistencia Room sobre una base **propia**
(`favorites.db`, version 1, tabla **multi-fila** `favorite_recipe` con la receta como columna JSON via
`RecipeJson`, `upsert` idempotente por `recipeId` y lectura ordenada por `savedAt` desc) y en
`com.sazon.app.ui.favorites` su presentacion: la logica pura `FavoritesSort`/`FavoritesViewMode`/
`FavoritesList.visible` (busqueda por titulo normalizada sin acentos y tres ordenes: recientes, tiempo
y dificultad) testeada en JVM, y la capa Compose delgada `FavoritesScreen` + `FavoriteRecipeCard`
(lista/rejilla, chips de orden, nota "Disponibles sin conexion", estado vacio con "Explorar recetas" y
swipe "Eliminar"). La integracion: `MainActivity` compone `FavoritesDatabase`/repositorio/servicio,
`MainShell` observa los favoritos para la pestana, alimenta `HomeRecipes.saved` (bloque "Guardadas" de
la Home) y cablea `isSaved`/`onToggleSave` del Detalle, y `ui/SectionPlaceholder.kt` se elimina al
quedarse sin usos. El Favorito es **independiente del Lote**: guarda su propia copia y sobrevive a su
refresco. Ver `docs/specs/favorites.md`.

El **filtrado local de "Tus recetas"** vive en `com.sazon.app.ui.filters`: `LocalResultFilter` (seleccion
**unica**: `All`, `Category(categoryId)`, `Fast` y `Vegetarian`, cada una con una `key` estable para
`rememberSaveable`) y `LocalResultFilters`, un `object` **puro** (sin Compose/Android, testeado en JVM)
con `visible(recipes, filter, taxonomy)`, `vegetarianTags(taxonomy)`, `categoriesIn(recipes, taxonomy)`,
`label(...)`, `isDerived(...)` y `fromKey(...)`. "Rápido" y "Vegetariano" son filtros **derivados**, no
categorias: "Rápido" selecciona `totalMinutes <= FAST_MAX_MINUTES` (30) y "Vegetariano" excluye recetas
con algun ingrediente resuelto cuyos tags esten en `dietPatterns[vegetarian].excludedTags`
(`meat`/`poultry`/`fish`/`seafood`, con respaldo constante defensivo); la deteccion es por **tags, no
por keywords**, asi que un ingrediente libre no se excluye. `ResultFilterRow` es la fila de chips
(`FilterChip` de Material 3, sin dependencias nuevas) con "Todas" primero, una chip por categoria
**presente** en las recetas (orden de `taxonomy.recipeCategories`), y "Rápido"/"Vegetariano" agrupados
al final y con el acento oliva (`secondary`) cuando estan seleccionados, para distinguirlos de las
categorias (terracota/`primary`). Se aplica al bloque "Tus recetas": `HomeRecipes` conserva un unico
grupo (`saved`, alimentado por los Favoritos) y `HomeRecipes.filteredBy(filter, taxonomy)` lo acota;
`HomeScreen` guarda la seleccion con `rememberSaveable` + `Saver` (clave/`fromKey`), pinta la fila y el
estado "sin resultados para este filtro", y **solo pinta los grupos con contenido** (un grupo sin
recetas no pinta ni titulo ni carrusel); si el bloque llega **vacio** solo pinta la linea de usuario
nuevo (sin chips). La logica es **reutilizable** por `favorites`. Ver `docs/specs/local-result-filters.md`.

El **paso de ingredientes del Generador** vive en `com.sazon.app.ui.generator`. La logica/estado es
**pura** y testeable en JVM: `GeneratorIngredient` (ingrediente anadido: de la Taxonomia si
`ingredientId != null`, **libre** si `null`), `GeneratorIngredientsState` (`typeQuery`, `toggleTaxonomy`,
`addFree`, `remove`, `count`, `canContinue` y `countLabel` con singular/plural) y `GeneratorStep`
(`INGREDIENTS` "¿Qué tienes?" y `SETTINGS` "Ajusta tu receta", con `next`/`previous`/`isLast`). Las
reglas: dedupe de Taxonomia por `ingredientId` (el segundo toque quita), dedupe de libres por **nombre
normalizado** (`trim` + minusculas + sin acentos) tambien contra los ya anadidos, y un texto en blanco
como **no-op**; `IngredientsFilter` (ya en `ui.preferences`) se reutiliza sin cambios para la nube de
sugeridos, que sale siempre de `taxonomy.ingredients` (la UI no diverge de la Taxonomia). La capa Compose
es `GeneratorFlow` (armazon del asistente dentro de `PushedScreen.GENERATOR`: barra superior con "Atras"
y el titulo del paso, primario anclado y `Saver` de `rememberSaveable` para el paso y el borrador, que
asi se conserva al ir y volver del paso 2) y `GeneratorIngredientsScreen` (input libre con "Añadir",
chips de los anadidos con "×", contador con cifras tabulares y nube **colapsable** "Ingredientes
sugeridos", sin iconos: "×" y "Atras" son texto). No se persiste el borrador en disco (asistente
efimero). `MainShell` monta `GeneratorFlow` en el destino `GENERATOR` en lugar del `SectionPlaceholder`
(ya eliminado al sustituir el ultimo destino). Ver `docs/specs/generator-ingredients-step.md`.

El **paso de ajustes del Generador** ("Ajusta tu receta") vive en el mismo `com.sazon.app.ui.generator`.
La logica/estado es **pura** y testeable en JVM: `GeneratorSettingsState` (`servings`, `mealTypeId`
opcional, `timeBucketId`, `difficultyId`, `hardRestrictionIds` y `notes`), con `initial(preferences,
limits)` que **precarga** comensales (`defaultServings` recortado a `taxonomy.servings`), franja,
dificultad y alergias del Perfil (el tipo de comida arranca en `null` porque el Perfil no lo tiene),
`setServings` (recorta con `coerceIn`), `selectMealType` (single-select **opcional**: el segundo toque lo
deja en `null`), `selectTimeBucket`/`selectDifficulty` (single-select que reemplazan),
`toggleHardRestriction` (conjunto, sin duplicados), `clearHardRestrictions`/`hasNoRestrictions` y
`typeNotes`. La capa Compose es `GeneratorSettingsScreen` (seis tarjetas `Surface` con
`SazonShapes.large`): "Tiempo disponible" (`TimeBucketEditor`), "Tipo de comida" (fila local de
`FilterChip` de `taxonomy.mealTypes`, incluido Postre, sin editor previo), "Comensales"
(`ServingsStepper`), "Dificultad" (`DifficultyEditor`), "Alergias a evitar" (`HardRestrictionsEditor` en
**ambar** y nunca en rojo, con la caption "Ninguna declarada" y el `TextButton` "Editar en mi perfil") y
"¿Algo mas que deba saber?" (`OutlinedTextField` multilinea). `GeneratorFlow` recibe ahora el Perfil y el
callback de Perfil (`onEditProfile`) y guarda el borrador de ajustes con `rememberSaveable` +
`GeneratorSettingsStateSaver`; `MainShell` pasa `profile.preferences` y
`onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) }` (el Perfil real es de
`profile-screen`). El primario "Generar recetas" no tiene efecto: la generacion es de
`generation-flow-with-validation`; el estado **no** se convierte a `GenerationConstraints`. Ver
`docs/specs/generator-settings-step.md`.

La **generacion validada del Generador** (el corte vertical principal) vive en
`com.sazon.app.domain.generation` (`GenerationResult` y `GenerationService`). El servicio es logica pura
(sin Android ni red): construye las `RecipeConstraints` con el `profile` **efectivo** que viaja en las
`GenerationConstraints`, llama a `RecipeGenerator.generateFromIngredients`, valida **receta a receta**
con `RecipeValidator.validateRecipe` (no usa `validateResponse`: el puerto ya devuelve `List<Recipe>`),
**re-lanza** la `CancellationException` y convierte cualquier otra `Exception` en `aiUnavailable = true`
con listas vacias, sin propagar (modo degradado). El mapeo borrador -> dominio es
`com.sazon.app.ui.generator.GeneratorRequestFactory` (puro): los ingredientes se mapean con
`quantity = 0.0`, `unit = null` e `isPantry = false` conservando `name`/`ingredientId`, y `constraints`
copia el Perfil **sustituyendo** sus `hardRestrictionIds` por las del paso de ajustes (y unas notas en
blanco pasan a `null`), de modo que el validador (y el prompt del proveedor real) ven las alergias **a
evitar ahora**. El microcopy del estado de carga es `GeneratorProgressMessages` (puro: cabecera fija y
sublineas derivadas de la Taxonomia y los ajustes). La capa Compose es delgada:
`GeneratorGeneratingScreen` (animacion terracota dibujada con `Canvas`, titular Manrope, sublineas
rotatorias y "Cancelar" secundario) y `ResultsScreen` (la pantalla de Resultados real; sustituye al seam
`GeneratorResultsPreview`, ya eliminado). `GeneratorFlow` guarda el resultado y
el token de generacion en `remember` (no `rememberSaveable`, `Recipe` no es serializable trivialmente),
dispara la generacion al pulsar "Generar recetas" en el paso de ajustes, respeta `MIN_VISIBLE_MILLIS`
(~900 ms) y cancela poniendo el token a `null` (la cancelacion se propaga por el servicio).
`MainActivity` compone `GenerationService(generator = FakeRecipeGenerator(), taxonomy = taxonomy)` (sin
DI) y lo encadena por `SazonApp` y `MainShell` hasta `GeneratorFlow`. Testeado en JVM
(`GenerationServiceTest`, `GeneratorRequestFactoryTest` y `GeneratorProgressMessagesTest`), incluido el
caso negativo obligatorio del `build-brief`: una receta insegura no llega a `accepted`. Ver
`docs/specs/generation-flow-with-validation.md`.

La **imagen de receta** (compartida por Home, Resultados, Favoritos y Detalle) combina **fotos
empaquetadas** y el **placeholder dibujado** de `recipe-artwork`. La resolucion de la foto es **pura** y
testeable en JVM (`com.sazon.app.data.RecipePhotoAssets`, sin Compose ni Android: `ROOT = img/recipes`,
`EXTENSIONS = webp|jpg|png`, `CATEGORY_IDS` con las 18 `recipeCategories` reales, `candidates` y
`resolve(recipeId, categoryId, exists)`), con la **cadena de respaldo determinista** foto propia de la
receta semilla (`seed/<id>.<ext>`) -> foto de su categoria (`category/cat-<categoryId>.<ext>`) -> foto
generica (`default.<ext>`) -> `null` (glifo). Un `categoryId` nulo/vacio/desconocido **no** produce
candidato de categoria (nunca `cat-no-existe`) y cae al `default`; la resolucion es determinista y no
lanza. La lectura/decodificacion es una **costura Android** en `com.sazon.app.data.AssetRecipePhotoSource`
(`context.assets.open` + `BitmapFactory` en `Dispatchers.IO`, `LRU` propia de 16 bitmaps, `null` sin
excepcion) y devuelve `android.graphics.Bitmap`, asi que `data` sigue sin depender de `ui`. El seam
Compose es `ui.artwork.RecipePhotoSource` (`fun interface` + `LocalRecipePhotoSource`, cuyo valor por
defecto devuelve `null`), y `MainActivity` lo provee con `CompositionLocalProvider`; el mapeo puro
`RecipeArtworkStyle` (sin Compose ni Android: `RecipeGlyph` con 18 miembros ↔ 18 `recipeCategories`,
`RecipeAccent` con 10 acentos + `DEFAULT` ↔ 10 `cuisines`, `RecipeStyle` y el fallback total
`null`/vacio/desconocido -> `PLATE` + `DEFAULT`) **no cambia** y sigue siendo el **ultimo recurso**. La
capa Compose es el unico componente delgado, `RecipeArtwork(recipe, modifier, glyphSize)`: pinta la foto
con `Image(ContentScale.Crop)` y, si no hay, el bloque con degradado por **roles del tema**
(`primaryContainer`, `secondaryContainer`, `tertiaryContainer`, `surfaceVariant`) y el glifo de linea con
primitivas de `Canvas`. El llamante aporta el ratio/tamaño/recorte, asi que se conservan el 16:10 de las
tarjetas (glifo 48 en `RecipeCard`, 56 en `ResultRecipeCard`), el 96x96 y el alto 96 de Favoritos (40) y
el hero de 240 del Detalle (72). Los 31 assets viven versionados (no ignorados) en
`app/src/main/assets/img/recipes/` (≤8 MB) y los empaqueta `scripts/fetch-recipe-photos.sh`, una
herramienta de una sola vez **fuera** del gate. Ver `docs/specs/recipe-photos.md`,
`docs/adr/0004-recipe-photos-as-assets.md` y `docs/specs/recipe-artwork.md`.

La **pantalla de Resultados** vive en `com.sazon.app.ui.results`, con logica **pura** testeable en JVM
(sin Compose ni Android) y una capa Compose delgada. `MatchScore.of(recipe, preferences, taxonomy,
userIngredients)` fija la formula **derivada** de `matchScore`: media ponderada en `0..100` de la
cobertura de los ingredientes del usuario (`W_COVERAGE`), los ingredientes preferidos resueltos
(`W_LIKED`), la cocina favorita (`W_CUISINE`), la franja de tiempo (`W_TIME`) y la dificultad
(`W_DIFFICULTY`), menos `DISLIKE_PENALTY` por cada ingrediente evitado presente (penaliza, nunca
descarta); es determinista y **no persistida** (se materializa en memoria con
`recipe.copy(matchScore = ...)`). `ResultsStatus.from(...)` decide el estado (`Content` con las aceptadas
puntuadas y ordenadas por relevancia, `Empty`, o `Unavailable` con las semillas locales) y
`ResultsHeaders.of(...)` formatea titulo/subtitulo (Generador o Busqueda con IA, la consulta entre
comillas). `ResultsScreen` monta el `Scaffold` (topBar con "Atras" + titulo/subtitulo, bottomBar "Cambiar
ingredientes", y los estados poblado/vacio/degradado con "Reintentar") y `ResultRecipeCard` la tarjeta a
ancho completo (imagen de receta `RecipeArtwork`, meta y pildora de coincidencia, sin corazon ni click).
La pantalla se
monta como *stage* de resultado dentro de `GeneratorFlow` (sin `PushedScreen.RESULTS`) para conservar los
borradores; `ai-search` reutilizara `ResultsScreen` con `ResultsOrigin.Search`. Ver
`docs/specs/results-screen.md`.

La **Busqueda con IA** (bloque 2 de la Home) vive en `com.sazon.app.domain.search` +
`com.sazon.app.ui.search`. La orquestacion es **pura** (sin Android ni red):
`AiSearchService.search(query, profile)` normaliza la consulta con `SearchMemo.key` (recorte +
minusculas), sirve el **memo de sesion** (`SearchMemo`, en memoria y **no** persistido) cuando la clave
ya existe, llama a `RecipeGenerator.searchByText` y valida **receta a receta** con
`RecipeValidator.validateRecipe` (patron de `GenerationService`); **re-lanza** la `CancellationException`,
convierte cualquier otra `Exception` en `aiUnavailable = true` **sin memoizar el fallo** (asi "Reintentar"
vuelve a llamar) y reporta las aceptadas por la costura `onRecipesGenerated` (Resumen de actividad). El
estado de la pagina es puro en `com.sazon.app.ui.search`: `AiSearchQuery` (`normalize`/`canSubmit`),
`AiSearchCopy` (microcopy en un unico sitio), `AiSearchProgress` (cabecera + tres sublineas + linea de
prompt) y `AiSearchOutcome.stageOf` (decide `Results`/`Blocked`/`NoResults`/`Failure`; el **bloqueo por
Perfil** se distingue de "sin resultados" por el **motivo del descarte** `DIET_PATTERN`/`HARD_RESTRICTION`,
no por la consulta). La capa Compose es delgada: `AiSearchBar` (pildora con glifo de destello y envio,
integrada en `HomeScreen` entre la cabecera y la tarjeta-heroe), `AiSearchFlow` (huesped de
`PushedScreen.SEARCH`: carga reutilizando `GeneratorGeneratingScreen` con `cancelLabel = "Cancelar
busqueda"`, resultados reutilizando `ResultsScreen` con `ResultsOrigin.Search`, y los estados de
`AiSearchStates`) y `AiSearchStates` (bloqueado en ambar `tertiaryContainer`, nunca rojo; sin resultados;
fallo). La unica dependencia externa es la **conectividad**: `com.sazon.app.data.NetworkStatusSource`
(sobre `ConnectivityManager`, con `NET_CAPABILITY_INTERNET` **y** `NET_CAPABILITY_VALIDATED`) alimenta
`HomeScreen`/`AiSearchBar`; exige el permiso `ACCESS_NETWORK_STATE` en `AndroidManifest.xml` y no anade
dependencias. `MainActivity` compone `AiSearchService` y `NetworkStatusSource` (sin DI), `SazonApp` los
encadena y `MainShell` guarda la consulta en `rememberSaveable` (sobrevive a empujar/sacar la pagina).
Testeado en JVM (`AiSearchServiceTest`, `SearchMemoTest`, `AiSearchQueryTest`, `AiSearchProgressTest` y
`AiSearchOutcomeTest`). Ver `docs/specs/ai-search.md`.

El **Detalle de receta** vive en `com.sazon.app.ui.detail`, con logica **pura** testeable en JVM (sin
Compose ni Android) y una capa Compose delgada. `ServingScaler.scaledIngredients(recipe, targetServings)`
escala las cantidades con **redondeo por unidad** (no lineal): `g`/`ml` a multiplos de 5, `ud`/`cda`/`cdta`
a medios (half-up, minimo el paso si `quantity > 0`), `unit = null` ("al gusto") **no se escala** y
`factor == 1` devuelve las cantidades originales intactas (sin division por cero con `recipe.servings = 0`).
`IngredientFormat` resuelve las etiquetas (`"½"`, `"4½ ud"`, `"300 g"`, `"Al gusto"`), `RecipeDetailFormat`
los cuatro pares del hero (Tiempo/Dificultad/Comensales/Calorias, sin `matchScore`) y `RecipeDetailState`
los comensales (acotados a `taxonomy.servings`) y la pestana activa. `RecipeDetailScreen` es la capa
Compose: hero con la imagen de receta (`ui.artwork.RecipeArtwork`, 240 dp) + "Atras" y corazon, tarjeta
superpuesta con la meta, pestanas
Ingredientes/Pasos/Notas (timeline con pildora ambar de duracion y nutricion opcional), el `ServingsStepper`
reutilizado **sin cambios** y barra inferior ("Guardar"/"Empezar a cocinar": "Guardar" sigue siendo la
costura no-op de `favorites` y "Empezar a cocinar" ya se cablea desde `MainShell` al modo cocina). El
detalle se hospeda como **overlay** en `MainShell`
(`var detailRecipe by remember`), de modo que se pinta **encima** de la Home o del Generador **sin
desmontarlos** (se conservan el resultado y el borrador); abre desde las tarjetas de Resultados y de la
Home. Ver `docs/specs/recipe-detail-screen.md`.

El **Modo cocina** (ampliacion del MVP, issue #76) vive en `com.sazon.app.ui.cooking`, con logica
**pura** testeable en JVM (sin Compose ni Android) y capa Compose delgada. `CookingTimerState` es la
maquina de estados del **temporizador por paso** (`IDLE`/`RUNNING`/`PAUSED`/`FINISHED`): `of(minutes)`
fija `total`/`remaining` en Inactivo (`minutes <= 0` deja `hasDuration = false` y la UI oculta el bloque
entero), y `start`/`pause`/`resume`/`reset`/`tick(deltaMillis)` son transiciones inmutables y
**deterministas**: el tiempo solo entra por `tick`, que descuenta unicamente en Marcha, acota a cero y
pasa a Terminado sin seguir contando (las transiciones invalidas son no-op). `CookingModeState` guarda
el paso actual (`stepIndex`/`totalSteps`), los pasos hechos (`completedOrders`, por `RecipeStep.order`)
y el temporizador; **no** guarda la `Recipe` (para el `Saver`), y `next`/`previous` reciben la
`List<RecipeStep>`, acotan a `0..totalSteps-1` y **reinan el temporizador al del paso nuevo** mientras
el marcado persiste. `CookingModeFormat` formatea el progreso (`"Paso 2 de 5"` + la fraccion de la
barra, `0f` sin pasos), la duracion (`"10 min"`) y la cuenta atras `mm:ss` en `Locale.ROOT`.
`CookingModeScreen` es la capa Compose (cabecera con "Atras" circular y barra fina terracota, tarjeta
del paso con circulo numerado y tachado/atenuado `0.45` si esta hecho, bloque miel del temporizador con
sus cuatro estados, aviso **en pantalla** "¡Listo!" en miel/oliva — nunca `error` —, fila "Marcar paso
como hecho" y barra inferior "Anterior" + "Siguiente"→"Terminar", con `CookingModeStateSaver` y un
`LaunchedEffect` que solo alimenta `tick(1000)`). Se hospeda como **overlay** en `MainShell`
(`var cookingRecipe by remember`) **encima** del Detalle y cablea la costura `onStartCooking` que
`recipe-detail-screen` dejo no-op. Sin notificacion de sistema, sonido, vibracion ni persistencia
(Non-Goals del MVP). Ver `docs/specs/cooking-mode.md`.

La pestana **Perfil** vive en `com.sazon.app.ui.profile`. El estado de edicion es **puro** y testeable
en JVM (`ProfileEditState`): identidad (`displayName`, `avatarColorId`) + `OnboardingPreferencesDraft`,
con `from(profile, taxonomy)` (normaliza el nombre y degrada un `avatarColorId` corrupto al primer
`avatarColors`), las copias inmutables `withDisplayName`/`withAvatarColorId`/`withPreferencesDraft`,
`isNameValid` y `toProfile(base, now)`. Materializa la regla "un editor, dos entradas": cada grupo de
preferencias abre una **hoja inferior** (`ModalBottomSheet` de Material 3, `ProfileEditorSheet` con
`ProfileSection`) que envuelve **sin cambios** los editores sin estado de `ui.preferences`
(`DietPatternEditor`, `SoftPreferencesEditor`, `CuisinesEditor`, `IngredientsEditor`,
`HardRestrictionsEditor`, `DifficultyEditor`, `TimeBucketEditor`, `ServingsStepper`);
`ProfileIdentityEditor` reutiliza `AvatarPreview` y las ocho muestras de `avatarColors`, y las filas de
resumen (`ProfileSectionRow`) resuelven labels de la Taxonomia con respaldo al `id`. La garantia clave:
`toProfile` **conserva** `base.preferences` tal cual (mismo `updatedAt`) si el borrador no cambio, de
modo que editar solo la identidad **no** cambia la Firma ni invalida el Lote; cambiar una preferencia
reconstruye el `PreferenceProfile` con `updatedAt = now` y si cambia la firma, y `createdAt` se
conserva. `OnboardingPreferencesDraft` se amplia **aditivo** con `from(preferences)` y
`toPreferences(updatedAt)`: el **mismo** motor de seleccion que el onboarding, sin divergencia. El
guardado usa `ProfileRepository.save` por observacion: `MainShell` recibe `onSaveProfile` y `SazonApp`
lo cablea con un `rememberCoroutineScope`, de modo que el armazon se recompone. La seccion
**"Sugerencias"** (`ProfileSuggestionsSection`) se pinta tras la cabecera de identidad y antes de "Mis
preferencias": muestra la antiguedad del Lote observado y ofrece el refresco manual. Su formateo de
antiguedad (`SuggestionsRefreshFormat`, puro, sin Compose/Android) deriva "hoy"/"hace N dias" de
`SuggestionBatch.generatedAt`; `MainShell` pasa `batch?.generatedAt`, un estado de UI "en vuelo" (que
deshabilita el boton y evita dobles disparos) y un callback que llama a
`SuggestionBatchService.ensureBatch(profile.preferences, force = true)` **sin** tocar
`ProfileRepository` (el refresco no cambia ninguna preferencia). Los ajustes y "Borrar mis datos"
quedan fuera de alcance (features propias).
Ver `docs/specs/profile-screen.md` y `docs/specs/manual-suggestions-refresh.md`.

El **Resumen de actividad** del Perfil vive en `com.sazon.app.domain.activity`
(`ActivitySummaryRepository` como puerto y `ActivitySummaryService`, logica pura sin Android) y en
`com.sazon.app.data.activity` su persistencia Room sobre una base **propia**
(`activity_summary.db`, version 1, una sola fila `activity_summary` con `recipesGeneratedCount`).
El servicio expone `observeRecipesGeneratedCount`/`recipesGeneratedCount` y la **unica** regla
"solo `ai`, solo suma": `recordAcceptedRecipes(recipes)` cuenta `recipes.count { source == AI }`, no
escribe si el delta es `0` y devuelve el valor persistido; como se cuenta cada receta `ai` aceptada
por el `RecipeValidator` (no recetas distintas), el contador **nunca baja** aunque el Lote reemplace
sus `ai` al refrescarse (la fila ausente se lee/emite como `0` y el incremento es atomico
`INSERT OR IGNORE` + `UPDATE ... + delta` en un `@Transaction`). El reporte es una costura **aditiva**
con default no-op (`onRecipesGenerated: suspend (List<Recipe>) -> Unit = {}`) colocada **antes** del
reloj inyectable en `SuggestionBatchService` (para no tragarse la lambda de cola `{ now }` de los
llamadores existentes): se invoca solo en la rama de generacion, tras validar, y la rama `UseCached`
**no** lo dispara; `GenerationService` recibe el mismo callback tras validar y `MainActivity` cablea
ambos a `ActivitySummaryService.recordAcceptedRecipes`. El numero de **favoritas** no se persiste:
`MainShell` lo **deriva** de la lista observada (`favorites.size`). La seccion "Resumen"
(`ActivitySummarySection`, dos tarjetas de igual ancho con cifras tabulares `"tnum"` y tokens del
tema, sin iconos) se pinta **entre la cabecera de identidad y "Sugerencias"**. Testeado en JVM
(`ActivitySummaryServiceTest` y `SuggestionBatchActivityRecordingTest`) y con Robolectric
(`RoomActivitySummaryRepositoryTest`). Ver `docs/specs/activity-summary.md`.

El Constructor de prompt vive en `com.sazon.app.domain.prompt.PromptBuilder.build(taxonomy, request)`:
traduce el Perfil de preferencias y los ajustes opcionales a un `BuiltPrompt(system, user)` que exige
el contrato de `docs/domain/taxonomy.md` seccion 4 con `id` de la Taxonomia y garantiza que el patron
de dieta y las restricciones duras aparecen **siempre**; pide 6 recetas por generacion
(`RECIPES_PER_GENERATION`). Es logica pura sin Android, red, persistencia ni coroutines, y no incluye
la Firma de preferencias (es clave de cache del Lote, no contenido del prompt). Testeado en JVM
(`PromptBuilderTest`). La implementacion real del proveedor lo consumira. Ver
`docs/specs/prompt-builder.md`.

El **proveedor de IA real** vive en `com.sazon.app.data.ai` y es la **unica** capa que conoce Ktor
(junto al cableado de `MainActivity`). `DevExpertRecipeGenerator` implementa el puerto
`RecipeGenerator`: las tres operaciones componen su prompt con `PromptBuilder` y llaman al mismo
`execute`, que aplica la politica fija de `AiProviderPolicy` en un **unico** sitio (connect 10 s,
socket 120 s, timeout por intento 150 s, deadline total 240 s; maximo **1 reintento** ante
timeout/`IOException`/408/429/5xx con backoff fijo de 500 ms y `Retry-After` solo si <= 5 s; nunca
400/401/403/404/422; el timeout de cada intento se **recorta** a `min(150 s, restante)`), cierra la
pregunta 1 de `docs/risks-and-open-questions.md` y traduce los fallos a `AiUnavailableException`
tipada, que los llamadores aceptados ya entienden como **modo semilla**. Los timeouts estan
calibrados contra la **peor latencia real medida** de la misma clave y el mismo prompt de 6 recetas
(issue #72): `mimo-v2.6-pro` **115,8 s**, con el alias diario `chat` entre 28,0 y 46,6 s y
`chat`+`json_object` / `chat-pro`+`json_schema` / `deepseek-v4-flash` en 85,3 / 97,0 / 52,6 s; el
socket cubre el peor caso con margen y el deadline deja sitio a otro intento igualmente lento tras el
backoff. Cada peticion envia ademas `response_format: {"type":"json_object"}` (correctiva
`ai-response-format`, issue #74) para pedir **forma** JSON parseable, sin usar
`json_schema`/structured outputs forzados: si el proveedor responde **400/422** con el formato activo
se reintenta **una sola vez** sin la clave (fallback one-shot, sin backoff, consumiendo el 2.º
intento), **solo si queda presupuesto de intentos**; el `400`/`422` del 2.º intento degrada a
`HTTP_ERROR` sin una 3.ª petición (`MAX_ATTEMPTS = 2`), y un interruptor,
`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`, permite apagar la feature por
completo (entonces la peticion **omite** la clave; nunca emite `response_format: null`). La KDoc de
`OpenAiChat` documenta el porque de `@EncodeDefault(NEVER)` sobre el campo nullable (`AiHttpClientFactory`
usa `encodeDefaults = true`). El fallo de transporte se clasifica por su causa en
`AiProviderPolicy.transportFailureReason`: timeout de socket (`java.net.SocketTimeoutException`),
timeout por intento (`HttpRequestTimeoutException`) o `InterruptedIOException` -> `TIMEOUT`; el resto
de `IOException` (DNS, conexion rechazada/reset; el `ConnectTimeoutException` de Ktor no es un timeout
del modelo) -> `NETWORK`. La clave ausente lanza
`MISSING_KEY` **sin** ninguna peticion; un 200 ilegible o vacio **no** lanza (devuelve las recetas
decodificables, posiblemente vacias via `AiRecipePayload.decode`, que mapea el **wire del contrato de
la IA** (`docs/domain/taxonomy.md` §4, sin `id` ni `source`) a Receta con el DTO `AiRecipeDraft`,
**completa `id`** (`AiRecipeIds`: `ai-` + SHA-256 hex del contenido, determinista y sin
`Random`/reloj/`UUID`) y `source = ai` **antes** de validar, y aplica las reglas del Validador con
`RecipeValidator.validateRecipe` y restricciones **neutras** -sin pre-filtrar por dieta/restricciones,
asi el estado Bloqueado de `ai-search` sigue siendo alcanzable-, elemento a elemento y sin lanzar);
los ingredientes del wire se decodifican con el DTO `AiIngredientDraft` de `quantity` **anulable**: una
`quantity` nula o ausente es un ingrediente **"al gusto"** y se normaliza al mapear a `RecipeIngredient`
a `quantity = 0.0` + `unit = null` (la `unit` del wire se descarta, porque sin cantidad no es
interpretable), de modo que un unico ingrediente "al gusto" **no** descarta la receta entera (correctiva
`ai-quantity-tolerance`); la canonicalizacion del `id` usa el ingrediente **ya normalizado** (las
variantes nula/ausente/`0.0`+`null` comparten `id` y los ids ya existentes no cambian);
el decode devuelve un **resultado tipado** `AiDecodeResult` (`accepted`, `received`, `discardedByReason`
por `DiscardReason`, `incoherentByCause` —el desglose de los descartes `INCOHERENT` por su causa del
Validador: `TIME_DRIFT` = regla 4a, `ALLERGEN_COVERAGE` = regla 4b—, `discards` y `contentChars`/
`contentBytes` con `contentState` BLANK/ILLEGIBLE/RECIPES) en lugar
de perder el motivo del descarte: asi un `EMPTY` es diagnosticable sin exponer el cuerpo. Las causas de
incoherencia se calculan **siempre** (correctiva `ai-verbose-diagnostics`) sin tocar el Validador: la
decision de aceptar/descartar sigue siendo suya; solo el **detalle** por descarte (motivo, causa y
titulo) y el `content` crudo del modelo quedan detras del interruptor `verboseDiagnostics` del
generador (apagado por defecto; `MainActivity` lo activa solo en debug, `BuildConfig.DEBUG`). La
`CancellationException` se relanza
siempre. `AiHttpClientFactory` construye el cliente (OkHttp, `ContentNegotiation` +
`HttpTimeout`, `expectSuccess = false`; `MockEngine` en tests), los DTOs del envelope OpenAI estan en
`OpenAiChat` y `MainActivity` inyecta `BuildConfig.DEVEXPERT_API_KEY` (desde `local.properties`) y
cablea el diagnostico a `Log`. El manifiesto declara el permiso `INTERNET`. Ver
`docs/specs/real-ai-provider.md`.

El Tema de la app vive en `com.sazon.app.ui.theme` y es la unica fuente de verdad visual,
materializada desde `DESIGN.md`: `Color.kt` (paleta + `LightColorScheme`/`DarkColorScheme` con todos
los roles sobrescritos, sin purple/pink por defecto), `Type.kt` (`SazonTypography` + familias
Manrope/Inter), `Shape.kt` (formas 16/24 + pildora) y `Spacing.kt` (espaciado 12/16/24 y metricas de
componentes). `SazonTheme` elige la variante clara/oscura con `isSystemInDarkTheme()` y **no** usa
color dinamico de Material You: ninguna ruta lee el fondo de pantalla, por lo que los colores no
cambian con el. El **modo oscuro entra en el MVP** (pregunta abierta 3 de `DESIGN.md`, resuelta). Las
fuentes OFL Manrope/Inter se empaquetan en `app/src/main/res/font/` (instancias estaticas de los pesos
600/700 y 400/500, generadas con `fontTools` de las fuentes variables oficiales; licencias en
`docs/licenses/`) y no se descargan por red. El fondo de ventana del sistema se alinea con
`background`/`darkBackground` (`values/colors.xml`, `values-night/colors.xml`, `themes.xml` y
`values-night/themes.xml`) y fija `android:forceDarkAllowed=false`. `DesignTokensTest` (JVM) compara
los tokens expuestos con el frontmatter YAML de `DESIGN.md` (con caso negativo) leyendo la ruta por la
propiedad de sistema `design.tokens.path`. Ver `docs/specs/design-tokens-and-theme.md`.

El punto de entrada real de la app vive en `com.sazon.app.ui`: `SazonApp` compone la raíz y la
**puerta de primer arranque**, que en esta feature es la **presencia de Perfil** en Room
(`ProfileRepository.observe()` → `AppGate.decide`): sin Perfil se muestra el onboarding obligatorio y
no saltable; con Perfil, el armazon principal (`MainShell`, ver mas arriba). **No** se usa el flag de DataStore (diferido sin segunda fuente de verdad que
pueda desincronizarse) ni una librería de navegación: el cambio de destino es un estado mínimo
(`AppDestination`), adoptable más tarde sin cambiar los contratos puros. El onboarding entrega el
**armazón reutilizable** (`OnboardingScaffold`: progreso "Paso N de 7" con puntos, "Atrás" y primario
anclado) y los **7 pasos reales** (paso 1 identidad; pasos 2–7 dieta, estilo, cocinas, ingredientes,
restricciones duras y cocina), con lógica pura en `com.sazon.app.ui.onboarding`
(`OnboardingStep`, `OnboardingProgress`, `AvatarInitials`, `OnboardingIdentityState`,
`OnboardingPreferencesDraft`, `OnboardingProfileBuilder`) testeada en JVM. El borrador vive en
`rememberSaveable` y al cerrar se construye y persiste el Perfil. `MainActivity` monta
`ProfileDatabase.build(applicationContext)` → `RoomProfileRepository` + `AssetTaxonomySource` +
`AssetSeedRecipeSource` + `SuggestionBatchDatabase.build(applicationContext)` →
`RoomSuggestionBatchRepository` + `SuggestionBatchService(FakeRecipeGenerator(), …)` y compone
`SazonTheme { SazonApp(...) }`. `SazonApp` observa el `UserProfile` completo (distingue la carga de la
ausencia de Perfil) y monta `MainShell`. Ver `docs/specs/onboarding-shell-and-identity.md` y
`docs/specs/home-offline-blocks.md`.

El borrador en memoria de los pasos 2–7 vive en
`com.sazon.app.ui.onboarding.OnboardingPreferencesDraft`, una data class **pura** sin imports de
Compose ni Android (se testea en JVM): `dietPatternId` (single-select, por defecto el primer
`dietPatterns` de la Taxonomía, `omnivore`), `softPreferenceIds`, `cuisineIds`,
`preferredIngredientIds` y `dislikedIngredientIds` (listas con semántica de conjunto, sin duplicados),
`hardRestrictionIds` (multi-select con "Ninguna" = lista vacía) y `difficultyId`, `timeBucketId` y
`defaultServings` (por defecto el primer `difficulties`/`timeBuckets` y `servings.default` de la
Taxonomía). Las operaciones son inmutables (`copy`): `selectDietPattern`,
`toggleSoftPreference`/`clearSoftPreferences`/`isStyleNone`, `toggleCuisine`,
`togglePreferredIngredient`/`toggleDislikedIngredient` (**exclusión mutua**: elegir un ingrediente en
un grupo lo quita del otro, de modo que la intersección nunca se solapa), `toggleHardRestriction`/
`clearHardRestrictions`/`hasNoRestrictions`, `selectDifficulty`, `selectTimeBucket` y
`setServings(limits)` (recorta al rango `min..max`). `OnboardingFlow` lo guarda con `rememberSaveable`
(Saver en la capa Compose, ampliado a todos los campos) y enruta los pasos 2–7 a los editores. Los
editores son **componentes reutilizables y sin estado** en `com.sazon.app.ui.preferences`
(`DietPatternEditor`, `SoftPreferencesEditor` con chip "Ninguno", `CuisinesEditor` en rejilla,
`IngredientsEditor` con buscador y dos grupos "Me gusta" (terracota) y "Evito" (oliva),
`HardRestrictionsEditor` con banner ámbar y chip "Ninguna", `DifficultyEditor`, `TimeBucketEditor` y
`ServingsStepper`), reciben las listas de la Taxonomía y `value` + callbacks, y no conocen el
onboarding ni el Perfil: materializan la regla "un editor, dos entradas" que `profile-screen`
reutilizará. `IngredientsFilter.matching(ingredients, query)` (puro) filtra por `label` sin distinguir
mayúsculas ni acentos. Los chips de alérgenos usan el rol **tertiary** (ámbar) y **nunca** el rol de
error/rojo; el stepper muestra cifras tabulares y deshabilita `−`/`+` en los límites. No renderizan
iconos (el repositorio no tiene assets ni dependencia de iconos) y usan solo tokens del tema.

El cierre del onboarding es **puro** en `OnboardingProfileBuilder.build(displayName, avatarColorId,
draft, now)`: valida y normaliza el nombre con `ProfileName` (mismo contrato que
`RoomProfileRepository.save`), inyecta el instante del cierre (`createdAt`/`updatedAt`) y copia todos
los campos del borrador a `PreferenceProfile`; un nombre inválido lanza `IllegalArgumentException` y no
se escribe ninguna fila. `OnboardingFlow` cambia `onFinished` a `(UserProfile) -> Unit` y construye el
Perfil al pulsar "Empezar a cocinar"; `SazonApp` lo persiste con `ProfileRepository.save` en un
`rememberCoroutineScope`, de modo que la observación existente hace que `AppGate.decide` pase a `MAIN`
y el onboarding no vuelva a aparecer. Ver `docs/specs/onboarding-ingredients-safety-kitchen.md`.

Decisiones de arquitectura que afectan a este documento: `docs/adr/0001-taxonomy-in-code.md`,
`docs/adr/0002-local-first-no-backend.md` y `docs/adr/0003-ai-provider.md`.

## Integrations

- **API de IA**: única integración externa. Proveedor decidido: **DevExpert Inference**, API
  compatible con OpenAI (base `https://inference.devexpert.io/v1`, `POST /chat/completions`, auth
  `Bearer DEVEXPERT_API_KEY`). Alias `chat` (diario) y `chat-pro` (avanzado); el modelo real puede
  cambiar sin cambiar el alias. Cuota **semanal** por clave. Ver `docs/adr/0003-ai-provider.md`.
- **Salida estructurada**: la compatibilidad con OpenAI **no** garantiza `json_schema`/structured
  outputs. El contrato del prompt pide JSON y el `RecipeValidator` parsea de forma tolerante (sin
  lanzar), por lo que la app **no** depende de salida estructurada forzada. Sí se pide
  `response_format: {"type":"json_object"}` (forma, no dominio) en el alias `chat`, con fallback
  one-shot ante `400`/`422` e interruptor en `AiProviderPolicy` (`ai-response-format`, issue #74);
  `json_schema` sigue **fuera** de alcance.
- El dominio depende de la interfaz `RecipeGenerator` (tres operaciones: sugerir por preferencias,
  buscar por texto y generar desde ingredientes), **no** de un SDK concreto. La implementación real
  es lo último que se conecta. El puerto vive en `com.sazon.app.domain.generator` (puro, sin Android)
  junto a `GenerationConstraints`, la costura minima de `generateFromIngredients`.
- **Puerto de dominio de la imagen de plato (fases 1-2 y 5)**: `com.sazon.app.domain.image`
  (puro: sin Android, sin Compose, sin Ktor, sin red y sin estado) fija la política con la que la app
  pedirá y cacheará la imagen generada de un plato (fase 1 de `docs/ai-dish-images-proposal.md`, ver
  `docs/adr/0005-ai-dish-images.md`). El puerto es
  `DishImageGenerator.suspend fun generate(request): ByteArray?` con **`null` = imagen no disponible**
  (no excepción; la cancelación sí se propaga) y el adaptador no conoce ni la `Recipe` ni la Taxonomía,
  porque `DishImageRequest.of(taxonomy, recipe, descriptor)` es el **único punto de composición** de
  `prompt`, `cacheKey` y `recipeId`. `DishImagePrompt` (objeto puro) construye la plantilla v2

  ```
  Fotografía gastronómica de <identidad>[, <cocina>][, <detalle>][, <ingredientes clave>], <COMPOSITION>, <STYLE_SUFFIX>, <NEGATIVES>
  ```

  con `<identidad>` = **título** de la receta (degradación `título → etiqueta de categoría real → plato
  casero`), `<detalle>` = `imagePrompt` del descriptor (v2: **ya no sustituye** al título, es el detalle
  visual del plato terminado, ver el párrafo siguiente), `<ingredientes clave>` = hasta
  `MAX_KEY_INGREDIENTS = 5` nombres de ingredientes **no despensa**, distintos y en el orden de la
  receta, `COMPOSITION = "una ración completa del plato en primer plano, sobre fondo neutro"`,
  `STYLE_SUFFIX = "cerámica artesanal, luz natural suave, estilo editorial gastronómico"` y
  `NEGATIVES = "sin texto, sin manos, sin personas, sin cubiertos ni marcas de agua, sin collage ni
  varios platos, sin fondo recargado"` (en v1 los negativos viajaban dentro de `STYLE_SUFFIX`), con
  omisión de la cláusula de cocina cuando el `id` no existe o está vacío. Los segmentos no vacíos se unen
  con `", "` y se colapsan las rachas de comas (nunca `,,` ni espacios dobles).
  `DishImageCacheKey` (objeto puro) deriva la clave **versionada** `dish-image-v2-<slug>` del **nombre
  del plato** normalizado tipográficamente (colapso de espacios, `NFD` + `\p{Mn}+`, `Locale.ROOT`,
  alfabeto `[a-z0-9-]`), con respaldo por `id` (`dish-image-v2-plato-<slug-id>`) y respaldo por defecto
  (`dish-image-v2-plato`) cuando no hay título; subir `VERSION` es el único mecanismo de invalidación, y
  `dish-image-prompt-v2` la sube a `dish-image-v2` al cambiar la plantilla (ADR 0005): las claves v1
  quedan huérfanas hasta que la LRU las evicta y los 6 assets del set semilla se **renombran** (mismos
  bytes, sin cuota).
  Desde la **fase 5** (`dish-visual-descriptor`) los tres ganchos aceptan un **descriptor visual
  opcional** (`DishVisualDescriptor`: `dishKey`/`imagePrompt`, valor puro con `of`/`isEmpty`/`NONE`): con
  `dishKey` utilizable la clave es la del **plato canónico** (misma normalización; `"arroz con pollo"` y
  `"arroz con pollo fácil"` **comparten** imagen) y con `imagePrompt` el prompt gana un **segmento de
  detalle** (v2; en la fase 5 sustituía la cláusula del plato, cambio deliberado de `dish-image-prompt-v2`:
  el **título es siempre la identidad**); sin descriptor la clave es la del título/`id` y el prompt
  simplemente no añade el detalle (el resto de la plantilla v2 es independiente del descriptor). La fase 5
  **no** subió `VERSION` (ni su normalización ni su plantilla cambiaron); el bump a `dish-image-v2` llega
  con `dish-image-prompt-v2` (cambio de plantilla).
  Desde `dish-visual-descriptor-wiring` (2026-10-13) el descriptor **sí se entrega en runtime**: el
  adaptador lo expone por `onDescriptors` y `MainActivity` compone con `descriptorFor(recipe.id)`
  (ver el bullet del adaptador de chat), así que un `dishKey` compartido se materializa en una sola
  imagen; sin descriptor (receta semilla, restaurada del Lote o un Favorito, o id desconocido) la
  composición es la de siempre.
  La **caché** de la fase 3 ya existe (`data.image`, ver el bullet de más abajo) y el adaptador real de
  la fase 2 también; desde la fase 4 (`dish-image-progressive-ui`) el puerto **sí tiene consumidor**: el
  nivel generado entra por delante de la cadena de `recipe-photos` (assets offline), que pasa a ser el
  **respaldo** (ver el bullet de la fase 4).
- **Adaptador real de la imagen de plato (fase 2)**: `com.sazon.app.data.ai.image` implementa el puerto
  anterior con **Ktor Client** (la misma capa `data.ai`, única que conoce la red): `POST
  /images/generations` sobre la base del proveedor con `Authorization: Bearer <clave>`, cuerpo
  `{"model":"image","prompt":<request.prompt>,"size":"1024x1024"}` (512 no lo soporta el proveedor) y
  `expectSuccess = false`. Decodifica `data[0].b64_json` de forma tolerante (`DishImagePayload`) y
  **reduce en local** a WebP q80 de 960 px de ancho (`AndroidDishImageReducer`, único fichero con
  `android.graphics`; nunca agranda). La política vive en `DishImagePolicy` (un único sitio): endpoint,
  alias `image`, `1024x1024`, ancho objetivo 960, calidad 80, timeouts propios **más cortos** que los de
  chat y **un solo intento** (`MAX_ATTEMPTS = 1`, sin reintento: no se gasta cuota dos veces). Contrato
  de fallo: error HTTP, timeout, fallo de transporte, cuerpo ilegible, base64 inválido o imagen
  indecodificable → **`null` sin lanzar**; la **cancelación** sí se propaga. La clave entra por
  constructor y solo viaja en la cabecera; el adaptador **no registra nada** (sin `Log`). Sigue **sin UI
  y sin consumidores** (lo cablea la fase 4), con el formato y el tamaño fijados aquí por el adaptador
  (ADR 0005). Ver `docs/specs/dish-image-generator.md`.
- **Ajuste con referencia de estilo (fase 6) en el adaptador de imagen**: `data.ai.image` gana
  `POST /images/edits` (multipart) con el alias `image-edit` **dentro del mismo adaptador**
  (`DevExpertDishImageGenerator`), gobernado por `DishImagePolicy` (`IMAGES_EDITS_PATH`,
  `EDIT_MODEL_ALIAS = "image-edit"`, `EDIT_FALLBACK_STATUSES = 400/404/405/413/415/422`,
  `editFailureRequiresTextFallback(status)` y el interruptor `STYLE_REFERENCE_ENABLED`). El cuerpo
  (`editsFormData`) lleva **solo** `model=image-edit`, `prompt` = el de la petición, `size=1024x1024` y
  `image` = los bytes de la referencia con su `Content-Type` y `filename`; la respuesta se decodifica
  (`DishImagePayload`) y se reduce (`DishImageReducer`) igual que en la fase 2. El respaldo por texto
  (`/images/generations`, camino de la fase 2 extraído y reutilizado) **solo** se dispara ante un
  **rechazo definitivo** del ajuste o una carga `2xx` inutilizable; un transporte/timeout o
  `401/403/429/5xx` devuelven `null` **sin** segunda llamada (misma disciplina que `MAX_ATTEMPTS = 1`),
  así que hay **como mucho dos llamadas** por `generate`. La **referencia** es un asset **empaquetado y
  versionado** (la foto neutra `img/recipes/default.<ext>` de `recipe-photos`, resuelta por extensiones
  con la política pura `DishImageStyleReferencePolicy` y leída por la costura Android
  `AssetDishImageStyleReference`; `null` sin lanzar → camino de texto), **nunca** una subida del usuario
  (sin permisos, galería, cámara, `mask`, `n`, `response_format` ni persistencia). `MainActivity` carga
  la referencia de forma *best-effort* y la pasa al adaptador; `DishImageCacheKey.VERSION` **no** sube
  (el prompt no cambia). **Matiz del presupuesto**: el tope por sesión sigue contando **una unidad por
  petición de imagen**, no por llamada al proveedor; un rechazo del ajuste implica **≤2 llamadas** (una
  desechada) para una unidad, acotado y reversible con `STYLE_REFERENCE_ENABLED = false`. Sin
  dependencias nuevas (el multipart vive en `ktor-client-core`) y sin UI. Ver
  `docs/specs/dish-style-reference.md`.
- **Caché de la imagen de plato (fase 3)**: `com.sazon.app.data.image` es la capa de datos que evita
  volver a pagar cuota y esperar por la misma imagen: `DishImageResolver.imageFor(request)` orquesta
  **memoria → disco → generador → persistir** con una `Mutex` **por clave** (dos peticiones simultáneas
  del mismo plato no pagan dos veces), `FileDishImageCache` guarda los bytes **tal cual** (sin códec:
  no importa `android.graphics`) como `<clave>.webp` bajo una raíz **inyectada** con **escritura
  atómica** (temporal + `renameTo`), tope de **80 MiB** y **evicción LRU** por `lastModified` (con toque
  de recencia al leer), y `MemoryDishImageCache` mantiene una **LRU de bytes** (16 entradas) por clave.
  `DishImageCachePolicy` reúne constantes y la selección pura de víctimas en un único sitio, y
  `AndroidDishImageCache.root(context) = File(context.cacheDir, "dish-images")` es el **único** fichero
  con `android.*` (**nunca** `filesDir`: la caché es descartable). Contrato de fallo coherente con el
  ADR 0005: `null` = no disponible (**nunca** excepción), la cancelación se propaga y la persistencia es
  *best-effort* (si el disco falla, se devuelven igualmente los bytes generados). La caché es
  JVM-testable con un generador falso y un directorio temporal, sin red, sin cuota y sin emulador
  (Robolectric solo para la costura de `cacheDir`). Desde la fase 4 tiene **consumidor y cableado** en
  `MainActivity` (a través del loader de `ui.artwork`); ver el bullet de la fase 4. Ver
  `docs/specs/dish-image-cache.md`.
- **Set semilla de imagenes de plato (fase 7)**: los **platos frecuentes del demo** viajan
  **preparados de antemano** y **versionados** en
  `app/src/main/assets/img/dishes/seed/<cacheKey>.<ext>` (extension probada en orden `webp -> jpg ->
  png`, reutilizando `RecipePhotoAssets.EXTENSIONS`) y se resuelven por la **clave canonica del plato**
  (`DishImageCacheKey`, p. ej. `dish-image-v2-arroz-con-pollo-criollo`), no por `recipeId`/`categoryId`.
  `DishImageResolver` recibe un parametro **nuevo al final con default** (`seed: DishImageSeedSource? =
  null`) y amplia su orden a **memoria -> disco -> semilla -> generador -> persistir**: un acierto de
  semilla se guarda en **memoria** y se devuelve **sin escribir en disco** (los bytes ya viajan en el
  APK; duplicarlos consumiria el tope de 80 MiB y podrian perderse por eviccion o por un borrado de
  `cacheDir`), **sin** invocar al generador (0 cuota, 0 red y 0 espera: ni la primera vez) y **sin**
  gastar el tope por sesion (solo lo consume el borde de generacion). El **disco gana a la semilla**
  (solo decide el caso degenerado de una copia generada antes de existir el asset). El mapa clave->ruta
  es **logico y puro** (`com.sazon.app.data.DishImageSeedAssets`: `ROOT`, `EXTENSIONS`, `SAFE_KEY =
  [a-z0-9-]+`, `isSafeKey`, `path`, `resolve(cacheKey, exists[, root])`, con el predicado `exists`
  **inyectado** y saneado de la clave —defensa en profundidad: una clave fabricada con `..`/`/` no
  produce ruta fuera de la raiz—), la lectura real de los assets es la costura Android
  `com.sazon.app.data.AssetDishImageSeedSource` (`Dispatchers.IO`; `null` **sin lanzar** si el asset
  falta, es ilegible o esta vacio) y el seam funcional `DishImageSeedSource` permite una fuente falsa
  en JVM. La semilla queda **detras del mismo gate** del interruptor de la fase 4 (apagado = solo la
  foto local) y la fase 7 **no** subio `DishImageCacheKey.VERSION` (solo anadio un origen de bytes); el
  bump a `dish-image-v2` de `dish-image-prompt-v2` (cambio de plantilla) obliga a **renombrar** los 6
  assets del set a `dish-image-v2-*.webp` (bytes identicos, sin cuota; no se regenera nada). La
  preparacion es una **herramienta de una sola
  vez** (`scripts/prepare-dish-image-seed.sh`, **fuera** de `init.sh` y **sin** ninguna referencia en
  `app/src`), idempotente, con la lista exacta de claves ordenadas, el prompt **espejo acotado** de
  `DishImagePrompt` v2 (título + cocina + `COMPOSITION` + `STYLE_SUFFIX` + `NEGATIVES`; no puede
  reproducir las cláusulas de `imagePrompt` ni de ingredientes, que necesitan receta/descriptor —
  deriva cosmética anotada en el propio script—, etiqueta de cocina leida de
  `docs/domain/taxonomy.json`), modo proveedor
  (`/images/edits` con la referencia neutra `img/recipes/default.<ext>` y respaldo **acotado por
  estado** a `/images/generations`) o modo `--source <dir>` **sin gastar cuota**, conversion a WebP
  q80 <=960 px cuando hay herramienta (`cwebp`/`magick`/`convert`; sin ninguna de ellas el JPEG del
  proveedor ronda los ~700 KB por plato y el presupuesto de **2 MiB** no se cumple, de modo que el
  script sale `!= 0` **sin** empaquetar nada en vez de dejar un set parcial), inventario y presupuesto
  **<= 2 MiB** con `exit != 0` ante
  cualquier desvio. Tests JVM: `DishImageSeedAssetsTest` (clave canonica -> asset real existente y no
  vacio para cada plato de la lista, orden `webp -> jpg -> png`, clave insegura/ajena al set -> `null`,
  determinismo, una clave por plato y presupuesto), `AssetDishImageSeedSourceTest` (Robolectric
  `@Config(sdk = [33])`: asset real con cabecera WebP, clave desconocida y raiz inexistente -> `null`
  sin lanzar) y los escenarios de semilla de `DishImageResolverTest` (la semilla gana al generador con
  **0** llamadas y **0** ficheros, la segunda resolucion no relee el asset, el disco gana a la semilla,
  `seed = null` mantiene la fase 3 y `null`/vacio/excepcion degradan con la cancelacion relanzada). Sin
  dependencias nuevas y sin UI. Ver `docs/specs/dish-image-seed.md`.
- **Diagnostico del origen de la imagen de plato (solo debug, `dish-image-prompt-v2`)**: responde a "de
  donde salio esta imagen" sin cambiar el pintado. `data.image.DishImageOrigin` es el vocabulario puro
  (`GENERATED` / `SEED` / `CATEGORY` / `GLYPH`; `CATEGORY` agrupa la cadena local —propia, categoria o
  por defecto— porque `RecipePhotoSource` no expone cual gano) y `data.image.DishImageOriginSink` es un
  sumidero **de sesion** (JDK puro, `ConcurrentHashMap`, sin persistencia; ignora ids en blanco y gana
  el ultimo). `DishImageResolver` gana un parametro **nuevo al final con default** no-op
  (`onResolved: (DishImageRequest, DishImageOrigin) -> Unit`) que reporta `SEED` al servir el set semilla
  y `GENERATED` en memoria/disco/generador (recuerda por clave el origen con el que pobló la memoria, así
  que un acierto de memoria sobre bytes semilla sigue reportando `SEED`); **no** cambia los bytes, el
  orden de la cadena, la `Mutex` ni la propagación de la cancelación, y un fallo **no** reporta.
  `RecipeArtworkImagePrecedence.originOf(local, generated, resolvedOrigin)` clasifica la capa que gana
  (`generated != null` → `SEED` si el resolutor registró `SEED`, `GENERATED` en otro caso; sin generada y
  con foto local → `CATEGORY`; sin ninguna → `GLYPH`) y `RecipeArtwork` lo notifica por la costura
  `ui.artwork.DishImageOriginDiagnostics` (`NoDishImageOriginDiagnostics` no-op /
  `LocalDishImageOriginDiagnostics`) como **llamada lateral** (ninguna rama de pintado depende de ella).
  `MainActivity` cablea el sumidero, el `onResolved` y el proveedor **solo** con `BuildConfig.DEBUG`,
  emitiendo `Log.d("SazonDishImage", "origin=… id=… title=…")`; en release queda el no-op y no se emite
  ninguna linea. Sin claves, URLs ni bytes en el log.
- **Imagen de plato en la UI y carga progresiva (fase 4)**: `com.sazon.app.ui.artwork` inserta el nivel
  generado **por delante** de la cadena local sin cambiar pantalla, firma ni geometría. `RecipeArtwork`
  (misma firma `(recipe, modifier, glyphSize)`) lee **dos** seams independientes —`LocalRecipePhotoSource`
  (foto local, instantánea) y `LocalGeneratedRecipeArtworkSource` (imagen generada, default no-op)— con
  sendos `produceState`, pinta **siempre** una **capa base** (foto local con `ContentScale.Crop` o, si no
  hay, el degradado + glifo de `RecipeArtworkStyle`) y **superpone** la generada con un alfa animado
  (`animateFloatAsState`, crossfade de **250 ms**): nunca hay hueco ni frame en blanco. La precedencia y
  el objetivo del crossfade son puros (`RecipeArtworkImagePrecedence`: `GENERATED`/`LOCAL`/`FALLBACK`),
  la política vive en un único sitio (`DishImageArtworkPolicy`: concurrencia **2**, crossfade **250 ms**,
  LRU de decodificadas **16**) y `DishImageArtworkLoader<T>` resuelve vía `DishImageResolver`
  (memoria → disco → generador), **decodifica** (`RecipeImageDecoder`; en la app
  `AndroidRecipeImageDecoder`, único fichero de `ui.artwork` con `android.graphics`) y mantiene una **LRU
  de `ImageBitmap`** por clave canónica. Contrato coherente con el ADR 0005: `null`/array vacío/códec
  roto degradan a la cadena local **sin cachear**, la **cancelación** se propaga (salir de pantalla o
  reciclar la tarjeta cancela la petición) y no hay `Log`. `MainActivity` compone la cadena reutilizando
  el `aiClient` compartido (los timeouts de imagen se aplican por petición) y la caché de la fase 3. La
  cadena en runtime queda `ui.artwork` → `data.image` → `data.ai.image`, con la cadena local
  (`RecipePhotoSource`/`AssetRecipePhotoSource`) como respaldo. El **ajuste de sesión** de la fase 4
  (`dish-image-settings-toggle`) se gobierna en **dos puntos**: el proveedor del seam gatea el
  **interruptor** (`DishImageSettings.generatedIfEnabled`; apagado = `null` **sin leer caché ni
  generar**, así que se usa solo la foto local) y el generador real va envuelto por
  `GatedDishImageGenerator`, que impone el **tope de generaciones por sesión** en el borde de generación
  (una unidad por petición al proveedor, `null` sin invocar al delegado cuando está agotado; los
  aciertos de memoria/disco no lo consumen). Las constantes viven en un único sitio
  (`DishImageSettingsPolicy`: `DEFAULT_ENABLED = true`, `MAX_GENERATIONS_PER_SESSION = 12`) y el estado
  observable (`DishImageSettings.state`, un `StateFlow`) alimenta la sección "Imágenes del plato" del
  Perfil. El ajuste es **de sesión** (vida del proceso, sin persistir): al reiniciar vuelve a activado
  y el contador a 12. Ver `docs/specs/dish-image-progressive-ui.md` y
  `docs/specs/dish-image-settings-toggle.md`.
- **Adaptador real (ya existe)**: `com.sazon.app.data.ai` implementa el puerto contra DevExpert
  Inference con **Ktor Client (engine OkHttp) + `kotlinx.serialization`**: compone el prompt con
  `PromptBuilder`, envia `{"model":"chat","messages":[{"role":"system"},{"role":"user"}]}` con
  `Authorization: Bearer <clave>` y, con el interruptor activo (por defecto),
  `"response_format":{"type":"json_object"}` (forma; **no** `json_schema`; sin `stream`, `temperature`
  ni `max_tokens`, ADR 0003), mapea `choices[0].message.content` y lo decodifica de forma **tolerante** con
  `AiRecipePayload`: DTO del wire `AiRecipeDraft` (contrato §4, **sin `id`/`source`**; los
  ingredientes con un DTO del wire `AiIngredientDraft` de `quantity` anulable que se normaliza a
  "al gusto" —`0.0`+`unit = null`— cuando llega nula o ausente, correctiva `ai-quantity-tolerance`), la
  app completa `id` (`AiRecipeIds`, determinista por contenido) y `source = ai` **antes** de aplicar
  `RecipeValidator.validateRecipe` con restricciones **neutras**; elemento a elemento, sin lanzar. Desde
  la **fase 5** el DTO declara además `dishKey`/`imagePrompt` como `JsonElement?` **a propósito** (un tipo
  JSON equivocado no puede contar la receta como `SCHEMA`; solo se aceptan `JsonPrimitive` de tipo string)
  y `AiDecodeResult.descriptors` expone el descriptor limpio por `id` generado **solo de recetas
  aceptadas**: desde `dish-visual-descriptor-wiring` **sí tiene consumidor en runtime** —el adaptador
  lo entrega por la costura aditiva `onDescriptors` (una vez por decode exitoso, nunca en
  fallo/degradación/cancelación), `data.image.DishVisualDescriptorStore` lo guarda **por sesión** por
  `id` y `MainActivity` compone `DishImageRequest.of(taxonomy, recipe, descriptorFor(recipe.id))`—, de
  modo que el `dishKey` del modelo es la **clave canónica compartida** y el `imagePrompt` el prompt; el
  descriptor sigue siendo **efímero** (no se persiste) y **no** entra en el `id` ni en `Recipe`. El
  **formato de la peticion** se prueba en `OpenAiChatTest` (sin formato **no** aparece
  `response_format` ni un `null`; con `ResponseFormat.JSON_OBJECT` se emite `{"type":"json_object"}`) y
  el **fallback one-shot** (400/422 -> 2 llamadas, la segunda sin formato; `401/403/404/429/5xx` sin
  fallback) y el modo (`responseFormat`/`FALLBACK`) en `DevExpertRecipeGeneratorTest`. La politica de timeouts/reintentos/degradacion vive en `AiProviderPolicy` (un unico sitio;
  timeouts calibrados contra la peor latencia real medida, connect 10 s / socket 120 s / request 150 s /
  deadline 240 s) y clasifica el fallo de transporte por su causa (timeout de socket o de peticion ->
  `TIMEOUT`; conexion o red -> `NETWORK`); los fallos se traducen a `AiUnavailableException` tipada
  (modo semilla). Ningun modulo fuera de
  `data.ai` (salvo el cableado de `MainActivity`) conoce Ktor; `FakeRecipeGenerator` se mantiene para
  test/desarrollo. Ver `docs/specs/real-ai-provider.md`.
- `FakeRecipeGenerator` (datos fijos, sin red) es la implementación por defecto en desarrollo y
  tests, y permite construir y validar toda la app antes de elegir proveedor. Vive en
  `com.sazon.app.data.generator` (Kotlin puro, sin Android ni red) y sus fixtures incluyen, a
  proposito, recetas que el Validador debe descartar: un ingrediente libre "Pan" (gluten) y un
  ingrediente de carne/ave con id real de la Taxonomia.
- **Búsqueda con IA**: no se persiste un caché consulta→resultado; se memoiza en memoria por sesión
  para evitar llamadas repetidas al volver atrás. Las recetas devueltas se guardan como recetas `ai`
  en la caché normal.
- No hay analítica, ni telemetría remota, ni backend.

## Authentication and Authorization

No aplica: no hay cuentas, roles, ni recursos compartidos. Todos los datos pertenecen al usuario del
dispositivo y no salen de él, salvo la petición de generación a la API de IA.

## Deployment and Operations

- Distribución como APK de depuración para el curso. **No** Play Store en el MVP.
- La clave de IA se inyecta en tiempo de build desde `local.properties` (que no se versiona).
- Sin CI obligatoria. El harness usa `init.sh` localmente, en WSL (ver `Testing and Verification`).
- Operación: la asume el propio estudiante; no hay entorno de producción.

## Testing and Verification

- **Tests unitarios de dominio en JVM** (obligatorios): integridad de la Taxonomía, `PromptBuilder`,
  parsing y validación de la respuesta, descarte por Restricciones duras, firma de preferencias y
  política de caché, escalado de raciones.
- **Tests JVM de la imagen de receta** (`recipe-photos`): `RecipePhotoAssetsTest` cubre la **cadena de
  respaldo pura** (semilla → categoría → `default` → `null`), el respaldo parcial (fallo del primer
  candidato), la categoría desconocida/`null` que cae a `default`, el determinismo y los casos negativos
  (`cat-no-existe` nunca es candidato); además, la **exhaustividad de assets** itera la Taxonomía real
  (`taxonomy.asset.path`) y el catálogo semilla real (`seed.recipes.asset.path`) y afirma que las **12
  recetas semilla**, las **18 `recipeCategories`** y `default` resuelven un fichero existente y no vacío
  bajo `app/src/main/assets/img/recipes/` (propiedad `recipe.photos.assets.dir` publicada por
  `app/build.gradle.kts`). `RecipeArtworkStyleTest` sigue verde sin cambios (el glifo es el último
  recurso).
- **Tests JVM de la política de imagen de plato (fases 1, 5 y v2, `domain.image`)**: `DishImagePromptTest`
  (determinismo con dos `Taxonomy` parseadas aparte, prefijo + identidad (título) + cocina real +
  ingredientes clave + `COMPOSITION` + `STYLE_SUFFIX` exactamente una vez + `NEGATIVES`, colapso de
  espacios, degradación por categoría real y al plato por defecto —receta vacía, categoría desconocida y
  Taxonomía vacía, sin lanzar—, cocina vacía o desconocida sin marcador, tope/orden/distintos/sin-despensa
  y omisión de la cláusula de ingredientes sin dejar comas sueltas, la plantilla literal y la pureza de los
  tres fuentes por lectura; con descriptor, el **título sigue siendo la identidad** y el `imagePrompt`
  entra como **detalle**, y sin `imagePrompt` el prompt no gana segmento),
  `DishImageCacheKeyTest` (las cuatro variantes del mismo nombre comparten clave, slug sin diacríticos,
  alfabeto `[a-z0-9-]` sin guiones sobrantes, estabilidad entre llamadas, discriminación por nombre,
  respaldo por `id` sin colisión, respaldo por defecto, frontera tipográfica no semántica, clave
  independiente del `locale` del dispositivo y `VERSION == "dish-image-v2"` con la clave v2 distinta de la
  v1; desde la fase 5, el `dishKey` gana al título y une variantes —`dish-image-v2-arroz-con-pollo`— y el
  respaldo es idéntico con descriptor inutilizable), `DishVisualDescriptorTest` (colapso de espacios,
  `dishKey` conservado tal cual y descartado si no deja `[a-z0-9]`, `imagePrompt` vacío descartado,
  `isEmpty`/`NONE` y pureza) y
  `DishImageRequestTest` (composición desde la Taxonomía y
  la receta, igualdad estructural, `recipeId = null` con `id` vacío o en blanco, no dependencia de la
  clasificación ni del origen, composición con descriptor y que el `data class` **no** gana campos). Corren
  en JVM con la Taxonomía real (`taxonomy.asset.path`) y **sin**
  dependencias nuevas: la fase 1 es política pura, sin red ni UI que probar. El parseo tolerante del wire
  (escenarios del descriptor presentes/ausentes/inválidos y estabilidad del `id`) vive en
  `DishVisualDescriptorWireTest` (`data.ai`). La **entrega en runtime**
  (`dish-visual-descriptor-wiring`, 2026-10-13) se prueba con `DishVisualDescriptorStoreTest`
  (`data.image`: ids en blanco y descriptores `NONE`/vacíos ignorados, último gana, `null` para
  id nulo/en blanco/desconocido y sin estado compartido entre instancias),
  `DishVisualDescriptorWiringTest` (`data.ai`: decode → `record` → `DishImageRequest.of` con la clave y
  el prompt del descriptor, dos variantes del mismo `dishKey` con la **misma** clave canónica, y
  degradación **exacta** a la fase 1 sin descriptor) y los casos nuevos de
  `DevExpertRecipeGeneratorTest` (la costura `onDescriptors` se invoca **una vez** con el mapa del
  decode —vacío si no hay descriptor—, nunca en fallo/cancelación, y su default es un no-op).
- **Tests JVM de la caché de imagen de plato (fase 3, `data.image`)**: `DishImageCachePolicyTest`
  (nombre de fichero `<clave>.webp`, constantes del tope, y selección pura de víctimas: orden por
  recencia con desempate por nombre —con un caso negativo que falla si el desempate se invierte—,
  lista vacía, ya por debajo del tope y candidato mayor que el tope), `FileDishImageCacheTest`
  (ida y vuelta, recencia al leer, escritura atómica sin temporales, evicción con un tope inyectado que
  no sale de su raíz ni toca otra extensión, y raíz/destino no utilizables sin excepción),
  `MemoryDishImageCacheTest` (LRU por acceso con tope de entradas, reemplazo y `clear`) y
  `DishImageResolverTest` (con un generador falso que cuenta llamadas: la segunda petición con la misma
  clave no genera, memoria fría → disco, sin red se sirve del disco y sin copia `null` sin escribir,
  títulos equivalentes comparten un fichero, el fichero es la clave + `.webp` con los bytes del
  generador, dos peticiones **concurrentes** de la misma clave pagan una sola generación, `null`/array
  vacío/excepción del generador degradan a `null` sin persistir, y `CancellationException` se relanza;
  desde `dish-image-prompt-v2`, `onResolved` reporta `SEED` para el set semilla —también en un acierto de
  memoria posterior— y `GENERATED` para disco/generador, un fallo/cancelación no reporta y el default
  no-op no cambia bytes ni orden de llamadas).
  `AndroidDishImageCacheTest` (Robolectric `@Config(sdk = [33])`) cubre la costura Android:
  `root(context) == File(context.cacheDir, "dish-images")` y la resolución real escribe bajo `cacheDir`,
  no bajo `filesDir`. Todo corre en JVM sin red, sin clave, sin cuota y sin emulador.
- **Tests JVM del set semilla de imagen de plato (fase 7, `data` y `data.image`)**:
  `DishImageSeedAssetsTest` (mapa puro clave canónica → asset: `DishImageCacheKey.of(título)` resuelve
  un fichero real, existente y no vacío para **cada** plato de la lista —directorio publicado por
  `dish.image.seed.assets.dir`—, orden de extensiones `webp → jpg → png`, raíz inyectable, clave
  insegura (`""`, en blanco, mayúsculas, `..`, `/`, `.`, NUL) o ajena al set → `null` **nunca** una ruta
  fuera de `ROOT`, determinismo, un asset por plato y total ≤ 2 MiB), `AssetDishImageSeedSourceTest`
  (Robolectric `@Config(sdk = [33])`: el asset real de un plato del set devuelve bytes no vacíos con
  cabecera real; clave desconocida, clave insegura y raíz inexistente → `null` sin lanzar) y, dentro de
  `DishImageResolverTest`, los escenarios de la semilla (gana al generador con **0** llamadas y **0**
  ficheros en la caché, la segunda resolución no relee el asset, el disco gana a la semilla, `seed =
  null` reproduce la fase 3, y `null`/vacío/excepción degradan con la cancelación relanzada).
- **Tests JVM de la carga progresiva de imagen (fase 4, `ui.artwork`)**: `RecipeArtworkImagePrecedenceTest`
  (precedencia pura con `String`: generada → `GENERATED`, local → `LOCAL`, ninguna → `FALLBACK`, y
  `crossfadeTarget` `1f`/`0f`; desde `dish-image-prompt-v2`, `originOf` cubre las cuatro capas del
  diagnóstico —`SEED` solo cuando el resolutor lo registró, `GENERATED` en el resto de casos con
  generada, `CATEGORY` con foto local y `GLYPH` sin ninguna—) y `DishImageArtworkLoaderTest` (con resolutor y decodificador **falsos**:
  misma clave canónica resuelta y decodificada una sola vez, `null`/array vacío/códec roto → `null`
  **sin cachear**, máximo de **2** resoluciones en vuelo con tres peticiones simultáneas, LRU acotado que
  descarta la menos usada y `CancellationException` propagada), más `GeneratedRecipeArtworkSourceTest`
  (el default no produce imagen) y `AndroidRecipeImageDecoderTest` (Robolectric `@GraphicsMode(NATIVE)`:
  WebP real → `ImageBitmap` no nulo; bytes vacíos/basura → `null`). Sin Compose en `testImplementation`:
  la lógica testeable se extrae de `@Composable`, así que corre en JVM.
- **Tests JVM del ajuste de imagen de plato (fase 4, `dish-image-settings-toggle`)**: `DishImageOriginSinkTest`
  (`data.image`: último origen gana por `recipeId`, ids `null`/en blanco ignorados sin crear entrada,
  `null` para id desconocido y sin estado compartido entre instancias —diagnóstico de
  `dish-image-prompt-v2`—), `DishImageGenerationBudgetTest` (contador atómico del tope con tope **inyectado**: `used`/`remaining`/`exhausted`
  y `remaining >= 0` con tope 0, N reservas **concurrentes** nunca rebasan `k < N`, y cada reserva
  consume una unidad), `DishImageSettingsTest` (interruptor apagado = `generatedIfEnabled` devuelve
  `null` con **0** invocaciones del productor y sin consumo; encendido = 1; apagado no reserva tope;
  apagar/encender **no** reinicia el tope agotado; `state` observable (`(true,0,12)` → `(true,1,12)`,
  `remaining`/`exhausted`) y como `StateFlow`; `GatedDishImageGenerator` no invoca al delegado con el
  tope agotado; acierto de **memoria y de disco** sin consumo con `DishImageResolver` real, caché
  temporal y generador falso; fallo → `null` con la unidad consumida y `CancellationException`
  propagada) y `DishImageSettingsCopyTest` (microcopy puro y determinista: singular/plural, tope
  alcanzado, apagado que manda sobre el tope y `max` inyectado en el texto).
- `FakeRecipeGenerator` (determinista, datos fijos, sin red) disponible para el flujo completo sin
  red, incluidas recetas inseguras para el test negativo del Validador.
- **Proveedor de IA probado en JVM con `MockEngine`** (sin red real ni clave real; la clave de test es
  ficticia): contrato de la peticion (URL, `Authorization: Bearer`, `model` y `messages`), reintentos y
  `Retry-After`, no-reintento de 4xx, deadline, cancelacion, clave ausente (0 peticiones), 200 ilegible
  y la degradacion de los llamadores reales. Ademas, la **holgura de la politica frente al peor caso
  medido** (`REAL_LATENCY_MILLIS = 115_800L`, `mimo-v2.6-pro`; `CHAT_WORST_LATENCY_MILLIS = 46_600L`:
  `socket > peor` y `socket >= 2x` el peor del alias `chat`, `request >= socket`, deadline que cabe el
  peor intento mas el backoff mas otro igualmente lento y `canRetry` cierto), la **frontera del recorte**
  por intento y la **clasificacion del fallo de transporte**
  (timeout de socket/peticion -> `TIMEOUT`; conexion/red -> `NETWORK`; resto -> `UNEXPECTED`), con el
  `detail` del `AiCallEvent`. El **diagnostico del evento** tambien se prueba: `EMPTY` por contenido
  vacio o ilegible (longitudes `content=…c/…b`, `received`/`accepted` a 0), lote parcial (`received`,
  `accepted` y `discardedByReason` con `SCHEMA`/`INCOHERENT`), lote sin aceptadas (`EMPTY` con motivos)
  y no-fuga (ningun `toString()` de evento lleva clave, `Authorization`, el cuerpo del modelo ni
  titulos). El decode se prueba con la **forma real** del contrato §4 (JSON literal
  **sin `id` ni `source`**, nunca `RecipeJson.encode`, que era el punto ciego del issue #70):
  `AiRecipePayloadTest` (decodifica, `source = ai` + `id` unico con duplicado literal, reglas del
  Validador, tolerancia y `matchScore` ignorado, resumen `received`/`accepted`/`discardedByReason` con
  los cuatro `DiscardReason` -dos de ellos con restricciones no neutras- y `contentState`
  BLANK/ILLEGIBLE/RECIPES con longitudes UTF-16 vs UTF-8; y la **tolerancia del ingrediente "al
  gusto"**, correctiva `ai-quantity-tolerance`: un ingrediente del wire con `quantity` nula, nula con
  `unit` no nula o campo ausente se normaliza a `quantity == 0.0` + `unit == null`
  -`IngredientFormat.quantityLabel` da `"Al gusto"`-, una `quantity` numerica se conserva intacta y un
  lote con una receta "al gusto" y otra rota sigue aceptando la buena) y `AiRecipeIdsTest` (determinismo,
  unicidad, formato del `id` y que las variantes `quantity` nula/ausente/`0.0`+`null` comparten `id`),
  mas la regresion end-to-end `MockEngine` 200 con forma real -> `GenerationService`
  real con `accepted` no vacio. El **desglose de `INCOHERENT`** (`ai-verbose-diagnostics`) tambien se
  prueba: las dos causas (`TIME_DRIFT`/`ALLERGEN_COVERAGE`) en el evento y **coherentes con
  `RecipeValidator.validateRecipe`** (solo 4a / solo 4b / ambas -> `TIME_DRIFT` / ninguna), y que con el
  interruptor apagado `discards`/`rawContent` quedan vacios y con el activo llevan el detalle con titulo
  y el `content` del modelo; el troceado y el formateo puros se prueban en `AiVerboseDiagnosticsTest`
  (reconstruccion por concatenacion, limite de Logcat, pares suplentes, newlines saneados y no-fuga).
  Suites en `app/src/test/java/com/sazon/app/data/ai`:
  `AiProviderPolicyTest`, `AiRecipeIdsTest`, `AiRecipePayloadTest`, `OpenAiChatTest`,
  `AiVerboseDiagnosticsTest` y `DevExpertRecipeGeneratorTest`.
  La prueba end-to-end contra el proveedor sigue siendo **manual** (emulador; ver `docs/build-brief.md` →
  Validation Plan).
- **Prueba manual end-to-end** en emulador (ver `docs/build-brief.md` → Validation Plan).
- `init.sh` debe ejecutar los tests de forma reproducible y ser el punto de entrada de verificación
  del harness.
- **Entorno de verificación: WSL2 (Ubuntu).** `bash init.sh` corre `./gradlew test` y `./gradlew lint`
  con **JDK 21** y un **SDK de Android de Linux** (el SDK de Windows solo trae binarios `.exe` y Gradle
  en Linux no puede usarlos). `scripts/wsl-env.sh` publica ese entorno —`JAVA_HOME`, `ANDROID_HOME`,
  `GRADLE_USER_HOME` en ext4, `PATH` de `gh`— y lo cargan `init.sh`, `scripts/gradle.sh` y
  `scripts/new-worktree.sh`; `scripts/wsl-bootstrap.sh` lo instala de cero (idempotente, sin `sudo`).
  Los `build/` y `.gradle/` del proyecto siguen bajo `/mnt/c` (visibles para Android Studio en
  Windows); la caché de Gradle vive en ext4 porque DrvFs es mucho más lento.

## Observability

- Sin telemetría. El diagnóstico se hace con **log local de eventos de IA**: proveedor, código de
  respuesta, duración, **longitudes del contenido**, **recetas recibidas vs. aceptadas** y **resumen
  del motivo de descarte del validador**.
- El **evento local de IA** está materializado como la costura `AiCallEvent` (`com.sazon.app.data.ai`:
  operación, desenlace `SUCCESS`/`EMPTY`/`RETRY`/`FALLBACK`/`UNAVAILABLE`, intento, estado HTTP,
  duración y `detail` con el motivo —`MISSING_KEY`, `HTTP 429`, `deadline`, …—, más el modo de
  `response_format` usado (`responseFormat = JSON_OBJECT`/`NONE`, `ai-response-format`),
  `contentChars`/`contentBytes`
  del `content` del modelo, `received`/`accepted`, `discardedByReason` con el recuento por
  `DiscardReason` y `incoherentByCause` con el **desglose** de los descartes `INCOHERENT`
  (`TIME_DRIFT` = regla 4a deriva de tiempos, `ALLERGEN_COVERAGE` = regla 4b `declaredAllergens`
  incompletos; correctiva `ai-verbose-diagnostics`), con default no-op y cableada a
  `android.util.Log` en `MainActivity` (`format=…` e `incoherent={…}` en la línea `SazonAi`; el segmento
  solo aparece cuando hay causas, de modo que un lote sin `INCOHERENT` loguea byte a byte lo de antes).
  **Nunca** incluye la clave ni la cabecera `Authorization`; en el camino por defecto tampoco el
  cuerpo del modelo ni títulos de recetas (solo longitudes, recuentos y motivos). No
  se persiste y no se muestra en la UI. Con `EMPTY`, el log distingue contenido en blanco
  (`0 c/0 b`), ilegible (longitudes > 0, `0` recibidas) y lote sin ninguna receta aceptada
  (`received > 0`, `accepted = 0` y `discards={…}`); con `FALLBACK`, que el intento con `json_object`
  fue rechazado con `400`/`422` y se reintenta sin formato.
- **Modo de diagnóstico verboso (temporal, debug-only).** El interruptor `verboseDiagnostics` de
  `DevExpertRecipeGenerator` (**apagado por defecto**; `MainActivity` lo activa solo en
  `BuildConfig.DEBUG`) añade al evento `discards` (motivo + causa + **título**) y `rawContent` (el
  `choices[0].message.content` crudo del modelo), y `MainActivity` emite una línea `discard[i/n]` por
  descarte y el crudo troceado en líneas `raw[i/n]` (helper puro `AiVerboseDiagnostics`, 3 000
  caracteres por trozo, sin partir pares suplentes). Con el interruptor apagado nada de esto viaja: el
  evento es el de antes. Es una ayuda de desarrollo **temporal** para depurar el prompt (ver
  `docs/risks-and-open-questions.md`); aun en debug, la clave y `Authorization` nunca aparecen (el
  crudo es el `content` del modelo, no la petición).
- El motivo del descarte es la señal más valiosa del producto: indica si el problema es el prompt o
  el modelo; el desglose `incoherent={TIME_DRIFT=…, ALLERGEN_COVERAGE=…}` dice **cuál** de las dos
  reglas de coherencia falló.

## Constraints

- **Sin backend**: la clave de IA viaja en el APK → solo válido para demo.
- **Coste y cuota**: DevExpert Inference tiene cuota **semanal** por clave (al 100 % se rechazan las
  peticiones hasta el reset) → la política cache-first y el modo semilla protegen el coste.
- **Red intermitente**: solo la Búsqueda con IA necesita red; el Lote de sugerencias, "Tus recetas" y
  Favoritos deben funcionar sin conexión.
- **Tamaño del APK**: las **31 fotos** de receta viajan empaquetadas en `app/src/main/assets/img/recipes/`
  y suman al APK; el presupuesto de esa carpeta es **≤ 8 MB** y conviene **WebP q80** con ancho máximo
  **960 px** cuando la máquina del implementer tiene `cwebp`/ImageMagick (`scripts/fetch-recipe-photos.sh`;
  si no hay herramienta, conserva el original y lo registra). Cada receta semilla nueva implica añadir su
  foto (ver `docs/adr/0004-recipe-photos-as-assets.md`).
- **OkHttp frente a `compileSdk`**: Ktor 3.6.0 trae OkHttp 5.5.0, cuyo artefacto Android
  (`okhttp-android`) declara `minCompileSdk=37`; con `compileSdk` 36.1 la verificación del AAR
  (`checkDebugAarMetadata`) falla. El build **fuerza OkHttp 5.4.0** en `app/build.gradle.kts` (la
  última cuyo artefacto Android acepta `compileSdk 36`). Antes de subir OkHttp/Ktor hay que comprobar
  el `minCompileSdk` del AAR o subir `compileSdk` (y su plataforma del SDK).
- **Tiempo de curso**: el MVP debe priorizar el corte vertical sobre la amplitud de features.
- **Complejidad de build**: Kotlin/Compose conlleva Gradle, SDK y emulador; es el mayor coste de
  arranque del proyecto.
