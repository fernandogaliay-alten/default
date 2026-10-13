# Validación independiente — `ai-quantity-tolerance`

- Fecha: 2026-10-13
- Feature: `ai-quantity-tolerance` (correctiva de `ai-recipe-decode`) · `area: integration` (no UI)
- Spec: `docs/specs/ai-quantity-tolerance.md`
- Rol: `validator` (agente con contexto limpio, skill `feature-validator`), **independiente** del implementer
- Rama / worktree: `feature/ai-quantity-tolerance`
- Estado validado: `passing` (sesión 081, implementer) — sin commit
- Dependencia: `ai-recipe-decode` (`accepted`, sesión 071) — satisfecha
- **Veredicto: `accept`**

## Alcance validado (leído en el diff, no del resumen del implementer)

1. **DTO del wire tolerante**: `data.ai` gana `AiIngredientDraft` (`quantity: Double? = null`, `unit`,
   `isPantry`, `ingredientId` con defaults tolerantes) y `AiRecipeDraft.ingredients` pasa a
   `List<AiIngredientDraft>`; `toRecipe()` normaliza con `map { it.toRecipeIngredient() }`.
2. **Contrato de normalización**: `AiIngredientDraft.toRecipeIngredient()` → `quantity == null`
   (nula **o** ausente) ⇒ `quantity = AL_GUSTO_QUANTITY (0.0)`, `unit = null` (se **descarta** la `unit`
   del wire); `quantity != null` ⇒ mapeo 1:1 intacto. Justificado por la semántica ya aceptada
   (`IngredientFormat` pinta "Al gusto" con `unit == null`; `ServingScaler` no escala).
3. **Ids**: `AiRecipeIds.canonical` canonicaliza el ingrediente **ya normalizado**, sin cambiar `PREFIX`
   ni `ALGORITHM_VERSION`.
4. **Puerta dura intacta**: `RecipeIngredient.quantity` sigue siendo `Double` no anulable en
   `domain.recipe`; `RecipeValidator` sigue siendo la puerta dura y **no lee `quantity`** en ninguna línea.

## Verificación re-ejecutada por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0**, pero primera pasada **`UP-TO-DATE`** (evidencia débil, no usada como prueba) |
| Tests forzados (evidencia fuerte) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL in 3m48s`, **34/34 tareas** ejecutadas |
| XML (parseo propio) | `app/build/test-results/**/*.xml` | **64 suites / 465 tests / 0 fallos / 0 errores / 0 saltados**. `data.ai` = **85**: `AiProviderPolicyTest` 12, `AiRecipeIdsTest` 7, `AiRecipePayloadTest` 22, `AiVerboseDiagnosticsTest` 7, `DevExpertRecipeGeneratorTest` 35, `OpenAiChatTest` 2 → reproduce los números del implementer |
| Lint XML | `app/build/reports/lint-results-debug.xml` | **0 errores**, 13 warnings (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), todas en `app/build.gradle.kts`/wrapper/version catalog, **0 bajo `app/src`** |
| Bytecode real | `javap` sobre las clases compiladas | `AiIngredientDraft.getQuantity(): java.lang.Double` (anulable) vs `RecipeIngredient.getQuantity(): double` (primitivo) → el DTO es el tolerante, **el dominio está intacto** |
| Verificador independiente (fuera del repo) | `/tmp/opencode/aqt`, compilado y ejecutado contra `app/build/intermediates/…/classes` + kotlin-stdlib 2.2.10 + kotlinx-serialization 1.9.0 | **28 checks / 0 fallos** |
| Alcance | `git diff --name-only`, `git status --porcelain`, `git diff --cached` | 9 ficheros previstos + spec untracked; índice vacío; prohibidos **UNCHANGED** |

## Comprobaciones independientes de los Scenarios 1-7 (28/28)

Taxonomía real (95 ingredientes) y cuerpos JSON **literales** del contrato §4.

- **S1** `{"quantity":null,"unit":null}` → accepted == 1, sin descartes; perejil `quantity == 0.0`,
  `unit == null`; `IngredientFormat.quantityLabel(0.0, null) == "Al gusto"`. **OK**.
- **S2** `{"quantity":null,"unit":"g"}` → accepted == 1; la `unit` del wire **descartada** (`null`). **OK**.
- **S3** campo `quantity` **ausente** → ingredientes **idénticos** al caso nulo. **OK**.
- **S4** `200 g` / `1 ud` **intactos**. **OK**.
- **S5** lote mixto (al gusto + `{"title":"Rota"}`) → accepted == 1, `received == 2`, `{SCHEMA:1}`. **OK**.
- **S6** Validador **no relajado**: `mealTypeId` inexistente → `{SCHEMA:1}`; deriva 60 vs 25 min →
  `{INCOHERENT:1}`; ninguna aceptada. **OK**.
- **S7** `source == AI`, `id` prefijo `ai-`, `matchScore == null`; `id(null) == id(ausente) ==
  id(0.0 + null)` y `!= id(receta numérica)`. **OK**.
- **Causa raíz (diferencial real)**: `RecipeIngredient` lanza
  `JsonDecodingException: Failed to parse type 'double' for input 'null' at path: $.quantity`;
  `AiIngredientDraft` decodifica y `toRecipeIngredient()` normaliza a `0.0 / unit = null` preservando
  `isPantry`/`ingredientId`. **OK**.
- **Estabilidad de ids (comprobación fuerte)**: recomputado **a mano** el hash de canonicalización
  **anterior** (prefijo `ai-recipe-v1`, separadores U+001F/U+001E, ingrediente **sin normalizar**
  decodificado del wire) + SHA-256 ⇒ `ai-5f86aeac…32c2`, **idéntico** al `AiRecipeIds.idFor` nuevo para
  la misma receta numérica. **Ningún id existente cambia** (mapeo numérico 1:1). **OK**.

## Hallazgos

**Critical / High / Medium: ninguno.**

**Low / informativos (no bloquean la aceptación):**

1. *`Validator Checklist` de la spec sin marcar* (convención heredada; el resto de specs aceptadas están igual). **No requiere cambio.**
2. *`AiRecipePayloadTest` (paquete `data.ai`) importa `ui.detail.IngredientFormat`* — es lo que **pide** el Verification Plan de la spec y la clase es pura (sin Compose/Android). Solo anotado como costura.
3. *Colapso de representaciones del wire* (`{"quantity":null}`, `{"quantity":0,"unit":null}`,
   `{"quantity":null,"unit":"g"}`) → mismo ingrediente normalizado y por tanto mismo `id`; la `unit` se
   pierde cuando no hay cantidad. Es **exactamente** el comportamiento especificado (Scenarios 2 y 7) y
   coherente con el render "Al gusto". **No requiere cambio.**
4. *Sin vector dorado del hash `ai-<sha256>`* — gap **preexistente** de `ai-recipe-decode`, no
   introducido aquí; cubierto por determinismo/unicidad y por la recomputación independiente. **No requiere cambio.**
5. *Artefactos del orquestador* — no existía `docs/validations/ai-quantity-tolerance.md` y la spec estaba
   untracked; **persistido ahora** por el orquestador junto con el estado. **Resuelto.**
6. *Scenario 8 (manual en emulador)* — no automatizable desde WSL (el `adb` de WSL no ve el emulador);
   queda **pendiente por diseño**, correctamente anotado en `feature_list.json` y `PROGRESS.md`.

## Disciplina de alcance (verificada)

`git diff --name-only HEAD` = **9 ficheros** + 1 spec untracked, todos del alcance:
`app/src/main/java/com/sazon/app/data/ai/{AiRecipeDraft,AiRecipeIds}.kt`,
`app/src/test/java/com/sazon/app/data/ai/{AiRecipeIdsTest,AiRecipePayloadTest}.kt`,
`docs/technical-discovery.md`, `docs/domain/taxonomy.md`, `docs/domain-model.md`, `feature_list.json`,
`PROGRESS.md` y `docs/specs/ai-quantity-tolerance.md` (spec del planner). `git diff --cached` **vacío**.
`git diff --quiet` confirma **intactos**: `Recipe.kt`/`RecipeIngredient` (`quantity: Double` no
anulable), `RecipeValidator.kt`, `RecipeJson.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`,
`AiProviderPolicy.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`
(`DevExpertRecipeGeneratorTest` no necesitó cambios), `domain/**`, `ui/**`, `data/generator/**`, Room,
`app/src/main/assets/**` (Taxonomía, `seed-recipes.json`), `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md` y
`docs/design/**`. **Sin dependencias nuevas** y **sin capturas** (no es UI).

## Seguridad (checklist acotada a la feature)

- **Sin hallazgos.** El diff no añade claves/tokens; `local.properties` sigue ignorado.
- **Decodificación pura en memoria**: no añade red, persistencia, logging ni exposición de datos
  personales; el id `ai-` es el hash unidireccional de contenido ya aceptado.
- **La puerta dura no se debilita**: `RecipeValidator` **no lee `quantity`** (solo `name`/`ingredientId`),
  y la validación con `AiProviderPolicy.NEUTRAL_CONSTRAINTS` se mantiene **después** de la normalización.

## Documentación y harness

- Docs durables actualizadas y coherentes con la implementación: `docs/technical-discovery.md`
  (párrafo `data.ai` + Testing and Verification), `docs/domain/taxonomy.md` §4 (el contrato explicita el
  ingrediente "al gusto" y su normalización) y `docs/domain-model.md` (`quantity` sigue no anulable en
  dominio; "al gusto" del wire → `0.0` + `unit = null` en el adaptador). No se toca `AGENTS.md`,
  `DESIGN.md`, `docs/design/**` ni `docs/risks-and-open-questions.md` (correcto).
- No es feature de UI (`area: integration`): sin capturas de `docs/design/concepts/**`, correcto.
- `init.sh` **sin cambios** y sigue siendo el gate real (test + lint vía `scripts/gradle.sh`, exit 0; sin
  procesos largos). `feature_list.json` coherente y `PROGRESS.md` con la sesión 081, reflejo del estado.
- El implementer **no hizo commit/push/PR**: HEAD en `085634c`, rama sin upstream,
  `gh pr list --head feature/ai-quantity-tolerance` vacío.

## Veredicto y acción siguiente

**`accept`.** Los Scenarios 1-7 están implementados, cubiertos por test y verificados de forma
independiente (rerun forzado 34/34 tareas, 64/465/0, lint 0 errores; verificador externo 28/28; ids
estables). Scenario 8 (manual en emulador) queda pendiente por diseño.

Acción del **orquestador** (esta sesión **no** la ejecuta): persistir `accepted` en `feature_list.json`
(hecho), crear el commit local de `feature/ai-quantity-tolerance` y dejar la publicación (push + PR,
`Closes #<n>`, sin merge) para `github-flow`. Queda la verificación **manual** de emulador (Scenario 8)
a cargo de la persona, no bloqueante para el flujo autónomo. Los 5 Low informativos quedan como pulido
opcional.
