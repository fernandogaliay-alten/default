# Feature Implementation Spec: Pantalla de resultados

## Source Feature

- `id`: `results-screen`
- `area`: `ui`
- `depends_on`: `["generation-flow-with-validation"]` (`accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Nota de la feature: "Cuando se llega desde la búsqueda con IA, el subtítulo es la consulta escrita.
  Aquí se fija la formula de matchScore (lo calcula la app, derivado, no persistido)."

> **Bloqueo de capturas (gate de UI).** Faltan las capturas de referencia obligatorias en
> `docs/design/concepts/results-screen/` (ver `Visual Design Impact`). Esta sesión de `planner` solo
> puede escribir en `docs/specs/`, así que **no** ha creado la carpeta ni su `README.md`; la persona (o
> el orquestador) debe crear `docs/design/concepts/results-screen/README.md` con la lista de abajo y
> dejar los tres ficheros de cada pantalla. La feature queda **`blocked`** hasta que las capturas
> existan (`AGENTS.md`; el `implementer` no arranca sin ellas).

## Goal

Entregar la **pantalla de Resultados real** que hoy es solo un *seam* mínimo
(`GeneratorResultsPreview`): al terminar una generación, el usuario ve una lista vertical de las
recetas **aceptadas** por el `RecipeValidator`, cada una con imagen (placeholder de degradado), título,
descripción de una línea, fila de metadatos (`tiempo · dificultad · comensales`) y una **píldora de
coincidencia** con el `matchScore`, además de los estados **vacío** y de **error/modo degradado**
(sin IA), con acción de reintento. Cuando la pantalla se alcance desde la **Búsqueda con IA**
(feature futura `ai-search`) el título y el subtítulo cambian: "Resultados de tu búsqueda" y la
consulta escrita entre comillas.

La feature también **fija la fórmula de `matchScore`** (hoy pregunta abierta resuelta a medias): es un
valor **derivado, calculado por la app y no persistido**, que se recalcula al mostrar resultados y se
materializa en la capa Compose. Se entrega como lógica **pura y testeable en JVM** (`MatchScore`,
`ResultsStatus`, `ResultsHeader`) más una capa Compose delgada y reutilizable (`ResultsScreen`,
`ResultRecipeCard`).

## Non-Goals

- **No** se implementa la **Búsqueda con IA** (`ai-search`): no hay barra en la Home, ni
  `searchByText`, ni memoización de consulta. Solo se deja lista la costura para su subtítulo
  (`ResultsOrigin.Search` + `ResultsScreen` reutilizable); su host lo monta `ai-search`.
- **No** se implementan los **chips de orden** de Stitch ("Relevancia / Más rápida / Más fácil / Mejor
  valoradas" ni "Ajustar"): fuera del `user_visible_behavior` y la verificación. Los resultados se
  pintan en un orden estable por relevancia (`matchScore` desc, empates al orden original).
- **No** se implementan **Favoritos / corazón para guardar** (`favorites`) ni el **Detalle de receta**
  (`recipe-detail-screen`): la tarjeta no es clicable y no lleva corazón en esta feature.
- **No** se persiste nada: ni recetas generadas, ni `matchScore`, ni el Resumen de actividad
  (`activity-summary`). El resultado vive en memoria durante la sesión del Generador.
- **No** se implementa el **proveedor real** (`real-ai-provider`): sin HTTP, sin red, sin clave; el
  generador conectado sigue siendo `FakeRecipeGenerator`.
- **No** se toca el Lote de sugerencias, `domain.suggestions`, `data/**`, `domain.recipe`,
  `domain.validation`, `domain.generation`, `domain.generator`, `domain.prompt`, la Taxonomía,
  `seed-recipes.json`, Room/Perfil ni el `init.sh`.
- **No** se añaden `navigation-compose`, iconos (`material-icons-*`), imágenes ni DI; **sin
  dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos).

## Job Story

Cuando he generado recetas con lo que tengo en casa,
quiero ver una lista clara y apetitosa con la coincidencia de cada receta,
para decidir qué cocinar; y cuando no hay resultados o la IA falla, quiero un estado claro que me deje
ajustar o reintentar.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo. Llega a Resultados tras "Generar recetas"; puede
  volver a ajustar ingredientes, reintentar o atrás.
- **Desarrollador / agente**: cubre en JVM la fórmula de `matchScore`, la selección de estado y el
  formateo del encabezado (incluido el subtítulo de búsqueda), con casos negativos; el acabado visual
  lo revisa la persona en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: no hay red ni persistencia; si el generador ya degradó a `aiUnavailable`, la pantalla
  muestra el modo degradado con las recetas semilla locales.

## Acceptance Scenarios

### Scenario 1: resultados poblados (camino feliz)

Given un `GenerationResult` con `accepted` no vacío y un `PreferenceProfile`,
When se construye el estado y se pinta la pantalla,
Then se muestra el título "Estas son tus recetas", el subtítulo
"A partir de N ingredientes · X · S comensales", y una tarjeta por receta aceptada (imagen, título,
descripción, meta `tiempo · dificultad · comensales` y píldora `N% coincidencia`), ordenadas por
`matchScore` descendente. Ninguna receta `discarded` se renderiza.

### Scenario 2: fórmula de `matchScore` (verificación exigida por el harness)

Given una receta y un Perfil con ingredientes preferidos, cocina favorita, franja y dificultad,
When se llama `MatchScore.of(recipe, preferences, taxonomy, userIngredients)`,
Then devuelve un `Int` en `0..100`, determinista, igual al ejemplo trabajado de la sección
`Technical Approach`; añadir un ingrediente de `dislikedIngredientIds` presente resta la penalización
configurada; una receta sin ingredientes resueltos no divide por cero.

### Scenario 3: estado vacío

Given `accepted` vacío y `aiUnavailable = false`,
When se pinta la pantalla,
Then aparece el estado vacío ("No hemos encontrado recetas con esos ingredientes") con la acción
"Cambiar ingredientes"; no aparece ninguna tarjeta ni la franja ámbar.

### Scenario 4: error / modo degradado con semilla (verificación manual: reintentar)

Given `aiUnavailable = true` y recetas semilla disponibles,
When se pinta la pantalla,
Then aparece una franja ámbar ("Ahora mismo no podemos generar recetas nuevas. Aquí tienes nuestras
sugerencias."), la lista de recetas semilla y la acción **"Reintentar"**; nunca se etiquetan las
semillas como generadas.

### Scenario 5: error sin semilla

Given `aiUnavailable = true` y **sin** recetas semilla,
When se pinta la pantalla,
Then aparece el titular de error ("No hemos podido generar las recetas. Revisa tu conexión.") con
**"Reintentar"**; nunca en rojo de alarma.

### Scenario 6: subtítulo de la Búsqueda con IA

Given `ResultsOrigin.Search(query = "algo con pollo y arroz")`,
When se formatea el encabezado,
Then el título es "Resultados de tu búsqueda" y el subtítulo es la consulta entre comillas.

### Scenario 7: reintentar y cambiar ingredientes

Given la pantalla mostrando un estado de error o poblado,
When el usuario pulsa "Reintentar",
Then se relanza la generación con la **misma** petición (ingredientes + ajustes ya en el borrador) sin
cambiar datos; When pulsa "Cambiar ingredientes", Then vuelve al paso de ingredientes del Generador
conservando el borrador.

## Repository Research

### Files Inspected

- `feature_list.json` — `results-screen` (verificación: estado vacío y error con reintentar; nota de
  `matchScore` y subtítulo de búsqueda) y vecinas `generation-flow-with-validation` (accepted),
  `ai-search`, `recipe-detail-screen`, `favorites`.
- `PROGRESS.md` — la generación quedó en `GeneratorResultsPreview` (seam); "el resultado vive en
  `remember`"; la UI de emulador la revisa la persona.
- `docs/technical-discovery.md` — orquestación en `domain.generation`, `ui.generator`, y "El
  matchScore es derivado y lo fija `results-screen`".
- `docs/domain-model.md` — escenario 9 (Búsqueda con IA → pantalla de Resultados con la consulta como
  subtítulo); estado `ai_available`/`ai_unavailable`; `Recipe.matchScore` opcional, derivado, no
  persistido; `RecipeCard` (imagen 16:10, meta, píldora).
- `docs/risks-and-open-questions.md` — "Cálculo de `matchScore`: lo calcula la app (derivado, no
  persistido); la fórmula se fija en `results-screen`" (pregunta resuelta); la 1 (errores/reintentos
  de IA) sigue abierta.
- `DESIGN.md` — RecipeCard, píldora de coincidencia, aviso degradado ámbar (nunca rojo), una acción
  principal terracota, cifras tabulares, objetivos ≥48dp, estados vacío/carga/error, capturas por
  pantalla en `docs/design/concepts/<feature-id>/<pantalla>/`.
- `docs/design/stitch-prompt.md` sección D y `docs/design/stitch/15-resultados.md` — encabezado,
  chips de orden (fuera de alcance), tarjeta, estado vacío, estado bloqueado por perfil (fuera de
  alcance: es de `ai-search`), error y modo IA no disponible.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — huésped real; hoy el stage
  `result != null` pinta `GeneratorResultsPreview`; `result` y `generationId` viven en `remember`.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorResultsPreview.kt` — seam que esta feature
  **reemplaza** (titular + `RecipeCardFormat.metaLine` + aviso ámbar + vacío).
- `app/src/main/java/com/sazon/app/domain/generation/GenerationResult.kt` — `accepted`, `discarded`,
  `aiUnavailable` (contrato existente; **no** se modifica).
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `matchScore: Int?`, campos de la
  tarjeta (`totalMinutes`, `difficultyId`, `servings`, `description`, `ingredients[].ingredientId`).
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — `difficulty(id)`, `timeBucket(id)`,
  `cuisine(id)`, `ingredient(id)`; `TimeBucket.maxMinutes` (nullable).
- `app/src/main/java/com/sazon/app/domain/profile/UserProfile.kt` / `PreferenceProfile` —
  `preferredIngredientIds`, `dislikedIngredientIds`, `cuisineIds`, `difficultyId`, `timeBucketId`.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` + `RecipeCardFormat.kt` — tarjeta y formato
  puros (`metaLine`, `matchBadge`) reutilizables; degradado melocotón→oliva + glifo de plato (sin
  assets ni iconos).
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorRequestFactory.kt` — ya mapea el borrador a
  `List<RecipeIngredient>` (los "ingredientes del usuario" para la cobertura).
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorSettingsState.kt` /
  `GeneratorIngredientsState.kt` — `servings`, `timeBucketId`, `count` para el subtítulo.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — posee `seedRecipes`; monta `GeneratorFlow`.
- `docs/design/concepts/README.md` — convención de capturas (subcarpeta por pantalla, tres ficheros).

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: los objetos de `ui.results` no importan Compose/Android y se
  testean en JVM; los `@Composable` solo pintan y delegan.
- **Reutilización**: `RecipeCardFormat.metaLine`/`matchBadge`, tokens del tema (`SazonTheme`,
  `SazonSpacing`, `SazonShapes`, `SazonPillShape`), degradado+glifo de plato, sin hex sueltos ni
  iconos ("Atrás" es texto).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_` y casos negativos que fallarían
  si la lógica no hiciera nada.
- **Reusar el generador y el validador ya aceptados**: no se reimplementa nada de dominio.
- **Sin dependencias nuevas**; `init.sh` sin cambios.

### Current Gaps

- El resultado se pinta con un preview mínimo: sin imagen, sin `matchScore`, sin descripción, sin
  estados vacío/error reales ni subtítulo.
- No existe la fórmula de `matchScore` ni un estado de pantalla puro (poblado/vacío/degradado).
- No hay tarjeta de resultados a ancho completo ni encabezado con título/subtítulo.

## Technical Approach

Estrategia: **lógica pura en `ui.results` + capa Compose delgada y reutilizable**, sin tocar dominio
aceptado ni añadir dependencias. La pantalla se monta como el **stage de resultado dentro de
`GeneratorFlow`** (no se añade `PushedScreen.RESULTS` en esta feature: el Generador ya es una página
empujada sobre las pestañas y así se conservan sus borradores; `ai-search` añadirá el destino al
iniciar Resultados desde la Home). Ver Decisión.

### 1. `MatchScore` — fórmula derivada (pura)

Nuevo objeto en `com.sazon.app.ui.results`:

```kotlin
object MatchScore {
    fun of(
        recipe: Recipe,
        preferences: PreferenceProfile,
        taxonomy: Taxonomy,
        userIngredients: List<RecipeIngredient> = emptyList(),
    ): Int
}
```

Señales, cada una una fracción `0..1`, con pesos como constantes en un único sitio:

| Señal | Cálculo | Peso | Aplica si… |
|---|---|---|---|
| `coverage` | (ingredientes del usuario presentes en la receta) / (nº de ingredientes del usuario) | `W_COVERAGE = 3` | `userIngredients` no vacío |
| `liked` | (ingredientes resueltos de la receta con `ingredientId ∈ preferredIngredientIds`) / (nº de ingredientes resueltos) | `W_LIKED = 3` | la receta tiene ≥1 ingrediente resuelto |
| `cuisine` | `1` si `recipe.cuisineId ∈ cuisineIds`, si no `0` | `W_CUISINE = 2` | siempre |
| `time` | `1` si `recipe.totalMinutes <= timeBuckets[timeBucketId].maxMinutes`, si no `0` | `W_TIME = 1` | la franja del Perfil tiene `maxMinutes != null` |
| `difficulty` | `1` si `recipe.difficultyId == preferences.difficultyId`, si no `0` | `W_DIFFICULTY = 1` | siempre |

- "Ingrediente resuelto" = `ingredientId != null`. Un ingrediente del usuario casa con uno de la
  receta si ambos tienen `ingredientId` igual, o por **nombre normalizado** (trim, minúsculas, sin
  acentos) — así un ingrediente libre también cuenta.
- Penalización: `DISLIKE_PENALTY = 10` puntos por cada ingrediente resuelto de la receta cuyo
  `ingredientId ∈ dislikedIngredientIds` (preferencia blanda: **penaliza, nunca descarta**).
- Fórmula: `score = clamp(round(100 * Σ(w·f) / Σ(w aplicables)) − penalización, 0, 100)`.
- **Determinista**, sin reloj ni `Random`; **no persistido**: se recalcula al mostrar y se materializa
  con `recipe.copy(matchScore = ...)` en memoria.

**Ejemplo trabajado (vector del test):** Taxonomía real; Perfil con
`preferredIngredientIds = [tomate, cebolla, ajo]`, `cuisineIds = [italian]`,
`timeBucketId = lte_30` (`maxMinutes = 30`), `difficultyId = easy`, sin `disliked`; receta
`cuisineId = italian`, `totalMinutes = 25`, `difficultyId = easy`, ingredientes resueltos
[tomate, cebolla, arroz] y `userIngredients` vacío ⇒ numerador `3·(2/3) + 2·1 + 1·1 + 1·1 = 6`,
denominador `7` ⇒ `round(85.71) = 86`. Con `dislikedIngredientIds = [cebolla]` (presente) ⇒ `76`.

### 2. `ResultsStatus` — selección de estado (pura)

```kotlin
sealed interface ResultsStatus {
    data class Content(val recipes: List<Recipe>) : ResultsStatus      // aceptadas, ya con matchScore
    data object Empty : ResultsStatus
    data class Unavailable(val seedRecipes: List<Recipe>) : ResultsStatus
}
```

`ResultsStatus.from(result, preferences, taxonomy, userIngredients, seedRecipes)`:
`accepted` no vacío → `Content` (puntuadas y ordenadas por `matchScore` desc, empates estables);
si no, `result.aiUnavailable` → `Unavailable` (semillas puntuadas/ordenadas; puede ser vacío); si no,
`Empty`. La presentación del estado `Unavailable` depende de `seedRecipes.isEmpty()` (franja ámbar +
lista vs titular de error), pero **ambos ofrecen "Reintentar"** (cubre la verificación manual).

### 3. `ResultsHeader` / `ResultsOrigin` — encabezado (puro)

```kotlin
sealed interface ResultsOrigin {
    data class Generator(val ingredientCount: Int, val timeBucketId: String?, val servings: Int) : ResultsOrigin
    data class Search(val query: String) : ResultsOrigin
}
data class ResultsHeader(val title: String, val subtitle: String)
object ResultsHeaders { fun of(origin: ResultsOrigin, taxonomy: Taxonomy): ResultsHeader }
```

- `Generator`: título "Estas son tus recetas"; subtítulo
  "A partir de N ingredientes · X · S comensales" (singular/plural; `X` = `maxMinutes` de la franja
  como "30 min" o, si es `null`, el `label` de la franja; si el `timeBucketId` no resuelve, se omite).
- `Search`: título "Resultados de tu búsqueda"; subtítulo = consulta recortada entre comillas (“…”).

### 4. Capa Compose — `com.sazon.app.ui.results`

- `ResultsScreen(status, header, taxonomy, onBack, onChangeIngredients, onRetry, modifier)`:
  `Scaffold` a pantalla completa sobre `colorScheme.background`; `topBar` con `TextButton("Atrás")` +
  título (`headlineLarge`) + subtítulo (`bodyLarge`); `bottomBar` fijo con secundario
  "Cambiar ingredientes"; cuerpo por estado:
  - `Content`: `LazyColumn` de `ResultRecipeCard`.
  - `Empty`: estado vacío (titular + acción "Cambiar ingredientes").
  - `Unavailable(seedRecipes)` no vacío: franja ámbar (`tertiaryContainer`, nunca rojo) con el texto de
    modo degradado, **"Reintentar"**, y `LazyColumn` de semillas.
  - `Unavailable` vacío: titular de error + **"Reintentar"**.
- `ResultRecipeCard(recipe, taxonomy)`: tarjeta a ancho completo; imagen placeholder de degradado
  melocotón→oliva 16:10 con esquinas 24px y glifo de plato; título (2 líneas), `description` (1 línea),
  `RecipeCardFormat.metaLine` con cifras tabulares, y `RecipeCardFormat.matchBadge(recipe.matchScore)`
  en píldora terracota `SazonPillShape`. **Sin corazón/corazón clicable**. Reutiliza el degradado/glifo
  de `RecipeCard` (extraer un placeholder interno compartido es opcional pero preferible).

### 5. Integración — `GeneratorFlow` / `MainShell`

- `MainShell` pasa `seedRecipes` a `GeneratorFlow` (ya lo posee).
- `GeneratorFlow`, en `result != null`, construye `ResultsStatus.from(result, preferences, taxonomy,
  GeneratorRequestFactory.ingredients(ingredients), seedRecipes)` y
  `ResultsHeaders.of(ResultsOrigin.Generator(ingredients.count, settings.timeBucketId, settings.servings),
  taxonomy)`, y pinta `ResultsScreen` en lugar de `GeneratorResultsPreview`.
- Acciones: `onBack` → `result = null; generationId = null; step = SETTINGS`; `onChangeIngredients` →
  `result = null; generationId = null; step = INGREDIENTS`; `onRetry` → `result = null;
  generationId = (generationId ?: 0) + 1` (el `LaunchedEffect` ya existente relanza la generación con
  el mismo borrador).
- Se **elimina** `GeneratorResultsPreview.kt` (queda sin uso).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/results/MatchScore.kt` — fórmula pura.
- `app/src/main/java/com/sazon/app/ui/results/ResultsStatus.kt` — estados + `from(...)`.
- `app/src/main/java/com/sazon/app/ui/results/ResultsHeader.kt` — `ResultsOrigin`, `ResultsHeader`,
  `ResultsHeaders.of(...)`.
- `app/src/main/java/com/sazon/app/ui/results/ResultsScreen.kt` — pantalla delgada.
- `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt` — tarjeta de resultados.
- `app/src/test/java/com/sazon/app/ui/results/MatchScoreTest.kt` — escenario 2 y casos negativos.
- `app/src/test/java/com/sazon/app/ui/results/ResultsStatusTest.kt` — escenarios 1, 3, 4, 5.
- `app/src/test/java/com/sazon/app/ui/results/ResultsHeaderTest.kt` — escenario 6 y singular/plural.
- `docs/design/concepts/results-screen/README.md` + subcarpetas `<pantalla>/` — **petición de capturas**
  (ver `Visual Design Impact`); las crea la persona/orquestador (esta sesión de planner solo escribe en
  `docs/specs/`).

**Modificar**

- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — recibir `seedRecipes`, construir
  estado/encabezado y montar `ResultsScreen`; `onRetry`; pasos destino de `onBack`/`onChangeIngredients`.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — pasar `seedRecipes` a `GeneratorFlow`.
- `docs/technical-discovery.md` — registrar `ui.results` (fórmula `matchScore`, estados y encabezado).
- `docs/domain-model.md` — apuntar la fórmula de `matchScore` a `com.sazon.app.ui.results.MatchScore`.
- `DESIGN.md` — registrar las capturas aceptadas de `results-screen` en "Generated Concept Images".

**Eliminar**

- `app/src/main/java/com/sazon/app/ui/generator/GeneratorResultsPreview.kt` — sustituido por
  `ResultsScreen`; sin referencias (los *stages* Compose no tienen test JVM).

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain.recipe`,
`domain.validation`, `domain.generation`, `domain.generator`, `domain.prompt`, `domain.suggestions`,
`data/**`, `ui.navigation/MainNavigationState.kt`, `domain.profile`, la Taxonomía, `seed-recipes.json`
ni `init.sh`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` sección D +
  `docs/design/stitch/15-resultados.md`.
- Screens or states affected: Resultados poblados (desde Generador), Resultados con subtítulo de
  Búsqueda con IA, estado vacío y estado de error/modo degradado. Chip de orden y estado "bloqueado por
  perfil" quedan **fuera** de alcance.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing`, `SazonShapes`, `SazonPillShape`,
  `MaterialTheme.colorScheme.background`/`primary`/`tertiaryContainer` (franja ámbar, nunca rojo);
  cifras tabulares en metadatos y coincidencia; objetivos ≥48dp; una única acción principal terracota
  por pantalla.
- **Captura de referencia (obligatoria).** Existe una única captura de Stitch para esta pantalla, en
  `docs/design/concepts/results-screen/resultados/` (los tres ficheros del export de Stitch: el `.png`,
  el `.html` y el `design.md`).
  Stitch no generó variantes por estado: los estados poblado, con subtítulo de búsqueda, vacío y
  error/modo degradado (ver arriba) se implementan a partir de esta misma dirección visual.
  Ante cualquier contradicción, manda `DESIGN.md`. Un defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar `ui.results` (fórmula `matchScore`, estados,
  encabezado) y que `ResultsScreen` sustituye al preview.
- `docs/domain-model.md`: **update** — la fórmula de `matchScore` vive en
  `com.sazon.app.ui.results.MatchScore` (derivada, no persistida); sin cambios de entidades.
- `docs/risks-and-open-questions.md`: **update** — cerrar la cita de `matchScore` con la ubicación de la
  fórmula (la pregunta ya está marcada resuelta).
- `DESIGN.md` y `docs/design/concepts/`: **update** — registrar las capturas aceptadas de
  `results-screen`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión.

## Implementation Plan

1. Crear `MatchScore` (fórmula) y `MatchScoreTest` con el ejemplo trabajado y casos negativos.
2. Crear `ResultsStatus` + `from(...)` y `ResultsStatusTest` (poblado/vacío/degradado sin semilla).
3. Crear `ResultsOrigin`/`ResultsHeader` + `ResultsHeaders.of(...)` y `ResultsHeaderTest` (generador y
   búsqueda, singular/plural).
4. Crear `ResultRecipeCard` y `ResultsScreen` (tokens, sin iconos ni corazón).
5. Integrar en `GeneratorFlow` (seedRecipes, estado, encabezado, `onRetry`, `onBack`,
   `onChangeIngredients`), pasar `seedRecipes` desde `MainShell` y eliminar `GeneratorResultsPreview`.
6. Ejecutar `init.sh` en verde, actualizar `docs/technical-discovery.md`/`docs/domain-model.md`; el
   implementer registra evidencia y estado en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `MatchScore.of(...)` con pesos como constantes y penalización de `dislikedIngredientIds`.
- [ ] Escribir `MatchScoreTest` (ejemplo 86/76, rango `0..100`, sin ingredientes resueltos, sin
      ingredientes del usuario, cobertura por nombre normalizado).
- [ ] Crear `ResultsStatus` + `ResultsStatus.from(...)` (scoring + orden por relevancia).
- [ ] Escribir `ResultsStatusTest` (Content ordenado, Empty, Unavailable con y sin semilla, `discarded`
      nunca en `Content`).
- [ ] Crear `ResultsOrigin`/`ResultsHeader`/`ResultsHeaders.of(...)`.
- [ ] Escribir `ResultsHeaderTest` (generador 1/6 ingredientes, franja con y sin `maxMinutes`,
      búsqueda entre comillas).
- [ ] Crear `ResultRecipeCard` (imagen degradada 16:10, título, descripción, meta tabular, píldora).
- [ ] Crear `ResultsScreen` (topBar con "Atrás", bottomBar "Cambiar ingredientes", estados).
- [ ] Modificar `GeneratorFlow` (recibir `seedRecipes`, montar `ResultsScreen`, `onRetry`/`onBack`/
      `onChangeIngredients`).
- [ ] Modificar `MainShell` para pasar `seedRecipes` a `GeneratorFlow`.
- [ ] Eliminar `GeneratorResultsPreview.kt`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`/`docs/domain-model.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: suites nuevas en verde sin romper las existentes
  (234 en el último estado aceptado):
  - `MatchScoreTest` (**verificación exigida**): ejemplo 86 y 76; `0..100`; sin ingredientes resueltos
    no divide por cero; cobertura por `ingredientId` y por nombre normalizado; determinista.
  - `ResultsStatusTest`: `Content` con recetas ordenadas por `matchScore` desc; `Empty` con
    `accepted` vacío y sin fallo; `Unavailable` con y sin semilla; ninguna `discarded` en `Content`.
  - `ResultsHeaderTest`: subtítulo de Generador (singular/plural, franja con y sin `maxMinutes`) y de
    Búsqueda (título + consulta entre comillas).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `ui.results` sin imports Compose/Android en `MatchScore`/`ResultsStatus`/`ResultsHeader`;
  `GeneratorResultsPreview.kt` eliminado y sin referencias; `domain/**`, `data/**`, la Taxonomía,
  `seed-recipes.json`, `MainNavigationState.kt` y el build intactos; sin dependencias ni hex sueltos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Con `FakeRecipeGenerator` y un Perfil con gluten, el "Bocadillo de tomate" **no** aparece en
    Resultados (solo aceptadas) y las píldoras de coincidencia se ven.
  - **Estado vacío**: aparece cuando no hay recetas aceptadas.
  - **Estado de error ofrece reintentar**: "Reintentar" responde (modo degradado/sin semilla según el
    caso) y "Cambiar ingredientes" vuelve al paso de ingredientes conservando el borrador.
  - El acabado (imagen, píldora, franja ámbar, claro/oscuro) se compara con las capturas obligatorias.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; la lógica (fórmula, estados, encabezado) se cubre en JVM y el acabado lo revisa la persona.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos ni necesita modos nuevos.

## Evidence To Capture

- Salida de `./gradlew test` con `MatchScoreTest`, `ResultsStatusTest` y `ResultsHeaderTest`, nº de
  tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `ui.results` puro sin imports Compose/Android; `GeneratorResultsPreview.kt` eliminado;
  sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio, `data/**`, la Taxonomía
  ni `init.sh`.
- Resultado manual en emulador (descripción; captura opcional) de lista poblada, estado vacío y estado
  de error con "Reintentar".
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Fórmula de `matchScore` (verificación exigida)**: se fija como media ponderada de señales del
  Perfil y de los ingredientes del usuario (sección 1), derivada y **no persistida**, materializada en
  memoria con `copy`. Se eligen pesos simples y constantes en un único sitio, revisables con feature
  correctiva; es un heurístico de presentación, no una regla de seguridad.
- **Sin chips de orden**: fuera del `user_visible_behavior` y la verificación de la feature; los
  resultados van en orden estable por relevancia (`matchScore` desc). Quedan para `ai-search` o una
  feature correctiva.
- **Sin `PushedScreen.RESULTS`**: la pantalla se monta como *stage* de resultado de `GeneratorFlow`
  (ya es una página empujada sobre las pestañas) para **conservar los borradores** del Generador con el
  `Saver` existente; añadir el destino y una pila sin perder estado exigiría `navigation-compose` (fuera
  de alcance). `ResultsScreen` es reutilizable y `ai-search` montará su propio host (payload en
  `remember`) al iniciar Resultados desde la Home.
- **Un único estado de fallo** (`Unavailable`) con dos presentaciones según haya semilla (franja ámbar +
  lista, o titular de error), **ambas con "Reintentar"**: el dominio (`GenerationResult`) hoy no
  distingue "sin IA" de "error" (todo `Exception` → `aiUnavailable`); la distinción fina es la pregunta
  1 de `docs/risks-and-open-questions.md` y la afinará `real-ai-provider`. Así la verificación manual
  ("el estado de error ofrece reintentar") es alcanzable y `DESIGN.md` se respeta (nunca rojo).
- **Subtítulo de búsqueda listo pero no conectado**: `ResultsOrigin.Search` + `ResultsScreen` cubren la
  nota de la feature y su test JVM, pero el disparo real desde la Home es de `ai-search` (no-goal).
- **Sin corazón ni navegación a Detalle**: `favorites` y `recipe-detail-screen` son features propias;
  la tarjeta no es clicable para no prometer comportamiento no implementado.
- **Imagen placeholder**: el repositorio no tiene assets de receta; se reutiliza el degradado
  melocotón→oliva + glifo de plato de `RecipeCard` (documentado).
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Pesos y microcopy**: los pesos de `MatchScore` y el cuerpo exacto de los estados son un heurístico
  revisable; no cambian el contrato puro. El acabado lo valida la persona contra las capturas.
- **Distinción "error" vs "sin IA"**: se resolverá cuando `real-ai-provider` exponga tipos de error
  (pregunta 1); hoy se colapsan en `Unavailable`.
- **Process death mostrando Resultados**: el `result` vive en `remember` (no `rememberSaveable`) desde
  `generation-flow-with-validation`; rotar vuelve a los ajustes. No se cambia aquí.
- **Orden por relevancia**: si el producto pide los chips de orden, van en feature posterior.

## Validator Checklist

- [ ] `MatchScore` es puro, determinista, en `0..100`, y el ejemplo trabajado (86/76) se cumple
      (**verificación exigida**).
- [ ] La pantalla lista **solo** `accepted`, con imagen, título, descripción, meta y píldora de
      coincidencia (escenario 1); ninguna `discarded` se renderiza.
- [ ] El **estado vacío** aparece cuando no hay recetas (escenario 3) y el **estado de error ofrece
      reintentar** (escenarios 4/5; verificación manual).
- [ ] El subtítulo de la Búsqueda con IA se formatea con la consulta entre comillas (escenario 6).
- [ ] "Reintentar" relanza con la misma petición y "Cambiar ingredientes" vuelve al paso de
      ingredientes conservando el borrador (escenario 7).
- [ ] `ui.results` puro está cubierto en JVM con casos negativos; `GeneratorResultsPreview.kt`
      eliminado sin referencias.
- [ ] `domain/**`, `data/**`, la Taxonomía, `seed-recipes.json`, `MainNavigationState.kt`, el build y
      `init.sh` quedan intactos; sin dependencias nuevas, iconos ni hex sueltos.
- [ ] Para UI: `Visual Design Impact` nombra las capturas de `docs/design/concepts/results-screen/`, que
      deben **existir** antes de implementar (hoy pendientes → feature `blocked`).
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `docs/technical-discovery.md`
      y `docs/domain-model.md` quedan coherentes.
