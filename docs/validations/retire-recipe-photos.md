# Validación independiente — `retire-recipe-photos`

- Feature id: `retire-recipe-photos` (`area: data`)
- Spec: `docs/specs/retire-recipe-photos.md`
- Rama/worktree: `feature/retire-recipe-photos`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/retire-recipe-photos`)
- Rol: `validator` (contexto limpio; no implementa, no commitea y **no** persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 111;
  trabajo **sin commitear**: `HEAD == ddafe9e` = merge de `dish-artwork-as-card-background`; sin push ni
  PR — esperado en `feature-flow`)
- Veredicto: **accept**

## Alcance validado

Quinta y última feature del ADR 0006, depende de `dish-artwork-as-card-background` (`accepted`). Retira
la cadena local de fotos de receta y sus assets, conservando el ancla de estilo `img/recipes/default.jpg`
(ADR 0005 fase 6) y el set semilla `img/dishes/seed/**` (ADR 0005 fase 7).

## Verificación re-ejecutada (independiente, mía)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`; primera pasada `UP-TO-DATE`) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL 5m03s** (34/34 tareas ejecutadas) |
| XML parseados por mí | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **97 suites / 731 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- 'app/src/test/**'` | **98 ficheros / 742 anotaciones** ⇒ delta **−1 suite / −11 tests** = `RecipePhotoAssetsTest` (único delta) |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL 9m57s** (30/30 ejecutadas) |
| SARIF parseado por mí | `app/build/reports/lint-results-debug.sarif` | **13 resultados, todos `warning`, 0 error**, **0 bajo `app/src`** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1) |
| APK | `bash scripts/gradle.sh assembleDebug` + `unzip -l` | exit 0; `assets/img/recipes/` = **solo `default.jpg` (137.359 B)**; `recipes/seed/` **0**, `recipes/category/` **0**; `img/dishes/seed/` **6 WebP (449.844 B)** |
| Inventario en disco | `find app/src/main/assets/img/...` | `img/recipes` = **1** fichero; `img/dishes` = **6** |
| Greps de alcance en `app/src` (9 patrones) | `grep -r` | **todos 0** (`AssetRecipePhotoSource`, `LocalRecipePhotoSource`, `RecipePhotoSource`, `RecipePhotoAssets.resolve/candidates/CATEGORY_IDS`, `DishImageOrigin.CATEGORY`, `recipe.photos.assets.dir`, `RecipePhotoAssetsTest`) |
| Script retirado | `test ! -e scripts/fetch-recipe-photos.sh` | OK (ausente) |
| Suites focales | `--tests` de los paquetes tocados | verdes: `AssetDishImageStyleReferenceTest` 2, `DishImageStyleReferenceTest` 7, `TaxonomyIntegrityTest` 5, `RecipeArtworkStyleTest` 8, `DishImageSettingsCopyTest` 8, `DishImageOriginSinkTest` 4, `RecipeArtworkImagePrecedenceTest` 5, `DishImageSeedAssetsTest` 6 |

## Escenarios de la spec (contrastados contra artefactos, no contra el resumen)

1. **APK sin fotos locales** — ✅ `default.jpg` (137.359 B) + 6 WebP semilla (449.844 B); 0 de `seed/` y
   `category/`.
2. **Ninguna pantalla muestra foto local** — ✅ 0 referencias en `app/src`; `RecipeArtwork.kt` solo lee
   `LocalGeneratedRecipeArtworkSource` y clasifica vía `originOf`; `MainActivity` sin
   `recipePhotoSource`/`LocalRecipePhotoSource provides …`.
3. **Ancla viva** — ✅ `default.jpg` conservado; la política resuelve por extensiones y `null` sin lanzar
   (`DishImageStyleReferenceTest` 7 verde; `AssetDishImageStyleReferenceTest` 2 Robolectric verde).
4. **Cadena retirada, módulo compartido intacto** — ✅ `RecipePhotoAssets` = solo
   `ROOT`/`DEFAULT_KEY`/`EXTENSIONS`; `ASSET_BASE == "img/recipes/default"` y
   `DishImageSeedAssets.EXTENSIONS == [webp, jpg, png]` derivados; suites verdes.
5. **Diagnóstico sin valor huérfano** — ✅ `DishImageOrigin` = `GENERATED`/`SEED`/`GLYPH`; `originOf` y el
   log `SazonDishImage` solo-debug intactos (dentro del bloque `BuildConfig.DEBUG`).
6. **Microcopy honesta** — ✅ `DISABLED_SUMMARY = "Desactivado: las recetas se muestran sin imagen."` +
   test + registro en `DESIGN.md`; KDoc de `DishImageSettingsState.enabled`, `DishImageSettings` y
   `GatedDishImageGenerator` ya hablan de ficha tipográfica.
7. **Taxonomía sin el campo** — ✅ `recipeCategories.items` = **18 items con solo `id`+`label`**, sin
   `image`/`imageResolution`, `purpose` reescrito; `RecipeCategory` sin `image`; ningún lector Kotlin de
   `.image`; `TaxonomyJson.kt`/`RecipeArtworkStyle.kt`/`RecipeValidator` sin diff.
8. **Docs durables en pasado** — ✅ ADR 0004/0005/0006, `DESIGN.md`, `technical-discovery.md`,
   `taxonomy.md`/`taxonomy.json`, `build-brief.md`, `risks-and-open-questions.md`, `CONTEXT.md`, notas en
   `recipe-photos.md` y `ai-dish-images-proposal.md`; ADR 0006 marca las **cinco** features;
   `scripts/fetch-recipe-photos.sh` **eliminado**; evidencia histórica no reescrita.
9. **Gate** — ✅ exactamente 97/731/0/0/0 y lint 0 errores / 13 warnings, 0 bajo `app/src`.
10. **Sin dependencias nuevas** — ✅ `gradle/libs.versions.toml` 0 diff; `app/build.gradle.kts` solo
    −4 líneas; `init.sh` intacto; sin capturas nuevas.

## Hallazgos por severidad

**Critical/High/Medium: ninguno.**

- **Low-1 — ADR 0004 §Consecuencias sin marcador inline.** `docs/adr/0004-recipe-photos-as-assets.md`
  mantiene el imperativo "Añadir una receta semilla = añadir su foto a `img/recipes/seed/`" y el
  presupuesto "~5,9 MB / 8 MB" sin la marca "*(retirado)*" que sí se puso en §Decisión. El banner
  "Retirada aplicada … ya no existe" y "el historial se conserva abajo tal cual" cubren el escenario 8,
  por lo que **no bloquea**.
- **Low-2 — `feature_list.json.user_visible_behavior` decía "~6,4 MiB".** La medición real es
  **5.954.912 B ≈ 5,68 MiB**. **Alineado** por el orquestador al persistir el `accept` (campo descriptivo;
  la evidencia ya registraba el número correcto).

Informativos (no bloqueantes): `ui/artwork/RecipeArtwork.kt` cambió 2 líneas de solo comentario dentro del
barrido de KDoc del paso 7; el implementer escribió `feature_list.json`/`PROGRESS.md` (convención del
`feature-implementer`, honestamente en `passing`); la evidencia histórica (`docs/validations/**`, specs
antiguas, historial de `PROGRESS.md`) sigue citando la cadena por Non-Goal explícito.

## Evaluación de seguridad

**Sin hallazgos relevantes.** El diff en `data/image`, `data/ai/image` y `ui/artwork` es **solo
comentarios + el miembro de enum `CATEGORY`** (valor de diagnóstico **sin productor**): no cambian
interruptor, tope por sesión, caché, clave canónica, transporte ni red; el log `SazonDishImage` sigue
**solo en debug**. Sin secretos ni credenciales en el diff; sin dependencias nuevas
(`libs.versions.toml` 0 diff); el ancla `default.jpg` es de solo lectura; sin rutas dinámicas nuevas.

## Documentación / harness

`init.sh` intacto (no hay pasos nuevos), `AGENTS.md` intacto. `docs/technical-discovery.md` actualizado en
la frontera durable (assets empaquetados, test borrado, presupuesto de APK). Repo reiniciable desde el
camino estándar.

## Repair brief

**No aplica** (veredicto `accept`). Las dos observaciones `Low` son mejoras cosméticas opcionales; la
Low-2 se alineó al persistir el `accept`.

## Estado y siguiente acción

- El orquestador persiste `retire-recipe-photos` → **`accepted`** en `feature_list.json` con evidencia que
  nombra esta validación independiente (97/731/0/0/0; lint 0 err/13 warn; APK sin `seed|category`; greps a
  0).
- El orquestador crea el commit local en `feature/retire-recipe-photos`; `github-flow` publica (push + PR
  → `main` con `Closes #<n>`). El **merge lo hace la persona**; queda su **smoke manual post-merge**
  (interruptor apagado → resumen "…sin imagen" y ninguna foto de categoría; encendido → imagen generada
  como capa de fondo), no automatizable desde WSL.
