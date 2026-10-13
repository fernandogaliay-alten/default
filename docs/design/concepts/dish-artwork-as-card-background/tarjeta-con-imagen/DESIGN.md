# Captura de referencia — `tarjeta-con-imagen`

La tarjeta **con el ajuste de imágenes del plato encendido**: la imagen generada como **capa de fondo**
(variante **L2**), y a la vez el par **OFF/ON** que documenta que la base sin imagen es la ficha
tipográfica.

| Fichero | Qué muestra |
|---|---|
| `screen.png` / `screen-dark.png` | Arriba, **ajuste apagado**: solo ficha tipográfica. Abajo, **ajuste encendido**: imagen 16:9 con `scrim` de contraste, categoría y título encima en blanco, y los **tres ingredientes con cantidad en banda sólida** con los tokens de superficie. |
| `code.html` | Referencia versionada (Chrome headless, 390×844 a 2×). |

Qué mirar:
- La imagen **no mueve el layout**: la tipografía es la capa base y la imagen entra detrás con el
  crossfade de 250 ms (`DishImageArtworkPolicy.CROSSFADE_MILLIS`). Sin hueco ni frame en blanco.
- En modo oscuro la banda sólida es `darkCardSurface` `#3A2E26`; el filo de la tarjeta es el borde
  `#7D6A5C` (3,26:1).
- El **`scrim`** es el recurso que `DESIGN.md` ya exigía para texto blanco sobre fotografía.

> La imagen de muestra es una **foto real del APK** (`img/recipes/category/cat-rice.jpg`) usada solo como
> fondo para comprobar cómo lee la tipografía encima. No es una imagen generada por la IA ni un asset del
> producto. Sirve además para ver el caso que motiva ADR 0006: esa foto de categoría **no** corresponde al
> plato "Arroz con pollo criollo".

Referencia: `docs/design/concepts/typographic-cards/{prototipo-c3.html,prototipo-c3-imagenes.html}`.
Manda `DESIGN.md`/ADR 0006.
