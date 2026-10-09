# Feature Implementation Spec: Filtros locales sobre las recetas del dispositivo

## Source Feature

- `id`: `local-result-filters`
- `area`: `ui`
- `depends_on`: `["seed-recipe-catalog", "home-offline-blocks"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Añadir el **filtrado local, sin red** de las recetas que ya están en el dispositivo, aplicado al
bloque **"Tus recetas"** de la Home: un **filtro por categoría** (`categoryId`) y dos **filtros
derivados** ("Rápido" = `totalMinutes ≤ 30`; "Vegetariano" = sin ingredientes con tags `meat`,
`poultry`, `fish` ni `seafood`). Es la definición de "Filtros de resultados" de `CONTEXT.md`.

Se entregan dos piezas: (1) una **lógica pura y reutilizable** en `com.sazon.app.ui.filters`
(selección + predicados + etiquetas + serialización de la selección) testeable en JVM, y (2) una
**fila de chips** (`FilterChip`) integrada en el bloque "Tus recetas", con selección única y los
chips derivados visualmente diferenciados de las categorías. El filtrado ocurre **sobre los datos ya
cargados en memoria** (el `HomeRecipes` que alimenta el bloque), nunca pide red.

> **Nota de secuencia (ver Decisiones):** hoy `HomeRecipes.saved` y `HomeRecipes.recentlyOpened`
> llegan **vacíos** desde `MainShell` (`home-offline-blocks` los dejó como costura documentada; los
> llenarán `favorites` y `recipe-detail-screen`). La lógica pura y sus predicados se verifican en JVM
> ahora; el efecto visual completo del bloque se observará cuando esas features pueblen la costura.

## Non-Goals

- **No** se implementa la pantalla **Favoritos** ni su buscador por texto: son de `favorites`
  (`CONTEXT.md` menciona "por texto en la pantalla Favoritos", pero `feature_list.json` limita esta
  feature a categoría + "Rápido"/"Vegetariano" en "Tus recetas"). Solo se deja la lógica
  **reutilizable** para que `favorites` la consuma.
- **No** se implementan `favorites` ni "Abiertas hace poco": esta feature **no** llena la costura
  `HomeRecipes`; filtra lo que reciba.
- **No** se filtran las recetas semilla del carrusel "Sugeridas para ti" ni la Búsqueda con IA:
  "Tus recetas" es el único consumidor actual.
- **No** se crea ninguna base de datos, índice ni persistencia del filtro: la selección vive en
  `rememberSaveable` durante la sesión.
- **No** se toca `Recipe`, `RecipeJson`, la Taxonomía, `domain.suggestions`/`data.suggestions`,
  `domain.recipe`/`data.recipe` ni el catálogo semilla.
- **No** se añaden dependencias de build (Material 3 ya aporta `FilterChip`) ni se añaden iconos o
  imágenes.

## Job Story

Cuando abro "Tus recetas" con recetas guardadas o abiertas hace poco,
quiero acotarlas por categoría o por filtros rápidos ("Rápido", "Vegetariano") sin conexión,
para decidir qué cocinar sin hacer scroll por una lista larga y sin depender de la red.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; elige un filtro y ve la lista acotada. Sin cuentas
  ni permisos.
- **Desarrollador / agente**: implementa la lógica pura y la fila de chips y la verifica en **JVM**
  (predicados de "Rápido"/"Vegetariano"/categoría, etiquetas, serialización de la selección); el
  acabado visual lo revisa la persona tras el merge (AGENTS.md no automatiza UI de emulador).

## Acceptance Scenarios

### Scenario 1: "Todas" no filtra (verificación base)

Given el bloque "Tus recetas" con recetas de varias categorías y tiempos,
When la selección es `LocalResultFilter.All`,
Then `LocalResultFilters.visible(recipes, All, taxonomy)` devuelve **las mismas recetas en el mismo
orden**.

### Scenario 2: filtro por categoría (`categoryId`)

Given recetas con `categoryId` `"pasta"` y `"rice"`,
When la selección es `Category("pasta")`,
Then solo se devuelven las recetas con `categoryId == "pasta"`; un `categoryId` inexistente devuelve
la lista vacía, no todas.

### Scenario 3: "Rápido" selecciona `totalMinutes ≤ 30` (verificación exigida)

Given recetas con `totalMinutes` 20, 30 y 31,
When la selección es `LocalResultFilter.Fast`,
Then la de 20 y la de 30 se incluyen y la de 31 se excluye; la frontera `30` se incluye
(`<= FAST_MAX_MINUTES`, con `FAST_MAX_MINUTES = 30`).

### Scenario 4: "Vegetariano" excluye tags de carne, ave, pescado o marisco (verificación exigida)

Given recetas cuyos ingredientes resuelven a `pollo` (`poultry`), `salmon` (`fish`), `gamba`
(`seafood`) y `ternera` (`meat`), y una receta con `tofu`/`brocoli` (sin esos tags),
When la selección es `LocalResultFilter.Vegetarian`,
Then las cuatro recetas con tags animales se **excluyen** y la vegetal se **incluye** (caso negativo:
ninguna receta animal se cuela).

### Scenario 5: ingrediente libre no detectado (edge case documentado)

Given una receta con un ingrediente de `ingredientId = null` (texto libre),
When se aplica `Vegetarian`,
Then no se excluye por tags (no hay resolución en la Taxonomía). Es la limitación ya aceptada del
enfoque "tags, no keywords"; se documenta como riesgo residual.

### Scenario 6: fila de chips con derivados diferenciados

Given el bloque "Tus recetas" con al menos una receta,
When se pinta el bloque,
Then aparece una fila de chips de selección única, con **"Todas"** primero, después una chip por
categoría **presente** en las recetas (etiqueta de `recipeCategories`) y al final **"Rápido"** y
**"Vegetariano"**; los chips derivados se distinguen visualmente de las categorías (tratamiento
seleccionado en oliva/`secondary` y agrupación aparte). Cambiar de chip recalcula el bloque.

### Scenario 7: estados vacíos

Given el bloque **sin** recetas (usuario nuevo), Then se muestra solo la línea
`"Cuando guardes una receta aparecerá aquí."` y **no** se pinta la fila de chips.
Given el bloque **con** recetas pero un filtro que no casa ninguna, Then se mantiene la fila de
chips y se muestra una línea de "sin resultados para este filtro"; al volver a "Todas" reaparecen.

### Scenario 8: todo funciona sin conexión

Given el dispositivo sin red,
When se usa el filtro,
Then los chips cambian la lista con datos ya cargados en memoria y sin ninguna llamada de red.

## Repository Research

### Files Inspected

- `feature_list.json` — definición, verificación y nota de `local-result-filters`; vecinas
  `favorites`, `recipe-detail-screen`, `profile-screen`.
- `PROGRESS.md` — camino de verificación `init.sh` (`./gradlew test` + `lint`); regla de no
  automatizar UI de emulador; `home-offline-blocks` es la última aceptada.
- `CONTEXT.md` — "Filtros de resultados" (definición canónica: categoría, texto en Favoritos,
  "Rápido" `≤ 30`, "Vegetariano" sin tags `meat`/`poultry`/`fish`/`seafood`; aplica a "Tus recetas"
  y Favoritos) y "Bloque 'Tus recetas'".
- `docs/domain-model.md` — esquema `Recipe`/`RecipeIngredient`; el bloque "Tus recetas" agrupa
  favoritas y abiertas hace poco.
- `docs/domain/taxonomy.md` — `ingredient.tags` y `dietPatterns[].excludedTags`; regla §2/§3.
- `docs/build-brief.md` — "Tus recetas" con Filtros de resultados locales; solo la búsqueda con IA
  necesita red.
- `docs/technical-discovery.md` — ubicación de `ui.home`/`MainShell`, el Lote y la Home offline.
- `docs/design/stitch-prompt.md` — bloque B.5: fila de chips `Todas, Postres, Pasta, Arroz,
  Ensaladas, Carnes, Vegetariano, Rápido` aplicada localmente, con la indicación explícita de que
  "Vegetariano" y "Rápido" son **derivados**, no categorías.
- `DESIGN.md` — `FilterChip` (fila desplazable, seleccionado terracota), `chip-selected-secondary`
  (oliva), tokens, targets ≥48dp.
- `docs/specs/home-offline-blocks.md` — `HomeRecipes`, `HomeScreen`, `MainShell`, costura del bloque 5
  alimentada vacía.
- `docs/specs/seed-recipe-catalog.md` — catálogo semilla (12 recetas, varias categorías y tiempos),
  útil como material de test.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — `RecipesSection`/`HomeRecipesGroup`
  (puntos de integración).
- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — costura `saved`/`recentlyOpened` +
  `isEmpty`.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` y `RecipeCardFormat.kt` — patrón de
  componente y de formato puro.
- `app/src/main/java/com/sazon/app/ui/preferences/IngredientsFilter.kt` — patrón de filtro puro.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `categoryId`, `totalMinutes`,
  `RecipeIngredient.ingredientId`.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — `ingredient(id).tags`,
  `dietPattern("vegetarian").excludedTags`, `recipeCategories`, `recipeCategory(id)`.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — precedente de exclusión por
  `tags` de ingrediente resuelto.
- `app/src/test/java/com/sazon/app/ui/home/{HomeRecipesTest,RecipeCardFormatTest}.kt` y
  `.../ui/preferences/IngredientsFilterTest.kt` — patrón de test JVM, carga de la Taxonomía real por
  `taxonomy.asset.path`, casos negativos.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: el filtro vive en Kotlin sin imports de Compose/Android y
  se testea en JVM; el `@Composable` solo pinta y guarda la selección.
- **Filtro puro con `object`** (`IngredientsFilter`) y **microcopy/labels desde la Taxonomía**.
- **Selección con `rememberSaveable` + `Saver`** (patrón `MainNavigationStateSaver`), sin librería de
  navegación.
- **Tokens del tema** (`MaterialTheme.colorScheme`, `SazonPillShape`, `SazonSpacing`): nunca hex
  sueltos.
- Tests JUnit 4 en `app/src/test/...`, nombres en español con `_`, casos negativos; la Taxonomía real
  se lee por `taxonomy.asset.path`.
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Current Gaps

- No existe ninguna lógica de filtrado local (`categoryId`, "Rápido", "Vegetariano").
- `HomeScreen`/`RecipesSection` no tienen fila de chips ni estado de filtro; el bloque pinta todas las
  recetas que reciba.
- La costura `HomeRecipes` recibe listas vacías (dato real de la secuencia); no hay consumidor de
  filtros todavía.

## Technical Approach

Estrategia: **lógica pura reutilizable + fila de chips delgada**, sin dependencias nuevas.

### 1. Lógica pura — `com.sazon.app.ui.filters.LocalResultFilter`

Sealed interface con una selección única:

```kotlin
sealed interface LocalResultFilter {
    val key: String
    data object All : LocalResultFilter            // key = "all"
    data class Category(val categoryId: String)    // key = "category:<id>"
    data object Fast : LocalResultFilter           // key = "fast"
    data object Vegetarian : LocalResultFilter     // key = "vegetarian"
}
```

`object LocalResultFilters` con operaciones puras (sin Compose/Android):

- `const val FAST_MAX_MINUTES = 30`.
- `fun visible(recipes: List<Recipe>, filter: LocalResultFilter, taxonomy: Taxonomy): List<Recipe>`:
  - `All` → `recipes` tal cual (orden preservado).
  - `Category(id)` → `recipes.filter { it.categoryId == id }`.
  - `Fast` → `recipes.filter { it.totalMinutes <= FAST_MAX_MINUTES }`.
  - `Vegetarian` → `recipes.filter { recipe -> recipe.ingredients.none { ing ->
    ing.ingredientId?.let { taxonomy.ingredient(it) }?.tags?.any { it in vegetarianTags(taxonomy) }
      == true } }`.
- `fun vegetarianTags(taxonomy: Taxonomy): Set<String>` =
  `taxonomy.dietPattern("vegetarian")?.excludedTags?.toSet() ?: FALLBACK_VEGETARIAN_TAGS`
  (`FALLBACK_VEGETARIAN_TAGS = setOf("meat","poultry","fish","seafood")`, defensivo; en la Taxonomía
  real `vegetarian.excludedTags` es exactamente ese conjunto).
- `fun categoriesIn(recipes: List<Recipe>, taxonomy: Taxonomy): List<RecipeCategory>`: categorías
  **presentes** en las recetas, ordenadas por el orden de `taxonomy.recipeCategories`, ignorando
  `categoryId` desconocidos.
- `fun label(taxonomy: Taxonomy, filter: LocalResultFilter): String`: `"Todas"`, label de la
  categoría (respaldo al id), `"Rápido"`, `"Vegetariano"`.
- `fun isDerived(filter: LocalResultFilter): Boolean` (verdadero para `Fast` y `Vegetarian`).
- `fun fromKey(key: String): LocalResultFilter` (inversa de `key`; `"category:<id>"` → `Category`).

### 2. Fila de chips — `com.sazon.app.ui.filters.ResultFilterRow`

`@Composable fun ResultFilterRow(taxonomy, categories: List<RecipeCategory>, selected, onSelect)`:
`LazyRow` de `FilterChip` de Material 3 con `[All] + categoriesIn(...).map { Category(it.id) } +
[Fast, Vegetarian]`. Chips de categoría con `chip-selected` (terracota); chips derivados
(`isDerived`) agrupados al final y con el tratamiento seleccionado en **oliva**/`secondary`
(`chip-selected-secondary`) para que se lean como derivados y no como categorías. Etiquetas vía
`LocalResultFilters.label(...)`.

### 3. Integración en la Home

- `HomeRecipes` gana `fun filteredBy(filter: LocalResultFilter, taxonomy: Taxonomy): HomeRecipes` que
  aplica `LocalResultFilters.visible` a `saved` y a `recentlyOpened` (puro, sin Compose).
- `HomeScreen` guarda la selección con
  `rememberSaveable(stateSaver = LocalResultFilterSaver) { mutableStateOf<LocalResultFilter>(All) }`
  (`LocalResultFilterSaver` usa `key`/`fromKey`, patrón `MainNavigationStateSaver`).
- `RecipesSection` recibe la selección y el `onSelect`. Construye `categories` con
  `LocalResultFilters.categoriesIn(recipes.saved + recipes.recentlyOpened, taxonomy)` y:
  - si el bloque **sin filtrar** está vacío → solo la línea de usuario nuevo (sin chips);
  - si no, pinta `ResultFilterRow` y luego los grupos filtrados; si el resultado filtrado queda vacío,
    muestra una línea de "sin resultados para este filtro" (microcopy provisional, ver Unknowns).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/filters/LocalResultFilter.kt` — selección sellada + object de
  predicados/etiquetas/claves puras (sin Compose/Android).
- `app/src/main/java/com/sazon/app/ui/filters/ResultFilterRow.kt` — fila de `FilterChip` reutilizable.
- `app/src/test/java/com/sazon/app/ui/filters/LocalResultFilterTest.kt` — escenarios 1–5 y claves.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — estado del filtro, fila de chips,
  agrupación filtrada y estado vacío de filtro.
- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — `filteredBy(filter, taxonomy)`.
- `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` — casos de `filteredBy` (grupos).
- `docs/technical-discovery.md` — registrar `ui.filters` (filtrado local derivado) y su aplicación a
  "Tus recetas".

**No se toca** `app/build.gradle.kts` ni `gradle/libs.versions.toml` (sin dependencias), ni
`Recipe.kt`/`RecipeJson.kt`, la Taxonomía, `seed-recipes.json`, `domain.suggestions`/`data.suggestions`,
`domain.profile`/`data.profile` ni las features de onboarding/Home previas.

## Visual Design Impact

- UI involved: **sí** (fila de chips dentro del bloque "Tus recetas").
- Design source: `DESIGN.md` (`FilterChip`, `chip-selected` / `chip-selected-secondary`, tokens,
  targets ≥48dp) y `docs/design/stitch-prompt.md` (bloque B.5, con los chips y la aclaración de que
  "Rápido"/"Vegetariano" son derivados).
- Screens or states affected: bloque "Tus recetas" (fila de chips: activa/inactiva, categoría vs
  derivado), estado vacío de usuario nuevo y estado "sin resultados para este filtro".
- Capturas de referencia (backfill PR #37): `docs/design/concepts/local-result-filters/` — pantalla
  `filtros-tus-recetas`. Pendiente de exportar desde Stitch (petición en el `README.md` de la carpeta);
  la distinción visual derivado/categoría (oliva + agrupación) es una decisión de esta spec y el repaso
  visual y de contraste lo hace la persona tras el merge; un defecto visual se corrige con feature
  correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — registrar `com.sazon.app.ui.filters` (filtrado local por
  `categoryId` y filtros derivados "Rápido"/"Vegetariano") y que se aplica al bloque "Tus recetas".
- `CONTEXT.md` / `docs/domain-model.md` / `DESIGN.md`: **not needed** — ya describen "Filtros de
  resultados" y los tokens; esta feature no cambia el modelo ni el diseño.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión de planificación.

## Implementation Plan

1. Crear `LocalResultFilter`/`LocalResultFilters` puros (selección, predicados, tags vegetarianos,
   `categoriesIn`, `label`, `isDerived`, `fromKey`).
2. Escribir `LocalResultFilterTest` (escenarios 1–5, frontera de `≤ 30`, tags animales, categoría,
   claves, respaldo de `vegetarianTags`).
3. Añadir `HomeRecipes.filteredBy` y sus casos en `HomeRecipesTest`.
4. Crear `ResultFilterRow` con los tokens del tema y la distinción derivado/categoría.
5. Integrar en `HomeScreen`/`RecipesSection` (estado `rememberSaveable` + `Saver`, chips, grupos
   filtrados, estados vacíos).
6. Ejecutar `init.sh` (test + lint) en verde y corregir lo que salga.
7. Actualizar `docs/technical-discovery.md`; el implementer registra evidencia y estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `LocalResultFilter` (`All`, `Category`, `Fast`, `Vegetarian`) con `key`.
- [ ] Crear `LocalResultFilters.visible` con los cuatro predicados y `FAST_MAX_MINUTES = 30`.
- [ ] Crear `vegetarianTags(taxonomy)` derivado de `dietPattern("vegetarian").excludedTags` con
      respaldo.
- [ ] Crear `categoriesIn`, `label`, `isDerived` y `fromKey`.
- [ ] Escribir `LocalResultFilterTest` con los escenarios 1–5 y las claves (incluye frontera 30/31 y
      caso negativo de tags animales).
- [ ] Añadir `HomeRecipes.filteredBy(filter, taxonomy)` y cubrirlo en `HomeRecipesTest`.
- [ ] Crear `ResultFilterRow` (`FilterChip`; categorías terracota, derivados oliva y agrupados).
- [ ] Integrar el estado de filtro (`rememberSaveable` + `Saver`) y la fila en `HomeScreen`.
- [ ] Aplicar el filtro a los grupos y pintar el estado "sin resultados para este filtro".
- [ ] Ejecutar `init.sh` en verde.
- [ ] Actualizar `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: `LocalResultFilterTest` verde sin romper las 159
  existentes:
  - `All` devuelve la lista intacta y en orden.
  - `Category("pasta")` devuelve solo `categoryId == "pasta"`; categoría inexistente → vacío.
  - `Fast`: 20 y **30** incluidos, 31 excluido (frontera `<= 30`). **Verificación exigida.**
  - `Vegetarian`: excluye recetas con `pollo`/`salmon`/`gamba`/`ternera` (tags `poultry`/`fish`/
    `seafood`/`meat`) e incluye la vegetal; **caso negativo** de que ninguna animal pasa.
  - Ingrediente libre (`ingredientId = null`) no excluye (limitación documentada).
  - `fromKey(All.key)`, `fromKey(Category("rice").key)` y los derivados hacen round-trip.
  - `categoriesIn` devuelve categorías presentes, sin duplicados y en orden de la Taxonomía.
  - `HomeRecipesTest.filteredBy`: filtra `saved` y `recentlyOpened` de forma independiente.
  - La Taxonomía se carga por `taxonomy.asset.path` (patrón existente).
- `./gradlew lint` (vía `init.sh`) → 0 errores, sin warnings nuevos en ficheros de la feature.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0, sin procesos largos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Con el bloque "Tus recetas" poblado (tras `favorites`/`recipe-detail-screen`), cambiar de chip
    acota la lista; "Rápido" y "Vegetariano" se distinguen de las categorías.
  - Sin conexión todo sigue funcionando.
  - **Hoy** la costura llega vacía: el bloque muestra solo el estado de usuario nuevo; se acepta que
    el efecto visual se observe al poblarse (ver Decisiones).

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador. La lógica/estado se cubre en JVM (que es lo que exige `feature_list.json`); el acabado
visual y la prueba offline son de la persona.

## Evidence To Capture

- Salida de `./gradlew test` con `LocalResultFilterTest` y `HomeRecipesTest` nuevos, nº de tests y 0
  fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `ui.filters` sin imports de Compose/Android en la lógica pura; sin dependencias nuevas;
  `Recipe`/Taxonomía/`seed-recipes.json`/`domain.suggestions` intactos.
- Nota explícita de que el bloque "Tus recetas" llega vacío hoy (costura) y que el filtro puro es lo
  verificado ahora.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **"Rápido" y "Vegetariano" son derivados, no categorías** (nota de la feature y `CONTEXT.md`). El
  filtro "Vegetariano" **no** es `categoryId == "vegetarian"` (que existe como categoría de receta):
  se calcula por tags de ingrediente. Por eso los chips derivados se distinguen de las categorías.
- **"Vegetariano" deriva sus tags de `dietPatterns[vegetarian].excludedTags`** (`meat`, `poultry`,
  `fish`, `seafood`), la fuente única de verdad, con respaldo constante defensivo. Es exactamente lo
  que exige la verificación.
- **Detección por tags, no por keywords**: se resuelven solo ingredientes con `ingredientId`; un
  ingrediente libre no se excluye. Es la limitación ya aceptada en `CONTEXT.md`/`recipe-validator` y
  se documenta como riesgo residual.
- **Selección única**: fila de chips con un solo filtro activo y valor por defecto "Todas",
  coherente con el bloque B.5 de `stitch-prompt.md`. No hay combinación categoría+derivado.
- **Categorías data-driven**: los chips de categoría salen de las categorías **presentes** en las
  recetas del bloque, en orden de la Taxonomía, evitando hardcodear la lista de ejemplo del prompt y
  chips de categorías vacías. Sin recetas, solo se muestran "Todas" + derivados (y hoy el bloque
  está vacío, así que ni eso; ver secuencia).
- **La lógica vive en `ui.filters` y es reutilizable**: `favorites` la consumirá para su pantalla
  (buscador por texto aparte), materializando la regla de `CONTEXT.md` "aplica a 'Tus recetas' y a
  Favoritos".
- **El filtro no llena la costura**: `HomeRecipes.saved`/`recentlyOpened` siguen llegando vacíos desde
  `MainShell`; llenarlos es de `favorites`/`recipe-detail-screen`. Esta feature **consume** lo que
  reciba.
- **Estado con `rememberSaveable` + `Saver`**: la selección sobrevive a cambios de configuración, sin
  añadir persistencia ni dependencias (patrón `MainNavigationStateSaver`).
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Secuencia (lo más relevante)**: como la costura `HomeRecipes` está vacía hasta
  `favorites`/`recipe-detail-screen`, el efecto visual del filtro no es observable hoy en la app; la
  verificación exigida (predicados en JVM) sí lo es. Si se quisiere un efecto inmediato habría que
  reordenar `feature_list.json` (p. ej. colocar `favorites` antes); se deja como observación, sin
  cambiarlo en esta sesión.
- **Microcopy del estado "sin resultados para este filtro"**: provisional (p. ej. "No hay recetas con
  este filtro."), sujeto al repaso de la persona; no hay literal en `stitch-prompt.md` para ese caso.
- **Distinción visual exacta de los chips derivados** (oliva + agrupación): decisión de esta spec; el
  repaso de contraste/claro-oscuro queda para la persona.
- **Aparecer o no de la fila con el bloque vacío**: se decide **no** mostrarla (limpieza del estado de
  usuario nuevo); ajustable como detalle visual sin cambiar la lógica pura.
- **¿Debe el filtro de categoría convivir con la categoría `vegetarian`?** Hoy la lista es
  data-driven y el derivado gana en intención; si en el futuro ambos coinciden en una receta, las
  etiquetas pueden repetirse: la distinción visual lo mitiga, pero se anota por si `favorites`
  propone agrupar derivados en una sección propia.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: solo filtrado local de "Tus recetas" (categoría +
      "Rápido" + "Vegetariano"); sin Favoritos, sin buscador por texto, sin red, sin persistencia y
      sin tocar `Recipe`/Taxonomía/`seed-recipes.json`/`domain.suggestions`.
- [ ] `LocalResultFilter`/`LocalResultFilters` son lógica pura (sin imports de Compose/Android).
- [ ] "Rápido" selecciona `totalMinutes <= 30` (frontera incluida) y "Vegetariano" excluye tags
      `meat`/`poultry`/`fish`/`seafood`, con caso negativo que no pasa en vacío (escenarios 3 y 4).
- [ ] El filtro por categoría usa `categoryId` y no confunde "Vegetariano" con la categoría
      `vegetarian` (escenario 2 y decisión).
- [ ] La fila de chips distingue visualmente los filtros derivados de las categorías y usa tokens del
      tema (sin hex sueltos) (escenario 6).
- [ ] Los estados vacíos se comportan como en el escenario 7.
- [ ] No se añaden dependencias (`app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios).
- [ ] `init.sh` queda en verde, la evidencia está en `feature_list.json`/`PROGRESS.md` y
      `docs/technical-discovery.md` queda coherente.
