# Feature Implementation Spec: Temporizador en primer plano con cuenta atrás visible y vuelta al paso

## Source Feature

- `id`: `cooking-timer-foreground-service`
- `area`: `integration`
- `depends_on`: `["cooking-timer-alarm-permission-context"]` (`accepted`: el aviso de fin aceptado —canal
  `cooking_timer`, alarma exacta de un solo disparo, receptor y permiso `POST_NOTIFICATIONS` pedido en
  contexto con rationale— se **reutiliza** y **no** se reabre).
- `status`: `not_started` en `feature_list.json` al planificar (`feature_list.json`, líneas 1694-1709).
  Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`.
- Verificación exigida por la entrada:
  - `bash init.sh` (test + lint) en verde; el manifiesto declara el servicio con su tipo de foreground y
    la app arranca y funciona **con el servicio apagado**.
  - Test **JVM** de la sesión del temporizador (reloj e instantes **inyectados**, patrón
    `CookingAlarmPolicy`): arrancar/reanudar proyectan el instante de fin y el paso; pausar/reiniciar/
    cambiar de paso/salir lo actualizan o lo paran; al llegar a cero se avisa **una sola vez**.
  - Test **JVM/Robolectric**: la notificación persistente (cuenta atrás) y la de fin se construyen sin
    excepción y **solo exponen lo necesario** para volver al paso (sin clave de IA ni cuerpo de la
    receta).
  - Prueba **manual** (persona, emulador; no automatizable según `AGENTS.md`): minimizar/bloquear y ver el
    contador decrecer; al terminar suena/vibra; el toque vuelve al paso **también tras morir el proceso**;
    pausar/reiniciar/salir detienen el servicio.
- Ampliación pedida por la persona (2026-10-15) tras probar `cooking-timer-alarm`: el aviso aceptado es
  **solo el de fin** y la cuenta atrás **no se ve fuera de la app**.
- Enmienda **acotada** del Non-Goal "Notificaciones y recordatorios" de `docs/build-brief.md` (misma
  clase que la de `cooking-mode`/`cooking-timer-alarm`) y **revisión** del Non-Goal "sin
  `foreground service`" (alternativa 3) y del "sin persistencia del modo" (alternativa 4) de
  `docs/adr/0007-cooking-timer-alarm.md`.

## Goal

Mientras corre el temporizador de un paso del Modo cocina, el usuario ve la **cuenta atrás decrecer en
una notificación persistente de un servicio en primer plano**, aunque la app esté minimizada o el móvil
bloqueado; al llegar a cero el aviso "¡Listo!" sigue sonando y vibrando (con el canal aceptado), y al
tocar cualquiera de las dos notificaciones la app vuelve **al paso exacto** del Modo cocina, incluso si el
proceso había muerto.

La pieza central sigue siendo **pura y testeable en JVM**: la **sesión del Modo cocina**
(`CookingSession`) proyecta, con el instante **inyectado**, el **instante de fin** y la posición de la
sesión (receta, paso, marcado, temporizador), y sabe **reanudarse** recalculando lo que queda de cuenta
atrás desde ese instante. El acceso a Android (servicio, notificaciones, alarma, Room) queda detrás de
costuras delgadas en `data.cooking` y de una costura nueva con default no-op.

## Non-Goals

- **No** se sustituye el aviso de fin aceptado: la **alarma exacta de un solo disparo** (`AlarmManager` +
  `CookingAlarmReceiver` + `CookingNotifications` con id fijo `1001`) sigue siendo la **única** dueña del
  "¡Listo!" (ver `Decisions`). `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `VIBRATE`,
  `SCHEDULE_EXACT_ALARM (maxSdkVersion=32)` y el receptor **no se tocan**.
- **No** se cambia la lógica aceptada del temporizador ni del modo: `CookingTimerState`,
  `CookingModeState`, `CookingModeFormat`, `CookingAlarmPolicy`, `CookingAlarmCommand`, la costura
  `CookingAlarm` y `NotificationPermissionPolicy`/`NotificationPermissionPrompt` quedan **sin diff**.
- **No** se añaden **acciones** a la notificación (pausar/reiniciar desde la bandeja): el estado del
  temporizador solo lo cambia la pantalla.
- **No** hay punto de entrada en la app para "volver a cocinar": la sesión se reanuda **solo** al abrir la
  app desde la notificación del temporizador (un arranque normal **no** abre el Modo cocina).
- **No** se persiste nada más que la **sesión del Modo cocina** (una fila): ni historial de sesiones, ni
  preferencias de cocina, ni el "tope"/ajustes de imagen. Tampoco se recupera el modo tras **reiniciar el
  dispositivo** ni si el usuario **quita la tarea** de recientes (ver `Unknowns`).
- **No** se crea una segunda notificación por segundo ni un bucle de `tick` en el servicio: la cuenta
  atrás la **renderiza el sistema** (cronómetro) y el servicio solo espera al instante de fin.
- **No** entra: recordatorios programados por el usuario, notificaciones de sugerencias/recetas, lista de
  la compra, multi-idioma, `WorkManager`/`androidx.work`, `WakeLock`, `full-screen intent`, DI, librería
  de navegación ni **dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml`
  intactos).
- **No** se toca `domain/**`, la Taxonomía, `seed-recipes.json`, `assets/**`, `ui/theme/**`, las bases
  Room aceptadas (`ProfileDatabase`, `FavoritesDatabase`, `SuggestionBatchDatabase`,
  `ActivitySummaryDatabase`), `init.sh`, `scripts/**` ni `docs/design/concepts/**`.
- **No** se crea captura de Stitch ni `docs/design/concepts/cooking-timer-foreground-service/`: las dos
  notificaciones son superficies del **sistema operativo**, no pantallas de la app (misma excepción que
  `cooking-timer-alarm`).

## Job Story

Cuando pongo el temporizador de un paso y sigo cocinando (o me alejo del móvil) con la app en segundo
plano,
quiero ver cuánto falta **sin volver a abrir la app**,
para saber si me queda tiempo y volver al paso exacto cuando me avise.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo. Arranca el temporizador de un paso, minimiza/bloquea el
  móvil, ve la cuenta atrás en la notificación persistente, recibe el "¡Listo!" con sonido y vibración y,
  al tocar la notificación, vuelve al paso exacto.
- **Permiso `POST_NOTIFICATIONS`** (runtime): **sin cambios**; lo pide en contexto
  `AndroidNotificationPermissionPrompt` la primera vez que arranca el temporizador. Si está **denegado**,
  la notificación persistente **no se ve** (el servicio sigue corriendo, sin crash), el Modo cocina, la
  cuenta atrás en pantalla y el "¡Listo!" siguen intactos (degradación silenciosa, ADR 0005).
- **Permisos nuevos** (ambos **normales**, sin diálogo, concedidos al instalar):
  - `FOREGROUND_SERVICE` (obligatorio para `startForeground`).
  - `FOREGROUND_SERVICE_SPECIAL_USE` (obligatorio en API 34+ para el tipo `specialUse`; `minSdk = 33`,
    `targetSdk = 36`).
- **Tipo de servicio**: `android:foregroundServiceType="specialUse"` + `<property
  android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" …>`. No existe un tipo de primer plano
  específico para temporizadores (el más afín, `shortService`, está limitado a ~3 minutos), así que el
  caso de uso documentado es "temporizador de cocina en curso, visible en la notificación". La app no se
  publica en Play (`docs/technical-discovery.md`), así que no hay formulario de revisión que rellenar.
- **Sistema operativo**: mantiene el proceso del servicio en primer plano (la cuenta atrás se sigue
  pintando con la app minimizada o el móvil bloqueado), ejecuta la alarma exacta del "¡Listo!" y trae la
  tarea al frente al tocar cualquier notificación.

## Acceptance Scenarios

### Scenario 1: Iniciar/Reanudar arrancan el servicio con la cuenta atrás (verificación exigida)

Given el Modo cocina en un paso de `minutes = 10` con el temporizador Inactivo,
When el usuario pulsa "Iniciar" y el adaptador recibe `CookingAlarmCommand.Schedule(now + 600_000)`,
Then arranca `CookingTimerService` en **primer plano** con la notificación **persistente** (id `1000`,
`ongoing`, sin sonido propio, `smallIcon` monocromo) cuyo instante de fin proyectado es `now + 600_000`, y
el sistema pinta la cuenta atrás decreciente; reanudar (`PAUSED→RUNNING`) vuelve a arrancar el servicio
con el **nuevo** instante de fin.

### Scenario 2: la cuenta atrás se ve con la app minimizada o el móvil bloqueado (verificación exigida)

Given el temporizador En marcha y la app en segundo plano (o el móvil bloqueado),
When pasan los segundos,
Then el contador de la notificación persistente **decrece** sin que la app esté en pantalla, y al llegar a
cero el aviso "¡Listo!" suena y vibra (una sola vez, canal `cooking_timer`) mientras la notificación
persistente **desaparece**.

> **Nota de vigencia (`cooking-timer-finish-notification`, 2026-10-16):** este escenario se cumplía solo
> de forma **no determinista** con la app en primer plano (la política cancelaba la alarma en
> `RUNNING → FINISHED` a la vez que el `tick` llegaba a cero). Desde la correctiva la alarma ya **no** se
> cancela al llegar a cero, así que el aviso se publica **siempre**; la persistente sigue desapareciendo
> porque el servicio se para con su propio `delay`. El resto de la evidencia histórica no se reescribe.

### Scenario 3: al llegar a cero se avisa una sola vez (verificación exigida)

Given una sesión del Modo cocina En marcha con su instante de fin proyectado,
When el temporizador pasa a Terminado,
Then la sesión persistida deja de proyectar instante de fin (`endAtElapsedMillis = null`, "nada que
rearrancar"), `CookingAlarmPolicy` devuelve `Cancel` (no reprograma) y los `tick`/reintentos posteriores
devuelven `None`; el aviso de fin se publica **una sola vez** (id fijo `1001` y disparo único aceptado).

> **Nota de vigencia (`cooking-timer-finish-notification`, 2026-10-16):** la primera mitad sigue siendo
> cierta (la sesión no proyecta fin y los `tick` posteriores dan `None`), pero `CookingAlarmPolicy` ya
> **no** devuelve `Cancel` en `RUNNING → FINISHED`: devuelve `None` para **dejar viva** la alarma (el
> único productor del aviso). El aviso se sigue publicando **una sola vez** (id fijo).

### Scenario 4: Pausar, Reiniciar, cambiar de paso y salir **paran el servicio** (verificación exigida)

Given el temporizador En marcha con el servicio corriendo,
When el usuario pulsa "Pausar", o "Reiniciar", o "Siguiente"/"Anterior", o "Atrás"/"Terminar",
Then la política devuelve `Cancel`, el servicio se **detiene** y su notificación persistente
**desaparece**; "Reanudar" lo vuelve a arrancar con el instante correcto. Pausar/reiniciar/cambiar de paso
**no** borran la sesión (siguen siendo reanudables); salir del modo **sí** la borra (Scenario 8).

> **Nota de vigencia (`cooking-timer-finish-notification`, 2026-10-16):** este escenario **se mantiene**
> (Pausar/Reiniciar/cambiar de paso/salir siguen dando `Cancel`); la única ampliación es que, al salir del
> modo ("Atrás"/"Terminar"), la costura retira además el aviso de fin `1001` ya mostrado
> (`dismissFinishNotification`, comando aparte de `Cancel` y nunca en el `onDispose`).

### Scenario 5: el toque en la notificación vuelve al paso exacto

Given la notificación persistente (o la de fin) mostrada y el proceso vivo con el Modo cocina abierto en
un paso,
When el usuario toca la notificación,
Then la app vuelve a primer plano **reutilizando la actividad** (`FLAG_ACTIVITY_NEW_TASK or
FLAG_ACTIVITY_SINGLE_TOP`) y se ve el Modo cocina **en ese paso**, con su marcado y su temporizador; el
toque **no** resetea el modo ni duplica el trabajo del sistema (arrancar dos veces el servicio o la alarma
es idempotente).

### Scenario 6: volver al paso con el proceso muerto (verificación exigida)

Given la sesión persistida (`recipe` + `stepIndex` + marcado + temporizador + instante de fin) y el
proceso de la app **muerto**,
When el usuario toca la notificación del "¡Listo!" (la única que sobrevive a la muerte del proceso, porque
la publica la alarma),
Then la app abre, **lee la sesión persistida** y monta el Modo cocina en **ese** paso con lo que quedaba
de cuenta atrás recalculado desde el instante de fin: si el instante ya pasó, el temporizador queda
**Terminado** con el aviso "¡Listo!" en pantalla; si aún no pasó (muerte del proceso a mitad), sigue
En marcha con el tiempo restante correcto.

> **Nota de vigencia (`cooking-timer-finish-notification`, 2026-10-16):** el escenario **se mantiene** y
> se refuerza: la puerta de reapertura pasa a `CookingSession.reopensFromNotification` (`isResumable`
> **o** Terminado), de modo que una sesión persistida en **Terminado** (`remaining = 0`) también reabre el
> paso desde el aviso, sin rearmar alarma ni servicio. `isResumable` conserva su semántica.

### Scenario 7: el permiso denegado no rompe nada

Given `POST_NOTIFICATIONS` denegado,
When el temporizador arranca y llega a cero,
Then no hay crash ni notificación visible, el Modo cocina, la cuenta atrás en pantalla y el "¡Listo!" en
pantalla funcionan igual, y la sesión se sigue persistiendo y reanudando con normalidad.

### Scenario 8: arranque normal, salida explícita y sesión obsoleta

Given una sesión persistida y la app abierta **sin** venir de la notificación,
When el usuario abre la app (en frío o desde el lanzador),
Then el Modo cocina **no** se abre solo; y si sale del modo con "Atrás"/"Terminar", la sesión se **borra**
(la notificación que quede en la bandeja ya no reanuda nada: la app abre en su estado normal).

> **Nota de vigencia (`cooking-timer-finish-notification`, 2026-10-16):** la salida explícita
> ("Atrás"/"Terminar") **también retira** el aviso de fin `1001` que pudiera quedar en la bandeja
> (`CookingAlarm.dismissFinishNotification`), de modo que ya **no** queda una notificación obsoleta; la
> sesión se sigue borrando y un arranque normal sigue sin abrir el Modo cocina.

### Scenario 9: las notificaciones y los `Intent` exponen lo mínimo (verificación exigida)

Given el servicio y el receptor construyendo sus notificaciones,
When se inspeccionan las notificaciones y los `Intent` asociados,
Then la persistente expone **solo** el título fijo y el instante de fin (cronómetro) y la de fin el
microcopy aceptado; **ninguna** incluye clave de IA, cuerpo de la receta, ingredientes ni pasos; el
`Intent` de arranque del servicio lleva **solo** el instante de fin y los `Intent` de contenido llevan
**solo** el flag de reanudación (booleano).

### Scenario 10: el manifiesto declara el servicio y la app vive sin él (verificación exigida)

Given el repositorio tras esta feature,
When se lee `app/src/main/AndroidManifest.xml` y se arranca la app sin ningún temporizador en marcha,
Then el manifiesto declara `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` y el `<service>` con
`foregroundServiceType="specialUse"`, `exported="false"` y su `<property>`, y la app funciona exactamente
igual que antes (ningún servicio vivo, ninguna notificación, ninguna excepción).

## Repository Research

### Files Inspected

- `feature_list.json` (líneas 1668-1709) — entradas `cooking-timer-alarm-permission-context` (aceptada,
  cuyo *no-goal* dice explícitamente que el `foreground service` y "volver al paso" son **esta** feature) y
  `cooking-timer-foreground-service` (comportamiento, `depends_on`, las cuatro verificaciones y el detalle
  de lo que **no** entra).
- `PROGRESS.md` (líneas 13-68) — estado aceptado del aviso y de su correctiva; gate de referencia
  (`bash init.sh` exit 0, **109 suites / 830 tests**, lint 0 errores / 13 warnings preexistentes / 0 bajo
  `app/src`).
- `docs/specs/cooking-timer-alarm.md` (645 líneas) y `docs/specs/cooking-timer-alarm-permission-context.md`
  (722 líneas) — contrato aceptado del canal `cooking_timer`, la alarma exacta de un solo disparo
  (`ELAPSED_REALTIME_WAKEUP`, `requestCode` fijo ⇒ una sola viva), el receptor `exported="false"`, el id
  fijo `1001`, el contenido **genérico** y su no-goal del `foreground service`.
- `docs/specs/cooking-mode.md` (Non-Goals 33-38 y 414-420) — "**no** se persiste nada: ni progreso, ni
  pasos hechos, ni el temporizador"; rotar/cerrar el modo lo cierra.
- `docs/adr/0007-cooking-timer-alarm.md` — decisiones 2, 3, 5, 6, 7 (contenido genérico, sin extras), 8
  (id fijo ⇒ reemplaza), 9 (`Cancel` no retira la notificación mostrada), 10 (sin persistencia) y las
  alternativas **3** (`foreground service` descartado) y **4** (persistir el modo descartado), que esta
  feature **revisa**.
- `app/src/main/java/com/sazon/app/ui/cooking/` — `CookingTimerState.kt` (máquina pura
  `IDLE`/`RUNNING`/`PAUSED`/`FINISHED`, `isRunning`, `hasDuration`, no-ops), `CookingModeState.kt`
  (`stepIndex`/`totalSteps`/`completedOrders`/`timer`, `next`/`previous` reinician el temporizador,
  `initial(recipe)`), `CookingAlarm.kt` (`CookingAlarmCommand.Schedule/Cancel/None`, `CookingAlarmPolicy`,
  costura + `NoOpCookingAlarm`), `CookingModeFormat.kt` (`countdown`/`progressLabel`/`durationLabel`,
  `FINISHED_NOTICE`), `CookingModeScreen.kt` (842 líneas: `rememberSaveable` con `CookingModeStateSaver`,
  el `LaunchedEffect(state.timer.status)` con `previousTimer` y `SystemClock.elapsedRealtime()`, el
  `DisposableEffect` de salida, el reloj `tick(1000)` y el `AlertDialog` del rationale),
  `NotificationPermission.kt` y `NotificationPermissionPrompt.kt` (política pura + costura no-op).
- `app/src/main/java/com/sazon/app/data/cooking/` — `AndroidCookingAlarm.kt` (adaptador
  `runCatching`/`canScheduleExactAlarms` con degradación, `requestCode` fijo, *no lanza*),
  `CookingAlarmReceiver.kt` (receptor que publica con `FLAG_ACTIVITY_NEW_TASK or
  FLAG_ACTIVITY_SINGLE_TOP`), `CookingNotifications.kt` (`CHANNEL_ID = "cooking_timer"`,
  `NOTIFICATION_ID = 1001`, `TITLE`/`TEXT`, `ensureChannel` idempotente, `notification`, `notify` con
  comprobación de permiso), `AndroidNotificationPermissionPrompt.kt`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (267 líneas) — composición de dependencias a mano,
  `registerForActivityResult(...)` registrado antes de `onCreate`, `CookingNotifications.ensureChannel`,
  `AndroidCookingAlarm(applicationContext)` y paso de las costuras a `SazonApp`; **sin** `onNewIntent`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` (123 líneas) y `ui/MainShell.kt` (468 líneas) — costuras
  Android por **parámetro** con default no-op (`cookingAlarm`, `notificationPermission`,
  `dishImageSettings`); el Modo cocina se hospeda como overlay (`var cookingRecipe by remember`,
  líneas 327-350) abierto desde `RecipeDetailScreen` con `onStartCooking`.
- `app/src/main/AndroidManifest.xml` — `ACCESS_NETWORK_STATE`, `INTERNET`,
  `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `VIBRATE`, `SCHEDULE_EXACT_ALARM (maxSdkVersion=32)`; una
  `Activity` y un `<receiver>`; **ningún** `<service>`.
- `app/src/main/res/drawable/ic_notification_timer.xml` — small icon monocromo aceptado (se reutiliza;
  **no** hace falta asset nuevo).
- `app/build.gradle.kts` — `minSdk 33`, `targetSdk 36`, `compileSdk 36.1`, Robolectric +
  `androidx.test.core` en `testImplementation`, `isIncludeAndroidResources = true` y las propiedades de
  sistema de los tests (`taxonomy.asset.path`, `seed.recipes.asset.path`, `design.tokens.path`,
  `android.manifest.path`, `android.res.dir`).
- `gradle/libs.versions.toml` — `coreKtx 1.10.1`, `room 2.8.5`, `coroutines 1.11.0`, `robolectric 4.17`,
  `androidxTestCore 1.7.0`, `kotlinxSerialization 1.9.0`: **todo lo necesario ya está**.
- `app/src/main/java/com/sazon/app/data/suggestions/{SuggestionBatchEntity,SuggestionBatchDao,
  SuggestionBatchMappers,SuggestionBatchDatabase}.kt` — patrón de **una fila** con columna JSON y base de
  datos propia (`exportSchema = false`, `version = 1`).
- `app/src/test/java/com/sazon/app/data/suggestions/RoomSuggestionBatchRepositoryTest.kt` — patrón de test
  Robolectric + `Room.inMemoryDatabaseBuilder(...allowMainThreadQueries())` con `runTest`.
- `app/src/test/java/com/sazon/app/{ui/cooking,data/cooking}/` — patrón de las 7 suites de `cooking`
  (JUnit 4 con nombres en español y vectores dorados en lo puro; `@RunWith(RobolectricTestRunner::class)`
  + `@Config(sdk = [33])` para framework).
- `app/src/test/java/com/sazon/app/ui/theme/AppResourceFiles.kt` + `LauncherIconTest.kt` — lectura del
  **manifiesto** (`android.manifest.path`) y del árbol `res/` (`android.res.dir`) desde un test JVM
  (`parse`, `manifestFile`, `elements`, `androidAttr`): permite verificar el manifiesto **sin** tocar
  `app/build.gradle.kts`.
- `app/src/main/java/com/sazon/app/domain/recipe/RecipeJson.kt` — ida y vuelta de una `Recipe` (con
  `nutrition`/`ingredientId` opcionales), ya usado como columna JSON por el Lote.
- `androidx.core:core:1.10.1` (aar de la caché de Gradle) — `NotificationCompat.Builder` **sí** tiene
  `setUsesChronometer`, `setChronometerCountDown`, `setOngoing`, `setOnlyAlertOnce`, `setSilent`,
  `setCategory` y `setWhen`; `ContextCompat.startForegroundService` **sí** está; en cambio
  `ServiceCompat.startForeground(...)` **no** existe en 1.10.1 (⇒ usar el `startForeground(id,
  notification)` del framework y el tipo declarado en el manifiesto).
- `docs/technical-discovery.md` (líneas 374-434) — párrafos del Modo cocina y del aviso de sistema
  (las frases "sin persistencia" y "sin `foreground service`" hay que actualizar).
- `docs/risks-and-open-questions.md` (líneas 126-134, 175-176, 223-232) — "Later / Not MVP" y las dos
  filas de riesgo del aviso, incluida "tocar la notificación **no** devuelve al paso si el sistema mató la
  actividad o el proceso" (esta feature la resuelve).
- `docs/build-brief.md` (líneas 38-47) — Non-Goal "Notificaciones y recordatorios" ya enmendado de forma
  acotada para el aviso de fin.
- `docs/domain-model.md` (secciones `Core Concepts`, `States and Lifecycles`) — no hay entrada de Modo
  cocina (nunca se persistió).
- `DESIGN.md` — reglas de no inventar pantallas/flujos y el registro de excepciones de capturas.

### Existing Patterns To Follow

- **Lógica pura + adaptador Android delgado**: decisiones sin `android.*`/`androidx.*` en `ui.cooking`;
  plataforma en `data.cooking`.
- **Costura por parámetro con default no-op** (`CookingAlarm`/`NoOpCookingAlarm`,
  `NotificationPermissionPrompt`, `DishImageSettings`): previews, tests y composiciones sin cableado
  compilan y no hacen nada.
- **Instantes inyectados, reloj nunca en lo puro** (`tick(deltaMillis)`, `nowMillis`): los tests JVM son
  deterministas.
- **Degradación silenciosa** (ADR 0005): las costuras "no lanzan"; un fallo de plataforma nunca rompe la
  UI ni el temporizador.
- **Una fila con columna JSON en base propia** (patrón `suggestion_batch` + `RecipeJson`), con
  `exportSchema = false` y `version = 1`.
- **Microcopy como constantes puras de Kotlin**, no `strings.xml` (el repo no lo usa para UI).
- **Tests JUnit 4 con nombres en español** y casos negativos; **Robolectric** (`@Config(sdk = [33])`) solo
  para framework y Room en memoria; **checks del manifiesto/`res` en JVM** con `AppResourceFiles`.
- **Sin dependencias nuevas**; `init.sh` sin cambios.

### Current Gaps

- **No hay ningún servicio** en el manifiesto ni en el código, ni los permisos `FOREGROUND_SERVICE` /
  `FOREGROUND_SERVICE_SPECIAL_USE`.
- `CookingNotifications` solo construye la notificación de **fin** (id `1001`, `autoCancel`, sin
  `ongoing`): no hay notificación persistente ni cuenta atrás.
- **El Modo cocina no se persiste** (`docs/specs/cooking-mode.md`, Non-Goal): no existe ningún almacén, así
  que la sesión se pierde si muere el proceso o se destruye la actividad.
- `CookingModeScreen` construye su estado con `rememberSaveable { CookingModeState.initial(recipe) }`: **no
  hay forma** de abrirlo en un paso concreto.
- Los `Intent` de contenido **no llevan ningún extra** y `MainActivity` **no** tiene `onNewIntent`: nada
  distingue "abrir la app" de "volver del temporizador".
- No hay infraestructura de test para "el manifiesto declara X" en `data/cooking` (sí existe el helper
  `AppResourceFiles` y las propiedades de sistema, que se pueden reutilizar tal cual).

## Technical Approach

Tres capas, con el mínimo cambio en lo aceptado: **sesión pura + costura**, **adaptadores Android**
(servicio + notificaciones + alarma + Room) y **cableado Compose**.

### 1. `ui/cooking/CookingSession.kt` — la sesión, pura (sin Android ni Compose)

```kotlin
/** Foto de la sesión del Modo cocina: la receta, el paso y el temporizador. Puro y serializable. */
data class CookingSession(
    val recipe: Recipe,
    val stepIndex: Int,
    val completedOrders: Set<Int>,
    val timer: CookingTimerState,
    /** Instante de fin en `ELAPSED_REALTIME`; **solo** no-nulo En marcha. */
    val endAtElapsedMillis: Long?,
) {
    val isRunning: Boolean get() = timer.status == CookingTimerStatus.RUNNING
    /** `true` si el toque en la notificación debe devolver al paso (En marcha o Pausado). */
    val isResumable: Boolean get() = isRunning || timer.status == CookingTimerStatus.PAUSED

    fun mode(): CookingModeState            // CookingModeState(stepIndex, recipe.steps.size, completedOrders, timer)
    fun resumedMode(nowMillis: Long): CookingModeState   // recalcula lo que queda desde el instante de fin
    fun projectedEndMillis(nowMillis: Long): Long?       // now + remaining si En marcha; null si no

    companion object {
        fun of(mode: CookingModeState, recipe: Recipe, nowMillis: Long): CookingSession
    }
}
```

Reglas **deterministas** (todas con el instante inyectado; nada de reloj):

- `of(...)`: `endAtElapsedMillis = now + timer.remainingMillis` **solo** si `timer.status == RUNNING` y
  `remainingMillis > 0`; en Inactivo, Pausado y Terminado es `null` (nada que rearrancar; cubre "pausar/
  reiniciar/cambiar de paso/llegar a cero lo paran", Scenario 3).
- `resumedMode(now)`: en `RUNNING` recalcula `remaining = (endAtElapsedMillis - now).coerceAtLeast(0)` y
  devuelve `RUNNING` (o **`FINISHED`** con `remaining = 0` si el instante ya pasó, Scenario 6); si
  `endAtElapsedMillis` fuera `null` (fila corrupta) degrada al `remainingMillis` persistido **sin lanzar**;
  en `PAUSED`/`IDLE`/`FINISHED` reconstruye lo persistido tal cual.
- `isResumable`: En marcha o Pausado (un Terminado se reanuda solo si el usuario viene de la notificación,
  Scenario 8).

### 2. `ui/cooking/CookingSessionStore.kt` — costura del almacén (pura)

```kotlin
/** Almacén de la sesión del Modo cocina. Contrato: **no lanza** (degradación silenciosa, ADR 0005). */
interface CookingSessionStore {
    suspend fun load(): CookingSession?      // null = no hay sesión
    suspend fun save(session: CookingSession)
    suspend fun clear()
}

/** Default **sin efecto**: nada se guarda y nada se reanuda (previews y composiciones sin cableado). */
object NoOpCookingSessionStore : CookingSessionStore {
    override suspend fun load(): CookingSession? = null
    override suspend fun save(session: CookingSession) = Unit
    override suspend fun clear() = Unit
}
```

### 3. `data/cooking/` — almacén Room (una fila, base propia)

- `CookingSessionEntity` (`@Entity(tableName = "cooking_session")`, `@PrimaryKey id = 1`): `recipeJson`
  (columna JSON con `RecipeJson.encode`), `stepIndex`, `completedOrders` (CSV, mismo criterio que el
  `Saver` aceptado), `timerStatus` (nombre del enum), `totalMillis`, `remainingMillis`,
  `endAtElapsedMillis` (`Long?`).
- `CookingSessionDao`: `get()`, `upsert(entity)` (`REPLACE` ⇒ una sola fila), `deleteAll()`.
- `CookingSessionDatabase`: `cooking_session.db`, `version = 1`, `exportSchema = false` (patrón
  `SuggestionBatchDatabase`; base propia para no migrar las aceptadas).
- `CookingSessionMappers`: `toDomain()`/`toEntity()` con `RecipeJson`.
- `RoomCookingSessionStore(dao)` : `CookingSessionStore` — `load()`, `save()`, `clear()` con `runCatching`
  (nunca lanza; una fila ilegible se comporta como "sin sesión").

### 4. `data/cooking/CookingTimerService.kt` — el servicio en primer plano

- `: Service()`; `onCreate`: `CookingNotifications.ensureChannel(this)` y un
  `CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())` propio (se cancela en `onDestroy`).
- `onStartCommand(intent, …)`: lee `EXTRA_END_AT_ELAPSED_MILLIS` (Long, **único** dato que viaja);
  `startForeground(ONGOING_NOTIFICATION_ID, CookingNotifications.ongoing(this, wallEnd, contentIntent))`
  **inmediato** (dentro de los 5 s que exige el framework) y programa el único despertar:
  `delay((endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0))` y, al vencer,
  `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`. Devuelve **`START_NOT_STICKY`**.
- `wallEnd` (para el cronómetro, que se expresa en hora de pared): `System.currentTimeMillis() +
  (endAt - SystemClock.elapsedRealtime())`; la **extinción real** la decide siempre el cronómetro
  monotónico (`endAt`), no la hora de pared.
- El servicio **no** publica el "¡Listo!" (eso es de la alarma aceptada) ni cambia ningún estado: es un
  **pintor** de la cuenta atrás.
- `onDestroy`: cancela el scope. `onBind`: `null`. Si `END_AT` falta o el proceso se recrea sin intent
  (`START_NOT_STICKY`), el servicio se apaga sin más (no lanza).
- No se usa `ServiceCompat.startForeground` (no existe en `core 1.10.1`): el `startForeground(id,
  notification)` de dos argumentos usa el tipo declarado en el manifiesto, que es lo que se quiere.

### 5. `data/cooking/CookingNotifications.kt` — la notificación persistente (se **añade**)

- `ONGOING_NOTIFICATION_ID = 1000` (el `1001` aceptado sigue siendo el del fin).
- `ONGOING_TITLE = "Cocina en marcha"`; la **cuenta atrás la pinta el sistema**: `setUsesChronometer(true)`
  + `setChronometerCountDown(true)` + `setWhen(wallEnd)`, con `setOngoing(true)`,
  `setOnlyAlertOnce(true)`, `setSilent(true)`, `setCategory(NotificationCompat.CATEGORY_STOPWATCH)`,
  `smallIcon = R.drawable.ic_notification_timer`, el mismo canal `cooking_timer` y **sin**
  `contentText`/`extras` propios (nada de receta, ni claves, ni pasos).
- `ongoing(context, endWallClockMillis, contentIntent): Notification` (función **pura de Android**: entra
  el instante ya calculado, de modo que el test puede fijarlo y comprobarlo).
- El `PendingIntent` de contenido (compartido con la notificación de fin) apunta a `MainActivity` con
  `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP` y añade **solo**
  `EXTRA_RESUME_COOKING = true` (booleano): nada de receta ni de paso. La notificación de fin aceptada no
  cambia salvo ese extra.

### 6. `data/cooking/AndroidCookingAlarm.kt` — el adaptador suma la pata del servicio

`apply(command)` conserva **tal cual** la alarma aceptada y añade el servicio (mismo `requestCode` fijo y
misma idempotencia):

- `Schedule(at)`: `ContextCompat.startForegroundService(context, CookingTimerService.intent(context, at))`
  **y** `setExactAndAllowWhileIdle`/`setAndAllowWhileIdle` como hoy. (El arranque ocurre siempre desde una
  interacción del usuario con la app en primer plano —"Iniciar"/"Reanudar"—, así que no aplica la
  restricción de arranque en segundo plano.)
- `Cancel`: `context.stopService(...)` **y** `alarmManager.cancel(pi)` como hoy (el `Cancel` del fin deja
  sin cuenta atrás y la notificación `1000` desaparece; la notificación `1001` ya mostrada **no** se
  retira, decisión 9 aceptada).
- Todo sigue envuelto en `runCatching` ("no lanza").

### 7. Cableado Compose

- `CookingModeScreen(recipe, onClose, modifier, alarm, notificationPermission, session =
  NoOpCookingSessionStore, resume: CookingSession? = null)`:
  - **Estado inicial** = `resume?.resumedMode(now) ?: CookingModeState.initial(recipe)` (así el overlay
    abre en el paso exacto).
  - **Persistencia**: `LaunchedEffect(state.stepIndex, state.timer.status, state.completedOrders) {
    session.save(CookingSession.of(state, recipe, SystemClock.elapsedRealtime())) }` — **una** escritura
    por cambio significativo (arrancar, pausar, reanudar, reiniciar, cambiar de paso, marcar, llegar a
    cero), **nunca** una por `tick` (lo que queda se recalcula al reanudar).
  - **Reanudación**: si `resume` es En marcha, aplicar una vez
    `CookingAlarmPolicy.onTimerChange(previous = timer con `status = IDLE`, next = timer, now)` ⇒
    `Schedule(now + remaining)`, para que el servicio (muerto con el proceso) vuelva a pintar la cuenta
    atrás; `previousTimer` arranca ya igual a `state.timer` para que el efecto de transiciones no lo repita.
  - **Salida**: `onClose` (Atrás/Terminar) lanza `session.clear()` **además** de `onClose()`; el
    `DisposableEffect` de `onDispose` **no** borra nada (una muerte de actividad/rotación no debe perder la
    sesión). El reloj de la UI, el "¡Listo!" en pantalla y la UI aceptada **no** cambian.
- `ui/MainShell.kt`: parámetros nuevos `cookingSession: CookingSessionStore = NoOpCookingSessionStore` y
  `resumeRequest: Int = 0`; un `LaunchedEffect(resumeRequest)` que, si es `> 0`, hace
  `cookingSession.load()` y, con una sesión **resumible**, abre el overlay en ese paso
  (`cookingRecipe = session.recipe`, `resumeMode = session.resumedMode(SystemClock.elapsedRealtime())`);
  `resumeMode` se limpia en el `onClose` del modo.
- `ui/SazonApp.kt`: reenvía `cookingSession` y `resumeRequest` (mismo camino por parámetro que
  `cookingAlarm`).
- `MainActivity.kt`: construye `RoomCookingSessionStore(CookingSessionDatabase.build(applicationContext)
  .cookingSessionDao())`, expone un estado de Compose `resumeRequest: Int` que **incrementa** cuando el
  `Intent` (de `onCreate` o de `onNewIntent`, con `setIntent`) trae `EXTRA_RESUME_COOKING`, y pasa ambos a
  `SazonApp`. `CookingNotifications.ensureChannel` y el lanzador del permiso se mantienen.

### 8. `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
…
<service
    android:name=".data.cooking.CookingTimerService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Temporizador de cocina en curso" />
</service>
```

Nombres verificados en `android.jar` 36.1: el literal `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` es
`PackageManager.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` vale
`1073741824` y `Service.STOP_FOREGROUND_REMOVE` existe (API 24+).

`android:stopWithTask` queda en su valor por defecto (`true`): quitar la tarea de recientes detiene el
servicio (documentado en `Unknowns`). Permisos y `<receiver>` aceptados **sin cambios**.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/cooking/CookingSession.kt` — sesión pura (`of`, `resumedMode`,
  `projectedEndMillis`, `isResumable`, `mode`); **sin** imports de Android/Compose/Room/reloj.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingSessionStore.kt` — costura + `NoOpCookingSessionStore`
  (pura).
- `app/src/main/java/com/sazon/app/data/cooking/CookingSessionEntity.kt`,
  `CookingSessionDao.kt`, `CookingSessionDatabase.kt`, `CookingSessionMappers.kt`,
  `RoomCookingSessionStore.kt` — persistencia de una fila (patrón `suggestion_batch`).
- `app/src/main/java/com/sazon/app/data/cooking/CookingTimerService.kt` — servicio en primer plano.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingSessionTest.kt` — sesión pura (**la verificación
  exigida**).
- `app/src/test/java/com/sazon/app/data/cooking/CookingTimerServiceTest.kt` — servicio (Robolectric,
  `ShadowService`).
- `app/src/test/java/com/sazon/app/data/cooking/RoomCookingSessionStoreTest.kt` — persistencia
  (Robolectric + Room en memoria).
- `app/src/test/java/com/sazon/app/data/cooking/CookingTimerServiceManifestTest.kt` — el manifiesto
  declara permisos, `<service>`, tipo y `<property>` (JVM, `AppResourceFiles` + `android.manifest.path`;
  **sin** tocar `app/build.gradle.kts`).
- `docs/adr/0008-cooking-timer-foreground-service.md` — decisión durable.

**Modificar**

- `app/src/main/java/com/sazon/app/data/cooking/CookingNotifications.kt` — notificación persistente
  (`ONGOING_NOTIFICATION_ID`, `ONGOING_TITLE`, `ongoing(...)`) y el extra de reanudación en el contenido.
- `app/src/main/java/com/sazon/app/data/cooking/AndroidCookingAlarm.kt` — arrancar/parar el servicio
  además de la alarma aceptada (misma idempotencia, mismo "no lanza").
- `app/src/main/java/com/sazon/app/data/cooking/CookingAlarmReceiver.kt` — el `Intent` de contenido gana
  solo el extra de reanudación.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — parámetros `session`/`resume`,
  persistencia por cambio significativo, `clear()` al salir y rearme al reanudar (UI, reloj y microcopy
  intactos).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt`, `ui/SazonApp.kt`, `MainActivity.kt` — costura nueva,
  `resumeRequest` y `onNewIntent`.
- `app/src/main/AndroidManifest.xml` — permisos de `FOREGROUND_SERVICE`, `<service>` con tipo y
  `<property>`.
- `app/src/test/java/com/sazon/app/data/cooking/CookingNotificationsTest.kt` — casos de la notificación
  persistente y del extra de reanudación (los casos aceptados del fin se mantienen).
- `docs/adr/0007-cooking-timer-alarm.md` — nota: alternativas 3 y 4 **revisadas** por ADR 0008; el resto
  no cambia.
- `docs/build-brief.md`, `docs/technical-discovery.md`, `docs/domain-model.md`,
  `docs/risks-and-open-questions.md`, `DESIGN.md` — ver `Durable Documentation Impact`.
- `feature_list.json` y `PROGRESS.md` — **al implementar** (estado `passing` + evidencia), no en esta
  sesión.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`
(salvo leer `RecipeJson`), la Taxonomía, `seed-recipes.json`, `assets/**`, las bases Room aceptadas,
`ui/theme/**`, `ui/detail/**`, `res/drawable/ic_notification_timer.xml`, los `mipmap` de lanzador,
`CookingAlarm.kt`, `CookingTimerState.kt`, `CookingModeState.kt`, `CookingModeFormat.kt`,
`NotificationPermission.kt`, `NotificationPermissionPrompt.kt`, `AndroidNotificationPermissionPrompt.kt` y
`docs/design/concepts/**`.

## Visual Design Impact

- UI involved: **parcialmente** — no hay pantalla nueva ni cambio de la pantalla del Modo cocina; lo
  visible son **dos notificaciones del sistema** (la persistente con la cuenta atrás y la de fin
  aceptada).
- Design source: `DESIGN.md` sigue siendo la fuente de verdad de las **pantallas**, pero **no** cubre
  notificaciones de sistema (no hay tokens y **no** se inventan). El único activo visual es el small icon
  **monocromo** ya aceptado (`ic_notification_timer`, el color lo impone el sistema al teñirlo).
- Screens or states affected: ninguna pantalla; el Modo cocina conserva su estructura, su microcopy y sus
  tokens. La novedad de comportamiento es un **estado transitorio**: el modo abierto en un paso restaurado
  (misma UI, otro punto de entrada).
- New design artifact required: **no** — no hay captura de Stitch y **no** se crea
  `docs/design/concepts/cooking-timer-foreground-service/`; se registra la excepción en `DESIGN.md`
  (misma clase que `cooking-timer-alarm`, #77, #85, #86 y #91).
- Tarea del implementer: no inventar estilos; el microcopy de la notificación persistente es una constante
  probada (`ONGOING_TITLE`) y la cuenta atrás la pinta el sistema.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace una regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/adr/0008-cooking-timer-foreground-service.md`: **create** — el `foreground service` como dueño de
  la **cuenta atrás visible**, la alarma one-shot aceptada como **única** dueña del "¡Listo!",
  `specialUse`, la sesión persistida (Room, una fila) y la reanudación **explícita** desde la notificación;
  con las alternativas descartadas (sustituir la alarma por el servicio, `shortService`, bucle de texto por
  segundo, persistir en `SharedPreferences`, reanudar siempre al abrir la app).
- `docs/adr/0007-cooking-timer-alarm.md`: **update** — nota de revisión en las alternativas 3 y 4 (y, si
  procede, en el `Unknowns` del "estado de arranque"), sin reescribir decisiones ni evidencia.
- `docs/technical-discovery.md`: **update** — nueva integración (servicio en primer plano + tipo
  `specialUse` + permisos `FOREGROUND_SERVICE*`, sesión Room propia `cooking_session.db`, reanudación por
  `EXTRA_RESUME_COOKING`) y corrección de las frases "sin persistencia" y "sin `foreground service`".
- `docs/domain-model.md`: **update** — entrada corta de la **sesión del Modo cocina** (qué guarda y sus
  estados: En marcha/Pausado reanudables; Terminado, no) en `States and Lifecycles`; **no** es una entidad
  de dominio nueva ni cambia ninguna relación.
- `docs/build-brief.md`: **update** — ampliación acotada del Non-Goal: entra la **cuenta atrás** del
  temporizador en una notificación persistente y la **reanudación del paso**; los recordatorios
  programados y las notificaciones de sugerencias/recetas **siguen fuera**.
- `docs/risks-and-open-questions.md`: **update** — cerrar/actualizar la fila "tocar la notificación no
  devuelve al paso" (resuelta vía sesión persistida) y añadir riesgos nuevos (tipo `specialUse` y permisos
  de primer plano; muerte del proceso ⇒ desaparece la cuenta atrás pero el "¡Listo!" llega; canal de
  importancia alta reutilizado).
- `DESIGN.md`: **update** — nota de la excepción de capturas y de las dos notificaciones del sistema.
- `CONTEXT.md`: **not needed** — "Modo cocina" y "Aviso" ya existen; "sesión" se usa en su sentido
  corriente.

## Implementation Plan

1. Crear `CookingSession.kt` + `CookingSessionStore.kt` y `CookingSessionTest` (JVM puro, vectores
   dorados de `of`/`resumedMode`/proyección del fin y el "aviso una sola vez" con `CookingAlarmPolicy`).
2. Crear la persistencia Room (`CookingSessionEntity`/`Dao`/`Database`/`Mappers`/`RoomCookingSessionStore`)
   y `RoomCookingSessionStoreTest` (Robolectric + Room en memoria).
3. Crear `CookingTimerService.kt` y la notificación persistente en `CookingNotifications` (constantes,
   `ongoing(...)`, extra de reanudación) con `CookingTimerServiceTest` y los casos nuevos de
   `CookingNotificationsTest`.
4. Añadir permisos, `<service>` y `<property>` al manifiesto y cubrirlo con
   `CookingTimerServiceManifestTest` (JVM, `AppResourcesFiles`).
5. Sumar la pata del servicio a `AndroidCookingAlarm` y el extra al `CookingAlarmReceiver`; cablear
   `CookingModeScreen` (persistencia + rearme al reanudar + `clear` al salir), `MainShell`/`SazonApp`
   (reanudación) y `MainActivity` (almacén, `resumeRequest`, `onNewIntent`).
6. `bash init.sh` en verde y actualizar ADR 0008, ADR 0007, `docs/build-brief.md`,
   `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md` y `DESIGN.md`.

## Implementation Tasks

- [x] Crear `CookingSession` (`of`/`resumedMode`/`projectedEndMillis`/`isResumable`/`mode`) **sin** imports
      de Android/Compose ni lecturas del reloj.
- [x] Crear `CookingSessionStore` + `NoOpCookingSessionStore` (contrato "no lanza").
- [x] Escribir `CookingSessionTest` (JUnit 4, nombres en español): `of` proyecta
      `endAtElapsedMillis = now + remaining` al arrancar/reanudar y `null` en Inactivo/Pausado/Terminado;
      `resumedMode` recalcula el restante desde el instante de fin (y da `FINISHED`/`0` si ya pasó, incluido
      el caso "muerte a mitad"); degradación defensiva con `endAtElapsedMillis = null`; el paso, el marcado
      y la receta viajan intactos; determinismo (mismo `now` ⇒ mismo resultado) y `now` inyectado;
      **"aviso una sola vez"**: en Terminado la sesión no proyecta fin y `CookingAlarmPolicy.onTimerChange`
      devuelve `Cancel` en `RUNNING→FINISHED` y `None` en los `tick` siguientes.
- [x] Crear entidad, DAO, base (versión 1, `exportSchema = false`, `cooking_session.db`), mappers y
      `RoomCookingSessionStore` (una fila, `REPLACE`, `clear()` ⇒ `null`).
- [x] Escribir `RoomCookingSessionStoreTest` (Robolectric `@Config(sdk = [33])`, Room en memoria): ida y
      vuelta exacta con `nutrition`/`ingredientId` opcionales, una sola fila al sobrescribir, `clear()` ⇒
      `null`, y `save`/`load` **no lanzan**.
- [x] Crear `CookingTimerService` (`onStartCommand` con `startForeground` inmediato y `START_NOT_STICKY`,
      un solo `delay` hasta el instante de fin, `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf`,
      `onDestroy` cancelando el scope, `onBind` nulo; sin publicar el "¡Listo!").
- [x] Añadir a `CookingNotifications`: `ONGOING_NOTIFICATION_ID = 1000`, `ONGOING_TITLE`,
      `ongoing(context, endWallClockMillis, contentIntent)` (cronómetro descendente, `ongoing`,
      `onlyAlertOnce`, `silent`, `CATEGORY_STOPWATCH`, small icon, canal `cooking_timer`) y la constante
      `EXTRA_RESUME_COOKING`.
- [x] Escribir `CookingTimerServiceTest` (Robolectric + `ShadowService`): `startForeground` con el id
      `1000` y la notificación persistente (y **no** con el `1001`), el servicio se detiene sin excepción al
      vencer, un `Intent` sin extra no rompe. Si algún shadow no expone lo necesario, documentarlo en la
      evidencia.
- [x] Extender `CookingNotificationsTest`: la persistente se construye sin excepción, es `ongoing`, con
      `when` igual al instante de fin, sin `contentText`/`extras` de receta; el `Intent` de arranque lleva
      **solo** el instante de fin y los de contenido **solo** el flag de reanudación (sin receta, sin
      pasos, sin claves); los casos aceptados del fin siguen verdes.
- [x] Añadir al manifiesto `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` y el `<service>` con
      `exported="false"`, `foregroundServiceType="specialUse"` y el `<property>` del subtipo.
- [x] Escribir `CookingTimerServiceManifestTest` (JVM con `AppResourceFiles.parse(AppResourceFiles
      .manifestFile)`): el servicio existe una sola vez, con el nombre, `exported="false"` y el tipo
      `specialUse`; están los dos permisos nuevos; el `<receiver>` y los permisos aceptados siguen ahí.
- [x] Sumar la pata del servicio a `AndroidCookingAlarm.apply` (`Schedule` ⇒ `startForegroundService` +
      alarma; `Cancel` ⇒ `stopService` + `alarmManager.cancel`) sin tocar la alarma aceptada ni el "no
      lanza"; añadir el extra de reanudación al `Intent` de contenido del `CookingAlarmReceiver`.
- [x] Cablear `CookingModeScreen`: parámetros `session`/`resume`, estado inicial restaurado, persistencia
      **por cambio significativo** (`stepIndex`/`timer.status`/`completedOrders`, nunca por `tick`), rearme
      del trabajo del sistema al reanudar una sesión En marcha y `clear()` en `onClose` (el
      `DisposableEffect` **no** borra).
- [x] Cablear `MainShell` (parámetros + `LaunchedEffect(resumeRequest)` que carga la sesión y abre el paso)
      y `SazonApp`; en `MainActivity`, construir el almacén y convertir `EXTRA_RESUME_COOKING` (de
      `onCreate` y de `onNewIntent`) en el estado `resumeRequest`.
- [x] `bash init.sh` en verde y actualizar ADR 0008, ADR 0007, `docs/build-brief.md`,
      `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md`,
      `DESIGN.md` y la evidencia en `feature_list.json`/`PROGRESS.md` (**sin** commit/push/PR).

## Verification Plan

- `bash init.sh` → **esperado**: código 0 (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`,
  sin procesos largos) y **`init.sh` sin cambios**.
- `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*" --tests
  "com.sazon.app.data.cooking.*"` → **esperado**: BUILD SUCCESSFUL con las suites de `cooking` que
  incluyen las **cuatro** nuevas (`CookingSessionTest` — **verificación exigida**,
  `CookingTimerServiceTest`, `RoomCookingSessionStoreTest`, `CookingTimerServiceManifestTest`) y sin
  suites aceptadas modificadas más allá de `CookingNotificationsTest` (casos **añadidos**).
- `./gradlew test` (vía `init.sh`) → **esperado**: baseline de partida **109 suites / 830 tests**
  (evidencia de la sesión 136, en `PROGRESS.md`); el implementer debe **re-derivarla** en `HEAD` con
  `git grep -c '@Test' -- app/src/test` y dejar registrado el delta. 0 fallos/errores/saltados.
  `CookingAlarmPolicyTest`, `CookingTimerStateTest`, `CookingModeStateTest`, `CookingModeFormatTest`,
  `NotificationPermissionPolicyTest` y `AndroidCookingAlarmTest` **sin cambios** (este último sigue
  cubriendo la alarma aceptada; si la nueva pata del servicio exigiera casos propios, se añaden **sin**
  alterar los aceptados y se registra).
- `./gradlew lint` → **esperado**: 0 errores y **0 warnings nuevos bajo `app/src`** (los 13 preexistentes se
  mantienen). Riesgo conocido: un aviso nuevo por el tipo de primer plano/permisos; el remedio es
  resolverlo con el estilo del repo o un `tools:ignore`/`lint.xml` **acotado y justificado** (precedente
  `app/lint.xml` de `app-launcher-icon`) y **registrar el hallazgo**.
- Inspección (greps propios del implementer y del validator):
  - `ui/cooking/CookingSession.kt` y `CookingSessionStore.kt` **no** importan `android.*`/`androidx.*` ni
    leen el reloj (0 `System.currentTimeMillis`/`SystemClock`); `CookingAlarm.kt`, `CookingTimerState.kt`,
    `CookingModeState.kt`, `CookingModeFormat.kt`, `NotificationPermission*.kt` **sin diff**.
  - `Service`/`startForeground`/`AlarmManager`/`NotificationManager` viven **solo** bajo
    `data/cooking/` (más el manifiesto y `MainActivity`, que solo consulta el `Intent`).
  - El `Intent` de arranque del servicio lleva **solo** el instante de fin; los `Intent` de contenido
    **solo** el booleano de reanudación: ninguna `Recipe`, ingrediente, paso, `notes` ni clave.
  - La sesión se persiste por **cambio significativo** (no hay escrituras por `tick`) y `onDispose` **no**
    la borra.
  - `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, las bases
    Room aceptadas, la Taxonomía, `seed-recipes.json`, `assets/**`, `ui/theme/**` y
    `ic_notification_timer.xml` intactos; **sin dependencias nuevas**.
  - El manifiesto mantiene el `<receiver>` aceptado y los permisos previos.
  - No se crea `docs/design/concepts/cooking-timer-foreground-service/`.
- Manual (emulador, lo revisa la persona; `AGENTS.md` no automatiza UI ni el sistema de notificaciones):
  1. Receta → "Empezar a cocinar" → paso con duración → "Iniciar": aparece la notificación persistente
     "Cocina en marcha" con la **cuenta atrás decreciendo**.
  2. Minimizar y **bloquear** el móvil: el contador sigue decreciendo en la notificación y en la pantalla de
     bloqueo.
  3. Al llegar a cero: **suena y vibra** ("¡Listo!") y la notificación persistente desaparece (sin
     duplicados ni avisos repetidos).
  4. Tocar la notificación (persistente y de fin) con la app en segundo plano: vuelve **al paso exacto**;
     repetir **cerrando antes la app** (proceso muerto): vuelve al paso con el tiempo restante correcto o
     con el "¡Listo!" si ya pasó.
  5. "Pausar", "Reiniciar", cambiar de paso y salir ("Atrás"/"Terminar") **detienen** el servicio (la
     notificación persistente desaparece) sin borrar la sesión salvo en la salida.
  6. Denegar `POST_NOTIFICATIONS`: el Modo cocina y su "¡Listo!" en pantalla siguen intactos (sin
     notificación persistente) y no hay crash.
  7. Abrir la app desde el lanzador (sin tocar notificación): **no** se abre el Modo cocina solo.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y `AGENTS.md` prohíbe automatizar UI de
emulador: la sesión (lógica pura) se cubre en JVM, la notificación/servicio/almacén en
Robolectric/manifiesto y el comportamiento del sistema (pantalla bloqueada, Doze, sonido, muerte de
proceso) lo revisa la persona.

## Evidence To Capture

- Salida del gate `bash init.sh` (código 0) y recuento propio de los XML: suites/tests totales, 0
  fallos/errores/saltados y el delta respecto a la baseline re-derivada en `HEAD` (partida conocida: 109
  suites / 830 tests).
- Salida de la tirada focal con las cuatro suites nuevas (nombre y nº de tests de cada una) y confirmación
  de que las suites aceptadas de `cooking` no cambian (salvo los casos **añadidos** en
  `CookingNotificationsTest`).
- Informe de lint: 0 errores y 13 warnings preexistentes, **0 bajo `app/src`** (o el remedio aplicado y su
  justificación).
- Inspección registrada: pureza de `CookingSession`/`CookingSessionStore`, confinamiento de
  `Service`/`startForeground` a `data/cooking`, `Intent` mínimo, persistencia por cambio significativo,
  `onDispose` sin borrado, build/lockfile/`init.sh`/dominio/Docs aceptadas intactos, manifiesto con el
  servicio y los permisos, sin capturas nuevas.
- Resultado manual del emulador (puntos 1-7), con la limitación de que no es automatizable.
- Anotación en `feature_list.json`/`PROGRESS.md` de la evidencia y del estado `passing` (al implementar;
  **no** en esta sesión).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **El `foreground service` es dueño de la cuenta atrás visible; la alarma one-shot aceptada sigue siendo
  la única dueña del "¡Listo!"** (no se sustituye). Motivos: (1) es el **único** mecanismo que sobrevive a
  la muerte del proceso, y el "¡Listo!" es justo el caso que debe llegar siempre; (2) con **un solo**
  publicador no hay sonido/vibración duplicados ni carrera entre dos relojes, y se conserva tal cual el
  comportamiento aceptado (id fijo `1001`, disparo único, degradación a alarma inexacta); (3) la
  alternativa "el servicio también avisa al llegar a cero" chocaría con el `Cancel` que la política emite
  en `RUNNING→FINISHED` (cuando el `tick` de la app llega a cero en primer plano) y añadiría una carrera
  difícil de probar en JVM. `USE_EXACT_ALARM` y el receptor conservan su propósito.
- **La política y la costura aceptadas no cambian**: `CookingAlarmCommand` ya transporta exactamente lo
  que el servicio necesita (`Schedule(endAtMillis)`/`Cancel`) y sus 15 tests siguen siendo la garantía
  JVM; el adaptador es lo único que gana la pata del servicio.
- **La notificación persistente solo existe En marcha**: Pausar/Reiniciar/cambiar de paso/salir emiten
  `Cancel` y el servicio se detiene (coincide con la verificación manual de la entrada: "pausar/reiniciar/
  salir detienen el servicio").
- **La cuenta atrás la pinta el sistema** (`setUsesChronometer(true)` + `setChronometerCountDown(true)` +
  `setWhen(...)`): cero despertares por segundo, sin deriva (el instante de fin es monotónico) y sin
  depender de que el proceso esté despierto para repintar texto.
- **El `Intent` de arranque del servicio lleva solo el instante de fin** y los de contenido **solo** un
  booleano de reanudación: se mantiene el espíritu de la decisión 7 aceptada (ni receta, ni pasos, ni
  claves cruzan al sistema). El `PendingIntent` de la **alarma** sigue sin extras.
- **La sesión se persiste en Room, una fila, base propia** (`cooking_session.db`, `version = 1`,
  `exportSchema = false`): mismo patrón que `suggestion_batch` (columna JSON con `RecipeJson`) y sin
  obligar a migrar las bases aceptadas. `SharedPreferences` se descarta para una receta completa.
- **Se persiste por cambio significativo, no por `tick`**: basta con el instante de fin para recalcular el
  resto al reanudar, así que no hay una escritura por segundo.
- **`onDispose` no borra la sesión; "Atrás"/"Terminar" sí**: una muerte de actividad/rotación no debe
  perder la sesión (es justo el caso a cubrir), mientras que salir del modo es una decisión explícita del
  usuario.
- **La reanudación es explícita**: solo el `Intent` con `EXTRA_RESUME_COOKING` (notificación persistente o
  de fin) abre el Modo cocina en su paso; un arranque normal **no** lo abre, así que una sesión antigua no
  secuestra el arranque de la app y no hace falta ventana de frescura ni reloj de pared.
- **Reanudar una sesión En marcha rearma el trabajo del sistema** con la misma decisión pura
  (`CookingAlarmPolicy.onTimerChange` tratando la reanudación como `IDLE→RUNNING`), porque la sesión puede
  sobrevivir a un proceso cuya alarma/servicio ya no existen.
- **`specialUse`** como tipo de primer plano (no hay tipo de "temporizador" y `shortService` está limitado a
  ~3 minutos) con su `<property>`; los dos permisos nuevos son **normales** (sin diálogo) y la app no se
  publica.
- **Sin dependencias nuevas**: `Service`, `NotificationCompat`/`NotificationManagerCompat`,
  `ContextCompat`, `AlarmManager` (aceptado), Room y coroutines ya están. `ServiceCompat.startForeground`
  **no** está en `core 1.10.1` (verificado en el aar de la caché), así que se usa el `startForeground(id,
  notification)` del framework con el tipo declarado en el manifiesto.

### Unknowns

- **Renderizado del cronómetro por el fabricante**: `setUsesChronometer` + `setChronometerCountDown` es el
  mecanismo estándar, pero si alguna capa (lock screen/OEM) no lo pintara, no habría cuenta atrás visible;
  se comprueba en la prueba **manual** y, si molesta, el remedio es una feature correctiva (bucle de texto
  por segundo o `RemoteViews`).
- **Canal reutilizado de importancia alta**: el canal `cooking_timer` es inmutable una vez creado y tiene
  sonido/vibración; la notificación persistente se publica con `setOnlyAlertOnce(true)`/`setSilent(true)`
  para **no** repetir alerta, pero **la primera** publicación podría alertar una vez (no se puede bajar la
  importancia de un canal existente). Se comprueba en la prueba manual; si molesta, una feature correctiva
  podría añadir un canal propio de importancia baja (sin tocar el aceptado).
- **Muerte del proceso / quitar la tarea**: el servicio es `START_NOT_STICKY` y `stopWithTask` queda por
  defecto (`true`), así que si el sistema mata el proceso o el usuario quita la tarea de recientes, la
  **cuenta atrás desaparece**; el "¡Listo!" sigue llegando (alarma) y el toque **sí** devuelve al paso
  (sesión persistida). Recuperar el servicio con `START_STICKY` exigiría releer la sesión desde el propio
  servicio y se deja fuera de alcance.
- **Reinicio del dispositivo**: no sobreviven ni la alarma ni el servicio (igual que el Modo cocina hoy);
  la sesión persistida quedaría obsoleta salvo que el usuario toque una notificación inexistente. Se acepta
  y se documenta (no se añade `RECEIVE_BOOT_COMPLETED`).
- **Deriva de la hora de pared**: el cronómetro se publica en hora de pared (`when`); si el reloj del
  dispositivo cambia mientras corre, la cuenta atrás mostrada puede desviarse respecto al fin real (que
  mide el cronómetro monotónico y la alarma). Cosmético y poco probable.
- **`specialUse` en Play**: aunque la app no se publica, si algún día se publicara, el subtipo exige
  justificación en la ficha de la consola.
- **Sesión ilegible o corrupta**: se degrada a "sin sesión" (no lanza); una sesión sin `recipe` válida no
  puede reanudarse y la app abre normal.
- **Peso de la base de datos**: la sesión guarda una copia JSON de la receta; se acepta (una fila) y se
  revisa en la prueba manual que el arranque no se resiente.
- **Acabado exacto** (aspecto del cronómetro en la pantalla de bloqueo, orden de las dos notificaciones
  mientras coexisten, tono/patrón): lo revisa la persona en el emulador; un defecto se corrige con una
  **feature correctiva** (AGENTS.md).

## Validator Checklist

- [ ] `CookingSession` es **pura** (0 imports `android.*`/`androidx.*`), **determinista** y **sin reloj**:
      proyecta `endAtElapsedMillis` al arrancar/reanudar y `null` en Inactivo/Pausado/Terminado;
      `resumedMode` recalcula el restante desde el instante de fin y da `FINISHED` si ya pasó; en Terminado
      `CookingAlarmPolicy` devuelve `Cancel` y después `None` (**"aviso una sola vez"**, Scenarios 1, 3 y 6,
      **verificación exigida**).
- [ ] El servicio se arranca En marcha y se **detiene** al pausar/reiniciar/cambiar de paso/salir, con la
      notificación persistente (id `1000`, `ongoing`, cronómetro descendente, sin datos de receta)
      (Scenarios 1, 2 y 4, **verificación exigida**).
- [ ] La sesión se **persiste** por cambio significativo y **no** en cada `tick`; `onDispose` no la borra y
      "Atrás"/"Terminar" sí; el toque (con proceso vivo o muerto) abre el **paso exacto** con el tiempo
      restante recalculado (Scenarios 5, 6 y 8, **verificación exigida**).
- [ ] Las notificaciones y los `Intent` exponen **solo** lo mínimo (título fijo + instante de fin; flag de
      reanudación; sin receta, pasos ni claves) y la notificación persistente y la de fin se construyen sin
      excepción (Scenario 9, **verificación exigida**).
- [ ] El manifiesto declara `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` y el `<service>` con
      `foregroundServiceType="specialUse"`, `exported="false"` y su `<property>`; el receptor y los permisos
      aceptados no cambian; la app funciona sin ningún temporizador en marcha (Scenario 10, **verificación
      exigida**).
- [ ] Permiso denegado ⇒ sin notificación persistente, sin crash, Modo cocina/cuenta atrás/"¡Listo!" en
      pantalla intactos y sesión igual de reanudable (Scenario 7).
- [ ] `CookingAlarmPolicy`/`CookingAlarmCommand`/`CookingAlarm`/`CookingTimerState`/`CookingModeState`/
      `CookingModeFormat`/`NotificationPermission*` quedan **sin diff**, y la alarma, el receptor, el canal,
      el id `1001` y el microcopy del fin siguen siendo los aceptados; `domain/**`, la Taxonomía,
      `seed-recipes.json`, `ui/theme/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml` e `init.sh`
      intactos; **sin dependencias nuevas**.
- [ ] Gate en verde (`bash init.sh` código 0) con la verificación exigida cubierta en JVM/Robolectric
      (casos negativos incluidos, no solo el camino feliz) y evidencia registrada (suites/tests/delta, lint
      con 0 warnings nuevos bajo `app/src`) en `feature_list.json`/`PROGRESS.md`.
- [ ] `docs/adr/0008-cooking-timer-foreground-service.md` (nuevo) y la nota de revisión en ADR 0007,
      `docs/build-brief.md`, `docs/technical-discovery.md`, `docs/domain-model.md`,
      `docs/risks-and-open-questions.md` y `DESIGN.md` quedan coherentes con lo implementado, y **no** se
      crea `docs/design/concepts/cooking-timer-foreground-service/`.
- [ ] El implementer **no** hizo commit/push/PR.
