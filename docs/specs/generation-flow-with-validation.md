# Feature Implementation Spec: Generación con validación y estado de carga

## Source Feature

- `id`: `generation-flow-with-validation`
- `area`: `ui`
- `depends_on`: `["generator-settings-step", "prompt-builder", "fake-recipe-generator", "recipe-validator"]`
  (las cuatro `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Cerrar el **corte vertical principal del MVP**: cuando el usuario pulsa **"Generar recetas"** en el
paso de ajustes, la app **pide recetas al generador** (`RecipeGenerator.generateFromIngredients`),
**descarta** las que incumplen el Patrón de dieta o las Restricciones duras con el `RecipeValidator`,
y **muestra solo las aceptadas**. Mientras se genera, se pinta un **estado de carga** cálido con un
botón **"Cancelar"** que responde.

La feature entrega: (1) la **orquestación pura** generador → validador (`GenerationService`),
testeable en JVM, que garantiza el caso negativo obligatorio del `build-brief` (una receta insegura
**no** llega a la lista aceptada); (2) el **mapeo puro** del borrador del Generador (ingredientes +
ajustes) a los tipos de dominio; (3) la **capa Compose delgada** del estado de carga/cancelación y de
un **preview mínimo** de resultados, dentro de `GeneratorFlow`. No se conecta red ni proveedor real:
el generador conectado sigue siendo `FakeRecipeGenerator`.

## Non-Goals

- **No** se construye la **pantalla de Resultados real** (`results-screen`): el orden (chips
  "Relevancia / Más rápida…"), las imágenes de receta, el `matchScore`, el subtítulo desde la
  Búsqueda con IA y los estados vacío/error/degradado completos son de `results-screen`. Aquí solo hay
  un **preview mínimo** (seam) que lista los títulos aceptados y vuelve a los ajustes.
- **No** se calcula `matchScore` (lo fija `results-screen`) ni se ordena ni se puntúa nada.
- **No** se implementa la **Búsqueda con IA** (`ai-search`): aquí solo el camino `generateFromIngredients`.
- **No** se implementa el **proveedor real** (`real-ai-provider`): sin HTTP, sin red, sin clave; el
  generador es `FakeRecipeGenerator`.
- **No** se invoca `PromptBuilder` desde esta feature: el puerto `RecipeGenerator` no recibe prompt y
  la implementación real lo compondrá dentro de sus operaciones. `prompt-builder` es dependencia de
  orden (la consume `real-ai-provider`), no una llamada de este corte.
- **No** se usa `RecipeValidator.validateResponse`: el puerto ya devuelve `List<Recipe>`, así que se
  valida receta a receta con `validateRecipe` (mismo patrón que `SuggestionBatchService`).
- **No** se persiste nada: ni las recetas generadas, ni favoritos, ni el Resumen de actividad
  (`favorites`, `activity-summary`). El resultado vive en memoria durante la sesión del Generador.
- **No** se toca el Lote de sugerencias, `domain.suggestions`, `data/**`, `domain.recipe`,
  `domain.validation`, `domain.generator` (incluido `GenerationConstraints`), `domain.prompt`, la
  Taxonomía, `seed-recipes.json` ni el Perfil/Room.
- **No** se añaden `navigation-compose`, iconos (`material-icons-*`), imágenes ni DI; **sin
  dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos).
- **No** se cambia `init.sh`.

## Job Story

Cuando he ajustado mis ingredientes y mis preferencias en el Generador,
quiero pedir recetas y ver un estado de carga que pueda cancelar,
para recibir solo recetas que respetan mi dieta y mis alergias, sin esperar a ciegas.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; no hay cuentas ni permisos. Pulsa "Generar recetas"
  y puede "Cancelar" mientras se genera.
- **Desarrollador / agente**: cubre la orquestación (generador → validador) con tests JVM, incluido el
  **caso negativo obligatorio**; el estado de carga/cancelación y el acabado visual los revisa la
  persona en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: llama al generador en best-effort; si el generador falla, marca `aiUnavailable` sin
  lanzar; si el usuario cancela, la corrutina se cancela limpiamente.

## Acceptance Scenarios

### Scenario 1: generación segura (camino feliz)

Given la Taxonomía real, un `PreferenceProfile` omnívoro sin restricciones duras y el
`FakeRecipeGenerator`,
When se genera desde ingredientes con una petición válida,
Then `aiUnavailable` es `false`, `accepted` contiene las recetas del falso seguras y **coherentes**
("Arroz salteado con verduras", "Lentejas estofadas" y "Bocadillo de tomate") y "Pollo al ajillo"
aparece en `discarded` por `INCOHERENT` — **hallazgo**: sus pasos suman 25 min frente a
`totalMinutes = 35` (ver Finding más abajo).

### Scenario 2: una receta insegura no llega a la lista aceptada (verificación exigida)

Given un Perfil con `hardRestrictionIds = ["gluten"]` y el `FakeRecipeGenerator` (cuya receta
"Bocadillo de tomate" lleva un ingrediente libre "Pan" que activa el gluten),
When se genera,
Then esa receta **no** está en `accepted`, sí aparece en `discarded` con
`DiscardReason.HARD_RESTRICTION` y el resto de recetas seguras sí entran en `accepted`.

### Scenario 3: el Patrón de dieta también descarta

Given un Perfil `dietPatternId = "vegetarian"` y el `FakeRecipeGenerator` (que devuelve "Pollo al
ajillo" con `ingredientId = "pollo"`, tag `poultry`),
When se genera,
Then "Pollo al ajillo" se descarta con `DiscardReason.DIET_PATTERN` y no entra en `accepted`.

### Scenario 4: se honran las alergias **por generación** (unknown de `generator-settings-step`)

Given un Perfil **sin** gluten pero un `GeneratorSettingsState.hardRestrictionIds = ["gluten"]`,
When el mapeo construye las `GenerationConstraints` y se genera,
Then la restricción efectiva es la del paso de ajustes (no la del Perfil): "Bocadillo de tomate" se
descarta. Al revés (quitar una alergia del perfil en el paso de ajustes) tampoco se aplica en esa
generación.

### Scenario 5: estado de carga y Cancelar (verificación manual)

Given el Generador en el paso de ajustes con al menos un ingrediente,
When se pulsa "Generar recetas",
Then se muestra la pantalla de generación ("Buscando recetas para ti…", sublíneas rotatorias y
"Cancelar") y, al pulsar "Cancelar", se vuelve a los ajustes sin mostrar resultados y sin error.

### Scenario 6: la cancelación se propaga

Given un generador que suspende indefinidamente,
When se cancela la corrutina de generación,
Then `GenerationService.generate` **re-lanza** `CancellationException` (no la traga) y el huésped
vuelve al paso de ajustes.

### Scenario 7: IA no disponible (modo degradado)

Given un generador que lanza una `Exception` (sin clave/red/error del proveedor),
When se genera,
Then `GenerationResult.aiUnavailable` es `true`, `accepted` queda vacío y no se propaga la excepción;
el preview muestra el aviso de modo degradado.

### Scenario 8: el preview muestra solo las aceptadas

Given un `GenerationResult` con `accepted` y `discarded`,
When se pinta el preview,
Then solo se listan las recetas de `accepted` (ninguna descartada), con título y línea de metadatos.

## Repository Research

### Files Inspected

- `feature_list.json` — `generation-flow-with-validation` (nota: "caso negativo obligatorio del
  `build-brief`; es el corte vertical principal del MVP"), verificación exigida y vecinas
  (`results-screen`, `ai-search`, `real-ai-provider`).
- `PROGRESS.md` — `generator-settings-step` y `profile-screen` aceptadas; el primario "Generar
  recetas" quedó **sin efecto** "la generación es de `generation-flow-with-validation`"; verificación
  por JVM, sin UI de emulador automatizada.
- `AGENTS.md` — una feature por sesión; UI verificada por lógica/estado en JVM; commit/push/PR fuera
  del planner.
- `docs/build-brief.md` — MVP slice 3–4 (Generador → validación), caso negativo obligatorio y plan de
  validación.
- `docs/domain-model.md` — escenario 2 ("Generar con ingredientes"), escenario 3 (restricción dura
  incumplida), estado `ai_available`/`ai_unavailable` y resultado de validación
  `accepted | discarded(...)`.
- `docs/domain/taxonomy.md` §5 — reglas 1–4 del validador (orden).
- `docs/design/stitch-prompt.md` sección C "Generating state (full screen)" y sección D — animación
  cálida, "Buscando recetas para ti…", sublíneas rotatorias, "Cancelar"; y el estado degradado.
- `DESIGN.md` — una acción principal terracota, objetivos ≥48dp, cifras tabulares, estados de carga
  cuidados, aviso degradado ámbar (nunca rojo).
- `app/src/main/java/com/sazon/app/domain/generator/{RecipeGenerator,GenerationConstraints}.kt` — el
  puerto (3 operaciones `suspend`, `generateFromIngredients(ingredients: List<RecipeIngredient>,
  constraints)`; errores abiertos, sin envoltorio) y los ajustes mínimos (`profile`, `servings`, …).
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — `RecipeConstraints`,
  `DiscardReason`, `DiscardedRecipe`, `RecipeValidation`, `validateRecipe`.
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` + `PromptIngredient`/
  `PromptRequest` — no se invoca aquí; queda documentada la razón.
- `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt` — 4 fixtures
  deterministas, incluida la insegura "Bocadillo de tomate" (Pan libre → gluten) y "Pollo al ajillo"
  (tag `poultry`); ignora la entrada y no filtra. **Finding**: "Pollo al ajillo" declara
  `totalMinutes = 35` pero sus pasos suman 25 min, así que el `RecipeValidator` lo descarta siempre por
  `INCOHERENT` (ver Decisions And Unknowns).
- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatchService.kt` — **patrón de
  referencia**: mapear el Perfil a `RecipeConstraints`, filtrar con `RecipeValidator.validateRecipe`,
  re-lanzar `CancellationException` y capturar `Exception` como degradación.
- `app/src/main/java/com/sazon/app/ui/generator/{GeneratorFlow,GeneratorIngredientsState,
  GeneratorSettingsState,GeneratorIngredient}.kt` — el huésped, los borradores y su `Saver`; el
  primario del paso 2 no tiene efecto hoy.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorSettingsScreen.kt` — la pantalla del paso 2.
- `app/src/main/java/com/sazon/app/ui/{MainShell,SazonApp}.kt` y `MainActivity.kt` — composición
  manual (sin DI) y paso de servicios por parámetro (`SuggestionBatchService`).
- `app/src/main/java/com/sazon/app/ui/navigation/MainNavigationState.kt` — `PushedScreen` (solo
  `GENERATOR`) con el comentario "crecerá con `RESULTS`/`DETAIL` en sus features".
- `app/src/main/java/com/sazon/app/ui/home/RecipeCardFormat.kt` — `metaLine(taxonomy, recipe)` puro,
  reutilizable para las filas del preview.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `Recipe`, `RecipeIngredient`,
  `RecipeSource`, `MeasureUnit`.
- `app/src/test/java/com/sazon/app/domain/suggestions/SuggestionBatchServiceTest.kt` — patrón de test
  JVM: `RecipeGenerator` de prueba, `perfil(...)`, `aiRecipe(...)` y Taxonomía real por
  `System.getProperty("taxonomy.asset.path")`.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: el servicio y los mapeos no importan Compose/Android y se
  testean en JVM; los `@Composable` solo pintan y delegan.
- **Servicio de dominio con puerto inyectado** (`SuggestionBatchService`): constructor con
  `generator` + `taxonomy`; `CancellationException` re-lanzada; `Exception` → degradación sin
  propagar.
- **Composición manual sin DI**: `MainActivity` crea el servicio y lo pasa por `SazonApp` →
  `MainShell` → `GeneratorFlow`, como ya hace con `SuggestionBatchService`.
- **Reutilización**: `RecipeCardFormat.metaLine` para la línea de metadatos; tokens del tema
  (`SazonTheme`, `SazonSpacing`, `SazonPillShape`); sin hex sueltos ni iconos ("Atrás"/"Cancelar" son
  texto).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_` y casos negativos que fallarían
  si la lógica no hiciera nada.
- **Sin dependencias nuevas**; `init.sh` sin cambios (`./gradlew test` + `./gradlew lint`).

### Current Gaps

- El primario "Generar recetas" del paso 2 **no tiene efecto**: no hay orquestación ni llamada al
  generador.
- No existe un servicio que cruce `RecipeGenerator` con `RecipeValidator` para el flujo principal
  (solo el Lote lo hace con `suggestByPreferences`).
- No hay mapeo de `GeneratorIngredient` (Taxonomía/libre) a `RecipeIngredient` ni de
  `GeneratorSettingsState` + Perfil a `GenerationConstraints`.
- No hay estado de carga, ni cancelación, ni presentación de resultados en el Generador.

## Technical Approach

Estrategia: **servicio de dominio puro + mapeo puro + huésped Compose delgado**, sin dependencias
nuevas ni cambios de dominio ya aceptado.

### 1. Orquestación pura — `com.sazon.app.domain.generation`

Nuevo paquete de dominio (sin Android, sin red, sin persistencia):

```kotlin
data class GenerationResult(
    val accepted: List<Recipe>,
    val discarded: List<DiscardedRecipe>,
    val aiUnavailable: Boolean = false,
)

class GenerationService(
    private val generator: RecipeGenerator,
    private val taxonomy: Taxonomy,
) {
    suspend fun generate(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): GenerationResult
}
```

- `generate` construye `RecipeConstraints(dietPatternId = constraints.profile.dietPatternId,
  hardRestrictionIds = constraints.profile.hardRestrictionIds.toSet())` — así las **restricciones
  efectivas** son las del `profile` que viene en las constraints (ver Decisión "Alergias por
  generación").
- Llama a `generator.generateFromIngredients(ingredients, constraints)`. La `CancellationException`
  se **re-lanza**; cualquier otra `Exception` devuelve `GenerationResult(emptyList(), emptyList(),
  aiUnavailable = true)` sin propagar (modo degradado esperado, como el Lote).
- Valida cada receta devuelta con `RecipeValidator.validateRecipe(recipe, taxonomy,
  recipeConstraints)`: `Accepted` → `accepted`; `Discarded` → `DiscardedRecipe(reason, detail,
  recipe.title)` en `discarded`. Nunca lanza por una receta mala.

### 2. Mapeo puro — `com.sazon.app.ui.generator`

`GeneratorRequestFactory` (objeto puro, sin Compose/Android; solo `domain.*`):

```kotlin
object GeneratorRequestFactory {
    fun ingredients(state: GeneratorIngredientsState): List<RecipeIngredient>
    fun constraints(settings: GeneratorSettingsState, profile: PreferenceProfile): GenerationConstraints
}
```

- `ingredients`: cada `GeneratorIngredient(name, ingredientId)` →
  `RecipeIngredient(name = name, quantity = 0.0, unit = null, isPantry = false,
  ingredientId = ingredientId)`. La cantidad/unidad del **input** no existen (las decide el modelo);
  de ahí `0.0`/`null` explícitos (ver Decisión).
- `constraints`: `GenerationConstraints(profile = profile.copy(hardRestrictionIds =
  settings.hardRestrictionIds), servings = settings.servings, mealTypeId = settings.mealTypeId,
  timeBucketId = settings.timeBucketId, difficultyId = settings.difficultyId,
  notes = settings.notes.ifBlank { null })`. El `profile` **efectivo** sustituye las restricciones del
  Perfil por las del paso de ajustes (que arrancan copiadas del Perfil), de modo que tanto el prompt
  (proveedor real) como el validador ven la selección por generación. Sin cambiar
  `GenerationConstraints`.

`GeneratorProgressMessages` (objeto puro) compone el microcopy del estado de carga: la cabecera fija
`"Buscando recetas para ti…"` y las sublíneas rotatorias a partir de la Taxonomía y los ajustes, p. ej.
"Combinando tus ingredientes", "Ajustando a {maxMinutes} minutos" (o "Ajustando el tiempo" si la franja
no tiene `maxMinutes`) y "Evitando tus alergias…" (solo si `hardRestrictionIds` no está vacía).

### 3. Huésped Compose — `com.sazon.app.ui.generator`

- `GeneratorFlow(..., generationService: GenerationService)` mantiene el **stage** de generación en
  la capa Compose (junto a los borradores ya existentes):
  - `var result: GenerationResult? by remember { mutableStateOf(null) }`
  - `var generationId: Int? by remember { mutableStateOf(null) }` (token; `null` = sin generación)
  - `LaunchedEffect(generationId)`: si `null`, no hace nada; si no, pinta el estado de carga, llama a
    `generationService.generate(GeneratorRequestFactory.ingredients(ingredients),
    GeneratorRequestFactory.constraints(settings, preferences))`, espera una **duración mínima
    visible** y fija `result`.
  - "Generar recetas" en el paso `SETTINGS` → `generationId = (generationId ?: 0) + 1` (en lugar de
    no hacer nada). En `INGREDIENTS` sigue siendo `step.next()`.
  - "Cancelar" → `generationId = null` (cancela la corrutina del `LaunchedEffect`) + `result = null`;
    vuelve a los ajustes. La cancelación se propaga por el servicio.
  - Pintado: si `result != null` → `GeneratorResultsPreview`; si `generationId != null` →
    `GeneratorGeneratingScreen`; si no → el `Scaffold` de edición actual (pasos 1 y 2).
- `GeneratorGeneratingScreen(headline, subLines, onCancel)`: pantalla completa sobre
  `colorScheme.background`, animación cálida terracota dibujada con Compose
  (`CircularProgressIndicator`/`Canvas`, sin iconos ni imágenes), titular Manrope, sublíneas
  rotatorias con `LaunchedEffect`+`delay`, y `TextButton("Cancelar")` (secundario, no la acción
  principal).
- `GeneratorResultsPreview(result, taxonomy, onBack, onChangeIngredients)`: **seam mínimo** (no es la
  pantalla de Resultados). Titular "Estas son tus recetas"; si `aiUnavailable`, línea de aviso (ámbar
  discreto); si no, lista de `accepted` con título + `RecipeCardFormat.metaLine(taxonomy, recipe)` y,
  si está vacía, una línea de estado vacío. Acciones: "Cambiar ingredientes" (vuelve a los ajustes) y
  "Atrás" (`onClose`). Sin imágenes, sin `matchScore`, sin orden ni estados de error completos (de
  `results-screen`).
- `MIN_VISIBLE_MILLIS` (~900 ms) en la capa Compose: garantiza que el estado de carga y "Cancelar"
  sean **observables** con el `FakeRecipeGenerator` (instantáneo) y evita un parpadeo con el
  proveedor real.

### 4. Composición e integración

- `MainActivity` compone `GenerationService(generator = FakeRecipeGenerator(), taxonomy = taxonomy)`
  (sin DI) y lo pasa a `SazonApp`.
- `SazonApp` recibe `generationService` y lo pasa a `MainShell`.
- `MainShell` lo pasa a `GeneratorFlow`. No se añade `PushedScreen.RESULTS`: `results-screen` es la
  dueña de la pantalla de Resultados real y de su destino (el enum ya lo anticipa).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/domain/generation/GenerationResult.kt` — resultado puro
  (`accepted`, `discarded`, `aiUnavailable`).
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` — orquestación
  generador → validador.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorRequestFactory.kt` — mapeo puro
  borrador → `RecipeIngredient`/`GenerationConstraints`.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorProgressMessages.kt` — microcopy puro del
  estado de carga.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorGeneratingScreen.kt` — estado de carga +
  Cancelar.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorResultsPreview.kt` — preview mínimo (seam).
- `app/src/test/java/com/sazon/app/domain/generation/GenerationServiceTest.kt` — escenarios 1–4, 6–7.
- `app/src/test/java/com/sazon/app/ui/generator/GeneratorRequestFactoryTest.kt` — mapeo (escenario 4)
  con casos negativos.
- `app/src/test/java/com/sazon/app/ui/generator/GeneratorProgressMessagesTest.kt` — sublíneas puras.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — `generationService`, stage de
  generación, disparo, cancelación, estado de carga y preview; se conservan los `Saver` de los
  borradores.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — recibir/pasar `generationService`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — recibir/pasar `generationService`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — componer `GenerationService`.
- `docs/technical-discovery.md` — registrar `domain.generation` y la orquestación en `ui.generator`.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain.recipe`,
`domain.validation`, `domain.generator` (`GenerationConstraints`), `domain.prompt`,
`domain.suggestions`, `data/**`, `ui.navigation/MainNavigationState.kt`, la Taxonomía,
`seed-recipes.json` ni `init.sh`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` secciones C ("Generating state") y D.
- Screens or states affected: estado de generación a pantalla completa (animación cálida terracota,
  titular, sublíneas rotatorias, "Cancelar") y preview de resultados dentro del Generador.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing`, `SazonShapes`, `SazonPillShape`,
  `MaterialTheme.colorScheme.background`/`primary`/`tertiaryContainer` (aviso degradado, nunca rojo);
  cifras tabulares en la línea de metadatos.
- Reglas visuales: una única acción principal terracota por pantalla; objetivos táctiles ≥48dp;
  tipografía dinámica (scroll). "Cancelar" y "Atrás" son texto (sin iconos).
- Capturas de referencia (backfill PR #37): `docs/design/concepts/generation-flow-with-validation/` —
  pantallas `generador-cargando` y `resultados-preview`. Pendiente de exportar desde Stitch (petición en
  el `README.md` de la carpeta); el acabado (animación, ritmo de las sublíneas, densidad del preview) lo
  revisa la persona tras el merge y un defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar `domain.generation` (orquestación) y la
  generación en `ui.generator` (disparo, cancelación, estado de carga y preview).
- `docs/domain-model.md`: **not needed** — el Generador, `GenerationConstraints`, el Validador y el
  modo `ai_unavailable` ya están descritos; no cambia el modelo.
- `DESIGN.md` y `docs/design/concepts/`: **update** — `DESIGN.md` sigue siendo la fuente; se añade el
  backfill de capturas `generador-cargando` y `resultados-preview` en
  `docs/design/concepts/generation-flow-with-validation/`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión.

## Implementation Plan

1. Crear `GenerationResult` y `GenerationService` (generador → validador, cancelación y degradación).
2. Escribir `GenerationServiceTest` con el **caso negativo obligatorio** (Pan → gluten), la dieta, las
   restricciones por generación, la cancelación y la IA no disponible.
3. Crear `GeneratorRequestFactory` y su test (mapeo y `profile.copy` de restricciones).
4. Crear `GeneratorProgressMessages` y su test.
5. Crear `GeneratorGeneratingScreen` y `GeneratorResultsPreview`.
6. Ampliar `GeneratorFlow` (stage, disparo, cancelación, carga y preview) y encadenar
   `generationService` por `MainShell`/`SazonApp`/`MainActivity`.
7. Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`; el implementer registra
   evidencia y estado en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `GenerationResult` (`accepted`, `discarded`, `aiUnavailable`) en `domain.generation`.
- [ ] Crear `GenerationService(generator, taxonomy).generate(ingredients, constraints)`: mapear a
      `RecipeConstraints`, re-lanzar `CancellationException`, capturar `Exception` como
      `aiUnavailable`, validar receta a receta con `RecipeValidator.validateRecipe`.
- [ ] Escribir `GenerationServiceTest` (escenarios 1–4, 6–7) con el `FakeRecipeGenerator` y la
      Taxonomía real por `taxonomy.asset.path`, incluido el negativo Pan → gluten.
- [ ] Crear `GeneratorRequestFactory` con `ingredients(...)` y `constraints(...)` (Perfil efectivo con
      las restricciones del paso de ajustes; notas en blanco → `null`).
- [ ] Escribir `GeneratorRequestFactoryTest` (mapeo de libres/Taxonomía y sustitución de
      restricciones) con casos negativos.
- [ ] Crear `GeneratorProgressMessages` (cabecera + sublíneas) y su test.
- [ ] Crear `GeneratorGeneratingScreen` (animación, sublíneas rotatorias, "Cancelar").
- [ ] Crear `GeneratorResultsPreview` (solo `accepted`; vacío y `aiUnavailable`).
- [ ] Ampliar `GeneratorFlow` con `generationService`, el token de generación, el disparo desde
      "Generar recetas", la cancelación y el pintado por stage.
- [ ] Encadenar `GenerationService` por `MainActivity` → `SazonApp` → `MainShell` → `GeneratorFlow`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: las suites nuevas en verde sin romper las 209
  existentes:
  - `GenerationServiceTest` (escenario 2, **verificación exigida**): con restricción `gluten`, la
    receta del falso "Bocadillo de tomate" (Pan libre) **no** está en `accepted` y sí en `discarded`
    con `HARD_RESTRICTION`; las seguras sí entran. Dieta `vegetarian` descarta "Pollo al ajillo"
    (`DIET_PATTERN`). Las restricciones del paso de ajustes (Perfil sin gluten + constraints con
    gluten) descartan el bocadillo. Un generador que lanza → `aiUnavailable` y `accepted` vacío, sin
    excepción; uno que suspende → la cancelación propaga `CancellationException`.
  - `GeneratorRequestFactoryTest`: libres y de Taxonomía conservan `name`/`ingredientId`; las
    restricciones de las constraints son las del paso de ajustes; notas en blanco → `null`. Caso
    negativo: cambiar las restricciones de los ajustes cambia el resultado (no pasa en vacío).
  - `GeneratorProgressMessagesTest`: la sublínea de tiempo usa `maxMinutes` de la franja elegida y la
    de alergias aparece solo con restricciones.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `GenerationService` y `GeneratorRequestFactory` sin imports Compose/Android;
  `MainNavigationState.kt`, `domain.generator/**`, `domain.validation/**`, la Taxonomía, `data/**` y el
  build intactos; `GenerationConstraints` sin cambios.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Al pulsar "Generar recetas" se muestra el estado de generación (titular, sublíneas y "Cancelar")
    y **"Cancelar" responde** y vuelve a los ajustes (**verificación exigida**).
  - Sin Cancelar, se listan **solo** recetas aceptadas; con un Perfil con gluten, el "Bocadillo de
    tomate" del falso **nunca** aparece (caso negativo visible).

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La orquestación y el mapeo (incluido el caso negativo obligatorio) se cubren en
JVM; el estado de carga/cancelación y el acabado visual son de la persona.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos ni necesita modos nuevos.

## Evidence To Capture

- Salida de `./gradlew test` con `GenerationServiceTest`, `GeneratorRequestFactoryTest` y
  `GeneratorProgressMessagesTest`, nº de tests y 0 fallos, y el total (esperado: 209 + los nuevos).
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `domain.generation` y `GeneratorRequestFactory` puros sin imports Compose/Android; sin
  cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `GenerationConstraints`,
  `domain.validation`, la Taxonomía ni `data/**`.
- Resultado manual en emulador del estado de carga y de "Cancelar", y de que una receta insegura del
  falso no aparece (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Validar con `validateRecipe`, no con `validateResponse`**: el puerto `RecipeGenerator` devuelve
  `List<Recipe>` ya decodificada; el parseo del envelope crudo se reserva al proveedor real. Mismo
  patrón que `SuggestionBatchService`.
- **Finding del fixture "Pollo al ajillo"**: el `FakeRecipeGenerator` (feature aceptada) declara
  `totalMinutes = 35` con pasos que suman 25 min, así que el validador lo descarta por `INCOHERENT` en
  **toda** generación (con dieta omnívora o vegetariana). No se "arregla" aquí: `fake-recipe-generator`
  está aceptada y este corte no la modifica. El implementer **no** debe asumir 4 recetas aceptadas; el
  caso negativo obligatorio se cubre con "Bocadillo de tomate". Si se quiere más variedad en la
  generación, va en una feature correctiva sobre el fixture.
- **Alergias por generación (unknown de `generator-settings-step`)**: se resuelve copiando el
  `PreferenceProfile` con `hardRestrictionIds = settings.hardRestrictionIds` en las
  `GenerationConstraints`. Así el validador y el prompt (proveedor real) ven la selección del paso de
  ajustes, **sin** modificar `GenerationConstraints`. Quitar una alergia del Perfil en el paso de
  ajustes la desactiva en esa generación (decisión de producto: el paso son las alergias **a evitar
  ahora**; los ajustes se precargan del Perfil).
- **Mapeo de ingredientes del usuario**: `quantity = 0.0`, `unit = null`, `isPantry = false` porque el
  input del usuario no lleva cantidad/unidad; las decide el modelo. El nombre y el `ingredientId`
  (Taxonomía o `null` libre) se conservan.
- **`PromptBuilder` no se invoca aquí**: el puerto no recibe prompt; lo compondrá la implementación
  real. `prompt-builder` es dependencia de orden del corte, no una llamada de este flujo.
- **Modo degradado con `aiUnavailable`**: cualquier `Exception` del generador (sin clave/red/error)
  se convierte en `aiUnavailable = true` con `accepted` vacío, como hace el Lote con el fallo del
  generador; la UI muestra el aviso degradado (el estado completo es de `results-screen`). Pregunta 1
  de `docs/risks-and-open-questions.md` (modelo de errores/reintentos) sigue abierta.
- **Preview mínimo, no la pantalla de Resultados**: `results-screen` es dueña del destino
  `PushedScreen.RESULTS`, las imágenes, el orden, el `matchScore` y los estados vacío/error. Aquí solo
  hay una lista de los aceptados que `results-screen` reemplazará.
- **Duración mínima visible** (`MIN_VISIBLE_MILLIS`, ~900 ms): el `FakeRecipeGenerator` es
  instantáneo; sin un mínimo, el estado de carga y "Cancelar" no serían observables (verificación
  exigida) y el proveedor real podría parpadear.
- **Cancelación limpia**: `GenerationService` re-lanza `CancellationException` (no la traga) y el
  huésped cancela la corrutina del `LaunchedEffect` poniendo el token a `null`.
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Microcopy exacto del estado de carga y del preview**: stitch fija el titular y el ejemplo
  "Ajustando a 30 minutos"; el resto (sublíneas, línea de estado vacío del preview) es provisional y
  queda sujeto a repaso visual (no cambia el contrato puro).
- **Duración mínima visible**: valor de UX (~900 ms) revisable; si molesta, se corrige con feature
  correctiva sin tocar el servicio.
- **Rotación/process death durante la generación o el preview**: el resultado vive en `remember` (no
  en `rememberSaveable`) porque `Recipe` no es serializable trivialmente; rotar vuelve a los ajustes.
  La pantalla de Resultados real (`results-screen`) definirá su propio estado.
- **Éxito con `accepted` vacío** (todas descartadas): se muestra la línea de estado vacío; el estado
  vacío "con causa" (contradicción con el Perfil) es de `results-screen`/`ai-search`.
- **Persistencia de las recetas generadas**: fuera de alcance; favoritos/caché son de sus features.

## Validator Checklist

- [ ] `GenerationService` orquesta generador → `RecipeValidator` y **una receta insegura no llega a
      `accepted`** (caso negativo obligatorio, escenario 2, verificación exigida).
- [ ] La dieta y las restricciones **del paso de ajustes** (Perfil copiado) se aplican; los
      descartes quedan registrados con su motivo.
- [ ] El estado de carga se muestra y **"Cancelar" responde** volviendo a los ajustes (escenario 5,
      verificación exigida); la cancelación propaga `CancellationException`.
- [ ] Un fallo del generador degrada a `aiUnavailable` sin lanzar (escenario 7).
- [ ] El preview lista **solo** `accepted` (escenario 8); no implementa la pantalla de Resultados real
      (sin imágenes, `matchScore`, orden ni estados de error completos).
- [ ] No se invoca `PromptBuilder`; no se usa `validateResponse`; `GenerationConstraints`,
      `domain.validation`, `domain.recipe`, `domain.prompt`, el Lote, la Taxonomía y `data/**` quedan
      intactos.
- [ ] La lógica pura (`GenerationService`, `GeneratorRequestFactory`, `GeneratorProgressMessages`)
      está cubierta en JVM con casos negativos que no pasan en vacío.
- [ ] Sin dependencias nuevas ni iconos; `app/build.gradle.kts` y `gradle/libs.versions.toml` sin
      cambios; sin hex sueltos.
- [ ] `init.sh` queda en verde y la evidencia está en `feature_list.json`/`PROGRESS.md`;
      `docs/technical-discovery.md` queda coherente.
