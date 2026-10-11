# Feature Implementation Spec: Búsqueda con IA desde la Home

## Source Feature

- `id`: `ai-search`
- `area`: `ui`
- `depends_on`: `["generation-flow-with-validation"]` (`accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Nota de la feature: "Único bloque de la Home que necesita red. Política de la búsqueda en
  `docs/domain-model.md`."

## Goal

Añadir el **bloque 2 de la Home** (barra de Búsqueda con IA) y su flujo: el usuario escribe qué le
apetece ("algo con pollo y arroz"), la app llama a `RecipeGenerator.searchByText`, pasa las recetas
por el `RecipeValidator` y muestra **solo las aceptadas** en la pantalla de Resultados con la consulta
como subtítulo. La Búsqueda con IA es el **único** punto de la Home que exige red: sin conexión, la
barra se deshabilita con aviso y el resto de la Home sigue igual. Si la consulta choca con el Perfil
(patrón de dieta o Restricción dura), la app muestra un estado vacío que lo explica y enlaza al Perfil
en lugar de enseñar la receta prohibida.

Se entrega como **lógica pura testeable en JVM** (orquestación `searchByText → RecipeValidator`, memo
de sesión, estado de la barra y decisión del estado de resultados) más una **capa Compose delgada** que
**reutiliza** `ResultsScreen`/`ResultsHeaders`/`ResultsStatus` (de `results-screen`) y la animación de
carga del Generador.

## Non-Goals

- **No** se implementa el **proveedor real** (`real-ai-provider`): sin HTTP, sin clave, sin red real; el
  generador conectado sigue siendo `FakeRecipeGenerator`.
- **No** se persiste nada: ni un caché consulta→resultado (se **memoiza en memoria** durante la
  sesión), ni las recetas devueltas en una base nueva. Las recetas `ai` aceptadas se reportan al
  Resumen de actividad por el seam existente (ver *Technical Approach* §6).
- **No** se añaden los **chips de orden** de Stitch ("Relevancia / Más rápida / Más fácil / Mejor
  valoradas" ni "Ajustar"): el `results-screen` los dejó fuera y no están en el
  `user_visible_behavior` ni en la verificación. Quedan para una feature correctiva.
- **No** se implementa el **modo semilla** en la Búsqueda con IA: una búsqueda fallida **no** cae a
  semilla (mostrar recetas no relacionadas sería engañoso); muestra reintento con la consulta.
- **No** se implementa el **Detalle** ni **Favoritos**: se reutilizan las costuras ya existentes
  (`onOpenRecipe` del overlay de Detalle en `MainShell`); la tarjeta no gana corazón.
- **No** se toca `ui.results` (se reutiliza tal cual), `domain.generation`, `domain.validation`,
  `domain.generator`, `domain.recipe`, `domain.suggestions`, `data/**` (salvo el nuevo
  `NetworkStatusSource`), la Taxonomía, `seed-recipes.json`, `init.sh` ni el tema.
- **No** se añaden `navigation-compose`, iconos (`material-icons-*`), imágenes ni DI; **sin
  dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos).

## Job Story

Cuando no sé qué cocinar pero sí me apetece algo concreto,
quiero pedírselo a la IA en una frase y ver recetas de verdad,
para decidir sin buscar en webs; y si lo que pido choca con mi dieta o mis alergias, quiero que me lo
explique en vez de arriesgarse.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). Escribe una consulta en la Home, espera
  (puede cancelar), ve resultados, abre el Detalle o vuelve. No hay cuentas ni roles.
- **Desarrollador / agente**: cubre en JVM la orquestación (consulta→validador), la decisión del estado
  (resultados/vacío/bloqueado/fallo), el memo y el estado de la barra, con casos negativos. El acabado
  visual y las pruebas manuales de emulador las revisa la persona (AGENTS.md no automatiza UI).
- **Sistema**: la red es la única dependencia externa y **solo** afecta al bloque 2; el resto de la
  Home, el Lote y "Tus recetas" siguen offline.

## Acceptance Scenarios

### Scenario 1: búsqueda con resultados (camino feliz)

Given la Home con conexión y la barra vacía,
When el usuario escribe "algo con pollo y arroz" y envía,
Then se muestra el estado de carga ("Buscando recetas para ti…") y después la pantalla de Resultados con
título "Resultados de tu búsqueda" y subtítulo `“algo con pollo y arroz”`, listando **solo** las recetas
aceptadas por el `RecipeValidator`, puntuadas y ordenadas por relevancia (`matchScore`).

### Scenario 2: consulta que contradice el patrón de dieta (verificación exigida por el harness)

Given un Perfil **vegano** y el `FakeRecipeGenerator` (que devuelve "Pollo al ajillo" con tag `poultry`),
When `AiSearchService.search("carne", perfilVegano)` completa,
Then `accepted` queda **vacío**, "Pollo al ajillo" aparece en `discarded` con
`DiscardReason.DIET_PATTERN`, el estado es **Bloqueado** y la UI muestra "Tu perfil no permite esta
receta" con "Revisar mi perfil"; **nunca** se pinta la receta prohibida.

### Scenario 3: restricción dura incumplida

Given un Perfil con restricción de **gluten**,
When se busca cualquier consulta y el generador devuelve "Bocadillo de tomate" (ingrediente libre "Pan"),
Then esa receta se descarta con `DiscardReason.HARD_RESTRICTION` y **no** aparece en los resultados; las
recetas seguras del mismo lote sí.

### Scenario 4: sin conexión (verificación manual)

Given el dispositivo **sin conexión**,
When se pinta la Home,
Then el bloque 2 aparece **deshabilitado y atenuado** con "Sin conexión · la búsqueda no está
disponible", el envío no es posible, y los bloques 1, 3, 4 y 5 funcionan con datos locales.

### Scenario 5: sin resultados (no bloqueo por Perfil)

Given `accepted` vacío y **sin** descartes por dieta/restricción dura,
When se pinta la búsqueda,
Then aparece el estado vacío de búsqueda con la consulta visible y el atajo "Crear receta" (abre el
Generador); no aparece el estado bloqueado.

### Scenario 6: fallo de la IA / red

Given que `searchByText` lanza,
When se pinta el resultado,
Then se conserva la consulta y aparece "No hemos podido buscar. Revisa tu conexión." con **"Reintentar"**;
"Reintentar" relanza la búsqueda con la misma consulta (sin usar el memo) y "Atrás" vuelve a la Home.

### Scenario 7: cancelar la búsqueda

Given el estado de carga en curso,
When el usuario pulsa "Cancelar búsqueda",
Then vuelve a la Home con la consulta **preservada** en la barra y sin resultados.

### Scenario 8: memo de sesión

Given una búsqueda exitosa de una consulta,
When se envía la **misma** consulta otra vez en la misma sesión,
Then **no** se vuelve a llamar al generador (se sirve el memo); una consulta **distinta** sí vuelve a
llamar. Un fallo **no** se memoiza, de modo que "Reintentar" vuelve a llamar.

### Scenario 9: Resumen de actividad

Given una búsqueda que acepta N recetas `ai`,
When completa,
Then el seam `onRecipesGenerated` recibe exactamente esas N recetas aceptadas.

### Scenario 10: el bloqueo enlaza al Perfil

Given el estado Bloqueado,
When el usuario pulsa "Revisar mi perfil",
Then la app muestra la pestaña **Perfil**.

## Repository Research

### Files Inspected

- `feature_list.json` — `ai-search` (verificación: consulta que contradice el patrón de dieta = cero
  recetas; barra deshabilitada sin conexión; subtítulo con la consulta) y `real-ai-provider`.
- `PROGRESS.md` — último estado verificado (316 tests, 49 suites, `activity-summary` `accepted`);
  `home-offline-blocks` dejó el bloque 2 pendiente "es de `ai-search`"; `results-screen` dejó
  `ResultsOrigin.Search` listo y dijo que `ai-search` montaría su propio host.
- `docs/domain-model.md` — **Política de la Búsqueda con IA** (§"Política de la Búsqueda con IA"),
  escenario 9 y los *edge cases* ("Búsqueda que choca con una restricción dura" → estado vacío que
  explica y enlaza a editar el Perfil; "sin resultados" → consulta visible + atajo a "Crear receta";
  "sin red" → conserva la consulta y reintentar). El `ActivitySummary` cuenta también las recetas de la
  Búsqueda con IA.
- `docs/technical-discovery.md` — puerto `RecipeGenerator.searchByText`, orquestación en
  `domain.generation`, `ui.results` reutilizable por `ai-search`, integración sin DI en `MainActivity`.
- `docs/design/stitch-prompt.md` sección B (bloque 2) y `docs/design/stitch/11-home-busqueda-ia.md` —
  microcopy y estados autoritativos. Sección D — copia del estado bloqueado y del error.
- `docs/design/concepts/ai-search/README.md`, `stitch-prompt.md` y
  `docs/design/concepts/ai-search/busqueda-ia/{screen.png,code.html,DESIGN.md}` — **capturas
  obligatorias presentes** (ver *Visual Design Impact*).
- `DESIGN.md` — "Buscador con IA: píldora en la cabecera de la Home, con icono de destello... Al
  enviar, lleva a Resultados con la consulta como subtítulo"; aviso de modo degradado ámbar, nunca rojo;
  una sola acción principal terracota; cifras tabulares; objetivos ≥48dp; estados vacío/carga/error.
- `docs/build-brief.md` — success criteria: "Con la red desconectada... solo la Búsqueda con IA queda
  deshabilitada con aviso"; caso negativo obligatorio (vegano que busca "carne").
- `docs/risks-and-open-questions.md` — riesgo "La Búsqueda con IA es el único punto de Home que exige
  red" y "se percibe como rota cuando el Perfil bloquea lo que el usuario escribe"; pregunta 1
  (errores/reintentos de IA) **abierta**.
- `app/src/main/java/com/sazon/app/domain/generator/RecipeGenerator.kt` — `searchByText(query, profile)`
  ya existe (`suspend`), devuelve `List<Recipe>` sin validar.
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` — patrón a imitar
  (`RecipeConstraints` desde el Perfil, `validateRecipe` receta a receta, re-lanza
  `CancellationException`, `Exception` → `aiUnavailable`, seam `onRecipesGenerated`).
- `app/src/main/java/com/sazon/app/domain/generation/GenerationResult.kt` y
  `domain/validation/RecipeValidator.kt` (`enum class DiscardReason { SCHEMA, DIET_PATTERN,
  HARD_RESTRICTION, INCOHERENT }`) — contrato reutilizado para decidir el estado.
- `app/src/main/java/com/sazon/app/ui/results/{ResultsScreen,ResultsStatus,ResultsHeader,ResultRecipeCard}.kt`
  — pantalla, estado y cabecera **reutilizables**; `ResultsOrigin.Search` ya formatea
  "Resultados de tu búsqueda" + la consulta entre comillas.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorGeneratingScreen.kt` — animación de carga
  reutilizable (hoy con "Cancelar" fijo).
- `app/src/main/java/com/sazon/app/ui/{MainShell,SazonApp}.kt`, `MainActivity.kt`,
  `ui/navigation/MainNavigationState.kt` — composición manual sin DI, `PushedScreen { GENERATOR }`,
  `MainNavigationStateSaver` (usa `name`, admite un valor nuevo), `MainShell` posee `seedRecipes`.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — los bloques 1, 3, 4 y 5; el bloque 2 falta.
- `app/src/main/AndroidManifest.xml` — sin permisos declarados hoy.
- `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt` — `searchByText` devuelve las
  4 fixtures fijas (incluye "Pollo al ajillo" con `poultry` y "Bocadillo de tomate" con "Pan" libre).

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: `ui.search` (y `domain.search`) sin imports Compose/Android,
  testeables en JVM; los `@Composable` solo pintan y delegan.
- **Reutilización**: `ResultsScreen`/`ResultsStatus`/`ResultsHeaders`/`ResultRecipeCard`, la animación
  de `GeneratorGeneratingScreen`, tokens del tema (`SazonTheme`, `SazonSpacing`, `SazonShapes`,
  `SazonPillShape`) y glifos dibujados con `Canvas` (sin assets ni iconos; "Atrás"/"×" son texto).
- **Composición manual sin DI** en `MainActivity` → `SazonApp` → `MainShell` (mismo patrón que
  `GenerationService`/`FavoriteService`).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, con casos negativos que fallarían
  si la lógica no hiciera nada.
- **Sin dependencias nuevas**; `init.sh` sin cambios; WSL (`bash init.sh`).

### Current Gaps

- La Home no tiene bloque 2: no hay barra de Búsqueda con IA ni estado "sin conexión".
- No existe orquestación de `searchByText → RecipeValidator` (solo la de `generateFromIngredients`).
- No hay detección de conectividad en el repo (`ConnectivityManager` no se usa en ningún sitio).
- No hay host de Resultados alcanzable desde la Home (`PushedScreen` solo tiene `GENERATOR`); el
  subtítulo de búsqueda existe pero nadie lo dispara.
- No existe un estado vacío "bloqueado por Perfil" ni copia de fallo de búsqueda.

## Technical Approach

Estrategia: **orquestación pura en `domain.search` + estado puro en `ui.search` + capa Compose delgada
que reutiliza `ui.results`**, con una costura mínima de conectividad para el bloque 2. Sin tocar
`ui.results`, `domain.generation` ni el build.

### 1. `domain.search.AiSearchService` (puro, patrón `GenerationService`)

```kotlin
class AiSearchService(
    private val generator: RecipeGenerator,
    private val taxonomy: Taxonomy,
    private val onRecipesGenerated: suspend (List<Recipe>) -> Unit = {},
    private val memo: SearchMemo = SearchMemo(),
) {
    suspend fun search(query: String, profile: PreferenceProfile): GenerationResult
}
```

- `key = SearchMemo.key(query)` (trim + minúsculas). Si `key` está vacío → `GenerationResult(emptyList(),
  emptyList())` **sin** llamar al generador (la UI impide enviar en blanco).
- Si `memo.get(key) != null` → devuelve el memo **sin** llamar al generador (Scenario 8).
- Si no: `generator.searchByText(key, profile)`; construye `RecipeConstraints(dietPatternId =
  profile.dietPatternId, hardRestrictionIds = profile.hardRestrictionIds.toSet())` y valida **receta a
  receta** con `RecipeValidator.validateRecipe` (no `validateResponse`; el puerto ya devuelve `List<Recipe>`).
- `CancellationException` se **re-lanza**; cualquier otra `Exception` → `aiUnavailable = true` (modo
  esperado, sin propagar) y **no** se memoiza.
- Si no hay fallo: llama `onRecipesGenerated(accepted)` y `memo.put(key, result)`.

### 2. `domain.search.SearchMemo` (puro)

`class SearchMemo { fun get(key: String): GenerationResult?; fun put(key: String, result: GenerationResult);
companion object { fun key(query: String): String } }` — mapa en memoria, **no** persistido; solo
memoiza resultados **exitosos** (fallo no). `key` = `query.trim().lowercase()`.

### 3. `ui.search` — lógica pura

- `AiSearchQuery`: `normalize(query) = query.trim()`; `canSubmit(query, isOnline) =
  isOnline && query.isNotBlank()`.
- `AiSearchCopy`: microcopy en un único sitio (ver *Visual Design Impact*): `PLACEHOLDER`, `HELPER`,
  `OFFLINE_NOTE`, `CANCEL_LABEL`, `FAILURE_TITLE`, `RETRY_LABEL`, `BLOCKED_TITLE`, `BLOCKED_BODY`,
  `REVIEW_PROFILE_LABEL`, `NO_RESULTS_TITLE`, `NO_RESULTS_BODY`, `CREATE_RECIPE_LABEL`.
- `AiSearchProgress`: `HEADLINE = "Buscando recetas para ti…"` y `subLines(query)` (tres sublíneas de la
  captura) + `promptLine(query) = "Prompt: “" + normalize(query) + "”"`.
- `AiSearchOutcome`: `sealed interface AiSearchStage { data class Results(val status: ResultsStatus, val
  header: ResultsHeader); data class Blocked(val query: String); data class NoResults(val query: String);
  data class Failure(val query: String) }` y `object AiSearchOutcome { fun stageOf(result:
  GenerationResult, query: String, preferences: PreferenceProfile, taxonomy: Taxonomy): AiSearchStage }`:
  1. `result.aiUnavailable` → `Failure`;
  2. `result.accepted.isNotEmpty()` → `Results(ResultsStatus.from(result, preferences, taxonomy,
     userIngredients = emptyList(), seedRecipes = emptyList()), ResultsHeaders.of(ResultsOrigin.Search(query), taxonomy))`;
  3. si algún `discarded.reason ∈ { DIET_PATTERN, HARD_RESTRICTION }` → `Blocked`;
  4. en otro caso → `NoResults`.
  (Regla de negocio testeable: el bloqueo por Perfil se distingue de "sin resultados" por el **motivo
  del descarte**, no adivinando.)

### 4. `data.NetworkStatusSource` (Android, costura mínima de conectividad)

`class NetworkStatusSource(context: Context) { fun observe(): Flow<Boolean> }` sobre `ConnectivityManager`
(`registerDefaultNetworkCallback` + `NetworkCapabilities` con `NET_CAPABILITY_INTERNET` y
`NET_CAPABILITY_VALIDATED`), con valor inicial derivado de la red activa. Requiere
`<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` en `AndroidManifest.xml`.
No es una dependencia nueva (API del framework). Es la **única** forma de cumplir el Scenario 4 con el
generador falso (que nunca falla por red); su decisión de UI es pura y testeable (`AiSearchQuery`).

### 5. Capa Compose — `ui.search`

- `AiSearchBar(query, isOnline, onQueryChange, onSubmit)`: bloque 2 de la Home. `Surface`/`TextField`
  con `SazonPillShape`, glifo de destello dibujado con `Canvas`, `placeholder = AiSearchCopy.PLACEHOLDER`,
  helper debajo `AiSearchCopy.HELPER`, `ImeAction.Search` + acción de envío, `enabled =
  AiSearchQuery.canSubmit(...)` para el envío. Sin conexión: campo deshabilitado, atenuado y con
  `AiSearchCopy.OFFLINE_NOTE`.
- `AiSearchFlow(query, preferences, taxonomy, searchService, onBack, onOpenRecipe, onEditProfile,
  onCreateRecipe)`: host de la página empujada. Mantiene `result`/`searchId` en `remember` y dispara
  `searchService.search(...)` en `LaunchedEffect(query, searchId)` con una duración mínima visible
  (~900 ms, paridad con el Generador). Pinta:
  - carga → `GeneratorGeneratingScreen(headline = AiSearchProgress.HEADLINE, subLines =
    AiSearchProgress.subLines(query), cancelLabel = AiSearchCopy.CANCEL_LABEL, onCancel = onBack)`
    (parámetro `cancelLabel: String = "Cancelar"` **aditivo** en esa pantalla);
  - `Results` → `ResultsScreen(status, header, taxonomy, onBack, onChangeIngredients = onCreateRecipe,
    onRetry = retry, onOpenRecipe)`;
  - `Blocked`/`NoResults`/`Failure` → estados delgados de `AiSearchStates.kt`, cada uno con cabecera
    "Atrás" (mismo estilo que `ResultsScreen`) y su cuerpo/acción.
- `AiSearchStates.kt`: `BlockedState` (tarjeta **ámbar** `tertiaryContainer`, nunca roja, con
  `BLOCKED_TITLE`/`BLOCKED_BODY` y secundario `REVIEW_PROFILE_LABEL`), `NoResultsState` (consulta
  visible + primario `CREATE_RECIPE_LABEL`) y `FailureState` (consulta conservada +
  `FAILURE_TITLE` + primario `RETRY_LABEL`).

### 6. Integración

- `PushedScreen` gana `SEARCH` (el `Saver` de `MainShell` no cambia: usa `name`).
- `HomeScreen` gana `isOnline`, `searchQuery`, `onSearchQueryChange`, `onSearch` y monta `AiSearchBar`
  **entre la cabecera (bloque 1) y la tarjeta-héroe (bloque 3)**.
- `MainShell`: `rememberSaveable` para la consulta (sobrevive al empujar/sacar la página; la Home se
  desmonta al empujar), `collectAsState` de `networkStatusSource.observe()` (inicial `true`), y en
  `PushedScreen.SEARCH` monta `AiSearchFlow`. Callbacks: `onBack = pop()`, `onOpenRecipe = { detailRecipe
  = it }`, `onEditProfile = pop().selectTab(MainTab.PROFILE)`, `onCreateRecipe =
  navigation.push(PushedScreen.GENERATOR)`.
- `SazonApp` recibe `aiSearchService` y `networkStatusSource` y los pasa a `MainShell`; `MainActivity`
  compone `AiSearchService(generator = generator, taxonomy = taxonomy, onRecipesGenerated = { recipes ->
  activitySummaryService.recordAcceptedRecipes(recipes) })` y `NetworkStatusSource(applicationContext)`.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/domain/search/AiSearchService.kt` — orquestación pura.
- `app/src/main/java/com/sazon/app/domain/search/SearchMemo.kt` — memo de sesión puro.
- `app/src/main/java/com/sazon/app/ui/search/AiSearchQuery.kt` — normalización y `canSubmit`.
- `app/src/main/java/com/sazon/app/ui/search/AiSearchCopy.kt` — microcopy único.
- `app/src/main/java/com/sazon/app/ui/search/AiSearchProgress.kt` — carga de búsqueda.
- `app/src/main/java/com/sazon/app/ui/search/AiSearchOutcome.kt` — decisión del estado (pura).
- `app/src/main/java/com/sazon/app/ui/search/AiSearchBar.kt` — bloque 2 (Compose).
- `app/src/main/java/com/sazon/app/ui/search/AiSearchFlow.kt` — host de la página empujada (Compose).
- `app/src/main/java/com/sazon/app/ui/search/AiSearchStates.kt` — bloqueado / sin resultados / fallo.
- `app/src/main/java/com/sazon/app/data/NetworkStatusSource.kt` — conectividad (Android).
- `app/src/test/java/com/sazon/app/domain/search/AiSearchServiceTest.kt`
- `app/src/test/java/com/sazon/app/domain/search/SearchMemoTest.kt`
- `app/src/test/java/com/sazon/app/ui/search/AiSearchQueryTest.kt`
- `app/src/test/java/com/sazon/app/ui/search/AiSearchOutcomeTest.kt`
- `app/src/test/java/com/sazon/app/ui/search/AiSearchProgressTest.kt`

**Modificar**

- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — montar el bloque 2 (nuevos parámetros).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — consulta en `rememberSaveable`, conectividad,
  `PushedScreen.SEARCH` → `AiSearchFlow` y callbacks.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — pasar `aiSearchService` y `networkStatusSource`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer ambos (sin DI).
- `app/src/main/java/com/sazon/app/ui/navigation/MainNavigationState.kt` — `PushedScreen.SEARCH`.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorGeneratingScreen.kt` — `cancelLabel` opcional
  (`= "Cancelar"`, comportamiento actual intacto).
- `app/src/main/AndroidManifest.xml` — `ACCESS_NETWORK_STATE`.
- `docs/technical-discovery.md`, `docs/domain-model.md`, `DESIGN.md` — ver *Durable Documentation Impact*.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `ui.results/**`, `ui.generator/**`
(salvo `cancelLabel`), `domain.generation`, `domain.validation`, `domain.generator`, `domain.recipe`,
`domain.suggestions`, `data/**` (salvo `NetworkStatusSource`), la Taxonomía, `seed-recipes.json`,
`init.sh`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` sección B + `docs/design/stitch/11-home-busqueda-ia.md`
  (y sección D para el estado bloqueado/error).
- Screens or states affected: bloque 2 de la Home (barra + helper), estado **sin conexión**, estado de
  **carga**, **Resultados con la consulta como subtítulo**, **fallo con reintento** y **bloqueo por
  Perfil**. Claro y oscuro.
- **Capturas de referencia (obligatorias, presentes)**:
  `docs/design/concepts/ai-search/busqueda-ia/screen.png`,
  `docs/design/concepts/ai-search/busqueda-ia/code.html`,
  `docs/design/concepts/ai-search/busqueda-ia/DESIGN.md` (+ `README.md` y `stitch-prompt.md` en la raíz
  de la feature). Ante cualquier contradicción, manda `DESIGN.md`. Un defecto visual se corrige con
  feature correctiva.
- Microcopy exacto (autoritativo de `stitch-prompt.md` §B y `11-home-busqueda-ia.md`; el resto se toma
  de la captura y es revisable por la persona):
  - Barra: placeholder "¿Qué te apetece hoy?"; helper "Pídeselo a la IA: «algo con pollo y arroz»";
    sin conexión: "Sin conexión · la búsqueda no está disponible".
  - Carga: "Buscando recetas para ti…", sublíneas "Interpretando antojos y texto", "Filtrando por
    alérgenos y dieta", "Calculando tiempos de cocción"; cancelar: "Cancelar búsqueda".
  - Resultados: título "Resultados de tu búsqueda" y subtítulo `“consulta”` (ya en `ResultsHeaders`).
  - Fallo: "No hemos podido buscar. Revisa tu conexión." + "Reintentar".
  - Bloqueado: "Tu perfil no permite esta receta" + "Tu alimentación o tus alergias impiden esta
    búsqueda." + "Revisar mi perfil" (tarjeta **ámbar**, nunca roja).
  - Sin resultados: título propuesto "No hemos encontrado recetas para esa búsqueda" + atajo
    "Crear receta" (microcopy propuesto, revisable).
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing`, `SazonShapes`, `SazonPillShape`,
  `MaterialTheme.colorScheme.primary`/`tertiaryContainer`; cifras tabulares; objetivos ≥48dp; una sola
  acción principal terracota por pantalla; glifos dibujados con `Canvas` (sin iconos ni imágenes).

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar `domain.search` (orquestación `searchByText` →
  `RecipeValidator`, memo de sesión) y `ui.search` (bloque 2, host empujado, estados), y la costura
  `NetworkStatusSource` + permiso.
- `docs/domain-model.md`: **update** — anotar dónde vive la orquestación de la Búsqueda con IA y que el
  memo es de sesión (sin persistencia consulta→resultado).
- `DESIGN.md`: **update** — registrar la captura aceptada `ai-search: busqueda-ia`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. `SearchMemo` + `AiSearchService` y sus tests JVM (incluido el caso negativo vegano→`DIET_PATTERN` y
   el gluten→`HARD_RESTRICTION`, el fallo del generador y la cancelación).
2. `ui.search` puro (`AiSearchQuery`, `AiSearchCopy`, `AiSearchProgress`, `AiSearchOutcome`) y tests.
3. `data.NetworkStatusSource` + permiso en el manifiesto.
4. Compose: `AiSearchBar`, `AiSearchFlow`, `AiSearchStates` y `cancelLabel` en `GeneratorGeneratingScreen`.
5. Integración: `HomeScreen`, `PushedScreen.SEARCH`, `MainShell`, `SazonApp`, `MainActivity`.
6. Ejecutar `bash init.sh` en verde y actualizar `docs/technical-discovery.md`, `docs/domain-model.md` y
   `DESIGN.md`; el implementer registra evidencia y estado.

## Implementation Tasks

- [ ] Crear `SearchMemo` (clave normalizada, get/put) y su test.
- [ ] Crear `AiSearchService.search` (valida receta a receta, re-lanza cancelación, degrada a
      `aiUnavailable`, reporta aceptadas, memoiza solo éxitos) y su test.
- [ ] Crear `AiSearchQuery` (normalize/canSubmit) y `AiSearchCopy` y sus tests.
- [ ] Crear `AiSearchProgress` (headline, sublíneas, prompt) y su test.
- [ ] Crear `AiSearchOutcome.stageOf` (Results/Blocked/NoResults/Failure) y su test.
- [ ] Crear `NetworkStatusSource` y declarar `ACCESS_NETWORK_STATE`.
- [ ] Crear `AiSearchBar` (píldora + destello, helper y nota offline) e integrarla en `HomeScreen`.
- [ ] Añadir `cancelLabel` a `GeneratorGeneratingScreen` (default "Cancelar").
- [ ] Crear `AiSearchFlow` (carga cancelable, reutiliza `ResultsScreen`/`ResultsStatus`/`ResultsHeaders`).
- [ ] Crear `AiSearchStates` (bloqueado ámbar, sin resultados con "Crear receta", fallo con "Reintentar").
- [ ] Añadir `PushedScreen.SEARCH` y enrutar en `MainShell` (consulta en `rememberSaveable`, conectividad,
      callbacks).
- [ ] Componer `AiSearchService` y `NetworkStatusSource` en `MainActivity` y pasarlos por `SazonApp`.
- [ ] Ejecutar `bash init.sh` en verde y actualizar `docs/technical-discovery.md`,
      `docs/domain-model.md` y `DESIGN.md`.

## Verification Plan

- `bash init.sh` (WSL) → **esperado**: código 0, `./gradlew test` + `./gradlew lint`, sin procesos
  largos. **No** cambia `init.sh`.
- `./gradlew testDebugUnitTest --rerun-tasks` → **esperado**: suites nuevas en verde sin romper las 49
  suites/316 tests del último estado aceptado:
  - `AiSearchServiceTest` (**verificación exigida**): perfil vegano + "carne" → `accepted` vacío y
    `Pollo al ajillo` en `discarded` con `DIET_PATTERN`; gluten → "Bocadillo de tomate" descartado con
    `HARD_RESTRICTION`; generador que lanza → `aiUnavailable` sin propagar; cancelación re-lanzada;
    consulta en blanco no llama al generador; mismo par consulta/Perfil no repite llamada (memo) y un
    fallo sí la repite; `onRecipesGenerated` recibe solo las aceptadas.
  - `SearchMemoTest`: `get`/`put`, clave normalizada (trim/minúsculas), ausente → `null`.
  - `AiSearchQueryTest`: `canSubmit` falso sin conexión aunque haya texto, falso en blanco/espacios,
    verdadero con conexión y texto; `normalize` recorta.
  - `AiSearchProgressTest`: headline, tres sublíneas y prompt con la consulta recortada.
  - `AiSearchOutcomeTest`: `Results` (aceptadas), `NoResults` (vacío sin descartes de dieta/restricción),
    `Blocked` (descarte `DIET_PATTERN`/`HARD_RESTRICTION`), `Failure` (`aiUnavailable`) y que un
    descarte `INCOHERENT`/`SCHEMA` **no** produce `Blocked`.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (se mantienen los 13 preexistentes).
- Inspección: `domain.search` sin imports `android.*`/`androidx.*`; la lógica pura de `ui.search`
  (`AiSearchQuery`/`AiSearchCopy`/`AiSearchProgress`/`AiSearchOutcome`) sin Compose/Android; `ui.results`
  intacto (solo se reutiliza); `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio aceptado,
  `data/**` (salvo `NetworkStatusSource`), la Taxonomía, `seed-recipes.json` e `init.sh` sin cambios; sin
  dependencias nuevas ni hex sueltos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - **Sin conexión** (modo avión): la barra aparece deshabilitada con "Sin conexión · la búsqueda no está
    disponible" y el resto de la Home funciona (Scenario 4).
  - Los resultados llegan con la consulta como subtítulo (Scenario 1) y el estado bloqueado enlaza al
    Perfil (Scenario 10); "Cancelar búsqueda" vuelve con la consulta conservada (Scenario 7).
  - El acabado (píldora + destello, ámbar del bloqueo, claro/oscuro) se compara con las capturas.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; la orquestación, el memo, el estado y la decisión de bloqueo se cubren en JVM y el acabado lo
revisa la persona.

## Evidence To Capture

- Salida de `bash init.sh` (código 0) y de `./gradlew testDebugUnitTest --rerun-tasks` con
  `AiSearchServiceTest`, `SearchMemoTest`, `AiSearchQueryTest`, `AiSearchOutcomeTest` y
  `AiSearchProgressTest`, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- Inspección: `domain.search` y la lógica pura de `ui.search` sin imports Compose/Android; `ui.results`
  intacto; sin cambios en build/lockfile/dominio aceptado/`init.sh`.
- Resultado manual en emulador (descripción; captura opcional): barra sin conexión, carga cancelable,
  resultados con subtítulo, estado bloqueado y fallo con "Reintentar".
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **`domain.search.AiSearchService` nuevo en vez de extender `GenerationService`**: `GenerationService`
  está `accepted` y cablea `generateFromIngredients`; la búsqueda usa otra operación del mismo puerto.
  Un servicio nuevo por operación evita tocar código aceptado y reutiliza el mismo contrato
  (`GenerationResult`).
- **Reutilizar `ResultsScreen` sin modificarlo para los resultados poblados**: su `bottomBar`
  "Cambiar ingredientes" se cablea a `onCreateRecipe` (abrir el Generador), que es la lectura más fiel
  del botón en el contexto de búsqueda; el `results-screen` ya dejó `ResultsOrigin.Search` listo. No se
  toca `ui.results`.
- **Bloqueo por Perfil decidido por el motivo del descarte** (`DIET_PATTERN`/`HARD_RESTRICTION`), no por
  la consulta: es la señal fiable que da el `RecipeValidator`; un vacío sin esos motivos es "sin
  resultados".
- **La búsqueda fallida no cae a semilla**: mostrar recetas locales no relacionadas con la consulta
  engañaría; `docs/domain-model.md` solo pide conservar la consulta y ofrecer reintento.
- **Memo de sesión, no persistencia**: la política del dominio dice "no se persiste caché
  consulta→resultado; solo se memoiza en memoria durante la sesión". El memo vive en el servicio (una
  instancia por sesión) para poder testear que no se repite la llamada.
- **Costura de conectividad mínima** (`NetworkStatusSource` + `ACCESS_NETWORK_STATE`): el generador
  conectado es el falso y nunca falla por red, así que el Scenario 4 exige detectar la conectividad; la
  decisión de UI (`canSubmit`) es pura y testeable. No añade dependencias.
- **Sin chips de orden ni modo semilla** (ver *Non-Goals*).
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Copia de "sin resultados" y del estado bloqueado**: la de la sección D y la captura difieren del
  texto de la sección B; se usa B/`11-home-busqueda-ia.md` para el fallo y D para el bloqueo, y la copia
  de "sin resultados" es propuesta. Revisable por la persona; un defecto se corrige con feature
  correctiva.
- **`onChangeIngredients` desde la búsqueda**: abrir el Generador es la lectura elegida del botón
  heredado de `ResultsScreen`; si el producto prefiere otro destino, es una feature correctiva.
- **Pregunta 1 abierta** (errores/reintentos de la IA): hoy cualquier `Exception` → `aiUnavailable`; la
  distinción fina (timeout/cuota/JSON) llegará con `real-ai-provider`. El memo no memoiza fallos, así que
  "Reintentar" siempre vuelve a llamar.
- **Process death en la página de búsqueda**: la consulta vive en `rememberSaveable` (sobrevive a
  rotación) pero el `result` en `remember`; rotar vuelve al estado de carga. Mismo criterio que el
  Generador.
- **"Las recetas devueltas entran en la caché normal de recetas `ai`"** (`docs/domain-model.md`): hoy la
  única persistencia de recetas `ai` es el Lote de sugerencias y los Favoritos. Se interpreta como
  "cuentan en el Resumen de actividad (seam) y son guardables/abribles"; no se crea caché nueva.
- **Hallazgo del implementer (Scenario 2)**: con el `FakeRecipeGenerator` real, un Perfil **vegano** solo
  descarta "Pollo al ajillo"; las otras tres fixtures son vegetales y se aceptan, así que `accepted` **no**
  queda vacío y el estado sería Resultados, no Bloqueado. La verificación exigida ("una consulta que
  contradice el patrón de dieta devuelve **cero** recetas") se cubre con un generador cuya salida es
  **toda** de dieta contraria; además se testea con el falso real que el pollo cae con `DIET_PATTERN` y
  nunca llega a `accepted`.

### Findings (implementer)

- **`cancelLabel` en `GeneratorGeneratingScreen`**: se añade como parámetro **aditivo** con default
  `"Cancelar"` (el Generador queda intacto).
- **Bloqueado/Sin resultados/Fallo**: sus cuerpos muestran la consulta en una pastilla entre comillas
  (microcopy de la captura, revisable); el bloqueado usa `tertiaryContainer` y su acción secundaria.

## Validator Checklist

- [ ] La Búsqueda con IA vive en `domain.search` + `ui.search` con lógica pura testeable en JVM y capa
      Compose delgada; `ui.results` **intacto** (solo reutilizado).
- [ ] Una consulta que contradice el patrón de dieta devuelve **cero recetas** y estado Bloqueado
      (**verificación exigida**); una Restricción dura incumplida tampoco llega a resultados.
- [ ] Sin conexión, la barra se muestra deshabilitada con aviso y el resto de la Home funciona
      (verificación manual).
- [ ] Los resultados llegan con la consulta como subtítulo ("Resultados de tu búsqueda" + `“consulta”`).
- [ ] El fallo conserva la consulta, ofrece "Reintentar" y no cae a semilla; "Cancelar búsqueda" vuelve
      con la consulta conservada.
- [ ] El memo es de sesión y no repite llamada al generador para la misma consulta; los fallos no se
      memoizan.
- [ ] Las recetas `ai` aceptadas se reportan al Resumen de actividad por el seam existente.
- [ ] `domain.generation`, `domain.validation`, `domain.generator`, `domain.recipe`,
      `domain.suggestions`, `data/**` (salvo `NetworkStatusSource`), la Taxonomía, `seed-recipes.json`,
      el build/lockfile e `init.sh` quedan intactos; sin dependencias nuevas, iconos ni hex sueltos.
- [ ] Para UI: `Visual Design Impact` nombra las capturas de `docs/design/concepts/ai-search/busqueda-ia/`,
      que **existen**, y la implementación las sigue (claro y oscuro).
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `docs/technical-discovery.md`,
      `docs/domain-model.md` y `DESIGN.md` quedan coherentes.
