# Capturas de referencia — `local-result-filters`

Dirección visual de la fila de filtros locales del bloque "Tus recetas" (exportaciones aceptadas de
Google Stitch). Ante cualquier contradicción, manda `DESIGN.md`.

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

- `filtros-tus-recetas/` — fila de chips del bloque "Tus recetas" (Todas, Postres, Pasta, Arroz,
  Ensaladas, Carnes, Vegetariano, Rápido), con la distinción visual entre categorías y filtros
  derivados (Vegetariano/Rápido), y los estados vacío de usuario nuevo y "sin resultados para este
  filtro". Fuente: `docs/design/stitch-prompt.md` sección B (bloque 5).
