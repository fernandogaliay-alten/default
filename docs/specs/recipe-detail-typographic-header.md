# Feature Implementation Spec: Hero tipográfico del Detalle con tira de metadatos

## Source Feature

- `id`: `recipe-detail-typographic-header`
- `area`: `ui`
- `depends_on`: `["recipe-card-typographic"]` (satisfecho: `accepted`, sesión 104)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Referencias de diseño: `docs/adr/0006-typographic-recipe-cards.md` (decisiones 2, 7 y "Consecuencias"),
  `DESIGN.md` (`Shapes`, `Core Screens` 5, `Components` → Imagen de receta) y las **capturas
  obligatorias** `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/` (aportadas el
  2026-10-14; ver `Visual Design Impact`).
- Es una **correctiva de `recipe-detail-screen`**: esa feature (aceptada) definió el hero con
  `RecipeArtwork` + la **tarjeta blanca superpuesta**; aquí se retira ese recurso.

## Goal

Abrir el Detalle con un **hero tipográfico** (ADR 0006, dirección C3): **categoría en versalitas**,
**título** en Manrope, los **tres ingredientes principales con su cantidad** en cifras tabulares, la
**marca de agua** del glifo de categoría al 12 % y el **filete de 6 px** con el degradado de la cocina
—**sin ninguna foto**—, seguido inmediatamente de la **tira de metadatos** (`Tiempo · Dificultad ·
Comensales · Calorías`) que ocupa el lugar de la tarjeta superpuesta.

El resto del Detalle **no cambia**: el escalado de raciones (`ServingScaler`), las pestañas
(Ingredientes · Pasos · Notas), el checklist con cantidades, la barra inferior ("Guardar" / "Empezar a
cocinar") y el host overlay de `MainShell` quedan exactamente como están. La feature solo sustituye el
bloque superior (hero + tarjeta solapada) por hero tipográfico + tira de metadatos.

## Non-Goals

- **No** se integra la **capa de fondo** con imagen generada (variante L2 del hero): es
  `dish-artwork-as-card-background`. Aquí el hero es **siempre tipográfico** y **no** consulta la caché,
  el set semilla ni el generador (ni lee el interruptor de imágenes del plato).
- **No** se retiran los assets de fotos ni la cadena local (`data.RecipePhotoAssets`,
  `data.AssetRecipePhotoSource`, `ui/artwork/RecipePhotoSource`, `RecipeArtwork`): es
  `retire-recipe-photos`. `RecipeArtwork` **no se borra ni se modifica**: en esta feature se queda
  **sin consumidores** (lo volverá a consumir `dish-artwork-as-card-background` como capa de fondo).
- **No** se toca `RecipeArtwork`, `RecipeArtworkStyle`, `RecipeArtworkPrimitives`, `RecipePhotoSource`
  ni el resto de `ui.artwork` (solo se **consumen** `RecipeGlyphArtwork`, `recipeAccentBrush`,
  `RecipeAccent`/`RecipeGlyph` de forma aditiva y sin editarlos).
- **No** cambian el escalado de raciones, el estado del Detalle, el formateo de cantidades, las
  pestañas, el timeline de pasos, la nutrición, el checklist, el `ServingsStepper`, la barra inferior
  ni el modo cocina (verificación explícita de la feature).
- **No** se cambian `RecipeDetailFormat` (los cuatro valores de la tira siguen siendo los mismos,
  incluido `"480 kcal"` y el `"—"` sin nutrición) ni sus tests.
- **No** se añaden tokens de tema, estilos a `SazonTypography`, hex sueltos, iconos, fuentes, assets ni
  **dependencias nuevas**; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh` y `scripts/**`
  quedan intactos.
- **No** se reordena ni rediseña el resto de la pantalla (pestañas, stepper y checklist conservan su
  implementación vigente aunque la captura los muestre con otro contenedor; ver "Desviaciones
  declaradas").
- **No** se decide la pregunta abierta **OQ7** (si el hero conserva el set semilla de imágenes):
  el hero queda tipográfico.

## Job Story

Cuando abro una receta desde Inicio, Resultados, Favoritos o la búsqueda con IA,
quiero ver de un vistazo qué plato es y qué lleva (con la marca de agua y el filete de la cocina), sin
una foto genérica que no corresponda al plato,
para decidir si la cocino sin que la imagen me confunda.

## Users And Permissions

- **Usuario local**: abre el Detalle, ajusta raciones, cambia de pestaña y vuelve con "Atrás"; sin
  permisos ni red.
- **Desarrollador / agente**: cubre en JVM el contenido del hero y los valores de la tira; el acabado lo
  revisa la persona contra las capturas en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: sin foto, sin caché y sin generador en el hero; la receta ya viene validada.

## Acceptance Scenarios

### Scenario 1: el hero tipográfico resume la receta (sin foto)

Given una receta con categoría, cocina y al menos tres ingredientes no-despensa,
When se abre el Detalle,
Then el hero muestra, en este orden: **categoría en versalitas** (`"Arroz · Latina"`, `RecipeCardFormat.
categoryLine`), **título** (Manrope Bold, hasta 2 líneas) y los **tres ingredientes principales con su
cantidad** (`400 g`, `1 ud`, `"Al gusto"`), con el nombre a la izquierda y la cantidad a la derecha en
cifras tabulares, un **hairline** bajo cada fila, la **marca de agua** del glifo de la categoría al 12 %
en la esquina inferior derecha y el **filete de 6 px** al pie del hero; **no** hay ninguna foto ni
placeholder de imagen.

### Scenario 2: geometría del hero conservada

Given cualquier receta,
When se pinta el hero,
Then mide **240 dp** de alto, la marca de agua **190 dp** al **12 %** de opacidad y el filete **6 dp**;
el hero va a sangre dentro del área de contenido con **margen lateral** de `SazonSpacing.md` (16 dp) y
**esquinas inferiores** de radio 24 dp (`SazonShapes.medium`), con las superiores rectas, y su contenido
está alineado al pie (si el título crece, crece hacia arriba, nunca se recorta).

### Scenario 3: la tarjeta superpuesta desaparece y la tira de metadatos ocupa su lugar

Given el Detalle abierto,
When se recorre la parte superior de la pantalla,
Then **no** existe ninguna tarjeta `surface` superpuesta (ni el desplazamiento
`offset(y = -SazonSpacing.lg)`) y, justo debajo del hero, hay una **tira de metadatos** con los **cuatro
pares** `Tiempo` (`"30 min"`), `Dificultad` (etiqueta real, respaldo al `id`), `Comensales`
(`servings`) y `Calorías` (`"480 kcal"`, `"—"` sin nutrición), con etiqueta en versalitas y valor en
Manrope tabular; el **título** ya se lee en el hero y la **descripción de la receta deja de pintarse**
(ver Scenario 8).

### Scenario 4: a receta sin datos, el hero no rompe

Given una receta con **menos de tres** ingredientes, o **solo** ingredientes `isPantry`, o **sin**
ingredientes, o con `categoryId`/`cuisineId` **desconocidos**,
When se pinta el hero,
Then pinta **los que haya** (hasta 3, en el orden de declaración, sin `Random`), la línea de categoría
cae al propio `id`, el glifo cae a `RecipeGlyph.PLATE` y el acento a `RecipeAccent.DEFAULT` (fallback
total de `RecipeArtworkStyle`), y **nunca** hay excepción, filas vacías ni hueco en blanco.

### Scenario 5: claro y oscuro con roles del tema

Given `darkTheme = true`,
When se pinta el hero y la tira,
Then el relleno del hero es el **degradado de la cocina** (`recipeAccentBrush` → roles
`*Container` oscuros), la marca de agua se tiñe con `onSurfaceVariant` al 12 %, los textos usan
`onSurface`/`onSurfaceVariant`, la tira usa `surface` y sus separadores el `outline` del tema; **no**
aparece ningún hex suelto fuera de `ui/theme`.

### Scenario 6: el escalado de raciones, las pestañas y la barra inferior no cambian (verificación exigida)

Given el Detalle abierto,
When el usuario ajusta comensales con `− N +` o cambia de pestaña,
Then las cantidades del **checklist** se recalculan con `ServingScaler` y la caption "Cantidades
ajustadas automáticamente" sigue visible; las tres pestañas (`RecipeDetailTab.entries`) y la barra
inferior (`OutlinedButton "Guardar"`/`"Guardada"` + `Button "Empezar a cocinar"` que abre el modo
cocina) siguen **exactamente** como hoy, y el estado (`RecipeDetailState` con su `Saver`) no cambia.

### Scenario 7: el Detalle sigue abriéndose y cerrándose igual

Given el Detalle abierto desde la Home, Resultados, Favoritos o la búsqueda con IA,
When el usuario pulsa "Atrás" o "Guardar",
Then se cierra/guarda con las costuras vigentes (`onBack`, `onToggleSave`) sin que `MainShell` ni el
host overlay cambien; el `RecipeDetailScreen` conserva su **firma** (`recipe`, `taxonomy`, `isSaved`,
`onToggleSave`, `onStartCooking`, `onBack`, `modifier`).

### Scenario 8: la descripción deja de pintarse (consecuencia declarada)

Given una receta con `description` no vacía,
When se abre el Detalle,
Then su descripción **no** aparece en pantalla: la captura aceptada y la sección `Core Screens` 5 de
`DESIGN.md` describen el Detalle como hero tipográfico + tira de metadatos, sin tarjeta ni descripción.
Es una consecuencia **deliberada y registrada** (ver "Decisiones y desviaciones"); si la persona la
quiere de vuelta, se pide una feature correctiva (no se improvisa aquí).

## Contrato de tests (JVM, sin emulador)

> El repo **no** tiene `androidx.compose.ui.test` en `testImplementation` (solo en `androidTest`), así
> que **no** se planifican tests de UI Compose ni Robolectric de composición: la verificación JVM es de
> **contenido y geometría pura** (patrón vigente de `ui.detail`) y el acabado se revisa contra las
> capturas. No se añade ninguna dependencia para testear.

- **Nuevo** `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailHeroFormatTest.kt` (JVM puro, JUnit4,
  Taxonomía real vía `System.getProperty("taxonomy.asset.path")`, patrón de `RecipeDetailFormatTest`):
  - `of(taxonomy, receta)` devuelve `HeroContent` con la línea de categoría real (`"Arroz · Latina"` con
    ids reales), el título y **hasta 3** ingredientes con **nombre + `quantityLabel`** (`"400 g"`,
    `"6 ud"`, `"Al gusto"`), excluyendo despensa y respetando el orden de declaración.
  - determinismo: dos llamadas iguales devuelven el mismo contenido.
  - negativos **no vacíos**: receta vacía → lista vacía sin excepción; 2 ingredientes → 2; solo despensa
    → se completa a 3; `categoryId`/`cuisineId` desconocidos → respaldo al `id` en la línea.
  - geometría publicada: `HERO_HEIGHT_DP == 240`, `FILLET_HEIGHT_DP == 6`, `WATERMARK_DP == 190`,
    `WATERMARK_ALPHA == 0.12f`, `MAIN_INGREDIENTS == 3` y `== RecipeCardFormat.MAIN_INGREDIENTS`.
- **Sin cambios** (deben seguir verdes tal cual): `RecipeDetailFormatTest` (los **cuatro valores** de la
  tira, con y sin nutrición, dificultad real y respaldo al `id`), `ServingScalerTest`,
  `IngredientFormatTest`, `RecipeDetailStateTest`.
- **Inspección** (lo que la JVM no puede aseverar): `grep -rn "RecipeArtwork(\|RecipePhotoSource" 
  app/src/main/java/com/sazon/app/ui/detail` → **0** (`RecipeArtworkStyle` **sí** se consume: es el mapeo
  puro glifo/acento que exige el Approach; lo que desaparece es la **llamada** a la imagen
  `RecipeArtwork(`); `RecipeDetailScreen.kt` no contiene `DetailCard`
  ni `offset(y = (-SazonSpacing.lg))`; `grep -rn "0xFF" app/src/main/java/com/sazon/app --include=*.kt
  | grep -v ui/theme` → solo las **dos máscaras de bytes preexistentes**.

## Repository Research

### Files Inspected

- `feature_list.json` (líneas 1276-1291) — alcance, verificación (tests de estado del Detalle con la
  geometría del hero y los cuatro valores de la tira, sin foto; sin mover escalado/pestañas/barra) y
  notas; las vecinas `recipe-card-typographic` (`accepted`) y `dish-artwork-as-card-background`
  (`not_started`, dueña de la capa de fondo).
- `PROGRESS.md` (sesión 104) — `recipe-card-typographic` aceptada; siguiente feature y capturas ya
  aportadas; gate `bash init.sh` sin procesos largos; WIP = 0.
- `AGENTS.md` — verificación de UI por lógica/estado en JVM + capturas obligatorias; nada de emulador
  automatizado; una feature por worktree.
- `DESIGN.md` — `Shapes` (la viñeta de "superficies superpuestas … la tarjeta blanca que sube sobre la
  foto del detalle" **contradice** el ADR 0006 y hay que corregirla), `Core Screens` 5 (ya describe
  hero tipográfico + tira de metadatos y "ya no hay tarjeta blanca"), `Components` → Imagen de receta
  (L2, marca de agua 12 %, filete 6 px) y `Accessibility Baseline` (objetivos ≥48 dp).
- `docs/adr/0006-typographic-recipe-cards.md` — decisión 2 (la imagen, cuando exista, es capa de fondo
  del hueco), decisión 7 (`RecipeArtwork` cambia de rol) y "En contra": el Detalle pierde el solape y
  "se sustituye por hero tipográfico + tira de metadatos".
- `docs/design/concepts/recipe-detail-typographic-header/README.md` y `detalle-receta/{screen.png,
  screen-dark.png,code.html,DESIGN.md}` — capturas **aportadas**; geometría, microcopy y "Qué mirar".
- `docs/design/concepts/typographic-cards/prototipo-c3.html` (Detalle, líneas 400-443; CSS 130-160 y
  141-145; hoja de componentes 563 y 294) y `comparativa-tipografica.html` (líneas 234-247) — los
  valores exactos del hero y de la tira.
- `docs/design/concepts/dish-artwork-as-card-background/{README.md,detalle-con-imagen/DESIGN.md}` — la
  variante con imagen (fuera de alcance) conserva **el mismo** hero de 240 y la misma tira.
- `docs/specs/recipe-detail-screen.md` y `docs/specs/recipe-photos.md` — lo aceptado que esta feature
  corrige (hero con `RecipeArtwork` 240 + tarjeta solapada con título/descripción/meta) y lo que **no**
  debe moverse (escalado, pestañas, barra, costuras).
- `docs/specs/recipe-card-typographic.md` — el precedente directo: ficha tipográfica, marca de agua,
  filete, `RecipeCardFormat`, estilo local Manrope y capturas.
- `app/src/main/java/com/sazon/app/ui/detail/{RecipeDetailScreen,RecipeDetailFormat,RecipeDetailState,
  ServingScaler,IngredientFormat}.kt` — estado actual: hero `RecipeArtwork` 240 + `DetailCard`
  (`surface`, `SazonShapes.large`, título `headlineLarge`, descripción, `MetaStrip` en `surfaceVariant`)
  y `Column(offset(y = -SazonSpacing.lg))`.
- `app/src/main/java/com/sazon/app/ui/home/{RecipeFicha,RecipeCardFormat,RecipeMonogram}.kt` — contenido
  compartido de la ficha, `categoryLine`/`mainIngredients`/`MainIngredient` puros y el patrón de
  estilos tipográficos locales (`FichaTypography`).
- `app/src/main/java/com/sazon/app/ui/artwork/{RecipeArtwork,RecipeArtworkPrimitives,RecipeArtworkStyle}.kt`
  — `RecipeArtworkStyle.of(recipe)` (glifo/acento), `RecipeGlyphArtwork(glyph, tint, modifier)` y
  `recipeAccentBrush(accent)`/`recipeAccentColors(accent)`; `RecipeArtwork` es hoy el **único** punto a
  sustituir.
- `app/src/main/java/com/sazon/app/ui/theme/{Type,Spacing,Shape,CardTokens}.kt` — `SazonFontFamilies.
  Display` (Manrope)/`Body` (Inter), `SazonSpacing` (sm/md/lg), `SazonShapes.medium` (24 dp),
  `SazonCardTokens`/`LocalSazonCardColors` (cromo **de tarjeta**, para la ficha, no para el hero).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` (líneas 271-283) — host overlay y cableado de
  `isSaved`/`onToggleSave`/`onStartCooking`/`onBack`; **no** necesita cambios.
- `app/src/test/java/com/sazon/app/ui/detail/*Test.kt` — patrón de los tests puros del Detalle.
- `app/build.gradle.kts` — `robolectric` y `androidx.test.core` en `testImplementation`, pero
  `androidx.compose.ui.test.junit4` **solo** en `androidTestImplementation`: sin tests de composición.
- `docs/technical-discovery.md` (323-339) — el párrafo del Detalle que hay que actualizar.

### Existing Patterns To Follow

- **Lógica/formato puro + capa Compose delgada**: los objetos de `ui.detail` no importan Compose/Android
  y se testean en JVM; los `@Composable` solo pintan y delegan.
- **Reutilizar, no duplicar**: `RecipeCardFormat.categoryLine`/`mainIngredients` (puros) y `ui.artwork`
  para glifo y degradado. `ui.artwork` sigue siendo la **única** fuente de dibujo.
- **Roles del tema, cero hex** fuera de `ui/theme`; **cifras tabulares** (`fontFeatureSettings = "tnum"`)
  en cantidades, comensales y valores de la tira.
- **Estilos tipográficos locales** desde `SazonFontFamilies` (como `FichaTypography`): el tema solo
  overridea 4 estilos, así que `titleSmall`/`labelSmall`/`headlineLarge` **no** usan Manrope donde se
  espera.
- **Sin librería de navegación ni dependencias nuevas**; `init.sh` sin cambios.
- Tests JUnit4 en español con casos **negativos no vacíos** y determinismo.

### Current Gaps

- El Detalle pinta `RecipeArtwork` (foto) en el hero y una tarjeta `surface` superpuesta con título,
  descripción y `MetaStrip` en `surfaceVariant`: es exactamente lo que el ADR 0006 retira.
- No existe ninguna lógica pura que describa el **contenido del hero** ni sus **constantes de
  geometría**: la verificación de la feature (geometría + sin foto) no es aseverable hoy.
- `DESIGN.md` → `Shapes` sigue citando la tarjeta superpuesta del Detalle como recurso vigente.

## Technical Approach

1. **Nuevo formato puro** `object RecipeDetailHeroFormat` en `com.sazon.app.ui.detail` (sin Compose ni
   Android): publica la **geometría** del hero como constantes (`HERO_HEIGHT_DP = 240`,
   `FILLET_HEIGHT_DP = 6`, `WATERMARK_DP = 190`, `WATERMARK_ALPHA = 0.12f`,
   `MAIN_INGREDIENTS = RecipeCardFormat.MAIN_INGREDIENTS`) y el **contenido** con
   `data class HeroContent(val category: String, val title: String, val ingredients: List<MainIngredient>)`
   y `fun of(taxonomy, recipe): HeroContent` compuesto de `RecipeCardFormat.categoryLine(taxonomy,
   recipe)` + `RecipeCardFormat.mainIngredients(recipe)` (reutiliza la regla de despensa/orden/tope ya
   aceptada; reutilizar `ui.home` desde `ui.detail` tiene precedente inverso: `RecipeCardFormat` usa
   `ui.detail.IngredientFormat`). La capa Compose convierte `_DP` a `dp`.
2. **Reescribir el bloque superior de `RecipeDetailScreen.kt`**:
   - Sustituir `DetailHero` por el hero tipográfico: `Box` de `HERO_HEIGHT_DP.dp` con
     `Modifier.fillMaxWidth().padding(horizontal = SazonSpacing.md).clip(esquinas inferiores 24 dp)`,
     relleno `recipeAccentBrush(RecipeArtworkStyle.of(recipe).accent)`, marca de agua
     `RecipeGlyphArtwork(glyph, onSurfaceVariant @ 12 %, 190 dp)` alineada al pie/derecha desbordando
     (offset derecho +24 dp, abajo +6 dp, recortada por el `clip`), y `Column` alineada al pie
     (`Arrangement.Bottom`) con **categoría** (10-11 sp, bold, `letterSpacing = 0.14.em`,
     `uppercase()`, `onSurfaceVariant`), **título** (Manrope Bold 29 sp, `lineHeight` ≈ 31 sp,
     `letterSpacing = -0.025.em`, `onSurface`, `maxLines = 2`, elipsis) y **tres filas** de ingrediente
     (nombre 12,5 sp `onSurfaceVariant` `maxLines = 1` + cantidad 12,5 sp Medium tabular `onSurface`,
     hairline `outline` bajo cada fila, incluida la última). El **filete de 6 dp**
     (`Brush.horizontalGradient(recipeAccentColors(accent))`) va sobre el borde inferior del hero.
   - **Conservar** el `Row` flotante con "Atrás" (píldora `surface`) y el corazón/guardar circular
     (`Surface` 48 dp, `HeartGlyph`) tal cual están hoy: `DESIGN.md` exige objetivos ≥48 dp y los
     círculos de 34 px de la captura son dirección, no especificación pixel-perfect.
   - **Eliminar** `DetailCard` y el `offset(y = -SazonSpacing.lg)`; el `MetaStrip` sigue
     inmediatamente al hero en el `Column` sin solape.
   - Rehacer `MetaStrip` como bloque propio según la captura: `Surface` con `color = surface` y
     **esquinas inferiores** a 24 dp, margen lateral `SazonSpacing.md`, mismo ancho que el hero, sombra
     suave (sin bordes duros), **cuatro columnas** `weight(1f)` centradas separadas por un hairline
     vertical (`outline`, no tras la última), etiqueta en versalitas (`label` de `metaEntries`,
     `uppercase()`, 9,5 sp, `letterSpacing = 0.1.em`, `onSurfaceVariant`) y valor en **Manrope Bold**
     14 sp tabular (`onSurface`). Sigue alimentándose de `RecipeDetailFormat.metaEntries(taxonomy,
     recipe)` (**sin** tocar ese objeto ni sus valores) y mantiene `SazonSpacing.sm`/`md` como aire.
   - El resto del `Column` (pestañas `DetailTabs`, cuerpo por pestaña, `DetailBottomBar`, `HeartGlyph`,
     `RecipeDetailStateSaver`) se mantiene **byte a byte** salvo el hueco del `offset` retirado.
3. **Sin cambios** en `RecipeDetailFormat`, `RecipeDetailState`, `ServingScaler`, `IngredientFormat`,
   `ui/artwork/**`, `ui/home/**`, `MainShell`, `ServingsStepper`, `ui/theme/**`, dominio, datos,
   build/gradle, `init.sh` ni `scripts/**`.
4. **Tests**: nuevo `RecipeDetailHeroFormatTest` (contrato de arriba) y gate completo.
5. **Docs durables**: `docs/technical-discovery.md` (párrafo del Detalle: hero tipográfico, tira de
   metadatos, `RecipeDetailHeroFormat`, sin foto), `docs/adr/0006` (Estado: tercera feature
   implementada; §7: el hero del Detalle ya no consume `RecipeArtwork`, que queda sin consumidores hasta
   `dish-artwork-as-card-background`) y `DESIGN.md` → `Shapes` (retirar la viñeta de la tarjeta
   superpuesta del Detalle y sustituirla por el apilado hero + tira como recurso de profundidad sin
   sombras fuertes).

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailHeroFormat.kt` — **create**; contenido puro del
  hero (`HeroContent`) y constantes de geometría.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — **modify**; hero tipográfico,
  fuera `DetailCard` y el `offset`, `MetaStrip` como bloque propio; el resto intacto.
- `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailHeroFormatTest.kt` — **create**; contrato de
  tests del hero (incl. negativos y geometría).
- `docs/technical-discovery.md` — **modify**; párrafo del Detalle (323-339).
- `docs/adr/0006-typographic-recipe-cards.md` — **modify**; Estado (§ tercera feature) y nota de §7.
- `DESIGN.md` — **modify**; `Shapes` deja de citar la tarjeta superpuesta del Detalle.
- `feature_list.json`, `PROGRESS.md` — los actualiza el **orquestador** al cerrar (no el implementer).

**Debe quedar intacto** (disciplina de alcance): `app/src/main/java/com/sazon/app/ui/detail/
{RecipeDetailFormat,RecipeDetailState,ServingScaler,IngredientFormat}.kt`,
`app/src/test/java/com/sazon/app/ui/detail/{RecipeDetailFormat,ServingScaler,IngredientFormat,
RecipeDetailState}Test.kt`, `ui/artwork/**`, `ui/home/**`, `ui/results/**`, `ui/favorites/**`,
`ui/cooking/**`, `ui/preferences/**`, `ui/MainShell.kt`, `ui/theme/**`, `domain/**`, `data/**`,
`app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `app/src/main/assets/**`,
`docs/domain/taxonomy.json`, `seed-recipes.json`, `docs/design/concepts/**` (las capturas se **leen**,
no se editan).

## Visual Design Impact

- UI involved: **sí** (`area: ui`).
- Design source: `DESIGN.md` (`Shapes`, `Core Screens` 5, `Components` → Imagen de receta,
  `Accessibility Baseline`) + `docs/adr/0006-typographic-recipe-cards.md` + capturas.
- Screens or states affected: **Detalle** (hero tipográfico + tira de metadatos), en **claro y oscuro**;
  el resto de la pantalla (pestañas, checklist, stepper, barra inferior) no cambia de diseño.
- New design artifact required: **no** — capturas **ya aportadas** (2026-10-14; desviación de la
  política de capturas autorizada por la persona, sesión 101; origen: prototipos locales
  `docs/design/concepts/typographic-cards/`, no export de Stitch).
- **Capturas obligatorias (deben existir y revisarse antes de implementar)**:
  - `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/screen.png` (claro)
  - `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/screen-dark.png` (oscuro)
  - Apoyo: `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/code.html` (valores
    exactos del hero y de la tira) y `.../DESIGN.md` (qué mirar). Petición/ficha:
    `docs/design/concepts/recipe-detail-typographic-header/README.md`.
  - Ante cualquier contradicción manda `DESIGN.md` (y el ADR 0006); un defecto visual se corrige con
    feature correctiva.
- Feature-specific visual states: **ninguno nuevo** (no hay estado vacío/carga/error ni estado de
  imagen en el hero; la variante con imagen de fondo es `dish-artwork-as-card-background`).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — el párrafo del Detalle documenta el hero tipográfico
  (240 dp, degradado de cocina, marca de agua 12 %, filete 6 px), la tira de metadatos como bloque
  propio, `RecipeDetailHeroFormat` entre la lógica pura y que `RecipeArtwork` deja de consumirse ahí.
- `docs/adr/0006-typographic-recipe-cards.md`: **update** — `Estado` marca esta feature implementada;
  §7 y "Consecuencias" reflejan que el Detalle ya no usa `RecipeArtwork`.
- `DESIGN.md`: **update** — `Shapes` retira la viñeta de la tarjeta superpuesta del Detalle
  (contradicción vigente con `Core Screens` 5 y con el ADR 0006). Sin cambios de tokens ni frontmatter.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas del repo.
- `docs/risks-and-open-questions.md`: **not needed** — no nace un riesgo nuevo; la pérdida de la
  descripción y el contraste del hero en oscuro quedan registrados aquí como desviaciones menores.
- `docs/design/concepts/**`: **not needed** — las capturas ya están aportadas y registradas; no se crean
  ficheros nuevos ni se editan los existentes.

## Implementation Plan

1. Crear `RecipeDetailHeroFormat` (constantes + `HeroContent` + `of`) reutilizando `RecipeCardFormat`.
2. Escribir `RecipeDetailHeroFormatTest` con vectores reales de la Taxonomía y casos negativos.
3. Reescribir el bloque superior de `RecipeDetailScreen` (hero tipográfico + tira), quitar `DetailCard`
   y el `offset`, conservar "Atrás"/corazón, pestañas, cuerpo y barra inferior.
4. Revisar el acabado contra las dos capturas (claro y oscuro) y los valores de `code.html`.
5. Ejecutar el gate (`bash init.sh`) y los greps de alcance.
6. Actualizar `docs/technical-discovery.md`, `docs/adr/0006` y `DESIGN.md` (`Shapes`); registrar
   evidencia.

## Implementation Tasks

- [ ] `RecipeDetailHeroFormat`: `HERO_HEIGHT_DP`, `FILLET_HEIGHT_DP`, `WATERMARK_DP`,
      `WATERMARK_ALPHA`, `MAIN_INGREDIENTS`, `HeroContent`, `of(taxonomy, recipe)`.
- [ ] `RecipeDetailHeroFormatTest` (JVM): categoría real, título, 3 ingredientes con cantidad,
      determinismo, receta vacía, 2 ingredientes, solo despensa, ids desconocidos, geometría.
- [ ] Hero tipográfico en `RecipeDetailScreen` (240 dp, margen lateral 16, esquinas inferiores 24,
      degradado de cocina, marca de agua 190 dp @12 %, categoría en versalitas, título Manrope 29 sp,
      tres filas con cantidad tabular, filete 6 dp).
- [ ] Conservar "Atrás" (píldora) y el corazón/guardar de 48 dp sin cambios de comportamiento.
- [ ] Eliminar `DetailCard` y el `offset(y = -SazonSpacing.lg)`; dejar el `MetaStrip` como bloque
      propio con la captura (surface, esquinas inferiores 24, 4 columnas con hairline vertical,
      etiqueta en versalitas, valor Manrope tabular) alimentado por `RecipeDetailFormat.metaEntries`.
- [ ] Confirmar que pestañas, checklist, `ServingsStepper`, `DetailBottomBar` y el `Saver` no cambian.
- [ ] `bash init.sh` verde + greps de alcance (`RecipeArtwork` en `ui/detail` → 0; hex fuera de
      `ui/theme` → solo los 2 preexistentes) + `git status` = Expected File Changes.
- [ ] Actualizar `docs/technical-discovery.md` (Detalle), `docs/adr/0006` (Estado y §7) y `DESIGN.md`
      (`Shapes`).

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante actual: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos); `init.sh` **no** cambia y no arranca servicios.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.detail.*"
  --rerun-tasks` → `BUILD SUCCESSFUL`.
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: `RecipeDetailHeroFormatTest`
  nuevo en verde y **0 fallos/errores/saltados**; los cuatro tests existentes de `ui.detail` verdes y
  **sin diff**; re-derivar la baseline en `HEAD` y registrar el delta. Baseline de partida conocida:
  **95 suites / 720 tests** (sesión 104), de los cuales `ui.detail` = 4 suites
  (`ServingScalerTest`, `IngredientFormatTest`, `RecipeDetailFormatTest`, `RecipeDetailStateTest`).
- Lint (`app/build/reports/lint-results-debug.sarif`): **0 errores** y ningún hallazgo nuevo bajo
  `app/src` (los 13 warnings de entorno preexistentes se mantienen).
- Vectores JVM a comprobar (Taxonomía y semillas reales): categoría `"Arroz · Latina"`
  (`categoryId = rice`, `cuisineId = latin`) y `"Huevos y desayunos · Española"` (tortilla,
  `egg_breakfast`/`spanish`); ingredientes de la tortilla semilla (`Patata 600 g`, `Huevo 6 ud`,
  `Cebolla 1 ud`, sin sal ni aceite); `"Al gusto"` para `unit = null`; geometría `240/6/190/0.12f/3`;
  determinismo.
- Disciplina de alcance por inspección: `git diff --name-only` = Expected File Changes (más la spec y lo
  que actualice el orquestador); `grep -rn "RecipeArtwork(\|RecipePhotoSource"
  app/src/main/java/com/sazon/app/ui/detail` → **0** (**0** también para la llamada `RecipeArtwork(` en
  todo `ui/**`: queda sin consumidores); sin `DetailCard` ni `offset(y = (-` en
  `RecipeDetailScreen.kt`; sin `0xFF` nuevos fuera de `ui/theme`; `test -f` de las dos capturas → **OK**.
- **E2E / UI automatizada: no aplica.** No hay suite E2E persistente y `AGENTS.md` prohíbe automatizar
  UI de emulador; el contenido y la geometría se cubren en JVM y el acabado (filete, marca de agua,
  tipografía, claro/oscuro, esquinas y sombras) lo revisa la persona tras el merge contra
  `detalle-receta/{screen,screen-dark}.png`.
- Manual (persona, emulador; no automatizable desde WSL): abrir una receta (semilla y de IA) en claro y
  oscuro, comprobar que el hero no tiene foto, que la tira muestra los cuatro valores, y que el
  stepper, las pestañas y la barra inferior se comportan igual que antes.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal; conteos de suites/tests con delta vs `HEAD`.
- Nombres y aserciones de `RecipeDetailHeroFormatTest` con sus **vectores** (categoría, tres
  ingredientes con cantidad, geometría, negativos) y confirmación de que los 4 tests previos de
  `ui.detail` siguen verdes sin cambios.
- Resultado de los greps de alcance y de `git diff --name-only`; informe de lint (SARIF).
- Confirmación de que las dos capturas existen y se revisaron, con las desviaciones declaradas de esta
  spec (descripción no pintada, `"480 kcal"` en la tira, controles ≥48 dp, contenedor de pestañas y
  stepper sin rediseñar).
- Registro en `docs/technical-discovery.md`, `docs/adr/0006` y `DESIGN.md` (`Shapes`).

## Decisiones y desviaciones declaradas

- **El hero no pinta foto alguna**: el Detalle deja de consumir `RecipeArtwork` (que **no se borra**:
  queda sin consumidores hasta `dish-artwork-as-card-background`). Es la decisión 2 del ADR 0006.
- **La descripción de la receta deja de pintarse** (Scenario 8): ni `DESIGN.md` → `Core Screens` 5 ni
  las capturas aceptadas (`detalle-receta`, `detalle-con-imagen`) la muestran; vivía en la tarjeta que
  esta feature retira. **Consecuencia de producto** documentada; si la persona la quiere de vuelta, es
  una feature correctiva (no se improvisa aquí).
- **Valores de la tira sin cambios** (`metaEntries`: `"30 min"`, etiqueta de dificultad, `servings`,
  `"480 kcal"`/`"—"`): el `code.html` de la captura dibuja el valor de calorías como `480` (sin
  unidad), pero el valor es contrato ya aceptado y **testeado** (`RecipeDetailFormatTest`) y la
  verificación de la feature pide mantener "sus cuatro valores". La UI solo aplica `uppercase()` a la
  etiqueta, sin tocar `RecipeDetailFormat`.
- **Controles "Atrás"/guardar**: la captura muestra círculos de 34 px; la app mantiene los de hoy
  (píldora + `Surface` 48 dp) porque `DESIGN.md` → `Accessibility Baseline` exige ≥48 dp y esta feature
  no cambia interacción. La captura es dirección, no especificación pixel-perfect.
- **Pestañas, stepper y checklist no se rediseñan**: la verificación de la feature exige que no cambien
  y `DESIGN.md` no fija el contenedor; el `code.html` los dibuja con otros contenedores (píldora de
  pestañas, filas en tarjeta `surface`), desviación **declarada** y no bloqueante.
- **El hero no pinta `matchScore`** (ni píldora ni texto): la captura y `Core Screens` 5 no lo incluyen;
  el Detalle nunca lo ha pintado en el hero (`RecipeDetailFormat` no lo usa).
- **Hero en oscuro sin borde**: el relleno del hero son los `*Container` oscuros de la cocina
  (sin `darkCardSurface`/`darkCardOutline`, que son tokens **de tarjeta** para la ficha) y el texto va
  en `onSurface`/`onSurfaceVariant` de alto contraste; la captura oscura no lleva borde.
- **Sin tests de composición**: añadir `androidx.compose.ui.test` a `testImplementation` sería una
  dependencia nueva; el contrato se cubre en JVM puro + capturas (justificado arriba).
- **Hallazgo de implementación (grep de alcance)**: el Approach exige
  `recipeAccentBrush(RecipeArtworkStyle.of(recipe).accent)` para el relleno del hero, así que
  `ui/detail` **sí** importa `RecipeArtworkStyle` (mapeo **puro** glifo/acento, sin Compose). El grep del
  plan de verificación se interpreta, por eso, como **0 llamadas a `RecipeArtwork(`** (la imagen) y **0**
  `RecipePhotoSource`, no como 0 menciones del texto "RecipeArtwork". `RecipeArtwork` (el `@Composable`
  de la foto) queda **sin ningún consumidor** en `ui/**`, como pide el non-goal; las tres líneas de
  inspección del plan y del checklist quedan reconciliadas arriba.

### Unknowns / Open Points

- Nombre y ubicación finales de `RecipeDetailHeroFormat`/`HeroContent` (provisionales): si se renombran,
  mantener el contrato y citarlo en la evidencia.
- Radio exacto de las esquinas inferiores y del hairline de la tira: se fijan en `SazonShapes.medium`
  (24 dp) y `outline`; son detalle visual revisable contra la captura, no cambian el contrato JVM.
- Contraste del hero contra `darkBackground` en oscuro (~1,5:1 con los `*Container` oscuros): no
  bloqueante (la captura aceptada no lleva borde y el contenido es de alto contraste); si la revisión
  visual lo pide, se corrige con feature correctiva.
- OQ7 (set semilla en el hero del Detalle) sigue **abierto**; esta feature lo deja tipográfico.

## Validator Checklist

- [ ] El hero es **tipográfico** y sin foto: `grep -rn "RecipeArtwork(\|RecipePhotoSource" 
      app/src/main/java/com/sazon/app/ui/detail` → **0** (el mapeo puro `RecipeArtworkStyle` sí se consume;
      la **llamada** a `RecipeArtwork(` no aparece en `ui/**`), y no consulta caché/generador/interruptor.
- [ ] El hero conserva su **geometría** (240 dp, marca de agua 190 dp @12 %, filete 6 dp) y su
      contenido (categoría, título, hasta 3 ingredientes con cantidad), verificado en JVM con casos
      negativos no vacíos y contra `detalle-receta/{screen,screen-dark}.png`.
- [ ] La **tarjeta superpuesta** desapareció (sin `DetailCard`, sin `offset(y = -…)`) y la **tira de
      metadatos** conserva sus **cuatro valores** (`RecipeDetailFormatTest` intacto y verde).
- [ ] **No** cambian el escalado de raciones, las pestañas ni la barra inferior (Guardar / Empezar a
      cocinar); `ServingScaler`, `RecipeDetailState`, `ServingsStepper`, `DetailBottomBar`, el `Saver`
      y el host overlay de `MainShell` quedan intactos (`git diff` lo demuestra).
- [ ] Sin dependencias nuevas, sin tokens/estilos de tema nuevos, sin hex fuera de `ui/theme`, sin
      assets nuevos; `init.sh`, `scripts/**`, build/gradle, dominio, datos y `ui/artwork/**` intactos.
- [ ] Las capturas de `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/` existen,
      se revisaron y la implementación las sigue; las desviaciones declaradas (descripción no pintada,
      `"480 kcal"`, controles ≥48 dp, pestañas/stepper sin rediseñar) están registradas.
- [ ] `bash init.sh` verde con la evidencia (comandos, conteos, vectores, greps) y sin cambios en las
      reglas de verificación.
- [ ] Docs durables actualizados (`docs/technical-discovery.md`, `docs/adr/0006`, `DESIGN.md` →
      `Shapes`); `feature_list.json`/`PROGRESS.md` los actualiza el orquestador.
- [ ] Persistent E2E: no aplica (justificado en el plan de verificación).
- [ ] No se añade comportamiento ajeno a la feature (ni capa de fondo L2, ni retirada de fotos, ni
      rediseño del resto de la pantalla).
