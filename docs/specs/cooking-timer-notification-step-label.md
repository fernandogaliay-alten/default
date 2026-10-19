# Feature Implementation Spec: La notificación del temporizador muestra el número y el título del paso

## Source Feature

- `id` `cooking-timer-notification-step-label` · `area` `integration` · `status` `not_started` (al planificar)
- `depends_on`: `cooking-timer-foreground-service` (`accepted`) · `cooking-timer-notification-reliability`
  (`accepted`)
- `source`: `feature_list.json:1837-1850` (el `user_visible_behavior`, las **tres** verificaciones y las
  `notes` con el alcance acordado, fuente de verdad: "ampliar `CookingTimerService.intent` y
  `CookingNotifications.ongoing(...)` con la etiqueta ('Paso N de M' + título del paso) y fijar la decisión
  de privacidad en los ADR"; "La persona **acepta** que el número y el título del paso crucen a la
  notificación del sistema").
- Mejora pedida por la persona (2026-10-16). **No** reabre lo aceptado: la **amplía** de forma acotada
  (el aviso de fin `1001` y su contenido genérico siguen intactos).

## Goal

Mientras corre la cuenta atrás de un paso del Modo cocina, la notificación **persistente** (`1000`) debe
decir **qué paso** es: el número en el formato ya aceptado de la pantalla ("Paso N de M", misma microcopy
del encabezado del Modo cocina) y el **título del paso**, además de la cuenta atrás que ya pinta el
sistema. El usuario sabe en qué punto de la receta está sin abrir la app.

Hoy es imposible porque la notificación persistente solo tiene el título fijo `ONGOING_TITLE`
("Cocina en marcha") y su cuenta atrás, y porque el **único** dato que cruza al servicio son dos escalares
(`EXTRA_END_AT_ELAPSED_MILLIS` del fin en `ELAPSED_REALTIME` y `EXTRA_FINISH_WALL_CLOCK_MILLIS`, el sello
de fin compartido con la alarma): el servicio **no recibe** el paso ni el título, así que no puede pintarlos
(`app/src/main/java/com/sazon/app/data/cooking/CookingTimerService.kt:148-155` y
`CookingNotifications.kt:159-176`). Esta feature hace cruzar **solo** ese dato (número + título), ni un
carácter más de la receta, y lo fija como decisión de privacidad en los ADR.

## Non-Goals

- **No** cruzan la descripción del paso, los ingredientes, las notas, los tips, la nutrición ni la clave de
  IA; ni la `Recipe`. El aviso de fin `1001` **no** cambia (sigue con el copy genérico
  `"¡Listo! El paso ha terminado"` / `"Toca para volver al modo cocina."`) ni lo hace el `PendingIntent`
  de la alarma (que sigue llevando **solo** el sello del fin).
- **No** se toca la cuenta atrás: sigue pintándola el **sistema** (`setUsesChronometer` +
  `setChronometerCountDown` + `when` en hora de pared **recalculado al publicar**), sin publicaciones por
  segundo ni latidos en el servicio (el inventario completo de "sin diff" está en `Expected File Changes`).
- **No** se persiste la etiqueta (se **deriva** del paso actual + la `Recipe` en el momento de armar el
  trabajo del sistema): sin cambio de fila, columnas ni mapeadores de Room.
- **No** se arreglan limitaciones preexistentes ya aceptadas (la rotación que cierra el Modo cocina, la
  `1001` obsoleta de un paso anterior, el doble reloj de pared/monotónico del cronómetro del sistema).
- **No** hay dependencias nuevas.

## Job Story

When pongo el temporizador de un paso y sigo cocinando con el móvil guardado, I want que la notificación me
diga el número del paso y su título (no solo "Cocina en marcha"), so I can saber en qué punto de la receta
estoy sin desbloquear el móvil ni volver a la app.

## Users And Permissions

- **Usuario local** (dueño único del dispositivo): arranca el temporizador de un paso y ve la notificación
  persistente con el número y el título del paso mientras corre la cuenta atrás.
- Sin permisos nuevos y sin rationale nuevo: `POST_NOTIFICATIONS` se sigue pidiendo **en contexto** y sin
  gatear nada. Sin permiso (o con las notificaciones apagadas) no hay notificación y **nada** lanza
  (ADR 0005); la etiqueta **no** cambia ese comportamiento.
- Decisión de privacidad **explícita**: la persona acepta que el número y el título del paso crucen al
  sistema (queda en los ADR 0007/0008 y en `docs/risks-and-open-questions.md`; el canal es
  `VISIBILITY_PUBLIC`, así que ese texto se ve en la pantalla de bloqueo).

## Acceptance Scenarios

### Scenario 1: la persistente muestra el número y el título del paso (verificación exigida "Test JVM/Robolectric")

Given el temporizador del paso 2 de 5 (`stepIndex = 1`, `totalSteps = 5`, título "Dorar el pollo") recién
arrancado y su trabajo de sistema armado,
When el servicio en primer plano publica la notificación persistente (`1000`),
Then la notificación lleva `EXTRA_TITLE = "Paso 2 de 5"` y `EXTRA_TEXT = "Dorar el pollo"`, sigue siendo
`ongoing` + `onlyAlertOnce` + `silent` con el cronómetro descendente y su `when`, y el `Intent` que arrancó
el servicio llevaba esos mismos dos textos.

### Scenario 2: el número reutiliza la microcopy del encabezado y solo cruzan número y título (privacy guard)

Given una receta de `n` pasos y el paso `stepIndex`,
When se compone la etiqueta del paso,
Then el número es **exactamente** `CookingModeFormat.progressLabel(stepIndex, totalSteps)` ("Paso N de M",
sin microcopy paralela) y la etiqueta contiene **solo** ese número y el `title` del paso: en el `Intent` del
servicio y en la notificación **no** aparece la descripción del paso, ni ingredientes, ni notas, ni la clave
de IA (los tests de firma y de contenido lo acreditan).

### Scenario 3: la etiqueta nunca queda obsoleta

Given el temporizador del paso 2 corriendo (la persistente dice "Paso 2 de 5"),
When el usuario Pausa, Reinicia, cambia de paso o sale del modo,
Then el servicio se para y su notificación desaparece (`Cancel`), y al arrancar el temporizador de otro paso
la persistente muestra la etiqueta del **paso nuevo** (cada armado compone la etiqueta del paso actual; no
hay etiqueta heredada).

### Scenario 4: la reanudación desde la notificación rearma con la etiqueta del paso persistido

Given una sesión persistida En marcha en el paso 3 y el toque de la notificación (o el arranque con
`resume != null`),
When la pantalla se compone y rearma **una vez** el trabajo del sistema,
Then el `Intent` del servicio lleva la etiqueta del paso 3 (compuesta desde el estado reanudado y la
`Recipe`), no la del paso 1.

### Scenario 5: sin etiqueta se degrada al título genérico y nada lanza

Given un `Intent` del servicio **sin** la etiqueta (p. ej. el de un `Schedule` sin paso, o un proceso
recreado con un `Intent` viejo),
When el servicio pinta la persistente,
Then el título vuelve a `ONGOING_TITLE` ("Cocina en marcha"), sin texto de cuerpo, con el cronómetro y el
`when` intactos, y **no** hay excepción. Una etiqueta en blanco se trata como ausente.

### Scenario 6: la deuda aceptada no cambia (sin permiso, primer plano, aviso de fin)

Given el permiso denegado, o la app en primer plano, o el fin llegando,
When el servicio/receptor publican o retiran,
Then el comportamiento aceptado sigue **idéntico**: sin permiso no se publica y no se lanza, el
"¡Listo!" en pantalla y el aviso `1001` (con su sello e idempotencia) no cambian, y el `onDispose` de la
pantalla sigue sin limpiar notificaciones.

### Scenario 7: prueba manual en el emulador (verificación exigida "Prueba manual")

Given el Modo cocina con el temporizador de un paso En marcha (no automatizable desde WSL, `AGENTS.md`),
When la persona mira la notificación persistente,
Then dice "Paso N de M" y el título del paso, con la cuenta atrás decreciendo; al cambiar de paso y
arrancar el nuevo temporizador, el texto es el del paso nuevo; sin permiso o con el texto largo, nada se
rompe y el acabado se revisa contra `DESIGN.md`.

## Repository Research

### Files Inspected

- `feature_list.json:1837-1850` — la feature (título, comportamiento visible, las tres verificaciones y las
  `notes` con el alcance: `intent` + `ongoing(...)` + el número/título + los ADR).
- `ui/cooking/CookingAlarm.kt` (129 líneas) — `CookingAlarmCommand.Schedule(triggerAtMillis)` (`:18`) y
  `CookingAlarmPolicy.onTimerChange(previous, next, nowMillis)` (`:67-84`, la regla 1 de `Schedule` en
  `:72-75`); `CookingAlarm.apply(command)`/`dismissFinishNotification` (`:107`, `:118`); `NoOpCookingAlarm`.
- `ui/cooking/CookingModeScreen.kt` (1085 líneas) — **los dos** call sites que arman el servicio:
  `onUserAction` (`:180-198`, `alarm.apply(CookingAlarmPolicy.onTimerChange(previous, next.timer, now))` en
  `:186`) y el rearme al reanudar (`LaunchedEffect(Unit)`, `:203-213`); la receta y el estado están en
  ámbito (`recipe.steps`, `state.stepIndex`, `state.totalSteps`, `recipe.steps.getOrNull(...)` en `:330`);
  el estado vive en `rememberSaveable(stateSaver = CookingModeStateSaver)` (`:155`, `Saver` en `:1052`), que
  **no** cambia.
- `ui/cooking/CookingModeFormat.kt` (41 líneas) — `progressLabel(stepIndex, totalSteps) = "Paso N de M"`
  (`:16`, la microcopy del encabezado), `progressFraction`, `durationLabel`, `countdown`.
- `ui/cooking/CookingModeState.kt` (86 líneas) — `stepIndex`/`totalSteps`/`isEmpty` y `next/previous`
  (que **reinician** el temporizador del paso nuevo ⇒ `Cancel` en la política: la etiqueta nunca queda
  obsoleta).
- `ui/cooking/CookingSession.kt` (105 líneas) — `of`/`resumedMode`/`reopensFromNotification`; la sesión ya
  lleva la `Recipe` y el `stepIndex`, así que la etiqueta es **derivable** y **no** se persiste.
- `data/cooking/CookingTimerService.kt` (156 líneas) — `onStartCommand` (`:59-94`, `startForeground` con
  `ongoing(...)` en `:74-86`), `scheduleFinish` (`:109-126`), `wallClockEnd` (`:135-136`), el extra
  `EXTRA_END_AT_ELAPSED_MILLIS` (`:141`) y `intent(context, endAtElapsedMillis, finishWallClockMillis)`
  (`:148-155`).
- `data/cooking/CookingNotifications.kt` (253 líneas) — `NOTIFICATION_ID = 1001` (`:56`),
  `ONGOING_NOTIFICATION_ID = 1000` (`:69`), `ONGOING_TITLE = "Cocina en marcha"` (`:76`),
  `notification(context, contentIntent, whenMillis)` (`:132-143`), `ongoing(context, endWallClockMillis,
  contentIntent)` (`:159-176`), `contentIntent` (`:185-194`), `notifyFinish` (`:209-224`), `canPost`
  (`:243-249`).
- `data/cooking/AndroidCookingAlarm.kt` (182 líneas) — `schedule(at)` (`:84-112`, calcula el sello una vez y
  llama a `CookingTimerService.intent(appContext, at, finishWallClockMillis)` en `:109`),
  `broadcastPendingIntent` (`:148-162`), `cancel` (`:118-127`).
- Tests vigentes: `data/cooking/CookingTimerServiceTest.kt` (**11**, `:164-182` "los dos escalares ... son lo
  unico que viaja al servicio", `:97-142` contenido de la persistente, `:145-161` sin cuerpo de texto),
  `data/cooking/CookingNotificationsTest.kt` (**22**, `:186-205` test **estructural** de firmas de
  `notification`/`notifyFinish`, `:377-423` la persistente con `assertNull(EXTRA_TEXT)`, `:150-164`
  `ongoing` vs `1001`), `data/cooking/AndroidCookingAlarmTest.kt` (**14**, `:141-164` extras del `Intent`
  del servicio, `:166-198` el sello en los dos `Intent`), `ui/cooking/CookingAlarmPolicyTest.kt` (**17**,
  `:26-49` y `:276` `Schedule(...)`), `ui/cooking/CookingSessionTest.kt` (**16**, `:242-276` el rearme).
- Docs: `docs/adr/0007-cooking-timer-alarm.md:69-79` (decisión 7: contenido genérico, ausencia de datos de
  receta **estructural**, y la nota de enmienda del sello), `:121-122` (decisión 12);
  `docs/adr/0008-cooking-timer-foreground-service.md:42-60` (decisiones 1 y 2: la cuenta atrás la pinta el
  sistema, id `1000`, y la enmienda de la `1001`) y `:120-135` (el sello, "escalar que comparten los dos
  publicadores"); `docs/technical-discovery.md:499-523` (párrafo de la cuenta atrás: "titulo fijo «Cocina en
  marcha»" y "Su **unico** dato de entrada son dos escalares", ambas frases a reconciliar);
  `docs/specs/cooking-timer-foreground-service.md:212-219` (escenario 9: "la persistente expone **solo** el
  título fijo y el instante de fin"); `DESIGN.md:300` (excepción de capturas que cita el "título fijo
  «Cocina en marcha»"); `docs/risks-and-open-questions.md:180-183` (canal de importancia alta inmutable,
  cronómetro del fabricante, heads-up).
- Baseline re-derivada **en este worktree**: rama `feature/cooking-timer-notification-step-label`, `HEAD`
  `3f3f0fe` (merge de la PR #163), `git grep -c '@Test' HEAD -- app/src/test` = **116 ficheros / 931
  anotaciones**; referencia del XML previo de `cooking`: **115 suites / 916 tests / 0 fallos** (el worktree
  sí tiene `app/build/test-results/`, pero la tirada fresca la produce el implementer).

### Existing Patterns To Follow

- **Costura + lógica pura + adaptador delgado**: decisión/label **pura** en `ui.cooking` (sin
  `android.*`/`androidx.*`, instantes y textos inyectados) y adaptador Android en `data.cooking` con
  contrato "**no lanza**" y degradación silenciosa (ADR 0005, `runCatching` por pata).
- **Verificación canónica en JVM**: `bash init.sh` (test + lint) o `bash scripts/gradle.sh <tareas>`;
  Robolectric (`@Config(sdk = [33])`, `shadowOf(notificationManager)`, `ShadowService`,
  `ShadowAlarmManager`) para lo que toca framework; **nunca** `./gradlew` directo ni `--stop`.
- **Ids fijos y garantía estructural**: `1000` ongoing / `1001` descartable; las funciones de construcción
  **no reciben la `Recipe`** y hay un test que inspecciona las firmas (`CookingNotificationsTest:186-205`);
  el `PendingIntent` de la alarma lleva un **único** escalar y los `Intent` de contenido solo el booleano
  `EXTRA_RESUME_COOKING`.
- **Microcopy pura reutilizada**: el número del paso ya existe como `CookingModeFormat.progressLabel`; la
  microcopy de este repo vive en constantes puras, no en recursos.
- **Sin persistencia de lo derivable**: lo que se puede recalcular del estado + la receta no se guarda (la
  sesión persiste la `Recipe` y el `stepIndex`, de donde sale la etiqueta).

### Current Gaps

La etiqueta **no existe** en ninguna capa: no hay tipo/microcopy que la componga, el comando `Schedule` no
la transporta, el `Intent` del servicio solo lleva dos escalares y `ongoing(...)` no acepta texto. Además,
la garantía "no cruzan datos de la receta" está redactada como **absoluta** en los ADR y en
`technical-discovery.md`, así que la feature debe **acotar** esa regla (solo el número y el título, por
decisión explícita de la persona) en lugar de dejar el texto vigente contradiciendo al código.

## Technical Approach

Una etiqueta **pura** del paso cruza por la costura ya existente (comando → `Intent` del servicio →
notificación persistente); nada más cambia de forma ni de comportamiento.

1. **`ui/cooking/CookingStepLabel.kt` (nuevo)**: tipo puro
   `data class CookingStepLabel(val progress: String, val title: String)` con
   `fun of(stepIndex: Int, totalSteps: Int, step: RecipeStep?): CookingStepLabel?`: devuelve `null` si
   `step == null` o `totalSteps <= 0` (degradación defensiva, patrón `steps.getOrNull`) y si no
   `CookingStepLabel(CookingModeFormat.progressLabel(stepIndex, totalSteps), step.title)` — **reutiliza** la
   microcopy del encabezado, no crea una paralela. KDoc: es la **frontera de privacidad** (lo único
   derivado de la receta que puede cruzar al sistema).
2. **`ui/cooking/CookingAlarm.kt`**: `CookingAlarmCommand.Schedule` gana
   `val stepLabel: CookingStepLabel? = null` y `CookingAlarmPolicy.onTimerChange` gana el último parámetro
   `stepLabel: CookingStepLabel? = null`, que se copia **solo** en la rama de `Schedule` (`:72-75`). El
   default `null` conserva los call sites y las aserciones de los tests aceptados (el comando sin etiqueta
   sigue siendo el mismo objeto) y el resto de reglas (`None`/`Cancel`) no cambia.
3. **`ui/cooking/CookingModeScreen.kt`**: un helper local (p. ej. `fun stepLabelOf(state: CookingModeState)`)
   que compone la etiqueta del paso **de ese estado** con la receta en ámbito, y se pasa en **los dos**
   armados: `onUserAction` (etiqueta del estado **nuevo** `next`, que en la rama `Schedule` es el paso
   actual) y el rearme al reanudar (`LaunchedEffect(Unit)`, con `resume`/`state`). Ni la UI, ni el `Saver`,
   ni la persistencia, ni la confirmación de salida/cambio de paso cambian.
4. **`data/cooking/AndroidCookingAlarm.kt`**: `schedule(at, stepLabel)` pasa la etiqueta a
   `CookingTimerService.intent(appContext, at, finishWallClockMillis, stepLabel)`. El `PendingIntent` de la
   alarma **no** la recibe (su `Intent` sigue con el único escalar del sello).
5. **`data/cooking/CookingTimerService.kt`**: dos extras nuevos
   (`EXTRA_STEP_PROGRESS = "com.sazon.app.extra.cooking.STEP_PROGRESS"` y
   `EXTRA_STEP_TITLE = "com.sazon.app.extra.cooking.STEP_TITLE"`), `intent(context, endAtElapsedMillis,
   finishWallClockMillis, stepLabel: CookingStepLabel?)` (los añade **solo** si la etiqueta no es `null`,
   para que el `keySet` siga siendo exacto y comprobable) y `onStartCommand` los lee con
   `getStringExtra(...)` y los pasa a `ongoing(...)`. El resto (fin ya pasado, `delay`, publicación de la
   `1001`, retirada, `START_NOT_STICKY`, "no lanza") no cambia.
6. **`data/cooking/CookingNotifications.kt`**: `ongoing(context, endWallClockMillis, stepProgress:
   String?, stepTitle: String?, contentIntent)` pinta `setContentTitle(stepProgress?.takeIf { it.isNotBlank()
   } ?: ONGOING_TITLE)` y, solo si el título no está en blanco, `setContentText(stepTitle)`. Sin etiqueta la
   notificación queda **igual** que hoy (título genérico, sin cuerpo). `ONGOING_TITLE` se conserva como
   **fallback** documentado. `notification`/`notifyFinish`/`contentIntent`/el canal/los ids **no** cambian.

## Expected File Changes

Rutas relativas a `app/src/main/java/com/sazon/app/` y `app/src/test/java/com/sazon/app/`.

- `ui/cooking/CookingStepLabel.kt` — **crear**; tipo + `of(...)` puros (frontera de privacidad), reutilizando
  `CookingModeFormat.progressLabel`.
- `ui/cooking/CookingAlarm.kt` — modificar; `Schedule(triggerAtMillis, stepLabel = null)`,
  `onTimerChange(..., stepLabel = null)` y el KDoc de la regla 1 (el comando transporta la etiqueta).
- `ui/cooking/CookingModeScreen.kt` — modificar; helper de la etiqueta y los **dos** armados con ella
  (`onUserAction` y el rearme al reanudar) + KDoc del párrafo del trabajo de sistema.
- `data/cooking/AndroidCookingAlarm.kt` — modificar; `schedule(at, stepLabel)` → `CookingTimerService.intent`.
- `data/cooking/CookingTimerService.kt` — modificar; los dos extras de la etiqueta, el parámetro de `intent`
  y su lectura para `ongoing(...)`; KDoc (ya no son "solo dos escalares").
- `data/cooking/CookingNotifications.kt` — modificar; `ongoing(...)` con el número y el título y el fallback
  al título genérico; KDoc.
- `test/.../ui/cooking/CookingStepLabelTest.kt` — **crear**; matriz pura (ver Verification Plan).
- `test/.../ui/cooking/CookingAlarmPolicyTest.kt` — modificar; la etiqueta viaja en `Schedule` y **no**
  cambia el instante programado ni las demás reglas.
- `test/.../data/cooking/CookingTimerServiceTest.kt` — modificar; el `keySet` del `Intent` (fin + sello +
  etiqueta), la persistente con número y título, sin etiqueta ⇒ título genérico, y el helper
  `conCuentaAtras` con etiqueta.
- `test/.../data/cooking/CookingNotificationsTest.kt` — modificar; la persistente con número y título, el
  fallback sin etiqueta, el texto en blanco tratado como ausente y el test **estructural** de firmas
  ampliado a `ongoing` (admite `String`).
- `test/.../data/cooking/AndroidCookingAlarmTest.kt` — modificar; la etiqueta viaja **solo** al `Intent` del
  servicio y **nunca** al `PendingIntent` de la alarma (que sigue con el único escalar del sello).
- **No** se tocan: `CookingModeState`, `CookingModeFormat`, `CookingTimerState`, `CookingSession`,
  `CookingSessionStore`, `CookingFinishPublication`, `CookingAlarmReceiver`, `NotificationPermission*`,
  `Room*`/`CookingSession*` de `data.cooking`, el manifiesto, `res/**`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `ui/theme/**`, `ui/MainShell.kt`,
  `MainActivity.kt` ni `SazonApp.kt`.

## Visual Design Impact

- UI involved: **no** en la app; lo que cambia es el **texto de una notificación del sistema operativo**.
- Design source: `DESIGN.md` (excepción de capturas ya registrada para las notificaciones del temporizador).
- Screens or states affected: ninguno; el Modo cocina (encabezado, tarjeta del paso, temporizador,
  "¡Listo!", diálogos) queda **idéntico**. **No** se crea
  `docs/design/concepts/cooking-timer-notification-step-label/`.
- New design artifact required: **no** — no hay tokens, tipografías, formas ni pantallas nuevas; el número
  reutiliza la microcopy aceptada del encabezado ("Paso N de M") y el título viene del dato de la receta.
  `DESIGN.md` solo necesita una **nota de precisión** (el título de la persistente deja de ser fijo).

## Durable Documentation Impact

- `docs/adr/0007-cooking-timer-alarm.md`: **update** — nota en la **decisión 7** (alcance de la garantía
  estructural: el aviso `1001` y el `PendingIntent` de la alarma siguen **igual**, y desde esta feature
  `ongoing(...)` acepta **solo** dos cadenas —el número y el título del paso— además de `Context`,
  `PendingIntent` y los escalares, por decisión explícita de la persona).
- `docs/adr/0008-cooking-timer-foreground-service.md`: **update** — nota en la **decisión 2** (la
  persistente ya **no** tiene el título fijo "Cocina en marcha" mientras corre el temporizador: muestra
  "Paso N de M" y el título del paso; el título genérico queda como **fallback** cuando la etiqueta falta) y
  en la del **sello/datos que cruzan** (`:120-135`): el `Intent` del servicio lleva, además de los dos
  escalares, la etiqueta del paso; el `PendingIntent` de la alarma **no** cambia. La privacidad se fija aquí
  como decisión explícita (número + título; nunca descripción, ingredientes, notas ni clave de IA).
- `docs/technical-discovery.md`: **update** — párrafo de la cuenta atrás (`:499-523`): la persistente deja
  de tener título fijo y el `Intent` del servicio ya no lleva "dos escalares" como único dato (lleva también
  el número y el título), con el límite explícito de lo que cruza.
- `docs/specs/cooking-timer-foreground-service.md`: **update** — nota de **vigencia** (sin reescribir la
  evidencia histórica) en el **escenario 9** (`:212-219`) y donde afirme que el único dato de entrada es el
  instante de fin.
- `DESIGN.md`: **update** — línea de precisión en la excepción de capturas de
  `cooking-timer-foreground-service` (`:300`): el título de la persistente ya no es fijo; el acabado de la
  notificación lo revisa la persona y un defecto se corrige con una **feature correctiva**.
- `docs/risks-and-open-questions.md`: **update** — fila (o extensión de la del canal, `:180`) con el
  **trade-off de privacidad aceptado**: el número y el título del paso se ven en una notificación
  `VISIBILITY_PUBLIC` (pantalla de bloqueo) y los atributos del canal son inmutables; la etiqueta **no** se
  persiste (se deriva del paso + la receta) y no hay mitigación sin un canal nuevo, que sería una correctiva.
- `docs/domain-model.md`: **not needed** — no hay entidad, relación ni estado nuevos (la etiqueta es
  derivada y efímera).
- `AGENTS.md` y `docs/build-brief.md`: **not needed** — no cambian el flujo de agentes, el camino de
  arranque, el alcance del MVP ni las reglas de verificación.

## Implementation Plan

1. **Etiqueta pura**: `CookingStepLabel` + `CookingStepLabelTest` (frontera de privacidad, reutilizando
   `CookingModeFormat.progressLabel`).
2. **Transporte en la costura**: `Schedule`/`onTimerChange` con la etiqueta y `CookingAlarmPolicyTest` al
   día; los **dos** armados de `CookingModeScreen` pasándola.
3. **Servicio y notificación**: extras + `intent(...)` + `ongoing(...)` con número/título y fallback;
   `AndroidCookingAlarm` pasando la etiqueta (y **no** a la alarma).
4. **Tests de Android (Robolectric)**: `CookingTimerServiceTest`, `CookingNotificationsTest`,
   `AndroidCookingAlarmTest` (extras exactos, contenido, fallback y firma estructural ampliada).
5. **Docs durables + verificación**: ADR 0007/0008, `technical-discovery.md`, nota de vigencia en
   `cooking-timer-foreground-service.md`, `DESIGN.md` y `risks-and-open-questions.md`; `bash init.sh`,
   tiradas forzadas, conteos del XML/SARIF, inspecciones de diff y la prueba manual anotada como pendiente.

## Implementation Tasks

- [x] `ui/cooking/CookingStepLabel.kt` (nuevo): `data class` + `of(stepIndex, totalSteps, step)` con `null`
      si falta el paso o no hay pasos; KDoc de frontera de privacidad.
- [x] `CookingStepLabelTest`: matriz pura (número con la microcopy del encabezado, título, `null` sin paso,
      `null` con `totalSteps = 0`, primer/último paso, determinismo, sin descripción ni ingredientes).
- [x] `CookingAlarm.kt`: `Schedule(triggerAtMillis, stepLabel = null)` y
      `onTimerChange(..., stepLabel = null)` (solo la rama de `Schedule` lo copia) + KDoc.
- [x] `CookingAlarmPolicyTest`: la etiqueta viaja en `Schedule` y el instante programado no cambia; el resto
      de reglas (`None`/`Cancel`) siguen con el comando sin etiqueta.
- [x] `CookingModeScreen.kt`: helper de la etiqueta del estado y los **dos** armados (`onUserAction` con el
      estado nuevo y el rearme al reanudar) + KDoc.
- [x] `CookingTimerService.kt`: `EXTRA_STEP_PROGRESS`/`EXTRA_STEP_TITLE`, `intent(..., stepLabel)`
      (extras **solo** si no es `null`) y su lectura para `ongoing(...)`; KDoc.
- [x] `CookingNotifications.kt`: `ongoing(context, endWallClockMillis, stepProgress, stepTitle,
      contentIntent)` con `setContentTitle(progress ?: ONGOING_TITLE)` y `setContentText(title)` solo si no
      está en blanco; KDoc.
- [x] `AndroidCookingAlarm.kt`: `schedule(at, stepLabel)` → `CookingTimerService.intent(...)`; la alarma
      sigue sin la etiqueta.
- [x] `CookingTimerServiceTest`, `CookingNotificationsTest` y `AndroidCookingAlarmTest`: extras exactos,
      contenido de la persistente, fallback sin etiqueta/texto en blanco, firma estructural ampliada a
      `ongoing` y "la etiqueta solo va al servicio".
- [x] Docs durables actualizados (ADR 0007/0008, `technical-discovery.md`, nota de vigencia en
      `cooking-timer-foreground-service.md`, `DESIGN.md`, `risks-and-open-questions.md`), gate en verde y
      evidencia registrada (sin commit/push/PR: los crea el orquestador).

> **Nota del implementer (sesión 143, 2026-10-19):** tareas completadas; `bash init.sh` **exit 0** (117
> suites / 951 tests / 0 fallos / 0 errores / 0 saltados; SARIF 13 preexistentes / 0 errores / 0 bajo
> `app/src`). Dos precisiones de implementación, ambas dentro del alcance: (i) `ongoing(...)` trata una
> etiqueta **a medias** (número en blanco **o** título en blanco) como **ausente** —título genérico y **sin**
> cuerpo—, que es la lectura literal del escenario 5 ("una etiqueta en blanco se trata como ausente"); (ii)
> `CookingAlarmCommand`/`CookingAlarmPolicy`/`CookingTimerService.intent` usan el `stepLabel` con default
> `null` en los dos primeros (para no cambiar los call sites aceptados) y **sin** default en `intent(...)`
> (sus dos call sites, producción y tests, lo pasan explícito). Nada más se desvía de la spec.

## Verification Plan

- `bash init.sh` (camino estándar, WSL): exit 0 con `./gradlew test` + `./gradlew lint`, sin procesos largos
  y sin tocar `init.sh`. Baseline re-derivada en `HEAD 3f3f0fe`:
  `git grep -c '@Test' HEAD -- app/src/test` = **116 ficheros / 931 anotaciones**; el XML de
  `app/build/test-results/testDebugUnitTest/TEST-*.xml` debe cuadrar con esa base **más** los casos nuevos
  (referencia previa de `cooking`: 115 suites / 916 tests / 0 fallos) y el SARIF de lint con los 13
  resultados preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), **0 errores** y **0 bajo `app/src`**.
- Evidencia **fresca** forzada con `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
  `bash scripts/gradle.sh lint --rerun-tasks` (nunca `./gradlew` directo ni `--stop`), más la tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*" --tests "com.sazon.app.data.cooking.*"`.
- Automatizable en JVM (escenarios 1, 2, 3, 4, 5 y 6):
  - `CookingStepLabelTest` (pura): "Paso 2 de 5" + "Dorar el pollo" para el paso 2 de 5; primer/último paso;
    `null` con `step = null` y con `totalSteps = 0`; la etiqueta **no** contiene la descripción ni el
    número de minutos y **no** muta nada; `progressLabel` reutilizada (misma cadena que el encabezado).
  - `CookingAlarmPolicyTest`: `Schedule(anchor, label)` con la etiqueta y `Schedule(anchor, null)` sin ella;
    el instante programado es el mismo con y sin etiqueta.
  - `CookingTimerServiceTest` (Robolectric): el `keySet` del `Intent` del servicio es **exactamente**
    {fin, sello, progreso, título} (y solo {fin, sello} si no hay etiqueta); la persistente publicada lleva
    `EXTRA_TITLE = "Paso 2 de 5"` y `EXTRA_TEXT = "Dorar el pollo"` con el cronómetro y el `when` intactos;
    sin etiqueta el título es `ONGOING_TITLE` y no hay `EXTRA_TEXT`.
  - `CookingNotificationsTest` (Robolectric): la persistente con número y título; el fallback sin etiqueta;
    el texto en blanco tratado como ausente; **sin** claves propias `com.sazon`; sin rastro de la receta; y
    el test **estructural** ampliado a `ongoing` (admite `Context`, `PendingIntent`, escalares y `String`).
  - `AndroidCookingAlarmTest` (Robolectric): `Schedule(item, label)` ⇒ el `Intent` del servicio lleva la
    etiqueta y el `PendingIntent` de la alarma **no** (sigue con el único escalar del sello).
  - Deuda aceptada intacta (escenario 6): los casos vigentes sin permiso, con el fin ya pasado, con el
    servicio parado y con el aviso `1001` idempotente siguen pasando sin cambios de contrato.
- **Manual en el emulador** (persona; `AGENTS.md`; escenario 7): con el temporizador En marcha, la
  notificación persistente muestra "Paso N de M" y el título del paso con la cuenta atrás decreciendo; al
  cambiar de paso y arrancar de nuevo, el texto es el del paso nuevo; con el móvil bloqueado se lee (canal
  público) y sin permiso nada se rompe. El acabado lo revisa la persona; un defecto se corrige con una
  **feature correctiva**.
- No hay E2E persistente ni pruebas de UI de emulador en este repo (`AGENTS.md`): la cobertura es lógica
  pura + Robolectric (notificación e `Intent` de verdad) más la revisión manual, como en las features
  aceptadas del temporizador. Por eso **no** se añade cobertura E2E.

## Evidence To Capture

- `bash init.sh` exit 0; conteos del XML fresco y delta respecto a `3f3f0fe` (116 ficheros / 931
  anotaciones) con el reparto por clase; SARIF con 0 errores y 0 bajo `app/src`; tirada focal de
  `ui.cooking` + `data.cooking`.
- Casos nuevos/modificados por suite (nueva `CookingStepLabelTest`; `CookingAlarmPolicyTest`,
  `CookingTimerServiceTest`, `CookingNotificationsTest` y `AndroidCookingAlarmTest` según los escenarios 1-6).
- Inspecciones: `CookingStepLabel.kt` sin imports `android.*`/`androidx.*` y sin leer el reloj; los **dos**
  armados de `CookingModeScreen` pasando la etiqueta; `git status`/`git diff --stat` con **solo** los
  ficheros previstos; `CookingNotifications.notification`/`notifyFinish` y el `PendingIntent` de la alarma
  **sin diff**.
- Diff de las notas durables y la **prueba manual** anotada como pendiente para la persona por el
  orquestador en `PROGRESS.md`/`feature_list.json`.

## Decisions And Unknowns

- **Etiqueta derivada, nunca persistida**: se compone al armar el trabajo del sistema (gesto y rearme) desde
  `stepIndex`/`totalSteps` + `RecipeStep.title`, así que **no** hay cambio de Room ni de `CookingSession` y
  la etiqueta no puede quedar obsoleta (un cambio de paso **reinicia** el temporizador ⇒ `Cancel` para el
  servicio y lo vuelve a armar con la etiqueta del paso nuevo).
- **Una cadena de número + una de título (no una compuesta)**: la persistente tiene libres las dos líneas de
  contenido (el cronómetro ocupa la línea del `when`), así que `setContentTitle("Paso N de M")` +
  `setContentText(título)` evita truncar una línea larga y deja el texto más útil en el título. Se descarta
  la etiqueta única (`"Paso 2 de 5 · Dorar el pollo"`) y se descarta cambiar el título por el del paso (el
  número es el punto de referencia que la persona pidió).
- **`ONGOING_TITLE` como fallback**: sin etiqueta (o con texto en blanco) la notificación queda **igual**
  que hoy ("Cocina en marcha", sin cuerpo); es la degradación defensiva coherente con el contrato "no lanza"
  y con los paths de `Intent` ya existentes en los tests.
- **La etiqueta viaja solo al servicio**: el `PendingIntent` de la alarma y la `1001` no la llevan (no la
  necesitan: el aviso de fin es genérico y no cambia); así el camino del respaldo con el proceso muerto
  sigue sin exponer nada nuevo.
- **Frontera de privacidad explícita**: lo único derivado de la receta que cruza es el número y el
  `RecipeStep.title`. Se mantiene la garantía **estructural** (las firmas no reciben la `Recipe`) y se
  amplía el test de firmas a `ongoing`, que ahora admite exactamente `Context`, `PendingIntent`, escalares y
  `String`; `notification`/`notifyFinish` siguen **sin** `String`.
- **Incidencia de plataforma a evitar**: `Intent.putExtra(name, null: String?)` deja la **clave presente**
  en el `keySet` (valor nulo) y rompería la aserción "nada más cruza"; `intent(...)` debe añadir los dos
  extras **solo** si la etiqueta no es `null`.
- **Incógnitas**: (i) el aspecto real de la persistente con **dos** líneas de texto y el cronómetro (y su
  lectura en la pantalla de bloqueo, canal `VISIBILITY_PUBLIC`) solo se ve en el emulador: lo revisa la
  persona y un defecto se corrige con una **feature correctiva**; (ii) no se puede automatizar desde WSL la
  comprobación de que el sistema renderiza bien título/texto largos; (iii) el número de líneas visibles
  depende del fabricante (no se mitiga).

## Validator Checklist

- [ ] La persistente muestra **"Paso N de M"** (misma cadena que `CookingModeFormat.progressLabel`) y el
      **título** del paso mientras corre la cuenta atrás; el cronómetro, el `when`, `ongoing`, `silent` y el
      id `1000` no cambian.
- [ ] La etiqueta se compone en lógica **pura** (`CookingStepLabel`) y se pasa en los **dos** armados de
      `CookingModeScreen` (`onUserAction` y el rearme al reanudar).
- [ ] El `Intent` del servicio lleva **solo** {fin, sello, progreso, título} y la notificación/los `Intent`
      **no** exponen descripción, ingredientes, notas, tips, nutrición ni clave de IA; el test estructural de
      firmas cubre `ongoing` y `notification`/`notifyFinish` siguen sin aceptar texto de receta.
- [ ] El `PendingIntent` de la alarma, el aviso `1001`, `CookingFinishPublication`, el canal, los ids y el
      microcopy del aviso siguen **sin cambios**.
- [ ] Sin etiqueta (o con texto en blanco) se degrada al título genérico sin lanzar.
- [ ] Los escenarios 1-7 pasan (1-6 en JVM/Robolectric; el 7 con la prueba manual anotada como pendiente).
- [ ] La verificación exigida está presente: `bash init.sh` en verde + evidencia fresca, conteos del XML
      contra la baseline de `3f3f0fe` (116 ficheros / 931 anotaciones) y lint 0 errores / 0 bajo `app/src`.
- [ ] Sin dependencias, permisos, manifiesto, build/lockfile, `init.sh`, `scripts/**`, `domain/**`, Room,
      `ui/theme/**`, `res/**` ni pantallas del Modo cocina tocados; sin capturas nuevas.
- [ ] Docs durables actualizados (ADR 0007/0008, `technical-discovery.md`, nota de vigencia en
      `cooking-timer-foreground-service.md`, `DESIGN.md`, `risks-and-open-questions.md`), sin reescribir
      evidencia histórica.
- [ ] No hay comportamiento no pedido (ni persistencia de la etiqueta, ni canal nuevo, ni cambios de
      microcopy del aviso, ni arreglo de la `1001` obsoleta o de la rotación).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por el orquestador (no por quien implementa).
