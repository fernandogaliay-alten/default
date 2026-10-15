# Validación independiente — `dish-image-settings-persistence`

- **Feature**: `dish-image-settings-persistence` (`area: ui`, correctiva del ajuste de imágenes de plato; el efecto visible es el interruptor del Perfil, el cambio técnico vive en `data.image` + el punto de composición)
- **Spec**: `docs/specs/dish-image-settings-persistence.md`
- **Rama / worktree**: `feature/dish-image-settings-persistence`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-settings-persistence`
- **Sesiones**: 128 (implementer, `passing`) y 129 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, trabajo en disco; `HEAD = 89ed694`, **sin commit, sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, **34/34 tareas ejecutadas**; parseo propio de los XML frescos: **100 suites / 756 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | exit 0; SARIF: **0 errores, 13 warnings** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**; sin `ApplySharedPref` ni `UseKtx` |
| Baseline re-derivada | `git grep -c '@Test'` en `HEAD 89ed694` | `HEAD`: **98 ficheros / 739 anotaciones** ⇒ delta exacto **+2 suites / +17 tests** (`DishImageSettingsPersistenceTest` 13, `AndroidDishImageSettingsStoreTest` 4), **0 suites modificadas** |
| Alcance | `git status --porcelain` + `git diff --name-only HEAD` | solo los previstos: 2 `main` nuevos (`data/image/{DishImageSettingsStore,AndroidDishImageSettingsStore}.kt`), 2 `main` modificados (`data/image/DishImageSettings.kt`, `MainActivity.kt`), 2 tests nuevos, 4 docs (`docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`, `DESIGN.md`, `docs/risks-and-open-questions.md`), `feature_list.json`, `PROGRESS.md` y la spec (untracked); **sin diff** en `ui/**`, `domain/**`, ficheros aceptados de `data/image`/`data/ai/image`, `DishImageGenerationBudget.kt`, `DishImageSettingsCopy.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `res/xml/**`, `init.sh`, `scripts/**` |
| Pureza | `grep -E '^import'` + grep de `android\.`/`androidx\.` | `DishImageSettingsStore.kt` **0 imports**; `AndroidDishImageSettingsStore.kt` es el único con Android (`android.content.Context` + extensión KTX `androidx.core.content.edit`) |
| Constantes | `grep -rn DEFAULT_ENABLED\|MAX_GENERATIONS_PER_SESSION app/src/main` | `DEFAULT_ENABLED = false` y `MAX_GENERATIONS_PER_SESSION = 12` intactos |
| Sin escritura bloqueante | `grep -rn 'commit()' app/src/main` | **0**; el adaptador usa `prefs.edit { putBoolean(...) }` → `apply()` |
| Gate de capturas | `test -d docs/design/concepts/dish-image-settings-persistence` | carpeta **no existe** (desviación autorizada, misma clase que #77/#85/#86/#91/#98/#101) |
| No publicación | `git status -sb`, refs, `git rev-list` | sin upstream/rama remota, **0 commits** por delante; el implementer **no** hizo commit/push/PR |
| Harness | `git diff` de `feature_list.json` | `verification` **intacto**; solo `status` (`passing`) + `evidence`; JSON válido, **sin** `in_progress` |

## Escenarios verificados (contra el código y los tests reales)

1. **Encendido sobrevive y pide imágenes (Scenarios 1, verificación exigida 1 y 2)**: `DishImageSettings.restored(store)` lee el bit; con `true` guardado el estado es `(enabled=true, used=0, max=12)` y `generatedIfEnabled { … }` invoca al productor **1** vez (`el_arranque_con_estado_persistido_encendido_pide_imagenes`).
2. **Apagado sobrevive y no pide nada (Scenario 2)**: con `false` guardado, `enabled == false`, el productor se invoca **0** veces y `tryAcquireGeneration()` devuelve `false` sin consumir tope (no se lee caché, ni semilla, ni API) (`el_arranque_con_estado_persistido_apagado_no_pide_imagenes`, `el_arranque_sin_estado_guardado_no_pide_imagenes`).
3. **Sin estado guardado → default de la política (Scenario 3)**: store vacío → `(false, 0, 12)` y **0** escrituras (`sin_estado_guardado_se_usa_el_default_de_la_politica_y_no_se_escribe`).
4. **Guardar y releer (Scenario 4, verificación exigida 1)**: `setEnabled(true)` → una segunda instancia sobre el mismo store lee `true`; `setEnabled(false)` → `false`; gana siempre el último valor (`guardar_encendido_*`, `guardar_apagado_*`, `el_valor_releido_es_siempre_el_ultimo`).
5. **El tope no se persiste (Scenario 5, verificación exigida 3)**: con `true` guardado y una sesión previa con consumo, la sesión nueva arranca en `used=0`, `max=12`, `remaining=12`, `exhausted=false` (`el_tope_no_se_persiste_y_arranca_en_12`, `la_sesion_nueva_no_hereda_el_consumo_*`); la costura solo expone el booleano por construcción (garantía estructural).
6. **Solo el interruptor se escribe, y solo al moverlo (Scenario 6)**: `setEnabled` = 1 escritura; `tryAcquireGeneration()` = **0** escrituras (`solo_el_interruptor_se_escribe_y_solo_al_moverlo`).
7. **Degradación silenciosa (Scenario 7)**: `loadEnabled()` que lanza → default apagado sin crash (`un_store_que_lanza_al_leer_degrada_al_default_apagado`); `saveEnabled()` que lanza → el estado en memoria cambia igual y la excepción no llega a la UI (`un_store_que_lanza_al_guardar_deja_la_sesion_funcionando`); valor corrupto → `null` (`AndroidDishImageSettingsStoreTest.un_valor_de_tipo_distinto_se_degrada_a_null`, `un_store_virgen_devuelve_null`).
8. **Alcance, pureza y semántica intacta (Scenario 8)**: verificado por inspección (tabla de alcance/pureza); `DishImageSettingsState`, `tryAcquireGeneration`, `generatedIfEnabled`, `GatedDishImageGenerator` y la política sin diff; sin dependencias nuevas.

## Non-goals respetados

No se persiste el tope (solo un booleano); no se toca `UserProfile`/`Room`/`PreferenceSignature` (no fuerza refresco del Lote); no se toca la cadena de resolución ni el gate (apagado = `null` sin ler caché/semilla/API); no se toca la UI, el microcopy ni `ui/theme/**`; `DEFAULT_ENABLED = false` sigue igual (primer arranque apagado); sin dependencias nuevas (nada de DataStore ni Room nuevo); no se toca build/lockfile/`AndroidManifest.xml`/`res/xml`/`init.sh`/`scripts/**`; no se tocan las specs aceptadas ni se crea carpeta de capturas.

## Decisiones del orquestador respetadas

- **Desviación de capturas autorizada**: `docs/design/concepts/dish-image-settings-persistence/` **no** existe y `DESIGN.md` registra la excepción con el mismo formato que #77/#85/#86/#91/#98/#101.
- **`verification` de `feature_list.json` intacto**: el diff solo añade `status`, `evidence` y `notes`.
- **Sin commit/push/PR**: `HEAD` == `89ed694`, sin rama remota y sin PR; el commit y la publicación son del orquestador (`feature-flow`/`github-flow`).

## Documentación durable

`docs/technical-discovery.md` (límite de almacenamiento: el flag del interruptor con `SharedPreferences` tras una costura pura; cadena en runtime: arranque restaurado y tope por sesión), `docs/adr/0005-ai-dish-images.md` (el interruptor se persiste con un bit; el tope sigue por sesión; la Decisión 2 de `dish-image-settings-toggle` queda superada en esa parte), `DESIGN.md` (primer arranque apagado / después se recuerda la última elección) y `docs/risks-and-open-questions.md` (el opt-in se recuerda; el tope, no) están actualizados y coherentes entre sí y con el código. `docs/domain-model.md`, `AGENTS.md` y `CONTEXT.md` correctamente **no** tocados.

## Evaluación de seguridad

Sin hallazgos bloqueantes ni de riesgo alto. Un único booleano no sensible almacenado con `MODE_PRIVATE`; sin secretos ni credenciales nuevas; sin permisos ni superficie de red nuevos (no hay llamadas externas); sin nueva entrada de datos más allá del interruptor local. Doble `runCatching` en lectura y escritura degrada en silencio (ADR 0005) y, al no haber suspensión, no se traga `CancellationException`. Se usa `apply()`, nunca `commit()`. El impacto de backup/restauración de Android (`allowBackup="true"`) es benigno y ya está documentado (no justifica tocar el manifiesto).

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (informativos, no bloquean la aceptación):**

1. El adaptador usa la extensión KTX `androidx.core.content.edit` en vez del pseudocódigo `prefs.edit().putBoolean(...).apply()` de la spec: es funcionalmente idéntico (`commit=false` → `apply()`), estrictamente mejor (evita el warning nuevo `UseKtx` bajo `app/src` que disparaba la primera versión) y la desviación está documentada en `PROGRESS.md` y en la evidencia de la feature.
2. `docs/risks-and-open-questions.md` recorta una frase ("un **set semilla** de 6 platos…") a mitad de oración en la fila editada: retoque cosmético, sin cambio de significado.

## Verificación del handoff del implementer

La evidencia del implementer es **real y exacta**: re-ejecuté el gate y, al salir `UP-TO-DATE`, forcé test y lint; el parseo propio de los XML y del SARIF coincide con lo registrado (100/756/0/0/0; +2 suites/+17 tests sobre 98/739 de `HEAD 89ed694`; 0 errores y 13 warnings preexistentes, 0 bajo `app/src`). El alcance, la pureza, las constantes (`DEFAULT_ENABLED=false`, `MAX_GENERATIONS_PER_SESSION=12`), la ausencia de `commit()`, la carpeta de capturas inexistente y la rama sin publicar se confirman. Nada del resumen del implementer resultó inflado.

## Veredicto

**`accept`.** La feature cumple la spec (Scenarios 1-8 y Validator Checklist verdes con evidencia re-ejecutada), la desviación de capturas está autorizada y registrada, el alcance y la pureza se respetan, la documentación durable es coherente y el repositorio es reiniciable por el camino estándar (`bash init.sh` exit 0). Sin commit/push/PR. Estado recomendado: **`accepted`** (lo persiste el orquestador).
