# Feature Implementation Spec: Resumen de actividad en el Perfil

## Source Feature

- `id`: `activity-summary`
- `area`: `ui`
- `depends_on`: `["profile-screen", "favorites"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Añadir a la pestaña **Perfil** la sección **"Resumen"** (entre la cabecera de identidad y la sección
"Sugerencias") con **dos tarjetas de estadística compactas**: **recetas generadas** y **favoritas**.

`favoritesCount` es **derivado** por consulta sobre Favoritos (ya observados en `MainShell`).
`recipesGeneratedCount` es un **contador persistido** que se incrementa con cada receta `ai` aceptada
por el `RecipeValidator` en un evento de generación (Lote de sugerencias o flujo del Generador). No
puede derivarse del Lote: al refrescarse se reemplazan sus recetas `ai` y un conteo derivado bajaría
(`docs/domain-model.md` → `ActivitySummary`; `CONTEXT.md` → "Resumen de actividad").

## Non-Goals

- **No** se implementa la **Búsqueda con IA** (`ai-search`): solo se documenta que deberá reportar sus
  recetas `ai` aceptadas por el mismo seam (ver *Unknowns*).
- **No** se cambia el almacenamiento de Favoritos ni del Perfil: el conteo de favoritas es
  `favorites.size` sobre lo ya observado; no se añade una segunda fuente de verdad.
- **No** se implementa "Ajustes", "Borrar mis datos" ni ningún **reset** del contador.
- **No** se añaden dependencias ni iconos/imágenes: se mantiene la regla del repositorio (sin
  `material-icons`); los micro-iconos del export son opcionales según `DESIGN.md`.
- **No** se consolida `sazon.db` + `suggestion_batch.db` + `favorites.db`: deuda ya anotada; esta
  feature añade su **propia** base (mismo criterio que `favorites`/`suggestion-batch-cache`).
- **No** se toca la Taxonomía, `seed-recipes.json`, la Firma de preferencias ni el flujo cache-first
  (solo se **invoca** un callback nuevo tras validar).
- **No** se añade navegación, ni librería de DI, ni cambios de tema/tokens.

## Job Story

Cuando quiero saber cuánto he usado Sazón,
quiero ver cuántas recetas se han generado para mí y cuántas he guardado,
para tener una idea de mi actividad sin que el número de generadas baje al refrescarse las
sugerencias.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); consulta (no edita) el Resumen. No hay
  cuentas ni roles.
- **Desarrollador / agente**: implementa el contador persistido, la derivación de favoritas y la
  sección; verifica **lógica/estado en JVM** (contador monótono, no-descenso al refrescar, favoritas
  derivadas). El acabado visual y la prueba manual las revisa la persona (AGENTS.md no automatiza UI
  de emulador).

## Acceptance Scenarios

### Scenario 1: el Perfil muestra las dos tarjetas con los números

Given un contador persistido de **24** y **12** Favoritos,
When se abre la pestaña Perfil,
Then la sección "Resumen" aparece **entre la cabecera de identidad y la sección "Sugerencias"** con
dos tarjetas lado a lado: **24** "recetas generadas" y **12** "favoritas", con cifras tabulares.

### Scenario 2: usuario nuevo (variante de contadores pequeños)

Given un contador ausente (nunca se generó nada) y cero Favoritos,
When se abre el Perfil,
Then ambas tarjetas muestran **"0"** con sus etiquetas "recetas generadas" y "favoritas", sin
romperse ni salir del layout.

### Scenario 3: el contador sube con cada receta `ai` aceptada (verificación exigida)

Given un contador en `C`,
When un evento de generación produce `N` recetas `ai` **aceptadas** por el `RecipeValidator`,
Then el contador persistido pasa a `C + N`; una generación **sin** recetas `ai` aceptadas (0 aceptadas,
solo semilla, o generador no disponible) **no** lo altera.

### Scenario 4: el contador no baja al refrescar el Lote (verificación exigida)

Given un contador `C` tras un refresco del Lote que aceptó `N` recetas `ai`,
When se vuelve a refrescar el Lote (más recetas, menos recetas, o fallo del generador → solo semilla),
Then el contador es `>= C` en todos los casos: **nunca disminuye**, aunque el Lote reemplace sus
recetas `ai`.

### Scenario 5: el número de favoritas coincide con las guardadas

Given un usuario con `F` recetas guardadas como Favorito,
When se abre el Perfil,
Then la tarjeta "favoritas" muestra exactamente `F`, derivada de la lista observada (guardar o quitar
un favorito la actualiza), sin contador propio.

### Scenario 6: caso negativo — la caché no vuelve a contar

Given un Lote fresco al que sirve la política cache-first,
When `ensureBatch` devuelve `UseCached` y **no** llama al generador,
Then no se registra ninguna receta `ai` y el contador queda igual.

### Scenario 7: caso negativo — identidad y preferencias no alteran el Resumen

Given un cambio de nombre, de avatar o de una preferencia,
When se refresca o edita el Perfil,
Then el contador de generadas **no** cambia por esas acciones (solo cambia al aceptar recetas `ai`), y
el de favoritas solo cambia si se guarda o quita un Favorito.

## Repository Research

### Files Inspected

- `feature_list.json` — `activity-summary` (`area: ui`, `depends_on`, verificación y nota "el contador
  se persiste; el de favoritas se deriva"); features vecinas `profile-screen`, `manual-suggestions-refresh`,
  `favorites`.
- `PROGRESS.md` — `favorites` `accepted` (sesión 053); `activity-summary` es la siguiente candidata.
- `AGENTS.md` — verificación de UI por lógica/estado en JVM; acabado visual y pruebas de emulador a
  cargo de la persona; capturas obligatorias antes de implementar.
- `CONTEXT.md` (Resumen de actividad, Pantalla Perfil) y `docs/domain-model.md` (ActivitySummary;
  relaciones `UserProfile acumula ActivitySummary`).
- `DESIGN.md` — Core Screens 7 (Perfil incluye "Resumen de actividad (recetas generadas y favoritas)"),
  tokens (surface, formas 24dp, cifras tabulares, terracota/miel como acentos) y registro de la
  captura `activity-summary: perfil-resumen`.
- `docs/design/stitch/21-perfil-resumen.md` y `docs/design/concepts/activity-summary/README.md` —
  alcance del corte (dos tarjetas, variante 0/0, entre identidad y Sugerencias).
- Código: `ui/profile/ProfileScreen.kt` (orden actual: identidad → "Sugerencias" → "Mis
  preferencias"; `ProfileSuggestionsSection`), `ui/profile/ProfileSuggestionsSection.kt`,
  `ui/MainShell.kt` (observa `FavoriteRepository` y `SuggestionBatchRepository`; enruta
  `MainTab.PROFILE`), `ui/SazonApp.kt` (`onSaveProfile` por observación), `MainActivity.kt`
  (composición manual sin DI), `domain/suggestions/SuggestionBatchService.kt` (valida `ai` y persiste
  el Lote), `domain/generation/GenerationService.kt` (valida y devuelve `accepted`/`discarded`),
  `domain/favorites/FavoriteRepository.kt`, `data/favorites/{FavoritesDatabase,FavoriteDao,
  RoomFavoriteRepository,FavoriteEntity}.kt` (patrón de base propia de una fila/multi-fila), `ui/
  favorites/FavoriteRecipeCard.kt` (cifras tabulares con `"tnum"`), `ui/theme/*`.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — Room, KSP, Compose, coroutines y
  `kotlinx.serialization` ya presentes; sin `material-icons` ni `navigation-compose`.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: la lógica testeable en JVM y los `@Composable` solo pintan.
- **Base Room propia por feature** (`version = 1`, `exportSchema = false`, `Database.build(context)`),
  como `favorites.db` y `suggestion_batch.db`, para no migrar bases aceptadas.
- **Puerto + servicio de dominio inyectables**, con reloj/efectos por parámetro (p. ej.
  `FavoriteService(now)`); sin Android en `domain.*`.
- **Costuras por callback/observación**: `MainShell` ya observa repositorios y pasa datos por
  parámetro a las pantallas.
- **Cifras tabulares** con `FontFeatureSettings = "tnum"` (patrón `FavoriteRecipeCard`/`RecipeDetailScreen`).
- **Tokens** desde `com.sazon.app.ui.theme` (nunca hex sueltos); tarjetas `surface` + `SazonShapes.large`.
- **Tests JUnit 4** en `app/src/test/...`, con Robolectric `@Config(sdk = [33])` y base en memoria para
  Room; casos negativos que no pasan en vacío.
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- No existe ningún contador de recetas generadas: nada incrementa ni persiste `recipesGeneratedCount`.
- `GenerationService` y `SuggestionBatchService` validan y descartan recetas `ai`, pero **no reportan**
  cuántas aceptaron a ningún sumidero.
- `ProfileScreen` no recibe ni pinta el Resumen (hueco que dejaron `profile-screen` y
  `manual-suggestions-refresh`).

## Technical Approach

Estrategia: **contador persistido de una fila en Room + servicio puro + un callback de reporte en los
dos eventos de generación + sección Compose delgada**, sin dependencias nuevas.

### 1. Dominio puro — `com.sazon.app.domain.activity`

- `ActivitySummaryRepository` (puerto):
  - `fun observeRecipesGeneratedCount(): Flow<Int>`
  - `suspend fun recipesGeneratedCount(): Int`
  - `suspend fun incrementRecipesGenerated(delta: Int)`
- `ActivitySummaryService(repository)`:
  - `observeRecipesGeneratedCount()` / `recipesGeneratedCount()` delegan en el puerto.
  - `suspend fun recordAcceptedRecipes(recipes: List<Recipe>): Int`: **cuenta solo**
    `recipes.count { it.source == RecipeSource.AI }`; si el delta es `0` **no escribe**; devuelve el
    valor persistido. Es la única regla "solo `ai`, solo suma" y aquí vive el test de monotonía.

### 2. Persistencia — `com.sazon.app.data.activity`

- `ActivitySummaryEntity(id = 1, recipesGeneratedCount: Int)` — **una fila** (`@PrimaryKey id` fija),
  análoga a `user_profile`/`suggestion_batch`.
- `ActivitySummaryDao`:
  - `observeCount(): Flow<Int?>` y `getCount(): Int?` (`SELECT recipesGeneratedCount WHERE id = 1`).
  - `insertIfAbsent(row)` (`@Insert(IGNORE)`) + `add(delta)`
    (`UPDATE ... SET recipesGeneratedCount = recipesGeneratedCount + :delta WHERE id = 1`).
  - Un método `@Transaction suspend fun increment(delta)` que inserta la fila si falta y suma el
    delta de forma atómica (interface con cuerpo por defecto o pequeña `abstract class` si Room lo
    exige).
- `ActivitySummaryDatabase` (`activity_summary.db`, `version = 1`, `exportSchema = false`,
  `build(context)`), base **propia**.
- `RoomActivitySummaryRepository(dao)`: `observe` mapea `null → 0`; `incrementRecipesGenerated(delta)`
  ignora `delta <= 0` y si no llama a `increment(delta)`.

### 3. Reporte desde los eventos de generación (costura aditiva)

Se añade a `SuggestionBatchService` y `GenerationService` un parámetro **con valor por defecto**
(no rompe los constructores ni los tests existentes):

`private val onRecipesGenerated: suspend (List<Recipe>) -> Unit = {}`

- `SuggestionBatchService.generate`: en la rama de generación, tras validar las recetas `ai` con el
  `RecipeValidator` y obtener la lista de `ai` aceptadas, invoca `onRecipesGenerated(aceptadasAi)`.
  La rama `UseCached` **no** lo invoca (Scenario 6); el fallo del generador → lista vacía → no escribe.
- `GenerationService.generate`: tras validar, invoca `onRecipesGenerated(accepted)` (el filtro "solo
  `ai`" lo aplica el servicio de dominio).
- La composición (`MainActivity`) cablea `onRecipesGenerated = activitySummaryService::recordAcceptedRecipes`
  en ambos servicios. Así se cubren **hoy** los dos eventos que aceptan recetas `ai` (Lote y Generador).

### 4. UI — `com.sazon.app.ui.profile`

- `ActivitySummarySection(recipesGeneratedCount: Int, favoritesCount: Int)`: encabezado "Resumen" y
  una `Row` con **dos** tarjetas de igual ancho (`Modifier.weight(1f)`, gap `SazonSpacing.sm`), cada
  una `Surface(color = surface, shape = SazonShapes.large)` con padding `SazonSpacing.md`, número
  grande con cifras tabulares (`"tnum"`, peso Medium) y etiqueta en `bodyMedium`/`onSurfaceVariant`.
  Sin iconos (el repositorio no tiene `material-icons`; `DESIGN.md` los hace opcionales).
- `ProfileScreen` recibe `recipesGeneratedCount: Int` y `favoritesCount: Int` y pinta
  `ActivitySummarySection` como item **entre** `ProfileIdentityEditor` y `ProfileSuggestionsSection`.
- `MainShell` observa `activitySummaryRepository.observeRecipesGeneratedCount()` (`collectAsState(0)`)
  y deriva `favoritesCount = favorites.size` (la lista ya observada); los pasa a `ProfileScreen`.
- `SazonApp`/`MainActivity` propagan el repositorio/servicio de actividad.

### 5. Sin cambios de build

No se tocan `app/build.gradle.kts` ni `gradle/libs.versions.toml`: Room/Compose/coroutines ya están.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/domain/activity/ActivitySummaryRepository.kt`
- `app/src/main/java/com/sazon/app/domain/activity/ActivitySummaryService.kt`
- `app/src/main/java/com/sazon/app/data/activity/ActivitySummaryEntity.kt`
- `app/src/main/java/com/sazon/app/data/activity/ActivitySummaryDao.kt`
- `app/src/main/java/com/sazon/app/data/activity/ActivitySummaryDatabase.kt`
- `app/src/main/java/com/sazon/app/data/activity/RoomActivitySummaryRepository.kt`
- `app/src/main/java/com/sazon/app/ui/profile/ActivitySummarySection.kt`
- `app/src/test/java/com/sazon/app/domain/activity/ActivitySummaryServiceTest.kt`
- `app/src/test/java/com/sazon/app/domain/suggestions/SuggestionBatchActivityRecordingTest.kt`
- `app/src/test/java/com/sazon/app/data/activity/RoomActivitySummaryRepositoryTest.kt`

**Modificar**

- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatchService.kt` — callback
  `onRecipesGenerated` (aditivo) e invocación tras validar.
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` — mismo callback tras validar.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — dos parámetros nuevos + insertar la
  sección.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — observar el contador y pasar los dos números.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — propagar el repositorio/servicio de actividad.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer la base/repositorio/servicio y cablear
  los dos callbacks.
- `docs/technical-discovery.md` — registrar `ActivitySummary`.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `ProfileDatabase`/`data.profile/**`,
`data.favorites/**`, `data.suggestions/**`, la Taxonomía, `seed-recipes.json`, `ui.preferences/**` ni el
onboarding (de `DESIGN.md` solo se añade el registro de la captura; ver *Durable Documentation Impact*).

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (Core Screens 7 y tokens) es la **fuente de verdad**; las capturas son
  dirección visual. Prompt-corte: `docs/design/stitch/21-perfil-resumen.md`.
- Screens or states affected: pestaña Perfil, sección "Resumen" (estado poblado y variante 0/0),
  claro y oscuro.
- **UI captures (required)**, ya presentes (verificado):
  - `docs/design/concepts/activity-summary/perfil-resumen/screen.png`
  - `docs/design/concepts/activity-summary/perfil-resumen/code.html`
  - `docs/design/concepts/activity-summary/perfil-resumen/DESIGN.md`
  - Petición/estado: `docs/design/concepts/activity-summary/README.md` (completa).
- Visuales a respetar: dos tarjetas al **mismo ancho**, radio 24 (`SazonShapes.large`), superficie
  `surface`, sombra/elevación **suave** (sin bordes duros), padding interior 16 (`SazonSpacing.md`) y
  separación 12–16; **cifras tabulares** en el número grande (peso Medium) y etiqueta en texto
  secundario; **el terracota/miel no compiten con la acción primaria** (las tarjetas son neutras; la
  única acción terracota del Perfil sigue siendo la edición). Sin iconos/imágenes: el repositorio no
  arrastra `material-icons` y `DESIGN.md` marca los micro-iconos como opcionales (`*if* each card
  carries a tiny line icon`). La sección va **entre la cabecera de identidad y "Sugerencias"**.
- No se crean pantallas nuevas ni se toca el tema.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace una regla MUST/MUST NOT de nivel repo; el contador
  persistido y la derivación de favoritas ya están en `docs/domain-model.md`/`CONTEXT.md`.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — registrar `domain.activity`/`data.activity` (base propia
  `activity_summary.db`, una fila), el callback `onRecipesGenerated` de los dos servicios y la sección
  "Resumen" del Perfil.
- `docs/domain-model.md`: **not needed** — ya describe `ActivitySummary` (contador persistido +
  favoritas derivadas).
- `DESIGN.md`: **update (hallazgo de implementación)** — la captura `activity-summary/perfil-resumen`
  existía en `docs/design/concepts/activity-summary/`, pero **no** estaba en la lista de "Capturas
  aceptadas" de `DESIGN.md`; se añade la línea `- activity-summary: perfil-resumen` (el resto de
  `DESIGN.md` no cambia). `docs/design/concepts/`: **not needed** — las capturas ya están.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en planificación.

## Implementation Findings

- **Orden del callback en `SuggestionBatchService`**: el WIP previo colocó `onRecipesGenerated`
  **después** del reloj inyectable, de modo que la lambda de cola de los llamadores existentes
  (`SuggestionBatchService(...) { now }`) se ligaba al callback y no al reloj, rompiendo 4 tests de
  `SuggestionBatchServiceTest`. Se corrigió dejando `onRecipesGenerated` **antes** de
  `now: () -> Long` (el reloj sigue siendo el último parámetro); la costura queda de verdad aditiva.
  Relevante para `ai-search` si añade un tercer punto de reporte.
- **`DESIGN.md`**: ver el hallazgo de documentación durable arriba (registro de la captura).
- Recuento de tests: 301 preexistentes + 15 nuevos = **316**, 0 fallos (coincide con el plan).

## Implementation Plan

1. Crear `domain.activity` (`ActivitySummaryRepository` + `ActivitySummaryService`) con su test puro.
2. Crear `data.activity` (Entity/DAO/Database/Repository) y su test Robolectric en memoria.
3. Añadir el callback `onRecipesGenerated` a `SuggestionBatchService`/`GenerationService` e invocarlo
   tras validar (rama de generación).
4. Crear `ActivitySummarySection` y ampliar `ProfileScreen` (dos parámetros + item entre identidad y
   Sugerencias).
5. Cablear `MainShell` (observar contador, `favoritesCount = favorites.size`), `SazonApp` y
   `MainActivity` (composición y callbacks).
6. Crear el test de reporte del Lote y ejecutar `init.sh` (test + lint) en verde.
7. Actualizar `docs/technical-discovery.md`; registrar evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `ActivitySummaryRepository` y `ActivitySummaryService` (solo `source == ai`, delta 0 =
      no-op, devuelve el valor persistido).
- [ ] Escribir `ActivitySummaryServiceTest` (escenarios 3, 4 y 7) con casos negativos.
- [ ] Crear `ActivitySummaryEntity`/`ActivitySummaryDao`/`ActivitySummaryDatabase`/
      `RoomActivitySummaryRepository` (base propia, una fila, `increment` atómico).
- [ ] Escribir `RoomActivitySummaryRepositoryTest` (fila ausente → 0; incrementa y acumula; `observe`
      emite; delta <= 0 no escribe).
- [ ] Añadir `onRecipesGenerated` (default no-op) a `SuggestionBatchService` y `GenerationService` e
      invocarlo tras validar; rama `UseCached` y fallo del generador no lo disparan.
- [ ] Escribir `SuggestionBatchActivityRecordingTest` (refresco registra las `ai` aceptadas; caché no;
      segundo refresco no baja el contador).
- [ ] Crear `ActivitySummarySection` (dos tarjetas, `tnum`, tokens) y añadirla a `ProfileScreen` entre
      identidad y Sugerencias; ampliar la firma de `ProfileScreen`.
- [ ] Cablear `MainShell`/`SazonApp`/`MainActivity` (observación + composición + callbacks).
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: nuevos tests en verde y sin romper los 301
  existentes:
  - `ActivitySummaryServiceTest`: solo `source == ai` cuenta; lista vacía o solo `seed` ⇒ no escribe;
    acumula `C → C+N`; **nunca decrece**; `incrementRecipesGenerated(delta <= 0)` no escribe.
  - `RoomActivitySummaryRepositoryTest` (Robolectric en memoria): sin fila ⇒ `0`; `increment` crea la
    fila y acumula; `observe` emite el valor nuevo; `delta <= 0` no cambia.
  - `SuggestionBatchActivityRecordingTest` (generador falso + puertos en memoria): un refresco registra
    `N` `ai` aceptadas; la segunda llamada servida por caché (`UseCached`) no registra; un segundo
    refresco (o fallo del generador → solo semilla) **no baja** el contador (verificación exigida).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (se mantienen los 13 preexistentes).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `domain.activity` sin imports `android.*`/`androidx.*`; `data.favorites`/`data.profile`/
  `data.suggestions` sin cambios; `app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios;
  `SuggestionBatchService`/`GenerationService` conservan su comportamiento cuando el callback es el
  no-op (tests existentes intactos).
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - El número de **favoritas** coincide con las recetas guardadas (verificación exigida) y se actualiza
    al guardar/quitar.
  - Tras generar recetas (Generador o refresco del Lote), "recetas generadas" sube y **no baja** al
    refrescar sugerencias ni al reabrir la app (persistencia).
  - Comparar el acabado con `docs/design/concepts/activity-summary/perfil-resumen/` (claro/oscuro y
    variante 0/0).

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y los tests nuevos corren en JVM/Robolectric sin emulador.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; la lógica verificable (contador monótono, no-descenso al refrescar, favoritas derivadas) se
cubre en JVM.

## Evidence To Capture

- Salida de `./gradlew test` con `ActivitySummaryServiceTest`,
  `SuggestionBatchActivityRecordingTest` y `RoomActivitySummaryRepositoryTest`, nº de tests y 0 fallos,
  y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `domain.activity` puro; sin cambios en `data.favorites`/`data.profile`/
  `data.suggestions` ni en build/lockfile; callbacks con default no-op.
- Resultado manual en emulador de la verificación exigida (favoritas = recetas guardadas; el contador
  no baja al refrescar) y del acabado contra la captura (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **El contador cuenta eventos de generación, no recetas distintas**: cada receta `ai` aceptada por el
  `RecipeValidator` suma 1, aunque ya exista otra con el mismo título; es lo que exige
  `docs/domain-model.md` y lo que garantiza que el número no baje al refrescar (el Lote reemplaza sus
  `ai`). El refresco automático del Lote al abrir la app también suma: es una generación de IA real.
- **Base Room propia y una fila** (`activity_summary.db`): evita migrar `sazon.db`/bases aceptadas,
  mismo criterio que `favorites`/`suggestion-batch-cache`; la consolidación queda como deuda.
- **Callback con default no-op** en los dos servicios (en vez de inyectar el servicio de actividad):
  mantiene `domain.suggestions`/`domain.generation` puros y **sin nueva dependencia de dominio**, no
  rompe constructores ni tests existentes, y deja la regla "solo `ai`" en un único sitio
  (`ActivitySummaryService`).
- **`favoritesCount = favorites.size`**: `MainShell` ya observa la lista de Favoritos; derivar el
  conteo de ahí evita una segunda fuente de verdad o una consulta COUNT nueva (coherente con
  `manual-suggestions-refresh`, que reutiliza el Lote observado).
- **Refresh solo en la rama de generación**: la caché (`UseCached`) no reporta, para no volver a contar
  el mismo Lote (Scenario 6).
- **Sin iconos ni imágenes**: el repositorio no arrastra `material-icons` y `DESIGN.md` marca los
  micro-iconos como opcionales; se pintan solo tarjetas y texto con tokens.
- **Sin reset**: "Borrar mis datos" está fuera del MVP; el contador es solo aditivo.

### Unknowns

- **Búsqueda con IA (`ai-search`)**: aún no existe. Cuando se implemente, deberá reportar sus recetas
  `ai` aceptadas por el mismo seam (su servicio de generación deberá exponer/consumir el callback
  `onRecipesGenerated` o llamar a `ActivitySummaryService.recordAcceptedRecipes`). Se documenta como
  follow-up; **no** se implementa aquí.
- **`@Transaction` en DAO de interfaz**: si Room rechaza el método con cuerpo por defecto, la
  alternativa es un DAO `abstract class` con el mismo contrato; no cambia la lógica pura.
- **Crecimiento semántico del contador**: incluye generaciones automáticas del Lote (no solo acciones
  explícitas); es la lectura literal del dominio y queda como decisión de producto revisable sin
  cambiar el contrato (solo el punto de reporte).
- **`Tabular figures`**: se reutiliza la convención `FontFeatureSettings("tnum")` ya usada en
  Favoritos/Detalle; el tamaño/peso exacto del número es detalle visual que revisa la persona.
- **Concurrencia del incremento**: `UPDATE ... + delta` es atómico en SQLite; no hay escritores
  concurrentes en la app (un único usuario), así que no se añade control de versión.

## Validator Checklist

- [ ] La implementación se limita a la sección "Resumen" y a la fontanería del contador; sin Búsqueda
      con IA, sin Ajustes, sin "Borrar mis datos" y sin reset.
- [ ] Los escenarios 1–7 pasan; la lógica está cubierta en JVM con casos negativos que no pasan en vacío
      (solo `ai`; delta 0 no-op; caché no reporta; el contador nunca decrece).
- [ ] `favoritesCount` se deriva de la lista observada (`favorites.size`); no hay contador propio.
- [ ] El contador se persiste en una base propia (`activity_summary.db` v1, una fila) y sobrevive a
      reaperturas.
- [ ] `SuggestionBatchService`/`GenerationService` conservan su comportamiento con el callback no-op;
      no se toca la política cache-first ni la validación.
- [ ] La sección aparece entre la cabecera de identidad y "Sugerencias"; dos tarjetas al mismo ancho con
      tokens de `SazonTheme`, cifras tabulares y sin hex sueltos ni iconos.
- [ ] Las capturas obligatorias de `docs/design/concepts/activity-summary/perfil-resumen/` existen y la
      implementación las sigue (`DESIGN.md` manda ante contradicciones).
- [ ] Sin dependencias nuevas: `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian.
- [ ] `feature_list.json`/`PROGRESS.md` quedan actualizados con evidencia verificable y
      `docs/technical-discovery.md` coherente.
