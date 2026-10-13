# Validación independiente — `dish-visual-descriptor-wiring`

- **Feature**: `dish-visual-descriptor-wiring` (`area: integration`, entrega en runtime del descriptor visual de la fase 5)
- **Spec**: `docs/specs/dish-visual-descriptor-wiring.md`
- **Rama / worktree**: `feature/dish-visual-descriptor-wiring`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-visual-descriptor-wiring`
- **Sesión**: 097 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, misma sesión, trabajo en disco; `HEAD` = merge del PR #107 `c038690`, **sin commit, sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (`./gradlew test` salió `UP-TO-DATE` + `./gradlew lint`, vía `scripts/gradle.sh`; sin procesos largos) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL**, 34/34 tareas, 5m11s; parseo propio de los XML frescos (19:52): **92 suites / 673 tests / 0 fallos / 0 errores / 0 saltados** |
| Conteo por clase | XML vs anotaciones `@Test` | `DishVisualDescriptorStoreTest` 5, `DishVisualDescriptorWiringTest` 5, `DevExpertRecipeGeneratorTest` 38 (+3) — coinciden |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL**, 30/30 tareas, 4m51s; SARIF fresco (19:57): **13 resultados, todos `warning`, 0 errores, 0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1; solo `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`) |
| Baseline re-derivada | `git grep -c '@Test' HEAD` vs worktree | `HEAD` **90 ficheros / 660** → worktree **673** ⇒ delta **+2 suites / +13 tests** (exacto) |
| Pureza | `grep -E '^import'` en `DishVisualDescriptorStore.kt` | **0** `import android`; solo `DishVisualDescriptor` y `java.util.concurrent.ConcurrentHashMap` |
| Alcance | `git status --short` + `git diff --name-only` | exactamente los previstos (los `Expected File Changes` de la spec + `PROGRESS.md` + `feature_list.json` + la spec untracked); **sin diff** en `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `AiRecipeDraft.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`, `domain/image/**`, `data/ai/image/**`, `data/image/**` (salvo el almacén nuevo), `ui/**`, `SazonApp.kt`, `MainShell.kt`, build, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `docs/domain/taxonomy.json`, `seed-recipes.json` |
| Constantes | grep `VERSION` | `DishImageCacheKey.VERSION = "dish-image-v1"` | 
| Gate de capturas | `ls docs/design/concepts/` | `docs/design/concepts/dish-visual-descriptor-wiring` **no existe** (correcto, `area: integration`) |
| No publicación | `git log origin/main..HEAD`, `git ls-remote`, `gh pr list` | `HEAD == origin/main` (`c038690`), **0 commits** por delante, rama sin upstream, **sin rama remota y sin PR**; nada en staging |

## Escenarios verificados (contra el código y los tests reales)

1. **La costura emite el descriptor una sola vez (Scenario 1)**: `onDescriptors` con default no-op se invoca **una vez** en la rama `AttemptOutcome.Success`, después de `report(...)` y antes de `return result.accepted`, con el mapa del decode por `id` generado; **nunca** en fallo/401, 200 sin recetas (mapa vacío) ni cancelación; los tests existentes no cambian de firma.
2. **El descriptor llega a la petición (Scenarios 2-3)**: `DishVisualDescriptorWiringTest` cubre decode → `DishVisualDescriptorStore.record` → `DishImageRequest.of(taxonomy, recipe, descriptorFor(recipe.id))`, con `cacheKey == DishImageCacheKey.of(recipe, descriptor)` (`dish-image-v1-arroz-con-pollo-criollo`) y `prompt == DishImagePrompt.build(...)` con el `imagePrompt`; `"Arroz con pollo"` y `"Arroz con pollo fácil"` con el **mismo** `dishKey = "arroz-con-pollo"` comparten `dish-image-v1-arroz-con-pollo` frente a sus claves por título distintas.
3. **Sin descriptor, composición de la fase 1 (Scenario 4)**: con receta semilla/restaurada o `id` en blanco/nulo, la composición es **exactamente** `DishImageRequest.of(taxonomy, recipe)` (igualdad del `data class`).
4. **Semántica del almacén (Scenario 5)**: `DishVisualDescriptorStoreTest`: `record(Map)` ignora ids en blanco y descriptores `isEmpty`/`NONE`, el mismo `id` **gana el último**, `descriptorFor` devuelve `null` para `id` nulo/en blanco/desconocido y no hay estado compartido entre instancias.
5. **Alcance y no-regresión (Scenario 6)**: `Recipe.kt`/`RecipeValidator.kt`/`AiRecipeIds.kt` sin diff (`AiDecodeResult.kt` queda intacto por spec), `VERSION` sin cambios, sin UI/capturas y sin dependencias nuevas.

## Non-goals respetados

Sin persistencia (el almacén es **efímero por sesión**), sin UI/microcopy/capturas, sin red/cuota adicional, sin tocar el puerto `RecipeGenerator`/`DishImageGenerator`, `domain/image/**`, `data/ai/image/**`, `data/image/**` (salvo el almacén nuevo), `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `AiDecodeResult.kt`, `ui/**`, build, `init.sh`, `scripts/**`, assets ni Taxonomía; sin dependencias nuevas; `DishImageCacheKey.VERSION` sigue `dish-image-v1`.

## Documentación durable

`docs/technical-discovery.md` (el descriptor **ya tiene consumidor en runtime**: `onDescriptors` + `DishVisualDescriptorStore` + `descriptorFor(recipe.id)`; tests nuevos), `docs/adr/0005-ai-dish-images.md` (entrega en runtime **cerrada**; sigue **Aceptado parcialmente (fases 1-7)**; descriptor sin persistir ni validar; `VERSION` intacta) y `docs/risks-and-open-questions.md` (fila del `dishKey`: entregada + **limitación viva**: efímero por sesión) son coherentes entre sí. `AGENTS.md`, `DESIGN.md` y `docs/domain-model.md` correctamente **no** tocados.

## Evaluación de seguridad

Sin hallazgos bloqueantes ni de riesgo. Sin secretos ni dependencias nuevas; sin superficie de red nueva (endpoint fijo); sin persistencia; sin `Log`. El `dishKey` lo controla el modelo y llega a un sumidero de nombre de fichero, pero `DishImageCacheKey.normalize` restringe el slug a `[a-z0-9-]` tras el prefijo fijo `dish-image-v1-`, así que traversal/injection de nombre de fichero es **imposible** (`File(root, "$cacheKey.webp")`).

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (informativos, no bloquean la aceptación):**

1. El KDoc de `AiDecodeResult.kt:44` sigue afirmando que el descriptor "no lo consume nadie en runtime (`descriptor = null`)", lo que ya es **falso**. Corregirlo está **prohibido** por esta spec (Scenario 6 lista el fichero como intocado); nota de seguimiento / enmienda de spec, no repair de esta feature.
2. La spec pedía "anotar el almacén"; está documentado en los bullets de `domain.image`, `data.ai` y tests de `technical-discovery.md`, pero no en el bullet de capa `data.image`. Pulido de colocación.

**Info (no requieren acción):** un `200` con contenido ilegible invoca el callback una vez con mapa vacío (según el propio enfoque técnico de la spec); como los descriptores solo existen para recetas aceptadas, el almacén queda intacto y el comportamiento es idéntico al de no invocar. El almacén es teóricamente no acotado (un valor pequeño por receta aceptada, liberado con la `Activity`); sin disco ni estado global.

## Veredicto

**`accept`.** La feature cumple la spec (Scenarios 1-6 y Validator Checklist verdes con evidencia re-ejecutada de forma independiente), el alcance y la pureza se respetan (`Recipe.kt`, `RecipeValidator.kt` y `AiRecipeIds.kt` sin diff; `VERSION` intacta; sin capturas, sin persistencia, sin dependencias), la documentación durable es coherente, el handoff del implementer es real y el repositorio es reiniciable por el camino estándar (`bash init.sh` exit 0). Sin commit/push/PR del validador. Estado recomendado: **`accepted`** (lo persiste el orquestador de `feature-flow`, que crea el commit local; `github-flow` publica).
