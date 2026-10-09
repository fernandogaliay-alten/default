# Feature Implementation Spec: Perfil con identidad y preferencias editables

## Source Feature

- `id`: `profile-screen`
- `area`: `ui`
- `depends_on`: `["home-offline-blocks", "profile-persistence", "preference-signature"]` (las tres
  `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Convertir la pestaña **Perfil** (`MainTab.PROFILE`) en una pantalla real que sustituya al
`SectionPlaceholder("Perfil")`, donde el Usuario local **consulta y modifica** su **Perfil de usuario**
(nombre + color de avatar) y su **Perfil de preferencias** (patrón de dieta, restricciones duras,
preferencias de estilo, cocinas favoritas, ingredientes que me gustan/evito, dificultad, tiempo
disponible y comensales habituales).

La regla de diseño **"un editor, dos entradas"** es el eje de la feature: cada grupo de preferencias se
edita con el **mismo composable sin estado** que el onboarding ya usa (`ui.preferences`), presentado
como hoja inferior sobre el Perfil. No se crean pantallas ni editores nuevos de preferencias. Al
guardar, se persiste el `UserProfile` completo con `ProfileRepository.save`; como `SazonApp` ya observa
`ProfileRepository.observe()`, el armazón se recompone con el Perfil nuevo y `MainShell` ya sabe
reaccionar a un cambio de `preferences` (firma distinta ⇒ refresco del Lote) o a un cambio solo de
identidad (firma idéntica ⇒ el Lote **no** se invalida).

## Non-Goals

- **No** se implementa el **Resumen de actividad** (recetas generadas / favoritas): es la feature
  `activity-summary`. La sección "Resumen" **no** se pinta en esta feature.
- **No** se implementa **"Actualizar sugerencias"** ni la fecha de última actualización: es la feature
  `manual-suggestions-refresh`.
- **No** se implementan las secciones **"Ajustes"** (Notificaciones, Idioma, Sobre la app) ni
  **"Borrar mis datos"**: fuera del alcance de la feature y sin contrato de verificación; el botón
  destructivo queda diferido (aunque `ProfileRepository.clear()` exista).
- **No** se crean ni modifican editores de preferencias: se **reutilizan** los de `ui.preferences`
  (`DietPatternEditor`, `SoftPreferencesEditor`, `CuisinesEditor`, `IngredientsEditor`,
  `HardRestrictionsEditor`, `DifficultyEditor`, `TimeBucketEditor`, `ServingsStepper`). No se duplica
  ninguna pantalla del onboarding.
- **No** se cambia el modelo de dominio, la persistencia Room, la Firma de preferencias ni la
  Taxonomía: esta feature es **UI + un estado de edición puro**.
- **No** se toca `ProfileDatabase`, `data.profile/**`, `domain.profile/**` (más allá de consumirlos),
  `domain.suggestions`/`data.suggestions`, `favorites`, `Home`, `Generator` ni el onboarding (salvo la
  adición aditiva de dos factorías al borrador de preferencias, ver "Technical Approach").
- **No** se introduce librería de navegación ni DI: el Perfil es una pestaña de `MainShell`.
- **No** se añaden dependencias: `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian.
- **No** se inventan opciones fuera de la Taxonomía ni se hardcodean etiquetas.

## Job Story

Cuando reviso mi cuenta en Sazón,
quiero ver y corregir mi nombre, mi avatar y todas mis preferencias en un mismo sitio, usando los
mismos editores del onboarding,
para mantener mis datos al día y que las sugerencias se ajusten a mí sin rehacer el onboarding.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); puede ver y editar su Perfil. No hay
  cuentas, roles ni compartición.
- **Desarrollador / agente**: implementa la pantalla y el estado de edición y verifica la
  **lógica/estado en JVM** (identidad vs. preferencias, firma, reutilización de editores); el acabado
  puramente visual (hojas inferiores, chips, claro/oscuro) lo revisa la persona tras el merge
  (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: el Perfil muestra la identidad y las ocho muestras de color de la Taxonomía

Given un `UserProfile` "Lucía" con `avatarColorId = "terracotta"` y la Taxonomía cargada,
When se abre la pestaña Perfil,
Then la cabecera muestra el avatar (iniciales sobre el `hex` de la Taxonomía), el nombre y la
etiqueta "Tu perfil"; en modo edición aparecen el campo "¿Cómo te llamamos?", su helper "Así te
saludaremos en Inicio." y las **8** muestras de `avatarColors`, con la seleccionada anillada en
terracota.

### Scenario 2: editar el nombre lo persiste normalizado y exige un nombre válido

Given el modo edición de identidad con el nombre "Lucía" y el campo vacío,
When se escribe "  Ana  " y se pulsa "Guardar",
Then se persiste un `UserProfile` con `displayName == "Ana"` (recortado), el mismo `avatarColorId`,
el mismo `createdAt` y el **mismo `PreferenceProfile`**; con el campo vacío, solo espacios o >40
caracteres tras recortar, "Guardar" está **deshabilitado** y `toProfile` rechaza la operación con
`IllegalArgumentException` (no se escribe).

### Scenario 3: editar el color de avatar lo persiste sin tocar las preferencias

Given el modo edición de identidad,
When se elige otra muestra de `avatarColors` (p. ej. `olive`) y se guarda,
Then el `UserProfile` persistido cambia `avatarColorId` a `olive` y conserva exactamente el mismo
`PreferenceProfile` (incluido `updatedAt`).

### Scenario 4: cambiar la identidad NO invalida el Lote de sugerencias (verificación exigida)

Given un `UserProfile` con sus preferencias y su firma,
When se edita **solo** el nombre o **solo** el color de avatar y se guarda,
Then el `PreferenceProfile` resultante es **igual** al original, de modo que
`PreferenceSignature.of(nuevo.preferences) == PreferenceSignature.of(original.preferences)` y el
`LaunchedEffect(profile.preferences)` de `MainShell` **no** vuelve a lanzar `ensureBatch` (el Lote
sigue `fresh` y no se gasta una llamada de IA).

### Scenario 5: cada grupo de preferencias abre el MISMO editor del onboarding

Given el Perfil en la sección "Mis preferencias",
When se pulsa una fila (Alimentación, Estilo, Cocinas favoritas, Ingredientes que me gustan, Ingredientes
que evito) o una fila de "Alergias e intolerancias" o de "Mi cocina",
Then se abre una **hoja inferior** con el composable compartido correspondiente de `ui.preferences`
(`DietPatternEditor`, `SoftPreferencesEditor`, `CuisinesEditor`, `IngredientsEditor`,
`HardRestrictionsEditor`, `DifficultyEditor`, `TimeBucketEditor`, `ServingsStepper`), con los valores
actuales del Perfil; no existe ninguna pantalla/editor duplicado del onboarding.

### Scenario 6: editar una preferencia se refleja igual que en el onboarding (verificación exigida)

Given un Perfil con `cuisineIds = ["mediterranean"]` y su borrador derivado,
When se abre "Cocinas favoritas", se alterna `italian` y se confirma la hoja,
Then se persiste un `UserProfile` cuyo `PreferenceProfile.cuisineIds == ["mediterranean", "italian"]`
(el mismo resultado que produciría `OnboardingPreferencesDraft.toggleCuisine("italian")`), con el
resto de campos intactos y `updatedAt` avanzado.

### Scenario 7: un cambio de preferencias sí invalida el Lote

Given un Perfil con su firma y un Lote persistido con esa firma,
When se edita cualquier campo de preferencias (dieta, restricción, estilo, cocina, ingrediente,
dificultad, tiempo o comensales) y se guarda,
Then el `PreferenceProfile` resultante cambia ese campo y
`PreferenceSignature.of(nuevo.preferences) != PreferenceSignature.of(original.preferences)`, de modo
que `MainShell` vuelve a llamar a `ensureBatch` y la política cache-first ya aceptada marca el Lote
`SIGNATURE_CHANGED`.

### Scenario 8: los editores conservan las reglas de selección del onboarding

Given una hoja de edición abierta,
When se opera sobre ella,
Then se mantienen las reglas ya aceptadas: dieta/dificultad/tiempo son **single-select**; estilo y
cocinas tienen semántica de **conjunto**; "Ninguno" deja el estilo vacío; "Ninguna" deja las
restricciones vacías; elegir un ingrediente en "Me gusta" lo quita de "Evito" y viceversa (intersección
vacía); y los comensales se recortan a `taxonomy.servings.min..max`.

### Scenario 9: caso negativo — identidad y preferencias son independientes

Given un borrador de Perfil con una preferencia cambiada y la identidad intacta,
When se construye el `UserProfile` (o al revés),
Then cada operación escribe **solo** su parte: cambiar la identidad no altera ningún campo de
preferencias y cambiar una preferencia no altera `displayName`/`avatarColorId`/`createdAt`.

### Scenario 10: caso negativo — color de avatar corrupto se degrada

Given un `UserProfile` cuyo `avatarColorId` no existe en la Taxonomía (perfil migrado/corrupto),
When se abre el Perfil,
Then el estado de edición resuelve el color al **primer** `avatarColors` de la Taxonomía (respaldo ya
descrito en `docs/domain-model.md`), sin romper y sin inventar ids.

## Repository Research

### Files Inspected

- `feature_list.json` — definición de `profile-screen` (`area: ui`, `depends_on`, verificación manual
  y nota "un editor, dos entradas"); features vecinas `activity-summary` y `manual-suggestions-refresh`,
  que se apoyan en esta y explican por qué su contenido queda fuera.
- `PROGRESS.md` — `home-offline-blocks` `accepted` (sesión 033) y estado verificado; `MainTab.PROFILE`
  sigue siendo `SectionPlaceholder("Perfil")`.
- `AGENTS.md` — verificación de UI por lógica/estado en JVM; lo puramente visual y las pruebas de
  emulador las revisa la persona; una feature por sesión.
- `CONTEXT.md` — "Pantalla Perfil", "Perfil de usuario", "Perfil de preferencias", "Resumen de
  actividad", "Firma de preferencias", "Avatar de iniciales", "Ingredientes que evito".
- `docs/domain-model.md` — `UserProfile`/`PreferenceProfile`, scenarios 4 (cambio de preferencias) y 7
  (cambio de identidad), y edge cases (color ausente, ingrediente en ambos grupos).
- `docs/technical-discovery.md` — `ui.home`/`MainShell` (barra inferior + FAB, `MainTab.PROFILE` como
  placeholder), la composición del Lote y la observación del Perfil en `SazonApp`.
- `DESIGN.md` — sección "Core Screens" 7 (Perfil: identidad, preferencias editables, comensales,
  tarjeta ámbar de alérgenos) y tokens (terracota = acción principal, ámbar = alérgenos, formas
  16/24/píldora, cifras tabulares).
- `docs/design/stitch-prompt.md` — sección G (Perfil): cabecera con avatar/nombre/"Editar", secciones
  "Mis preferencias", "Alergias e intolerancias" (tarjeta ámbar) y "Mi cocina" (nivel, tiempo,
  comensales); regla "Each row opens the SAME editor used in the corresponding onboarding step...
  Do not design separate duplicated editor screens".
- `docs/specs/profile-persistence.md` — `UserProfile`/`PreferenceProfile` no-null, `ProfileName` 1–40
  con recorte, `ProfileRepository` (`get`/`observe`/`save`/`clear`) y `save` que rechaza nombre inválido.
- `docs/specs/preference-signature.md` — la firma incluye las 9 preferencias y **excluye** identidad y
  `updatedAt`; base del Scenario 4 y del Scenario 7.
- `docs/specs/onboarding-diet-style-cuisines.md` y `docs/specs/onboarding-ingredients-safety-kitchen.md`
  — contrato y semántica de `OnboardingPreferencesDraft` y de los editores sin estado en
  `ui.preferences`; la regla "un editor, dos entradas" se declara ahí como preparación explícita de
  esta feature.
- `docs/specs/home-offline-blocks.md` — `MainShell` (`MainTab`, `PushedScreen`, `MainNavigationState`,
  `LaunchedEffect(profile.preferences) { ensureBatch }`), `SazonApp` observando el `UserProfile`, y la
  costura `savedRecipes`/`recentlyOpenedRecipes`.
- Código: `ui/MainShell.kt` (enruta `MainTab.PROFILE` a `SectionPlaceholder("Perfil")`; ya reacciona a
  `profile.preferences`), `ui/SazonApp.kt` (observa el Perfil y persiste con `ProfileRepository.save`
  en el cierre del onboarding), `ui/navigation/MainNavigationState.kt`,
  `ui/onboarding/{OnboardingPreferencesDraft,AvatarPreview,AvatarInitials}.kt`,
  `ui/preferences/{DietPatternEditor,SoftPreferencesEditor,CuisinesEditor,IngredientsEditor,
  HardRestrictionsEditor,DifficultyEditor,TimeBucketEditor,ServingsStepper,IngredientsFilter}.kt`,
  `ui/home/{HomeScreen,HomeIdentity,HomeGreeting}.kt`, `ui/theme/{Color,Spacing,Shape,Type}.kt`,
  `domain/profile/{UserProfile,ProfileName,ProfileRepository,PreferenceSignature}.kt`,
  `domain/taxonomy/Taxonomy.kt`, `MainActivity.kt`.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — Material 3 (incluye `ModalBottomSheet`),
  Compose, coroutines y Room ya disponibles; sin `material-icons` ni `navigation-compose`.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: la edición se modela en Kotlin puro sin imports de
  Compose/Android (`ProfileEditState`) y se testea en JVM; los `@Composable` solo pintan.
- **Un editor, dos entradas**: componentes sin estado (`value` + callbacks) en `ui.preferences`,
  reutilizados aquí **sin cambios**; las hojas solo los envuelven.
- **Borrador de preferencias compartido**: `OnboardingPreferencesDraft` ya materializa la semántica de
  selección (single/multi, "Ninguno"/"Ninguna", exclusión mutua, recorte de comensales). Se reutiliza
  para no reimplementar la lógica ni arriesgar divergencia.
- **Taxonomía como única fuente**: las opciones salen de `Taxonomy`; nunca se duplican listas ni
  labels.
- **Tokens**: colores/espaciado/formas desde `com.sazon.app.ui.theme` (nunca hex sueltos); terracota =
  `primary`/`primaryContainer`, oliva = `secondary`/`secondaryContainer`, ámbar =
  `tertiary`/`tertiaryContainer`; el rol `error` no se usa para alérgenos.
- **Persistencia por observación**: se escribe el Perfil con `ProfileRepository.save` y el armazón se
  recompone por `observe()`; no se navega ni se muta el estado global a mano.
- **`rememberSaveable` con `Saver`** en la capa Compose para el estado de edición, siguiendo
  `OnboardingFlow`/`HomeScreen`.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, con casos negativos que no
  pasan en vacío; la Taxonomía real se lee por `taxonomy.asset.path`.
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- `MainTab.PROFILE` pinta `SectionPlaceholder("Perfil")`: no hay pantalla, ni cabecera de identidad, ni
  filas de preferencias, ni hojas de edición.
- No existe ningún estado de edición del Perfil: el onboarding construye un `UserProfile` de cero
  (`OnboardingProfileBuilder`) y no hay forma de derivar un estado editable **desde** un Perfil
  existente ni de reconstruir un `UserProfile` editado.
- `OnboardingPreferencesDraft` solo se crea con `initial(taxonomy)` (valores por defecto); no sabe
  partir de un `PreferenceProfile` existente ni volver a uno.
- `MainShell` no expone ninguna costura para escribir el Perfil editado.

## Technical Approach

Estrategia: **estado de edición puro testeable en JVM + pantalla Compose delgada que reutiliza los
editores existentes**, sin dependencias nuevas.

### 1. Borrador de preferencias compartido (aditivo) — `ui/onboarding/OnboardingPreferencesDraft`

Se amplía la data class pura ya aceptada con dos operaciones **aditivas** (no cambian el
comportamiento existente ni sus tests):

- `companion object { fun from(preferences: PreferenceProfile): OnboardingPreferencesDraft }` — copia
  los nueve campos de preferencia (sin `updatedAt`, que no es una preferencia).
- `fun toPreferences(updatedAt: Long): PreferenceProfile` — reconstruye un `PreferenceProfile` con los
  nueve campos y el `updatedAt` dado.

Así el **único** motor de selección de preferencias (single/multi-select, "Ninguno"/"Ninguna",
exclusión mutua, recorte de comensales) es el ya aceptado: la entrada del Perfil y la del onboarding
no pueden divergir. Es el mismo criterio aditivo ya usado al ampliar `RecipeJson` en
`suggestion-batch-cache`.

### 2. Estado de edición puro — `com.sazon.app.ui.profile.ProfileEditState`

Data class inmutable, sin imports de Compose/Android:

- Campos: `displayName: String`, `avatarColorId: String`,
  `preferencesDraft: OnboardingPreferencesDraft`.
- `companion object { fun from(profile: UserProfile, taxonomy: Taxonomy): ProfileEditState }`:
  normaliza el nombre con `ProfileName.normalize`; resuelve `avatarColorId` con
  `taxonomy.avatarColor(id)?.id ?: taxonomy.avatarColors.first().id` (respaldo del edge case); y
  `preferencesDraft = OnboardingPreferencesDraft.from(profile.preferences)`.
- `withDisplayName(name)` / `withAvatarColorId(id)` / `withPreferencesDraft(draft)` — copias inmutables.
- `isNameValid: Boolean` = `ProfileName.isValid(displayName)`.
- `fun toProfile(base: UserProfile, now: Long): UserProfile`: exige `isNameValid`
  (`IllegalArgumentException` si no, mismo contrato que `ProfileRepository.save`); normaliza el nombre;
  conserva `createdAt = base.createdAt`; y decide `updatedAt`:
  - si `preferencesDraft == OnboardingPreferencesDraft.from(base.preferences)` (identidad sola o sin
    cambios), reutiliza `base.preferences` **tal cual** (mismo `updatedAt`), de modo que el
    `PreferenceProfile` es **igual** y no se invalida el Lote (Scenario 4);
  - si cambió alguna preferencia, `base.preferences` se reconstruye con `updatedAt = now`
    (`preferencesDraft.toPreferences(now)`), lo que cambia la firma (Scenario 7).

No valida ids contra la Taxonomía: las hojas solo ofrecen ids válidos (coherente con
`profile-persistence`). La igualdad de `PreferenceProfile` (data class) es la que decide si hay cambio
de preferencias.

### 3. Pantalla Compose — `com.sazon.app.ui.profile`

- `ProfileScreen(profile: UserProfile, taxonomy: Taxonomy, onSaveProfile: (UserProfile) -> Unit)`:
  - `LazyColumn` con la cabecera de identidad, "Mis preferencias", "Alergias e intolerancias" y "Mi
    cocina".
  - Estado `ProfileEditState` con `rememberSaveable(stateSaver = ProfileEditStateSaver)` (el `Saver`
    vive en la capa Compose y serializa los campos escalares + las listas del borrador) y
    `LaunchedEffect(profile)` que lo resincroniza cuando el Perfil persistido cambia (p. ej. tras un
    guardado).
  - `editingIdentity: Boolean` y `activeSection: ProfileSection?` son **mutuamente excluyentes**
    (abrir uno cierra el otro) para no mezclar ediciones no guardadas.
  - Guardado: cada operación construye `updated = state.toProfile(profile, System.currentTimeMillis())`
    y, si `updated != profile`, llama `onSaveProfile(updated)`. Identidad: botón "Guardar"
    deshabilitado con `!isNameValid`. Preferencias: la hoja persiste al cerrarse ("Listo" o descarte) si
    hubo cambio.
- `ProfileIdentityEditor(state, taxonomy, onNameChange, onColorChange, onSave, isEditing)`:
  cabecera con `AvatarPreview` (reutilizado) y `AvatarInitials`/nombre; en modo edición, campo +
  helper + fila de 8 muestras de `avatarColors` (anillo terracota en la seleccionada) y "Guardar".
- `ProfileEditorSheet(section, state, taxonomy, onDraftChange, onDismiss)`: `ModalBottomSheet`
  (`@OptIn(ExperimentalMaterial3Api::class)` si el BOM lo exige) que envuelve el editor compartido de
  la sección con su título y un primario "Listo"; mantiene la `query` del `IngredientsEditor` con
  `rememberSaveable`.
- `ProfileSection` (enum puro): `DIET`, `STYLE`, `CUISINES`, `INGREDIENTS`, `RESTRICTIONS`,
  `DIFFICULTY`, `TIME_BUCKET`, `SERVINGS`, cada uno con su título de fila.
- Filas de resumen (`ProfileSectionRow`): etiqueta del grupo, resumen de los valores actuales (labels
  resueltos con la Taxonomía, con respaldo al `id`) y chevron; las filas de "Mis preferencias",
  "Alergias e intolerancias" y "Mi cocina" abren su `ProfileSection`.
- Las opciones salen de `taxonomy.dietPatterns`, `softPreferences`, `cuisines`, `ingredients`,
  `hardRestrictions`, `difficulties`, `timeBuckets`, `servings` y `avatarColors`.

### 4. Integración en el armazón

- `MainShell`: añadir el parámetro `onSaveProfile: (UserProfile) -> Unit` y sustituir
  `MainTab.PROFILE -> SectionPlaceholder(title = "Perfil")` por
  `MainTab.PROFILE -> ProfileScreen(profile = profile, taxonomy = taxonomy,
  onSaveProfile = onSaveProfile)`. Nada más cambia: el Lote, las pestañas y el FAB quedan igual.
- `SazonApp`: crear el `rememberCoroutineScope()` en la rama `MAIN` (o reutilizar uno de la raíz) y
  pasar `onSaveProfile = { updated -> scope.launch { profileRepository.save(updated) } }` a
  `MainShell`. La observación existente hace que el Perfil editado vuelva a `MainShell`; cambiar
  `preferences` reinicia el `LaunchedEffect(profile.preferences)` (refresco del Lote) y cambiar solo la
  identidad no.

### 5. Sin cambios de build

No se tocan `app/build.gradle.kts` ni `gradle/libs.versions.toml`: no se añaden dependencias
(`ModalBottomSheet` viene de Material 3, ya presente).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/profile/ProfileEditState.kt` — estado de edición puro
  (identidad + borrador) y `toProfile`.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — pantalla del Perfil, secciones,
  estado y `Saver`.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileIdentityEditor.kt` — cabecera/edición de identidad.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileEditorSheet.kt` — hoja inferior y `ProfileSection`.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileSectionRow.kt` — fila de resumen con chevron.
- `app/src/test/java/com/sazon/app/ui/profile/ProfileEditStateTest.kt` — escenarios de lógica (JVM).

**Modificar**

- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraft.kt` — añadir
  `from(preferences)` y `toPreferences(updatedAt)` (aditivo).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — parámetro `onSaveProfile` y enrutado de
  `MainTab.PROFILE` a `ProfileScreen`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — pasar `onSaveProfile` (coroutine +
  `ProfileRepository.save`).
- `docs/technical-discovery.md` — registrar `ui.profile` y el guardado por observación.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `ProfileDatabase`,
`data.profile/**`, `domain.profile/**` (salvo consumir `PreferenceSignature` en tests),
`domain.suggestions`/`data.suggestions`, la Taxonomía, `seed-recipes.json`, `favorites`, `Home`,
`Generator` ni las pantallas del onboarding.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (Core Screens 7 y tokens) + `docs/design/stitch-prompt.md` sección G.
- Screens or states affected: pestaña Perfil (cabecera de identidad, "Mis preferencias", "Alergias e
  intolerancias", "Mi cocina"), modo edición de identidad, hojas inferiores de edición, filas de
  resumen y estado de color corrupto.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonShapes` (24dp), `SazonPillShape`
  (chips), `primary`/`primaryContainer` (acción principal y selección), `secondary`/`secondaryContainer`
  ("Evito"), `tertiary`/`tertiaryContainer` (tarjeta y chips de alérgenos). El color del avatar sale del
  `hex` de `avatarColors`.
- Reglas visuales: una única acción principal terracota por pantalla/hoja; objetivos táctiles ≥48dp;
  cifras tabulares en comensales; tipografía dinámica (scroll, nunca recortar); alérgenos en **ámbar,
  nunca rojo**; no depender solo del color (etiquetas y chevron).
- Se reutilizan `AvatarPreview` y los editores de `ui.preferences`; no se renderizan imágenes ni iconos
  externos (sin dependencia de iconos).
- Capturas de referencia (backfill PR #37): `docs/design/concepts/profile-screen/` — pantallas `perfil`
  y `perfil-editar`. Pendiente de exportar desde Stitch (petición en el `README.md` de la carpeta); el
  acabado puramente visual lo revisa la persona tras el merge y un defecto se corrige con feature
  correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo; "un
  editor, dos entradas" ya vive en `docs/design/stitch-prompt.md`/`DESIGN.md` y en `CONTEXT.md`.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — registrar `ui.profile` (`ProfileEditState` +
  `ProfileScreen`), la reutilización de los editores de `ui.preferences` vía hoja inferior, y que el
  guardado usa `ProfileRepository.save` por observación (identidad no invalida el Lote; preferencias
  sí).
- `docs/domain-model.md`: **not needed** — ya describe el Perfil editable, la Firma y los edge cases.
- `DESIGN.md` y `docs/design/concepts/`: **update** — se añade el backfill de capturas `perfil` y
  `perfil-editar` en `docs/design/concepts/profile-screen/`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión de planificación.

## Implementation Plan

1. Añadir a `OnboardingPreferencesDraft` las factorías aditivas `from(preferences)` y
   `toPreferences(updatedAt)`.
2. Crear `ProfileEditState` (identidad + borrador, `isNameValid`, `toProfile`, `from`) con su test JVM.
3. Crear la capa Compose del Perfil: `ProfileIdentityEditor`, `ProfileSectionRow`,
   `ProfileEditorSheet` y `ProfileScreen`, reutilizando `AvatarPreview` y los editores de
   `ui.preferences`.
4. Cablear `MainShell` (`onSaveProfile` + `MainTab.PROFILE -> ProfileScreen`) y `SazonApp`
   (`rememberCoroutineScope` + `ProfileRepository.save`).
5. Ejecutar `init.sh` (test + lint) en verde y corregir lo que salga.
6. Actualizar `docs/technical-discovery.md`; el implementer registra la evidencia y el estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Añadir `OnboardingPreferencesDraft.from(preferences)` y
      `toPreferences(updatedAt)` (aditivo, sin cambiar el comportamiento existente).
- [ ] Crear `ProfileEditState` con `from(profile, taxonomy)`, `withDisplayName`,
      `withAvatarColorId`, `withPreferencesDraft`, `isNameValid` y `toProfile(base, now)`.
- [ ] Escribir `ProfileEditStateTest` (escenarios 2–4, 9 y 10; semántica de preferencias del 8) con
      casos negativos.
- [ ] Crear `ProfileIdentityEditor` (campo + helper + 8 muestras + `AvatarPreview` + "Guardar").
- [ ] Crear `ProfileSectionRow` (resumen + chevron) y `ProfileSection` (enum).
- [ ] Crear `ProfileEditorSheet` envolviendo los editores de `ui.preferences` (dieta, estilo,
      cocinas, ingredientes, restricciones, dificultad, tiempo, comensales).
- [ ] Crear `ProfileScreen` (secciones, estado con `rememberSaveable` + `Saver`, sincronización con
      `profile`, guardado por `onSaveProfile`).
- [ ] Cablear `MainShell` (`onSaveProfile`, enrutado de `MainTab.PROFILE`) y `SazonApp` (guardar con
      `ProfileRepository.save`).
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: `ProfileEditStateTest` en verde sin romper los 189
  tests existentes:
  - `from(profile, taxonomy)`: conserva `displayName` normalizado y los nueve campos del borrador;
    con `avatarColorId` inexistente resuelve el primer `avatarColors`.
  - Identidad: `withDisplayName("  Ana  ").toProfile(base, now)` → `displayName == "Ana"`, mismo
    `avatarColorId`, `createdAt == base.createdAt` y `preferences == base.preferences` (incluido
    `updatedAt`); `withAvatarColorId("olive")` idem; ambos ⇒
    `PreferenceSignature.of(nuevo.preferences) == PreferenceSignature.of(base.preferences)`
    (Scenario 4, caso negativo de que no cambia en vacío: un cambio de preferencia sí la cambia).
  - Preferencias: para cada campo representativo (`dietPatternId`, `hardRestrictionIds`,
    `softPreferenceIds`, `cuisineIds`, `preferredIngredientIds`, `dislikedIngredientIds`,
    `difficultyId`, `timeBucketId`, `defaultServings`), editar produce un `PreferenceProfile` con ese
    campo cambiado, `updatedAt == now` y firma distinta; y el resultado es **igual** al que produce
    `OnboardingPreferencesDraft` para la misma operación (misma semántica que el onboarding).
  - Semántica del Scenario 8: "Ninguno"/"Ninguna" dejan las listas vacías; exclusión mutua de
    ingredientes; `setServings` recorta al rango; single-select reemplaza.
  - Negativos: nombre vacío/solo espacios/>40 ⇒ `isNameValid == false` y `toProfile` lanza
    `IllegalArgumentException`; identidad y preferencias son independientes (Scenario 9).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: los editores de `ui.preferences` **no** cambian; `ProfileEditState` sin imports de
  Compose/Android; `MainTab.FAVORITES` y `Home` intactos; `app/build.gradle.kts` y
  `gradle/libs.versions.toml` sin cambios.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Editar una preferencia desde el Perfil se refleja igual que si se hubiera elegido en el onboarding
    (verificación exigida).
  - Cambiar el nombre o el color de avatar **no** invalida el Lote de sugerencias (verificación
    exigida): la Home sigue con las mismas sugerencias.
  - Los alérgenos se ven en ámbar y nunca en rojo; las hojas abren los mismos editores del onboarding.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y el test del estado de edición corre dentro de `./gradlew test` sin
emulador.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La lógica verificable (identidad vs. preferencias, firma, reutilización del borrador)
se cubre en JVM; el acabado visual y las dos pruebas manuales exigidas son de la persona.

## Evidence To Capture

- Salida de `./gradlew test` con `ProfileEditStateTest`, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: editores de `ui.preferences` reutilizados sin cambios; `ProfileEditState` sin imports
  Compose/Android; sin cambios en build/lockfile.
- Resultado manual en emulador de las dos verificaciones exigidas (preferencia editada se refleja como
  en el onboarding; identidad no invalida el Lote) y del acabado visual de las hojas (descripción;
  captura opcional).
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Reutilizar `OnboardingPreferencesDraft` (aditivo) como único motor de preferencias**: evita
  reimplementar toggles y materializa la mitigación del riesgo "el Perfil duplica pantallas del
  onboarding y divergen". Las dos factorías nuevas no cambian ninguna operación existente, igual que
  se hizo con `RecipeJson` en `suggestion-batch-cache`. `ProfileEditState` vive en `ui.profile` y
  **importa** el borrador de `ui.onboarding`; es una dirección de dependencia aceptable (el borrador es
  lógica pura) y una futura renombrada neutra a `ui.preferences` queda como mejora opcional, no como
  trabajo de esta feature.
- **`toProfile` conserva `base.preferences` si el borrador no cambió**: es la garantía estructural de
  que editar la identidad no invalida el Lote (Scenario 4). Además, al ser `PreferenceProfile` una data
  class, `LaunchedEffect(profile.preferences)` de `MainShell` ni siquiera se re-ejecuta si el objeto es
  igual.
- **Un solo estado de edición (`ProfileEditState`)** en vez de varios borradores: una única fuente
  editable con identidad + preferencias, y una única traducción a `UserProfile`.
- **`updatedAt` solo avanza cuando cambian preferencias**: coherente con que `updatedAt` es un sello
  del `PreferenceProfile` y con que la Firma lo excluye; evita ruido.
- **`createdAt` se conserva**: el Perfil es una edición, no una reconstrucción; el onboarding lo fijó y
  no debe cambiar.
- **Guardado por `ProfileRepository.save` + observación**: reutiliza la puerta/observación ya aceptada;
  no se muta el estado global ni se navega manualmente. Un fallo de `save` (no esperado, porque el
  primario exige nombre válido) deja la pantalla como está; no se añade manejo de error.
- **Hojas inferiores con los editores compartidos** (`ModalBottomSheet` de Material 3, sin dependencia
  nueva): cumple el literal de `docs/design/stitch-prompt.md` sección G y la regla "un editor, dos
  entradas". Identidad y hojas son modos mutuamente excluyentes para no mezclar ediciones sin guardar.
- **Resumen y "Actualizar sugerencias" fuera de alcance**: tienen feature propia
  (`activity-summary`, `manual-suggestions-refresh`) que depende de esta; incluirlos aquí sería invadir
  su contrato y su verificación.
- **Sin imágenes ni iconos externos**: se reutiliza `AvatarPreview` (que ya dibuja el icono genérico con
  `Canvas`) y el chevron es un glifo de texto/Compose, coherente con el repositorio (sin
  `material-icons`).

### Unknowns

- **Visibilidad del guardado**: la hoja persiste al cerrarse; si el usuario la descarta sin cambios,
  `toProfile == profile` y no se escribe. No hay botón global "Guardar todo": es una elección de UX
  alineada con el "Editar" por sección de `DESIGN.md`, ajustable como detalle visual sin cambiar el
  contrato puro.
- **Round-trip del `ProfileEditStateSaver`**: no se testea en JVM (vive en la capa Compose); se verifica
  por inspección y retención manual, como en features previas.
- **`ModalBottomSheet` y el BOM**: puede requerir `@OptIn(ExperimentalMaterial3Api::class)`; no añade
  dependencias. Si el BOM no la expusiera, la alternativa es un `Dialog`/`AlertDialog` con el mismo
  editor, sin cambiar los contratos puros.
- **Carga de assets en el hilo principal**: ya registrada; el Perfil no la empeora (recibe la Taxonomía
  y el Perfil ya cargados).
- **Consolidación de bases Room** (`sazon.db` + `suggestion_batch.db`): deuda ya anotada; esta feature
  no la toca.

## Validator Checklist

- [ ] La implementación se queda en la pestaña Perfil (identidad + preferencias) sobre `MainShell`; sin
      Resumen de actividad, sin "Actualizar sugerencias", sin Ajustes ni "Borrar mis datos".
- [ ] Los escenarios 1–10 pasan; la lógica está cubierta en JVM con casos negativos que no pasan en
      vacío (identidad vs. preferencias, firma, nombre inválido, color corrupto).
- [ ] Editar una preferencia desde el Perfil produce el mismo `PreferenceProfile` que el onboarding
      (motor compartido) y una firma distinta (Scenario 6 y 7).
- [ ] Editar solo nombre o color **no** cambia `preferences` ni la firma (Scenario 4); `createdAt` se
      conserva.
- [ ] Los editores de `ui.preferences` se reutilizan **sin cambios** en hojas inferiores; no hay
      pantallas/editor duplicados del onboarding.
- [ ] Las opciones salen de la Taxonomía (nada hardcodeado); alérgenos en ámbar y nunca `error`; sin
      hex sueltos.
- [ ] La persistencia usa `ProfileRepository.save` por observación; no se toca `ProfileDatabase` ni
      `data.profile/**`/`domain.profile/**`.
- [ ] `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian (sin dependencias nuevas); no se
      introduce librería de navegación.
- [ ] `feature_list.json`/`PROGRESS.md` quedan actualizados con evidencia verificable y
      `docs/technical-discovery.md` coherente con `ui.profile` y el guardado.

## Implementation Findings (implementer, 2026-10-09)

- **Callbacks de edición de identidad**: la firma de `ProfileIdentityEditor` de la spec no incluía cómo
  entrar/salir del modo edición. La implementación añade `onEdit` y `onCancel` (necesarios para el
  "Editar"/"Cancelar" de `DESIGN.md`/`stitch-prompt.md`) además de `onSave`; los contratos puros
  (`ProfileEditState`) no cambian.
- **`ModalBottomSheet` es experimental en el BOM actual (2026.02.01)**: `ProfileEditorSheet` requiere
  `@OptIn(ExperimentalMaterial3Api::class)`. No añade dependencias (ya venía de Material 3), como preveía
  la sección *Unknowns*.
- **Firma de `ProfileEditorSheet`**: se respeta `ProfileEditorSheet(section, state, taxonomy,
  onDraftChange, onDismiss)` de la spec (el estado se lee como `state.preferencesDraft`).
- **Reutilización sin cambios**: los ocho editores de `ui.preferences` y `AvatarPreview` se consumen sin
  modificar; `git status` confirma que solo cambian los ficheros previstos más
  `OnboardingPreferencesDraft.kt` (aditivo). `app/build.gradle.kts` y `gradle/libs.versions.toml` no
  cambian.
- **Verificación**: `ProfileEditStateTest` (9 tests, JVM) en verde; 198 tests / 0 fallos en 31 suites;
  lint 0 errores y 13 warnings preexistentes, ninguno en ficheros de la feature.

