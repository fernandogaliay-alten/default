# Feature Implementation Spec: Carrusel "Sugeridas para ti" personalizado y acotado

## Source Feature

- `id`: `home-suggestions-personalized`
- `area`: `ui`
- `depends_on`: `["home-offline-blocks", "suggestion-batch-cache", "results-screen"]` (las tres `accepted`;
  esta correctiva **no** reabre ni replanifica ninguna de ellas)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json` + petición directa de la persona (correctiva, 2026-10-13); la issue la
  asigna `github-flow` al provisionar la feature.
- Verificación exigida (de la feature): `./gradlew test` (vía `bash init.sh`) cubre en JVM el **tope de
  6**, el **orden por `MatchScore` descendente**, la **exclusión por restricciones duras o patrón de
  dieta** (caso negativo obligatorio con una semilla que incumple) y que **un lote ya resuelto vacío
  sigue siendo vacío** (no cae a semilla); inspección de que la lógica nueva de `ui.home` es pura, sin
  Compose/Android, reutiliza `MatchScore` y `RecipeValidator`, y de que no cambian
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`; prueba manual de emulador (persona).
- **Desviación de capturas autorizada por la persona**: esta correctiva **no** crea
  `docs/design/concepts/home-suggestions-personalized/` (mismo diseño visual; precedentes
  `recipe-artwork` #77, `favorites-sort-controls` #85, `profile-stats-heading` #86,
  `home-recently-opened-group` #91, `dish-image-progressive-ui` #98). Ver `Visual Design Impact`.

## Goal

Corregir el **contenido** del carrusel "Sugeridas para ti" de la Home sin cambiar su aspecto: hoy el
carrusel pinta **tal cual** todas las recetas del Lote (12 semillas + hasta 6 `ai`, hasta 18 tarjetas),
así que las semillas fijas dominan el carrusel y parece que las preferencias no se aplican.

Tras esta feature el carrusel muestra **como máximo 6** recetas **seguras** (descartadas las que
incumplen una **restricción dura** o el **patrón de dieta**, tanto semilla como `ai`) y **ordenadas por
relevancia con el Perfil** reutilizando la fórmula ya aceptada de `MatchScore`
(`ui/results/MatchScore.kt`: ingredientes preferidos, cocinas favoritas, franja de tiempo y dificultad,
penalizando —nunca descartando— los ingredientes que el usuario evita). La sección conserva su título
"Sugeridas para ti" y su microcopy "basado en tus gustos", y **el mismo diseño visual**: mismas
tarjetas, mismas imágenes y **sin** píldora de coincidencia nueva.

Toda la lógica nueva es **pura** (sin Compose ni Android) y vive en `com.sazon.app.ui.home`, de modo
que se testea en JVM sin emulador, sin dependencias nuevas y sin tocar dominio, persistencia ni el
build.

## Non-Goals

- **No** hay cambio visual: no se crean componentes, no se toca `HomeScreen`/`RecipeCard`/
  `RecipeCardFormat`, no se añade ninguna píldora ni contador ("6 de…") y no se cambia el orden de los
  bloques de la Home.
- **No** se materializa `matchScore` en las recetas del carrusel (`copy(matchScore = …)`): el score se
  usa **solo para ordenar**. Hoy las semillas y las recetas `ai` persistidas tienen `matchScore = null`
  y la píldora de `RecipeCard` no se pinta; se mantiene así (ver Decisiones).
- **No** se aplica el filtro de seguridad en la lista degradada de Resultados
  (`ResultsStatus.Unavailable`): queda fuera de alcance (resolución de la incógnita 1; ver Decisiones).
  `ui/results/**` **no** se modifica (solo se reutiliza `MatchScore` sin tocarlo).
- **No** se toca el dominio (`domain.validation`, `domain.suggestions`, `domain.recipe`,
  `domain.profile`), la persistencia (`data/**`), la Taxonomía, `seed-recipes.json` ni la política
  cache-first del Lote (`SuggestionBatchPolicy`/`SuggestionBatchService`).
- **No** se deduplica por título (el Lote ya deduplica en `SuggestionBatchService` y el catálogo semilla
  ya tiene ids únicos), ni se persiste nada, ni se filtra por preferencias **blandas** (solo penalizan
  el ranking vía `MatchScore`).
- **No** se implementan los chips de orden del carrusel ni el bloque "Tus recetas" (otros grupos,
  filtros locales o "Abiertas hace poco"): nada de eso cambia.
- **No** se añaden dependencias ni se toca `app/build.gradle.kts`, `gradle/libs.versions.toml` ni
  `init.sh`.
- **No** se crea `docs/design/concepts/home-suggestions-personalized/` ni su `README.md` (capturas
  exentas).

## Job Story

Cuando abro la Home queriendo ideas para cocinar,
quiero ver pocas sugerencias, seguras para mí y ordenadas por lo que me gusta (mis ingredientes, mis
cocinas, mi tiempo y mi nivel),
para confiar en que la app aplica mi Perfil en vez de enseñarme siempre las mismas recetas fijas.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo. Ve el carrusel ya personalizado; puede abrir una
  tarjeta (Detalle). No hay cuentas ni roles.
- **Desarrollador / agente**: cubre en JVM el tope, el orden, el filtro de seguridad (con caso negativo
  obligatorio) y el respaldo de semilla offline; verifica por inspección/grep la pureza de `ui.home`,
  la reutilización de `MatchScore`/`RecipeValidator` y la ausencia de cambios en el build.
- **Sistema**: sin red ni persistencia nueva; si el Lote no se ha resuelto, el respaldo son las semillas
  empaquetadas, tratadas por el mismo camino (filtro → orden → tope).

## Acceptance Scenarios

### Scenario 1: tope de 6 tarjetas (verificación exigida)

Given un Lote resuelto con 18 recetas seguras para el Perfil (o el catálogo semilla real, 12 recetas),
When se calcula la lista del carrusel,
Then `HomeSuggestions.visible(...)` devuelve **exactamente 6** (`MAX_VISIBLE`), nunca más, y nunca
recorta por debajo si hay menos: con 4 recetas seguras devuelve 4.

### Scenario 2: orden por relevancia descendente (verificación exigida)

Given dos recetas seguras donde una coincide con el Perfil en cocina favorita, franja de tiempo y
dificultad y la otra no,
When se calcula la lista,
Then la de mayor `MatchScore.of(recipe, preferences, taxonomy)` aparece antes; **con empates se
conserva el orden de origen** (orden estable: primero el Lote semilla, luego las `ai`).

### Scenario 3: exclusión por restricciones duras y patrón de dieta, semilla e IA (verificación exigida; caso negativo obligatorio)

Given un Perfil con `dietPatternId = "vegetarian"` y una receta **semilla** con `ingredientId = "pollo"`
(tag `poultry`), y dado un Perfil con `hardRestrictionIds = ["gluten"]` y una receta **semilla** con un
ingrediente libre `name = "Pan", ingredientId = null`, y una receta **`ai`** con `pollo`,
When se calcula la lista con cada Perfil,
Then **ninguna** de esas recetas aparece en el carrusel (descartadas por el `RecipeValidator` con
`DIET_PATTERN`/`HARD_RESTRICTION`), mientras que las recetas seguras del input **sí** aparecen; el caso
negativo **no pasa en vacío**: con un Perfil `omnivore` sin restricciones duras esas mismas recetas sí
aparecen.

### Scenario 4: un lote ya resuelto vacío sigue siendo vacío (verificación exigida)

Given el catálogo semilla con 12 recetas y `batchRecipes = emptyList()` (Lote resuelto sin recetas),
When se calcula la lista,
Then el resultado es **vacío**: no cae al respaldo de semilla (un Lote resuelto vacío es un resultado
real de la política cache-first, no una ausencia de dato).

### Scenario 5: el respaldo de semilla offline se acota y se filtra igual (resolución de la incógnita 2)

Given `batchRecipes = null` y el catálogo semilla real,
When se calcula la lista con un Perfil `omnivore` sin restricciones,
Then devuelve 6 tarjetas (tope aplicado a las 12 semillas); con un Perfil con
`hardRestrictionIds = ["gluten"]`, devuelve ≤ 6 y **ninguna** receta que el `RecipeValidator` descarte
(concretamente **no** aparece `seed-pasta-tomate`, cuyo ingrediente `pasta` y `declaredAllergens`
activan gluten).

### Scenario 6: las preferencias blandas penalizan, nunca descartan

Given dos recetas seguras idénticas salvo que una contiene un `ingredientId` de
`dislikedIngredientIds`,
When se calcula la lista,
Then **ambas aparecen** (el filtro no descarta por preferencia blanda) y la que contiene el ingrediente
evitado queda **después** (su `MatchScore` es menor por `DISLIKE_PENALTY`).

### Scenario 7: la sección no cambia de aspecto (verificación exigida por inspección)

Given la Home renderizada,
When se aplica la feature,
Then el título "Sugeridas para ti" y el microcopy "basado en tus gustos" siguen igual, las tarjetas son
las mismas (`RecipeCard`, imagen 16:10) y **no** aparece la píldora de coincidencia (las `Recipe`
devueltas conservan su `matchScore` original, `null` para semilla y `ai` persistida); no cambia ningún
otro bloque de la Home ni la navegación.

## Repository Research

### Files Inspected

- `feature_list.json` — entry `home-suggestions-personalized` (área `ui`, `depends_on` las tres
  `accepted`, verificación y nota con la causa raíz, el alcance acordado, la exención de capturas y las
  dos incógnitas a resolver); vecinas `home-offline-blocks`, `suggestion-batch-cache`, `results-screen`,
  `seed-catalog-50` (`not_started`, depende de esta: debe ir **después**).
- `PROGRESS.md` — último gate aceptado (90 suites / 660 tests); camino `init.sh`
  (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`); regla de no automatizar UI de emulador;
  política de capturas y sus desviaciones autorizadas.
- `AGENTS.md` — UI verificada por lógica/estado en JVM cuando sea posible; capturas obligatorias salvo
  desviación autorizada; una feature por sesión; nunca `./gradlew` directo.
- `CONTEXT.md` — "Restricción dura" (asunto **de seguridad**: si una receta la incumple, se descarta),
  "Patrón de dieta", "Preferencia blanda" (influye en el ranking, **nunca** descarta), "Ingredientes que
  evito" (preferencia blanda), "Lote de sugerencias", "Sugerencia".
- `docs/build-brief.md` — MVP: "ofrecer sugerencias relevantes en la pantalla principal a partir del
  Perfil"; Success Criteria: "ninguna receta mostrada incumple una Restricción dura marcada".
- `docs/domain-model.md` — Lote (`suggestionBatch` contiene 0..n `Recipe`); "Restricciones duras y
  Patrón de dieta se aplican siempre, las garantiza el Validador"; "preferencias blandas, cocinas, tiempo
  y dificultad **solo ordenan**; nunca filtran".
- `docs/domain/taxonomy.json` — `dietPatterns` (`vegetarian` excluye `meat/poultry/fish/seafood`;
  `vegan` añade `dairy/egg/honey/gelatin`); ingredientes reales `pollo` (tag `poultry`), `pasta` y `pan`
  (alérgeno `gluten`).
- `app/src/main/java/com/sazon/app/ui/home/HomeSuggestions.kt` — objeto puro actual:
  `visible(batchRecipes, seedRecipes) = batchRecipes ?: seedRecipes` (sin filtro, sin tope, sin orden);
  es el **único** punto a cambiar.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — único llamador (línea 146); posee `batch`
  (`suggestionBatchRepository.observe()`), `seedRecipes`, `profile.preferences` y `taxonomy`, y pasa la
  lista ya calculada a `HomeScreen(suggestions = …)`.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — `SuggestionsSection` pinta "Sugeridas para
  ti" + "basado en tus gustos" y un `LazyRow` de `RecipeCard` sobre `suggestions` (no cambia).
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` — pinta la píldora **solo** si
  `recipe.matchScore != null` (`RecipeCardFormat.matchBadge`); decisivo para "sin cambio visual".
- `app/src/main/java/com/sazon/app/ui/results/MatchScore.kt` — fórmula pura ya aceptada
  (`W_COVERAGE`/`W_LIKED`/`W_CUISINE`/`W_TIME`/`W_DIFFICULTY`, `DISLIKE_PENALTY = 10`,
  `of(recipe, preferences, taxonomy, userIngredients = emptyList())`); **no** se modifica.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — `validateRecipe(recipe,
  taxonomy, constraints): RecipeValidation` (`Accepted`/`Discarded`), con `RecipeConstraints(
  dietPatternId, hardRestrictionIds)`, reglas 1 (esquema/ids), 2 (dieta), 3 (restricciones duras) y 4
  (coherencia); **no** se modifica.
- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatchService.kt` — mapeo inline
  `RecipeConstraints(dietPatternId = profile.dietPatternId, hardRestrictionIds =
  profile.hardRestrictionIds.toSet())` (mismo mapeo en `GenerationService` y `AiSearchService`): patrón a
  reutilizar sin crear un mapper nuevo.
- `app/src/main/java/com/sazon/app/ui/results/ResultsStatus.kt` — `Unavailable(seedRecipes)` hoy
  **puntúa y ordena sin filtrar** (`scoreAndRank`), y `scoreAndRank` materializa `matchScore` con `copy`.
- `app/src/test/java/com/sazon/app/ui/results/ResultsStatusTest.kt` — **evidencia de la resolución 1**:
  sus fixtures (`recetaAlta`/`recetaBaja`) tienen `steps = emptyList()` con `totalMinutes = 25/90`, es
  decir **incoherentes** para la regla 4 del Validador; filtrar en `Unavailable` las descartaría y
  rompería ese contrato aceptado.
- `app/src/test/java/com/sazon/app/ui/home/HomeSuggestionsTest.kt` — 3 casos actuales
  (`null → seeds`, lote con recetas → lote, Lote vacío → vacío) y helper `receta(id)`; hay que adaptar
  las llamadas a la nueva firma y ampliar los escenarios.
- `app/src/test/java/com/sazon/app/domain/recipe/SeedRecipeCatalogTest.kt` — patrón de lectura de la
  Taxonomía (`taxonomy.asset.path`) y del catálogo semilla (`seed.recipes.asset.path`) por `File`, e
  integridad que garantiza que **las 12 semillas pasan** las reglas 1 y 4 del Validador
  (`declaredAllergens` completo y `|sum(steps) − totalMinutes| ≤ 5`).
- `app/src/main/assets/seed-recipes.json` — 12 semillas; `seed-pollo-al-ajillo`, `seed-tacos-de-pollo`
  (pollo) y `seed-pasta-tomate`/`seed-gazpacho-andaluz`/`seed-tortitas-de-avena`/`seed-tarta-de-manzana`
  (gluten) son los casos negativos reales.
- `app/build.gradle.kts` — `testDebugUnitTest` ya publica `taxonomy.asset.path` y
  `seed.recipes.asset.path` (líneas 130-131): los tests nuevos no necesitan cambios de build.
- `docs/technical-discovery.md` — describe el carrusel como "consume el Lote tal cual" y
  `HomeSuggestions.visible` (hay que actualizarlo).
- `docs/risks-and-open-questions.md` — registro de riesgos y preguntas abiertas (destino de la deuda de
  la resolución 1).
- `DESIGN.md` — "Core Screens" (Home, carrusel "Sugeridas para ti") + `Components` (`RecipeCard`) +
  sección **Generated Concept Images** con el patrón documentado de excepciones de capturas.
- `docs/design/concepts/home-offline-blocks/README.md` — petición de backfill con las pantallas
  `home` y `home-sin-conexion` (los ficheros aún **no** existen).

### Existing Patterns To Follow

- **Lógica pura en `ui.home` + capa Compose delgada**: los objetos puros no importan Compose/Android y
  se testean en JVM; `MainShell`/`HomeScreen` solo llaman y pintan.
- **Reutilizar lo aceptado**: `MatchScore` (formula del ranking) y `RecipeValidator` (seguridad) se
  usan tal cual; no se reimplementa ni se duplica su lógica.
- **Mapeo `PreferenceProfile → RecipeConstraints` inline**, como en `SuggestionBatchService` (no hay
  mapper compartido y no se crea uno aquí).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, casos negativos que fallarían si
  el código no hiciera nada; Taxonomía/catálogo reales leídos por las propiedades de sistema.
- **Memorización con `remember`** para trabajo derivado costoso en Composable (patrón ya usado en
  `HomeScreen` con `remember(profile, taxonomy)`).
- **Sin dependencias nuevas**; `init.sh` sin cambios (`./gradlew test` + `./gradlew lint`, sin procesos
  largos).

### Current Gaps

- El carrusel **no filtra por seguridad** y **no ordena por relevancia**: muestra el Lote tal cual
  (12 semillas fijas + hasta 6 `ai` = hasta 18 tarjetas), de ahí la percepción de que el Perfil no se
  aplica (causa raíz de la feature).
- No hay ningún tope de tarjetas para el carrusel ni ninguna métrica de "relevancia" aplicada en la
  Home (aunque `MatchScore` ya existe y está aceptado).
- No existe test que cubra el filtrado/orden/tope del carrusel ni el caso negativo con una semilla
  insegura.

## Technical Approach

Estrategia: **un único punto puro en `ui.home` + un cableado mínimo en `MainShell`**, reutilizando
`MatchScore` y `RecipeValidator`. Sin dependencias nuevas, sin tocar el dominio ni el build.

### 1. Contrato puro — `com.sazon.app.ui.home.HomeSuggestions` (`HomeSuggestions.kt`)

```kotlin
object HomeSuggestions {

    /** Tope de tarjetas del carrusel "Sugeridas para ti". */
    const val MAX_VISIBLE = 6

    fun visible(
        batchRecipes: List<Recipe>?,
        seedRecipes: List<Recipe>,
        preferences: PreferenceProfile,
        taxonomy: Taxonomy,
    ): List<Recipe>
}
```

Algoritmo (en este orden, determinista y sin reloj/`Random`):

1. `val source = batchRecipes ?: seedRecipes` — preserva el respaldo inmediato a semilla y la regla
   "un Lote resuelto **vacío** no cae a semilla" (`emptyList()` no es `null`).
2. `val constraints = RecipeConstraints(dietPatternId = preferences.dietPatternId,
   hardRestrictionIds = preferences.hardRestrictionIds.toSet())` — mismo mapeo inline que
   `SuggestionBatchService`; no se crea un mapper nuevo ni se toca el dominio.
3. **Filtro de seguridad**: conservar solo
   `RecipeValidator.validateRecipe(recipe, taxonomy, constraints) is RecipeValidation.Accepted`. Se
   aplica a **todas** las recetas (`seed` y `ai`); no se usa el `recipe` normalizado que devuelve el
   `Accepted` (la Home pinta la receta original).
4. **Ranking por relevancia**: decorar → ordenar → desordenar, con la puntuación calculada **una sola
   vez** por receta (`compareBy` re-evaluaría el selector en cada comparación):
   `source.map { it to MatchScore.of(it, preferences, taxonomy) }.sortedByDescending { it.second }
   .map { it.first }`. `userIngredients` se deja por defecto (`emptyList()`): en la Home no hay
   ingredientes declarados, así que la señal `coverage` no aplica (el denominador de `MatchScore` ya lo
   contempla). `sortedByDescending` es **estable**: los empates conservan el orden de origen.
5. **Tope**: `.take(MAX_VISIBLE)`.
6. **No** se hace `copy(matchScore = …)`: se devuelven las mismas instancias `Recipe` (con
   `matchScore` original, hoy `null`) → la píldora de `RecipeCard` **no** se pinta y el aspecto no
   cambia.

El KDoc del objeto se actualiza para describir filtro, orden, tope y la ausencia de materialización del
score. El contrato es **puro**: solo importa `domain.profile.PreferenceProfile`,
`domain.recipe.Recipe`, `domain.taxonomy.Taxonomy`,
`domain.validation.RecipeConstraints/RecipeValidation/RecipeValidator` y
`ui.results.MatchScore` (objeto puro); **cero** imports de Compose/Android.

### 2. Cableado — `com.sazon.app.ui.MainShell`

```kotlin
val suggestions = remember(batch?.recipes, seedRecipes, profile.preferences, taxonomy) {
    HomeSuggestions.visible(
        batchRecipes = batch?.recipes,
        seedRecipes = seedRecipes,
        preferences = profile.preferences,
        taxonomy = taxonomy,
    )
}
```

- Se añade `import androidx.compose.runtime.remember` (ya se usa `remember` en el fichero).
- El `remember(...)` evita re-ejecutar hasta 18 validaciones + 18 puntuaciones en recomposiciones no
  relacionadas (favoritos, contador de actividad, etc.). La lógica sigue siendo pura y testeable.
- No cambia nada más de `MainShell` (ni `HomeScreen` ni su firma: sigue recibiendo
  `suggestions: List<Recipe>` ya calculada).

### 3. Tests — `app/src/test/java/com/sazon/app/ui/home/HomeSuggestionsTest.kt`

Ampliar el fichero existente (mantener el estilo y los casos actuales adaptados a la nueva firma):

- Fixtures propias con `steps` que **sumen** `totalMinutes` (patrón `recetaValida()` de
  `RecipeValidatorTest`) y `declaredAllergens` coherentes con los ingredientes resueltos, para que el
  filtro de seguridad no descarte por regla 4 lo que el test quiere conservar.
- Perfil real (`PreferenceProfile`) y **Taxonomía real** por `taxonomy.asset.path`; para el respaldo
  semilla, **catálogo real** por `seed.recipes.asset.path` (patrón de `SeedRecipeCatalogTest`).

## Expected File Changes

**Modificar**

- `app/src/main/java/com/sazon/app/ui/home/HomeSuggestions.kt` — nueva firma de `visible(...)`,
  `MAX_VISIBLE = 6`, filtro por `RecipeValidator`, ranking por `MatchScore` y `take`; KDoc actualizado.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — llamada con `preferences`/`taxonomy` envuelta en
  `remember` (+ import); una sola línea de lógica.
- `app/src/test/java/com/sazon/app/ui/home/HomeSuggestionsTest.kt` — adaptar las 3 llamadas actuales y
  añadir los escenarios 1-6 (tope, orden estable, negativos de dieta y restricción dura con semilla y
  con `ai`, Lote vacío, respaldo semilla filtrado/acotado, "evito" penaliza y no descarta).
- `docs/technical-discovery.md` — el carrusel ya no "consume el Lote tal cual": tope 6, filtro por el
  `RecipeValidator` (restricciones duras + patrón de dieta), orden por `MatchScore` **sin**
  materializarlo, y respaldo semilla por el mismo camino.
- `docs/risks-and-open-questions.md` — registrar la **deuda de seguridad** de la resolución 1 (el modo
  degradado de Resultados sigue mostrando semillas sin filtro) con su recomendación de correctiva.
- `DESIGN.md` — registrar la **excepción de capturas** de `home-suggestions-personalized` (sección
  "Generated Concept Images", patrón de #77/#85/#86/#91/#98) y anotar el comportamiento vigente del
  carrusel (≤6 tarjetas relevantes al Perfil; mismo diseño, sin píldora nueva).
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar** (evidencia y estado `passing`),
  no en esta sesión de planificación.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`domain/**`, `data/**`, `ui/results/**` (solo se importa `MatchScore`), `ui/home/HomeScreen.kt`,
`RecipeCard.kt`, `RecipeCardFormat.kt`, `HomeRecipes.kt`, `HomeGreeting.kt`, `HomeIdentity.kt`,
`ui/navigation/**`, `ui.theme/**`, la Taxonomía, `seed-recipes.json`, `docs/design/**` y
`docs/design/concepts/**` (no se crea carpeta de capturas).

## Visual Design Impact

- UI involved: **sí** (comportamiento del carrusel "Sugeridas para ti"); **sin cambio visual**.
- Design source: `DESIGN.md` ("Core Screens" → Home; `Components` → `RecipeCard`) y la **dirección
  visual aplicable** = backfill de `home-offline-blocks` (`home`, `home-sin-conexion`, ver
  `docs/design/concepts/home-offline-blocks/README.md`; los ficheros aún **no** están exportados, el
  backfill sigue pendiente). Ante contradicción, manda `DESIGN.md`.
- Screens or states affected: carrusel "Sugeridas para ti" en la Home, con Lote resuelto y con respaldo
  de semilla offline; claro y oscuro. **No** cambian cabecera, buscador IA, héroe, "Tus recetas", barra
  inferior, FAB ni el Detalle.
- New design artifact required: **no**.
- **Capturas de UI — exención autorizada por la persona (misma clase que #77/#85/#86/#91/#98)**: esta
  feature **no** genera capturas y **no** se crea `docs/design/concepts/home-suggestions-personalized/`
  ni su `README.md`. Motivo: el diseño **no** cambia (mismo título, mismo microcopy, mismas tarjetas,
  misma imagen, **sin** píldora de coincidencia); solo cambia **qué** recetas entran y **en qué orden**,
  que es comportamiento verificable en JVM y por la persona en el emulador contra la dirección visual
  vigente.
- **`DESIGN.md` debe registrar, como parte del alcance**, la excepción de capturas (issue que asigne
  `github-flow`) y el comportamiento vigente del carrusel (≤6 sugerencias seguras ordenadas por
  relevancia con el Perfil; respaldo de semilla con el mismo camino; **sin** píldora en la Home). Es
  contra ese registro contra lo que la persona revisa el acabado en el emulador.
- Un defecto visual se corrige con una **feature correctiva** (regla de AGENTS.md), no bloquea el flujo.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo (la de seguridad
  ya existe: la garantiza el `RecipeValidator`).
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — `ui.home.HomeSuggestions` pasa a filtrar (Validador),
  ordenar (`MatchScore`, no materializado) y acotar a 6 el carrusel, también en el respaldo a semilla.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados (el Lote y el
  Perfil se consumen igual).
- `docs/risks-and-open-questions.md`: **update** — registrar la deuda de la resolución 1 (resultados
  degradados sin filtro de seguridad) y su correctiva recomendada.
- `DESIGN.md`: **update** — excepción de capturas + comportamiento vigente del carrusel.
- `docs/design/concepts/home-suggestions-personalized/`: **no se crea** (exención autorizada).
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no ahora.

## Implementation Plan

1. Reescribir `HomeSuggestions.visible(...)` con la nueva firma, `MAX_VISIBLE`, el filtro por
   `RecipeValidator`, el ranking decorado por `MatchScore` y el `take(6)`; actualizar el KDoc.
2. Adaptar `HomeSuggestionsTest` a la nueva firma y añadir los escenarios 1-6 (tope, orden estable,
   negativos de dieta/restricción dura con semilla y `ai`, Lote vacío, respaldo semilla con el catálogo
   real, "evito" penaliza y no descarta).
3. Cablear `MainShell` (llamada con `preferences`/`taxonomy` dentro de `remember`).
4. Ejecutar `bash init.sh` en verde; corregir lo que salga.
5. Actualizar `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`, y registrar la
   excepción de capturas y el comportamiento del carrusel en `DESIGN.md`.
6. Anotar evidencia y estado `passing` en `feature_list.json`/`PROGRESS.md` (lo hace el implementer).

## Implementation Tasks

- [ ] `HomeSuggestions`: añadir `MAX_VISIBLE = 6` y la nueva firma `visible(batchRecipes, seedRecipes,
      preferences, taxonomy)`.
- [ ] Implementar el pipeline: `batchRecipes ?: seedRecipes` → `RecipeConstraints` inline → filtro
      `RecipeValidator.validateRecipe(...) is Accepted` → ranking decorado por `MatchScore.of(...)` →
      `take(MAX_VISIBLE)`; sin `copy(matchScore = …)`.
- [ ] Actualizar el KDoc (filtro, orden, tope, sin materialización del score, Lote vacío ≠ fallback).
- [ ] Adaptar `HomeSuggestionsTest` a la nueva firma manteniendo los 3 casos actuales, **haciendo
      coherentes las fixtures** (`receta(id)` de hoy tiene `steps = emptyList()` con
      `totalMinutes = 20` y el filtro la descartaría por la regla 4; hay que darle pasos que sumen el
      total y `declaredAllergens` coherentes).
- [ ] Añadir los tests del escenario 1 (tope con fixtures de 7+ recetas seguras y con el catálogo real).
- [ ] Añadir el test del escenario 2 (orden descendente + empates estables).
- [ ] Añadir los tests del escenario 3: negativo de **dieta** (`vegetarian` + semilla con `pollo`) y de
      **restricción dura** (`gluten` + semilla con "Pan" libre), más el negativo con una receta `ai`; y
      el control positivo con Perfil `omnivore` sin restricciones (el caso negativo no pasa en vacío).
- [ ] Añadir el test del escenario 4 (Lote `emptyList()` → vacío).
- [ ] Añadir los tests del escenario 5 (respaldo `null` con el catálogo real: 6 tarjetas con `omnivore`;
      ≤ 6 y sin `seed-pasta-tomate` con `gluten`).
- [ ] Añadir el test del escenario 6 (`dislikedIngredientIds` penaliza y **no** descarta).
- [ ] Cablear `MainShell` con `remember(batch?.recipes, seedRecipes, profile.preferences, taxonomy)`.
- [ ] Ejecutar `bash init.sh` en verde y actualizar `docs/technical-discovery.md`,
      `docs/risks-and-open-questions.md` y `DESIGN.md`.

## Verification Plan

- `bash init.sh` → **esperado**: exit 0 (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`),
  sin procesos largos. Es el gate no bloqueante; **no cambia** y no necesita modos nuevos.
- `./gradlew test` (vía `init.sh`) → **esperado**: verdes las suites nuevas y las existentes (baseline
  del último estado aceptado: 90 suites / 660 tests). En `HomeSuggestionsTest`:
  - **Tope**: 7+ recetas seguras → `size == 6`; 4 recetas seguras → `size == 4`; catálogo semilla real
    con Perfil `omnivore` → `size == 6`.
  - **Orden**: la receta que coincide en cocina/tiempo/dificultad va antes que la que no; con empates se
    conserva el orden del input (caso negativo: no se invierte por azar).
  - **Seguridad (caso negativo obligatorio)**: con `dietPatternId = "vegetarian"` la semilla con
    `ingredientId = "pollo"` **no** aparece; con `hardRestrictionIds = ["gluten"]` la semilla con
    `name = "Pan", ingredientId = null` **no** aparece; una receta `ai` con `pollo` tampoco; y con
    Perfil `omnivore` sin restricciones esas mismas recetas **sí** aparecen (el test no pasa en vacío).
  - **Lote vacío**: `visible(emptyList(), semillas, …)` → vacío.
  - **Respaldo semilla**: `visible(null, catalogoReal, omnivore, taxonomia)` → 6; con `gluten`, ≤ 6 y sin
    `seed-pasta-tomate`.
  - **Preferencia blanda**: la receta con un ingrediente de `dislikedIngredientIds` sigue apareciendo y
    queda después de su gemela sin él.
  - **Sin materialización**: las recetas devueltas conservan `matchScore == null` (sin cambio visual).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y **sin warnings nuevos** en ficheros de la
  feature (los 13 preexistentes se mantienen).
- **Inspección/grep** (verificación exigida):
  - `ui/home/HomeSuggestions.kt` **sin** imports de Compose/Android y **usando**
    `com.sazon.app.ui.results.MatchScore` y `com.sazon.app.domain.validation.RecipeValidator`
    (`grep` de ambos).
  - `app/build.gradle.kts` y `gradle/libs.versions.toml` **intactos**; `git diff --stat` solo con los
    ficheros previstos; `domain/**`, `data/**`, `ui/results/**`, `ui/home/HomeScreen.kt`,
    `RecipeCard.kt` y `docs/design/**` sin diff; no existe
    `docs/design/concepts/home-suggestions-personalized/`.
- **Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI)** — **verificación exigida**:
  - Con Lote resuelto: el carrusel muestra **≤ 6** tarjetas acordes al Perfil (ninguna con un alérgeno
    del usuario ni contraria a su patrón de dieta) y las mejor puntuadas primero.
  - Con el respaldo de semilla offline (sin Lote): mismo comportamiento (≤ 6, filtradas, ordenadas).
  - Apariencia sin cambios: título "Sugeridas para ti" + "basado en tus gustos", tarjetas e imágenes
    iguales, sin píldora de coincidencia nueva, en claro y oscuro.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; esta feature introduce lógica **pura** (filtro, orden, tope) que se cubre al 100 % en JVM,
incluido el caso negativo obligatorio, y el acabado lo revisa la persona.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`) y
no arranca procesos largos.

## Evidence To Capture

- Salida de `./gradlew test` (vía `init.sh`): `HomeSuggestionsTest` con el nº de tests nuevo y
  **0 fallos/errores/saltados**; total de suites/tests y su delta contra la baseline.
- Salida de `lint`: 0 errores y **sin** warnings nuevos en ficheros de la feature; `init.sh` con
  código 0.
- Inspección: `ui.home` puro sin imports Compose/Android; `MatchScore` y `RecipeValidator` reutilizados
  (grep); `app/build.gradle.kts`/`gradle/libs.versions.toml` intactos; `git diff --stat` solo con lo
  previsto; `docs/design/concepts/home-suggestions-personalized/` no existe.
- `DESIGN.md` con la excepción de capturas y el comportamiento del carrusel registrados;
  `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` coherentes.
- Resultado manual en emulador (descripción; captura opcional): carrusel ≤ 6 tarjetas seguras y
  ordenadas con Lote y con respaldo semilla, sin cambios de aspecto.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar; **no** es la
  aceptación final).

## Decisions And Unknowns

### Decisión de la incógnita 1 — el filtro de seguridad se aplica **solo en la Home** en esta feature

- **Decisión**: `HomeSuggestions.visible(...)` filtra por `RecipeValidator`; la lista degradada de
  Resultados (`ResultsStatus.Unavailable`) **no** cambia en esta feature.
- **Por qué**: (a) el alcance acordado de la feature es el carrusel "Sugeridas para ti" y su verificación
  no incluye Resultados; (b) `results-screen` está **aceptada** y su contrato (semillas puntuadas y
  ordenadas sin filtrar) se quedaría alterado en silencio; (c) la evidencia es concluyente:
  `ResultsStatusTest` usa fixtures con `steps = emptyList()` y `totalMinutes = 25/90`, que el
  `RecipeValidator` descarta por `INCOHERENT` (regla 4, deriva > 5 min), así que filtrar `Unavailable`
  **vaciaría** hoy su lista degradada y rompería tests aceptados.
- **Consecuencia honesta**: el Success Criterion de `docs/build-brief.md` ("ninguna receta mostrada
  incumple una Restricción dura marcada") sigue **sin cumplirse** en esa pantalla. Se registra como
  **deuda de seguridad conocida** en `docs/risks-and-open-questions.md` y se recomienda una correctiva
  propia (p. ej. `results-seed-safety-filter`) que decida si el modo degradado debe filtrar y rehaga
  los fixtures de `ResultsStatusTest` (hoy incoherentes). No se da de alta en `feature_list.json` desde
  aquí (el planner no lo modifica).

### Decisión de la incógnita 2 — el respaldo de semilla offline (Lote nulo) se acota y se filtra igual

- **Decisión**: `batchRecipes == null` → las `seedRecipes` pasan por **el mismo pipeline** (filtro de
  seguridad → orden por `MatchScore` → `take(6)`). El respaldo no es un caso especial.
- **Por qué**: el `user_visible_behavior` de la feature aplica a "tanto las semilla como las de IA" y su
  verificación manual exige "≤ 6 tarjetas acordes al Perfil **tanto con el Lote resuelto como en el
  respaldo de semilla offline**"; además, con un Perfil con restricciones las 12 semillas fijas serían
  el peor caso de relevancia (justo la causa raíz de la feature).
- **Se preserva** la semántica ya aceptada y testeada: `batchRecipes = emptyList()` (Lote **resuelto**
  sin recetas) devuelve **vacío**, no cae a semilla.

### Decisiones fijadas (justificadas contra el repositorio)

- **Sin materializar `matchScore`**: `RecipeCard` pinta la píldora solo si `matchScore != null`; un
  `copy(matchScore = …)` la haría aparecer en el carrusel y rompería la premisa "sin cambio visual" que
  justifica la exención de capturas. El score se usa **solo** para ordenar. (Si la persona quiere la
  píldora en la Home, es una correctiva posterior con su propia revisión visual.)
- **Tope en un único sitio** (`MAX_VISIBLE = 6` en `HomeSuggestions`), **no** acoplado a
  `PromptBuilder.RECIPES_PER_GENERATION` (6 es el tamaño de una generación, no el tope del carrusel).
- **Filtro con `RecipeValidator.validateRecipe(...)`**, no con un reimplemento: es la capa de seguridad
  aceptada (`domain.validation`) y su aplicación es **idempotente** (las `ai` del Lote ya se validaron
  al generarse); el `recipe` normalizado que devuelve `Accepted` **no** se usa (la Home pinta la receta
  original).
- **`RecipeConstraints` inline** (mismo mapeo de 2 líneas que `SuggestionBatchService`/
  `GenerationService`/`AiSearchService`): no se crea un mapper compartido ni se toca el dominio.
- **Ranking decorado** (score calculado una vez por receta) en lugar de `sortedByDescending { MatchScore.of(...) }`,
  que re-evaluaría el selector en cada comparación; `sortedByDescending` es estable → empates
  deterministas.
- **Preferencias blandas solo ordenan**: `dislikedIngredientIds` penaliza (`DISLIKE_PENALTY`) pero
  **nunca** descarta (`CONTEXT.md`/`docs/domain-model.md`); el filtro solo mira dieta y restricciones
  duras.
- **`remember` en `MainShell`**: el trabajo derivado (hasta 18 validaciones + 18 puntuaciones) se
  memoriza por `(batch?.recipes, seedRecipes, profile.preferences, taxonomy)` para no repetirlo en
  recomposiciones no relacionadas; no cambia el contrato puro.
- **Sin dependencias nuevas y `init.sh` sin cambios**; el pipeline no toca build, dominio, persistencia
  ni UI de Resultados.

### Unknowns

- **Pesos y empates**: `MatchScore` (ya aceptado) decide el orden; su heurística y los empates estables
  son revisables con feature correctiva sin cambiar el contrato de `HomeSuggestions`.
- **Nº de seguras < 6**: con perfiles restrictivos (p. ej. vegano + gluten) puede haber menos de 6
  semillas seguras y el carrusel mostrará 4-5 tarjetas. Es el comportamiento correcto ("como máximo 6");
  la ampliación `seed-catalog-50` (que depende de esta feature) dará más variedad.
- **Coste del re-validado en recomposición**: mitigado con `remember`; si en el futuro el Lote crece
  mucho (p. ej. `seed-catalog-50`), convendría un `ViewModel`/derived state. No bloquea.
- **Deuda de seguridad de Resultados** (resolución 1): queda registrada en
  `docs/risks-and-open-questions.md` como correctiva recomendada, no se resuelve aquí.
- **Revisión visual**: el acabado (densidad del carrusel, tarjetas, claro/oscuro) lo revisa la persona
  contra la dirección visual vigente (backfill de `home-offline-blocks` + `DESIGN.md`).

## Validator Checklist

- [ ] La implementación se queda en `ui/home/HomeSuggestions.kt` + el cableado de `MainShell.kt` +
      `HomeSuggestionsTest.kt` (+ docs); **no** toca `ui.results/**`, `HomeScreen.kt`, `RecipeCard.kt`,
      dominio, `data/**` ni el build.
- [ ] El carrusel devuelve **≤ 6** recetas (`MAX_VISIBLE`), nunca más, y no recorta cuando hay menos
      (Scenario 1; verificación exigida).
- [ ] El orden es por `MatchScore` **descendente** y **estable** en empates (Scenario 2; verificación
      exigida).
- [ ] Se excluyen las recetas que incumplen **restricción dura** o **patrón de dieta**, tanto semilla
      como `ai`, con **caso negativo obligatorio** que no pasa en vacío (Scenario 3; verificación
      exigida).
- [ ] Un Lote resuelto **vacío** sigue siendo vacío y **no** cae a semilla (Scenario 4; verificación
      exigida).
- [ ] El **respaldo de semilla offline** (`batchRecipes == null`) se filtra, se ordena y se acota con el
      mismo camino (Scenario 5; resolución de la incógnita 2).
- [ ] Los ingredientes de `dislikedIngredientIds` **penalizan y no descartan** (Scenario 6).
- [ ] **Sin cambio visual**: `RecipeCard`/`HomeScreen` intactos, sin materializar `matchScore` (las
      recetas devueltas conservan `matchScore == null`) y sin píldora nueva; título y microcopy iguales
      (Scenario 7).
- [ ] La lógica nueva de `ui.home` es **pura** (cero imports Compose/Android) y **reutiliza**
      `MatchScore` y `RecipeValidator` (verificación exigida por inspección).
- [ ] **No** hay dependencias nuevas (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos) ni
      cambios en `init.sh`.
- [ ] Para UI: `Visual Design Impact` registra la **exención de capturas** autorizada, **no** se crea
      `docs/design/concepts/home-suggestions-personalized/`, y `DESIGN.md` registra la excepción y el
      comportamiento del carrusel.
- [ ] La deuda de seguridad de Resultados (resolución 1) queda registrada en
      `docs/risks-and-open-questions.md` y **no** se ha "arreglado" fuera de alcance.
- [ ] `init.sh` en verde (evidencia con el delta de suites/tests), `feature_list.json` en `passing` y
      `PROGRESS.md` coherentes; `docs/technical-discovery.md` actualizado.
