# Feature Implementation Spec: Confirmación al salir del modo cocina

## Source Feature

- `id`: `cooking-mode-exit-confirmation`
- `area`: `ui`
- `depends_on`: `["cooking-mode", "cooking-timer-foreground-service"]` (**ambas `accepted`**, verificado
  en `feature_list.json` y `PROGRESS.md`: sesión 078 y sesión 137).
- `status`: `not_started` a la hora de planificar (entrada `feature_list.json`, líneas ~1745-1757).
- `source`: `feature_list.json` (mejora pedida por la persona, 2026-10-16).
- Verificación exigida por la entrada: `bash init.sh` en verde sin dependencias nuevas; **test JVM de
  la decisión pura de confirmar la salida** (patrón `CookingAlarmPolicy`/`NotificationPermissionPolicy`,
  estado inyectado, cubriendo **los dos puntos de salida** "Atrás" y "Terminar"); **prueba manual** de la
  persona en emulador (no automatizable según `AGENTS.md`).
- Ambas dependencias ya existen en disco: `CookingModeScreen` con su salida actual
  (`exiting` → `session.clear()` → `onClose()`), `CookingModeState`, `CookingAlarm`/`CookingAlarmPolicy`,
  `NotificationPermissionPolicy` y `CookingSessionStore`.
- **Comparte `CookingModeScreen.kt`** con `cooking-mode-step-change-confirmation` (que depende de esta y
  reutilizará la decisión, el `AlertDialog` y el microcopy aquí introducidos). Esta spec **no** incluye
  la confirmación de cambio de paso.

## Goal

Cerrar la última vía de pérdida accidental de la sesión del Modo cocina: hoy "Atrás" (cabecera),
"Terminar" (primario del último paso) y el **gesto atrás del sistema** salen de un solo toque y
descartan paso, temporizador y pasos marcados. Esta feature **intercepta las tres vías** y, solo cuando
hay algo que perder (temporizador del paso actual **En marcha o Pausado** o `completedOrders` no vacío),
muestra un diálogo de confirmación; si el usuario confirma, sale y descarta la sesión como hoy; si no,
se queda en el modo **sin cambios**. Sin nada que perder, se sale directamente.

La pieza central es una **decisión pura y testeable en JVM** en `com.sazon.app.ui.cooking`
(`CookingExitPolicy` con el estado inyectado, más el microcopy en constante pura `CookingExitCopy`), y
la capa Compose es delgada: un `BackHandler` (hoy la app **no** intercepta el atrás en ninguna pantalla)
y un `AlertDialog` de Material 3 con los roles de `SazonTheme`, siguiendo el patrón ya aceptado del
rationale del permiso (`NotificationPermissionRationale`). No se toca la máquina del temporizador
(`CookingTimerState`), el estado del modo (`CookingModeState`) ni sus formatos.

## Non-Goals

- **No** se cambia el microcopy ni el comportamiento de los botones de salida ("Atrás"/"Terminar"): solo
  se **gatean**.
- **No** se pausa la cuenta atrás mientras el diálogo está abierto: el reloj sigue corriendo detrás.
- **No** se intercepta el atrás fuera del Modo cocina: Detalle, Home, Generador, Favoritos y Perfil
  conservan su comportamiento actual (la app no gana una pila de navegación global).
- **No** se toca `CookingTimerState`, `CookingModeState`, `CookingModeFormat`, `CookingAlarm`/
  `CookingAlarmPolicy`, `CookingSession`/`CookingSessionStore`, `NotificationPermission*`, el
  manifiesto, los permisos, las notificaciones ni `data.cooking/**`.
- **No** entra la confirmación al **cambiar de paso** (es `cooking-mode-step-change-confirmation`), ni
  "guardar y salir", ni `snackbar` con "Deshacer", ni persistir la decisión de salida.
- **No** se toca la corrección `cooking-timer-finish-notification` (entrada vecina, sigue `not_started`).
- **No** se añaden dependencias (el `BackHandler` ya está disponible: `androidx.activity:activity-compose`
  1.8.0 es dependencia directa), ni iconos, ni tokens/colores nuevos, ni capturas nuevas.
- **No** se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
  `domain/**`, `data/**`, `ui/theme/**`, `ui/MainShell.kt` ni la Taxonomía/`seed-recipes.json`.

## Job Story

Cuando estoy cocinando con las manos ocupadas y pulso "Atrás"/"Terminar" (o el gesto atrás sin querer),
quiero que la app me avise si voy a perder el temporizador o los pasos ya marcados,
para no descartar toda la sesión por un toque accidental.

## Users And Permissions

- **Usuario local**: único actor. Con algo que perder, elige "Salir" (descarta) o "Seguir cocinando"
  (no cambia nada). Sin nada que perder, sale sin diálogo.
- **Desarrollador / agente**: cubre en JVM la decisión pura (matriz de estados × los dos puntos de
  salida) con casos negativos; el cableado Compose (`BackHandler`, `AlertDialog`) se verifica por
  inspección, coherente con `AGENTS.md` (no se automatiza UI de emulador).
- **Sistema**: el gesto/tecla atrás se despacha a `BackHandler` mientras el Modo cocina está compuesto;
  el `AlertDialog` vive en su propia ventana y consume el atrás como "descartar".

## Acceptance Scenarios

### Scenario 1: "Atrás" con el temporizador En marcha (verificación exigida)

Given el Modo cocina abierto en un paso con el temporizador **En marcha**,
When el usuario pulsa "Atrás" en la cabecera,
Then **no** se sale: aparece el diálogo de confirmación (`CookingExitCopy`), con "Salir" como única
acción principal y "Seguir cocinando" como descarte.

### Scenario 2: "Terminar" con pasos marcados (verificación exigida)

Given el Modo cocina en el **último** paso con el temporizador **Inactivo** y al menos un paso en
`completedOrders`,
When el usuario pulsa "Terminar",
Then aparece el mismo diálogo (misma decisión que "Atrás": `CookingExitPolicy` da `true` para los dos
puntos de salida).

### Scenario 3: gesto atrás del sistema

Given el Modo cocina abierto con algo que perder,
When el usuario usa el gesto/tecla atrás del sistema,
Then el atrás lo intercepta `BackHandler` y se comporta igual que "Atrás" (diálogo), **sin** cerrar la
actividad ni la app.

### Scenario 4: confirmar sale y descarta la sesión

Given el diálogo visible,
When el usuario pulsa "Salir",
Then se ejecuta la **salida actual sin cambios**: se borra la sesión persistida
(`CookingSessionStore.clear()`), se cierra el modo (`onClose()` → vuelve al Detalle intacto) y el
desmontaje aplica `CookingAlarmPolicy.onExit()` (alarma/servicio cancelados).

### Scenario 5: cancelar no cambia nada

Given el diálogo visible con el temporizador En marcha,
When el usuario pulsa "Seguir cocinando" (o descarta el diálogo),
Then se cierra el diálogo y **nada** cambia: mismo paso, mismo tiempo restante, mismos
`completedOrders`, la sesión persistida sigue y la alarma/servicio siguen programados. La cuenta atrás
**sigue corriendo** detrás del diálogo.

### Scenario 6: nada que perder → salida directa

Given el Modo cocina con el temporizador **Inactivo o Terminado** y `completedOrders` **vacío**,
When el usuario pulsa "Atrás", "Terminar" o el gesto atrás,
Then **no** aparece diálogo: se sale directamente (mismo camino que hoy).

### Scenario 7: el temporizador Pausado también cuenta

Given el temporizador en **Pausado** (y sin pasos marcados),
When el usuario intenta salir por cualquiera de las tres vías,
Then pide confirmación (Pausado tiene tiempo que perder).

### Scenario 8: atrás con el diálogo abierto

Given el diálogo de confirmación visible,
When el usuario usa el gesto/tecla atrás,
Then el diálogo se **descarta** (equivale a cancelar): no se sale, no se reabre y el estado queda
intacto.

## Repository Research

### Files Inspected

- `feature_list.json` (líneas ~1745-1757) — entrada de la feature: alcance, verificación y decisiones
  (confirmar solo con algo que perder; también "Terminar"; `BackHandler`; cancelar alarma/servicio solo
  al confirmar; el diálogo no pausa la cuenta atrás). La entrada vecina
  `cooking-timer-finish-notification` (líneas ~1726-1742) queda **fuera** de alcance.
- `PROGRESS.md` — sesiones 131-137: estado real del Modo cocina, el aviso de sistema, el permiso en
  contexto y el servicio en primer plano (ambas dependencias `accepted`).
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — cuerpo principal (líneas 117-283):
  `state` con `rememberSaveable`, `exiting` + `LaunchedEffect(exiting)` que hace
  `session.clear(); onClose()` (166-174), `requestExit` (174), `DisposableEffect { onDispose { alarm.apply(onExit()) } }`
  (203-209), reloj `LaunchedEffect` con `tick(1000)` (211-216), cableado `onBack = requestExit` y
  `onClose = requestExit` (228, 241); diálogo del rationale (286-315); barra inferior (746-792:
  "Anterior"/"Siguiente"→"Terminar"). **Punto exacto a modificar.**
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeState.kt` — estado puro: `stepIndex`,
  `totalSteps`, `completedOrders: Set<Int>`, `timer`; `isEmpty`; sin la `Recipe`.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingTimerState.kt` — `CookingTimerStatus`
  (`IDLE`/`RUNNING`/`PAUSED`/`FINISHED`), `isRunning`, `isFinished`, `hasDuration`.
- `app/src/main/java/com/sazon/app/ui/cooking/NotificationPermission.kt` — **patrón a imitar**: un solo
  fichero con estado inyectado + comando + `object` de microcopy (`NotificationPermissionCopy`) +
  política pura (`onTimerChange`), sin leer el reloj.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingAlarm.kt` — patrón de política pura + costura +
  default no-op.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingSessionStore.kt` — `load`/`save`/`clear`, contrato
  "no lanza".
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` (líneas 154-186, 344-386) — el Modo cocina se hospeda
  como overlay (`var cookingRecipe by remember`) y `onClose` hace `cookingRecipe = null`. **No necesita
  cambios**: el gate vive dentro de `CookingModeScreen`.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingAlarmPolicyTest.kt` — estilo de test JUnit 4 con
  nombres en español y vectores/negativos explícitos.
- `gradle/libs.versions.toml` — `activityCompose = "1.8.0"` y `androidx-activity-compose` como
  dependencia directa ⇒ `androidx.activity.compose.BackHandler` está disponible **sin** dependencias
  nuevas (confirmado por `MainActivity` usando `androidx.activity.compose.setContent`).
- `docs/technical-discovery.md` (líneas 375-401) — párrafo de `ui.cooking` a reconciliar.
- `DESIGN.md` (líneas 287-300) — precedentes de **excepción de capturas** de la familia `cooking-*`
  (notificaciones y rationale de permiso) y línea 135 (convención de capturas).
- `docs/specs/cooking-mode.md` y `docs/specs/cooking-timer-foreground-service.md` — contratos aceptados
  de la pantalla, la sesión persistida y la salida explícita.

### Existing Patterns To Follow

- **Lógica pura + Compose delgado**: `ui.cooking` sin imports `android.*`/`androidx.*` salvo la capa
  Compose; política con estado inyectado y **sin leer el reloj**.
- **Un fichero por concern con política + microcopy** (`NotificationPermission.kt`).
- **Diálogo Compose `private`** junto al rationale (`NotificationPermissionRationale`), con `AlertDialog`
  de Material 3 y **roles** de `SazonTheme` (nada de hex ni tokens nuevos).
- **Salida centralizada**: un único camino `exiting → session.clear() → onClose()`; el desmontaje
  cancela alarma/servicio en `onDispose`. La feature **gatea qué activa** `exiting`, no lo reimplementa.
- **Sin dependencias nuevas**, `init.sh` sin cambios, tests JUnit 4 en español con negativos.

### Current Gaps

- `requestExit` sale **inmediatamente** (`exiting = true`) por las tres vías y no hay ninguna decisión
  previa.
- **No existe `BackHandler` en `app/src/main`** (verificado: 0 apariciones de `BackHandler`/
  `onBackPressed`), así que el gesto atrás del sistema no pasa por la salida del modo.
- No hay microcopy ni diálogo de confirmación de salida.

## Technical Approach

### 1. Decisión pura — `app/src/main/java/com/sazon/app/ui/cooking/CookingExitConfirmation.kt` (nuevo)

```kotlin
/** Punto de salida del Modo cocina que pide la confirmación. */
enum class CookingExitPoint { BACK_BUTTON, FINISH }

object CookingExitPolicy {
    fun requiresConfirmation(state: CookingModeState, point: CookingExitPoint): Boolean
}

object CookingExitCopy {
    const val TITLE = "¿Salir del modo cocina?"
    const val MESSAGE = "Si sales ahora perderás el progreso de tu receta."
    const val CONFIRM = "Salir"
    const val DISMISS = "Seguir cocinando"
}
```

- **Regla** (`requiresConfirmation`): `true` si
  `state.timer.status == RUNNING || state.timer.status == PAUSED || state.completedOrders.isNotEmpty()`;
  `false` en `IDLE`/`FINISHED` sin marcados (un temporizador Terminado **solo** no es "algo que
  perder", lo fija la nota de la entrada).
- **Misma regla para los dos puntos**: `BACK_BUTTON` y `FINISH` comparten la única condición (una sola
  puerta). El parámetro existe para que "Atrás y Terminar piden lo mismo" sea un **invariante testeado**
  y para tener un único sitio donde una divergencia futura sería deliberada.
- Sin Compose/Android, sin reloj, sin `Recipe`: solo depende de `CookingModeState`. Caso defensivo:
  receta sin pasos (`isEmpty`, temporizador `of(0)` `IDLE`, sin marcados) ⇒ `false` ⇒ salida directa.

### 2. Cableado Compose — `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` (modificar)

- Nuevo import `androidx.activity.compose.BackHandler` y nuevo estado
  `var confirmExit by remember { mutableStateOf(false) }`.
- Dos entradas gateadas sobre el camino de salida **existente**:
  - `requestBack` (punto `BACK_BUTTON`): usado por la cabecera (`onBack`) y por el `BackHandler`.
  - `requestFinish` (punto `FINISH`): usado por la barra inferior (`onClose`).
  - Cada una: `if (CookingExitPolicy.requiresConfirmation(state, punto)) confirmExit = true else exiting = true`.
- `BackHandler { if (confirmExit) confirmExit = false else requestBack() }`: con el diálogo abierto, el
  atrás **descarta el diálogo** (cancelar) y no reabre ni sale; con el diálogo cerrado, enruta por el
  gate. Se declara en la capa Compose (no en `ui.cooking` puro).
- `if (confirmExit) CookingExitDialog(onConfirm = { confirmExit = false; exiting = true },
  onDismiss = { confirmExit = false })`, composable **`private`** junto a
  `NotificationPermissionRationale`: `AlertDialog` de Material 3, título/cuerpo/botones desde
  `CookingExitCopy`, **una** acción principal (`TextButton` en `colorScheme.primary`, misma convención
  que el rationale aceptado) y descarte sin énfasis. **Sin** tokens, colores, tipografías ni formas
  nuevas (no se usa `colorScheme.error`: `DESIGN.md` no define estilo destructivo de diálogos).
- **Invariante de "solo al confirmar"**: abrir/cerrar el diálogo **no** fija `exiting`, **no** llama a
  `session.clear()`, **no** aplica ningún `CookingAlarmCommand` y **no** pausa el temporizador; alarma,
  servicio, sesión persistida y reloj quedan intactos hasta "Salir". Al confirmar se conserva el orden
  actual: `clear()` → `onClose()` → `onDispose` (`CookingAlarmPolicy.onExit()`).
- El `LaunchedEffect` del reloj (`tick(1000)`) no se toca: la cuenta atrás sigue corriendo detrás del
  diálogo.

### 3. Tests JVM — `app/src/test/java/com/sazon/app/ui/cooking/CookingExitPolicyTest.kt` (nuevo)

Matriz completa × los **dos** puntos de salida, con casos negativos que fallarían si la política no
hiciera nada (JUnit 4, nombres en español, `CookingModeState`/`CookingTimerState` reales).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingExitConfirmation.kt` — `CookingExitPoint`,
  `CookingExitPolicy`, `CookingExitCopy` (puros).
- `app/src/test/java/com/sazon/app/ui/cooking/CookingExitPolicyTest.kt` — la verificación exigida.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — `BackHandler`, gate de los dos
  puntos de salida y `CookingExitDialog` (`private`).
- `docs/technical-discovery.md` — añadir el gate de salida al párrafo de `ui.cooking`.
- `DESIGN.md` — nota de **excepción de capturas** de esta feature (ver `Visual Design Impact`).

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`domain/**`, `data/**` (incluido `data/cooking/**`), `ui/theme/**`, `ui/MainShell.kt`,
`CookingTimerState.kt`, `CookingModeState.kt`, `CookingModeFormat.kt`, `CookingAlarm.kt`,
`CookingSession*.kt`, `NotificationPermission*.kt`, `AndroidManifest.xml`, `res/**` ni la
Taxonomía/`seed-recipes.json`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` (fuente de verdad visual) — **roles** de `SazonTheme` y sus reglas vigentes
  (una sola acción principal por superficie, descarte sin énfasis). No se inventan colores, tipografías,
  formas ni tokens nuevos; no se crea material en `docs/design/concepts/`.
- Screens or states affected: el **Modo cocina ya aceptado** con el **diálogo de confirmación de salida**
  visible, en sus dos entradas (desde "Atrás"/gesto atrás y desde "Terminar"), sobre los estados ya
  aceptados del temporizador (En marcha y Pausado son los que lo disparan). Claro/oscuro por el tema.
- New design artifact required: **no** — excepción de capturas (misma clase que
  `cooking-timer-alarm-permission-context`): un `AlertDialog` de Material 3 **no es una pantalla
  diseñada**; reutiliza los roles del tema, no añade tokens y su microcopy vive en la constante pura
  `CookingExitCopy`, no en recursos. El acabado del diálogo en claro/oscuro lo revisa la persona en el
  emulador tras el merge; un defecto visual se corrige con una **feature correctiva**.
- Microcopy: **elegida por el planner** (`CookingExitCopy`), porque `DESIGN.md` no cubre diálogos de
  confirmación; la persona puede afinarla tras el merge (defecto de texto ⇒ feature correctiva).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — añadir al párrafo de `ui.cooking` que la salida del modo
  pasa por la decisión pura `CookingExitPolicy` (En marcha/Pausado o pasos marcados), que el gesto atrás
  del sistema se intercepta con `BackHandler` mientras el modo está compuesto y que alarma/servicio y
  sesión solo se cancelan/borran al **confirmar**.
- `docs/domain-model.md`: **not needed** — no hay entidad, relación ni estado de dominio nuevos (es
  presentación sobre `CookingModeState`, ya registrado).
- `AGENTS.md`: **not needed** — el flujo, el arranque y las reglas de repo no cambian.
- `DESIGN.md`: **update** — registrar la excepción de capturas (diálogo de confirmación no diseñado).
- `docs/build-brief.md`: **not needed** — no cambia el alcance del MVP ni ningún Non-Goal (es pulido de
  UX del Modo cocina ya aceptado; no hay notificaciones, permisos ni integraciones nuevas).
- `docs/adr/`: **not needed** — no hay decisión arquitectónica nueva; la feature sigue los patrones ya
  registrados (`CookingAlarmPolicy`/`NotificationPermissionPolicy`, ADR 0007/0008).
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear `CookingExitConfirmation.kt` (`CookingExitPoint`, `CookingExitPolicy`, `CookingExitCopy`),
   puro y sin leer el reloj.
2. Escribir `CookingExitPolicyTest` con la matriz de estados × ambos puntos de salida y negativos.
3. Modificar `CookingModeScreen.kt`: `BackHandler`, `requestBack`/`requestFinish` gateados y
   `CookingExitDialog` reutilizando los roles del tema.
4. Ejecutar `bash init.sh` en verde (test + lint) y actualizar `docs/technical-discovery.md` y
   `DESIGN.md`.

## Implementation Tasks

- [ ] Crear `CookingExitPoint` (`BACK_BUTTON`, `FINISH`) y `CookingExitPolicy.requiresConfirmation`.
- [ ] Crear `CookingExitCopy` (título, mensaje, confirmar, descartar) como constantes puras.
- [ ] Escribir `CookingExitPolicyTest`: En marcha ⇒ `true`; Pausado sin marcados ⇒ `true`; Inactivo con
      marcados ⇒ `true`; Terminado con marcados ⇒ `true`; Inactivo sin marcados ⇒ `false`; Terminado sin
      marcados ⇒ `false`; receta sin pasos ⇒ `false`; y **cada caso por los dos puntos de salida**
      (`BACK_BUTTON` y `FINISH`) con aserción explícita.
- [ ] Añadir `BackHandler` en `CookingModeScreen.kt` que, con el diálogo abierto, lo descarta, y si no
      enruta por `requestBack`.
- [ ] Gatear la cabecera (`requestBack`) y la barra inferior (`requestFinish`) sin cambiar el camino de
      salida (`exiting` → `session.clear()` → `onClose()` → `onDispose`).
- [ ] Añadir `CookingExitDialog` (`private`, `AlertDialog` de Material 3) con `CookingExitCopy` y roles
      del tema; "Salir" ⇒ `exiting = true`; "Seguir cocinando"/descarte ⇒ solo cierra el diálogo.
- [ ] Ejecutar `bash init.sh` en verde y actualizar `docs/technical-discovery.md` y `DESIGN.md`.

## Verification Plan

- `./gradlew test` (vía `bash init.sh`) → **esperado**: `CookingExitPolicyTest` en verde (la
  **verificación exigida**) sin romper las suites existentes de `cooking` (`CookingAlarmPolicyTest` 15,
  `CookingTimerStateTest` 11, `CookingModeStateTest` 10, `CookingModeFormatTest` 8,
  `CookingSessionTest` 14, `NotificationPermissionPolicyTest` 14 y las de `data.cooking`).
- `./gradlew lint` (vía `bash init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de
  la feature (los 13 preexistentes se mantienen).
- `bash init.sh` → **esperado**: código 0, sin procesos largos; `init.sh` **no cambia**.
- Inspección: `CookingExitConfirmation.kt` sin imports `android.*`/`androidx.*` y sin leer el reloj;
  `BackHandler` solo en `CookingModeScreen.kt`; el diálogo es `private` y no introduce hex ni tokens;
  sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`, `data/**`,
  `ui/theme/**`, `ui/MainShell.kt`, `CookingTimerState`/`CookingModeState`/`CookingModeFormat`,
  `CookingAlarm*`/`CookingSession*`/`NotificationPermission*`, el manifiesto ni `res/**`.
- Manual (emulador, lo revisa la persona; `AGENTS.md` no automatiza UI):
  - Con el temporizador En marcha, "Atrás", "Terminar" (último paso) y el **gesto atrás** piden
    confirmación.
  - "Seguir cocinando" no pierde nada y la **cuenta atrás sigue** correcta.
  - "Salir" cierra el modo, vuelve al Detalle y limpia el aviso/servicio; al reabrir la app **no** se
    reanuda la sesión.
  - Sin nada que perder (temporizador Inactivo/Terminado y sin marcados) se sale directo.
  - Con el diálogo abierto, el atrás lo descarta.
  - Acabado del diálogo en claro y oscuro frente a las reglas de `DESIGN.md`.

Cobertura E2E: **no aplica**. El repo no tiene suite E2E persistente y `AGENTS.md` prohíbe automatizar UI
de emulador; la decisión se cubre en JVM y el cableado Compose se verifica por inspección y prueba manual.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`) y no
arranca procesos largos ni necesita modos nuevos.

## Evidence To Capture

- Salida de `./gradlew test` con `CookingExitPolicyTest` (nº de tests nuevo, 0 fallos) y el total de la
  suite; caso negativo explícito que demuestre que la política no aprueba siempre.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature; `bash init.sh` con
  código 0.
- Inspección: pureza de `CookingExitConfirmation.kt`, ausencia de `BackHandler` fuera de
  `CookingModeScreen.kt`, y `git diff --stat` mostrando solo los ficheros previstos (sin
  build/lockfile/`init.sh`/`MainShell.kt`/dominio/`data.cooking`/tema/manifiesto/`res`).
- Resultado manual en emulador (descripción) de las tres vías con y sin algo que perder, del "Seguir
  cocinando" con la cuenta atrás viva y del atrás con el diálogo abierto.
- Anotación en `feature_list.json`/`PROGRESS.md` de la evidencia y del estado `passing` (al implementar,
  por el implementer; el planner no los toca).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Decisión pura con estado inyectado** (`CookingExitPolicy`), sin reloj ni Android: mismo patrón que
  `CookingAlarmPolicy`/`NotificationPermissionPolicy`; testeable en JVM (verificación exigida).
- **"Algo que perder" = temporizador En marcha/Pausado o `completedOrders` no vacío**: un temporizador
  **Terminado solo** no pide confirmación (lo fija la nota de la entrada); el marcado sí pesa.
- **El punto de salida forma parte del contrato** (`CookingExitPoint`): las tres vías usan una sola
  puerta y "Atrás y Terminar piden lo mismo" queda como invariante testeado (evita el fallo típico de
  gatear solo una).
- **`BackHandler` solo mientras el Modo cocina está compuesto**: no se introduce una pila de navegación
  global; fuera del modo, el atrás sigue como hoy.
- **Cancelar alarma/servicio y borrar la sesión solo al confirmar**: se conserva el camino actual
  (`exiting` → `clear()` → `onClose()` → `onDispose`); el diálogo no toca `exiting` ni el reloj.
- **Diálogo de Material 3 con roles del tema, acción principal en `primary`** (`Error` no se usa:
  `DESIGN.md` no define estilo destructivo de diálogos); mismo criterio que el rationale aceptado.
- **Excepción de capturas**: un `AlertDialog` no es pantalla diseñada; se documenta en `DESIGN.md`.
- **Sin dependencias nuevas**: `BackHandler` viene de `androidx.activity:activity-compose` 1.8.0, ya
  dependencia directa.

### Unknowns

- **Microcopy del diálogo**: `DESIGN.md` no cubre confirmaciones; el texto de `CookingExitCopy` es
  decisión del planner y la persona puede afinarlo tras el merge (defecto ⇒ feature correctiva).
- **Reparto del atrás entre la ventana del diálogo y `BackHandler`**: en Android el `Dialog` suele
  consumir el atrás y llamar a `onDismissRequest`; la rama `if (confirmExit)` hace explícita la semántica
  "atrás = cancelar" sin doble manejo. Se verifica por inspección y a mano, no en JVM.
- **Rotación con el diálogo abierto**: `confirmExit` vive en `remember` y se pierde al rotar; es la misma
  limitación ya aceptada del modo (`cookingRecipe` en `remember`), no se resuelve aquí.
- **Gesto atrás por hardware/3 botones/gesto**: mismo despacho del sistema; lo revisa la persona en el
  emulador.

## Validator Checklist

- [ ] `CookingExitPolicy` es **pura** (sin `android.*`/`androidx.*`, sin leer el reloj) y pide
      confirmación con el temporizador En marcha o Pausado, y con `completedOrders` no vacío
      (escenarios 1, 2, 7, **verificación exigida**).
- [ ] **No** pide confirmación con el temporizador Inactivo/Terminado sin marcados ni con una receta sin
      pasos (escenario 6, con caso negativo que no pasa en vacío).
- [ ] La decisión cubre **los dos puntos de salida** ("Atrás" y "Terminar") con aserción explícita en el
      test JVM (**verificación exigida**).
- [ ] El **gesto atrás del sistema** se intercepta con `BackHandler` en `CookingModeScreen.kt` y enruta
      por el mismo gate (escenario 3); con el diálogo abierto, el atrás lo descarta (escenario 8).
- [ ] "Salir" ejecuta la salida actual sin cambios (`clear()` → `onClose()` → `onDispose`) y "Seguir
      cocinando" no cambia estado, sesión ni alarma/servicio, con la cuenta atrás viva (escenarios 4 y 5).
- [ ] Alarma, servicio, sesión persistida y reloj **no** se tocan al abrir/cerrar el diálogo (solo al
      confirmar).
- [ ] Para UI: `Visual Design Impact` identifica `DESIGN.md` como fuente de verdad, el diálogo usa sus
      **roles** sin tokens nuevos y la nota de **excepción de capturas** queda registrada en `DESIGN.md`.
- [ ] `domain/**`, `data/**`, `ui/theme/**`, `ui/MainShell.kt`, `CookingTimerState`/`CookingModeState`/
      `CookingModeFormat`, `CookingAlarm*`/`CookingSession*`/`NotificationPermission*`, el manifiesto,
      `res/**`, el build/`lockfile` e `init.sh` quedan intactos; sin dependencias nuevas.
- [ ] `bash init.sh` en verde y evidencia registrada en `feature_list.json`/`PROGRESS.md`, con
      `docs/technical-discovery.md` y `DESIGN.md` coherentes.
