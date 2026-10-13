# Captura de referencia — `tarjeta-oscura`

Pantalla: **Inicio** (mismo layout que `recipe-card-typographic/recipe-card`), capturada para revisar el
**contraste de la tarjeta en modo oscuro** (decisión **B + C** de ADR 0006).

- `screen.png` — modo claro (referencia del par).
- `screen-dark.png` — modo oscuro: es la captura que justifica la feature.
- `code.html` — referencia versionada (abre en el navegador; `?mode=dark` para la variante oscura).
- Capturas de 390×844 a 2× (780×1688), renderizadas de los prototipos con Chrome headless
  (`--force-device-scale-factor=2`).

Qué mirar:
- La tarjeta usa `darkCardSurface` `#3A2E26` y borde `darkCardOutline` `#7D6A5C`, con el **tinte cálido** de
  la cocina: contraste **3,26:1** con `darkBackground` `#241C16`, frente al **1,15:1** de
  `darkSurface` `#312720` (por debajo del 3:1 de WCAG 1.4.11).
- El borde sustituye a la sombra como elevación: en oscuro la sombra no se ve.
- El navegador inferior, las hojas y el resto de pantallas **no** cambian: son tokens de **tarjeta**.

Referencia: `docs/design/concepts/typographic-cards/{prototipo-c3.html,prototipo-c3-imagenes.html}`.
Manda `DESIGN.md` (y el ADR 0006) si algo contradice esta captura.
