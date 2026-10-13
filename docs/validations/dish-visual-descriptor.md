# Validación independiente — `dish-visual-descriptor`

- **Feature**: `dish-visual-descriptor` (`area: integration`, fase 5 de `docs/ai-dish-images-proposal.md`, variante B)
- **Spec**: `docs/specs/dish-visual-descriptor.md`
- **Rama / worktree**: `feature/dish-visual-descriptor`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-visual-descriptor`
- **Sesión**: 094 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, misma sesión, trabajo en disco; `HEAD` = merge del PR #102, **sin commit, sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (`./gradlew test` `UP-TO-DATE` + `./gradlew lint` ejecutado, vía `scripts/gradle.sh`; sin procesos largos) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, **34/34 tareas ejecutadas**; parseo propio de los XML frescos (15:42): **86 suites / 619 tests / 0 fallos / 0 errores / 0 saltados** |
| Conteo por clase | XML vs anotaciones `@Test` | `DishVisualDescriptorTest` 7, `DishVisualDescriptorWireTest` 7, `DishImageCacheKeyTest` 12, `DishImagePromptTest` 11, `DishImageRequestTest` 7, `PromptBuilderTest` 12 — coinciden |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | exit 0; SARIF: **13 resultados, todos `warning`, 0 errores, 0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) |
| Baseline re-derivada | `git grep -o '@Test'` en `HEAD` vs worktree | `HEAD` **593** → worktree **619** ⇒ delta **+26** (coincide con la evidencia) |
| Pureza | `grep -E '^import'` + grep de `android.`/`androidx.`/`io.ktor`/`java.net`/`Random`/`UUID`/`currentTimeMillis` en `domain/image` y `DishVisualDescriptor.kt` | solo `domain.recipe`/`domain.taxonomy` + `java.text.Normalizer`/`java.util.Locale`; `DishVisualDescriptor.kt` **0 imports**; **0** Android/red/reloj/azar |
| Alcance | `git status --porcelain` + `git diff --name-only origin/main` | exactamente los previstos: 7 `main`, 4 tests ampliados, 3 tests nuevos, 4 docs durables, `feature_list.json`, `PROGRESS.md` y la spec (untracked); **sin diff** en `Recipe.kt`, `RecipeValidator.kt`, `domain/validation/**`, `AiRecipeIds.kt`, `data/image/**`, `data/ai/image/**`, `ui/**`, `MainActivity.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`, `assets/**` |
| Gate de capturas | `ls docs/design/concepts/` | `docs/design/concepts/dish-visual-descriptor` **no existe** (correcto, `area: integration`) |
| No publicación | `git log origin/main..HEAD`, `git ls-remote`, `gh pr list` | **0 commits** por delante, rama sin upstream, **sin rama remota y sin PR**; sin ficheros sueltos (`hs_err*`, logs) |

## Escenarios verificados (contra el código y los tests reales)

1. **El contrato pide los campos opcionales (Scenario 1)**: `PromptBuilderTest` comprueba el `OUTPUT_CONTRACT` con `dishKey`/`imagePrompt` en el esqueleto y la instrucción de opcionalidad (plato terminado, nunca un ingrediente ni la categoría; "si no los envías" la app usa el título y la taxonomía), manteniendo el prompt determinista y el resto del contrato §4.
2. **Descriptor presente se extrae y no altera la receta (Scenario 2)**: `DishVisualDescriptorWireTest` con JSON literal del contrato (sin `id`/`source`): la receta aceptada es idéntica y el descriptor queda en `result.descriptors[id generado]`.
3. **Sin descriptor, degradación y sin cambios (Scenario 3)**: ausente/`null`/en blanco ⇒ `descriptors` **vacío** y `accepted`/`discardedByReason` idénticos al baseline; clave y prompt reproducen exactamente la fase 1.
4. **Formato inválido ⇒ se ignora, nunca descarta (Scenario 4, negativo)**: `dishKey`/`imagePrompt` con tipo JSON no textual (número, decimal, booleano, objeto, array) o cadena en blanco o `dishKey` sin `[a-z0-9]` (`"🍲"`, `"..."`, `"寿司"`, `"---"`) ⇒ receta **aceptada** (nunca `SCHEMA`), sin descriptor; un descriptor parcial conserva solo el campo utilizable; un descarte no aporta descriptor.
5. **La clave usa `dishKey` cuando existe (Scenario 5)**: `DishImageCacheKeyTest`: `"Arroz con pollo fácil"` y `"Arroz con Pollo Criollo!!!"` con `dishKey = "arroz-con-pollo"` comparten `dish-image-v1-arroz-con-pollo` (frente a sus claves por título distintas); se normaliza igual que el nombre de plato y conserva alfabeto `[a-z0-9-]` e independencia de `Locale` (`tr-TR`).
6. **La clave mantiene su respaldo (Scenario 6)**: con descriptor `null`/`NONE`/`dishKey` vacío/`"🍲"`/`"..."` o solo `imagePrompt`, el resultado es idéntico a la fase 1 (título → respaldo por `id` → `dish-image-v1-plato`); `VERSION` sigue `dish-image-v1`.
7. **El prompt usa `imagePrompt` cuando existe (Scenario 7)**: `DishImagePromptTest`: `imagePrompt` como cláusula del plato (espacios colapsados), conservando prefijo, cláusula de cocina y `STYLE_SUFFIX` exactamente una vez, sin quedar en blanco.
8. **Sin `imagePrompt`, prompt de la fase 1 (Scenario 8)**: con `null`/`NONE`/solo `dishKey`/en blanco, idéntico a `DishImagePrompt.build(taxonomy, recipe)`.
9. **La petición compone ambas piezas (Scenario 9)**: `DishImageRequestTest`: `cacheKey`/`prompt` coinciden con las funciones homónimas y `recipeId` no cambia; por reflexión los campos del `data class` siguen siendo exactamente `{cacheKey, prompt, recipeId}` (sin campo de descriptor); sin descriptor, resultado de la fase 1.
10. **Los `id` no cambian (Scenario 10)**: `id` idéntico con y sin descriptor (el descriptor queda fuera de `AiRecipeIds.canonical`); `AiRecipeIds.kt` sin diff.

## Non-goals respetados

Sin entrega en runtime (`MainActivity` sigue componiendo `DishImageRequest.of(taxonomy, recipe)` sin descriptor; `AiDecodeResult.descriptors` es solo el transportador, sin consumidor), sin UI/microcopy/capturas, sin persistencia, sin red/cuota, sin tocar el puerto `DishImageGenerator`, `data/ai/image/**`, `data/image/**`, `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `domain/validation/**`, `ui/**`, build, `init.sh`, `scripts/**`, assets ni Taxonomía; sin fases 6-7; sin dependencias nuevas.

## Documentación durable

`docs/domain/taxonomy.md` (descriptor **opcional** del contrato en §4 y clave = `dishKey` cuando existe en §6), `docs/adr/0005-ai-dish-images.md` ("Aceptado parcialmente (fases 1-5)", alcance real de la fase 5, regla de `VERSION` intacta y entrega en runtime como non-goal pendiente), `docs/technical-discovery.md` (descriptor puro en `domain.image` + parseo tolerante en `data.ai` + tests JVM) y `docs/risks-and-open-questions.md` (riesgo de `dishKey` grueso = colisión de imagen y entrega en runtime/persistencia pendientes) están actualizados y son coherentes entre sí. `docs/domain-model.md`, `AGENTS.md` y `DESIGN.md` correctamente **no** tocados.

## Evaluación de seguridad

Sin hallazgos bloqueantes ni de riesgo. Sin secretos/tokens en el diff, tests ni docs; sin dependencias nuevas ni cambios de build; sin autenticación/autorización en el slice; la entrada del wire es tolerante y **no lanza** (tipo JSON erróneo, blanco, no canonicalizable ⇒ degradación); el `dishKey` que pasa a clave atraviesa `normalize` (alfabeto `[a-z0-9-]`, apto como nombre de fichero, sin traversal); `imagePrompt` es salida del propio modelo hacia el modelo de imagen, no input de usuario; `descriptors` es `internal`, solo en memoria, sin `Log` ni persistencia y con consumidor `null` en runtime.

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (informativos, no bloquean la aceptación):**

1. El guard estático de pureza `DishImagePromptTest.el_paquete_domain_image_no_importa_android_ni_hace_red` sigue enumerando los 3 fuentes preexistentes y no incluye `DishVisualDescriptor.kt`; la pureza del fichero nuevo queda cubierta por su test de comportamiento y por inspección manual (0 imports). Una futura regresión que importe Android en ese fichero no la cazaría el guard.
2. Duplicación menor del colapso de espacios (`singleLine` privado en `DishImagePrompt` y en `DishVisualDescriptor`): la spec pedía "reutilizar el criterio", no necesariamente la función; deuda cosmética.
3. Línea en blanco extra en `docs/risks-and-open-questions.md` tras la fila nueva (cosmético).

## Veredicto

**`accept`.** La feature cumple la spec (Scenarios 1-10 y Validator Checklist verdes con evidencia re-ejecutada de forma independiente), el alcance y la pureza se respetan (`Recipe.kt`, `RecipeValidator.kt` y `AiRecipeIds.kt` sin diff; sin capturas, sin red, sin persistencia, sin dependencias), la documentación durable es coherente, el handoff del implementer es real y el repositorio es reiniciable por el camino estándar (`bash init.sh` exit 0). Sin commit/push/PR. Estado recomendado: **`accepted`** (lo persiste el orquestador). Seguimiento no bloqueante: dar de alta la feature de integración `dish-visual-descriptor-wiring` (hoy el descriptor no tiene consumidor en runtime) y plegar en ella los dos hallazgos Low.
