# Conceptos de diseño (Google Stitch)

Carpeta para las exportaciones aceptadas de Stitch. Son **dirección visual**, no especificación
pixel-perfect: ante cualquier contradicción, manda `DESIGN.md` (fuente de verdad de la UI).

## Capturas obligatorias para features de UI

Toda feature con UI (campo `area: ui` en `feature_list.json`, o cualquier comportamiento de
pantalla/visual) debe tener sus **capturas de referencia** antes de implementarse, en una
**subcarpeta por feature**: `docs/design/concepts/<feature-id>/`.

- Dentro, cada **pantalla** (no componente) se guarda en su propia subcarpeta con los **tres ficheros
  que exporta Stitch** (el `.png`, el `.html` y el `design.md`, con los nombres que dé Stitch):
  `docs/design/concepts/<feature-id>/<pantalla>/` (p. ej.
  `docs/design/concepts/results-screen/resultados/`).
- Si faltan, el agente **crea la subcarpeta** `docs/design/concepts/<feature-id>/` y un `README.md`
  dentro con la **lista exacta de pantallas** esperadas (y una línea de qué es cada una); después se
  detiene, te las pide y la feature queda `blocked`. En `github-flow`, además, `execute` deja la carpeta
  de petición (con `README.md` + `stitch-prompt.md`) **dentro del worktree** de la feature, sin lanzar
  sesión ni ensuciar la base. Tú dejas cada subcarpeta `<pantalla>/` con sus tres ficheros y reanudas
  con `github-flow execute <feature-id>`.
- La spec de la feature nombra esas pantallas en su sección "Visual Design Impact".
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

1. Pegar el prompt **troceado** de `docs/design/stitch/`: primero `00-contexto.md` y después una pantalla
   a la vez, en orden, **en la misma conversación** de Stitch (ver `docs/design/stitch/README.md`).
   `docs/design/stitch-prompt.md` sigue siendo la fuente completa de referencia.
2. Revisar cada salida contra `DESIGN.md` (tokens y reglas); descartar o ajustar lo que contradiga.
3. Guardar cada pantalla aceptada en su subcarpeta de la feature:
   `docs/design/concepts/<feature-id>/<pantalla>/` con los tres ficheros del export
   (el `.png`, el `.html` y el `design.md`, tal cual los exporta Stitch; p. ej.
   `docs/design/concepts/results-screen/resultados/`).
4. Registrar su estado en `DESIGN.md` ("Generated Concept Images" y Open Design Questions).
5. Las decisiones abiertas #4 (fotos reales vs ilustración) y #5 (estilo de las ilustraciones de
   vacío) no las resuelve Stitch: se deciden en `DESIGN.md`.

## Checklist

- [ ] Prompt revisado y vigente (`docs/design/stitch-prompt.md`).
- [ ] Pantallas generadas: Onboarding, Home, Generador, Resultados, Detalle, Favoritos, Perfil.
- [ ] Hoja "Design System" incluida (paleta, tipografía, variantes, iconos, nav).
- [ ] Estados por lista: poblado, vacío, carga y error.
- [ ] Modos degradados revisados (sin conexión, sin IA, modo semilla).
- [ ] Exportaciones aceptadas guardadas en `docs/design/concepts/<feature-id>/`.
- [ ] Estado registrado en `DESIGN.md`.
- [ ] Decisiones #4 (fotos) y #5 (ilustraciones) anotadas.
