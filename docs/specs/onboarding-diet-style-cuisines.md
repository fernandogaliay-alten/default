# Feature Implementation Spec: Onboarding: dieta, estilo y cocinas

## Source Feature

- `id`: `onboarding-diet-style-cuisines`
- `area`: `ui`
- `depends_on`: `["onboarding-shell-and-identity"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Sustituir los marcadores de posición de los **pasos 2, 3 y 4** del onboarding por sus editores reales,
alimentados por la Taxonomía: **patrón de dieta** (single-select, paso 2), **preferencias de estilo**
(multi-select opcional con "Ninguno", paso 3) y **cocinas favoritas** (multi-select, paso 4). Las
selecciones se guardan en un **borrador en memoria** dentro de `OnboardingFlow`, sobre el armazón ya
existente (`OnboardingScaffold`/`OnboardingProgress`), sin persistir un Perfil (eso es de
`onboarding-ingredients-safety-kitchen`).

La feature entrega, además, los tres editores como **componentes reutilizables y sin estado** que la
pantalla Perfil reutilizará después en hojas superpuestas (regla "un editor, dos entradas"), y la
**lógica pura** de selección (borrador y alternancia) testeable en JVM. Los pasos 5–7 siguen siendo
marcadores: pertenecen a `onboarding-ingredients-safety-kitchen`.

## Non-Goals

- **No** se implementan los pasos 5, 6 y 7 (ingredientes, restricciones duras y "tu cocina"): siguen
  como `OnboardingPlaceholderStep` y su editor real llega en `onboarding-ingredients-safety-kitchen`.
  En particular, **no** se toca el editor de alérgenos (es seguridad y tiene pantalla propia).
- **No** se persiste el Perfil ni el borrador parcial: ni Room, ni DataStore, ni recuperación de un
  onboarding interrumpido. El guardado/cierre sigue en la última feature del onboarding; esta feature
  mantiene el borrador en memoria (`rememberSaveable`) como ya hace el armazón.
- **No** se implementa la pantalla Perfil (`profile-screen`): solo se preparan los editores para que
  Perfil los reutilice; no se crean hojas superpuestas, filas de resumen ni navegación a Perfil.
- **No** se introduce la barra de navegación inferior, el FAB, la Home real ni navegación con
  librería; no se toca `SazonApp`/`AppGate`/`HomePlaceholder`.
- **No** se añaden dependencias: ni `material-icons`/Material Symbols, ni librería de navegación, ni
  DI. Los editores no renderizan iconos porque el repositorio no tiene assets ni dependencia de
  iconos (ver "Decisions And Unknowns").
- **No** se inventan opciones fuera de la Taxonomía ni se duplica ninguna lista de la Taxonomía en
  código.

## Job Story

Cuando construyo mi perfil de gustos en el onboarding,
quiero decirle a Sazón cómo me alimento, qué estilos me importan y qué cocinas prefiero, eligiendo de
opciones reales y cortas,
para que la app personalice sus sugerencias sin pedírmelo otra vez.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); no puede saltarse el onboarding y elige
  entre las opciones de la Taxonomía.
- **Desarrollador / agente**: implementa los editores y el borrador y verifica la **lógica/estado en
  JVM**; el acabado puramente visual lo revisa la persona tras el merge (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: el paso 2 ofrece los patrones de dieta de la Taxonomía y elige uno solo

Given el onboarding en el paso 2 (`DIET`),
When se renderiza el editor de dieta,
Then se ofrecen **exactamente** los `dietPatterns` de la Taxonomía (5: Omnívoro, Flexitariano,
Pescetariano, Vegetariano, Vegano) con su `label` y `description`; hay **una** selección y su valor
inicial es el primer patrón (`omnivore`); elegir otro patrón **reemplaza** al anterior (nunca quedan
dos seleccionados).

### Scenario 2: el paso 3 es opcional y admite "Ninguno"

Given el onboarding en el paso 3 (`STYLE`) sin ninguna preferencia marcada,
When se observa el editor,
Then "Ninguno" está **seleccionado** por defecto (se deriva de `softPreferenceIds` vacío); al marcar
una preferencia blanda "Ninguno" deja de estar seleccionado; al pulsar "Ninguno" se **vacían** todas
las preferencias. "Ninguno" y una preferencia concreta **nunca** están seleccionados a la vez.

### Scenario 3: el paso 3 alterna preferencias blandas (multi-selección)

Given el paso 3,
When se pulsa una preferencia blanda,
Then se añade; al volver a pulsarla se quita; nunca hay duplicados (semántica de conjunto); la lista
puede quedar vacía sin bloquear el avance.

### Scenario 4: el paso 4 alterna cocinas favoritas desde la Taxonomía

Given el onboarding en el paso 4 (`CUISINES`),
When se renderiza el editor de cocinas,
Then se ofrecen las 10 `cuisines` de la Taxonomía; pulsar una la añade y volver a pulsarla la quita;
se pueden elegir varias o ninguna.

### Scenario 5: los pasos 2–4 no bloquean el avance y conservan las elecciones al ir y volver

Given el onboarding con selecciones hechas en los pasos 2–4,
When se avanza y se retrocede con el armazón,
Then las selecciones siguen ahí (borrador en `OnboardingFlow` con `rememberSaveable`) y el primario
está habilitado en los tres pasos (la dieta tiene valor por defecto; estilo y cocinas son opcionales).

### Scenario 6: el borrador arranca con valores válidos y de la Taxonomía

Given la Taxonomía cargada,
When se crea el borrador inicial,
Then `dietPatternId == taxonomy.dietPatterns.first().id` (`omnivore`) y `softPreferenceIds` /
`cuisineIds` están vacías; ninguna opción procede de una lista propia distinta de la Taxonomía.

### Scenario 7: caso negativo — cada paso escribe solo su campo

Given un borrador con estilo y cocinas elegidos,
When se cambia el patrón de dieta,
Then las preferencias de estilo y las cocinas **no** cambian; y alternar una cocina no altera el
estilo (los tres campos son independientes).

### Scenario 8: caso negativo — "Ninguno" no deja residuo

Given el paso 3 con varias preferencias marcadas,
When se pulsa "Ninguno" y después se vuelve a marcar una preferencia,
Then la lista contiene **solo** esa preferencia (no arrastra las anteriores) y `isStyleNone` vuelve a
ser falso.

## Repository Research

### Files Inspected

- `feature_list.json` — definición de la feature (`area: ui`, `depends_on`, verificación manual) y
  nota "el paso de estilo permite 'Ninguno'; no duplicar pantallas".
- `PROGRESS.md` — `onboarding-shell-and-identity` está `accepted`; estado verificado y sesión 024.
- `CONTEXT.md` — vocabulario: `Patrón de dieta` (elección única), `Preferencia blanda` (multi-select,
  nunca descarta), `Taxonomía`, `Perfil de preferencias`, `Onboarding`.
- `docs/domain-model.md` — `PreferenceProfile` (`dietPatternId` uno, `softPreferenceIds[]`,
  `cuisineIds[]`); estado del onboarding y edge cases.
- `docs/build-brief.md` — onboarding obligatorio de 7 pasos; "una decisión por pantalla".
- `docs/domain/taxonomy.md` y `docs/domain/taxonomy.json` — las tres colecciones: `dietPatterns`
  (single-select, con `description`), `softPreferences` (multi-select, con `description`),
  `cuisines` (multi-select, con `icon`); regla "la UI nunca diverge de la Taxonomía".
- `DESIGN.md` — tokens (colores, tipografía, formas 16/24 y píldora, espaciado 12/16/24, 48dp táctil)
  y regla "una acción principal por pantalla".
- `docs/design/stitch-prompt.md` — sección A, pasos 2–4 (microcopy y affordances) y sección G, regla
  "Each row opens the SAME editor used in the corresponding onboarding step... Do not design separate
  duplicated editor screens".
- `docs/specs/onboarding-shell-and-identity.md` — contrato del armazón, pasos 1–7, decisión de no
  persistir el borrador y costura `onFinished` sin efecto.
- `docs/technical-discovery.md` — stack Compose/Material 3; la UI consume la Taxonomía; verificación
  JVM sin emulador; describe `SazonApp` y el armazón.
- Código: `ui/onboarding/{OnboardingFlow,OnboardingStep,OnboardingScaffold,OnboardingIdentityState,
  AvatarPreview}.kt`, `ui/{SazonApp,AppGate,HomePlaceholder}.kt`, `domain/taxonomy/Taxonomy.kt`,
  `domain/profile/UserProfile.kt` (`PreferenceProfile`), `ui/theme/{Spacing,Shape,Theme,Color}.kt`.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — sin dependencia de iconos; verificado que no
  se declara ningún artefacto `material-icons`.
- `docs/adr/0002-local-first-no-backend.md` — todo el estado en el dispositivo.

### Existing Patterns To Follow

- **Lógica pura sin Compose/Android** en `ui.onboarding` (como `OnboardingProgress`,
  `OnboardingIdentityState`), testeada en JVM con JUnit 4 y nombres de test en español. Solo las
  pantallas dependen de Android.
- **Un editor, dos entradas**: componentes **sin estado** que reciben valor y callbacks, sin conocer
  quién los usa. Se ubican fuera de `ui.onboarding` para que `profile-screen` los importe sin
  dependencia conceptual del onboarding.
- **Taxonomía como única fuente**: las listas de opciones se reciben de `Taxonomy`; nunca se duplican
  ni se hardcodean labels.
- **Tokens**: colores/espaciado/formas desde `com.sazon.app.ui.theme` (nunca hex sueltos); el patrón
  de selección es "píldora/tarjeta terracota" como en `OnboardingIdentityScreen`.
- **`rememberSaveable`** para el borrador del flujo (persistencia en la misma sesión/config change),
  como ya hace `OnboardingFlow` con nombre y color.
- **Casos negativos** en los tests que fallarían si la lógica no hiciera nada (patrón
  `OnboardingProgressTest`).
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- Los pasos 2–7 son `OnboardingPlaceholderStep` ("Este paso llega en una próxima versión."); no existe
  ningún editor real ni estado de preferencias.
- No existe un paquete de componentes de preferencias reutilizables.
- El repositorio **no** tiene Material Symbols ni dependencia de iconos (`AvatarPreview` ya documenta
  el hallazgo); las cocinas llevan `icon` en la Taxonomía pero no hay mapping a recurso local.

## Technical Approach

Estrategia: **lógica pura testeable en JVM + editores Compose delgados y sin estado**, sin
dependencias nuevas y sin persistencia.

### 1. Lógica pura (nueva) — `com.sazon.app.ui.onboarding.OnboardingPreferencesDraft`

Data class inmutable en Kotlin puro, sin Compose:

- Campos: `dietPatternId: String`, `softPreferenceIds: List<String>`, `cuisineIds: List<String>`.
- `companion object { fun initial(taxonomy: Taxonomy): OnboardingPreferencesDraft }` →
  `dietPatternId = taxonomy.dietPatterns.first().id` (la integridad de la Taxonomía garantiza ≥1) y
  listas vacías.
- `selectDietPattern(id): OnboardingPreferencesDraft` → single-select (`copy`).
- `toggleSoftPreference(id): OnboardingPreferencesDraft` y `toggleCuisine(id): ...` → alternan con
  **semántica de conjunto** (sin duplicados; si está, se quita).
- `clearSoftPreferences(): ...` → deja `softPreferenceIds` vacía (acción "Ninguno").
- `isStyleNone: Boolean` → `softPreferenceIds.isEmpty()`.
- No valida ids contra la Taxonomía: el editor solo ofrece ids válidos, y la validación del perfil no
  pertenece aquí (coherente con `profile-persistence`). La máquina de pasos (`OnboardingProgress`) no
  cambia.

### 2. Editores reutilizables (nuevos) — `com.sazon.app.ui.preferences`

Composables **sin estado**, con listas de la Taxonomía y callbacks; no conocen el onboarding ni el
Perfil (regla "un editor, dos entradas"):

- `DietPatternEditor(patterns: List<DietPattern>, selectedId: String, onSelect: (String) -> Unit)`:
  tarjetas seleccionables (esquinas 24dp) con `label` y `description`; seleccionada = borde/fondo
  `primary`. Corresponde al paso 2.
- `SoftPreferencesEditor(options: List<SoftPreference>, selectedIds: List<String>,
  onToggle: (String) -> Unit, onClear: () -> Unit)`: chips tipo píldora con `label` (helper
  "Opcional. Puedes cambiarlo cuando quieras.") y un chip **"Ninguno"** seleccionado cuando
  `selectedIds` está vacía; "Ninguno" invoca `onClear`. Corresponde al paso 3.
- `CuisinesEditor(cuisines: List<Cuisine>, selectedIds: List<String>, onToggle: (String) -> Unit)`:
  rejilla de 2 columnas de tiles seleccionables con `label`. Corresponde al paso 4.
- Los tres usan `SazonTheme`/`SazonSpacing`/`SazonShapes`/`SazonPillShape`; sin hex sueltos. **Sin
  iconos**: el repo no tiene assets ni dependencia de iconos y esta feature no añade ninguna.

### 3. Cableado en `OnboardingFlow`

- Añadir el borrador con `rememberSaveable` (Saver propio para la data class, en la capa Compose, de
  modo que la clase pura quede libre de Compose):
  `var draft by rememberSaveable(stateSaver = OnboardingPreferencesDraftSaver) { mutableStateOf(
  OnboardingPreferencesDraft.initial(taxonomy)) }`.
- Enrutar `DIET` → `DietPatternEditor`, `STYLE` → `SoftPreferencesEditor`, `CUISINES` →
  `CuisinesEditor`, pasando las listas de `taxonomy` y cerrando sobre `draft`.
- `INGREDIENTS`/`RESTRICTIONS`/`KITCHEN` permanecen en `OnboardingPlaceholderStep`.
- `primaryEnabled`: en el paso 1 se mantiene la regla por identidad; en los pasos 2–4 es `true`
  (dieta con valor por defecto; estilo y cocinas opcionales).
- `onFinished` sigue siendo `() -> Unit` y sin efecto: la feature que cierre el onboarding levantará
  el borrador (identidad + pasos 2–7) y persistirá el `UserProfile`. No se cambia ese contrato aquí.

### 4. Sin cambios de build

No se tocan `app/build.gradle.kts` ni `gradle/libs.versions.toml`: no se añaden dependencias. No
cambian `OnboardingStep`, `OnboardingScaffold`, `SazonApp`, `AppGate` ni `MainActivity`.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraft.kt` — crear; borrador puro
  y alternancia (single/multi-select, "Ninguno").
- `app/src/main/java/com/sazon/app/ui/preferences/DietPatternEditor.kt` — crear; editor reutilizable
  del paso 2.
- `app/src/main/java/com/sazon/app/ui/preferences/SoftPreferencesEditor.kt` — crear; editor
  reutilizable del paso 3 (chips + "Ninguno").
- `app/src/main/java/com/sazon/app/ui/preferences/CuisinesEditor.kt` — crear; editor reutilizable
  del paso 4.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingFlow.kt` — modificar; estado del borrador y
  enrutado de los pasos 2–4 a los editores.
- `app/src/test/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraftTest.kt` — crear;
  escenarios 1–8 (lógica pura).
- `docs/technical-discovery.md` — modificar; anotar el borrador de preferencias y los editores
  reutilizables.
- `docs/domain-model.md` — **no** cambia (implementa lo ya descrito).
- `feature_list.json` y `PROGRESS.md` — los actualiza el implementer al cerrar; no el planner.

No se modifican `app/build.gradle.kts` ni `gradle/libs.versions.toml`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` (sección A pasos 2–4; sección G para la
  futura reutilización en Perfil).
- Screens or states affected: onboarding pasos 2 (tarjetas seleccionables), 3 (chips + "Ninguno") y 4
  (rejilla de tiles); estados seleccionado/no seleccionado; estado opcional de estilo (vacío =
  "Ninguno").
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonShapes` (24dp tarjetas),
  `SazonPillShape` (chips), `primary`/`primaryContainer`/`onPrimary` para la selección, `outline` para
  los no seleccionados. Una sola acción principal (el primario del armazón) por pantalla.
- Reglas visuales: chips/tarjetas en píldora o 24dp; objetivos táctiles ≥48dp; tipografía dinámica
  (scroll, nunca recortar). Los chips de **alérgenos** (ámbar, nunca rojo) no son de esta feature.
- Iconos: no se renderizan (sin assets ni dependencia); los tiles de cocina muestran `label`. Queda
  como repaso visual de la persona y, si se quiere, feature correctiva.
- New design artifact required: **no**; el acabado puramente visual lo revisa la persona tras el
  merge.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — registrar el borrador en memoria de los pasos 2–4 y los
  editores reutilizables (`ui.preferences`) que Perfil reutilizará.
- `docs/domain-model.md`: **not needed** — describe ya `PreferenceProfile`, `Patrón de dieta` y
  `Preferencia blanda`.
- `docs/design/concepts/`: **not needed** — los mockups de Stitch siguen siendo dirección, no spec.

## Implementation Plan

1. Crear `OnboardingPreferencesDraft` (campos, `initial`, `selectDietPattern`, `toggleSoftPreference`,
   `toggleCuisine`, `clearSoftPreferences`, `isStyleNone`).
2. Crear los tres editores sin estado en `ui.preferences` con tokens del tema y sin iconos.
3. Cablear `OnboardingFlow`: borrador con `rememberSaveable` (Saver en la capa Compose) y enrutado de
   `DIET`/`STYLE`/`CUISINES` a los editores; dejar 5–7 como marcadores.
4. Escribir `OnboardingPreferencesDraftTest` (escenarios 1–8) y dejar el gate en verde.
5. Actualizar `docs/technical-discovery.md`; el implementer registra evidencia y estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `OnboardingPreferencesDraft` con `initial(taxonomy)` (primer `dietPatterns` por defecto).
- [ ] Añadir `selectDietPattern` (reemplazo), `toggleSoftPreference` y `toggleCuisine` (conjunto, sin
      duplicados).
- [ ] Añadir `clearSoftPreferences` e `isStyleNone` ("Ninguno" = lista vacía).
- [ ] Crear `DietPatternEditor` (tarjetas seleccionables con label + description).
- [ ] Crear `SoftPreferencesEditor` (chips + "Ninguno" + helper opcional).
- [ ] Crear `CuisinesEditor` (rejilla 2 columnas de tiles).
- [ ] Cablear `OnboardingFlow` con el borrador `rememberSaveable` y los pasos 2–4; mantener 5–7 como
      marcadores y `onFinished` sin efecto.
- [ ] Escribir `OnboardingPreferencesDraftTest` con los escenarios 1–8 y casos negativos.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: `OnboardingPreferencesDraftTest` en verde sin
  romper los 85 tests existentes:
  - Escenario 1/6: `initial(taxonomy).dietPatternId == taxonomy.dietPatterns.first().id`
    (`omnivore`), listas vacías; `selectDietPattern("vegan")` deja solo `vegan`.
  - Escenario 2/8: `isStyleNone` verdadero en el borrador inicial (estilo vacío); tras
    `toggleSoftPreference("keto")` es falso; `clearSoftPreferences()` vuelve a vaciar;
    `clearSoftPreferences().toggleSoftPreference(x)` contiene solo `x`.
  - Escenario 3/4: `toggleSoftPreference`/`toggleCuisine` añaden y quitan; alternar dos veces el mismo
    id deja la lista igual (sin duplicados).
  - Escenario 7: cambiar dieta no altera `softPreferenceIds`/`cuisineIds`; alternar cocina no altera el
    estilo.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Los pasos 2–4 muestran **exactamente** las opciones de la Taxonomía (5 dietas, 7 estilos + "Ninguno",
    10 cocinas).
  - Dieta: selección única; estilo: opcional con "Ninguno"; cocinas: varias o ninguna.
  - Avanzar y retroceder conserva las selecciones.
- **Desviación documentada** sobre la verificación de `feature_list.json` ("lo elegido persiste entre
  reinicios"): la persistencia del `UserProfile` es de `onboarding-ingredients-safety-kitchen`; en esta
  feature el borrador vive en memoria y sobrevive a cambios de configuración/proceso dentro de la
  sesión, no a un reinicio de app. El reinicio manual se comprobará en la feature que cierra el
  onboarding.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y el test del borrador corre dentro de `./gradlew test` sin emulador.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La lógica/estado (borrador, alternancia, "Ninguno") se cubre en JVM; el acabado
visual es manual.

## Evidence To Capture

- Salida de `./gradlew test` con `OnboardingPreferencesDraftTest`, nº de tests y 0 fallos, y el total
  de tests (esperado: 85 + los nuevos).
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `ui.preferences` reutilizable y sin estado; `OnboardingPreferencesDraft` sin imports de
  Compose/Android; sin cambios en `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Resultado manual en emulador de los pasos 2–4 (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y del estado `passing`.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Borrador en memoria con `rememberSaveable`**: persistir un `UserProfile` parcial está prohibido
  por `profile-persistence` ("no se inventa un perfil por defecto") y `onboarding-shell-and-identity`
  ya difirió el guardado a `onboarding-ingredients-safety-kitchen`. El borrador sobrevive a cambios de
  configuración/proceso, no a un reinicio de app.
- **Dieta con valor por defecto `omnivore` (primer `dietPatterns`)**: da un estado siempre válido y
  evita bloquear el paso 2 con una decisión extra; coincide con el patrón de `avatarColors.first()`.
  El primario está habilitado en los pasos 2–4.
- **"Ninguno" es la selección vacía de estilo**: no se añade un item a la Taxonomía; `isStyleNone` se
  deriva de `softPreferenceIds.isEmpty()` y por construcción "Ninguno" nunca coexiste con una
  preferencia.
- **Editores en `ui.preferences`, sin estado y con listas de la Taxonomía**: materializa la regla "un
  editor, dos entradas" para `profile-screen`; reciben `value` + callbacks y no conocen el onboarding.
- **Sin iconos y sin dependencias**: el repo no tiene Material Symbols ni `material-icons`; añadirlos
  no es imprescindible. Los tiles de cocina muestran su `label`. Si la persona quiere iconos, será una
  feature correctiva.
- **`onFinished` sin cambios**: la feature de cierre levantará el borrador completo (identidad + pasos
  2–7) y persistirá; cambiar ahora el contrato sería especulativo.

### Unknowns

- **Reutilización exacta en Perfil**: la firma de los editores (`value` + callbacks) es suficiente para
  esta feature; si `profile-screen` necesita un envoltorio en hoja superpuesta, lo añadirá sin cambiar
  los editores.
- **Persistencia entre reinicios**: fuera de alcance por diseño; la verifica la feature que cierra el
  onboarding. No bloquea esta feature.
- **Rendimiento de la carga del asset**: ya registrado en `onboarding-shell-and-identity`; sin cambios.

## Validator Checklist

- [ ] La implementación se queda en los pasos 2–4 (editores + borrador) sobre el armazón; los pasos
      5–7 siguen como marcadores y no se implementa el editor de alérgenos.
- [ ] Los escenarios 1–8 pasan; los tests JVM cubren single-select, multi-select, "Ninguno" y casos
      negativos que no pasan en vacío.
- [ ] Las opciones de los tres editores son **exactamente** las de `dietPatterns`, `softPreferences` y
      `cuisines` de la Taxonomía; no hay listas duplicadas ni labels hardcodeados.
- [ ] Ningún paso 2–4 persiste: sin Room/DataStore, sin `save`, sin recuperación de onboarding.
- [ ] Los editores viven en `ui.preferences`, están sin estado y no dependen del onboarding ni del
      Perfil (reutilizables por `profile-screen`).
- [ ] No hay hex sueltos; los colores/formas salen del tema; no se usan iconos ni se añade la
      dependencia de iconos.
- [ ] `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian (sin dependencias nuevas).
- [ ] La evidencia está en `feature_list.json`/`PROGRESS.md` y `docs/technical-discovery.md` queda
      coherente con el borrador y los editores reutilizables.
