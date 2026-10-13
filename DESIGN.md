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
    imageRatio: "16:10"
    rounded: "{rounded.md}"
    shadow: "y=4 blur=16 alpha=6%"
  bottom-nav:
    backgroundColor: "{colors.surface}"
    activeColor: "{colors.primary}"
    inactiveColor: "#9A8C7F"
    height: 80dp
---

# Design Direction

## Overview

"Sazón" debe sentirse como un libro de cocina moderno: **cálido, apetitoso y limpio**, nunca clínico
ni corporativo. La comida y las fotos mandan; el cromo de la interfaz se retira para no competir. La
identidad visual se apoya en tres ideas: fondo crema, tarjetas blancas con esquinas muy redondeadas,
y un naranja terracota reservado para la acción principal.

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
fotos** (una imagen `screen.png` + `code.html` + `DESIGN.md` por lote) y son a la vez la **fuente** de los
31 assets y la **dirección visual** del acabado. `fotos-semillas` expone las 12 fotos de las recetas
semilla **en el orden** del `README.md` (el `alt` no es la clave: la clave va en el comentario
`<!-- NN. <clave> -->`); `fotos-categorias` expone las 18 `cat-<categoryId>` **más** `default`. La
procedencia, el mapeo y el procedimiento de descarga viven en
`docs/design/concepts/recipe-photos/README.md` (v2) y el prompt en `stitch-prompt.md`.

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

Al aceptar cada exportación, registrar aquí su estado como **dirección visual** (no especificación
pixel-perfect).

## Product Feel

- **Apetitoso antes que técnico**: la foto del plato siempre presente, con luz cálida y natural.
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
- Superficies superpuestas (la tarjeta blanca que sube sobre la foto del detalle) para dar
  profundidad sin sombras fuertes.

## Components

- **RecipeCard** (vertical): imagen 16:10 redondeada, título a 2 líneas máximo, fila de metadatos
  `tiempo · dificultad · comensales` y una píldora terracota con el porcentaje de coincidencia.
- **RecipeListCard** (horizontal): miniatura 96x96, texto a la derecha, corazón a la derecha.
- **Imagen de receta** (`ui.artwork.RecipeArtwork`, features `recipe-artwork` y `recipe-photos`): la
  **fotografía del plato** empaquetada como asset **offline** (31 fotos en
  `app/src/main/assets/img/recipes/`) es la imagen principal; el **bloque con degradado + glifo de
  línea** dibujado con Compose queda como **último recurso** si falta la foto.
  - **Cadena de respaldo** (determinista, en `data.RecipePhotoAssets`): foto propia de la
    receta semilla (`seed/<id>.<ext>`) → foto de su categoría (`category/cat-<categoryId>.<ext>`) →
    foto genérica (`default.<ext>`) → glifo. Las recetas generadas por IA no tienen foto propia y usan
    la de su categoría; `categoryId` nulo/vacío/desconocido cae al `default`.
  - La foto se recorta con `ContentScale.Crop` y conserva la geometría aceptada (16:10 de las tarjetas,
    96x96 y alto 96 de Favoritos, hero de 240 del Detalle); no cambian esquinas ni tamaños. La imagen
    es **decorativa** (`contentDescription = null`): el título de la receta ya se anuncia.
  - **Último recurso** (degradado + glifo): el **glifo** identifica la categoría (`categoryId`) y el
    **acento del degradado** la cocina (`cuisineId`), de modo que dos recetas de la misma categoría y
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
    - El glifo se tiñe con `onSurfaceVariant` y el componente no fija forma: mantiene el 16:10 de las
      tarjetas (`RecipeCard` 48 dp de glifo, `ResultRecipeCard` 56), el 96x96 y el alto 96 de Favoritos
      (40) y el hero de 240 del Detalle (72).
  - Es el **único punto a cambiar** para la imagen de receta: la fuente de la foto se inyecta por
    `LocalRecipePhotoSource`, así que los 5 puntos de uso no conocen ni el asset ni Android. Ver
    `docs/adr/0004-recipe-photos-as-assets.md` y `docs/technical-discovery.md`.
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
   que abre el Generador, carrusel "Sugeridas para ti", y el bloque **"Tus recetas"** con filtros
   locales (categoría + "Rápido"/"Vegetariano") y dos grupos: "Guardadas" y "Abiertas hace poco".
3. **Generador**: paso 1 ingredientes (chips + input), paso 2 ajustes agrupados en tarjetas, estado
   de generación con animación cálida.
4. **Resultados**: chips de orden + lista de RecipeCard; estados vacío, error y **modo semilla** (sin
   IA disponible, se muestran las recetas locales).
5. **Detalle**: foto ancha, tarjeta superpuesta con metadatos, pestañas Ingredientes · Pasos · Notas,
   barra inferior con "Guardar" y "Empezar a cocinar".
6. **Favoritos**: búsqueda, orden (chip por defecto **"Recientes"**) con el toggle lista/rejilla
   icónico, lista, estado vacío con nota "Disponibles sin conexión".
7. **Perfil**: identidad (nombre y color de avatar con vista previa), preferencias editables
   (alimentación, estilo, cocinas, ingredientes que me gustan e ingredientes que evito), comensales
   habituales, tarjeta ámbar de alérgenos, bloque de cifras titulado **"Estadísticas"** (recetas
   generadas y favoritas, informativas y **no navegables**), acción "Actualizar sugerencias" con la
   fecha del último refresco, y ajustes.

## Responsive Baseline

Diseño base para Pixel 390x844. Debe sobrevivir a pantallas pequeñas (360x640) y a tipografía
ampliada por accesibilidad sin romper layouts: los textos largos hacen scroll, nunca se recortan.
Si el modo oscuro entra en el MVP (ver Open Design Questions), esbozarlo con `darkBackground` y
`darkSurface`, conservando los acentos terracota y oliva.

## Accessibility Baseline

- Objetivos táctiles mínimos de 48dp.
- Contraste suficiente de texto sobre fotografía: usar un degradado sutil bajo el texto blanco.
- No depender solo del color para transmitir estado (los alérgenos llevan icono y etiqueta, no solo
  ámbar).
- Soporte de tipografía dinámica de Android.
- El estado activo de la barra inferior combina icono relleno + etiqueta, no solo color.

## Do's and Don'ts

**Sí**
- Deja que la foto del plato sea la protagonista.
- Usa el terracota para una sola acción principal por pantalla.
- Mantén cifras tabulares en todo lo cuantificable.
- Muestra siempre estados vacíos y de carga cuidados.

**No**
- No usar rojo de alarma para alérgenos o errores de contenido.
- No introducir una tercera tipografía ni gradientes llamativos.
- No añadir bordes duros ni sombras marcadas.
- No inventar pantallas o flujos que no estén en `docs/build-brief.md`.

## Open Design Questions

1. **Prompt de Stitch**: generado y guardado en `docs/design/stitch-prompt.md`. Las pantallas
   actuales quedan obsoletas; hay que regenerarlas con ese prompt. Al aceptar exportaciones,
   guardarlas en `docs/design/concepts/` y registrar su estado en este documento.
2. ¿Existe nombre de producto e icono definitivos, o "Sazón" es un marcador de posición?
3. **Resuelto (2026-10-06):** el modo oscuro **entra en el MVP**. Se implementa la variante oscura con
   `darkBackground`/`darkSurface` y los acentos terracota y oliva (feature `design-tokens-and-theme`).
4. **Resuelto (2026-10-12, `recipe-artwork`) y revisado (2026-10-13, `recipe-photos`, issue #83):** el
   MVP **sí** lleva fotos de receta, pero **empaquetadas como assets offline** (31 fotos en
   `app/src/main/assets/img/recipes/`), **sin** dependencia de imágenes ni red en tiempo de ejecución. La
   cadena es determinista: foto propia de la receta semilla → foto de su categoría → foto por defecto →
   placeholder dibujado `ui.artwork.RecipeArtwork` (glifo por categoría + acento por cocina), que queda
   como **último recurso**. Ver la sección **Imagen de receta** de `Components` y
   `docs/adr/0004-recipe-photos-as-assets.md`. `RecipeArtwork` sigue siendo el único punto de imagen.
5. ¿La ilustración de los estados vacíos es un estilo definido (línea, color plano) o se improvisa?
