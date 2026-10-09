# Feature Implementation Spec: Generador: paso de ajustes

## Source Feature

- `id`: `generator-settings-step`
- `area`: `ui`
- `depends_on`: `["generator-ingredients-step"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Rellenar el **paso 2 real del Generador, "Ajusta tu receta"** (hoy un marcador dentro de
`GeneratorFlow`), donde el usuario ajusta, **agrupados en tarjetas**: el **tiempo disponible**
(franja), el **tipo de comida** (cinco chips, incluido **Postre**), los **comensales** (stepper
**precargado con los comensales habituales del perfil**), la **dificultad**, las **alergias a evitar**
(chips **en ámbar** precargados del perfil, con un enlace "Editar en mi perfil" a la pestaña Perfil) y
**notas libres**. El primario anclado del paso sigue siendo "Generar recetas"; en esta feature **no
genera** (la generación es de `generation-flow-with-validation`).

La feature entrega el **estado puro del paso** (precarga desde el `PreferenceProfile` y operaciones de
edición) testeable en JVM, la **capa Compose delgada** de la pantalla, y la **integración** en
`GeneratorFlow` (que pasa a recibir el perfil y el callback de Perfil) y en `MainShell`. Reutiliza
editores ya aceptados (`TimeBucketEditor`, `DifficultyEditor`, `ServingsStepper`,
`HardRestrictionsEditor`) y los tokens del tema. Sin dependencias nuevas y sin red.

## Non-Goals

- **No** se implementa la **generación**: no se llama al `RecipeGenerator`, ni al `RecipeValidator`,
  ni al `PromptBuilder`; no hay estado de carga ni "Cancelar" (es `generation-flow-with-validation`).
  El primario "Generar recetas" queda **sin efecto** en esta feature.
- **No** se implementan Resultados ni Detalle (`results-screen`, `recipe-detail-screen`).
- **No** se construye la **pantalla Perfil** real: el enlace "Editar en mi perfil" solo **navega** a la
  pestaña Perfil (hoy `SectionPlaceholder`; la construye `profile-screen`).
- **No** se persiste el borrador de ajustes en disco: `rememberSaveable` + `Saver` dentro de la
  sesión, como el borrador de ingredientes. Es un asistente efímero, no un dato del Perfil.
- **No** se escribe de vuelta en el `PreferenceProfile` ni en `ProfileRepository`: los ajustes (y las
  alergias) son **por generación**, no editan el perfil.
- **No** se toca `GenerationConstraints`, `RecipeGenerator`, `PromptBuilder`, `RecipeValidator`, la
  Taxonomía, `domain.recipe`, `domain.profile` ni el build. En particular, **no** se convierte el
  estado a `GenerationConstraints` (ver "Unknowns": la alergia por generación necesita una decisión de
  `generation-flow-with-validation`).
- **No** se añade `navigation-compose`, iconos (`material-icons-*`), imágenes ni DI. "Editar en mi
  perfil" es un `TextButton` de texto (sin iconos) y la navegación usa `MainNavigationState` ya
  aceptado.
- **No** se calcula `matchScore` ni se filtran/ordenan recetas aquí.

## Job Story

Cuando he elegido mis ingredientes en el Generador,
quiero ajustar el tiempo, el tipo de comida, los comensales, la dificultad y las alergias (todo
precargado con mi perfil para no repetirlo) y añadir una nota,
para pedir recetas que encajen con lo que quiero cocinar ahora sin rehacer mi perfil.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; no hay cuentas ni permisos. Ajusta los valores y
  decide cuándo "Generar recetas".
- **Desarrollador / agente**: implementa el estado y la pantalla y verifica la **lógica/estado en
  JVM** (precarga, single-select, multi-select, recorte del stepper, notas); el acabado puramente
  visual (tarjetas, ámbar de alergias, densidad) lo revisa la persona tras el merge (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: precarga desde el perfil (verificación exigida: comensales)

Given un `PreferenceProfile` con `defaultServings = 4`, `hardRestrictionIds = ["gluten"]`,
`difficultyId = "medium"` y `timeBucketId = "lte_30"`,
When se abre el paso de ajustes,
Then el estado arranca con `servings == 4`, la alergia `gluten` marcada, `medium` y `lte_30`
seleccionados, `mealTypeId == null` y `notes == ""`; el stepper muestra **4**.

### Scenario 2: el tipo de comida son los cinco de la Taxonomía, incluido Postre

Given la Taxonomía (`taxonomy.mealTypes`, 5 opciones),
When se pinta el grupo "Tipo de comida",
Then hay un chip por `mealType` (Desayuno, Comida, Cena, Snack y **Postre**), y al pulsar Postre el
estado queda en `mealTypeId == "dessert"`; volver a pulsarlo lo deja en `null` (sin preferencia).

### Scenario 3: tiempo y dificultad son single-select que reemplazan

Given el paso con `timeBucketId = "lte_30"` y `difficultyId = "easy"`,
When se pulsa "Hasta 45 min" y luego "Elaborada",
Then el estado queda en `lte_45` y `hard` (reemplazan, no acumulan); no hay dos opciones marcadas.

### Scenario 4: comensales con recorte al rango de la Taxonomía

Given `taxonomy.servings` con `min = 1`, `max = 12` y un perfil con `defaultServings = 2`,
When se pulsa "+" hasta 12 y una vez más, y "−" hasta 1 y una vez más,
Then el valor nunca sale de `1..12` (`setServings` recorta con `coerceIn`).

### Scenario 5: alergias a evitar (multi-select en ámbar, prefijadas)

Given el paso con `hardRestrictionIds = ["gluten"]`,
When se pulsa el chip "Lactosa" y luego se pulsa "Ninguna",
Then la primera pulsación añade `lactose` sin duplicar y la segunda deja la lista vacía
(`hasNoRestrictions == true`); los chips seleccionados se pintan con el rol **ámbar**
(`tertiary`/`tertiaryContainer`), nunca con el rol de error.

### Scenario 6: enlace "Editar en mi perfil"

Given el paso de ajustes abierto con alguna alergia o ninguna,
When se pulsa "Editar en mi perfil",
Then se invoca `onEditProfile`: `MainShell` cierra el Generador (`pop()`) y selecciona la pestaña
Perfil (`selectTab(MainTab.PROFILE)`); no se abre ninguna pantalla nueva ni se sale de la app.

### Scenario 7: notas libres

Given el grupo "¿Algo más que deba saber?" con el textarea vacío,
When se escribe un texto multilínea ("Ej.: sin horno, aprovechar las sobras…"),
Then `notes` refleja el texto tal cual (se conservan saltos de línea); si queda en blanco, el estado
guarda cadena vacía.

### Scenario 8: navegación y conservación del borrador

Given el Generador abierto en el paso 1 con ingredientes,
When se pulsa "Continuar",
Then se ve el **paso 2 real** (no el marcador) y, al pulsar "Atrás", se vuelve al paso 1
**conservando** los ingredientes; al volver a "Continuar", los **ajustes** también se conservan
(estado en `rememberSaveable` del huésped). "Generar recetas" existe pero **no tiene efecto** (la
generación es de `generation-flow-with-validation`).

## Repository Research

### Files Inspected

- `feature_list.json` — `generator-settings-step` (nota: "los ajustes van agrupados en tarjetas; las
  alergias se muestran en ámbar con enlace a Perfil"; verificación manual "el stepper aparece con los
  comensales habituales del perfil" y "los cinco tipos de comida incluyen Postre"; dependencia
  `generator-ingredients-step` `accepted`).
- `PROGRESS.md` — `generator-ingredients-step` aceptado (189 tests, 13 warnings);
  `generation-flow-with-validation` es la siguiente tras esta.
- `AGENTS.md` — verificación de UI por lógica/estado en JVM; commit/push/PR fuera del planner.
- `docs/design/stitch-prompt.md` — sección C paso 2: "Ajusta tu receta" con grupos en tarjeta
  (Tiempo disponible, Tipo de comida, Comensales precargados, Dificultad, Alergias a evitar
  precargadas en ámbar + "Editar en mi perfil"/"Ninguna declarada", notas) y primario "Generar
  recetas".
- `DESIGN.md` — tarjetas 24px, interior 12px, padding lateral 16px, ritmo 24px; una acción principal
  terracota; aviso de alérgenos en ámbar, **nunca rojo**; cifras tabulares; editores píldora.
- `docs/domain-model.md` — `GenerationConstraints(profile, servings, mealTypeId, timeBucketId,
  difficultyId, notes)`; las restricciones duras viven en `PreferenceProfile`.
- `docs/domain/taxonomy.md` / `taxonomy.json` — `mealTypes` (5, incluye `dessert` "Postre"),
  `timeBuckets` (5), `difficulties` (3, `level`), `servings` (min 1, max 12, default 2),
  `hardRestrictions` (11); "la UI nunca diverge de la Taxonomía".
- `docs/specs/generator-ingredients-step.md` — patrón de la feature anterior: estado puro en
  `ui.generator`, `GeneratorFlow` con `rememberSaveable` + `Saver`, placeholder del paso 2,
  `MainShell` montando `GeneratorFlow`, reutilización de editores.
- `docs/specs/onboarding-ingredients-safety-kitchen.md` — patrón del borrador con `Saver` y de los
  editores sin estado de `ui.preferences`.
- Código: `ui/generator/{GeneratorFlow,GeneratorStep,GeneratorIngredientsScreen}.kt`,
  `ui/MainShell.kt`, `ui/navigation/MainNavigationState.kt`,
  `ui/preferences/{TimeBucketEditor,DifficultyEditor,ServingsStepper,HardRestrictionsEditor}.kt`,
  `domain/generator/GenerationConstraints.kt`, `domain/profile/UserProfile.kt`,
  `domain/taxonomy/Taxonomy.kt`, `ui/theme/{Shape,Spacing}.kt`.
- `docs/technical-discovery.md` — párrafo del paso de ingredientes (`ui.generator`) que cita el paso 2
  como marcador; se actualiza aquí.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: el estado vive en Kotlin sin imports de Compose/Android y se
  testea en JVM; los `@Composable` solo pintan y delegan en callbacks.
- **`rememberSaveable` + `Saver`** en `GeneratorFlow` (patrón `GeneratorIngredientsStateSaver` /
  `OnboardingPreferencesDraftSaver`), manteniendo la lógica libre de Compose.
- **Reutilización de editores** de `ui.preferences` sin modificarlos: `TimeBucketEditor` (single),
  `DifficultyEditor` (single, ordena por `level`), `ServingsStepper` (cifras tabulares; deshabilita en
  los extremos) y `HardRestrictionsEditor` (chips ámbar + "Ninguna", banner de seguridad).
- **Taxonomía como única fuente**: los grupos salen de `taxonomy.timeBuckets`/`mealTypes`/
  `difficulties`/`hardRestrictions`; nunca se hardcodean labels ni listas.
- **Sin iconos ni imágenes**: "Editar en mi perfil" y "Atrás" son texto; coherente con el paso 1.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_` y casos negativos que fallarían
  si la lógica no hiciera nada.
- **Sin dependencias nuevas**; `init.sh` sin cambios (`./gradlew test` + `./gradlew lint`).

### Current Gaps

- `GeneratorFlow` pinta el paso 2 como `GeneratorSettingsPlaceholder`; no hay estado de ajustes ni
  pantalla.
- `GeneratorFlow` no recibe el perfil: no puede precargar comensales, alergias, dificultad ni franja.
- No existe callback hacia Perfil desde el Generador, ni navegación a la pestaña Perfil.
- No existe ningún editor de "tipo de comida" (no hay `MealTypeEditor`).

## Technical Approach

Estrategia: **estado puro testeable en JVM + pantalla Compose delgada + integración en el huésped**,
sin dependencias nuevas ni cambios de dominio.

### 1. Lógica pura — `com.sazon.app.ui.generator`

- `GeneratorSettingsState`:
  - `servings: Int`, `mealTypeId: String? = null`, `timeBucketId: String`, `difficultyId: String`,
    `hardRestrictionIds: List<String> = emptyList()`, `notes: String = ""`.
  - `companion object { fun initial(preferences: PreferenceProfile, limits: Servings):
    GeneratorSettingsState }`: copia `defaultServings` (recortado con `coerceIn(limits.min,
    limits.max)`), `timeBucketId`, `difficultyId` y `hardRestrictionIds` del perfil; `mealTypeId =
    null` (el perfil no tiene tipo de comida) y `notes = ""`.
  - `setServings(value: Int, limits: Servings)`: recorta con `coerceIn`.
  - `selectMealType(id: String)`: single-select **opcional** (si ya está seleccionado lo deja en
    `null`, si no lo reemplaza).
  - `selectTimeBucket(id: String)` y `selectDifficulty(id: String)`: reemplazan (single-select
    obligatorio).
  - `toggleHardRestriction(id: String)`: semántica de conjunto (sin duplicados, alterna).
  - `clearHardRestrictions()` y `hasNoRestrictions: Boolean`.
  - `typeNotes(text: String)`.
  - Todas las operaciones inmutables (`copy`); no conoce Compose ni Android (solo
    `domain.profile.PreferenceProfile` y `domain.taxonomy.Servings`).

### 2. Capa Compose — `com.sazon.app.ui.generator`

- `GeneratorSettingsScreen(state, taxonomy, onServingsChange, onMealTypeSelect, onTimeBucketSelect,
  onDifficultySelect, onToggleRestriction, onClearRestrictions, onNotesChange, onEditProfile)`:
  seis tarjetas blancas (`Surface` con `colorScheme.surface` y `SazonShapes.large`, sin bordes duros;
  padding interior `SazonSpacing.sm`) con título `titleMedium`:
  1. "Tiempo disponible" → `TimeBucketEditor(taxonomy.timeBuckets, state.timeBucketId, …)`.
  2. "Tipo de comida" → chip **single-select** por `taxonomy.mealTypes` (`FilterChip` con
     `SazonPillShape`, seleccionado `primary`/`onPrimary`). Sin editor existente: fila local
     reutilizando el mismo tratamiento visual; el toque sobre el seleccionado lo deselecciona.
  3. "Comensales" → `ServingsStepper(taxonomy.servings, state.servings, onServingsChange)`.
  4. "Dificultad" → `DifficultyEditor(taxonomy.difficulties, state.difficultyId, …)`.
  5. "Alergias a evitar" → `HardRestrictionsEditor(taxonomy.hardRestrictions,
     state.hardRestrictionIds, …)` (banner y chips **ámbar**; "Ninguna"), seguido de la caption
     "Ninguna declarada" cuando `hasNoRestrictions` y de un `TextButton("Editar en mi perfil",
     onClick = onEditProfile)`. **Nunca** el rol de error/rojo.
  6. "¿Algo más que deba saber?" → `OutlinedTextField` multilínea (`minLines = 3`, `singleLine =
     false`) con placeholder "Ej.: sin horno, aprovechar las sobras, en 10 minutos…".
- `GeneratorFlow(taxonomy, preferences, onClose, onEditProfile)`:
  - Añade `preferences: PreferenceProfile` y `onEditProfile: () -> Unit`.
  - Guarda `settings` con `rememberSaveable(stateSaver = GeneratorSettingsStateSaver)` inicializado
    con `GeneratorSettingsState.initial(preferences, taxonomy.servings)`.
  - `GeneratorStep.SETTINGS` monta `GeneratorSettingsScreen` en lugar de
    `GeneratorSettingsPlaceholder` (que se elimina); el resto (barra superior, "Atrás", primario
    anclado, `Saver` de ingredientes) no cambia. "Generar recetas" sigue `enabled` y sin efecto
    (`step.next() == null`).
  - `GeneratorSettingsStateSaver`: `listSaver` que serializa `[servings,
    mealTypeId.orEmpty(), timeBucketId, difficultyId, count, …restrictionIds, notes]`.

### 3. Integración en `MainShell`

- `PushedScreen.GENERATOR` pasa `preferences = profile.preferences` y
  `onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) }` a `GeneratorFlow`.
  (El Perfil real es de `profile-screen`; el enlace aterriza hoy en el `SectionPlaceholder`.)

### 4. Sin cambios de build ni de dominio

No se tocan `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`, `data/**`, la Taxonomía
ni los editores de `ui.preferences`.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/generator/GeneratorSettingsState.kt` — estado puro del paso 2
  (precarga del perfil + operaciones).
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorSettingsScreen.kt` — pantalla del paso 2
  (tarjetas, chips, stepper, notas y enlace a Perfil).
- `app/src/test/java/com/sazon/app/ui/generator/GeneratorSettingsStateTest.kt` — escenarios 1–7.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — parámetros `preferences` y
  `onEditProfile`, estado de ajustes con `Saver`, montar `GeneratorSettingsScreen` y borrar el
  placeholder.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — pasar el perfil y el callback de Perfil.
- `docs/technical-discovery.md` — registrar el paso de ajustes en `ui.generator`.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `GeneratorStep.kt`,
`GeneratorIngredient.kt`, `GeneratorIngredientsState.kt`, `GeneratorIngredientsScreen.kt`,
`ui/preferences/**`, `domain/**`, `data/**`, la Taxonomía ni `seed-recipes.json`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` (sección C paso 2 y sección 3).
- Screens or states affected: Generador paso 2 ("Ajusta tu receta") con seis grupos en tarjeta; enlace
  "Editar en mi perfil".
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonShapes.large` (tarjetas),
  `SazonPillShape` (chips), `MaterialTheme.colorScheme.primary` (chips seleccionados y única acción
  principal), `tertiary`/`tertiaryContainer` **solo** para las alergias (ámbar, nunca rojo). Cifras
  tabulares en el stepper.
- Reglas visuales: una única acción principal terracota (el primario anclado "Generar recetas");
  objetivos táctiles ≥48dp; tipografía dinámica (scroll, nunca recortar); el texto largo de las notas
  hace scroll.
- **Sin** material-icons ni imágenes: "Editar en mi perfil" y "Atrás" son texto.
- Capturas de referencia (backfill PR #37): `docs/design/concepts/generator-settings-step/` — pantalla
  `generador-ajustes` (paso 2). Pendiente de exportar desde Stitch (petición en el `README.md` de la
  carpeta); el acabado visual (tarjetas, densidad, ámbar) lo revisa la persona tras el merge y un
  defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar el paso de ajustes (`GeneratorSettingsState` +
  `GeneratorSettingsScreen`) en `ui.generator` y anotar que `GeneratorFlow` recibe el perfil y el
  callback de Perfil.
- `docs/domain-model.md`: **not needed** — `GenerationConstraints` ya describe los ajustes; esta
  feature no cambia el modelo (solo captura los valores).
- `DESIGN.md` y `docs/design/concepts/`: **update** — `DESIGN.md` sigue siendo la fuente; se añade el
  backfill de captura `generador-ajustes` en `docs/design/concepts/generator-settings-step/`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión.

## Implementation Plan

1. Crear `GeneratorSettingsState` (`initial` con precarga y las operaciones).
2. Escribir `GeneratorSettingsStateTest` (escenarios 1–7) con casos negativos.
3. Crear `GeneratorSettingsScreen` (tarjetas + editores reutilizados + chips de tipo + notas +
   enlace).
4. Ampliar `GeneratorFlow` (parámetros, estado con `Saver`, montaje real del paso 2) y borrar el
   placeholder.
5. Integrar en `MainShell` (perfil + `onEditProfile`) y ejecutar `init.sh` en verde.
6. Actualizar `docs/technical-discovery.md`; el implementer registra evidencia y estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `GeneratorSettingsState` con `initial(preferences, limits)`, `setServings`,
      `selectMealType`, `selectTimeBucket`, `selectDifficulty`, `toggleHardRestriction`,
      `clearHardRestrictions`, `hasNoRestrictions` y `typeNotes`.
- [ ] Escribir `GeneratorSettingsStateTest` con casos negativos (single-select que reemplaza, recorte
      del stepper, `clear` sin residuo, independencia entre campos e inmutabilidad).
- [ ] Añadir un test que recorra `taxonomy.mealTypes` y compruebe las cinco opciones, incluida
      "Postre".
- [ ] Crear `GeneratorSettingsScreen` con las seis tarjetas, reutilizando `TimeBucketEditor`,
      `DifficultyEditor`, `ServingsStepper` y `HardRestrictionsEditor`; añadir la caption "Ninguna
      declarada" y el `TextButton` "Editar en mi perfil".
- [ ] Ampliar `GeneratorFlow` (`preferences`, `onEditProfile`, `GeneratorSettingsStateSaver`) y montar
      la pantalla donde estaba `GeneratorSettingsPlaceholder`; eliminar el placeholder.
- [ ] Integrar en `MainShell` (`preferences = profile.preferences`,
      `onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) }`).
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: la suite nueva en verde sin romper las 189
  existentes:
  - `GeneratorSettingsStateTest`: `initial` copia comensales/alergias/dificultad/franja del perfil
    (`mealTypeId == null`, `notes == ""`); un `defaultServings` fuera de rango se recorta al rango de
    la Taxonomía; `selectTimeBucket`/`selectDifficulty` reemplazan (no acumulan); `selectMealType`
    alterna a `null`; `toggleHardRestriction` añade/quita sin duplicados y `clearHardRestrictions`
    deja `hasNoRestrictions`; `typeNotes` conserva el texto; un caso negativo comprueba que cambiar un
    campo no toca los demás y que las operaciones son inmutables; un test recorre `taxonomy.mealTypes`
    (5 opciones, incluye "Postre"/`dessert`).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `GeneratorSettingsState` puro sin imports de Compose/Android; los editores de
  `ui.preferences` reutilizados sin modificarlos; sin cambios en `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `domain/**` ni `data/**`; `GeneratorFlow` no convierte el estado a
  `GenerationConstraints` ni llama al generador.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - El stepper aparece con los **comensales habituales del perfil** (verificación exigida).
  - Los **cinco tipos de comida incluyen Postre** (verificación exigida).
  - Los grupos se ven en tarjetas, las alergias en **ámbar** (nunca rojo) y "Editar en mi perfil"
    lleva a la pestaña Perfil; "Atrás" conserva los ajustes al volver del paso 1.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La lógica/estado (precarga, single-select, multi-select, recorte y notas) se cubre
en JVM, que es lo que AGENTS.md exige "cuando sea posible"; el acabado visual es de la persona.

## Evidence To Capture

- Salida de `./gradlew test` con `GeneratorSettingsStateTest`, nº de tests y 0 fallos, y el total
  (esperado: 189 + los nuevos).
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `GeneratorSettingsState` puro sin imports Compose/Android; editores reutilizados sin
  cambios; sin dependencias nuevas; sin conversión a `GenerationConstraints` ni llamada al generador.
- Resultado manual en emulador del stepper precargado, de los cinco tipos (incluido Postre), del ámbar
  de las alergias y del enlace a Perfil (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Paso 2 real sustituye al marcador**: `generator-ingredients-step` dejó `GeneratorSettingsPlaceholder`
  "a rellenar"; esta feature monta la pantalla y elimina el placeholder, igual que el paso 1 sustituyó
  su `SectionPlaceholder`.
- **Precarga desde el `PreferenceProfile`**: comensales (`defaultServings`), franja, dificultad y
  alergias se copian del perfil; el tipo de comida arranca en `null` porque el perfil no lo tiene. La
  verificación exige el stepper precargado y stitch exige las alergias precargadas.
- **`GeneratorSettingsState` recibe `Servings` (no toda la Taxonomía)**: el recorte del stepper solo
  necesita los límites; el resto de opciones las aporta la pantalla desde `taxonomy`.
- **Tipo de comida single-select opcional**: no hay valor por defecto; tocar el chip seleccionado lo
  deselecciona (`mealTypeId = null` = sin preferencia), coherente con `GenerationConstraints` opcional.
- **Alergias: reutilizar `HardRestrictionsEditor`** (ya pinta banner y chips ámbar + "Ninguna") en vez
  de duplicar el editor; se añade la caption "Ninguna declarada" y el `TextButton` "Editar en mi
  perfil". Nunca se usa el rol de error.
- **Enlace a Perfil por navegación existente**: `onEditProfile` cierra el Generador y selecciona la
  pestaña Perfil con `MainNavigationState` (sin librería de navegación).
- **Borrador por generación y efímero**: vive en `rememberSaveable` del huésped y no se persiste ni
  edita el Perfil.
- **"Generar recetas" sin efecto**: la generación y su estado de carga son de
  `generation-flow-with-validation`.
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Alergia por generación en `GenerationConstraints`**: la restricción por generación vive en el
  estado, pero `GenerationConstraints` solo tiene `profile`; `generation-flow-with-validation` deberá
  decidir cómo honrarla (extender `GenerationConstraints` con `hardRestrictionIds` opcional o copiar
  el `PreferenceProfile`). Esta feature no convierte el estado a `GenerationConstraints`.
- **Enlace a Perfil sin pantalla real**: hoy aterriza en el `SectionPlaceholder` de Perfil; el editor
  real llega con `profile-screen`. No es un fallo de esta feature.
- **Precarga de franja/dificultad**: stitch no lo pide explícitamente; se precargan del perfil por
  coherencia. Si el repaso visual lo considera confuso, se corrige con feature correctiva.
- **Densidad/densidad táctil de seis tarjetas**: puede requerir ajuste visual; se corrige con feature
  correctiva.
- **Persistencia del borrador**: no sobrevive al cierre de la app (solo a cambios de
  configuración/proceso de la sesión).

## Validator Checklist

- [ ] El paso 2 es **real** (no el marcador); no se implementa generación, validación, carga,
      resultados ni detalle.
- [ ] El stepper aparece con los **comensales habituales del perfil** y el estado arranca con las
      alergias, la franja y la dificultad del perfil (escenario 1, verificación exigida).
- [ ] Los cinco tipos de comida salen de `taxonomy.mealTypes` e incluyen **Postre** (escenario 2,
      verificación exigida).
- [ ] Tiempo y dificultad son single-select que reemplazan; comensales se recortan a
      `taxonomy.servings`; las alergias son multi-select en ámbar (nunca rojo) con "Ninguna".
- [ ] "Editar en mi perfil" invoca `onEditProfile` y `MainShell` navega a la pestaña Perfil sin
      construir la pantalla Perfil.
- [ ] Las notas libres se capturan tal cual; el texto en blanco no rompe nada.
- [ ] El borrador se guarda con `rememberSaveable` + `Saver` y se conserva al ir y volver del paso 1;
      "Generar recetas" no tiene efecto.
- [ ] Los editores de `ui.preferences` se reutilizan **sin modificarlos**; no hay labels hardcodeados
      ni listas duplicadas.
- [ ] La lógica pura (`GeneratorSettingsState`) está cubierta en JVM con casos negativos que no pasan en
      vacío.
- [ ] No se toca dominio, persistencia, la Taxonomía ni el build; sin dependencias nuevas ni iconos.
- [ ] `init.sh` queda en verde y la evidencia está en `feature_list.json`/`PROGRESS.md`;
      `docs/technical-discovery.md` queda coherente.
