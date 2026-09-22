# Prompt para Google Stitch

Prompt derivado de `DESIGN.md` y de las decisiones del `build-brief`. Las pantallas de Stitch
generadas con el prompt anterior quedan obsoletas: este es el que hay que usar.

Notas de uso:

- Pegar completo como primer mensaje. Si Stitch lo trunca, pegar por bloques: secciones 1–3,
  después A–B, después C–E, y por último F–G.
- Los conceptos que devuelva Stitch son **dirección visual**, no especificación pixel-perfect.
  Si contradicen `DESIGN.md`, manda `DESIGN.md`.
- Guardar las exportaciones aceptadas en `docs/design/concepts/` y marcar su estado en `DESIGN.md`.

---

```text
Design a native Android mobile app UI (Material 3, portrait, Pixel frame 390x844).
App name: "Sazón". All UI copy must be in SPANISH (Spain): every label, button, title, empty
state and microcopy, using the exact strings listed below.

Do not invent screens, flows or features beyond the ones listed in section 4. Anything in
section 5 is explicitly out of scope and must not appear.

============================================================
1. PRODUCT
============================================================
Sazón turns the ingredients a user already has at home into complete recipes using AI.

- First launch: a MANDATORY 7-step onboarding (cannot be skipped, no "skip" affordance anywhere).
  Step 1 captures identity: the user's name and the colour of their initial-based avatar.
  Steps 2-7 build the taste profile: diet pattern, cooking style priorities, favourite cuisines,
  ingredients they like and ingredients they avoid, hard restrictions (allergies/intolerances), and
  cooking level, available time and usual number of servings.
- Home then shows suggestions: always a bundled local recipe catalogue, plus AI-generated recipes
  based on the profile, cached on the device. Home must feel instant and must work offline —
  never show a full-screen loading state on Home; show content immediately.
- The user can also add ingredients, answer a few quick questions, and receive a list of matching
  recipes, each with a detail page.
- Any recipe can be saved to Favorites, which work offline.

There are NO accounts, NO login and NO sync. One single local user per device.

============================================================
2. VISUAL LANGUAGE — "WARM FOODIE"
============================================================
Mood: an appetizing, cozy, premium-but-friendly modern cookbook. Never clinical, never corporate,
never childish. Food photography leads; UI chrome stays quiet.

COLORS (use exactly)
- Primary terracotta  #D9552B   (one primary action per screen, active states, match badge)
- On primary          #FFFFFF
- Primary container   #FFDCCF   (soft accent fills)
- Secondary olive     #5F7A3D   (fresh/vegetable accents)
- Accent honey        #F2A93B   (durations, timers, allergen notices)
- Background          #FFF8F0   (cream)
- Surface             #FFFFFF
- Text                #2B2118   (warm charcoal)
- Outline             #E8DDD3   (hairlines only)
- Success             #4C7A34 · Error #B3261E
Rule: reserve terracotta for ONE primary action per screen. If everything is orange, nothing is.

TYPOGRAPHY
- Headings: Manrope SemiBold/Bold, 28–32sp, slightly tight tracking, title case.
- Body: Inter Regular/Medium, 14–16sp, line height 1.5.
- Every quantifiable number (times, quantities, servings, kcal) uses tabular figures, Medium
  weight, so columns align. This is important: numbers are compared at a glance.
- Only these two families. Strong hierarchy: big title, muted supporting text.

SHAPE, DEPTH, SPACING
- Cards and images 24px radius. Inputs 16px. Chips and buttons fully rounded pills.
- Shadows extremely soft (y=4, blur=16, 6% black). No hard borders to separate cards.
- Screen side padding 16px, 24px vertical rhythm between sections, 12px inside cards.
- Overlapping surface: on the recipe detail, a white card with 24px top corners slides up over
  the hero photo to create depth without heavy shadow.

IMAGERY
- Food photography is central: top-down, warm natural light, shallow depth of field, rustic
  ceramic. Always 24px rounded corners. Never cluttered or stock-generic.
- For placeholders use a soft peach-to-olive gradient block with a thin line-art plate icon.
- Ingredient icons: small, rounded, single-style line icons. Never mix icon styles.
- Avatars are the user's initials on a flat colour from an 8-swatch warm palette. Never photos,
  never faces. If there is no name, use a generic person icon on the chosen colour.

============================================================
3. NAVIGATION
============================================================
- Onboarding: no bottom bar. Shared layout with step progress dots at the top, a "Atrás" text
  button top-left, and the primary "Siguiente" pill pinned at the bottom.
- After onboarding: a persistent bottom navigation bar, 80dp tall, white with rounded top
  corners, three destinations — Inicio (home icon), Favoritos (bookmark icon), Perfil (person
  icon) — plus a prominent terracotta FAB "＋ Crear receta" that opens the Generator.
  Active destination = filled icon + label in terracotta. Inactive = muted grey, icon only.
- Generator, Results and Recipe Detail are full-screen pages pushed above the tabs, with a
  circular floating back button.

============================================================
4. SCREENS
============================================================

A) ONBOARDING — 7 steps, obligatory, shared shell (progress dots, "Atrás", "Siguiente")
  Step 1 "Bienvenida": warm food photo or collage of fresh ingredients, logo, headline
    "Cocina con lo que ya tienes", subhead "Dinos qué te gusta y la IA hará el resto."
    Below, a REQUIRED text field "¿Cómo te llamamos?" with helper "Así te saludaremos en Inicio.",
    and under it a row of 8 circular avatar colour swatches. Show a live preview of the avatar: the
    typed initials on the selected colour. The primary CTA "Empezar" stays disabled until the field
    has content; never show more than 2 initials for long names.
  Step 2 "¿Cómo te alimentas?": large selectable cards with a short description each —
    Omnívoro, Flexitariano, Pescetariano, Vegetariano, Vegano.
  Step 3 "¿Algún estilo que te importe?" (multi-select, optional): a helper line
    "Opcional. Puedes cambiarlo cuando quieras." then chips — Real Fooding, Bajo en calorías,
    Keto / bajo en carbohidratos, Alto en proteína, Sin azúcar añadido, Económico, Aprovechamiento
    and a "Ninguno" option.
  Step 4 "Tus cocinas favoritas": 2-column grid of selectable tiles with a small icon —
    Española, Mediterránea, Italiana, Asiática, Japonesa, India, Mexicana, Americana,
    Oriente Medio, Latina.
  Step 5 "¿Qué te gusta cocinar?": two clearly separated groups on the same screen.
    Group "Me gusta": a chip cloud of common ingredients (tomate, pollo, pasta, arroz, huevo,
    salmón, garbanzos, aguacate, queso, patata, espinacas, zanahoria…) where each chip carries a
    tiny ingredient icon; selected chips fill terracotta.
    Group "Evito": the same chip cloud style but with a different selected treatment — olive green —
    and the helper "No son alergias, solo cosas que prefieres no ver."
    A searchable "+ Añadir otro" input above the cloud, and a note that ingredients are shared
    between both groups (choosing one removes it from the other).
  Step 6 "Alergias e intolerancias": a honey-tinted information banner that reads
    "Lo usamos para que ninguna receta ponga en riesgo tu salud.", then selectable chips —
    Gluten, Lactosa, Frutos secos, Cacahuete, Soja, Marisco, Pescado, Huevo, Sésamo, Apio,
    Mostaza — plus a clearly visible "Ninguna" option.
  Step 7 "Tu cocina": three compact groups — "Nivel de cocina" (Principiante / Intermedio /
    Avanzado), "¿Cuánto tiempo sueles tener?" (Hasta 15 min / 30 min / 45 min / Más de 60 min) and
    "¿Para cuántos cocinas normalmente?" (stepper − 2 +).
    Final CTA: "Empezar a cocinar".
  Allergen chips must be honey-tinted when selected, NEVER red.

B) HOME (tab "Inicio") — exactly five stacked blocks, in this order. Everything must look
   populated immediately; never a full-screen loading state on Home
  1) HEADER: time-based greeting using the name from the profile, e.g. "Buenas tardes, Lucía", plus
     a small round avatar with the initials on the user's chosen colour. If there is no name, show
     "Buenas tardes" with a generic person icon.
  2) AI SEARCH BAR: a pill field with a sparkle icon, placeholder "¿Qué te apetece hoy?" and helper
     text under it: "Pídeselo a la IA: «algo con pollo y arroz»". The sparkle icon must make it
     obvious this is not a normal search. On submit, show the same warm full-screen loading animation
     as the Generator ("Buscando recetas para ti…") and then open the Results screen with the query as
     its subtitle. On failure keep the typed query and show "No hemos podido buscar. Revisa tu
     conexión." with a "Reintentar" action. This is the ONLY block on Home that needs a connection.
     Results always respect the profile: diet and allergies are enforced even if the query asks
     otherwise, so a vegan searching "carne" gets the empty state instead of a meat recipe.
  3) HERO CARD to the Generator: terracotta gradient, white text, thin food illustration,
     "¿Qué tienes en la nevera?" / "Añade tus ingredientes y te proponemos recetas al instante."
     + white pill button "Crear receta".
  4) "Sugeridas para ti" with the caption "basado en tus gustos": horizontal carousel of RecipeCards.
     No filter row here — it is a curated selection.
  5) "Tus recetas": one single block that replaces what used to be three separate sections.
     - Filter row (horizontal chips, applied locally with no network): Todas, Postres, Pasta, Arroz,
       Ensaladas, Carnes, Vegetariano, Rápido. Make it visually clear that "Vegetariano" and "Rápido"
       are derived filters rather than recipe categories.
     - Group "Guardadas": horizontal list of favourite RecipeListCards with a heart.
     - Group "Abiertas hace poco": horizontal list of RecipeListCards without the heart filled.
     - Empty state for a brand-new user: a short line "Cuando guardes una receta aparecerá aquí."
  Offline variant: block 2 becomes muted with "Sin conexión · la búsqueda no está disponible" and
  blocks 1, 3, 4 and 5 render normally from the device.

C) RECIPE GENERATOR
  Step 1 "¿Qué tienes?"
    Title plus helper "Añade los ingredientes que tengas y nosotros hacemos el resto."
    A pill input with an inline "Añadir" button. Added ingredients shown as chips with a small ×.
    A counter "6 ingredientes añadidos". Below, a collapsible "Ingredientes sugeridos" chip
    cloud with small ingredient icons. Sticky primary button "Continuar", disabled until at
    least one ingredient is added.
  Step 2 "Ajusta tu receta"
    Grouped settings, each group inside a white card:
    - "Tiempo disponible": pills Hasta 15 / 30 / 45 / Más de 60 min.
    - "Tipo de comida": chips Desayuno / Comida / Cena / Snack / Postre.
    - "Comensales": stepper "− 2 +" with a people icon, tabular figures, pre-filled with the
      user's usual number of servings.
    - "Dificultad": segmented Fácil / Media / Elaborada.
    - "Alergias a evitar": chips pre-filled from onboarding, honey-tinted, plus an "Editar en mi
      perfil" link. If the user has none, show "Ninguna declarada".
    - "¿Algo más que deba saber?": multiline text area, placeholder
      "Ej.: sin horno, aprovechar las sobras, en 10 minutos…".
    Sticky primary button "Generar recetas".
  Generating state (full screen)
    A slow circular plate/timer animation in terracotta on cream, headline
    "Buscando recetas para ti…", rotating sublines: "Combinando tus ingredientes",
    "Ajustando a 30 minutos", "Evitando tus alergias…", plus a secondary "Cancelar" text button.

D) RESULTS
  Header: floating back button, title "Estas son tus recetas", subtitle
  "A partir de 6 ingredientes · 30 min · 2 comensales".
  When this screen is reached from the AI search bar, the title reads
  "Resultados de tu búsqueda" and the subtitle is the typed query in quotes, e.g.
  "“algo con pollo y arroz”".
  Horizontal chip row for ordering — Relevancia, Más rápida, Más fácil, Mejor valoradas — and an
  "Ajustar" chip with a sliders icon.
  Vertical list of RecipeCards, each with the food image (24px radius), a heart button floating
  top-right over the image, title, one-line description, and a meta row of icons + text:
  "25 min · Fácil · 2 pers." plus a small terracotta pill "92% coincidencia".
  Sticky secondary button at the bottom: "Cambiar ingredientes".
  Empty state: simple line illustration of an empty plate, headline
  "No hemos encontrado recetas con esos ingredientes", body copy, primary button
  "Ajustar mi búsqueda".
  Blocked-by-profile state (only when the query contradicts the user's diet or allergies): a
  honey-tinted card with the headline "Tu perfil no permite esta receta" and the body
  "Tu alimentación o tus alergias impiden esta búsqueda.", plus a secondary button
  "Revisar mi perfil". Never show the forbidden recipe.
  Error state: "No hemos podido generar las recetas. Revisa tu conexión." + "Reintentar".
  AI-unavailable state: a honey banner reading
  "Ahora mismo no podemos generar recetas nuevas. Aquí tienes nuestras sugerencias." and, below,
  the local seed recipes, so the screen is never empty.

E) RECIPE DETAIL
  - Hero photo edge-to-edge at the top, bottom corners 24px, with a floating circular back button
    and a floating circular heart/save button.
  - A white card overlapping the photo with 24px top corners, containing: title (Manrope Bold,
    28sp), one-line description, and a meta strip of four icon+value pairs — Tiempo, Dificultad,
    Comensales, Calorías.
  - Sticky segmented tab row: "Ingredientes · Pasos · Notas".
  - Ingredientes tab: at the top a servings adjuster "Comensales − 2 +" with the caption
    "Cantidades ajustadas automáticamente"; then a checklist of rows, each with a small checkbox,
    ingredient name, ingredient icon and right-aligned quantity in tabular figures, separated by
    hairlines. A "Añadir a la lista de la compra" secondary button is NOT included — omit it.
  - Pasos tab: a vertical timeline with numbered terracotta circles joined by a line; each step
    has a bold one-line title, body text, and a small honey pill with its duration ("5 min").
  - Notas tab: tips, substitutions and a small nutrition card (Calorías, Proteínas, Carbohidratos,
    Grasas) as a compact grid.
  - Sticky bottom action bar (white, soft top shadow, two buttons side by side): outline
    "Guardar" and terracotta "Empezar a cocinar".

F) FAVORITES (tab "Favoritos")
  Header "Mis recetas guardadas" plus a count line "12 recetas".
  A search bar and a sort chip row (Añadidas recientemente, Tiempo, Dificultad), plus a two-button
  list/grid view toggle.
  List of RecipeListCards with 96x96 rounded thumbnails, text on the right, heart on the far right;
  show a swipe-to-delete affordance with a visible "Eliminar" action.
  A small note with a cloud-off icon: "Disponibles sin conexión".
  Empty state: line illustration of a bookmark, headline "Aún no has guardado ninguna receta",
  body "Pulsa el corazón de cualquier receta para tenerla siempre a mano.", primary button
  "Explorar recetas".

G) PROFILE (tab "Perfil")
  Header: a large round avatar (initials on the chosen colour) with the display name beside it, the
  caption "Tu perfil" and an "Editar" link. In edit mode, show a text field "¿Cómo te llamamos?"
  with helper "Así te saludaremos en Inicio." (cannot be saved empty), plus a row of the eight
  circular avatar colour swatches with the selected one ringed in terracotta.
  Section "Resumen": two compact stat cards side by side — "24 recetas generadas" and
  "12 favoritas".
  Section "Sugerencias": a row reading "Última actualización: hace 3 días" with a secondary pill
  button "Actualizar sugerencias" and the helper "Se renuevan cuando cambian tus preferencias."
  Section "Mis preferencias": rows mirroring the onboarding choices — Alimentación, Estilo, Cocinas
  favoritas, Ingredientes que me gustan, Ingredientes que evito — each showing the current values
  as chips and a chevron. Each row opens the SAME editor used in the corresponding onboarding step,
  presented as a bottom sheet over the profile. Do not design separate duplicated editor screens.
  Section "Alergias e intolerancias": honey-tinted card listing the selected restrictions, with
  the caption "Esto se usa para evitar ingredientes peligrosos" and an "Editar" link.
  Section "Mi cocina": Nivel de cocina, Tiempo disponible and Comensales habituales.
  Section "Ajustes": rows for Notificaciones, Idioma, Sobre la app — render them as disabled or
  coming-soon, they are not part of this design.
  A destructive text button "Borrar mis datos".

============================================================
5. OUT OF SCOPE — DO NOT DESIGN
============================================================
- Shopping list.
- Step-by-step cooking mode with timers or full-screen instructions.
- Sharing or exporting a recipe.
- Language selector (the app is Spanish only).
- Notifications or reminders.
- Creating or editing recipes by hand.
No login, sign-up, account, subscription or settings-for-AI-provider screens.

============================================================
6. STATES AND QUALITY BAR
============================================================
- For every list, design the populated state, the empty state, a skeleton loading state (grey
  rounded blocks matching the final geometry) and an error state with a retry action.
- Minimum 48dp touch targets. Sufficient contrast for white text over photography (use a subtle
  gradient scrim).
- Support Android dynamic type: long text wraps and scrolls, never clips.
- Also produce one compact "Design System" sticker sheet frame next to the screens showing: the
  colour palette with hex values, the type scale (Display, Title, Body, Caption), button / chip /
  card / input variants, the ingredient icon style, and the bottom navigation in active and
  inactive states.
```
