# Captura de referencia — `favoritos-rejilla`

**Favoritos en rejilla**: el toggle en la posición de rejilla y cuatro tarjetas tipográficas en dos
columnas.

| Fichero | Qué muestra |
|---|---|
| `screen.png` / `screen-dark.png` | Cada tarjeta con categoría, título a dos líneas, **ingredientes en una línea** y el filete de color de la cocina; sin foto. |
| `code.html` | Referencia versionada (Chrome headless, 390×844 a 2×). |

Qué mirar:
- La rejilla es donde el **16:10 aprieta**: el título se limita a dos líneas y los ingredientes a una
  línea con recorte. Si se quiere las tres filas también aquí, la tarjeta tendría que subir a **3:2**
  (decisión abierta, OQ6 de `DESIGN.md`).
- Ninguna tarjeta repite el color de la de al lado: el filete sale del `cuisineId`.

Referencia: `docs/design/concepts/typographic-cards/` (prototipos). Manda `DESIGN.md`/ADR 0006.
