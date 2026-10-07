# Feature Implementation Spec: Catálogo de recetas semilla offline

## Source Feature

- `id`: `seed-recipe-catalog`
- `area`: `data`
- `depends_on`: `["recipe-domain-model"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Hacer que la app tenga un **catálogo local de recetas semilla**, disponible siempre y sin conexión.
Hoy `docs/domain-model.md` define la Receta (`seed` | `ai`) y `docs/technical-discovery.md` ya fija
que las recetas semilla son **JSON en assets**, pero no existe ningún fichero de contenido ni forma
de leerlo: cualquier pantalla offline ("Tus recetas", sugerencias semilla, filtros locales) tendría
que inventarse sus propias recetas en Kotlin.

Esta feature entrega tres cosas: (1) **contenido** — 12 recetas semilla autoradas en un único JSON
empaquetado en `app/src/main/assets/seed-recipes.json`; (2) una **costura de lectura** — un parser
puro en dominio y un lector de asset en `data`, sin Android en el dominio; (3) un **test de
integridad** que comprueba que el catálogo se carga, no está vacío y todas sus recetas cumplen el
esquema y son coherentes con la Taxonomía.

Es contenido + lectura + verificación: **sin UI, sin red, sin persistencia y sin generador**.

## Non-Goals

- **No** se implementa ninguna pantalla ni se muestra el catálogo (Home, "Tus recetas", resultados):
  eso es `home-offline-blocks`, `local-result-filters` y posteriores.
- **No** se usa el catálogo en el Lote de sugerencias ni se mezcla con recetas `ai`
  (`suggestion-batch-cache`).
- **No** se pasa el catálogo por el `RecipeValidator` en ejecución: la seguridad del contenido autorado
  la cubre el test; el Validador es para la salida de la IA (`recipe-validator`, ya `accepted`).
- **No** se calcula `matchScore` (derivado, no persistido; es de `results-screen`): las semillas lo
  dejan `null`.
- **No** se implementa el generador (`fake-recipe-generator` ya existe) ni el prompt (`prompt-builder`).
- **No** se empaquetan iconos ni fotos de receta: la resolución visual (`assets/img/recipes/...`) es de
  las pantallas.
- **No** se modela ni se implementa una base de datos de recetas `seed`: son contenido inmutable del APK.

## Job Story

Cuando construyo la Home offline, los filtros locales y el modo semilla (sin proveedor de IA),
quiero un conjunto de recetas `seed` tipadas, cargado desde un asset y verificado por test,
para poblar la app sin red, sin inventarme recetas en Kotlin y sin que un typo en el contenido pase
desapercibido hasta la UI.

## Users And Permissions

- **Desarrollador / agente**: puede cargar el catálogo desde assets y verificar su integridad en JVM,
  sin red, sin permisos y sin emulador.
- **Usuario local**: no interactúa todavía con esta feature; se beneficia de que la app tenga contenido
  local cuando lleguen las pantallas.

## Acceptance Scenarios

### Scenario 1: el catálogo se carga y no está vacío

Given el asset `app/src/main/assets/seed-recipes.json` empaquetado,
When `SeedRecipeJson.parse` lo decodifica,
Then devuelve un `SeedRecipeCatalog` con **al menos 12** recetas, todas con `source = seed`,
`matchScore = null`, `id` único y con prefijo `seed-`.

### Scenario 2: el asset se carga desde Android con el nombre correcto

Given el contexto de la aplicación,
When se ejecuta `AssetSeedRecipeSource(context).load()`,
Then devuelve el mismo catálogo (≥12 recetas) sin excepción, lo que confirma que el fichero existe en
`assets/`, se llama `seed-recipes.json` y está empaquetado (no ignorado en git).

### Scenario 3: todas las recetas cumplen el esquema de Receta

Given cada receta del catálogo,
When se serializa con `RecipeJson.encode` y se vuelve a parsear con `RecipeJson.parse`,
Then la receta recuperada es **igual** a la original (esquema `Recipe` válido; los campos opcionales
`nutrition`, `matchScore` e `ingredientId` pueden ser `null` sin romper).

### Scenario 4: integridad referencial contra la Taxonomía

Given el catálogo y la Taxonomía reales,
When se resuelven los `*Id` de cada receta,
Then `categoryId`, `mealTypeId`, `cuisineId` y `difficultyId` existen en la Taxonomía; cada
`ingredientId` no nulo existe como ingrediente; `declaredAllergens` ⊆ `hardRestrictions.id`; y
`servings` está dentro de `1..12` (rango de la Taxonomía).

### Scenario 5: coherencia de contenido y alérgenos declarados

Given cada receta del catálogo,
When se comprueba su contenido,
Then `ingredients` y `steps` no están vacíos, los `steps` van de `1..n` con `minutes ≥ 0`
(`abs(sum(steps.minutes) - totalMinutes) ≤ 5`), y `declaredAllergens` incluye **todos** los alérgenos
de los ingredientes resueltos (p. ej. un ingrediente `pasta` exige `gluten` declarado).

### Scenario 6: un catálogo roto es rechazado (caso negativo obligatorio)

Given un JSON con una errata deliberada (p. ej. `categoryId = "categoria-inexistente"` o un
`declaredAllergens` que omite `gluten` de un ingrediente `pasta`),
When se ejecuta la comprobación de integridad,
Then devuelve **al menos una violación**, de modo que el test no puede pasar sin comprobar de verdad.

## Repository Research

### Files Inspected

- `feature_list.json` — definición, verificación y nota de `seed-recipe-catalog`; dependencia
  `recipe-domain-model` en `accepted`.
- `PROGRESS.md` — sesiones 015/017: última aceptada `fake-recipe-generator`; gate `./gradlew test` +
  `./gradlew lint` vía `init.sh`.
- `docs/domain-model.md` — esquema `Recipe`/`RecipeIngredient`/`RecipeStep`/`Nutrition` y la nota
  "`id`: lo aporta el paquete de recetas semilla".
- `docs/technical-discovery.md` — fila "Recetas semilla | JSON en assets (`app/src/main/assets/`),
  cargado por la app y validado por test"; patrón Taxonomía (`TaxonomyJson.parse` en dominio,
  `AssetTaxonomySource` en `data`); el dominio no depende de Android para testarse en JVM.
- `docs/domain/taxonomy.md` y `docs/domain/taxonomy.json` — ids canónicos de `recipeCategories`,
  `mealTypes`, `cuisines`, `difficulties`, `servings` (`1..12`), `ingredients` con sus `allergens` y
  `hardRestrictions` (alérgenos; regla de coherencia de la §5).
- `docs/risks-and-open-questions.md` — "Formato de las recetas semilla": resuelto como JSON en assets
  con **12** recetas; riesgo "el catálogo semilla no se autora a tiempo (es contenido, no código)".
- `docs/build-brief.md` — "la semilla local puede ser pequeña (12–20 recetas)".
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` y `RecipeJson.kt` — esquema real y
  serialización (`ignoreUnknownKeys`, `encodeDefaults`).
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` y `TaxonomyJson.kt` — patrón de
  catálogo de dominio y parser puro.
- `app/src/main/java/com/sazon/app/data/AssetTaxonomySource.kt` — patrón de lector de asset.
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` — patrón de test JUnit 4
  con `violations(...)` y **dos casos negativos** con JSON mutado a mano.
- `app/src/test/java/com/sazon/app/data/profile/RoomProfileRepositoryTest.kt` — Robolectric ya está
  configurado (`@Config(sdk = [33])`, `isIncludeAndroidResources = true`).
- `app/build.gradle.kts` — tarea `syncTaxonomy`, dependencias de assets y el bloque
  `tasks.withType<Test>` que pasa `taxonomy.asset.path` como propiedad de sistema.
- `.gitignore` — ignora **solo** `app/src/main/assets/taxonomy.json` (generado); el directorio
  `assets/` no está ignorado.

### Existing Patterns To Follow

- **Costura dominio/datos**: parser puro en `domain.*` (testeable en JVM), lectura del asset en
  `data` (`AssetTaxonomySource` → `AssetSeedRecipeSource`).
- **Asset generado vs asset fuente**: `taxonomy.json` es **generado** (`syncTaxonomy`, ignorado en
  git); el catálogo semilla es **fuente** y se commitea.
- Modelos `@Serializable` `data class`; config `Json { ignoreUnknownKeys = true }` en un `object`.
- Identificadores de código en inglés; texto de contenido en español.
- Tests JUnit 4 en `app/src/test/...`, nombres en español con `_`, helpers `private fun`.
- Verificación única: `./gradlew test` + `./gradlew lint` vía `init.sh`; sin procesos largos.

### Current Gaps

- **No existe `app/src/main/assets/seed-recipes.json`** ni contenido de recetas semilla.
- **No existe ninguna forma de cargar recetas semilla** (no hay parser ni lector de asset).
- **No existe ningún test** que valide el catálogo semilla.
- El asset de la Taxonomía está ignorado en git; **hay que evitar ignorar el de semillas** (son
  naturalezas distintas: uno se genera, el otro se autora).

## Technical Approach

### 1. Dónde vive el contenido y por qué se commitea

El catálogo se autora directamente en `app/src/main/assets/seed-recipes.json` y **se versiona en git**.
A diferencia de `taxonomy.json` (generado desde `docs/domain/taxonomy.json` con `syncTaxonomy` e
ignorado), las recetas semilla **no tienen un gemelo en `docs/`**: la nota de la feature y
`technical-discovery.md` fijan el asset como hogar del contenido. Un `docs/` + tarea `sync` añadiría
una copia que mantener sincronizada sin ninguna fuente documental que la justifique. Como el fichero
está en `src/main/assets/`, las tareas `merge*Assets` lo empaquetan sin tocar el build por ese lado.

`app/build.gradle.kts` solo gana una **propiedad de sistema** para que el test JVM pueda leer el
fichero por ruta absoluta, igual que ya hace con `taxonomy.asset.path`:

```
val seedRecipesAssetFile = layout.projectDirectory.file("src/main/assets/seed-recipes.json")
...
systemProperty("seed.recipes.asset.path", seedRecipesAssetFile.asFile.absolutePath)
```

**No** se añade tarea `sync` ni entrada en `.gitignore`.

### 2. Formato del fichero

Un único objeto con envelope, coherente con el contrato de la IA que consume el Validador:

```json
{
  "recipes": [
    {
      "id": "seed-tortilla-patatas", "source": "seed",
      "title": "Tortilla de patatas", "description": "Tortilla clásica de patata y huevo.",
      "categoryId": "egg_breakfast", "mealTypeId": "dinner", "cuisineId": "spanish",
      "totalMinutes": 40, "prepMinutes": 15, "cookMinutes": 25,
      "difficultyId": "easy", "servings": 4,
      "ingredients": [
        { "name": "Patata", "quantity": 600, "unit": "g", "isPantry": false, "ingredientId": "patata" },
        { "name": "Huevo", "quantity": 6, "unit": "ud", "isPantry": false, "ingredientId": "huevo" },
        { "name": "Sal", "quantity": 1, "unit": null, "isPantry": true, "ingredientId": "sal" }
      ],
      "steps": [
        { "order": 1, "title": "Pelar y cortar", "description": "Corta las patatas.", "minutes": 15 },
        { "order": 2, "title": "Freír", "description": "Fríe a fuego medio.", "minutes": 15 },
        { "order": 3, "title": "Cuajar", "description": "Cuaja con el huevo batido.", "minutes": 10 }
      ],
      "nutrition": { "kcal": 320, "protein": 14, "carbs": 24, "fat": 19 },
      "declaredAllergens": ["egg"]
    }
  ]
}
```

Los campos desconocidos se ignoran (`ignoreUnknownKeys`), de modo que el fichero puede crecer sin
romper el parser. `matchScore` se **omite** (queda `null`).

### 3. Costura dominio / datos

| Capa | Paquete | Responsabilidad | ¿Android? |
|---|---|---|---|
| Modelo + parser | `com.sazon.app.domain.recipe` | `SeedRecipeCatalog` + `SeedRecipeJson.parse(json): SeedRecipeCatalog` | **No** |
| Lectura del asset | `com.sazon.app.data` | `AssetSeedRecipeSource(context).load(): SeedRecipeCatalog` | Sí |

`SeedRecipeCatalog` es un `@Serializable data class(val recipes: List<Recipe>)`; el envelope
`{ "recipes": [...] }` lo decodifica directamente sin clase intermedia. El parser recibe un `String`;
leer bytes es de la capa `data`, igual que en la Taxonomía.

### 4. Reglas de autoría del contenido (las que el test vigila)

- **12 recetas** (`id` `seed-<slug>`, único y estable), `source = "seed"`, `matchScore` omitido.
- Todos los `*Id` y `ingredientId` salen de la Taxonomía; `servings` en `1..12`.
- Ingredientes con `quantity > 0`, `unit` del enum `MeasureUnit` o `null`; productos de despensa
  (sal, aceite, pimienta, agua) con `isPantry = true`.
- `declaredAllergens` cubre los `allergens` de cada ingrediente resuelto (p. ej. `pasta` → `gluten`).
- Coherencia de tiempos: `abs(sum(steps.minutes) - totalMinutes) ≤ 5` (regla §5 del Validador).
- **Diversidad mínima útil offline**: al menos una receta sin etiquetas animales (apta vegana) y al
  menos una con `totalMinutes ≤ 30`, para que Home y los filtros locales tengan material.

### 5. Modo de fallo

Coherente con la Taxonomía: **fallar rápido**. Si el asset falta o el JSON está malformado,
`SeedRecipeJson.parse` lanza `SerializationException`. No es alcanzable en un build correcto: el test
de integridad lo cubre. La carga en ejecución es responsabilidad de los llamantes (pantallas); esta
feature no añade caché ni degradación.

## Expected File Changes

- `app/src/main/assets/seed-recipes.json` — **crear**; 12 recetas semilla autoradas (contenido
  versionado, no generado).
- `app/src/main/java/com/sazon/app/domain/recipe/SeedRecipeCatalog.kt` — **crear**;
  `SeedRecipeCatalog` (`@Serializable`) y `SeedRecipeJson` (parser puro, `ignoreUnknownKeys`), sin
  imports de Android.
- `app/src/main/java/com/sazon/app/data/AssetSeedRecipeSource.kt` — **crear**; lee
  `assets/seed-recipes.json` y delega en el parser (mismo patrón que `AssetTaxonomySource`).
- `app/src/test/java/com/sazon/app/domain/recipe/SeedRecipeCatalogTest.kt` — **crear**; escenarios 1,
  3–6 (parser puro por propiedad de sistema).
- `app/src/test/java/com/sazon/app/data/AssetSeedRecipeSourceTest.kt` — **crear**; escenario 2
  (Robolectric, carga real del asset por nombre).
- `app/build.gradle.kts` — **modificar**; añadir `seedRecipesAssetFile` y la propiedad de sistema
  `seed.recipes.asset.path` en el bloque `tasks.withType<Test>` ya existente.
- `docs/technical-discovery.md` — **modificar**; anotar dónde vive el catálogo semilla, el parser y el
  lector, y que a diferencia de la Taxonomía el JSON es fuente versionada (no generada).
- `PROGRESS.md` y `feature_list.json` — **modificar al implementar**; estado y evidencia (no en esta
  sesión de planificación).

**No se toca** `.gitignore` (el asset es fuente, no generado), ni `domain.recipe/Recipe.kt`,
`RecipeJson.kt`, `domain.taxonomy/**`, `data.profile/**`, `data.generator/**` ni el catálogo de
versiones.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna (el contenido se mostrará en `home-offline-blocks`).
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness; la
  costura dominio/datos se documenta en `docs/technical-discovery.md`.
- `CONSTRAINTS.md`: **not needed** — la pureza del dominio y la separación dominio/datos ya están
  documentadas; esta feature no introduce una regla global nueva.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- Other docs: `docs/technical-discovery.md` — update (ubicación y naturaleza del catálogo semilla);
  `PROGRESS.md` y `feature_list.json` — update al implementar.

## Implementation Plan

1. Autorar `app/src/main/assets/seed-recipes.json` con 12 recetas que cumplan las reglas de §4.
2. Crear `SeedRecipeCatalog` + `SeedRecipeJson` en `com.sazon.app.domain.recipe` (puro, sin Android).
3. Crear `AssetSeedRecipeSource` en `com.sazon.app.data` (lector del asset).
4. Añadir la propiedad de sistema `seed.recipes.asset.path` en `app/build.gradle.kts`.
5. Escribir `SeedRecipeCatalogTest` (escenarios 1, 3–6, incluido el caso negativo con JSON mutado) y
   `AssetSeedRecipeSourceTest` (escenario 2, Robolectric).
6. Ejecutar `init.sh` (test + lint), corregir lo que salga y comprobar que el asset viaja en el APK.
7. Actualizar `docs/technical-discovery.md`; registrar evidencia en `feature_list.json` y
   `PROGRESS.md` (lo hace el implementer).

## Implementation Tasks

- [ ] Crear `seed-recipes.json` con 12 recetas semilla (`source = seed`, `matchScore` omitido, `id`
      `seed-*`).
- [ ] Verificar que todos los `*Id` e `ingredientId` usan ids reales de la Taxonomía.
- [ ] Marcar los productos de despensa con `isPantry = true` y declarar los alérgenos de cada
      ingrediente resuelto.
- [ ] Ajustar `abs(sum(steps.minutes) - totalMinutes) ≤ 5` en las 12 recetas.
- [ ] Crear `SeedRecipeCatalog` y `SeedRecipeJson` en `domain.recipe` (sin imports de Android).
- [ ] Crear `AssetSeedRecipeSource` en `data`, con el nombre de asset `seed-recipes.json`.
- [ ] Añadir la propiedad de sistema `seed.recipes.asset.path` en `app/build.gradle.kts`.
- [ ] Escribir `SeedRecipeCatalogTest` con los escenarios 1 y 3–6 y el caso negativo.
- [ ] Escribir `AssetSeedRecipeSourceTest` (Robolectric) para el escenario 2.
- [ ] Comprobar que `.gitignore` **no** ignora `seed-recipes.json` y que el asset está versionado.
- [ ] Ejecutar `init.sh` en verde.
- [ ] Actualizar `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`.

## Verification Plan

- `./gradlew test` → verde, con `SeedRecipeCatalogTest` y `AssetSeedRecipeSourceTest` ejecutados y
  **0 fallos**. Para depurar un test concreto:
  `./gradlew testDebugUnitTest --tests "com.sazon.app.domain.recipe.SeedRecipeCatalogTest"`
  (en este proyecto `./gradlew test --tests` no acepta `--tests`; ver `PROGRESS.md`, sesión 014).
- El **caso negativo** se implementa como aserción sobre una función de integridad
  (`violations(catalog, taxonomy)`), no como un test que se espera rojo.
- `./gradlew lint` → sin errores nuevos (los ~21 warnings preexistentes son aceptables).
- `./gradlew assembleDebug` → APK generado; `unzip -l` debe listar `assets/seed-recipes.json`.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.
- Inspección: `com.sazon.app.domain.recipe` no importa `android.*`; `seed-recipes.json` aparece en
  `git status` como fichero nuevo versionado (no ignorado).

Sobre `init.sh`: **no cambia**. Sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y esta feature no introduce UI ni flujo
visible; la verificación es de JVM (más un test Robolectric de carga de asset), que es lo que exige
`feature_list.json`. La prueba manual end-to-end llegará con las pantallas.

## Evidence To Capture

- Salida de `./gradlew test` con `SeedRecipeCatalogTest` y `AssetSeedRecipeSourceTest`, número de tests
  y 0 fallos.
- Resultado del caso negativo: el test que confirma que un catálogo roto produce ≥1 violación.
- Salida de `./gradlew lint` sin errores nuevos y `init.sh` con código 0.
- Listado del APK mostrando `assets/seed-recipes.json`.
- Confirmación de que `seed-recipes.json` está versionado (no ignorado) y que `domain.recipe` no
  importa Android.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas

- **El JSON es fuente versionada en `assets/`, no generado.** La nota de la feature y
  `technical-discovery.md` fijan `app/src/main/assets/`; no hay fuente documental que justifique un
  `docs/` + `sync`. Se distingue explícitamente de `taxonomy.json` (generado e ignorado).
- **Envelope `{ "recipes": [...] }`**: coherente con el contrato de la IA que consume el Validador y
  tolerante a campos nuevos (`ignoreUnknownKeys`).
- **Parser en `domain.recipe`, lector en `data`**: mantiene el dominio testable en JVM sin Android,
  igual que la Taxonomía.
- **12 recetas, `>= 12` en el test**: fija el compromiso de arranque sin bloquear el crecimiento del
  contenido.
- **`source = seed` y `matchScore = null`**: la identidad de la semilla y el score derivado (no
  persistido).
- **El catálogo no pasa por el `RecipeValidator`**: el contenido autorado se verifica en el test; el
  Validador es para la salida de la IA.

### Unknowns

- **Crecimiento del catálogo**: el test exige `≥ 12`; añadir recetas es contenido nuevo, no un cambio
  de código.
- **Uso del catálogo aguas abajo**: si `suggestion-batch-cache` o el flujo de generación deciden
  validar también las semillas con el `RecipeValidator`, se hará en esa feature; aquí solo se verifica
  integridad y coherencia básica.
- **Riqueza de contenido** (`tips`, `nutrition`): recomendada pero no obligatoria por esquema; queda a
  criterio del autor sin que el test lo exija.
- **Riesgo de calendario**: autorar 12 recetas es contenido, no código (riesgo ya registrado en
  `docs/risks-and-open-questions.md`); esta spec fija la forma y el mínimo verificable para que el
  contenido no bloquee al resto del flujo.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: sin UI, sin red, sin persistencia, sin generador,
      sin `matchScore` y sin tocar `Recipe.kt`/`RecipeJson.kt` ni la Taxonomía.
- [ ] El catálogo se carga, tiene ≥12 recetas y todas cumplen el esquema (`RecipeJson` ida y vuelta).
- [ ] Todas las recetas tienen `source = seed`, `matchScore = null` e `id` único con prefijo `seed-`.
- [ ] Todos los `*Id`, `ingredientId` y `declaredAllergens` resuelven en la Taxonomía; `servings` en
      `1..12`.
- [ ] `declaredAllergens` cubre los alérgenos de los ingredientes resueltos y los pasos son coherentes
      con `totalMinutes` (±5).
- [ ] El **caso negativo** existe y el test falla si el catálogo se rompe (no pasa en vacío).
- [ ] `com.sazon.app.domain.recipe` **no** importa Android; los tests corren en JVM (el de carga de
      asset, con Robolectric ya configurado).
- [ ] `seed-recipes.json` está versionado en git (no ignorado) y aparece en el APK de depuración.
- [ ] `init.sh` queda en verde, y `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`
      quedan actualizados y coherentes.

## Findings During Implementation

_(A rellenar por el implementer si aparecen desviaciones o hallazgos.)_
