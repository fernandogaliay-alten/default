# Capturas de referencia — `design-tokens-and-theme`

Dirección visual de los tokens y el tema de la app (exportaciones aceptadas de Google Stitch). Ante
cualquier contradicción, manda `DESIGN.md`.

> **Backfill (PR #37):** esta feature se implementó antes de la política de capturas de UI. Faltan sus
> capturas; déjalas en las subcarpetas de abajo y registra el estado en `DESIGN.md`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

## Pantallas esperadas

- `design-system/` — hoja "Design System" (sticker sheet): paleta con hex, escala tipográfica
  (Display/Title/Body/Caption), variantes de botón/chip/tarjeta/input, estilo de iconos de ingrediente
  y nav inferior en activo/inactivo. Fuente: `docs/design/stitch-prompt.md` sección 6.
