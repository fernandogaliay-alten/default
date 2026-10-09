# Capturas de referencia — `recipe-detail-screen`

Dirección visual de la pantalla de Detalle de receta (exportación aceptada de Google Stitch). Ante
cualquier contradicción, manda `DESIGN.md`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

Fuente: `DESIGN.md` + `docs/design/stitch/16-detalle-receta.md` (y `docs/design/stitch-prompt.md`
como referencia completa).

## Pantalla esperada

- `detalle-receta/` — **captura única** del Detalle de receta: foto hero a sangre con botones
  flotantes (atrás y corazón/guardar); tarjeta blanca solapada con título (Manrope Bold 28sp),
  descripción de una línea y meta de cuatro pares icono+valor (Tiempo, Dificultad, Comensales,
  Calorías, cifras tabulares); fila de pestañas fija *Ingredientes · Pasos · Notas*; ajustador de
  comensales ("Comensales − 2 +" con la nota "Cantidades ajustadas automáticamente"); timeline vertical
  de pasos con píldoras de duración; y barra inferior fija con "Guardar" (outline) y "Empezar a cocinar"
  (terracota). Modo claro y oscuro.

Fuera de alcance: el botón "Añadir a la lista de la compra" (la parte `16` de Stitch pide no incluirlo);
los iconos e imágenes de Stitch son placeholders y en la app se resuelven con Material Symbols y
degradados propios.
