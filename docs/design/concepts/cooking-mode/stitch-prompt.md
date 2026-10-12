# Prompt de Stitch — `cooking-mode` (copia derivada, lista para pegar)

Derivado de `docs/design/stitch/00-contexto.md` + una parte de pantalla nueva para el modo cocina (no
existe trozo previo). El maestro `docs/design/stitch-prompt.md` y `docs/design/stitch/` siguen siendo la
fuente de verdad.

**Cómo usarlo** (igual que `docs/design/stitch/README.md`): abre una conversación **nueva** en Stitch,
pega la **PARTE 0**, espera a que responda "OK", y luego pega la **PARTE 1**. Guarda el export aceptado
en `docs/design/concepts/cooking-mode/modo-cocina/` (`.png` + `.html` + `design.md`).

---

## PARTE 0 — pégalo primero

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
- (AMENDMENT for this session) The step-by-step cooking mode WITH timers and full-screen instructions
  is now IN SCOPE: it is the single screen I will request next. Everything else below still applies.
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

---

## PARTE 1 — pégalo después del "OK"

Sazón — Cooking Mode (step by step, with a per-step timer).

Full-screen page pushed above the tabs (NO bottom navigation bar, like the Recipe Detail). Opened from
the sticky "Empezar a cocinar" button on the Recipe Detail. Light and dark. Only this screen.

Screen content:
- Top area: a floating circular back button (top-left) and a compact progress label "Paso 2 de 5"
  (tabular figures), with a thin linear progress bar under it, terracotta fill.
- Recipe title at the top of the content (Manrope Bold, 24sp, single line, ellipsized if long).
- A large current-step card (white surface, 24px radius, soft shadow): a numbered terracotta circle
  with the step number, the step title (Manrope SemiBold, 22sp) and the step body text (Inter 16sp,
  line height 1.5, wraps and scrolls if long).
- A per-step timer block using the honey accent #F2A93B: a honey pill with the step duration
  ("10 min") and a timer control (start/pause) that shows the countdown in tabular figures ("09:42").
  It must be designed in four states: idle ("Iniciar"), running (countdown + subtle pulse),
  paused and finished. When it finishes, show a soft success notice "¡Listo!" with a honey/olive tint
  (NEVER red) and stop the countdown. If a step has no duration, hide the timer block.
- A row to complete the step: a checkbox with the label "Marcar paso como hecho".
- Sticky bottom action bar (white, soft top shadow, two buttons side by side): an outline "Anterior"
  (disabled on the first step) and a terracotta primary "Siguiente"; on the last step the primary reads
  "Terminar".

Only this screen.
