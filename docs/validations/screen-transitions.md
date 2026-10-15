# Validación independiente — `screen-transitions`

- **Feature**: `screen-transitions` (`area: ui`; ampliación de UI pedida por la persona 2026-10-15)
- **Spec**: `docs/specs/screen-transitions.md`
- **Rama / worktree**: `feature/screen-transitions`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/screen-transitions`
- **HEAD**: `14bc91f` (trabajo en disco, sin commit)
- **Estado previo**: `passing` (implementer; autoverificado, sin commit/push/PR)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | exit 0, pero todo `UP-TO-DATE` (config cache) → insuficiente por sí solo |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, `BUILD SUCCESSFUL`, 34 tareas ejecutadas (3m53s) |
| Parseo propio de XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **100 suites / 747 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline re-derivada | `git grep -c "@Test" HEAD -- app/src/test` | **98 ficheros / 739 anotaciones** en `HEAD`; delta **+2 suites / +8 tests** |
| Tests nuevos | lectura + XML | `MotionTokensTest` 3, `MotionPolicyTest` 5 (=8); `DesignTokensTest` 8 sin cambios |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | exit 0, `BUILD SUCCESSFUL`, 30 tareas ejecutadas (3m38s) |
| Parseo propio del SARIF | `app/build/reports/lint-results-debug.sarif` | **0 errores, 13 warnings** preexistentes, **0 bajo `app/src`** |
| Caso negativo de tokens | reimplementación independiente del parser en `/tmp` | copia con `duration: 999ms` → 1 deriva detectada (no pasa en vacío) |

Baseline `98/739` confirmada por mi cuenta; los 8 tests nuevos son exactamente las 2 suites nuevas, de
modo que **ninguna aserción existente cambió** (el diff de `DesignTokensTest.kt` solo extrae el parser
`frontmatterSectionMap` a `DesignFrontmatter.kt`, sin tocar ningún `@Test`).

## Checklist del validador (spec)

- [x] Los cuatro frentes usan la **única** primitiva `MotionContent` en los 13 puntos listados
      (`MainShell` pestaña/página/Detalle/Modo cocina; `OnboardingFlow` paso; `GeneratorFlow` paso+stage;
      `AiSearchFlow` stage; `ResultsScreen` estado; `FavoritesScreen` vacío↔poblado; `HomeScreen` dos
      bloques). Sin duraciones ni desplazamientos literales fuera de `SazonMotion` (los únicos literales
      en `ui/**` son preexistentes y ajenos: crossfade 250 ms, spinner 1800 ms, pulso 900 ms, geometría).
- [x] Escenario 7: `MotionTokensTest` compara `duration`/`slide`/`easing`/`enterOnTop` con el bloque
      `motion:` del frontmatter y el caso negativo falla de verdad (verificado con parser propio).
- [x] Escenario 8: `MotionPolicyTest` cubre `FORWARD`/`BACKWARD`/`NONE` para pestañas, páginas y pasos,
      más los offsets `+24`/`-24`/`0`, con casos negativos (`FORWARD != BACKWARD`, `NONE = 0`).
- [x] Escenario 9: `bash init.sh` en verde, 0 aserciones modificadas, lint sin warnings nuevos
      (13 preexistentes, todos fuera de `app/src`), y `app/build.gradle.kts` / `gradle/libs.versions.toml`
      / `gradle.properties` / `init.sh` / `scripts/**` / `domain/**` / `data/**` / Taxonomía /
      `seed-recipes.json` / `assets/**` intactos; `androidx.compose.animation` **no** declarado.
- [x] `SazonMotion` (sin imports) y `MotionPolicy` (solo `ui.navigation` + `ui.theme`) puros y deterministas.
- [x] Efectos secundarios (`LaunchedEffect(profile.preferences) { ensureBatch }`, `LaunchedEffect(generationId)`,
      `LaunchedEffect(query, searchId)`) y `Saver` fuera de las transiciones; crossfade 250 ms, spinner y
      pulso intactos.
- [x] `DESIGN.md` gana el bloque `motion:` + la sección `## Motion` + la excepción de capturas citando
      `screen-transitions`; **no** se crea `docs/design/concepts/screen-transitions/`.
- [x] `docs/technical-discovery.md` actualizado; `feature_list.json` en `passing` con evidencia.
- [ ] Manual (persona, emulador, tras el merge): suavidad en claro/oscuro — no automatizable (AGENTS.md).

## Disciplina de alcance

`git status` limitado a los ficheros previstos: 12 modificados (`DESIGN.md`, `PROGRESS.md`, 7 de UI,
`DesignTokensTest.kt`, `docs/technical-discovery.md`, `feature_list.json`) + nuevos
(`ui/motion/**`, `ui/theme/Motion.kt`, `ui/motion/MotionPolicyTest.kt`, `ui/theme/DesignFrontmatter.kt`,
`ui/theme/MotionTokensTest.kt`, `docs/specs/screen-transitions.md`). Build/lockfile/`init.sh`/`scripts/**`/
dominio/datos/Taxonomía/`seed-recipes.json`/assets/tokens visuales intactos; nada en `docs/design/concepts/`.

## Hallazgos

**Critical / High / Medium: ninguno.**

- **L1 (Low, informativo, no bloqueante)** — `HomeScreen` usa la propia lista observable como
  `targetState` de los dos bloques (`suggestions`, `recipes.latest().saved`) en lugar de una clave de
  "ids visibles" explícita; la identidad del `AnimatedContent` cambia con el contenido, así que el
  comportamiento observado (escenario 6) se cumple, pero es una lectura menos literal de la tabla de la
  spec.
- **L2 (Low, formal)** — Las casillas del `Validator Checklist` de `docs/specs/screen-transitions.md`
  siguen sin marcar (el planner/implementer no las marca); no afecta al contrato.
- **L3 (informativo)** — La transición de página empujada envuelve el `Scaffold` completo, así que al
  empujar el Generador/la Búsqueda la barra inferior y el FAB salen con la capa saliente (no se
  duplican). Es materia de la revisión manual en claro/oscuro, no un defecto detectable en JVM.

## Seguridad (checklist de la feature)

Sin hallazgos. Feature puramente visual: no añade red, permisos, almacenamiento, entrada/salida de datos
ni dependencias (Compose `animation` llega transitivamente vía `material3`). Sin secretos en el diff
(los `matches` de "token" son los *design tokens* de movimiento). Sin sinks, sin parsing, sin logs de
datos sensibles. Superficie de ataque nula.

## Documentación y harness

- `DESIGN.md`: bloque `motion:` (`duration 240ms`, `slide 24px`, `easing standard`, `enterOnTop true`),
  sección `## Motion` (lenguaje único, regla de dirección, capas, dónde aplica, qué no se anima,
  accesibilidad) y excepción de capturas — todo coherente con `SazonMotion`.
- `docs/technical-discovery.md`: ubicación de tokens/política/primitiva, procedencia transitiva de
  `androidx.compose.animation` y verificación JVM. Coherente con el código.
- `init.sh` sin cambios; gate real (`test` + `lint` vía `scripts/gradle.sh`), exit 0, sin procesos largos.
- `feature_list.json` (`passing` + evidencia) y `PROGRESS.md` reflejan el estado real.

## Conclusión

La feature cumple su spec: un único lenguaje de movimiento (`SazonMotion` + `MotionPolicy` +
`MotionContent`) cableado en los cuatro frentes, con tokens verificados contra `DESIGN.md` (caso negativo
no vacuo), selector de dirección cubierto con negativos, comportamiento funcional sin cambios
(0 aserciones tocadas, 98/739 preexistentes verdes + 8 nuevos), sin dependencias nuevas y documentación
durable coherente. **Veredicto: `accept`.** Queda la prueba manual de la persona en el emulador (claro y
oscuro), no automatizable. El implementer no hizo commit/push/PR y el validador tampoco.
