Sazón — Home (tab "Inicio").

Design exactly five stacked blocks, in this order. Everything must look populated immediately; never a
full-screen loading state on Home.

1) HEADER: time-based greeting using the name from the profile, e.g. "Buenas tardes, Lucía", plus a
   small round avatar with the initials on the user's chosen colour. If there is no name, show
   "Buenas tardes" with a generic person icon.
2) AI SEARCH BAR: a pill field with a sparkle icon, placeholder "¿Qué te apetece hoy?" and helper text
   under it: "Pídeselo a la IA: «algo con pollo y arroz»". The sparkle icon must make it obvious this is
   not a normal search.
3) HERO CARD to the Generator: terracotta gradient, white text, thin food illustration,
   "¿Qué tienes en la nevera?" / "Añade tus ingredientes y te proponemos recetas al instante."
   + white pill button "Crear receta".
4) "Sugeridas para ti" with the caption "basado en tus gustos": a horizontal carousel of RecipeCards.
   No filter row here — it is a curated selection.
5) "Tus recetas": one single block:
   - A horizontal filter row of chips — Todas, Postres, Pasta, Arroz, Ensaladas, Carnes, Vegetariano,
     Rápido. Make it visually clear that "Vegetariano" and "Rápido" are derived filters rather than
     recipe categories.
   - Group "Guardadas": horizontal list of favourite RecipeListCards with a filled heart.
   - Group "Abiertas hace poco": horizontal list of RecipeListCards without the heart filled.
   - Empty state for a brand-new user: a short line "Cuando guardes una receta aparecerá aquí."

Also include the persistent bottom navigation bar (80dp, white with rounded top corners; three
destinations — Inicio active with filled home icon + terracotta label, Favoritos, Perfil) and the
terracotta FAB "＋ Crear receta".

Light and dark. Only this screen.
