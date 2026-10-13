# Validación independiente — `dish-image-progressive-ui`

- **Feature**: `dish-image-progressive-ui` (`area: integration`, fase 4 de `docs/ai-dish-images-proposal.md`, mitad de integración del split)
- **Spec**: `docs/specs/dish-image-progressive-ui.md`
- **Rama / worktree**: `feature/dish-image-progressive-ui`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-progressive-ui`
- **Sesión**: 092 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, misma sesión, trabajo en disco; `HEAD = 09d6eec` = `origin/main`, sin commit, sin push, sin PR)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); la primera pasada salió `UP-TO-DATE` y se forzó |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0; **81 suites / 562 tests / 0 fallos / 0 errores / 0 saltados** (parseo propio de los XML `TEST-*.xml`) |
| Tirada focal | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` | exit 0; **5 suites / 19 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline re-derivada | `git grep -c '@Test' 09d6eec -- app/src/test` + `git ls-tree` | `HEAD`: **77 ficheros / 551 tests** ⇒ delta exacto **+4 suites / +11 tests** (`RecipeArtworkImagePrecedenceTest` 2, `DishImageArtworkLoaderTest` 6, `GeneratedRecipeArtworkSourceTest` 1, `AndroidRecipeImageDecoderTest` 2) |
| Lint | `bash scripts/gradle.sh lint --rerun-tasks` + parseo del SARIF | exit 0; **0 errores, 13 resultados** preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`** |
| Pureza | grep de imports/`Log` en `ui/artwork` | `import android.` **solo** en `AndroidRecipeImageDecoder.kt`; `Log\.\|filesDir\|getDatabasePath` → **0** |
| Seam/acceso | `grep -rl LocalGeneratedRecipeArtworkSource\|GeneratedRecipeArtworkSource app/src/main` | solo `ui/artwork/{GeneratedRecipeArtworkSource,RecipeArtwork}.kt` + `MainActivity.kt`; **ningún** punto de uso ni pantalla |
| Alcance | `git status --short` | solo los 6 `main` nuevos de `ui/artwork`, `RecipeArtwork.kt`, `MainActivity.kt`, los 4 tests nuevos, `docs/specs/dish-image-progressive-ui.md` (planner), 5 docs, `feature_list.json` y `PROGRESS.md`; los 5 puntos de uso, `RecipePhotoSource.kt`, `data/**`, `domain/**`, build/lockfile, `init.sh`, `scripts/**`, `assets/**`, `seed-recipes.json` y las specs aceptadas **sin diff**; sin dependencias nuevas; `docs/design/concepts/dish-image-progressive-ui/` **no existe** |

## Escenarios verificados (contra el código y los tests reales)

1. **Progresión con crossfade (Scenario 1)**: `RecipeArtwork` lanza **dos** `produceState` independientes (local y generada, claves `recipe.id`/`recipe.categoryId`); la **capa base se pinta siempre** (foto local con `ContentScale.Crop` o el degradado + glifo) y la generada se **superpone** con `animateFloatAsState` a **250 ms** (`DishImageArtworkPolicy.CROSSFADE_MILLIS`). Ningún frame en blanco. Verificado por inspección de `RecipeArtwork.kt` (líneas ~91-128); lo puramente visual queda para la persona tras el merge (AGENTS.md).
2. **Precedencia pura (Scenario 2)**: `RecipeArtworkImagePrecedenceTest` (no vacuo, `T = String`): `layerOf("local","generada") → GENERATED`, `layerOf("local", null) → LOCAL`, `layerOf(null, "generada") → GENERATED`, `layerOf(null,null) → FALLBACK`; `crossfadeTarget("generada") == 1f`, `crossfadeTarget(null) == 0f`.
3. **Degradación silenciosa (Scenario 3)**: `DishImageArtworkLoaderTest` — `null` y array vacío → `null` **sin cachear** (el resolutor se re-invoca; `decodedCount == 0`); `decode → null` (códec roto) → `null` sin cachear; `CancellationException` se propaga y no se traga. `GeneratedRecipeArtworkSourceTest` prueba que el default `NoGeneratedRecipeArtworkSource` devuelve `null` (host sin proveedor sigue pintando lo de hoy).
4. **Acierto de caché sin gasto (Scenario 4)**: el loader consulta el LRU por `request.cacheKey` **antes** del `Semaphore`: dos títulos equivalentes (`"Arroz con Pollo Criollo"` vs `"  arroz  con   pollo criollo "`) comparten la clave canónica **real** (`DishImageCacheKey`) y el resolutor y el decodificador se invocan **una sola vez**. El loader vive una vez en `MainActivity` (fuera de `setContent`), así que su LRU sobrevive a recomposiciones/scroll.
5. **Cancelación propagada (Scenario 5)**: el loader no captura `CancellationException` (`semaphore.withPermit { resolve(request) }` la relanza); los `produceState` cancelan al salir de composición o reciclar la tarjeta.
6. **Concurrencia ≤ 2 (Scenario 6)**: `Semaphore(MAX_CONCURRENT_GENERATIONS = 2)`; con tres `async` y un `CompletableDeferred` sin liberar, `maxInFlight == 2` y `calls == 2`; al liberar, las tres terminan y `calls == 3`.
7. **LRU acotada por clave canónica (Scenario 7)**: `LinkedHashMap(accessOrder = true)` con `removeEldestEntry(size > maxDecoded)`; con tope 2, la tercera clave descarta la menos usada, A sobrevive por haber sido usada, B (la descartada) se vuelve a resolver; `decodedCount <= 2` siempre.
8. **Alcance y pureza (Scenario 8)**: 5 puntos de uso **sin cambios** (`RecipeCard` 16:10, `ResultRecipeCard` 16:10/56, `FavoriteRecipeCard` alto 96/40 y miniatura 96x96/40, `DetailHero` 240/72); firma `(recipe, modifier, glyphSize)` intacta; `import android.graphics` solo en `AndroidRecipeImageDecoder.kt`; `RecipePhotoSource.kt` y `data/**`/`domain/**` intactos; sin consumidores fuera de `ui/artwork` + `MainActivity`; sin dependencias nuevas; sin carpeta de capturas.
9. **Decodificador Android (Robolectric)**: `AndroidRecipeImageDecoderTest` (`@Config(sdk=[33])`, `@GraphicsMode(NATIVE)`) genera un WebP **dentro** del test (no se versiona binario), lo decodifica a `ImageBitmap` no nulo (64x48) y confirma `null` para bytes vacíos/basura sin lanzar.

## Decisiones del orquestador respetadas

- **Desviación de capturas autorizada**: no existe `docs/design/concepts/dish-image-progressive-ui/` y `DESIGN.md` registra la excepción con el mismo formato que #77/#85/#86/#91 (feature + **issue #98**), más la cadena en runtime y el crossfade de 250 ms en **Components → Imagen de receta**.
- **Tope por sesión NO exigido aquí**: `feature_list.json` ya retiró "tope de generaciones por sesión respetado" y lo sustituyó por "límite de concurrencia (2) respetado" (se implementa en el hermano `dish-image-settings-toggle`). No se reporta su ausencia como defecto.

## Non-goals respetados

Sin UI/pantalla/microcopy/estado visual nuevos, sin interruptor de usuario ni tope por sesión, sin tocar la cadena de `recipe-photos` (`data.RecipePhotoAssets`/`data.AssetRecipePhotoSource` intactos), sin tocar `data.image`/`data.ai.image`/`domain.image`, sin set semilla ni `/images/edits` ni `dishKey`, sin timeout nuevo (se usan los `DishImagePolicy.TIMEOUTS` de la fase 2), sin dependencias nuevas y sin cambios en `app/build.gradle.kts`/`gradle/libs.versions.toml`/`init.sh`/`scripts/**`/`AndroidManifest.xml`.

## Documentación durable

`DESIGN.md` (excepción #98 + cadena + crossfade), `docs/technical-discovery.md` (bullet de la fase 4, consumidor real del puerto/caché, tests JVM), `docs/adr/0005-ai-dish-images.md` ("Aceptado parcialmente (fases 1-4)", split con el hermano, cadena local como respaldo), `docs/domain/taxonomy.md` §6 (nivel generado delante de la cadena de assets) y `docs/risks-and-open-questions.md` (el gasto de cuota **se materializa**; mitigación implementada = caché + concurrencia 2 + cancelación; pendiente = tope por sesión + interruptor del hermano y set semilla de fase 7) están actualizados y son coherentes entre sí. `docs/domain-model.md` y `AGENTS.md` correctamente **no** tocados.

## Evaluación de seguridad

Sin hallazgos bloqueantes ni de riesgo alto. Sin `Log` ni telemetría en `ui/artwork` (no se exponen la clave ni las claves canónicas; el adaptador de red sigue aislado en `data.ai.image`, fase 2 ya auditada). Sin permisos nuevos ni superficie de red nueva en la capa de UI (la red ocurre solo a través de `DishImageResolver`/`DevExpertDishImageGenerator` existentes); el `aiClient` se **reutiliza** (no se crea un segundo motor HTTP). Sin datos personales. Contrato de fallo `null`/no-excepción con cancelación propagada, coherente con el ADR 0005. La imagen generada vive a lo sumo en `cacheDir` (descartable).

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (no bloquean la aceptación):**

1. `docs/adr/0005-ai-dish-images.md` (alcance de la fase 4): una frase empieza en minúscula tras un punto ("…tampoco dependencias nuevas ni `Log`. la cadena de `recipe-photos`…"). Typo de redacción, sin efecto material.
2. El loader **no** hace *single-flight* a nivel de loader para claves canónicas idénticas concurrentes (dos `imageFor` simultáneos de la misma clave pueden llamar a `resolve` dos veces); la deduplicación real la aporta la `Mutex` por clave + caché de memoria de `DishImageResolver` (fase 3), como documenta la spec. No testeado de forma concurrente; inocuo (una sola generación efectiva).
3. `produceState` conserva el valor anterior al cambiar de clave (no lo resetea a `null`): en un reciclado muy rápido podría mostrarse un frame del bitmap previo. Es comportamiento **preexistente** de la cadena local (no lo introduce esta feature) y no afecta a la corrección de la progresión.
4. Scenario 1 (crossfade visible) y la cancelación desde `produceState` se verifican por inspección + prueba manual de la persona, no por test automatizado: coherente con AGENTS.md (no se automatiza UI de emulador) y con el plan de verificación de la spec.

## Handoff

- Trabajo del implementer **sin commit/push/PR**; `HEAD` sigue en `09d6eec`. El commit y la publicación los hace el orquestador de `github-flow`.
- Recomendación: persistir el estado **`accepted`** en `feature_list.json` (orquestador) y registrar la evidencia; siguiente feature del plan = `dish-image-settings-toggle` (`area: ui`, hermano de la fase 4; requiere capturas o desviación autorizada).
