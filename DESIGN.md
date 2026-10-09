---
name: Sazón
description: App Android de recetas generadas por IA a partir de los ingredientes del usuario. Identidad cálida y apetitosa, tipo libro de cocina moderno.
designAssets:
  sourceOfTruth: []
  generatedConcepts: []
colors:
  primary: "#D9552B"
  onPrimary: "#FFFFFF"
  primaryContainer: "#FFDCCF"
  secondary: "#5F7A3D"
  accent: "#F2A93B"
  background: "#FFF8F0"
  surface: "#FFFFFF"
  text: "#2B2118"
  outline: "#E8DDD3"
  success: "#4C7A34"
  error: "#B3261E"
  darkBackground: "#241C16"
  darkSurface: "#312720"
typography:
  display:
    fontFamily: Manrope
    fontSize: 32sp
    fontWeight: 700
  h1:
    fontFamily: Manrope
    fontSize: 28sp
    fontWeight: 600
  body:
    fontFamily: Inter
    fontSize: 16sp
    fontWeight: 400
  caption:
    fontFamily: Inter
    fontSize: 12sp
    fontWeight: 500
rounded:
  sm: 16px
  md: 24px
  full: 999px
spacing:
  sm: 12px
  md: 16px
  lg: 24px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.full}"
    height: 52dp
  button-secondary:
    backgroundColor: transparent
    textColor: "{colors.text}"
    borderColor: "{colors.outline}"
    rounded: "{rounded.full}"
  chip-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.full}"
  chip-selected-secondary:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.full}"
  recipe-card:
    backgroundColor: "{colors.surface}"
    imageRatio: "16:10"
    rounded: "{rounded.md}"
    shadow: "y=4 blur=16 alpha=6%"
  bottom-nav:
    backgroundColor: "{colors.surface}"
    activeColor: "{colors.primary}"
    inactiveColor: "#9A8C7F"
    height: 80dp
---

# Design Direction

## Overview

"Sazón" debe sentirse como un libro de cocina moderno: **cálido, apetitoso y limpio**, nunca clínico
ni corporativo. La comida y las fotos mandan; el cromo de la interfaz se retira para no competir. La
identidad visual se apoya en tres ideas: fondo crema, tarjetas blancas con esquinas muy redondeadas,
y un naranja terracota reservado para la acción principal.

Esta dirección nace del prompt de Stitch y de las decisiones de discovery. **Es la fuente de verdad
de la UI**: si un mockup generado contradice estos tokens o estas reglas, manda este documento.

## Existing Design Assets

- Pantallas de **Google Stitch** ya generadas por el usuario, basadas en un prompt previo.
- **Autoridad: obsoletas / a regenerar.** No se importan al repositorio. Deben regenerarse a partir de
  este documento y del prompt derivado (ver Open Design Questions).
- Cuando se regeneren, las exportaciones aceptadas se guardarán en `docs/design/concepts/` y se
  marcarán aquí como referencia de dirección, no como especificación pixel-perfect.

## Generated Concept Images

Ninguna en el repositorio todavía. La política de capturas de UI (PR #37) exige una captura por
**pantalla** en `docs/design/concepts/<feature-id>/<pantalla>/` (el `.png`, el `.html` y el `design.md`
del export de Stitch). Las features de UI aceptadas **antes** de esa política tienen pendiente este
**backfill**; la petición concreta de cada una vive en su `docs/design/concepts/<feature-id>/README.md`:

- `design-tokens-and-theme`: `design-system`
- `onboarding-shell-and-identity`: `bienvenida`
- `onboarding-diet-style-cuisines`: `dieta`, `estilo`, `cocinas`
- `onboarding-ingredients-safety-kitchen`: `ingredientes`, `alergias`, `tu-cocina`
- `home-offline-blocks`: `home`, `home-sin-conexion`
- `local-result-filters`: `filtros-tus-recetas`
- `generator-ingredients-step`: `generador-ingredientes`
- `generator-settings-step`: `generador-ajustes`
- `generation-flow-with-validation`: `generador-cargando`, `resultados-preview`
- `profile-screen`: `perfil`, `perfil-editar`
- `manual-suggestions-refresh`: `perfil-sugerencias`

Al aceptar cada exportación, registrar aquí su estado como **dirección visual** (no especificación
pixel-perfect).

## Product Feel

- **Apetitoso antes que técnico**: la foto del plato siempre presente, con luz cálida y natural.
- **Cercano, no infantil**: redondeado y amable, pero con tipografía y jerarquía de nivel editorial.
- **Tranquilo**: mucho aire, pocos colores, sin adornos innecesarios.
- **Táctil**: botones altos, chips tipo píldora, sombras muy suaves.

## Colors

| Rol | Hex | Uso |
|---|---|---|
| Primary | `#D9552B` | Acción principal, estado activo, coincidencia |
| Primary container | `#FFDCCF` | Fondos suaves de acento |
| Secondary | `#5F7A3D` | Ingredientes frescos, vegetal, etiquetas saludables |
| Accent | `#F2A93B` | Temporizadores, duración de pasos, avisos de alérgenos |
| Background | `#FFF8F0` | Fondo de pantalla |
| Surface | `#FFFFFF` | Tarjetas, hojas, barras |
| Text | `#2B2118` | Texto principal |
| Outline | `#E8DDD3` | Hairlines y separadores |
| Avatar (iniciales) | 8 tonos cálidos | Identidad; lista canónica en `avatarColors` de `docs/domain/taxonomy.json` |

Regla de uso: el terracota se reserva para **una** acción principal por pantalla. Si todo es naranja,
nada lo es.

## Typography

- **Títulos**: Manrope, SemiBold/Bold, 28–32sp, tracking ligeramente cerrado.
- **Cuerpo**: Inter, 14–16sp, interlineado 1.5.
- **Números** (tiempos, cantidades, comensales): cifras tabulares, peso Medium. Es información que el
  usuario compara de un vistazo; debe alinearse.
- Máximo dos familias tipográficas.

## Layout

- Retrato, un solo pulgar. Padding lateral 16px, ritmo vertical 24px, interior de tarjeta 12px.
- Una idea por sección; los títulos de sección son pequeños y en mayúsculas discretas, no gigantes.
- Carruseles horizontales para sugerencias, listas verticales para resultados y favoritos.
- Barras de acción pegadas al fondo (`sticky`) en el detalle y en el asistente.

## Shapes

- Tarjetas e imágenes: 24px. Inputs: 16px. Chips y botones: píldora completa.
- Sombras solo suaves y de baja opacidad; nunca bordes duros para separar tarjetas.
- Superficies superpuestas (la tarjeta blanca que sube sobre la foto del detalle) para dar
  profundidad sin sombras fuertes.

## Components

- **RecipeCard** (vertical): imagen 16:10 redondeada, título a 2 líneas máximo, fila de metadatos
  `tiempo · dificultad · comensales` y una píldora terracota con el porcentaje de coincidencia.
- **RecipeListCard** (horizontal): miniatura 96x96, texto a la derecha, corazón a la derecha.
- **FilterChip**: fila desplazable; seleccionado relleno de terracota.
- **Stepper** de comensales: `− 2 +` con cifras tabulares.
- **Paso de receta**: línea de tiempo vertical con círculos numerados en terracota y una píldora
  ámbar con la duración.
- **Aviso de alérgenos**: tarjeta con tinte ámbar, nunca roja. Es información de seguridad, no una
  alarma.
- **Estados vacío/carga/error**: ilustración lineal sencilla + titular cálido + una única acción. El
  esqueleto de carga usa bloques grises redondeados con la geometría final.
- **Aviso de modo degradado** (sin conexión o sin IA): franja ámbar discreta bajo la cabecera; el
  contenido local se muestra igual. Nunca una pantalla vacía.
- **Buscador con IA**: píldora en la cabecera de la Home, con icono de destello para distinguirlo de
  un buscador normal. Al enviar, lleva a Resultados con la consulta como subtítulo.

## Core Screens

1. **Onboarding** (7 pasos, obligatorio, sin barra inferior): bienvenida con **nombre y color de
   avatar**, patrón de dieta, preferencias de estilo, cocinas favoritas, ingredientes que me gustan +
   ingredientes que evito, restricciones duras, y "tu cocina" (nivel, tiempo disponible y comensales
   habituales).
2. **Home** con cinco bloques: cabecera (saludo y avatar de iniciales), **buscador con IA** —el único
   bloque que necesita red y que debe degradar con aviso—, tarjeta-héroe "¿Qué tienes en la nevera?"
   que abre el Generador, carrusel "Sugeridas para ti", y el bloque **"Tus recetas"** con filtros
   locales (categoría + "Rápido"/"Vegetariano") y dos grupos: "Guardadas" y "Abiertas hace poco".
3. **Generador**: paso 1 ingredientes (chips + input), paso 2 ajustes agrupados en tarjetas, estado
   de generación con animación cálida.
4. **Resultados**: chips de orden + lista de RecipeCard; estados vacío, error y **modo semilla** (sin
   IA disponible, se muestran las recetas locales).
5. **Detalle**: foto ancha, tarjeta superpuesta con metadatos, pestañas Ingredientes · Pasos · Notas,
   barra inferior con "Guardar" y "Empezar a cocinar".
6. **Favoritos**: búsqueda, orden, lista, estado vacío con nota "Disponibles sin conexión".
7. **Perfil**: identidad (nombre y color de avatar con vista previa), preferencias editables
   (alimentación, estilo, cocinas, ingredientes que me gustan e ingredientes que evito), comensales
   habituales, tarjeta ámbar de alérgenos, Resumen de actividad (recetas generadas y favoritas),
   acción "Actualizar sugerencias" con la fecha del último refresco, y ajustes.

## Responsive Baseline

Diseño base para Pixel 390x844. Debe sobrevivir a pantallas pequeñas (360x640) y a tipografía
ampliada por accesibilidad sin romper layouts: los textos largos hacen scroll, nunca se recortan.
Si el modo oscuro entra en el MVP (ver Open Design Questions), esbozarlo con `darkBackground` y
`darkSurface`, conservando los acentos terracota y oliva.

## Accessibility Baseline

- Objetivos táctiles mínimos de 48dp.
- Contraste suficiente de texto sobre fotografía: usar un degradado sutil bajo el texto blanco.
- No depender solo del color para transmitir estado (los alérgenos llevan icono y etiqueta, no solo
  ámbar).
- Soporte de tipografía dinámica de Android.
- El estado activo de la barra inferior combina icono relleno + etiqueta, no solo color.

## Do's and Don'ts

**Sí**
- Deja que la foto del plato sea la protagonista.
- Usa el terracota para una sola acción principal por pantalla.
- Mantén cifras tabulares en todo lo cuantificable.
- Muestra siempre estados vacíos y de carga cuidados.

**No**
- No usar rojo de alarma para alérgenos o errores de contenido.
- No introducir una tercera tipografía ni gradientes llamativos.
- No añadir bordes duros ni sombras marcadas.
- No inventar pantallas o flujos que no estén en `docs/build-brief.md`.

## Open Design Questions

1. **Prompt de Stitch**: generado y guardado en `docs/design/stitch-prompt.md`. Las pantallas
   actuales quedan obsoletas; hay que regenerarlas con ese prompt. Al aceptar exportaciones,
   guardarlas en `docs/design/concepts/` y registrar su estado en este documento.
2. ¿Existe nombre de producto e icono definitivos, o "Sazón" es un marcador de posición?
3. **Resuelto (2026-10-06):** el modo oscuro **entra en el MVP**. Se implementa la variante oscura con
   `darkBackground`/`darkSurface` y los acentos terracota y oliva (feature `design-tokens-and-theme`).
4. ¿Las fotos de receta son reales (banco de imágenes) o ilustraciones? Afecta al tono general.
5. ¿La ilustración de los estados vacíos es un estilo definido (línea, color plano) o se improvisa?
