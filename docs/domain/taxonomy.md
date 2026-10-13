# Taxonomía del dominio

Artefacto de discovery del `build-brief`. Los **valores canónicos** viven en
[`taxonomy.json`](./taxonomy.json); este documento explica el modelo, las reglas y cómo lo
consume cada parte de la app. No dupliques listas aquí: si cambia un valor, cambia solo el JSON.

- Versión: `0.1.0` · Estado: `baselined` · Locale: `es-ES`
- Inventario actual: 5 patrones de dieta · 11 restricciones duras · 7 preferencias blandas ·
  5 tipos de comida · 10 cocinas · 95 ingredientes · 18 categorías de receta · 8 colores de avatar

---

## 1. Regla de oro: una sola fuente de verdad

La `Taxonomy` es el único catálogo de opciones del producto. La consumen tres sitios que **nunca
deben divergir**:

1. **UI**: onboarding, generador, búsqueda con IA, filtros, perfil.
2. **Constructor de prompt** (ids → labels + hints) para la API de IA.
3. **Validador** de la respuesta de la IA.

La IA **nunca** devuelve texto libre para nada clasificable: devuelve **ids de esta taxonomía**,
validados contra un esquema. Así el icono, la imagen y los filtros se resuelven localmente, sin red.

Consecuencia de arquitectura: onboarding, filtros y prompt son **el mismo modelo de datos leído de
sitios distintos**. No hay base de datos para el catálogo; solo para el estado del usuario
(preferencias, favoritos, caché).

---

## 2. Las tres capas de decisión

Mezclar estas capas es lo que degrada el filtrado. Son distintas por semántica y por consecuencia.

| Capa | Cardinalidad | Efecto | Quién la aplica |
|---|---|---|---|
| `dietPatterns` | single-select | Restricción dura | Prompt + validador |
| `hardRestrictions` | multi-select | Restricción **de seguridad** | Prompt + validador (obligatorio) |
| `softPreferences` | multi-select | Solo ranking | Prompt (sugerencia), nunca descarta |

Reglas:

- **`dietPatterns`** excluye por `excludedTags` de ingrediente (`meat`, `poultry`, `fish`,
  `seafood`, `dairy`, `egg`, `honey`, `gelatin`). `flexitarian` no excluye nada: es una instrucción
  de priorización (`promptHint`).
- **`hardRestrictions`** son alérgenos e intolerancias. Nunca son un tema de gusto: si el usuario
  marca `gluten` y la IA devuelve pasta, la receta **se descarta**. Cada restricción tiene
  `keywords` para el validador.
- **`softPreferences`** incluye `real_fooding`, `keto`, `low_calorie`, `economical`, etc. No son
  seguridad, son calidad percibida. `real_fooding` va aquí, no en `hardRestrictions`.

---

## 3. Responsabilidad de cada colección

| Colección | Campos propios | Notas |
|---|---|---|
| `dietPatterns` | `excludedTags`, `promptHint` | `vegan ⊃ vegetarian ⊃ pescatarian` |
| `hardRestrictions` | `keywords`, `icon` | `keywords` = detección sobre texto libre |
| `softPreferences` | `promptHint`, `icon` | Se inyectan como instrucciones al modelo |
| `mealTypes` | `icon` | También usado por el generador (paso 2) |
| `cuisines` | `icon` | Multi-select, suma al prompt |
| `timeBuckets` | `maxMinutes` (o `null`) | `null` = sin límite máximo |
| `difficulties` | `level` (1..3) | Ordenable |
| `servings` | `min`, `max`, `default` | Rango del stepper y de "comensales habituales" |
| `avatarColors` | `hex`, `label` | Identidad, **no** preferencia: no entra en la firma |
| `ingredients` | `category`, `icon`, `allergens`, `tags` | Semilla extensible, no lista cerrada |
| `recipeCategories` | `image` | Resolvía la imagen local de cada receta; **retirado por ADR 0006** (la imagen es tipográfica y la foto es capa de fondo). El campo y sus assets los limpia `retire-recipe-photos`. Ver §6 |

### Invariantes (verificadas por test)

- `ingredient.allergens[]` ⊆ `hardRestrictions.id`.
- `ingredient.tags[]` ⊆ unión de `dietPatterns[].excludedTags`.
- Todo `ingredient.icon` tiene asset local o cae a `categoryFallbackIcon[category]`.
- ~~Todo `recipeCategories[].id` tiene imagen local; si falta, se usa `other`.~~ **Retirado (ADR 0006):**
  deja de aplicar en cuanto `retire-recipe-photos` elimine `app/src/main/assets/img/recipes/` y el campo
  `recipeCategories[].image`. Mientras esa feature no aterrice, el código vigente sigue cumpliéndolo.
- Todo `avatarColors[].hex` es un color válido y con contraste suficiente para iniciales en blanco.
- `id` estables y únicos, en `snake_case`, sin acentos. Los `label` sí llevan acentos.

---

## 4. Contrato con la IA

El constructor de prompt traduce ids a labels/hints y exige **salida estructurada**. Esquema
borrador de la respuesta (todo lo enumerado debe ser un id de la taxonomía):

```json
{
  "recipes": [
    {
      "title": "string",
      "description": "string (1 línea)",
      "categoryId": "recipeCategories.id",
      "mealTypeId": "mealTypes.id",
      "cuisineId": "cuisines.id",
      "totalMinutes": 25,
      "prepMinutes": 10,
      "cookMinutes": 15,
      "difficultyId": "difficulties.id",
      "servings": 2,
      "matchScore": 92,
      "ingredients": [
        { "name": "string", "quantity": 200, "unit": "g", "isPantry": false }
      ],
      "steps": [
        { "order": 1, "title": "string", "description": "string", "minutes": 5 }
      ],
      "tips": ["string"],
      "nutrition": { "kcal": 420, "protein": 25, "carbs": 30, "fat": 18 },
      "declaredAllergens": ["hardRestrictions.id"]
    }
  ]
}
```

`declaredAllergens` es la **autodeclaración** del modelo: sirve como pista, nunca como garantía.
El validador la **recalcula** y compara.

Un ingrediente puede venir **"al gusto"**: `"quantity"` nula o **ausente** (y, con ella, la `unit`
irrelevante). El dominio exige `quantity: Double` no anulable, así que el adaptador
(`com.sazon.app.data.ai`) lo **normaliza** al mapear a `RecipeIngredient`: `quantity = 0.0` y
`unit = null` (la `unit` del wire se **descarta**), que es lo que la app pinta como "Al gusto" y no
escala. Un ingrediente así **no** descarta la receta entera; el Validador sigue aplicando sus reglas
con normalidad.

`id`, `source` y `matchScore` **no** forman parte del contrato: los posee la app. **El modelo no debe
enviarlos** (si los enviara, se ignoran). El adaptador (`com.sazon.app.data.ai`) completa `id`
(determinista por contenido, `ai-<sha256>`) y `source = ai` al decodificar la respuesta y **antes** de
aplicar el Validador; `matchScore` lo deriva la app en memoria y no se persiste.

**Descriptor visual opcional (fase 5).** El modelo **puede** añadir a cada receta dos campos
**opcionales** (si no los envía, la app usa el título y la taxonomía como hasta ahora):

- `dishKey`: el **plato canónico** (plato terminado, p. ej. `"arroz-con-pollo"`), **breve y específico**
  —nunca un ingrediente ni la categoría—. La app lo usa como **clave de caché compartida** (misma
  normalización tipográfica que el título): dos variantes del mismo plato (`"arroz con pollo"` y
  `"arroz con pollo fácil"`) comparten imagen. Un `dishKey` demasiado grueso (`"pollo"`) hace que platos
  distintos compartan imagen, así que la especificidad importa.
- `imagePrompt`: una **descripción visual breve y de una línea** del plato terminado —ingredientes
  principales y aspecto (color, textura, guarnición)—, **sin texto ni manos ni personas**. La app lo usa
  como **detalle** del prompt de imagen: el **título de la receta es siempre la identidad** del prompt y
  este campo se añade después, junto a los ingredientes clave, la composición y los negativos. **No**
  sustituye al título (cambio de `dish-image-prompt-v2` respecto a la fase 5), de modo que el prompt
  sigue siendo reconocible aunque el modelo describa el plato con otras palabras.

La app **normaliza** estos campos (colapsa espacios y descarta los inútiles) y **no los valida**: son
información **cosmética del modelo**, **no** forman parte de `Recipe`, **no** se persisten y **no**
entran en el `id` de la receta; un tipo JSON equivocado, un valor en blanco o un `dishKey` no
canonicalizable se **ignora** sin descartar la receta (el Validador no cambia). Ver
`docs/specs/dish-visual-descriptor.md` y `docs/adr/0005-ai-dish-images.md`.

---

## 5. Validación post-IA (capa obligatoria)

Sin esta capa la IA puede devolver gluten a un celíaco. Reglas del validador, en este orden:

1. **Esquema**: la respuesta parsea y todos los `*Id` existen en la taxonomía.
2. **Restricción de dieta**: ningún ingrediente tiene un `tag` en `dietPatterns[].excludedTags`.
3. **Alérgenos duros**: para cada restricción marcada, se cruza cada ingrediente contra
   `hardRestrictions[].keywords` (texto normalizado, sin acentos, en minúsculas). Si hay match →
   receta descartada.
4. **Coherencia**: `sum(steps.minutes) ≈ totalMinutes`; `declaredAllergens` no omite un alérgeno
   detectado en el paso 3.
5. **Ranking**: las `softPreferences` solo reordenan, no descartan.

La lógica de 1–4 es pura (sin red) → es unit-testeable y es donde vive la calidad del producto.

---

## 6. Estrategia visual: la IA decide *qué*, la app decide *cómo*

La IA de texto **no devuelve imágenes**. Resolución determinista:

- **Iconos de ingredientes**: `assets/icons/ingredients/{icon}.svg`, con
  `_fallback` por categoría. Set local, offline, licencia abierta.
- **Iconos de UI** (nav, acciones, chips): Material Symbols Rounded.
- **Fotos de receta de la cadena local (retiradas, ADR 0006)**: los assets versionados en
  `app/src/main/assets/img/recipes/` con la raíz `img/recipes`: foto propia por receta semilla
  (`seed/<recipeId>.<ext>`, 12), foto por categoría (`category/cat-<categoryId>.<ext>`, 18 — una por
  `recipeCategories`) y una genérica por defecto (`default.<ext>`, 1); `<ext>` es `webp`/`jpg`/`png`; y la
  cadena de respaldo (semilla → categoría → `default` → glifo) en `data.RecipePhotoAssets` **quedan
  retirados**; se **conserva** `default.<ext>` (~136 KB) como ancla de estilo de `dish-style-reference`
  (ADR 0005, fase 6), que no se muestra en ninguna pantalla. La IA solo devuelve `categoryId` (no imágenes), así que las recetas `ai-*` caían en la foto
  de su categoría, que **no corresponde al plato**: es el síntoma de `dish-image-prompt-v2`. Con
  `docs/adr/0006-typographic-recipe-cards.md` la **imagen base de una receta es una ficha tipográfica** y,
  cuando existe imagen, es **capa de fondo** del hueco. Ver
  `docs/adr/0004-recipe-photos-as-assets.md` (ajustado).
- **Avatares**: iniciales sobre un color de `avatarColors`. Sin assets: el avatar se compone en
  tiempo de ejecución.
- **Imagen de plato generada (real desde `dish-image-progressive-ui`, fase 4; capa de fondo, ADR 0006)**:
  la app pide al proveedor la imagen del plato (`data.ai.image`), la resuelve por `data.image` (memoria →
  disco → **assets semilla** → generador real, con la clave canónica `dish-image-v2-…` del nombre del
  plato —o del `dishKey` del modelo cuando existe, descriptor de la fase 5— y el prompt v2 de
  `domain.image.DishImagePrompt`: título como identidad + detalle del descriptor + ingredientes clave +
  composición + negativos) y `ui.artwork.RecipeArtwork`, que pasa a ser el **proveedor de la capa de
  fondo** del hueco, la pinta **por detrás de la tipografía** con un crossfade de 250 ms, sin cambiar la
  firma, la geometría ni los 5 puntos de uso. Solo hay imagen si el ajuste "Generar imágenes del plato"
  está encendido y el resolver devuelve una; apagado = `null` **sin** leer caché, sin red y sin cuota.
  En depuración, el **origen resuelto** (`GENERATED`/`SEED`/`GLYPH` tras la retirada de la cadena local,
  que ya no puede dar `CATEGORY`) es observable en logcat (`SazonDishImage`) sin alterar el pintado; en
  release no se emite nada. Los **platos frecuentes del demo** tienen desde la fase 7 (`dish-image-seed`)
  su foto **preparada de antemano y versionada**
  (`app/src/main/assets/img/dishes/seed/<clave canónica>.<ext>`, ya `dish-image-v2-…`,
  `data.DishImageSeedAssets` + `data.AssetDishImageSeedSource`), de modo que se resuelven **sin red y
  sin gastar cuota** (ni la primera vez) y **sin** duplicar bytes en `cacheDir`; ese nivel va entre el
  disco y el generador y queda detrás del mismo interruptor. Si no está disponible (sin red, fallo,
  timeout o cancelación) se **conserva la ficha tipográfica base** (con el glifo de último recurso), así
  que **nunca** hay error de UI ni hueco y la app sigue funcionando offline: la generación **no** es
  dependencia del MVP. Ver `docs/adr/0005-ai-dish-images.md` y `docs/specs/dish-image-seed.md`.
- **Glifo y acento por cocina (vivos, ADR 0006)**: siguen siendo la identidad visual de reserva. El
  **glifo** por categoría (`RecipeGlyph`, los 18 `recipeCategories`) es la **marca de agua** de la ficha
  tipográfica; el **acento** por cocina (`RecipeAccent`, 10 cocinas + `DEFAULT`) es el **filete de 6px**
  de la tarjeta y, si no hay imagen, el degradado + glifo sigue siendo el **último recurso**.

---

## 7. Versionado y extensión

- Añadir una opción = añadir un item. No rompe nada.
- **Nunca** renombrar ni borrar un `id`: rompe preferencias guardadas y cachés. Se deprecia con
  alias.
- Cambiar un `label` es seguro (solo UI y prompt).
- Política de **trazas** (ej. `avena`): por defecto conservadora — un ingrediente con posible traza
  se marca con el alérgeno. La decisión final vive en `docs/risks-and-open-questions.md`.
- Ingrediente libre del usuario: se guarda como `{name}` sin id; el validador lo resuelve por
  `keywords`, nunca por la lista `ingredients`.

---

## 8. Cuestiones abiertas

Las incógnitas derivadas de esta taxonomía (idioma y nº de recetas, unidades, nutrición, política de
trazas, licencias, `matchScore`) están centralizadas en `docs/risks-and-open-questions.md`. No las
dupliques aquí: si se resuelven, se resuelven allí.
