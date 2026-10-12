# Capturas de referencia — `cooking-mode`

Estas capturas son **obligatorias antes de implementar** (política de capturas de UI del repo). Ante
cualquier contradicción, manda `DESIGN.md`.

## Convención

Un **subdirectorio por pantalla** con los **tres ficheros** que exporta Stitch, tal cual:
`docs/design/concepts/cooking-mode/modo-cocina/` → `screen.png` (o el `.png`), `code.html` y
`DESIGN.md` (el `design.md` del export). Dirección visual, no especificación pixel-perfect.

## Pantallas esperadas

- `docs/design/concepts/cooking-mode/modo-cocina/` — **Modo cocina** (pantalla a pantalla completa que
  recorre los pasos con progreso, Siguiente/Anterior, marcado de pasos y **temporizador por paso**).
  Debe verse: círculo numerado terracota, título y cuerpo del paso, píldora honey con la duración, el
  control del temporizador con la cuenta atrás en cifras tabulares, el checkbox "Marcar paso como
  hecho" y la barra inferior con "Anterior" (outline) y "Siguiente" (terracota); en el último paso,
  "Terminar". Incluye los estados: temporizador parado, en marcha, en pausa y terminado.

## Prompt de Stitch

Usa `docs/design/concepts/cooking-mode/stitch-prompt.md` (copia derivada, lista para pegar). El maestro
`docs/design/stitch-prompt.md` y los trozos `docs/design/stitch/` siguen siendo la fuente de verdad.

## Reanudar

Cuando dejes las capturas en el worktree:

```bash
github-flow execute cooking-mode                 # reutiliza worktree/rama y lanza la sesión
github-flow execute cooking-mode --captures <ruta>   # importa los exports desde una carpeta externa
```
