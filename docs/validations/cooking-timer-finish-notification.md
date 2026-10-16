# Validación independiente — `cooking-timer-finish-notification`

- **Feature**: `cooking-timer-finish-notification` (`area: integration`, correctiva de
  `cooking-timer-foreground-service`)
- **Spec**: `docs/specs/cooking-timer-finish-notification.md`
- **Rama / worktree**: `feature/cooking-timer-finish-notification`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-timer-finish-notification`
- **Sesión**: 139 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, sesión 138, trabajo en disco; `HEAD = d75c7a5`, **sin commit,
  sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); `testDebugUnitTest`/`lintDebug` salieron `UP-TO-DATE` ⇒ **no** válido como evidencia fresca |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL 3m59s**, `TEST_EXIT=0`; parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **113 suites / 882 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL 3m15s**, `LINT_EXIT=0`; SARIF fresco: **13 resultados, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) — idéntico al baseline |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` | `HEAD d75c7a5`: **113 ficheros / 874 anotaciones** ⇒ delta exacto **+8 tests / 0 suites nuevas** = 882 |
| Reparto del delta | XML por clase | `CookingAlarmPolicyTest` 15→**16**, `CookingSessionTest` 14→**16**, `AndroidCookingAlarmTest` 10→**13**, `CookingNotificationsTest` 15→**17**; suites aceptadas de `cooking` **sin cambio de recuento** |
| Alcance | `git status --porcelain` (17 entradas, idénticas antes y después de la sesión) | solo los 9 ficheros de `app/src` previstos + 6 docs durables + `feature_list.json` + `PROGRESS.md`; **sin** tocar `CookingTimerService`, `CookingAlarmReceiver`, manifiesto, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `ui/theme/**`, `docs/design/**`; sin dependencias ni permisos nuevos |
| Pureza | `grep` propio | `ui/cooking/CookingAlarm.kt` y `ui/cooking/CookingSession.kt` con **0** imports `android.*`/`androidx.*` (único import: `domain.recipe.Recipe`) y **0** lecturas del reloj |
| Confinamiento Android | `grep` propio | `SystemClock`/`Service`/`NotificationManagerCompat` **solo** bajo `ui/cooking/CookingModeScreen.kt` (fuente del instante, ya aceptado) y `data/cooking/` |
| No mutación por el validador | `git status --porcelain` antes/después | 17 entradas idénticas ⇒ el validador no tocó nada |

## Escenarios verificados

1. **Llegar a cero no cancela la alarma (Scenario 1, verificación exigida).** `CookingAlarm.kt:69` añade
   `next.isFinished && previous != next -> None` **antes** de la regla genérica `previous != next ->
   Cancel`; `CookingAlarmPolicyTest` invierte `llegar_a_cero_no_cancela_la_alarma_y_no_reprograma` (cruce del
   cero → `None`, ticks posteriores → `None`) y la corrida completa `IDLE→RUNNING` + 600 ticks + 2 no-ops
   emite **exactamente 1 `Schedule` y 0 `Cancel`**.
2. **El aviso se publica siempre y el servicio se para solo (Scenario 2, verificación exigida).**
   `CookingNotificationsTest` fija id `1001`, `setAutoCancel(true)` y **sin** `FLAG_ONGOING_EVENT`, sin
   extras de receta y republicación que reemplaza; `CookingTimerService` **sin diff** (sigue parando con su
   propio `delay` + `STOP_FOREGROUND_REMOVE`). La comprobación real en primer plano/minimizado es **prueba
   manual** (no automatizable; ver más abajo).
3. **Pausar, Reiniciar, cambiar de paso y salir siguen cancelando (Scenario 3, no-regresión).** Pausar /
   Reiniciar (desde En marcha, Pausado y Terminado — `reiniciar_desde_terminado_si_cancela_la_alarma`) /
   cambio de paso / `onExit()` / `FINISHED → IDLE` siguen dando `Cancel`; "Reanudar" vuelve a programar con
   el instante correcto (`al_reanudar_se_reprograma_desde_el_tiempo_congelado`).
4. **El toque del aviso vuelve al paso en Terminado (Scenario 4, verificación exigida).**
   `CookingSession.reopensFromNotification = isResumable || FINISHED` (`CookingSession.kt:44-45`), consumido
   por el gate de `resumeRequest` en `MainShell`; tests del predicado (En marcha/Pausado/Terminado `true`,
   Inactivo `false`) y de la reapertura de una sesión Terminada (`remaining = 0`, sin rearmar alarma ni
   servicio); el "¡Listo!" en pantalla se renderiza en la rama `CookingTimerStatus.FINISHED`
   (`CookingModeScreen.kt:588-600`); `isResumable` **intacto** y sigue vivo.
5. **La salida explícita limpia el aviso; la rotación no (Scenario 5).** `dismissFinishNotification()` se
   llama **solo** en `CookingModeScreen.kt:173` (`LaunchedEffect(exiting)`, antes de
   `session.clear()`/`onClose()`); el `DisposableEffect` → `onDispose` sigue **sin diff** (solo
   `CookingAlarmPolicy.onExit()`); `cancelar_no_retira_la_1001_ya_mostrada_guarda_decision_9` guarda la
   decisión 9 del ADR 0007 (una rotación **no** borra la `1001` por construcción).
6. **Degradación sin permiso (Scenario 6).** Sin permiso `notify` no publica y no lanza (preexistente);
   `dismiss` idempotente y sin permiso sin lanzar (`runCatching` en el adaptador).

## Validator Checklist

**10/10** verificados: (1) arista `→ FINISHED = None` con `Cancel` intacto para Pausar/Reiniciar/cambio de
paso/salir/`onExit()`/`FINISHED → IDLE` y ticks `None`; (2) escenarios 1-6; (3) verificación exigida
presente (`bash init.sh` verde + evidencia fresca forzada y conteos del XML); (4) pureza de
`CookingAlarm.kt`/`CookingSession.kt`; (5) `dismissFinishNotification` solo en la salida explícita y
`onDispose` sin diff; (6) `1001` con id/canal/microcopy/small icon intactos, no `ongoing` y sin datos de
receta; (7) sin dependencias/permisos/manifiesto/build/lockfile/`init.sh`/`scripts/**`/`domain/**`/Room y
`CookingTimerService` sin diff; (8) docs durables actualizados; (9) sin comportamiento no pedido; (10)
estado en `feature_list.json`.

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

- **Low L1 (informativo, no bloqueante).** La `1001` obsoleta tras "Reiniciar" desde Terminado o al avanzar
  de paso sigue sin retirarse: `dismissFinishNotification` está cableado **solo** a la salida explícita. Es
  exactamente el alcance que fija la spec (P4) y queda documentado como cosmético en el ADR 0007. Requeriría
  una **feature correctiva** si molesta; **no** es defecto de esta feature.
- **Low L2 (informativo).** `dismiss_finish_notification_es_idempotente_y_sin_permiso_no_lanza` combina
  permiso denegado y ausencia de notificación mostrada: comprueba "no lanza", pero no ejercita "hay `1001`
  mostrada y luego se deniega el permiso". Cobertura suficiente; no exigible por la spec.
- **Low L3 (informativo).** La frase "el adaptador `AndroidCookingAlarm` **NO** cancela la alarma al llegar a
  cero" se acredita de forma transitiva (política → `None`; `none_no_toca_el_alarmmanager` /
  `none_no_arranca_ni_para_el_servicio`), sin un test ad-hoc del adaptador para esa frase.

## Seguridad (checklist portable, alcance = diff)

Sin hallazgos, ni bloqueantes ni notas relevantes: sin secretos/config nueva; sin permisos nuevos
(`POST_NOTIFICATIONS` intacto y `NotificationManagerCompat.cancel` no requiere permiso, con `runCatching` ⇒
sin crash con permiso denegado); la `1001` mantiene id/canal/microcopy/small icon y ningún dato de receta
(el `Intent` de contenido sigue llevando solo el booleano); superficie nueva mínima (1 método de costura + 1
arista de política, sin red, I/O, SQL, input de usuario ni intents implícitos); 0 dependencias nuevas.

## Pendiente no bloqueante (persona, emulador)

No automatizable desde WSL según `AGENTS.md`: con la app en primer plano el aviso **queda** en la bandeja al
llegar a cero; minimizada, lo mismo; tocar el aviso vuelve al paso aunque el temporizador ya esté en
Terminado; "Atrás"/"Terminar" limpian el aviso; una **rotación** no lo borra; descartar y "borrar todo" sin
crash; sin permiso, nada se rompe. Trade-off aceptado y documentado: en primer plano el heads-up del canal
de importancia alta puede sonar/vibrar **además** del "¡Listo!" en pantalla, **sin duplicado** (mismo id
`1001`, disparo único). Un defecto de acabado se corrige con una **feature correctiva**.
