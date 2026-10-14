# Feature Implementation Spec: RecipeCard tipográfica con los tres ingredientes principales y monograma en Favoritos

## Source Feature

- `id`: `recipe-card-typographic`
- `area`: `ui`
- `depends_on`: `["card-typographic-tokens-and-design"]` (satisfecho: `accepted`, sesión 103)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Referencias de diseño: `docs/adr/0006-typographic-recipe-cards.md` (decisiones 1, 5, 6),
  `DESIGN.md` (`components.recipe-card`, `components.recipe-tile-favorites`, `Components`,
  `Core Screens` 4 y 6) y `docs/design/concepts/recipe-card-typographic/` (capturas, ver más abajo).

## Goal

Convertir la tarjeta de receta de Home, Resultados y Favoritos en la **ficha tipográfica** del ADR 0006
(dirección **C3 "carta de restaurante"**): categoría en versalitas, título en Manrope, los **tres
ingredientes principales** con cantidad en cifras tabulares, la fila de metadatos, el porcentaje de
coincidencia en terracota, el **filete de 6px** con el degradado de la cocina (`RecipeAccent`) y la
**marca de agua** del glifo de categoría al 12 % — **sin ninguna foto**. En Favoritos, la miniatura de
la lista (96×96) pasa a un **monograma de iniciales** del plato sobre `surfaceVariant`.

La feature entrega además la **lógica pura testeable en JVM** que sostiene la tarjeta: la selección
determinista de los tres ingredientes principales y el monograma, compartidas por `RecipeCard`,
`RecipeListCard` y la rejilla de Favoritos. Es la segunda de las cinco features del ADR 0006 y consume
los tokens de tarjeta ya publicados (`SazonCardTokens`/`SazonCardColors`/`LocalSazonCardColors`,
`darkCardSurface #3A2E26`, `darkCardOutline #7D6A5C`, `tintedSurface`).

## Non-Goals

- **No** se toca el **Detalle** (`recipe-detail-screen` / `recipe-detail-typographic-header`): el hero
  tipográfico es de esa otra feature.
- **No** se integra la imagen como **capa de fondo** (variante L2): es `dish-artwork-as-card-background`.
  En esta feature las tarjetas **no muestran imagen alguna** (ni foto local ni generada).
- **No** se retiran aún los assets de fotos ni la cadena local: es `retire-recipe-photos`.
- **No** se añaden assets, iconos, fuentes ni dependencias; no cambian `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh` ni `scripts/**`.
- **No** se añaden tokens de tema nuevos ni se alteran `LightColorScheme`/`DarkColorScheme`,
  `darkSurface` ni el navegador/hojas. Los tokens de tarjeta ya existen.
- **No** se cambia el resto de Home (saludo, buscador IA, héroe, carrusel/bloque/estado vacío),
  el resto de Resultados (chips, cabecera, estados) ni el resto de Favoritos (buscador, chips de
  orden, toggle, nota offline, estado vacío, swipe "Eliminar", corazón).
- **No** se decide la pregunta abierta **OQ6** (subir el carrusel a **3:2**): se mantiene **16:10**, con
  los ingredientes en **una línea** cuando la tarjeta es estrecha y en **tres filas** cuando hay ancho.

## Job Story

Cuando hojeo recetas en Inicio, Resultados o Favoritos,
quiero leer de un vistazo qué plato es y qué lleva (los tres ingredientes principales, con cantidad y
sin una foto que no corresponda al plato),
para decidir qué cocinar sin que una imagen genérica me confunda.

## Users And Permissions

- **Usuario local**: ve las tarjetas tipográficas en Inicio, Resultados y Favoritos; sin permisos.
- **Desarrollador / agente**: escribe la lógica pura, los composables, los tests JVM y el gate.

## Coherencia de color obligatoria (mandato: `DESIGN.md` manda)

Hay **dos contratos de color distintos** y no deben confundirse:

- **Tarjetas tipográficas** (`RecipeCard`, `ResultRecipeCard`, rejilla de Favoritos): el único cromo de
  color es el **filete de 6px** con el **degradado por cocina** (`RecipeAccent`, según `cuisineId`) del
  borde inferior. La variación por cocina vive **aquí**.
- **Miniatura de la lista de Favoritos** (`RecipeListCard`, 96×96): la regla es **terracota**
  (`primary`), de **5px a la izquierda** (`DESIGN.md` → `components.recipe-tile-favorites.accentRule:
  "5px a la izquierda, terracota (primary)"`). **No** es por cocina: a 5px los pares suaves de
  `RecipeAccent` no se perciben.

Redacciones previas que **no** mandan y quedan como punto a reconciliar (el planner no edita fuera de
`docs/specs/`; lo resuelve el implementer/orquestador):

- `feature_list.json` → `user_visible_behavior` de esta feature dice "la regla de color de la cocina a
  la izquierda" (frase previa; debe leerse **terracota**).
- `docs/adr/0006` **§6** dice "la regla de color de la cocina a la izquierda" (genérico; reconciliar a
  **terracota** para la miniatura, dejando el por-cocina en el filete de 6px).
- `docs/design/concepts/recipe-card-typographic/README.md` (línea ~28) repite la frase previa; la
  propia captura `favoritos-lista/DESIGN.md` y la captura `recipe-card/DESIGN.md` ya dicen **terracota**.

## Acceptance Scenarios

### Scenario 1: `RecipeCard` de Inicio es una ficha tipográfica estrecha

Given una receta con categoría, cocina e ingredientes,
When se pinta la tarjeta del carrusel "Sugeridas para ti" (ancho ~220–232 dp, 16:10),
Then no hay ninguna imagen; dentro del hueco aparecen la **categoría en versalitas**, el título (máx. 2
líneas), los **tres ingredientes principales en una línea** separados por `·`, la fila
`tiempo · dificultad · comensales`, el porcentaje de coincidencia en terracota (solo si `matchScore` no
es `null`), el **filete de 6px** por cocina y la **marca de agua** del glifo de categoría al 12 %.

### Scenario 2: `ResultRecipeCard` de Resultados es una ficha tipográfica ancha

Given una receta en la lista de Resultados (ancho completo),
When se pinta la tarjeta,
Then no hay imagen; los **tres ingredientes principales** se muestran en **tres filas** (nombre a la
izquierda, **cantidad a la derecha en cifras tabulares**, hairline entre filas), y se conservan el
título, la descripción (1 línea), la fila de metadatos y la coincidencia; el filete y la marca de agua
siguen presentes.

### Scenario 3: rejilla de Favoritos comparte la ficha y la lista usa el monograma

Given una receta favorita,
When la pestaña Favoritos está en **rejilla**,
Then cada celda es la ficha tipográfica estrecha (ingredientes en una línea, filete por cocina, sin
foto), con el corazón superpuesto.
When está en **lista**,
Then la fila muestra una **miniatura 96×96 de monograma** de iniciales del plato sobre `surfaceVariant`
con la **regla terracota de 5px a la izquierda** (no por cocina), texto a la derecha y corazón.

> **Hallazgo del implementer (2026-10-14).** La celda de la rejilla es la ficha estrecha **compacta**
> que muestra la captura `favoritos-rejilla/screen.png`: título a **16sp**, ingredientes a **10,5sp** y
> **sin** fila de metadatos. A 16:10 y ~172dp de ancho (dos columnas) la fila de metadatos **no cabe**
> junto al título a dos líneas, y subir la tarjeta a 3:2 es justo **OQ6**, que esta feature **no**
> decide; la captura aceptada resuelve el aprieto omitiendo esa fila. El resto (categoría, ingredientes
> en una línea, filete por cocina y marca de agua) es idéntico a la tarjeta de Inicio, y el corazón se
> mantiene superpuesto.

### Scenario 4: selección determinista de los tres ingredientes principales

Given una receta con ingredientes marcados `isPantry`,
When la lógica pura selecciona los ingredientes principales,
Then devuelve **hasta 3**, en el **orden de declaración**, **excluyendo** los `isPantry == true`; si
tras excluirlos hay menos de 3, se completan con los `isPantry` en su orden; si la receta tiene menos
de 3 en total, devuelve los que haya; sin `Random`, sin reloj, sin Android y sin red: dos llamadas
iguales devuelven la misma lista.

### Scenario 5: monograma determinista del título

Given un título de receta,
When se calcula el monograma,
Then son **hasta 2 iniciales en mayúscula** de las palabras significativas (se saltan los conectores
`de`, `del`, `la`, `el`, `los`, `las`, `con`, `al`, `y`, `a`, `en`, `para`, `un`, `una`, `o`, `e`): la
inicial de la **primera** palabra significativa y la de la **última**, y reproduce los vectores de las
capturas: `Tortilla de patatas`→`TP`, `Arroz con pollo criollo`→`AC`, `Gazpacho andaluz`→`GA`,
`Curry de garbanzos`→`CG`, `Pasta al tomate`→`PT`. Un título de una sola palabra significativa da
**1 inicial**; un título vacío o en blanco da `null` (la UI no pinta monograma); dos llamadas iguales
coinciden.

> **Hallazgo del implementer (2026-10-14, reconciliación de la redacción).** La redacción previa de
> este escenario decía "hasta 2 iniciales **de las palabras significativas**" sin precisar cuáles, y el
> vector `Arroz con pollo criollo`→`AC` es **incompatible** con "las dos primeras significativas"
> (`Arroz` + `pollo` daría `AP`, porque `con` se salta). Se implementa y se documenta la lectura que
> reproduce **todos** los vectores de las capturas aceptadas: **primera + última** palabra
> significativa (`A` de `Arroz` + `C` de `criollo`). Sin cambio de vectores, de la lista de conectores
> ni del caso negativo; los títulos de menos de tres palabras significativas (todos los demás vectores)
> no distinguen entre ambas lecturas.

### Scenario 6: claro y oscuro con los tokens de tarjeta

Given `darkTheme = true`,
When se pinta cualquier tarjeta,
Then la superficie es `SazonCardTokens.dark.surface` (`#3A2E26`), lleva el **borde** de
`boundaryWidth`/`boundary` (`#7D6A5C`, 1 dp) en lugar de sombra y la superficie va **teñida** con el
acento de la cocina (`SazonCardTokens.tintedSurface`); las tarjetas claras usan `surface` (`#FFFFFF`)
sin borde. El filete y la marca de agua usan **roles del tema**, nunca hex sueltos.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `recipe-card-typographic`, líneas 1253-1266) — alcance, verificación y
  notas.
- `docs/adr/0006-typographic-recipe-cards.md` — decisiones 1 (ficha tipográfica), 5 (tokens oscuros),
  6 (miniatura de Favoritos) y 7 (`RecipeArtwork` cambia de rol).
- `DESIGN.md` — `components.recipe-card` (67-73), `components.recipe-tile-favorites` (79-83),
  `Typography` (271-277, cifras tabulares), `Components` (295-302) y `Core Screens` 4 y 6 (433-443).
- `docs/design/concepts/recipe-card-typographic/{recipe-card,favoritos-lista,favoritos-rejilla}/`
  (`screen.png`, `screen-dark.png`, `code.html`, `DESIGN.md`) — capturas **aportadas** y la nota de
  coherencia (regla 5px **terracota**).
- `docs/design/concepts/typographic-cards/prototipo-c3.html` — CSS de la tarjeta C3
  (`.ncard`, `.ing1`/`.ing3`, `.filete`, `.wm`, `.mono-tile`) y la hoja de componentes (562-570).
- Capturas de tokens: `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/`.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` y `RecipeCardFormat.kt` — la tarjeta de
  Inicio (hoy `RecipeArtwork` + `titleMedium` + píldora de coincidencia) y sus formatos puros.
- `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt` — tarjeta ancha (hoy imagen 16:10 y
  descripción).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` y `FavoritesScreen.kt` —
  `FavoriteRecipeCard` (lista) y `FavoriteRecipeTile` (rejilla), hoy con `RecipeArtwork`.
- `app/src/main/java/com/sazon/app/ui/artwork/{RecipeArtworkStyle,RecipeArtwork}.kt` —
  `RecipeGlyph`/`RecipeAccent` (puros) y el pintado del glifo (`RecipeGlyphCanvas`, **privado**) y el
  degradado de acento (`accentColors`, **privado**).
- `app/src/main/java/com/sazon/app/ui/theme/{CardTokens,Type,Spacing,Shape}.kt` — tokens de tarjeta
  (`SazonCardTokens`, `tintedSurface`), `SazonTypography` (solo overridea `displayLarge`,
  `headlineLarge`, `bodyLarge`, `labelSmall`), `SazonSpacing`, `SazonShapes`.
- `app/src/main/java/com/sazon/app/ui/detail/IngredientFormat.kt` — `quantityLabel(quantity, unit)`
  puro y reutilizable.
- `app/src/main/java/com/sazon/app/ui/results/MatchScore.kt` — `matchScore` es **derivado**, no
  persistido; en Home y favoritos vale `null` (no hay píldora).
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `RecipeIngredient` (`name`, `quantity`,
  `unit: MeasureUnit?`, `isPantry`, `ingredientId`) y `Recipe.matchScore`.
- `app/src/main/assets/seed-recipes.json` — orden real de ingredientes y marca `isPantry` (sal,
  aceite, vinagre).
- `app/src/test/java/com/sazon/app/ui/home/RecipeCardFormatTest.kt` — patrón de test JVM de formatos.
- `docs/technical-discovery.md` (105-129, 131-143, 229-262) — Home, Favoritos y el párrafo del ADR 0006
  (que ya describe la ficha tipográfica y da las capturas por **pendientes**).

### Existing Patterns To Follow

- **Lógica/estado puro + capa Compose delgada**: la tarjeta se sostiene en objetos puros testeables en
  JVM (`RecipeCardFormat`, `FavoritesList`, `MatchScore`), patrón del repo.
- **Cifras tabulares** con `fontFeatureSettings = "tnum"` (ya usado en `ResultRecipeCard` y
  `FavoriteRecipeCard`).
- **Tokens de tarjeta** por `LocalSazonCardColors.current` (no hex sueltos; `Color.kt` es el único
  sitio con hex).
- **Reutilizar el estilo de la receta**: `RecipeArtworkStyle.of(recipe)` da el glifo (marca de agua) y
  el acento (filete) sin duplicar la tabla de 18+11.
- **Casos negativos no vacíos** y determinismo (patrón `CardTokensTest`, `RecipeArtworkStyleTest`).
- `ui.artwork` es la **única** fuente de dibujo del glifo y del degradado de acento; no se reimplementa.

### Current Gaps

- `RecipeCard`/`ResultRecipeCard`/`FavoriteRecipeCard`/`FavoriteRecipeTile` **usan `RecipeArtwork`**
  (imagen/placeholder): hay que sustituirlo por la ficha tipográfica y el monograma.
- No existe selección pura de "ingredientes principales" ni monograma de receta.
- `RecipeCard`/`ResultRecipeCard` pintan el título con `titleMedium`/**`titleSmall`**, que en este
  tema caen a la **tipografía por defecto de Material** (Roboto), no a Manrope: la ficha debe fijar la
  tipografía de tarjeta (Manrope) **sin** cambiar la escala compartida del tema.
- `RecipeGlyphCanvas` (dibujo del glifo) y `accentColors` (degradado de acento) son **privados** en
  `RecipeArtwork.kt`: la ficha necesita ambos (marca de agua y filete).
- La coincidencia se pinta hoy como **píldora rellena**; la captura / `DESIGN.md` usan **texto
  terracota**.

## Technical Approach

1. **Contenido puro compartido** (JVM, sin Compose/Android):
   - En `ui/home/RecipeCardFormat.kt`: `categoryLine(taxonomy, recipe): String` (p. ej. `"Arroz ·
     Latina"`, con respaldo al id si falta etiqueta), `mainIngredients(recipe, limit = 3):
     List<RecipeIngredient>` (regla del Escenario 4) y un modelo mínimo `MainIngredient(name,
     quantityLabel: String)` con `quantityLabel = IngredientFormat.quantityLabel(quantity, unit)`
     (reutiliza `ui.detail.IngredientFormat`, puro). Añadir `matchBadge(score, compact = false)` —
     `"92% coincidencia"` por defecto (contrato actual intacto), `"92%"` en compacto.
   - Nuevo `ui/home/RecipeMonogram.kt`: `object RecipeMonogram { fun of(title: String): String? }`
     (regla del Escenario 5).
2. **Primitivas de dibujo reutilizables**: extraer de `RecipeArtwork.kt` el pintado del glifo
   (`RecipeGlyphCanvas` → `@Composable fun RecipeGlyphArtwork(glyph, tint, modifier)`) y el degradado
   de acento (`accentColors` → `@Composable fun recipeAccentBrush(accent: RecipeAccent): Brush`) a un
   fichero compartido de `ui/artwork` (nombres/ubicación provisionales). `RecipeArtwork` **debe**
   conservar su firma y su comportamiento exacto (solo pasa a usar las funciones extraídas).
3. **Composable de ficha** reutilizable (p. ej. `ui/home/RecipeFicha.kt`): un único contenido que pinta
   categoría + título + ingredientes (una línea o tres filas según `wide`) + metadatos + coincidencia +
   filete + marca de agua, con la superficie/borde/tinte de `LocalSazonCardColors`. `RecipeCard`,
   `ResultRecipeCard` y la rejilla de Favoritos lo consumen (estrecho/estrecho/ancho-ancho según punto
   de uso) en lugar de `RecipeArtwork`.
4. **Miniatura de monograma** en la lista de Favoritos: `Box` 96×96, `surfaceVariant`, esquina
   `SazonShapes.medium`, **regla terracota de 5px a la izquierda**, monograma en Manrope centrado
   (`onSurfaceVariant`); conserva texto y corazón. Sin filete de cocina ni imagen.
5. **Tipografía de tarjeta**: estilos locales construidos desde `SazonFontFamilies.Display` (Manrope) y
   los valores de `DESIGN.md` (título 21sp estrecho / 26sp ancho, Bold; categoría en versalitas
   `letterSpacing` + `uppercase()`; coincidencia 13sp Bold `primary`). **No** se añaden roles a
   `SazonTypography` en esta feature.
6. **Tests JVM** (sin emulador, sin Robolectric, sin red): `RecipeCardFormatTest` ampliado
   (`categoryLine`, `mainIngredients`, `matchBadge` compacto) y `RecipeMonogramTest` nuevo.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/home/RecipeCardFormat.kt` — modify; `categoryLine`,
  `mainIngredients`/`MainIngredient`, `matchBadge(score, compact)`.
- `app/src/main/java/com/sazon/app/ui/home/RecipeMonogram.kt` — create; monograma puro.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` — modify; ficha tipográfica estrecha.
- `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt` — modify; ficha ancha (3 filas).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — modify; miniatura de
  monograma (lista) y ficha (rejilla).
- `app/src/main/java/com/sazon/app/ui/home/RecipeFicha.kt` (nombre provisional) — create; contenido
  compartido de la ficha.
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — modify; usa las primitivas extraídas
  (firma y comportamiento intactos).
- `app/src/main/java/com/sazon/app/ui/artwork/<primitivas>.kt` (nombre provisional) — create;
  `RecipeGlyphArtwork` + `recipeAccentBrush`.
- `app/src/test/java/com/sazon/app/ui/home/RecipeCardFormatTest.kt` — modify.
- `app/src/test/java/com/sazon/app/ui/home/RecipeMonogramTest.kt` — create.
- `docs/technical-discovery.md` — modify; el párrafo de la tarjeta (229-262) pasa de "dirección" a
  implementada, nombra la lógica pura y actualiza el estado de capturas.
- `docs/adr/0006-typographic-recipe-cards.md` — modify; `Estado` marca esta feature implementada y
  reconcilia **§6** (regla 5px **terracota** en la miniatura; el por-cocina es el filete de 6px).
- `docs/design/concepts/recipe-card-typographic/README.md` — modify (opcional, reconciliación de la
  frase previa sobre la regla); lo decide el implementer.
- `DESIGN.md` — **not needed** (ya declara filete 6px por cocina, miniatura terracota 5px y
  `darkCardSurface`/`darkCardOutline`).
- `feature_list.json`, `PROGRESS.md` — los actualiza el orquestador al cerrar, no el implementer.

## Visual Design Impact

- UI involved: **yes** (`area: ui`).
- Design source: `DESIGN.md` (`components.recipe-card`, `components.recipe-tile-favorites`,
  `Typography`, `Components`, `Core Screens` 4/6) + ADR 0006 + capturas.
- Screens or states affected: **Home** (carrusel "Sugeridas para ti"), **Resultados** (lista),
  **Favoritos** (lista y rejilla), en **claro y oscuro**. Afecta también las tarjetas de semilla del
  modo degradado de Resultados (mismo `ResultRecipeCard`).
- New design artifact required: **no** (capturas ya aportadas, 2026-10-14).
- **UI captures (required, ya existentes)** — el implementer **debe** revisarlas antes de dar por bueno
  el acabado y el resultado debe reproducirlas:
  - `docs/design/concepts/recipe-card-typographic/recipe-card/screen.png` y `screen-dark.png` (Inicio;
    tarjeta estrecha con ingredientes en una línea, filete y marca de agua).
  - `docs/design/concepts/recipe-card-typographic/recipe-card/screen-resultados.png` y
    `screen-resultados-dark.png` (Resultados; tarjeta ancha con tres filas y cantidad).
  - `docs/design/concepts/recipe-card-typographic/favoritos-lista/screen.png` y `screen-dark.png`
    (miniatura **96×96 de monograma** con la **regla terracota de 5px**).
  - `docs/design/concepts/recipe-card-typographic/favoritos-rejilla/screen.png` y `screen-dark.png`
    (ficha estrecha en dos columnas; ingredientes en una línea).
  - Apoyo: `code.html`/`code-resultados.html`/`DESIGN.md` en cada carpeta.
- Feature-specific visual states: **ninguno nuevo** (los estados vacío/carga/error ya existen y no
  cambian); la ficha no tiene estado de imagen.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — el párrafo de la imagen de receta (229-262) deja de
  describir una "dirección pendiente" y documenta la ficha implementada, la lógica pura
  (`RecipeCardFormat`, `RecipeMonogram`) y las capturas ya aportadas.
- `docs/adr/0006-typographic-recipe-cards.md`: **update** — `Estado` marca `recipe-card-typographic`
  implementada; **§6** se reconcilia a **terracota 5px** en la miniatura (el filete de 6px sigue siendo
  el cromo por cocina).
- `docs/domain-model.md`: **not needed** — no hay entidades, relaciones ni estados.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas del repo.
- `DESIGN.md`: **not needed** — ya es la fuente de verdad y no cambia.
- `feature_list.json`: punto a reconciliar (`user_visible_behavior` con la frase previa) — lo edita el
  orquestador, no el planner ni el implementer.

## Implementation Plan

1. Añadir la lógica pura: `RecipeCardFormat.categoryLine`/`mainIngredients`/`MainIngredient`/
   `matchBadge(compact)` y `RecipeMonogram`.
2. Extraer `RecipeGlyphArtwork` y `recipeAccentBrush` de `RecipeArtwork.kt` sin cambiar su firma ni su
   comportamiento.
3. Crear el contenido compartido de la ficha (categoría, título, ingredientes, metadatos, coincidencia,
   filete, marca de agua) sobre `LocalSazonCardColors`.
4. Reescribir `RecipeCard` (estrecho), `ResultRecipeCard` (ancho) y la rejilla de `FavoriteRecipeCard`
   (estrecho) con el contenido compartido; sustituir la miniatura de la lista por el monograma.
5. Añadir/ampliar los tests JVM (`RecipeCardFormatTest`, `RecipeMonogramTest`) y ejecutar el gate.
6. Actualizar `docs/technical-discovery.md`, `docs/adr/0006` (Estado y §6) y registrar la evidencia.

## Implementation Tasks

- [ ] `RecipeCardFormat.categoryLine`, `mainIngredients` (`isPantry` excluido, orden estable, máx. 3) y
      `matchBadge(score, compact)` (contrato por defecto intacto).
- [ ] `RecipeMonogram.of` con la lista de conectores, tope de 2, `null` si vacío.
- [ ] Extraer `RecipeGlyphArtwork` y `recipeAccentBrush` a `ui/artwork`; `RecipeArtwork` intacto.
- [ ] Contenido compartido de la ficha (estrecho: una línea; ancho: tres filas con `tnum`).
- [ ] `RecipeCard`, `ResultRecipeCard` y rejilla de Favoritos consumen la ficha; sin `RecipeArtwork`.
- [ ] Miniatura 96×96 de monograma con regla **terracota 5px** en la lista de Favoritos.
- [ ] Estilos de tarjeta en Manrope desde `SazonFontFamilies` (título, categoría, coincidencia).
- [ ] Tests JVM de selección de ingredientes, monograma y `categoryLine`/`matchBadge` compacto.
- [ ] `bash init.sh` verde y registro de evidencia; greps de alcance.
- [ ] Actualizar `docs/technical-discovery.md` y `docs/adr/0006` (incl. §6).

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante actual: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **no** arranca procesos largos ni cambia `init.sh`.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.home.*"
  --rerun-tasks` → `BUILD SUCCESSFUL`.
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: `RecipeCardFormatTest`
  (ampliado) y `RecipeMonogramTest` (nuevo) en verde, **0 fallos/errores/saltados**; re-derivar la
  baseline en `HEAD` y registrar el delta.
- Vectores de monograma a comprobar (de las capturas): `Tortilla de patatas`→`TP`, `Arroz con pollo
  criollo`→`AC`, `Gazpacho andaluz`→`GA`, `Curry de garbanzos`→`CG`, `Pasta al tomate`→`PT`; además,
  una palabra→1 inicial y vacío/solo espacios→`null`.
- Vectores de selección: receta con `isPantry` (p. ej. tortilla: Patata, Huevo, Cebolla, Aceite+Sal
  pantry) → `Patata, Huevo, Cebolla`; receta con 2 no-pantry y 3 pantry → se completa a 3 con pantry;
  receta con 2 ingredientes → 2; determinismo (dos llamadas iguales).
- Lint (SARIF `app/build/reports/lint-results-debug.sarif`): **0 errores** y ningún hallazgo nuevo bajo
  `app/src`.
- Disciplina de alcance por inspección: `grep -rn "RecipeArtwork(" app/src/main/java/com/sazon/app/ui`
  **no** aparece en las tarjetas ni en la rejilla; `grep -rn "0xFF" app/src/main/java/com/sazon/app`
  sin hex nuevos fuera de `ui/theme`; `git diff --name-only` = Expected File Changes (más spec,
  `feature_list.json`, `PROGRESS.md` y docs durables); **sin** diff en el Detalle, `DESIGN.md`,
  `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`.
- Capturas: `test -f docs/design/concepts/recipe-card-typographic/{recipe-card,favoritos-lista,
  favoritos-rejilla}/screen.png` (y `screen-dark.png`) → **existen**; el acabado se revisa contra ellas.
- **E2E / UI automatizada: no aplica** — `AGENTS.md` no automatiza UI de emulador. Esta feature se
  verifica por **lógica/estado en JVM** (selección de ingredientes, monograma, formatos) y contra las
  **capturas obligatorias**; la revisión visual fina (filete, marca de agua, tipografía, claro/oscuro)
  la hace la persona tras el merge.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal; conteos de suites/tests con delta vs `HEAD`.
- Nombres y aserciones de los tests nuevos/ampliados, con los **vectores** de monograma y de selección
  de ingredientes (incluidos los casos negativos no vacíos: vacío→`null`, receta con pantry).
- Resultado de `grep` de `RecipeArtwork(` en tarjetas, de hex sueltos, y de `git diff --name-only`;
  resultado de lint (SARIF).
- Confirmación de que las 6 capturas (`screen.png`/`screen-dark.png` × `recipe-card` con
  `screen-resultados*`, `favoritos-lista`, `favoritos-rejilla`) existen y se revisaron; mención del
  **I1** (KDoc de `tintedSurface` en Oklab) si se corrige aquí.
- Registro en `docs/technical-discovery.md` y `docs/adr/0006` (Estado + §6).

## Validator Checklist

- [ ] La implementación se queda en el alcance (sin Detalle, sin capa de fondo L2, sin retirar assets,
      sin dependencias/`init.sh`/escala del tema cambiados).
- [ ] Pasan los escenarios 1-6; la ficha no usa ninguna imagen y `RecipeArtwork` conserva firma y
      comportamiento.
- [ ] La **miniatura de Favoritos** es monograma con **regla terracota de 5px** (no por cocina) y el
      **filete de 6px** de las tarjetas es el degradado **por cocina** (`RecipeAccent`).
- [ ] Los ingredientes principales y el monograma son **deterministas** y con casos negativos no vacíos;
      el 16:10 se mantiene (una línea estrecho / tres filas ancho), sin decidir OQ6.
- [ ] Las capturas de `docs/design/concepts/recipe-card-typographic/` existen y el acabado las sigue.
- [ ] `bash init.sh` verde, evidencia (comandos, conteos, vectores, greps) presente.
- [ ] `docs/technical-discovery.md` y `docs/adr/0006` (Estado + §6) actualizados; `feature_list.json`/
      `PROGRESS.md` los actualiza el orquestador.
- [ ] Persistent E2E coverage: no aplica (justificado en el plan de verificación).
- [ ] No se añade comportamiento ajeno a la feature.

## Unknowns / Open Points

- **Monograma (resuelto por el implementer, 2026-10-14)**: la redacción del Escenario 5 era ambigua
  entre "las dos primeras palabras significativas" y "primera + última"; solo la segunda reproduce el
  vector `Arroz con pollo criollo`→`AC` de las capturas. El hallazgo queda documentado en el propio
  Escenario 5; los vectores, la lista de conectores y los casos negativos no cambian.
- **Nombre y ubicación** de las primitivas extraídas de `ui/artwork` y del contenido compartido de la
  ficha: provisionales; si se renombran, mantener el contrato y citarlo en la evidencia.
- **Rol de acento del tinte oscuro** (`start` o `end` del par de `RecipeAccent`): detalle visual sutil;
  el implementer lo fija en un único sitio y lo contrasta con las capturas (la verificación JVM no lo
  exige).
- **OQ6** (carrusel a 3:2) sigue **abierto**; no se decide aquí.
- **I1 diferido** de `card-typographic-tokens-and-design`: el KDoc de `tintedSurface` dice "sRGB"
  cuando `lerp` interpola en Oklab; si se toca, corregirlo aquí sin cambiar el comportamiento.

## Evidencia de autoverificación (implementer, 2026-10-14)

> Registro del implementer para el validador independiente; `feature_list.json` y `PROGRESS.md` los
> actualiza el orquestador al cerrar.

- `bash init.sh` → **exit 0** (gate no bloqueante: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos y sin tocar `init.sh`).
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.home.*"
  --rerun-tasks` → `BUILD SUCCESSFUL` (34/34 tareas), **51 tests / 0 fallos / 0 errores / 0 saltados**
  (`RecipeCardFormatTest` **13** = 4 previos + 9 nuevos; `RecipeMonogramTest` **7** nuevos).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **95 suites / 720 tests /
  0 fallos / 0 errores / 0 saltados**; baseline re-derivada en `HEAD` **94 suites / 704 tests** ⇒ delta
  **+1 suite / +16 tests**.
- Lint (`app/build/reports/lint-results-debug.sarif`): **0 errores**, **13 warnings** preexistentes de
  entorno (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2,
  `OldTargetApi` 1) y **0 bajo `app/src`**.
- Vectores del monograma (tests): `TP`, `AC`, `GA`, `CG`, `PT`; una palabra → una inicial; vacío /
  blanco / solo conectores → `null`; 3+ palabras significativas → primera + última (`TV`, `TP`);
  determinismo.
- Vectores de selección de ingredientes (tests): tortilla real (`Patata`/`Huevo`/`Cebolla` con
  `600 g`/`6 ud`/`1 ud`, sin sal ni aceite), 2 sin despensa + 3 de despensa → los 3 en orden, receta
  con 2 ingredientes → 2, receta sin ingredientes → lista vacía, determinismo.
- Alcance por inspección: `grep -rn "RecipeArtwork(" app/src` → la definición y **un único punto de
  uso** (`ui/detail/RecipeDetailScreen.kt`, de otra feature). `grep -rn "0xFF" app/src --include=*.kt
  | grep -v ui/theme` → solo dos máscaras de bytes preexistentes. `git status` / `git diff --name-only`
  = los **Expected File Changes** (con `RecipeArtworkPrimitives.kt` y `RecipeFicha.kt` como nombres
  reales de los provisionales) + esta spec + `docs/adr/0006` (Estado, §6 y §7) + el README de capturas
  + `docs/technical-discovery.md` + la corrección **I1** de KDoc en `ui/theme/CardTokens.kt`
  (comentario, sin cambio de comportamiento); **sin** diff en el Detalle, `DESIGN.md`,
  `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `assets/**`, `feature_list.json` ni
  `PROGRESS.md`.
- Primitivas: `RecipeGlyphCanvas`, `accentColors` y `drawLeaf` se movieron **verbatim** a
  `ui/artwork/RecipeArtworkPrimitives.kt` (cuerpos comparados y **idénticos**); `RecipeArtwork` solo
  cambia `Brush.linearGradient(accentColors(...))` → `recipeAccentBrush(...)` y `RecipeGlyphCanvas` →
  `RecipeGlyphArtwork`, conservando firma y comportamiento.
- Capturas revisadas (las 8 de la feature existen): `recipe-card/{screen,screen-dark,
  screen-resultados,screen-resultados-dark}.png`, `favoritos-lista/{screen,screen-dark}.png`,
  `favoritos-rejilla/{screen,screen-dark}.png`.
- Las dos reconciliaciones de redacción (monograma y celda compacta de la rejilla) quedan documentadas
  en los Escenarios 5 y 3 de esta spec.
- Revisión visual fina (filete, marca de agua, tipografía, claro/oscuro) y prueba en emulador: **manual
  de la persona** tras el merge (no automatizable desde WSL, `AGENTS.md`).
- **Sin commit, push ni PR** (los hace el orquestador tras el `accept`).
