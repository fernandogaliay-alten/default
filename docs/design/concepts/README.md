# Conceptos de diseño (Google Stitch)

Carpeta para las exportaciones aceptadas de Stitch. Son **dirección visual**, no especificación
pixel-perfect: ante cualquier contradicción, manda `DESIGN.md` (fuente de verdad de la UI).

## Capturas obligatorias para features de UI

Toda feature con UI (campo `area: ui` en `feature_list.json`, o cualquier comportamiento de
pantalla/visual) debe tener aquí sus **capturas de referencia** antes de implementarse:

- Nombre: `<pantalla>.<ext>`, p. ej. `home.png`, `perfil.png` o `design-system.png`.
- La spec de la feature nombra los ficheros concretos en su sección "Visual Design Impact".
- Si faltan, `planner`, `implementer` y `feature-flow` **se detienen y las piden** a la persona; la
  feature queda `blocked` hasta que existan. No se implementa UI "a ciegas".
- Son entrada obligatoria, no sustituto de `DESIGN.md`: los tokens y reglas de `DESIGN.md` mandan.

## Qué tomar de Stitch y qué no

De Stitch te llevas la **dirección**: el layout de las pantallas, la jerarquía, la hoja "Design System"
(color, tipografía, botones, chips, tarjetas, inputs y nav inferior) y el estilo de los estados
vacío/carga/error.

NO tomas de Stitch assets usables: sus iconos e imágenes son placeholders. En la app los iconos se
resuelven con Material Symbols (viene con Material 3) y las imágenes con placeholders de degradado o
fotos propias. La app es personal y no se publica, así que la licencia de assets no es un bloqueo
(ver `docs/risks-and-open-questions.md`).

## Sesión de (re)generación

1. Pegar `docs/design/stitch-prompt.md` completo en Stitch. Si lo trunca, por bloques: secciones 1–3,
   A–B, C–E, F–G.
2. Revisar cada salida contra `DESIGN.md` (tokens y reglas); descartar o ajustar lo que contradiga.
3. Guardar las exportaciones aceptadas aquí con el nombre `<pantalla>.<ext>` (p. ej. `home.png`,
   `design-system.png`).
4. Registrar su estado en `DESIGN.md` ("Generated Concept Images" y Open Design Questions).
5. Las decisiones abiertas #4 (fotos reales vs ilustración) y #5 (estilo de las ilustraciones de
   vacío) no las resuelve Stitch: se deciden en `DESIGN.md`.

## Checklist

- [ ] Prompt revisado y vigente (`docs/design/stitch-prompt.md`).
- [ ] Pantallas generadas: Onboarding, Home, Generador, Resultados, Detalle, Favoritos, Perfil.
- [ ] Hoja "Design System" incluida (paleta, tipografía, variantes, iconos, nav).
- [ ] Estados por lista: poblado, vacío, carga y error.
- [ ] Modos degradados revisados (sin conexión, sin IA, modo semilla).
- [ ] Exportaciones aceptadas guardadas en esta carpeta.
- [ ] Estado registrado en `DESIGN.md`.
- [ ] Decisiones #4 (fotos) y #5 (ilustraciones) anotadas.
