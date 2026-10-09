# Feature Implementation Spec: Generador: paso de ingredientes

## Source Feature

- `id`: `generator-ingredients-step`
- `area`: `ui`
- `depends_on`: `["home-offline-blocks"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Abrir el **Generador** sustituyendo su `SectionPlaceholder` por su **paso 1 real, "¿Qué tienes?"**:
el usuario añade ingredientes de la **Taxonomía** (buscador / nube de "Ingredientes sugeridos") o
**libres** (input de texto), los ve como **chips** con una "×" para quitarlos, ve un **contador**
("N ingredientes añadidos") que se actualiza al añadir y quitar, y **no puede continuar** ("Continuar"
deshabilitado) hasta que hay **al menos un ingrediente**. "Continuar" avanza al **paso 2** ("Ajusta tu
receta"), que en esta feature es un **marcador** a reemplazar por `generator-settings-step`.

La feature entrega la **lógica/estado puro** del paso (selección de ingredientes, deduplicación y
contador) testeable en JVM, la **capa Compose delgada** de la pantalla y del armazón mínimo del
Generador (título, "Atrás" y primario anclado), y **reutiliza** helpers ya aceptados
(`IngredientsFilter`, tokens del tema). El Generador se abre desde el FAB/héroe de la Home, que ya
empujan `PushedScreen.GENERATOR` (`home-offline-blocks`). Sin dependencias nuevas y sin red.

## Non-Goals

- **No** se implementa el **paso 2** (tiempo, tipo de comida, comensales, dificultad, alergias,
  notas): es `generator-settings-step`. Aquí solo hay su marcador y "Continuar" avanza a él.
- **No** se implementa la **generación** (llamar al `RecipeGenerator`, validar con `RecipeValidator`,
  estado de carga): es `generation-flow-with-validation`.
- **No** se calcula ni muestra `matchScore`, ni se muestran Resultados/Detalle (`results-screen`,
  `recipe-detail-screen`).
- **No** se cambia `ProfileRepository`/Room, `PreferenceProfile`, `GenerationConstraints`,
  `RecipeGenerator`, `PromptBuilder`, la Taxonomía, el catálogo semilla ni el build.
- **No** se añade `navigation-compose`, iconos (`material-icons-*`), imágenes ni DI: el paso vive
  dentro de `PushedScreen.GENERATOR` con la navegación mínima ya aceptada.
- **No** se persiste el borrador del generador en disco: vive en `rememberSaveable` dentro de la
  sesión (un asistente de dos pasos, no un dato del usuario).
- **No** se resuelve la conversión a `RecipeIngredient`/`PromptIngredient`: el estado guarda `name` +
  `ingredientId` opcional y su traducción es de `generation-flow-with-validation`.
- **No** se pre-cargan ingredientes del Perfil (p. ej. `preferredIngredientIds`) como sugerencias: la
  nube usa `taxonomy.ingredients`. Si la persona lo pide, será una feature correctiva.

## Job Story

Cuando abro el Generador desde la Home,
quiero añadir los ingredientes que tengo (los busque en la lista o los escriba libremente), verlos
como chips y saber cuántos llevo,
para continuar a los ajustes y pedir recetas a partir de lo que ya tengo en casa.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; no hay cuentas ni permisos. Añade/quita ingredientes
  y decide cuándo Continuar.
- **Desarrollador / agente**: implementa el paso y verifica la **lógica/estado en JVM** (selección,
  dedupe, contador, habilitación, navegación de paso); el acabado puramente visual (chips, densidad de
  la nube, claro/oscuro) lo revisa la persona tras el merge (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: añadir un ingrediente de la Taxonomía lo muestra como chip y actualiza el contador

Given el paso 1 con cero ingredientes,
When se pulsa un chip de "Ingredientes sugeridos" (p. ej. Tomate),
Then aparece un chip "Tomate", el contador dice "1 ingrediente añadido" y el chip sugerido queda
seleccionado; el botón "Continuar" pasa a habilitado.

### Scenario 2: añadir un ingrediente libre mediante el input

Given el paso 1 con la consulta libre en blanco,
When se escribe un texto (p. ej. "cebolla") y se pulsa "Añadir",
Then se añade un chip "cebolla" como ingrediente **libre** (`ingredientId == null`), el input se
**vacía** y el contador sube. Con el texto en blanco o solo espacios, "Añadir" no hace nada.

### Scenario 3: quitar un chip con la "×" baja el contador

Given el paso 1 con dos ingredientes añadidos,
When se pulsa la "×" de uno de los chips,
Then ese ingrediente desaparece, el contador baja a "1 ingrediente añadido" y el otro chip se
mantiene. Quitar todos deja el contador en "0 ingredientes añadidos" y "Continuar" deshabilitado.

### Scenario 4: no se puede continuar sin al menos un ingrediente (verificación exigida)

Given el paso 1,
When el contador está en cero,
Then el botón "Continuar" está **deshabilitado**; en cuanto hay un ingrediente queda **habilitado**, y
vuelve a deshabilitarse si se quitan todos.

### Scenario 5: deduplicación y alternancia

Given el paso 1,
When se pulsa dos veces el mismo chip de la Taxonomía,
Then el primer toque añade y el segundo quita (el estado no guarda duplicados por `ingredientId`); y
When se intenta añadir un libre cuyo nombre normalizado (sin distinguir mayúsculas ni acentos, sin
espacios sobrantes) ya existe entre los añadidos,
Then el estado **no** cambia (no se crean dos chips "Tomate"/"tomate").

### Scenario 6: contador con concordancia singular/plural

Given el número de ingredientes añadidos,
When el contador es 0, 1 o n,
Then las etiquetas puras son "0 ingredientes añadidos", "1 ingrediente añadido" y
"n ingredientes añadidos" respectivamente.

### Scenario 7: navegación del asistente

Given el Generador abierto en el paso 1,
When se pulsa "Atrás",
Then se cierra el Generador y se vuelve a la Home (`pop()`), y
When se pulsa "Continuar" (con ingredientes),
Then se avanza al **paso 2** (marcador) y "Atrás" desde el paso 2 vuelve al paso 1 **conservando** los
ingredientes añadidos.

### Scenario 8: el Generador se abre desde la Home y sigue siendo local

Given la Home con el FAB/héroe,
When se abren, la app muestra el **paso 1 real** del Generador (no el `SectionPlaceholder`), sin red,
y la Taxonomía de la nube y del buscador es exactamente `taxonomy.ingredients`.

## Repository Research

### Files Inspected

- `feature_list.json` — `generator-ingredients-step` (`depends_on: home-offline-blocks` `accepted`;
  verificación manual "Continuar deshabilitado con cero ingredientes y el contador se actualiza"; la
  nota "el paso 2 se implementa en `generator-settings-step`") y las features vecinas.
- `PROGRESS.md` — última aceptada `local-result-filters`; siguiente en lista esta feature; camino de
  verificación `init.sh`; no se automatiza UI de emulador.
- `AGENTS.md` — una feature por sesión, verificación de UI por lógica/estado en JVM, commit/push/PR
  fuera del planner.
- `CONTEXT.md` — "Ingrediente" (de la Taxonomía con `id`, o libre sin él), "Ingrediente de despensa"
  (no se declara), "Generador de recetas".
- `docs/design/stitch-prompt.md` — sección C (Generador) paso 1: título "¿Qué tienes?" + helper
  "Añade los ingredientes que tengas y nosotros hacemos el resto.", input píldora con "Añadir", chips
  con "×", contador "N ingredientes añadidos", nube colapsable "Ingredientes sugeridos" y primario
  "Continuar" **deshabilitado hasta que haya al menos uno**; sección 3 (páginas empujadas con botón
  atrás).
- `DESIGN.md` — tokens y reglas: una acción principal terracota, chips píldora, formas 16/24,
  espaciado 12/16/24, objetivos táctiles ≥48dp, cifras tabulares para lo cuantificable, sin rojo.
- `docs/domain-model.md` — `RecipeGenerator.generateFromIngredients(ingredients, constraints)`;
  `GenerationConstraints(profile, servings, mealTypeId, timeBucketId, difficultyId, notes)`;
  "Ingrediente" de Taxonomía o libre.
- `docs/domain/taxonomy.md` / `taxonomy.json` — `ingredients` (95, `label`, con texto libre) y la
  regla "la UI nunca diverge de la Taxonomía".
- `docs/specs/home-offline-blocks.md` — el FAB/héroe ya empujan `PushedScreen.GENERATOR` y el destino
  es `SectionPlaceholder("Generador", onBack = pop)`, "sustituible por su feature"; patrón de
  navegación pura + `Saver`.
- `docs/specs/onboarding-ingredients-safety-kitchen.md` — patrón de borrador en `rememberSaveable` +
  `Saver`, editores sin estado en `ui.preferences` e `IngredientsFilter.matching` (filtro puro por
  `label`, insensible a mayúsculas/acentos).
- Código: `ui/MainShell.kt`, `ui/SectionPlaceholder.kt`, `ui/navigation/MainNavigationState.kt`,
  `ui/home/{HomeScreen,RecipeCard}.kt`, `ui/onboarding/{OnboardingFlow,AvatarPreview}.kt`,
  `ui/preferences/{IngredientsEditor,IngredientsFilter,SoftPreferencesEditor}.kt`,
  `domain/taxonomy/Taxonomy.kt`, `domain/generator/{RecipeGenerator,GenerationConstraints}.kt`,
  `domain/recipe/Recipe.kt`, `ui/theme/{Color,Spacing,Shape}.kt`, `app/build.gradle.kts`.
- `docs/technical-discovery.md` — `MainShell` enruta `SectionPlaceholder` para Generador; se actualiza
  aquí.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: el estado del paso vive en Kotlin sin imports de
  Compose/Android y se testea en JVM; los `@Composable` solo pintan.
- **`rememberSaveable` + `Saver` en la capa Compose** (patrón `OnboardingPreferencesDraftSaver` /
  `MainNavigationStateSaver`), manteniendo la lógica libre de Compose.
- **Reutilización**: `IngredientsFilter.matching` para el buscador de la nube y los tokens del tema;
  `SectionPlaceholder` se sigue usando para Favoritos/Perfil.
- **Taxonomía como única fuente**: la nube y el buscador salen de `taxonomy.ingredients`; nunca se
  hardcodean labels.
- **Sin iconos ni imágenes**: los chips muestran el `label` y la "×" es texto; el botón atrás es
  "Atrás" (texto), coherente con `SectionPlaceholder`/`OnboardingScaffold`.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_` y casos negativos que fallarían
  si la lógica no hiciera nada.
- **Sin dependencias nuevas**; `init.sh` sin cambios (`./gradlew test` + `./gradlew lint`).

### Current Gaps

- `PushedScreen.GENERATOR` renderiza `SectionPlaceholder("Generador")`: no hay paso 1, ni estado de
  selección, ni armazón del asistente.
- No existe ningún modelo de ingredientes del generador ni forma de deduplicar/contar.
- `IngredientsFilter` existe pero solo se usa en el onboarding; la nube de sugeridos del generador no
  está.

## Technical Approach

Estrategia: **estado puro testeable en JVM + capa Compose delgada**, sin dependencias nuevas.

### 1. Lógica pura — `com.sazon.app.ui.generator`

- `GeneratorIngredient(name: String, ingredientId: String? = null)`: `ingredientId` apunta a la
  Taxonomía; `null` es ingrediente libre (solo `name`). Es la costura hacia `PromptIngredient`/
  `RecipeIngredient` que consumirá `generation-flow-with-validation`.
- `GeneratorIngredientsState(ingredients: List<GeneratorIngredient> = emptyList(), query: String = "")`:
  - `count`, `isEmpty`, `canContinue = ingredients.isNotEmpty()` y `countLabel`
    ("0 ingredientes añadidos" / "1 ingrediente añadido" / "N ingredientes añadidos").
  - `typeQuery(text)` para el input libre.
  - `toggleTaxonomy(ingredient: Ingredient)`: si ya está (por `ingredientId`) la quita; si no, la
    añade con `label` + `id`.
  - `addFree()`: recorta `query`; si queda vacía **no** añade; si su nombre normalizado coincide con el
    de un ingrediente ya añadido (libre o de Taxonomía) **no** añade; en otro caso añade un libre y
    **vacía `query`**. Normalización: `trim` + minúsculas + sin acentos (`java.text.Normalizer`).
  - `remove(ingredient)`: quita el chip concreto.
  - Operaciones inmutables (`copy`); sin ids fuera de la Taxonomía validados aquí (el editor solo
    ofrece ids válidos).
- `GeneratorStep` (enum puro): `INGREDIENTS("¿Qué tienes?", "Continuar")` y
  `SETTINGS("Ajusta tu receta", "Generar recetas")`, con `isLast`, `next()` y `previous()` (sin salirse
  de rango). Materializa el asistente de dos pasos; `generator-settings-step` rellenará `SETTINGS`.

### 2. Capa Compose — `com.sazon.app.ui.generator`

- `GeneratorFlow(taxonomy, onClose)`: armazón del asistente dentro del destino empujado.
  - Estado con `rememberSaveable`: `step` (mutable) y `ingredients` con
    `GeneratorIngredientsStateSaver` (lista `[query, count, name, id]…`) en la capa Compose.
  - `Scaffold` con barra superior (botón "Atrás" y el título del paso) y barra inferior anclada con el
    primario (`step.primaryLabel`), **deshabilitado** en `INGREDIENTS` con `!ingredients.canContinue`.
  - "Atrás" = `step.previous() ?: onClose()`; el primario = `step.next()` (en `SETTINGS`, sin efecto:
    la generación es de `generation-flow-with-validation`).
  - `INGREDIENTS` → `GeneratorIngredientsScreen`; `SETTINGS` → marcador con una línea
    ("El paso de ajustes llega en 'generator-settings-step'.").
- `GeneratorIngredientsScreen(state, taxonomy, onQueryChange, onAddFree, onToggleTaxonomy, onRemove)`:
  - Helper "Añade los ingredientes que tengas y nosotros hacemos el resto.".
  - Input píldora (`OutlinedTextField`) con botón "Añadir" en el `trailingIcon` (deshabilitado con
    texto en blanco).
  - Chips de los añadidos (`FlowRow`) con "×" que llama a `onRemove`; el contador `state.countLabel`
    con cifras tabulares.
  - Nube **colapsable** "Ingredientes sugeridos" (estado local `rememberSaveable`) con los
    `taxonomy.ingredients` filtrados por `IngredientsFilter.matching(..., state.query)`; un chip
    seleccionado si su `id` está entre los añadidos; al pulsar, `onToggleTaxonomy`. Sin iconos (solo
    `label`).
- Integración: en `MainShell`, el `PushedScreen.GENERATOR` deja de pintar `SectionPlaceholder` y monta
  `GeneratorFlow(taxonomy = taxonomy, onClose = { navigation = navigation.pop() })`.

### 3. Sin cambios de build

No se tocan `app/build.gradle.kts` ni `gradle/libs.versions.toml`. `SectionPlaceholder` sigue
existiendo para Favoritos/Perfil.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/generator/GeneratorIngredient.kt` — modelo puro del ingrediente
  añadido (Taxonomía o libre).
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorIngredientsState.kt` — estado puro (añadir,
  alternar, quitar, dedupe, contador, `canContinue`).
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorStep.kt` — pasos puros del asistente.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — armazón del asistente + `Saver`.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorIngredientsScreen.kt` — paso 1 (input, chips,
  contador y nube de sugeridos).
- `app/src/test/java/com/sazon/app/ui/generator/GeneratorIngredientsStateTest.kt` — escenarios 1–6.
- `app/src/test/java/com/sazon/app/ui/generator/GeneratorStepTest.kt` — escenario 7 (navegación pura).

**Modificar**

- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — `PushedScreen.GENERATOR` monta `GeneratorFlow`
  en lugar del `SectionPlaceholder`.
- `docs/technical-discovery.md` — registrar `ui.generator` (estado puro + armazón + paso 1) y que
  `MainShell` sustituye el placeholder del Generador.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `ProfileDatabase`,
`data.profile/**`, `domain.profile/**`, `domain.suggestions/**`, `data.suggestions/**`,
`domain.generator/**`, `domain.prompt/**`, `domain.recipe/**`, la Taxonomía, `seed-recipes.json` ni las
features de onboarding/Home.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` (sección C paso 1 y sección 3).
- Screens or states affected: Generador paso 1 (input + "Añadir", chips con "×", contador, nube
  colapsable de sugeridos), paso 2 como marcador, barra superior con "Atrás" y primario anclado
  "Continuar" (deshabilitado/habilitado).
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonPillShape` (chips/input),
  `SazonShapes`, `MaterialTheme.colorScheme.primary` (acción principal), `tertiary` **solo** si se
  necesitara un aviso (no hay alérgenos en este paso). Cifras tabulares en el contador.
- Reglas visuales: una única acción principal terracota por pantalla; objetivos táctiles ≥48dp;
  tipografía dinámica (scroll, nunca recortar).
- **Sin** material-icons ni imágenes: "×" y "Atrás" son texto; los chips muestran su `label` (mismo
  criterio documentado en `AvatarPreview`/onboarding).
- New design artifact required: **no**; el acabado visual (densidad de la nube, tratamiento de chips) lo
  revisa la persona tras el merge; un defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar `ui.generator` (estado puro, armazón y paso 1) y
  anotar que `MainShell` sustituye el `SectionPlaceholder` del Generador.
- `docs/domain-model.md`: **not needed** — ya describe el Generador, los ingredientes de Taxonomía o
  libres y `GenerationConstraints`; esta feature no cambia el modelo.
- `DESIGN.md` y `docs/design/concepts/`: **not needed** — siguen siendo la fuente/dirección; no cambian.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear la lógica pura (`GeneratorIngredient`, `GeneratorIngredientsState`, `GeneratorStep`).
2. Escribir los tests JVM (`GeneratorIngredientsStateTest`, `GeneratorStepTest`) con casos negativos.
3. Crear `GeneratorIngredientsScreen` (input + chips + contador + nube de sugeridos) sobre los tokens.
4. Crear `GeneratorFlow` (Scaffold, título, "Atrás", primario anclado, `Saver`) con el marcador del
   paso 2.
5. Integrar en `MainShell` (`PushedScreen.GENERATOR` → `GeneratorFlow`) y ejecutar `init.sh` en verde.
6. Actualizar `docs/technical-discovery.md`; el implementer registra evidencia y estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [x] Crear `GeneratorIngredient` (Taxonomía si `ingredientId != null`, libre si `null`).
- [x] Crear `GeneratorIngredientsState` con `typeQuery`, `toggleTaxonomy`, `addFree`, `remove`,
      `count`, `canContinue` y `countLabel`.
- [x] Crear `GeneratorStep` (`INGREDIENTS`/`SETTINGS`) con `next`/`previous`/`isLast`.
- [x] Escribir `GeneratorIngredientsStateTest` y `GeneratorStepTest` con casos negativos (duplicado,
      blanco, quitar hasta cero, rangos de paso).
- [x] Crear `GeneratorIngredientsScreen` (input con "Añadir", chips con "×", contador, nube de
      sugeridos filtrada con `IngredientsFilter`).
- [x] Crear `GeneratorFlow` con el `Saver`, la barra superior ("Atrás") y el primario anclado.
- [x] Integrar `GeneratorFlow` en `MainShell` en el destino `GENERATOR`.
- [x] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: las suites nuevas en verde sin romper las 172
  existentes:
  - `GeneratorIngredientsStateTest`: arranca en 0 con `canContinue == false`; `toggleTaxonomy(tomate)`
    añade (1) y vuelve a quitarlo; `addFree()` con "cebolla" añade un libre con `ingredientId == null`
    y vacía `query`; `addFree()` con blanco/"  " no cambia; un libre duplicado por nombre normalizado
    (" cebolla ", "CEBOLLA") no crea chip; `remove` baja el contador; quitar el último deja
    `canContinue == false`; `countLabel` singular/plural. Caso negativo: el estado no contiene dos
    ingredientes con la misma identidad normalizada.
  - `GeneratorStepTest`: `INGREDIENTS.next() == SETTINGS`; `SETTINGS.isLast` y `next() == null`;
    `INGREDIENTS.previous() == null`; `SETTINGS.previous() == INGREDIENTS`; etiquetas del microcopy.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `ui.generator` puro (modelo/estado/`GeneratorStep`) sin imports de Compose/Android;
  `ui.preferences`/`IngredientsFilter` reutilizados sin modificarlos; sin cambios en
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - "Continuar" está **deshabilitado** con cero ingredientes y **se actualiza el contador** al añadir y
    quitar (verificación exigida).
  - Se añade un ingrediente de la nube y uno libre por el input; la "×" quita el chip.
  - "Atrás" cierra el Generador y vuelve a la Home; desde el paso 2 vuelve al paso 1 conservando los
    ingredientes.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas de
UI de emulador. La lógica/estado (añadir/quitar, dedupe, contador, habilitación y navegación de paso) se
cubre en JVM, que es lo que AGENTS.md exige "cuando sea posible"; el acabado visual es de la persona.

## Evidence To Capture

- Salida de `./gradlew test` con `GeneratorIngredientsStateTest` y `GeneratorStepTest`, nº de tests y 0
  fallos, y el total (esperado: 172 + los nuevos).
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `ui.generator` puro sin imports Compose/Android; `IngredientsFilter` reutilizado sin
  cambios; sin dependencias nuevas.
- Resultado manual en emulador del "Continuar" deshabilitado con cero ingredientes, del contador al
  añadir/quitar y de la "×" (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Paso 1 real + paso 2 marcador**: `home-offline-blocks` dejó el Generador como
  `SectionPlaceholder` "sustituible por su feature"; esta feature sustituye el destino por el paso 1 y
  avanza a un marcador del paso 2, igual que hizo `onboarding-shell-and-identity` con sus pasos
  posteriores. Así `generator-settings-step` rellena solo su contenido.
- **Estado del paso en `GeneratorFlow`**: el borrador de ingredientes vive en el huésped (no en la
  pantalla) para que el paso 2 lo conserve; `rememberSaveable` + `Saver` (patrón del onboarding).
- **Contador puro**: `countLabel` es lógica pura y testeable, con singular/plural; las cifras se pintan
  tabulares (`DESIGN.md`).
- **Dedupe**: por `ingredientId` para la Taxonomía y por nombre normalizado (minúsculas, sin acentos,
  `trim`) para los libres, comprobando también contra los ya añadidos; un texto en blanco es un no-op.
  `IngredientsFilter` (puro) se reutiliza para el buscador de la nube.
- **Ingrediente libre con `name` + `ingredientId = null`**: replica el modelo de `PromptIngredient`/
  `RecipeIngredient` y difiere la conversión a `generation-flow-with-validation`; el estado no inventa
  ids.
- **Nube con `taxonomy.ingredients`**: la UI no diverge de la Taxonomía; el buscador filtra por `label`.
  No se pre-cargan preferencias del Perfil (feature correctiva si se pide).
- **Sin iconos**: el repositorio no tiene assets ni dependencia de iconos; "×" y "Atrás" son texto.
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Microcopy del input libre**: `stitch-prompt` solo fija el botón "Añadir"; el placeholder es
  provisional y queda sujeto a repaso visual (no cambia el contrato puro).
- **Densidad de la nube**: mostrar los 95 ingredientes (colapsada) es denso; el buscador y el
  plegado lo mitigan. Si el repaso visual no convence, se corrige con feature correctiva.
- **Continuar en el paso 2**: el primario "Generar recetas" no tiene efecto hasta
  `generation-flow-with-validation`; es un marcador temporal.
- **Persistencia del borrador**: no sobrevive al cierre de la app (solo a cambios de
  configuración/proceso de la sesión); es un asistente efímero, no un dato del Perfil.
- **Ingredientes libres en el prompt**: la traducción a `RecipeIngredient` (cantidad/unidad) y el paso
  por el Validador (keywords de restricciones) son de features posteriores; aquí solo se capturan.

## Validator Checklist

- [ ] El Generador renderiza el **paso 1 real** (no el `SectionPlaceholder`) y el paso 2 es solo un
      marcador; no se implementa generación, resultados, detalle ni el paso de ajustes.
- [ ] "Continuar" está deshabilitado con cero ingredientes y el contador se actualiza al añadir y
      quitar (escenarios 3 y 4, verificación exigida).
- [ ] Se añaden ingredientes de la Taxonomía y libres; se ven como chips con "×" y se pueden quitar; no
      hay duplicados (por `ingredientId` y por nombre normalizado) y el texto en blanco es un no-op.
- [ ] "Atrás" cierra el Generador desde el paso 1 y vuelve al paso 1 desde el 2 conservando los
      ingredientes; el estado se guarda con `rememberSaveable` + `Saver`.
- [ ] La nube de sugeridos y el buscador salen de `taxonomy.ingredients` (con `IngredientsFilter`) y no
      hay labels hardcodeados ni listas duplicadas.
- [ ] La lógica pura (`GeneratorIngredient`, `GeneratorIngredientsState`, `GeneratorStep`) está cubierta
      en JVM con casos negativos que no pasan en vacío.
- [ ] Se mantiene en `ui.generator`/`ui`; no toca dominio, persistencia ni el build
      (`app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios).
- [ ] Sin hex sueltos ni iconos: los colores/formas salen del tema y la "×"/"Atrás" son texto.
- [ ] `init.sh` queda en verde y la evidencia está en `feature_list.json`/`PROGRESS.md`;
      `docs/technical-discovery.md` queda coherente.
