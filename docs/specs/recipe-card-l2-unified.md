# Feature Implementation Spec: Tarjetas de receta con la estructura L2 unificada (con o sin imagen)

## Source Feature

- `id`: `recipe-card-l2-unified`
- `area`: `ui`
- `depends_on`: `["recipe-card-typographic", "dish-artwork-as-card-background"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (issue **#129**)
- Referencias de diseño: `DESIGN.md` (Overview; `components.recipe-card`,
  `components.recipe-card-image`; sección `Components` → RecipeCard; `Core Screens` 4/6; OQ6
  **resuelta**), `docs/adr/0006-typographic-recipe-cards.md` y las capturas **ya aceptadas** de
  `recipe-card-typographic` y `dish-artwork-as-card-background`.
- **Exención de capturas autorizada** (2026-10-14, issue #129; misma clase que #77/#85/#86/#91/#98/
  #101/#110, registrada en `DESIGN.md`): esta feature **aplica** la variante **L2 ya aceptada** y
  **retira** la estructura no usada (hueco **16:10** sin ajuste). **No** se crea ni se exige
  `docs/design/concepts/recipe-card-l2-unified/`. La feature **no** queda bloqueada por capturas.

## Goal

Unificar las **dos** estructuras de `RecipeFicha` en **una sola** (la variante **L2** del ADR 0006) para
que la tarjeta de receta se vea **igual** con el ajuste "Generar imágenes del plato" encendido o
apagado: **hueco 16:9** con la **categoría y el título encima** (más la marca de agua del glifo y el
filete de 6px) y, **debajo**, la **banda sólida** con los **tres ingredientes principales con cantidad**
y la **fila de metadatos** con la coincidencia. Con el ajuste apagado —o cuando no hay imagen— solo
**desaparece la capa de fondo**; el layout **no** cambia. Se **retira** la estructura no usada (hueco
**16:10** con todo el contenido dentro del hueco), con lo que desaparece la dualidad 16:10/16:9 y con
ella la **OQ6**.

Afecta a las tres variantes de `RecipeFicha` y a sus tres consumidores de tarjeta: **Inicio**
(`RecipeCard`: carrusel "Sugeridas para ti" y bloque "Tus recetas"/"Guardadas"), **Resultados**
(`ResultRecipeCard`) y la **rejilla de Favoritos** (`FavoriteRecipeTile`).

## Non-Goals

- **No** se toca la **capa de fondo** ni su mecánica: `ui.artwork.RecipeArtwork` (imagen + `scrim` +
  crossfade de 250 ms), `DishImageArtworkPolicy`, el **interruptor** (`DishImageSettings`) y el **tope
  por sesión** (`GatedDishImageGenerator`) se conservan **tal cual**. Sin imagen la degradación sigue
  siendo **silenciosa**.
- **No** se toca el **Detalle** (`RecipeDetailScreen`, hero 240 dp): es `recipe-detail-typographic-header`
  y su variante a sangre es `detail-hero-full-bleed`.
- **No** se toca la **tarjeta de lista de Favoritos** (`RecipeMonogramTile` 96×96, monograma + regla
  terracota de 5px): es `favorites`/`recipe-card-typographic`; el color de la miniatura es
  `favorites-tile-contrast`.
- **No** se altera el **bloque "Tus recetas"** de la Home (chips, 6 últimas, "Ver todas"): es
  `home-tus-recetas-ver-todas`. Tampoco el resto de Home, Resultados (chips, cabecera, estados,
  modo semilla) ni Favoritos (buscador, orden, toggle lista/rejilla, swipe, corazón).
- **No** hay pantalla, ruta, microcopy, estado, token, tipografía ni asset nuevos. **Sin** dependencias
  nuevas; **sin** cambios en `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `assets/**`,
  `AndroidManifest.xml`, `domain/**`, `data/**`.
- **No** se crean ni modifican capturas bajo `docs/design/concepts/**` (exención de #129); en
  particular **no** se crea `docs/design/concepts/recipe-card-l2-unified/`.
- **No** se cambia ninguna regla de verificación ni el gate (`init.sh`).

## Job Story

Cuando hojeo recetas en Inicio, Resultados o la rejilla de Favoritos,
quiero que la tarjeta se lea siempre igual (hueco con categoría y título, y debajo la banda con los
ingredientes y los metadatos) sin que el interruptor de imágenes cambie la forma de la tarjeta,
para reconocer la ficha de un vistazo y no depender de una preferencia para que el diseño sea coherente.

## Users And Permissions

- **Usuario local**: ve las tarjetas en Inicio, Resultados y Favoritos (claro y oscuro). No interactúa
  con nada nuevo; el interruptor sigue en Perfil y **no** cambia de comportamiento.
- **Desarrollador / agente**: factoriza la estructura en `RecipeFicha`, extrae la regla pura de la
  banda y verifica en **JVM** (sin emulador, sin red, sin Robolectric).
- **Persona**: revisa el acabado en el emulador tras el merge (AGENTS.md no automatiza UI), contra
  `DESIGN.md` y las capturas aceptadas de referencia.

## Acceptance Scenarios

### Scenario 1: ajuste apagado = misma estructura L2, sin capa de fondo

Given el ajuste "Generar imágenes del plato" **apagado** y una receta con categoría, cocina e
ingredientes,
When se pinta `RecipeCard` en el carrusel "Sugeridas para ti" (~220 dp de ancho),
Then la tarjeta tiene **un único** hueco **16:9** (`DishImageArtworkOverlay.HOLE_RATIO`) con la
categoría en versalitas y el título (máx. 2 líneas) **dentro** del hueco, la **marca de agua** del
glifo al 12 % y el **filete de 6 px** con el degradado de la cocina al pie; **debajo** del hueco, la
**banda sólida** con los tres ingredientes principales **en una línea** separados por `·` y la fila
`tiempo · dificultad · comensales` con la coincidencia en terracota (solo si `matchScore` no es
`null`); **no** se compone `RecipeArtwork` (0 lecturas de caché/semilla/generador) y el cromo
(superficie, borde y tinte de la cocina en oscuro) no cambia.

### Scenario 2: ajuste encendido + imagen = solo aparece la capa de fondo

Given el ajuste **encendido** y una imagen generada disponible,
When se pinta la misma tarjeta,
Then la estructura es **idéntica** a la del escenario 1 (mismo hueco 16:9 y misma banda en el mismo
sitio) y lo único que cambia es que se pinta la **imagen de fondo** con su `scrim` y el **crossfade**
(`DishImageArtworkPolicy.CROSSFADE_MILLIS`), la categoría y el título pasan al tratamiento **sobre
imagen** (`headingOnImage(GENERATED) == true`) y la marca de agua **no** se pinta
(`watermarkVisible(GENERATED) == false`). Sin salto de layout.

### Scenario 3: ajuste encendido sin imagen = se ve igual que apagado

Given el ajuste encendido y un resolver que devuelve `null` (sin caché, sin red, sin clave), un
decodificador roto, un timeout, una cancelación (que se **propaga**) o el **tope por sesión** agotado,
When se pinta la tarjeta,
Then la capa resuelta es `TYPOGRAPHIC` y el resultado visual es **el mismo** que con el ajuste
apagado: hueco 16:9 + banda sólida, marca de agua visible, tipografía sobre superficie, sin microcopy
ni estado de error. Es la unificación: apagado y "encendido sin imagen" ya **no** se distinguen.

### Scenario 4: tarjeta ancha de Resultados

Given una receta en la lista de Resultados (ancho completo),
When se pinta `ResultRecipeCard`,
Then el hueco es **16:9** con categoría + título (26 sp) dentro, y la **banda** lleva los tres
ingredientes en **tres filas** con la **cantidad a la derecha en cifras tabulares** (hairline entre
filas) y la fila de metadatos con la coincidencia; la **descripción** (1 línea) sigue **debajo** de la
banda, dentro de la misma tarjeta. El filete y la marca de agua siguen presentes.

### Scenario 5: celda compacta de la rejilla de Favoritos

Given una receta favorita y la pestaña Favoritos en **rejilla** (dos columnas, ~172 dp),
When se pinta `FavoriteRecipeTile`,
Then la celda es el hueco **16:9** + banda con los ingredientes **en una línea** y **sin** fila de
metadatos (contenido aceptado de la celda compacta: se conserva para no apretar la celda y **no** se
sube la tarjeta a 3:2) y el **corazón** sigue superpuesto arriba a la derecha. La lista de Favoritos
(`RecipeMonogramTile`) **no** cambia.

### Scenario 6: regla pura de la estructura (JVM)

Given la implementación terminada,
When se consulta la regla pura de la banda,
Then `bandLayout(wide = false, compact = false)` → **una línea**; `bandLayout(wide = true, compact =
false)` → **tres filas**; `bandLayout(wide = false, compact = true)` → **una línea**; en el caso
imposible `wide && compact` manda `compact` (determinista, sin excepción); `HOLE_RATIO == 16f / 9f` y
las decisiones de capa siguen siendo excluyentes (`headingOnImage` ⇔ `!watermarkVisible`). Sin Android
ni Compose; dos llamadas iguales devuelven lo mismo.

### Scenario 7: la estructura 16:10 se retira

Given la implementación terminada,
When se inspecciona el diff,
Then **no** existe ya la rama "ficha apagada 16:10" (`TypographicHole` y el uso de
`SazonComponents.recipeCardImageRatio` en el hueco) ni ninguna constante de ratio 16:10 usada como
geometría de tarjeta (`grep` de `16f / 10f` y de `recipeCardImageRatio` en `app/src` → **0**), y el
KDoc / el test que afirmaban "el 16:10 de la ficha apagada" están reconciliados (ya no hay ficha
apagada distinta).

### Scenario 8: alcance, pureza y capturas

Given la implementación terminada,
When se inspecciona el diff,
Then `domain/**`, `data/**` (interruptor y tope intactos), `ui/artwork/RecipeArtwork*`,
`ui/theme` fuera de la retirada de la constante muerta, la Taxonomía, `seed-recipes.json`, `assets/**`,
build/lockfile, `init.sh` y `scripts/**` están intactos; **no** existe
`docs/design/concepts/recipe-card-l2-unified/` y las capturas aceptadas de `recipe-card-typographic` y
`dish-artwork-as-card-background` **no** se han modificado; la lógica nueva testeable en JVM no importa
Android ni Compose; y no hay hex sueltos nuevos fuera de `ui/theme`.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `recipe-card-l2-unified` (metadata, `verification`, notas y exención de
  capturas de la issue #129) y las entradas `accepted` de `recipe-card-typographic` y
  `dish-artwork-as-card-background`.
- `DESIGN.md` — Overview (párrafo "Estructura de tarjeta unificada (2026-10-14)"), frontmatter
  `components.recipe-card` y `components.recipe-card-image` (`ratio: "16:9 (… estructura L2 unificada
  con el ajuste encendido o apagado)"`), `Components` → RecipeCard, `Core Screens` 4/6 y OQ6
  (**resuelta**: "el hueco es siempre 16:9", ingredientes "en **una línea** cuando la tarjeta es
  estrecha y en **tres filas** cuando es ancha (Resultados y Detalle)"), y el bloque *Excepción de
  capturas* (issues #129–#132, que ya da esta feature por `not_started` y **sin** carpeta).
- `app/src/main/java/com/sazon/app/ui/home/RecipeFicha.kt` — **el** fichero a unificar: hoy bifurca
  `if (imageBackground) { ImageBackgroundHole + SolidBand } else { TypographicHole }`; `TypographicHole`
  usa `SazonComponents.recipeCardImageRatio` (16:10); `ImageBackgroundHole` usa
  `DishImageArtworkOverlay.HOLE_RATIO` (16:9) + `RecipeArtwork` + el slot con la capa resuelta;
  `SolidBand` pinta `FichaIngredients(wide = true)` para todo lo no-compacto; constantes de geometría
  (padding 13/16, banda 11 top, filete 6, hairlines, marca de agua 150@12 %, offsets 16/18) y
  `FichaTypography` local en Manrope.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt`,
  `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — los tres llamantes:
  `RecipeCard` (estrecho, 220 dp), `ResultRecipeCard` (ancho + `description`),
  `FavoriteRecipeTile` (compacto + corazón; `RecipeMonogramTile` es la lista, aparte).
- `app/src/main/java/com/sazon/app/ui/artwork/{DishImageArtworkOverlay,RecipeArtwork,
  RecipeArtworkImagePrecedence}.kt` — ratio L2 16:9, `headingOnImage`/`watermarkVisible`, `layerOf`,
  `crossfadeTarget` y el proveedor de la capa de fondo (firma y geometría a conservar).
- `app/src/main/java/com/sazon/app/ui/theme/Spacing.kt` — `SazonComponents.recipeCardImageRatio` =
  `16f / 10f` (**único** lector: `TypographicHole`; queda muerto al unificar).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` y
  `ui/{home/HomeScreen,results/ResultsScreen,favorites/FavoritesScreen}.kt` — ya observan el ajuste y
  reenvían `imageBackground` (no requieren cambios).
- `app/src/test/java/com/sazon/app/ui/artwork/DishImageArtworkOverlayTest.kt` — su primera aserción
  compara `HOLE_RATIO` con `16f / 10f` ("la ficha apagada"), frase que deja de ser cierta.
- `app/src/test/java/com/sazon/app/ui/home/RecipeCardFormatTest.kt`,
  `app/src/test/java/com/sazon/app/ui/artwork/RecipeBackgroundGateTest.kt` — patrón de test JVM del
  gate y de los formatos (no se debilitan).
- `docs/specs/{recipe-card-typographic,dish-artwork-as-card-background}.md` — contratos aceptados que
  esta feature **no** reabre salvo la forma de la tarjeta (la OQ6 que ellos dejaron abierta queda
  cerrada por `DESIGN.md`).
- `docs/adr/0006-typographic-recipe-cards.md` — decisión 2 (L2) y estado de las cinco features.
- `docs/design/concepts/typographic-cards/prototipo-c3-imagenes.html` — `.ncard.l2 .ingband{padding:11px
  13px 13px;background:surface}`, hueco L2 `aspect-ratio:16/9` y el ejemplo L2 de banda con `.ing3`.
- `docs/technical-discovery.md` (líneas 40, 230-257) — el párrafo de la tarjeta describe hoy "16:10 de
  la ficha apagada" (queda obsoleto).

### Existing Patterns To Follow

- **Una única `RecipeFicha` compartida** por estrecho/ancho/compacto: la unificación se hace **dentro**
  del componente, no duplicando tarjetas.
- **Estructura L2 por capas**: `RecipeArtwork` como proveedor de la capa de fondo con **slot** que
  recibe la capa resuelta; la **base tipográfica la pinta el llamante** (nunca queda hueco ni frame en
  blanco).
- **Modo explícito por parámetro** (`imageBackground`), no `CompositionLocal`; con `false` es el
  comportamiento por defecto (previews/tests) y **no** se compone `RecipeArtwork`.
- **Lógica/estado puro testeable en JVM + capa Compose delgada**; decisiones en un objeto puro
  (`DishImageArtworkOverlay`) y **0 hex fuera de `ui/theme`**.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- `RecipeFicha` mantiene **dos** estructuras y **dos** ratios (16:10 apagado / 16:9 L2); el apagado es
  el que **no** está unificado.
- La banda L2 actual usa **tres filas** también en la tarjeta **estrecha** (extrapolación de
  `dish-artwork-as-card-background` sin captura), mientras `DESIGN.md` (revisión del 2026-10-14) manda
  **una línea** cuando la tarjeta es estrecha. La unificación **normaliza** la banda a la regla de
  `DESIGN.md`.
- No hay regla pura que exprese "la banda es una línea o tres filas según el ancho": hoy vive dentro
  de `@Composable` y no es testeable en JVM.
- `SazonComponents.recipeCardImageRatio` (16:10) quedaría **sin lectores** y el KDoc de
  `DishImageArtworkOverlay` y el test afirman la existencia de una "ficha apagada 16:10".

## Technical Approach

1. **Una sola estructura en `RecipeFicha`**: eliminar la bifurcación `TypographicHole` /
   `ImageBackgroundHole` y dejar **siempre** `Hole` (16:9) + `SolidBand`. El hueco se pinta por capas:
   - **Con `imageBackground == false`**: **no** se compone `RecipeArtwork`; se pinta directamente el
     contenido del "slot" con la capa sintética `TYPOGRAPHIC` (marca de agua + categoría/título sobre
     superficie). Así se conserva el contrato aceptado "apagado = 0 lecturas de caché/semilla/
     generador" sin depender del gate.
   - **Con `imageBackground == true`**: `RecipeArtwork(recipe, Modifier.matchParentSize()) { layer ->
     contenido(layer) }`, exactamente como hoy.
   - El **contenido del hueco** (marca de agua condicionada por `watermarkVisible(layer)`, categoría +
     título con `headingOnImage(layer)`) se factoriza en **un solo** composable privado; el **filete de
     6 px** va encima, al pie, en los dos caminos.
2. **Banda sólida siempre**: debajo del hueco, `SolidBand` con los ingredientes y (salvo compacto) la
   fila de metadatos. El **ancho** de la banda lo decide la regla pura del punto 3 (una línea /
   tres filas). Se conservan los paddings (`BAND_PADDING_TOP` 11, lateral 13), el `Surface`/borde/
   tinte de `LocalSazonCardColors` y la descripción de Resultados **después** de la banda.
3. **Regla pura de la banda** (JVM, sin Compose): en `DishImageArtworkOverlay` (junto al ratio L2),
   `enum class FichaBandLayout { ONE_LINE, THREE_ROWS }` y
   `fun bandLayout(wide: Boolean, compact: Boolean): FichaBandLayout` con el contrato del Escenario 6
   (`compact` manda; `wide` → tres filas). Nombres/ubicación **provisionales**: si se mueven, mantener
   el contrato y citarlo en la evidencia.
4. **Retirada de lo no usado**: borrar `TypographicHole` y la constante muerta
   `SazonComponents.recipeCardImageRatio` (`ui/theme/Spacing.kt`); actualizar el KDoc de
   `DishImageArtworkOverlay` (ya no hay "ficha apagada 16:10") y los KDoc de `RecipeCard`,
   `ResultRecipeCard` y `FavoriteRecipeTile`.
5. **Tests JVM**: ampliar `DishImageArtworkOverlayTest` con los vectores de `bandLayout` y reconciliar
   la aserción que citaba el 16:10 (mantener cobertura: `HOLE_RATIO == 16f / 9f` y `> 1f`), sin tocar
   `RecipeBackgroundGateTest` ni los formatos (`RecipeCardFormatTest`, `RecipeMonogramTest`).

**Invariante**: encendido y apagado comparten **la misma** geometría; lo único que puede cambiar es el
**alfa** de la capa de fondo (`0f` → `1f`) y el tratamiento tipográfico derivado de la capa. Por eso
"encendido sin imagen" (Escenario 3) es visualmente idéntico a "apagado".

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/home/RecipeFicha.kt` — modificar; una sola estructura (hueco
  16:9 + banda siempre), contenido del hueco compartido, `TypographicHole` eliminado, banda según
  `bandLayout`.
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkOverlay.kt` — modificar; `FichaBandLayout`
  + `bandLayout(wide, compact)` (puro) y KDoc reconciliado.
- `app/src/main/java/com/sazon/app/ui/theme/Spacing.kt` — modificar; **retirar**
  `SazonComponents.recipeCardImageRatio` (16:10) por quedar sin lectores.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt`,
  `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — modificar; **solo** KDoc
  (16:10 → 16:9; la geometría la aporta `RecipeFicha`). Sin cambio funcional.
- `app/src/test/java/com/sazon/app/ui/artwork/DishImageArtworkOverlayTest.kt` — modificar; vectores de
  `bandLayout` y aserción del 16:10 reconciliada.
- `docs/technical-discovery.md` — modificar; el párrafo de la tarjeta (líneas ~230-257) deja de citar
  la ficha apagada 16:10 y documenta la estructura L2 única y la regla pura de la banda.
- `docs/adr/0006-typographic-recipe-cards.md` — modificar; el `Estado` anota la unificación 16:9 del
  2026-10-14 (revisión de diseño, issue #129) y la retirada de la dualidad 16:10/16:9.
- `DESIGN.md` — **not needed**; ya es la fuente de verdad y ya declara "estructura L2 siempre", el
  ratio 16:9 y la OQ6 resuelta (lo escribió la sesión 113).
- `docs/design/concepts/**` — **not needed** (exención de capturas; solo lectura, no se crean
  carpetas).
- `feature_list.json`, `PROGRESS.md` — los actualiza el orquestador; el implementer **no** los toca.

## Visual Design Impact

- UI involved: **sí** (`area: ui`; afecta a la forma de las tarjetas de tres pantallas).
- Design source: `DESIGN.md` (**manda**) + capturas **ya aceptadas** de referencia:
  - `docs/design/concepts/recipe-card-typographic/recipe-card/{screen.png,screen-dark.png,
    screen-resultados.png,screen-resultados-dark.png,code.html,code-resultados.html,DESIGN.md}`
  - `docs/design/concepts/recipe-card-typographic/favoritos-rejilla/{screen.png,screen-dark.png,
    code.html,DESIGN.md}`
  - `docs/design/concepts/dish-artwork-as-card-background/tarjeta-con-imagen/{screen.png,
    screen-dark.png,code.html,DESIGN.md}`
- Screens or states affected: **Inicio** (carrusel "Sugeridas para ti" y "Tus recetas"),
  **Resultados** (lista y modo semilla, mismo `ResultRecipeCard`) y **rejilla de Favoritos**; estados
  **apagado**, **encendido con imagen** y **encendido sin imagen** (ahora equivalentes al apagado), en
  **claro y oscuro**.
- New design artifact required: **no**.
- **UI captures (NOT required — exención autorizada, issue #129)**: `DESIGN.md` registra que esta
  feature **no** genera capturas nuevas y que **no** se crea
  `docs/design/concepts/recipe-card-l2-unified/`. El implementer **no** crea ni modifica nada bajo
  `docs/design/concepts/**`. La dirección visual aplicable son las capturas aceptadas listadas arriba
  (geometría L2, filete de 6 px, marca de agua, tipografía y banda) más `DESIGN.md`.
- Feature-specific visual states: **ninguno nuevo**; la geometría del hueco (16:9), la banda (padding
  11/13), el filete (6 px), la marca de agua (150 dp @12 %, offsets 16/18) y la tipografía **se
  conservan**.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — la tarjeta pasa a tener una **única** estructura L2
  (hueco 16:9 + banda) y la regla pura de la banda; se retira la mención al 16:10 de la ficha apagada.
- `docs/adr/0006-typographic-recipe-cards.md`: **update** — el `Estado` anota la unificación del
  2026-10-14 (issue #129) y que la dualidad 16:10/16:9 queda retirada.
- `DESIGN.md`: **not needed** — ya declara la estructura L2 siempre, el ratio 16:9, la banda y la OQ6
  resuelta; esta feature lo **implementa**, no lo cambia.
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas del repo.
- `docs/design/concepts/**`: **not needed** — exención de capturas; nada que versionar aquí.

## Implementation Plan

1. Añadir la regla pura `FichaBandLayout`/`bandLayout` en `DishImageArtworkOverlay` y ampliar su test.
2. Reescribir `RecipeFicha`: hueco **siempre** 16:9 con contenido del hueco compartido (marca de agua
   + categoría/título según la capa), `RecipeArtwork` **solo** con `imageBackground == true`, banda
   **siempre** debajo y `TypographicHole` eliminado.
3. Retirar `SazonComponents.recipeCardImageRatio` y reconciliar el KDoc de `DishImageArtworkOverlay`;
   actualizar los KDoc de `RecipeCard`, `ResultRecipeCard` y `FavoriteRecipeTile` (16:10 → 16:9).
4. Ejecutar el gate y las inspecciones de alcance/pureza (greps del Verification Plan).
5. Actualizar `docs/technical-discovery.md` y `docs/adr/0006`, registrar la evidencia y dejar la
   feature `passing` (la validación independiente es de otro rol).

## Implementation Tasks

- [ ] `DishImageArtworkOverlay`: `FichaBandLayout { ONE_LINE, THREE_ROWS }` y
      `bandLayout(wide, compact)` con el contrato del Escenario 6; KDoc sin "ficha apagada 16:10".
- [ ] `DishImageArtworkOverlayTest`: un caso por vector (`false/false` → una línea, `true/false` →
      tres filas, `false/true` → una línea, `true/true` → una línea por `compact`) y la aserción del
      ratio sin la frase del 16:10.
- [ ] `RecipeFicha`: una sola estructura — hueco 16:9 (`DishImageArtworkOverlay.HOLE_RATIO`) con
      contenido compartido y filete de 6 px; `RecipeArtwork` solo con `imageBackground == true`.
- [ ] `RecipeFicha`: **banda siempre** debajo del hueco (tres filas si `bandLayout == THREE_ROWS`; una
      línea con `·` si `ONE_LINE`), fila de metadatos salvo `compact`, y descripción de Resultados
      después de la banda.
- [ ] Eliminar `TypographicHole` y retirar `SazonComponents.recipeCardImageRatio` de
      `ui/theme/Spacing.kt`.
- [ ] KDoc de `RecipeCard`, `ResultRecipeCard` y `FavoriteRecipeTile` reconciliado (16:9 siempre; la
      geometría vive en `RecipeFicha`).
- [ ] Verificar por inspección: `RecipeArtwork` conserva firma/geometría y sus **2** consumidores; sin
      hex nuevos fuera de `ui/theme`; `data/**`, `domain/**`, build/lockfile, `init.sh`, `scripts/**` y
      `assets/**` intactos; `docs/design/concepts/**` sin cambios y **sin** carpeta nueva.
- [ ] Actualizar `docs/technical-discovery.md` y `docs/adr/0006` (Estado).
- [ ] `bash init.sh` (test + lint) en verde, registrar evidencia y avisar al orquestador (la
      `feature_list.json`/`PROGRESS.md` las cierra el orquestador).

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante estándar: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **no** arranca procesos largos y `init.sh` **no** se modifica.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.*"
  --rerun-tasks` → `BUILD SUCCESSFUL` (sin regresión de `RecipeCardFormatTest`, `RecipeMonogramTest`,
  `RecipeBackgroundGateTest` ni `RecipeArtworkImagePrecedenceTest`).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **0 fallos/errores/
  saltados**; re-derivar la baseline en `HEAD` (`git grep -c '@Test'`) y registrar el delta.
- Lint: `app/build/reports/lint-results-debug.sarif` → **0 errores** y ningún hallazgo nuevo bajo
  `app/src` (los `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion`/`OldTargetApi`
  son preexistentes).
- Mapeo explícito del `verification` de la feature:
  - "las tres variantes (estrecha, ancha y compacta) usan siempre el hueco 16:9 + banda sólida":
    escenarios 1, 2, 4 y 5 por inspección de la única estructura + `bandLayout` (escenario 6) +
    gate/`RecipeBackgroundGateTest` intacto.
  - "sin imagen solo desaparece la capa de fondo y el layout no cambia": escenarios 2 y 3 (misma
    estructura, único cambio = alfa de la capa; `HOLE_RATIO` fijo).
  - "se deja de usar el hueco 16:10 con los ingredientes dentro": escenario 7 (`grep` de
    `16f / 10f` y `recipeCardImageRatio` en `app/src` → 0).
  - "se conservan el filete de 6 px, la marca de agua, la tipografía y la geometría del hueco L2":
    inspección de las constantes conservadas en `RecipeFicha` (6 dp, 150 dp @12 %, offsets 16/18,
    16:9) + escenario 6.
  - "capturas de `docs/design/concepts/recipe-card-l2-unified/` respetadas" → **reconciliado por la
    exención de la issue #129** (registrada en `DESIGN.md`): esa carpeta **no** se crea ni se exige; la
    referencia son las capturas aceptadas de `recipe-card-typographic` y
    `dish-artwork-as-card-background`, que se comprueban **sin modificar** (`git status
    docs/design/concepts` limpio y `test ! -e docs/design/concepts/recipe-card-l2-unified`).
  - "'bash init.sh' verde; sin dependencias nuevas": gate + `git diff --name-only`.
- Inspección de pureza y alcance:
  - `grep -rn "recipeCardImageRatio\|16f / 10f" app/src` → **0**.
  - `grep -rn "import android\|import androidx" app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkOverlay.kt`
    → **0**.
  - `grep -rn "0xFF\|0x[0-9A-Fa-f]\{8\}" app/src/main/java/com/sazon/app/ui/home app/src/main/java/com/sazon/app/ui/artwork`
    → sin hex nuevos (los valores viven en `ui/theme`).
  - `grep -rn "RecipeArtwork(" app/src/main/java/com/sazon/app/ui` → la definición + **2**
    consumidores (hueco de `RecipeFicha` y hero del Detalle).
  - `git diff --name-only` = Expected File Changes (+ esta spec + docs durables); **sin** diff en
    `data/**`, `domain/**`, `assets/**`, `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`,
    `DESIGN.md` ni `docs/design/concepts/**`.
- Cobertura **E2E**: **no aplica** — el repo no tiene suite E2E persistente y `AGENTS.md` no automatiza
  UI de emulador; la cobertura canónica es la lógica/estado en JVM (regla de la banda y capa) más la
  revisión visual de la persona.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: en el emulador, con el ajuste
  **apagado**, abrir Inicio (carrusel y "Tus recetas"), Resultados y la rejilla de Favoritos en claro y
  oscuro y comprobar que la tarjeta es hueco 16:9 + banda (ingredientes y metadatos en la banda, filete,
  marca de agua); después **encender** el ajuste y confirmar que con imagen cambia **solo** el fondo (sin
  salto) y que sin imagen (modo avión o tope agotado) la tarjeta se ve **igual** que apagada.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal; conteos de suites/tests con delta vs `HEAD`.
- Vectores de la regla pura: `bandLayout(false,false) == ONE_LINE`, `bandLayout(true,false) ==
  THREE_ROWS`, `bandLayout(false,true) == ONE_LINE`, `bandLayout(true,true) == ONE_LINE`;
  `HOLE_RATIO == 16f / 9f`.
- Confirmación de que el camino apagado **no** compone `RecipeArtwork` (0 lecturas de caché/semilla/
  generador) y de que `RecipeBackgroundGateTest` sigue verde sin cambios.
- Resultado de los greps de alcance/pureza y de `git diff --name-only` (0 referencias a 16:10, 2
  consumidores de `RecipeArtwork`, `docs/design/concepts/**` intacto y **sin** carpeta
  `recipe-card-l2-unified/`).
- Registro en `docs/technical-discovery.md` y `docs/adr/0006`; confirmación de que el implementer **no**
  hizo commit, push ni PR.

## Decisions And Unknowns

Decisiones cerradas (el implementer no las reabre):

1. **La estructura por defecto (apagado) es la L2**: hueco **16:9** + banda. El default de
   `imageBackground` sigue siendo `false` (previews y tests), pero el default ya **no** es una ficha
   distinta.
2. **La banda sigue el ancho, no el ajuste**: una **línea** en tarjeta estrecha (`RecipeCard`, celda
   compacta) y **tres filas** en la ancha (`ResultRecipeCard`) — regla de `DESIGN.md` y resolución de
   la **OQ6**. Esto **normaliza** la banda de la tarjeta estrecha con el ajuste encendido, que hasta
   ahora usaba tres filas (extrapolación de `dish-artwork-as-card-background` **sin captura**). Es el
   punto que la persona revisa en el emulador.
3. **Apagado no compone `RecipeArtwork`** (capa sintética `TYPOGRAPHIC`): mantiene el contrato aceptado
   "0 lecturas de caché/semilla/generador" y la degradación silenciosa.
4. **La celda compacta conserva su contenido** (ingredientes en una línea, **sin** fila de metadatos) y
   el corazón superpuesto; solo cambia la estructura (hueco 16:9 + banda).
5. **Se retira la constante 16:10** (`SazonComponents.recipeCardImageRatio`) por quedar sin lectores:
   dejar una constante de geometría muerta y engañosa sería la clase de deriva que esta feature corrige.
6. **Sin capturas nuevas**: exención autorizada (#129) registrada en `DESIGN.md`; nada que crear bajo
   `docs/design/concepts/**`.

Incógnitas y riesgos (no bloquean, quedan anotados):

- **Banda estrecha (una línea vs tres filas)**: es la única diferencia perceptible respecto al L2 que ya
  existía con el ajuste encendido en el carrusel; se sigue `DESIGN.md`/OQ6 y lo revisa la persona. Un
  defecto visual se corrige con una **feature correctiva**, sin bloquear el flujo autónomo.
- **Celda compacta sin metadatos**: se conserva lo aceptado; si al ver la banda la persona quiere la
  fila, es una feature correctiva (no se improvisa aquí).
- **Alturas**: el hueco 16:9 es **más alto** que el 16:10 en la tarjeta estrecha, así que las tarjetas
  del carrusel y del bloque "Tus recetas" **crecen** ~algo y la banda suma alto; es la forma aceptada
  (no un bug). No hay salto de layout *durante* el crossfade.
- **Contraste del título sobre la imagen**: depende del `scrim` sobre una foto arbitraria; no es
  medible en JVM (queda como estaba, token aceptado).
- **`ui/theme/Spacing.kt`**: la retirada es de una constante **muerta**; no se añade ni se cambia ningún
  token de tema (superficie, borde, tinte, escala tipográfica intactos).

## Validator Checklist

- [ ] Alcance: solo `ui/home/RecipeFicha.kt`, `ui/artwork/DishImageArtworkOverlay.kt`, `ui/theme/Spacing.kt`
      (retirada de la constante muerta), KDoc de los 3 llamantes, el test de `DishImageArtworkOverlay` y
      las 2 docs durables; **sin** tocar `RecipeArtwork`/su política, el interruptor/tope (`data/**`),
      `domain/**`, `assets/**`, build/lockfile, `init.sh`, `scripts/**`, `DESIGN.md` ni
      `docs/design/concepts/**`, y **sin** dependencias nuevas.
- [ ] Escenarios 1-8: una sola estructura (16:9 + banda) en estrecha/ancha/compacta; apagado = encendido
      sin imagen; encendido con imagen solo cambia el fondo y el tratamiento tipográfico; sin salto de
      layout; filete 6 px, marca de agua 12 %, tipografía y geometría L2 conservados.
- [ ] La banda sigue `bandLayout` (una línea estrecha/compacta, tres filas ancha) y la regla pura es
      determinista, con casos negativos que no pasan en vacío.
- [ ] Ninguna referencia a un ratio 16:10 de tarjeta en `app/src`; la constante retirada no tenía otros
      lectores.
- [ ] El camino apagado no compone `RecipeArtwork` ni lee caché/semilla/generador
      (`RecipeBackgroundGateTest` verde sin cambios).
- [ ] `bash init.sh` verde con evidencia (comandos, conteos, vectores, greps); lint sin hallazgos nuevos
      bajo `app/src`.
- [ ] Capturas: **no** se exige ni existe `docs/design/concepts/recipe-card-l2-unified/` (exención #129
      registrada en `DESIGN.md`) y las capturas aceptadas de referencia **no** se han modificado.
- [ ] `docs/technical-discovery.md` y `docs/adr/0006` actualizados; `feature_list.json`/`PROGRESS.md`
      los actualiza el orquestador y el implementer no hizo commit, push ni PR.
- [ ] No se añade comportamiento ajeno a la feature (ni Detalle, ni miniatura de la lista, ni bloque
      "Tus recetas", ni tokens/microcopy/estados nuevos).
