# Captura de referencia — `detalle-receta`

**Detalle con hero tipográfico**: sustituye el hero fotográfico con la tarjeta blanca superpuesta.

| Fichero | Qué muestra |
|---|---|
| `screen.png` / `screen-dark.png` | Hero de 240 con categoría en versalitas, título grande y los **tres ingredientes principales con cantidad**, marca de agua del glifo al 12 % y filete de la cocina; debajo, la **tira de metadatos** (Tiempo · Dificultad · Comensales · Calorías), las pestañas (Ingredientes · Pasos · Notas), el escalador de comensales, el checklist de ingredientes con cantidades y la barra inferior (Guardar / Empezar a cocinar). |
| `code.html` | Referencia versionada (Chrome headless, 390×844 a 2×). |

Qué mirar:
- **Desaparece el solape** de la tarjeta blanca sobre la foto: el hero y la tira de metadatos ocupan su
  lugar, sin sombras fuertes (regla de `DESIGN.md` → Shapes).
- Geometría conservada: hero 240, mismo checklist, mismo stepper y misma barra inferior.
- La variante **con imagen generada** (ajuste encendido) está en
  `dish-artwork-as-card-background/detalle-con-imagen/`.

Referencia: `docs/design/concepts/typographic-cards/` (prototipos). Manda `DESIGN.md`/ADR 0006.
