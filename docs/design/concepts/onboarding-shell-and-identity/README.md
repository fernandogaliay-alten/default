# Capturas de referencia — `onboarding-shell-and-identity`

Dirección visual del armazón de onboarding y el paso 1 de identidad (exportaciones aceptadas de Google
Stitch). Ante cualquier contradicción, manda `DESIGN.md`.

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

- `bienvenida/` — paso 1 "Bienvenida": armazón compartido (progreso "Paso 1 de 7", "Atrás" no
  disponible, primario anclado), campo requerido "¿Cómo te llamamos?", swatches de color de avatar y
  vista previa de iniciales; CTA "Empezar" deshabilitado hasta escribir un nombre. Fuente:
  `docs/design/stitch-prompt.md` sección A (paso 1) y sección 3.
