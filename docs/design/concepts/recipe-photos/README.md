# Capturas de referencia — `recipe-photos`

Estas capturas son **obligatorias antes de implementar** (política de capturas de UI del repo). Ante
cualquier contradicción, manda `DESIGN.md`.

Estas láminas son el **origen de los assets**: de cada export se descargan las fotografías (URLs
`lh3.googleusercontent.com/aida-public/...` del `code.html`) a su resolución nativa (~1400x768) y se
empaquetan como **assets offline** del repo, mapeadas por la **clave** de cada fila.

> **Primera entrega rechazada (2026-10-13).** Las dos láminas anteriores no valían:
> `fotos-semillas/` contenía la pantalla **Modo Cocina** (export equivocado) y `fotos-categorias/` vino
> como **una única imagen compuesta** (1376x768) con las 19 fotos dentro, ~275x192 cada una (borrosas al
> tamaño de tarjeta). Se eliminaron y se regeneran con `stitch-prompt.md` (v2), que pide una **lista de
> filas con una foto independiente por fila** para que Stitch genere imágenes separadas y nítidas.

## Cómo generarlas

1. Abre una conversación **nueva** en Stitch (no reutilices la de Modo Cocina) y pega la **PARTE 0**;
   espera a que responda "OK".
2. Pega la **PARTE 1** (lote A, 12 semillas) y exporta. **Párate aquí**: entrégala y la verifico antes de
   seguir, para confirmar que salen 12 fotos **separadas** y nítidas.
3. Cuando te confirme, pega la **PARTE 2** (lote B, 18 categorías + por defecto) y exporta.

Exporta cada lámina tal cual la da Stitch: el `.png`, el `.html` (`code.html`) y el `.md` (`design.md`).

## Convención

Un **subdirectorio por lámina** con los **tres ficheros** del export, tal cual: `screen.png` (o el
`.png`), `code.html` (o el `.html`) y `DESIGN.md` (o el `.md`).

## Pantallas esperadas

- `fotos-semillas/` — **lote A** (12 fotos): una foto por receta semilla, con su clave como rótulo
  (`seed-tortilla-patatas`, `seed-gazpacho-andaluz`, `seed-pasta-tomate`, `seed-pollo-al-ajillo`,
  `seed-lentejas-estofadas`, `seed-salmon-a-la-plancha`, `seed-tacos-de-pollo`, `seed-ensalada-de-quinoa`,
  `seed-tofu-salteado`, `seed-crema-de-calabaza`, `seed-tortitas-de-avena`, `seed-tarta-de-manzana`).
  Destino: `docs/design/concepts/recipe-photos/fotos-semillas/`.
- `fotos-categorias/` — **lote B** (19 fotos): una por categoría (`cat-pasta` … `cat-other`) **más** la
  foto por defecto con la clave `default` (genérica/neutra). Destino:
  `docs/design/concepts/recipe-photos/fotos-categorias/`.

## Reanudar

`github-flow execute recipe-photos` (o `--captures <ruta>`). Mientras falten ficheros válidos, la
feature queda `blocked` y no se lanza sesión.

Issue: [#83](https://github.com/fernandogaliay-alten/default/issues/83).
