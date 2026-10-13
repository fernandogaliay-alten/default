# Validación independiente — `dish-style-reference`

- Feature id: `dish-style-reference`
- Spec: `docs/specs/dish-style-reference.md`
- Rama/worktree: `feature/dish-style-reference` (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-style-reference`)
- Rol: `validator` (contexto limpio; no implementa ni acepta en ficheros)
- Fecha: 2026-10-13
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 095)
- Veredicto: **accept**

## Alcance validado

Fase 6 (variante C) sobre `data.ai.image`: cuando el adaptador recibe una referencia de estilo, pide
`POST /images/edits` (multipart, alias `image-edit`) con esa referencia y degrada a
`POST /images/generations` (texto→imagen de la fase 2) **solo** ante rechazo definitivo
(`400/404/405/413/415/422`) o carga `2xx` inutilizable. La referencia es el asset versionado
`img/recipes/default.<ext>` (nunca subida del usuario). `area: integration`, sin UI; el gate de capturas
`docs/design/concepts/<feature-id>/` no aplica.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0**. Ejecuta `./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`; no arranca procesos largos; no toca `init.sh`. (En la primera pasada las tareas de test salieron `UP-TO-DATE` por no haber cambios desde la sesión del implementer; por eso se forzó el rerun.) |
| Tests (forzado) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (4m 09s; 34 tasks ejecutadas). |
| Lint (forzado) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (3m 14s); SARIF regenerado. |
| Parseo XML propio | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **88 suites / 643 tests / 0 fallos / 0 errores / 0 saltados**. |
| Parseo SARIF propio | `app/build/reports/lint-results-debug.sarif` | **0 errores**; 13 warnings preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**. |

Salida XML de `data.ai.image`: `AndroidDishImageReducerTest` 5, `AssetDishImageStyleReferenceTest` 2,
`DevExpertDishImageGeneratorTest` 20, `DishImagePayloadTest` 3, `DishImagePolicyTest` 9,
`DishImageStyleReferenceTest` 7 → **46 tests** focales, 0 fallos.

### Baseline y delta re-derivados (independientes)

Recuento de `@Test` en las versiones de `HEAD` frente al árbol de trabajo:

- `DishImagePolicyTest.kt`: HEAD 7 → 9 (`+2`).
- `DevExpertDishImageGeneratorTest.kt`: HEAD 7 → 20 (`+13`).
- `DishImageStyleReferenceTest.kt`: nueva, 7.
- `AssetDishImageStyleReferenceTest.kt`: nueva, 2.

Delta = **+2 suites / +24 tests** → baseline **86 suites / 619 tests** (coincide con lo declarado). Ningún
otro fichero de test cambió (`git status -- app/src/test` solo lista estos cuatro).

Nota: los mensajes `Failed to create image decoder with message 'unimplemented'` en la salida de Gradle
son ruido esperado de Robolectric/`BitmapFactory` bajo test; no son fallos (643/643 verdes).

## Escenarios de la spec (13/13)

| # | Escenario | Evidencia | Estado |
|---|---|---|---|
| 1 | Sin referencia (o interruptor apagado o bytes vacíos) = contrato fase 2, 1 petición JSON a `/images/generations` (`model=image`, `prompt`, `size=1024x1024`) | `DevExpertDishImageGeneratorTest.sin_referencia_utilizable_el_contrato_de_la_fase_2_no_cambia` | OK |
| 2 | Con referencia = 1 `POST` multipart a `/images/edits` (`Bearer`, `Content-Type` multipart con boundary; partes `model=image-edit`, `prompt` idéntico, `size=1024x1024`, `image` con `name`/`filename`/`Content-Type` y bytes byte a byte; sin `cacheKey`/`recipeId`) | `...con_referencia_hace_un_unico_post_multipart_a_edits_con_las_cuatro_partes` | OK |
| 3 | Rechazo definitivo (`400/404/405/413/415/422`) → exactamente 2 peticiones (edits + generations) con el mismo prompt | `...un_rechazo_definitivo_del_ajuste_degrada_a_generations` | OK |
| 4 | `200` inutilizable (`no JSON`, `{}`, `data:[]`, `b64_json` vacío/base64 inválido; o reductor `null`/vacío) → 2 peticiones; si el respaldo falla, `null` sin lanzar | `...un_200_inutilizable...`, `...un_200_que_el_reductor_no_puede_reducir...`, `...si_el_ajuste_y_el_respaldo_fallan_devuelve_null_sin_lanzar` | OK |
| 5 | Transporte/timeout del ajuste (`SocketTimeoutException`, `HttpRequestTimeoutException`, `IOException` o timeout inyectado) → `null` sin lanzar, 1 petición | `...un_fallo_de_transporte_o_timeout_del_ajuste_devuelve_null_sin_respaldo`, `...una_respuesta_lenta_del_ajuste_excede_el_timeout_y_no_respalda` | OK |
| 6 | `401/403/429/500` (y `408`) → `null`, 1 petición, sin respaldo | `...los_estados_que_no_son_rechazo_devuelven_null_sin_respaldo` | OK |
| 7 | Sin encadenar ni reintentar: `404` en edits + `500` en generations = 2 peticiones, `null`, 1 solo intento a edits | `...el_respaldo_nunca_encadena_ni_reintenta` | OK |
| 8 | `apiKey` en blanco + referencia = 0 peticiones | `...sin_clave_con_referencia_no_hay_ninguna_peticion` | OK |
| 9 | `CancellationException` relanzada en la fase de ajuste y en el respaldo | `...la_cancelacion_se_propaga_en_la_fase_de_ajuste`, `...la_cancelacion_se_propaga_en_el_respaldo` | OK |
| 10 | Referencia = asset versionado `img/recipes/default.<ext>` (extensiones de `RecipePhotoAssets.EXTENSIONS` en orden; `mediaType`/`fileName`; `null` sin lanzar si falta/ilegible/vacío; sin galería/cámara/permisos/persistencia) | `DishImageStyleReferenceTest` (7) + `AssetDishImageStyleReferenceTest` (2, Robolectric lee `default.jpg` real) | OK |
| 11 | Política pura en un único sitio: `editsUrl()`, `EDIT_MODEL_ALIAS=image-edit`, `STYLE_REFERENCE_ENABLED`, `editFailureRequiresTextFallback` solo `400/404/405/413/415/422` | `DishImagePolicyTest` (2 tests) | OK |
| 12 | Prompt/clave/política de fases 1/5 sin cambios (`VERSION` = `dish-image-v1`, prompt = `request.prompt`, WebP ≤960 q80) | Inspección: `git status` sin `domain/image/**`; `DishImageCacheKey.VERSION` intacto | OK |
| 13 | Cableado `MainActivity` pasa `styleReference` del asset; `null` = fase 2 exacta; sin UI ni cambio de `DishImageSettings`/tope | `git diff MainActivity.kt` (1 import + carga best-effort + 1 argumento nombrado) | OK |

## Pureza y alcance (inspección directa)

- `import android` en `data.ai.image`: solo `AndroidDishImageReducer.kt` (`android.graphics`) y
  `AssetDishImageStyleReference.kt` (`android.content.Context`). OK.
- `grep Log. / filesDir / getDatabasePath`: 0 usos reales en `data.ai.image`. OK (transporte mudo).
- `docs/design/concepts/dish-style-reference`: **no existe** (gate de UI no aplica). OK.
- Sin diff en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
  `AndroidManifest.xml`, `app/src/main/assets/**`, `domain/image/**`, `data/image/**`, `ui/**`,
  `RecipePhotoAssets.kt`. **Sin dependencias nuevas** (multipart en `ktor-client-core`). OK.
- Búsqueda de patrones de subida de usuario (`READ_MEDIA_*`, `CAMERA`, `GetContent`, `ActivityResult`,
  `takePicture`) en el código nuevo: 0. OK.
- Ficheros del diff = exactamente los 4 `main` previstos + `MainActivity` + 2 tests ampliados + 2 tests
  nuevos + 3 docs durables + `feature_list.json` + `PROGRESS.md` + la spec. OK.

## Seguridad (checklist feature-scoped)

- **Secretos**: ninguno en el diff; la clave solo llega por constructor y viaja en `Authorization`; el
  test usa `test-key` ficticia. Los assets de referencia son contenido versionado intencional (no un
  secreto). Sin hallazgos.
- **Entrada/injection**: el `prompt` viaja como campo multipart (Ktor lo codifica); `fileName`/`mediaType`
  se derivan de la extensión del asset resuelto (conjunto fijo `webp/jpg/png`), no de datos del usuario;
  el endpoint se construye desde `AiProviderPolicy.BASE_URL` (constante). Sin hallazgos.
- **Subida de usuario / privacidad**: la referencia es un asset del APK; no hay permisos, galería,
  cámara ni persistencia nueva. Sin hallazgos.
- **Exposición de datos**: el multipart lleva solo `model`/`prompt`/`size`/`image`. Sin hallazgos.
- **Dependencias**: sin cambios en build/catálogo. Sin hallazgos.
- **Abuso/cuota**: el respaldo está acotado (≤2 llamadas) y es reversible con
  `STYLE_REFERENCE_ENABLED`; el gasto `<2 %` por imagen se documenta en la fila de riesgo. Sin hallazgos.

Evaluación: **sin hallazgos de seguridad relevantes** para este slice.

## Documentación y harness

- `docs/adr/0005-ai-dish-images.md` → fases **1-6**, con el alcance de la fase 6 y la consecuencia de
  gasto (≤2 llamadas, una desechada) y el no-incremento de `VERSION`. Coherente.
- `docs/technical-discovery.md` → nueva frontera durable del adaptador (`/images/edits` multipart +
  respaldo), referencia como asset versionado, cableado en `MainActivity` y matiz del presupuesto.
  Coherente.
- `docs/risks-and-open-questions.md` → fila de cuota/latencia y lista de ADRs actualizadas a fases 1-6.
  Coherente.
- `docs/domain-model.md`, `docs/domain/taxonomy.md`, `AGENTS.md`, `DESIGN.md`,
  `docs/ai-dish-images-proposal.md`, `docs/design/**`: correctamente **no tocados** (impacto declarado
  "not needed").
- `init.sh`: cumple la regla del gate (ejecuta test+lint vía `scripts/gradle.sh`, `exit 0`, sin procesos
  largos, sin modificar el script).
- `feature_list.json` (`passing` + evidencia) y `PROGRESS.md` (sesión 095) reflejan el estado real y el
  siguiente paso (validación independiente).

## Estado de publicación

- `HEAD` = `51e39f5` (merge en `main`), **0 commits por delante de `origin/main`**.
- Rama `feature/dish-style-reference` sin upstream; **no existe** rama remota; **no hay PR**.
- El implementer **no** hizo commit, push ni PR. Correcto (lo orquesta `github-flow` al `accept`).

## Hallazgos

Sin hallazgos **Critical / High / Medium**.

### Finding (Low, informativo — no bloquea): imprecisión del grep de alcance en la spec

- Severidad: Low
- Evidencia: la Verification Plan y el Validator Checklist de `docs/specs/dish-style-reference.md`
  esperan `grep -rn "Recipe|Taxonomy|DishImagePrompt|DishImageCacheKey" ... → 0`, pero la propia spec
  (Expected File Changes y Technical Approach §3) **manda** que `DishImageStyleReference.kt` reutilice
  `RecipePhotoAssets` (`ASSET_BASE = "${RecipePhotoAssets.ROOT}/..."`, `EXTENSIONS = RecipePhotoAssets.EXTENSIONS`).
  Hay 4 coincidencias de `Recipe`, todas de `RecipePhotoAssets`; **0** de `Taxonomy`,
  `DishImagePrompt` y `DishImageCacheKey`.
- Por qué importa: la expectativa "0" es contradictoria con el diseño y puede inducir a futuras
  sesiones a "corregir" una reutilización correcta. No es un defecto de la implementación: es una
  imprecisión de redacción de la spec (ya señalada por el implementer).
- Cambio requerido: ninguno en código. Opcional: precisar el grep de la spec a
  `DishImagePrompt|DishImageCacheKey|Taxonomy|domain.Recipe` (excluyendo `RecipePhotoAssets`).
- Verificación tras el cambio: no aplica (solo redacción).

## Resumen de seguridad

Sin hallazgos relevantes (checklist feature-scoped sin notas ni bloqueos).

## Siguiente acción requerida

- **aceptar**: el orquestador debe persistir el estado `accepted` en `feature_list.json` (el validador no
  lo hace) y proceder al **commit local** de la rama `feature/dish-style-reference`; `github-flow`
  publicará (push + PR con `Closes #<n>`). No hay trabajo de reparación pendiente.
- No revalidar salvo petición explícita.
