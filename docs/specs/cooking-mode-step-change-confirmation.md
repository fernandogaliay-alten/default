# Feature Implementation Spec: Confirmación al cambiar de paso en el modo cocina

## Source Feature

- `id`: `cooking-mode-step-change-confirmation`
- `area`: `ui`
- `depends_on`: `["cooking-mode", "cooking-mode-exit-confirmation"]` (**ambas `accepted`**: `cooking-mode`
  sesión 078; `cooking-mode-exit-confirmation` sesión 139, `docs/validations/cooking-mode-exit-confirmation.md`,
  ya en `main` en el commit `7c15642`).
- `status`: `not_started` a la hora de planificar (`feature_list.json`, líneas ~1780-1794).
- `source`: `feature_list.json` (mejora pedida por la persona, 2026-10-16).
- Verificación exigida: `bash init.sh` en verde sin dependencias nuevas; **test JVM de la decisión pura de
  confirmar el cambio de paso** (patrón `CookingExitPolicy`/`CookingAlarmPolicy`, estado inyectado) que pida
  confirmación **solo** con el temporizador del paso actual **En marcha o Pausado** y **no** con
  Inactivo/Terminado, con un paso sin duración, en el primer paso ("Anterior" deshabilitado) ni en el último
  ("Terminar" no cambia de paso); **prueba manual** de la persona en emulador (no automatizable según
  `AGENTS.md`).
- Comparte `CookingModeScreen.kt` con `cooking-mode-exit-confirmation` (que ya introdujo la decisión pura, el
  `AlertDialog` de Material 3, el microcopy en constante pura y el `BackHandler`). Esta spec **reutiliza ese
  patrón**; la confirmación de salida **no** cambia.

## Goal

Evitar que un toque accidental en "Siguiente"/"Anterior" tire el tiempo que el usuario lleva cocinando: hoy
`CookingModeState.next()`/`previous()` **reemplazan** el temporizador por el del paso nuevo (Inactivo), de
modo que un temporizador **En marcha o Pausado** se pierde sin aviso (el **marcado** `completedOrders` no se
pierde: persiste entre pasos).

Con esta feature, si el temporizador del paso actual está **En marcha o Pausado**, cambiar de paso pide
confirmación; **confirmar ejecuta `next()`/`previous()` como hoy** (mismo reinicio del temporizador, regla
aceptada intacta) y **cancelar no cambia nada** (mismo paso, mismo tiempo, con la cuenta atrás corriendo
detrás del diálogo). En el resto de casos el cambio de paso es **directo**, sin fricción.

La pieza central es una **decisión pura y testeable en JVM** en `com.sazon.app.ui.cooking`
(`CookingStepChangePolicy` con el estado inyectado, más el microcopy en constante pura
`CookingStepChangeCopy`); la capa Compose es delgada: gatear los dos botones de la barra inferior y añadir un
`AlertDialog` de Material 3 con los roles de `SazonTheme`. "Terminar" (último paso) **ya** está cubierto por
la confirmación de salida aceptada y **no** se duplica aquí.

## Non-Goals

- **No** se cambia la regla aceptada de que **el temporizador se reinicia al cambiar de paso** ni se añade
  estado al dominio para "recordar" temporizadores por paso.
- **No** se toca `CookingTimerState`, `CookingModeState`, `CookingModeFormat`, `CookingExitConfirmation.kt`
  (se **usa** como referencia), `CookingAlarm*`, `CookingSession*`/`CookingSessionStore`,
  `NotificationPermission*`, `data.cooking/**`, el manifiesto ni `res/**`.
- **No** se toca la **confirmación de salida** ("Atrás"/"Terminar"/gesto atrás): el `BackHandler` solo gana
  una rama para el diálogo nuevo.
- **No** entra: `snackbar` con "Deshacer", pausar la cuenta atrás con el diálogo abierto, re-evaluar la
  decisión si el temporizador termina con el diálogo abierto, ni persistir el diálogo pendiente.
- **No** se crean capturas ni `docs/design/concepts/cooking-mode-step-change-confirmation/`.
- **No** se refactoriza el `AlertDialog` de salida para extraer un diálogo genérico compartido.
- **No** se añaden dependencias, ni tokens/colores/tipografías/formas nuevas, ni iconos.
- **No** se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`,
  `data/**`, `ui/theme/**`, `ui/MainShell.kt` ni la Taxonomía/`seed-recipes.json`.

## Job Story

Cuando estoy cocinando con el temporizador de un paso en marcha, quiero que la app me avise antes de pasar al
siguiente paso (o volver al anterior),
para no perder el tiempo que llevo contando por un toque accidental sin renunciar a la navegación.

## Users And Permissions

- **Usuario local**: único actor. Con el temporizador En marcha/Pausado elige "Cambiar de paso" (cambia; el
  temporizador del paso nuevo queda Inactivo) o "Seguir en este paso" (no cambia nada). En el resto de casos
  cambia de paso sin diálogo.
- **Desarrollador / agente**: cubre en JVM la decisión pura (matriz de estados e índices × las dos
  direcciones) con casos negativos; el cableado Compose (gate de los botones, `AlertDialog`, rama del
  `BackHandler`) se verifica por inspección, coherente con `AGENTS.md` (no se automatiza UI de emulador).
- **Sistema**: sin permisos, alarmas ni notificaciones nuevas; la cancelación de alarma/servicio al confirmar
  la ejecutan **los efectos aceptados que ya existen** (no esta feature).

## Acceptance Scenarios

### Scenario 1: "Siguiente" con el temporizador En marcha (verificación exigida)

Given el Modo cocina en un paso intermedio con el temporizador **En marcha**,
When el usuario pulsa "Siguiente",
Then **no** cambia de paso: aparece el diálogo de confirmación (`CookingStepChangeCopy`), con "Cambiar de
paso" como única acción principal y "Seguir en este paso" como descarte.

### Scenario 2: "Anterior" con el temporizador Pausado (verificación exigida)

Given el temporizador del paso actual en **Pausado** (tiempo congelado),
When el usuario pulsa "Anterior",
Then aparece el mismo diálogo (misma decisión en las dos direcciones, con aserción explícita en JVM).

### Scenario 3: confirmar cambia de paso como hoy

Given el diálogo visible por "Siguiente",
When el usuario pulsa "Cambiar de paso",
Then se ejecuta la navegación **actual sin cambios** (`state.next(recipe.steps)`; para "Anterior",
`state.previous(recipe.steps)`): avanza el paso, el progreso cambia y el temporizador queda **Inactivo con la
duración del paso nuevo**; el marcado persiste y el paso marcado sigue tachado/atenuado.

### Scenario 4: cancelar no cambia nada

Given el diálogo visible con el temporizador En marcha,
When el usuario pulsa "Seguir en este paso" (o descarta el diálogo),
Then se cierra el diálogo y **nada** cambia: mismo paso, mismo tiempo restante, mismos `completedOrders`,
misma sesión persistida y alarma/servicio del temporizador intactos. La cuenta atrás **sigue corriendo**
detrás del diálogo.

### Scenario 5: sin temporizador en curso, el cambio es directo

Given el Modo cocina con el temporizador **Inactivo** (incluido un paso **sin duración**, donde el bloque del
temporizador ni se pinta) o **Terminado**,
When el usuario pulsa "Siguiente" o "Anterior",
Then **no** aparece diálogo: cambia de paso directamente, como hoy. Con `completedOrders` no vacío pero el
temporizador Inactivo/Terminado la decisión sigue siendo `false` (el marcado **no** se pierde al cambiar de
paso: esa es la diferencia con la confirmación de salida).

### Scenario 6: primer y último paso no meten fricción

Given el Modo cocina con el temporizador En marcha en el **primer** paso,
When el usuario mira la barra inferior,
Then "Anterior" está **deshabilitado** (no hay cambio al que confirmar: `PREVIOUS` es `false`) y "Siguiente"
**sí** pide confirmación. En el **último** paso la acción primaria lee "Terminar" y va por la confirmación de
**salida** aceptada (`NEXT` es `false`: `next()` es no-op allí).

### Scenario 7: atrás del sistema con el diálogo de cambio de paso abierto

Given el diálogo de confirmación de cambio de paso visible,
When el usuario usa el gesto/tecla atrás del sistema,
Then el diálogo se **descarta** (equivale a cancelar: no cambia de paso, no sale del modo y no se reabre).

### Scenario 8: las dos confirmaciones conviven sin contradecirse

Given el Modo cocina con el temporizador En marcha,
When el usuario prueba las cuatro vías ("Atrás", "Terminar", "Siguiente", "Anterior"),
Then "Atrás"/"Terminar" piden la confirmación de **salida** (aceptada) y "Siguiente"/"Anterior" la de
**cambio de paso**: cada diálogo se abre por su gate, nunca los dos a la vez, y ninguno altera al otro.

## Repository Research

### Files Inspected

- `feature_list.json` (líneas ~1780-1794 y, como contrato del patrón, 1755-1779) — alcance: solo
  En marcha/Pausado; confirmar ejecuta `next()`/`previous()`; cancelar no cambia nada; sin tocar el reinicio
  aceptado ni el dominio.
- `PROGRESS.md` (sesión 139 y anteriores) — ambas dependencias `accepted`; `CookingExitConfirmation.kt` y el
  `BackHandler` ya están en `main`.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — **fichero a modificar**. Puntos
  exactos: estado y gates de salida (152-208), `BackHandler` (210-220), barra inferior cableada (264-276:
  `onPrevious`/`onNext` llaman a `previous`/`next` directos; `onClose = requestFinish`), diálogos `private`
  (330-406), `CookingBottomBar` (835-882: `enabled = isEmpty || !isFirstStep`; `primaryIsFinish = isEmpty ||
  isLastStep`), reloj (257-262) y efectos de persistencia/transición (178-247) que **no** se tocan.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingExitConfirmation.kt` — **patrón a imitar**: enum de
  punto + política pura + microcopy, 0 imports, sin reloj, solo `CookingModeState`.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeState.kt` y `CookingTimerState.kt` —
  `stepIndex`/`totalSteps`/`isFirstStep`/`isLastStep`/`isEmpty`, `completedOrders`, `next`/`previous` (no-op
  acotado + `timerFor`), `CookingTimerStatus` y `hasDuration`.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingExitPolicyTest.kt` — estilo a replicar (JUnit 4,
  nombres en español, matriz con aserción por punto, negativos que no pasan en vacío).
- `docs/specs/cooking-mode.md` (escenario 2 en 75-82; decisión 221; barra 258-259 y 410) — la regla aceptada
  **"cambiar de paso reinicia el temporizador a Inactivo"** que esta feature **no** cambia.
- `docs/technical-discovery.md` (375-417) — párrafos de `ui.cooking` y de la salida gateada, a ampliar.
- `DESIGN.md` (287-302 y regla de una sola acción principal en 345/683) — precedentes de **excepción de
  capturas** de la familia `cooking-*`, incluida `cooking-mode-exit-confirmation` (misma clase que esta).
- `gradle/libs.versions.toml` y dependencias ya usadas por el fichero — **sin** dependencias nuevas (Material
  3, `BackHandler` y Compose ya están).
- Baseline en `HEAD e5012d1`: `git grep -c '@Test' HEAD -- app/src/test` = **114 ficheros / 896
  anotaciones** (suites de `ui.cooking`: `CookingAlarmPolicyTest` 16, `CookingExitPolicyTest` 14,
  `CookingModeStateTest` 10, `CookingTimerStateTest` 11, `CookingSessionTest` 16,
  `NotificationPermissionPolicyTest` 14, `CookingModeFormatTest` 8).

### Existing Patterns To Follow

- **Lógica pura + Compose delgado**: el dominio de decisión en `ui.cooking` sin imports `android.*`/
  `androidx.*`, sin Compose y **sin leer el reloj**; la pantalla solo pregunta y reacciona.
- **Un fichero por concern con política + microcopy** (`NotificationPermission.kt`,
  `CookingExitConfirmation.kt`) ⇒ aquí un fichero nuevo `CookingStepChangeConfirmation.kt`.
- **Microcopy en constante pura** (`object ...Copy`), no en `strings.xml`.
- **Diálogo Compose `private`** con `AlertDialog` de Material 3 y **roles** de `SazonTheme`: una sola acción
  principal (`TextButton` en `colorScheme.primary`) y descarte; sin hex, tokens, formas ni `error`.
- **Acciones gateadas sobre el camino existente**: la salida ya hace `if (policy) abrir else ejecutar`; el
  cambio de paso se gatea igual y **confirmar** llama a `next()`/`previous()` sin reimplementarlos.
- **Sin dependencias nuevas**, `init.sh` sin cambios, tests JUnit 4 en español con negativos explícitos.

### Current Gaps

- `onPrevious`/`onNext` llaman a `previous`/`next` **directamente** (ninguna decisión previa): ahí se pierde
  el temporizador.
- No existe decisión, ni microcopy, ni diálogo de confirmación de cambio de paso; el `BackHandler` actual
  solo conoce el diálogo de salida.

## Technical Approach

### 1. Decisión pura — `app/src/main/java/com/sazon/app/ui/cooking/CookingStepChangeConfirmation.kt` (nuevo)

```kotlin
/** Dirección del cambio de paso que puede pedir confirmación. */
enum class CookingStepChangePoint { NEXT, PREVIOUS }

object CookingStepChangePolicy {
    fun requiresConfirmation(state: CookingModeState, point: CookingStepChangePoint): Boolean
}

object CookingStepChangeCopy {
    const val TITLE = "¿Cambiar de paso?"
    const val MESSAGE = "El temporizador de este paso se reiniciará con el del paso nuevo."
    const val CONFIRM = "Cambiar de paso"
    const val DISMISS = "Seguir en este paso"
}
```

- **Regla**: `true` **solo** si el temporizador del paso actual está **En marcha o Pausado** **y** esa
  dirección cambia realmente de paso:
  - `NEXT` ⇒ `!state.isEmpty && !state.isLastStep` (en el último paso la primaria lee "Terminar" y `next()`
    es no-op).
  - `PREVIOUS` ⇒ `!state.isEmpty && !state.isFirstStep` ("Anterior" está deshabilitado en el primer paso y
    `previous()` es no-op).
- **`completedOrders` no cuenta** (a diferencia de `CookingExitPolicy`): el marcado **persiste** entre pasos,
  así que no hay nada que perder por él. Es la diferencia clave entre las dos políticas y un caso negativo
  que el test debe fijar explícitamente.
- **`FINISHED` no cuenta**: su tiempo ya se consumió (coherente con la política de salida aceptada).
- Sin Compose/Android, sin reloj y sin `Recipe`: solo depende de `CookingModeState`; caso defensivo
  `isEmpty` ⇒ `false`.
- El enum de dirección hace que "Siguiente y Anterior piden lo mismo **cuando la dirección es posible**" sea
  un **invariante testeado** y evita el fallo típico de gatear solo uno de los dos botones.
- La guarda de dirección **no** cambia el comportamiento observable: la barra inferior ya garantiza que
  "Anterior" solo se pulsa si `!isFirstStep` y que en el último paso la primaria es "Terminar"; con ella
  "no-op nunca pide confirmación" pasa a ser una **invariante probada** en JVM en vez de un argumento de
  estructura de UI (y es lo que la verificación exige cubrir para primer/último paso).

### 2. Cableado Compose — `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` (modificar)

- Nuevo estado `var pendingStepChange by remember { mutableStateOf<CookingStepChangePoint?>(null) }` (enum y
  no lambda: estado estable para Compose; `remember`, como `confirmExit`, no `rememberSaveable`).
- Dos entradas gateadas sobre la navegación **existente** (`requestPrevious`, `requestNext`): cada una
  `if (CookingStepChangePolicy.requiresConfirmation(state, punto)) pendingStepChange = punto else state =
  state.previous(recipe.steps)` (o `next`). La barra inferior pasa a usarlas (`onPrevious = requestPrevious`,
  `onNext = requestNext`); **`CookingBottomBar` no cambia** y `onClose` sigue siendo `requestFinish` (salida).
- `if (pendingStepChange != null) CookingStepChangeDialog(onConfirm = { val punto = pendingStepChange;
  pendingStepChange = null; state = when (punto) { NEXT -> state.next(recipe.steps); PREVIOUS ->
  state.previous(recipe.steps) } }, onDismiss = { pendingStepChange = null })`: composable **`private`** junto
  a `CookingExitDialog`, `AlertDialog` de Material 3, textos desde `CookingStepChangeCopy`, **una** acción
  principal en `colorScheme.primary` y descarte, con la **misma** forma que el diálogo de salida (sin tokens,
  colores, tipografías ni formas nuevas; sin `colorScheme.error`). No se refactoriza el diálogo aceptado.
- **Rama nueva del `BackHandler`** existente: `when { pendingStepChange != null -> pendingStepChange = null;
  confirmExit -> confirmExit = false; else -> requestBack() }`. Con el diálogo de cambio de paso abierto el
  atrás lo **descarta**; nunca sale.
- **Invariante de "solo al confirmar"**: abrir/cerrar el diálogo **no** cambia `stepIndex`, `timer` ni
  `completedOrders`, **no** fija `exiting`, **no** llama a `session.clear()` ni a
  `CookingAlarm.dismissFinishNotification()` y **no** pausa el reloj. Confirmar se apoya en los efectos
  aceptados existentes (`LaunchedEffect(state.timer.status)` cancela alarma/servicio al pasar a Inactivo;
  `LaunchedEffect(state.stepIndex, ...)` persiste la sesión).
- **No** se toca el `LaunchedEffect` del reloj (`tick(1000)`) ni `CookingModeStateSaver` (`pendingStepChange`
  no se persiste: misma limitación aceptada que `confirmExit` al rotar).

### 3. Tests JVM — `app/src/test/java/com/sazon/app/ui/cooking/CookingStepChangePolicyTest.kt` (nuevo)

Matriz de estados × las **dos** direcciones (sobre un paso intermedio, para que ambas sean posibles), más los
casos de índice (primer/último paso, receta de un solo paso, receta sin pasos) y negativos que fallarían si
la política aprobara siempre o si copiara la condición de `CookingExitPolicy` (`completedOrders`). JUnit 4,
nombres en español, `CookingModeState`/`CookingTimerState` reales (mismo estilo que `CookingExitPolicyTest`).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingStepChangeConfirmation.kt` — `CookingStepChangePoint`,
  `CookingStepChangePolicy`, `CookingStepChangeCopy` (puros, 0 imports).
- `app/src/test/java/com/sazon/app/ui/cooking/CookingStepChangePolicyTest.kt` — la verificación exigida.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — `pendingStepChange`, entradas gateadas,
  rama del `BackHandler` y `CookingStepChangeDialog` (`private`); KDoc ampliada.
- `docs/technical-discovery.md` — párrafo de `ui.cooking` con el gate del cambio de paso.
- `DESIGN.md` — nota de **excepción de capturas** de esta feature.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`,
`data/**` (incluido `data/cooking/**`), `ui/theme/**`, `ui/MainShell.kt`, `CookingTimerState.kt`,
`CookingModeState.kt`, `CookingModeFormat.kt`, `CookingExitConfirmation.kt`, `CookingAlarm*.kt`,
`CookingSession*.kt`, `NotificationPermission*.kt`, `AndroidManifest.xml`, `res/**` ni la
Taxonomía/`seed-recipes.json`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (fuente de verdad visual) — **roles** de `SazonTheme` y sus reglas vigentes (una
  sola acción principal por superficie, descarte sin énfasis). No se inventan colores, tipografías, formas ni
  tokens nuevos; no se crea material en `docs/design/concepts/`.
- Screens or states affected: el **Modo cocina ya aceptado** con el **diálogo de confirmación de cambio de
  paso** visible, disparado desde "Siguiente"/"Anterior", sobre los estados ya aceptados del temporizador (En
  marcha y Pausado son los que lo disparan). Claro/oscuro por el tema.
- New design artifact required: **no** — excepción de capturas (misma clase que
  `cooking-mode-exit-confirmation` y `cooking-timer-alarm-permission-context`): un `AlertDialog` de Material 3
  **no es una pantalla diseñada**; reutiliza los roles del tema, no añade tokens y su microcopy vive en la
  constante pura `CookingStepChangeCopy`. El acabado en claro/oscuro lo revisa la persona en el emulador tras
  el merge; un defecto visual se corrige con una **feature correctiva**.
- Microcopy: **elegida por el planner** (`CookingStepChangeCopy`), porque `DESIGN.md` no cubre diálogos de
  confirmación; es **neutra respecto a la dirección** (vale para "Siguiente" y "Anterior") y **no** interpola
  el tiempo restante (sería un valor móvil mientras la cuenta atrás corre detrás). La persona puede afinarla
  tras el merge (defecto de texto ⇒ feature correctiva).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — añadir al párrafo de `ui.cooking` que el **cambio de paso**
  también pasa por una decisión pura (`CookingStepChangePolicy`: solo temporizador En marcha/Pausado y solo
  si la dirección cambia de paso, con `completedOrders` fuera por persistir el marcado), que el
  `AlertDialog`/microcopy siguen el patrón de la salida y que confirmar ejecuta `next()`/`previous()` sin
  tocar la regla aceptada de reinicio del temporizador.
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados de dominio nuevos (es
  presentación sobre `CookingModeState`, ya registrado; la regla de reinicio no cambia).
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de repo sin cambios.
- `DESIGN.md`: **update** — registrar la excepción de capturas (diálogo de confirmación no diseñado).
- `docs/build-brief.md`: **not needed** — no cambia el alcance del MVP ni ningún Non-Goal (pulido de UX del
  Modo cocina ya aceptado).
- `docs/adr/`: **not needed** — sin decisión arquitectónica nueva; se reutiliza el patrón registrado
  (`CookingExitPolicy`, ADR 0007/0008).
- `docs/specs/cooking-mode.md`: **not needed** — su regla de reinicio sigue vigente; esta spec es el contrato
  del gate, no una enmienda.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear `CookingStepChangeConfirmation.kt` (enum de dirección, política pura, microcopy), sin leer el reloj y
   sin importar nada.
2. Escribir `CookingStepChangePolicyTest` (matriz de estados, casos de índice y negativos, incluido "marcados
   + Inactivo ⇒ no confirma").
3. Modificar `CookingModeScreen.kt`: estado pendiente, entradas gateadas, rama del `BackHandler` y
   `CookingStepChangeDialog`; sin tocar los efectos aceptados (reloj, sesión, alarma) ni la salida.
4. Ejecutar `bash init.sh` en verde (test + lint) y actualizar `docs/technical-discovery.md` y `DESIGN.md`.

## Implementation Tasks

- [ ] Crear `CookingStepChangePoint` (`NEXT`, `PREVIOUS`) y `CookingStepChangePolicy.requiresConfirmation`
      (En marcha/Pausado **y** dirección que cambia de paso; `completedOrders` **no** cuenta).
- [ ] Crear `CookingStepChangeCopy` (título, mensaje, confirmar, descartar) como constantes puras.
- [ ] Escribir `CookingStepChangePolicyTest`: En marcha ⇒ `true`; Pausado ⇒ `true`; Inactivo ⇒ `false`;
      Terminado ⇒ `false`; Inactivo/Terminado **con marcados** ⇒ `false`; paso sin duración ⇒ `false`; primer
      paso (`PREVIOUS` `false`, `NEXT` `true` con En marcha); último paso (`NEXT` `false`, `PREVIOUS` `true`
      con En marcha); receta de un solo paso ⇒ `false` en las dos; receta sin pasos ⇒ `false` en las dos;
      ticks intermedios no cambian la decisión; determinismo y no mutación; microcopy estable.
- [ ] Añadir `pendingStepChange` y las entradas gateadas (`requestPrevious`, `requestNext`), cablearlas en
      `CookingBottomBar` **sin** cambiar el contrato de la navegación (`next`/`previous` aceptados).
- [ ] Añadir la rama de `pendingStepChange` al `BackHandler` **antes** de la de `confirmExit`.
- [ ] Añadir `CookingStepChangeDialog` (`private`, `AlertDialog` de Material 3, roles del tema): confirmar
      ejecuta `next()`/`previous()`; descartar solo cierra el diálogo.
- [ ] Ejecutar `bash init.sh` en verde y actualizar `docs/technical-discovery.md` y `DESIGN.md`.

## Verification Plan

- `bash init.sh` → **esperado**: código 0, test + lint, sin procesos largos; `init.sh` **no cambia**.
- `./gradlew test` (vía `bash init.sh`) → **esperado**: `CookingStepChangePolicyTest` en verde (la
  **verificación exigida**) sin romper las suites de `cooking` (`CookingExitPolicyTest` 14,
  `CookingAlarmPolicyTest` 16, `CookingModeStateTest` 10, `CookingTimerStateTest` 11, `CookingModeFormatTest`
  8, `CookingSessionTest` 16, `NotificationPermissionPolicyTest` 14 y las de `data.cooking`). Baseline a
  re-derivar con el método del repo (`git grep -c '@Test' HEAD -- app/src/test`): **114 ficheros / 896
  anotaciones** en `HEAD e5012d1`; delta esperado **+1 suite** (≈12-16 tests) y **0** suites existentes
  modificadas.
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y tirada focal
  `--tests "com.sazon.app.ui.cooking.*"` → **esperado**: BUILD SUCCESSFUL, 0 fallos; XML frescos.
- `bash scripts/gradle.sh lint --rerun-tasks` → **esperado**: 0 errores y sin warnings nuevos en ficheros de
  la feature (`app/src`) sobre el SARIF.
- Inspección: `CookingStepChangeConfirmation.kt` con **0** imports y sin `SystemClock`/`currentTimeMillis`;
  `BackHandler` sigue **solo** en `CookingModeScreen.kt`; el diálogo nuevo es `private` y no introduce hex ni
  tokens; `CookingExitConfirmation.kt` **sin diff**; sin cambios en build/lockfile/`init.sh`/`scripts/**`/
  `domain/**`/`data/**`/`ui/theme/**`/`ui/MainShell.kt`/estado-temporizador/`CookingAlarm*`/
  `CookingSession*`/`NotificationPermission*`/manifiesto/`res/**`; sin dependencias nuevas; sin
  `docs/design/concepts/cooking-mode-step-change-confirmation/`.
- Manual (emulador, lo revisa la persona; `AGENTS.md` no automatiza UI):
  - Con el temporizador En marcha, "Siguiente" y "Anterior" avisan de que el temporizador se reinicia.
  - "Seguir en este paso" mantiene el paso y su tiempo, con la cuenta atrás viva.
  - "Cambiar de paso" avanza/retrocede con el temporizador del paso nuevo en Inactivo y el marcado intacto.
  - Con el temporizador Inactivo/Terminado o un paso sin duración, el cambio es directo.
  - En el primer paso "Anterior" no ofrece nada; en el último la primaria "Terminar" pide la confirmación de
    **salida**; con el diálogo de cambio de paso abierto el atrás lo descarta.
  - Acabado del diálogo en claro y oscuro frente a las reglas de `DESIGN.md`.

Cobertura E2E: **no aplica**. El repo no tiene suite E2E persistente y `AGENTS.md` prohíbe automatizar UI de
emulador; la decisión se cubre en JVM y el cableado Compose se verifica por inspección y prueba manual.

## Evidence To Capture

- Salida de `./gradlew test` con `CookingStepChangePolicyTest` (suite, nº de tests, 0 fallos) y total de la
  suite (suites/tests, 0 fallos/errores/saltados); caso negativo explícito de que la política no aprueba
  siempre (p. ej. marcados + Inactivo ⇒ `false`).
- Baseline re-derivada y delta (`+1` suite, N tests nuevos, 0 suites existentes modificadas).
- Salida de `lint` (0 errores, sin warnings nuevos en `app/src`) y `bash init.sh` con código 0.
- Inspección: pureza de `CookingStepChangeConfirmation.kt`, `BackHandler` solo en `CookingModeScreen.kt`,
  `CookingExitConfirmation.kt` sin diff y `git diff --stat` limitado a los ficheros previstos.
- Resultado manual en emulador (descripción): "Siguiente"/"Anterior" con En marcha y Pausado, cancelar con la
  cuenta atrás viva, confirmar con reinicio del temporizador y los casos directos (Inactivo/Terminado/sin
  duración/primer/último paso).
- Anotación en `feature_list.json`/`PROGRESS.md` de la evidencia y del estado `passing` (al implementar, por
  el implementer; el planner no los toca).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Decisión pura con estado inyectado** (`CookingStepChangePolicy`): mismo patrón que las políticas
  aceptadas; testeable en JVM (verificación exigida).
- **Confirmar solo con el temporizador En marcha o Pausado**: es lo único que se pierde al cambiar de paso
  (`completedOrders` persiste) y evita fricción en pasos sin duración (bloque ni se pinta).
- **Dirección como contrato** (`CookingStepChangePoint`): un solo sitio donde una divergencia futura de los
  dos botones tendría que ser deliberada, y "Siguiente y Anterior piden lo mismo" como invariante testeado.
- **Guarda de "dirección que cambia de paso"** (`!isEmpty` + `!isFirstStep`/`!isLastStep`) además del estado
  del temporizador: hace que "no-op nunca pide confirmación" sea testeable en JVM (la verificación exige
  cubrir primer y último paso) aunque la barra inferior ya lo garantice por estructura; no cambia el
  comportamiento observable.
- **Confirmar ejecuta `next()`/`previous()` sin reimplementarlos**: la regla aceptada de reinicio del
  temporizador al paso nuevo (y la persistencia del marcado) queda intacta.
- **Diálogo de Material 3 con roles del tema, acción principal en `primary`** (`error` no se usa: `DESIGN.md`
  no define estilo destructivo), misma forma que el diálogo de salida aceptado; no se refactoriza aquel.
- **El atrás con el diálogo abierto lo descarta**: se amplía el `BackHandler` aceptado con una rama, sin
  cambiar su semántica de salida.
- **Excepción de capturas**: un `AlertDialog` no es pantalla diseñada; se documenta en `DESIGN.md`.
- **Sin dependencias nuevas**: Material 3 y `BackHandler` ya están disponibles.

### Unknowns

- **Microcopy**: `DESIGN.md` no cubre confirmaciones; el texto de `CookingStepChangeCopy` es decisión del
  planner y la persona puede afinarlo tras el merge (defecto ⇒ feature correctiva).
- **Temporizador que llega a cero con el diálogo abierto**: la decisión se evaluó al pulsar, así que el
  diálogo permanece y confirmar cambia de paso (sin perder nada, ya Terminado). Misma limitación aceptada de
  la confirmación de salida; no se re-evalúa aquí.
- **Rotación con el diálogo abierto**: `pendingStepChange` vive en `remember` y se pierde al rotar; misma
  limitación aceptada del modo (`confirmExit`, `cookingRecipe`).
- **Descarte sin énfasis**: el `dismissButton` hereda el color por defecto de `TextButton` (`primary`), como
  en el diálogo de salida aceptado (hallazgo Low informativo del validador). Se mantiene la homogeneidad y
  **no** se corrige aquí; si la persona lo ve como defecto visual, feature correctiva.

## Validator Checklist

- [ ] `CookingStepChangePolicy` es **pura** (sin `android.*`/`androidx.*`, sin leer el reloj) y pide
      confirmación **solo** con el temporizador En marcha o Pausado (escenarios 1 y 2, **verificación
      exigida**).
- [ ] **No** pide confirmación con el temporizador Inactivo/Terminado (tampoco con pasos marcados), con un
      paso sin duración, en el primer paso para `PREVIOUS`, en el último para `NEXT`, con receta de un solo
      paso ni sin pasos, con negativos que no pasan en vacío (escenarios 5 y 6, **verificación exigida**).
- [ ] La decisión cubre **las dos direcciones** ("Siguiente" y "Anterior") con aserción explícita en el test
      JVM (**verificación exigida**).
- [ ] Confirmar ejecuta la navegación **aceptada** (`next()`/`previous()`, temporizador del paso nuevo
      Inactivo, marcado persistente) y cancelar **no** cambia paso, tiempo, marcado, sesión, alarma ni reloj,
      con la cuenta atrás viva detrás del diálogo (escenarios 3 y 4).
- [ ] El `BackHandler` **descarta** el diálogo de cambio de paso (no sale del modo) y la confirmación de
      salida aceptada sigue intacta (escenarios 7 y 8).
- [ ] `CookingExitConfirmation.kt`, `CookingTimerState`/`CookingModeState`/`CookingModeFormat`,
      `CookingAlarm*`/`CookingSession*`/`NotificationPermission*`, `domain/**`, `data/**`, `ui/theme/**`,
      `ui/MainShell.kt`, el manifiesto, `res/**`, el build/`lockfile` e `init.sh` quedan intactos; sin
      dependencias nuevas ni capturas.
- [ ] Para UI: `Visual Design Impact` identifica `DESIGN.md` como fuente de verdad, el diálogo usa sus
      **roles** sin tokens nuevos y la nota de **excepción de capturas** queda registrada en `DESIGN.md`.
- [ ] `bash init.sh` en verde y evidencia registrada en `feature_list.json`/`PROGRESS.md`, con
      `docs/technical-discovery.md` y `DESIGN.md` coherentes.
