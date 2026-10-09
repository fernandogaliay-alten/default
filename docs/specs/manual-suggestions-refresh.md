# Feature Implementation Spec: Actualizar sugerencias desde el Perfil

## Source Feature

- `id`: `manual-suggestions-refresh`
- `area`: `ui`
- `depends_on`: `["suggestion-batch-cache", "profile-screen"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Exponer al usuario la **política cache-first** del Lote de sugerencias, que hasta ahora es invisible:
añadir a la pestaña **Perfil** (ya real, `profile-screen`) una sección **"Sugerencias"** que muestre
**cuándo se actualizaron por última vez** las sugerencias (`SuggestionBatch.generatedAt`, leído del
Lote ya observado) y un botón **"Actualizar sugerencias"** que **fuerza** un refresco llamando al
servicio ya aceptado con `force = true`.

El refresco **no cambia ninguna preferencia**: solo consume `profile.preferences`, vuelve a generar
el Lote (semilla + IA válida) y lo persiste con un `generatedAt` nuevo; como `MainShell` ya observa
`SuggestionBatchRepository.observe()`, la fecha mostrada y el carrusel de la Home se actualizan solos.
La feature es **UI de Perfil + un formateador de fecha puro**; no toca la política, el servicio, el
dominio ni la persistencia.

## Non-Goals

- **No** se implementa el **Resumen de actividad** (recetas generadas / favoritas): es
  `activity-summary`; su sección "Resumen" **no** se pinta aquí.
- **No** se reimplementa ni se modifica la **política cache-first** (`SuggestionBatchPolicy`), el
  servicio (`SuggestionBatchService.ensureBatch`), el puerto/repositorio del Lote ni su persistencia
  Room: se **consumen** tal cual. La costura `force = true` ya existe y está aceptada
  (`suggestion-batch-cache`, escenario 6).
- **No** se cambia la **Firma de preferencias**, el `PreferenceProfile` ni el Perfil persistido: el
  refresco **no** llama a `ProfileRepository.save` y **no** altera `preferences`, `createdAt`,
  `updatedAt` ni la identidad (`displayName`/`avatarColorId`).
- **No** se conecta el **proveedor real de IA** (`real-ai-provider`): sigue `FakeRecipeGenerator`, sin
  red; el refresco es determinista en desarrollo.
- **No** se crea una librería de navegación ni DI, ni se añaden dependencias
  (`app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian).
- **No** se implementan "Ajustes" ni "Borrar mis datos" (fuera de alcance y ya diferidos por
  `profile-screen`); **no** se añaden editores ni pantallas nuevas de preferencias.
- **No** se muestra un huso horario o fecha absoluta: el diseño (`docs/design/stitch-prompt.md`
  sección G) pide una **antigüedad relativa** ("hace 3 días").

## Job Story

Cuando quiero que mis sugerencias se ajusten a lo que tengo ahora,
quiero ver desde cuándo no se actualizan y poder forzar un refresco con un solo toque desde mi Perfil,
para decidir yo cuándo pedir un lote nuevo sin rehacer el onboarding ni tocar mis preferencias.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); puede ver la antigüedad de las
  sugerencias y forzar el refresco. No hay cuentas ni roles.
- **Desarrollador / agente**: verifica en **JVM** la parte pura (el formateador de antigüedad) y, por
  inspección, el cableado del botón al servicio con `force = true`; el acabado puramente visual y la
  prueba manual de emulador los revisa la persona tras el merge (AGENTS.md).
- **Sistema**: ante fallo del generador, el servicio ya degrada a lote solo semilla (modo esperado);
  esta feature no añade manejo de error propio.

## Acceptance Scenarios

### Scenario 1: el Perfil muestra la antigüedad del Lote y la acción de refresco (happy path)

Given un `UserProfile` existente y un Lote cacheado con `generatedAt = now - 3 días`,
When se abre la pestaña Perfil,
Then aparece el encabezado "Sugerencias", el texto "Última actualización: hace 3 días", el helper
"Se renuevan cuando cambian tus preferencias." y el botón "Actualizar sugerencias".

### Scenario 2: forzar el refresco renueva el Lote sin cambiar preferencias (verificación exigida)

Given la pestaña Perfil con un Lote cacheado **fresco** y firma igual a la actual,
When el usuario pulsa "Actualizar sugerencias",
Then `MainShell` lanza `SuggestionBatchService.ensureBatch(profile.preferences, force = true)`, la
política decide `Generate(FORCED)`, se persiste un Lote nuevo con `generatedAt = ahora` y **no** se
llama a `ProfileRepository.save` ni cambia ningún campo de `profile.preferences`.

### Scenario 3: la fecha mostrada se actualiza al avanzar `generatedAt` (lógica pura)

Given el formateador puro `SuggestionsRefreshFormat.lastUpdatedLabel(generatedAt, now)`,
When se compara un `generatedAt` de hace 3 días con uno más reciente (mismo `now`),
Then el texto pasa de "Última actualización: hace 3 días" a "Última actualización: hoy"; es decir, la
fecha derivada refleja el `generatedAt` que emite `observe()` tras el refresco.

### Scenario 4: tabla de antigüedad relativa y casos límite (lógica pura)

Given `now` fijo,
When `now - generatedAt` es `< 1 día` → "Última actualización: hoy"; `== 1 día` → "hace 1 día";
`== 3 días` → "hace 3 días"; `== 7 días` → "hace 7 días"; y exactamente `1 día - 1 ms` → "hoy",
Then se cumple cada etiqueta, sin negativos aunque `now < generatedAt` (cambio de hora del
dispositivo se degrada a "hoy").

### Scenario 5: sin Lote todavía se muestra un respaldo y el botón sigue disponible

Given que `observe()` aún no ha emitido ningún Lote (`generatedAt == null`),
When se abre la pestaña Perfil,
Then el texto es el respaldo "Aún no hay sugerencias." y "Actualizar sugerencias" permanece
**habilitado** (el usuario puede forzar el primer lote).

### Scenario 6: el refresco en vuelo evita disparos duplicados

Given que un refresco manual está en curso,
When el usuario vuelve a tocar el botón antes de que termine,
Then el botón está **deshabilitado** mientras `isRefreshingSuggestions == true` (no se lanza una
segunda llamada a `ensureBatch`); al terminar vuelve a habilitarse.

### Scenario 7: caso negativo — el refresco no toca la identidad, las preferencias ni el borrador

Given el Perfil abierto con un `ProfileEditState` derivado (o con una edición de identidad sin
guardar),
When se pulsa "Actualizar sugerencias",
Then el `PreferenceProfile` persistido, `createdAt`, la identidad y el estado editable quedan
**intactos**: `PreferenceSignature.of(profile.preferences)` no cambia y no hay escritura en
`ProfileRepository`; el refresco solo escribe el Lote en `SuggestionBatchRepository`.

## Repository Research

### Files Inspected

- `feature_list.json` — definición de `manual-suggestions-refresh` (`area: ui`, `depends_on`,
  verificación manual, nota "expone la política cache-first"); features vecinas `activity-summary`
  (también de Perfil) y `suggestion-batch-cache`/`profile-screen`, ya `accepted`.
- `PROGRESS.md` — sesiones 038 y 040 (`profile-screen` implementada y validada con `accept`); sesiones
  030–031 (`suggestion-batch-cache` `accepted`, con `force`/`FORCED` cubierto). Estado verificado:
  200 tests / 0 fallos; siguiente feature en lista `generation-flow-with-validation`.
- `AGENTS.md` — verificación de UI por lógica/estado en JVM; lo puramente visual y las pruebas de
  emulador las revisa la persona; una feature por sesión.
- `CONTEXT.md` — "Pantalla Perfil" ya declara que el usuario "fuerza la actualización del Lote de
  sugerencias"; "Lote de sugerencias", "Firma de preferencias", "Sugerencia".
- `docs/domain-model.md` — `SuggestionBatch` (`preferenceSignature`, `generatedAt`, recetas; `status`
  `fresh`/`stale` **derivado** por la política, TTL 7 días) y Scenario 8 "Refresco manual" ("desde la
  pantalla Perfil el usuario fuerza 'Actualizar sugerencias' → se solicita un lote `ai` nuevo sin
  cambiar ninguna preferencia").
- `docs/design/stitch-prompt.md` — sección G (Perfil): "Section 'Sugerencias': a row reading 'Última
  actualización: hace 3 días' with a secondary pill button 'Actualizar sugerencias' and the helper
  'Se renuevan cuando cambian tus preferencias.'".
- `DESIGN.md` — Core Screens 7 (Perfil): "acción 'Actualizar sugerencias' con la fecha del último
  refresco"; tokens (acción principal terracota, secundario oliva, formas 24/píldora).
- `docs/technical-discovery.md` — `MainShell` compone el Lote: `observe()` alimenta el carrusel y
  `ensureBatch` se llama una vez al entrar (cache-first); `ui.profile` (`ProfileEditState`,
  `ProfileScreen`) y la nota de que "Actualizar sugerencias" quedó fuera de alcance.
- `docs/specs/suggestion-batch-cache.md` — `SuggestionBatchService.ensureBatch(profile, force)`,
  `SuggestionBatch.generatedAt`, `SuggestionBatchRepository.observe()` y la costura `force`
  (`Generate(FORCED)`), ya entregada y aceptada.
- `docs/specs/profile-screen.md` — estructura de `ProfileScreen` (header, "Mis preferencias",
  "Alergias e intolerancias", "Mi cocina"), el patrón `rememberSaveable` + `Saver` y la nota de que
  "Sugerencias"/Resumen se difieren a features propias.
- Código: `ui/MainShell.kt` (observa el Lote con `collectAsState`, `LaunchedEffect(profile.preferences)
  { ensureBatch }`, enruta `MainTab.PROFILE -> ProfileScreen`), `ui/SazonApp.kt` (observa el Perfil,
  `rememberCoroutineScope` + `ProfileRepository.save`),
  `ui/profile/{ProfileScreen,ProfileEditState}.kt`, `ui/theme/{Color,Spacing,Shape,Type}.kt`,
  `domain/suggestions/{SuggestionBatch,SuggestionBatchPolicy,SuggestionBatchService,SuggestionBatchRepository}.kt`,
  `data/suggestions/{RoomSuggestionBatchRepository,SuggestionBatchDatabase}.kt`, `MainActivity.kt`
  (composición de dependencias a mano).
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — Material 3 (`Button`/`FilledTonalButton`),
  Compose y coroutines ya presentes; sin `material-icons` ni `navigation-compose`.

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: el formateo de fecha se modela en Kotlin puro sin imports de
  Compose/Android (`SuggestionsRefreshFormat`, estilo `HomeGreeting`/`RecipeCardFormat`) y se testea en
  JVM; los `@Composable` solo pintan.
- **El Lote se consume tal cual**: `MainShell` ya tiene `batch` (de `observe()`) y el
  `SuggestionBatchService`; no se reimplementa la política ni se duplica la observación.
- **El async lo orquesta la capa superior**: como `SazonApp` guarda con `ProfileRepository.save` en un
  `rememberCoroutineScope` y pasa `onSaveProfile` a `MainShell`, aquí `MainShell` lanza
  `ensureBatch(force = true)` y pasa al Perfil un callback + el estado "en vuelo".
- **Tokens**: colores/espaciado/formas desde `com.sazon.app.ui.theme` (nunca hex sueltos); botón de
  acción en tono secundario (oliva) y formas píldora (`SazonPillShape`), coherente con
  `ResultFilterRow`/`ServingsStepper`.
- **Microcopy fija en un solo sitio**: la etiqueta relativa y los textos ("Actualizar sugerencias",
  helper) viven en el objeto puro, no repartidos por la capa Compose.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, con casos negativos que no pasan
  en vacío.
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- `ProfileScreen` no recibe el Lote ni ninguna costura de refresco: no muestra la antigüedad ni tiene
  acción de refresco (la sección "Sugerencias" no existe).
- No existe ningún formateador de la antigüedad relativa de `generatedAt`.
- `MainShell` solo llama a `ensureBatch` desde un `LaunchedEffect(profile.preferences)` (una vez al
  entrar o al cambiar preferencias); no expone `force = true` a ninguna acción del usuario.

## Technical Approach

Estrategia: **formateador puro testeable en JVM + sección Compose delgada + cableado del botón al
servicio con `force = true` en `MainShell`**, sin tocar dominio, persistencia ni build.

### 1. Formateador puro — `com.sazon.app.ui.profile.SuggestionsRefreshFormat`

Objeto **puro** (sin Compose/Android), único responsable de la antigüedad relativa y del microcopy:

- `const val DAY_MILLIS: Long = 24L * 60 * 60 * 1_000`.
- `fun lastUpdatedLabel(generatedAt: Long?, now: Long): String`:
  - `generatedAt == null` → `"Aún no hay sugerencias."`;
  - si no, `elapsedDays = ((now - generatedAt).coerceAtLeast(0)) / DAY_MILLIS` →
    `0 → "Última actualización: hoy"`, `1 → "Última actualización: hace 1 día"`,
    `>= 2 → "Última actualización: hace N días"`.
- Constantes de microcopy reutilizadas por la UI: `ACTION_LABEL = "Actualizar sugerencias"`,
  `REFRESHING_LABEL = "Actualizando…"`, `HELPER_TEXT = "Se renuevan cuando cambian tus preferencias."`.

El `now` lo inyecta la capa Compose (capturado una vez con `remember`, como `HomeGreeting` captura la
hora); la función es determinista y testeable.

### 2. Sección Compose — `com.sazon.app.ui.profile.ProfileSuggestionsSection`

`@Composable fun ProfileSuggestionsSection(generatedAt: Long?, isRefreshing: Boolean, onRefresh: () ->
Unit, now: Long)`:

- Encabezado "Sugerencias" (se reutiliza el `SectionHeading` ya privado de `ProfileScreen`; si no es
  accesible, se replica su estilo `titleMedium`/`onBackground`).
- Texto `SuggestionsRefreshFormat.lastUpdatedLabel(generatedAt, now)` + helper `HELPER_TEXT`.
- Botón **píldora** (`SazonPillShape`) con el tono secundario del tema: texto `ACTION_LABEL`, y
  `REFRESHING_LABEL` mientras `isRefreshing`; `enabled = !isRefreshing`; `onClick = onRefresh`.
- Sin iconos ni imágenes (no hay `material-icons`): el botón es solo texto.

### 3. `ProfileScreen` — nuevos parámetros y posición

Se añaden tres parámetros a `ProfileScreen`:

```kotlin
suggestionsGeneratedAt: Long?,          // batch?.generatedAt observado
isRefreshingSuggestions: Boolean,       // refresco en vuelo
onRefreshSuggestions: () -> Unit,       // dispara ensureBatch(force = true)
```

La sección se inserta como `item { ... }` **después de la cabecera de identidad y antes de "Mis
preferencias"** (el hueco que `profile-screen` dejó libre al diferir "Resumen"/"Sugerencias"). El
`now` se captura una vez con `remember { System.currentTimeMillis() }` y se pasa al composable. Nada
más de `ProfileScreen` cambia: el estado editable, las hojas y el guardado por `onSaveProfile` siguen
igual.

### 4. `MainShell` — costura del refresco con `force = true`

`MainShell` ya tiene `batch` (de `collectAsState`) y `suggestionBatchService`. Añade:

- `val scope = rememberCoroutineScope()` y `var refreshingSuggestions by remember { mutableStateOf(false) }`
  (estado **de UI**, no persistido; un refresco no sobrevive a un cambio de configuración, igual que
  un `editingIdentity`).
- Pasa a `ProfileScreen`:
  - `suggestionsGeneratedAt = batch?.generatedAt`,
  - `isRefreshingSuggestions = refreshingSuggestions`,
  - `onRefreshSuggestions = { if (!refreshingSuggestions) { refreshingSuggestions = true; scope.launch {
    try { suggestionBatchService.ensureBatch(profile.preferences, force = true) } finally {
    refreshingSuggestions = false } } } }`.

El `LaunchedEffect(profile.preferences)` existente se mantiene intacto. Tras el refresco, el `save`
del servicio emite por `observe()`, `batch` cambia y tanto la fecha del Perfil como el carrusel de la
Home se recomponen sin intervención extra. No se añade un `LaunchedEffect` nuevo.

### 5. Sin cambios de dominio, persistencia ni build

No se tocan `SuggestionBatchPolicy`, `SuggestionBatchService`, `SuggestionBatchRepository`,
`data.suggestions/**`, `domain.profile/**`, `data.profile/**`, la Taxonomía, `app/build.gradle.kts`
ni `gradle/libs.versions.toml`.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/profile/SuggestionsRefreshFormat.kt` — formateador puro de la
  antigüedad relativa y microcopy.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileSuggestionsSection.kt` — sección Compose
  (encabezado, fecha, helper y botón píldora).
- `app/src/test/java/com/sazon/app/ui/profile/SuggestionsRefreshFormatTest.kt` — escenarios 3–5 (JVM).

**Modificar**

- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — tres parámetros nuevos y el `item`
  de la sección "Sugerencias" entre la cabecera y "Mis preferencias".
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — `rememberCoroutineScope` + estado "en vuelo" y
  cableado de `onRefreshSuggestions = ensureBatch(preferences, force = true)`.
- `docs/technical-discovery.md` — registrar la sección "Sugerencias" del Perfil y el refresco manual
  por `force = true`.

**No se toca** `domain.suggestions/**`, `data.suggestions/**`, `domain.profile/**`,
`data.profile/**`, la Taxonomía, `seed-recipes.json`, `Home`, `Generator`, `SazonApp`,
`app/build.gradle.kts` ni `gradle/libs.versions.toml`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (Core Screens 7 y tokens) + `docs/design/stitch-prompt.md` sección G
  ("Section 'Sugerencias'").
- Screens or states affected: pestaña Perfil, sección "Sugerencias" (fecha relativa + helper) y botón
  en dos estados (normal / "Actualizando…" deshabilitado); estado sin Lote (`generatedAt == null`).
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing` (12/16/24), `SazonPillShape` para el botón y
  tono **secundario** (oliva/`secondary`/`secondaryContainer`) para distinguir la acción de la única
  acción principal terracota de la pantalla; objetivos táctiles ≥48dp; tipografía dinámica.
- Sin iconos ni imágenes externas (no hay `material-icons` en el repositorio).
- Capturas de referencia (backfill PR #37): `docs/design/concepts/manual-suggestions-refresh/` —
  pantalla `perfil-sugerencias`. Pendiente de exportar desde Stitch (petición en el `README.md` de la
  carpeta); el acabado puramente visual lo revisa la persona tras el merge y un defecto se corrige con
  feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT nueva de nivel repo; el
  refresco consume la política ya documentada.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** — añadir la sección "Sugerencias" de `ui.profile` y que el
  refresco manual llama a `SuggestionBatchService.ensureBatch(..., force = true)` desde `MainShell`
  (mismo Lote observado; no cambia preferencias).
- `docs/domain-model.md`: **not needed** — ya describe el Scenario 8 "Refresco manual" y el
  `generatedAt`/`status` derivado del Lote.
- `DESIGN.md` y `docs/design/stitch-prompt.md`: **not needed** — la sección ya está descrita.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión de planificación.

## Implementation Plan

1. Crear `SuggestionsRefreshFormat` (constantes + `lastUpdatedLabel`) y su test JVM.
2. Crear `ProfileSuggestionsSection` con el encabezado, la fecha, el helper y el botón píldora.
3. Ampliar `ProfileScreen` con los tres parámetros y el `item` de la sección.
4. Cablear `MainShell` (`rememberCoroutineScope`, estado "en vuelo" y
   `ensureBatch(profile.preferences, force = true)`).
5. Ejecutar `init.sh` (test + lint) en verde y corregir lo que salga.
6. Actualizar `docs/technical-discovery.md`; el implementer registra la evidencia y el estado en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Crear `SuggestionsRefreshFormat` con `DAY_MILLIS`, `lastUpdatedLabel`, `ACTION_LABEL`,
      `REFRESHING_LABEL` y `HELPER_TEXT` (puro, sin Compose/Android).
- [ ] Escribir `SuggestionsRefreshFormatTest` (escenarios 3–5 y límites de la tabla) con casos
      negativos que no pasan en vacío.
- [ ] Crear `ProfileSuggestionsSection` (encabezado + fecha + helper + botón píldora secundario).
- [ ] Ampliar `ProfileScreen` con `suggestionsGeneratedAt`, `isRefreshingSuggestions` y
      `onRefreshSuggestions`, y colocar la sección tras la cabecera.
- [ ] Cablear `MainShell` para pasar `batch?.generatedAt`, el estado "en vuelo" y el callback que llama
      a `ensureBatch(profile.preferences, force = true)` sin tocar `ProfileRepository`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: `SuggestionsRefreshFormatTest` en verde sin romper
  los 200 tests existentes:
  - `lastUpdatedLabel(null, now) == "Aún no hay sugerencias."`.
  - `now - generatedAt` `< 1 día` → "hoy"; `== 1 día` → "hace 1 día"; `== 3 días` → "hace 3 días";
    `== 7 días` → "hace 7 días"; `1 día - 1 ms` → "hoy"; `now < generatedAt` → "hoy" (sin negativos).
  - Con el mismo `now`, un `generatedAt` más reciente produce una etiqueta más corta (escenario 3).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: la llamada del botón usa `force = true`; `MainShell`/`ProfileScreen` **no** llaman a
  `ProfileRepository.save` desde el refresco; `domain.suggestions`/`data.suggestions` sin cambios;
  `app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios; `SuggestionsRefreshFormat` sin
  imports Compose/Android.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Pulsar "Actualizar sugerencias" renueva el Lote **sin cambiar ninguna preferencia** y **actualiza
    la fecha mostrada** (verificación exigida de `feature_list.json`); las recetas del carrusel de la
    Home pueden cambiar (misma ejecución del generador falso).
  - El botón se deshabilita mientras "Actualizando…"; sin Lote, la fecha cae al respaldo.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y el test del formateador corre dentro de `./gradlew test` sin emulador.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador. La parte verificable (formateo de la antigüedad y límites) se cubre en JVM; el
cableado del botón se verifica por inspección y la prueba manual (renovación sin cambiar preferencias y
fecha actualizada) es de la persona. La costura `force` del servicio ya está cubierta por
`SuggestionBatchServiceTest` (escenario 6 de `suggestion-batch-cache`), por lo que no se duplica.

## Evidence To Capture

- Salida de `./gradlew test` con `SuggestionsRefreshFormatTest`, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: el callback de refresco usa `force = true`; no hay `ProfileRepository.save` en el
  refresco; `SuggestionsRefreshFormat` sin imports Compose/Android; sin cambios en
  `domain.suggestions`/`data.suggestions` ni en el build.
- Resultado manual en emulador de la verificación exigida (renueva el Lote sin cambiar preferencias y
  actualiza la fecha mostrada) y del estado "Actualizando…" (descripción; captura opcional).
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Consumir la costura `force = true` ya aceptada**: `SuggestionBatchPolicy` decide `Generate(FORCED)`
  con `force`; la política, el servicio y la persistencia no se tocan. `force` ya está probado en
  `suggestion-batch-cache`, de modo que esta feature solo lo expone en la UI.
- **Sin `ProfileRepository.save` en el refresco**: la garantía "sin cambiar ninguna preferencia" se
  materializa no escribiendo el Perfil; `ensureBatch` solo **lee** `profile.preferences`. Los campos
  de identidad y preferencias quedan intactos y `PreferenceSignature` no cambia.
- **La antigüedad se deriva, no se persiste**: `generatedAt` ya es el sello del Lote; la etiqueta es
  puramente derivada por `SuggestionsRefreshFormat`. No se añade un campo "lastRefreshedAt" al Perfil.
- **La fecha se lee del Lote observado, no de una copia local**: `MainShell` pasa
  `batch?.generatedAt`; tras el `save` del servicio, `observe()` recompone y la fecha se actualiza sin
  que la UI lleve su propio contador. Evita una segunda fuente de verdad.
- **Antigüedad relativa en días, "hoy"/"hace N días"**: es lo que pide el diseño ("hace 3 días"); se
  evita una escala horas/minutos para ser determinista y simple, con el TTL de 7 días como horizonte
  natural.
- **Sin doble disparo**: el estado "en vuelo" deshabilita el botón. Es estado de UI (como
  `editingIdentity` de `ProfileScreen`), no persistido; `MainShell` lo orquesta porque posee el
  servicio y el `rememberCoroutineScope`.
- **El botón es la única acción secundaria de la pantalla** (tono oliva/píldora), para no competir con
  la acción principal terracota del Perfil.
- **`SazonApp` no cambia**: `MainShell` ya recibe el servicio y el Lote; el refresco no necesita una
  costura nueva en la raíz.
- **Resumen de actividad fuera de alcance**: es `activity-summary`; su sección no se pinta aquí.

### Unknowns

- **Concurrencia entre el refresco manual y el `ensureBatch` de entrada**: el servicio no serializa
  llamadas (`SuggestionBatchPolicy` es puro y el repo no tiene mutex). Si el usuario fuerza el refresco
  justo cuando `LaunchedEffect(profile.preferences)` aún corre, podrían solaparse dos generaciones; en
  la práctica el de entrada es rápido y la persona llega al Perfil después. El botón deshabilitado
  evita solo el doble toque manual. No se añade sincronización en esta feature.
- **`now` capturado una vez**: al usar `remember { System.currentTimeMillis() }`, en una sesión muy
  larga la etiqueta podría quedarse "hoy" aunque pasen horas; coherente con `HomeGreeting` (que captura
  la hora una vez) y sin impacto en la verificación. Ajustable como detalle visual.
- **Rango de la escala de días**: un Lote muy antiguo mostraría "hace 30 días"; con TTL de 7 días el
  Lote caduca antes y se refresca al entrar, así que no se acota.
- **Prueba manual de la fecha**: que la fecha se actualice depende de que `observe()` emita el Lote
  nuevo; en un reloj con granularidad gruesa podría mostrarse el mismo "hoy" antes y después (la
  renovación del Lote sí es observable en las recetas). Se resuelve en la prueba manual de la persona.

## Validator Checklist

- [ ] La implementación se queda en `ui.profile` (formateador + sección) y el cableado en `MainShell`;
      no toca `domain.suggestions`, `data.suggestions`, `ProfileDatabase`, `data.profile` ni
      `domain.profile`.
- [ ] Los escenarios 1–7 pasan; la parte pura (escenarios 3–5) está cubierta en JVM con casos límite y
      negativos que no pasan en vacío.
- [ ] Pulsar "Actualizar sugerencias" llama a `ensureBatch(profile.preferences, force = true)` y
      **no** escribe el Perfil (verificación exigida: renueva el Lote sin cambiar preferencias).
- [ ] La fecha mostrada se deriva de `SuggestionBatch.generatedAt` observado y se actualiza al emitir
      el Lote nuevo (verificación exigida: actualiza la fecha mostrada).
- [ ] Mientras un refresco está en vuelo el botón está deshabilitado; sin Lote se muestra el respaldo y
      el botón sigue disponible.
- [ ] `PreferenceSignature`/`preferences`/`createdAt`/identidad quedan intactos tras el refresco; no hay
      `ProfileRepository.save` desde el refresco.
- [ ] El formateador es puro (sin imports Compose/Android) y el microcopy vive en un solo sitio; sin hex
      sueltos ni iconos; `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian.
- [ ] `SazonApp`, `Home`, `Generator`, la Taxonomía, el estilo de diseño y las features vecinas quedan
      intactos; no se implementa Resumen de actividad, Ajustes ni "Borrar mis datos".
- [ ] `feature_list.json`/`PROGRESS.md` quedan actualizados con evidencia verificable y
      `docs/technical-discovery.md` coherente con la sección "Sugerencias" y el refresco por `force`.
