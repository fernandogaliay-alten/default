# Feature Implementation Spec: Onboarding: armazón y paso de identidad

## Source Feature

- `id`: `onboarding-shell-and-identity`
- `area`: `ui`
- `depends_on`: `["profile-persistence", "taxonomy-in-code"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Abrir el **primer punto de entrada real de la app**: en el primer arranque debe aparecer un
onboarding **obligatorio y no saltable**, y el paso 1 debe capturar la identidad del Usuario local
(`displayName` obligatorio + `avatarColorId` de la Taxonomía) con **vista previa de iniciales**.

La feature entrega tres cosas: (1) un **armazón de onboarding reutilizable** (indicador de paso
1..7 sobre 7 pasos, botón "Atrás" y botón primario "Siguiente"/"Empezar" configurable) que los pasos
2–7 y la pantalla Perfil reutilizarán; (2) el **paso 1 (identidad)** real sobre ese armazón, con
nombre validado por `ProfileName`, selección de color entre los `avatarColors` de la Taxonomía y
avatares de iniciales; y (3) la **puerta de primer arranque** que decide entre onboarding y el
resto de la app según si existe Perfil de usuario.

El armazón **no** persiste el Perfil: los pasos 2–7 se dejan como marcadores de posición y el
guardado/cierre del onboarding pertenece a `onboarding-ingredients-safety-kitchen` (última feature
del onboarding). Esta feature entrega el andamio y la primera pantalla, no el flujo completo.

## Non-Goals

- **No** se implementan los pasos 2–7 (dieta, estilo, cocinas, ingredientes, alérgenos, "tu cocina")
  ni su editor; quedan como pantallas temporales marcadas como pendientes (`onboarding-placeholder`),
  sustituidas por `onboarding-diet-style-cuisines` y `onboarding-ingredients-safety-kitchen`.
- **No** se persiste el Perfil de usuario ni un borrador *en curso*: el paso 1 mantiene nombre y color
  **en memoria** (`rememberSaveable`). No se guarda un `UserProfile` con un `PreferenceProfile`
  inventado (lo prohíbe `profile-persistence`: "no se inventa un perfil por defecto").
- **No** se introduce el flag de onboarding en DataStore ni la máquina `not_started → in_progress →
  completed` con reanudación de paso: la puerta usa la **presencia de Perfil** en Room (ver
  "Decisions And Unknowns"). La reanudación de un onboarding interrumpido queda diferida.
- **No** se implementa la Home real (`home-offline-blocks`): tras un Perfil existente se muestra un
  marcador de posición mínimo ("Inicio"), solo para que la puerta tenga destino.
- **No** se edita el Perfil (`profile-screen`) ni se introduce la barra de navegación inferior ni el
  FAB: son features posteriores.
- **No** se añade una librería de navegación (`navigation-compose`) ni inyección de dependencias
  (Hilt/Koin); el cambio de destino es un estado mínimo (ver "Technical Approach").
- **No** se descargan assets ni fuentes nuevas; se usan los tokens de `design-tokens-and-theme` y los
  `avatarColors` ya empaquetados.
- **No** se copia identidad al `UserProfile` ni se valida que `avatarColorId` exista en la Taxonomía
  más allá de elegir de la propia lista.

## Job Story

Cuando abro Sazón por primera vez,
quiero decirle cómo me llaman y elegir el color de mi avatar sin poder saltarme ese paso,
para que la app me identifique por mi nombre y empiece a construir mi Perfil conmigo.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo; no hay cuentas, roles ni compartición (ADR 0002).
  No puede saltar el onboarding.
- **Desarrollador / agente**: construye el armazón y el paso 1 y valida la **lógica/estado en JVM**;
  el acabado puramente visual lo revisa la persona tras el merge (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: primer arranque muestra el onboarding y no se puede saltar

Given una base de datos sin Perfil de usuario,
When se abre la app,
Then se muestra el onboarding en el paso 1 y **no existe** ningún control de "saltar" ni forma de
llegar a la Home sin Perfil (la puerta es `initialDestination(profile == null) == ONBOARDING`).

### Scenario 2: el paso 1 exige nombre (verificación exigida)

Given el onboarding en el paso 1 con el campo vacío,
When el usuario no ha escrito nada,
Then el botón primario "Empezar" está **deshabilitado**; al escribir un nombre válido (1–40 tras
recortar) se **habilita**; con solo espacios o más de 40 caracteres vuelve a deshabilitarse.

### Scenario 3: progreso 1 de 7 y "Atrás" no disponible en el paso 1 (verificación exigida)

Given el onboarding en el paso 1,
When se observa la cabecera y la barra de acciones,
Then el progreso indica "Paso 1 de 7" y "Atrás" **no** está disponible; al avanzar al paso 2 el
progreso indica "Paso 2 de 7" y "Atrás" se habilita; "Atrás" desde el paso 2 vuelve al paso 1.

### Scenario 4: vista previa de iniciales sobre el color elegido

Given un nombre escrito y un color de avatar seleccionado de los 8 de la Taxonomía,
When se renderiza la vista previa,
Then se ven **como máximo 2 iniciales** en mayúsculas sobre ese color; con el nombre vacío se
muestra un **icono genérico de persona** sobre el color elegido, nunca iniciales vacías.

### Scenario 5: un Perfil existente no vuelve a mostrar el onboarding

Given un `UserProfile` ya persistido,
When se abre la app,
Then la puerta decide `MAIN` y se muestra el marcador de posición de Inicio, no el onboarding
(`initialDestination(profile != null) == MAIN`).

### Scenario 6: color por defecto ante ausencia de elección

Given el estado inicial del paso 1,
When aún no se ha tocado ningún color,
Then el color seleccionado es el **primero** de `avatarColors` de la Taxonomía
(`terracotta`, `#D9552B`), que actúa también como respaldo ante un `avatarColorId` ausente/corrupto.

## Repository Research

### Files Inspected

- `feature_list.json` — definición, `depends_on`, verificación manual exigida y nota de que este paso
  introduce el armazón compartido.
- `CONTEXT.md` / `docs/domain-model.md` — vocabulario (`Onboarding`, `UserProfile`, `Avatar de
  iniciales`), estados del onboarding, edge cases de nombre/iniciales/color ausente.
- `docs/build-brief.md` — onboarding obligatorio de 7 pasos y corte vertical del MVP.
- `DESIGN.md` — tokens (colores, Manrope/Inter, formas píldora, 52dp de botón), regla "una acción
  principal por pantalla", avatar de iniciales y baseline de accesibilidad (48dp, tipografía dinámica).
- `docs/design/stitch-prompt.md` — sección A (onboarding) y G (Perfil): microcopy exacto del paso 1,
  cabecera con puntos de progreso, "Atrás" arriba a la izquierda y primario anclado abajo.
- `docs/domain/taxonomy.md` y `docs/domain/taxonomy.json` — `avatarColors` (8 items, `id`/`label`/`hex`;
  el primero es `terracotta` `#D9552B`) y la regla "la UI nunca diverge de la Taxonomía".
- `docs/technical-discovery.md` — stack Compose/Material 3, DataStore como flag previsto, "el dominio
  no depende de Android", verificación por tests JVM.
- `docs/specs/profile-persistence.md` — `UserProfile`/`PreferenceProfile` no-null, `ProfileRepository`
  (`get`/`observe`/`save`/`clear`), `ProfileName` 1–40 con recorte; difiere el flag de onboarding.
- `docs/specs/design-tokens-and-theme.md` — tokens y tema ya disponibles; "la primera pantalla real es
  `onboarding-shell-and-identity`".
- `docs/adr/0002-local-first-no-backend.md` — todo el estado en el dispositivo.
- Código: `MainActivity.kt` (Surface vacía), `domain/profile/{UserProfile,ProfileName,ProfileRepository}.kt`,
  `data/profile/{ProfileDatabase,RoomProfileRepository,ProfileDao}.kt`, `domain/taxonomy/Taxonomy.kt`
  (`avatarColors`, `avatarColor(id)`), `data/AssetTaxonomySource.kt` (carga del asset), `ui/theme/**`
  (tokens), `app/build.gradle.kts` (sin dependencias de navegación/ViewModel).

### Existing Patterns To Follow

- Paquetes por capa; **dominio puro sin Android** y tests en JVM. La lógica del onboarding (máquina de
  pasos, validación del nombre, iniciales, puerta) se escribe en Kotlin puro para testearse sin
  emulador; solo las pantallas Compose dependen de Android.
- Comentarios y microcopy en **español**; identificadores en inglés; colores/tokens desde
  `com.sazon.app.ui.theme` (nunca hex sueltos en pantallas).
- Tests JUnit 4 en `app/src/test/...` con nombres en español y **casos negativos** que prueban que no
  pasan en vacío (patrón `TaxonomyIntegrityTest`/`DesignTokensTest`).
- La Taxonomía se lee del asset con `AssetTaxonomySource(context).load()` (patrón `taxonomy-in-code`).
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- No existe ningún punto de entrada: `MainActivity` pinta una `Surface` vacía y **no** monta la base de
  datos ni el repositorio (decisión explícita de `profile-persistence`).
- No hay navegación, ni estado de la app, ni destino posterior al onboarding.
- No existen pantallas ni componentes de UI (el tema sí).
- No hay una noción de "primer arranque" implementada; `ProfileRepository.observe()` es la única señal
  disponible y ya está aceptada.

## Technical Approach

Estrategia: **lógica pura testeable en JVM + capa Compose delgada**, con una costura de navegación
mínima (sin librería) y sin dependencias nuevas.

1. **Lógica pura (`com.sazon.app.ui.onboarding`, sin imports de Compose/Android):**
   - `OnboardingStep` (enum en orden 1..7, con `title` y `nextLabel`): `IDENTITY` "Empezar",
     `DIET`/`STYLE`/`CUISINES`/`INGREDIENTS`/`RESTRICTIONS` "Siguiente", `KITCHEN` "Empezar a
     cocinar". `TOTAL_STEPS = 7`.
   - `OnboardingProgress(totalSteps, stepIndex)`: `stepNumber`, `step`, `canGoBack = stepIndex > 0`,
     `isLastStep`, `next()`, `back()` (sin salirse de rango) y `progressLabel = "Paso N de 7"`.
   - `AvatarInitials.of(displayName): String?`: `trim`; si queda vacío → `null` (icono genérico); si
     hay varias palabras → primera letra de las **dos primeras**, en mayúsculas; una sola palabra →
     su primera letra. Máximo 2 caracteres; nunca devuelve iniciales vacías.
   - `OnboardingIdentityState(name, avatarColorId)`: `normalizedName` (vía `ProfileName`),
     `isNameValid`/`canProceed` (vía `ProfileName.isValid`), `initials` (vía `AvatarInitials`), y
     `initial(taxonomy)` que arranca con `avatarColorId = taxonomy.avatarColors.first().id`
     (respaldo = primer color; la integridad de la Taxonomía garantiza ≥1).
2. **Puerta de arranque (`com.sazon.app.ui`):** `enum AppDestination { ONBOARDING, MAIN }` y
   `AppGate.decide(profile: UserProfile?): AppDestination` → `null` = `ONBOARDING`, no-null = `MAIN`.
   Función pura, testeable en JVM.
3. **Capa Compose (`com.sazon.app.ui`):**
   - `SazonApp(profileRepository, taxonomySource)`: observa
     `profileRepository.observe().map { AppGate.decide(it) }` con `collectAsState(initial = null)`
     (`null` = "aún cargando", fondo del tema); al resolver, muestra `OnboardingFlow` o
     `HomePlaceholder`. Carga la Taxonomía una vez con `remember { taxonomySource.load() }` y la pasa
     al onboarding.
   - `OnboardingScaffold(progress, canGoBack, onBack, primaryLabel, primaryEnabled, onPrimary,
     content)`: armazón reutilizable — cabecera con etiqueta de progreso + puntos, "Atrás" arriba a la
     izquierda (oculto/deshabilitado si `!canGoBack`), contenido con scroll y botón primario anclado
     abajo (píldora, `SazonComponents.buttonHeight`). Una sola acción terracota por pantalla.
   - `OnboardingIdentityScreen(identity, taxonomy, onNameChange, onColorChange)`: titular/subtítulo de
     `stitch-prompt` ("Cocina con lo que ya tienes" / "Dinos qué te gusta y la IA hará el resto."),
     campo "¿Cómo te llamamos?" con helper "Así te saludaremos en Inicio.", fila de 8 muestras
     circulares con anillo terracota en la seleccionada y `AvatarPreview` en vivo.
   - `AvatarPreview(colorHex, initials)`: círculo del color elegido con las iniciales o
     `Icons.Default.Person` si no hay nombre (reutilizable después en Home/Perfil).
   - `OnboardingFlow(taxonomy, onFinished)`: mantiene `stepIndex`, `name` y `avatarColorId` con
     `rememberSaveable`; deriva `OnboardingProgress`/`OnboardingIdentityState`; enruta el paso actual
     (`IDENTITY` → pantalla real; 2–7 → `OnboardingPlaceholderStep(step)`); el primario avanza el paso
     (habilitado en el paso 1 solo si `identity.canProceed`) y en el último paso invoca `onFinished`.
   - `HomePlaceholder()`: marcador de posición mínimo ("Inicio") sustituido por `home-offline-blocks`.
4. **Punto de entrada:** `MainActivity` construye `ProfileDatabase.build(applicationContext)` +
   `RoomProfileRepository(db.profileDao())` + `AssetTaxonomySource(applicationContext)` y compone
   `SazonTheme { SazonApp(...) }`. El `onFinished` del onboarding es, en esta feature, una **costura sin
   efecto** (documentada): el guardado/cierre llega en la última feature del onboarding, de modo que la
   puerta sigue mostrando el onboarding hasta entonces.

**Contrato del armazón (para los pasos siguientes):** `OnboardingScaffold` no conoce el contenido ni
los datos; recibe progreso, acción de atrás, etiqueta/habilitación del primario y el contenido del
paso. Reutilizar el mismo armazón en los pasos 2–7 y en Perfil (regla "un editor, dos entradas").

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingStep.kt` — crear; enum 1..7 + etiquetas.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingProgress.kt` — crear; máquina de pasos pura.
- `app/src/main/java/com/sazon/app/ui/onboarding/AvatarInitials.kt` — crear; iniciales puras (máx. 2).
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingIdentityState.kt` — crear; estado del paso 1.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingScaffold.kt` — crear; armazón reutilizable.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingIdentityScreen.kt` — crear; paso 1.
- `app/src/main/java/com/sazon/app/ui/onboarding/AvatarPreview.kt` — crear; avatar de iniciales.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingFlow.kt` — crear; huésped de pasos + placeholders.
- `app/src/main/java/com/sazon/app/ui/AppGate.kt` — crear; `AppDestination` + `decide(profile)` pura.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — crear; raíz, puerta y costura de navegación.
- `app/src/main/java/com/sazon/app/ui/HomePlaceholder.kt` — crear; destino mínimo tras el Perfil.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar; montar BD/repositorio/asset y `SazonApp`.
- `app/src/test/java/com/sazon/app/ui/onboarding/OnboardingProgressTest.kt` — crear; escenarios 1 y 3 (lógica).
- `app/src/test/java/com/sazon/app/ui/onboarding/AvatarInitialsTest.kt` — crear; escenario 4 (iniciales).
- `app/src/test/java/com/sazon/app/ui/onboarding/OnboardingIdentityStateTest.kt` — crear; escenarios 2 y 6.
- `app/src/test/java/com/sazon/app/ui/AppGateTest.kt` — crear; escenarios 1 y 5.
- `docs/technical-discovery.md` — modificar; punto de entrada, puerta por presencia de Perfil y costura
  de navegación.
- `docs/domain-model.md` — **no** cambia (implementa lo ya descrito).

No se modifican `app/build.gradle.kts` ni `gradle/libs.versions.toml`: no se añaden dependencias.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` (sección A, paso 1, y sección 3).
- Screens or states affected: onboarding (armazón + paso 1), estado de campo vacío/válido, avatar sin
  nombre (icono) y con nombre (iniciales), paso 1 vs. pasos 2–7 (marcador).
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonPillShape` y
  `SazonComponents.buttonHeight` (52dp); fondo `background`, texto `onBackground`, primario terracota
  `primary`, píldora `primaryContainer` si se usa. El color del avatar sale del `hex` de la Taxonomía.
- Reglas visuales: una única acción principal por pantalla (terracota); "Atrás" como botón de texto;
  objetivos táctiles ≥48dp; tipografía dinámica (el texto hace scroll, nunca se recorta).
- New design artifact required: **no**. El repaso puramente visual (muestras, contraste, claro/oscuro)
  lo hace la persona tras el merge; un defecto visual se corrige con una feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — registrar el punto de entrada (`SazonApp`), que la puerta
  de primer arranque es la presencia de Perfil (y no un flag DataStore en esta feature) y la costura de
  navegación sin librería.
- `docs/domain-model.md`: **not needed** — describe ya `Onboarding`, `UserProfile` y el avatar.
- `docs/design/concepts/`: **not needed** — los mockups de Stitch siguen siendo dirección, no spec.

## Implementation Plan

1. Crear la lógica pura: `OnboardingStep`, `OnboardingProgress`, `AvatarInitials`,
   `OnboardingIdentityState` y `AppGate`.
2. Crear el armazón `OnboardingScaffold` y el `AvatarPreview` sobre los tokens del tema.
3. Crear `OnboardingIdentityScreen` (microcopy del `stitch-prompt`) y `OnboardingFlow` con placeholders
   para los pasos 2–7.
4. Crear `SazonApp` + `HomePlaceholder` y cablear `MainActivity` (BD, repositorio, asset) sin tocar el
   build.
5. Escribir los tests JVM (máquina de pasos, iniciales, identidad y puerta) y dejar el gate en verde.
6. Actualizar `docs/technical-discovery.md`; el implementer registra evidencia y estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [x] Crear `OnboardingStep` con 7 pasos en orden y sus `title`/`nextLabel`; `TOTAL_STEPS = 7`.
- [x] Crear `OnboardingProgress` puro (`stepNumber`, `canGoBack`, `next`, `back`, `progressLabel`).
- [x] Crear `AvatarInitials.of` (máx. 2 iniciales; `null` si el nombre queda vacío).
- [x] Crear `OnboardingIdentityState` con `initial(taxonomy)` (primer `avatarColors` por defecto).
- [x] Crear `AppDestination` + `AppGate.decide(profile)` puros.
- [x] Crear `AvatarPreview` (iniciales o icono genérico sobre el color de la Taxonomía).
- [x] Crear `OnboardingScaffold` (progreso, "Atrás", primario anclado, contenido con scroll).
- [x] Crear `OnboardingIdentityScreen` (campo obligatorio + helper + 8 muestras + vista previa).
- [x] Crear `OnboardingFlow` (estado con `rememberSaveable`, placeholders 2–7, costura `onFinished`).
- [x] Crear `SazonApp` + `HomePlaceholder` y cablear `MainActivity` (BD + repositorio + asset).
- [x] Escribir los cuatro tests JVM (pasos, iniciales, identidad, puerta) con casos negativos.
- [x] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: tests JVM de onboarding en verde sin romper los 67
  existentes:
  - `OnboardingProgressTest`: en el índice 0 → `stepNumber == 1`, `TOTAL_STEPS == 7`, `canGoBack` falso,
    `progressLabel == "Paso 1 de 7"`; `next()` → paso 2 con `canGoBack` verdadero; `back()` → paso 1;
    en el último paso `next()` no se sale de rango.
  - `AvatarInitialsTest`: `""`/`"   "` → `null`; `"Lucía"` → `"L"`; `"Lucía Gómez"` → `"LG"`;
    nombre con espacios sobrantes → iniciales correctas; nunca más de 2 caracteres.
  - `OnboardingIdentityStateTest`: `initial(taxonomy).avatarColorId == "terracotta"`; nombre vacío o
    solo espacios → `canProceed` falso; nombre válido → verdadero; 40 caracteres válido y 41 no;
    cambiar `avatarColorId` se refleja en el estado.
  - `AppGateTest`: `decide(null) == ONBOARDING`; `decide(perfil) == MAIN`.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores, sin warnings nuevos en ficheros de la
  feature.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Primer arranque sin Perfil → aparece el onboarding en el paso 1, sin control de saltar.
  - "Empezar" deshabilitado hasta escribir un nombre; se habilita con nombre válido.
  - Progreso "Paso 1 de 7" y "Atrás" no disponible; avanzar a 2 y volver con "Atrás".
  - Vista previa de iniciales sobre el color elegido; con nombre vacío, icono genérico.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y el gate del onboarding corre dentro de `./gradlew test` sin emulador.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La lógica/estado del onboarding (puerta, pasos, validación del nombre, iniciales) se
cubre en JVM, que es lo que AGENTS.md exige "cuando sea posible"; el acabado visual es manual.

## Evidence To Capture

- Salida de `./gradlew test` con los nombres `OnboardingProgressTest`, `AvatarInitialsTest`,
  `OnboardingIdentityStateTest` y `AppGateTest`, nº de tests y 0 fallos, y el total de tests.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Resultado manual en emulador de los escenarios 2–4 (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y del estado `passing`.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **La puerta de primer arranque es la presencia de Perfil**, no un flag en DataStore: el Perfil es el
  artefacto que produce el onboarding y `ProfileRepository.observe()` ya está aceptado. Evita una
  segunda fuente de verdad que pueda desincronizarse con la fila de Room y evita añadir dependencias.
  `docs/technical-discovery.md` menciona DataStore para el flag; esta feature lo **difiere** y deja
  constancia. Si más adelante se exige reanudar un onboarding interrumpido, se añadirá entonces.
- **Sin librería de navegación**: con solo dos destinos de nivel superior y un flujo lineal, un estado
  mínimo (`AppDestination` + índice de paso) es suficiente y no introduce un riesgo de versión de
  Compose. `navigation-compose` y la DI se pueden adoptar más tarde sin cambiar los contratos puros.
- **Borrador en memoria (`rememberSaveable`)**: guardar el nombre/color parcial no es posible sin un
  `PreferenceProfile` válido; persistir un perfil inventado lo prohíbe `profile-persistence`. El cierre
  y guardado del onboarding quedan en `onboarding-ingredients-safety-kitchen`.
- **Pasos 2–7 como marcadores declarados**: permiten ejercitar el armazón sin inventar contenido; el
  primario de un marcador avanza el paso, y el `onFinished` es una costura sin efecto en esta feature.
- **Edge cases de identidad cubiertos**: nombre vacío → CTA deshabilitado y avatar genérico; iniciales
  máx. 2; color ausente → primer `avatarColors` (`terracotta`). Son los que fija `docs/domain-model.md`.

### Unknowns

- **Rendimiento de carga del asset en composición**: `AssetTaxonomySource.load()` lee 22 KB en el hilo
  principal. Aceptable para esta feature; si Home/Perfil lo exigen, se moverá a un `ViewModel`/coroutine.
- **Reanudación de onboarding interrumpido**: fuera de alcance; requiere persistir el paso pendiente
  (no cubierto por `profile-persistence`). No bloquea la verificación de esta feature.

### Hallazgos del implementer

- **`AvatarPreview` sin `Icons.Default.Person`**: este repositorio no arrastra
  `androidx.compose.material:material-icons-core` (verificado con
  `./gradlew :app:dependencies --configuration debugRuntimeClasspath`: no aparece ningún artefacto
  `material-icons`), y añadir esa dependencia lo prohíbe esta spec ("no se añaden dependencias"). Por
  eso el icono genérico de persona (nombre vacío) se dibuja con primitivas de `Canvas` (cabeza +
  hombros) en lugar de `Icons.Default.Person`. El comportamiento exigido se mantiene: iniciales sobre
  el color elegido cuando hay nombre, icono genérico cuando no lo hay, nunca iniciales vacías.
- **`SazonApp` usa `produceState`** en vez de `collectAsState(initial = null)` para observar el
  Perfil: es equivalente (mientras no se resuelve el perfil se pinta solo el fondo del tema) y evita
  una conversión innecesaria; no cambia ningún contrato puro ni el destino.

## Validator Checklist

- [ ] La implementación se queda en onboarding (armazón + paso 1) y en la puerta de arranque; sin pasos
      2–7 reales, sin persistir Perfil, sin Home real, sin barra inferior/FAB ni edición de Perfil.
- [ ] Los escenarios 1–6 pasan; la lógica pura (pasos, iniciales, identidad, puerta) está cubierta en
      JVM y los tests incluyen casos negativos que no pasan en vacío.
- [ ] El paso 1 no permite avanzar con nombre vacío, solo espacios o más de 40 caracteres, y el armazón
      no ofrece "saltar".
- [ ] `canGoBack` es falso en el paso 1 y el progreso es "Paso 1 de 7"; "Atrás" vuelve desde el paso 2.
- [ ] Los colores salen de `SazonTheme`/`avatarColors`; no hay hex sueltos ni paleta de plantilla.
- [ ] `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian (sin dependencias nuevas).
- [ ] La evidencia de verificación está en `feature_list.json`/`PROGRESS.md` y `docs/technical-discovery.md`
      queda coherente (puerta por Perfil, navegación sin librería).
- [ ] No se ha añadido trabajo fuera de alcance (pasos reales 2–7, DataStore, Home, Perfil, navegación).
