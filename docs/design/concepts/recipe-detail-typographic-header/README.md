# Capturas de referencia — `recipe-detail-typographic-header`

Dirección visual del **hero tipográfico del Detalle** según
`docs/adr/0006-typographic-recipe-cards.md`. Ante cualquier contradicción, mandan `DESIGN.md` y el
ADR 0006.

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

- `detalle-receta/` — el Detalle con el **hero tipográfico**: categoría en versalitas, título grande,
  los **tres ingredientes principales con cantidad** y el filete de la cocina, con la marca de agua del
  glifo al 12 %; debajo, la **tira de metadatos** (Tiempo · Dificultad · Comensales · Calorías), las
  pestañas (Ingredientes · Pasos · Notas), el escalador de comensales, el checklist de ingredientes con
  cantidades y la barra inferior (Guardar / Empezar a cocinar). Debe verse que **ya no hay tarjeta
  blanca superpuesta sobre la foto**. Fuente: `prototipo-c3.html` (pantalla Detalle).

Ambas variantes (clara y oscura).

## Qué NO entra

- La variante del Detalle **con imagen de fondo** es `dish-artwork-as-card-background`
  (`detalle-con-imagen/`).
- Las tarjetas de Inicio, Resultados y Favoritos son `recipe-card-typographic`.
