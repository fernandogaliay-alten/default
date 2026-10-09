# Feature Implementation Spec: Onboarding: ingredientes, alérgenos y tu cocina

## Source Feature

- `id`: `onboarding-ingredients-safety-kitchen`
- `area`: `ui`
- `depends_on`: `["onboarding-diet-style-cuisines"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Cerrar el onboarding sustituyendo los marcadores de los **pasos 5, 6 y 7** por sus editores reales y
**persistiendo el `UserProfile` completo**:

1. **Paso 5 (`INGREDIENTS`)** — ingredientes que **me gustan** (`preferredIngredientIds`) y que
   **evito** (`dislikedIngredientIds`), como chips de la Taxonomía, con una nota de que un ingrediente
   se comparte entre ambos grupos: elegirlo en "Me gusta" lo quita de "Evito" y viceversa.
2. **Paso 6 (`RESTRICTIONS`)** — **restricciones duras** (`hardRestrictionIds`) como pantalla propia de
   seguridad, con banner de tinte ámbar y chip "Ninguna". Los chips van en **ámbar, nunca en rojo**.
3. **Paso 7 (`KITCHEN`)** — nivel de cocina (`difficultyId`), tiempo disponible (`timeBucketId`) y
   comensales habituales (`defaultServings`), los comensales con un `stepper`. Los comensales
   habituales precargan el stepper del generador (feature posterior), por eso se persisten.
4. **Cierre**: construir el `UserProfile` desde la identidad (paso 1) y el borrador (pasos 2–7) y
   persistirlo con `ProfileRepository.save(...)`. Con el Perfil persistido, la puerta `AppGate`
   decide `MAIN`: el onboarding **no vuelve a aparecer** al reabrir la app.

La feature entrega, además, los editores de los pasos 5–7 como **componentes reutilizables y sin
estado** en `ui.preferences` (regla "un editor, dos entradas"), que la pantalla Perfil reutilizará
después, y la **lógica pura** (borrador y construcción del Perfil) testeable en JVM. Reutiliza el
armazón ya existente (`OnboardingScaffold`/`OnboardingProgress`) y **no** añade dependencias.

## Non-Goals

- **No** se implementa la pantalla Perfil (`profile-screen`): solo se preparan los editores para que
  Perfil los reutilice; no se crean hojas superpuestas, filas de resumen ni navegación a Perfil.
- **No** se implementa la Home real ni la barra de navegación inferior / FAB
  (`home-offline-blocks`); `HomePlaceholder` sigue siendo el destino tras el Perfil.
- **No** se introduce una librería de navegación ni inyección de dependencias; la puerta y el cambio
  de destino siguen siendo el estado mínimo ya aceptado (`AppGate` + `ProfileRepository.observe()`).
- **No** se añade el flag de onboarding en DataStore ni reanudación de un onboarding interrumpido: la
  puerta sigue siendo la **presencia de Perfil** en Room (decisión de `onboarding-shell-and-identity`).
- **No** se añaden dependencias: ni `material-icons`/Material Symbols, ni imágenes, ni DI. Los
  editores no renderizan iconos (el repositorio no los tiene).
- **No** se admiten ingredientes libres en el paso 5: `PreferenceProfile` solo guarda ids
  (`preferredIngredientIds`/`dislikedIngredientIds`) y esta feature no cambia el esquema; el buscador
  filtra la lista de la Taxonomía, no crea texto libre.
- **No** se reconcilia de oficio "Me gusta" con "Restricciones duras" (p. ej. gustar de `pan` y marcar
  `gluten`): la seguridad la garantiza el Validador aguas abajo; el onboarding no borra selecciones.
- **No** se calcula ni persiste la Firma de preferencias (eso es `suggestion-batch-cache`), ni se
  toca `PreferenceSignature`, `RecipeGenerator`, la Taxonomía ni el modelo de Receta.

## Job Story

Cuando termino de contarle a Sazón lo que cocino,
quiero decirle qué ingredientes me gustan y cuáles evito, qué alergias o intolerancias tengo y cómo
es mi cocina (nivel, tiempo y comensales),
para que la app guarde mi Perfil completo y empiece a proponerme recetas seguras y a mi medida sin
volver a preguntármelo.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; no hay cuentas, roles ni compartición (ADR 0002).
  No puede saltarse el onboarding y elige entre las opciones de la Taxonomía.
- **Desarrollador / agente**: implementa los editores, el borrador y el cierre y verifica la
  **lógica/estado en JVM**; el acabado puramente visual (ámbar de alérgenos, tratamientos de chip) lo
  revisa la persona tras el merge (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: el paso 5 ofrece ingredientes de la Taxonomía en dos grupos compartidos

Given el onboarding en el paso 5 (`INGREDIENTS`),
When se renderiza el editor,
Then se ofrecen los `ingredients` de la Taxonomía como chips en dos grupos, **"Me gusta"** (tratamiento
terracota) y **"Evito"** (tratamiento oliva, con el helper "No son alergias, solo cosas que prefieres
no ver."), hay un buscador que filtra el listado por `label`, y se indica que un ingrediente se
comparte entre ambos grupos.

### Scenario 2: elegir en "Me gusta" lo quita de "Evito" y viceversa (verificación exigida)

Given el borrador del paso 5 sin selección,
When se elige `tomate` en "Me gusta" y después se elige `tomate` en "Evito",
Then `preferredIngredientIds` deja de contener `tomate` y `dislikedIngredientIds` lo contiene (el
ingrediente **nunca** está en las dos listas a la vez); y al revés, elegirlo otra vez en "Me gusta" lo
quita de "Evito".

### Scenario 3: alternancia con semántica de conjunto y sin duplicados

Given `tomate` ya está en "Me gusta",
When se vuelve a pulsar,
Then se quita de `preferredIngredientIds`; alternar dos veces el mismo id deja la lista como estaba y
nunca aparecen duplicados en ninguno de los dos grupos.

### Scenario 4: caso negativo — "Me gusta" y "Evito" no se solapan nunca

Given cualquier secuencia de alternancias sobre los mismos ids,
When se inspecciona el borrador,
Then la intersección de `preferredIngredientIds` y `dislikedIngredientIds` es **vacía** (la UI impide
el solape que describe `docs/domain-model.md`).

### Scenario 5: el paso 6 es la pantalla propia de seguridad y ofrece las restricciones duras

Given el onboarding en el paso 6 (`RESTRICTIONS`),
When se renderiza el editor,
Then aparece el banner de tinte ámbar "Lo usamos para que ninguna receta ponga en riesgo tu salud." y
se ofrecen **exactamente** los 11 `hardRestrictions` de la Taxonomía (Gluten, Lactosa, Frutos secos,
Cacahuete, Soja, Marisco, Pescado, Huevo, Sésamo, Apio, Mostaza) como chips multi-select, más un chip
**"Ninguna"** seleccionado cuando no hay ninguna restricción marcada.

### Scenario 6: alternancia de restricciones y "Ninguna" sin residuo; chips en ámbar, nunca rojo

Given el paso 6 con varias restricciones marcadas,
When se pulsa "Ninguna",
Then `hardRestrictionIds` queda vacía y `hasNoRestrictions` es verdadero; al marcar después una
restricción, la lista contiene **solo** esa. Los chips seleccionados se pintan en **ámbar**
(`colorScheme.tertiary`/`tertiaryContainer`) y en ningún caso con el rol de error/rojo.

### Scenario 7: el paso 7 muestra las tres colecciones de la Taxonomía con valores por defecto

Given el onboarding en el paso 7 (`KITCHEN`),
When se renderiza el editor,
Then aparecen tres grupos — "Nivel de cocina" (los 3 `difficulties`), "¿Cuánto tiempo sueles tener?"
(los 5 `timeBuckets`) y "¿Para cuántos cocinas normalmente?" (un `stepper` entre
`taxonomy.servings.min` y `max`) — con `difficultyId` = primer `difficulties` (`easy`), `timeBucketId`
= primer `timeBuckets` (`lte_15`) y `defaultServings` = `taxonomy.servings.default` (`2`).

### Scenario 8: paso 7 single-select y límites del stepper

Given el paso 7,
When se elige otra dificultad u otra franja,
Then la elección **reemplaza** a la anterior (nunca hay dos activas); y al mover los comensales el
valor se mantiene dentro de `1..12` (no baja de `min` ni sube de `max`).

### Scenario 9: caso negativo — cada paso escribe solo sus campos

Given un borrador con ingredientes, restricciones y cocina elegidos,
When se cambia cualquier campo de los pasos 5–7,
Then los campos de los pasos 2–4 (dieta, estilo, cocinas) y el resto de campos de los pasos 5–7 **no**
cambian (cada editor toca solo lo suyo).

### Scenario 10: el cierre construye y persiste el `UserProfile` completo

Given un onboarding completado con identidad y borrador válidos,
When se pulsa "Empezar a cocinar",
Then se construye un `UserProfile` puro con `displayName` normalizado (1–40), `avatarColorId` del paso
1, `createdAt`/`updatedAt` = instante del cierre y un `PreferenceProfile` con **todos** los campos del
borrador (dieta, restricciones duras, estilo, cocinas, ingredientes que me gustan/evito, dificultad,
tiempo y comensales), y se invoca `ProfileRepository.save(profile)` una sola vez.

### Scenario 11: tras el cierre el onboarding no vuelve a aparecer (verificación exigida)

Given el `UserProfile` ya persistido,
When `ProfileRepository.observe()` emite el Perfil,
Then `AppGate.decide(profile)` es `MAIN` y la app muestra el destino principal; al reabrir la app el
onboarding **no** se vuelve a mostrar (la puerta por presencia de Perfil ya está aceptada).

### Scenario 12: caso negativo — un nombre inválido no cierra el onboarding

Given el paso 7 con un nombre que quedó inválido (vacío, solo espacios o >40 tras recortar),
When se intenta cerrar,
Then el primario está **deshabilitado** y el constructor puro rechaza el perfil con
`IllegalArgumentException`; no se escribe ninguna fila.

## Repository Research

### Files Inspected

- `feature_list.json` — definición, `depends_on`, la verificación manual exigida ("Me gusta" quita de
  "Evito"; chips de alérgenos en ámbar y nunca en rojo; el onboarding no reaparece) y la nota "los
  comensales habituales precargan el stepper del generador".
- `PROGRESS.md` — `onboarding-diet-style-cuisines` está `accepted`; los pasos 5–7 son marcadores y el
  cierre/persistencia se difirió a esta feature (sesiones 024/027).
- `CONTEXT.md` — vocabulario: `Ingredientes que evito` (preferencia blanda, no confundir con
  Restricción dura), `Restricción dura` (seguridad), `Comensales habituales`, `Onboarding`,
  `Perfil de preferencias`, `Taxonomía`.
- `docs/domain-model.md` — `PreferenceProfile` (`preferredIngredientIds[]`, `dislikedIngredientIds[]`,
  `hardRestrictionIds[]`, `difficultyId`, `timeBucketId`, `defaultServings`); edge case "ingrediente
  en 'me gusta' y en 'evito' a la vez: gana 'evito'; la UI debe impedir el solape"; estados del
  onboarding.
- `docs/build-brief.md` — onboarding obligatorio de 7 pasos; "una decisión por pantalla"; los
  comensales habituales precargan el generador; el Perfil edita lo mismo que el onboarding.
- `docs/domain/taxonomy.md` y `docs/domain/taxonomy.json` — `ingredients` (95, multi-select + texto
  libre, con `label`/`allergens`/`tags`), `hardRestrictions` (11, seguridad, multi-select),
  `difficulties` (3, `level`), `timeBuckets` (5, `maxMinutes`), `servings` (`min` 1, `max` 12,
  `default` 2) y la regla "la UI nunca diverge de la Taxonomía"; `usedIn` marca `onboarding.step5/6/7`.
- `DESIGN.md` — tokens (terracota = acción principal, oliva = vegetal, ámbar = avisos de alérgenos,
  **nunca rojo**; formas 16/24/píldora; espaciado 12/16/24; 48dp táctil) y regla "una acción principal
  por pantalla".
- `docs/design/stitch-prompt.md` — sección A pasos 5–7: microcopy exacto ("¿Qué te gusta cocinar?",
  grupos "Me gusta"/"Evito" con tratamiento oliva y helper, buscador "+ Añadir otro", banner ámbar,
  "Alergias e intolerancias", "Tu cocina" con tres grupos y CTA "Empezar a cocinar"); sección G,
  regla "un editor, dos entradas" (mismas pantallas reutilizadas en Perfil).
- `docs/specs/onboarding-shell-and-identity.md` — contrato del armazón, `onFinished` **sin efecto**
  en esa feature, puerta por presencia de Perfil y decisión de no persistir un perfil inventado.
- `docs/specs/onboarding-diet-style-cuisines.md` — patrón de borrador en memoria
  (`OnboardingPreferencesDraft` + Saver), editores sin estado en `ui.preferences` y "un editor, dos
  entradas".
- `docs/specs/profile-persistence.md` / `docs/domain-model.md` — `UserProfile`/`PreferenceProfile`
  no-null, `ProfileName` 1–40 con recorte, `ProfileRepository.save` (normaliza y rechaza nombre
  inválido) y `ProfileRepository.observe()`.
- `docs/technical-discovery.md` — stack Compose/Material 3; la puerta usa la presencia de Perfil; el
  dominio puro se testea en JVM sin emulador.
- `docs/risks-and-open-questions.md` — sin preguntas nuevas que afecten a esta feature.
- Código: `ui/onboarding/{OnboardingFlow,OnboardingStep,OnboardingProgress,OnboardingScaffold,
  OnboardingPreferencesDraft,OnboardingIdentityState}.kt`, `ui/preferences/{DietPatternEditor,
  SoftPreferencesEditor,CuisinesEditor}.kt`, `ui/{SazonApp,AppGate,HomePlaceholder}.kt`,
  `domain/profile/{UserProfile,ProfileName,ProfileRepository}.kt`,
  `data/profile/{RoomProfileRepository,ProfileMappers}.kt`, `domain/taxonomy/Taxonomy.kt`,
  `ui/theme/{Color,Spacing,Shape,Theme}.kt`, `MainActivity.kt`.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — sin `material-icons`; FlowRow (Experimental
  Layout API) ya se usa en `SoftPreferencesEditor`.

### Existing Patterns To Follow

- **Lógica pura sin Compose/Android** en `ui.onboarding` (como `OnboardingProgress`,
  `OnboardingPreferencesDraft`), testeada en JVM con JUnit 4 y nombres de test en español; solo las
  pantallas dependen de Android.
- **Un editor, dos entradas**: componentes **sin estado** con `value` + callbacks, sin conocer el
  onboarding ni el Perfil, en `ui.preferences`, reutilizables por `profile-screen`.
- **Taxonomía como única fuente**: las listas de opciones se reciben de `Taxonomy`; nunca se duplican
  ni se hardcodean labels.
- **Tokens**: colores/espaciado/formas desde `com.sazon.app.ui.theme` (nunca hex sueltos);
  terracota = `primary`/`primaryContainer`, oliva = `secondary`/`secondaryContainer`, ámbar =
  `tertiary`/`tertiaryContainer`. El rol `error` **no** se usa para alérgenos.
- **`rememberSaveable`** con un `Saver` en la capa Compose para el borrador del flujo.
- **Casos negativos** en los tests que fallarían si la lógica no hiciera nada (patrón
  `OnboardingPreferencesDraftTest`).
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- Los pasos 5–7 son `OnboardingPlaceholderStep` ("Este paso llega en una próxima versión."); no
  existen los editores de ingredientes, restricciones ni cocina.
- `OnboardingPreferencesDraft` solo cubre los pasos 2–4; no hay un borrador para los campos de los
  pasos 5–7.
- No existe un constructor puro `UserProfile` desde el borrador ni nada que invoque
  `ProfileRepository.save`: el `onFinished` de `OnboardingFlow`/`SazonApp` es una costura sin efecto.

## Technical Approach

Estrategia: **lógica pura testeable en JVM + editores Compose delgados y sin estado + cierre que
persiste el Perfil**, sin dependencias nuevas.

### 1. Lógica pura (ampliar) — `com.sazon.app.ui.onboarding.OnboardingPreferencesDraft`

Se **amplía** la data class pura ya existente (los campos nuevos no rompen su test actual, que usa
`initial(taxonomy)` y las operaciones) para cubrir todos los pasos:

- Campos añadidos: `preferredIngredientIds`, `dislikedIngredientIds`, `hardRestrictionIds`,
  `difficultyId`, `timeBucketId`, `defaultServings`.
- `initial(taxonomy)` añade: listas nuevas vacías, `difficultyId = taxonomy.difficulties.first().id`
  (`easy`), `timeBucketId = taxonomy.timeBuckets.first().id` (`lte_15`) y
  `defaultServings = taxonomy.servings.default` (`2`).
- Ingredientes con **exclusión mutua**: `togglePreferredIngredient(id)` alterna en
  `preferredIngredientIds`; si **añade**, quita `id` de `dislikedIngredientIds`; si **quita**, no toca
  el otro grupo. `toggleDislikedIngredient(id)` simétrico. Operaciones **inmutables** (`copy`) y con
  semántica de conjunto (sin duplicados).
- Restricciones duras: `toggleHardRestriction(id)` (conjunto), `clearHardRestrictions()` y
  `hasNoRestrictions: Boolean` ("Ninguna" = lista vacía, por construcción nunca coexiste con una
  marcada).
- Cocina: `selectDifficulty(id)` y `selectTimeBucket(id)` (single-select con `copy`),
  `setServings(servings: Int, limits: Servings)` que **recorta** al rango
  `limits.min..limits.max` (garantiza un Perfil válido).
- No valida ids contra la Taxonomía: el editor solo ofrece ids válidos; la validación del Perfil no
  pertenece aquí (coherente con `profile-persistence`).

### 2. Lógica pura (nueva) — `com.sazon.app.ui.onboarding.OnboardingProfileBuilder`

`object` puro, sin Compose:

- `build(displayName: String, avatarColorId: String, draft: OnboardingPreferencesDraft, now: Long):
  UserProfile` — `require(ProfileName.isValid(displayName))` (mismo contrato que
  `RoomProfileRepository.save`), normaliza el nombre con `ProfileName.normalize`, usa `now` para
  `createdAt` y `updatedAt` y copia **todos** los campos del borrador a `PreferenceProfile`. Es la
  única traducción borrador → Perfil, testeable en JVM y sin reloj oculto (el instante se inyecta).

### 3. Editores reutilizables (nuevos) — `com.sazon.app.ui.preferences`

Composables **sin estado**, con listas de la Taxonomía y `value` + callbacks; no conocen el onboarding
ni el Perfil:

- `IngredientsEditor(ingredients, preferredIds, dislikedIds, query, onQueryChange,
  onTogglePreferred, onToggleDisliked)`: buscador (filtro por `label`) y **dos grupos** "Me gusta"
  (chips terracota) y "Evito" (chips oliva + helper "No son alergias, solo cosas que prefieres no
  ver.") sobre el mismo listado filtrado; la nota de que el ingrediente se comparte. Sin iconos.
- `HardRestrictionsEditor(restrictions, selectedIds, onToggle, onClear)`: banner ámbar con
  "Lo usamos para que ninguna receta ponga en riesgo tu salud." y chips multi-select + "Ninguna"
  (seleccionado cuando `selectedIds` está vacía). Chips seleccionados en **ámbar**
  (`tertiaryContainer`/`tertiary`), **nunca** `error`/rojo.
- `DifficultyEditor(difficulties, selectedId, onSelect)`: chips single-select; `level` puede ordenar.
- `TimeBucketEditor(timeBuckets, selectedId, onSelect)`: chips single-select.
- `ServingsStepper(servings: Servings, value: Int, onChange: (Int) -> Unit)`: `− value +` con cifras
  tabulares, `−` deshabilitado en `min` y `+` en `max` (tokens del tema).
- Filtro puro del buscador: `IngredientsFilter.matching(ingredients: List<Ingredient>, query: String):
  List<Ingredient>` (sin Compose), insensible a mayúsculas y acentos; `query` en blanco devuelve
  todos. Se testea en JVM.

### 4. Cableado en `OnboardingFlow` y cierre

- Ampliar el `Saver` del borrador (`mapSaver` o equivalente) para que **todos** los campos nuevos
  hagan ida y vuelta.
- Enrutar `INGREDIENTS` → `IngredientsEditor` (+ título del paso), `RESTRICTIONS` →
  `HardRestrictionsEditor` y `KITCHEN` → los tres editores de cocina.
- `primaryEnabled` = `true` en los pasos 5–7 (ingredientes y restricciones son opcionales; cocina
  tiene valores por defecto); la regla por identidad del paso 1 se mantiene para todo el flujo.
- Cambiar el contrato `onFinished: () -> Unit` a `onFinished: (UserProfile) -> Unit`. En el último
  paso, `OnboardingFlow` construye el Perfil con
  `OnboardingProfileBuilder.build(name, avatarColorId, draft, System.currentTimeMillis())` y lo
  entrega al callback. El `OnboardingFlow` no conoce el repositorio ni las coroutines.

### 5. Cierre en `SazonApp`

- `SazonApp` recibe el `ProfileRepository` (ya lo tiene) y añade un `rememberCoroutineScope`:
  `OnboardingFlow(taxonomy, onFinished = { profile -> scope.launch { profileRepository.save(profile) } })`.
- Como `SazonApp` observa `profileRepository.observe()`, tras el `save` el flujo emite el Perfil y
  `AppGate.decide` pasa a `MAIN`; el onboarding desaparece sin navegación adicional. `HomePlaceholder`
  sigue siendo el destino (feature posterior lo sustituye).

### 6. Sin cambios de build

No se tocan `app/build.gradle.kts` ni `gradle/libs.versions.toml`: no se añaden dependencias. No
cambian `OnboardingStep`, `OnboardingProgress`, `OnboardingScaffold`, `AppGate` ni `MainActivity`.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraft.kt` — modificar; campos
  de los pasos 5–7 y sus operaciones (exclusión mutua, restricciones, cocina).
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingProfileBuilder.kt` — crear; construcción
  pura del `UserProfile` desde el borrador.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingFlow.kt` — modificar; Saver ampliado,
  enrutado de los pasos 5–7 y `onFinished(UserProfile)`.
- `app/src/main/java/com/sazon/app/ui/preferences/IngredientsEditor.kt` — crear; paso 5 (dos grupos).
- `app/src/main/java/com/sazon/app/ui/preferences/HardRestrictionsEditor.kt` — crear; paso 6 (banner
  ámbar + chips + "Ninguna").
- `app/src/main/java/com/sazon/app/ui/preferences/DifficultyEditor.kt` — crear; nivel de cocina.
- `app/src/main/java/com/sazon/app/ui/preferences/TimeBucketEditor.kt` — crear; tiempo disponible.
- `app/src/main/java/com/sazon/app/ui/preferences/ServingsStepper.kt` — crear; comensales.
- `app/src/main/java/com/sazon/app/ui/preferences/IngredientsFilter.kt` — crear; filtro puro del
  buscador (sin Compose).
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — modificar; guardar el Perfil al cerrar el
  onboarding (coroutine + `ProfileRepository.save`).
- `app/src/test/java/com/sazon/app/ui/onboarding/OnboardingIngredientsDraftTest.kt` — crear;
  escenarios 1–9 (lógica pura del borrador).
- `app/src/test/java/com/sazon/app/ui/onboarding/OnboardingProfileBuilderTest.kt` — crear; escenarios
  10 y 12 (construcción y cierre lógico).
- `app/src/test/java/com/sazon/app/ui/preferences/IngredientsFilterTest.kt` — crear; filtro del
  buscador (casos con/sin acentos y consulta vacía).
- `docs/technical-discovery.md` — modificar; registrar el borrador ampliado, los editores de los
  pasos 5–7, el constructor del Perfil y el cierre que persiste con `ProfileRepository.save`.
- `docs/domain-model.md` — **no** cambia (describe ya `PreferenceProfile` y sus campos).
- `feature_list.json` y `PROGRESS.md` — los actualiza el implementer al cerrar; no el planner.

No se modifican `app/build.gradle.kts` ni `gradle/libs.versions.toml`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` (sección A pasos 5–7; sección G para la
  futura reutilización en Perfil).
- Screens or states affected: paso 5 (dos grupos de chips + buscador), paso 6 (banner ámbar + chips
  multi-select + "Ninguna"), paso 7 (chips de dificultad y tiempo + stepper de comensales); estados
  seleccionado/no seleccionado; "Ninguna" activo cuando la lista está vacía.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonShapes` (24dp tarjetas),
  `SazonPillShape` (chips), `primary`/`primaryContainer` (Me gusta, acción principal),
  `secondary`/`secondaryContainer` (Evito), `tertiary`/`tertiaryContainer` (ámbar de alérgenos),
  `outline` (no seleccionados). El rol `error`/rojo **no** se usa para alérgenos.
- Reglas visuales: una sola acción principal (el primario del armazón) por pantalla; objetivos
  táctiles ≥48dp; tipografía dinámica (scroll, nunca recortar); los chips de alérgenos son información
  de seguridad, no una alarma.
- Iconos: no se renderizan (sin assets ni dependencia de iconos); los chips muestran su `label`.
- Capturas de referencia (backfill PR #37):
  `docs/design/concepts/onboarding-ingredients-safety-kitchen/` — pantallas `ingredientes`, `alergias`,
  `tu-cocina` (pasos 5–7). Pendiente de exportar desde Stitch (petición en el `README.md` de la
  carpeta); el acabado puramente visual (tinte ámbar, tratamientos de selección, claro/oscuro) lo revisa
  la persona tras el merge.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — registrar los campos nuevos del borrador, los editores
  reutilizables de los pasos 5–7, el `OnboardingProfileBuilder` y que el cierre persiste el
  `UserProfile` (`ProfileRepository.save`) y la puerta pasa a `MAIN`.
- `docs/domain-model.md`: **not needed** — describe ya `PreferenceProfile`, `Restricción dura`,
  `Ingredientes que evito`, `Comensales habituales` y el estado del onboarding.
- `docs/design/concepts/`: **update** — backfill de capturas (PR #37): pantallas `ingredientes`,
  `alergias`, `tu-cocina` en `docs/design/concepts/onboarding-ingredients-safety-kitchen/`.

## Implementation Plan

1. Ampliar `OnboardingPreferencesDraft` con los campos de los pasos 5–7 y las operaciones (exclusión
   mutua de ingredientes, restricciones con "Ninguna", cocina con recorte de comensales).
2. Crear `OnboardingProfileBuilder` (builder puro del `UserProfile`).
3. Crear los editores sin estado en `ui.preferences` (`IngredientsEditor`, `HardRestrictionsEditor`,
   `DifficultyEditor`, `TimeBucketEditor`, `ServingsStepper`) y `IngredientsFilter` (filtro puro), con
   tokens del tema y sin iconos, usando ámbar (nunca rojo) en alérgenos.
4. Cablear `OnboardingFlow`: Saver ampliado, enrutado de `INGREDIENTS`/`RESTRICTIONS`/`KITCHEN` y
   `onFinished(UserProfile)` que construye el Perfil al pulsar "Empezar a cocinar".
5. Cablear `SazonApp` para persistir el Perfil (`rememberCoroutineScope` + `ProfileRepository.save`);
   la puerta pasa a `MAIN` por la observación existente.
6. Escribir los tests JVM (`OnboardingIngredientsDraftTest`, `OnboardingProfileBuilderTest`,
   `IngredientsFilterTest`) y dejar el gate en verde.
7. Actualizar `docs/technical-discovery.md`; el implementer registra evidencia y estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Ampliar `OnboardingPreferencesDraft` con `preferredIngredientIds`, `dislikedIngredientIds`,
      `hardRestrictionIds`, `difficultyId`, `timeBucketId` y `defaultServings`.
- [ ] Añadir `togglePreferredIngredient`/`toggleDislikedIngredient` con exclusión mutua y semántica de
      conjunto; `toggleHardRestriction`/`clearHardRestrictions`/`hasNoRestrictions`;
      `selectDifficulty`/`selectTimeBucket`/`setServings(limits)`.
- [ ] Actualizar `initial(taxonomy)` con los valores por defecto de la Taxonomía (primer
      `difficulties`/`timeBuckets` y `servings.default`).
- [ ] Crear `OnboardingProfileBuilder.build(...)` puro (nombre validado/normalizado, `now` inyectado y
      `PreferenceProfile` completo) y `IngredientsFilter.matching(...)`.
- [ ] Crear `IngredientsEditor` (buscador + grupos "Me gusta"/"Evito" con nota de compartición).
- [ ] Crear `HardRestrictionsEditor` (banner ámbar + chips + "Ninguna", sin rojo).
- [ ] Crear `DifficultyEditor`, `TimeBucketEditor` y `ServingsStepper`.
- [ ] Ampliar el `Saver` del borrador y enrutar los pasos 5–7 en `OnboardingFlow`; cambiar
      `onFinished` a `(UserProfile) -> Unit`.
- [ ] Cablear `SazonApp` para guardar el Perfil al cerrar (coroutine + `ProfileRepository.save`).
- [ ] Escribir `OnboardingIngredientsDraftTest`, `OnboardingProfileBuilderTest` e
      `IngredientsFilterTest` con casos negativos.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: los tests nuevos en verde sin romper los 93
  existentes:
  - `OnboardingIngredientsDraftTest` (escenarios 1–9): `initial(taxonomy)` arranca con dificultad
    `easy`, franja `lte_15` y `defaultServings == 2`; `togglePreferredIngredient("tomate")` lo deja
    solo en "Me gusta"; `toggleDislikedIngredient("tomate")` lo mueve a "Evito" (intersección vacía);
    alternar dos veces quita; `toggleHardRestriction` añade/quita sin duplicados y
    `clearHardRestrictions()` deja `hasNoRestrictions` verdadero; `setServings` recorta a `1..12`;
    caso negativo de independencia (cambiar un campo no toca los de los pasos 2–4 ni el resto).
  - `OnboardingProfileBuilderTest` (escenarios 10 y 12): el Perfil construido tiene el nombre
    recortado, la identidad, `createdAt == updatedAt == now` y un `PreferenceProfile` igual al
    esperado con **todos** los campos; `AppGate.decide(profile) == MAIN`; un nombre vacío, solo
    espacios o de 41 caracteres lanza `IllegalArgumentException`.
  - `IngredientsFilterTest`: consulta vacía devuelve todo; coincidencia por subcadena insensible a
    mayúsculas/acentos; una consulta sin coincidencias devuelve lista vacía.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `ui.preferences` reutilizable y sin estado; `OnboardingPreferencesDraft` y
  `OnboardingProfileBuilder` sin imports de Compose/Android; `HardRestrictionsEditor` no referencia
  `colorScheme.error` para los chips; sin cambios en `app/build.gradle.kts` ni
  `gradle/libs.versions.toml`.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Paso 5: elegir un ingrediente en "Me gusta" lo quita de "Evito" y viceversa; el buscador filtra.
  - Paso 6: los chips se muestran en **ámbar y nunca en rojo**; "Ninguna" desmarca todo.
  - Paso 7: el stepper de comensales nace con los comensales habituales.
  - Al pulsar "Empezar a cocinar", el onboarding **no vuelve a aparecer** al reabrir la app.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y los tests de la feature corren dentro de `./gradlew test` sin emulador.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La lógica/estado (borrador, exclusión mutua, restricciones, cocina, construcción
del Perfil) se cubre en JVM; el acabado visual y el reinicio real son manuales.

## Evidence To Capture

- Salida de `./gradlew test` con `OnboardingIngredientsDraftTest`, `OnboardingProfileBuilderTest` e
  `IngredientsFilterTest`, nº de tests y 0 fallos, y el total (esperado: 93 + los nuevos).
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: editores reutilizables y sin estado; ausencia de imports Compose/Android en la lógica
  pura; `HardRestrictionsEditor` sin rojo; sin cambios de build.
- Resultado manual en emulador de los pasos 5–7 y del cierre (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y del estado `passing`.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Se amplía `OnboardingPreferencesDraft`** en vez de crear un segundo borrador: es la traducción
  directa a `PreferenceProfile` y evita mantener dos `Saver` y dos fuentes de estado en
  `OnboardingFlow`. El test aceptado de los pasos 2–4 sigue siendo válido (usa `initial` y las
  operaciones).
- **Exclusión mutua en el borrador**: los toggles de "Me gusta"/"Evito" se excluyen por construcción,
  lo que materializa el edge case de `docs/domain-model.md` ("la UI debe impedir el solape") sin
  depender de la regla "gana evito" del validador.
- **Ingredientes solo de la Taxonomía**: `PreferenceProfile` guarda ids, no texto libre; el buscador
  filtra la lista, no crea ingredientes. El paso de ingredientes libres pertenece al generador, no al
  onboarding.
- **Sin iconos**: el repositorio no tiene Material Symbols ni `material-icons`; añadirlos no es
  imprescindible. Los chips muestran su `label`; si la persona quiere iconos, será una feature
  correctiva.
- **Restricciones duras en ámbar (`tertiary`)**: `DESIGN.md` reserva el ámbar para avisos de alérgenos
  y prohíbe el rojo de alarma; el editor nunca usa `colorScheme.error` para los chips.
- **Valores por defecto de cocina**: primer `difficulties` (`easy`), primer `timeBuckets` (`lte_15`) y
  `servings.default` (`2`), siguiendo el patrón de `avatarColors.first()`/`dietPatterns.first()` para
  que el paso 7 siempre tenga un Perfil válido y no bloquee el avance.
- **`defaultServings` precarga el generador**: se persiste aquí porque `docs/domain-model.md` y
  `docs/build-brief.md` lo exigen; `generator-settings-step` lo consumirá (features posteriores).
- **Cierre en `SazonApp`**: el builder es puro y `OnboardingFlow` solo entrega el `UserProfile`; quien
  tiene el repositorio y el scope de coroutines es `SazonApp`. Así el borrador y el flujo siguen sin
  conocer la persistencia, y `AppGate` reacciona por la observación ya aceptada.
- **`onFinished` pasa a `(UserProfile) -> Unit`**: es el contrato que `onboarding-shell-and-identity`
  difirió deliberadamente a esta feature; ya no se necesita una costura sin efecto.
- **Sin dependencias nuevas**: los editores usan `FlowRow` (API experimental ya usada) y los tokens
  existentes.

### Unknowns

- **Reutilización exacta en Perfil**: la firma (`value` + callbacks) es suficiente; si `profile-screen`
  necesita un envoltorio en hoja superpuesta, lo añadirá sin cambiar los editores.
- **Peso visual del paso 5**: mostrar los 95 ingredientes en dos grupos es denso; el buscador lo
  mitiga. Si el repaso visual no convence, se corrige con una feature correctiva (no bloquea).
- **Round-trip del `Saver` ampliado**: no se testea en JVM (vive en la capa Compose); se verifica por
  inspección y con la retención al ir y volver en el emulador, como en los pasos 2–4.
- **Fallo de `save`**: si `ProfileRepository.save` lanzara (no debería, porque el primario exige un
  nombre válido), la app no navegaría y el onboarding seguiría visible. No se añade manejo de error
  en esta feature.

## Validator Checklist

- [ ] La implementación se queda en los pasos 5–7 y el cierre: sin pantalla Perfil real, sin Home
      real, sin barra inferior/FAB, sin DataStore, sin navegación con librería ni DI.
- [ ] Los escenarios 1–12 pasan; los tests JVM cubren exclusión mutua, restricciones con "Ninguna",
      cocina con recorte de comensales y el builder del Perfil, con casos negativos que no pasan en
      vacío.
- [ ] Elegir un ingrediente en "Me gusta" lo quita de "Evito" y viceversa; ambos grupos nunca se
      solapan.
- [ ] Los chips de alérgenos usan ámbar (`tertiary`/`tertiaryContainer`) y **nunca** el rol de error.
- [ ] Las opciones de los editores son **exactamente** las de `ingredients`, `hardRestrictions`,
      `difficulties`, `timeBuckets` y `servings` de la Taxonomía; no hay listas duplicadas ni labels
      hardcodeados.
- [ ] Al cerrar se persiste un `UserProfile` con todos los campos del borrador y, por la puerta ya
      aceptada, el onboarding no vuelve a aparecer.
- [ ] Los editores viven en `ui.preferences`, están sin estado y no dependen del onboarding ni del
      Perfil (reutilizables por `profile-screen`).
- [ ] No hay hex sueltos; los colores/formas salen del tema; no se usan iconos ni se añade la
      dependencia de iconos.
- [ ] `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian (sin dependencias nuevas).
- [ ] La evidencia está en `feature_list.json`/`PROGRESS.md` y `docs/technical-discovery.md` queda
      coherente con el borrador ampliado, los editores y el cierre.
