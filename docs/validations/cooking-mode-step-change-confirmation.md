# Validación independiente — `cooking-mode-step-change-confirmation`

- **Feature**: `cooking-mode-step-change-confirmation` (`area: ui`; depende de `cooking-mode` y
  `cooking-mode-exit-confirmation`, **ambas `accepted`**).
- **Spec**: `docs/specs/cooking-mode-step-change-confirmation.md`
- **Rama / worktree**: `feature/cooking-mode-step-change-confirmation`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-mode-step-change-confirmation`
- **Sesión**: validación independiente (subagente `validator`, contexto limpio, sin fiarse del handoff
  del implementer); el número de sesión lo fija el orquestador al persistir el veredicto.
- **Estado previo**: `passing` (implementer, trabajo en disco; `HEAD = e5012d1`, **sin commit, sin push,
  sin PR**).
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (`test` + `lint` vía `scripts/gradle.sh`, sin procesos largos, `init.sh` sin cambios). En esta pasada `:app:testDebugUnitTest`/`lint*` salieron **`UP-TO-DATE`** ⇒ **no** vale como evidencia fresca; por eso re-ejecuté todo forzado |
| Tests forzados (foco) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks --tests "com.sazon.app.ui.cooking.*"` | **exit 0**, `BUILD SUCCESSFUL in 2m 47s` (34/34 tareas ejecutadas). XML regenerados: **8 suites / 104 tests / 0 fallos / 0 errores / 0 saltados** |
| Tests forzados (total) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 3m 57s` (34/34 ejecutadas). Parseo propio de `TEST-*.xml`: **115 suites / 911 tests / 0 fallos / 0 errores / 0 saltados** |
| Suite nueva (verificación exigida) | XML por clase | `CookingStepChangePolicyTest` **15 tests / 0 fallos** |
| Suites `cooking` aceptadas | XML por clase | `CookingAlarmPolicyTest` 16, `CookingExitPolicyTest` 14, `CookingTimerStateTest` 11, `CookingModeStateTest` 10, `CookingModeFormatTest` 8, `CookingSessionTest` 16, `NotificationPermissionPolicyTest` 14 — **sin cambios de recuento** |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` vs árbol | `HEAD e5012d1`: **114 ficheros / 896 anotaciones**; árbol: **115 / 911** ⇒ delta **+1 suite / +15 tests**, exactamente `CookingStepChangePolicyTest`; **0** suites existentes modificadas |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 3m 32s` (30/30 ejecutadas). SARIF fresco `app/build/reports/lint-results-debug.sarif`: **13 resultados, 13 warning, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1 — los preexistentes, sin tocar el build) |
| Pureza de la decisión | `grep` propio | `ui/cooking/CookingStepChangeConfirmation.kt`: **0 imports**, **0** `SystemClock`/`currentTimeMillis`, sin `android.*`/`androidx.*`, sin hex/`Color(`, sin `Recipe`; solo depende de `CookingModeState` |
| Confinamiento Compose | `grep -rn 'BackHandler' app/src/main` | `BackHandler` **solo** en `CookingModeScreen.kt` (1 uso real, línea 251, + KDoc). La aparición en `CookingExitConfirmation.kt` es **solo KDoc** y ese fichero está **sin diff** |
| Alcance | `git status --porcelain` / `git diff --stat HEAD` | Solo `DESIGN.md`, `PROGRESS.md`, `docs/technical-discovery.md`, `feature_list.json`, `CookingModeScreen.kt` (+2 ficheros nuevos de código) y `docs/specs/…` (planner). **Sin diff** en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `data/**` (incl. `data/cooking/**`), `ui/theme/**`, `ui/MainShell.kt`, `CookingTimerState`/`CookingModeState`/`CookingModeFormat`, `CookingExitConfirmation`, `CookingAlarm*`/`CookingSession*`/`NotificationPermission*`, manifiesto ni `res/**`; **sin dependencias nuevas**; **no** se crea `docs/design/concepts/cooking-mode-step-change-confirmation/` |
| Estado de features | `git diff HEAD -- feature_list.json` | **Un solo** cambio de `"status"` (`not_started` → `passing`) en la entrada de **esta** feature + evidencia; ninguna otra feature tocada |
| No publicación | `git log --oneline -1` | `HEAD == e5012d1`; **0** commits nuevos de la feature (trabajo en disco), sin push ni PR |

## Escenarios de la spec verificados

1. **`NEXT` con En marcha (Scenario 1, verificación exigida).** `CookingBottomBar(onNext = requestNext)`
   → `CookingStepChangePolicy.requiresConfirmation(state, NEXT)`; con `RUNNING` da `true` (test
   `en_marcha_en_un_paso_intermedio_pide_confirmacion_en_las_dos_direcciones`) y la pantalla fija
   `pendingStepChange = NEXT` en vez de ejecutar `state.next`. El diálogo muestra `CookingStepChangeCopy`
   con "Cambiar de paso" (única acción principal, `primary`) y "Seguir en este paso" como descarte.
2. **`PREVIOUS` con Pausado (Scenario 2, verificación exigida).** `OutlinedButton(onClick = onPrevious)`
   → punto `PREVIOUS`; `PAUSED` ⇒ `true`
   (`pausado_en_un_paso_intermedio_pide_confirmacion_en_las_dos_direcciones`, con
   `assertAmbasDirecciones`): **misma decisión en las dos direcciones**, aserción explícita.
3. **Confirmar cambia de paso como hoy (Scenario 3).** `CookingStepChangeDialog(onConfirm = { pendingStepChange
   = null; state = when(point){ NEXT -> state.next(recipe.steps); PREVIOUS -> state.previous(recipe.steps) } })`
   ejecuta la navegación **aceptada sin reimplementarla**: `CookingModeState.next/previous` reinician el
   temporizador al del paso nuevo (Inactivo, con su duración) y **no** tocan `completedOrders` (el marcado
   persiste y el paso sigue tachado/atenuado). Regla aceptada de reinicio: **intacta**.
4. **Cancelar no cambia nada (Scenario 4).** `onDismissButton`/`onDismissRequest` solo ponen
   `pendingStepChange = null`: no tocan `stepIndex`, `timer`, `completedOrders`, `exiting`, la sesión ni
   la alarma; el `LaunchedEffect` del reloj (`tick(1000)`, líneas 294-299) **no** se toca y el estado es
   `rememberSaveable`, así que la cuenta atrás sigue corriendo **detrás** del diálogo.
5. **Sin temporizador en curso ⇒ directo (Scenario 5).** `IDLE` (incluido `hasDuration = false` de un paso
   sin duración, con `assertFalse(sinDuracion.timer.hasDuration)`) y `FINISHED` ⇒ `false` ⇒
   `state.next/previous` directo. **`IDLE` + `completedOrders` no vacío ⇒ `false`**
   (`inactivo_con_marcados_no_pide_confirmacion_en_ninguna_direccion`): es el caso negativo clave, que
   **fallaría** si la política copiara la condición de `CookingExitPolicy` (el marcado **no** se pierde al
   cambiar de paso). Los negativos no pasan en vacío: los positivos de 1/2 exigen `true`.
6. **Primer y último paso sin fricción (Scenario 6).** Primer paso: `PREVIOUS` ⇒ `false`
   ("Anterior" está `enabled = isEmpty || !isFirstStep` y `previous()` es no-op) y `NEXT` ⇒ `true`;
   último paso: `NEXT` ⇒ `false` (la primaria es "Terminar", `primaryIsFinish = isEmpty || isLastStep`, y
   va por `onClose = requestFinish`, la confirmación de **salida** aceptada) y `PREVIOUS` ⇒ `true`.
   Receta de un solo paso ⇒ `false` en las dos; receta sin pasos (`isEmpty`, temporizador En marcha
   inyectado) ⇒ `false` en las dos (guarda defensiva).
7. **Atrás del sistema con el diálogo de cambio de paso abierto (Scenario 7).** `BackHandler { when {
   pendingStepChange != null -> pendingStepChange = null; confirmExit -> confirmExit = false; else ->
   requestBack() } }`: la rama nueva está **antes** de la de salida, así que descarta (equivale a
   "Seguir en este paso": no cambia de paso, no sale, no reabre). Además el `AlertDialog` es su propia
   ventana y ya resuelve el atrás por `onDismissRequest = onDismiss`.
8. **Las dos confirmaciones conviven (Scenario 8).** `pendingStepChange` y `confirmExit` se fijan por
   gates disjuntos (`requestPrevious`/`requestNext` vs `requestBack`/`requestFinish`) y solo se alcanzan
   desde UI que queda bloqueada por el propio `AlertDialog` mientras hay uno abierto ⇒ **nunca** se abren
   los dos a la vez, y ninguno altera al otro. El camino de salida aceptado (header/"Terminar"/atrás sin
   diálogo) sigue idéntico (5 hunks, todos aditivos; ningún `LaunchedEffect` ni `DisposableEffect`
   modificado).

Extra: `los_ticks_no_cambian_la_decision_mientras_corre_el_temporizador` (599 ticks intermedios ⇒ `true`,
estado `RUNNING`), `la_decision_es_determinista_y_no_muta_el_estado`,
`desmarcar_no_cambia_la_decision_con_el_temporizador_en_marcha` y microcopy estable. No hay
`currentTimeMillis`/`Random` en el diff: la decisión es determinista y sin reloj.

## Hallazgos

Sin hallazgos **critical/high/medium** que impidan la aceptación. Observaciones **Low** (no bloquean, no
exigen reparación para aceptar; tres de ellas están **ya reconocidas** en los `Unknowns` de la spec):

- **L1 — Descarte con el mismo rol de color**: el `dismissButton` es un `TextButton` sin `colors` propio,
  así que hereda `primary` como "Cambiar de paso" (el énfasis lo da el orden/rol del `AlertDialog`). Es
  **exactamente** el patrón ya aceptado del `CookingExitDialog`/`NotificationPermissionRationale` y la
  spec lo declara como Unknown ("se mantiene la homogeneidad y no se corrige aquí"); el acabado lo revisa
  la persona en el emulador (defecto ⇒ feature correctiva). Coherente, no bloquea.
- **L2 — Decisión "congelada" al abrir el diálogo**: si el temporizador llega a `FINISHED` mientras el
  diálogo está abierto, no se re-evalúa ni se cierra solo (la decisión se toma **al pulsar**, que es lo
  que fija la spec). Confirmar entonces cambia de paso sin perder nada (ya Terminado). Semántica razonable
  y coherente con el Non-Goal de no pausar la cuenta atrás.
- **L3 — Microcopy "congelado" en el test** (`el_microcopy_del_dialogo_es_no_vacio_y_estable`): repite las
  4 cadenas literales de `CookingStepChangeCopy`, así que afinar el copy tras el merge obligará a
  actualizar el test. Mismo patrón que `CookingExitPolicyTest`; aceptable.
- **L4 — Rotación con el diálogo abierto**: `pendingStepChange` es `remember` (no `rememberSaveable`), así
  que al rotar el diálogo **se pierde** y la confirmación deja de estar visible, con el temporizador
  (`rememberSaveable`) intacto y corriendo. No hay pérdida de datos (una rotación **nunca** cambia de paso
  ni pierde tiempo: el descarte es el comportamiento seguro) y es la misma limitación aceptada de
  `confirmExit`; la spec ya la registra.

## Evaluación de seguridad (checklist por feature)

- **Secretos/configuración**: sin secretos, claves ni URLs en el diff; no se tocan propiedades ni
  `local.properties`.
- **Authn/z**: no aplica (app local sin identidad); es UI de una pantalla existente, sin rutas ni permisos
  nuevos.
- **Validación de entrada / inyección**: la feature no procesa entrada de usuario ni datos de red; el
  microcopy son constantes puras **sin interpolación** (nada móvil).
- **Exposición de datos / privacidad**: no se añaden logs, pantallas ni persistencia nueva. El único
  efecto de datos es el **objetivo** de la feature: cancelar **no** cambia de paso (antes un toque
  reemplazaba el temporizador del paso actual); no expone nada nuevo.
- **Servicios externos**: ninguno. Sin alarmas/notificaciones nuevas; la cancelación de alarma/servicio al
  pasar a Inactivo la siguen ejecutando los efectos aceptados existentes.
- **Dependencias/tooling**: **sin** dependencias nuevas y sin cambios en build/lockfile (`BackHandler` y
  Material 3 ya estaban); sin artefactos generados añadidos al control de versiones.
- **Frontera cliente/servidor**: no aplica (Android local).
- **Auditoría/abuso**: no aplica; no hay operación destructiva nueva (la de "Terminar" sigue detrás de la
  confirmación de salida aceptada).

Conclusión: **sin hallazgos de seguridad** relevantes en el alcance de la feature.

## Documentación durable y harness

- `docs/technical-discovery.md`: **actualizado** (diff aditivo, +17 líneas) con el **cambio de paso**
  gateado: decisión pura `CookingStepChangePolicy`, las dos direcciones con su guarda (`!isEmpty` +
  `!isLastStep`/`!isFirstStep`), `completedOrders` fuera por persistir el marcado, diálogo con los roles
  del tema y confirmar ejecutando `next()`/`previous()` sin tocar la regla de reinicio. Coherente con el
  código; el párrafo de la salida aceptada queda **intacto**.
- `DESIGN.md`: **actualizado** con la nota de **excepción de capturas** de esta feature (misma clase que
  `cooking-mode-exit-confirmation`), sin inventar tokens/colores y con el microcopy en constante pura.
  Coherente con las reglas vigentes (una sola acción principal por superficie).
- `docs/domain-model.md`, `AGENTS.md`, `docs/build-brief.md`, `docs/adr/`, `docs/specs/cooking-mode.md`:
  **sin cambios justificados** (sin entidad/estado de dominio nuevos, sin regla de repo nueva, sin
  decisión arquitectónica nueva, sin enmienda de la regla aceptada de reinicio).
- `init.sh`: **sin cambios** y sigue siendo el gate real no bloqueante (`test` + `lint`, exit 0, sin
  procesos largos). Regla `init.sh` del rubric: **cumple**.
- `PROGRESS.md` (sesión 140) y `feature_list.json` (`passing` + evidencia): reflejan el estado real y el
  siguiente paso; **todos** los números del handoff los reproduje (115 suites / 911 tests, +15 tests,
  baseline 896, focused 8/104, lint 13/0).

## Siguiente acción requerida

1. El **orquestador** persiste el veredicto: `cooking-mode-step-change-confirmation` → **`accepted`** en
   `feature_list.json`, con la evidencia de esta validación; **no** lo hace el validador.
2. Commit local en `feature/cooking-mode-step-change-confirmation` (lo crea el orquestador de
   `feature-flow` al `accept`) y publicación del PR por `github-flow` (título
   `[cooking-mode-step-change-confirmation] …`, cuerpo con `Closes #<n>`).
3. **Pendiente no bloqueante (persona, emulador)**: prueba manual del `Verification Plan` —"Siguiente" y
   "Anterior" con En marcha/Pausado, "Seguir en este paso" con la cuenta atrás viva, "Cambiar de paso" con
   el temporizador del paso nuevo en Inactivo y el marcado intacto, casos directos (Inactivo/Terminado/sin
   duración), primer/último paso, atrás que descarta el diálogo y acabado del diálogo en claro/oscuro
   frente a `DESIGN.md`—. Un defecto visual o de copy se corrige con una **feature correctiva**.

No se ha tocado código ni estado durante esta validación (solo lectura, ejecución y este registro); sin
commits, `push` ni PR de esta sesión.
