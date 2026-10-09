# Validation Record: local-result-filters

- Feature: `local-result-filters`
- Spec: `docs/specs/local-result-filters.md`
- Date: 2026-10-09
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → exit 0 (`./gradlew test` + `./gradlew lint`,
  sin procesos largos). Una primera pasada falló por un flake transitorio de resultados de test
  (`NoSuchFileException: …in-progress-results-generic.bin`) tras el `./gradlew --stop` del trap de
  salida; la segunda pasada quedó en verde.
- `./gradlew testDebugUnitTest --rerun-tasks` → 28 suites, **172 tests, 0 fallos/errores/saltados**;
  nuevos: `LocalResultFilterTest` 8 y `HomeRecipesTest` 8 (de 3 a 8, +5 de `filteredBy`); 159
  preexistentes en verde, sin regresiones.
- `./gradlew lint` (vía `init.sh`) → 0 errores, 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature.
- Pureza: `com.sazon.app.ui.filters.LocalResultFilter`/`LocalResultFilters` solo importan
  `domain.recipe`/`domain.taxonomy`; `ResultFilterRow` es la única capa Compose.
- Build intacto: `app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios; sin hex sueltos en
  `ResultFilterRow.kt` (usa `colorScheme.primary`/`secondary` y `SazonPillShape`/`SazonSpacing`).

## Conformance

- Escenario 3 (exigido): `Fast` incluye 20 y la frontera 30, excluye 31; `FAST_MAX_MINUTES == 30`.
- Escenario 4 (exigido): `Vegetarian` excluye `pollo`/`salmon`/`gamba`/`ternera` (tags
  `poultry`/`fish`/`seafood`/`meat`) e incluye la vegetal; caso negativo real que no pasa en vacío.
- Escenario 5: ingrediente libre (`ingredientId = null`) no se excluye; se incluye además el mismo
  caso con id real para que no pase en vacío.
- Escenarios 1 y 2: `All` preserva lista y orden; `Category` filtra por `categoryId` y una categoría
  inexistente devuelve vacío.
- Escenarios 6, 7 y 8: fila de chips data-driven con derivados agrupados y en oliva
  (`secondary`) frente a categorías terracota (`primary`); estados vacíos correctos; offline por
  inspección (sin imports de red/Android).
- Claves: `fromKey`/`key` con round-trip (`All`, `Category`, `Fast`, `Vegetarian`, clave desconocida
  → `All`); `vegetarianTags` deriva de `dietPatterns[vegetarian].excludedTags` con respaldo
  defensivo.
- Disciplina de alcance: sin Favoritos, sin buscador por texto, sin red, sin persistencia, sin tocar
  `Recipe`/Taxonomía/`seed-recipes.json`/`domain.suggestions`/`data.suggestions` ni el build.
- `docs/technical-discovery.md` coherente con la implementación.

## Findings

- Sin hallazgos Critical/High/Medium.
- Low + informativo (no bloqueantes):
  1. Nombre del test `escenario_6_…` desalineado con la spec (cubre claves/`label`/`isDerived`, no la
     fila de chips, no testeable en JVM).
  2. Aserción redundante en `escenario_4` (`assertTrue(ids.none { … })` junto a
     `assertEquals(listOf("vegetal"), ids)`).
  3. `FALLBACK_VEGETARIAN_TAGS` es público; podría ser `private`.
  4. Flake transitorio del harness Gradle en la primera pasada de `init.sh` (robustez del harness).
  5. `Validator Checklist` de la spec sin marcar (por diseño; codifica las comprobaciones del
     validador).

## Result

`accept`. Sin reparaciones requeridas. El orquestador persiste `accepted` en `feature_list.json` y
crea el commit local de la feature (sin push ni PR: la publicación la hace `github-flow`).
