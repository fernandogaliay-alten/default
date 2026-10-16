# Validación independiente — `cooking-mode-exit-confirmation`

- **Feature**: `cooking-mode-exit-confirmation` (`area: ui`; depende de `cooking-mode` y
  `cooking-timer-foreground-service`, **ambas `accepted`**).
- **Spec**: `docs/specs/cooking-mode-exit-confirmation.md`
- **Rama / worktree**: `feature/cooking-mode-exit-confirmation`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-mode-exit-confirmation`
- **Sesión**: validación independiente (subagente `validator`, contexto limpio, sin confiar en el
  handoff del implementer); el número de sesión lo fija el orquestador al persistir el veredicto.
- **Estado previo**: `passing` (implementer, trabajo en disco; `HEAD = 83d0549`, **sin commit, sin push,
  sin PR**).
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); en esa pasada `:app:testDebugUnitTest`/`lint*` salieron `UP-TO-DATE` ⇒ **no** válida como evidencia fresca |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 4m 48s`. Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml` (regenerados a las 10:09 local por esta validación): **114 suites / 888 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 3m 50s` (`30/30` tareas ejecutadas). SARIF fresco (10:12 local): **13 resultados, 13 warning, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1 — los preexistentes) |
| Baseline re-derivada | `git grep -h '@Test' HEAD -- app/src/test \| wc -l` vs árbol | `HEAD 83d0549`: **874 anotaciones `@Test`** (113 ficheros con coincidencias); árbol: **888** ⇒ delta **+14**, exactamente `CookingExitPolicyTest`. Los XML dan **114 suites** (= 113 + la nueva) y 888 tests, coherente. (Para el recuento de ficheros uso `git grep -c`/XML, no `grep -rc`, que lista también ficheros con 0 coincidencias.) |
| Suite nueva (verificación exigida) | XML por clase | `CookingExitPolicyTest` **14 tests / 0 fallos** |
| Suites `cooking` aceptadas | XML por clase | `CookingAlarmPolicyTest` 15, `CookingTimerStateTest` 11, `CookingModeStateTest` 10, `CookingModeFormatTest` 8, `CookingSessionTest` 14, `NotificationPermissionPolicyTest` 14 — **sin cambios de recuento** |
| Pureza de la decisión | `grep` propio | `ui/cooking/CookingExitConfirmation.kt`: **0 imports**, **0** `SystemClock`/`currentTimeMillis`, sin `Recipe`; solo depende de `CookingModeState` |
| Confinamiento Compose | `grep -rn "BackHandler\|onBackPressed" app/src/` | `BackHandler` **solo** en `CookingModeScreen.kt` (1 uso real + KDoc); la otra mención es la KDoc del fichero puro. Sin apariciones previas en `app/src` (la app no interceptaba el atrás) |
| Alcance | `git status --porcelain` | Solo `DESIGN.md`, `PROGRESS.md`, `docs/technical-discovery.md`, `feature_list.json` (estado/evidencia), `CookingModeScreen.kt` (+2 nuevos de código) y `docs/specs/cooking-mode-exit-confirmation.md` (planner). **Sin diff** en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `data/**`, `ui/theme/**`, `ui/MainShell.kt`, `CookingTimerState`/`CookingModeState`/`CookingModeFormat`, `CookingAlarm*`/`CookingSession*`/`NotificationPermission*`, manifiesto ni `res/**`; **sin dependencias nuevas**; **no** se crea `docs/design/concepts/cooking-mode-exit-confirmation/` |
| Camino de salida intacto | inspección del diff | `exiting` → `LaunchedEffect(exiting)` → `session.clear()` → `onClose()` → `onDispose` (`CookingAlarmPolicy.onExit()`); no queda rastro de `requestExit`; el `LaunchedEffect` del reloj (`tick(1000)`) **no** se toca |
| No publicación | `git rev-parse HEAD` | `HEAD == 83d0549`, **0** commits nuevos por la feature (trabajo en disco) |

**Nota sobre `testDebugUnitTest lint --rerun-tasks` en una sola invocación**: lo ejecuté primero y
**falla** con `A problem was found with the configuration of task ':app:generateDebugLintReportModel' …
uses this output of task ':app:syncTaxonomy' without declaring an explicit or implicit dependency
(app/src/main/assets)`. Es **deuda preexistente y documentada** (`PROGRESS.md`, sesión del chore
`gradle-daemon-hang`, punto **D2**: unir `test lint` en una sola invocación falla por validación de
dependencia implícita y arreglarlo exige tocar el build, fuera de alcance; también en
`docs/risks-and-open-questions.md`). No es un defecto de esta feature (no toca el build) y **no** afecta
al gate canónico: `bash init.sh` invoca las dos tareas por separado y da exit 0. Reproducido de forma
independiente; queda como **nota**.

## Escenarios de la spec verificados

1. **"Atrás" con el temporizador En marcha (Scenario 1, verificación exigida).** `CookingModeHeader(
   onBack = requestBack)` → `CookingExitPolicy.requiresConfirmation(state, BACK_BUTTON)`; con `RUNNING`
   la regla da `true` (test `en_marcha_pide_confirmacion_por_los_dos_puntos_de_salida`) y la pantalla
   abre `CookingExitDialog` en vez de `exiting = true`.
2. **"Terminar" con pasos marcados (Scenario 2, verificación exigida).** `CookingBottomBar(onClose =
   requestFinish)` → punto `FINISH`; `IDLE` + `completedOrders` no vacío ⇒ `true`. Cubierto por
   `inactivo_con_pasos_marcados_pide_confirmacion_por_los_dos_puntos_de_salida` **y** por
   `un_ultimo_paso_inactivo_con_marcados_es_el_escenario_de_terminar` (último paso real, sin duración).
3. **Gesto/tecla atrás del sistema (Scenario 3).** `BackHandler { if (confirmExit) confirmExit = false
   else requestBack() }` declarado en la capa Compose de `CookingModeScreen`, que `MainShell` compone
   dentro de la **misma** actividad (overlay por `AnimatedContent`): mientras el modo está compuesto el
   atrás lo consume `BackHandler` y **no** cierra la actividad. Fuera del modo no hay `BackHandler`
   (una sola aparición en `app/src`). Verificado por inspección, como fija la spec (no automatizable).
4. **Confirmar sale y descarta (Scenario 4).** `onConfirm = { confirmExit = false; exiting = true }` y a
   partir de ahí el camino aceptado **sin cambios**: `session.clear()` → `onClose()` → desmontaje con
   `alarm.apply(CookingAlarmPolicy.onExit())`.
5. **Cancelar no cambia nada (Scenario 5).** `onDismiss`/`dismissButton` solo ponen `confirmExit =
   false`: no tocan `exiting`, la sesión, la alarma/servicio ni el reloj; la cuenta atrás sigue porque
   el `LaunchedEffect` del `tick(1000)` es independiente y no se modificó.
6. **Nada que perder ⇒ salida directa (Scenario 6).** `IDLE`/`FINISHED` sin marcados ⇒ `false` ⇒
   `exiting = true` (tests negativos `inactivo_sin_nada_que_perder_…`, `terminado_solo_…`, receta sin
   pasos `isEmpty`). Los negativos **fallarían** si la política aprobara siempre (no es un test vacío).
7. **Pausado también cuenta (Scenario 7).** `PAUSED` ⇒ `true`
   (`pausado_sin_pasos_marcados_pide_confirmacion_por_los_dos_puntos_de_salida`).
8. **Atrás con el diálogo abierto (Scenario 8).** Con `confirmExit == true` el `BackHandler` pone
   `confirmExit = false` (y además el `AlertDialog` es su propia ventana y ya resuelve el atrás por
   `onDismissRequest = onDismiss`): descarta, no sale, no reabre y el estado queda intacto.

Extra: `los_ticks_no_cambian_la_decision_mientras_corre_el_temporizador` (599 ticks intermedios ⇒
`true`), `los_dos_puntos_de_salida_piden_siempre_lo_mismo` (7 estados ⇒ invariante `BACK_BUTTON ==
FINISH`), `la_decision_es_determinista_y_no_muta_el_estado` y `desmarcar_el_ultimo_paso_vuelve_a_permitir_la_salida_directa`.

## Hallazgos

Sin hallazgos **critical/high/medium** que impidan la aceptación. Observaciones **Low** (no bloquean, no
exigen reparación para aceptar):

- **L1 — Microcopy "congelado" en el test** (`CookingExitPolicyTest.el_microcopy_del_dialogo_es_no_vacio_y_estable`):
  repite las 4 cadenas literales de `CookingExitCopy`, así que cualquier retoque de copy hace fallar el
  test. La spec permite afinar el texto tras el merge (defecto ⇒ feature correctiva), y esa corrección
  tendrá que actualizar el test; conviene saberlo. No hay precedente de este aserto en
  `NotificationPermissionPolicyTest`, pero tampoco contradice ninguna regla. Aceptable tal cual.
- **L2 — Decisión "congelada" al abrir el diálogo**: si el temporizador llega a `FINISHED` mientras el
  diálogo está abierto y no había pasos marcados, el diálogo sigue visible (la decisión pura se evalúa
  **al pulsar**, que es lo que fija la spec; no se re-evalúa ni se cierra solo). "Salir" sigue siendo una
  salida explícita válida, así que no hay pérdida ni bloqueo; es una semántica razonable y coherente con
  el Non-Goal de no pausar la cuenta atrás. No es incumplimiento de ningún escenario.
- **L3 — Descarte con el mismo rol de color**: el `dismissButton` es un `TextButton` sin `colors`
  propio, así que hereda `primary` igual que "Salir" (el énfasis lo da el orden/rol del `AlertDialog`).
  Es **exactamente** el patrón del `NotificationPermissionRationale` ya aceptado y lo que la nota de
  `DESIGN.md` declara ("una sola acción principal, descarte sin énfasis"). El acabado visual lo revisa
  la persona en el emulador tras el merge (defecto ⇒ feature correctiva), según `AGENTS.md`.

No se ha tocado código ni estado durante esta validación (solo lectura y ejecución); no hay commits,
`push` ni PR de esta sesión.

## Evaluación de seguridad (checklist por feature)

- **Secretos/configuración**: sin secretos, claves ni URLs en el diff; no se tocan `.env`/propiedades.
- **Authn/z**: no aplica (app local sin identidad; el cambio es de UI de una pantalla existente y no
  altera rutas ni permisos).
- **Validación de entrada / inyección**: la feature no procesa entrada de usuario ni datos de red; el
  microcopy son constantes puras sin interpolación.
- **Exposición de datos / privacidad**: no se añaden logs, pantallas ni persistencia nueva. La única
  consecuencia de datos es que **cancelar** ya **no** borra la sesión (antes un toque la descartaba);
  eso es precisamente el objetivo de la feature y no expone nada nuevo (la sesión ya se persistía con la
  feature aceptada `cooking-timer-foreground-service`).
- **Servicios externos**: ninguno.
- **Dependencias/tooling**: **sin** dependencias nuevas y sin cambios en build/lockfile (`BackHandler`
  ya estaba disponible por `androidx.activity:activity-compose` 1.8.0). Sin artefactos generados
  añadidos al control de versiones.
- **Frontera cliente/servidor**: no aplica (Android local).
- **Auditoría/abuso**: no aplica; ninguna operación destructiva nueva (la única acción destructiva sigue
  siendo la salida explícita ya aceptada, ahora **detrás de una confirmación**, lo que **reduce** el
  riesgo de pérdida accidental de estado).

Conclusión: **sin hallazgos de seguridad** relevantes en el alcance de la feature (ni notas accionables).

## Documentación durable y harness

- `docs/technical-discovery.md`: **actualizado** con la salida gateada del Modo cocina (decisión pura,
  los dos puntos de salida, `BackHandler` solo mientras el modo está compuesto, y "solo al confirmar" se
  cancelan alarma/servicio y se borra la sesión). Coherente con el código.
- `DESIGN.md`: **actualizado** con la nota de **excepción de capturas** de esta feature (misma clase que
  `cooking-timer-alarm-permission-context`), sin inventar tokens/colores y con el microcopy en constante
  pura. Coherente con el `AlertDialog` implementado.
- `docs/domain-model.md`, `AGENTS.md`, `docs/build-brief.md`, `docs/adr/`: **sin cambios justificados**
  (sin entidad/estado de dominio nuevos, sin regla de repo nueva, sin decisión arquitectónica nueva).
- `init.sh`: **sin cambios** y sigue siendo el gate real no bloqueante (`test` + `lint`, exit 0, sin
  procesos largos). Regla `init.sh` del rubric: **cumple**.
- `PROGRESS.md` (sesión 138) y `feature_list.json` (`passing` + 9 entradas de evidencia): reflejan el
  estado real y el siguiente paso; el handoff del implementer es preciso (los números que verifiqué
  coinciden).

## Siguiente acción requerida

1. El **orquestador** persiste el veredicto: `cooking-mode-exit-confirmation` → **`accepted`** en
   `feature_list.json`, con la evidencia de esta validación; **no** lo hace el validador.
2. Commit local en `feature/cooking-mode-exit-confirmation` (lo crea el orquestador de `feature-flow` al
   `accept`) y publicación del PR por `github-flow` (título `[cooking-mode-exit-confirmation] …`, cuerpo
   con `Closes #<n>`).
3. **Pendiente no bloqueante (persona, emulador)**: prueba manual del `Verification Plan` (tres vías con
   y sin algo que perder, "Seguir cocinando" con la cuenta atrás viva, "Salir" limpia sesión/alarma y no
   reanuda al reabrir, atrás con el diálogo abierto, y acabado del diálogo en claro/oscuro frente a
   `DESIGN.md`). Un defecto visual o de copy se corrige con una **feature correctiva**.
4. `cooking-mode-step-change-confirmation` (depende de esta) queda desbloqueada al persistir `accepted`.
