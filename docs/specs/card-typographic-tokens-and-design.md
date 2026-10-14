# Feature Implementation Spec: Tokens de la tarjeta tipográfica y DESIGN.md como fuente de verdad

## Source Feature

- `id`: `card-typographic-tokens-and-design`
- `area`: `ui`
- `depends_on`: `["design-tokens-and-theme"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Referencia de diseño: `docs/adr/0006-typographic-recipe-cards.md` (decisión 5) y `DESIGN.md`.

## Goal

Cerrar el **defecto de contraste del modo oscuro** que corrige el ADR 0006: en oscuro la tarjeta se
dibujaba con `darkSurface` `#312720` sobre `darkBackground` `#241C16` = **1,15:1**, por debajo del
**3:1** que WCAG 1.4.11 pide para distinguir un límite de interfaz. La feature entrega los **tokens de
tarjeta** declarados por `DESIGN.md` como constantes de `ui/theme` accesibles por rol (sin hex sueltos
en pantallas), el **tinte cálido de la cocina** como política pura a nivel de tarjeta, y la **prueba
JVM** que falla si el límite de la tarjeta baja de 3:1. Es la **base** de las otras cuatro features del
ADR 0006: aquí nacen los tokens; ninguna pantalla los aplica todavía.

## Non-Goals

- **No** se cambia ninguna pantalla ni componente (`RecipeCard`, `RecipeListCard`, rejilla de Favoritos,
  Detalle, Home): el layout tipográfico es `recipe-card-typographic` y `recipe-detail-typographic-header`;
  la imagen como capa de fondo es `dish-artwork-as-card-background`; la retirada de fotos es
  `retire-recipe-photos`.
- **No** se modifica `darkSurface` `#312720` ni ningún rol de `DarkColorScheme` (`surface`,
  `surfaceVariant`, `outline`, `outlineVariant`): los tokens nuevos son **de tarjeta** y no alteran
  navegador, hojas ni el resto de pantallas.
- **No** se toca `ui/artwork` (el mapeo `RecipeAccent` → rol del tema sigue como está, privado en
  `RecipeArtwork.kt`); esta feature no consume ni expone `RecipeAccent`.
- **No** hay assets, iconos, fuentes, dependencias ni cambios en `gradle/libs.versions.toml`,
  `app/build.gradle.kts`, `init.sh` ni `scripts/**`.
- **No** se añaden capturas nuevas: las de `tarjeta-oscura/` ya están aportadas y son la dirección
  visual; tampoco se crea carpeta fuera de `docs/design/concepts/` existente.

## Job Story

Cuando la app entra en modo oscuro,
quiero que cada tarjeta se distinga del fondo (superficie y borde propios, ≥ 3:1) y que la cocina tiña
la tarjeta de forma sutil,
para que las fichas tipográficas del ADR 0006 se lean como tarjetas y no como texto flotando en el
fondo.

## Users And Permissions

- **Desarrollador / agente**: declara los tokens, ejecuta los tests JVM y el gate. Sin permisos.
- **Usuario local**: en esta feature **no ve ningún cambio** (ninguna pantalla consume los tokens aún);
  percibirá el efecto cuando llegue `recipe-card-typographic`.

## Acceptance Scenarios

### Scenario 1: `DESIGN.md` declara los tokens de tarjeta y el tema los expone

Given el frontmatter de `DESIGN.md` con `colors.darkCardSurface: "#3A2E26"` y
`colors.darkCardOutline: "#7D6A5C"` (ya declarados),
When el test JVM compara el frontmatter con `SazonDarkCardSurface` / `SazonDarkCardOutline` de
`Color.kt` y con `SazonCardTokens.of(dark = true)`,
Then los hex coinciden exactamente y ninguna pantalla declara esos hex (solo viven en `ui/theme`).

### Scenario 2: el límite de la tarjeta cumple el 3:1 de WCAG 1.4.11

Given `darkCardOutline` `#7D6A5C` y `darkBackground` `#241C16`,
When el test calcula el **ratio de contraste WCAG** (luminancia relativa sRGB: `(L_claro+0.05)/(L_oscuro+0.05)`),
Then el ratio es **≈ 3,26:1 ≥ 3,0** y el test **falla** si el token deriva por debajo de 3:1.
Nota vinculante: el límite visual de la tarjeta en oscuro es el **borde** (`darkCardOutline`), no el
relleno (ver Repository Research: el relleno solo da 1,28:1).

### Scenario 3: `darkSurface` no cambia y el test no pasa en vacío

Given `DarkColorScheme` y el frontmatter,
When el test inspecciona `surface` y la métrica de contraste,
Then `surface` sigue siendo `#312720` (igual en frontmatter y en tema), el ratio del **antiguo**
`#312720` sobre `darkBackground` es **≈ 1,15:1 < 3,0** y el ratio del **relleno** nuevo `#3A2E26` es
**≈ 1,28:1 < 3,0**; ambas aserciones documentan el defecto y demuestran que la métrica no es trivial.

### Scenario 4: el tinte cálido de la cocina es sutil y no se come el borde

Given los tokens oscuros de tarjeta y los **cuatro roles de acento oscuro** que usan las 11
`RecipeAccent` (`primaryContainer` `#6E2C15`, `secondaryContainer` `#2F3D1E`, `tertiaryContainer`
`#4A3413`, `surfaceVariant` `#3D3129`),
When se compone el relleno teñido `tintedSurface(darkCardSurface, accent)` con el alfa del token,
Then para todos ellos: el relleno teñido sigue por debajo del umbral (`contraste vs darkBackground
< 2,0`; medido 1,28–1,32), el borde sigue siendo el límite (`contraste relleno↔borde ≥ 2,0`; medido
2,48–2,56), el alfa está dentro de la banda documentada **12–16 %** y la función es determinista
(dos llamadas iguales devuelven el mismo color).

### Scenario 5: el modo claro queda intacto

Given `darkTheme = false`,
When se resuelven los tokens de tarjeta,
Then el relleno es `surface` `#FFFFFF`, no hay borde (transparente y 0 dp) y el esquema claro no cambia
respecto a `DesignTokensTest` (que sigue verde).

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `card-typographic-tokens-and-design`, líneas 1229-1243) — alcance,
  verificación y notas (decisión B+C).
- `docs/adr/0006-typographic-recipe-cards.md` — decisión 5: `darkCardSurface` `#3A2E26` +
  `darkCardOutline` `#7D6A5C` = 3,26:1, **tokens de tarjeta**, no un cambio de `darkSurface`.
- `DESIGN.md` — frontmatter líneas 19-22 (`darkBackground`, `darkSurface`, **`darkCardSurface` y
  `darkCardOutline` ya declarados**), `components.recipe-card` (69-73: `backgroundDark`/`borderDark`),
  tabla de color (265-266), `Components` (295-302: "En oscuro: superficie `darkCardSurface` + borde
  `darkCardOutline` (3,26:1) + tinte cálido de la cocina") y `Accessibility Baseline` (465-467).
- `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/{README.md, DESIGN.md,
  screen.png, screen-dark.png, code.html}` — capturas **aportadas** (2026-10-14).
- `docs/design/concepts/typographic-cards/prototipo-c3-imagenes.html` — "Opción C · tinte cálido por
  cocina" (163-185): degradado **de la cocina al 12–16 %** sobre `#3A2E26` (`→ #4A2A22` rosa/latina,
  `→ #453215` curry/india), "la luminancia no cambia".
- `app/src/main/java/com/sazon/app/ui/theme/{Color,Theme,Spacing,Shape}.kt` — paleta y esquemas;
  `SazonSpacing`/`SazonComponents` (patrón de objeto de tokens); `SazonTheme` con `darkTheme`.
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` — test JVM frontmatter ↔ roles con caso
  negativo no vacío; helpers `frontmatter*` **privados de fichero** (no reutilizables desde otro fichero).
- `app/build.gradle.kts` (130-134) — `design.tokens.path` ya se inyecta al test JVM.
- `app/src/main/java/com/sazon/app/ui/artwork/{RecipeArtworkStyle,RecipeArtwork}.kt` — los 11
  `RecipeAccent` y su resolución (privada) a los cuatro roles de acento del tema.
- `docs/technical-discovery.md` (460-477) — el Tema y los dos tokens ya están descritos.

### Existing Patterns To Follow

- `DESIGN.md` es la **única fuente de verdad**: el hex se declara ahí y `Color.kt` lo materializa;
  ninguna pantalla declara hex (`Color.kt` línea 7).
- Los tests de tokens son **JVM puro** (sin emulador): `Color`, `dp` y `RoundedCornerShape` funcionan,
  y el frontmatter se lee por `System.getProperty("design.tokens.path")`.
- El **caso negativo no vacío** (patrón `DesignTokensTest` y `TaxonomyIntegrityTest`): un test debe
  probar que la métrica detecta el defecto, no pasar en vacío.
- Los objetos de tokens (`SazonSpacing`, `SazonComponents`) y los proveedores por `CompositionLocal`
  (`LocalRecipePhotoSource`, `LocalGeneratedRecipeArtworkSource`) son el precedente para exponer algo
  que `MaterialTheme.colorScheme` no cubre.

### Current Gaps

- `SazonDarkCardSurface` / `SazonDarkCardOutline` **no existen** en `Color.kt` (grep `3A2E26|7D6A5C` en
  `app/src/main/java/com/sazon/app/ui` → **0** coincidencias); no hay forma de que una pantalla los
  consuma sin hex suelto.
- No existe política de tinte cálido ni test de contraste (no hay helper de ratio WCAG en el repo).
- **Hallazgo de cálculo (crítico para no escribir un test que falle):** con la fórmula WCAG,
  `darkCardOutline` `#7D6A5C` vs `darkBackground` = **3,263:1** (el 3,26 del ADR), pero
  `darkCardSurface` `#3A2E26` vs `darkBackground` = **1,276:1** y `darkSurface` `#312720` =
  **1,151:1**. Es decir: **el 3:1 lo da el borde**, no el relleno; un test que exija ≥ 3:1 al *relleno*
  fallaría. El relleno + borde son el cromo completo de la tarjeta.

## Technical Approach

1. **Tokens de color** en `Color.kt`, dentro del bloque "Paleta de DESIGN.md" y sin tocar los esquemas:
   `SazonDarkCardSurface = Color(0xFF3A2E26)` y `SazonDarkCardOutline = Color(0xFF7D6A5C)`.
2. **Contrato de tarjeta** en un fichero nuevo `ui/theme/CardTokens.kt` (nombres propuestos; si se
   renombran, mantener el contrato y citarlo en la evidencia):
   - `data class SazonCardColors(val surface: Color, val boundary: Color, val boundaryWidth: Dp)`.
   - `object SazonCardTokens`: `light` (`surface = SazonSurface`, `boundary = Color.Transparent`,
     `boundaryWidth = 0.dp`), `dark` (`surface = SazonDarkCardSurface`,
     `boundary = SazonDarkCardOutline`, `boundaryWidth = 1.dp`), `fun of(dark: Boolean)`,
     `const val TINT_ALPHA` (valor único dentro de **0.12–0.16**, p. ej. `0.14f`) y
     `fun tintedSurface(surface: Color, accent: Color, alpha: Float = TINT_ALPHA): Color` — composición
     **pura** por componente en sRGB del acento sobre la superficie (vale `androidx.compose.ui.graphics.lerp`
     o la interpolación equivalente, siempre que se cumplan los vectores del plan de verificación).
   - `val LocalSazonCardColors = staticCompositionLocalOf { SazonCardTokens.light }`.
   No se implementa ninguna sombra ni geometría nueva: el borde sustituye a la sombra como elevación.
3. **Cableado** en `Theme.kt`: `SazonTheme` provee
   `LocalSazonCardColors provides SazonCardTokens.of(darkTheme)` envolviendo el `content` de
   `MaterialTheme`. Sin cambios en los esquemas ni en `isSystemInDarkTheme`.
4. **Tests JVM** (sin emulador, sin Robolectric):
   - `DesignTokensTest` (modificado, **sin** tocar `mismatches()`): dos `@Test` nuevos —
     frontmatter ↔ `Color.kt` de los dos tokens de tarjeta, y `darkSurface` intacto (`#312720` en
     frontmatter y en `DarkColorScheme.surface`) con los tokens de tarjeta distintos de él.
   - `CardTokensTest` (nuevo, mismo paquete): helpers privados `relativeLuminance(hex)` y
     `contrastRatio(a, b)` y los escenarios 2-5, con los **valores medidos** como vectores orientativos
     (tolerancia ±0,05 en el ratio; ±1 por componente si se comparan hex).
5. **Sin consumidores**: los tokens quedan publicados para `recipe-card-typographic`. Un `CompositionLocal`
   sin consumidor y una función pura sin llamadas **son deliberados** (base de las otras features, igual
   que `domain.image` en su día): no añadir usos inventados en pantallas para "justificarlos".

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/theme/Color.kt` — modify; añadir `SazonDarkCardSurface` y
  `SazonDarkCardOutline` al bloque de paleta; **no** tocar `LightColorScheme`/`DarkColorScheme`.
- `app/src/main/java/com/sazon/app/ui/theme/CardTokens.kt` — create; contrato de tarjeta y tinte.
- `app/src/main/java/com/sazon/app/ui/theme/Theme.kt` — modify; proveer `LocalSazonCardColors`.
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` — modify; 2 `@Test` nuevos.
- `app/src/test/java/com/sazon/app/ui/theme/CardTokensTest.kt` — create; contraste WCAG y tinte.
- `DESIGN.md` — not needed (ya declara los tokens y el tinte; ver Durable Documentation Impact).
- `docs/adr/0006-typographic-recipe-cards.md` — modify; marcar `card-typographic-tokens-and-design`
  como implementada en el apartado `Estado` (las otras cuatro siguen pendientes).
- `docs/technical-discovery.md` — modify; Tema (contrato de tarjeta) y Testing (`CardTokensTest`).
- `feature_list.json`, `PROGRESS.md` — los actualiza el orquestador al cerrar, no el implementer.

## Visual Design Impact

- UI involved: **yes** (`area: ui`; la feature existe por un defecto visual de contraste).
- Design source: `DESIGN.md` (frontmatter 19-22, tabla de color 265-266, `Components` 295-302,
  `Accessibility Baseline` 465-467) + `docs/adr/0006-typographic-recipe-cards.md` (decisión 5).
- Screens or states affected: **ninguna pantalla en esta feature**. El estado afectado es el *token* que
  las tarjetas de oscuro usarán (Inicio, Resultados, Favoritos, Detalle) y que se revisará visualmente
  cuando lo apliquen `recipe-card-typographic` y `recipe-detail-typographic-header`.
- New design artifact required: **no** (capturas ya aportadas, 2026-10-14).
- **UI captures (required, ya existentes)**: la dirección visual obligatoria es
  `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/screen-dark.png` (oscuro; la
  captura que justifica la feature) y `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/screen.png`
  (claro, el par de comparación), con `code.html` y `DESIGN.md` en la misma carpeta. El implementer
  **debe** revisar que el contrato de tokens reproduce lo que muestran antes de darlo por bueno
  (superficie `#3A2E26`, borde `#7D6A5C`, tinte cálido de la cocina, borde en lugar de sombra en oscuro).
- Feature-specific visual states: ninguno nuevo (no hay estados vacío/carga/error en esta feature).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — el párrafo del Tema (460-477) gana el contrato de tarjeta
  (`SazonCardColors`/`SazonCardTokens`/`LocalSazonCardColors` y el tinte) y el de Testing nombra
  `CardTokensTest` y el `DesignTokensTest` ampliado. Cambia un límite durable del tema.
- `docs/domain-model.md`: **not needed** — no hay entidades, relaciones ni estados; son tokens visuales.
- `AGENTS.md`: **not needed** — no cambian el flujo de agentes, el arranque ni las reglas del repo.
- `DESIGN.md`: **not needed** — ya declara `darkCardSurface`/`darkCardOutline` en el frontmatter, el
  tinte y el 3,26:1; duplicar los tokens o añadir claves nuevas solo crea deriva (el test parsea el
  frontmatter). Si el implementer necesita una línea para el alfa del tinte, la añade en la prosa de
  `Components` sin tocar el frontmatter.
- `docs/adr/0006-typographic-recipe-cards.md`: **update** — `Estado` refleja que esta feature (la
  primera de las cinco) está implementada; las otras cuatro siguen pendientes.

## Implementation Plan

1. Declarar `SazonDarkCardSurface` y `SazonDarkCardOutline` en `Color.kt` (bloque de paleta de
   `DESIGN.md`), sin tocar los esquemas.
2. Crear `ui/theme/CardTokens.kt` con `SazonCardColors`, `SazonCardTokens` (`light`, `dark`, `of`,
   `TINT_ALPHA`, `tintedSurface`) y `LocalSazonCardColors`.
3. Proveer `LocalSazonCardColors` en `SazonTheme` según `darkTheme`.
4. Ampliar `DesignTokensTest` (frontmatter ↔ `Color.kt` + `darkSurface` intacto).
5. Crear `CardTokensTest` con el ratio WCAG (escenarios 2-3) y el tinte (escenario 4) y el claro (5).
6. Ejecutar `bash init.sh`, parsear los XML y el SARIF, y verificar la disciplina de alcance por grep.
7. Actualizar `docs/technical-discovery.md` y `docs/adr/0006`, y registrar la evidencia.

## Implementation Tasks

- [ ] Añadir los dos tokens a `Color.kt` con KDoc que cite `DESIGN.md`/ADR 0006 y su ámbito (tarjeta).
- [ ] Crear `CardTokens.kt` con el contrato completo y el tinte puro (alfa en 12–16 %).
- [ ] Proveer `LocalSazonCardColors` en `Theme.kt`.
- [ ] Añadir los 2 `@Test` de frontmatter/`darkSurface` a `DesignTokensTest`.
- [ ] Crear `CardTokensTest` con `relativeLuminance`/`contrastRatio` y los escenarios 2-5.
- [ ] Verificar `bash init.sh` verde y registrar la tirada focal y el delta de tests.
- [ ] Comprobar por grep que no hay hex de tarjeta fuera de `ui/theme` y que no hay dependencias nuevas.
- [ ] Actualizar `docs/technical-discovery.md` y `docs/adr/0006`.

## Verification Plan

- `bash init.sh` → **exit 0**: ejecuta el gate no bloqueante actual (`./gradlew test` + `./gradlew lint`
  vía `scripts/gradle.sh`); **no** arranca procesos largos (servidores, watchers, emulador) ni cambia
  `init.sh`.
- Tirada focal (sin esperar al cache):
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.theme.*" --rerun-tasks` →
  `BUILD SUCCESSFUL`.
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: `DesignTokensTest` (6 → 8
  tests) y `CardTokensTest` (nueva, ~5-7 tests) en verde; **0 fallos, 0 errores, 0 saltados**; re-derivar
  la baseline en `HEAD` (nº de ficheros con `@Test` y de tests) y registrar el delta (no fiarse de los
  conteos citados en `PROGRESS.md` sesión 100).
- Lint (SARIF `app/build/reports/lint-results-debug.sarif`): **0 errores** y ningún hallazgo nuevo bajo
  `app/src` (los 13 warnings preexistentes de entorno no cuentan).
- Vectores de contraste a re-comprobar en el test (orientativos, tolerancia ±0,05):
  `darkCardOutline` vs `darkBackground` **3,26**; `darkSurface` `#312720` vs `darkBackground` **1,15**;
  `darkCardSurface` vs `darkBackground` **1,28**; relleno teñido vs fondo **1,28–1,32**; relleno teñido
  vs borde **2,48–2,56**.
- Disciplina de alcance por inspección:
  `grep -rn "3A2E26\|7D6A5C" app/src` solo aparece en `ui/theme` (`Color.kt`, y `CardTokens.kt` si los
  cita); `grep -rn "0xFF" app/src/main/java/com/sazon/app` sin hex nuevos fuera de `ui/theme`;
  `git diff --name-only` = la lista de Expected File Changes (más spec, `feature_list.json`,
  `PROGRESS.md` y los docs durables); **sin** diff en `ui/artwork/**`, pantallas, `app/build.gradle.kts`,
  `gradle/**`, `init.sh`, `scripts/**`, `DESIGN.md`, `docs/design/**`; `test -d
  docs/design/concepts/card-typographic-tokens-and-design` → **existe** (con las capturas, sin cambios).
- **E2E / UI automatizada: no aplica** — `AGENTS.md` no automatiza UI de emulador y esta feature no
  cambia ninguna pantalla; se verifica por lógica/estado en JVM (tokens y contraste) y contra las
  capturas obligatorias. La revisión puramente visual llega con las features que apliquen los tokens.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal; conteos de suites/tests con el delta respecto
  a `HEAD`.
- Nombres y aserciones de los tests nuevos, con el hex de cada token y los **ratios medidos** de los
  escenarios 2, 3 y 4 (incluidos los cuatro roles de acento).
- Valor de `TINT_ALPHA` y dos vectores de tinte (p. ej. `primaryContainer` 12 % → `#402E24`,
  `secondaryContainer` 16 % → `#383025`).
- Resultado del grep de hex y de `git diff --name-only`; resultado de lint.
- Confirmación de que las capturas `tarjeta-oscura/{screen.png,screen-dark.png}` existen y se revisaron.
- Registro en `docs/technical-discovery.md` y `docs/adr/0006`.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (ninguna pantalla, `ui/artwork`, gradle ni
      `init.sh` tocados; `darkSurface` y los esquemas intactos).
- [ ] Acceptance scenarios pass (1-5), incluida la no-vacuidad (los ratios 1,15 y 1,28 como defecto).
- [ ] El test de contraste mide el **límite** de la tarjeta (`darkCardOutline` vs `darkBackground`)
      ≈ 3,26 ≥ 3,0 y falla si baja de 3,0 — no se exige el 3:1 al relleno.
- [ ] `DesignTokensTest` sigue verde y el frontmatter de `DESIGN.md` no se duplica ni se reordena.
- [ ] Verification evidence is present (comandos, conteos, ratios, greps).
- [ ] Estado de `init.sh` y `feature_list.json`/`PROGRESS.md`: actualizados por el orquestador, no por
      el implementer.
- [ ] Persistent E2E coverage: no aplica (justificado en el plan de verificación).
- [ ] For UI features, `Visual Design Impact` names captures under
      `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/` that exist
      (`screen.png`, `screen-dark.png`) y el contrato de tokens coincide con ellas.
- [ ] No unrelated product behavior or extra feature work was added.
