# Stitch — prompt para `generator-ingredients-step` (Generador: paso de ingredientes)

Prompt listo para pegar en Google Stitch, generado a partir de `docs/design/stitch/` (`00-contexto.md` + `12-generador-ingredientes.md`). Procedimiento completo en `docs/design/stitch/README.md`. Ante cualquier contradicción, manda `DESIGN.md`.

## Pantallas y carpeta destino

- `generador-ingredientes/` → `docs/design/concepts/generator-ingredients-step/generador-ingredientes/`

## Cómo usarlo

1. Abre una conversación nueva en Stitch.
2. Pega **la Parte 1** (contexto) y espera a que responda "OK".
3. Pega **la Parte 2** (la pantalla) y espera a que genere antes de enviar la siguiente.
4. Revisa cada salida contra `DESIGN.md` (tokens y reglas); descarta o ajusta lo que contradiga.
5. Guarda cada exportación aceptada en su subcarpeta de arriba con los tres ficheros que da Stitch (`*.png`, `*.html`, `design.md`).
6. Registra su estado en `DESIGN.md` ("Generated Concept Images") y reanuda la feature con `github-flow execute generator-ingredients-step`.

---

## Parte 1 — Contexto (pegar primero, esperar "OK")

```text
Design a native Android mobile app UI (Material 3, portrait, Pixel frame 390x844).

App name: "Sazón". All UI copy must be in SPANISH (Spain): every label, button, title, empty state and
microcopy, using the exact strings listed in the screen requests that follow.

This is the SHARED CONTEXT for the whole app. I will then ask you for the screens ONE AT A TIME. Do not
invent screens, flows or features beyond the ones I request. There are NO accounts, NO login and NO
sync; one single local user per device.

============================================================
1. PRODUCT
============================================================
Sazón turns the ingredients a user already has at home into complete recipes using AI.

- First launch: a MANDATORY 7-step onboarding (cannot be skipped, no "skip" affordance anywhere).
  Step 1 captures identity: the user's name and the colour of their initial-based avatar. Steps 2-7
  build the taste profile: diet pattern, cooking style priorities, favourite cuisines, ingredients
  they like and ingredients they avoid, hard restrictions (allergies/intolerances), and cooking level,
  available time and usual number of servings.
- Home then shows suggestions: always a bundled local recipe catalogue, plus AI-generated recipes based
  on the profile, cached on the device. Home must feel instant and must work offline — never show a
  full-screen loading state on Home; show content immediately.
- The user can also add ingredients, answer a few quick questions, and receive a list of matching
  recipes, each with a detail page.
- Any recipe can be saved to Favorites, which work offline.

============================================================
2. VISUAL LANGUAGE — "WARM FOODIE"
============================================================
Mood: an appetizing, cozy, premium-but-friendly modern cookbook. Never clinical, never corporate, never
childish. Food photography leads; UI chrome stays quiet.

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
- Headings: Manrope SemiBold/Bold, 28-32sp, slightly tight tracking, title case.
- Body: Inter Regular/Medium, 14-16sp, line height 1.5.
- Every quantifiable number (times, quantities, servings, kcal) uses tabular figures, Medium weight,
  so columns align. Numbers are compared at a glance.
- Only these two families. Strong hierarchy: big title, muted supporting text.

SHAPE, DEPTH, SPACING
- Cards and images 24px radius. Inputs 16px. Chips and buttons fully rounded pills.
- Shadows extremely soft (y=4, blur=16, 6% black). No hard borders to separate cards.
- Screen side padding 16px, 24px vertical rhythm between sections, 12px inside cards.
- Overlapping surface: on the recipe detail, a white card with 24px top corners slides up over the
  hero photo to create depth without heavy shadow.

IMAGERY
- Food photography is central: top-down, warm natural light, shallow depth of field, rustic ceramic.
  Always 24px rounded corners. Never cluttered or stock-generic.
- For placeholders use a soft peach-to-olive gradient block with a thin line-art plate icon.
- Ingredient icons: small, rounded, single-style line icons. Never mix icon styles.
- Avatars are the user's initials on a flat colour from an 8-swatch warm palette. Never photos, never
  faces. If there is no name, use a generic person icon on the chosen colour.

============================================================
3. NAVIGATION
============================================================
- Onboarding: no bottom bar. Shared layout with step progress dots at the top, a "Atrás" text button
  top-left, and the primary "Siguiente" pill pinned at the bottom.
- After onboarding: a persistent bottom navigation bar, 80dp tall, white with rounded top corners,
  three destinations — Inicio (home icon), Favoritos (bookmark icon), Perfil (person icon) — plus a
  prominent terracotta FAB "＋ Crear receta" that opens the Generator. Active destination = filled icon
  + label in terracotta. Inactive = muted grey, icon only.
- Generator, Results and Recipe Detail are full-screen pages pushed above the tabs, with a circular
  floating back button.

============================================================
4. OUT OF SCOPE — DO NOT DESIGN
============================================================
- Shopping list.
- Step-by-step cooking mode with timers or full-screen instructions.
- Sharing or exporting a recipe.
- Language selector (the app is Spanish only).
- Notifications or reminders.
- Creating or editing recipes by hand.
- No login, sign-up, account, subscription or settings-for-AI-provider screens.

============================================================
5. STATES AND QUALITY BAR (applies to every screen I request)
============================================================
- For every list, design the populated state, the empty state, a skeleton loading state (grey rounded
  blocks matching the final geometry) and an error state with a retry action.
- Minimum 48dp touch targets. Sufficient contrast for white text over photography (use a subtle
  gradient scrim).
- Support Android dynamic type: long text wraps and scrolls, never clips.

Reply "OK" when you have this context. I will then send the screens one at a time.
```

---

## Parte 2 — generador-ingredientes (pegar después)

```text
Sazón — Recipe Generator, step 1 of 2: "¿Qué tienes?".

Full-screen page pushed above the tabs, with a circular floating back button and the primary button
pinned at the bottom.

Screen content:
- Title, plus helper "Añade los ingredientes que tengas y nosotros hacemos el resto."
- A pill input with an inline "Añadir" button.
- Added ingredients shown as chips with a small "×".
- A counter, e.g. "6 ingredientes añadidos" (tabular figures).
- Below, a collapsible "Ingredientes sugeridos" chip cloud with small ingredient icons.
- Sticky primary button "Continuar", DISABLED until at least one ingredient is added.

Light and dark. Only this screen.
```
