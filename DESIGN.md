---
name: Sazón
description: App Android de recetas generadas por IA a partir de los ingredientes del usuario. Identidad cálida y apetitosa, tipo libro de cocina moderno.
designAssets:
  sourceOfTruth: []
  generatedConcepts: []
colors:
  primary: "#D9552B"
  onPrimary: "#FFFFFF"
  primaryContainer: "#FFDCCF"
  secondary: "#5F7A3D"
  accent: "#F2A93B"
  background: "#FFF8F0"
  surface: "#FFFFFF"
  text: "#2B2118"
  outline: "#E8DDD3"
  success: "#4C7A34"
  error: "#B3261E"
  darkBackground: "#241C16"
  darkSurface: "#312720"
  darkCardSurface: "#3A2E26"
  darkCardOutline: "#7D6A5C"
typography:
  display:
    fontFamily: Manrope
    fontSize: 32sp
    fontWeight: 700
  h1:
    fontFamily: Manrope
    fontSize: 28sp
    fontWeight: 600
  body:
    fontFamily: Inter
    fontSize: 16sp
    fontWeight: 400
  caption:
    fontFamily: Inter
    fontSize: 12sp
    fontWeight: 500
rounded:
  sm: 16px
  md: 24px
  full: 999px
spacing:
  sm: 12px
  md: 16px
  lg: 24px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.full}"
    height: 52dp
  button-secondary:
    backgroundColor: transparent
    textColor: "{colors.text}"
    borderColor: "{colors.outline}"
    rounded: "{rounded.full}"
  chip-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.full}"
  chip-selected-secondary:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.full}"
  recipe-card:
    backgroundColor: "{colors.surface}"
    backgroundDark: "{colors.darkCardSurface}"
    borderDark: "{colors.darkCardOutline}"
    rounded: "{rounded.md}"
    accentRule: "6px, degradado por cocina (RecipeAccent)"
    shadow: "y=4 blur=16 alpha=6%"
  recipe-card-image:
    position: "capa de fondo del hueco, solo con el ajuste de imágenes encendido"
    ratio: "16:9 (tarjeta y héroe; estructura L2 unificada con el ajuste encendido o apagado)"
    rounded: "{rounded.md}"
    scrim: "degradado de contraste bajo el texto blanco (claro rgba(20,12,8,.42/.18/.78), oscuro rgba(10,6,4,.5/.22/.82))"
    overImageText: "#FFFFFF"
    overImageTextVariant: "rgba(255,255,255,.86)"
    overImageHairline: "rgba(255,255,255,.32)"
  recipe-tile-favorites:
    size: "96x96"
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    content: "monograma de iniciales del plato"
  bottom-nav:
    backgroundColor: "{colors.surface}"
    activeColor: "{colors.primary}"
    inactiveColor: "#9A8C7F"
    height: 80dp
---

# Design Direction

## Overview

"Sazón" debe sentirse como un libro de cocina moderno: **cálido, apetitoso y limpio**, nunca clínico
ni corporativo. La identidad visual se apoya en tres ideas: fondo crema, tarjetas blancas con esquinas
muy redondeadas, y un naranja terracota reservado para la acción principal.

**La imagen de una receta es tipográfica** (ADR 0006). La tarjeta es una **ficha editorial**: categoría
en versalitas, título grande y los **tres ingredientes principales** con su cantidad, más un filete de
color por cocina. Nunca una foto por defecto que no corresponda al plato. Si el ajuste **"Generar
imágenes del plato"** está encendido y hay imagen disponible, esa imagen es la **capa de fondo** del
hueco (variante **L2**) y la tipografía se queda encima. La dirección anterior —"la foto del plato como
protagonista"— queda **sustituida**; ver ADR 0006.

**Estructura de tarjeta unificada (2026-10-14, revisión de la persona).** La variante **L2** deja de
estar supeditada al ajuste de imágenes: la tarjeta usa **siempre** el hueco **16:9** con la categoría y
el título encima y, debajo, la **banda sólida** con los ingredientes y los metadatos. Con el ajuste
apagado (o cuando no hay imagen) solo **desaparece la capa de fondo**; no cambia el layout. Es una
decisión sobre la **forma**, no sobre la fotografía: la ficha sigue siendo el fondo por defecto. Aplica
a Inicio ("Sugeridas para ti" y "Tus recetas"), Resultados y la rejilla de Favoritos
(`recipe-card-l2-unified`).

Esta dirección nace del prompt de Stitch y de las decisiones de discovery. **Es la fuente de verdad
de la UI**: si un mockup generado contradice estos tokens o estas reglas, manda este documento.

## Existing Design Assets

- Pantallas de **Google Stitch** ya generadas por el usuario, basadas en un prompt previo.
- **Autoridad: obsoletas / a regenerar.** No se importan al repositorio. Deben regenerarse a partir de
  este documento y del prompt derivado (ver Open Design Questions).
- Cuando se regeneren, las exportaciones aceptadas se guardarán en `docs/design/concepts/` y se
  marcarán aquí como referencia de dirección, no como especificación pixel-perfect.

## Generated Concept Images

La política de capturas de UI (PR #37) exige una captura por **pantalla** en
`docs/design/concepts/<feature-id>/<pantalla>/` (el `.png`, el `.html` y el `design.md` del export de
Stitch). La primera captura aceptada es `results-screen/resultados/` (dirección visual de la pantalla
de Resultados, registrada abajo). Las features de UI aceptadas **antes** de esa política tienen
pendiente este **backfill**; la petición concreta de cada una vive en su
`docs/design/concepts/<feature-id>/README.md`.

Capturas aceptadas (dirección visual, no especificación pixel-perfect):

- `results-screen`: `resultados`
- `recipe-detail-screen`: `detalle-receta`
- `favorites`: `favoritos`
- `activity-summary`: `perfil-resumen`
- `ai-search`: `busqueda-ia`
- `cooking-mode`: `modo-cocina`
- `recipe-photos`: `fotos-semillas`, `fotos-categorias`

**Capturas de `recipe-photos` (2026-10-13, issue #83):** no son pantallas de la app sino **láminas de
fotos** (una imagen `screen.png` + `code.html` + `DESIGN.md` por lote) y fueron a la vez la **fuente** de los
31 assets y la **dirección visual** del acabado. `fotos-semillas` expone las 12 fotos de las recetas
semilla **en el orden** del `README.md` (el `alt` no es la clave: la clave va en el comentario
`<!-- NN. <clave> -->`); `fotos-categorias` expone las 18 `cat-<categoryId>` **más** `default`. La
procedencia, el mapeo y el procedimiento de descarga viven en
`docs/design/concepts/recipe-photos/README.md` (v2) y el prompt en `stitch-prompt.md`. **Retirada
(2026-10-14, `retire-recipe-photos`, ADR 0006):** los 30 assets de `seed/` y `category/` salieron del
APK y `scripts/fetch-recipe-photos.sh` se eliminó; las capturas se conservan **solo como procedencia**
del ancla de estilo `img/recipes/default.jpg` (137.359 B), que no se muestra en ninguna pantalla.

Backfill pendiente:

- `design-tokens-and-theme`: `design-system`
- `onboarding-shell-and-identity`: `bienvenida`
- `onboarding-diet-style-cuisines`: `dieta`, `estilo`, `cocinas`
- `onboarding-ingredients-safety-kitchen`: `ingredientes`, `alergias`, `tu-cocina`
- `home-offline-blocks`: `home`, `home-sin-conexion`
- `local-result-filters`: `filtros-tus-recetas`
- `generator-ingredients-step`: `generador-ingredientes`
- `generator-settings-step`: `generador-ajustes`
- `generation-flow-with-validation`: `generador-cargando`, `resultados-preview`
- `profile-screen`: `perfil`, `perfil-editar`
- `manual-suggestions-refresh`: `perfil-sugerencias`

**Excepción de capturas (desviación documentada, issue #77):** `recipe-artwork` **no** genera capturas
nuevas ni carpeta `docs/design/concepts/recipe-artwork/`. El placeholder ya estaba definido en este
documento y en el prompt de Stitch como el **fallback** del diseño ("soft peach-to-olive gradient block
with a thin line-art plate icon"), y esa feature solo lo **extiende** por categoría y cocina. La
dirección visual aplicable es la tabla de la sección **Imagen de receta** de `Components` (más abajo),
que es contra la que se revisa el acabado en el emulador.

**Excepción de capturas (desviación documentada, issue #85):** `favorites-sort-controls` **no** genera
capturas nuevas ni carpeta `docs/design/concepts/favorites-sort-controls/`. El toggle lista/rejilla
icónico ya está en la captura aceptada `favorites/favoritos` y el único cambio de microcopy
("Recientes") es un **acortamiento** del label existente. La dirección visual aplicable es esa captura
más las secciones **FilterChip** y **Toggle lista/rejilla de Favoritos** de `Components` (más abajo),
contra las que se revisa el acabado en el emulador.

**Excepción de capturas (desviación documentada, issue #86):** `profile-stats-heading` **no** genera
capturas nuevas ni carpeta `docs/design/concepts/profile-stats-heading/`. Es un cambio de **microcopy de
una sola cadena**: la dirección visual aplicable es la captura aceptada `activity-summary:
perfil-resumen`, que titula la sección "Resumen", y el **titular visible** del bloque de cifras del
Perfil pasa a ser **"Estadísticas"** para explicitar que las dos tarjetas ("recetas generadas" y
"favoritas") son estadística informativa y **no navegable**. El resto de la sección (layout, tokens,
orden de secciones y tarjetas `Surface`) no cambia.

**Excepción de capturas (desviación documentada, issue #91):** `home-recently-opened-group` **no** genera
capturas nuevas ni carpeta `docs/design/concepts/home-recently-opened-group/`. Es una correctiva que
**elimina** un grupo vacío del bloque "Tus recetas" (no introduce pantalla, layout, token ni estado
nuevo): el bloque **solo lista los grupos con contenido** (hoy, "Guardadas") y conserva la fila de
filtros locales, la línea "No hay recetas con este filtro." y el estado vacío "Cuando guardes una receta
aparecerá aquí.". La dirección visual aplicable es el **backfill pendiente** de `home-offline-blocks`
(`home`, `home-sin-conexion`, ver `docs/design/concepts/home-offline-blocks/README.md`) más la sección
**Core Screens** (Home, arriba) y `Components`, contra las que se revisa el acabado en el emulador.

**Excepción de capturas (desviación documentada, issue #98):** `dish-image-progressive-ui` **no** genera
capturas nuevas ni carpeta `docs/design/concepts/dish-image-progressive-ui/`. Es la **mitad de
integración** de la fase 4 de `docs/ai-dish-images-proposal.md`: **no** introduce pantalla, layout,
geometría, token, microcopy ni estado visual nuevos — reutiliza las pantallas ya aceptadas
(`results-screen: resultados`, `recipe-detail-screen: detalle-receta`, `favorites: favoritos` y la Home
de `home-offline-blocks`, backfill pendiente) y se limita a **cambiar de dónde sale la imagen**
(local → generada) con un crossfade estándar. La dirección visual aplicable son esas capturas aceptadas
más la sección **Imagen de receta** de `Components` (más abajo), contra las que se revisa el acabado en
el emulador tras el merge.

**Excepción de capturas (desviación documentada, issue #101):** `dish-image-settings-toggle` **no**
genera capturas nuevas ni carpeta `docs/design/concepts/dish-image-settings-toggle/`. Es la **mitad de
ajuste** de la fase 4 de `docs/ai-dish-images-proposal.md`: **no** introduce pantalla, navegación,
layout, geometría ni estilo nuevos — **reutiliza el estilo de ajustes ya existente** de la pantalla
Perfil (título `titleMedium`, fila label + resumen, tokens del tema) y solo añade un `Switch` de
`material3` y una línea de microcopy con el **tope por sesión**. La dirección visual aplicable es este
documento más las capturas aceptadas del Perfil (`activity-summary: perfil-resumen`) y el **backfill
pendiente** de `profile-screen` (`perfil`, `perfil-editar`; ver
`docs/design/concepts/profile-screen/README.md`), contra las que se revisa el acabado en el emulador
tras el merge.

**Excepción de capturas (desviación documentada, issue #110):** `home-suggestions-personalized` **no**
genera capturas nuevas ni carpeta `docs/design/concepts/home-suggestions-personalized/`. Es una
**correctiva de `home-offline-blocks`** que **no** cambia el diseño: no introduce pantalla, layout,
geometría, token, microcopy ni estado visual nuevos — reutiliza el carrusel ya existente ("Sugeridas
para ti" + "basado en tus gustos" y las tarjetas `RecipeCard`, hoy en estructura L2 a 16:9) y solo cambia **qué**
recetas entran y **en qué orden**. Comportamiento vigente del carrusel: muestra **como máximo 6**
sugerencias **seguras** (descarta con el `RecipeValidator` las que incumplen una Restricción dura o el
Patrón de dieta, semilla y `ai` por igual) **ordenadas por relevancia con el Perfil** (`MatchScore`:
ingredientes preferidos, cocinas favoritas, franja de tiempo y dificultad, penalizando —nunca
descartando— los ingredientes que el usuario evita), **sin** materializar el score, así que **no**
aparece la píldora de coincidencia (`matchScore` sigue `null`); el respaldo de semilla offline (Lote
nulo) pasa por el mismo camino (filtro → orden → tope) y un Lote resuelto vacío sigue vacío. La
dirección visual aplicable es el **backfill pendiente** de `home-offline-blocks` (`home`,
`home-sin-conexion`; ver `docs/design/concepts/home-offline-blocks/README.md`) más la sección **Core
Screens** (Home, arriba) y `Components`, contra las que se revisa el acabado en el emulador tras el
merge.

**Excepción de capturas (desviación documentada, issues #129–#132):** las cuatro features de la
**revisión de diseño** del 2026-10-14 (`recipe-card-l2-unified`, `favorites-tile-contrast`,
`home-tus-recetas-ver-todas` y `detail-hero-full-bleed`) **no** generan capturas nuevas ni carpeta
`docs/design/concepts/<feature-id>/`. Autorización de la persona: **no hay diseño nuevo que revisar**;
son la aplicación de un diseño **ya aceptado** o la retirada de lo no usado, más una correctiva de
color. La dirección visual aplicable son las capturas ya aceptadas más este documento, contra los que la
persona revisa el acabado en el emulador tras el merge:
- `recipe-card-l2-unified` **aplica** la variante **L2** ya aceptada (`dish-artwork-as-card-background`)
  y **retira** la estructura no usada (el hueco 16:10 sin ajuste) → capturas de referencia:
  `recipe-card-typographic/{recipe-card,resultados,favoritos-rejilla}` y
  `dish-artwork-as-card-background/{tarjeta-con-imagen}`.
- `detail-hero-full-bleed` **reutiliza** el hero ya aceptado (`recipe-detail-typographic-header`) y solo
  lo lleva **de borde a borde** → capturas: `recipe-detail-typographic-header/detalle-receta` y
  `dish-artwork-as-card-background/detalle-con-imagen`.
- `home-tus-recetas-ver-todas` es una **correctiva** del bloque "Tus recetas" (quita los chips, deja las
  6 últimas y un botón "Ver todas") → captura: `favorites/favoritos` y la Home del backfill de
  `home-offline-blocks`.
- `favorites-tile-contrast` es una **correctiva de color** de la miniatura (fondo `primary` + monograma
  `onPrimary`) → captura: `favorites/favoritos` y la sección `Components` de este documento.

Las cuatro quedan **`not_started`** (listas para implementar) y **no** se crea
`docs/design/concepts/<feature-id>/`.

Al aceptar cada exportación, registrar aquí su estado como **dirección visual** (no especificación
pixel-perfect).

**Dirección tipográfica: capturas desde los prototipos locales (ADR 0006, 2026-10-13, autorizado):**
la dirección tipográfica (tarjeta **C3**,
oscuro **B+C**, imagen **L2**) se decidió sobre **prototipos HTML locales**, no sobre export de Stitch.
Los prototipos que sostienen la decisión están versionados en
`docs/design/concepts/typographic-cards/`. Las **capturas `.png` por pantalla** están **aportadas**
(2026-10-14) en `docs/design/concepts/<feature-id>/<pantalla>/` —siete pantallas, en claro y oscuro,
renderizadas de esos prototipos con Chrome headless a 390×844 y 2×— para `card-typographic-tokens-and-design`
(`tarjeta-oscura`), `recipe-card-typographic` (`recipe-card` —Inicio y Resultados—, `favoritos-lista`,
`favoritos-rejilla`), `recipe-detail-typographic-header` (`detalle-receta`) y
`dish-artwork-as-card-background` (`tarjeta-con-imagen`, `detalle-con-imagen`). Es una **desviación de la
política de capturas** (que parte de export de Stitch) **autorizada por la persona** (2026-10-13, sesión
101): el origen son los prototipos locales, no Stitch.

## Product Feel

- **Editorial antes que fotográfico**: el apetito lo dan la jerarquía tipográfica, el color cálido y la
  claridad de la ficha (qué plato es y qué lleva). La fotografía es opcional y, cuando aparece, es
  **fondo**, no protagonista.
- **Cercano, no infantil**: redondeado y amable, pero con tipografía y jerarquía de nivel editorial.
- **Tranquilo**: mucho aire, pocos colores, sin adornos innecesarios.
- **Táctil**: botones altos, chips tipo píldora, sombras muy suaves.

## Colors

| Rol | Hex | Uso |
|---|---|---|
| Primary | `#D9552B` | Acción principal, estado activo, coincidencia |
| Primary container | `#FFDCCF` | Fondos suaves de acento |
| Secondary | `#5F7A3D` | Ingredientes frescos, vegetal, etiquetas saludables |
| Accent | `#F2A93B` | Temporizadores, duración de pasos, avisos de alérgenos |
| Background | `#FFF8F0` | Fondo de pantalla |
| Surface | `#FFFFFF` | Tarjetas, hojas, barras |
| Text | `#2B2118` | Texto principal |
| Outline | `#E8DDD3` | Hairlines y separadores |
| Avatar (iniciales) | 8 tonos cálidos | Identidad; lista canónica en `avatarColors` de `docs/domain/taxonomy.json` |
| Tarjeta (modo oscuro) | `#3A2E26` | Superficie de las tarjetas en oscuro; **3,26:1** con `darkBackground` (ADR 0006) |
| Borde de tarjeta (modo oscuro) | `#7D6A5C` | Separa la tarjeta del fondo en oscuro; cumple el 3:1 de WCAG 1.4.11 |

Regla de uso: el terracota se reserva para **una** acción principal por pantalla. Si todo es naranja,
nada lo es.

## Typography

- **Títulos**: Manrope, SemiBold/Bold, 28–32sp, tracking ligeramente cerrado.
- **Cuerpo**: Inter, 14–16sp, interlineado 1.5.
- **Números** (tiempos, cantidades, comensales): cifras tabulares, peso Medium. Es información que el
  usuario compara de un vistazo; debe alinearse.
- Máximo dos familias tipográficas.

## Layout

- Retrato, un solo pulgar. Padding lateral 16px, ritmo vertical 24px, interior de tarjeta 12px.
- Una idea por sección; los títulos de sección son pequeños y en mayúsculas discretas, no gigantes.
- Carruseles horizontales para sugerencias, listas verticales para resultados y favoritos.
- Barras de acción pegadas al fondo (`sticky`) en el detalle y en el asistente.

## Shapes

- Tarjetas e imágenes: 24px. Inputs: 16px. Chips y botones: píldora completa.
- Sombras solo suaves y de baja opacidad; nunca bordes duros para separar tarjetas.
- Superficies superpuestas **con el contenedor de fondo** (el apilado **hero + tira de metadatos** del
  Detalle: hero tipográfico de 240 con esquinas inferiores de 24 y, debajo, la tira de metadatos con las
  mismas esquinas y solo la sombra suave) para dar profundidad sin sombras fuertes. Ya **no** hay tarjeta
  blanca que suba sobre la foto del Detalle (ADR 0006). El hero y la tira de metadatos van **de borde a
  borde** (sin padding lateral de 16dp) desde la revisión de diseño del 2026-10-14
  (`detail-hero-full-bleed`).

## Components

- **RecipeCard** (ficha editorial, ADR 0006): **sin foto por defecto** y **estructura L2 siempre**
  (hueco **16:9** + banda sólida, unificado el 2026-10-14). En el hueco, con la superficie de la
  tarjeta como base: categoría en versalitas (`onSurfaceVariant`, tracking amplio) y **título** en
  Manrope Bold (21sp en el carrusel, 26sp en Resultados) a 2 líneas máximo, con la **marca de agua** del
  glifo al 12 % y el **filete de 6px** al pie. Debajo del hueco, la **banda sólida** con los **tres
  ingredientes principales** con su cantidad en cifras tabulares (tres filas con hairline entre ellas
  cuando hay ancho; una línea con separadores `·` cuando la tarjeta es estrecha), la fila
  `tiempo · dificultad · comensales` y el **porcentaje de coincidencia** en terracota. Con el ajuste de
  imágenes encendido, la imagen generada es la **capa de fondo** del hueco (categoría y título pasan al
  tratamiento sobre imagen); la geometría no cambia. En oscuro: superficie `darkCardSurface` + borde
  `darkCardOutline` (3,26:1) + tinte cálido de la cocina.
- **RecipeListCard** (horizontal): **miniatura 96x96 con el monograma** de iniciales sobre `primary`
  (`onPrimary` para las iniciales; 96x96, esquina 24) para que **resalte** del fondo de la fila en claro
  y en oscuro (≈3,97:1 / ≈3,31:1), texto a la derecha, corazón a la derecha.
- **Imagen de receta** (`ui.artwork.RecipeArtwork`, features `recipe-artwork`, `recipe-photos`,
  `dish-image-*` y `dish-artwork-as-card-background`): la **capa base es la ficha tipográfica** del
  `RecipeCard`/hero, no una foto. `RecipeArtwork` es el **proveedor de la capa de fondo** del hueco y del
  hero (implementado, ADR 0006) y solo aporta algo cuando existe imagen:
  - **Variante L2** (la aceptada e **implementada**, ADR 0006): la imagen ocupa la parte superior (16:9 en
    las tarjetas y en el héroe) con un `scrim` de contraste y la **categoría y el título encima**; los
    ingredientes con cantidad y los metadatos van en una **banda sólida** con los tokens de superficie.
    Sus **dos** consumidores son el hueco de `RecipeFicha` (tarjeta estrecha/ancha/rejilla, banda sólida) y
    el hero del Detalle; `MainShell` reparte el interruptor como `imageBackground` (default `false` = ficha
    y hero aceptados).
  - **Sin imagen**: queda la ficha tipográfica (color de superficie o degradado del hero), con una
    **marca de agua** del glifo de la categoría al
    12 % en la esquina inferior derecha, para que dos recetas de la misma categoría y cocina no se vean
    idénticas.
  - **Cuándo hay imagen**: solo si el ajuste **"Generar imágenes del plato"** está encendido y el
    resolver devuelve una (memoria → disco → semilla → generador). Apagado = `null`, sin leer caché, sin
    red y sin cuota. Degradación **silenciosa**: si falla, tarda, se cancela o se agota el tope por
    sesión, se queda la tipografía, sin microcopy ni estado de error.
  - La imagen entra con el **crossfade de 250 ms** (`DishImageArtworkPolicy.CROSSFADE_MILLIS`) sobre la
    tipografía: **nunca hay hueco, frame en blanco ni salto de layout**.
  - **`scrim` y texto sobre imagen** (`ui/theme/CardImageOverlay.kt`, único sitio con esos valores; 0 hex
    en `ui/artwork`): el `scrim` es un degradado vertical de contraste —claro
    `rgba(20,12,8,.42)`/`.18` al 52 %/`.78`, oscuro `rgba(10,6,4,.5)`/`.22`/`.82`— y la tipografía sobre
    la imagen usa **blanco** (`#FFFFFF`), blanco al 86 % para la categoría en versalitas y hairline blanco
    al 32 % para las filas de ingredientes del hero. El ratio **16:9** del hueco L2 y las decisiones
    (`headingOnImage`, `watermarkVisible`, alfa derivado) son la lógica pura
    `ui/artwork/DishImageArtworkOverlay`.

  > Los apartados siguientes conservan la referencia de la **identidad visual** (glifo + acento) y de la
  > cadena de imagen; la **cadena local de fotos se retiró por completo** con `retire-recipe-photos`
  > (ADR 0006).
  - **Cadena de respaldo local (retirada, ADR 0006)**: `data.RecipePhotoAssets` resolvía foto propia de
    la receta semilla (`seed/<id>.<ext>`) → foto de su categoría (`category/cat-<categoryId>.<ext>`) →
    foto genérica (`default.<ext>`) → glifo. Con `retire-recipe-photos` (2026-10-14) esa cadena
    **desapareció** y con ella el seam Compose `LocalRecipePhotoSource`, la costura Android
    `data.AssetRecipePhotoSource` y los 30 assets de `seed/` y `category/` (**5.954.912 B ≈ 5,68 MiB**),
    porque la foto de categoría es la que no corresponde al plato. Se **conserva** `default.<ext>`
    (`img/recipes/default.jpg`, 137.359 B) como ancla de estilo de `dish-style-reference`, que no se
    muestra en ninguna pantalla (OQ7). `data.RecipePhotoAssets` queda solo con `ROOT`/`DEFAULT_KEY`/
    `EXTENSIONS` (el ancla y el orden de extensiones que comparten las cadenas de assets vivas).
  - La **imagen** se recorta con `ContentScale.Crop` y conserva la geometría aceptada (16:9 del hueco L2,
    hero de 240 del Detalle); no cambian esquinas ni tamaños. La imagen
    es **decorativa** (`contentDescription = null`): el título de la receta ya se anuncia.
  - **Identidad visual de reserva** (glifo + acento): el **glifo** identifica la categoría (`categoryId`) y
    el **acento del degradado** la cocina (`cuisineId`), de modo que dos recetas de la misma categoría y
    distinta cocina no se ven iguales:
    - Glifo por categoría (18, la Taxonomía real): `pasta`, `rice`, `soup_cream`, `salad`, `meat`,
      `poultry`, `fish_seafood`, `vegetarian`, `vegan`, `legume_stew`, `pizza_bread`, `egg_breakfast`,
      `sandwich_wrap`, `tapas_appetizer`, `dessert`, `smoothie_drink`, `casserole_oven` y `other` → plato.
    - Acento por cocina (10, la Taxonomía real): el degradado va de `start` (arriba) a `end` (abajo),
      siempre con roles del tema (nunca hex propios), así que el modo oscuro usa los `*Container`
      oscuros. Tabla:

      | `cuisineId` | `RecipeAccent` | `start` → `end` |
      |---|---|---|
      | `spanish` | `EMBER` | `surfaceVariant` → `primaryContainer` |
      | `mediterranean` | `GROVE` | `secondaryContainer` → `primaryContainer` |
      | `italian` | `TOMATO` | `primaryContainer` → `tertiaryContainer` |
      | `asian` | `GINGER` | `tertiaryContainer` → `primaryContainer` |
      | `japanese` | `SESAME` | `surfaceVariant` → `secondaryContainer` |
      | `indian` | `CURRY` | `tertiaryContainer` → `secondaryContainer` |
      | `mexican` | `CHILI` | `primaryContainer` → `surfaceVariant` |
      | `american` | `HONEY` | `tertiaryContainer` → `surfaceVariant` |
      | `middle_eastern` | `CLAY` | `secondaryContainer` → `surfaceVariant` |
      | `latin` | `ROSE` | `secondaryContainer` → `tertiaryContainer` |
      | (nulo/vacío/desconocido) | `DEFAULT` | `primaryContainer` → `secondaryContainer` |

    - **Fallback total**: un id nulo, vacío o desconocido pinta el **plato** (dos círculos concéntricos)
      con el degradado **melocotón→oliva** (`DEFAULT`) de siempre; nunca un bloque vacío ni una excepción.
    - El glifo se tiñe con `onSurfaceVariant` y el componente no fija forma: la **marca de agua** mide
      **150 dp** en la tarjeta y **190 dp** en el hero (desbordando por la esquina inferior derecha,
      recortada por el `clip`); el filete de 6 px va al pie del hueco y del hero.
  - La imagen se inyecta por el seam `LocalGeneratedRecipeArtworkSource`, así que los llamantes no conocen
    ni el asset ni Android; es la **única** fuente de imagen de una receta (el seam de la foto local se
    retiró con `retire-recipe-photos`). Ver
    `docs/adr/0004-recipe-photos-as-assets.md` y `docs/technical-discovery.md`.
  - **Carga progresiva y crossfade estándar** (fase 4, `dish-image-progressive-ui`): `RecipeArtwork` lee
    el seam de la **imagen generada** (`LocalGeneratedRecipeArtworkSource`; por defecto no produce imagen),
    pinta la imagen con `ContentScale.Crop` y **su `scrim`** con un alfa animado (`animateFloatAsState`,
    crossfade de **250 ms**, `DishImageArtworkPolicy.CROSSFADE_MILLIS`, en un único sitio) y deja la
    tipografía al **slot de contenido** del llamante, que recibe la capa resuelta. La **capa base**
    (ficha/hero tipográficos) se pinta **siempre**: nunca hay
    hueco ni frame en blanco. Si la generación falla, tarda, se cancela o no hay red, se **conserva
    la tipografía** (degradación silenciosa, contrato `null` del ADR 0005). No hay microcopy ni estado
    visual nuevos; la firma `(recipe, modifier, glyphSize)` y la geometría no cambian y los consumidores
    son **dos** (hueco y hero).
  - **Cadena de imagen en runtime**: `ui.artwork.RecipeArtwork` → `LocalGeneratedRecipeArtworkSource` →
    `DishImageArtworkLoader` (límite de **2** resoluciones en vuelo y LRU de **16** imágenes
    decodificadas) → `data.image.DishImageResolver` → `FileDishImageCache` (80 MiB en
    `cacheDir/dish-images`) / `MemoryDishImageCache` / **assets semilla** /
    `data.ai.image.DevExpertDishImageGenerator`. La cadena local
    (`LocalRecipePhotoSource` → `data.AssetRecipePhotoSource`) **ya no existe** (retirada por
    `retire-recipe-photos`): la ficha tipográfica es el único respaldo sin imagen. El proveedor del seam
    (en `MainActivity`) **gatea
    el interruptor** (`DishImageSettings.generatedIfEnabled`: apagado = `null` sin leer caché ni
    generar) y el generador real va envuelto por `GatedDishImageGenerator`, que impone el **tope por
    sesión** (`DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION`) en el borde de generación:
    agotado = `null` sin invocar al delegado. Ver `docs/adr/0005-ai-dish-images.md` y
    `docs/technical-discovery.md`.
- **Ajuste de imagen de plato** (`ui.profile.DishImageSettingsSection`, feature
  `dish-image-settings-toggle`): sección al **final** del Perfil con el estilo de sección ya existente
  (título `titleMedium`; fila label `bodyLarge` + resumen `bodyMedium`/`onSurfaceVariant` + helper
  `labelSmall`; `SazonSpacing.md`; ≥48dp). Interruptor **"Generar imágenes del plato"** (`Switch` de
  `material3`) en una fila `toggleable` con `role = Role.Switch`, de modo que la fila entera es el
  control y el `Switch` solo refleja el estado (no depende solo del color). El resumen del **tope por
  sesión** sale de un objeto puro (`DishImageSettingsCopy`): "Quedan N de 12 imágenes en esta sesión.",
  "Queda 1 de 12 imágenes en esta sesión." (singular), "Tope de esta sesión alcanzado (12 de 12). Se
  reanudará al reiniciar la app." y "Desactivado: las recetas se muestran sin imagen."; helper "Las imágenes
  nuevas se generan con IA y consumen cuota; las que ya existen se reutilizan.".
- **FilterChip**: fila desplazable; seleccionado relleno de terracota. En Favoritos el chip de orden
  por defecto se lee **"Recientes"** (microcopy corto, sin recorte; feature `favorites-sort-controls`).
- **Toggle lista/rejilla de Favoritos** (`ViewModeToggle`, feature `favorites-sort-controls`): píldora
  `surface` con sombra suave que contiene **dos botones circulares de 28dp solo con icono**; el activo
  se rellena en `primary` (terracota) con el glifo en `onPrimary` y el inactivo no lleva relleno y usa
  el glifo en `onSurfaceVariant`. Los glifos de lista y rejilla se dibujan con Compose (sin dependencia
  de iconos) y cada botón se anuncia con descripción ("Lista"/"Rejilla") y estado seleccionado por rol
  de radio, para no depender solo del color.
- **Stepper** de comensales: `− 2 +` con cifras tabulares.
- **Paso de receta**: línea de tiempo vertical con círculos numerados en terracota y una píldora
  ámbar con la duración.
- **Aviso de alérgenos**: tarjeta con tinte ámbar, nunca roja. Es información de seguridad, no una
  alarma.
- **Estados vacío/carga/error**: ilustración lineal sencilla + titular cálido + una única acción. El
  esqueleto de carga usa bloques grises redondeados con la geometría final.
- **Aviso de modo degradado** (sin conexión o sin IA): franja ámbar discreta bajo la cabecera; el
  contenido local se muestra igual. Nunca una pantalla vacía.
- **Buscador con IA**: píldora en la cabecera de la Home, con icono de destello para distinguirlo de
  un buscador normal. Al enviar, lleva a Resultados con la consulta como subtítulo.

## Core Screens

1. **Onboarding** (7 pasos, obligatorio, sin barra inferior): bienvenida con **nombre y color de
   avatar**, patrón de dieta, preferencias de estilo, cocinas favoritas, ingredientes que me gustan +
   ingredientes que evito, restricciones duras, y "tu cocina" (nivel, tiempo disponible y comensales
   habituales).
2. **Home** con cinco bloques: cabecera (saludo y avatar de iniciales), **buscador con IA** —el único
   bloque que necesita red y que debe degradar con aviso—, tarjeta-héroe "¿Qué tienes en la nevera?"
   que abre el Generador, carrusel "Sugeridas para ti" (**como máximo 6** sugerencias seguras
   —filtrando restricciones duras y patrón de dieta— ordenadas por relevancia con el Perfil; **sin**
   píldora de coincidencia, ver la excepción de capturas de `home-suggestions-personalized` más arriba),
   y el bloque **"Tus recetas"**, que **solo lista
   los grupos con contenido** (hoy, "Guardadas", alimentado por los Favoritos) y, desde la revisión de
   diseño del 2026-10-14 (`home-tus-recetas-ver-todas`), muestra las **6 últimas** guardadas con un botón
   **"Ver todas"** que abre la pestaña Favoritos (ya **no** hay chips de filtro en la Home; el filtro
   local sigue en Resultados), con el estado vacío "Cuando guardes una receta aparecerá aquí."
   (se retira la línea "No hay recetas con este filtro." de la Home).
3. **Generador**: paso 1 ingredientes (chips + input), paso 2 ajustes agrupados en tarjetas, estado
   de generación con animación cálida.
4. **Resultados**: chips de orden + lista de **RecipeCard tipográfica** (título, tres ingredientes con
   cantidad, metadatos y coincidencia); estados vacío, error y **modo semilla** (sin IA disponible, se
   muestran las recetas locales).
5. **Detalle**: **hero tipográfico** (categoría + título + tres ingredientes con cantidad, con la marca
   de agua del glifo y el filete de la cocina) y, solo con el ajuste de imágenes encendido, la foto como
   **capa de fondo** de ese hero; tira de metadatos (Tiempo · Dificultad · Comensales · Calorías),
   pestañas Ingredientes · Pasos · Notas, barra inferior con "Guardar" y "Empezar a cocinar". Ya **no
   hay** tarjeta blanca superpuesta sobre la foto: el hero y la tira de metadatos ocupan su lugar, y
   desde la revisión de diseño del 2026-10-14 (`detail-hero-full-bleed`) van **de borde a borde**.
6. **Favoritos**: búsqueda, orden (chip por defecto **"Recientes"**) con el toggle lista/rejilla
   icónico, lista con **miniaturas de monograma** (96x96 sobre `primary`, con las iniciales en
   `onPrimary`, para que resalten del fondo), rejilla con las tarjetas tipográficas (L2), estado
   vacío con nota "Disponibles sin conexión".
7. **Perfil**: identidad (nombre y color de avatar con vista previa), preferencias editables
   (alimentación, estilo, cocinas, ingredientes que me gustan e ingredientes que evito), comensales
   habituales, tarjeta ámbar de alérgenos, bloque de cifras titulado **"Estadísticas"** (recetas
   generadas y favoritas, informativas y **no navegables**), acción "Actualizar sugerencias" con la
   fecha del último refresco, y al final la sección **"Imágenes del plato"**: interruptor **"Generar
   imágenes del plato"** con el tope **por sesión** visible ("Quedan N de 12 imágenes en esta sesión.")
   y el helper de cuota. No es una preferencia culinaria: es un ajuste de app, colocado al final para
   no reordenar las secciones aceptadas.

## Responsive Baseline

Diseño base para Pixel 390x844. Debe sobrevivir a pantallas pequeñas (360x640) y a tipografía
ampliada por accesibilidad sin romper layouts: los textos largos hacen scroll, nunca se recortan.
Si el modo oscuro entra en el MVP (ver Open Design Questions), esbozarlo con `darkBackground` y
`darkSurface`, conservando los acentos terracota y oliva.

## Accessibility Baseline

- Objetivos táctiles mínimos de 48dp.
- Contraste suficiente de texto sobre fotografía: usar un degradado sutil bajo el texto blanco (el
  `scrim` de la variante L2).
- **Contraste de las tarjetas sobre el fondo en modo oscuro ≥ 3:1** (WCAG 1.4.11): con
  `darkCardSurface` `#3A2E26` y borde `#7D6A5C` el ratio es **3,26:1**. Los tokens anteriores daban
  1,15:1 (el defecto que corrige el ADR 0006).
- No depender solo del color para transmitir estado (los alérgenos llevan icono y etiqueta, no solo
  ámbar).
- Soporte de tipografía dinámica de Android.
- El estado activo de la barra inferior combina icono relleno + etiqueta, no solo color.

## Do's and Don'ts

**Sí**
- Escribe la receta como una **ficha**: qué plato es y qué lleva, legible de un vistazo.
- Usa el terracota para una sola acción principal por pantalla.
- Mantén cifras tabulares en todo lo cuantificable (cantidades, tiempos, comensales).
- Usa el filete de color de la cocina como único cromo de color de la tarjeta.
- Muestra siempre estados vacíos y de carga cuidados.

**No**
- No usar rojo de alarma para alérgenos o errores de contenido.
- No introducir una tercera tipografía ni gradientes llamativos.
- No añadir bordes duros ni sombras marcadas (salvo el borde de tarjeta en modo oscuro, que es
  contraste de accesibilidad, no decoración).
- No mostrar una foto que no corresponda al plato: si no hay imagen, la ficha tipográfica se queda.
- No inventar pantallas o flujos que no estén en `docs/build-brief.md`.

## Open Design Questions

1. **Prompt de Stitch**: generado y guardado en `docs/design/stitch-prompt.md`. Las pantallas
   actuales quedan obsoletas; hay que regenerarlas con ese prompt. Al aceptar exportaciones,
   guardarlas en `docs/design/concepts/` y registrar su estado en este documento.
2. ¿Existe nombre de producto e icono definitivos, o "Sazón" es un marcador de posición?
3. **Resuelto (2026-10-06):** el modo oscuro **entra en el MVP**. Se implementa la variante oscura con
   `darkBackground`/`darkSurface` y los acentos terracota y oliva (feature `design-tokens-and-theme`).
4. **Resuelto y revisado (2026-10-13, ADR 0006):** la imagen de una receta es una **ficha tipográfica**
   (categoría, título y los tres ingredientes principales con cantidad) y **ninguna foto por defecto**.
   La fotografía queda como **capa de fondo opcional** (variante L2), supeditada al ajuste **"Generar
   imágenes del plato"**. Resoluciones anteriores, sustituidas: `recipe-artwork` (2026-10-12) fijó el
   glifo + acento por cocina y `recipe-photos` (issue #83) las 31 fotos empaquetadas; esa cadena local
   **se retiró** con `retire-recipe-photos` (2026-10-14). Ver **Imagen de receta** en `Components`,
   `docs/adr/0006-typographic-recipe-cards.md` y
   `docs/adr/0004-recipe-photos-as-assets.md` / `0005-ai-dish-images.md` (ajustados).
5. ¿La ilustración de los estados vacíos es un estilo definido (línea, color plano) o se improvisa?
6. **Resuelto (2026-10-14, `recipe-card-l2-unified`):** el carrusel **no** sube a 3:2. Todas las
   tarjetas usan la estructura **L2** (hueco **16:9** + banda sólida): los ingredientes van en la banda,
   en **una línea** cuando la tarjeta es estrecha y en **tres filas** cuando es ancha (Resultados y
   Detalle). Se acaba con la dualidad 16:10/16:9: el hueco es siempre 16:9.
7. **Resuelto (2026-10-14, `retire-recipe-photos`):** los assets `seed/` y `category/` de
   `img/recipes/` y la cadena local **salieron** del APK y del runtime (30 assets, ≈5,68 MiB), y
   **se conserva `img/recipes/default.jpg`** (137.359 B) como **ancla de estilo**
   de `dish-style-reference` (ADR 0005, fase 6): no se muestra en ninguna pantalla, así que no
   reintroduce ninguna imagen imprecisa ni ninguna ficha. El set `img/dishes/seed/**` de
   `dish-image-seed` (ADR 0005, fase 7) se conserva: no alimenta el hero, sino la capa de fondo
   generada, y el hero se queda siempre tipográfico.
8. **Abierto:** ¿la marca de agua del glifo (12 % de opacidad) es suficiente para distinguir dos recetas
   de la misma categoría y cocina, o conviene el monograma del título también en la tarjeta?
