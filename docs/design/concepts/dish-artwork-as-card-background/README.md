# Capturas de referencia — `dish-artwork-as-card-background`

Dirección visual de la **imagen de IA como capa de fondo** (variante **L2**) según
`docs/adr/0006-typographic-recipe-cards.md`. Ante cualquier contradicción, mandan `DESIGN.md` y el
ADR 0006.

> **Capturas aportadas (2026-10-14):** sí, en esta carpeta, en claro y oscuro. Origen: los **prototipos
> locales** versionados en `docs/design/concepts/typographic-cards/` (no export de Stitch), renderizados
> con Chrome headless a 390×844 y 2×. Es una **desviación de la política de capturas autorizada por la
> persona** (2026-10-13, sesión 101).
>
> Las capturas usan fotos reales del APK **solo como fondo de muestra** (para comprobar cómo lee la
> tipografía encima); no son assets del producto ni deben interpretarse como la imagen que generará la IA.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con el `.png` (claro y
oscuro), el `.html` de referencia y un `design.md`:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

## Pantallas esperadas

- `tarjeta-con-imagen/` — la tarjeta con el ajuste **"Generar imágenes del plato" encendido**: imagen
  16:9 con `scrim` de contraste, categoría y título encima, y los **ingredientes con cantidad y los
  metadatos en una banda sólida** (L2), con el filete de la cocina. Incluir el par **OFF / ON** para que
  se vea que la base sin imagen es la ficha tipográfica. Fuente: `prototipo-c3.html` (hoja de
  componentes, par OFF/ON) y `prototipo-c3-imagenes.html` (L1 a sangre descartada vs L2 elegida).
- `detalle-con-imagen/` — el hero del Detalle (240) con la imagen de fondo, el `scrim` y la tipografía
  encima, seguido de la tira de metadatos. Fuente: `prototipo-c3-imagenes.html` (L2 en claro y oscuro).

Ambas variantes (clara y oscura).

## Qué NO entra

- Los tokens de contraste en oscuro son `card-typographic-tokens-and-design`.
- El layout tipográfico sin imagen es `recipe-card-typographic` y `recipe-detail-typographic-header`.
- La retirada de los assets locales es `retire-recipe-photos`.
