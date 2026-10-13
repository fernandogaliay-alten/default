# Validación independiente — `dish-image-prompt-v2`

- Feature id: `dish-image-prompt-v2`
- Spec: `docs/specs/dish-image-prompt-v2.md`
- Rama/worktree: `feature/dish-image-prompt-v2`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-prompt-v2`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-13
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 098)
- Veredicto: **accept**

## Alcance validado

Correctiva pedida por la persona (2026-10-13), `area: integration`. Dos cambios acotados más el
diagnóstico de debug:

1. **`DishImagePrompt` v2**: la identidad es **siempre** el título de la receta (degradación
   `título → etiqueta de categoría real → plato casero`), el `imagePrompt` del descriptor pasa a ser
   el **detalle** (ya no lo sustituye), se añaden hasta `MAX_KEY_INGREDIENTS = 5` ingredientes clave
   no-despensa (distintos, en el orden de la receta), `COMPOSITION` y `NEGATIVES` reforzados
   (`STYLE_SUFFIX` pierde la parte de negativos).
2. **Invalidación coherente** (ADR 0005): `DishImageCacheKey.VERSION = "dish-image-v2"` y renombrado
   (`git mv`, bytes idénticos) de los 6 assets semilla a `dish-image-v2-*.webp`; `EXPECTED_SEED_KEYS`
   del script en v2.
3. **Diagnóstico del origen** (solo debug): `data.image.DishImageOrigin` + `DishImageOriginSink`,
   `DishImageResolver.onResolved` aditivo, `RecipeArtworkImagePrecedence.originOf`, seam
   `ui.artwork.DishImageOriginDiagnostics` y `Log.d("SazonDishImage", …)` en `MainActivity` **solo**
   con `BuildConfig.DEBUG`.

Sin UI nueva; el gate de capturas `docs/design/concepts/dish-image-prompt-v2/` **no** aplica y
**no** existe.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`; sin red, sin cuota, sin procesos largos, `init.sh` intacto). |
| Tests (forzado, mío) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (5m 13s; 34 tareas ejecutadas). |
| Lint (forzado, mío) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (4m 31s; 30 tareas ejecutadas). |
| Parseo XML propio | `app/build/test-results/testDebugUnitTest/TEST-*.xml` (frescos, mtime 22:09) | **93 suites / 687 tests / 0 fallos / 0 errores / 0 saltados**. |
| Parseo SARIF propio | `app/build/reports/lint-results-debug.sarif` (fresco, mtime 22:14) | **0 errores**; 13 warnings preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**. |

Suites focales del XML: `DishImagePromptTest` 13, `DishImageCacheKeyTest` 13, `DishImageOriginSinkTest`
4, `RecipeArtworkImagePrecedenceTest` 4, `DishImageResolverTest` 24, `DishImageSeedAssetsTest` 6,
`PromptBuilderTest` 12, `DishVisualDescriptorWiringTest` 5, `DishImageRequestTest` 7 (todas 0/0/0).

### Baseline y delta re-derivados (independientes)

`git grep -c '@Test' HEAD -- app/src/test` → **92 ficheros / 673 anotaciones**. Árbol de trabajo:
**93 suites / 687 tests** → delta **+1 suite / +14 tests**, exactamente `DishImageOriginSinkTest` (4,
nueva) + `DishImagePromptTest` (11→13) + `DishImageCacheKeyTest` (12→13) + `DishImageResolverTest`
(19→24) + `RecipeArtworkImagePrecedenceTest` (2→4). 673 + 14 = 687 (el fichero nuevo está *untracked*,
así que `git grep` del árbol de trabajo no lo cuenta; el desglose por XML lo confirma).

### Assets semilla (sin red, sin cuota)

`ls` → 6 ficheros `dish-image-v2-*.webp`; `md5sum` por fichero **coincide** con el reportado
(71dea23f…, 558c3a71…, 3ad15dbc…, 0cc292b1…, 036e0898…, ddd2d7b4…); total **449 844 B ≤ 2 MiB**;
`grep -rn "dish-image-v1" app/src docs/adr docs/technical-discovery.md docs/domain/taxonomy.md
docs/risks-and-open-questions.md scripts` → **0**.

## Escenarios de la spec

- **1** (identidad + ingredientes + composición + estilo + negativos): vector exacto confirmado
  (`Fotografía gastronómica de Arroz con pollo criollo, Española, Pollo, Arroz, Zanahoria,
  <COMPOSITION>, <STYLE_SUFFIX>, <NEGATIVES>`), determinista con dos `Taxonomy`, nunca en blanco, sin
  `,,` ni dobles espacios, `STYLE_SUFFIX` exactamente una vez.
- **2** (con descriptor): el título sigue siendo identidad **y** el `imagePrompt` entra colapsado como
  detalle, antes de ingredientes/composición/estilo/negativos; el título ya **no** desaparece.
- **3** (ingredientes clave): tope 5, distintos, en orden, despensa fuera y omisión sin comas sueltas
  (caso negativo sin ingredientes / solo despensa / nombres vacíos → igualdad exacta).
- **4** (degradación sin regresión): título vacío con categoría real → etiqueta real; categoría
  desconocida / Taxonomía vacía / cocina vacía o desconocida → identidad `plato casero`, cocina omitida,
  sin marcador de "desconocida", siempre con composición/estilo/negativos.
- **5** (`VERSION` v2): `DishImageCacheKey.VERSION == "dish-image-v2"` y
  `of("Arroz con pollo criollo") == "dish-image-v2-arroz-con-pollo-criollo"`, alfabeto `[a-z0-9-]`,
  respaldo `dish-image-v2-plato`, independiente de `Locale`; la clave v2 difiere de la v1 (invalidación).
- **6** (assets por clave v2): los 6 platos resuelven un asset existente/no vacío bajo
  `dish-image-v2-<slug>.<ext>` con `of(título)`, uno por plato, ≤ 2 MiB, bytes idénticos.
- **7** (`PromptBuilder`): `OUTPUT_CONTRACT` conserva `dishKey`/`imagePrompt` opcionales y pide un
  `imagePrompt` de una línea del plato terminado (ingredientes principales y aspecto), sin texto ni
  manos ni personas, manteniendo "usará el título y la taxonomía" y el determinismo.
- **8** (clasificación pura): `originOf(null,img,null)=GENERATED`, `originOf(null,img,SEED)=SEED`,
  `originOf(img,null,null)=CATEGORY`, `originOf(null,null,null)=GLYPH`, determinista.
- **9** (sumidero): gana el último por `recipeId`, ids `null`/en blanco ignorados, `null` para
  desconocido, sin estado compartido entre instancias, sin disco.
- **10** (resolutor): `SEED` al servir el set semilla (también en acierto de **memoria** posterior),
  `GENERATED` en disco/generador, un fallo **no** reporta, la `CancellationException` se propaga sin
  reportar, y el callback por defecto (no-op) devuelve los mismos bytes/orden/ficheros persistidos.
- **11** (solo debug): `MainActivity` provee el seam real **solo** con `BuildConfig.DEBUG`; en release
  queda `NoDishImageOriginDiagnostics` (no-op); la notificación de `RecipeArtwork` es un `LaunchedEffect`
  lateral y ninguna rama de pintado lo lee (mismo fallback, crossfade y geometría).
- **12** (alcance/pureza): `import android|io.ktor|java.net|Log` en `domain/image` → **0**; en
  `data/image` solo `AndroidDishImageCache.kt`; `git diff --name-only` = lista de Expected File Changes
  (+ spec, `feature_list.json`, `PROGRESS.md` y 4 docs durables); ficheros prohibidos sin diff; sin
  dependencias nuevas; `docs/design/concepts/dish-image-prompt-v2/` **no** existe; specs aceptadas y
  `docs/validations/**` no reescritas.

## Hallazgos

Sin hallazgos Critical/High/Medium. Observaciones Low (no bloquean):

- **Low — plantilla ilustrativa `": "` vs regla normativa `", "`.** La spec muestra en §1 un bloque
  ilustrativo con `... <identidad>[, <cocina>]: <detalle>, ...`, pero su regla normativa de ensamblado
  dice "une los segmentos no vacíos con `', '`". El implementer siguió la regla normativa (`", "`), que
  es la que exigen los escenarios (ninguno pide `:`, y sí exigen la ausencia de `,,` y de dobles
  espacios); los 4 docs durables y el espejo del script se actualizaron en coherencia con `", "`.
  Reparación sugerida (redacción, opcional): editar el bloque ilustrativo de la spec para usar `, ` y
  eliminar la ambigüedad.
- **Low — nuevo eje `ui.artwork → data.image`.** `RecipeArtworkImagePrecedence.originOf` y
  `DishImageOriginDiagnostics` importan `com.sazon.app.data.image.DishImageOrigin` (antes `ui/artwork` no
  importaba `data`). Es una decisión **explícita de la spec** (Technical Approach §4/§5: enum puro en
  `data.image`, clasificador puro en `ui.artwork`), con el enum como vocabulario puro; se registra como
  nota de arquitectura, no como violación.

## Seguridad

Sin hallazgos. El log de diagnóstico es **solo debug** (`BuildConfig.DEBUG`) y emite únicamente
`origin`, `id` y `title` (no claves, URLs, bytes ni datos sensibles); el sumidero es en memoria y
efímero por sesión, sin persistencia; no hay red, permisos, cuota ni endpoints nuevos; sin secretos ni
dependencias nuevas; el renombrado de assets conserva bytes y no ejecuta nada. Revisada la ruta
`Recipe.title/categoryId/cuisineId/ingredients → DishImagePrompt` (texto puro, sin sink peligroso) y el
uso de `imagePrompt` como texto de prompt (no se interpola en rutas ni consultas).

## Documentación / harness

`init.sh` intacto (sigue ejecutando el gate real, exit 0). Los cuatro docs durables están actualizados y
coherentes con `dish-image-v2` y el diagnóstico: ADR 0005 (plantilla v2, primera aplicación de la regla
de invalidación, alcance de la correctiva), `docs/technical-discovery.md` (plantilla v2, `VERSION`
`dish-image-v2`, ejemplos/tests y bullet nuevo del diagnóstico), `docs/domain/taxonomy.md` §4
(`imagePrompt` como detalle, no sustituto) y §6 (clave `dish-image-v2-…`, assets renombrados, origen
observable en debug), y `docs/risks-and-open-questions.md` (fila nueva + ejemplos a v2).
`docs/domain-model.md`, `AGENTS.md`, `DESIGN.md`/`docs/design/**`, la propuesta y build **no** se
tocaron (correcto). `feature_list.json` con evidencia completa en `passing`; WIP = 1 (este worktree).
Sin `docs/design/concepts/dish-image-prompt-v2/`.

## Independencia del implementer

Confirmado que el implementer **no** hizo commit, push ni PR: `git log origin/main..HEAD` vacío, rama
sin upstream configurado y `gh pr list --head feature/dish-image-prompt-v2` vacío. Todo su trabajo está
en el árbol de trabajo (ficheros modificados + 3 nuevos + la spec), como exige `feature-flow`.

## Siguiente acción requerida

1. El **orquestador** persiste `accepted` en `feature_list.json`/`PROGRESS.md` (el validador **no**
   modifica el estado) y `github-flow` commitea/publica la rama `feature/dish-image-prompt-v2`
   (`Closes #111`, sin merge).
2. No bloqueante (persona, tras el merge): prueba manual en emulador con clave válida — comprobar en
   logcat `SazonDishImage` que el origen es `SEED` para los 6 platos del demo, `GENERATED` al generar y
   `CATEGORY` cuando el proveedor falla o se agota el tope.
3. No bloqueante (opcional): corregir la redacción del bloque ilustrativo de la spec (hallazgo Low #1).
