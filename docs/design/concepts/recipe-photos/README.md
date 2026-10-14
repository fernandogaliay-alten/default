# Capturas de referencia — `recipe-photos`

Estas capturas son **obligatorias antes de implementar** (política de capturas de UI del repo). Ante
cualquier contradicción, manda `DESIGN.md`.

No son pantallas de la app: son **láminas de fotos de plato** cuyas imágenes se descargan desde el
`code.html`, se empaquetaban como **assets offline** del repo y se mapean por la clave de cada fila.

> **Retirada (2026-10-14, `retire-recipe-photos`, ADR 0006):** los 30 assets de `seed/` y `category/`
> que salieron de estas láminas **ya no viajan en el APK** ni se muestran; `scripts/fetch-recipe-photos.sh`
> se eliminó. Las capturas se conservan **solo como procedencia** del ancla de estilo
> `app/src/main/assets/img/recipes/default.jpg` (137.359 B), que sí se conserva. El mapeo y el
> procedimiento de descarga de abajo quedan como registro histórico.

## Estado

Capturas **completas y válidas** (v2, 2026-10-13). Sustituyen a la primera entrega, rechazada porque
`fotos-semillas/` era la pantalla **Modo Cocina** y `fotos-categorias/` era **una sola imagen compuesta**
(1376x768, ~275x192 por plato).

## Mapeo y descarga (para el implementer)

Ambas láminas exponen **imágenes separadas** a resolución nativa **1376x768**. Descargar cada `src` con
el sufijo `=s0` (sin sufijo, el host devuelve un thumbnail de 512 px).

- `fotos-semillas/code.html` → **12 `<img>` en orden**; el `alt` describe el plato (**no** es la clave).
  Mapear **por posición** a las 12 recetas semilla:
  `seed-tortilla-patatas, seed-gazpacho-andaluz, seed-pasta-tomate, seed-pollo-al-ajillo,
  seed-lentejas-estofadas, seed-salmon-a-la-plancha, seed-tacos-de-pollo, seed-ensalada-de-quinoa,
  seed-tofu-salteado, seed-crema-de-calabaza, seed-tortitas-de-avena, seed-tarta-de-manzana`.
- `fotos-categorias/code.html` → **19 `<img>`**; el `alt` **es exactamente la clave**:
  `cat-pasta, cat-rice, cat-soup_cream, cat-salad, cat-meat, cat-poultry, cat-fish_seafood,
  cat-vegetarian, cat-vegan, cat-legume_stew, cat-pizza_bread, cat-egg_breakfast, cat-sandwich_wrap,
  cat-tapas_appetizer, cat-dessert, cat-smoothie_drink, cat-casserole_oven, cat-other, default`.

Extracción de ejemplo:

```
grep -oE 'src="https://lh3\.googleusercontent\.com/aida[^"]+"' fotos-semillas/code.html
curl -L -o "<clave>.jpg" "<url>=s0"
```

## Convención

Un **subdirectorio por lámina** con los **tres ficheros** del export, tal cual: `screen.png` (o el
`.png`), `code.html` (o el `.html`) y `DESIGN.md` (o el `.md`).

## Pantallas esperadas

- `fotos-semillas/` — **lote A** (12 fotos): una foto por receta semilla. Destino:
  `docs/design/concepts/recipe-photos/fotos-semillas/`.
- `fotos-categorias/` — **lote B** (19 fotos): una por categoría **más** la foto por defecto con la clave
  `default` (genérica/neutra). Destino: `docs/design/concepts/recipe-photos/fotos-categorias/`.

## Reanudar

`github-flow execute recipe-photos`. Issue: [#83](https://github.com/fernandogaliay-alten/default/issues/83).
