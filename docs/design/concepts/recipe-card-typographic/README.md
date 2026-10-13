# Capturas de referencia — `recipe-card-typographic`

Dirección visual de la **tarjeta tipográfica** (dirección **C3 "carta de restaurante"** más los tres
ingredientes principales) según `docs/adr/0006-typographic-recipe-cards.md`. Ante cualquier
contradicción, mandan `DESIGN.md` y el ADR 0006.

> **Capturas aportadas (2026-10-14):** sí, en esta carpeta, en claro y oscuro. Origen: los **prototipos
> locales** versionados en `docs/design/concepts/typographic-cards/` (no export de Stitch), renderizados
> con Chrome headless a 390×844 y 2×. Es una **desviación de la política de capturas autorizada por la
> persona** (2026-10-13, sesión 101).

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con el `.png` (claro y
oscuro), el `.html` de referencia y un `design.md`:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

## Pantallas esperadas

- `recipe-card/` — la tarjeta en Inicio (carrusel "Sugeridas para ti") y en Resultados (lista vertical),
  en las dos anchuras: **estrecha** (ingredientes en una línea, separados por `·`) y **ancha** (tres
  filas con cantidad en cifras tabulares). Debe incluir el filete de 6px con el degradado de la cocina y
  la marca de agua del glifo al 12 %. Fuente: `prototipo-c3.html` (Inicio, Resultados y hoja de
  componentes).
- `favoritos-lista/` — Favoritos en lista: miniatura **96×96 con monograma** sobre `surfaceVariant` con
  la regla de color de la cocina a la izquierda, texto a la derecha y corazón. Fuente:
  `prototipo-c3.html` (Favoritos lista).
- `favoritos-rejilla/` — Favoritos en rejilla: tarjetas tipográficas en dos columnas con los
  ingredientes en una línea. Fuente: `prototipo-c3.html` (Favoritos rejilla).

Ambas variantes (clara y oscura) en cada pantalla.

## Qué NO entra

- Los tokens de tarjeta en oscuro (superficie, borde y contraste) son
  `card-typographic-tokens-and-design`.
- El Detalle es `recipe-detail-typographic-header`.
- La capa de fondo con imagen es `dish-artwork-as-card-background`.
