Sazón — Results.

Full-screen page pushed above the tabs.

Screen content:
- Header: floating back button, title "Estas son tus recetas", subtitle
  "A partir de 6 ingredientes · 30 min · 2 comensales".
- When reached from the AI search bar, the title reads "Resultados de tu búsqueda" and the subtitle is
  the typed query in quotes, e.g. "“algo con pollo y arroz”".
- A horizontal chip row for ordering — Relevancia, Más rápida, Más fácil, Mejor valoradas — and an
  "Ajustar" chip with a sliders icon.
- Vertical list of RecipeCards, each with the food image (24px radius), a heart button floating
  top-right over the image, title, one-line description, and a meta row of icons + text
  "25 min · Fácil · 2 pers." plus a small terracotta pill "92% coincidencia".
- Sticky secondary button at the bottom: "Cambiar ingredientes".
- Empty state: simple line illustration of an empty plate, headline
  "No hemos encontrado recetas con esos ingredientes", body copy, primary button "Ajustar mi búsqueda".
- Blocked-by-profile state (only when the query contradicts the user's diet or allergies): a honey-tinted
  card with headline "Tu perfil no permite esta receta" and body
  "Tu alimentación o tus alergias impiden esta búsqueda.", plus a secondary button "Revisar mi perfil".
  Never show the forbidden recipe.
- Error state: "No hemos podido generar las recetas. Revisa tu conexión." + "Reintentar".
- AI-unavailable state: a honey banner "Ahora mismo no podemos generar recetas nuevas. Aquí tienes
  nuestras sugerencias." and, below, the local seed recipes, so the screen is never empty.

Light and dark. Only this screen.
