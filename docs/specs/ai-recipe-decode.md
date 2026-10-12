# Feature Implementation Spec: Decodificar las recetas de la IA completando id y source

## Source Feature

- `id`: `ai-recipe-decode` · `area`: `integration` · `depends_on`: `["real-ai-provider"]`
  (`accepted`, sesión 067) · `status`: `not_started` · `source`: `feature_list.json`
- **No es feature de UI**: sin pantallas, estados, microcopy ni capturas. El comportamiento visible
  ya existe (Generador, Resultados, Home, Búsqueda con IA); lo que cambia es que **deja de quedar
  vacío**.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) `./gradlew test` cubre que una respuesta con la **forma real del
  contrato** (sin `id` ni `source`) se decodifica a recetas válidas, que cada receta recibe **`id`
  único** y **`source = ai`**, y que las **reglas del Validador siguen aplicándose**; (2) prueba
  **manual** en emulador: con clave válida, la búsqueda por texto (*"arroz con pollo"*) y el Generador
  muestran recetas reales; con clave ausente la app sigue en modo semilla.

**Contexto (correctiva de `real-ai-provider`, issue #70).** Reproducido contra el proveedor real:
HTTP 200 con **6 recetas válidas**, todos los ids de Taxonomía correctos, pero **sin `id` ni `source`
en ninguna**. `AiRecipePayload.decode` delega en `RecipeValidator.validateResponse`, que deserializa
cada elemento de `{"recipes":[...]}` **directamente con `Recipe.serializer()`**; `Recipe.id` y
`Recipe.source` son campos **obligatorios** y el contrato de `docs/domain/taxonomy.md` §4 **no los
pide** (los pone la app). Resultado: toda receta cae como `DiscardReason.SCHEMA`, el `accepted` queda
vacío y la operación termina en **EMPTY** (estado "no ha encontrado recetas" / degradación), no en
recetas reales. Los tests no lo detectaban porque fabricaban la respuesta con
`RecipeJson.encode(recipe)`, que **sí** incluye `id` y `source`.

## Goal

Que el adaptador real del puerto `RecipeGenerator` **decodifique la forma que el modelo envía de
verdad** (`docs/domain/taxonomy.md` §4, sin `id` ni `source`) y la complete como Receta de dominio
**antes** de aplicar las reglas del Validador: la app asigna a cada receta un `id` y `source = ai`, y
después se aplican las reglas 1–4 con `RecipeValidator.validateRecipe`. Todo el cambio vive en
`com.sazon.app.data.ai` y sus tests de JVM.

La frontera de dominio **no se debilita**: `Recipe.id` y `Recipe.source` siguen siendo obligatorios,
`Recipe` y `RecipeValidator` **no se tocan** y el esquema no se hace opcional. Lo que cambia es que el
adaptador deja de exigir al modelo campos que el contrato reserva a la app. Resultado: con clave
válida, Generador, Home (Lote) y Búsqueda con IA vuelven a mostrar recetas reales del proveedor.

## Non-Goals

- **No** se cambia el modelo de dominio `Recipe` (ni `id`/`source` opcionales), ni `RecipeValidator`,
  ni `domain.generator`, `domain.validation`, `domain.prompt`, `domain.recipe`, `domain.profile`,
  `domain.suggestions`, `domain.search`, `domain.generation`. El contrato del Validador y sus reglas
  quedan **intactos**.
- **No** se cambia el contrato del prompt ni de la petición (`PromptBuilder`, `AiProviderPolicy`,
  `OpenAiChat`, `AiHttpClientFactory`): el modelo sigue sin recibir ni enviar `id`/`source`.
- **No** se cambia `DevExpertRecipeGenerator.execute` (sigue llamando a `AiRecipePayload.decode`), ni
  la política de timeouts/reintentos/deadline, ni la degradación a modo semilla
  (`ai-provider-timeouts` no se toca).
- **No** se persiste nada nuevo ni se cambia el Lote, Favoritos, la Home ni la UI.
- **No** se implementan `response_format`/structured outputs, `chat-pro`, streaming, `max_tokens` ni
  multi-proveedor (ADR 0003).
- **No** se añaden dependencias ni cambios de build (`app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**` no se tocan).
- **No** se reescribe la spec histórica `docs/specs/real-ai-provider.md` (solo se puede anotar como
  hallazgo si el implementer lo considera útil).

## Job Story

Como usuario local con clave válida,
quiero que la app acepte las recetas que el modelo devuelve tal como el contrato las define, aunque
no traigan `id` ni `source`,
para volver a ver recetas reales en el Generador y en la Búsqueda con IA en lugar del estado "no ha
encontrado recetas".

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia lo que ve salvo que las recetas
  pasan a mostrarse; sigue pudiendo cancelar la carga.
- **Desarrollador / agente**: verifica en **JVM** con `MockEngine` y Taxonomía real: forma real del
  contrato, unicidad/determinismo de `id`, `source = ai` y reglas del Validador. Sin red real ni clave
  real.
- **Sistema**: `AiRecipePayload.decode` sigue siendo **puro y tolerante** (nunca lanza; `Malformed` →
  lista vacía; elemento roto no tumba el lote) y sigue aplicando las reglas del Validador con las
  restricciones **neutras** de `AiProviderPolicy` (el proveedor no pre-filtra por dieta/restricciones;
  eso lo hacen los llamadores aceptados).

## Acceptance Scenarios

### Scenario 1: forma real del contrato → recetas válidas (la verificación exigida)

Given un `content` = `{"recipes":[<receta del contrato §4>]}` **sin `id` ni `source`** y la Taxonomía
real,
When se llama a `AiRecipePayload.decode(content, taxonomy)`,
Then devuelve **1 receta** (no vacío), decodificable, con `title` y el resto de campos intactos.

### Scenario 2: cada receta recibe `id` único y `source = ai`

Given un `content` con **varias** recetas del contrato §4 (sin `id`/`source`) y una de ellas
**duplicada literalmente**,
When se decodifica,
Then **todas** las recetas llevan `source == RecipeSource.AI`, `id` no vacío y prefijo `ai-`, y **no
hay dos ids iguales** en la lista (el duplicado se desambigua).

### Scenario 3: ids deterministas, sin `Random`, reloj ni `UUID`

Given el mismo `content` decodificado **dos veces** (y en distinto orden de recetas),
When se comparan,
Then cada receta obtiene **el mismo `id`** que su contenido (misma receta → mismo id; contenido
distinto → id distinto) y no depende de reloj ni de azar.

### Scenario 4: las reglas del Validador siguen aplicándose

Given un `content` con una receta con `mealTypeId` (o `cuisineId`/`difficultyId`) **inexistente** y
otra con **incoherencia de tiempos** (`sum(steps.minutes)` se desvía más de 5 min de `totalMinutes`),
When se decodifica,
Then esas recetas **no** aparecen en el resultado (descartadas por `SCHEMA`/`INCOHERENT`), mientras la
receta válida del mismo lote **sí** aparece (el elemento roto no tumba el resto).

### Scenario 5: tolerancia preservada (no lanza)

Given un `content` `null`, en blanco, no-JSON, `{"foo":1}`, `[1,2]`, `{"recipes": 3}`, o envuelto en
una valla markdown,
When se decodifica,
Then devuelve **lista vacía** sin lanzar (una sola "operación", sin reintento).

### Scenario 6: `matchScore` lo sigue derivando la app

Given un contrato del modelo que incluyera `matchScore` (campo de §4),
When se decodifica,
Then la receta devuelta lleva `matchScore == null` (el score es derivado y no persistido; coincide con
`FakeRecipeGenerator` y con `ui.results.MatchScore`).

### Scenario 7: regresión end-to-end (el bug de #70)

Given un `MockEngine` que responde `200` con `choices[0].message.content` = forma real del contrato
(sin `id`/`source`) y una clave de test,
When se invoca `generateFromIngredients(...)` y, con el `DevExpertRecipeGenerator` real, se ejecuta
`GenerationService.generate(...)` con un Perfil sin restricciones,
Then el proveedor devuelve las recetas y el `GenerationResult` tiene `accepted` **no vacío** con
`aiUnavailable == false`. Con el código de `main` este mismo test daría lista vacía: es la prueba de
regresión real.

### Scenario 8: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida en `local.properties` y el emulador de Android Studio en **Windows**,
When se busca *"arroz con pollo"* y se genera desde el Generador,
Then aparecen recetas reales del proveedor (pasan el Validador) en lugar del estado vacío; y con la
clave vaciada o ausente, la app sigue en **modo semilla** sin fallar.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `ai-recipe-decode` (causa raíz de #70) y `real-ai-provider`/`ai-provider-timeouts`.
- `docs/specs/real-ai-provider.md` — contrato de `AiRecipePayload.decode` (§3) y su *Unknown* de
  fidelidad del modelo al contrato.
- `docs/specs/ai-provider-timeouts.md` — convenciones de una correctiva de `real-ai-provider`.
- `docs/domain/taxonomy.md` §4 (contrato JSON: **no** incluye `id` ni `source`; sí `matchScore`) y §5
  (reglas del Validador 1–4).
- `docs/domain-model.md` — entidad `Recipe` (§48–63) y la línea **drift** sobre el `id` de las recetas
  `ai` ("lo asigna la app al persistir").
- `docs/technical-discovery.md` — párrafo de `data.ai` (líneas ~340–360 y ~446–458) que hoy describe
  el decode "elemento a elemento con `RecipeValidator`".
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — el sitio del cambio (llama a
  `RecipeValidator.validateResponse`).
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — `execute` llama a
  `AiRecipePayload.decode(outcome.content, taxonomy)`; **no cambia**.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — `validateResponse` (decodifica
  con `Recipe.serializer()` y por eso exige `id`/`source`) y `validateRecipe` (**público**, aplica las
  reglas 1–4; es la costura que se reutiliza).
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `id`/`source` obligatorios;
  `RecipeIngredient`, `RecipeStep`, `Nutrition` (`@Serializable`) sin campos de app.
- `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt` — precedente de ids
  deterministas (`fake-*`), `source = AI` y `matchScore = null`, sin `Random`/reloj/`UUID`.
- `app/src/main/java/com/sazon/app/domain/profile/PreferenceSignature.kt` — precedente de id/firma
  determinista con **SHA-256 en hex minúscula** sobre cadena canónica con separador `U+001F`.
- `app/src/main/java/com/sazon/app/data/favorites/{FavoriteEntity.kt,FavoriteDao.kt}` y
  `ui/MainShell.kt` — `Recipe.id` es la **clave primaria** del Favorito y la identidad de
  `isSaved`: justifica que los ids no colisionen entre llamadas.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` y `DevExpertRecipeGeneratorTest.kt`
  — ambos fabrican el envelope con `RecipeJson.encode(recipe)` (incluye `id`/`source`): es el punto
  ciego que esta feature cierra.
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` — valida receta a receta
  con `validateRecipe`; permite el test de regresión end-to-end en JVM.
- `init.sh` y `scripts/gradle.sh` — `bash init.sh` = `./gradlew test` + `./gradlew lint`; sin modos
  nuevos.

### Existing Patterns To Follow

- **Puerto de dominio + adaptador en `data`**: `data.ai` traduce el formato del proveedor al modelo de
  dominio; el dominio no conoce el wire.
- **Determinismo sin `Random`/reloj/`UUID`**: `FakeRecipeGenerator` y `PreferenceSignature` lo
  declaran y lo testean; la nueva política de ids sigue esa disciplina.
- **SHA-256 hex minúscula** con prefijo de versión y separador `U+001F` (`PreferenceSignature`).
- **Tests JUnit 4 en JVM**, Taxonomía real por `System.getProperty("taxonomy.asset.path")`, `MockEngine`
  con clave ficticia `test-key`, nombres en español con `_` y casos negativos que no pasan en vacío.
- **Tolerancia "nunca lanza"** del decode (elemento a elemento; `Malformed` → vacío).

### Current Gaps

- El decode exige `id`/`source` que el contrato no pide → **todos** los lotes reales caen como
  `SCHEMA` y la operación queda EMPTY (bug real, issue #70).
- No existe una representación del **wire** del contrato §4 (Receta *sin* los campos que posee la app).
- No hay tests con la **forma real** del contrato (todos usan `RecipeJson.encode`, que añade `id`/`source`).
- `docs/domain-model.md` está **desalineado**: dice que el `id` de las recetas `ai` se asigna "al
  persistir", cuando la arquitectura actual exige tenerlo antes de validar y mostrar.

## Technical Approach

Estrategia: **capa anti-corrupción en `data.ai`**. El adaptador decodifica el wire del modelo a un
DTO del contrato, lo completa a `Recipe` de dominio (con `id` y `source = ai`) y **después** aplica las
reglas del Validador con `RecipeValidator.validateRecipe`. Ni `Recipe` ni `RecipeValidator` cambian.

### 1. DTO del wire — `com.sazon.app.data.ai.AiRecipeDraft`

`@Serializable internal data class AiRecipeDraft` que **calca el contrato §4 menos los campos que
posee la app**: `title`, `description`, `categoryId`, `mealTypeId`, `cuisineId`, `totalMinutes`,
`prepMinutes`, `cookMinutes`, `difficultyId`, `servings`, `ingredients: List<RecipeIngredient>`,
`steps: List<RecipeStep>`, `tips: List<String> = emptyList()`, `nutrition: Nutrition? = null`,
`declaredAllergens: List<String> = emptyList()`.

- Reutiliza `RecipeIngredient`, `RecipeStep`, `Nutrition` (y `MeasureUnit`) de `domain.recipe`: son
  esquema sin campos de app.
- **No** declara `id`, `source` ni `matchScore`: se decodifica con `ignoreUnknownKeys = true`, así que
  un `id`/`source`/`matchScore` enviado por el modelo **se ignora** (la app siempre posee esos campos).
- `fun toRecipe(id: String): Recipe` mapea 1:1 y fija `source = RecipeSource.AI`, `matchScore = null`
  (derivado y no persistido).

### 2. Política de ids determinista — `com.sazon.app.data.ai.AiRecipeIds`

`internal object AiRecipeIds`, sin `Random`, sin reloj y sin `UUID`:

```kotlin
internal object AiRecipeIds {
    const val PREFIX = "ai-"
    /** Cadena canonica del contenido (prefijo de version + campos con separador U+001F). */
    fun canonical(draft: AiRecipeDraft): String
    /** id unico en la llamada: PREFIX + sha256hex(canonical); ante contenido duplicado, sufijo -2, -3... */
    fun idFor(draft: AiRecipeDraft, used: MutableSet<String>): String
}
```

- `canonical`: prefijo `ai-recipe-v1` + `title` · `description` · `categoryId` · `mealTypeId` ·
  `cuisineId` · `totalMinutes` · `prepMinutes` · `cookMinutes` · `difficultyId` · `servings` ·
  ingredientes en orden (`name`/`quantity`/`unit`/`isPantry`/`ingredientId`) · pasos en orden
  (`order`/`title`/`minutes`) · `declaredAllergens` ordenados · nutrición (o `none`), unidos con
  `U+001F` (listas con `U+001E`), como `PreferenceSignature`.
- `idFor`: `PREFIX + SHA-256(canonical)` en hex minúscula (`java.security.MessageDigest`); si el id ya
  está en `used`, prueba `-2`, `-3`, … (determinista según el orden de la respuesta) y lo añade a
  `used`. Garantiza **unicidad en la llamada** y **estabilidad** (mismo contenido → mismo id).

**Justificación (frente a `UUID.randomUUID()` / índice / reloj):**

- El repo evita `Random`/reloj/`UUID` en lógica determinista (`FakeRecipeGenerator`,
  `PreferenceSignature`) y lo testea; un hash es **asertable** por el test (mismo contenido → mismo
  id) mientras que un `UUID` no.
- `Recipe.id` es **clave primaria de Favoritos** (`favorite_recipe.recipeId`) y la identidad de
  `MainShell.isSaved`: ids basados solo en el índice (`ai-0`, `ai-1`) **colisionarían entre
  llamadas** y un Favorito de una receta generada se sobrescribiría con otra receta distinta. El hash
  de contenido es único entre llamadas (salvo duplicado exacto, que se desambigua).
- Bonus: regenerar el **mismo** contenido conserva el id → la receta sigue marcada como guardada; es
  coherente con la deduplicación por título del Lote y con `PreferenceSignature`.

### 3. `AiRecipePayload.decode` — completar antes de validar

Sin cambiar la firma `(content: String?, taxonomy: Taxonomy): List<Recipe>`:

1. `content` `null`/en blanco → `emptyList()`.
2. `stripMarkdownFence` (se conserva).
3. Parseo del root `JsonObject` y del array `recipes`; si no encaja → `emptyList()` (tolerante, nunca
   lanza).
4. Elemento a elemento: `json.decodeFromJsonElement(AiRecipeDraft.serializer(), element)`; si falla, se
   salta (equivale al descarte `SCHEMA`) sin tumbar el resto.
5. `draft.toRecipe(id = AiRecipeIds.idFor(draft, usedIds))` → **la app completa `id` y `source = ai`
   ANTES del Validador**.
6. `RecipeValidator.validateRecipe(recipe, taxonomy, AiProviderPolicy.NEUTRAL_CONSTRAINTS)`:
   `Accepted` → lista; `Discarded` → se omite (el decode solo devuelve aceptadas, igual que hoy).

### 4. Decisión sobre `RecipeValidator.validateResponse`

`validateResponse` **no se modifica** (es API de dominio aceptada y sus tests siguen verdes). Su
contrato es "decodifica elementos que llevan el esquema **completo** de `Recipe`"; el wire del
proveedor legítimamente **no** lo lleva, por eso el adaptador hace su propio wire→dominio y delega
**las reglas** en `validateRecipe` (el mismo motor, público). Consecuencia asumida y explícita:
`validateResponse` deja de tener llamador en `main` (sigue cubierto por `RecipeValidatorTest`). Se
documenta como decisión; no se borra ni se toca el dominio.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/data/ai/AiRecipeDraft.kt` — DTO del wire del contrato §4 +
  `toRecipe(id)`.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeIds.kt` — ids deterministas (`canonical`,
  `idFor`), sin `Random`/reloj/`UUID`.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipeIdsTest.kt` — determinismo y unicidad de la
  política (JVM, puro).

**Modificar**

- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — decode por `AiRecipeDraft`, completar
  `id`/`source` y aplicar `RecipeValidator.validateRecipe`; KDoc actualizado.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` — escenarios 1–6 con la **forma
  real** del contrato (**JSON literal sin `id`/`source`**, nunca `RecipeJson.encode`).
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — escenario 7: `MockEngine`
  200 con forma real → recetas + `GenerationService` con `accepted` no vacío.
- `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/domain/taxonomy.md` — ver *Durable
  Documentation Impact*.

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiProviderPolicy.kt`,
`AiDiagnostics.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`,
`domain/**`, `data/generator/**`, `ui/**`, las bases Room, `app/src/main/assets/**`,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
`DESIGN.md`, `docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). No hay pantalla, estado, microcopy, token ni acabado
  nuevos; el comportamiento visible ya aceptado (Generador, Resultados, Home, Búsqueda con IA) pasa a
  mostrar recetas reales en lugar del estado vacío.
- New design artifact required: **no**; `docs/design/concepts/ai-recipe-decode/` **no** es necesario y
  la feature **no** se bloquea por capturas. No se piden capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — el párrafo de `data.ai` (y "Testing and Verification")
  pasa de "decode elemento a elemento con `RecipeValidator`" a "el adaptador mapea el **wire del
  contrato §4** (`AiRecipeDraft`) a `Recipe`, **completando `id` (`ai-<sha256>`) y `source = ai`**
  antes de aplicar `RecipeValidator.validateRecipe` con restricciones neutras".
- `docs/domain-model.md`: **update** — corregir el drift de la línea del `id` (§60): en las recetas
  `ai` la app asigna `id` y `source = ai` **al decodificar la respuesta del proveedor** (en `data.ai`,
  antes del Validador), no "al persistir"; el id es determinista por contenido y eso es lo que hace
  que un Favorito siga resolviendo aunque el Lote se refresque.
- `docs/domain/taxonomy.md`: **update (mínimo)** — una línea en §4 aclarando que `id` y `source` los
  pone la app (el modelo **no** debe enviarlos). Es el contrato que se incumplía de facto en el código.
- `docs/risks-and-open-questions.md`: **not needed** — no nace ninguna pregunta abierta; el *Unknown*
  de "fidelidad del modelo al contrato" de `real-ai-provider` se puede dar por resuelto en lo que
  respecta a `id`/`source` (opcional anotarlo en `real-ai-provider.md`).
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian. `DESIGN.md` y `docs/design/**`:
  **not needed**. `CONSTRAINTS.md`/`ARCHITECTURE.md`: no aplican (el repo los omite).
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. Crear `AiRecipeDraft` (wire del contrato §4) y `AiRecipeIds` (determinista).
2. Reescribir `AiRecipePayload.decode` para decodificar drafts, completar `id`/`source` y delegar las
   reglas en `RecipeValidator.validateRecipe`; actualizar su KDoc.
3. Añadir los tests de la forma real a `AiRecipePayloadTest` y `AiRecipeIdsTest`.
4. Añadir el test de regresión end-to-end a `DevExpertRecipeGeneratorTest`.
5. `bash init.sh` en verde; actualizar `docs/technical-discovery.md`, `docs/domain-model.md` y
   `docs/domain/taxonomy.md`; el implementer registra estado y evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `AiRecipeDraft` con los campos de §4 (sin `id`/`source`/`matchScore`), defaults de `tips`/
      `nutrition`/`declaredAllergens` y `toRecipe(id)` que fija `source = AI` y `matchScore = null`.
- [ ] `AiRecipeIds`: `canonical` (prefijo `ai-recipe-v1`, separadores `U+001F`/`U+001E`) e `idFor`
      (`ai-` + SHA-256 hex minúscula, sufijo `-2`/`-3`… ante duplicado exacto); sin
      `Random`/reloj/`UUID`.
- [ ] `AiRecipePayload.decode`: root/array tolerantes; elemento a elemento con `AiRecipeDraft` +
      `ignoreUnknownKeys`; completar `id`/`source`; `RecipeValidator.validateRecipe` con
      `AiProviderPolicy.NEUTRAL_CONSTRAINTS`; nunca lanza; KDoc actualizado.
- [ ] `AiRecipePayloadTest`: fixture **literal** del contrato sin `id`/`source` (no `RecipeJson.encode`);
      escenarios 1 (decodifica), 2 (id único + `source = ai`, con duplicado literal), 4 (ids
      desconocidos y deriva de tiempos descartados; la válida sobrevive), 5 (tolerancia), 6
      (`matchScore == null`); conservar los 5 tests actuales en verde.
- [ ] `AiRecipeIdsTest`: mismo contenido → mismo id; contenido distinto → id distinto; dos drafts
      idénticos con `used` compartido → ids distintos; prefijo `ai-` y hex de 64 caracteres.
- [ ] `DevExpertRecipeGeneratorTest`: `MockEngine` 200 con forma real (JSON literal) →
      `generateFromIngredients` devuelve recetas y `GenerationService.generate` da `accepted` no vacío
      con `aiUnavailable == false` (regresión de #70).
- [ ] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
      verde (0 errores de lint y sin warnings nuevos en ficheros de la feature).
- [ ] Inspección de alcance: `git diff --stat` solo con lo previsto; `Recipe.kt` y `RecipeValidator.kt`
      intactos.
- [ ] Docs durables (`technical-discovery.md`, `domain-model.md`, `taxonomy.md` §4).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).
- `AiRecipePayloadTest` — **forma real** `{"recipes":[<contrato §4>]}` (literal, sin `id`/`source`):
  decodifica a recetas válidas (Scenario 1); todas con `source == RecipeSource.AI` y `id` con prefijo
  `ai-`, y la lista de ids **sin duplicados** aunque el content lleve una receta repetida
  (Scenario 2); `mealTypeId`/`cuisineId`/`difficultyId` inexistente y deriva de tiempos > 5 min → esa
  receta no aparece y la válida sí (Scenario 4); `null`/blanco/no-JSON/`{"foo":1}`/`[1,2]`/
  `{"recipes":3}`/valla markdown → lista vacía sin lanzar (Scenario 5); `matchScore` del content → la
  receta sale con `matchScore == null` (Scenario 6). En todos, sin `RecipeJson.encode`.
- `AiRecipeIdsTest` — determinismo y unicidad (Scenario 3): misma entrada → mismo id; entrada distinta
  → id distinto; `used` compartido desambigua duplicados; formato `ai-<64 hex>`.
- `DevExpertRecipeGeneratorTest` — Scenario 7: `MockEngine` 200 con forma real → recetas; con
  `GenerationService` real (Perfil sin restricciones) → `accepted` no vacío, `aiUnavailable == false`,
  1 llamada. Regresión: contra `main` este test daría `accepted` vacío.
- Regresión: los 33 tests actuales de `data.ai` (`AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5,
  `DevExpertRecipeGeneratorTest` 18) siguen verdes; `RecipeValidatorTest` (12) intacto.
- **Lint:** `bash init.sh` → 0 errores y sin warnings nuevos (los 13 preexistentes se mantienen).
- **Inspección:** `git diff --stat` limitado a lo previsto; `Recipe.kt`, `RecipeValidator.kt`,
  `AiProviderPolicy.kt`, `DevExpertRecipeGenerator.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`,
  `domain/**`, `ui/**`, build, Taxonomía, `seed-recipes.json` e `init.sh` sin cambios; sin dependencias
  nuevas. `git grep -n "validateResponse" app/src/main` debe quedarse sin resultados (decisión §4).
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):** con
  `devexpert.apiKey` válida en `local.properties`, `bash scripts/gradle.sh assembleDebug` y ejecutar;
  buscar *"arroz con pollo"* y generar desde el Generador → **recetas reales** que pasan el Validador
  (verificación 2, parte 1); logcat `SUCCESS` en vez de `EMPTY`; con la clave vaciada o ausente → sigue
  el **modo semilla** sin fallar (parte 2). Cambiar `local.properties` exige reconstruir.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; el `MockEngine` cubre el transporte y el
  wiring en JVM y lo visual/manual lo revisa la persona). `init.sh` **no cambia**.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.*` con recuento de tests y 0 fallos
  (esperado: 376 preexistentes + los nuevos, sin eliminar ninguno).
- Nombres y resultado de los tests nuevos: forma real → recetas con `id`/`source`; unicidad y
  determinismo de ids; reglas del Validador (ids desconocidos y deriva de tiempos); tolerancia;
  `matchScore == null`; y el end-to-end con `GenerationService` (nota de que contra `main` daría vacío).
- Reproducción del bug (contexto de #70): HTTP 200 con 6 recetas válidas y sin `id`/`source` → decode
  vacío; tras el cambio, decode con recetas.
- `git diff --stat` de lo tocado y confirmación de que `Recipe.kt`, `RecipeValidator.kt`,
  `DevExpertRecipeGenerator.kt`, `domain/**`, `ui/**`, build, Taxonomía, `seed-recipes.json` e
  `init.sh` no cambian; `validateResponse` sin llamador en `main`.
- Resultado **manual** del emulador (clave válida → recetas reales en Búsqueda y Generador; clave
  ausente → modo semilla) y la línea de logcat.
- Docs actualizadas (`technical-discovery.md`, `domain-model.md`, `taxonomy.md` §4) y anotación de
  estado/evidencia en `feature_list.json` y `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **Dónde se completan `id`/`source`: en `AiRecipePayload.decode` (`data.ai`), antes del Validador.**
  El adaptador es el dueño de traducir el wire del proveedor al modelo de dominio; `Recipe` y
  `RecipeValidator` (dominio aceptado) **no se tocan** y `id`/`source` siguen obligatorios (el esquema
  **no** se debilita ni se hace opcional).
- **Wire DTO `AiRecipeDraft`** en lugar de inyectar `id`/`source` en el JSON o de relajar el decoder:
  es explícito sobre qué campos posee el modelo, ignora un `id`/`source`/`matchScore` enviado por el
  modelo (la app siempre los posee) y es testeable sin plumbing de JSON.
- **Las reglas se aplican con `RecipeValidator.validateRecipe`**, el mismo motor público del
  Validador, con `NEUTRAL_CONSTRAINTS` (el proveedor **no** pre-filtra por dieta/restricciones; eso lo
  hacen los llamadores aceptados, manteniendo vivo el estado Bloqueado de `ai-search`). Consecuencia
  explícita: `validateResponse` deja de tener llamador en `main` (sigue cubierto por su test) y no se
  modifica.
- **Ids deterministas por contenido (`ai-` + SHA-256 hex), sin `Random`/reloj/`UUID`**: `Recipe.id` es
  la PK de Favoritos y la identidad de `isSaved`; el hash evita colisiones entre llamadas
  (que un índice `ai-0` provocaría) y hace que regenerar el mismo contenido conserve el id. Un
  duplicado exacto dentro de una respuesta se desambigua con `-2`/`-3`… (determinista por orden).
- **`matchScore` se descarta en el wire y se devuelve `null`**: es derivado y no persistido
  (`ui.results.MatchScore`); alinea el proveedor real con `FakeRecipeGenerator` y evita persistir un
  score calculado por el modelo. Es una consecuencia directa de no incluir el campo en el DTO, no una
  funcionalidad nueva.
- **Sin cambios de UI ni de build**: no hay estados ni microcopy nuevos; no hacen falta dependencias.

### Hallazgos de la sesión de implementación

- **`tips` entra en la cadena canónica del `id`.** La lista de campos de `canonical` (§1) omite
  `tips`, pero el *Unknown* "Estabilidad de ids frente a cambios mínimos del modelo" afirma
  explícitamente que "un cambio de contenido (p. ej. tips) cambia el `id`". Se sigue el *Unknown*
  (contrato observable) e `AiRecipeIds.canonical` incluye los `tips` en orden: excluirlos generaría
  ids compartidos entre llamadas para recetas que solo difieren en los trucos, justo lo que el hash
  de contenido evita. `AiRecipeIdsTest.contenido_distinto_produce_id_distinto` lo cubre.

### Unknowns

- **Robustez del modelo fuera del contrato §4**: si el modelo envía un campo obligatorio del contrato
  (`prepMinutes`, `steps`, …) ausente o mal tipado, esa receta se descarta como `SCHEMA` (una receta
  rota no tumba el lote). Sigue vigente el *Unknown* de `real-ai-provider` sobre prosa/JSON embebido;
  no se mitiga aquí.
- **Colisión teórica de SHA-256**: irrelevante en la práctica (64 hex) y, además, desambiguada por el
  set de ids usados en la llamada.
- **Estabilidad de ids frente a cambios mínimos del modelo**: un cambio de contenido (p. ej. tips)
  cambia el `id`; un Favorito previo de esa receta no se "reconocería". Comportamiento esperado y
  documentado; el Favorito conserva su copia independientemente.
- **`validateResponse` sin llamador en `main`**: decisión documentada; si en el futuro aparece un
  consumidor con esquema completo, sigue disponible. No se elimina (dominio aceptado).
- **Latencia/UX de la espera**: sin cambios; `ai-provider-timeouts` ya la acotó y la carga es
  cancelable.

## Validator Checklist

- [ ] `AiRecipePayload.decode` decodifica la **forma real** del contrato §4 (sin `id`/`source`) a
      recetas válidas; con `main` ese mismo test daría lista vacía (regresión real de #70).
- [ ] Cada receta decodificada lleva `id` **único** en la llamada (sin duplicados aun con receta
      repetida) y `source == RecipeSource.AI`, completados **antes** de aplicar el Validador.
- [ ] Los ids son **deterministas y sin `Random`/reloj/`UUID`** (mismo contenido → mismo id; la
      política está en `AiRecipeIds` y se testea).
- [ ] Las **reglas del Validador siguen aplicándose**: ids de Taxonomía desconocidos → descartadas
      (`SCHEMA`); deriva de tiempos > 5 min → descartada (`INCOHERENT`); la receta válida del lote
      sobrevive; el proveedor **no** pre-filtra por dieta/restricciones (neutras).
- [ ] La tolerancia se preserva: `null`/blanco/no-JSON/`{"foo":1}`/`[1,2]`/`{"recipes":3}`/valla
      markdown → lista vacía sin lanzar; un elemento roto no tumba el resto.
- [ ] `Recipe.kt` y `RecipeValidator.kt` **intactos** (`id`/`source` siguen obligatorios; el esquema no
      se debilita); `DevExpertRecipeGenerator.kt`, `AiProviderPolicy.kt`, `OpenAiChat.kt`,
      `AiHttpClientFactory.kt` sin cambios.
- [ ] Test end-to-end en JVM: `MockEngine` 200 con forma real + `GenerationService` real → `accepted`
      no vacío y `aiUnavailable == false`.
- [ ] `matchScore` devuelto `null` (derivado por la app), coherente con `FakeRecipeGenerator`.
- [ ] Alcance: solo `data.ai` (3 ficheros nuevos/modificados) + sus tests + las tres docs; `domain/**`,
      `ui/**`, build, Taxonomía, `seed-recipes.json`, `init.sh`, `DESIGN.md` y `docs/design/**` sin
      cambios; sin dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos) y regresión de los tests
      de `data.ai` y `RecipeValidatorTest`.
- [ ] Docs actualizadas (`technical-discovery.md`, `domain-model.md` con el drift del `id` corregido,
      `taxonomy.md` §4 con la nota de que `id`/`source` los pone la app).
- [ ] `Visual Design Impact` sin capturas (no hay UI) y la verificación **manual** en emulador anotada
      como tal (clave válida → recetas reales; clave ausente → modo semilla), no automatizable desde WSL.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por implementer/validador/orquestador.
