# Validación independiente — `ai-recipe-decode`

- Fecha: 2026-10-12
- Feature: `ai-recipe-decode` (correctiva de `real-ai-provider`, issue #70) · `area: integration`
- Spec: `docs/specs/ai-recipe-decode.md`
- Rol: `validator` (agente con contexto limpio, skill `feature-validator`)
- Rama / worktree: `feature/ai-recipe-decode`
- Veredicto: **`accept`**

## Alcance validado

- `AiRecipeDraft` (nuevo, `data.ai`): DTO del **wire** del contrato de `docs/domain/taxonomy.md` §4
  **sin** `id`/`source`/`matchScore`; reutiliza `RecipeIngredient`/`RecipeStep`/`Nutrition`;
  `toRecipe(id)` fija `source = RecipeSource.AI` y `matchScore = null`.
- `AiRecipeIds` (nuevo): id determinista `ai-` + SHA-256 hex minúscula de la cadena canónica (prefijo
  `ai-recipe-v1`, separadores `U+001F`/`U+001E`), **sin `Random`, reloj ni `UUID`**; duplicado exacto en
  la llamada se desambigua con `-2`/`-3`… (determinista por orden).
- `AiRecipePayload.decode` reescrito: parseo tolerante del root/array, elemento a elemento con
  `AiRecipeDraft`, **completar `id`/`source` antes** de aplicar
  `RecipeValidator.validateRecipe(..., AiProviderPolicy.NEUTRAL_CONSTRAINTS)`; nunca lanza.
- `Recipe.kt` y `RecipeValidator.kt` **intactos** (`id`/`source` siguen obligatorios: el esquema no se
  debilita). `validateResponse` queda sin llamador real en `main` (solo su definición + 2 KDoc).

## Verificación re-ejecutada por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | exit 0; `test` y `lint` vía `scripts/gradle.sh` (primera pasada `UP-TO-DATE`) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL, 34/34 tareas (2m39s) |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | BUILD SUCCESSFUL, 30/30 tareas (2m43s) |
| XML (parseo propio) | `app/build/test-results/testDebugUnitTest/*.xml` | **58 suites / 386 tests / 0 fallos / 0 errores / 0 saltados** (376 previos + 10 nuevos) |
| `data.ai` | mismo XML | 43 tests: `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8, `DevExpertRecipeGeneratorTest` 19 |
| Lint | `app/build/reports/lint-results-debug.xml` | **0 errores, 13 warnings** preexistentes; 0 bajo `app/src` |

## Comprobaciones adicionales

- **Reproducción independiente del bug y del arreglo** con un verificador Java propio en
  `/tmp/opencode/sazon-verify/` (JDK 21 sobre las clases compiladas + `taxonomy.json` real; **ningún
  fichero del repo tocado**), `ALL CHECKS PASSED` (16/16):
  - JSON **literal sin `id`/`source`** → 1 receta, `source=AI`, `id=ai-…`, `matchScore=null`,
    título/ingredientes intactos.
  - **Discriminador de #70**: el mismo literal por el camino de `main` (`RecipeValidator.validateResponse`)
    → `accepted = 0` / 1 descarte `SCHEMA`; con el cambio → 1 receta. Prueba real de que el test nuevo es
    de regresión y no complaciente.
  - Mismo contenido → mismo id e independiente del orden de la respuesta; 3 recetas con duplicado literal
    → 3 ids únicos (`-2`/`-3`).
  - Reglas del Validador vivas: `mealTypeId`/`cuisineId`/`difficultyId` inexistentes y deriva de tiempos
    (`steps` 25 min vs `totalMinutes` 60) → solo sobrevive la receta válida (una rota no tumba el lote).
  - Tolerancia: `null`, blanco, no-JSON, `{"foo":1}`, `[1,2]`, `{"recipes":3}` y valla markdown → sin
    lanzar.
  - Extra: `id`/`source`/`matchScore` enviados por el modelo → ignorados (`id` `ai-*`, `source=AI`,
    score `null`).
- **Forma real vs punto ciego**: `AiRecipePayloadTest` ya **no importa `RecipeJson`**; el envelope y el
  `draftJson` son JSON **literal** del contrato §4. `DevExpertRecipeGeneratorTest` añade
  `REAL_CONTRACT_CONTENT` literal y el escenario 7 end-to-end con `GenerationService` real.
- **Alcance**: el diff solo toca `data.ai` (+2 nuevos) y sus 3 tests + 3 docs + harness. `domain/**`,
  `ui/**`, `data/generator/**`, Room, assets, `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin
  dependencias nuevas. `git grep validateResponse app/src/main` → solo la definición + 2 comentarios.
- **Seguridad**: sin claves/secretos en el diff (tests con `test-key`); `local.properties` ignorado
  (`git check-ignore` → `.gitignore:5`); entrada no confiable (salida del modelo) con `runCatching`
  elemento a elemento + Validador; el `id` es un hash one-way sin PII.

## Hallazgos por severidad

- **Critical/High/Medium: ninguno.**
- Low 1 — la cadena canónica del `id` (`AiRecipeIds.canonical`) usa `order`/`title`/`minutes` y **omite
  `RecipeStep.description`**: dos recetas que solo difieren en la prosa de un paso comparten `id` base
  entre llamadas (misma llamada → sufijo). Impacto bajo (mismo título/ingredientes/minutos); cumple la
  lista de campos de la spec. Sugerencia de correctiva futura: incluir `description` + vector dorado.
- Low 2 — la frase añadida a `docs/domain/taxonomy.md` §4 ("el modelo no debe enviar `matchScore`")
  convive con el bloque JSON de la misma sección y con `PromptBuilder`, que **sí** lo pide (la app lo
  ignora): drift preexistente, agravado; tocar `PromptBuilder` queda fuera de alcance.
- Low 3 — endurecimiento de tests opcional: falta un **vector dorado** del hash (a diferencia de
  `PreferenceSignature`), ningún test asevera que un `id`/`source` del modelo se ignora (verificado
  externamente por el validador) y el escenario 4 no distingue `SCHEMA` de `INCOHERENT` en `decode`.
- Informativos: 18 de los 19 casos de `DevExpertRecipeGeneratorTest` siguen usando `RecipeJson.encode`
  (documentado: ejercitan otros caminos); la spec estaba sin commitear (entra en el commit de aceptación).

## Pendiente (no automatizable desde WSL; la persona)

- Escenario 8, emulador de Android Studio en **Windows** (el `adb` de WSL no lo ve): con
  `devexpert.apiKey` válida, buscar *"arroz con pollo"* y generar en el Generador → recetas reales que
  pasan el Validador (logcat `SUCCESS` en vez de `EMPTY`); con la clave vaciada o ausente → modo semilla
  sin fallar. Un defecto se corrige con una feature correctiva, sin reabrir esta.
- Se mantiene el backfill de capturas de UI de features previas (sesión 046).
