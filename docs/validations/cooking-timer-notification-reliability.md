# Validación independiente — `cooking-timer-notification-reliability`

- **Feature**: `cooking-timer-notification-reliability` (`area: integration`, correctiva de la persona
  2026-10-16; depende de `cooking-timer-foreground-service` y `cooking-timer-finish-notification`, ambas
  `accepted`)
- **Spec**: `docs/specs/cooking-timer-notification-reliability.md`
- **Rama / worktree**: `feature/cooking-timer-notification-reliability`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-timer-notification-reliability`
- **Sesión**: 141 (subagente `validator` con contexto limpio, independiente del `implementer`)
- **Estado previo**: `passing` (implementer, sesión 140, trabajo en disco; `HEAD = e5012d1`, **sin commit,
  sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); `testDebugUnitTest`/`lintDebug` salieron `UP-TO-DATE` ⇒ **no** valen como evidencia fresca |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL 3m46s**, `34/34` tareas ejecutadas, `exit 0`; parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **115 suites / 916 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL 3m13s**, `30/30` tareas ejecutadas, `exit 0`; SARIF fresco (16:22): **13 resultados, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1; los 3 ficheros señalados son `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`) — **idéntico** al baseline aceptado |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` y `git grep -l '@Test'` | `HEAD e5012d1`: **114 ficheros / 896 anotaciones**; árbol de trabajo: **115 / 916** ⇒ delta exacto **+1 suite / +20 tests** |
| Reparto del delta | conteo propio por clase (fuente y XML) | `CookingTimerStateTest` 11→**15**, `CookingAlarmPolicyTest` 16→**17**, `CookingSessionTest` 16→**16**, `CookingTimerServiceTest` 10→**11**, `CookingNotificationsTest` 17→**22**, `AndroidCookingAlarmTest` 13→**14**, **nueva** `CookingFinishPublicationTest` **8**; `NotificationPermissionPolicyTest` 14, `CookingModeStateTest` 10, `CookingExitPolicyTest` 14 y `RoomCookingSessionStoreTest` 7 **sin cambio** ⇒ **+20** exactos. Los conteos del XML coinciden **uno a uno** con los del handoff |
| Alcance | `git status --porcelain` / `git diff --name-only` | solo los 9 ficheros de `app/src/main` y 11 de `app/src/test` previstos + 7 docs durables + `feature_list.json` + `PROGRESS.md` (+ la spec y este registro, sin commit); **sin** diff en manifiesto, permisos, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `res/**`, `ui/theme/**`, `docs/design/**`, `data/cooking/CookingSessionEntity|Dao|Database|Mappers|RoomCookingSessionStore` |
| Pureza | `grep` propio de imports y de reloj | `ui/cooking/CookingTimerState.kt`, `ui/cooking/CookingSession.kt`, `ui/cooking/CookingAlarm.kt` y `data/cooking/CookingFinishPublication.kt` con **0** imports `android.*`/`androidx.*` y **0** lecturas del reloj (`SystemClock`/`currentTimeMillis`/`elapsedRealtime`); el único import ajeno es `domain.recipe.Recipe` |
| Confinamiento Android | `grep` propio | `SystemClock`/`AlarmManager`/`PendingIntent`/`NotificationManager*` **solo** en `ui/cooking/CookingModeScreen.kt` (lectura del reloj, ya aceptada) y `data/cooking/**`; `tick(` = **0** apariciones en `app/src` |
| Desviaciones declaradas | `javap` sobre los artefactos reales | (i) `NotificationManagerCompat` de `androidx.core 1.10.1` **no** expone `getActiveNotifications()` (verificado en el AAR transformado) ⇒ el uso del `NotificationManager` del framework (API 23+; `minSdk 33`) es la única vía y Robolectric lo implementa; (iv) `CookingSessionMappers` **no** persiste el ancla del temporizador (sí `endAtElapsedMillis` de la sesión) ⇒ el ajuste de `RoomCookingSessionStoreTest` es correcto |
| No mutación por el validador | `git status --porcelain` antes/después | las mismas entradas (más este registro) ⇒ el validador no tocó código ni estado |

## Escenarios verificados

1. **Un solo reloj (Scenario 1, verificación exigida «Test JVM»).** `CookingTimerState` gana
   `endAtElapsedMillis: Long?` (`CookingTimerState.kt:45`) con invariante «no nulo solo En marcha con
   `remaining > 0`»; `at(now)` (`:103-113`) proyecta `(endAt - now).coerceAtLeast(0)` y pasa a
   `FINISHED`/`0` **sin ancla** al alcanzar o pasar el fin; `tick(deltaMillis)` **ya no existe**. El
   llamante no puede acumular deriva porque no envía `delta`. Tests: proyección exacta con latidos de
   1,5 s / 3 s / 61 s (`598_500`/`597_000`/`539_000`), deriva 0 (misma proyección con y sin latidos
   intermedios) y nunca negativo. **Rebajado a evidencia de ejecución propia**: `testDebugUnitTest`
   completo en verde (ver tabla).
2. **Pausar/Reanudar/Reiniciar por el instante (Scenario 2).** `pause(now)` congela `at(now).remaining` y
   **anula el ancla**; `resume(now)` reancla en `now + remaining`; `reset()` vuelve a Inactivo con el total
   sin ancla; las transiciones inválidas siguen siendo no-op (`CookingTimerStateTest`,
   `CookingAlarmPolicyTest`); `NotificationPermissionPolicyTest` reescrito sin cambio de contrato.
3. **El servicio publica el aviso en el instante de fin y retira la persistente (Scenario 3, verificación
   exigida «Test JVM/Robolectric»).** `CookingTimerService.scheduleFinish` (`:109-126`) publica
   `CookingNotifications.notifyFinish(...)` con el sello **antes** de `stopForeground(STOP_FOREGROUND_REMOVE)`
   y `stopSelf()`; el `notifyFinish` publica como `when` el sello y **no recibe ningún dato de receta**
   (`CookingNotifications.kt:132-143, 209-224`). `CookingTimerServiceTest` afirma en Robolectric título,
   texto, `when = sello`, `isForegroundStopped`, `notificationShouldRemoved` y `isStoppedBySelf()`, y que
   el aviso **no lleva** claves propias de la app ni rastro de la receta.
4. **La alarma sigue siendo el respaldo y no duplica (Scenario 4).** `CookingAlarmReceiver.onReceive`
   (`:43-61`) para el servicio, retira la `1000` y publica con el sello que le llegó (o degrada a `now` sin
   lanzar); `CookingNotificationsTest.el_receptor_de_respaldo_no_duplica_un_fin_ya_anunciado_por_el_servicio`
   comprueba `assertSame` sobre la `1001` (una sola notificación y **una sola** alerta) y
   `AndroidCookingAlarmTest.el_sello_de_fin_compartido_viaja_en_los_dos_intent` comprueba que el sello se
   calcula **una vez** y viaja **igual** en el `Intent` del servicio y en el `PendingIntent` de la alarma.
   La arista `→ FINISHED` de `CookingAlarmPolicy` sigue devolviendo `None` (la alarma **no** se cancela).
5. **Nunca se pinta en negativo (Scenario 5).** `CookingTimerService.onStartCommand` (`:69-73`) sale **sin
   pintar** si `endAt <= elapsedRealtime()` (test: `lastForegroundNotification == null` y servicio parado);
   el `when` del cronómetro se recalcula al publicar (`wallClockEnd`, `:135-136`); y el receptor de respaldo
   retira la `1000` al disparar (parar servicio + `cancel(1000)`), que es la retirada exacta con el FGS
   congelado/muerto.
6. **Sin aviso del paso anterior (Scenario 6).** `CookingFinishPublication.decide` (`:50-60`) decide con el
   `when` del aviso mostrado y la ventana de frescura de `60_000` ms: mismo sello ⇒ `ALREADY_ANNOUNCED`,
   fuera de ventana ⇒ `STALE` (nada se publica), fin nuevo ⇒ `PUBLISH`; `setOnlyAlertOnce` **no** se usa.
   8 casos puros + 3 de Robolectric (mismo sello no republica, fuera de ventana no publica, fin nuevo sí
   alerta). El límite exacto (`+60_000` publica / `+60_001` descarta) está fijado.
7. **Trabajo del sistema por gesto (Scenario 7, verificación «Prueba manual» + inspección).** Un único
   `onUserAction` (`CookingModeScreen.kt:164-182`) hace **una sola** lectura de
   `SystemClock.elapsedRealtime()` y con ella aplica el estado y las **dos** decisiones puras
   (`CookingAlarmPolicy` —que emite `Schedule(next.endAtElapsedMillis ?: now + remaining)`— y
   `NotificationPermissionPolicy`); el `LaunchedEffect(state.timer.status)` y `previousTimer` **están
   borrados** (`grep previousTimer`: solo la mención de la KDoc) y el único `LaunchedEffect` del reloj está
   keyed por `state.timer.isRunning` y solo hace el latido derivado, alineado al segundo
   (`remaining % 1_000`). Se conservan la persistencia por cambio significativo, el `DisposableEffect` de
   salida y el gate de confirmación. La parte de comportamiento en dispositivo es **prueba manual**.
8. **Reanudación desde la notificación (Scenario 8).** `LaunchedEffect(Unit)` (`:187-197`) rearma el
   trabajo del sistema **una vez** tratando la reanudación como `IDLE → RUNNING` con la misma decisión pura
   (el `copy(status = IDLE)` conserva el ancla ⇒ `Schedule(ancla)`), y sale sin rearmar si la sesión
   reanudada no está En marcha (Terminado). `CookingSession.of`/`resumedMode` reutilizan el ancla
   (`projectedEndMillis`), con la degradación defensiva intacta si el ancla fuera `null`; el `Saver`
   persiste el ancla (`CookingModeStateSaver.save/restore`, `:961-992`).
9. **Degradación silenciosa (Scenario 9).** Sin permiso `notifyFinish` no publica y no lanza (tests
   existentes adaptados), el servicio arranca igual con el permiso denegado y el receptor sin el escalar
   del sello publica con el instante actual sin lanzar; el canal, los ids, el microcopy y el aspecto del
   Modo cocina **no cambian** (`git diff` de `CookingNotifications` no toca `ongoing`, el canal, ni el copy;
   `res/**` y `ui/theme/**` sin diff).

## Validator Checklist

**10/10** verificados: (1) el restante se deriva de `endAtElapsedMillis` (`at`/`projectedEndMillis`), sin
deltas fijos, sin deriva y sin negativos, con `tick` **eliminado** (0 apariciones en `app/src`); (2)
`CookingAlarmPolicy`/`CookingSession` comparten el ancla y el `Saver` la persiste; (3) el servicio publica
la `1001` y retira la `1000` al vencer, no pinta si el fin ya pasó, recalcula el `when`, y el receptor para
el servicio/retira la `1000` al disparar; (4) escenarios 1-9 (1-6 y 9 en JVM/Robolectric; 3, 5, 6 y 7 con la
prueba manual anotada como pendiente de la persona); (5) idempotencia por sello (mismo fin ⇒ una
notificación y una alerta, fin nuevo ⇒ vuelve a alertar, fuera de ventana ⇒ se descarta); (6) nada lanza sin
`POST_NOTIFICATIONS`/con las notificaciones apagadas y el Modo cocina y el "¡Listo!" en pantalla quedan
intactos; (7) verificación exigida presente (gate en verde + **evidencia fresca forzada** + conteos del XML
y del SARIF contra la baseline de `e5012d1`); (8) sin dependencias, permisos, manifiesto, build/lockfile,
`init.sh`, `scripts/**`, `domain/**`, Room ni `ui/theme/**` tocados, y sin cambios en canal/ids/copy/aspecto;
(9) docs durables actualizados con notas de enmienda y sin reescribir evidencia histórica; (10) sin
comportamiento no pedido (`setOnlyAlertOnce` descartado, sin canal nuevo, sin servicio vivo tras el fin, sin
tocar la hija `cooking-timer-notification-step-label`, sin `docs/design/concepts/`).

## Hallazgos por severidad

**Critical / High / Medium: ninguno.** La implementación cumple los nueve escenarios y las cuatro
verificaciones exigidas; los conteos del handoff son **reales** (reproducidos de forma independiente) y las
cuatro desviaciones declaradas son legítimas y acotadas.

- **Low L1 (informativo, no bloqueante).** El receptor de respaldo **para el servicio y retira la `1000`
  antes** de evaluar la decisión de publicación (`CookingAlarmReceiver.kt:47-50`), es decir, también cuando
  el sello resulta `STALE`. Es exactamente lo que fija la spec (item 6: «el receptor, al disparar, para el
  servicio y retira la `1000`»), así que **no** es un defecto de alcance; el único caso adverso es la
  carrera ya reconocida (un broadcast de la alarma del paso anterior despachado justo cuando corre el paso
  siguiente), en el que la cuenta atrás persistente del paso nuevo desaparecería y el servicio se pararía,
  aunque su "¡Listo!" llegaría igualmente por su propia alarma de respaldo. Endurecimiento opcional
  (feature correctiva o nota en el ADR): condicionar `stopService`/`cancel(1000)` a
  `decision != STALE`.
- **Low L2 (informativo).** El sello se compara contra `System.currentTimeMillis()` con una ventana de
  frescura de 60 s: un **salto hacia delante del reloj de pared > 60 s** durante la cuenta atrás haría que
  el sello pareciera caducado y **los dos** publicadores descartarían el aviso (el "¡Listo!" en pantalla no
  se ve afectado). Es el precio explícito de la ventana de frescura (decisión de la spec, documentada en
  ADR 0008 y en `docs/risks-and-open-questions.md`) y solo se mitiga con una referencia monótona; se deja
  como riesgo residual, no como bloqueo.
- **Low L3 (informativo).** El test nuevo `AndroidCookingAlarmTest.el_sello_de_fin_compartido_viaja_en_los_dos_intent`
  usa el campo **deprecado** `ShadowAlarmManager.ScheduledAlarm.operation` (`:176`), lo que añade **un
  warning de compilación de Kotlin** (`'field operation: PendingIntent?' is deprecated`) a la tirada de
  tests. El build sigue en verde y no hay API no deprecada equivalente en Robolectric 4.17 para ese
  `PendingIntent`; bastaría `@Suppress("DEPRECATION")` en el test para dejar la consola limpia.
- **Low L4 (informativo).** `CookingTimerState.at(now)` devuelve `this` si el estado está En marcha **sin
  ancla** (fila corrupta), de modo que el latido de la pantalla no descuenta y el número quedaría
  congelado (antes, con `tick`, drenaba hasta Terminado). El camino solo es alcanzable con un estado
  corrupto —el `Saver` y `CookingSession.endAtElapsedMillis` (Room) persistieron el ancla/instante— y la
  degradación sigue siendo «no lanza». No bloquea; si se quiere, el bucle puede tratar «En marcha sin
  ancla» como «no corre».
- **Low L5 (doc, informativo).** En `docs/risks-and-open-questions.md`, la fila del heads-up en primer
  plano conserva la frase en presente «la alarma sigue siendo el **único** productor del aviso» y solo dos
  frases después la nota de la correctiva dice lo contrario (el productor primario es el servicio). No
  afecta al comportamiento (la corrección está en la misma celda y en los ADR 0007/0008), pero conviene
  pasarla a pasado para que la fila no se contradiga a sí misma. Del mismo modo, la decisión 7 del ADR 0008
  mantiene «lo **único** que cruza al sistema junto al instante de fin del servicio», cuando ahora también
  cruza el sello (ya cubierto por la nota de la decisión 9).

## Seguridad (checklist portable, alcance = diff)

Sin hallazgos, ni bloqueantes ni notas relevantes. Sin secretos ni configuración nueva; **sin permisos
nuevos** (`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`,
`VIBRATE` y el receptor **intactos**, manifiesto sin diff). Privacidad: lo que cruza al sistema sigue siendo
**genérico** —el `Intent` del servicio y el `PendingIntent` de la alarma solo llevan **dos escalares**
(`EXTRA_END_AT_ELAPSED_MILLIS` y el sello `EXTRA_FINISH_WALL_CLOCK_MILLIS`, un instante de hora de pared que
no identifica receta ni paso) y el `Intent` de contenido sigue llevando **solo** el booleano
`EXTRA_RESUME_COOKING`—; los tests de firma estructural siguen exigiendo que `notification`/`notifyFinish`
solo acepten `Context`, `PendingIntent` y el escalar. La lectura del aviso mostrado usa la API del framework
(`getActiveNotifications`, API 23+) dentro de `runCatching`. Sin red, I/O, SQL, input de usuario ni intents
implícitos; superficie nueva mínima (una decisión pura + un extra escalar + el `when` de la `1001`); **0**
dependencias nuevas. La idempotencia no debilita ninguna garantía previa: el id fijo sigue evitando
notificaciones duplicadas y el sello evita el segundo sonido/vibración, con el fin legítimo siguiente
**sí** alertando.

## Docs durables y harness

Coherentes con el comportamiento implementado y sin reescribir evidencia histórica: notas de **enmienda** en
ADR 0008 (decisiones 1, 2, 6, 9, 12 y alternativa 1 + consecuencias del aviso y del cronómetro negativo) y
en ADR 0007 (decisiones 3, 5, 7, 8, 9 y consecuencia del disparo único/efecto por gesto); reescritura de los
párrafos del reloj, del aviso y de la cuenta atrás en `docs/technical-discovery.md`; el reloj único en
`docs/domain-model.md`; las filas del aviso, del cronómetro y del heads-up más una **fila nueva** del
aplazamiento/coalescencia de la alarma (ColorOS, ventana de frescura y retraso del primer arranque) en
`docs/risks-and-open-questions.md`; y notas de **vigencia** en las dos specs anteriores
(`cooking-timer-foreground-service`, `cooking-timer-finish-notification`). `AGENTS.md`, `DESIGN.md`,
`docs/build-brief.md` y `docs/design/**` **sin tocar** (no aplica: no hay pantallas ni tokens nuevos y la
feature no cambia el flujo de agentes). `init.sh` intacto y en verde (no imprime comandos: ejecuta el gate y
falla si falla). Único desajuste documental: **Low L5**.

## Pendiente no bloqueante (persona, dispositivo ColorOS)

No automatizable desde WSL (`AGENTS.md`): la **primera** cuenta atrás tras abrir la app arranca **sin** el
retraso de 10-15 s; pantalla y notificación **sincronizadas** durante todo el contador; la notificación
**no** se pinta en negativo al llegar a cero; el **"¡Listo!"** llega **en el instante** con la app
minimizada y el móvil bloqueado y **una sola vez** (un solo sonido/vibración); durante el paso siguiente
**no** llega el aviso del anterior; Pausar/Reiniciar/cambiar de paso/salir siguen parando el servicio; sin
permiso, nada se rompe. El acabado (aspecto del cronómetro en la pantalla de bloqueo, orden de las dos
notificaciones, tono/patrón) lo revisa la persona; un defecto se corrige con una **feature correctiva**.

## Acción requerida del orquestador

Persistir el veredicto como estado **`accepted`** en `feature_list.json` (y anotarlo en `PROGRESS.md`), crear
el commit local en `feature/cooking-timer-notification-reliability` y publicar el PR vía `github-flow`
(título `[cooking-timer-notification-reliability] …`, con `Closes #<n>` de su issue). El validador **no**
modifica el estado ni commitea.
