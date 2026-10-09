# Capturas de referencia — `favorites`

Dirección visual de la pestaña Favoritos (exportación aceptada de Google Stitch). Ante cualquier
contradicción, manda `DESIGN.md`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

Fuente: `DESIGN.md` + `docs/design/stitch/17-favoritos.md` (y `docs/design/stitch-prompt.md` como
referencia completa).

## Pantalla esperada

- `favoritos/` — **captura única** de la pestaña Favoritos: cabecera "Mis recetas guardadas" con línea de
  contador ("12 recetas"); buscador + fila de chips de orden (*Añadidas recientemente*, *Tiempo*,
  *Dificultad*) y toggle lista/rejilla; lista de `RecipeListCard` con miniatura redondeada 96x96, texto a
  la derecha y corazón en el extremo; afordancia de swipe-to-delete con acción visible "Eliminar"; nota
  discreta con icono de nube tachada "Disponibles sin conexión"; **estado vacío** con ilustración de
  marcador, titular "Aún no has guardado ninguna receta", cuerpo "Pulsa el corazón de cualquier receta
  para tenerla siempre a mano." y botón primario "Explorar recetas". Incluye la barra de navegación
  inferior persistente con *Favoritos* activo. Modo claro y oscuro.

Stitch no generó variantes por estado: los estados **poblado** y **vacío** se implementan a partir de
esta misma dirección y se describen en `docs/specs/favorites.md`.

Fuera de alcance: los iconos e imágenes de Stitch son placeholders y en la app se resuelven con Material
Symbols y degradados propios (ver `docs/design/concepts/README.md`).
