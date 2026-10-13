# Capturas de referencia — `card-typographic-tokens-and-design`

Dirección visual de los **tokens de tarjeta** (modo oscuro) según `docs/adr/0006-typographic-recipe-cards.md`.
Ante cualquier contradicción, mandan `DESIGN.md` y el ADR 0006.

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

- `tarjeta-oscura/` — la tarjeta tipográfica sobre el fondo de la app **en modo oscuro**, con la
  decisión **B + C** aplicada: superficie `#3A2E26`, borde `#7D6A5C` (contraste **3,26:1** con
  `darkBackground` `#241C16`) y el tinte cálido de la cocina. Debe verse que la tarjeta **se distingue**
  del fondo (el defecto que corrige esta feature: 1,15:1). Incluir, en la misma captura o en una
  adicional, el mismo par en modo claro para comparar. Fuente: `prototipo-c3.html` (hoja de
  componentes y pantalla Inicio) y `prototipo-c3-imagenes.html` (opciones A/B/C).

## Qué NO entra

- El layout tipográfico de las tarjetas (`RecipeCard`, `RecipeListCard`, rejilla, monograma) es
  `recipe-card-typographic`.
- La capa de fondo con imagen es `dish-artwork-as-card-background`.
