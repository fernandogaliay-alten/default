# Validation Record: generator-settings-step

- Feature: `generator-settings-step`
- Spec: `docs/specs/generator-settings-step.md`
- Date: 2026-10-09
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → exit 0 (`./gradlew test` + `./gradlew lint`,
  sin procesos largos).
- `./gradlew testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL (32/32 tareas), `TEST_EXIT=0`;
  parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **31 suites, 200 tests,
  0 failures, 0 errors, 0 skipped**; `GeneratorSettingsStateTest` = **11**.
- `./gradlew lint --rerun-tasks` → BUILD SUCCESSFUL (28/28 tareas), `LINT_EXIT=0`; informe
  `lint-results-debug.xml`: **0 errores, 13 warnings** preexistentes (2 `AndroidGradlePluginVersion`,
  7 `GradleDependency`, 3 `NewerVersionAvailable`, 1 `OldTargetApi`), **0 en ficheros de la feature**.
- Pureza: `GeneratorSettingsState` solo importa `domain.profile` y `domain.taxonomy` (sin
  Compose/Android).
- Build/dominio intactos: `git status` no lista `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `domain/**`, `data/**`, la Taxonomía ni `seed-recipes.json`; `ui/preferences/**` sin modificar.

## Conformance

- Escenarios 1–8 cubiertos:
  - **S1 precarga:** `initial(preferences, limits)` copia `defaultServings` (con `coerceIn` a
    `taxonomy.servings` 1..12), franja, dificultad y alergias; `mealTypeId == null`, `notes == ""`.
  - **S2 tipo de comida:** chips desde `taxonomy.mealTypes` (5: `breakfast/lunch/dinner/snack/dessert`,
    "Postre"); `selectMealType` single-select opcional (el segundo toque → `null`).
  - **S3 single-select:** `selectTimeBucket`/`selectDifficulty` reemplazan sin acumular.
  - **S4 recorte:** `setServings` usa `coerceIn(min, max)`; no sale de `1..12`.
  - **S5 alergias:** `toggleHardRestriction` sin duplicados; `clearHardRestrictions` → `hasNoRestrictions`;
    `HardRestrictionsEditor` reutilizado **sin modificar** usa `tertiary`/`tertiaryContainer`, **nunca**
    `colorScheme.error`.
  - **S6 enlace Perfil:** `MainShell` pasa `onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) }`;
    no construye la pantalla Perfil ni sale de la app.
  - **S7 notas:** `typeNotes` conserva el texto (multilínea y vacío) tal cual.
  - **S8 navegación/borrador:** el paso 2 real monta `GeneratorSettingsScreen`; `GeneratorSettingsPlaceholder`
    eliminado sin referencias colgantes; `settings` en `rememberSaveable` + `GeneratorSettingsStateSaver`;
    "Generar recetas" sigue `enabled` y `step.next() == null` (sin efecto).
- No-goals respetados: sin generación/validador/prompt/`GenerationConstraints` (solo mención en
  comentario); sin persistir el borrador; sin escribir el `PreferenceProfile`; sin Resultados/Detalle/Perfil;
  sin `navigation-compose`, iconos ni DI.

## Findings

- Sin hallazgos Critical/High/Medium.
- Low/informativos (no bloqueantes):
  1. Escenarios 6 y 8 viven en la capa Compose (navegación y round-trip del `Saver`): no automatizables
     por AGENTS.md; verificados por inspección y pendientes del pase manual de la persona.
  2. `escenario_2` fija el orden literal de `mealTypes`: correcto hoy y actúa como guarda.
  3. `escenario_4` usa 21 folds en lugar de aseverar el no-op en los extremos; equivalente por `coerceIn`.
  4. Microcopy de los títulos de tarjeta hardcodeado (dictado por la spec); las opciones/labels de grupo
     sí salen de la Taxonomía.
  5. `Validator Checklist` de la spec sin marcar (patrón heredado).
  6. `docs/validations/generator-settings-step.md` creado por el orquestador.

## Security

Sin riesgos relevantes: feature de estado/UI sin red, sin secretos, sin persistencia y sin llamadas
externas. `notes` es texto libre que vive solo en memoria de sesión.

## Result

`accept`. Sin reparaciones requeridas. El orquestador persiste `accepted` en `feature_list.json` y crea
el commit de la feature.
