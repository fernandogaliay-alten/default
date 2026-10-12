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
| `recipeCategories` | `image` | Resuelve la imagen local de cada receta |

### Invariantes (verificadas por test)

- `ingredient.allergens[]` ⊆ `hardRestrictions.id`.
- `ingredient.tags[]` ⊆ unión de `dietPatterns[].excludedTags`.
- Todo `ingredient.icon` tiene asset local o cae a `categoryFallbackIcon[category]`.
- Todo `recipeCategories[].id` tiene imagen local; si falta, se usa `other`.
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

`id`, `source` y `matchScore` **no** forman parte del contrato: los posee la app. **El modelo no debe
enviarlos** (si los enviara, se ignoran). El adaptador (`com.sazon.app.data.ai`) completa `id`
(determinista por contenido, `ai-<sha256>`) y `source = ai` al decodificar la respuesta y **antes** de
aplicar el Validador; `matchScore` lo deriva la app en memoria y no se persiste.

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
- **Fotos de receta** (real desde `recipe-photos`, issue #83): assets versionados en
  `app/src/main/assets/img/recipes/` con la raíz `img/recipes`: foto propia por receta semilla
  (`seed/<recipeId>.<ext>`, 12), foto por categoría (`category/cat-<categoryId>.<ext>`, 18 — una por
  `recipeCategories`) y una genérica por defecto (`default.<ext>`, 1); `<ext>` es `webp`/`jpg`/`png`. La
  cadena de respaldo (semilla → categoría → `default` → glifo) vive en `data.RecipePhotoAssets`. La IA
  solo devuelve `categoryId` (no imágenes), así que las recetas `ai-*` usan la foto de su categoría. Ver
  `docs/adr/0004-recipe-photos-as-assets.md`.
- **Avatares**: iniciales sobre un color de `avatarColors`. Sin assets: el avatar se compone en
  tiempo de ejecución.
- **Generación real de imagen (Imagen API)**: *feature opcional/estirar*, nunca dependencia del MVP.

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
