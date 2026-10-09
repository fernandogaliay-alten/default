# Capturas de referencia — `generator-ingredients-step`

Dirección visual del paso 1 del Generador (exportaciones aceptadas de Google Stitch). Ante cualquier
contradicción, manda `DESIGN.md`.

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

- `generador-ingredientes/` — Generador paso 1 "¿Qué tienes?": input con "Añadir", chips con "×",
  contador "N ingredientes añadidos", nube colapsable "Ingredientes sugeridos" y primario anclado
  "Continuar" (deshabilitado hasta añadir al menos uno). Fuente: `docs/design/stitch-prompt.md`
  sección C (paso 1) y sección 3.
