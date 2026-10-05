# Feature Implementation Spec: Modelo de Receta con origen seed o ai

## Source Feature

- `id`: `recipe-domain-model`
- `area`: `domain`
- `depends_on`: `["taxonomy-in-code"]` (satisfecho: `accepted`)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Hacer que la app tenga **una única forma de representar una receta**, venga de donde venga. Hoy
`docs/domain-model.md` define el esquema de `Recipe`, `RecipeIngredient`, `RecipeStep` y `Nutrition`,
y `docs/domain/taxonomy.md` (sección 4) fija el contrato JSON que devolverá la IA, pero no existe
ninguna clase en el código: cada feature futura (validador, generador falso, catálogo semilla,
persistencia, resultados, detalle) tendería a inventarse su propio modelo y a divergir.

Esta feature entrega ese esquema como **modelos Kotlin serializables** en la capa de dominio, con
`source` (`seed` | `ai`), campos opcionales (`nutrition`, `matchScore`, `ingredientId`) y una prueba
de ida y vuelta que demuestra que una receta se serializa y se deserializa **sin pérdida**, incluida
una sin nutrición. Es cimentación pura: ninguna UI, ninguna persistencia, ninguna llamada de red.

## Non-Goals

- **No** se implementa el validador de recetas (`recipe-validator`), ni el generador falso
  (`fake-recipe-generator`), ni el catálogo semilla (`seed-recipe-catalog`), ni el constructor de
  prompt (`prompt-builder`).
- **No** se persiste nada: ni Room, ni DataStore, ni caché. Los campos de caché `generatedAt` y
  `lastOpenedAt` que menciona `docs/domain-model.md` pertenecen a `suggestion-batch-cache`; no se
  modelan aquí.
- **No** se amplía el conjunto de unidades más allá del propuesto: se adopta `MeasureUnit`
  (`g`, `ml`, `ud`, `cda`, `cdta`, según la pregunta 4). Añadir una unidad después es un cambio
  contenido en este paquete.
- **No** se implementa el cálculo de `matchScore` (ni su fórmula, ni el ranking): se modela como
  `Int?` opcional y se documenta como **derivado y no persistido**; el scorer es de `results-screen`.
  Ver "Decisions And Unknowns".
- **No** se parsea la respuesta envuelta de la IA (`{ "recipes": [...] }`): eso es del validador.
  Aquí solo se serializa/deserializa una `Recipe` individual.
- **No** se crea UI ni se empaquetan assets.

## Job Story

Cuando construyo el validador, el generador, la caché o cualquier pantalla que muestre una receta,
quiero leer y escribir un único modelo tipado, con el mismo esquema para `seed` y `ai`,
para no duplicar campos, no divergir del contrato de la IA y poder testear en JVM sin emulador.

## Users And Permissions

- **Desarrollador / agente**: puede construir, serializar y leer `Recipe` desde el dominio, sin
  permisos ni red.
- **Usuario local**: no interactúa todavía. Su efecto se verá en features de UI posteriores.

## Acceptance Scenarios

### Scenario 1: ida y vuelta de una receta completa

Given una `Recipe` de origen `seed` con nutrición, `matchScore`, un ingrediente de la Taxonomía
(con `ingredientId`), un ingrediente libre (`ingredientId = null`) y un ingrediente de despensa,
When se serializa a JSON y se vuelve a deserializar,
Then la `Recipe` resultante es **igual** a la original (todos los campos, incluidos anidados).

### Scenario 2: ida y vuelta de una receta sin nutrición (campo opcional)

Given una `Recipe` de origen `ai` **sin** bloque `nutrition` y **sin** `matchScore`,
When se serializa y se deserializa,
Then la `Recipe` resultante es igual a la original y `nutrition` y `matchScore` son `null`.

### Scenario 3: el origen se conserva como tipo distinto de `seed`

Given una receta `ai` y una `seed`,
When se serializan,
Then el JSON contiene `"source": "ai"` y `"source": "seed"` respectivamente, y al deserializar cada
una recupera su valor `RecipeSource` correcto.

### Scenario 4: la unidad es un valor cerrado y admite ausencia

Given un ingrediente con `unit = MeasureUnit.GRAM` y otro sin unidad (`unit = null`, "al gusto"),
When se serializan y deserializan,
Then el primero conserva `"g"` y su `MeasureUnit`, y el segundo conserva `null` sin error.

### Scenario 5: el modelo no depende de Android

Given el paquete `com.sazon.app.domain.recipe`,
When se compila y se ejecuta el test,
Then corre en JVM sin emulador y ninguna clase del paquete importa `android.*`.

## Repository Research

### Files Inspected

- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — patrón de modelos `@Serializable` y
  catálogo de dominio, puro Kotlin.
- `app/src/main/java/com/sazon/app/domain/taxonomy/TaxonomyJson.kt` — patrón de parser puro con
  `Json { ignoreUnknownKeys = true }`.
- `app/src/main/java/com/sazon/app/data/AssetTaxonomySource.kt` — costura dominio/datos existente.
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` — patrón de test JUnit 4
  en dominio, sin Android.
- `app/build.gradle.kts` y `gradle/libs.versions.toml` — `kotlinx.serialization` 1.9.0 ya está
  aplicado (`kotlin.plugin.serialization`) y en dependencias; **no hace falta tocar el build**.
- `docs/domain-model.md` — esquema exacto de `Recipe`, `RecipeIngredient`, `RecipeStep`, `Nutrition`.
- `docs/domain/taxonomy.md` (sección 4) — contrato JSON de la respuesta de la IA.
- `docs/risks-and-open-questions.md` — preguntas 4 (`unit`), 5 (`nutrition`) y 6 (`matchScore`)
  (numeración antigua; ver "Findings During Implementation").
- `docs/technical-discovery.md` — "el dominio no debe depender de Android para poder testarse sin
  emulador".

### Existing Patterns To Follow

- Identificadores y `@SerialName` en inglés; texto de dominio y mensajes en español.
- Paquete de dominio puro: `com.sazon.app.domain.<área>`, sin `android.*`.
- Modelos como `data class` con `@Serializable`; objetos de config JSON con `Json` propio.
- Tests JUnit 4 en `app/src/test/java/com/sazon/app/domain/...`, nombres en español con `_`.
- Verificación: `./gradlew test` y `./gradlew lint` vía `init.sh`.

### Current Gaps

- **No existe el paquete `com.sazon.app.domain.recipe`** (solo `domain.taxonomy`).
- **No hay modelo de receta**: solo el documento de dominio.
- **Faltaba cerrar la pregunta 4 (unidades) y la 5 (nutrición)**: esta feature las resuelve (ver
  "Decisions And Unknowns").
- No hay test de serialización, que es justo la verificación que exige `feature_list.json`.

## Technical Approach

### 1. Dónde vive

Nuevo paquete `com.sazon.app.domain.recipe`, puro Kotlin, apoyado en `kotlinx.serialization` ya
presente. Se mantiene la misma costura que en la Taxonomía: el dominio no lee ficheros ni conoce
Android; solo modela y (de)serializa.

### 2. Modelos

Un único esquema para `seed` y `ai`, tomado literalmente de `docs/domain-model.md`:

- `RecipeSource` — `enum class` serializable con `@SerialName("seed") SEED` y `@SerialName("ai") AI`.
- `MeasureUnit` — `enum class` serializable con los cinco ids de la pregunta 4:
  `@SerialName("g") GRAM`, `@SerialName("ml") MILLILITER`, `@SerialName("ud") UNIT`,
  `@SerialName("cda") TABLESPOON`, `@SerialName("cdta") TEASPOON`.
- `Recipe` — `id`, `source`, `title`, `description`, `categoryId`, `mealTypeId`, `cuisineId`,
  `totalMinutes`, `prepMinutes`, `cookMinutes`, `difficultyId`, `servings`, `ingredients`,
  `steps`, `tips`, `nutrition: Nutrition? = null`, `declaredAllergens`, `matchScore: Int? = null`.
- `RecipeIngredient` — `name`, `quantity: Double`, `unit: MeasureUnit? = null`,
  `isPantry: Boolean = false`, `ingredientId: String? = null`.
- `RecipeStep` — `order`, `title`, `description`, `minutes`.
- `Nutrition` — `kcal`, `protein`, `carbs`, `fat` (`Int`).

Decisiones concretas:

- **`source` es un enum, no un `String`**, para que un valor raro no compile en silencio y para que
  el test pueda comparar tipos.
- **`tips` y `declaredAllergens` con `= emptyList()`**: las recetas legítimamente pueden no tenerlos;
  evita fallos de parseo sin abrir la puerta a campos obligatorios ausentes.
- **`nutrition`, `matchScore` e `ingredientId` son opcionales** (`null`), según el esquema.
- **`matchScore` es un campo de cable, no una verdad**: se mantiene para no romper el contrato JSON
  (`docs/domain/taxonomy.md`), pero la app **recalcula** el porcentaje; el valor que traiga la IA no
  se persiste ni se muestra tal cual. Ver "Decisions And Unknowns".
- **`quantity` es `Double`** para permitir el escalado de raciones futuro.
- **`unit` es `MeasureUnit?`**: enum cerrado para lo medible (facilita el redondeo por unidad del
  escalado de raciones) y `null` para "al gusto" / sin medida. Un valor fuera del enum se rechaza o
  se normaliza en el Validador, no aquí.

### 3. Configuración de serialización

Un objeto `RecipeJson` (patrón de `TaxonomyJson`) encapsula un único `Json` con
`ignoreUnknownKeys = true` y `encodeDefaults = true`, y expone `encode(recipe): String` y
`parse(json): Recipe`. Centralizar la config evita que cada feature futura ajuste la suya y
garantiza **ida y vuelta sin pérdida** (con `encodeDefaults` los campos con defecto se escriben).

### 4. Modo de fallo

Coherente con la Taxonomía: **fallar rápido**. Si un JSON no encaja con el esquema, `parse` lanza
`SerializationException`; no se degrada en silencio. El manejo de JSON inválido de la IA (reintentos,
cuarentena) es de `recipe-validator` / integración, no de este modelo.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — crear; `Recipe`, `RecipeIngredient`,
  `RecipeStep`, `Nutrition`, `RecipeSource` y `MeasureUnit`.
- `app/src/main/java/com/sazon/app/domain/recipe/RecipeJson.kt` — crear; `Json` compartido y
  helpers `encode`/`parse`.
- `app/src/test/java/com/sazon/app/domain/recipe/RecipeSerializationTest.kt` — crear; escenarios de
  ida y vuelta (con y sin nutrición) y conservación de `source`.
- `docs/technical-discovery.md` — modificar si procede; anotar dónde vive el modelo de Receta (junto
  a la nota ya existente sobre la Taxonomía).
- `docs/risks-and-open-questions.md` — modificar; mover las preguntas 4 y 5 a resueltas y ajustar la
  numeración posicional y sus citas.

No se toca `app/build.gradle.kts` ni `libs.versions.toml`: la serialización ya está configurada.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — "el dominio no depende de Android" ya está documentado.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- Other docs: `docs/technical-discovery.md` — update ligero si se añade la ubicación del modelo;
  `docs/risks-and-open-questions.md` — update al resolver las preguntas 4 (unidades) y 5 (nutrición);
  `PROGRESS.md` y `feature_list.json` — update con estado y evidencia al implementar.

## Implementation Plan

1. Crear `Recipe.kt` con `RecipeSource`, `MeasureUnit`, `Recipe`, `RecipeIngredient`, `RecipeStep` y
   `Nutrition`.
2. Crear `RecipeJson.kt` con el `Json` compartido y `encode`/`parse`.
3. Escribir `RecipeSerializationTest.kt` con la receta completa, la receta sin nutrición, la
   conservación de `source`, la unidad cerrada/ausente y la ausencia de imports de Android
   (verificable por inspección).
4. Resolver las preguntas 4 y 5 en `docs/risks-and-open-questions.md`, actualizando las citas de
   numeración que se vean afectadas.
5. Ejecutar `init.sh` (test + lint), corregir lo que salga y actualizar documentación y harness.

## Implementation Tasks

- [x] Crear el paquete `com.sazon.app.domain.recipe`.
- [x] Modelar `RecipeSource` como enum serializable (`seed`/`ai`).
- [x] Modelar `MeasureUnit` como enum serializable (`g`/`ml`/`ud`/`cda`/`cdta`).
- [x] Modelar `Recipe`, `RecipeIngredient`, `RecipeStep` y `Nutrition` con los campos y opcionales
      de `docs/domain-model.md`.
- [x] Crear `RecipeJson` con `ignoreUnknownKeys` y `encodeDefaults`.
- [x] Escribir el test de ida y vuelta de una receta completa.
- [x] Escribir el test de ida y vuelta de una receta sin nutrición ni `matchScore`.
- [x] Escribir el test de conservación del `source`.
- [x] Escribir el test de unidad cerrada (`g`) y unidad ausente (`null`).
- [x] Verificar que no hay imports `android.*` en el paquete.
- [x] Resolver las preguntas 4 y 5 en `docs/risks-and-open-questions.md` y ajustar las citas.
- [x] Ejecutar `init.sh` en verde.
- [x] Actualizar `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`.

## Verification Plan

- `./gradlew test` → verde, con `RecipeSerializationTest` ejecutado y 0 fallos.
- `./gradlew lint` → sin errores.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.
- Inspección: `RecipeSerializationTest` no importa `android.*`; el paquete de dominio tampoco.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y la feature no cambia comportamiento
observable; no hay UI ni flujo. El test de JVM es el nivel adecuado y es lo que exige
`feature_list.json`.

## Evidence To Capture

- Salida de `./gradlew test` con el nombre y el número de tests de `RecipeSerializationTest` y 0
  fallos.
- Salida de `./gradlew lint` sin errores.
- Salida de `init.sh` con código 0.
- Confirmación por inspección de que `domain.recipe` no importa Android.
- Anotación en `feature_list.json` de la evidencia y el cambio de estado a `passing`.

## Decisions And Unknowns

Decisiones cerradas por esta feature (deben persistirse en `docs/risks-and-open-questions.md` al
implementar):

- **Pregunta 4 (unidades)**: **resuelta**. Conjunto cerrado `MeasureUnit` = `g`, `ml`, `ud`, `cda`,
  `cdta`, y `unit` nullable para "al gusto". Es el enum lo que permite el redondeo por unidad del
  escalado de raciones (`recipe-detail-screen`).
- **Pregunta 5 (nutrición)**: **resuelta** como **opcional en el esquema** (`nutrition: Nutrition?`),
  pero **se pedirá siempre que se pueda** en el constructor de prompt y el generador. La instrucción
  "intenta obtenerla siempre" es de `prompt-builder` / generación, no de este modelo.

Dirección provisional (la pregunta 6, numeración antigua —hoy la 4— sigue **abierta** en
`docs/risks-and-open-questions.md` hasta cerrarla en `results-screen`):

- **Pregunta 6 (`matchScore`)**: lo **calcula la app**, de forma determinista y en el dominio, no el
  modelo. Motivo decisivo: las recetas `seed` son contenido offline y deben mostrar coincidencia sin
  IA, así que el score tiene que ser calculable sin red. El valor es **derivado y no persistido** (el
  mismo `Recipe` puntúa distinto según perfil/ingredientes/consulta); por eso este modelo solo
  garantiza que el campo existe como `Int?` y que no es fuente de verdad. La **fórmula y los pesos
  quedan pendientes** y no se inventan aquí; la firma y la ubicación del scorer son de
  `results-screen` (rankings por preferencias blandas: `docs/domain-model.md`). Uso previsto: mostrar
  el "porcentaje de coincidencia" en la tarjeta y ordenar resultados; nunca descarta recetas.

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance: sin validador, sin generador, sin catálogo,
      sin persistencia, sin UI.
- [ ] Los cinco escenarios de aceptación pasan, incluida la receta sin nutrición y la unidad ausente.
- [ ] El esquema coincide campo a campo con `docs/domain-model.md` y con la sección 4 de
      `docs/domain/taxonomy.md`.
- [ ] `MeasureUnit` cubre exactamente `g`, `ml`, `ud`, `cda`, `cdta`; no se cuelan unidades extra.
- [ ] `com.sazon.app.domain.recipe` **no** importa Android y los tests corren en JVM.
- [ ] No se han tocado `app/build.gradle.kts` ni `libs.versions.toml` sin necesidad.
- [ ] Las preguntas 4 y 5 quedan resueltas en `docs/risks-and-open-questions.md` de forma coherente
      con el modelo; la pregunta 6 (numeración antigua, hoy 4) sigue **abierta** allí, con la
      dirección provisional (lo calcula la app, derivado y no persistido) registrada solo en esta
      spec.
- [ ] El modelo **no** trata `matchScore` como fuente de verdad ni lo persiste; queda como `Int?` de
      cable.
- [ ] `feature_list.json`, `PROGRESS.md` y la documentación relevante están actualizados y coherentes.

## Findings During Implementation

1. **Renumeración de las preguntas abiertas.** Al resolver las antiguas preguntas 4 (unidades) y 5
   (nutrición), se retiraron de la tabla de `docs/risks-and-open-questions.md` y el resto se
   renumeró: `matchScore` pasó a ser la 4, trazas la 5, errores/reintentos la 6, ingrediente libre la
   7, licencias la 8 y recetas semilla la 9. Se actualizaron las citas afectadas en
   `feature_list.json` (nota de `seed-recipe-catalog`) y en `docs/technical-discovery.md`.
2. **La pregunta 4 (`matchScore`) sigue abierta.** No se tocó en `docs/risks-and-open-questions.md`
   más allá de la renumeración; la dirección provisional (lo calcula la app, derivado y no
   persistido) se registra solo en esta spec, tal y como se acordó.
3. **Falló una aserción del propio test, no el modelo.** La primera ejecución dio `1 failed` en
   `una_receta_completa_va_y_vuelve_sin_perdida`: esperaba 1 ingrediente libre y la fixture tiene 2.
   Corregida la fixture/aserción; el modelo serializa y deserializa sin pérdida.
