# Capturas de referencia — `home-offline-blocks`

Dirección visual de la Home (exportaciones aceptadas de Google Stitch). Ante cualquier contradicción,
manda `DESIGN.md`.

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

- `home/` — Home (tab "Inicio") con los bloques 1 (cabecera), 3 (hérao al Generador), 4
  ("Sugeridas para ti") y 5 ("Tus recetas" con filtros, "Guardadas" y "Abiertas hace poco"), más la
  barra inferior y el FAB "＋ Crear receta". El bloque 2 (buscador IA) pertenece a `ai-search`.
  Fuente: `docs/design/stitch-prompt.md` sección B y sección 3.
- `home-sin-conexion/` — variante offline: bloque 2 atenuado con "Sin conexión · la búsqueda no está
  disponible" y el resto desde el dispositivo. Fuente: sección B (variante offline).
