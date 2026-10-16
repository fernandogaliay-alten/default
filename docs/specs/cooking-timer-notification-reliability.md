# Feature Implementation Spec: Notificación del temporizador fiable: reloj único, sin negativo y aviso puntual

## Source Feature

- `id` `cooking-timer-notification-reliability` · `area` `integration` · `status` `not_started` (al planificar)
- `depends_on`: `cooking-timer-foreground-service` (`accepted`) · `cooking-timer-finish-notification` (`accepted`)
- `source`: `feature_list.json:1795-1810` (`user_visible_behavior`, las **cuatro** verificaciones y las `notes`
  con el análisis de las cuatro causas `(a)`–`(d)` y el alcance acordado `(1)`–`(4)`, fuente de verdad del
  alcance)
- Correctiva pedida por la persona (2026-10-16) tras probar el servicio en primer plano en un dispositivo
  ColorOS. **No** reabre lo aceptado: lo **enmienda** donde el análisis lo exige (ADR 0008, decisión 1 y
  alternativa 1: la alarma deja de ser el **único** productor del "¡Listo!").

## Goal

Mientras corre el temporizador de un paso, la **cuenta atrás de la pantalla y la de la notificación** deben ir
sincronizadas porque **ambas derivan del mismo instante absoluto** (un reloj único); la notificación **nunca**
debe pintarse en negativo al llegar a cero; y el aviso **"¡Listo!"** debe publicarse **en el instante de fin**
(también con la app minimizada, el móvil bloqueado o tras un arranque en frío) sin retrasos y sin avisos de un
paso anterior durante el siguiente.

Hoy falla por cuatro causas independientes que el análisis de `feature_list.json:1809` fija literalmente: `(a)`
la UI resta **1000 ms fijos** por `tick` (`CookingModeScreen.kt:257-262` +
`CookingTimerState.tick(deltaMillis)`) mientras la notificación y la sesión usan un instante absoluto
(`endAtElapsedMillis`); `(b)` la retirada de la persistente (`1000`) depende de un `delay` "blando" del servicio
(`CookingTimerService.kt:92-99`), así que si llega tarde o el FGS se congela el cronómetro del sistema **cuenta
hacia arriba**; `(c)` el aviso lo publica la **alarma** (`AlarmManager`, que se difiere/coalesce) en vez del
servicio, que es quien conoce el instante exacto; `(d)` el ciclo del temporizador (alarma + servicio) se dispara
desde **efectos de Compose** en el hilo principal (`CookingModeScreen.kt:226-247`), de donde sale el retraso de
la primera cuenta atrás tras un arranque en frío.

El alcance acordado en las `notes` son cuatro items, que los escenarios y las tareas citan por su número: **(1)**
reloj único (la UI deriva `remaining = endAt - elapsedRealtime`), **(2)** el servicio publica el aviso de fin en su
instante exacto y la **alarma queda como respaldo** (idempotente, id fijo `1001`) —enmienda la decisión de ADR 0008
que dejaba el aviso "solo en manos de la alarma"—, **(3)** sin negativo (retirada en el instante exacto y `when`
recalculado al publicar) y **(4)** menos acoplamiento del arranque del servicio a la composición.

## Non-Goals

- **No** entra el número/título del paso en la notificación (es `cooking-timer-notification-step-label`, que
  **depende** de esta y toca los mismos ficheros: se ejecuta después) ni cambios de microcopy del aviso.
- **No** se mantiene vivo el servicio tras el fin, ni se crea un canal nuevo, ni se cambia el canal `cooking_timer`
  ni su importancia alta: la `1001` sigue siendo descartable (`autoCancel`, no `ongoing`).
- **No** se toca `CookingModeState`, `CookingModeFormat`, `CookingExitConfirmation`, la UI/microcopy del Modo
  cocina, `MainShell`, `SazonApp`, `MainActivity`, la persistencia Room (`CookingSessionEntity`/`Dao`/`Database`/
  `Mappers`/`RoomCookingSessionStore`), el manifiesto, permisos, build/lockfile, `init.sh` ni `scripts/**`.
- **No** se arreglan limitaciones preexistentes ya aceptadas: la rotación que cierra el Modo cocina, la `1001`
  obsoleta que sobrevive a "Reiniciar desde Terminado" o al cambio de paso (hallazgo **Low L1** de
  `docs/validations/cooking-timer-finish-notification.md`), el doble reloj de pared/monotónico del cronómetro del
  sistema, ni el rearme del trabajo del sistema tras una muerte de proceso sin tocar la notificación.
- **No** hay dependencias nuevas.

## Job Story

When pongo el temporizador de un paso y sigo cocinando con el móvil guardado, I want que la cuenta atrás de la
pantalla y la de la notificación digan lo mismo, que no se ponga en negativo al llegar a cero y que el "¡Listo!"
llegue en el instante exacto (y no el de un paso anterior mientras cocino el siguiente), so I can fiarme del
temporizador sin volver a mirar la app.

## Users And Permissions

Ningún permiso nuevo ni rationale nuevo: `POST_NOTIFICATIONS` se sigue pidiendo **en contexto** (ADR 0005,
`cooking-timer-alarm-permission-context`) y sigue sin gatear nada. `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`, `VIBRATE` y el receptor quedan **intactos**. Sin permiso, no
hay notificación y **nada** lanza; la prueba del sistema de notificaciones y del dispositivo ColorOS es
**manual** (`AGENTS.md`).

## Acceptance Scenarios

### Scenario 1: un solo reloj — la UI deriva del instante absoluto (alcance **(1)**; verificación exigida "Test JVM")

Given un temporizador arrancado en `now0` con 600 000 ms anclado en `endAt = now0 + 600_000`,
When los latidos llegan tarde o desiguales (`now0+1_500`, `+3_000`, `+61_000`, …) y la pantalla recalcula con
`at(now)`,
Then el restante mostrado es **siempre** la proyección exacta `endAt - now` (nunca la suma de deltas), la deriva
acumulada es **0** (no hay `tick(1000)`), `remaining >= 0` siempre y al alcanzar/pasar el fin queda `FINISHED`
con `remaining = 0`.

### Scenario 2: pausar/reanudar/reiniciar siguen congelando el tiempo por el instante

Given un temporizador En marcha con `endAt` anclado,
When el usuario Pausa en `nowP` y luego Reanuda en `nowR`,
Then Pausar congela `remaining = at(nowP).remaining` y **anula el ancla**, Reanudar vuelve a anclar en
`nowR + remaining` y Reiniciar devuelve a Inactivo con el total (sin ancla); ninguna transición inválida muta el
estado (no-ops) y las tres siguen emitiendo `Cancel` en la política del aviso.

### Scenario 3: el servicio publica el aviso en el instante de fin y retira la persistente (alcance **(2)**; verificación exigida "Test JVM/Robolectric")

Given el servicio en primer plano pintando la `1000` con un instante de fin `T` y el sello de fin compartido `S`,
When vence la cuenta atrás (`delay` hasta `T`),
Then el servicio publica el aviso `1001` (mismo canal, mismo copy genérico, id fijo, `when = S`, **sin** datos de
la receta), retira su notificación persistente (`STOP_FOREGROUND_REMOVE`) y se detiene; republicarlo **no**
duplica (id fijo) y **no** vuelve a alertar por el mismo fin (sello `S`).

### Scenario 4: la alarma sigue siendo el respaldo y no duplica el aviso (alcance **(2)**)

Given el fin anunciado por el servicio (una `1001` con `when = S` ya en la bandeja),
When la alarma exacta dispara (a `T`, o diferida por el fabricante más tarde) con el **mismo** sello `S`,
Then el receptor **no** vuelve a publicar (una sola notificación y **un solo** sonido/vibración), y Given el
servicio muerto/congelado (nada anunciado), When la alarma dispara con `S` dentro de la ventana de frescura,
Then el receptor **sí** publica (`1001`), retira la `1000` y para el servicio. Ningún camino lanza sin permiso.

### Scenario 5: nunca se pinta la cuenta atrás en negativo (alcance **(3)**)

Given el instante de fin ya pasado cuando el servicio iba a pintar,
When el servicio recibe un `endAt <= now` al publicar,
Then **no** pinta la persistente (no hay cronómetro caducado) y se apaga; y Given el FGS congelado o muerto,
When la alarma exacta dispara en `T`,
Then la `1000` se retira en ese instante (parar servicio + `cancel(1000)`), de modo que el cronómetro no puede
seguir contando hacia arriba.

### Scenario 6: no llega el aviso de un paso anterior durante el siguiente

Given el fin del paso anterior ya anunciado (o su alarma diferida llegando tarde),
When el usuario está en la cuenta atrás del paso siguiente,
Then ninguna publicación se repite para ese fin (sello ya mostrado) y una llegada más tarde que la ventana de
frescura se descarta; el aviso del paso siguiente, en cambio, **sí** se anuncia (sello distinto).

### Scenario 7: el trabajo del sistema cuelga del gesto, no de un efecto (alcance **(4)**; verificación "Prueba manual")

Given un arranque en frío del dispositivo y el usuario en el Modo cocina,
When el usuario pulsa "Iniciar"/"Reanudar"/"Pausar"/"Reiniciar"/"Anterior"/"Siguiente",
Then **una sola** lectura del reloj hecha en el propio gesto alimenta el estado puro, la decisión del aviso
(`CookingAlarmPolicy`) y la del permiso (`NotificationPermissionPolicy`), de modo que la alarma y el servicio se
arman/cancelan en ese mismo punto (sin `LaunchedEffect` con `state.timer.status` como clave y sin el estado
`previousTimer`); la cuenta atrás de pantalla y la del sistema quedan ancladas al **mismo** instante.

### Scenario 8: la reanudación desde la notificación sigue rearmando una vez

Given una sesión persistida En marcha y un toque en la notificación (`resume != null`),
When la pantalla se compone,
Then el estado inicial sale de `resumedMode(now)` (derivado con el reloj único) y el trabajo del sistema se
rearma **una vez** tratando la reanudación como `IDLE → RUNNING`; con la sesión en Terminado no se rearma nada.

### Scenario 9: degradación silenciosa (sin permiso, notificaciones apagadas, fila ilegible)

Given `POST_NOTIFICATIONS` denegado (o las notificaciones apagadas) y el fin llegando,
When el servicio/receptor intenta publicar y retirar,
Then nada lanza, la cuenta atrás en pantalla y el "¡Listo!" en pantalla siguen intactos y el Modo cocina no se
rompe (ADR 0005).

## Repository Research

### Files Inspected

- `feature_list.json:1795-1810` (la feature y sus cuatro verificaciones) y `:1811-1824` (la hija
  `cooking-timer-notification-step-label`, que depende de esta y toca los mismos ficheros).
- `app/src/main/java/com/sazon/app/ui/cooking/CookingTimerState.kt` (92 líneas) — `tick(deltaMillis)` (`:68-76`,
  el delta fijo que causa la deriva), `start/pause/resume/reset` (`:47-60`), `of(minutes)` (`:83-90`).
- `ui/cooking/CookingModeScreen.kt` (985 líneas) — bucle del reloj `delay(1_000)` + `tick(1_000)` (`:257-262`),
  efecto de arranque de la alarma/permiso con `previousTimer` (`:143-147`, `:226-247`), rearme al reanudar
  (`:163-173`), persistencia por cambio significativo (`:178-180`), salida explícita (`:185-191`), handlers
  `onStart/onPause/onResume/onReset` (`:302-306`), `CookingModeStateSaver` (`:957-985`), `DisposableEffect`
  (`:253-255`).
- `ui/cooking/CookingAlarm.kt` (119 líneas) — `CookingAlarmPolicy.onTimerChange` (`:59-74`, regla 1
  `Schedule(nowMillis + remaining)` en `:64-65`), `CookingAlarmCommand` (`:11-28`), costura y
  `dismissFinishNotification` (`:94-119`).
- `ui/cooking/CookingSession.kt` (107 líneas) — `endAtElapsedMillis` (`:23`), `resumedMode(now)` (`:64-75`),
  `projectedEndMillis(now)` (`:82-83`), `of(mode, recipe, now)` (`:93-105`); `ui/cooking/CookingSessionStore.kt`.
- `data/cooking/AndroidCookingAlarm.kt` (135 líneas) — `apply` (`:52-58`), `schedule(at)` con las **dos** patas
  (alarma + `startForegroundService`) (`:77-99`), `cancel()` (`:105-114`), `broadcastPendingIntent()` con el
  `requestCode` fijo y **sin extras** (`:121-134`), `dismissFinishNotification` (`:66-71`).
- `data/cooking/CookingTimerService.kt` (123 líneas) — `onStartCommand` (`:53-79`), `scheduleStop` (`:92-99`,
  `delay` + `STOP_FOREGROUND_REMOVE` + `stopSelf`), `wallClockEnd` (`:107-108`), el extra único
  `EXTRA_END_AT_ELAPSED_MILLIS` (`:113`) y `intent(...)` (`:119-121`).
- `data/cooking/CookingNotifications.kt` (211 líneas) — ids `1001`/`1000` (`:51`, `:64`), `notification(...)`
  sin `when` propio (`:117-127`), `ongoing(...)` con `setUsesChronometer`/`setWhen` (`:143-160`),
  `contentIntent` (`:169-178`), `notify(...)` con permiso/`SecurityException` (`:189-198`), `canPost` (`:200-207`).
- `data/cooking/CookingAlarmReceiver.kt` (45 líneas) — `onReceive` publica la `1001` sin estado (`:31-38`).
- Tests vigentes: `ui/cooking/CookingTimerStateTest.kt` (11 `@Test`, los de `tick` en `:49-83`, `:140-151`),
  `CookingAlarmPolicyTest.kt` (16, `:23-74`, `:232-267`), `CookingSessionTest.kt` (16),
  `data/cooking/CookingTimerServiceTest.kt` (10, `:59-78` afirma "el servicio **no** publica la `1001`",
  `:146-157` "único dato que viaja al servicio", `:174-196` instante ya pasado / sin extra),
  `CookingNotificationsTest.kt` (17, `:156-189` sin extras propios + garantía estructural de firma), 
  `AndroidCookingAlarmTest.kt` (13, `:139-159` pata del servicio), `RoomCookingSessionStoreTest.kt` (7) y
  `CookingTimerServiceManifestTest.kt` (6, guarda del manifiesto).
- Docs: `docs/adr/0008-cooking-timer-foreground-service.md:42-48` (decisión 1: la alarma es el **único** dueño
  del "¡Listo!"; "con un solo publicador no hay sonido/vibración duplicados"), `:49-54` (decisión 2), `:68-73`
  (decisión 6), `:90-95` (decisión 9), `:103-114` (decisión 12), `:126-135` (alternativa 1 revisada: el servicio
  sigue descartado como productor), `:164-171` y `:181-184` (consecuencias); 
  `docs/adr/0007-cooking-timer-alarm.md:43-53` (alarma exacta + receptor), `:56-62` (decisiones 7 y 8: contenido
  genérico, `PendingIntent` sin extras, id fijo), `:63-75` (decisión 9 y su nota), `:152-160` (disparo único);
  `docs/technical-discovery.md:391` (`tick(1000)`), `:419-464` (párrafo del aviso de sistema), `:465-497`
  (párrafo de la cuenta atrás); `docs/domain-model.md:139-152` (sesión persistida);
  `docs/risks-and-open-questions.md:177-182` (aviso que no llega, servicio en primer plano, cronómetro del
  fabricante, heads-up en primer plano); `PROGRESS.md` (sesiones 137-139).
- Baseline re-derivada **en este worktree**: rama `feature/cooking-timer-notification-reliability`, `HEAD`
  `e5012d1`, `git grep -c '@Test' HEAD -- app/src/test` = **114 ficheros / 896 anotaciones** (no hay
  `app/build/test-results/` en el worktree, así que la tirada completa la produce el implementer). Contexto
  registrado: en `83d0549` eran 874 anotaciones/888 tests y las PR #158/#159 añadieron +14 y +8.

### Existing Patterns To Follow

- **Costura + lógica pura + adaptador delgado**: decisión pura en `ui.cooking` (sin `android.*`/`androidx.*`) y
  adaptador Android en `data.cooking`, con **instantes inyectados** (nunca se lee el reloj en la lógica pura),
  contrato "**no lanza**" y **degradación silenciosa** (ADR 0005, `runCatching` por pata).
- **Verificación canónica en JVM**: `bash init.sh` (test + lint) o `bash scripts/gradle.sh <tareas>`; Robolectric
  (`@Config(sdk = [33])`) para lo que toca framework (`ShadowAlarmManager`, `ShadowService`,
  `shadowOf(notificationManager)`); **nunca** `./gradlew` directo ni `--stop`.
- **Ids fijos y contenido genérico**: `1001` descartable / `1000` `ongoing`; las funciones de construcción **no
  reciben la receta** (garantía estructural, con test de firmas); el único booleano que cruza a `MainActivity` es
  `EXTRA_RESUME_COOKING`.
- **Persistencia por cambio significativo**: nunca una escritura por latido; el instante de fin es lo que permite
  recalcular.
- `NotificationManagerCompat.getActiveNotifications()` (API 23+; `minSdk = 33`) está **implementado** por
  `ShadowNotificationManager` en Robolectric 4.17 (`getActiveNotifications()` aparece en la clase del shadow), así
  que la lectura del aviso ya mostrado es verificable en JVM.

### Current Gaps

La deriva de `(a)` (dos relojes) y el retraso de `(d)` (efectos de Compose en el hilo principal) existen porque
`CookingTimerState` **no conoce ningún instante absoluto**: solo acumula deltas. La retirada "blanda" de `(b)` y el
productor único de `(c)` están **decididos** en el ADR 0008 (decisiones 1, 2 y alternativa 1) y hay que
**enmendarlos**; además, con **dos** publicadores potenciales hace falta una regla de idempotencia explícita,
porque el id fijo evita dos notificaciones pero **no** dos sonidos/vibraciones.

## Technical Approach

Reloj único en la lógica pura; el servicio pasa a ser el **productor primario** del aviso y la alarma queda como
**respaldo** con una idempotencia explícita por **sello de fin**; el arranque del trabajo del sistema cuelga del
gesto, no de un efecto.

1. **`CookingTimerState` gana el instante absoluto** (`endAtElapsedMillis: Long?`, no nulo solo En marcha con
   tiempo que contar, el mismo invariante que `CookingSession`):
   - `start(nowMillis)` / `resume(nowMillis)` **anclan** (`endAt = now + remaining`, solo si `remaining > 0`);
     `pause(nowMillis)` **congela** (`remaining = at(now).remaining`) y anula el ancla; `reset()` sigue devolviendo
     a Inactivo con el total y sin ancla.
   - `at(nowMillis)`: si no está En marcha, `this`; si lo está, `remaining = (endAt - now).coerceAtLeast(0)`
     (nunca negativo) y si llega a `0` pasa a `FINISHED` con `remaining = 0` y sin ancla.
   - `projectedEndMillis(nowMillis) = if (isRunning && remaining > 0) (endAt ?: now + remaining) else null`.
   - **Se elimina `tick(deltaMillis)`** (el delta fijo es la causa de la deriva y quedaría sin llamantes).
   - `of(minutes)` no cambia (Inactivo, sin ancla), así que `CookingModeState` **no** cambia.
2. **`CookingSession` reutiliza el ancla**: `of` proyecta `endAtElapsedMillis = mode.timer.projectedEndMillis(now)`
   (deja de recalcular `now + remainingMillis`, que arrastraba hasta un latido de retraso) y `resumedMode(now)`
   pasa por `timer.at(now)`, conservando la degradación defensiva cuando el ancla fuera `null`. El estado de la
   pantalla (`CookingModeStateSaver`) **persiste el ancla**, o una rotación volvería a derivar de un
   `remainingMillis` obsoleto.
3. **`CookingAlarmPolicy` promueve el ancla a instante oficial**: la regla de `Schedule` emite
   `next.endAtElapsedMillis ?: (nowMillis + next.remainingMillis)`, de modo que la UI, la sesión persistida, la
   alarma y el servicio comparten **el mismo `Long`**. El resto de reglas (ticks `None`, entrada en `FINISHED`
   `None`, Pausar/Reiniciar/cambio de paso/salir `Cancel`, `onExit()`) **no** cambian.
4. **La pantalla deja de contar con deltas y de armarse desde un efecto**: el bucle del reloj recalcula
   `at(SystemClock.elapsedRealtime())` con un latido **alineado al segundo** de la cuenta atrás
   (`remaining % 1_000`, o 1 s si es exacto), de modo que el número de la pantalla voltea con el cronómetro del
   sistema y nunca acumula deriva; y **un único punto de entrada por gesto** (p. ej. `onUserAction { now -> … }`)
   aplica el estado nuevo y, con **la misma** lectura del reloj, las dos decisiones puras
   (`CookingAlarmPolicy.onTimerChange(previous, next, now)` y `NotificationPermissionPolicy.onTimerChange`). Se
   **eliminan** el efecto con `state.timer.status` como clave y el estado `previousTimer`; se **mantienen** el
   rearme al reanudar (`LaunchedEffect(Unit)`), la persistencia por cambio significativo, el `DisposableEffect` de
   salida y el gate de confirmación de salida.
5. **El servicio publica y retira en el instante exacto**: al vencer su `delay`, publica el aviso `1001` con el
   **sello de fin compartido** y retira la `1000` (`STOP_FOREGROUND_REMOVE`) antes de `stopSelf()`. Al arrancar,
   si el instante ya pasó **no pinta** (no hay cronómetro caducado) y se apaga; el `when` del cronómetro se
   **recalcula en la publicación** a partir de lo que queda en esa misma lectura (`currentTimeMillis() +
   (endAt - elapsedRealtime())`).
6. **La alarma queda como respaldo** y el receptor, al disparar, **para el servicio y retira la `1000`** (así la
   retirada también es exacta cuando el FGS está congelado/muerto) y publica el aviso solo si procede.
7. **Idempotencia por sello de fin** (necesaria al haber dos publicadores): `AndroidCookingAlarm.schedule(at)`
   calcula **una vez** el sello en hora de pared (`currentTimeMillis() + (at - elapsedRealtime())`) y lo lleva a
   los **dos** `Intent` (el del servicio y el del `PendingIntent` de la alarma); la `1001` se publica con
   `setWhen(sello)` y una decisión **pura** (`CookingFinishPublication`) decide, con `now` y el `when` del aviso
   mostrado si lo hay: **no publicar** si ya hay una `1001` con ese mismo sello (mismo fin ya anunciado) ni si el
   sello es más viejo que una **ventana de frescura** (p. ej. 60 s, un aviso diferido mucho más tarde es ya un
   aviso de un paso anterior). `setOnlyAlertOnce` **no** se usa (silenciaría el fin legítimo siguiente mientras la
   `1001` anterior siga en la bandeja).

## Expected File Changes

Rutas relativas a `app/src/main/java/com/sazon/app/` y `app/src/test/java/com/sazon/app/`.

- `ui/cooking/CookingTimerState.kt` — modificar; ancla `endAtElapsedMillis`, `start/resume/pause(nowMillis)`,
  `at(nowMillis)`, `projectedEndMillis(nowMillis)`, se elimina `tick`; KDoc del modelo de reloj.
- `ui/cooking/CookingAlarm.kt` — modificar; `Schedule` prefiere el ancla y el KDoc de `Cancel`/reglas pasa de
  "único productor" a "**respaldo**" (la idempotencia la da el sello).
- `ui/cooking/CookingSession.kt` — modificar; `of`/`resumedMode` reutilizan el ancla (sin cambiar la API ni la
  degradación defensiva).
- `ui/cooking/CookingModeScreen.kt` — modificar; latido derivado y alineado al segundo, punto único por gesto
  (aviso + permiso con la misma lectura), `Saver` con el ancla, KDoc; **se borran** el efecto con
  `state.timer.status` como clave y `previousTimer`.
- `data/cooking/CookingTimerService.kt` — modificar; publica la `1001` y retira la `1000` al vencer, no pinta si
  el fin ya pasó, `when` recalculado al publicar, extra del sello y KDoc (el servicio ya no es solo un "pintor").
- `data/cooking/CookingNotifications.kt` — modificar; `notification(context, contentIntent, whenMillis)`,
  `notify(...)` → `notifyFinish(context, finishWallClockMillis, contentIntent)` con el guard de idempotencia, y la
  constante del sello (una sola, compartida por el servicio y el receptor). Sin cambios en `ongoing(...)`, el canal,
  los ids ni el copy.
- `data/cooking/AndroidCookingAlarm.kt` — modificar; calcula el sello **una vez** en `schedule(at)` y lo pone en el
  `Intent` del servicio y en el `PendingIntent` de la alarma (con `FLAG_UPDATE_CURRENT`, que ya está).
- `data/cooking/CookingAlarmReceiver.kt` — modificar; para el servicio / retira la `1000` y publica vía
  `notifyFinish` (con el sello del extra; si falta, degrada a `now`, sin lanzar).
- `data/cooking/CookingFinishPublication.kt` — **crear**; decisión pura (sin `android.*`) "¿publicar este fin?" con
  el aviso mostrado y el instante actual **inyectados** + la ventana de frescura.
- Tests: `ui/cooking/CookingTimerStateTest.kt` (los casos de `tick` se reescriben como `at(now)`: proyección exacta,
  deriva cero con latidos desiguales, nunca negativo, ancla en start/resume, congela en pause),
  `ui/cooking/CookingAlarmPolicyTest.kt` (`Schedule` con el ancla + el caso vigente sin ancla),
  `ui/cooking/CookingSessionTest.kt` (`of`/`resumedMode` prefieren el ancla),
  `data/cooking/CookingTimerServiceTest.kt` (**se invierte** "el servicio no publica la `1001`": ahora publica la
  `1001` y retira la `1000` al vencer; sin pintar si el fin ya pasó; el extra nuevo),
  `data/cooking/CookingNotificationsTest.kt` (`notifyFinish` idempotente por sello, fresco/caducado, `when` de la
  `1001`, y el test estructural de firmas acepta el escalar del sello), `data/cooking/AndroidCookingAlarmTest.kt`
  (el sello viaja a **los dos** `Intent`), y **nuevo** `data/cooking/CookingFinishPublicationTest.kt`.
- **No** se tocan: `CookingModeState`, `CookingModeFormat`, `CookingExitConfirmation`, `CookingSessionStore`,
  `NotificationPermission*`, `ui/MainShell.kt`, `ui/SazonApp.kt`, `MainActivity.kt`, `data/cooking/Room*`,
  `CookingSession*` de Room, el manifiesto, `res/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/**`, `domain/**`, `ui/theme/**`, assets ni `docs/design/**`.

## Visual Design Impact

- UI involved: **no**. Ambas notificaciones son superficies del **sistema operativo** y la pantalla del Modo cocina
  no cambia de aspecto (mismo número, mismos colores, mismo microcopy).
- Design source: no aplicable (`DESIGN.md` no cubre notificaciones de sistema; excepción ya registrada).
- Screens or states affected: ninguno; **no** se crea `docs/design/concepts/cooking-timer-notification-reliability/`.

## Durable Documentation Impact

- `docs/adr/0008-cooking-timer-foreground-service.md`: **update** — nota de enmienda en la **decisión 1** (la
  alarma **deja** de ser el único dueño del "¡Listo!": el servicio publica en el instante exacto y la alarma es el
  respaldo; el "sin sonido duplicado" pasa a sostenerse en el **sello de fin**, no en el publicador único), en la
  **decisión 2** (el servicio retira la `1000` **y** publica la `1001`; `when` recalculado al publicar; no pinta si
  el fin ya pasó), en la **decisión 6/9** (el reloj único: el ancla en `CookingTimerState` y el sello compartido en
  los dos `Intent`), en la **decisión 12** (la lista de "sin diff" deja de aplicar a `CookingTimerState`,
  `CookingAlarmPolicy`, `CookingSession`, el servicio, `CookingNotifications` y el receptor) y en la **alternativa
  1** (revisada: el servicio **sí** es productor, como primario; el respaldo sigue siendo la alarma), más las
  consecuencias del aviso y del cronómetro negativo.
- `docs/adr/0007-cooking-timer-alarm.md`: **update** — nota en las decisiones 3/5 (la alarma pasa a **respaldo**),
  en la 7 (el `PendingIntent` de la alarma lleva **un escalar**: el sello del fin; sigue **sin** ningún dato de la
  receta) y en la 8 (dos publicadores posibles: el id fijo evita dos notificaciones y el sello evita el segundo
  aviso).
- `docs/technical-discovery.md`: **update** — `:391` (el reloj de la pantalla ya no es `tick(1000)`), `:419-464`
  (el aviso: productor primario el servicio, respaldo la alarma, sello de fin e idempotencia) y `:465-497` (la
  cuenta atrás: retirada exacta, sin negativo, el servicio publica al vencer).
- `docs/domain-model.md`: **update** — la sesión del Modo cocina (`:139-152`): mientras el temporizador está En
  marcha, **el propio temporizador** lleva el instante de fin (reloj único) además de la fila persistida.
- `docs/risks-and-open-questions.md`: **update** — fila `:181` (cronómetro del fabricante: retirada en el instante
  exacto, no se pinta si el fin ya pasó, y el riesgo vivo del `when` en hora de pared), fila `:177` (el aviso: el
  productor primario es el servicio), fila `:182` (dos publicadores: un solo aviso y una sola alerta por fin
  gracias al sello) y **fila nueva** del aplazamiento/coalescencia de la alarma exacta por el fabricante
  (ColorOS) con la ventana de frescura y el retraso del primer arranque en frío.
- `docs/specs/cooking-timer-foreground-service.md` y `docs/specs/cooking-timer-finish-notification.md`: **update** —
  nota de **vigencia** (sin reescribir la evidencia histórica) en los escenarios y Non-Goals donde dicen que el
  servicio **no** publica el aviso, que la alarma es el **único** productor, que el reloj resta 1000 ms por `tick`
  o que el único dato que cruza al servicio es el instante de fin.
- `AGENTS.md`, `docs/build-brief.md` y `DESIGN.md`: **not needed** — no cambian el flujo de agentes, el arranque ni
  el alcance del MVP, y no hay pantallas ni tokens nuevos.

## Implementation Plan

1. **Reloj único (item 1)**: ancla y API de `CookingTimerState` (+ eliminar `tick`), `CookingSession` y
   `CookingAlarmPolicy` sobre el ancla, y la pantalla derivando del instante (latido alineado + `Saver` con el
   ancla) con `CookingTimerStateTest`/`CookingAlarmPolicyTest`/`CookingSessionTest` al día.
2. **Sin negativo (item 3)**: el servicio publica/retira en el instante exacto, no pinta si el fin ya pasó y
   recalcula el `when`; el receptor para el servicio y retira la `1000` al disparar. Tests de servicio/receptor.
3. **Aviso puntual e idempotente (item 2)**: sello compartido en los dos `Intent`, `when` de la `1001`,
   `CookingFinishPublication` + `notifyFinish` y tests (servicio, notificaciones, adaptador, decisión pura).
4. **Desacoplo del gesto (item 4)**: punto único por gesto en la pantalla con la misma lectura del reloj; borrar el
   efecto con `state.timer.status` como clave y `previousTimer`; conservar rearme al reanudar, persistencia y
   salida.
5. **Docs durables + verificación**: `bash init.sh`, tiradas forzadas, conteos del XML y del SARIF, diffs
   inspeccionados y la prueba manual anotada como pendiente para la persona.

## Implementation Tasks

- [x] `CookingTimerState`: `endAtElapsedMillis`, `start/resume/pause(nowMillis)`, `at(nowMillis)`,
      `projectedEndMillis(nowMillis)`, **eliminar** `tick(deltaMillis)`; KDoc del reloj único.
- [x] `CookingTimerStateTest`: reescribir los casos de `tick` como proyección desde el ancla (exacta con latidos
      desiguales, deriva 0, nunca negativo, `FINISHED`/0 al final, congela al pausar, no-ops y determinismo).
- [x] `CookingSession.of`/`resumedMode` y `CookingAlarmPolicy` sobre el ancla; `CookingSessionTest` y
      `CookingAlarmPolicyTest` (caso del ancla + el vigente sin ancla) al día.
- [x] `CookingModeScreen`: latido alineado al segundo que recalcula `at(now)`, punto único por gesto con las dos
      decisiones puras, `Saver` con el ancla, **borrar** el efecto de arranque y `previousTimer`; KDoc.
- [x] `CookingTimerService`: publicar la `1001` (sello) y retirar la `1000` al vencer, no pintar si el fin ya pasó,
      `when` recalculado al publicar, extra del sello y KDoc.
- [x] `CookingNotifications`: `when` en la `1001`, `notifyFinish` con el guard de idempotencia, constante única del
      sello; **sin** tocar `ongoing`, el canal, los ids ni el copy.
- [x] `AndroidCookingAlarm` calcula el sello una vez y lo pone en los dos `Intent`; `CookingAlarmReceiver` para el
      servicio / retira la `1000` y publica vía `notifyFinish`.
- [x] `CookingFinishPublication` (nueva, pura) + `CookingFinishPublicationTest`; casos de
      `CookingTimerServiceTest` (invertir "no publica la `1001`"), `CookingNotificationsTest` (idempotencia por
      sello, frescura, firma estructural) y `AndroidCookingAlarmTest` (sello en los dos `Intent`).
- [x] Docs durables actualizados (ADR 0007/0008, `technical-discovery`, `domain-model`, `risks` y notas de vigencia
      en las dos specs anteriores); gate en verde y evidencia registrada (sin commit/push/PR: los crea el
      orquestador).

## Verification Plan

- `bash init.sh` (camino estándar, WSL): exit 0 con `./gradlew test` + `./gradlew lint`, sin procesos largos y sin
  tocar `init.sh`. Baseline re-derivada del repo en `HEAD e5012d1`:
  `git grep -c '@Test' HEAD -- app/src/test` = **114 ficheros / 896 anotaciones** (el worktree no tiene
  `app/build/test-results/`, así que la tirada completa la produce esta sesión); el conteo de tests del XML debe
  cuadrar con esa base **más** los casos nuevos, y el informe de lint con los 13 resultados preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 errores** y
  **0 bajo `app/src`**.
- Evidencia **fresca** forzada con `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
  `bash scripts/gradle.sh lint --rerun-tasks` (nunca `./gradlew` directo ni `--stop`), más la tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*" --tests "com.sazon.app.data.cooking.*"`.
- Automatizable en JVM (escenarios 1, 2, 3, 4, 5 -retirada-, 6 y 9): `CookingTimerStateTest`/
  `CookingAlarmPolicyTest`/`CookingSessionTest` para el reloj único, `CookingFinishPublicationTest` para la
  idempotencia, y Robolectric (`ShadowService`, `shadowOf(notificationManager)`, `ShadowAlarmManager`) para el
  servicio, las notificaciones y el adaptador.
- **Manual en dispositivo ColorOS** (persona; `AGENTS.md`; escenarios 3, 5, 6 y 7): la primera cuenta atrás tras
  abrir la app arranca **sin** el retraso; pantalla y notificación sincronizadas durante todo el contador; la
  notificación **no** se pone en negativo al llegar a cero; el "¡Listo!" llega **en el instante** con la app
  minimizada y el móvil bloqueado, **una sola vez** (un solo sonido/vibración); durante el paso siguiente **no**
  llega el aviso del anterior; pausar/reiniciar/cambiar de paso/salir siguen parando el servicio; sin permiso, nada
  se rompe. El acabado (aspecto del cronómetro en la pantalla de bloqueo, orden de las dos notificaciones,
  tono/patrón) lo revisa la persona; un defecto se corrige con una **feature correctiva**.
- No hay E2E persistente: la UI de emulador y el sistema de notificaciones no se automatizan (`AGENTS.md`), así que
  la cobertura es lógica/estado en JVM (más Robolectric) y revisión manual de la persona, como en las features
  aceptadas del Modo cocina.

## Evidence To Capture

- `bash init.sh` exit 0; conteos del XML fresco (`app/build/test-results/testDebugUnitTest/TEST-*.xml`) con el delta
  respecto a `e5012d1` (114 ficheros / 896 anotaciones) y el informe de lint (0 errores, 0 bajo `app/src`).
- Casos nuevos/modificados: `CookingTimerStateTest` (proyección exacta desde el ancla, deriva 0, nunca negativo),
  `CookingAlarmPolicyTest` (`Schedule` con el ancla), `CookingSessionTest`, `CookingTimerServiceTest` (publica la
  `1001` y retira la `1000` al vencer; no pinta si ya pasó), `CookingNotificationsTest` (idempotencia por sello),
  `AndroidCookingAlarmTest` (sello en los dos `Intent`) y `CookingFinishPublicationTest` (frescura y ya anunciado).
- Inspecciones: `CookingTimerState.kt`/`CookingSession.kt`/`CookingFinishPublication.kt` sin imports `android.*` y
  sin leer el reloj; **0** apariciones de `tick(` en `app/src/main`; ninguna `LaunchedEffect` con
  `state.timer.status` para armar el aviso; `git status`/`git diff --stat` con **solo** los ficheros previstos y
  sin diff en build/lockfile/manifiesto/permisos/`init.sh`/`scripts/**`/`domain/**`/`Room`/`ui/theme/**`.
- Diff de las notas durables y **pendiente manual** anotado por el orquestador en `PROGRESS.md`/`feature_list.json`.

## Decisions And Unknowns

- **Reloj único en `CookingTimerState`** (el ancla vive en el temporizador, no en `CookingModeState` ni en la
  pantalla): es la única opción que deja una sola fuente del instante para UI, sesión, alarma y servicio y que es
  testeable en JVM con el instante inyectado. Se descarta mantener `remainingMillis` como fuente y recalcularlo solo
  al persistir (seguiría habiendo dos relojes) y se descarta conservar `tick` "por compatibilidad" (quedaría sin
  llamantes y mantendría viva la trampa de la deriva).
- **El servicio pasa a ser el productor primario y la alarma el respaldo** (enmienda la decisión 1 y la alternativa
  1 del ADR 0008): el `delay` del FGS está en el proceso vivo (que además muestra la cuenta atrás) y la alarma sigue
  cubriendo la muerte del proceso. Los dos **no** pueden alternarse sin una regla de idempotencia, porque el id fijo
  evita dos notificaciones pero **no** dos sonidos/vibraciones.
- **Idempotencia por sello de fin compartido**: el sello se calcula **una vez** al programar y viaja en los dos
  `Intent`, y la `1001` se publica con `setWhen(sello)` para que un publicador reconozca que **ese** fin ya está
  anunciado. Alternativas descartadas: `setOnlyAlertOnce` en la `1001` (silenciaría el fin legítimo siguiente
  mientras la `1001` anterior siga en la bandeja) y la cancelación mutua servicio↔alarma (los dos corren en el hilo
  principal y un broadcast ya despachado no se puede cancelar: quedaría una carrera real).
- **Ventana de frescura** (p. ej. `60_000L`): un aviso diferido mucho más tarde es ya "el aviso de un paso
  anterior" que la persona reporta, así que se descarta en lugar de mostrarse; el precio (una alarma que el
  fabricante retrase más de la ventana con el proceso muerto se pierde) se documenta y se revisa en la prueba
  manual. Si el implementer acreditara que la regla no es verificable, **no** puede implementarse a medias: la
  decisión debe registrarse en la evidencia.
- **`when` del cronómetro recalculado al publicar** (1000) frente al **sello del fin** en la `1001`: son dos
  valores distintos con dos fines distintos (robustez al cambio de reloj vs. identidad del fin). El sello es un
  escalar (nunca datos de receta), coherente con la decisión 7 del ADR 0007, que se enmienda con esa precisión.
- **El arranque del trabajo del sistema cuelga del gesto** (item 4), con una única lectura del reloj compartida por
  el estado y las dos políticas; el permiso viaja en el mismo punto porque hoy comparte el efecto que se elimina (la
  misma arista `IDLE → RUNNING` y el mismo `markAsked(now)`).
- **Incógnitas**: (i) la causa real del retraso de 10-15 s en el primer arranque en frío **no** se puede reproducir
  desde WSL —el desacoplo del gesto es la mitigación pedida y la prueba manual decide; si persiste, es una feature
  correctiva—; (ii) el doble reloj de pared/monotónico del cronómetro del sistema sigue siendo un riesgo aceptado
  (`docs/risks-and-open-questions.md:181`); (iii) si el fabricante realmente difiere/coalesce la alarma exacta solo
  se ve en el dispositivo; (iv) tras una muerte de proceso y una restauración de actividad, el trabajo del sistema
  **no** se rearma sin tocar la notificación (preexistente, fuera de alcance).

## Validator Checklist

- [ ] `CookingTimerState` deriva el restante de `endAtElapsedMillis` (`at`/`remainingAt`), **no** resta deltas
      fijos, no acumula deriva y nunca da valores negativos; `tick(deltaMillis)` ya no existe.
- [ ] `CookingSession`/`CookingAlarmPolicy` usan el ancla, así que UI, sesión persistida, alarma y servicio
      comparten el mismo instante; el `Saver` de la pantalla persiste el ancla.
- [ ] El servicio **publica** la `1001` y **retira** la `1000` al vencer, no pinta si el fin ya pasó y recalcula el
      `when`; el receptor para el servicio/retira la `1000` al disparar.
- [ ] Los escenarios 1-9 pasan (1-6 y 9 en JVM/Robolectric; 3, 5, 6 y 7 con la prueba manual anotada).
- [ ] La idempotencia por sello funciona: mismo fin → **una** notificación y **una** alerta, también con el servicio
      y la alarma disparando a la vez; fin nuevo → vuelve a alertar; un fin fuera de la ventana de frescura se
      descarta.
- [ ] Nada lanza sin `POST_NOTIFICATIONS`/con las notificaciones apagadas y el Modo cocina, la cuenta atrás en
      pantalla y el "¡Listo!" siguen intactos.
- [ ] La verificación exigida está presente: `bash init.sh` en verde + evidencia fresca, conteos del XML contra la
      baseline de `e5012d1` (114 ficheros / 896 anotaciones) y lint 0 errores / 0 bajo `app/src`.
- [ ] Sin dependencias, permisos, manifiesto, build/lockfile, `init.sh`, `scripts/**`, `domain/**`, Room ni
      `ui/theme/**` tocados, y sin cambios en el canal, los ids, el copy o el aspecto del Modo cocina.
- [ ] Docs durables actualizados (ADR 0007/0008, `technical-discovery`, `domain-model`, `risks` y notas de vigencia
      en las dos specs anteriores), sin reescribir evidencia histórica.
- [ ] No hay comportamiento no pedido (`setOnlyAlertOnce` descartado, sin canal nuevo, sin servicio vivo tras el
      fin, sin tocar la hija `cooking-timer-notification-step-label`).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por el orquestador (no por quien implementa).
