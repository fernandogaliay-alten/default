# Feature Implementation Spec: Home con sus bloques locales

## Source Feature

- `id`: `home-offline-blocks`
- `area`: `ui`
- `depends_on`: `["suggestion-batch-cache"]` (`accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Abrir la **Home real** de Sazón y el **armazón principal** de la app: sustituir `HomePlaceholder` por
una pantalla `MainShell` con **barra de navegación inferior** (Inicio · Favoritos · Perfil) y **FAB**
"＋ Crear receta", y montar la Home con sus **bloques locales** según `DESIGN.md`:

1. **Cabecera** con saludo por franja horaria + nombre y avatar de iniciales.
2. *(Búsqueda con IA — **fuera de alcance**, la implementa `ai-search`.)*
3. **Tarjeta-héroe** "¿Qué tienes en la nevera?" que abre el Generador.
4. **Carrusel "Sugeridas para ti"** poblado desde el **Lote de sugerencias** cacheado (feature
   `suggestion-batch-cache`), con las recetas semilla como respaldo inmediato.
5. Bloque **"Tus recetas"** (Guardadas · Abiertas hace poco) con su estado vacío de usuario nuevo.

Todo el contenido local (bloques 1, 3, 4 y 5) y la navegación funcionan **sin conexión** y la Home se
muestra **poblada de inmediato, sin pantalla de carga**. Esta feature es la primera que **compone** el
Lote de sugerencias en la app (hasta ahora existía sin conectar) y la primera que usa la barra
inferior y el FAB. No implementa la búsqueda con IA, los Filtros de resultados, ni las pantallas reales
de Generador, Resultados, Detalle, Favoritos o Perfil.

## Non-Goals

- **No** se implementa el **bloque 2 (Búsqueda con IA)**: lo hará `ai-search`. La Home de esta feature
  renderiza 4 de los 5 bloques de `DESIGN.md`.
- **No** se implementan los **Filtros de resultados** de "Tus recetas" (categoría, "Rápido",
  "Vegetariano"): son de `local-result-filters`.
- **No** se implementan los datos de **Favoritos** ("Guardadas") ni de **"Abiertas hace poco"**:
  `favorites` y `recipe-detail-screen` los aportarán. El bloque 5 de esta feature pinta su estructura
  y su **estado vacío**; la costura de datos recibe listas y hoy se alimenta vacía (ver Decisiones).
- **No** se implementan las pantallas reales de **Generador** (`generator-ingredients-step`),
  **Resultados** (`results-screen`) ni **Detalle** (`recipe-detail-screen`): el FAB y la tarjeta-héroe
  abren un `SectionPlaceholder` ("Generador") con botón atrás, sustituible por su feature.
- **No** se implementan las pantallas reales de **Favoritos** (`favorites`) ni **Perfil**
  (`profile-screen`): sus pestañas muestran un `SectionPlaceholder` mínimo.
- **No** se añade librería de navegación (`navigation-compose`), ni iconos (`material-icons-*`), ni
  carga de imágenes (Coil/recursos): la navegación es un estado puro y las imágenes/iconos se
  sustituyen por bloques degradados y glifos dibujados con Compose (mismo criterio que `AvatarPreview`).
- **No** se implementa la búsqueda con IA, ni red de ningún tipo: el único generador conectado es
  `FakeRecipeGenerator` (sin red); la conexión al proveedor real es `real-ai-provider`.
- **No** se calcula `matchScore` (su fórmula es de `results-screen`): la píldora de coincidencia solo
  se pinta cuando el valor no es `null`.
- **No** se toca `ProfileDatabase` ni `data.profile/**`/`domain.profile/**`/`domain.suggestions/**`; el
  Lote se **consume** tal cual.
- **No** se añaden dependencias de build: Compose, Material 3, Room, corrutinas y serialización ya están.

## Job Story

Cuando abro Sazón con mi Perfil creado,
quiero una Home que me salude por mi nombre, me ofrezca sugerencias al instante y me deje crear una
receta o ir a mis recetas guardadas y a mi perfil,
para tener siempre algo que cocinar sin conexión y sin esperas.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; ve y navega la Home. No hay cuentas ni permisos.
- **Desarrollador / agente**: implementa la pantalla y el armazón y valida la **lógica/estado en JVM**
  (saludo, identidad/avatar, fallback a semilla, estado del bloque 5, navegación); el acabado
  puramente visual lo revisa la persona tras el merge (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: la Home se muestra poblada de inmediato, sin pantalla de carga (verificación exigida)

Given un Perfil existente y el Lote de sugerencias aún sin resolver en disco,
When se entra en `MAIN`,
Then la Home pinta cabecera, tarjeta-héroe, carrusel y "Tus recetas" sin estado de carga a pantalla
completa, y el carrusel ya muestra las **recetas semilla** empaquetadas
(`HomeSuggestions.visible(batchRecipes = null, seedRecipes)` devuelve `seedRecipes`).

### Scenario 2: cabecera con saludo por franja horaria y avatar de iniciales

Given un Perfil "Lucía" con `avatarColorId = "terracotta"` y la hora 14,
When se pinta la cabecera,
Then el saludo es `"Buenas tardes, Lucía"` y el avatar muestra la inicial `"L"` sobre el `hex`
terracota de la Taxonomía; con la hora 9 es `"Buenos días, Lucía"` y con la 22
`"Buenas noches, Lucía"`.

### Scenario 3: nombre ausente o color corrupto se degradan (edge cases de `docs/domain-model.md`)

Given un Perfil con `displayName` vacío/solo espacios o con `avatarColorId` inexistente,
When se pinta la cabecera,
Then el saludo cae a `"Buenas tardes"` (sin nombre), el avatar muestra el **icono genérico** (nunca
iniciales vacías) y el color usa el **primer** `avatarColors` de la Taxonomía.

### Scenario 4: el carrusel refleja el Lote de sugerencias cacheado

Given un Lote persistido con recetas `seed` y `ai`,
When la Home observa `SuggestionBatchRepository.observe()`,
Then el carrusel "Sugeridas para ti" muestra las recetas del Lote; al entrar se asegura el Lote una vez
con `SuggestionBatchService.ensureBatch` (política **cache-first** ya aceptada en
`suggestion-batch-cache`: sin red y sin llamada al Generador si la caché es fresca y la firma no cambia).

### Scenario 5: bloque "Tus recetas" con estado vacío de usuario nuevo

Given que no hay recetas guardadas ni abiertas hace poco,
When se pinta el bloque "Tus recetas",
Then se ven su título, las dos etiquetas de grupo ("Guardadas" y "Abiertas hace poco") y la línea de
estado vacío `"Cuando guardes una receta aparecerá aquí."` (`HomeRecipes.isEmpty == true`); si
cualquiera de las dos listas trae contenido, `isEmpty` es `false` y el grupo correspondiente se pinta.

### Scenario 6: barra de navegación inferior de tres destinos (verificación manual "navegan")

Given `MAIN`,
When se observa la barra inferior,
Then tiene tres destinos en orden **Inicio · Favoritos · Perfil** (`MainTab.entries`), arranca en
`Inicio`, y al pulsar otro destino el estado cambia a ese destino (`MainNavigationState.selectTab`);
el activo usa terracota (icono relleno + etiqueta) y el inactivo el gris de la barra
(`SazonComponents.bottomNavInactive`).

### Scenario 7: el FAB y la tarjeta-héroe abren el Generador y se puede volver

Given la Home seleccionada,
When se pulsa el FAB "＋ Crear receta" o el botón "Crear receta" de la tarjeta-héroe,
Then el estado de navegación empuja `PushedScreen.GENERATOR` (la pestaña no cambia), se muestra el
`SectionPlaceholder` del Generador con botón atrás y `pop()` vuelve a la Home sin perder la pestaña.

### Scenario 8: todo el contenido local y la navegación funcionan sin conexión (verificación exigida)

Given el dispositivo sin red,
When se usa la Home,
Then los bloques 1, 3, 4 y 5 y la navegación inferior/FAB funcionan; el Lote se resuelve desde la
caché/`seed` y, si hubiera que generar, `SuggestionBatchService` degrada a semilla (comportamiento ya
aceptado). La Búsqueda con IA (bloque 2, el único que necesita red) **no** se renderiza en esta
feature.

## Repository Research

### Files Inspected

- `feature_list.json` — definición de `home-offline-blocks` (verificación manual de bloques 1, 3, 4 y
  5 sin red y "poblada de inmediato"; nota "cinco bloques según DESIGN.md", búsqueda con IA en
  `ai-search`, introduce barra inferior y FAB) y features vecinas (`local-result-filters`,
  `generator-ingredients-step`, `favorites`, `profile-screen`, `ai-search`).
- `PROGRESS.md` — última aceptada `suggestion-batch-cache`; siguiente lista `home-offline-blocks`;
  camino de verificación `init.sh` (`./gradlew test` + `lint`); regla de no automatizar UI de emulador.
- `AGENTS.md` — verificación de UI por lógica/estado en JVM cuando sea posible; commit/push/PR fuera
  del planner; una feature por sesión.
- `CONTEXT.md` — "Bloque 'Tus recetas'", "Filtros de resultados", "Sugerencia", "Lote de sugerencias",
  "Búsqueda con IA", "Favorito".
- `docs/build-brief.md` — Home de cinco bloques; solo la búsqueda necesita red; "la semilla se muestra
  de inmediato".
- `docs/domain-model.md` — `SuggestionBatch` y su ciclo; scenario 1 (semilla inmediata), 5 (apertura sin
  conexión); edge cases de nombre vacío, color ausente y duplicados; el bloque "Tus recetas" agrupa
  favoritas y abiertas hace poco.
- `DESIGN.md` — dirección visual, tokens, `bottom-nav` (80dp, `activeColor` primary,
  `inactiveColor #9A8C7F`), `RecipeCard`/`RecipeListCard`, "una acción principal por pantalla".
- `docs/design/stitch-prompt.md` — sección 3 (navegación: barra de 3 destinos + FAB "＋ Crear receta";
  Generador/Resultados/Detalle son páginas empujadas con botón atrás) y sección B (Home: microcopy
  exacto de los cinco bloques; variante offline). Sección A solo se cita para el armazón previo.
- `docs/technical-discovery.md` — Lote en `domain.suggestions` + `data.suggestions`, sin composición
  en la app todavía ("la monta `home-offline-blocks`"); `SazonApp`/`AppGate`/`HomePlaceholder`.
- `docs/specs/suggestion-batch-cache.md` — `SuggestionBatchService.ensureBatch(profile, force)`,
  `SuggestionBatchRepository.observe()`, `SuggestionBatch`, `SuggestionBatchDatabase.build`,
  `RoomSuggestionBatchRepository`; la composición queda para esta feature.
- `docs/specs/onboarding-shell-and-identity.md` — patrón de costura de navegación mínima
  (`AppDestination`, sin librería), `AvatarPreview` reutilizable, `AppGate`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — raíz de composición actual (ProfileDatabase +
  RoomProfileRepository + AssetTaxonomySource + `SazonApp`).
- `app/src/main/java/com/sazon/app/ui/{SazonApp,AppGate,HomePlaceholder}.kt` — observación del Perfil,
  puerta y destino `MAIN` placeholder.
- `app/src/main/java/com/sazon/app/ui/onboarding/{OnboardingScaffold,AvatarPreview,AvatarInitials,
  OnboardingFlow,OnboardingPreferencesDraft}.kt` — armazón, avatar y patrón de Saver de Compose.
- `app/src/main/java/com/sazon/app/domain/suggestions/*` — modelo, puerto, política y servicio del
  Lote; `app/src/main/java/com/sazon/app/data/suggestions/*` — persistencia Room.
- `app/src/main/java/com/sazon/app/domain/{recipe/Recipe,recipe/SeedRecipeCatalog,profile/UserProfile,
  taxonomy/Taxonomy}.kt` — `Recipe`/`RecipeSource`, `SeedRecipeCatalog.recipes`, `UserProfile.
  preferences`, `Taxonomy.difficulty/avatarColor/recipeCategory`.
- `app/src/main/java/com/sazon/app/data/{AssetTaxonomySource,AssetSeedRecipeSource}.kt` — lectura de
  assets; `data/generator/FakeRecipeGenerator.kt` — generador sin red.
- `app/src/main/java/com/sazon/app/ui/theme/{Color,Spacing,Shape,Type}.kt` — tokens, roles
  `bottomNavHeight`/`bottomNavInactive`, `SazonPillShape`, esquemas claro/oscuro.
- `app/src/test/java/com/sazon/app/ui/{AppGateTest}.kt` y `.../onboarding/*Test.kt` — patrón de tests
  JVM puros con nombres en español y casos negativos.
- `app/build.gradle.kts` — propiedades de sistema `taxonomy.asset.path` y
  `seed.recipes.asset.path` ya disponibles; sin dependencia de navegación, iconos ni imágenes.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: los cálculos/estados de la Home viven en Kotlin sin imports
  de Compose/Android y se testean en JVM; los `@Composable` solo pintan.
- **Costura de navegación sin librería**: patrón de `AppDestination` + estado mínimo. Aquí se amplía a
  `MainTab`/`PushedScreen`/`MainNavigationState` puros.
- **Composición de dependencias manual** en `MainActivity` (sin DI), como ya hace con el Perfil.
- **Reutilización**: `AvatarPreview` y `AvatarInitials` ya existen y se reutilizan en la cabecera.
- **Tokens desde `MaterialTheme`/`SazonSpacing`/`SazonComponents`**: nunca hex sueltos en pantallas.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, casos negativos que prueban que
  no pasan en vacío; la Taxonomía real se lee por `taxonomy.asset.path`.
- **Sin dependencias nuevas** y `init.sh` sin cambios (`./gradlew test` + `./gradlew lint`, sin
  procesos largos).

### Current Gaps

- `MAIN` es `HomePlaceholder` (una Surface con "Inicio"): no hay Home, ni bloques, ni barra inferior,
  ni FAB.
- El Lote de sugerencias existe pero **no está compuesto** en la app: `MainActivity` no monta
  `SuggestionBatchDatabase`/`RoomSuggestionBatchRepository`/`SuggestionBatchService` ni llama a
  `ensureBatch`; la Home no observa el Lote.
- No hay destinos de nivel superior (`MainTab`), ni pestañas placeholder, ni forma de abrir el
  Generador.
- No existe ningún componente de tarjeta de receta, cabecera, héroe, carrusel ni bloque "Tus recetas".
- El catálogo semilla y la Taxonomía se leen, pero la Home no los usa todavía.

## Technical Approach

Estrategia: **lógica/estado puro testeable en JVM + capa Compose delgada**, sin dependencias nuevas.

### 1. Lógica pura de la Home — `com.sazon.app.ui.home`

- `HomeGreeting.text(displayName: String?, hour: Int): String`:
  - `displayName` nulo o en blanco (tras `trim`) → `"Buenas tardes"` (literal de
    `docs/design/stitch-prompt.md` y `docs/domain-model.md`).
  - Con nombre → `"$banda, ${displayName.trim()}"`, con `banda` = `"Buenos días"` (5..11),
    `"Buenas tardes"` (12..20) o `"Buenas noches"` (21..4).
- `HomeIdentity(hex: String, initials: String?)` y `HomeIdentity.from(profile: UserProfile,
  taxonomy: Taxonomy)`: `hex` del `avatarColorId` y, si no resuelve, del **primer** `avatarColors`;
  `initials` con `AvatarInitials.of(displayName)`.
- `HomeSuggestions.visible(batchRecipes: List<Recipe>?, seedRecipes: List<Recipe>): List<Recipe>` =
  `batchRecipes ?: seedRecipes` (respaldo inmediato a semilla).
- `HomeRecipes(saved: List<Recipe>, recentlyOpened: List<Recipe>)` con `val isEmpty: Boolean`
  (`saved.isEmpty() && recentlyOpened.isEmpty()`). Es la costura del bloque 5.
- `RecipeCardFormat.metaLine(taxonomy, recipe): String` =
  `"${totalMinutes} min · ${dificultad} · ${servings} pers."`, con la etiqueta de `difficultyId` y
  respaldo al propio id si no resuelve; `RecipeCardFormat.matchBadge(score: Int?): String?` =
  `"$score% coincidencia"` o `null`.

### 2. Navegación pura — `com.sazon.app.ui.navigation`

- `enum class MainTab(val label: String) { HOME("Inicio"), FAVORITES("Favoritos"), PROFILE("Perfil") }`.
- `enum class PushedScreen { GENERATOR }` (crecerá con `RESULTS`/`DETAIL` en sus features).
- `data class MainNavigationState(val tab: MainTab = MainTab.HOME, val pushed: PushedScreen? = null)`
  con `selectTab(tab)`, `push(screen)` y `pop()` inmutables (`copy`). La pestaña se conserva al
  empujar/sacar una página.

### 3. Capa Compose — `com.sazon.app.ui` y `com.sazon.app.ui.home`

- `MainShell(profile, taxonomy, suggestionBatchRepository, suggestionBatchService, seedRecipes,
  savedRecipes, recentlyOpenedRecipes)`: `Scaffold` con:
  - `bottomBar` = Material 3 `NavigationBar` (80dp, `SazonComponents.bottomNavHeight`) con
    `NavigationBarItem` por `MainTab` (glifo simple dibujado con Compose; activo con
    `colorScheme.primary`, inactivo `SazonComponents.bottomNavInactive`).
  - `floatingActionButton` = FAB/`ExtendedFloatingActionButton` terracota "＋ Crear receta" que
    `push(GENERATOR)`.
  - Contenido según `nav.tab`: `HOME` → `HomeScreen(...)`; `FAVORITES`/`PROFILE` →
    `SectionPlaceholder(label, ...)`. Si `nav.pushed == GENERATOR` → `SectionPlaceholder("Generador",
    onBack = pop)` a pantalla completa (por encima de las pestañas, con botón atrás).
  - El estado se guarda con `rememberSaveable(stateSaver = MainNavigationStateSaver)` (patrón Saver de
    `OnboardingFlow`), de modo que sobrevive a cambios de configuración.
- `HomeScreen(profile, taxonomy, suggestions, recipes, onOpenGenerator)`: `LazyColumn` con los bloques
  locales en orden:
  1. **Cabecera**: `HomeGreeting.text(profile.displayName, hour)` (hora con `Calendar`, una vez) +
     `AvatarPreview(identity.hex, identity.initials, size = 40.dp)`.
  2. *(Búsqueda con IA: no se pinta en esta feature.)*
  3. **Tarjeta-héroe**: título "¿Qué tienes en la nevera?", texto "Añade tus ingredientes y te
     proponemos recetas al instante." y botón "Crear receta" (`onOpenGenerator`).
  4. **"Sugeridas para ti"** (caption "basado en tus gustos"): `LazyRow` de `RecipeCard` sobre
     `suggestions`.
  5. **"Tus recetas"**: título + `HomeRecipesGroup("Guardadas", recipes.saved)` y
     `HomeRecipesGroup("Abiertas hace poco", recipes.recentlyOpened)`; cuando el bloque está vacío, la
     línea "Cuando guardes una receta aparecerá aquí."
- `RecipeCard(recipe, taxonomy, modifier)`: tarjeta 24dp con **bloque degradado** melocotón→oliva y un
  glifo de plato dibujado con Compose (sin imágenes ni iconos), título a 2 líneas, `metaLine` y píldora
  terracota de coincidencia solo si `matchBadge != null`. Sin acción de click (el Detalle es de
  `recipe-detail-screen`).
- `HomeRecipesGroup(title, recipes, taxonomy)`: título de grupo y, si la lista no está vacía,
  `LazyRow` de tarjetas horizontales; si está vacía, no pinta nada (el estado vacío global lo cubre la
  línea del bloque).
- `SectionPlaceholder(title, onBack: (() -> Unit)? = null)`: destino mínimo reutilizable (Inicio
  provisional eliminado; Favoritos/Perfil/Generador). Sustituible por sus features.

### 4. Integración en la app

- `MainActivity` amplía la composición: monta `SuggestionBatchDatabase.build(applicationContext)` +
  `RoomSuggestionBatchRepository(db.suggestionBatchDao())`, carga la Taxonomía
  (`AssetTaxonomySource`) y las recetas semilla (`AssetSeedRecipeSource.load().recipes`) y construye
  `SuggestionBatchService(FakeRecipeGenerator(), suggestionBatchRepository, taxonomy, seedRecipes)`.
  Pasa todo a `SazonApp` (la Taxonomía deja de cargarse perezosamente dentro de `SazonApp`).
- `SazonApp` observa el **`UserProfile` completo** (no solo la puerta): mientras es `null` pinta el
  fondo del tema; con `AppGate.decide == ONBOARDING` monta `OnboardingFlow`; con `MAIN` monta
  `MainShell(profile = profile, ...)`.
- `HomeScreen` observa `suggestionBatchRepository.observe()` (`collectAsState(initial = null)`) y
  lanza `LaunchedEffect(profile) { suggestionBatchService.ensureBatch(profile.preferences) }` una vez
  al entrar. Sugerencias = `HomeSuggestions.visible(batch?.recipes, seedRecipes)`, de modo que la
  semilla pinta al instante y el Lote la sustituye cuando se resuelve. No se usa `runCatching` (no
  tragarse `CancellationException`); el servicio ya degrada a semilla ante fallo del generador.
- El bloque 5 recibe `savedRecipes = emptyList()` y `recentlyOpenedRecipes = emptyList()` desde
  `MainShell` como costura documentada (ver Decisiones); `favorites`/`recipe-detail-screen` los
  alimentarán.
- `HomePlaceholder.kt` se elimina (lo sustituye `MainShell` + `HomeScreen`).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/home/HomeGreeting.kt` — saludo puro por franja horaria.
- `app/src/main/java/com/sazon/app/ui/home/HomeIdentity.kt` — avatar (hex + iniciales) del Perfil.
- `app/src/main/java/com/sazon/app/ui/home/HomeSuggestions.kt` — respaldo inmediato a semilla.
- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — modelo del bloque "Tus recetas".
- `app/src/main/java/com/sazon/app/ui/home/RecipeCardFormat.kt` — `metaLine`/`matchBadge` puros.
- `app/src/main/java/com/sazon/app/ui/navigation/MainNavigationState.kt` — `MainTab`, `PushedScreen` y
  estado de navegación puros.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — barra inferior + FAB + enrutado de pestañas y
  página empujada + `Saver`.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — los cuatro bloques locales.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` — tarjeta del carrusel (imagen degradada +
  glifo).
- `app/src/main/java/com/sazon/app/ui/SectionPlaceholder.kt` — destino mínimo reutilizable.
- `app/src/test/java/com/sazon/app/ui/home/HomeGreetingTest.kt` — escenarios 2 y 3.
- `app/src/test/java/com/sazon/app/ui/home/HomeIdentityTest.kt` — escenarios 2 y 3.
- `app/src/test/java/com/sazon/app/ui/home/HomeSuggestionsTest.kt` — escenario 1.
- `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` — escenario 5.
- `app/src/test/java/com/sazon/app/ui/home/RecipeCardFormatTest.kt` — formato de tarjeta.
- `app/src/test/java/com/sazon/app/ui/navigation/MainNavigationStateTest.kt` — escenarios 6 y 7.

**Modificar**

- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer las dependencias del Lote y dejar de
  cargar la Taxonomía dentro de `SazonApp`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — observar el `UserProfile`, enrutar a `MainShell`
  y recibir las nuevas dependencias.
- `docs/technical-discovery.md` — ubicar la Home (`ui.home`), el armazón (`MainShell`: barra inferior
  de 3 destinos + FAB), la integración del Lote en la app y la Home offline.

**Eliminar**

- `app/src/main/java/com/sazon/app/ui/HomePlaceholder.kt` — sustituido por `MainShell`/`HomeScreen`.

**No se toca** `app/build.gradle.kts` ni `gradle/libs.versions.toml` (sin dependencias); tampoco
`ProfileDatabase`, `data.profile/**`, `domain.profile/**`, `domain.suggestions/**`,
`data.suggestions/**`, la Taxonomía, `seed-recipes.json` ni las features de onboarding.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (`bottom-nav`, `RecipeCard`, tipografía, formas, spacing) +
  `docs/design/stitch-prompt.md` (secciones 3 y B, con la microcopy exacta de los bloques 1, 3, 4 y 5).
- Screens or states affected: Home (cabecera, héroe, carrusel, "Tus recetas"), estado vacío de "Tus
  recetas", barra inferior (activo/inactivo), FAB y placeholders (Favoritos/Perfil/Generador).
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonComponents.bottomNavHeight`
  (80dp)/`bottomNavInactive` (#9A8C7F), `MaterialTheme.colorScheme.primary` (acción principal),
  `SazonPillShape`; el `hex` del avatar sale de `avatarColors` de la Taxonomía.
- Reglas visuales: una única acción principal terracota por pantalla; tarjetas/imágenes a 24dp; sombras
  suaves; objetivos táctiles ≥48dp; tipografía dinámica (el texto hace scroll, nunca se recorta); el
  activo de la barra combina glifo relleno + etiqueta, no solo color.
- **Sin** material-icons ni imágenes: glifos y bloque de imagen con primitivas Compose (mismo criterio
  documentado en `onboarding-shell-and-identity` para `AvatarPreview`).
- Capturas de referencia (backfill PR #37): `docs/design/concepts/home-offline-blocks/` — pantallas
  `home` y `home-sin-conexion`. Pendiente de exportar desde Stitch (petición en el `README.md` de la
  carpeta); el repaso visual (glifos, contraste claro/oscuro, densidad del carrusel) lo hace la persona
  tras el merge y un defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — registrar `ui.home`/`MainShell` (barra inferior + FAB),
  la integración del Lote en la app (composición en `MainActivity`, `ensureBatch` al entrar en Home,
  respaldo a semilla) y que la Home es offline.
- `docs/domain-model.md`: **not needed** — describe ya `SuggestionBatch`, "Tus recetas" y sus edge
  cases; esta feature no cambia el modelo.
- `DESIGN.md` y `docs/design/concepts/`: **update** — `DESIGN.md` sigue siendo la fuente de verdad; se
  añade el backfill de capturas `home` y `home-sin-conexion` en
  `docs/design/concepts/home-offline-blocks/`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión de planificación.

## Implementation Plan

1. Crear la lógica pura de la Home (`HomeGreeting`, `HomeIdentity`, `HomeSuggestions`, `HomeRecipes`,
   `RecipeCardFormat`) y de navegación (`MainTab`, `PushedScreen`, `MainNavigationState`).
2. Escribir los tests JVM de esa lógica (saludo, avatar, respaldo a semilla, bloque vacío, formato de
   tarjeta, navegación) con casos negativos.
3. Crear `RecipeCard`, `HomeScreen`, `SectionPlaceholder` y `MainShell` sobre los tokens del tema.
4. Ampliar `MainActivity` (SuggestionBatchDatabase + repositorio + servicio + Taxonomía + semillas) y
   reescribir `SazonApp` para observar el `UserProfile` y montar `MainShell`.
5. Eliminar `HomePlaceholder.kt`.
6. Ejecutar `init.sh` (test + lint) en verde y corregir lo que salga.
7. Actualizar `docs/technical-discovery.md`; el implementer registra la evidencia y el estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `HomeGreeting.text(name, hour)` con franjas 5..11/12..20/21..4 y fallback "Buenas tardes".
- [ ] Crear `HomeIdentity.from(profile, taxonomy)` con iniciales (`AvatarInitials`) y respaldo de color.
- [ ] Crear `HomeSuggestions.visible(batchRecipes, seedRecipes)`.
- [ ] Crear `HomeRecipes(saved, recentlyOpened)` con `isEmpty`.
- [ ] Crear `RecipeCardFormat.metaLine`/`matchBadge`.
- [ ] Crear `MainTab`, `PushedScreen` y `MainNavigationState` (`selectTab`/`push`/`pop`).
- [ ] Escribir los tests JVM de los seis puntos anteriores con casos negativos.
- [ ] Crear `RecipeCard` (bloque degradado + glifo, `metaLine`, píldora de coincidencia opcional).
- [ ] Crear `HomeScreen` con los bloques 1, 3, 4 y 5 y `SectionPlaceholder`.
- [ ] Crear `MainShell` (NavigationBar de 3 destinos, FAB, pestañas placeholder, Generador empujado,
      `Saver`) y eliminarlo del placeholder anterior.
- [ ] Ampliar `MainActivity` (SuggestionBatchDatabase/repositorio/servicio, Taxonomía y semillas) y
      `SazonApp` (observar `UserProfile`, montar `MainShell`).
- [ ] Eliminar `ui/HomePlaceholder.kt`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: las nuevas suites JVM en verde sin romper las 132
  existentes:
  - `HomeGreetingTest`: `(9,"Lucía")` → `"Buenos días, Lucía"`; `(14,"Lucía")` →
    `"Buenas tardes, Lucía"`; `(22,"Lucía")` → `"Buenas noches, Lucía"`; `(null/""/"  ")` →
    `"Buenas tardes"`; fronteras 4/5/11/12/20/21 (caso negativo: no devuelve nombre vacío ni coma
    suelta).
  - `HomeIdentityTest`: `avatarColorId` válido → su `hex`; id inexistente → el del primer
    `avatarColors`; `"Lucía Gómez"` → inicial `"LG"`; nombre vacío → `initials == null`.
  - `HomeSuggestionsTest`: `visible(null, seeds) == seeds`; `visible(recetas, seeds) == recetas`
    (caso negativo: la lista vacía no sustituye a las semillas).
  - `HomeRecipesTest`: sin listas → `isEmpty`; con `saved` o `recentlyOpened` → no vacío.
  - `RecipeCardFormatTest`: `metaLine` con la dificultad real de la Taxonomía (`... · ${label} · ...`);
    id desconocido → respaldo al id; `matchBadge(null) == null` y `matchBadge(92) == "92% coincidencia"`.
  - `MainNavigationStateTest`: por defecto `HOME` y sin página empujada; `selectTab` cambia la pestaña;
    `push(GENERATOR)` conserva la pestaña y `pop()` la deja limpia (caso negativo: `pop()` sin página
    no cambia nada).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores, sin warnings nuevos en ficheros de la
  feature (los preexistentes de versión/plantilla son aceptables).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Con la red desactivada, los bloques 1, 3, 4 y 5 se ven y navegan; la Home se muestra poblada de
    inmediato, sin pantalla de carga (verificación exigida).
  - La barra inferior cambia entre Inicio/Favoritos/Perfil; el FAB y "Crear receta" abren el Generador
    y se vuelve con atrás.
  - El saludo y el avatar coinciden con el Perfil; con nombre/color ausente se degradan.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas de
UI de emulador. La lógica/estado de la Home (saludo, avatar, respaldo a semilla, bloque vacío, formato,
navegación) se cubre en JVM, que es lo que AGENTS.md exige "cuando sea posible"; el acabado visual y la
prueba manual offline son de la persona.

## Evidence To Capture

- Salida de `./gradlew test` con las seis suites nuevas, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Resultado manual en emulador sin red de los bloques 1, 3, 4 y 5, de la navegación inferior/FAB y de
  la ausencia de pantalla de carga (descripción; captura opcional).
- Inspección: `ui.home` y `ui.navigation` sin imports de Compose/Android en la lógica pura; sin
  dependencias nuevas; `domain.suggestions`/`data.suggestions` intactos.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **La Home renderiza 4 de los 5 bloques**: el bloque 2 (Búsqueda con IA) es de `ai-search` y la
  verificación de esta feature solo exige los bloques 1, 3, 4 y 5. No se pinta una píldora deshabilitada
  que simule una búsqueda inexistente.
- **Respaldo inmediato a semilla**: como el Lote puede resolverse de forma asíncrona, el carrusel usa
  `batch?.recipes ?: seedRecipes`. Cumple "poblada de inmediato, sin pantalla de carga" sin depender de
  que la caché ya exista en el primer arranque. El catálogo semilla es contenido empaquetado y siempre
  disponible offline (`seed-recipe-catalog`).
- **El Lote se consume tal cual**: `SuggestionBatchService.ensureBatch` se llama una vez al entrar en
  Home y `SuggestionBatchRepository.observe()` alimenta el carrusel; la política cache-first, la
  validación y el fallback a semilla ya están aceptados en `suggestion-batch-cache` y **no** se
  reimplementan.
- **Composición en `MainActivity`** (raíz sin DI): es donde ya vive `ProfileDatabase`; `SazonApp` deja de
  cargar la Taxonomía de forma perezosa y la recibe ya cargada, junto con las semillas y el servicio.
- **Navegación sin librería**: `MainTab`/`PushedScreen`/`MainNavigationState` puros + un `Saver` para
  `rememberSaveable`, siguiendo el precedente de `AppDestination`/`OnboardingPreferencesDraftSaver`.
  `navigation-compose` se puede adoptar después sin cambiar los contratos puros.
- **Favoritos/Perfil/Generador como `SectionPlaceholder`**: son features posteriores
  (`favorites`, `profile-screen`, `generator-ingredients-step`); el armazón los deja enrutados para que
  esas features sustituyan solo su contenido, igual que `HomePlaceholder` se sustituye aquí.
- **Bloque "Tus recetas" con datos vacíos hoy**: ni Favoritos ni "Abiertas hace poco" existen. La
  costura `HomeRecipes(saved, recentlyOpened)` recibe listas vacías desde `MainShell` y pinta el estado
  vacío de usuario nuevo; `favorites`/`recipe-detail-screen` la alimentarán. Los Filtros de resultados
  quedan para `local-result-filters`.
- **Saludo sin nombre = "Buenas tardes"**: literal de `docs/design/stitch-prompt.md` y
  `docs/domain-model.md`; con nombre se usa la franja horaria. Es un caso de perfil corrupto/migrado, no
  del flujo normal.
- **Sin imágenes ni iconos**: no hay assets de receta ni dependencia de iconos; el bloque de imagen es
  un degradado melocotón→oliva con un glifo de plato y los iconos de la barra son glifos Compose,
  coherente con el hallazgo ya documentado de `AvatarPreview`.
- **`matchScore` sin píldora**: su fórmula es de `results-screen`; en esta feature `matchScore` es
  `null` y la píldora no se pinta (`matchBadge(null) == null`).
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Franjas horarias del saludo**: `DESIGN.md`/`stitch-prompt` solo dan el ejemplo "Buenas tardes"; los
  cortes 5..11/12..20/21..4 son una elección documentada, ajustable como detalle visual sin cambiar el
  contrato puro (editar `HomeGreeting`).
- **Repaso visual** de barra inferior, FAB, glifos, contraste claro/oscuro y densidad del carrusel:
  queda para la persona tras el merge (un defecto visual se corrige con feature correctiva).
- **Carga de assets en el hilo principal** (`taxonomy.json` ~22 KB + `seed-recipes.json` ~20 KB) al
  crear `MainActivity`: aceptable para el MVP (ya anotado en onboarding); si Home/Perfil lo exigen, se
  moverá a un `ViewModel`/coroutine.
- **Unificación de bases Room**: se mantienen `sazon.db` y `suggestion_batch.db`; consolidarlas con
  migración probada sigue siendo deuda futura (ya anotada en `suggestion-batch-cache`).
- **Navegación de tarjetas**: abrir una receta desde el carrusel o "Tus recetas" es de
  `recipe-detail-screen`; en esta feature las tarjetas no navegan.

## Validator Checklist

- [ ] La Home renderiza los bloques 1, 3, 4 y 5 (no el bloque 2 de Búsqueda con IA) y la barra inferior
      de 3 destinos + FAB; las pantallas reales de Generador/Favoritos/Perfil no se implementan.
- [ ] Se mantiene en `ui.home`/`ui.navigation`/`ui`; no toca `domain.suggestions`, `data.suggestions`,
      `ProfileDatabase`, `data.profile/**`, `domain.profile/**` ni el build.
- [ ] La lógica pura (saludo, avatar, respaldo a semilla, bloque vacío, formato y navegación) está
      cubierta en JVM con casos negativos que no pasan en vacío.
- [ ] La Home se muestra poblada de inmediato con la semilla, sin pantalla de carga (escenario 1).
- [ ] El carrusel consume el Lote cacheado (`observe` + `ensureBatch`) sin reimplementar la política
      cache-first; sin red degrada a semilla (escenario 8).
- [ ] El bloque "Tus recetas" pinta sus grupos y su estado vacío de usuario nuevo (escenario 5).
- [ ] El saludo/avatar degradan correctamente con nombre o color ausentes (escenario 3).
- [ ] La barra inferior arranca en Inicio, cambia de pestaña y el FAB/héroe abren el Generador con
      vuelta atrás (escenarios 6 y 7).
- [ ] No se añaden dependencias (`app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios) y no
      se usan imágenes/iconos externos.
- [ ] `init.sh` queda en verde, la evidencia está en `feature_list.json`/`PROGRESS.md` y
      `docs/technical-discovery.md` queda coherente.
