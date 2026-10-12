# Capturas de referencia — `recipe-photos`

Estas capturas son **obligatorias antes de implementar** (política de capturas de UI del repo). Ante
cualquier contradicción, manda `DESIGN.md`.

No son pantallas de la app: son **láminas de fotos de plato** que Stitch genera a partir de
`stitch-prompt.md`. De cada lámina se descargan las imágenes (URLs `lh3.googleusercontent.com/aida-public/...`
del `code.html`) a 1200x896 y se empaquetan como **assets offline** del repo. No hace falta recortarlas
ni renombrarlas a mano: el orquestador extrae las URLs y las mapea por la **clave impresa bajo cada foto**.

## Cómo generarlas

1. Abre una conversación **nueva** en Stitch y pega la **PARTE 0** de `stitch-prompt.md`; espera a que
   responda "OK".
2. Pega la **PARTE 1** (lote A, 12 fotos) y exporta la lámina.
3. Vuelve a pegar la **PARTE 2** (lote B, 18 categorías + por defecto) y exporta la lámina.

Exporta cada lámina tal cual la da Stitch: el `.png`, el `.html` (`code.html`) y el `.md` (`design.md`).

## Convención

Un **subdirectorio por lámina** con los **tres ficheros** del export, tal cual:
`screen.png` (o el `.png`), `code.html` (o el `.html`) y `DESIGN.md` (o el `.md`). Dirección visual,
no especificación pixel-perfect.

## Pantallas esperadas

- `fotos-semillas/` — **lote A** (12 fotos). Una foto por receta semilla, cada una con su clave exacta
  impresa debajo (`seed-tortilla-patatas`, `seed-gazpacho-andaluz`, `seed-pasta-tomate`,
  `seed-pollo-al-ajillo`, `seed-lentejas-estofadas`, `seed-salmon-a-la-plancha`, `seed-tacos-de-pollo`,
  `seed-ensalada-de-quinoa`, `seed-tofu-salteado`, `seed-crema-de-calabaza`, `seed-tortitas-de-avena`,
  `seed-tarta-de-manzana`). Destino: `docs/design/concepts/recipe-photos/fotos-semillas/`.
- `fotos-categorias/` — **lote B** (19 fotos). Una foto por categoría (`cat-pasta`, `cat-rice`,
  `cat-soup_cream`, `cat-salad`, `cat-meat`, `cat-poultry`, `cat-fish_seafood`, `cat-vegetarian`,
  `cat-vegan`, `cat-legume_stew`, `cat-pizza_bread`, `cat-egg_breakfast`, `cat-sandwich_wrap`,
  `cat-tapas_appetizer`, `cat-dessert`, `cat-smoothie_drink`, `cat-casserole_oven`, `cat-other`) **más**
  la **foto por defecto** con la clave `default` (genérica/neutra, lo más general posible). Destino:
  `docs/design/concepts/recipe-photos/fotos-categorias/`.

## Reanudar

`github-flow execute recipe-photos` (añade `--captures <ruta>` para importar los exports desde una
carpeta externa). Mientras falten ficheros, la feature queda `blocked` y no se lanza sesión.

Issue: [#83](https://github.com/fernandogaliay-alten/default/issues/83).
