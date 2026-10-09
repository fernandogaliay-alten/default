# Capturas de referencia — `results-screen`

Dirección visual de la pantalla de Resultados (exportación aceptada de Google Stitch). Ante cualquier
contradicción, manda `DESIGN.md`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

Fuente: `DESIGN.md` + `docs/design/stitch-prompt.md` sección D + `docs/design/stitch/15-resultados.md`.

## Pantalla esperada

- `resultados/` — **captura única** de Resultados: lista de tarjetas con imagen, nombre, tiempo,
  dificultad, comensales y píldora de coincidencia; acción principal terracota.

Stitch no generó variantes por estado. Los estados **poblado (desde el Generador)**, **con subtítulo de
búsqueda**, **vacío** y **error/modo degradado** (franja ámbar, nunca roja, con "Reintentar") se
implementan a partir de esta misma dirección y se describen en `docs/specs/results-screen.md`.

El chip de orden y el estado "bloqueado por perfil" que aparecen en Stitch quedan **fuera** de alcance.
