# Capturas de referencia — `generation-flow-with-validation`

Dirección visual del estado de generación y el preview de resultados dentro del Generador
(exportaciones aceptadas de Google Stitch). Ante cualquier contradicción, manda `DESIGN.md`.

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

- `generador-cargando/` — estado de generación a pantalla completa: animación cálida terracota,
  titular "Buscando recetas para ti…", sublínas rotatorias y "Cancelar" como texto. Fuente:
  `docs/design/stitch-prompt.md` sección C ("Generating state").
- `resultados-preview/` — preview de resultados dentro del Generador (tarjetas con imagen, título,
  descripción, meta de iconos y pill de coincidencia). Fuente: sección D.
