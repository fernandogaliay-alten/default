# ADR 0004: Las fotos de plato viajan empaquetadas como assets offline

## Estado

Aceptado (feature `recipe-photos`, issue #83). Ajusta parcialmente la decisión "sin imágenes" que
fijaba `DESIGN.md` (Open Design Question 4, resuelta el 2026-10-12 por `recipe-artwork`).

**Ajustado por `docs/adr/0006-typographic-recipe-cards.md`**: la **cadena local de fotos se retira** con
la feature `retire-recipe-photos` (la imagen de una receta es **tipográfica** y, cuando existe foto, es
**capa de fondo** del hueco). Se conserva únicamente `img/recipes/default.<ext>` (~136 KB) como **ancla
de estilo** de `dish-style-reference` (ADR 0005, fase 6), que no se muestra en ninguna pantalla.

## Contexto

El MVP resolvía la imagen de una receta con un **placeholder dibujado**: un bloque con degradado
(acento por cocina) y un **glifo de línea** por categoría, en el componente único
`ui.artwork.RecipeArtwork` (feature `recipe-artwork`). `DESIGN.md` lo pedía explícitamente ("la foto del
plato siempre presente", "deja que la foto del plato sea la protagonista") pero el MVP no empaquetaba
fotos: no había assets de imagen ni dependencia de imágenes, y `docs/domain/taxonomy.md` §6 describía
una convención (`assets/img/recipes/{categoryId}.webp`) que nunca llegó a existir.

La app es **local-first** (ADR 0002): sin backend, sin cuenta y sin red salvo la llamada a la IA. La
verificación canónica corre en JVM (`bash init.sh`), sin emulador. La persona generó con Stitch un set
propio de fotografías (2 láminas, 12 + 19 imágenes a 1376x768) y las entregó como capturas de
referencia versionadas, con la clave de cada foto expuesta en el `code.html` del export.

## Decisión

- Las **31 fotos de plato** (12 recetas semilla + 18 categorías + 1 por defecto) viajan **empaquetadas
  como assets offline** en `app/src/main/assets/img/recipes/`, **versionadas** como
  `seed-recipes.json` (no se ignoran en git).
- Convención de nombres (la clave es mecánica, sin campo nuevo en el dominio):

  | Grupo | Ruta | Clave |
  |---|---|---|
  | Foto propia de una receta semilla | `img/recipes/seed/<recipeId>.<ext>` | `id` de la receta |
  | Foto de la categoría | `img/recipes/category/cat-<categoryId>.<ext>` | `categoryId` real (18) |
  | Foto genérica de último recurso | `img/recipes/default.<ext>` | `default` |

- **Cadena de respaldo determinista**: foto propia → foto de la categoría → foto por defecto → **`null`**.
  El último caso vuelve al placeholder aceptado (degradado + glifo), **sin cambios** en
  `RecipeArtworkStyle`. La resolución es lógica **pura** (`data.RecipePhotoAssets`) con la existencia
  de fichero **inyectada** (`exists: (String) -> Boolean`), de modo que la misma función sirve al test
  JVM (fichero en disco) y a la app (lectura de assets).
- La extensión **no** se asume: se prueban `webp`, `jpg` y `png` en ese orden, así que migrar a WebP no
  requiere cambios de Kotlin.
- La lectura y decodificación viven en `data.AssetRecipePhotoSource` (`AssetManager` +
  `BitmapFactory`, `Dispatchers.IO`, caché LRU de 16 bitmaps) y se inyectan en Compose por
  `LocalRecipePhotoSource` (con default nulo: cualquier host sin proveedor sigue pintando el
  placeholder). `RecipeArtwork` conserva su firma y su geometría (16:10, 96x96, alto 96, hero 240) y
  pinta la foto con `ContentScale.Crop`.
- El empaquetado lo hace `scripts/fetch-recipe-photos.sh`, **herramienta de una sola vez** (necesita
  red) que **no** entra en `init.sh`: el gate sigue siendo offline y no bloqueante. Descarga cada `src`
  de las láminas con el sufijo `=s0`, valida que las claves son exactamente las 12/19 esperadas (orden
  incluido en las semillas) y falla con código ≠ 0 ante cualquier descarte: nunca un empaquetado
  parcial silencioso.

## Alternativas consideradas

1. **Coil (u otra librería de imágenes).** Cargaría y cachearía por URL, pero la app no tiene red en el
   caso normal y añadir una dependencia contradice el alcance del MVP: los assets están en el APK y
   `BitmapFactory` ya cubre la decodificación. Descartado (sin dependencias nuevas).
2. **Descargar en tiempo de ejecución y cachear en disco.** Exigiría red, permiso y gestión de caché, y
   rompería el funcionamiento offline. Descartado.
3. **Añadir un campo de imagen a `Recipe`.** Duplicaría la convención en el dominio y obligaría a
   migrar el esquema. La imagen se resuelve por convención a partir de `id`/`categoryId`. Descartado.
4. **Ampliar el glifo dibujado (más ilustración) en vez de usar fotos.** Es lo que ya había; no cumple
   la dirección visual de `DESIGN.md` ("la foto del plato es la protagonista"). Descartado por la
   persona, que encargó las fotografías.
5. **Un `CompositionLocal` versus pasar el loader por los 5 puntos de llamada.** El `CompositionLocal`
   con default nulo es el diff mínimo y el único que degrada "solo" si nadie provee el loader.
   Elegido.

## Consecuencias

- **Tamaño del APK**: los assets suman ~5,9 MB (los 31 JPEG a 1376x768 descargados con `=s0`), por
  debajo del presupuesto de **8 MB**. Si en la máquina del empaquetado hay `cwebp` o ImageMagick, el
  script convierte a **WebP q80** y redimensiona a 960 px de ancho (objetivo ~2–4 MB); en caso
  contrario conserva el original y lo registra (es lo que ocurrió en la entrega de esta feature).
- **Añadir una receta semilla = añadir su foto** a `img/recipes/seed/` (o asumir el respaldo a la foto
  de su categoría). El script es la vía documentada para empaquetarla.
- El panel de imágenes de la app gana una dependencia de **contenido** (assets), no de **código**: sin
  librerías nuevas, sin red en tiempo de ejecución y sin caché en disco.
- La procedencia de las fotos queda registrada: son **generadas por nosotros** con el prompt propio del
  repo (`docs/design/concepts/recipe-photos/stitch-prompt.md`) y la app es personal y no se distribuye
  (coherente con `docs/risks-and-open-questions.md`).
- Si una foto no encaja visualmente (encuadre, plato poco apetitoso, foto equivocada), se corrige con
  una **feature correctiva** (regla de `AGENTS.md`), no reabriendo esta feature.
