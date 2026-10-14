# ADR 0006: La imagen de una receta es tipográfica y, cuando existe, la foto es capa de fondo

## Estado

**Aceptado** (2026-10-13, decisión de la persona tras revisar los prototipos locales de `preview/`).
**Implementadas** la primera, la segunda y la tercera de las cinco features:
`card-typographic-tokens-and-design`
(2026-10-14: tokens `darkCardSurface`/`darkCardOutline` en `ui/theme/Color.kt`, contrato de tarjeta
`SazonCardTokens`/`SazonCardColors`/`LocalSazonCardColors` + tinte cálido en `ui/theme/CardTokens.kt`,
cableado en `SazonTheme` y tests JVM de contraste y tinte; **`accepted`**), `recipe-card-typographic`
(2026-10-14: la ficha tipográfica de `RecipeCard`/`ResultRecipeCard` y de la rejilla de Favoritos —sin
ninguna foto—, el monograma 96×96 de la miniatura de la lista de Favoritos, la lógica pura
`RecipeCardFormat`/`RecipeMonogram` y las primitivas compartidas `RecipeGlyphArtwork`/
`recipeAccentBrush`/`recipeAccentColors` en `ui/artwork`; **`accepted`**) y
`recipe-detail-typographic-header`
(2026-10-14: el hero del Detalle deja de ser una foto y pasa a **hero tipográfico** —categoría en
versalitas, título Manrope, los tres ingredientes con cantidad, marca de agua del glifo al 12 % y filete
de 6 px— seguido de la **tira de metadatos** como bloque propio, sin la tarjeta superpuesta; lógica pura
`RecipeDetailHeroFormat`, el Detalle **ya no consume `ui.artwork.RecipeArtwork`**; implementada y
**autoverificada**, pendiente de validación independiente). **Implementación pendiente** de las otras
dos: `dish-artwork-as-card-background` y `retire-recipe-photos`.

Ajusta:
- **ADR 0004** (fotos empaquetadas como assets): la foto deja de ser la **imagen principal**; la cadena
  local se retira.
- **ADR 0005** (imágenes generadas por IA): la imagen generada deja de ser "la imagen de la receta" y
  pasa a ser **capa de fondo** del hueco, supeditada al ajuste del usuario.

## Contexto

El MVP resolvía la imagen con un placeholder dibujado (feature `recipe-artwork`); después se añadieron
**31 fotos** empaquetadas como assets (`recipe-photos`, ADR 0004) y, encima, la generación por IA con
interruptor y tope de sesión (ADR 0005, fases 1-7, más `dish-visual-descriptor-wiring` y la correctiva
`dish-image-prompt-v2`).

Al revisar la dirección, la persona planteó dos problemas de las **fotos reales**:

1. **Dependencia de una API de IA con cuota** (~2 % de la cuota semanal por imagen; tope de 12 por
   sesión). No es algo que se quiera por defecto.
2. **Imágenes preestablecidas imprecisas**: una receta generada por IA no tiene foto propia y cae en la
   foto de su **categoría**, que no corresponde al plato. Es el síntoma ya registrado en
   `dish-image-prompt-v2` (imágenes que no cuadran con el plato y una repetida tres veces).

Se bocetaron seis alternativas sin foto (glifo + degradado, glifo + monograma, glifo + dato del plato,
collage de ingredientes, tarjeta tipográfica y degradado abstracto con etiqueta) en los prototipos
`preview/*.html`, y se midió el contraste de las tarjetas en modo oscuro: **1,15:1** entre
`darkSurface` (`#312720`) y `darkBackground` (`#241C16`), por debajo del **3:1** que WCAG 1.4.11 pide
para distinguir límites de interfaz.

Decisiones de la persona sobre los prototipos:

| Pieza | Elección |
|---|---|
| Dirección de la tarjeta | **C3 "carta de restaurante"** + los **3 ingredientes principales** con cantidad |
| Contraste en oscuro | **B + C**: superficie `#3A2E26` + borde `#7D6A5C` (3,26:1) + tinte cálido de la cocina |
| Colocación de la imagen | **L2**: imagen arriba (16:9) con categoría y título encima; ingredientes y metadatos en **banda sólida** |
| Interruptor de imágenes por IA | Se **conserva**: encendido = imagen de fondo; apagado = solo tipografía |

## Decisión

1. **La imagen base de una receta es una ficha tipográfica**: categoría en versalitas, título en Manrope
   y los **tres ingredientes principales** con su cantidad en cifras tabulares, más un **filete de color**
   por cocina (`RecipeAccent`). Sin foto por defecto: la tarjeta **nunca afirma un plato que no es**.
2. **La imagen, cuando existe, es capa de fondo** del hueco (variante **L2**): imagen 16:9 con `scrim` de
   contraste y la categoría y el título encima; los ingredientes y los metadatos van en una **banda
   sólida** con los tokens de superficie. La tipografía es la **capa base**, así que la imagen entra con
   el **crossfade de 250 ms** ya implementado (`DishImageArtworkPolicy.CROSSFADE_MILLIS`) y **sin salto
   de layout**.
3. **El ajuste "Generar imágenes del plato" sigue mandando**: apagado ⇒ `null`, sin leer caché, sin red
   y sin cuota (comportamiento ya implementado). Encendido ⇒ imagen de fondo si está disponible, con
   tope por sesión y caché persistentes.
4. **Se retira la cadena local de fotos**: con el ajuste apagado no se consulta, y la foto de categoría
   es precisamente la que no corresponde al plato. Se retiran del APK **30 de los 31 assets** (~6,2 MiB)
   junto con `recipe-photos`; se **conserva** `img/recipes/default.<ext>` (~136 KB) como **ancla de estilo**
   de `dish-style-reference` (ADR 0005, fase 6), porque no se muestra en ninguna pantalla.
5. **Modo oscuro**: la tarjeta usa `darkCardSurface` (`#3A2E26`) y `darkCardOutline` (`#7D6A5C`) —
   3,26:1, cumple WCAG 1.4.11 — más el tinte cálido de la cocina. Son tokens **de tarjeta**, no un
   cambio de `darkSurface`: no alteran navegador, hojas ni el resto de pantallas.
6. **Favoritos**: la miniatura de 96×96 pasa a **monograma** de iniciales sobre `surfaceVariant` con la
   **regla terracota** (`primary`) **de 5px a la izquierda** (`DESIGN.md` →
   `components.recipe-tile-favorites.accentRule`). **No** es la regla por cocina: a 5px los pares suaves
   de `RecipeAccent` no se perciben, así que la variación por cocina vive **solo** en el **filete de
   6px** del borde inferior de las tarjetas tipográficas (decisión 1). *(Redacción previa de este punto:
   "la regla de color de la cocina a la izquierda"; reconciliada a terracota con la feature
   `recipe-card-typographic`.)*
7. **`RecipeArtwork` cambia de rol**: deja de ser "la imagen de la receta" y pasa a ser **el proveedor
   de la capa de fondo** (con crossfade y degradación silenciosa). Conserva firma y geometría; los 5
   puntos de uso se revisan. *(Tras `recipe-card-typographic` las tres tarjetas —Inicio, Resultados y
   Favoritos— y la miniatura de la lista **ya no lo consumen** (ficha tipográfica y monograma); tras
   `recipe-detail-typographic-header` el hero del Detalle tampoco: `RecipeArtwork` queda **sin
   consumidores** en la app hasta que `dish-artwork-as-card-background` lo recupere como capa de fondo
   del hueco.)*

## Consecuencias

**A favor**

- **Honestidad**: no hay ninguna imagen que no corresponda al plato. El problema 2 queda resuelto de raíz.
- **Sin red ni cuota por defecto**: el problema 1 solo existe si el usuario enciende el interruptor.
- **−6,2 MiB** de APK (30 de los 31 assets; se conserva `default.<ext>` como ancla de estilo) y menos
  decodificación de bitmaps (el techo LRU de 16 bitmaps de 1376×768 era de
  ~64 MiB).
- **Sin salto de layout** al insertar la imagen: la capa base es tipografía, no una imagen.
- **Cumple accesibilidad** en oscuro (3,26:1) con un cambio acotado a la tarjeta.
- **Sin assets nuevos ni dependencias nuevas**: el filete reutiliza `RecipeAccent` y la marca de agua
  reutiliza `RecipeGlyph`.

**En contra / riesgos**

- **Se renuncia al apetito de la fotografía** como protagonista; pasa a un registro editorial. Es una
  decisión de producto consciente y reversible (volver a las fotos es un `revert`).
- **El Detalle pierde el recurso del solape** (la tarjeta blanca que subía sobre el hero). Se sustituye
  por hero tipográfico + tira de metadatos; hay que revisar el acabado en el emulador. Con ello el hero
  del Detalle **ya no consume `RecipeArtwork`**, que queda sin consumidores en la app hasta que
  `dish-artwork-as-card-background` lo recupere como capa de fondo.
- **Es un cambio de layout**: la política de capturas de `docs/design/concepts/` obliga a capturas de
  referencia nuevas por feature (no vale la excepción de las correctivas de microcopy).
- **Modo semilla**: el set semilla de imágenes de la fase 7 (`img/dishes/seed/`) queda **sin uso** si se
  apaga el interruptor; mantenerlo no cuesta red y sirve si se enciende.
- Se revisan `DESIGN.md` (Imagery, Product Feel, RecipeCard, Detalle, OQ4) y las specs aceptadas que
  describían la foto como imagen principal.

## Alternativas consideradas

1. **Conservar las fotos como imagen principal y solo apagar la IA.** Resuelve el problema 1 (cuota) pero
   no el 2 (la receta de IA seguiría con la foto de su categoría). Descartada por la persona.
2. **Foto solo para las 12 recetas semilla y tipografía para las de IA.** Es una cadena mixta: dos
   lenguajes visuales en la misma lista. Descartada por consistencia.
3. **Ilustración propia por categoría.** Sigue siendo genérica (18 categorías para N platos) y exige
   producir y versionar arte nuevo. Descartada en esta fase.
4. **Collage de iconos de ingredientes.** Informativo y sin fotos, pero necesita un set de iconos de
   ingrediente que hoy no existe (la Taxonomía trae `icon` por ingrediente, pero la app no los dibuja).
   Se deja como evolutivo: la ficha tipográfica ya muestra los ingredientes en texto.
5. **Generación local en el dispositivo.** Descartada en el ADR 0005 (§7.3): tamaño y rendimiento.
6. **Tarjeta tipográfica sin ingredientes.** Es la variante C original; la persona pidió añadir los tres
   ingredientes principales, de ahí el diseño final.

## Referencias

- `DESIGN.md` — dirección visual y tokens (actualizados con esta decisión).
- `docs/adr/0004-recipe-photos-as-assets.md` — cadena de fotos que este ADR retira.
- `docs/adr/0005-ai-dish-images.md` — puerto, caché, crossfade y control de gasto que este ADR reutiliza.
- `docs/ai-dish-images-proposal.md` — propuesta de imágenes generadas (fases 1-7, completas).
- `preview/prototipo-c3.html`, `preview/prototipo-c3-imagenes.html`, `preview/comparativa-imagenes.html`,
  `preview/comparativa-tipografica.html` — prototipos que sostienen la decisión (fuera del repo).
