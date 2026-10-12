# Domain Model

Catálogo de valores: `docs/domain/taxonomy.json` (fuente de verdad) y `docs/domain/taxonomy.md`
(modelo y reglas). Este documento no repite listas: describe conceptos, relaciones y estados.

## Core Concepts

### Taxonomía (Taxonomy)
Catálogo **estático** de opciones, en código. Colecciones: `dietPatterns` (única),
`hardRestrictions` (múltiple, seguridad), `softPreferences` (múltiple, ranking), `mealTypes`,
`cuisines`, `timeBuckets`, `difficulties`, `servings`, `avatarColors`, `ingredients` y
`recipeCategories`.
Toda opción tiene `id` estable (inglés, `snake_case`, sin acentos) y `label` (español, con acentos).

### Perfil de usuario (UserProfile)
Identidad del Usuario local, capturada en el onboarding y editable en la pantalla Perfil:
`displayName` (**obligatorio**: 1–40 caracteres tras recortar espacios), `avatarColorId` (de
`avatarColors`) y `createdAt`.
**No** forma parte de la Firma de preferencias.

### Perfil de preferencias (PreferenceProfile)
Salida del onboarding y editable en la pantalla Perfil. Contiene:
`dietPatternId` (uno), `hardRestrictionIds[]`, `softPreferenceIds[]`, `cuisineIds[]`,
`preferredIngredientIds[]`, `dislikedIngredientIds[]`, `difficultyId`, `timeBucketId`,
`defaultServings` y `updatedAt`.

`dislikedIngredientIds` es una **preferencia blanda**, no una restricción: influye en el prompt y en
el ranking, pero no descarta recetas.

### Firma de preferencias (preferenceSignature)
Hash determinista del **Perfil de preferencias**. Se guarda con cada Lote de sugerencias generado por
IA. Es el mecanismo para decidir si un lote es obsoleto, sin adivinar.

- **Incluye**: patrón de dieta, restricciones duras, preferencias blandas, cocinas, ingredientes
  preferidos, ingredientes que evito, dificultad, tiempo disponible y comensales habituales.
- **Excluye**: nombre y color de avatar. Cambiar de identidad no invalida recetas.
- Trata las listas como **conjuntos**: reordenar la misma selección (o duplicarla) no altera la firma.
- Los sellos temporales (`createdAt`, `updatedAt`) tampoco son una preferencia y quedan excluidos.

### Resumen de actividad (ActivitySummary)
Datos que la pantalla Perfil muestra al usuario:

- `recipesGeneratedCount`: **contador persistido**, se incrementa con cada receta `ai` aceptada por el
  Validador (incluidas las de la Búsqueda con IA). No puede derivarse: al refrescarse el Lote de
  sugerencias se reemplazan recetas `ai` y el número bajaría.
- `favoritesCount`: **derivado** por consulta sobre Favoritos.

### Receta (Recipe)
Toda receta, sea `seed` o `ai`, cumple el mismo esquema:
`id`, `source` (`seed` | `ai`), `title`, `description`, `categoryId`, `mealTypeId`, `cuisineId`,
`totalMinutes`, `prepMinutes`, `cookMinutes`, `difficultyId`, `servings`, `ingredients[]`, `steps[]`,
`tips[]`, `nutrition` (opcional), `declaredAllergens[]` y `matchScore` (opcional; lo **calcula la app**,
derivado y **no persistido**, con la formula en `com.sazon.app.ui.results.MatchScore`, ver
`docs/risks-and-open-questions.md`).

- **RecipeIngredient**: `name`, `quantity`, `unit`, `isPantry`, `ingredientId` (opcional; `null`
  cuando es ingrediente libre).
- **RecipeStep**: `order`, `title`, `description`, `minutes`.
- **Nutrition**: `kcal`, `protein`, `carbs`, `fat` (estimados).
- **id**: lo aporta el paquete de recetas semilla; en las recetas `ai` lo asigna la app **al
  decodificar la respuesta del proveedor** (en `com.sazon.app.data.ai`, al completar el wire del
  contrato antes del Validador), con `source = ai`. El `id` es **determinista por contenido**
  (`ai-<sha256>` de los campos de la receta): regenerar el mismo contenido conserva el `id`, de modo
  que un Favorito sigue resolviendo aunque el Lote de sugerencias se refresque. Un duplicado exacto
  dentro de una misma respuesta se desambigua con `-2`, `-3`, …
- **Persistencia**: una receta `ai` en caché guarda `generatedAt` y `lastOpenedAt`. `lastOpenedAt`
  alimenta el grupo "Abiertas hace poco" del bloque "Tus recetas".

### Lote de sugerencias (SuggestionBatch)
Conjunto persistido que alimenta Home:
`preferenceSignature`, `generatedAt` y sus recetas. El lote **persiste sus recetas** (`seed` + `ai`),
no solo `recipeIds[]`, para que la lectura sea autocontenida offline.
Contiene recetas `seed` siempre, y recetas `ai` solo si se han podido generar y han pasado el
Validador. El `status` (`fresh` | `stale`) es **derivado** por la política de frescura
(`SuggestionBatchPolicy.decide`), no una columna persistida: una firma distinta o la caducidad
(**7 días**, `BATCH_TTL_MILLIS`) lo marcan `stale`.

### Favorito (Favorite)
Marcador del usuario sobre una Receta. Persiste `recipeId` + una copia de la Receta (para que siga
disponible si el origen era `ai`). Es independiente del Lote de sugerencias.

### Generador de recetas (RecipeGenerator)
Puerto con tres operaciones:
- `suggestByPreferences(profile)` → recetas sin ingredientes declarados; alimenta el Lote.
- `searchByText(query, profile)` → **Búsqueda con IA** escrita en la Home.
- `generateFromIngredients(ingredients, constraints)` → recetas para el flujo principal.

Las tres devuelven `Recipe` y **todas** pasan por el Validador. `constraints` es
`GenerationConstraints` (`profile`, `servings` y los ajustes opcionales del paso del generador).
Implementación real: `DevExpertRecipeGenerator` (en `data.ai`, Ktor + DevExpert Inference;
`docs/specs/real-ai-provider.md`). Implementación de test: `FakeRecipeGenerator` (en `data.generator`,
determinista y sin red).

### Política de la Búsqueda con IA
- **Restricciones duras y Patrón de dieta se aplican siempre**, las garantiza el Validador. No son
  negociables aunque el texto escrito las pida.
- Preferencias blandas, cocinas, tiempo y dificultad **solo ordenan**; nunca filtran.
- **El texto manda sobre lo blando**: si pides "italiano" y tu perfil prefiere "mexicana", gana el
  texto.
- **No se persiste un caché consulta→resultado.** La búsqueda es una acción explícita del usuario, así
  que se acepta su latencia; solo se memoiza en memoria durante la sesión. Las recetas devueltas sí
  entran en la caché normal de recetas `ai`.
- **Implementación (MVP)**: la orquestación (`searchByText` → Validador) vive en
  `com.sazon.app.domain.search.AiSearchService`, con el memo de sesión en `SearchMemo` (en memoria, no
  persistido: se pierde al cerrar la app; un fallo no se memoiza). El estado de la página (resultados /
  bloqueado por Perfil / sin resultados / fallo) se decide en
  `com.sazon.app.ui.search.AiSearchOutcome` por el motivo del descarte del Validador.

### Validador de recetas (RecipeValidator)
Comprueba el esquema, los `*Id` contra la Taxonomía, el Patrón de dieta y las Restricciones duras.
Devuelve `accepted` o `discarded(reason)`. Nunca muestra una receta descartada.

## Relationships

- `UserProfile` **contiene** la identidad (nombre, `avatarColorId`) y un `PreferenceProfile`.
- `UserProfile` **acumula** el `ActivitySummary` (contador persistido + favoritos derivados).
- `PreferenceProfile` **selecciona** items de la `Taxonomy` (1..n por colección).
- `PreferenceProfile` **produce** una `preferenceSignature`.
- `SuggestionBatch` **contiene** 0..n `Recipe` y **referencia** una `preferenceSignature`.
- `Recipe` **pertenece a** una `recipeCategory` y **tiene** 1..n `RecipeIngredient` y 1..n `RecipeStep`.
- `RecipeIngredient` **referencia** 0..1 `Ingredient` de la Taxonomía (o es texto libre).
- `Favorite` **referencia** 1 `Recipe`.
- `RecipeGenerator` **produce** `Recipe`; `RecipeValidator` **filtra** `Recipe`.

## States and Lifecycles

### Receta
`generada en memoria` → (si se muestra u opera) → `en caché` → `favorita` → `no favorita`.
Las recetas `ai` no favoritas pueden descartarse al refrescar el lote; las `seed` nunca.

### Lote de sugerencias
`inexistente` → `solicitado a IA` → `fresh` → `stale` (firma cambiada o caducidad) → `refresco`.
Si la IA no está disponible, el lote cae a **solo semilla** y Home sigue siendo usable.

### Disponibilidad de IA
`ai_available` | `ai_unavailable` (sin clave, sin red, error del proveedor o cuota). No es un estado
de error de la app: es un modo degradado esperado.

### Resultado de validación
`accepted` | `discarded(schema | diet_pattern | hard_restriction | incoherent)`. Se registra el
motivo para poder diagnosticar fallos del modelo.

### Onboarding
`not_started` → `in_progress` → `completed`. Es **obligatorio**: no existe transición a un estado
"omitido" en el MVP. Captura en el paso 1 la identidad y en los pasos 2–7 el Perfil de preferencias.

## Important Scenarios

1. **Primer arranque**: sin perfil → onboarding → se calcula la firma → Home muestra la semilla de
   inmediato → el lote `ai` se solicita en segundo plano y se persiste.
2. **Generar con ingredientes**: ingredientes + ajustes → `RecipeGenerator` → `RecipeValidator` →
   lista de resultados.
3. **Restricción dura incumplida**: la IA devuelve una receta con un alérgeno marcado → se descarta
   y no aparece en la lista.
4. **Cambio de preferencias**: el usuario edita Perfil → cambia la firma → el lote pasa a `stale` →
   se refresca en la siguiente apertura.
5. **Apertura sin conexión**: Home lee el lote de caché; si es `stale`, se refresca cuando vuelva la
   red, sin bloquear.
6. **Sin clave de IA**: la app funciona en modo semilla; el generador muestra un aviso claro en vez
   de fallar.
7. **Cambio de identidad**: el usuario edita nombre o color de avatar → cambia el `UserProfile` pero
   **no** la firma → el lote sigue `fresh` y no se gasta una llamada de IA.
8. **Refresco manual**: desde la pantalla Perfil el usuario fuerza "Actualizar sugerencias" → se
   solicita un lote `ai` nuevo sin cambiar ninguna preferencia.
9. **Búsqueda con IA**: el usuario escribe una consulta en la Home → `searchByText` → Validador →
   pantalla de Resultados con la consulta como subtítulo. Los resultados se cachean como cualquier
   receta `ai`.

## Edge Cases

- **Nombre vacío**: no puede ocurrir desde el onboarding, que lo exige. Solo si el perfil se corrompe
  o migra: el saludo cae a "Buenas tardes" y el avatar muestra un icono genérico sobre el color
  elegido, nunca iniciales vacías.
- **Nombre muy largo o con emojis**: las iniciales se limitan a dos caracteres y el saludo se trunca
  sin romper el layout.
- **Color de avatar ausente** (perfil migrado o corrupto): se usa el primer color de `avatarColors`.
- **Ingrediente en "me gusta" y en "evito" a la vez**: gana "evito"; la UI debe impedir el solape.
- **Ingrediente libre con alérgeno**: el usuario escribe "pan" y tiene restricción de gluten → el
  validador debe detectarlo por `keywords`, no por la lista de ingredientes.
- **Ids desconocidos**: la IA devuelve un `categoryId` inexistente → se resuelve a `other`, no se
  rompe la UI.
- **Recetas de menos**: la IA devuelve menos recetas de las pedidas → se muestra lo que hay, sin
  error.
- **Búsqueda que choca con una restricción dura**: una persona vegana busca "carne" → no se genera
  nada. Se muestra un estado vacío que explica que el Perfil lo impide y enlaza a editarlo. La
  seguridad nunca se negocia por texto libre.
- **Búsqueda con IA sin resultados**: estado vacío de Resultados con la consulta visible y un atajo a
  "Crear receta".
- **Búsqueda con IA sin red**: se conserva la consulta escrita y se ofrece reintentar; el resto de la
  Home sigue funcionando con datos locales.
- **Duplicados**: la misma receta en semilla y en el lote `ai` → se deduplica por título normalizado.
- **Escalado de raciones**: no es lineal (especias, sal) → se redondea por unidad y se avisa. La lógica
  vive en `com.sazon.app.ui.detail.ServingScaler` (`g`/`ml` a múltiplos de 5, `ud`/`cda`/`cdta` a medios,
  `unit = null` "al gusto" no se escala; a las raciones originales las cantidades quedan intactas).
- **Favorito eliminado que sigue en el lote**: el lote no cambia; el corazón se desmarca.
- **Onboarding interrumpido**: al reabrir, se reanuda en el paso pendiente; no se completa a medias.
- **Cambio de preferencias durante una generación**: el resultado en vuelo se valida contra la firma
  con la que se pidió, no contra la nueva.
