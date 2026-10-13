# Validación — `recipe-photos` (issue #83)

- **Fecha:** 2026-10-13
- **Rama/worktree:** `feature/recipe-photos`
- **Spec:** `docs/specs/recipe-photos.md`
- **Validador:** agente independiente con contexto limpio (skill `feature-validator`), distinto del
  implementer
- **Veredicto:** **accept**

## Comprobaciones re-ejecutadas (independientes)

| Comprobación | Comando | Resultado |
| --- | --- | --- |
| Gate estándar | `bash init.sh` | **exit 0** (~10 s), pero test/lint salieron `UP-TO-DATE` → se forzaron abajo |
| Tests (forzados) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL 2 m 42 s, exit 0** |
| Lint (forzado) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL 2 m 40 s, exit 0** |
| Parseo propio de XML | `TEST-*.xml` (mtime fresco) | **65 suites / 467 tests / 0 fallos / 0 errores / 0 saltados**; `RecipePhotoAssetsTest` **11/0/0/0**, `RecipeArtworkStyleTest` **8/0/0/0** |
| Parseo propio de lint | `app/build/reports/lint-results-debug.xml` | **0 errores / 13 warnings** preexistentes (versiones en build files); **0 bajo `app/src`** |
| Empaquetado | `bash scripts/gradle.sh assembleDebug` + inspección del ZIP | exit 0; **31** `assets/img/recipes/**`, **6.092.271 B (5,81 MiB) ≤ 8 MB** (12 seed / 18 category / 1 default) |
| Corrección de assets | re-extrajo los `src` de ambos `code.html`, descargó los 31 con `=s0` y comparó md5 con los empaquetados | **31/31 byte-idénticos**, 0 faltantes, 0 duplicados; JPEG **1376×768**; `alt` del lote A coincide semánticamente con los 12 `seed-*` en orden |
| Escenario 5 recomputado (Python, con `taxonomy.json` + `seed-recipes.json` + FS reales) | reimplementación propia de `candidates`/`resolve` | 12 semillas → foto propia, 18 categorías → foto propia, `default` → `default.jpg`; `null`/`""`/`"  "`/`"no-existe"` → `default`; nada existe → `null`; todo correcto |
| Alcance / pureza | `grep` / `git diff` | **0 `PlateGlyph`**; `BitmapFactory`/`assets.open` solo en `AssetRecipePhotoSource.kt`; `RecipeArtwork` en 4 ficheros / 5 usos, sin cambios de firma ni geometría; `libs.versions.toml` **0 diff**; sin dependencias nuevas; `CONTEXT.md`, `docs/domain-model.md`, `ui.theme/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`, `init.sh`, `scripts/gradle.sh` **0 cambios** |
| Sin commit/push/PR | `git status`, `git log`, `git ls-remote`, `gh pr list` | implementación sin commitear sobre `2f737d9`; sin upstream; `ls-remote --heads origin feature/recipe-photos` vacío; PR inexistente |

## Escenarios de la spec

- 1 (foto propia de semilla) ✓, 2 (receta IA → foto de categoría) ✓, 3 (respaldo parcial) ✓,
  4 (desconocido/`null` → `default` → glifo; sin excepción ni bloque vacío) ✓,
  5 (existencia de assets sobre Taxonomía y catálogo reales, recomputado) ✓,
  6 (un componente / 5 puntos de llamada, 0 `PlateGlyph`) ✓,
  7 (geometría preservada) ✓, 8 (offline / sin dependencias / ≤ 8 MB) ✓,
  9 (modo oscuro: la foto no se tiñe y `contentDescription = null` se mantiene) ✓ por inspección,
  10 (ADR 0004 + docs durables coherentes) ✓.
- Scenario 9/10 visual en emulador: **pendiente por diseño** (lo revisa la persona; `AGENTS.md` no
  automatiza UI de emulador).

## Hallazgos

**Critical / High / Medium: ninguno.**

**Low (informativos, no bloqueantes; un defecto visual/perf se corrige con una feature correctiva):**

1. El KDoc de `AssetRecipePhotoSource.kt` dice que los assets están acotados a 960 px de ancho, pero
   los 31 son **1376×768** (sin `cwebp`/ImageMagick, el script conserva el original). Un bitmap
   ARGB_8888 ≈ **4,03 MiB** y el techo del LRU-16 ≈ **64,5 MiB**, frente a los ~33 MiB estimados en
   la spec a ≤960 px. El riesgo ya está registrado en `docs/risks-and-open-questions.md` y en los
   *Unknowns* de la spec. Seguimiento sugerido: corregir el comentario y/o `inSampleSize`, RGB_565,
   LRU menor, o regenerar en WebP a 960 px.
2. La evidencia afirma que migrar a WebP no requiere cambios en Kotlin, pero `RecipePhotoAssetsTest`
   fija `.jpg` literal en 4 aserciones. Endurecimiento barato: asertar
   `resolved!!.substringBeforeLast('.')` o `endsWith`.
3. `docs/domain/taxonomy.json` conserva `imageResolution: assets/img/recipes/{id}.webp` y los
   `image: …/<id>.webp` por categoría, superados por el ADR 0004. Ningún código lee ese campo y la
   spec congeló el fichero a propósito (solo se actualizó `docs/domain/taxonomy.md` §6). Reconciliar
   o anotar en un seguimiento.
4. `docs/technical-discovery.md` (~línea 504) enumera solo los ADR 0001–0003 como "afectan a este
   documento" pese a citar el 0004 en otros puntos.
5. Informativo: el seam Android (`AssetRecipePhotoSource`) no tiene test unitario (la cadena pura sí
   está cubierta en JVM, como pedía la spec); cada resolución prueba hasta 9 aperturas de asset más
   una reapertura para decodificar (I/O menor). La rama estaba 4 commits por detrás de `origin/main`
   → el orquestador debe `scripts/sync-worktree.sh recipe-photos` antes de publicar. Los 31 assets
   están *untracked* → el commit de aceptación debe añadir `app/src/main/assets/img/`.

## Evaluación de seguridad

Sin hallazgos relevantes: sin dependencias nuevas (nada de Coil), sin red en runtime, assets dentro
del APK, sin permisos ni cambios de manifiesto, sin secretos ni PII, degradación a `null` sin crash,
ids de receta generados por la app (sin superficie de path injection) y el script de descarga
`scripts/fetch-recipe-photos.sh` es de un solo uso, dev-only (fuera de `init.sh`), HTTPS, valida por
*magic bytes*, falla rápido (`exit ≠ 0`), con `set -euo pipefail` + `mktemp`/trap y sin imprimir
credenciales.
