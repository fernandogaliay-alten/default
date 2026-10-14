# Feature Implementation Spec: Detalle: la cabecera termina en bordes rectos

## Source Feature

- `id`: `detail-hero-square-bottom`
- `area`: `ui`
- `depends_on`: `["detail-hero-full-bleed"]` — `accepted` y mergeada (PR #135); dependencia **satisfecha**.
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner **no**
  modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (entrada `detail-hero-square-bottom`).
- **Correctiva de diseño pedida por la persona** (2026-10-14): la cabecera (hero) del Detalle termina en
  **esquinas inferiores redondeadas** que **no encajan** con la tira de metadatos (Tiempo · Dificultad ·
  Comensales · Calorías) que va justo debajo. Se pide **quitar el redondeado inferior** del hero.
- **Decisión de alcance cerrada (no se reabre)**: solo el **hero**. La **tira conserva** sus esquinas
  inferiores de 24 dp, así que el bloque hero+tira mantiene el borde inferior redondeado; cuadrar también la
  tira sería **otra** correctiva.
- **Política del harness vigente (sesión 122)**: las capturas de UI **ya no son un requisito** (`AGENTS.md`
  → "Diseño de UI opcional"); `DESIGN.md` es la **fuente de verdad visual**. Esta spec **no** añade
  obligaciones ni exenciones de capturas.

## Goal

Sustituir el recorte con esquinas inferiores redondeadas del **hero** del Detalle por un recorte
**rectangular**, de modo que su borde inferior sea **recto** y encaje **a ras** con el borde superior
(recto) de la tira de metadatos; el hero **sigue recortando su contenido a sus límites** (la marca de agua
del glifo, 190 dp al 12 % con offsets 24/6, desborda por la esquina inferior derecha y el filete de 6 px va
al pie, dentro del recorte). Nada más del Detalle se mueve: ni la geometría (240/6/190/0,12f), ni el
contenido (categoría · cocina, título, tres ingredientes con cantidad, los cuatro pares de metadatos), ni
la tipografía, ni la tira (que conserva sus 24 dp y su sombra), ni pestañas, cuerpo, escalador de
comensales, barra inferior ni estado.

La feature **reconcilia la fuente de verdad visual**: `DESIGN.md` hoy afirma que el hero lleva esquinas
inferiores de 24 dp y que la tira lleva "las mismas"; tras la correctiva el hero **no** lleva esquinas
inferiores y la tira **conserva** las suyas.

## Non-Goals

- **No** se toca la **tira de metadatos**: conserva esquinas inferiores de 24 dp, `color = surface`,
  `shadowElevation = 4.dp`, su fila interna (`vertical = sm`, `horizontal = 4.dp`), sus cuatro pares y sus
  hairlines. Cuadrarla también es otra correctiva.
- **No** se elimina el **recorte** del hero: se sustituye el redondeado por uno **rectangular**. Quitar el
  `clip` sin más es un **defecto** (la marca de agua invadiría la tira/fondo), no una opción.
- **No** cambian: alto 240 (`RecipeDetailHeroFormat.HERO_HEIGHT_DP`), filete 6 px (`FILLET_HEIGHT_DP`) y su
  `Brush.horizontalGradient`, `HeroContent`/`of()`, `HERO_PADDING = 18.dp`, `WATERMARK_OFFSET_X/Y = 24/6`,
  `WATERMARK_DP = 190`, `WATERMARK_ALPHA = 0,12f`, `HeroTypography`, `RecipeDetailFormat.metaEntries`
  (incluidos `"480 kcal"` y el `"—"` sin nutrición), ni sus tests.
- **No** cambian: las esquinas **superiores** del hero (ya rectas), el `Box` exterior
  `Modifier.fillMaxWidth()`, el `Row` flotante "Atrás" + corazón (posición, 48 dp, relleno `surface`,
  comportamiento), el reparto de `imageBackground` ni la capa de fondo `RecipeArtwork`
  (`matchParentSize()` dentro del `Box` recortado: hereda el recorte rectangular).
- **No** cambian: pestañas, cuerpo por pestaña, `ServingsStepper`, caption, `Hairline`, barra inferior,
  `RecipeDetailState`/`Saver`, `ui/MainShell.kt` ni las demás pantallas.
- **No** se toca `ui/detail/{RecipeDetailHeroFormat,RecipeDetailFormat,RecipeDetailState,ServingScaler,IngredientFormat}.kt`,
  `ui/artwork/**`, `ui/theme/**`, `domain/**`, `data/**`, `assets/**`, build/gradle, `init.sh`,
  `scripts/**`, el ADR 0006 ni `docs/design/concepts/**`; sin dependencias, tokens, hex, iconos, fuentes ni
  constantes nuevas de geometría, y **sin** rediseñar nada más de la pantalla (un defecto visual posterior
  se corrige con una feature correctiva).
- **Sin tests nuevos**: no hay tests de composición en `testImplementation`
  (`androidx.compose.ui.test.junit4` solo está en `androidTestImplementation`) y "sin redondeo inferior" no
  es aseverable en JVM; la cobertura JVM son las suites existentes **sin diff**.

## Job Story

Cuando abro el Detalle de una receta y miro la unión entre la cabecera y la tira de metadatos,
quiero que la cabecera termine en bordes rectos y encaje a ras con la tira,
para que el bloque superior se lea como una sola pieza continua y no como dos superficies que no casan.

## Users And Permissions

- **Usuario local**: abre el Detalle, ajusta comensales, cambia de pestaña, guarda y vuelve con "Atrás".
  Sin permisos, sin red y sin nada nuevo que aprender.
- **Desarrollador / agente**: verifica en **JVM** que geometría, contenido y suites del Detalle no cambian,
  y por **inspección del diff** que el hero ya no aplica ninguna forma redondeada y que sigue recortando; el
  acabado visual lo revisa la persona en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: idéntico a hoy; el ajuste de imágenes solo decide la **capa de fondo** del hero.

## Acceptance Scenarios

### Scenario 1: la cabecera termina en bordes rectos y encaja a ras con la tira

Given el Detalle abierto (ajuste de imágenes **apagado**),
When se pinta el hero,
Then su borde **inferior es recto** de extremo a extremo (sin esquinas redondeadas), sigue midiendo
**240 dp** y ocupando todo el ancho, y coincide **a ras en x** con el borde superior —también recto— de la
tira: ya no queda hueco que muestre el fondo de pantalla en la unión (antes el hueco de las esquinas de
24 dp dejaba ver el fondo sobre la tira).

### Scenario 2: el hero sigue recortando su contenido (marca de agua y filete)

Given el Detalle abierto con el hero sin esquinas inferiores redondeadas,
When se mira la esquina inferior derecha y el pie del hero,
Then la **marca de agua** (190 dp al 12 %, offsets +24/+6) sigue **desbordando** por la esquina inferior
derecha y queda **recortada por el recorte rectangular del hero** (no invade la tira ni el fondo), y el
**filete de 6 px** sigue al pie, **dentro** del recorte y a todo el ancho; sin bordes duros, sombras
marcadas ni restos fuera del hero.

### Scenario 3: la tira de metadatos no cambia

Given el Detalle abierto,
When se recorre la parte superior,
Then la tira conserva **sus** esquinas inferiores de 24 dp (el borde inferior redondeado del bloque
hero+tira sigue existiendo), `surface`, `shadowElevation = 4.dp`, sus cuatro pares en columnas iguales con
hairline vertical (no tras la última), etiqueta en versalitas y valor en Manrope tabular, con los **mismos
valores** de hoy.

### Scenario 4: nada más del Detalle se mueve

Given la implementación terminada,
When se inspecciona el diff y se ejecuta el gate,
Then `RecipeDetailHeroFormat`/`RecipeDetailHeroFormatTest` y `RecipeDetailFormat`/`RecipeDetailFormatTest`
están **sin cambios**, y pestañas, checklist, `ServingsStepper`, caption, barra inferior,
`RecipeDetailState`/`Saver` y el overlay de `MainShell` quedan **intactos**.

### Scenario 5: el ajuste de imágenes se ve igual (encendido y apagado)

Given `imageBackground = true` y el resolver devolviendo imagen,
When se pinta el hero,
Then la imagen (`ContentScale.Crop`) y el `scrim` cubren todo el ancho recortados por el **mismo recorte
rectangular** (la capa de fondo sigue siendo hija con `matchParentSize()` del `Box` recortado, sin tocar
`ui/artwork/**`), la geometría (240 dp) y la tira no se mueven (sin salto de layout) y el fundido sigue en
`DishImageArtworkPolicy.CROSSFADE_MILLIS`. Con `imageBackground = false` o sin imagen (fallo, timeout,
cancelación o tope agotado) el hero es el **tipográfico** de hoy, con la marca de agua visible, sin
microcopy ni error.

### Scenario 6: claro y oscuro

Given `darkTheme = true` (y `false`),
When se pinta el Detalle,
Then el resultado es el mismo con los roles oscuros: hero con borde inferior recto, tira con sus esquinas
inferiores de 24 dp y contraste de texto sin cambios.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `detail-hero-square-bottom`) — `area: ui`, dependencia `detail-hero-full-bleed`
  (`accepted`, PR #135 mergeado), `verification` textual (recorte rectangular + geometría/contenido
  intactos + gate verde + sin dependencias) y `notes` con el alcance cerrado y la reconciliación de
  `DESIGN.md`.
- `PROGRESS.md` — sesión 121 (precedente): gate verde con **98 suites / 737 tests / 0 fallos / 0 errores /
  0 saltados** y lint **0 errores / 13 warnings** preexistentes. Sesión 122: capturas **opcionales**;
  `DESIGN.md` manda como fuente de verdad visual.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — **único** fichero de código a tocar:
  `HeroBottomShape` (línea 80, `RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)`) compartido por
  el hero (línea 219 `.clip(HeroBottomShape)`) y la tira (línea 403 `shape = HeroBottomShape`); KDoc del
  encabezado (106-115, cita `HeroBottomShape` y "las **mismas** esquinas"), de `DetailHero` (192-201) y de
  `MetaStrip` (387-393); comentario de la marca de agua (311, ya describe el recorte);
  `import androidx.compose.ui.draw.clip` (42) solo se usa en el hero; `RoundedCornerShape` seguiría en uso
  por la tira.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailHeroFormat.kt` +
  `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailHeroFormatTest.kt` — geometría/contenido puros
  (240/6/190/0,12f/3): contrato de "no cambia"; **sin diff**.
- `app/src/main/java/com/sazon/app/ui/theme/Shape.kt` — `SazonShapes.medium = RoundedCornerShape(24.dp)`; no
  se añade ni cambia ninguna forma del tema. `ui/artwork/RecipeArtwork.kt` y `ui/theme/CardImageOverlay.kt`
  — la capa de fondo es hija `matchParentSize()` del `Box` recortado (hereda el recorte) y el `scrim` es un
  degradado vertical de cobertura completa: el recorte rectangular no los altera.
- `app/build.gradle.kts` (108-118) — `androidx.compose.ui.test.junit4` solo en `androidTestImplementation`:
  **no** hay tests de composición en `test`.
- `DESIGN.md` — a **reconciliar**: `Shapes` (326-331: "hero tipográfico de 240 con esquinas inferiores de 24
  y, debajo, la tira de metadatos con las mismas esquinas y solo la sombra suave"; más el borde a borde de
  `detail-hero-full-bleed`) y `Core Screens` 5 (~502-507: hero + tira "de borde a borde" desde 2026-10-14,
  sin decir cómo termina el hero). El bloque histórico de "Excepción de capturas (#129-#132)" (237-260) y
  `docs/design/concepts/**` **no** se tocan.
- `docs/technical-discovery.md` (344-356) — describe la lógica pura y la capa Compose del Detalle
  (240 dp, filete 6 px, marca de agua 190 dp al 12 %); **no** menciona esquinas inferiores.
- `docs/adr/0006-typographic-recipe-cards.md` (grep `esquina` → 0) y
  `docs/design/concepts/recipe-detail-screen/detalle-receta/code.html` (material **histórico** que aún dice
  "esquinas inferiores a 24px" del Detalle antiguo con tarjeta superpuesta): ninguno es fuente de verdad.
- `docs/specs/detail-hero-full-bleed.md` + `docs/validations/detail-hero-full-bleed.md` — contrato y
  validación que **no** se reabren (esta correctiva solo cambia el recorte inferior del hero).
- `test ! -e docs/design/concepts/detail-hero-square-bottom` → **OK** (no existe; capturas opcionales).

### Existing Patterns To Follow

- **Capa Compose delgada sobre lógica pura**: los `@Composable` pintan; geometría y contenido publicados
  viven en `RecipeDetailHeroFormat`/`RecipeDetailFormat` y se testean en JVM.
- **Roles del tema y cero hex** fuera de `ui/theme`; **cifras tabulares** en cantidades y metadatos.
- **Sin nombres engañosos ni constantes muertas** (el repo retiró `SazonComponents.recipeCardImageRatio` al
  quedarse sin lectores): una constante que sobrevive **se renombra** para que su nombre siga siendo cierto.
- **KDoc como contrato**: los bloques del Detalle citan `DESIGN.md` y la feature que fijó la dirección; al
  cambiar la dirección, el KDoc se actualiza en el mismo cambio.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo ni `./gradlew --stop`.
- Tests JUnit4 en español, con casos negativos no vacíos (patrón vigente).

### Current Gaps

- El hero aplica `HeroBottomShape` y la tira lo **reutiliza**: el hero no puede dejar de redondear sin dejar
  de compartir la constante. Es el defecto a corregir.
- `DESIGN.md` (`Shapes`, `Core Screens` 5) declara hoy lo contrario de lo que la persona pide.
- "Sin redondeo inferior" **no** es aseverable en JVM: se verifica por inspección del diff (grep) y el
  acabado lo revisa la persona en el emulador.

## Technical Approach

1. **Recorte rectangular del hero** (`DetailHero`, línea ~219): sustituir `.clip(HeroBottomShape)` por un
   recorte **rectangular** —`.clip(RectangleShape)` (preferido; conserva el import de `clip` y añade
   `androidx.compose.ui.graphics.RectangleShape`)— dejando
   `.fillMaxWidth().clip(RectangleShape).background(recipeAccentBrush(style.accent)).height(RecipeDetailHeroFormat.HERO_HEIGHT_DP.dp)`.
   **Equivalente aceptado**: `.clipToBounds()`, siempre que se **retire** el import
   `androidx.compose.ui.draw.clip` si queda sin uso (evitar imports muertos/hallazgos nuevos de lint). El
   motivo de conservar el recorte: la marca de agua (offsets +24/+6) desborda por la esquina inferior derecha
   y el filete va al pie; ambos deben quedar recortados a los límites del hero, ahora con esquinas rectas.
2. **Renombrar la constante superviviente** (línea ~80): `HeroBottomShape` → `MetaStripBottomShape` (mismo
   valor, `RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)`) y ajustar su comentario (esquinas
   inferiores **de la tira**). La tira (línea ~403) pasa a `shape = MetaStripBottomShape`.
3. **KDoc** (sin cambios funcionales): encabezado de `RecipeDetailScreen` (el hero termina en **bordes
   rectos**; solo la tira lleva esquinas inferiores de 24 dp, que cierran el bloque; `DESIGN.md` → `Shapes`,
   2026-10-14, `detail-hero-square-bottom`), KDoc de `DetailHero` (recorte rectangular que sigue conteniendo
   marca de agua y filete) y de `MetaStrip` (`MetaStripBottomShape`; ya **no** "las mismas esquinas que el
   hero"). El comentario de la marca de agua (311) sigue siendo cierto: no hace falta tocarlo.
4. **`DESIGN.md` — reconciliación** (dos puntos, nada más): `Shapes` (el hero de 240 termina en **bordes
   rectos**; la tira **conserva** sus esquinas inferiores de 24 dp y la sombra suave; revisión 2026-10-14,
   `detail-hero-square-bottom`; localizar por el texto "hero tipográfico de 240 con esquinas inferiores de
   24 y, debajo, la tira de metadatos con las mismas esquinas") y `Core Screens` 5 (el hero termina en
   **bordes rectos** y la tira conserva sus esquinas inferiores). **No** tocar el frente de capturas.
5. **Sin cambios** en `RecipeDetailHeroFormat`, `RecipeDetailFormat`, `RecipeDetailState`, `ServingScaler`,
   `IngredientFormat`, `ui/artwork/**`, `ui/theme/**`, `ui/MainShell.kt`, resto de `ui/**`, `domain/**`,
   `data/**`, assets, build/gradle, `init.sh`, `scripts/**`, ADR 0006 ni `docs/design/concepts/**`, y **sin
   tests nuevos**.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — **modify**: (a) `.clip(HeroBottomShape)`
  → recorte rectangular en `DetailHero` (+ import `RectangleShape`); (b) `HeroBottomShape` →
  `MetaStripBottomShape` (definición, comentario y uso en la tira); (c) KDoc del encabezado, de `DetailHero`
  y de `MetaStrip`. **Ningún otro cambio** en el fichero.
- `DESIGN.md` — **modify**: `Shapes` y `Core Screens` 5 (hero con esquinas inferiores rectas; tira con las
  suyas de 24 dp). Nada más.
- `docs/specs/detail-hero-square-bottom.md` — **create** (esta spec, del planner).
- `feature_list.json` — `evidence`/estado los registra quien implementa y el **orquestador** reconcilia el
  estado (`passing` → `accepted`); el planner **no** lo toca.
- `app/src/test/java/com/sazon/app/ui/detail/*Test.kt` — **sin cambios** (`RecipeDetailHeroFormatTest`,
  `RecipeDetailFormatTest`, `ServingScalerTest`, `IngredientFormatTest`, `RecipeDetailStateTest`).
- `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/adr/0006-typographic-recipe-cards.md`,
  `AGENTS.md`, `docs/risks-and-open-questions.md`, `init.sh`, `scripts/**` — **sin cambios**.

**Debe quedar intacto** (disciplina de alcance): `ui/detail/{RecipeDetailHeroFormat,RecipeDetailFormat,
RecipeDetailState,ServingScaler,IngredientFormat}.kt`, `ui/artwork/**`, `ui/theme/**`, `ui/home/**`,
`ui/results/**`, `ui/favorites/**`, `ui/cooking/**`, `ui/preferences/**`, `ui/MainShell.kt`, `domain/**`,
`data/**`, `app/src/main/assets/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`,
`app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `docs/design/concepts/**` y las specs
aceptadas.

## Visual Design Impact

- UI involved: **sí** (`area: ui`).
- Design source: `DESIGN.md` → `Shapes` y `Core Screens` 5, **reconciliados por esta feature** (hero con
  bordes rectos; tira con sus 24 dp inferiores y su sombra). Sin tokens, hex ni formas nuevas.
- Screens or states affected: **Detalle** (hero) en **claro y oscuro**, con el ajuste de imágenes
  **encendido** (imagen de fondo + `scrim`) y **apagado** (hero tipográfico con marca de agua y filete). La
  tira y el resto de la pantalla no cambian de diseño.
- New design artifact required: **no**. Las capturas son **opcionales** desde la sesión 122 (`AGENTS.md` →
  "Diseño de UI opcional") y `DESIGN.md` es la fuente de verdad; **no** existe
  `docs/design/concepts/detail-hero-square-bottom/` y `docs/design/concepts/**` no se toca. El cambio es
  **quitar** un redondeo existente y registrarlo, no inventar dirección visual: no hay gap de diseño.
- El acabado final lo revisa la persona en el emulador tras el merge; un defecto visual se corrige con una
  feature correctiva.

## Durable Documentation Impact

- `DESIGN.md`: **update** — es la fuente de verdad visual y hoy afirma que el hero lleva esquinas inferiores
  de 24 dp y que la tira lleva "las mismas"; se reconcilian `Shapes` y `Core Screens` 5 (cambio pedido por
  la persona).
- `docs/technical-discovery.md`: **not needed** — describe geometría/contenido publicados (240/6/190/al 12 %)
  y las costuras del Detalle, no las esquinas (grep `esquina` → 0 en ese documento).
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados nuevos.
- `AGENTS.md`: **not needed** — no cambian workflow, arranque ni reglas de verificación (las capturas ya
  eran opcionales desde la sesión 122).
- `docs/adr/0006-typographic-recipe-cards.md`: **not needed** — no menciona esquinas y la dirección no
  cambia; solo se precisa su acabado.
- `docs/risks-and-open-questions.md`: **not needed** — sin riesgo nuevo de producto; los de implementación
  van en esta spec y la revisión manual se registra en `PROGRESS.md`.
- `docs/design/concepts/**`: **not needed** — capturas opcionales; no se crea ni se edita nada (los
  `code.html` históricos que citan 24 px son material antiguo, no fuente de verdad).

## Implementation Plan

1. En `RecipeDetailScreen.kt`, sustituir `.clip(HeroBottomShape)` del `DetailHero` por un recorte
   **rectangular** manteniendo el orden de la cadena (`fillMaxWidth` → recorte → `background` → `height`).
2. Renombrar `HeroBottomShape` → `MetaStripBottomShape` (definición, comentario y uso en `MetaStrip`), de
   modo que sea la única forma redondeada del fichero.
3. Actualizar el KDoc del encabezado, de `DetailHero` y de `MetaStrip`.
4. Reconciliar `DESIGN.md` (`Shapes` + `Core Screens` 5), sin tocar nada de capturas.
5. Confirmar por lectura que geometría (240/6/190/0,12f), contenido, `HeroTypography`, el `Row` flotante, la
   fila interna de la tira, la capa de fondo y `imageBackground` no cambian.
6. Ejecutar gate (`bash init.sh`), tirada focal de `ui.detail` y greps de alcance; registrar evidencia (sin
   commit, push ni PR: los hace el orquestador).

## Implementation Tasks

- [ ] Recorte rectangular del hero (`.clip(RectangleShape)` o `.clipToBounds()`), conservando el orden de la
      cadena de modificadores y sin dejar imports muertos.
- [ ] Renombrar `HeroBottomShape` → `MetaStripBottomShape` y usarla solo en `MetaStrip`.
- [ ] Verificar (sin tocar) 240 dp, filete 6 y su gradiente, `HERO_PADDING` 18, offsets 24/6, marca de agua
      190 al 12 %, `HeroTypography`, el `Row` flotante de "Atrás"/corazón y la fila interna de la tira.
- [ ] Actualizar el KDoc del encabezado, de `DetailHero` y de `MetaStrip`.
- [ ] Reconciliar `DESIGN.md` (`Shapes` + `Core Screens` 5) y **no** tocar capturas ni
      `docs/design/concepts/**`.
- [ ] `bash init.sh` verde + tirada focal `ui.detail` + greps de alcance + `git diff` = Expected File Changes.
- [ ] Registrar la evidencia en `feature_list.json`/`PROGRESS.md` según `feature-flow` (el planner no los
      toca; el implementer no hace commit, push ni PR).

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante estándar: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos; `init.sh` **no** cambia y no arranca servicios).
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.detail.*" --rerun-tasks`
  → `BUILD SUCCESSFUL` con los **cinco** tests de `ui.detail` verdes y **sin diff** (incluido
  `RecipeDetailHeroFormatTest.la_geometria_del_hero_es_contrato`: 240/6/190/0,12f/3).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **0 fallos / 0 errores /
  0 saltados**. Baseline registrada en `PROGRESS.md` (sesión 121) y re-derivada en `HEAD` `6f4e353`:
  **98 suites / 737 tests** (`git grep -c "@Test" -- 'app/src/test/**'` = 98 ficheros / 737 anotaciones) →
  **delta 0** esperado; el implementer re-deriva el número exacto en su HEAD.
- Lint (`app/build/reports/lint-results-debug.sarif`): **0 errores** y ningún hallazgo **nuevo** bajo
  `app/src` (los 13 warnings preexistentes se mantienen; ojo con imports muertos si se usa `clipToBounds()`).
- **Inspección (lo que la JVM no puede aseverar)**, en `.../ui/detail/RecipeDetailScreen.kt`:
  - `grep -n "HeroBottomShape"` → **0**; `grep -n "MetaStripBottomShape"` → definido **una** vez y usado
    **una** vez (la tira).
  - `grep -n "RoundedCornerShape"` → solo la **definición** de la constante de la tira (el hero ya no aplica
    forma); `grep -n "bottomStart\|bottomEnd"` → solo en esa constante.
  - `grep -n "clip("` → un único recorte, el del `DetailHero`, **rectangular**.
  - `git diff --name-only` dentro de Expected File Changes; el diff de código es un solo fichero con el
    recorte, el rename y KDoc; `DESIGN.md` con **solo** los dos puntos reconciliados; sin diff en
    formato/estado/ServingScaler/IngredientFormat/`ui.artwork`/`ui.theme`/`MainShell`/`domain`/`data`/
    assets/build/lockfile/`init.sh`/scripts/ADR 0006/`docs/design/concepts/**`.
  - `test ! -e docs/design/concepts/detail-hero-square-bottom` → **OK** y `git status docs/design/concepts`
    limpio.
  - Recordatorio: si el árbol de trabajo ya trae cambios de **harness** (sesión 122) sin commitear, **no**
    forman parte del diff de esta feature; acotar `git diff` a la rama.
- **E2E / UI automatizada: no aplica.** No hay suite E2E persistente y `AGENTS.md` prohíbe automatizar UI de
  emulador; "sin redondeo inferior" se cubre por inspección del diff y el acabado lo revisa la persona.
- **Prueba manual (persona, emulador; no automatizable desde WSL)**: abrir el Detalle desde Inicio y desde
  Resultados, en **claro y oscuro**, con el ajuste **apagado** y **encendido**: (a) el hero termina en
  bordes rectos y encaja a ras con la tira (sin hueco de fondo en la unión); (b) la tira conserva su borde
  inferior redondeado y su sombra; (c) la marca de agua y el filete siguen dentro del hero; (d) "Atrás" y el
  corazón siguen legibles y pulsables; (e) pestañas, comensales, checklist y barra inferior no se han movido.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal `ui.detail` (cinco suites verdes sin diff).
- Conteos de suites/tests parseados del XML y delta vs la baseline (`98/737` en `HEAD` `6f4e353`); lint
  (0 errores, 13 warnings preexistentes, 0 nuevos bajo `app/src`).
- Greps de inspección (`HeroBottomShape` → 0; `MetaStripBottomShape` definido/usado una vez;
  `RoundedCornerShape` solo en la constante de la tira; un único recorte, rectangular), `git diff`
  (un fichero de código + `DESIGN.md`), ausencia de tests/dependencias nuevas y
  `test ! -e docs/design/concepts/detail-hero-square-bottom` **OK**.
- Cita del texto **antes/después** de `DESIGN.md` en `Shapes` y `Core Screens` 5.
- Revisión manual pendiente de la persona tras el merge (emulador, claro/oscuro × ajuste on/off).

## Decisions And Unknowns

Decisiones cerradas (el implementer no las reabre):

1. **Alcance = solo el hero**. La **tira conserva** sus esquinas inferiores de 24 dp: el bloque hero+tira
   mantiene el borde inferior redondeado. Cuadrar también la tira sería **otra** correctiva (y está fuera
   del `verification` de la feature).
2. **Recorte rectangular, no ausencia de recorte**: el hero debe seguir recortando su contenido (marca de
   agua con offsets +24/+6 y filete de 6 px al pie, dentro del clip). Se prefiere `.clip(RectangleShape)`
   (conserva el import de `clip`); `.clipToBounds()` es equivalente si se retira el import que quede sin uso.
3. **Renombrar `HeroBottomShape` → `MetaStripBottomShape`** (mismo fichero, misma constante privada, mismo
   valor de 24 dp): al usarse solo en la tira, el nombre viejo sería engañoso. `RoundedCornerShape` sigue en
   uso, así que su import no cambia.
4. **Sin constantes nuevas** de geometría ni de radio (no se introduce un `0.dp`: sería un nodo de layout
   redundante y el repo retira constantes sin lectores). El "sin redondeo" se prueba con el **diff/grep**.
5. **Sin tests nuevos ni dependencias**: `androidx.compose.ui.test` solo está en `androidTestImplementation`;
   "sin redondeo inferior" no es aseverable en JVM. Cobertura JVM = suites existentes sin diff.
6. **`DESIGN.md` se reconcilia** (Shapes + Core Screens 5): es la fuente de verdad visual y hoy declara lo
   contrario; cambio pedido por la persona, no ampliación de alcance. **No** se toca nada de capturas.
7. **Controles flotantes y capa de fondo intactos**: "Atrás"/corazón y `RecipeArtwork` (hija
   `matchParentSize()` del `Box` recortado) se comportan igual; el recorte rectangular no cambia su geometría
   ni su objetivo táctil (≥48 dp).

Unknowns y riesgos (no bloquean; quedan anotados):

- **Hueco en la unión hero → tira**: era consecuencia directa del redondeo; al cuadrar el hero el hueco
  desaparece y la unión se vuelve una línea recta continua. Solo la persona puede juzgar el acabado en el
  emulador; si no le convence, es una feature correctiva.
- **Recorte y desbordamiento de la marca de agua**: si se elimina el recorte en lugar de cuadrarlo, la marca
  de agua saldría fuera del hero (sobre la tira/fondo). El escenario 2 y el grep del recorte lo cubren.
- **`DESIGN.md` línea ~392** ("no cambian esquinas ni tamaños", sección de la imagen de fondo): redacción
  genérica de `recipe-photos` sobre el recorte de la foto; no nombra 24 dp ni el hero. Se deja como está; si
  el validador lo lee como deriva documental, es informativo y no bloqueante (mismo tipo de hallazgo Low que
  en features previas).
- **`docs/design/concepts/recipe-detail-screen/detalle-receta/code.html`** sigue diciendo "esquinas
  inferiores a 24px": es dirección visual antigua (previa a ADR 0006 y a `detail-hero-full-bleed`) y no
  fuente de verdad; no se reescribe (mismo criterio que la sesión 122 con la historia).
- **Modo oscuro**: el recorte no depende de roles; la verificación manual debe cubrir claro y oscuro para
  descartar artefactos en la unión.

## Validator Checklist

- [ ] El hero **ya no** aplica ninguna forma redondeada: `grep "HeroBottomShape"` → **0**;
      `grep "RoundedCornerShape"` en `RecipeDetailScreen.kt` → solo la constante de la tira; el único
      recorte del fichero es el del `DetailHero` y es **rectangular**.
- [ ] La **tira de metadatos no cambia**: conserva sus esquinas inferiores de 24 dp
      (`MetaStripBottomShape`, usada **solo** ahí), `surface`, `shadowElevation = 4.dp`, sus cuatro pares y
      sus hairlines.
- [ ] El hero sigue **recortando** marca de agua y filete a sus límites (escenario 2): no se ha quitado el
      recorte.
- [ ] **Geometría y contenido intactos**: 240/6/190/0,12f y `RecipeDetailHeroFormat`/`RecipeDetailFormat` sin
      diff, con sus tests verdes; sin tests nuevos.
- [ ] `DESIGN.md` reconciliado **solo** en `Shapes` y `Core Screens` 5, sin tocar el bloque histórico de
      capturas ni `docs/design/concepts/**`.
- [ ] Claro y oscuro, y ajuste de imágenes **encendido y apagado**: sin salto de layout, imagen y `scrim`
      recortados por el mismo recorte, degradación silenciosa sin imagen; controles flotantes intactos
      (≥48 dp, comportamiento igual).
- [ ] Alcance: `git diff --name-only` = `RecipeDetailScreen.kt` + `DESIGN.md` (+ `feature_list.json`/
      `PROGRESS.md` del orquestador); `ui/artwork/**`, `ui/theme/**`, resto de `ui/**`, `domain/**`,
      `data/**`, assets, build/lockfile, `init.sh`, `scripts/**`, ADR 0006 y `docs/design/concepts/**`
      intactos; sin dependencias nuevas.
- [ ] `bash init.sh` verde con evidencia registrada (comandos, conteos con delta vs `HEAD`, lint, greps,
      diff) y sin cambios en las reglas de verificación.
- [ ] Persistent E2E: no aplica (justificado en el plan de verificación).
- [ ] No se añade comportamiento ajeno a la feature (ni se cuadra la tira, ni se reabre
      `detail-hero-full-bleed`, ni se rediseñan pestañas/cuerpo/barra, ni se añaden capturas).
