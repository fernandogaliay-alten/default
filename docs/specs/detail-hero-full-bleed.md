# Feature Implementation Spec: Cabecera del Detalle a todo el ancho

## Source Feature

- `id`: `detail-hero-full-bleed`
- `area`: `ui`
- `depends_on`: `["recipe-detail-typographic-header", "dish-artwork-as-card-background"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner **no**
  modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (entrada `detail-hero-full-bleed`; notas con la correctiva y la exención de
  capturas de la issue #132).
- **Correctiva de diseño pedida por la persona** (2026-10-14, sesión 113): el hero (nombre + ingredientes
  principales) y la tira de metadatos quedaron **embutidos por el padding lateral de 16 dp** que añadió la
  capa Compose; el diseño aceptado los quiere **de borde a borde**.
- **Autoridad vigente, no se reabre**: `DESIGN.md` → `Shapes` (líneas 327-332: "El hero y la tira de
  metadatos van **de borde a borde** (sin padding lateral de 16dp) desde la revisión de diseño del
  2026-10-14 (`detail-hero-full-bleed`)") y `Core Screens` 5. Las capturas aceptadas son **previas** a esa
  revisión: se renderizaron el 2026-10-14 (`7f1e113`) **antes** de que `DESIGN.md` se actualizara en
  `45203e2`, y por eso todavía muestran el hero y la tira **embutidos** por 16 px por lado
  (`code.html` envuelve `.herowrap` en `<div style="padding:0 16px">`; el PNG aceptado ocupa x = 16 →
  373,5 px de un marco de 390 px). El margen de 16 dp es una **desviación de la implementación** respecto a
  `DESIGN.md`, que es la fuente de verdad; las capturas quedan como material de dirección visual histórico
  y **no** como prueba del borde a borde.

## Goal

Llevar el **hero** y la **tira de metadatos** del Detalle **de borde a borde** (sin
`padding(horizontal = SazonSpacing.md)`), sin tocar su geometría interna (alto **240 dp**, filete **6 dp**,
esquinas inferiores de **24 dp**, marca de agua de **190 dp** al 12 % y su desbordamiento), su contenido
(categoría · cocina, título, tres ingredientes con cantidad, los cuatro pares de metadatos), la tipografía,
las pestañas, el cuerpo, el escalador de comensales, la barra inferior ni el estado.

La corrección incluye **revisar y confirmar** tres puntos que el margen ocultaba: (a) el **recorte de las
esquinas inferiores de 24 dp** con el filete dentro del `clip` y la marca de agua desbordando por la esquina
inferior derecha; (b) la **posición de la píldora "Atrás" y del corazón flotantes**, que pasan a quedar
íntegramente sobre el hero (antes colgaban sobre el fondo crema por el borde izquierdo); (c) que la tira
sigue **alineada** con el hero en las dos esquinas inferiores de 24 dp.

## Non-Goals

- **No** se tocan los insets **verticales**: el hero sigue empezando bajo la barra de estado (el `Column`
  conserva `.padding(padding)` de `Scaffold`). "Borde a borde" aquí es **solo horizontal**; no se cambian
  `enableEdgeToEdge`, la barra de estado ni `contentWindowInsets`.
- **No** cambia `RecipeDetailHeroFormat` (240/6/190/0,12f/3, `HeroContent`, `of()`) ni su test; tampoco
  `HERO_PADDING = 18.dp`, el padding interno de la tira (`vertical = sm`, `horizontal = 4.dp`), los offsets
  de la marca de agua, el filete, `HeroTypography` ni `RecipeDetailFormat.metaEntries` (sus cuatro valores,
  incluidos `"480 kcal"` y el `"—"` sin nutrición, siguen siendo contrato).
- **No** cambian los controles flotantes: píldora "Atrás" (`Surface` + `TextButton`) y corazón (`Surface`
  circular de 48 dp) conservan color `surface`, tamaño, comportamiento y `SazonSpacing.md` del borde; solo
  se **verifica** que siguen legibles y pulsables. **No** se adoptan los 34 px/14 px del prototipo
  (dirección visual; `Accessibility Baseline` exige ≥48 dp).
- **No** cambian pestañas, cuerpo, `ServingsStepper`, caption de cantidades, barra inferior,
  `RecipeDetailState`/`Saver`, el overlay de `MainShell` ni el reparto de `imageBackground`.
- **No** se toca `ui/artwork/**`, `ui/theme/**`, `ui/**` (resto), `domain/**`, `data/**`, la Taxonomía,
  `assets/**`; sin dependencias, tokens, hex, iconos ni fuentes nuevos.
- **No** se generan capturas: **exención autorizada** (issue #132, misma clase que
  #77/#85/#86/#91/#98/#101/#110/#129) y **`docs/design/concepts/detail-hero-full-bleed/` NO debe existir**.
- **No** se rediseña nada más de la pantalla (ni la separación hero → pestañas ni el `margin-bottom` del
  prototipo): un defecto visual posterior se corrige con una feature correctiva.

## Job Story

Cuando abro el Detalle de una receta,
quiero que la cabecera (nombre e ingredientes principales) y la tira de metadatos lleguen hasta los bordes
de la pantalla, como en el diseño aprobado,
para que el bloque superior se sienta como la portada de la receta y no como una tarjeta encogida por un
margen lateral que nadie pidió.

## Users And Permissions

- **Usuario local**: abre el Detalle, ajusta comensales, cambia de pestaña, guarda y vuelve con "Atrás". Sin
  permisos, sin red y sin nada nuevo que aprender.
- **Desarrollador / agente**: verifica en **JVM** que la geometría y el contenido publicados no cambian y,
  por inspección, que el padding lateral desaparece del hero y de la tira; el acabado lo revisa la persona
  en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: idéntico a hoy; el ajuste de imágenes solo cambia la **capa de fondo** del hero.

## Acceptance Scenarios

### Scenario 1: el hero llega de borde a borde

Given el Detalle abierto con el ajuste de imágenes **apagado**,
When se pinta el hero,
Then el degradado de la cocina, la marca de agua, la categoría, el título, las filas de ingredientes y el
filete de 6 dp ocupan **todo el ancho** (sin el margen lateral de 16 dp), el hero mide **240 dp**, sus
**esquinas inferiores** son de **24 dp** y las superiores rectas, y la marca de agua (190 dp al 12 %) sigue
desbordando por la esquina inferior derecha **recortada por el `clip`**; con `darkTheme = true` el resultado
es el mismo con los roles oscuros.

### Scenario 2: la tira de metadatos va a sangre y sigue alineada con el hero

Given el Detalle abierto,
When se recorre la parte superior,
Then la tira (`Surface`, `color = surface`, `shadowElevation = 4.dp`, esquinas **inferiores** de 24 dp)
ocupa **todo el ancho**, empieza **inmediatamente** debajo del hero sin solape ni `offset`, y sus esquinas
inferiores de 24 dp **coinciden en x** con las del hero; muestra los cuatro pares de
`RecipeDetailFormat.metaEntries` en columnas iguales con hairline vertical (no tras la última), etiqueta en
versalitas y valor en Manrope tabular, con los **mismos valores** de hoy.

### Scenario 3: recorte de las esquinas inferiores de 24 revisado

Given el Detalle abierto (con y sin imagen),
When se mira la unión hero → tira,
Then el filete de 6 dp queda **dentro** del `clip` y se redondea con la esquina inferior del hero; el hueco
de las esquinas redondeadas del hero deja ver el **fondo de pantalla** (crema en claro, `darkBackground` en
oscuro) sobre el borde superior recto de la tira —lo que ya muestra la captura aceptada—; no aparecen bordes
duros, sombras marcadas ni marca de agua cortada fuera del hero.

### Scenario 4: "Atrás" y corazón siguen legibles y pulsables (verificación exigida)

Given el Detalle abierto con el ajuste encendido y apagado,
When el usuario mira y pulsa los controles flotantes,
Then la píldora "Atrás" y el corazón siguen **flotando encima del hero** a `SazonSpacing.md` (16 dp) de los
bordes, con el relleno `surface`/`primary` de hoy (legibles sobre el degradado de la cocina y sobre el
`scrim` de la imagen), con objetivo táctil ≥48 dp (corazón `Surface` de 48 dp; `TextButton` con el mínimo
interactivo de Material3), y su comportamiento no cambia ("Atrás" cierra el Detalle; el corazón
guarda/quita de Favoritos).

### Scenario 5: el ajuste de imágenes funciona igual (encendido y apagado)

Given `imageBackground = true` y el resolver devolviendo imagen,
When se pinta el hero,
Then la imagen (`ContentScale.Crop`) y el `scrim` cubren **todo el ancho**, recortados por el mismo `clip`
de 24 dp, la tipografía usa el tratamiento **sobre imagen** y la marca de agua **no** se pinta; la geometría
(240 dp) y la tira no se mueven (sin salto de layout) y el fundido sigue en
`DishImageArtworkPolicy.CROSSFADE_MILLIS`. Con `imageBackground = false` o sin imagen (fallo, timeout,
cancelación o tope agotado) el hero es el **tipográfico** de hoy, sin microcopy ni error.

### Scenario 6: nada más del Detalle se mueve

Given la implementación terminada,
When se inspecciona el diff y se ejecuta el gate,
Then `RecipeDetailHeroFormat` y su test están **sin cambios** (240/6/190/0,12f/3 y el contenido), la tira
conserva sus cuatro valores (`RecipeDetailFormat` sin tocar y `RecipeDetailFormatTest` verde), y pestañas,
checklist, `ServingsStepper`, caption, barra inferior, `RecipeDetailState`/`Saver` y el overlay de
`MainShell` quedan **byte a byte** iguales.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `detail-hero-full-bleed`, líneas 1410-1422) — `area: ui`, dependencias
  satisfechas, verificación textual (padding fuera + geometría y contenido intactos + ajuste on/off y
  controles + gate verde) y notas con la exención de capturas (#132).
- `PROGRESS.md` (sesiones 115-116) — baseline en `HEAD` `804f009`: **97 suites / 733 tests**,
  `git grep -c '@Test'` = 97 ficheros / 733 anotaciones, lint 0 errores / 13 warnings preexistentes.
- `DESIGN.md` — `Shapes` (hero 240 con esquinas inferiores 24 + tira con las mismas esquinas y sombra suave;
  **de borde a borde** desde `detail-hero-full-bleed`), `Core Screens` 5, `Components` → Imagen de receta
  (L2 + `scrim`) y `Accessibility Baseline` (≥48 dp).
- `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/{screen.png,screen-dark.png,code.html,DESIGN.md}`
  y `docs/design/concepts/dish-artwork-as-card-background/detalle-con-imagen/{screen.png,screen-dark.png,code.html,DESIGN.md}`
  — capturas aceptadas (anteriores a la revisión `detail-hero-full-bleed`: aún **con inset de 16 px** por
  lado en `code.html`/PNG); dirección visual histórica, no prueba del borde a borde.
- `docs/design/concepts/typographic-cards/{prototipo-c3.html,prototipo-c3-imagenes.html}` — origen de los
  valores (hero 240, padding interno 18, marca de agua, tira a sangre).
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — el padding a retirar: **línea 215**
  (hero: `.fillMaxWidth().padding(horizontal = SazonSpacing.md).clip(HeroBottomShape)...`) y **línea 399**
  (tira: `.fillMaxWidth().padding(horizontal = SazonSpacing.md)`); `HeroBottomShape` (línea 80) compartido
  por hero (216) y tira (400); `Row` flotante "Atrás"/corazón (235-262); KDoc del encabezado, del hero y de
  la tira.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailHeroFormat.kt` y
  `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailHeroFormatTest.kt` — geometría/contenido puros y su
  aserción de 240/6/190/0,12f/3: son el contrato de "no cambia"; **sin diff**.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — la capa de fondo se pinta con
  `Modifier.matchParentSize()` **dentro** del `Box` recortado del hero: ya hereda el `clip` de 24 dp y el
  ancho completo, sin cambios.
- `app/src/main/java/com/sazon/app/ui/theme/CardImageOverlay.kt` — `scrimBrush` es un degradado **vertical**
  de cobertura completa: ensanchar el hero no altera sus paradas.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` (280-291) — el Detalle se monta como overlay con
  `Modifier.fillMaxSize()` y recibe `imageBackground`: no hay padding intermedio que reintroduzca el margen.
- `docs/specs/{recipe-detail-typographic-header,dish-artwork-as-card-background}.md` — los contratos
  aceptados que esta correctiva **no** reabre.
- `app/build.gradle.kts` — `androidx.compose.ui.test.junit4` solo en `androidTestImplementation`: no hay
  tests de composición disponibles en `test` (ver decisiones).

### Existing Patterns To Follow

- **Capa Compose delgada sobre lógica pura**: los `@Composable` pintan; geometría y contenido publicados
  viven en `RecipeDetailHeroFormat` y se testean en JVM.
- **Roles del tema y cero hex** fuera de `ui/theme`; **cifras tabulares** en cantidades y metadatos.
- **Sin constantes muertas**: el repo retiró `SazonComponents.recipeCardImageRatio` al quedarse sin lectores
  (`recipe-card-l2-unified`); por eso esta spec **no** añade constantes nuevas.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo ni `./gradlew --stop`.
- Tests JUnit4 en español, con casos negativos no vacíos (patrón vigente).

### Current Gaps

- El hero y la tira llevan un `padding(horizontal = SazonSpacing.md)` que **contradice** `DESIGN.md`
  (`Shapes`) y las capturas aceptadas: es el único defecto que esta feature corrige.
- "Sin padding lateral" **no** es aseverable en JVM (no hay test de composición en `testImplementation`): se
  verifica por inspección del diff y contra las capturas, como el resto de geometrías Compose del Detalle.

## Technical Approach

1. **Hero a sangre** (`DetailHero`, `RecipeDetailScreen.kt`): eliminar `.padding(horizontal = SazonSpacing.md)`
   de la cadena del `Box` del hero (línea 215) y dejarla como
   `.fillMaxWidth().clip(HeroBottomShape).background(recipeAccentBrush(style.accent)).height(RecipeDetailHeroFormat.HERO_HEIGHT_DP.dp)`.
   No cambia nada más del bloque: `HeroLayer`/`HeroForeground` (marca de agua, tipografía, filas), el filete de
   6 dp, el `Row` flotante de "Atrás"/corazón (conserva `.padding(SazonSpacing.md)`) ni
   `HERO_PADDING`/`WATERMARK_OFFSET_X/Y`.
2. **Tira a sangre** (`MetaStrip`): eliminar `.padding(horizontal = SazonSpacing.md)` del `modifier` de la
   `Surface` (línea 399), conservando `fillMaxWidth()`, `shape = HeroBottomShape`, `color = scheme.surface` y
   `shadowElevation = 4.dp`; su fila interna (`vertical = SazonSpacing.sm`, `horizontal = 4.dp`) y los
   hairlines verticales **no cambian**. Al compartir `HeroBottomShape`, las esquinas inferiores de hero y
   tira quedan en la **misma** x.
3. **Revisión de lo que el margen ocultaba** (sin cambios de código): el `clip` sigue recortando el filete y
   la marca de agua; la capa de imagen + `scrim` de `RecipeArtwork` (hijo con `matchParentSize()` del `Box`
   recortado) cubre ahora el ancho completo **sin tocar `ui/artwork`**; los controles flotantes quedan
   **sobre** el hero a 16 dp del borde con sus objetivos táctiles de hoy (≥48 dp).
4. **KDoc**: actualizar el encabezado de `RecipeDetailScreen`, de `DetailHero` y de `MetaStrip` para
   registrar el borde a borde (`DESIGN.md` → `Shapes`, revisión 2026-10-14, `detail-hero-full-bleed`) y la
   alineación de la tira con el hero; sin tocar comentarios de otros bloques.
5. **Sin cambios** en `RecipeDetailHeroFormat`, `RecipeDetailFormat`, `RecipeDetailState`, `ServingScaler`,
   `IngredientFormat`, `ui/artwork/**`, `ui/theme/**`, `ui/**` (resto), `domain/**`, `data/**`, build/gradle,
   `init.sh`, `scripts/**` ni `docs/design/concepts/**`, y **sin tests nuevos**.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — **modify**; se eliminan **dos** líneas
  (el `padding(horizontal = SazonSpacing.md)` del hero y el de la tira) y se actualiza el KDoc del
  encabezado, de `DetailHero` y de `MetaStrip`. **Ningún otro cambio** en el fichero.
- `app/src/test/java/com/sazon/app/ui/detail/*Test.kt` — **sin cambios** (`RecipeDetailHeroFormatTest`,
  `RecipeDetailFormatTest`, `ServingScalerTest`, `IngredientFormatTest`, `RecipeDetailStateTest`).
- `DESIGN.md` — **sin cambios**: ya declara el borde a borde de `detail-hero-full-bleed`; el implementer
  **verifica** que sigue siendo cierto, pero no lo edita.
- `docs/technical-discovery.md` — **sin cambios**: describe la lógica pura y las costuras del Detalle, no
  insets laterales (la fuente de verdad visual es `DESIGN.md`).
- `docs/adr/0006-typographic-recipe-cards.md` — **sin cambios**: la dirección no cambia, solo se cumple.
- `docs/design/concepts/**` — **sin cambios** y **sin crear** `docs/design/concepts/detail-hero-full-bleed/`.
- `feature_list.json`, `PROGRESS.md` — los actualiza el **orquestador** (no el implementer).

**Debe quedar intacto** (disciplina de alcance): `ui/detail/{RecipeDetailHeroFormat,RecipeDetailFormat,
RecipeDetailState,ServingScaler,IngredientFormat}.kt`, `ui/artwork/**`, `ui/theme/**`, `ui/home/**`,
`ui/results/**`, `ui/favorites/**`, `ui/cooking/**`, `ui/preferences/**`, `ui/MainShell.kt`, `domain/**`,
`data/**`, `app/src/main/assets/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`,
`app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **sí** (`area: ui`).
- Design source: `DESIGN.md` → `Shapes` (hero 240 con esquinas inferiores 24 + tira con las mismas esquinas y
  sombra suave; **de borde a borde** desde el 2026-10-14) y `Core Screens` 5; `Accessibility Baseline`
  (≥48 dp) para los controles flotantes.
- Screens or states affected: **Detalle** (hero + tira) en **claro y oscuro**, con el ajuste de imágenes
  **encendido** (imagen de fondo + `scrim`) y **apagado** (hero tipográfico). El resto de la pantalla no
  cambia de diseño.
- New design artifact required: **no** — **exención de capturas autorizada** (issue #132, 2026-10-14).
  **`docs/design/concepts/detail-hero-full-bleed/` no debe crearse** (el validador comprueba `test ! -e`).
- **Capturas de referencia** (ya aceptadas, solo lectura): son **anteriores** a la revisión y aún muestran
  el inset de 16 px, así que **no** prueban el borde a borde; manda `DESIGN.md`.
  `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/{screen.png,screen-dark.png}` y
  `docs/design/concepts/dish-artwork-as-card-background/detalle-con-imagen/{screen.png,screen-dark.png}`
  (apoyo: sus `code.html`, con `.hero`/`.metastrip` de 240/24 dp de radio pero envueltos en
  `<div style="padding:0 16px">`).
- Desviación declarada y **no bloqueante**: los controles del prototipo son círculos de 34 px a 14 px del
  borde; la app mantiene píldora + `Surface` de 48 dp a 16 dp por `Accessibility Baseline`. Manda `DESIGN.md`;
  un defecto visual se corrige con una feature correctiva.

## Durable Documentation Impact

- `DESIGN.md`: **not needed** — ya declara el borde a borde de `detail-hero-full-bleed`; esta feature hace
  que la app cumpla ese texto.
- `docs/technical-discovery.md`: **not needed** — su párrafo del Detalle describe la lógica pura y las
  costuras (overlay, `imageBackground`), no los insets laterales; duplicar `DESIGN.md` está desaconsejado.
- `docs/domain-model.md`, `docs/adr/0006-typographic-recipe-cards.md`, `AGENTS.md`,
  `docs/risks-and-open-questions.md`: **not needed** — sin entidades/estados nuevos, sin decisiones nuevas
  de arquitectura, sin cambios de flujo/arranque y sin riesgo nuevo (la revisión manual se registra en
  `PROGRESS.md`).
- `docs/design/concepts/**`: **not needed** — exención de capturas; el implementer no crea ni edita nada.

## Implementation Plan

1. Retirar el `padding(horizontal = SazonSpacing.md)` del hero (línea 215) y de la tira (línea 399),
   dejando `clip(HeroBottomShape)`, el fondo, el alto de 240 y el filete como están.
2. Confirmar por lectura que el `Row` flotante, `HERO_PADDING`, los offsets de la marca de agua, la fila
   interna de la tira y `HeroBottomShape` (compartido) no cambian, y que la capa de fondo sigue dentro del
   `Box` recortado.
3. Actualizar el KDoc del encabezado, de `DetailHero` y de `MetaStrip` (borde a borde, alineación con el
   hero, `DESIGN.md` → `Shapes`, 2026-10-14).
4. Revisar el resultado contra las cuatro capturas de referencia (claro/oscuro × ajuste on/off): recorte de
   las esquinas inferiores y posición/legibilidad de los controles flotantes.
5. Ejecutar el gate (`bash init.sh`) y los greps de alcance; registrar la evidencia.

## Implementation Tasks

- [ ] Quitar `.padding(horizontal = SazonSpacing.md)` del `Box` del hero y de la `Surface` de la tira.
- [ ] Verificar que el hero conserva `clip(HeroBottomShape)`, `background(recipeAccentBrush(...))`,
      `height(HERO_HEIGHT_DP)` y el filete de 6 dp; y la tira, `shape = HeroBottomShape`, `color = surface`
      y `shadowElevation = 4.dp`.
- [ ] Confirmar (sin tocar) el `Row` flotante: "Atrás" + corazón a `SazonSpacing.md` del borde, corazón de
      48 dp, comportamiento intacto, y que `RecipeArtwork`/`scrim` siguen recortados por el `clip`.
- [ ] Actualizar el KDoc del encabezado, de `DetailHero` y de `MetaStrip`.
- [ ] Revisar contra las cuatro capturas y anotar la desviación declarada de los controles (34 px/14 px del
      prototipo vs 48 dp/16 dp de la app).
- [ ] `bash init.sh` verde + greps de alcance + `git diff` = Expected File Changes (2 líneas retiradas + KDoc).
- [ ] Registrar la evidencia (sin commit, push ni PR: los hace el orquestador).

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante estándar: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos; `init.sh` **no** cambia y no arranca servicios).
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.detail.*" --rerun-tasks`
  → `BUILD SUCCESSFUL` con los **cinco** tests de `ui.detail` verdes y **sin diff** (incluido
  `RecipeDetailHeroFormatTest.la_geometria_del_hero_es_contrato`: 240/6/190/0,12f/3).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **0 fallos / 0 errores /
  0 saltados**. Baseline de partida en `HEAD` (`804f009`): **97 suites / 733 tests** → se espera **delta 0**
  (esta feature no añade tests); el implementer re-deriva el número exacto en su HEAD.
- Lint (`app/build/reports/lint-results-debug.sarif`): **0 errores** y ningún hallazgo nuevo bajo `app/src`
  (los 13 warnings preexistentes se mantienen).
- **Inspección (lo que la JVM no puede aseverar)**:
  - `grep -n "horizontal = SazonSpacing.md" app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt`
    → **0** (hoy: líneas 215 y 399).
  - `grep -n "padding(horizontal" .../RecipeDetailScreen.kt` → solo los del cuerpo (`sm`/`lg`), **ninguno**
    en `DetailHero` ni en `MetaStrip`.
  - `grep -n "HeroBottomShape" .../RecipeDetailScreen.kt` → definido una vez y usado **dos** (hero y tira).
  - `git diff --stat` = **1** fichero de código con **2** líneas retiradas + KDoc; `git diff --name-only`
    dentro de Expected File Changes; `git diff` sin cambios en build/lockfile, `init.sh`, `scripts/**`,
    `domain/**`, `data/**`, `assets/**`, `DESIGN.md` ni ADR.
  - `test ! -e docs/design/concepts/detail-hero-full-bleed` → **OK** y `git status docs/design/concepts`
    limpio (exención #132).
- **E2E / UI automatizada: no aplica.** No hay suite E2E persistente y `AGENTS.md` prohíbe automatizar UI de
  emulador; el padding/geometría se cubre por inspección y el acabado lo revisa la persona.
- **Prueba manual (persona, emulador; no automatizable desde WSL)**: abrir el Detalle desde Inicio y desde
  Resultados, en **claro y oscuro**, con el ajuste **apagado** y **encendido**: (a) hero y tira llegan a los
  bordes; (b) las esquinas inferiores de 24 dp se ven limpias y alineadas; (c) la marca de agua no se ve
  cortada fuera del hero; (d) "Atrás" y el corazón se leen y responden igual; (e) pestañas, comensales,
  checklist y barra inferior no se han movido.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal `ui.detail`.
- Conteos de suites/tests parseados del XML y delta vs `HEAD` (`804f009`), confirmando las cinco suites de
  `ui.detail` verdes **sin diff**.
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Resultado de los greps de inspección, del `git diff` (2 líneas retiradas + KDoc, un solo fichero de código)
  y de `test ! -e docs/design/concepts/detail-hero-full-bleed` **OK**.
- Confirmación de que las cuatro capturas de referencia existen, se revisaron y la implementación las sigue,
  con la desviación declarada de los controles (34 px/14 px vs 48 dp/16 dp).

## Decisions And Unknowns

Decisiones cerradas (el implementer no las reabre):

1. **"Borde a borde" es solo horizontal**: se retiran exactamente los dos `padding(horizontal = SazonSpacing.md)`.
   Los insets verticales del `Scaffold` quedan como están; el hero verticalmente a sangre sería otra feature.
2. **Sin constantes nuevas en `RecipeDetailHeroFormat`** para el inset 0 ni el radio de 24: el repo retira
   constantes sin lectores y un `padding(0.dp)` solo añadiría un nodo de layout redundante. El radio de 24
   sigue en `HeroBottomShape` (compartido) y su "no cambio" se prueba con el **diff** (no se toca).
3. **Los controles flotantes se mantienen tal cual** (píldora + `Surface` de 48 dp, `SazonSpacing.md`): los
   34 px/14 px del prototipo son dirección visual y `Accessibility Baseline` exige ≥48 dp. Al ir el hero a
   sangre, quedan **íntegramente sobre el hero** (antes colgaban sobre el fondo crema), más cerca del prototipo.
4. **Hero y tira comparten `HeroBottomShape`**: sus esquinas inferiores de 24 dp quedan a la **misma** x; el
   hueco que dejan sobre el borde recto de la tira muestra el fondo de pantalla, y es lo que ya muestra la
   captura aceptada.
5. **Sin tests nuevos ni dependencias**: `androidx.compose.ui.test` solo está en `androidTestImplementation`;
   "sin padding lateral" se verifica por inspección del diff + capturas. Cobertura JVM = suites existentes
   sin diff.

Unknowns y riesgos (no bloquean; quedan anotados):

- **Encuadre de la imagen**: al ensanchar el hero (de 358×240 a 390×240), `ContentScale.Crop` muestra un
  encuadre algo más ancho de la misma imagen; consecuencia esperada de ir a sangre (sin cambio de layout ni
  de código), a revisar contra `detalle-con-imagen`.
- **Insets laterales en horizontal/cutout**: en retrato son 0 (hero a sangre real); en horizontal el
  `Scaffold` puede aportar inset lateral. Fuera de alcance y no observable en el Pixel de referencia.
- **Posición exacta de los controles** (16 dp de la app vs 14 px del prototipo): se acepta la de la app; si
  la revisión visual la quiere a 14 px, es una feature correctiva.
- **Radio de 24 dp y hueco en la unión hero → tira**: se conservan; si la persona ve un artefacto, se
  corrige con feature correctiva sin reabrir esta spec.

## Validator Checklist

- [ ] El hero y la tira **no** tienen `padding(horizontal = SazonSpacing.md)` (grep → 0) y ocupan todo el
      ancho; el diff de `RecipeDetailScreen.kt` es exactamente las **dos** líneas retiradas + KDoc.
- [ ] La **geometría interna** no cambia (240/6/190/0,12f/3 y el contenido de `RecipeDetailHeroFormat`
      intactos, `RecipeDetailHeroFormatTest` verde **sin diff**) y la tira conserva `HeroBottomShape`,
      `surface` y la sombra suave, alineada con el hero.
- [ ] El **recorte de las esquinas inferiores de 24 dp** se revisó (filete dentro del `clip`, marca de agua
      recortada por el hero, hueco de fondo en la unión con la tira) y coincide con `detalle-receta`.
- [ ] "Atrás" y el corazón siguen **legibles y pulsables** (≥48 dp, relleno `surface`, comportamiento
      intacto) sobre el hero a sangre, con el ajuste encendido y apagado.
- [ ] Funciona igual con `imageBackground = true` (imagen + `scrim` a todo el ancho, recortados por el mismo
      `clip`, sin salto de layout) y con `false`/sin imagen (hero tipográfico, degradación silenciosa).
- [ ] Alcance: sin dependencias, tokens, hex, iconos ni fuentes nuevos; `ui/artwork/**`, `ui/theme/**`,
      `ui/**` (resto), `domain/**`, `data/**`, `assets/**`, build/lockfile, `init.sh`, `scripts/**`,
      `DESIGN.md`, el ADR 0006 y `docs/design/concepts/**` intactos.
- [ ] **No** existe `docs/design/concepts/detail-hero-full-bleed/` (exención #132) y las capturas de
      referencia aceptadas se citaron y respetaron.
- [ ] `bash init.sh` verde con la evidencia registrada (comandos, conteos con delta vs `HEAD`, lint, greps,
      diff) y sin cambios en las reglas de verificación.
- [ ] `feature_list.json` (evidencia; el orquestador reconcilia `verification[2]` con la exención de
      capturas, como en `recipe-card-l2-unified`) y `PROGRESS.md` quedan actualizados por el orquestador; el
      implementer no hizo commit, push ni PR.
- [ ] Persistent E2E: no aplica (justificado en el plan de verificación).
- [ ] No se añade comportamiento ajeno a la feature (ni rediseño de pestañas/cuerpo/barra, ni cambios de
      insets verticales, ni capturas nuevas).
