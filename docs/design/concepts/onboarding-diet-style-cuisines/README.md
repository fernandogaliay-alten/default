# Capturas de referencia — `onboarding-diet-style-cuisines`

Dirección visual de los pasos 2–4 del onboarding (exportaciones aceptadas de Google Stitch). Ante
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

- `dieta/` — paso 2 "¿Cómo te alimentas?": tarjetas grandes seleccionables con descripción
  (Omnívoro, Flexitariano, Pescetariano, Vegetariano, Vegano). Fuente: `docs/design/stitch-prompt.md`
  sección A (paso 2).
- `estilo/` — paso 3 "¿Algún estilo que te importe?" (multi-select, opcional): chips con "Ninguno".
  Fuente: sección A (paso 3).
- `cocinas/` — paso 4 "Tus cocinas favoritas": rejilla a 2 columnas de tiles con icono. Fuente:
  sección A (paso 4).
