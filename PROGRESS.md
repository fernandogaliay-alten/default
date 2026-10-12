# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `/mnt/c/Users/fernando.galiay/Documents/Default-Project` (WSL2) =
  `C:\Users\fernando.galiay\Documents\Default-Project` (Windows)
- Camino estándar de arranque: **`bash init.sh` en WSL** (nativo; ya no hace falta Git Bash). El
  entorno de Linux (JDK 21, SDK de Android, `gh`) lo publica `scripts/wsl-env.sh`, que cargan
  `init.sh`, `scripts/gradle.sh` y `scripts/new-worktree.sh`; `scripts/wsl-bootstrap.sh` lo instala
  desde cero, sin `sudo`. Ver Sesión 059.
- Camino estándar de verificación: `./gradlew test` y `./gradlew lint`, ambos vía `init.sh` (que
  enruta la salida por `scripts/gradle.sh` para no colgar la shell del agente; ver Sesión 056)
- Flujo de entrega: `github-flow` (worktrees + issues/PR) sobre `feature-flow`; rama base `main` y
  remoto `origin` → `https://github.com/fernandogaliay-alten/default.git` (privado)
- Últimas features aceptadas: `fake-recipe-generator`, `recipe-validator`, `prompt-builder`,
  `seed-recipe-catalog`, `design-tokens-and-theme`, `onboarding-shell-and-identity`,
  `onboarding-diet-style-cuisines`,   `onboarding-ingredients-safety-kitchen`, `suggestion-batch-cache`,
  `home-offline-blocks`, `local-result-filters`, `generator-ingredients-step`, `generator-settings-step`,
  `profile-screen`, `generation-flow-with-validation`, `manual-suggestions-refresh`, `results-screen`,
  `recipe-detail-screen`, `favorites`, `activity-summary`, `ai-search` y `real-ai-provider` (validador independiente, veredicto `accept`). El onboarding queda **completo**
  (7 pasos) y persiste el Perfil al cerrar; el Generador ya ejecuta el corte vertical principal del MVP
  (generar -> validar -> mostrar solo aceptadas, con carga cancelable) y monta la pantalla de Resultados
  real (fórmula de `matchScore`, estados vacío/error con "Reintentar", subtítulo de búsqueda); la pestaña
  Perfil es real (identidad + preferencias editables reutilizando los editores del onboarding) y permite
  forzar el refresco del Lote, además del nuevo Resumen de actividad (recetas generadas persistidas +
  favoritas derivadas); el Detalle de receta ya muestra ingredientes con cantidades, ajusta las
  raciones (escalado con redondeo por unidad), recorre los pasos y consulta tips/nutrición opcional; la
  pestaña Favoritos es real (guardar/quitar, persistencia local de una copia de la Receta y consulta sin
  conexión, con estados poblado/vacío). El proveedor de IA real ya está conectado tras el puerto
  `RecipeGenerator` (DevExpert Inference, Ktor + OkHttp aislado en `data.ai`, política de
  timeouts/reintentos y degradación a modo semilla), con lo que el corte vertical principal del MVP queda
  completo de punta a punta.
- Siguiente feature en orden de lista: **ninguna**; `real-ai-provider` fue la última feature del plan y
  quedó `accepted` en la sesión 067 (validación independiente, rama `feature/real-ai-provider`). El commit
  local lo hace el orquestador de `feature-flow` y la publicación queda para `github-flow`. Después se
  añadió la **correctiva** `ai-provider-timeouts` (issue #68), que quedó `accepted` en la sesión 069
  (validación independiente, veredicto `accept`, rama `feature/ai-provider-timeouts`, sesión 068 la había
  dejado `passing` implementada y autoverificada). No hay features `not_started` ni `in_progress`. El plan
  de features del MVP sigue **cerrado**. Después se añadió una segunda **correctiva** de
  `real-ai-provider`, `ai-recipe-decode` (issue #70): implementada y autoverificada (`passing`) en la
  sesión 070 y **`accepted`** en la sesión 071 (validación independiente, veredicto `accept`, rama
  `feature/ai-recipe-decode`). El adaptador real ya decodifica la **forma real** del contrato §4 (sin
  `id` ni `source`), completa `id` (`ai-<sha256>` determinista) y `source = ai` antes de aplicar
  `RecipeValidator.validateRecipe`; `Recipe.kt`/`RecipeValidator.kt` intactos. No hay features
  `not_started`, `in_progress` ni `passing`: la tercera **correctiva** de `real-ai-provider`,
  `ai-call-diagnostics` (issue #72), quedó **`accepted`** en la sesión 073 (validación independiente,
  veredicto `accept`, rama `feature/ai-call-diagnostics`). El plan del MVP sigue **cerrado**.
- Capturas de UI (política del PR #37): **backfill pendiente**; 11 features de UI `accepted` y 18
  pantallas por exportar de Stitch (ver sesión 046 y `docs/design/concepts/<feature-id>/README.md`).
  `results-screen`, `recipe-detail-screen`, `favorites`, `activity-summary` y `ai-search` ya aportan sus
  capturas (`docs/design/concepts/results-screen/resultados/`,
  `docs/design/concepts/recipe-detail-screen/detalle-receta/`,
  `docs/design/concepts/favorites/favoritos/`,
  `docs/design/concepts/activity-summary/perfil-resumen/` y
  `docs/design/concepts/ai-search/busqueda-ia/`).
- Capturas de UI — cambio de política del harness (2026-10-11, sesión 055): en `github-flow`, una
  feature de UI sin capturas ya **no** bloquea sin crear nada ni ensucia el checkout base. `execute`
  **provisiona** la feature dentro de su propio worktree (issue, rama `feature/<id>` y worktree) y deja
  allí `docs/design/concepts/<feature-id>/` con un `README.md` (pantallas y ficheros esperados) y un
  `stitch-prompt.md` (prompt listo para pegar en Stitch, derivado de `docs/design/stitch/`), **sin**
  lanzar sesión. La feature queda `blocked`/`provisioned` hasta que la persona coloca las capturas en el
  worktree y re-ejecuta `github-flow execute <feature-id>`.
- Propuesta post-MVP documentada (no aceptada, no planificada, fuera del MVP):
  `docs/backend-proposal.md` — backend Kotlin + Ktor (opción B) para cuenta, sincronización
  offline-first y proxy de IA, con módulo de dominio compartido. No altera el estado de ninguna feature
  ni el MVP local-first; queda para retomar en una fase posterior.
- Última feature **`accepted`**: **`recipe-artwork`** (issue #77, `area: ui`), cerrada en la sesión 078
  con validación independiente (veredicto `accept`, rama `feature/recipe-artwork`; la sesión 077 la dejó
  `passing`). Es la primera feature del plan posterior al MVP ya cerrado. Sustituye las **4 copias** del
  placeholder de receta
  (degradado melocotón→oliva + `PlateGlyph` duplicado a mano) por un **único componente**
  `ui.artwork.RecipeArtwork` con mapeo **puro** (`RecipeArtworkStyle`: glifo por `categoryId` —18
  categorías reales—, acento del degradado por `cuisineId` —10 cocinas reales—) y **fallback total**
  (id nulo/vacío/desconocido → plato + degradado por defecto). Sin dependencias nuevas, sin assets, sin
  hex sueltos (roles del tema) y con la geometría aceptada intacta (16:10, 96x96, alto 96, hero 240;
  glifos 48/56/40/40/72 dp). La desviación de capturas está autorizada (issue #77) y registrada en
  `DESIGN.md`; **no** se crea `docs/design/concepts/recipe-artwork/`. WIP = 0. Antes:
  la cuarta **correctiva** de `real-ai-provider`, `ai-response-format` (issue #74, `area: integration`,
  no UI), quedó **`accepted`** en la sesión 076 (validación independiente, veredicto `accept`, rama
  `feature/ai-response-format`; la sesión 074 la dejó `passing` y la 075 la reparó tras un `revise`).
  Envía `response_format {"type":"json_object"}` en el alias `chat`, con un interruptor
  (`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`), un **fallback one-shot** sin `response_format` solo ante
  400/422 (respetando `MAX_ATTEMPTS = 2`) y observabilidad del modo/fallback por
  `AiCallEvent`/`Log.d("SazonAi")`; `RecipeValidator` sigue intacto como puerta dura. Antes,
  `ai-call-diagnostics` (issue #72) quedó `accepted` en la sesión 073. `ai-recipe-decode` (issue #70) en
  la 071 y `ai-provider-timeouts` (issue #68) en la 069. El resto del plan sigue cerrado.
- Cuarta **correctiva** de `real-ai-provider`, `ai-response-format` (issue #74): implementada y
  autoverificada (`passing`) en la sesión 074 por el implementer, rama `feature/ai-response-format`.
  La validación independiente devolvió **`revise`** (Finding 1 Medium: el fallback podía añadir un
  **tercer** intento HTTP) y el implementer la **reparó** en la sesión 075 (sigue `passing`,
  **pendiente de re-validación**). Acota un cambio en `data.ai` + el formato del `Log.d` de
  `MainActivity`: envía `response_format {"type":"json_object"}` en el alias `chat` (no `json_schema`,
  no `chat-pro`), con **fallback one-shot** ante `400`/`422` sin la clave **y solo si queda presupuesto
  de intentos** (nunca supera `MAX_ATTEMPTS = 2`), interruptor
  `AiProviderPolicy.RESPONSE_FORMAT_ENABLED` (por defecto `true`) con costura de constructor,
  observabilidad del modo/fallback sobre `AiCallEvent` (`responseFormat` + `FALLBACK`; `format=…` en el
  log) y `@EncodeDefault(NEVER)` para **nunca** emitir `"response_format":null` pese a
  `encodeDefaults = true`. `Recipe.kt`/`RecipeValidator.kt` intactos (siguen siendo la puerta dura).
- Quinta **correctiva** de `real-ai-provider`, `ai-verbose-diagnostics` (`area: integration`, no UI):
  implementada y autoverificada (`passing`) en la sesión 079 y **`accepted`** en la sesión 080 (validación
  independiente, veredicto `accept`, rama `feature/ai-verbose-diagnostics`). Desglosa `INCOHERENT` en sus
  dos causas del Validador (regla 4a, deriva de tiempos → `TIME_DRIFT`; 4b, cobertura de alérgenos →
  `ALLERGEN_COVERAGE`) siempre activo (`AiIncoherentCause` + `AiCallEvent.incoherentByCause`, `incoherent={…}`
  en el log, sin tocar `DiscardReason`/`RecipeValidator`) y añade un modo de depuración ocultable
  (`verboseDiagnostics`, activo solo en debug vía `BuildConfig.DEBUG`) con el detalle de cada descarte
  (`discard[i/n]`) y el JSON crudo troceado (`raw[i/n]` vía `AiVerboseDiagnostics`, ≤3000 char/trozo),
  apagado por defecto y sin exponer nunca la clave ni `Authorization`. El plan del MVP sigue **cerrado**.
- Bloqueo actual: ninguno. Queda la **verificación manual** de la persona en el emulador de Android
  Studio (Windows): con clave válida, generar/buscar y comprobar en logcat (`SazonAi`) el detalle
  enriquecido por llamada (`content=…c/…b`, `received=… accepted=…`, `discards={…}`) y el modo de
  formato (`format=JSON_OBJECT`/`NONE`, y la línea `FALLBACK` si el proveedor rechaza `json_object`);
  con un lote sin recetas aceptadas, que el log indique el motivo; con `RESPONSE_FORMAT_ENABLED=false`
  el comportamiento es idéntico al actual (sin `response_format`); con la clave ausente o inválida,
  modo semilla sin fallar. No es automatizable desde WSL (el `adb` de WSL no ve el emulador). Para
  `recipe-artwork` queda también la **revisión manual** de emulador (Home, Resultados, Favoritos
  lista+rejilla y Detalle, en claro y oscuro) del acabado de los 18 glifos y los 11 degradados, contra
  el registro de `DESIGN.md`; un defecto visual se corrige con una feature correctiva.
- Última verificación (2026-10-12, `recipe-artwork`, validator, sesión 078): **veredicto `accept`
  independiente** (agente con contexto limpio). Re-ejecutó `bash init.sh` (exit 0) y, al verlo
  `UP-TO-DATE`, forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML (**60 suites /
  413 tests / 0 fallos / 0 errores / 0 saltados**; `RecipeArtworkStyleTest` 8) y el informe de lint
  (**0 errores, 13 warnings** preexistentes, 0 bajo `app/src`). Recomputó el mapeo desde el código contra
  la Taxonomía real (18 `recipeCategories` → 18 `RecipeGlyph` distintos == `RecipeGlyph.entries`; 10
  `cuisines` → 10 `RecipeAccent` distintos + `DEFAULT` == `RecipeAccent.entries`; `else`/`other` →
  `PLATE`/`DEFAULT`), confirmó pureza de `ui.artwork`, sin hex sueltos, **0 `PlateGlyph`** en `app/src`,
  5 puntos de llamada con geometría/tamaños preservados, disciplina de alcance (domain/Taxonomía/seed/
  Room/build/`init.sh` intactos, sin dependencias) y que el implementer no hizo commit/push/PR. Sin
  hallazgos Critical/High/Medium; 2 Low informativos no bloqueantes (redacción "vertical" del degradado
  cuando `Brush.linearGradient` es diagonal, idéntico al placeholder aceptado antes; Scenario 6 solo por
  inspección, coherente con AGENTS.md). Detalle en `docs/validations/recipe-artwork.md`.
- Verificación anterior (2026-10-12, `recipe-artwork`, implementer, sesión 077): **trabajo en disco, sin
  commit**. `bash init.sh` exit 0 (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin
  procesos largos) y, antes, tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` (BUILD SUCCESSFUL);
  parseo propio de los XML: **60 suites / 413 tests / 0 fallos / 0 errores / 0 saltados** (405 previos +
  8 nuevos de `RecipeArtworkStyleTest`) y del informe de lint: **0 errores, 13 warnings preexistentes**
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo
  `app/src`**. `RecipeArtworkStyleTest` itera la Taxonomía real vía
  `System.getProperty("taxonomy.asset.path")`: 18 `recipeCategories` → 18 `RecipeGlyph` distintos
  (== `RecipeGlyph.entries`) y 10 `cuisines` → 10 `RecipeAccent` distintos (+`DEFAULT` ==
  `RecipeAccent.entries`), más determinismo, fallback total (`null`/`""`/`"  "`/desconocido →
  `PLATE`+`DEFAULT`, también con una `Recipe` de ids vacíos) y caso negativo. `grep`: **0 apariciones de
  `PlateGlyph`** en `app/src`; el degradado del placeholder (`primaryContainer` →
  `secondaryContainer`) solo vive en `RecipeArtwork.kt` (el otro `Brush.linearGradient` es el hero
  preexistente de `HomeScreen`, ajeno a la feature: finding anotado en la spec). `RecipeArtworkStyle.kt`
  solo importa `domain.recipe.Recipe` (sin Compose/Android) y `ui/artwork` no tiene hex sueltos.
  Geometría y tamaños preservados (16:10, 96x96, alto 96, hero 240; glifos 48/56/40/40/72 dp); `git
  diff --stat` solo con los ficheros previstos y sin tocar build/lockfile/dominio/Taxonomía/
  `seed-recipes.json`/Room/`ui.theme`/`init.sh`. `DESIGN.md` y `docs/technical-discovery.md`
  actualizados; **no** se crea `docs/design/concepts/recipe-artwork/` (desviación autorizada, issue #77).
- Verificación anterior (2026-10-12, `ai-response-format`, implementer, **reparación del Finding 1**):
  **trabajo en disco, sin commit**. La rama de fallback de `DevExpertRecipeGenerator` ahora exige
  `attempt < AiProviderPolicy.MAX_ATTEMPTS` y se quitó el `!usedFallback` redundante (`includeFormat`
  ya lo implica): un `503`/`IOException` en el 1.er intento (reintento de transporte) seguido de
  `400`/`422` en el 2.º ya **no** genera un 3.er intento HTTP (`calls=2`, `UNAVAILABLE/HTTP_ERROR` con
  el formato del intento, sin evento `FALLBACK`). Test de regresión nuevo
  `DevExpertRecipeGeneratorTest.el_fallback_no_anade_un_tercer_intento` (cubre `503→400` y
  `IOException→422`; los tests del fallback aislado siguen intactos). Tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` en verde (**62 tests / 0
  fallos**, +1); `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`); parseo propio de los XML:
  **59 suites / 405 tests / 0 fallos / 0 errores / 0 saltados** (404 previos + 1 nuevo) y del informe de
  lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`). KDoc del generador y dos docs durables
  (`docs/risks-and-open-questions.md`, `docs/technical-discovery.md`) matizadas para ceñir el fallback
  al presupuesto; sin cambios fuera de `DevExpertRecipeGenerator.kt`, su test y esas docs.
- Verificación anterior (2026-10-12, `ai-response-format`, implementer, sesión 074): **trabajo en disco, sin commit**.
  `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`); tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` en verde (61 tests / 0
  fallos). Parseo propio de los XML del gate: **59 suites / 404 tests / 0 fallos / 0 errores / 0
  saltados** (393 preexistentes + 11 nuevos de `data.ai`: `AiProviderPolicyTest` 12,
  `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 30, `OpenAiChatTest` 2)
  y del informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`, 0 nuevos por
  `@EncodeDefault`/`@OptIn`). `git diff --stat` solo con lo previsto; `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, Room,
  Taxonomía, `seed-recipes.json`, build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**`
  intactos; sin dependencias nuevas; sin capturas (no es UI).
- Verificación anterior (2026-10-12, `ai-call-diagnostics`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`), forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con parseo propio de los XML
  (**58 suites / 393 tests / 0 fallos/errores/saltados**; 386 preexistentes + 7 nuevos de `data.ai`:
  `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11,
  `DevExpertRecipeGeneratorTest` 23) y del informe de lint (0 errores, 10 warnings, todos en el
  build/version catalog, 0 bajo `app/src`). Reprodujo la **regresión por aritmética**: con los valores
  viejos (90/120/180) fallan `SOCKET > peor caso`, `SOCKET ≥ 2×` el peor del alias `chat` y el margen de
  reintento; con 120/150/240 las tres son ciertas. Detalle en `docs/validations/ai-call-diagnostics.md`.
- Verificación anterior (2026-10-12, `ai-recipe-decode`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`), forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con parseo propio de los XML
  (**58 suites / 386 tests / 0 fallos/errores/saltados**; 376 preexistentes + 10 nuevos de `data.ai`:
  `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8,
  `DevExpertRecipeGeneratorTest` 19) y del informe de lint (0 errores, 13 warnings preexistentes, 0 bajo
  `app/src`). Reprodujo el bug y el arreglo de forma independiente con un verificador Java fuera del
  repo (16/16): el mismo JSON literal sin `id`/`source` por el camino de `main` acepta 0 / descarta 1
  como SCHEMA, mientras `AiRecipePayload.decode` acepta 1. Detalle en
  `docs/validations/ai-recipe-decode.md`.
- Verificación anterior (2026-10-12, `ai-provider-timeouts`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`), forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con parseo propio de los XML
  (**57 suites / 376 tests / 0 fallos/errores/saltados**; 371 preexistentes + 5 nuevos de `data.ai`:
  `AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18) y del informe
  de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`). Comprobó con `javap` sobre los jars
  reales la jerarquía de excepciones y la ausencia de otro techo de timeout. Detalle en
  `docs/validations/ai-provider-timeouts.md`.
- Verificación anterior (2026-10-12, `real-ai-provider`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`),
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*" checkDebugAarMetadata`,
  con parseo propio de los XML (**57 suites / 371 tests / 0 fallos/errores/saltados**; 28 nuevos de
  `data.ai`: `AiProviderPolicyTest` 8, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 15) y del
  informe de lint (0 errores, 13 warnings preexistentes, 0 en ficheros de la feature). Detalle en
  `docs/validations/real-ai-provider.md`.

## Registro de sesiones

> Las sesiones **001–045** están archivadas en [`docs/progress/sessions-001-045.md`](docs/progress/sessions-001-045.md).
> Aquí se conserva el estado actual y las sesiones recientes (046–…).

### Sesión 046

- Fecha: 2026-10-09
- Objetivo: **chore de harness** (no es feature de producto): **Fase 1** del backfill de capturas de UI
  exigidas por la política del PR #37, aplicada a las 11 features de `area: ui` ya `accepted` (que se
  implementaron antes de que existiera la política). Rama `chore/ui-captures-backfill`.
- Completado:
  - Convención de guardado actualizada en `docs/design/concepts/README.md`: **una subcarpeta por
    pantalla** (no por componente) con los **tres ficheros del export de Stitch** (`.png`, `.html` y
    `design.md`, con los nombres que dé Stitch).
  - **11 `docs/design/concepts/<feature-id>/README.md`** nuevos (la "petición" de cada feature) con la
    lista exacta de pantallas esperadas y una línea de qué es cada una: `design-tokens-and-theme`
    (`design-system`); `onboarding-shell-and-identity` (`bienvenida`);
    `onboarding-diet-style-cuisines` (`dieta`, `estilo`, `cocinas`);
    `onboarding-ingredients-safety-kitchen` (`ingredientes`, `alergias`, `tu-cocina`);
    `home-offline-blocks` (`home`, `home-sin-conexion`); `local-result-filters`
    (`filtros-tus-recetas`); `generator-ingredients-step` (`generador-ingredientes`);
    `generator-settings-step` (`generador-ajustes`); `generation-flow-with-validation`
    (`generador-cargando`, `resultados-preview`); `profile-screen` (`perfil`, `perfil-editar`);
    `manual-suggestions-refresh` (`perfil-sugerencias`). Total: **18 pantallas**.
  - `DESIGN.md`: sección "Generated Concept Images" registra el backfill y las pantallas por feature
    (la petición concreta vive en cada `README.md`).
  - **11 specs**: la sección "Visual Design Impact" nombra sus capturas y el impacto en
    `docs/design/concepts/` pasa de `not needed` a `update` (backfill).
- Verificación ejecutada: cambio **solo de documentación** (ningún fichero Kotlin/recursos/gradle), por
  lo que el gate `init.sh` no aplica; revisión manual del árbol `docs/design/concepts/`, de los 11
  `README.md` y de `git status` (24 ficheros, +358/−40).
- Ficheros creados/cambiados: `docs/design/concepts/README.md`, los 11
  `docs/design/concepts/<feature-id>/README.md` (nuevos), `DESIGN.md`, los 11 `docs/specs/<feature>.md`
  y este registro. `feature_list.json` **sin cambios** (ninguna feature cambia de estado).
- Hallazgos relevantes: las 11 features de UI aceptadas no tienen capturas porque son anteriores al
  PR #37; sus specs incluso decían `docs/design/concepts/: not needed`. El backfill no reabre features
  aceptadas: solo deja la petición y, cuando lleguen los exports, se hará la **Fase 2** de
  reconciliación (UI implementada vs captura) **una a una**, abriendo una feature correctiva solo si
  aparece una desviación visual real.
- Riesgo o incógnita abierta: la Fase 2 no puede empezar hasta que la persona exporte las 18 pantallas
  desde Stitch; no hay bloqueo para el resto del plan (`results-screen` sigue siendo la siguiente
  feature de producto).
- Siguiente paso: commit + push + PR del chore (publicación por `github-flow`). Después, **Fase 2**:
  reconciliar feature por feature según lleguen los exports.

### Sesión 047

- Fecha: 2026-10-09
- Objetivo: **chore de harness**: trocear el prompt de Stitch **por pantalla** para que Stitch no lo
  trunque y genere todas las pantallas (el prompt completo `docs/design/stitch-prompt.md` se truncaba).
- Completado: nueva carpeta `docs/design/stitch/` con `README.md` (guía de uso + mapa parte → carpeta de
  `docs/design/concepts/`) y **21 partes** listas para copiar/pegar de una en una en la misma
  conversación de Stitch: `00-contexto.md` (producto, lenguaje visual, navegación, fuera de alcance y
  calidad, se pega primero), `01-design-system`, 7 pasos de onboarding, `home`, `home-sin-conexion`,
  `home-busqueda-ia`, generador (ingredientes, ajustes, cargando), resultados, detalle, favoritos,
  perfil, perfil-editar y perfil-sugerencias. Referencias actualizadas para apuntar al prompt troceado
  (`docs/design/concepts/README.md` y cabecera de `docs/design/stitch-prompt.md`, que sigue siendo la
  fuente completa de referencia).
- Verificación ejecutada: cambio **solo de documentación** (ningún fichero Kotlin/recursos/gradle), por
  lo que el gate `init.sh` no aplica; revisión manual de los 22 ficheros y de `git status`.
- Ficheros creados/cambiados: los 22 ficheros de `docs/design/stitch/`, `docs/design/concepts/README.md`,
  `docs/design/stitch-prompt.md` y este registro.
- Hallazgos relevantes: el prompt largo se trunca; pegarlo en partes pequeñas y en la **misma** sesión
  (no sesiones distintas) es lo que permite generar todo. La hoja "Design System" es una parte propia.
- Riesgo o incógnita abierta: ninguno nuevo. Las partes de Resultados, Detalle, Favoritos y búsqueda IA
  corresponden a features aún `not_started` (igual que en el prompt original).
- Siguiente paso: la persona pega las partes en Stitch en orden y guarda cada export en
  `docs/design/concepts/<feature-id>/<pantalla>/`; después, **Fase 2** de reconciliación una a una.

### Sesión 048

- Fecha: 2026-10-09
- Objetivo: ejecutar `feature-flow` para la feature `results-screen` (única seleccionada), en modo
  hasta-aceptación. Solo planner/implementer/validator; **sin push ni PR** (publica `github-flow`).
- Completado:
  - Selección: `results-screen` (`area: ui`, `depends_on: generation-flow-with-validation` ya
    `accepted`), sin spec previa. Rol 1 ejecutado: **planner** (`feature-spec`).
  - Spec creada: `docs/specs/results-screen.md` (514 líneas). Fija la fórmula de `matchScore`
    (derivada, en memoria, no persistida; media ponderada de cobertura/gustados/cocina/tiempo/
    dificultad menos penalización por evitados; ejemplo dorado 86/76), los estados poblado/vacío/
    `Unavailable` (con "Reintentar") y el subtítulo de búsqueda. Sustituye y elimina
    `GeneratorResultsPreview`.
  - **Gate de UI**: la spec es de UI y exige capturas de referencia en
    `docs/design/concepts/results-screen/` antes de implementar. No existen. Se crea la carpeta con
    `docs/design/concepts/results-screen/README.md` (petición) y se **detiene** el flujo, marcando la
    feature `blocked` (regla de `feature-flow` y `AGENTS.md`): no se lanza el `implementer`.
- Verificación ejecutada: ninguna de código (no hay implementación). Revisión de artefactos: la spec
  nombra las cuatro pantallas con sus tres ficheros cada una; `docs/design/concepts/results-screen/`
  solo contiene el `README.md` de petición; `git status` sin cambios fuera de la spec, el README y el
  harness.
- Ficheros creados/cambiados: `docs/specs/results-screen.md` (planner),
  `docs/design/concepts/results-screen/README.md` (nuevo), `feature_list.json` (`results-screen` a
  `blocked`) y este registro.
- Hallazgos relevantes: el planner (rol limitado a `docs/specs/`) nombra las capturas pero no crea la
  carpeta; esa petición la materializa el orquestador, como exige el gate de UI. El bloqueo es de
  entrada, no técnico: falta que la persona exporte de Stitch las cuatro pantallas.
- Riesgo o incógnita abierta: **bloqueada** `results-screen` hasta que existan las capturas de
  `docs/design/concepts/results-screen/{resultados,resultados-busqueda,resultados-vacio,resultados-error}/`
  (cada una con `.png`, `.html` y `design.md`). Sigue pendiente además la Fase 2 de reconciliación de
  las 11 features de UI ya aceptadas (sesión 046).
- Siguiente paso: la persona pega en Stitch la parte `resultados` (más los estados vacío/error) y deja
  los exports en `docs/design/concepts/results-screen/`; entonces se desbloquea la feature y el
  orquestador lanza el `implementer`.

### Sesión 049

- Fecha: 2026-10-09
- Objetivo: implementar `results-screen` (pantalla de Resultados real) según
  `docs/specs/results-screen.md`. Sesión reanudada: el trabajo ya existía en disco sin commitear y sin
  los artefactos de harness/docs; esta sesión lo revisa, lo reconcilia con la spec, completa lo que
  faltaba y verifica. El gate de capturas de UI (que bloqueaba la sesión 048) queda satisfecho: existe
  `docs/design/concepts/results-screen/resultados/` con sus tres ficheros.
- Completado: `com.sazon.app.ui.results` como lógica **pura** testeable en JVM más capa Compose delgada.
  `MatchScore.of(...)` fija la fórmula **derivada y no persistida** de `matchScore` (media ponderada
  `Σ(w·f)/Σw` de cobertura/`W_COVERAGE`, gustados/`W_LIKED`, cocina/`W_CUISINE`, tiempo/`W_TIME` y
  dificultad/`W_DIFFICULTY`, menos `DISLIKE_PENALTY` por evitado presente; ejemplo dorado 86/76; sin
  `Random` ni reloj). `ResultsStatus.from(...)` puntúa las aceptadas con `copy(matchScore = ...)` y las
  ordena por relevancia (`Content`/`Empty`/`Unavailable`); `ResultsHeader`/`ResultsOrigin`/`ResultsHeaders.of`
  formatean el encabezado (Generador con singular/plural y franja, o Búsqueda con la consulta entre
  comillas). Compose: `ResultsScreen` (`Scaffold`: topBar "Atrás" + título/subtítulo, bottomBar "Cambiar
  ingredientes", estados poblado/vacío/degradado con "Reintentar") y `ResultRecipeCard` (degradado+glifo
  16:10, título, descripción, meta tabular y píldora de coincidencia; sin corazón ni click). Integración:
  `MainShell` pasa `seedRecipes` a `GeneratorFlow`, que en `result != null` monta `ResultsScreen` con
  `onBack` (→ ajustes), `onChangeIngredients` (→ ingredientes) y `onRetry` (incrementa el token); se
  elimina `GeneratorResultsPreview.kt`.
- Verificación ejecutada: `init.sh` con Git Bash (exit 0, `./gradlew test` + `./gradlew lint`, sin
  procesos largos); antes, `./gradlew testDebugUnitTest --tests "com.sazon.app.ui.results.*"` con parseo
  propio de los XML. Inspección de pureza, del fichero eliminado, de que no cambian build/lockfile/
  dominio/`data`/Taxonomía/`seed-recipes.json`/`MainNavigationState.kt`/`init.sh` y de que no hay hex
  sueltos.
- Evidencia capturada: **255 tests en 39 suites, 0 fallos/errores/saltados** (234 preexistentes + 21
  nuevos: `MatchScoreTest` 8, `ResultsHeaderTest` 7, `ResultsStatusTest` 6); lint 0 errores y 13 warnings
  preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), ninguno en ficheros de la feature; `init.sh` con código 0; `GeneratorResultsPreview.kt`
  eliminado y sin referencias en `app/src`; `MatchScore`/`ResultsStatus`/`ResultsHeader` solo importan
  `domain.*`/`java.text.Normalizer`; sin dependencias nuevas ni cambios en el build, el lockfile, el
  dominio, `data/**`, la Taxonomía, `seed-recipes.json` ni `init.sh`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/results/{MatchScore,ResultsStatus,ResultsHeader,ResultsScreen,ResultRecipeCard}.kt`,
  `app/src/test/java/com/sazon/app/ui/results/{MatchScoreTest,ResultsStatusTest,ResultsHeaderTest}.kt`,
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` y `app/src/main/java/com/sazon/app/ui/MainShell.kt`
  (integración), `GeneratorResultsPreview.kt` (eliminado), `docs/technical-discovery.md`,
  `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `DESIGN.md` (`results-screen/resultados`),
  `feature_list.json` (`results-screen` a `passing`) y este registro.
- Hallazgos relevantes: la fórmula y los estados ya estaban implementados y en verde; no hubo que
  corregir código, solo completar los artefactos de harness/docs y verificar. `./gradlew testDebugUnitTest
  --tests "...ui.results.*"` deja el daemon vivo (el `trap` de `init.sh` lo evita); se paró con `./gradlew --stop`.
- Riesgo o incógnita abierta: falta la **revisión manual** de emulador que `AGENTS.md` reserva a la
  persona (estado vacío; error con "Reintentar"; "Cambiar ingredientes" conservando el borrador; acabado
  contra `docs/design/concepts/results-screen/resultados/`). Sigue pendiente además la Fase 2 de
  reconciliación de capturas de features de UI ya aceptadas (sesión 046).
- Siguiente paso: validación independiente de `results-screen` con `feature-validator`; después, commit
  local (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 050

- Fecha: 2026-10-10
- Objetivo: reanudar e implementar `recipe-detail-screen` (Detalle de receta con raciones ajustables)
  según `docs/specs/recipe-detail-screen.md`. Una ejecución previa se interrumpió y dejó el trabajo en
  disco sin commitear y sin los artefactos de harness/docs; esta sesión lo revisa contra la spec,
  completa lo que faltaba (el gate de UI ya estaba satisfecho con las capturas de
  `docs/design/concepts/recipe-detail-screen/detalle-receta/`), corrige una desviación del test de
  escalado y lo autoverifica sin rehacer lo ya válido.
- Completado: lógica **pura** en `com.sazon.app.ui.detail` testeable en JVM (sin Compose ni Android):
  `ServingScaler.scaledIngredients(recipe, targetServings)` con **redondeo por unidad** (no lineal),
  `GRAM_STEP = 5` (`g`/`ml`), `UNIT_STEP = 0.5` (`ud`/`cda`/`cdta`), half-up (`Math.round`), mínimo el
  paso si `quantity > 0`, `unit = null` ("al gusto") **no se escala**, `factor == 1` intacto y sin
  división por cero (`servings.coerceAtLeast(1)`); `IngredientFormat.quantityLabel` (`"½"`, `"4½ ud"`,
  `"300 g"`, `"Al gusto"`, nunca `"0.5"`); `RecipeDetailFormat.metaEntries` (Tiempo/Dificultad real con
  respaldo al `id`/Comensales/Calorías con `"—"` sin nutrición, sin `matchScore`) y `ADJUSTED_CAPTION`;
  `RecipeDetailTab` + `RecipeDetailState` (`initial`/`setServings` con `coerceIn`/`selectTab`, inmutable,
  sin guardar el `Recipe`). Capa Compose **delgada** `RecipeDetailScreen` (héroe degradado + "Atrás" y
  corazón; tarjeta superpuesta con meta; pestañas Ingredientes/Pasos/Notas; `ServingsStepper`
  reutilizado **sin cambios** + caption; filas con checkbox y cantidad en cifras tabulares; timeline con
  píldora ámbar; tips + rejilla de nutrición opcional; barra inferior "Guardar"/"Empezar a cocinar";
  `RecipeDetailStateSaver`). Integración: `RecipeCard`/`ResultRecipeCard` ganan `onClick` opcional;
  `HomeScreen`/`ResultsScreen`/`GeneratorFlow` reciben `onOpenRecipe`; `MainShell` hospeda el detalle
  como **overlay** (`var detailRecipe by remember`) encima de la Home/Generador **sin desmontarlos**;
  `isSaved = false`, `onToggleSave = {}`, `onStartCooking = {}` como costuras no-op de
  `favorites`/modo cocina. Tests JVM con los vectores dorados (escenarios 1-4) y casos negativos.
  Sin dependencias nuevas; no se tocan `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`,
  `data/**`, la Taxonomía, `seed-recipes.json`, `ui.preferences/ServingsStepper.kt`,
  `ui.navigation/MainNavigationState.kt` ni `init.sh`.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados
  (`test-results/testDebugUnitTest/TEST-*.xml`) y del informe `lint-results-debug.xml`; inspección de
  imports de la lógica pura (sin Compose/Android) y de `git status` (solo los ficheros previstos).
- Evidencia capturada: **43 suites, 277 tests, 0 fallos/errores/saltados** (22 nuevos:
  `ServingScalerTest` 8, `IngredientFormatTest` 5, `RecipeDetailStateTest` 5, `RecipeDetailFormatTest` 4;
  255 preexistentes en verde); lint 0 errores y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature;
  `init.sh` con código 0; `ServingScaler`/`IngredientFormat`/`RecipeDetailFormat`/`RecipeDetailState`
  sin imports Compose/Android; `ServingsStepper`, `MainNavigationState`, el build/lockfile, el dominio,
  `data/**`, la Taxonomía, `seed-recipes.json` e `init.sh` sin cambios.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/detail/{ServingScaler,IngredientFormat,RecipeDetailFormat,
  RecipeDetailState,RecipeDetailScreen}.kt` (nuevos),
  `app/src/test/java/com/sazon/app/ui/detail/{ServingScalerTest,IngredientFormatTest,
  RecipeDetailFormatTest,RecipeDetailStateTest}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/{MainShell.kt, generator/GeneratorFlow.kt, home/HomeScreen.kt,
  home/RecipeCard.kt, results/ResultRecipeCard.kt, results/ResultsScreen.kt}` (modificados),
  `docs/technical-discovery.md`, `docs/domain-model.md`, `DESIGN.md`, `feature_list.json` y este
  registro.
- Hallazgos relevantes: la inspección inicial detectó un **defecto real** en
  `ServingScalerTest.una_receta_sin_raciones_no_divide_por_cero`: con `recipe.servings = 0` y
  `targetServings = 2` el factor es `2` (por `coerceAtLeast(1)`), así que `Patata` queda en `1200 g` y no
  en `600 g` como asertaba el test original; corregido el vector (y añadido `Huevo = 12 ud`) para
  alinearlo con la fórmula de la spec. El test pasó a verde en la siguiente ejecución. El resto de la
  lógica y la capa Compose ya eran correctas.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge la prueba de
  emulador exigida (abrir una receta desde Resultados y desde la Home, cambiar las raciones y ver las
  cantidades actualizarse, recorrer las tres pestañas y "Atrás" sin perder resultado/borrador) y el
  acabado visual contra `docs/design/concepts/recipe-detail-screen/detalle-receta/`; un defecto visual se
  corrige con feature correctiva. Rotar con el detalle abierto lo cierra (`detailRecipe` vive en
  `remember`), como documenta la spec (Unknown). Se mantiene la Fase 2 de reconciliación de capturas de
  las features de UI previas (sesión 046).
- Siguiente paso: validación independiente de `recipe-detail-screen` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`. La
  siguiente feature en orden de lista es `favorites` (espera a que `recipe-detail-screen` quede
  `accepted`).

### Sesión 051

- Fecha: 2026-10-10
- Objetivo: validar `recipe-detail-screen` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/recipe-detail-screen` (worktree), solo
  hasta-aceptación; sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos) y, al ver la primera pasada en `UP-TO-DATE`, forzó
  `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks`; parseó
  por su cuenta los XML de resultados y el informe `lint-results-debug.xml`, e inspeccionó el código.
- Evidencia capturada: 43 suites, 277 tests, 0 fallos/errores/saltados (22 nuevos: `ServingScalerTest`
  8, `IngredientFormatTest` 5, `RecipeDetailStateTest` 5, `RecipeDetailFormatTest` 4; 255 preexistentes
  en verde); lint 0 errores y 13 warnings preexistentes, 0 en ficheros de `ui.detail`. El validador
  recalculó a mano el escalado contra `seed-recipes.json` real (`seed-tortilla-patatas`, `servings` 4):
  4→2 da `300 g`/`3 ud`/`½ ud`/`75 ml` y `Sal` sin escalar; 4→3 da `450 g`/`4½ ud`/`1 ud`/`115 ml`;
  factor 1 devuelve el original intacto; `unit = null` nunca escala; `servings = 0` no divide por cero;
  determinista. Confirmó pureza de `ui.detail` (sin Compose/Android salvo `RecipeDetailScreen`, sin hex
  sueltos), `ServingsStepper` reutilizado sin cambios, el detalle como overlay en `MainShell` (no un
  `PushedScreen` nuevo) con las costuras no-op, la captura obligatoria `detalle-receta/`
  (`screen.png`/`code.html`/`DESIGN.md`) presente y seguida, y disciplina de alcance (build/lockfile,
  dominio, `data/**`, la Taxonomía, `seed-recipes.json`, `MainNavigationState.kt` e `init.sh` intactos;
  sin dependencias nuevas). Confirmó además que el implementer no hizo commit/push/PR.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low informativos, no bloqueantes): el `README.md` de capturas repite la
  plantilla con `design.md` en minúscula mientras el fichero real es `DESIGN.md` (inocuo en Windows,
  heredado de la plantilla); `RecipeDetailFormatTest.la_caption_del_ajustador_es_estable` es una
  aserción trivial (la cobertura real está en el resto del test). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge la
  prueba de emulador exigida (abrir una receta desde Resultados y desde la Home, cambiar las raciones y
  ver las cantidades actualizarse, recorrer las tres pestañas y "Atrás" sin perder resultado/borrador) y
  el acabado visual contra `docs/design/concepts/recipe-detail-screen/detalle-receta/`; un defecto visual
  se corrige con una feature correctiva. Se mantiene la Fase 2 de reconciliación de capturas de las
  features de UI previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/recipe-detail-screen` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `favorites` (ya con sus dos
  dependencias `accepted`).

### Sesión 052

- Fecha: 2026-10-10
- Objetivo: implementar `favorites` (Favoritos disponibles sin conexión) según
  `docs/specs/favorites.md`, en la rama `feature/favorites` (worktree); solo hasta implementación y
  autoverificación (`passing`), sin commit/push/PR (los hace el orquestador).
- Completado: dominio puro `com.sazon.app.domain.favorites` (`Favorite` con **copia** de la `Recipe` +
  `savedAt`, el puerto `FavoriteRepository` y `FavoriteService.toggle` con reloj inyectable que
  normaliza `matchScore` a `null`); persistencia Room en `com.sazon.app.data.favorites` sobre base
  **propia** `favorites.db` (version 1, tabla **multi-fila** `favorite_recipe` con la receta como
  columna JSON vía `RecipeJson`, `upsert` idempotente por `recipeId` y orden `savedAt` desc); lógica de
  presentación pura `com.sazon.app.ui.favorites` (`FavoritesSort`, `FavoritesViewMode`,
  `FavoritesList.visible`/`countLabel`) y capa Compose delgada `FavoritesScreen` (cabecera "Mis recetas
  guardadas" + conteo, buscador, chips de orden terracota, toggle lista/rejilla oliva, nota
  "Disponibles sin conexión", estado vacío con marcador y "Explorar recetas", `Saver` de
  `rememberSaveable`) y `FavoriteRecipeCard`/`FavoriteRecipeTile` (miniatura 96x96 degradada, meta en
  cifras tabulares y corazón; swipe "Eliminar" vía `SwipeToDismissBox` de Material 3). Integración:
  `MainActivity` compone `FavoritesDatabase`/repositorio/servicio, `SazonApp` las pasa a `MainShell`,
  éste observa los favoritos para la pestaña, alimenta `HomeRecipes.saved` (bloque "Guardadas") y
  cablea `isSaved`/`onToggleSave` del Detalle; `ui/SectionPlaceholder.kt` eliminado. Sin Resumen de
  actividad, sin "Abiertas hace poco", sin modo cocina, sin Búsqueda con IA ni proveedor real, sin
  dependencias nuevas.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); se parsearon por separado los XML de resultados y el informe
  `lint-results-debug.xml`.
- Evidencia capturada: **46 suites, 301 tests, 0 fallos/errores/saltados**; 24 nuevos:
  `FavoriteServiceTest` 6, `FavoritesListTest` 9, `RoomFavoriteRepositoryTest` 9 (277 preexistentes en
  verde). Lint 0 errores y 13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), ninguno con localización bajo `app/src`. Inspección:
  `domain.favorites` y la lógica pura de `ui.favorites` sin imports `android.*`/`androidx.*`;
  `SectionPlaceholder` sin referencias en `app/src`; `git diff --stat` sin cambios en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, el dominio/Lote/Perfil, las bases ajenas, la
  Taxonomía ni `seed-recipes.json`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/domain/favorites/{Favorite,FavoriteRepository,FavoriteService}.kt`,
  `app/src/main/java/com/sazon/app/data/favorites/{FavoriteEntity,FavoriteDao,FavoritesDatabase,FavoritesMappers,RoomFavoriteRepository}.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/{FavoritesSort,FavoritesViewMode,FavoritesList,FavoritesScreen,FavoriteRecipeCard}.kt`,
  `app/src/test/java/com/sazon/app/domain/favorites/FavoriteServiceTest.kt`,
  `app/src/test/java/com/sazon/app/ui/favorites/FavoritesListTest.kt`,
  `app/src/test/java/com/sazon/app/data/favorites/RoomFavoriteRepositoryTest.kt`,
  `MainActivity.kt`, `ui/SazonApp.kt`, `ui/MainShell.kt` (modificados), `ui/SectionPlaceholder.kt`
  (eliminado), `docs/technical-discovery.md`, `DESIGN.md`, `feature_list.json` y este registro.
- Hallazgo de entorno: la primera ejecución del test JVM (R8/`testDebugUnitTest`) abortó con un
  `EXCEPTION_ACCESS_VIOLATION` en un hilo GC de Corretto 21 (`-Xmx512m`, `jvm.dll`), sin relación con el
  código nuevo (la compilación Kotlin de main y test terminó sin errores); la re-ejecución de `init.sh`
  fue en verde. Se registra como flake de entorno.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge
  las pruebas exigidas (guardar una receta generada por IA y verla tras reiniciar sin conexión; el
  estado vacío con "Explorar recetas" llevando a Inicio) y el acabado visual (lista/rejilla, chips,
  corazón, swipe "Eliminar", claro/oscuro) contra `docs/design/concepts/favorites/favoritos/`; un
  defecto visual se corrige con una feature correctiva.
- Siguiente paso: validación independiente de `favorites` con `feature-validator`.

### Sesión 053

- Fecha: 2026-10-10
- Objetivo: validar `favorites` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Commit local de la rama
  `feature/favorites` (lo hace el orquestador de `feature-flow`); la publicación queda para
  `github-flow`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0) y, al ver test/lint en
  `UP-TO-DATE`, forzó `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks` (28/28 tareas); parseó por su cuenta los XML de resultados y el informe
  de lint.
- Evidencia capturada: 46 suites, 301 tests, 0 fallos/errores/saltados (`FavoriteServiceTest` 6,
  `FavoritesListTest` 9, `RoomFavoriteRepositoryTest` 9); lint 0 errores y 13 warnings preexistentes, 0
  con localización bajo `app/src`. Confirmó los escenarios 1-2 y 10 (copia con `savedAt=now`,
  `matchScore` normalizado a `null`, toggle idempotente, dos filas coexisten, `remove` selectivo,
  `clear`, `observe`, orden `savedAt` desc) y 5-6 (labels, tres órdenes, búsqueda por título insensible a
  mayúsculas/acentos, caso negativo por descripción, sin coincidencias vacío, `fromKey` con fallback);
  3/4/7/8/9 por inspección de la integración. Pureza de `domain.favorites` y de la lógica pura de
  `ui.favorites`, base Room propia `favorites.db` v1 con `@Query` parametrizado, disciplina de alcance
  (build/lockfile/dominio/Lote/Perfil/bases ajenas/Taxonomía/`seed-recipes.json` intactos, sin
  dependencias nuevas) y capturas obligatorias presentes
  (`docs/design/concepts/favorites/favoritos/{screen.png,code.html,DESIGN.md}` + `README.md`).
  Seguridad: sin secretos, sin red, sin construcción de SQL por concatenación.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (3 Low, informativos, ninguno bloqueante): `README.md` de capturas con
  `design.md` en minúscula heredado de la plantilla; microcopy aditivo "No hay favoritas con esta
  búsqueda." fuera de la spec pero coherente; `Validator Checklist` de la spec sin marcar.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge las
  dos pruebas exigidas (guardar una receta generada por IA y verla tras reiniciar sin conexión; el estado
  vacío con "Explorar recetas" llevando a Inicio) y el acabado visual contra
  `docs/design/concepts/favorites/favoritos/`; un defecto visual se corrige con una feature correctiva.
- Siguiente paso: `activity-summary` (depende de `profile-screen` y `favorites`, ambas `accepted`),
  empezando por `planner`/`feature-spec`.

### Sesión 054

- Fecha: 2026-10-11
- Objetivo: analizar y documentar (sin implementar) la propuesta de backend post-MVP para cuenta,
  sincronización y proxy de IA.
- Completado: creado `docs/backend-proposal.md` con la opción B (**Kotlin + Ktor + PostgreSQL**),
  alternativas consideradas, alcance por servicios, superficie HTTP propuesta, modelo de datos,
  autenticación/seguridad, estrategia de sincronización offline-first (Room como verdad local,
  servidor como réplica, LWW + tombstones + WorkManager), módulo de dominio compartido (KMP) para
  reutilizar `RecipeValidator`/`PromptBuilder`/`PreferenceSignature`, despliegue y operaciones, plan por
  fases (backlog propuesto), impacto en el harness (ADR 0004 sugerido), riesgos, preguntas abiertas y un
  checklist de retomada.
- Verificación ejecutada: ninguna de código (cambio solo documental). No se ha tocado
  `feature_list.json`, `init.sh`, el build ni el código de la app; `docs/backend-proposal.md` no se cita
  como decisión aceptada.
- Evidencia capturada: documento versionado en `docs/backend-proposal.md`; referencia añadida en el
  estado de este registro; ninguna feature cambia de estado (WIP = 0).
- Ficheros actualizados: `docs/backend-proposal.md` (nuevo) y este registro.
- Hallazgo relevante: el backend **revierte** el no-objetivo permanente de `build-brief.md` y el
  `ADR 0002`; construirlo exige un ADR nuevo que ajuste el 0002 y actualizar discovery (recogido en
  §13 del documento). La propuesta explota que el dominio ya es puro y que la app define puertos
  (`ProfileRepository`, `FavoriteRepository`, `SuggestionBatchRepository`, `RecipeGenerator`), por lo
  que la integración es, sobre todo, adaptadores detrás de esos puertos más un motor de sync.
- Riesgo o incógnita abierta: sin resolver a propósito (el desarrollo no se inicia ahora). Preguntas
  abiertas en §15 del documento (identidad email/password vs social, qué se sincroniza, hosting).
- Siguiente paso: sin cambios en el plan del MVP; si se decide construir el backend, empezar por la
  fase 1 (`shared-domain-module`) y por el ADR 0004, según el checklist de §16 del documento.

### Sesión 055

- Fecha: 2026-10-11
- Objetivo: cambiar el paso de capturas de UI del flujo `github-flow` para que, cuando falten, se
  **provisione la feature dentro del worktree** en lugar de bloquear sin crear nada (que ensuciaba la
  base y obligaba a commitear/pushear `main`).
- Completado:
  - `.opencode/skills/github-flow/SKILL.md`: nueva Hard Rule de provisioning; preconditions #5 y párrafo
    de fallo ajustados; nuevo artefacto determinista (carpeta de petición **dentro del worktree**);
    `Mode: execute` reordenado (fija `base_sha` antes del gate) con paso explícito de *UI-captures
    provisioning* que crea worktree/rama + `README.md` + `stitch-prompt.md` y **no** lanza sesión; nueva
    sección `## UI Captures Provisioning`; `Blocked → Resume` actualizado (nada en la base, sin copias al
    reanudar); `Output Report` con estado `provisioned` y `session: none`.
  - `.opencode/skills/feature-flow/SKILL.md`: el UI gate reutiliza el material ya dejado en el worktree y
    genera también el `stitch-prompt.md` si falta.
  - `.opencode/skills/github-flow/references/evals.md`: nueva *Eval 6* (contrato del provisioning en
    modo `plan`, cero efectos).
  - `AGENTS.md`: política de capturas de UI actualizada (worktree igualmente creado, base sin ensuciar).
- Verificación ejecutada: ninguna de código (cambio solo documental del harness). No se tocó
  `feature_list.json`, `init.sh`, el build ni el código de la app; no se ejecutó `init.sh`/Gradle porque
  el cambio no altera el camino de arranque.
- Evidencia capturada: `git diff` de los cuatro ficheros; el flujo resultante documentado arriba en
  "Estado verificado actual". Ninguna feature cambia de estado (WIP = 0).
- Ficheros actualizados: `.opencode/skills/github-flow/SKILL.md`,
  `.opencode/skills/github-flow/references/evals.md`, `.opencode/skills/feature-flow/SKILL.md`,
  `AGENTS.md` y este registro.
- Riesgo o incógnita abierta: el `stitch-prompt.md` es una copia derivada de `docs/design/stitch/`
  (fuente de verdad); si las pantallas cambian, hay que regenerarlo. La generación del prompt depende del
  mapeo feature→pantalla, que se toma de la spec o de `feature_list.json`.
- Siguiente paso: sin cambios en el plan del MVP; `activity-summary` sigue siendo la siguiente candidata
  (`planner`/`feature-spec`) y, si le faltaran capturas, ahora se provisionará su worktree en vez de
  bloquear en la base.

### Sesión 056

- Fecha: 2026-10-11
- Objetivo: eliminar el cuelgue de las sesiones de agente al ejecutar Gradle (incidencia real: la
  sesión hija de `activity-summary` quedó congelada con su último comando de Gradle ya terminado).
- Diagnóstico: el subagente `implementer` lanzó `./gradlew testDebugUnitTest …` **a mano** (sin pasar
  por `init.sh`). El build imprimió `BUILD SUCCESSFUL` a las 19:45, pero el daemon de Kotlin (hijo del
  daemon de Gradle) heredó el stdout del comando y sobrevivió al build: el pipe nunca llegó a EOF, la
  herramienta `bash` de opencode quedó en estado `running` durante ~26 min y el proceso `opencode run`
  dejó de escribir log (19:44:29). Se desbloqueó matando el proceso hijo; el worktree conserva el WIP.
- Completado:
  - `scripts/gradle.sh` (nuevo, ejecutable): ejecuta `./gradlew "$@"` con la salida redirigida a un
    fichero y la vuelca al terminar, devolviendo el código de salida de Gradle. Los daemons heredan el
    fichero, no el pipe del llamante, así que no pueden colgar la shell. **No** llama a `--stop`.
  - `init.sh`: usa el wrapper para `test` y `lint` y elimina su `trap './gradlew --stop' EXIT`.
  - `AGENTS.md`: regla en "Notas de entorno" (nunca `./gradlew` directo; wrapper para comprobaciones
    puntuales; nunca `--stop`).
  - `.opencode/skills/feature-implementer/SKILL.md` (Self-Verify) y `.opencode/skills/feature-validator/SKILL.md`
    (Gradle daemon hygiene): usan el wrapper en vez de la receta `trap … --stop`.
- Hallazgo extra: `./gradlew --stop` es **global** y mata también los builds de otros worktrees. Se
  verificó en directo: el gate de esta chore falló con `Gradle build daemon has been stopped: stop
  command received` porque el `init.sh` (con el trap) de la otra feature corrió a la vez. Por eso se
  elimina el trap: el redirigido a fichero ya evita el cuelgue y los worktrees paralelos dejan de
  matarse entre sí.
- Verificación ejecutada:
  1. `bash init.sh` en el worktree de `chore/gradle-daemon-hang`: `./gradlew test` → `BUILD
     SUCCESSFUL`, `./gradlew lint` → `BUILD SUCCESSFUL`, exit 0, **con otro build corriendo en
     paralelo** (ya no se interrumpen).
  2. Prueba decisiva del wrapper sin redirección externa: `bash scripts/gradle.sh compileDebugKotlin
     --rerun-tasks` → exit 0 y la herramienta devolvió, con el daemon de Kotlin **vivo** (1 GB)
     sujetando solo el fichero de log. Es el caso que antes colgaba la sesión.
- Evidencia capturada: logs de las dos ejecuciones anteriores; `git diff` de los seis ficheros.
- Ficheros actualizados: `scripts/gradle.sh` (nuevo), `init.sh`, `AGENTS.md`,
  `.opencode/skills/feature-implementer/SKILL.md`, `.opencode/skills/feature-validator/SKILL.md` y este
  registro. No se toca `feature_list.json`, el código de la app ni el build (WIP = 0).
- Riesgo o incógnita abierta: los daemons de Gradle ya no se paran al terminar (se apagan solos por
  inactividad, ~3 h); si se acumulan, limpiarlos a mano con `./gradlew --stop` **sin builds en vuelo**.
  Las ramas de features abiertas conservan el `init.sh` con el trap hasta que se mergeen.
- Siguiente paso: sin cambios en el plan del MVP. `activity-summary` sigue en curso con la sesión
  relanzada; su WIP rompía 4 tests de `SuggestionBatchServiceTest` (`./gradlew test` fallaba) y esa
  sesión debe corregirlos antes de que el validador pueda aceptar.

### Sesión 057

- Fecha: 2026-10-11
- Objetivo: retomar y cerrar `activity-summary` (Resumen de actividad en el Perfil) sobre el WIP que
  dejó la sesión interrumpida por el cuelgue de Gradle (sesión 056), corregir los 4 tests rotos y
  dejar el repositorio listo para validación independiente.
- Completado:
  - **Corregido el WIP roto**: el orden de los parámetros de `SuggestionBatchService` dejaba
    `onRecipesGenerated` **después** del reloj, de modo que el trailing lambda `{ now }` de los tests
    existentes se ligaba al callback en vez de al reloj (4 fallos en `SuggestionBatchServiceTest`). Se
    movió el callback **antes** de `now: () -> Long` (el reloj sigue siendo el último parámetro), así la
    costura es de verdad aditiva y los tests existentes quedan intactos.
  - Reutilizado el resto del WIP en disco: `domain.activity` (puerto + `ActivitySummaryService`),
    `data.activity` (base propia `activity_summary.db` v1, una fila, `increment` atómico), el callback
    `onRecipesGenerated` en `SuggestionBatchService`/`GenerationService` (default no-op; la rama
    `UseCached` no lo invoca), `ActivitySummarySection`, y el cableado `ProfileScreen`/`MainShell`/
    `SazonApp`/`MainActivity` (`favoritesCount = favorites.size`).
  - Documentación durable: registrado `domain.activity`/`data.activity`, el callback y la sección
    "Resumen" en `docs/technical-discovery.md` (y corregida la fila de la tabla: las favoritas se
    derivan de la lista observada, no de un `COUNT`).
  - Hallazgo de documentación: `DESIGN.md` no tenía registrada la captura `activity-summary:
    perfil-resumen`; se añadió la línea a "Capturas aceptadas" (el resto de `DESIGN.md` no cambia) y se
    anotó el hallazgo en `docs/specs/activity-summary.md`.
- Verificación ejecutada:
  1. `bash scripts/gradle.sh testDebugUnitTest --tests "…SuggestionBatchServiceTest"` → `BUILD
     SUCCESSFUL` (reprodujo y confirmó el arreglo del WIP roto).
  2. `bash init.sh` con Git Bash → `./gradlew test` y `./gradlew lint`, ambos `BUILD SUCCESSFUL`,
     exit 0, sin procesos largos. Parseo propio de los XML de resultados y del informe de lint.
- Evidencia capturada: 49 suites, **316 tests/0 fallos/errores/saltados** (301 preexistentes + 15
  nuevos: `ActivitySummaryServiceTest` 6, `RoomActivitySummaryRepositoryTest` 5,
  `SuggestionBatchActivityRecordingTest` 4); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; `domain.activity` sin imports `android.*`/`androidx.*`;
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `data.favorites/**`, `data.profile/**` y
  `data.suggestions/**` sin cambios; sin dependencias nuevas.
- Ficheros creados (WIP reutilizado): los siete de `domain.activity`/`data.activity`/
  `ActivitySummarySection` y los tres tests nuevos (`ActivitySummaryServiceTest`,
  `RoomActivitySummaryRepositoryTest`, `SuggestionBatchActivityRecordingTest`), más el corte de diseño
  `docs/design/stitch/21-perfil-resumen.md` y la captura
  `docs/design/concepts/activity-summary/perfil-resumen/`. Modificados: `SuggestionBatchService.kt`,
  `GenerationService.kt`, `ProfileScreen.kt`, `MainShell.kt`, `SazonApp.kt`, `MainActivity.kt`,
  `docs/technical-discovery.md`, `DESIGN.md`, `docs/design/stitch/README.md`,
  `docs/specs/activity-summary.md`, `feature_list.json` y este registro.
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la verificación manual de emulador
  (favoritas = recetas guardadas; el contador sube al generar y no baja al refrescar ni al reabrir) y el
  acabado visual contra la captura, conforme a AGENTS.md (no se automatiza UI de emulador). La costura
  documentada: cuando se implemente `ai-search`, deberá reportar sus recetas `ai` aceptadas por el mismo
  `onRecipesGenerated`/`ActivitySummaryService.recordAcceptedRecipes` (follow-up, no en esta feature).
- Siguiente paso: validación independiente de `activity-summary` con `feature-validator`.

### Sesión 058

- Fecha: 2026-10-11
- Objetivo: validar `activity-summary` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Sin cambios de código tras la
  validación (solo estado/evidencia y este registro). Commit local en la rama `feature/activity-summary`
  (la publicación push + PR la hace `github-flow`, no esta sesión).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`, sin procesos largos), `./gradlew testDebugUnitTest
  --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks`, y parseó por su cuenta los XML de
  resultados y el informe de lint; inspeccionó pureza de dominio, tokens/UI, alcance/build y capturas.
- Evidencia capturada: **49 suites, 316 tests, 0 fallos/errores/saltados** (`ActivitySummaryServiceTest`
  6, `RoomActivitySummaryRepositoryTest` 5, `SuggestionBatchActivityRecordingTest` 4; regresión
  `SuggestionBatchServiceTest` 9/9 y `GenerationServiceTest` 6/6); lint 0 errores y 13 warnings
  preexistentes (0 en ficheros de la feature); `domain.activity` sin imports `android.*`/`androidx.*`;
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `data.favorites/**`, `data.profile/**` y
  `data.suggestions/**` sin cambios; capturas `docs/design/concepts/activity-summary/perfil-resumen/`
  presentes (`screen.png`, `code.html`, `DESIGN.md`) y registradas en `DESIGN.md`; escenarios 3, 4, 6 y 7
  cubiertos por tests JVM y la regla "solo `source == ai`, solo suma" confirmada en
  `ActivitySummaryService.recordAcceptedRecipes` (reforzada en `RoomActivitySummaryRepository`).
- Hallazgos del validador (3 Low/informativos, ninguno bloqueante): `onRecipesGenerated` se invoca tras
  `repository.save(batch)` en `SuggestionBatchService` (si la escritura del contador lanzara,
  `ensureBatch` propagaría el error con el Lote ya guardado; robustez menor); checklists de la spec sin
  marcar (patrón heredado del repo); nota de harness del worktree (`.git` referencia ruta WSL y `git`
  desnudo falló de forma intermitente desde Git Bash, resuelto con `GIT_DIR`/`GIT_WORK_TREE`).
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la verificación manual de emulador
  (favoritas = recetas guardadas; el contador sube al generar y no baja al refrescar ni al reabrir) y el
  acabado visual contra la captura, conforme a AGENTS.md (no se automatiza UI de emulador). La costura
  documentada: cuando se implemente `ai-search`, deberá reportar sus recetas `ai` aceptadas por el mismo
  `onRecipesGenerated`/`ActivitySummaryService.recordAcceptedRecipes` (follow-up).
- Siguiente paso: commit local de la rama `feature/activity-summary` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, especificar `ai-search` con
  `feature-spec`.
### Sesión 059

- Fecha: 2026-10-11
- Objetivo: dejar **todo el flujo corriendo nativo en WSL**. El repositorio vivía en `/mnt/c` pero se
  trabajaba con Git Bash y el `java` de Windows: `bash init.sh` en WSL fallaba con
  `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH`.
- Completado:
  - `scripts/wsl-env.sh` (nuevo, se *sourcea*): publica `JAVA_HOME` (JDK 21 de Linux),
    `ANDROID_HOME`/`ANDROID_SDK_ROOT` (SDK de Linux), `GRADLE_USER_HOME` en ext4 y el `PATH` de `gh`;
    desactiva el file-watching de Gradle (`-Dorg.gradle.vfs.watch=false`, no fiable en DrvFs) y
    normaliza `local.properties` cuando trae una ruta del SDK de Windows. Fuera de WSL no hace nada.
  - `scripts/wsl-bootstrap.sh` (nuevo, idempotente, **sin sudo**): instala JDK 21 (Temurin,
    `~/.local/opt/jdk-21`), el SDK de Android de Linux (`~/Android/Sdk`: cmdline-tools 19.0,
    `platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`), `gh` nativo y reutiliza la sesión
    de `gh` de Windows (su token está en el keyring, no en `hosts.yml`: `gh.exe auth token` →
    `gh auth login --with-token`) y la distribución de Gradle ya descargada en Windows.
  - `init.sh`, `scripts/gradle.sh` y `scripts/new-worktree.sh` sourcean `wsl-env.sh`.
  - Worktrees: reparada la metadata de `activity-summary` y `chore-gradle-daemon-hang`, que guardaba
    rutas `C:/...` creadas por Git Bash (en WSL salían `prunable` y `git status` dentro fallaba con
    `not a git repository`). Ciclo completo probado desde WSL con un id desechable:
    `scripts/new-worktree.sh` (crea, ramifica y siembra `local.properties` con la ruta Linux) y
    `scripts/cleanup-worktree.sh` (elimina worktree y rama).
  - Causa confirmada por evidencia ajena: la Sesión 058 ya anotó como incidencia de harness que el
    worktree fallase **desde el otro sistema** (resuelto a mano con `GIT_DIR`/`GIT_WORK_TREE`); esta
    sesión corrige la raíz (la metadata del worktree guarda rutas absolutas en el formato del sistema
    que lo creó) y deja la regla escrita en `AGENTS.md`.
  - Harness nativo: `gh` en `~/.local/bin/gh`, helper de credenciales de git apuntando al `gh` nativo
    (antes invocaba al `gh.exe` de Windows por interop) y bloque de entorno en `~/.profile` para las
    shells interactivas. `opencode` ya era el binario Linux (`~/.opencode/bin/opencode`, v2.0.26), pero
    en shells **no** interactivas el PATH resolvía al shim npm de Windows (el alias de `~/.bashrc` solo
    aplica a las interactivas, y esa build de Bun es la que se colgaba al cerrar): `wsl-bootstrap.sh`
    deja un enlace en `~/.local/bin/opencode`, que va primero en el PATH, así que resuelve nativo
    siempre.
  - Documentación: `AGENTS.md` ("Notas de entorno" reescritas para WSL: plataforma, JDK/SDK, caché de
    Gradle, regla de worktrees desde WSL), `docs/technical-discovery.md` (`Testing and Verification` y
    `Deployment and Operations`) y `.opencode/skills/github-flow/SKILL.md` (la sesión se lanza en WSL).
- Verificación ejecutada:
  1. `bash scripts/wsl-bootstrap.sh` dos veces (instalación y re-ejecución idempotente, sin descargas
     nuevas).
  2. Sonda de SDK: con `sdk.dir` = ruta de Windows (inexistente en WSL) y `ANDROID_HOME` al SDK de
     Linux, `:app:processDebugResources` → `BUILD SUCCESSFUL` (AGP avisa de `sdk.dir` no válido y cae a
     `ANDROID_HOME`); con `wsl-env.sh` cargado, `local.properties` queda en la ruta Linux y el aviso
     desaparece.
  3. `bash init.sh` en WSL (camino estándar, con `local.properties` apuntando todavía al SDK de
     Windows): `./gradlew test` → `BUILD SUCCESSFUL` en 11m31s (**301 tests, 0 fallos, 0 errores, 0
     omitidos**) y `./gradlew lint` → `BUILD SUCCESSFUL` en 2m20s; exit 0. Primera pasada, con la
     descarga inicial de dependencias de Maven: **13m52s**. `local.properties` quedó apuntado a
     `/home/fernandogaliay/Android/Sdk` durante la propia ejecución. Segunda pasada en caliente:
     **8,7 s** en total (test y lint `UP-TO-DATE`, caché de configuración reutilizada).
  4. Reescritura de `local.properties`: probada con la forma escapada de Android Studio (`sdk.dir=C\:\\...`)
     y sin escapar; conserva otras claves (`ia.apiKey`), es idempotente con una ruta Linux ya correcta y
     no crea el fichero si no existe.
  5. `gh auth status`, `gh repo view` y `gh issue list` nativos en WSL; `git ls-remote origin` autentica
     con el helper nativo (repo privado).
  6. Ciclo de worktree de punta a punta (crear, abrir y eliminar) y `git worktree list` con los tres
     worktrees operativos desde WSL.
  7. Post-merge (tras integrar `origin/main` `0de0719`, con el código de `activity-summary`): `bash
     init.sh` → exit 0 en 2m29s (`test` `BUILD SUCCESSFUL` en 1m29s y `lint` en 59s), con **49 suites /
     316 tests / 0 fallos / 0 errores / 0 omitidos** y 0 errores de lint: las mismas 316 que registraron
     las Sesiones 057 y 058 en Windows, lo que confirma que WSL reproduce la verificación.
  8. Resolución de herramientas: `command -v java/gh/adb/opencode` → binarios nativos en shells de login
     y no interactivas (`opencode v2.0.26`, no el shim de Windows `1.18.35`).
- Evidencia capturada: salida de todos los comandos anteriores y `git worktree list` reconstruido.
- Ficheros actualizados: `scripts/wsl-env.sh` y `scripts/wsl-bootstrap.sh` (nuevos), `init.sh`,
  `scripts/gradle.sh`, `scripts/new-worktree.sh`, `AGENTS.md`, `docs/technical-discovery.md`,
  `.opencode/skills/github-flow/SKILL.md` y este registro. No se toca `feature_list.json` ni el código
  de la app (WIP = 0 en esta rama).
- Riesgo o incógnita abierta: (a) la metadata de un worktree guarda rutas absolutas en el formato del
  sistema que lo creó, así que **se crean desde WSL** (ya documentado; el que se cree desde Git Bash
  aparecerá `prunable` en WSL); (b) si Android Studio en Windows resincroniza el proyecto, reescribe
  `local.properties` con la ruta de Windows y WSL la corrige en el siguiente build; (c) quedan restos de
  la etapa Windows: el directorio vacío `Documents\Default Project-worktrees` y el worktree
  `chore-gradle-daemon-hang` (PR #53 ya mergeado: candidato a `github-flow prune`, que es idempotente);
  (d) el `adb` de WSL no ve el emulador (ni un móvil por USB) que corre en Windows, así que
  `installDebug` desde WSL no encuentra dispositivo: la comprobación visual/manual sigue en Android
  Studio (documentado en `AGENTS.md`).
- Siguiente paso: sin cambios en el plan del MVP. `activity-summary` quedó **aceptada** (Sesión 058) y
  **mergeada** (PR #56) mientras corría esta sesión, con su worktree ya limpiado; la siguiente
  candidata es `ai-search` (`feature-spec`). Esta rama integra `origin/main` (`0de0719`) conservando
  ambas aportaciones en `PROGRESS.md`.

### Sesión 060

- Fecha: 2026-10-12
- Objetivo: implementar `ai-search` (bloque 2 de la Home: Búsqueda con IA y sus estados) según
  `docs/specs/ai-search.md`, en el worktree de la rama `feature/ai-search`.
- Completado:
  - **Dominio puro** en `com.sazon.app.domain.search`: `AiSearchService.search(query, profile)` (normaliza
    con `SearchMemo.key`, sirve el memo, llama a `RecipeGenerator.searchByText`, valida **receta a receta**
    con `RecipeValidator.validateRecipe`, re-lanza `CancellationException`, degrada a `aiUnavailable` y
    **no** memoiza el fallo, reporta aceptadas por `onRecipesGenerated`) y `SearchMemo` (memo de sesión en
    memoria, clave `trim().lowercase()`).
  - **Lógica/estado puro** en `com.sazon.app.ui.search`: `AiSearchQuery` (normalize/canSubmit),
    `AiSearchCopy` (microcopy único), `AiSearchProgress` (cabecera + tres sublíneas + línea de prompt) y
    `AiSearchOutcome.stageOf` (Results/Blocked/NoResults/Failure; el bloqueo por Perfil lo decide el
    **motivo del descarte** `DIET_PATTERN`/`HARD_RESTRICTION`, no la consulta).
  - **Costura de conectividad**: `com.sazon.app.data.NetworkStatusSource` (sobre `ConnectivityManager`,
    `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`) y permiso `ACCESS_NETWORK_STATE` en el
    manifiesto. Sin dependencias nuevas.
  - **Capa Compose delgada**: `AiSearchBar` (bloque 2 de la Home, píldora + destello + envío; deshabilitada
    y con `OFFLINE_NOTE` sin conexión), `AiSearchFlow` (host de `PushedScreen.SEARCH`: carga reutilizando
    `GeneratorGeneratingScreen` con `cancelLabel` aditivo y `ResultsScreen` con `ResultsOrigin.Search`, y
    los estados de `AiSearchStates`) y `AiSearchStates` (bloqueado en ámbar `tertiaryContainer`, sin
    resultados y fallo, cada uno con "Atrás" y su acción).
  - **Integración sin DI**: `PushedScreen.SEARCH`; `HomeScreen` monta `AiSearchBar` con la conectividad y
    la consulta; `MainShell` guarda la consulta en `rememberSaveable`, observa la conectividad y enruta
    `SEARCH` a `AiSearchFlow` con los callbacks; `SazonApp`/`MainActivity` componen `AiSearchService` y
    `NetworkStatusSource`.
  - **Tests JVM nuevos** (27): `AiSearchServiceTest` 9, `SearchMemoTest` 5, `AiSearchOutcomeTest` 6,
    `AiSearchQueryTest` 4 y `AiSearchProgressTest` 3.
- Verificación ejecutada: `bash init.sh` (exit 0, `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos); `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  (32/32 tareas) y `bash scripts/gradle.sh lint`, con parseo propio de los XML de resultados y del
  informe de lint.
- Evidencia capturada: **54 suites, 343 tests, 0 fallos/errores/saltados** (316 preexistentes en verde);
  `AiSearchServiceTest` 9 (`accepted` vacío con dieta contraria + `DIET_PATTERN`; el falso real descarta
  el pollo por dieta; gluten → `HARD_RESTRICTION`; fallo → `aiUnavailable` sin propagar; cancelación
  re-lanzada; consulta en blanco no llama; memo no repite y el fallo no se memoiza; `onRecipesGenerated`
  solo con aceptadas); lint **0 errores, 13 warnings** preexistentes y **0 en ficheros de la feature**
  (se corrigió 1 Hint `AutoboxingStateCreation` en `AiSearchFlow.kt` con `mutableIntStateOf`).
  Inspección: `domain.search` y la lógica pura de `ui.search` sin imports `android.*`/`androidx.*`;
  `ui.results` intacto; sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio
  aceptado, `data/**` (salvo `NetworkStatusSource`), la Taxonomía, `seed-recipes.json` ni `init.sh`; sin
  hex sueltos ni iconos.
- Ficheros creados: `app/src/main/java/com/sazon/app/domain/search/{AiSearchService,SearchMemo}.kt`,
  `app/src/main/java/com/sazon/app/ui/search/{AiSearchQuery,AiSearchCopy,AiSearchProgress,AiSearchOutcome,AiSearchBar,AiSearchFlow,AiSearchStates}.kt`,
  `app/src/main/java/com/sazon/app/data/NetworkStatusSource.kt` y los cinco tests en
  `app/src/test/java/com/sazon/app/{domain,ui}/search/`.
- Ficheros modificados: `AndroidManifest.xml` (permiso `ACCESS_NETWORK_STATE`), `MainActivity.kt`,
  `ui/SazonApp.kt`, `ui/MainShell.kt`, `ui/home/HomeScreen.kt`, `ui/navigation/MainNavigationState.kt`
  (`PushedScreen.SEARCH`), `ui/generator/GeneratorGeneratingScreen.kt` (`cancelLabel` aditivo con default
  `"Cancelar"`), `feature_list.json` y este registro.
- Docs actualizados: `docs/technical-discovery.md` (ubicación de `domain.search`/`ui.search`, estados y
  costura `NetworkStatusSource` + permiso), `docs/domain-model.md` (dónde vive la orquestación de la
  Búsqueda con IA y que el memo es de sesión) y `DESIGN.md` (captura aceptada `ai-search: busqueda-ia`).
- Hallazgo (spec actualizada): el **Scenario 2** es inconsistente con el `FakeRecipeGenerator` real — un
  Perfil vegano solo descarta "Pollo al ajillo" (las otras tres fixtures son vegetales), así que
  `accepted` no queda vacío y el estado sería Resultados, no Bloqueado. La verificación exigida ("una
  consulta que contradice el patrón de dieta devuelve cero recetas") se cubre con un generador cuya salida
  es **toda** de dieta contraria; además se comprueba con el falso real que el pollo cae por
  `DIET_PATTERN`. Anotado en `docs/specs/ai-search.md` (Unknowns/Findings).
- Riesgo o incógnita abierta: el acabado visual (píldora + destello, ámbar del bloqueo, claro/oscuro) y
  las pruebas manuales de emulador (barra sin conexión; carga cancelable; resultados con subtítulo;
  bloqueo → Perfil) quedan para revisión de la persona tras el merge (AGENTS.md no automatiza UI de
  emulador). Se mantiene la pregunta 1 abierta (errores/reintentos de la IA) hasta `real-ai-provider`.
- Siguiente paso: validación independiente de `ai-search` con `feature-validator`; el commit local de la
  rama lo hace el orquestador de `feature-flow` y la publicación queda para `github-flow`.

### Sesión 061

- Fecha: 2026-10-12
- Objetivo: validar `ai-search` de forma independiente y persistir la aceptación, en el worktree de la
  rama `feature/ai-search`.
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`); `feature_list.json` pasa a `accepted` con evidencia que nombra la validación
  independiente; registrado el detalle en `docs/validations/ai-search.md`. Sin cambios de código tras
  la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos), `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  (32/32 tareas) y parseó por su cuenta los XML de resultados y el informe de lint; inspeccionó pureza
  de `domain.search`/`ui.search`, el alcance del diff y `ui.results`.
- Evidencia capturada: **54 suites, 343 tests, 0 fallos/errores/saltados** (27 nuevos de `ai-search`,
  316 preexistentes en verde), reproducidos exactamente respecto al implementer; lint **0 errores, 13
  warnings** preexistentes y **0 en ficheros de la feature**; `ui.results` intacto; `data/**` solo el
  nuevo `NetworkStatusSource`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio aceptado,
  la Taxonomía, `seed-recipes.json` e `init.sh` sin cambios.
- Hallazgos del validador (4 Low/informativos, ninguno bloqueante): (1) el **Scenario 2** es imposible
  con el `FakeRecipeGenerator` real (un Perfil vegano solo descarta "Pollo al ajillo"; `accepted` no
  queda vacío y el estado real sería Resultados, no Bloqueado); lo acepta como limitación de la fixture
  de desarrollo, no de la lógica, porque la verificación exigida ("cero recetas" con dieta contraria)
  se cubre con un test negativo genuino, el bloqueo por `DIET_PATTERN`/`HARD_RESTRICTION` se cubre en
  `AiSearchOutcomeTest` y la inconsistencia está trazada en la spec/PROGRESS/evidencia; (2)
  `AiSearchProgress.subLines(query)` no usa el parámetro (microcopy estable por diseño); (3) el memo se
  indexa solo por consulta normalizada, como fija la spec; (4) rotar la página vuelve a carga, mismo
  criterio que el Generador y declarado como *Unknown*. Sin hallazgos Critical/High/Medium; sin
  acciones de seguridad requeridas.
- Riesgo o incógnita abierta: el estado Bloqueado no es demostrable en smoke manual con el fake actual
  (un camino manual demostrable exigiría una feature correctiva de fixture, fuera de alcance). Se
  mantiene la revisión manual de la persona (barra sin conexión, carga cancelable, subtítulo de
  resultados, acabado frente a las capturas) y la pregunta 1 abierta hasta `real-ai-provider`.
- Siguiente paso: `feature_list.json` queda en `accepted` y el orquestador de `feature-flow` crea el
  **commit local** de la rama `feature/ai-search` (nunca push ni PR: la publicación es del orquestador
  de `github-flow`). Después, la siguiente candidata es `real-ai-provider`.

### Sesión 062

- Fecha: 2026-10-12
- Objetivo: chore de mantenimiento del harness y de tests, tras analizar por qué `ai-search` tardó más de
  lo esperado. Dos frentes: (a) el CLI `opencode` v2 ya no soporta `--dir`; (b) los tests de `observe()`
  de los repositorios Room usaban un margen de 5 s que a veces se agota con builds de Gradle en paralelo.
- Completado:
  - `.opencode/skills/github-flow/SKILL.md`, `.opencode/skills/github-flow/references/evals.md` y
    `AGENTS.md`: el arranque de la sesión de una feature pasa a ser `opencode run` **con el worktree como
    directorio de trabajo** (`cd`/cwd), porque `opencode` v2 no tiene `--dir`; nota de
    `NODE_OPTIONS=--dns-result-order=ipv4first` ante DNS IPv6 y de `-m <provider/model>` solo si hace
    falta override.
  - `.opencode/skills/feature-implementer/SKILL.md` (Self-Verify): preferir tiradas enfocadas y un único
    `bash init.sh` final (cada gate completo cuesta ~1.5–2.5 min).
  - Estabilizados los cuatro tests Room de `observe()` (`RoomActivitySummaryRepositoryTest`,
    `RoomFavoriteRepositoryTest`, `RoomProfileRepositoryTest`, `RoomSuggestionBatchRepositoryTest`): el
    margen de espera de las emisiones del Flow de Room pasa de 5 s a 30 s (con comentario), sin cambiar el
    comportamiento verificado.
- Verificación ejecutada: `bash init.sh` en el worktree → exit 0 (`./gradlew test` BUILD SUCCESSFUL en
  1m52s; `./gradlew lint` BUILD SUCCESSFUL en 1m41s, 0 errores), y una tirada enfocada
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` de las cuatro clases (28 tests, 0 fallos).
- Evidencia capturada: salida de los comandos anteriores; `git diff --stat` (4 ficheros de test, +16/-8;
  4 ficheros de docs/skills).
- Ficheros actualizados: `.opencode/skills/github-flow/SKILL.md`,
  `.opencode/skills/github-flow/references/evals.md`, `.opencode/skills/feature-implementer/SKILL.md`,
  `AGENTS.md`, los cuatro `Room*RepositoryTest.kt` y este registro. No se toca `feature_list.json` ni el
  código de la app (WIP = 0 en esta rama).
- Riesgo o incógnita abierta: el arreglo del flake es un margen mayor, no una eliminación determinista de
  la carrera (sería inyectar un executor de test en Room y usar reloj virtual); si reapareciera, se hace
  una correctiva. El PR #60 (`ai-search`) se mergeó mientras corría esta sesión, así que esta entrada se
  renumeró a 062 al integrar `origin/main`.
- Siguiente paso: sin cambios en el plan del MVP. Siguiente candidata: `real-ai-provider`.

### Sesión 063

- Fecha: 2026-10-12
- Objetivo: saneado del harness tras la revisión de coherencia (deriva de plantilla, referencias
  obsoletas y huecos de tooling).
- Completado:
  - A2: sustituidas en las skills las referencias a ficheros inexistentes de stack ajeno
    (`docs/product-brief.md`, `docs/user-and-access-model.md`, `docs/mvp-scope.md`, `ARCHITECTURE.md`,
    `CONSTRAINTS.md`, `pnpm`, `bootstrap-nextjs-shell`) por los reales de este repo.
  - A3: comando del gate unificado a `bash init.sh` (feature-flow ya no dice `CI=true ./init.sh`).
  - A4: corregido el ejemplo de capturas de `AGENTS.md` y el id `perfil-screen` → `profile-screen` en
    `evals`.
  - A6: `docs/design/concepts/README.md` documenta el provisioning en el worktree; el mapa de Stitch
    deja de marcar `ai-search/` como "futuro".
  - D3: nuevo `scripts/launch-feature-session.sh` (centraliza cwd del worktree, modelo,
    `NODE_OPTIONS=--dns-result-order=ipv4first` y título); `github-flow` y `AGENTS.md` lo referencian.
  - D4: `new-worktree.sh`/`sync-worktree.sh`/`cleanup-worktree.sh` aceptan `chore/<id>`; y los cuatro
    scripts resuelven la raíz por el git common dir (bug: ejecutados desde un worktree apuntaban al
    worktree en vez de al checkout base).
  - D2 (descartado): unir `test lint` en una sola invocación de Gradle **falla** por validación de
    dependencia implícita (`lint` lee `app/src/main/assets`, salida de `syncTaxonomy`). Se mantiene el
    `init.sh` con dos invocaciones; arreglarlo exige tocar el build (fuera de este chore).
- Verificación ejecutada: `bash init.sh` exit 0 (`./gradlew test` 1m42s + `./gradlew lint`); `bash -n`
  de todos los scripts; dry-runs de `cleanup-worktree.sh` con `chore/gradle-daemon-hang`, `ai-search` y
  `real-ai-provider` (resuelven dir/rama correctos).
- Evidencia capturada: salida de los comandos anteriores; `git diff --stat`.
- Ficheros actualizados: skills (`feature-flow`, `feature-spec`, `feature-implementer`,
  `feature-validator` + `references/`, `github-flow`), `AGENTS.md`, `docs/design/concepts/README.md`,
  `docs/design/stitch/README.md`,
  `scripts/{new-worktree,sync-worktree,cleanup-worktree,launch-feature-session}.sh` y este registro. No
  se toca `init.sh` (D2 revertido), `feature_list.json` ni el código de la app.
- Riesgo o incógnita abierta: el worktree huérfano `chore-gradle-daemon-hang` (PR #53 mergeado) ya es
  limpiable con el script mejorado, pero no se ha borrado en esta sesión. D1 (tamaño de contexto de
  `PROGRESS.md`/`feature_list.json`) queda para un chore aparte.
- Siguiente paso: merge del PR #61 y de este chore; después, D1 (aligerar contexto) y `real-ai-provider`.

### Sesión 064

- Fecha: 2026-10-12
- Objetivo: aligerar el contexto de arranque (D1, punto 1): archivar las sesiones antiguas de
  `PROGRESS.md`.
- Completado: movidas las sesiones **001–045** a `docs/progress/sessions-001-045.md`; `PROGRESS.md`
  conserva el bloque "Estado verificado actual" y las sesiones **046–063** (todas las referenciadas por
  el estado actual) más un puntero al archivo. `PROGRESS.md` pasa de **206 KB a ~70 KB** (−66 %).
- Verificación ejecutada: ninguna de código (cambio solo documental). Comprobado por script: el archivo
  conserva íntegras las sesiones 001–045, `PROGRESS.md` conserva 046–063 y el estado actual, y no hay
  referencias a sesiones ≤045 dentro de `PROGRESS.md`.
- Ficheros actualizados: `PROGRESS.md` y `docs/progress/sessions-001-045.md` (nuevo). No se toca
  `feature_list.json` ni el código de la app.
- Riesgo o incógnita abierta: algunas specs (`docs/specs/*.md`) citan sesiones archivadas (001, 006,
  010, 013, 014, 024, 033); la referencia sigue siendo localizable por el puntero de `PROGRESS.md` y la
  cabecera del archivo. El punto 2 de D1 (evidencia de `feature_list.json`) queda pendiente.
- Siguiente paso: merge de #61/#62/#63; después, D1 punto 2 (si se decide) y `real-ai-provider`.

### Sesión 065

- Fecha: 2026-10-12
- Objetivo: que `github-flow` acepte una ruta externa con los exports de Stitch y los importe al
  worktree, sin copiarlos a mano a `docs/design/concepts/`.
- Completado:
  - Nuevo `scripts/import-stitch-captures.sh <feature-id> <ruta-origen> [--screen <pantalla>]`: importa
    exports (subcarpetas por pantalla, o ficheros sueltos de una pantalla), acepta rutas WSL y Windows,
    valida que cada pantalla esperada tenga png + html + md y devuelve 0 (ok) / 1 (faltan ficheros) /
    2 (uso, origen o worktree).
  - `github-flow` (skill + evals): input opcional `captures_path` para `execute`; en el provisioning, si
    el import valida, resuelve el bloqueo y **lanza la sesión en la misma ejecución**; nueva Eval 7.
  - `AGENTS.md` y `docs/design/concepts/README.md` documentan
    `github-flow execute <feature-id> --captures <ruta>`.
- Verificación ejecutada: `bash -n` de todos los scripts; pruebas del importador (subcarpetas; ficheros
  sueltos con `--screen` y inferidos del README; fichero faltante -> rc 1; ruta Windows inexistente ->
  rc 2).
- Ficheros actualizados: `scripts/import-stitch-captures.sh` (nuevo),
  `.opencode/skills/github-flow/SKILL.md`, `.opencode/skills/github-flow/references/evals.md`,
  `AGENTS.md`, `docs/design/concepts/README.md` y este registro. No se toca `feature_list.json` ni el
  código de la app.
- Riesgo o incógnita abierta: el importador no borra ficheros del destino que no estén en el origen
  (solo copia); si una pantalla cambia de nombre, conviene limpiar la carpeta destino.
- Siguiente paso: `real-ai-provider`.

### Sesión 066

- Fecha: 2026-10-12
- Objetivo: implementar **`real-ai-provider`** (worktree
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/real-ai-provider`, rama
  `feature/real-ai-provider`) según `docs/specs/real-ai-provider.md`: conectar el proveedor real
  (DevExpert Inference, API compatible con OpenAI) **detrás** del puerto `RecipeGenerator`, con Ktor
  Client (engine OkHttp) + `kotlinx.serialization`, política de timeouts/reintentos/degradación en un
  único sitio y verificación JVM con `MockEngine`. Sin commit ni push (los hace el orquestador al
  `accept`).
- Completado:
  - Build: Ktor 3.6.0 en el version catalog (5 librerías), dependencias en `app/build.gradle.kts`
    (`implementation` ×4 + `ktor-client-mock` de test), `buildConfig = true` y
    `buildConfigField("String", "DEVEXPERT_API_KEY", …)` leyendo `local.properties` en tiempo de
    configuración (valor `""` si falta; nunca se imprime), y permiso `INTERNET` en el manifiesto.
  - `com.sazon.app.data.ai`: `AiProviderPolicy` (constantes y decisiones puras), `AiDiagnostics`
    (`AiUnavailableReason`, `AiUnavailableException`, `AiCallOutcome`, `AiOperation`, `AiCallEvent`),
    `OpenAiChat` (DTOs del envelope), `AiRecipePayload` (decode tolerante, valla markdown, parseo
    elemento a elemento con restricciones **neutras**), `AiHttpClientFactory` (OkHttp/Mock,
    `ContentNegotiation` + `HttpTimeout`, `expectSuccess = false`) y `DevExpertRecipeGenerator`
    (3 operaciones, prompt con `PromptBuilder`, deadline total, 1 reintento con backoff/`Retry-After`,
    `CancellationException` siempre relanzada, clave ausente → `MISSING_KEY` sin petición, 200 ilegible
    → lista vacía).
  - `MainActivity`: se sustituye `FakeRecipeGenerator()` por `AiHttpClientFactory.create()` +
    `DevExpertRecipeGenerator(BuildConfig.DEVEXPERT_API_KEY, taxonomy, client, onEvent = Log)`.
  - Tests nuevos en JVM (`MockEngine`, reloj inyectado, clave ficticia `test-key`):
    `AiProviderPolicyTest` (8), `AiRecipePayloadTest` (5) y `DevExpertRecipeGeneratorTest` (15,
    escenarios 1–10 de la spec, incluidas la degradación con `GenerationService` y `AiSearchService`
    reales). **28 tests nuevos.**
- Verificación ejecutada: `bash init.sh` **exit 0**; tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` en verde. Parseo propio
  de los XML: **371 tests/0 fallos/errores/saltados** en 57 suites (343 preexistentes + 28 nuevos) y
  del informe de lint: **0 errores, 13 warnings preexistentes**, ninguno en ficheros de la feature.
  `:app:checkDebugAarMetadata` en verde.
- Hallazgo/desviación mínima registrada: Ktor 3.6.0 arrastra OkHttp **5.5.0**, cuyo artefacto Android
  declara `minCompileSdk=37` y rompía `checkDebugAarMetadata` con el `compileSdk` del repo (API 36.1).
  `app/build.gradle.kts` **fuerza OkHttp 5.4.0** (la última cuyo artefacto Android acepta
  `compileSdk 36`) con `resolutionStrategy.force` y la versión en `gradle/libs.versions.toml`; no se
  cambia el engine ni `compileSdk`. Documentado en `docs/technical-discovery.md` ("Constraints") y
  anotado en la spec (Unknowns). Segundo hallazgo anotado en la spec: cuando lo que impide el reintento
  es el **deadline** (y no el número de intentos), el motivo es `TIMEOUT`.
- Ficheros cambiados: `gradle/libs.versions.toml`, `app/build.gradle.kts`,
  `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/sazon/app/MainActivity.kt`,
  `app/src/main/java/com/sazon/app/data/ai/*` (6 ficheros nuevos),
  `app/src/test/java/com/sazon/app/data/ai/*` (3 suites nuevas), `docs/domain-model.md`,
  `docs/risks-and-open-questions.md` (pregunta 1 → "Resueltas"; tabla sin filas + nota),
  `docs/technical-discovery.md` (Candidate Stack, clave, `data.ai`, Integrations, Observability,
  Testing, Constraints), `docs/specs/real-ai-provider.md` (hallazgos) y este registro +
  `feature_list.json`. **No** se toca `domain/**`, `data/generator/FakeRecipeGenerator.kt`, `ui/**`
  (salvo el cableado de `MainActivity`), las bases Room, la Taxonomía, `seed-recipes.json`,
  `DESIGN.md`, `docs/design/**` ni `init.sh`.
- Riesgo o incógnita abierta: **verificación manual pendiente** (la persona, emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): (1) clave válida → recetas reales que pasan el
  Validador; (2) clave ausente/inválida → modo semilla sin fallar (cambiar `local.properties` exige
  reconstruir). Riesgos de producto ya asumidos en `docs/risks-and-open-questions.md` (clave en el APK,
  cuota semanal, latencia/creatividad del modelo).
- Siguiente paso: **validación independiente** de `real-ai-provider` (validator); después, el
  orquestador de `feature-flow`/`github-flow` hará el commit y publicará el PR.

### Sesión 067

- Fecha: 2026-10-12
- Objetivo: validar `real-ai-provider` de forma independiente y persistir la aceptación, en el worktree
  de la rama `feature/real-ai-provider` (ejecutada por el orquestador de `feature-flow`; solo
  hasta-aceptación, **sin push ni PR**: publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`); `feature_list.json` pasa a `accepted` con evidencia que nombra la validación
  independiente; registrado el detalle en `docs/validations/real-ai-provider.md`. Sin cambios de código
  tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos), `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*" checkDebugAarMetadata`, y
  parseó por su cuenta los XML de resultados y el informe de lint; inspeccionó la política de
  reintentos, el decode tolerante, la degradación por clave ausente, la disciplina de alcance (solo
  `data.ai` importa Ktor) y la ausencia de fuga de la clave.
- Evidencia capturada: **57 suites, 371 tests, 0 fallos/errores/saltados** (28 nuevos de `data.ai`,
  reproducidos exactamente respecto al implementer; 343 preexistentes en verde); lint **0 errores, 13
  warnings** preexistentes y **0 en ficheros de la feature**; `checkDebugAarMetadata` en verde con el
  pin de OkHttp; `domain/**`, `FakeRecipeGenerator`, `ui/**` (salvo el cableado de `MainActivity`), las
  bases Room, la Taxonomía, `seed-recipes.json`, `DESIGN.md`, `docs/design/**` e `init.sh` intactos.
- Hallazgos del validador (3 Low/informativos, ninguno bloqueante): la ruta real del engine OkHttp no se
  cubre en JVM (solo con `MockEngine`) y la verificación E2E es manual, por lo que el pin a OkHttp 5.4.0
  no queda ejercitado automáticamente; `parseContent` usa `runCatching` síncrono (sin impacto práctico);
  `AiCallEvent.detail` para fallos HTTP es solo `HTTP <status>` (cosmético). Acepta como correcta y bien
  documentada la desviación del pin de OkHttp. Sin hallazgos Critical/High/Medium; sin acciones de
  seguridad requeridas (el valor real de `devexpert.apiKey` no aparece en el árbol versionado, logs,
  mensajes de error, tests ni evidencia; `local.properties` sigue ignorado).
- Riesgo o incógnita abierta: la **verificación end-to-end real** es **manual** (la persona, emulador de
  Android Studio en Windows; el `adb` de WSL no lo ve): (1) con `devexpert.apiKey` válida, generar
  recetas reales que pasan el Validador; (2) con la clave ausente o inválida, modo semilla sin fallar
  (cambiar `local.properties` exige reconstruir). Riesgos de producto ya asumidos en
  `docs/risks-and-open-questions.md` (clave en el APK, cuota semanal, latencia/creatividad del modelo).
  Se mantiene el backfill de capturas de UI de features anteriores (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/real-ai-provider` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR) queda para `github-flow`, que nunca hace merge. Con esta
  feature, el plan de features del MVP queda **cerrado** (0 `not_started`, 0 `passing`).

### Sesión 068

- Fecha: 2026-10-12
- Objetivo: implementar la correctiva `ai-provider-timeouts` (issue #68) según
  `docs/specs/ai-provider-timeouts.md`, en la rama `feature/ai-provider-timeouts` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - `AiProviderPolicy` (único sitio de la política): `SOCKET_TIMEOUT_MILLIS` 30 s → **90 s**,
    `REQUEST_TIMEOUT_MILLIS` 40 s → **120 s**, `TOTAL_DEADLINE_MILLIS` 60 s → **180 s** (KDoc con la
    latencia medida: HTTP 200 en 28,0 s / 6.194 tokens, issue #68); `CONNECT_TIMEOUT_MILLIS` 10 s,
    `MAX_ATTEMPTS` 2, `RETRY_BACKOFF_MILLIS` 500 ms y `RETRY_AFTER_MAX_MILLIS` 5 s **sin cambios**.
    `attemptRequestTimeoutMillis` (recorte `min(120 s, restante)`) tampoco cambia de lógica.
  - `AiProviderPolicy.transportFailureReason(cause)` nueva y **pura**: `HttpRequestTimeoutException`,
    `java.net.SocketTimeoutException` e `InterruptedIOException` → `TIMEOUT`; `IOException` genérico
    (incluido el `ConnectTimeoutException` de Ktor, que es un `java.net.ConnectException`) → `NETWORK`;
    cualquier otra causa, incluido `null` → `UNEXPECTED`. Los **dos** bloques de `catch` de `sendAttempt`
    (petición y `bodyAsText()`) pasan por ella, capturando `SocketTimeoutException` **antes** del
    `IOException` genérico y relanzando `CancellationException` primero.
  - KDoc de `AiUnavailableReason.TIMEOUT` (incluye timeout de socket) y `NETWORK` (fallo que no es
    timeout); comentario de `throwDeadlineExhausted` referido a la constante.
  - `AiProviderPolicyTest`: corregida la única aserción que codificaba el valor viejo
    (`attemptRequestTimeoutMillis(REQUEST + 1)`), frontera del recorte (`REQUEST`/`REQUEST ± 1`,
    `TOTAL_DEADLINE_MILLIS`, 0, negativo), test de **holgura** (`REAL_LATENCY_MILLIS = 28_000L`: socket
    ≥ 2×, intento ≥ 3×, margen de reintento, connect 10 s) y test de **clasificación** del fallo de
    transporte (positivos/negativos/nulo).
  - `DevExpertRecipeGeneratorTest`: timeout de socket en los dos intentos → 2 intentos,
    `AiUnavailableException(TIMEOUT)` y evento final `UNAVAILABLE` con `detail = "TIMEOUT"`; timeout de
    socket y luego 200 → 2 intentos, recetas, evento `RETRY` con `detail = "TIMEOUT"` y backoff fijo de
    500 ms; intento lento (35 s de reloj inyectado) → 1 llamada y recetas.
  - Docs durables: `docs/technical-discovery.md` (valores nuevos + clasificación en el párrafo de
    `data.ai`, "Integrations" y "Testing and Verification"), `docs/risks-and-open-questions.md` (bullet
    "Errores y reintentos de la IA" en *Resueltas*) y `docs/specs/real-ai-provider.md` (nota de
    resolución en el Unknown "Latencia real de 6 recetas"; el resto de la spec histórica no se reescribe).
- Verificación ejecutada:
  1. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
     `BUILD SUCCESSFUL`; parseo propio de los XML: **3 suites / 33 tests / 0 fallos / 0 errores /
     0 saltados** (`AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18).
  2. Gate `bash init.sh` → exit 0 (test + lint vía `scripts/gradle.sh`, sin procesos largos); parseo
     propio de los XML: **57 suites / 376 tests / 0 fallos / 0 errores / 0 saltados** (371 preexistentes
     + 5 nuevos, ninguno eliminado); `lint-results-debug.xml`: **0 errores y 13 warnings** preexistentes
     (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0**
     con localización bajo `app/src`.
  3. Prueba de **regresión real** (temporal, revertida): con los valores viejos (socket 30 s) el test de
     holgura **falla** en su primera aserción ("El socket timeout debe ser al menos 2x la latencia
     medida"); con los nuevos queda verde. En `main`, un `SocketTimeoutException` caía en el `catch`
     genérico de `IOException` y se etiquetaba `NETWORK` (hoy `TIMEOUT`).
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt`,
  `.../DevExpertRecipeGenerator.kt`, `.../AiDiagnostics.kt` (modificados),
  `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` y `.../DevExpertRecipeGeneratorTest.kt`
  (modificados), `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `docs/specs/real-ai-provider.md`, `feature_list.json` (`ai-provider-timeouts` a `passing`) y este
  registro.
- Hallazgos relevantes: el `git grep 30_000/40_000/60_000` en `data.ai` deja **dos** coincidencias, ambas
  argumentos `remainingMillis` de `canRetry` en el test preexistente (`60_000L`), ninguna constante de
  timeout; se dejan intactas para no alterar tests aceptados. `AiHttpClientFactory.kt` sigue leyendo las
  constantes (no tiene literales propios), así que no se toca.
- Riesgo o incógnita abierta: la **verificación end-to-end real** sigue siendo **manual** (la persona,
  emulador de Android Studio en Windows; el `adb` de WSL no lo ve): (1) con `devexpert.apiKey` válida, el
  Generador/Home/Búsqueda devuelven recetas reales (~28 s, carga cancelable) sin degradar a modo semilla;
  (2) con la clave ausente, modo semilla sin fallar. La latencia real puede ser mayor y variable (28,0 s
  es **una** medición): si aparecen operaciones > 90 s, la degradación sigue siendo honesta (`TIMEOUT`) y
  la calibración es revisable en el mismo sitio. Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **validación independiente** de `ai-provider-timeouts` con `feature-validator`
  (la feature queda `passing`, **no** `accepted`); después, commit local de la rama (orquestador de
  `feature-flow`) y publicación por `github-flow`.

### Sesión 069

- Fecha: 2026-10-12
- Objetivo: validar `ai-provider-timeouts` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-provider-timeouts` (worktree), solo
  hasta-aceptación; **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`);
  `feature_list.json` pasa a `accepted` con evidencia que nombra la validación independiente; se añade
  `docs/validations/ai-provider-timeouts.md`. Sin cambios de código tras la validación (solo
  estado/evidencia/registro).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`); al ver test/lint en `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), y parseó por su cuenta los XML de
  resultados y el informe de lint. Comprobó con `javap` sobre los jars reales de la caché de Gradle la
  jerarquía de excepciones (`HttpRequestTimeoutException extends java.io.IOException`;
  `ConnectTimeoutException extends java.net.ConnectException`; el engine OkHttp mapea
  `SocketTimeoutException` al typealias de Ktor) y que no hay `withTimeout` ni otro `*_MILLIS` que acote
  al proveedor.
- Evidencia capturada: **57 suites, 376 tests, 0 fallos/errores/saltados** (371 preexistentes + 5 nuevos
  de `data.ai`: `AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18);
  lint 0 errores y 13 warnings preexistentes, 0 bajo `app/src`. Escenarios 1-5 de la spec confirmados
  (holgura con `REAL_LATENCY_MILLIS = 28_000L`; frontera del recorte `REQUEST`/`REQUEST±1`/
  `TOTAL_DEADLINE`/`0`/negativo; tabla de clasificación positiva/negativa/`null` con socket timeout →
  `TIMEOUT` y ya no `NETWORK`; socket+200 → `RETRY`/`detail = "TIMEOUT"` con backoff fijo 500 ms;
  regresión de los 28 escenarios aceptados). Alcance: solo `data.ai`, sus tests y las tres docs;
  `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, `data/generator/**`, Room, assets, build/gradle,
  manifiesto, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas.
  Seguridad: sin clave/token en el diff; `local.properties` ignorado; tests con la clave ficticia
  `test-key`.
- Hallazgos del validador (4 Low/informativos, ninguno bloqueante): redacción imprecisa de la spec sobre
  qué aserciones fallan con los valores viejos (fallan 1, 3 y 4; la 2 se cumple con 40 ≥ 30, y la prueba
  de regresión sigue siendo válida); el test de "intento lento" usa reloj inyectado y no ejercita el
  plugin `HttpTimeout` real (el discriminador viejo-vs-nuevo es el test de holgura y los de socket
  timeout); `docs/specs/real-ai-provider.md` conserva 30/40/60 s en sus secciones históricas a propósito;
  la spec `docs/specs/ai-provider-timeouts.md` estaba sin commitear y el commit de aceptación la incluye.
  Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo. Queda la verificación **manual** de la persona en el
  emulador de Android Studio (Windows; el `adb` de WSL no lo ve): con clave válida, el
  Generador/Home/Búsqueda devuelven recetas reales (~28 s) sin modo semilla y logcat con
  `detail = "TIMEOUT"` (nunca `NETWORK`) si aún se agota; con clave ausente, modo semilla sin fallar.
  Se mantiene el backfill de capturas de UI de features previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/ai-provider-timeouts` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. No hay más features pendientes en el plan.

### Sesión 070

- Fecha: 2026-10-12
- Objetivo: implementar la correctiva `ai-recipe-decode` (issue #70) según
  `docs/specs/ai-recipe-decode.md`, en la rama `feature/ai-recipe-decode` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - **Causa raíz corregida**: `AiRecipePayload.decode` delegaba en `RecipeValidator.validateResponse`,
    que deserializa cada elemento con `Recipe.serializer()`; `Recipe.id`/`source` son **obligatorios** y
    el contrato §4 **no los pide**, así que toda receta caía como `SCHEMA` y la operación quedaba EMPTY.
  - **Capa anti-corrupción en `data.ai`** (sin tocar el dominio):
    - `AiRecipeDraft` (nuevo): DTO del **wire** del contrato §4 (sin `id`/`source`/`matchScore`) que
      reutiliza `RecipeIngredient`/`RecipeStep`/`Nutrition`; `toRecipe(id)` fija `source = RecipeSource.AI`
      y `matchScore = null`. Con `ignoreUnknownKeys = true`, un `id`/`source`/`matchScore` del modelo se
      ignora (los posee la app).
    - `AiRecipeIds` (nuevo): id **determinista** = `ai-` + SHA-256 hex minúscula de la cadena canónica
      (prefijo `ai-recipe-v1`, separadores `U+001F`/`U+001E`), **sin `Random`, reloj ni `UUID`**; un
      duplicado exacto en la misma llamada se desambigua con `-2`/`-3`… (determinista por orden).
    - `AiRecipePayload.decode` reescrito: `content` nulo/blanco → vacío; valla markdown (se conserva);
      parseo tolerante del root `{"recipes":[...]}`; elemento a elemento con `AiRecipeDraft` (el que no
      encaja se salta = descarte SCHEMA sin tumbar el resto); la app completa `id`/`source` **antes** de
      `RecipeValidator.validateRecipe(recipe, taxonomy, AiProviderPolicy.NEUTRAL_CONSTRAINTS)`; nunca
      lanza. KDoc actualizado.
  - **Tests con la forma REAL del contrato** (JSON **literal** sin `id`/`source`; nunca
    `RecipeJson.encode`, que era el punto ciego de #70): `AiRecipePayloadTest` reescrito (8) con los
    escenarios 1, 2, 4, 5 y 6; `AiRecipeIdsTest` nuevo (6) con el escenario 3; y
    `DevExpertRecipeGeneratorTest` ampliado (19) con el escenario 7 (regresión end-to-end con
    `GenerationService` real). Los 18 tests previos de esa suite se conservan intactos (siguen usando su
    envelope con `RecipeJson.encode`, que ejercita el camino con `ingredientId` para dieta/restricciones).
  - **Docs durables**: `docs/technical-discovery.md` (párrafo de `data.ai` + *Integrations* + *Testing and
    Verification*), `docs/domain-model.md` (drift del `id` corregido: la app asigna `id` y `source = ai`
    **al decodificar** la respuesta, no "al persistir") y `docs/domain/taxonomy.md` §4 (una línea: `id`,
    `source` y `matchScore` los pone la app; el modelo no debe enviarlos).
- Verificación ejecutada:
  1. Reproducción del bug con un **test temporal** (creado y borrado en esta sesión): sobre el mismo JSON
     literal sin `id`/`source`, el camino viejo (`RecipeValidator.validateResponse`, el de `main`) acepta
     **0** y descarta 1 como `SCHEMA`, mientras `AiRecipePayload.decode` acepta **1**. `BUILD SUCCESSFUL`.
  2. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
     `BUILD SUCCESSFUL`; parseo propio de los XML: **4 suites / 43 tests / 0 fallos / 0 errores /
     0 saltados** (`AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8,
     `DevExpertRecipeGeneratorTest` 19).
  3. Gate `bash init.sh` → **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); parseo
     propio de los XML: **58 suites / 386 tests / 0 fallos / 0 errores / 0 saltados** (376 preexistentes +
     10 nuevos, ninguno eliminado); `lint-results-debug.xml`: **0 errores y 13 warnings** preexistentes
     (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0**
     con localización bajo `app/src`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/{AiRecipeDraft,AiRecipeIds}.kt`
  (nuevos), `.../AiRecipePayload.kt` (modificado), `app/src/test/java/com/sazon/app/data/ai/{AiRecipeIdsTest}.kt`
  (nuevo), `.../{AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt` (modificados),
  `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/domain/taxonomy.md`,
  `docs/specs/ai-recipe-decode.md` (hallazgo), `feature_list.json` (`ai-recipe-decode` a `passing`) y este
  registro. **No** se toca `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiProviderPolicy.kt`,
  `AiDiagnostics.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`,
  `domain/**`, `data/generator/**`, `ui/**`, las bases Room, `app/src/main/assets/**`,
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
  `DESIGN.md` ni `docs/design/**`; sin dependencias nuevas; sin capturas de UI (no es feature de UI).
- Inspección de decisiones: `git grep "validateResponse" app/src/main` deja solo la **definición** en
  `RecipeValidator.kt` y dos comentarios KDoc (`GenerationService`, `AiSearchService`): sin llamador real
  (decisión documentada; el método no se modifica y sigue cubierto por `RecipeValidatorTest`). El esquema
  **no** se debilita: `id`/`source` siguen obligatorios en `Recipe`.
- Hallazgo de spec anotado: la lista de campos de `AiRecipeIds.canonical` **omitía `tips`**, pero el
  *Unknown* de la spec afirma que "un cambio de contenido (p. ej. tips) cambia el `id`". Se incluyen los
  `tips` en la cadena canónica (de lo contrario, recetas que solo difieren en los trucos compartirían
  `id` entre llamadas) y el test `AiRecipeIdsTest.contenido_distinto_produce_id_distinto` lo cubre.
- Riesgo o incógnita abierta: la **verificación manual** sigue siendo de la persona (emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, la búsqueda *"arroz con
  pollo"* y el Generador muestran recetas reales que pasan el Validador (logcat `SUCCESS` en vez de
  `EMPTY`); con la clave vaciada o ausente, modo semilla sin fallar. Sigue vigente el *Unknown* de
  robustez del modelo fuera del contrato §4 (un campo obligatorio ausente/mal tipado descarta esa receta;
  no tumba el lote) y el de estabilidad de ids (un cambio de contenido cambia el `id`; el Favorito
  conserva su copia). Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **validación independiente** de `ai-recipe-decode` con `feature-validator` (la feature
  queda `passing`, **no** `accepted`); después, commit local de la rama `feature/ai-recipe-decode`
  (orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 071

- Fecha: 2026-10-12
- Objetivo: validar `ai-recipe-decode` (issue #70) de forma independiente y persistir la aceptación.
  Ejecutada por el orquestador de `feature-flow` en la rama `feature/ai-recipe-decode` (worktree), solo
  hasta-aceptación; **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`); `feature_list.json`
  pasa a `accepted` con evidencia que nombra la validación independiente; se añade
  `docs/validations/ai-recipe-decode.md`. Sin cambios de código tras la validación (solo
  estado/evidencia/registro).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`); al ver test/lint en `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas, 2m39s) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas, 2m43s), y parseó por su cuenta los XML de
  resultados y el informe de lint. Reprodujo además el bug y el arreglo con un verificador Java propio
  fuera del repo (`/tmp/opencode/sazon-verify/`, JDK 21 sobre las clases compiladas + `taxonomy.json`
  real; ningún fichero del repo tocado), con **16/16 comprobaciones**.
- Evidencia capturada: **58 suites, 386 tests, 0 fallos/errores/saltados** (376 preexistentes + 10 nuevos
  de `data.ai`: `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8,
  `DevExpertRecipeGeneratorTest` 19); lint 0 errores y 13 warnings preexistentes, 0 bajo `app/src`.
  Escenarios 1-7 confirmados: forma real del contrato → recetas válidas con `source = AI`, id `ai-<sha256>`
  y `matchScore = null`; id único con duplicado literal (`-2`/`-3`) y determinista por contenido; reglas
  del Validador vivas (ids de Taxonomía inexistentes → `SCHEMA`; deriva de tiempos → `INCOHERENT`; un
  elemento roto no tumba el lote); tolerancia sin lanzar; y regresión end-to-end con `GenerationService`
  real. **Discriminador de #70** verificado de forma independiente: el mismo JSON literal sin `id`/`source`
  por el camino de `main` (`RecipeValidator.validateResponse`) da `accepted 0` / 1 descarte `SCHEMA`,
  mientras `AiRecipePayload.decode` da 1 receta. Alcance: solo `data.ai` (+2 nuevos), sus 3 tests, 3 docs
  y harness; `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiProviderPolicy.kt`, `AiDiagnostics.kt`,
  `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`, `domain/**`, `ui/**`,
  `data/generator/**`, Room, assets, build/gradle, manifiesto, `init.sh`, `scripts/**`, `DESIGN.md` y
  `docs/design/**` intactos; sin dependencias nuevas. Seguridad: sin claves/secretos en el diff;
  `local.properties` ignorado; tests con `test-key`; el `id` es un hash one-way sin PII.
- Hallazgos del validador (3 Low + varios informativos, ninguno bloqueante): (1) la cadena canónica del
  `id` omite `RecipeStep.description` (dos recetas que solo difieren en la prosa de un paso comparten `id`
  base entre llamadas; impacto bajo, cumple la lista de campos de la spec; sugerencia de correctiva
  futura con vector dorado); (2) la frase añadida a `docs/domain/taxonomy.md` §4 sobre `matchScore`
  convive con el prompt real que todavía lo pide (drift preexistente, `PromptBuilder` fuera de alcance);
  (3) falta vector dorado del hash y un test de que un `id`/`source` del modelo se ignora (verificado
  externamente por el validador). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: la **verificación manual** sigue siendo de la persona (emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, la búsqueda *"arroz con
  pollo"* y el Generador muestran recetas reales que pasan el Validador (logcat `SUCCESS` en vez de
  `EMPTY`); con la clave vaciada o ausente, modo semilla sin fallar. Se mantiene el backfill de capturas
  de UI de features previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/ai-recipe-decode` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR) queda para `github-flow`, que nunca hace merge. No hay más
  features pendientes en el plan.

### Sesión 072

- Fecha: 2026-10-12
- Objetivo: implementar la correctiva `ai-call-diagnostics` (issue #72) según
  `docs/specs/ai-call-diagnostics.md`, en la rama `feature/ai-call-diagnostics` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - `AiProviderPolicy` (único sitio de la política): `SOCKET_TIMEOUT_MILLIS` **90 s → 120 s**,
    `REQUEST_TIMEOUT_MILLIS` **120 s → 150 s**, `TOTAL_DEADLINE_MILLIS` **180 s → 240 s**;
    `CONNECT_TIMEOUT_MILLIS` (10 s), `MAX_ATTEMPTS` (2), `RETRY_BACKOFF_MILLIS` (500 ms) y
    `RETRY_AFTER_MAX_MILLIS` (5 s) **intactos**, y `attemptRequestTimeoutMillis`/`canRetry`/`Retry-After`
    sin cambio de lógica. KDoc con la evidencia multi-modelo (peor caso `mimo-v2.6-pro` 115,8 s; alias
    diario `chat` 28,0/33,1/42,1/46,6 s; `chat`+`json_object` 85,3 s; `chat-pro`+`json_schema` 97,0 s;
    `deepseek-v4-flash` 52,6 s).
  - **Diagnóstico de `EMPTY`**: `AiDecodeResult.kt` nuevo (`internal enum AiContentState`
    BLANK/ILLEGIBLE/RECIPES + `internal data class AiDecodeResult` con `accepted`, `received`,
    `discardedByReason`, `contentChars`, `contentBytes`, `contentState`). `AiRecipePayload.decode` deja
    de devolver `List<Recipe>` y devuelve el resultado: cuenta `received`/`accepted`, cuenta los
    descartes **por motivo** (solo motivos presentes, en orden de `DiscardReason`, perdiendo el `detail`
    a propósito) y calcula longitudes UTF-16/UTF-8 (`0/0` si el contenido está en blanco); sigue **sin
    lanzar** y con `constraints` por defecto `NEUTRAL_CONSTRAINTS` (el proveedor **no** pre-filtra: el
    estado Bloqueado de `ai-search` se conserva).
  - `AiDiagnostics.kt`: `AiCallEvent` gana **5 campos aditivos** con default (`contentBytes`,
    `contentChars`, `received`, `accepted` = `null`; `discardedByReason` = `emptyMap()`);
    `DevExpertRecipeGenerator.report(...)` los rellena desde el resultado del decode (`null`/vacío
    cuando no hubo decode: clave ausente, `RETRY`, `UNAVAILABLE`, deadline). `MainActivity`:
    `Log.d("SazonAi", …)` pasa a mostrar `content=…c/…b`, `received=…`, `accepted=…` y `discards={…}`.
    **Nunca** clave, `Authorization`, cuerpos ni títulos: solo longitudes, recuentos y motivos.
  - Tests (7 nuevos): `AiProviderPolicyTest` recalibrado (`REAL_LATENCY_MILLIS = 115_800L` +
    `CHAT_WORST_LATENCY_MILLIS = 46_600L`, invariantes y márgenes de Scenario 1, frontera del recorte);
    `AiRecipePayloadTest` 11 (adaptado a `.accepted` + resumen con los **cuatro** `DiscardReason`
    representables con restricciones no neutras y en orden del enum + `contentState` con longitudes
    UTF-16 vs UTF-8 + tolerancia/ids/reglas/`matchScore`); `DevExpertRecipeGeneratorTest` 23
    (escenario 2 `EMPTY` por vacío/ilegible con longitudes y 0/0, escenario 3 lote parcial con
    `{SCHEMA:2, INCOHERENT:1}`, escenario 4 todas descartadas → `EMPTY` con motivos sin lanzar y
    escenario 5 no-fuga de clave/cabecera/cuerpos/títulos).
  - Docs durables: `docs/technical-discovery.md` (párrafo de `data.ai` con 120/150/240 y el decode
    tipado, *Integrations*, *Observability* con los campos nuevos y el detalle de `EMPTY`, *Testing and
    Verification* con el peor caso y los tests del evento), `docs/risks-and-open-questions.md` (bullet
    "Errores y reintentos de la IA"), y notas de *Unknown* en `docs/specs/real-ai-provider.md` (latencia
    y `AiCallEvent`) y `docs/specs/ai-provider-timeouts.md` (latencia y "el motivo no se registraba").
- Verificación ejecutada:
  1. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
     `BUILD SUCCESSFUL`; **50 tests / 0 fallos** (`AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6,
     `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 23).
  2. Prueba de **regresión real** (temporal, revertida): con los valores viejos (90/120/180) el test de
     holgura **falla** en su primera aserción ("El socket timeout debe superar el peor caso medido
     (mimo-v2.6-pro, 115,8 s)"; `90_000 > 115_800` es falso); con los nuevos queda verde.
  3. Gate `bash init.sh` → **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos);
     parseo propio de los XML: **58 suites / 393 tests / 0 fallos / 0 errores / 0 saltados** (386
     preexistentes + 7 nuevos, ninguno eliminado); `lint-results-debug.xml`: **0 errores** y **7
     warnings** preexistentes (GradleDependency 5, NewerVersionAvailable 1, OldTargetApi 1), todos en el
     build/version catalog y **0 bajo `app/src`** (la diferencia frente a los 13 históricos es del
     chequeo de versiones disponibles de lint, que depende de su caché de red; ningún warning nuevo).
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` (nuevo),
  `.../AiProviderPolicy.kt`, `.../AiRecipePayload.kt`, `.../AiDiagnostics.kt`,
  `.../DevExpertRecipeGenerator.kt` y `app/src/main/java/com/sazon/app/MainActivity.kt` (log),
  `app/src/test/java/com/sazon/app/data/ai/{AiProviderPolicyTest,AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt`,
  las cuatro docs y este registro + `feature_list.json`. **No** se toca `Recipe.kt`,
  `RecipeValidator.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`,
  `domain/**`, `data/generator/**`, `ui/**`, las bases Room, `app/src/main/assets/**`,
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
  `DESIGN.md` ni `docs/design/**`; sin dependencias nuevas; sin capturas de UI (no es feature de UI).
- Hallazgo relevante (spec actualizada): el `internal` que la spec proponía para
  `AiDecodeResult`/`AiContentState` **no compila** con `AiRecipePayload.decode` público
  (`'public' function exposes its 'internal' return type`). Resuelto con el criterio que la spec pide:
  los dos tipos siguen `internal` y `decode` pasa a `internal fun` (el objeto `AiRecipePayload` sigue
  público); los tests JVM del mismo módulo lo usan por los *friend paths*, igual que ya hacían con
  `AiRecipeIds`/`AiRecipeDraft`. Anotado en "Hallazgos de la sesión de implementación" de la spec.
- Riesgo o incógnita abierta: la **verificación manual** sigue siendo de la persona (emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, logcat (`SazonAi`) debe
  mostrar por llamada estado, intento, duración, `content=…c/…b`, `received=… accepted=…` y
  `discards={…}`, con las recetas reales llegando a Resultados; con un lote sin recetas aceptadas el log
  debe indicar el motivo; con la clave ausente o vaciada sigue el modo semilla. Sigue vigente el margen
  estrecho con `mimo-v2.6-pro` (115,8 s vs socket 120 s: otra medición por encima degradará como
  `TIMEOUT` honesto, revisable en el mismo sitio) y el `Unknown` de que `DIET_PATTERN`/`HARD_RESTRICTION`
  no son alcanzables en producción (restricciones neutras). Se mantiene el backfill de capturas de UI
  (sesión 046).
- Siguiente paso: **validación independiente** de `ai-call-diagnostics` con `feature-validator` (la
  feature queda `passing`, **no** `accepted`); después, commit local de la rama
  `feature/ai-call-diagnostics` (orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 073

- Fecha: 2026-10-12
- Objetivo: validar `ai-call-diagnostics` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-call-diagnostics` (worktree), solo
  hasta-aceptación; **sin push ni PR** (la publicación es de `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`); `feature_list.json`
  pasa a `accepted` con la evidencia de la validación independiente; sin cambios de código tras la
  validación. Commit local de la rama `feature/ai-call-diagnostics` (lo hace el orquestador).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos) y, al ver la primera pasada en `UP-TO-DATE`, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con **parseo propio** de los XML y del
  informe de lint.
- Evidencia capturada: **58 suites / 393 tests / 0 fallos/errores/saltados** (386 preexistentes + 7
  nuevos de `data.ai`: `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11,
  `DevExpertRecipeGeneratorTest` 23; regresión `RecipeValidatorTest` intacta); lint 0 errores y 10
  warnings (todos en el build/version catalog, **0 bajo `app/src`**). Reprodujo la **regresión de forma
  independiente por aritmética**: con los valores viejos (90/120/180) fallan `SOCKET > peor caso`
  (`90_000 > 115_800` falso), `SOCKET ≥ 2×` el peor del alias `chat` (`90_000 ≥ 2×46_600` falso) y el
  margen de reintento (`180_000-115_800-500 ≥ 115_800` falso); con 120/150/240 las tres son ciertas.
  Confirmó los escenarios 1-6 (holgura con `REAL_LATENCY_MILLIS` = 115_800 y frontera del recorte;
  `EMPTY` por vacío/ilegible con `received=0`/`accepted=0` y longitudes `0/0` vs `>0` y 1 sola llamada;
  lote parcial `received=4 accepted=1 {SCHEMA:2, INCOHERENT:1}`; los **cuatro** `DiscardReason`
  representables con restricciones no neutras y en orden del enum; todas descartadas → `EMPTY` con
  motivos sin lanzar; no-fuga: ningún `toString()` de evento contiene `test-key`, `Bearer`/`Authorization`,
  el cuerpo del modelo ni títulos), que `AiProviderPolicy` sigue siendo el **único** sitio de la política
  (connect 10 s, `MAX_ATTEMPTS` 2, backoff 500 ms, `Retry-After` ≤ 5 s y
  `attemptRequestTimeoutMillis`/`canRetry` sin cambios), que `decode` no lanza y usa restricciones neutras
  por defecto (el estado Bloqueado de `ai-search` se conserva) y que `response_format` queda fuera.
  Disciplina de alcance verificada (solo `data.ai` + el `Log.d` de `MainActivity` + 4 docs; `Recipe.kt`,
  `RecipeValidator.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`,
  `domain/**`, `ui/**`, Room, Taxonomía, assets, build/version catalog, `AndroidManifest.xml`, `init.sh`,
  `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas; sin capturas de UI) y
  seguridad (sin claves/secretos en el diff; `local.properties` ignorado; tests con `test-key`; el evento
  no expone clave, `Authorization`, cuerpos ni títulos). Detalle en
  `docs/validations/ai-call-diagnostics.md`.
- Hallazgos del validador (3 Low informativos, ninguno bloqueante): `contentState` (BLANK vs ILLEGIBLE)
  no viaja en el evento (la distinción va por las longitudes `0/0` vs `>0`); el número de warnings de
  lint oscila con su caché de red (siempre 0 bajo `app/src`); `AiRecipePayload.decode` pasó de `public`
  a `internal` (con `AiDecodeResult`/`AiContentState`) para no exponer un tipo `internal` desde una API
  pública, sin llamadores fuera del módulo. Sin hallazgos Critical/High/Medium.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/ai-call-diagnostics.md` (nuevo) y este registro. Sin cambios de código tras la
  validación.
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la **verificación manual** en el
  emulador de Android Studio (Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, logcat
  (`SazonAi`) muestra por llamada estado, intento, duración, `content=…c/…b`, `received=… accepted=…` y
  `discards={…}`, y las recetas reales llegan a Resultados; con un lote sin recetas aceptadas el log
  indica el motivo; con la clave ausente o vaciada sigue el modo semilla. Se mantiene el margen estrecho
  con `mimo-v2.6-pro` (115,8 s vs socket 120 s) y el `Unknown` de que `DIET_PATTERN`/`HARD_RESTRICTION`
  no son alcanzables en producción (restricciones neutras). Se mantiene el backfill de capturas de UI
  (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/ai-call-diagnostics` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, sin merge) queda para `github-flow`. No hay más features
  del plan del MVP; si aparece un defecto visual/manual, se corrige con una feature correctiva nueva.

### Sesión 074

- Fecha: 2026-10-12
- Objetivo: implementar la cuarta **correctiva** de `real-ai-provider`, `ai-response-format` (issue #74),
  según `docs/specs/ai-response-format.md`, en la rama `feature/ai-response-format` (worktree); solo
  hasta implementación y autoverificación (`passing`), sin commit/push/PR (los hace el orquestador).
  `area: integration`, **no** es UI (sin capturas).
- Completado:
  - `AiProviderPolicy`: `RESPONSE_FORMAT_ENABLED = true`, `RESPONSE_FORMAT_JSON_OBJECT = "json_object"` y
    `requiresResponseFormatFallback(status) = status == 400 || status == 422`. **Sin** tocar
    `isRetryableStatus`, timeouts (120/150/240), `MAX_ATTEMPTS`/backoff/`Retry-After`.
  - `OpenAiChat`: `ResponseFormat` (+ `JSON_OBJECT`) y `ChatCompletionRequest.responseFormat` con
    `@SerialName("response_format") @EncodeDefault(EncodeDefault.Mode.NEVER)` y default `null`; `@OptIn`
    a nivel de fichero. La KDoc deja de afirmar que no se envía `response_format`.
  - `AiDiagnostics`: `enum AiResponseFormat { JSON_OBJECT, NONE }`, `AiCallOutcome.FALLBACK` y
    `AiCallEvent.responseFormat: AiResponseFormat = NONE` (aditivo con default).
  - `DevExpertRecipeGenerator`: costura `responseFormatEnabled` por constructor (default de la política);
    `sendAttempt(prompt, remaining, includeFormat)` monta `ResponseFormat.JSON_OBJECT` solo si
    `includeFormat`; rama de fallback (400/422, **una vez** por operación, sin backoff, `attempt += 1`)
    antes de la lógica de transporte; `formatOf(includeFormat)` en todas las llamadas a `report`.
  - `MainActivity`: `format=${event.responseFormat}` en el `Log.d("SazonAi", …)` (único cambio fuera de
    `data.ai`).
  - Tests: `OpenAiChatTest` (nuevo, Scenario 3) y casos de formato/fallback/observabilidad en
    `DevExpertRecipeGeneratorTest` (Scenarios 1, 2, 4, 5, 6 y 7; el antiguo `nunca_reintenta_un_4xx_no_reintentable`
    se acota a 401/403/404 porque 400/422 ahora disparan el fallback) y `requiresResponseFormatFallback`/
    interruptor por defecto en `AiProviderPolicyTest`.
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.data.ai.*"` (BUILD SUCCESSFUL) y gate estándar `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos); parseo propio de los XML y del informe de lint.
- Evidencia capturada: **61 tests / 0 fallos en `data.ai`** (50 preexistentes + 11 nuevos); gate
  **59 suites / 404 tests / 0 fallos / 0 errores / 0 saltados** (393 preexistentes + 11 nuevos); lint
  **0 errores y 13 warnings preexistentes** (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`** y 0 nuevos por
  `@EncodeDefault`/`@OptIn`; `git diff --stat` solo con lo previsto y `grep response_format` restringido
  a `data.ai` (código y tests) más la KDoc del fallback.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/{AiProviderPolicy,OpenAiChat,
  AiDiagnostics,DevExpertRecipeGenerator}.kt` y `app/src/main/java/com/sazon/app/MainActivity.kt`
  (modificados); `app/src/test/java/com/sazon/app/data/ai/{AiProviderPolicyTest,DevExpertRecipeGeneratorTest}.kt`
  (modificados) y `app/src/test/java/com/sazon/app/data/ai/OpenAiChatTest.kt` (nuevo); `docs/technical-discovery.md`,
  `docs/risks-and-open-questions.md`, `docs/adr/0003-ai-provider.md`, `docs/specs/real-ai-provider.md`,
  `docs/specs/ai-call-diagnostics.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: el test `nunca_reintenta_un_4xx_no_reintentable` agrupaba 400/422 con los 4xx
  no reintentables; con el formato activo pasan a 2 llamadas (fallback), así que se acotó a 401/403/404 y
  el comportamiento nuevo se cubre con tests propios. La primera pasada falló una aserción ingenua
  (`body.contains("null")`) porque el **prompt** enumera la unidad `null`: se sustituyó por el parseo
  preciso de `response_format` (`OpenAiChatTest` cubre el negativo de `"response_format":null`).
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (latencia de `json_object` 85,3 s
  frente a `chat` 28,0–46,6 s; un 400/422 no relacionado con el formato también dispararía el fallback,
  coste de una petición extra con rastro `FALLBACK`; `FALLBACK` no se reintenta si el 2.º intento también
  da 400/422). Queda la verificación **manual** de emulador de la persona (logcat `format=…`/`FALLBACK`;
  interruptor apagado idéntico; clave ausente → modo semilla), no automatizable desde WSL. Se mantiene el
  backfill de capturas de UI (sesión 046).
- Siguiente paso: validación independiente de `ai-response-format` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 075

- Fecha: 2026-10-12
- Objetivo: **reparar** `ai-response-format` (issue #74) tras el veredicto **`revise`** del validador
  independiente (`docs/validations/ai-response-format.md`, Finding 1 **Medium**): el fallback de formato
  podía añadir un **tercer** intento HTTP y romper la invariante `MAX_ATTEMPTS = 2`. Solo hasta
  implementación y autoverificación (`passing`), sin commit/push/PR.
- Diagnóstico: en `DevExpertRecipeGenerator.execute`, la condición del fallback
  (`includeFormat && requiresResponseFormatFallback(status) && !usedFallback`) **no** consultaba el
  presupuesto de intentos. Con `503 → 400` (o `IOException → 422`), el primer intento consumía el
  reintento de transporte (`attempt = 2`) y el `400/422` del segundo disparaba igualmente el fallback
  (`attempt` pasaba a 3) → `calls = 3`, `formats = [json_object, json_object, null]`.
- Reparación aplicada:
  - La rama `AttemptOutcome.HttpFailure` añade la guarda `attempt < AiProviderPolicy.MAX_ATTEMPTS` a la
    condición del fallback y se elimina `!usedFallback` (redundante: `includeFormat` ya lo implica).
  - Un `400/422` sin presupuesto cae en la rama actual → `UNAVAILABLE`/`HTTP_ERROR` con el formato del
    intento (`JSON_OBJECT`), **sin** tercera petición.
  - KDoc del generador precisada (el fallback solo consume el 2.º intento si queda presupuesto).
  - Docs durables matizadas para que el enunciado sea coherente con la cota: `docs/risks-and-open-questions.md`
    y `docs/technical-discovery.md` (`MAX_ATTEMPTS = 2`).
- Test de regresión nuevo: `DevExpertRecipeGeneratorTest.el_fallback_no_anade_un_tercer_intento` —
  `503 → 400` y `IOException → 422` ⇒ exactamente **2** llamadas, sin evento `FALLBACK`, evento `RETRY`
  de transporte presente, `UNAVAILABLE`/`HTTP_ERROR` y evento final con `responseFormat = JSON_OBJECT`.
  Los tests del fallback aislado (`400/422` en el 1.er intento ⇒ 2 llamadas, la 2.ª sin la clave) siguen
  intactos y en verde.
- Verificación ejecutada: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`
  (BUILD SUCCESSFUL) y gate estándar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`, sin
  procesos largos); parseo propio de los XML y del informe de lint.
- Evidencia capturada: **62 tests / 0 fallos en `data.ai`** (61 previos + 1 nuevo;
  `DevExpertRecipeGeneratorTest` 31); gate **59 suites / 405 tests / 0 fallos / 0 errores / 0 saltados**
  (404 previos + 1 nuevo); lint **0 errores y 13 warnings preexistentes** (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`**.
- Ficheros cambiados: `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` y
  `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt`, más
  `docs/risks-and-open-questions.md` y `docs/technical-discovery.md`; `feature_list.json` y este registro.
  Sin tocar `isRetryableStatus`, timeouts 120/150/240, `MAX_ATTEMPTS`, backoff, `Retry-After`,
  `Recipe.kt`/`RecipeValidator.kt`, `AiRecipePayload`/`AiDecodeResult`/`AiHttpClientFactory`, `ui/**`,
  Room, Taxonomía, build, dependencias, `init.sh`, `scripts/**`, `DESIGN.md` ni `docs/design/**`.
- Hallazgos relevantes: el camino mixto (reintento de transporte + `400/422`) no estaba cubierto por
  ningún test; el nuevo caso lo fija. La interpretación elegida es la de la spec (el fallback **consume**
  el 2.º intento, no lo añade), no la alternativa de ampliar la spec a 3 peticiones.
- Siguiente paso: **re-validación independiente** de `ai-response-format` con `feature-validator`
  (puede ser focal: una condición + un test); después, commit local de la rama (lo hace el orquestador de
  `feature-flow`) y publicación por `github-flow`. Se mantiene la verificación **manual** de emulador de
  la persona (no automatizable desde WSL) y el backfill de capturas de UI (sesión 046).

### Sesión 076

- Fecha: 2026-10-12
- Objetivo: validar `ai-response-format` (correctiva de `real-ai-provider`, issue #74) de forma
  independiente y persistir la aceptación, en el worktree de la rama `feature/ai-response-format`;
  modo hasta-aceptación, sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`). La primera validación había devuelto **`revise`** (Finding 1 **Medium**: el
  fallback podía añadir un tercer intento HTTP en secuencia mixta), se reparó en la sesión 075
  (`attempt < AiProviderPolicy.MAX_ATTEMPTS` + test de regresión) y la re-validación confirmó el arreglo.
  `feature_list.json` pasa a `accepted` con la evidencia del validador independiente; detalle en
  `docs/validations/ai-response-format.md`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos) y, al ver UP-TO-DATE, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML y el informe
  de lint, e **independientemente reprodujo el Fix** con una sonda propia en copia scratch (7/7).
- Evidencia capturada: **59 suites / 405 tests / 0 fallos/errores/saltados** (62 en `data.ai`:
  `AiProviderPolicyTest` 12, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest`
  31, `OpenAiChatTest` 2); lint **0 errores, 13 warnings** preexistentes, **0 bajo `app/src`**. La sonda
  del validador: mixto `503→400` y `IOException→422` ⇒ `calls=2` sin `FALLBACK` y `HTTP_ERROR` (el bug
  `calls=3` ya no reproduce); fallback aislado `400/422` ⇒ `calls=2` con `FALLBACK`+`SUCCESS`; persistente
  `400` y `fallback_luego_429` ⇒ `calls=2` (sin tercera); transporte `429` persistente ⇒ `calls=2` ambas
  con formato y `RATE_LIMITED`. Scenarios 1-8 confirmados; `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, Room,
  Taxonomía, `seed-recipes.json`, build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**`
  intactos; sin dependencias nuevas; sin capturas (no es UI). Sin hallazgos Critical/High/Medium
  abiertos; 2 Low informativos no bloqueantes.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/ai-response-format.md` y este registro. Sin cambios de código.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (latencia de `json_object` 85,3 s
  frente a `chat` 28,0–46,6 s; un 400/422 no relacionado con el formato dispara igualmente el fallback,
  con rastro `FALLBACK`; `FALLBACK` no se reintenta si el 2.º intento también da 400/422). Queda la
  verificación **manual** de emulador de la persona (logcat `format=…`/`FALLBACK` con recetas reales;
  interruptor apagado → comportamiento idéntico; clave ausente o vaciada → modo semilla), no
  automatizable desde WSL. Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/ai-response-format` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, sin merge) queda para `github-flow`. No hay más features
  del plan del MVP; el plan sigue **cerrado**.
### Sesión 077

- Fecha: 2026-10-12
- Objetivo: implementar y autoverificar `recipe-artwork` (placeholder de receta por categoría y cocina,
  issue #77, `area: ui`) según `docs/specs/recipe-artwork.md`, en la rama `feature/recipe-artwork`
  (worktree); solo hasta implementación y autoverificación (`passing`), sin commit/push/PR (los hace el
  orquestador).
- Completado: paquete nuevo `com.sazon.app.ui.artwork` con **mapeo puro** y **un** componente Compose.
  `RecipeArtworkStyle` (sin Compose/Android, solo `domain.recipe.Recipe`) fija `RecipeGlyph` (18
  miembros ↔ 18 `recipeCategories` reales, `PLATE` = `other` **y** fallback), `RecipeAccent` (10
  acentos ↔ 10 `cuisines` + `DEFAULT`), `RecipeStyle` y `glyphFor`/`accentFor`/`of(recipe)` con
  **fallback total** (`null`/vacío/desconocido → `PLATE` + `DEFAULT`; nunca vacío ni excepción).
  `RecipeArtwork(recipe, modifier, glyphSize = 48.dp)` pinta el bloque con `Brush.linearGradient` de
  **roles del tema** (`primaryContainer`/`secondaryContainer`/`tertiaryContainer`/`surfaceVariant`) y
  un `RecipeGlyphCanvas` privado con los 18 glifos de línea (trazo 2 dp, coordenadas normalizadas,
  `PLATE` = los dos círculos concéntricos de siempre). `RecipeArtworkDefaults.GlyphSize` es el 48 dp
  de la tarjeta. Se sustituyen las **4 copias** y se eliminan los `PlateGlyph` privados:
  `RecipeCard` (16:10, 48 dp), `ResultRecipeCard` (16:10, 56), `FavoriteRecipeCard` (miniatura 96x96
  con `clip(SazonShapes.small)` y bloque de la rejilla de alto 96, 40; se **conservan**
  `HeartGlyph`/`HeartButton`/`DismissBackground` y el corazón superpuesto) y `DetailHero` (240 dp,
  72 dp; `DetailHero` ahora recibe `recipe` y "Atrás"/corazón siguen siendo hermanos). Se limpian los
  imports huérfanos (`Canvas`/`Brush`/`Color`/`Stroke`/`Alignment`/`size`/`Sazon*Container` según el
  fichero). Sin dependencias nuevas, sin assets y sin hex sueltos. `RecipeArtworkStyleTest` itera la
  **Taxonomía real**.
- Verificación ejecutada: tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` (BUILD SUCCESSFUL) y
  gate estándar `bash init.sh` (exit 0, `./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin
  procesos largos); parseo propio de los XML de `app/build/test-results/testDebugUnitTest/` y del
  informe `app/build/reports/lint-results-debug.xml`; inspección de pureza, de ausencia de hex y de
  `git diff --stat`.
- Evidencia capturada: **60 suites / 413 tests / 0 fallos / 0 errores / 0 saltados** (405 previos + 8
  nuevos de `RecipeArtworkStyleTest`: `cada_categoria_real_de_la_taxonomia_tiene_su_glifo_sin_duplicados`,
  `cada_cocina_real_de_la_taxonomia_tiene_su_acento_sin_duplicados`, `el_mapeo_es_determinista`,
  `un_id_nulo_vacio_o_desconocido_cae_al_fallback_de_plato_y_degradado_por_defecto`,
  `el_fallback_reproduce_el_estilo_de_la_categoria_other`,
  `un_id_desconocido_no_devuelve_el_estilo_de_ninguna_categoria_o_cocina_concreta`,
  `el_estilo_de_una_receta_sale_de_su_categoria_y_su_cocina`,
  `una_receta_sin_clasificar_no_lanza_y_usa_el_fallback`). El test lee
  `docs/domain/taxonomy.json` por `System.getProperty("taxonomy.asset.path")` (mismo patrón que
  `TaxonomyIntegrityTest`) y exige 18 categorías → 18 glifos distintos (`== RecipeGlyph.entries`) y 10
  cocinas → 10 acentos distintos (`+ DEFAULT == RecipeAccent.entries`), determinismo, fallback total y
  caso negativo (un id desconocido no comparte glifo con ninguna categoría concreta salvo `other` —el
  propio fallback— ni acento con ninguna cocina real). Lint **0 errores y 13 warnings preexistentes**
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo
  `app/src`**. `grep`: **0 apariciones de `PlateGlyph`** en `app/src`; el degradado del placeholder
  (`primaryContainer` → `secondaryContainer`) solo vive en `RecipeArtwork.kt`. `RecipeArtworkStyle.kt`
  sin Compose/Android; `ui/artwork` sin hex sueltos; geometría y tamaños intactos (16:10, 96x96, alto
  96, hero 240; 48/56/40/40/72 dp); `git diff --stat` solo con los ficheros previstos.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/artwork/{RecipeArtworkStyle,RecipeArtwork}.kt`
  y `app/src/test/java/com/sazon/app/ui/artwork/RecipeArtworkStyleTest.kt` (nuevos);
  `app/src/main/java/com/sazon/app/ui/{home/RecipeCard.kt, results/ResultRecipeCard.kt,
  favorites/FavoriteRecipeCard.kt, detail/RecipeDetailScreen.kt}` (modificados); `DESIGN.md`,
  `docs/technical-discovery.md`, `docs/specs/recipe-artwork.md` (finding del grep), `feature_list.json`
  y este registro. **No** se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`,
  `data/**`, la Taxonomía, `seed-recipes.json`, `ui.theme/**`, `MainShell.kt`, `SazonApp.kt`,
  `MainActivity.kt` ni `init.sh`; **no** se crea `docs/design/concepts/recipe-artwork/` (desviación
  autorizada en el issue #77).
- Hallazgos relevantes: (1) Kotlin **anida** los comentarios de bloque, así que `assets/img/recipes/**`
  dentro de un KDoc abre un comentario anidado y rompe la compilación ("Unclosed comment"): se escribió
  sin el `**`. (2) `Recipe.categoryId`/`cuisineId` **no son anulables**, así que el caso "sin
  clasificar" real que cubre el test es un id **vacío o desconocido** (el mapeo puro sí acepta `null`
  por si un futuro campo se anula). (3) El "grep de `Brush.linearGradient` solo en `RecipeArtwork.kt`"
  de la spec es impreciso: `HomeScreen` ya usaba `Brush.linearGradient` (hero `primary` →
  `inversePrimary`, ajeno a esta feature); el degradado **del placeholder** sí queda solo en
  `RecipeArtwork.kt`. Anotado en la spec.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (fidelidad fina de los 18 glifos
  line-art y contraste del glifo `onSurfaceVariant` sobre los 11 degradados). Queda para revisión manual
  de la persona en el emulador (Home, Resultados, Favoritos lista+rejilla y Detalle, en claro y oscuro)
  el acabado contra el registro de `DESIGN.md`; un defecto visual se corrige con una feature correctiva.
  No se ha hecho commit ni push. Se mantiene el backfill de capturas de UI de las features previas
  (sesión 046).
- Siguiente paso: **validación independiente** de `recipe-artwork` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 078

- Fecha: 2026-10-12
- Objetivo: validar `recipe-artwork` de forma independiente y persistir la aceptación. Ejecutada por el
  orquestador de `feature-flow` en la rama `feature/recipe-artwork` (worktree), solo hasta-aceptación;
  **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente; creado `docs/validations/recipe-artwork.md`.
  Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0) y, al verlo `UP-TO-DATE`, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML de resultados y
  el informe `lint-results-debug.xml`, e inspeccionó el código y el diff.
- Evidencia capturada: **60 suites / 413 tests / 0 fallos / 0 errores / 0 saltados**
  (`RecipeArtworkStyleTest` 8; 405 preexistentes en verde) y lint **0 errores, 13 warnings** preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 bajo
  `app/src`. El validador **recomputó** el mapeo contra la Taxonomía real: 18 `recipeCategories` → 18
  `RecipeGlyph` distintos (`== RecipeGlyph.entries`) y 10 `cuisines` → 10 `RecipeAccent` distintos
  (+`DEFAULT` `== RecipeAccent.entries`), `else`/`other` → `PLATE`/`DEFAULT`; confirmó la pureza de
  `ui.artwork` (sin Compose/Android), la ausencia de hex sueltos, **0 `PlateGlyph`** en `app/src`, el
  degradado del placeholder solo en `RecipeArtwork.kt`, los 5 puntos de llamada con geometría/tamaños
  preservados (16:10, 96x96, alto 96, hero 240; glifos 48/56/40/40/72 dp), que `DESIGN.md` y
  `docs/technical-discovery.md` quedan actualizados, la exención de capturas (issue #77) tratada como
  autorizada, la disciplina de alcance y que el implementer no hizo commit/push/PR.
- Ficheros actualizados por el orquestador: `feature_list.json` (`recipe-artwork` a `accepted` +
  evidencia), `docs/validations/recipe-artwork.md` (nuevo) y este registro. Commit local de la rama
  `feature/recipe-artwork`; la publicación queda para `github-flow`.
- Hallazgos del validador (2 Low informativos, no bloqueantes): la redacción "vertical" del degradado
  cuando `Brush.linearGradient` es diagonal (idéntico al placeholder aceptado antes, sin regresión);
  Scenario 6 (roles del tema por claro/oscuro) es solo por inspección, coherente con el Verification Plan
  y con AGENTS.md (no se automatiza UI de emulador). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (fidelidad fina de los 18 glifos
  line-art y contraste del glifo `onSurfaceVariant` sobre los 11 degradados). Queda para revisión manual de
  la persona en el emulador (Home, Resultados, Favoritos lista+rejilla y Detalle, en claro y oscuro) el
  acabado contra el registro de `DESIGN.md`; un defecto visual se corrige con una feature correctiva. Se
  mantiene el backfill de capturas de UI de las features previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/recipe-artwork` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `recipe-artwork` era la única feature
  de este plan; el plan del MVP sigue cerrado y no hay más features `not_started`.

### Sesión 079

- Fecha: 2026-10-12
- Objetivo: implementar `ai-verbose-diagnostics` (correctiva de `ai-call-diagnostics`) en el worktree de
  la rama `feature/ai-verbose-diagnostics`: (A+B) desglosar `INCOHERENT` en sus dos causas del Validador
  y (D) modo de depuración (interruptor, detalle de descartes con título y JSON crudo troceado). Solo
  hasta implementación y autoverificación (`passing`), sin commit/push/PR.
- Completado:
  - **Desglose siempre activo**: `AiIncoherentCause { TIME_DRIFT, ALLERGEN_COVERAGE }` (diagnóstico en
    `data.ai`, **no** se toca `DiscardReason`) y `AiCallEvent.incoherentByCause`; `AiRecipePayload.decode`
    clasifica cada descarte `INCOHERENT` recomputando **solo** la regla 4a con
    `RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES` (si no es 4a es 4b: el Validador devuelve el primer
    fallo y evalúa 4a antes que 4b).
  - **Modo depuración**: `DevExpertRecipeGenerator` gana `verboseDiagnostics: Boolean = false`;
    `decode(verbose)` rellena `AiDecodeResult.discards` (`AiDiscardDetail`: motivo, causa y título del
    draft o del primitivo `title` del JSON bruto) solo en verbose; el generador añade
    `rawContent = outcome.content` solo en verbose. `MainActivity` usa
    `verboseDiagnostics = BuildConfig.DEBUG` y añade `incoherent={…}` (solo si hay causas) más las líneas
    verbosas; el modo lo decide un único sitio (el generador) y el log es renderizador.
  - **Helper puro** `AiVerboseDiagnostics`: `MAX_CHUNK_CHARS = 3_000`, `rawLines` (trocea sin cortar
    pares suplentes; `null`/vacío → vacío) y `discardLines` (motivo/causa/título saneado; `(sin titulo)`).
- Verificación ejecutada: tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` (BUILD SUCCESSFUL) y gate
  estándar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`, sin procesos largos); parseo
  propio de los XML y del informe de lint.
- Evidencia capturada: **76 tests / 0 fallos en `data.ai`** (62 previos + 14 nuevos: `AiRecipePayloadTest`
  14, `AiVerboseDiagnosticsTest` 7, `DevExpertRecipeGeneratorTest` 35, más los intactos `AiProviderPolicyTest`
  12, `AiRecipeIdsTest` 6 y `OpenAiChatTest` 2); gate **60 suites / 419 tests / 0 fallos / 0 errores / 0
  saltados** (405 previos + 14 nuevos); lint **0 errores y 13 warnings preexistentes** (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`**.
  Scenario 1: lote con una 4a y una 4b → `discardedByReason {INCOHERENT: 2}` e `incoherentByCause
  {TIME_DRIFT=1, ALLERGEN_COVERAGE=1}` en orden del enum, sin títulos. Scenario 2: coherencia con
  `RecipeValidator.validateRecipe` (solo 4a → TIME_DRIFT; solo 4b → ALLERGEN_COVERAGE; ambas → TIME_DRIFT;
  ninguna → `Accepted` con el desglose vacío). Scenario 3: por defecto `discards` vacío, `rawContent` null
  y `toString()` sin títulos ni cuerpo. Scenario 4: con el interruptor activo y un 200 de ~14 KB,
  `rawContent == content` del `MockEngine`, `discards` con título/causa y el render reconstruye el crudo
  por concatenación. Scenario 5: ningún evento ni línea lleva `test-key`/`Bearer`/`Authorization` en
  ningún modo.
- Ficheros cambiados: `app/src/main/java/com/sazon/app/data/ai/{AiDiagnostics,AiDecodeResult,
  AiRecipePayload,DevExpertRecipeGenerator}.kt`, `app/src/main/java/com/sazon/app/data/ai/AiVerboseDiagnostics.kt`
  (nuevo), `app/src/main/java/com/sazon/app/MainActivity.kt`, `app/src/test/java/com/sazon/app/data/ai/
  {AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt`, `app/src/test/java/com/sazon/app/data/ai/
  AiVerboseDiagnosticsTest.kt` (nuevo), `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `docs/adr/0003-ai-provider.md`, `feature_list.json` y este registro. Sin tocar `Recipe.kt`,
  `RecipeValidator.kt` (`DiscardReason`/`RecipeValidation`/`DiscardedRecipe` intactos), `AiRecipeDraft.kt`,
  `AiRecipeIds.kt`, `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`, `ui/**`,
  `data/generator/**`, Room, Taxonomía, `seed-recipes.json`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md` ni
  `docs/design/**`; sin dependencias nuevas.
- Hallazgos relevantes: la primera pasada de la batería de coherencia falló por un error del propio test
  (decodificaba una receta suelta en vez del envelope `{recipes:[…]}`); corregido. El desglose **no** lleva
  texto de recetas por sí mismo (el título solo viaja con el interruptor). `incoherentByCause` es el único
  campo nuevo siempre presente en el evento (vacío si no hay `INCOHERENT`), así que con un lote sin
  `INCOHERENT` la línea `SazonAi` es la actual (interpretación de «idéntico al actual» de la spec).
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (duplicación mínima de la 4a, mitigada
  reutilizando la constante y con el test de coherencia; volumen de log en debug, ~5 líneas de ~3 KB por
  llamada con posible rate-limit de Logcat; `rawContent` puede ser `null`/vacío en un 200 ilegible). Queda
  la verificación **manual** de emulador de la persona (logcat con `incoherent={…}` y, en debug,
  `discard[i/n]`/`raw[i/n]`; release → formato anterior; clave ausente → modo semilla), no automatizable
  desde WSL. El modo verboso queda trazado como **deuda temporal** (debug-only) en
  `docs/risks-and-open-questions.md` y con nota en `docs/adr/0003-ai-provider.md`.
- Siguiente paso: **validación independiente** de `ai-verbose-diagnostics` con `feature-validator`;
  después, commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por
  `github-flow`. Se mantiene el backfill de capturas de UI (sesión 046).

### Sesión 080

- Fecha: 2026-10-12
- Objetivo: validar `ai-verbose-diagnostics` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-verbose-diagnostics` (worktree), solo
  hasta-aceptación; sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con la evidencia del validador independiente y se crea
  `docs/validations/ai-verbose-diagnostics.md`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0; test + lint vía `scripts/gradle.sh`)
  y, al ver la primera pasada en `UP-TO-DATE`, forzó `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.data.ai.*" --rerun-tasks` (34/34 tareas) y `bash scripts/gradle.sh lint --rerun-tasks`
  (30/30 tareas); parseó por su cuenta los XML de resultados y el informe de lint.
- Evidencia capturada: gate **60 suites / 419 tests / 0 fallos / 0 errores / 0 saltados** (405 previos + 14
  nuevos); rerun focal fresco del paquete `data.ai`: **76/76** (`AiRecipePayloadTest` 14,
  `AiVerboseDiagnosticsTest` 7, `DevExpertRecipeGeneratorTest` 35, más `AiProviderPolicyTest` 12,
  `AiRecipeIdsTest` 6 y `OpenAiChatTest` 2); lint **0 errores y 13 warnings preexistentes**, 0 bajo
  `app/src`. Confirmó los Scenarios 1-6 (desglose 4a/4b y su coherencia con `RecipeValidator.validateRecipe`;
  con el interruptor apagado sin detalle ni crudo y la línea `SazonAi` byte a byte con un lote sin
  `INCOHERENT`; con el interruptor activo `rawContent == content` del `MockEngine` y render reconstruible;
  no-fuga de `test-key`/`Bearer`/`Authorization` en ambos modos; regresión: los 62 tests previos de `data.ai`
  intactos). Scenario 7 (manual en emulador) pendiente por diseño. Alcance confirmado
  (`RecipeValidator.kt`/`DiscardReason`, `Recipe.kt`, `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`,
  `OpenAiChat.kt`, `domain/**`, `ui/**`, Room, assets, build, `init.sh`, `scripts/**`, `DESIGN.md` y
  `docs/design/**` intactos; sin dependencias nuevas; sin capturas, no es UI).
- Hallazgos del validador (3 Low informativos, ninguno bloqueante): la igualdad byte a byte del prefijo
  `incoherent=` vive en `MainActivity` sin test JVM (el helper puro sí está probado);
  `AiVerboseDiagnostics.chunks` con `maxChars == 1` podría partir un par suplente (no alcanzable en
  producción, que usa 3000); el registro de validación aún no existía (persistido en esta sesión). Sin
  hallazgos Critical/High/Medium.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/ai-verbose-diagnostics.md` (nuevo) y este registro. Sin cambios de código.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (duplicación mínima de la 4a mitigada
  con la constante y el test de coherencia; volumen de log en debug, ~5 líneas de ~3 KB por llamada con
  posible rate-limit de Logcat; `rawContent` puede ser null/vacío en un 200 ilegible). Queda la verificación
  **manual** de emulador de la persona (logcat con `incoherent={…}` y, en debug, `discard[i/n]`/`raw[i/n]`;
  release → formato anterior; clave ausente → modo semilla), no automatizable desde WSL. El modo verboso
  queda trazado como deuda temporal (debug-only) en `docs/risks-and-open-questions.md` y con nota en
  `docs/adr/0003-ai-provider.md`. Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/ai-verbose-diagnostics` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, sin merge) queda para `github-flow`. No hay más features del
  plan del MVP; el plan sigue **cerrado**.
