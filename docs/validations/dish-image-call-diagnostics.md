# Validación independiente — `dish-image-call-diagnostics`

- **Feature**: `dish-image-call-diagnostics` (`area: integration`; depende de `dish-image-generator`,
  **`accepted`**).
- **Spec**: `docs/specs/dish-image-call-diagnostics.md`
- **Rama / worktree**: `feature/dish-image-call-diagnostics`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-call-diagnostics`
- **Sesión**: validación independiente (subagente `validator`, contexto limpio, sin fiarse del handoff
  del implementer); el número de sesión lo fija el orquestador al persistir el veredicto.
- **Estado previo**: `passing` (implementer, sesión 142; trabajo en disco; `HEAD == 5ef8f7a` — merge del
  PR #161 —, **sin commit, sin push, sin PR**).
- **Veredicto**: **`accept`**

## Alcance validado

Correctiva de **observación pasiva** de la llamada HTTP de imagen de DevExpert:

- nuevo `app/src/main/java/com/sazon/app/data/ai/image/DishImageDiagnostics.kt` (`DishImageCall`,
  `DishImageCallReason`, `DishImageCallEvent`),
- `DishImagePolicy.reasonFor(httpStatus, deliveredBytes)` pura y total (único sitio de la política),
- costura aditiva `onEvent`/`now` en `DevExpertDishImageGenerator` con un evento por llamada HTTP real
  en `editImage` (`/images/edits`) y `generateFromText` (`/images/generations`),
- una línea `Log.d("SazonDishImage", …)` en la construcción del adaptador en `MainActivity`.

## Verificación re-ejecutada por el validador (números reales)

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0**. Con matiz: `:app:testDebugUnitTest`/`:app:test` y los `lintAnalyze*`/`lintReportDebug` salieron **`UP-TO-DATE`** (30 tareas, 1 ejecutada) ⇒ **no** vale como evidencia fresca; por eso re-ejecuté todo forzado |
| Tirada completa **forzada** | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 4m 2s`. Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **115 suites / 927 tests / 0 fallos / 0 errores / 0 saltados** |
| Paquete `data.ai.image` (XML fresco) | mismas XML por clase | **6 suites / 62 tests / 0 fallos**: `DevExpertDishImageGeneratorTest` 33, `DishImagePolicyTest` 12, `DishImageStyleReferenceTest` 7, `AndroidDishImageReducerTest` 5, `DishImagePayloadTest` 3, `AssetDishImageStyleReferenceTest` 2 |
| Baseline re-derivada (no se confía del handoff) | `git show HEAD:<test>.kt \| grep -c '@Test'` vs árbol | `HEAD 5ef8f7a`: **911** anotaciones (`data.ai.image` **46**); árbol: **927** (`data.ai.image` **62**) ⇒ delta **+16 tests**, exactamente `DevExpertDishImageGeneratorTest` **20→33** y `DishImagePolicyTest` **9→12**; **0** suites nuevas/eliminadas y **0** aserciones existentes modificadas (las diffs de test son puramente aditivas) |
| Lint **forzado** | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 3m 21s`. SARIF fresco `app/build/reports/lint-results-debug.sarif`: **13 resultados, 13 warning, 0 errores**, **0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1 — los preexistentes; sin tocar build) |
| Nombres de los tests nuevos (XML fresco) | `testcase/@name` | 13 nuevos en `DevExpertDishImageGeneratorTest` (`una_llamada_de_texto_con_bytes_emite_un_evento_ok`, `la_duracion_del_evento_usa_el_reloj_inyectado`, `los_estados_no_2xx_emiten_el_motivo_clasificado`, `un_fallo_de_transporte_emite_transport_sin_estado`, `un_200_ilegible_emite_invalid`, `un_200_que_el_reductor_rechaza_emite_invalid`, `el_ajuste_con_bytes_emite_un_evento_edit_ok`, `el_transporte_del_ajuste_emite_un_solo_edit_transport`, `un_rechazo_del_ajuste_emite_edit_y_luego_generate`, `un_200_inutilizable_del_ajuste_emite_edit_invalid_y_generate`, `sin_clave_no_hay_peticiones_ni_eventos`, `una_llamada_cancelada_no_emite_evento`, `ningun_evento_expone_la_clave_la_cabecera_ni_el_prompt`) y 3 en `DishImagePolicyTest` (`reasonFor_clasifica_los_estados_exigidos`, `reasonFor_distingue_ok_de_invalid_en_los_2xx`, `reasonFor_no_confunde_motivos`). Todos verdes |
| Alcance | `git status --porcelain` / `git diff --stat` | Solo lo previsto: `MainActivity.kt` (+12), `DevExpertDishImageGenerator.kt` (+105), `DishImagePolicy.kt` (+21), los 2 tests del paquete, 3 docs y `feature_list.json`/`PROGRESS.md`; nuevos `DishImageDiagnostics.kt` y `docs/specs/dish-image-call-diagnostics.md` (planner). **0 diff** en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `app/src/main/assets/**`, `app/src/main/res/**`, `init.sh`, `scripts/**`, `domain/**`, `ui/**`, `data/image/**`, `data/ai/AiDiagnostics.kt` y el resto de `data.ai`; **sin dependencias nuevas** |
| Estado de features | `git diff -U0 -- feature_list.json` | Un **único** cambio de `"status"` (`not_started` → `passing`) en la entrada de **esta** feature + evidencia; ninguna otra tocada |
| No publicación | `git log --oneline -1` | `HEAD == 5ef8f7a`; **0** commits nuevos (trabajo en disco), sin push ni PR |

## Escenarios de la spec verificados (lectura del diff + XML)

1. **Scenario 1 — 2xx con bytes ⇒ `OK`.** El camino de texto emite **un** evento `GENERATE`/`OK`/
   `status=200`/`bytes=size`/`durationMillis>=0` y `detail="HTTP 200"`; `1` petición. La variante del
   reloj inyectado fija la duración exacta (`250 ms`), lo que demuestra que se mide **por llamada**.
2. **Scenario 2 — clasificación no-2xx y transporte.** `402/429 → CUOTA`, `401/403 → AUTH`,
   `500/503 → SERVER`, `400/404/408 → HTTP_ERROR`, y `IOException`/`SocketTimeoutException`/
   `HttpRequestTimeoutException → TRANSPORT` con `httpStatus=null`; en todos `bytes=null`, `null` sin
   lanzar y **una** petición. Leído en el código: cada rama hace `report(...)` y **retorna
   inmediatamente** (no hay doble emisión ni camino que emita dos veces).
3. **Scenario 3 — 2xx ilegible ⇒ `INVALID`.** Cubiertos `"no soy json"`, `"{}"`, `"{\"data\":[]}"`,
   `b64_json` vacío y base64 inválido, más el caso en que el `DishImageReducer` devuelve `null`.
   `status=200`, `bytes=null`.
4. **Scenario 4 — camino de ajuste.** `EDIT`/`OK` con `200`; transporte del ajuste ⇒ **un solo** `EDIT`
  /`TRANSPORT` **sin** respaldo; rechazo definitivo `404` ⇒ `EDIT`/`HTTP_ERROR` **y después**
   `GENERATE`/`OK` (2 eventos, **2** peticiones, sin tercera); `200` inutilizable del ajuste ⇒ `EDIT`/
   `INVALID` + `GENERATE`. Coherente con `editFailureRequiresTextFallback` y `MAX_ATTEMPTS = 1`.
5. **Scenario 5 — no-fuga.** El test recoge 5 eventos de cuatro caminos y comprueba que **ningún**
   `toString()` contiene `test-key`, `Bearer`, `Authorization` ni el `prompt` reconocible. Verificado
   también por inspección: `DishImageCallEvent` **solo** tiene `call`/`reason`/`httpStatus`/
   `durationMillis`/`detail`/`bytes`; `detail` se construye con `"HTTP $status"` o `"transport"`; el
   fichero `DishImageDiagnostics.kt` tiene **0 imports** y ningún campo de clave/cabecera/prompt/cuerpo.
6. **Scenario 6 — regresión.** Clave en blanco (`""` y `"   "`): `0` peticiones y **0** eventos (el
   `apiKey.isBlank()` de `generate` retorna **antes** de cualquier fase). Cancelación: se **relanza**
   *antes* de `report` en las dos fases (`catch (CancellationException) { throw … }` precede a los
   `catch (Exception) { report(...) }`), con test de "cancelada ⇒ 0 eventos". Sin reintentos. Los 46
   tests previos de `data.ai.image` siguen verdes **sin cambiar aserciones** (el delta es aditivo).
7. **Scenario 7 — `reasonFor` pura y total.** Cubre los seis motivos, el residual `HTTP_ERROR`
   (`400/404/408/301/100/600`), `2xx` con/sin bytes (`OK`/`INVALID` en `200/201/204/299`) y el caso
   negativo (`402 != AUTH`, `500 != CUOTA`, `400 != SERVER`, `200` con y sin bytes difieren). La
   función es un `when` sin `else` ambiguo (el residual es explícito) y no toca red, Android ni estado.
8. **Scenario 8 — manual (la persona).** **No** re-ejecutado: requiere el emulador de Android Studio en
   Windows con `devexpert.apiKey` real (el `adb` de WSL no lo ve). Queda anotado como pendiente manual
   explícito en `PROGRESS.md`/`feature_list.json`; la spec lo declara no automatizable desde WSL y la
   verificación exigida por la feature es JVM (`MockEngine`), que sí se re-ejecutó.

## Hallazgos

- **Critical / High / Medium: ninguno.**
- **Low 1 (informativo, preexistente en el ADR)** — `docs/adr/0005-ai-dish-images.md` (~línea 133) deja
  la frase "y **nadie lo consume todavía**" junto a la nueva mención de "diagnóstico local aditivo";
  el adaptador **sí** se consume (`MainActivity` lo construye dentro de `GatedDishImageGenerator` →
  `DishImageResolver`). La frase es **preexistente** (idéntica en `HEAD`) y el encargo de la spec era
  actualizar solo las dos menciones "sin `Log`". No bloquea.
- **Low 2 (informativo)** — `onEvent` se invoca **sin** `try/catch`; un consumidor que lanzara rompería
  el contrato "`null` sin excepción" del adaptador. Hoy el consumidor es `Log.d` (no lanza) y el
  `onEvent` del chat hace lo mismo; comportamiento aceptado por la spec ("observación pasiva"). No
  bloquea.
- **Low 3 (informativo)** — la línea de log **no** está tras `BuildConfig.DEBUG`; es una decisión
  explícita de la spec (equiparada al `SazonAi` del chat) y el evento no lleva clave, `Authorization`,
  `prompt` ni cuerpo. No bloquea.
- **Low 4 (informativo)** — la spec estimaba "4 suites / 22 tests" en `data.ai.image`; la baseline real
  del `HEAD` es **6 suites / 46 tests** (el planner no ejecutó la tirada). El implementer lo detectó y
  lo anotó en `PROGRESS.md`/`feature_list.json`; ninguna aserción se debilitó. No bloquea.

## Seguridad (checklist feature-scoped)

- **Sin secretos en el diff**: solo la clave ficticia `test-key` en tests; `local.properties` sigue
  ignorado; el log no imprime la clave (`"${event.call} ${event.reason} status=… ms bytes=… ${event.detail}"`).
- **No fuga de credenciales ni de payload**: el tipo de evento no tiene campo para clave,
  `Authorization`, `prompt` ni cuerpo; `detail` es determinista y derivado solo del estado HTTP; hay
  test de no-fuga sobre `toString()` de varios caminos.
- **Sin telemetría ni persistencia**: el evento es local y de solo lectura en Logcat; no se escribe en
  disco ni en Room, no se envía a red.
- **Sin cambios en el borde de seguridad**: la cabecera `Authorization` sigue construyéndose igual
  (`"Bearer $apiKey"`), los timeouts y `MAX_ATTEMPTS = 1` intactos, sin nuevas URLs ni entradas no
  confiables procesadas (el `detail` no interpola el cuerpo).
- **Superficie nueva**: 1 línea de `Log.d` en `MainActivity` (mismo patrón ya aceptado del chat).
  **Ningún hallazgo de seguridad relevante; sin bloqueos.**

## Documentación / harness

- `docs/technical-discovery.md` (**Observability**): nueva viñeta con la costura `DishImageCallEvent`,
  el mapeo de motivos, el `Log.d("SazonDishImage", …)` y la ausencia de clave/`Authorization`/prompt/
  cuerpo — coincide con lo implementado.
- `docs/adr/0005-ai-dish-images.md`: las dos menciones "sin `Log`" (~132 y ~257) actualizadas a
  "transporte + códec puros con diagnóstico local aditivo"; el contrato `null`/`MAX_ATTEMPTS = 1`/
  respaldo por estado **no** cambia (verificado en el código).
- `docs/specs/dish-image-generator.md`: decisión 7 anotada como atendida por esta correctiva, sin
  reescribir sus escenarios/valores históricos.
- `feature_list.json` (estado `passing` + evidencia) y `PROGRESS.md` (sesión 142) alineados con lo
  re-verificado; **no** se crean docs ni capturas innecesarias (no es UI; `docs/design/**` y `DESIGN.md`
  sin diff). `init.sh` sin cambios y sigue ejecutando el gate real (test + lint).

## Acción siguiente

El **orquestador** persiste el `accept` (estado `accepted` en `feature_list.json`, evidencia y registro
en `PROGRESS.md`) y crea el **commit local** de `feature/dish-image-call-diagnostics`. La
**publicación** (push + PR) la hace `github-flow`; esta sesión **no** hace push ni PR. Queda como
pendiente **manual** de la persona la prueba en el emulador (una línea `SazonDishImage` por llamada con
llamada/motivo/estado/duración/bytes y `CUOTA status=402|429` ante cuota agotada; ninguna línea en
aciertos de caché/semilla), según la regla del repositorio de no automatizar UI/emulador.
