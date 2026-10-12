# Validation Record: recipe-artwork

- Feature: `recipe-artwork`
- Spec: `docs/specs/recipe-artwork.md`
- Date: 2026-10-12
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `bash init.sh` (WSL, vía `scripts/gradle.sh`) → exit 0 (`./gradlew test` + `./gradlew lint`, sin
  procesos largos).
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL, 34/34 tareas
  ejecutadas.
- `bash scripts/gradle.sh lint --rerun-tasks` → BUILD SUCCESSFUL, 30/30 tareas ejecutadas.
- Parseo propio de `app/build/test-results/testDebugUnitTest/*.xml` → **60 suites, 413 tests, 0
  fallos/errores/saltados**; 8 nuevos (`RecipeArtworkStyleTest` 8) + 405 preexistentes en verde. Los
  números coinciden exactamente con los del implementer (reproducidos, no aceptados de su resumen).
- Parseo propio de `lint-results-debug.xml` → **0 errores, 13 warnings** preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1);
  **0 en ficheros de la feature** (`app/src`).

## Exhaustive mapping recomputed from source

- `docs/domain/taxonomy.json` real: **18 `recipeCategories`** y **10 `cuisines`** (ids coinciden con la
  spec).
- Del código (`RecipeArtworkStyle`), extraído con un parseo propio y comparado con la Taxonomía: las
  ramas de glifo cubren los 18 ids con **18 `RecipeGlyph` distintos (`== RecipeGlyph.entries`)** y las de
  acento los 10 ids con **10 `RecipeAccent` distintos + `DEFAULT` (`== RecipeAccent.entries`)**;
  `else`/`other` → `PLATE`/`DEFAULT`.
- El test itera la Taxonomía **real** vía `System.getProperty("taxonomy.asset.path")` (mismo patrón que
  `TaxonomyIntegrityTest`): no es una comprobación de no-nulidad, afirma la tabla esperada, la unicidad y
  un caso negativo de fallback.

## Conformance

- **Un único componente** `ui.artwork.RecipeArtwork` sustituye las 4 copias: `grep -rn PlateGlyph
  app/src` → **0**; el degradado del placeholder (`primaryContainer` → `secondaryContainer`) solo vive en
  `RecipeArtwork.kt` (el otro `Brush.linearGradient` es el hero preexistente de `HomeScreen`, ajeno).
- 5 puntos de llamada (`RecipeCard`, `ResultRecipeCard`, `FavoriteRecipeCard` miniatura y bloque de
  rejilla, hero de `RecipeDetailScreen`) con **geometría y tamaños preservados**: 16:10 (48 dp), 16:10
  (56 dp), 96x96 `clip.small` (40), alto 96 (40), hero alto 240 (72). Comportamiento no-visual (título,
  meta, píldora, corazón, click, pestañas, barra inferior) intacto.
- **Fallback total**: `null`/`""`/`"  "`/desconocido → `PLATE` + `DEFAULT`; `PLATE` reproduce el
  `PlateGlyph` de dos círculos concéntricos; nunca vacío ni excepción.
- **Pureza y determinismo**: `RecipeArtworkStyle.kt` solo importa `domain.recipe.Recipe` (sin
  Compose/Android), sin `Random`/reloj/red; `ui/artwork` sin hex sueltos (acentos por roles de
  `MaterialTheme.colorScheme`).
- **Alcance**: `git diff --stat` solo con los ficheros previstos; `domain/**`, la Taxonomía,
  `seed-recipes.json`, Room, `ui.theme/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml` e
  `init.sh` sin cambios; sin dependencias nuevas; sin assets de imagen.
- **Documentación**: `DESIGN.md` registra la extensión del placeholder (glifo por categoría, acento por
  cocina, fallback) y la desviación explícita de capturas (issue #77), y marca resuelta la Open Design
  Question 4; `docs/technical-discovery.md` anota `ui.artwork.RecipeArtwork` y que `assets/img/recipes/`
  no se empaqueta en el MVP.
- **Exención de capturas**: la ausencia de `docs/design/concepts/recipe-artwork/` está **autorizada** por
  la persona (issue #77 + nota de la entry en `feature_list.json`); se trató como decisión válida, **no**
  como defecto.
- **Sin commit/push/PR**: el trabajo está en disco; HEAD es solo el commit de provisioning
  (`chore: registra recipe-artwork`), sin commit de implementación ni rama remota/PR.

## Findings

- Sin hallazgos Critical/High/Medium.
- Low / informativos (no bloqueantes):
  1. **Redacción del degradado**: la spec/KDoc dicen "start arriba → end abajo" mientras el código usa
     `Brush.linearGradient(colors)` con los offsets por defecto (`Offset.Zero` → `Offset.Infinite`, es
     decir diagonal). Es **idéntico al placeholder aceptado antes**, así que no hay regresión y `DEFAULT`
     es fiel; la variedad por cocina la da el par de colores. Nit de redacción.
  2. **Scenario 6** (roles del tema en claro/oscuro) es solo por inspección, coherente con el Verification
     Plan de la spec y con AGENTS.md (no se automatiza UI de emulador).
- Drifts preexistentes, fuera de alcance y anotados: `docs/domain/taxonomy.md` §6 sigue citando
  `assets/img/recipes/{id}.webp`; el `Brush.linearGradient` extra de `HomeScreen.kt`.
- **Seguridad**: sin hallazgos. Offline y determinista, sin red, sin datos personales, sin secretos, sin
  dependencias nuevas.

## Result

`accept`. Sin reparaciones requeridas. El orquestador persiste `accepted` en `feature_list.json` y crea
el commit local de la feature (la publicación queda para el orquestador de `github-flow`). Queda para la
persona la revisión manual de emulador (Home, Resultados, Favoritos lista+rejilla y Detalle, en claro y
oscuro) del acabado de los 18 glifos y los 11 degradados contra el registro de `DESIGN.md`; un defecto
visual se corrige con una feature correctiva.
