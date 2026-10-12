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
| Iconos e imágenes de receta | Assets locales (`assets/icons/**`, `assets/img/recipes/**`) | Determinista, offline, sin coste |
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
categorias (terracota/`primary`). Se aplica al bloque "Tus recetas": `HomeRecipes.filteredBy(filter,
taxonomy)` acota cada grupo de forma independiente y `HomeScreen` guarda la seleccion con
`rememberSaveable` + `Saver` (clave/`fromKey`), pinta la fila y el estado "sin resultados para este
filtro"; si el bloque llega **vacio** solo pinta la linea de usuario nuevo (sin chips). La logica es
**reutilizable** por `favorites`. Ver `docs/specs/local-result-filters.md`.

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
ancho completo (degradado + glifo, meta y pildora de coincidencia, sin corazon ni click). La pantalla se
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
Compose: hero con degradado + "Atras" y corazon, tarjeta superpuesta con la meta, pestanas
Ingredientes/Pasos/Notas (timeline con pildora ambar de duracion y nutricion opcional), el `ServingsStepper`
reutilizado **sin cambios** y barra inferior ("Guardar"/"Empezar a cocinar", costuras no-op de
`favorites`/modo cocina). El detalle se hospeda como **overlay** en `MainShell`
(`var detailRecipe by remember`), de modo que se pinta **encima** de la Home o del Generador **sin
desmontarlos** (se conservan el resultado y el borrador); abre desde las tarjetas de Resultados y de la
Home. Ver `docs/specs/recipe-detail-screen.md`.

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
backoff. El fallo de transporte se clasifica por su causa en
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
el decode devuelve un **resultado tipado** `AiDecodeResult` (`accepted`, `received`, `discardedByReason`
por `DiscardReason`, `contentChars`/`contentBytes` y `contentState` BLANK/ILLEGIBLE/RECIPES) en lugar
de perder el motivo del descarte: asi un `EMPTY` es diagnosticable sin exponer el cuerpo. La
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
  lanzar), por lo que la app **no** depende de salida estructurada forzada.
- El dominio depende de la interfaz `RecipeGenerator` (tres operaciones: sugerir por preferencias,
  buscar por texto y generar desde ingredientes), **no** de un SDK concreto. La implementación real
  es lo último que se conecta. El puerto vive en `com.sazon.app.domain.generator` (puro, sin Android)
  junto a `GenerationConstraints`, la costura minima de `generateFromIngredients`.
- **Adaptador real (ya existe)**: `com.sazon.app.data.ai` implementa el puerto contra DevExpert
  Inference con **Ktor Client (engine OkHttp) + `kotlinx.serialization`**: compone el prompt con
  `PromptBuilder`, envia `{"model":"chat","messages":[{"role":"system"},{"role":"user"}]}` con
  `Authorization: Bearer <clave>` (sin `response_format`, `stream`, `temperature` ni `max_tokens`,
  ADR 0003), mapea `choices[0].message.content` y lo decodifica de forma **tolerante** con
  `AiRecipePayload`: DTO del wire `AiRecipeDraft` (contrato §4, **sin `id`/`source`**), la app completa
  `id` (`AiRecipeIds`, determinista por contenido) y `source = ai` **antes** de aplicar
  `RecipeValidator.validateRecipe` con restricciones **neutras**; elemento a elemento, sin lanzar. La politica de timeouts/reintentos/degradacion vive en `AiProviderPolicy` (un unico sitio;
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
  BLANK/ILLEGIBLE/RECIPES con longitudes UTF-16 vs UTF-8) y `AiRecipeIdsTest` (determinismo, unicidad y
  formato del `id`), mas la regresion end-to-end `MockEngine` 200 con forma real -> `GenerationService`
  real con `accepted` no vacio. Suites en `app/src/test/java/com/sazon/app/data/ai`:
  `AiProviderPolicyTest`, `AiRecipeIdsTest`, `AiRecipePayloadTest` y `DevExpertRecipeGeneratorTest`.
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
  operación, desenlace `SUCCESS`/`EMPTY`/`RETRY`/`UNAVAILABLE`, intento, estado HTTP, duración y
  `detail` con el motivo —`MISSING_KEY`, `HTTP 429`, `deadline`, …—, más `contentChars`/`contentBytes`
  del `content` del modelo, `received`/`accepted` y `discardedByReason` con el recuento por
  `DiscardReason`), con default no-op y cableada a
  `android.util.Log` en `MainActivity`. **Nunca** incluye la clave ni la cabecera `Authorization`, ni el
  cuerpo del modelo ni títulos de recetas (solo longitudes, recuentos y motivos); no
  se persiste y no se muestra en la UI. Con `EMPTY`, el log distingue contenido en blanco
  (`0 c/0 b`), ilegible (longitudes > 0, `0` recibidas) y lote sin ninguna receta aceptada
  (`received > 0`, `accepted = 0` y `discards={…}`).
- El motivo del descarte es la señal más valiosa del producto: indica si el problema es el prompt o
  el modelo.

## Constraints

- **Sin backend**: la clave de IA viaja en el APK → solo válido para demo.
- **Coste y cuota**: DevExpert Inference tiene cuota **semanal** por clave (al 100 % se rechazan las
  peticiones hasta el reset) → la política cache-first y el modo semilla protegen el coste.
- **Red intermitente**: solo la Búsqueda con IA necesita red; el Lote de sugerencias, "Tus recetas" y
  Favoritos deben funcionar sin conexión.
- **Tamaño del APK**: las imágenes locales de receta suman; conviene formato WebP y resolución
  moderada.
- **OkHttp frente a `compileSdk`**: Ktor 3.6.0 trae OkHttp 5.5.0, cuyo artefacto Android
  (`okhttp-android`) declara `minCompileSdk=37`; con `compileSdk` 36.1 la verificación del AAR
  (`checkDebugAarMetadata`) falla. El build **fuerza OkHttp 5.4.0** en `app/build.gradle.kts` (la
  última cuyo artefacto Android acepta `compileSdk 36`). Antes de subir OkHttp/Ktor hay que comprobar
  el `minCompileSdk` del AAR o subir `compileSdk` (y su plataforma del SDK).
- **Tiempo de curso**: el MVP debe priorizar el corte vertical sobre la amplitud de features.
- **Complejidad de build**: Kotlin/Compose conlleva Gradle, SDK y emulador; es el mayor coste de
  arranque del proyecto.
