# Feature Implementation Spec: Aviso de sistema al terminar el temporizador del modo cocina

## Source Feature

- `id`: `cooking-timer-alarm`
- `area`: `integration`
- `depends_on`: `["cooking-mode"]` (`accepted`; el modo cocina ya existe con su temporizador puro).
- `status`: `not_started` en `feature_list.json` a la hora de planificar. Esta spec es el contrato
  planner → implementer → validator. El planner **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (líneas 1621-1636).
- Verificación exigida por la entrada: `bash init.sh` en verde; test **JVM** de la política pura de
  aviso con **instantes inyectados** (programar al Iniciar/Reanudar, cancelar al Pausar/Reiniciar/
  salir, disparo único al llegar a cero); test **JVM/Robolectric** de canal y contenido de la
  notificación (sin excepción y sin datos sensibles); prueba **manual** en emulador (persona).
- Enmienda acotada del Non-Goal "Notificaciones y recordatorios" de `docs/build-brief.md`: **solo**
  el aviso de fin del temporizador (no recordatorios programados ni notificaciones de sugerencias).
- **Vigencia (correctiva `cooking-timer-alarm-permission-context`, 2026-10-15):** esta spec es el
  registro histórico del aviso aceptado; **desde la correctiva**, el permiso `POST_NOTIFICATIONS` ya
  **no** se pide "una vez por proceso al arrancar" sino **en contexto**, la primera vez que arranca el
  temporizador de un paso y tras un rationale (decisión pura `NotificationPermissionPolicy` + adaptador
  `data.cooking.AndroidNotificationPermissionPrompt`). Los escenarios, el plan de verificación y las
  casillas de esta spec quedan como registro de lo validado el 2026-10-15; ver
  `docs/specs/cooking-timer-alarm-permission-context.md`.

## Goal

Cuando el temporizador de un paso del Modo cocina llega a cero, además del aviso **en pantalla**
"¡Listo!" (ya aceptado), el usuario recibe un **aviso de sistema** (notificación en un canal propio
con sonido y vibración) aunque Sazón esté en segundo plano o el móvil bloqueado; al tocar la
notificación, Sazón vuelve a primer plano con el Modo cocina del paso que terminó.

La pieza central es la **decisión pura y testeable en JVM** del aviso: una política sin Compose ni
Android y **sin leer el reloj** (los instantes entran inyectados) que, a partir de la transición del
[`CookingTimerState`] ya aceptado, decide **programar** la alarma para el instante de fin o
**cancelarla**. Los adaptadores Android (alarma exacta + receptor + notificación) son delgados y
viven fuera de `ui.cooking`.

## Non-Goals

- **No** se implementan **recordatorios programados** por el usuario, ni notificaciones de
  sugerencias/recetas, ni un `foreground service` permanente: **solo** el aviso de fin del
  temporizador del Modo cocina.
- **No** se persiste **nada**: ni el estado del Modo cocina (paso, marcado, temporizador), ni el
  instante de fin. "Reanudar donde lo dejé" es **otra** feature; si el proceso muere, la app abre en
  su estado de arranque (ver `Unknowns`).
- **No** se cambia la lógica del temporizador aceptada (`CookingTimerState`, `CookingModeState`,
  `CookingModeFormat`) ni sus transiciones, ni la pantalla del Modo cocina salvo la costura del aviso
  y los dos efectos que la alimentan. El "¡Listo!" en pantalla sigue **igual**.
- **No** se añade `full-screen intent` (no es una app de reloj/despertador), ni `WakeLock`, ni
  `WorkManager`, ni `androidx.work`, ni `androidx.core.splashscreen`, ni iconos (`material-icons`),
  ni DI; **sin dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml`
  intactos; `NotificationCompat`, `AlarmManager` y `NotificationChannel` son framework +
  `androidx.core` ya presente).
- **No** entra: lista de la compra, exportar/compartir, multi-idioma, editor de recetas, ni tocar
  `domain/**`, `data/**` (salvo el paquete nuevo `data/cooking/`), la Taxonomía, `seed-recipes.json`,
  Room, `ui/detail/**`, `ui/theme/**`, `init.sh` ni `scripts/**`.
- **No** se crea captura de Stitch ni `docs/design/concepts/cooking-timer-alarm/`: la notificación es
  una superficie del **sistema operativo**, no una pantalla de la app.

## Job Story

Cuando dejo el temporizador de un paso en marcha y salgo de la app (o bloqueo el móvil) para hacer
otra cosa,
quiero que el móvil me avise con una notificación sonora cuando el paso termina,
para no pasarme de tiempo ni tener que quedarme mirando la pantalla.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo. Abre el Modo cocina, arranca el temporizador y
  puede salir/bloquear el móvil; recibe el aviso y, si lo toca, vuelve al paso.
- **Permiso `POST_NOTIFICATIONS`** (runtime, Android 13+ = todo el rango soportado, `minSdk = 33`):
  se pide **en contexto**, la **primera vez** que el usuario arranca el temporizador de un paso y
  **después** de un rationale breve, solo si no está concedido y no se ha pedido ya en la sesión
  (desde la correctiva `cooking-timer-alarm-permission-context`; antes se pedía una vez por proceso al
  arrancar — ver la nota de vigencia de `Source Feature`). **Denegado**: no hay aviso de sistema, pero
  el Modo cocina, la cuenta atrás y el "¡Listo!" en pantalla siguen **intactos** (degradación
  silenciosa: el receptor no publica nada y no falla).
- **Permiso `USE_EXACT_ALARM`** (API 33+, concedido en la instalación; su caso de uso documentado son
  relojes, **temporizadores** y calendarios; la app no se publica): permite programar la alarma en el
  instante exacto con `setExactAndAllowWhileIdle`. Si el sistema no lo concediera
  (`canScheduleExactAlarms() == false`), el adaptador degrada a una alarma **inexacta** que despierta
  el dispositivo (`setAndAllowWhileIdle`): el aviso puede llegar tarde, pero **nunca** hay crash.
- **Permiso `VIBRATE`** (normal, sin diálogo): habilita la vibración del canal.
- **Sistema operativo**: ejecuta la alarma y publica la notificación aunque la app esté en segundo
  plano, el proceso muerto o la pantalla bloqueada; el toque en la notificación trae la tarea al
  frente.

## Acceptance Scenarios

### Scenario 1: Iniciar/Reanudar programa el aviso en el instante de fin (verificación exigida)

Given el Modo cocina abierto en un paso de `minutes = 10` con el temporizador Inactivo,
When el usuario pulsa "Iniciar",
Then la política pura devuelve `Schedule(now + 600_000)` (`now` inyectado) y el adaptador programa una
alarma **exacta** para ese instante;
When el usuario pausa y después pulsa "Reanudar" con `remainingMillis = 300_000`,
Then la política devuelve `Schedule(now + 300_000)` para ese nuevo `now`.

### Scenario 2: el aviso suena con la app en segundo plano o el móvil bloqueado (verificación exigida)

Given el temporizador **En marcha** y la app en segundo plano (o el móvil bloqueado),
When llega el instante de fin,
Then el sistema publica **una única** notificación en el canal propio `cooking_timer`
(importancia alta, con sonido y vibración) y, con la app en primer plano, esa misma notificación
aparece **sin duplicarse** (id de notificación fijo y un solo `PendingIntent`).

### Scenario 3: tocar la notificación vuelve al Modo cocina del paso terminado (verificación exigida)

Given la notificación mostrada y la actividad de Sazón todavía viva con el Modo cocina abierto en ese
paso (app en segundo plano o pantalla bloqueada),
When el usuario toca la notificación,
Then Sazón vuelve a primer plano **resumiendo la actividad existente** (sin recrearla) y se ve el Modo
cocina de ese paso con su "¡Listo!"; la notificación se descarta (`autoCancel`).
Si el sistema hubiera destruido la actividad o el proceso, la app abre en su estado de arranque (sin
Modo cocina): limitación aceptada y documentada en `Unknowns`.

### Scenario 4: Pausar, Reiniciar, cambiar de paso y salir **cancelan** (verificación exigida)

Given el temporizador En marcha con el aviso ya programado,
When el usuario pulsa "Pausar", o "Reiniciar", o "Siguiente"/"Anterior", o "Atrás"/"Terminar" (o sale
del modo por cualquier vía),
Then la política devuelve `Cancel` (`onTimerChange` para las tres primeras; `onExit()` para la
salida) y el adaptador cancela la alarma pendiente; **no** se publica ninguna notificación al llegar
al instante que se habría cumplido.
`Cancel` cancela **solo** la alarma pendiente: **no** retira una notificación **ya mostrada**.

### Scenario 5: disparo único, sin reprogramar ni duplicar (verificación exigida)

Given el temporizador En marcha,
When la cuenta atrás llega a cero (por el `tick` de la app y/o por la alarma del sistema),
Then: la transición `RUNNING → FINISHED` devuelve `Cancel` (no se reprograma nada); los `tick`
posteriores son no-op y devuelven `None`; el receptor publica como máximo **una** notificación (la
primera vez; una nueva publicación con el mismo id la **reemplaza**, no la duplica); el "¡Listo!" en
pantalla sigue apareciendo.

### Scenario 6: permiso de notificaciones denegado

Given `POST_NOTIFICATIONS` denegado,
When el temporizador llega a cero,
Then no hay crash ni notificación; el Modo cocina y su "¡Listo!" en pantalla funcionan **igual** y la
alarma pendiente se cancela con normalidad.

### Scenario 7: paso sin duración ⇒ ninguna alarma

Given un paso con `minutes <= 0` (el bloque del temporizador está oculto y no hay "Iniciar"),
Then nunca se programa ninguna alarma (`Schedule` no es alcanzable: el temporizador no puede pasar a
`RUNNING`).

### Scenario 8: decisión pura y determinista (verificación exigida)

Given dos transiciones cualesquiera del [`CookingTimerState`] y un `now` inyectado,
When se evalúa `CookingAlarmPolicy.onTimerChange(previous, next, now)`,
Then el resultado es **determinista**, **no lee el reloj**, no muta nada y solo puede ser `Schedule`
(en las aristas `IDLE→RUNNING` y `PAUSED→RUNNING`), `Cancel` (salida de `RUNNING`, `Reiniciar` desde
`PAUSED`/`FINISHED` y `FINISHED`) o `None` (ticks y no-ops).

## Repository Research

### Files Inspected

- `feature_list.json` (líneas 1621-1636) — entrada `cooking-timer-alarm`: comportamiento,
  `depends_on`, verificación (gate + JVM puro + JVM/Robolectric de canal/contenido + manual) y el
  detalle del enfoque (`AlarmManager`, cancelación, disparo único, `POST_NOTIFICATIONS`, decisión pura
  con instantes inyectados) y de lo que **no** entra.
- `PROGRESS.md` — `cooking-mode` quedó `accepted` (sesión 078); WIP = 0; el gate del repo es
  `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`).
- `docs/specs/cooking-mode.md` — contrato aceptado del modo cocina: `CookingTimerState` puro
  (`of`/`start`/`pause`/`resume`/`reset`/`tick`, `hasDuration`, no-ops), overlay en `MainShell`,
  `LaunchedEffect` que solo alimenta `tick(1000)` y Microcopy "¡Listo!" (`FINISHED_NOTICE`).
- `app/src/main/java/com/sazon/app/ui/cooking/CookingTimerState.kt` — máquina pura y determinista
  (`IDLE`/`RUNNING`/`PAUSED`/`FINISHED`); `tick` no-op fuera de `RUNNING` o con `delta <= 0` y pasa a
  `FINISHED` con `remaining = 0`.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeState.kt` — `next`/`previous` **reinician**
  el temporizador al del paso nuevo (Inactivo) ⇒ transición `RUNNING → IDLE` que debe cancelar.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — capa Compose delgada: estado
  `rememberSaveable`, `LaunchedEffect(state.timer.status)` con el reloj, botones Iniciar/Pausar/
  Reanudar/Reiniciar, `onClose` y glifos `Canvas` (`ClockGlyph`, `CheckGlyph`).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — hospeda el Modo cocina como **overlay**
  (`var cookingRecipe by remember`) y `CookingModeScreen(recipe, onClose, modifier)`; ya recibe
  costuras Android por parámetro (`networkStatusSource`, `dishImageSettings`).
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — raíz de la app: compone `MainShell` con las
  dependencias construidas en `MainActivity` (sin DI).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — composición de dependencias a mano
  (`applicationContext`), `enableEdgeToEdge()`, `setContent` + `SazonTheme`; punto natural de la
  costura Android y de la petición de permiso.
- `app/src/main/AndroidManifest.xml` — hoy solo `ACCESS_NETWORK_STATE` e `INTERNET`; una única
  `Activity` (`MainActivity`, `exported="true"`); **sin** receptores ni servicios; `minSdk = 33`,
  `targetSdk = 36`.
- `app/build.gradle.kts` — `minSdk 33`, `compileSdk 36.1`, `buildConfig = true`, Robolectric +
  `androidx.test.core` ya en `testImplementation`, `isIncludeAndroidResources = true`.
- `app/src/main/java/com/sazon/app/data/NetworkStatusSource.kt` — patrón de costura Android en
  `data/` sobre un servicio del sistema, con KDoc que justifica permisos y degradación.
- `app/src/main/java/com/sazon/app/data/image/DishImageSettingsStore.kt` +
  `AndroidDishImageSettingsStore.kt` — patrón de costura con `object NoOp…` por defecto y adaptador
  que "no lanza".
- `app/src/test/java/com/sazon/app/data/image/AndroidDishImageSettingsStoreTest.kt` — patrón de test
  Robolectric (`@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [33])`,
  `ApplicationProvider.getApplicationContext()`).
- `app/src/test/java/com/sazon/app/ui/cooking/CookingTimerStateTest.kt` — patrón de test JUnit 4 puro
  con nombres en español y vectores dorados.
- `app/src/main/res/` — no hay `strings.xml` de microcopy de UI (solo `app_name`), ni iconos de
  notificación; los glifos de la app se dibujan con `Canvas`. `values/colors.xml` solo tiene el fondo
  de ventana.
- `docs/build-brief.md` — el Non-Goal "Notificaciones y recordatorios" sigue vigente y la ampliación
  de `cooking-mode` (issue #76) dejó escrito que el aviso era **visual**; hay que enmendarlo.
- `docs/risks-and-open-questions.md` — "Notificaciones y recordatorios" también en "Later / Not MVP";
  tabla de riesgos con la convención de fila + mitigación.
- `docs/technical-discovery.md` (líneas 373-392) — párrafo del Modo cocina que dice "Sin notificación
  de sistema, sonido, vibración ni persistencia (Non-Goals del MVP)".
- `CONTEXT.md` — vocabulario del dominio (Recipe, `RecipeStep`, Modo cocina no aparece como entidad).

### Existing Patterns To Follow

- **Lógica pura + adaptador Android delgado**: `ui.cooking` puro (sin Compose/Android) para la
  decisión; `data/` para el adaptador de plataforma (patrón `NetworkStatusSource`,
  `AndroidDishImageSettingsStore`).
- **Costura con default no-op**: `interface` + `object NoOpX` (patrón
  `NoOpDishImageSettingsStore`/`NoDishImageOriginDiagnostics`) para que previews y composiciones sin
  cableado compilen y no hagan nada.
- **Instantes inyectados, nunca reloj en lo puro**: el tiempo entra por parámetro (patrón
  `tick(deltaMillis)` de `CookingTimerState`) ⇒ tests JVM deterministas.
- **La degeneración es silenciosa**: un fallo (permiso denegado, excepción de plataforma) degrada a
  "sin aviso", nunca rompe la UI (patrón ADR 0005 / "no lanza" de las costuras).
- **Tests JUnit 4** con nombres en español, casos negativos y vectores dorados; **Robolectric**
  (`@Config(sdk = [33])`) solo para lo que toca framework.
- **Sin dependencias nuevas**; `init.sh` sin cambios.

### Current Gaps

- No existe **ninguna** notificación ni permiso de notificaciones en la app (0 referencias en
  `app/src`); tampoco hay `NotificationChannel`, `AlarmManager`, `BroadcastReceiver` ni entrada de
  receptor en el manifiesto.
- No hay **canal** ni **icono** de notificación: `res/` solo tiene los `mipmap` de plantilla y
  `strings.xml` con `app_name`. La notificación exige un **small icon** monocromo (no vale un
  `mipmap` de lanzador).
- No hay **ninguna vía de petición de permiso** en runtime en el repo
  (`rememberLauncherForActivityResult`/`ActivityResultContracts` no aparecen).
- El aviso actual es solo visual: el reloj vive en un `LaunchedEffect` que solo corre con la
  composición activa, y **no hay nada persistido** que sobreviva a que el modo se cierre.
- La app no tiene `Application` propia: el canal debe crearse desde la `MainActivity` o de forma
  idempotente en el adaptador.

## Technical Approach

Estrategia: **política pura + costura no-op + adaptador Android de plataforma**, con el mínimo cambio
en la capa Compose aceptada.

### 1. `ui/cooking/CookingAlarm.kt` — decisión pura y costura (sin Android)

```kotlin
/** Decisión del aviso de sistema. `None` = no cambia nada (no es "nada que hacer" nuevo). */
sealed interface CookingAlarmCommand {
    data class Schedule(val triggerAtMillis: Long) : CookingAlarmCommand
    data object Cancel : CookingAlarmCommand
    data object None : CookingAlarmCommand
}

/** Decisión **pura** del aviso: no lee el reloj, los instantes entran inyectados. */
object CookingAlarmPolicy {
    fun onTimerChange(
        previous: CookingTimerState,
        next: CookingTimerState,
        nowMillis: Long,
    ): CookingAlarmCommand

    /** Salir del Modo cocina ("Atrás"/"Terminar"/`onClose`, y descarte de la composición): cancela. */
    fun onExit(): CookingAlarmCommand = CookingAlarmCommand.Cancel
}

/** Costura del aviso de sistema. Default no-op para previews/tests/composiciones sin cableado. */
interface CookingAlarm {
    fun apply(command: CookingAlarmCommand)
}

object NoOpCookingAlarm : CookingAlarm {
    override fun apply(command: CookingAlarmCommand) = Unit
}
```

Regla de `onTimerChange` (determinista, sobre la tabla de verdad ya verificada en el repo):

| `next` | `previous` | Comando |
|---|---|---|
| `RUNNING` | no `RUNNING` y `next.remainingMillis > 0` | `Schedule(nowMillis + next.remainingMillis)` |
| no `RUNNING` | `previous != next` (transición real) | `Cancel` |
| no `RUNNING` | `previous == next` (tick/no-op devuelve el mismo estado) | `None` |
| `RUNNING` | `RUNNING` (ticks; doble "Iniciar" no-op) | `None` |
| cualquiera | `next.remainingMillis <= 0` con `RUNNING` (defensivo, no alcanzable por UI) | `None` |

- `nowMillis + next.remainingMillis` es el **instante de fin** en la base de `ELAPSED_REALTIME`
  (la capa Compose pasa `SystemClock.elapsedRealtime()`); nunca se lee el reloj aquí.
- Cubre los cuatro bordes exigidos: Iniciar (`IDLE→RUNNING`) y Reanudar (`PAUSED→RUNNING`) →
  `Schedule`; Pausar (`RUNNING→PAUSED`), Reiniciar (`*→IDLE`) y cambio de paso (`RUNNING→IDLE` por
  `next`/`previous`) → `Cancel`; llegar a cero (`RUNNING→FINISHED`) → `Cancel` (no reprogama).
- `onExit()` es el único borde sin transición de temporizador y existe para que la salida también sea
  una decisión **testeable en JVM**.

### 2. `ui/cooking/CookingModeScreen.kt` — dos efectos (mínimo cambio)

Se añade el parámetro `alarm: CookingAlarm = NoOpCookingAlarm` (al final, con default: los llamantes
existentes siguen compilando) y **dos** efectos, sin tocar el reloj ni la UI:

```kotlin
var previousTimer by remember { mutableStateOf(state.timer) }

LaunchedEffect(state.timer.status) {
    alarm.apply(
        CookingAlarmPolicy.onTimerChange(
            previous = previousTimer,
            next = state.timer,
            nowMillis = SystemClock.elapsedRealtime(),
        ),
    )
    previousTimer = state.timer
}

DisposableEffect(Unit) {
    onDispose { alarm.apply(CookingAlarmPolicy.onExit()) }
}
```

- El efecto se dispara en cada **cambio de estado** del temporizador: solo las aristas interesan y
  los `tick` devuelven `None` (no se reprograma cada segundo).
- `previousTimer` solo se consulta por `isRunning`/igualdad (nunca por su `remainingMillis`), así que
  guardar el primer estado de cada estado es suficiente.
- `onDispose` cubre "Atrás"/"Terminar"/`onClose` **y** cualquier descarte de la composición
  (p. ej. rotación, que ya cierra el modo aceptado). En muerte de proceso no corre ⇒ la alarma del
  sistema sobrevive y dispara (comportamiento deseado).

### 3. `data/cooking/` — adaptadores Android (los únicos que importan `android.*`)

- `AndroidCookingAlarm(context)` : `CookingAlarm`
  - `Schedule(at)`: `PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context,
    CookingAlarmReceiver::class.java), FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)` y
    `AlarmManager.setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, at, pi)`; si
    `!canScheduleExactAlarms()` o salta `SecurityException`, degradar a
    `setAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, at, pi)` (inexacta, despierta igual).
  - `Cancel`: `alarmManager.cancel(pi)` con el **mismo** `PendingIntent` (mismo `requestCode` y mismo
    Intent). **No** toca notificaciones ya publicadas.
  - `None`: no-op. Todo va envuelto en "no lanza" (mejor esfuerzo).
- `CookingAlarmReceiver` : `BroadcastReceiver` (manifest, `exported="false"`; el `PendingIntent`
  explícito del propio proceso funciona). En `onReceive` construye el `PendingIntent` de contenido
  hacia `MainActivity` (`FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP`,
  `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`) y llama a `CookingNotifications.notify(...)`. Nunca lanza:
  si el permiso está denegado o hay cualquier fallo, no publica nada.
- `CookingNotifications` (constantes + construcción, sin `MainActivity`):
  - `CHANNEL_ID = "cooking_timer"`, `CHANNEL_NAME = "Temporizador de cocina"`,
    `CHANNEL_DESCRIPTION = "Aviso cuando termina el temporizador de un paso"`,
    `NOTIFICATION_ID = 1001` (**fijo** ⇒ reemplaza, nunca duplica).
  - `TITLE = "¡Listo! El paso ha terminado"`, `TEXT = "Toca para volver al modo cocina."`
    (genéricos: la función **no recibe** la receta, así que no puede filtrar nada).
  - `ensureChannel(context)`: `NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_HIGH)` con
    `description`, `enableVibration(true)` + patrón corto, `setSound(Settings.System.DEFAULT_NOTIFICATION_URI,
    AudioAttributes(USAGE_NOTIFICATION_EVENT))`, `lockscreenVisibility = VISIBILITY_PUBLIC`; idempotente
    (crear dos veces con el mismo id no cambia nada).
  - `notification(context, contentIntent): Notification` con `NotificationCompat.Builder`
    (`smallIcon = R.drawable.ic_notification_timer`, título, texto, `autoCancel(true)`,
    `category = CATEGORY_ALARM`, `priority = PRIORITY_HIGH`) — **sin** `extras` propios.
  - `notify(context, contentIntent)`: `ensureChannel` + si `POST_NOTIFICATIONS` no está concedido
    (`checkSelfPermission`) o `areNotificationsEnabled()` es `false`, **no publica**; si publica,
    `NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)`.
- `MainActivity`: construir `AndroidCookingAlarm(applicationContext)`, mantener
  `CookingNotifications.ensureChannel(applicationContext)` y el
  `registerForActivityResult(ActivityResultContracts.RequestPermission())` (registrado **antes** de
  `onCreate`) y construir `AndroidNotificationPermissionPrompt(applicationContext,
  requestNotificationsPermission::launch)`, que lo lanza **en contexto** (desde
  `cooking-timer-alarm-permission-context` ya **no** se pide nada al arrancar); pasar ambas costuras a
  `SazonApp`.
- `SazonApp`/`MainShell`: nuevos parámetros `cookingAlarm: CookingAlarm = NoOpCookingAlarm` y
  `notificationPermission: NotificationPermissionPrompt = NoOpNotificationPermissionPrompt` (por
  parámetro, como `dishImageSettings`) reenviados a `CookingModeScreen`.

### 4. `res/drawable/ic_notification_timer.xml`

Vector 24×24 **monocromo** (blanco, sin color ni degradado: el sistema lo tiñe), trazado a partir del
mismo lenguaje del `ClockGlyph` que ya dibuja `CookingModeScreen` (círculo + agujas), dentro del área
segura de 24dp. Es el único asset nuevo; no se toca ningún `mipmap` ni el icono de lanzador (eso es la
feature `app-launcher-icon`, ajena).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingAlarm.kt` — comando, política pura y costura
  (`CookingAlarm` + `NoOpCookingAlarm`); **sin** imports de Android/Compose.
- `app/src/main/java/com/sazon/app/data/cooking/AndroidCookingAlarm.kt` — adaptador `AlarmManager`.
- `app/src/main/java/com/sazon/app/data/cooking/CookingAlarmReceiver.kt` — receptor de la alarma.
- `app/src/main/java/com/sazon/app/data/cooking/CookingNotifications.kt` — canal, contenido y
  publicación.
- `app/src/main/res/drawable/ic_notification_timer.xml` — small icon monocromo.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingAlarmPolicyTest.kt` — política pura (JVM).
- `app/src/test/java/com/sazon/app/data/cooking/CookingNotificationsTest.kt` — canal + contenido
  (Robolectric).
- `app/src/test/java/com/sazon/app/data/cooking/AndroidCookingAlarmTest.kt` — adaptador
  (Robolectric + `ShadowAlarmManager`).
- `docs/adr/0007-cooking-timer-alarm.md` — decisión durable (canal propio + alarma exacta +
  receptor + aviso efímero sin persistir el modo cocina; enmienda del Non-Goal).

**Modificar**

- `app/src/main/AndroidManifest.xml` — `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `VIBRATE` y el
  `<receiver android:name=".data.cooking.CookingAlarmReceiver" android:exported="false" />`.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — parámetro `alarm` + los dos
  efectos (reloj, UI y microcopy intactos).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — parámetro `cookingAlarm` y reenvío.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — parámetro `cookingAlarm` y reenvío.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — costura Android, canal y permiso.
- `docs/build-brief.md` — enmienda acotada del Non-Goal "Notificaciones y recordatorios".
- `docs/technical-discovery.md` — integración (canal, alarma, receptor, permiso, degradación) y
  corrección del párrafo del Modo cocina ("sin notificación…").
- `docs/risks-and-open-questions.md` — anotar "Later / Not MVP" y dos filas de riesgo nuevas.
- `DESIGN.md` — nota breve de la excepción de capturas (superficie del sistema, sin pantalla).
- `PROGRESS.md` y `feature_list.json` — **al implementar** (estado y evidencia), no en esta sesión.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`domain/**`, `data/**` salvo el paquete nuevo, la Taxonomía, `seed-recipes.json`, `assets/**`,
`ui/theme/**`, `ui/detail/**`, los `mipmap` de lanzador ni las capturas de `docs/design/concepts/`.

## Visual Design Impact

- UI involved: **parcialmente** — no se añade ni cambia ninguna **pantalla** de la app; el cambio
  visible es una **notificación del sistema operativo** y, en pantalla, nada (el "¡Listo!" aceptado
  sigue igual).
- Design source: `DESIGN.md` es la fuente de verdad de las pantallas, pero **no** cubre notificaciones
  de sistema (no hay tokens para ello y no se inventan). El único activo visual nuevo (el small icon)
  se deriva del lenguaje de glifos ya aceptado (`ClockGlyph` de `CookingModeScreen`) y es **monocromo**:
  el color lo impone el sistema al teñirlo, así que **no** se usan tokens de `DESIGN.md`.
- Screens or states affected: ninguna pantalla de la app; estados del Modo cocina sin cambios.
- New design artifact required: **no** — no hay captura de Stitch (una notificación no es una
  pantalla) y **no** se crea `docs/design/concepts/cooking-timer-alarm/`. Se registra la excepción en
  `DESIGN.md` (misma clase que las de #77/#85/#86/#91).
- Tarea del implementer: no inventar estilos; el contenido de la notificación es microcopy fijo y
  probado (título/texto de `CookingNotifications`).

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — este repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace una regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — primera integración de notificaciones/alarmas del
  repo: canal propio, alarma exacta (`USE_EXACT_ALARM` + degradación inexacta), receptor en el
  manifiesto, permiso `POST_NOTIFICATIONS` en runtime y su degradación; corregir el párrafo del Modo
  cocina que hoy afirma "sin notificación de sistema".
- `docs/domain-model.md`: **not needed** — no hay entidad, relación ni estado de dominio nuevos (el
  aviso es efímero y no se persiste).
- `docs/build-brief.md`: **update** — enmendar el Non-Goal "Notificaciones y recordatorios": sigue
  vigente para recordatorios programados y notificaciones de sugerencias, pero el **aviso de fin del
  temporizador** pasa a ser notificación de sistema con sonido y vibración (misma clase de enmienda
  acotada que la de `cooking-mode`, issue #76).
- `docs/risks-and-open-questions.md`: **update** — anotar la entrada de "Later / Not MVP" y añadir dos
  riesgos (permiso denegado / alarma exacta no concedida; aviso de sistema que no devuelve al paso si
  el proceso murió).
- `docs/adr/0007-cooking-timer-alarm.md`: **create** — decisión y alternativas descartadas
  (`WorkManager`/alarma inexacta/`foreground service`/persistir el modo cocina).
- `DESIGN.md`: **update** — nota breve de la excepción de capturas (sin pantalla nueva, sin tokens).
- `CONTEXT.md`: **not needed** — no introduce vocabulario de dominio nuevo.

## Implementation Plan

1. Crear `CookingAlarm.kt` (comando + política + costura no-op) y su test JVM con los vectores
   dorados de la tabla de verdad y del `now` inyectado.
2. Crear `res/drawable/ic_notification_timer.xml` y `CookingNotifications.kt` (canal, construcción,
   publicación con permiso) con su test Robolectric.
3. Crear `AndroidCookingAlarm.kt` y `CookingAlarmReceiver.kt`, declarar permisos y receptor en el
   manifiesto y cubrir el adaptador con `ShadowAlarmManager`.
4. Cablear `CookingModeScreen` (parámetro + dos efectos), `MainShell`, `SazonApp` y `MainActivity`
   (costura, canal, permiso una vez por proceso — momento **superado** por
   `cooking-timer-alarm-permission-context`, que lo pide en contexto; ver la nota de vigencia).
5. Ejecutar `bash init.sh` en verde (test + lint) y actualizar la documentación durable (ADR 0007,
   `docs/build-brief.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
   `DESIGN.md`).

## Implementation Tasks

- [x] Crear `CookingAlarmCommand` (`Schedule(triggerAtMillis)`, `Cancel`, `None`), `CookingAlarmPolicy`
      (`onTimerChange`, `onExit`) y la costura `CookingAlarm` + `NoOpCookingAlarm`, **sin** imports de
      Android/Compose.
- [x] Escribir `CookingAlarmPolicyTest` (JUnit 4, nombres en español): Iniciar `of(10)` →
      `Schedule(now + 600_000)`; Reanudar con `remaining = 300_000` → `Schedule(now + 300_000)`;
      Pausar → `Cancel`; Reiniciar desde `RUNNING`/`PAUSED`/`FINISHED` → `Cancel`; cambio de paso
      (`RUNNING → IDLE`) → `Cancel`; `RUNNING → FINISHED` → `Cancel`; tick en `RUNNING` → `None`;
      tick/no-op en `IDLE`/`PAUSED`/`FINISHED` → `None`; doble "Iniciar" (no-op) → `None`;
      `onExit()` → `Cancel`; determinismo (mismo `now` ⇒ mismo resultado) y `now` inyectado (dos
      `now` distintos ⇒ dos instantes distintos).
- [x] Crear `res/drawable/ic_notification_timer.xml` (vector 24dp monocromo, círculo + agujas).
- [x] Crear `CookingNotifications` (constantes, `ensureChannel`, `notification`, `notify` con permiso)
      sin recibir la receta.
- [x] Escribir `CookingNotificationsTest` (Robolectric, `@Config(sdk = [33])`): `ensureChannel`
      idempotente y canal con `IMPORTANCE_HIGH`, sonido y vibración; la notificación se construye sin
      excepción con `smallIcon` no nulo, título/texto exactos, `autoCancel` y sin `extras` propios ni
      datos de receta/claves; con permiso denegado (`ShadowApplication`/sin conceder) `notify` no
      publica y no lanza.
- [x] Crear `AndroidCookingAlarm` (programar/cancelar con `ELAPSED_REALTIME_WAKEUP`, degradación
      inexacta, "no lanza") y `CookingAlarmReceiver` (publica la notificación; no lanza).
- [x] Escribir `AndroidCookingAlarmTest` (Robolectric + `ShadowAlarmManager`): `Schedule(at)` →
      una alarma programada para `at`; `Cancel` → ninguna; `None` → ninguna. Si el shadow no soporta
      `setExactAndAllowWhileIdle`, verificar la degradación a `setAndAllowWhileIdle` y **anotarlo** en
      la evidencia.
- [x] Añadir al manifiesto `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `VIBRATE` y el receptor
      `exported="false"`.
- [x] Cablear `CookingModeScreen` (`alarm` con default no-op + los dos efectos), `MainShell` y
      `SazonApp` (parámetro + reenvío).
- [x] Cablear `MainActivity`: `AndroidCookingAlarm(applicationContext)`, `ensureChannel` y petición de
      `POST_NOTIFICATIONS` una vez por proceso (`registerForActivityResult`), sin gatear la UI.
      *(Registro histórico: desde `cooking-timer-alarm-permission-context` la petición es en contexto.)*
- [x] `bash init.sh` en verde y actualizar ADR 0007, `docs/build-brief.md`,
      `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y `DESIGN.md`.

## Verification Plan

- `bash init.sh` → **esperado**: código 0 (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos) y **`init.sh` sin cambios**.
- `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*" --tests "com.sazon.app.data.cooking.*"`
  → **esperado**: BUILD SUCCESSFUL con las tres suites nuevas en verde
  (`CookingAlarmPolicyTest` — **verificación exigida**, `CookingNotificationsTest`,
  `AndroidCookingAlarmTest`) y sin suites modificadas.
- `./gradlew test` → **esperado**: se mantienen las suites existentes (baseline actual: 100 suites /
  747 tests) y el delta son **solo** las suites/tests nuevos; `CookingTimerStateTest`,
  `CookingModeStateTest` y `CookingModeFormatTest` **sin tocar**.
- `./gradlew lint` → **esperado**: 0 errores y **0 warnings nuevos bajo `app/src`** (los 13
  preexistentes se mantienen). Riesgo conocido: si lint emite un warning nuevo por la alarma exacta,
  el remedio documentado es emparejar `USE_EXACT_ALARM` con
  `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />`
  (patrón oficial; con `minSdk 33` es inocuo) y registrar el hallazgo.
- Inspección (greps propios del implementer y del validator):
  - `ui/cooking/CookingAlarm.kt` **no** importa `android.*`/`androidx.*`: la política es pura.
  - `AlarmManager`/`PendingIntent`/`NotificationManager` aparecen **solo** bajo
    `app/src/main/java/com/sazon/app/data/cooking/` (además del manifiesto y `MainActivity`).
  - `CookingTimerState.kt`, `CookingModeState.kt`, `CookingModeFormat.kt` y `ui/theme/**` intactos;
    `app/build.gradle.kts` y `gradle/libs.versions.toml` intactos (sin dependencias nuevas).
  - El manifiesto declara el receptor **`exported="false"`** y los tres permisos; no hay servicios.
  - No se crea `docs/design/concepts/cooking-timer-alarm/` ni se tocan los `mipmap` de lanzador.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI ni el sistema de notificaciones):
  1. Abrir una receta → "Empezar a cocinar" → un paso con duración → "Iniciar".
  2. Dejar la app en segundo plano / bloquear el móvil y esperar al instante de fin: **suena/vibra**
     y aparece la notificación "¡Listo! El paso ha terminado"; al tocarla, vuelve el Modo cocina de
     ese paso con su "¡Listo!" en pantalla.
  3. Con la app en primer plano, el aviso de sistema aparece **una sola vez** (no se duplica) junto al
     "¡Listo!" de la pantalla.
  4. "Pausar", "Reiniciar", cambiar de paso y salir ("Atrás"/"Terminar") **no** dejan ningún aviso.
  5. Denegar `POST_NOTIFICATIONS`: el Modo cocina y su "¡Listo!" siguen funcionando; no hay crash.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; la decisión del aviso (lógica pura), el canal/contenido y el adaptador de alarma se cubren
en JVM/Robolectric y el comportamiento de sistema (Doze, pantalla bloqueada, permisos) lo revisa la
persona.

## Evidence To Capture

- Salida del gate `bash init.sh` (código 0) y recuento propio de los XML: suites/tests totales, 0
  fallos/errores/saltados y el delta respecto a la baseline en `HEAD` (100 suites / 747 tests).
- Salida de la tirada focal con `CookingAlarmPolicyTest`, `CookingNotificationsTest` y
  `AndroidCookingAlarmTest` (nombre de cada test y nº de tests).
- Informe de lint: 0 errores y 13 warnings preexistentes, **0 bajo `app/src`** (o el remedio aplicado
  si aparece el warning de alarma exacta).
- Inspección registrada: pureza de `CookingAlarm.kt`, adaptadores confinados a `data/cooking/`, build/
  lockfile/`init.sh`/dominio/temporizador aceptado intactos, receptor `exported="false"` en el
  manifiesto, sin capturas nuevas.
- Resultado manual del emulador (app en segundo plano/bloqueada, notificación única, toque → vuelve
  al paso, y el caso de permiso denegado), con la limitación de que no es automatizable.
- Anotación en `feature_list.json`/`PROGRESS.md` de la evidencia y del estado `passing` (al
  implementar; **no** en esta sesión).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **La decisión del aviso es pura y determinista** (`CookingAlarmPolicy`) con **instantes inyectados**
  y sin leer el reloj: mismo criterio que `CookingTimerState` (tests JVM con vectores dorados) y
  verificable sin emulador, que es la única verificación automatizable real del repo.
- **La costura es `apply(command)` con default no-op** (`NoOpCookingAlarm`, patrón
  `NoOpDishImageSettingsStore`): previews y composiciones sin cableado siguen compilando y sin efecto;
  `CookingModeScreen` gana **un** parámetro al final y **dos** efectos, sin tocar reloj, UI ni microcopy.
- **Alarma exacta con `AlarmManager`** (`ELAPSED_REALTIME_WAKEUP` + `setExactAndAllowWhileIdle`) y
  **receptor de broadcast declarado en el manifiesto**: es lo único que dispara con la app en segundo
  plano, el proceso muerto o la pantalla apagada/Doze, sin depender de la composición ni de un
  `foreground service` (no-goal).
- **`USE_EXACT_ALARM`** y no `SCHEDULE_EXACT_ALARM`: con `minSdk 33` es el permiso concedido en la
  instalación para apps cuyo caso de uso es un **temporizador** (la app no se publica, así que la
  política de Play no aplica) y evita mandar al usuario a Ajustes; se conserva la degradación a alarma
  **inexacta** si `canScheduleExactAlarms()` fuera `false` (nunca crash).
- **Cancelar no retira una notificación ya mostrada**: `Cancel` cancela solo la alarma pendiente. Si
  retirara también la notificación, el `Cancel` de `RUNNING → FINISHED` (cuando la app está en primer
  plano y su `tick` llega a cero) borraría el aviso recién disparado por el receptor. Contrapartida
  aceptada: una notificación ya vista puede quedar en la bandeja tras "Reiniciar" (se reemplaza sola
  con el mismo id en el siguiente disparo).
- **Sin persistencia**: el instante de fin vive en la alarma del sistema y el estado del modo cocina
  sigue sin persistirse (no-goal explícito de la entrada). "Reanudar donde lo dejé" es otra feature.
- **El permiso se pide en contexto, al arrancar el temporizador de un paso y tras un rationale**
  (desde `cooking-timer-alarm-permission-context`; **ya no** al arrancar la app): `minSdk 33` obliga al
  permiso en runtime y la entrada exige que su ausencia **no** rompa el flujo; denegado, el aviso
  simplemente no aparece y el modo cocina queda intacto (y la petición **no** gatea el temporizador).
- **Sin dependencias nuevas**: `NotificationCompat` (`androidx.core`), `AlarmManager`,
  `NotificationChannel` y `ActivityResultContracts` (androidx.activity) ya están disponibles.

### Unknowns

- **Proceso muerto o actividad destruida**: si el sistema mata el proceso antes del instante de fin, la
  alarma sigue programada (no se pierde) y el receptor se recrea para publicar la notificación; al
  tocar, la app abre en su estado de arranque (sin Modo cocina), porque el modo **no** se persiste. Lo
  mismo si el sistema destruye solo la actividad (proceso vivo). Es la limitación aceptada de la
  entrada ("reanudar donde lo dejé" queda fuera); si molesta, será una feature propia.
- **Reinicio del dispositivo**: `ELAPSED_REALTIME` y las alarmas no sobreviven a un reinicio; no se
  añade `RECEIVE_BOOT_COMPLETED` (el modo cocina tampoco sobrevive).
- **Precisión bajo batería agresiva**: `setExactAndAllowWhileIdle` está sujeto a las políticas del
  fabricante (y a límites de Doze); el caso real se comprueba en la prueba manual, que no es
  automatizable.
- **Canal ya creado con otros ajustes**: si una instalación previa tuviera el canal `cooking_timer`
  con otro sonido/importancia, Android **no** lo actualiza al recrearlo; no aplica a un canal nuevo y
  cambiarlo es un cambio de `CHANNEL_ID`.
- **Notificación obsoleta en la bandeja**: ver la decisión de `Cancel`; cosmético y corregible con una
  feature correctiva si molesta.
- **Acabado/timbre exactos** (tono de notificación, patrón de vibración, aspecto en la pantalla de
  bloqueo): quedan para la revisión de la persona en el emulador; un defecto se corrige con una
  feature correctiva (AGENTS.md).

## Validator Checklist

- [ ] `CookingAlarmPolicy` es **puro** (sin `android.*`/`androidx.*`), **determinista** y **no lee el
      reloj**: `Schedule(now + remaining)` al Iniciar/Reanudar; `Cancel` al Pausar, Reiniciar, cambiar
      de paso, llegar a cero y salir; `None` en ticks y no-ops (escenarios 1, 4, 5 y 8, **verificación
      exigida**).
- [ ] Con permiso denegado no hay crash ni notificación y el Modo cocina y su "¡Listo!" siguen
      intactos (escenario 6); un paso sin duración nunca programa alarma (escenario 7).
- [ ] Canal y contenido se construyen sin excepción en Robolectric, el canal es `HIGH` con sonido y
      vibración, el `NOTIFICATION_ID` es fijo y el contenido es **genérico** (no recibe la receta; sin
      `extras` propios ni claves) (escenario 2, **verificación exigida**).
- [ ] El manifiesto declara `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `VIBRATE` y el receptor
      `exported="false"`; el adaptador usa alarma **exacta** con degradación inexacta y **no lanza**.
- [ ] La decisión pura está cubierta en JVM con casos negativos (no solo el camino feliz) y el
      adaptador Android está cubierto en Robolectric (o se documenta la limitación del shadow).
- [ ] `CookingTimerState`/`CookingModeState`/`CookingModeFormat` y la UI/microcopy aceptada del Modo
      cocina quedan **sin cambios**; `domain/**`, la Taxonomía, `seed-recipes.json`, `ui/theme/**`,
      `app/build.gradle.kts`, `gradle/libs.versions.toml` e `init.sh` intactos; sin dependencias
      nuevas.
- [ ] La petición de `POST_NOTIFICATIONS` es no bloqueante y **no gatea** ninguna pantalla ni el
      temporizador; el arranque no se resiente. (Momento de la petición: **en contexto**, al arrancar el
      temporizador de un paso y tras un rationale, desde `cooking-timer-alarm-permission-context`; ya no
      al arrancar la app.)
- [ ] `docs/build-brief.md` (Non-Goal enmendado de forma **acotada**), `docs/technical-discovery.md`,
      `docs/risks-and-open-questions.md`, `DESIGN.md` y el ADR 0007 quedan coherentes con lo
      implementado; **no** se crea `docs/design/concepts/cooking-timer-alarm/`.
- [ ] Gate en verde (`bash init.sh` código 0), con evidencia registrada (suites/tests/delta, lint con
      0 warnings nuevos bajo `app/src`) en `feature_list.json`/`PROGRESS.md`, y **sin** commit/push/PR
      del implementer.
