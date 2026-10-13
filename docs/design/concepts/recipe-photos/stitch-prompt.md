# Prompt de Stitch — `recipe-photos` (copia derivada, v2, lista para pegar)

Este prompt **no** se deriva de `docs/design/stitch/` como los de pantalla: no pide pantallas de la app
para copiar, sino **fotografías** que después se empaquetan como assets offline. El maestro
`docs/design/stitch-prompt.md` sigue siendo la fuente de verdad para las pantallas de UI.

**Qué cambió respecto a la v1:** la v1 pedía "hojas de contactos / cuadrículas" y Stitch devolvía **una
sola imagen compuesta** (y una de las láminas salió como otra pantalla). La v2 pide una **lista de filas**
donde **cada fila lleva su propia foto como elemento independiente**, que es lo que hace que Stitch
genere **una imagen separada por plato** (como en la pantalla de Favoritos, donde salen 6 imágenes
distintas a ~1408x768).

**Cómo usarlo:** abre una conversación **nueva** en Stitch, pega la **PARTE 0**, espera a que responda
"OK", pega la **PARTE 1** y exporta (párate ahí: se verifica antes de seguir). Después pega la **PARTE 2**
y exporta. Guarda cada export en `docs/design/concepts/recipe-photos/<lámina>/` (el `.png` + el `.html` +
el `.md`).

---

## PARTE 0 — pégalo primero

```
Design a native Android mobile app screen (Material 3, portrait, phone frame 390x844) for the
"Sazón" recipe app. The screen is simply a VERTICAL LIST of recipe rows. Each row shows ONE dish
photograph and, under/next to it, a short caption with the exact KEY I give you.

CRITICAL — how the photographs must be produced:
- Each row must contain ITS OWN dish photograph as a SEPARATE image element.
- Do NOT create a contact sheet, a grid graphic, a collage or any single combined "master" image.
  I need N independent photographs, one per row.
- Do not add app chrome beyond the list (no bottom bar, no extra screens, no marketing copy).

Photo style (identical for every photo, so the set looks like ONE photo shoot):
- Warm, appetizing "modern cookbook" food photography.
- ONE plated dish per photo, centred and fully visible; top-down or 45-degree close-up.
- Warm natural light, shallow depth of field; rustic wood or ceramic surface; warm cream/terracotta.
- Landscape orientation (wider than tall), roughly 16:10 or 16:9.
- Crisp and high resolution (no blur); no need for extreme detail.
- No text, watermark or logo inside the photo; no hands, no faces, no people.
- Same lighting, same background family, same colour grade in all of them.

For each row, use the exact KEY I give you as that row's short caption.

Reply "OK" and I'll send the dishes in two batches.
```

---

## PARTE 1 — lote A (12 recetas semilla). Pégalo tras el "OK" y exporta.

```
Batch A: one vertical list of 12 rows, each row = its own dish photograph + the KEY as its caption.
1.  seed-tortilla-patatas     -> Spanish potato tortilla, a golden wedge on a plate.
2.  seed-gazpacho-andaluz     -> chilled Andalusian tomato soup in a bowl.
3.  seed-pasta-tomate         -> pasta with tomato sauce and oregano.
4.  seed-pollo-al-ajillo      -> chicken pieces in glossy garlic sauce, clay dish.
5.  seed-lentejas-estofadas   -> Spanish brown lentil stew in a terracotta pot.
6.  seed-salmon-a-la-plancha  -> grilled salmon fillet with lemon.
7.  seed-tacos-de-pollo       -> two chicken tacos, Mexican style.
8.  seed-ensalada-de-quinoa   -> quinoa salad bowl with vegetables.
9.  seed-tofu-salteado        -> stir-fried tofu with vegetables, Asian style.
10. seed-crema-de-calabaza    -> pumpkin cream soup in a bowl.
11. seed-tortitas-de-avena    -> stacked oat-and-banana pancakes with syrup.
12. seed-tarta-de-manzana     -> home-baked apple tart, one slice cut.
Exactly 12 rows, one independent photo each.
```

---

## PARTE 2 — lote B (18 categorías + 1 por defecto). Pégalo después y exporta.

```
Batch B: one vertical list of 19 rows, each row = its own dish photograph + the KEY as its caption.
1.  cat-pasta           -> a generic pasta dish.
2.  cat-rice            -> a rice dish (Spanish style).
3.  cat-soup_cream      -> a creamy soup.
4.  cat-salad           -> a fresh green salad bowl.
5.  cat-meat            -> a cooked meat dish (steak or chop).
6.  cat-poultry         -> roasted/grilled chicken.
7.  cat-fish_seafood    -> a fish or seafood dish.
8.  cat-vegetarian      -> a vegetable dish with no meat or fish.
9.  cat-vegan           -> a fully plant-based bowl.
10. cat-legume_stew     -> a legume stew in a pot.
11. cat-pizza_bread     -> a pizza slice or rustic bread.
12. cat-egg_breakfast   -> a breakfast egg dish.
13. cat-sandwich_wrap   -> a sandwich or wrap.
14. cat-tapas_appetizer -> a tapas / appetizer platter.
15. cat-dessert         -> a dessert.
16. cat-smoothie_drink  -> a smoothie or drink in a glass.
17. cat-casserole_oven  -> an oven casserole in a dish.
18. cat-other           -> a mixed assorted plate (the "other" category).
19. default             -> a NEUTRAL generic placeholder: a single plain plated dish on a neutral
                           surface, very general and non-specific, gently desaturated (almost
                           black-and-white), so it reads as "any dish" and never looks broken.
Exactly 19 rows, one independent photo each.
```
