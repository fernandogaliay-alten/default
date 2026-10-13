# Captura de referencia — `favoritos-lista`

**Favoritos en lista**: buscador, chips de orden (por defecto **"Recientes"**), toggle lista/rejilla y la
nota "Disponibles sin conexión".

| Fichero | Qué muestra |
|---|---|
| `screen.png` / `screen-dark.png` | Cuatro filas con **miniatura 96×96 de monograma** de iniciales sobre `surfaceVariant`, con la **regla terracota de 5 px** a la izquierda, título y metadatos a la derecha y el corazón. |
| `code.html` | Referencia versionada (Chrome headless, 390×844 a 2×). |

Qué mirar:
- La miniatura **ya no es una foto**: es el monograma del plato (AC, TP, CG, GA). Es el cambio de esta
  pantalla respecto a la captura aceptada de `favorites`.
- El resto (buscador, chips, corazón, nota de sin conexión y navegador inferior) **no cambia**.

Referencia: `docs/design/concepts/typographic-cards/` (prototipos). Manda `DESIGN.md`/ADR 0006.
