# Validación independiente — `cooking-timer-alarm-permission-context`

- **Feature**: `cooking-timer-alarm-permission-context` (`area: ui`, correctiva de `cooking-timer-alarm`)
- **Spec**: `docs/specs/cooking-timer-alarm-permission-context.md`
- **Rama / worktree**: `feature/cooking-timer-alarm-permission-context`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-timer-alarm-permission-context`
- **Sesión**: 136 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, trabajo en disco; `HEAD = b80421a`, **sin commit, sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); primera pasada con **29/30 tareas `up-to-date`** (test cacheado) ⇒ forzada |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, **34/34 tareas ejecutadas** (`BUILD SUCCESSFUL in 3m 54s`); parseo propio de los XML frescos (`app/build/test-results/testDebugUnitTest/TEST-*.xml`, mtime 21:55): **109 suites / 830 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | exit 0, **30/30 tareas ejecutadas** (`BUILD SUCCESSFUL in 3m 13s`); SARIF fresco (mtime 21:59): **13 resultados, todos `warning`, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` | `HEAD b80421a`: **107 ficheros / 809 anotaciones** ⇒ delta exacto **+2 suites / +21 tests** (`NotificationPermissionPolicyTest` 14, `AndroidNotificationPermissionPromptTest` 7) = 830; **0 suites modificadas** |
| Suites nuevas | XML por clase | `NotificationPermissionPolicyTest` 14/0/0/0 (**la verificación exigida**) y `AndroidNotificationPermissionPromptTest` 7/0/0/0 |
| Suites `cooking` preexistentes | XML por clase | `CookingAlarmPolicyTest` 15, `CookingTimerStateTest` 11, `CookingModeStateTest` 10, `CookingModeFormatTest` 8, `CookingNotificationsTest` 11, `AndroidCookingAlarmTest` 7 — **sin cambios** |
| Pureza | `grep -n '^import' ui/cooking/NotificationPermission.kt ui/cooking/NotificationPermissionPrompt.kt` | **0** imports `android.*`/`androidx.*`; **0** lecturas del reloj (`SystemClock|currentTimeMillis|nanoTime|Instant|Clock` = NONE); `onTimerChange` sigue el orden de reglas de la spec y `asked()` devuelve copia |
| Punto de petición | `grep -rn` del lanzador y del permiso | el **único** punto de petición es `data.cooking.AndroidNotificationPermissionPrompt.state()/request()`, invocado **solo** desde `CookingModeScreen` (`onConfirm`); `MainActivity` conserva `registerForActivityResult` (registrado antes de `onCreate`) + `CookingNotifications.ensureChannel` y construye el adaptador (**Scenario 1**) |
| Rationale | lectura del diff de `CookingModeScreen.kt` | se muestra solo cuando la decisión pura es `AskWithRationale`, en el **mismo** efecto y **después** de `alarm.apply(...)` (la alarma ya está programada ⇒ **Scenario 6**); `markAsked(now)` al **mostrar**; `AlertDialog` Material 3 con `TITLE`/`MESSAGE`/`CONFIRM`("Continuar", `primary`)/`DISMISS`("Ahora no"), una sola acción principal, sin tokens nuevos |
| Denegación | lectura del diff + adaptador | el callback del lanzador es no-op y `request()` va en `runCatching`; nada toca el temporizador, la cuenta atrás ni el "¡Listo!" (**Scenario 5**); el rationale **no** gatea el reloj |
| Reconciliación | `git diff` + `grep -rn "una vez por proceso" docs/ app/src/main` | vigencia + cuatro frases de estado en `docs/specs/cooking-timer-alarm.md`, enmienda de la decisión 11 y alternativa 9 marcada *superada* en ADR 0007, `docs/technical-discovery.md`, la fila de `docs/risks-and-open-questions.md` (+ "no gatea el temporizador"), comentario del manifiesto (**solo comentario**), paréntesis de `PROGRESS.md` (sesión 131) y nota de capturas en `DESIGN.md`; `grep` deja solo ocurrencias **históricas/marcadas** (**Scenario 9**) |
| Alcance | `git status --porcelain -uall` | 12 modificados + 6 nuevos, exactamente los previstos; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, los adaptadores aceptados de `data/cooking` (salvo el nuevo), `CookingAlarm.kt`/`CookingTimerState.kt`/`CookingModeState.kt`/`CookingModeFormat.kt` y los permisos/receptor del manifiesto **sin diff**; **sin dependencias nuevas**; **no** existe `docs/design/concepts/cooking-timer-alarm-permission-context/` |
| Integridad de evidencia aceptada | comparación programática de JSON `HEAD:feature_list.json` vs árbol | la entrada `cooking-timer-alarm` es **byte-idéntica**; la única entrada cambiada es esta (`not_started` → `passing`) |
| No publicación | `git rev-parse HEAD` / `git log` | `HEAD == origin/main == b80421a`, **0 commits** por delante, árbol con cambios sin commitear |

## Escenarios verificados

1. **No al arrancar (Scenario 1).** `MainActivity` ya no llama a
   `requestNotificationsPermissionOncePerProcess()` ni conserva el método ni el flag `@Volatile`; **0**
   apariciones de ambos símbolos. El lanzador sigue registrado antes de `onCreate` y `ensureChannel` crea
   el canal (que no pide nada).
2. **Rationale → diálogo del sistema (Scenario 2).** Al primer `Iniciar` el temporizador ya corre y la
   alarma ya está programada cuando se decide `AskWithRationale`; el rationale (`AlertDialog`) precede al
   diálogo del sistema; "Continuar" lanza el lanzador.
3. **Decisión pura (Scenario 3, verificación exigida).** `NotificationPermissionPolicyTest` 14 tests en
   verde: `IDLE==previous==next` → `None`; `IDLE→RUNNING` sin permiso y sin intento → `AskWithRationale`;
   concedido → `None`; `PAUSED→RUNNING` con intento previo → `None`; segundo arranque → `None`; cambio de
   paso → `None`; ticks/no-ops/599 ticks → `None`; determinismo; el instante entra por parámetro y
   `asked()` no muta el receptor.
4. **Una sola vez por sesión (Scenario 4).** Dentro del `When` enumerado (mismo proceso, sin recreación de
   la `Activity`) `markAsked` deja las siguientes llamadas en `None`. **Low-1**: la memoria de sesión del
   adaptador vive en la instancia de `Activity` (creada en `onCreate`), no a nivel de proceso, así que una
   recreación podría volver a mostrar el rationale; es exactamente el diseño que fija la spec y el `When`
   del escenario no enumera recreación ⇒ no es fallo de contrato.
5. **Denegación intacta (Scenario 5).** El temporizador, la cuenta atrás y el "¡Listo!" no dependen de la
   decisión ni del permiso; el `LaunchedEffect`/`DisposableEffect`/UI del timer no cambian funcionalmente;
   `request()` no lanza.
6. **Alarma intacta (Scenario 6).** `alarm.apply(...)` se evalúa antes que el rationale; los archivos
   aceptados de la alarma y el canal no tienen diff.
7. **Adaptador (Scenario 7).** Robolectric `@Config(sdk = [33])`, 7 tests: `granted` refleja el permiso en
   vivo, `markAsked` fija el instante, `request()` invoca el lanzador una vez con `POST_NOTIFICATIONS` y no
   lanza aunque el lanzador falle.
8. **Test JVM de la decisión pura (Scenario 8).** Ver punto 3.
9. **Reconciliación del texto (Scenario 9).** No queda ninguna afirmación **vigente** de "una vez por
   proceso al arrancar": solo registros históricos marcados; la evidencia de `cooking-timer-alarm` en
   `feature_list.json` no se reescribe.

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (informativos, no bloqueantes):**

1. **Memoria de sesión por instancia de `Activity`, no por proceso.** `AndroidNotificationPermissionPrompt`
   (`private var askedAtMillis`) se crea en `MainActivity.onCreate` y el manifiesto no declara
   `configChanges`/`screenOrientation`; una recreación de la `Activity` en el mismo proceso (rotación,
   cambio de tema) reinicia la memoria y podría volver a mostrar el rationale en un `PAUSED→RUNNING`
   posterior. Es el diseño que prescribe la spec ("una instancia por proceso, creada en `onCreate`) y el
   `When` del escenario 4 no enumera recreación ⇒ no es fallo de contrato. *Follow-up opcional:* elevar la
   memoria a un contenedor a nivel de proceso (feature correctiva o la futura integración con
   `shouldShowRequestPermissionRationale`).
2. **`docs/validations/cooking-timer-alarm.md:33`** conserva en presente "La petición es **no bloqueante y
   una vez por proceso** (`MainActivity.kt:60-66,268-283`)". Es un **registro fechado** de la validación de
   la feature aceptada —la misma clase de artefacto que la spec preserva (como la evidencia de
   `feature_list.json`)— y la lista de reconciliación de la spec lo excluye deliberadamente ⇒ el escenario
   9 se mantiene. Retoque de texto opcional.

## Non-goals respetados

No entra el **foreground service** ni volver al paso (`cooking-timer-foreground-service`); **sin**
dependencias nuevas; sin cambios en `app/build.gradle.kts`/`gradle/libs.versions.toml`; sin tocar
`domain/**`, `scripts/**`, `init.sh`, la Taxonomía, `assets/**`, `ui/theme/**`, los mipmaps ni los archivos
aceptados de la alarma/notificaciones; **no** se crea
`docs/design/concepts/cooking-timer-alarm-permission-context/`.

## Evaluación de seguridad

Alcance = diff. **Sin hallazgos.** No se añaden permisos (el manifiesto cambia **solo** un comentario), ni
dependencias, ni secretos, ni exposición de datos. `request()` nunca lanza (`runCatching`), el permiso solo
se **lee** con `ContextCompat.checkSelfPermission`, el adaptador nuevo es el único fichero de superficie
Android nuevo y la degradación sigue siendo silenciosa (ADR 0005). Nada nuevo exportado.

## Documentación / estado del harness

Coherente y reiniciable: `DESIGN.md` registra la excepción de capturas, y
`docs/technical-discovery.md`/ADR 0007/`risks`/spec aceptada/`PROGRESS.md` (paréntesis de la sesión 131 +
entrada de la sesión 136) son consistentes; `feature_list.json` queda `accepted` con evidencia real;
`init.sh`/`scripts/**` sin cambios.

## Pendiente no bloqueante

Prueba **manual** de la persona en el emulador (puntos 1-6 del plan): abrir la app no muestra nada; primer
"Iniciar" muestra rationale → diálogo; conceder mantiene el aviso; denegar/"Ahora no" deja el Modo cocina,
la cuenta atrás y el "¡Listo!" intactos y no re-pregunta en la sesión; reabrir no muestra diálogo al
arrancar; ya concedido, no hay rationale. No automatizable según `AGENTS.md`; el acabado del diálogo
(claro/oscuro) se revisa tras el merge y un defecto se corrige con una feature correctiva.

## Persistencia

El **orquestador de `feature-flow`** persiste la aceptación (`feature_list.json`: `passing` → `accepted` con
esta evidencia; sesión 136 en `PROGRESS.md`) y crea el **commit local** (sin push ni PR; la publicación es
de `github-flow`).
