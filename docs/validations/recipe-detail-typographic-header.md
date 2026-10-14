# Validación independiente — `recipe-detail-typographic-header`

- Feature id: `recipe-detail-typographic-header`
- Spec: `docs/specs/recipe-detail-typographic-header.md`
- Rama/worktree: `feature/recipe-detail-typographic-header`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/recipe-detail-typographic-header`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 105;
  sin commit/push/PR, trabajo en disco — esperado)
- Veredicto: **accept**

## Alcance validado

Tercera de las cinco features del ADR 0006 (`area: ui`, depende de `recipe-card-typographic`, ya
`accepted`). Correctiva de `recipe-detail-screen`: el Detalle deja de abrir con foto + tarjeta
superpuesta y pasa a **hero tipográfico + tira de metadatos**.

1. **Lógica pura nueva** `ui/detail/RecipeDetailHeroFormat.kt` (sin Compose ni Android): geometría
   publicada (`HERO_HEIGHT_DP = 240`, `FILLET_HEIGHT_DP = 6`, `WATERMARK_DP = 190`,
   `WATERMARK_ALPHA = 0.12f`, `MAIN_INGREDIENTS == RecipeCardFormat.MAIN_INGREDIENTS`),
   `HeroContent(category, title, ingredients)` y `of(taxonomy, recipe)` = `RecipeCardFormat.categoryLine`
   + `RecipeCardFormat.mainIngredients` (una sola regla de "categoría · cocina" y de ingredientes
   principales: despensa fuera, orden de declaración, tope 3, respaldo al `id`).
2. **Capa Compose** (`RecipeDetailScreen.kt`): hero de 240 dp con margen lateral 16 dp y clip de
   esquinas **inferiores** 24 dp, relleno `recipeAccentBrush(RecipeArtworkStyle.of(recipe).accent)`,
   marca de agua `RecipeGlyphArtwork` 190 dp tintada `onSurfaceVariant` al 12 % desbordando por la
   esquina inferior derecha (+24/+6, recortada por el clip), `Column` al pie con categoría en versalitas
   (11 sp Bold, 0.14 em), título Manrope 29/31 sp (−0.025 em, 2 líneas con elipsis), tres filas de
   ingrediente (nombre 12,5 sp `onSurfaceVariant` + cantidad 12,5 sp Medium tabular `onSurface`,
   hairline `outline` bajo cada fila incluida la última) y filete de 6 dp con degradado horizontal de la
   cocina; estilos locales `HeroTypography` desde `SazonFontFamilies`. **Eliminados** `DetailCard` y el
   `Column(offset(y = -SazonSpacing.lg))`; `MetaStrip` pasa a bloque propio (`surface`, esquinas
   inferiores 24 dp, margen lateral 16 dp, sombra suave 4 dp, cuatro columnas `weight(1f)` con hairline
   vertical `outline` no tras la última, etiqueta 9,5 sp versalitas, valor Manrope Bold 14 sp tabular)
   alimentado por `RecipeDetailFormat.metaEntries` **sin tocar** sus cuatro valores.
3. **Capturas obligatorias revisadas** (`detalle-receta/{screen.png,screen-dark.png,code.html,DESIGN.md}`,
   todas versionadas y **sin modificar**): hero sin foto con marca de agua y filete, tira de cuatro
   valores, pestañas, stepper, checklist y barra inferior.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0**, pero `:app:testDebugUnitTest` salió **UP-TO-DATE** ⇒ se forzó la tirada (abajo) |
| Tests completo (forzado, mío) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (3m45s; 34/34 tareas **executed**), exit 0 |
| Parseo XML propio (fresco, 11:33) | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **96 suites / 730 tests / 0 fallos / 0 errores / 0 saltados** |
| `ui.detail` (del XML fresco) | — | `RecipeDetailHeroFormatTest` **10/10**, `ServingScalerTest` 8, `IngredientFormatTest` 5, `RecipeDetailFormatTest` 4 (los cuatro valores de la tira), `RecipeDetailStateTest` 5 |
| Baseline en `HEAD` | `git grep -c "@Test" HEAD` | **95 ficheros / 720 `@Test`** vs 96/730 en el árbol ⇒ delta **+1 suite / +10 tests** |
| Lint (forzado, mío) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (3m17s; 30/30 tareas **executed**), exit 0 |
| SARIF fresco (11:36) | `app/build/reports/lint-results-debug.sarif` | **13 resultados**, 0 errores: `GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1 (entorno); **0 bajo `app/src`** |
| Alcance | `git status --short` + `git diff --name-only` | Expected File Changes + spec del planner + `feature_list.json`/`PROGRESS.md` (harness); **sin** diff en `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `app/src/main/assets/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`, `ui/artwork/**`, `ui/theme/**`, `ui/home/**`, `MainShell.kt`, dominio ni datos |
| Capturas | `git ls-files docs/design/concepts/recipe-detail-typographic-header/` + `test -f` | 5 ficheros **versionados y sin modificar**; sin capturas nuevas ni borradas |
| `RecipeArtwork(` / `RecipePhotoSource` | `grep -rn` en `ui/detail` | **0** (y **0 llamadas** en todo `ui/**`: solo la **definición** en `ui/artwork/RecipeArtwork.kt`) |
| `DetailCard` / `offset(y = -` | `grep -rn` en `ui/detail` | **0** / **0** |
| Hex sueltos | `grep -rn "0xFF"` fuera de `ui/theme` | solo las **2 máscaras de bytes preexistentes** (`data/ai/AiRecipeIds.kt:115`, `domain/profile/PreferenceSignature.kt:73`) |
| Caché/generador/interruptor en el hero | `grep -rn "PhotoSource\|ArtworkCache\|imagesEnabled\|dishArtwork\|generator"` en `ui/detail` | **0** (el hero no consulta nada: no decide OQ7) |
| Intactos por diff | `git diff -U0 RecipeDetailScreen.kt` (hunks) | hunks solo en imports/constantes/hero/`MetaStrip` + `HeroTypography` al final ⇒ `DetailTabs`, `IngredientsTab`, `StepsTab`, `NotesTab`, `DetailBottomBar`, `HeartGlyph` y `RecipeDetailStateSaver` **byte a byte**; firma de `RecipeDetailScreen` (`recipe, taxonomy, isSaved, onToggleSave, onStartCooking, onBack, modifier`) **sin cambios** |

## Escenarios de la spec contrastados

| Escenario | Comprobación independiente | Resultado |
|---|---|---|
| 1 — hero tipográfico sin foto | código del hero + capturas (claro/oscuro) + `RecipeDetailHeroFormatTest` (categoría/título/3 ingredientes con cantidad) | **Cumple**: no hay foto ni placeholder; orden categoría → título → ingredientes → marca de agua → filete |
| 2 — geometría conservada | `code.html` (`.hero{height:240px;border-radius:0 0 24px 24px;padding:18px}`) vs código (`HERO_HEIGHT_DP`, `HeroBottomShape` inferior, margen 16, `Arrangement.Bottom`) | **Cumple**: 240 dp, marca de agua 190 dp @12 %, filete 6 dp, margen `SazonSpacing.md`, esquinas inferiores 24 dp, contenido al pie |
| 3 — sin tarjeta superpuesta; tira en su lugar | greps (`DetailCard`/`offset` → 0) + `MetaStrip` justo bajo el hero con los cuatro pares | **Cumple**: `RecipeDetailFormat.metaEntries` intacto (test 4/4 verde) |
| 4 — receta sin datos no rompe | 4 negativos **no vacíos** en `RecipeDetailHeroFormatTest` (vacía → lista vacía sin excepción; 2 → 2; solo despensa → 3; desconocidos → `"futurista · marte"`) + `RecipeCardFormat.mainIngredients` (fallback) y `RecipeArtworkStyle.of` (PLATE/DEFAULT) | **Cumple**: sin excepción, sin filas vacías (`HeroIngredients` retorna temprano con lista vacía) ni hueco en blanco |
| 5 — claro/oscuro con roles del tema | código (`onSurfaceVariant.copy(alpha = 0.12f)`, `onSurface`/`onSurfaceVariant`, `surface`, `outline`) + grep de hex (0 nuevos) + captura oscura | **Cumple**: cero hex fuera de `ui/theme` |
| 6 — raciones/pestañas/barra intactas | hunks del diff: esas secciones **no aparecen**; `RecipeDetailFormatTest` y `RecipeDetailStateTest` verdes | **Cumple** |
| 7 — se abre/cierra igual | firma del `@Composable` sin cambios; `ui/MainShell.kt` **sin diff** | **Cumple** |
| 8 — descripción no pintada | único `description` en `ui/detail` es `step.description` (timeline, preexistente); `DetailCard` eliminado | **Cumple** (desviación declarada) |

Contrato de tests: los 10 casos declarados existen y pasan; los **negativos son no vacíos** y hay
determinismo por igualdad estructural. La verificación JVM se limita a contenido/geometría pura porque el
repo **no** tiene `androidx.compose.ui.test` en `testImplementation` (justificado en la spec): el acabado
se revisa contra las capturas.

## Hallazgos

Sin hallazgos **Critical/High/Medium**. Ninguno bloquea la aceptación.

- **Low (no bloqueante)**: `HeroBottomShape` (`RecipeDetailScreen.kt:74-75`) **duplica el valor** de
  `SazonShapes.medium` (`24.dp`) en un literal, aunque su KDoc nombre el token (`Shape.kt:14`:
  `medium = RoundedCornerShape(24.dp)`). El radio resultante es el correcto (y el token no es componible
  tal cual para "solo esquinas inferiores"), así que es solo trazabilidad/pulido: si se toca, extraer el
  radio del token o dejarlo documentado como literal derivado. No exige re-validación.
- **Low / Info (tensión captura ↔ contrato, resuelta a favor del contrato)**: la captura pinta la
  categoría como `DE LA COCINA LATINA`, mientras la implementación pinta `ARROZ · LATINA`
  (`RecipeCardFormat.categoryLine`), que es lo que exige el **Escenario 1** de la spec. La diferencia de
  copy no figura en "Desviaciones declaradas": aviso para la revisión visual de la persona; si se
  quisiera el literal de la captura, sería una feature correctiva, no un bloqueo.
- **Info**: el prototipo (`code.html`) marca el título a `font-weight:800`; la implementación usa
  `FontWeight.Bold` (700), igual que el precedente **aceptado** de `RecipeFicha` con el mismo prototipo.
  Coherente con la baseline aceptada; sin acción.
- **Info**: `ui.detail` ahora importa `ui.home.RecipeCardFormat` mientras `ui.home` ya importa
  `ui.detail.IngredientFormat` (dependencia mutua de paquetes). Es la reutilización que **manda** el
  Approach de la spec, con precedente documentado; sin acción.
- **Info**: la línea de alcance de la spec ("`RecipeArtwork\|RecipePhotoSource` → 0") chocaba con su
  propio Approach (que exige `RecipeArtworkStyle.of(recipe).accent`); el implementer la reconcilió a
  "**0 llamadas a `RecipeArtwork(`**" en la spec y en `PROGRESS.md`. Verificado de forma independiente:
  0 llamadas en `ui/detail` y en todo `ui/**` salvo la definición ⇒ el non-goal (que `RecipeArtwork`
  quede **sin consumidores**) se cumple.
- **Positivo**: el `init.sh` de este worktree reportó `:app:testDebugUnitTest` **UP-TO-DATE**, así que la
  evidencia del implementer **no** se aceptó a ciegas: la tirada forzada (34/34 tareas ejecutadas) y el
  lint forzado (30/30) reproducen los mismos conteos (96/730, 0 fallos; 13 warnings de entorno).

## Seguridad

Sin hallazgos: el diff es Compose + lógica pura JVM. No hay red, ni I/O, ni secretos, ni permisos, ni
autenticación/autorización, ni persistencia, ni entrada de usuario nueva, ni datos personales, ni
dependencias nuevas (`app/build.gradle.kts` y `gradle/**` intactos), ni assets nuevos. El hero no
consulta caché, generador ni el interruptor de imágenes del plato (grep → 0), así que no abre ninguna
superficie nueva. No aplica ninguna sección de la checklist de seguridad más allá de
dependencias/configuración, ambas en verde.

## Documentación / harness

- `docs/technical-discovery.md`: actualizado (párrafo del Detalle: hero tipográfico, tira como bloque
  propio, `RecipeDetailHeroFormat` entre la lógica pura, el Detalle deja de consumir `RecipeArtwork`).
- `docs/adr/0006`: `Estado` (tercera feature; `recipe-card-typographic` marcada `accepted`), §7 y
  "Consecuencias" reconciliados con "el hero del Detalle ya no usa `RecipeArtwork`".
- `DESIGN.md` → `Shapes`: retirada la viñeta de la tarjeta superpuesta del Detalle y sustituida por el
  apilado hero + tira (sin contradicción con `Core Screens` 5). Sin cambios de tokens ni frontmatter.
- `docs/domain-model.md`, `AGENTS.md`, `docs/risks-and-open-questions.md`, `docs/design/concepts/**`:
  **sin cambios**, como declaraba la spec.
- `init.sh`/`scripts/**`: intactos; el worktree sigue reiniciable por el camino estándar (gate exit 0).
- `feature_list.json`: `passing` + evidencia; `accepted` lo persiste el orquestador, nunca el validador.
- E2E/UI automatizada: **no aplica** (no hay suite E2E persistente y `AGENTS.md` no automatiza UI de
  emulador); queda para la persona la revisión visual en claro y oscuro.

## Cierre

El **orquestador** de `feature-flow` persiste `accepted` en `feature_list.json` con la evidencia del
validador, registra la sesión en `PROGRESS.md` y crea el **commit local** en
`feature/recipe-detail-typographic-header`. **Sin** push ni PR (los hace `github-flow`).
