# Feature Implementation Spec: Modo cocina — pasos a pantalla completa con temporizadores

## Source Feature

- `id`: `cooking-mode`
- `area`: `ui`
- `depends_on`: `["recipe-detail-screen"]` (`accepted`; hoy `onStartCooking` es una costura no-op).
- `status`: `blocked` en `feature_list.json` a la hora de planificar (falta de capturas). Esta spec es
  el contrato planner → implementer → validator. El planner **no** modifica `feature_list.json` ni
  `PROGRESS.md`.
- `source`: `feature_list.json` (issue #76).
- Nota de la feature: "Amplía el MVP: 'Modo cocina con temporizadores reales' estaba en Non-Goals de
  `docs/build-brief.md`". Verificación exigida: `./gradlew test` cubre en JVM la **lógica de estado del
  modo cocina** (paso actual, navegación Siguiente/Anterior, marcado y temporizador); prueba manual en
  emulador: "Empezar a cocinar" abre el modo, se recorren los pasos y el temporizador cuenta y avisa al
  terminar.
- Capturas obligatorias (gate de UI) **ya presentes**: ver `Visual Design Impact`.

## Goal

Entregar la pantalla **Modo cocina**: desde el Detalle, "Empezar a cocinar" (hoy costura no-op) abre
una página a pantalla completa que recorre los pasos de la receta uno a uno con **progreso**
("Paso 2 de 5" + barra), **navegación Anterior/Siguiente** (y "Terminar" en el último paso),
**marcado de paso como hecho** (con tachado/atenuado) y un **temporizador por paso** con cuenta atrás
en cifras tabulares, estados Inactivo/En marcha/Pausado/Terminado y un **aviso de éxito en la app**
("¡Listo!", tono miel/oliva, nunca rojo) al llegar a cero.

La pieza central es la **lógica de estado pura y testeable en JVM** (sin Compose ni Android): la
máquina de estados del temporizador (`CookingTimerState`) y el estado del modo (`CookingModeState`),
más un formateador (`CookingModeFormat`). La capa Compose (`CookingModeScreen`) es delgada y conduce
el reloj real mediante un `LaunchedEffect` que solo alimenta `tick(1000)` a la lógica pura.

## Non-Goals

- **No** se implementa **notificación de sistema, sonido ni vibración**: el "aviso al terminar" es la
  **notificación visual en pantalla** ("¡Listo!"). Notificaciones/recordatorios siguen fuera del MVP
  (`docs/build-brief.md`); no se añaden permisos ni dependencias de audio. No hay _wake-lock_.
- **No** se persiste nada: ni progreso, ni pasos hechos, ni el temporizador. Cerrar el modo lo
  descarta.
- **No** se implementa la **lista de la compra** ni "Añadir a la lista" (fuera del MVP).
- **No** se toca la firma ni el contenido de `recipe-detail-screen`: su `onStartCooking` ya existe como
  costura; esta feature **solo** la cablea desde `MainShell`.
- **No** se implementa **Favoritos**, **Búsqueda con IA**, proveedor real ni el Resumen de actividad.
- **No** se toca `domain.recipe`, `domain.validation`, `domain.generation`, `domain.generator`,
  `domain.prompt`, `domain.suggestions`, `domain.profile`, `data/**`, la Taxonomía, `seed-recipes.json`,
  Room/Perfil ni `init.sh`.
- **No** se añaden `navigation-compose`, iconos (`material-icons-*`), imágenes (Coil), DI ni permisos;
  **sin dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos).

## Job Story

Cuando estoy cocinando con las manos ocupadas y la receta abierta,
quiero seguir los pasos de uno en uno, marcarlos como hechos y cronometrar cada uno con un temporizador,
para no perder el hilo ni pasarme de tiempo y saber cuándo el paso está listo.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo. Abre el modo desde el Detalle, navega los pasos,
  marca, arranca/pausa/reanuda el temporizador y sale con el botón "Atrás" o "Terminar".
- **Desarrollador / agente**: cubre en JVM la máquina del temporizador, la navegación de pasos, el
  marcado y los formateos, con vectores dorados y casos negativos; el acabado visual (tarjeta del paso,
  píldora miel, cuenta atrás, estados del temporizador, claro/oscuro) lo revisa la persona en el
  emulador (AGENTS.md no automatiza UI).
- **Sistema**: sin red y sin persistencia; la receta ya viene validada y sus pasos con su `minutes`.

## Acceptance Scenarios

### Scenario 1: abrir el modo desde el Detalle

Given una receta con `steps` abierta en el Detalle,
When el usuario pulsa "Empezar a cocinar" en la barra inferior,
Then se abre `CookingModeScreen` a pantalla completa **encima** del Detalle (sin desmontarlo), en el
**paso 1** (`stepIndex = 0`, `"Paso 1 de N"`), y "Anterior" queda **deshabilitado**.

### Scenario 2: navegación Siguiente/Anterior y "Terminar" (verificación exigida)

Given el modo abierto en el paso 2 de 5,
When el usuario pulsa "Siguiente",
Then avanza al paso 3, el progreso pasa a `"Paso 3 de 5"` y el temporizador **se reinicia** al de ese
paso (Inactivo, con su duración);
When el usuario pulsa "Anterior",
Then vuelve al paso 2 (con su temporizador reiniciado); en el **primer** paso "Anterior" es un no-op y
en el **último** el botón primario pasa a leer **"Terminar"**, que cierra el modo y vuelve al Detalle.

### Scenario 3: el temporizador cuenta atrás y avisa al terminar (verificación exigida)

Given el modo abierto en un paso de `minutes = 10` con el temporizador Inactivo (`"10:00"`, botón
"Iniciar"),
When el usuario pulsa "Iniciar",
Then el temporizador pasa a **En marcha**, la cuenta atrás **decrementa** en cifras tabulares
(`CookingTimerState.tick`, p. ej. `600000 → 590000 → …`), y al llegar a **cero** pasa a **Terminado**
mostrando `"00:00"` y el aviso de éxito **"¡Listo!"** (miel/oliva, nunca rojo), sin seguir contando.

### Scenario 4: pausar y reanudar (verificación exigida)

Given el temporizador **En marcha** con `remainingMillis` conocido,
When el usuario pulsa "Pausar",
Then pasa a **Pausado** y el tiempo queda **congelado** (`tick` no lo reduce);
When pulsa "Reanudar",
Then sigue contando desde el valor congelado; el control de reinicio vuelve a **Inactivo**
(`remainingMillis = totalMillis`).

### Scenario 5: marcar el paso como hecho (verificación exigida)

Given el modo abierto,
When el usuario marca el checkbox "Marcar paso como hecho",
Then ese paso (`RecipeStep.order`) entra en `completedOrders`, el título/cuerpo se **tachan** y
**atenúan** (opacidad 0.45, regla de `DESIGN.md`); desmarcarlo lo saca. El marcado es **independiente**
de la navegación y del temporizador.

### Scenario 6: paso sin duración → sin temporizador

Given un paso con `minutes <= 0` (IA o receta sin tiempo),
When se muestra ese paso,
Then el **bloque de temporizador se oculta** (`CookingTimerState.hasDuration == false`); no aparece
píldora de duración ni cuenta atrás, y el resto de la pantalla (paso, marcado, barra inferior) sigue
igual.

### Scenario 7: receta sin pasos (defensivo)

Given una receta con `steps` vacía,
When se abre el modo,
Then **no** hay crash ni índice fuera de rango: se muestra el título y un mensaje de que no hay pasos,
con la barra inferior reducida a "Atrás"/"Terminar"; el dominio garantiza `1..n` pasos, así que es una
guarda defensiva.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `cooking-mode` (líneas 709-723): `area: ui`, `depends_on` `[recipe-detail-screen]`, verificación (capturas + JVM de estado + prueba manual) y nota de ampliación del MVP (issue #76).
- `docs/design/concepts/cooking-mode/README.md` — petición: una pantalla `modo-cocina/` con los tres ficheros del export y los estados del temporizador.
- `docs/design/concepts/cooking-mode/modo-cocina/{screen.png,code.html,DESIGN.md}` — **captura obligatoria ya presente** (matriz de 4 estados del temporizador; pantalla clara y oscura; reglas: paso sin duración oculta el bloque, último paso "Terminar").
- `docs/design/concepts/cooking-mode/stitch-prompt.md` — microcopy/layout exactos (PARTE 1): "Paso 2 de 5", título, tarjeta del paso, píldora miel, control del temporizador, "Marcar paso como hecho", barra "Anterior"/"Siguiente" → "Terminar".
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — costura `onStartCooking: () -> Unit` y barra inferior "Empezar a cocinar"; `TABULAR_FIGURES`, `SazonPillShape`, glifos por `Canvas`.
- `app/src/main/java/com/sazon/app/ui/detail/{RecipeDetailState,ServingScaler,IngredientFormat,RecipeDetailFormat}.kt` — patrón "lógica pura inmutable + estado sin la `Recipe` + `Saver` en Compose".
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — hospeda el Detalle como **overlay** (`var detailRecipe by remember`) y cablea `onStartCooking = {}`; punto natural para hospedar el modo cocina encima.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `RecipeStep(order, title, description, minutes)`, `Recipe.steps` (`1..n`).
- `app/src/main/java/com/sazon/app/ui/theme/{Color,Shape,Spacing,Type}.kt` — tokens: `SazonAccentContainer` → `tertiaryContainer` (miel), `SazonSecondaryContainer` (oliva), `SazonPillShape`, `SazonShapes`, `SazonComponents.buttonHeight`, `SazonSpacing`, cifras tabulares.
- `app/src/main/java/com/sazon/app/ui/preferences/ServingsStepper.kt` — patrón de control con `FilledTonalButton` circular (referencia de tamaño ≥48dp).
- `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailStateTest.kt` — patrón de tests JUnit 4 con nombres en español y recetas de prueba.
- `docs/technical-discovery.md` (líneas 262-276) — dónde ubicar `ui.cooking`; `docs/specs/recipe-detail-screen.md` — precedente del overlay y de las costuras no-op.
- `docs/design/concepts/recipe-detail-screen/detalle-receta/` y `DESIGN.md` §"Generated Concept Images" — modelo de captura aceptada por feature/pantalla.
- `docs/build-brief.md` — "Modo cocina con temporizadores reales" listado en Non-Goals (evolutivo) y "Notificaciones y recordatorios" fuera del MVP (confirma el aviso solo visual).

### Existing Patterns To Follow

- **Lógica pura + Compose delgado**: `ui.cooking` sin imports Compose/Android salvo `CookingModeScreen`; los `object`/`data class` se testean en JVM.
- **Estado sin la `Recipe`** (como `RecipeDetailState`): el estado del modo guarda índices/colecciones/servicios de tiempo, no el `Recipe`; el `Saver` de `rememberSaveable` vive en la capa Compose.
- **Overlay sobre `MainShell`**: mismo patrón que el Detalle (`var … by remember`) para abrir encima del Detalle **sin desmontarlo** y sin librería de navegación.
- **Reutilización de tokens y glifos `Canvas`**; cifras tabulares (`"tnum"`) en progreso, cuenta atrás y número de paso; objetivos ≥48dp; una única acción principal terracota por pantalla.
- **Tests JUnit 4** con nombres en español y casos negativos que fallarían si la lógica no hiciera nada.
- **Sin dependencias nuevas**; `init.sh` sin cambios.

### Current Gaps

- No existe `ui/cooking/`, ni estado de modo cocina, ni temporizador.
- `onStartCooking` del Detalle es `{}` en `MainShell`: "Empezar a cocinar" no hace nada.
- No hay formateo de cuenta atrás (`mm:ss`) ni de progreso "Paso X de N".
- No hay diferencia de tiempo testeable sin Android: hace falta una lógica que reciba el `delta` explícito.

## Technical Approach

Estrategia: **lógica pura en `com.sazon.app.ui.cooking` + capa Compose delgada**, con el modo hospedado
como overlay en `MainShell`; sin tocar el dominio aceptado ni añadir dependencias.

### 1. `CookingTimerState` — máquina de estados del temporizador (pura)

```kotlin
enum class CookingTimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class CookingTimerState(
    val status: CookingTimerStatus,
    val totalMillis: Long,
    val remainingMillis: Long,
) {
    val hasDuration: Boolean get() = totalMillis > 0
    val isRunning: Boolean get() = status == CookingTimerStatus.RUNNING
    val isFinished: Boolean get() = status == CookingTimerStatus.FINISHED

    fun start(): CookingTimerState     // IDLE -> RUNNING
    fun pause(): CookingTimerState     // RUNNING -> PAUSED
    fun resume(): CookingTimerState    // PAUSED -> RUNNING
    fun reset(): CookingTimerState     // cualquiera -> IDLE con remaining = total
    fun tick(deltaMillis: Long): CookingTimerState // RUNNING: remaining -= max(0, delta), clamp 0 -> FINISHED

    companion object { fun of(minutes: Int): CookingTimerState } // total = max(0, minutes) * 60_000L, remaining = total, IDLE
}
```

- **Determinista**: no lee reloj; el tiempo entra por `tick(deltaMillis)`. `delta <= 0` no altera nada.
- Transiciones no válidas son **no-op** (`start` desde `RUNNING`/`FINISHED`, `pause`/`resume` desde el estado equivocado, `tick` fuera de `RUNNING`).
- Al llegar `remainingMillis == 0` el estado pasa a `FINISHED` y **deja de contar** (los `tick` posteriores son no-op).
- `of(0)` (o `minutes <= 0`) → `hasDuration = false`; la UI oculta el bloque.

### 2. `CookingModeState` — paso actual, marcado y temporizador (pura)

```kotlin
data class CookingModeState(
    val stepIndex: Int,
    val totalSteps: Int,
    val completedOrders: Set<Int> = emptySet(),
    val timer: CookingTimerState,
) {
    val isFirstStep: Boolean get() = stepIndex <= 0
    val isLastStep: Boolean  get() = stepIndex >= totalSteps - 1

    fun next(steps: List<RecipeStep>): CookingModeState      // avanza y reinicia el timer al paso nuevo
    fun previous(steps: List<RecipeStep>): CookingModeState  // retrocede (no-op en el primero) y reinicia el timer
    fun toggleCompleted(order: Int): CookingModeState        // añade/quita el `RecipeStep.order`
    fun withTimer(timer: CookingTimerState): CookingModeState

    companion object { fun initial(recipe: Recipe): CookingModeState }
}
```

- `initial(recipe)`: `stepIndex = 0`, `totalSteps = recipe.steps.size`, `completedOrders = emptySet()`,
  `timer = CookingTimerState.of(recipe.steps.firstOrNull()?.minutes ?: 0)`.
- `next`/`previous` acotan a `0..totalSteps-1` (no salen de rango) y **reemplazan** el temporizador por
  `CookingTimerState.of(steps[newIndex].minutes)` usando `getOrNull` (no lanza si la lista está vacía).
  Decisión: **el temporizador está ligado al paso actual**; cambiar de paso lo reinicia a Inactivo.
- `completedOrders` es independiente del índice y del temporizador; las operaciones son inmutables.
- El estado **no guarda la `Recipe`** (para el `Saver`); la navegación recibe `List<RecipeStep>`.

### 3. `CookingModeFormat` — microcopy y formatos (pura)

```kotlin
object CookingModeFormat {
    const val FINISHED_NOTICE = "¡Listo!"
    fun progressLabel(stepIndex: Int, totalSteps: Int): String   // "Paso 2 de 5"
    fun progressFraction(stepIndex: Int, totalSteps: Int): Float // (stepIndex+1)/totalSteps, acotado 0..1
    fun durationLabel(minutes: Int): String                      // "10 min"
    fun countdown(remainingMillis: Long): String                 // "09:42" (mm:ss, Locale.ROOT, clamp >= 0)
}
```

- `countdown`: `remainingMillis` acotado a `>= 0`, a **segundos floor** y formateado `"%02d:%02d"` con
  `Locale.ROOT`; `600000 → "10:00"`, `582000 → "09:42"`, `0 → "00:00"`.
- `progressFraction` con `totalSteps <= 0` devuelve `0f` (sin división por cero).
- `durationLabel` reproduce la píldora miel del Detalle (`"N min"`).

### 4. Capa Compose — `com.sazon.app.ui.cooking.CookingModeScreen`

- `CookingModeScreen(recipe: Recipe, onClose: () -> Unit, modifier: Modifier = Modifier)`:
  - `Scaffold` sobre `colorScheme.background` con **barra inferior fija** (blanca).
  - **Cabecera**: botón "Atrás" (circular/flotante, `onClose`), etiqueta "Paso 2 de 5" en cifras
    tabulares y `LinearProgressIndicator` fina terracota (`progressFraction`).
  - **Título** de la receta (Manrope, una línea, elipsis) y **tarjeta del paso** (superficie blanca,
    `SazonShapes.large`): círculo numerado terracota (`step.order`), título y cuerpo; si el paso está en
    `completedOrders`, título/cuerpo **tachados** (`TextDecoration.LineThrough`) y **opacidad 0.45**.
  - **Bloque del temporizador** (solo si `state.timer.hasDuration`), con acento miel
    (`tertiaryContainer`/`onTertiaryContainer`): píldora `durationLabel(step.minutes)`, cuenta atrás
    `countdown(remainingMillis)` en cifras tabulares y el control según estado:
    `IDLE → "Iniciar"` (start), `RUNNING → "Pausar"` (pause), `PAUSED → "Reanudar"` (resume) + reinicio,
    `FINISHED → "00:00"` + aviso **"¡Listo!"** (miel/oliva, **nunca** `error`) con control de reinicio
    (`reset` → IDLE). Microcopy exacta según la captura.
  - **Fila de marcado**: `Checkbox` + "Marcar paso como hecho" (`toggleCompleted(step.order)`).
  - **Barra inferior**: `OutlinedButton "Anterior"` (deshabilitado si `isFirstStep`) y primario terracota
    `"Siguiente"` (`next`); en el último paso, `"Terminar"` (`onClose`); ambos pills `SazonPillShape` a
    `SazonComponents.buttonHeight`.
  - Estado con `rememberSaveable(stateSaver = CookingModeStateSaver)` (índice, total, `completedOrders`,
    estado y millis del temporizador); la `Recipe` es parámetro y no se guarda.
  - **Reloj**: `LaunchedEffect(state.timer.status) { while (isRunning) { delay(1000); state =
    state.withTimer(state.timer.tick(1000)) } }`. La lógica pura nunca lee reloj.
  - Receta sin pasos: guarda defensiva (mensaje + cierre), sin índice fuera de rango.

### 5. Integración — `MainShell`

- Añadir `var cookingRecipe by remember { mutableStateOf<Recipe?>(null) }`.
- Cablear `RecipeDetailScreen(..., onStartCooking = { cookingRecipe = recipe }, ...)` (hoy `{}`).
- Tras el bloque `detailRecipe?.let { … }`, pintar
  `cookingRecipe?.let { CookingModeScreen(recipe = it, onClose = { cookingRecipe = null }) }`, de modo
  que el modo va **encima** del Detalle y, al cerrarlo, se vuelve al Detalle intacto.
- Sin cambios en la firma de `RecipeDetailScreen` (la costura ya existe).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingTimerState.kt` — estado + enum del temporizador.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeState.kt` — paso actual, marcado y temporizador.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeFormat.kt` — progreso, duración y cuenta atrás.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — pantalla delgada + `Saver`.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingTimerStateTest.kt` — escenarios 3 y 4.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingModeStateTest.kt` — escenarios 2, 5 y 7.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingModeFormatTest.kt` — formatos y casos límite.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — hospedar el modo cocina (overlay) y cablear `onStartCooking`.
- `docs/technical-discovery.md` — ubicar `ui.cooking` (máquina del temporizador, navegación de pasos, host).
- `DESIGN.md` — registrar la captura aceptada `cooking-mode`: `modo-cocina`.
- `docs/build-brief.md` — anotar que "Modo cocina con temporizadores reales" pasa a incluirse (issue #76); sin reescribir el resto.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`, `data/**`, la
Taxonomía, `seed-recipes.json`, `ui/detail/**`, `ui/navigation/MainNavigationState.kt`,
`ui/preferences/ServingsStepper.kt`, Room/Perfil ni `init.sh`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/concepts/cooking-mode/modo-cocina/` (dirección visual) +
  `docs/design/concepts/cooking-mode/stitch-prompt.md`.
- Screens or states affected: Modo cocina (paso actual con progreso, tarjeta del paso, píldora miel de
  duración), los **cuatro estados del temporizador** (Inactivo · En marcha · Pausado · Terminado), paso
  marcado (tachado/atenuado), paso **sin** duración (bloque oculto), último paso ("Terminar") y las
  variantes claro/oscuro.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing`, `SazonShapes`, `SazonPillShape`,
  `SazonComponents.buttonHeight`, `MaterialTheme.colorScheme.{primary, onPrimary, tertiaryContainer,
  onTertiaryContainer, secondaryContainer, onSecondaryContainer, surface, onSurface, onSurfaceVariant,
  outlineVariant}`; cifras tabulares (`"tnum"`) en progreso, cuenta atrás y número de paso; objetivos
  ≥48dp; **una** acción principal terracota por pantalla; la duración/aviso usan miel/oliva, **nunca**
  `error`.
- **Capturas de referencia (obligatorias, ya presentes).** Pantalla única en
  `docs/design/concepts/cooking-mode/modo-cocina/`:
  - `docs/design/concepts/cooking-mode/modo-cocina/screen.png` (export de la pantalla + matriz de estados),
  - `docs/design/concepts/cooking-mode/modo-cocina/code.html` (código del export),
  - `docs/design/concepts/cooking-mode/modo-cocina/DESIGN.md` (notas del sistema del export).
  - Petición/ficha: `docs/design/concepts/cooking-mode/README.md`; prompt: `…/stitch-prompt.md`.
  Stitch no generó variante por cada estado del temporizador por separado: los cuatro estados se
  implementan a partir de esta misma dirección. Ante cualquier contradicción, manda `DESIGN.md`. Un
  defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar `ui.cooking` (máquina del temporizador, navegación
  de pasos, host overlay en `MainShell`; el aviso es en pantalla, sin notificación de sistema).
- `docs/domain-model.md`: **not needed** — no hay entidad, relación ni estado de dominio nuevos; el modo
  cocina es presentación sobre `Recipe`/`RecipeStep`.
- `docs/build-brief.md`: **update** — anotar la ampliación del MVP (issue #76): el modo cocina con
  temporizadores deja de ser Non-Goal; los Non-Goals de notificaciones/recordatorios se mantienen.
- `DESIGN.md` y `docs/design/concepts/`: **update** — registrar la captura aceptada `modo-cocina` de
  `cooking-mode`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear `CookingTimerState`, `CookingModeState` y `CookingModeFormat` (puros, sin Compose/Android).
2. Escribir sus tests JVM con los vectores dorados (escenarios 2–5) y casos negativos/límite.
3. Crear `CookingModeScreen` (cabecera+progreso, tarjeta del paso, temporizador por estados, marcado,
   barra "Anterior"/"Siguiente"-"Terminar", `Saver`, `LaunchedEffect` de reloj).
4. Hospedar el modo cocina como overlay en `MainShell` y cablear `onStartCooking`.
5. Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`, `DESIGN.md` y
   `docs/build-brief.md`.

## Implementation Tasks

- [ ] Crear `CookingTimerState` (`of`, `start`, `pause`, `resume`, `reset`, `tick`, `hasDuration`).
- [ ] Escribir `CookingTimerStateTest`: `of(10)`/`of(0)`, 10 ticks de 1000 → `600000→590000`, 600 ticks → `FINISHED`/`0`, overshoot, pausa congela, reanudar continúa, `reset` → IDLE, `tick` no-RUNNING y `delta<=0` no-op.
- [ ] Crear `CookingModeState` (`initial`, `next`, `previous`, `toggleCompleted`, `withTimer`, `isFirstStep`/`isLastStep`).
- [ ] Escribir `CookingModeStateTest`: arranque en paso 1 con timer del paso 1; `next`/`previous` con reinicio de timer; no-op fuera de rango; marcado idempotente e independiente; receta sin pasos sin crash; inmutabilidad.
- [ ] Crear `CookingModeFormat` (`progressLabel`, `progressFraction`, `durationLabel`, `countdown`, `FINISHED_NOTICE`).
- [ ] Escribir `CookingModeFormatTest`: `"Paso 2 de 5"`, `0.4f`, `total=0 → 0f`, `"10 min"`, `"10:00"`/`"09:42"`/`"00:00"`, negativo → `"00:00"`.
- [ ] Crear `CookingModeScreen` reutilizando tokens y cifras tabulares; temporizador con los 4 estados; marcado con tachado/atenuado; `CookingModeStateSaver`; `LaunchedEffect` que solo alimenta `tick(1000)`.
- [ ] Hospedar `CookingModeScreen` como overlay en `MainShell` y cablear `onStartCooking`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`, `DESIGN.md` y `docs/build-brief.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: suites nuevas en verde sin romper las existentes:
  - `CookingTimerStateTest` (**verificación exigida**): cuenta atrás determinista, pausa/reanudación,
    terminado al llegar a cero, `reset`, no-ops.
  - `CookingModeStateTest` (**verificación exigida**): navegación Siguiente/Anterior con reinicio del
    temporizador, primer/último paso, marcado del paso (`completedOrders`), receta sin pasos.
  - `CookingModeFormatTest`: `mm:ss` (`10:00`/`09:42`/`00:00`), `"Paso 2 de 5"`, fracción de progreso,
    `durationLabel`.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `bash init.sh` → **esperado**: código 0, sin procesos largos; `init.sh` **no cambia**.
- Inspección: `ui.cooking` puro sin imports Compose/Android en `CookingTimerState`/`CookingModeState`/
  `CookingModeFormat`; sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`,
  `data/**`, la Taxonomía, `seed-recipes.json`, `ui/detail/**` (la costura se cablea desde `MainShell`);
  sin hex sueltos ni iconos externos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - "Empezar a cocinar" abre el modo; se recorren los pasos con "Siguiente"/"Anterior"; en el último,
    "Terminar" vuelve al Detalle.
  - "Iniciar" cuenta la cuenta atrás; "Pausar"/"Reanudar" funcionan; al llegar a cero aparece "¡Listo!"
    (miel/oliva, nunca rojo) y **no** sigue contando.
  - Un paso sin duración oculta el bloque; marcar un paso lo tacha/atenúa; el acabado se compara con
    `docs/design/concepts/cooking-mode/modo-cocina/`.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; la lógica de estado y los formatos se cubren en JVM y el acabado lo revisa la persona.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`), no
arranca procesos largos ni necesita modos nuevos.

## Evidence To Capture

- Salida de `./gradlew test` con `CookingTimerStateTest`, `CookingModeStateTest` y
  `CookingModeFormatTest`, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `ui.cooking` puro sin imports Compose/Android; `MainShell` hospeda el modo; `ui/detail/**`,
  el build, el dominio, `data/**`, la Taxonomía y `seed-recipes.json` intactos.
- Resultado manual en emulador (descripción; captura opcional) de la navegación de pasos y del
  temporizador (cuenta atrás + "¡Listo!"), comparado con la captura `modo-cocina`.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Reloj real fuera de la lógica pura**: el tiempo entra por `tick(deltaMillis)` explícito; la lógica
  es determinista y testeable en JVM y la capa Compose solo alimenta `tick(1000)` (mismo criterio
  "puro + Compose delgado" que `ui.detail`/`ui.results`).
- **El temporizador está ligado al paso actual**: cambiar de paso lo reinicia a Inactivo con la duración
  del paso nuevo; el marcado (`completedOrders`) **persiste** entre pasos. Simple y consistente con
  "temporizador por paso".
- **El "aviso al terminar" es la notificación visual en pantalla** ("¡Listo!"): notificaciones,
  sonido y vibración son Non-Goals del MVP (`docs/build-brief.md`); no se añaden permisos/dependencias.
- **Paso sin duración (`minutes <= 0`) oculta el bloque**: lo fija la captura y el `stitch-prompt`; `hasDuration` lo hace explícito y testeable.
- **Modo hospedado como overlay en `MainShell`** (no un `PushedScreen` nuevo): se abre **encima** del
  Detalle sin desmontarlo y sin librería de navegación, reutilizando el patrón ya aceptado del Detalle.
- **Sin cambios en la firma de `RecipeDetailScreen`**: `onStartCooking` ya existe como costura; esta
  feature la cablea, cumpliendo lo que `recipe-detail-screen` delegó explícitamente.
- **Sin persistencia y sin dependencias nuevas**; `init.sh` sin cambios.

### Unknowns

- **Rotación / process death con el modo abierto**: `cookingRecipe` vive en `remember` (`Recipe` no es
  serializable), así que rotar cierra el modo (como el Detalle y el `result` de Resultados). El modo
  sobrevive a recomposiciones con `rememberSaveable`, pero no a la rotación. No se resuelve aquí.
- **Precisión del reloj**: `delay(1000)` acumula deriva bajo carga; suficiente para el MVP. Si molesta,
  migrar a `withFrameNanos`/tiempo real es un cambio local en la capa Compose, sin tocar la lógica pura.
- **Microcopy fina de los controles del temporizador** (p. ej. el control de reinicio en Terminado): la
  etiqueta exacta sigue la captura; afinarla es detalle visual.
- **Repaso visual** de la tarjeta del paso, la píldora miel, el contraste claro/oscuro y la animación de
  pulso: queda para la persona tras el merge; un defecto visual se corrige con feature correctiva.

## Validator Checklist

- [ ] `CookingTimerState` es puro y determinista: cuenta atrás, pausa/reanudación, `FINISHED` al llegar a
      cero (sin seguir contando), `reset` y no-ops (escenarios 3 y 4, **verificación exigida**).
- [ ] `CookingModeState` cubre paso actual, "Siguiente"/"Anterior" con reinicio del temporizador,
      primer/último paso ("Terminar"), marcado por `RecipeStep.order` y receta sin pasos sin crash
      (escenarios 2, 5 y 7, **verificación exigida**).
- [ ] "Empezar a cocinar" del Detalle **abre** el modo (costura cableada en `MainShell`) y "Atrás"/"Terminar"
      vuelven al Detalle intacto (escenario 1).
- [ ] Un paso con `minutes <= 0` **oculta** el bloque del temporizador (escenario 6).
- [ ] `CookingModeFormat` produce `mm:ss` y `"Paso X de N"` con casos límite (`total=0`, negativo).
- [ ] `ui.cooking` puro está cubierto en JVM con casos negativos; el tiempo solo entra por `tick`.
- [ ] `domain/**`, `data/**`, la Taxonomía, `seed-recipes.json`, `ui/detail/**`, el build y `init.sh`
      quedan intactos; sin dependencias nuevas, iconos ni imágenes externas.
- [ ] Para UI: `Visual Design Impact` nombra las capturas de
      `docs/design/concepts/cooking-mode/modo-cocina/` (`screen.png`, `code.html`, `DESIGN.md`), que
      **existen** antes de implementar, y la implementación las sigue.
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `docs/technical-discovery.md`,
      `DESIGN.md` y `docs/build-brief.md` quedan coherentes.
