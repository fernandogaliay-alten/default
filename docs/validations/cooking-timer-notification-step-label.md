# Validación independiente — `cooking-timer-notification-step-label`

- **Feature**: `cooking-timer-notification-step-label` (`area: integration`; depende de
  `cooking-timer-foreground-service` y `cooking-timer-notification-reliability`, ambas `accepted`)
- **Spec**: `docs/specs/cooking-timer-notification-step-label.md`
- **Rama / worktree**: `feature/cooking-timer-notification-step-label`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-timer-notification-step-label`
- **Validación**: subagente `validator` con contexto limpio (independiente del `implementer`, que cerró la
  sesión 143)
- **Estado previo**: `passing` (implementer, sesión 143, trabajo **en disco**: `HEAD = 3f3f0fe`, merge de la
  PR #163; **sin commit, sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar #1 | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); `testDebugUnitTest` salió **`UP-TO-DATE`** ⇒ por sí solo **no** vale como evidencia fresca |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL 3m52s**, `34/34` tareas **ejecutadas**, `exit 0`; parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **117 suites / 951 tests / 0 fallos / 0 errores / 0 saltados** (mtimes `09:56:36`, hashes de XML **distintos** a los del árbol previo ⇒ tirada **mía**) |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL 3m22s**, `30/30` tareas ejecutadas, `exit 0`; SARIF fresco (`10:00`): **13 resultados**, **0 errores**, **0 bajo `app/src`**, reparto `GradleDependency` 7 / `NewerVersionAvailable` 3 / `AndroidGradlePluginVersion` 2 / `OldTargetApi` 1 (**sin diff** en `app/build.gradle.kts`, `gradle/libs.versions.toml` ni el wrapper ⇒ preexistentes) |
| Tirada focal | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*" --tests "com.sazon.app.data.cooking.*"` | **BUILD SUCCESSFUL 39s**, `exit 0`; XML: **16 suites / 204 tests / 0 fallos** |
| Gate estándar #2 (árbol final) | `bash init.sh` | **exit 0**; `testDebugUnitTest` **ejecutado** (sin filtro ⇒ suite completa, `1m26s`) y `lint` `UP-TO-DATE` (fresco de mi tirada forzada); XML final **117 / 951 / 0 / 0 / 0** (`10:05`) |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` + conteo del árbol | `HEAD 3f3f0fe`: **116 ficheros / 931 anotaciones**; árbol de trabajo (tracked): **943** (+12 en las 4 suites modificadas) y **+8** en la suite **nueva** sin trackear `CookingStepLabelTest.kt` ⇒ **+1 suite / +20 tests** exactos |
| Reparto del delta | conteo propio por clase (XML vs anotaciones de `HEAD`) | `AndroidCookingAlarmTest` 14→**16** (+2), `CookingAlarmPolicyTest` 17→**20** (+3), `CookingNotificationsTest` 22→**26** (+4), `CookingTimerServiceTest` 11→**14** (+3) y **nueva** `CookingStepLabelTest` **8**; ninguna **otra** suite del repo cambia de recuento ⇒ **+20** |
| Alcance | `git status --porcelain` / `git diff --stat` | solo los ficheros previstos: 6 de `app/src/main` (1 nuevo), 4 + 1 de `app/src/test`, 5 docs + `DESIGN.md` + `PROGRESS.md` + `feature_list.json` (+ la spec y este registro, sin trackear). **Sin** diff en manifiesto/permisos, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `res/**`, `init.sh`, `scripts/**`, `domain/**`, `ui/theme/**`, Room ni pantallas del Modo cocina; **sin** capturas nuevas |
| Funciones que debían quedar intactas | `diff` función a función contra `git show HEAD:` | **sin diff**: `CookingNotifications.notification` / `notifyFinish` / `contentIntent` / `ensureChannel`, `CookingTimerService.scheduleFinish` / `wallClockEnd` / `onDestroy` / `onBind`, `AndroidCookingAlarm.broadcastPendingIntent` / `cancel` |
| Pureza de la lógica nueva | `grep` propio de imports y de reloj | `ui/cooking/CookingStepLabel.kt`: **único** import `com.sazon.app.domain.recipe.RecipeStep`; **0** imports `android.*`/`androidx.*`; **0** lecturas del reloj |
| Puntos de armado | `grep` de `onTimerChange` / `CookingTimerService.intent` | **un** call site de producción de `CookingTimerService.intent` (`AndroidCookingAlarm.schedule`) y **dos** armados en `CookingModeScreen` (`:203-208` en `onUserAction` y `:230-237` en el `LaunchedEffect(Unit)` del rearme), ambos con la etiqueta |
| Manifiesto / PendingIntents | `grep` en `AndroidManifest.xml` y `data/cooking/*.kt` | `CookingTimerService` sigue `exported="false"`; los `PendingIntent` siguen con `FLAG_IMMUTABLE` (+ `FLAG_UPDATE_CURRENT` / `FLAG_NO_CREATE`) — **sin diff** |
| No mutación por el validador | `git status --porcelain` antes/después | las mismas entradas más **este registro** ⇒ no se tocó código, `PROGRESS.md` ni `feature_list.json` |

## Escenarios verificados

1. **La persistente muestra el número y el título (Scenario 1, verificación exigida «Test
   JVM/Robolectric»).** `CookingNotifications.ongoing(context, endWallClockMillis, stepProgress, stepTitle,
   contentIntent)` (`CookingNotifications.kt:175-199`) pinta `setContentTitle(progress)` y
   `setContentText(stepTitle)`; `CookingTimerService.onStartCommand` (`:83-84`) los lee con
   `getStringExtra(...)`. Reproducido por mi tirada: `CookingTimerServiceTest` prueba que la **notificación
   publicada** (`lastForegroundNotification`) lleva `EXTRA_TITLE = "Paso 2 de 5"` y
   `EXTRA_TEXT = "Dorar el pollo"` con el cronómetro (`EXTRA_SHOW_CHRONOMETER` +
   `EXTRA_CHRONOMETER_COUNT_DOWN`), el `when` y los flags `ongoing`/`onlyAlertOnce` **intactos**, sin claves
   `com.sazon` ni rastro de la descripción del paso; y que el `Intent` de arranque lleva esos mismos textos.
   El id `1000` y las líneas de `setSilent(true)`/`setOngoing(true)` no tienen diff.
2. **El número reutiliza la microcopy y solo cruzan número y título (Scenario 2, PRIVACY GUARD).**
   `CookingStepLabel.of` (`CookingStepLabel.kt:32-38`) devuelve
   `CookingStepLabel(CookingModeFormat.progressLabel(stepIndex, totalSteps), step.title)` y `null` si
   `step == null` o `totalSteps <= 0`; el test puro compara el número con `CookingModeFormat.progressLabel`
   (sin microcopy paralela), comprueba que el `toString()` no arrastra la descripción ni los minutos y que el
   tipo tiene **solo** los campos `progress`/`title` (reflexión). En el `Intent` y en la notificación, mi
   tirada confirma el `keySet` **exacto** y la ausencia de cualquier otro dato de receta; la firma de
   `ongoing` admite solo `Context`, `PendingIntent`, escalares y **exactamente dos** `String`, mientras
   `notification`/`notifyFinish` siguen **sin** aceptar texto (test estructural, re-ejecutado).
3. **La etiqueta nunca queda obsoleta (Scenario 3).** La política aceptada no cambia: un cambio de paso
   reinicia el temporizador ⇒ regla 4 ⇒ `Cancel` (servicio parado y notificación retirada) y el armado
   siguiente compone la etiqueta del paso **nuevo**. `CookingTimerServiceTest` reproduce la secuencia con
   dos `onStartCommand` consecutivos: la persistente pasa de `"Paso 1 de 5"` a `"Paso 2 de 5"` +
   `"Dorar el pollo"` (nunca arrastra la anterior). `CookingAlarmPolicy.onTimerChange` (`:85-99`) solo copia
   `stepLabel` en la rama de `Schedule`.
4. **La reanudación rearma con la etiqueta del paso persistido (Scenario 4).** El rearme usa
   `stepLabelOf(state)` con el estado **reanudado** (`CookingModeScreen.kt:235`, `state` inicializado en
   `:159-163` desde `resume.resumedMode(...)`, que conserva `stepIndex`) y el mismo camino de política que el
   gesto; `CookingAlarmPolicyTest` prueba que la etiqueta del paso **3** viaja en el `Schedule` del rearme
   `IDLE -> RUNNING` con el instante programado **igual** con y sin etiqueta y que `Cancel`/`None` no la
   llevan. La pantalla no es automatizable en WSL (`AGENTS.md`): queda verificado por inspección + política
   pura.
5. **Sin etiqueta se degrada al título genérico y nada lanza (Scenario 5).** Con `stepProgress = null` el
   título es `ONGOING_TITLE` y **no** hay cuerpo (`CookingNotificationsTest`, re-ejecutado); con etiqueta
   presente el `Intent` lleva **solo** fin + sello; con el instante de fin ya pasado el servicio sigue **sin**
   pintar y apagándose; con `intent == null` o sin el extra de fin sigue `stopSelf()` +
   `START_NOT_STICKY` (sin diff).
6. **La deuda aceptada no cambia (Scenario 6).** Sin diff en el aviso `1001`, su microcopy, el canal, los
   ids, `CookingFinishPublication`, `CookingAlarmReceiver`, el `PendingIntent` de la alarma (mi tirada
   confirma `keySet == {EXTRA_FINISH_WALL_CLOCK_MILLIS}` exacto, sin `EXTRA_STEP_*`), el `onDispose` que no
   limpia notificaciones ni el caso sin permiso (los 26 casos de `CookingNotificationsTest` y los 14 de
   `CookingTimerServiceTest` pasan sin cambio de contrato).
7. **Prueba manual en el emulador (Scenario 7).** No automatizable desde WSL (`AGENTS.md`): queda anotada
   como **pendiente de la persona** en `PROGRESS.md` (sesión 143), en la evidencia de `feature_list.json` y
   en este registro. No bloquea (el acabado visual lo revisa la persona tras el merge; un defecto se corrige
   con una **feature correctiva**).

## Validator Checklist (spec)

| # | Item | Resultado |
|---|---|---|
| 1 | La persistente muestra "Paso N de M" (misma cadena que `progressLabel`) + título; cronómetro, `when`, `ongoing`, `silent` y id `1000` sin cambios | **OK** (escenario 1, re-ejecutado) |
| 2 | Etiqueta en lógica **pura** y en los **dos** armados de `CookingModeScreen` | **OK** (`CookingStepLabel.kt` sin Android ni reloj; `:207` y `:235`) |
| 3 | El `Intent` del servicio lleva **solo** {fin, sello, progreso, título}; ninguna otra exposición; firma estructural cubre `ongoing` y `notification`/`notifyFinish` siguen sin texto | **OK** (keySets exactos + test de firmas, re-ejecutados) |
| 4 | `PendingIntent` de la alarma, aviso `1001`, `CookingFinishPublication`, canal, ids y microcopy sin cambios | **OK** (diff función a función: sin diff) |
| 5 | Sin etiqueta (o texto en blanco) degrada al título genérico sin lanzar | **OK** (escenario 5, re-ejecutado) |
| 6 | Escenarios 1-7 (1-6 en JVM/Robolectric; 7 anotado como pendiente) | **OK** (7 = pendiente de la persona, declarado) |
| 7 | Verificación exigida presente: `init.sh` en verde + evidencia fresca, conteos contra la baseline de `3f3f0fe`, lint 0 errores / 0 bajo `app/src` | **OK** (tabla de arriba) |
| 8 | Sin dependencias, permisos, manifiesto, build/lockfile, `init.sh`, `scripts/**`, `domain/**`, Room, `ui/theme/**`, `res/**` ni pantallas tocadas; sin capturas | **OK** (`git status`/`diff --stat`) |
| 9 | Docs durables actualizados sin reescribir evidencia histórica | **OK** con el nit **Low L2** |
| 10 | Sin comportamiento no pedido (ni persistencia de la etiqueta, ni canal nuevo, ni microcopy del aviso, ni arreglo de la `1001` obsoleta o la rotación) | **OK** |
| 11 | `feature_list.json`/`PROGRESS.md` los actualiza el **orquestador** | **OK en la interpretación del repo**: el implementer registró `passing` + evidencia y el log de su sesión (patrón de todas las features previas); la persistencia de `accepted` queda para el orquestador |

## Hallazgos por severidad

**Critical / High / Medium: ninguno.** Los siete escenarios quedan cubiertos (1-6 reproducidos por mí en
JVM/Robolectric; 7 pendiente de la persona, declarado), los conteos del handoff son **reales**
(reproducidos de forma independiente) y las **tres** desviaciones declaradas son legítimas y acotadas.

- **Low L1 (informativo, no bloqueante).** `ongoing(...)` trata una etiqueta **a medias** (número en blanco
  **o** título en blanco) como etiqueta **ausente**: sin número no pinta tampoco el cuerpo
  (`CookingNotifications.kt:182-185`). Es la lectura literal del escenario 5 («una etiqueta en blanco se
  trata como ausente») y deja la notificación **igual** que antes de la feature, pero difiere del literal
  del *Technical Approach* paso 6 (que pondría `ONGOING_TITLE` como título **conservando** el cuerpo del
  paso). No afecta a ningún escenario exigido, está testeado (caso de número en blanco y de título en
  blanco) y documentado en el KDoc y en la nota del implementer: **se acepta tal cual**; si el `planner`
  quiere cerrar la ambigüedad, basta una línea en la spec (no requiere reimplementación).
- **Low L2 (doc, informativo).** La nota **"Precisión"** de `DESIGN.md:300` convive en el **mismo párrafo**
  con la frase histórica «título fijo "Cocina en marcha"» de `cooking-timer-foreground-service` (la
  precisión la matiza dos frases después, así que el párrafo no se contradice de forma peligrosa), y
  `docs/specs/cooking-timer-foreground-service.md:871` (checklist histórico "título fijo + instante de fin")
  queda a ~650 líneas de la nota de vigencia de `:221-229`. Es el mismo `Low L5` que la validación de
  `cooking-timer-notification-reliability` dejó apuntado en esa clase de líneas históricas: evidencia
  histórica conservada a propósito, sin efecto en el comportamiento. Opcional: extender la nota de vigencia
  a la checklist de `:871` / pasar a pasado la frase de `DESIGN.md` en una correctiva de docs.
- **Low L3 (informativo, no bloqueante).** La desviación (3) declarada se confirma: el test nuevo
  `AndroidCookingAlarmTest.la_etiqueta_del_paso_viaja_solo_al_servicio_y_nunca_a_la_alarma` (`:194`) usa el
  campo **deprecado** `ShadowAlarmManager.ScheduledAlarm.operation`, así que la compilación de tests emite
  **dos** warnings del mismo tipo (el preexistente de `:229` —mismo patrón ya aceptado en la feature
  anterior— y el nuevo de `:194`). `BUILD SUCCESSFUL` en las tres tiradas y **0** impacto en lint/SARIF; si se
  quiere consola limpia, `@Suppress("DEPRECATION")` en el test basta (no hay accesor no deprecado
  equivalente en la versión de Robolectric del repo).
- **Low L4 (informativo, no bloqueante).** Desviación (2) declarada y **coherente con la spec**:
  `CookingAlarmCommand.Schedule` y `CookingAlarmPolicy.onTimerChange` usan `stepLabel: CookingStepLabel? =
  null` (preservan los call sites y las aserciones aceptadas), mientras `CookingTimerService.intent(...)`
  **no** lleva default, de modo que sus dos call sites (producción y tests) deciden de forma explícita. No es
  una desviación real, es la traducción literal del *Technical Approach* pasos 2 y 5.
- **Low L5 (harness, informativo).** El `Intent` de la etiqueta se construye con `putExtra` solo si
  `stepLabel != null` (`CookingTimerService.kt:174-183`), que es la lectura correcta de la incidencia de
  plataforma documentada en la spec (`putExtra(name, null)` dejaría la clave presente). Sin acción.

## Seguridad (checklist portable, alcance = diff)

Sin hallazgos, ni bloqueantes ni notas relevantes más allá del trade-off **aceptado y documentado**:

- **Secretos/configuración**: nada. Sin `.env`, claves, tokens ni credenciales en el diff; sin dependencias
  nuevas (`gradle/libs.versions.toml` y `app/build.gradle.kts` sin diff).
- **Autenticación/autorización**: no aplica (app local de un solo usuario). Sin permisos nuevos: el
  manifiesto no tiene diff y `POST_NOTIFICATIONS` sigue pidiéndose **en contexto** sin gatear nada; sin
  permiso no se publica y nada lanza (ADR 0005, casos vigentes re-ejecutados).
- **Input y superficie**: la etiqueta **no** viene de red ni de input de usuario; se **deriva** de la
  `Recipe` ya en memoria. La superficie nueva es una `data class` pura y dos extras de tipo `String` en un
  `Intent` **explícito** hacia un servicio **`exported="false"`** ⇒ **ninguna** otra app puede arrancarlo ni
  leer el `Intent`; los `PendingIntent` siguen `FLAG_IMMUTABLE`. Sin `Intent` implícitos, sin SQL, sin I/O,
  sin logs con datos del paso.
- **Exposición de datos / privacidad (frontera explícita)**: lo único derivado de la receta que cruza son
  el número ("Paso N de M") y el `RecipeStep.title`; lo prueban (i) la **garantía estructural** —ni
  `ongoing` ni `notification`/`notifyFinish` reciben la `Recipe` (test de firmas re-ejecutado)—, (ii) los
  `keySet` **exactos** del `Intent` del servicio y del `PendingIntent` de la alarma, y (iii) las
  aserciones de "sin rastro" de descripción/ingredientes y de "sin claves `com.sazon`" en la notificación.
  El aviso de fin `1001`, su sello, la idempotencia y el `Intent` de contenido (solo
  `EXTRA_RESUME_COOKING`) quedan **sin diff**. El precio —ese texto se lee en la pantalla de bloqueo por el
  canal `VISIBILITY_PUBLIC`— es una **decisión explícita de la persona**, registrada en ADR 0007 (nota
  "Acotada"), ADR 0008 (decisiones 2, 9 y 12), `docs/technical-discovery.md` y una **fila nueva** de
  `docs/risks-and-open-questions.md` con la mitigación descartada (canal nuevo ⇒ correctiva). Sin deriva
  del contrato "no lanza": la degradación sin etiqueta está testeada.
- **Servicios externos / SSRF**: no aplica (sin llamadas de red en el diff).

## Docs durables y harness

Coherentes con el comportamiento implementado y sin reescribir evidencia histórica: nota **"Acotada"** en la
decisión 7 de **ADR 0007**; notas en las decisiones 2, 9 y 12 de **ADR 0008** (la persistente deja de tener
título fijo, `ONGOING_TITLE` como fallback, el `Intent` del servicio con la etiqueta y el `PendingIntent` de
la alarma sin ella); párrafo de la cuenta atrás en **`docs/technical-discovery.md`** (título/cuerpo nuevos y
dato de entrada ampliado con el límite explícito de lo que cruza); nota de **vigencia** en el escenario 9 de
**`docs/specs/cooking-timer-foreground-service.md`**; **precisión** en la excepción de capturas de
**`DESIGN.md`**; y **fila nueva** del trade-off de privacidad en **`docs/risks-and-open-questions.md`**.
`docs/domain-model.md`, `AGENTS.md` y `docs/build-brief.md` **sin tocar** (correcto: sin entidad ni estado
nuevos, sin regla operativa nueva y sin cambio de alcance del MVP). Sin `docs/design/concepts/…` nuevo
(correcto: no hay pantalla ni token nuevos; el texto lo pinta el sistema). `init.sh` **intacto** y ejecutando
el gate real (test + lint, falla si fallan), sin procesos largos. `PROGRESS.md` (sesión 143) y
`feature_list.json` (`passing` + evidencia) reflejan el estado real y el siguiente paso (esta validación);
no hay bloqueos ocultos. Único desajuste documental: **Low L2**.

## Pendiente no bloqueante (persona, emulador)

No automatizable desde WSL (`AGENTS.md`; escenario 7): con el temporizador de un paso **En marcha**, la
notificación persistente dice **"Paso N de M"** y el **título del paso** con la cuenta atrás decreciendo; al
cambiar de paso y arrancar el nuevo temporizador, el texto es el del **paso nuevo**; con el móvil bloqueado el
texto se lee (canal público); sin permiso o con un título largo, nada se rompe. El acabado (dos líneas de
texto + cronómetro, truncado según el fabricante) lo revisa la persona; un defecto se corrige con una
**feature correctiva**.

## Acción requerida del orquestador

Persistir el veredicto como estado **`accepted`** en `feature_list.json` (y anotarlo en `PROGRESS.md`), crear
el commit local en `feature/cooking-timer-notification-step-label` y publicar el PR vía `github-flow` (título
`[cooking-timer-notification-step-label] …`, con `Closes #<n>` de su issue). El validador **no** modifica el
estado, no commitea y no hace push: el único artefacto que añade es este registro.
