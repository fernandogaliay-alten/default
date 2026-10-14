# Conceptos de diseño (Google Stitch)

Carpeta para las exportaciones aceptadas de Stitch. Son **dirección visual**, no especificación
pixel-perfect: ante cualquier contradicción, manda `DESIGN.md` (fuente de verdad de la UI).

## Capturas de referencia para features de UI (opcionales)

Las capturas son **material de apoyo opcional**, no un requisito previo: `DESIGN.md` es la fuente de
verdad visual y el flujo nunca se bloquea por su ausencia. Cuando existan, se guardan en una
**subcarpeta por feature**: `docs/design/concepts/<feature-id>/`.

- Dentro, cada **pantalla** (no componente) se guarda en su propia subcarpeta con los **tres ficheros
  que exporta Stitch** (el `.png`, el `.html` y el `design.md`, con los nombres que dé Stitch):
  `docs/design/concepts/<feature-id>/<pantalla>/` (p. ej.
  `docs/design/concepts/results-screen/resultados/`).
- La spec de la feature puede nombrar esas pantallas en su sección "Visual Design Impact" y usarlas como
  referencia, pero su ausencia no bloquea la planificación ni la implementación.
- Nunca sustituyen a `DESIGN.md`: los tokens y reglas de `DESIGN.md` mandan.
- Utilidad manual para importarlas desde otra carpeta (WSL o Windows), fuera del flujo:
  `scripts/import-stitch-captures.sh <feature-id> <ruta>` las copia a la subcarpeta de la feature.

**Dirección tipográfica (ADR 0006, desviación autorizada).** La dirección "tarjeta tipográfica" se
decidió sobre **prototipos HTML locales**, no sobre un export de Stitch. Los prototipos que la sostienen
están versionados en `docs/design/concepts/typographic-cards/` y sus `.png` por pantalla van en
`docs/design/concepts/<feature-id>/<pantalla>/` (en claro y oscuro).

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
