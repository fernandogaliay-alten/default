# Captura de referencia — `detalle-con-imagen`

**Detalle con la imagen generada como capa de fondo del hero** (variante **L2**).

| Fichero | Qué muestra |
|---|---|
| `screen.png` / `screen-dark.png` | Hero de 240 con la imagen, el `scrim` de contraste y **categoría, título y los tres ingredientes en blanco** encima; filete de la cocina al pie del hero; debajo, la tira de metadatos, las pestañas, el escalador, el checklist con cantidades y la barra inferior. |
| `code.html` | Referencia versionada (Chrome headless, 390×844 a 2×). |

Qué mirar:
- El hero **conserva su geometría de 240** y la estructura de la pantalla no se mueve al aparecer la
  imagen.
- Si la generación falla, tarda, se cancela o se agota el tope por sesión, el hero se queda **tipográfico**
  (`recipe-detail-typographic-header/detalle-receta/`), sin microcopy ni estado de error.

> La imagen de muestra es una **foto real del APK** (`img/recipes/category/cat-rice.jpg`) usada solo como
> fondo de muestra. No es una imagen generada por la IA ni un asset del producto.

Referencia: `docs/design/concepts/typographic-cards/`. Manda `DESIGN.md`/ADR 0006.
