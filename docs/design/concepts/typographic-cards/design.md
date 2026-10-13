# Dirección visual: tarjeta tipográfica e imagen como capa de fondo

**Estado: dirección visual aceptada, implementación pendiente** (ADR 0006, 2026-10-13).

Esta carpeta **no es una subcarpeta de feature**: guarda los **prototipos versionados** que sostienen la
decisión de `docs/adr/0006-typographic-recipe-cards.md`. Son la **dirección visual** de las cuatro
features de UI que la implementan, y a la vez la **referencia** de la que salen sus capturas por
pantalla.

## Prototipos

| Fichero | Qué muestra |
|---|---|
| `prototipo-c3.html` | **La maqueta final**: cinco pantallas (Inicio, Resultados, Favoritos lista y rejilla, Detalle) en claro y oscuro, hoja de componentes y especificación de la tarjeta. Incluye el par OFF/ON del ajuste de imágenes (L2). |
| `prototipo-c3-imagenes.html` | El detalle de las **tres opciones de contraste** en oscuro (A/B/C, elegida **B+C**) y las variantes **L1 a sangre** y **L2 banda sólida** (elegida **L2**). |
| `comparativa-imagenes.html` | Las **seis alternativas** a la foto del plato (glifo, monograma, dato, collage, tipográfica, degradado abstracto) y la cadena actual con fotos reales. |
| `comparativa-tipografica.html` | Cinco tratamientos de la tarjeta tipográfica (C1–C5); la elegida es **C3 "carta de restaurante"**. |

Los prototipos abren en el navegador; cargan Manrope/Inter desde Google Fonts (sin red caen a las
fuentes del sistema y el layout aguanta). Las `img` que usan son **las fotos reales del APK**, puestas
como fondo para comprobar cómo lee la tipografía encima; no son assets del producto.

## Qué NO es

- No son una especificación pixel-perfect: mandan `DESIGN.md` y el ADR 0006.
- No tienen `.png`: las capturas por pantalla van en su **subcarpeta de feature**
  (`docs/design/concepts/<feature-id>/<pantalla>/`), en claro y oscuro, según el `README.md` de cada una.
- No pasan por Stitch: es una **desviación de la política de capturas autorizada por la persona**
  (2026-10-13, sesión 101); las capturas se toman de estos prototipos, en claro y oscuro.

## Capturas aportadas por feature (2026-10-14)

Renderizadas de estos prototipos con Chrome headless (`--force-device-scale-factor=2`, 780×1688) y
depositadas en la subcarpeta de cada pantalla junto al `code.html` y su `DESIGN.md`:

| Feature | Carpeta | Pantallas |
|---|---|---|
| `card-typographic-tokens-and-design` | `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/` | `screen.png` (claro), `screen-dark.png` (oscuro) |
| `recipe-card-typographic` | `docs/design/concepts/recipe-card-typographic/recipe-card/` | `screen.png`, `screen-dark.png` (Inicio) y `screen-resultados.png`, `screen-resultados-dark.png` |
| | `.../favoritos-lista/` | `screen.png`, `screen-dark.png` |
| | `.../favoritos-rejilla/` | `screen.png`, `screen-dark.png` |
| `recipe-detail-typographic-header` | `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/` | `screen.png`, `screen-dark.png` |
| `dish-artwork-as-card-background` | `docs/design/concepts/dish-artwork-as-card-background/tarjeta-con-imagen/` | `screen.png`, `screen-dark.png` |
| | `.../detalle-con-imagen/` | `screen.png`, `screen-dark.png` |
