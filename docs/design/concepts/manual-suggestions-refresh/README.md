# Capturas de referencia — `manual-suggestions-refresh`

Dirección visual de la sección "Sugerencias" del Perfil (exportaciones aceptadas de Google Stitch).
Ante cualquier contradicción, manda `DESIGN.md`.

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

- `perfil-sugerencias/` — sección "Sugerencias" del Perfil: línea "Última actualización: hace X días",
  botón secundario "Actualizar sugerencias" (estados normal / "Actualizando…" deshabilitado) y helper
  "Se renuevan cuando cambian tus preferencias."; incluye el estado sin lote. Fuente:
  `docs/design/stitch-prompt.md` sección G ("Section Sugerencias").
