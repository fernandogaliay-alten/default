# Feature Implementation Spec: Aviso de fin del temporizador duradero y vuelta al paso en Terminado

## Source Feature

- `id` `cooking-timer-finish-notification` · `area` `integration` · `status` `not_started` (al planificar)
- `depends_on`: `cooking-timer-foreground-service` (`accepted`) · `source`: `feature_list.json` (1725-1743)
- Correctiva pedida por la persona (2026-10-16) sobre `cooking-timer-foreground-service`; **no** reabre ninguna
  decisión: aplica la Opción A acordada (P1+P2+P3+P4) y solo decide la formulación de P3 y la vía de P4.

## Goal

Al llegar a cero la cuenta atrás de un paso del Modo cocina, el aviso **"¡Listo!"** (notificación id `1001`) debe
publicarse **siempre** —primer plano, minimizada o proceso muerto— y **quedarse en la bandeja** hasta que el usuario
lo toque o lo descarte; el toque debe devolver al paso **aunque el temporizador ya esté en Terminado**; y salir del
modo ("Atrás"/"Terminar") debe limpiar el aviso.

Hoy falla por una carrera no determinista: `CookingAlarmPolicy.onTimerChange` devuelve `Cancel` en `RUNNING →
FINISHED` (`ui/cooking/CookingAlarm.kt:59`) y `AndroidCookingAlarm.cancel()` (`AndroidCookingAlarm.kt:88-97`) cancela
la alarma **y** para el servicio, así que si el `tick` cruza el cero antes de que dispare `AlarmManager` la `1001`
**no se publica**. Además `CookingSession.isResumable` excluye `FINISHED` (`CookingSession.kt:33`), así que una
sesión persistida en Terminado no reabre el paso (`ui/MainShell.kt:184`).

## Non-Goals

- **No** mantener el servicio en primer plano vivo tras el fin ni publicar la `1001` desde él (Opción B): la alarma
  sigue siendo el **único** productor y el servicio se para con su propio `delay`.

  > **Nota de vigencia (`cooking-timer-notification-reliability`, 2026-10-16):** la Opción B se **adopta** en la
  > correctiva siguiente: el **servicio** pasa a ser el productor **primario** de la `1001` (el aviso puntual) y
  > la alarma queda como **respaldo** con un **sello de fin** compartido. El servicio sigue **sin** quedarse vivo
  > tras el fin (publica el aviso, retira la `1000` y se detiene).
- **No** cambiar la notificación de fin (id `1001`, canal `cooking_timer`, microcopy, small icon): sigue siendo
  **descartable** (`setAutoCancel(true)`, no `ongoing`).
- **No** tocar la lógica del temporizador ni el Modo cocina (`CookingTimerState`, `CookingModeState`,
  `CookingModeFormat`, UI y microcopy), `CookingTimerService`, `CookingAlarmReceiver`, el manifiesto, permisos,
  rationale ni Room; ni añadir persistencias, migraciones, dependencias o permisos.
- **No** arreglar que la rotación cierre el Modo cocina (preexistente): solo se garantiza que **no borra** la `1001`.

## Job Story

When la cuenta atrás de un paso termina con el móvil en la mano o guardado en el bolsillo, I want que el aviso
"¡Listo!" esté siempre en la bandeja y que, al tocarlo, la app vuelva a ese paso aunque el temporizador ya marque
Terminado, so I can enterarme de que el paso terminó y ver en qué punto de la receta estoy sin perder el hilo.

## Users And Permissions

Ningún permiso nuevo: `POST_NOTIFICATIONS` sigue pidiéndose **en contexto** y, denegado, no hay aviso visible ni crash (ADR 0005). La prueba del sistema de notificaciones es **manual** en el emulador (`AGENTS.md`).

## Acceptance Scenarios

### Scenario 1: llegar a cero no cancela la alarma (P1, verificación exigida)

Given un temporizador En marcha con su alarma programada en el instante de fin,
When el `tick` cruza el cero y `RUNNING → FINISHED` llega a `onTimerChange` con `nowMillis` inyectado,
Then devuelve `CookingAlarmCommand.None` (no `Cancel`), la alarma sigue viva y los `tick` posteriores en Terminado dan
`None`; una corrida completa (`IDLE→RUNNING`, 600 ticks, cruce del cero y 2 no-ops) emite **exactamente un**
`Schedule` y **cero** `Cancel`.

### Scenario 2: el aviso se publica siempre y el servicio se para solo (P1+P2, verificación exigida)

Given el temporizador En marcha con la persistente `1000` publicada, con la app en primer plano, minimizada o el
proceso muerto,
When la cuenta atrás llega a cero y `AlarmManager` dispara `CookingAlarmReceiver`,
Then el receptor publica la `1001` (microcopy genérico, descartable, **no** `ongoing`, sin datos de la receta;
republicar **reemplaza**, nunca duplica) y el servicio retira su `1000` (`STOP_FOREGROUND_REMOVE`) y se detiene con su
propio `delay`, sin `Cancel` y sin quedarse vivo para sostener el aviso.

### Scenario 3: Pausar, Reiniciar, cambiar de paso y salir siguen cancelando (no-regresión de P1)

Given el temporizador En marcha con la alarma programada,
When el usuario Pausa, Reinicia, cambia de paso o sale del modo,
Then `onTimerChange`/`onExit()` siguen devolviendo `Cancel` (alarma cancelada, servicio parado), "Reanudar" vuelve a
programar con el instante correcto y `FINISHED → IDLE` ("Reiniciar" desde Terminado) también da `Cancel`.

### Scenario 4: el toque del aviso vuelve al paso en Terminado (P3, verificación exigida)

Given una sesión persistida en **Terminado** (`remaining = 0`, `endAtElapsedMillis = null`) y un `Intent` con
`EXTRA_RESUME_COOKING`,
When el usuario toca el aviso y `MainShell` carga la sesión,
Then el predicado de reapertura desde la notificación es `true` y el Modo cocina se abre en **ese** paso con
`resumedMode(now)` = Terminado, `remaining = 0` y el "¡Listo!" en pantalla, sin rearmar alarma ni servicio; un arranque
**normal** (sin el flag) sigue sin abrir el Modo cocina.

### Scenario 5: la salida explícita limpia el aviso; la rotación no (P4)

Given el aviso `1001` en la bandeja,
When el usuario sale del modo con "Atrás"/"Terminar",
Then la costura recibe la orden de limpieza y la `1001` desaparece **antes** de cerrar el modo (además de borrarse la
sesión); si lo que ocurre es una **rotación** u otro desmontaje, la `1001` **permanece** (el `onDispose` solo emite
`Cancel`, que —decisión 9 del ADR 0007— no retira notificaciones ya mostradas).

### Scenario 6: sin permiso o con las notificaciones apagadas no se rompe nada (degradación)

Given `POST_NOTIFICATIONS` denegado (o las notificaciones apagadas),
When llega el fin de la cuenta atrás y el usuario sale del modo,
Then ninguna llamada lanza: la `1001` no se publica, la limpieza no hace nada y el Modo cocina, la cuenta atrás en
pantalla, el "¡Listo!" y la sesión persistida siguen intactos (ADR 0005).

## Repository Research

### Files Inspected

- `feature_list.json:1725-1743` (la feature y sus cuatro verificaciones) y `:1693-1723` (la anterior, `accepted`);
  `PROGRESS.md` (gate: `bash init.sh` exit 0, **113 suites / 874 tests**, lint 0 errores / 13 warnings / 0 bajo
  `app/src`; `HEAD` = `610fe14`).
- `ui/cooking/CookingAlarm.kt` (94 líneas) — `CookingAlarmCommand` (KDoc de `Cancel` en `:19`), regla
  `previous != next -> Cancel` (`:59`), `onExit()` (`:68`), costura (`:82-86`); `ui/cooking/CookingSession.kt`
  (95 líneas) — `isResumable` (`:33`), `resumedMode` (`:52-63`), `of` (`:81-93`).
- `ui/MainShell.kt:112-118, 181-187` (gate `isResumable` en `:184`); `ui/cooking/CookingModeScreen.kt`: rearme
  (`:147-157`), persistencia (`:162-164`), salida explícita `exiting` (`:168-174`), `DisposableEffect` → `onExit()`
  (`:207-209`), con "Atrás" (`:241`) y "Terminar" (`:228`) compartiendo `requestExit`.
- `data/cooking/AndroidCookingAlarm.kt` (118 líneas) — `apply` (`:48-54`), `schedule` (alarma +
  `startForegroundService`, `:60-82`), `cancel` (`:88-97`, **no** retira notificaciones);
  `CookingNotifications.kt`: ids `1001` (`:51`) y `1000` (`:64`), `setAutoCancel(true)` (`:117-127`),
  `notify()` (`:189-198`), `contentIntent()` (`:169-178`); `CookingAlarmReceiver.kt:31-38` publica la `1001`;
  `CookingTimerService.kt:92-99` (`delay` + `STOP_FOREGROUND_REMOVE` + `stopSelf`, `START_NOT_STICKY`).
- Tests vigentes: `ui/cooking/CookingAlarmPolicyTest.kt` (`:121-137`, `:218-248`: afirman **1** `Cancel`);
  `ui/cooking/CookingSessionTest.kt` (`:57-59`, `:202`: Terminado no es resumible); `AndroidCookingAlarmTest.kt`
  (Robolectric + `ShadowAlarmManager`); `CookingNotificationsTest.kt` (`shadowOf(notificationManager)`,
  `getNotification(id)`, `size()`).
- Docs: `docs/adr/0007-cooking-timer-alarm.md:63-65, 142, 159`; `docs/adr/0008-…:74-79, 84-89, 97-101, 145-146`;
  `docs/specs/cooking-timer-foreground-service.md:115, 123-129, 131-137, 149-158, 167-172`;
  `docs/technical-discovery.md:403-443, 445-469`; `docs/domain-model.md:139-147`;
  `docs/risks-and-open-questions.md:177-181, 228-244`.

### Existing Patterns To Follow

- **Costura `interface` + default `NoOp…`**: decisión **pura** en `ui.cooking` (sin `android.*`/`androidx.*`),
  adaptador delgado en `data.cooking`, cableado **por parámetro** de `MainActivity` a `CookingModeScreen`; con
  **instantes inyectados** (la política nunca lee el reloj) y **degradación silenciosa** (ADR 0005, `runCatching`).
- **Verificación canónica en JVM**: `bash init.sh` o `bash scripts/gradle.sh <tareas>`; Robolectric para lo que toca
  framework; nunca `./gradlew` directo ni `--stop`. **Ids fijos y contenido genérico**: `1001` descartable / `1000`
  `ongoing`; ninguna notificación recibe la receta; el único extra que cruza al sistema es `EXTRA_RESUME_COOKING`.

### Current Gaps

`RUNNING → FINISHED` mata la alarma pendiente; `isResumable` excluye `FINISHED` (incoherente con el escenario 6
aceptado de la feature anterior, y hallazgo **Low L1** de
`docs/validations/cooking-timer-foreground-service.md`); la costura no puede **retirar** una notificación ya mostrada
(decisión 9 del ADR 0007); `docs/domain-model.md:144-146` dice que `Terminado` no es reanudable "por sí solo".

## Technical Approach

**P1**: en `onTimerChange` se añade **antes** de la regla genérica de cancelación: si `next` entra en `FINISHED` con
transición real (`previous != next`) → `None`. Orden final del `when`: (1) `!previous.isRunning && next.isRunning &&
remaining > 0` → `Schedule`; (2) `next.isRunning` → `None`; (3) **entra en `FINISHED`** → `None`; (4)
`previous != next` → `Cancel` (Pausar/Reiniciar/cambio de paso/salir); (5) resto → `None`. **No** se toca
`CookingTimerService`: su `delay` ya retira la `1000` y se detiene. Se actualizan los KDoc de `Cancel` (`:19`) y de las
reglas (`:39-40`), que hoy dicen "llegar a cero".

**P2**: sin cambios de código; se añade la aserción de que la `1001` no lleva `FLAG_ONGOING_EVENT`.

**P3**: nuevo predicado en `CookingSession`: `val reopensFromNotification: Boolean get() = isResumable ||
timer.status == CookingTimerStatus.FINISHED` (En marcha, Pausado o Terminado; `Inactivo` fuera), consumido por
`MainShell:184`. Se **no** amplía `isResumable`: mantiene su semántica aceptada ("hay cuenta atrás que reanudar") y sus
aserciones vigentes, y sigue siendo código vivo porque el predicado nuevo lo **compone**. En la reapertura
`resumedMode(now)` da Terminado con `remaining = 0`, el rearme no hace nada (solo actúa En marcha) y el "¡Listo!" en
pantalla no cambia.

**P4**: la costura `CookingAlarm` gana `fun dismissFinishNotification()` (`NoOpCookingAlarm` → `Unit`;
`AndroidCookingAlarm` → `NotificationManagerCompat.from(appContext).cancel(1001)` en `runCatching`). Es **distinto** de
`Cancel` (que sigue sin retirar notificaciones mostradas: decisión 9 intacta) y lo llama **solo** el
`LaunchedEffect(exiting)` de `CookingModeScreen` (`:168-174`), **no** el `onDispose`.

**Trade-off asumido (documentar, no mitigar)**: con la app en primer plano el canal de importancia alta puede
sonar/vibrar (heads-up) **además** del "¡Listo!" en pantalla; **no** hay duplicado (mismo id `1001`, disparo único).

## Expected File Changes

Rutas relativas a `app/src/main/java/com/sazon/app/` y a `app/src/test/java/com/sazon/app/`.

- `ui/cooking/CookingAlarm.kt` — modificar; arista `→ FINISHED` = `None`, KDoc de `Cancel`/reglas y `dismissFinishNotification()` en la costura (+ default no-op).
- `ui/cooking/CookingSession.kt` — modificar; `reopensFromNotification` + KDoc (sin cambiar `isResumable`); `ui/MainShell.kt`: `:184` con el predicado nuevo y KDoc `:112-118`.
- `ui/cooking/CookingModeScreen.kt` — modificar; `LaunchedEffect(exiting)` limpia el aviso antes de `session.clear()`/`onClose()`; `onDispose` **sin diff**.
- `data/cooking/AndroidCookingAlarm.kt` — modificar; implementa el método nuevo (`Cancel` igual).
- Tests: `ui/cooking/CookingAlarmPolicyTest.kt` (invertir `llegar_a_cero_cancela_y_no_reprograma` → `None`, corrida completa con **0** `Cancel`, no-regresión, no-op del método nuevo) y `CookingSessionTest.kt` (predicado nuevo y reapertura de Terminado con `remaining = 0`).
- Tests: `data/cooking/AndroidCookingAlarmTest.kt` (retira la `1001`, idempotente/sin permiso no lanza y `Cancel` **no** la retira, guarda de la decisión 9) y `CookingNotificationsTest.kt` (la `1001` no es `ongoing`).
- **No** se tocan: `CookingTimerService`, `CookingAlarmReceiver`, el manifiesto, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `ui/theme/**`, assets ni `docs/design/**`.

## Visual Design Impact

UI involved: **no**. Ambas notificaciones son superficies del **sistema operativo**: sin pantalla, token, microcopy ni captura nueva (excepción ya registrada en `DESIGN.md` por `cooking-timer-alarm`; `DESIGN.md` no cubre notificaciones de sistema). No se crea `docs/design/concepts/cooking-timer-finish-notification/`.

## Durable Documentation Impact

- `docs/adr/0007-cooking-timer-alarm.md`: **update** — nota en la decisión 9 y la consecuencia `:142` (`RUNNING → FINISHED` ya **no** emite `Cancel`; `Cancel` sigue sin retirar notificaciones mostradas y la limpieza de la `1001` es un comando aparte); `docs/adr/0008-cooking-timer-foreground-service.md`: **update** — notas en las decisiones 7/12 y consecuencias `:145-146` (Terminado también reabre; el servicio se para con su `delay`).
- `docs/specs/cooking-timer-foreground-service.md`: **update** — nota de vigencia en los escenarios 2, 3, 4, 6 y 8 (donde dice que la política devuelve `Cancel` al llegar a cero y que la notificación obsoleta sobrevive a la salida explícita), sin reescribir la evidencia histórica.
- `docs/technical-discovery.md`: **update** — párrafo del aviso (`:409-410`, quitar "llegar a cero" de la lista de `Cancel`) y párrafo de la cuenta atrás (`:445-469`: quién para el servicio, limpieza de la `1001`, reapertura en Terminado); `docs/domain-model.md`: **update** — `:144-146`, una sesión en `Terminado` **sí** reabre el paso.
- `docs/risks-and-open-questions.md`: **update** — filas `:177` (el aviso no llega) y `:178` (el toque no devuelve al paso, ahora también en Terminado); **nueva** fila del trade-off "heads-up además del ¡Listo!" (mismo id, sin duplicado) y de que la rotación cierra el Modo cocina (preexistente, fuera de alcance).
- `AGENTS.md`, `docs/build-brief.md` y `DESIGN.md`: **not needed** — no cambian el flujo de agentes ni el arranque, el Non-Goal "Notificaciones y recordatorios" ya quedó enmendado por ADR 0007/0008 y no hay tokens ni pantallas nuevas.

## Implementation Plan

1. **P1**: arista "entra en `FINISHED` → `None`" en `onTimerChange` + KDoc actualizados, con los tests de la política
   invertidos y la no-regresión de Pausar/Reiniciar/cambio de paso/salir/`FINISHED → IDLE`.
2. **P3**: `CookingSession.reopensFromNotification` (+ KDoc), uso en `MainShell` y tests del predicado y de la
   reapertura de Terminado.
3. **P4**: `dismissFinishNotification()` en la costura, implementación en `AndroidCookingAlarm`, llamada desde
   `LaunchedEffect(exiting)` y tests de adaptador (retira la `1001`; `Cancel` no la retira).
4. **P2**: aserción de que la `1001` es descartable y no `ongoing`.
5. **Docs durables** y **verificación** (`bash init.sh`, `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
   `lint --rerun-tasks`) con la evidencia registrada y la prueba manual anotada.

## Implementation Tasks

- [ ] `CookingAlarmPolicy.onTimerChange`: arista `next.status == FINISHED && previous != next -> None` **antes** de `previous != next -> Cancel`; KDoc de `CookingAlarmCommand.Cancel` y de las reglas sin "llegar a cero".
- [ ] `CookingSession.reopensFromNotification` (`isResumable || status == FINISHED`) con KDoc; `isResumable` sin cambios.
- [ ] `CookingSessionTest`: predicado (En marcha/Pausado/Terminado → `true`; Inactivo → `false`) y `resumedMode` de una sesión Terminada → `FINISHED` con `remaining = 0`.
- [ ] `MainShell`: gate `:184` con el predicado nuevo + KDoc `:112-118`.
- [ ] Costura `CookingAlarm.dismissFinishNotification()` + `NoOpCookingAlarm` (no-op) con KDoc "no lanza"; `AndroidCookingAlarm.dismissFinishNotification()` con `NotificationManagerCompat.cancel(1001)` en `runCatching`.
- [ ] `CookingModeScreen`: `LaunchedEffect(exiting)` limpia el aviso antes de `session.clear()`/`onClose()`; `onDispose` **sin diff**.
- [ ] `AndroidCookingAlarmTest`: retira la `1001`; idempotente y sin permiso no lanza; `Cancel` **no** la retira.
- [ ] `CookingNotificationsTest`: la `1001` no lleva `FLAG_ONGOING_EVENT`.
- [ ] Docs durables actualizados; gate en verde y evidencia registrada (sin commit/push/PR: los crea el orquestador).

## Verification Plan

- `bash init.sh` (camino estándar, WSL): exit 0 con `./gradlew test` + `./gradlew lint`, sin procesos largos y sin tocar `init.sh`. Baseline: **113 suites / 874 tests / 0 fallos**, lint 0 errores / 13 warnings preexistentes (0 bajo `app/src`). Evidencia fresca forzada con `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y `bash scripts/gradle.sh lint --rerun-tasks` (nunca `./gradlew` directo ni `--stop`).
- Automatizable en JVM (escenarios 1, 2 -publicación-, 3, 4 y 6): política pura, sesión pura y adaptadores con Robolectric (`ShadowAlarmManager` + `shadowOf(notificationManager)`).
- **Manual en emulador** (persona; `AGENTS.md`; escenarios 2 y 5): en primer plano el aviso **queda** en la bandeja al llegar a cero; minimizada, lo mismo; tocar el aviso vuelve al paso en Terminado; "Atrás"/"Terminar" limpian el aviso; una **rotación** no lo borra; descartar y "borrar todo" sin crash; sin permiso, nada se rompe.
- No hay E2E persistente: la UI de emulador no se automatiza, así que la cobertura es lógica/estado en JVM más revisión de la persona, como en las features aceptadas del Modo cocina.

## Evidence To Capture

- `bash init.sh` exit 0 y conteos del XML fresco (`app/build/test-results/testDebugUnitTest/TEST-*.xml`) con el delta respecto a `610fe14`; informe de lint (0 errores; 13 warnings preexistentes, 0 bajo `app/src`).
- Casos nuevos/modificados: `CookingAlarmPolicyTest` (cruce del cero → `None`; corrida completa con 0 `Cancel`; no-regresión), `CookingSessionTest` (`reopensFromNotification`), `AndroidCookingAlarmTest` (limpieza de la `1001`; `Cancel` no la retira) y `CookingNotificationsTest` (no `ongoing`).
- `git diff --stat`/`git status` con solo los ficheros previstos y diff de las notas durables; **pendiente manual** anotado por el orquestador en `PROGRESS.md`/`feature_list.json`.

## Decisions And Unknowns

- **P1**: la alarma es el **único** productor del aviso y la transición a Terminado no la cancela (Opción A). Se descarta mover el aviso al servicio (Opción B) y un `Cancel` "condicional" en el adaptador: la decisión debe seguir siendo **pura** y testeable en JVM.

  > **Nota de vigencia (`cooking-timer-notification-reliability`, 2026-10-16):** P1 (la transición a Terminado **no**
  > cancela la alarma) sigue vigente, pero "la alarma es el **único** productor" deja de serlo: el aviso puntual
  > lo publica el **servicio** (productor primario) y la alarma es el **respaldo**, sin duplicar ni repetir alerta
  > gracias al **sello de fin** compartido.
- **P3**: predicado nuevo en vez de ampliar `isResumable`, para no redefinir semántica aceptada ni sus aserciones; `Inactivo` queda fuera (no hay nada que reabrir y no puede coexistir con un aviso `1001`).
- **P4**: método dedicado en la costura en lugar de reutilizar `Cancel`, porque la decisión 9 del ADR 0007 prohíbe que `Cancel` retire notificaciones ya mostradas; la llamada vive solo en la salida explícita y **nunca** en `onDispose`. Si el implementer juzgara que excede lo razonable, la alternativa —dejarlo fuera— debe registrarse como decisión explícita en la evidencia, no implementarse a medias.
- **Trade-off aceptado / limitación preexistente**: heads-up en primer plano además del "¡Listo!" en pantalla, sin duplicado (mismo id, disparo único). Rotar o volver a la app cierra el Modo cocina (`MainShell` lo hospeda con `remember`) y el `onDispose` cancela alarma/servicio: **fuera de alcance** (un defecto aquí sería una **feature correctiva** nueva). Doze/batería/OEM siguen siendo prueba manual.

## Validator Checklist

- [ ] `onTimerChange` devuelve `None` al entrar en `FINISHED`; `Cancel` sigue para Pausar/Reiniciar/cambio de paso/salir/`onExit()`/`FINISHED → IDLE`; los `tick` siguen dando `None`.
- [ ] Los escenarios 1-6 pasan (1, 2 -publicación-, 3, 4 y 6 en JVM; 2 y 5 con la prueba manual anotada).
- [ ] La verificación exigida está presente: `bash init.sh` en verde + evidencia fresca y conteos del XML.
- [ ] `CookingAlarm.kt` y `CookingSession.kt` siguen **sin** imports `android.*`/`androidx.*` y sin leer el reloj.
- [ ] `dismissFinishNotification` se llama **solo** en la salida explícita; `onDispose` sigue emitiendo únicamente `onExit()`/`Cancel` y una rotación **no** borra la `1001`.
- [ ] La `1001` mantiene id, canal, microcopy y small icon, y **no** es `ongoing`; ninguna notificación expone datos de la receta.
- [ ] Sin dependencias, permisos, manifiesto, build/lockfile, `init.sh`, `scripts/**`, `domain/**` ni Room tocados; `CookingTimerService` sin diff.
- [ ] Docs durables actualizados (ADR 0007/0008 y nota de vigencia en la spec anterior, sin reescribir evidencia).
- [ ] No hay comportamiento no pedido (Opción B descartada, sin servicio vivo tras el fin, sin canal nuevo, sin tocar el microcopy ni la UI del Modo cocina).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por el orquestador (no por quien implementa).
