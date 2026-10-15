# Validación independiente — `cooking-timer-foreground-service`

- **Feature**: `cooking-timer-foreground-service` (`area: integration`, ampliación de
  `cooking-timer-alarm`)
- **Spec**: `docs/specs/cooking-timer-foreground-service.md`
- **Rama / worktree**: `feature/cooking-timer-foreground-service`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/cooking-timer-foreground-service`
- **Sesión**: 137 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, trabajo en disco; `HEAD = 3c3ec1b`, **sin commit, sin push,
  sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); `:app:testDebugUnitTest UP-TO-DATE` ⇒ **no** válido como evidencia fresca y forzada |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, **BUILD SUCCESSFUL 4m28s, 34/34 tareas ejecutadas**; parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **113 suites / 874 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | exit 0, **BUILD SUCCESSFUL 3m51s, 30/30 tareas ejecutadas**; SARIF fresco: **13 resultados, todos warning, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` | `HEAD 3c3ec1b`: **109 ficheros / 830 anotaciones** ⇒ delta exacto **+4 suites / +44 tests** = 874 |
| Suites nuevas | XML por clase | `CookingSessionTest` 14 (**la verificación exigida**), `CookingTimerServiceTest` 10, `RoomCookingSessionStoreTest` 7, `CookingTimerServiceManifestTest` 6 |
| Suites modificadas | XML por clase | `CookingNotificationsTest` 11→15, `AndroidCookingAlarmTest` 7→10 |
| Suites `cooking` aceptadas | XML por clase | `CookingAlarmPolicyTest` 15, `CookingTimerStateTest` 11, `CookingModeStateTest` 10, `CookingModeFormatTest` 8, `NotificationPermissionPolicyTest` 14 — **sin cambios** |
| Pureza | `grep` propio | `ui/cooking/CookingSession.kt` y `CookingSessionStore.kt` con **0** imports `android.*`/`androidx.*` (único import: `Recipe`) y **0** `SystemClock`/`currentTimeMillis` |
| Confinamiento Android | `grep` propio | `Service`/`startForeground`/`stopForeground` **solo** bajo `data/cooking/` (más el manifiesto) |
| Alcance | `git status --porcelain -uall` (32 entradas, idénticas antes y después de la sesión) | build/lockfile/`init.sh`/`scripts/**`/`domain/**`/`ui/theme/**`/`res/xml/**`/`ic_notification_timer.xml`/`app/lint.xml`/suites aceptadas de `cooking` **sin diff**; **sin dependencias nuevas**; **no** existe `docs/design/concepts/cooking-timer-foreground-service/` |
| No publicación | `git rev-parse HEAD` | `HEAD == 3c3ec1b`, **0 commits** por delante, árbol con cambios sin commitear |

## Escenarios verificados

1. **Iniciar/Reanudar arrancan el servicio (Scenario 1, verificación exigida).** `AndroidCookingAlarm`
   añade la pata `Schedule` → `ContextCompat.startForegroundService(CookingTimerService, …)` con el
   **mismo** instante de fin que la alarma exacta aceptada, y `CookingTimerService` llama a
   `startForeground(1000, CookingNotifications.ongoing(...))` de forma inmediata; el caso de reanudar
   En marcha vuelve a arrancar el servicio con el nuevo instante (test del adaptador + del servicio).
2. **Cuenta atrás visible fuera de la app (Scenario 2).** La notificación persistente usa
   `setUsesChronometer(true)` + `setChronometerCountDown(true)` + `setWhen(...)`: la pinta el **sistema**,
   sin publicación por segundo ni `tick` en el servicio. La comprobación real con la app minimizada /
   móvil bloqueado es **prueba manual** (no automatizable; ver más abajo).
3. **Aviso una sola vez al llegar a cero (Scenario 3).** El servicio **no** publica el "¡Listo!" (lo sigue
   publicando la alarma/receptor aceptados con id `1001`); `CookingSessionTest` fija que en `Terminado` la
   sesión no proyecta fin, la política da `Cancel` en `RUNNING→FINISHED` y `None` en los ticks siguientes.
4. **Pausar/Reiniciar/cambiar de paso/salir (Scenario 4).** `Cancel` del adaptador hace `stopService`
   además de `alarmManager.cancel`; cada operación con su `runCatching`.
5. **Vuelta al paso exacto (Scenario 5).** `EXTRA_RESUME_COOKING` (solo un booleano) viaja en el
   `contentIntent` compartido (`FLAG_IMMUTABLE`, `NEW_TASK or SINGLE_TOP`) y `MainActivity` lo convierte en
   `resumeRequest` desde `onCreate` **y** `onNewIntent`; `MainShell` reabre el overlay solo si la sesión es
   resumible. Con el proceso muerto, la sesión persistida permite recalcular el restante
   (`resumedMode(now)`) o dar `Terminado`/`0` si el instante ya pasó.
6. **Muerte del proceso / instante ya pasado (Scenario 6).** `CookingSessionTest` cubre `RUNNING` con
   restante, `FINISHED`/`0` si el instante pasó y degradación defensiva si `endAtElapsedMillis` es `null`.
7. **Pausar/reiniciar/salir detienen el servicio (Scenario 7).** `Cancel` → `stopService`; la salida
   explícita limpia la sesión (`clear()` antes de `onClose()`), mientras `onDispose` **no** borra nada.
8. **Arranque normal no abre el Modo cocina (Scenario 8).** Sin `EXTRA_RESUME_COOKING` no se incrementa
   `resumeRequest`; la carga de la sesión exige el flag.
9. **Intents mínimos / sin fuga de datos (Scenario 9).** El `Intent` de arranque del servicio lleva **solo**
   el `Long` de fin; los de contenido **solo** un booleano. Tests: `claves.any { it.startsWith("com.sazon") }`,
   `EXTRA_TEXT == null` y conjunto de extras mínimo ⇒ la receta/pasos/ingredientes y la clave de IA **no**
   cruzan al sistema.
10. **Manifiesto (Scenario 10).** `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` (permisos
    normales) y `<service exported="false" foregroundServiceType="specialUse">` con su `<property>`; test
    con **caso negativo** sobre el XML mutado (el test no puede pasar en vacío) y receptor/permisos
    aceptados intactos.

## Validator Checklist (spec)

Los 10 puntos se cumplen. Matiz: la lectura amplia del punto 3 se relaciona con el hallazgo **L1** (abajo).

## Hallazgos

**Critical / High / Medium: ninguno.**

- **L1 (Low).** Una sesión persistida como `FINISHED` no reanuda desde la notificación del "¡Listo!":
  `CookingSession.isResumable` exige En marcha o Pausado y `MainShell` hace `return` si no lo es. La rama
  realista (minimizado/bloqueado, la recomposición no llega a persistir `FINISHED`) **sí** funciona. La
  spec se contradice a sí misma (`isResumable` frente al paréntesis del `Terminado`); la implementación
  sigue la definición normativa + decisión documentada (ADR 0008 §7, `docs/domain-model.md`). Follow-up:
  aclaración de la spec y/o feature correctiva. **No bloquea.**
- **L2 (Low).** Rotar con el modo abierto desde una notificación lo **reabre** (el `EXTRA_RESUME_COOKING`
  sobrevive en el `Intent` de lanzamiento y `observeResumeCookingRequest` corre en cada `onCreate`). No
  contradice ningún escenario de esta spec (`cooking-mode.md:425` dice "rotar cierra el modo", no resuelto
  allí). Follow-up: verificar en la prueba manual; correctiva si molesta. **No bloquea.**
- **L3 (Low, nota).** El cableado Compose/Activity (gate `isResumable`, `LaunchedEffect(resumeRequest)`,
  `onNewIntent`, `clear()` al salir, no-borrado en `onDispose`) se verifica por **inspección**, coherente
  con `AGENTS.md` y con el `Verification Plan` de la spec (que solo exige JVM/Robolectric). **No bloquea.**

## Seguridad (feature-scoped)

Sin hallazgos accionables. Sin secretos en el diff; notificaciones sin receta/pasos/ingredientes/clave;
`<service>` y `<receiver>` `exported="false"`; Room con queries parametrizadas y `runCatching` (fila
corrupta ⇒ "sin sesión", sin crash); `PendingIntent` `FLAG_IMMUTABLE`; permisos normales justificados con
`<property>`; **cero** dependencias nuevas; degradación silenciosa en todas las patas.

## Documentación y harness

`docs/adr/0008-cooking-timer-foreground-service.md` (nuevo), revisión de las alternativas 3/4 y del
"estado de arranque" en `docs/adr/0007-cooking-timer-alarm.md`, ampliación acotada del Non-Goal en
`docs/build-brief.md`, `docs/technical-discovery.md`, `docs/domain-model.md`,
`docs/risks-and-open-questions.md` (fila "tocar la notificación no devuelve al paso" cerrada + tres
riesgos nuevos) y `DESIGN.md` (excepción de capturas). `AGENTS.md`, `CONTEXT.md`, `init.sh`, `scripts/**`
y `app/lint.xml` **sin tocar**; el repo sigue reiniciable desde `bash init.sh`. Implementer **sin**
commit/push/PR (HEAD sigue en `3c3ec1b`).

## Pendiente (no bloqueante, no automatizable desde WSL)

Prueba **manual** de la persona en el emulador, puntos 1-7 de la spec: cuenta atrás decreciendo con la app
minimizada/móvil bloqueado; sonido/vibración al llegar a cero con la persistente desapareciendo sin
duplicados; toque que vuelve al paso exacto con el proceso vivo **y** muerto; pausar/reiniciar/cambiar de
paso/salir detienen el servicio; permiso denegado sin crash; lanzador que **no** abre el Modo cocina. El
acabado (aspecto del cronómetro en la pantalla de bloqueo, orden de las dos notificaciones, tono/patrón) se
revisa tras el merge; un defecto se corrige con una **feature correctiva**.

## Siguiente acción

El orquestador de `feature-flow` persiste `accepted` en `feature_list.json` + `PROGRESS.md` y crea el
commit local; la publicación (push + PR) queda para el orquestador de `github-flow`.
