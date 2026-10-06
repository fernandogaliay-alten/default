# Validation Record: recipe-validator

- Feature: `recipe-validator`
- Spec: `docs/specs/recipe-validator.md`
- Date: 2026-10-05
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → exit 0 (`./gradlew test` + `./gradlew lint`,
  sin procesos largos).
- `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL; `RecipeValidatorTest` tests=12 failures=0
  errors=0; 38 tests en total, 0 fallos.
- `./gradlew lint --rerun-tasks` → BUILD SUCCESSFUL, 0 errores; ningún issue en
  `RecipeValidator`/`IngredientText`/`domain.validation`.
- Pureza: `com.sazon.app.domain.validation` sin imports `android.*`/`androidx.*`.
- Build intacto: `git diff HEAD -- app/build.gradle.kts gradle/libs.versions.toml` vacío.
- Citas de numeración de preguntas en `docs/risks-and-open-questions.md` consistentes.

## Conformance

- Escenarios 1–11 cubiertos y con el comportamiento esperado, más el extra de `ingredientId`
  desconocido degradado a ingrediente libre.
- Orden de reglas de `docs/domain/taxonomy.md` §5 respetado: esquema/ids → dieta → restricciones
  duras (allergens curados + keywords) → coherencia (deriva ≤ 5 min y `declaredAllergens`).
- Una respuesta malformada nunca lanza excepción (`Malformed`); un elemento ilegible se descarta con
  `SCHEMA` sin tumbar el lote.
- Alcance respetado: sin UI, generador, prompt, persistencia ni ranking/`matchScore`.
- Preguntas 5 (trazas) y 7 (ingrediente libre) resueltas con renumeración coherente.

## Findings

- Sin hallazgos Critical/High/Medium.
- Low (informativos, no bloqueantes):
  1. `docs/risks-and-open-questions.md` tiene dos entradas "Resueltas" con "(antes pregunta 5)";
     legibles por contexto pero podrían pulirse.
  2. `FALLBACK_CATEGORY_ID = "other"` está fijado en código en lugar de leerse de
     `recipeCategories.fallback` (aceptable: `Taxonomy` no expone el fallback).
  3. Riesgo residual ya documentado: un ingrediente libre que viole dieta no se detecta por la regla
     de dieta, solo por keywords de restricciones duras.

## Result

`accept`. Sin reparaciones requeridas. El orquestador persiste `accepted` en `feature_list.json` y
crea el commit de la feature.
