# Captura de referencia — `recipe-card`

La **tarjeta tipográfica** (dirección **C3 "carta de restaurante"** + los tres ingredientes principales)
en las dos anchuras del sistema.

| Fichero | Qué muestra |
|---|---|
| `screen.png` / `screen-dark.png` | **Inicio**: carrusel "Sugeridas para ti" con la tarjeta a 232 px (16:10) e ingredientes en **una línea** (`pollo · arroz · zanahoria`), más el bloque "Tus recetas" con la fila de filtros y una tarjeta de lista. |
| `screen-resultados.png` / `screen-resultados-dark.png` | **Resultados**: tarjeta ancha (358 px, 16:10) con las **tres filas con cantidad** en cifras tabulares. |
| `code.html`, `code-resultados.html` | Referencia versionada (Chrome headless, 390×844 a 2×). |

Qué mirar:
- El hueco no lleva foto: **categoría en versalitas**, título en Manrope 800, ingredientes con cantidad,
  fila `tiempo · dificultad · comensales` y el **porcentaje de coincidencia** en terracota.
- El **filete de 6 px** del borde inferior usa el degradado de la cocina (`RecipeAccent`).
- La marca de agua del glifo (`RecipeGlyph`) va al 12 % como textura, no como imagen.
- La **regla de 5 px** de la miniatura de Favoritos es **terracota** (`primary`), no por cocina: a 5 px los
  pares suaves de `RecipeAccent` no se perciben (la variación por cocina vive en el filete de 6 px).

Referencia: `docs/design/concepts/typographic-cards/` (prototipos). Manda `DESIGN.md`/ADR 0006.
