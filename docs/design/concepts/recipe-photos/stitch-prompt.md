# Prompt de Stitch — `recipe-photos` (copia derivada, lista para pegar)

Este prompt **no** se deriva de `docs/design/stitch/` como los de pantalla: no pide pantallas de la app,
sino **láminas de fotos de plato** que después se empaquetan como assets offline. El maestro
`docs/design/stitch-prompt.md` sigue siendo la fuente de verdad para las pantallas de UI.

**Cómo usarlo:** abre una conversación **nueva** en Stitch, pega la **PARTE 0**, espera a que responda
"OK", pega la **PARTE 1** y exporta; luego pega la **PARTE 2** en la misma conversación y exporta.
Guarda cada export en `docs/design/concepts/recipe-photos/<lámina>/` (el `.png` + el `.html` + el `.md`).

---

## PARTE 0 — pégalo primero

```
You are a food-photography art director. I need ASSET SHEETS: clean grids of dish
photographs that I will crop out and use as recipe images in a mobile app.

This is NOT a phone UI screen: do NOT draw any app chrome, phone frame, buttons,
navigation, headers or captions. Just photographs in a grid.

STYLE (identical for every photo, so the set looks like ONE photo shoot):
- Warm, appetizing "modern cookbook" food photography.
- Top-down or 45-degree close-up; ONE plated dish per photo, centred and fully visible.
- Warm natural light, shallow depth of field, rustic wood or ceramic surfaces,
  warm cream/terracotta tones.
- Landscape orientation (wider than tall), roughly 16:10 or 3:2.
- Crisp and high resolution (no blur), but no need for extreme detail.
- No text inside the photo, no watermark, no logo, no hands, no faces, no people.
- Same lighting, same background family, same colour grade in all of them.

LAYOUT: a clean grid of equal tiles (3 columns), a thin margin between tiles so each
photo is easy to crop. Under each tile, in small plain text, print the exact KEY I
give you. Only the photographs + the key label per tile.

Reply "OK" and I will send the dish lists in two batches.
```

---

## PARTE 1 — lote A (12 recetas semilla). Pégalo tras el "OK" y exporta.

```
Batch A: 12 photos, one per dish. Under each tile print the KEY exactly.
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
Only these 12 photos.
```

---

## PARTE 2 — lote B (18 categorías + 1 por defecto). Pégalo después y exporta.

```
Batch B: 19 photos: one per CATEGORY (a generic representative dish of each) plus a
DEFAULT image. Under each tile print the KEY exactly.
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
19. default             -> a NEUTRAL generic placeholder: a single plain plated dish on a
                           neutral surface, very general and non-specific, gently
                           desaturated (almost black-and-white), so it reads as "any dish"
                           and never looks like a broken image.
Only these 19 photos.
```
