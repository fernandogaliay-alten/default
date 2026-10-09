# Feature Implementation Spec: Favoritos disponibles sin conexión

## Source Feature

- `id`: `favorites`
- `area`: `ui`
- `depends_on`: `["recipe-detail-screen", "profile-persistence"]` (las dos `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`
- Nota de la feature: "El favorito persiste una copia de la Receta, porque el lote de sugerencias
  puede refrescarse y perderla." Verificación exigida: prueba manual de **guardar una receta generada
  por IA y verla tras reiniciar sin conexión**, y el **estado vacío** cuando no hay favoritos.
- Capturas obligatorias (gate de UI) **ya presentes**: ver `Visual Design Impact`.

## Goal

Entregar la pestaña **Favoritos** (`MainTab.FAVORITES`) real, que sustituye al
`SectionPlaceholder("Favoritos")`, y cerrar el lazo de guardado: el Usuario local **marca y
desmarca** una receta como Favorito desde el Detalle de receta (el corazón del hero y
"Guardar"/"Guardada" de la barra inferior, hoy costuras no-op de `recipe-detail-screen`) y la
**consulta sin conexión** desde su pestaña y desde el bloque "Tus recetas → Guardadas" de la Home.

La pieza central es que **el Favorito persiste una copia completa de la `Recipe`** (no solo un id),
porque el Lote de sugerencias puede refrescarse y perder la receta `ai` original. La lógica de
marcado (`toggle`) y la de la lista (conteo, búsqueda por texto y orden) son **puras y testeables en
JVM**; la persistencia sigue el patrón Room ya establecido (base propia de una tabla multi-fila); la
capa Compose es delgada y reutiliza los tokens, el degradado/glifo y el corazón del repositorio.

## Non-Goals

- **No** se implementa **"Abiertas hace poco"** (`recentlyOpenedRecipes`): no hay ninguna feature que
  registre `lastOpenedAt`; el grupo de la Home sigue recibiendo `emptyList()` y queda documentado.
- **No** se implementa el **Resumen de actividad** (recetas generadas / favoritas): es la feature
  `activity-summary` (consumirá el conteo derivado de Favoritos).
- **No** se implementa el **modo cocina** con temporizadores: "Empezar a cocinar" sigue siendo una
  costura no-op (`recipe-detail-screen`; fuera del MVP).
- **No** se implementa la **Búsqueda con IA** (`ai-search`) ni el proveedor real
  (`real-ai-provider`); sin red.
- **No** se persisten recetas fuera del Favorito: ni un caché nuevo de recetas, ni "últimas abiertas",
  ni el contador de actividad. El Lote de sugerencias **no** se toca.
- **No** se toca `domain.recipe`, `domain.suggestions`, `domain.profile`, `data.profile/**`,
  `data.suggestions/**`, `ProfileDatabase`/`SuggestionBatchDatabase`, la Taxonomía,
  `seed-recipes.json` ni `init.sh` (más allá de **consumir** `Recipe`, `RecipeJson`, `Taxonomy` y el
  escalado del Detalle).
- **No** se añaden librería de navegación, iconos (`material-icons-*`), imágenes (Coil) ni DI; **sin
  dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos).

## Job Story

Cuando una receta me convence,
quiero guardarla con un toque y volver a encontrarla después, incluso sin conexión y aunque las
sugerencias se hayan refrescado,
para tener siempre a mano las recetas que me interesan y decidir qué cocinar sin depender de la red.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002). Marca/desmarca favoritos desde el
  Detalle y desde la pestaña; los consulta offline. No hay cuentas, roles ni compartición.
- **Desarrollador / agente**: cubre en JVM el `toggle` (copia + idempotencia), la persistencia
  multi-fila, el conteo, la búsqueda y el orden; el acabado visual (lista, chips, corazón, swipe,
  estado vacío, claro/oscuro) lo revisa la persona en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: no hay red. Todo el acceso a Favoritos lee la base local; el Favorito es independiente
  del Lote de sugerencias y no lo modifica.

## Acceptance Scenarios

### Scenario 1: guardar desde el Detalle persiste una copia (verificación exigida)

Given una receta generada por IA (`source = ai`, p. ej. una del flujo del Generador) abierta en el
Detalle con el corazón vacío,
When el usuario pulsa el corazón (o "Guardar"),
Then `isSaved` pasa a `true`, el corazón se rellena y la barra muestra "Guardada", y se escribe **una
fila** de Favorito con una **copia completa de la `Recipe`** y `savedAt = now` (la copia normaliza
`matchScore` a `null`, por ser derivado y no persistido).

### Scenario 2: quitar desde el Detalle no toca el Lote (verificación exigida)

Given una receta ya guardada,
When el usuario vuelve a pulsar el corazón/"Guardada",
Then se elimina su fila de Favorito, `isSaved` vuelve a `false` y el Lote de sugerencias **no cambia**
(si la receta sigue en el Lote, el corazón simplemente se desmarca; edge case de `docs/domain-model.md`).

### Scenario 3: la copia sobrevive al refresco del Lote (verificación exigida)

Given un Favorito de una receta `ai` que ya **no** está en el Lote (el Lote se refrescó y la perdió),
When se abre la pestaña Favoritos,
Then la receta **sigue listándose** porque el Favorito guarda su propia copia y no depende del Lote.

### Scenario 4: la pestaña Favoritos lista y funciona sin conexión (verificación exigida)

Given favoritos persistidos,
When se abre la pestaña Favoritos (incluido tras **reiniciar la app sin conexión**),
Then se listan las recetas favoritas con la cabecera "Mis recetas guardadas", la línea de conteo y la
nota "Disponibles sin conexión", **sin red y sin pantalla de carga**.

### Scenario 5: estado vacío cuando no hay favoritos (verificación exigida)

Given que no hay ningún Favorito,
When se abre la pestaña Favoritos,
Then se muestra la ilustración de marcador, el titular "Aún no has guardado ninguna receta", el cuerpo
"Pulsa el corazón de cualquier receta para tenerla siempre a mano." y el botón primario
"Explorar recetas" (`FavoritesList` sobre lista vacía; el conteo es "0 recetas").

### Scenario 6: búsqueda y orden locales

Given varios favoritos,
When el usuario escribe en el buscador o elige un chip de orden ("Añadidas recientemente" · "Tiempo" ·
"Dificultad"),
Then la lista se **filtra por título** (sin distinguir mayúsculas ni acentos; una consulta en blanco
devuelve todo) y se **ordena** por recientes (`savedAt` desc), tiempo (`totalMinutes` asc) o dificultad
(orden de `taxonomy.difficulties`); "Explorar recetas" (estado vacío) lleva a **Inicio**.

### Scenario 7: quitar desde la lista

Given la lista poblada,
When el usuario pulsa el corazón de una tarjeta (o la arrastra con la acción "Eliminar"),
Then esa receta sale de la lista y de la persistencia; si era la última, aparece el estado vacío.

### Scenario 8: abrir el Detalle desde Favoritos

Given la lista de Favoritos,
When el usuario toca una tarjeta,
Then se abre el `RecipeDetailScreen` (overlay de `MainShell`) con el corazón **relleno** ("Guardada");
al pulsar "Atrás" vuelve a Favoritos sin perder la pestaña.

### Scenario 9: "Tus recetas → Guardadas" de la Home refleja los favoritos

Given favoritos persistidos,
When se abre la Home,
Then el grupo **"Guardadas"** del bloque "Tus recetas" muestra las recetas favoritas (offline); con
cero favoritos el bloque mantiene su estado vacío de usuario nuevo.

### Scenario 10: persistencia multi-fila e idempotencia (JVM)

Given dos favoritos distintos guardados y releídos,
Then coexisten (**2 filas**), volver a guardar el **mismo** `recipeId` **no** duplica (sigue 1 fila),
`remove(recipeId)` borra **solo** esa receta y `clear()` deja la lectura vacía; `observe()` emite los
cambios.

## Repository Research

### Files Inspected

- `feature_list.json` — definición de `favorites` (`area: ui`, `depends_on`, verificación manual y
  nota de la copia) y features vecinas (`activity-summary` depende de ésta; `ai-search`).
- `PROGRESS.md` — última aceptada `recipe-detail-screen` (síntesis del corte vertical completo);
  camino `init.sh` (`./gradlew test` + `./gradlew lint`); regla de no automatizar UI de emulador;
  política de capturas por pantalla (PR #37).
- `AGENTS.md` — verificación de UI por lógica/estado en JVM cuando sea posible; capturas obligatorias
  antes de implementar; una feature por sesión.
- `CONTEXT.md` — "Favorito" ("se guarda en local y es independiente de si su `source` es `seed` o
  `ai`"), "Bloque 'Tus recetas'", "Filtros de resultados", "Receta guardada" (usar **Favorito**).
- `docs/build-brief.md` — MVP slice 7 "Favoritos persistidos en local y consultables sin conexión";
  Success Criteria "con la red desconectada … Favoritos siguen siendo utilizables"; Validation Plan
  (guardar favorito → ver favorito sin conexión).
- `docs/domain-model.md` — `Favorite` ("Persiste `recipeId` + una copia de la Receta"; "independiente
  del Lote"), relación `Favorite` → 1 `Recipe`, ciclo `en caché → favorita → no favorita`, y edge case
  "Favorito eliminado que sigue en el lote: el lote no cambia; el corazón se desmarca"; `matchScore`
  "derivado y **no persistido**".
- `docs/technical-discovery.md` — "Favoritos | Room | Disponibilidad offline garantizada"; `data.profile`
  y `data.suggestions` como precedentes de persistencia Room; `MainShell`/`ui.home` (barra de 3
  destinos, bloque "Tus recetas" con costura `HomeRecipes` hoy vacía) y el Detalle hospedado como
  overlay con las costuras no-op `isSaved`/`onToggleSave`.
- `docs/risks-and-open-questions.md` — sin preguntas abiertas que bloqueen; ADR 0002 (todo el estado en
  el dispositivo).
- `DESIGN.md` — Core Screen 6 (**Favoritos**: "búsqueda, orden, lista, estado vacío con nota
  'Disponibles sin conexión'"), `RecipeListCard` (miniatura 96x96, texto a la derecha, corazón a la
  derecha), chips tipo píldora, tokens y "no inventar pantallas". `Generated Concept Images`: registrar
  la captura aceptada `favorites/favoritos`.
- `docs/design/stitch/17-favoritos.md` y `docs/design/stitch-prompt.md` (sección F) — microcopy y
  layout exactos: cabecera "Mis recetas guardadas" + conteo, buscador + chips de orden + toggle
  lista/rejilla, lista de `RecipeListCard` con corazón y swipe "Eliminar", nota "Disponibles sin
  conexión", estado vacío con "Explorar recetas", barra inferior persistente con Favoritos activo.
- `docs/design/concepts/favorites/README.md` y `favoritos/` — **capturas obligatorias** ya presentes
  (ver `Visual Design Impact`).
- `docs/specs/recipe-detail-screen.md` — las costuras `isSaved`/`onToggleSave` documentadas como "de
  `favorites`"; `ui.detail` y el `HeartGlyph`; el Detalle como overlay en `MainShell`.
- `docs/specs/home-offline-blocks.md` y `docs/specs/local-result-filters.md` — `HomeRecipes(saved,
  recentlyOpened)` y `filteredBy`; "favorites/recipe-detail-screen las alimentarán"; el bloque
  "Tus recetas" y su estado vacío ya existen.
- `docs/specs/suggestion-batch-cache.md` — patrón Room de **base propia** para no migrar
  `ProfileDatabase` (migración no ejercitable en JVM); `RecipeJson.encodeList`/`parseList`.
- `docs/specs/profile-persistence.md` — patrón `data.profile` (entidad/DAO/mappers/repositorio/fábrica
  `build(context)`), test Room en JVM con Robolectric `@Config(sdk = [33])` y base en memoria.
- Código: `ui/MainShell.kt` (enruta `MainTab.FAVORITES` a `SectionPlaceholder`; hospeda el Detalle
  overlay con `isSaved = false`/`onToggleSave = {}`; pasa `savedRecipes`/`recentlyOpenedRecipes` a
  `HomeRecipes`), `ui/SazonApp.kt` (pasa `emptyList()` como costura), `ui/SectionPlaceholder.kt` (solo
  lo usa Favoritos), `ui/detail/RecipeDetailScreen.kt` (`isSaved`/`onToggleSave`, `HeartGlyph`),
  `ui/home/{HomeRecipes,HomeScreen,RecipeCard,RecipeCardFormat}.kt`,
  `ui/results/ResultRecipeCard.kt`, `domain/recipe/{Recipe,RecipeJson}.kt`,
  `data/profile/{ProfileEntity,ProfileDao,ProfileMappers,RoomProfileRepository,ProfileDatabase}.kt`,
  `data/suggestions/SuggestionBatchDatabase.kt`, `MainActivity.kt`,
  `app/src/test/.../data/profile/RoomProfileRepositoryTest.kt`.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — Room 2.8.5, KSP, corrutinas, Robolectric,
  `androidx.test:core` y `kotlinx.serialization` ya presentes; **sin** iconos ni imágenes.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: `toggle`, el conteo, la búsqueda y el orden viven en Kotlin
  sin imports de Compose/Android y se testean en JVM; los `@Composable` solo pintan.
- **Costura dominio / datos**: lógica pura en `com.sazon.app.domain.<área>` (sin `android.*`),
  persistencia en `com.sazon.app.data.<área>` sobre Room.
- **Base de datos propia + mapeo `*Mappers.kt`**: entidad con el modelo serializado a JSON por
  `kotlinx.serialization`; `RecipeJson` ya expone `encode`/`parse` de una receta.
- **Reloj inyectable** (`now: () -> Long`) para determinismo, como en `SuggestionBatchService` y
  `OnboardingProfileBuilder`.
- **Composición a mano en `MainActivity`** (sin DI), como el Perfil y el Lote.
- **Costuras ya existentes**: `RecipeDetailScreen(isSaved, onToggleSave, …)`, el overlay `detailRecipe`
  de `MainShell` y `HomeRecipes(saved, recentlyOpened)`. Se **cablean**, no se reimplementan.
- **Tokens y sin assets**: colores/espaciado/formas desde `ui.theme` (nunca hex sueltos); degradado
  melocotón→oliva y glifos/corazón dibujados con Compose (mismo criterio que `RecipeCard`/
  `RecipeDetailScreen`).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, casos negativos que no pasan en
  vacío; Room con Robolectric `@Config(sdk = [33])` y base en memoria.
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); sin procesos largos.

### Current Gaps

- `MainTab.FAVORITES` pinta `SectionPlaceholder("Favoritos")`: no hay pantalla, ni lista, ni buscador,
  ni chips de orden, ni estado vacío.
- El corazón/"Guardar" del Detalle y el "Empezar a cocinar" son costuras **no-op**: no se persiste
  ningún Favorito.
- No existe ningún modelo, puerto, servicio ni persistencia de Favoritos.
- El bloque "Tus recetas → Guardadas" de la Home recibe `emptyList()`: nunca se puebla.
- `SectionPlaceholder` solo lo usa la pestaña Favoritos; al sustituirla queda **sin referencias**.

## Technical Approach

Estrategia: **dominio puro `domain.favorites` + persistencia Room en `data.favorites` (base propia) +
lógica de presentación pura `ui.favorites` + capa Compose delgada**, sin tocar el dominio aceptado ni
añadir dependencias.

### 1. Dominio puro — `com.sazon.app.domain.favorites`

- `Favorite(recipe: Recipe, savedAt: Long)` con `val recipeId: String get() = recipe.id`.
- `interface FavoriteRepository`:
  - `suspend fun get(): List<Favorite>`
  - `fun observe(): Flow<List<Favorite>>`
  - `suspend fun isFavorite(recipeId: String): Boolean`
  - `suspend fun save(favorite: Favorite)`
  - `suspend fun remove(recipeId: String)`
  - `suspend fun clear()`
- `class FavoriteService(repository, now: () -> Long = System::currentTimeMillis)`:
  - `suspend fun toggle(recipe: Recipe): Boolean` — si `isFavorite(recipe.id)` → `remove(id)` y
    devuelve `false`; si no → `save(Favorite(recipe = recipe.copy(matchScore = null), savedAt = now()))`
    y devuelve `true`. **Normaliza `matchScore` a `null`**: es derivado y `docs/domain-model.md` fija
    que **no se persiste**.
  - `suspend fun isFavorite(recipeId: String): Boolean = repository.isFavorite(recipeId)`.

### 2. Persistencia Room — `com.sazon.app.data.favorites`

Base de datos **propia** (`favorites.db`, versión 1) con una tabla **multi-fila** una por Favorito:

```kotlin
@Entity(tableName = "favorite_recipe")
data class FavoriteEntity(
    @PrimaryKey val recipeId: String,
    val savedAt: Long,
    val recipeJson: String,
)

@Dao interface FavoriteDao {
    @Query("SELECT * FROM favorite_recipe ORDER BY savedAt DESC") fun observe(): Flow<List<FavoriteEntity>>
    @Query("SELECT * FROM favorite_recipe ORDER BY savedAt DESC") suspend fun getAll(): List<FavoriteEntity>
    @Query("SELECT * FROM favorite_recipe WHERE recipeId = :recipeId") suspend fun getById(recipeId: String): FavoriteEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: FavoriteEntity)
    @Query("DELETE FROM favorite_recipe WHERE recipeId = :recipeId") suspend fun delete(recipeId: String)
    @Query("DELETE FROM favorite_recipe") suspend fun deleteAll()
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    companion object { fun build(context: Context): FavoritesDatabase = /* "favorites.db" */ }
}
```

- `FavoritesMappers.kt`: `FavoriteEntity.toDomain()` / `Favorite.toEntity()` usando
  `RecipeJson.parse`/`encode` (ya existentes); `recipeId` = `recipe.id`.
- `RoomFavoriteRepository(dao)` implementa el puerto (`observe` delega en el DAO; `isFavorite` =
  `getById(id) != null`).
- Motivo de la **base propia** (Decisión): añadir la entidad a `ProfileDatabase` obligaría a subir a
  versión 2 y a una **migración no ejercitable en tests JVM**; mismo criterio que
  `SuggestionBatchDatabase` en `suggestion-batch-cache`.

### 3. Lógica de presentación pura — `com.sazon.app.ui.favorites`

- `enum class FavoritesSort(val label: String, val key: String)`:
  `RECENT("Añadidas recientemente", "recent")`, `TIME("Tiempo", "time")`,
  `DIFFICULTY("Dificultad", "difficulty")`, con `fromKey(key)` (clave desconocida → `RECENT`).
- `enum class FavoritesViewMode(val key: String)` `{ LIST("list"), GRID("grid") }` con `fromKey`.
- `object FavoritesList`:
  - `fun visible(favorites: List<Favorite>, query: String, sort: FavoritesSort, taxonomy: Taxonomy): List<Favorite>`
    — filtra por `title` normalizado (minúsculas + sin acentos con `java.text.Normalizer`, recortado;
    consulta en blanco = todo) y ordena: `RECENT` por `savedAt` desc, `TIME` por `totalMinutes` asc,
    `DIFFICULTY` por el índice de `difficultyId` en `taxonomy.difficulties` (respaldo estable).
  - `fun countLabel(count: Int): String` = `"1 receta"` / `"$count recetas"`.

### 4. Capa Compose — `com.sazon.app.ui.favorites`

- `FavoritesScreen(favorites: List<Favorite>, taxonomy, onOpenRecipe: (Recipe) -> Unit,
  onToggleFavorite: (Recipe) -> Unit, onExplore: () -> Unit, modifier)`:
  - `LazyColumn` sobre `colorScheme.background` con: cabecera "Mis recetas guardadas" + `countLabel`;
    `OutlinedTextField` de búsqueda; `FavoritesSortRow` (fila de `FilterChip` con `FavoritesSort.entries`)
    y toggle lista/rejilla; cuerpo poblado (lista de `FavoriteRecipeCard` o rejilla 2 columnas) con la
    nota discreta **"Disponibles sin conexión"**; o **estado vacío** (glifo de marcador dibujado con
    Compose, titular, cuerpo y `Button` "Explorar recetas" → `onExplore`).
  - Estado `query`/`sort`/`viewMode` con `rememberSaveable` + `Saver` (patrón de `HomeScreen`), sin
    `remember` de la lista (llega observada).
- `FavoriteRecipeCard(favorite, taxonomy, onClick, onRemove)` (`RecipeListCard` del `DESIGN.md`):
  miniatura **degradada** 96x96 redondeada con glifo de plato, título (2 líneas), `RecipeCardFormat.metaLine`
  en cifras tabulares y el **corazón** a la derecha (relleno; `onRemove`). El corazón se dibuja con
  Compose (glifo propio de `ui.favorites`, mismo criterio que el `HeartGlyph` del Detalle: el
  repositorio no tiene iconos). Tap en la tarjeta (`onClick`) abre el Detalle.
  - Swipe-to-delete con acción visible "Eliminar" (captura) vía `SwipeToDismissBox` de Material 3 (sin
    dependencia nueva); `onRemove` es la operación pura y lo que se testea; el gesto es visual/revisión
    de la persona.

### 5. Integración (cablear las costuras, sin librería de navegación)

- `MainActivity`: componer `FavoritesDatabase.build(applicationContext)` → `RoomFavoriteRepository` +
  `FavoriteService` y pasarlos por `SazonApp`.
- `SazonApp`: recibir `favoriteRepository`/`favoriteService` y pasarlos a `MainShell`; deja de pasar
  `savedRecipes`/`recentlyOpenedRecipes` (los deriva `MainShell` de los favoritos).
- `MainShell`:
  - `val favorites by favoriteRepository.observe().collectAsState(initial = emptyList())`.
  - `MainTab.FAVORITES -> FavoritesScreen(favorites, taxonomy, onOpenRecipe = { detailRecipe = it },
    onToggleFavorite = { scope.launch { favoriteService.toggle(it) } },
    onExplore = { navigation = navigation.selectTab(MainTab.HOME) })`.
  - Home: `HomeRecipes(saved = favorites.map { it.recipe }, recentlyOpened = emptyList())`.
  - Detalle overlay: `isSaved = favorites.any { it.recipe.id == recipe.id }` y
    `onToggleSave = { scope.launch { favoriteService.toggle(recipe) } }` (sustituyen los `false`/`{}`).
- Eliminar `ui/SectionPlaceholder.kt` (queda **sin referencias** al sustituirse su único uso).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/domain/favorites/Favorite.kt` — modelo con `recipeId`.
- `app/src/main/java/com/sazon/app/domain/favorites/FavoriteRepository.kt` — puerto.
- `app/src/main/java/com/sazon/app/domain/favorites/FavoriteService.kt` — `toggle`/`isFavorite` puros.
- `app/src/main/java/com/sazon/app/data/favorites/FavoriteEntity.kt` — tabla `favorite_recipe`.
- `app/src/main/java/com/sazon/app/data/favorites/FavoriteDao.kt` — consultas.
- `app/src/main/java/com/sazon/app/data/favorites/FavoritesDatabase.kt` — `favorites.db`, versión 1.
- `app/src/main/java/com/sazon/app/data/favorites/FavoritesMappers.kt` — entidad ↔ dominio.
- `app/src/main/java/com/sazon/app/data/favorites/RoomFavoriteRepository.kt` — implementación.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesSort.kt` — orden + `fromKey`.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesViewMode.kt` — lista/rejilla.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesList.kt` — `visible`/`countLabel` puros.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — pantalla (cabecera, buscador,
  chips, toggle, lista/rejilla, nota, estado vacío, `Saver`).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — tarjeta horizontal + corazón.
- `app/src/test/java/com/sazon/app/domain/favorites/FavoriteServiceTest.kt` — escenarios 1-2 y 10 (toggle).
- `app/src/test/java/com/sazon/app/ui/favorites/FavoritesListTest.kt` — escenarios 5-6 (conteo, orden, búsqueda).
- `app/src/test/java/com/sazon/app/data/favorites/RoomFavoriteRepositoryTest.kt` — escenario 10 (Room en JVM).

**Modificar**

- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer `FavoritesDatabase`/repositorio/servicio.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — recibir y pasar las dependencias de Favoritos.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — enrutar `MainTab.FAVORITES` a `FavoritesScreen`,
  alimentar `HomeRecipes.saved` y cablear `isSaved`/`onToggleSave` del Detalle.
- `docs/technical-discovery.md` — ubicar `domain.favorites` + `data.favorites` (base propia, tabla
  multi-fila), `ui.favorites` y la integración (Home "Guardadas" + corazón del Detalle).
- `DESIGN.md` — registrar la captura aceptada `favorites/favoritos`.
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar** (estado y evidencia), no en esta sesión.

**Eliminar**

- `app/src/main/java/com/sazon/app/ui/SectionPlaceholder.kt` — su único uso (Favoritos) desaparece.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain.recipe`,
`domain.suggestions`, `domain.profile`, `data.profile/**`, `data.suggestions/**`, `ProfileDatabase`,
`SuggestionBatchDatabase`, `ui.detail/**`, `ui.navigation/MainNavigationState.kt`, la Taxonomía,
`seed-recipes.json` ni `init.sh`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (Core Screen 6, `RecipeListCard`, chips, tokens) +
  `docs/design/stitch/17-favoritos.md` / `docs/design/stitch-prompt.md` (sección F).
- Screens or states affected: pestaña Favoritos **poblada** (cabecera "Mis recetas guardadas" + conteo,
  buscador, chips de orden, toggle lista/rejilla, `FavoriteRecipeCard` con corazón y swipe "Eliminar",
  nota "Disponibles sin conexión") y **estado vacío** (glifo de marcador, titular "Aún no has guardado
  ninguna receta", cuerpo "Pulsa el corazón de cualquier receta para tenerla siempre a mano.", botón
  "Explorar recetas"); corazón del Detalle relleno/vacío; bloque "Guardadas" de la Home; claro y oscuro.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonShapes` (16/24), `SazonPillShape`
  (chips/botones), `MaterialTheme.colorScheme.{primary, onPrimary, surface, onSurface, surfaceVariant,
  onSurfaceVariant, outlineVariant}`; degradado `SazonPrimaryContainer`→`SazonSecondaryContainer` como
  miniatura; cifras tabulares (`"tnum"`) en el conteo y la meta; terracota = única acción principal;
  objetivos ≥48dp; tipografía dinámica (scroll, nunca recortar).
- Sin iconos ni imágenes externas: el marcador del estado vacío, el glifo de plato y el corazón se
  dibujan con Compose (mismo criterio ya documentado en `RecipeCard`/`RecipeDetailScreen`).
- **Capturas de referencia (obligatorias, ya presentes).** Existe una única captura de Stitch de la
  pestaña en `docs/design/concepts/favorites/`:
  - `docs/design/concepts/favorites/favoritos/screen.png` (export de la pantalla; claro y oscuro),
  - `docs/design/concepts/favorites/favoritos/code.html` (código del export),
  - `docs/design/concepts/favorites/favoritos/DESIGN.md` (notas del sistema del export),
  - petición/ficha: `docs/design/concepts/favorites/README.md`.
  Stitch no generó variantes por estado: poblado y vacío se implementan a partir de esta misma
  dirección. Ante cualquier contradicción, manda `DESIGN.md`; un defecto visual se corrige con feature
  correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — registrar `domain.favorites`/`data.favorites` (base
  propia `favorites.db`, tabla multi-fila) y `ui.favorites`, más la integración (Home "Guardadas" y
  corazón del Detalle).
- `docs/domain-model.md`: **not needed** — ya define `Favorite` (copia de la Recipe, independiente del
  Lote), su relación y el edge case del favorito eliminado.
- `DESIGN.md` y `docs/design/concepts/`: **update** — registrar la captura aceptada `favorites/favoritos`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear `domain.favorites` (`Favorite`, `FavoriteRepository`, `FavoriteService`) y su test JVM.
2. Crear `data.favorites` (entidad, DAO, base `favorites.db`, mappers, repositorio) y el test Room JVM.
3. Crear la lógica pura de presentación (`FavoritesSort`, `FavoritesViewMode`, `FavoritesList`) y su test.
4. Crear `FavoritesScreen`/`FavoriteRecipeCard` sobre los tokens y el degradado existentes.
5. Integrar: `MainActivity`/`SazonApp` (componer dependencias), `MainShell` (pestaña, Home "Guardadas",
   corazón del Detalle); eliminar `SectionPlaceholder.kt`.
6. Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`/`DESIGN.md`.

## Implementation Tasks

- [ ] Crear `Favorite`/`FavoriteRepository`/`FavoriteService` (`toggle` con copia y `matchScore = null`).
- [ ] Escribir `FavoriteServiceTest` (marcar, desmarcar, idempotencia, copia y `savedAt`, casos negativos).
- [ ] Crear `FavoriteEntity`/`FavoriteDao`/`FavoritesDatabase` (`favorites.db`, v1) y `FavoritesMappers`.
- [ ] Crear `RoomFavoriteRepository` y escribir `RoomFavoriteRepositoryTest` (round-trip, 2 filas,
      upsert sin duplicar, `remove` selectivo, `clear`, `observe`).
- [ ] Crear `FavoritesSort`/`FavoritesViewMode`/`FavoritesList` (`visible`/`countLabel`).
- [ ] Escribir `FavoritesListTest` (conteo singular/plural, los tres órdenes, búsqueda por título
      normalizada, consulta en blanco, sin coincidencias, `fromKey` desconocido).
- [ ] Crear `FavoriteRecipeCard` (miniatura 96x96, meta, corazón, swipe "Eliminar").
- [ ] Crear `FavoritesScreen` (cabecera+conteo, buscador, chips de orden, toggle, lista/rejilla, nota
      "Disponibles sin conexión", estado vacío con "Explorar recetas", `Saver`).
- [ ] Componer en `MainActivity`/`SazonApp` (`FavoritesDatabase`/repositorio/servicio).
- [ ] Integrar `MainShell` (pestaña Favoritos, `HomeRecipes.saved`, corazón del Detalle) y eliminar
      `SectionPlaceholder.kt`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`/`DESIGN.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: suites nuevas en verde sin romper las 277 existentes:
  - `FavoriteServiceTest`: `toggle` de una receta no guardada guarda **una copia** con `savedAt = now`
    y devuelve `true`; volver a alternar la **quita** y devuelve `false`; `isFavorite` refleja el
    estado; la copia normaliza `matchScore` a `null`; dos recetas distintas coexisten (caso negativo:
    alternar dos veces deja la lista vacía; el repositorio no se consulta mal).
  - `FavoritesListTest`: `countLabel(1) == "1 receta"` y `countLabel(0)/countLabel(3)` en plural;
    `RECENT` ordena por `savedAt` desc, `TIME` por `totalMinutes` asc, `DIFFICULTY` por la Taxonomía;
    consulta en blanco devuelve todo; la búsqueda filtra por **título** ignorando mayúsculas y acentos
    (caso negativo: una receta que solo coincide por descripción **no** entra); sin coincidencias
    devuelve vacío; `fromKey` desconocido → `RECENT`.
  - `RoomFavoriteRepositoryTest` (Robolectric, base en memoria): ida y vuelta exacta de una `Recipe`
    completa con `nutrition`/`ingredientId` opcionales; `isFavorite`; **dos** favoritos coexisten
    (`COUNT = 2`); reescribir el mismo `recipeId` no duplica (`COUNT = 1`); `remove(id)` borra solo esa;
    `clear()` deja la lectura vacía; `observe()` emite; el orden devuelto es `savedAt` desc.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `domain.favorites` sin imports `android.*`/`androidx.*`; `ui.favorites` con la lógica
  pura (`FavoritesList`/`FavoritesSort`/`FavoritesViewMode`) sin imports Compose/Android; sin cambios en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio aceptado, `data.profile/**`/
  `data.suggestions/**`, la Taxonomía ni `seed-recipes.json`; sin hex sueltos ni iconos externos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - **Guardar una receta generada por IA** (del flujo del Generador) con el corazón/"Guardar";
    **reiniciar la app sin conexión** y verla en la pestaña Favoritos y en "Tus recetas → Guardadas"
    (verificación exigida).
  - **Estado vacío** cuando no hay favoritos, con "Explorar recetas" llevando a Inicio (verificación
    exigida); desmarcar el corazón devuelve el estado vacío si era la última.
  - Buscador/chips de orden y toggle lista/rejilla; swipe "Eliminar"; abrir el Detalle desde Favoritos
    con el corazón relleno; el acabado se compara con la captura `favoritos`.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador. El comportamiento verificable (copia persistida, idempotencia, conteo, búsqueda, orden y
ida/vuelta de persistencia) se cubre en JVM; el acabado visual y las dos pruebas manuales exigidas son
de la persona.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos ni necesita modos nuevos.

## Evidence To Capture

- Salida de `./gradlew test` con `FavoriteServiceTest`, `FavoritesListTest` y
  `RoomFavoriteRepositoryTest`, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `domain.favorites` puro sin imports Compose/Android; `ui.favorites` con la lógica de
  presentación pura; sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio
  aceptado ni bases ajenas; `SectionPlaceholder.kt` eliminado sin referencias colgantes.
- Resultado manual en emulador (descripción; captura opcional) de guardar una receta IA, verla tras
  reiniciar sin conexión, del estado vacío y del acabado frente a la captura `favoritos`.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **El Favorito persiste una copia de la `Recipe`** (no solo el id): lo fija `docs/domain-model.md` y
  lo exige la nota de la feature; es la garantía de que un favorito `ai` sigue disponible al refrescarse
  el Lote (Scenario 3).
- **`matchScore` se normaliza a `null` al guardar**: es un valor **derivado y no persistido**
  (`docs/domain-model.md`); la tarjeta de Favoritos no muestra píldora de coincidencia.
- **Base de datos propia `favorites.db` (tabla multi-fila)**: añadir la entidad a `ProfileDatabase`
  obligaría a subir a versión 2 y a una migración **no ejercitable en tests JVM**; mismo criterio que
  `SuggestionBatchDatabase` en `suggestion-batch-cache`. Deuda anotada: consolidar las bases Room.
- **`savedAt` persistido por favorito**: alimenta el orden por defecto "Añadidas recientemente" y el
  conteo; el reloj se inyecta en `FavoriteService` (`now`), determinista en test.
- **El "marcar/desmarcar" vive en `FavoriteService.toggle`**: una sola operación pura y testeada (con
  puerto y reloj inyectables) evita duplicar la lógica en el Detalle y en la lista.
- **Reutilizar las costuras existentes**: `RecipeDetailScreen(isSaved, onToggleSave)` y el overlay
  `detailRecipe` de `MainShell` ya existen; la pestaña abre el mismo Detalle sin `PushedScreen` nuevo ni
  librería de navegación.
- **La Home "Guardadas" se alimenta de los favoritos** (`HomeRecipes.saved`), coherente con
  `CONTEXT.md`/`DESIGN.md` y con "favorites las alimentará"; **"Abiertas hace poco"** queda vacía (no
  hay feature que registre `lastOpenedAt`).
- **Buscador y orden locales** (`FavoritesSort`/`FavoritesList`): `DESIGN.md` Core Screen 6 pide "búsqueda,
  orden, lista"; son lógica pura y offline, sin red.
- **Swipe-to-delete con acción "Eliminar"** (captura) como afordancia secundaria sobre `SwipeToDismissBox`
  de Material 3 (sin dependencia nueva); la operación pura es `remove(recipeId)` y el gesto es revisión
  de la persona. La vía principal de quitar es el **corazón** (también en el Detalle).
- **`SectionPlaceholder.kt` se elimina**: al sustituir la pestaña Favoritos queda sin referencias
  (mismo criterio con el que `home-offline-blocks` eliminó `HomePlaceholder.kt`).
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Ids de las recetas `ai`**: el Favorito usa `recipe.id` como clave; con `FakeRecipeGenerator` los ids
  son estables, pero el proveedor real asignará ids al persistir las recetas `ai` (`docs/domain-model.md`).
  Si dos recetas generadas distintas colisionaran en `id`, el upsert sobrescribiría el favorito; se
  resolverá al integrar `real-ai-provider`.
- **Swipe-to-delete / rejilla**: son puramente visuales/gestuales y no automatizables; un defecto se
  corrige con feature correctiva.
- **"Abiertas hace poco"**: no se implementa (ninguna feature registra aún `lastOpenedAt`); el grupo de
  la Home sigue vacío.
- **Consolidación de bases Room** (`sazon.db` + `suggestion_batch.db` + `favorites.db`): deuda ya
  anotada; esta feature no la toca.
- **Process death mostrando el Detalle**: `detailRecipe` vive en `remember`; rotar cierra el Detalle
  (Unknown heredado de `recipe-detail-screen`).

## Validator Checklist

- [ ] La implementación se queda en `domain.favorites`, `data.favorites` y `ui.favorites` + la
      integración en `MainShell`/`SazonApp`/`MainActivity`; sin Resumen de actividad, sin "Abiertas hace
      poco", sin modo cocina, sin Búsqueda con IA ni proveedor real.
- [ ] El Favorito persiste una **copia de la `Recipe`** y sobrevive al refresco del Lote (Scenario 3).
- [ ] Marcar/desmarcar desde el Detalle y desde la lista persiste y actualiza el corazón (Scenarios 1, 2
      y 7); desmarcar **no** modifica el Lote.
- [ ] La pestaña lista y funciona **sin conexión** y muestra el **estado vacío** exigido (Scenarios 4 y 5,
      verificaciones exigidas).
- [ ] El conteo, la búsqueda por título y los tres órdenes son puros y están cubiertos en JVM con casos
      negativos (Scenarios 5 y 6).
- [ ] La persistencia Room es idempotente y multi-fila; `remove` es selectivo y `clear` vacía (Scenario 10).
- [ ] "Tus recetas → Guardadas" de la Home refleja los favoritos (Scenario 9).
- [ ] `domain.favorites` no importa Android; `ui.favorites` mantiene la lógica pura sin Compose; sin hex
      sueltos ni iconos/images externos.
- [ ] No se añaden dependencias (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos); no se
      tocan las bases ni el dominio aceptados.
- [ ] Para UI: `Visual Design Impact` nombra las capturas de
      `docs/design/concepts/favorites/favoritos/` (`screen.png`, `code.html`, `DESIGN.md`), que
      **existen** antes de implementar, y la implementación las sigue.
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `docs/technical-discovery.md`
      y `DESIGN.md` quedan coherentes.
