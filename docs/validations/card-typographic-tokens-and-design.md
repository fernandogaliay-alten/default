# Validación independiente — `card-typographic-tokens-and-design`

- Feature id: `card-typographic-tokens-and-design`
- Spec: `docs/specs/card-typographic-tokens-and-design.md`
- Rama/worktree: `feature/card-typographic-tokens-and-design`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/card-typographic-tokens-and-design`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 103)
- Veredicto: **accept**

## Alcance validado

Primera de las cinco features del ADR 0006 (`area: ui`). Solo tokens de tarjeta y su política, **sin
pantallas**:

1. **Tokens de color** en `ui/theme/Color.kt`: `SazonDarkCardSurface` `#3A2E26` y
   `SazonDarkCardOutline` `#7D6A5C`. `LightColorScheme`/`DarkColorScheme` **intactos**; `darkSurface`
   sigue `#312720`.
2. **Contrato de tarjeta** nuevo `ui/theme/CardTokens.kt`: `SazonCardColors(surface, boundary,
   boundaryWidth)`, `SazonCardTokens.light`/`dark`/`of(dark)`, `TINT_ALPHA = 0.14f` (banda documentada
   12–16 %) y `tintedSurface(surface, accent, alpha)` puro (`lerp`), más `LocalSazonCardColors`
   (`staticCompositionLocalOf`, default la variante clara).
3. **Cableado** en `ui/theme/Theme.kt`: `SazonTheme` provee `LocalSazonCardColors` según `darkTheme`.

Capturas de referencia obligatorias aportadas y revisadas:
`docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/{screen.png,screen-dark.png}`
(+ `code.html` y `DESIGN.md`).

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint; sin procesos largos, `init.sh` intacto) |
| Tests (forzado, mío) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (3m41s; 34/34 tareas) |
| Lint (forzado, mío) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (3m1s; 30/30 tareas) |
| Parseo XML propio | `app/build/test-results/testDebugUnitTest/TEST-*.xml` (frescos) | **94 suites / 704 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline en `HEAD` | `git grep -l/-o "@Test"` | **93 ficheros / 697 `@Test`** ⇒ delta **+1 suite / +7 tests** |
| Suites de tema (XML fresco) | `CardTokensTest` / `DesignTokensTest` | **5/5** y **8/8**, 0 fallos/errores/saltados |
| SARIF regenerado | `app/build/reports/lint-results-debug.sarif` | **0 errores**, 13 warnings de entorno; **0 bajo `app/src`** |
| Contraste WCAG (cálculo propio) | fórmula WCAG reimplementada | borde **3,263** ≥ 3,0; `#312720` **1,151** y relleno **1,276** (< 3,0); tinte 14 % vs fondo **1,276–1,315** (< 2,0), vs borde **2,481–2,557** (≥ 2,0) |
| Alcance | `git diff main --name-only` | 7 ficheros esperados + spec + `feature_list.json` + `PROGRESS.md`; **sin** diff en pantallas, `ui/artwork/**`, `gradle/**`, `app/build.gradle.kts`, `init.sh`, `scripts/**`, `DESIGN.md`, `docs/design/**` |
| Hex sueltos | `grep -rn "3A2E26\|7D6A5C" app/src` | solo `ui/theme` (`Color.kt` + KDoc/test); **0** consumidores fuera de `ui/theme` |
| Dependencias nuevas | diff `gradle/**` + `app/build.gradle.kts` | **ninguna** |

### Escenarios de la spec

1. **Frontmatter ↔ `Color.kt` ↔ `SazonCardTokens.dark`** sin hex en pantallas — ✓.
2. **Contraste ≥ 3:1 (WCAG 1.4.11)** sobre `darkBackground` y fallo real si baja — ✓ (`assertTrue(ratio
   >= 3.0)` + `assertEquals(3.26, ±0.05)`; el valor límite es el **borde**).
3. **No vacuo**: `darkSurface` `#312720` = 1,151 y relleno `#3A2E26` = 1,276, ambos < 3,0, con
   `surface` intacto en frontmatter y `DarkColorScheme` — ✓.
4. **Tinte cálido** de los 4 roles de acento oscuro, alfa en banda, determinismo — ✓ (relleno vs fondo
   < 2,0; relleno vs borde ≥ 2,0; vectores del prototipo C3 con ±1 por canal).
5. **Claro intacto** (`#FFFFFF`, borde transparente + 0 dp) — ✓.

Las capturas se reproducen: en oscuro la tarjeta tiene superficie + borde visibles, **sin sombra** y con
tinte cálido sutil; en claro es blanca y sin borde.

## Hallazgos

Sin hallazgos **Critical / High / Medium**.

### Finding 1 (Low, no bloqueante) — KDoc dice "sRGB" pero `lerp` interpola en Oklab

`app/src/main/java/com/sazon/app/ui/theme/CardTokens.kt:63-68` documenta `tintedSurface` como
"por componente en sRGB"; el bytecode real de `androidx.compose.ui:ui-graphics-android:1.10.4`
(`ColorKt.lerp-jxsXWHM`) interpola en **Oklab**. El comportamiento **sí** cumple la spec (el Technical
Approach admite `lerp` si se cumplen los vectores, y se cumplen) y las cotas duras del test están
aseveradas. Corrección diferida a la feature consumidora `recipe-card-typographic` (evita re-validar
por un comentario).

### Info (Low) — rangos de ratio de la evidencia

Los rangos "1,28–1,31" / "2,49–2,56" de `PROGRESS.md`/`feature_list.json` son orientativos en **sRGB**
(la evidencia los etiqueta como tales); con Oklab real son 1,276–1,315 y 2,481–2,557. Las cotas duras
(`< 2,0` / `≥ 2,0`) sí están en el test.

### Nota preexistente (no imputable)

`DESIGN.md` (Shapes) dice "nunca bordes duros para separar tarjetas", mientras la sección de
`Components` (recipe-card) y el ADR 0006 fijan `darkCardOutline` en oscuro. La spec prohíbe tocar
`DESIGN.md` aquí; reconciliar cuando `recipe-card-typographic` aplique los tokens.

## Seguridad

Sin hallazgos. El diff son constantes de tema y tests JVM: sin secretos, red, I/O de usuario,
autenticación, superficies de datos ni llamadas externas. `CompositionLocalProvider` con default seguro
(`SazonCardTokens.light`). Sin dependencias nuevas.

## Documentación y harness

- `feature_list.json`: JSON válido (56 features); el implementer registró `passing` + evidencia.
- `PROGRESS.md`: sesión 103 con comandos, conteos, ratios, greps y siguiente paso; conteos
  reproducidos por el validador.
- `docs/technical-discovery.md` y `docs/adr/0006` actualizados; `DESIGN.md` intacto (como pedía la spec).
- `init.sh` sin cambios y ejecuta el gate real; worktree reiniciable por el camino estándar.

## Cierre

El **orquestador** de `feature-flow` persistió `accepted` en `feature_list.json` y creó el **commit
local** `feat: complete card-typographic-tokens-and-design` en
`feature/card-typographic-tokens-and-design`. **Sin** push ni PR (los hace `github-flow`).
